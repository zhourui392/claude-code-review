package com.example.gitreview.infrastructure.claude.adapter;

import com.example.gitreview.infrastructure.claude.ClaudeQueryPort;
import com.example.gitreview.infrastructure.claude.ClaudeQueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Claude CLI适配器
 * 使用实际的 Claude CLI 工具进行代码审查和查询
 */
@Component
public class ClaudeCliAdapter implements ClaudeQueryPort {

    private static final Logger logger = LoggerFactory.getLogger(ClaudeCliAdapter.class);

    @Value("${claude.command:claude}")
    private String claudeCommand;

    @Value("${claude.timeout:300000}")
    private long timeoutMs;

    private final AtomicBoolean available = new AtomicBoolean(false);
    private final Properties reviewPrompts = new Properties();

    @PostConstruct
    public void init() {
        // 加载审查提示词配置
        loadReviewPrompts();

        try {
            logger.info("Checking Claude CLI availability...");

            // Windows环境下需要使用.cmd后缀
            String command = claudeCommand;
            if (System.getProperty("os.name").toLowerCase().contains("windows")
                && !command.endsWith(".cmd") && !command.endsWith(".exe")) {
                command = command + ".cmd";
            }

            // 测试 Claude CLI 是否可用
            ProcessBuilder pb = new ProcessBuilder(command, "--version");
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);

            if (finished && process.exitValue() == 0) {
                available.set(true);
                claudeCommand = command; // 更新为实际可用的命令
                logger.info("Claude CLI is available: {}", command);
            } else {
                available.set(false);
                logger.warn("Claude CLI is not available or not responding");
            }
        } catch (Exception e) {
            logger.error("Failed to check Claude CLI availability", e);
            available.set(false);
        }
    }

    @Override
    public boolean isAvailable() {
        return available.get();
    }

    @Override
    public ClaudeQueryResponse query(String prompt) {
        if (!isAvailable()) {
            return ClaudeQueryResponse.failure(-1, "Claude CLI is not available", 0, "N/A");
        }

        try {
            long startTime = System.currentTimeMillis();

            logger.debug("Executing Claude query with prompt length: {}", prompt.length());

            // 执行 Claude CLI 命令 - 使用管道输入
            List<String> command = new ArrayList<>();
            command.add(claudeCommand);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 通过标准输入发送提示词
            try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(prompt);
                writer.flush();
            }

            // 读取输出
            String output = readProcessOutput(process);

            // 等待进程完成
            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            long executionTime = System.currentTimeMillis() - startTime;

            if (!finished) {
                process.destroyForcibly();
                return ClaudeQueryResponse.failure(-1, "Query timeout after " + timeoutMs + "ms", executionTime, "claude query");
            }

            int exitCode = process.exitValue();
            if (exitCode == 0 && !output.trim().isEmpty()) {
                return ClaudeQueryResponse.success(output, executionTime, "claude query");
            } else {
                return ClaudeQueryResponse.failure(exitCode, "Claude CLI returned error: " + output, executionTime, "claude query");
            }

        } catch (Exception e) {
            logger.error("Error executing Claude query", e);
            return ClaudeQueryResponse.exception(e, "claude query");
        }
    }

    @Override
    public ClaudeQueryResponse reviewCodeChanges(String diffContent, String projectContext,
                                               String commitMessage, String reviewMode) {
        if (!isAvailable()) {
            return ClaudeQueryResponse.failure(-1, "Claude CLI is not available", 0, "N/A");
        }

        try {
            long startTime = System.currentTimeMillis();

            // 构建审查提示词
            String prompt = buildReviewPrompt(diffContent, projectContext, commitMessage, reviewMode);

            logger.debug("Executing Claude review with prompt length: {}", prompt.length());

            // 执行 Claude CLI 命令 - 使用管道输入
            List<String> command = new ArrayList<>();
            command.add(claudeCommand);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 通过标准输入发送提示词
            try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(prompt);
                writer.flush();
            }

            // 读取输出
            String output = readProcessOutput(process);

            // 等待进程完成
            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            long executionTime = System.currentTimeMillis() - startTime;

            if (!finished) {
                process.destroyForcibly();
                return ClaudeQueryResponse.failure(-1, "Review timeout after " + timeoutMs + "ms", executionTime, "claude review");
            }

            int exitCode = process.exitValue();
            if (exitCode == 0 && !output.trim().isEmpty()) {
                return ClaudeQueryResponse.success(output, executionTime, "claude review");
            } else {
                return ClaudeQueryResponse.failure(exitCode, "Claude CLI returned error: " + output, executionTime, "claude review");
            }

        } catch (Exception e) {
            logger.error("Error executing Claude review", e);
            return ClaudeQueryResponse.exception(e, "claude review");
        }
    }

    @Override
    public String getVersion() {
        if (!isAvailable()) {
            return "Claude CLI not available";
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(claudeCommand, "--version");
            Process process = pb.start();

            String output = readProcessOutput(process);
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);

            if (finished && process.exitValue() == 0) {
                return "Claude CLI: " + output.trim();
            } else {
                return "Claude CLI version unknown";
            }
        } catch (Exception e) {
            logger.error("Failed to get Claude CLI version", e);
            return "Claude CLI version error: " + e.getMessage();
        }
    }

    /**
     * 读取进程输出
     */
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

    /**
     * 加载审查提示词配置
     */
    private void loadReviewPrompts() {
        try {
            ClassPathResource resource = new ClassPathResource("review-prompts.properties");
            try (InputStream is = resource.getInputStream()) {
                reviewPrompts.load(new InputStreamReader(is, StandardCharsets.UTF_8));
                logger.info("Loaded {} review prompt templates", reviewPrompts.size());
            }
        } catch (IOException e) {
            logger.error("Failed to load review-prompts.properties", e);
        }
    }

    /**
     * 构建审查提示词
     */
    private String buildReviewPrompt(String diffContent, String projectContext,
                                     String commitMessage, String reviewMode) {
        // 从配置文件加载对应模式的 Prompt 模板
        String promptKey = "review.prompt." + reviewMode.toLowerCase();
        String promptTemplate = reviewPrompts.getProperty(promptKey);

        if (promptTemplate == null) {
            logger.warn("No prompt template found for mode: {}, using standard", reviewMode);
            promptTemplate = reviewPrompts.getProperty("review.prompt.standard",
                    getDefaultPrompt());
        }

        StringBuilder prompt = new StringBuilder();

        // 添加项目上下文
        if (projectContext != null && !projectContext.isEmpty()) {
            prompt.append("## 项目上下文\n");
            prompt.append(projectContext).append("\n\n");
        }

        // 添加提交信息
        if (commitMessage != null && !commitMessage.isEmpty()) {
            prompt.append("## 变更说明\n");
            prompt.append(commitMessage).append("\n\n");
        }

        // 添加审查提示词
        prompt.append(promptTemplate).append("\n\n");

        // 添加代码变更内容
        prompt.append("## 代码变更\n");
        prompt.append("```diff\n");
        prompt.append(diffContent);
        prompt.append("\n```\n");

        return prompt.toString();
    }

    /**
     * 获取默认提示词（当配置文件加载失败时使用）
     */
    private String getDefaultPrompt() {
        return "请审查此代码变更，重点关注：\n" +
               "- Bug 和潜在的逻辑错误\n" +
               "- 安全漏洞\n" +
               "- 性能瓶颈\n" +
               "- 代码质量问题";
    }
}