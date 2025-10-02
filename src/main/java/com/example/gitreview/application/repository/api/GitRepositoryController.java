package com.example.gitreview.application.repository.api;

import com.example.gitreview.application.repository.GitRepositoryApplicationService;
import com.example.gitreview.application.repository.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Git仓库REST API控制器
 */
@RestController
@RequestMapping("/api/repositories")
@Validated
@CrossOrigin(origins = "*")
public class GitRepositoryController {

    private static final Logger logger = LoggerFactory.getLogger(GitRepositoryController.class);

    @Autowired
    private GitRepositoryApplicationService applicationService;

    /**
     * 获取所有仓库
     */
    @GetMapping
    public ResponseEntity<List<GitRepositoryDTO>> getAllRepositories() {
        logger.info("Getting all repositories");
        List<GitRepositoryDTO> repositories = applicationService.getAllRepositories();
        return ResponseEntity.ok(repositories);
    }

    /**
     * 获取单个仓库
     */
    @GetMapping("/{id}")
    public ResponseEntity<GitRepositoryDTO> getRepository(@PathVariable Long id) {
        logger.info("Getting repository: {}", id);
        GitRepositoryDTO repository = applicationService.getRepository(id);
        return ResponseEntity.ok(repository);
    }

    /**
     * 创建仓库
     */
    @PostMapping
    public ResponseEntity<GitRepositoryDTO> createRepository(@Valid @RequestBody GitRepositoryCreateDTO createDTO) {
        logger.info("Creating repository: {}", createDTO.getName());
        GitRepositoryDTO repository = applicationService.createRepository(createDTO);
        return ResponseEntity.ok(repository);
    }

    /**
     * 更新仓库
     */
    @PutMapping("/{id}")
    public ResponseEntity<GitRepositoryDTO> updateRepository(
            @PathVariable Long id,
            @Valid @RequestBody GitRepositoryUpdateDTO updateDTO) {
        logger.info("Updating repository: {}", id);
        GitRepositoryDTO repository = applicationService.updateRepository(id, updateDTO);
        return ResponseEntity.ok(repository);
    }

    /**
     * 删除仓库
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRepository(@PathVariable Long id) {
        logger.info("Deleting repository: {}", id);
        applicationService.deleteRepository(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 测试仓库连接
     */
    @PostMapping("/{id}/test-connection")
    public ResponseEntity<ConnectionTestResultDTO> testConnection(@PathVariable Long id) {
        logger.info("Testing connection for repository: {}", id);
        ConnectionTestResultDTO result = applicationService.testConnection(id);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取远程分支列表
     */
    @GetMapping("/{id}/remote-branches")
    public ResponseEntity<BranchListDTO> getRemoteBranches(@PathVariable Long id) {
        logger.info("Getting remote branches for repository: {}", id);
        BranchListDTO branches = applicationService.getRemoteBranches(id);
        return ResponseEntity.ok(branches);
    }

    /**
     * 检查仓库状态
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<GitRepositoryDTO> checkRepositoryStatus(@PathVariable Long id) {
        logger.info("Checking status for repository: {}", id);
        GitRepositoryDTO status = applicationService.checkRepositoryStatus(id);
        return ResponseEntity.ok(status);
    }

    /**
     * 获取当前用户的仓库
     */
    @GetMapping("/user/current")
    public ResponseEntity<List<GitRepositoryDTO>> getUserRepositories() {
        logger.info("Getting repositories for current user");
        List<GitRepositoryDTO> repositories = applicationService.getUserRepositories();
        return ResponseEntity.ok(repositories);
    }
}
