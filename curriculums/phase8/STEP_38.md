# Step 38: Thymeleafでブログ画面実装

## 🎯 このステップの目標

- ThymeleafテンプレートエンジンでWebページを構築する
- REST APIに加えて、ブラウザで操作可能なUI を実装する
- レイアウトテンプレートで共通部品（ヘッダー・フッター）を再利用する
- 記事一覧、詳細、投稿・編集フォームを実装する
- ログイン・サインアップフォームを実装する
- Spring Securityのフォームログインと統合する

**所要時間**: 約120分

---

## 📋 事前準備

このステップを始める前に、以下を確認してください：

- Step 34〜37が完了していること
- BlogHubアプリケーションが正常に起動すること
- REST APIでユーザー登録・ログイン・記事CRUD・コメント投稿が動作すること
- `pom.xml`に`spring-boot-starter-thymeleaf`が含まれていること

依存関係の確認:

```bash
grep -A 3 "spring-boot-starter-thymeleaf" pom.xml
```

---

## 🎨 ThymeleafとREST APIの役割分担

### なぜThymeleafが必要か

これまでのステップでは、REST APIを構築してきました。APIはフロントエンドやモバイルアプリから利用できる柔軟な設計ですが、**すぐにブラウザで使えるUI**を提供するには、サーバーサイドレンダリング（SSR）が便利です。

| 方式 | メリット | デメリット |
|------|---------|-----------|
| **REST API** | フロントエンド技術の自由度が高い、モバイルアプリでも使える | フロントエンドの実装が別途必要 |
| **Thymeleaf (SSR)** | サーバー側で完結、SEOに有利、初期表示が速い | 動的なUI更新が難しい |

BlogHubでは、**両方のアプローチを併用**します：
- REST API: モバイルアプリや他サービスからの利用を想定
- Thymeleaf: 管理画面やユーザー向けWebページ

---

## 🚀 ステップ1: プロジェクト構造の準備

### 1-1. テンプレートディレクトリの作成

Thymeleafのテンプレートファイルを格納するディレクトリを作成します：

```bash
cd ~/git/spring-boot-curriculum/workspace/bloghub
mkdir -p src/main/resources/templates/{layouts,articles,auth,users}
mkdir -p src/main/resources/static/{css,js,images}
```

### 1-2. ディレクトリ構成の確認

```
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
│   └── index.html               # トップページ（記事一覧）
└── static/
    ├── css/
    │   └── style.css
    └── js/
        └── app.js
```

---

## 🚀 ステップ2: 共通レイアウトの作成

### 2-1. layout.htmlの作成

すべてのページで使用する共通レイアウトを作成します。

**ファイルパス**: `src/main/resources/templates/layouts/layout.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="${title} ? ${title} + ' - BlogHub' : 'BlogHub'">BlogHub</title>
    
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    
    <!-- Font Awesome -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    
    <!-- Custom CSS -->
    <link th:href="@{/css/style.css}" rel="stylesheet">
</head>
<body>
    <!-- ナビゲーションバー -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-primary">
        <div class="container">
            <a class="navbar-brand" th:href="@{/}">
                <i class="fas fa-blog"></i> BlogHub
            </a>
            
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
                <span class="navbar-toggler-icon"></span>
            </button>
            
            <div class="collapse navbar-collapse" id="navbarNav">
                <ul class="navbar-nav me-auto">
                    <li class="nav-item">
                        <a class="nav-link" th:href="@{/}">
                            <i class="fas fa-home"></i> ホーム
                        </a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" th:href="@{/tags}">
                            <i class="fas fa-tags"></i> タグ一覧
                        </a>
                    </li>
                </ul>
                
                <ul class="navbar-nav">
                    <!-- 未ログイン時 -->
                    <li class="nav-item" sec:authorize="!isAuthenticated()">
                        <a class="nav-link" th:href="@{/login}">
                            <i class="fas fa-sign-in-alt"></i> ログイン
                        </a>
                    </li>
                    <li class="nav-item" sec:authorize="!isAuthenticated()">
                        <a class="nav-link" th:href="@{/signup}">
                            <i class="fas fa-user-plus"></i> 新規登録
                        </a>
                    </li>
                    
                    <!-- ログイン時 -->
                    <li class="nav-item" sec:authorize="isAuthenticated()">
                        <a class="nav-link" th:href="@{/articles/new}">
                            <i class="fas fa-pen"></i> 記事を書く
                        </a>
                    </li>
                    <li class="nav-item dropdown" sec:authorize="isAuthenticated()">
                        <a class="nav-link dropdown-toggle" href="#" role="button" data-bs-toggle="dropdown">
                            <i class="fas fa-user"></i> <span sec:authentication="name">ユーザー</span>
                        </a>
                        <ul class="dropdown-menu dropdown-menu-end">
                            <li>
                                <a class="dropdown-item" th:href="@{/users/my-articles}">
                                    <i class="fas fa-file-alt"></i> 自分の記事
                                </a>
                            </li>
                            <li>
                                <a class="dropdown-item" th:href="@{/users/profile}">
                                    <i class="fas fa-user-cog"></i> プロフィール
                                </a>
                            </li>
                            <li><hr class="dropdown-divider"></li>
                            <li>
                                <form th:action="@{/logout}" method="post">
                                    <button type="submit" class="dropdown-item">
                                        <i class="fas fa-sign-out-alt"></i> ログアウト
                                    </button>
                                </form>
                            </li>
                        </ul>
                    </li>
                </ul>
            </div>
        </div>
    </nav>
    
    <!-- メインコンテンツ -->
    <main class="container mt-4">
        <!-- フラッシュメッセージ -->
        <div th:if="${message}" class="alert alert-success alert-dismissible fade show" role="alert">
            <span th:text="${message}"></span>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
        
        <div th:if="${error}" class="alert alert-danger alert-dismissible fade show" role="alert">
            <span th:text="${error}"></span>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
        
        <!-- ページごとのコンテンツ -->
        <th:block th:replace="${content}"></th:block>
    </main>
    
    <!-- フッター -->
    <footer class="mt-5 py-4 bg-light">
        <div class="container text-center">
            <p class="text-muted mb-0">
                &copy; 2025 BlogHub. All rights reserved. | 
                <a href="#">プライバシーポリシー</a> | 
                <a href="#">利用規約</a>
            </p>
        </div>
    </footer>
    
    <!-- Bootstrap JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    
    <!-- Custom JS -->
    <script th:src="@{/js/app.js}"></script>
</body>
</html>
```

