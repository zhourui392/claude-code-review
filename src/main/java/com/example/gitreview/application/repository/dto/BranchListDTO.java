package com.example.gitreview.application.repository.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 分支列表DTO
 */
public class BranchListDTO {

    @JsonProperty("branches")
    private List<String> branches;

    @JsonProperty("count")
    private Integer count;

    @JsonProperty("fetchDuration")
    private Long fetchDuration;

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("defaultBranch")
    private String defaultBranch;

    // 默认构造函数
    public BranchListDTO() {
        this.timestamp = System.currentTimeMillis();
    }

    public BranchListDTO(List<String> branches, Integer count, Long fetchDuration) {
        this.branches = branches;
        this.count = count;
        this.fetchDuration = fetchDuration;
        this.timestamp = System.currentTimeMillis();

        // 尝试识别默认分支
        if (branches != null && !branches.isEmpty()) {
            this.defaultBranch = detectDefaultBranch(branches);
        }
    }

    /**
     * 检测默认分支
     */
    private String detectDefaultBranch(List<String> branches) {
        // 常见的默认分支名称
        String[] commonDefaults = {"main", "master", "develop", "dev"};

        for (String defaultName : commonDefaults) {
            if (branches.contains(defaultName)) {
                return defaultName;
            }
        }

        // 如果没有找到常见的默认分支，返回第一个
        return branches.get(0);
    }

    // Getters and Setters
    public List<String> getBranches() {
        return branches;
    }

    public void setBranches(List<String> branches) {
        this.branches = branches;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Long getFetchDuration() {
        return fetchDuration;
    }

    public void setFetchDuration(Long fetchDuration) {
        this.fetchDuration = fetchDuration;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    @Override
    public String toString() {
        return "BranchListDTO{" +
                "count=" + count +
                ", fetchDuration=" + fetchDuration +
                ", defaultBranch='" + defaultBranch + '\'' +
                '}';
    }
}