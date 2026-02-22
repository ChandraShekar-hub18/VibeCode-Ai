package com.vibecode.project.dto;

import java.util.List;

import com.vibecode.project.entity.ProjectFile;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProjectFilesRequest {

    @NotEmpty
    private List<ProjectFile> files;
    private String versionMessage = "Updated project files";
}
