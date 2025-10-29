# Step 11: リレーションシップ（1対多）

## 🎯 このステップの目標

- エンティティ間のリレーションシップ（関連）を理解する
- `@OneToMany`と`@ManyToOne`を使って1対多の関係を実装する
- Cascade（カスケード）の動作を理解する
- Fetch戦略（EAGER vs LAZY）を理解する
- 循環参照問題を解決する

**所要時間**: 約1時間30分

---

## 📋 事前準備

- Step 10までのカスタムクエリが理解できていること
- UserエンティティとUserRepositoryが動作していること

**Step 10をまだ完了していない場合**: [Step 10: カスタムクエリ](STEP_10.md)を先に進めてください。

---

## 💡 リレーションシップとは？

### データベースの関連

実世界のデータは互いに関連しています：

```
ユーザー (User)  ←─1対多─→  投稿 (Post)
- 1人のユーザーは複数の投稿を持つ
- 1つの投稿は1人のユーザーに属する
```

### リレーションシップの種類

| 種類 | 説明 | 例 |
|------|------|-----|
| **1対多 (One-to-Many)** | 1つのエンティティが複数のエンティティを持つ | ユーザー ← 投稿 |
| **多対1 (Many-to-One)** | 複数のエンティティが1つのエンティティに属する | 投稿 → ユーザー |
| **1対1 (One-to-One)** | 1つのエンティティが1つのエンティティを持つ | ユーザー ← プロフィール |
| **多対多 (Many-to-Many)** | 複数のエンティティが複数のエンティティを持つ | 学生 ←→ 授業 |

**このステップでは1対多（One-to-Many）を扱います。**

---

## 🚀 ステップ1: Postエンティティの作成

### 1-1. Postエンティティ

ユーザーが投稿を作成できるシステムを実装します。

**ファイルパス**: `src/main/java/com/example/hellospringboot/entity/Post.java`

```java
package com.example.hellospringboot.entity.

;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 多対1の関係（多くの投稿が1人のユーザーに属する）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"posts"})  // 循環参照を防ぐ
    private User user;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
```

### 1-2. アノテーションの解説

#### `@ManyToOne`
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
private User user;
```
- **多対1の関係**: 多くの投稿が1人のユーザーに属する
- `fetch = FetchType.LAZY`: 遅延ロード（必要になるまでUserを読み込まない）
- `@JoinColumn(name = "user_id")`: 外部キーカラム名を指定

#### `@JsonIgnoreProperties`
```java
@JsonIgnoreProperties({"posts"})
```
- JSON変換時に循環参照を防ぐ
- `user.posts.user.posts...`の無限ループを回避

#### `@PrePersist`
```java
@PrePersist
protected void onCreate() {
    this.createdAt = LocalDateTime.now();
}
```
- エンティティが保存される直前に実行されるメソッド
- 作成日時を自動設定

---

## 🚀 ステップ2: Userエンティティの更新

### 2-1. 1対多の関係を追加

`User.java`に投稿のリストを追加します。

**ファイルパス**: `src/main/java/com/example/hellospringboot/entity/User.java`

既存の`User.java`に以下を追加：

```java
package com.example.hellospringboot.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column
    private Integer age;

    // 1対多の関係（1人のユーザーが複数の投稿を持つ）
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"user"})  // 循環参照を防ぐ
    @Builder.Default  // Builderパターンで初期化
    private List<Post> posts = new ArrayList<>();

    // ヘルパーメソッド: 投稿を追加
    public void addPost(Post post) {
        posts.add(post);
        post.setUser(this);
    }

    // ヘルパーメソッド: 投稿を削除
    public void removePost(Post post) {
        posts.remove(post);
        post.setUser(null);
    }
}
```

### 2-2. @OneToManyの解説

```java
@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Post> posts = new ArrayList<>();
```

#### `mappedBy = "user"`
- リレーションシップの所有者は`Post`エンティティの`user`フィールド
- 外部キーは`posts`テーブルの`user_id`

#### `cascade = CascadeType.ALL`
- ユーザーに対する操作を投稿にも伝播
- `PERSIST`: 保存を伝播
- `MERGE`: 更新を伝播
- `REMOVE`: 削除を伝播
- `ALL`: すべて伝播

**例**:
```java
User user = new User();
Post post = new Post();
user.addPost(post);
userRepository.save(user);  // userとpostの両方が保存される
```

#### `orphanRemoval = true`
- 親（User）から削除された子（Post）を自動的にデータベースから削除

**例**:
```java
user.getPosts().remove(post);  // postsテーブルからも削除される
```

---

## 🚀 ステップ3: PostRepositoryとPostServiceの作成

### 3-1. PostRepository

**ファイルパス**: `src/main/java/com/example/hellospringboot/repository/PostRepository.java`

```java
package com.example.hellospringboot.repository;

