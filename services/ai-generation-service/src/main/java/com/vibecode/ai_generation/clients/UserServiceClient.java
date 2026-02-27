package com.vibecode.ai_generation.clients;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.vibecode.ai_generation.dto.UserUsageResponse;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/users/{userId}/usage")
    UserUsageResponse getUsage(
            @PathVariable UUID userId,
            @RequestHeader("Authorization") String token
    );

    @PostMapping("/internal/users/{userId}/usage/increment")
    void incrementUsage(
            @PathVariable UUID userId,
            @RequestParam int tokens,
            @RequestHeader("Authorization") String token
    );
}
