package com.example.gitreview.application.testgen;

import com.example.gitreview.application.testgen.dto.TestGenerationRequestDTO;
import com.example.gitreview.application.testgen.dto.TestGenerationResultDTO;
import com.example.gitreview.application.testgen.dto.TestSuiteDTO;
import com.example.gitreview.application.testgen.dto.TestStatusDTO;
import com.example.gitreview.application.testgen.assembler.TestGenerationAssembler;
import com.example.gitreview.domain.testgen.model.aggregate.TestSuite;
import com.example.gitreview.domain.testgen.model.valueobject.JavaClass;
import com.example.gitreview.domain.testgen.model.valueobject.TestTemplate;
import com.example.gitreview.domain.testgen.service.TestGenerationDomainService;
import com.example.gitreview.domain.shared.model.aggregate.Repository;
import com.example.gitreview.domain.shared.repository.GitRepositoryRepository;
import com.example.gitreview.domain.testgen.repository.TestSuiteRepository;
import com.example.gitreview.infrastructure.claude.ClaudeQueryPort;
import com.example.gitreview.infrastructure.claude.ClaudeQueryResponse;
import com.example.gitreview.infrastructure.claude.ClaudeCodePort;
import com.example.gitreview.infrastructure.git.GitOperationPort;
import com.example.gitreview.infrastructure.git.GitCommitService;
import com.example.gitreview.infrastructure.compilation.CodeCompilationService;
import com.example.gitreview.infrastructure.compilation.CompilationResult;
import com.example.gitreview.domain.shared.exception.ResourceNotFoundException;
import com.example.gitreview.domain.shared.exception.BusinessRuleException;
import com.example.gitreview.domain.shared.exception.ValidationException;

import org.eclipse.jgit.api.Git;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 测试生成应用服务
 * 编排测试生成的完整业务流程，协调领域服务与基础设施
 */
