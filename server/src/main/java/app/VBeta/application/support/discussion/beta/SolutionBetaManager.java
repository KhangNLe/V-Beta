package app.VBeta.application.support.discussion.beta;

import app.VBeta.domain.model.discussions.DiscussionRoot;
import app.VBeta.domain.model.discussions.DiscussionType;
import app.VBeta.domain.model.discussions.SolutionBeta;
import com.google.cloud.storage.StorageException;
import app.VBeta.api.dto.discussions.video.CloudFileStorageRequest;
import app.VBeta.api.dto.discussions.video.CloudFileStorageResponse;
import app.VBeta.application.support.problem.ClimbingProblemManager;
import app.VBeta.repository.SolutionBetaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import app.VBeta.domain.model.climb.ClimbingProblem;
import org.springframework.web.server.ResponseStatusException;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * {@code SolutionBetaManager} encapsulates domain operations for solution beta video entries.
 * It manages {@link SolutionBeta} entries keyed by {@link DiscussionRoot}, validates uniqueness,
 * coordinates object-storage cleanup, and prepares signed upload metadata.
 * <p>
 * Storage concerns are delegated to {@link GcpFileStorageAdapter}, while climbing problem validation
 * is delegated to {@link ClimbingProblemManager}.
 */
@Service
public class SolutionBetaManager {
    private final SolutionBetaRepository solutionBetaRepository;
    private final GcpFileStorageAdapter gcpFileStorageAdapter;
    private final ClimbingProblemManager climbingProblemManager;

    /**
     * Constructs a new {@code SolutionBetaManager} with repository and storage dependencies.
     *
     * @param solutionBetaRepository repository for solution beta entities
     * @param gcpFileStorageAdapter storage adapter used for URL and object deletion
     * @param climbingProblemManager manager used to validate active problem state
     */
    public SolutionBetaManager(SolutionBetaRepository solutionBetaRepository,
                               GcpFileStorageAdapter gcpFileStorageAdapter,
                               ClimbingProblemManager climbingProblemManager){
        this.solutionBetaRepository = solutionBetaRepository;
        this.gcpFileStorageAdapter = gcpFileStorageAdapter;
        this.climbingProblemManager = climbingProblemManager;
    }

    /**
     * Returns persisted solution beta objects for discussion roots of type {@link DiscussionType#BETA}.
     *
     * @param discussionRoots discussion roots to resolve
     * @return list of matching solution betas (missing rows are skipped)
     */
    public List<SolutionBeta> getProblemSolutionBeta(List<DiscussionRoot> discussionRoots){
        return discussionRoots.stream()
                .filter(d -> d.getDiscussionType().equals(DiscussionType.BETA))
                .map(this::getSolutionBetaFromDiscussionRoot)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Returns the solution beta associated with a discussion root.
     *
     * @param discussionRoot discussion root parent record
     * @return solution beta or {@code null} when not found
     */
    public SolutionBeta getSolutionBetaFromDiscussionRoot(DiscussionRoot discussionRoot){
        Optional<SolutionBeta> solutionBeta = solutionBetaRepository.findByDiscussionRoot(discussionRoot);
        return solutionBeta.orElse(null);
    }

    /**
     * Stores a solution beta for a discussion root.
     *
     * @param discussionRoot discussion root parent record
     * @param objectFileName storage object key/name
     * @param publicUrl public media URL
     * @return persisted solution beta
     */
    public SolutionBeta storeUserSolutionBeta(DiscussionRoot discussionRoot, String objectFileName, String publicUrl){
        return createSolutionBeta(discussionRoot, objectFileName, publicUrl);
    }

    /**
     * Removes a solution beta and its underlying storage object.
     *
     * @param discussionRoot discussion root parent record
     * @param publicUrl public URL identifying the beta to remove
     */
    public void removeUserSolutionBeta(DiscussionRoot discussionRoot, String publicUrl){
        SolutionBeta solutionBeta = findSolutionBeta(discussionRoot, publicUrl);
        gcpFileStorageAdapter.deleteFile(gcpFileStorageAdapter.getPublicBucketName(), solutionBeta.getBetaName());
        solutionBetaRepository.delete(solutionBeta);
    }

    private SolutionBeta createSolutionBeta(DiscussionRoot discussionRoot, String objectFileName, String publicURL){
        checkForExistingSolutionBeta(publicURL);
        SolutionBeta solutionBeta = new SolutionBeta();
        solutionBeta.setDiscussionRoot(discussionRoot);
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

    private SolutionBeta findSolutionBeta(DiscussionRoot discussionRoot, String publicUrl){
        Optional<SolutionBeta> solutionBeta = solutionBetaRepository.findByDiscussionRootAndVideoURL(
                discussionRoot, publicUrl);
        return solutionBeta.orElseThrow( () ->
            new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Unable to find any solution beta for climbing problem from user."
            )
        );
    }

    /**
     * Removes every beta/video row associated with the provided discussion roots.
     * <p>
     * This bulk flow validates one-to-one row consistency before deleting
     * storage objects and database rows.
     *
     * @param discussionRoots discussion roots whose beta entries should be removed
     */
    public void removeAllDiscussionRelatedSolutionBeta(List<DiscussionRoot> discussionRoots){
        List<SolutionBeta> solutionBetas = getAllUserRelateSolutionBeta(discussionRoots);
        solutionBetas.forEach(sb ->
                gcpFileStorageAdapter.deleteFile(gcpFileStorageAdapter.getPublicBucketName(), sb.getBetaName())
        );
        solutionBetaRepository.deleteAll(solutionBetas);
    }

    /**
     * Resolves solution-beta rows for discussion roots and checks
     * one-to-one consistency.
     *
     * @param discussionRoots discussion roots expected to have solution-beta rows
     * @return matching solution-beta rows
     * @throws ResponseStatusException with {@link HttpStatus#INTERNAL_SERVER_ERROR}
     * when one or more solution rows are missing
     */
    private List<SolutionBeta> getAllUserRelateSolutionBeta(List<DiscussionRoot> discussionRoots){
        List<SolutionBeta> solutionBetas = solutionBetaRepository.findByDiscussionRootIn(discussionRoots);

        if (solutionBetas.size() != discussionRoots.size()){
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    String.format(
                            "Mismatching size of solution betas %d and user betas %s from user beta id %d. "
                            + "Please report this to the developers.",
                            solutionBetas.size(), discussionRoots.size(),
                            discussionRoots.get(0).getUserAccount().getId()
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
