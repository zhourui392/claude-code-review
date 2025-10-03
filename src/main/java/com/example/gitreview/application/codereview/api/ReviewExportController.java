package com.example.gitreview.application.codereview.api;

import com.example.gitreview.application.codereview.service.CodeReviewApplicationService;
import com.example.gitreview.domain.codereview.model.aggregate.CodeReview;
import com.example.gitreview.domain.codereview.model.valueobject.ReviewResult;
import com.example.gitreview.domain.codereview.repository.CodeReviewRepository;
import com.example.gitreview.infrastructure.export.MarkdownExporter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * ReviewExportController
 * 代码审查结果导出控制器
 *
 * @author zhourui(V33215020)
 * @since 2025/10/03
 */
@RestController
@CrossOrigin(origins = "*")
public class ReviewExportController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewExportController.class);

    @Autowired
    private CodeReviewRepository codeReviewRepository;

    @Autowired
    private MarkdownExporter markdownExporter;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 导出Markdown格式报告
     *
     * @param reviewId 审查ID
     * @param repositoryName 仓库名称（可选）
     * @param baseBranch 基础分支（可选）
     * @param targetBranch 目标分支（可选）
     * @param mode 审查模式（可选）
     * @return Markdown格式的报告
     */
    @GetMapping("/api/review/{reviewId}/export/markdown")
    public ResponseEntity<byte[]> exportMarkdown(
            @PathVariable Long reviewId,
            @RequestParam(required = false, defaultValue = "Unknown Repository") String repositoryName,
            @RequestParam(required = false, defaultValue = "main") String baseBranch,
            @RequestParam(required = false, defaultValue = "feature") String targetBranch,
            @RequestParam(required = false, defaultValue = "standard") String mode) {

        logger.info("Exporting review {} to Markdown", reviewId);

        try {
            CodeReview review = codeReviewRepository.findById(reviewId)
                    .orElseThrow(() -> new RuntimeException("Review not found: " + reviewId));

            if (review.getFinalResult() == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("审查未完成，无法导出".getBytes(StandardCharsets.UTF_8));
            }

            String markdown = markdownExporter.export(review, repositoryName, baseBranch, targetBranch, mode);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(new MediaType("text", "markdown", StandardCharsets.UTF_8));
            headers.setContentDispositionFormData("attachment",
                    String.format("review-%d-%s.md",
                            reviewId,
                            review.getUpdateTime().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))));

            return new ResponseEntity<>(markdown.getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Failed to export Markdown for review {}", reviewId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("导出失败: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 导出JSON格式报告
     *
     * @param reviewId 审查ID
     * @return JSON格式的完整审查结果
     */
    @GetMapping("/api/review/{reviewId}/export/json")
    public ResponseEntity<byte[]> exportJson(@PathVariable Long reviewId) {
        logger.info("Exporting review {} to JSON", reviewId);

        try {
            CodeReview review = codeReviewRepository.findById(reviewId)
                    .orElseThrow(() -> new RuntimeException("Review not found: " + reviewId));

            ReviewResult result = review.getFinalResult();
            if (result == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("{\"error\":\"审查未完成，无法导出\"}".getBytes(StandardCharsets.UTF_8));
            }

            // 构建导出对象
            ExportData exportData = new ExportData(review, result);
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportData);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDispositionFormData("attachment",
                    String.format("review-%d-%s.json",
                            reviewId,
                            review.getUpdateTime().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))));

            return new ResponseEntity<>(json.getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Failed to export JSON for review {}", reviewId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("{\"error\":\"导出失败: " + e.getMessage() + "\"}").getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * JSON导出数据封装类
     */
    public static class ExportData {
        private Long reviewId;
        private String status;
        private String createTime;
        private String updateTime;
        private int progress;
        private ReviewResult result;

        public ExportData(CodeReview review, ReviewResult result) {
            this.reviewId = review.getId();
            this.status = review.getStatus().name();
            this.createTime = review.getCreateTime().format(DateTimeFormatter.ISO_DATE_TIME);
            this.updateTime = review.getUpdateTime().format(DateTimeFormatter.ISO_DATE_TIME);
            this.progress = review.getProgress();
            this.result = result;
        }

        // Getters
        public Long getReviewId() { return reviewId; }
        public String getStatus() { return status; }
        public String getCreateTime() { return createTime; }
        public String getUpdateTime() { return updateTime; }
        public int getProgress() { return progress; }
        public ReviewResult getResult() { return result; }
    }
}
