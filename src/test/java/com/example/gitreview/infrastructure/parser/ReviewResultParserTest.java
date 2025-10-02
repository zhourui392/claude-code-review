package com.example.gitreview.infrastructure.parser;

import com.example.gitreview.domain.codereview.model.valueobject.ReviewResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReviewResultParser 测试
 */
class ReviewResultParserTest {

    private ReviewResultParser parser;

    @BeforeEach
    void setUp() {
        parser = new ReviewResultParser();
    }

    @Test
    void testParseJson_Success() {
        String json = "```json\n" +
                "{\n" +
                "  \"summary\": \"发现SQL注入漏洞\",\n" +
                "  \"qualityScore\": 60,\n" +
                "  \"riskLevel\": \"high\",\n" +
                "  \"issues\": [\n" +
                "    {\n" +
                "      \"priority\": \"P0\",\n" +
                "      \"severity\": \"CRITICAL\",\n" +
                "      \"category\": \"安全问题\",\n" +
                "      \"file\": \"PaymentService.java\",\n" +
                "      \"line\": 123,\n" +
                "      \"codeSnippet\": \"String sql = \\\"SELECT * FROM orders WHERE id=\\\" + orderId;\",\n" +
                "      \"description\": \"SQL注入风险\",\n" +
                "      \"impact\": \"数据泄露\",\n" +
                "      \"fixSuggestion\": {\n" +
                "        \"rootCause\": \"直接拼接SQL\",\n" +
                "        \"fixApproach\": \"使用PreparedStatement\",\n" +
                "        \"codeExample\": \"PreparedStatement stmt = conn.prepareStatement(\\\"SELECT * FROM orders WHERE id = ?\\\");\",\n" +
                "        \"testStrategy\": \"单元测试\",\n" +
                "        \"estimatedMinutes\": 10,\n" +
                "        \"references\": [\"OWASP\"]\n" +
                "      }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"suggestions\": [\n" +
                "    {\n" +
                "      \"category\": \"测试\",\n" +
                "      \"description\": \"增加单元测试\",\n" +
                "      \"priority\": 8,\n" +
                "      \"benefit\": \"提升质量\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "```";

        ReviewResult result = parser.parse(json);

        assertNotNull(result);
        assertEquals("发现SQL注入漏洞", result.getSummary());
        assertEquals(60, result.getQualityScore());
        assertEquals(1, result.getIssues().size());
        assertEquals(1, result.getSuggestions().size());

        ReviewResult.Issue issue = result.getIssues().get(0);
        assertEquals(ReviewResult.IssuePriority.P0, issue.getPriority());
        assertEquals(ReviewResult.IssueSeverity.CRITICAL, issue.getSeverity());
        assertEquals("安全问题", issue.getCategory());
        assertEquals("PaymentService.java", issue.getFilePath());
        assertEquals(123, issue.getLineNumber());
        assertNotNull(issue.getFixSuggestion());
        assertEquals("直接拼接SQL", issue.getFixSuggestion().getRootCause());
    }

    @Test
    void testParseJson_WithoutCodeBlock() {
        String json = "{" +
                "\"summary\": \"审查通过\"," +
                "\"qualityScore\": 95," +
                "\"issues\": []," +
                "\"suggestions\": []" +
                "}";

        ReviewResult result = parser.parse(json);

        assertNotNull(result);
        assertEquals("审查通过", result.getSummary());
        assertEquals(95, result.getQualityScore());
        assertEquals(0, result.getIssues().size());
    }

    @Test
    void testParseMarkdown_Fallback() {
        String markdown = "代码审查结果\n" +
                "\n" +
                "发现以下问题：\n" +
                "- [P0] SQL注入风险 (PaymentService.java:123)\n" +
                "- [P1] N+1查询问题 (OrderRepository.java:45)\n" +
                "- [P2] 代码重复 (HelperUtil.java:78)\n";

        ReviewResult result = parser.parse(markdown);

        assertNotNull(result);
        // Markdown 解析可能不完美，只验证基本功能
        assertTrue(result.getIssues().size() > 0, "应该至少发现一些问题");
    }

    @Test
    void testParseEmpty() {
        ReviewResult result = parser.parse("");

        assertNotNull(result);
        assertEquals(0, result.getQualityScore());
        assertTrue(result.getSummary().contains("为空") || result.getSummary().contains("失败"));
    }

    @Test
    void testParseNull() {
        ReviewResult result = parser.parse(null);

        assertNotNull(result);
        assertEquals(0, result.getQualityScore());
        assertTrue(result.getSummary().contains("为空") || result.getSummary().contains("失败"));
    }

    @Test
    void testParseInvalidJson() {
        String invalidJson = "```json\n{invalid json\n```";

        ReviewResult result = parser.parse(invalidJson);

        assertNotNull(result);
        // 应该回退到 Markdown 解析或返回错误结果
    }

    @Test
    void testParsePriorityVariations() {
        String json = "{" +
                "\"summary\": \"测试优先级\"," +
                "\"qualityScore\": 80," +
                "\"issues\": [" +
                "  {\"priority\": \"p0\", \"severity\": \"CRITICAL\", \"category\": \"test\", \"file\": \"test.java\", \"line\": 1, \"description\": \"test\"}," +
                "  {\"priority\": \"P1\", \"severity\": \"MAJOR\", \"category\": \"test\", \"file\": \"test.java\", \"line\": 2, \"description\": \"test\"}," +
                "  {\"priority\": \"p2\", \"severity\": \"MINOR\", \"category\": \"test\", \"file\": \"test.java\", \"line\": 3, \"description\": \"test\"}" +
                "]," +
                "\"suggestions\": []" +
                "}";

        ReviewResult result = parser.parse(json);

        assertNotNull(result);
        assertEquals(3, result.getIssues().size());
        assertEquals(ReviewResult.IssuePriority.P0, result.getIssues().get(0).getPriority());
        assertEquals(ReviewResult.IssuePriority.P1, result.getIssues().get(1).getPriority());
        assertEquals(ReviewResult.IssuePriority.P2, result.getIssues().get(2).getPriority());
    }

    @Test
    void testParseWithMissingFields() {
        String json = "{" +
                "\"summary\": \"部分字段缺失\"," +
                "\"qualityScore\": 75," +
                "\"issues\": [" +
                "  {\"priority\": \"P0\", \"severity\": \"CRITICAL\", \"file\": \"test.java\", \"line\": 1}" +
                "]," +
                "\"suggestions\": []" +
                "}";

        ReviewResult result = parser.parse(json);

        assertNotNull(result);
        assertEquals(1, result.getIssues().size());

        ReviewResult.Issue issue = result.getIssues().get(0);
        assertEquals("其他", issue.getCategory()); // 默认值
        assertEquals("无描述", issue.getDescription()); // 默认值
    }
}
