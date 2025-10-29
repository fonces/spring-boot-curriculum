# Step 36: コメント機能とThymeleafでの画面実装

## 🎯 目標
記事に対するコメント機能を実装し、Thymeleafを使ってブログの画面を作成します。
これにより、REST APIとサーバーサイドレンダリングの両方を体験します。

## 📋 機能要件

### バックエンド（REST API）
- 記事へのコメント投稿（認証必須）
- コメント一覧の取得
- コメントの編集・削除（自分のコメントのみ）
- コメント数のカウント

### フロントエンド（Thymeleaf）
- ブログトップページ（記事一覧）
- 記事詳細ページ（コメント表示・投稿フォーム付き）
- ユーザープロフィールページ
- 記事投稿・編集フォーム

## 🗂️ データベース設計

### commentsテーブル
```sql
CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_post_id (post_id),
    INDEX idx_user_id (user_id)
);
```

## 💡 実装のヒント（バックエンド）

### 1. プロジェクト構成
以下のクラスを追加してください：

```
src/main/java/com/example/blog/
├── controller/
│   ├── CommentController.java (REST API)
│   └── WebController.java (Thymeleaf用)
├── service/
│   └── CommentService.java
├── repository/
│   └── CommentMapper.java (MyBatis)
├── entity/
│   └── Comment.java
└── dto/
    ├── CommentRequest.java
    ├── CommentResponse.java
    └── CommentWithUserResponse.java
```

### 2. MyBatisでのコメント管理
`CommentMapper.xml`で以下のSQLを実装してください：

**例**:
```xml
<!-- CommentMapper.xml の例 -->
<mapper namespace="com.example.blog.repository.CommentMapper">
    <resultMap id="CommentWithUser" type="Comment">
        <id property="id" column="id"/>
        <result property="content" column="content"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
        <association property="user" javaType="User">
            <id property="id" column="user_id"/>
            <result property="username" column="username"/>
            <result property="displayName" column="display_name"/>
        </association>
    </resultMap>
    
    <select id="findByPostId" resultMap="CommentWithUser">
        SELECT 
            c.*,
            u.username,
            u.display_name
        FROM comments c
        INNER JOIN users u ON c.user_id = u.id
        WHERE c.post_id = #{postId}
        ORDER BY c.created_at ASC
    </select>
    
    <select id="countByPostId" resultType="int">
        <!-- SQLを実装してください -->
    </select>
    
    <!-- 他のクエリを実装してください -->
</mapper>
```

### 3. REST APIエンドポイント

#### コメントの投稿（認証必須）
```
POST /api/posts/{postId}/comments
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "content": "とても参考になりました！"
}

Response (201 Created):
{
  "id": 1,
  "content": "とても参考になりました！",
  "user": {
    "id": 1,
    "username": "johndoe",
    "displayName": "John Doe"
  },
  "createdAt": "2025-10-29T11:00:00"
}
```

#### コメント一覧の取得
```
GET /api/posts/{postId}/comments

Response (200 OK):
{
  "comments": [
    {
      "id": 1,
      "content": "とても参考になりました！",
      "user": {
        "username": "johndoe",
        "displayName": "John Doe"
      },
      "createdAt": "2025-10-29T11:00:00",
      "isAuthor": false
    }
  ],
  "total": 1
}
```

#### コメントの削除（認証必須・作成者のみ）
```
DELETE /api/comments/{commentId}
Authorization: Bearer YOUR_JWT_TOKEN

Response (204 No Content)
```

## 💡 実装のヒント（フロントエンド）

### 1. Thymeleafテンプレート構成
以下のテンプレートを作成してください：

```
src/main/resources/templates/
├── layout/
│   ├── base.html (共通レイアウト)
│   ├── header.html (ヘッダーフラグメント)
│   └── footer.html (フッターフラグメント)
├── index.html (記事一覧)
├── post/
│   ├── detail.html (記事詳細)
│   ├── create.html (記事作成フォーム)
│   └── edit.html (記事編集フォーム)
├── user/
│   ├── profile.html (プロフィール)
│   └── posts.html (ユーザーの記事一覧)
└── auth/
    ├── login.html (ログインページ)
    └── register.html (登録ページ)
```

### 2. 共通レイアウトの作成
`layout/base.html`を作成し、全ページで共通のヘッダー・フッターを定義してください。

