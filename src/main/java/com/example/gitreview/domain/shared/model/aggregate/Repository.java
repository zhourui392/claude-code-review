package com.example.gitreview.domain.shared.model.aggregate;

import com.example.gitreview.domain.shared.model.valueobject.Branch;
import com.example.gitreview.domain.shared.model.valueobject.Credential;
import com.example.gitreview.domain.shared.model.valueobject.GitUrl;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Repository聚合根
 * Git仓库的领域模型，封装仓库相关的业务逻辑和规则
 */
public class Repository {

    /**
     * 仓库状态枚举
     */
    public enum RepositoryStatus {
        ACTIVE("活跃"),
        INACTIVE("不活跃"),
        ARCHIVED("已归档"),
        ERROR("错误状态");

        private final String description;

        RepositoryStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private Long id;
    private String name;
    private String description;
    private GitUrl gitUrl;
    private Credential credential;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime lastAccessTime;
    private boolean active;
    private String createdBy;
    private Long accessCount;
    private RepositoryStatus status;

    // 临时字段用于兼容旧JSON格式反序列化
    private transient String tempUsername;
    private transient String tempPassword;

    // 构造函数
    protected Repository() {
        // JPA需要的默认构造函数
    }

    public Repository(String name, String description, GitUrl gitUrl, Credential credential) {
        this(name, description, gitUrl, credential, "system");
    }

    public Repository(String name, String description, GitUrl gitUrl, Credential credential, String createdBy) {
        this.name = validateName(name);
        this.description = description;
        this.gitUrl = Objects.requireNonNull(gitUrl, "GitUrl cannot be null");
        this.credential = Objects.requireNonNull(credential, "Credential cannot be null");
        this.createdBy = createdBy != null ? createdBy : "system";
        this.createTime = LocalDateTime.now();
        this.updateTime = this.createTime;
        this.lastAccessTime = this.createTime;
        this.active = true;
        this.accessCount = 0L;
        this.status = RepositoryStatus.ACTIVE;
    }

    // 业务方法

    /**
     * 测试仓库连接
     * @return 连接是否成功
     */
    public boolean testConnection() {
        // 验证凭据和URL的有效性
        return gitUrl.isValid() && credential.isValid();
    }

    /**
     * 更新仓库信息
     * @param name 新名称
     * @param description 新描述
     */
    public void updateInfo(String name, String description) {
        this.name = validateName(name);
        this.description = description;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 更新凭据
     * @param newCredential 新凭据
     */
    public void updateCredential(Credential newCredential) {
        this.credential = Objects.requireNonNull(newCredential, "Credential cannot be null");
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 激活仓库
     */
    public void activate() {
        this.active = true;
        this.status = RepositoryStatus.ACTIVE;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 停用仓库
     */
    public void deactivate() {
        this.active = false;
        this.status = RepositoryStatus.INACTIVE;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 归档仓库
     */
    public void archive() {
        this.active = false;
        this.status = RepositoryStatus.ARCHIVED;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 标记仓库为错误状态
     */
    public void markAsError() {
        this.status = RepositoryStatus.ERROR;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 记录访问
     */
    public void recordAccess() {
        this.accessCount++;
        this.lastAccessTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 检查是否可以执行Git操作
     * @return 是否可以执行
     */
    public boolean canPerformGitOperations() {
        return active && gitUrl.isValid() && credential.isValid();
    }

    /**
     * 获取仓库显示名称
     * @return 显示名称
     */
    public String getDisplayName() {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        return gitUrl.getRepositoryName();
    }

    /**
     * 创建分支对象
     * @param branchName 分支名称
     * @return Branch对象
     */
    public Branch createBranch(String branchName) {
        return new Branch(branchName, this.id);
    }

    // 私有验证方法
    private String validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Repository name cannot be null or empty");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("Repository name cannot exceed 100 characters");
        }
        return name.trim();
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public GitUrl getGitUrl() {
        return gitUrl;
    }

    /**
     * 获取仓库URL（兼容旧接口）
     * @return URL字符串
     */
    public String getUrl() {
        return gitUrl != null ? gitUrl.getUrl() : null;
    }

    public Credential getCredential() {
        return credential;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public LocalDateTime getLastAccessTime() {
        return lastAccessTime;
    }

    public boolean isActive() {
        return active;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Long getAccessCount() {
        return accessCount;
    }

    public RepositoryStatus getStatus() {
        return status;
    }

    // 用于持久化的setter（仅限基础设施层使用）
    public void setId(Long id) {
        this.id = id;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public void setLastAccessTime(LocalDateTime lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public void setActive(boolean active) {
        this.active = active;
        // 确保反序列化时status与active一致
        if (this.status == null) {
            this.status = active ? RepositoryStatus.ACTIVE : RepositoryStatus.INACTIVE;
        }
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy != null ? createdBy : "system";
    }

    public void setAccessCount(Long accessCount) {
        this.accessCount = accessCount != null ? accessCount : 0L;
    }

    public void setStatus(RepositoryStatus status) {
        this.status = status;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setGitUrl(GitUrl gitUrl) {
        this.gitUrl = gitUrl;
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
    }

    /**
     * 兼容旧JSON格式: repositoryUrl -> gitUrl
     */
    @JsonSetter("repositoryUrl")
    public void setRepositoryUrl(String repositoryUrl) {
        if (repositoryUrl != null && !repositoryUrl.trim().isEmpty()) {
            this.gitUrl = new GitUrl(repositoryUrl);
        }
    }

    /**
     * 兼容旧JSON格式: username + encryptedPassword -> credential
     */
    @JsonSetter("username")
    public void setUsername(String username) {
        this.tempUsername = username;
        buildCredentialFromTemp();
    }

    /**
     * 兼容旧JSON格式: encryptedPassword -> credential
     */
    @JsonSetter("encryptedPassword")
    public void setEncryptedPassword(String encryptedPassword) {
        this.tempPassword = encryptedPassword;
        buildCredentialFromTemp();
    }

    /**
     * 从临时字段构建Credential对象
     */
    private void buildCredentialFromTemp() {
        if (this.tempUsername != null && this.tempPassword != null) {
            this.credential = new Credential(this.tempUsername, this.tempPassword);
            // 清空临时字段
            this.tempUsername = null;
            this.tempPassword = null;
        }
    }

    /**
     * 兼容旧JSON格式: createdAt -> createTime
     */
    @JsonSetter("createdAt")
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createTime = createdAt;
    }

    /**
     * 兼容旧JSON格式: updatedAt -> updateTime
     */
    @JsonSetter("updatedAt")
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updateTime = updatedAt;
    }

    /**
     * 确保反序列化后字段不为null
     */
    @com.fasterxml.jackson.annotation.JsonAnySetter
    public void handleUnknown(String key, Object value) {
        // 忽略未知字段
    }

    /**
     * 反序列化后的初始化
     */
    private void ensureDefaults() {
        if (this.accessCount == null) {
            this.accessCount = 0L;
        }
        if (this.active && this.status == null) {
            this.status = RepositoryStatus.ACTIVE;
        }
        if (this.createTime == null) {
            this.createTime = LocalDateTime.now();
        }
        if (this.updateTime == null) {
            this.updateTime = this.createTime;
        }
        if (this.lastAccessTime == null) {
            this.lastAccessTime = this.createTime;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Repository that = (Repository) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Repository{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", gitUrl=" + gitUrl +
                ", active=" + active +
                '}';
    }
}