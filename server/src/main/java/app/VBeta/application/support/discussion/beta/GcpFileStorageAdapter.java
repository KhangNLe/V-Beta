package app.VBeta.application.support.discussion.beta;

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

}
