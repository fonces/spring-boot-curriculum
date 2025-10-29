# Step 14: JPAとMyBatisの使い分け

## 🎯 このステップの目標

- JPAとMyBatisそれぞれの得意分野を理解する
- 同一プロジェクトでJPAとMyBatisを併用する方法を学ぶ
- トランザクション管理を統一して扱う
- 適切な技術選択の判断基準を身につける
- パフォーマンス特性の違いを理解する

**所要時間**: 約1.5時間

---

## 📋 事前準備

- Step 12, 13の完了
- JPAでのCRUD操作の理解（Phase 2）
- MyBatisでのSQL記述の理解（Step 12, 13）
- MySQLが起動していること

**Step 13をまだ完了していない場合**: [Step 13: MyBatisで複雑なクエリ](STEP_13.md)を先に進めてください。

---

## 💡 JPAとMyBatisの使い分け

### 技術選択の判断基準

| ケース | 推奨技術 | 理由 |
|--------|----------|------|
| シンプルなCRUD操作 | **JPA** | コード量が少なく、開発が高速 |
| 複雑な集計・レポート | **MyBatis** | SQLを直接制御でき、最適化しやすい |
| エンティティ間のリレーション管理 | **JPA** | `@OneToMany`等で自然に表現できる |
| 動的な検索条件 | **MyBatis** | 動的SQLが直感的で柔軟 |
| 複数テーブルのJOIN | **MyBatis** | SQLで明示的に書ける |
| トランザクション境界内の複数操作 | **両方可** | Spring Transactionで統一管理 |
| レガシーDB（命名規則が独特） | **MyBatis** | 柔軟なマッピングが可能 |
| ビジネスロジック中心 | **JPA** | オブジェクト指向的に実装できる |
| パフォーマンスクリティカル | **MyBatis** | 必要なカラムだけ取得、SQLチューニング容易 |

---

## 1. プロジェクト構成

### 1.1 依存関係（pom.xml）

JPAとMyBatisの両方を含める：

```xml
<dependencies>
    <!-- Spring Boot Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- MyBatis -->
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>3.0.3</version>
    </dependency>

    <!-- MySQL -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

### 1.2 application.yml

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/myapp
    username: user
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  # JPA設定
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect

# MyBatis設定
mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.example.demo.entity
  configuration:
    map-underscore-to-camel-case: true

# ログ設定
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.mybatis: DEBUG
```

---

## 2. 実践例: ブログシステム

### 2.1 テーブル設計

```sql
-- ユーザーテーブル（JPA管理）
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 記事テーブル（JPA管理）
CREATE TABLE articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    user_id BIGINT NOT NULL,
    published BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- コメントテーブル（JPA管理）
CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (article_id) REFERENCES articles(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- いいねテーブル（MyBatis管理）
CREATE TABLE article_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_article_user (article_id, user_id),
    FOREIGN KEY (article_id) REFERENCES articles(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 統計情報ビュー（MyBatis専用）
CREATE VIEW article_statistics AS
SELECT 
    a.id as article_id,
    a.title,
    u.username as author,
    COUNT(DISTINCT c.id) as comment_count,
    COUNT(DISTINCT al.id) as like_count,
    a.created_at
FROM articles a
LEFT JOIN users u ON a.user_id = u.id
LEFT JOIN comments c ON a.id = c.article_id
LEFT JOIN article_likes al ON a.id = al.article_id
GROUP BY a.id, a.title, u.username, a.created_at;
```

---

## 3. JPAエンティティの実装

### 3.1 Userエンティティ

**com/example/demo/entity/User.java**
```java
package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(nullable = false, length = 100)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Article> articles;
    
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

### 3.2 Articleエンティティ

**com/example/demo/entity/Article.java**
```java
package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "articles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Article {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    private Boolean published;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (published == null) {
            published = false;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### 3.3 Commentエンティティ

