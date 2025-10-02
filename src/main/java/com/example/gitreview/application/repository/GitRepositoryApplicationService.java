package com.example.gitreview.application.repository;

import com.example.gitreview.application.repository.dto.GitRepositoryCreateDTO;
import com.example.gitreview.application.repository.dto.GitRepositoryDTO;
import com.example.gitreview.application.repository.dto.GitRepositoryUpdateDTO;
import com.example.gitreview.application.repository.dto.ConnectionTestResultDTO;
import com.example.gitreview.application.repository.dto.BranchListDTO;
import com.example.gitreview.application.repository.assembler.GitRepositoryAssembler;
import com.example.gitreview.domain.shared.model.aggregate.Repository;
import com.example.gitreview.domain.shared.repository.GitRepositoryRepository;
import com.example.gitreview.domain.shared.service.RepositoryDomainService;
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
import java.util.stream.Collectors;

/**
 * Git仓库管理应用服务
 * 协调仓库管理的完整业务流程
 */
@Service
@Transactional
public class GitRepositoryApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(GitRepositoryApplicationService.class);

    private final GitRepositoryRepository repositoryRepository;
    private final RepositoryDomainService repositoryDomainService;
    private final GitOperationPort gitOperationPort;
    private final GitRepositoryAssembler assembler;

    @Autowired
    public GitRepositoryApplicationService(
            GitRepositoryRepository repositoryRepository,
            RepositoryDomainService repositoryDomainService,
            GitOperationPort gitOperationPort,
            GitRepositoryAssembler assembler) {
        this.repositoryRepository = repositoryRepository;
        this.repositoryDomainService = repositoryDomainService;
        this.gitOperationPort = gitOperationPort;
        this.assembler = assembler;
    }

    /**
     * 创建新仓库
     * @param createDTO 创建请求
     * @return 仓库信息
     */
    public GitRepositoryDTO createRepository(GitRepositoryCreateDTO createDTO) {
        try {
            logger.info("Creating new repository: {}", createDTO.getName());

            // 验证请求参数
            validateCreateRequest(createDTO);

            // 检查仓库名称唯一性
            if (repositoryRepository.existsByName(createDTO.getName())) {
                throw new ValidationException("Repository name already exists: " + createDTO.getName());
            }

            // 创建仓库聚合根（通过Assembler）
            Repository repository = assembler.fromCreateDTO(createDTO);

            // 验证仓库连接性
            if (createDTO.getTestConnection() != null && createDTO.getTestConnection()) {
                repositoryDomainService.validateRepositoryAccess(repository);
            }

            // 保存仓库
            Repository savedRepository = repositoryRepository.save(repository);

            logger.info("Repository created successfully with ID: {}", savedRepository.getId());
            return assembler.toGitRepositoryDTO(savedRepository);

        } catch (ValidationException | BusinessRuleException e) {
            logger.warn("Repository creation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error creating repository", e);
            throw new BusinessRuleException("Failed to create repository: " + e.getMessage());
        }
    }

    /**
     * 更新仓库信息
     * @param id 仓库ID
     * @param updateDTO 更新请求
     * @return 更新后的仓库信息
     */
    public GitRepositoryDTO updateRepository(Long id, GitRepositoryUpdateDTO updateDTO) {
        try {
            logger.info("Updating repository: {}", id);

            // 查找仓库
            Repository repository = repositoryRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Repository not found: " + id));

            // 验证更新权限
            validateUpdatePermission(repository);

            // 更新基本信息
            if (updateDTO.getName() != null) {
                // 检查名称唯一性（排除当前仓库）
                if (!repository.getName().equals(updateDTO.getName()) &&
                    repositoryRepository.existsByName(updateDTO.getName())) {
                    throw new ValidationException("Repository name already exists: " + updateDTO.getName());
                }
                repository.updateInfo(updateDTO.getName(), repository.getDescription());
            }

            if (updateDTO.getDescription() != null) {
                repository.updateInfo(repository.getName(), updateDTO.getDescription());
            }

            // 更新URL和凭据暂不支持，需要重新创建Repository聚合根
            if (updateDTO.getUrl() != null) {
                logger.warn("URL update not supported in current domain model for repository: {}", id);
            }

            // 更新凭据
            if (updateDTO.getUsername() != null || updateDTO.getPassword() != null) {
                logger.warn("Credential update not fully supported in current domain model for repository: {}", id);
            }

            // 测试连接（如果请求）
            if (updateDTO.getTestConnection() != null && updateDTO.getTestConnection()) {
                repositoryDomainService.validateRepositoryAccess(repository);
            }

            // 保存更新
            Repository savedRepository = repositoryRepository.save(repository);

            logger.info("Repository updated successfully: {}", id);
            return assembler.toGitRepositoryDTO(savedRepository);

        } catch (ResourceNotFoundException | ValidationException | BusinessRuleException e) {
            logger.warn("Repository update failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error updating repository: {}", id, e);
            throw new BusinessRuleException("Failed to update repository: " + e.getMessage());
        }
    }

    /**
     * 删除仓库
     * @param id 仓库ID
     */
    public void deleteRepository(Long id) {
        try {
            logger.info("Deleting repository: {}", id);

            // 查找仓库
            Repository repository = repositoryRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Repository not found: " + id));

            // 验证删除权限
            validateDeletePermission(repository);

            // 检查是否有关联的代码审查或测试套件
            repositoryDomainService.validateCanDelete(repository);

            // 执行删除
            repositoryRepository.deleteById(repository.getId());

            logger.info("Repository deleted successfully: {}", id);

        } catch (ResourceNotFoundException | BusinessRuleException e) {
            logger.warn("Repository deletion failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error deleting repository: {}", id, e);
            throw new BusinessRuleException("Failed to delete repository: " + e.getMessage());
        }
    }

    /**
     * 获取仓库详情
     * @param id 仓库ID
     * @return 仓库信息
     */
    @Transactional(readOnly = true)
    public GitRepositoryDTO getRepository(Long id) {
        logger.debug("Fetching repository: {}", id);

        Repository repository = repositoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found: " + id));

        return assembler.toGitRepositoryDTO(repository);
    }

    /**
     * 获取所有仓库列表
     * @return 仓库列表
     */
    @Transactional(readOnly = true)
    public List<GitRepositoryDTO> getAllRepositories() {
        logger.debug("Fetching all repositories");

        List<Repository> repositories = repositoryRepository.findAll();
        return repositories.stream()
                .map(assembler::toGitRepositoryDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户的仓库列表
     * @return 用户仓库列表
     */
    @Transactional(readOnly = true)
    public List<GitRepositoryDTO> getUserRepositories() {
        String currentUser = getCurrentUser();
        logger.debug("Fetching repositories for user: {}", currentUser);

        List<Repository> repositories = repositoryRepository.findByCreatedBy(currentUser);
        return repositories.stream()
                .map(assembler::toGitRepositoryDTO)
                .collect(Collectors.toList());
    }

    /**
     * 测试仓库连接
     * @param id 仓库ID
     * @return 连接测试结果
     */
    public ConnectionTestResultDTO testConnection(Long id) {
        try {
            logger.info("Testing connection for repository: {}", id);

            // 查找仓库
            Repository repository = repositoryRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Repository not found: " + id));

            // 执行连接测试
            long startTime = System.currentTimeMillis();
            repositoryDomainService.validateRepositoryAccess(repository);
            long duration = System.currentTimeMillis() - startTime;

            logger.info("Connection test successful for repository: {}", id);
            return new ConnectionTestResultDTO(true, "Connection successful", duration, null);

        } catch (ResourceNotFoundException e) {
            logger.warn("Repository not found for connection test: {}", id);
            throw e;
        } catch (Exception e) {
            logger.warn("Connection test failed for repository {}: {}", id, e.getMessage());
            return new ConnectionTestResultDTO(false, "Connection failed: " + e.getMessage(), 0L, e.getClass().getSimpleName());
        }
    }

    /**
     * 获取远程分支列表
     * @param id 仓库ID
     * @return 分支列表
     */
    public BranchListDTO getRemoteBranches(Long id) {
        try {
            logger.info("Fetching remote branches for repository: {}", id);

            // 查找仓库
            Repository repository = repositoryRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Repository not found: " + id));

            // 获取远程分支
            long startTime = System.currentTimeMillis();
            List<String> branches = gitOperationPort.getRemoteBranches(
                repository.getGitUrl().getUrl(),
                repository.getCredential().getUsername(),
                repository.getCredential().getPassword()
            );
            long duration = System.currentTimeMillis() - startTime;

            logger.info("Fetched {} branches for repository: {}", branches.size(), id);
            return new BranchListDTO(branches, branches.size(), duration);

        } catch (ResourceNotFoundException e) {
            logger.warn("Repository not found for branch fetch: {}", id);
            throw e;
        } catch (Exception e) {
            logger.error("Failed to fetch branches for repository: {}", id, e);
            throw new BusinessRuleException("Failed to fetch remote branches: " + e.getMessage());
        }
    }

    /**
     * 检查仓库状态
     * @param id 仓库ID
     * @return 仓库状态信息
     */
    public GitRepositoryDTO checkRepositoryStatus(Long id) {
        try {
            logger.info("Checking status for repository: {}", id);

            Repository repository = repositoryRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Repository not found: " + id));

            // 检查仓库可访问性
            boolean isAccessible = repositoryDomainService.isRepositoryAccessible(repository);

            // 保存状态更新（Repository健康状态通过canPerformGitOperations计算）
            Repository savedRepository = repositoryRepository.save(repository);

            logger.info("Repository status checked: {} - {}", id, isAccessible ? "healthy" : "unhealthy");
            return assembler.toGitRepositoryDTO(savedRepository);

        } catch (ResourceNotFoundException e) {
            logger.warn("Repository not found for status check: {}", id);
            throw e;
        } catch (Exception e) {
            logger.error("Failed to check repository status: {}", id, e);
            throw new BusinessRuleException("Failed to check repository status: " + e.getMessage());
        }
    }

    // 私有方法

    /**
     * 验证创建请求
     */
    private void validateCreateRequest(GitRepositoryCreateDTO createDTO) {
        if (createDTO.getName() == null || createDTO.getName().trim().isEmpty()) {
            throw new ValidationException("Repository name is required");
        }
        if (createDTO.getUrl() == null || createDTO.getUrl().trim().isEmpty()) {
            throw new ValidationException("Repository URL is required");
        }

        // 验证URL格式
        repositoryDomainService.validateRepositoryUrl(createDTO.getUrl());
    }

    /**
     * 验证更新权限
     */
    private void validateUpdatePermission(Repository repository) {
        String currentUser = getCurrentUser();
        if (!repository.getCreatedBy().equals(currentUser) && !isAdmin()) {
            throw new BusinessRuleException("You do not have permission to update this repository");
        }
    }

    /**
     * 验证删除权限
     */
    private void validateDeletePermission(Repository repository) {
        String currentUser = getCurrentUser();
        if (!repository.getCreatedBy().equals(currentUser) && !isAdmin()) {
            throw new BusinessRuleException("You do not have permission to delete this repository");
        }
    }

    /**
     * 获取当前用户
     */
    private String getCurrentUser() {
        // 实际项目中应该从SecurityContext获取当前用户
        return "system";
    }

    /**
     * 检查是否为管理员
     */
    private boolean isAdmin() {
        // 实际项目中应该检查用户角色
        return false;
    }
}