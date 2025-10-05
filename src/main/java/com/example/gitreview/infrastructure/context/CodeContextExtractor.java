package com.example.gitreview.infrastructure.context;

import com.example.gitreview.infrastructure.parser.JavaParserService;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.JavadocComment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 代码上下文提取器
 * 提取变更文件的类定义、方法、依赖关系等上下文信息
 */
@Component
public class CodeContextExtractor {

    private static final Logger logger = LoggerFactory.getLogger(CodeContextExtractor.class);
    private static final int MAX_CONTEXT_LINES = 2000;
    private static final int MAX_CLASS_LINES = 50;

    private final JavaParserService javaParser;

    public CodeContextExtractor(JavaParserService javaParser) {
        this.javaParser = javaParser;
    }

    /**
     * 提取文件上下文
     * @param filePath 文件路径
     * @param fileContent 文件内容
     * @param changedLines 变更的行号列表
     * @return FileContext
     */
    public FileContext extractContext(String filePath, String fileContent, List<Integer> changedLines) {
        logger.debug("提取文件上下文: {}, 变更行数: {}", filePath, changedLines.size());

        // 解析 AST
        CompilationUnit cu = javaParser.parseJavaFile(fileContent);
        if (cu == null) {
            logger.warn("无法解析文件: {}, 返回空上下文", filePath);
            return createEmptyContext(filePath);
        }

        try {
            // 1. 提取类级上下文
            ClassContext classContext = extractClassContext(cu);

            // 2. 提取变更方法的完整代码
            List<MethodContext> methodContexts = extractMethodContexts(cu, changedLines);

            // 3. 提取依赖方法（简化版：提取类中的其他方法签名）
            List<DependencyContext> dependencies = extractDependencies(cu, methodContexts);

            FileContext context = new FileContext(filePath, classContext, methodContexts, dependencies);

            // 4. 检查大小限制
            if (context.exceedsLimit(MAX_CONTEXT_LINES)) {
                logger.warn("上下文超过限制 {} 行，进行截断", MAX_CONTEXT_LINES);
                context = context.truncate(MAX_CONTEXT_LINES);
            }

            logger.debug("上下文提取完成: {}", context);
            return context;

        } catch (Exception e) {
            logger.error("提取上下文失败: " + filePath, e);
            return createEmptyContext(filePath);
        }
    }

    /**
     * 提取类级上下文
     */
    private ClassContext extractClassContext(CompilationUnit cu) {
        // 获取第一个类声明（简化处理）
        return cu.findFirst(ClassOrInterfaceDeclaration.class)
                .map(classDecl -> {
                    String className = classDecl.getNameAsString();
                    String packageName = cu.getPackageDeclaration()
                            .map(pd -> pd.getNameAsString())
                            .orElse(null);

                    // 提取类注释
                    String classComment = classDecl.getComment()
                            .filter(c -> c instanceof JavadocComment)
                            .map(c -> c.getContent())
                            .orElse(null);

                    // 提取字段声明（最多10个）
                    List<String> fields = classDecl.getFields().stream()
                            .limit(10)
                            .map(this::fieldToString)
                            .collect(Collectors.toList());

                    return new ClassContext(className, classComment, fields, packageName);
                })
                .orElse(null);
    }

    /**
     * 提取变更方法的完整代码
     */
    private List<MethodContext> extractMethodContexts(CompilationUnit cu, List<Integer> changedLines) {
        if (changedLines == null || changedLines.isEmpty()) {
            return Collections.emptyList();
        }

        List<MethodContext> contexts = new ArrayList<>();

        cu.findAll(MethodDeclaration.class).forEach(method -> {
            if (method.getRange().isPresent()) {
                int startLine = method.getRange().get().begin.line;
                int endLine = method.getRange().get().end.line;

                // 检查方法是否包含变更行
                boolean isChanged = changedLines.stream()
                        .anyMatch(line -> line >= startLine && line <= endLine);

                if (isChanged) {
                    String methodName = method.getNameAsString();
                    String methodComment = method.getComment()
                            .map(c -> c.getContent())
                            .orElse(null);
                    String methodCode = method.toString();

                    contexts.add(new MethodContext(
                            methodName, methodComment, methodCode,
                            startLine, endLine, true
                    ));
                }
            }
        });

        logger.debug("找到 {} 个变更方法", contexts.size());
        return contexts;
    }

