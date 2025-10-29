# Step 36: サービスとコントローラー実装（Thymeleaf + MyBatis）

## 🎯 このステップの目標

- **MyBatis Mapper**でデータアクセス層を実装する
- **Service層**でビジネスロジックを実装する
- **Thymeleafコントローラー**で画面制御を実装する
- DTOを使ったデータ変換を理解する

**所要時間**: 約3時間

---

## 📋 実装要件

このステップでは、タスク管理システムのコア機能を**Thymeleaf + MyBatis**で実装します。

### 実装の流れ

```
1. Entity定義 (POJOクラス)
   ↓
2. MyBatis Mapper作成 (Interface + XML)
   ↓
3. Service層実装 (ビジネスロジック)
   ↓
4. Thymeleafコントローラー実装 (画面制御)
   ↓
5. Thymeleafテンプレート作成 (HTML)
```

---

## 🚀 ステップ1: Entityクラスの実装

### 1-1. Task Entity

**ファイルパス**: `src/main/java/com/example/taskapp/entity/Task.java`

```java
package com.example.taskapp.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Task {
    private Long id;
    private Long projectId;
    private String title;
    private String description;
    private TaskStatus status;
    private Priority priority;
    private Long assigneeId;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // MyBatisのJOIN結果用（transient）
    private String projectName;
    private String assigneeName;
}
```

### 1-2. Enum定義

**TaskStatus.java**:
```java
package com.example.taskapp.entity.enums;

public enum TaskStatus {
    TODO("未着手"),
    IN_PROGRESS("進行中"),
    DONE("完了");
    
    private final String displayName;
    
    TaskStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
```

**Priority.java**:
```java
package com.example.taskapp.entity.enums;

public enum Priority {
    LOW("低", "#6c757d"),
    MEDIUM("中", "#ffc107"),
    HIGH("高", "#dc3545");
    
    private final String displayName;
    private final String color;
    
    Priority(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getColor() {
        return color;
    }
}
```

---

## 🚀 ステップ2: MyBatis Mapperの実装

### 2-1. TaskMapper Interface

**ファイルパス**: `src/main/java/com/example/taskapp/mapper/TaskMapper.java`

```java
package com.example.taskapp.mapper;

import com.example.taskapp.entity.Task;
import com.example.taskapp.dto.TaskSearchCriteria;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Optional;

@Mapper
public interface TaskMapper {
    
    /**
     * タスク作成
     */
    @Insert("""
        INSERT INTO tasks (project_id, title, description, status, priority, assignee_id, due_date)
        VALUES (#{projectId}, #{title}, #{description}, #{status}, #{priority}, #{assigneeId}, #{dueDate})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Task task);
    
    /**
     * タスク取得（詳細情報付き）
     */
    @Select("""
        SELECT 
            t.*,
            p.name as project_name,
            u.username as assignee_name
        FROM tasks t
        LEFT JOIN projects p ON t.project_id = p.id
        LEFT JOIN users u ON t.assignee_id = u.id
        WHERE t.id = #{id}
    """)
    Optional<Task> findByIdWithDetails(Long id);
    
    /**
     * プロジェクトのタスク一覧
     */
    @Select("""
        SELECT 
            t.*,
            u.username as assignee_name
        FROM tasks t
        LEFT JOIN users u ON t.assignee_id = u.id
        WHERE t.project_id = #{projectId}
        ORDER BY 
            CASE t.status
                WHEN 'TODO' THEN 1
                WHEN 'IN_PROGRESS' THEN 2
                WHEN 'DONE' THEN 3
            END,
            t.created_at DESC
    """)
    List<Task> findByProjectId(Long projectId);
    
    /**
     * タスク更新
     */
    @Update("""
        UPDATE tasks
        SET title = #{title},
            description = #{description},
            status = #{status},
            priority = #{priority},
            assignee_id = #{assigneeId},
            due_date = #{dueDate},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id}
    """)
    void update(Task task);
    
    /**
     * ステータス更新
     */
    @Update("""
        UPDATE tasks
        SET status = #{status},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id}
    """)
    void updateStatus(@Param("id") Long id, @Param("status") String status);
    
    /**
     * タスク削除
     */
    @Delete("DELETE FROM tasks WHERE id = #{id}")
    void deleteById(Long id);
    
    /**
     * タスク検索（動的SQL - XMLで定義）
     */
    List<Task> search(@Param("criteria") TaskSearchCriteria criteria);
    
    /**
     * 期限切れタスク取得
     */
    @Select("""
        SELECT 
            t.*,
            p.name as project_name,
            u.username as assignee_name
        FROM tasks t
        LEFT JOIN projects p ON t.project_id = p.id
        LEFT JOIN users u ON t.assignee_id = u.id
        WHERE t.due_date < CURDATE()
          AND t.status != 'DONE'
          AND (t.assignee_id = #{userId} OR p.owner_id = #{userId})
        ORDER BY t.due_date ASC
    """)
    List<Task> findOverdueTasks(Long userId);
}
```

