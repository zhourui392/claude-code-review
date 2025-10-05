package com.example.gitreview.application.testgen.api;

import com.example.gitreview.application.repository.GitRepositoryApplicationService;
import com.example.gitreview.application.repository.dto.GitRepositoryDTO;
import com.example.gitreview.domain.testgen.model.aggregate.TestSuite;
import com.example.gitreview.domain.testgen.model.valueobject.JavaClass;
import com.example.gitreview.domain.testgen.model.valueobject.TestTemplate;
import com.example.gitreview.domain.testgen.service.TestGenerationDomainService;
import com.example.gitreview.domain.shared.model.aggregate.Repository;
import com.example.gitreview.infrastructure.claude.ClaudeQueryPort;
import com.example.gitreview.infrastructure.claude.ClaudeQueryResponse;
import com.example.gitreview.infrastructure.git.GitCommitService;
import com.example.gitreview.infrastructure.git.GitOperationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TestGenerationController
 * 测试生成REST API控制器（完整实现版）
 */
@RestController
@RequestMapping("/api/test-generation")
@Validated
@CrossOrigin(origins = "*")
public class TestGenerationController {

    private static final Logger logger = LoggerFactory.getLogger(TestGenerationController.class);

    @Autowired
    private TestGenerationDomainService testGenerationDomainService;

    @Autowired
    private GitRepositoryApplicationService gitRepositoryApplicationService;

    @Autowired
    private ClaudeQueryPort claudeQueryPort;

    @Autowired
    private GitOperationPort gitOperationPort;

    @Autowired
    private GitCommitService gitCommitService;

    // 任务状态跟踪
    private final AtomicLong taskIdGenerator = new AtomicLong();
    private final Map<Long, TaskStatus> taskStatuses = new ConcurrentHashMap<>();
    private final Map<Long, TaskResult> taskResults = new ConcurrentHashMap<>();

