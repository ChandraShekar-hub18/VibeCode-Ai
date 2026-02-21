package com.vibecode.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserUsageResponse {

    private UUID userId;
    private String planType;
    private Integer tokenQuota;
    private Integer tokenUsed;
    private Integer remainingTokens;
    private LocalDateTime quotaResetAt;

}
