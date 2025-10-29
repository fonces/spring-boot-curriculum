# Step 34: プロジェクト設計

## 🎯 このステップの目標

- タスク管理システムの要件を定義する
- ER図とAPI設計を元に開発する
- プロジェクト構造を整理する

**所要時間**: 約1時間30分

---

## 📋 要件定義

### 機能要件

#### 1. ユーザー管理
- ユーザー登録・ログイン（JWT認証）
- プロフィール編集
- パスワード変更

#### 2. プロジェクト管理
- プロジェクト作成・編集・削除
- プロジェクトメンバーの追加・削除
- プロジェクト一覧・詳細

#### 3. タスク管理
- タスク作成・編集・削除
- タスクのステータス管理（TODO, IN_PROGRESS, DONE）
- 優先度設定（HIGH, MEDIUM, LOW）
- 期限設定
- 担当者割り当て
- タグ付け

#### 4. コメント機能
- タスクへのコメント追加
- コメント編集・削除

#### 5. 通知機能
- タスク割り当て時にメール通知
- 期限間近のタスクをリマインド

#### 6. 検索・フィルタ機能
- タスク検索（キーワード、ステータス、優先度、担当者）
- ソート（作成日、期限、優先度）
- ページング

---

## 🗂️ ER図（Entity Relationship Diagram）

```
┌─────────────┐
│    User     │
├─────────────┤
│ id          │ PK
│ username    │
│ email       │ UNIQUE
│ password    │
│ role        │ (ADMIN, USER)
│ createdAt   │
│ updatedAt   │
└─────────────┘
       │
       │ 1:N
       ▼
┌─────────────────┐
│ ProjectMember   │
├─────────────────┤
│ id              │ PK
│ projectId       │ FK
│ userId          │ FK
│ role            │ (OWNER, MEMBER)
│ joinedAt        │
└─────────────────┘
       │
       │ N:1
       ▼
┌─────────────┐
│   Project   │
├─────────────┤
│ id          │ PK
│ name        │
│ description │
│ ownerId     │ FK → User
│ createdAt   │
│ updatedAt   │
└─────────────┘
       │
       │ 1:N
       ▼
┌─────────────┐
│    Task     │
├─────────────┤
│ id          │ PK
│ projectId   │ FK → Project
│ title       │
│ description │
│ status      │ ENUM (TODO, IN_PROGRESS, DONE)
│ priority    │ ENUM (LOW, MEDIUM, HIGH)
│ assigneeId  │ FK → User
│ dueDate     │
│ createdAt   │
│ updatedAt   │
└─────────────┘
       │                   ┌─────────────┐
       │ 1:N               │     Tag     │
       ▼                   ├─────────────┤
┌─────────────┐            │ id          │ PK
│   Comment   │            │ name        │ UNIQUE
├─────────────┤            │ color       │
│ id          │ PK         └─────────────┘
│ taskId      │ FK                │
│ userId      │ FK                │ N:N
│ content     │                   ▼
│ createdAt   │            ┌─────────────┐
│ updatedAt   │            │  TaskTag    │
└─────────────┘            ├─────────────┤
                           │ taskId      │ FK
                           │ tagId       │ FK
                           └─────────────┘
```

---

## 🔌 API設計

### 認証API

| メソッド | パス | 説明 |
|---------|------|------|
| POST | `/api/auth/register` | ユーザー登録 |
| POST | `/api/auth/login` | ログイン |
| GET | `/api/auth/me` | 現在のユーザー情報 |

### プロジェクトAPI

| メソッド | パス | 説明 |
|---------|------|------|
| GET | `/api/projects` | プロジェクト一覧 |
| POST | `/api/projects` | プロジェクト作成 |
| GET | `/api/projects/{id}` | プロジェクト詳細 |
| PUT | `/api/projects/{id}` | プロジェクト更新 |
| DELETE | `/api/projects/{id}` | プロジェクト削除 |
| POST | `/api/projects/{id}/members` | メンバー追加 |
| DELETE | `/api/projects/{id}/members/{userId}` | メンバー削除 |

### タスクAPI

