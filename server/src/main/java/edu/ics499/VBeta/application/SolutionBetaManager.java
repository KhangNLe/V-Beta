package edu.ics499.VBeta.application;

import com.google.cloud.storage.StorageException;
import edu.ics499.VBeta.api.dto.CloudFileStorageRequest;
import edu.ics499.VBeta.api.dto.CloudFileStorageResponse;
import edu.ics499.VBeta.application.support.GcpFileStorageAdapter;
import edu.ics499.VBeta.domain.model.ClimbingProblem;
import edu.ics499.VBeta.domain.model.LifecycleStatus;
import edu.ics499.VBeta.repository.ClimbingProblemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class SolutionBetaManager {
    private final ClimbingProblemRepository climbingProblemRepository;
    private final GcpFileStorageAdapter fileStorageAdapter;

    public SolutionBetaManager(
            ClimbingProblemRepository climbingProblemRepository,
            GcpFileStorageAdapter fileStorageAdapter
    ){
        this.climbingProblemRepository = climbingProblemRepository;
        this.fileStorageAdapter = fileStorageAdapter;
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
            signedURL = fileStorageAdapter.generateSignedPutURL(objectName, contentType);
        } catch (StorageException e){
            throw new IllegalStateException("Failed to create signed upload URL.", e);
        }
        String publicVideoUrl = fileStorageAdapter.generatePublicURL(
                fileStorageAdapter.getPublicBucketName(),
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
        Optional<ClimbingProblem> problem = climbingProblemRepository.findById(problemId);
        return problem
                .map(climbingProblem ->
                    climbingProblem.getProblemStatus().equals(LifecycleStatus.ACTIVE))
                .orElse(false);
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