@Service
@Transactional
public class TestGenerationApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(TestGenerationApplicationService.class);

    private final TestGenerationDomainService testGenerationDomainService;
    private final GitRepositoryRepository repositoryRepository;
    private final TestSuiteRepository testSuiteRepository;
    private final ClaudeQueryPort claudeQueryPort;
    private final GitOperationPort gitOperationPort;
    private final TestGenerationAssembler assembler;
    private final CodeCompilationService compilationService;
    private final ClaudeCodePort claudeCodePort;
    private final GitCommitService gitCommitService;

    @Value("${test.generation.max-fix-retries:3}")
    private int maxFixRetries;

    // 任务状态缓存，实际项目中应使用Redis等分布式缓存
    private final ConcurrentMap<String, TestSuite> taskCache = new ConcurrentHashMap<>();

    @Autowired
    public TestGenerationApplicationService(
            TestGenerationDomainService testGenerationDomainService,
            GitRepositoryRepository repositoryRepository,
            TestSuiteRepository testSuiteRepository,
            ClaudeQueryPort claudeQueryPort,
            GitOperationPort gitOperationPort,
            TestGenerationAssembler assembler,
            CodeCompilationService compilationService,
            ClaudeCodePort claudeCodePort,
            GitCommitService gitCommitService) {
        this.testGenerationDomainService = testGenerationDomainService;
        this.repositoryRepository = repositoryRepository;
        this.testSuiteRepository = testSuiteRepository;
        this.claudeQueryPort = claudeQueryPort;
        this.gitOperationPort = gitOperationPort;
        this.assembler = assembler;
        this.compilationService = compilationService;
        this.claudeCodePort = claudeCodePort;
        this.gitCommitService = gitCommitService;
    }

    /**
     * 创建测试生成任务
     * @param requestDTO 测试生成请求
     * @return 任务状态信息
     */
    public TestStatusDTO createTestGenerationTask(TestGenerationRequestDTO requestDTO) {
        try {
            logger.info("Creating test generation task for repository: {}, class: {}",
                       requestDTO.getRepositoryId(), requestDTO.getClassName());

            // 验证请求参数
            validateRequest(requestDTO);

            // 查询仓库信息
            Repository repository = repositoryRepository.findById(requestDTO.getRepositoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Repository not found: " + requestDTO.getRepositoryId()));

            // 获取目标类信息
            JavaClass targetClass = extractJavaClass(repository, requestDTO);

            // 创建测试模板
            TestTemplate template = createTestTemplate(requestDTO);

            // 创建测试套件聚合根
            TestSuite testSuite = new TestSuite(
                    requestDTO.getRepositoryId(),
                    generateSuiteName(requestDTO),
                    requestDTO.getDescription(),
                    targetClass,
                    template,
                    getCurrentUser()
            );

            // 保存测试套件
            TestSuite savedTestSuite = testSuiteRepository.save(testSuite);

            // 缓存任务状态
            String taskId = generateTaskId(savedTestSuite.getId());
            taskCache.put(taskId, savedTestSuite);

            // 异步启动测试生成
            CompletableFuture.runAsync(() -> executeTestGeneration(savedTestSuite, repository, requestDTO));

            logger.info("Test generation task created successfully with ID: {}", taskId);
            return assembler.toTestStatusDTO(savedTestSuite, taskId);

        } catch (ValidationException | ResourceNotFoundException e) {
            logger.warn("Test generation task creation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error creating test generation task", e);
            throw new BusinessRuleException("Failed to create test generation task: " + e.getMessage());
        }
    }

    /**
     * 查询任务状态
     * @param taskId 任务ID
     * @return 任务状态信息
     */
    @Transactional(readOnly = true)
    public TestStatusDTO getTaskStatus(String taskId) {
        logger.debug("Querying status for task: {}", taskId);

        // 首先从缓存中查找
        TestSuite testSuite = taskCache.get(taskId);
        if (testSuite != null) {
            return assembler.toTestStatusDTO(testSuite, taskId);
        }

        // 从数据库查找
        Long suiteId = parseTaskId(taskId);
        testSuite = testSuiteRepository.findById(suiteId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

        // 更新缓存
        taskCache.put(taskId, testSuite);

        return assembler.toTestStatusDTO(testSuite, taskId);
    }

    /**
     * 验证测试套件
     * @param taskId 任务ID
     * @return 验证结果
     */
    public TestStatusDTO validateTestSuite(String taskId) {
        logger.info("Starting validation for task: {}", taskId);

        TestSuite testSuite = getTestSuiteByTaskId(taskId);

        try {
            // 开始验证
            testSuite.startValidation();
            testSuiteRepository.save(testSuite);

            // 异步执行验证
            CompletableFuture.runAsync(() -> executeTestValidation(testSuite));

            // 更新缓存
            taskCache.put(taskId, testSuite);

            logger.info("Test validation started for task: {}", taskId);
            return assembler.toTestStatusDTO(testSuite, taskId);

        } catch (BusinessRuleException e) {
            logger.warn("Cannot start validation for task {}: {}", taskId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error starting validation for task: {}", taskId, e);
            throw new BusinessRuleException("Failed to start validation: " + e.getMessage());
        }
    }

    /**
     * 获取测试生成结果
     * @param taskId 任务ID
     * @return 测试生成结果
     */
    @Transactional(readOnly = true)
    public TestGenerationResultDTO getTestResult(String taskId) {
        logger.debug("Retrieving test result for task: {}", taskId);

        TestSuite testSuite = getTestSuiteByTaskId(taskId);

        if (!testSuite.canExecuteTests()) {
            throw new BusinessRuleException("Test result not available for task: " + taskId);
        }

        return assembler.toTestGenerationResultDTO(testSuite, taskId);
    }

    /**
     * 取消测试生成任务
     * @param taskId 任务ID
     * @return 操作结果
     */
    public TestStatusDTO cancelTask(String taskId) {
        logger.info("Cancelling task: {}", taskId);

        TestSuite testSuite = getTestSuiteByTaskId(taskId);

        if (testSuite.getStatus() == TestSuite.GenerationStatus.COMPLETED) {
            throw new BusinessRuleException("Cannot cancel completed task: " + taskId);
        }

        // 标记为失败（取消）
        testSuite.markAsFailed("Task cancelled by user");
        testSuiteRepository.save(testSuite);

        // 更新缓存
        taskCache.put(taskId, testSuite);

        logger.info("Task cancelled successfully: {}", taskId);
        return assembler.toTestStatusDTO(testSuite, taskId);
    }

    /**
     * 重新生成测试
     * @param taskId 任务ID
     * @return 操作结果
     */
    public TestStatusDTO restartTask(String taskId) {
        logger.info("Restarting task: {}", taskId);

        TestSuite testSuite = getTestSuiteByTaskId(taskId);

        if (!testSuite.canRegenerate()) {
            throw new BusinessRuleException("Cannot restart task in current status: " + testSuite.getStatus());
        }

        // 重新开始生成
        testSuite.restart();
        testSuiteRepository.save(testSuite);

        // 异步重新执行生成
        Repository repository = repositoryRepository.findById(testSuite.getRepositoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found: " + testSuite.getRepositoryId()));

        CompletableFuture.runAsync(() -> executeTestGeneration(testSuite, repository, null));

        // 更新缓存
        taskCache.put(taskId, testSuite);

        logger.info("Task restarted successfully: {}", taskId);
        return assembler.toTestStatusDTO(testSuite, taskId);
    }

    /**
     * 获取用户的测试套件列表
     * @param repositoryId 仓库ID（可选）
     * @return 测试套件列表
     */
    @Transactional(readOnly = true)
    public List<TestSuiteDTO> getUserTestSuites(Long repositoryId) {
        String currentUser = getCurrentUser();
        List<TestSuite> testSuites;

        if (repositoryId != null) {
            testSuites = testSuiteRepository.findByRepositoryIdAndCreatedBy(repositoryId, currentUser);
        } else {
            testSuites = testSuiteRepository.findByCreatedBy(currentUser);
        }

        return testSuites.stream()
                .map(assembler::toTestSuiteDTO)
                .collect(Collectors.toList());
    }

    // 私有方法

    /**
     * 执行测试生成
     */
    private void executeTestGeneration(TestSuite testSuite, Repository repository, TestGenerationRequestDTO requestDTO) {
        try {
            logger.info("Starting test generation for suite: {}", testSuite.getId());

            // 开始生成
            testSuite.startGeneration();
            testSuiteRepository.save(testSuite);

            // 调用领域服务生成测试
            testGenerationDomainService.generateTests(testSuite, repository);

            // 完成生成
            testSuite.completeGeneration();
            testSuiteRepository.save(testSuite);

            logger.info("Test generation completed for suite: {}", testSuite.getId());

        } catch (Exception e) {
            logger.error("Test generation failed for suite: {}", testSuite.getId(), e);
            testSuite.markAsFailed(e.getMessage());
            testSuiteRepository.save(testSuite);
        }
    }

    /**
     * 执行测试验证
     */
    private void executeTestValidation(TestSuite testSuite) {
        try {
            logger.info("Starting test validation for suite: {}", testSuite.getId());

            // 调用领域服务验证测试
            TestSuite.TestExecutionResult result = testGenerationDomainService.validateTests(testSuite);

            // 完成验证
            testSuite.completeValidation(result);
            testSuiteRepository.save(testSuite);

            logger.info("Test validation completed for suite: {}", testSuite.getId());

        } catch (Exception e) {
            logger.error("Test validation failed for suite: {}", testSuite.getId(), e);
            testSuite.markAsFailed("Validation failed: " + e.getMessage());
            testSuiteRepository.save(testSuite);
        }
    }

    /**
     * 验证请求参数
     */
    private void validateRequest(TestGenerationRequestDTO requestDTO) {
        if (requestDTO.getRepositoryId() == null) {
            throw new ValidationException("Repository ID is required");
        }
        if (requestDTO.getClassName() == null || requestDTO.getClassName().trim().isEmpty()) {
            throw new ValidationException("Class name is required");
        }
        if (requestDTO.getBranch() == null || requestDTO.getBranch().trim().isEmpty()) {
            throw new ValidationException("Branch is required");
        }
    }

    /**
     * 提取Java类信息
     */
    private JavaClass extractJavaClass(Repository repository, TestGenerationRequestDTO requestDTO) {
        // 这里应该调用代码分析服务提取类信息
        // 为了演示，返回一个简单的JavaClass对象
        return new JavaClass(
                requestDTO.getClassName(),
                requestDTO.getPackageName() != null ? requestDTO.getPackageName() : "",
                List.of(), // 方法列表需要从代码中提取
                List.of()  // 字段列表需要从代码中提取
        );
    }

    /**
     * 创建测试模板
     */
    private TestTemplate createTestTemplate(TestGenerationRequestDTO requestDTO) {
        TestTemplate.TestType testType = parseTestType(requestDTO.getTestType());
        int qualityLevel = requestDTO.getQualityLevel() != null ? requestDTO.getQualityLevel() : 3;

        return new TestTemplate(
                testType,
                qualityLevel,
                requestDTO.getMockFramework(),
                requestDTO.getAssertionFramework(),
                requestDTO.getAdditionalDependencies()
        );
    }

    /**
     * 解析测试类型
     */
    private TestTemplate.TestType parseTestType(String testType) {
        if (testType == null) {
            return TestTemplate.TestType.BASIC;
        }

        switch (testType.toLowerCase()) {
            case "comprehensive":
                return TestTemplate.TestType.COMPREHENSIVE;
            case "mock":
                return TestTemplate.TestType.MOCK;
            case "integration":
                return TestTemplate.TestType.INTEGRATION;
            default:
                return TestTemplate.TestType.BASIC;
        }
    }

    /**
     * 生成套件名称
     */
    private String generateSuiteName(TestGenerationRequestDTO requestDTO) {
        return requestDTO.getClassName() + "Tests_" + System.currentTimeMillis();
    }

    /**
     * 生成任务ID
     */
    private String generateTaskId(Long suiteId) {
        return "TG_" + suiteId + "_" + System.currentTimeMillis();
    }

    /**
     * 解析任务ID获取套件ID
     */
    private Long parseTaskId(String taskId) {
        try {
            String[] parts = taskId.split("_");
            if (parts.length >= 2) {
                return Long.parseLong(parts[1]);
            }
            throw new IllegalArgumentException("Invalid task ID format");
        } catch (Exception e) {
            throw new ValidationException("Invalid task ID: " + taskId);
        }
    }

    /**
     * 根据任务ID获取测试套件
     */
    private TestSuite getTestSuiteByTaskId(String taskId) {
        TestSuite testSuite = taskCache.get(taskId);
        if (testSuite != null) {
            return testSuite;
        }

        Long suiteId = parseTaskId(taskId);
        testSuite = testSuiteRepository.findById(suiteId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

        taskCache.put(taskId, testSuite);
        return testSuite;
    }

    /**
     * 批量生成测试
     * @param repositoryId 仓库ID
     * @param branch 分支名
     * @param classNames 类名列表
     * @param testType 测试类型
     * @param qualityLevel 质量等级
     * @param gateId Gate ID
     * @param requirement 需求描述
     * @return 批次ID
     */
    public Long startBatchGeneration(Long repositoryId, String branch, List<String> classNames,
                                     String testType, Integer qualityLevel, String gateId, String requirement) {
        logger.info("Starting batch test generation for {} classes in repository: {}", classNames.size(), repositoryId);
        
        Repository repo = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found: " + repositoryId));
        
        Long batchId = System.currentTimeMillis();
        
        CompletableFuture.runAsync(() -> 
            executeBatchGenerationInternal(batchId, repo, branch, classNames, testType, qualityLevel, gateId, requirement)
        );
        
        return batchId;
    }

    /**
     * 获取批次状态
     */
    public Map<String, Object> getBatchStatus(Long batchId) {
        String marker = String.valueOf(batchId);
        List<TestSuite> suites = testSuiteRepository.findBySuiteNameContaining(marker);
        
        long total = suites.size();
        long generating = suites.stream().filter(s -> s.getStatus() == TestSuite.GenerationStatus.GENERATING).count();
        long validating = suites.stream().filter(s -> s.getStatus() == TestSuite.GenerationStatus.VALIDATING).count();
        long completed = suites.stream().filter(s -> s.getStatus() == TestSuite.GenerationStatus.COMPLETED).count();
        long failed = suites.stream().filter(s -> s.getStatus() == TestSuite.GenerationStatus.FAILED).count();
        
        // 计算进度和状态
        // 前端步骤映射：0-25%=分析源码, 25-50%=生成测试, 50-75%=验证编译, 75-100%=执行测试
        String statusTemp;
        int progressTemp;
        String messageTemp;
        
        if (total == 0) {
            // 任务刚创建，还没开始
            statusTemp = "PENDING";
            progressTemp = 0;
            messageTemp = "任务已创建，等待开始";
        } else if (failed > 0) {
            // 有失败的任务
            statusTemp = "FAILED";
            progressTemp = (int) ((completed * 100.0) / total);
            messageTemp = String.format("生成失败: %d/%d 成功", completed, total);
        } else if (completed == total) {
            // 全部完成
            statusTemp = "COMPLETED";
            progressTemp = 100;
            messageTemp = String.format("生成完成: %d/%d 成功", completed, total);
        } else {
            // 正在进行中
            statusTemp = "IN_PROGRESS";
            
            // 根据任务状态分布推算当前阶段
            if (generating > 0 && validating == 0 && completed == 0) {
                // 阶段1: 正在生成测试代码（25-50%）
                progressTemp = 25 + (int) ((generating * 25.0) / total);
                messageTemp = String.format("正在生成测试代码: %d/%d", generating, total);
            } else if (validating > 0) {
                // 阶段2: 正在验证/测试（50-100%）
                int completedAndValidating = (int) (completed + validating);
                progressTemp = 50 + (int) ((completedAndValidating * 50.0) / total);
                messageTemp = String.format("正在验证与测试: %d/%d 完成", completed, total);
            } else if (completed > 0 && completed < total) {
                // 部分完成，其他可能在队列中
                progressTemp = 50 + (int) ((completed * 50.0) / total);
                messageTemp = String.format("正在处理: %d/%d 完成", completed, total);
            } else {
                // 默认：分析源码阶段（0-25%）
                progressTemp = 10;
                messageTemp = "正在分析源码...";
            }
            
            progressTemp = Math.min(99, Math.max(1, progressTemp)); // 确保在1-99%范围内
        }
        
        final String status = statusTemp;
        final int progress = progressTemp;
        final String message = messageTemp;
        
        return new HashMap<String, Object>() {{
            put("status", status);
            put("progress", progress);
            put("message", message);
            put("batchId", batchId);
            put("total", total);
            put("generating", generating);
            put("validating", validating);
            put("completed", completed);
            put("failed", failed);
        }};
    }

    /**
     * 获取批次结果（与 getBatchStatus 功能相同，为兼容 Controller 调用）
     */
    public Map<String, Object> getBatchResult(Long batchId) {
        return getBatchStatus(batchId);
    }

    /**
     * 批量生成的内部实现
     */
    private void executeBatchGenerationInternal(Long batchId, Repository repo, String branch, List<String> classNames,
                                               String testType, Integer qualityLevel, String gateId, String requirement) {
        long start = System.currentTimeMillis();
        java.io.File repoDir = null;
        
        // 立即为每个类创建 TestSuite 记录（GENERATING 状态），以便前端能查询到进度
        List<TestSuite> suites = new ArrayList<>();
        for (String className : classNames) {
            TestTemplate template = new TestTemplate(parseTestType(testType),
                    qualityLevel != null ? qualityLevel : 5, "Mockito", "AssertJ", null);
            JavaClass target = new JavaClass(className, "", List.of(), List.of());
            TestSuite suite = new TestSuite(repo.getId(), className + "Tests_" + batchId, "Batch test generation",
                    target, template, getCurrentUser());
            suite.startGeneration(); // 状态: GENERATING
            testSuiteRepository.save(suite);
            suites.add(suite);
        }
        
        try {
            // 克隆仓库
            repoDir = gitOperationPort.cloneRepository(
                    repo.getUrl(),
                    repo.getCredential() != null ? repo.getCredential().getUsername() : null,
                    repo.getCredential() != null ? repo.getCredential().getPassword() : null,
                    branch
            );

            int success = 0;
            for (int i = 0; i < classNames.size(); i++) {
                String className = classNames.get(i);
                TestSuite suite = suites.get(i);
                
                try {
                    // 定位源码并读取
                    String classPath = findJavaClassFile(repoDir, className);
                    if (classPath == null) {
                        throw new RuntimeException("Class not found: " + className);
                    }
                    String classCode = java.nio.file.Files.readString(
                        java.nio.file.Path.of(repoDir.getAbsolutePath(), classPath)
                    );

                    // 生成测试代码
                    String prompt = buildTestGenerationPrompt(className, classCode, testType, qualityLevel, requirement);
                    ClaudeQueryResponse response = claudeQueryPort.query(prompt);
                    if (!response.isSuccessful()) {
                        throw new RuntimeException("Claude generation failed: " + response.getError());
                    }
                    String testCode = extractTestCodeFromResponse(response.getOutput());

                    // 写入测试文件
                    String testFilePath = convertToTestPath(classPath, className);
                    java.io.File testFile = new java.io.File(repoDir, testFilePath);
                    if (!testFile.getParentFile().exists()) {
                        testFile.getParentFile().mkdirs();
                    }
                    java.nio.file.Files.writeString(testFile.toPath(), testCode, java.nio.charset.StandardCharsets.UTF_8);

                    // 更新 TestSuite: 添加测试用例但先不标记完成（等编译/测试通过后再完成）
                    suite.addTestCase(new com.example.gitreview.domain.testgen.model.entity.TestCase(
                            "GeneratedByClaude", "Auto generated", 
                            com.example.gitreview.domain.testgen.model.entity.TestCase.TestType.UNIT, "", ""));
                    testSuiteRepository.save(suite);

                    success++;
                } catch (Exception ex) {
                    logger.warn("Generate test for {} failed: {}", className, ex.getMessage());
                    suite.markAsFailed("Generation failed: " + ex.getMessage());
                    testSuiteRepository.save(suite);
                }
            }

            // 编译测试，失败时使用 Claude Code CLI 自动修复
            boolean compilationOk = false;
            boolean testsOk = false;
            
            if (success > 0) {
                // 更新所有成功生成的 TestSuite 到 VALIDATING 状态
                for (TestSuite suite : suites) {
                    if (suite.getStatus() == TestSuite.GenerationStatus.GENERATING) {
                        suite.startValidation();
                        testSuiteRepository.save(suite);
                    }
                }
                
                compilationOk = compileTestsWithClaudeCodeFix(repoDir, maxFixRetries);
                
                if (compilationOk) {
                    // 运行测试，失败时使用 Claude Code CLI 自动修复
                    testsOk = runTestsWithClaudeCodeFix(repoDir, maxFixRetries);
                }
            }

            // 尝试本地安全提交并推送
            if (compilationOk && testsOk) {
                String commitMessage = gitCommitService.buildBatchCommitMessage(classNames.size(), gateId, repoDir);
                String username = repo.getCredential() != null ? repo.getCredential().getUsername() : null;
                String password = repo.getCredential() != null ? repo.getCredential().getPassword() : null;
                tryCommitAndPushTests(repoDir, commitMessage, username, password);
            }

            // 更新所有 TestSuite 到最终状态（COMPLETED 或 FAILED）
            for (TestSuite suite : suites) {
                if (suite.getStatus() == TestSuite.GenerationStatus.VALIDATING) {
                    if (compilationOk && testsOk) {
                        suite.completeGeneration();
                    } else {
                        suite.markAsFailed(compilationOk ? "Tests failed" : "Compilation failed");
                    }
                    testSuiteRepository.save(suite);
                }
            }

            long cost = (System.currentTimeMillis() - start) / 1000;
            logger.info("Batch {} finished. success {}/{} in {}s", 
                batchId, success, classNames.size(), cost);
        } catch (Exception e) {
            logger.error("Batch generation failed: {}", e.getMessage(), e);
        }
    }

    /**
     * 使用 Claude Code CLI 修复编译错误
     */
    private boolean compileTestsWithClaudeCodeFix(java.io.File repoDir, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            logger.info("编译测试代码 (尝试 {}/{})", attempt, maxRetries);
            
            CompilationResult compResult = compilationService.compileTests(repoDir);
            
            if (compResult.isSuccess()) {
                logger.info("测试编译成功");
                return true;
            }
            
            // 编译失败，使用 Claude Code CLI 自动修复
            if (attempt < maxRetries) {
                logger.warn("测试编译失败，使用 Claude Code CLI 自动修复 (第 {} 次)", attempt);
                try {
                    boolean fixed = fixCompilationErrorsWithClaudeCode(repoDir, compResult.getOutput());
                    if (!fixed) {
                        logger.warn("Claude Code CLI 无法自动修复编译错误");
                        break;
                    }
                } catch (Exception e) {
                    logger.error("Claude Code CLI 修复过程出错: {}", e.getMessage(), e);
                    break;
                }
            }
        }
        
        logger.error("测试编译失败，已尝试 {} 次", maxRetries);
        return false;
    }

    /**
     * 使用 Claude Code CLI 修复编译错误
     */
    private boolean fixCompilationErrorsWithClaudeCode(java.io.File repoDir, String compilationError) {
        try {
            logger.info("调用 Claude Code CLI 修复编译错误...");
            
            // 构建修复提示词
            String fixPrompt = buildCompilationFixPrompt(compilationError);
            
            // 调用 Claude Code CLI 在仓库目录中执行修复
            com.example.gitreview.infrastructure.claude.ClaudeCodeResult result = 
                claudeCodePort.fixCompilationError(repoDir, "COMPILATION", compilationError, fixPrompt);
            boolean success = result != null && result.isSuccess();
            
            if (success) {
                logger.info("Claude Code CLI 修复完成");
                return true;
            } else {
                logger.warn("Claude Code CLI 修复失败");
                return false;
            }
        } catch (Exception e) {
            logger.error("Claude Code CLI 修复过程异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 运行测试并使用 Claude Code CLI 修复失败的测试
     */
    private boolean runTestsWithClaudeCodeFix(java.io.File repoDir, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            logger.info("运行单元测试 (尝试 {}/{})", attempt, maxRetries);
            
            CompilationResult testResult = compilationService.runTests(repoDir);
            
            if (testResult.isSuccess()) {
                logger.info("所有测试通过");
                return true;
            }
            
            // 测试失败，使用 Claude Code CLI 自动修复
            if (attempt < maxRetries) {
                logger.warn("测试失败，使用 Claude Code CLI 自动修复 (第 {} 次)", attempt);
                try {
                    boolean fixed = fixTestFailuresWithClaudeCode(repoDir, testResult.getOutput());
                    if (!fixed) {
                        logger.warn("Claude Code CLI 无法自动修复测试失败");
                        break;
                    }
                } catch (Exception e) {
                    logger.error("Claude Code CLI 修复测试过程出错: {}", e.getMessage(), e);
                    break;
                }
            }
        }
        
        logger.error("测试失败，已尝试 {} 次", maxRetries);
        return false;
    }

    /**
     * 使用 Claude Code CLI 修复测试失败
     */
    private boolean fixTestFailuresWithClaudeCode(java.io.File repoDir, String testError) {
        try {
            logger.info("调用 Claude Code CLI 修复测试失败...");
            
            // 构建修复提示词
            String fixPrompt = buildTestFixPrompt(testError);
            
            com.example.gitreview.infrastructure.claude.ClaudeCodeResult result = 
                claudeCodePort.fixCompilationError(repoDir, "TEST", testError, fixPrompt);
            boolean success = result != null && result.isSuccess();
            
            if (success) {
                logger.info("Claude Code CLI 修复测试完成");
                return true;
            } else {
                logger.warn("Claude Code CLI 修复测试失败");
                return false;
            }
        } catch (Exception e) {
            logger.error("Claude Code CLI 修复测试过程异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 构建编译错误修复提示词
     */
    private String buildCompilationFixPrompt(String compilationError) {
        return String.format(
            "测试代码编译失败，请修复所有编译错误：\n\n" +
            "编译错误信息：\n```\n%s\n```\n\n" +
            "要求：\n" +
            "1. 仔细分析编译错误，找到所有问题根源\n" +
            "2. 修复所有编译错误，只修改 src/test/java 目录下的测试代码\n" +
            "3. 不要修改 src/main/java 中的源代码\n" +
            "4. 确保修复后的代码可以编译通过\n" +
            "5. 保持测试逻辑和覆盖范围不变",
            compilationError
        );
    }

    /**
     * 构建测试失败修复提示词
     */
    private String buildTestFixPrompt(String testError) {
        return String.format(
            "单元测试失败，请修复所有测试错误：\n\n" +
            "测试失败信息：\n```\n%s\n```\n\n" +
            "要求：\n" +
            "1. 仔细分析测试失败原因（断言失败、异常等）\n" +
            "2. 修复所有测试失败，只修改 src/test/java 目录下的测试代码\n" +
            "3. 不要修改 src/main/java 中的源代码\n" +
            "4. 确保所有测试都能通过\n" +
            "5. 保持测试覆盖范围，不要删除测试用例",
            testError
        );
    }

    /**
     * 提交并推送测试代码
     */
    private void tryCommitAndPushTests(java.io.File repoDir, String commitMessage, String username, String password) {
        try {
            // 本地提交
            tryCommitTests(repoDir, commitMessage);
            
            // 推送到远程
            logger.info("推送测试代码到远程仓库...");
            boolean pushSuccess = gitCommitService.pushToRemoteWithAuth(repoDir, username, password);
            
            if (pushSuccess) {
                logger.info("测试代码已成功推送到远程仓库");
            } else {
                logger.warn("推送测试代码到远程仓库失败");
            }
        } catch (Exception e) {
            logger.warn("提交/推送测试代码失败: {}", e.getMessage());
        }
    }

    /**
     * 更新批次状态
     */
    private void updateBatchStatus(Long batchId, boolean compilationOk, boolean testsOk) {
        try {
            String marker = String.valueOf(batchId);
            List<TestSuite> suites = testSuiteRepository.findBySuiteNameContaining(marker);
            for (TestSuite s : suites) {
                try {
                    if (!compilationOk) {
                        s.markAsFailed("编译失败");
                    } else if (!testsOk) {
                        s.markAsFailed("测试失败");
                    } else {
                        s.startValidation();
                        TestSuite.TestExecutionResult result = new TestSuite.TestExecutionResult(
                                1, 1, 0, 0, 0.0, "Batch validation"
                        );
                        s.completeValidation(result);
                    }
                    testSuiteRepository.save(s);
                } catch (Exception ignore) {
                    logger.warn("Update suite status failed: {}", ignore.getMessage());
                }
            }
        } catch (Exception e) {
            logger.warn("Batch status update error: {}", e.getMessage());
        }
    }

    /**
     * 提交测试代码
     */
    private void tryCommitTests(java.io.File repoDir, String commitMessage) {
        try (Git git = Git.open(repoDir)) {
            git.add().addFilepattern("src/test/java").call();
            java.io.File pom = new java.io.File(repoDir, "pom.xml");
            if (pom.exists()) {
                git.add().addFilepattern("pom.xml").call();
            }
            git.commit().setMessage(commitMessage).call();
            logger.info("Committed generated tests locally.");
        } catch (Exception e) {
            logger.warn("Commit tests failed: {}", e.getMessage());
        }
    }

    /**
     * 查找 Java 类文件
     */
    private String findJavaClassFile(java.io.File repoDir, String className) {
        try {
            // 转换类名为路径
            String relativePath = className.replace('.', '/') + ".java";
            
            // 尝试在多模块项目中查找
            List<String> modules = findPomModules(repoDir);
            for (String module : modules) {
                java.io.File candidate = new java.io.File(repoDir, module + "/src/main/java/" + relativePath);
                if (candidate.exists()) {
                    return module + "/src/main/java/" + relativePath;
                }
            }
            
            // 在根目录查找
            java.io.File rootCandidate = new java.io.File(repoDir, "src/main/java/" + relativePath);
            if (rootCandidate.exists()) {
                return "src/main/java/" + relativePath;
            }
            
            // 递归查找
            final String[] result = new String[1];
            java.nio.file.Files.walk(repoDir.toPath())
                .filter(p -> p.toString().endsWith(className + ".java"))
                .filter(p -> p.toString().contains("src/main/java") || p.toString().contains("src\\main\\java"))
                .findFirst()
                .ifPresent(p -> result[0] = repoDir.toPath().relativize(p).toString());
            
            return result[0];
        } catch (Exception e) {
            logger.error("Error finding class file: {}", className, e);
            return null;
        }
    }

    /**
     * 查找 POM 模块
     */
    private List<String> findPomModules(java.io.File repoDir) {
        List<String> modules = new java.util.ArrayList<>();
        java.io.File rootPom = new java.io.File(repoDir, "pom.xml");
        if (!rootPom.exists()) {
            return modules;
        }
        
        try {
            javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
            org.w3c.dom.Document doc = db.parse(rootPom);
            org.w3c.dom.NodeList moduleNodes = doc.getElementsByTagName("module");
            for (int i = 0; i < moduleNodes.getLength(); i++) {
                modules.add(moduleNodes.item(i).getTextContent().trim());
            }
        } catch (Exception e) {
            logger.warn("Failed to parse root pom.xml: {}", e.getMessage());
        }
        return modules;
    }

    /**
     * 转换为测试路径
     */
    private String convertToTestPath(String classPath, String className) {
        return classPath.replace("src/main/java", "src/test/java")
                .replace("src\\main\\java", "src\\test\\java")
                .replace(".java", "Test.java");
    }

    /**
     * 构建测试生成提示词
     */
    private String buildTestGenerationPrompt(String className, String classCode, String testType, Integer qualityLevel, String requirement) {
        return String.format(
            "请为以下 Java 类生成单元测试：\n\n" +
            "类名：%s\n" +
            "测试类型：%s\n" +
            "质量等级：%d\n" +
            "需求：%s\n\n" +
            "源代码：\n```java\n%s\n```\n\n" +
            "要求：\n" +
            "1. 生成完整的测试类，包含 package 和 import\n" +
            "2. 使用 JUnit 5 和 Mockito 框架\n" +
            "3. 测试覆盖主要方法和边界条件\n" +
            "4. 只返回测试代码，不要解释",
            className, testType, qualityLevel != null ? qualityLevel : 5, 
            requirement != null ? requirement : "无特殊要求",
            classCode
        );
    }

    /**
     * 从响应中提取测试代码
     */
    private String extractTestCodeFromResponse(String response) {
        // 提取 ```java 代码块
        int startIndex = response.indexOf("```java");
        if (startIndex != -1) {
            int endIndex = response.indexOf("```", startIndex + 7);
            if (endIndex != -1) {
                return response.substring(startIndex + 7, endIndex).trim();
            }
        }
        // 提取 ``` 代码块
        startIndex = response.indexOf("```");
        if (startIndex != -1) {
            int endIndex = response.indexOf("```", startIndex + 3);
            if (endIndex != -1) {
                return response.substring(startIndex + 3, endIndex).trim();
            }
        }
        // 提取 public class 开始的内容
        startIndex = response.indexOf("public class");
        if (startIndex != -1) {
            return response.substring(startIndex).trim();
        }
        return response.trim();
    }

    /**
     * 获取当前用户
     */
    private String getCurrentUser() {
        // 实际项目中应该从SecurityContext获取当前用户
        return "system";
    }
}