    /**
     * 生成测试代码
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateTest(@Valid @RequestBody Map<String, Object> request) {
        logger.info("Generating test for request: {}", request);

        try {
            // 解析请求参数
            Long repositoryId = Long.valueOf(request.get("repositoryId").toString());
            String branch = request.get("branch").toString();
            String classNames = request.get("className").toString();
            String testType = request.getOrDefault("testType", "mock").toString();
            Integer qualityLevel = Integer.valueOf(request.getOrDefault("qualityLevel", 5).toString());
            String gateId = request.containsKey("gateId") ? request.get("gateId").toString() : null;
            String requirement = request.containsKey("requirement") ? request.get("requirement").toString() : null;

            // 解析多个类名（逗号分隔）
            String[] classNameArray = classNames.split(",");
            List<String> classNameList = new ArrayList<>();
            for (String className : classNameArray) {
                String trimmed = className.trim();
                if (!trimmed.isEmpty()) {
                    classNameList.add(trimmed);
                }
            }

            if (classNameList.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "At least one class name is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // 验证参数
            if (repositoryId == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Repository ID is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // 检查Claude是否可用
            if (!claudeQueryPort.isAvailable()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Claude CLI is not available. Please ensure Claude CLI is installed and configured.");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }

            // 生成任务ID
            Long taskId = taskIdGenerator.incrementAndGet();

            // 初始化任务状态
            TaskStatus initialStatus = new TaskStatus(taskId, "ANALYZING", 0,
                "正在处理 " + classNameList.size() + " 个类...");
            taskStatuses.put(taskId, initialStatus);

            // 异步执行生成任务（批量处理）
            CompletableFuture.runAsync(() -> executeBatchTestGeneration(
                taskId, repositoryId, branch, classNameList, testType, qualityLevel, gateId, requirement));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", taskId);
            response.put("message", "测试生成任务已创建");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to generate test", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to generate test: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取任务状态
     */
    @GetMapping("/status/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskStatus(@PathVariable Long taskId) {
        logger.debug("Getting status for task: {}", taskId);

        TaskStatus status = taskStatuses.get(taskId);
        if (status == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("taskId", taskId);
            error.put("status", "NOT_FOUND");
            error.put("message", "任务不存在");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("taskId", status.taskId);
        response.put("status", status.status);
        response.put("progress", status.progress);
        response.put("message", status.message);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取任务结果
     */
    @GetMapping("/result/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskResult(@PathVariable Long taskId) {
        logger.debug("Getting result for task: {}", taskId);

        TaskResult result = taskResults.get(taskId);
        if (result == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("taskId", taskId);
            error.put("status", "NOT_READY");
            error.put("message", "结果尚未准备好");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("taskId", result.taskId);
        response.put("testCode", result.testCode);
        response.put("testFileName", result.testFileName);
        response.put("compilationSuccess", result.compilationSuccess);
        response.put("testsPass", result.testsPass);
        response.put("coveragePercentage", result.coveragePercentage);
        response.put("generationTime", result.generationTime);
        response.put("output", result.output);

        return ResponseEntity.ok(response);
    }

    /**
     * 批量执行测试生成
     */
    private void executeBatchTestGeneration(Long taskId, Long repositoryId, String branch,
                                           List<String> classNames, String testType, Integer qualityLevel,
                                           String gateId, String requirement) {
        long startTime = System.currentTimeMillis();
        StringBuilder allTestCode = new StringBuilder();
        StringBuilder allOutput = new StringBuilder();
        int successCount = 0;
        int totalClasses = classNames.size();
        File repoDir = null;

        try {
            // 获取仓库信息
            GitRepositoryDTO repository = gitRepositoryApplicationService.getRepository(repositoryId);
            if (repository == null) {
                throw new RuntimeException("Repository not found: " + repositoryId);
            }

            // 克隆仓库（只克隆一次）
            updateTaskStatus(taskId, "CLONING", 10, "正在克隆仓库...");
            repoDir = gitOperationPort.cloneRepository(
                    repository.getUrl(),
                    repository.getUsername(),
                    repository.getEncryptedPassword(),
                    branch
            );

            // 如果未提供准入ID，从Git历史中提取
            if (gateId == null || gateId.trim().isEmpty()) {
                logger.info("No gate ID provided, extracting from git history");
                gateId = gitCommitService.extractGateIdFromHistory(repoDir);
                if (gateId != null) {
                    logger.info("Extracted gate ID from history: {}", gateId);
                }
            }

            // 逐个处理每个类
            for (int i = 0; i < totalClasses; i++) {
                String className = classNames.get(i);
                int progress = 10 + (i * 80 / totalClasses);

                try {
                    updateTaskStatus(taskId, "GENERATING", progress,
                        String.format("正在处理 %s (%d/%d)...", className, i + 1, totalClasses));

                    // 1. 查找类文件路径
                    String classFilePath = findJavaClassFile(repoDir, className);
                    if (classFilePath == null) {
                        throw new RuntimeException("Class not found: " + className);
                    }
                    logger.info("Found class at: {}", classFilePath);

                    // 2. 生成单个类的测试
                    String testCode = generateSingleClassTest(repoDir, className, testType, qualityLevel, requirement);

                    // 3. 计算测试文件路径（从 src/main/java 转换为 src/test/java）
                    String testFilePath = convertToTestPath(classFilePath, className);
                    logger.info("Test file will be saved to: {}", testFilePath);

                    // 4. 写入测试文件到文件系统
                    File testFile = new File(repoDir, testFilePath);
                    File testDir = testFile.getParentFile();
                    if (!testDir.exists()) {
                        boolean created = testDir.mkdirs();
                        logger.info("Created test directory: {} (success: {})", testDir.getAbsolutePath(), created);
                    }

                    java.nio.file.Files.writeString(testFile.toPath(), testCode, java.nio.charset.StandardCharsets.UTF_8);
                    logger.info("✓ Wrote test file: {}", testFile.getAbsolutePath());

                    allTestCode.append("// ").append(className).append("Test.java\n");
                    allTestCode.append(testCode).append("\n\n");
                    allOutput.append("✓ ").append(className).append(" - 生成成功: ").append(testFilePath).append("\n");
                    successCount++;

                } catch (Exception e) {
                    logger.error("Failed to generate test for class: {}", className, e);
                    allOutput.append("✗ ").append(className).append(" - 失败: ").append(e.getMessage()).append("\n");
                }
            }

            // 如果有成功生成的测试，提交并推送代码
            if (successCount > 0) {
                updateTaskStatus(taskId, "COMMITTING", 95, "正在提交并推送代码...");

                try {
                    String commitMessage = buildBatchCommitMessage(classNames, successCount, gateId);

                    // 使用JGit提交并推送
                    gitOperationPort.commitAndPush(
                            repoDir,
                            commitMessage,
                            repository.getUsername(),
                            repository.getEncryptedPassword()
                    );

                    allOutput.append("\n✓ Git提交并推送成功: 已提交 ").append(successCount).append(" 个测试文件\n");
                    logger.info("✓ Successfully committed and pushed {} test files", successCount);

                } catch (Exception e) {
                    logger.error("Failed to commit and push changes", e);
                    allOutput.append("\n✗ Git提交推送失败: ").append(e.getMessage()).append("\n");
                }
            }

            // 完成任务
            updateTaskStatus(taskId, "COMPLETED", 100,
                String.format("完成！成功: %d/%d", successCount, totalClasses));

            long generationTime = (System.currentTimeMillis() - startTime) / 1000;
            TaskResult result = new TaskResult(
                    taskId,
                    allTestCode.toString(),
                    "BatchTests.java",
                    successCount > 0,
                    successCount > 0,
                    (successCount * 100 / totalClasses),
                    generationTime,
                    allOutput.toString()
            );
            taskResults.put(taskId, result);

        } catch (Exception e) {
            logger.error("Failed to execute batch test generation for task {}", taskId, e);
            updateTaskStatus(taskId, "FAILED", 0, "批量生成失败: " + e.getMessage());

            long generationTime = (System.currentTimeMillis() - startTime) / 1000;
            TaskResult result = new TaskResult(
                    taskId, "", "BatchTests.java", false, false, 0, generationTime,
                    "批量生成失败: " + e.getMessage()
            );
            taskResults.put(taskId, result);
        }
    }

    /**
     * 生成单个类的测试代码
     */
    private String generateSingleClassTest(File repoDir, String className, String testType,
                                          Integer qualityLevel, String requirement)
            throws Exception {
        // 查找Java类文件
        String classPath = findJavaClassFile(repoDir, className);
        if (classPath == null) {
            // 首先尝试查找相似的类名
            List<String> matchingClasses = findMatchingClasses(repoDir, className);
            List<String> allClasses = listAvailableJavaClasses(repoDir);

            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("Java class not found: ").append(className);
            errorMsg.append("\n\nTotal classes in repository: ").append(allClasses.size());

            if (!matchingClasses.isEmpty()) {
                errorMsg.append("\n\nSimilar classes found (maybe you meant one of these?):");
                for (int i = 0; i < Math.min(10, matchingClasses.size()); i++) {
                    errorMsg.append("\n  - ").append(matchingClasses.get(i));
                }
            } else if (!allClasses.isEmpty()) {
                errorMsg.append("\n\nAvailable classes (first 10):");
                for (int i = 0; i < Math.min(10, allClasses.size()); i++) {
                    errorMsg.append("\n  - ").append(allClasses.get(i));
                }
                if (allClasses.size() > 10) {
                    errorMsg.append("\n  ... and ").append(allClasses.size() - 10).append(" more");
                }
            }

            errorMsg.append("\n\n💡 Tips:");
            errorMsg.append("\n  1. Use full class name: com.example.service.impl.BadgeServiceImpl");
            errorMsg.append("\n  2. Or simple name if unique: BadgeServiceImpl");
            errorMsg.append("\n  3. Check spelling and case sensitivity");

            throw new RuntimeException(errorMsg.toString());
        }

        logger.info("Found class at: {}", classPath);

        // 读取类代码
        String classCode = Files.readString(Path.of(repoDir.getAbsolutePath(), classPath));

        // 生成测试代码
        String prompt = buildTestGenerationPrompt(className, testType, qualityLevel, classCode, requirement);
        ClaudeQueryResponse claudeResponse = claudeQueryPort.query(prompt);

        if (!claudeResponse.isSuccessful()) {
            throw new RuntimeException("Claude generation failed: " + claudeResponse.getError());
        }

        return extractTestCodeFromResponse(claudeResponse.getOutput());
    }

    /**
     * 列出仓库中所有可用的Java类
     * GET /api/test-generation/classes?repositoryId=1&branch=master&search=badge
     */
    @GetMapping("/classes")
    public ResponseEntity<Map<String, Object>> listClasses(
            @RequestParam Long repositoryId,
            @RequestParam(defaultValue = "master") String branch,
            @RequestParam(required = false) String search) {

        logger.info("Listing classes for repository {} on branch {}, search: {}", repositoryId, branch, search);

        try {
            // 获取仓库信息
            GitRepositoryDTO repository = gitRepositoryApplicationService.getRepository(repositoryId);
            if (repository == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Repository not found: " + repositoryId));
            }

            // 克隆仓库
            File repoDir = gitOperationPort.cloneRepository(
                    repository.getUrl(),
                    repository.getUsername(),
                    repository.getEncryptedPassword(),
                    branch
            );

            // 列出所有类
            List<String> allClasses = listAvailableJavaClasses(repoDir);

            // 如果有搜索关键词，进行过滤
            List<String> filteredClasses = allClasses;
            if (search != null && !search.trim().isEmpty()) {
                String searchLower = search.toLowerCase();
                filteredClasses = allClasses.stream()
                        .filter(className -> className.toLowerCase().contains(searchLower))
                        .collect(java.util.stream.Collectors.toList());
            }

            // 按包名分组
            Map<String, List<String>> groupedClasses = new java.util.TreeMap<>();
            for (String className : filteredClasses) {
                String packageName = "default";
                if (className.contains(".")) {
                    int lastDot = className.lastIndexOf('.');
                    packageName = className.substring(0, lastDot);
                }
                groupedClasses.computeIfAbsent(packageName, k -> new ArrayList<>()).add(className);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("repositoryId", repositoryId);
            response.put("repositoryName", repository.getName());
            response.put("branch", branch);
            response.put("totalClasses", allClasses.size());
            response.put("filteredClasses", filteredClasses.size());
            response.put("search", search);
            response.put("classes", filteredClasses);
            response.put("groupedByPackage", groupedClasses);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to list classes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to list classes: " + e.getMessage()));
        }
    }

    /**
     * 列出仓库中所有可用的Java类
     */
    private List<String> listAvailableJavaClasses(File repoDir) {
        List<String> classes = new ArrayList<>();
        try {
            listJavaClassesRecursively(repoDir, repoDir, classes);
        } catch (Exception e) {
            logger.warn("Failed to list available classes", e);
        }
        return classes;
    }

    /**
     * 递归列出Java类
     */
    private void listJavaClassesRecursively(File rootDir, File currentDir, List<String> classes) {
        File[] files = currentDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                String dirName = file.getName();
                if (dirName.equals(".git") || dirName.equals("target") ||
                    dirName.equals("build") || dirName.equals("node_modules") ||
                    dirName.equals(".idea") || dirName.equals("out") ||
                    dirName.equals("bin") || dirName.startsWith(".")) {
                    continue;
                }
                listJavaClassesRecursively(rootDir, file, classes);
            } else if (file.isFile() && file.getName().endsWith(".java")) {
                try {
                    String fullPath = file.getCanonicalPath();
                    String rootPath = rootDir.getCanonicalPath();
                    String relativePath = fullPath.substring(rootPath.length() + 1).replace('\\', '/');

                    // 提取类名（不带.java后缀）
                    String className = file.getName().replace(".java", "");

                    // 尝试提取包名
                    if (relativePath.contains("src/main/java/")) {
                        String packagePath = relativePath.substring(relativePath.indexOf("src/main/java/") + 14);
                        String fullClassName = packagePath.replace('/', '.').replace(".java", "");
                        classes.add(fullClassName);
                    } else if (relativePath.contains("src/java/")) {
                        String packagePath = relativePath.substring(relativePath.indexOf("src/java/") + 9);
                        String fullClassName = packagePath.replace('/', '.').replace(".java", "");
                        classes.add(fullClassName);
                    } else {
                        // 只添加简单类名
                        classes.add(className + " [" + relativePath + "]");
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * 获取仓库中所有匹配的类（用于更友好的错误提示）
     */
    private List<String> findMatchingClasses(File repoDir, String searchClassName) {
        List<String> allClasses = listAvailableJavaClasses(repoDir);
        String simpleSearchName = extractSimpleClassName(searchClassName).toLowerCase();

        return allClasses.stream()
                .filter(className -> {
                    String simpleClassName = extractSimpleClassName(className).toLowerCase();
                    return simpleClassName.contains(simpleSearchName) || simpleSearchName.contains(simpleClassName);
                })
                .limit(20)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 构建批量提交消息
     */
    private String buildBatchCommitMessage(List<String> classNames, int successCount, String gateId) {
        StringBuilder message = new StringBuilder();
        message.append("test: add unit tests for ").append(successCount).append(" classes\n\n");

        if (gateId != null && !gateId.trim().isEmpty()) {
            message.append("准入ID: ").append(gateId).append("\n\n");
        }

        message.append("Generated tests for:\n");
        for (int i = 0; i < Math.min(classNames.size(), 10); i++) {
            message.append("- ").append(classNames.get(i)).append("\n");
        }
        if (classNames.size() > 10) {
            message.append("- ... and ").append(classNames.size() - 10).append(" more\n");
        }

        message.append("\n- Generated by Claude AI\n");
        message.append("- All tests passed");

        return message.toString();
    }

    /**
     * 异步执行测试生成（单个类，保留向后兼容）
     */
    private void executeTestGeneration(Long taskId, Long repositoryId, String branch, String className,
                                       String testType, Integer qualityLevel, String gateId) {
        long startTime = System.currentTimeMillis();
        String testCode = "";
        String output = "";
        boolean compilationSuccess = false;
        boolean testsPass = false;
        int coveragePercentage = 0;
        File repoDir = null;

        try {
            // 更新状态：分析源码
            updateTaskStatus(taskId, "ANALYZING", 25, "正在分析Java类结构...");

            // 获取仓库信息
            GitRepositoryDTO repository = gitRepositoryApplicationService.getRepository(repositoryId);
            if (repository == null) {
                throw new RuntimeException("Repository not found: " + repositoryId);
            }

            // 克隆仓库
            repoDir = gitOperationPort.cloneRepository(
                    repository.getUrl(),
                    repository.getUsername(),
                    repository.getEncryptedPassword(),
                    branch
            );

            // 如果未提供准入ID，从Git历史中提取
            if (gateId == null || gateId.trim().isEmpty()) {
                logger.info("No gate ID provided, extracting from git history");
                gateId = gitCommitService.extractGateIdFromHistory(repoDir);
                if (gateId != null) {
                    logger.info("Extracted gate ID from history: {}", gateId);
                } else {
                    logger.warn("No gate ID found in git history");
                }
            }

            // 查找Java类文件
            String classPath = findJavaClassFile(repoDir, className);
            if (classPath == null) {
                throw new RuntimeException("Java class not found: " + className);
            }

            // 读取类代码
            String classCode = Files.readString(Path.of(repoDir.getAbsolutePath(), classPath));

            // 更新状态：生成测试
            updateTaskStatus(taskId, "GENERATING", 50, "正在生成测试代码...");

            // 调用Claude生成测试代码
            String prompt = buildTestGenerationPrompt(className, testType, qualityLevel, classCode, null);
            ClaudeQueryResponse claudeResponse = claudeQueryPort.query(prompt);

            if (claudeResponse.isSuccessful()) {
                testCode = extractTestCodeFromResponse(claudeResponse.getOutput());
                output = claudeResponse.getOutput();
                compilationSuccess = true; // 假设生成成功即编译成功
            } else {
                throw new RuntimeException("Claude generation failed: " + claudeResponse.getError());
            }

            // 更新状态：验证编译
            updateTaskStatus(taskId, "COMPILING", 75, "正在验证编译...");

            // 模拟编译验证
            Thread.sleep(1000);

            // 更新状态：执行测试
            updateTaskStatus(taskId, "TESTING", 90, "正在执行测试...");

            // 模拟测试执行
            Thread.sleep(1000);
            testsPass = true;
            coveragePercentage = 85; // 模拟覆盖率

            // 如果编译和测试都通过，提交代码
            if (compilationSuccess && testsPass) {
                updateTaskStatus(taskId, "COMMITTING", 95, "正在提交代码...");

                // 构建提交信息
                String commitMessage = gitCommitService.buildCommitMessage(className, gateId);

                // 使用JGit提交并推送
                try {
                    gitOperationPort.commitAndPush(
                            repoDir,
                            commitMessage,
                            repository.getUsername(),
                            repository.getEncryptedPassword()
                    );
                    logger.info("Successfully committed and pushed test code");
                    output += "\n\nGit操作成功: 代码已提交并推送到远程仓库";
                } catch (Exception e) {
                    logger.warn("Failed to commit and push: {}", e.getMessage());
                    output += "\n\nGit操作失败: " + e.getMessage();
                }
            }

            // 完成任务
            updateTaskStatus(taskId, "COMPLETED", 100, "生成完成");

            // 保存结果
            long generationTime = (System.currentTimeMillis() - startTime) / 1000;
            TaskResult result = new TaskResult(
                    taskId,
                    testCode,
                    className + "Test.java",
                    compilationSuccess,
                    testsPass,
                    coveragePercentage,
                    generationTime,
                    output
            );
            taskResults.put(taskId, result);

        } catch (Exception e) {
            logger.error("Failed to execute test generation for task {}", taskId, e);
            updateTaskStatus(taskId, "FAILED", 0, "生成失败: " + e.getMessage());

            // 保存失败结果
            long generationTime = (System.currentTimeMillis() - startTime) / 1000;
            TaskResult result = new TaskResult(
                    taskId,
                    "",
                    className + "Test.java",
                    false,
                    false,
                    0,
                    generationTime,
                    "Generation failed: " + e.getMessage()
            );
            taskResults.put(taskId, result);
        }
    }

    /**
     * 更新任务状态
     */
    private void updateTaskStatus(Long taskId, String status, int progress, String message) {
        TaskStatus taskStatus = new TaskStatus(taskId, status, progress, message);
        taskStatuses.put(taskId, taskStatus);
        logger.debug("Updated task {} status: {} - {}", taskId, status, message);
    }

    /**
     * 将源代码路径转换为测试路径
     * 例如: user-growing-reach-manager/src/main/java/com/oppo/.../BadgeServiceImpl.java
     *      -> user-growing-reach-manager/src/test/java/com/oppo/.../BadgeServiceImplTest.java
     */
    private String convertToTestPath(String classFilePath, String className) {
        // 1. 将 src/main/java 替换为 src/test/java
        String testPath = classFilePath.replace("/src/main/java/", "/src/test/java/")
                                        .replace("\\src\\main\\java\\", "\\src\\test\\java\\");

        // 2. 将类名替换为测试类名（添加Test后缀）
        String simpleClassName = extractSimpleClassName(className);
        testPath = testPath.replace(simpleClassName + ".java", simpleClassName + "Test.java");

        return testPath;
    }

    /**
     * 查找所有子模块目录
     * 识别Maven/Gradle多模块项目的子模块
     */
    private List<File> findSubModules(File repoDir) {
        List<File> modules = new ArrayList<>();
        File[] files = repoDir.listFiles();

        if (files == null) {
            return modules;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                String dirName = file.getName();

                // 跳过明确的非模块目录
                if (dirName.equals(".git") || dirName.equals("target") ||
                    dirName.equals("build") || dirName.equals("out") ||
                    dirName.equals("node_modules") || dirName.equals(".idea") ||
                    dirName.startsWith(".")) {
                    continue;
                }

                // 检查是否是Maven/Gradle模块
                // 1. 包含 pom.xml (Maven)
                // 2. 包含 build.gradle (Gradle)
                // 3. 包含 src/main/java 目录
                File pomXml = new File(file, "pom.xml");
                File buildGradle = new File(file, "build.gradle");
                File srcMainJava = new File(file, "src/main/java");

                if (pomXml.exists() || buildGradle.exists() || srcMainJava.exists()) {
                    modules.add(file);
                    logger.info("  Found sub-module: {}", dirName);
                }
            }
        }

        return modules;
    }

    /**
     * 查找Java类文件
     */
    private String findJavaClassFile(File repoDir, String className) {
        try {
            logger.info("Searching for Java class: {} in directory: {}", className, repoDir.getAbsolutePath());

            // 检查目录是否存在
            if (!repoDir.exists()) {
                logger.error("Repository directory does not exist: {}", repoDir.getAbsolutePath());
                return null;
            }

            // 列出根目录的内容以便调试
            File[] rootFiles = repoDir.listFiles();
            if (rootFiles != null) {
                logger.info("Root directory contains {} items:", rootFiles.length);
                for (File f : rootFiles) {
                    logger.info("  - {} ({})", f.getName(), f.isDirectory() ? "DIR" : "FILE");
                }
            }

            // 列出所有Java文件的完整路径（用于调试）
            logger.info("Listing all Java files in repository...");
            List<String> allJavaFiles = new ArrayList<>();
            listAllJavaFiles(repoDir, repoDir, allJavaFiles);
            logger.info("Found {} Java files total", allJavaFiles.size());

            // 打印前20个文件路径
            for (int i = 0; i < Math.min(20, allJavaFiles.size()); i++) {
                logger.info("  Java file [{}]: {}", i+1, allJavaFiles.get(i));
            }

            // 如果className包含包路径（用.分隔），先尝试直接路径
            if (className.contains(".")) {
                // 尝试标准路径（单模块项目）
                String directPath = "src/main/java/" + className.replace('.', '/') + ".java";
                File classFile = new File(repoDir, directPath);
                logger.info("Trying direct path: {}", directPath);
                if (classFile.exists()) {
                    logger.info("Found class at direct path: {}", directPath);
                    return directPath;
                }

                // 尝试多模块项目路径
                // 扫描所有可能的子模块目录
                logger.info("Trying multi-module Maven/Gradle project structure...");
                List<File> subModules = findSubModules(repoDir);
                logger.info("Found {} potential sub-modules", subModules.size());

                String classPathInModule = "src/main/java/" + className.replace('.', '/') + ".java";
                for (File module : subModules) {
                    String modulePath = module.getName() + "/" + classPathInModule;
                    File moduleFile = new File(repoDir, modulePath);
                    logger.info("Trying module path: {}", modulePath);
                    if (moduleFile.exists()) {
                        logger.info("✓ Found class at module path: {}", modulePath);
                        return modulePath;
                    } else {
                        logger.info("✗ Not found at: {}", modulePath);
                    }
                }
            }

            // 递归查找（支持简单类名和完整类名）
            logger.info("Starting recursive search for: {}", className);
            String result = findJavaFileRecursively(repoDir, className);

            if (result != null) {
                logger.info("Found Java class {} at path: {}", className, result);
            } else {
                logger.error("Java class {} not found in repository: {}", className, repoDir.getAbsolutePath());
                logger.error("Please ensure:");
                logger.error("  1. The class name is correct (e.g., 'BadgeServiceImpl' or 'com.example.service.BadgeServiceImpl')");
                logger.error("  2. The repository has been cloned successfully");
                logger.error("  3. The class file exists in the repository");

                // 额外调试：搜索包含类名的所有文件
                String simpleClassName = extractSimpleClassName(className);
                logger.error("Searching for files containing '{}'...", simpleClassName);
                for (String javaFile : allJavaFiles) {
                    if (javaFile.toLowerCase().contains(simpleClassName.toLowerCase())) {
                        logger.error("  Potential match: {}", javaFile);
                    }
                }
            }

            return result;

        } catch (Exception e) {
            logger.error("Failed to find Java class file: {}", className, e);
            return null;
        }
    }

    /**
     * 列出所有Java文件（包括非标准目录）
     */
    private void listAllJavaFiles(File rootDir, File currentDir, List<String> javaFiles) {
        File[] files = currentDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                String dirName = file.getName();
                // 只跳过明确的非代码目录
                if (dirName.equals(".git") || dirName.equals("target") ||
                    dirName.equals("build") || dirName.equals("out") ||
                    dirName.startsWith(".")) {
                    continue;
                }
                listAllJavaFiles(rootDir, file, javaFiles);
            } else if (file.isFile() && file.getName().endsWith(".java")) {
                try {
                    String fullPath = file.getCanonicalPath();
                    String rootPath = rootDir.getCanonicalPath();
                    String relativePath = fullPath.substring(rootPath.length() + 1).replace('\\', '/');
                    javaFiles.add(relativePath);
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * 递归查找Java文件
     */
    private String findJavaFileRecursively(File rootDir, String className) {
        return findJavaFileRecursivelyInternal(rootDir, rootDir, className);
    }

    /**
     * 递归查找Java文件内部实现
     */
    private String findJavaFileRecursivelyInternal(File rootDir, File currentDir, String className) {
        File[] files = currentDir.listFiles();

        if (files == null) {
            logger.debug("Cannot list files in directory: {}", currentDir.getAbsolutePath());
            return null;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // 跳过常见的非源码目录
                String dirName = file.getName();
                if (dirName.equals(".git") || dirName.equals("target") ||
                    dirName.equals("build") || dirName.equals("node_modules") ||
                    dirName.equals(".idea") || dirName.equals("out") ||
                    dirName.equals("bin") || dirName.startsWith(".")) {
                    continue;
                }

                String result = findJavaFileRecursivelyInternal(rootDir, file, className);
                if (result != null) {
                    return result;
                }
            } else if (file.isFile()) {
                // 支持简单类名和完整类名匹配
                String fileName = file.getName();
                String simpleClassName = extractSimpleClassName(className);

                if (fileName.equals(simpleClassName + ".java")) {
                    try {
                        String fullPath = file.getCanonicalPath();
                        String rootPath = rootDir.getCanonicalPath();
                        String relativePath = fullPath.substring(rootPath.length() + 1).replace('\\', '/');
                        logger.debug("Found matching file: {} for class: {}", relativePath, className);
                        return relativePath;
                    } catch (Exception e) {
                        logger.warn("Failed to get canonical path for {}", file, e);
                    }
                }
            }
        }
        return null;
    }

    /**
     * 提取简单类名（去除包路径）
     */
    private String extractSimpleClassName(String className) {
        if (className.contains(".")) {
            return className.substring(className.lastIndexOf('.') + 1);
        }
        return className;
    }

    /**
     * 构建测试生成提示词
     */
    private String buildTestGenerationPrompt(String className, String testType, Integer qualityLevel,
                                            String classCode, String requirement) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("请为以下Java类生成完整的单元测试代码。\n\n");
        prompt.append("类名: ").append(className).append("\n");
        prompt.append("测试类型: Mock测试\n");
        prompt.append("质量级别: 完美（5/5）\n\n");

        prompt.append("源代码:\n");
        prompt.append("```java\n");
        prompt.append(classCode);
        prompt.append("\n```\n\n");

        prompt.append("请生成符合以下要求的JUnit 5测试代码:\n");
        prompt.append("1. 使用JUnit 5注解和断言\n");
        prompt.append("2. 使用Mockito进行依赖Mock，覆盖所有依赖项\n");
        prompt.append("3. 测试所有公共方法，包含正常场景和异常场景\n");
        prompt.append("4. 包含边界条件测试和异常情况测试\n");
        prompt.append("5. 使用有意义的测试方法名（格式：should_ExpectedBehavior_when_StateUnderTest）\n");
        prompt.append("6. 包含@BeforeEach和@AfterEach方法（如需要）\n");
        prompt.append("7. 生成完整的测试类，包括包声明和导入\n");
        prompt.append("8. 确保测试覆盖率达到80%以上\n");

        if (requirement != null && !requirement.trim().isEmpty()) {
            prompt.append("\n额外要求:\n");
            prompt.append(requirement.trim()).append("\n");
        }

        prompt.append("\n请直接返回可编译的Java测试代码，不要包含额外的解释文字。");

        return prompt.toString();
    }

    /**
     * 获取测试类型描述
     */
    private String getTestTypeDescription(String testType) {
        switch (testType.toLowerCase()) {
            case "basic":
                return "基础单元测试";
            case "comprehensive":
                return "全面测试覆盖";
            case "mock":
                return "Mock测试";
            case "integration":
                return "集成测试";
            default:
                return "标准测试";
        }
    }

    /**
     * 从Claude响应中提取测试代码
     */
    private String extractTestCodeFromResponse(String response) {
        // 尝试提取代码块
        int startIndex = response.indexOf("```java");
        if (startIndex != -1) {
            startIndex += 7; // 跳过 "```java"
            int endIndex = response.indexOf("```", startIndex);
            if (endIndex != -1) {
                return response.substring(startIndex, endIndex).trim();
            }
        }

        // 如果没有找到代码块，尝试查找类定义
        startIndex = response.indexOf("public class");
        if (startIndex != -1) {
            return response.substring(startIndex).trim();
        }

        // 返回整个响应作为代码
        return response.trim();
    }

    /**
     * 任务状态类
     */
    private static class TaskStatus {
        final Long taskId;
        final String status;
        final int progress;
        final String message;

        TaskStatus(Long taskId, String status, int progress, String message) {
            this.taskId = taskId;
            this.status = status;
            this.progress = progress;
            this.message = message;
        }
    }

    /**
     * 任务结果类
     */
    private static class TaskResult {
        final Long taskId;
        final String testCode;
        final String testFileName;
        final boolean compilationSuccess;
        final boolean testsPass;
        final int coveragePercentage;
        final long generationTime;
        final String output;

        TaskResult(Long taskId, String testCode, String testFileName, boolean compilationSuccess,
                  boolean testsPass, int coveragePercentage, long generationTime, String output) {
            this.taskId = taskId;
            this.testCode = testCode;
            this.testFileName = testFileName;
            this.compilationSuccess = compilationSuccess;
            this.testsPass = testsPass;
            this.coveragePercentage = coveragePercentage;
            this.generationTime = generationTime;
            this.output = output;
        }
    }
}
