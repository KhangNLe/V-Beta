package app.VBeta.api.dto;

/**
 * Response payload containing signed upload details and resulting public URL.
 *
 * @param signedURL signed URL used for uploading the object
 * @param method HTTP method expected for the signed upload request
 * @param uploadObjectName storage object key for the upload
 * @param publicURL public URL that can be used to access the uploaded object
 */
public record CloudFileStorageResponse(
        String signedURL,
        String method,
        String uploadObjectName,
        String publicURL
) {
}