### 2-2. TaskMapper XML（動的SQL）

**ファイルパス**: `src/main/resources/mapper/TaskMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
  PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.example.taskapp.mapper.TaskMapper">

    <!-- ResultMap定義 -->
    <resultMap id="TaskResultMap" type="com.example.taskapp.entity.Task">
        <id property="id" column="id"/>
        <result property="projectId" column="project_id"/>
        <result property="title" column="title"/>
        <result property="description" column="description"/>
        <result property="status" column="status" typeHandler="org.apache.ibatis.type.EnumTypeHandler"/>
        <result property="priority" column="priority" typeHandler="org.apache.ibatis.type.EnumTypeHandler"/>
        <result property="assigneeId" column="assignee_id"/>
        <result property="dueDate" column="due_date"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
        <result property="projectName" column="project_name"/>
        <result property="assigneeName" column="assignee_name"/>
    </resultMap>

    <!-- 動的検索クエリ -->
    <select id="search" resultMap="TaskResultMap">
        SELECT 
            t.*,
            p.name as project_name,
            u.username as assignee_name
        FROM tasks t
        LEFT JOIN projects p ON t.project_id = p.id
        LEFT JOIN users u ON t.assignee_id = u.id
        WHERE 1=1
        <if test="criteria.projectId != null">
            AND t.project_id = #{criteria.projectId}
        </if>
        <if test="criteria.status != null">
            AND t.status = #{criteria.status}
        </if>
        <if test="criteria.priority != null">
            AND t.priority = #{criteria.priority}
        </if>
        <if test="criteria.assigneeId != null">
            AND t.assignee_id = #{criteria.assigneeId}
        </if>
        <if test="criteria.keyword != null and criteria.keyword != ''">
            AND (
                t.title LIKE CONCAT('%', #{criteria.keyword}, '%')
                OR t.description LIKE CONCAT('%', #{criteria.keyword}, '%')
            )
        </if>
        <if test="criteria.dueDateFrom != null">
            AND t.due_date &gt;= #{criteria.dueDateFrom}
        </if>
        <if test="criteria.dueDateTo != null">
            AND t.due_date &lt;= #{criteria.dueDateTo}
        </if>
        ORDER BY 
        <choose>
            <when test="criteria.sortBy == 'priority'">
                CASE t.priority
                    WHEN 'HIGH' THEN 1
                    WHEN 'MEDIUM' THEN 2
                    WHEN 'LOW' THEN 3
                END,
                t.created_at DESC
            </when>
            <when test="criteria.sortBy == 'dueDate'">
                t.due_date ASC NULLS LAST,
                t.created_at DESC
            </when>
            <when test="criteria.sortBy == 'status'">
                CASE t.status
                    WHEN 'TODO' THEN 1
                    WHEN 'IN_PROGRESS' THEN 2
                    WHEN 'DONE' THEN 3
                END,
                t.created_at DESC
            </when>
            <otherwise>
                t.created_at DESC
            </otherwise>
        </choose>
        <if test="criteria.limit != null">
            LIMIT #{criteria.limit}
        </if>
        <if test="criteria.offset != null">
            OFFSET #{criteria.offset}
        </if>
    </select>

</mapper>
```

