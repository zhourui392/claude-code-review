package com.example.gitreview.domain.workflow.model.valueobject;

import com.example.gitreview.domain.workflow.model.TaskStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 任务值对象
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
public class Task {

    private final String id;
    private final String title;
    private final String description;
    private final TaskStatus status;
    private final List<String> dependencies;
    private final String targetFile;
    private final String generatedCode;
    private final LocalDateTime completedAt;

    @JsonCreator
    public Task(@JsonProperty("id") String id,
                @JsonProperty("title") String title,
                @JsonProperty("description") String description,
                @JsonProperty("status") TaskStatus status,
                @JsonProperty("dependencies") List<String> dependencies,
                @JsonProperty("targetFile") String targetFile,
                @JsonProperty("generatedCode") String generatedCode,
                @JsonProperty("completedAt") LocalDateTime completedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.dependencies = dependencies;
        this.targetFile = targetFile;
        this.generatedCode = generatedCode;
        this.completedAt = completedAt;
    }

    /**
     * 完成任务
     *
     * @param code 生成的代码
     * @return 已完成的任务
     */
    public Task complete(String code) {
        return new Task(this.id, this.title, this.description, TaskStatus.COMPLETED,
                this.dependencies, this.targetFile, code, LocalDateTime.now());
    }

    /**
     * 标记任务失败
     *
     * @param reason 失败原因
     * @return 失败的任务
     */
    public Task fail(String reason) {
        return new Task(this.id, this.title, this.description, TaskStatus.FAILED,
                this.dependencies, this.targetFile, this.generatedCode, LocalDateTime.now());
    }

    /**
     * 检查任务是否可执行
     *
     * @param allTasks 所有任务列表
     * @return 如果所有依赖任务已完成则返回true
     */
    public boolean isExecutable(List<Task> allTasks) {
        if (this.status != TaskStatus.PENDING) {
            return false;
        }

        if (this.dependencies == null || this.dependencies.isEmpty()) {
            return true;
        }

        return this.dependencies.stream()
                .allMatch(depId -> allTasks.stream()
                        .anyMatch(t -> t.id.equals(depId) && t.status == TaskStatus.COMPLETED));
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

    public List<String> getDependencies() {
        return dependencies;
    }

    public String getTargetFile() {
        return targetFile;
    }

    public String getGeneratedCode() {
        return generatedCode;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
