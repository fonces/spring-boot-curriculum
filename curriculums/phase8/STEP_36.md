# Step 36: 記事とコメント機能の実装

## 🎯 このステップの目標

- 認証済みユーザーのみが記事を投稿できる機能を実装できる
- 記事の編集・削除時に所有者チェック（認可）ができる
- コメント機能を実装できる
- タグによる記事の分類ができる
- ページネーションで記事一覧を表示できる

**所要時間**: 約90分

---

## 📋 事前準備

- Step 35までの内容を完了していること
- JWT認証が正常に動作していること
- データベースにUser、Article、Comment、Tagテーブルが存在すること

---

## 🚀 ステップ1: Repositoryの実装

### 1-1. ArticleRepositoryの作成

`src/main/java/com/example/bloghub/repositories/ArticleRepository.java`を作成：

```java
package com.example.bloghub.repositories;

import com.example.bloghub.entities.Article;
import com.example.bloghub.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    
    // 著者別の記事一覧（ページネーション）
    Page<Article> findByAuthor(User author, Pageable pageable);
    
    // タイトルで部分一致検索
    Page<Article> findByTitleContaining(String title, Pageable pageable);
    
    // タグで絞り込み
    @Query("SELECT DISTINCT a FROM Article a JOIN a.tags t WHERE t.name = :tagName")
    Page<Article> findByTagName(@Param("tagName") String tagName, Pageable pageable);
    
    // 最新記事一覧
    Page<Article> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
```

### 1-2. CommentRepositoryの作成

`src/main/java/com/example/bloghub/repositories/CommentRepository.java`を作成：

```java
package com.example.bloghub.repositories;

import com.example.bloghub.entities.Article;
import com.example.bloghub.entities.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    // 記事別のコメント一覧
    List<Comment> findByArticleOrderByCreatedAtDesc(Article article);
    
    // 記事別のコメント件数
    Long countByArticle(Article article);
}
```

### 1-3. TagRepositoryの作成

`src/main/java/com/example/bloghub/repositories/TagRepository.java`を作成：

```java
package com.example.bloghub.repositories;

import com.example.bloghub.entities.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    
    Optional<Tag> findByName(String name);
    
    Boolean existsByName(String name);
}
```

---

## 🚀 ステップ2: DTOの作成

### 2-1. リクエストDTO

`src/main/java/com/example/bloghub/dto/request/ArticleCreateRequest.java`を作成：

```java
package com.example.bloghub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class ArticleCreateRequest {
    
    @NotBlank(message = "タイトルは必須です")
    @Size(max = 200, message = "タイトルは200文字以内で入力してください")
    private String title;
    
    @NotBlank(message = "本文は必須です")
    private String content;
    
    private String imageUrl;
    
    private Set<String> tags;
}
```

`src/main/java/com/example/bloghub/dto/request/ArticleUpdateRequest.java`を作成：

```java
package com.example.bloghub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class ArticleUpdateRequest {
    
    @NotBlank(message = "タイトルは必須です")
    @Size(max = 200, message = "タイトルは200文字以内で入力してください")
    private String title;
    
    @NotBlank(message = "本文は必須です")
    private String content;
    
    private String imageUrl;
    
    private Set<String> tags;
}
```

`src/main/java/com/example/bloghub/dto/request/CommentCreateRequest.java`を作成：

```java
package com.example.bloghub.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentCreateRequest {
    
    @NotBlank(message = "コメント内容は必須です")
    private String content;
}
```

### 2-2. レスポンスDTO

`src/main/java/com/example/bloghub/dto/response/ArticleResponse.java`を作成：

