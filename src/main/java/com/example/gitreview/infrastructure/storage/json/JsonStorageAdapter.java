package com.example.gitreview.infrastructure.storage.json;

import com.example.gitreview.infrastructure.storage.StoragePort;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.lang.reflect.Method;

/**
 * JSON存储适配器
 * 提供基于JSON文件的通用存储实现
 * @param <T> 实体类型
 */
public class JsonStorageAdapter<T> implements StoragePort<T, Long> {

    private static final Logger logger = LoggerFactory.getLogger(JsonStorageAdapter.class);

    private String storageFile = "data/repositories.json";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicLong idCounter = new AtomicLong(1);

    private Class<T> entityClass;
    private TypeReference<List<T>> typeReference;
    private Function<T, Long> idExtractor;
    private Function<T, T> idSetter;

    public void init() {
        // 配置ObjectMapper
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 确保存储目录存在
        try {
            Path storagePath = Paths.get(storageFile);
            Files.createDirectories(storagePath.getParent());

            // 如果文件不存在,创建空的JSON数组文件
            if (!Files.exists(storagePath)) {
                objectMapper.writeValue(storagePath.toFile(), new ArrayList<>());
                logger.info("Created new storage file: {}", storageFile);
            }

            // 初始化ID计数器
            initializeIdCounter();
            logger.info("JsonStorageAdapter initialized with file: {}", storageFile);
        } catch (IOException e) {
            logger.error("Failed to initialize JsonStorageAdapter", e);
            throw new RuntimeException("Failed to initialize storage", e);
        }
    }

    /**
     * 设置实体类型信息
     * @param entityClass 实体类
     * @param typeReference 类型引用
     * @param idExtractor ID提取器
     * @param idSetter ID设置器（接收实体和ID，返回设置了ID的实体）
     */
    public void configure(Class<T> entityClass, TypeReference<List<T>> typeReference,
                         Function<T, Long> idExtractor, BiFunction<T, Long, T> idSetter) {
        this.entityClass = entityClass;
        this.typeReference = typeReference;
        this.idExtractor = idExtractor;
        this.idSetter = entity -> idSetter.apply(entity, idCounter.get() - 1); // 适配原接口
    }

    /**
     * 设置实体类型信息（简化版本，使用反射设置ID）
     */
    public void configure(Class<T> entityClass, TypeReference<List<T>> typeReference,
                         Function<T, Long> idExtractor, String idSetterMethodName) {
        this.entityClass = entityClass;
        this.typeReference = typeReference;
        this.idExtractor = idExtractor;

        // 使用反射创建ID设置器
        this.idSetter = entity -> {
            try {
                Long newId = idCounter.get() - 1;
                Method setIdMethod = entityClass.getMethod(idSetterMethodName, Long.class);
                setIdMethod.invoke(entity, newId);
                return entity;
            } catch (Exception e) {
                logger.error("Failed to set ID using reflection", e);
                throw new RuntimeException("Failed to set entity ID", e);
            }
        };
    }

    @Override
    public T save(T entity) {
        try {
            List<T> entities = loadEntities();
            Long id = idExtractor.apply(entity);

            if (id == null) {
                // 新实体，生成ID
                Long newId = idCounter.getAndIncrement();
                entity = setEntityId(entity, newId);
                entities.add(entity);
                logger.debug("Creating new entity with ID: {}", newId);
            } else {
                // 更新现有实体
                boolean found = false;
                for (int i = 0; i < entities.size(); i++) {
                    if (idExtractor.apply(entities.get(i)).equals(id)) {
                        entities.set(i, entity);
                        found = true;
                        logger.debug("Updated entity with ID: {}", id);
                        break;
                    }
                }
                if (!found) {
                    entities.add(entity);
                    logger.debug("Added entity with ID: {}", id);
                }
            }

            saveEntities(entities);
            return entity;
        } catch (IOException e) {
            logger.error("Failed to save entity", e);
            throw new RuntimeException("Failed to save entity", e);
        }
    }

    /**
     * 设置实体ID的辅助方法
     */
    private T setEntityId(T entity, Long id) {
        try {
            Method setIdMethod = entityClass.getMethod("setId", Long.class);
            setIdMethod.invoke(entity, id);
            return entity;
        } catch (Exception e) {
            logger.error("Failed to set entity ID", e);
            throw new RuntimeException("Failed to set entity ID", e);
        }
    }

    @Override
    public Optional<T> findById(Long id) {
        try {
            List<T> entities = loadEntities();
            return entities.stream()
                    .filter(entity -> idExtractor.apply(entity).equals(id))
                    .findFirst();
        } catch (IOException e) {
            logger.error("Failed to find entity by ID: {}", id, e);
            return Optional.empty();
        }
    }

    @Override
    public List<T> findAll() {
        try {
            return loadEntities();
        } catch (IOException e) {
            logger.error("Failed to load all entities", e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean deleteById(Long id) {
        try {
            List<T> entities = loadEntities();
            boolean removed = entities.removeIf(entity -> idExtractor.apply(entity).equals(id));
            if (removed) {
                saveEntities(entities);
                logger.debug("Deleted entity with ID: {}", id);
            }
            return removed;
        } catch (IOException e) {
            logger.error("Failed to delete entity by ID: {}", id, e);
            return false;
        }
    }

    @Override
    public boolean existsById(Long id) {
        return findById(id).isPresent();
    }

    @Override
    public long count() {
        try {
            return loadEntities().size();
        } catch (IOException e) {
            logger.error("Failed to count entities", e);
            return 0;
        }
    }

    @Override
    public List<T> saveAll(List<T> entities) {
        List<T> savedEntities = new ArrayList<>();
        for (T entity : entities) {
            savedEntities.add(save(entity));
        }
        return savedEntities;
    }

    @Override
    public void deleteAll() {
        try {
            saveEntities(new ArrayList<>());
            idCounter.set(1);
            logger.info("Deleted all entities");
        } catch (IOException e) {
            logger.error("Failed to delete all entities", e);
            throw new RuntimeException("Failed to delete all entities", e);
        }
    }

    /**
     * 从文件加载实体列表
     */
    private List<T> loadEntities() throws IOException {
        File file = new File(storageFile);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        if (typeReference != null) {
            return objectMapper.readValue(file, typeReference);
        } else {
            // 如果没有配置类型引用，返回空列表
            return new ArrayList<>();
        }
    }

    /**
     * 保存实体列表到文件
     */
    private void saveEntities(List<T> entities) throws IOException {
        File file = new File(storageFile);
        objectMapper.writeValue(file, entities);
    }

    /**
     * 初始化ID计数器
     */
    private void initializeIdCounter() {
        try {
            List<T> entities = loadEntities();
            long maxId = entities.stream()
                    .mapToLong(entity -> idExtractor != null ? idExtractor.apply(entity) : 0L)
                    .max()
                    .orElse(0L);
            idCounter.set(maxId + 1);
            logger.debug("Initialized ID counter to: {}", idCounter.get());
        } catch (Exception e) {
            logger.warn("Failed to initialize ID counter, using default value", e);
            idCounter.set(1);
        }
    }

    /**
     * 获取存储文件路径
     */
    public String getStorageFile() {
        return storageFile;
    }

    /**
     * 设置存储文件路径
     */
    public void setStorageFile(String storageFile) {
        this.storageFile = storageFile;
    }
}