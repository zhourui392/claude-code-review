package com.example.gitreview.domain.shared.repository;

import com.example.gitreview.domain.shared.model.aggregate.Repository;
import com.example.gitreview.domain.shared.model.valueobject.GitUrl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * GitRepositoryRepository
 * Git仓库聚合根的仓储接口，定义持久化操作
 */
public interface GitRepositoryRepository {

    /**
     * 保存Git仓库
     * @param repository Git仓库聚合根
     * @return 保存后的仓库
     */
    Repository save(Repository repository);

    /**
     * 根据ID查找Git仓库
     * @param id 仓库ID
     * @return Git仓库（如果存在）
     */
    Optional<Repository> findById(Long id);

    /**
     * 查找所有Git仓库
     * @return 仓库列表
     */
    List<Repository> findAll();

    /**
     * 根据名称查找Git仓库
     * @param name 仓库名称
     * @return Git仓库（如果存在）
     */
    Optional<Repository> findByName(String name);

    /**
     * 根据创建者查找Git仓库
     * @param createdBy 创建者
     * @return Git仓库列表
     */
    List<Repository> findByCreatedBy(String createdBy);

    /**
     * 根据Git URL查找仓库
     * @param gitUrl Git URL
     * @return Git仓库（如果存在）
     */
    Optional<Repository> findByGitUrl(GitUrl gitUrl);

    /**
     * 查找活跃的Git仓库
     * @return 活跃的仓库列表
     */
    List<Repository> findByActive(boolean active);

    /**
     * 根据名称模糊查询
     * @param namePattern 名称模式
     * @return 匹配的仓库列表
     */
    List<Repository> findByNameContaining(String namePattern);

    /**
     * 查找指定时间范围内创建的仓库
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 仓库列表
     */
    List<Repository> findByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 删除Git仓库
     * @param id 仓库ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);

    /**
     * 检查Git仓库是否存在
     * @param id 仓库ID
     * @return 是否存在
     */
    boolean existsById(Long id);

    /**
     * 检查指定名称的仓库是否存在
     * @param name 仓库名称
     * @return 是否存在
     */
    boolean existsByName(String name);

    /**
     * 检查指定Git URL的仓库是否存在
     * @param gitUrl Git URL
     * @return 是否存在
     */
    boolean existsByGitUrl(GitUrl gitUrl);

    /**
     * 获取Git仓库总数
     * @return 总数
     */
    long count();

    /**
     * 获取活跃仓库数量
     * @return 活跃仓库数量
     */
    long countByActive(boolean active);

    /**
     * 批量更新活跃状态
     * @param ids 仓库ID列表
     * @param active 新的活跃状态
     * @return 更新的记录数
     */
    int updateActiveStatusBatch(List<Long> ids, boolean active);

    /**
     * 删除指定时间之前的仓库记录
     * @param beforeTime 时间阈值
     * @return 删除的记录数
     */
    int deleteOldRecords(LocalDateTime beforeTime);

    /**
     * 检查仓库连接状态
     * @param id 仓库ID
     * @return 连接是否成功
     */
    boolean testConnection(Long id);

    /**
     * 获取最近更新的仓库
     * @param limit 限制数量
     * @return 最近更新的仓库列表
     */
    List<Repository> findRecentlyUpdated(int limit);

    /**
     * 根据描述内容查找仓库
     * @param descriptionPattern 描述模式
     * @return 匹配的仓库列表
     */
    List<Repository> findByDescriptionContaining(String descriptionPattern);
}