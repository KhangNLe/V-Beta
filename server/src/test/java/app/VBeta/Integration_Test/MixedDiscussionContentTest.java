package app.VBeta.Integration_Test;

import app.VBeta.api.dto.discussions.DiscussionContentDeletionRequest;
import app.VBeta.api.dto.discussions.DiscussionContentRequest;
import app.VBeta.api.dto.discussions.UserDiscussionData;
import app.VBeta.api.dto.discussions.comment.DiscussionCommentRequest;
import app.VBeta.application.ProblemDiscussionService;
import app.VBeta.application.support.account.UserAccountManager;
import app.VBeta.application.support.cloud.GcpFileStorageAdapter;
import app.VBeta.application.support.discussion.ClimbingProblemDiscussionManager;
import app.VBeta.application.support.problem.ClimbingProblemManager;
import app.VBeta.config.TestGcpStorageConfig;
import app.VBeta.domain.model.climb.ClimbingProblem;
import app.VBeta.domain.model.discussions.DiscussionRoot;
import app.VBeta.domain.model.discussions.DiscussionType;
import app.VBeta.domain.model.user.UserAccount;
import app.VBeta.repository.DiscussionCommentRepository;
import app.VBeta.repository.DiscussionRootRepository;
import app.VBeta.repository.SolutionBetaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Import(TestGcpStorageConfig.class)
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource("classpath:application-postgres-it.properties")
public class MixedDiscussionContentTest {
    private static final String OWNER_UID = "testFirebaseUid";
    private static final String OTHER_UID = "testFirebaseUid2";
    private static final String ADMIN_UID = "testFirebaseUid3";
    private static final Long ACTIVE_PROBLEM_ID = 2L;
    private static final Long ARCHIVED_PROBLEM_ID = 3L;
    private static final String COMMENT_TEXT = "Heel hook on the left, then match";
    private static final String USER_DELETED_OWN_DISCUSSION = "User deleted their own discussion";
    private static final String ADMIN_FORCED_DELETE_DISCUSSION = "Admin forced delete the discussion";

    @Autowired
    private ProblemDiscussionService problemDiscussionService;

    @Autowired
    private ClimbingProblemDiscussionManager climbingProblemDiscussionManager;

    @Autowired
    private ClimbingProblemManager climbingProblemManager;

    @Autowired
    private UserAccountManager userAccountManager;

    @Autowired
    private DiscussionRootRepository discussionRootRepository;

    @Autowired
    private DiscussionCommentRepository discussionCommentRepository;

    @Autowired
    private SolutionBetaRepository solutionBetaRepository;

    @MockitoBean
    private GcpFileStorageAdapter gcpFileStorageAdapter;

    @Test
    @DisplayName("saving discussion content creates a MIXED root with both comment and beta rows")
    void testSaveMixedDiscussionContent() {
        DiscussionContentRequest request = newMixedRequest(ACTIVE_PROBLEM_ID);

        UserDiscussionData response = problemDiscussionService.saveUserDiscussionContent(OWNER_UID, request);

        assertNotNull(response.discussionId());
        assertEquals(COMMENT_TEXT, response.comment());
        assertEquals(request.videoURL(), response.videoURL());

        DiscussionRoot root = discussionRootRepository.findById(response.discussionId()).orElseThrow();
        assertEquals(DiscussionType.MIXED, root.getDiscussionType());
        assertEquals(COMMENT_TEXT, discussionCommentRepository.findByDiscussionRoot(root).orElseThrow().getCommentInfo());
        assertEquals(request.objectFileName(),
                solutionBetaRepository.findByDiscussionRoot(root).orElseThrow().getBetaName());
    }

    @Test
    @DisplayName("saving discussion content on an archived problem fails without creating rows")
    void testSaveMixedDiscussionContentOnArchivedProblemFails() {
        DiscussionContentRequest request = newMixedRequest(ARCHIVED_PROBLEM_ID);

        assertThrows(RuntimeException.class,
                () -> problemDiscussionService.saveUserDiscussionContent(OWNER_UID, request));
        assertTrue(solutionBetaRepository.findByVideoURL(request.videoURL()).isEmpty());
    }

