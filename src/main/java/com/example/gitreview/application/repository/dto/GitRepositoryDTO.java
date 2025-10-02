package com.example.gitreview.application.repository.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * Git仓库信息DTO
 */
public class GitRepositoryDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("url")
    private String url;

    @JsonProperty("description")
    private String description;

    @JsonProperty("username")
    private String username;

    @JsonProperty("encryptedPassword")
    private String encryptedPassword;

    @JsonProperty("hasCredentials")
    private Boolean hasCredentials;

    @JsonProperty("status")
    private String status;

    @JsonProperty("statusMessage")
    private String statusMessage;

    @JsonProperty("createTime")
    private LocalDateTime createTime;

    @JsonProperty("updateTime")
    private LocalDateTime updateTime;

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("lastAccessTime")
    private LocalDateTime lastAccessTime;

    @JsonProperty("accessCount")
    private Integer accessCount;

    @JsonProperty("tags")
    private String[] tags;

    @JsonProperty("isHealthy")
    private Boolean isHealthy;

    @JsonProperty("lastHealthCheck")
    private LocalDateTime lastHealthCheck;

    // 统计信息
    @JsonProperty("reviewCount")
    private Integer reviewCount;

    @JsonProperty("testSuiteCount")
    private Integer testSuiteCount;

    // 默认构造函数
    public GitRepositoryDTO() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public Boolean getHasCredentials() {
        return hasCredentials;
    }

    public void setHasCredentials(Boolean hasCredentials) {
        this.hasCredentials = hasCredentials;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(LocalDateTime lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public Integer getAccessCount() {
        return accessCount;
    }

    public void setAccessCount(Integer accessCount) {
        this.accessCount = accessCount;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public Boolean getIsHealthy() {
        return isHealthy;
    }

    public void setIsHealthy(Boolean isHealthy) {
        this.isHealthy = isHealthy;
    }

    public LocalDateTime getLastHealthCheck() {
        return lastHealthCheck;
    }

    public void setLastHealthCheck(LocalDateTime lastHealthCheck) {
        this.lastHealthCheck = lastHealthCheck;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public Integer getTestSuiteCount() {
        return testSuiteCount;
    }

    public void setTestSuiteCount(Integer testSuiteCount) {
        this.testSuiteCount = testSuiteCount;
    }

    @Override
    public String toString() {
        return "GitRepositoryDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", status='" + status + '\'' +
                ", isHealthy=" + isHealthy +
                ", createdBy='" + createdBy + '\'' +
                '}';
    }
}