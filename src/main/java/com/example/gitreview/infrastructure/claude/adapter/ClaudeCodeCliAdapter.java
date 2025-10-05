package com.example.gitreview.infrastructure.claude.adapter;

import com.example.gitreview.infrastructure.claude.ClaudeCodePort;
import com.example.gitreview.infrastructure.claude.ClaudeCodeResult;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Claude Code CLI 适配器
 * 调用 Claude Code CLI 在仓库目录中生成代码
 *
 * @author zhourui(V33215020)
 * @since 2025/10/05
 */
@Component
public class ClaudeCodeCliAdapter implements ClaudeCodePort {

    private static final Logger logger = LoggerFactory.getLogger(ClaudeCodeCliAdapter.class);

    @Value("${claude.command:claude}")
    private String claudeCommand;

    @Value("${claude.code.timeout:600000}")
    private long timeoutMs;

    private boolean available = false;

    @PostConstruct
    public void init() {
        try {
            logger.info("检查 Claude Code CLI 可用性...");

            String command = claudeCommand;
            if (System.getProperty("os.name").toLowerCase().contains("windows")
                && !command.endsWith(".cmd") && !command.endsWith(".exe")) {
                command = command + ".cmd";
            }

            ProcessBuilder pb = new ProcessBuilder(command, "--version");
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);

            if (finished && process.exitValue() == 0) {
                available = true;
                claudeCommand = command;
                logger.info("Claude Code CLI 可用: {}", command);
            } else {
                available = false;
                logger.warn("Claude Code CLI 不可用或无响应");
            }
        } catch (Exception e) {
            logger.error("检查 Claude Code CLI 可用性失败", e);
            available = false;
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public ClaudeCodeResult generateCode(File repoDir, String taskDescription,
                                          String technicalDesign, String contextCode) {
        if (!isAvailable()) {
            return ClaudeCodeResult.failure("Claude Code CLI 不可用", 0);
        }

        long startTime = System.currentTimeMillis();

        try {
            logger.info("在仓库目录执行代码生成: {}", repoDir.getAbsolutePath());

            List<String> filesBeforeGeneration = listFiles(repoDir);

            String prompt = buildPrompt(taskDescription, technicalDesign, contextCode);

            Path tempPromptFile = Files.createTempFile("claude-prompt-", ".txt");
            Files.writeString(tempPromptFile, prompt, StandardCharsets.UTF_8);

            List<String> command = new ArrayList<>();
            command.add(claudeCommand);
            command.add("--dangerously-skip-git-check");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(repoDir);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(prompt);
                writer.flush();
            }

            String output = readProcessOutput(process);

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            long executionTime = System.currentTimeMillis() - startTime;

            Files.deleteIfExists(tempPromptFile);

            if (!finished) {
                process.destroyForcibly();
                return ClaudeCodeResult.failure("代码生成超时: " + timeoutMs + "ms", executionTime);
            }

            int exitCode = process.exitValue();

            List<String> filesAfterGeneration = listFiles(repoDir);
            List<String> modifiedFiles = findModifiedFiles(filesBeforeGeneration, filesAfterGeneration);

            logger.info("代码生成完成，修改文件数: {}", modifiedFiles.size());

            if (exitCode == 0 || !modifiedFiles.isEmpty()) {
                return ClaudeCodeResult.success(
                    "代码生成成功，修改了 " + modifiedFiles.size() + " 个文件",
                    modifiedFiles,
                    executionTime
                );
            } else {
                return ClaudeCodeResult.failure(
                    "Claude Code CLI 返回错误: " + output,
                    executionTime
                );
            }

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("代码生成执行失败", e);
            return ClaudeCodeResult.failure("执行异常: " + e.getMessage(), executionTime);
        }
    }

