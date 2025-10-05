package com.example.gitreview.domain.workflow.model.valueobject;

import com.example.gitreview.domain.workflow.model.TaskStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Task list value object.
 *
 * @author zhourui(V33215020)
 * @since 2025/10/05
 */
public class TaskList {
    private String content;
    private List<Task> tasks;
    private LocalDateTime generatedAt;

    // 默认构造函数供Jackson使用
    public TaskList() {
        this.tasks = new ArrayList<>();
    }

    public TaskList(List<Task> tasks, LocalDateTime generatedAt) {
        this.tasks = new ArrayList<>(tasks);
        this.generatedAt = generatedAt;
    }

    @JsonCreator
    public TaskList(
            @JsonProperty("content") String content,
            @JsonProperty("tasks") List<Task> tasks,
            @JsonProperty("generatedAt") LocalDateTime generatedAt) {
        this.content = content;
        this.tasks = tasks != null ? new ArrayList<>(tasks) : new ArrayList<>();
        this.generatedAt = generatedAt;
    }

    public String getContent() {
        return content;
    }

    public List<Task> getTasks() {
        return new ArrayList<>(tasks);
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public Optional<Task> getTaskById(String taskId) {
        return tasks.stream()
                .filter(task -> task.getId().equals(taskId))
                .findFirst();
    }

    public int getProgress() {
        if (tasks.isEmpty()) {
            return 0;
        }
        long completedCount = tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                .count();
        return (int) (completedCount * 100 / tasks.size());
    }

    /**
     * 获取所有可执行的任务（依赖已满足且状态为PENDING）
     *
     * @return 可执行的任务列表
     */
    public List<Task> getExecutableTasks() {
        Set<String> completedTaskIds = tasks.stream()
            .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
            .map(Task::getId)
            .collect(Collectors.toSet());

        return tasks.stream()
            .filter(task -> task.getStatus() == TaskStatus.PENDING)
            .filter(task -> completedTaskIds.containsAll(task.getDependencies()))
            .collect(Collectors.toList());
    }
}
