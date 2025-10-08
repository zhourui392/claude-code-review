package com.example.gitreview.infrastructure.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Git提交相关服务
 */
@Service
public class GitCommitService {

    private static final Logger logger = LoggerFactory.getLogger(GitCommitService.class);

    // 准入ID的正则模式，支持多种格式
    private static final Pattern[] GATE_ID_PATTERNS = {
        Pattern.compile("准入[ID|id|Id][:：]\\s*([A-Za-z0-9-_]+)"),
        Pattern.compile("gate[_-]?id[:：]\\s*([A-Za-z0-9-_]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\[([A-Za-z0-9-_]+)\\]"),  // [GATE-123]
        Pattern.compile("#([A-Za-z0-9-_]+)")  // #GATE-123
    };

    /**
     * 从Git仓库的最近提交中提取准入ID
     */
    public String extractGateIdFromHistory(File repoDir) {
        try (Git git = Git.open(repoDir)) {
            Repository repository = git.getRepository();
            ObjectId head = repository.resolve("HEAD");

            if (head == null) {
                logger.warn("No HEAD found in repository: {}", repoDir);
                return null;
            }

            try (RevWalk revWalk = new RevWalk(repository)) {
                RevCommit commit = revWalk.parseCommit(head);

                // 检查最近5个提交
                int maxCommits = 5;
                int count = 0;

                while (commit != null && count < maxCommits) {
                    String commitMessage = commit.getFullMessage();
                    logger.debug("Checking commit: {} - {}", commit.getName(), commitMessage);

                    String gateId = extractGateIdFromMessage(commitMessage);
                    if (gateId != null) {
                        logger.info("Found gate ID: {} in commit: {}", gateId, commit.getName());
                        return gateId;
                    }

                    // 移动到下一个提交
                    RevCommit[] parents = commit.getParents();
                    if (parents.length > 0) {
                        commit = revWalk.parseCommit(parents[0]);
                    } else {
                        break;
                    }
                    count++;
                }
            }

            logger.warn("No gate ID found in recent commits");
            return null;

        } catch (Exception e) {
            logger.error("Failed to extract gate ID from git history: {}", repoDir, e);
            return null;
        }
    }

    /**
     * 从提交信息中提取准入ID
     */
    private String extractGateIdFromMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return null;
        }

        for (Pattern pattern : GATE_ID_PATTERNS) {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                String gateId = matcher.group(1);
                if (gateId != null && !gateId.trim().isEmpty()) {
                    return gateId.trim();
                }
            }
        }

        return null;
    }

    /**
     * 构建批量测试生成的提交信息
     * @param classCount 类数量
     * @param gateId 准入ID
     * @param repoDir 仓库目录（用于从历史中提取准入ID）
     * @return 提交信息
     */
    public String buildBatchCommitMessage(int classCount, String gateId, File repoDir) {
        // 如果没有传入 gateId，尝试从历史提交中提取
        String finalGateId = gateId;
        if (finalGateId == null || finalGateId.trim().isEmpty()) {
            logger.info("未提供准入ID，尝试从历史提交中提取...");
            finalGateId = extractGateIdFromHistory(repoDir);
            if (finalGateId != null) {
                logger.info("从历史提交中提取到准入ID: {}", finalGateId);
            } else {
                logger.warn("未能从历史提交中提取准入ID");
            }
        }
        
        StringBuilder message = new StringBuilder();
        
        // 标题
        message.append("test: 新增 ").append(classCount).append(" 个类的单元测试\n");
        
        // 适用范围
        message.append("适用范围：{无}\n");
        
        // 准入ID
        if (finalGateId != null && !finalGateId.trim().isEmpty()) {
            String formattedGateId = finalGateId.trim();
            if (!formattedGateId.startsWith("#")) {
                formattedGateId = "#" + formattedGateId;
            }
            message.append("准入id：{").append(formattedGateId).append("}\n");
        } else {
            message.append("准入id：{无}\n");
        }
        
        // 分析
        message.append("分析：{使用Claude AI自动生成单元测试}\n");
        
        // 方案
        if (classCount == 1) {
            message.append("方案：{新增测试类}\n");
        } else {
            message.append("方案：{新增 ").append(classCount).append(" 个测试类}\n");
        }
        
        // 风险及影响
        message.append("风险及影响[快/稳/省/功能/安全隐私]：{功能}\n");
        
        // 测试建议
        message.append("测试建议：{已自动生成单元测试并验证通过}");
        
        return message.toString();
    }
    
    /**
     * 构建单个类的提交信息（已废弃，使用 buildBatchCommitMessage）
     * @deprecated 使用 buildBatchCommitMessage 代替
     */
    @Deprecated
    public String buildCommitMessage(String className, String gateId) {
        return buildBatchCommitMessage(1, gateId, null);
    }

    /**
     * 推送到远程仓库（当前分支，带认证）
     * 
     * @param repoDir 仓库目录
     * @param username 用户名（可为null）
     * @param password 密码（可为null）
     * @return 推送是否成功
     */
    public boolean pushToRemoteWithAuth(File repoDir, String username, String password) {
        try (Git git = Git.open(repoDir)) {
            // 获取当前分支
            String currentBranch = git.getRepository().getBranch();
            
            logger.info("推送当前分支到远程仓库: origin/{}", currentBranch);
            
            var pushCommand = git.push()
                .setRemote("origin")
                .setRefSpecs(new org.eclipse.jgit.transport.RefSpec(currentBranch + ":" + currentBranch));
            
            // 如果提供了认证信息，设置凭据提供者
            if (username != null && password != null) {
                pushCommand.setCredentialsProvider(
                    new org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider(username, password)
                );
                logger.debug("使用认证信息推送: username={}", username);
            }
            
            pushCommand.call();
            
            logger.info("推送成功: origin/{}", currentBranch);
            return true;
            
        } catch (Exception e) {
            logger.error("推送到远程仓库失败", e);
            return false;
        }
    }

    /**
     * 推送到远程仓库（指定远程名称和分支）
     * 
     * @param repoDir 仓库目录
     * @param remoteName 远程名称（如 origin）
     * @param branch 分支名称
     * @return 推送是否成功
     */
    public boolean pushToRemote(File repoDir, String remoteName, String branch) {
        return pushToRemote(repoDir, remoteName, branch, null, null);
    }
    
    /**
     * 推送到远程仓库（指定远程名称、分支和认证信息）
     * 
     * @param repoDir 仓库目录
     * @param remoteName 远程名称（如 origin）
     * @param branch 分支名称
     * @param username 用户名
     * @param password 密码
     * @return 推送是否成功
     */
    public boolean pushToRemote(File repoDir, String remoteName, String branch, String username, String password) {
        try (Git git = Git.open(repoDir)) {
            logger.info("推送到远程仓库: {}/{}", remoteName, branch);
            
            var pushCommand = git.push()
                .setRemote(remoteName)
                .setRefSpecs(new org.eclipse.jgit.transport.RefSpec(branch + ":" + branch));
            
            // 如果提供了认证信息，设置凭据提供者
            if (username != null && password != null) {
                pushCommand.setCredentialsProvider(
                    new org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider(username, password)
                );
                logger.debug("使用认证信息推送: username={}", username);
            }
            
            pushCommand.call();
            
            logger.info("推送成功: {}/{}", remoteName, branch);
            return true;
            
        } catch (Exception e) {
            logger.error("推送到远程仓库失败: {}/{}", remoteName, branch, e);
            return false;
        }
    }
}
