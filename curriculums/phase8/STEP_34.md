# Step 34: プロジェクト概要と環境構築

## 🎯 このステップの目標

- 最終プロジェクト「ミニブログアプリケーション」の全体像を理解できる
- プロジェクトの技術スタックと機能要件を把握できる
- 開発環境を構築し、プロジェクトの初期設定を完了できる
- データベース設計とエンティティモデルを作成できる
- これまで学んだすべての技術を統合する準備ができる

**所要時間**: 約60分

---

## 📋 事前準備

- Phase 1〜7の全ステップを完了していること
- Docker Desktop、MySQL、VSCodeが正常に動作すること
- JWT認証、JPA、MyBatis、Thymeleaf、キャッシュ、非同期処理の基礎知識があること
- Gitリポジトリの準備（コード管理用）

---

## 🚀 ステップ1: プロジェクト概要

### 1-1. ミニブログアプリケーションとは

**プロジェクト名**: **BlogHub**（ブログハブ）

**コンセプト**: 
シンプルで実践的なブログプラットフォーム。ユーザーが記事を投稿し、他のユーザーがコメントできるコミュニティ型ブログシステム。

### 1-2. 機能要件

#### 認証・認可機能
- ✅ ユーザー登録（メールアドレス、パスワード、ユーザー名）
- ✅ ログイン（JWT認証）
- ✅ ログアウト
- ✅ プロフィール編集
- ✅ パスワード変更

#### 記事管理機能
- ✅ 記事投稿（タイトル、本文、画像、タグ）
- ✅ 記事編集（自分の記事のみ）
- ✅ 記事削除（自分の記事のみ）
- ✅ 記事一覧表示（ページネーション付き）
- ✅ 記事詳細表示
- ✅ タグによる記事検索
- ✅ キーワード検索

#### コメント機能
- ✅ 記事へのコメント投稿
- ✅ コメント編集（自分のコメントのみ）
- ✅ コメント削除（自分のコメントのみ）
- ✅ コメント一覧表示

#### ファイル管理機能
- ✅ 記事の画像アップロード
- ✅ プロフィール画像アップロード
- ✅ 画像のサムネイル表示

### 1-3. 非機能要件

#### パフォーマンス
- ✅ 記事一覧のキャッシュ（Caffeine）
- ✅ ページネーション（1ページ10件）
- ✅ 画像アップロードの非同期処理

#### セキュリティ
- ✅ JWT認証によるステートレス認証
- ✅ パスワードのBCrypt暗号化
- ✅ CSRF対策
- ✅ XSS対策（入力サニタイズ）
- ✅ ファイルアップロードの検証（拡張子、サイズ）

#### テスト
- ✅ ユニットテスト（カバレッジ70%以上）
- ✅ 統合テスト（主要なAPIエンドポイント）
- ✅ セキュリティテスト

### 1-4. 技術スタック

| 分類 | 技術 | 用途 |
|------|------|------|
| **フレームワーク** | Spring Boot 3.5.8 | アプリケーション基盤 |
| **データアクセス** | Spring Data JPA | 基本的なCRUD操作 |
| **データアクセス** | MyBatis | 複雑な検索クエリ |
| **データベース** | MySQL 8.0 | データ永続化 |
| **認証** | Spring Security + JWT | 認証・認可 |
| **テンプレート** | Thymeleaf | サーバーサイドレンダリング |
| **キャッシュ** | Caffeine | パフォーマンス最適化 |
| **ビルドツール** | Maven | 依存関係管理 |
| **テスト** | JUnit 5 + Mockito | ユニット/統合テスト |

### 1-5. アーキテクチャ

