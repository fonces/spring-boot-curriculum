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

> **📌 このステップの実装方針**
> 
> このステップでは、**あなた自身が設計・実装**することで、実践力を身につけます。
> 各セクションには「機能要件」「実装ヒント」を記載していますので、それを参考に自分で考えながらコードを書いてください。
> 
> 困ったときは、Step 17（例外ハンドリング）、Step 19（DTOとEntityの分離）、Step 31（ページネーション）などの過去のステップを振り返りましょう。

---

### 手順1: 例外クラスの作成 【自分で実装】

エラーハンドリングのための例外クラスを作成します。Step 17で学んだ内容を活用しましょう。

#### 1.1 ResourceNotFoundException

**ファイルパス**: `src/main/java/com/example/bloghub/exception/ResourceNotFoundException.java`

**機能要件**:
- `RuntimeException`を継承したカスタム例外クラス
- リソースが見つからない場合にスローする
- 複数のコンストラクタで柔軟なメッセージ生成

**必要なコンストラクタ**:

| コンストラクタ | 用途 |
|---------------|------|
| `ResourceNotFoundException(String message)` | カスタムメッセージ |
| `ResourceNotFoundException(String resourceName, Long id)` | 「{resourceName} not found with id: {id}」形式 |
| `ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue)` | 「{resourceName} not found with {fieldName}: {fieldValue}」形式 |

**実装のポイント**:
- `String.format()`を使ってメッセージを組み立てる
- `super(message)`で親クラスにメッセージを渡す

---

#### 1.2 UnauthorizedException

**ファイルパス**: `src/main/java/com/example/bloghub/exception/UnauthorizedException.java`

**機能要件**:
- `RuntimeException`を継承
- 権限がない操作を試みた場合にスロー
- デフォルトメッセージ: 「You are not authorized to perform this action」

**必要なコンストラクタ**:

| コンストラクタ | 用途 |
|---------------|------|
| `UnauthorizedException(String message)` | カスタムメッセージ |
| `UnauthorizedException()` | デフォルトメッセージ |

---

#### 1.3 ErrorResponse

**ファイルパス**: `src/main/java/com/example/bloghub/dto/ErrorResponse.java`

**機能要件**:
- APIエラーレスポンスの統一フォーマット
- Lombokを使ってシンプルに

**必要なフィールド**:

| フィールド | 型 | 説明 |
|-----------|-----|------|
| `status` | `int` | HTTPステータスコード |
| `message` | `String` | エラーメッセージ |
| `timestamp` | `LocalDateTime` | 発生日時 |
| `path` | `String` | リクエストパス |

**実装のポイント**:
- `@Data`と`@AllArgsConstructor`を使用
- `java.time.LocalDateTime`をインポート

---

#### 1.4 GlobalExceptionHandler

**ファイルパス**: `src/main/java/com/example/bloghub/exception/GlobalExceptionHandler.java`

**機能要件**:
- `@RestControllerAdvice`で全コントローラーの例外を一元管理
- 例外の種類に応じて適切なHTTPステータスコードを返す
- `WebRequest`からリクエストパスを取得

**ハンドリングする例外**:

| 例外 | HTTPステータス | 説明 |
|------|---------------|------|
| `ResourceNotFoundException` | 404 Not Found | リソースが存在しない |
| `UnauthorizedException` | 403 Forbidden | 権限がない |
| `MethodArgumentNotValidException` | 400 Bad Request | バリデーションエラー |
| `Exception` | 500 Internal Server Error | その他のエラー |

**実装のポイント**:
- `@ExceptionHandler`でハンドリング対象を指定
- `request.getDescription(false).replace("uri=", "")`でパスを取得
- バリデーションエラーは`errors`フィールドにフィールド名とエラーメッセージのMapを返す

**バリデーションエラーのレスポンス例**:
```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "title": "Title is required",
    "content": "Content is required"
  },
  "timestamp": "2025-12-13T10:00:00",
  "path": "/api/articles"
}
```

---

### 手順2: Repository層の実装 【自分で実装】

#### 2.1 ArticleRepository

