package com.example.gitreview.infrastructure.workspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 临时工作空间管理器
 * 管理临时目录的创建、使用和清理
 */
@Service
public class TempWorkspaceManager {

    private static final Logger logger = LoggerFactory.getLogger(TempWorkspaceManager.class);

    private final Map<String, WorkspaceInfo> activeWorkspaces = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    /**
     * 工作空间信息
     */
    private static class WorkspaceInfo {
        final String workspaceId;
        final Path path;
        final LocalDateTime createTime;
        final long ttlMinutes;

        WorkspaceInfo(String workspaceId, Path path, long ttlMinutes) {
            this.workspaceId = workspaceId;
            this.path = path;
            this.createTime = LocalDateTime.now();
            this.ttlMinutes = ttlMinutes;
        }

        boolean isExpired() {
            return LocalDateTime.now().isAfter(createTime.plusMinutes(ttlMinutes));
        }
    }

    /**
     * 创建临时工作空间
     * @param prefix 目录前缀
     * @param ttlMinutes 生存时间（分钟）
     * @return 工作空间ID
     * @throws IOException 创建失败异常
     */
    public String createWorkspace(String prefix, long ttlMinutes) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String workspaceId = prefix + "_" + timestamp + "_" + System.currentTimeMillis();

        Path workspacePath = Files.createTempDirectory(prefix + "-");
        WorkspaceInfo workspaceInfo = new WorkspaceInfo(workspaceId, workspacePath, ttlMinutes);

        activeWorkspaces.put(workspaceId, workspaceInfo);

        logger.info("Created workspace: {} at {} (TTL: {} minutes)",
                   workspaceId, workspacePath, ttlMinutes);

        // 启动自动清理任务
        scheduleCleanup(workspaceId, ttlMinutes);

        return workspaceId;
    }

    /**
     * 获取工作空间路径
     * @param workspaceId 工作空间ID
     * @return 工作空间路径
     */
    public Path getWorkspacePath(String workspaceId) {
        WorkspaceInfo info = activeWorkspaces.get(workspaceId);
        if (info == null) {
            throw new IllegalArgumentException("Workspace not found: " + workspaceId);
        }
        return info.path;
    }

    /**
     * 获取工作空间文件对象
     * @param workspaceId 工作空间ID
     * @return 工作空间文件对象
     */
    public File getWorkspaceFile(String workspaceId) {
        return getWorkspacePath(workspaceId).toFile();
    }

    /**
     * 检查工作空间是否存在
     * @param workspaceId 工作空间ID
     * @return 是否存在
     */
    public boolean workspaceExists(String workspaceId) {
        WorkspaceInfo info = activeWorkspaces.get(workspaceId);
        return info != null && Files.exists(info.path);
    }

    /**
     * 清理工作空间
     * @param workspaceId 工作空间ID
     * @return 是否清理成功
     */
    public boolean cleanupWorkspace(String workspaceId) {
        WorkspaceInfo info = activeWorkspaces.remove(workspaceId);
        if (info == null) {
            logger.warn("Workspace not found for cleanup: {}", workspaceId);
            return false;
        }

        try {
            deleteRecursively(info.path.toFile());
            logger.info("Cleaned up workspace: {} at {}", workspaceId, info.path);
            return true;
        } catch (Exception e) {
            logger.error("Failed to cleanup workspace: {} at {}", workspaceId, info.path, e);
            return false;
        }
    }

    /**
     * 清理所有过期的工作空间
     */
    public void cleanupExpiredWorkspaces() {
        int cleaned = 0;
        for (Map.Entry<String, WorkspaceInfo> entry : activeWorkspaces.entrySet()) {
            if (entry.getValue().isExpired()) {
                if (cleanupWorkspace(entry.getKey())) {
                    cleaned++;
                }
            }
        }
        if (cleaned > 0) {
            logger.info("Cleaned up {} expired workspaces", cleaned);
        }
    }

    /**
     * 清理所有工作空间
     */
    public void cleanupAllWorkspaces() {
        int cleaned = 0;
        for (String workspaceId : activeWorkspaces.keySet()) {
            if (cleanupWorkspace(workspaceId)) {
                cleaned++;
            }
        }
        logger.info("Cleaned up all {} workspaces", cleaned);
    }

    /**
     * 获取活跃工作空间数量
     * @return 活跃工作空间数量
     */
    public int getActiveWorkspaceCount() {
        return activeWorkspaces.size();
    }

    /**
     * 扩展工作空间TTL
     * @param workspaceId 工作空间ID
     * @param additionalMinutes 额外的分钟数
     */
    public void extendWorkspaceTTL(String workspaceId, long additionalMinutes) {
        WorkspaceInfo info = activeWorkspaces.get(workspaceId);
        if (info != null) {
            // 重新创建工作空间信息以更新TTL
            WorkspaceInfo newInfo = new WorkspaceInfo(info.workspaceId, info.path,
                                                     info.ttlMinutes + additionalMinutes);
            activeWorkspaces.put(workspaceId, newInfo);

            logger.debug("Extended TTL for workspace: {} by {} minutes", workspaceId, additionalMinutes);
        }
    }

    /**
     * 获取工作空间信息
     * @param workspaceId 工作空间ID
     * @return 工作空间信息字符串
     */
    public String getWorkspaceInfo(String workspaceId) {
        WorkspaceInfo info = activeWorkspaces.get(workspaceId);
        if (info == null) {
            return "Workspace not found: " + workspaceId;
        }

        return String.format("Workspace %s: path=%s, created=%s, ttl=%d minutes, expired=%s",
                           workspaceId, info.path, info.createTime, info.ttlMinutes, info.isExpired());
    }

    /**
     * 计划清理任务
     */
    private void scheduleCleanup(String workspaceId, long ttlMinutes) {
        cleanupExecutor.schedule(() -> {
            if (activeWorkspaces.containsKey(workspaceId)) {
                cleanupWorkspace(workspaceId);
            }
        }, ttlMinutes, TimeUnit.MINUTES);
    }

    /**
     * 递归删除目录
     */
    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursively(child);
                }
            }
        }

        if (!file.delete()) {
            logger.warn("Failed to delete: {}", file.getAbsolutePath());
        }
    }

    /**
     * 关闭清理执行器
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}