```
┌─────────────────────────────────────────────────┐
│                   Client (Browser)              │
│         (Thymeleaf + Bootstrap 5)               │
└─────────────────┬───────────────────────────────┘
                  │ HTTP/HTTPS
┌─────────────────▼───────────────────────────────┐
│              Controller Layer                   │
│  AuthController, ArticleController,             │
│  CommentController, FileController              │
└─────────────────┬───────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────┐
│               Service Layer                     │
│  AuthService, ArticleService,                   │
│  CommentService, FileStorageService             │
│  (@Transactional, @Cacheable, @Async)           │
└─────────┬───────────────────┬───────────────────┘
          │                   │
┌─────────▼─────────┐  ┌──────▼────────────────┐
│ Repository Layer  │  │  MyBatis Mapper       │
│  (Spring Data JPA)│  │  (Complex Queries)    │
│  UserRepository   │  │  ArticleSearchMapper  │
│  ArticleRepo...   │  │  CommentMapper        │
└─────────┬─────────┘  └──────┬────────────────┘
          │                   │
          └───────────┬───────┘
                      │
          ┌───────────▼────────────┐
          │   MySQL 8.0 Database   │
          │   (Docker Container)   │
          └────────────────────────┘
```

---

## 🚀 ステップ2: プロジェクトの作成

### 2-1. Spring Initializrでプロジェクト作成

