package com.example.gitreview.domain.workflow.repository;

import com.example.gitreview.domain.workflow.model.aggregate.DevelopmentWorkflow;

import java.util.List;
import java.util.Optional;

/**
 * Workflow repository interface.
 *
 * @author zhourui(V33215020)
 * @since 2025/10/05
 */
public interface WorkflowRepository {
    
    /**
     * Save workflow.
     *
     * @param workflow workflow to save
     * @return saved workflow
     */
    DevelopmentWorkflow save(DevelopmentWorkflow workflow);
    
    /**
     * Find workflow by ID.
     *
     * @param id workflow ID
     * @return workflow if found
     */
    Optional<DevelopmentWorkflow> findById(Long id);
    
    /**
     * Find all workflows.
     *
     * @return all workflows
     */
    List<DevelopmentWorkflow> findAll();
    
    /**
     * Delete workflow by ID.
     *
     * @param id workflow ID
     */
    void deleteById(Long id);
}
