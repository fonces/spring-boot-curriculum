# Step 38: Thymeleafでブログ画面実装

## 🎯 このステップの目標

- ThymeleafテンプレートエンジンでWebページを構築する
- REST APIに加えて、ブラウザで操作可能なUIを実装する
- レイアウトテンプレートで共通部品（ヘッダー・フッター）を再利用する
- Spring Securityのフォームログインと統合する

**所要時間**: 約120分

---

## 📋 事前準備

このステップを始める前に、以下を確認してください：

- Step 34〜37が完了していること
- BlogHubアプリケーションが正常に起動すること
- REST APIでユーザー登録・ログイン・記事CRUD・コメント投稿が動作すること
- `pom.xml`に`spring-boot-starter-thymeleaf`が含まれていること

---

## 🎨 ThymeleafとREST APIの役割分担

| 方式 | メリット | デメリット |
|------|---------|-----------|
| **REST API** | フロントエンド技術の自由度が高い、モバイルアプリでも使える | フロントエンドの実装が別途必要 |
| **Thymeleaf (SSR)** | サーバー側で完結、SEOに有利、初期表示が速い | 動的なUI更新が難しい |

BlogHubでは、**両方のアプローチを併用**します。

---

## 📝 実装概要

このステップでは、以下の画面とコントローラーを**自分で実装**します：

### 実装する画面一覧

| 画面 | URL | テンプレート | 説明 |
|------|-----|-------------|------|
| 共通レイアウト | - | `layouts/layout.html` | ヘッダー・フッター・ナビゲーション |
| トップページ | `/` | `index.html` | 記事一覧、タグフィルター、ページネーション |
| タグ一覧 | `/tags` | `tags.html` | 全タグ表示 |
| 記事詳細 | `/articles/{id}` | `article-detail.html` | 記事本文、コメント一覧・投稿 |
| 記事投稿 | `/articles/new` | `articles/form.html` | 新規記事フォーム |
| 記事編集 | `/articles/{id}/edit` | `articles/form.html` | 編集フォーム（共用） |
| ユーザープロフィール | `/users/{username}` | `user-profile.html` | ユーザー情報と投稿記事 |
| ログイン | `/login` | `auth/login.html` | ログインフォーム |
| 新規登録 | `/signup` | `auth/signup.html` | ユーザー登録フォーム |

### 実装するコントローラー

| クラス | 責務 |
|--------|------|
| `WebController` | トップページ、タグ一覧、記事詳細、記事CRUD |
| `AuthWebController` | ログイン・サインアップ画面 |

---

## 🚀 ステップ1: プロジェクト構造の準備

### 1-1. テンプレートディレクトリの作成

```bash
cd ~/git/spring-boot-curriculum/workspace/bloghub
mkdir -p src/main/resources/templates/{layouts,articles,auth,users}
mkdir -p src/main/resources/static/{css,js,images}
```

### 1-2. ディレクトリ構成

```sh
src/main/resources/
├── templates/
│   ├── layouts/
│   │   └── layout.html          # 共通レイアウト
│   ├── articles/
│   │   ├── detail.html          # 記事詳細
│   │   └── form.html            # 記事作成・編集フォーム
│   ├── auth/
│   │   ├── login.html           # ログインフォーム
│   │   └── signup.html          # サインアップフォーム
│   ├── users/
│   │   └── profile.html         # ユーザープロフィール
│   ├── tags.html                # タグ一覧
│   └── index.html               # トップページ（記事一覧）
└── static/
    ├── css/
    │   └── style.css
    └── js/
        └── app.js
```

---

## 🚀 ステップ2: 共通レイアウトの実装 【自分で実装】

### 📁 ファイルパス
`src/main/resources/templates/layouts/layout.html`

### 📋 実装要件

**含めるべき要素**:
- ナビゲーションバー（ロゴ、ホーム、タグ一覧リンク）
- 未ログイン時: ログイン・新規登録リンク
- ログイン時: 記事を書く、ユーザードロップダウン（プロフィール、ログアウト）
- フラッシュメッセージ表示エリア（成功・エラー）
- メインコンテンツ挿入ポイント（`th:replace`）
- フッター

### 💡 Thymeleaf構文リファレンス

