package com.example.gitreview.infrastructure.claude;

import java.io.File;

/**
 * Claude Code CLI 端口接口
 * 用于调用 Claude Code CLI 在实际仓库中生成代码
 *
 * @author zhourui(V33215020)
 * @since 2025/10/05
 */
public interface ClaudeCodePort {

    /**
     * 检查 Claude Code CLI 是否可用
     *
     * @return true 如果可用
     */
    boolean isAvailable();

    /**
     * 在指定仓库目录中执行代码生成任务
     *
     * @param repoDir 仓库目录
     * @param taskDescription 任务描述
     * @param technicalDesign 技术方案
     * @param contextCode 上下文代码
     * @return 生成结果
     */
    ClaudeCodeResult generateCode(File repoDir, String taskDescription,
                                   String technicalDesign, String contextCode);

    /**
     * 修复编译或测试错误
     *
     * @param repoDir 仓库目录
     * @param errorType 错误类型 (COMPILATION / TEST)
     * @param errorOutput 错误输出信息
     * @param taskDescription 任务描述
     * @return 修复结果
     */
    ClaudeCodeResult fixCompilationError(File repoDir, String errorType,
                                         String errorOutput, String taskDescription);

    /**
     * 使用 Claude Code CLI 执行 Git 提交
     *
     * @param repoDir 仓库目录
     * @param commitMessageExample 提交信息示例模板
     * @param contextDescription 提交上下文描述
     * @return 提交结果
     */
    ClaudeCodeResult gitCommitAndPush(File repoDir, String commitMessageExample,
                                      String contextDescription);

    /**
     * 获取 Claude Code CLI 版本
     *
     * @return 版本信息
     */
    String getVersion();
}
