package com.example.gitreview.domain.shared.model.valueobject;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Branch值对象
 * 表示Git分支的领域概念
 */
public class Branch {

    private static final Pattern VALID_BRANCH_NAME = Pattern.compile("^[a-zA-Z0-9._/-]+$");
    private static final int MAX_BRANCH_NAME_LENGTH = 250;

    private final String name;
    private final Long repositoryId;
    private final BranchType type;

    public enum BranchType {
        LOCAL,
        REMOTE,
        UNKNOWN
    }

    public Branch(String name, Long repositoryId) {
        this.name = validateBranchName(name);
        this.repositoryId = Objects.requireNonNull(repositoryId, "Repository ID cannot be null");
        this.type = determineBranchType(name);
    }

    public Branch(String name, Long repositoryId, BranchType type) {
        this.name = validateBranchName(name);
        this.repositoryId = Objects.requireNonNull(repositoryId, "Repository ID cannot be null");
        this.type = Objects.requireNonNull(type, "Branch type cannot be null");
    }

    /**
     * 验证分支名称
     */
    private String validateBranchName(String branchName) {
        if (branchName == null || branchName.trim().isEmpty()) {
            throw new IllegalArgumentException("Branch name cannot be null or empty");
        }

        String trimmed = branchName.trim();

        if (trimmed.length() > MAX_BRANCH_NAME_LENGTH) {
            throw new IllegalArgumentException("Branch name cannot exceed " + MAX_BRANCH_NAME_LENGTH + " characters");
        }

        // 检查是否包含危险字符
        if (trimmed.contains("..") || trimmed.startsWith("/") || trimmed.endsWith("/") ||
            trimmed.contains("//") || trimmed.contains("\\")) {
            throw new IllegalArgumentException("Branch name contains invalid characters: " + trimmed);
        }

        return trimmed;
    }

    /**
     * 确定分支类型
     */
    private BranchType determineBranchType(String branchName) {
        if (branchName.startsWith("refs/remotes/") || branchName.startsWith("origin/")) {
            return BranchType.REMOTE;
        } else if (branchName.startsWith("refs/heads/") ||
                   (!branchName.contains("/") && !branchName.startsWith("refs/"))) {
            return BranchType.LOCAL;
        } else {
            return BranchType.UNKNOWN;
        }
    }

    /**
     * 获取简化的分支名称（移除refs/前缀）
     */
    public String getSimpleName() {
        if (name.startsWith("refs/heads/")) {
            return name.substring("refs/heads/".length());
        } else if (name.startsWith("refs/remotes/origin/")) {
            return name.substring("refs/remotes/origin/".length());
        } else if (name.startsWith("origin/")) {
            return name.substring("origin/".length());
        }
        return name;
    }

    /**
     * 检查是否为主分支
     */
    public boolean isMainBranch() {
        String simpleName = getSimpleName();
        return "main".equals(simpleName) || "master".equals(simpleName) || "develop".equals(simpleName);
    }

    /**
     * 检查是否为特性分支
     */
    public boolean isFeatureBranch() {
        String simpleName = getSimpleName();
        return simpleName.startsWith("feature/") || simpleName.startsWith("feat/");
    }

    /**
     * 检查是否为发布分支
     */
    public boolean isReleaseBranch() {
        String simpleName = getSimpleName();
        return simpleName.startsWith("release/") || simpleName.startsWith("rel/");
    }

    /**
     * 检查是否为热修复分支
     */
    public boolean isHotfixBranch() {
        String simpleName = getSimpleName();
        return simpleName.startsWith("hotfix/") || simpleName.startsWith("fix/");
    }

    /**
     * 创建远程分支引用
     */
    public Branch asRemoteBranch() {
        if (type == BranchType.REMOTE) {
            return this;
        }
        String remoteName = "refs/remotes/origin/" + getSimpleName();
        return new Branch(remoteName, repositoryId, BranchType.REMOTE);
    }

    /**
     * 创建本地分支引用
     */
    public Branch asLocalBranch() {
        if (type == BranchType.LOCAL) {
            return this;
        }
        String localName = getSimpleName();
        return new Branch(localName, repositoryId, BranchType.LOCAL);
    }

    /**
     * 比较分支优先级（用于排序）
     */
    public int comparePriority(Branch other) {
        // 主分支优先级最高
        if (this.isMainBranch() && !other.isMainBranch()) {
            return -1;
        }
        if (!this.isMainBranch() && other.isMainBranch()) {
            return 1;
        }

        // 本地分支优先于远程分支
        if (this.type == BranchType.LOCAL && other.type == BranchType.REMOTE) {
            return -1;
        }
        if (this.type == BranchType.REMOTE && other.type == BranchType.LOCAL) {
            return 1;
        }

        // 按名称排序
        return this.getSimpleName().compareTo(other.getSimpleName());
    }

    // Getters
    public String getName() {
        return name;
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public BranchType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Branch branch = (Branch) o;
        return Objects.equals(name, branch.name) &&
               Objects.equals(repositoryId, branch.repositoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, repositoryId);
    }

    @Override
    public String toString() {
        return "Branch{" +
                "name='" + name + '\'' +
                ", repositoryId=" + repositoryId +
                ", type=" + type +
                '}';
    }
}