**com/example/demo/entity/Comment.java**
```java
package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

---

## 4. JPAリポジトリの実装

### 4.1 UserRepository

**com/example/demo/repository/UserRepository.java**
```java
package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
```

### 4.2 ArticleRepository

**com/example/demo/repository/ArticleRepository.java**
```java
package com.example.demo.repository;

import com.example.demo.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    
    List<Article> findByPublishedTrue();
    
    List<Article> findByUserId(Long userId);
    
    @Query("SELECT a FROM Article a WHERE a.published = true ORDER BY a.createdAt DESC")
    List<Article> findPublishedArticlesOrderByCreatedAtDesc();
}
```

---

## 5. MyBatis DTOとMapperの実装

### 5.1 統計情報DTO

**com/example/demo/dto/ArticleStatistics.java**
```java
package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleStatistics {
    private Long articleId;
    private String title;
    private String author;
    private Long commentCount;
    private Long likeCount;
    private LocalDateTime createdAt;
}
```

### 5.2 検索条件DTO

**com/example/demo/dto/ArticleSearchRequest.java**
```java
package com.example.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleSearchRequest {
    private String keyword;
    private Long userId;
    private Boolean published;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer minComments;
    private Integer minLikes;
    private String sortBy; // created_at, like_count, comment_count
    private String sortOrder; // asc, desc
}
```

### 5.3 ArticleStatisticsMapper

**com/example/demo/mapper/ArticleStatisticsMapper.java**
```java
package com.example.demo.mapper;

import com.example.demo.dto.ArticleSearchRequest;
import com.example.demo.dto.ArticleStatistics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ArticleStatisticsMapper {
    
    List<ArticleStatistics> findAllStatistics();
    
    ArticleStatistics findStatisticsByArticleId(@Param("articleId") Long articleId);
    
    List<ArticleStatistics> searchWithStatistics(ArticleSearchRequest request);
    
    List<ArticleStatistics> findPopularArticles(@Param("limit") int limit);
    
    void addLike(@Param("articleId") Long articleId, @Param("userId") Long userId);
    
    void removeLike(@Param("articleId") Long articleId, @Param("userId") Long userId);
    
    boolean isLikedByUser(@Param("articleId") Long articleId, @Param("userId") Long userId);
}
```

### 5.4 Mapper XML

**resources/mapper/ArticleStatisticsMapper.xml**
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.demo.mapper.ArticleStatisticsMapper">

    <!-- ResultMap -->
    <resultMap id="articleStatisticsResultMap" type="com.example.demo.dto.ArticleStatistics">
        <result property="articleId" column="article_id"/>
        <result property="title" column="title"/>
        <result property="author" column="author"/>
        <result property="commentCount" column="comment_count"/>
        <result property="likeCount" column="like_count"/>
        <result property="createdAt" column="created_at"/>
    </resultMap>

    <!-- 全記事の統計情報を取得 -->
    <select id="findAllStatistics" resultMap="articleStatisticsResultMap">
        SELECT * FROM article_statistics
        ORDER BY created_at DESC
    </select>

    <!-- 特定記事の統計情報を取得 -->
    <select id="findStatisticsByArticleId" resultMap="articleStatisticsResultMap">
        SELECT * FROM article_statistics
        WHERE article_id = #{articleId}
    </select>

    <!-- 動的検索 -->
    <select id="searchWithStatistics" resultMap="articleStatisticsResultMap">
        SELECT 
            a.id as article_id,
            a.title,
            u.username as author,
            COUNT(DISTINCT c.id) as comment_count,
            COUNT(DISTINCT al.id) as like_count,
            a.created_at
        FROM articles a
        LEFT JOIN users u ON a.user_id = u.id
        LEFT JOIN comments c ON a.id = c.article_id
        LEFT JOIN article_likes al ON a.id = al.article_id
        <where>
            <if test="keyword != null and keyword != ''">
                AND (a.title LIKE CONCAT('%', #{keyword}, '%') 
                     OR a.content LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="userId != null">
                AND a.user_id = #{userId}
            </if>
            <if test="published != null">
                AND a.published = #{published}
            </if>
            <if test="startDate != null">
                AND a.created_at &gt;= #{startDate}
            </if>
            <if test="endDate != null">
                AND a.created_at &lt;= #{endDate}
            </if>
        </where>
        GROUP BY a.id, a.title, u.username, a.created_at
        <if test="minComments != null">
            HAVING comment_count &gt;= #{minComments}
        </if>
        <if test="minLikes != null">
            <if test="minComments != null">AND</if>
            <if test="minComments == null">HAVING</if>
            like_count &gt;= #{minLikes}
        </if>
        <if test="sortBy != null">
            ORDER BY
            <choose>
                <when test="sortBy == 'like_count'">like_count</when>
                <when test="sortBy == 'comment_count'">comment_count</when>
                <otherwise>created_at</otherwise>
            </choose>
            <choose>
                <when test="sortOrder == 'asc'">ASC</when>
                <otherwise>DESC</otherwise>
            </choose>
        </if>
    </select>

    <!-- 人気記事を取得 -->
    <select id="findPopularArticles" resultMap="articleStatisticsResultMap">
        SELECT * FROM article_statistics
        ORDER BY like_count DESC, comment_count DESC
        LIMIT #{limit}
    </select>

    <!-- いいねを追加 -->
    <insert id="addLike">
        INSERT INTO article_likes (article_id, user_id)
        VALUES (#{articleId}, #{userId})
    </insert>

    <!-- いいねを削除 -->
    <delete id="removeLike">
        DELETE FROM article_likes
        WHERE article_id = #{articleId} AND user_id = #{userId}
    </delete>

    <!-- ユーザーがいいね済みか確認 -->
    <select id="isLikedByUser" resultType="boolean">
        SELECT COUNT(*) > 0
        FROM article_likes
        WHERE article_id = #{articleId} AND user_id = #{userId}
    </select>

</mapper>
```

