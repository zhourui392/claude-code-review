package com.example.gitreview.domain.codereview.model.valueobject;

import com.example.gitreview.domain.shared.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.*;

/**
 * CodeDiff值对象
 * 表示代码差异的领域概念
 */
public class CodeDiff {

    private final String baseBranch;
    private final String targetBranch;
    private final String diffContent;
    private final List<FileChange> fileChanges;
    private final DiffStats stats;
    private final LocalDateTime generateTime;
    private final Long repositoryId;

    // 文件变更类型
    public enum ChangeType {
        ADDED,      // 新增
        MODIFIED,   // 修改
        DELETED,    // 删除
        RENAMED     // 重命名
    }

    /**
     * 文件变更信息
     */
    public static class FileChange {
        private final String filePath;
        private final ChangeType changeType;
        private final int addedLines;
        private final int deletedLines;
        private final String oldFilePath; // 用于重命名

        public FileChange(String filePath, ChangeType changeType, int addedLines, int deletedLines) {
            this(filePath, changeType, addedLines, deletedLines, null);
        }

        public FileChange(String filePath, ChangeType changeType, int addedLines, int deletedLines, String oldFilePath) {
            this.filePath = validateFilePath(filePath);
            this.changeType = Objects.requireNonNull(changeType, "Change type cannot be null");
            this.addedLines = Math.max(0, addedLines);
            this.deletedLines = Math.max(0, deletedLines);
            this.oldFilePath = oldFilePath;
        }

        private String validateFilePath(String filePath) {
            if (filePath == null || filePath.trim().isEmpty()) {
                throw new ValidationException("File path cannot be null or empty");
            }
            return filePath.trim();
        }

        public String getFilePath() { return filePath; }
        public ChangeType getChangeType() { return changeType; }
        public int getAddedLines() { return addedLines; }
        public int getDeletedLines() { return deletedLines; }
        public String getOldFilePath() { return oldFilePath; }
        public int getNetLines() { return addedLines - deletedLines; }

        public boolean isJavaFile() {
            return filePath.endsWith(".java");
        }

        public boolean isTestFile() {
            return filePath.contains("test") || filePath.contains("Test");
        }

        public boolean isConfigFile() {
            return filePath.endsWith(".properties") || filePath.endsWith(".yml") ||
                   filePath.endsWith(".yaml") || filePath.endsWith(".xml") ||
                   filePath.endsWith(".json");
        }

        @Override
        public String toString() {
            return String.format("%s: %s (+%d, -%d)", changeType, filePath, addedLines, deletedLines);
        }
    }

    /**
     * 差异统计信息
     */
    public static class DiffStats {
        private final int totalFiles;
        private final int addedLines;
        private final int deletedLines;
        private final int addedFiles;
        private final int deletedFiles;
        private final int modifiedFiles;

        public DiffStats(List<FileChange> fileChanges) {
            this.totalFiles = fileChanges.size();
            this.addedLines = fileChanges.stream().mapToInt(FileChange::getAddedLines).sum();
            this.deletedLines = fileChanges.stream().mapToInt(FileChange::getDeletedLines).sum();
            this.addedFiles = (int) fileChanges.stream().filter(fc -> fc.getChangeType() == ChangeType.ADDED).count();
            this.deletedFiles = (int) fileChanges.stream().filter(fc -> fc.getChangeType() == ChangeType.DELETED).count();
            this.modifiedFiles = (int) fileChanges.stream().filter(fc -> fc.getChangeType() == ChangeType.MODIFIED).count();
        }

        public int getTotalFiles() { return totalFiles; }
        public int getAddedLines() { return addedLines; }
        public int getDeletedLines() { return deletedLines; }
        public int getNetLines() { return addedLines - deletedLines; }
        public int getAddedFiles() { return addedFiles; }
        public int getDeletedFiles() { return deletedFiles; }
        public int getModifiedFiles() { return modifiedFiles; }

        public int getTotalChangedLines() { return addedLines + deletedLines; }

        @Override
        public String toString() {
            return String.format("Files: %d, Lines: +%d/-%d, Changes: +%d ~%d -%d",
                               totalFiles, addedLines, deletedLines, addedFiles, modifiedFiles, deletedFiles);
        }
    }

    // 构造函数
    public CodeDiff(Long repositoryId, String baseBranch, String targetBranch,
                   String diffContent, List<FileChange> fileChanges) {
        this.repositoryId = Objects.requireNonNull(repositoryId, "Repository ID cannot be null");
        this.baseBranch = validateBranch(baseBranch, "Base branch");
        this.targetBranch = validateBranch(targetBranch, "Target branch");
        this.diffContent = validateDiffContent(diffContent);
        this.fileChanges = Collections.unmodifiableList(new ArrayList<>(
            Objects.requireNonNull(fileChanges, "File changes cannot be null")));
        this.stats = new DiffStats(this.fileChanges);
        this.generateTime = LocalDateTime.now();
    }

    // 业务方法

    /**
     * 检查是否为空差异
     * @return 是否为空差异
     */
    public boolean isEmpty() {
        return fileChanges.isEmpty() || stats.getTotalChangedLines() == 0;
    }

