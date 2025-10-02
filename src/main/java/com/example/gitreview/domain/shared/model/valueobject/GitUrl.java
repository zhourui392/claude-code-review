package com.example.gitreview.domain.shared.model.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * GitUrl值对象
 * 表示Git仓库URL的领域概念，包含URL验证和解析逻辑
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitUrl {

    private static final Pattern GIT_URL_PATTERN = Pattern.compile(
        "^(https?://|git://|ssh://|git@)[\\w\\.-]+[:/]([\\w\\.-]+/)*[\\w\\.-]+(\\.git)?/?$"
    );

    private final String url;
    private final GitUrlType type;
    private final String hostname;
    private final String repositoryName;
    private final String organizationName;

    public enum GitUrlType {
        HTTPS,
        SSH,
        GIT,
        UNKNOWN
    }

    @JsonCreator
    public GitUrl(@JsonProperty("url") String url) {
        this.url = validateAndNormalizeUrl(url);
        this.type = determineUrlType(this.url);
        this.hostname = extractHostname(this.url);
        this.repositoryName = extractRepositoryName(this.url);
        this.organizationName = extractOrganizationName(this.url);
    }

    /**
     * 验证并标准化URL
     */
    private String validateAndNormalizeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("Git URL cannot be null or empty");
        }

        String trimmedUrl = url.trim();

        // 基本格式验证
        if (!GIT_URL_PATTERN.matcher(trimmedUrl).matches()) {
            throw new IllegalArgumentException("Invalid Git URL format: " + url);
        }

        // 标准化URL（确保以.git结尾，除非是SSH格式）
        if (!trimmedUrl.startsWith("git@") && !trimmedUrl.endsWith(".git") && !trimmedUrl.endsWith("/")) {
            trimmedUrl = trimmedUrl + ".git";
        }

        return trimmedUrl;
    }

    /**
     * 确定URL类型
     */
    private GitUrlType determineUrlType(String url) {
        if (url.startsWith("https://") || url.startsWith("http://")) {
            return GitUrlType.HTTPS;
        } else if (url.startsWith("git@") || url.startsWith("ssh://")) {
            return GitUrlType.SSH;
        } else if (url.startsWith("git://")) {
            return GitUrlType.GIT;
        } else {
            return GitUrlType.UNKNOWN;
        }
    }

    /**
     * 提取主机名
     */
    private String extractHostname(String url) {
        try {
            if (url.startsWith("git@")) {
                // SSH格式: git@hostname:org/repo.git
                int atIndex = url.indexOf('@');
                int colonIndex = url.indexOf(':', atIndex);
                if (atIndex != -1 && colonIndex != -1) {
                    return url.substring(atIndex + 1, colonIndex);
                }
            } else {
                URI uri = new URI(url);
                return uri.getHost();
            }
        } catch (URISyntaxException e) {
            // 解析失败时从URL中提取
            return extractHostnameFromString(url);
        }
        return "unknown";
    }

    /**
     * 从字符串中提取主机名
     */
    private String extractHostnameFromString(String url) {
        // 简单的字符串解析
        String cleaned = url.replaceFirst("^(https?://|git://|ssh://)", "");
        if (cleaned.startsWith("git@")) {
            cleaned = cleaned.substring(4);
        }

        int colonIndex = cleaned.indexOf(':');
        int slashIndex = cleaned.indexOf('/');
        int endIndex = Math.min(
            colonIndex == -1 ? Integer.MAX_VALUE : colonIndex,
            slashIndex == -1 ? Integer.MAX_VALUE : slashIndex
        );

        if (endIndex == Integer.MAX_VALUE) {
            return cleaned;
        }

        return cleaned.substring(0, endIndex);
    }

    /**
     * 提取仓库名称
     */
    private String extractRepositoryName(String url) {
        String path = extractPath(url);
        if (path.isEmpty()) {
            return "unknown";
        }

        // 移除.git后缀
        if (path.endsWith(".git")) {
            path = path.substring(0, path.length() - 4);
        }

        // 获取最后一个路径段
        String[] segments = path.split("/");
        return segments[segments.length - 1];
    }

    /**
     * 提取组织名称
     */
    private String extractOrganizationName(String url) {
        String path = extractPath(url);
        if (path.isEmpty()) {
            return "unknown";
        }

        String[] segments = path.split("/");
        if (segments.length >= 2) {
            return segments[segments.length - 2];
        }
        return "unknown";
    }

    /**
     * 提取URL路径部分
     */
    private String extractPath(String url) {
        try {
            if (url.startsWith("git@")) {
                // SSH格式: git@hostname:org/repo.git
                int colonIndex = url.indexOf(':');
                if (colonIndex != -1) {
                    return url.substring(colonIndex + 1);
                }
            } else {
                URI uri = new URI(url);
                String path = uri.getPath();
                if (path != null && path.startsWith("/")) {
                    return path.substring(1);
                }
                return path != null ? path : "";
            }
        } catch (URISyntaxException e) {
            // 解析失败时返回空路径
            return "";
        }
        return "";
    }

    /**
     * 验证URL是否有效
     */
    public boolean isValid() {
        return url != null && !url.trim().isEmpty() &&
               type != GitUrlType.UNKNOWN &&
               !hostname.equals("unknown");
    }

    /**
     * 检查是否为GitHub仓库
     */
    public boolean isGitHub() {
        return hostname.toLowerCase().contains("github.com");
    }

    /**
     * 检查是否为GitLab仓库
     */
    public boolean isGitLab() {
        return hostname.toLowerCase().contains("gitlab");
    }

    /**
     * 检查是否为Bitbucket仓库
     */
    public boolean isBitbucket() {
        return hostname.toLowerCase().contains("bitbucket");
    }

    /**
     * 获取Web界面URL（如果是HTTPS类型）
     */
    public String getWebUrl() {
        if (type == GitUrlType.HTTPS) {
            return url.endsWith(".git") ? url.substring(0, url.length() - 4) : url;
        }

        if (type == GitUrlType.SSH && (isGitHub() || isGitLab() || isBitbucket())) {
            // 转换SSH URL为HTTPS URL
            return "https://" + hostname + "/" + organizationName + "/" + repositoryName;
        }

        return null;
    }

    /**
     * 获取克隆用的URL
     */
    public String getCloneUrl() {
        return url;
    }

    /**
     * 获取仓库的完整名称（组织/仓库）
     */
    public String getFullName() {
        if ("unknown".equals(organizationName) || "unknown".equals(repositoryName)) {
            return repositoryName;
        }
        return organizationName + "/" + repositoryName;
    }

    /**
     * 转换为HTTPS URL（如果可能）
     */
    public GitUrl toHttpsUrl() {
        if (type == GitUrlType.HTTPS) {
            return this;
        }

        if (type == GitUrlType.SSH && !hostname.equals("unknown")) {
            String httpsUrl = "https://" + hostname + "/" + organizationName + "/" + repositoryName + ".git";
            return new GitUrl(httpsUrl);
        }

        throw new IllegalStateException("Cannot convert URL to HTTPS: " + url);
    }

    /**
     * 转换为SSH URL（如果可能）
     */
    public GitUrl toSshUrl() {
        if (type == GitUrlType.SSH) {
            return this;
        }

        if (type == GitUrlType.HTTPS && !hostname.equals("unknown")) {
            String sshUrl = "git@" + hostname + ":" + organizationName + "/" + repositoryName + ".git";
            return new GitUrl(sshUrl);
        }

        throw new IllegalStateException("Cannot convert URL to SSH: " + url);
    }

    // Getters
    public String getUrl() {
        return url;
    }

    public GitUrlType getType() {
        return type;
    }

    public String getHostname() {
        return hostname;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GitUrl gitUrl = (GitUrl) o;
        return Objects.equals(url, gitUrl.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    @Override
    public String toString() {
        return "GitUrl{" +
                "url='" + url + '\'' +
                ", type=" + type +
                ", hostname='" + hostname + '\'' +
                ", fullName='" + getFullName() + '\'' +
                '}';
    }
}