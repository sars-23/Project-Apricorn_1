package com.apricorn.user.controller;

import java.util.Map;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {
    @GetMapping("/test")
    public Map<String, String> test() { return Map.of("status", "ADMIN_ACCESS_GRANTED"); }
}
