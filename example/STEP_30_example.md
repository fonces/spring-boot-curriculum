# Step 30: サービスとAPI実装

## 🎯 このステップの目標

- ビジネスロジックをサービス層に実装する
- REST APIコントローラーを作成する
- DTO（Data Transfer Object）を定義する
- MapStructでマッピングを自動化する

**所要時間**: 約3時間

---

## 🚀 ステップ1: DTOクラス

### 1-1. ProjectCreateRequest

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/request/ProjectCreateRequest.java`

```java
package com.example.hellospringboot.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "プロジェクト作成リクエスト")
public class ProjectCreateRequest {

    @Schema(description = "プロジェクト名", example = "新規Webサイト開発", required = true)
    @NotBlank(message = "プロジェクト名は必須です")
    @Size(min = 1, max = 100, message = "プロジェクト名は1文字以上100文字以内で入力してください")
    private String name;

    @Schema(description = "プロジェクト説明", example = "ECサイトの新規開発プロジェクト")
    @Size(max = 1000, message = "説明は1000文字以内で入力してください")
    private String description;
}
```

### 1-2. TaskCreateRequest

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/request/TaskCreateRequest.java`

```java
package com.example.hellospringboot.dto.request;

import com.example.hellospringboot.enums.Priority;
import com.example.hellospringboot.enums.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "タスク作成リクエスト")
public class TaskCreateRequest {

    @Schema(description = "タイトル", example = "ログイン画面の実装", required = true)
    @NotBlank(message = "タイトルは必須です")
    @Size(min = 1, max = 200, message = "タイトルは1文字以上200文字以内で入力してください")
    private String title;

    @Schema(description = "説明", example = "ユーザーログイン画面のフロントエンド実装")
    @Size(max = 2000, message = "説明は2000文字以内で入力してください")
    private String description;

    @Schema(description = "プロジェクトID", example = "1", required = true)
    @NotNull(message = "プロジェクトIDは必須です")
    private Long projectId;

    @Schema(description = "ステータス", example = "TODO")
    private TaskStatus status;

    @Schema(description = "優先度", example = "HIGH")
    private Priority priority;

    @Schema(description = "担当者ID", example = "2")
    private Long assigneeId;

    @Schema(description = "期限", example = "2025-12-31")
    private LocalDate dueDate;

    @Schema(description = "タグID一覧", example = "[1, 2, 3]")
    private Set<Long> tagIds;
}
```

### 1-3. TaskResponse

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/response/TaskResponse.java`

```java
package com.example.hellospringboot.dto.response;

import com.example.hellospringboot.enums.Priority;
import com.example.hellospringboot.enums.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "タスクレスポンス")
public class TaskResponse {

    @Schema(description = "タスクID", example = "1")
    private Long id;

    @Schema(description = "タイトル", example = "ログイン画面の実装")
    private String title;

    @Schema(description = "説明", example = "ユーザーログイン画面のフロントエンド実装")
    private String description;

    @Schema(description = "プロジェクトID", example = "1")
    private Long projectId;

    @Schema(description = "プロジェクト名", example = "新規Webサイト開発")
    private String projectName;

    @Schema(description = "ステータス", example = "IN_PROGRESS")
    private TaskStatus status;

    @Schema(description = "優先度", example = "HIGH")
    private Priority priority;

    @Schema(description = "担当者情報")
    private UserSummaryResponse assignee;

    @Schema(description = "期限", example = "2025-12-31")
    private LocalDate dueDate;

    @Schema(description = "タグ一覧")
    private Set<TagResponse> tags;

    @Schema(description = "作成日時", example = "2025-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "更新日時", example = "2025-01-01T15:30:00")
    private LocalDateTime updatedAt;
}
```

---

## 🚀 ステップ2: Mapper

### 2-1. TaskMapper

**ファイルパス**: `src/main/java/com/example/hellospringboot/mapper/TaskMapper.java`

```java
package com.example.hellospringboot.mapper;

import com.example.hellospringboot.dto.request.TaskCreateRequest;
import com.example.hellospringboot.dto.response.TaskResponse;
import com.example.hellospringboot.entity.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TaskMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Task toEntity(TaskCreateRequest request);