### 2-2. レイアウトの解説

#### Thymeleafの名前空間
```html
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
```
- `th`: Thymeleafの標準タグ
- `sec`: Spring Securityとの統合タグ

#### `th:replace`
```html
<th:block th:replace="${content}"></th:block>
```
- `${content}`変数に指定されたテンプレートフラグメントを埋め込む
- 各ページでこの部分を上書きする

#### `sec:authorize`
```html
<li sec:authorize="!isAuthenticated()">
    <a th:href="@{/login}">ログイン</a>
</li>
```
- `isAuthenticated()`: ログイン状態の判定
- ログイン時のみ、未ログイン時のみ表示する要素を制御

**重要**: Thymeleaf 3.1以降では、Spring Security統合のために以下の名前空間宣言が必要です：

```html
<html xmlns:th="http://www.thymeleaf.org" 
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
```

これにより、`sec:authorize`属性が正しく動作します。

---

## 🚀 ステップ3: トップページ（記事一覧）の実装

### 3-1. DTOクラスの準備

WebControllerでフォームからDTOを生成する際、`@Builder`アノテーションが必要です。

以下の4つのDTOクラスに`@Builder`を追加します：

**ArticleCreateRequest.java**:
```java
import lombok.Builder;
import lombok.Data;

@Data
@Builder  // 追加
public class ArticleCreateRequest {
    // フィールドは既存のまま
}
```

**ArticleUpdateRequest.java**:
```java
import lombok.Builder;
import lombok.Data;

@Data
@Builder  // 追加
public class ArticleUpdateRequest {
    // フィールドは既存のまま
}
```

**CommentCreateRequest.java**:
```java
import lombok.Builder;
import lombok.Data;

@Data
@Builder  // 追加
public class CommentCreateRequest {
    // フィールドは既存のまま
}
```

**SignupRequest.java**:
```java
import lombok.Builder;
import lombok.Data;

@Data
@Builder  // 追加
public class SignupRequest {
    // フィールドは既存のまま
}
```

### 3-2. WebControllerの作成

**ファイルパス**: `src/main/java/com/example/bloghub/controller/WebController.java`

```java
package com.example.bloghub.controller;

import com.example.bloghub.dto.article.ArticleResponse;
import com.example.bloghub.service.ArticleService;
import com.example.bloghub.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final ArticleService articleService;
    private final TagService tagService;

    @GetMapping("/")
    public String home(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String tag,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, 10);
        Page<ArticleResponse> articles;
        
        if (tag != null && !tag.isEmpty()) {
            articles = articleService.getArticlesByTag(tag, pageable);
            model.addAttribute("selectedTag", tag);
        } else {
            articles = articleService.getAllArticles(pageable);
        }
        
        model.addAttribute("title", "ホーム");
        model.addAttribute("content", "index :: content");
        model.addAttribute("articles", articles);
        model.addAttribute("tags", tagService.getAllTags());
        
        return "layouts/layout";
    }
}
```

### 3-2. index.htmlの作成

