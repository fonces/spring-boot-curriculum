# Step 31: 高度な機能実装

## 🎯 このステップの目標

- 統計・ダッシュボード機能を実装する
- 全文検索を実装する
- ファイル添付機能を追加する
- リマインダー機能を実装する

**所要時間**: 約2時間30分

---

## 🚀 ステップ1: 統計・ダッシュボード

### 1-1. ProjectStatisticsResponse

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/response/ProjectStatisticsResponse.java`

```java
package com.example.hellospringboot.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "プロジェクト統計レスポンス")
public class ProjectStatisticsResponse {

    @Schema(description = "プロジェクトID", example = "1")
    private Long projectId;

    @Schema(description = "プロジェクト名", example = "新規Webサイト開発")
    private String projectName;

    @Schema(description = "総タスク数", example = "25")
    private Long totalTasks;

    @Schema(description = "未着手タスク数", example = "10")
    private Long todoTasks;

    @Schema(description = "進行中タスク数", example = "8")
    private Long inProgressTasks;

    @Schema(description = "完了タスク数", example = "7")
    private Long doneTasks;

    @Schema(description = "進捗率（%）", example = "28.0")
    private Double completionRate;

    @Schema(description = "優先度別タスク数")
    private Map<String, Long> tasksByPriority;

    @Schema(description = "期限切れタスク数", example = "2")
    private Long overdueTasks;
}
```

### 1-2. StatisticsService

**ファイルパス**: `src/main/java/com/example/hellospringboot/service/StatisticsService.java`

```java
package com.example.hellospringboot.service;

import com.example.hellospringboot.dto.response.ProjectStatisticsResponse;
import com.example.hellospringboot.entity.Project;
import com.example.hellospringboot.entity.Task;
import com.example.hellospringboot.enums.Priority;
import com.example.hellospringboot.enums.TaskStatus;
import com.example.hellospringboot.exception.ResourceNotFoundException;
import com.example.hellospringboot.repository.ProjectRepository;
import com.example.hellospringboot.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    /**
     * プロジェクト統計を取得
     */
    @Cacheable(value = "statistics", key = "'project-' + #projectId")
    public ProjectStatisticsResponse getProjectStatistics(Long projectId) {
        log.info("プロジェクト統計を取得します: {}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("プロジェクト", projectId));

        // タスク統計をクエリ
        List<Object[]> stats = taskRepository.getTaskStatisticsByProject(projectId);
        
        Map<TaskStatus, Long> statusMap = new HashMap<>();
        for (Object[] row : stats) {
            TaskStatus status = (TaskStatus) row[0];
            Long count = (Long) row[1];
            statusMap.put(status, count);
        }

        long todoTasks = statusMap.getOrDefault(TaskStatus.TODO, 0L);
        long inProgressTasks = statusMap.getOrDefault(TaskStatus.IN_PROGRESS, 0L);
        long doneTasks = statusMap.getOrDefault(TaskStatus.DONE, 0L);
        long totalTasks = todoTasks + inProgressTasks + doneTasks;

        // 進捗率を計算
        double completionRate = totalTasks > 0 
                ? (doneTasks * 100.0 / totalTasks) 
                : 0.0;

        // 優先度別タスク数
        Map<String, Long> tasksByPriority = getTaskCountByPriority(projectId);

        // 期限切れタスク数
        long overdueTasks = countOverdueTasks(projectId);

        return ProjectStatisticsResponse.builder()
                .projectId(projectId)
                .projectName(project.getName())
                .totalTasks(totalTasks)
                .todoTasks(todoTasks)
                .inProgressTasks(inProgressTasks)
                .doneTasks(doneTasks)
                .completionRate(Math.round(completionRate * 10.0) / 10.0)
                .tasksByPriority(tasksByPriority)
                .overdueTasks(overdueTasks)
                .build();
    }

    /**
     * 優先度別タスク数を取得
     */
    private Map<String, Long> getTaskCountByPriority(Long projectId) {
        Map<String, Long> result = new HashMap<>();
        
        for (Priority priority : Priority.values()) {
            long count = taskRepository.searchTasks(projectId, null, priority, null, null, 
                    org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
            result.put(priority.name(), count);
        }
        
        return result;
    }

    /**
     * 期限切れタスク数をカウント
     */
    private long countOverdueTasks(Long projectId) {
        LocalDate today = LocalDate.now();
        List<Task> overdueTasks = taskRepository.findByDueDateBefore(today);
        return overdueTasks.stream()
                .filter(task -> task.getProject().getId().equals(projectId))
                .filter(task -> task.getStatus() != TaskStatus.DONE)
                .count();
    }
}
```

---

## 🚀 ステップ2: タスク添付ファイル

### 2-1. TaskAttachment エンティティ

**ファイルパス**: `src/main/java/com/example/hellospringboot/entity/TaskAttachment.java`

```java
package com.example.hellospringboot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private Long fileSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;
}
```

### 2-2. TaskAttachmentRepository

**ファイルパス**: `src/main/java/com/example/hellospringboot/repository/TaskAttachmentRepository.java`

```java
package com.example.hellospringboot.repository;

