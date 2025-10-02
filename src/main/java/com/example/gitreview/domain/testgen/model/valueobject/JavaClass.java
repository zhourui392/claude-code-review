package com.example.gitreview.domain.testgen.model.valueobject;

import com.example.gitreview.domain.shared.exception.ValidationException;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;

/**
 * JavaClass值对象
 * 表示要为其生成测试的Java类信息
 */
public class JavaClass {

    private final String simpleName;
    private final String packageName;
    private final List<JavaMethod> methods;
    private final List<JavaField> fields;

    public JavaClass(String simpleName, String packageName, List<JavaMethod> methods, List<JavaField> fields) {
        this.simpleName = validateSimpleName(simpleName);
        this.packageName = validatePackageName(packageName);
        this.methods = methods != null ? new ArrayList<>(methods) : new ArrayList<>();
        this.fields = fields != null ? new ArrayList<>(fields) : new ArrayList<>();
    }

    /**
     * 获取完整类名
     * @return 完整类名
     */
    public String getFullName() {
        if (packageName == null || packageName.trim().isEmpty()) {
            return simpleName;
        }
        return packageName + "." + simpleName;
    }

    /**
     * 获取方法数量
     * @return 方法数量
     */
    public int getMethodCount() {
        return methods.size();
    }

    /**
     * 获取字段数量
     * @return 字段数量
     */
    public int getFieldCount() {
        return fields.size();
    }

    /**
     * 检查是否为接口
     * @return 是否为接口
     */
    public boolean isInterface() {
        // 简单检查，实际项目中应该从源代码或反射信息中获取
        return simpleName.startsWith("I") && Character.isUpperCase(simpleName.charAt(1));
    }

    /**
     * 检查是否为抽象类
     * @return 是否为抽象类
     */
    public boolean isAbstract() {
        // 简单检查，实际项目中应该从源代码或反射信息中获取
        return simpleName.contains("Abstract") || simpleName.endsWith("Base");
    }

    /**
     * 检查是否有复杂方法
     * @return 是否有复杂方法
     */
    public boolean hasComplexMethods() {
        return methods.stream().anyMatch(JavaMethod::isComplex);
    }

    /**
     * 检查是否有异步方法
     * @return 是否有异步方法
     */
    public boolean hasAsyncMethods() {
        return methods.stream().anyMatch(JavaMethod::isAsync);
    }

    /**
     * 检查是否有静态方法
     * @return 是否有静态方法
     */
    public boolean hasStaticMethods() {
        return methods.stream().anyMatch(JavaMethod::isStatic);
    }

    /**
     * 获取公共方法列表
     * @return 公共方法列表
     */
    public List<JavaMethod> getPublicMethods() {
        return methods.stream()
                .filter(JavaMethod::isPublic)
                .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
    }

    /**
     * 获取需要测试的方法列表
     * @return 需要测试的方法列表
     */
    public List<JavaMethod> getTestableMethods() {
        return methods.stream()
                .filter(method -> method.isPublic() && !method.isConstructor())
                .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
    }

    /**
     * 计算类的复杂度
     * @return 复杂度分数 (1-10)
     */
    public int getComplexityScore() {
        if (methods.isEmpty()) {
            return 1;
        }

        // 基于方法数量和方法复杂度计算
        int methodCount = methods.size();
        double avgMethodComplexity = methods.stream()
                .mapToInt(JavaMethod::getComplexityScore)
                .average()
                .orElse(1.0);

        int complexity = 1;

        // 方法数量影响
        if (methodCount > 20) {
            complexity += 3;
        } else if (methodCount > 10) {
            complexity += 2;
        } else if (methodCount > 5) {
            complexity += 1;
        }

        // 平均方法复杂度影响
        complexity += Math.min(4, (int) avgMethodComplexity);

        // 字段数量影响
        if (fields.size() > 10) {
            complexity += 1;
        }

        // 特殊类型影响
        if (isInterface()) {
            complexity -= 1; // 接口通常较简单
        }
        if (isAbstract()) {
            complexity += 1; // 抽象类通常较复杂
        }

        return Math.min(10, Math.max(1, complexity));
    }

    /**
     * 检查是否为实体类
     * @return 是否为实体类
     */
    public boolean isEntity() {
        return simpleName.endsWith("Entity") ||
               fields.stream().anyMatch(field -> field.getName().equals("id"));
    }

    /**
     * 检查是否为服务类
     * @return 是否为服务类
     */
    public boolean isService() {
        return simpleName.endsWith("Service") ||
               simpleName.endsWith("Manager") ||
               packageName.contains("service");
    }

