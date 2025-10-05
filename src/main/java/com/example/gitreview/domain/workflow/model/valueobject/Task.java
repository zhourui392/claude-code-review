package com.example.gitreview.domain.workflow.model.valueobject;

import com.example.gitreview.domain.workflow.model.TaskStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Task value object.
 *
 * @author zhourui(V33215020)
 * @since 2025/10/05
 */
public class Task {
    private String id;
    private String title;
    private String description;
    private TaskStatus status;
    private String errorMessage;
    private String generatedCode;
    private String targetFile;
    private List<String> dependencies;

    // 默认构造函数供Jackson使用
    public Task() {
        this.dependencies = new ArrayList<>();
    }

    public Task(String id, String description) {
        this.id = id;
        this.title = description;
        this.description = description;
        this.status = TaskStatus.PENDING;
        this.dependencies = new ArrayList<>();
    }

    @JsonCreator
    private Task(
            @JsonProperty("id") String id,
            @JsonProperty("title") String title,
            @JsonProperty("description") String description,
            @JsonProperty("status") TaskStatus status,
            @JsonProperty("errorMessage") String errorMessage,
            @JsonProperty("generatedCode") String generatedCode,
            @JsonProperty("targetFile") String targetFile,
            @JsonProperty("dependencies") List<String> dependencies) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.errorMessage = errorMessage;
        this.generatedCode = generatedCode;
        this.targetFile = targetFile;
        this.dependencies = dependencies != null ? new ArrayList<>(dependencies) : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getGeneratedCode() {
        return generatedCode;
    }

    public String getTargetFile() {
        return targetFile;
    }

    public List<String> getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    public void markInProgress() {
        this.status = TaskStatus.IN_PROGRESS;
    }

    public void markCompleted() {
        this.status = TaskStatus.COMPLETED;
    }

    public void markFailed(String errorMessage) {
        this.status = TaskStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public Task complete(String code) {
        return new Task(this.id, this.title, this.description, TaskStatus.COMPLETED, null, code, this.targetFile, this.dependencies);
    }

    public Task fail(String error) {
        return new Task(this.id, this.title, this.description, TaskStatus.FAILED, error, null, this.targetFile, this.dependencies);
    }
}
