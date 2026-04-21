package edu.ics499.VBeta.application;

import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * {@code HealthService} supplies a minimal readiness payload for health endpoints.
 * <p>
 * It intentionally exposes a stable, lightweight response suitable for uptime checks,
 * container probes, and basic environment verification.
 */
@Service
public class HealthService {

    /**
     * Returns a simple status payload used by health checks.
     *
     * @return map containing health status information
     */
    public Map<String, String> getHealthStatus() {
        return Map.of("status", "ok");
    }
}
