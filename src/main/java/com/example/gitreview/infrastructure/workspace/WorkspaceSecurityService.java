package com.example.gitreview.infrastructure.workspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 工作空间安全服务
 * 提供工作空间的安全检查和隔离机制
 */
@Service
public class WorkspaceSecurityService {

    private static final Logger logger = LoggerFactory.getLogger(WorkspaceSecurityService.class);

    // 危险路径模式
    private static final List<String> DANGEROUS_PATHS = Arrays.asList(
        "..", "../", "..\\", "/", "\\", "C:", "D:", "E:", "F:", "G:", "H:",
        "/etc", "/usr", "/var", "/sys", "/proc", "/dev", "/boot",
        "C:\\Windows", "C:\\Program Files", "C:\\Users"
    );

    // 危险文件名模式
    private static final Pattern DANGEROUS_FILENAME = Pattern.compile(
        ".*\\.(exe|bat|cmd|com|scr|pif|vbs|js|jar|dll|sys)$",
        Pattern.CASE_INSENSITIVE
    );

    // 允许的文件扩展名
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        ".java", ".txt", ".md", ".json", ".xml", ".yml", ".yaml", ".properties",
        ".log", ".diff", ".patch", ".gitignore", ".gitattributes"
    );

    /**
     * 验证路径安全性
     * @param path 路径
     * @return 是否安全
     */
    public boolean isPathSafe(String path) {
        if (path == null || path.trim().isEmpty()) {
            logger.warn("Empty or null path provided");
            return false;
        }

        String normalizedPath = path.trim().toLowerCase();

        // 检查危险路径
        for (String dangerousPath : DANGEROUS_PATHS) {
            if (normalizedPath.contains(dangerousPath.toLowerCase())) {
                logger.warn("Dangerous path detected: {} contains {}", path, dangerousPath);
                return false;
            }
        }

        // 检查路径遍历攻击
        if (normalizedPath.contains("..")) {
            logger.warn("Path traversal attempt detected: {}", path);
            return false;
        }

        return true;
    }

    /**
     * 验证文件名安全性
     * @param fileName 文件名
     * @return 是否安全
     */
    public boolean isFileNameSafe(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            logger.warn("Empty or null filename provided");
            return false;
        }

        String normalizedFileName = fileName.trim();

        // 检查危险文件名模式
        if (DANGEROUS_FILENAME.matcher(normalizedFileName).matches()) {
            logger.warn("Dangerous filename detected: {}", fileName);
            return false;
        }

        // 检查控制字符
        for (char c : normalizedFileName.toCharArray()) {
            if (Character.isISOControl(c)) {
                logger.warn("Control character detected in filename: {}", fileName);
                return false;
            }
        }

        return true;
    }

    /**
     * 验证文件扩展名是否允许
     * @param fileName 文件名
     * @return 是否允许
     */
    public boolean isFileExtensionAllowed(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }

        String lowerFileName = fileName.toLowerCase();
        return ALLOWED_EXTENSIONS.stream()
                .anyMatch(lowerFileName::endsWith);
    }

    /**
     * 验证工作空间路径是否在允许的范围内
     * @param workspacePath 工作空间路径
     * @param allowedBasePath 允许的基础路径
     * @return 是否在允许范围内
     */
    public boolean isWorkspacePathAllowed(Path workspacePath, Path allowedBasePath) {
        try {
            Path normalizedWorkspacePath = workspacePath.normalize().toAbsolutePath();
            Path normalizedBasePath = allowedBasePath.normalize().toAbsolutePath();

            boolean allowed = normalizedWorkspacePath.startsWith(normalizedBasePath);
            if (!allowed) {
                logger.warn("Workspace path {} is outside allowed base path {}",
                           normalizedWorkspacePath, normalizedBasePath);
            }
            return allowed;
        } catch (Exception e) {
            logger.error("Error validating workspace path: {}", workspacePath, e);
            return false;
        }
    }

    /**
     * 清理文件名，移除危险字符
     * @param fileName 原始文件名
     * @return 清理后的文件名
     */
    public String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "sanitized_file";
        }

        String sanitized = fileName.trim()
                .replaceAll("[<>:\"/\\\\|?*]", "_")  // Windows危险字符
                .replaceAll("\\.\\.+", ".")          // 多个连续点
                .replaceAll("^\\.", "")              // 开头的点
                .replaceAll("\\.$", "");             // 结尾的点

        // 确保不为空
        if (sanitized.isEmpty()) {
            sanitized = "sanitized_file";
        }

        // 限制长度
        if (sanitized.length() > 255) {
            sanitized = sanitized.substring(0, 255);
        }

        logger.debug("Sanitized filename: {} -> {}", fileName, sanitized);
        return sanitized;
    }

    /**
     * 验证工作空间大小是否在限制内
     * @param workspaceDir 工作空间目录
     * @param maxSizeBytes 最大大小（字节）
     * @return 是否在限制内
     */
    public boolean isWorkspaceSizeAllowed(File workspaceDir, long maxSizeBytes) {
        try {
            long totalSize = calculateDirectorySize(workspaceDir);
            boolean allowed = totalSize <= maxSizeBytes;

            if (!allowed) {
                logger.warn("Workspace size {} bytes exceeds limit {} bytes",
                           totalSize, maxSizeBytes);
            }

            return allowed;
        } catch (Exception e) {
            logger.error("Error calculating workspace size: {}", workspaceDir, e);
            return false;
        }
    }

    /**
     * 计算目录大小
     * @param directory 目录
     * @return 目录大小（字节）
     */
    private long calculateDirectorySize(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return 0;
        }

        long size = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    size += calculateDirectorySize(file);
                } else {
                    size += file.length();
                }
            }
        }
        return size;
    }

    /**
     * 验证完整的工作空间安全性
     * @param workspacePath 工作空间路径
     * @param allowedBasePath 允许的基础路径
     * @param maxSizeBytes 最大大小限制
     * @return 安全检查结果
     */
    public SecurityCheckResult validateWorkspaceSecurity(Path workspacePath,
                                                        Path allowedBasePath,
                                                        long maxSizeBytes) {
        SecurityCheckResult result = new SecurityCheckResult();

        // 检查路径安全性
        result.pathSafe = isPathSafe(workspacePath.toString());

        // 检查路径是否在允许范围内
        result.pathAllowed = isWorkspacePathAllowed(workspacePath, allowedBasePath);

        // 检查工作空间大小
        result.sizeAllowed = isWorkspaceSizeAllowed(workspacePath.toFile(), maxSizeBytes);

        result.overallSafe = result.pathSafe && result.pathAllowed && result.sizeAllowed;

        if (!result.overallSafe) {
            logger.warn("Workspace security check failed: {}", result);
        }

        return result;
    }

    /**
     * 安全检查结果
     */
    public static class SecurityCheckResult {
        public boolean pathSafe;
        public boolean pathAllowed;
        public boolean sizeAllowed;
        public boolean overallSafe;

        @Override
        public String toString() {
            return String.format("SecurityCheckResult{pathSafe=%s, pathAllowed=%s, sizeAllowed=%s, overallSafe=%s}",
                               pathSafe, pathAllowed, sizeAllowed, overallSafe);
        }
    }
}