```java
package com.example.bloghub.dto.response;

import com.example.bloghub.entities.Article;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleResponse {
    
    private Long id;
    private String title;
    private String content;
    private String imageUrl;
    private Integer viewCount;
    private AuthorResponse author;
    private Set<String> tags;
    private Integer commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorResponse {
        private Long id;
        private String username;
        private String profileImage;
    }
    
    public static ArticleResponse from(Article article) {
        return ArticleResponse.builder()
            .id(article.getId())
            .title(article.getTitle())
            .content(article.getContent())
            .imageUrl(article.getImageUrl())
            .viewCount(article.getViewCount())
            .author(AuthorResponse.builder()
                .id(article.getAuthor().getId())
                .username(article.getAuthor().getUsername())
                .profileImage(article.getAuthor().getProfileImage())
                .build())
            .tags(article.getTags().stream()
                .map(tag -> tag.getName())
                .collect(Collectors.toSet()))
            .commentCount(article.getComments().size())
            .createdAt(article.getCreatedAt())
            .updatedAt(article.getUpdatedAt())
            .build();
    }
}
```

`src/main/java/com/example/bloghub/dto/response/CommentResponse.java`を作成：

```java
package com.example.bloghub.dto.response;

import com.example.bloghub.entities.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    
    private Long id;
    private String content;
    private UserInfo user;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String profileImage;
    }
    
    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
            .id(comment.getId())
            .content(comment.getContent())
            .user(UserInfo.builder()
                .id(comment.getUser().getId())
                .username(comment.getUser().getUsername())
                .profileImage(comment.getUser().getProfileImage())
                .build())
            .createdAt(comment.getCreatedAt())
            .updatedAt(comment.getUpdatedAt())
            .build();
    }
}
```

---

## 🚀 ステップ3: サービスの実装

### 3-1. ArticleServiceの作成

`src/main/java/com/example/bloghub/services/ArticleService.java`を作成：