import com.example.hellospringboot.entity.TaskAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, Long> {

    /**
     * タスクIDで添付ファイルを検索
     */
    List<TaskAttachment> findByTaskId(Long taskId);
}
```

### 2-3. TaskAttachmentService

**ファイルパス**: `src/main/java/com/example/hellospringboot/service/TaskAttachmentService.java`

```java
package com.example.hellospringboot.service;

import com.example.hellospringboot.dto.response.UploadFileResponse;
import com.example.hellospringboot.entity.Task;
import com.example.hellospringboot.entity.TaskAttachment;
import com.example.hellospringboot.entity.User;
import com.example.hellospringboot.exception.ResourceNotFoundException;
import com.example.hellospringboot.repository.TaskAttachmentRepository;
import com.example.hellospringboot.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskAttachmentService {

    private final TaskAttachmentRepository attachmentRepository;
    private final TaskRepository taskRepository;
    private final FileStorageService fileStorageService;

    /**
     * ファイルをタスクに添付
     */
    @Transactional
    public UploadFileResponse attachFile(Long taskId, MultipartFile file, User currentUser) {
        log.info("タスクにファイルを添付します: taskId={}, filename={}", taskId, file.getOriginalFilename());

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("タスク", taskId));

        // ファイルを保存
        String filename = fileStorageService.storeFile(file);

        // 添付ファイル情報を保存
        TaskAttachment attachment = TaskAttachment.builder()
                .task(task)
                .filename(filename)
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedBy(currentUser)
                .build();

        attachmentRepository.save(attachment);

        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/tasks/")
                .path(taskId.toString())
                .path("/attachments/")
                .path(attachment.getId().toString())
                .toUriString();

        return UploadFileResponse.builder()
                .filename(filename)
                .fileDownloadUri(fileDownloadUri)
                .fileType(file.getContentType())
                .size(file.getSize())
                .build();
    }

    /**
     * タスクの添付ファイル一覧を取得
     */
    public List<UploadFileResponse> getAttachments(Long taskId) {
        return attachmentRepository.findByTaskId(taskId).stream()
                .map(attachment -> UploadFileResponse.builder()
                        .filename(attachment.getFilename())
                        .fileDownloadUri(ServletUriComponentsBuilder.fromCurrentContextPath()
                                .path("/api/tasks/")
                                .path(taskId.toString())
                                .path("/attachments/")
                                .path(attachment.getId().toString())
                                .toUriString())
                        .fileType(attachment.getContentType())
                        .size(attachment.getFileSize())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 添付ファイルを削除
     */
    @Transactional
    public void deleteAttachment(Long attachmentId) {
        TaskAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("添付ファイル", attachmentId));

        // ファイルを削除
        fileStorageService.deleteFile(attachment.getFilename());

        // レコードを削除
        attachmentRepository.delete(attachment);
    }
}
```

---

## 🚀 ステップ3: リマインダー機能

### 3-1. ReminderService

**ファイルパス**: `src/main/java/com/example/hellospringboot/service/ReminderService.java`

```java
package com.example.hellospringboot.service;

