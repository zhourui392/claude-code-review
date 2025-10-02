package com.example.gitreview.infrastructure.parser;

import com.example.gitreview.domain.codereview.model.valueobject.ReviewResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Claude 审查结果解析器
 * 支持 JSON 和 Markdown 两种格式解析
 */
@Component
public class ReviewResultParser {

    private static final Logger logger = LoggerFactory.getLogger(ReviewResultParser.class);

    private final Gson gson;

    public ReviewResultParser() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    /**
     * 解析 Claude 返回的审查结果
     * @param claudeResponse Claude 原始响应
     * @return ReviewResult
     */
    public ReviewResult parse(String claudeResponse) {
        if (claudeResponse == null || claudeResponse.trim().isEmpty()) {
            return createErrorResult("Claude 响应为空");
        }

        // 1. 尝试 JSON 解析（优先）
        String jsonContent = extractJsonBlock(claudeResponse);
        if (jsonContent != null) {
            try {
                return parseJson(jsonContent);
            } catch (Exception e) {
                logger.warn("JSON 解析失败: {}", e.getMessage());
            }
        }

        // 2. 回退到 Markdown 解析
        try {
            return parseMarkdown(claudeResponse);
        } catch (Exception e) {
            logger.error("Markdown 解析失败", e);
        }

        // 3. 兜底：返回原始文本作为摘要
        return createErrorResult("解析失败，原始响应：\n" + claudeResponse);
    }

    /**
     * 提取 JSON 块
     */
    private String extractJsonBlock(String response) {
        // 匹配 ```json ... ``` 格式
        Pattern jsonBlockPattern = Pattern.compile("```json\\s*(.+?)\\s*```", Pattern.DOTALL);
        Matcher matcher = jsonBlockPattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // 匹配 { ... } 格式（完整 JSON 对象）
        Pattern jsonObjectPattern = Pattern.compile("\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}", Pattern.DOTALL);
        matcher = jsonObjectPattern.matcher(response);
        if (matcher.find()) {
            String json = matcher.group();
            // 验证是否包含必要字段
            if (json.contains("\"summary\"") || json.contains("\"issues\"")) {
                return json;
            }
        }

        return null;
    }

    /**
     * 解析 JSON 格式
     */
    private ReviewResult parseJson(String json) throws JsonSyntaxException {
        ClaudeReviewResponse response = gson.fromJson(json, ClaudeReviewResponse.class);

        if (response == null) {
            throw new JsonSyntaxException("解析结果为 null");
        }

        // 转换为领域模型
        List<ReviewResult.Issue> issues = convertIssues(response.issues);
        List<ReviewResult.Suggestion> suggestions = convertSuggestions(response.suggestions);

        String summary = response.summary != null ? response.summary : "审查完成";
        int qualityScore = Math.max(0, Math.min(100, response.qualityScore));

        return ReviewResult.complete(summary, "", qualityScore, issues, suggestions);
    }

    /**
     * 转换 Issue 列表
     */
    private List<ReviewResult.Issue> convertIssues(List<ClaudeReviewResponse.ClaudeIssue> claudeIssues) {
        if (claudeIssues == null || claudeIssues.isEmpty()) {
            return Collections.emptyList();
        }

        return claudeIssues.stream()
                .map(this::convertToIssue)
                .collect(Collectors.toList());
    }

    /**
     * 转换单个 Issue
     */
    private ReviewResult.Issue convertToIssue(ClaudeReviewResponse.ClaudeIssue claudeIssue) {
        // 解析优先级
        ReviewResult.IssuePriority priority = parsePriority(claudeIssue.priority);

        // 解析严重程度
        ReviewResult.IssueSeverity severity = parseSeverity(claudeIssue.severity);

        // 转换修复建议
        ReviewResult.FixSuggestion fixSuggestion = null;
        if (claudeIssue.fixSuggestion != null) {
            fixSuggestion = new ReviewResult.FixSuggestion(
                    claudeIssue.fixSuggestion.rootCause,
                    claudeIssue.fixSuggestion.fixApproach,
                    claudeIssue.fixSuggestion.codeExample,
                    claudeIssue.fixSuggestion.testStrategy,
                    claudeIssue.fixSuggestion.estimatedMinutes,
                    claudeIssue.fixSuggestion.references
            );
        }

        return new ReviewResult.Issue(
                claudeIssue.file != null ? claudeIssue.file : "unknown",
                claudeIssue.line,
                severity,
                priority,
                claudeIssue.category != null ? claudeIssue.category : "其他",
                claudeIssue.description != null ? claudeIssue.description : "无描述",
                claudeIssue.codeSnippet,
                claudeIssue.impact,
                fixSuggestion
        );
    }

