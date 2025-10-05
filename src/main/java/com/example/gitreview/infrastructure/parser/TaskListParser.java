package com.example.gitreview.infrastructure.parser;

import com.example.gitreview.domain.workflow.model.TaskStatus;
import com.example.gitreview.domain.workflow.model.valueobject.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 任务列表解析器
 * 解析 Markdown 格式的任务列表，提取任务信息
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
@Component
public class TaskListParser {

    private static final Logger logger = LoggerFactory.getLogger(TaskListParser.class);

    private static final Pattern TASK_TITLE_PATTERN = Pattern.compile("^### .* (P\\d+-\\d+).*$");
    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile("\\*\\*依赖\\*\\*:?\\s*(.+)");
    private static final Pattern TARGET_FILE_PATTERN = Pattern.compile("\\*\\*文件\\*\\*:?\\s*(.+)");
    private static final Pattern WORKLOAD_PATTERN = Pattern.compile("\\*\\*工时\\*\\*:?\\s*(.+)");
    private static final Pattern CHECKLIST_PATTERN = Pattern.compile("^- \\[ \\] (.+)$");

    /**
     * 解析 Markdown 格式的任务列表
     *
     * @param markdownContent 任务列表 Markdown 内容
     * @return 任务列表
     */
    public List<Task> parse(String markdownContent) {
        if (markdownContent == null || markdownContent.trim().isEmpty()) {
            logger.warn("任务列表内容为空");
            return new ArrayList<>();
        }

        List<Task> tasks = new ArrayList<>();
        String[] lines = markdownContent.split("\\r?\\n");

        String currentTaskId = null;
        String currentTaskTitle = null;
        StringBuilder currentDescription = new StringBuilder();
        List<String> currentDependencies = new ArrayList<>();
        String currentTargetFile = null;

        for (String line : lines) {
            Matcher titleMatcher = TASK_TITLE_PATTERN.matcher(line);
            if (titleMatcher.matches()) {
                if (currentTaskId != null) {
                    tasks.add(createTask(currentTaskId, currentTaskTitle, currentDescription.toString(),
                            currentDependencies, currentTargetFile));
                }

                currentTaskId = titleMatcher.group(1);
                currentTaskTitle = line.replaceFirst("^### ", "").replaceFirst(" \\(?" + currentTaskId + "\\)?", "").trim();
                currentDescription = new StringBuilder();
                currentDependencies = new ArrayList<>();
                currentTargetFile = null;

                logger.debug("解析到任务: {} - {}", currentTaskId, currentTaskTitle);
                continue;
            }

            if (currentTaskId == null) {
                continue;
            }

            Matcher depMatcher = DEPENDENCY_PATTERN.matcher(line);
            if (depMatcher.find()) {
                String depStr = depMatcher.group(1).trim();
                if (!depStr.equals("无") && !depStr.equals("None")) {
                    currentDependencies = Arrays.stream(depStr.split("[,，]"))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());
                }
                continue;
            }

            Matcher fileMatcher = TARGET_FILE_PATTERN.matcher(line);
            if (fileMatcher.find()) {
                currentTargetFile = fileMatcher.group(1).trim();
                continue;
            }

            Matcher checklistMatcher = CHECKLIST_PATTERN.matcher(line);
            if (checklistMatcher.matches()) {
                if (currentDescription.length() > 0) {
                    currentDescription.append("\n");
                }
                currentDescription.append("- ").append(checklistMatcher.group(1));
            }
        }

        if (currentTaskId != null) {
            tasks.add(createTask(currentTaskId, currentTaskTitle, currentDescription.toString(),
                    currentDependencies, currentTargetFile));
        }

        logger.info("成功解析 {} 个任务", tasks.size());
        return tasks;
    }

    private Task createTask(String id, String title, String description,
                            List<String> dependencies, String targetFile) {
        return new Task(
                id,
                title,
                description,
                TaskStatus.PENDING,
                dependencies,
                targetFile,
                null,
                null
        );
    }
}
