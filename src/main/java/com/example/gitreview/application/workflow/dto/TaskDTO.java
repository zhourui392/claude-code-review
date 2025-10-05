package com.example.gitreview.application.workflow.dto;

import java.util.List;

/**
 * 任务响应DTO
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
public class TaskDTO {

    private String id;
    private String title;
    private String description;
    private String status;
    private List<String> dependencies;
    private String generatedCode;

    public TaskDTO() {
    }

    public TaskDTO(String id, String title, String description, String status,
                   List<String> dependencies, String generatedCode) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.dependencies = dependencies;
        this.generatedCode = generatedCode;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public String getGeneratedCode() {
        return generatedCode;
    }

    public void setGeneratedCode(String generatedCode) {
        this.generatedCode = generatedCode;
    }
}
