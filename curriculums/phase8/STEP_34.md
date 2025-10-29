# Step 34: プロジェクト設計

## 🎯 このステップの目標

- タスク管理システムの要件を定義する
- ER図とデータベース設計を作成する
- プロジェクト構造を設計する
- **ThymeleafとMyBatisを使った実装方針を決定する**

**所要時間**: 約2時間

> **このステップはあなたが主体となって設計します！**
> 
> これまでのPhaseで学んだ知識を総動員して、実際にタスク管理システムを設計してください。
> このドキュメントは設計のガイドラインと参考例です。あなた自身のアイデアを盛り込んでください。

---

## 💡 このプロジェクトの技術選択

Phase 8の最終プロジェクトでは、**Thymeleaf + MyBatis**の組み合わせで実装します。

### なぜThymeleaf + MyBatisなのか？

| 技術 | 選択理由 | 学習効果 |
|------|---------|---------|
| **Thymeleaf** | サーバーサイドレンダリング、Spring Boot統合 | フルスタック開発を1つのプロジェクトで完結 |
| **MyBatis** | 複雑なクエリ、動的SQL、パフォーマンス最適化 | Phase 3で学んだ知識を実践で活用 |

### Phase 3-5の復習

- **Phase 3**: MyBatisの基礎（STEP_12-14）
- **Phase 4**: レイヤードアーキテクチャ（STEP_15）でJPA/MyBatisの使い分けを学習
- **Phase 5**: Thymeleafの基礎（STEP_21-24）

### このプロジェクトでの実装方針

```
┌─────────────────────────────────────────┐
│        フロントエンド層                  │
│   Thymeleaf + Bootstrap 5               │
│   (templates/*.html)                    │
└─────────────────────────────────────────┘
                  ↓↑
┌─────────────────────────────────────────┐
│        コントローラー層                  │
│   @Controller (Web)                     │
│   Model, RedirectAttributes             │
└─────────────────────────────────────────┘
                  ↓↑
┌─────────────────────────────────────────┐
│        サービス層                        │
│   @Service                              │
│   ビジネスロジック、トランザクション管理   │
└─────────────────────────────────────────┘
                  ↓↑
┌─────────────────────────────────────────┐
│        データアクセス層                  │
│   MyBatis Mapper (@Mapper)              │
│   XML/Annotationベースの動的SQL          │
└─────────────────────────────────────────┘
                  ↓↑
┌─────────────────────────────────────────┐
│        データベース                      │
│   MySQL                                 │
└─────────────────────────────────────────┘
```

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

## � 画面設計（Thymeleaf）

### 画面一覧

| URL | 説明 | 主な機能 |
|-----|------|---------|
| `/login` | ログイン画面 | ユーザー認証 |
| `/register` | ユーザー登録画面 | 新規ユーザー登録 |
| `/dashboard` | ダッシュボード | 統計情報、期限間近のタスク表示 |
| `/projects` | プロジェクト一覧 | プロジェクト一覧表示、新規作成 |
| `/projects/{id}` | プロジェクト詳細 | タスクボード（カンバン形式）、タスク作成 |
| `/projects/{id}/settings` | プロジェクト設定 | メンバー管理、プロジェクト編集 |
| `/tasks/{id}` | タスク詳細 | タスク編集、コメント追加、ファイル添付 |
| `/tasks/search` | タスク検索 | 条件検索、フィルタリング |
| `/profile` | プロフィール | ユーザー情報編集 |

### 主要画面のワイヤーフレーム

#### ダッシュボード
```
┌────────────────────────────────────────────┐
│ 📋 タスク管理システム    👤 ユーザー名 [ログアウト] │
├────────────────────────────────────────────┤
│ ナビゲーション: [ダッシュボード] [プロジェクト] [タスク] │
├────────────────────────────────────────────┤
│                                            │
│  ⚠️ 期限切れタスク: 3件                      │
│  📅 今日が期限のタスク: 2件                   │
│                                            │
│  ┌─────────────┐ ┌─────────────┐          │
│  │プロジェクトA  │ │プロジェクトB  │          │
│  │進捗率: 75%   │ │進捗率: 30%   │          │
│  │[詳細を見る]  │ │[詳細を見る]  │          │
│  └─────────────┘ └─────────────┘          │
│                                            │
│  📊 ステータス別タスク数（グラフ）              │
│                                            │
└────────────────────────────────────────────┘
```

