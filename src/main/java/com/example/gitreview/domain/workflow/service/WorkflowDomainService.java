package com.example.gitreview.domain.workflow.service;

import com.example.gitreview.domain.workflow.model.TaskStatus;
import com.example.gitreview.domain.workflow.model.aggregate.DevelopmentWorkflow;
import com.example.gitreview.domain.workflow.model.valueobject.Specification;
import com.example.gitreview.domain.workflow.model.valueobject.TechnicalDesign;
import org.springframework.stereotype.Service;

/**
 * Workflow domain service.
 *
 * @author zhourui(V33215020)
 * @since 2025/10/05
 */
@Service
public class WorkflowDomainService {

    /**
     * Validate workflow name.
     *
     * @param name workflow name
     * @return validation result
     */
    public boolean validateWorkflowName(String name) {
        return name != null && !name.trim().isEmpty() && name.length() <= 100;
    }

    /**
     * Calculate workflow progress.
     *
     * @param completedTasks completed task count
     * @param totalTasks total task count
     * @return progress percentage (0-100)
     */
    public int calculateProgress(int completedTasks, int totalTasks) {
        if (totalTasks == 0) {
            return 0;
        }
        return (int) ((double) completedTasks / totalTasks * 100);
    }

    /**
     * 验证规格文档
     *
     * @param specification 规格文档
     */
    public void validateSpecification(Specification specification) {
        if (specification == null) {
            throw new IllegalArgumentException("规格文档不能为空");
        }
        if (specification.getGeneratedContent() == null || specification.getGeneratedContent().trim().isEmpty()) {
            throw new IllegalArgumentException("规格文档内容不能为空");
        }
        if (specification.getGeneratedContent().length() < 100) {
            throw new IllegalArgumentException("规格文档内容过短，至少需要100个字符");
        }
    }

    /**
     * 验证技术方案
     *
     * @param technicalDesign 技术方案
     */
    public void validateTechnicalDesign(TechnicalDesign technicalDesign) {
        if (technicalDesign == null) {
            throw new IllegalArgumentException("技术方案不能为空");
        }
        if (technicalDesign.getContent() == null || technicalDesign.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("技术方案内容不能为空");
        }
        if (technicalDesign.getContent().length() < 100) {
            throw new IllegalArgumentException("技术方案内容过短，至少需要100个字符");
        }
    }

    /**
     * 计算工作流进度
     *
     * @param workflow 工作流
     * @return 进度百分比 (0-100)
     */
    public int calculateProgress(DevelopmentWorkflow workflow) {
        if (workflow.getTaskList() == null || workflow.getTaskList().getTasks().isEmpty()) {
            return 0;
        }

        long completedCount = workflow.getTaskList().getTasks().stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                .count();

        return (int) (completedCount * 100 / workflow.getTaskList().getTasks().size());
    }
}
