package com.vibecode.user.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vibecode.user.dto.CreateUserProfileRequest;
import com.vibecode.user.dto.UpdateUserPlanRequest;
import com.vibecode.user.dto.UserProfileResponse;
import com.vibecode.user.dto.UserUsageResponse;
import com.vibecode.user.service.UserProfileService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @PostMapping("/")
    public ResponseEntity<String> createProfile(@Valid @RequestBody CreateUserProfileRequest request) {

        userProfileService.createProfile(request);

        return ResponseEntity.ok("User profile created successfully");
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getProfile(@PathVariable("userId") UUID userUuid) {
        System.out.println("Received request to get profile for userId: " + userUuid);
        return ResponseEntity.ok(userProfileService.getProfile(userUuid));
    }

    @PutMapping("/{userId}/plan")
    public ResponseEntity<String> updatePlan(@PathVariable("userId") UUID userId, @Valid @RequestBody UpdateUserPlanRequest request) {
        userProfileService.updateUserPlan(userId, request);
        return ResponseEntity.ok("User plan updated successfully");
    }

    @GetMapping("/{userId}/usage")
    public ResponseEntity<UserUsageResponse> getUserUsage(@PathVariable UUID userId) {
        return ResponseEntity.ok(userProfileService.getUserUsage(userId));
    }

    @PostMapping("/internal/users/{userId}/usage/increment")
    public ResponseEntity<Void> incrementUsage(
            @PathVariable UUID userId,
            @RequestParam int tokens) {

        userProfileService.incrementUsage(userId, tokens);
        return ResponseEntity.ok().build();
    }

}