| メソッド | パス | 説明 |
|---------|------|------|
| GET | `/api/projects/{projectId}/tasks` | タスク一覧 |
| POST | `/api/projects/{projectId}/tasks` | タスク作成 |
| GET | `/api/tasks/{id}` | タスク詳細 |
| PUT | `/api/tasks/{id}` | タスク更新 |
| DELETE | `/api/tasks/{id}` | タスク削除 |
| PATCH | `/api/tasks/{id}/status` | ステータス変更 |
| PATCH | `/api/tasks/{id}/assign` | 担当者割り当て |
| GET | `/api/tasks/search` | タスク検索 |

### コメントAPI

| メソッド | パス | 説明 |
|---------|------|------|
| GET | `/api/tasks/{taskId}/comments` | コメント一覧 |
| POST | `/api/tasks/{taskId}/comments` | コメント作成 |
| PUT | `/api/comments/{id}` | コメント更新 |
| DELETE | `/api/comments/{id}` | コメント削除 |

### タグAPI

| メソッド | パス | 説明 |
|---------|------|------|
| GET | `/api/tags` | タグ一覧 |
| POST | `/api/tags` | タグ作成 |
| POST | `/api/tasks/{taskId}/tags/{tagId}` | タグ追加 |
| DELETE | `/api/tasks/{taskId}/tags/{tagId}` | タグ削除 |

---

## 📁 プロジェクト構造

```
src/main/java/com/example/hellospringboot/
├── config/
│   ├── AsyncConfig.java
│   ├── CacheConfig.java
│   ├── OpenAPIConfig.java
│   └── SecurityConfig.java
├── controller/
│   ├── AuthController.java
│   ├── ProjectController.java
│   ├── TaskController.java
│   ├── CommentController.java
│   └── TagController.java
├── dto/
│   ├── request/
│   │   ├── ProjectCreateRequest.java
│   │   ├── TaskCreateRequest.java
│   │   ├── TaskUpdateRequest.java
│   │   ├── CommentCreateRequest.java
│   │   └── ...
│   └── response/
│       ├── ProjectResponse.java
│       ├── TaskResponse.java
│       ├── CommentResponse.java
│       └── ...
├── entity/
│   ├── User.java
│   ├── Project.java
│   ├── ProjectMember.java
│   ├── Task.java
│   ├── Comment.java
│   ├── Tag.java
│   └── TaskTag.java
├── enums/
│   ├── TaskStatus.java
│   ├── Priority.java
│   └── ProjectRole.java
├── exception/
│   ├── BusinessException.java
│   ├── ResourceNotFoundException.java
│   └── ...
├── mapper/
│   ├── ProjectMapper.java
│   ├── TaskMapper.java
│   └── ...
├── repository/
│   ├── UserRepository.java
│   ├── ProjectRepository.java
│   ├── TaskRepository.java
│   ├── CommentRepository.java
│   └── TagRepository.java
├── service/
│   ├── AuthService.java
│   ├── ProjectService.java
│   ├── TaskService.java
│   ├── CommentService.java
│   ├── TagService.java
│   └── NotificationService.java
└── security/
    ├── JwtUtil.java
    ├── JwtAuthenticationFilter.java
    └── CustomUserDetailsService.java
```

---

## 💡 補足: Data Access層の技術選択

Phase 3でMyBatisを、Phase 2でJPAを学習してきました。最終プロジェクトでは、**用途に応じて使い分ける**ことが重要です。

### JPA vs MyBatisの使い分け指針

**このプロジェクトでの推奨**:

| 機能 | 推奨技術 | 理由 |
|------|---------|------|
| **User CRUD** | JPA | シンプルなCRUD操作 |
| **Project CRUD** | JPA | リレーション管理が簡単 |
| **Task CRUD** | JPA | エンティティ間の関連が多い |
| **Task検索** | MyBatis | 複雑な条件検索、動的クエリ |
| **ダッシュボード集計** | MyBatis | 複数テーブルのJOIN、集計 |
| **レポート出力** | MyBatis | パフォーマンス最適化 |

### 併用パターンの実装例

