package com.example.gitreview.domain.workflow.model.valueobject;

import com.example.gitreview.domain.workflow.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Task 值对象单元测试
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
class TaskTest {

    @Test
    void should_create_task_with_pending_status() {
        Task task = new Task("P0-1", "测试任务", "任务描述", TaskStatus.PENDING,
                Collections.emptyList(), "File.java", null, null);

        assertThat(task.getId()).isEqualTo("P0-1");
        assertThat(task.getTitle()).isEqualTo("测试任务");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(task.getGeneratedCode()).isNull();
        assertThat(task.getCompletedAt()).isNull();
    }

    @Test
    void should_complete_task_and_set_code() {
        Task task = new Task("P0-1", "测试任务", "描述", TaskStatus.PENDING,
                Collections.emptyList(), "File.java", null, null);
        String code = "public class Test {}";

        Task completedTask = task.complete(code);

        assertThat(completedTask.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(completedTask.getGeneratedCode()).isEqualTo(code);
        assertThat(completedTask.getCompletedAt()).isNotNull();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
    }

    @Test
    void should_mark_task_as_failed() {
        Task task = new Task("P0-1", "测试任务", "描述", TaskStatus.PENDING,
                Collections.emptyList(), "File.java", null, null);

        Task failedTask = task.fail("生成失败");

        assertThat(failedTask.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
    }

    @Test
    void should_be_executable_when_no_dependencies() {
        Task task = new Task("P0-1", "测试任务", "描述", TaskStatus.PENDING,
                Collections.emptyList(), "File.java", null, null);

        boolean executable = task.isExecutable(Collections.emptyList());

        assertThat(executable).isTrue();
    }

    @Test
    void should_be_executable_when_all_dependencies_completed() {
        Task dep1 = new Task("P0-1", "依赖1", "描述", TaskStatus.COMPLETED,
                Collections.emptyList(), "File1.java", "code1", LocalDateTime.now());
        Task dep2 = new Task("P0-2", "依赖2", "描述", TaskStatus.COMPLETED,
                Collections.emptyList(), "File2.java", "code2", LocalDateTime.now());
        Task task = new Task("P0-3", "测试任务", "描述", TaskStatus.PENDING,
                Arrays.asList("P0-1", "P0-2"), "File3.java", null, null);

        boolean executable = task.isExecutable(Arrays.asList(dep1, dep2, task));

        assertThat(executable).isTrue();
    }

    @Test
    void should_not_be_executable_when_dependencies_not_completed() {
        Task dep1 = new Task("P0-1", "依赖1", "描述", TaskStatus.PENDING,
                Collections.emptyList(), "File1.java", null, null);
        Task task = new Task("P0-2", "测试任务", "描述", TaskStatus.PENDING,
                Arrays.asList("P0-1"), "File2.java", null, null);

        boolean executable = task.isExecutable(Arrays.asList(dep1, task));

        assertThat(executable).isFalse();
    }

    @Test
    void should_not_be_executable_when_already_completed() {
        Task task = new Task("P0-1", "测试任务", "描述", TaskStatus.COMPLETED,
                Collections.emptyList(), "File.java", "code", LocalDateTime.now());

        boolean executable = task.isExecutable(Collections.emptyList());

        assertThat(executable).isFalse();
    }

    @Test
    void should_be_equal_when_same_id() {
        Task task1 = new Task("P0-1", "任务1", "描述1", TaskStatus.PENDING,
                Collections.emptyList(), "File1.java", null, null);
        Task task2 = new Task("P0-1", "任务2", "描述2", TaskStatus.COMPLETED,
                Collections.emptyList(), "File2.java", "code", LocalDateTime.now());

        assertThat(task1).isEqualTo(task2);
        assertThat(task1.hashCode()).isEqualTo(task2.hashCode());
    }

    @Test
    void should_not_be_equal_when_different_id() {
        Task task1 = new Task("P0-1", "任务", "描述", TaskStatus.PENDING,
                Collections.emptyList(), "File.java", null, null);
        Task task2 = new Task("P0-2", "任务", "描述", TaskStatus.PENDING,
                Collections.emptyList(), "File.java", null, null);

        assertThat(task1).isNotEqualTo(task2);
    }
}
