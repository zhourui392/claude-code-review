package com.example.gitreview.infrastructure.workspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

/**
 * 工作空间清理服务
 * 定期清理过期的临时工作空间
 */
@Service
public class WorkspaceCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(WorkspaceCleanupService.class);

    @Autowired
    private TempWorkspaceManager tempWorkspaceManager;

    /**
     * 应用启动后的初始化
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("Workspace cleanup service started");

        // 启动时清理一次过期工作空间
        tempWorkspaceManager.cleanupExpiredWorkspaces();
    }

    /**
     * 定期清理过期工作空间
     * 每10分钟执行一次
     */
    @Scheduled(fixedRate = 10 * 60 * 1000) // 10分钟
    public void cleanupExpiredWorkspaces() {
        try {
            logger.debug("Starting scheduled workspace cleanup");
            tempWorkspaceManager.cleanupExpiredWorkspaces();

            int activeCount = tempWorkspaceManager.getActiveWorkspaceCount();
            logger.debug("Workspace cleanup completed. Active workspaces: {}", activeCount);
        } catch (Exception e) {
            logger.error("Error during scheduled workspace cleanup", e);
        }
    }

    /**
     * 清理所有工作空间（紧急情况下使用）
     */
    public void emergencyCleanup() {
        logger.warn("Performing emergency workspace cleanup");
        tempWorkspaceManager.cleanupAllWorkspaces();
    }

    /**
     * 获取工作空间统计信息
     * @return 统计信息
     */
    public WorkspaceStats getWorkspaceStats() {
        int activeCount = tempWorkspaceManager.getActiveWorkspaceCount();
        return new WorkspaceStats(activeCount);
    }

    /**
     * 应用关闭时的清理
     */
    @PreDestroy
    public void onApplicationShutdown() {
        logger.info("Application shutting down, cleaning up all workspaces");
        tempWorkspaceManager.cleanupAllWorkspaces();
        tempWorkspaceManager.shutdown();
    }

    /**
     * 工作空间统计信息
     */
    public static class WorkspaceStats {
        private final int activeWorkspaces;

        public WorkspaceStats(int activeWorkspaces) {
            this.activeWorkspaces = activeWorkspaces;
        }

        public int getActiveWorkspaces() {
            return activeWorkspaces;
        }

        @Override
        public String toString() {
            return String.format("WorkspaceStats{activeWorkspaces=%d}", activeWorkspaces);
        }
    }
}