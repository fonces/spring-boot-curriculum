# Step 36: 記事とコメント機能の実装

**所要時間**: 約90分

---

## 📌 このステップの目標

BlogHubアプリケーションのコア機能である**記事（Article）とコメント（Comment）機能**を実装します。このステップでは、RESTful APIの設計、ページネーション、N+1問題の回避、所有者チェック、タグ機能など、実践的なWeb開発で必要となる技術を総合的に学びます。

**学ぶこと**:
- カスタムクエリとページネーションの実装
- N+1問題の回避（JOIN FETCH）
- 所有者チェックによるセキュリティ強化
- タグの自動作成と関連付け処理
- ネストされたRESTfulルート設計
- グローバルエラーハンドリング
- DTOとEntityの適切な分離

**成果物**:
- 記事のCRUD API（作成・取得・更新・削除）
- コメントのCRUD API
- タグ検索機能
- ページネーション対応の一覧取得
- 所有者のみが編集・削除できる権限制御

---

## 🔧 事前準備

### 前提条件

- Step 35で認証・認可機能が実装済みであること
- JWTトークンによる認証が動作していること
- User、Article、Comment、Tagエンティティが定義済みであること
- Docker ComposeでMySQLが起動していること

### 必要な知識

- Spring Data JPAのカスタムクエリ（`@Query`アノテーション）
- ページネーション（`Pageable`と`Page`）
- N+1問題とその対策（`JOIN FETCH`）
- RESTful APIの設計原則
- 例外ハンドリング（`@ControllerAdvice`）

### 環境確認

```bash
# MySQLコンテナが起動しているか確認
docker ps | grep mysql

# アプリケーションが起動しているか確認
curl http://localhost:8080/actuator/health
```

---

## 📝 実装手順

### 手順1: 例外クラスの作成

まず、エラーハンドリングのための例外クラスを作成します。

#### 1.1 ResourceNotFoundExceptionの作成

`src/main/java/com/example/bloghub/exception/ResourceNotFoundException.java`:

```java
package com.example.bloghub.exception;

public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String resourceName, Long id) {
        super(String.format("%s not found with id: %d", resourceName, id));
    }
    
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: %s", resourceName, fieldName, fieldValue));
    }
}
```

#### 1.2 UnauthorizedExceptionの作成

`src/main/java/com/example/bloghub/exception/UnauthorizedException.java`:

```java
package com.example.bloghub.exception;

public class UnauthorizedException extends RuntimeException {
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public UnauthorizedException() {
        super("You are not authorized to perform this action");
    }
}
```

#### 1.3 ErrorResponseの作成

`src/main/java/com/example/bloghub/dto/ErrorResponse.java`:

```java
package com.example.bloghub.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String message;
    private LocalDateTime timestamp;
    private String path;
}
```

#### 1.4 GlobalExceptionHandlerの作成

`src/main/java/com/example/bloghub/exception/GlobalExceptionHandler.java`:

```java
package com.example.bloghub.exception;

import com.example.bloghub.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                LocalDateTime.now(),
                request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(
            UnauthorizedException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                ex.getMessage(),
                LocalDateTime.now(),
                request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("message", "Validation failed");
        response.put("errors", errors);
        response.put("timestamp", LocalDateTime.now());
        response.put("path", request.getDescription(false).replace("uri=", ""));
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred: " + ex.getMessage(),
                LocalDateTime.now(),
                request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

---

### 手順2: Repository層の実装

#### 2.1 ArticleRepositoryの作成

`src/main/java/com/example/bloghub/repository/ArticleRepository.java`:

```java
package com.example.bloghub.repository;