---

## 6. Serviceレイヤーで併用

### 6.1 ArticleService

**com/example/demo/service/ArticleService.java**
```java
package com.example.demo.service;

import com.example.demo.dto.ArticleSearchRequest;
import com.example.demo.dto.ArticleStatistics;
import com.example.demo.entity.Article;
import com.example.demo.entity.User;
import com.example.demo.mapper.ArticleStatisticsMapper;
import com.example.demo.repository.ArticleRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleService {
    
    private final ArticleRepository articleRepository; // JPA
    private final UserRepository userRepository; // JPA
    private final ArticleStatisticsMapper statisticsMapper; // MyBatis
    
    /**
     * 記事作成 - JPAを使用（シンプルなCRUD）
     */
    @Transactional
    public Article createArticle(Long userId, String title, String content) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Article article = Article.builder()
                .title(title)
                .content(content)
                .user(user)
                .published(false)
                .build();
        
        return articleRepository.save(article);
    }
    
    /**
     * 記事更新 - JPAを使用（エンティティベースの操作）
     */
    @Transactional
    public Article updateArticle(Long articleId, String title, String content) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article not found"));
        
        article.setTitle(title);
        article.setContent(content);
        
        return articleRepository.save(article);
    }
    
    /**
     * 記事削除 - JPAを使用（関連データもカスケード削除）
     */
    @Transactional
    public void deleteArticle(Long articleId) {
        articleRepository.deleteById(articleId);
    }
    
    /**
     * 公開済み記事一覧 - JPAを使用（シンプルな取得）
     */
    @Transactional(readOnly = true)
    public List<Article> getPublishedArticles() {
        return articleRepository.findPublishedArticlesOrderByCreatedAtDesc();
    }
    
    /**
     * 統計情報付き記事一覧 - MyBatisを使用（複雑な集計）
     */
    @Transactional(readOnly = true)
    public List<ArticleStatistics> getArticlesWithStatistics() {
        return statisticsMapper.findAllStatistics();
    }
    
    /**
     * 高度な検索 - MyBatisを使用（動的SQL）
     */
    @Transactional(readOnly = true)
    public List<ArticleStatistics> searchArticles(ArticleSearchRequest request) {
        return statisticsMapper.searchWithStatistics(request);
    }
    
    /**
     * 人気記事取得 - MyBatisを使用（複雑なソート）
     */
    @Transactional(readOnly = true)
    public List<ArticleStatistics> getPopularArticles(int limit) {
        return statisticsMapper.findPopularArticles(limit);
    }
    
    /**
     * いいね機能 - MyBatisとJPAの併用
     */
    @Transactional
    public void toggleLike(Long articleId, Long userId) {
        // 記事の存在確認はJPA
        articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article not found"));
        
        // いいね操作はMyBatis（シンプルなINSERT/DELETE）
        if (statisticsMapper.isLikedByUser(articleId, userId)) {
            statisticsMapper.removeLike(articleId, userId);
        } else {
            statisticsMapper.addLike(articleId, userId);
        }
    }
}
```

