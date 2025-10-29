# Step 20: ロギング

## 🎯 このステップの目標

- Phase 4で学んだことを振り返る
- 実践的な演習を通じて知識を定着させる
- アーキテクチャとベストプラクティスを統合する

**所要時間**: 約2時間

---

## 📋 Phase 4の振り返り

### Step 15: レイヤードアーキテクチャとDTOパターン

**学んだこと**:
- ✅ Presentation / Business Logic / Data Access レイヤー
- ✅ DTOパターンでエンティティとAPIを分離
- ✅ Request DTO / Response DTO
- ✅ MapperクラスとMapStruct

**重要ポイント**:
```java
Controller (UserResponse) → Service (Entity) → Repository (Entity) → DB
         ←                 ←                  ←                    ←
```

### Step 16: DI/IoCコンテナの深掘り

**学んだこと**:
- ✅ 依存性注入（DI）とは何か、なぜ必要か
- ✅ `@Component`, `@Service`, `@Repository`の違いと使い分け
- ✅ コンストラクタインジェクション vs フィールドインジェクション
- ✅ Bean のスコープ（Singleton, Prototype, Request等）

### Step 17: 例外ハンドリング

**学んだこと**:
- ✅ カスタム例外（BusinessException, ResourceNotFoundException）
- ✅ GlobalExceptionHandler
- ✅ HTTPステータスコードの使い分け
- ✅ ユーザーフレンドリーなエラーレスポンス

**重要ポイント**:
```java
public UserResponse getUserById(Long id) {
    return userRepository.findById(id)
            .map(userMapper::toResponse)
            .orElseThrow(() -> new UserNotFoundException(id));  // 404
}
```

### Step 18: バリデーション

**学んだこと**:
- ✅ Bean Validation (`@Valid`, `@NotBlank`, `@Email`)
- ✅ カスタムバリデーション
- ✅ グループバリデーション
- ✅ `@RestControllerAdvice`での例外ハンドリング

**重要ポイント**:
```java
@PostMapping
public ResponseEntity<UserResponse> createUser(
        @Valid @RequestBody UserCreateRequest request) {  // @Validで自動検証
    // ...
}
```

### Step 19: DTOとEntityの分離

**学んだこと**:
- ✅ SOLID原則
- ✅ コンストラクタインジェクション
- ✅ トランザクション管理
- ✅ N+1問題の回避
- ✅ コーディング規約

---

## 🚀 総合演習: ブログシステムの実装

### 演習の概要

Phase 4で学んだすべての要素を統合して、簡単なブログシステムを実装します。

### 要件

#### 機能要件
1. **記事（Article）管理**
   - 記事の作成・更新・削除・一覧取得・詳細取得
   - タイトル、本文、カテゴリ、公開状態、著者

2. **カテゴリ（Category）管理**
   - カテゴリの作成・一覧取得
   - 1つの記事は1つのカテゴリに属する

3. **コメント（Comment）管理**
   - 記事へのコメント作成・削除
   - 1つの記事に複数のコメント

#### 非機能要件
- ✅ レイヤードアーキテクチャ
- ✅ DTOパターン
- ✅ バリデーション
- ✅ カスタム例外
- ✅ ロギング
- ✅ ベストプラクティス準拠

---

## 🚀 演習1: エンティティ設計

### 1-1. Articleエンティティ

**ファイルパス**: `src/main/java/com/example/hellospringboot/entity/Article.java`

```java
package com.example.hellospringboot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "articles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Boolean published;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    // 多対1: 記事 → ユーザー（著者）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    // 多対1: 記事 → カテゴリ
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // 1対多: 記事 → コメント
    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

### 1-2. Categoryエンティティ

**ファイルパス**: `src/main/java/com/example/hellospringboot/entity/Category.java`

```java
package com.example.hellospringboot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    // 1対多: カテゴリ → 記事
    @OneToMany(mappedBy = "category")
    @Builder.Default
    private List<Article> articles = new ArrayList<>();
}
```

### 1-3. Commentエンティティ

**ファイルパス**: `src/main/java/com/example/hellospringboot/entity/Comment.java`

```java
package com.example.hellospringboot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 多対1: コメント → 記事
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    // 多対1: コメント → ユーザー
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
```

---

## 🚀 演習2: DTO作成

### 2-1. ArticleCreateRequest

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/request/ArticleCreateRequest.java`