import com.example.bloghub.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    
    /**
     * ユーザーの記事一覧をページネーションで取得
     * 新しい順にソート
     */
    Page<Article> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * タグで記事を検索
     * 同じタグが複数回紐づいていても重複しないようDISTINCTを使用
     */
    @Query("SELECT DISTINCT a FROM Article a JOIN a.tags t WHERE t.name = :tagName ORDER BY a.createdAt DESC")
    Page<Article> findByTagName(@Param("tagName") String tagName, Pageable pageable);
    
    /**
     * 記事とユーザー情報を一緒に取得（N+1問題回避）
     * JOIN FETCHでユーザー情報を同時に取得
     */
    @Query("SELECT a FROM Article a JOIN FETCH a.user WHERE a.id = :id")
    Optional<Article> findByIdWithUser(@Param("id") Long id);
    
    /**
     * すべての記事を新しい順で取得
     */
    Page<Article> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
```

#### 2.2 CommentRepositoryの作成

`src/main/java/com/example/bloghub/repository/CommentRepository.java`:

```java
package com.example.bloghub.repository;

import com.example.bloghub.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    /**
     * 記事のコメント一覧を取得
     * 古い順にソート（会話の流れを保つため）
     */
    List<Comment> findByArticleIdOrderByCreatedAtAsc(Long articleId);
    
    /**
     * 記事のコメント数をカウント
     */
    long countByArticleId(Long articleId);
}
```

#### 2.3 TagRepositoryの作成

`src/main/java/com/example/bloghub/repository/TagRepository.java`:

```java
package com.example.bloghub.repository;

import com.example.bloghub.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    
    /**
     * タグ名で検索
     */
    Optional<Tag> findByName(String name);
    
    /**
     * タグ名が存在するかチェック
     */
    boolean existsByName(String name);
}
```

---

### 手順3: DTO層の実装

#### 3.1 記事関連DTOの作成

**ArticleCreateRequest** (`src/main/java/com/example/bloghub/dto/article/ArticleCreateRequest.java`):

```java
package com.example.bloghub.dto.article;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class ArticleCreateRequest {
    
    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;
    
    @NotBlank(message = "Content is required")
    @Size(min = 1, max = 10000, message = "Content must be between 1 and 10000 characters")
    private String content;
    
    private Set<String> tags = new HashSet<>();
}
```

**ArticleUpdateRequest** (`src/main/java/com/example/bloghub/dto/article/ArticleUpdateRequest.java`):

```java
package com.example.bloghub.dto.article;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class ArticleUpdateRequest {
    
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;
    
    @Size(min = 1, max = 10000, message = "Content must be between 1 and 10000 characters")
    private String content;
    
    private Set<String> tags = new HashSet<>();
}
```

**ArticleResponse** (`src/main/java/com/example/bloghub/dto/article/ArticleResponse.java`):

```java
package com.example.bloghub.dto.article;

import com.example.bloghub.dto.user.UserResponse;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
public class ArticleResponse {
    private Long id;
    private String title;
    private String content;
    private Set<String> tags = new HashSet<>();
    private UserResponse user;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long commentCount;
}
```

**ArticleSummaryResponse** (`src/main/java/com/example/bloghub/dto/article/ArticleSummaryResponse.java`):

```java
package com.example.bloghub.dto.article;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
public class ArticleSummaryResponse {
    private Long id;
    private String title;
    private String username;
    private Set<String> tags = new HashSet<>();
    private LocalDateTime createdAt;
    private Long commentCount;
}
```

#### 3.2 コメント関連DTOの作成

**CommentCreateRequest** (`src/main/java/com/example/bloghub/dto/comment/CommentCreateRequest.java`):

```java
package com.example.bloghub.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentCreateRequest {
    
    @NotBlank(message = "Content is required")
    @Size(min = 1, max = 1000, message = "Content must be between 1 and 1000 characters")
    private String content;
}
```

**CommentResponse** (`src/main/java/com/example/bloghub/dto/comment/CommentResponse.java`):

```java
package com.example.bloghub.dto.comment;

import com.example.bloghub.dto.user.UserResponse;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentResponse {
    private Long id;
    private String content;
    private UserResponse user;
    private LocalDateTime createdAt;
}
```

---

### 手順4: Service層の実装

#### 4.1 TagServiceの作成

まず、タグの管理を行うサービスを作成します。

`src/main/java/com/example/bloghub/service/TagService.java`:

```java
package com.example.bloghub.service;

