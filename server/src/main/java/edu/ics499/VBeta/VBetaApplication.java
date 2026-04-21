package edu.ics499.VBeta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * {@code VBetaApplication} is the Spring Boot entry point for the Team Satisfaction server.
 */
@SpringBootApplication
public class VBetaApplication {

    /**
     * Launches the Spring Boot application context.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(VBetaApplication.class, args);
    }
}
