package com.example.gitreview.domain.codereview.model.valueobject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReviewResult 单元测试
 * 测试 Issue、Suggestion、ReviewMetrics 等功能
 *
 * @author zhourui(V33215020)
 * @since 2025/10/03
 */
class ReviewResultTest {

    // ========== Issue 测试 ==========

    @Test
    void testIssue_Creation() {
        ReviewResult.Issue issue = new ReviewResult.Issue(
                "PaymentService.java",
                123,
                ReviewResult.IssueSeverity.CRITICAL,
                ReviewResult.IssuePriority.P0,
                "安全问题",
                "SQL注入风险",
                "String sql = \"SELECT * FROM users WHERE id=\" + userId;",
                "可能导致数据泄露",
                new ReviewResult.FixSuggestion(
                        "直接拼接SQL",
                        "使用PreparedStatement",
                        "PreparedStatement stmt = conn.prepareStatement(\"SELECT * FROM users WHERE id = ?\");",
                        "单元测试验证",
                        10,
                        Arrays.asList("OWASP", "阿里巴巴规范")
                )
        );

        assertEquals("PaymentService.java", issue.getFilePath());
        assertEquals(123, issue.getLineNumber());
        assertEquals(ReviewResult.IssueSeverity.CRITICAL, issue.getSeverity());
        assertEquals(ReviewResult.IssuePriority.P0, issue.getPriority());
        assertEquals("安全问题", issue.getCategory());
        assertEquals("SQL注入风险", issue.getDescription());
        assertEquals("可能导致数据泄露", issue.getImpact());
        assertNotNull(issue.getCodeSnippet());
        assertNotNull(issue.getFixSuggestion());

        assertTrue(issue.isCritical());
        assertTrue(issue.isBlocking());
        assertTrue(issue.isHighPriority());
    }

    @Test
    void testIssue_NullFilePath() {
        assertThrows(Exception.class, () -> {
            new ReviewResult.Issue(
                    null,
                    1,
                    ReviewResult.IssueSeverity.MINOR,
                    ReviewResult.IssuePriority.P3,
                    "问题",
                    "描述",
                    null,
                    null,
                    null
            );
        });
    }

    @Test
    void testIssue_EmptyCategory() {
        assertThrows(Exception.class, () -> {
            new ReviewResult.Issue(
                    "test.java",
                    1,
                    ReviewResult.IssueSeverity.MINOR,
                    ReviewResult.IssuePriority.P3,
                    "",
                    "描述",
                    null,
                    null,
                    null
            );
        });
    }

    @Test
    void testIssue_EmptyDescription() {
        assertThrows(Exception.class, () -> {
            new ReviewResult.Issue(
                    "test.java",
                    1,
                    ReviewResult.IssueSeverity.MINOR,
                    ReviewResult.IssuePriority.P3,
                    "问题",
                    "",
                    null,
                    null,
                    null
            );
        });
    }

    @Test
    void testIssue_NegativeLineNumber() {
        ReviewResult.Issue issue = new ReviewResult.Issue(
                "test.java",
                -5,
                ReviewResult.IssueSeverity.MINOR,
                ReviewResult.IssuePriority.P3,
                "问题",
                "描述",
                null,
                null,
                null
        );

        assertEquals(0, issue.getLineNumber(), "负数行号应转为0");
    }

    @Test
    void testIssue_ToString() {
        ReviewResult.Issue issue = new ReviewResult.Issue(
                "test.java",
                10,
                ReviewResult.IssueSeverity.MAJOR,
                ReviewResult.IssuePriority.P1,
                "性能问题",
                "N+1查询",
                null,
                null,
                null
        );

        String str = issue.toString();
        assertTrue(str.contains("test.java"));
        assertTrue(str.contains("10"));
        assertTrue(str.contains("MAJOR"));
        assertTrue(str.contains("🟠")); // P1 emoji
    }

    // ========== Suggestion 测试 ==========

    @Test
    void testSuggestion_Creation() {
        ReviewResult.Suggestion suggestion = new ReviewResult.Suggestion(
                "测试覆盖",
                "建议增加单元测试",
                8
        );

        assertEquals("测试覆盖", suggestion.getCategory());
        assertEquals("建议增加单元测试", suggestion.getDescription());
        assertEquals(8, suggestion.getPriority());
        assertTrue(suggestion.isHighPriority());
    }

    @Test
    void testSuggestion_PriorityBounds() {
        ReviewResult.Suggestion lowPriority = new ReviewResult.Suggestion(
                "优化", "代码格式", -5
        );
        assertEquals(1, lowPriority.getPriority(), "低于1应转为1");

        ReviewResult.Suggestion highPriority = new ReviewResult.Suggestion(
                "重要", "架构调整", 20
        );
        assertEquals(10, highPriority.getPriority(), "高于10应转为10");
    }

