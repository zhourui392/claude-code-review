package com.example.gitreview.domain.shared.model.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Credential Value Object Test
 * Test credential validation and behavior
 */
public class CredentialTest {

    @Test
    void testUsernamePasswordCredential() {
        // Given
        String username = "testuser";
        String password = "testpass";

        // When
        Credential credential = new Credential(username, password);

        // Then
        assertEquals(username, credential.getUsername());
        assertEquals(password, credential.getPassword());
        assertEquals(Credential.CredentialType.USERNAME_PASSWORD, credential.getType());
        assertTrue(credential.isValid());
        assertTrue(credential.isSecure());
        assertEquals("testuser", credential.getDisplayUsername());
    }

    @Test
    void testCreateUsernamePassword() {
        // Given
        String username = "user";
        String password = "pass";

        // When
        Credential credential = Credential.createUsernamePassword(username, password);

        // Then
        assertEquals(username, credential.getUsername());
        assertEquals(password, credential.getPassword());
        assertEquals(Credential.CredentialType.USERNAME_PASSWORD, credential.getType());
        assertTrue(credential.isValid());
    }

    @Test
    void testCreateToken() {
        // Given
        String token = "ghp_1234567890abcdef";

        // When
        Credential credential = Credential.createToken(token);

        // Then
        assertNull(credential.getUsername());
        assertEquals(token, credential.getPassword());
        assertEquals(Credential.CredentialType.TOKEN, credential.getType());
        assertTrue(credential.isValid());
        assertEquals("anonymous", credential.getDisplayUsername());
    }

    @Test
    void testCreateSshKey() {
        // Given
        String username = "git";
        String privateKey = "-----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEA...";

        // When
        Credential credential = Credential.createSshKey(username, privateKey);

        // Then
        assertEquals(username, credential.getUsername());
        assertEquals(privateKey, credential.getPassword());
        assertEquals(Credential.CredentialType.SSH_KEY, credential.getType());
        assertTrue(credential.isValid());
    }

    @Test
    void testCreateAnonymous() {
        // When
        Credential credential = Credential.createAnonymous();

        // Then
        assertNull(credential.getUsername());
        assertNull(credential.getPassword());
        assertEquals(Credential.CredentialType.ANONYMOUS, credential.getType());
        assertTrue(credential.isValid());
        assertFalse(credential.isSecure());
        assertEquals("anonymous", credential.getDisplayUsername());
    }

    @Test
    void testInvalidCredentials() {
        // Test empty username
        assertThrows(IllegalArgumentException.class, () -> {
            Credential.createUsernamePassword("", "password");
        });

        // Test null password
        assertThrows(IllegalArgumentException.class, () -> {
            Credential.createUsernamePassword("user", null);
        });

        // Test empty token
        assertThrows(IllegalArgumentException.class, () -> {
            Credential.createToken("");
        });

        // Test invalid SSH key
        assertThrows(IllegalArgumentException.class, () -> {
            Credential.createSshKey("user", "");
        });
    }

    @Test
    void testWithPassword() {
        // Given
        Credential original = Credential.createUsernamePassword("user", "oldpass");

        // When
        Credential updated = original.withPassword("newpass");

        // Then
        assertEquals("user", updated.getUsername());
        assertEquals("newpass", updated.getPassword());
        assertEquals(Credential.CredentialType.USERNAME_PASSWORD, updated.getType());

        // Original should be unchanged
        assertEquals("oldpass", original.getPassword());
    }

    @Test
    void testPasswordStrength() {
        // Test weak password
        Credential weak = Credential.createUsernamePassword("user", "123");
        assertEquals(Credential.PasswordStrength.WEAK, weak.checkPasswordStrength());

        // Test medium password
        Credential medium = Credential.createUsernamePassword("user", "Password1");
        assertEquals(Credential.PasswordStrength.MEDIUM, medium.checkPasswordStrength());

        // Test strong password
        Credential strong = Credential.createUsernamePassword("user", "StrongP@ssw0rd123!");
        assertEquals(Credential.PasswordStrength.STRONG, strong.checkPasswordStrength());

        // Test anonymous (not applicable)
        Credential anonymous = Credential.createAnonymous();
        assertEquals(Credential.PasswordStrength.NOT_APPLICABLE, anonymous.checkPasswordStrength());
    }

    @Test
    void testGetTypeDescription() {
        // Test different types
        assertEquals("用户名密码", Credential.createUsernamePassword("u", "p").getTypeDescription());
        assertEquals("访问令牌", Credential.createToken("token").getTypeDescription());
        assertEquals("SSH密钥", Credential.createSshKey("user", "key").getTypeDescription());
        assertEquals("匿名访问", Credential.createAnonymous().getTypeDescription());
    }

    @Test
    void testAutoTypeDetection() {
        // Test auto-detection with username and password
        Credential userPass = new Credential("user", "pass");
        assertEquals(Credential.CredentialType.USERNAME_PASSWORD, userPass.getType());

        // Test auto-detection with empty values
        Credential empty = new Credential("", "");
        assertEquals(Credential.CredentialType.ANONYMOUS, empty.getType());

        // Test auto-detection with null values
        Credential nullValues = new Credential(null, null);
        assertEquals(Credential.CredentialType.ANONYMOUS, nullValues.getType());
    }

    @Test
    void testEqualsAndHashCode() {
        // Given
        Credential cred1 = Credential.createUsernamePassword("user", "pass");
        Credential cred2 = Credential.createUsernamePassword("user", "pass");
        Credential cred3 = Credential.createUsernamePassword("other", "pass");

        // When & Then
        assertEquals(cred1, cred2);
        assertEquals(cred1.hashCode(), cred2.hashCode());
        assertNotEquals(cred1, cred3);
        assertNotEquals(cred1.hashCode(), cred3.hashCode());
    }

    @Test
    void testToString() {
        // Given
        Credential credential = Credential.createUsernamePassword("testuser", "secret");

        // When
        String toString = credential.toString();

        // Then
        assertTrue(toString.contains("testuser"));
        assertTrue(toString.contains("USERNAME_PASSWORD"));
        assertTrue(toString.contains("hasPassword=true"));
        assertFalse(toString.contains("secret")); // Password should not be exposed
    }
}