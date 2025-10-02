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
import com.example.gitreview.infrastructure.git.GitOperationPort;
import com.example.gitreview.domain.shared.exception.ResourceNotFoundException;
import com.example.gitreview.domain.shared.exception.BusinessRuleException;
import com.example.gitreview.domain.shared.exception.ValidationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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

    // 任务状态缓存，实际项目中应使用Redis等分布式缓存
    private final ConcurrentMap<String, TestSuite> taskCache = new ConcurrentHashMap<>();

    @Autowired
    public TestGenerationApplicationService(
            TestGenerationDomainService testGenerationDomainService,
            GitRepositoryRepository repositoryRepository,
            TestSuiteRepository testSuiteRepository,
            ClaudeQueryPort claudeQueryPort,
            GitOperationPort gitOperationPort,
            TestGenerationAssembler assembler) {
        this.testGenerationDomainService = testGenerationDomainService;
        this.repositoryRepository = repositoryRepository;
        this.testSuiteRepository = testSuiteRepository;
        this.claudeQueryPort = claudeQueryPort;
        this.gitOperationPort = gitOperationPort;
        this.assembler = assembler;
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
     * 获取当前用户
     */
    private String getCurrentUser() {
        // 实际项目中应该从SecurityContext获取当前用户
        return "system";
    }
}