**例**:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title th:text="${pageTitle} + ' | ミニブログ'">ミニブログ</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <!-- ヘッダー -->
    <div th:replace="~{layout/header :: header}"></div>
    
    <!-- メインコンテンツ -->
    <main class="container my-4">
        <div th:replace="${content}"></div>
    </main>
    
    <!-- フッター -->
    <div th:replace="~{layout/footer :: footer}"></div>
    
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
```

### 3. 記事一覧ページ
`index.html`で記事一覧を表示してください。

**例**:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>記事一覧 | ミニブログ</title>
</head>
<body>
    <div th:replace="~{layout/header :: header}"></div>
    
    <div class="container my-4">
        <h1>最新の記事</h1>
        
        <div th:if="${posts.empty}" class="alert alert-info">
            まだ記事がありません。
        </div>
        
        <div th:each="post : ${posts}" class="card mb-3">
            <div class="card-body">
                <h5 class="card-title">
                    <a th:href="@{/posts/{id}(id=${post.id})}" 
                       th:text="${post.title}">記事タイトル</a>
                </h5>
                <p class="card-text text-muted">
                    <small>
                        投稿者: <span th:text="${post.author.displayName}">著者名</span> |
                        投稿日: <span th:text="${#temporals.format(post.publishedAt, 'yyyy/MM/dd HH:mm')}">日時</span> |
                        閲覧数: <span th:text="${post.viewCount}">0</span> |
                        コメント: <span th:text="${post.commentCount}">0</span>
                    </small>
                </p>
                <!-- 記事の抜粋を表示 -->
            </div>
        </div>
        
        <!-- ページネーション -->
        <nav th:if="${totalPages > 1}">
            <ul class="pagination">
                <!-- ページネーションのリンクを実装してください -->
            </ul>
        </nav>
    </div>
    
    <div th:replace="~{layout/footer :: footer}"></div>
</body>
</html>
```

### 4. 記事詳細ページ（コメント機能付き）
`post/detail.html`で記事詳細とコメントを表示してください。

**例**:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title th:text="${post.title} + ' | ミニブログ'">記事詳細</title>
</head>
<body>
    <div th:replace="~{layout/header :: header}"></div>
    
    <div class="container my-4">
        <!-- 記事本体 -->
        <article>
            <h1 th:text="${post.title}">記事タイトル</h1>
            <p class="text-muted">
                <small>
                    投稿者: <a th:href="@{/users/{username}(username=${post.author.username})}"
                              th:text="${post.author.displayName}">著者名</a> |
                    投稿日: <span th:text="${#temporals.format(post.publishedAt, 'yyyy年MM月dd日 HH:mm')}">日時</span>
                </small>
            </p>
            
            <!-- 自分の記事の場合、編集・削除ボタンを表示 -->
            <div th:if="${isAuthor}" class="mb-3">
                <a th:href="@{/posts/{id}/edit(id=${post.id})}" class="btn btn-primary">編集</a>
                <button type="button" class="btn btn-danger" 
                        onclick="deletePost()">削除</button>
            </div>
            
            <div class="content" th:utext="${post.content}">
                記事の内容
            </div>
        </article>
        
        <hr class="my-5">
        
        <!-- コメントセクション -->
        <section id="comments">
            <h3>コメント (<span th:text="${comments.size()}">0</span>)</h3>
            
            <!-- コメント投稿フォーム（ログイン時のみ表示） -->
            <div th:if="${isAuthenticated}" class="mb-4">
                <form id="commentForm">
                    <div class="mb-3">
                        <textarea class="form-control" id="commentContent" 
                                  rows="3" placeholder="コメントを入力..."></textarea>
                    </div>
                    <button type="submit" class="btn btn-primary">コメントを投稿</button>
                </form>
            </div>
            
            <div th:if="${!isAuthenticated}" class="alert alert-info">
                コメントを投稿するには<a th:href="@{/login}">ログイン</a>してください。
            </div>
            
            <!-- コメント一覧 -->
            <div class="comments-list">
                <div th:each="comment : ${comments}" class="card mb-2">
                    <div class="card-body">
                        <p th:text="${comment.content}">コメント内容</p>
                        <small class="text-muted">
                            <strong th:text="${comment.user.displayName}">ユーザー名</strong> -
                            <span th:text="${#temporals.format(comment.createdAt, 'yyyy/MM/dd HH:mm')}">日時</span>
                        </small>
                        <!-- 自分のコメントの場合、削除ボタンを表示 -->
                        <button th:if="${comment.isOwner}" 
                                th:data-comment-id="${comment.id}"
                                class="btn btn-sm btn-danger float-end"
                                onclick="deleteComment(this.dataset.commentId)">削除</button>
                    </div>
                </div>
            </div>
        </section>
    </div>
    
    <div th:replace="~{layout/footer :: footer}"></div>
    
    <!-- JavaScriptでREST API呼び出し -->
    <script th:inline="javascript">
        const postId = /*[[${post.id}]]*/ 0;
        const token = localStorage.getItem('jwt_token');
        
        // コメント投稿
        document.getElementById('commentForm')?.addEventListener('submit', async (e) => {
            e.preventDefault();
            const content = document.getElementById('commentContent').value;
            
            // REST APIを呼び出してコメントを投稿
            // fetch API を使って POST /api/posts/{postId}/comments
            // 成功したらページをリロードまたは動的にDOMに追加
        });
        
        // コメント削除
        function deleteComment(commentId) {
            if (!confirm('このコメントを削除しますか？')) return;
            
            // REST APIを呼び出してコメントを削除
            // fetch API を使って DELETE /api/comments/{commentId}
        }
        
        // 記事削除
        function deletePost() {
            if (!confirm('この記事を削除しますか？')) return;
            
            // REST APIを呼び出して記事を削除
            // fetch API を使って DELETE /api/posts/{postId}
        }
    </script>
