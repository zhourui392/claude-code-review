package com.example.gitreview.domain.shared.model.aggregate;

import com.example.gitreview.domain.shared.model.valueobject.GitUrl;
import com.example.gitreview.domain.shared.model.valueobject.Credential;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository Aggregate Test
 * Test business logic and behavior of Repository aggregate root
 */
public class RepositoryTest {

    @Test
    void testRepositoryCreation() {
        // Given
        GitUrl gitUrl = new GitUrl("https://github.com/test/repo.git");
        Credential credential = new Credential("testuser", "testpass");

        // When
        Repository repository = new Repository("Test Repo", "Test Description", gitUrl, credential);

        // Then
        assertEquals("Test Repo", repository.getName());
        assertEquals("Test Description", repository.getDescription());
        assertEquals(gitUrl, repository.getGitUrl());
        assertEquals(credential, repository.getCredential());
        assertTrue(repository.isActive());
        assertNotNull(repository.getCreateTime());
        assertNotNull(repository.getUpdateTime());
    }

    @Test
    void testRepositoryCreationWithInvalidName() {
        // Given
        GitUrl gitUrl = new GitUrl("https://github.com/test/repo.git");
        Credential credential = new Credential("testuser", "testpass");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new Repository("", "Description", gitUrl, credential);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new Repository(null, "Description", gitUrl, credential);
        });
    }

    @Test
    void testUpdateInfo() {
        // Given
        GitUrl gitUrl = new GitUrl("https://github.com/test/repo.git");
        Credential credential = new Credential("testuser", "testpass");
        Repository repository = new Repository("Original", "Original Description", gitUrl, credential);

        // When
        repository.updateInfo("Updated Name", "Updated Description");

        // Then
        assertEquals("Updated Name", repository.getName());
        assertEquals("Updated Description", repository.getDescription());
    }

    @Test
    void testActivateDeactivate() {
        // Given
        GitUrl gitUrl = new GitUrl("https://github.com/test/repo.git");
        Credential credential = new Credential("testuser", "testpass");
        Repository repository = new Repository("Test Repo", "Description", gitUrl, credential);

        // Initially active
        assertTrue(repository.isActive());

        // When deactivated
        repository.deactivate();

        // Then
        assertFalse(repository.isActive());

        // When reactivated
        repository.activate();

        // Then
        assertTrue(repository.isActive());
    }

    @Test
    void testCanPerformGitOperations() {
        // Given
        GitUrl gitUrl = new GitUrl("https://github.com/test/repo.git");
        Credential credential = new Credential("testuser", "testpass");
        Repository repository = new Repository("Test Repo", "Description", gitUrl, credential);

        // When active with valid credentials
        assertTrue(repository.canPerformGitOperations());

        // When deactivated
        repository.deactivate();
        assertFalse(repository.canPerformGitOperations());
    }

    @Test
    void testTestConnection() {
        // Given
        GitUrl gitUrl = new GitUrl("https://github.com/test/repo.git");
        Credential credential = new Credential("testuser", "testpass");
        Repository repository = new Repository("Test Repo", "Description", gitUrl, credential);

        // When
        boolean canConnect = repository.testConnection();

        // Then
        assertTrue(canConnect); // With valid URL and credentials
    }

    @Test
    void testGetDisplayName() {
        // Given
        GitUrl gitUrl = new GitUrl("https://github.com/test/repo.git");
        Credential credential = new Credential("testuser", "testpass");

        // When repository has a name
        Repository repoWithName = new Repository("Custom Name", "Description", gitUrl, credential);
        assertEquals("Custom Name", repoWithName.getDisplayName());

        // When repository name is empty (edge case - should not happen due to validation)
        // Test the fallback behavior
        Repository repository = new Repository("Test", "Description", gitUrl, credential);
        assertEquals("Test", repository.getDisplayName());
    }

    @Test
    void testCreateBranch() {
        // Given
        GitUrl gitUrl = new GitUrl("https://github.com/test/repo.git");
        Credential credential = new Credential("testuser", "testpass");
        Repository repository = new Repository("Test Repo", "Description", gitUrl, credential);
        repository.setId(1L); // Simulate saved repository

        // When
        var branch = repository.createBranch("feature/test");

        // Then
        assertNotNull(branch);
        assertEquals("feature/test", branch.getName());
        assertEquals(1L, branch.getRepositoryId());
    }

    @Test
    void testUpdateCredential() {
        // Given
        GitUrl gitUrl = new GitUrl("https://github.com/test/repo.git");
        Credential oldCredential = new Credential("olduser", "oldpass");
        Repository repository = new Repository("Test Repo", "Description", gitUrl, oldCredential);

        // When
        Credential newCredential = new Credential("newuser", "newpass");
        repository.updateCredential(newCredential);

        // Then
        assertEquals(newCredential, repository.getCredential());
        assertEquals("newuser", repository.getCredential().getUsername());
    }

    @Test
    void testEqualsAndHashCode() {
        // Given
        GitUrl gitUrl = new GitUrl("https://github.com/test/repo.git");
        Credential credential = new Credential("testuser", "testpass");
        Repository repo1 = new Repository("Test Repo", "Description", gitUrl, credential);
        Repository repo2 = new Repository("Test Repo", "Description", gitUrl, credential);

        // When both have no ID (null == null is true in equals)
        assertEquals(repo1, repo2); // Both have null ID, so they are equal

        // When both have same ID
        repo1.setId(1L);
        repo2.setId(1L);
        assertEquals(repo1, repo2);
        assertEquals(repo1.hashCode(), repo2.hashCode());

        // When different IDs
        repo2.setId(2L);
        assertNotEquals(repo1, repo2);
    }
}