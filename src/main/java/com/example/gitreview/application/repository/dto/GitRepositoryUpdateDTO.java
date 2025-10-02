package com.example.gitreview.application.repository.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

/**
 * Git仓库更新请求DTO
 */
public class GitRepositoryUpdateDTO {

    @Size(min = 1, max = 100, message = "Repository name must be between 1 and 100 characters")
    @JsonProperty("name")
    private String name;

    @Size(min = 1, max = 500, message = "Repository URL must be between 1 and 500 characters")
    @JsonProperty("url")
    private String url;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    @JsonProperty("description")
    private String description;

    @Size(max = 100, message = "Username cannot exceed 100 characters")
    @JsonProperty("username")
    private String username;

    @Size(max = 100, message = "Password cannot exceed 100 characters")
    @JsonProperty("password")
    private String password;

    @JsonProperty("testConnection")
    private Boolean testConnection;

    @JsonProperty("tags")
    private String[] tags;

    // 默认构造函数
    public GitRepositoryUpdateDTO() {
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getTestConnection() {
        return testConnection;
    }

    public void setTestConnection(Boolean testConnection) {
        this.testConnection = testConnection;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "GitRepositoryUpdateDTO{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", description='" + description + '\'' +
                ", username='" + username + '\'' +
                ", testConnection=" + testConnection +
                '}';
    }
}