import com.example.hellospringboot.entity.Task;
import com.example.hellospringboot.enums.TaskStatus;
import com.example.hellospringboot.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final TaskRepository taskRepository;
    private final EmailService emailService;

    /**
     * 期限間近のタスクをリマインド（毎日午前9時）
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendDueDateReminders() {
        log.info("期限間近のタスクをリマインドします");

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Task> upcomingTasks = taskRepository.findByDueDateBefore(tomorrow).stream()
                .filter(task -> task.getStatus() != TaskStatus.DONE)
                .filter(task -> task.getAssignee() != null)
                .filter(task -> task.getDueDate() != null)
                .filter(task -> !task.getDueDate().isBefore(LocalDate.now()))
                .toList();

        for (Task task : upcomingTasks) {
            sendReminderEmail(task);
        }

        log.info("リマインダーを{}件送信しました", upcomingTasks.size());
    }

    /**
     * 期限切れタスクをリマインド（毎日午前10時）
     */
    @Scheduled(cron = "0 0 10 * * *")
    public void sendOverdueReminders() {
        log.info("期限切れタスクをリマインドします");

        LocalDate today = LocalDate.now();
        List<Task> overdueTasks = taskRepository.findByDueDateBefore(today).stream()
                .filter(task -> task.getStatus() != TaskStatus.DONE)
                .filter(task -> task.getAssignee() != null)
                .toList();

        for (Task task : overdueTasks) {
            sendOverdueEmail(task);
        }

        log.info("期限切れリマインダーを{}件送信しました", overdueTasks.size());
    }

    /**
     * リマインダーメールを送信
     */
    private void sendReminderEmail(Task task) {
        String to = task.getAssignee().getEmail();
        String subject = "【リマインダー】タスクの期限が近づいています";
        String text = String.format(
                "タスク「%s」の期限が近づいています。\n\n" +
                "プロジェクト: %s\n" +
                "期限: %s\n" +
                "優先度: %s",
                task.getTitle(),
                task.getProject().getName(),
                task.getDueDate(),
                task.getPriority().getDisplayName()
        );

        try {
            emailService.sendSimpleEmail(to, subject, text);
        } catch (Exception e) {
            log.error("リマインダーメールの送信に失敗しました: {}", e.getMessage());
        }
    }

    /**
     * 期限切れメールを送信
     */
    private void sendOverdueEmail(Task task) {
        String to = task.getAssignee().getEmail();
        String subject = "【重要】タスクの期限が過ぎています";
        String text = String.format(
                "タスク「%s」の期限が過ぎています。\n\n" +
                "プロジェクト: %s\n" +
                "期限: %s\n" +
                "優先度: %s\n\n" +
                "至急対応をお願いします。",
                task.getTitle(),
                task.getProject().getName(),
                task.getDueDate(),
                task.getPriority().getDisplayName()
        );

        try {
            emailService.sendSimpleEmail(to, subject, text);
        } catch (Exception e) {
            log.error("期限切れメールの送信に失敗しました: {}", e.getMessage());
        }
    }
}
```

---

## 🚀 ステップ4: StatisticsController

**ファイルパス**: `src/main/java/com/example/hellospringboot/controller/StatisticsController.java`

```java
package com.example.hellospringboot.controller;

import com.example.hellospringboot.dto.response.ProjectStatisticsResponse;
import com.example.hellospringboot.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Tag(name = "Statistics", description = "統計API")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "プロジェクト統計", description = "プロジェクトの統計情報を取得します")
    @GetMapping("/projects/{projectId}")
    public ResponseEntity<ProjectStatisticsResponse> getProjectStatistics(@PathVariable Long projectId) {
        ProjectStatisticsResponse statistics = statisticsService.getProjectStatistics(projectId);
        return ResponseEntity.ok(statistics);
    }
}
```

---

## ✅ 動作確認

### 統計情報取得

```bash
curl http://localhost:8080/api/statistics/projects/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**レスポンス例**:
```json
{
  "projectId": 1,
  "projectName": "新規Webサイト開発",
  "totalTasks": 25,
  "todoTasks": 10,
  "inProgressTasks": 8,
  "doneTasks": 7,
  "completionRate": 28.0,
  "tasksByPriority": {
    "HIGH": 8,
    "MEDIUM": 12,
    "LOW": 5
  },
  "overdueTasks": 2
}
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: ユーザーダッシュボード

ユーザーごとの統計（担当タスク数、完了率など）を表示します。

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/response/UserStatisticsResponse.java`