    /**
     * 转换 Suggestion 列表
     */
    private List<ReviewResult.Suggestion> convertSuggestions(
            List<ClaudeReviewResponse.ClaudeSuggestion> claudeSuggestions) {
        if (claudeSuggestions == null || claudeSuggestions.isEmpty()) {
            return Collections.emptyList();
        }

        return claudeSuggestions.stream()
                .map(s -> new ReviewResult.Suggestion(
                        s.category != null ? s.category : "优化建议",
                        s.description != null ? s.description : "",
                        s.priority > 0 ? s.priority : 5
                ))
                .collect(Collectors.toList());
    }

    /**
     * 解析优先级
     */
    private ReviewResult.IssuePriority parsePriority(String priority) {
        if (priority == null) {
            return ReviewResult.IssuePriority.P2; // 默认 P2
        }

        try {
            return ReviewResult.IssuePriority.fromCode(priority);
        } catch (Exception e) {
            logger.warn("无法解析优先级: {}, 使用默认值 P2", priority);
            return ReviewResult.IssuePriority.P2;
        }
    }

    /**
     * 解析严重程度
     */
    private ReviewResult.IssueSeverity parseSeverity(String severity) {
        if (severity == null) {
            return ReviewResult.IssueSeverity.MINOR; // 默认 MINOR
        }

        try {
            return ReviewResult.IssueSeverity.valueOf(severity.toUpperCase());
        } catch (Exception e) {
            logger.warn("无法解析严重程度: {}, 使用默认值 MINOR", severity);
            return ReviewResult.IssueSeverity.MINOR;
        }
    }

    /**
     * 解析 Markdown 格式（回退方案）
     */
    private ReviewResult parseMarkdown(String markdown) {
        List<ReviewResult.Issue> issues = new ArrayList<>();
        List<ReviewResult.Suggestion> suggestions = new ArrayList<>();

        // 简单的正则提取问题
        // 匹配格式: - [P0] 或 🔴 [P0] 或 ❌ [严重]
        Pattern issuePattern = Pattern.compile(
                "[-•]\\s*(?:[🔴🟠🟡⚪]\\s*)?\\[?([P0-3]|严重|警告|建议)\\]?\\s*(.+?)\\((.+?):(\\d+)\\)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = issuePattern.matcher(markdown);
        while (matcher.find()) {
            String level = matcher.group(1);
            String description = matcher.group(2).trim();
            String file = matcher.group(3).trim();
            int line = Integer.parseInt(matcher.group(4));

            ReviewResult.IssuePriority priority = mapLevelToPriority(level);
            ReviewResult.IssueSeverity severity = mapPriorityToSeverity(priority);

            issues.add(new ReviewResult.Issue(
                    file, line, severity, priority,
                    "代码问题", description, null, null, null
            ));
        }

        // 提取摘要
        String summary = extractSummary(markdown);

        // 估算质量分数
        int qualityScore = estimateQualityScore(issues.size());

        return ReviewResult.complete(summary, markdown, qualityScore, issues, suggestions);
    }

    /**
     * 映射级别到优先级
     */
    private ReviewResult.IssuePriority mapLevelToPriority(String level) {
        if (level.matches("P0|严重")) {
            return ReviewResult.IssuePriority.P0;
        } else if (level.matches("P1|警告")) {
            return ReviewResult.IssuePriority.P1;
        } else if (level.matches("P2")) {
            return ReviewResult.IssuePriority.P2;
        } else {
            return ReviewResult.IssuePriority.P3;
        }
    }

    /**
     * 映射优先级到严重程度
     */
    private ReviewResult.IssueSeverity mapPriorityToSeverity(ReviewResult.IssuePriority priority) {
        switch (priority) {
            case P0:
                return ReviewResult.IssueSeverity.CRITICAL;
            case P1:
                return ReviewResult.IssueSeverity.MAJOR;
            case P2:
                return ReviewResult.IssueSeverity.MINOR;
            default:
                return ReviewResult.IssueSeverity.INFO;
        }
    }

    /**
     * 提取摘要
     */
    private String extractSummary(String markdown) {
        // 尝试提取第一行或第一段
        String[] lines = markdown.split("\\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("#") && !line.startsWith("-")) {
                return line.length() > 100 ? line.substring(0, 100) + "..." : line;
            }
        }
        return "代码审查完成";
    }

    /**
     * 估算质量分数
     */
    private int estimateQualityScore(int issueCount) {
        if (issueCount == 0) {
            return 95;
        } else if (issueCount <= 2) {
            return 85;
        } else if (issueCount <= 5) {
            return 75;
        } else if (issueCount <= 10) {
            return 65;
        } else {
            return 50;
        }
    }

    /**
     * 创建错误结果
     */
    private ReviewResult createErrorResult(String errorMessage) {
        return ReviewResult.success(
                "审查失败",
                errorMessage,
                0
        );
    }
}
