package com.vibecode.project.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/projects")
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "Project Service is healthy!";
    }
}
