package com.example.gitreview.domain.shared.model.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Credential值对象
 * 表示访问Git仓库的凭据信息
 */
public class Credential {

    private final String username;
    private final String password;
    private final CredentialType type;

    public enum CredentialType {
        USERNAME_PASSWORD,
        TOKEN,
        SSH_KEY,
        ANONYMOUS
    }

    /**
     * Jackson反序列化构造函数
     */
    @JsonCreator
    public Credential(
            @JsonProperty("username") String username,
            @JsonProperty("password") String password,
            @JsonProperty("type") CredentialType type) {
        this.username = username;
        this.password = password;
        this.type = type != null ? type : determineType(username, password);
    }

    /**
     * 兼容性构造函数（用于数据迁移）
     */
    public Credential(String username, String password) {
        this(username, password, determineType(username, password));
    }

    /**
     * 根据用户名和密码自动判断凭据类型
     */
    private static CredentialType determineType(String username, String password) {
        if ((username == null || username.trim().isEmpty()) &&
            (password == null || password.trim().isEmpty())) {
            return CredentialType.ANONYMOUS;
        }

        if (username != null && !username.trim().isEmpty() &&
            password != null && !password.trim().isEmpty()) {
            return CredentialType.USERNAME_PASSWORD;
        }

        return CredentialType.USERNAME_PASSWORD; // 默认类型
    }

    /**
     * 创建用户名密码凭据
     */
    public static Credential createUsernamePassword(String username, String password) {
        validateUsername(username);
        validatePassword(password);
        return new Credential(username, password, CredentialType.USERNAME_PASSWORD);
    }

    /**
     * 创建Token凭据
     */
    public static Credential createToken(String token) {
        validateToken(token);
        return new Credential(null, token, CredentialType.TOKEN);
    }

    /**
     * 创建SSH密钥凭据
     */
    public static Credential createSshKey(String username, String privateKey) {
        validateUsername(username);
        validateSshKey(privateKey);
        return new Credential(username, privateKey, CredentialType.SSH_KEY);
    }

    /**
     * 创建匿名凭据
     */
    public static Credential createAnonymous() {
        return new Credential(null, null, CredentialType.ANONYMOUS);
    }

    /**
     * 验证凭据是否有效
     */
    public boolean isValid() {
        switch (type) {
            case USERNAME_PASSWORD:
                return username != null && !username.trim().isEmpty() &&
                       password != null && !password.trim().isEmpty();
            case TOKEN:
                return password != null && !password.trim().isEmpty();
            case SSH_KEY:
                return username != null && !username.trim().isEmpty() &&
                       password != null && !password.trim().isEmpty();
            case ANONYMOUS:
                return true;
            default:
                return false;
        }
    }

    /**
     * 检查是否为安全凭据（包含敏感信息）
     */
    public boolean isSecure() {
        return type != CredentialType.ANONYMOUS;
    }

    /**
     * 获取用于显示的用户名（隐藏敏感信息）
     */
    public String getDisplayUsername() {
        if (username == null || username.trim().isEmpty()) {
            return "anonymous";
        }
        return username;
    }

    /**
     * 获取凭据类型描述
     */
    public String getTypeDescription() {
        switch (type) {
            case USERNAME_PASSWORD:
                return "用户名密码";
            case TOKEN:
                return "访问令牌";
            case SSH_KEY:
                return "SSH密钥";
            case ANONYMOUS:
                return "匿名访问";
            default:
                return "未知类型";
        }
    }

    /**
     * 创建副本（用于更新密码等场景）
     */
    public Credential withPassword(String newPassword) {
        switch (type) {
            case USERNAME_PASSWORD:
                return createUsernamePassword(this.username, newPassword);
            case TOKEN:
                return createToken(newPassword);
            case SSH_KEY:
                return createSshKey(this.username, newPassword);
            default:
                throw new IllegalStateException("Cannot update password for credential type: " + type);
        }
    }

    /**
     * 检查密码强度（基本检查）
     */
    public PasswordStrength checkPasswordStrength() {
        if (type == CredentialType.ANONYMOUS || password == null) {
            return PasswordStrength.NOT_APPLICABLE;
        }

        if (password.length() < 8) {
            return PasswordStrength.WEAK;
        }

        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");

        int strengthScore = 0;
        if (hasUpper) strengthScore++;
        if (hasLower) strengthScore++;
        if (hasDigit) strengthScore++;
        if (hasSpecial) strengthScore++;

        if (strengthScore >= 3 && password.length() >= 12) {
            return PasswordStrength.STRONG;
        } else if (strengthScore >= 2) {
            return PasswordStrength.MEDIUM;
        } else {
            return PasswordStrength.WEAK;
        }
    }

    public enum PasswordStrength {
        WEAK, MEDIUM, STRONG, NOT_APPLICABLE
    }

    // 私有验证方法
    private static void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (username.length() > 100) {
            throw new IllegalArgumentException("Username cannot exceed 100 characters");
        }
    }

    private static void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        if (password.length() > 500) {
            throw new IllegalArgumentException("Password cannot exceed 500 characters");
        }
    }

    private static void validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        if (token.length() > 1000) {
            throw new IllegalArgumentException("Token cannot exceed 1000 characters");
        }
    }

    private static void validateSshKey(String sshKey) {
        if (sshKey == null || sshKey.trim().isEmpty()) {
            throw new IllegalArgumentException("SSH key cannot be null or empty");
        }
        if (sshKey.length() > 5000) {
            throw new IllegalArgumentException("SSH key cannot exceed 5000 characters");
        }
    }

    // Getters
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public CredentialType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Credential that = (Credential) o;
        return Objects.equals(username, that.username) &&
               Objects.equals(password, that.password) &&
               type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password, type);
    }

    @Override
    public String toString() {
        return "Credential{" +
                "username='" + getDisplayUsername() + '\'' +
                ", type=" + type +
                ", hasPassword=" + (password != null && !password.isEmpty()) +
                '}';
    }
}