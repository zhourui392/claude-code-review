package com.example.gitreview.domain.workflow.model.valueobject;

import com.example.gitreview.domain.workflow.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.util.Collections;

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
        Task task = new Task("P0-1", "测试任务描述");

        assertThat(task.getId()).isEqualTo("P0-1");
        assertThat(task.getTitle()).isEqualTo("测试任务描述");
        assertThat(task.getDescription()).isEqualTo("测试任务描述");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(task.getGeneratedCode()).isNull();
        assertThat(task.getErrorMessage()).isNull();
    }

    @Test
    void should_complete_task_and_set_code() {
        Task task = new Task("P0-1", "测试任务");
        String code = "public class Test {}";

        Task completedTask = task.complete(code);

        assertThat(completedTask.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(completedTask.getGeneratedCode()).isEqualTo(code);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
    }

    @Test
    void should_mark_task_as_failed() {
        Task task = new Task("P0-1", "测试任务");

        Task failedTask = task.fail("生成失败");

        assertThat(failedTask.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(failedTask.getErrorMessage()).isEqualTo("生成失败");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
    }

    @Test
    void should_mark_in_progress() {
        Task task = new Task("P0-1", "测试任务");

        task.markInProgress();

        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void should_mark_completed() {
        Task task = new Task("P0-1", "测试任务");

        task.markCompleted();

        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    void should_mark_failed() {
        Task task = new Task("P0-1", "测试任务");

        task.markFailed("编译错误");

        assertThat(task.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(task.getErrorMessage()).isEqualTo("编译错误");
    }

    @Test
    void should_get_dependencies() {
        Task task = new Task("P0-1", "测试任务");

        assertThat(task.getDependencies()).isNotNull();
        assertThat(task.getDependencies()).isEmpty();
    }
}
