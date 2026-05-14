package edu.ics499.VBeta.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * {@code GcpConfig} provides Google Cloud Platform infrastructure beans.
 * <p>
 * It creates a configured {@link Storage} client using application credentials
 * and project metadata from Spring properties.
 */
@Configuration
@ConditionalOnProperty(
        prefix = "spring.cloud.gcp.storage",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class GcpConfig {

    /**
     * Creates a Google Cloud Storage client bean.
     *
     * @param projectId GCP project identifier used by the storage client
     * @param credentialsLocation resource location of the service-account credentials file
     * @return configured Google Cloud {@link Storage} service
     * @throws IOException when credential resource cannot be read
     */
    @Bean
    public Storage storage(
            @Value("${spring.cloud.gcp.project-id}") String projectId,
            @Value("${spring.cloud.gcp.credentials.location}") Resource credentialsLocation)
            throws IOException {
        GoogleCredentials credentials;
        try (InputStream is = credentialsLocation.getInputStream()) {
            credentials = GoogleCredentials.fromStream(is);
        }
        return StorageOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build()
                .getService();
    }
}