    /**
     * 提取依赖方法（简化版：提取类中未变更的方法签名）
     */
    private List<DependencyContext> extractDependencies(CompilationUnit cu, List<MethodContext> changedMethods) {
        List<String> changedMethodNames = changedMethods.stream()
                .map(MethodContext::getMethodName)
                .collect(Collectors.toList());

        List<DependencyContext> dependencies = new ArrayList<>();

        cu.findAll(MethodDeclaration.class).stream()
                .filter(method -> !changedMethodNames.contains(method.getNameAsString()))
                .limit(10) // 最多10个依赖方法
                .forEach(method -> {
                    String signature = method.getSignature().asString();
                    String comment = method.getComment()
                            .map(c -> c.getContent())
                            .orElse(null);
                    String className = cu.findFirst(ClassOrInterfaceDeclaration.class)
                            .map(ClassOrInterfaceDeclaration::getNameAsString)
                            .orElse("Unknown");

                    dependencies.add(new DependencyContext(signature, comment, className));
                });

        logger.debug("找到 {} 个依赖方法", dependencies.size());
        return dependencies;
    }

    /**
     * 字段转字符串
     */
    private String fieldToString(FieldDeclaration field) {
        return field.toString().trim();
    }

    /**
     * 创建空上下文
     */
    private FileContext createEmptyContext(String filePath) {
        return new FileContext(
                filePath,
                null,
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    /**
     * 提取仓库结构
     * 遍历项目目录，提取主要包结构和类列表
     *
     * @param repoPath 仓库路径
     * @return Markdown格式的树形结构文本
     */
    public String extractRepositoryStructure(String repoPath) {
        logger.info("开始提取仓库结构: {}", repoPath);

        try {
            Path rootPath = Paths.get(repoPath);
            if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
                logger.warn("仓库路径不存在或不是目录: {}", repoPath);
                return "## Repository Structure\n\n仓库路径无效";
            }

            // 查找 src/main/java 目录
            Path javaSourcePath = findJavaSourcePath(rootPath);
            if (javaSourcePath == null) {
                logger.warn("未找到 Java 源码目录");
                return "## Repository Structure\n\n未找到 Java 源码目录";
            }

            // 构建包结构
            Map<String, List<String>> packageStructure = buildPackageStructure(javaSourcePath);

            // 生成 Markdown 输出
            StringBuilder sb = new StringBuilder();
            sb.append("## Repository Structure\n\n");
            sb.append("```\n");

            // 提取基础包名
            String basePackage = extractBasePackage(packageStructure);
            if (basePackage != null) {
                sb.append(basePackage).append("\n");
                generateStructureTree(sb, packageStructure, basePackage, "  ");
            } else {
                // 如果无法确定基础包，直接输出所有包
                packageStructure.forEach((pkg, classes) -> {
                    sb.append("- ").append(pkg).append("\n");
                    classes.forEach(cls -> sb.append("    - ").append(cls).append("\n"));
                });
            }

            sb.append("```\n");

            String result = sb.toString();
            logger.info("仓库结构提取完成，包数量: {}", packageStructure.size());
            return result;

        } catch (Exception e) {
            logger.error("提取仓库结构失败", e);
            return "## Repository Structure\n\n提取失败: " + e.getMessage();
        }
    }

    /**
     * 查找 Java 源码路径
     */
    private Path findJavaSourcePath(Path rootPath) throws IOException {
        // 尝试常见的 Java 源码路径
        String[] commonPaths = {
                "src/main/java",
                "src/java",
                "java",
                "src"
        };

        for (String commonPath : commonPaths) {
            Path candidate = rootPath.resolve(commonPath);
            if (Files.exists(candidate) && Files.isDirectory(candidate)) {
                // 检查是否包含 Java 文件
                try (Stream<Path> stream = Files.walk(candidate, 10)) {
                    boolean hasJavaFiles = stream.anyMatch(p -> p.toString().endsWith(".java"));
                    if (hasJavaFiles) {
                        logger.debug("找到 Java 源码目录: {}", candidate);
                        return candidate;
                    }
                }
            }
        }

        return null;
    }

    /**
     * 构建包结构
     */
    private Map<String, List<String>> buildPackageStructure(Path javaSourcePath) throws IOException {
        Map<String, List<String>> structure = new TreeMap<>();

        try (Stream<Path> paths = Files.walk(javaSourcePath, 10)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .filter(Files::isRegularFile)
                    .forEach(javaFile -> {
                        try {
                            String relativePath = javaSourcePath.relativize(javaFile).toString();
                            String packagePath = relativePath.replace("\\", "/");

                            // 提取包名和类名
                            int lastSlash = packagePath.lastIndexOf('/');
                            String packageName = lastSlash > 0 ?
                                    packagePath.substring(0, lastSlash).replace('/', '.') : "";
                            String className = packagePath.substring(lastSlash + 1, packagePath.length() - 5); // 去掉 .java

                            structure.computeIfAbsent(packageName, k -> new ArrayList<>()).add(className);

                        } catch (Exception e) {
                            logger.warn("处理文件失败: {}", javaFile, e);
                        }
                    });
        }

        return structure;
    }

    /**
     * 提取基础包名
     */
    private String extractBasePackage(Map<String, List<String>> packageStructure) {
        if (packageStructure.isEmpty()) {
            return null;
        }

        // 找到最短的包名作为基础包
        String basePackage = packageStructure.keySet().stream()
                .min((p1, p2) -> Integer.compare(p1.split("\\.").length, p2.split("\\.").length))
                .orElse(null);

        // 如果基础包的层级太深，取前3级
        if (basePackage != null) {
            String[] parts = basePackage.split("\\.");
            if (parts.length > 3) {
                basePackage = String.join(".", parts[0], parts[1], parts[2]);
            }
        }

        return basePackage;
    }

    /**
     * 生成结构树
     */
    private void generateStructureTree(StringBuilder sb, Map<String, List<String>> packageStructure,
                                       String basePackage, String indent) {
        // 按包名分组
        Map<String, Map<String, List<String>>> grouped = new TreeMap<>();

        packageStructure.forEach((pkg, classes) -> {
            if (pkg.startsWith(basePackage)) {
                String subPackage = pkg.substring(basePackage.length());
                if (subPackage.startsWith(".")) {
                    subPackage = subPackage.substring(1);
                }

                if (subPackage.isEmpty()) {
                    // 基础包直接的类
                    grouped.computeIfAbsent("", k -> new TreeMap<>()).put(pkg, classes);
                } else {
                    // 子包
                    String firstLevel = subPackage.contains(".") ?
                            subPackage.substring(0, subPackage.indexOf('.')) : subPackage;
                    grouped.computeIfAbsent(firstLevel, k -> new TreeMap<>()).put(pkg, classes);
                }
            }
        });

        // 输出结构
        grouped.forEach((subPkg, pkgs) -> {
            if (!subPkg.isEmpty()) {
                sb.append(indent).append("└── ").append(subPkg).append("\n");
                pkgs.forEach((fullPkg, classes) -> {
                    if (!classes.isEmpty()) {
                        String classesStr = classes.stream()
                                .limit(20) // 最多显示20个类
                                .collect(Collectors.joining(", "));
                        if (classes.size() > 20) {
                            classesStr += " (+" + (classes.size() - 20) + " more)";
                        }
                        sb.append(indent).append("    └── ").append(classesStr).append("\n");
                    }
                });
            }
        });
    }
}
