package com.vibecode.user.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.vibecode.user.dto.CreateUserProfileRequest;
import com.vibecode.user.dto.UpdateUserPlanRequest;
import com.vibecode.user.dto.UserProfileResponse;
import com.vibecode.user.dto.UserUsageResponse;
import com.vibecode.user.entity.PlanQuota;
import com.vibecode.user.entity.PlanType;
import com.vibecode.user.entity.UserProfile;
import com.vibecode.user.entity.UserRole;
import com.vibecode.user.repository.UserProfileRepository;
import com.vibecode.user.repository.UserRoleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserRoleRepository userRoleRepository;

    public void createProfile(CreateUserProfileRequest request) {
        if (userProfileRepository.existsById(request.getId())) {
            throw new RuntimeException("User profile with this ID already exists");
        }

        LocalDateTime now = LocalDateTime.now();

        UserProfile profile = UserProfile.builder()
                .id(request.getId())
                .fullName(request.getFullName())
                .avatarUrl(request.getAvatarUrl())
                .bio(request.getBio())
                .planType(PlanType.FREE.name())
                .tokenQuota(PlanQuota.FREE_TOKENS) // default for free plan
                .quotaResetAt(now.plusMonths(1)) // example reset time
                .createdAt(now)
                .updatedAt(now)
                .build();
        // Implementation for creating a user profile
        userProfileRepository.save(profile);

        UserRole userRole = UserRole.builder()
                .userId(request.getId())
                .roleName("USER")
                .assignedAt(now) // default role
                .build();
        userRoleRepository.save(userRole);

    }

    public UserProfileResponse getProfile(UUID userId) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"));

        List<String> roles = userRoleRepository.findByUserId(userId)
                .stream()
                .map(UserRole::getRoleName)
                .toList();

        return UserProfileResponse.builder()
                .userId(profile.getId())
                .fullName(profile.getFullName())
                .avatarUrl(profile.getAvatarUrl())
                .bio(profile.getBio())
                .planType(profile.getPlanType())
                .tokenQuota(profile.getTokenQuota())
                .tokenUsed(profile.getTokensUsed())
                .roles(roles)
                .quotaResetAt(profile.getQuotaResetAt())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private int resolveQuota(PlanType planType) {
        return switch (planType) {
            case FREE ->
                PlanQuota.FREE_TOKENS;
            case PRO ->
                PlanQuota.PRO_TOKENS;
            case ENTERPRISE ->
                PlanQuota.ENTERPRISE_TOKENS;
        };
    }

    public void updateUserPlan(UUID userId, UpdateUserPlanRequest request) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"));
        PlanType newPlan = request.getPlanType();
        profile.setPlanType(newPlan.name());
        profile.setTokenQuota(resolveQuota(newPlan));
        profile.setTokensUsed(0);
        profile.setQuotaResetAt(LocalDateTime.now().plusMonths(1)); // example reset time
        profile.setSubscriptionId(request.getSubscriptionId());
        profile.setUpdatedAt(LocalDateTime.now());

        userProfileRepository.save(profile);

    }

    public UserUsageResponse getUserUsage(UUID userId) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"));

        return UserUsageResponse.builder()
                .userId(profile.getId())
                .planType(profile.getPlanType())
                .tokenQuota(profile.getTokenQuota())
                .tokenUsed(profile.getTokensUsed())
                .remainingTokens(profile.getTokenQuota() - profile.getTokensUsed())
                .quotaResetAt(profile.getQuotaResetAt())
                .build();
    }
}
