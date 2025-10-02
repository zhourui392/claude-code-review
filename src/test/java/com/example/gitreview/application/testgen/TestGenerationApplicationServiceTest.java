package com.example.gitreview.application.testgen;

import com.example.gitreview.application.testgen.assembler.TestGenerationAssembler;
import com.example.gitreview.application.testgen.dto.TestGenerationRequestDTO;
import com.example.gitreview.application.testgen.dto.TestGenerationResultDTO;
import com.example.gitreview.application.testgen.dto.TestStatusDTO;
import com.example.gitreview.application.testgen.dto.TestSuiteDTO;
import com.example.gitreview.domain.shared.exception.BusinessRuleException;
import com.example.gitreview.domain.shared.exception.ResourceNotFoundException;
import com.example.gitreview.domain.shared.exception.ValidationException;
import com.example.gitreview.domain.shared.model.aggregate.Repository;
import com.example.gitreview.domain.shared.model.valueobject.Credential;
import com.example.gitreview.domain.shared.model.valueobject.GitUrl;
import com.example.gitreview.domain.shared.repository.GitRepositoryRepository;
import com.example.gitreview.domain.testgen.model.aggregate.TestSuite;
import com.example.gitreview.domain.testgen.model.valueobject.JavaClass;
import com.example.gitreview.domain.testgen.model.valueobject.TestTemplate;
import com.example.gitreview.domain.testgen.repository.TestSuiteRepository;
import com.example.gitreview.domain.testgen.service.TestGenerationDomainService;
import com.example.gitreview.infrastructure.claude.ClaudeQueryPort;
import com.example.gitreview.infrastructure.git.GitOperationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TestGenerationApplicationService Test
 * 完整测试测试生成应用服务的业务逻辑
 */
@ExtendWith(MockitoExtension.class)
public class TestGenerationApplicationServiceTest {

    @Mock
    private GitRepositoryRepository gitRepositoryRepository;

    @Mock
    private TestSuiteRepository testSuiteRepository;

    @Mock
    private TestGenerationDomainService testGenerationDomainService;

    @Mock
    private GitOperationPort gitOperationPort;

    @Mock
    private ClaudeQueryPort claudeQueryPort;

    @Mock
    private TestGenerationAssembler assembler;

    @InjectMocks
    private TestGenerationApplicationService testGenerationApplicationService;

    private Repository testRepository;
    private TestGenerationRequestDTO requestDTO;
    private TestSuite testSuite;
    private TestStatusDTO testStatusDTO;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        GitUrl gitUrl = new GitUrl("https://github.com/test/repo.git");
        Credential credential = new Credential("testuser", "testpass");
        testRepository = new Repository("Test Repo", "Test Description", gitUrl, credential);
        testRepository.setId(1L);

        // 准备测试请求
        requestDTO = new TestGenerationRequestDTO();
        requestDTO.setRepositoryId(1L);
        requestDTO.setClassName("UserService");
        requestDTO.setBranch("main");
        requestDTO.setTestType("basic");
        requestDTO.setQualityLevel(3);

        // 准备测试套件
        JavaClass javaClass = new JavaClass("UserService", "com.example.service", List.of(), List.of());
        TestTemplate template = new TestTemplate(
                TestTemplate.TestType.BASIC,
                3,
                "mockito",
                "junit5",
                List.of()
        );
        testSuite = new TestSuite(1L, "UserServiceTests", "Test suite for UserService",
                javaClass, template, "system");
        testSuite.setId(1L);

