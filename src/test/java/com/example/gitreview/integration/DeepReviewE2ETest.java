package com.example.gitreview.integration;

import com.example.gitreview.domain.codereview.model.valueobject.ReviewResult;
import com.example.gitreview.infrastructure.parser.ReviewResultParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 深度代码审查端到端集成测试
 * 验证P0-P3问题分级和深度审查流程
 *
 * @author zhourui(V33215020)
 * @since 2025/10/03
 */
@SpringBootTest
class DeepReviewE2ETest {

    @Autowired
    private ReviewResultParser reviewResultParser;

    /**
     * 测试深度审查完整流程
     * 模拟Claude返回包含P0-P3所有优先级问题的JSON响应
     */
    @Test
    void testDeepReviewFlow_AllPriorities() {
        // 模拟Claude深度审查的JSON响应
        String claudeResponse = createMockClaudeResponse();

        // 解析审查结果
        ReviewResult result = reviewResultParser.parse(claudeResponse);

        // 验证基本信息
        assertNotNull(result);
        assertNotNull(result.getSummary());
        assertTrue(result.getQualityScore() >= 0 && result.getQualityScore() <= 100);

        // 验证问题列表包含所有优先级
        assertTrue(result.getIssues().size() >= 4, "应包含至少4个问题（P0-P3各一个）");

        // 验证P0问题
        long p0Count = result.getIssues().stream()
                .filter(i -> i.getPriority() == ReviewResult.IssuePriority.P0)
                .count();
        assertTrue(p0Count >= 1, "应包含至少1个P0问题");

        // 验证P1问题
        long p1Count = result.getIssues().stream()
                .filter(i -> i.getPriority() == ReviewResult.IssuePriority.P1)
                .count();
        assertTrue(p1Count >= 1, "应包含至少1个P1问题");

        // 验证P2问题
        long p2Count = result.getIssues().stream()
                .filter(i -> i.getPriority() == ReviewResult.IssuePriority.P2)
                .count();
        assertTrue(p2Count >= 1, "应包含至少1个P2问题");

        // 验证P3建议
        long p3Count = result.getIssues().stream()
                .filter(i -> i.getPriority() == ReviewResult.IssuePriority.P3)
                .count();
        assertTrue(p3Count >= 1, "应包含至少1个P3建议");

        // 验证问题分级准确性
        ReviewResult.Issue p0Issue = result.getIssuesByPriority(ReviewResult.IssuePriority.P0).get(0);
        assertTrue(p0Issue.isBlocking(), "P0问题应为阻断性");
        assertTrue(p0Issue.isCritical(), "P0问题应为严重级别");
        assertEquals(ReviewResult.IssueSeverity.CRITICAL, p0Issue.getSeverity());

        // 验证修复建议
        assertTrue(result.getIssues().stream()
                        .anyMatch(i -> i.getFixSuggestion() != null),
                "至少有一个问题应包含修复建议");

        // 验证metrics统计
        ReviewResult.ReviewMetrics metrics = result.getMetrics();
        assertNotNull(metrics);
        assertTrue(metrics.getP0Issues() >= 1);
        assertTrue(metrics.getP1Issues() >= 1);
        assertTrue(metrics.getP2Issues() >= 1);
        assertTrue(metrics.getP3Issues() >= 1);
    }

    /**
     * 测试P0问题识别准确性
     */
    @Test
    void testP0IssueDetection() {
        String sqlInjectionResponse = createP0SqlInjectionResponse();
        ReviewResult result = reviewResultParser.parse(sqlInjectionResponse);

        assertNotNull(result);
        assertTrue(result.hasCriticalIssues());

        ReviewResult.Issue p0Issue = result.getBlockingIssues().get(0);
        assertEquals(ReviewResult.IssuePriority.P0, p0Issue.getPriority());
        assertTrue(p0Issue.getCategory().contains("安全") || p0Issue.getCategory().toLowerCase().contains("security"));
        assertNotNull(p0Issue.getFixSuggestion());
        assertTrue(p0Issue.getFixSuggestion().getEstimatedMinutes() > 0);
    }

    /**
     * 测试深度审查包含上下文信息
     */
    @Test
    void testDeepReviewWithContext() {
        String responseWithContext = createResponseWithCodeSnippets();
        ReviewResult result = reviewResultParser.parse(responseWithContext);

        assertNotNull(result);

        // 验证至少有一个问题包含代码片段
        assertTrue(result.getIssues().stream()
                        .anyMatch(i -> i.getCodeSnippet() != null && !i.getCodeSnippet().isEmpty()),
                "深度审查应包含问题代码片段");

        // 验证至少有一个问题包含影响说明
        assertTrue(result.getIssues().stream()
                        .anyMatch(i -> i.getImpact() != null && !i.getImpact().isEmpty()),
                "深度审查应包含影响说明");
    }

