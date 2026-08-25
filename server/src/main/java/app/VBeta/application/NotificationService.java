package app.VBeta.application;

import app.VBeta.api.dto.notification.EventTypeDTO;
import app.VBeta.api.dto.notification.NotificationClickDTO;
import app.VBeta.api.dto.notification.NotificationClickKind;
import app.VBeta.api.dto.notification.QuickNotificationDTO;
import app.VBeta.application.support.account.UserAccountManager;
import app.VBeta.application.support.events.EventsManager;
import app.VBeta.application.support.events.NotificationManager;
import app.VBeta.domain.model.actions.RoleType;
import app.VBeta.domain.model.moderation.ModerationAction;
import app.VBeta.domain.model.notification.EventTypeName;
import app.VBeta.domain.model.notification.Events;
import app.VBeta.domain.model.notification.Notification;
import app.VBeta.domain.model.report.Report;
import app.VBeta.domain.model.user.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * {@code NotificationService} is the orchestration layer for in-app moderation notifications.
 * <p>
 * It records {@code REPORT_CREATED} and {@code APPEAL_SUBMITTED} events and fans
 * out inbox rows to admin recipients, records report-queue outcome events for
 * reporters and owners, maps unread notifications into
 * {@link QuickNotificationDTO} (including click metadata), and marks a caller's
 * notification as read.
 */
@Service
@Transactional
public class NotificationService {
    private final NotificationManager notificationManager;
    private final EventsManager eventManager;
    private final UserAccountManager userAccountManager;

    /**
     * Constructs a new {@code NotificationService} with required collaborators.
     *
     * @param notificationManager manager for inbox persistence, unread reads, and mark-read
     * @param eventManager manager for event persistence
     * @param userAccountManager manager for recipient account lookups
     */
    public NotificationService(NotificationManager notificationManager,
                               EventsManager eventManager,
                               UserAccountManager userAccountManager) {
        this.notificationManager = notificationManager;
        this.eventManager = eventManager;
        this.userAccountManager = userAccountManager;
    }

    /**
     * Records a {@code REPORT_CREATED} event and notifies admins, skipping the reporter.
     *
     * @param report persisted report that triggered the event
     */
    public void saveNewReportNotification(Report report) {
        Events event = eventManager.createReportEvent(report);
        for (UserAccount admin : userAccountManager.findUsersOfRole(RoleType.ADMIN)) {
            if (Objects.equals(admin.getId(), report.getReporter().getId())) {
                continue;
            }
            notificationManager.pushNotification(event, admin);
        }
    }

    /**
     * Records an {@code APPEAL_SUBMITTED} event and notifies admins, skipping the appellant.
     *
     * @param report report whose removal is being appealed
     * @param appealUser content owner who submitted the appeal
     */
    public void saveAppealSubmittedNotification(Report report, UserAccount appealUser) {
        Events event = eventManager.createAppealSubmittedEvent(report, appealUser);
        for (UserAccount admin : userAccountManager.findUsersOfRole(RoleType.ADMIN)) {
            if (Objects.equals(admin.getId(), appealUser.getId())) {
                continue;
            }
            notificationManager.pushNotification(event, admin);
        }
    }

    /**
     * Records a report-queue outcome event and pushes one inbox row to {@code toUser}.
     * <p>
     * The event actor is the deciding admin (never the reporter). Typical types:
     * {@link EventTypeName#REPORT_DISMISSED} and {@link EventTypeName#REPORT_APPROVED}
     * for the reporter, {@link EventTypeName#CONTENT_REMOVED} for the owner.
     *
     * @param decision logbook row whose admin is the event actor
     * @param toUser inbox recipient
     * @param report report the event targets
     * @param eventTypeName seeded event kind to record
     */
    public void sendReportModerationNotification(ModerationAction decision, UserAccount toUser, Report report,
                                                 EventTypeName eventTypeName) {
        Events event = eventManager.createModeratedReportEvent(report, eventTypeName, decision);
        notificationManager.pushNotification(event, toUser);

    }

