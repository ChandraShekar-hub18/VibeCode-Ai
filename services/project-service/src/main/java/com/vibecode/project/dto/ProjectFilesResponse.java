package com.vibecode.project.dto;

import java.util.List;

import com.vibecode.project.entity.ProjectFile;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProjectFilesResponse {

    private String projectId;
    private List<ProjectFile> files;
}