```java
package com.example.hellospringboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatisticsResponse {
    private Long userId;
    private String username;
    private Long totalAssignedTasks;
    private Long todoTasks;
    private Long inProgressTasks;
    private Long doneTasks;
    private Double completionRate;
    private Long overdueTasks;
    private Map<String, Long> tasksByPriority;
    private Map<String, Long> tasksByProject;
}
```

**ファイルパス**: `src/main/java/com/example/hellospringboot/repository/TaskRepository.java`（メソッド追加）

```java
@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, TaskRepositoryCustom {
    
    // 既存のメソッド...
    
    /**
     * ユーザーの担当タスク統計
     */
    @Query("SELECT t.status, COUNT(t) FROM Task t " +
           "WHERE t.assignee.id = :userId " +
           "GROUP BY t.status")
    List<Object[]> getUserTaskStatistics(@Param("userId") Long userId);
    
    /**
     * ユーザーの優先度別タスク数
     */
    @Query("SELECT t.priority, COUNT(t) FROM Task t " +
           "WHERE t.assignee.id = :userId " +
           "GROUP BY t.priority")
    List<Object[]> getUserTasksByPriority(@Param("userId") Long userId);
    
    /**
     * ユーザーのプロジェクト別タスク数
     */
    @Query("SELECT p.name, COUNT(t) FROM Task t " +
           "JOIN t.project p " +
           "WHERE t.assignee.id = :userId " +
           "GROUP BY p.id, p.name")
    List<Object[]> getUserTasksByProject(@Param("userId") Long userId);
    
    /**
     * ユーザーの期限切れタスク数
     */
    @Query("SELECT COUNT(t) FROM Task t " +
           "WHERE t.assignee.id = :userId " +
           "AND t.dueDate < CURRENT_DATE " +
           "AND t.status != 'DONE'")
    Long countOverdueTasksByUser(@Param("userId") Long userId);
}
```