    @Test
    @DisplayName("problem timeline returns MIXED posts with both comment and video")
    void testTimelineIncludesMixedDiscussion() {
        DiscussionContentRequest request = newMixedRequest(ACTIVE_PROBLEM_ID);
        UserDiscussionData saved = problemDiscussionService.saveUserDiscussionContent(OWNER_UID, request);

        ClimbingProblem problem = climbingProblemManager.getActiveProblem(ACTIVE_PROBLEM_ID);
        UserDiscussionData timelineItem = climbingProblemDiscussionManager.getCommentsForProblem(problem).stream()
                .filter(item -> item.discussionId().equals(saved.discussionId()))
                .findFirst()
                .orElseThrow();

        assertEquals(COMMENT_TEXT, timelineItem.comment());
        assertEquals(request.videoURL(), timelineItem.videoURL());
    }

    @Test
    @DisplayName("getDiscussionData maps a MIXED root to both comment and video")
    void testGetDiscussionDataForMixedRoot() {
        DiscussionContentRequest request = newMixedRequest(ACTIVE_PROBLEM_ID);
        UserDiscussionData saved = problemDiscussionService.saveUserDiscussionContent(OWNER_UID, request);

        DiscussionRoot root = discussionRootRepository.findById(saved.discussionId()).orElseThrow();
        UserDiscussionData data = climbingProblemDiscussionManager.getDiscussionData(root);

        assertNotNull(data);
        assertEquals(COMMENT_TEXT, data.comment());
        assertEquals(request.videoURL(), data.videoURL());
    }

    @Test
    @DisplayName("owner soft-deletes a MIXED post and keeps comment, beta, and storage object")
    void testOwnerSoftDeletesMixedDiscussion() {
        DiscussionContentRequest request = newMixedRequest(ACTIVE_PROBLEM_ID);
        UserDiscussionData saved = problemDiscussionService.saveUserDiscussionContent(OWNER_UID, request);
        UserAccount owner = userAccountManager.findUserAccount(OWNER_UID);

        problemDiscussionService.softDeleteUserDiscussionContent(OWNER_UID,
                deletionRequest(saved, COMMENT_TEXT, request.videoURL(), USER_DELETED_OWN_DISCUSSION));

        DiscussionRoot root = discussionRootRepository.findById(saved.discussionId()).orElseThrow();
        assertNotNull(root.getDeletedAt());
        assertEquals(USER_DELETED_OWN_DISCUSSION, root.getDeletedReason());
        assertEquals(owner.getId(), root.getDeletedBy().getId());
        assertTrue(discussionCommentRepository.findByDiscussionRoot(root).isPresent());
        assertTrue(solutionBetaRepository.findByDiscussionRoot(root).isPresent());
        verify(gcpFileStorageAdapter, never()).deleteFile(anyString(), anyString());

        ClimbingProblem problem = climbingProblemManager.getActiveProblem(ACTIVE_PROBLEM_ID);
        assertTrue(climbingProblemDiscussionManager.getCommentsForProblem(problem).stream()
                .anyMatch(item -> item.discussionId().equals(saved.discussionId()) && item.isDeleted()));
    }

    @Test
    @DisplayName("admin soft-deletes another user's MIXED post")
    void testAdminSoftDeletesMixedDiscussion() {
        DiscussionContentRequest request = newMixedRequest(ACTIVE_PROBLEM_ID);
        UserDiscussionData saved = problemDiscussionService.saveUserDiscussionContent(OWNER_UID, request);
        UserAccount admin = userAccountManager.findUserAccount(ADMIN_UID);

        problemDiscussionService.softDeleteUserDiscussionContent(ADMIN_UID,
                deletionRequest(saved, COMMENT_TEXT, request.videoURL(), ADMIN_FORCED_DELETE_DISCUSSION));

        DiscussionRoot root = discussionRootRepository.findById(saved.discussionId()).orElseThrow();
        assertNotNull(root.getDeletedAt());
        assertEquals(ADMIN_FORCED_DELETE_DISCUSSION, root.getDeletedReason());
        assertEquals(admin.getId(), root.getDeletedBy().getId());
    }

