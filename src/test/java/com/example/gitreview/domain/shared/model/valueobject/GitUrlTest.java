package com.example.gitreview.domain.shared.model.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GitUrl Value Object Test
 * Test URL validation and parsing logic
 */
public class GitUrlTest {

    @Test
    void testValidHttpsUrl() {
        // Given
        String url = "https://github.com/user/repo.git";

        // When
        GitUrl gitUrl = new GitUrl(url);

        // Then
        assertEquals(url, gitUrl.getUrl());
        assertEquals(GitUrl.GitUrlType.HTTPS, gitUrl.getType());
        assertEquals("github.com", gitUrl.getHostname());
        assertEquals("repo", gitUrl.getRepositoryName());
        assertEquals("user", gitUrl.getOrganizationName());
        assertTrue(gitUrl.isValid());
        assertTrue(gitUrl.isGitHub());
    }

    @Test
    void testValidSshUrl() {
        // Given
        String url = "git@github.com:user/repo.git";

        // When
        GitUrl gitUrl = new GitUrl(url);

        // Then
        assertEquals(url, gitUrl.getUrl());
        assertEquals(GitUrl.GitUrlType.SSH, gitUrl.getType());
        assertEquals("github.com", gitUrl.getHostname());
        assertEquals("repo", gitUrl.getRepositoryName());
        assertEquals("user", gitUrl.getOrganizationName());
        assertTrue(gitUrl.isValid());
        assertTrue(gitUrl.isGitHub());
    }

    @Test
    void testInvalidUrl() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            new GitUrl("");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new GitUrl(null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new GitUrl("invalid-url");
        });
    }

    @Test
    void testGitLabUrl() {
        // Given
        String url = "https://gitlab.com/user/repo.git";

        // When
        GitUrl gitUrl = new GitUrl(url);

        // Then
        assertTrue(gitUrl.isGitLab());
        assertFalse(gitUrl.isGitHub());
        assertFalse(gitUrl.isBitbucket());
    }

    @Test
    void testBitbucketUrl() {
        // Given
        String url = "https://bitbucket.org/user/repo.git";

        // When
        GitUrl gitUrl = new GitUrl(url);

        // Then
        assertTrue(gitUrl.isBitbucket());
        assertFalse(gitUrl.isGitHub());
        assertFalse(gitUrl.isGitLab());
    }

    @Test
    void testGetWebUrl() {
        // Given HTTPS URL
        GitUrl httpsUrl = new GitUrl("https://github.com/user/repo.git");

        // When
        String webUrl = httpsUrl.getWebUrl();

        // Then
        assertEquals("https://github.com/user/repo", webUrl);

        // Given SSH URL
        GitUrl sshUrl = new GitUrl("git@github.com:user/repo.git");

        // When
        String sshWebUrl = sshUrl.getWebUrl();

        // Then
        assertEquals("https://github.com/user/repo", sshWebUrl);
    }

    @Test
    void testGetFullName() {
        // Given
        GitUrl gitUrl = new GitUrl("https://github.com/myorg/myrepo.git");

        // When
        String fullName = gitUrl.getFullName();

        // Then
        assertEquals("myorg/myrepo", fullName);
    }

    @Test
    void testToHttpsUrl() {
        // Given SSH URL
        GitUrl sshUrl = new GitUrl("git@github.com:user/repo.git");

        // When
        GitUrl httpsUrl = sshUrl.toHttpsUrl();

        // Then
        assertEquals("https://github.com/user/repo.git", httpsUrl.getUrl());
        assertEquals(GitUrl.GitUrlType.HTTPS, httpsUrl.getType());

        // Test idempotent conversion
        GitUrl alreadyHttps = new GitUrl("https://github.com/user/repo.git");
        GitUrl convertedHttps = alreadyHttps.toHttpsUrl();
        assertEquals(alreadyHttps.getUrl(), convertedHttps.getUrl());
    }

    @Test
    void testToSshUrl() {
        // Given HTTPS URL
        GitUrl httpsUrl = new GitUrl("https://github.com/user/repo.git");

        // When
        GitUrl sshUrl = httpsUrl.toSshUrl();

        // Then
        assertEquals("git@github.com:user/repo.git", sshUrl.getUrl());
        assertEquals(GitUrl.GitUrlType.SSH, sshUrl.getType());

        // Test idempotent conversion
        GitUrl alreadySsh = new GitUrl("git@github.com:user/repo.git");
        GitUrl convertedSsh = alreadySsh.toSshUrl();
        assertEquals(alreadySsh.getUrl(), convertedSsh.getUrl());
    }

    @Test
    void testUrlNormalization() {
        // Test URL without .git extension
        GitUrl url1 = new GitUrl("https://github.com/user/repo");
        assertEquals("https://github.com/user/repo.git", url1.getUrl());

        // Test URL with .git extension
        GitUrl url2 = new GitUrl("https://github.com/user/repo.git");
        assertEquals("https://github.com/user/repo.git", url2.getUrl());
    }

    @Test
    void testEqualsAndHashCode() {
        // Given
        GitUrl url1 = new GitUrl("https://github.com/user/repo.git");
        GitUrl url2 = new GitUrl("https://github.com/user/repo.git");
        GitUrl url3 = new GitUrl("https://github.com/user/other.git");

        // When & Then
        assertEquals(url1, url2);
        assertEquals(url1.hashCode(), url2.hashCode());
        assertNotEquals(url1, url3);
        assertNotEquals(url1.hashCode(), url3.hashCode());
    }

    @Test
    void testToString() {
        // Given
        GitUrl gitUrl = new GitUrl("https://github.com/user/repo.git");

        // When
        String toString = gitUrl.toString();

        // Then
        assertTrue(toString.contains("https://github.com/user/repo.git"));
        assertTrue(toString.contains("HTTPS"));
        assertTrue(toString.contains("github.com"));
        assertTrue(toString.contains("user/repo"));
    }
}