    @Override
    public ClaudeCodeResult fixCompilationError(File repoDir, String errorType,
                                                String errorOutput, String taskDescription) {
        if (!isAvailable()) {
            return ClaudeCodeResult.failure("Claude Code CLI 不可用", 0);
        }

        long startTime = System.currentTimeMillis();

        try {
            logger.info("开始修复{}错误: {}", errorType, repoDir.getAbsolutePath());

            String prompt = buildFixPrompt(errorType, errorOutput, taskDescription);

            List<String> command = new ArrayList<>();
            command.add(claudeCommand);
            command.add("--dangerously-skip-git-check");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(repoDir);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(prompt);
                writer.flush();
            }

            String output = readProcessOutput(process);

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            long executionTime = System.currentTimeMillis() - startTime;

            if (!finished) {
                process.destroyForcibly();
                return ClaudeCodeResult.failure("修复超时: " + timeoutMs + "ms", executionTime);
            }

            int exitCode = process.exitValue();

            logger.info("错误修复完成，退出码: {}", exitCode);

            if (exitCode == 0) {
                return ClaudeCodeResult.success(
                    "错误修复成功",
                    List.of(),
                    executionTime
                );
            } else {
                return ClaudeCodeResult.failure(
                    "Claude Code CLI 修复失败: " + output,
                    executionTime
                );
            }

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("错误修复执行失败", e);
            return ClaudeCodeResult.failure("修复异常: " + e.getMessage(), executionTime);
        }
    }

    @Override
    public String getVersion() {
        if (!isAvailable()) {
            return "Claude Code CLI 不可用";
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(claudeCommand, "--version");
            Process process = pb.start();

            String output = readProcessOutput(process);
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);

            if (finished && process.exitValue() == 0) {
                return "Claude Code CLI: " + output.trim();
            } else {
                return "Claude Code CLI 版本未知";
            }
        } catch (Exception e) {
            logger.error("获取 Claude Code CLI 版本失败", e);
            return "Claude Code CLI 版本错误: " + e.getMessage();
        }
    }

    private String buildPrompt(String taskDescription, String technicalDesign, String contextCode) {
        return String.format(
            "你是一个资深开发工程师。根据以下任务描述和技术方案，生成完整的代码实现。\n\n" +
            "要求：\n" +
            "1. 严格遵循技术方案中的架构设计\n" +
            "2. 遵循 DDD 六边形架构分层（Domain/Application/Infrastructure）\n" +
            "3. 包含完整的 Javadoc 注释（@author zhourui(V33215020) @since 2025/10/05）\n" +
            "4. 符合 Java 最佳实践和 Alibaba-P3C 规范\n" +
            "5. 方法长度不超过50行，参数不超过5个\n" +
            "6. 使用中文注释\n" +
            "7. 确保代码可编译通过\n\n" +
            "任务描述：\n%s\n\n" +
            "技术方案（摘要）：\n%s\n\n" +
            "已完成代码上下文：\n%s\n\n" +
            "请直接在当前仓库中创建或修改文件，生成完整的实现代码。",
            taskDescription,
            technicalDesign.substring(0, Math.min(1000, technicalDesign.length())),
            contextCode
        );
    }

    private String buildFixPrompt(String errorType, String errorOutput, String taskDescription) {
        String errorTypeDesc = errorType.equals("COMPILATION") ? "编译错误" : "测试失败";

        return String.format(
            "你是一个资深开发工程师。当前代码存在%s，需要你修复。\n\n" +
            "任务上下文：\n%s\n\n" +
            "错误信息：\n```\n%s\n```\n\n" +
            "要求：\n" +
            "1. 仔细分析错误信息，定位问题根源\n" +
            "2. 修复代码使其能够编译通过和测试通过\n" +
            "3. 保持代码风格一致，遵循 DDD 六边形架构\n" +
            "4. 不要修改无关代码\n" +
            "5. 确保修复后的代码符合 Alibaba-P3C 规范\n" +
            "6. 如果是依赖问题，检查 import 语句和类路径\n" +
            "7. 如果是测试问题，检查测试数据和断言逻辑\n\n" +
            "请直接在当前仓库中修改文件，修复上述错误。",
            errorTypeDesc,
            taskDescription,
            errorOutput.substring(0, Math.min(2000, errorOutput.length()))
        );
    }

    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        return output.toString();
    }

    private List<String> listFiles(File dir) {
        List<String> files = new ArrayList<>();
        try {
            Files.walk(dir.toPath())
                .filter(Files::isRegularFile)
                .filter(p -> !p.toString().contains(".git"))
                .filter(p -> !p.toString().contains("target"))
                .forEach(p -> files.add(dir.toPath().relativize(p).toString()));
        } catch (IOException e) {
            logger.warn("列出文件失败: {}", dir, e);
        }
        return files;
    }

    private List<String> findModifiedFiles(List<String> before, List<String> after) {
        return after.stream()
            .filter(file -> !before.contains(file))
            .collect(Collectors.toList());
    }
}
