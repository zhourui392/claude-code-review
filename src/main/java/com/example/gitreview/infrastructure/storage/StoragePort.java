package com.example.gitreview.infrastructure.storage;

import java.util.List;
import java.util.Optional;

/**
 * 存储端口接口
 * 定义通用的数据存储操作接口
 * @param <T> 实体类型
 * @param <ID> 主键类型
 */
public interface StoragePort<T, ID> {

    /**
     * 保存实体
     * @param entity 实体对象
     * @return 保存后的实体
     */
    T save(T entity);

    /**
     * 根据ID查找实体
     * @param id 主键
     * @return 实体（如果存在）
     */
    Optional<T> findById(ID id);

    /**
     * 查找所有实体
     * @return 实体列表
     */
    List<T> findAll();

    /**
     * 根据ID删除实体
     * @param id 主键
     * @return 是否删除成功
     */
    boolean deleteById(ID id);

    /**
     * 检查实体是否存在
     * @param id 主键
     * @return 是否存在
     */
    boolean existsById(ID id);

    /**
     * 获取总数量
     * @return 实体总数
     */
    long count();

    /**
     * 批量保存
     * @param entities 实体列表
     * @return 保存后的实体列表
     */
    List<T> saveAll(List<T> entities);

    /**
     * 删除所有数据
     */
    void deleteAll();
}