---

## 🚀 ステップ3: DTOクラスの実装

### 3-1. TaskSearchCriteria

**ファイルパス**: `src/main/java/com/example/taskapp/dto/TaskSearchCriteria.java`

```java
package com.example.taskapp.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class TaskSearchCriteria {
    private Long projectId;
    private String status;
    private String priority;
    private Long assigneeId;
    private String keyword;
    private LocalDate dueDateFrom;
    private LocalDate dueDateTo;
    private String sortBy = "createdAt";
    private Integer limit;
    private Integer offset;
}
```

### 3-2. TaskCreateRequest

```java
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
```

---

## 🚀 ステップ4: Service層の実装

### 4-1. TaskService

**ファイルパス**: `src/main/java/com/example/taskapp/service/TaskService.java`

```java
package com.example.taskapp.service;

import com.example.taskapp.entity.Task;
import com.example.taskapp.dto.TaskSearchCriteria;
import com.example.taskapp.dto.request.TaskCreateRequest;
import com.example.taskapp.mapper.TaskMapper;
import com.example.taskapp.mapper.ProjectMapper;
import com.example.taskapp.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskMapper taskMapper;
    private final ProjectMapper projectMapper;

    /**
     * タスク作成
     */
    @Transactional
    public Task createTask(TaskCreateRequest request) {
        log.info("タスクを作成します: {}", request.getTitle());
        
        // プロジェクトの存在確認
        projectMapper.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("プロジェクトが見つかりません"));
        
        Task task = new Task();
        task.setProjectId(request.getProjectId());
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(TaskStatus.valueOf(request.getStatus()));
        task.setPriority(Priority.valueOf(request.getPriority()));
        task.setAssigneeId(request.getAssigneeId());
        task.setDueDate(request.getDueDate());
        
        taskMapper.insert(task);
        log.info("タスクを作成しました: id={}", task.getId());
        
        return task;
    }

    /**
     * タスク取得
     */
    public Task getTaskById(Long id) {
        return taskMapper.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("タスクが見つかりません: id=" + id));
    }

    /**
     * プロジェクトのタスク一覧取得
     */
    public List<Task> getProjectTasks(Long projectId) {
        return taskMapper.findByProjectId(projectId);
    }

    /**
     * タスク検索
     */
    public List<Task> searchTasks(TaskSearchCriteria criteria) {
        log.info("タスクを検索します: {}", criteria);
        return taskMapper.search(criteria);
    }

    /**
     * タスク更新
     */
    @Transactional
    public Task updateTask(Long id, TaskCreateRequest request) {
        log.info("タスクを更新します: id={}", id);
        
        Task task = getTaskById(id);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(TaskStatus.valueOf(request.getStatus()));
        task.setPriority(Priority.valueOf(request.getPriority()));
        task.setAssigneeId(request.getAssigneeId());
        task.setDueDate(request.getDueDate());
        
        taskMapper.update(task);
        log.info("タスクを更新しました: id={}", id);
        
        return task;
    }

    /**
     * ステータス更新
     */
    @Transactional
    public void updateTaskStatus(Long id, String status) {
        log.info("タスクステータスを更新します: id={}, status={}", id, status);
        taskMapper.updateStatus(id, status);
    }

    /**
     * タスク削除
     */
    @Transactional
    public void deleteTask(Long id) {
        log.info("タスクを削除します: id={}", id);
        taskMapper.deleteById(id);
    }

    /**
     * 期限切れタスク取得
     */
    public List<Task> getOverdueTasks(Long userId) {
        return taskMapper.findOverdueTasks(userId);
    }
}
```

---

## 🚀 ステップ5: Thymeleafコントローラーの実装

### 5-1. TaskController（画面制御）

**ファイルパス**: `src/main/java/com/example/taskapp/controller/TaskController.java`