import com.example.bloghub.entity.Tag;
import com.example.bloghub.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {
    
    private final TagRepository tagRepository;
    
    /**
     * タグ名から取得または新規作成
     */
    @Transactional
    public Tag findOrCreateTag(String name) {
        return tagRepository.findByName(name)
                .orElseGet(() -> {
                    Tag newTag = new Tag();
                    newTag.setName(name);
                    return tagRepository.save(newTag);
                });
    }
    
    /**
     * すべてのタグを取得
     */
    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }
}
```

#### 4.2 ArticleServiceの作成

`src/main/java/com/example/bloghub/service/ArticleService.java`:

```java
package com.example.bloghub.service;

import com.example.bloghub.dto.article.*;
import com.example.bloghub.dto.user.UserResponse;
import com.example.bloghub.entity.Article;
import com.example.bloghub.entity.Tag;
import com.example.bloghub.entity.User;
import com.example.bloghub.exception.ResourceNotFoundException;
import com.example.bloghub.exception.UnauthorizedException;
import com.example.bloghub.repository.ArticleRepository;
import com.example.bloghub.repository.CommentRepository;
import com.example.bloghub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleService {
    
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final TagService tagService;
    
    /**
     * 記事を作成
     */
    @Transactional
    public ArticleResponse createArticle(ArticleCreateRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        Article article = new Article();
        article.setTitle(request.getTitle());
        article.setContent(request.getContent());
        article.setUser(user);
        
        // タグの処理
        Set<Tag> tags = processTags(request.getTags());
        article.setTags(tags);
        
        Article savedArticle = articleRepository.save(article);
        return convertToResponse(savedArticle);
    }
    
    /**
     * 記事を更新
     */
    @Transactional
    public ArticleResponse updateArticle(Long id, ArticleUpdateRequest request, String username) {
        Article article = articleRepository.findByIdWithUser(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article", id));
        
        // 所有者チェック
        if (!article.getUser().getUsername().equals(username)) {
            throw new UnauthorizedException("You can only update your own articles");
        }
        
        // 更新
        if (request.getTitle() != null) {
            article.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            article.setContent(request.getContent());
        }
        if (request.getTags() != null) {
            Set<Tag> tags = processTags(request.getTags());
            article.setTags(tags);
        }
        
        Article updatedArticle = articleRepository.save(article);
        return convertToResponse(updatedArticle);
    }
    
    /**
     * 記事を削除
     */
    @Transactional
    public void deleteArticle(Long id, String username) {
        Article article = articleRepository.findByIdWithUser(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article", id));
        
        // 所有者チェック
        if (!article.getUser().getUsername().equals(username)) {
            throw new UnauthorizedException("You can only delete your own articles");
        }
        
        articleRepository.delete(article);
    }
    
    /**
     * 記事詳細を取得
     */
    public ArticleResponse getArticleById(Long id) {
        Article article = articleRepository.findByIdWithUser(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article", id));
        return convertToResponse(article);
    }
    
    /**
     * すべての記事を取得（ページネーション）
     */
    public Page<ArticleSummaryResponse> getAllArticles(Pageable pageable) {
        Page<Article> articles = articleRepository.findAllByOrderByCreatedAtDesc(pageable);
        return articles.map(this::convertToSummaryResponse);
    }
    
    /**
     * タグで記事を検索
     */
    public Page<ArticleSummaryResponse> getArticlesByTag(String tagName, Pageable pageable) {
        Page<Article> articles = articleRepository.findByTagName(tagName, pageable);
        return articles.map(this::convertToSummaryResponse);
    }
    
    /**
     * 自分の記事一覧を取得
     */
    public Page<ArticleSummaryResponse> getMyArticles(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        Page<Article> articles = articleRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        return articles.map(this::convertToSummaryResponse);
    }
    
    /**
     * タグ名のセットからTagエンティティのセットを作成
     */
    private Set<Tag> processTags(Set<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new HashSet<>();
        }
        
        return tagNames.stream()
                .filter(name -> name != null && !name.trim().isEmpty())
                .map(tagService::findOrCreateTag)
                .collect(Collectors.toSet());
    }
    
    /**
     * ArticleエンティティをArticleResponseに変換
     */
    private ArticleResponse convertToResponse(Article article) {
        ArticleResponse response = new ArticleResponse();
        response.setId(article.getId());
        response.setTitle(article.getTitle());
        response.setContent(article.getContent());
        response.setTags(article.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toSet()));
        
        UserResponse userResponse = new UserResponse();
        userResponse.setId(article.getUser().getId());
        userResponse.setUsername(article.getUser().getUsername());
        userResponse.setEmail(article.getUser().getEmail());
        userResponse.setCreatedAt(article.getUser().getCreatedAt());
        response.setUser(userResponse);
        
        response.setCreatedAt(article.getCreatedAt());
        response.setUpdatedAt(article.getUpdatedAt());
        
        // コメント数を取得
        long commentCount = commentRepository.countByArticleId(article.getId());
        response.setCommentCount(commentCount);
        
        return response;
    }
    
    /**
     * ArticleエンティティをArticleSummaryResponseに変換
     */
    private ArticleSummaryResponse convertToSummaryResponse(Article article) {
        ArticleSummaryResponse response = new ArticleSummaryResponse();
        response.setId(article.getId());
        response.setTitle(article.getTitle());
        response.setUsername(article.getUser().getUsername());
        response.setTags(article.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toSet()));
        response.setCreatedAt(article.getCreatedAt());
        
        // コメント数を取得
        long commentCount = commentRepository.countByArticleId(article.getId());
        response.setCommentCount(commentCount);
        
        return response;
    }
}
```

#### 4.3 CommentServiceの作成

`src/main/java/com/example/bloghub/service/CommentService.java`:

```java
package com.example.bloghub.service;

