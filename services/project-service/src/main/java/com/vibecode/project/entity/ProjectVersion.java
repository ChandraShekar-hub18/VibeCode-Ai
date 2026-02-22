package com.vibecode.project.entity;

import java.time.LocalDateTime;
import java.util.List;

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
public class ProjectVersion {

    private Integer versionNumber;
    private String message;
    private List<ProjectFile> filesSnapshot;
    private LocalDateTime createdAt;
}