**ファイルパス**: `src/main/java/com/example/hellospringboot/service/StatisticsService.java`（メソッド追加）

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    // 既存のメソッド...

    @Cacheable(value = "statistics", key = "'user-' + #userId")
    public UserStatisticsResponse getUserStatistics(Long userId) {
        // ステータス別集計
        List<Object[]> statusStats = taskRepository.getUserTaskStatistics(userId);
        Map<String, Long> statusMap = statusStats.stream()
                .collect(Collectors.toMap(
                        arr -> ((TaskStatus) arr[0]).name(),
                        arr -> (Long) arr[1]
                ));
        
        Long todoTasks = statusMap.getOrDefault("TODO", 0L);
        Long inProgressTasks = statusMap.getOrDefault("IN_PROGRESS", 0L);
        Long doneTasks = statusMap.getOrDefault("DONE", 0L);
        Long totalTasks = todoTasks + inProgressTasks + doneTasks;
        
        // 完了率計算
        Double completionRate = totalTasks > 0 
                ? (doneTasks.doubleValue() / totalTasks.doubleValue()) * 100 
                : 0.0;
        
        // 優先度別集計
        List<Object[]> priorityStats = taskRepository.getUserTasksByPriority(userId);
        Map<String, Long> priorityMap = priorityStats.stream()
                .collect(Collectors.toMap(
                        arr -> ((Priority) arr[0]).name(),
                        arr -> (Long) arr[1]
                ));
        
        // プロジェクト別集計
        List<Object[]> projectStats = taskRepository.getUserTasksByProject(userId);
        Map<String, Long> projectMap = projectStats.stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));
        
        // 期限切れタスク数
        Long overdueTasks = taskRepository.countOverdueTasksByUser(userId);
        
        return UserStatisticsResponse.builder()
                .userId(userId)
                .totalAssignedTasks(totalTasks)
                .todoTasks(todoTasks)
                .inProgressTasks(inProgressTasks)
                .doneTasks(doneTasks)
                .completionRate(Math.round(completionRate * 10.0) / 10.0)
                .overdueTasks(overdueTasks)
                .tasksByPriority(priorityMap)
                .tasksByProject(projectMap)
                .build();
    }
}
```

**ファイルパス**: `src/main/java/com/example/hellospringboot/controller/StatisticsController.java`（エンドポイント追加）

```java
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/projects/{projectId}")
    public ResponseEntity<ProjectStatisticsResponse> getProjectStatistics(@PathVariable Long projectId) {
        ProjectStatisticsResponse response = statisticsService.getProjectStatistics(projectId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserStatisticsResponse> getUserStatistics(@PathVariable Long userId) {
        UserStatisticsResponse response = statisticsService.getUserStatistics(userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/me")
    public ResponseEntity<UserStatisticsResponse> getMyStatistics(
            @AuthenticationPrincipal User currentUser) {
        UserStatisticsResponse response = statisticsService.getUserStatistics(currentUser.getId());
        return ResponseEntity.ok(response);
    }
}
```

### チャレンジ 2: Export機能

タスク一覧をCSVやExcelでエクスポートできるようにします。

**依存関係追加** (`pom.xml`):

```xml
<!-- Apache Commons CSV -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-csv</artifactId>
    <version>1.10.0</version>
</dependency>

<!-- Apache POI (Excel) -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.3</version>
</dependency>
```

**ファイルパス**: `src/main/java/com/example/hellospringboot/service/ExportService.java`

```java
package com.example.hellospringboot.service;

import com.example.hellospringboot.entity.Task;
import com.example.hellospringboot.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final TaskRepository taskRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String exportTasksToCsv(Long projectId) throws IOException {
        List<Task> tasks = taskRepository.findByProjectId(projectId, null).getContent();
        
        StringWriter writer = new StringWriter();
        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                .withHeader("ID", "タイトル", "説明", "ステータス", "優先度", "担当者", "期限", "作成日"));
        
        for (Task task : tasks) {
            csvPrinter.printRecord(
                    task.getId(),
                    task.getTitle(),
                    task.getDescription(),
                    task.getStatus().getDisplayName(),
                    task.getPriority().getDisplayName(),
                    task.getAssignee() != null ? task.getAssignee().getUsername() : "",
                    task.getDueDate() != null ? task.getDueDate().format(DATE_FORMATTER) : "",
                    task.getCreatedAt().format(DATETIME_FORMATTER)
            );
        }
        
        csvPrinter.flush();
        return writer.toString();
    }

    public byte[] exportTasksToExcel(Long projectId) throws IOException {
        List<Task> tasks = taskRepository.findByProjectId(projectId, null).getContent();
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("タスク一覧");
            
            // ヘッダースタイル
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            // ヘッダー行
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "タイトル", "説明", "ステータス", "優先度", "担当者", "期限", "作成日"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // データ行
            int rowNum = 1;
            for (Task task : tasks) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(task.getId());
                row.createCell(1).setCellValue(task.getTitle());
                row.createCell(2).setCellValue(task.getDescription() != null ? task.getDescription() : "");
                row.createCell(3).setCellValue(task.getStatus().getDisplayName());
                row.createCell(4).setCellValue(task.getPriority().getDisplayName());
                row.createCell(5).setCellValue(task.getAssignee() != null ? task.getAssignee().getUsername() : "");
                row.createCell(6).setCellValue(task.getDueDate() != null ? task.getDueDate().format(DATE_FORMATTER) : "");
                row.createCell(7).setCellValue(task.getCreatedAt().format(DATETIME_FORMATTER));
            }
            
            // 列幅自動調整
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
```

**ファイルパス**: `src/main/java/com/example/hellospringboot/controller/ExportController.java`

```java
package com.example.hellospringboot.controller;

import com.example.hellospringboot.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @GetMapping("/projects/{projectId}/tasks/csv")
    public ResponseEntity<byte[]> exportTasksToCsv(@PathVariable Long projectId) throws IOException {
        String csv = exportService.exportTasksToCsv(projectId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "tasks.csv");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/projects/{projectId}/tasks/excel")
    public ResponseEntity<byte[]> exportTasksToExcel(@PathVariable Long projectId) throws IOException {
        byte[] excel = exportService.exportTasksToExcel(projectId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "tasks.xlsx");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(excel);
    }
}
```

### チャレンジ 3: WebSocket通知

期限間近のタスクをリアルタイムで通知します。

**依存関係追加** (`pom.xml`):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

**ファイルパス**: `src/main/java/com/example/hellospringboot/config/WebSocketConfig.java`

```java
package com.example.hellospringboot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/notification/TaskNotification.java`

```java
package com.example.hellospringboot.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskNotification {
    private Long taskId;
    private String title;
    private String message;
    private String type; // "DUE_SOON", "OVERDUE", "ASSIGNED"
    private LocalDate dueDate;
}
```

**ファイルパス**: `src/main/java/com/example/hellospringboot/service/WebSocketNotificationService.java`

```java
package com.example.hellospringboot.service;

import com.example.hellospringboot.dto.notification.TaskNotification;
import com.example.hellospringboot.entity.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendTaskDueSoonNotification(Task task, String username) {
        TaskNotification notification = TaskNotification.builder()
                .taskId(task.getId())
                .title(task.getTitle())
                .message("タスクの期限が近づいています")
                .type("DUE_SOON")
                .dueDate(task.getDueDate())
                .build();
        
        messagingTemplate.convertAndSendToUser(
                username,
                "/topic/notifications",
                notification
        );
    }

    public void sendTaskOverdueNotification(Task task, String username) {
        TaskNotification notification = TaskNotification.builder()
                .taskId(task.getId())
                .title(task.getTitle())
                .message("タスクの期限が過ぎています")
                .type("OVERDUE")
                .dueDate(task.getDueDate())
                .build();
        
        messagingTemplate.convertAndSendToUser(
                username,
                "/topic/notifications",
                notification
        );
    }

    public void sendTaskAssignedNotification(Task task, String username) {
        TaskNotification notification = TaskNotification.builder()
                .taskId(task.getId())
                .title(task.getTitle())
                .message("新しいタスクが割り当てられました")
                .type("ASSIGNED")
                .dueDate(task.getDueDate())
                .build();
        
        messagingTemplate.convertAndSendToUser(
                username,
                "/topic/notifications",
                notification
        );
    }
}
```

**ReminderServiceの更新**:

```java
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final TaskRepository taskRepository;
    private final EmailService emailService;
    private final WebSocketNotificationService webSocketNotificationService;

    @Scheduled(cron = "0 0 9 * * *")
    @Async
    public void sendDueDateReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Task> tasks = taskRepository.findByDueDateAndStatusNot(tomorrow, TaskStatus.DONE);
        
        for (Task task : tasks) {
            if (task.getAssignee() != null) {
                User assignee = task.getAssignee();
                
                // メール送信
                emailService.sendEmail(
                        assignee.getEmail(),
                        "【リマインダー】タスクの期限が近づいています",
                        String.format("タスク「%s」の期限が明日です。\n\n" +
                                     "プロジェクト: %s\n" +
                                     "期限: %s\n" +
                                     "優先度: %s",
                                task.getTitle(),
                                task.getProject().getName(),
                                task.getDueDate(),
                                task.getPriority().getDisplayName())
                );
                
                // WebSocket通知
                webSocketNotificationService.sendTaskDueSoonNotification(task, assignee.getUsername());
            }
        }
    }
}
```

**フロントエンド接続例（JavaScript）**:

```javascript
// SockJS + STOMP接続
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
    
    // 通知を購読
    stompClient.subscribe('/user/topic/notifications', function(notification) {
        const data = JSON.parse(notification.body);
        showNotification(data);
    });
});

function showNotification(data) {
    // ブラウザ通知を表示
    if (Notification.permission === "granted") {
        new Notification(data.title, {
            body: data.message,
            icon: '/icon.png'
        });
    }
}
```

---

## 📚 このステップで学んだこと

- ✅ 統計情報の集計
- ✅ キャッシュの活用
- ✅ ファイル添付機能
- ✅ スケジュールタスク（リマインダー）
- ✅ 複雑なクエリの実装
