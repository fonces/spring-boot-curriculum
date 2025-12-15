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

```sh
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

```sh
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
      - ./my.cnf:/etc/mysql/conf.d/my.cnf
    command:
      - --default-authentication-plugin=mysql_native_password
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
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

### 2-3. MySQL設定ファイル（my.cnf）の作成

**日本語の文字化けを防ぐため**、MySQL設定ファイルを作成します。

**ファイルパス**: `bloghub/my.cnf`

```ini
[client]
default-character-set=utf8mb4

[mysql]
default-character-set=utf8mb4

[mysqld]
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
init-connect='SET NAMES utf8mb4'
skip-character-set-client-handshake
```

#### 設定の解説

- **`[client]`セクション**: MySQLクライアントのデフォルト文字セット
- **`[mysql]`セクション**: mysqlコマンドラインツールの文字セット
- **`[mysqld]`セクション**: MySQLサーバーの文字セット設定
  - `skip-character-set-client-handshake`: クライアントの文字セット自動判別を無効化し、常にutf8mb4を使用

これにより、**データベースへの接続時に文字化けが発生しない**ようになります。

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
    url: jdbc:mysql://localhost:3307/bloghub?useSSL=false&serverTimezone=Asia/Tokyo&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&useUnicode=true
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
  servlet:
    encoding:
      charset: UTF-8
      enabled: true
      force: true
```

### 4-2. 設定の解説

#### `ddl-auto: validate`
- **validate**: エンティティとテーブル定義が一致するか検証のみ
- **create**: 毎回テーブルを作り直す（開発初期のみ）
- **update**: スキーマを自動更新（本番では非推奨）
- **none**: Hibernateによるスキーマ管理を無効化

今回は`init.sql`でテーブルを作成するため、`validate`を使用します。

#### `characterEncoding=UTF-8&useUnicode=true`
- **characterEncoding=UTF-8**: JDBC接続時の文字エンコーディングをUTF-8に指定
- **useUnicode=true**: Unicode文字の使用を有効化
- これらのパラメータにより、日本語などのマルチバイト文字が正しく扱われます

#### `server.servlet.encoding`
- **charset: UTF-8**: HTTPリクエスト/レスポンスの文字エンコーディング
- **enabled: true**: エンコーディングフィルタを有効化
- **force: true**: すべてのリクエスト/レスポンスに強制適用
- これにより、Webページで日本語が正しく表示されます

#### `multipart`
- ファイルアップロード機能で使用
- 最大10MBまでのファイルを受け付ける

---

## 🚀 ステップ5: エンティティクラスの実装 【自分で実装】

ここからは、これまで学んだ知識を活かして**自分でエンティティクラスを実装**してください。

### 📁 パッケージ構成

以下のパッケージ構成でエンティティクラスを作成します：

```sh
com.example.bloghub/
├── BloghubApplication.java
└── entity/
    ├── User.java
    ├── Article.java
    ├── Comment.java
    └── Tag.java