import com.example.bloghub.dto.comment.CommentCreateRequest;
import com.example.bloghub.dto.comment.CommentResponse;
import com.example.bloghub.dto.user.UserResponse;
import com.example.bloghub.entity.Article;
import com.example.bloghub.entity.Comment;
import com.example.bloghub.entity.User;
import com.example.bloghub.exception.ResourceNotFoundException;
import com.example.bloghub.exception.UnauthorizedException;
import com.example.bloghub.repository.ArticleRepository;
import com.example.bloghub.repository.CommentRepository;
import com.example.bloghub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {
    
    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    
    /**
     * コメントを作成
     */
    @Transactional
    public CommentResponse createComment(Long articleId, CommentCreateRequest request, String username) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article", articleId));
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setArticle(article);
        comment.setUser(user);
        
        Comment savedComment = commentRepository.save(comment);
        return convertToResponse(savedComment);
    }
    
    /**
     * コメントを削除
     */
    @Transactional
    public void deleteComment(Long commentId, String username) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));
        
        // 所有者チェック
        if (!comment.getUser().getUsername().equals(username)) {
            throw new UnauthorizedException("You can only delete your own comments");
        }
        
        commentRepository.delete(comment);
    }
    
    /**
     * 記事のコメント一覧を取得
     */
    public List<CommentResponse> getCommentsByArticleId(Long articleId) {
        // 記事の存在確認
        if (!articleRepository.existsById(articleId)) {
            throw new ResourceNotFoundException("Article", articleId);
        }
        
        List<Comment> comments = commentRepository.findByArticleIdOrderByCreatedAtAsc(articleId);
        return comments.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * CommentエンティティをCommentResponseに変換
     */
    private CommentResponse convertToResponse(Comment comment) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setContent(comment.getContent());
        
        UserResponse userResponse = new UserResponse();
        userResponse.setId(comment.getUser().getId());
        userResponse.setUsername(comment.getUser().getUsername());
        userResponse.setEmail(comment.getUser().getEmail());
        userResponse.setCreatedAt(comment.getUser().getCreatedAt());
        response.setUser(userResponse);
        
        response.setCreatedAt(comment.getCreatedAt());
        
        return response;
    }
}
```

---

### 手順5: Controller層の実装

#### 5.1 ArticleControllerの作成

`src/main/java/com/example/bloghub/controller/ArticleController.java`:

```java
package com.example.bloghub.controller;

