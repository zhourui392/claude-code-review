package com.example.gitreview.domain.codereview.service;

import com.example.gitreview.domain.codereview.model.valueobject.ReviewResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CodeReviewDomainService 单元测试
 * 测试 P0-P3 优先级映射逻辑
 */
class CodeReviewDomainServiceTest {

    private CodeReviewDomainService domainService;

    @BeforeEach
    void setUp() {
        domainService = new CodeReviewDomainService();
    }

    // ========== P0 优先级测试 ==========

    @Test
    void testCalculateIssuePriority_P0_SecurityCritical() {
        ReviewResult.IssuePriority priority = domainService.calculateIssuePriority(
                ReviewResult.IssueSeverity.CRITICAL,
                "安全问题",
                "SQL注入漏洞",
                "PaymentService.java"
        );

        assertEquals(ReviewResult.IssuePriority.P0, priority);
        assertTrue(priority.isBlocking());
        assertTrue(priority.isCritical());
    }

    @Test
    void testCalculateIssuePriority_P0_DataLoss() {
        ReviewResult.IssuePriority priority = domainService.calculateIssuePriority(
                ReviewResult.IssueSeverity.CRITICAL,
                "数据问题",
                "可能导致数据丢失",
                "UserRepository.java"
        );

        assertEquals(ReviewResult.IssuePriority.P0, priority);
    }

    @Test
    void testCalculateIssuePriority_P0_CoreBusiness() {
        ReviewResult.IssuePriority priority = domainService.calculateIssuePriority(
                ReviewResult.IssueSeverity.CRITICAL,
                "逻辑错误",
                "订单状态流转错误",
                "OrderService.java"
        );

        assertEquals(ReviewResult.IssuePriority.P0, priority);
    }

    // ========== P1 优先级测试 ==========

    @Test
    void testCalculateIssuePriority_P1_CriticalNonCore() {
        ReviewResult.IssuePriority priority = domainService.calculateIssuePriority(
                ReviewResult.IssueSeverity.CRITICAL,
                "逻辑错误",
                "非核心功能异常",
                "HelperService.java"
        );

        assertEquals(ReviewResult.IssuePriority.P1, priority);
        assertFalse(priority.isBlocking());
        assertTrue(priority.isCritical());
    }

    @Test
    void testCalculateIssuePriority_P1_MajorSecurity() {
        ReviewResult.IssuePriority priority = domainService.calculateIssuePriority(
                ReviewResult.IssueSeverity.MAJOR,
                "Security",
                "权限验证缺失",
                "AuthController.java"
        );

        assertEquals(ReviewResult.IssuePriority.P1, priority);
    }

    @Test
    void testCalculateIssuePriority_P1_MajorPerformance() {
        ReviewResult.IssuePriority priority = domainService.calculateIssuePriority(
                ReviewResult.IssueSeverity.MAJOR,
                "性能问题",
                "N+1查询问题",
                "OrderRepository.java"
        );

        assertEquals(ReviewResult.IssuePriority.P1, priority);
    }

    // ========== P2 优先级测试 ==========

    @Test
    void testCalculateIssuePriority_P2_MajorNonSecurity() {
        ReviewResult.IssuePriority priority = domainService.calculateIssuePriority(
                ReviewResult.IssueSeverity.MAJOR,
                "代码质量",
                "代码重复严重",
                "UtilService.java"
        );

        assertEquals(ReviewResult.IssuePriority.P2, priority);
    }

    @Test
    void testCalculateIssuePriority_P2_MinorImportantModule() {
        ReviewResult.IssuePriority priority = domainService.calculateIssuePriority(
                ReviewResult.IssueSeverity.MINOR,
                "代码规范",
                "命名不规范",
                "UserController.java"
        );

        assertEquals(ReviewResult.IssuePriority.P2, priority);
    }

    // ========== P3 优先级测试 ==========

    @Test
    void testCalculateIssuePriority_P3_MinorNonImportant() {
        ReviewResult.IssuePriority priority = domainService.calculateIssuePriority(
                ReviewResult.IssueSeverity.MINOR,
                "代码风格",
                "注释不完整",
                "StringUtil.java"
        );

        assertEquals(ReviewResult.IssuePriority.P3, priority);
    }

    @Test
    void testCalculateIssuePriority_P3_Info() {
        ReviewResult.IssuePriority priority = domainService.calculateIssuePriority(
                ReviewResult.IssueSeverity.INFO,
                "建议",
                "可以优化变量命名",
                "AnyFile.java"
        );

        assertEquals(ReviewResult.IssuePriority.P3, priority);
    }

    // ========== 安全关键词识别测试 ==========

    @ParameterizedTest
    @CsvSource({
            "安全, SQL注入风险",
            "Security, XSS vulnerability",
            "其他, sql injection detected",
            "问题, 存在csrf漏洞",
            "bug, 权限绕过风险",
            "漏洞, password exposed"
    })
    void testSecurityRelatedDetection(String category, String description) {
        ReviewResult.IssuePriority priority = domainService.calculateIssuePriority(
                ReviewResult.IssueSeverity.CRITICAL,
                category,
                description,
                "TestFile.java"
        );

        assertEquals(ReviewResult.IssuePriority.P0, priority,
                "应识别为安全问题并映射为P0: " + category + " - " + description);
    }

    // ========== 数据关键词识别测试 ==========