```

---

### 📋 実装要件

### 5-1. Userエンティティ

**ファイルパス**: `src/main/java/com/example/bloghub/entity/User.java`

**機能要件**:
- テーブル名: `users`
- ユーザーの基本情報（ID、ユーザー名、メール、パスワード、プロフィール画像）を管理
- 作成日時・更新日時を自動設定
- リレーション: 1対多でArticleとCommentを持つ

**カラム制約**:

| フィールド | 型 | 制約 |
|-----------|-----|------|
| id | Long | 主キー、自動採番 |
| username | String | nullable=false, unique=true, length=50 |
| email | String | nullable=false, unique=true, length=100 |
| password | String | nullable=false |
| profileImage | String | カラム名は`profile_image`（nullable） |
| createdAt | LocalDateTime | 自動設定、更新不可 |
| updatedAt | LocalDateTime | 自動更新 |
| articles | List\<Article\> | 1対多（mappedBy="user"）、cascade=ALL, orphanRemoval=true |
| comments | List\<Comment\> | 1対多（mappedBy="user"）、cascade=ALL, orphanRemoval=true |

**実装ヒント**:
- `@Entity`, `@Table(name = "users")` を使用
- `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` でID自動生成
- `@Column` で制約を設定
- `@CreationTimestamp`, `@UpdateTimestamp` で日時を自動設定
- `@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)` でリレーション定義
- Lombokの `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder` を使用
- コレクションフィールドには `@Builder.Default` を付けて `new ArrayList<>()` で初期化

---

### 5-2. Articleエンティティ

**ファイルパス**: `src/main/java/com/example/bloghub/entity/Article.java`

**機能要件**:
- テーブル名: `articles`
- 記事の情報（ID、タイトル、本文）を管理
- 作成日時・更新日時を自動設定
- リレーション:
  - 多対1でUserに属する
  - 1対多でCommentを持つ
  - 多対多でTagを持つ（中間テーブル: `article_tags`）

**カラム制約**:

| フィールド | 型 | 制約 |
|-----------|-----|------|
| id | Long | 主キー、自動採番 |
| title | String | nullable=false, length=200 |
| content | String | nullable=false, columnDefinition="TEXT" |
| user | User | 多対1、外部キー`user_id`、nullable=false、LAZY |
| createdAt | LocalDateTime | 自動設定、更新不可 |
| updatedAt | LocalDateTime | 自動更新 |
| comments | List\<Comment\> | 1対多（mappedBy="article"）、cascade=ALL, orphanRemoval=true |
| tags | Set\<Tag\> | 多対多、中間テーブル`article_tags`、LAZY |

**実装ヒント**:
- `@ManyToOne(fetch = FetchType.LAZY)` と `@JoinColumn(name = "user_id", nullable = false)` でUser関連
- `@ManyToMany` と `@JoinTable` で多対多リレーション定義
- タグのコレクションは `Set<Tag>` を使用（重複防止）

**⚠️ 重要: Hibernateとの互換性**

Articleエンティティでは、`@Data` の代わりに `@Getter` + `@Setter` を使用し、`tags`フィールドにはカスタムsetterを実装してください：

```java
// カスタムsetterでHibernateのコレクション置換を防ぐ
public void setTags(Set<Tag> tags) {
    if (this.tags == null) {
        this.tags = new HashSet<>();
    }
    this.tags.clear();
    if (tags != null) {
        this.tags.addAll(tags);
    }
}
```

**理由**: `@Data`が生成するsetterは単純な代入（`this.tags = tags`）を行うため、Hibernateの遅延ロード用コレクション（`PersistentSet`）が破壊され、`ConcurrentModificationException`が発生することがあります。

---

### 5-3. Commentエンティティ

**ファイルパス**: `src/main/java/com/example/bloghub/entity/Comment.java`

**機能要件**:
- テーブル名: `comments`
- コメントの情報（ID、内容）を管理
- 作成日時を自動設定（更新日時は不要）
- リレーション:
  - 多対1でArticleに属する
  - 多対1でUserに属する

**カラム制約**:

| フィールド | 型 | 制約 |
|-----------|-----|------|
| id | Long | 主キー、自動採番 |
| content | String | nullable=false, columnDefinition="TEXT" |
| article | Article | 多対1、外部キー`article_id`、nullable=false、LAZY |
| user | User | 多対1、外部キー`user_id`、nullable=false、LAZY |
| createdAt | LocalDateTime | 自動設定、更新不可 |

**実装ヒント**:
- Userと同様に`@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`を使用
- `@ManyToOne(fetch = FetchType.LAZY)` で遅延ロード

---

### 5-4. Tagエンティティ

**ファイルパス**: `src/main/java/com/example/bloghub/entity/Tag.java`

**機能要件**:
- テーブル名: `tags`
- タグの情報（ID、名前）を管理
- 作成日時を自動設定
- リレーション: 多対多でArticleを持つ（逆側）

**カラム制約**:

| フィールド | 型 | 制約 |
|-----------|-----|------|
| id | Long | 主キー、自動採番 |
| name | String | nullable=false, unique=true, length=50 |
| createdAt | LocalDateTime | 自動設定、更新不可 |
| articles | Set\<Article\> | 多対多（mappedBy="tags"）|

**実装ヒント**:
- `@ManyToMany(mappedBy = "tags")` でリレーションの逆側を定義
- `Set<Article>`を使用

---

### 📖 リファレンス: JPAアノテーション早見表

実装時に参考にしてください：

| アノテーション | 用途 |
|--------------|------|
| `@Entity` | エンティティクラスとしてマーク |
| `@Table(name = "xxx")` | マッピングするテーブル名を指定 |
| `@Id` | 主キーフィールドとしてマーク |
| `@GeneratedValue(strategy = GenerationType.IDENTITY)` | 自動採番 |
| `@Column(nullable = false, unique = true, length = 50)` | カラム制約 |
| `@Column(columnDefinition = "TEXT")` | TEXT型を指定 |
| `@CreationTimestamp` | 作成日時を自動設定（Hibernate拡張） |
| `@UpdateTimestamp` | 更新日時を自動設定（Hibernate拡張） |
| `@ManyToOne(fetch = FetchType.LAZY)` | 多対1リレーション |
| `@OneToMany(mappedBy = "xxx", cascade = CascadeType.ALL, orphanRemoval = true)` | 1対多リレーション |
| `@ManyToMany` | 多対多リレーション |
| `@JoinColumn(name = "xxx_id")` | 外部キーカラム名を指定 |
| `@JoinTable(name = "xxx", joinColumns = ..., inverseJoinColumns = ...)` | 中間テーブルを指定 |

### 📖 リファレンス: Lombokアノテーション早見表

| アノテーション | 用途 |
|--------------|------|
| `@Data` | getter, setter, toString, equals, hashCode を自動生成 |
| `@Getter` | getterのみ自動生成 |
| `@Setter` | setterのみ自動生成 |
| `@NoArgsConstructor` | 引数なしコンストラクタを生成 |
| `@AllArgsConstructor` | 全フィールドのコンストラクタを生成 |
| `@Builder` | Builderパターンを生成 |
| `@Builder.Default` | Builderパターン使用時のデフォルト値を指定 |

---

### ✅ 実装チェックリスト

すべてのエンティティを実装したら、以下をチェックしてください：

- [ ] **User.java**: 全フィールド、リレーション（articles, comments）を定義
- [ ] **Article.java**: 全フィールド、3つのリレーション（user, comments, tags）を定義
- [ ] **Article.java**: `@Getter`+`@Setter`を使用し、`setTags`カスタムsetterを実装
- [ ] **Comment.java**: 全フィールド、2つのリレーション（article, user）を定義
- [ ] **Tag.java**: 全フィールド、リレーション（articles）を定義
- [ ] すべてのエンティティにLombokアノテーション（`@Data`または`@Getter/@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`）を付与
- [ ] コレクションフィールドに`@Builder.Default`を付与し、初期化

---

## ✅ ステップ6: 動作確認

### 6-1. MySQLコンテナの起動

```bash
cd ~/git/spring-boot-curriculum/workspace/bloghub
docker-compose up -d
```

**期待される結果**:
```sh
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
```sh
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
```sh
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
```sh
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

```sh
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

