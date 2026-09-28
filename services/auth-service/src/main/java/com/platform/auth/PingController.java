package com.platform.auth;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Smoke-test endpoint. Delete once the real endpoints exist. */
@RestController
public class PingController {

    @GetMapping("/api/auth/ping")
    public Map<String, String> ping() {
        return Map.of("service", "auth-service", "status", "UP");
    }
}