```java
package com.example.bloghub.services;

import com.example.bloghub.dto.request.ArticleCreateRequest;
import com.example.bloghub.dto.request.ArticleUpdateRequest;
import com.example.bloghub.dto.response.ArticleResponse;
import com.example.bloghub.dto.response.PageResponse;
import com.example.bloghub.entities.Article;
import com.example.bloghub.entities.Tag;
import com.example.bloghub.entities.User;
import com.example.bloghub.repositories.ArticleRepository;
import com.example.bloghub.repositories.TagRepository;
import com.example.bloghub.repositories.UserRepository;
import com.example.bloghub.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * 記事サービス
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {
    
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    
    /**
     * 記事一覧取得（ページネーション）
     */
    @Cacheable(value = "articles", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public PageResponse<ArticleResponse> getAllArticles(Pageable pageable) {
        Page<Article> articles = articleRepository.findAllByOrderByCreatedAtDesc(pageable);
        Page<ArticleResponse> responses = articles.map(ArticleResponse::from);
        return PageResponse.of(responses);
    }
    
    /**
     * 記事詳細取得
     */
    @Transactional
    public ArticleResponse getArticleById(Long id) {
        Article article = articleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Article not found with id: " + id));
        
        // 閲覧数をインクリメント
        article.incrementViewCount();
        articleRepository.save(article);
        
        return ArticleResponse.from(article);
    }
    
    /**
     * 記事投稿
     */
    @CacheEvict(value = "articles", allEntries = true)
    @Transactional
    public ArticleResponse createArticle(ArticleCreateRequest request) {
        UserPrincipal userPrincipal = getCurrentUserPrincipal();
        User author = userRepository.findById(userPrincipal.getId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // タグの処理
        Set<Tag> tags = processTags(request.getTags());
        
        // 記事作成
        Article article = Article.builder()
            .title(request.getTitle())
            .content(request.getContent())
            .imageUrl(request.getImageUrl())
            .author(author)
            .viewCount(0)
            .tags(tags)
            .build();
        
        Article savedArticle = articleRepository.save(article);
        log.info("Article created: id={}, title={}", savedArticle.getId(), savedArticle.getTitle());
        
        return ArticleResponse.from(savedArticle);
    }
    
    /**
     * 記事更新
     */
    @CacheEvict(value = "articles", allEntries = true)
    @Transactional
    public ArticleResponse updateArticle(Long id, ArticleUpdateRequest request) {
        Article article = articleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Article not found with id: " + id));
        
        // 所有者チェック
        checkArticleOwnership(article);
        
        // タグの処理
        Set<Tag> tags = processTags(request.getTags());
        
        // 記事更新
        article.setTitle(request.getTitle());
        article.setContent(request.getContent());
        article.setImageUrl(request.getImageUrl());
        article.getTags().clear();
        article.getTags().addAll(tags);
        
        Article updatedArticle = articleRepository.save(article);
        log.info("Article updated: id={}", updatedArticle.getId());
        
        return ArticleResponse.from(updatedArticle);
    }
    
    /**
     * 記事削除
     */
    @CacheEvict(value = "articles", allEntries = true)
    @Transactional
    public void deleteArticle(Long id) {
        Article article = articleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Article not found with id: " + id));
        
        // 所有者チェック
        checkArticleOwnership(article);
        
        articleRepository.delete(article);
        log.info("Article deleted: id={}", id);
    }
    
    /**
     * タグ検索
     */
    @Transactional(readOnly = true)
    public PageResponse<ArticleResponse> searchByTag(String tagName, Pageable pageable) {
        Page<Article> articles = articleRepository.findByTagName(tagName, pageable);
        Page<ArticleResponse> responses = articles.map(ArticleResponse::from);
        return PageResponse.of(responses);
    }
    
    /**
     * キーワード検索
     */
    @Transactional(readOnly = true)
    public PageResponse<ArticleResponse> searchByKeyword(String keyword, Pageable pageable) {
        Page<Article> articles = articleRepository.findByTitleContaining(keyword, pageable);
        Page<ArticleResponse> responses = articles.map(ArticleResponse::from);
        return PageResponse.of(responses);
    }
    
    /**
     * タグの処理（存在しない場合は作成）
     */
    private Set<Tag> processTags(Set<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new HashSet<>();
        }
        
        Set<Tag> tags = new HashSet<>();
        for (String tagName : tagNames) {
            Tag tag = tagRepository.findByName(tagName)
                .orElseGet(() -> {
                    Tag newTag = Tag.builder().name(tagName).build();
                    return tagRepository.save(newTag);
                });
            tags.add(tag);
        }
        return tags;
    }
    
    /**
     * 記事の所有者チェック
     */
    private void checkArticleOwnership(Article article) {
        UserPrincipal userPrincipal = getCurrentUserPrincipal();
        if (!article.getAuthor().getId().equals(userPrincipal.getId())) {
            throw new RuntimeException("You are not the owner of this article");
        }
    }
    
    /**
     * 現在のユーザー取得
     */
    private UserPrincipal getCurrentUserPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();
    }
}
```

### 3-2. CommentServiceの作成

`src/main/java/com/example/bloghub/services/CommentService.java`を作成：

```java
package com.example.bloghub.services;

import com.example.bloghub.dto.request.CommentCreateRequest;
import com.example.bloghub.dto.response.CommentResponse;
import com.example.bloghub.entities.Article;
import com.example.bloghub.entities.Comment;
import com.example.bloghub.entities.User;
import com.example.bloghub.repositories.ArticleRepository;
import com.example.bloghub.repositories.CommentRepository;
import com.example.bloghub.repositories.UserRepository;
import com.example.bloghub.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * コメントサービス
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {
    
    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    
    /**
     * 記事のコメント一覧取得
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByArticle(Long articleId) {
        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new RuntimeException("Article not found with id: " + articleId));
        
        return commentRepository.findByArticleOrderByCreatedAtDesc(article)
            .stream()
            .map(CommentResponse::from)
            .collect(Collectors.toList());
    }
    
    /**
     * コメント投稿
     */
    @Transactional
    public CommentResponse createComment(Long articleId, CommentCreateRequest request) {
        UserPrincipal userPrincipal = getCurrentUserPrincipal();
        
        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new RuntimeException("Article not found with id: " + articleId));
        
        User user = userRepository.findById(userPrincipal.getId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Comment comment = Comment.builder()
            .content(request.getContent())
            .article(article)
            .user(user)
            .build();
        
        Comment savedComment = commentRepository.save(comment);
        log.info("Comment created: id={}, articleId={}", savedComment.getId(), articleId);
        
        return CommentResponse.from(savedComment);
    }
    
    /**
     * コメント削除
     */
    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));
        
        // 所有者チェック
        checkCommentOwnership(comment);
        
        commentRepository.delete(comment);
        log.info("Comment deleted: id={}", commentId);
    }
    
    /**
     * コメントの所有者チェック
     */
    private void checkCommentOwnership(Comment comment) {
        UserPrincipal userPrincipal = getCurrentUserPrincipal();
        if (!comment.getUser().getId().equals(userPrincipal.getId())) {
            throw new RuntimeException("You are not the owner of this comment");
        }
    }
    
    /**
     * 現在のユーザー取得
     */
    private UserPrincipal getCurrentUserPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();
    }
}
```

