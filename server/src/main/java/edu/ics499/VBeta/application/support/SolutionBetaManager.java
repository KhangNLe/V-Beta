package edu.ics499.VBeta.application.support;

import com.google.cloud.storage.StorageException;
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
import java.util.*;

@Service
public class SolutionBetaManager {
    private final UserBetaRepository userBetaRepository;
    private final SolutionBetaRepository solutionBetaRepository;
    private final GcpFileStorageAdapter gcpFileStorageAdapter;
    private final ClimbingProblemManager climbingProblemManager;

    public SolutionBetaManager(UserBetaRepository userBetaRepository,
                               SolutionBetaRepository solutionBetaRepository,
                               GcpFileStorageAdapter gcpFileStorageAdapter,
                               ClimbingProblemManager climbingProblemManager){
        this.userBetaRepository = userBetaRepository;
        this.solutionBetaRepository = solutionBetaRepository;
        this.gcpFileStorageAdapter = gcpFileStorageAdapter;
        this.climbingProblemManager = climbingProblemManager;
    }

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

    public List<UserBeta> getUserBetasForClimbingProblem(ClimbingProblem problem){
        return userBetaRepository.findByProblem(problem);
    }

    public SolutionBeta getSolutionBetaFromUserBeta(UserBeta userBeta){
        Optional<SolutionBeta> solutionBeta = solutionBetaRepository.findByUserBeta(userBeta);
        return solutionBeta.orElse(null);
    }


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
    }}