import com.example.hellospringboot.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // ユーザーIDで投稿を検索
    List<Post> findByUserId(Long userId);

    // タイトルで部分一致検索
    List<Post> findByTitleContaining(String keyword);

    // ユーザーIDと作成日時で並び替え
    List<Post> findByUserIdOrderByCreatedAtDesc(Long userId);
}
```

### 3-2. PostService

**ファイルパス**: `src/main/java/com/example/hellospringboot/service/PostService.java`

```java
package com.example.hellospringboot.service;

import com.example.hellospringboot.entity.Post;
import com.example.hellospringboot.entity.User;
import com.example.hellospringboot.repository.PostRepository;
import com.example.hellospringboot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    /**
     * 投稿を作成
     */
    @Transactional
    public Post createPost(Long userId, String title, String content) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Post post = Post.builder()
                .title(title)
                .content(content)
                .user(user)
                .build();

        return postRepository.save(post);
    }

    /**
     * 全投稿を取得
     */
    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    /**
     * IDで投稿を取得
     */
    public Optional<Post> getPostById(Long id) {
        return postRepository.findById(id);
    }

    /**
     * ユーザーの投稿一覧を取得
     */
    public List<Post> getPostsByUserId(Long userId) {
        return postRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 投稿を更新
     */
    @Transactional
    public Optional<Post> updatePost(Long id, String title, String content) {
        return postRepository.findById(id)
                .map(post -> {
                    post.setTitle(title);
                    post.setContent(content);
                    return postRepository.save(post);
                });
    }

    /**
     * 投稿を削除
     */
    @Transactional
    public boolean deletePost(Long id) {
        if (postRepository.existsById(id)) {
            postRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
```

---

## 🚀 ステップ4: PostControllerの作成

### 4-1. PostController

**ファイルパス**: `src/main/java/com/example/hellospringboot/controller/PostController.java`

```java
package com.example.hellospringboot.controller;

import com.example.hellospringboot.entity.Post;
import com.example.hellospringboot.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * 投稿作成
     * POST /api/posts
     */
    @PostMapping
    public ResponseEntity<Post> createPost(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        String title = request.get("title").toString();
        String content = request.get("content").toString();

        Post post = postService.createPost(userId, title, content);
        return ResponseEntity.status(HttpStatus.CREATED).body(post);
    }

    /**
     * 全投稿取得
     * GET /api/posts
     */
    @GetMapping
    public ResponseEntity<List<Post>> getAllPosts() {
        List<Post> posts = postService.getAllPosts();
        return ResponseEntity.ok(posts);
    }

    /**
     * IDで投稿取得
     * GET /api/posts/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Post> getPostById(@PathVariable Long id) {
        return postService.getPostById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * ユーザーの投稿一覧取得
     * GET /api/users/{userId}/posts
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Post>> getPostsByUserId(@PathVariable Long userId) {
        List<Post> posts = postService.getPostsByUserId(userId);
        return ResponseEntity.ok(posts);
    }

    /**
     * 投稿更新
     * PUT /api/posts/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Post> updatePost(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String title = request.get("title");
        String content = request.get("content");

        return postService.updatePost(id, title, content)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 投稿削除
     * DELETE /api/posts/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        if (postService.deletePost(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
```

---

## ✅ ステップ5: 動作確認

### 5-1. アプリケーション起動

アプリケーションを起動すると、`posts`テーブルが自動作成されます。

### 5-2. ユーザー作成

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Taro Yamada",
    "email": "taro@example.com",
    "age": 30
  }'
```

IDが1と仮定します。

### 5-3. 投稿作成

```bash
curl -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "title": "My First Post",
    "content": "This is my first post using Spring Boot!"
  }'

curl -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "title": "Learning JPA",
    "content": "JPA relationships are very powerful!"
  }'
```

### 5-4. 投稿一覧取得

```bash
curl http://localhost:8080/api/posts
```

**期待される結果**:
```json
[
  {
    "id": 1,
    "title": "My First Post",
    "content": "This is my first post using Spring Boot!",
    "createdAt": "2025-10-27T10:30:00",
    "user": {
      "id": 1,
      "name": "Taro Yamada",
      "email": "taro@example.com",
      "age": 30
    }
  },
  ...
]
```

### 5-5. 特定ユーザーの投稿取得

```bash
curl http://localhost:8080/api/posts/user/1
```

### 5-6. DBeaverで確認

```sql
SELECT * FROM posts;

-- JOINでユーザー情報も表示
SELECT p.id, p.title, u.name as user_name 
FROM posts p 
JOIN users u ON p.user_id = u.id;
```

---

## 🚀 ステップ6: Fetch戦略の理解

### 6-1. LAZY vs EAGER

| Fetch戦略 | 説明 | 使用場面 |
|-----------|------|----------|
| **LAZY（遅延）** | 関連エンティティを必要になるまで読み込まない | デフォルト推奨 |
| **EAGER（即時）** | 関連エンティティを即座に読み込む | 必ず使うデータ |

### 6-2. LAZYの動作

```java
@ManyToOne(fetch = FetchType.LAZY)
private User user;
```

**実行されるSQL**:
```sql
-- 投稿を取得
SELECT * FROM posts WHERE id = 1;

-- user.getName()を呼び出したときにUserを取得
SELECT * FROM users WHERE id = ?;
```

### 6-3. EAGERの動作

```java
@ManyToOne(fetch = FetchType.EAGER)
private User user;
```

**実行されるSQL**:
```sql
-- 投稿を取得するときに一緒にUserも取得
SELECT p.*, u.* 
FROM posts p 
LEFT JOIN users u ON p.user_id = u.id 
WHERE p.id = 1;
```

### 6-4. N+1問題

**問題**: LAZYで全投稿を取得してユーザー名を表示

```java
List<Post> posts = postRepository.findAll();  // 1クエリ
for (Post post : posts) {
    System.out.println(post.getUser().getName());  // Nクエリ
}
// 合計 N+1 クエリが発生！
```

**解決策1**: `@EntityGraph`を使用

```java
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    @EntityGraph(attributePaths = {"user"})
    List<Post> findAll();
}
```

**解決策2**: JPQLでJOIN FETCH

```java
@Query("SELECT p FROM Post p JOIN FETCH p.user")
List<Post> findAllWithUser();
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: コメント機能

投稿に対するコメント機能を追加してください。

**要件**:
- `Comment`エンティティを作成
- 1つの投稿に複数のコメント（1対多）
- コメント作成・取得・削除API

**ヒント**:
```java
@Entity
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String content;
    
    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
```

### チャレンジ 2: いいね機能

投稿へのいいね（Like）機能を実装してください。

**要件**:
- ユーザーは投稿に1回だけいいねできる
- いいね数を取得できる

### チャレンジ 3: カテゴリ機能

投稿にカテゴリを追加してください。

**要件**:
- 1つの投稿は1つのカテゴリに属する
- カテゴリ別に投稿を検索できる

---

## 🐛 トラブルシューティング

### "Could not write JSON: Infinite recursion"

**原因**: 循環参照

```java
User → posts → user → posts → ...
```

**解決策**: `@JsonIgnoreProperties`を使用

```java
@OneToMany(mappedBy = "user")
@JsonIgnoreProperties({"user"})
private List<Post> posts;
```

### "LazyInitializationException"

**エラー**: `could not initialize proxy - no Session`

**原因**: トランザクション外でLAZYロードしようとした

**解決策**:
```java
@Transactional(readOnly = true)
public Post getPostWithUser(Long id) {
    Post post = postRepository.findById(id).orElseThrow();
    post.getUser().getName();  // トランザクション内でアクセス
    return post;
}
```

### CascadeTypeの誤用

**症状**: ユーザーを削除したら投稿も削除された

**原因**: `CascadeType.REMOVE`または`CascadeType.ALL`

**解決策**: 必要なCascadeTypeだけ指定

```java
@OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
private List<Post> posts;
```

---

## 📚 このステップで学んだこと

- ✅ 1対多（@OneToMany）と多対1（@ManyToOne）の関係
- ✅ `@JoinColumn`で外部キーを指定
- ✅ `mappedBy`でリレーションシップの所有者を指定
- ✅ Cascade（カスケード）の種類と動作
- ✅ `orphanRemoval`で孤立エンティティを削除
- ✅ Fetch戦略（LAZY vs EAGER）
- ✅ N+1問題と解決方法
- ✅ `@JsonIgnoreProperties`で循環参照を防ぐ

---

## 💡 補足: リレーションシップ設計のベストプラクティス

### 双方向 vs 単方向

**双方向**（今回の実装）:
```java
// User側
@OneToMany(mappedBy = "user")
private List<Post> posts;

// Post側
@ManyToOne
private User user;
```

**単方向**（シンプル）:
```java
// Post側のみ
@ManyToOne
private User user;
```

**推奨**: 必要な場合のみ双方向にする

### Cascadeの推奨設定

```java
// 親が子のライフサイクルを完全に管理する場合
@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)

// 子が独立している場合
@OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})

// 推奨しない
@OneToMany(mappedBy = "user")  // Cascadeなし
```

### Fetch戦略の推奨

- **@ManyToOne, @OneToOne**: `FetchType.LAZY`
- **@OneToMany, @ManyToMany**: デフォルトで`LAZY`

**理由**: パフォーマンス最適化、必要なデータだけ読み込む

---

## 🔄 Gitへのコミットとレビュー依頼

進捗を記録してレビューを受けましょう：

```bash
git add .
git commit -m "Step 11: リレーションシップ実装完了（@OneToMany/@ManyToOne、Cascade、Fetch戦略）"
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

レビューが完了したら、[Step 12: MyBatisの基礎](../phase3/STEP_12.md)へ進みましょう！

次のステップでは、MyBatisを使ったSQLの直接制御について学びます。
JPAとは異なるアプローチでのデータベース操作を習得します。Phase 3のスタートです！

---

お疲れさまでした！ 🎉

リレーションシップはデータベース設計の核心部分です。
@OneToManyと@ManyToOneを使いこなせるようになると、複雑なデータ構造も扱えるようになります！