```java
package com.example.hellospringboot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleCreateRequest {

    @NotBlank(message = "タイトルは必須です")
    @Size(max = 200, message = "タイトルは200文字以内で入力してください")
    private String title;

    @NotBlank(message = "本文は必須です")
    private String content;

    @NotNull(message = "カテゴリIDは必須です")
    private Long categoryId;

    @NotNull(message = "公開状態は必須です")
    private Boolean published;
}
```

### 2-2. ArticleResponse

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/response/ArticleResponse.java`

```java
package com.example.hellospringboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleResponse {

    private Long id;
    private String title;
    private String content;
    private Boolean published;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private AuthorSummary author;
    private CategorySummary category;
    private Integer commentCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuthorSummary {
        private Long id;
        private String name;
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategorySummary {
        private Long id;
        private String name;
    }
}
```

---

## 🚀 演習3: Repository作成

### 3-1. ArticleRepository

**ファイルパス**: `src/main/java/com/example/hellospringboot/repository/ArticleRepository.java`

```java
package com.example.hellospringboot.repository;

import com.example.hellospringboot.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    // 公開済み記事を取得（ページネーション対応）
    Page<Article> findByPublishedTrue(Pageable pageable);

    // カテゴリIDで記事を検索
    List<Article> findByCategoryId(Long categoryId);

    // 著者IDで記事を検索
    List<Article> findByAuthorId(Long authorId);

    // タイトルで部分一致検索
    List<Article> findByTitleContaining(String keyword);

    // N+1問題を避けるためJOIN FETCH
    @Query("SELECT a FROM Article a " +
           "JOIN FETCH a.author " +
           "JOIN FETCH a.category " +
           "WHERE a.id = :id")
    Article findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT a FROM Article a " +
           "JOIN FETCH a.author " +
           "JOIN FETCH a.category " +
           "WHERE a.published = true")
    List<Article> findAllPublishedWithDetails();
}
```

### 3-2. CategoryRepository

**ファイルパス**: `src/main/java/com/example/hellospringboot/repository/CategoryRepository.java`

```java
package com.example.hellospringboot.repository;

import com.example.hellospringboot.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    boolean existsByName(String name);
}
```

---

## 🚀 演習4: Service作成

### 4-1. ArticleService

**ファイルパス**: `src/main/java/com/example/hellospringboot/service/ArticleService.java`

```java
package com.example.hellospringboot.service;

import com.example.hellospringboot.dto.request.ArticleCreateRequest;
import com.example.hellospringboot.dto.response.ArticleResponse;
import com.example.hellospringboot.entity.Article;
import com.example.hellospringboot.entity.Category;
import com.example.hellospringboot.entity.User;
import com.example.hellospringboot.exception.ResourceNotFoundException;
import com.example.hellospringboot.mapper.ArticleMapper;
import com.example.hellospringboot.repository.ArticleRepository;
import com.example.hellospringboot.repository.CategoryRepository;
import com.example.hellospringboot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ArticleMapper articleMapper;

    /**
     * 記事を作成
     */
    @Transactional
    public ArticleResponse createArticle(Long authorId, ArticleCreateRequest request) {
        log.info("Creating article by user {}: {}", authorId, request.getTitle());

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("ユーザー", authorId));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("カテゴリ", request.getCategoryId()));

        Article article = Article.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .published(request.getPublished())
                .author(author)
                .category(category)
                .build();

        Article savedArticle = articleRepository.save(article);
        log.info("Article created successfully with ID: {}", savedArticle.getId());

        return articleMapper.toResponse(savedArticle);
    }

    /**
     * 公開済み記事を取得（ページネーション）
     */
    public Page<ArticleResponse> getPublishedArticles(Pageable pageable) {
        log.debug("Fetching published articles - page: {}, size: {}", 
                 pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Article> articles = articleRepository.findByPublishedTrue(pageable);
        return articles.map(articleMapper::toResponse);
    }

    /**
     * 記事詳細を取得
     */
    public ArticleResponse getArticleById(Long id) {
        log.debug("Fetching article with ID: {}", id);
        
        Article article = articleRepository.findByIdWithDetails(id);
        if (article == null) {
            throw new ResourceNotFoundException("記事", id);
        }
        
        return articleMapper.toResponse(article);
    }

    /**
     * 記事を更新
     */
    @Transactional
    public ArticleResponse updateArticle(Long id, ArticleCreateRequest request) {
        log.info("Updating article with ID: {}", id);

        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("記事", id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("カテゴリ", request.getCategoryId()));

        article.setTitle(request.getTitle());
        article.setContent(request.getContent());
        article.setPublished(request.getPublished());
        article.setCategory(category);

        Article updatedArticle = articleRepository.save(article);
        log.info("Article updated successfully with ID: {}", updatedArticle.getId());

        return articleMapper.toResponse(updatedArticle);
    }

    /**
     * 記事を削除
     */
    @Transactional
    public void deleteArticle(Long id) {
        log.info("Deleting article with ID: {}", id);

        if (!articleRepository.existsById(id)) {
            throw new ResourceNotFoundException("記事", id);
        }

        articleRepository.deleteById(id);
        log.info("Article deleted successfully with ID: {}", id);
    }
}
```

---

## 🚀 演習5: Controller作成

### 5-1. ArticleController

**ファイルパス**: `src/main/java/com/example/hellospringboot/controller/ArticleController.java`

```java
package com.example.hellospringboot.controller;