```java
package com.example.taskapp.controller;

import com.example.taskapp.entity.Task;
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
    public String listTasks(@RequestParam(required = false) Long projectId,
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
        
        return "tasks/list";  // templates/tasks/list.html
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
    public String createTask(@Valid @ModelAttribute("task") TaskCreateRequest request,
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
    public String updateTask(@PathVariable Long id,
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
     * ステータス更新（AJAX）
     */
    @PostMapping("/{id}/status")
    @ResponseBody
    public Map<String, Object> updateStatus(@PathVariable Long id,
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
```

### 5-2. ProjectController（プロジェクト管理）

**ファイルパス**: `src/main/java/com/example/taskapp/controller/ProjectController.java`

```java
package com.example.taskapp.controller;

import com.example.taskapp.entity.Project;
import com.example.taskapp.dto.request.ProjectCreateRequest;
import com.example.taskapp.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

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
        List<Task> tasks = taskService.getProjectTasks(id);
        
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
    public String createProject(@Valid @ModelAttribute("project") ProjectCreateRequest request,
                               BindingResult result,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            return "projects/form";
        }
        
        Project created = projectService.createProject(request, authentication.getName());
        redirectAttributes.addFlashAttribute("message", "プロジェクトを作成しました");
        
        return "redirect:/projects/" + created.getId();
        
        redirectAttributes.addFlashAttribute("message", "プロジェクトを作成しました");
        return "redirect:/projects/" + created.getId();
    }

    /**
     * プロジェクト詳細画面
     */
    @GetMapping("/{id}")
    public String projectDetail(@PathVariable Long id, Model model) {
        ProjectResponse project = projectService.getProjectById(id);
        List<TaskResponse> tasks = projectService.getProjectTasks(id);
        
        model.addAttribute("project", project);
        model.addAttribute("tasks", tasks);
        model.addAttribute("newTask", new TaskCreateRequest());
        
        return "projects/detail";
    }
}
```

---

## 🚀 ステップ6: Thymeleafテンプレートの実装

### 6-1. タスク一覧画面

