package com.vibecode.user.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProfileResponse {

    private UUID userId;
    private String fullName;
    private String avatarUrl;
    private String bio;

    private String planType;
    private Integer tokenQuota;
    private Integer tokenUsed;
    private LocalDateTime quotaResetAt;
    private List<String> roles;

    private LocalDateTime createdAt;    
    private LocalDateTime updatedAt;



}
