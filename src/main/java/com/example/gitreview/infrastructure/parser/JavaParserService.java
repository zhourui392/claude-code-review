package com.example.gitreview.infrastructure.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * JavaParser 服务
 * 负责解析 Java 代码生成 AST
 */
@Service
public class JavaParserService {

    private static final Logger logger = LoggerFactory.getLogger(JavaParserService.class);

    private final JavaParser javaParser;

    public JavaParserService() {
        this.javaParser = new JavaParser();
    }

    /**
     * 解析 Java 文件内容
     * @param fileContent Java 文件内容
     * @return CompilationUnit (AST根节点)，解析失败返回 null
     */
    public CompilationUnit parseJavaFile(String fileContent) {
        if (fileContent == null || fileContent.trim().isEmpty()) {
            logger.warn("文件内容为空，无法解析");
            return null;
        }

        try {
            ParseResult<CompilationUnit> parseResult = javaParser.parse(fileContent);

            if (!parseResult.isSuccessful()) {
                logger.warn("Java 文件解析失败: {}", parseResult.getProblems());
                return null;
            }

            Optional<CompilationUnit> cuOpt = parseResult.getResult();
            if (cuOpt.isPresent()) {
                return cuOpt.get();
            } else {
                logger.warn("解析结果为空");
                return null;
            }
        } catch (Exception e) {
            logger.error("Java 文件解析异常", e);
            return null;
        }
    }

    /**
     * 安全解析（吞掉所有异常）
     */
    public Optional<CompilationUnit> safeParse(String fileContent) {
        try {
            CompilationUnit cu = parseJavaFile(fileContent);
            return Optional.ofNullable(cu);
        } catch (Exception e) {
            logger.error("解析异常", e);
            return Optional.empty();
        }
    }
}
