package com.vibecode.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/users")
public class HealthController {

    @GetMapping("/health")
    public String healthString() {
        return "User Service is healthy";
    }
    

}
