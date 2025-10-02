package com.example.gitreview.application.repository.assembler;

import com.example.gitreview.application.repository.dto.GitRepositoryDTO;
import com.example.gitreview.application.repository.dto.GitRepositoryCreateDTO;
import com.example.gitreview.domain.shared.model.aggregate.Repository;
import com.example.gitreview.domain.shared.model.valueobject.GitUrl;
import com.example.gitreview.domain.shared.model.valueobject.Credential;
import org.springframework.stereotype.Component;

/**
 * Git仓库Assembler
 * 负责领域对象与DTO之间的转换
 */
@Component
public class GitRepositoryAssembler {

    /**
     * 转换为GitRepositoryDTO
     * @param repository 仓库聚合根
     * @return GitRepositoryDTO
     */
    public GitRepositoryDTO toGitRepositoryDTO(Repository repository) {
        GitRepositoryDTO dto = new GitRepositoryDTO();

        dto.setId(repository.getId());
        dto.setName(repository.getName());
        dto.setUrl(repository.getGitUrl().getUrl());
        dto.setDescription(repository.getDescription());
        dto.setCreateTime(repository.getCreateTime());
        dto.setUpdateTime(repository.getUpdateTime());
        dto.setCreatedBy("system"); // 暂时设为system，后续可以从Repository中添加字段

        // 设置凭据信息（包含密码用于Git操作）
        if (repository.getCredential() != null) {
            dto.setUsername(repository.getCredential().getUsername());
            dto.setEncryptedPassword(repository.getCredential().getPassword());
            dto.setHasCredentials(true);
        } else {
            dto.setHasCredentials(false);
        }

        // 设置状态信息（基于active字段）
        dto.setStatus(repository.isActive() ? "ACTIVE" : "INACTIVE");
        dto.setStatusMessage(repository.isActive() ? "Repository is active" : "Repository is inactive");
        dto.setIsHealthy(repository.canPerformGitOperations());
        dto.setLastHealthCheck(repository.getUpdateTime()); // 使用更新时间作为健康检查时间

        // 设置访问统计（暂时使用默认值）
        dto.setLastAccessTime(repository.getLastAccessTime() != null ? repository.getLastAccessTime() : repository.getUpdateTime());
        dto.setAccessCount(repository.getAccessCount() != null ? repository.getAccessCount().intValue() : 0);

        // 设置统计信息（需要从其他服务获取）
        dto.setReviewCount(0); // 实际项目中需要查询相关服务
        dto.setTestSuiteCount(0); // 实际项目中需要查询相关服务

        return dto;
    }

    /**
     * 从CreateDTO创建Repository聚合根
     * @param createDTO 创建DTO
     * @return Repository聚合根
     */
    public Repository fromCreateDTO(GitRepositoryCreateDTO createDTO) {
        GitUrl gitUrl = new GitUrl(createDTO.getUrl());

        Credential credential = null;
        if (createDTO.getUsername() != null && createDTO.getPassword() != null) {
            credential = new Credential(createDTO.getUsername(), createDTO.getPassword());
        } else {
            credential = Credential.createAnonymous();
        }

        return new Repository(
                createDTO.getName(),
                createDTO.getDescription(),
                gitUrl,
                credential
        );
    }
}