    @Mapping(source = "project.id", target = "projectId")
    @Mapping(source = "project.name", target = "projectName")
    TaskResponse toResponse(Task task);
}
```

---

## 🚀 ステップ3: サービス層

### 3-1. TaskService

**ファイルパス**: `src/main/java/com/example/hellospringboot/service/TaskService.java`

```java
package com.example.hellospringboot.service;

import com.example.hellospringboot.dto.request.TaskCreateRequest;
import com.example.hellospringboot.dto.request.TaskUpdateRequest;
import com.example.hellospringboot.dto.response.TaskResponse;
import com.example.hellospringboot.entity.Project;
import com.example.hellospringboot.entity.Tag;
import com.example.hellospringboot.entity.Task;
import com.example.hellospringboot.entity.User;
import com.example.hellospringboot.enums.Priority;
import com.example.hellospringboot.enums.TaskStatus;
import com.example.hellospringboot.exception.ResourceNotFoundException;
import com.example.hellospringboot.mapper.TaskMapper;
import com.example.hellospringboot.repository.ProjectRepository;
import com.example.hellospringboot.repository.TagRepository;
import com.example.hellospringboot.repository.TaskRepository;
import com.example.hellospringboot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final TaskMapper taskMapper;
    private final NotificationService notificationService;

    /**
     * タスク作成
     */
    @Transactional
    public TaskResponse createTask(TaskCreateRequest request) {
        log.info("タスク作成: {}", request.getTitle());

        // プロジェクトを取得
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("プロジェクト", request.getProjectId()));

        // エンティティに変換
        Task task = taskMapper.toEntity(request);
        task.setProject(project);

        // 担当者を設定
        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("ユーザー", request.getAssigneeId()));
            task.setAssignee(assignee);
        }

        // タグを設定
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            Set<Tag> tags = new HashSet<>(tagRepository.findAllById(request.getTagIds()));
            task.setTags(tags);
        }

        // 保存
        Task savedTask = taskRepository.save(task);

        // 担当者に通知
        if (savedTask.getAssignee() != null) {
            notificationService.notifyTaskAssignment(savedTask);
        }

        return taskMapper.toResponse(savedTask);
    }

    /**
     * タスク取得
     */
    public TaskResponse getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("タスク", id));
        return taskMapper.toResponse(task);
    }

    /**
     * プロジェクトのタスク一覧取得
     */
    public Page<TaskResponse> getTasksByProject(Long projectId, Pageable pageable) {
        return taskRepository.findByProjectId(projectId, pageable)
                .map(taskMapper::toResponse);
    }

    /**
     * タスク検索
     */
    public Page<TaskResponse> searchTasks(
            Long projectId,
            TaskStatus status,
            Priority priority,
            Long assigneeId,
            String keyword,
            Pageable pageable) {
        
        return taskRepository.searchTasks(projectId, status, priority, assigneeId, keyword, pageable)
                .map(taskMapper::toResponse);
    }

    /**
     * タスク更新
     */
    @Transactional
    public TaskResponse updateTask(Long id, TaskUpdateRequest request) {
        log.info("タスク更新: {}", id);

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("タスク", id));

        // 更新
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());

        // 担当者更新
        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("ユーザー", request.getAssigneeId()));
            task.setAssignee(assignee);
        }

        Task updatedTask = taskRepository.save(task);
        return taskMapper.toResponse(updatedTask);
    }

    /**
     * ステータス変更
     */
    @Transactional
    public TaskResponse updateStatus(Long id, TaskStatus status) {
        log.info("タスクステータス変更: {} -> {}", id, status);

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("タスク", id));
        
        task.setStatus(status);
        Task updatedTask = taskRepository.save(task);
        
        return taskMapper.toResponse(updatedTask);
    }

    /**
     * タスク削除
     */
    @Transactional
    public void deleteTask(Long id) {
        log.info("タスク削除: {}", id);

        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("タスク", id);
        }

        taskRepository.deleteById(id);
    }

    /**
     * タグ追加
     */
    @Transactional
    public TaskResponse addTag(Long taskId, Long tagId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("タスク", taskId));
        
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("タグ", tagId));

        task.getTags().add(tag);
        Task updatedTask = taskRepository.save(task);
        
        return taskMapper.toResponse(updatedTask);
    }

    /**
     * タグ削除
     */
    @Transactional
    public TaskResponse removeTag(Long taskId, Long tagId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("タスク", taskId));
        
        task.getTags().removeIf(tag -> tag.getId().equals(tagId));
        Task updatedTask = taskRepository.save(task);
        
        return taskMapper.toResponse(updatedTask);
    }
}
```

### 3-2. NotificationService

**ファイルパス**: `src/main/java/com/example/hellospringboot/service/NotificationService.java`

```java
package com.example.hellospringboot.service;

