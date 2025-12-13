# Step 11: リレーションシップ（1対多）

## 🎯 このステップの目標

- エンティティ間の関連（リレーションシップ）を理解できる
- `@OneToMany`と`@ManyToOne`を使って1対多の関連を実装できる
- 双方向関連と単方向関連の違いを理解できる
- カスケード操作とFetch戦略を理解できる
- 実務でよく使う親子関係のパターンを実装できる

**所要時間**: 約60分

---

## 📋 事前準備

- [Step 10](STEP_10.md)が完了していること
- `Product`エンティティが作成されていること
- MySQLコンテナが起動していること

---

## 🧩 リレーションシップとは

### エンティティ間の関連

実世界のデータは互いに関連しています。例えば：

- **カテゴリ**は複数の**商品**を持つ
- **商品**は1つの**カテゴリ**に属する

```
┌──────────────────┐         ┌──────────────────┐
│   Category       │         │    Product       │
│  （カテゴリ）     │    1    │   （商品）        │
│                  │────────>│                  │
│  - id            │    N    │  - id            │
│  - name          │         │  - name          │
│                  │<────────│  - categoryId    │
└──────────────────┘         └──────────────────┘
   1つのカテゴリ              複数の商品
```

### リレーションシップの種類

| 種類 | 説明 | 例 |
|---|---|---|
| **1対1** (One-to-One) | 1つのAに1つのBが対応 | ユーザーとプロフィール |
| **1対多** (One-to-Many) | 1つのAに複数のBが対応 | カテゴリと商品 ✅ |
| **多対多** (Many-to-Many) | 複数のAに複数のBが対応 | 商品とタグ |

このステップでは、最も一般的な**1対多**を実装します。

**実装例**:
- **カテゴリと商品**: 1つのカテゴリに複数の商品
- **ユーザーと投稿**: 1人のユーザーが複数の投稿を作成

---

## 🚀 ステップ1: UserエンティティをJPAエンティティに変更

Phase 1で作成した`User`クラスをJPAエンティティに変更し、`Post`との1対多のリレーションシップを実装します。

### 1-1. UserエンティティをJPAエンティティに変更

**ファイルパス**: `src/main/java/com/example/hellospringboot/User.java`

```java
package com.example.hellospringboot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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
    
    @Column(nullable = false, unique = true, length = 255)
    private String email;
    
    private Integer age;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore  // JSONシリアライズ時に無限ループを防ぐ
    @Builder.Default
    private List<Post> posts = new ArrayList<>();
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // ヘルパーメソッド：投稿を追加
    public void addPost(Post post) {
        posts.add(post);
        post.setUser(this);
    }
    
    // ヘルパーメソッド：投稿を削除
    public void removePost(Post post) {
        posts.remove(post);
        post.setUser(null);
    }
}
```

### 1-2. Postエンティティの作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/Post.java`

```java
package com.example.hellospringboot;

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
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "posts"})
    private User user;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### 1-3. コードの解説

#### `@Builder.Default`（Userエンティティ）

Lombokの`@Builder`使用時に、初期化式を保持するために必要です。

```java
@Builder.Default
private List<Post> posts = new ArrayList<>();
```

**注意**: これがないと、Builderで生成したオブジェクトの`posts`が`null`になります。

#### `@JsonIgnore`（Userエンティティ）

User→Postsの無限ループを防ぎます。

```java
@JsonIgnore
private List<Post> posts;
```

これにより、`GET /api/users`でユーザー情報を取得する際に、投稿リストは含まれません。

#### `@JsonIgnoreProperties`（Postエンティティ）

Post→Userのシリアライズ時に以下を無視します：

- `hibernateLazyInitializer`: Hibernateの遅延ロードプロキシ
- `handler`: Hibernateのハンドラー
- `posts`: User側の投稿リスト（循環参照防止）

```java
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "posts"})
private User user;
```

これにより、`GET /api/posts`で投稿を取得する際に、投稿に紐づくユーザー情報は含まれますが、そのユーザーの投稿リストは含まれません（無限ループ防止）。

---

## 🚀 ステップ2: Categoryエンティティの作成（オプション）

商品のカテゴリ管理も実装する場合は、以下のエンティティを作成します。

### 2-1. エンティティクラスを作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/Category.java`

```java
package com.example.hellospringboot;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // 1対多の関連
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Product> products = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // ヘルパーメソッド
    public void addProduct(Product product) {
        products.add(product);
        product.setCategory(this);
    }
    
    public void removeProduct(Product product) {
        products.remove(product);
        product.setCategory(null);
    }
}
```

