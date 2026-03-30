package edu.ics499.teamsatisfaction.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import edu.ics499.teamsatisfaction.repository.WallSectionRepository;

/**
 * Logs how many rows JPA sees in {@code Wall_Section} at startup (not in {@code test} profile).
 * If this is 0 but MySQL shows data, the app is pointed at the wrong schema or the wrong table name.
 */
@Component
@Profile("!test")
public class WallSectionStartupDiagnostics implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WallSectionStartupDiagnostics.class);

    private final Environment environment;
    private final WallSectionRepository wallSectionRepository;

    public WallSectionStartupDiagnostics(Environment environment, WallSectionRepository wallSectionRepository) {
        this.environment = environment;
        this.wallSectionRepository = wallSectionRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        String url = environment.getProperty("spring.datasource.url", "(not set)");
        log.info("JDBC URL (password omitted): {}", sanitizeJdbcUrlForLog(url));
        log.info("Wall_Section row count (JPA): {}", wallSectionRepository.count());
    }

    private static String sanitizeJdbcUrlForLog(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        return url.replaceAll("([?&]password=)[^&]*", "$1***");
    }
}
