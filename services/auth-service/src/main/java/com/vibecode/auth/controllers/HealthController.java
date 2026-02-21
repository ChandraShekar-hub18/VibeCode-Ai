package com.vibecode.auth.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "Auth Service is healthy!";
    }

    @GetMapping("/")
    public String hello(){
        return "Hello";
    }

}
