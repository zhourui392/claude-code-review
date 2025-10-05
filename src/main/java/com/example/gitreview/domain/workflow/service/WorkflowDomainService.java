package com.example.gitreview.domain.workflow.service;

import com.example.gitreview.domain.workflow.model.WorkflowStatus;
import com.example.gitreview.domain.workflow.model.aggregate.DevelopmentWorkflow;
import com.example.gitreview.domain.workflow.model.valueobject.Specification;
import com.example.gitreview.domain.workflow.model.valueobject.TechnicalDesign;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 工作流领域服务
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
@Service
public class WorkflowDomainService {

    private static final Map<WorkflowStatus, Set<WorkflowStatus>> TRANSITION_MATRIX;

    static {
        Map<WorkflowStatus, Set<WorkflowStatus>> matrix = new HashMap<>();
        matrix.put(WorkflowStatus.DRAFT, new HashSet<>(Set.of(WorkflowStatus.SPEC_GENERATING)));
        matrix.put(WorkflowStatus.SPEC_GENERATING, new HashSet<>(Set.of(WorkflowStatus.SPEC_GENERATED, WorkflowStatus.FAILED)));
        matrix.put(WorkflowStatus.SPEC_GENERATED, new HashSet<>(Set.of(WorkflowStatus.SPEC_GENERATING, WorkflowStatus.TECH_DESIGN_GENERATING)));
        matrix.put(WorkflowStatus.TECH_DESIGN_GENERATING, new HashSet<>(Set.of(WorkflowStatus.TECH_DESIGN_GENERATED, WorkflowStatus.FAILED)));
        matrix.put(WorkflowStatus.TECH_DESIGN_GENERATED, new HashSet<>(Set.of(WorkflowStatus.TECH_DESIGN_GENERATING, WorkflowStatus.TECH_DESIGN_APPROVED)));
        matrix.put(WorkflowStatus.TECH_DESIGN_APPROVED, new HashSet<>(Set.of(WorkflowStatus.TASK_LIST_GENERATING)));
        matrix.put(WorkflowStatus.TASK_LIST_GENERATING, new HashSet<>(Set.of(WorkflowStatus.TASK_LIST_GENERATED, WorkflowStatus.FAILED)));
        matrix.put(WorkflowStatus.TASK_LIST_GENERATED, new HashSet<>(Set.of(WorkflowStatus.CODE_GENERATING)));
        matrix.put(WorkflowStatus.CODE_GENERATING, new HashSet<>(Set.of(WorkflowStatus.COMPLETED, WorkflowStatus.FAILED)));

        for (WorkflowStatus status : WorkflowStatus.values()) {
            matrix.computeIfAbsent(status, k -> new HashSet<>()).add(WorkflowStatus.CANCELLED);
        }

        TRANSITION_MATRIX = Collections.unmodifiableMap(matrix);
    }

    /**
     * 验证工作流状态转换是否合法
     *
     * @param from 源状态
     * @param to   目标状态
     * @return 是否可以转换
     */
    public boolean isValidTransition(WorkflowStatus from, WorkflowStatus to) {
        return TRANSITION_MATRIX.getOrDefault(from, Set.of()).contains(to);
    }

    /**
     * 验证规格文档完整性
     *
     * @param spec 规格文档
     */
    public void validateSpecification(Specification spec) {
        if (spec == null) {
            throw new IllegalArgumentException("规格文档不能为空");
        }
        if (spec.getPrdContent() == null || spec.getPrdContent().trim().isEmpty()) {
            throw new IllegalArgumentException("PRD内容不能为空");
        }
        if (spec.getGeneratedContent() == null || spec.getGeneratedContent().trim().isEmpty()) {
            throw new IllegalArgumentException("生成的规格文档内容不能为空");
        }
    }

    /**
     * 验证技术方案完整性
     *
     * @param design 技术方案
     */
    public void validateTechnicalDesign(TechnicalDesign design) {
        if (design == null) {
            throw new IllegalArgumentException("技术方案不能为空");
        }
        if (design.getContent() == null || design.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("技术方案内容不能为空");
        }
        if (design.getVersion() < 1) {
            throw new IllegalArgumentException("技术方案版本号必须大于等于1");
        }
    }

    /**
     * 计算工作流总体进度
     *
     * @param workflow 工作流
     * @return 进度百分比 (0-100)
     */
    public int calculateProgress(DevelopmentWorkflow workflow) {
        WorkflowStatus status = workflow.getStatus();

        switch (status) {
            case DRAFT:
                return 0;
            case SPEC_GENERATING:
            case SPEC_GENERATED:
                return 20;
            case TECH_DESIGN_GENERATING:
            case TECH_DESIGN_GENERATED:
            case TECH_DESIGN_APPROVED:
                return 40;
            case TASK_LIST_GENERATING:
            case TASK_LIST_GENERATED:
                return 60;
            case CODE_GENERATING:
                if (workflow.getTaskList() != null) {
                    int taskProgress = workflow.getTaskList().getProgress();
                    return 60 + (taskProgress * 39 / 100);
                }
                return 60;
            case COMPLETED:
                return 100;
            case FAILED:
            case CANCELLED:
                return workflow.getProgress();
            default:
                return 0;
        }
    }
}