</body>
</html>
```

### 5. WebControllerの実装
`@Controller`を使ってThymeleafビューを返すコントローラーを実装してください。

**例**:
```java
@Controller
public class WebController {
    
    private final PostService postService;
    private final CommentService commentService;
    
    @GetMapping("/")
    public String index(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        
        // 記事一覧を取得してModelに追加
        // model.addAttribute("posts", ...);
        // model.addAttribute("currentPage", page);
        // model.addAttribute("totalPages", ...);
        
        return "index";
    }
    
    @GetMapping("/posts/{id}")
    public String postDetail(@PathVariable Long id, Model model, Principal principal) {
        // 記事詳細を取得
        // コメント一覧を取得
        // 現在のユーザーが記事の作成者かチェック
        
        // model.addAttribute("post", ...);
        // model.addAttribute("comments", ...);
        // model.addAttribute("isAuthor", ...);
        // model.addAttribute("isAuthenticated", principal != null);
        
        return "post/detail";
    }
    
    // 他のエンドポイントを実装してください
}
```

### 6. JavaScriptでREST APIを非同期呼び出し
Thymeleafで画面を表示しつつ、コメント投稿・削除はREST APIを使って非同期で実行してください。

**考えるポイント**:
- JWTトークンはlocalStorageに保存するべきか、Cookieにするべきか？
- CSRF対策は必要か？
- エラーハンドリングをどうするか？

## ✅ 動作確認

### 1. ブラウザでアクセス
```
http://localhost:8080/
```

### 2. 記事詳細を表示してコメントを投稿
1. ログインする
2. 記事詳細ページにアクセス
3. コメントフォームに入力して送信
4. ページをリロードせずにコメントが表示されることを確認

### 3. 自分の記事に編集・削除ボタンが表示されることを確認
1. 自分で投稿した記事の詳細ページを表示
2. 編集・削除ボタンが表示されることを確認
3. 他のユーザーの記事ではボタンが表示されないことを確認

## 🎓 学習ポイント

1. **1対多リレーション**: 記事とコメントの関連
2. **Thymeleaf**: サーバーサイドレンダリング
3. **REST API + Thymeleaf**: ハイブリッドなアプローチ
4. **非同期通信**: JavaScriptでのfetch API使用
5. **認証状態の判定**: ビューでの条件分岐
6. **フラグメント**: 共通部品の再利用

## 📝 追加課題（オプション）

1. コメントの編集機能
2. コメントへの返信機能（ネストコメント）
3. コメントのページネーション
4. いいね機能（記事・コメント両方）
5. リアルタイム更新（WebSocket使用）
6. マークダウンエディタの導入

## 🚀 次のステップ
Step 37では、タグ機能と画像アップロード機能を実装します。