**ファイルパス**: `src/main/java/com/example/bloghub/repository/ArticleRepository.java`

**機能要件**:
- `JpaRepository<Article, Long>`を継承
- ページネーション対応の記事一覧取得
- タグによる記事検索
- N+1問題を回避した記事詳細取得

**必要なメソッド**:

| メソッド | 戻り値 | 説明 |
|---------|--------|------|
| `findByUserIdOrderByCreatedAtDesc` | `Page<Article>` | ユーザーの記事一覧（新しい順） |
| `findByTagName` | `Page<Article>` | タグ名で記事検索（@Queryで実装） |
| `findByIdWithUser` | `Optional<Article>` | JOIN FETCHでユーザー・タグも取得 |
| `findAllByOrderByCreatedAtDesc` | `Page<Article>` | 全記事を新しい順で取得 |

**実装のポイント**:
- `@Query`でJPQLを記述
- `JOIN FETCH`でN+1問題を回避
- `Page<Article>`と`Pageable`でページネーション
- `DISTINCT`で重複を防ぐ（タグ検索時）
- `LEFT JOIN FETCH`でタグがない記事も取得

**JPQLサンプル（タグ検索）**:
```java
@Query("SELECT DISTINCT a FROM Article a JOIN a.tags t WHERE t.name = :tagName ORDER BY a.createdAt DESC")
Page<Article> findByTagName(@Param("tagName") String tagName, Pageable pageable);
```

---

#### 2.2 CommentRepository

**ファイルパス**: `src/main/java/com/example/bloghub/repository/CommentRepository.java`

**機能要件**:
- `JpaRepository<Comment, Long>`を継承
- 記事に紐づくコメント一覧を取得
- コメント数をカウント

**必要なメソッド**:

| メソッド | 戻り値 | 説明 |
|---------|--------|------|
| `findByArticleIdOrderByCreatedAtAsc` | `List<Comment>` | 記事のコメント一覧（古い順） |
| `countByArticleId` | `long` | 記事のコメント数 |

**実装のポイント**:
- メソッド名規則でSpring Dataが自動実装
- コメントは会話の流れを保つため古い順（Asc）でソート

---

#### 2.3 TagRepository

**ファイルパス**: `src/main/java/com/example/bloghub/repository/TagRepository.java`

**機能要件**:
- `JpaRepository<Tag, Long>`を継承
- タグ名での検索・存在チェック

**必要なメソッド**:

| メソッド | 戻り値 | 説明 |
|---------|--------|------|
| `findByName` | `Optional<Tag>` | タグ名で検索 |
| `existsByName` | `boolean` | タグ名の存在チェック |

---

### 手順3: DTO層の実装 【自分で実装】

#### 3.1 記事関連DTO

**ArticleCreateRequest**

**ファイルパス**: `src/main/java/com/example/bloghub/dto/article/ArticleCreateRequest.java`

| フィールド | 型 | バリデーション |
|-----------|-----|---------------|
| `title` | `String` | `@NotBlank`, `@Size(min=1, max=200)` |
| `content` | `String` | `@NotBlank`, `@Size(min=1, max=10000)` |
| `tags` | `Set<String>` | 任意（デフォルトは空のHashSet） |

---

**ArticleUpdateRequest**

**ファイルパス**: `src/main/java/com/example/bloghub/dto/article/ArticleUpdateRequest.java`

| フィールド | 型 | バリデーション |
|-----------|-----|---------------|
| `title` | `String` | `@Size(min=1, max=200)`（任意） |
| `content` | `String` | `@Size(min=1, max=10000)`（任意） |
| `tags` | `Set<String>` | 任意 |

**ポイント**: 更新時はnullのフィールドは更新しない（部分更新対応）

---

**ArticleResponse**

**ファイルパス**: `src/main/java/com/example/bloghub/dto/article/ArticleResponse.java`

| フィールド | 型 | 説明 |
|-----------|-----|------|
| `id` | `Long` | 記事ID |
| `title` | `String` | タイトル |
| `content` | `String` | 本文 |
| `tags` | `Set<String>` | タグ名のセット |
| `user` | `UserResponse` | 投稿者情報 |
| `createdAt` | `LocalDateTime` | 作成日時 |
| `updatedAt` | `LocalDateTime` | 更新日時 |
| `commentCount` | `Long` | コメント数 |

