package com.example.gitreview.infrastructure.claude;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 使用Claude CLI执行Git操作的服务
 */
@Service
public class ClaudeGitService {

    private static final Logger logger = LoggerFactory.getLogger(ClaudeGitService.class);

    @Value("${claude.command:claude}")
    private String claudeCommand;

    @Value("${claude.git.timeout:120000}")
    private long gitTimeout;

    private String actualClaudeCommand;

    @PostConstruct
    public void init() {
        // Windows环境下需要使用.cmd后缀
        actualClaudeCommand = claudeCommand;
        if (System.getProperty("os.name").toLowerCase().contains("windows")
            && !claudeCommand.endsWith(".cmd") && !claudeCommand.endsWith(".exe")) {
            actualClaudeCommand = claudeCommand + ".cmd";
        }
        logger.info("ClaudeGitService initialized with command: {}", actualClaudeCommand);
    }

    /**
     * 使用Claude CLI提交并推送代码
     *
     * @param repoDir 仓库目录
     * @param commitMessage 提交信息
     * @param push 是否推送到远程
     * @return 执行结果
     */
    public GitOperationResult commitAndPush(File repoDir, String commitMessage, boolean push) {
        try {
            logger.info("Committing changes with Claude CLI in directory: {}", repoDir);

            // 构建Claude CLI命令
            String prompt = buildGitCommitPrompt(commitMessage, push);

            // 执行Claude CLI
            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(repoDir);

            List<String> command = new ArrayList<>();
            command.add(actualClaudeCommand);

            pb.command(command);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // 发送提示词
            process.getOutputStream().write(prompt.getBytes(StandardCharsets.UTF_8));
            process.getOutputStream().flush();
            process.getOutputStream().close();

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    logger.debug("Claude output: {}", line);
                }
            }

            // 等待完成
            boolean finished = process.waitFor(gitTimeout, TimeUnit.MILLISECONDS);

            if (!finished) {
                process.destroyForcibly();
                return GitOperationResult.failure("Git operation timeout after " + gitTimeout + "ms");
            }

            int exitCode = process.exitValue();
            String result = output.toString();

            if (exitCode == 0) {
                return GitOperationResult.success(result);
            } else {
                return GitOperationResult.failure("Git operation failed with exit code: " + exitCode + "\n" + result);
            }

        } catch (Exception e) {
            logger.error("Failed to commit and push with Claude CLI", e);
            return GitOperationResult.failure("Exception: " + e.getMessage());
        }
    }

    /**
     * 构建Git提交的提示词
     */
    private String buildGitCommitPrompt(String commitMessage, boolean push) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("请执行以下Git操作：\n\n");
        prompt.append("1. 添加所有测试文件到暂存区：git add .\n");
        prompt.append("2. 使用以下提交信息创建提交：\n");
        prompt.append("```\n");
        prompt.append(commitMessage);
        prompt.append("\n```\n");

        if (push) {
            prompt.append("3. 推送到远程仓库：git push\n");
        }

        prompt.append("\n请直接执行这些Git命令，不要询问确认。");

        return prompt.toString();
    }

    /**
     * Git操作结果
     */
    public static class GitOperationResult {
        private final boolean success;
        private final String message;
        private final String output;

        private GitOperationResult(boolean success, String message, String output) {
            this.success = success;
            this.message = message;
            this.output = output;
        }

        public static GitOperationResult success(String output) {
            return new GitOperationResult(true, "Success", output);
        }

        public static GitOperationResult failure(String message) {
            return new GitOperationResult(false, message, "");
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getOutput() {
            return output;
        }
    }
}