| 構文 | 説明 | 例 |
|------|------|-----|
| `th:text` | テキスト出力 | `<span th:text="${user.name}">` |
| `th:href` | リンク生成 | `<a th:href="@{/articles/{id}(id=${article.id})}">` |
| `th:if` / `th:unless` | 条件分岐 | `<div th:if="${message}">` |
| `th:each` | ループ | `<div th:each="article : ${articles}">` |
| `th:replace` | フラグメント挿入 | `<th:block th:replace="${content}">` |
| `sec:authorize` | 認証状態判定 | `sec:authorize="isAuthenticated()"` |

### 🔒 Spring Security統合

```html
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">

<!-- 未ログイン時のみ表示 -->
<li sec:authorize="!isAuthenticated()">
    <a th:href="@{/login}">ログイン</a>
</li>

<!-- ログイン時のみ表示 -->
<li sec:authorize="isAuthenticated()">
    <span sec:authentication="name">ユーザー名</span>
</li>

<!-- ログアウトフォーム -->
<form th:action="@{/logout}" method="post">
    <button type="submit">ログアウト</button>
</form>
```

### 🎨 推奨ライブラリ

```html
<!-- Bootstrap CSS -->
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">

<!-- Font Awesome -->
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">

<!-- Bootstrap JS -->
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
```

---

## 🚀 ステップ3: トップページの実装 【自分で実装】

### 3-1. WebController

**ファイルパス**: `src/main/java/com/example/bloghub/controller/WebController.java`

**機能要件**:
| メソッド | URL | 処理 |
|---------|-----|------|
| `home` | `GET /` | 記事一覧取得、タグフィルター対応、ページネーション |

**クエリパラメータ**:
| パラメータ | 型 | 説明 |
|-----------|-----|------|
| page | int | ページ番号（デフォルト0） |
| tag | String | フィルタするタグ名（オプション） |

**Modelに設定する属性**:
| 属性名 | 型 | 説明 |
|--------|-----|------|
| `articles` | `Page<ArticleResponse>` | 記事一覧 |
| `tags` | `List<Tag>` | 全タグ |
| `selectedTag` | `String` | 選択中のタグ |
| `title` | `String` | ページタイトル |

### 3-2. index.html

**ファイルパス**: `src/main/resources/templates/index.html`

**レイアウト**:
- サイドバー（タグ一覧）
- メインエリア（記事カード一覧）
- ページネーション

**必要なThymeleaf機能**:
| 機能 | 構文 |
|------|------|
| 記事ループ | `th:each="article : ${articles.content}"` |
| 日時フォーマット | `${#temporals.format(article.createdAt, 'yyyy-MM-dd HH:mm')}` |
| 文字列切り詰め | `${#strings.abbreviate(article.content, 200)}` |
| ページネーション | `${articles.number}`, `${articles.totalPages}` |

---

## 🚀 ステップ4: 記事詳細ページの実装 【自分で実装】

### 4-1. WebControllerにメソッド追加

**エンドポイント**:
| メソッド | URL | 処理 |
|---------|-----|------|
| `articleDetail` | `GET /articles/{id}` | 記事詳細とコメント取得 |
| `createComment` | `POST /articles/{id}/comments` | コメント投稿 |
| `deleteComment` | `POST /comments/{id}/delete` | コメント削除 |

**Modelに設定する属性**:
| 属性名 | 型 | 説明 |
|--------|-----|------|
| `article` | `ArticleResponse` | 記事詳細 |
| `comments` | `List<CommentResponse>` | コメント一覧 |
| `isOwner` | `boolean` | ログインユーザーが所有者か |

### 4-2. article-detail.html

**表示要素**:
- 記事タイトル、投稿者、作成日時、更新日時
- タグバッジ
- 記事本文
- 編集・削除ボタン（所有者のみ）
- コメント投稿フォーム（ログイン時のみ）
- コメント一覧（削除ボタンは所有者のみ）

**所有者チェックの例**:
```html
<div sec:authorize="isAuthenticated()">
    <div th:if="${#authentication.name == article.user.username}">
        <!-- 編集・削除ボタン -->
    </div>
</div>
```

---

## 🚀 ステップ5: 記事投稿・編集フォームの実装 【自分で実装】

### 5-1. WebControllerにメソッド追加