    /**
     * 测试质量评分计算
     */
    @Test
    void testQualityScoreCalculation() {
        // 无问题的代码
        String perfectCodeResponse = createPerfectCodeResponse();
        ReviewResult perfectResult = reviewResultParser.parse(perfectCodeResponse);
        assertTrue(perfectResult.getQualityScore() >= 90, "无问题代码质量分应≥90");

        // 有严重问题的代码
        String criticalIssuesResponse = createCriticalIssuesResponse();
        ReviewResult criticalResult = reviewResultParser.parse(criticalIssuesResponse);
        assertTrue(criticalResult.getQualityScore() < 70, "严重问题代码质量分应<70");
    }

    // ========== 测试数据生成方法 ==========

    /**
     * 创建包含所有优先级问题的模拟响应
     */
    private String createMockClaudeResponse() {
        return "```json\n" +
                "{\n" +
                "  \"summary\": \"发现多个安全、性能和代码质量问题\",\n" +
                "  \"qualityScore\": 65,\n" +
                "  \"riskLevel\": \"high\",\n" +
                "  \"issues\": [\n" +
                "    {\n" +
                "      \"priority\": \"P0\",\n" +
                "      \"severity\": \"CRITICAL\",\n" +
                "      \"category\": \"安全问题\",\n" +
                "      \"file\": \"PaymentService.java\",\n" +
                "      \"line\": 123,\n" +
                "      \"codeSnippet\": \"String sql = \\\"SELECT * FROM orders WHERE id=\\\" + orderId;\",\n" +
                "      \"description\": \"SQL注入漏洞\",\n" +
                "      \"impact\": \"攻击者可获取所有订单数据，导致严重数据泄露\",\n" +
                "      \"fixSuggestion\": {\n" +
                "        \"rootCause\": \"直接拼接用户输入到SQL语句\",\n" +
                "        \"fixApproach\": \"使用PreparedStatement参数化查询\",\n" +
                "        \"codeExample\": \"PreparedStatement stmt = conn.prepareStatement(\\\"SELECT * FROM orders WHERE id = ?\\\");\\nstmt.setLong(1, orderId);\",\n" +
                "        \"testStrategy\": \"单元测试验证参数化查询，尝试SQL注入攻击测试\",\n" +
                "        \"estimatedMinutes\": 10,\n" +
                "        \"references\": [\"OWASP Top 10 - Injection\", \"阿里巴巴Java开发手册\"]\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"priority\": \"P1\",\n" +
                "      \"severity\": \"MAJOR\",\n" +
                "      \"category\": \"性能问题\",\n" +
                "      \"file\": \"OrderRepository.java\",\n" +
                "      \"line\": 45,\n" +
                "      \"codeSnippet\": \"for (Order order : orders) { order.getItems(); }\",\n" +
                "      \"description\": \"N+1查询问题\",\n" +
                "      \"impact\": \"大量订单时性能严重下降\",\n" +
                "      \"fixSuggestion\": {\n" +
                "        \"rootCause\": \"未使用JOIN预加载关联数据\",\n" +
                "        \"fixApproach\": \"使用FETCH JOIN一次性加载\",\n" +
                "        \"codeExample\": \"@Query(\\\"SELECT o FROM Order o LEFT JOIN FETCH o.items\\\")\",\n" +
                "        \"testStrategy\": \"性能测试验证查询次数减少\",\n" +
                "        \"estimatedMinutes\": 20,\n" +
                "        \"references\": [\"Hibernate性能优化\"]\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"priority\": \"P2\",\n" +
                "      \"severity\": \"MAJOR\",\n" +
                "      \"category\": \"代码质量\",\n" +
                "      \"file\": \"UserService.java\",\n" +
                "      \"line\": 78,\n" +
                "      \"description\": \"代码重复，三处相同逻辑\",\n" +
                "      \"impact\": \"可维护性差，修改时容易遗漏\",\n" +
                "      \"fixSuggestion\": {\n" +
                "        \"rootCause\": \"未提取公共方法\",\n" +
                "        \"fixApproach\": \"抽取为私有方法\",\n" +
                "        \"codeExample\": \"private void validateUser(User user) { ... }\",\n" +
                "        \"testStrategy\": \"重构后运行现有测试\",\n" +
                "        \"estimatedMinutes\": 15,\n" +
                "        \"references\": [\"重构：改善既有代码的设计\"]\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"priority\": \"P3\",\n" +
                "      \"severity\": \"MINOR\",\n" +
                "      \"category\": \"代码规范\",\n" +
                "      \"file\": \"StringUtil.java\",\n" +
                "      \"line\": 12,\n" +
                "      \"description\": \"变量命名不规范，使用缩写\",\n" +
                "      \"impact\": \"可读性略差\",\n" +
                "      \"fixSuggestion\": {\n" +
                "        \"rootCause\": \"使用了不清晰的缩写\",\n" +
                "        \"fixApproach\": \"使用完整单词命名\",\n" +
                "        \"codeExample\": \"String requestId 替换 String reqId\",\n" +
                "        \"testStrategy\": \"无需测试\",\n" +
                "        \"estimatedMinutes\": 2,\n" +
                "        \"references\": [\"阿里巴巴命名规范\"]\n" +
                "      }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"suggestions\": [\n" +
                "    {\n" +
                "      \"category\": \"测试覆盖\",\n" +
                "      \"description\": \"建议为支付核心逻辑添加单元测试\",\n" +
                "      \"priority\": 9,\n" +
                "      \"benefit\": \"降低Bug引入风险\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "```";
    }

    private String createP0SqlInjectionResponse() {
        return "```json\n" +
                "{\n" +
                "  \"summary\": \"发现严重SQL注入漏洞\",\n" +
                "  \"qualityScore\": 40,\n" +
                "  \"riskLevel\": \"critical\",\n" +
                "  \"issues\": [\n" +
                "    {\n" +
                "      \"priority\": \"P0\",\n" +
                "      \"severity\": \"CRITICAL\",\n" +
                "      \"category\": \"安全问题\",\n" +
                "      \"file\": \"PaymentService.java\",\n" +
                "      \"line\": 50,\n" +
                "      \"description\": \"SQL注入风险\",\n" +
                "      \"fixSuggestion\": {\n" +
                "        \"rootCause\": \"SQL拼接\",\n" +
                "        \"fixApproach\": \"使用PreparedStatement\",\n" +
                "        \"codeExample\": \"stmt.setString(1, userId)\",\n" +
                "        \"testStrategy\": \"单元测试\",\n" +
                "        \"estimatedMinutes\": 10,\n" +
                "        \"references\": [\"OWASP\"]\n" +
                "      }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"suggestions\": []\n" +
                "}\n" +
                "```";
    }