    @ParameterizedTest
    @CsvSource({
            "数据, 可能导致数据丢失",
            "Data, data leak detected",
            "问题, transaction失败导致数据损坏"
    })
    void testDataRelatedDetection(String category, String description) {
        ReviewResult.IssuePriority priority = domainService.calculateIssuePriority(
                ReviewResult.IssueSeverity.CRITICAL,
                category,
                description,
                "TestFile.java"
        );

        assertEquals(ReviewResult.IssuePriority.P0, priority,
                "应识别为数据问题并映射为P0: " + category + " - " + description);
    }

    // ========== 核心业务文件识别测试 ==========

    @ParameterizedTest
    @CsvSource({
            "PaymentService.java",
            "OrderController.java",
            "AccountRepository.java",
            "TransactionManager.java",
            "WalletService.java"
    })
    void testCoreBusinessFileDetection(String filePath) {
        ReviewResult.IssuePriority priority = domainService.calculateIssuePriority(
                ReviewResult.IssueSeverity.CRITICAL,
                "逻辑错误",
                "业务流程错误",
                filePath
        );

        assertEquals(ReviewResult.IssuePriority.P0, priority,
                "应识别为核心业务文件并映射为P0: " + filePath);
    }

    // ========== 性能关键词识别测试 ==========

    @ParameterizedTest
    @CsvSource({
            "性能, N+1查询",
            "Performance, memory leak detected",
            "问题, 存在慢查询slow query",
            "bug, deadlock风险"
    })
    void testPerformanceRelatedDetection(String category, String description) {
        ReviewResult.IssuePriority priority = domainService.calculateIssuePriority(
                ReviewResult.IssueSeverity.MAJOR,
                category,
                description,
                "TestFile.java"
        );

        assertEquals(ReviewResult.IssuePriority.P1, priority,
                "应识别为性能问题并映射为P1: " + category + " - " + description);
    }

    // ========== 边界条件测试 ==========

    @Test
    void testCalculateIssuePriority_NullFilePath() {
        ReviewResult.IssuePriority priority = domainService.calculateIssuePriority(
                ReviewResult.IssueSeverity.CRITICAL,
                "安全问题",
                "SQL注入",
                null
        );

        assertEquals(ReviewResult.IssuePriority.P0, priority,
                "即使文件路径为null，安全问题仍应为P0");
    }

    @Test
    void testCalculateIssuePriority_EmptyCategory() {
        ReviewResult.IssuePriority priority = domainService.calculateIssuePriority(
                ReviewResult.IssueSeverity.MINOR,
                "",
                "一般问题",
                "test.java"
        );

        assertEquals(ReviewResult.IssuePriority.P3, priority);
    }

    @Test
    void testCalculateIssuePriority_EmptyDescription() {
        ReviewResult.IssuePriority priority = domainService.calculateIssuePriority(
                ReviewResult.IssueSeverity.MAJOR,
                "代码质量",
                "",
                "service/UserService.java"
        );

        assertEquals(ReviewResult.IssuePriority.P2, priority);
    }

    // ========== IssuePriority 枚举测试 ==========

    @Test
    void testIssuePriority_Properties() {
        ReviewResult.IssuePriority p0 = ReviewResult.IssuePriority.P0;
        assertEquals("P0", p0.getCode());
        assertEquals("阻断性", p0.getDisplayName());
        assertEquals("🔴", p0.getEmoji());
        assertEquals(1, p0.getLevel());
        assertTrue(p0.isBlocking());
        assertTrue(p0.isCritical());
    }

    @Test
    void testIssuePriority_FromCode() {
        assertEquals(ReviewResult.IssuePriority.P0, ReviewResult.IssuePriority.fromCode("P0"));
        assertEquals(ReviewResult.IssuePriority.P1, ReviewResult.IssuePriority.fromCode("p1"));
        assertEquals(ReviewResult.IssuePriority.P2, ReviewResult.IssuePriority.fromCode("P2"));
        assertEquals(ReviewResult.IssuePriority.P3, ReviewResult.IssuePriority.fromCode("p3"));
    }

    @Test
    void testIssuePriority_FromCode_Invalid() {
        assertThrows(Exception.class, () -> {
            ReviewResult.IssuePriority.fromCode("P4");
        });
    }

    @Test
    void testIssuePriority_ToString() {
        String p0String = ReviewResult.IssuePriority.P0.toString();
        assertTrue(p0String.contains("🔴"));
        assertTrue(p0String.contains("P0"));
        assertTrue(p0String.contains("阻断性"));
    }

    // ========== FixSuggestion 测试 ==========

    @Test
    void testFixSuggestion_Creation() {
        ReviewResult.FixSuggestion suggestion = new ReviewResult.FixSuggestion(
                "直接拼接SQL",
                "使用PreparedStatement",
                "PreparedStatement stmt = ...",
                "单元测试验证",
                10,
                java.util.Arrays.asList("OWASP", "阿里巴巴规范")
        );

        assertEquals("直接拼接SQL", suggestion.getRootCause());
        assertEquals("使用PreparedStatement", suggestion.getFixApproach());
        assertEquals(10, suggestion.getEstimatedMinutes());
        assertEquals(2, suggestion.getReferences().size());
    }

    @Test
    void testFixSuggestion_NegativeMinutes() {
        ReviewResult.FixSuggestion suggestion = new ReviewResult.FixSuggestion(
                "原因", "方法", "代码", "测试", -5, null
        );

        assertEquals(0, suggestion.getEstimatedMinutes(),
                "负数时间应该被转换为0");
    }

    @Test
    void testFixSuggestion_NullReferences() {
        ReviewResult.FixSuggestion suggestion = new ReviewResult.FixSuggestion(
                "原因", "方法", "代码", "测试", 10, null
        );

        assertNotNull(suggestion.getReferences());
        assertEquals(0, suggestion.getReferences().size());
    }
}
