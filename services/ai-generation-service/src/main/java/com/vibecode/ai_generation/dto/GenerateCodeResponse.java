package com.vibecode.ai_generation.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GenerateCodeResponse {

    private String projectId;
    private String message;
    private boolean success;
}