**ファイルパス**: `src/main/resources/templates/index.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
    <th:block th:fragment="content">
        <div class="row">
            <!-- サイドバー -->
            <div class="col-md-3">
                <div class="card mb-4">
                    <div class="card-header bg-primary text-white">
                        <i class="fas fa-tags"></i> タグで絞り込み
                    </div>
                    <div class="card-body">
                        <a th:href="@{/}" class="badge bg-secondary me-1 mb-1">すべて</a>
                        <a th:each="tag : ${tags}"
                           th:href="@{/(tag=${tag.name})}"
                           th:text="${tag.name}"
                           th:class="${tag.name == selectedTag} ? 'badge bg-primary me-1 mb-1' : 'badge bg-light text-dark me-1 mb-1'">
                        </a>
                    </div>
                </div>
            </div>
            
            <!-- 記事一覧 -->
            <div class="col-md-9">
                <h1 class="mb-4">
                    <i class="fas fa-newspaper"></i> 
                    <span th:if="${selectedTag}">
                        「<span th:text="${selectedTag}"></span>」の記事
                    </span>
                    <span th:unless="${selectedTag}">最新記事</span>
                </h1>
                
                <!-- 記事が0件の場合 -->
                <div th:if="${articles.empty}" class="alert alert-info">
                    <i class="fas fa-info-circle"></i> 記事がまだありません。
                </div>
                
                <!-- 記事カード -->
                <div th:each="article : ${articles.content}" class="card mb-3">
                    <div class="card-body">
                        <h5 class="card-title">
                            <a th:href="@{/articles/{id}(id=${article.id})}" 
                               th:text="${article.title}"
                               class="text-decoration-none">
                            </a>
                        </h5>
                        
                        <div class="mb-2">
                            <small class="text-muted">
                                <i class="fas fa-user"></i> 
                                <a th:href="@{/users/{username}(username=${article.user.username})}"
                                   th:text="${article.user.username}"
                                   class="text-decoration-none">
                                </a>
                                
                                <i class="fas fa-calendar-alt ms-3"></i>
                                <span th:text="${#temporals.format(article.createdAt, 'yyyy-MM-dd HH:mm')}"></span>
                                
                                <i class="fas fa-comment ms-3"></i>
                                <span th:text="${article.commentCount}"></span> コメント
                            </small>
                        </div>
                        
                        <p class="card-text" th:text="${#strings.abbreviate(article.content, 200)}"></p>
                        
                        <div>
                            <span th:each="tag : ${article.tags}" class="badge bg-light text-dark me-1">
                                <i class="fas fa-tag"></i> <span th:text="${tag}"></span>
                            </span>
                        </div>
                    </div>
                </div>
                
                <!-- ページネーション -->
                <nav th:if="${articles.totalPages > 1}">
                    <ul class="pagination justify-content-center">
                        <li class="page-item" th:classappend="${articles.first} ? 'disabled'">
                            <a class="page-link" 
                               th:href="@{/(page=${articles.number - 1}, tag=${selectedTag})}">
                                前へ
                            </a>
                        </li>
                        
                        <li th:each="i : ${#numbers.sequence(0, articles.totalPages - 1)}"
                            class="page-item"
                            th:classappend="${i == articles.number} ? 'active'">
                            <a class="page-link" 
                               th:href="@{/(page=${i}, tag=${selectedTag})}"
                               th:text="${i + 1}">
                            </a>
                        </li>
                        
                        <li class="page-item" th:classappend="${articles.last} ? 'disabled'">
                            <a class="page-link" 
                               th:href="@{/(page=${articles.number + 1}, tag=${selectedTag})}">
                                次へ
                            </a>
                        </li>
                    </ul>
                </nav>
            </div>
        </div>
    </th:block>
</body>
</html>
```

### 3-3. コードの解説

#### `th:fragment`
```html
<th:block th:fragment="content">
```
- このブロックを`layout.html`の`${content}`部分に埋め込む

#### `th:each`
```html
<div th:each="article : ${articles.content}">
```
- Javaの拡張for文と同様
- `articles.content`（Pageの実際のデータ）をループ

#### `#temporals.format`
```html
<span th:text="${#temporals.format(article.createdAt, 'yyyy-MM-dd HH:mm')}"></span>
```
- 日時を指定したフォーマットで表示
- `LocalDateTime`を扱う

#### `#strings.abbreviate`
```html
<p th:text="${#strings.abbreviate(article.content, 200)}"></p>
```
- 文字列を指定文字数で切り詰め、`...`を付ける

---

## 🚀 ステップ4: 記事詳細ページの実装

### 4-1. WebControllerにメソッド追加

**ファイルパス**: `src/main/java/com/example/bloghub/controller/WebController.java`

```java
@GetMapping("/articles/{id}")
public String articleDetail(@PathVariable Long id, Model model) {
    ArticleResponse article = articleService.getArticleById(id);
    List<CommentResponse> comments = commentService.getCommentsByArticleId(id);
    
    model.addAttribute("title", article.getTitle());
    model.addAttribute("content", "articles/detail :: content");
    model.addAttribute("article", article);
    model.addAttribute("comments", comments);
    model.addAttribute("commentRequest", new CommentCreateRequest());
    
    return "layouts/layout";
}
```

### 4-2. articles/detail.htmlの作成