### 1-2. コードの解説

#### `@OneToMany`

1対多の関連を定義します（カテゴリ側から見て「1つのカテゴリに複数の商品」）。

```java
@OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Product> products = new ArrayList<>();
```

**属性**:

| 属性 | 説明 |
|---|---|
| `mappedBy = "category"` | 関連の所有者がProduct側の`category`フィールドであることを示す |
| `cascade = CascadeType.ALL` | カテゴリの操作（保存、削除など）を商品にも伝播させる |
| `orphanRemoval = true` | カテゴリから削除された商品を物理削除する |

#### カスケードタイプ

| タイプ | 説明 |
|---|---|
| `PERSIST` | 保存時に関連エンティティも保存 |
| `MERGE` | 更新時に関連エンティティも更新 |
| `REMOVE` | 削除時に関連エンティティも削除 |
| `REFRESH` | リフレッシュ時に関連エンティティもリフレッシュ |
| `DETACH` | デタッチ時に関連エンティティもデタッチ |
| `ALL` | 上記全て |

#### `@JsonManagedReference`

JSON変換時の循環参照を防ぎます（後述）。

#### ヘルパーメソッド

双方向関連を正しく維持するためのメソッドです。

```java
public void addProduct(Product product) {
    products.add(product);          // Categoryのリストに追加
    product.setCategory(this);      // Productのcategoryフィールドをセット
}
```

---

## 🚀 ステップ2: Productエンティティの更新

### 2-1. 多対1の関連を追加

`Product.java`に以下のフィールドを**追加**します：

```java
package com.example.hellospringboot;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private Integer price;
    
    @Column(nullable = false)
    private Integer stock = 0;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // 多対1の関連を追加
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @JsonBackReference
    private Category category;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### 2-2. コードの解説

#### `@ManyToOne`

多対1の関連を定義します（商品側から見て「複数の商品が1つのカテゴリに属する」）。

```java
@ManyToOne(fetch = FetchType.LAZY)
private Category category;
```

#### `@JoinColumn`

外部キーカラムを指定します。

```java
@JoinColumn(name = "category_id")
```

これにより、`products`テーブルに`category_id`カラムが作成されます。

#### Fetch戦略

| 戦略 | 説明 | SQLの実行タイミング |
|---|---|---|
| `EAGER` | 即座にロード | エンティティ取得時に同時にJOIN |
| `LAZY` | 遅延ロード | 実際にアクセスしたときにSELECT ✅ 推奨 |

**推奨**: `LAZY`を使うことで、不要なデータを取得しないようにします（N+1問題の回避）。

#### `@JsonBackReference`

`@JsonManagedReference`とペアで使い、JSON変換時の循環参照を防ぎます。

```
Category → products → category → products → ... (無限ループ)
```

**解決策**:
- `Category.products`に`@JsonManagedReference`（親側、シリアライズされる）
- `Product.category`に`@JsonBackReference`（子側、シリアライズされない）

---

## 🚀 ステップ3: UserRepositoryとPostRepositoryの作成

### 3-1. UserRepositoryの作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/UserRepository.java`

```java
package com.example.hellospringboot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // メールアドレスでユーザーを検索
    Optional<User> findByEmail(String email);
    
    // 名前で検索（部分一致）
    List<User> findByNameContaining(String keyword);
}
```

### 3-2. PostRepositoryの作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/PostRepository.java`

```java
package com.example.hellospringboot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    // ユーザーIDで投稿を取得
    List<Post> findByUserId(Long userId);
    
    // タイトルで検索
    List<Post> findByTitleContaining(String keyword);
}
```

---

## 🚀 ステップ4: UserControllerとPostControllerの作成

### 4-1. UserControllerの更新

Phase 1で作成した`UserController`をJPAリポジトリを使うように更新します。

**ファイルパス**: `src/main/java/com/example/hellospringboot/UserController.java`

```java
package com.example.hellospringboot;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    // CREATE - ユーザーを登録
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User saved = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // READ - 全ユーザーを取得
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    // READ - IDでユーザーを取得
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // READ - ユーザーの投稿一覧を取得（リレーション）
    @GetMapping("/{id}/posts")
    public ResponseEntity<List<Post>> getUserPosts(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        List<Post> posts = postRepository.findByUserId(id);
        return ResponseEntity.ok(posts);
    }

    // UPDATE - ユーザーを更新
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @RequestBody User userDetails) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setName(userDetails.getName());
                    user.setEmail(userDetails.getEmail());
                    user.setAge(userDetails.getAge());
                    User updated = userRepository.save(user);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE - ユーザーを削除
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
    // SEARCH - 名前で検索
    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String keyword) {
        List<User> users = userRepository.findByNameContaining(keyword);
        return ResponseEntity.ok(users);
    }
}
```

