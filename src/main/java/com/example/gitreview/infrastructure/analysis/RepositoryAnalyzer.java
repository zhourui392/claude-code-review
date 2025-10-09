package com.example.gitreview.infrastructure.analysis;

import com.example.gitreview.infrastructure.parser.JavaParserService;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 代码仓库分析器
 * 分析代码仓库的结构、架构模式、代码风格等上下文信息
 *
 * @author zhourui(V33215020)
 * @since 2025/10/10
 */
@Component
public class RepositoryAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryAnalyzer.class);
    private static final int MAX_FILES_TO_ANALYZE = 50;
    private static final int MAX_SAMPLE_CLASSES = 10;

    @Autowired
    private JavaParserService javaParser;

    /**
     * 分析仓库上下文
     *
     * @param repoDir 仓库目录
     * @return 仓库上下文信息
     */
    public RepositoryContext analyzeRepository(File repoDir) {
        logger.info("开始分析仓库: {}", repoDir.getAbsolutePath());

        try {
            // 1. 分析目录结构
            DirectoryStructure structure = analyzeDirectoryStructure(repoDir);

            // 2. 分析架构模式
            ArchitecturePattern architecture = detectArchitecturePattern(repoDir, structure);

            // 3. 分析代码风格
            CodingStyle codingStyle = analyzeCodingStyle(repoDir, structure);

            // 4. 提取核心实体类示例
            List<String> sampleClasses = extractSampleClasses(repoDir, structure);

            RepositoryContext context = new RepositoryContext(
                    structure,
                    architecture,
                    codingStyle,
                    sampleClasses
            );

            logger.info("仓库分析完成: {}", context.getSummary());
            return context;

        } catch (Exception e) {
            logger.error("仓库分析失败: " + repoDir.getAbsolutePath(), e);
            return createDefaultContext();
        }
    }

    /**
     * 分析目录结构
     */
    private DirectoryStructure analyzeDirectoryStructure(File repoDir) {
        logger.debug("分析目录结构: {}", repoDir.getAbsolutePath());

        Map<String, Integer> packageMap = new HashMap<>();
        List<String> mainPackages = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(Paths.get(repoDir.getAbsolutePath()), 10)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> p.toString().contains("src/main/java"))
                    .forEach(path -> {
                        String packagePath = extractPackagePath(path);
                        if (packagePath != null) {
                            packageMap.put(packagePath, packageMap.getOrDefault(packagePath, 0) + 1);
                        }
                    });

            // 提取主要包名（一级和二级包）
            mainPackages = packageMap.keySet().stream()
                    .map(this::extractRootPackage)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

        } catch (IOException e) {
            logger.error("分析目录结构失败", e);
        }

        return new DirectoryStructure(packageMap, mainPackages);
    }

    /**
     * 检测架构模式
     */
    private ArchitecturePattern detectArchitecturePattern(File repoDir, DirectoryStructure structure) {
        logger.debug("检测架构模式");

        List<String> packages = structure.getMainPackages();
        String detectedPattern = "未知架构";
        int confidence = 0;

        // DDD六边形架构检测
        if (packages.contains("domain") && packages.contains("application") && packages.contains("infrastructure")) {
            detectedPattern = "DDD 六边形架构（Domain/Application/Infrastructure）";
            confidence = 90;
        }
        // MVC三层架构检测
        else if (packages.contains("controller") && packages.contains("service") && packages.contains("dao")) {
            detectedPattern = "MVC 三层架构（Controller/Service/DAO）";
            confidence = 85;
        }
        // 分层架构检测
        else if (packages.contains("controller") && packages.contains("service")) {
            detectedPattern = "分层架构（基于Spring Boot）";
            confidence = 70;
        }
        // 默认Spring Boot
        else {
            detectedPattern = "Spring Boot 标准架构";
            confidence = 50;
        }

        return new ArchitecturePattern(detectedPattern, confidence, packages);
    }

    /**
     * 分析代码风格
     */
    private CodingStyle analyzeCodingStyle(File repoDir, DirectoryStructure structure) {
        logger.debug("分析代码风格");

        List<File> javaFiles = findJavaFiles(repoDir, 20);
        if (javaFiles.isEmpty()) {
            return CodingStyle.createDefault();
        }

        int totalClasses = 0;
        final int[] chineseComments = {0};
        final int[] englishComments = {0};
        List<String> namingExamples = new ArrayList<>();

        for (File file : javaFiles) {
            try {
                String content = Files.readString(file.toPath());
                CompilationUnit cu = javaParser.parseJavaFile(content);
                if (cu == null) continue;

                totalClasses++;

                // 分析注释语言
                cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
                    classDecl.getComment().ifPresent(comment -> {
                        String commentText = comment.getContent();
                        if (containsChinese(commentText)) {
                            chineseComments[0]++;
                        } else {
                            englishComments[0]++;
                        }
                    });

                    // 收集命名示例
                    namingExamples.add("类名: " + classDecl.getNameAsString());
                });

                // 分析方法命名
                cu.findAll(MethodDeclaration.class).stream().limit(3).forEach(method -> {
                    namingExamples.add("方法: " + method.getNameAsString());
                });

            } catch (Exception e) {
                logger.debug("分析文件失败: {}", file.getName());
            }
        }

        String commentLanguage = (chineseComments[0] > englishComments[0]) ? "中文" : "英文";
        String namingConvention = "驼峰命名法"; // Java默认

        return new CodingStyle(
                commentLanguage,
                namingConvention,
                "Alibaba-P3C 规范",
                namingExamples.stream().limit(10).collect(Collectors.toList())
        );
    }

    /**
     * 提取示例类代码
     */
    private List<String> extractSampleClasses(File repoDir, DirectoryStructure structure) {
        logger.debug("提取示例类");

        List<String> samples = new ArrayList<>();
        List<File> javaFiles = findJavaFiles(repoDir, MAX_SAMPLE_CLASSES);

        for (File file : javaFiles) {
            try {
                String content = Files.readString(file.toPath());
                CompilationUnit cu = javaParser.parseJavaFile(content);
                if (cu == null) continue;

                cu.findFirst(ClassOrInterfaceDeclaration.class).ifPresent(classDecl -> {
                    StringBuilder sample = new StringBuilder();
                    sample.append("// ").append(file.getName()).append("\n");
                    sample.append("package ").append(cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("")).append(";\n\n");

                    // 类声明和注释
                    classDecl.getComment().ifPresent(comment -> sample.append(comment).append("\n"));
                    sample.append("public class ").append(classDecl.getNameAsString()).append(" {\n");

                    // 字段
                    cu.findAll(FieldDeclaration.class).stream().limit(5).forEach(field -> {
                        sample.append("    ").append(field.toString()).append("\n");
                    });

                    // 方法签名
                    cu.findAll(MethodDeclaration.class).stream().limit(3).forEach(method -> {
                        method.getComment().ifPresent(comment -> sample.append("    ").append(comment).append("\n"));
                        sample.append("    ").append(method.getDeclarationAsString()).append(" { ... }\n");
                    });

                    sample.append("}\n");
                    samples.add(sample.toString());
                });

            } catch (Exception e) {
                logger.debug("提取示例类失败: {}", file.getName());
            }

            if (samples.size() >= MAX_SAMPLE_CLASSES) break;
        }

        return samples;
    }

    private List<File> findJavaFiles(File dir, int limit) {
        try (Stream<Path> paths = Files.walk(dir.toPath(), 10)) {
            return paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> p.toString().contains("src/main/java"))
                    .filter(p -> !p.toString().contains("/test/"))
                    .limit(limit)
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("查找Java文件失败", e);
            return Collections.emptyList();
        }
    }

    private String extractPackagePath(Path path) {
        String pathStr = path.toString().replace("\\", "/");
        int srcIndex = pathStr.indexOf("src/main/java/");
        if (srcIndex == -1) return null;

        String packagePath = pathStr.substring(srcIndex + "src/main/java/".length());
        int lastSlash = packagePath.lastIndexOf('/');
        if (lastSlash == -1) return null;

        return packagePath.substring(0, lastSlash).replace("/", ".");
    }

    private String extractRootPackage(String fullPackage) {
        String[] parts = fullPackage.split("\\.");
        if (parts.length >= 4) {
            return parts[3]; // com.example.project.[domain]
        }
        return fullPackage;
    }

    private boolean containsChinese(String text) {
        return text != null && text.matches(".*[\\u4e00-\\u9fa5]+.*");
    }

    private RepositoryContext createDefaultContext() {
        return new RepositoryContext(
                new DirectoryStructure(Collections.emptyMap(), Collections.emptyList()),
                new ArchitecturePattern("Spring Boot 标准架构", 50, Collections.emptyList()),
                CodingStyle.createDefault(),
                Collections.emptyList()
        );
    }

    /**
     * 目录结构
     */
    public static class DirectoryStructure {
        private final Map<String, Integer> packageMap;
        private final List<String> mainPackages;

        public DirectoryStructure(Map<String, Integer> packageMap, List<String> mainPackages) {
            this.packageMap = packageMap;
            this.mainPackages = mainPackages;
        }

        public Map<String, Integer> getPackageMap() {
            return packageMap;
        }

        public List<String> getMainPackages() {
            return mainPackages;
        }

        @Override
        public String toString() {
            return "主要包: " + String.join(", ", mainPackages) + " (共" + packageMap.size() + "个包)";
        }
    }

    /**
     * 架构模式
     */
    public static class ArchitecturePattern {
        private final String pattern;
        private final int confidence;
        private final List<String> packages;

        public ArchitecturePattern(String pattern, int confidence, List<String> packages) {
            this.pattern = pattern;
            this.confidence = confidence;
            this.packages = packages;
        }

        public String getPattern() {
            return pattern;
        }

        public int getConfidence() {
            return confidence;
        }

        public List<String> getPackages() {
            return packages;
        }

        @Override
        public String toString() {
            return pattern + " (置信度: " + confidence + "%)";
        }
    }

    /**
     * 代码风格
     */
    public static class CodingStyle {
        private final String commentLanguage;
        private final String namingConvention;
        private final String codingStandard;
        private final List<String> namingExamples;

        public CodingStyle(String commentLanguage, String namingConvention, String codingStandard, List<String> namingExamples) {
            this.commentLanguage = commentLanguage;
            this.namingConvention = namingConvention;
            this.codingStandard = codingStandard;
            this.namingExamples = namingExamples;
        }

        public String getCommentLanguage() {
            return commentLanguage;
        }

        public String getNamingConvention() {
            return namingConvention;
        }

        public String getCodingStandard() {
            return codingStandard;
        }

        public List<String> getNamingExamples() {
            return namingExamples;
        }

        public static CodingStyle createDefault() {
            return new CodingStyle("中文", "驼峰命名法", "Alibaba-P3C 规范", Collections.emptyList());
        }

        @Override
        public String toString() {
            return String.format("注释: %s, 命名: %s, 规范: %s", commentLanguage, namingConvention, codingStandard);
        }
    }

    /**
     * 仓库上下文
     */
    public static class RepositoryContext {
        private final DirectoryStructure structure;
        private final ArchitecturePattern architecture;
        private final CodingStyle codingStyle;
        private final List<String> sampleClasses;

        public RepositoryContext(DirectoryStructure structure, ArchitecturePattern architecture,
                                 CodingStyle codingStyle, List<String> sampleClasses) {
            this.structure = structure;
            this.architecture = architecture;
            this.codingStyle = codingStyle;
            this.sampleClasses = sampleClasses;
        }

        public DirectoryStructure getStructure() {
            return structure;
        }

        public ArchitecturePattern getArchitecture() {
            return architecture;
        }

        public CodingStyle getCodingStyle() {
            return codingStyle;
        }

        public List<String> getSampleClasses() {
            return sampleClasses;
        }

        public String getSummary() {
            return String.format("架构: %s, 代码风格: %s, 示例类: %d个",
                    architecture.getPattern(), codingStyle.toString(), sampleClasses.size());
        }

        public String toMarkdown() {
            StringBuilder md = new StringBuilder();
            md.append("## 代码仓库上下文\n\n");

            md.append("### 架构模式\n");
            md.append("- **检测到的架构**: ").append(architecture.getPattern()).append("\n");
            md.append("- **置信度**: ").append(architecture.getConfidence()).append("%\n");
            md.append("- **主要包结构**: ").append(String.join(", ", architecture.getPackages())).append("\n\n");

            md.append("### 代码风格\n");
            md.append("- **注释语言**: ").append(codingStyle.getCommentLanguage()).append("\n");
            md.append("- **命名规范**: ").append(codingStyle.getNamingConvention()).append("\n");
            md.append("- **编码规范**: ").append(codingStyle.getCodingStandard()).append("\n\n");

            if (!codingStyle.getNamingExamples().isEmpty()) {
                md.append("### 命名示例\n");
                codingStyle.getNamingExamples().stream().limit(5).forEach(example -> {
                    md.append("- ").append(example).append("\n");
                });
                md.append("\n");
            }

            if (!sampleClasses.isEmpty()) {
                md.append("### 示例代码（仓库中已有的类）\n\n");
                sampleClasses.stream().limit(3).forEach(sample -> {
                    md.append("```java\n").append(sample).append("\n```\n\n");
                });
            }

            return md.toString();
        }
    }
}
