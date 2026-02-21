package com.vibecode.user.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserProfileRequest {

    @NotNull
    private UUID id;

    private String fullName;
    private String avatarUrl;
    private String bio;

}
