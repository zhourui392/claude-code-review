package com.example.gitreview.infrastructure.context;

/**
 * 依赖上下文
 * 包含被调用方法的签名和注释
 */
public class DependencyContext {
    private final String methodSignature;
    private final String methodComment;
    private final String className;

    public DependencyContext(String methodSignature, String methodComment, String className) {
        this.methodSignature = methodSignature;
        this.methodComment = methodComment;
        this.className = className;
    }

    /**
     * 转换为签名形式
     */
    public String toSignature() {
        StringBuilder sb = new StringBuilder();

        sb.append("// ").append(className).append("\n");

        if (methodComment != null && !methodComment.isEmpty()) {
            sb.append(methodComment).append("\n");
        }

        sb.append(methodSignature);

        return sb.toString();
    }

    public int getLineCount() {
        int lines = 1; // signature
        if (methodComment != null) {
            lines += methodComment.split("\n").length;
        }
        return Math.min(lines, 5); // 最多5行
    }

    // Getters
    public String getMethodSignature() {
        return methodSignature;
    }

    public String getMethodComment() {
        return methodComment;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public String toString() {
        return "DependencyContext{" +
               "className='" + className + '\'' +
               ", signature='" + methodSignature + '\'' +
               '}';
    }
}
