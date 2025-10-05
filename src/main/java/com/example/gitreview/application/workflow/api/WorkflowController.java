package com.example.gitreview.application.workflow.api;

import com.example.gitreview.application.workflow.WorkflowApplicationService;
import com.example.gitreview.application.workflow.dto.*;
import com.example.gitreview.domain.workflow.exception.InvalidWorkflowTransitionException;
import com.example.gitreview.domain.workflow.exception.WorkflowNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流 REST API 控制器
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
@RestController
@RequestMapping("/api/workflow")
@Validated
@CrossOrigin(origins = "*")
public class WorkflowController {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowController.class);

    @Autowired
    private WorkflowApplicationService workflowApplicationService;

    /**
     * 创建工作流
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createWorkflow(@Valid @RequestBody CreateWorkflowRequest request) {
        logger.info("创建工作流: {}", request.getName());
        Long workflowId = workflowApplicationService.createWorkflow(request);
        Map<String, Object> response = new HashMap<>();
        response.put("workflowId", workflowId);
        response.put("message", "工作流创建成功");
        return ResponseEntity.ok(response);
    }

    /**
     * 获取所有工作流
     */
    @GetMapping
    public ResponseEntity<List<WorkflowStatusDTO>> getAllWorkflows() {
        List<WorkflowStatusDTO> workflows = workflowApplicationService.getAllWorkflows();
        return ResponseEntity.ok(workflows);
    }

    /**
     * 获取工作流状态
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<WorkflowStatusDTO> getStatus(@PathVariable Long id) {
        WorkflowStatusDTO status = workflowApplicationService.getWorkflowStatus(id);
        return ResponseEntity.ok(status);
    }

    /**
     * 获取工作流进度
     */
    @GetMapping("/{id}/progress")
    public ResponseEntity<WorkflowProgressDTO> getProgress(@PathVariable Long id) {
        logger.info("获取工作流进度: {}", id);
        WorkflowProgressDTO progress = workflowApplicationService.getProgress(id);
        return ResponseEntity.ok(progress);
    }

    /**
     * 生成规格文档
     */
    @PostMapping("/{id}/spec/generate")
    public ResponseEntity<Map<String, String>> generateSpecification(
            @PathVariable Long id,
            @Valid @RequestBody SpecGenerationRequest request) {
        logger.info("生成规格文档，工作流ID: {}", id);
        workflowApplicationService.generateSpecification(id, request);
        Map<String, String> response = new HashMap<>();
        response.put("message", "规格文档生成已启动，请稍后查询状态");
        return ResponseEntity.accepted().body(response);
    }

    /**
     * 获取规格文档
     */
    @GetMapping("/{id}/spec")
    public ResponseEntity<SpecificationDTO> getSpecification(@PathVariable Long id) {
        logger.info("获取规格文档，工作流ID: {}", id);
        SpecificationDTO spec = workflowApplicationService.getSpecification(id);
        return ResponseEntity.ok(spec);
    }

    /**
     * 生成技术方案
     */
    @PostMapping("/{id}/tech-design/generate")
    public ResponseEntity<Map<String, String>> generateTechnicalDesign(@PathVariable Long id) {
        logger.info("生成技术方案，工作流ID: {}", id);
        workflowApplicationService.generateTechnicalDesign(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "技术方案生成已启动，请稍后查询状态");
        return ResponseEntity.accepted().body(response);
    }

    /**
     * 获取技术方案
     */
    @GetMapping("/{id}/tech-design")
    public ResponseEntity<TechnicalDesignDTO> getTechnicalDesign(@PathVariable Long id) {
        logger.info("获取技术方案，工作流ID: {}", id);
        TechnicalDesignDTO design = workflowApplicationService.getTechnicalDesign(id);
        return ResponseEntity.ok(design);
    }

    /**
     * 更新技术方案
     */
    @PutMapping("/{id}/tech-design")
    public ResponseEntity<Map<String, String>> updateTechnicalDesign(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        logger.info("更新技术方案，工作流ID: {}", id);
        String content = payload.get("content");
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "技术方案内容不能为空"));
        }
        workflowApplicationService.updateTechnicalDesign(id, content);
        return ResponseEntity.ok(Map.of("message", "技术方案更新成功"));
    }

    /**
     * 批准技术方案
     */
    @PostMapping("/{id}/tech-design/approve")
    public ResponseEntity<Map<String, String>> approveTechnicalDesign(@PathVariable Long id) {
        logger.info("批准技术方案，工作流ID: {}", id);
        workflowApplicationService.approveTechnicalDesign(id);
        return ResponseEntity.ok(Map.of("message", "技术方案已批准"));
    }

    /**
     * 生成任务列表
     */
    @PostMapping("/{id}/tasklist/generate")
    public ResponseEntity<Map<String, String>> generateTaskList(@PathVariable Long id) {
        logger.info("生成任务列表，工作流ID: {}", id);
        workflowApplicationService.generateTaskList(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "任务列表生成已启动，请稍后查询状态");
        return ResponseEntity.accepted().body(response);
    }

    /**
     * 获取任务列表
     */
    @GetMapping("/{id}/tasklist")
    public ResponseEntity<TaskListDTO> getTaskList(@PathVariable Long id) {
        logger.info("获取任务列表，工作流ID: {}", id);
        TaskListDTO taskList = workflowApplicationService.getTaskList(id);
        return ResponseEntity.ok(taskList);
    }

    /**
     * 开始代码生成
     */
    @PostMapping("/{id}/code-generation/start")
    public ResponseEntity<Map<String, String>> startCodeGeneration(@PathVariable Long id) {
        logger.info("开始代码生成，工作流ID: {}", id);
        workflowApplicationService.startCodeGeneration(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "代码生成已启动，请稍后查询进度");
        return ResponseEntity.accepted().body(response);
    }

    /**
     * 取消工作流
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, String>> cancelWorkflow(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        logger.info("取消工作流，ID: {}", id);
        String reason = payload.getOrDefault("reason", "用户取消");
        workflowApplicationService.cancelWorkflow(id, reason);
        return ResponseEntity.ok(Map.of("message", "工作流已取消"));
    }
}