        // 准备DTO
        testStatusDTO = new TestStatusDTO();
        testStatusDTO.setTaskId("TG_1_123456789");
        testStatusDTO.setStatus("PENDING");
        testStatusDTO.setProgress(0);
    }

    @Test
    void testCreateTestGenerationTaskSuccess() {
        // Given
        when(gitRepositoryRepository.findById(1L)).thenReturn(Optional.of(testRepository));
        when(testSuiteRepository.save(any(TestSuite.class))).thenReturn(testSuite);
        when(assembler.toTestStatusDTO(any(TestSuite.class), anyString())).thenReturn(testStatusDTO);

        // When
        TestStatusDTO result = testGenerationApplicationService.createTestGenerationTask(requestDTO);

        // Then
        assertNotNull(result);
        assertEquals("TG_1_123456789", result.getTaskId());
        verify(gitRepositoryRepository).findById(1L);
        verify(testSuiteRepository).save(any(TestSuite.class));
        verify(assembler).toTestStatusDTO(any(TestSuite.class), anyString());
    }

    @Test
    void testCreateTestGenerationTaskWithInvalidRepositoryId() {
        // Given
        when(gitRepositoryRepository.findById(999L)).thenReturn(Optional.empty());
        requestDTO.setRepositoryId(999L);

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            testGenerationApplicationService.createTestGenerationTask(requestDTO);
        });

        verify(gitRepositoryRepository).findById(999L);
        verify(testSuiteRepository, never()).save(any());
    }

    @Test
    void testCreateTestGenerationTaskWithNullRepositoryId() {
        // Given
        requestDTO.setRepositoryId(null);

        // When & Then
        assertThrows(ValidationException.class, () -> {
            testGenerationApplicationService.createTestGenerationTask(requestDTO);
        });

        verify(gitRepositoryRepository, never()).findById(any());
        verify(testSuiteRepository, never()).save(any());
    }

    @Test
    void testCreateTestGenerationTaskWithEmptyClassName() {
        // Given
        requestDTO.setClassName("");

        // When & Then
        assertThrows(ValidationException.class, () -> {
            testGenerationApplicationService.createTestGenerationTask(requestDTO);
        });

        verify(gitRepositoryRepository, never()).findById(any());
        verify(testSuiteRepository, never()).save(any());
    }

    @Test
    void testCreateTestGenerationTaskWithEmptyBranch() {
        // Given
        requestDTO.setBranch("");

        // When & Then
        assertThrows(ValidationException.class, () -> {
            testGenerationApplicationService.createTestGenerationTask(requestDTO);
        });

        verify(gitRepositoryRepository, never()).findById(any());
        verify(testSuiteRepository, never()).save(any());
    }

    @Test
    void testGetTaskStatusFromCache() throws InterruptedException {
        // Given
        String taskId = "TG_1_123456789";
        when(gitRepositoryRepository.findById(1L)).thenReturn(Optional.of(testRepository));
        when(testSuiteRepository.save(any(TestSuite.class))).thenReturn(testSuite);
        when(assembler.toTestStatusDTO(any(TestSuite.class), anyString())).thenReturn(testStatusDTO);

        // 先创建任务（任务会被缓存）
        testGenerationApplicationService.createTestGenerationTask(requestDTO);

        // 等待异步任务启动
        Thread.sleep(100);

        // When - 从缓存获取状态
        TestStatusDTO result = testGenerationApplicationService.getTaskStatus(taskId);

        // Then
        assertNotNull(result);
        verify(testSuiteRepository, times(1)).save(any(TestSuite.class)); // 只在创建时保存
    }

    @Test
    void testGetTaskStatusFromDatabase() {
        // Given
        String taskId = "TG_1_123456789";
        when(testSuiteRepository.findById(1L)).thenReturn(Optional.of(testSuite));
        when(assembler.toTestStatusDTO(testSuite, taskId)).thenReturn(testStatusDTO);

        // When
        TestStatusDTO result = testGenerationApplicationService.getTaskStatus(taskId);

        // Then
        assertNotNull(result);
        verify(testSuiteRepository).findById(1L);
        verify(assembler).toTestStatusDTO(testSuite, taskId);
    }

    @Test
    void testGetTaskStatusWithInvalidTaskId() {
        // Given
        String invalidTaskId = "INVALID_TASK_ID";

        // When & Then
        assertThrows(ValidationException.class, () -> {
            testGenerationApplicationService.getTaskStatus(invalidTaskId);
        });
    }

    @Test
    void testGetTaskStatusNotFound() {
        // Given
        String taskId = "TG_999_123456789";
        when(testSuiteRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            testGenerationApplicationService.getTaskStatus(taskId);
        });

        verify(testSuiteRepository).findById(999L);
    }

    @Test
    void testValidateTestSuite() {
        // Given
        String taskId = "TG_1_123456789";
        when(testSuiteRepository.findById(1L)).thenReturn(Optional.of(testSuite));
        when(testSuiteRepository.save(testSuite)).thenReturn(testSuite);
        when(assembler.toTestStatusDTO(testSuite, taskId)).thenReturn(testStatusDTO);

        // When
        TestStatusDTO result = testGenerationApplicationService.validateTestSuite(taskId);

        // Then
        assertNotNull(result);
        verify(testSuiteRepository).findById(1L);
        verify(testSuiteRepository).save(testSuite);
    }

    @Test
    void testGetTestResultSuccess() {
        // Given
        String taskId = "TG_1_123456789";
        testSuite.completeGeneration(); // 模拟生成完成

        TestGenerationResultDTO resultDTO = new TestGenerationResultDTO();
        resultDTO.setTaskId(taskId);
        resultDTO.setGeneratedCode("public class UserServiceTest {}");

        when(testSuiteRepository.findById(1L)).thenReturn(Optional.of(testSuite));
        when(assembler.toTestGenerationResultDTO(testSuite, taskId)).thenReturn(resultDTO);

        // When
        TestGenerationResultDTO result = testGenerationApplicationService.getTestResult(taskId);

        // Then
        assertNotNull(result);
        assertEquals(taskId, result.getTaskId());
        verify(testSuiteRepository).findById(1L);
        verify(assembler).toTestGenerationResultDTO(testSuite, taskId);
    }

    @Test
    void testGetTestResultWhenNotReady() {
        // Given
        String taskId = "TG_1_123456789";
        // testSuite 保持 PENDING 状态
        when(testSuiteRepository.findById(1L)).thenReturn(Optional.of(testSuite));

        // When & Then
        assertThrows(BusinessRuleException.class, () -> {
            testGenerationApplicationService.getTestResult(taskId);
        });

        verify(testSuiteRepository).findById(1L);
        verify(assembler, never()).toTestGenerationResultDTO(any(), any());
    }

    @Test
    void testCancelTask() {
        // Given
        String taskId = "TG_1_123456789";
        when(testSuiteRepository.findById(1L)).thenReturn(Optional.of(testSuite));
        when(testSuiteRepository.save(testSuite)).thenReturn(testSuite);
        when(assembler.toTestStatusDTO(testSuite, taskId)).thenReturn(testStatusDTO);

        // When
        TestStatusDTO result = testGenerationApplicationService.cancelTask(taskId);

        // Then
        assertNotNull(result);
        verify(testSuiteRepository).findById(1L);
        verify(testSuiteRepository).save(testSuite);
        verify(assembler).toTestStatusDTO(testSuite, taskId);
    }

    @Test
    void testCancelCompletedTask() {
        // Given
        String taskId = "TG_1_123456789";
        testSuite.completeGeneration(); // 已完成
        when(testSuiteRepository.findById(1L)).thenReturn(Optional.of(testSuite));

        // When & Then
        assertThrows(BusinessRuleException.class, () -> {
            testGenerationApplicationService.cancelTask(taskId);
        });

        verify(testSuiteRepository).findById(1L);
        verify(testSuiteRepository, never()).save(any());
    }

    @Test
    void testRestartTask() {
        // Given
        String taskId = "TG_1_123456789";
        testSuite.markAsFailed("Previous failure"); // 失败状态可以重启

        when(testSuiteRepository.findById(1L)).thenReturn(Optional.of(testSuite));
        when(gitRepositoryRepository.findById(1L)).thenReturn(Optional.of(testRepository));
        when(testSuiteRepository.save(testSuite)).thenReturn(testSuite);
        when(assembler.toTestStatusDTO(testSuite, taskId)).thenReturn(testStatusDTO);

        // When
        TestStatusDTO result = testGenerationApplicationService.restartTask(taskId);

        // Then
        assertNotNull(result);
        verify(testSuiteRepository).findById(1L);
        verify(gitRepositoryRepository).findById(1L);
        verify(testSuiteRepository).save(testSuite);
        verify(assembler).toTestStatusDTO(testSuite, taskId);
    }

    @Test
    void testGetUserTestSuitesWithRepositoryId() {
        // Given
        List<TestSuite> testSuites = Arrays.asList(testSuite);
        List<TestSuiteDTO> testSuiteDTOs = Arrays.asList(new TestSuiteDTO());

        when(testSuiteRepository.findByRepositoryIdAndCreatedBy(1L, "system"))
                .thenReturn(testSuites);
        when(assembler.toTestSuiteDTO(testSuite)).thenReturn(testSuiteDTOs.get(0));

        // When
        List<TestSuiteDTO> result = testGenerationApplicationService.getUserTestSuites(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(testSuiteRepository).findByRepositoryIdAndCreatedBy(1L, "system");
        verify(assembler).toTestSuiteDTO(testSuite);
    }

    @Test
    void testGetUserTestSuitesWithoutRepositoryId() {
        // Given
        List<TestSuite> testSuites = Arrays.asList(testSuite);
        List<TestSuiteDTO> testSuiteDTOs = Arrays.asList(new TestSuiteDTO());

        when(testSuiteRepository.findByCreatedBy("system")).thenReturn(testSuites);
        when(assembler.toTestSuiteDTO(testSuite)).thenReturn(testSuiteDTOs.get(0));

        // When
        List<TestSuiteDTO> result = testGenerationApplicationService.getUserTestSuites(null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(testSuiteRepository).findByCreatedBy("system");
        verify(assembler).toTestSuiteDTO(testSuite);
    }

    @Test
    void testServiceHasCorrectDependencies() {
        // Verify that the service has the correct DDD dependencies
        assertNotNull(testGenerationApplicationService);
    }
}