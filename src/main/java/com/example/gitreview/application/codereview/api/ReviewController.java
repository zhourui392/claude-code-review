package com.example.gitreview.application.codereview.api;

import com.example.gitreview.application.codereview.service.CodeReviewApplicationService;
import com.example.gitreview.application.repository.GitRepositoryApplicationService;
import com.example.gitreview.application.repository.dto.GitRepositoryDTO;
import com.example.gitreview.infrastructure.claude.ClaudeQueryPort;
import com.example.gitreview.infrastructure.claude.ClaudeQueryResponse;
import com.example.gitreview.infrastructure.git.GitOperationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ReviewController
 * 代码审查REST API控制器（完整实现版）
 */
@RestController
@Validated
@CrossOrigin(origins = "*")
public class ReviewController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    @Autowired
    private CodeReviewApplicationService codeReviewApplicationService;

    @Autowired
    private GitRepositoryApplicationService gitRepositoryApplicationService;

    @Autowired
    private ClaudeQueryPort claudeQueryPort;

    @Autowired
    private GitOperationPort gitOperationPort;

    /**
     * 简化的代码审查接口（直接审查版）
     */
    @PostMapping("/api/review/{repositoryId}/claude")
    public ResponseEntity<String> reviewWithClaude(
            @PathVariable Long repositoryId,
            @RequestParam String baseBranch,
            @RequestParam String targetBranch) {
        logger.info("Starting Claude review for repository {} from {} to {}", repositoryId, baseBranch, targetBranch);

        try {
            // 获取仓库信息
            GitRepositoryDTO repository = gitRepositoryApplicationService.getRepository(repositoryId);
            if (repository == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("仓库不存在: " + repositoryId);
            }

            // 检查Claude是否可用
            if (!claudeQueryPort.isAvailable()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Claude CLI 服务不可用，请确保已正确安装和配置 Claude CLI 工具");
            }

            // 获取代码差异
            logger.info("Cloning repository and getting diff...");
            File repoDir = gitOperationPort.cloneRepository(
                    repository.getUrl(),
                    repository.getUsername(),
                    repository.getEncryptedPassword(),
                    targetBranch
            );

            // 获取分支差异
            List<org.eclipse.jgit.diff.DiffEntry> diffEntries = gitOperationPort.getDiffBetweenBranches(
                    repoDir, baseBranch, targetBranch);

            if (diffEntries.isEmpty()) {
                return ResponseEntity.ok("未发现代码差异，两个分支内容相同");
            }

            // 生成差异内容
            StringBuilder diffContent = new StringBuilder();
            for (org.eclipse.jgit.diff.DiffEntry entry : diffEntries) {
                String fileDiff = gitOperationPort.getDiffContent(repoDir, entry);
                diffContent.append(fileDiff).append("\n");
            }

            // 调用Claude进行审查
            logger.info("Calling Claude for code review...");
            ClaudeQueryResponse response = claudeQueryPort.reviewCodeChanges(
                    diffContent.toString(),
                    "Git代码审查项目 - " + repository.getName(),
                    "代码审查: " + baseBranch + " -> " + targetBranch,
                    "comprehensive"
            );

            if (response.isSuccessful()) {
                return ResponseEntity.ok(response.getOutput());
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("代码审查失败: " + response.getError());
            }

        } catch (Exception e) {
            logger.error("Failed to perform Claude review", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("代码审查失败: " + e.getMessage());
        }
    }

    /**
     * 获取代码审查状态
     */
    @GetMapping("/api/code-review/{reviewId}/status")
    public ResponseEntity<Map<String, Object>> getReviewStatus(@PathVariable Long reviewId) {
        logger.debug("Getting review status for {}", reviewId);

        try {
            var statusInfo = codeReviewApplicationService.getReviewStatus(reviewId);

            Map<String, Object> response = new HashMap<>();
            response.put("id", statusInfo.getId());
            response.put("status", statusInfo.getStatus().name());
            response.put("progress", statusInfo.getProgress());
            response.put("summary", statusInfo.getSummary());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to get review status {}", reviewId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
