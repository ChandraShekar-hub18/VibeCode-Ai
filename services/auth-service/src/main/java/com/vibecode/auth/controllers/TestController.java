package com.vibecode.auth.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/test")
public class TestController {
    @GetMapping("/secure")
    public String secure() {
        return "This is a secure endpoint. You are authenticated!";
    }
    
}
