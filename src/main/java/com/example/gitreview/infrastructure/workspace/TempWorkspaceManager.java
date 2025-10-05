package com.example.gitreview.infrastructure.workspace;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

/**
 * Temporary workspace manager for Git operations.
 *
 * @author zhourui(V33215020)
 * @since 2025/10/05
 */
@Component
public class TempWorkspaceManager {
    
    @Value("${git.temp.dir:C:/tmp/git-review}")
    private String tempBaseDir;
    
    /**
     * Create temporary workspace.
     *
     * @param workflowId workflow ID
     * @return workspace path
     */
    public File createWorkspace(Long workflowId) throws IOException {
        return createWorkspace("workflow-" + workflowId);
    }

    /**
     * Create temporary workspace with custom name.
     *
     * @param workspaceName workspace name
     * @return workspace path
     */
    public File createWorkspace(String workspaceName) throws IOException {
        Path workspacePath = Paths.get(tempBaseDir, workspaceName);
        Files.createDirectories(workspacePath);
        return workspacePath.toFile();
    }
    
    /**
     * Clean up workspace.
     *
     * @param workspaceDir workspace directory
     */
    public void cleanupWorkspace(File workspaceDir) throws IOException {
        if (workspaceDir == null || !workspaceDir.exists()) {
            return;
        }
        
        Files.walk(workspaceDir.toPath())
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
    
    /**
     * Get workspace path.
     *
     * @param workflowId workflow ID
     * @return workspace path
     */
    public File getWorkspace(Long workflowId) {
        return Paths.get(tempBaseDir, "workflow-" + workflowId).toFile();
    }

    /**
     * Get workspace file by name.
     *
     * @param workspaceName workspace name
     * @return workspace file
     */
    public File getWorkspaceFile(String workspaceName) {
        return Paths.get(tempBaseDir, workspaceName).toFile();
    }
}
