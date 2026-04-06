package edu.ics499.VBeta;

import edu.ics499.VBeta.application.AccountService;
import edu.ics499.VBeta.application.ProblemDiscussionService;
import edu.ics499.VBeta.controller.ProblemDiscussionController;
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
}
