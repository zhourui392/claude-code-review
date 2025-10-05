package com.example.gitreview.domain.workflow.repository;

import com.example.gitreview.domain.workflow.model.aggregate.DevelopmentWorkflow;

import java.util.List;
import java.util.Optional;

/**
 * 工作流仓储接口
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
public interface WorkflowRepository {

    /**
     * 保存或更新工作流
     *
     * @param workflow 工作流聚合根
     * @return 保存后的工作流（包含生成的ID）
     */
    DevelopmentWorkflow save(DevelopmentWorkflow workflow);

    /**
     * 根据ID查找工作流
     *
     * @param id 工作流ID
     * @return 工作流对象
     */
    Optional<DevelopmentWorkflow> findById(Long id);

    /**
     * 查找所有工作流
     *
     * @return 工作流列表
     */
    List<DevelopmentWorkflow> findAll();

    /**
     * 根据仓库ID查找工作流
     *
     * @param repositoryId 仓库ID
     * @return 工作流列表
     */
    List<DevelopmentWorkflow> findByRepositoryId(Long repositoryId);

    /**
     * 删除工作流
     *
     * @param id 工作流ID
     */
    void delete(Long id);
}
