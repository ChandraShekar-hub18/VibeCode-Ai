package com.vibecode.ai_generation.dto;

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
public class ProjectFile {
    private String path;
    private String filename;
    private String language;
    private String content;

    private Long size;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    
}