[Spring Initializr](https://start.spring.io/)で以下の設定でプロジェクトを作成します：

**基本設定**:
- **Project**: Maven
- **Language**: Java
- **Spring Boot**: 3.5.8
- **Group**: `com.example`
- **Artifact**: `bloghub`
- **Name**: `BlogHub`
- **Package name**: `com.example.bloghub`
- **Packaging**: Jar
- **Java**: 21

**依存関係**:
```
Spring Web
Spring Data JPA
Spring Security
MySQL Driver
Lombok
Validation
Thymeleaf
Spring Boot DevTools
Spring Cache Abstraction
```

### 2-2. 追加依存関係の設定

`pom.xml`に以下の依存関係を追加します：

```xml
<!-- pom.xml -->
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>
    
    <!-- Thymeleaf Security Extension -->
    <dependency>
        <groupId>org.thymeleaf.extras</groupId>
        <artifactId>thymeleaf-extras-springsecurity6</artifactId>
    </dependency>
    
    <!-- Database -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- MyBatis -->
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>3.0.3</version>
    </dependency>
    
    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.5</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.5</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.5</version>
        <scope>runtime</scope>
    </dependency>
    
    <!-- Caffeine Cache -->
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
    </dependency>
    
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    
    <!-- DevTools -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-devtools</artifactId>
        <scope>runtime</scope>
        <optional>true</optional>
    </dependency>
    
    <!-- Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 🚀 ステップ3: データベース設計

### 3-1. ER図

```
┌──────────────────┐          ┌──────────────────┐
│      User        │          │     Article      │
├──────────────────┤          ├──────────────────┤
│ id (PK)          │─────┐    │ id (PK)          │
│ username         │     │    │ title            │
│ email (UNIQUE)   │     │    │ content          │
│ password         │     │    │ image_url        │
│ profile_image    │     └───<│ author_id (FK)   │
│ role             │          │ view_count       │
│ created_at       │          │ created_at       │
│ updated_at       │          │ updated_at       │
└──────────────────┘          └────────┬─────────┘
                                       │
                                       │
                              ┌────────▼─────────┐
                              │    Comment       │
                              ├──────────────────┤
                              │ id (PK)          │
                              │ content          │
                              │ article_id (FK)  │─┐
                              │ user_id (FK)     │ │
                              │ created_at       │ │
                              │ updated_at       │ │
                              └──────────────────┘ │
                                                   │
┌──────────────────┐          ┌──────────────────┐ │
│       Tag        │          │  Article_Tag     │ │
├──────────────────┤          ├──────────────────┤ │
│ id (PK)          │─────────<│ article_id (FK)  │<┘
│ name (UNIQUE)    │          │ tag_id (FK)      │
│ created_at       │          └──────────────────┘
└──────────────────┘
```

### 3-2. テーブル定義

#### Userテーブル
```sql
CREATE TABLE user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    profile_image VARCHAR(500),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
);
```

#### Articleテーブル
```sql
CREATE TABLE article (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    image_url VARCHAR(500),
    author_id BIGINT NOT NULL,
    view_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (author_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_author_id (author_id),
    INDEX idx_created_at (created_at DESC)
);
```

#### Commentテーブル
```sql
CREATE TABLE comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content TEXT NOT NULL,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (article_id) REFERENCES article(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_article_id (article_id),
    INDEX idx_user_id (user_id)
);
```

#### Tagテーブル
```sql
CREATE TABLE tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_name (name)
);
```

#### Article_Tagテーブル（多対多の中間テーブル）
```sql
CREATE TABLE article_tag (
    article_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (article_id, tag_id),
    FOREIGN KEY (article_id) REFERENCES article(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE
);
```

### 3-3. Docker Composeでデータベース構築

`docker-compose.yml`を作成：

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: bloghub-mysql
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: bloghub
      MYSQL_USER: bloghub_user
      MYSQL_PASSWORD: bloghub_pass
    ports:
      - "3307:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

volumes:
  mysql_data:
```

`init.sql`を作成（上記のテーブル定義をすべて含める）：

```sql
-- init.sql
CREATE TABLE user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    profile_image VARCHAR(500),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
);

CREATE TABLE article (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    image_url VARCHAR(500),
    author_id BIGINT NOT NULL,
    view_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (author_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_author_id (author_id),
    INDEX idx_created_at (created_at DESC)
);

CREATE TABLE comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content TEXT NOT NULL,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (article_id) REFERENCES article(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_article_id (article_id),
    INDEX idx_user_id (user_id)
);

CREATE TABLE tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_name (name)
);

CREATE TABLE article_tag (
    article_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (article_id, tag_id),
    FOREIGN KEY (article_id) REFERENCES article(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE
);

-- テストデータ（開発用）
INSERT INTO user (username, email, password, role) VALUES
('admin', 'admin@bloghub.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN'),
('testuser', 'test@bloghub.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER');
-- パスワードはどちらも "password123" (BCrypt)

INSERT INTO tag (name) VALUES ('Java'), ('Spring Boot'), ('開発'), ('チュートリアル'), ('Tips');
```

データベースを起動：

```bash
docker-compose up -d
```

---

## 🚀 ステップ4: 基本設定

### 4-1. application.ymlの設定

`src/main/resources/application.yml`を作成：

```yaml
spring:
  application:
    name: BlogHub
  
  # データソース設定
  datasource:
    url: jdbc:mysql://localhost:3307/bloghub?useSSL=false&serverTimezone=Asia/Tokyo&characterEncoding=utf8mb4
    username: bloghub_user
    password: bloghub_pass
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  # JPA設定
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect
  
  # MyBatis設定
  mybatis:
    mapper-locations: classpath:mybatis/mapper/**/*.xml
    type-aliases-package: com.example.bloghub.entities
    configuration:
      map-underscore-to-camel-case: true
  
  # ファイルアップロード設定
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 10MB
  
  # キャッシュ設定
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=600s
  
  # Thymeleaf設定
  thymeleaf:
    cache: false
    prefix: classpath:/templates/
    suffix: .html

# ファイルストレージ設定
file:
  upload-dir: uploads
  max-size: 5242880  # 5MB

# JWT設定
jwt:
  secret: YourSecretKeyHereMustBeLongEnoughForHS512Algorithm
  expiration: 86400000  # 24時間（ミリ秒）

# ログ設定
logging:
  level:
    com.example.bloghub: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
```

### 4-2. プロジェクトディレクトリ構造

以下の構造でパッケージを作成：

```
src/
├── main/
│   ├── java/com/example/bloghub/
│   │   ├── BlogHubApplication.java
│   │   ├── config/
│   │   │   ├── SecurityConfig.java
│   │   │   ├── CacheConfig.java
│   │   │   ├── AsyncConfig.java
│   │   │   └── WebConfig.java
│   │   ├── controllers/
│   │   │   ├── AuthController.java
│   │   │   ├── ArticleController.java
│   │   │   ├── CommentController.java
│   │   │   ├── FileController.java
│   │   │   └── HomeController.java
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── SignupRequest.java
│   │   │   │   ├── ArticleCreateRequest.java
│   │   │   │   └── CommentCreateRequest.java
│   │   │   └── response/
│   │   │       ├── JwtResponse.java
│   │   │       ├── ArticleResponse.java
│   │   │       ├── CommentResponse.java
│   │   │       └── PageResponse.java
│   │   ├── entities/
│   │   │   ├── User.java
│   │   │   ├── Article.java
│   │   │   ├── Comment.java
│   │   │   ├── Tag.java
│   │   │   └── Role.java (enum)
│   │   ├── repositories/
│   │   │   ├── UserRepository.java
│   │   │   ├── ArticleRepository.java
│   │   │   ├── CommentRepository.java
│   │   │   └── TagRepository.java
│   │   ├── mappers/
│   │   │   ├── ArticleSearchMapper.java
│   │   │   └── CommentMapper.java
│   │   ├── services/
│   │   │   ├── AuthService.java
│   │   │   ├── ArticleService.java
│   │   │   ├── CommentService.java
│   │   │   ├── TagService.java
│   │   │   ├── FileStorageService.java
│   │   │   └── UserDetailsServiceImpl.java
│   │   ├── security/
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── JwtTokenProvider.java
│   │   │   └── UserPrincipal.java
│   │   └── exception/
│   │       ├── GlobalExceptionHandler.java
│   │       ├── ResourceNotFoundException.java
│   │       └── UnauthorizedException.java
│   └── resources/
│       ├── application.yml
│       ├── mybatis/mapper/
│       │   ├── ArticleSearchMapper.xml
│       │   └── CommentMapper.xml
│       ├── templates/
│       │   ├── layout/
│       │   │   ├── header.html
│       │   │   └── footer.html
│       │   ├── auth/
│       │   │   ├── login.html
│       │   │   └── signup.html
│       │   ├── articles/
│       │   │   ├── list.html
│       │   │   ├── detail.html
│       │   │   ├── create.html
│       │   │   └── edit.html
│       │   └── index.html
│       └── static/
│           ├── css/
│           ├── js/
│           └── images/
└── test/
    └── java/com/example/bloghub/
        ├── controllers/
        ├── services/
        └── repositories/
```

---

## 🚀 ステップ5: エンティティの作成

### 5-1. Roleエンティティ（Enum）

`src/main/java/com/example/bloghub/entities/Role.java`を作成：

```java
package com.example.bloghub.entities;

public enum Role {
    USER,
    ADMIN
}
```

### 5-2. Userエンティティ

`src/main/java/com/example/bloghub/entities/User.java`を作成：

```java
package com.example.bloghub.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(length = 500)
    private String profileImage;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Article> articles = new ArrayList<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();
}
```

### 5-3. Articleエンティティ

`src/main/java/com/example/bloghub/entities/Article.java`を作成：

```java
package com.example.bloghub.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "article")
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
    
    @Column(length = 500)
    private String imageUrl;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer viewCount = 0;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();
    
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "article_tag",
        joinColumns = @JoinColumn(name = "article_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();
    
    // ヘルパーメソッド
    public void incrementViewCount() {
        this.viewCount++;
    }
    
    public void addTag(Tag tag) {
        this.tags.add(tag);
        tag.getArticles().add(this);
    }
    
    public void removeTag(Tag tag) {
        this.tags.remove(tag);
        tag.getArticles().remove(this);
    }
}
```

### 5-4. Commentエンティティ

`src/main/java/com/example/bloghub/entities/Comment.java`を作成：

```java
package com.example.bloghub.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "comment")
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
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

### 5-5. Tagエンティティ

`src/main/java/com/example/bloghub/entities/Tag.java`を作成：

```java
package com.example.bloghub.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tag")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String name;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @ManyToMany(mappedBy = "tags")
    @Builder.Default
    private Set<Article> articles = new HashSet<>();
}
```

---

## ✅ 動作確認

### 1. データベース起動確認

```bash
docker-compose ps
```

**期待される結果**:
```
NAME              IMAGE       STATUS
bloghub-mysql     mysql:8.0   Up
```

### 2. データベース接続確認

```bash
docker exec -it bloghub-mysql mysql -ubloghub_user -pbloghub_pass bloghub
```

MySQLに接続後、テーブル確認：

```sql
SHOW TABLES;
```

**期待される結果**:
```
+-------------------+
| Tables_in_bloghub |
+-------------------+
| article           |
| article_tag       |
| comment           |
| tag               |
| user              |
+-------------------+
```

### 3. アプリケーション起動確認

```bash
cd /path/to/bloghub
./mvnw clean install
./mvnw spring-boot:run
```

**期待されるログ**:
```
Started BlogHubApplication in X.XXX seconds
```

エラーがなく起動すれば成功です！

---

## 🎨 チャレンジ課題

基本が理解できたら、以下にチャレンジしてみましょう：

### チャレンジ 1: サンプルデータの追加

**目標**: 開発用のテストデータをもっと充実させる

**ヒント**:
```sql
-- init.sqlに追加
INSERT INTO article (title, content, author_id, view_count) VALUES
('Spring Bootの基礎', 'Spring Bootは...', 1, 120),
('JPAとMyBatisの使い分け', 'JPAは...', 1, 85),
('セキュリティ設定のベストプラクティス', 'Spring Securityで...', 2, 200);

