# Step 34: プロジェクト概要と環境構築

## 🎯 このステップの目標

- BlogHubアプリケーションの全体像と要件を理解する
- ER図を元にデータベース設計を理解する
- Docker ComposeでMySQL環境を構築する
- JPAエンティティクラスを作成し、テーブル間の関連を定義する
- データベース初期化スクリプトを作成し、テストデータを投入する

**所要時間**: 約60分

---

## 📋 事前準備

このステップを始める前に、以下を確認してください：

- Phase 1〜7のカリキュラムを完了していること
- Docker Desktop がインストールされ、起動していること
- Java 21、Maven、VSCodeがインストールされていること
- 以下のコマンドで環境を確認できること：

```bash
docker --version
docker-compose --version
java -version
./mvnw --version
```

---

## 🎨 BlogHubアプリケーションとは

### アプリケーション概要

**BlogHub**は、複数のユーザーが記事を投稿し、コメントやタグで情報を整理できるミニブログプラットフォームです。これまで学んだすべての技術を統合し、実践的なWebアプリケーションを開発します。

### 主要機能

1. **ユーザー管理**
   - ユーザー登録（サインアップ）
   - ログイン/ログアウト（JWT認証）
   - プロフィール画像のアップロード

2. **記事管理**
   - 記事の作成・編集・削除（CRUD）
   - マークダウン形式での記事作成
   - タグによる記事の分類
   - 記事の検索（タイトル、本文、タグ）

3. **コメント機能**
   - 記事へのコメント投稿
   - コメントの削除（投稿者のみ）

4. **検索機能**
   - キーワード検索（MyBatis動的SQL）
   - タグフィルタリング
   - ページネーション

5. **Web画面（Thymeleaf）**
   - 記事一覧ページ
   - 記事詳細ページ
   - 記事投稿・編集フォーム
   - ユーザー登録・ログインフォーム

### 技術スタック

| レイヤー | 技術 |
|---------|------|
| **バックエンド** | Spring Boot 3.5.X |
| **データアクセス** | Spring Data JPA + MyBatis |
| **認証** | Spring Security + JWT |
| **テンプレート** | Thymeleaf |
| **データベース** | MySQL 8.0 (Docker) |
| **ビルドツール** | Maven |
| **テスト** | JUnit 5 + Mockito + JaCoCo |

---

## 🗄️ データベース設計

### ER図

```
┌─────────────────┐
│      User       │
├─────────────────┤
│ id (PK)         │
│ username        │
│ email           │
│ password        │
│ profile_image   │
│ created_at      │
│ updated_at      │
└────────┬────────┘
         │
         │ 1
         │
         │ N
         │
┌────────┴────────┐      N ┌─────────────────┐ N
│     Article     │────────┤  Article_Tag    │────────┐
├─────────────────┤        ├─────────────────┤        │
│ id (PK)         │        │ article_id (FK) │        │
│ title           │        │ tag_id (FK)     │        │
│ content         │        └─────────────────┘        │
│ user_id (FK)    │                                   │
│ created_at      │                           ┌───────┴────────┐
│ updated_at      │                           │      Tag       │
└────────┬────────┘                           ├────────────────┤
         │                                    │ id (PK)        │
         │ 1                                  │ name           │
         │                                    │ created_at     │
         │ N                                  └────────────────┘
         │
┌────────┴────────┐
│    Comment      │
├─────────────────┤
│ id (PK)         │
│ content         │
│ article_id (FK) │
│ user_id (FK)    │
│ created_at      │
└─────────────────┘
```

### テーブル定義

#### 1. `users` テーブル

| カラム名 | 型 | 制約 | 説明 |
|---------|-----|------|------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | ユーザーID |
| `username` | VARCHAR(50) | UNIQUE, NOT NULL | ユーザー名 |
| `email` | VARCHAR(100) | UNIQUE, NOT NULL | メールアドレス |
| `password` | VARCHAR(255) | NOT NULL | ハッシュ化されたパスワード |
| `profile_image` | VARCHAR(255) | NULL | プロフィール画像のパス |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新日時 |

#### 2. `articles` テーブル

| カラム名 | 型 | 制約 | 説明 |
|---------|-----|------|------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 記事ID |
| `title` | VARCHAR(200) | NOT NULL | タイトル |
| `content` | TEXT | NOT NULL | 本文（マークダウン） |
| `user_id` | BIGINT | FOREIGN KEY → users(id) | 投稿者ID |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新日時 |

