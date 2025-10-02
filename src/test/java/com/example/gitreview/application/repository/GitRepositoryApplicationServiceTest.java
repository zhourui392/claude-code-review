package com.example.gitreview.application.repository;

import com.example.gitreview.application.repository.assembler.GitRepositoryAssembler;
import com.example.gitreview.application.repository.dto.GitRepositoryCreateDTO;
import com.example.gitreview.application.repository.dto.GitRepositoryDTO;
import com.example.gitreview.domain.shared.model.aggregate.Repository;
import com.example.gitreview.domain.shared.model.valueobject.GitUrl;
import com.example.gitreview.domain.shared.model.valueobject.Credential;
import com.example.gitreview.domain.shared.repository.GitRepositoryRepository;
import com.example.gitreview.domain.shared.service.RepositoryDomainService;
import com.example.gitreview.domain.shared.exception.ResourceNotFoundException;
import com.example.gitreview.domain.shared.exception.ValidationException;
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
 * GitRepositoryApplicationService Test
 * Test application service layer business logic
 */
@ExtendWith(MockitoExtension.class)
public class GitRepositoryApplicationServiceTest {

    @Mock
    private GitRepositoryRepository repositoryRepository;

    @Mock
    private RepositoryDomainService repositoryDomainService;

    @Mock
    private GitRepositoryAssembler assembler;

    @InjectMocks
    private GitRepositoryApplicationService applicationService;

    private Repository testRepository;
    private GitRepositoryCreateDTO createDTO;
    private GitRepositoryDTO repositoryDTO;

    @BeforeEach
    void setUp() {
        // Setup test data
        GitUrl gitUrl = new GitUrl("https://github.com/test/repo.git");
        Credential credential = new Credential("testuser", "testpass");
        testRepository = new Repository("Test Repo", "Test Description", gitUrl, credential);
        testRepository.setId(1L);

        createDTO = new GitRepositoryCreateDTO();
        createDTO.setName("Test Repo");
        createDTO.setUrl("https://github.com/test/repo.git");
        createDTO.setDescription("Test Description");
        createDTO.setUsername("testuser");
        createDTO.setPassword("testpass");

        repositoryDTO = new GitRepositoryDTO();
        repositoryDTO.setId(1L);
        repositoryDTO.setName("Test Repo");
        repositoryDTO.setUrl("https://github.com/test/repo.git");
        repositoryDTO.setDescription("Test Description");
    }

    @Test
    void testCreateRepository() {
        // Given
        when(repositoryRepository.existsByName("Test Repo")).thenReturn(false);
        when(assembler.fromCreateDTO(createDTO)).thenReturn(testRepository);
        when(repositoryRepository.save(any(Repository.class))).thenReturn(testRepository);
        when(assembler.toGitRepositoryDTO(testRepository)).thenReturn(repositoryDTO);

        // When
        GitRepositoryDTO result = applicationService.createRepository(createDTO);

        // Then
        assertNotNull(result);
        assertEquals("Test Repo", result.getName());
        verify(repositoryRepository).existsByName("Test Repo");
        verify(assembler).fromCreateDTO(createDTO);
        verify(repositoryRepository).save(any(Repository.class));
        verify(assembler).toGitRepositoryDTO(testRepository);
    }

    @Test
    void testCreateRepositoryWithDuplicateName() {
        // Given
        when(repositoryRepository.existsByName("Test Repo")).thenReturn(true);

        // When & Then
        assertThrows(ValidationException.class, () -> {
            applicationService.createRepository(createDTO);
        });

        verify(repositoryRepository).existsByName("Test Repo");
        verify(assembler, never()).fromCreateDTO(any());
        verify(repositoryRepository, never()).save(any());
    }

    @Test
    void testGetRepositoryById() {
        // Given
        when(repositoryRepository.findById(1L)).thenReturn(Optional.of(testRepository));
        when(assembler.toGitRepositoryDTO(testRepository)).thenReturn(repositoryDTO);

        // When
        GitRepositoryDTO result = applicationService.getRepository(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Repo", result.getName());
        verify(repositoryRepository).findById(1L);
        verify(assembler).toGitRepositoryDTO(testRepository);
    }

    @Test
    void testGetRepositoryByIdNotFound() {
        // Given
        when(repositoryRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            applicationService.getRepository(999L);
        });

        verify(repositoryRepository).findById(999L);
        verify(assembler, never()).toGitRepositoryDTO(any());
    }

    @Test
    void testGetAllRepositories() {
        // Given
        List<Repository> repositories = Arrays.asList(testRepository);
        List<GitRepositoryDTO> repositoryDTOs = Arrays.asList(repositoryDTO);

        when(repositoryRepository.findAll()).thenReturn(repositories);
        when(assembler.toGitRepositoryDTO(testRepository)).thenReturn(repositoryDTO);

        // When
        List<GitRepositoryDTO> result = applicationService.getAllRepositories();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Repo", result.get(0).getName());
        verify(repositoryRepository).findAll();
        verify(assembler).toGitRepositoryDTO(testRepository);
    }

    @Test
    void testDeleteRepository() {
        // Given
        when(repositoryRepository.findById(1L)).thenReturn(Optional.of(testRepository));
        doNothing().when(repositoryDomainService).validateCanDelete(testRepository);
        when(repositoryRepository.deleteById(1L)).thenReturn(true);

        // When
        applicationService.deleteRepository(1L);

        // Then
        verify(repositoryRepository).findById(1L);
        verify(repositoryRepository).deleteById(1L);
    }

    @Test
    void testDeleteRepositoryNotFound() {
        // Given
        when(repositoryRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            applicationService.deleteRepository(999L);
        });

        verify(repositoryRepository).findById(999L);
        verify(repositoryRepository, never()).deleteById(any());
    }

    @Test
    void testCheckRepositoryStatus() {
        // Given
        when(repositoryRepository.findById(1L)).thenReturn(Optional.of(testRepository));
        when(repositoryDomainService.isRepositoryAccessible(testRepository)).thenReturn(true);
        when(repositoryRepository.save(testRepository)).thenReturn(testRepository);
        when(assembler.toGitRepositoryDTO(testRepository)).thenReturn(repositoryDTO);

        // When
        GitRepositoryDTO result = applicationService.checkRepositoryStatus(1L);

        // Then
        assertNotNull(result);
        verify(repositoryRepository).findById(1L);
        verify(repositoryDomainService).isRepositoryAccessible(testRepository);
        verify(repositoryRepository).save(testRepository);
        verify(assembler).toGitRepositoryDTO(testRepository);
    }
}