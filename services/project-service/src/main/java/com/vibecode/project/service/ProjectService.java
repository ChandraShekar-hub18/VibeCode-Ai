package com.vibecode.project.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.vibecode.project.dto.CreateProjectRequest;
import com.vibecode.project.dto.ProjectFilesResponse;
import com.vibecode.project.entity.Project;
import com.vibecode.project.entity.ProjectFile;
import com.vibecode.project.entity.ProjectVersion;
import com.vibecode.project.repository.ProjectRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    public Project createProject(CreateProjectRequest request, UUID ownerId) {
        LocalDateTime now = LocalDateTime.now();
        Project project = Project.builder()
                .ownerId(ownerId)
                .name(request.getName())
                .description(request.getDescription())
                .techStack(request.getTechStack())
                .tags(request.getTags())
                .visibility(request.getVisibility())
                .files(new ArrayList<>())
                .versions(new ArrayList<>())
                .prompts(new ArrayList<>())
                .createdAt(now)
                .updatedAt(now)
                .build();
        ProjectVersion version = ProjectVersion.builder()
                .versionNumber(1)
                .message("Initial Project version")
                .filesSnapshot(new ArrayList<>())
                .createdAt(now)
                .build();
        project.getVersions().add(version);
        return projectRepository.save(project);
    }

    public ProjectFilesResponse getProjectFiles(String projectId, UUID userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        boolean isOwner = project.getOwnerId().equals(userId);
        boolean isPublic = "public".equalsIgnoreCase(project.getVisibility());

        if (!isOwner && !isPublic) {
            throw new RuntimeException("Access denied");
        }
        return ProjectFilesResponse.builder()
                .projectId(project.getId())
                .files(project.getFiles())
                .build();
    }

    public Project forkProject(String projectId, UUID requestId) {

        Project original = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        boolean isOwner = original.getOwnerId().equals(requestId);
        boolean isPublic = "public".equalsIgnoreCase(original.getVisibility());

        if (!isOwner && !isPublic) {
            throw new RuntimeException("Access denied");
        }

        LocalDateTime now = LocalDateTime.now();
        //Deep copy files
        // Deep copy files
        List<ProjectFile> copiedFiles = original.getFiles()
                .stream()
                .map(file -> ProjectFile.builder()
                .path(file.getPath())
                .filename(file.getFilename())
                .language(file.getLanguage())
                .content(file.getContent())
                .size(file.getSize())
                .createdAt(now)
                .updatedAt(now)
                .build())
                .toList();

        // Deep copy versions
        List<ProjectVersion> copiedVersions = original.getVersions()
                .stream()
                .map(version -> ProjectVersion.builder()
                .versionNumber(version.getVersionNumber())
                .message(version.getMessage())
                .filesSnapshot(copiedFiles)
                .createdAt(now)
                .build())
                .toList();

        Project forked = Project.builder()
                .ownerId(requestId)
                .name(original.getName() + " (Fork)")
                .description(original.getDescription())
                .techStack(original.getTechStack())
                .tags(original.getTags())
                .files(copiedFiles)
                .versions(copiedVersions)
                .prompts(new ArrayList<>()) // do not copy prompt history
                .visibility("PRIVATE") // forks start private
                .parentProjectId(original.getId())
                .forkedFromUserId(original.getOwnerId())
                .createdAt(now)
                .updatedAt(now)
                .build();

        return projectRepository.save(forked);
    }

    public Project updateProjectFiles(
            String projectId,
            List<ProjectFile> updatedFiles,
            String versionMessage,
            UUID userId
    ) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getOwnerId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        LocalDateTime now = LocalDateTime.now();

        //Update file timestamps
        List<ProjectFile> filesWithTimestamps = updatedFiles.stream()
                .map(file -> ProjectFile.builder()
                .path(file.getPath())
                .filename(file.getFilename())
                .language(file.getLanguage())
                .content(file.getContent())
                .size(file.getSize())
                .createdAt(now)
                .updatedAt(now)
                .build())
                .toList();
        // Replace current files
        project.setFiles(filesWithTimestamps);

        //Determine new version number
        int newVersionNumber = project.getVersions().size() + 1;

        //Create version snapshot
        ProjectVersion newVersion = ProjectVersion.builder()
                .versionNumber(newVersionNumber)
                .message(versionMessage != null ? versionMessage : "Updated project files")
                .filesSnapshot(filesWithTimestamps)
                .createdAt(now)
                .build();

        project.getVersions().add(newVersion);

        project.setUpdatedAt(now);
        return projectRepository.save(project);

    }

}
