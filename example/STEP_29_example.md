# Step 29: エンティティとリポジトリ実装

## 🎯 このステップの目標

- 全エンティティクラスを実装する
- Enumクラスを定義する
- リポジトリインターフェースを作成する
- カスタムクエリメソッドを実装する

**所要時間**: 約2時間30分

---

## 🚀 ステップ1: Enumクラス

### 1-1. TaskStatus

**ファイルパス**: `src/main/java/com/example/hellospringboot/enums/TaskStatus.java`

```java
package com.example.hellospringboot.enums;

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

### 1-2. Priority

**ファイルパス**: `src/main/java/com/example/hellospringboot/enums/Priority.java`

```java
package com.example.hellospringboot.enums;

public enum Priority {
    LOW("低"),
    MEDIUM("中"),
    HIGH("高");

    private final String displayName;

    Priority(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

### 1-3. ProjectRole

**ファイルパス**: `src/main/java/com/example/hellospringboot/enums/ProjectRole.java`

```java
package com.example.hellospringboot.enums;

public enum ProjectRole {
    OWNER("オーナー"),
    MEMBER("メンバー");

    private final String displayName;

    ProjectRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

---

## 🚀 ステップ2: エンティティクラス

### 2-1. Project

**ファイルパス**: `src/main/java/com/example/hellospringboot/entity/Project.java`

```java
package com.example.hellospringboot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Task> tasks = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProjectMember> members = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

### 2-2. ProjectMember

**ファイルパス**: `src/main/java/com/example/hellospringboot/entity/ProjectMember.java`

```java
package com.example.hellospringboot.entity;

import com.example.hellospringboot.enums.ProjectRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_members",
       uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectRole role;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;
}
```

### 2-3. Task

**ファイルパス**: `src/main/java/com/example/hellospringboot/entity/Task.java`

```java
package com.example.hellospringboot.entity;

import com.example.hellospringboot.enums.Priority;
import com.example.hellospringboot.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_task_status", columnList = "status"),
        @Index(name = "idx_task_priority", columnList = "priority"),
        @Index(name = "idx_task_due_date", columnList = "due_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TaskStatus status = TaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "task_tags",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

### 2-4. Comment

**ファイルパス**: `src/main/java/com/example/hellospringboot/entity/Comment.java`

```java
package com.example.hellospringboot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

### 2-5. Tag

**ファイルパス**: `src/main/java/com/example/hellospringboot/entity/Tag.java`

```java
package com.example.hellospringboot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 7)
    private String color;  // HEX color code (例: #FF5733)

    @ManyToMany(mappedBy = "tags")
    @Builder.Default
    private Set<Task> tasks = new HashSet<>();
}
```

---

## 🚀 ステップ3: リポジトリ

### 3-1. ProjectRepository

**ファイルパス**: `src/main/java/com/example/hellospringboot/repository/ProjectRepository.java`

```java
package com.example.hellospringboot.repository;

import com.example.hellospringboot.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * オーナーIDでプロジェクトを検索
     */
    List<Project> findByOwnerId(Long ownerId);

    /**
     * ユーザーが参加しているプロジェクトを取得
     */
    @Query("SELECT DISTINCT p FROM Project p " +
           "LEFT JOIN p.members m " +
           "WHERE p.owner.id = :userId OR m.user.id = :userId")
    List<Project> findProjectsByUserId(@Param("userId") Long userId);

    /**
     * プロジェクト名で検索（部分一致）
     */
    List<Project> findByNameContainingIgnoreCase(String name);
}
```

### 3-2. TaskRepository

**ファイルパス**: `src/main/java/com/example/hellospringboot/repository/TaskRepository.java`

```java
package com.example.hellospringboot.repository;

import com.example.hellospringboot.entity.Task;
import com.example.hellospringboot.enums.Priority;
import com.example.hellospringboot.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * プロジェクトIDでタスクを検索
     */
    Page<Task> findByProjectId(Long projectId, Pageable pageable);

    /**
     * 担当者IDでタスクを検索
     */
    Page<Task> findByAssigneeId(Long assigneeId, Pageable pageable);

    /**
     * ステータスでタスクを検索
     */
    List<Task> findByStatus(TaskStatus status);

    /**
     * 優先度でタスクを検索
     */
    List<Task> findByPriority(Priority priority);

    /**
     * 期限が指定日以前のタスクを検索
     */
    List<Task> findByDueDateBefore(LocalDate date);

    /**
     * 複合条件検索
     */
    @Query("SELECT t FROM Task t " +
           "WHERE (:projectId IS NULL OR t.project.id = :projectId) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:priority IS NULL OR t.priority = :priority) " +
           "AND (:assigneeId IS NULL OR t.assignee.id = :assigneeId) " +
           "AND (:keyword IS NULL OR t.title LIKE %:keyword% OR t.description LIKE %:keyword%)")
    Page<Task> searchTasks(
            @Param("projectId") Long projectId,
            @Param("status") TaskStatus status,
            @Param("priority") Priority priority,
            @Param("assigneeId") Long assigneeId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /**
     * プロジェクトのタスク統計
     */
    @Query("SELECT t.status, COUNT(t) FROM Task t " +
           "WHERE t.project.id = :projectId " +
           "GROUP BY t.status")
    List<Object[]> getTaskStatisticsByProject(@Param("projectId") Long projectId);
}
```

### 3-3. CommentRepository

**ファイルパス**: `src/main/java/com/example/hellospringboot/repository/CommentRepository.java`

```java
package com.example.hellospringboot.repository;

import com.example.hellospringboot.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * タスクIDでコメントを検索
     */
    List<Comment> findByTaskIdOrderByCreatedAtDesc(Long taskId);

    /**
     * ユーザーIDでコメントを検索
     */
    List<Comment> findByUserId(Long userId);
}
```

### 3-4. TagRepository

**ファイルパス**: `src/main/java/com/example/hellospringboot/repository/TagRepository.java`

```java
package com.example.hellospringboot.repository;

import com.example.hellospringboot.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    /**
     * タグ名で検索
     */
    Optional<Tag> findByName(String name);

    /**
     * タグ名の存在確認
     */
    boolean existsByName(String name);
}
```

### 3-5. ProjectMemberRepository

**ファイルパス**: `src/main/java/com/example/hellospringboot/repository/ProjectMemberRepository.java`

```java
package com.example.hellospringboot.repository;

import com.example.hellospringboot.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    /**
     * プロジェクトIDでメンバーを検索
     */
    List<ProjectMember> findByProjectId(Long projectId);

    /**
     * プロジェクトとユーザーでメンバーを検索
     */
    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    /**
     * プロジェクトとユーザーの組み合わせが存在するかチェック
     */
    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    /**
     * プロジェクトとユーザーでメンバーを削除
     */
    void deleteByProjectIdAndUserId(Long projectId, Long userId);
}
```

---

## ✅ 動作確認

### データベーステーブルの確認

アプリケーションを起動して、テーブルが作成されることを確認します。

```bash
./mvnw spring-boot:run
```

H2コンソールにアクセス:
```
http://localhost:8080/h2-console
```

確認するテーブル:
- projects
- project_members
- tasks
- comments
- tags
- task_tags

---

## 🎨 チャレンジ課題

### チャレンジ 1: 監査フィールド

`@EntityListeners`と`@MappedSuperclass`を使って共通の監査フィールドを抽出します。

**ファイルパス**: `src/main/java/com/example/hellospringboot/entity/BaseEntity.java`

```java
package com.example.hellospringboot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    private String lastModifiedBy;
}
```

**AuditorAwareの実装**:

**ファイルパス**: `src/main/java/com/example/hellospringboot/config/AuditorAwareImpl.java`

```java
package com.example.hellospringboot.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of("system");
        }
        
        return Optional.of(authentication.getName());
    }
}
```

**JPA設定に追加**:

**ファイルパス**: `src/main/java/com/example/hellospringboot/config/JpaConfig.java`

```java
package com.example.hellospringboot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
```

**エンティティの修正例**:

```java
@Entity
@Table(name = "tasks")
public class Task extends BaseEntity {
    // createdAt, updatedAt, createdBy, lastModifiedBy は継承される
    // その他のフィールド...
}
```

### チャレンジ 2: ソフトデリート

物理削除ではなく論理削除を実装します。

**ベースエンティティに削除フラグを追加**:

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Where(clause = "deleted = false")  // Hibernate拡張
public abstract class BaseEntity {

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean deleted = false;

    private LocalDateTime deletedAt;
}
```

**ソフトデリートメソッドの実装**:

**ファイルパス**: `src/main/java/com/example/hellospringboot/service/TaskService.java`

```java
@Transactional
public void deleteTask(Long taskId) {
    Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    
    // 物理削除の代わりにソフトデリート
    task.setDeleted(true);
    task.setDeletedAt(LocalDateTime.now());
    taskRepository.save(task);
}
```

**リポジトリでのクエリ**:

```java
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    // ソフトデリートされていないタスクのみ取得
    @Query("SELECT t FROM Task t WHERE t.deleted = false")
    List<Task> findAllActive();
    
    // 削除済みタスクも含めて取得
    @Query("SELECT t FROM Task t")
    @QueryHints(@QueryHint(name = org.hibernate.annotations.QueryHints.SOFT_DELETE, value = "false"))
    List<Task> findAllIncludingDeleted();
}
```

### チャレンジ 3: カスタムクエリ

Criteria APIを使った動的検索を実装します。

**ファイルパス**: `src/main/java/com/example/hellospringboot/repository/TaskRepositoryCustom.java`

```java
package com.example.hellospringboot.repository;

import com.example.hellospringboot.entity.Task;
import com.example.hellospringboot.enums.Priority;
import com.example.hellospringboot.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface TaskRepositoryCustom {
    Page<Task> searchTasksDynamically(
            Long projectId,
            TaskStatus status,
            Priority priority,
            Long assigneeId,
            String keyword,
            LocalDate dueDateFrom,
            LocalDate dueDateTo,
            Pageable pageable
    );
}
```

**ファイルパス**: `src/main/java/com/example/hellospringboot/repository/TaskRepositoryCustomImpl.java`

```java
package com.example.hellospringboot.repository;

import com.example.hellospringboot.entity.Task;
import com.example.hellospringboot.enums.Priority;
import com.example.hellospringboot.enums.TaskStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TaskRepositoryCustomImpl implements TaskRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Task> searchTasksDynamically(
            Long projectId,
            TaskStatus status,
            Priority priority,
            Long assigneeId,
            String keyword,
            LocalDate dueDateFrom,
            LocalDate dueDateTo,
            Pageable pageable
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Task> query = cb.createQuery(Task.class);
        Root<Task> task = query.from(Task.class);

        // 動的に条件を追加
        List<Predicate> predicates = new ArrayList<>();

        if (projectId != null) {
            predicates.add(cb.equal(task.get("project").get("id"), projectId));
        }

        if (status != null) {
            predicates.add(cb.equal(task.get("status"), status));
        }

        if (priority != null) {
            predicates.add(cb.equal(task.get("priority"), priority));
        }

        if (assigneeId != null) {
            predicates.add(cb.equal(task.get("assignee").get("id"), assigneeId));
        }

        if (keyword != null && !keyword.isEmpty()) {
            String pattern = "%" + keyword + "%";
            Predicate titleMatch = cb.like(task.get("title"), pattern);
            Predicate descMatch = cb.like(task.get("description"), pattern);
            predicates.add(cb.or(titleMatch, descMatch));
        }

        if (dueDateFrom != null) {
            predicates.add(cb.greaterThanOrEqualTo(task.get("dueDate"), dueDateFrom));
        }

        if (dueDateTo != null) {
            predicates.add(cb.lessThanOrEqualTo(task.get("dueDate"), dueDateTo));
        }

        // 全条件をANDで結合
        query.where(cb.and(predicates.toArray(new Predicate[0])));

        // ソート処理
        if (pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            pageable.getSort().forEach(order -> {
                if (order.isAscending()) {
                    orders.add(cb.asc(task.get(order.getProperty())));
                } else {
                    orders.add(cb.desc(task.get(order.getProperty())));
                }
            });
            query.orderBy(orders);
        }

        // 結果の取得
        List<Task> results = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        // 総件数の取得
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Task> countRoot = countQuery.from(Task.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(cb.and(predicates.toArray(new Predicate[0])));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }
}
```

**TaskRepositoryに継承を追加**:

```java
@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, TaskRepositoryCustom {
    // 既存のメソッド...
}
```

**使用例**:

```java
@Service
public class TaskService {
    
    @Autowired
    private TaskRepository taskRepository;
    
    public Page<Task> searchTasks(TaskSearchCriteria criteria, Pageable pageable) {
        return taskRepository.searchTasksDynamically(
            criteria.getProjectId(),
            criteria.getStatus(),
            criteria.getPriority(),
            criteria.getAssigneeId(),
            criteria.getKeyword(),
            criteria.getDueDateFrom(),
            criteria.getDueDateTo(),
            pageable
        );
    }
}
```

---

## 📚 このステップで学んだこと

- ✅ 複雑なエンティティ設計
- ✅ Enumの活用
- ✅ 多対多リレーションシップ
- ✅ カスタムクエリメソッド
- ✅ インデックスの設定