import com.example.hellospringboot.entity.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EmailService emailService;

    /**
     * タスク割り当て通知
     */
    @Async
    public void notifyTaskAssignment(Task task) {
        if (task.getAssignee() == null || task.getAssignee().getEmail() == null) {
            return;
        }

        String to = task.getAssignee().getEmail();
        String subject = "新しいタスクが割り当てられました";
        String text = String.format(
                "タスク「%s」が割り当てられました。\n\nプロジェクト: %s\n期限: %s",
                task.getTitle(),
                task.getProject().getName(),
                task.getDueDate() != null ? task.getDueDate().toString() : "未設定"
        );

        try {
            emailService.sendSimpleEmail(to, subject, text);
            log.info("タスク割り当て通知を送信しました: {}", to);
        } catch (Exception e) {
            log.error("タスク割り当て通知の送信に失敗しました: {}", e.getMessage());
        }
    }
}
```

---

## 🚀 ステップ4: コントローラー

### 4-1. TaskController

**ファイルパス**: `src/main/java/com/example/hellospringboot/controller/TaskController.java`

```java
package com.example.hellospringboot.controller;

import com.example.hellospringboot.dto.request.TaskCreateRequest;
import com.example.hellospringboot.dto.request.TaskUpdateRequest;
import com.example.hellospringboot.dto.response.TaskResponse;
import com.example.hellospringboot.enums.Priority;
import com.example.hellospringboot.enums.TaskStatus;
import com.example.hellospringboot.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Task", description = "タスク管理API")
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "タスク作成", description = "新しいタスクを作成します")
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskCreateRequest request) {
        TaskResponse response = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "タスク取得", description = "IDを指定してタスクを取得します")
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTask(@PathVariable Long id) {
        TaskResponse response = taskService.getTaskById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "タスク検索", description = "条件を指定してタスクを検索します")
    @GetMapping("/search")
    public ResponseEntity<Page<TaskResponse>> searchTasks(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<TaskResponse> tasks = taskService.searchTasks(
                projectId, status, priority, assigneeId, keyword, pageable);
        return ResponseEntity.ok(tasks);
    }

    @Operation(summary = "タスク更新", description = "タスクを更新します")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskUpdateRequest request) {
        TaskResponse response = taskService.updateTask(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "ステータス変更", description = "タスクのステータスを変更します")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TaskResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam TaskStatus status) {
        TaskResponse response = taskService.updateStatus(id, status);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "タスク削除", description = "タスクを削除します")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "タグ追加", description = "タスクにタグを追加します")
    @PostMapping("/{taskId}/tags/{tagId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TaskResponse> addTag(
            @PathVariable Long taskId,
            @PathVariable Long tagId) {
        TaskResponse response = taskService.addTag(taskId, tagId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "タグ削除", description = "タスクからタグを削除します")
    @DeleteMapping("/{taskId}/tags/{tagId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TaskResponse> removeTag(
            @PathVariable Long taskId,
            @PathVariable Long tagId) {
        TaskResponse response = taskService.removeTag(taskId, tagId);
        return ResponseEntity.ok(response);
    }
}
```

---

## ✅ 動作確認

### タスク作成

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "title": "ログイン画面の実装",
    "description": "ユーザーログイン画面のフロントエンド実装",
    "projectId": 1,
    "priority": "HIGH",
    "dueDate": "2025-12-31"
  }'
```