    /**
     * 检查是否为控制器类
     * @return 是否为控制器类
     */
    public boolean isController() {
        return simpleName.endsWith("Controller") ||
               packageName.contains("controller") ||
               packageName.contains("web");
    }

    /**
     * 检查是否为工具类
     * @return 是否为工具类
     */
    public boolean isUtility() {
        return simpleName.endsWith("Utils") ||
               simpleName.endsWith("Util") ||
               simpleName.endsWith("Helper") ||
               hasStaticMethods();
    }

    // 私有验证方法
    private String validateSimpleName(String simpleName) {
        if (simpleName == null || simpleName.trim().isEmpty()) {
            throw new ValidationException("Class simple name cannot be null or empty");
        }
        if (!simpleName.matches("^[A-Z][a-zA-Z0-9_]*$")) {
            throw new ValidationException("Class simple name must be a valid Java identifier starting with uppercase");
        }
        return simpleName.trim();
    }

    private String validatePackageName(String packageName) {
        if (packageName == null) {
            return "";
        }
        String trimmed = packageName.trim();
        if (!trimmed.isEmpty() && !trimmed.matches("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$")) {
            throw new ValidationException("Package name must be a valid Java package identifier");
        }
        return trimmed;
    }

    // Getters
    public String getSimpleName() {
        return simpleName;
    }

    public String getPackageName() {
        return packageName;
    }

    public List<JavaMethod> getMethods() {
        return new ArrayList<>(methods);
    }

    public List<JavaField> getFields() {
        return new ArrayList<>(fields);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JavaClass javaClass = (JavaClass) o;
        return Objects.equals(simpleName, javaClass.simpleName) &&
               Objects.equals(packageName, javaClass.packageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(simpleName, packageName);
    }

    @Override
    public String toString() {
        return "JavaClass{" +
                "fullName='" + getFullName() + '\'' +
                ", methods=" + methods.size() +
                ", fields=" + fields.size() +
                ", complexity=" + getComplexityScore() +
                '}';
    }

    /**
     * JavaMethod内部类
     * 表示Java方法信息
     */
    public static class JavaMethod {
        private final String name;
        private final String returnType;
        private final List<String> parameterTypes;
        private final boolean isPublic;
        private final boolean isStatic;
        private final boolean isAbstract;

        public JavaMethod(String name, String returnType, List<String> parameterTypes,
                         boolean isPublic, boolean isStatic, boolean isAbstract) {
            this.name = name;
            this.returnType = returnType;
            this.parameterTypes = parameterTypes != null ? new ArrayList<>(parameterTypes) : new ArrayList<>();
            this.isPublic = isPublic;
            this.isStatic = isStatic;
            this.isAbstract = isAbstract;
        }

        public boolean isConstructor() {
            return "<init>".equals(name) || name.equals(returnType);
        }

        public boolean isGetter() {
            return name.startsWith("get") && parameterTypes.isEmpty();
        }

        public boolean isSetter() {
            return name.startsWith("set") && parameterTypes.size() == 1;
        }

        public boolean isComplex() {
            return parameterTypes.size() > 3 ||
                   returnType.contains("List") ||
                   returnType.contains("Map") ||
                   name.contains("process") ||
                   name.contains("calculate") ||
                   name.contains("validate");
        }

        public boolean isAsync() {
            return returnType.contains("Future") ||
                   returnType.contains("CompletableFuture") ||
                   returnType.contains("Mono") ||
                   returnType.contains("Flux") ||
                   name.contains("async");
        }

        public int getComplexityScore() {
            int score = 1;
            score += parameterTypes.size();
            if (isComplex()) score += 2;
            if (isAsync()) score += 1;
            return Math.min(5, score);
        }

        // Getters
        public String getName() { return name; }
        public String getReturnType() { return returnType; }
        public List<String> getParameterTypes() { return new ArrayList<>(parameterTypes); }
        public boolean isPublic() { return isPublic; }
        public boolean isStatic() { return isStatic; }
        public boolean isAbstract() { return isAbstract; }
    }

    /**
     * JavaField内部类
     * 表示Java字段信息
     */
    public static class JavaField {
        private final String name;
        private final String type;
        private final boolean isPublic;
        private final boolean isStatic;
        private final boolean isFinal;

        public JavaField(String name, String type, boolean isPublic, boolean isStatic, boolean isFinal) {
            this.name = name;
            this.type = type;
            this.isPublic = isPublic;
            this.isStatic = isStatic;
            this.isFinal = isFinal;
        }

        // Getters
        public String getName() { return name; }
        public String getType() { return type; }
        public boolean isPublic() { return isPublic; }
        public boolean isStatic() { return isStatic; }
        public boolean isFinal() { return isFinal; }
    }
}