package com.vibecode.ai_generation.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "AI generation service running";
    }
}