### タスク検索

```bash
curl "http://localhost:8080/api/tasks/search?projectId=1&status=TODO&page=0&size=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: ProjectService実装

ProjectServiceとProjectControllerを実装します。

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/request/ProjectCreateRequest.java`

```java
package com.example.hellospringboot.dto.request;

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
```

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/response/ProjectResponse.java`

```java
package com.example.hellospringboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private UserSummaryResponse owner;
    private Integer taskCount;
    private Integer memberCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**ファイルパス**: `src/main/java/com/example/hellospringboot/mapper/ProjectMapper.java`

```java
package com.example.hellospringboot.mapper;

import com.example.hellospringboot.dto.request.ProjectCreateRequest;
import com.example.hellospringboot.dto.response.ProjectResponse;
import com.example.hellospringboot.entity.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface ProjectMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tasks", ignore = true)
    @Mapping(target = "members", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "owner", ignore = true)
    Project toEntity(ProjectCreateRequest request);
    
    @Mapping(target = "taskCount", expression = "java(project.getTasks() != null ? project.getTasks().size() : 0)")
    @Mapping(target = "memberCount", expression = "java(project.getMembers() != null ? project.getMembers().size() : 0)")
    ProjectResponse toResponse(Project project);
}
```

**ファイルパス**: `src/main/java/com/example/hellospringboot/service/ProjectService.java`

```java
package com.example.hellospringboot.service;

import com.example.hellospringboot.dto.request.ProjectCreateRequest;
import com.example.hellospringboot.dto.response.ProjectResponse;
import com.example.hellospringboot.entity.Project;
import com.example.hellospringboot.entity.ProjectMember;
import com.example.hellospringboot.entity.User;
import com.example.hellospringboot.enums.ProjectRole;
import com.example.hellospringboot.exception.ResourceNotFoundException;
import com.example.hellospringboot.mapper.ProjectMapper;
import com.example.hellospringboot.repository.ProjectMemberRepository;
import com.example.hellospringboot.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMapper projectMapper;

    @Transactional
    public ProjectResponse createProject(ProjectCreateRequest request, User currentUser) {
        Project project = projectMapper.toEntity(request);
        project.setOwner(currentUser);
        
        Project savedProject = projectRepository.save(project);
        
        // オーナーをメンバーとして追加
        ProjectMember ownerMember = ProjectMember.builder()
                .project(savedProject)
                .user(currentUser)
                .role(ProjectRole.OWNER)
                .build();
        projectMemberRepository.save(ownerMember);
        
        return projectMapper.toResponse(savedProject);
    }

    public List<ProjectResponse> getMyProjects(User currentUser) {
        List<Project> projects = projectRepository.findProjectsByUserId(currentUser.getId());
        return projects.stream()
                .map(projectMapper::toResponse)
                .collect(Collectors.toList());
    }

    public ProjectResponse getProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        return projectMapper.toResponse(project);
    }

    @Transactional
    public ProjectResponse updateProject(Long projectId, ProjectCreateRequest request, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        
        // オーナーチェック
        if (!project.getOwner().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Only project owner can update");
        }
        
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        
        return projectMapper.toResponse(projectRepository.save(project));
    }

    @Transactional
    public void deleteProject(Long projectId, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        
        // オーナーチェック
        if (!project.getOwner().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Only project owner can delete");
        }
        
        projectRepository.delete(project);
    }

    @Transactional
    public void addMember(Long projectId, Long userId, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        
        // オーナーチェック
        if (!project.getOwner().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Only project owner can add members");
        }
        
        // 既に参加しているかチェック
        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new IllegalArgumentException("User is already a member");
        }
        
        User user = new User();
        user.setId(userId);
        
        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(user)
                .role(ProjectRole.MEMBER)
                .build();
        
        projectMemberRepository.save(member);
    }

    @Transactional
    public void removeMember(Long projectId, Long userId, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        
        // オーナーチェック
        if (!project.getOwner().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Only project owner can remove members");
        }
        
        // オーナーは削除できない
        if (project.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("Cannot remove project owner");
        }
        
        projectMemberRepository.deleteByProjectIdAndUserId(projectId, userId);
    }
}
```

