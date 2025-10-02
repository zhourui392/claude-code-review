package com.example.gitreview.infrastructure.storage.adapter;

import com.example.gitreview.domain.shared.model.aggregate.Repository;
import com.example.gitreview.domain.shared.model.valueobject.GitUrl;
import com.example.gitreview.domain.shared.repository.GitRepositoryRepository;
import com.example.gitreview.infrastructure.storage.json.JsonStorageAdapter;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * GitRepository持久化适配器
 * 基于JSON文件实现GitRepository的持久化操作
 */
@Component
public class GitRepositoryStorageAdapter implements GitRepositoryRepository {

    private static final Logger logger = LoggerFactory.getLogger(GitRepositoryStorageAdapter.class);

    @Value("${json.storage.repository.file:data/repositories.json}")
    private String storageFile;

    private final JsonStorageAdapter<Repository> storageAdapter;

    public GitRepositoryStorageAdapter() {
        this.storageAdapter = new JsonStorageAdapter<>();
    }

    @PostConstruct
    public void init() {
        // 先配置存储适配器
        storageAdapter.configure(
                Repository.class,
                new TypeReference<List<Repository>>() {},
                Repository::getId,
                "setId"
        );
        // 设置存储文件路径
        storageAdapter.setStorageFile(storageFile);
        // 然后初始化
        storageAdapter.init();
        logger.info("GitRepositoryStorageAdapter initialized with file: {}", storageFile);
    }

    @Override
    public Repository save(Repository repository) {
        logger.debug("Saving Repository: {}", repository.getId());
        return storageAdapter.save(repository);
    }

    @Override
    public Optional<Repository> findById(Long id) {
        logger.debug("Finding Repository by ID: {}", id);
        return storageAdapter.findById(id);
    }

    @Override
    public List<Repository> findAll() {
        List<Repository> repositories = storageAdapter.findAll();
        // 确保所有仓库的默认值都正确设置
        repositories.forEach(this::ensureRepositoryDefaults);
        return repositories;
    }

    /**
     * 确保仓库实体的默认值正确设置(兼容旧数据)
     */
    private void ensureRepositoryDefaults(Repository repository) {
        if (repository.getAccessCount() == null) {
            repository.setAccessCount(0L);
        }
        if (repository.getCreatedBy() == null || repository.getCreatedBy().isEmpty()) {
            repository.setCreatedBy("system");
        }
        if (repository.getStatus() == null) {
            repository.setStatus(repository.isActive() ?
                Repository.RepositoryStatus.ACTIVE : Repository.RepositoryStatus.INACTIVE);
        }
        if (repository.getLastAccessTime() == null && repository.getUpdateTime() != null) {
            repository.setLastAccessTime(repository.getUpdateTime());
        }
        if (repository.getLastAccessTime() == null && repository.getCreateTime() != null) {
            repository.setLastAccessTime(repository.getCreateTime());
        }
        // 确保Credential不为null
        if (repository.getCredential() == null) {
            repository.setCredential(com.example.gitreview.domain.shared.model.valueobject.Credential.createAnonymous());
        }
    }

    @Override
    public Optional<Repository> findByName(String name) {
        logger.debug("Finding Repository by name: {}", name);
        return storageAdapter.findAll().stream()
                .filter(repo -> name.equals(repo.getName()))
                .findFirst();
    }

    @Override
    public List<Repository> findByCreatedBy(String createdBy) {
        logger.debug("Finding Repositories by createdBy: {}", createdBy);
        return storageAdapter.findAll().stream()
                .filter(repo -> createdBy.equals(repo.getCreatedBy()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Repository> findByGitUrl(GitUrl gitUrl) {
        logger.debug("Finding Repository by GitUrl: {}", gitUrl);
        return storageAdapter.findAll().stream()
                .filter(repo -> gitUrl.equals(repo.getGitUrl()))
                .findFirst();
    }

    @Override
    public List<Repository> findByActive(boolean active) {
        logger.debug("Finding Repositories by active status: {}", active);
        return storageAdapter.findAll().stream()
                .filter(repo -> repo.isActive() == active)
                .collect(Collectors.toList());
    }

    @Override
    public List<Repository> findByNameContaining(String namePattern) {
        logger.debug("Finding Repositories containing name pattern: {}", namePattern);
        return storageAdapter.findAll().stream()
                .filter(repo -> repo.getName() != null &&
                               repo.getName().toLowerCase().contains(namePattern.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Repository> findByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Finding Repositories between {} and {}", startTime, endTime);
        return storageAdapter.findAll().stream()
                .filter(repo -> {
                    LocalDateTime createTime = repo.getCreateTime();
                    return createTime != null &&
                           !createTime.isBefore(startTime) &&
                           !createTime.isAfter(endTime);
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteById(Long id) {
        logger.debug("Deleting Repository by ID: {}", id);
        return storageAdapter.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return storageAdapter.existsById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return findByName(name).isPresent();
    }

    @Override
    public boolean existsByGitUrl(GitUrl gitUrl) {
        return findByGitUrl(gitUrl).isPresent();
    }

    @Override
    public long count() {
        return storageAdapter.count();
    }

    @Override
    public long countByActive(boolean active) {
        return findByActive(active).size();
    }

    @Override
    public int updateActiveStatusBatch(List<Long> ids, boolean active) {
        logger.debug("Updating active status to {} for {} repositories", active, ids.size());
        int updateCount = 0;

        for (Long id : ids) {
            Optional<Repository> repoOpt = findById(id);
            if (repoOpt.isPresent()) {
                Repository repo = repoOpt.get();
                try {
                    if (active) {
                        repo.activate();
                    } else {
                        repo.deactivate();
                    }
                    save(repo);
                    updateCount++;
                } catch (Exception e) {
                    logger.warn("Failed to update active status for repository {}: {}", id, e.getMessage());
                }
            }
        }

        return updateCount;
    }

    @Override
    public int deleteOldRecords(LocalDateTime beforeTime) {
        logger.debug("Deleting Repositories before {}", beforeTime);
        List<Repository> allRepos = storageAdapter.findAll();
        List<Repository> toDelete = allRepos.stream()
                .filter(repo -> repo.getCreateTime().isBefore(beforeTime))
                .collect(Collectors.toList());

        int deleteCount = 0;
        for (Repository repo : toDelete) {
            if (deleteById(repo.getId())) {
                deleteCount++;
            }
        }

        return deleteCount;
    }

    @Override
    public boolean testConnection(Long id) {
        logger.debug("Testing connection for Repository: {}", id);
        Optional<Repository> repoOpt = findById(id);
        if (repoOpt.isPresent()) {
            Repository repo = repoOpt.get();
            return repo.testConnection();
        }
        return false;
    }

    @Override
    public List<Repository> findRecentlyUpdated(int limit) {
        logger.debug("Finding {} recently updated repositories", limit);
        return storageAdapter.findAll().stream()
                .sorted(Comparator.comparing(Repository::getUpdateTime, Comparator.reverseOrder()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<Repository> findByDescriptionContaining(String descriptionPattern) {
        logger.debug("Finding Repositories containing description pattern: {}", descriptionPattern);
        return storageAdapter.findAll().stream()
                .filter(repo -> repo.getDescription() != null &&
                               repo.getDescription().toLowerCase().contains(descriptionPattern.toLowerCase()))
                .collect(Collectors.toList());
    }
}