### 4-2. PostControllerの作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/PostController.java`

```java
package com.example.hellospringboot;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    
    // CREATE - 投稿を作成
    @PostMapping
    public ResponseEntity<Post> createPost(@RequestBody PostRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));
        
        Post post = Post.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .user(user)
                .build();
        
        Post saved = postRepository.save(post);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
    
    // READ - 全投稿を取得
    @GetMapping
    public ResponseEntity<List<Post>> getAllPosts() {
        List<Post> posts = postRepository.findAll();
        return ResponseEntity.ok(posts);
    }
    
    // READ - IDで投稿を取得
    @GetMapping("/{id}")
    public ResponseEntity<Post> getPostById(@PathVariable Long id) {
        return postRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // READ - ユーザーの投稿一覧を取得
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Post>> getPostsByUserId(@PathVariable Long userId) {
        List<Post> posts = postRepository.findByUserId(userId);
        return ResponseEntity.ok(posts);
    }
    
    // UPDATE - 投稿を更新
    @PutMapping("/{id}")
    public ResponseEntity<Post> updatePost(
            @PathVariable Long id,
            @RequestBody PostRequest request) {
        return postRepository.findById(id)
                .map(post -> {
                    post.setTitle(request.getTitle());
                    post.setContent(request.getContent());
                    Post updated = postRepository.save(post);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    // DELETE - 投稿を削除
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        if (!postRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        postRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
    // SEARCH - タイトルで検索
    @GetMapping("/search")
    public ResponseEntity<List<Post>> searchPosts(@RequestParam String keyword) {
        List<Post> posts = postRepository.findByTitleContaining(keyword);
        return ResponseEntity.ok(posts);
    }
}
```

### 4-3. PostRequestの作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/PostRequest.java`

```java
package com.example.hellospringboot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostRequest {
    private Long userId;
    private String title;
    private String content;
}
```

---

## 🚀 ステップ5: CategoryRepositoryとServiceの作成（オプション）

**ファイルパス**: `src/main/java/com/example/hellospringboot/CategoryRepository.java`

```java
package com.example.hellospringboot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    // カテゴリ名で検索
    Category findByName(String name);
    
    // 商品数が多い順にカテゴリを取得
    @Query("SELECT c FROM Category c LEFT JOIN c.products p GROUP BY c ORDER BY COUNT(p) DESC")
    List<Category> findCategoriesOrderByProductCount();
}
```

### 3-2. CategoryServiceの作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/CategoryService.java`

```java
package com.example.hellospringboot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryService {
    
    private final CategoryRepository categoryRepository;
    
    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }
    
    @Transactional
    public Category save(Category category) {
        return categoryRepository.save(category);
    }
    
    @Transactional
    public Category update(Long id, Category categoryDetails) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        category.setName(categoryDetails.getName());
        category.setDescription(categoryDetails.getDescription());
        
        return categoryRepository.save(category);
    }
    
    @Transactional
    public void deleteById(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("Category not found");
        }
        categoryRepository.deleteById(id);
    }
}
```

### 3-3. CategoryControllerの作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/CategoryController.java`

```java
package com.example.hellospringboot;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    
    private final CategoryService categoryService;
    
    @GetMapping
    public List<Category> getAllCategories() {
        return categoryService.findAll();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Long id) {
        return categoryService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        Category saved = categoryService.save(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(
            @PathVariable Long id,
            @RequestBody Category categoryDetails) {
        try {
            Category updated = categoryService.update(id, categoryDetails);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        try {
            categoryService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
```

---

## 🚀 ステップ4: ProductServiceの更新

### 4-1. カテゴリ付きで商品を作成

`ProductService.java`に以下のメソッドを**追加**:

```java
private final CategoryRepository categoryRepository;  // 追加

@Transactional
public Product saveWithCategory(Product product, Long categoryId) {
    if (categoryId != null) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        product.setCategory(category);
    }
    return productRepository.save(product);
}
```

### 4-2. ProductControllerの更新

`ProductController.java`の`createProduct`メソッドを**修正**:

