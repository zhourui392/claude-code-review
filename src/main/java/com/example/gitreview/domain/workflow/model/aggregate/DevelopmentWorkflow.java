package com.example.gitreview.domain.workflow.model.aggregate;

import com.example.gitreview.domain.workflow.exception.InvalidWorkflowTransitionException;
import com.example.gitreview.domain.workflow.model.WorkflowStatus;
import com.example.gitreview.domain.workflow.model.valueobject.Specification;
import com.example.gitreview.domain.workflow.model.valueobject.Task;
import com.example.gitreview.domain.workflow.model.valueobject.TaskList;
import com.example.gitreview.domain.workflow.model.valueobject.TechnicalDesign;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 开发工作流聚合根
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
public class DevelopmentWorkflow {

    private Long id;
    private String name;
    private Long repositoryId;
    private WorkflowStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;

    private Specification specification;
    private TechnicalDesign technicalDesign;
    private TaskList taskList;
    private List<Task> codeGenerationTasks;

    private int progress;
    private String currentStage;

    public DevelopmentWorkflow() {
        this.codeGenerationTasks = new ArrayList<>();
    }

    public static DevelopmentWorkflow create(String name, Long repositoryId, String createdBy) {
        DevelopmentWorkflow workflow = new DevelopmentWorkflow();
        workflow.name = name;
        workflow.repositoryId = repositoryId;
        workflow.status = WorkflowStatus.DRAFT;
        workflow.createdAt = LocalDateTime.now();
        workflow.updatedAt = LocalDateTime.now();
        workflow.createdBy = createdBy;
        workflow.progress = 0;
        workflow.currentStage = "草稿";
        return workflow;
    }

    public void startSpecGeneration() {
        validateTransition(WorkflowStatus.SPEC_GENERATING);
        this.status = WorkflowStatus.SPEC_GENERATING;
        this.currentStage = "生成规格文档中";
        this.updatedAt = LocalDateTime.now();
    }

    public void completeSpecGeneration(Specification spec) {
        validateTransition(WorkflowStatus.SPEC_GENERATED);
        this.specification = spec;
        this.status = WorkflowStatus.SPEC_GENERATED;
        this.currentStage = "规格文档已生成";
        this.progress = 20;
        this.updatedAt = LocalDateTime.now();
    }

    public void startTechDesign() {
        validateTransition(WorkflowStatus.TECH_DESIGN_GENERATING);
        this.status = WorkflowStatus.TECH_DESIGN_GENERATING;
        this.currentStage = "生成技术方案中";
        this.updatedAt = LocalDateTime.now();
    }

    public void completeTechDesign(TechnicalDesign design) {
        validateTransition(WorkflowStatus.TECH_DESIGN_GENERATED);
        this.technicalDesign = design;
        this.status = WorkflowStatus.TECH_DESIGN_GENERATED;
        this.currentStage = "技术方案已生成";
        this.progress = 40;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateTechDesign(String content) {
        if (this.status != WorkflowStatus.TECH_DESIGN_GENERATED) {
            throw new InvalidWorkflowTransitionException(this.status, WorkflowStatus.TECH_DESIGN_GENERATED);
        }
        this.technicalDesign = this.technicalDesign.createNewVersion(content);
        this.updatedAt = LocalDateTime.now();
    }

    public void approveTechDesign() {
        validateTransition(WorkflowStatus.TECH_DESIGN_APPROVED);
        this.technicalDesign = this.technicalDesign.approve();
        this.status = WorkflowStatus.TECH_DESIGN_APPROVED;
        this.currentStage = "技术方案已批准";
        this.updatedAt = LocalDateTime.now();
    }

    public void startTaskListGeneration() {
        validateTransition(WorkflowStatus.TASK_LIST_GENERATING);
        this.status = WorkflowStatus.TASK_LIST_GENERATING;
        this.currentStage = "生成任务列表中";
        this.updatedAt = LocalDateTime.now();
    }

    public void completeTaskListGeneration(TaskList tasks) {
        validateTransition(WorkflowStatus.TASK_LIST_GENERATED);
        this.taskList = tasks;
        this.status = WorkflowStatus.TASK_LIST_GENERATED;
        this.currentStage = "任务列表已生成";
        this.progress = 60;
        this.updatedAt = LocalDateTime.now();
    }

    public void startCodeGeneration() {
        validateTransition(WorkflowStatus.CODE_GENERATING);
        this.status = WorkflowStatus.CODE_GENERATING;
        this.currentStage = "代码生成中";
        this.updatedAt = LocalDateTime.now();
    }

    public void completeTask(String taskId, String code) {
        Task task = this.taskList.getTaskById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + taskId));

