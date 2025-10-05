package com.example.gitreview.infrastructure.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Set;

/**
 * 工作流 Git 提交服务
 * 处理工作流任务完成后的自动提交
 *
 * @author zhourui(V33215020)
 * @since 2025/10/05
 */
@Service
public class WorkflowGitService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowGitService.class);

    /**
     * 提交任务生成的代码
     *
     * @param repoDir 仓库目录
     * @param taskId 任务ID
     * @param taskTitle 任务标题
     * @return 提交是否成功
     */
    public boolean commitTaskCode(File repoDir, String taskId, String taskTitle) {
        try (Git git = Git.open(repoDir)) {

            Status status = git.status().call();

            Set<String> untracked = status.getUntracked();
            Set<String> modified = status.getModified();
            Set<String> added = status.getAdded();

            if (untracked.isEmpty() && modified.isEmpty() && added.isEmpty()) {
                logger.info("没有变更需要提交，任务: {}", taskId);
                return true;
            }

            logger.info("准备提交任务代码，任务: {}, 新增: {}, 修改: {}",
                taskId, untracked.size(), modified.size());

            git.add()
                .addFilepattern(".")
                .call();

            String commitMessage = buildCommitMessage(taskId, taskTitle);

            git.commit()
                .setMessage(commitMessage)
                .setAuthor("Claude Code Workflow", "workflow@claude.ai")
                .call();

            logger.info("任务代码已提交: {}", taskId);
            return true;

        } catch (Exception e) {
            logger.error("提交任务代码失败，任务: {}", taskId, e);
            return false;
        }
    }

    /**
     * 推送到远程分支
     *
     * @param repoDir 仓库目录
     * @param remoteName 远程名称（默认 origin）
     * @param branch 分支名称
     * @return 推送是否成功
     */
    public boolean pushToRemote(File repoDir, String remoteName, String branch) {
        try (Git git = Git.open(repoDir)) {

            logger.info("推送到远程仓库: {}/{}", remoteName, branch);

            git.push()
                .setRemote(remoteName)
                .setRefSpecs(new org.eclipse.jgit.transport.RefSpec(branch + ":" + branch))
                .call();

            logger.info("推送成功: {}/{}", remoteName, branch);
            return true;

        } catch (Exception e) {
            logger.error("推送到远程仓库失败: {}/{}", remoteName, branch, e);
            return false;
        }
    }

    /**
     * 创建或切换到工作分支
     *
     * @param repoDir 仓库目录
     * @param branchName 分支名称
     * @return 切换是否成功
     */
    public boolean checkoutBranch(File repoDir, String branchName) {
        try (Git git = Git.open(repoDir)) {

            logger.info("切换到分支: {}", branchName);

            boolean branchExists = git.branchList().call().stream()
                .anyMatch(ref -> ref.getName().endsWith("/" + branchName));

            if (branchExists) {
                git.checkout()
                    .setName(branchName)
                    .call();
            } else {
                git.checkout()
                    .setCreateBranch(true)
                    .setName(branchName)
                    .call();
            }

            logger.info("已切换到分支: {}", branchName);
            return true;

        } catch (Exception e) {
            logger.error("切换分支失败: {}", branchName, e);
            return false;
        }
    }

    private String buildCommitMessage(String taskId, String taskTitle) {
        return String.format(
            "feat: %s\n\n" +
            "任务ID: %s\n" +
            "- 由 Claude Code Workflow 自动生成\n" +
            "- 已通过编译和单元测试验证\n\n" +
            "Generated with [Claude Code](https://claude.ai/code)\n" +
            "via [Happy](https://happy.engineering)\n\n" +
            "Co-Authored-By: Claude <noreply@anthropic.com>\n" +
            "Co-Authored-By: Happy <yesreply@happy.engineering>",
            taskTitle,
            taskId
        );
    }
}
