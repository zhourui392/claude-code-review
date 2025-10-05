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
                return CompilationResult.failure("编译失败，退出码: " + exitCode, output, executionTime);
            }

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("编译执行异常", e);
            return CompilationResult.failure("编译异常: " + e.getMessage(), "", executionTime);
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

            if (exitCode == 0) {
                logger.info("测试通过，耗时: {}ms", executionTime);
                return CompilationResult.success("测试通过", output, executionTime);
            } else {
                logger.error("测试失败，退出码: {}", exitCode);
                return CompilationResult.failure("测试失败，退出码: " + exitCode, output, executionTime);
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
        command.add("-q");

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
}