        Task completedTask = task.complete(code);

        List<Task> updatedTasks = new ArrayList<>();
        for (Task t : this.taskList.getTasks()) {
            if (t.getId().equals(taskId)) {
                updatedTasks.add(completedTask);
            } else {
                updatedTasks.add(t);
            }
        }

        this.taskList = new TaskList(this.taskList.getContent(), updatedTasks, this.taskList.getGeneratedAt());
        this.progress = 60 + (this.taskList.getProgress() * 39 / 100);
        this.updatedAt = LocalDateTime.now();

        if (this.taskList.getProgress() == 100) {
            this.status = WorkflowStatus.COMPLETED;
            this.currentStage = "全部完成";
            this.progress = 100;
        }
    }

    public void markAsFailed(String reason) {
        this.status = WorkflowStatus.FAILED;
        this.currentStage = "失败: " + reason;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel(String reason) {
        this.status = WorkflowStatus.CANCELLED;
        this.currentStage = "已取消: " + reason;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateProgress(int progress) {
        this.progress = progress;
        this.updatedAt = LocalDateTime.now();
    }

    private void validateTransition(WorkflowStatus targetStatus) {
        boolean isValid = false;

        switch (this.status) {
            case DRAFT:
                isValid = targetStatus == WorkflowStatus.SPEC_GENERATING;
                break;
            case SPEC_GENERATING:
                isValid = targetStatus == WorkflowStatus.SPEC_GENERATED || targetStatus == WorkflowStatus.FAILED;
                break;
            case SPEC_GENERATED:
                isValid = targetStatus == WorkflowStatus.SPEC_GENERATING ||
                         targetStatus == WorkflowStatus.TECH_DESIGN_GENERATING;
                break;
            case TECH_DESIGN_GENERATING:
                isValid = targetStatus == WorkflowStatus.TECH_DESIGN_GENERATED || targetStatus == WorkflowStatus.FAILED;
                break;
            case TECH_DESIGN_GENERATED:
                isValid = targetStatus == WorkflowStatus.TECH_DESIGN_GENERATING ||
                         targetStatus == WorkflowStatus.TECH_DESIGN_APPROVED;
                break;
            case TECH_DESIGN_APPROVED:
                isValid = targetStatus == WorkflowStatus.TASK_LIST_GENERATING;
                break;
            case TASK_LIST_GENERATING:
                isValid = targetStatus == WorkflowStatus.TASK_LIST_GENERATED || targetStatus == WorkflowStatus.FAILED;
                break;
            case TASK_LIST_GENERATED:
                isValid = targetStatus == WorkflowStatus.CODE_GENERATING;
                break;
            case CODE_GENERATING:
                isValid = targetStatus == WorkflowStatus.COMPLETED || targetStatus == WorkflowStatus.FAILED;
                break;
        }

        if (!isValid) {
            throw new InvalidWorkflowTransitionException(this.status, targetStatus);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Specification getSpecification() {
        return specification;
    }

    public TechnicalDesign getTechnicalDesign() {
        return technicalDesign;
    }

    public TaskList getTaskList() {
        return taskList;
    }

    public List<Task> getCodeGenerationTasks() {
        return codeGenerationTasks;
    }

    public int getProgress() {
        return progress;
    }

    public String getCurrentStage() {
        return currentStage;
    }
}
