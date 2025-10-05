package com.example.gitreview.infrastructure.parser;

import com.example.gitreview.domain.workflow.model.valueobject.Task;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Claude-generated task lists.
 *
 * @author zhourui(V33215020)
 * @since 2025/10/05
 */
@Component
public class TaskListParser {
    
    private static final Pattern TASK_PATTERN = Pattern.compile("^\\s*\\d+\\.\\s+(.+)$", Pattern.MULTILINE);
    
    /**
     * Parse task list from markdown format.
     *
     * @param markdown markdown content
     * @return list of tasks
     */
    public List<Task> parse(String markdown) {
        List<Task> tasks = new ArrayList<>();
        Matcher matcher = TASK_PATTERN.matcher(markdown);
        
        while (matcher.find()) {
            String taskDescription = matcher.group(1).trim();
            tasks.add(new Task(UUID.randomUUID().toString(), taskDescription));
        }
        
        return tasks;
    }
}
