package com.vibecode.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private String token;
    private String refreshToken;
    private String tokenType = "Bearer";

}