```java
@PostMapping
public ResponseEntity<Product> createProduct(
        @RequestBody Product product,
        @RequestParam(required = false) Long categoryId) {
    Product saved = productService.saveWithCategory(product, categoryId);
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
}
```

---

## ✅ ステップ6: 動作確認

### 6-1. アプリケーションの再起動

```bash
./mvnw clean install
./mvnw spring-boot:run
```

### 6-2. ユーザーを作成

```bash
# ユーザー1: 田中太郎
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "田中太郎",
    "email": "tanaka@example.com",
    "age": 30
  }'

# ユーザー2: 佐藤花子
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "佐藤花子",
    "email": "sato@example.com",
    "age": 25
  }'
```

**期待される結果**（ユーザー1）:

```json
{
  "id": 1,
  "name": "田中太郎",
  "email": "tanaka@example.com",
  "age": 30,
  "createdAt": "2025-12-13T12:00:00.123456",
  "updatedAt": "2025-12-13T12:00:00.123456"
}
```

**返却されたIDをメモ**してください（以下の例では`id=1`と`id=2`とします）。

### 6-3. 投稿を作成

```bash
# ユーザー1の投稿1
curl -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "title": "Spring Bootの学習",
    "content": "Spring Bootの学習を始めました。とても楽しいです。"
  }'

# ユーザー1の投稿2
curl -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "title": "データベース連携",
    "content": "MySQLとの連携ができました！"
  }'

# ユーザー2の投稿
curl -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 2,
    "title": "JPAのリレーション",
    "content": "1対多のリレーションシップを実装しました。"
  }'
```

**期待される結果**（投稿1）:

```json
{
  "id": 1,
  "title": "Spring Bootの学習",
  "content": "Spring Bootの学習を始めました。とても楽しいです。",
  "user": {
    "id": 1,
    "name": "田中太郎",
    "email": "tanaka@example.com",
    "age": 30,
    "createdAt": "2025-12-13T12:00:00.123456",
    "updatedAt": "2025-12-13T12:00:00.123456"
  },
  "createdAt": "2025-12-13T12:01:00.123456",
  "updatedAt": "2025-12-13T12:01:00.123456"
}
```

**ポイント**:
- 投稿のレスポンスには、紐づくユーザー情報が含まれます
- ただし、ユーザー情報の中に`posts`フィールドは含まれません（`@JsonIgnore`の効果）

### 6-4. ユーザーの投稿一覧を取得

```bash
# ユーザー1の投稿一覧
curl http://localhost:8080/api/users/1/posts
```

**期待される結果**:

```json
[
  {
    "id": 1,
    "title": "Spring Bootの学習",
    "content": "Spring Bootの学習を始めました。とても楽しいです。",
    "user": {
      "id": 1,
      "name": "田中太郎",
      "email": "tanaka@example.com",
      "age": 30,
      "createdAt": "2025-12-13T12:00:00.123456",
      "updatedAt": "2025-12-13T12:00:00.123456"
    },
    "createdAt": "2025-12-13T12:01:00.123456",
    "updatedAt": "2025-12-13T12:01:00.123456"
  },
  {
    "id": 2,
    "title": "データベース連携",
    "content": "MySQLとの連携ができました！",
    "user": {
      "id": 1,
      "name": "田中太郎",
      "email": "tanaka@example.com",
      "age": 30,
      "createdAt": "2025-12-13T12:00:00.123456",
      "updatedAt": "2025-12-13T12:00:00.123456"
    },
    "createdAt": "2025-12-13T12:02:00.123456",
    "updatedAt": "2025-12-13T12:02:00.123456"
  }
]
```

### 6-5. MySQLで確認

```bash
docker compose exec mysql mysql -u springuser -pspringpass hello_spring_boot -e "SELECT * FROM users;"
docker compose exec mysql mysql -u springuser -pspringpass hello_spring_boot -e "SELECT * FROM posts;"
```

**期待される結果**（posts）:

```
+----+-------------------------+----------------------------------------------+---------+----------------------------+----------------------------+
| id | title                   | content                                      | user_id | created_at                 | updated_at                 |
+----+-------------------------+----------------------------------------------+---------+----------------------------+----------------------------+
|  1 | Spring Bootの学習       | Spring Bootの学習を始めました。              |       1 | 2025-12-13 12:01:00.123456 | 2025-12-13 12:01:00.123456 |
|  2 | データベース連携         | MySQLとの連携ができました！                  |       1 | 2025-12-13 12:02:00.123456 | 2025-12-13 12:02:00.123456 |
|  3 | JPAのリレーション        | 1対多のリレーションシップを実装しました。    |       2 | 2025-12-13 12:03:00.123456 | 2025-12-13 12:03:00.123456 |
+----+-------------------------+----------------------------------------------+---------+----------------------------+----------------------------+
```

