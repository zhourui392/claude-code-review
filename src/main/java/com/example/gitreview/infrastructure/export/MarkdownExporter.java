package com.example.gitreview.infrastructure.export;

import com.example.gitreview.domain.codereview.model.aggregate.CodeReview;
import com.example.gitreview.domain.codereview.model.valueobject.ReviewResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Markdown报告导出器
 * 将代码审查结果导出为Markdown格式
 *
 * @author zhourui(V33215020)
 * @since 2025/10/03
 */
@Component
public class MarkdownExporter {

    private static final Logger logger = LoggerFactory.getLogger(MarkdownExporter.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String template;

    public MarkdownExporter() {
        loadTemplate();
    }

    /**
     * 导出代码审查结果为Markdown格式
     *
     * @param review 代码审查对象
     * @param repositoryName 仓库名称
     * @param baseBranch 基础分支
     * @param targetBranch 目标分支
     * @param reviewMode 审查模式
     * @return Markdown格式的报告
     */
    public String export(CodeReview review, String repositoryName, String baseBranch,
                        String targetBranch, String reviewMode) {
        logger.info("Exporting review {} to Markdown", review.getId());

        ReviewResult result = review.getFinalResult();
        if (result == null) {
            return "# 审查未完成\n\n审查尚未完成或失败，暂无结果可导出。";
        }

        String markdown = template;

        // 基本信息替换
        markdown = replacePlaceholder(markdown, "repositoryName", repositoryName);
        markdown = replacePlaceholder(markdown, "baseBranch", baseBranch);
        markdown = replacePlaceholder(markdown, "targetBranch", targetBranch);
        markdown = replacePlaceholder(markdown, "reviewTime", formatDateTime(review.getUpdateTime()));
        markdown = replacePlaceholder(markdown, "reviewMode", reviewMode);
        markdown = replacePlaceholder(markdown, "qualityScore", String.valueOf(result.getQualityScore()));
        markdown = replacePlaceholder(markdown, "riskLevel", calculateRiskLevel(result));
        markdown = replacePlaceholder(markdown, "summary", result.getSummary());
        markdown = replacePlaceholder(markdown, "generatedAt", formatDateTime(LocalDateTime.now()));

        // 问题统计
        List<ReviewResult.Issue> p0Issues = filterByPriority(result.getIssues(), ReviewResult.IssuePriority.P0);
        List<ReviewResult.Issue> p1Issues = filterByPriority(result.getIssues(), ReviewResult.IssuePriority.P1);
        List<ReviewResult.Issue> p2Issues = filterByPriority(result.getIssues(), ReviewResult.IssuePriority.P2);
        List<ReviewResult.Issue> p3Issues = filterByPriority(result.getIssues(), ReviewResult.IssuePriority.P3);

        markdown = replacePlaceholder(markdown, "p0Count", String.valueOf(p0Issues.size()));
        markdown = replacePlaceholder(markdown, "p1Count", String.valueOf(p1Issues.size()));
        markdown = replacePlaceholder(markdown, "p2Count", String.valueOf(p2Issues.size()));
        markdown = replacePlaceholder(markdown, "p3Count", String.valueOf(p3Issues.size()));
        markdown = replacePlaceholder(markdown, "totalIssues", String.valueOf(result.getIssues().size()));
        markdown = replacePlaceholder(markdown, "hasCriticalIssues", result.hasCriticalIssues() ? "是" : "否");

        // 处理条件块和问题列表
        markdown = processConditionalBlock(markdown, "hasP0Issues", !p0Issues.isEmpty());
        markdown = processConditionalBlock(markdown, "hasP1Issues", !p1Issues.isEmpty());
        markdown = processConditionalBlock(markdown, "hasP2Issues", !p2Issues.isEmpty());
        markdown = processConditionalBlock(markdown, "hasP3Issues", !p3Issues.isEmpty());
        markdown = processConditionalBlock(markdown, "hasSuggestions", !result.getSuggestions().isEmpty());

        // 填充问题列表
        markdown = fillIssuesList(markdown, "p0Issues", p0Issues);
        markdown = fillIssuesList(markdown, "p1Issues", p1Issues);
        markdown = fillIssuesList(markdown, "p2Issues", p2Issues);
        markdown = fillIssuesList(markdown, "p3Issues", p3Issues);

        // 填充建议列表
        markdown = fillSuggestionsList(markdown, result.getSuggestions());

        return markdown;
    }

    private void loadTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource("templates/review-report-template.md");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                template = reader.lines().collect(Collectors.joining("\n"));
            }
            logger.info("Loaded Markdown template successfully");
        } catch (IOException e) {
            logger.error("Failed to load Markdown template", e);
            template = "# 模板加载失败\n\n无法加载Markdown模板文件。";
        }
    }

    private String replacePlaceholder(String content, String key, String value) {
        return content.replace("{{" + key + "}}", value != null ? value : "");
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_FORMATTER) : "";
    }

    private List<ReviewResult.Issue> filterByPriority(List<ReviewResult.Issue> issues,
                                                      ReviewResult.IssuePriority priority) {
        return issues.stream()
                .filter(issue -> issue.getPriority() == priority)
                .collect(Collectors.toList());
    }

    private String processConditionalBlock(String content, String blockName, boolean condition) {
        String startTag = "{{#" + blockName + "}}";
        String endTag = "{{/" + blockName + "}}";

        int startIndex = content.indexOf(startTag);
        if (startIndex == -1) {
            return content;
        }

        int endIndex = content.indexOf(endTag, startIndex);
        if (endIndex == -1) {
            return content;
        }

        if (condition) {
            // 保留块内容，移除标签
            String before = content.substring(0, startIndex);
            String blockContent = content.substring(startIndex + startTag.length(), endIndex);
            String after = content.substring(endIndex + endTag.length());
            return before + blockContent + after;
        } else {
            // 移除整个块
            String before = content.substring(0, startIndex);
            String after = content.substring(endIndex + endTag.length());
            return before + after;
        }
    }

    private String fillIssuesList(String content, String listName, List<ReviewResult.Issue> issues) {
        String startTag = "{{#" + listName + "}}";
        String endTag = "{{/" + listName + "}}";

        int startIndex = content.indexOf(startTag);
        if (startIndex == -1) {
            return content;
        }

        int endIndex = content.indexOf(endTag, startIndex);
        if (endIndex == -1) {
            return content;
        }

        String before = content.substring(0, startIndex);
        String itemTemplate = content.substring(startIndex + startTag.length(), endIndex);
        String after = content.substring(endIndex + endTag.length());

        if (issues.isEmpty()) {
            return before + after;
        }

        StringBuilder issuesContent = new StringBuilder();
        for (int i = 0; i < issues.size(); i++) {
            ReviewResult.Issue issue = issues.get(i);
            String issueBlock = fillIssueTemplate(itemTemplate, issue, i + 1);
            issuesContent.append(issueBlock);
        }

        return before + issuesContent.toString() + after;
    }

    private String fillIssueTemplate(String template, ReviewResult.Issue issue, int index) {
        String result = template;
        result = replacePlaceholder(result, "index", String.valueOf(index));
        result = replacePlaceholder(result, "description", issue.getDescription());
        result = replacePlaceholder(result, "file", issue.getFilePath());
        result = replacePlaceholder(result, "line", String.valueOf(issue.getLineNumber()));
        result = replacePlaceholder(result, "category", issue.getCategory());
        result = replacePlaceholder(result, "severity", issue.getSeverity().name());
        result = replacePlaceholder(result, "impact", issue.getImpact() != null ? issue.getImpact() : "未说明");
        result = replacePlaceholder(result, "language", detectLanguage(issue.getFilePath()));

        // 代码片段处理
        if (issue.getCodeSnippet() != null && !issue.getCodeSnippet().isEmpty()) {
            result = replacePlaceholder(result, "codeSnippet", issue.getCodeSnippet());
        } else {
            result = processConditionalBlock(result, "codeSnippet", false);
        }

        // 修复建议
        ReviewResult.FixSuggestion fix = issue.getFixSuggestion();
        if (fix != null) {
            result = replacePlaceholder(result, "fixSuggestion.rootCause", fix.getRootCause());
            result = replacePlaceholder(result, "fixSuggestion.fixApproach", fix.getFixApproach());
            result = replacePlaceholder(result, "fixSuggestion.codeExample", fix.getCodeExample());
            result = replacePlaceholder(result, "fixSuggestion.testStrategy", fix.getTestStrategy());
            result = replacePlaceholder(result, "fixSuggestion.estimatedMinutes",
                    String.valueOf(fix.getEstimatedMinutes()));

            // 参考资料
            if (fix.getReferences() != null && !fix.getReferences().isEmpty()) {
                String references = String.join(", ", fix.getReferences());
                result = replacePlaceholder(result, "fixSuggestion.references", references);
            }
        }

        return result;
    }

    private String fillSuggestionsList(String content, List<ReviewResult.Suggestion> suggestions) {
        String startTag = "{{#suggestions}}";
        String endTag = "{{/suggestions}}";

        int startIndex = content.indexOf(startTag);
        if (startIndex == -1) {
            return content;
        }

        int endIndex = content.indexOf(endTag, startIndex);
        if (endIndex == -1) {
            return content;
        }

        String before = content.substring(0, startIndex);
        String itemTemplate = content.substring(startIndex + startTag.length(), endIndex);
        String after = content.substring(endIndex + endTag.length());

        if (suggestions.isEmpty()) {
            return before + after;
        }

        StringBuilder suggestionsContent = new StringBuilder();
        for (int i = 0; i < suggestions.size(); i++) {
            ReviewResult.Suggestion suggestion = suggestions.get(i);
            String suggestionBlock = itemTemplate;
            suggestionBlock = replacePlaceholder(suggestionBlock, "index", String.valueOf(i + 1));
            suggestionBlock = replacePlaceholder(suggestionBlock, "category", suggestion.getCategory());
            suggestionBlock = replacePlaceholder(suggestionBlock, "description", suggestion.getDescription());
            suggestionBlock = replacePlaceholder(suggestionBlock, "priority", String.valueOf(suggestion.getPriority()));
            // benefit字段不存在，用priority的描述代替
            String benefit = suggestion.getPriority() >= 8 ? "高优先级，建议尽快处理" :
                           suggestion.getPriority() >= 5 ? "中等优先级，可适当安排" :
                           "低优先级，可延后处理";
            suggestionBlock = replacePlaceholder(suggestionBlock, "benefit", benefit);
            suggestionsContent.append(suggestionBlock);
        }

        return before + suggestionsContent.toString() + after;
    }

    private String detectLanguage(String filename) {
        if (filename == null) {
            return "java";
        }
        if (filename.endsWith(".java")) {
            return "java";
        } else if (filename.endsWith(".js") || filename.endsWith(".jsx")) {
            return "javascript";
        } else if (filename.endsWith(".ts") || filename.endsWith(".tsx")) {
            return "typescript";
        } else if (filename.endsWith(".py")) {
            return "python";
        } else if (filename.endsWith(".xml")) {
            return "xml";
        } else if (filename.endsWith(".yml") || filename.endsWith(".yaml")) {
            return "yaml";
        } else if (filename.endsWith(".json")) {
            return "json";
        }
        return "java";
    }

    /**
     * 根据问题优先级计算风险等级
     */
    private String calculateRiskLevel(ReviewResult result) {
        long p0Count = result.getIssues().stream()
                .filter(issue -> issue.getPriority() == ReviewResult.IssuePriority.P0)
                .count();
        long p1Count = result.getIssues().stream()
                .filter(issue -> issue.getPriority() == ReviewResult.IssuePriority.P1)
                .count();

        if (p0Count > 0 || p1Count >= 3) {
            return "high";
        } else if (p1Count > 0) {
            return "medium";
        } else {
            return "low";
        }
    }
}
