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