#### プロジェクト詳細（カンバンボード）
```
┌────────────────────────────────────────────┐
│ ← プロジェクト: Webサイトリニューアル          │
│ [➕ 新規タスク] [⚙️ 設定]                    │
├────────────────────────────────────────────┤
│ TODO         │ 進行中        │ 完了         │
│┌───────────┐ │┌───────────┐ │┌───────────┐│
││タスク1     │ ││タスク4     │ ││タスク7     ││
││🔴HIGH     │ ││🟡MEDIUM   │ ││🟢LOW      ││
││📅 12/31   │ ││📅 12/25   │ ││✅ 完了     ││
│└───────────┘ │└───────────┘ │└───────────┘│
│┌───────────┐ │┌───────────┐ │              │
││タスク2     │ ││タスク5     │ │              │
││🟡MEDIUM   │ ││🔴HIGH     │ │              │
│└───────────┘ │└───────────┘ │              │
└────────────────────────────────────────────┘
```

---

## 🗄️ データベース設計（MyBatis）

### テーブル定義

**users テーブル**:
```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**projects テーブル**:
```sql
CREATE TABLE projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);
```

**tasks テーブル**:
```sql
CREATE TABLE tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'TODO',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    assignee_id BIGINT,
    due_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (assignee_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_project_status (project_id, status),
    INDEX idx_assignee_status (assignee_id, status),
    INDEX idx_due_date (due_date)
);
```

**comments テーブル**:
```sql
CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

**tags テーブル**:
```sql
CREATE TABLE tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    color VARCHAR(7) DEFAULT '#6c757d'
);
```

**task_tags テーブル**（多対多の中間テーブル）:
```sql
CREATE TABLE task_tags (
    task_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (task_id, tag_id),
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);
```

**project_members テーブル**:
```sql
CREATE TABLE project_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_project_user (project_id, user_id)
);
```

---

## 📁 プロジェクト構造（Thymeleaf + MyBatis）

```
src/main/java/com/example/taskapp/
├── config/
│   ├── SecurityConfig.java
│   ├── ThymeleafConfig.java
│   └── MyBatisConfig.java
├── controller/
│   ├── web/                      ← Thymeleafコントローラー
│   │   ├── DashboardController.java
│   │   ├── ProjectWebController.java
│   │   ├── TaskWebController.java
│   │   └── AuthController.java
│   └── (api/)                    ← オプション: REST API
├── dto/
│   ├── request/
│   │   ├── ProjectCreateRequest.java
│   │   ├── TaskCreateRequest.java
│   │   └── CommentCreateRequest.java
│   └── response/
│       ├── ProjectResponse.java
│       ├── TaskResponse.java
│       └── TaskStatistics.java
├── entity/
│   ├── User.java
│   ├── Project.java
│   ├── Task.java
│   ├── Comment.java
│   ├── Tag.java
│   └── enums/
│       ├── TaskStatus.java
│       └── Priority.java
├── mapper/                       ← MyBatis Mapper
│   ├── UserMapper.java
│   ├── ProjectMapper.java
│   ├── TaskMapper.java
│   ├── CommentMapper.java
│   └── TagMapper.java
├── service/
│   ├── UserService.java
│   ├── ProjectService.java
│   ├── TaskService.java
│   ├── CommentService.java
│   └── NotificationService.java
├── security/
│   ├── CustomUserDetailsService.java
│   └── SecurityUtils.java
└── exception/
    ├── ResourceNotFoundException.java
    └── GlobalExceptionHandler.java

src/main/resources/
├── application.yml
├── mapper/                       ← MyBatis XML Mapper
│   ├── UserMapper.xml
│   ├── ProjectMapper.xml
│   ├── TaskMapper.xml
│   ├── CommentMapper.xml
│   └── TagMapper.xml
├── templates/                    ← Thymeleafテンプレート
│   ├── layout/
│   │   ├── main.html            ← 共通レイアウト
│   │   └── fragments.html       ← 共通パーツ
│   ├── auth/
│   │   ├── login.html
│   │   └── register.html
│   ├── dashboard/
│   │   └── index.html
│   ├── projects/
│   │   ├── list.html
│   │   ├── detail.html
│   │   ├── form.html
│   │   └── settings.html
│   ├── tasks/
│   │   ├── detail.html
│   │   ├── search.html
│   │   └── components/
│   │       └── task-card.html
│   └── error/
│       ├── 404.html
│       └── 500.html
└── static/
    ├── css/
    │   └── custom.css
    ├── js/
    │   └── app.js
    └── images/
```

