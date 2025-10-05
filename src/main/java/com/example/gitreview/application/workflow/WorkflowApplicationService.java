package com.example.gitreview.application.workflow;

import com.example.gitreview.application.workflow.dto.*;
import com.example.gitreview.domain.workflow.exception.WorkflowNotFoundException;
import com.example.gitreview.domain.workflow.model.WorkflowStatus;
import com.example.gitreview.domain.workflow.model.aggregate.DevelopmentWorkflow;
import com.example.gitreview.domain.workflow.model.valueobject.Specification;
import com.example.gitreview.domain.workflow.model.valueobject.Task;
import com.example.gitreview.domain.workflow.model.valueobject.TaskList;
import com.example.gitreview.domain.workflow.model.valueobject.TechnicalDesign;
import com.example.gitreview.domain.workflow.repository.WorkflowRepository;
import com.example.gitreview.domain.workflow.service.WorkflowDomainService;
import com.example.gitreview.infrastructure.claude.ClaudeQueryPort;
import com.example.gitreview.infrastructure.git.GitOperationPort;
import com.example.gitreview.infrastructure.parser.TaskListParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 工作流应用服务
 * 协调工作流的创建、状态管理和各阶段生成逻辑
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
@Service
@Transactional
public class WorkflowApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowApplicationService.class);

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private WorkflowDomainService workflowDomainService;

    @Autowired
    private ClaudeQueryPort claudeQueryPort;

    @Autowired
    private GitOperationPort gitOperationPort;

    @Autowired
    private TaskListParser taskListParser;

    @Value("${workflow.prompts.file:workflow-prompts.properties}")
    private String promptsFile;

    /**
     * 创建工作流
     *
     * @param request 创建工作流请求
     * @return 工作流ID
     */
    public Long createWorkflow(CreateWorkflowRequest request) {
        logger.info("创建工作流: {}, 仓库ID: {}", request.getName(), request.getRepositoryId());

        DevelopmentWorkflow workflow = DevelopmentWorkflow.create(
                request.getName(),
                request.getRepositoryId(),
                request.getCreatedBy()
        );

        DevelopmentWorkflow savedWorkflow = workflowRepository.save(workflow);

        logger.info("工作流创建成功，ID: {}", savedWorkflow.getId());
        return savedWorkflow.getId();
    }

    /**
     * 生成规格文档（异步）
     *
     * @param workflowId 工作流ID
     * @param request    规格文档生成请求
     */
    @Async("workflowExecutor")
    public void generateSpecification(Long workflowId, SpecGenerationRequest request) {
        logger.info("开始生成规格文档，工作流ID: {}", workflowId);

        try {
            DevelopmentWorkflow workflow = loadWorkflow(workflowId);

            workflow.startSpecGeneration();
            workflowRepository.save(workflow);

            String prompt = buildSpecPrompt(request.getPrdContent(), request.getDocumentPaths());

            logger.debug("调用Claude生成规格文档");
            String generatedSpec = claudeQueryPort.query(prompt).getOutput();

            Specification specification = new Specification(
                    request.getPrdContent(),
                    request.getDocumentPaths(),
                    generatedSpec,
                    LocalDateTime.now()
            );

            workflowDomainService.validateSpecification(specification);

            workflow.completeSpecGeneration(specification);
            workflowRepository.save(workflow);

            logger.info("规格文档生成成功，工作流ID: {}", workflowId);

        } catch (Exception e) {
            logger.error("规格文档生成失败，工作流ID: {}", workflowId, e);
            markWorkflowAsFailed(workflowId, "规格文档生成失败: " + e.getMessage());
        }
    }

    /**
     * 获取规格文档
     *
     * @param workflowId 工作流ID
     * @return 规格文档DTO
     */
    @Transactional(readOnly = true)
    public SpecificationDTO getSpecification(Long workflowId) {
        logger.debug("获取规格文档，工作流ID: {}", workflowId);

        DevelopmentWorkflow workflow = loadWorkflow(workflowId);

        if (workflow.getSpecification() == null) {
            throw new IllegalStateException("规格文档尚未生成");
        }

        return new SpecificationDTO(
                workflow.getSpecification().getGeneratedContent(),
                workflow.getSpecification().getPrdContent(),
                workflow.getSpecification().getDocumentPaths(),
                workflow.getSpecification().getGeneratedAt()
        );
    }

    /**
     * 获取工作流状态
     *
     * @param workflowId 工作流ID
     * @return 工作流状态DTO
     */
    @Transactional(readOnly = true)
    public WorkflowStatusDTO getWorkflowStatus(Long workflowId) {
        logger.debug("获取工作流状态，工作流ID: {}", workflowId);

        DevelopmentWorkflow workflow = loadWorkflow(workflowId);

        return new WorkflowStatusDTO(
                workflow.getId(),
                workflow.getName(),
                workflow.getStatus().name(),
                workflow.getProgress(),
                workflow.getCreatedAt(),
                workflow.getUpdatedAt()
        );
    }

    /**
     * 获取工作流进度
     *
     * @param workflowId 工作流ID
     * @return 工作流进度DTO
     */
    @Transactional(readOnly = true)
    public WorkflowProgressDTO getProgress(Long workflowId) {
        logger.debug("获取工作流进度，工作流ID: {}", workflowId);

        DevelopmentWorkflow workflow = loadWorkflow(workflowId);

        int completedTasks = 0;
        int totalTasks = 0;

        if (workflow.getTaskList() != null) {
            totalTasks = workflow.getTaskList().getTasks().size();
            completedTasks = (int) workflow.getTaskList().getTasks().stream()
                    .filter(task -> task.getStatus() == com.example.gitreview.domain.workflow.model.TaskStatus.COMPLETED)
                    .count();
        }

        return new WorkflowProgressDTO(
                workflow.getId(),
                workflow.getStatus().name(),
                workflow.getProgress(),
                workflow.getCurrentStage(),
                completedTasks,
                totalTasks
        );
    }

    /**
     * 取消工作流
     *
     * @param workflowId 工作流ID
     * @param reason     取消原因
     */
    public void cancelWorkflow(Long workflowId, String reason) {
        logger.info("取消工作流，ID: {}, 原因: {}", workflowId, reason);

        DevelopmentWorkflow workflow = loadWorkflow(workflowId);
        workflow.cancel(reason);
        workflowRepository.save(workflow);

        logger.info("工作流已取消，ID: {}", workflowId);
    }

    /**
     * 获取所有工作流
     *
     * @return 工作流状态列表
     */
    @Transactional(readOnly = true)
    public List<WorkflowStatusDTO> getAllWorkflows() {
        logger.debug("获取所有工作流");

        return workflowRepository.findAll().stream()
                .map(workflow -> new WorkflowStatusDTO(
                        workflow.getId(),
                        workflow.getName(),
                        workflow.getStatus().name(),
                        workflow.getProgress(),
                        workflow.getCreatedAt(),
                        workflow.getUpdatedAt()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 生成技术方案（异步）
     *
     * @param workflowId 工作流ID
     */
    @Async("workflowExecutor")
    public void generateTechnicalDesign(Long workflowId) {
        logger.info("开始生成技术方案，工作流ID: {}", workflowId);

        try {
            DevelopmentWorkflow workflow = loadWorkflow(workflowId);

            workflow.startTechDesign();
            workflowRepository.save(workflow);

            String specContent = workflow.getSpecification().getGeneratedContent();

            String repoStructure = extractRepositoryStructure(workflow.getRepositoryId());

            String prompt = buildTechDesignPrompt(specContent, repoStructure);

            logger.debug("调用Claude生成技术方案");
            String generatedDesign = claudeQueryPort.query(prompt).getOutput();

            int newVersion = (workflow.getTechnicalDesign() != null)
                    ? workflow.getTechnicalDesign().getVersion() + 1
                    : 1;

            TechnicalDesign technicalDesign = new TechnicalDesign(
                    generatedDesign,
                    newVersion,
                    false,
                    LocalDateTime.now(),
                    null
            );

            workflowDomainService.validateTechnicalDesign(technicalDesign);

            workflow.completeTechDesign(technicalDesign);
            workflowRepository.save(workflow);

            logger.info("技术方案生成成功，工作流ID: {}, 版本: {}", workflowId, newVersion);

        } catch (Exception e) {
            logger.error("技术方案生成失败，工作流ID: {}", workflowId, e);
            markWorkflowAsFailed(workflowId, "技术方案生成失败: " + e.getMessage());
        }
    }

    /**
     * 获取技术方案
     *
     * @param workflowId 工作流ID
     * @return 技术方案DTO
     */
    @Transactional(readOnly = true)
    public TechnicalDesignDTO getTechnicalDesign(Long workflowId) {
        logger.debug("获取技术方案，工作流ID: {}", workflowId);

        DevelopmentWorkflow workflow = loadWorkflow(workflowId);

        if (workflow.getTechnicalDesign() == null) {
            throw new IllegalStateException("技术方案尚未生成");
        }

        TechnicalDesign design = workflow.getTechnicalDesign();
        return new TechnicalDesignDTO(
                design.getContent(),
                design.getVersion(),
                design.isApproved(),
                design.getCreatedAt(),
                design.getApprovedAt()
        );
    }

    /**
     * 更新技术方案
     *
     * @param workflowId 工作流ID
     * @param content    新的技术方案内容
     */
    public void updateTechnicalDesign(Long workflowId, String content) {
        logger.info("更新技术方案，工作流ID: {}", workflowId);

        DevelopmentWorkflow workflow = loadWorkflow(workflowId);
        workflow.updateTechDesign(content);
        workflowRepository.save(workflow);

        logger.info("技术方案更新成功，工作流ID: {}, 新版本: {}",
                workflowId, workflow.getTechnicalDesign().getVersion());
    }

    /**
     * 批准技术方案
     *
     * @param workflowId 工作流ID
     */
    public void approveTechnicalDesign(Long workflowId) {
        logger.info("批准技术方案，工作流ID: {}", workflowId);

        DevelopmentWorkflow workflow = loadWorkflow(workflowId);
        workflow.approveTechDesign();
        workflowRepository.save(workflow);

        logger.info("技术方案已批准，工作流ID: {}", workflowId);
    }

    /**
     * 生成任务列表（异步）
     *
     * @param workflowId 工作流ID
     */
    @Async("workflowExecutor")
    public void generateTaskList(Long workflowId) {
        logger.info("开始生成任务列表，工作流ID: {}", workflowId);

        try {
            DevelopmentWorkflow workflow = loadWorkflow(workflowId);

            workflow.startTaskListGeneration();
            workflowRepository.save(workflow);

            String techDesignContent = workflow.getTechnicalDesign().getContent();

            String prompt = buildTaskListPrompt(techDesignContent);

            logger.debug("调用Claude生成任务列表");
            String generatedTaskListMd = claudeQueryPort.query(prompt).getOutput();

            List<Task> tasks = taskListParser.parse(generatedTaskListMd);

            logger.info("任务列表解析完成，共 {} 个任务", tasks.size());

            TaskList taskList = new TaskList(
                    generatedTaskListMd,
                    tasks,
                    LocalDateTime.now()
            );

            workflow.completeTaskListGeneration(taskList);
            workflowRepository.save(workflow);

            logger.info("任务列表生成成功，工作流ID: {}, 任务数: {}", workflowId, tasks.size());

        } catch (Exception e) {
            logger.error("任务列表生成失败，工作流ID: {}", workflowId, e);
            markWorkflowAsFailed(workflowId, "任务列表生成失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务列表
     *
     * @param workflowId 工作流ID
     * @return 任务列表DTO
     */
    @Transactional(readOnly = true)
    public TaskListDTO getTaskList(Long workflowId) {
        logger.debug("获取任务列表，工作流ID: {}", workflowId);

        DevelopmentWorkflow workflow = loadWorkflow(workflowId);

        if (workflow.getTaskList() == null) {
            throw new IllegalStateException("任务列表尚未生成");
        }

        TaskList taskList = workflow.getTaskList();

        List<TaskDTO> taskDTOs = taskList.getTasks().stream()
                .map(task -> new TaskDTO(
                        task.getId(),
                        task.getTitle(),
                        task.getDescription(),
                        task.getStatus().name(),
                        task.getDependencies(),
                        task.getGeneratedCode()
                ))
                .collect(Collectors.toList());

        return new TaskListDTO(taskList.getContent(), taskDTOs);
    }

    /**
     * 开始代码生成（异步）
     *
     * @param workflowId 工作流ID
     */
    @Async("workflowExecutor")
    public void startCodeGeneration(Long workflowId) {
        logger.info("开始代码生成，工作流ID: {}", workflowId);

        try {
            DevelopmentWorkflow workflow = loadWorkflow(workflowId);

            workflow.startCodeGeneration();
            workflowRepository.save(workflow);

            TaskList taskList = workflow.getTaskList();
            List<Task> allTasks = taskList.getTasks();

            logger.info("总任务数: {}", allTasks.size());

            int maxIterations = allTasks.size() * 2;
            int iteration = 0;

            while (iteration < maxIterations) {
                List<Task> executableTasks = taskList.getExecutableTasks();

                if (executableTasks.isEmpty()) {
                    logger.info("没有可执行任务，检查是否全部完成");
                    break;
                }

                logger.info("第 {} 轮，可执行任务数: {}", iteration + 1, executableTasks.size());

                for (Task task : executableTasks) {
                    try {
                        logger.info("开始执行任务: {} - {}", task.getId(), task.getTitle());

                        String generatedCode = generateCodeForTask(task, workflow);

                        workflow.completeTask(task.getId(), generatedCode);
                        workflowRepository.save(workflow);

                        taskList = workflow.getTaskList();

                        int progress = workflowDomainService.calculateProgress(workflow);
                        workflow.updateProgress(progress);
                        workflowRepository.save(workflow);

                        logger.info("任务完成: {} - {}, 当前进度: {}%", task.getId(), task.getTitle(), progress);

                    } catch (Exception e) {
                        logger.error("任务执行失败: {} - {}", task.getId(), task.getTitle(), e);

                        Task failedTask = task.fail(e.getMessage());
                        List<Task> updatedTasks = new ArrayList<>();
                        for (Task t : taskList.getTasks()) {
                            if (t.getId().equals(task.getId())) {
                                updatedTasks.add(failedTask);
                            } else {
                                updatedTasks.add(t);
                            }
                        }
                        workflow = loadWorkflow(workflowId);
                        taskList = new TaskList(taskList.getContent(), updatedTasks, taskList.getGeneratedAt());
                        workflowRepository.save(workflow);
                    }
                }

                iteration++;
            }

            workflow = loadWorkflow(workflowId);
            if (workflow.getTaskList().getProgress() == 100) {
                logger.info("所有任务已完成，工作流ID: {}", workflowId);
            } else {
                logger.warn("代码生成未完全完成，工作流ID: {}, 进度: {}%",
                        workflowId, workflow.getTaskList().getProgress());
            }

        } catch (Exception e) {
            logger.error("代码生成失败，工作流ID: {}", workflowId, e);
            markWorkflowAsFailed(workflowId, "代码生成失败: " + e.getMessage());
        }
    }

    /**
     * 为单个任务生成代码
     *
     * @param task     任务
     * @param workflow 工作流
     * @return 生成的代码
     */
    private String generateCodeForTask(Task task, DevelopmentWorkflow workflow) {
        logger.debug("为任务生成代码: {} - {}", task.getId(), task.getTitle());

        String taskDescription = buildTaskDescription(task);

        String codeContext = extractCodeContext(task, workflow);

        String prompt = buildCodeGenerationPrompt(taskDescription, codeContext);

        logger.debug("调用Claude生成代码，任务: {}", task.getId());
        String generatedCode = claudeQueryPort.query(prompt).getOutput();

        logger.debug("代码生成完成，任务: {}, 代码长度: {}", task.getId(), generatedCode.length());

        return generatedCode;
    }

    /**
     * 构建任务描述
     */
    private String buildTaskDescription(Task task) {
        StringBuilder desc = new StringBuilder();
        desc.append("任务ID: ").append(task.getId()).append("\n");
        desc.append("任务标题: ").append(task.getTitle()).append("\n");
        desc.append("目标文件: ").append(task.getTargetFile() != null ? task.getTargetFile() : "无").append("\n");
        desc.append("\n任务描述:\n").append(task.getDescription());
        return desc.toString();
    }

    /**
     * 提取代码上下文
     */
    private String extractCodeContext(Task task, DevelopmentWorkflow workflow) {
        StringBuilder context = new StringBuilder();

        context.append("## 项目架构\n");
        context.append("DDD六边形架构，Spring Boot 3.2 + Java 17\n\n");

        context.append("## 相关领域模型\n");
        if (workflow.getSpecification() != null) {
            context.append("规格文档摘要: ").append(
                    workflow.getSpecification().getGeneratedContent()
                            .substring(0, Math.min(500, workflow.getSpecification().getGeneratedContent().length()))
            ).append("...\n\n");
        }

        if (workflow.getTechnicalDesign() != null) {
            context.append("技术方案摘要: ").append(
                    workflow.getTechnicalDesign().getContent()
                            .substring(0, Math.min(500, workflow.getTechnicalDesign().getContent().length()))
            ).append("...\n\n");
        }

        context.append("## 已完成任务的代码\n");
        List<Task> completedTasks = workflow.getTaskList().getTasks().stream()
                .filter(t -> t.getStatus() == com.example.gitreview.domain.workflow.model.TaskStatus.COMPLETED)
                .filter(t -> t.getGeneratedCode() != null && !t.getGeneratedCode().isEmpty())
                .limit(3)
                .collect(Collectors.toList());

        for (Task completedTask : completedTasks) {
            context.append("任务 ").append(completedTask.getId()).append(": ").append(completedTask.getTitle()).append("\n");
            context.append("```java\n");
            context.append(completedTask.getGeneratedCode()
                    .substring(0, Math.min(1000, completedTask.getGeneratedCode().length())));
            context.append("\n```\n\n");
        }

        return context.toString();
    }

    /**
     * 构建代码生成提示词
     */
    private String buildCodeGenerationPrompt(String taskDescription, String codeContext) {
        return String.format(
                "你是一个资深开发工程师。\n" +
                        "根据以下任务描述和代码上下文，生成完整的代码实现。\n\n" +
                        "要求：\n" +
                        "1. 遵循现有代码风格\n" +
                        "2. 遵循DDD六边形架构分层\n" +
                        "3. 包含必要的Javadoc注释（@author zhourui(V33215020) @since 2025/10/04）\n" +
                        "4. 符合Java最佳实践和Alibaba-P3C规范\n" +
                        "5. 仅返回完整的代码，不要额外说明\n" +
                        "6. 方法长度不超过50行，参数不超过5个\n" +
                        "7. 使用中文注释\n\n" +
                        "任务描述：\n%s\n\n" +
                        "代码上下文：\n%s\n\n" +
                        "请直接输出完整的Java代码。",
                taskDescription, codeContext
        );
    }

    private DevelopmentWorkflow loadWorkflow(Long workflowId) {
        return workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
    }

    private void markWorkflowAsFailed(Long workflowId, String reason) {
        try {
            DevelopmentWorkflow workflow = loadWorkflow(workflowId);
            workflow.markAsFailed(reason);
            workflowRepository.save(workflow);
        } catch (Exception e) {
            logger.error("标记工作流失败时出错，工作流ID: {}", workflowId, e);
        }
    }

    private String buildSpecPrompt(String prdContent, List<String> documentPaths) {
        String documentsContent = documentPaths != null && !documentPaths.isEmpty()
                ? String.join("\n", documentPaths)
                : "无";

        return String.format(
                "你是一个资深软件工程师。\n" +
                        "根据以下PRD和文档资料，生成一份详细的规格文档（spec.md）。\n\n" +
                        "要求：\n" +
                        "1. 明确需求目标和边界\n" +
                        "2. 定义核心功能模块\n" +
                        "3. 列出关键业务规则\n" +
                        "4. 说明外部依赖和接口\n" +
                        "5. 使用Markdown格式，结构清晰\n" +
                        "6. 包含功能清单和验收标准\n\n" +
                        "PRD内容：\n%s\n\n" +
                        "参考文档：\n%s\n\n" +
                        "请生成完整的spec.md内容，使用中文。",
                prdContent, documentsContent
        );
    }

    private String buildTechDesignPrompt(String specContent, String repoStructure) {
        return String.format(
                "你是一个资深架构师。\n" +
                        "根据以下规格文档和代码仓库结构，生成详细的技术方案。\n\n" +
                        "要求：\n" +
                        "1. 分析现有代码架构\n" +
                        "2. 设计领域模型（遵循DDD六边形架构）\n" +
                        "3. 定义API接口（RESTful风格）\n" +
                        "4. 规划实现步骤（按优先级）\n" +
                        "5. 识别技术风险和依赖\n" +
                        "6. 使用Markdown格式，包含代码示例\n" +
                        "7. 符合项目现有技术栈（Spring Boot 3.2 + Java 17）\n\n" +
                        "规格文档：\n%s\n\n" +
                        "代码仓库结构：\n%s\n\n" +
                        "请生成完整的技术方案，使用中文。",
                specContent, repoStructure
        );
    }

    private String buildTaskListPrompt(String techDesignContent) {
        return String.format(
                "你是一个项目经理。\n" +
                        "根据以下技术方案，生成详细的任务列表（tasklist.md）。\n\n" +
                        "要求：\n" +
                        "1. 按优先级分组（P0/P1/P2/P3）\n" +
                        "2. 每个任务包含：ID、标题、描述、依赖、预计工时\n" +
                        "3. 任务颗粒度适中（0.5-1天）\n" +
                        "4. 明确任务依赖关系（使用任务ID）\n" +
                        "5. 使用固定的Markdown格式\n\n" +
                        "任务格式示例：\n" +
                        "### ✅ P0-1: 创建领域层枚举和异常\n" +
                        "**工时**: 0.5天\n" +
                        "**依赖**: 无\n" +
                        "**文件**: domain/workflow/model/\n" +
                        "**任务清单**:\n" +
                        "- [ ] 创建 WorkflowStatus 枚举\n" +
                        "- [ ] 创建 TaskStatus 枚举\n" +
                        "- [ ] 创建异常类\n\n" +
                        "技术方案：\n%s\n\n" +
                        "请生成完整的tasklist.md内容，使用中文，严格遵循上述格式。",
                techDesignContent
        );
    }

    private String extractRepositoryStructure(Long repositoryId) {
        logger.debug("提取仓库结构，仓库ID: {}", repositoryId);

        return "## 代码仓库结构\n\n" +
                "基于Spring Boot 3.2 + Java 17的DDD六边形架构：\n\n" +
                "- domain/ - 领域层\n" +
                "  - workflow/ - 工作流领域\n" +
                "    - model/ - 领域模型\n" +
                "    - service/ - 领域服务\n" +
                "    - repository/ - 仓储接口\n" +
                "- application/ - 应用层\n" +
                "  - workflow/ - 工作流应用服务\n" +
                "- infrastructure/ - 基础设施层\n" +
                "  - storage/ - 存储适配器\n" +
                "  - claude/ - Claude CLI适配器\n" +
                "  - git/ - Git操作适配器\n\n" +
                "注：实际项目中应通过GitOperationPort克隆仓库并分析真实结构";
    }
}
