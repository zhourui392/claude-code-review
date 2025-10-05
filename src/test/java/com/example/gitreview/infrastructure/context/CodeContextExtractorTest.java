package com.example.gitreview.infrastructure.context;

import com.example.gitreview.infrastructure.parser.JavaParserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * CodeContextExtractor 测试
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
class CodeContextExtractorTest {

    private CodeContextExtractor extractor;

    @BeforeEach
    void setUp() {
        JavaParserService javaParserService = new JavaParserService();
        extractor = new CodeContextExtractor(javaParserService);
    }

    @Test
    void should_extract_repository_structure_successfully(@TempDir Path tempDir) throws IOException {
        // 创建测试项目结构
        Path srcMainJava = tempDir.resolve("src/main/java");
        Files.createDirectories(srcMainJava);

        // 创建包结构
        Path comExampleDomain = srcMainJava.resolve("com/example/domain");
        Path comExampleService = srcMainJava.resolve("com/example/service");
        Path comExampleController = srcMainJava.resolve("com/example/controller");

        Files.createDirectories(comExampleDomain);
        Files.createDirectories(comExampleService);
        Files.createDirectories(comExampleController);

        // 创建Java文件
        Files.writeString(comExampleDomain.resolve("User.java"), "public class User {}");
        Files.writeString(comExampleDomain.resolve("Order.java"), "public class Order {}");
        Files.writeString(comExampleService.resolve("UserService.java"), "public class UserService {}");
        Files.writeString(comExampleController.resolve("UserController.java"), "public class UserController {}");

        // 执行提取
        String structure = extractor.extractRepositoryStructure(tempDir.toString());

        // 验证结果
        assertThat(structure).isNotNull();
        assertThat(structure).contains("## Repository Structure");
        assertThat(structure).contains("com.example");
        assertThat(structure).containsAnyOf("domain", "service", "controller");
    }

    @Test
    void should_return_error_message_when_path_not_exists() {
        String structure = extractor.extractRepositoryStructure("/nonexistent/path");

        assertThat(structure).contains("仓库路径无效");
    }

    @Test
    void should_return_error_when_no_java_source_found(@TempDir Path tempDir) {
        // 创建空目录
        String structure = extractor.extractRepositoryStructure(tempDir.toString());

        assertThat(structure).contains("未找到 Java 源码目录");
    }

    @Test
    void should_extract_current_project_structure() {
        // 测试当前项目
        String currentDir = System.getProperty("user.dir");
        String structure = extractor.extractRepositoryStructure(currentDir);

        assertThat(structure).isNotNull();
        assertThat(structure).contains("## Repository Structure");
        // 当前项目应该包含这些包
        assertThat(structure).containsAnyOf("gitreview", "domain", "application", "infrastructure");
    }
}
