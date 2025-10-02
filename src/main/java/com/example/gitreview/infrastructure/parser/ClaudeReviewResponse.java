package com.example.gitreview.infrastructure.parser;

import java.util.List;

/**
 * Claude 审查响应 DTO
 * 用于解析 Claude 返回的 JSON 结果
 */
public class ClaudeReviewResponse {
    public String summary;
    public int qualityScore;
    public String riskLevel;
    public List<ClaudeIssue> issues;
    public List<ClaudeSuggestion> suggestions;

    public static class ClaudeIssue {
        public String priority;       // "P0", "P1", "P2", "P3"
        public String severity;       // "CRITICAL", "MAJOR", "MINOR", "INFO"
        public String category;
        public String file;
        public int line;
        public String codeSnippet;
        public String description;
        public String impact;
        public ClaudeFixSuggestion fixSuggestion;
    }

    public static class ClaudeFixSuggestion {
        public String rootCause;
        public String fixApproach;
        public String codeExample;
        public String testStrategy;
        public int estimatedMinutes;
        public List<String> references;
    }

    public static class ClaudeSuggestion {
        public String category;
        public String description;
        public int priority;
        public String benefit;
    }
}
