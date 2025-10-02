package com.example.gitreview.domain.testgen.model.valueobject;

import com.example.gitreview.domain.shared.exception.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * TestTemplate值对象
 * 表示测试生成的模板配置
 */
public class TestTemplate {

    private final TestType testType;
    private final int qualityLevel;
    private final String mockFramework;
    private final String assertionFramework;
    private final List<String> additionalDependencies;

    /**
     * 测试类型枚举
     */
    public enum TestType {
        BASIC("基础测试", "生成基本的单元测试"),
        COMPREHENSIVE("全面测试", "生成全面的测试覆盖"),
        MOCK("模拟测试", "生成基于Mock的测试"),
        INTEGRATION("集成测试", "生成集成测试");

        private final String displayName;
        private final String description;

        TestType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    public TestTemplate(TestType testType, int qualityLevel, String mockFramework,
                       String assertionFramework, List<String> additionalDependencies) {
        this.testType = Objects.requireNonNull(testType, "Test type cannot be null");
        this.qualityLevel = validateQualityLevel(qualityLevel);
        this.mockFramework = validateFramework(mockFramework, "mockito");
        this.assertionFramework = validateFramework(assertionFramework, "junit");
        this.additionalDependencies = additionalDependencies != null ?
                new ArrayList<>(additionalDependencies) : new ArrayList<>();
    }

    // 静态工厂方法

    /**
     * 创建基础测试模板
     */
    public static TestTemplate basic() {
        return new TestTemplate(TestType.BASIC, 3, "mockito", "junit", List.of());
    }

    /**
     * 创建全面测试模板
     */
    public static TestTemplate comprehensive() {
        return new TestTemplate(TestType.COMPREHENSIVE, 5, "mockito", "junit",
                List.of("junit-jupiter-params", "assertj-core"));
    }

    /**
     * 创建Mock测试模板
     */
    public static TestTemplate mockBased() {
        return new TestTemplate(TestType.MOCK, 4, "mockito", "junit",
                List.of("mockito-core", "mockito-junit-jupiter"));
    }

    /**
     * 创建集成测试模板
     */
    public static TestTemplate integration() {
        return new TestTemplate(TestType.INTEGRATION, 4, "mockito", "junit",
                List.of("spring-boot-starter-test", "testcontainers"));
    }

    /**
     * 创建自定义测试模板
     */
    public static TestTemplate custom(TestType testType, int qualityLevel) {
        return new TestTemplate(testType, qualityLevel, "mockito", "junit", List.of());
    }

    // 业务方法

    /**
     * 估算测试生成时间
     * @param methodCount 方法数量
     * @return 估算时间（秒）
     */
    public int estimateGenerationTime(int methodCount) {
        int baseTime = getBaseGenerationTime();
        int methodTime = methodCount * getTimePerMethod();
        int qualityTime = qualityLevel * 5;

        return baseTime + methodTime + qualityTime;
    }

    /**
     * 获取基础生成时间
     */
    private int getBaseGenerationTime() {
        switch (testType) {
            case BASIC:
                return 15;
            case COMPREHENSIVE:
                return 45;
            case MOCK:
                return 30;
            case INTEGRATION:
                return 60;
            default:
                return 20;
        }
    }

    /**
     * 获取每个方法的生成时间
     */
    private int getTimePerMethod() {
        switch (testType) {
            case BASIC:
                return 3;
            case COMPREHENSIVE:
                return 8;
            case MOCK:
                return 5;
            case INTEGRATION:
                return 10;
            default:
                return 4;
        }
    }

    /**
     * 检查是否适合大型项目
     * @return 是否适合大型项目
     */
    public boolean isSuitableForLargeProjects() {
        return testType == TestType.COMPREHENSIVE || testType == TestType.INTEGRATION;
    }

    /**
     * 检查是否需要外部依赖
     * @return 是否需要外部依赖
     */
    public boolean requiresExternalDependencies() {
        return testType == TestType.INTEGRATION ||
               !additionalDependencies.isEmpty();
    }

    /**
     * 获取推荐的测试覆盖率目标
     * @return 覆盖率目标百分比
     */
    public int getRecommendedCoverageTarget() {
        switch (testType) {
            case BASIC:
                return 60;
            case COMPREHENSIVE:
                return 90;
            case MOCK:
                return 75;
            case INTEGRATION:
                return 70;
            default:
                return 65;
        }
    }

    /**
     * 获取推荐的测试方法数量倍数
     * @return 相对于源方法的倍数
     */
    public double getRecommendedTestMethodMultiplier() {
        switch (testType) {
            case BASIC:
                return 1.0; // 每个方法1个测试
            case COMPREHENSIVE:
                return 3.0; // 每个方法3个测试
            case MOCK:
                return 2.0; // 每个方法2个测试
            case INTEGRATION:
                return 1.5; // 每个方法1.5个测试
            default:
                return 1.2;
        }
    }

    /**
     * 检查是否支持参数化测试
     * @return 是否支持参数化测试
     */
    public boolean supportsParameterizedTests() {
        return testType == TestType.COMPREHENSIVE || qualityLevel >= 4;
    }

    /**
     * 检查是否支持异常测试
     * @return 是否支持异常测试
     */
    public boolean supportsExceptionTests() {
        return qualityLevel >= 3;
    }