**ファイルパス**: `src/main/resources/templates/articles/detail.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<body>
    <th:block th:fragment="content">
        <!-- 記事詳細 -->
        <article class="mb-5">
            <h1 th:text="${article.title}"></h1>
            
            <div class="mb-3">
                <small class="text-muted">
                    <i class="fas fa-user"></i> 
                    <a th:href="@{/users/{username}(username=${article.user.username})}"
                       th:text="${article.user.username}">
                    </a>
                    
                    <i class="fas fa-calendar-alt ms-3"></i>
                    <span th:text="${#temporals.format(article.createdAt, 'yyyy年MM月dd日 HH:mm')}"></span>
                    
                    <i class="fas fa-edit ms-3"></i>
                    <span th:text="${#temporals.format(article.updatedAt, 'yyyy年MM月dd日 HH:mm')}"></span>
                </small>
            </div>
            
            <div class="mb-3">
                <span th:each="tag : ${article.tags}" class="badge bg-primary me-1">
                    <i class="fas fa-tag"></i> <span th:text="${tag}"></span>
                </span>
            </div>
            
            <!-- 記事本文（マークダウン → HTML変換は別途実装） -->
            <div class="article-content" th:utext="${article.content}"></div>
            
            <!-- 編集・削除ボタン（所有者のみ） -->
            <div class="mt-4" sec:authorize="isAuthenticated()">
                <div th:if="${#authentication.name == article.user.username}">
                    <a th:href="@{/articles/{id}/edit(id=${article.id})}" class="btn btn-primary">
                        <i class="fas fa-edit"></i> 編集
                    </a>
                    <form th:action="@{/articles/{id}/delete(id=${article.id})}" 
                          method="post" 
                          style="display: inline;"
                          onsubmit="return confirm('本当に削除しますか？');">
                        <button type="submit" class="btn btn-danger">
                            <i class="fas fa-trash"></i> 削除
                        </button>
                    </form>
                </div>
            </div>
        </article>
        
        <hr>
        
        <!-- コメントセクション -->
        <section>
            <h3><i class="fas fa-comments"></i> コメント (<span th:text="${comments.size()}"></span>)</h3>
            
            <!-- コメント投稿フォーム（ログイン時のみ） -->
            <div sec:authorize="isAuthenticated()" class="mb-4">
                <form th:action="@{/articles/{id}/comments(id=${article.id})}" 
                      th:object="${commentRequest}" 
                      method="post">
                    <div class="mb-3">
                        <label for="content" class="form-label">コメントを投稿</label>
                        <textarea th:field="*{content}" 
                                  class="form-control" 
                                  id="content" 
                                  rows="3" 
                                  placeholder="コメントを入力してください"></textarea>
                        <div th:if="${#fields.hasErrors('content')}" class="text-danger">
                            <small th:errors="*{content}"></small>
                        </div>
                    </div>
                    <button type="submit" class="btn btn-primary">
                        <i class="fas fa-paper-plane"></i> 投稿
                    </button>
                </form>
            </div>
            
            <!-- ログインしていない場合 -->
            <div sec:authorize="!isAuthenticated()" class="alert alert-info">
                コメントを投稿するには<a th:href="@{/login}">ログイン</a>してください。
            </div>
            
            <!-- コメント一覧 -->
            <div th:if="${comments.empty}" class="alert alert-secondary">
                まだコメントがありません。
            </div>
            
            <div th:each="comment : ${comments}" class="card mb-3">
                <div class="card-body">
                    <div class="d-flex justify-content-between align-items-start">
                        <div>
                            <strong th:text="${comment.user.username}"></strong>
                            <small class="text-muted ms-2">
                                <i class="fas fa-clock"></i>
                                <span th:text="${#temporals.format(comment.createdAt, 'yyyy-MM-dd HH:mm')}"></span>
                            </small>
                        </div>
                        
                        <!-- 削除ボタン（所有者のみ） -->
                        <div sec:authorize="isAuthenticated()">
                            <form th:if="${#authentication.name == comment.user.username}"
                                  th:action="@{/comments/{id}/delete(id=${comment.id})}" 
                                  method="post"
                                  onsubmit="return confirm('コメントを削除しますか？');">
                                <button type="submit" class="btn btn-sm btn-outline-danger">
                                    <i class="fas fa-trash"></i>
                                </button>
                            </form>
                        </div>
                    </div>
                    
                    <p class="mt-2 mb-0" th:text="${comment.content}"></p>
                </div>
            </div>
        </section>
    </th:block>
</body>
</html>
```

---

## 🚀 ステップ5: 記事投稿・編集フォームの実装

### 5-1. WebControllerにメソッド追加