---

## 💡 補足: Data Access層の技術選択

Phase 3でMyBatisを、Phase 2でJPAを学習してきました。最終プロジェクトでは、**MyBatisを中心に使う**ことで、Phase 3の知識を実践で活用します。

### MyBatisを選ぶ理由（このプロジェクト）

| 機能 | MyBatisが適している理由 |
|------|----------------------|
| **タスク検索** | 複雑な条件検索、動的WHERE句が必要 |
| **ダッシュボード集計** | 複数テーブルのJOIN、GROUP BY、集計関数 |
| **パフォーマンス最適化** | 必要なカラムだけをSELECT、N+1問題の回避 |
| **柔軟なクエリ** | プロジェクトごとのタスク統計、期限切れタスクの抽出など |

### MyBatis実装の例

**TaskMapper.java**:
```java
@Mapper
public interface TaskMapper {
    
    // 基本CRUD
    void insert(Task task);
    Task findById(Long id);
    List<Task> findAll();
    void update(Task task);
    void deleteById(Long id);
    
    // 複雑な検索（動的SQL）
    List<Task> search(@Param("criteria") TaskSearchCriteria criteria);
    
    // 統計
    TaskStatistics getProjectStatistics(@Param("projectId") Long projectId);
    
    // プロジェクトのタスク取得（JOIN）
    List<Task> findByProjectIdWithDetails(@Param("projectId") Long projectId);
    
    // 期限切れタスク
    List<Task> findOverdueTasks(@Param("userId") Long userId);
}
```

**TaskMapper.xml** （動的SQL例）:
```xml
<select id="search" resultMap="TaskResultMap">
    SELECT 
        t.*, 
        u.username as assignee_name,
        p.name as project_name
    FROM tasks t
    LEFT JOIN users u ON t.assignee_id = u.id
    LEFT JOIN projects p ON t.project_id = p.id
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
        AND (t.title LIKE CONCAT('%', #{criteria.keyword}, '%')
             OR t.description LIKE CONCAT('%', #{criteria.keyword}, '%'))
    </if>
    <if test="criteria.dueDateFrom != null">
        AND t.due_date &gt;= #{criteria.dueDateFrom}
    </if>
    <if test="criteria.dueDateTo != null">
        AND t.due_date &lt;= #{criteria.dueDateTo}
    </if>
    ORDER BY 
    <choose>
        <when test="criteria.sortBy == 'priority'">t.priority DESC, t.created_at DESC</when>
        <when test="criteria.sortBy == 'dueDate'">t.due_date ASC NULLS LAST</when>
        <when test="criteria.sortBy == 'status'">t.status ASC, t.created_at DESC</when>
        <otherwise>t.created_at DESC</otherwise>
    </choose>
    LIMIT #{criteria.limit} OFFSET #{criteria.offset}
</select>
```

### JPAとの比較

| 観点 | MyBatis（このプロジェクト） | JPA |
|------|---------------------------|-----|
| **CRUD操作** | アノテーションで簡潔 | 自動生成で簡単 |
| **複雑な検索** | XMLで柔軟に記述 ✅ | JPQLやSpecificationが煩雑 |
| **動的クエリ** | `<if>`, `<choose>`で自然 ✅ | Criteria APIが複雑 |
| **パフォーマンス** | 必要なカラムのみSELECT ✅ | N+1問題に注意が必要 |
| **学習コスト** | SQLがわかれば使える ✅ | JPAの概念理解が必要 |