    /**
     * 检查是否支持边界值测试
     * @return 是否支持边界值测试
     */
    public boolean supportsBoundaryTests() {
        return testType == TestType.COMPREHENSIVE || qualityLevel >= 4;
    }

    /**
     * 检查是否支持性能测试
     * @return 是否支持性能测试
     */
    public boolean supportsPerformanceTests() {
        return testType == TestType.INTEGRATION && qualityLevel >= 4;
    }

    /**
     * 获取测试模板复杂度评分
     * @return 复杂度评分 (1-10)
     */
    public int getComplexityScore() {
        int score = qualityLevel;

        switch (testType) {
            case BASIC:
                break; // 不增加
            case MOCK:
                score += 1;
                break;
            case COMPREHENSIVE:
                score += 2;
                break;
            case INTEGRATION:
                score += 3;
                break;
        }

        // 依赖数量影响
        score += Math.min(2, additionalDependencies.size() / 2);

        return Math.min(10, Math.max(1, score));
    }

    /**
     * 生成Maven依赖配置
     * @return Maven依赖XML片段
     */
    public String generateMavenDependencies() {
        StringBuilder deps = new StringBuilder();
        deps.append("<!-- Test Dependencies -->\n");

        // 基础JUnit依赖
        deps.append("<dependency>\n");
        deps.append("    <groupId>org.junit.jupiter</groupId>\n");
        deps.append("    <artifactId>junit-jupiter-engine</artifactId>\n");
        deps.append("    <scope>test</scope>\n");
        deps.append("</dependency>\n");

        // Mock框架依赖
        if ("mockito".equals(mockFramework)) {
            deps.append("<dependency>\n");
            deps.append("    <groupId>org.mockito</groupId>\n");
            deps.append("    <artifactId>mockito-core</artifactId>\n");
            deps.append("    <scope>test</scope>\n");
            deps.append("</dependency>\n");
        }

        // 额外依赖
        for (String dependency : additionalDependencies) {
            deps.append("<!-- ").append(dependency).append(" -->\n");
            deps.append("<!-- Add specific dependency configuration here -->\n");
        }

        return deps.toString();
    }

    /**
     * 获取测试类模板
     * @param className 类名
     * @return 测试类模板
     */
    public String getTestClassTemplate(String className) {
        StringBuilder template = new StringBuilder();

        template.append("import org.junit.jupiter.api.Test;\n");
        template.append("import org.junit.jupiter.api.BeforeEach;\n");
        template.append("import org.junit.jupiter.api.AfterEach;\n");

        if (testType == TestType.MOCK || testType == TestType.COMPREHENSIVE) {
            template.append("import org.mockito.Mock;\n");
            template.append("import org.mockito.MockitoAnnotations;\n");
        }

        template.append("import static org.junit.jupiter.api.Assertions.*;\n");

        if ("mockito".equals(mockFramework)) {
            template.append("import static org.mockito.Mockito.*;\n");
        }

        template.append("\n");
        template.append("class ").append(className).append("Test {\n\n");

        if (testType == TestType.MOCK || testType == TestType.COMPREHENSIVE) {
            template.append("    @Mock\n");
            template.append("    private SomeDependency mockDependency;\n\n");
        }

        template.append("    private ").append(className).append(" testInstance;\n\n");

        template.append("    @BeforeEach\n");
        template.append("    void setUp() {\n");

        if (testType == TestType.MOCK || testType == TestType.COMPREHENSIVE) {
            template.append("        MockitoAnnotations.openMocks(this);\n");
        }

        template.append("        testInstance = new ").append(className).append("();\n");
        template.append("    }\n\n");

        template.append("    @AfterEach\n");
        template.append("    void tearDown() {\n");
        template.append("        // Cleanup resources if needed\n");
        template.append("    }\n\n");

        template.append("    // Test methods will be generated here\n");
        template.append("}\n");

        return template.toString();
    }

    // 私有验证方法
    private int validateQualityLevel(int qualityLevel) {
        if (qualityLevel < 1 || qualityLevel > 5) {
            throw new ValidationException("Quality level must be between 1 and 5");
        }
        return qualityLevel;
    }

    private String validateFramework(String framework, String defaultFramework) {
        if (framework == null || framework.trim().isEmpty()) {
            return defaultFramework;
        }
        return framework.trim().toLowerCase();
    }

    // Getters
    public TestType getTestType() {
        return testType;
    }

    public int getQualityLevel() {
        return qualityLevel;
    }

    public String getMockFramework() {
        return mockFramework;
    }

    public String getAssertionFramework() {
        return assertionFramework;
    }

    public List<String> getAdditionalDependencies() {
        return new ArrayList<>(additionalDependencies);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestTemplate that = (TestTemplate) o;
        return qualityLevel == that.qualityLevel &&
               testType == that.testType &&
               Objects.equals(mockFramework, that.mockFramework) &&
               Objects.equals(assertionFramework, that.assertionFramework) &&
               Objects.equals(additionalDependencies, that.additionalDependencies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testType, qualityLevel, mockFramework, assertionFramework, additionalDependencies);
    }

    @Override
    public String toString() {
        return "TestTemplate{" +
                "testType=" + testType +
                ", qualityLevel=" + qualityLevel +
                ", mockFramework='" + mockFramework + '\'' +
                ", assertionFramework='" + assertionFramework + '\'' +
                ", dependencies=" + additionalDependencies.size() +
                ", complexity=" + getComplexityScore() +
                '}';
    }
}