```java
@GetMapping("/articles/new")
public String newArticleForm(Model model) {
    model.addAttribute("title", "新規記事作成");
    model.addAttribute("content", "articles/form :: content");
    model.addAttribute("articleRequest", new ArticleCreateRequest());
    model.addAttribute("isEdit", false);
    
    return "layouts/layout";
}

@PostMapping("/articles/new")
public String createArticle(
        @Valid @ModelAttribute("articleRequest") ArticleCreateRequest request,
        BindingResult result,
        Authentication authentication,
        RedirectAttributes redirectAttributes,
        Model model
) {
    if (result.hasErrors()) {
        model.addAttribute("title", "新規記事作成");
        model.addAttribute("content", "articles/form :: content");
        model.addAttribute("isEdit", false);
        return "layouts/layout";
    }
    
    String username = authentication.getName();
    ArticleResponse article = articleService.createArticle(request, username);
    
    redirectAttributes.addFlashAttribute("message", "記事を投稿しました！");
    return "redirect:/articles/" + article.getId();
}

@GetMapping("/articles/{id}/edit")
public String editArticleForm(@PathVariable Long id, Authentication authentication, Model model) {
    ArticleResponse article = articleService.getArticleById(id);
    
    // 所有者チェック
    if (!article.getUser().getUsername().equals(authentication.getName())) {
        throw new UnauthorizedException("この記事を編集する権限がありません");
    }
    
    ArticleUpdateRequest updateRequest = new ArticleUpdateRequest();
    updateRequest.setTitle(article.getTitle());
    updateRequest.setContent(article.getContent());
    updateRequest.setTags(article.getTags());
    
    model.addAttribute("title", "記事編集");
    model.addAttribute("content", "articles/form :: content");
    model.addAttribute("articleRequest", updateRequest);
    model.addAttribute("isEdit", true);
    model.addAttribute("articleId", id);
    
    return "layouts/layout";
}

@PostMapping("/articles/{id}/edit")
public String updateArticle(
        @PathVariable Long id,
        @Valid @ModelAttribute("articleRequest") ArticleUpdateRequest request,
        BindingResult result,
        Authentication authentication,
        RedirectAttributes redirectAttributes,
        Model model
) {
    if (result.hasErrors()) {
        model.addAttribute("title", "記事編集");
        model.addAttribute("content", "articles/form :: content");
        model.addAttribute("isEdit", true);
        model.addAttribute("articleId", id);
        return "layouts/layout";
    }
    
    String username = authentication.getName();
    articleService.updateArticle(id, request, username);
    
    redirectAttributes.addFlashAttribute("message", "記事を更新しました！");
    return "redirect:/articles/" + id;
}

@PostMapping("/articles/{id}/delete")
public String deleteArticle(
        @PathVariable Long id,
        Authentication authentication,
        RedirectAttributes redirectAttributes
) {
    String username = authentication.getName();
    articleService.deleteArticle(id, username);
    
    redirectAttributes.addFlashAttribute("message", "記事を削除しました");
    return "redirect:/";
}
```

### 5-2. articles/form.htmlの作成

**ファイルパス**: `src/main/resources/templates/articles/form.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
    <th:block th:fragment="content">
        <div class="row justify-content-center">
            <div class="col-md-10">
                <h1 th:text="${isEdit} ? '記事を編集' : '新しい記事を書く'"></h1>
                
                <form th:action="${isEdit} ? @{/articles/{id}/edit(id=${articleId})} : @{/articles/new}" 
                      th:object="${articleRequest}" 
                      method="post">
                    
                    <!-- タイトル -->
                    <div class="mb-3">
                        <label for="title" class="form-label">タイトル <span class="text-danger">*</span></label>
                        <input type="text" 
                               th:field="*{title}" 
                               class="form-control" 
                               th:classappend="${#fields.hasErrors('title')} ? 'is-invalid'"
                               id="title" 
                               placeholder="記事のタイトルを入力">
                        <div th:if="${#fields.hasErrors('title')}" class="invalid-feedback">
                            <span th:errors="*{title}"></span>
                        </div>
                    </div>
                    
                    <!-- 本文 -->
                    <div class="mb-3">
                        <label for="content" class="form-label">本文 <span class="text-danger">*</span></label>
                        <textarea th:field="*{content}" 
                                  class="form-control" 
                                  th:classappend="${#fields.hasErrors('content')} ? 'is-invalid'"
                                  id="content" 
                                  rows="15" 
                                  placeholder="マークダウン形式で記事を書いてください"></textarea>
                        <div th:if="${#fields.hasErrors('content')}" class="invalid-feedback">
                            <span th:errors="*{content}"></span>
                        </div>
                        <small class="form-text text-muted">
                            <i class="fas fa-info-circle"></i> マークダウン記法が使えます
                        </small>
                    </div>
                    
                    <!-- タグ -->
                    <div class="mb-3">
                        <label for="tags" class="form-label">タグ</label>
                        <input type="text" 
                               th:field="*{tags}" 
                               class="form-control" 
                               id="tags" 
                               placeholder="Java, Spring Boot, Tutorial（カンマ区切り）">
                        <small class="form-text text-muted">
                            <i class="fas fa-info-circle"></i> カンマ（,）で区切って複数のタグを入力できます
                        </small>
                    </div>
                    
                    <!-- ボタン -->
                    <div class="d-flex gap-2">
                        <button type="submit" class="btn btn-primary">
                            <i th:class="${isEdit} ? 'fas fa-save' : 'fas fa-paper-plane'"></i>
                            <span th:text="${isEdit} ? '更新' : '投稿'"></span>
                        </button>
                        <a th:href="@{/}" class="btn btn-secondary">
                            <i class="fas fa-times"></i> キャンセル
                        </a>
                    </div>
                </form>
            </div>
        </div>
    </th:block>
</body>
</html>
```

