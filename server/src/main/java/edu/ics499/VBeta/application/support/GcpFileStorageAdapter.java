package edu.ics499.VBeta.application.support;

import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.BlobInfo;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class GcpFileStorageAdapter implements VideoStoragePort {
    private final Storage storage;
    @Getter
    private final String publicBucketName;
    private final long expirationMinutes;

    public GcpFileStorageAdapter(
            Storage storage,
            @Value("${app.public-bucket-name}") String publicBucketName,
            @Value("${gcp.signed-url.expiration-minutes}") long expirationMinutes) {
        this.storage = storage;
        this.publicBucketName = publicBucketName;
        this.expirationMinutes = expirationMinutes;
    }

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

    @Override
    public String generatePublicURL(String bucketName, String fileName){
        return String.format("https://storage.googleapis.com/%s/%s",
                bucketName,
                fileName);
    }

}
