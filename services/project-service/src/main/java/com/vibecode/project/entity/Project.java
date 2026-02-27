package com.vibecode.project.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {


    @Id
    private String id;

    private UUID ownerId;
    private String name;
    private String description;
    private List<String> techStack;
    private List<String> tags;

    private List<ProjectFile> files;
    private List<ProjectVersion> versions;
    private List<PromptRecord> prompts;

    @NotNull
    private String visibility = "private"; // default to private
    private String parentProjectId;
    private UUID forkedFromUserId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