---

**ArticleSummaryResponse**

**ファイルパス**: `src/main/java/com/example/bloghub/dto/article/ArticleSummaryResponse.java`

| フィールド | 型 | 説明 |
|-----------|-----|------|
| `id` | `Long` | 記事ID |
| `title` | `String` | タイトル |
| `username` | `String` | 投稿者名 |
| `tags` | `Set<String>` | タグ名のセット |
| `createdAt` | `LocalDateTime` | 作成日時 |
| `commentCount` | `Long` | コメント数 |

**ポイント**: 一覧表示用のため、`content`は含めない（軽量化）

---

#### 3.2 コメント関連DTO

**CommentCreateRequest**

**ファイルパス**: `src/main/java/com/example/bloghub/dto/comment/CommentCreateRequest.java`

| フィールド | 型 | バリデーション |
|-----------|-----|---------------|
| `content` | `String` | `@NotBlank`, `@Size(min=1, max=1000)` |

---

**CommentResponse**

**ファイルパス**: `src/main/java/com/example/bloghub/dto/comment/CommentResponse.java`

| フィールド | 型 | 説明 |
|-----------|-----|------|
| `id` | `Long` | コメントID |
| `content` | `String` | 本文 |
| `user` | `UserResponse` | 投稿者情報 |
| `createdAt` | `LocalDateTime` | 作成日時 |

---

### 手順4: Service層の実装 【自分で実装】

#### 4.1 TagService

**ファイルパス**: `src/main/java/com/example/bloghub/service/TagService.java`

**機能要件**:
- タグの「findOrCreate」パターン実装
- 既存タグがあれば取得、なければ新規作成

**必要なメソッド**:

| メソッド | 処理 |
|---------|------|
| `findOrCreateTag(String name)` | タグを検索、存在しなければ新規作成して返す |
| `getAllTags()` | 全タグを取得 |

**実装のポイント**:
- `@Transactional`で書き込みメソッドを保護
- `@Transactional(readOnly = true)`をクラスレベルに、書き込みメソッドには`@Transactional`を付与
- `orElseGet()`でLazy評価（存在する場合は新規作成しない）

---

#### 4.2 ArticleService

**ファイルパス**: `src/main/java/com/example/bloghub/service/ArticleService.java`

**機能要件**:
- 記事のCRUD操作
- 所有者チェックによる権限制御
- タグの自動作成・関連付け
- Entity→DTOの変換

**依存関係**:
- `ArticleRepository`
- `UserRepository`
- `CommentRepository`
- `TagService`

**メソッド別処理フロー**:

| メソッド | 処理フロー |
|---------|-----------|
| `createArticle(request, username)` | ユーザー取得 → Article作成 → タグ処理 → 保存 → レスポンス変換 |
| `updateArticle(id, request, username)` | 記事取得 → **所有者チェック** → フィールド更新 → 保存 |
| `deleteArticle(id, username)` | 記事取得 → **所有者チェック** → 削除 |
| `getArticleById(id)` | JOIN FETCHで取得 → レスポンス変換 |
| `getAllArticles(pageable)` | ページネーション取得 → Summary変換 |
| `getArticlesByTag(tagName, pageable)` | タグ検索 → Summary変換 |
| `getMyArticles(username, pageable)` | ユーザーID取得 → 記事検索 → Summary変換 |

**所有者チェックの実装**:
```java
// 記事の所有者と認証ユーザーを比較
if (!article.getUser().getUsername().equals(username)) {
    throw new UnauthorizedException("You can only update your own articles");
}
```

**タグ処理ヘルパーメソッド**:
```java
private Set<Tag> processTags(Set<String> tagNames) {
    if (tagNames == null || tagNames.isEmpty()) {
        return new HashSet<>();
    }
    return tagNames.stream()
            .filter(name -> name != null && !name.trim().isEmpty())
            .map(tagService::findOrCreateTag)
            .collect(Collectors.toSet());
}
```

