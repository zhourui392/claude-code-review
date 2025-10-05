package com.example.gitreview.domain.workflow.model.valueobject;

import com.example.gitreview.domain.workflow.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * TaskList 值对象单元测试
 *
 * @author zhourui(V33215020)
 * @since 2025/10/04
 */
class TaskListTest {

    @Test
    void should_create_task_list_with_tasks() {
        Task task1 = createPendingTask("P0-1", Collections.emptyList());
        Task task2 = createPendingTask("P0-2", Arrays.asList("P0-1"));
        List<Task> tasks = Arrays.asList(task1, task2);

        TaskList taskList = new TaskList("任务列表内容", tasks, LocalDateTime.now());

        assertThat(taskList.getContent()).isEqualTo("任务列表内容");
        assertThat(taskList.getTasks()).hasSize(2);
        assertThat(taskList.getGeneratedAt()).isNotNull();
    }

    @Test
    void should_get_executable_tasks_when_no_dependencies() {
        Task task1 = createPendingTask("P0-1", Collections.emptyList());
        Task task2 = createPendingTask("P0-2", Collections.emptyList());
        TaskList taskList = new TaskList("内容", Arrays.asList(task1, task2), LocalDateTime.now());

        List<Task> executableTasks = taskList.getExecutableTasks();

        assertThat(executableTasks).hasSize(2);
    }

    @Test
    void should_get_executable_tasks_when_dependencies_met() {
        Task task1 = createCompletedTask("P0-1", Collections.emptyList());
        Task task2 = createPendingTask("P0-2", Arrays.asList("P0-1"));
        TaskList taskList = new TaskList("内容", Arrays.asList(task1, task2), LocalDateTime.now());

        List<Task> executableTasks = taskList.getExecutableTasks();

        assertThat(executableTasks).hasSize(1);
        assertThat(executableTasks.get(0).getId()).isEqualTo("P0-2");
    }

    @Test
    void should_not_get_executable_tasks_when_dependencies_not_met() {
        Task task1 = createPendingTask("P0-1", Collections.emptyList());
        Task task2 = createPendingTask("P0-2", Arrays.asList("P0-1"));
        TaskList taskList = new TaskList("内容", Arrays.asList(task1, task2), LocalDateTime.now());

        List<Task> executableTasks = taskList.getExecutableTasks();

        assertThat(executableTasks).hasSize(1);
        assertThat(executableTasks.get(0).getId()).isEqualTo("P0-1");
    }

    @Test
    void should_calculate_zero_progress_when_no_tasks_completed() {
        Task task1 = createPendingTask("P0-1", Collections.emptyList());
        Task task2 = createPendingTask("P0-2", Collections.emptyList());
        TaskList taskList = new TaskList("内容", Arrays.asList(task1, task2), LocalDateTime.now());

        int progress = taskList.getProgress();

        assertThat(progress).isEqualTo(0);
    }

    @Test
    void should_calculate_50_progress_when_half_completed() {
        Task task1 = createCompletedTask("P0-1", Collections.emptyList());
        Task task2 = createPendingTask("P0-2", Collections.emptyList());
        TaskList taskList = new TaskList("内容", Arrays.asList(task1, task2), LocalDateTime.now());

        int progress = taskList.getProgress();

        assertThat(progress).isEqualTo(50);
    }

    @Test
    void should_calculate_100_progress_when_all_completed() {
        Task task1 = createCompletedTask("P0-1", Collections.emptyList());
        Task task2 = createCompletedTask("P0-2", Collections.emptyList());
        TaskList taskList = new TaskList("内容", Arrays.asList(task1, task2), LocalDateTime.now());

        int progress = taskList.getProgress();

        assertThat(progress).isEqualTo(100);
    }

    @Test
    void should_return_zero_progress_when_task_list_empty() {
        TaskList taskList = new TaskList("内容", Collections.emptyList(), LocalDateTime.now());

        int progress = taskList.getProgress();

        assertThat(progress).isEqualTo(0);
    }

    @Test
    void should_find_task_by_id() {
        Task task1 = createPendingTask("P0-1", Collections.emptyList());
        Task task2 = createPendingTask("P0-2", Collections.emptyList());
        TaskList taskList = new TaskList("内容", Arrays.asList(task1, task2), LocalDateTime.now());

        Optional<Task> found = taskList.getTaskById("P0-1");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo("P0-1");
    }

    @Test
    void should_return_empty_when_task_not_found() {
        Task task1 = createPendingTask("P0-1", Collections.emptyList());
        TaskList taskList = new TaskList("内容", Arrays.asList(task1), LocalDateTime.now());

        Optional<Task> found = taskList.getTaskById("P0-999");

        assertThat(found).isEmpty();
    }

    @Test
    void should_be_equal_when_same_content_and_tasks() {
        Task task = createPendingTask("P0-1", Collections.emptyList());
        TaskList taskList1 = new TaskList("内容", Arrays.asList(task), LocalDateTime.now());
        TaskList taskList2 = new TaskList("内容", Arrays.asList(task), LocalDateTime.now().plusHours(1));

        assertThat(taskList1).isEqualTo(taskList2);
    }

    private Task createPendingTask(String id, List<String> dependencies) {
        return new Task(id, "任务" + id, "描述", TaskStatus.PENDING,
                dependencies, "File.java", null, null);
    }

    private Task createCompletedTask(String id, List<String> dependencies) {
        return new Task(id, "任务" + id, "描述", TaskStatus.COMPLETED,
                dependencies, "File.java", "code", LocalDateTime.now());
    }
}