#### 3. `comments` テーブル

| カラム名 | 型 | 制約 | 説明 |
|---------|-----|------|------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | コメントID |
| `content` | TEXT | NOT NULL | コメント内容 |
| `article_id` | BIGINT | FOREIGN KEY → articles(id) | 記事ID |
| `user_id` | BIGINT | FOREIGN KEY → users(id) | 投稿者ID |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 作成日時 |

#### 4. `tags` テーブル

| カラム名 | 型 | 制約 | 説明 |
|---------|-----|------|------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | タグID |
| `name` | VARCHAR(50) | UNIQUE, NOT NULL | タグ名 |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 作成日時 |

#### 5. `article_tags` テーブル（中間テーブル）

| カラム名 | 型 | 制約 | 説明 |
|---------|-----|------|------|
| `article_id` | BIGINT | FOREIGN KEY → articles(id) | 記事ID |
| `tag_id` | BIGINT | FOREIGN KEY → tags(id) | タグID |

- 複合主キー: `(article_id, tag_id)`

---

## 🚀 ステップ1: プロジェクト作成

### 1-1. Spring Initializrでプロジェクト生成

ブラウザで [https://start.spring.io/](https://start.spring.io/) にアクセスし、以下の設定でプロジェクトを生成します：

**プロジェクト設定**:
- **Project**: Maven
- **Language**: Java
- **Spring Boot**: 3.5.8
- **Packaging**: Jar
- **Java**: 21

**Project Metadata**:
- **Group**: `com.example`
- **Artifact**: `bloghub`
- **Name**: `bloghub`
- **Package name**: `com.example.bloghub`

**依存関係（Dependencies）**:
- Spring Web
- Spring Data JPA
- Spring Security
- MySQL Driver
- Lombok
- Validation
- Thymeleaf
- MyBatis Framework

### 1-2. プロジェクトのダウンロードと配置

1. **GENERATE**ボタンをクリックして`bloghub.zip`をダウンロード
2. zipファイルを解凍
3. `workspace/bloghub/`ディレクトリに配置

```bash
cd ~/git/spring-boot-curriculum/workspace
unzip ~/Downloads/bloghub.zip
cd bloghub
```

### 1-3. プロジェクト構造の確認

```
bloghub/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           └── bloghub/
│   │   │               └── BloghubApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── static/
│   │       └── templates/
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── bloghub/
├── pom.xml
└── mvnw
```

---

## 🚀 ステップ2: Docker ComposeでMySQL環境構築

### 2-1. docker-compose.ymlの作成

プロジェクトルート（`bloghub/`）に`docker-compose.yml`を作成します：

**ファイルパス**: `bloghub/docker-compose.yml`

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
      MYSQL_PASSWORD: bloghub_password
    ports:
      - "3307:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    command: --default-authentication-plugin=mysql_native_password
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      timeout: 20s
      retries: 10

volumes:
  mysql_data:
```

### 2-2. コードの解説

#### `ports: "3307:3306"`
- ホストの**3307番ポート**をコンテナの3306番ポートにマッピング
- Phase 2の`hello-spring-boot`プロジェクトがポート3306を使用している場合があるため、衝突を避ける

#### `volumes`
- `mysql_data`: データの永続化（コンテナ削除後もデータ保持）
- `./init.sql`: 初回起動時に自動実行されるSQLスクリプト

#### `healthcheck`
- MySQLが完全に起動するまで待機する設定

---

## 🚀 ステップ3: データベース初期化スクリプト作成

### 3-1. init.sqlの作成

**ファイルパス**: `bloghub/init.sql`

```sql
-- データベースの選択
USE bloghub;

-- テーブルが存在する場合は削除（開発用）
DROP TABLE IF EXISTS article_tags;
DROP TABLE IF EXISTS comments;
DROP TABLE IF EXISTS articles;
DROP TABLE IF EXISTS tags;
DROP TABLE IF EXISTS users;

-- 1. usersテーブル
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    profile_image VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. articlesテーブル
CREATE TABLE articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. commentsテーブル
CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content TEXT NOT NULL,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_article_id (article_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. tagsテーブル
CREATE TABLE tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. article_tagsテーブル（中間テーブル）
CREATE TABLE article_tags (
    article_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (article_id, tag_id),
    FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- テストデータの投入
-- パスワードは "password123" のBCryptハッシュ（実際のアプリではSpring Securityで生成）
INSERT INTO users (username, email, password) VALUES
('alice', 'alice@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'),
('bob', 'bob@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy');

INSERT INTO tags (name) VALUES
('Spring Boot'),
('Java'),
('Database'),
('Tutorial');

INSERT INTO articles (title, content, user_id) VALUES
('Spring Bootの始め方', '# Spring Boot入門\n\nSpring Bootは...', 1),
('JPAとMyBatisの違い', '## データアクセス技術の比較\n\nJPAは...', 1),
('Docker入門', 'Dockerを使うと...', 2);

INSERT INTO article_tags (article_id, tag_id) VALUES
(1, 1), -- Spring Bootの始め方 → Spring Boot
(1, 4), -- Spring Bootの始め方 → Tutorial
(2, 1), -- JPAとMyBatisの違い → Spring Boot
(2, 3), -- JPAとMyBatisの違い → Database
(3, 4); -- Docker入門 → Tutorial

INSERT INTO comments (content, article_id, user_id) VALUES
('とても参考になりました！', 1, 2),
('続きが気になります', 1, 2),
('JPAの方が使いやすいですね', 2, 2);
```

### 3-2. SQLスクリプトの解説

#### インデックス設定
```sql
INDEX idx_user_id (user_id),
INDEX idx_created_at (created_at)
```
- 検索パフォーマンスを向上させるためのインデックス
- `user_id`での検索や`created_at`でのソートが高速化される

#### カスケード削除
```sql
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
```
- ユーザーが削除されたとき、そのユーザーの記事・コメントも自動削除
- データの整合性を保つ

#### 文字コード設定
```sql
ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
```
- **utf8mb4**: 絵文字を含むすべてのUnicode文字に対応
- **InnoDB**: トランザクション対応のストレージエンジン

---

## 🚀 ステップ4: application.ymlの設定

### 4-1. application.ymlの作成

**ファイルパス**: `src/main/resources/application.yml`

```yaml
spring:
  application:
    name: bloghub

  datasource:
    url: jdbc:mysql://localhost:3307/bloghub?useSSL=false&serverTimezone=Asia/Tokyo&allowPublicKeyRetrieval=true
    username: bloghub_user
    password: bloghub_password
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect

  security:
    user:
      name: admin
      password: admin123

  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

mybatis:
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true

logging:
  level:
    com.example.bloghub: DEBUG
    org.springframework.security: DEBUG

server:
  port: 8080
```

### 4-2. 設定の解説

#### `ddl-auto: validate`
- **validate**: エンティティとテーブル定義が一致するか検証のみ
- **create**: 毎回テーブルを作り直す（開発初期のみ）
- **update**: スキーマを自動更新（本番では非推奨）
- **none**: Hibernateによるスキーマ管理を無効化

今回は`init.sql`でテーブルを作成するため、`validate`を使用します。

#### `multipart`
- ファイルアップロード機能で使用
- 最大10MBまでのファイルを受け付ける

---

## 🚀 ステップ5: エンティティクラスの作成

### 5-1. パッケージ構成

```
com.example.bloghub/
├── BloghubApplication.java
└── entity/
    ├── User.java
    ├── Article.java
    ├── Comment.java
    └── Tag.java
```

### 5-2. Userエンティティの作成

**ファイルパス**: `src/main/java/com/example/bloghub/entity/User.java`

```java
package com.example.bloghub.entity;

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
@Table(name = "users")
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

    @Column(name = "profile_image")
    private String profileImage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 1対多のリレーション: ユーザーは複数の記事を持つ
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Article> articles = new ArrayList<>();

    // 1対多のリレーション: ユーザーは複数のコメントを持つ
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();
}
```

### 5-3. Articleエンティティの作成

**ファイルパス**: `src/main/java/com/example/bloghub/entity/Article.java`

```java
package com.example.bloghub.entity;

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

    // 多対1のリレーション: 記事は1人のユーザーに属する
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 1対多のリレーション: 記事は複数のコメントを持つ
    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    // 多対多のリレーション: 記事は複数のタグを持つ
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "article_tags",
        joinColumns = @JoinColumn(name = "article_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();
}
```

### 5-4. Commentエンティティの作成

**ファイルパス**: `src/main/java/com/example/bloghub/entity/Comment.java`

```java
package com.example.bloghub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

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

    // 多対1のリレーション: コメントは1つの記事に属する
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    // 多対1のリレーション: コメントは1人のユーザーに属する
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
```

### 5-5. Tagエンティティの作成

**ファイルパス**: `src/main/java/com/example/bloghub/entity/Tag.java`

```java
package com.example.bloghub.entity;

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
@Table(name = "tags")
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
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 多対多のリレーション: タグは複数の記事を持つ
    @ManyToMany(mappedBy = "tags")
    @Builder.Default
    private Set<Article> articles = new HashSet<>();
}
```

### 5-6. エンティティの解説

#### `@ManyToOne` vs `@OneToMany`

```java
// Article側（多側）
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id")
private User user;

// User側（1側）
@OneToMany(mappedBy = "user")
private List<Article> articles;
```

- **`mappedBy`**: リレーションの所有者を示す（外部キーを持つのはArticle側）
- **`FetchType.LAZY`**: 必要になるまでデータを取得しない（N+1問題を防ぐ）

#### `@ManyToMany`

```java
@ManyToMany
@JoinTable(
    name = "article_tags",
    joinColumns = @JoinColumn(name = "article_id"),
    inverseJoinColumns = @JoinColumn(name = "tag_id")
)
private Set<Tag> tags;
```

- 中間テーブル`article_tags`を自動生成（今回はinit.sqlで手動作成）
- `Set`を使うことで重複を防ぐ

#### `cascade` と `orphanRemoval`

```java
@OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Comment> comments;
```

- **`cascade = CascadeType.ALL`**: 親エンティティの操作（保存、削除）を子にも適用
- **`orphanRemoval = true`**: 親から切り離された子エンティティを自動削除

#### `@CreationTimestamp` と `@UpdateTimestamp`

```java
@CreationTimestamp
@Column(name = "created_at", updatable = false)
private LocalDateTime createdAt;

@UpdateTimestamp
@Column(name = "updated_at")
private LocalDateTime updatedAt;
```

- Hibernateが自動的にタイムスタンプを設定
- `updatable = false`で作成日時の上書きを防ぐ

---

## ✅ ステップ6: 動作確認

### 6-1. MySQLコンテナの起動

```bash
cd ~/git/spring-boot-curriculum/workspace/bloghub
docker-compose up -d
```

**期待される結果**:
```
[+] Running 2/2
 ✔ Network bloghub_default      Created
 ✔ Container bloghub-mysql      Started
```

### 6-2. MySQL接続確認

```bash
docker exec -it bloghub-mysql mysql -u bloghub_user -pbloghub_password bloghub
```

MySQLコンソールで以下のコマンドを実行：

```sql
SHOW TABLES;
```

**期待される結果**:
```
+-------------------+
| Tables_in_bloghub |
+-------------------+
| article_tags      |
| articles          |
| comments          |
| tags              |
| users             |
+-------------------+
5 rows in set (0.00 sec)
```

テストデータの確認：

```sql
SELECT username, email FROM users;
```

**期待される結果**:
```
+----------+-------------------+
| username | email             |
+----------+-------------------+
| alice    | alice@example.com |
| bob      | bob@example.com   |
+----------+-------------------+
2 rows in set (0.00 sec)
```

MySQLから抜ける：

```sql
EXIT;
```

### 6-3. Spring Bootアプリケーションの起動

```bash
./mvnw clean install
./mvnw spring-boot:run
```

**期待される結果**:
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::               (v3.5.8)

...
Started BloghubApplication in 3.456 seconds
```

エラーが表示される場合は、トラブルシューティングを参照してください。

### 6-4. エンティティの検証

起動ログに以下のようなメッセージが表示されれば、エンティティとテーブルの定義が一致しています：

```
Hibernate: validate the schema against the database
```

エラーが出る場合は、エンティティの`@Column`定義とinit.sqlのテーブル定義を見直してください。

---

## 🐛 トラブルシューティング

### エラー: "Access denied for user 'bloghub_user'@'localhost'"

**原因**: データベースのユーザー名またはパスワードが間違っている

**解決策**:
1. `docker-compose.yml`の`MYSQL_USER`と`MYSQL_PASSWORD`を確認
2. `application.yml`の`username`と`password`が一致しているか確認
3. コンテナを再起動: `docker-compose down && docker-compose up -d`

### エラー: "Table 'bloghub.users' doesn't exist"

**原因**: init.sqlが実行されていない

**解決策**:
1. コンテナとボリュームを削除: `docker-compose down -v`
2. init.sqlが`bloghub/`直下にあることを確認
3. コンテナを再起動: `docker-compose up -d`
4. ログを確認: `docker-compose logs mysql | grep init.sql`

### エラー: "Schema-validation: missing table [users]"

**原因**: エンティティのテーブル名とデータベースのテーブル名が一致していない

**解決策**:
1. エンティティの`@Table(name = "users")`を確認
2. MySQLでテーブル名を確認: `SHOW TABLES;`
3. テーブル名は複数形（users, articles, comments, tags）

### エラー: "Port 3306 is already in use"

**原因**: 別のMySQLコンテナがポート3306を使用している

**解決策**:
- `docker-compose.yml`で`ports`を`"3307:3306"`に変更済み
- `application.yml`のURLも`localhost:3307`になっているか確認

### エラー: "Could not autowire. No beans of 'UserRepository' type found"

**原因**: まだRepositoryを作成していない（次のステップで作成します）

**解決策**: Step 35でRepositoryを作成するため、現時点では無視してOK

---

## 🎨 チャレンジ課題

基本が理解できたら、以下にチャレンジしてみましょう：

### チャレンジ 1: createdAtでソートしたデータ取得

MySQLコンソールで、記事を新しい順に取得するクエリを書いてみましょう。

```sql
-- ヒント: ORDER BY句を使用
SELECT title, created_at FROM articles ORDER BY ? DESC;
```

### チャレンジ 2: タグ名で記事を検索

特定のタグ（例: "Spring Boot"）が付いた記事を取得するSQLを書いてみましょう。

```sql
-- ヒント: JOINを使用
SELECT a.title, t.name 
FROM articles a
JOIN article_tags at ON a.id = at.article_id
JOIN tags t ON at.tag_id = t.id
WHERE t.name = ?;
```

### チャレンジ 3: ユーザーごとの記事数を集計

各ユーザーが投稿した記事数を集計するSQLを書いてみましょう。

```sql
-- ヒント: COUNT()とGROUP BYを使用
SELECT u.username, COUNT(a.id) as article_count
FROM users u
LEFT JOIN articles a ON u.id = a.user_id
GROUP BY u.id, u.username;
```

---

## 📚 このステップで学んだこと

- ✅ BlogHubアプリケーションの全体像と要件を理解した
- ✅ ER図を元にデータベース設計（5つのテーブル）を理解した
- ✅ Docker ComposeでMySQL環境を構築した
- ✅ init.sqlでテーブル作成とテストデータ投入を自動化した
- ✅ JPAエンティティクラスで複雑なリレーション（1対多、多対多）を定義した
- ✅ `@ManyToOne`, `@OneToMany`, `@ManyToMany`の使い分けを理解した
- ✅ `cascade`と`orphanRemoval`でデータの整合性を保つ方法を学んだ

---

## 💡 補足: リレーションシップの設計パターン

### 双方向 vs 単方向

**双方向リレーション**（今回の実装）:
```java
// User側
@OneToMany(mappedBy = "user")
private List<Article> articles;

// Article側
@ManyToOne
@JoinColumn(name = "user_id")
private User user;
```

**メリット**: 両側からナビゲーション可能
**デメリット**: `toString()`や`equals()`で無限ループに注意

**単方向リレーション**:
```java
// Article側のみ
@ManyToOne
@JoinColumn(name = "user_id")
private User user;
```

**メリット**: シンプル、循環参照の心配なし
**デメリット**: User側から直接Articleにアクセスできない

### Fetch戦略の選択

| Fetch Type | タイミング | 用途 |
|-----------|----------|------|
| **LAZY** | 必要になった時 | 関連エンティティが大きい場合（デフォルト推奨） |
| **EAGER** | 親エンティティ取得時 | 常に必要な関連データ（N+1問題に注意） |

**推奨**:
- `@ManyToOne`, `@OneToOne`: デフォルトはEAGER → `LAZY`に変更推奨
- `@OneToMany`, `@ManyToMany`: デフォルトはLAZY → そのまま

### N+1問題の回避

```java
// 悪い例: N+1問題が発生
List<Article> articles = articleRepository.findAll();
for (Article article : articles) {
    String username = article.getUser().getUsername(); // 毎回SQLが発行される
}

// 良い例: JOINで1回のSQLで取得
@Query("SELECT a FROM Article a JOIN FETCH a.user")
List<Article> findAllWithUser();
```

Step 36でこのテクニックを詳しく学びます！

---

## ➡️ 次のステップ

[Step 35: 認証・認可機能の実装](STEP_35.md)へ進みましょう！

次は、Spring Security + JWT認証でユーザー管理機能を実装します。ユーザー登録、ログイン、トークン検証など、セキュアなAPIを構築していきます。
