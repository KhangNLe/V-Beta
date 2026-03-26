package edu.ics499.VBeta.Integration_Test;

import edu.ics499.VBeta.api.dto.AccountRequest;
import edu.ics499.VBeta.api.dto.AccountResponse;
import edu.ics499.VBeta.application.UserAccountManager;
import edu.ics499.VBeta.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.Assertions.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AccountControllerTest {
    @Autowired
    private UserAccountRepository accountRepository;

    @Autowired
    private UserAccountManager userAccountManager;

    @Test
    void testAccountConnection() throws Exception {
        AccountRequest req = new AccountRequest(
            "testUser",
            "testUser@gmail.com",
            "testFirebaseUid"
        );

        AccountResponse resp = userAccountManager.loginAccount(req);
        assertTrue(accountRepository.findByFirebaseUid("testFirebaseUid").isPresent());
        assertNotNull(resp);
        assertEquals(1L, resp.id());
        assertEquals("testFirebaseUid", resp.firebaseUid());
        assertEquals("testUser", resp.username());
        assertEquals("testUser@gmail.com", resp.email());
    }
}