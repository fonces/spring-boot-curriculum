# Step 35: エンティティとリポジトリ実装

## 🎯 このステップの目標

- 全エンティティクラスを実装する
- Enumクラスを定義する
- リポジトリインターフェースを作成する
- カスタムクエリメソッドを実装する

**所要時間**: 約2時間30分

---

## 📋 実装要件

このステップでは、[STEP_34で設計したER図](STEP_34.md#er図entity-relationship-diagram)を基に、以下のクラスを実装してください。

### 実装するクラス一覧

#### Enumクラス (3つ)
- `TaskStatus` - タスクのステータス（TODO, IN_PROGRESS, DONE）
- `Priority` - タスクの優先度（LOW, MEDIUM, HIGH）
- `ProjectRole` - プロジェクトメンバーの役割（OWNER, MEMBER）

#### エンティティクラス (5つ)
- `Project` - プロジェクト
- `ProjectMember` - プロジェクトメンバー（中間テーブル）
- `Task` - タスク
- `Comment` - コメント
- `Tag` - タグ

#### リポジトリインターフェース (5つ)
- `ProjectRepository`
- `ProjectMemberRepository`
- `TaskRepository`
- `CommentRepository`
- `TagRepository`

---

## 🚀 ステップ1: Enumクラスの実装

### 実装するEnum

各Enumには以下を含めてください：
- ステータス/優先度/役割の値
- 日本語表示名（displayName）
- getterメソッド

**配置場所**: `src/main/java/com/example/hellospringboot/enums/`

### 実装のヒント

- `TaskStatus`: TODO（未着手）、IN_PROGRESS（進行中）、DONE（完了）
- `Priority`: LOW（低）、MEDIUM（中）、HIGH（高）
- `ProjectRole`: OWNER（オーナー）、MEMBER（メンバー）

---

## 🚀 ステップ2: エンティティクラスの実装

### 2-1. Projectエンティティ

**必須フィールド**:
- `id` (Long) - 主キー、自動生成
- `name` (String) - プロジェクト名、NOT NULL、最大100文字
- `description` (String) - 説明、TEXT型
- `owner` (User) - オーナー、ManyToOne、LAZY、NOT NULL
- `tasks` (List<Task>) - タスクリスト、OneToMany、CASCADE、orphanRemoval
- `members` (List<ProjectMember>) - メンバーリスト、OneToMany、CASCADE、orphanRemoval
- `createdAt` (LocalDateTime) - 作成日時、@CreationTimestamp
- `updatedAt` (LocalDateTime) - 更新日時、@UpdateTimestamp

**アノテーション**:
- `@Entity`, `@Table(name = "projects")`
- Lombokアノテーション（@Getter, @Setter, @Builder等）

### 2-2. ProjectMemberエンティティ

**必須フィールド**:
- `id` (Long) - 主キー
- `project` (Project) - プロジェクト、ManyToOne、LAZY、NOT NULL
- `user` (User) - ユーザー、ManyToOne、LAZY、NOT NULL
- `role` (ProjectRole) - 役割、Enum、NOT NULL
- `joinedAt` (LocalDateTime) - 参加日時、@CreationTimestamp

**制約**:
- プロジェクトIDとユーザーIDの組み合わせは一意（`@UniqueConstraint`）

### 2-3. Taskエンティティ

**必須フィールド**:
- `id` (Long) - 主キー
- `project` (Project) - プロジェクト、ManyToOne、LAZY、NOT NULL
- `title` (String) - タイトル、NOT NULL、最大200文字
- `description` (String) - 説明、TEXT型
- `status` (TaskStatus) - ステータス、Enum、デフォルトTODO
- `priority` (Priority) - 優先度、Enum、デフォルトMEDIUM
- `assignee` (User) - 担当者、ManyToOne、LAZY、nullable
- `dueDate` (LocalDate) - 期限、nullable
- `comments` (List<Comment>) - コメントリスト、OneToMany、CASCADE
- `tags` (Set<Tag>) - タグセット、ManyToMany
- `createdAt`, `updatedAt` - 作成・更新日時

**インデックス**:
- `status`, `priority`, `dueDate`にインデックスを設定

**多対多関係**:
- `@JoinTable`で中間テーブル`task_tags`を定義

### 2-4. Commentエンティティ

**必須フィールド**:
- `id`, `task` (ManyToOne), `user` (ManyToOne), `content` (TEXT)
- `createdAt`, `updatedAt`

### 2-5. Tagエンティティ

**必須フィールド**:
- `id` (Long)
- `name` (String) - タグ名、UNIQUE、最大50文字
- `color` (String) - HEXカラーコード（例: #FF5733）、7文字
- `tasks` (Set<Task>) - ManyToMany（mappedBy）

**配置場所**: `src/main/java/com/example/hellospringboot/entity/`

---

## 🚀 ステップ3: リポジトリインターフェースの実装

### 3-1. ProjectRepository

**必須メソッド**:
```java
// オーナーIDでプロジェクトを検索
List<Project> findByOwnerId(Long ownerId);

// ユーザーが参加しているプロジェクトを取得（JPQL使用）
@Query("SELECT DISTINCT p FROM Project p LEFT JOIN p.members m WHERE p.owner.id = :userId OR m.user.id = :userId")
List<Project> findProjectsByUserId(@Param("userId") Long userId);

// プロジェクト名で検索（部分一致、大文字小文字区別なし）
List<Project> findByNameContainingIgnoreCase(String name);
```

### 3-2. TaskRepository

**必須メソッド**:
```java
// プロジェクトIDでタスクを検索（ページング）
Page<Task> findByProjectId(Long projectId, Pageable pageable);

// 担当者IDでタスクを検索（ページング）
Page<Task> findByAssigneeId(Long assigneeId, Pageable pageable);

// ステータスで検索
List<Task> findByStatus(TaskStatus status);

// 優先度で検索
List<Task> findByPriority(Priority priority);

// 期限が指定日以前のタスクを検索
List<Task> findByDueDateBefore(LocalDate date);

// 複合条件検索（JPQL使用、すべての条件がoptional）
@Query("SELECT t FROM Task t WHERE (:projectId IS NULL OR t.project.id = :projectId) AND ...")
Page<Task> searchTasks(...);

// プロジェクトのタスク統計（ステータスごとの件数）
@Query("SELECT t.status, COUNT(t) FROM Task t WHERE t.project.id = :projectId GROUP BY t.status")
List<Object[]> getTaskStatisticsByProject(@Param("projectId") Long projectId);
```

### 3-3. CommentRepository

**必須メソッド**:
```java
// タスクIDでコメントを検索（作成日時の降順）
List<Comment> findByTaskIdOrderByCreatedAtDesc(Long taskId);

// ユーザーIDでコメントを検索
List<Comment> findByUserId(Long userId);
```

### 3-4. TagRepository

**必須メソッド**:
```java
// タグ名で検索
Optional<Tag> findByName(String name);

// タグ名の存在確認
boolean existsByName(String name);
```

### 3-5. ProjectMemberRepository

**必須メソッド**:
```java
// プロジェクトIDでメンバーを検索
List<ProjectMember> findByProjectId(Long projectId);

// プロジェクトとユーザーでメンバーを検索
Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

// プロジェクトとユーザーの組み合わせが存在するかチェック
boolean existsByProjectIdAndUserId(Long projectId, Long userId);

// プロジェクトとユーザーでメンバーを削除
void deleteByProjectIdAndUserId(Long projectId, Long userId);
```

**配置場所**: `src/main/java/com/example/hellospringboot/repository/`

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

**確認するテーブル**:
- projects
- project_members
- tasks
- comments
- tags
- task_tags

**確認ポイント**:
- ✅ 全てのテーブルが作成されている
- ✅ 外部キー制約が正しく設定されている
- ✅ インデックスが設定されている
- ✅ UNIQUE制約が設定されている

---

## 💡 実装のポイント

### Enumの定義
- `@Enumerated(EnumType.STRING)`を使用（DBに文字列として保存）
- 日本語の表示名を持たせる

### エンティティのリレーション
- **ManyToOne**: `fetch = FetchType.LAZY`を使用（N+1問題回避）
- **OneToMany**: `cascade = CascadeType.ALL`と`orphanRemoval = true`を設定
- **ManyToMany**: `@JoinTable`で中間テーブルを明示的に定義

### インデックスの設定
- 検索条件として使用するフィールドにインデックスを設定
- `@Index`アノテーションを使用

### Lombokの活用
- `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`を使用
- コンストラクタではなくBuilderパターンを推奨

### タイムスタンプ
- `@CreationTimestamp`: 作成時に自動設定
- `@UpdateTimestamp`: 更新時に自動設定

---

## 🎨 チャレンジ課題

### チャレンジ 1: 監査フィールドの共通化

`@EntityListeners`と`@MappedSuperclass`を使って、`createdAt`と`updatedAt`を共通基底クラスに抽出してください。

**ヒント**:
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

### チャレンジ 2: ソフトデリート

物理削除ではなく論理削除（ソフトデリート）を実装してください。

**ヒント**:
- `deletedAt`フィールドを追加
- `@Where(clause = "deleted_at IS NULL")`を使用

### チャレンジ 3: Criteria APIによる動的検索

`TaskRepository`に、Criteria APIを使った動的検索を実装してください。

---

## 📚 このステップで学んだこと

- ✅ 複雑なエンティティ設計
- ✅ Enumの活用
- ✅ 多対多リレーションシップ
- ✅ カスタムクエリメソッド
- ✅ インデックスの設定
- ✅ JPQL / Spring Data JPAクエリメソッド

---

## 🔄 Gitへのコミット

```bash
git add .
git commit -m "Step 35: エンティティとリポジトリ実装完了"
git push origin main
```

---

## ➡️ 次のステップ

次は[Step 36: サービスとAPI実装](STEP_36.md)へ進みましょう！

---

お疲れさまでした！ 🎉