**エンドポイント**:
| メソッド | URL | 処理 |
|---------|-----|------|
| `newArticleForm` | `GET /articles/new` | 新規記事フォーム表示 |
| `createArticle` | `POST /articles/new` | 記事作成処理 |
| `editArticleForm` | `GET /articles/{id}/edit` | 編集フォーム表示 |
| `updateArticle` | `POST /articles/{id}/edit` | 記事更新処理 |
| `deleteArticle` | `POST /articles/{id}/delete` | 記事削除処理 |

**フォームバインディング**:
- `@ModelAttribute("articleRequest")` でDTOにバインド
- `@Valid` + `BindingResult` でバリデーション
- `RedirectAttributes.addFlashAttribute()` でフラッシュメッセージ

### 5-2. articles/form.html

**フォーム要素**:
| フィールド | 入力タイプ | バリデーション |
|-----------|----------|---------------|
| title | text | 必須、1〜200文字 |
| content | textarea | 必須 |
| tags | text | カンマ区切り |

**新規/編集の切り替え**:
```html
<form th:action="${isEdit} ? @{/articles/{id}/edit(id=${articleId})} : @{/articles/new}"
      th:object="${articleRequest}" 
      method="post">
```

**バリデーションエラー表示**:
```html
<input th:field="*{title}" 
       th:classappend="${#fields.hasErrors('title')} ? 'is-invalid'">
<div th:if="${#fields.hasErrors('title')}" class="invalid-feedback">
    <span th:errors="*{title}"></span>
</div>
```

---

## 🚀 ステップ6: ログイン・サインアップの実装 【自分で実装】

### 6-1. AuthWebController

**ファイルパス**: `src/main/java/com/example/bloghub/controller/AuthWebController.java`

**エンドポイント**:
| メソッド | URL | 処理 |
|---------|-----|------|
| `loginForm` | `GET /login` | ログインフォーム表示 |
| `signupForm` | `GET /signup` | サインアップフォーム表示 |
| `signup` | `POST /signup` | ユーザー登録処理 |

**ログイン処理**:
- Spring Securityが`POST /login`を自動処理
- 成功時: `/`にリダイレクト
- 失敗時: `/login?error`にリダイレクト

### 6-2. auth/login.html

**表示要素**:
- ユーザー名入力
- パスワード入力
- ログインボタン
- エラーメッセージ（`${param.error}`）
- ログアウトメッセージ（`${param.logout}`）
- 新規登録リンク

**ログインフォームの例**:
```html
<form th:action="@{/login}" method="post">
    <input type="text" name="username" required>
    <input type="password" name="password" required>
    <button type="submit">ログイン</button>
</form>
```

### 6-3. auth/signup.html

**表示要素**:
- ユーザー名入力（3〜20文字）
- メールアドレス入力
- パスワード入力（8文字以上）
- 登録ボタン
- バリデーションエラー表示
- ログインページリンク

---

## 🚀 ステップ7: ユーザープロフィールの実装 【自分で実装】

### 7-1. WebControllerにメソッド追加

**エンドポイント**:
| メソッド | URL | 処理 |
|---------|-----|------|
| `userProfile` | `GET /users/{username}` | ユーザー情報と投稿記事取得 |
| `myProfile` | `GET /profile` | 自分のプロフィール（リダイレクト） |

### 7-2. user-profile.html

**表示要素**:
- ユーザー情報カード（ユーザー名、メール、登録日）
- 投稿記事一覧（ページネーション付き）
- ホームに戻るリンク

---

## 🚀 ステップ8: SecurityConfigの更新 【自分で実装】

### 📋 実装要件

REST API（`/api/**`）とWeb画面で異なるセキュリティ設定を適用します。

**2つのSecurityFilterChainを定義**:

| FilterChain | 順序 | 対象 | 特徴 |
|-------------|------|------|------|
| `apiSecurityFilterChain` | @Order(1) | `/api/**` | JWT認証、ステートレス |
| `webSecurityFilterChain` | @Order(2) | その他 | フォームログイン、セッション |

**Web画面の公開パス**:
- `/`, `/articles/{id}`, `/tags`, `/users/{username}`
- `/login`, `/signup`
- `/css/**`, `/js/**`, `/images/**`

**認証必須パス**:
- `/articles/new`, `/articles/{id}/edit`, `/articles/{id}/delete`
- `/articles/{id}/comments`, `/comments/{id}/delete`
- `/profile`

