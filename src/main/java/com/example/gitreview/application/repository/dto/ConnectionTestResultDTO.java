package com.example.gitreview.application.repository.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 连接测试结果DTO
 */
public class ConnectionTestResultDTO {

    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("message")
    private String message;

    @JsonProperty("duration")
    private Long duration;

    @JsonProperty("errorType")
    private String errorType;

    @JsonProperty("timestamp")
    private Long timestamp;

    // 默认构造函数
    public ConnectionTestResultDTO() {
        this.timestamp = System.currentTimeMillis();
    }

    public ConnectionTestResultDTO(Boolean success, String message, Long duration, String errorType) {
        this.success = success;
        this.message = message;
        this.duration = duration;
        this.errorType = errorType;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ConnectionTestResultDTO{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", duration=" + duration +
                ", errorType='" + errorType + '\'' +
                '}';
    }
}