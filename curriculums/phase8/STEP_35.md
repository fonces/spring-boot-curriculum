# Step 35: 記事投稿機能と認可制御

## 🎯 目標
ブログの核となる記事（Post）の投稿・編集・削除機能を実装します。
認証されたユーザーのみが記事を投稿でき、自分の記事のみ編集・削除できる認可制御を実装します。

## 📋 機能要件
- 記事の新規投稿（認証必須）
- 記事一覧の取得（ページネーション付き）
- 記事詳細の取得
- 記事の編集（自分の記事のみ）
- 記事の削除（自分の記事のみ）
- 下書き/公開ステータス管理

## 🗂️ データベース設計

### postsテーブル
```sql
CREATE TABLE posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    slug VARCHAR(250) UNIQUE,
    status VARCHAR(20) DEFAULT 'DRAFT', -- DRAFT, PUBLISHED
    author_id BIGINT NOT NULL,
    view_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    published_at TIMESTAMP NULL,
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_author_id (author_id),
    INDEX idx_status (status),
    INDEX idx_published_at (published_at)
);
```

## 💡 実装のヒント

### 1. プロジェクト構成
以下のクラスを追加してください：

```
src/main/java/com/example/blog/
├── controller/
│   └── PostController.java
├── service/
│   └── PostService.java
├── repository/
│   └── PostMapper.java (MyBatis)
├── entity/
│   ├── Post.java
│   └── PostStatus.java (Enum)
├── dto/
│   ├── PostCreateRequest.java
│   ├── PostUpdateRequest.java
│   ├── PostResponse.java
│   └── PostListResponse.java
└── exception/
    ├── PostNotFoundException.java
    └── UnauthorizedException.java
```

### 2. Enumでステータス管理
記事のステータスをEnumで管理してください。

**例**:
```java
public enum PostStatus {
    DRAFT,      // 下書き
    PUBLISHED   // 公開
}
```

### 3. MyBatisでの記事管理
`PostMapper.xml`で以下のSQLを実装してください：
- 記事の投稿（INSERT）
- 記事一覧取得（SELECT with JOIN）※ユーザー情報も含める
- 記事詳細取得（SELECT with JOIN）
- 記事の更新（UPDATE）
- 記事の削除（DELETE）
- ページネーション対応のクエリ

**例**:
```xml
<!-- PostMapper.xml の例 -->
<mapper namespace="com.example.blog.repository.PostMapper">
    <resultMap id="PostWithAuthor" type="Post">
        <id property="id" column="id"/>
        <result property="title" column="title"/>
        <!-- 他のフィールド -->
        <association property="author" javaType="User">
            <id property="id" column="author_id"/>
            <result property="username" column="username"/>
            <result property="displayName" column="display_name"/>
        </association>
    </resultMap>
    
    <select id="findAllPublished" resultMap="PostWithAuthor">
        SELECT 
            p.*,
            u.username,
            u.display_name
        FROM posts p
        INNER JOIN users u ON p.author_id = u.id
        WHERE p.status = 'PUBLISHED'
        ORDER BY p.published_at DESC
        LIMIT #{limit} OFFSET #{offset}
    </select>
    
    <!-- 他のクエリを実装してください -->
</mapper>
```

### 4. 認可制御の実装
記事の編集・削除時に、現在のユーザーが記事の作成者かをチェックしてください。

**例**:
```java
@Service
public class PostService {
    
    public void updatePost(Long postId, PostUpdateRequest request, String username) {
        Post post = postMapper.findById(postId)
            .orElseThrow(() -> new PostNotFoundException("Post not found"));
        
        // 認可チェック: 作成者本人か確認
        if (!post.getAuthor().getUsername().equals(username)) {
            throw new UnauthorizedException("You can only edit your own posts");
        }
        
        // 更新処理
        // ...
    }
}
```

### 5. REST APIエンドポイント

#### 記事の投稿（認証必須）
```
POST /api/posts
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "title": "Spring Bootの基礎",
  "content": "Spring Bootは...",
  "status": "PUBLISHED"
}

Response (201 Created):
{
  "id": 1,
  "title": "Spring Bootの基礎",
  "slug": "spring-boot-no-kiso",
  "content": "Spring Bootは...",
  "status": "PUBLISHED",
  "author": {
    "id": 1,
    "username": "johndoe",
    "displayName": "John Doe"
  },
  "viewCount": 0,
  "createdAt": "2025-10-29T10:00:00",
  "publishedAt": "2025-10-29T10:00:00"
}
```

#### 記事一覧の取得（ページネーション付き）
```
GET /api/posts?page=0&size=10&sort=publishedAt,desc

Response (200 OK):
{
  "content": [
    {
      "id": 1,
      "title": "Spring Bootの基礎",
      "slug": "spring-boot-no-kiso",
      "excerpt": "Spring Bootは...",
      "author": {
        "username": "johndoe",
        "displayName": "John Doe"
      },
      "viewCount": 42,
      "publishedAt": "2025-10-29T10:00:00"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 50,
  "totalPages": 5
}
```