---

## 7. トランザクション管理の統一

### 7.1 Spring Transactionの仕組み

JPAとMyBatisは両方とも **同じDataSource** を使用し、Spring Transactionで統一管理されます。

```java
@Transactional
public void complexOperation() {
    // JPA操作
    User user = userRepository.save(newUser);
    
    // MyBatis操作
    statisticsMapper.addLike(articleId, user.getId());
    
    // どちらかでエラーが発生すると、両方ロールバック
}
```

### 7.2 トランザクション設定

**com/example/demo/config/TransactionConfig.java**
```java
package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class TransactionConfig {
    // Spring Bootが自動的にトランザクションマネージャーを設定
    // JPAとMyBatisで同じDataSourceを共有
}
```

---

## 8. Controllerの実装

**com/example/demo/controller/ArticleController.java**
```java
package com.example.demo.controller;

import com.example.demo.dto.ArticleSearchRequest;
import com.example.demo.dto.ArticleStatistics;
import com.example.demo.entity.Article;
import com.example.demo.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {
    
    private final ArticleService articleService;
    
    // JPA経由
    @PostMapping
    public Article createArticle(
            @RequestParam Long userId,
            @RequestParam String title,
            @RequestParam String content
    ) {
        return articleService.createArticle(userId, title, content);
    }
    
    // JPA経由
    @GetMapping("/published")
    public List<Article> getPublishedArticles() {
        return articleService.getPublishedArticles();
    }
    
    // MyBatis経由（統計情報付き）
    @GetMapping("/statistics")
    public List<ArticleStatistics> getArticlesWithStatistics() {
        return articleService.getArticlesWithStatistics();
    }
    
    // MyBatis経由（動的検索）
    @GetMapping("/search")
    public List<ArticleStatistics> searchArticles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Boolean published,
            @RequestParam(required = false) Integer minComments,
            @RequestParam(required = false) Integer minLikes,
            @RequestParam(defaultValue = "created_at") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder
    ) {
        ArticleSearchRequest request = new ArticleSearchRequest();
        request.setKeyword(keyword);
        request.setUserId(userId);
        request.setPublished(published);
        request.setMinComments(minComments);
        request.setMinLikes(minLikes);
        request.setSortBy(sortBy);
        request.setSortOrder(sortOrder);
        
        return articleService.searchArticles(request);
    }
    
    // MyBatis経由（人気記事）
    @GetMapping("/popular")
    public List<ArticleStatistics> getPopularArticles(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return articleService.getPopularArticles(limit);
    }
    
    // JPA + MyBatis併用
    @PostMapping("/{articleId}/like")
    public void toggleLike(
            @PathVariable Long articleId,
            @RequestParam Long userId
    ) {
        articleService.toggleLike(articleId, userId);
    }
}
```

