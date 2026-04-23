package edu.ics499.VBeta.application.support;

import com.google.cloud.storage.StorageException;
import edu.ics499.VBeta.api.dto.ClimbingProblemResponse;
import edu.ics499.VBeta.api.dto.CloudFileStorageRequest;
import edu.ics499.VBeta.api.dto.CloudFileStorageResponse;
import edu.ics499.VBeta.domain.model.*;
import edu.ics499.VBeta.repository.UserBetaRepository;
import edu.ics499.VBeta.repository.SolutionBetaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import edu.ics499.VBeta.domain.model.ClimbingProblem;
import org.springframework.web.server.ResponseStatusException;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;

/**
 * {@code SolutionBetaManager} encapsulates domain operations for solution beta video entries.
 * It manages the relationship between {@link UserBeta} and {@link SolutionBeta}, validates uniqueness,
 * coordinates object-storage cleanup, and prepares signed upload metadata.
 * <p>
 * Storage concerns are delegated to {@link GcpFileStorageAdapter}, while climbing problem validation
 * is delegated to {@link ClimbingProblemManager}.
 */
@Service
public class SolutionBetaManager {
    private final UserBetaRepository userBetaRepository;
    private final SolutionBetaRepository solutionBetaRepository;
    private final GcpFileStorageAdapter gcpFileStorageAdapter;
    private final ClimbingProblemManager climbingProblemManager;

    /**
     * Constructs a new {@code SolutionBetaManager} with repository and storage dependencies.
     *
     * @param userBetaRepository repository for user/problem beta links
     * @param solutionBetaRepository repository for solution beta entities
     * @param gcpFileStorageAdapter storage adapter used for URL and object deletion
     * @param climbingProblemManager manager used to validate active problem state
     */
    public SolutionBetaManager(UserBetaRepository userBetaRepository,
                               SolutionBetaRepository solutionBetaRepository,
                               GcpFileStorageAdapter gcpFileStorageAdapter,
                               ClimbingProblemManager climbingProblemManager){
        this.userBetaRepository = userBetaRepository;
        this.solutionBetaRepository = solutionBetaRepository;
        this.gcpFileStorageAdapter = gcpFileStorageAdapter;
        this.climbingProblemManager = climbingProblemManager;
    }

    /**
     * Returns persisted solution beta objects for a climbing problem.
     *
     * @param problem climbing problem context
     * @return list of solution betas, or {@code null} when none exist
     */
    public List<SolutionBeta> getProblemSolutionBeta(ClimbingProblem problem){
        List<UserBeta> userBetas = getUserBetasForClimbingProblem(problem);
        if (userBetas.isEmpty()) return null;
        List<SolutionBeta> betas = new ArrayList<>();
        userBetas.forEach(ub -> {
            Optional<SolutionBeta> beta = solutionBetaRepository.findByUserBeta(ub);
            beta.ifPresent(betas::add);
        });
        return betas;
    }

    /**
     * Returns user-beta link rows for a climbing problem.
     *
     * @param problem climbing problem context
     * @return matching user-beta rows
     */
    public List<UserBeta> getUserBetasForClimbingProblem(ClimbingProblem problem){
        return userBetaRepository.findByProblem(problem);
    }

    /**
     * Returns the solution beta associated with a user-beta row.
     *
     * @param userBeta user-beta relationship row
     * @return solution beta or {@code null} when not found
     */
    public SolutionBeta getSolutionBetaFromUserBeta(UserBeta userBeta){
        Optional<SolutionBeta> solutionBeta = solutionBetaRepository.findByUserBeta(userBeta);
        return solutionBeta.orElse(null);
    }

    /**
     * Stores a solution beta for a user and climbing problem.
     *
     * @param user owner account
     * @param problem target climbing problem
     * @param objectFileName storage object key/name
     * @param publicUrl public media URL
     * @return persisted solution beta
     */
    public SolutionBeta storeUserSolutionBeta(UserAccount user, ClimbingProblem problem,
                                      String objectFileName, String publicUrl){
        UserBeta userBeta = createUserBeta(user, problem);
        return createSolutionBeta(userBeta, objectFileName, publicUrl);
    }

    /**
     * Removes a user's solution beta and its underlying storage object.
     *
     * @param userAccount owner account
     * @param problem target climbing problem
     * @param publicUrl public URL identifying the beta to remove
     */
    public void removeUserSolutionBeta(UserAccount userAccount, ClimbingProblem problem, String publicUrl){
        List<UserBeta> userBetas = userBetaRepository.findByUserAndProblem(userAccount, problem);
        SolutionBeta solutionBeta = findSolutionBeta(userBetas, publicUrl);
        gcpFileStorageAdapter.deleteFile(gcpFileStorageAdapter.getPublicBucketName(), solutionBeta.getBetaName());
        UserBeta deletingBeta = solutionBeta.getUserBeta();
        solutionBetaRepository.delete(solutionBeta);
        userBetaRepository.delete(deletingBeta);
    }

    private UserBeta createUserBeta(UserAccount userAccount, ClimbingProblem problem){
        UserBeta userBeta = new UserBeta();
        userBeta.setUser(userAccount);
        userBeta.setProblem(problem);
        return userBetaRepository.save(userBeta);
    }

    private SolutionBeta createSolutionBeta(UserBeta userBeta, String objectFileName, String publicURL){
        checkForExistingSolutionBeta(publicURL);
        SolutionBeta solutionBeta = new SolutionBeta();
        solutionBeta.setUserBeta(userBeta);
        solutionBeta.setBetaName(objectFileName);
        solutionBeta.setVideoURL(publicURL);
        solutionBeta.setCreateDate(LocalDateTime.now());
        return solutionBetaRepository.save(solutionBeta);
    }