#### 記事詳細の取得
```
GET /api/posts/{id}

または

GET /api/posts/slug/{slug}

Response (200 OK):
{
  "id": 1,
  "title": "Spring Bootの基礎",
  "slug": "spring-boot-no-kiso",
  "content": "Spring Bootは...",
  "status": "PUBLISHED",
  "author": {
    "id": 1,
    "username": "johndoe",
    "displayName": "John Doe"
  },
  "viewCount": 43,
  "createdAt": "2025-10-29T10:00:00",
  "updatedAt": "2025-10-29T10:00:00",
  "publishedAt": "2025-10-29T10:00:00"
}
```

#### 記事の更新（認証必須・作成者のみ）
```
PUT /api/posts/{id}
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "title": "Spring Bootの基礎（改訂版）",
  "content": "Spring Bootは...",
  "status": "PUBLISHED"
}

Response (200 OK):
{
  "id": 1,
  "title": "Spring Bootの基礎（改訂版）",
  ...
}
```

#### 記事の削除（認証必須・作成者のみ）
```
DELETE /api/posts/{id}
Authorization: Bearer YOUR_JWT_TOKEN

Response (204 No Content)
```

#### 自分の記事一覧取得（認証必須）
```
GET /api/posts/my-posts?page=0&size=10
Authorization: Bearer YOUR_JWT_TOKEN

Response (200 OK):
{
  "content": [
    {
      "id": 1,
      "title": "Spring Bootの基礎",
      "status": "PUBLISHED",
      "viewCount": 42,
      "createdAt": "2025-10-29T10:00:00"
    },
    {
      "id": 2,
      "title": "MyBatisの使い方",
      "status": "DRAFT",
      "viewCount": 0,
      "createdAt": "2025-10-28T15:30:00"
    }
  ],
  ...
}
```

### 6. Slugの自動生成
記事のURLフレンドリーなslugを自動生成してください。

**考えるポイント**:
- タイトルから自動生成する方法
- 日本語タイトルの場合の処理（ローマ字化 or ID使用）
- 重複を防ぐ方法（連番追加など）

**例**:
```java
private String generateSlug(String title) {
    // タイトルからslugを生成するロジックを実装
    // 例: "Spring Bootの基礎" -> "spring-boot-no-kiso"
    // または "Spring Bootの基礎" -> "1-spring-boot-post"
}
```

### 7. 閲覧数のカウント
記事詳細取得時に閲覧数をインクリメントしてください。

**例**:
```java
@Transactional
public PostResponse getPostById(Long id) {
    Post post = postMapper.findById(id)
        .orElseThrow(() -> new PostNotFoundException("Post not found"));
    
    // 閲覧数をインクリメント
    postMapper.incrementViewCount(id);
    
    return toPostResponse(post);
}
```

### 8. バリデーション
リクエストDTOに適切なバリデーションを追加してください：
- タイトル: 1〜200文字、必須
- 内容: 1文字以上、必須
- ステータス: DRAFT または PUBLISHED

## ✅ 動作確認

### 1. 記事の投稿
```bash
TOKEN="YOUR_JWT_TOKEN_HERE"

curl -X POST http://localhost:8080/api/posts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "初めてのブログ投稿",
    "content": "これは私の最初のブログ記事です。",
    "status": "PUBLISHED"
  }'
```

### 2. 記事一覧の取得
```bash
curl -X GET "http://localhost:8080/api/posts?page=0&size=10"
```

### 3. 記事詳細の取得
```bash
curl -X GET http://localhost:8080/api/posts/1
```

### 4. 記事の編集
```bash
curl -X PUT http://localhost:8080/api/posts/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "初めてのブログ投稿（更新版）",
    "content": "内容を更新しました。",
    "status": "PUBLISHED"
  }'
```

### 5. 他人の記事を編集しようとする（エラーになるべき）
```bash
# 別のユーザーでログインしてトークンを取得
OTHER_TOKEN="ANOTHER_USER_JWT_TOKEN"

curl -X PUT http://localhost:8080/api/posts/1 \
  -H "Authorization: Bearer $OTHER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "勝手に編集",
    "content": "これはエラーになるべき",
    "status": "PUBLISHED"
  }'

# Expected: 403 Forbidden
```

## 🎓 学習ポイント

1. **MyBatisでのJOIN**: ユーザー情報と記事情報の結合
2. **認可制御**: リソースの所有者のみが操作可能にする
3. **ページネーション**: 大量データの効率的な取得
4. **トランザクション**: 閲覧数カウントなどの複合操作
5. **Enum**: ステータス管理の型安全性
6. **Slug**: SEOフレンドリーなURL設計

## 📝 追加課題（オプション）

1. 記事の検索機能（タイトル・内容で全文検索）
2. 公開予約機能（未来の日時を指定して公開）
3. 記事のアーカイブ機能（削除せずに非表示）
4. 人気記事ランキング（閲覧数でソート）
5. ユーザーごとの記事数カウント

## 🚀 次のステップ
Step 36では、コメント機能を実装し、記事とコメントの1対多リレーションシップを扱います。
