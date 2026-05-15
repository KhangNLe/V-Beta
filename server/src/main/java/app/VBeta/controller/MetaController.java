package app.VBeta.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code MetaController} exposes basic API metadata for clients and diagnostics.
 */
@RestController
@RequestMapping(path = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class MetaController {

    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * Returns metadata describing the running application.
     *
     * @return map containing application metadata fields
     */
    @GetMapping("/meta")
    public Map<String, String> meta() {
        return Map.of("name", applicationName);
    }
}
