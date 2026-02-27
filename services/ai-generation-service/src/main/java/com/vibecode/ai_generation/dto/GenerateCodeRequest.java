package com.vibecode.ai_generation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenerateCodeRequest{

    @NotBlank
    private String projectId;

    @NotBlank
    private String prompt;

    private String model = "gpt-4"; //default model
}