INSERT INTO article_tag (article_id, tag_id) VALUES
(1, 2), (1, 3),
(2, 1), (2, 2),
(3, 2), (3, 5);
```

### チャレンジ 2: データベース設計の検証

**目標**: リレーションシップが正しく動作するか確認

**手順**:
1. ユーザーを削除したら記事も削除されるか？（CASCADE動作）
2. 記事を削除したらコメントも削除されるか？
3. タグを削除しても記事は残るか？

### チャレンジ 3: プロファイル分離

**目標**: 開発環境と本番環境で設定を分ける

**ヒント**:
- `application-dev.yml`: 開発環境用（ログレベルDEBUG）
- `application-prod.yml`: 本番環境用（ログレベルINFO、キャッシュ有効）

---

## 🐛 トラブルシューティング

### エラー: "Access denied for user 'bloghub_user'@'localhost'"

**原因**: データベース認証情報が間違っている

**解決策**:
1. `docker-compose.yml`の認証情報を確認
2. コンテナを再作成: `docker-compose down && docker-compose up -d`
3. `application.yml`のusername/passwordを確認

### エラー: "Table 'bloghub.user' doesn't exist"

**原因**: init.sqlが実行されていない

**解決策**:
```bash
# コンテナとボリュームを完全削除
docker-compose down -v