**Entity→DTO変換のポイント**:
- `ArticleResponse`には全情報を含める
- `ArticleSummaryResponse`には一覧表示に必要な情報のみ
- コメント数は`commentRepository.countByArticleId()`で取得
- タグ名は`article.getTags().stream().map(Tag::getName).collect(Collectors.toSet())`

---

#### 4.3 CommentService

**ファイルパス**: `src/main/java/com/example/bloghub/service/CommentService.java`

**機能要件**:
- コメントのCRUD操作
- 所有者チェックによる削除権限制御
- 記事の存在確認

**依存関係**:
- `CommentRepository`
- `ArticleRepository`
- `UserRepository`

**メソッド別処理フロー**:

| メソッド | 処理フロー |
|---------|-----------|
| `createComment(articleId, request, username)` | 記事存在確認 → ユーザー取得 → Comment作成 → 保存 |
| `deleteComment(commentId, username)` | コメント取得 → **所有者チェック** → 削除 |
| `getCommentsByArticleId(articleId)` | 記事存在確認 → コメント一覧取得 → 変換 |

**実装のポイント**:
- コメント作成時は`article`と`user`の両方を設定
- コメント一覧は古い順（会話の流れを保持）
- 記事が存在しない場合は`ResourceNotFoundException`

---

### 手順5: Controller層の実装 【自分で実装】

#### 5.1 ArticleController

**ファイルパス**: `src/main/java/com/example/bloghub/controller/ArticleController.java`

**ベースパス**: `/api/articles`

**エンドポイント一覧**:

| メソッド | パス | 認証 | 説明 |
|---------|------|------|------|
| `POST` | `/api/articles` | 必要 | 記事作成 |
| `GET` | `/api/articles` | 不要 | 記事一覧（ページネーション、タグフィルター対応） |
| `GET` | `/api/articles/{id}` | 不要 | 記事詳細 |
| `PUT` | `/api/articles/{id}` | 必要 | 記事更新 |
| `DELETE` | `/api/articles/{id}` | 必要 | 記事削除 |
| `GET` | `/api/articles/my` | 必要 | 自分の記事一覧 |

**実装のポイント**:
- `Authentication authentication`から`authentication.getName()`でユーザー名取得
- `@Valid`でリクエストボディをバリデーション
- `@RequestParam`でクエリパラメータを受け取る（`page`, `size`, `tag`）
- 記事一覧はタグパラメータがあればタグ検索、なければ全件取得
- 作成時は`HttpStatus.CREATED`、削除時は`HttpStatus.NO_CONTENT`

**クエリパラメータ**:

| パラメータ | デフォルト | 説明 |
|-----------|-----------|------|
| `page` | 0 | ページ番号（0始まり） |
| `size` | 10 | 1ページあたりの件数 |
| `tag` | null | タグでフィルタリング |

---

#### 5.2 CommentController

**ファイルパス**: `src/main/java/com/example/bloghub/controller/CommentController.java`

**エンドポイント一覧**:

| メソッド | パス | 認証 | 説明 |
|---------|------|------|------|
| `POST` | `/api/articles/{articleId}/comments` | 必要 | コメント作成 |
| `GET` | `/api/articles/{articleId}/comments` | 不要 | コメント一覧 |
| `DELETE` | `/api/comments/{id}` | 必要 | コメント削除 |

**実装のポイント**:
- ネストされたRESTfulルート: `/api/articles/{articleId}/comments`
- コメント削除は単独リソースとして: `/api/comments/{id}`
- `@PathVariable`で`articleId`を取得

---

#### 5.3 TagController

**ファイルパス**: `src/main/java/com/example/bloghub/controller/TagController.java`

**ベースパス**: `/api/tags`

**エンドポイント一覧**:

| メソッド | パス | 認証 | 説明 |
|---------|------|------|------|
| `GET` | `/api/tags` | 不要 | 全タグ一覧（名前のリスト） |

**実装のポイント**:
- タグエンティティを`List<String>`（タグ名のリスト）に変換して返す
- `Tag::getName`でタグ名を抽出

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
