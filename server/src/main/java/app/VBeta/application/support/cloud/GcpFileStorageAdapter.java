package app.VBeta.application.support.cloud;

import com.google.cloud.storage.*;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * {@code GcpFileStorageAdapter} is a Google Cloud Storage-backed implementation of
 * {@link VideoStoragePort}.
 * <p>
 * It generates V4 signed upload URLs, builds public object URLs, and deletes uploaded
 * objects by bucket/key identity.
 */
@Service
public class GcpFileStorageAdapter implements VideoStoragePort {
    private final Storage storage;
    @Getter
    private final String publicBucketName;
    private final long expirationMinutes;

    private static final long MAX_IMAGE_BYTES = 8L * 1024 * 1024;

    /**
     * Creates a cloud storage adapter with configured bucket and URL expiration.
     *
     * @param storage Google Cloud storage client
     * @param publicBucketName public bucket name for solution videos
     * @param expirationMinutes signed URL expiration duration in minutes
     */
    public GcpFileStorageAdapter(
            Storage storage,
            @Value("${app.public-bucket-name}") String publicBucketName,
            @Value("${gcp.signed-url.expiration-minutes}") long expirationMinutes) {
        this.storage = storage;
        this.publicBucketName = publicBucketName;
        this.expirationMinutes = expirationMinutes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URL generateSignedPutURL(String objectName, String contentType){
        BlobInfo blobInfo = BlobInfo.newBuilder(publicBucketName, objectName)
                .setContentType(contentType)
                .build();

        return storage.signUrl(
                blobInfo,expirationMinutes,
                TimeUnit.MINUTES,
                Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                Storage.SignUrlOption.withExtHeaders(Map.of("Content-Type", contentType)),
                Storage.SignUrlOption.withV4Signature()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String generatePublicURL(String bucketName, String fileName){
        return String.format("https://storage.googleapis.com/%s/%s",
                bucketName,
                fileName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteFile(String bucketName, String fileName){
        Blob blob = storage.get(bucketName, fileName);

        if (blob == null){
            throw new RuntimeException(String.format("Unable to find file %s inside the cloud storage. The object may already be deleted.",
                            fileName)
            );
        }

        BlobId idWithGeneration = blob.getBlobId();
        storage.delete(idWithGeneration);
    }

    public void assertImageObjectWithinSizeLimit(String objectFileName){
        if (objectFileName == null || objectFileName.isEmpty()){
            throw new IllegalArgumentException("Object file name cannot be null or empty");
        }

        Blob blob = storage.get(publicBucketName, objectFileName,
                Storage.BlobGetOption.fields(Storage.BlobField.SIZE, Storage.BlobField.CONTENT_TYPE));

        if (blob == null){
            throw new IllegalArgumentException("Uploaded image not found in storage");
        }

        long size = blob.getSize();
        if (size <= 0){
            storage.delete(blob.getBlobId());
            throw new IllegalArgumentException("Uploaded image is empty");
        } else if (size > MAX_IMAGE_BYTES){
            storage.delete(blob.getBlobId());
            throw new IllegalArgumentException("Uploaded image exceeds 8 MB limit");
        }

        grantPublicRead(blob);
    }

    /**
     * Allows anonymous browser {@code <img>} loads of the uploaded object.
     * No-op when the bucket uses uniform access and already grants public read via IAM.
     */
    private void grantPublicRead(Blob blob){
        try {
            storage.createAcl(
                    blob.getBlobId(),
                    Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)
            );
        } catch (StorageException ignored) {
            // Uniform bucket-level access rejects object ACLs. Public GET then
            // depends on bucket IAM (allUsers as Storage Object Viewer).
        }
    }
}