    @Test
    @DisplayName("deleting a MIXED post fails when the comment text does not match")
    void testSoftDeleteMixedDiscussionFailsForWrongComment() {
        DiscussionContentRequest request = newMixedRequest(ACTIVE_PROBLEM_ID);
        UserDiscussionData saved = problemDiscussionService.saveUserDiscussionContent(OWNER_UID, request);

        assertThrows(RuntimeException.class, () -> problemDiscussionService.softDeleteUserDiscussionContent(OWNER_UID,
                deletionRequest(saved, "Some other comment", request.videoURL(), USER_DELETED_OWN_DISCUSSION)));
        assertNull(discussionRootRepository.findById(saved.discussionId()).orElseThrow().getDeletedAt());
    }

    @Test
    @DisplayName("deleting a MIXED post fails when the video URL does not match")
    void testSoftDeleteMixedDiscussionFailsForWrongVideo() {
        DiscussionContentRequest request = newMixedRequest(ACTIVE_PROBLEM_ID);
        UserDiscussionData saved = problemDiscussionService.saveUserDiscussionContent(OWNER_UID, request);

        assertThrows(RuntimeException.class, () -> problemDiscussionService.softDeleteUserDiscussionContent(OWNER_UID,
                deletionRequest(saved, COMMENT_TEXT, "https://storage.googleapis.com/test-bucket/other.mp4",
                        USER_DELETED_OWN_DISCUSSION)));
        assertNull(discussionRootRepository.findById(saved.discussionId()).orElseThrow().getDeletedAt());
    }

    @Test
    @DisplayName("non-owner non-admin cannot delete a MIXED post")
    void testSoftDeleteMixedDiscussionFailsForOtherUser() {
        DiscussionContentRequest request = newMixedRequest(ACTIVE_PROBLEM_ID);
        UserDiscussionData saved = problemDiscussionService.saveUserDiscussionContent(OWNER_UID, request);

        assertThrows(RuntimeException.class, () -> problemDiscussionService.softDeleteUserDiscussionContent(OTHER_UID,
                deletionRequest(saved, COMMENT_TEXT, request.videoURL(), USER_DELETED_OWN_DISCUSSION)));
        assertNull(discussionRootRepository.findById(saved.discussionId()).orElseThrow().getDeletedAt());
    }

    @Test
    @DisplayName("mixed deletion against a comment-only post fails")
    void testSoftDeleteMixedDiscussionFailsForCommentOnlyRoot() {
        UserDiscussionData comment = problemDiscussionService.addComment(OWNER_UID,
                new DiscussionCommentRequest(ACTIVE_PROBLEM_ID, COMMENT_TEXT));

        assertThrows(RuntimeException.class, () -> problemDiscussionService.softDeleteUserDiscussionContent(OWNER_UID,
                deletionRequest(comment, COMMENT_TEXT, "https://storage.googleapis.com/test-bucket/none.mp4",
                        USER_DELETED_OWN_DISCUSSION)));
        assertNull(discussionRootRepository.findById(comment.discussionId()).orElseThrow().getDeletedAt());
    }

    private DiscussionContentRequest newMixedRequest(Long problemId) {
        String objectKey = "w1/p" + problemId + "/" + UUID.randomUUID() + ".mp4";
        return new DiscussionContentRequest(
                problemId,
                COMMENT_TEXT,
                objectKey,
                "https://storage.googleapis.com/test-bucket/" + objectKey
        );
    }

    private DiscussionContentDeletionRequest deletionRequest(UserDiscussionData saved, String comment,
                                                             String videoUrl, String reason) {
        return new DiscussionContentDeletionRequest(
                saved.userId(),
                saved.discussionId(),
                ACTIVE_PROBLEM_ID,
                comment,
                videoUrl,
                reason
        );
    }
}
