package com.vibecode.user.dto;

import com.vibecode.user.entity.PlanType;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserPlanRequest {

    @NotNull
    private PlanType planType;
    private String subscriptionId; // For PRO/ENTERPRISE plans, store subscription details

}
