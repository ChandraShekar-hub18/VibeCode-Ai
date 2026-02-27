package com.vibecode.ai_generation.controller;

import java.util.UUID;

import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vibecode.ai_generation.dto.GenerateCodeRequest;
import com.vibecode.ai_generation.dto.GenerateCodeResponse;
import com.vibecode.ai_generation.service.AiGenerationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiGenerationController {

    private final AiGenerationService aiService;

    @PostMapping("/generate")
    public ResponseEntity<GenerateCodeResponse> generate(
            @Valid @RequestBody GenerateCodeRequest request,
            @RequestHeader("Authorization") String authHeader,
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        
        return ResponseEntity.ok(
                aiService.generate(request, authHeader, userId)
        );
    }

    @PostMapping("/generateTest")
    public ResponseEntity<String> generateTest(@RequestBody String prompt) {
        return ResponseEntity.ok(aiService.generateTest(prompt));
    }

}
