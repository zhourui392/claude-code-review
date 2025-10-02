package com.example.gitreview.infrastructure.claude;

/**
 * Claude查询端口接口
 * 定义Claude AI相关操作的抽象接口，支持适配器模式
 */
public interface ClaudeQueryPort {

    /**
     * 检查Claude CLI是否可用
     * @return 是否可用
     */
    boolean isAvailable();

    /**
     * 执行简单查询
     * @param prompt 查询提示词
     * @return 查询响应
     */
    ClaudeQueryResponse query(String prompt);

    /**
     * 代码审查查询
     * @param diffContent 代码差异内容
     * @param projectContext 项目上下文
     * @param commitMessage 提交信息
     * @param reviewMode 审查模式
     * @return 审查响应
     */
    ClaudeQueryResponse reviewCodeChanges(String diffContent, String projectContext,
                                         String commitMessage, String reviewMode);

    /**
     * 获取版本信息
     * @return 版本信息
     */
    String getVersion();
}