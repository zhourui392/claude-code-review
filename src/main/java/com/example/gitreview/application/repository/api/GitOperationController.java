package com.example.gitreview.application.repository.api;

import com.example.gitreview.application.repository.GitRepositoryApplicationService;
import com.example.gitreview.application.repository.dto.BranchListDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Git操作Controller
 * 处理分支查询等Git操作
 */
@RestController
@RequestMapping("/api/git")
@CrossOrigin(origins = "*")
public class GitOperationController {

    private static final Logger logger = LoggerFactory.getLogger(GitOperationController.class);

    @Autowired
    private GitRepositoryApplicationService repositoryApplicationService;

    /**
     * 获取仓库分支列表
     */
    @GetMapping("/{repositoryId}/branches")
    public ResponseEntity<BranchListDTO> getBranches(@PathVariable Long repositoryId) {
        logger.info("Getting branches for repository: {}", repositoryId);
        BranchListDTO branches = repositoryApplicationService.getRemoteBranches(repositoryId);
        return ResponseEntity.ok(branches);
    }
}
