package com.vibecode.project.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromptRecord {

    private String promptText;
    private Integer tokensUsed;
    private String model;
    private LocalDateTime generatedAt;
}
