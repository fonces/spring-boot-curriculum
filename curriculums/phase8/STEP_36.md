# Step 36: サービスとAPI実装

## 🎯 このステップの目標

- ビジネスロジックをサービス層に実装する
- REST APIコントローラーを作成する
- DTO（Data Transfer Object）を定義する
- MapStructでマッピングを自動化する

**所要時間**: 約3時間

---

## 📋 実装要件

このステップでは、タスク管理システムのコア機能を実装します。

### 実装するクラス一覧

#### DTOクラス
- **Request**: `ProjectCreateRequest`, `TaskCreateRequest`, `TaskUpdateRequest`, `CommentCreateRequest`等
- **Response**: `ProjectResponse`, `TaskResponse`, `CommentResponse`, `TagResponse`等

#### Mapperインターフェース (MapStruct)
- `ProjectMapper`, `TaskMapper`, `CommentMapper`, `TagMapper`

#### Serviceクラス
- `ProjectService`, `TaskService`, `CommentService`, `TagService`, `NotificationService`

#### Controllerクラス
- `ProjectController`, `TaskController`, `CommentController`, `TagController`

---

## 🚀 ステップ1: DTOクラスの実装

### 1-1. ProjectCreateRequest

**必須フィールド**:
- `name` (String) - @NotBlank, @Size(min=1, max=100)
- `description` (String) - @Size(max=1000)

### 1-2. TaskCreateRequest

**必須フィールド**:
- `title` (String) - @NotBlank, @Size(min=1, max=200)
- `description` (String) - @Size(max=2000)
- `projectId` (Long) - @NotNull
- `status` (TaskStatus) - optional
- `priority` (Priority) - optional
- `assigneeId` (Long) - optional
- `dueDate` (LocalDate) - optional
- `tagIds` (Set<Long>) - optional

### 1-3. TaskResponse

**必須フィールド**:
- `id`, `title`, `description`
- `projectId`, `projectName`
- `status`, `priority`
- `assignee` (UserSummaryResponse型)
- `dueDate`
- `tags` (Set<TagResponse>)
- `createdAt`, `updatedAt`

**配置場所**: 
- `src/main/java/com/example/hellospringboot/dto/request/`
- `src/main/java/com/example/hellospringboot/dto/response/`

**ポイント**:
- Swaggerアノテーション（`@Schema`）を使って説明とexampleを追加
- Jakarta Validationでバリデーション

---

## 🚀 ステップ2: MapStructマッパーの実装

### 2-1. TaskMapper

**必須メソッド**:
```java
@Mapper(componentModel = "spring")
public interface TaskMapper {
    
    // RequestからEntityへ変換
    Task toEntity(TaskCreateRequest request);
    
    // EntityからResponseへ変換
    TaskResponse toResponse(Task task);
    
    // リスト変換
    List<TaskResponse> toResponseList(List<Task> tasks);
}
```

**実装のポイント**:
- `@Mapping`で特殊なマッピングを定義
- ネストしたオブジェクト（project.name → projectName）の変換

**配置場所**: `src/main/java/com/example/hellospringboot/mapper/`

---

## 🚀 ステップ3: サービス層の実装

### 3-1. TaskService

**必須メソッド**:

#### 作成
```java
@Transactional
TaskResponse createTask(TaskCreateRequest request)
```
- プロジェクトの存在確認
- 担当者の設定（存在する場合）
- タグの設定
- 保存後、担当者にメール通知

#### 取得
```java
TaskResponse getTaskById(Long id)
Page<TaskResponse> getTasksByProject(Long projectId, Pageable pageable)
Page<TaskResponse> searchTasks(Long projectId, TaskStatus status, Priority priority, Long assigneeId, String keyword, Pageable pageable)
```

#### 更新
```java
@Transactional
TaskResponse updateTask(Long id, TaskUpdateRequest request)

@Transactional
TaskResponse updateStatus(Long id, TaskStatus status)
```

#### 削除
```java
@Transactional
void deleteTask(Long id)
```

#### タグ操作
```java
@Transactional
TaskResponse addTag(Long taskId, Long tagId)

@Transactional
TaskResponse removeTag(Long taskId, Long tagId)
```

### 3-2. NotificationService

**必須メソッド**:
```java
@Async
void notifyTaskAssignment(Task task)
```
- タスク割り当て時のメール通知を非同期で送信
- EmailServiceを使用

