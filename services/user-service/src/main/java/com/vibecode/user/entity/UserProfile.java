package com.vibecode.user.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    private UUID id; //same id from auth service

    private String fullName;

    private String avatarUrl;

    @Column(length=500)
    private String bio;

    @Column(nullable=false)
    private String planType; // e.g., "free", "pro", "enterprise"

    @Column(nullable=false)
    private Integer tokenQuota;

    private Integer tokensUsed=0;

    @Column(nullable=false)
    private LocalDateTime quotaResetAt;

    private String subscriptionId;

    @Column(nullable=false)
    private LocalDateTime createdAt;

    @Column(nullable=false)
    private LocalDateTime updatedAt;
}
