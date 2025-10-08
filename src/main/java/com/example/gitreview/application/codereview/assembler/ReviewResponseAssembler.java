package com.example.gitreview.application.codereview.assembler;

import com.example.gitreview.application.codereview.dto.request.CodeReviewRequest;
import com.example.gitreview.application.codereview.dto.response.CodeReviewResponse;
import com.example.gitreview.domain.codereview.model.aggregate.CodeReview;
import com.example.gitreview.domain.codereview.model.valueobject.CodeDiff;
import com.example.gitreview.domain.codereview.model.valueobject.ReviewResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 代码审查对象组装器
 * 负责DTO与领域对象之间的转换
 */
@Component
public class ReviewResponseAssembler {

    /**
     * 将CodeReview领域对象转换为响应DTO
     * @param codeReview 代码审查领域对象
     * @return 代码审查响应DTO
     */
    public CodeReviewResponse toResponse(CodeReview codeReview) {
        if (codeReview == null) {
            return null;
        }

        CodeReviewResponse response = new CodeReviewResponse();

        // 基本信息
        response.setId(codeReview.getId());
        response.setRepositoryId(codeReview.getRepositoryId());
        response.setTitle(codeReview.getTitle());
        response.setDescription(codeReview.getDescription());
        response.setStatus(codeReview.getStatus().name());
        response.setStatusDescription(getStatusDescription(codeReview.getStatus()));
        response.setProgress(codeReview.getProgress());
        response.setQualityScore(codeReview.getQualityScore());
        response.setHasCriticalIssues(codeReview.hasCriticalIssues());
        response.setSummary(codeReview.getSummary());
        response.setCreateTime(codeReview.getCreateTime());
        response.setUpdateTime(codeReview.getUpdateTime());
        response.setCreatedBy(codeReview.getCreatedBy());

        // 策略信息
        if (codeReview.getStrategy() != null) {
            response.setStrategyMode(codeReview.getStrategy().getMode().getCode());
            response.setStrategyDescription(codeReview.getStrategy().getFocusDescription());
        }

        // 代码差异信息
        if (codeReview.getCodeDiff() != null) {
            CodeDiff codeDiff = codeReview.getCodeDiff();
            response.setBaseBranch(codeDiff.getBaseBranch());
            response.setTargetBranch(codeDiff.getTargetBranch());
            response.setTotalFiles(codeDiff.getStats().getTotalFiles());
            response.setAddedLines(codeDiff.getStats().getAddedLines());
            response.setDeletedLines(codeDiff.getStats().getDeletedLines());
        }

        // 审查结果信息
        if (codeReview.getFinalResult() != null) {
            ReviewResult result = codeReview.getFinalResult();
            response.setIssues(convertIssues(result.getIssues()));
            response.setSuggestions(convertSuggestions(result.getSuggestions()));
        }

        return response;
    }

    /**
     * 批量转换CodeReview列表
     * @param codeReviews 代码审查列表
     * @return 响应DTO列表
     */
    public List<CodeReviewResponse> toResponseList(List<CodeReview> codeReviews) {
        if (codeReviews == null) {
            return null;
        }

        return codeReviews.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 创建简要响应（不包含详细的问题和建议）
     * @param codeReview 代码审查领域对象
     * @return 简要响应DTO
     */
    public CodeReviewResponse toBriefResponse(CodeReview codeReview) {
        CodeReviewResponse response = toResponse(codeReview);
        if (response != null) {
            // 清除详细信息以减少响应大小
            response.setIssues(null);
            response.setSuggestions(null);
        }
        return response;
    }

    /**
     * 批量创建简要响应列表
     * @param codeReviews 代码审查列表
     * @return 简要响应DTO列表
     */
    public List<CodeReviewResponse> toBriefResponseList(List<CodeReview> codeReviews) {
        if (codeReviews == null) {
            return null;
        }

        return codeReviews.stream()
                .map(this::toBriefResponse)
                .collect(Collectors.toList());
    }

    /**
     * 从请求DTO提取基本信息（用于验证和日志）
     * @param request 请求DTO
     * @return 格式化的请求信息
     */
    public String extractRequestInfo(CodeReviewRequest request) {
        if (request == null) {
            return "null request";
        }

        return String.format("Repository: %d, Branches: %s->%s, Strategy: %s, Creator: %s",
                           request.getRepositoryId(),
                           request.getBaseBranch(),
                           request.getTargetBranch(),
                           request.getStrategyMode(),
                           request.getCreatedBy());
    }

    // 私有辅助方法

    /**
     * 获取状态描述
     */
    private String getStatusDescription(CodeReview.ReviewStatus status) {
        switch (status) {
            case PENDING:
                return "等待开始";
            case IN_PROGRESS:
                return "审查中";
            case COMPLETED:
                return "已完成";
            case FAILED:
                return "审查失败";
            case CANCELLED:
                return "已取消";
            default:
                return "未知状态";
        }
    }

    /**
     * 转换问题列表
     */
    private List<CodeReviewResponse.IssueInfo> convertIssues(List<ReviewResult.Issue> issues) {
        if (issues == null) {
            return null;
        }

        return issues.stream()
                .map(this::convertIssue)
                .collect(Collectors.toList());
    }

    /**
     * 转换单个问题
     */
    private CodeReviewResponse.IssueInfo convertIssue(ReviewResult.Issue issue) {
        String fixApproach = issue.getFixSuggestion() != null
            ? issue.getFixSuggestion().getFixApproach()
            : null;
        return new CodeReviewResponse.IssueInfo(
            issue.getFilePath(),
            issue.getLineNumber(),
            issue.getSeverity().name(),
            issue.getCategory(),
            issue.getDescription(),
            fixApproach
        );
    }

    /**
     * 转换建议列表
     */
    private List<CodeReviewResponse.SuggestionInfo> convertSuggestions(List<ReviewResult.Suggestion> suggestions) {
        if (suggestions == null) {
            return null;
        }

        return suggestions.stream()
                .map(this::convertSuggestion)
                .collect(Collectors.toList());
    }

    /**
     * 转换单个建议
     */
    private CodeReviewResponse.SuggestionInfo convertSuggestion(ReviewResult.Suggestion suggestion) {
        return new CodeReviewResponse.SuggestionInfo(
            suggestion.getCategory(),
            suggestion.getDescription(),
            suggestion.getPriority()
        );
    }
}