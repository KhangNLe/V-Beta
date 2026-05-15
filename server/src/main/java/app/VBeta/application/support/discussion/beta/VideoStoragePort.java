package app.VBeta.application.support.discussion.beta;

import java.net.URL;

/**
 * {@code VideoStoragePort} defines storage operations required by beta video workflows.
 * <p>
 * Implementations provide signed upload URL generation, public URL building, and object deletion.
 */
public interface VideoStoragePort {
    /**
     * Generates a signed URL for uploading an object with HTTP PUT.
     *
     * @param objectName object key to upload
     * @param contentType MIME type for upload
     * @return signed upload URL
     */
    URL generateSignedPutURL(String objectName, String contentType);

    /**
     * Builds a public URL for an object in a bucket.
     *
     * @param bucketName storage bucket name
     * @param fileName object key
     * @return public object URL
     */
    String generatePublicURL(String bucketName, String fileName);

    /**
     * Deletes an object from the storage bucket.
     *
     * @param bucketName storage bucket name
     * @param fileName object key
     */
    void deleteFile(String bucketName, String fileName);
}
