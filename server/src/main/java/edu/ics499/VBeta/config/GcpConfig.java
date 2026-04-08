package edu.ics499.VBeta.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class GcpConfig {

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