    @Test
    void testSuggestion_HighPriorityThreshold() {
        ReviewResult.Suggestion lowPri = new ReviewResult.Suggestion("test", "test", 7);
        assertFalse(lowPri.isHighPriority());

        ReviewResult.Suggestion highPri = new ReviewResult.Suggestion("test", "test", 8);
        assertTrue(highPri.isHighPriority());
    }

    // ========== ReviewMetrics 测试 ==========

    @Test
    void testReviewMetrics_AllPriorities() {
        List<ReviewResult.Issue> issues = Arrays.asList(
                createIssue(ReviewResult.IssueSeverity.CRITICAL, ReviewResult.IssuePriority.P0),
                createIssue(ReviewResult.IssueSeverity.CRITICAL, ReviewResult.IssuePriority.P0),
                createIssue(ReviewResult.IssueSeverity.MAJOR, ReviewResult.IssuePriority.P1),
                createIssue(ReviewResult.IssueSeverity.MINOR, ReviewResult.IssuePriority.P2),
                createIssue(ReviewResult.IssueSeverity.INFO, ReviewResult.IssuePriority.P3)
        );

        List<ReviewResult.Suggestion> suggestions = Arrays.asList(
                new ReviewResult.Suggestion("测试", "增加单元测试", 9),
                new ReviewResult.Suggestion("文档", "补充注释", 5)
        );

        ReviewResult.ReviewMetrics metrics = new ReviewResult.ReviewMetrics(issues, suggestions);

        assertEquals(5, metrics.getTotalIssues());
        assertEquals(2, metrics.getCriticalIssues());
        assertEquals(2, metrics.getP0Issues());
        assertEquals(1, metrics.getP1Issues());
        assertEquals(1, metrics.getP2Issues());
        assertEquals(1, metrics.getP3Issues());
        assertEquals(2, metrics.getTotalSuggestions());
        assertEquals(1, metrics.getHighPrioritySuggestions());
        assertTrue(metrics.getCodeQualityIndex() < 10.0);
        assertTrue(metrics.getCodeQualityIndex() >= 0.0);
    }

    @Test
    void testReviewMetrics_Empty() {
        ReviewResult.ReviewMetrics metrics = new ReviewResult.ReviewMetrics(
                Collections.emptyList(),
                Collections.emptyList()
        );

        assertEquals(0, metrics.getTotalIssues());
        assertEquals(0, metrics.getCriticalIssues());
        assertEquals(0, metrics.getP0Issues());
        assertEquals(10.0, metrics.getCodeQualityIndex(), 0.01, "无问题时质量指数应为满分");
    }

    @Test
    void testReviewMetrics_QualityIndexCalculation() {
        // P0问题权重最高
        List<ReviewResult.Issue> p0Issues = Arrays.asList(
                createIssue(ReviewResult.IssueSeverity.CRITICAL, ReviewResult.IssuePriority.P0)
        );
        ReviewResult.ReviewMetrics metrics1 = new ReviewResult.ReviewMetrics(p0Issues, Collections.emptyList());

        // P3问题权重最低
        List<ReviewResult.Issue> p3Issues = Arrays.asList(
                createIssue(ReviewResult.IssueSeverity.INFO, ReviewResult.IssuePriority.P3)
        );
        ReviewResult.ReviewMetrics metrics2 = new ReviewResult.ReviewMetrics(p3Issues, Collections.emptyList());

        assertTrue(metrics1.getCodeQualityIndex() < metrics2.getCodeQualityIndex(),
                "P0问题应导致更低的质量指数");
    }

    @Test
    void testReviewMetrics_ToString() {
        List<ReviewResult.Issue> issues = Arrays.asList(
                createIssue(ReviewResult.IssueSeverity.CRITICAL, ReviewResult.IssuePriority.P0),
                createIssue(ReviewResult.IssueSeverity.MAJOR, ReviewResult.IssuePriority.P1)
        );

        ReviewResult.ReviewMetrics metrics = new ReviewResult.ReviewMetrics(issues, Collections.emptyList());
        String str = metrics.toString();

        assertTrue(str.contains("P0: 1"));
        assertTrue(str.contains("P1: 1"));
        assertTrue(str.contains("Quality"));
    }

    // ========== ReviewResult 测试 ==========

    @Test
    void testReviewResult_Creation() {
        List<ReviewResult.Issue> issues = Arrays.asList(
                createIssue(ReviewResult.IssueSeverity.CRITICAL, ReviewResult.IssuePriority.P0)
        );
        List<ReviewResult.Suggestion> suggestions = Arrays.asList(
                new ReviewResult.Suggestion("测试", "增加测试", 8)
        );

        ReviewResult result = ReviewResult.complete(
                "发现安全问题",
                "详细报告...",
                75,
                issues,
                suggestions
        );

        assertEquals("发现安全问题", result.getSummary());
        assertEquals(75, result.getQualityScore());
        assertEquals(1, result.getIssues().size());
        assertEquals(1, result.getSuggestions().size());
        assertTrue(result.hasCriticalIssues());
        assertFalse(result.isQualityAcceptable()); // 有P0问题
    }

