# Step 28: プロジェクト設計

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
git commit -m "Phase 6: STEP_28完了（プロジェクト設計）"
git push origin main
```

---

## ➡️ 次のステップ

次は[Step 29: エンティティとリポジトリ](STEP_29.md)へ進みましょう！

---

お疲れさまでした！ 🎉