**ファイルパス**: `src/main/resources/templates/tasks/list.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>タスク一覧 - Task Management</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
</head>
<body>
    <div th:replace="~{fragments/navbar :: navbar}"></div>
    
    <div class="container mt-4">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h1>タスク一覧</h1>
            <a th:href="@{/tasks/new(projectId=${criteria.projectId})}" class="btn btn-primary">
                <i class="bi bi-plus-circle"></i> 新規タスク
            </a>
        </div>
        
        <!-- 検索フォーム -->
        <div class="card mb-4">
            <div class="card-body">
                <form th:action="@{/tasks}" method="get" class="row g-3">
                    <div class="col-md-3">
                        <label class="form-label">プロジェクト</label>
                        <select name="projectId" class="form-select">
                            <option value="">すべて</option>
                            <option th:each="project : ${projects}" 
                                    th:value="${project.id}"
                                    th:text="${project.name}"
                                    th:selected="${criteria.projectId == project.id}"></option>
                        </select>
                    </div>
                    <div class="col-md-2">
                        <label class="form-label">ステータス</label>
                        <select name="status" class="form-select">
                            <option value="">すべて</option>
                            <option value="TODO" th:selected="${criteria.status == 'TODO'}">未着手</option>
                            <option value="IN_PROGRESS" th:selected="${criteria.status == 'IN_PROGRESS'}">進行中</option>
                            <option value="DONE" th:selected="${criteria.status == 'DONE'}">完了</option>
                        </select>
                    </div>
                    <div class="col-md-2">
                        <label class="form-label">優先度</label>
                        <select name="priority" class="form-select">
                            <option value="">すべて</option>
                            <option value="HIGH" th:selected="${criteria.priority == 'HIGH'}">高</option>
                            <option value="MEDIUM" th:selected="${criteria.priority == 'MEDIUM'}">中</option>
                            <option value="LOW" th:selected="${criteria.priority == 'LOW'}">低</option>
                        </select>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label">キーワード</label>
                        <input type="text" name="keyword" class="form-control" 
                               th:value="${criteria.keyword}" placeholder="タイトル・説明で検索">
                    </div>
                    <div class="col-md-2 d-flex align-items-end">
                        <button type="submit" class="btn btn-secondary w-100">検索</button>
                    </div>
                </form>
            </div>
        </div>
        
        <!-- タスク一覧 -->
        <div class="row">
            <div class="col-md-12" th:each="task : ${tasks}">
                <div class="card mb-3">
                    <div class="card-body">
                        <div class="d-flex justify-content-between">
                            <h5 class="card-title">
                                <a th:href="@{/tasks/{id}(id=${task.id})}" th:text="${task.title}"></a>
                            </h5>
                            <span class="badge" th:classappend="${task.priority.name() == 'HIGH' ? 'bg-danger' : 
                                                                   task.priority.name() == 'MEDIUM' ? 'bg-warning' : 'bg-secondary'}"
                                  th:text="${task.priority.displayName}"></span>
                        </div>
                        <p class="text-muted small">
                            <i class="bi bi-folder"></i> <span th:text="${task.projectName}"></span> |
                            <i class="bi bi-person"></i> <span th:text="${task.assigneeName ?: '未割当'}"></span>
                        </p>
                        <p class="card-text" th:text="${task.description}"></p>
                        <div class="d-flex justify-content-between align-items-center">
                            <span class="badge" th:classappend="${task.status.name() == 'TODO' ? 'bg-info' : 
                                                                  task.status.name() == 'IN_PROGRESS' ? 'bg-primary' : 'bg-success'}"
                                  th:text="${task.status.displayName}"></span>
                            <small class="text-muted">
                                期限: <span th:text="${task.dueDate != null ? #temporals.format(task.dueDate, 'yyyy/MM/dd') : '未設定'}"></span>
                            </small>
                        </div>
                    </div>
                </div>
            </div>
            
            <div class="col-12" th:if="${#lists.isEmpty(tasks)}">
                <div class="alert alert-info">タスクがありません</div>
            </div>
        </div>
    </div>
    
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
```

### 6-2. カンバンボード画面