    private String createResponseWithCodeSnippets() {
        return createMockClaudeResponse(); // 已包含代码片段
    }

    private String createPerfectCodeResponse() {
        return "```json\n" +
                "{\n" +
                "  \"summary\": \"代码质量优秀，未发现严重问题\",\n" +
                "  \"qualityScore\": 95,\n" +
                "  \"riskLevel\": \"low\",\n" +
                "  \"issues\": [],\n" +
                "  \"suggestions\": [\n" +
                "    {\n" +
                "      \"category\": \"优化建议\",\n" +
                "      \"description\": \"可考虑添加更多注释\",\n" +
                "      \"priority\": 3,\n" +
                "      \"benefit\": \"提升可维护性\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "```";
    }

    private String createCriticalIssuesResponse() {
        return "```json\n" +
                "{\n" +
                "  \"summary\": \"发现多个严重问题\",\n" +
                "  \"qualityScore\": 45,\n" +
                "  \"riskLevel\": \"critical\",\n" +
                "  \"issues\": [\n" +
                "    {\n" +
                "      \"priority\": \"P0\",\n" +
                "      \"severity\": \"CRITICAL\",\n" +
                "      \"category\": \"安全\",\n" +
                "      \"file\": \"test.java\",\n" +
                "      \"line\": 1,\n" +
                "      \"description\": \"SQL注入\",\n" +
                "      \"fixSuggestion\": {\"rootCause\": \"拼接\", \"fixApproach\": \"PreparedStatement\", \"codeExample\": \"stmt.set\", \"testStrategy\": \"测试\", \"estimatedMinutes\": 10, \"references\": []}\n" +
                "    },\n" +
                "    {\n" +
                "      \"priority\": \"P0\",\n" +
                "      \"severity\": \"CRITICAL\",\n" +
                "      \"category\": \"数据\",\n" +
                "      \"file\": \"test.java\",\n" +
                "      \"line\": 2,\n" +
                "      \"description\": \"数据泄露风险\",\n" +
                "      \"fixSuggestion\": {\"rootCause\": \"权限\", \"fixApproach\": \"添加验证\", \"codeExample\": \"check\", \"testStrategy\": \"测试\", \"estimatedMinutes\": 20, \"references\": []}\n" +
                "    }\n" +
                "  ],\n" +
                "  \"suggestions\": []\n" +
                "}\n" +
                "```";
    }
}
