package edu.ics499.VBeta;

import edu.ics499.VBeta.application.UserAccountManager;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestConfig {

    @Bean
    UserAccountManager userAccountManager() {
        return Mockito.mock(UserAccountManager.class);
    }
}