**配置場所**: `src/main/java/com/example/hellospringboot/service/`

**実装のポイント**:
- `@Transactional(readOnly = true)`を読み取り専用メソッドに付与
- 例外ハンドリング（ResourceNotFoundException等）
- ログ出力

---

## 🚀 ステップ4: コントローラー層の実装

### 4-1. TaskController

**必須エンドポイント**:

| メソッド | パス | 説明 |
|---------|------|------|
| POST | `/api/tasks` | タスク作成 |
| GET | `/api/tasks/{id}` | タスク取得 |
| GET | `/api/tasks/search` | タスク検索 |
| PUT | `/api/tasks/{id}` | タスク更新 |
| PATCH | `/api/tasks/{id}/status` | ステータス変更 |
| DELETE | `/api/tasks/{id}` | タスク削除 |
| POST | `/api/tasks/{taskId}/tags/{tagId}` | タグ追加 |
| DELETE | `/api/tasks/{taskId}/tags/{tagId}` | タグ削除 |

**実装のポイント**:
- `@RestController`, `@RequestMapping("/api/tasks")`
- `@PreAuthorize`でロール制御
- Swaggerアノテーション（`@Operation`, `@Tag`）
- `@Valid`でバリデーション
- `ResponseEntity`で適切なHTTPステータスを返す
- ページングには`@PageableDefault`を使用

**配置場所**: `src/main/java/com/example/hellospringboot/controller/`

---

## 🚀 ステップ5: 他のエンティティの実装

同様に以下を実装してください：

### ProjectService & ProjectController

**エンドポイント**:
- `GET /api/projects` - プロジェクト一覧
- `POST /api/projects` - プロジェクト作成
- `GET /api/projects/{id}` - プロジェクト詳細
- `PUT /api/projects/{id}` - プロジェクト更新
- `DELETE /api/projects/{id}` - プロジェクト削除
- `POST /api/projects/{id}/members` - メンバー追加
- `DELETE /api/projects/{id}/members/{userId}` - メンバー削除

### CommentService & CommentController

**エンドポイント**:
- `GET /api/tasks/{taskId}/comments` - コメント一覧
- `POST /api/tasks/{taskId}/comments` - コメント作成
- `PUT /api/comments/{id}` - コメント更新
- `DELETE /api/comments/{id}` - コメント削除

### TagService & TagController

**エンドポイント**:
- `GET /api/tags` - タグ一覧
- `POST /api/tags` - タグ作成

---

## ✅ 動作確認

### タスク作成APIのテスト

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

### タスク検索APIのテスト

