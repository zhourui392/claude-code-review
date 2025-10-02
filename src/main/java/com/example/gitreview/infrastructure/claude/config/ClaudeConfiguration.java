package com.example.gitreview.infrastructure.claude.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Claude配置类
 * 管理Claude AI相关的配置参数
 */
@Configuration
@ConfigurationProperties(prefix = "claude")
public class ClaudeConfiguration {

    /**
     * Claude CLI命令
     */
    private String command = "claude";

    /**
     * 超时时间（毫秒）
     */
    private long timeoutMs = 30000;

    /**
     * 最大重试次数
     */
    private int maxRetries = 3;

    /**
     * SDK超时时间（分钟）
     */
    private int sdkTimeoutMinutes = 10;

    /**
     * 是否启用CLI模式
     */
    private boolean cliEnabled = true;

    /**
     * 是否启用详细日志
     */
    private boolean enableVerboseLogging = false;

    // Getters and Setters
    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getSdkTimeoutMinutes() {
        return sdkTimeoutMinutes;
    }

    public void setSdkTimeoutMinutes(int sdkTimeoutMinutes) {
        this.sdkTimeoutMinutes = sdkTimeoutMinutes;
    }

    public boolean isCliEnabled() {
        return cliEnabled;
    }

    public void setCliEnabled(boolean cliEnabled) {
        this.cliEnabled = cliEnabled;
    }

    public boolean isEnableVerboseLogging() {
        return enableVerboseLogging;
    }

    public void setEnableVerboseLogging(boolean enableVerboseLogging) {
        this.enableVerboseLogging = enableVerboseLogging;
    }
}