> **💡 このプロジェクトの方針**: 
> - **基本CRUD**: MyBatisアノテーション（`@Select`, `@Insert`等）
> - **複雑な検索・集計**: MyBatis XML（動的SQL活用）
> - **トランザクション**: `@Transactional`でSpring管理
> 
> Phase 3で学んだMyBatisの知識を最大限に活用し、実務で使える実装パターンを身につけます！

---

## �🎨 チャレンジ課題

### チャレンジ 1: ワイヤーフレーム

画面設計図（ワイヤーフレーム）を描いてください。

### チャレンジ 2: ユースケース図

ユーザーとシステムの相互作用を図示してください。

### チャレンジ 3: シーケンス図

タスク作成フローのシーケンス図を作成してください。

---

## � あなたの設計課題

以下の課題に取り組んで、タスク管理システムの設計を完成させてください。

### 課題1: 画面設計（Thymeleaf）

以下の画面を設計してください（ワイヤーフレームを描く、または要件を箇条書きにする）：

1. **ログイン画面** (`/login`)
2. **ダッシュボード** (`/dashboard`)
3. **プロジェクト一覧** (`/projects`)
4. **プロジェクト詳細** (`/projects/{id}`)
5. **カンバンボード** (`/projects/{id}/kanban`)
6. **タスク詳細** (`/tasks/{id}`)
7. **タスク作成/編集フォーム** (`/tasks/new`, `/tasks/{id}/edit`)
8. **タスク検索** (`/tasks/search`)
9. **ユーザープロフィール** (`/profile`)

> **💡 ヒント**: `example/STEP_34_design_example.md`に参考例があります

### 課題2: データベース設計

以下のテーブルの詳細設計を行ってください：

**必須テーブル**: `users`, `projects`, `tasks`, `comments`, `tags`, `task_tags`, `project_members`

**設計すべき項目**:
1. 各テーブルのカラム定義（型、NULL制約、デフォルト値）
2. 主キー、外部キー
3. インデックス（検索性能向上のため）

**DDL作成**: `src/main/resources/schema.sql`にCREATE TABLE文を書いてください。

> **💡 ヒント**: `example/STEP_34_schema_example.sql`に参考例があります

### 課題3: MyBatis Mapper設計

主要なMapperインターフェースとXMLファイルを設計してください。

> **💡 ヒント**: `example/STEP_34_mapper_example.xml`に参考例があります

### 課題4: プロジェクト構造設計

以下のディレクトリ構造を実際に作成してください：

```bash
mkdir -p src/main/java/com/example/taskapp/{config,controller,service,mapper,entity,dto/request,dto/response,exception}
mkdir -p src/main/resources/{mapper,templates/{layouts,projects,tasks,dashboard,auth},static/{css,js,images}}
mkdir -p src/test/java/com/example/taskapp/{controller,service,mapper}
```

---

## ✅ チェックリスト

- [ ] 画面設計（全9画面）を完成させた
- [ ] データベース設計（全7テーブル）のDDLを作成した
- [ ] 主要なMapperインターフェースを設計した
- [ ] プロジェクト構造（ディレクトリ）を作成した
- [ ] `schema.sql`を作成して配置した

---

## 💡 参考実装例

- `example/STEP_34_design_example.md` - 画面設計の参考例
- `example/STEP_34_schema_example.sql` - DDLの参考例
- `example/STEP_34_mapper_example.xml` - MyBatis XMLの参考例

---

## �📚 このステップで学んだこと

- ✅ システム要件定義の方法
- ✅ ER図からテーブル設計への落とし込み
- ✅ Thymeleaf画面設計のポイント
- ✅ MyBatis動的SQLの設計パターン
- ✅ プロジェクト構造の最適化

---

## 🔄 Gitへのコミット

```bash
git add .
git commit -m "Step 34: タスク管理システムの設計完了"
git push origin main
```

---

## ➡️ 次のステップ

次は[Step 35: エンティティとMapper実装](STEP_35.md)へ進みましょう！

---

お疲れさまでした！ 🎉
