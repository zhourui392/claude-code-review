package com.example.gitreview.application.codereview.api;

import com.example.gitreview.application.codereview.service.CodeReviewApplicationService;
import com.example.gitreview.application.repository.GitRepositoryApplicationService;
import com.example.gitreview.application.repository.dto.GitRepositoryDTO;
import com.example.gitreview.infrastructure.claude.ClaudeQueryPort;
import com.example.gitreview.infrastructure.claude.ClaudeQueryResponse;
import com.example.gitreview.infrastructure.git.GitOperationPort;
import org.eclipse.jgit.diff.DiffEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ReviewController集成测试
 * 测试代码审查完整链路，包括Claude CLI调用流程
 */
@WebMvcTest(ReviewController.class)
public class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CodeReviewApplicationService codeReviewApplicationService;

    @MockBean
    private GitRepositoryApplicationService gitRepositoryApplicationService;

    @MockBean
    private ClaudeQueryPort claudeQueryPort;

    @MockBean
    private GitOperationPort gitOperationPort;

    private GitRepositoryDTO repositoryDTO;
    private ClaudeQueryResponse claudeSuccessResponse;
    private ClaudeQueryResponse claudeErrorResponse;
    private File mockRepoDir;
    private List<DiffEntry> mockDiffEntries;

    @BeforeEach
    void setUp() {
        // 准备仓库数据
        repositoryDTO = new GitRepositoryDTO();
        repositoryDTO.setId(1L);
        repositoryDTO.setName("Test Repo");
        repositoryDTO.setUrl("https://github.com/test/repo.git");
        repositoryDTO.setUsername("testuser");
        repositoryDTO.setEncryptedPassword("testpass");

        // 准备Claude响应
        claudeSuccessResponse = ClaudeQueryResponse.success(
                "Code review completed successfully.\n\n" +
                "## Summary\n" +
                "- Found 3 issues\n" +
                "- 2 warnings\n" +
                "- Overall quality: Good\n\n" +
                "## Details\n" +
                "1. Null pointer check missing in UserService.java:45\n" +
                "2. Resource leak in FileHandler.java:120\n" +
                "3. Deprecated API usage in Utils.java:78",
                1000L,
                "claude --print"
        );

        claudeErrorResponse = ClaudeQueryResponse.failure(
                1,
                "Claude CLI execution failed",
                500L,
                "claude --print"
        );

        // 准备Git相关Mock数据
        mockRepoDir = new File(System.getProperty("java.io.tmpdir"), "test-repo");
        mockDiffEntries = Arrays.asList(
                mock(DiffEntry.class),
                mock(DiffEntry.class)
        );
    }

    @Test
    void testReviewWithClaudeSuccess() throws Exception {
        // Given
        when(gitRepositoryApplicationService.getRepository(1L)).thenReturn(repositoryDTO);
        when(claudeQueryPort.isAvailable()).thenReturn(true);
        when(gitOperationPort.cloneRepository(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mockRepoDir);
        when(gitOperationPort.getDiffBetweenBranches(any(File.class), anyString(), anyString()))
                .thenReturn(mockDiffEntries);
        when(gitOperationPort.getDiffContent(any(File.class), any(DiffEntry.class)))
                .thenReturn("diff --git a/test.java b/test.java\n+added line");
        when(claudeQueryPort.reviewCodeChanges(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(claudeSuccessResponse);

        // When & Then
        mockMvc.perform(post("/api/review/1/claude")
                        .param("baseBranch", "main")
                        .param("targetBranch", "feature/test")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Code review completed successfully")))
                .andExpect(content().string(containsString("Found 3 issues")));

        // 验证调用链路
        verify(gitRepositoryApplicationService).getRepository(1L);
        verify(claudeQueryPort).isAvailable();
        verify(gitOperationPort).cloneRepository(
                eq("https://github.com/test/repo.git"),
                eq("testuser"),
                eq("testpass"),
                eq("feature/test")
        );
        verify(gitOperationPort).getDiffBetweenBranches(mockRepoDir, "main", "feature/test");
        verify(claudeQueryPort).reviewCodeChanges(
                anyString(),
                eq("Git代码审查项目 - Test Repo"),
                eq("代码审查: main -> feature/test"),
                eq("standard") // 默认模式已改为 standard
        );
    }

    @Test
    void testReviewWithClaudeRepositoryNotFound() throws Exception {
        // Given
        when(gitRepositoryApplicationService.getRepository(999L)).thenReturn(null);

        // When & Then
        mockMvc.perform(post("/api/review/999/claude")
                        .param("baseBranch", "main")
                        .param("targetBranch", "feature/test")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("仓库不存在: 999")));

        verify(gitRepositoryApplicationService).getRepository(999L);
        verify(claudeQueryPort, never()).isAvailable();
        verify(gitOperationPort, never()).cloneRepository(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testReviewWithClaudeNotAvailable() throws Exception {
        // Given
        when(gitRepositoryApplicationService.getRepository(1L)).thenReturn(repositoryDTO);
        when(claudeQueryPort.isAvailable()).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/review/1/claude")
                        .param("baseBranch", "main")
                        .param("targetBranch", "feature/test")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(containsString("Claude CLI 服务不可用")));

        verify(gitRepositoryApplicationService).getRepository(1L);
        verify(claudeQueryPort).isAvailable();
        verify(gitOperationPort, never()).cloneRepository(anyString(), anyString(), anyString(), anyString());
        verify(claudeQueryPort, never()).reviewCodeChanges(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testReviewWithClaudeNoDiff() throws Exception {
        // Given
        when(gitRepositoryApplicationService.getRepository(1L)).thenReturn(repositoryDTO);
        when(claudeQueryPort.isAvailable()).thenReturn(true);
        when(gitOperationPort.cloneRepository(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mockRepoDir);
        when(gitOperationPort.getDiffBetweenBranches(any(File.class), anyString(), anyString()))
                .thenReturn(Arrays.asList()); // 空的diff列表

        // When & Then
        mockMvc.perform(post("/api/review/1/claude")
                        .param("baseBranch", "main")
                        .param("targetBranch", "feature/test")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("未发现代码差异")));

        verify(gitRepositoryApplicationService).getRepository(1L);
        verify(claudeQueryPort).isAvailable();
        verify(gitOperationPort).cloneRepository(anyString(), anyString(), anyString(), anyString());
        verify(gitOperationPort).getDiffBetweenBranches(mockRepoDir, "main", "feature/test");
        verify(claudeQueryPort, never()).reviewCodeChanges(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testReviewWithClaudeExecutionFailed() throws Exception {
        // Given
        when(gitRepositoryApplicationService.getRepository(1L)).thenReturn(repositoryDTO);
        when(claudeQueryPort.isAvailable()).thenReturn(true);
        when(gitOperationPort.cloneRepository(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mockRepoDir);
        when(gitOperationPort.getDiffBetweenBranches(any(File.class), anyString(), anyString()))
                .thenReturn(mockDiffEntries);
        when(gitOperationPort.getDiffContent(any(File.class), any(DiffEntry.class)))
                .thenReturn("diff content");
        when(claudeQueryPort.reviewCodeChanges(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(claudeErrorResponse);

        // When & Then
        mockMvc.perform(post("/api/review/1/claude")
                        .param("baseBranch", "main")
                        .param("targetBranch", "feature/test")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("代码审查失败")))
                .andExpect(content().string(containsString("Claude CLI execution failed")));

        verify(claudeQueryPort).reviewCodeChanges(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testReviewWithGitOperationException() throws Exception {
        // Given
        when(gitRepositoryApplicationService.getRepository(1L)).thenReturn(repositoryDTO);
        when(claudeQueryPort.isAvailable()).thenReturn(true);
        when(gitOperationPort.cloneRepository(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Git clone failed: Authentication error"));

        // When & Then
        mockMvc.perform(post("/api/review/1/claude")
                        .param("baseBranch", "main")
                        .param("targetBranch", "feature/test")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("代码审查失败")))
                .andExpect(content().string(containsString("Authentication error")));

        verify(gitOperationPort).cloneRepository(anyString(), anyString(), anyString(), anyString());
        verify(claudeQueryPort, never()).reviewCodeChanges(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testReviewWithLargeDiff() throws Exception {
        // Given - 模拟大量diff文件
        when(gitRepositoryApplicationService.getRepository(1L)).thenReturn(repositoryDTO);
        when(claudeQueryPort.isAvailable()).thenReturn(true);
        when(gitOperationPort.cloneRepository(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mockRepoDir);
        when(gitOperationPort.getDiffBetweenBranches(any(File.class), anyString(), anyString()))
                .thenReturn(mockDiffEntries);

        // 模拟大的diff内容
        StringBuilder largeDiff = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeDiff.append("diff line ").append(i).append("\n");
        }
        when(gitOperationPort.getDiffContent(any(File.class), any(DiffEntry.class)))
                .thenReturn(largeDiff.toString());
        when(claudeQueryPort.reviewCodeChanges(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(claudeSuccessResponse);

        // When & Then
        mockMvc.perform(post("/api/review/1/claude")
                        .param("baseBranch", "main")
                        .param("targetBranch", "feature/test")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk());

        verify(claudeQueryPort).reviewCodeChanges(
                argThat(diff -> diff.length() > 10000), // 验证diff内容较大
                anyString(),
                anyString(),
                anyString()
        );
    }
}
