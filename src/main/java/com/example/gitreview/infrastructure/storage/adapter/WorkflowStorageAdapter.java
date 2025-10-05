package com.example.gitreview.infrastructure.storage.adapter;

import com.example.gitreview.domain.workflow.model.aggregate.DevelopmentWorkflow;
import com.example.gitreview.domain.workflow.repository.WorkflowRepository;
import com.example.gitreview.infrastructure.storage.json.JsonStorageAdapter;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 工作流存储适配器
 * 基于JSON文件实现工作流的持久化操作
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
@Component
public class WorkflowStorageAdapter implements WorkflowRepository {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowStorageAdapter.class);

    @Value("${workflow.storage.file:data/workflows.json}")
    private String storageFile;

    private final JsonStorageAdapter<DevelopmentWorkflow> storageAdapter;

    public WorkflowStorageAdapter() {
        this.storageAdapter = new JsonStorageAdapter<>();
    }

    @PostConstruct
    public void init() {
        storageAdapter.configure(
                DevelopmentWorkflow.class,
                new TypeReference<List<DevelopmentWorkflow>>() {},
                DevelopmentWorkflow::getId,
                "setId"
        );
        storageAdapter.setStorageFile(storageFile);
        storageAdapter.init();
        logger.info("WorkflowStorageAdapter initialized with file: {}", storageFile);
    }

    @Override
    public DevelopmentWorkflow save(DevelopmentWorkflow workflow) {
        logger.debug("Saving DevelopmentWorkflow: {}", workflow.getId());
        return storageAdapter.save(workflow);
    }

    @Override
    public Optional<DevelopmentWorkflow> findById(Long id) {
        logger.debug("Finding DevelopmentWorkflow by ID: {}", id);
        return storageAdapter.findById(id);
    }

    @Override
    public List<DevelopmentWorkflow> findAll() {
        logger.debug("Finding all DevelopmentWorkflows");
        return storageAdapter.findAll();
    }

    @Override
    public List<DevelopmentWorkflow> findByRepositoryId(Long repositoryId) {
        logger.debug("Finding DevelopmentWorkflows by repositoryId: {}", repositoryId);
        return storageAdapter.findAll().stream()
                .filter(workflow -> repositoryId.equals(workflow.getRepositoryId()))
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        logger.debug("Deleting DevelopmentWorkflow by ID: {}", id);
        storageAdapter.deleteById(id);
    }
}
