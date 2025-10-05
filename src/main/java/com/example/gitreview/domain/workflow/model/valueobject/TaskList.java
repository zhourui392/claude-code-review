package com.example.gitreview.domain.workflow.model.valueobject;

import com.example.gitreview.domain.workflow.model.TaskStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 任务列表值对象
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
public class TaskList {

    private final String content;
    private final List<Task> tasks;
    private final LocalDateTime generatedAt;

    @JsonCreator
    public TaskList(@JsonProperty("content") String content,
                    @JsonProperty("tasks") List<Task> tasks,
                    @JsonProperty("generatedAt") LocalDateTime generatedAt) {
        this.content = content;
        this.tasks = tasks;
        this.generatedAt = generatedAt;
    }

    /**
     * 获取可执行的任务列表
     *
     * @return 可执行的任务列表
     */
    public List<Task> getExecutableTasks() {
        return tasks.stream()
                .filter(task -> task.isExecutable(tasks))
                .collect(Collectors.toList());
    }

    /**
     * 计算任务完成进度
     *
     * @return 完成进度百分比 (0-100)
     */
    public int getProgress() {
        if (tasks == null || tasks.isEmpty()) {
            return 0;
        }

        long completedCount = tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                .count();

        return (int) ((completedCount * 100) / tasks.size());
    }

    /**
     * 根据ID查找任务
     *
     * @param id 任务ID
     * @return 任务对象
     */
    public Optional<Task> getTaskById(String id) {
        return tasks.stream()
                .filter(task -> task.getId().equals(id))
                .findFirst();
    }

    public String getContent() {
        return content;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskList taskList = (TaskList) o;
        return Objects.equals(content, taskList.content) &&
                Objects.equals(tasks, taskList.tasks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, tasks);
    }
}