import com.example.bloghub.dto.article.ArticleCreateRequest;
import com.example.bloghub.dto.article.ArticleResponse;
import com.example.bloghub.dto.article.ArticleSummaryResponse;
import com.example.bloghub.dto.article.ArticleUpdateRequest;
import com.example.bloghub.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {
    
    private final ArticleService articleService;
    
    /**
     * 記事を作成
     */
    @PostMapping
    public ResponseEntity<ArticleResponse> createArticle(
            @Valid @RequestBody ArticleCreateRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        ArticleResponse response = articleService.createArticle(request, username);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    /**
     * 記事一覧を取得（ページネーション、タグフィルター対応）
     */
    @GetMapping
    public ResponseEntity<Page<ArticleSummaryResponse>> getArticles(
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ArticleSummaryResponse> articles;
        
        if (tag != null && !tag.trim().isEmpty()) {
            articles = articleService.getArticlesByTag(tag, pageable);
        } else {
            articles = articleService.getAllArticles(pageable);
        }
        
        return ResponseEntity.ok(articles);
    }
    
    /**
     * 記事詳細を取得
     */
    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponse> getArticle(@PathVariable Long id) {
        ArticleResponse response = articleService.getArticleById(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 記事を更新
     */
    @PutMapping("/{id}")
    public ResponseEntity<ArticleResponse> updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody ArticleUpdateRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        ArticleResponse response = articleService.updateArticle(id, request, username);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 記事を削除
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArticle(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        articleService.deleteArticle(id, username);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 自分の記事一覧を取得
     */
    @GetMapping("/my")
    public ResponseEntity<Page<ArticleSummaryResponse>> getMyArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        String username = authentication.getName();
        Pageable pageable = PageRequest.of(page, size);
        Page<ArticleSummaryResponse> articles = articleService.getMyArticles(username, pageable);
        return ResponseEntity.ok(articles);
    }
}
```

#### 5.2 CommentControllerの作成

`src/main/java/com/example/bloghub/controller/CommentController.java`:

```java
package com.example.bloghub.controller;

import com.example.bloghub.dto.comment.CommentCreateRequest;
import com.example.bloghub.dto.comment.CommentResponse;
import com.example.bloghub.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentController {
    
    private final CommentService commentService;
    
    /**
     * コメントを作成
     */
    @PostMapping("/api/articles/{articleId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long articleId,
            @Valid @RequestBody CommentCreateRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        CommentResponse response = commentService.createComment(articleId, request, username);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    /**
     * 記事のコメント一覧を取得
     */
    @GetMapping("/api/articles/{articleId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long articleId) {
        List<CommentResponse> comments = commentService.getCommentsByArticleId(articleId);
        return ResponseEntity.ok(comments);
    }
    
    /**
     * コメントを削除
     */
    @DeleteMapping("/api/comments/{id}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        commentService.deleteComment(id, username);
        return ResponseEntity.noContent().build();
    }
}
```

#### 5.3 TagControllerの作成

`src/main/java/com/example/bloghub/controller/TagController.java`:

```java
package com.example.bloghub.controller;

import com.example.bloghub.entity.Tag;
import com.example.bloghub.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {
    
    private final TagService tagService;
    
    /**
     * すべてのタグを取得
     */
    @GetMapping
    public ResponseEntity<List<String>> getAllTags() {
        List<String> tags = tagService.getAllTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tags);
    }
}
```

---

### 手順6: ビルドと起動

すべてのコードを実装したら、アプリケーションをビルドして起動します。

```bash
# プロジェクトのルートディレクトリに移動
cd workspace/bloghub

# ビルド
./mvnw clean install

# 起動
./mvnw spring-boot:run
```

起動後、以下のURLでアプリケーションが動作していることを確認します：

```bash
curl http://localhost:8080/actuator/health
```

---

## ✅ 動作確認

実装した機能が正しく動作するか、curlコマンドで確認します。

### 1. ユーザー登録とログイン

```bash
# ユーザー登録
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "email": "alice@example.com",
    "password": "password123"
  }'

# ログイン（JWTトークンを取得）
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "password": "password123"
  }'
```

**期待される結果**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "username": "alice"
}
```

取得した`token`を環境変数に設定しておくと便利です：

```bash
export TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 2. 記事の作成

```bash
# 記事を作成
curl -X POST http://localhost:8080/api/articles \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Spring Bootの始め方",
    "content": "# はじめに\nSpring Bootは、Javaで簡単にWebアプリケーションを作成できるフレームワークです。\n\n## セットアップ\n1. JDKをインストール\n2. Spring Initializrでプロジェクト作成\n3. 依存関係を追加",
    "tags": ["Spring Boot", "Java", "Tutorial"]
  }'