**ファイルパス**: `src/main/resources/templates/tasks/kanban.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>カンバンボード - <span th:text="${project.name}"></span></title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        .kanban-column {
            min-height: 500px;
            background-color: #f8f9fa;
            border-radius: 8px;
            padding: 15px;
        }
        .kanban-card {
            cursor: move;
            margin-bottom: 10px;
            transition: transform 0.2s;
        }
        .kanban-card:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 8px rgba(0,0,0,0.1);
        }
    </style>
</head>
<body>
    <div th:replace="~{fragments/navbar :: navbar}"></div>
    
    <div class="container-fluid mt-4">
        <h2 th:text="${project.name}"></h2>
        <hr>
        
        <div class="row">
            <!-- TODO列 -->
            <div class="col-md-4">
                <h5 class="text-center mb-3">
                    <span class="badge bg-info">未着手</span>
                    <span class="badge bg-secondary" th:text="${#lists.size(todoTasks)}"></span>
                </h5>
                <div class="kanban-column" id="todo-column">
                    <div class="card kanban-card" th:each="task : ${todoTasks}" th:data-task-id="${task.id}">
                        <div class="card-body">
                            <h6 class="card-title" th:text="${task.title}"></h6>
                            <p class="card-text small" th:text="${task.description}"></p>
                            <div class="d-flex justify-content-between">
                                <span class="badge" th:classappend="${task.priority.color}" th:text="${task.priority.displayName}"></span>
                                <small class="text-muted" th:text="${task.assigneeName}"></small>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            <!-- IN_PROGRESS列 -->
            <div class="col-md-4">
                <h5 class="text-center mb-3">
                    <span class="badge bg-primary">進行中</span>
                    <span class="badge bg-secondary" th:text="${#lists.size(inProgressTasks)}"></span>
                </h5>
                <div class="kanban-column" id="in-progress-column">
                    <div class="card kanban-card" th:each="task : ${inProgressTasks}" th:data-task-id="${task.id}">
                        <div class="card-body">
                            <h6 class="card-title" th:text="${task.title}"></h6>
                            <p class="card-text small" th:text="${task.description}"></p>
                            <div class="d-flex justify-content-between">
                                <span class="badge" th:style="'background-color:' + ${task.priority.color}" th:text="${task.priority.displayName}"></span>
                                <small class="text-muted" th:text="${task.assigneeName}"></small>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            <!-- DONE列 -->
            <div class="col-md-4">
                <h5 class="text-center mb-3">
                    <span class="badge bg-success">完了</span>
                    <span class="badge bg-secondary" th:text="${#lists.size(doneTasks)}"></span>
                </h5>
                <div class="kanban-column" id="done-column">
                    <div class="card kanban-card" th:each="task : ${doneTasks}" th:data-task-id="${task.id}">
                        <div class="card-body">
                            <h6 class="card-title" th:text="${task.title}"></h6>
                            <p class="card-text small" th:text="${task.description}"></p>
                            <div class="d-flex justify-content-between">
                                <span class="badge" th:style="'background-color:' + ${task.priority.color}" th:text="${task.priority.displayName}"></span>
                                <small class="text-muted" th:text="${task.assigneeName}"></small>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/sortablejs@1.15.0/Sortable.min.js"></script>
    <script>
        // ドラッグ&ドロップ実装
        const columns = ['todo', 'in-progress', 'done'];
        const statusMap = {
            'todo': 'TODO',
            'in-progress': 'IN_PROGRESS',
            'done': 'DONE'
        };
        
        columns.forEach(columnId => {
            new Sortable(document.getElementById(columnId + '-column'), {
                group: 'kanban',
                animation: 150,
                onEnd: function(evt) {
                    const taskId = evt.item.dataset.taskId;
                    const newStatus = statusMap[evt.to.id.replace('-column', '')];
                    
                    // ステータス更新API呼び出し
                    fetch(`/tasks/${taskId}/status`, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded',
                        },
                        body: `status=${newStatus}`
                    })
                    .then(response => response.json())
                    .then(data => {
                        console.log('Status updated:', data);
                    })
                    .catch(error => {
                        console.error('Error:', error);
                        alert('ステータス更新に失敗しました');
                    });
                }
            });
        });
    </script>
</body>
</html>
```

---

## ✅ 動作確認

### 1. アプリケーション起動

```bash
./mvnw spring-boot:run
```

### 2. ログインしてタスク一覧にアクセス

```
http://localhost:8080/tasks
```

### 3. タスク作成

1. 「新規タスク」ボタンをクリック
2. フォームに入力して送信
3. タスク詳細画面にリダイレクト

### 4. カンバンボードで確認

```
http://localhost:8080/tasks/kanban?projectId=1
```

タスクをドラッグ&ドロップして、ステータスが更新されることを確認。

### 5. 検索機能の確認

- プロジェクト、ステータス、優先度でフィルタリング
- キーワード検索

---

## 💡 実装のポイント

### MyBatisの利点

1. **柔軟なSQL**: 動的SQLで複雑な検索条件に対応
2. **パフォーマンス**: 必要なカラムのみ取得、JOINの最適化
3. **学習曲線**: SQLの知識があれば習得が容易

### Thymeleafの利点

1. **サーバーサイドレンダリング**: SEO対応が容易
2. **Spring統合**: Spring Securityとシームレスに連携
3. **シンプル**: フロントエンドフレームワーク不要

### アーキテクチャのポイント

```
Controller (Thymeleaf)
   ↓
Service (ビジネスロジック)
   ↓
Mapper (MyBatis - データアクセス)
   ↓
Database (MySQL)
```

- **責務の分離**: 各層が明確な役割を持つ
- **テスタビリティ**: Service層を単体テスト可能
- **保守性**: SQL変更はXMLファイルで完結

---

## 🎓 チャレンジ課題