---

## 🚀 ステップ6: ログイン・サインアップフォームの実装

### 6-1. auth/login.htmlの作成

**ファイルパス**: `src/main/resources/templates/auth/login.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
    <th:block th:fragment="content">
        <div class="row justify-content-center">
            <div class="col-md-5">
                <div class="card">
                    <div class="card-header bg-primary text-white">
                        <h4 class="mb-0"><i class="fas fa-sign-in-alt"></i> ログイン</h4>
                    </div>
                    <div class="card-body">
                        <!-- エラーメッセージ -->
                        <div th:if="${param.error}" class="alert alert-danger">
                            <i class="fas fa-exclamation-circle"></i> ユーザー名またはパスワードが正しくありません
                        </div>
                        
                        <div th:if="${param.logout}" class="alert alert-success">
                            <i class="fas fa-check-circle"></i> ログアウトしました
                        </div>
                        
                        <form th:action="@{/login}" method="post">
                            <div class="mb-3">
                                <label for="username" class="form-label">ユーザー名</label>
                                <input type="text" 
                                       class="form-control" 
                                       id="username" 
                                       name="username" 
                                       placeholder="ユーザー名を入力"
                                       required>
                            </div>
                            
                            <div class="mb-3">
                                <label for="password" class="form-label">パスワード</label>
                                <input type="password" 
                                       class="form-control" 
                                       id="password" 
                                       name="password" 
                                       placeholder="パスワードを入力"
                                       required>
                            </div>
                            
                            <div class="d-grid">
                                <button type="submit" class="btn btn-primary">
                                    <i class="fas fa-sign-in-alt"></i> ログイン
                                </button>
                            </div>
                        </form>
                        
                        <hr>
                        
                        <p class="text-center mb-0">
                            アカウントをお持ちでない方は 
                            <a th:href="@{/signup}">新規登録</a>
                        </p>
                    </div>
                </div>
            </div>
        </div>
    </th:block>
</body>
</html>
```

### 6-2. auth/signup.htmlの作成

**ファイルパス**: `src/main/resources/templates/auth/signup.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
    <th:block th:fragment="content">
        <div class="row justify-content-center">
            <div class="col-md-5">
                <div class="card">
                    <div class="card-header bg-primary text-white">
                        <h4 class="mb-0"><i class="fas fa-user-plus"></i> 新規登録</h4>
                    </div>
                    <div class="card-body">
                        <form th:action="@{/signup}" th:object="${signupRequest}" method="post">
                            <div class="mb-3">
                                <label for="username" class="form-label">ユーザー名 <span class="text-danger">*</span></label>
                                <input type="text" 
                                       th:field="*{username}" 
                                       class="form-control" 
                                       th:classappend="${#fields.hasErrors('username')} ? 'is-invalid'"
                                       id="username" 
                                       placeholder="ユーザー名（3〜20文字）">
                                <div th:if="${#fields.hasErrors('username')}" class="invalid-feedback">
                                    <span th:errors="*{username}"></span>
                                </div>
                            </div>
                            
                            <div class="mb-3">
                                <label for="email" class="form-label">メールアドレス <span class="text-danger">*</span></label>
                                <input type="email" 
                                       th:field="*{email}" 
                                       class="form-control" 
                                       th:classappend="${#fields.hasErrors('email')} ? 'is-invalid'"
                                       id="email" 
                                       placeholder="example@example.com">
                                <div th:if="${#fields.hasErrors('email')}" class="invalid-feedback">
                                    <span th:errors="*{email}"></span>
                                </div>
                            </div>
                            
                            <div class="mb-3">
                                <label for="password" class="form-label">パスワード <span class="text-danger">*</span></label>
                                <input type="password" 
                                       th:field="*{password}" 
                                       class="form-control" 
                                       th:classappend="${#fields.hasErrors('password')} ? 'is-invalid'"
                                       id="password" 
                                       placeholder="8文字以上">
                                <div th:if="${#fields.hasErrors('password')}" class="invalid-feedback">
                                    <span th:errors="*{password}"></span>
                                </div>
                            </div>
                            
                            <div class="d-grid">
                                <button type="submit" class="btn btn-primary">
                                    <i class="fas fa-user-plus"></i> 登録
                                </button>
                            </div>
                        </form>
                        
                        <hr>
                        
                        <p class="text-center mb-0">
                            すでにアカウントをお持ちの方は 
                            <a th:href="@{/login}">ログイン</a>
                        </p>
                    </div>
                </div>
            </div>
        </div>
    </th:block>
</body>
</html>
```

### 6-3. AuthControllerの追加

**ファイルパス**: `src/main/java/com/example/bloghub/controller/AuthWebController.java`

