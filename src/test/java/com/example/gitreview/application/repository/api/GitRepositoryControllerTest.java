package com.example.gitreview.application.repository.api;

import com.example.gitreview.application.repository.GitRepositoryApplicationService;
import com.example.gitreview.application.repository.dto.*;
import com.example.gitreview.domain.shared.exception.ResourceNotFoundException;
import com.example.gitreview.domain.shared.exception.ValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GitRepositoryController集成测试
 * 测试从前端请求到Controller的完整链路
 */
@WebMvcTest(GitRepositoryController.class)
public class GitRepositoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GitRepositoryApplicationService applicationService;

    private GitRepositoryDTO repositoryDTO;
    private GitRepositoryCreateDTO createDTO;
    private GitRepositoryUpdateDTO updateDTO;
    private BranchListDTO branchListDTO;
    private ConnectionTestResultDTO connectionTestResultDTO;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        repositoryDTO = new GitRepositoryDTO();
        repositoryDTO.setId(1L);
        repositoryDTO.setName("Test Repo");
        repositoryDTO.setUrl("https://github.com/test/repo.git");
        repositoryDTO.setDescription("Test Description");
        repositoryDTO.setUsername("testuser");

        createDTO = new GitRepositoryCreateDTO();
        createDTO.setName("Test Repo");
        createDTO.setUrl("https://github.com/test/repo.git");
        createDTO.setDescription("Test Description");
        createDTO.setUsername("testuser");
        createDTO.setPassword("testpass");

        updateDTO = new GitRepositoryUpdateDTO();
        updateDTO.setName("Updated Repo");
        updateDTO.setDescription("Updated Description");

        branchListDTO = new BranchListDTO();
        branchListDTO.setBranches(Arrays.asList("main", "develop", "feature/test"));

        connectionTestResultDTO = new ConnectionTestResultDTO();
        connectionTestResultDTO.setSuccess(true);
        connectionTestResultDTO.setMessage("Connection successful");
    }

    @Test
    void testGetAllRepositories() throws Exception {
        // Given
        List<GitRepositoryDTO> repositories = Arrays.asList(repositoryDTO);
        when(applicationService.getAllRepositories()).thenReturn(repositories);

        // When & Then
        mockMvc.perform(get("/api/repositories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Repo"))
                .andExpect(jsonPath("$[0].url").value("https://github.com/test/repo.git"));

        verify(applicationService).getAllRepositories();
    }

    @Test
    void testGetRepositoryById() throws Exception {
        // Given
        when(applicationService.getRepository(1L)).thenReturn(repositoryDTO);

        // When & Then
        mockMvc.perform(get("/api/repositories/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Repo"));

        verify(applicationService).getRepository(1L);
    }

    @Test
    void testGetRepositoryByIdNotFound() throws Exception {
        // Given
        when(applicationService.getRepository(999L))
                .thenThrow(new ResourceNotFoundException("Repository not found: 999"));

        // When & Then
        mockMvc.perform(get("/api/repositories/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(applicationService).getRepository(999L);
    }

    @Test
    void testCreateRepository() throws Exception {
        // Given
        when(applicationService.createRepository(any(GitRepositoryCreateDTO.class)))
                .thenReturn(repositoryDTO);

        // When & Then
        mockMvc.perform(post("/api/repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Repo"));

        verify(applicationService).createRepository(any(GitRepositoryCreateDTO.class));
    }

    @Test
    void testCreateRepositoryWithInvalidData() throws Exception {
        // Given
        when(applicationService.createRepository(any(GitRepositoryCreateDTO.class)))
                .thenThrow(new ValidationException("Repository name is required"));

        // When & Then
        mockMvc.perform(post("/api/repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isInternalServerError());

        verify(applicationService).createRepository(any(GitRepositoryCreateDTO.class));
    }

    @Test
    void testUpdateRepository() throws Exception {
        // Given
        GitRepositoryDTO updatedDTO = new GitRepositoryDTO();
        updatedDTO.setId(1L);
        updatedDTO.setName("Updated Repo");
        updatedDTO.setDescription("Updated Description");

        when(applicationService.updateRepository(eq(1L), any(GitRepositoryUpdateDTO.class)))
                .thenReturn(updatedDTO);

        // When & Then
        mockMvc.perform(put("/api/repositories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Updated Repo"));

        verify(applicationService).updateRepository(eq(1L), any(GitRepositoryUpdateDTO.class));
    }

    @Test
    void testDeleteRepository() throws Exception {
        // Given
        doNothing().when(applicationService).deleteRepository(1L);

        // When & Then
        mockMvc.perform(delete("/api/repositories/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(applicationService).deleteRepository(1L);
    }

    @Test
    void testDeleteRepositoryNotFound() throws Exception {
        // Given
        doThrow(new ResourceNotFoundException("Repository not found: 999"))
                .when(applicationService).deleteRepository(999L);

        // When & Then
        mockMvc.perform(delete("/api/repositories/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(applicationService).deleteRepository(999L);
    }

    @Test
    void testTestConnection() throws Exception {
        // Given
        when(applicationService.testConnection(1L)).thenReturn(connectionTestResultDTO);

        // When & Then
        mockMvc.perform(post("/api/repositories/1/test-connection")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Connection successful"));

        verify(applicationService).testConnection(1L);
    }

    @Test
    void testTestConnectionFailed() throws Exception {
        // Given
        ConnectionTestResultDTO failedResult = new ConnectionTestResultDTO();
        failedResult.setSuccess(false);
        failedResult.setMessage("Authentication failed");

        when(applicationService.testConnection(1L)).thenReturn(failedResult);

        // When & Then
        mockMvc.perform(post("/api/repositories/1/test-connection")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication failed"));

        verify(applicationService).testConnection(1L);
    }

    @Test
    void testGetRemoteBranches() throws Exception {
        // Given
        when(applicationService.getRemoteBranches(1L)).thenReturn(branchListDTO);

        // When & Then
        mockMvc.perform(get("/api/repositories/1/remote-branches")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branches[0]").value("main"))
                .andExpect(jsonPath("$.branches[1]").value("develop"))
                .andExpect(jsonPath("$.branches[2]").value("feature/test"));

        verify(applicationService).getRemoteBranches(1L);
    }

    @Test
    void testGetRemoteBranchesNotFound() throws Exception {
        // Given
        when(applicationService.getRemoteBranches(999L))
                .thenThrow(new ResourceNotFoundException("Repository not found: 999"));

        // When & Then
        mockMvc.perform(get("/api/repositories/999/remote-branches")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(applicationService).getRemoteBranches(999L);
    }

    @Test
    void testCheckRepositoryStatus() throws Exception {
        // Given
        when(applicationService.checkRepositoryStatus(1L)).thenReturn(repositoryDTO);

        // When & Then
        mockMvc.perform(get("/api/repositories/1/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(applicationService).checkRepositoryStatus(1L);
    }

    @Test
    void testGetUserRepositories() throws Exception {
        // Given
        List<GitRepositoryDTO> repositories = Arrays.asList(repositoryDTO);
        when(applicationService.getUserRepositories()).thenReturn(repositories);

        // When & Then
        mockMvc.perform(get("/api/repositories/user/current")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Repo"));

        verify(applicationService).getUserRepositories();
    }
}
