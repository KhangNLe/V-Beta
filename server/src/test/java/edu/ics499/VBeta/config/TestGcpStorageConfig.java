package edu.ics499.VBeta.config;

import com.google.cloud.storage.Storage;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Provides a mock {@link Storage} client when the test profile disables real GCP storage.
 * CI and local integration tests must not require a {@code google-account-credential.json} file.
 */
@Configuration
@Profile("test")
public class TestGcpStorageConfig {

    @Bean
    public Storage storage() {
        return Mockito.mock(Storage.class);
    }
}
