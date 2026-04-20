package edu.ics499.VBeta.controller;

import java.util.Map;

import edu.ics499.VBeta.application.HealthService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code HealthController} publishes a lightweight health endpoint for uptime checks.
 * <p>
 * It delegates status payload creation to {@link HealthService}.
 */
@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class HealthController {

    private final HealthService healthService;

    /**
     * Constructs a new {@code HealthController}.
     *
     * @param healthService health service dependency
     */
    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    /**
     * Returns service health status.
     *
     * @return simple health status map
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return healthService.getHealthStatus();
    }
}
