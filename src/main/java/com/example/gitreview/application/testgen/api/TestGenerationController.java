package com.example.gitreview.application.testgen.api;

import com.example.gitreview.application.testgen.TestGenerationApplicationService;
import com.example.gitreview.application.repository.GitRepositoryApplicationService;
import com.example.gitreview.application.repository.dto.GitRepositoryDTO;
import com.example.gitreview.infrastructure.git.GitOperationPort;
import com.example.gitreview.domain.testgen.service.TestGenerationDomainService;
import com.example.gitreview.infrastructure.claude.ClaudeQueryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.io.File;
import java.util.*;

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
    private ClaudeQueryPort claudeQueryPort;

    @Autowired
    private TestGenerationApplicationService applicationService;

    @Autowired
    private GitRepositoryApplicationService gitRepositoryApplicationService;

    @Autowired
    private GitOperationPort gitOperationPort;

    /**
     * 生成测试代码
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateTest(@Valid @RequestBody Map<String, Object> request) {
        logger.info("Generating test for request: {}", request);

        try {
            Long repositoryId = Long.valueOf(request.get("repositoryId").toString());
            String branch = request.get("branch").toString();
            String classNames = request.get("className").toString();
            String testType = request.getOrDefault("testType", "mock").toString();
            Integer qualityLevel = Integer.valueOf(request.getOrDefault("qualityLevel", 5).toString());
            String gateId = request.containsKey("gateId") ? request.get("gateId").toString() : null;
            String requirement = request.containsKey("requirement") ? request.get("requirement").toString() : null;

            List<String> classNameList = Arrays.stream(classNames.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList();

            if (!claudeQueryPort.isAvailable()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("success", false, "error", "Claude CLI is not available"));
            }

            Long taskId = applicationService.startBatchGeneration(repositoryId, branch, classNameList,
                    testType, qualityLevel, gateId, requirement);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "taskId", taskId,
                    "message", "测试生成任务已创建"
            ));

        } catch (Exception e) {
            logger.error("Failed to generate test", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", "Failed to generate test: " + e.getMessage()));
        }
    }

    /**
     * 获取任务状态
     */
    @GetMapping("/status/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskStatus(@PathVariable Long taskId) {
        logger.debug("Getting status for task: {}", taskId);

        Map<String, Object> response = applicationService.getBatchStatus(taskId);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取任务结果
     */
    @GetMapping("/result/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskResult(@PathVariable Long taskId) {
        logger.debug("Getting result for task: {}", taskId);

        Map<String, Object> response = applicationService.getBatchResult(taskId);
        return ResponseEntity.ok(response);
    }

    /**
     * 生成单个类的测试代码
     */

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
     * 获取仓库中所有匹配的类（用于更友好的错误提示）
     */
    // 保留类清单 API：复用原实现

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
     * 递归列出Java类（生成全限定名）
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

                    if (relativePath.contains("src/main/java/")) {
                        String packagePath = relativePath.substring(relativePath.indexOf("src/main/java/") + 14);
                        String fullClassName = packagePath.replace('/', '.').replace(".java", "");
                        classes.add(fullClassName);
                    } else if (relativePath.contains("src/java/")) {
                        String packagePath = relativePath.substring(relativePath.indexOf("src/java/") + 9);
                        String fullClassName = packagePath.replace('/', '.').replace(".java", "");
                        classes.add(fullClassName);
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    /**
     * 构建批量提交消息
     */
    private String buildBatchCommitMessage(List<String> classNames, int successCount, String gateId) {
        StringBuilder message = new StringBuilder();

        // 使用规范的提交信息模板
        message.append("{\n");
        message.append("test: 新增 ").append(successCount).append(" 个类的单元测试\n");
        message.append("}\n");
        message.append("适用范围：{无}\n");

        // 准入ID
        if (gateId != null && !gateId.trim().isEmpty()) {
            String formattedGateId = gateId.trim();
            if (!formattedGateId.startsWith("#")) {
                formattedGateId = "#" + formattedGateId;
            }
            message.append("准入id：{").append(formattedGateId).append("}\n");
        } else {
            message.append("准入id：{无}\n");
        }

        message.append("分析：{使用Claude AI自动生成单元测试}\n");

        // 方案：列出生成的测试类
        message.append("方案：{新增测试类: ");
        for (int i = 0; i < Math.min(classNames.size(), 5); i++) {
            if (i > 0) message.append(", ");
            message.append(classNames.get(i)).append("Test");
        }
        if (classNames.size() > 5) {
            message.append(" 等 ").append(classNames.size()).append(" 个");
        }
        message.append("}\n");

        message.append("风险及影响[快/稳/省/功能/安全隐私]：{功能}\n");
        message.append("测试建议：{已自动生成单元测试并验证通过}");

        return message.toString();
    }


    /**
     * 更新任务状态
     */
    // 历史状态接口由应用层统一实现，这里不再维护本地状态

    /**
     * 将源代码路径转换为测试路径
     * 例如: user-growing-reach-manager/src/main/java/com/oppo/.../BadgeServiceImpl.java
     *      -> user-growing-reach-manager/src/test/java/com/oppo/.../BadgeServiceImplTest.java
     */
    // 由应用层实现

    /**
     * 查找所有子模块目录
     * 识别Maven/Gradle多模块项目的子模块
     */
    private List<java.io.File> findSubModules(java.io.File repoDir) {
        List<java.io.File> modules = new ArrayList<>();
        java.io.File[] files = repoDir.listFiles();

        if (files == null) {
            return modules;
        }

        for (java.io.File file : files) {
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
                java.io.File pomXml = new java.io.File(file, "pom.xml");
                java.io.File buildGradle = new java.io.File(file, "build.gradle");
                java.io.File srcMainJava = new java.io.File(file, "src/main/java");

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
    private String findJavaClassFile(java.io.File repoDir, String className) {
        try {
            logger.info("Searching for Java class: {} in directory: {}", className, repoDir.getAbsolutePath());

            // 检查目录是否存在
            if (!repoDir.exists()) {
                logger.error("Repository directory does not exist: {}", repoDir.getAbsolutePath());
                return null;
            }

            // 列出根目录的内容以便调试
            java.io.File[] rootFiles = repoDir.listFiles();
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
                java.io.File classFile = new java.io.File(repoDir, directPath);
                logger.info("Trying direct path: {}", directPath);
                if (classFile.exists()) {
                    logger.info("Found class at direct path: {}", directPath);
                    return directPath;
                }

                // 尝试多模块项目路径
                // 扫描所有可能的子模块目录
                logger.info("Trying multi-module Maven/Gradle project structure...");
                List<java.io.File> subModules = findSubModules(repoDir);
                logger.info("Found {} potential sub-modules", subModules.size());

                String classPathInModule = "src/main/java/" + className.replace('.', '/') + ".java";
                for (java.io.File module : subModules) {
                    String modulePath = module.getName() + "/" + classPathInModule;
                    java.io.File moduleFile = new java.io.File(repoDir, modulePath);
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
    private void listAllJavaFiles(java.io.File rootDir, java.io.File currentDir, List<String> javaFiles) {
        java.io.File[] files = currentDir.listFiles();
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
    private String findJavaFileRecursively(java.io.File rootDir, String className) {
        return findJavaFileRecursivelyInternal(rootDir, rootDir, className);
    }

    /**
     * 递归查找Java文件内部实现
     */
    private String findJavaFileRecursivelyInternal(java.io.File rootDir, java.io.File currentDir, String className) {
        java.io.File[] files = currentDir.listFiles();

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
}