---

## 🚀 ステップ4: コントローラーの実装

### 4-1. ArticleControllerの作成

`src/main/java/com/example/bloghub/controllers/ArticleController.java`を作成：

```java
package com.example.bloghub.controllers;

import com.example.bloghub.dto.request.ArticleCreateRequest;
import com.example.bloghub.dto.request.ArticleUpdateRequest;
import com.example.bloghub.dto.response.ArticleResponse;
import com.example.bloghub.dto.response.PageResponse;
import com.example.bloghub.services.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 記事コントローラー
 */
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {
    
    private final ArticleService articleService;
    
    /**
     * 記事一覧取得
     */
    @GetMapping
    public ResponseEntity<PageResponse<ArticleResponse>> getAllArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        PageResponse<ArticleResponse> response = articleService.getAllArticles(pageable);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 記事詳細取得
     */
    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponse> getArticleById(@PathVariable Long id) {
        ArticleResponse response = articleService.getArticleById(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 記事投稿
     */
    @PostMapping
    public ResponseEntity<ArticleResponse> createArticle(@Valid @RequestBody ArticleCreateRequest request) {
        ArticleResponse response = articleService.createArticle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * 記事更新
     */
    @PutMapping("/{id}")
    public ResponseEntity<ArticleResponse> updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody ArticleUpdateRequest request) {
        ArticleResponse response = articleService.updateArticle(id, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 記事削除
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * タグ検索
     */
    @GetMapping("/search/tag")
    public ResponseEntity<PageResponse<ArticleResponse>> searchByTag(
            @RequestParam String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<ArticleResponse> response = articleService.searchByTag(tag, pageable);
        return ResponseEntity.ok(response);
    }
    
    /**
     * キーワード検索
     */
    @GetMapping("/search")
    public ResponseEntity<PageResponse<ArticleResponse>> searchByKeyword(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<ArticleResponse> response = articleService.searchByKeyword(keyword, pageable);
        return ResponseEntity.ok(response);
    }
}
```

### 4-2. CommentControllerの作成

`src/main/java/com/example/bloghub/controllers/CommentController.java`を作成：

```java
package com.example.bloghub.controllers;

import com.example.bloghub.dto.request.CommentCreateRequest;
import com.example.bloghub.dto.response.CommentResponse;
import com.example.bloghub.services.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * コメントコントローラー
 */
@RestController
@RequestMapping("/api/articles/{articleId}/comments")
@RequiredArgsConstructor
public class CommentController {
    
    private final CommentService commentService;
    
    /**
     * 記事のコメント一覧取得
     */
    @GetMapping
    public ResponseEntity<List<CommentResponse>> getCommentsByArticle(@PathVariable Long articleId) {
        List<CommentResponse> responses = commentService.getCommentsByArticle(articleId);
        return ResponseEntity.ok(responses);
    }
    
    /**
     * コメント投稿
     */
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long articleId,
            @Valid @RequestBody CommentCreateRequest request) {
        CommentResponse response = commentService.createComment(articleId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * コメント削除
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
```

