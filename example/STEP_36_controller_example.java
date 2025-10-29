// STEP_36 Controller層実装例

package com.example.taskapp.controller;

import com.example.taskapp.entity.Task;
import com.example.taskapp.entity.enums.TaskStatus;
import com.example.taskapp.dto.TaskSearchCriteria;
import com.example.taskapp.dto.request.TaskCreateRequest;
import com.example.taskapp.service.TaskService;
import com.example.taskapp.service.ProjectService;
import com.example.taskapp.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final ProjectService projectService;
    private final UserService userService;

    /**
     * タスク一覧画面
     */
    @GetMapping
    public String listTasks(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String keyword,
            Model model,
            Authentication authentication) {
        
        TaskSearchCriteria criteria = new TaskSearchCriteria();
        criteria.setProjectId(projectId);
        criteria.setStatus(status);
        criteria.setPriority(priority);
        criteria.setKeyword(keyword);
        
        List<Task> tasks = taskService.searchTasks(criteria);
        
        model.addAttribute("tasks", tasks);
        model.addAttribute("projects", projectService.getUserProjects(authentication.getName()));
        model.addAttribute("criteria", criteria);
        
        return "tasks/list";
    }

    /**
     * カンバンボード画面
     */
    @GetMapping("/kanban")
    public String kanbanBoard(@RequestParam Long projectId, Model model) {
        List<Task> tasks = taskService.getProjectTasks(projectId);
        
        // ステータスごとにグループ化
        List<Task> todoTasks = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.TODO)
                .toList();
        List<Task> inProgressTasks = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS)
                .toList();
        List<Task> doneTasks = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .toList();
        
        model.addAttribute("project", projectService.getProjectById(projectId));
        model.addAttribute("todoTasks", todoTasks);
        model.addAttribute("inProgressTasks", inProgressTasks);
        model.addAttribute("doneTasks", doneTasks);
        
        return "tasks/kanban";
    }

    /**
     * タスク詳細画面
     */
    @GetMapping("/{id}")
    public String taskDetail(@PathVariable Long id, Model model) {
        Task task = taskService.getTaskById(id);
        model.addAttribute("task", task);
        return "tasks/detail";
    }

    /**
     * タスク作成フォーム
     */
    @GetMapping("/new")
    public String newTaskForm(@RequestParam Long projectId, Model model) {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setProjectId(projectId);
        
        model.addAttribute("task", request);
        model.addAttribute("project", projectService.getProjectById(projectId));
        model.addAttribute("users", userService.getAllUsers());
        
        return "tasks/form";
    }

    /**
     * タスク作成処理
     */
    @PostMapping
    public String createTask(
            @Valid @ModelAttribute("task") TaskCreateRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            model.addAttribute("project", projectService.getProjectById(request.getProjectId()));
            model.addAttribute("users", userService.getAllUsers());
            return "tasks/form";
        }
        
        Task created = taskService.createTask(request);
        redirectAttributes.addFlashAttribute("message", "タスクを作成しました");
        
        return "redirect:/tasks/" + created.getId();
    }

    /**
     * タスク編集フォーム
     */
    @GetMapping("/{id}/edit")
    public String editTaskForm(@PathVariable Long id, Model model) {
        Task task = taskService.getTaskById(id);
        
        TaskCreateRequest request = new TaskCreateRequest();
        request.setProjectId(task.getProjectId());
        request.setTitle(task.getTitle());
        request.setDescription(task.getDescription());
        request.setStatus(task.getStatus().name());
        request.setPriority(task.getPriority().name());
        request.setAssigneeId(task.getAssigneeId());
        request.setDueDate(task.getDueDate());
        
        model.addAttribute("task", request);
        model.addAttribute("taskId", id);
        model.addAttribute("project", projectService.getProjectById(task.getProjectId()));
        model.addAttribute("users", userService.getAllUsers());
        
        return "tasks/form";
    }

    /**
     * タスク更新処理
     */
    @PostMapping("/{id}")
    public String updateTask(
            @PathVariable Long id,
            @Valid @ModelAttribute("task") TaskCreateRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            model.addAttribute("taskId", id);
            model.addAttribute("project", projectService.getProjectById(request.getProjectId()));
            model.addAttribute("users", userService.getAllUsers());
            return "tasks/form";
        }
        
        taskService.updateTask(id, request);
        redirectAttributes.addFlashAttribute("message", "タスクを更新しました");
        
        return "redirect:/tasks/" + id;
    }

    /**
     * ステータス更新（AJAX用）
     */
    @PostMapping("/{id}/status")
    @ResponseBody
    public Map<String, Object> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        
        taskService.updateTaskStatus(id, status);
        return Map.of("success", true, "message", "ステータスを更新しました");
    }

    /**
     * タスク削除
     */
    @PostMapping("/{id}/delete")
    public String deleteTask(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Task task = taskService.getTaskById(id);
        Long projectId = task.getProjectId();
        
        taskService.deleteTask(id);
        redirectAttributes.addFlashAttribute("message", "タスクを削除しました");
        
        return "redirect:/projects/" + projectId;
    }
}

// ========================================

package com.example.taskapp.controller;

import com.example.taskapp.entity.Project;
import com.example.taskapp.dto.request.ProjectCreateRequest;
import com.example.taskapp.service.ProjectService;
import com.example.taskapp.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final TaskService taskService;

    /**
     * プロジェクト一覧画面
     */
    @GetMapping
    public String listProjects(Model model, Authentication authentication) {
        List<Project> projects = projectService.getUserProjects(authentication.getName());
        model.addAttribute("projects", projects);
        return "projects/list";
    }

    /**
     * プロジェクト詳細画面
     */
    @GetMapping("/{id}")
    public String projectDetail(@PathVariable Long id, Model model) {
        Project project = projectService.getProjectById(id);
        var tasks = taskService.getProjectTasks(id);
        
        model.addAttribute("project", project);
        model.addAttribute("tasks", tasks);
        
        return "projects/detail";
    }

    /**
     * プロジェクト作成フォーム
     */
    @GetMapping("/new")
    public String newProjectForm(Model model) {
        model.addAttribute("project", new ProjectCreateRequest());
        return "projects/form";
    }

    /**
     * プロジェクト作成処理
     */
    @PostMapping
    public String createProject(
            @Valid @ModelAttribute("project") ProjectCreateRequest request,
            BindingResult result,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            return "projects/form";
        }
        
        Project created = projectService.createProject(request, authentication.getName());
        redirectAttributes.addFlashAttribute("message", "プロジェクトを作成しました");
        
        return "redirect:/projects/" + created.getId();
    }
}

// ========================================

package com.example.taskapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class TaskCreateRequest {
    
    @NotNull(message = "プロジェクトIDは必須です")
    private Long projectId;
    
    @NotBlank(message = "タイトルは必須です")
    @Size(max = 200, message = "タイトルは200文字以内で入力してください")
    private String title;
    
    @Size(max = 2000, message = "説明は2000文字以内で入力してください")
    private String description;
    
    private String status = "TODO";
    private String priority = "MEDIUM";
    private Long assigneeId;
    private LocalDate dueDate;
}

// ========================================

package com.example.taskapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProjectCreateRequest {
    
    @NotBlank(message = "プロジェクト名は必須です")
    @Size(max = 100, message = "プロジェクト名は100文字以内で入力してください")
    private String name;
    
    @Size(max = 2000, message = "説明は2000文字以内で入力してください")
    private String description;
}