```bash
curl "http://localhost:8080/api/tasks/search?projectId=1&status=TODO&page=0&size=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Swagger UIで確認

```
http://localhost:8080/swagger-ui.html
```

---

## 💡 実装のポイント

### DTOパターン
- エンティティを直接公開せず、DTOを経由
- RequestとResponseで別のクラスを使用
- バリデーションはRequestクラスに定義

### MapStruct
- コンパイル時にマッピングコードを自動生成
- 手動マッピングより安全で高速
- `@Mapping`でカスタムマッピング

### サービス層
- ビジネスロジックはサービス層に集約
- トランザクション境界を明確に
- 適切な例外処理

### コントローラー層
- HTTPリクエスト/レスポンスの変換のみ
- ビジネスロジックは書かない
- 適切なHTTPステータスコードを返す

---

## 💡 補足: フロントエンド実装の選択肢

Phase 5でThymeleafを学習しました。最終プロジェクトでは、**REST API**と**Thymeleaf UI**のどちらを使うか、または両方を組み合わせるか選択できます。

### アプローチ1: REST API（推奨：モダンなSPA）

**フロントエンド**: React, Vue.js, Angular など

**メリット**:
- フロントエンドとバックエンドの完全分離
- リッチなUIを実装可能
- モバイルアプリとも共通のAPIを利用

**このプロジェクトでの実装**:
```
バックエンド: Spring Boot REST API（このステップで実装済み）
フロントエンド: React/Vue（別リポジトリで実装）
```

### アプローチ2: Thymeleaf（推奨：学習目的、小規模）

**フロントエンド**: Thymeleaf + Bootstrap/Tailwind CSS

**メリット**:
- サーバーサイドで完結、シンプル
- Spring Bootの知識だけで実装可能
- 学習コストが低い

**Thymeleaf版のコントローラー例**:
```java
@Controller
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectWebController {

    private final ProjectService projectService;

    /**
     * プロジェクト一覧画面
     */
    @GetMapping
    public String listProjects(Model model, Authentication authentication) {
        String username = authentication.getName();
        List<ProjectResponse> projects = projectService.getUserProjects(username);
        model.addAttribute("projects", projects);
        return "projects/list";  // templates/projects/list.html
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

        String username = authentication.getName();
        ProjectResponse created = projectService.createProject(request, username);
        
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

**Thymeleafテンプレート例**: `templates/projects/list.html`
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>プロジェクト一覧</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <div class="container mt-5">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h1>プロジェクト一覧</h1>
            <a th:href="@{/projects/new}" class="btn btn-primary">
                ➕ 新規プロジェクト
            </a>
        </div>

        <div th:if="${message}" class="alert alert-success alert-dismissible fade show">
            <span th:text="${message}"></span>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>

        <div class="row">
            <div class="col-md-4 mb-4" th:each="project : ${projects}">
                <div class="card h-100">
                    <div class="card-body">
                        <h5 class="card-title" th:text="${project.name}">プロジェクト名</h5>
                        <p class="card-text text-muted" th:text="${project.description}">説明</p>
                        <p class="card-text">
                            <small class="text-muted">
                                作成日: <span th:text="${#temporals.format(project.createdAt, 'yyyy/MM/dd')}">2025/01/01</span>
                            </small>
                        </p>
                    </div>
                    <div class="card-footer">
                        <a th:href="@{/projects/{id}(id=${project.id})}" class="btn btn-sm btn-outline-primary">
                            詳細を見る
                        </a>
                    </div>
                </div>
            </div>
        </div>

        <div th:if="${#lists.isEmpty(projects)}" class="alert alert-info">
            プロジェクトがありません。新しいプロジェクトを作成しましょう！
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
```

**タスク詳細画面**: `templates/projects/detail.html`
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title th:text="${project.name}">プロジェクト詳細</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <div class="container mt-5">
        <nav aria-label="breadcrumb">
            <ol class="breadcrumb">
                <li class="breadcrumb-item"><a th:href="@{/projects}">プロジェクト</a></li>
                <li class="breadcrumb-item active" th:text="${project.name}">現在のプロジェクト</li>
            </ol>
        </nav>

        <h1 th:text="${project.name}">プロジェクト名</h1>
        <p class="lead" th:text="${project.description}">説明</p>

        <h2 class="mt-5">タスク一覧</h2>
        
        <div class="row">
            <div class="col-md-4" th:each="status : ${T(com.example.hellospringboot.entity.TaskStatus).values()}">
                <div class="card mb-4">
                    <div class="card-header" th:classappend="${status == T(com.example.hellospringboot.entity.TaskStatus).TODO ? 'bg-secondary' : 
                                                              status == T(com.example.hellospringboot.entity.TaskStatus).IN_PROGRESS ? 'bg-primary' : 'bg-success'} text-white">
                        <h5 th:text="${status.name()}">TODO</h5>
                    </div>
                    <ul class="list-group list-group-flush">
                        <li class="list-group-item" 
                            th:each="task : ${tasks}" 
                            th:if="${task.status == status}">
                            <div class="d-flex justify-content-between align-items-start">
                                <div>
                                    <h6 class="mb-1" th:text="${task.title}">タスク名</h6>
                                    <small class="text-muted">
                                        <span th:if="${task.assignee != null}">
                                            👤 <span th:text="${task.assignee.name}">担当者</span>
                                        </span>
                                        <span th:if="${task.dueDate != null}">
                                            📅 <span th:text="${#temporals.format(task.dueDate, 'MM/dd')}">期限</span>
                                        </span>
                                    </small>
                                </div>
                                <span class="badge" 
                                      th:classappend="${task.priority == T(com.example.hellospringboot.entity.Priority).HIGH ? 'bg-danger' : 
                                                       task.priority == T(com.example.hellospringboot.entity.Priority).MEDIUM ? 'bg-warning' : 'bg-secondary'}"
                                      th:text="${task.priority.name()}">HIGH</span>
                            </div>
                        </li>
                    </ul>
                </div>
            </div>
        </div>

        <!-- タスク作成フォーム -->
        <div class="card mt-4">
            <div class="card-header">
                <h5>新しいタスクを作成</h5>
            </div>
            <div class="card-body">
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