**1. JPAでCRUD、MyBatisで検索**:
```java
@Service
@RequiredArgsConstructor
public class TaskService {
    
    private final TaskRepository taskRepository;  // JPA
    private final TaskSearchMapper taskSearchMapper;  // MyBatis
    
    // CRUDはJPA
    public TaskResponse createTask(TaskCreateRequest request) {
        Task task = taskMapper.toEntity(request);
        Task saved = taskRepository.save(task);
        return taskMapper.toResponse(saved);
    }
    
    // 複雑な検索はMyBatis
    public List<TaskResponse> searchTasks(TaskSearchCriteria criteria) {
        return taskSearchMapper.search(criteria);
    }
}
```

**2. MyBatisでの複雑な検索Mapper**:
```java
@Mapper
public interface TaskSearchMapper {
    
    List<TaskResponse> search(@Param("criteria") TaskSearchCriteria criteria);
    
    List<TaskStatistics> getProjectStatistics(@Param("projectId") Long projectId);
    
    List<TaskResponse> findUpcomingTasks(@Param("days") int days);
}
```

**Mapper XML（動的SQL）**:
```xml
<select id="search" resultType="TaskResponse">
    SELECT 
        t.id, t.title, t.description, t.status, t.priority,
        u.username as assigneeName,
        p.name as projectName
    FROM tasks t
    LEFT JOIN users u ON t.assignee_id = u.id
    LEFT JOIN projects p ON t.project_id = p.id
    WHERE 1=1
    <if test="criteria.status != null">
        AND t.status = #{criteria.status}
    </if>
    <if test="criteria.priority != null">
        AND t.priority = #{criteria.priority}
    </if>
    <if test="criteria.keyword != null">
        AND (t.title LIKE CONCAT('%', #{criteria.keyword}, '%')
             OR t.description LIKE CONCAT('%', #{criteria.keyword}, '%'))
    </if>
    ORDER BY 
    <choose>
        <when test="criteria.sortBy == 'priority'">t.priority DESC</when>
        <when test="criteria.sortBy == 'dueDate'">t.due_date ASC</when>
        <otherwise>t.created_at DESC</otherwise>
    </choose>
</select>
```

### 実装上の注意点

**1. トランザクション管理**:
```java
@Service
@RequiredArgsConstructor
public class TaskService {
    
    private final TaskRepository taskRepository;  // JPA
    private final TaskMapper taskMapper;  // MyBatis
    
    @Transactional  // JPA、MyBatis両方に適用される
    public void processTask(Long taskId) {
        // JPA操作
        Task task = taskRepository.findById(taskId).orElseThrow();
        task.setStatus(TaskStatus.IN_PROGRESS);
        taskRepository.save(task);
        
        // MyBatis操作
        taskMapper.updateLastProcessedAt(taskId, LocalDateTime.now());
    }
}
```

**2. 依存関係**:
```xml
<!-- pom.xml -->
<dependencies>
    <!-- JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- MyBatis -->
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>3.0.3</version>
    </dependency>
</dependencies>
```

**3. 設定ファイル**:
```yaml
# application.yml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    
mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
```

### このプロジェクトでの実装方針

Phase 8の最終プロジェクトでは、以下のアプローチを推奨します：

1. **基本はJPA**: CRUD操作、エンティティ管理
2. **複雑な検索はMyBatis**: タスク検索、ダッシュボード集計
3. **パフォーマンス重視の部分はMyBatis**: レポート、統計情報

この使い分けにより、開発速度とパフォーマンスのバランスが取れます。

---

## 🎨 チャレンジ課題

### チャレンジ 1: ワイヤーフレーム

画面設計図（ワイヤーフレーム）を描いてください。

### チャレンジ 2: ユースケース図

ユーザーとシステムの相互作用を図示してください。

### チャレンジ 3: シーケンス図

タスク作成フローのシーケンス図を作成してください。

---

## 📚 このステップで学んだこと

- ✅ 要件定義
- ✅ ER図の設計
- ✅ RESTful API設計
- ✅ プロジェクト構造の整理

---

## 🔄 Gitへのコミット

```bash
git add .
git commit -m "Step 34: プロジェクト設計完了"
git push origin main
```

---

## ➡️ 次のステップ

次は[Step 35: エンティティとリポジトリ](STEP_35.md)へ進みましょう！

---

お疲れさまでした！ 🎉
