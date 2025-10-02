package com.example.gitreview.infrastructure.context;

/**
 * 方法级上下文
 * 包含变更方法的完整代码
 */
public class MethodContext {
    private final String methodName;
    private final String methodComment;
    private final String methodCode;
    private final int startLine;
    private final int endLine;
    private final boolean isChanged;

    public MethodContext(String methodName, String methodComment,
                         String methodCode, int startLine, int endLine,
                         boolean isChanged) {
        this.methodName = methodName;
        this.methodComment = methodComment;
        this.methodCode = methodCode;
        this.startLine = startLine;
        this.endLine = endLine;
        this.isChanged = isChanged;
    }

    /**
     * 转换为代码形式
     */
    public String toCode() {
        StringBuilder sb = new StringBuilder();

        if (methodComment != null && !methodComment.isEmpty()) {
            sb.append(methodComment).append("\n");
        }

        sb.append(methodCode);

        if (isChanged) {
            sb.append("  // ⚠️ 此方法包含变更");
        }

        return sb.toString();
    }

    public int getLineCount() {
        return endLine - startLine + 1;
    }

    // Getters
    public String getMethodName() {
        return methodName;
    }

    public String getMethodComment() {
        return methodComment;
    }

    public String getMethodCode() {
        return methodCode;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public boolean isChanged() {
        return isChanged;
    }

    @Override
    public String toString() {
        return "MethodContext{" +
               "methodName='" + methodName + '\'' +
               ", lines=" + getLineCount() +
               ", changed=" + isChanged +
               '}';
    }
}