**フォームログイン設定**:
```java
.formLogin(form -> form
    .loginPage("/login")
    .defaultSuccessUrl("/", true)
    .permitAll()
)
.logout(logout -> logout
    .logoutUrl("/logout")
    .logoutSuccessUrl("/?logout")
    .permitAll()
)
```

---

## ✅ ステップ9: 動作確認

### 9-1. アプリケーションの起動

```bash
cd ~/git/spring-boot-curriculum/workspace/bloghub
./mvnw clean install
./mvnw spring-boot:run
```

### 9-2. ブラウザでの確認チェックリスト

| 確認項目 | URL | 期待される動作 |
|---------|-----|---------------|
| トップページ | `http://localhost:8080/` | 記事一覧表示 |
| タグフィルター | `http://localhost:8080/?tag=Spring` | フィルタリング動作 |
| 記事詳細 | 記事タイトルをクリック | 詳細とコメント表示 |
| 新規登録 | 右上の「新規登録」 | フォーム表示、登録成功 |
| ログイン | ログインページ | 認証成功でリダイレクト |
| 記事投稿 | 「記事を書く」クリック | フォーム表示、投稿成功 |
| コメント投稿 | 記事詳細ページ | 即座に表示 |
| 記事編集 | 自分の記事の「編集」 | フォームに既存データ |
| 記事削除 | 自分の記事の「削除」 | 確認後、トップにリダイレクト |
| ログアウト | ユーザーメニューから | トップにリダイレクト |

---

## 🐛 トラブルシューティング

### エラー: Template might not exist

**原因**: テンプレートファイルが見つからない

**解決策**:
1. ファイルパスを確認: `src/main/resources/templates/...`
2. ファイル名とController内の文字列が一致しているか確認

### エラー: CSRFトークンエラーでログインできない

**解決策**: フォームに`th:action`を使用
```html
<form th:action="@{/login}" method="post">
```

### エラー: sec:authorize属性が認識されない

**解決策1**: HTMLに名前空間を追加
```html
<html xmlns:th="http://www.thymeleaf.org" 
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
```

**解決策2**: 依存関係を確認
```xml
<dependency>
    <groupId>org.thymeleaf.extras</groupId>
    <artifactId>thymeleaf-extras-springsecurity6</artifactId>
</dependency>
```

### エラー: @PathVariableが機能しない

**解決策**: パラメータ名を明示
```java
@GetMapping("/articles/{id}")
public String articleDetail(@PathVariable("id") Long id, Model model)
```

### エラー: ConcurrentModificationException

**解決策**: Articleエンティティでカスタムsetterを使用（Step 34参照）

---

## 🎨 チャレンジ課題

### チャレンジ 1: マークダウンプレビュー

記事投稿フォームにリアルタイムプレビュー機能を追加しましょう。

**ヒント**: marked.jsを使用

### チャレンジ 2: ダークモード切り替え

LocalStorageを使ってダークモード設定を保存する機能を追加しましょう。

### チャレンジ 3: いいね機能（非同期）

Ajaxで非同期にいいね数を更新する機能を実装しましょう。

---

## 📚 このステップで学んだこと

- ✅ Thymeleafテンプレートエンジンの基本構文
- ✅ レイアウトテンプレートで共通部品を再利用する方法
- ✅ Spring Securityとの統合（`sec:authorize`, `sec:authentication`）
- ✅ フォームバインディングとバリデーションエラー表示
- ✅ ページネーションの実装
- ✅ フラッシュメッセージの表示
- ✅ REST APIとWeb画面で異なるセキュリティ設定を適用する方法
- ✅ フォームログインとJWT認証の併用

---

## 💡 補足: ThymeleafとReact/Vueの使い分け

| ケース | 推奨 |
|--------|------|
| SEO重要、シンプルUI | Thymeleaf |
| リッチUI、モバイル共通化 | React/Vue |
| プロトタイプ開発 | Thymeleaf |
| フロントエンド専門チーム | React/Vue |

---

## ➡️ 次のステップ

[Step 39: テストとデプロイ準備](STEP_39.md)へ進みましょう！

次は、ユニットテストと統合テストを実装し、JaCoCoでテストカバレッジを測定します。また、本番環境向けの設定とDockerコンテナ化を行います。
