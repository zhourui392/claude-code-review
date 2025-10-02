package com.example.gitreview.infrastructure.context;

import java.util.List;

/**
 * 类级上下文
 * 包含类定义、注释、字段声明
 */
public class ClassContext {
    private final String className;
    private final String classComment;
    private final List<String> fields;
    private final String packageName;
    private final int lineCount;

    public ClassContext(String className, String classComment,
                        List<String> fields, String packageName) {
        this.className = className;
        this.classComment = classComment;
        this.fields = fields;
        this.packageName = packageName;
        this.lineCount = calculateLineCount();
    }

    private int calculateLineCount() {
        int lines = 1; // package
        if (classComment != null) {
            lines += classComment.split("\n").length;
        }
        lines += 1; // class declaration
        lines += fields.size();
        return Math.min(lines, 50); // 最多50行
    }

    /**
     * 转换为代码形式
     */
    public String toCode() {
        StringBuilder sb = new StringBuilder();

        if (packageName != null) {
            sb.append("package ").append(packageName).append(";\n\n");
        }

        if (classComment != null && !classComment.isEmpty()) {
            sb.append(classComment).append("\n");
        }

        sb.append("public class ").append(className).append(" {\n");

        for (String field : fields) {
            sb.append("    ").append(field).append("\n");
        }

        sb.append("    // ... 其他方法 ...\n");
        sb.append("}");

        return sb.toString();
    }

    public boolean isEmpty() {
        return className == null || className.isEmpty();
    }

    // Getters
    public String getClassName() {
        return className;
    }

    public String getClassComment() {
        return classComment;
    }

    public List<String> getFields() {
        return fields;
    }

    public String getPackageName() {
        return packageName;
    }

    public int getLineCount() {
        return lineCount;
    }

    @Override
    public String toString() {
        return "ClassContext{" +
               "className='" + className + '\'' +
               ", fields=" + fields.size() +
               ", lines=" + lineCount +
               '}';
    }
}