1. **ページネーション実装**: タスク一覧にページネーションを追加（MyBatis `LIMIT`/`OFFSET`使用）
2. **タグ機能**: タスクにタグを付けて、タグで絞り込み検索
3. **コメント機能**: タスクにコメントを投稿・表示
4. **統計ダッシュボード**: プロジェクトごとのタスク完了率をグラフ表示（Chart.js使用）
5. **メール通知**: タスク割り当て時に担当者にメール送信（`@Async`）

---

## 📖 次のステップ

**STEP_37**: 高度な機能実装（ダッシュボード、ファイルアップロード）でプロジェクトを完成させます。

**ポイント**: 
- ThymeleafとMyBatisを組み合わせることで、Spring Bootの全体像を理解できます
- Phase 3（MyBatis）、Phase 5（Thymeleaf）の知識が統合されます
- 実務でも通用するアーキテクチャパターンを学習できます
                <form th:action="@{/projects/{id}/tasks(id=${project.id})}" th:object="${newTask}" method="post">
                    <div class="mb-3">
                        <label for="title" class="form-label">タイトル</label>
                        <input type="text" class="form-control" id="title" th:field="*{title}" required>
                    </div>
                    <div class="mb-3">
                        <label for="description" class="form-label">説明</label>
                        <textarea class="form-control" id="description" th:field="*{description}" rows="3"></textarea>
                    </div>
                    <div class="row">
                        <div class="col-md-4">
                            <label for="priority" class="form-label">優先度</label>
                            <select class="form-select" id="priority" th:field="*{priority}">
                                <option th:each="p : ${T(com.example.hellospringboot.entity.Priority).values()}" 
                                        th:value="${p}" th:text="${p.name()}">HIGH</option>
                            </select>
                        </div>
                        <div class="col-md-4">
                            <label for="dueDate" class="form-label">期限</label>
                            <input type="date" class="form-control" id="dueDate" th:field="*{dueDate}">
                        </div>
                    </div>
                    <button type="submit" class="btn btn-primary mt-3">作成</button>
                </form>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
```

### アプローチ3: ハイブリッド（推奨：実務）

**構成**:
- 管理画面: Thymeleaf（社内ユーザー向け）
- 外部API: REST API（モバイルアプリ、外部連携）

**プロジェクト構造**:
```
src/main/java/com/example/hellospringboot/
├── controller/
│   ├── api/          ← REST APIコントローラー（このステップで実装）
│   └── web/          ← Thymeleafコントローラー（オプション）
├── service/          ← 共通のビジネスロジック
└── ...
```

> **💡 選択のポイント**:
> - **学習目的**: Thymeleafで完結させて、フルスタック開発を体験
> - **実務想定**: REST API + React/Vue で、モダンな開発を学ぶ
> - **段階的移行**: Thymeleafから始めて、後でREST API化
>
> Phase 5で学んだThymeleafの知識を活かして、自分のプロジェクトに最適なアプローチを選びましょう！

---

## 🎨 チャレンジ課題

### チャレンジ 1: プロジェクトメンバー権限チェック

プロジェクトメンバーのみがタスクを編集できるようにしてください。

**ヒント**: カスタムアノテーション`@ProjectMember`を作成

### チャレンジ 2: 楽観的ロック

エンティティに`@Version`を追加して楽観的ロックを実装してください。

### チャレンジ 3: 一括操作API

複数タスクのステータスを一括更新するAPIを実装してください。

---

## 📚 このステップで学んだこと

- ✅ DTOパターンの実装
- ✅ MapStructによる自動マッピング
- ✅ サービス層のビジネスロジック
- ✅ RESTful API設計
- ✅ トランザクション管理
- ✅ 非同期通知

---

## 🔄 Gitへのコミット

```bash
git add .
git commit -m "Step 36: サービスとAPI実装完了"
git push origin main
```

---

## ➡️ 次のステップ

次は[Step 37: 高度な機能実装](STEP_37.md)へ進みましょう！

---

お疲れさまでした！ 🎉
