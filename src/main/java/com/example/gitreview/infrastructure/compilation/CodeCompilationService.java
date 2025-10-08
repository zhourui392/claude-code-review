package com.example.gitreview.infrastructure.compilation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 代码编译验证服务
 * 在仓库目录中执行 Maven 编译和测试
 *
 * @author zhourui(V33215020)
 * @since 2025/10/05
 */
@Service
public class CodeCompilationService {

    private static final Logger logger = LoggerFactory.getLogger(CodeCompilationService.class);

    @Value("${compilation.timeout:300000}")
    private long compilationTimeoutMs;

    @Value("${test.timeout:600000}")
    private long testTimeoutMs;

    /**
     * 编译代码
     *
     * @param repoDir 仓库目录
     * @return 编译结果
     */
    public CompilationResult compile(File repoDir) {
        logger.info("开始编译代码: {}", repoDir.getAbsolutePath());

        long startTime = System.currentTimeMillis();

        try {
            List<String> command = buildCompileCommand();

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(repoDir);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            String output = readProcessOutput(process);

            boolean finished = process.waitFor(compilationTimeoutMs, TimeUnit.MILLISECONDS);
            long executionTime = System.currentTimeMillis() - startTime;

            if (!finished) {
                process.destroyForcibly();
                return CompilationResult.failure("编译超时: " + compilationTimeoutMs + "ms", output, executionTime);
            }

            int exitCode = process.exitValue();

            if (exitCode == 0) {
                logger.info("编译成功，耗时: {}ms", executionTime);
                return CompilationResult.success("编译成功", output, executionTime);
            } else {
                logger.error("编译失败，退出码: {}", exitCode);
                logger.error("编译错误输出:\n{}", output);
                return CompilationResult.failure("编译失败，退出码: " + exitCode, output, executionTime);
            }

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("编译执行异常", e);
            return CompilationResult.failure("编译异常: " + e.getMessage(), "", executionTime);
        }
    }

    /**
     * 编译测试代码
     *
     * @param repoDir 仓库目录
     * @return 编译结果
     */
    public CompilationResult compileTests(File repoDir) {
        logger.info("开始编译测试代码: {}", repoDir.getAbsolutePath());

        long startTime = System.currentTimeMillis();

        try {
            List<String> command = buildTestCompileCommand();

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(repoDir);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            String output = readProcessOutput(process);

            boolean finished = process.waitFor(compilationTimeoutMs, TimeUnit.MILLISECONDS);
            long executionTime = System.currentTimeMillis() - startTime;

            if (!finished) {
                process.destroyForcibly();
                return CompilationResult.failure("测试编译超时: " + compilationTimeoutMs + "ms", output, executionTime);
            }

            int exitCode = process.exitValue();

            if (exitCode == 0) {
                logger.info("测试编译成功，耗时: {}ms", executionTime);
                return CompilationResult.success("测试编译成功", output, executionTime);
            } else {
                logger.error("测试编译失败，退出码: {}", exitCode);
                logger.error("编译错误输出:\n{}", output);
                return CompilationResult.failure("测试编译失败，退出码: " + exitCode, output, executionTime);
            }

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("测试编译执行异常", e);
            return CompilationResult.failure("测试编译异常: " + e.getMessage(), "", executionTime);
        }
    }