### エラー: 日本語が文字化けする（"ã" などの文字が表示される）

**原因**: MySQL、JDBC、またはTomcatの文字エンコーディング設定が不足している

**解決策**:
1. **my.cnf**が正しく作成され、docker-compose.ymlでマウントされているか確認
2. **application.yml**のdatasource URLに`characterEncoding=UTF-8&useUnicode=true`があるか確認
3. **server.servlet.encoding**が設定されているか確認
4. **既にデータが入っている場合**: コンテナとボリュームを削除して再作成
   ```bash
   cd ~/workspace/bloghub
   docker-compose down -v
   docker-compose up -d
   ```
5. **文字セット確認**: MySQLコンソールで以下を実行
   ```sql
   SHOW VARIABLES LIKE 'char%';
   ```
   すべての項目が`utf8mb4`になっているべきです

### ⚠️ 重要: Hibernate使用時のコレクション操作の注意

**問題**: `@ManyToMany`や`@OneToMany`のコレクションで`ConcurrentModificationException`が発生

**原因**: Lombokの`@Data`によって生成されるsetterがHibernateのコレクション管理と競合

**解決策**: Articleエンティティで`@Data`を使わず、`@Getter`+`@Setter`を使用し、カスタムsetterを実装:

```java
@Entity
@Table(name = "articles")
@Getter
@Setter  // @Dataの代わり
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Article {
    // フィールド定義...
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "article_tags", ...)
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();
    
    // カスタムsetter
    public void setTags(Set<Tag> tags) {
        if (this.tags == null) {
            this.tags = new HashSet<>();
        }
        this.tags.clear();
        if (tags != null) {
            this.tags.addAll(tags);
        }
    }
}
```

**なぜ必要か**:
- Hibernateは遅延ロード用に特別なコレクション実装（`PersistentSet`など）を使用
- Lombokの`@Data`が生成するsetterは単純な代入（`this.tags = tags`）を行う
- これによりHibernateのコレクション管理が破壊され、例外が発生

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
