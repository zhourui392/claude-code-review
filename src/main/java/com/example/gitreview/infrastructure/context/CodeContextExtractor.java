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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
}