**ファイルパス**: `src/main/java/com/example/hellospringboot/controller/ProjectController.java`

```java
package com.example.hellospringboot.controller;

import com.example.hellospringboot.dto.request.ProjectCreateRequest;
import com.example.hellospringboot.dto.response.ProjectResponse;
import com.example.hellospringboot.entity.User;
import com.example.hellospringboot.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody ProjectCreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        ProjectResponse response = projectService.createProject(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getMyProjects(
            @AuthenticationPrincipal User currentUser) {
        List<ProjectResponse> projects = projectService.getMyProjects(currentUser);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProject(@PathVariable Long id) {
        ProjectResponse response = projectService.getProject(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectCreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        ProjectResponse response = projectService.updateProject(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        projectService.deleteProject(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{projectId}/members/{userId}")
    public ResponseEntity<Void> addMember(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        projectService.addMember(projectId, userId, currentUser);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        projectService.removeMember(projectId, userId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
```

### チャレンジ 2: CommentService実装

CommentServiceとCommentControllerを実装します。

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/request/CommentCreateRequest.java`

```java
package com.example.hellospringboot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentCreateRequest {
    
    @NotBlank(message = "コメント内容は必須です")
    @Size(max = 5000, message = "コメントは5000文字以内で入力してください")
    private String content;
}
```

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/response/CommentResponse.java`

```java
package com.example.hellospringboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private String content;
    private UserSummaryResponse user;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**ファイルパス**: `src/main/java/com/example/hellospringboot/mapper/CommentMapper.java`

```java
package com.example.hellospringboot.mapper;

import com.example.hellospringboot.dto.request.CommentCreateRequest;
import com.example.hellospringboot.dto.response.CommentResponse;
import com.example.hellospringboot.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface CommentMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "task", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Comment toEntity(CommentCreateRequest request);
    
    CommentResponse toResponse(Comment comment);
}
```

**ファイルパス**: `src/main/java/com/example/hellospringboot/service/CommentService.java`

```java
package com.example.hellospringboot.service;

import com.example.hellospringboot.dto.request.CommentCreateRequest;
import com.example.hellospringboot.dto.response.CommentResponse;
import com.example.hellospringboot.entity.Comment;
import com.example.hellospringboot.entity.Task;
import com.example.hellospringboot.entity.User;
import com.example.hellospringboot.exception.ResourceNotFoundException;
import com.example.hellospringboot.mapper.CommentMapper;
import com.example.hellospringboot.repository.CommentRepository;
import com.example.hellospringboot.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final CommentMapper commentMapper;

    @Transactional
    public CommentResponse addComment(Long taskId, CommentCreateRequest request, User currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        
        Comment comment = commentMapper.toEntity(request);
        comment.setTask(task);
        comment.setUser(currentUser);
        
        Comment savedComment = commentRepository.save(comment);
        return commentMapper.toResponse(savedComment);
    }

    public List<CommentResponse> getComments(Long taskId) {
        List<Comment> comments = commentRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
        return comments.stream()
                .map(commentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentResponse updateComment(Long commentId, CommentCreateRequest request, User currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        
        // 作成者チェック
        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Only comment author can update");
        }
        
        comment.setContent(request.getContent());
        return commentMapper.toResponse(commentRepository.save(comment));
    }

    @Transactional
    public void deleteComment(Long commentId, User currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        
        // 作成者チェック
        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Only comment author can delete");
        }
        
        commentRepository.delete(comment);
    }
}
```

**ファイルパス**: `src/main/java/com/example/hellospringboot/controller/CommentController.java`

```java
package com.example.hellospringboot.controller;