```java
package com.example.bloghub.controller;

import com.example.bloghub.dto.auth.SignupRequest;
import com.example.bloghub.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthWebController {

    private final AuthService authService;

    @GetMapping("/login")
    public String loginForm(Model model) {
        model.addAttribute("title", "ログイン");
        model.addAttribute("content", "auth/login :: content");
        return "layouts/layout";
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("title", "新規登録");
        model.addAttribute("content", "auth/signup :: content");
        model.addAttribute("signupRequest", new SignupRequest());
        return "layouts/layout";
    }

    @PostMapping("/signup")
    public String signup(
            @Valid @ModelAttribute SignupRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (result.hasErrors()) {
            model.addAttribute("title", "新規登録");
            model.addAttribute("content", "auth/signup :: content");
            return "layouts/layout";
        }

        try {
            authService.signup(request);
            redirectAttributes.addFlashAttribute("message", "登録が完了しました。ログインしてください。");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("title", "新規登録");
            model.addAttribute("content", "auth/signup :: content");
            return "layouts/layout";
        }
    }
}
```

---

## 🚀 ステップ7: SecurityConfigの更新

### 7-1. SecurityConfigの修正

**必要なインポートの追加**:

まず、SecurityConfigに以下のインポート文を追加します：

```java
import org.springframework.security.config.Customizer;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
```

**ファイルパス**: `src/main/java/com/example/bloghub/config/SecurityConfig.java`

SecurityConfigを修正し、フォームログインとREST APIの両方をサポートします：