    /**
     * 检查是否为大型变更
     * @return 是否为大型变更（超过500行变更）
     */
    public boolean isLargeChange() {
        return stats.getTotalChangedLines() > 500;
    }

    /**
     * 检查是否包含测试文件变更
     * @return 是否包含测试文件变更
     */
    public boolean hasTestChanges() {
        return fileChanges.stream().anyMatch(FileChange::isTestFile);
    }

    /**
     * 检查是否包含Java文件变更
     * @return 是否包含Java文件变更
     */
    public boolean hasJavaChanges() {
        return fileChanges.stream().anyMatch(FileChange::isJavaFile);
    }

    /**
     * 检查是否包含配置文件变更
     * @return 是否包含配置文件变更
     */
    public boolean hasConfigChanges() {
        return fileChanges.stream().anyMatch(FileChange::isConfigFile);
    }

    /**
     * 获取Java文件变更列表
     * @return Java文件变更列表
     */
    public List<FileChange> getJavaFileChanges() {
        return fileChanges.stream()
                .filter(FileChange::isJavaFile)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取测试文件变更列表
     * @return 测试文件变更列表
     */
    public List<FileChange> getTestFileChanges() {
        return fileChanges.stream()
                .filter(FileChange::isTestFile)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取变更复杂度评分
     * @return 复杂度评分 (1-10)
     */
    public int getComplexityScore() {
        int score = 1;

        // 基于文件数量
        if (stats.getTotalFiles() > 10) score += 2;
        else if (stats.getTotalFiles() > 5) score += 1;

        // 基于变更行数
        if (stats.getTotalChangedLines() > 1000) score += 3;
        else if (stats.getTotalChangedLines() > 500) score += 2;
        else if (stats.getTotalChangedLines() > 100) score += 1;

        // 基于文件类型多样性
        boolean hasJava = hasJavaChanges();
        boolean hasTest = hasTestChanges();
        boolean hasConfig = hasConfigChanges();
        int diversity = (hasJava ? 1 : 0) + (hasTest ? 1 : 0) + (hasConfig ? 1 : 0);
        if (diversity >= 3) score += 2;
        else if (diversity == 2) score += 1;

        return Math.min(10, score);
    }

    /**
     * 生成差异摘要
     * @return 差异摘要
     */
    public String generateSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("变更摘要: ");
        summary.append(stats.toString());

        if (hasJavaChanges()) {
            long javaFiles = fileChanges.stream().filter(FileChange::isJavaFile).count();
            summary.append(", Java文件: ").append(javaFiles);
        }

        if (hasTestChanges()) {
            long testFiles = fileChanges.stream().filter(FileChange::isTestFile).count();
            summary.append(", 测试文件: ").append(testFiles);
        }

        summary.append(", 复杂度: ").append(getComplexityScore()).append("/10");

        return summary.toString();
    }

    /**
     * 检查差异内容大小是否适合审查
     * @param maxSizeBytes 最大字节数
     * @return 是否适合审查
     */
    public boolean isSuitableForReview(int maxSizeBytes) {
        return diffContent.getBytes().length <= maxSizeBytes && !isEmpty();
    }

    /**
     * 创建限制大小的副本
     * @param maxSizeBytes 最大字节数
     * @return 限制大小后的CodeDiff
     */
    public CodeDiff withSizeLimit(int maxSizeBytes) {
        if (diffContent.getBytes().length <= maxSizeBytes) {
            return this;
        }

        String truncatedContent = diffContent.substring(0, Math.min(diffContent.length(), maxSizeBytes / 2));
        truncatedContent += "\n\n[注意：差异内容过大，已截取前" + (maxSizeBytes / 2) + "字节进行审查]";

        return new CodeDiff(repositoryId, baseBranch, targetBranch, truncatedContent, fileChanges);
    }

    // 私有验证方法
    private String validateBranch(String branch, String fieldName) {
        if (branch == null || branch.trim().isEmpty()) {
            throw new ValidationException(fieldName + " cannot be null or empty");
        }
        return branch.trim();
    }

    private String validateDiffContent(String diffContent) {
        if (diffContent == null) {
            throw new ValidationException("Diff content cannot be null");
        }
        return diffContent;
    }

    // Getters
    public String getBaseBranch() {
        return baseBranch;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public String getDiffContent() {
        return diffContent;
    }

    public List<FileChange> getFileChanges() {
        return fileChanges;
    }

    public DiffStats getStats() {
        return stats;
    }

    public LocalDateTime getGenerateTime() {
        return generateTime;
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeDiff codeDiff = (CodeDiff) o;
        return Objects.equals(repositoryId, codeDiff.repositoryId) &&
               Objects.equals(baseBranch, codeDiff.baseBranch) &&
               Objects.equals(targetBranch, codeDiff.targetBranch) &&
               Objects.equals(diffContent, codeDiff.diffContent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repositoryId, baseBranch, targetBranch, diffContent);
    }

    @Override
    public String toString() {
        return "CodeDiff{" +
                "repositoryId=" + repositoryId +
                ", baseBranch='" + baseBranch + '\'' +
                ", targetBranch='" + targetBranch + '\'' +
                ", stats=" + stats +
                ", complexity=" + getComplexityScore() +
                '}';
    }
}