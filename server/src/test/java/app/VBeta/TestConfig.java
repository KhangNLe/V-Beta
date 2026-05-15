package app.VBeta;

import app.VBeta.application.AccountService;
import app.VBeta.application.AuthorizationService;
import app.VBeta.application.ProblemDiscussionService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestConfig {

    @Bean
    AccountService userAccountManager() {
        return Mockito.mock(AccountService.class);
    }

    @Bean
    ProblemDiscussionService problemDiscussionService() {
        return Mockito.mock(ProblemDiscussionService.class);
    }

    @Bean
    AuthorizationService authorizationService(){
        return Mockito.mock(AuthorizationService.class);
    }
}