    /**
     * 运行单元测试
     *
     * @param repoDir 仓库目录
     * @return 测试结果
     */
    public CompilationResult runTests(File repoDir) {
        logger.info("开始运行单元测试: {}", repoDir.getAbsolutePath());

        long startTime = System.currentTimeMillis();

        try {
            List<String> command = buildTestCommand();

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(repoDir);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            String output = readProcessOutput(process);

            boolean finished = process.waitFor(testTimeoutMs, TimeUnit.MILLISECONDS);
            long executionTime = System.currentTimeMillis() - startTime;

            if (!finished) {
                process.destroyForcibly();
                return CompilationResult.failure("测试超时: " + testTimeoutMs + "ms", output, executionTime);
            }

            int exitCode = process.exitValue();

            // 解析测试统计信息
            TestStatistics stats = parseTestStatistics(output);
            
            logger.info("测试输出（部分）:\n{}", output.substring(0, Math.min(500, output.length())));

            if (exitCode == 0) {
                logger.info("测试通过，耗时: {}ms, 统计: {}", executionTime, stats);
                return CompilationResult.success("测试通过: " + stats, output, executionTime);
            } else {
                logger.error("测试失败，退出码: {}, 统计: {}", exitCode, stats);
                return CompilationResult.failure("测试失败: " + stats, output, executionTime);
            }

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("测试执行异常", e);
            return CompilationResult.failure("测试异常: " + e.getMessage(), "", executionTime);
        }
    }

    /**
     * 编译并测试
     *
     * @param repoDir 仓库目录
     * @return 编译测试结果
     */
    public CompilationResult compileAndTest(File repoDir) {
        CompilationResult compileResult = compile(repoDir);

        if (!compileResult.isSuccess()) {
            return compileResult;
        }

        return runTests(repoDir);
    }

    private List<String> buildCompileCommand() {
        List<String> command = new ArrayList<>();

        if (isWindows()) {
            command.add("cmd");
            command.add("/c");
            command.add("mvn");
        } else {
            command.add("mvn");
        }

        command.add("clean");
        command.add("compile");
        command.add("-DskipTests");
        command.add("-q");

        return command;
    }

    private List<String> buildTestCompileCommand() {
        List<String> command = new ArrayList<>();

        if (isWindows()) {
            command.add("cmd");
            command.add("/c");
            command.add("mvn");
        } else {
            command.add("mvn");
        }

        command.add("test-compile");
        command.add("-DfailIfNoTests=false");

        return command;
    }

    private List<String> buildTestCommand() {
        List<String> command = new ArrayList<>();

        if (isWindows()) {
            command.add("cmd");
            command.add("/c");
            command.add("mvn");
        } else {
            command.add("mvn");
        }

        command.add("test");
        command.add("-DfailIfNoTests=false");
        // 不在测试命令中加 jacoco:report，避免未配置 JaCoCo 时导致构建失败

        return command;
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                logger.debug(line);
            }
        }

        return output.toString();
    }

    /**
     * 测试统计信息
     */
    private static class TestStatistics {
        int total = 0;
        int failures = 0;
        int errors = 0;
        int skipped = 0;

        @Override
        public String toString() {
            if (total == 0) {
                return "未运行测试";
            }
            int passed = total - failures - errors - skipped;
            return String.format("共%d个测试，通过%d，失败%d，错误%d，跳过%d", 
                total, passed, failures, errors, skipped);
        }
    }

    /**
     * 从 Maven 输出中解析测试统计信息
     * Maven Surefire 输出格式示例：
     * Tests run: 5, Failures: 1, Errors: 0, Skipped: 0
     */
    private TestStatistics parseTestStatistics(String output) {
        TestStatistics stats = new TestStatistics();
        
        try {
            // 匹配 "Tests run: X, Failures: Y, Errors: Z, Skipped: W" 格式
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "Tests run: (\\d+),\\s*Failures: (\\d+),\\s*Errors: (\\d+),\\s*Skipped: (\\d+)"
            );
            java.util.regex.Matcher matcher = pattern.matcher(output);
            
            // 找到最后一次出现的统计（通常是总计）
            while (matcher.find()) {
                stats.total = Integer.parseInt(matcher.group(1));
                stats.failures = Integer.parseInt(matcher.group(2));
                stats.errors = Integer.parseInt(matcher.group(3));
                stats.skipped = Integer.parseInt(matcher.group(4));
            }
        } catch (Exception e) {
            logger.debug("解析测试统计信息失败: {}", e.getMessage());
        }
        
        return stats;
    }

}