    @Test
    void testReviewResult_Success() {
        ReviewResult result = ReviewResult.success(
                "审查通过",
                "无严重问题",
                95
        );

        assertEquals(95, result.getQualityScore());
        assertEquals(0, result.getIssues().size());
        assertFalse(result.hasCriticalIssues());
        assertTrue(result.isQualityAcceptable());
        assertEquals("优秀", result.getQualityGrade());
    }

    @Test
    void testReviewResult_QualityGrade() {
        assertEquals("优秀", createReviewResult(95).getQualityGrade());
        assertEquals("良好", createReviewResult(85).getQualityGrade());
        assertEquals("中等", createReviewResult(75).getQualityGrade());
        assertEquals("及格", createReviewResult(65).getQualityGrade());
        assertEquals("不及格", createReviewResult(50).getQualityGrade());
    }

    @Test
    void testReviewResult_GetIssuesByPriority() {
        List<ReviewResult.Issue> issues = Arrays.asList(
                createIssue(ReviewResult.IssueSeverity.CRITICAL, ReviewResult.IssuePriority.P0),
                createIssue(ReviewResult.IssueSeverity.MAJOR, ReviewResult.IssuePriority.P1),
                createIssue(ReviewResult.IssueSeverity.CRITICAL, ReviewResult.IssuePriority.P0),
                createIssue(ReviewResult.IssueSeverity.INFO, ReviewResult.IssuePriority.P3)
        );

        ReviewResult result = ReviewResult.withIssues(
                "测试",
                "",
                80,
                issues
        );

        List<ReviewResult.Issue> p0Issues = result.getIssuesByPriority(ReviewResult.IssuePriority.P0);
        assertEquals(2, p0Issues.size());

        List<ReviewResult.Issue> blockingIssues = result.getBlockingIssues();
        assertEquals(2, blockingIssues.size());

        List<ReviewResult.Issue> highPriorityIssues = result.getHighPriorityIssues();
        assertEquals(3, highPriorityIssues.size()); // P0 + P1
    }

    @Test
    void testReviewResult_Merge() {
        ReviewResult result1 = ReviewResult.withIssues(
                "结果1",
                "详情1",
                80,
                Arrays.asList(createIssue(ReviewResult.IssueSeverity.MAJOR, ReviewResult.IssuePriority.P1))
        );

        ReviewResult result2 = ReviewResult.withIssues(
                "结果2",
                "详情2",
                70,
                Arrays.asList(createIssue(ReviewResult.IssueSeverity.CRITICAL, ReviewResult.IssuePriority.P0))
        );

        ReviewResult merged = result1.merge(result2);

        assertEquals(2, merged.getIssues().size());
        assertTrue(merged.getSummary().contains("结果1"));
        assertTrue(merged.getSummary().contains("结果2"));
        assertEquals(75, merged.getQualityScore()); // 平均值
    }

    @Test
    void testReviewResult_WithError() {
        ReviewResult result = ReviewResult.success("成功", "", 90);
        ReviewResult errorResult = result.withError("解析失败");

        assertTrue(errorResult.getSummary().contains("成功"));
        assertTrue(errorResult.getSummary().contains("解析失败"));
        assertEquals(0, errorResult.getQualityScore());
    }

    @Test
    void testReviewResult_GenerateBriefReport() {
        List<ReviewResult.Issue> issues = Arrays.asList(
                createIssue(ReviewResult.IssueSeverity.CRITICAL, ReviewResult.IssuePriority.P0)
        );

        ReviewResult result = ReviewResult.withIssues("测试", "", 65, issues);
        String report = result.generateBriefReport();

        assertTrue(report.contains("65"));
        assertTrue(report.contains("及格"));
        assertTrue(report.contains("⚠️")); // 严重问题警告
    }

    @Test
    void testReviewResult_InvalidQualityScore() {
        assertThrows(Exception.class, () -> {
            ReviewResult.success("测试", "", -10);
        });

        assertThrows(Exception.class, () -> {
            ReviewResult.success("测试", "", 150);
        });
    }

    @Test
    void testReviewResult_InvalidSummary() {
        assertThrows(Exception.class, () -> {
            ReviewResult.success("", "", 80);
        });

        assertThrows(Exception.class, () -> {
            ReviewResult.success(null, "", 80);
        });
    }

    // ========== 辅助方法 ==========

    private ReviewResult.Issue createIssue(ReviewResult.IssueSeverity severity, ReviewResult.IssuePriority priority) {
        return new ReviewResult.Issue(
                "test.java",
                1,
                severity,
                priority,
                "测试问题",
                "问题描述",
                null,
                null,
                null
        );
    }

    private ReviewResult createReviewResult(int qualityScore) {
        return ReviewResult.success("测试", "", qualityScore);
    }
}