```

**期待される結果**:
```json
{
  "id": 1,
  "title": "Spring Bootの始め方",
  "content": "# はじめに\nSpring Bootは...",
  "tags": ["Spring Boot", "Java", "Tutorial"],
  "user": {
    "id": 1,
    "username": "alice",
    "email": "alice@example.com",
    "createdAt": "2025-12-13T10:00:00"
  },
  "createdAt": "2025-12-13T10:30:00",
  "updatedAt": "2025-12-13T10:30:00",
  "commentCount": 0
}
```

### 3. 記事一覧の取得

```bash
# 記事一覧を取得（ページネーション）
curl "http://localhost:8080/api/articles?page=0&size=10"
```

**期待される結果**:
```json
{
  "content": [
    {
      "id": 1,
      "title": "Spring Bootの始め方",
      "username": "alice",
      "tags": ["Spring Boot", "Java", "Tutorial"],
      "createdAt": "2025-12-13T10:30:00",
      "commentCount": 0
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 1,
  "totalPages": 1
}
```

### 4. タグで検索

```bash
# タグで記事を検索
curl "http://localhost:8080/api/articles?tag=Spring%20Boot&page=0&size=10"
```

### 5. 記事詳細の取得

```bash
# 記事詳細を取得
curl http://localhost:8080/api/articles/1
```

### 6. 記事の更新

```bash
# 記事を更新
curl -X PUT http://localhost:8080/api/articles/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Spring Bootの始め方【改訂版】",
    "content": "# はじめに（更新版）\n...",
    "tags": ["Spring Boot", "Java", "Tutorial", "Updated"]
  }'
```

### 7. コメントの作成

```bash
# 別のユーザーを登録してログイン
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "bob",
    "email": "bob@example.com",
    "password": "password123"
  }'

# bobでログイン
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "bob",
    "password": "password123"
  }'

# bobのトークンを保存
export TOKEN_BOB="取得したトークン"

# bobがaliceの記事にコメント
curl -X POST http://localhost:8080/api/articles/1/comments \
  -H "Authorization: Bearer $TOKEN_BOB" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "とても参考になりました！ありがとうございます。"
  }'