`user_id`カラムが追加され、値が設定されていることを確認してください。

### 6-6. カスケード削除の確認

ユーザーを削除すると、そのユーザーの投稿も自動的に削除されます（`cascade = CascadeType.ALL`の効果）。

```bash
# ユーザー2を削除
curl -X DELETE http://localhost:8080/api/users/2

# 投稿一覧を確認（ユーザー2の投稿が削除されているはず）
curl http://localhost:8080/api/posts
```

**期待される結果**: ユーザー2の投稿（id=3）が削除されています。

---

## ✅ ステップ7: カテゴリと商品の動作確認（オプション）

CategoryとProductのリレーションも実装した場合は、以下の手順で動作確認を行います。

### 7-1. カテゴリを作成

```bash
# カテゴリ1: パソコン
curl -X POST http://localhost:8080/api/categories \
  -H "Content-Type: application/json" \
  -d '{
    "name": "パソコン",
    "description": "デスクトップ・ノートPC"
  }'

# カテゴリ2: 周辺機器
curl -X POST http://localhost:8080/api/categories \
  -H "Content-Type: application/json" \
  -d '{
    "name": "周辺機器",
    "description": "マウス・キーボードなど"
  }'
```

**返却されたIDをメモ**してください（以下の例では`id=1`と`id=2`とします）。

### 5-3. カテゴリ付きで商品を作成

```bash
# パソコンカテゴリの商品
curl -X POST "http://localhost:8080/api/products?categoryId=1" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "ノートPC",
    "description": "高性能ノートパソコン",
    "price": 150000,
    "stock": 10
  }'

curl -X POST "http://localhost:8080/api/products?categoryId=1" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "デスクトップPC",
    "description": "ゲーミングPC",
    "price": 200000,
    "stock": 5
  }'

# 周辺機器カテゴリの商品
curl -X POST "http://localhost:8080/api/products?categoryId=2" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "マウス",
    "description": "ワイヤレスマウス",
    "price": 3000,
    "stock": 50
  }'

curl -X POST "http://localhost:8080/api/products?categoryId=2" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "キーボード",
    "description": "メカニカルキーボード",
    "price": 12000,
    "stock": 20
  }'
```

### 5-4. カテゴリと商品を一緒に取得

```bash
curl http://localhost:8080/api/categories/1
```

**期待される結果**:

```json
{
  "id": 1,
  "name": "パソコン",
  "description": "デスクトップ・ノートPC",
  "createdAt": "2025-12-13T12:00:00",
  "updatedAt": "2025-12-13T12:00:00",
  "products": [
    {
      "id": 1,
      "name": "ノートPC",
      "description": "高性能ノートパソコン",
      "price": 150000,
      "stock": 10,
      "createdAt": "2025-12-13T12:01:00",
      "updatedAt": "2025-12-13T12:01:00"
    },
    {
      "id": 2,
      "name": "デスクトップPC",
      "description": "ゲーミングPC",
      "price": 200000,
      "stock": 5,
      "createdAt": "2025-12-13T12:02:00",
      "updatedAt": "2025-12-13T12:02:00"
    }
  ]
}
```

### 5-5. MySQLで確認

```bash
docker compose exec mysql mysql -u springuser -pspringpass hello_spring_boot -e "SELECT * FROM products;"
```

`category_id`カラムが追加され、値が設定されていることを確認してください。

---

## 🎨 チャレンジ課題

基本が理解できたら、以下にチャレンジしてみましょう：

### チャレンジ 1: カテゴリの商品数を取得

カテゴリごとの商品数をカウントするエンドポイントを作成してください。

**ヒント**:

```java
@Query("SELECT c.name, COUNT(p) FROM Category c LEFT JOIN c.products p GROUP BY c")
List<Object[]> getCategoryProductCounts();
```

### チャレンジ 2: 多対多の関連

商品とタグの多対多関連を実装してください。

**Tagエンティティ**:

```java
@Entity
@Table(name = "tags")
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    @ManyToMany(mappedBy = "tags")
    private Set<Product> products = new HashSet<>();
}
```

**Productエンティティに追加**:

```java
@ManyToMany
@JoinTable(
    name = "product_tags",
    joinColumns = @JoinColumn(name = "product_id"),
    inverseJoinColumns = @JoinColumn(name = "tag_id")
)
private Set<Tag> tags = new HashSet<>();
```

### チャレンジ 3: 1対1の関連

商品に1つの詳細情報（ProductDetail）を持たせてください。

**ProductDetailエンティティ**:

```java
@Entity
public class ProductDetail {
    @Id
    private Long id;
    
    private String manufacturer;
    private String warranty;
    
    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private Product product;
}
```

---

## 🐛 トラブルシューティング

### エラー: "LazyInitializationException"

**原因**: トランザクション外で遅延ロードしようとしている

**解決策1**: `@Transactional`を付ける

```java
@Transactional(readOnly = true)
public Category getCategoryWithProducts(Long id) {
    Category category = categoryRepository.findById(id).orElseThrow();
    category.getProducts().size();  // 遅延ロードを強制実行
    return category;
}
```

**解決策2**: EAGER fetchに変更（非推奨）

```java
@OneToMany(fetch = FetchType.EAGER)
```

**解決策3**: JOIN FETCHを使用

```java
@Query("SELECT c FROM Category c LEFT JOIN FETCH c.products WHERE c.id = :id")
Category findByIdWithProducts(@Param("id") Long id);
```

### 循環参照でJSONシリアライズエラー

**エラーメッセージ**:

```
com.fasterxml.jackson.databind.exc.InvalidDefinitionException: 
No serializer found for class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor
```

**原因**: `@JsonManagedReference`と`@JsonBackReference`が付いていない、またはHibernate遅延ロードプロキシがJSONシリアライズされようとしている

**解決策1**: `@JsonManagedReference`と`@JsonBackReference`を使用（推奨）

```java
// Category
@JsonManagedReference
private List<Product> products;

// Product
@JsonBackReference
private Category category;
```

**解決策2**: `@JsonIgnoreProperties`を使用

```java
// Product
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "category_id")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "products"})
private Category category;
```

**解決策3**: `@JsonIgnore`で完全に無視（単方向のみ必要な場合）

```java
// Product
@JsonIgnore
private Category category;
```

**ポイント**:
- `hibernateLazyInitializer`と`handler`を無視することで、Hibernateプロキシのシリアライズエラーを防げます
- 循環参照を防ぐため、親側（`products`）も無視リストに追加します

### カテゴリを削除すると商品も削除される

**原因**: `cascade = CascadeType.ALL`と`orphanRemoval = true`

**対策**:

商品を残したい場合は、カスケードを変更：

```java
@OneToMany(mappedBy = "category", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
private List<Product> products;
```

---

## 📚 このステップで学んだこと

- ✅ `@OneToMany`と`@ManyToOne`で1対多の関連を実装した
- ✅ 双方向関連と単方向関連の違いを理解した
- ✅ カスケード操作で親の変更を子に伝播させる方法を学んだ
- ✅ Fetch戦略（EAGERとLAZY）の使い分けを理解した
- ✅ `@JsonManagedReference`と`@JsonBackReference`で循環参照を防いだ
- ✅ 外部キー（`@JoinColumn`）の設定方法を学んだ
- ✅ ヘルパーメソッドで双方向関連を正しく維持する方法を学んだ
- ✅ 実務でよく使うカテゴリと商品の親子関係を実装した

---

## 💡 補足: エンティティ設計のベストプラクティス

### 双方向 vs 単方向

| 種類 | メリット | デメリット |
|---|---|---|
| **双方向** | 両側からアクセス可能 | 循環参照に注意、保守が複雑 |
| **単方向** | シンプル、循環参照なし | 片側からしかアクセスできない |

**推奨**: 必要な場合のみ双方向にする。

### N+1問題

```java
List<Category> categories = categoryRepository.findAll();
for (Category category : categories) {
    category.getProducts().size();  // ❌ カテゴリごとにSELECT発行（N+1問題）
}
```

**解決策**: JOIN FETCHを使用

```java
@Query("SELECT c FROM Category c LEFT JOIN FETCH c.products")
List<Category> findAllWithProducts();
```

---

## ➡️ 次のステップ

Phase 2は完了です！お疲れ様でした！

[Phase 3: MyBatisによるSQL制御](../phase3/STEP_12.md)へ進みましょう。

次のPhaseでは、Spring Data JPAとは異なるアプローチで、SQLを直接制御できるMyBatisを学びます。
