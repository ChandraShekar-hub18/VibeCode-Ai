package com.vibecode.ai_generation.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.vibecode.ai_generation.clients.ProjectServiceClient;
import com.vibecode.ai_generation.clients.UserServiceClient;
import com.vibecode.ai_generation.dto.GenerateCodeRequest;
import com.vibecode.ai_generation.dto.GenerateCodeResponse;
import com.vibecode.ai_generation.dto.ProjectFile;
import com.vibecode.ai_generation.dto.UpdateProjectFilesRequest;
import com.vibecode.ai_generation.dto.UserUsageResponse;
import com.vibecode.ai_generation.integration.OllamaClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiGenerationService {

    private final ProjectServiceClient projectClient;
    private final UserServiceClient userClient;
    private final OllamaClient ollamaClient;

    public GenerateCodeResponse generate(
            GenerateCodeRequest request,
            String authHeader,
            UUID userId) {

        // STEP 1 — check project access
        projectClient.getProjectFiles(request.getProjectId(), authHeader);

        // STEP 2 — check quota
        UserUsageResponse usage
                = userClient.getUsage(userId, authHeader);

        int requiredTokens = estimateTokens(request.getPrompt());

        if (usage.getRemainingTokens() < requiredTokens) {
            throw new RuntimeException("Quota exceeded");
        }

        // STEP 3 — generate mock files
        // ProjectFile generatedFile = mockGenerate(request.getPrompt());
        String llmOutput = ollamaClient.generateCode(request.getPrompt());
        ProjectFile generatedFile = convertToProjectFile(llmOutput);

        // STEP 4 — update project
        projectClient.updateProjectFiles(
                request.getProjectId(),
                buildUpdateRequest(generatedFile, request),
                authHeader
        );

        // STEP 5 — deduct tokens
        userClient.incrementUsage(userId, requiredTokens, authHeader);

        return GenerateCodeResponse.builder()
                .projectId(request.getProjectId())
                .success(true)
                .message("AI generation completed. Tokens used: " + requiredTokens)
                .build();
    }

    private ProjectFile convertToProjectFile(String llmOutput) {

        LocalDateTime now = LocalDateTime.now();

        return ProjectFile.builder()
                .path("src/AiGenerated.js")
                .filename("AiGenerated.js")
                .language("javascript")
                .content(llmOutput)
                .size((long) llmOutput.length())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private int estimateTokens(String prompt) {
        return Math.max(50, prompt.length() / 4);
    }

    private ProjectFile mockGenerate(String prompt) {

        LocalDateTime now = LocalDateTime.now();

        String generatedContent = """
        // ======================================
        // AI GENERATED FILE
        // ======================================
        // Prompt:
        // %s
        // ======================================

        export default function GeneratedComponent() {
            return (
                <div>
                    <h1>Generated from AI prompt</h1>
                    <p>%s</p>
                </div>
            );
        }
        """.formatted(prompt, prompt);

        return ProjectFile.builder()
                .path("src/GeneratedComponent.js")
                .filename("GeneratedComponent.js")
                .language("javascript")
                .content(generatedContent)
                .size((long) generatedContent.length())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private UpdateProjectFilesRequest buildUpdateRequest(
            ProjectFile file,
            GenerateCodeRequest request) {

        return UpdateProjectFilesRequest.builder()
                .files(List.of(file))
                .versionMessage("AI generation: " + request.getPrompt())
                .build();
    }

    public String generateTest(String prompt) {
        return ollamaClient.generateCode(prompt);
    }

}
