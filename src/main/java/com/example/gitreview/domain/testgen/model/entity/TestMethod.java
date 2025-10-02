package com.example.gitreview.domain.testgen.model.entity;

import com.example.gitreview.domain.shared.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * TestMethod实体
 * 表示一个具体的测试方法
 */
public class TestMethod {

    private Long id;
    private String name;
    private String description;
    private String targetMethod;
    private String testCode;
    private List<String> annotations;
    private List<String> expectedExceptions;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 构造函数
    protected TestMethod() {
        // JPA需要的默认构造函数
        this.annotations = new ArrayList<>();
        this.expectedExceptions = new ArrayList<>();
    }

    public TestMethod(String name, String description, String targetMethod,
                     String testCode, List<String> annotations, List<String> expectedExceptions) {
        this.name = validateName(name);
        this.description = description;
        this.targetMethod = targetMethod;
        this.testCode = validateTestCode(testCode);
        this.annotations = annotations != null ? new ArrayList<>(annotations) : new ArrayList<>();
        this.expectedExceptions = expectedExceptions != null ? new ArrayList<>(expectedExceptions) : new ArrayList<>();
        this.createTime = LocalDateTime.now();
        this.updateTime = this.createTime;
    }

    // 业务方法

    /**
     * 更新测试方法信息
     * @param name 新名称
     * @param description 新描述
     */
    public void updateInfo(String name, String description) {
        this.name = validateName(name);
        this.description = description;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 更新测试代码
     * @param testCode 新的测试代码
     */
    public void updateTestCode(String testCode) {
        this.testCode = validateTestCode(testCode);
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 添加注解
     * @param annotation 注解
     */
    public void addAnnotation(String annotation) {
        if (annotation != null && !annotation.trim().isEmpty() && !annotations.contains(annotation)) {
            annotations.add(annotation);
            this.updateTime = LocalDateTime.now();
        }
    }

    /**
     * 移除注解
     * @param annotation 注解
     */
    public void removeAnnotation(String annotation) {
        if (annotations.remove(annotation)) {
            this.updateTime = LocalDateTime.now();
        }
    }

    /**
     * 添加预期异常
     * @param exception 异常类名
     */
    public void addExpectedException(String exception) {
        if (exception != null && !exception.trim().isEmpty() && !expectedExceptions.contains(exception)) {
            expectedExceptions.add(exception);
            this.updateTime = LocalDateTime.now();
        }
    }

    /**
     * 移除预期异常
     * @param exception 异常类名
     */
    public void removeExpectedException(String exception) {
        if (expectedExceptions.remove(exception)) {
            this.updateTime = LocalDateTime.now();
        }
    }

    /**
     * 检查测试方法是否有效
     * @return 是否有效
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() &&
               testCode != null && !testCode.trim().isEmpty() &&
               annotations.contains("@Test");
    }

    /**
     * 获取方法复杂度评分
     * @return 复杂度评分 (1-10)
     */
    public int getComplexityScore() {
        if (testCode == null || testCode.trim().isEmpty()) {
            return 1;
        }

        int complexity = 1;

        // 基于代码行数
        int lineCount = testCode.split("\n").length;
        complexity += Math.min(3, lineCount / 5);

        // 基于断言数量
        long assertCount = testCode.lines().filter(line -> line.contains("assert")).count();
        complexity += Math.min(2, (int) assertCount);

        // 基于预期异常
        if (!expectedExceptions.isEmpty()) {
            complexity += 1;
        }

        // 基于复杂关键字
        String[] complexKeywords = {"if", "for", "while", "try", "catch", "switch"};
        for (String keyword : complexKeywords) {
            if (testCode.contains(keyword)) {
                complexity += 1;
                break;
            }
        }

        return Math.min(10, complexity);
    }

    /**
     * 估算执行时间
     * @return 估算执行时间（毫秒）
     */
    public int estimateExecutionTime() {
        int baseTime = 50; // 基础执行时间

        if (testCode == null || testCode.trim().isEmpty()) {
            return baseTime;
        }

        // 基于代码复杂度调整时间
        int complexity = getComplexityScore();
        baseTime += complexity * 10;

        // 基于特殊操作调整时间
        if (testCode.contains("Thread.sleep") || testCode.contains("await")) {
            baseTime += 100; // 异步操作需要更多时间
        }

        if (testCode.contains("@Timeout")) {
            // 如果有超时注解，尝试解析超时时间
            baseTime += 200;
        }

        return baseTime;
    }

    /**
     * 生成测试方法代码
     * @return 测试方法代码
     */
    public String generateCode() {
        StringBuilder code = new StringBuilder();

        // 添加注释
        if (description != null && !description.trim().isEmpty()) {
            code.append("/**\n");
            code.append(" * ").append(description).append("\n");
            if (targetMethod != null && !targetMethod.trim().isEmpty()) {
                code.append(" * 测试目标方法: ").append(targetMethod).append("\n");
            }
            code.append(" */\n");
        }

        // 添加注解
        for (String annotation : annotations) {
            code.append(annotation).append("\n");
        }

        // 添加方法签名
        code.append("public void ").append(name).append("() ");

        // 添加异常声明
        if (!expectedExceptions.isEmpty()) {
            code.append("throws ").append(String.join(", ", expectedExceptions)).append(" ");
        }

        code.append("{\n");

        // 添加测试代码
        String[] lines = testCode.split("\n");
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                code.append("    ").append(line).append("\n");
            } else {
                code.append("\n");
            }
        }

        code.append("}");

        return code.toString();
    }

    /**
     * 检查是否为参数化测试
     * @return 是否为参数化测试
     */
    public boolean isParameterized() {
        return annotations.contains("@ParameterizedTest") ||
               annotations.stream().anyMatch(ann -> ann.contains("Source"));
    }

    /**
     * 检查是否为性能测试
     * @return 是否为性能测试
     */
    public boolean isPerformanceTest() {
        return annotations.contains("@Timeout") ||
               testCode.contains("measureTime") ||
               testCode.contains("benchmark");
    }

    /**
     * 检查是否为集成测试
     * @return 是否为集成测试
     */
    public boolean isIntegrationTest() {
        return annotations.contains("@IntegrationTest") ||
               annotations.contains("@SpringBootTest") ||
               testCode.contains("@Autowired");
    }

    // 私有验证方法
    private String validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Test method name cannot be null or empty");
        }
        if (name.length() > 100) {
            throw new ValidationException("Test method name cannot exceed 100 characters");
        }
        // 检查方法名格式
        if (!name.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new ValidationException("Test method name must be a valid Java identifier");
        }
        return name.trim();
    }

    private String validateTestCode(String testCode) {
        if (testCode == null || testCode.trim().isEmpty()) {
            throw new ValidationException("Test code cannot be null or empty");
        }
        return testCode;
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

    public String getTargetMethod() {
        return targetMethod;
    }

    public String getTestCode() {
        return testCode;
    }

    public List<String> getAnnotations() {
        return new ArrayList<>(annotations);
    }

    public List<String> getExpectedExceptions() {
        return new ArrayList<>(expectedExceptions);
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
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
        TestMethod that = (TestMethod) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TestMethod{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", targetMethod='" + targetMethod + '\'' +
                ", complexity=" + getComplexityScore() +
                ", annotations=" + annotations.size() +
                '}';
    }
}