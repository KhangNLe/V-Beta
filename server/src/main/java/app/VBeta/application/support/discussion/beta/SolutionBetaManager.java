package app.VBeta.application.support.discussion.beta;

import app.VBeta.application.support.cloud.CloudStorageManager;
import app.VBeta.application.support.cloud.GcpFileStorageAdapter;
import app.VBeta.domain.model.discussions.DiscussionRoot;
import app.VBeta.domain.model.discussions.DiscussionType;
import app.VBeta.domain.model.discussions.SolutionBeta;
import com.google.cloud.storage.StorageException;
import app.VBeta.api.dto.discussions.video.CloudFileStorageRequest;
import app.VBeta.api.dto.discussions.video.CloudFileStorageResponse;
import app.VBeta.application.support.problem.ClimbingProblemManager;
import app.VBeta.repository.SolutionBetaRepository;
import org.springframework.stereotype.Service;
import app.VBeta.domain.model.climb.ClimbingProblem;

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
    private final CloudStorageManager cloudStorageManager;

    /**
     * Constructs a new {@code SolutionBetaManager} with repository and storage dependencies.
     *
     * @param solutionBetaRepository repository for solution beta entities
     * @param gcpFileStorageAdapter storage adapter used for URL and object deletion
     * @param cloudStorageManager user for signed URL creation
     */
    public SolutionBetaManager(SolutionBetaRepository solutionBetaRepository,
                               GcpFileStorageAdapter gcpFileStorageAdapter,
                               CloudStorageManager cloudStorageManager) {
        this.solutionBetaRepository = solutionBetaRepository;
        this.gcpFileStorageAdapter = gcpFileStorageAdapter;
        this.cloudStorageManager = cloudStorageManager;
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
            throw new RuntimeException("Cannot submit the same solution beta video twice.");
        }
    }

    private SolutionBeta findSolutionBeta(DiscussionRoot discussionRoot, String publicUrl){
        Optional<SolutionBeta> solutionBeta = solutionBetaRepository.findByDiscussionRootAndVideoURL(
                discussionRoot, publicUrl);
        return solutionBeta.orElseThrow( () ->
            new RuntimeException("Unable to find any solution beta for climbing problem from user.")
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
     * @throws RuntimeException
     * when one or more solution rows are missing
     */
    private List<SolutionBeta> getAllUserRelateSolutionBeta(List<DiscussionRoot> discussionRoots){
        List<SolutionBeta> solutionBetas = solutionBetaRepository.findByDiscussionRootIn(discussionRoots);

        if (solutionBetas.size() != discussionRoots.size()){
            throw new RuntimeException(String.format(
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
        return cloudStorageManager.createVideoSignedUrl(request);
    }
}