    private void checkForExistingSolutionBeta(String publicUrl){
        Optional<SolutionBeta> beta = solutionBetaRepository.findByVideoURL(publicUrl);
        if (beta.isPresent()){
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Cannot submit the same solution beta video twice."
            );
        }
    }

    private SolutionBeta findSolutionBeta(List<UserBeta> betas, String publicUrl){
        Optional<SolutionBeta> solutionBeta = solutionBetaRepository.findByUserBetaInAndVideoURL(betas, publicUrl);
        return solutionBeta.orElseThrow( () ->
            new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Unable to find any solution beta for climbing problem from user."
            )
        );
    }

    /**
     * Removes every beta/video row associated with a user.
     * <p>
     * This bulk flow validates that each {@link UserBeta} has a corresponding
     * {@link SolutionBeta} before deleting storage objects and database rows.
     *
     * @param userAccount account whose beta entries should be removed
     */
    public void removeAllUserRelatedSolutionBeta(UserAccount userAccount){
        List<UserBeta> userBetas = getAllUserBetas(userAccount);
        if (userBetas.isEmpty()) return;
        List<SolutionBeta> solutionBetas = getAllUserRelateSolutionBeta(userBetas);
        solutionBetas.forEach(sb ->
                gcpFileStorageAdapter.deleteFile(gcpFileStorageAdapter.getPublicBucketName(), sb.getBetaName())
        );
        solutionBetaRepository.deleteAll(solutionBetas);
        userBetaRepository.deleteAll(userBetas);
    }

    /**
     * Returns all user-beta link rows owned by a user.
     *
     * @param userAccount owner account
     * @return user-beta rows for the account
     */
    private List<UserBeta> getAllUserBetas(UserAccount userAccount){
        return userBetaRepository.findByUser(userAccount);
    }

    /**
     * Resolves solution-beta rows for a user's user-beta links and checks
     * one-to-one consistency.
     *
     * @param userBetas user-beta rows expected to have solution-beta rows
     * @return matching solution-beta rows
     * @throws ResponseStatusException with {@link HttpStatus#INTERNAL_SERVER_ERROR}
     * when one or more solution rows are missing
     */
    private List<SolutionBeta> getAllUserRelateSolutionBeta(List<UserBeta> userBetas){
        List<SolutionBeta> solutionBetas = solutionBetaRepository.findByUserBetaIn(userBetas);

        if (solutionBetas.size() != userBetas.size()){
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    String.format(
                            "Mismatching size of solution betas %d and user betas %s from user beta id %d. "
                            + "Please report this to the developers.",
                            solutionBetas.size(), userBetas.size(), userBetas.get(0).getId()
                    )
            );
        }
        return solutionBetas;
    }

    /**
     * Creates signed upload data for a new solution video.
     *
     * @param request upload request payload
     * @return signed upload and public URL metadata
     */
    public CloudFileStorageResponse createSignedUrl(CloudFileStorageRequest request){
        if(!checkForActiveClimbingProblem(request.problemId())){
            throw new IllegalStateException(
                    "Cannot upload solution video for inactive or unknown climbing problem."
            );
        }
        String objectName = buildPublicObjectName(request);
        String contentType = resolveContentType(objectName, request.contentType());

        URL signedURL;
        try {
            signedURL = gcpFileStorageAdapter.generateSignedPutURL(objectName, contentType);
        } catch (StorageException e){
            throw new IllegalStateException("Failed to create signed upload URL.", e);
        }
        String publicVideoUrl = gcpFileStorageAdapter.generatePublicURL(
                gcpFileStorageAdapter.getPublicBucketName(),
                objectName
        );
        return new CloudFileStorageResponse(
                signedURL.toString(),
                "PUT",
                objectName,
                publicVideoUrl
        );
    }

    private boolean checkForActiveClimbingProblem(Long problemId){
        ClimbingProblem problem = climbingProblemManager.getActiveProblem(problemId);
        if (problem == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "The problem id does not exist or the problem is longer active"
            );
        }
        return true;
    }

    private String buildPublicObjectName(CloudFileStorageRequest request) {
        if (request.fileName() == null || request.fileName().isBlank()) {
            throw new IllegalArgumentException("fileName is required.");
        }
        int lastDot = request.fileName().lastIndexOf('.');
        String safeBase;
        String suffix = "";
        if (lastDot > 0 && lastDot < request.fileName().length() - 1) {
            safeBase = sanitizeFileName(request.fileName().substring(0, lastDot));
            suffix = "." + sanitizeFileName(
                    request.fileName().substring(lastDot + 1)
            ).toLowerCase(Locale.ROOT);
        } else {
            safeBase = sanitizeFileName(request.fileName());
        }
        if (safeBase.isBlank()) {
            safeBase = "video";
        }
        return String.format(
                "wallSection-%d/problem-%d/%s-%s%s",
                request.wallSectionId(),
                request.problemId(),
                UUID.randomUUID(),
                safeBase,
                suffix
        );
    }

    private String resolveContentType(String objectName, String requestedContentType) {
        if (requestedContentType != null && !requestedContentType.isBlank()) {
            return requestedContentType.trim();
        }
        int lastDot = objectName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < objectName.length() - 1) {
            String ext = objectName.substring(lastDot + 1).toLowerCase(Locale.ROOT);
            if ("mp4".equals(ext)) {
                return "video/mp4";
            }
            if ("webm".equals(ext)) {
                return "video/webm";
            }
        }
        return "application/octet-stream";
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
