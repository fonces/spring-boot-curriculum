# Step 35: エンティティとMapper実装

## 🎯 このステップの目標

- 全エンティティクラスを実装する
- Enumクラスを定義する
- **MyBatis Mapperインターフェース**を作成する
- 基本的なCRUD操作を実装する

**所要時間**: 約3時間

> **このステップはあなたが実装します！**
> 
> STEP_34で設計した内容をもとに、実際にコードを書いてください。
> わからない場合は`example/STEP_35_*.java`の実装例を参考にしてください。

---

## 📋 実装課題

### 課題1: Enumクラスの実装（3つ）

以下のEnumクラスを実装してください：

**TaskStatus.java** (`src/main/java/com/example/taskapp/entity/enums/`)
```java
package com.example.taskapp.entity.enums;

public enum TaskStatus {
    TODO("未着手"),
    IN_PROGRESS("進行中"),
    DONE("完了");
    
    private final String displayName;
    
    // TODO: コンストラクタとgetterを実装
}
```

**Priority.java**
```java
package com.example.taskapp.entity.enums;

public enum Priority {
    LOW("低", "#6c757d"),
    MEDIUM("中", "#ffc107"),
    HIGH("高", "#dc3545");
    
    private final String displayName;
    private final String color;
    
    // TODO: コンストラクタとgetterを実装
}
```

**ProjectRole.java**
```java
package com.example.taskapp.entity.enums;

public enum ProjectRole {
    OWNER("オーナー"),
    MEMBER("メンバー");
    
    // TODO: 実装
}
```

> **💡 ヒント**: `example/STEP_35_enum_example.java`に完全な実装例があります

---

### 課題2: エンティティクラスの実装（5つ）

以下のエンティティクラスを実装してください：

**User.java** (`src/main/java/com/example/taskapp/entity/`)
```java
package com.example.taskapp.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String email;
    private String password;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**Project.java**
```java
@Data
public class Project {
    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // MyBatis結果マッピング用（JOIN結果）
    private String ownerName;
}
```

**Task.java**
```java
@Data
public class Task {
    private Long id;
    private Long projectId;
    private String title;
    private String description;
    private TaskStatus status;  // Enum
    private Priority priority;   // Enum
    private Long assigneeId;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // JOIN結果用
    private String projectName;
    private String assigneeName;
}
```

**Comment.java**と**Tag.java**も同様に実装してください。

> **💡 ヒント**: 
> - MyBatisではJPAのような`@Entity`アノテーションは不要
> - POJOクラス（Plain Old Java Object）として実装
> - `example/STEP_35_entity_example.java`に完全な実装例があります

---

### 課題3: MyBatis Mapperインターフェースの実装

各エンティティに対応するMapperインターフェースを実装してください：

**TaskMapper.java** (`src/main/java/com/example/taskapp/mapper/`)
```java
package com.example.taskapp.mapper;

import com.example.taskapp.entity.Task;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Optional;

@Mapper
public interface TaskMapper {
    
    // TODO: 以下のメソッドを実装してください
    
    /**
     * タスク作成
     * ヒント: @Insert, @Options(useGeneratedKeys = true)
     */
    void insert(Task task);
    
    /**
     * ID検索
     * ヒント: @Select
     */
    Optional<Task> findById(Long id);
    
    /**
     * プロジェクトのタスク一覧
     * ヒント: @Select, WHERE句
     */
    List<Task> findByProjectId(Long projectId);
    
    /**
     * タスク更新
     * ヒント: @Update
     */
    void update(Task task);
    
    /**
     * タスク削除
     * ヒント: @Delete
     */
    void deleteById(Long id);
}
```

同様に以下のMapperも実装してください：
- `ProjectMapper.java`
- `CommentMapper.java`
- `TagMapper.java`
- `UserMapper.java`

> **💡 ヒント**: `example/STEP_35_mapper_example.java`に完全な実装例があります

---

### 課題4: MyBatis設定ファイル

**application.yml**にMyBatis設定を追加してください：

```yaml
mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.taskapp.entity
  configuration:
    map-underscore-to-camel-case: true
    default-fetch-size: 100
    default-statement-timeout: 30
```

**schema.sql**（STEP_34で作成したもの）を`src/main/resources/`に配置してください。

---

## ✅ チェックリスト

実装が完了したら確認してください：

- [ ] Enumクラス3つ（TaskStatus, Priority, ProjectRole）を実装
- [ ] Entityクラス5つ（User, Project, Task, Comment, Tag）を実装
- [ ] Mapperインターフェース5つを実装
- [ ] 各MapperにCRUDメソッドを定義
- [ ] application.ymlにMyBatis設定を追加
- [ ] schema.sqlを配置

---

## 🧪 動作確認

### テストコードで確認

`src/test/java/com/example/taskapp/mapper/TaskMapperTest.java`を作成：

```java
@SpringBootTest
@MybatisTest
class TaskMapperTest {
    
    @Autowired
    private TaskMapper taskMapper;
    
    @Test
    void testInsertAndFind() {
        Task task = new Task();
        task.setProjectId(1L);
        task.setTitle("テストタスク");
        task.setStatus(TaskStatus.TODO);
        task.setPriority(Priority.HIGH);
        
        taskMapper.insert(task);
        assertThat(task.getId()).isNotNull();
        
        Optional<Task> found = taskMapper.findById(task.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("テストタスク");
    }
}
```

### アプリケーション起動確認

```bash
./mvnw spring-boot:run
```

エラーなく起動すればOKです！

---

## 💡 参考実装例

- `example/STEP_35_enum_example.java` - Enum完全実装
- `example/STEP_35_entity_example.java` - Entity完全実装  
- `example/STEP_35_mapper_example.java` - Mapper完全実装
- `example/STEP_35_test_example.java` - テストコード例

---

## 📚 このステップで学んだこと

- ✅ MyBatis用POJOエンティティの実装
- ✅ Enumクラスの活用
- ✅ MyBatis Mapperインターフェースの作成
- ✅ アノテーションベースのSQL定義
- ✅ MyBatisの設定方法

---

## 🔄 Gitへのコミット

```bash
git add .
git commit -m "Step 35: エンティティとMapper実装完了

- Enum: TaskStatus, Priority, ProjectRole
- Entity: User, Project, Task, Comment, Tag
- Mapper: 各エンティティのCRUD操作"
git push origin main
```

---

## ➡️ 次のステップ

次は[Step 36: サービスとコントローラー実装](STEP_36.md)へ進みましょう！

---

お疲れさまでした！ 🎉
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