```java
package com.example.bloghub.config;

import com.example.bloghub.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // REST API用のセキュリティ設定
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Web画面用のセキュリティ設定
    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/articles/{id}", "/tags", "/users/{username}",
                        "/login", "/signup", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/articles/new", "/articles/{id}/edit", "/articles/{id}/delete",
                        "/articles/{id}/comments", "/comments/{id}/delete").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?logout")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

### 7-2. コードの解説

#### `@Order`アノテーション
```java
@Bean
@Order(1)
public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http)
```
- 複数の`SecurityFilterChain`を定義する場合、優先順位を指定
- 数字が小さいほど優先度が高い

#### `securityMatcher`
```java
.securityMatcher("/api/**")
```
- このFilterChainが適用されるURLパターンを指定
- `/api/**`に一致するリクエストにのみ適用

#### セッション管理の違い
- **REST API**: `SessionCreationPolicy.STATELESS`（セッションを使わない）
- **Web画面**: デフォルト（セッションを使用）

---

## ✅ ステップ8: 動作確認

### 8-1. アプリケーションの起動

```bash
cd ~/git/spring-boot-curriculum/workspace/bloghub
./mvnw clean install
./mvnw spring-boot:run
```

### 8-2. ブラウザでの確認

1. **トップページにアクセス**: `http://localhost:8080/`
   - 記事一覧が表示されること
   - タグフィルターが動作すること
   - ページネーションが表示されること

2. **記事詳細ページ**: 記事タイトルをクリック
   - 記事の詳細が表示されること
   - コメントが表示されること
   - ログインしていない場合はコメントフォームが非表示

3. **新規登録**: 右上の「新規登録」をクリック
   - ユーザー名、メール、パスワードを入力
   - バリデーションエラーが表示されること
   - 登録後、ログインページにリダイレクト

4. **ログイン**: ユーザー名とパスワードでログイン
   - ログイン成功後、トップページにリダイレクト
   - ナビゲーションバーに「記事を書く」ボタンが表示

5. **記事投稿**: 「記事を書く」をクリック
   - タイトル、本文、タグを入力
   - 投稿後、記事詳細ページにリダイレクト

6. **コメント投稿**: 記事詳細ページでコメント入力
   - コメントが即座に表示されること

7. **記事編集**: 自分の記事の「編集」ボタンをクリック
   - フォームに既存データが入っていること
   - 更新後、記事詳細ページにリダイレクト

8. **記事削除**: 自分の記事の「削除」ボタンをクリック
   - 確認ダイアログが表示されること
   - 削除後、トップページにリダイレクト

---

## 🐛 トラブルシューティング

### エラー: "Circular view path [error]"

**原因**: エラーページがテンプレートとして解決されようとしている

**解決策**: `application.yml`にエラーページの設定を追加:

```yaml
server:
  error:
    whitelabel:
      enabled: true
```

### エラー: "Template might not exist or might not be accessible"

**原因**: テンプレートファイルが見つからない

**解決策**:
1. ファイルパスを確認: `src/main/resources/templates/...`
2. ファイル名とController内の文字列が一致しているか確認
3. `th:fragment`の名前が正しいか確認

### エラー: CSRFトークンエラーでログインできない

**原因**: CSRFトークンが正しく送信されていない

**解決策**: フォームに`th:action`を使用していることを確認:

```html
<!-- ❌ 悪い例 -->
<form action="/login" method="post">

<!-- ✅ 良い例 -->
<form th:action="@{/login}" method="post">
```

### エラー: 日本語が文字化けする

**原因**: テンプレートのエンコーディングが正しくない

**解決策**: `application.yml`に以下を追加:

```yaml
spring:
  thymeleaf:
    encoding: UTF-8
```

### エラー: スタイルが適用されない

**原因**: CSSファイルのパスが間違っている、またはSecurityConfigで許可されていない

**解決策**:
1. パスを確認: `/css/style.css`は`src/main/resources/static/css/style.css`に配置
2. SecurityConfigで`/css/**`を許可

### エラー: cannot find symbol - method builder()

**原因**: DTOクラスに`@Builder`アノテーションが不足している

**解決策**: 以下のDTOクラスに`@Builder`アノテーションを追加:

```java
import lombok.Builder;
import lombok.Data;

@Data
@Builder  // これを追加
public class ArticleCreateRequest {
    // ...
}
```

対象クラス:
- `ArticleCreateRequest`
- `ArticleUpdateRequest`
- `CommentCreateRequest`
- `SignupRequest`

### エラー: cannot find symbol - class NegatedRequestMatcher

**原因**: SecurityConfigに必要なインポート文が不足している

**解決策**: 以下のインポートを追加:

```java
import org.springframework.security.config.Customizer;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
```

### エラー: sec:authorize属性が認識されない

**原因**: Thymeleaf Security名前空間の宣言が不足、または依存関係が不足

**解決策1**: HTMLファイルに名前空間を追加:

```html
<html xmlns:th="http://www.thymeleaf.org" 
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
```

**解決策2**: `pom.xml`に依存関係を確認:

```xml
<dependency>
    <groupId>org.thymeleaf.extras</groupId>
    <artifactId>thymeleaf-extras-springsecurity6</artifactId>
</dependency>
```

### エラー: Port 8080 was already in use

**原因**: 既に別のSpring Bootプロセスがポート8080を使用している

**解決策**: 既存プロセスを停止してから起動:

```bash
# プロセスを特定して終了
lsof -ti:8080 | xargs kill -9

# または全てのSpring Bootプロセスを終了
pkill -f "spring-boot"

# 再度起動
./mvnw spring-boot:run
```

---

## 🎨 チャレンジ課題

基本が理解できたら、以下にチャレンジしてみましょう：

### チャレンジ 1: マークダウンプレビュー機能

記事投稿フォームにリアルタイムプレビュー機能を追加しましょう。

**ヒント**:
- JavaScriptライブラリ「marked.js」を使用
- テキストエリアの内容が変更されたら、プレビューエリアを更新

```html
<script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
<script>
document.getElementById('content').addEventListener('input', function() {
    const markdown = this.value;
    const html = marked.parse(markdown);
    document.getElementById('preview').innerHTML = html;
});
</script>
```

### チャレンジ 2: ダークモード切り替え

ナビゲーションバーにダークモード切り替えボタンを追加しましょう。

**ヒント**:
- LocalStorageに設定を保存
- BootstrapのDark Modeクラスを使用

```javascript
const toggleDarkMode = () => {
    document.body.classList.toggle('dark-mode');
    localStorage.setItem('darkMode', document.body.classList.contains('dark-mode'));
};
```

### チャレンジ 3: いいね機能（フロントエンド）

記事に「いいね」ボタンを追加し、Ajaxで非同期にいいね数を更新しましょう。

**ヒント**:
- REST API: `POST /api/articles/{id}/like`
- JavaScriptの`fetch`でAPI呼び出し
- DOMを更新していいね数を表示

---

## 📚 このステップで学んだこと

- ✅ Thymeleafテンプレートエンジンの基本構文（`th:text`, `th:each`, `th:if`など）
- ✅ レイアウトテンプレートで共通部品を再利用する方法
- ✅ Spring Securityとの統合（`sec:authorize`, `sec:authentication`）
- ✅ フォームバインディング（`th:object`, `th:field`）とバリデーションエラー表示
- ✅ ページネーションの実装
- ✅ フラッシュメッセージの表示
- ✅ REST APIとWeb画面で異なるセキュリティ設定を適用する方法
- ✅ フォームログインとJWT認証の併用
- ✅ 日時フォーマットや文字列操作などのThymeleafユーティリティ
- ✅ BootstrapとFont Awesomeを使ったモダンなUIデザイン

---

## 💡 補足: ThymeleafとReact/Vueの使い分け

### Thymeleafが適しているケース

- **SEOが重要**: サーバーサイドレンダリングで初期HTMLが完全
- **シンプルなUI**: 複雑なインタラクションが少ない
- **プロトタイプ開発**: すぐに動くUIが欲しい
- **チーム構成**: バックエンドエンジニアのみ

### React/Vueが適しているケース

- **リッチなUI**: 複雑な状態管理やアニメーション
- **モバイルアプリとの共通化**: REST APIを共有
- **リアルタイム更新**: WebSocketやSSEとの連携
- **フロントエンド専門チーム**: 分業しやすい

**BlogHubの場合**: 両方のアプローチを提供することで、柔軟な選択肢を実現しています！

---

## ➡️ 次のステップ

[Step 39: テストとデプロイ準備](STEP_39.md)へ進みましょう！

次は、ユニットテストと統合テストを実装し、JaCoCoでテストカバレッジを測定します。また、本番環境向けの設定とDockerコンテナ化を行い、デプロイ可能な状態に仕上げます。
