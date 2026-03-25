package edu.ics499.VBeta.application;

import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public Map<String, String> getHealthStatus() {
        return Map.of("status", "ok");
    }
}