```

**期待される結果**:
```json
{
  "id": 1,
  "content": "とても参考になりました！ありがとうございます。",
  "user": {
    "id": 2,
    "username": "bob",
    "email": "bob@example.com",
    "createdAt": "2025-12-13T10:35:00"
  },
  "createdAt": "2025-12-13T10:40:00"
}
```

### 8. コメント一覧の取得

```bash
# 記事のコメント一覧を取得
curl http://localhost:8080/api/articles/1/comments
```

**期待される結果**:
```json
[
  {
    "id": 1,
    "content": "とても参考になりました！ありがとうございます。",
    "user": {
      "id": 2,
      "username": "bob",
      "email": "bob@example.com",
      "createdAt": "2025-12-13T10:35:00"
    },
    "createdAt": "2025-12-13T10:40:00"
  }
]
```

### 9. 自分の記事一覧の取得

```bash
# 自分の記事一覧を取得
curl http://localhost:8080/api/articles/my \
  -H "Authorization: Bearer $TOKEN"
```

### 10. タグ一覧の取得

```bash
# すべてのタグを取得
curl http://localhost:8080/api/tags
```

**期待される結果**:
```json
["Spring Boot", "Java", "Tutorial", "Updated"]
```

### 11. 記事の削除

```bash
# 記事を削除（所有者のみ）
curl -X DELETE http://localhost:8080/api/articles/1 \
  -H "Authorization: Bearer $TOKEN"
```

**期待される結果**: HTTPステータス204 No Content

### 12. コメントの削除

```bash
# コメントを削除（所有者のみ）
curl -X DELETE http://localhost:8080/api/comments/1 \
  -H "Authorization: Bearer $TOKEN_BOB"
```

---

## 💪 チャレンジ課題

基本機能の理解を深めるため、以下の課題に挑戦してみましょう。

### 課題1: いいね機能を追加する

**要件**:
- Likeエンティティを作成（User、Article、createdAtを持つ）
- ユーザーは1つの記事に1回だけいいねできる（複合主キー）
- いいね数を記事一覧・詳細に表示
- `POST /api/articles/{id}/like`でいいね
- `DELETE /api/articles/{id}/like`でいいね解除

**ヒント**:
```java
@Entity
@Table(name = "likes")
@IdClass(LikeId.class)
public class Like {
    @Id
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @Id
    @ManyToOne
    @JoinColumn(name = "article_id")
    private Article article;
    
