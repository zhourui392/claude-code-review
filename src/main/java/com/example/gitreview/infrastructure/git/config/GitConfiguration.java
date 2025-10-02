package com.example.gitreview.infrastructure.git.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Git配置类
 * 管理Git相关的配置参数
 */
@Configuration
@ConfigurationProperties(prefix = "git")
public class GitConfiguration {

    /**
     * 临时目录前缀
     */
    private String tempDirPrefix = "git-review-";

    /**
     * 克隆超时时间（秒）
     */
    private int cloneTimeoutSeconds = 300;

    /**
     * 是否启用克隆所有分支
     */
    private boolean cloneAllBranches = true;

    /**
     * 最大重试次数
     */
    private int maxRetries = 3;

    /**
     * 是否启用详细日志
     */
    private boolean enableVerboseLogging = false;

    // Getters and Setters
    public String getTempDirPrefix() {
        return tempDirPrefix;
    }

    public void setTempDirPrefix(String tempDirPrefix) {
        this.tempDirPrefix = tempDirPrefix;
    }

    public int getCloneTimeoutSeconds() {
        return cloneTimeoutSeconds;
    }

    public void setCloneTimeoutSeconds(int cloneTimeoutSeconds) {
        this.cloneTimeoutSeconds = cloneTimeoutSeconds;
    }

    public boolean isCloneAllBranches() {
        return cloneAllBranches;
    }

    public void setCloneAllBranches(boolean cloneAllBranches) {
        this.cloneAllBranches = cloneAllBranches;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public boolean isEnableVerboseLogging() {
        return enableVerboseLogging;
    }

    public void setEnableVerboseLogging(boolean enableVerboseLogging) {
        this.enableVerboseLogging = enableVerboseLogging;
    }
}