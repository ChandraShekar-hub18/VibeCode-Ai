package com.vibecode.ai_generation.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "project-service")
public interface ProjectServiceClient {

    @GetMapping("/projects/{projectId}/files")
    Object getProjectFiles(
            @PathVariable String projectId,
            @RequestHeader("Authorization") String token
    );

    @PostMapping("/projects/{projectId}/files")
    Object updateProjectFiles(
            @PathVariable String projectId,
            Object request,
            @RequestHeader("Authorization") String token
    );

}