    private LocalDateTime createdAt;
}
```

### 課題2: 記事の下書き保存機能

**要件**:
- Articleエンティティに`published`フラグ（Boolean）を追加
- デフォルトはfalse（下書き）
- `PUT /api/articles/{id}/publish`で公開
- 公開されていない記事は所有者のみが閲覧可能
- 記事一覧には公開済みの記事のみ表示

**ヒント**:
```java
// Repositoryにカスタムクエリを追加
Page<Article> findByPublishedTrueOrderByCreatedAtDesc(Pageable pageable);
Page<Article> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable); // 自分の記事は下書きも含む
```

### 課題3: タグの人気順ソート

**要件**:
- 各タグが使用されている記事数をカウント
- `GET /api/tags/popular`で人気順（記事数降順）にタグを取得
- 記事数も一緒に返す

**ヒント**:
```java
@Query("SELECT t.name, COUNT(a) as count FROM Tag t JOIN t.articles a GROUP BY t.id ORDER BY count DESC")
List<Object[]> findTagsWithArticleCount();
```

---

## 🔍 トラブルシューティング

### エラー1: `ResourceNotFoundException: Article not found with id: 1`

**原因**: 指定したIDの記事が存在しない

**解決方法**:
1. 記事が作成されているか確認：
   ```bash
   curl http://localhost:8080/api/articles
   ```
2. 正しいIDを指定しているか確認
3. データベースの内容を確認：
   ```bash
   docker exec -it bloghub-mysql mysql -u root -prootpassword bloghub -e "SELECT * FROM articles;"
   ```

### エラー2: `UnauthorizedException: You can only update your own articles`

**原因**: 他のユーザーの記事を更新しようとしている

**解決方法**:
1. 正しいユーザーのトークンを使用しているか確認
2. 記事の所有者を確認：
   ```bash
   curl http://localhost:8080/api/articles/1
   ```
3. 自分の記事一覧を確認：
   ```bash
   curl http://localhost:8080/api/articles/my -H "Authorization: Bearer $TOKEN"
   ```

### エラー3: `MethodArgumentNotValidException: Validation failed`

**原因**: リクエストボディのバリデーションエラー

**解決方法**:
1. エラーレスポンスの`errors`フィールドを確認：
   ```json
   {
     "status": 400,
     "message": "Validation failed",
     "errors": {
       "title": "Title is required",
       "content": "Content must be between 1 and 10000 characters"
     }
   }
   ```
2. 必須フィールドが含まれているか確認
3. 文字数制限を守っているか確認

### エラー4: N+1問題によるパフォーマンス低下

**現象**: 記事一覧取得時にSQLが大量に実行される

**解決方法**:
1. `application.yml`でSQLログを有効化：
   ```yaml
   spring:
     jpa:
       show-sql: true
       properties:
         hibernate:
           format_sql: true
   ```
2. `JOIN FETCH`を使用しているか確認
3. 必要に応じて`@EntityGraph`を使用：
   ```java
   @EntityGraph(attributePaths = {"user", "tags"})
   Page<Article> findAllByOrderByCreatedAtDesc(Pageable pageable);
   ```

### エラー5: `LazyInitializationException`

**原因**: トランザクション外でLazy Loadingを試みている

**解決方法**:
1. `@Transactional(readOnly = true)`をサービスクラスに付与
2. `JOIN FETCH`でデータを事前にロード
3. DTOへの変換をトランザクション内で行う

---

## 📚 まとめ

お疲れさまでした！このステップでは、BlogHubアプリケーションのコア機能である**記事とコメント機能**を実装しました。

### 学んだこと

1. **カスタムクエリの実装**: `@Query`アノテーションでJPQLを記述し、複雑な検索条件を実装
2. **ページネーションの実装**: `Pageable`と`Page`を使った効率的なデータ取得
3. **N+1問題の回避**: `JOIN FETCH`による関連エンティティの一括取得
4. **所有者チェック**: 認証されたユーザーが自分のリソースのみを編集・削除できるセキュリティ実装
5. **タグの自動作成**: `findOrCreateTag`パターンによる柔軟なタグ管理
6. **ネストされたRESTfulルート**: `/api/articles/{articleId}/comments`によるリソースの階層構造表現
7. **グローバルエラーハンドリング**: `@ControllerAdvice`による統一的な例外処理
8. **DTOの活用**: エンティティとAPIレスポンスの分離による柔軟性向上
9. **複合的なビジネスロジック**: Repository、Service、Controllerの適切な役割分担
10. **RESTful API設計**: HTTPメソッド、ステータスコード、リソース設計のベストプラクティス

### 重要なポイント

- **N+1問題**: 関連エンティティを取得する際は`JOIN FETCH`を使用してクエリ数を削減
- **所有者チェック**: セキュリティの観点から、リソースの所有者のみが編集・削除できるよう制御
- **ページネーション**: 大量データを効率的に取得するため、必ずページネーションを実装
- **トランザクション管理**: `@Transactional`を適切に使用してデータ整合性を保証
- **バリデーション**: `@Valid`と`@NotBlank`などで入力値を検証
- **エラーハンドリング**: `GlobalExceptionHandler`で一貫性のあるエラーレスポンスを返す
- **RESTful設計**: ネストされたルート（`/articles/{id}/comments`）でリソースの関係性を明確に表現

---

## 🚀 次のステップへ

記事とコメント機能の実装が完了しました！次のステップでは、**画像アップロードと検索機能**を実装します。

👉 **[Step 37: 画像アップロードと検索機能](STEP_37.md)** に進みましょう！

Step 37では以下を学びます：
- ファイルアップロード処理（Step 30の応用）
- 画像とArticleの関連付け
- 全文検索機能の実装
- 複合的な検索条件（タグ + キーワード）
- 画像のサムネイル生成（チャレンジ課題）

**頑張ってください！ 🎉**
