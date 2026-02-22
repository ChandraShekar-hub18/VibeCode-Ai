package com.vibecode.project.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateProjectRequest {

    @NotBlank
    private String name;

    private String description;
    private List<String> techStack;
    private List<String> tags;

    private String visibility; // "public" or "private"
}
