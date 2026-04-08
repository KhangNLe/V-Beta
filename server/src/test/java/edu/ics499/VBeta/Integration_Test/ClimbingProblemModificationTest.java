package edu.ics499.VBeta.Integration_Test;

import edu.ics499.VBeta.application.ClimbingWallService;
import edu.ics499.VBeta.application.support.ClimbingProblemManager;
import edu.ics499.VBeta.application.support.RoleBasedAuthenticationManager;
import edu.ics499.VBeta.domain.model.ActionDefinition;
import edu.ics499.VBeta.domain.model.RoleType;
import edu.ics499.VBeta.repository.RolePermissionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3307}/${MYSQL_TEST_DB:V_Beta_Test}",
        "spring.datasource.username=${MYSQL_USERNAME:${SQL_USERNAME:khang}}",
        "spring.datasource.password=${MYSQL_PASSWORD:${SQL_PASSWORD:}}",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect"
})
public class ClimbingProblemModificationTest {
    @Autowired
    private ClimbingWallService climbingWallService;

    @Autowired
    private ClimbingProblemManager climbingProblemManager;


}