import com.example.hellospringboot.dto.request.CommentCreateRequest;
import com.example.hellospringboot.dto.response.CommentResponse;
import com.example.hellospringboot.entity.User;
import com.example.hellospringboot.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks/{taskId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long taskId,
            @Valid @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        CommentResponse response = commentService.addComment(taskId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long taskId) {
        List<CommentResponse> comments = commentService.getComments(taskId);
        return ResponseEntity.ok(comments);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long taskId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        CommentResponse response = commentService.updateComment(commentId, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long taskId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal User currentUser) {
        commentService.deleteComment(commentId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
```

### チャレンジ 3: 権限チェック

プロジェクトメンバーのみがタスクを編集できるようにします。

**ファイルパス**: `src/main/java/com/example/hellospringboot/security/ProjectPermissionEvaluator.java`

```java
package com.example.hellospringboot.security;

import com.example.hellospringboot.entity.Project;
import com.example.hellospringboot.entity.Task;
import com.example.hellospringboot.entity.User;
import com.example.hellospringboot.repository.ProjectMemberRepository;
import com.example.hellospringboot.repository.ProjectRepository;
import com.example.hellospringboot.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectPermissionEvaluator {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;

    public boolean isProjectMember(Long projectId, User user) {
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            return false;
        }
        
        // オーナーまたはメンバーかチェック
        return project.getOwner().getId().equals(user.getId()) ||
               projectMemberRepository.existsByProjectIdAndUserId(projectId, user.getId());
    }

    public boolean canAccessTask(Long taskId, User user) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return false;
        }
        
        return isProjectMember(task.getProject().getId(), user);
    }

    public boolean isProjectOwner(Long projectId, User user) {
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            return false;
        }
        
        return project.getOwner().getId().equals(user.getId());
    }
}
```

**TaskServiceに権限チェックを追加**:

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TaskMapper taskMapper;
    private final NotificationService notificationService;
    private final ProjectPermissionEvaluator permissionEvaluator;

    @Transactional
    public TaskResponse updateTask(Long taskId, TaskUpdateRequest request, User currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        
        // 権限チェック
        if (!permissionEvaluator.canAccessTask(taskId, currentUser)) {
            throw new IllegalArgumentException("You don't have permission to access this task");
        }
        
        // 更新処理...
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        
        if (request.getAssigneeId() != null) {
            User assignee = new User();
            assignee.setId(request.getAssigneeId());
            task.setAssignee(assignee);
            
            notificationService.sendTaskAssignedEmail(assignee, task);
        }
        
        Task updatedTask = taskRepository.save(task);
        return taskMapper.toResponse(updatedTask);
    }

    @Transactional
    public void deleteTask(Long taskId, User currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        
        // 権限チェック
        if (!permissionEvaluator.canAccessTask(taskId, currentUser)) {
            throw new IllegalArgumentException("You don't have permission to delete this task");
        }
        
        taskRepository.delete(task);
    }
}
```

---

## 📚 このステップで学んだこと

- ✅ 複雑なビジネスロジック実装
- ✅ DTOパターン
- ✅ MapStructによるマッピング
- ✅ トランザクション管理
- ✅ ページング・ソート
