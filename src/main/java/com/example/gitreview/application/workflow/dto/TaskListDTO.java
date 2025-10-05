package com.example.gitreview.application.workflow.dto;

import java.util.List;

/**
 * Task list DTO.
 *
 * @author zhourui(V33215020)
 * @since 2025/10/05
 */
public class TaskListDTO {
    private String content;
    private List<TaskDTO> tasks;

    public TaskListDTO() {}

    public TaskListDTO(String content, List<TaskDTO> tasks) {
        this.content = content;
        this.tasks = tasks;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<TaskDTO> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskDTO> tasks) {
        this.tasks = tasks;
    }
}
