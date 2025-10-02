package com.example.gitreview.domain.testgen.model.entity;

import com.example.gitreview.domain.shared.exception.BusinessRuleException;
import com.example.gitreview.domain.shared.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * TestCase实体
 * 表示一个测试用例，包含多个测试方法
 */
public class TestCase {

    private Long id;
    private String name;
    private String description;
    private TestType testType;
    private String setupCode;
    private String teardownCode;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 实体内的测试方法集合
    private final List<TestMethod> testMethods = new ArrayList<>();

    /**
     * 测试类型枚举
     */
    public enum TestType {
        UNIT("单元测试"),
        INTEGRATION("集成测试"),
        FUNCTIONAL("功能测试"),
        PERFORMANCE("性能测试"),
        MOCK("模拟测试");

        private final String displayName;

        TestType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // 构造函数
    protected TestCase() {
        // JPA需要的默认构造函数
    }

    public TestCase(String name, String description, TestType testType,
                   String setupCode, String teardownCode) {
        this.name = validateName(name);
        this.description = description;
        this.testType = Objects.requireNonNull(testType, "Test type cannot be null");
        this.setupCode = setupCode;
        this.teardownCode = teardownCode;
        this.createTime = LocalDateTime.now();
        this.updateTime = this.createTime;
    }

    // 业务方法

    /**
     * 添加测试方法
     * @param testMethod 测试方法
     */
    public void addTestMethod(TestMethod testMethod) {
        Objects.requireNonNull(testMethod, "Test method cannot be null");

        // 检查方法名是否重复
        if (hasMethodWithName(testMethod.getName())) {
            throw new BusinessRuleException("Test method with name '" + testMethod.getName() + "' already exists");
        }

        testMethods.add(testMethod);
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 移除测试方法
     * @param methodName 方法名
     */
    public void removeTestMethod(String methodName) {
        testMethods.removeIf(method -> method.getName().equals(methodName));
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 更新测试用例信息
     * @param name 新名称
     * @param description 新描述
     */
    public void updateInfo(String name, String description) {
        this.name = validateName(name);
        this.description = description;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 更新设置代码
     * @param setupCode 设置代码
     */
    public void updateSetupCode(String setupCode) {
        this.setupCode = setupCode;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 更新清理代码
     * @param teardownCode 清理代码
     */
    public void updateTeardownCode(String teardownCode) {
        this.teardownCode = teardownCode;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 检查是否有指定名称的方法
     * @param methodName 方法名
     * @return 是否存在
     */
    public boolean hasMethodWithName(String methodName) {
        return testMethods.stream()
                .anyMatch(method -> method.getName().equals(methodName));
    }

    /**
     * 根据名称获取测试方法
     * @param methodName 方法名
     * @return 测试方法
     */
    public TestMethod getMethodByName(String methodName) {
        return testMethods.stream()
                .filter(method -> method.getName().equals(methodName))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取测试方法数量
     * @return 方法数量
     */
    public int getMethodCount() {
        return testMethods.size();
    }

    /**
     * 检查测试用例是否为空
     * @return 是否为空
     */
    public boolean isEmpty() {
        return testMethods.isEmpty();
    }

    /**
     * 检查测试用例是否有效
     * @return 是否有效
     */
    public boolean isValid() {
        if (isEmpty()) {
            return false;
        }

        // 检查所有测试方法是否有效
        return testMethods.stream().allMatch(TestMethod::isValid);
    }

    /**
     * 获取测试用例的复杂度评分
     * @return 复杂度评分 (1-10)
     */
    public int getComplexityScore() {
        if (isEmpty()) {
            return 1;
        }

        // 基于方法数量和方法复杂度计算
        int methodCount = testMethods.size();
        double avgComplexity = testMethods.stream()
                .mapToInt(TestMethod::getComplexityScore)
                .average()
                .orElse(1.0);

        // 基础复杂度 + 方法数量影响 + 平均方法复杂度
        int complexity = 1 + Math.min(3, methodCount / 2) + (int) avgComplexity;
        return Math.min(10, complexity);
    }

    /**
     * 估算执行时间
     * @return 估算执行时间（毫秒）
     */
    public int estimateExecutionTime() {
        if (isEmpty()) {
            return 100; // 基础时间
        }

        int baseTime = 200; // 基础时间
        int methodTime = testMethods.stream()
                .mapToInt(TestMethod::estimateExecutionTime)
                .sum();

        // 加上设置和清理时间
        int setupTime = (setupCode != null && !setupCode.trim().isEmpty()) ? 50 : 0;
        int teardownTime = (teardownCode != null && !teardownCode.trim().isEmpty()) ? 50 : 0;

        return baseTime + methodTime + setupTime + teardownTime;
    }

    /**
     * 生成测试用例代码
     * @return 测试用例代码
     */
    public String generateCode() {
        StringBuilder code = new StringBuilder();

        // 添加注释
        code.append("/**\n");
        code.append(" * ").append(description != null ? description : name).append("\n");
        code.append(" */\n");

        // 添加设置方法
        if (setupCode != null && !setupCode.trim().isEmpty()) {
            code.append("@BeforeEach\n");
            code.append("void setUp() {\n");
            code.append(setupCode);
            if (!setupCode.endsWith("\n")) {
                code.append("\n");
            }
            code.append("}\n\n");
        }

        // 添加测试方法
        for (TestMethod method : testMethods) {
            code.append(method.generateCode()).append("\n\n");
        }

        // 添加清理方法
        if (teardownCode != null && !teardownCode.trim().isEmpty()) {
            code.append("@AfterEach\n");
            code.append("void tearDown() {\n");
            code.append(teardownCode);
            if (!teardownCode.endsWith("\n")) {
                code.append("\n");
            }
            code.append("}\n");
        }

        return code.toString();
    }

    // 私有验证方法
    private String validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Test case name cannot be null or empty");
        }
        if (name.length() > 100) {
            throw new ValidationException("Test case name cannot exceed 100 characters");
        }
        return name.trim();
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public TestType getTestType() {
        return testType;
    }

    public String getSetupCode() {
        return setupCode;
    }

    public String getTeardownCode() {
        return teardownCode;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public List<TestMethod> getTestMethods() {
        return new ArrayList<>(testMethods);
    }

    // 用于持久化的setter（仅限基础设施层使用）
    public void setId(Long id) {
        this.id = id;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestCase testCase = (TestCase) o;
        return Objects.equals(id, testCase.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TestCase{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", testType=" + testType +
                ", methodCount=" + testMethods.size() +
                ", complexity=" + getComplexityScore() +
                '}';
    }
}