# 再度起動（init.sqlが自動実行される）
docker-compose up -d
```

### エラー: "Port 3307 is already in use"

**原因**: ポート3307が他のプロセスで使用中

**解決策**:
```bash
# ポートを使用しているプロセスを確認
lsof -ti:3307

# 別のポートに変更（docker-compose.ymlとapplication.yml両方）
# 例: 3308:3306
```

### エラー: "Failed to load ApplicationContext"

**原因**: 依存関係やBean定義の問題

**解決策**:
1. `./mvnw clean install`で依存関係を再取得
2. エラーメッセージを確認し、不足しているBean定義を追加
3. `@EnableJpaRepositories`や`@EntityScan`が必要な場合も

### エラー: "HikariPool connection timeout"

**原因**: データベースに接続できない

**解決策**:
1. MySQLコンテナが起動しているか確認: `docker-compose ps`
2. ポート番号が正しいか確認（3307）
3. ファイアウォールでポートがブロックされていないか確認

---

## 📚 このステップで学んだこと

- ✅ 最終プロジェクト「BlogHub」の全体像と機能要件を理解した
- ✅ Spring Boot + JPA + MyBatis + Thymeleafの統合プロジェクトを設計した
- ✅ データベーススキーマを設計し、ER図を作成した
- ✅ Docker Composeでデータベース環境を構築した
- ✅ エンティティモデル（User, Article, Comment, Tag）を作成した
- ✅ 1対多、多対多のリレーションシップを実装した
- ✅ プロジェクトの全体的なディレクトリ構造を理解した

---

## ➡️ 次のステップ

[Step 35: 認証・認可機能の実装](STEP_35.md)へ進みましょう！

次のステップでは、Spring SecurityとJWTを使った認証・認可機能を実装します：
- ユーザー登録・ログイン機能
- JWTトークンの発行と検証
- ログイン状態の管理
- ロール別のアクセス制御

プロジェクトの基盤が整ったので、いよいよセキュリティ機能の実装です！🔒
