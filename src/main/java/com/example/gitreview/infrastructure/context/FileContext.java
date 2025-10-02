package com.example.gitreview.infrastructure.context;

import java.util.List;

/**
 * 文件上下文
 * 包含文件的类定义、方法、依赖等信息
 */
public class FileContext {
    private final String filePath;
    private final ClassContext classContext;
    private final List<MethodContext> methodContexts;
    private final List<DependencyContext> dependencies;
    private final int totalLines;

    public FileContext(String filePath,
                       ClassContext classContext,
                       List<MethodContext> methodContexts,
                       List<DependencyContext> dependencies) {
        this.filePath = filePath;
        this.classContext = classContext;
        this.methodContexts = methodContexts;
        this.dependencies = dependencies;
        this.totalLines = calculateTotalLines();
    }

    private int calculateTotalLines() {
        int lines = 0;
        if (classContext != null) {
            lines += classContext.getLineCount();
        }
        for (MethodContext method : methodContexts) {
            lines += method.getLineCount();
        }
        for (DependencyContext dep : dependencies) {
            lines += dep.getLineCount();
        }
        return lines;
    }

    /**
     * 转换为适合 Prompt 的格式
     */
    public String toPromptString() {
        StringBuilder sb = new StringBuilder();

        sb.append("## 文件上下文: ").append(filePath).append("\n\n");

        // 类级上下文
        if (classContext != null && !classContext.isEmpty()) {
            sb.append("### 类定义\n");
            sb.append("```java\n");
            sb.append(classContext.toCode());
            sb.append("\n```\n\n");
        }

        // 变更方法
        if (!methodContexts.isEmpty()) {
            sb.append("### 变更方法\n");
            for (MethodContext method : methodContexts) {
                sb.append("```java\n");
                sb.append(method.toCode());
                sb.append("\n```\n\n");
            }
        }

        // 依赖方法
        if (!dependencies.isEmpty()) {
            sb.append("### 依赖方法签名\n");
            for (DependencyContext dep : dependencies) {
                sb.append("```java\n");
                sb.append(dep.toSignature());
                sb.append("\n```\n\n");
            }
        }

        return sb.toString();
    }

    /**
     * 检查上下文是否超过大小限制
     */
    public boolean exceedsLimit(int maxLines) {
        return totalLines > maxLines;
    }

    /**
     * 截断上下文到指定大小
     */
    public FileContext truncate(int maxLines) {
        if (!exceedsLimit(maxLines)) {
            return this;
        }

        // 优先保留类上下文和变更方法，依赖可以截断
        int remaining = maxLines;

        ClassContext truncatedClass = classContext;
        if (classContext != null) {
            remaining -= Math.min(classContext.getLineCount(), 50);
        }

        List<MethodContext> truncatedMethods = methodContexts;
        for (MethodContext method : methodContexts) {
            remaining -= method.getLineCount();
        }

        // 截断依赖
        List<DependencyContext> truncatedDeps = dependencies.subList(0,
                Math.min(dependencies.size(), remaining / 5));

        return new FileContext(filePath, truncatedClass, truncatedMethods, truncatedDeps);
    }

    // Getters
    public String getFilePath() {
        return filePath;
    }

    public ClassContext getClassContext() {
        return classContext;
    }

    public List<MethodContext> getMethodContexts() {
        return methodContexts;
    }

    public List<DependencyContext> getDependencies() {
        return dependencies;
    }

    public int getTotalLines() {
        return totalLines;
    }

    public boolean isEmpty() {
        return (classContext == null || classContext.isEmpty()) &&
               methodContexts.isEmpty() &&
               dependencies.isEmpty();
    }

    @Override
    public String toString() {
        return "FileContext{" +
               "filePath='" + filePath + '\'' +
               ", totalLines=" + totalLines +
               ", methods=" + methodContexts.size() +
               ", dependencies=" + dependencies.size() +
               '}';
    }
}
