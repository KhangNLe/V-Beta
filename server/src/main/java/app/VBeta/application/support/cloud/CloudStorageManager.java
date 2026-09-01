package app.VBeta.application.support.cloud;

import app.VBeta.api.dto.discussions.video.CloudFileStorageRequest;
import app.VBeta.api.dto.discussions.video.CloudFileStorageResponse;
import app.VBeta.api.dto.image.ImageStorageRequest;
import app.VBeta.application.support.problem.ClimbingProblemManager;
import com.google.cloud.storage.StorageException;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Locale;
import java.util.UUID;

/**
 * Coordinates signed upload URL generation and object deletion for solution-beta videos
 * and wall/problem/profile images in Google Cloud Storage.
 */
@Service
public class CloudStorageManager {
    private final GcpFileStorageAdapter gcpFileStorageAdapter;
    private final ClimbingProblemManager climbingProblemManager;

    public  CloudStorageManager(GcpFileStorageAdapter gcpFileStorageAdapter,
                                ClimbingProblemManager climbingProblemManager) {
        this.gcpFileStorageAdapter = gcpFileStorageAdapter;
        this.climbingProblemManager = climbingProblemManager;
    }

    public CloudFileStorageResponse createVideoSignedUrl(CloudFileStorageRequest request) {
        if(!checkForActiveClimbingProblem(request.problemId())){
            throw new IllegalStateException(
                    "Cannot upload solution video for inactive or unknown climbing problem."
            );
        }
        String objectName = buildVideoPublicObjectName(request);
        String contentType = resolveVideoContentType(objectName, request.contentType());

        return createSignedUrl(objectName, contentType);
    }

    /**
     * Builds a signed PUT URL and public URL for an image upload.
     *
     * @param request file name, content type, and image target metadata
     * @return signed upload response shared with solution-beta uploads
     * @throws IllegalArgumentException when the extension or content type is unsupported
     */
    public CloudFileStorageResponse createImageSignedUrl(ImageStorageRequest request){
        String objectName = buildImagePublicObjectName(request);
        String contentType = resolveImageContentType(objectName, request.contentType());
        return createSignedUrl(objectName, contentType);
    }

    /**
     * Deletes an image object from the public bucket when a key is present.
     * No-op when {@code objectFileName} is null or blank.
     *
     * @param objectFileName GCS object key
     */
    public void deleteImageObject(String objectFileName){
        if (objectFileName == null || objectFileName.isEmpty()) {return;}
        gcpFileStorageAdapter.deleteFile(gcpFileStorageAdapter.getPublicBucketName(), objectFileName);
    }

    private CloudFileStorageResponse createSignedUrl(String objectName, String contentType){
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

    private record FileParts(String base, String suffix){};

    private String resolveVideoContentType(String objectName, String requestedContentType){
        String fromName = videoContentTypeFromObjectName(objectName);
        if (fromName == null){
            throw new IllegalStateException("Solution beta must be mp4, webm, or mov. Other types are not playable");
        }

        if (requestedContentType != null && !requestedContentType.isBlank()) {
            String requested = requestedContentType.trim().toLowerCase(Locale.ROOT);
            if (requested.equals("video/x-quicktime")) {
                requested = "video/quicktime";
            }
            if (!requested.equals(fromName)) {
                throw new IllegalArgumentException(
                        "Solution beta content type must match mp4, webm, or mov."
                );
            }
            return fromName;
        }
        return fromName;
    }

    private String resolveImageContentType(String objectName, String requestedContentType){
        String fromName = imageContentTypeFromObjectName(objectName);
        if (fromName == null) {
            throw new IllegalArgumentException("Image content type must match jpeg, jpg, png, or webp");
        }
        if (requestedContentType != null && !requestedContentType.isBlank()) {
            String requested = requestedContentType.trim().toLowerCase(Locale.ROOT);
            if (!requested.equals(fromName)) {
                throw new IllegalArgumentException(
                        "Image content type must match jpeg, jpg, png, or webp"
                );
            }
        }
        return fromName;
    }

    private String buildVideoPublicObjectName(CloudFileStorageRequest request) {
        FileParts fileparts = getFileBaseAndSuffix(request.fileName());
        String safeBase = fileparts.base;
        String suffix = fileparts.suffix;
        if (fileparts.base().isBlank()) {
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

    private String buildImagePublicObjectName(ImageStorageRequest request){
        FileParts fileparts = getFileBaseAndSuffix(request.fileName());

        String base = fileparts.base();
        String suffix = fileparts.suffix();

        if (base.isEmpty()){
            base = "photo";
        }
        return switch (request.imageTargetType()){
            case USER_ACCOUNT -> String.format(
                    "image/userProfile-%d/%s-%s%s",
                    request.userid(),
                    UUID.randomUUID(),
                    base,
                    suffix
            );
            case WALL_SECTION -> String.format(
                    "image/wallSection-%d/%s-%s%s",
                    request.wallSectionId(),
                    UUID.randomUUID(),
                    base,
                    suffix
            );
            case CLIMBING_PROBLEM -> String.format(
                    "image/problem-%d/%s-%s%s",
                    request.problemId(),
                    UUID.randomUUID(),
                    base,
                    suffix
            );
        };
    }

    private FileParts getFileBaseAndSuffix(String fileName){
        if (fileName == null || fileName.isEmpty()){
            throw new IllegalArgumentException("fileName is null or empty");
        }

        String base = "", suffix = "";
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1){
            base = sanitizeFileName(fileName.substring(0, lastDot));
            suffix = "." + sanitizeFileName(fileName.substring(lastDot + 1)).toLowerCase(Locale.ROOT);
        } else {
            base = sanitizeFileName(fileName);
        }
        return new FileParts(base, suffix);
    }

    private String videoContentTypeFromObjectName(String objectName){
        int lastDot = objectName.lastIndexOf('.');
        String contentType = null;
        if (lastDot > 0 && lastDot < objectName.length() - 1) {
            String extension = objectName.substring(lastDot + 1).toLowerCase(Locale.ROOT);
            contentType = switch (extension){
                case "mp4" -> "video/mp4";
                case "webm" -> "video/webm";
                case "mov" -> "video/quicktime";
                default -> null;
            };
        }
        return contentType;
    }

    private String imageContentTypeFromObjectName(String objectName){
        int lastDot = objectName.lastIndexOf('.');
        String contentType = null;
        if (lastDot > 0 && lastDot < objectName.length() - 1){
            String fileType = objectName.substring(lastDot + 1).toLowerCase(Locale.ROOT);
            contentType =  switch (fileType){
                case "jpeg", "jpg" -> "image/jpeg";
                case "png" -> "image/png";
                case "webp" -> "image/webp";
                default -> null;
            };
        }
        return contentType;
    }

    private String sanitizeFileName(String filename){
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private boolean checkForActiveClimbingProblem(Long problemId){
        return climbingProblemManager.getActiveProblem(problemId) != null;
    }
}
