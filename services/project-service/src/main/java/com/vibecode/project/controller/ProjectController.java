package com.vibecode.project.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vibecode.project.dto.CreateProjectRequest;
import com.vibecode.project.dto.ProjectFilesResponse;
import com.vibecode.project.dto.UpdateProjectFilesRequest;
import com.vibecode.project.entity.Project;
import com.vibecode.project.service.ProjectService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping("/")
    public ResponseEntity<Project> createProject(@Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID ownerId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(projectService.createProject(request, ownerId));
    }

    @GetMapping("/{projectId}/files")
    public ResponseEntity<ProjectFilesResponse> getProjectFile(@PathVariable String projectId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(projectService.getProjectFiles(projectId, userId));
    }

    @PostMapping("/{projectId}/fork")
    public ResponseEntity<Project> forkProject(
            @PathVariable String projectId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID requesterId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok(
                projectService.forkProject(projectId, requesterId)
        );
    }

    @PutMapping("/{projectId}/files")
    public ResponseEntity<Project> updateProjectFiles(
            @PathVariable String projectId,
            @Valid @RequestBody UpdateProjectFilesRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID requesterId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok(
                projectService.updateProjectFiles(
                        projectId,
                        request.getFiles(),
                        request.getVersionMessage(),
                        requesterId
                )
        );
    }

}