---

## ✅ 動作確認

### 1. 記事投稿

```bash
TOKEN="<Step 35で取得したJWTトークン>"

curl -X POST http://localhost:8080/api/articles \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Spring Bootで作るブログアプリ",
    "content": "Spring BootとJPAを使って...",
    "tags": ["Java", "Spring Boot", "開発"]
  }'
```

**期待される結果**:
```json
{
  "id": 1,
  "title": "Spring Bootで作るブログアプリ",
  "content": "Spring BootとJPAを使って...",
  "viewCount": 0,
  "author": {
    "id": 1,
    "username": "testuser",
    "profileImage": null
  },
  "tags": ["Java", "Spring Boot", "開発"],
  "commentCount": 0,
  "createdAt": "2025-12-13T10:00:00",
  "updatedAt": "2025-12-13T10:00:00"
}
```

### 2. 記事一覧取得

```bash
curl http://localhost:8080/api/articles?page=0&size=10
```

### 3. 記事詳細取得

```bash
curl http://localhost:8080/api/articles/1
```

### 4. コメント投稿

```bash
curl -X POST http://localhost:8080/api/articles/1/comments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "とても参考になりました！"
  }'
```

### 5. タグ検索

```bash
curl "http://localhost:8080/api/articles/search/tag?tag=Spring%20Boot"
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: いいね機能の実装

**目標**: 記事にいいねできる機能を追加

**ヒント**:
```java
@Entity
public class ArticleLike {
    @Id
    private Long id;
    
    @ManyToOne
    private Article article;
    
    @ManyToOne
    private User user;
    
    // 同じユーザーが同じ記事に複数回いいねできないよう、
    // article_id と user_id で複合ユニーク制約
}
```

### チャレンジ 2: 下書き機能

**目標**: 記事を下書きとして保存し、後で公開できる

**ヒント**:
```java
@Entity
public class Article {
    @Enumerated(EnumType.STRING)
    private ArticleStatus status; // DRAFT, PUBLISHED
}
```

### チャレンジ 3: 人気記事ランキング

**目標**: 閲覧数の多い記事をランキング表示

**ヒント**:
```java
@Query("SELECT a FROM Article a ORDER BY a.viewCount DESC")
Page<Article> findPopularArticles(Pageable pageable);
```

---

## 🐛 トラブルシューティング

### エラー: "You are not the owner of this article"

**原因**: 他人の記事を編集・削除しようとしている

**解決策**: 自分の記事のみ編集・削除可能です

### エラー: "Article not found"

**原因**: 存在しないIDを指定している

**解決策**: 記事一覧で有効なIDを確認

### エラー: "LazyInitializationException"

**原因**: トランザクション外でLazy fetchの関連エンティティにアクセス

**解決策**: `@Transactional`を付与、またはFetch戦略を`EAGER`に変更

### エラー: キャッシュが更新されない

**原因**: `@CacheEvict`が正しく動作していない

**解決策**: キャッシュ設定を確認し、`allEntries = true`を使用

---

## 📚 このステップで学んだこと

- ✅ 認証済みユーザーのみが投稿できる機能を実装した
- ✅ 記事の所有者チェック（認可）を実装した
- ✅ コメント機能を実装した
- ✅ タグによる記事分類を実装した
- ✅ ページネーションで記事一覧を表示した
- ✅ キャッシュでパフォーマンスを最適化した
- ✅ JPA/JPQLで複雑なクエリを実装した

---

## ➡️ 次のステップ

[Step 37: 画像アップロードと検索機能](STEP_37.md)へ進みましょう！

次のステップでは、記事への画像アップロード機能と高度な検索機能を実装します：
- 画像アップロード（記事・プロフィール）
- 画像のサムネイル生成
- MyBatisで複雑な検索クエリ
- 全文検索機能

ブログの実用的な機能を追加していきます！📸