---

## 9. 動作確認

### 9.1 アプリケーション起動

```bash
./mvnw spring-boot:run
```

### 9.2 テストデータ投入

```bash
# ユーザー作成
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com","password":"pass123"}'

# 記事作成
curl -X POST "http://localhost:8080/api/articles?userId=1&title=MyFirstArticle&content=HelloWorld"

# いいね
curl -X POST "http://localhost:8080/api/articles/1/like?userId=1"
```

### 9.3 各種検索

```bash
# 統計情報付き一覧（MyBatis）
curl http://localhost:8080/api/articles/statistics | jq

# 動的検索（MyBatis）
curl "http://localhost:8080/api/articles/search?keyword=Spring&minLikes=5&sortBy=like_count&sortOrder=desc" | jq

# 人気記事TOP10（MyBatis）
curl "http://localhost:8080/api/articles/popular?limit=10" | jq

# 公開済み記事（JPA）
curl http://localhost:8080/api/articles/published | jq
```

---

## 10. パフォーマンス比較

### 10.1 N+1問題の検証

**❌ JPA（N+1問題が発生）**
```java
// 記事一覧を取得（1回のクエリ）
List<Article> articles = articleRepository.findAll();

// 各記事のコメント数を取得（N回のクエリ）
for (Article article : articles) {
    int commentCount = article.getComments().size(); // Lazy Loading
}
```

**✅ MyBatis（1回のクエリ）**
```java
// 統計情報を含めて一度に取得
List<ArticleStatistics> statistics = statisticsMapper.findAllStatistics();
```

### 10.2 必要なカラムだけ取得

**JPA（全カラム取得）**
```java
List<Article> articles = articleRepository.findAll();
// SELECT * FROM articles が実行される
```

**MyBatis（必要なカラムのみ）**
```xml
<select id="findTitlesOnly" resultType="String">
    SELECT title FROM articles
</select>
```

---

## 11. まとめ

### 使い分けの実践的ガイドライン

#### JPAを選ぶ場合
- ✅ CRUD操作が中心
- ✅ エンティティ間のリレーションを扱う
- ✅ オブジェクト指向で設計したい
- ✅ 開発速度を重視

#### MyBatisを選ぶ場合
- ✅ 複雑な集計・レポート
- ✅ 動的な検索条件が多い
- ✅ パフォーマンス最適化が必要
- ✅ SQLを直接制御したい

#### 併用のベストプラクティス
1. **基本CRUD**: JPA
2. **複雑な検索**: MyBatis
3. **レポート/統計**: MyBatis
4. **トランザクション管理**: Spring Transaction（統一）
5. **1つのServiceで両方使用可**: 適材適所で使い分け

### できるようになったこと
✅ JPAとMyBatisの違いを理解  
✅ 同一プロジェクトでの併用方法を習得  
✅ トランザクションの統一管理  
✅ 適切な技術選択の判断基準  
✅ パフォーマンス特性の理解  

---

## 🔄 Gitへのコミットとレビュー依頼

進捗を記録してレビューを受けましょう：

```bash
git add .
git commit -m "Step 14: JPAとMyBatisの使い分け完了"
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

レビューが完了したら、**Phase 4: アーキテクチャとベストプラクティス**へ進みましょう！

Phase 3が完了しました！  
次は **Phase 4: アーキテクチャとベストプラクティス** で、より実践的な設計パターンを学びます。

[Step 15: レイヤー化アーキテクチャ](../phase4/STEP_15.md)で、責務を分離した設計を学びます。

---

## 参考資料
- [Spring Data JPA Documentation](https://spring.pleiades.io/spring-data/jpa/docs/current/reference/html/)
- [MyBatis Spring Boot Starter](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)
- [Spring Transaction Management](https://spring.pleiades.io/spring-framework/docs/current/reference/html/data-access.html#transaction)