import com.example.hellospringboot.dto.request.ArticleCreateRequest;
import com.example.hellospringboot.dto.response.ArticleResponse;
import com.example.hellospringboot.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    /**
     * 記事作成
     * POST /api/articles
     */
    @PostMapping
    public ResponseEntity<ArticleResponse> createArticle(
            @RequestParam Long authorId,
            @Valid @RequestBody ArticleCreateRequest request) {
        ArticleResponse response = articleService.createArticle(authorId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 公開済み記事一覧取得
     * GET /api/articles
     */
    @GetMapping
    public ResponseEntity<Page<ArticleResponse>> getPublishedArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
        
        Pageable pageable = PageRequest.of(page, size, 
            Sort.by(Sort.Direction.fromString(sort[1]), sort[0]));
        
        Page<ArticleResponse> articles = articleService.getPublishedArticles(pageable);
        return ResponseEntity.ok(articles);
    }

    /**
     * 記事詳細取得
     * GET /api/articles/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponse> getArticleById(@PathVariable Long id) {
        ArticleResponse response = articleService.getArticleById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 記事更新
     * PUT /api/articles/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ArticleResponse> updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody ArticleCreateRequest request) {
        ArticleResponse response = articleService.updateArticle(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 記事削除
     * DELETE /api/articles/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

## ✅ 動作確認

### カテゴリ作成
```bash
curl -X POST http://localhost:8080/api/categories \
  -H "Content-Type: application/json" \
  -d '{"name": "Technology", "description": "Tech articles"}'
```

### ユーザー作成
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe", "email": "john@example.com", "age": 30}'
```

### 記事作成
```bash
curl -X POST "http://localhost:8080/api/articles?authorId=1" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Introduction to Spring Boot",
    "content": "Spring Boot makes it easy...",
    "categoryId": 1,
    "published": true
  }'
```

### 記事一覧取得
```bash
curl "http://localhost:8080/api/articles?page=0&size=10"
```

---

## 📚 Phase 3で学んだことの統合

この演習では以下をすべて使いました：

- ✅ **レイヤードアーキテクチャ**: Controller / Service / Repository
- ✅ **DTOパターン**: Request/Response DTO
- ✅ **バリデーション**: `@Valid`, `@NotBlank`
- ✅ **例外ハンドリング**: ResourceNotFoundException
- ✅ **ロギング**: `@Slf4j`, log.info/debug
- ✅ **ベストプラクティス**: コンストラクタインジェクション、トランザクション
- ✅ **パフォーマンス**: JOIN FETCH、ページネーション

---

## 🔄 Gitへのコミット

Phase 4完了です！

```bash
git add .
git commit -m "Step 20: ロギング完了 - Phase 4完了"
git push origin main
```

---

## ➡️ 次のPhase

Phase 4お疲れさまでした！次は**Phase 5: Thymeleafでサーバーサイドレンダリング**に進みます。

[Step 21: Thymeleafの基礎](../phase5/STEP_21.md)で、サーバーサイドレンダリングの基本を学びます。

Phase 5では以下を学びます：
- Thymeleafテンプレートエンジンの基礎
- フォーム送信とバリデーション
- レイアウトとフラグメント
- Thymeleaf + REST API連携

---

お疲れさまでした！ 🎉

Phase 4では、実務で必須のアーキテクチャとベストプラクティスを学びました。
これらの知識は、どんなプロジェクトでも活用できる基礎となります！