    /**
     * Returns unread notification summaries for a user identified by Firebase UID.
     * <p>
     * Each item includes {@code notificationId}, catalog {@code summary}, a
     * {@link NotificationClickDTO} derived from the event target, and {@code createdAt}.
     * Only the caller's unread rows are returned. The payload does not include
     * report reason or admin notes.
     *
     * @param firebaseUid Firebase UID of the inbox owner
     * @return unread notification DTOs (empty when none)
     * @throws RuntimeException when no account matches the UID
     */
    public List<QuickNotificationDTO> getQuickNotifications(String firebaseUid) {
        UserAccount user = userAccountManager.findUserAccount(firebaseUid);
        if (user == null) {
            throw new RuntimeException("User is not found");
        }
        List<Notification> notifications = notificationManager.getUserUnreadNotifications(user);

        return notifications.stream().map(this::createQuickNotificationDTO).toList();
    }

    /**
     * Marks one notification read when it belongs to {@code firebaseUid}.
     * <p>
     * A second call on an already-read row is a no-op. Another user's
     * notification is treated as missing.
     *
     * @param firebaseUid Firebase UID of the inbox owner
     * @param notificationId inbox row identifier
     * @throws RuntimeException when the account is missing, or the notification
     *         is missing or not owned by this user
     */
    public void updateNotificationToRead(String firebaseUid, Long notificationId){
        UserAccount user = userAccountManager.findUserAccount(firebaseUid);
        if (user == null) {
            throw new RuntimeException("User is not found");
        }
        Notification notification = notificationManager.findNotificationByIdAndOwner(notificationId, user)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (notification.getReadAt() != null) {
            return;
        }

        notification.setReadAt(Instant.now());
        notificationManager.save(notification);
    }
    /**
     * Maps a persisted notification into the short inbox DTO.
     *
     * @param notification unread notification row
     * @return id, summary, click target, and created-at
     */
    private QuickNotificationDTO createQuickNotificationDTO(Notification notification) {
        return new QuickNotificationDTO(
                notification.getNotificationId(),
                createEventTypeDTO(notification.getEvent()),
                createNotificationRedirection(notification.getEvent()),
                notification.getCreatedAt()
        );
    }

    /**
     * Maps an event's catalog type into the API DTO.
     *
     * @param event persisted event
     * @return event type name and description
     */
    private EventTypeDTO createEventTypeDTO(Events event) {
        return new EventTypeDTO(
                event.getEventType().getEventTypeName().name(),
                event.getEventType().getDescription()
        );
    }

    /**
     * Builds click metadata from the event's typed target.
     * <p>
     * Current moderation events use {@code target_type = REPORT}, so
     * {@code kind} is {@link NotificationClickKind#REPORT_QUEUE} and
     * {@code reportId} is set. Other kinds are reserved for later event targets.
     * This does not leak reporter identity or report reason.
     *
     * @param event happened-fact row the notification points at
     * @return click kind and the ids that match that target
     */
    private NotificationClickDTO createNotificationRedirection(Events event){
        NotificationClickKind click = null;
        Long wallSectionId = null, climbingProblemId = null, discussionId = null,
                userId = null, reportId = null;

        switch(event.getTargetType()){
            case REPORT:
                click = NotificationClickKind.REPORT_QUEUE;
                reportId = event.getReport().getReportId();
                break;
            case WALL_SECTION:
                click = NotificationClickKind.WALL_SECTION;
                wallSectionId = event.getWallSection().getId();
                break;
            case CLIMBING_PROBLEM:
                click = NotificationClickKind.PROBLEM;
                wallSectionId = event.getProblem().getWallSection().getId();
                climbingProblemId = event.getWallSection().getId();
                break;
            case DISCUSSION:
                click = NotificationClickKind.PROBLEM_DISCUSSION;
                wallSectionId = event.getWallSection().getId();
                climbingProblemId = event.getWallSection().getId();
                discussionId = event.getWallSection().getId();
                break;
            case USER_ACCOUNT:
                userId = event.getUser().getId();
                break;
        }

        return new NotificationClickDTO(
                click,
                reportId,
                wallSectionId,
                climbingProblemId,
                discussionId,
                userId
        );
    }
}
