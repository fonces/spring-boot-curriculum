# Step 23: レイアウトとフラグメント

## 🎯 このステップの目標

- `th:fragment`で共通部品を定義する方法を学ぶ
- `th:replace`と`th:insert`でフラグメントを再利用する
- Thymeleaf Layout Dialectでレイアウトを構築する
- BootstrapなどのCSSフレームワークを統合する
- DRY原則に基づいたテンプレート設計を実践する

**所要時間**: 約2時間30分

---

## 📋 事前準備

- Step 21、22の完了
- HTMLとCSSの基礎知識
- Bootstrapの基本的な理解（オプション）

---

## 💡 なぜフラグメントが必要なのか？

### 問題: コードの重複

各ページで同じヘッダー・フッターをコピペ:

```html
<!-- users/list.html -->
<header>...</header>  <!-- 重複 -->
<main>ユーザー一覧</main>
<footer>...</footer>  <!-- 重複 -->

<!-- products/list.html -->
<header>...</header>  <!-- 重複 -->
<main>商品一覧</main>
<footer>...</footer>  <!-- 重複 -->
```

**問題点**:
- ヘッダーを変更すると全ページ修正が必要
- メンテナンス性が悪い
- ミスが発生しやすい

### 解決: フラグメント化

```html
<!-- fragments/layout.html -->
<header th:fragment="header">...</header>
<footer th:fragment="footer">...</footer>

<!-- users/list.html -->
<div th:replace="~{fragments/layout :: header}"></div>
<main>ユーザー一覧</main>
<div th:replace="~{fragments/layout :: footer}"></div>
```

**メリット**:
- 1箇所修正すれば全ページに反映
- DRY原則（Don't Repeat Yourself）
- 保守性向上

---

## 🏗️ 実装手順

### Step 1: 基本的なフラグメントの作成

`src/main/resources/templates/fragments/layout.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head th:fragment="head(title)">
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <!-- 動的にタイトルを設定 -->
    <title th:text="${title}">デフォルトタイトル</title>
    
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" 
          rel="stylesheet">
    
    <!-- カスタムCSS -->
    <style>
        body {
            min-height: 100vh;
            display: flex;
            flex-direction: column;
        }
        main {
            flex: 1;
        }
        footer {
            margin-top: auto;
        }
    </style>
</head>

<body>
    <!-- ヘッダー部分 -->
    <header th:fragment="header">
        <nav class="navbar navbar-expand-lg navbar-dark bg-primary">
            <div class="container">
                <a class="navbar-brand" th:href="@{/}">
                    Spring Boot カリキュラム
                </a>
                <button class="navbar-toggler" type="button" 
                        data-bs-toggle="collapse" data-bs-target="#navbarNav">
                    <span class="navbar-toggler-icon"></span>
                </button>
                <div class="collapse navbar-collapse" id="navbarNav">
                    <ul class="navbar-nav ms-auto">
                        <li class="nav-item">
                            <a class="nav-link" th:href="@{/}">ホーム</a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link" th:href="@{/users}">ユーザー管理</a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link" th:href="@{/api/products}">商品管理</a>
                        </li>
                    </ul>
                </div>
            </div>
        </nav>
    </header>
    
    <!-- フッター部分 -->
    <footer th:fragment="footer" class="bg-dark text-white py-4 mt-5">
        <div class="container text-center">
            <p class="mb-0">&copy; 2025 Spring Boot Curriculum. All rights reserved.</p>
            <p class="mb-0 small">
                Powered by 
                <a href="https://spring.io/" target="_blank" class="text-white">Spring Boot</a> &amp; 
                <a href="https://www.thymeleaf.org/" target="_blank" class="text-white">Thymeleaf</a>
            </p>
        </div>
    </footer>
    
    <!-- Bootstrap JS -->
    <script th:fragment="scripts">
        <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    </script>
</body>
</html>
```

### Step 2: アラートメッセージのフラグメント

`src/main/resources/templates/fragments/messages.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
    <!-- 成功メッセージ -->
    <div th:fragment="success" th:if="${successMessage}">
        <div class="alert alert-success alert-dismissible fade show" role="alert">
            <i class="bi bi-check-circle-fill"></i>
            <span th:text="${successMessage}">成功メッセージ</span>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </div>
    
    <!-- エラーメッセージ -->
    <div th:fragment="error" th:if="${errorMessage}">
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            <i class="bi bi-exclamation-triangle-fill"></i>
            <span th:text="${errorMessage}">エラーメッセージ</span>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </div>
    
    <!-- 情報メッセージ -->
    <div th:fragment="info" th:if="${infoMessage}">
        <div class="alert alert-info alert-dismissible fade show" role="alert">
            <i class="bi bi-info-circle-fill"></i>
            <span th:text="${infoMessage}">情報メッセージ</span>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </div>
</body>
</html>
```

### Step 3: ページネーションのフラグメント

`src/main/resources/templates/fragments/pagination.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
    <!-- 
        パラメータ:
        - page: Pageオブジェクト
        - url: ベースURL
    -->
    <nav th:fragment="paging(page, url)" th:if="${page.totalPages > 1}">
        <ul class="pagination justify-content-center">
            <!-- 前へ -->
            <li class="page-item" th:classappend="${page.first} ? 'disabled'">
                <a class="page-link" 
                   th:href="${page.first} ? '#' : ${url + '?page=' + (page.number - 1)}">
                    前へ
                </a>
            </li>
            
            <!-- ページ番号 -->
            <li class="page-item" 
                th:each="i : ${#numbers.sequence(0, page.totalPages - 1)}"
                th:classappend="${i == page.number} ? 'active'">
                <a class="page-link" th:href="${url + '?page=' + i}" th:text="${i + 1}">1</a>
            </li>
            
            <!-- 次へ -->
            <li class="page-item" th:classappend="${page.last} ? 'disabled'">
                <a class="page-link" 
                   th:href="${page.last} ? '#' : ${url + '?page=' + (page.number + 1)}">
                    次へ
                </a>
            </li>
        </ul>
    </nav>
</body>
</html>
```

### Step 4: フラグメントを使ったページの作成

`src/main/resources/templates/users/list.html`を更新:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <!-- headフラグメントを使用（タイトルを渡す） -->
    <th:block th:replace="~{fragments/layout :: head('ユーザー一覧')}"></th:block>
</head>
<body>
    <!-- ヘッダー -->
    <div th:replace="~{fragments/layout :: header}"></div>
    
    <main class="container my-5">
        <h1 class="mb-4">ユーザー一覧</h1>
        
        <!-- メッセージ表示 -->
        <div th:replace="~{fragments/messages :: success}"></div>
        <div th:replace="~{fragments/messages :: error}"></div>
        
        <!-- 新規登録ボタン -->
        <div class="mb-3">
            <a th:href="@{/users/new}" class="btn btn-primary">
                <i class="bi bi-plus-circle"></i> 新規登録
            </a>
        </div>
        
        <!-- ユーザーテーブル -->
        <div class="card">
            <div class="card-body">
                <div th:if="${users.isEmpty()}" class="alert alert-info">
                    ユーザーが登録されていません。
                </div>
                
                <table th:unless="${users.isEmpty()}" class="table table-hover">
                    <thead class="table-light">
                        <tr>
                            <th>ID</th>
                            <th>名前</th>
                            <th>メールアドレス</th>
                            <th>登録日時</th>
                            <th>操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr th:each="user : ${users}">
                            <td th:text="${user.id}">1</td>
                            <td th:text="${user.name}">山田太郎</td>
                            <td th:text="${user.email}">yamada@example.com</td>
                            <td th:text="${#temporals.format(user.createdAt, 'yyyy-MM-dd HH:mm')}">
                                2025-10-29 10:00
                            </td>
                            <td>
                                <a th:href="@{/users/{id}(id=${user.id})}" 
                                   class="btn btn-sm btn-info">詳細</a>
                                <a th:href="@{/users/{id}/edit(id=${user.id})}" 
                                   class="btn btn-sm btn-warning">編集</a>
                                <form th:action="@{/users/{id}/delete(id=${user.id})}" 
                                      method="post" 
                                      style="display: inline;"
                                      onsubmit="return confirm('本当に削除しますか？');">
                                    <button type="submit" class="btn btn-sm btn-danger">削除</button>
                                </form>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </main>
    
    <!-- フッター -->
    <div th:replace="~{fragments/layout :: footer}"></div>
    
    <!-- JavaScript -->
    <div th:replace="~{fragments/layout :: scripts}"></div>
</body>
</html>
```

### Step 5: Thymeleaf Layout Dialectを使った高度なレイアウト

`src/main/resources/templates/layouts/default.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title layout:title-pattern="$CONTENT_TITLE - $LAYOUT_TITLE">Spring Boot App</title>
    
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" 
          rel="stylesheet">
    <link rel="stylesheet" 
          href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
    
    <!-- ページ固有のCSS -->
    <th:block layout:fragment="extra-css"></th:block>
</head>
<body>
    <!-- ヘッダー -->
    <div th:replace="~{fragments/layout :: header}"></div>
    
    <!-- メインコンテンツ（各ページで置き換え） -->
    <main class="container my-5" layout:fragment="content">
        <p>ここにコンテンツが入ります</p>
    </main>
    
    <!-- フッター -->
    <div th:replace="~{fragments/layout :: footer}"></div>
    
    <!-- Bootstrap JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    
    <!-- ページ固有のJS -->
    <th:block layout:fragment="extra-js"></th:block>
</body>
</html>
```

### Step 6: レイアウトを使ったページ

`src/main/resources/templates/users/list-with-layout.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layouts/default}">
<head>
    <title>ユーザー一覧</title>
    
    <!-- ページ固有のCSS -->
    <th:block layout:fragment="extra-css">
        <style>
            .user-table {
                box-shadow: 0 0 10px rgba(0,0,0,0.1);
            }
        </style>
    </th:block>
</head>
<body>
    <!-- contentフラグメントの内容がレイアウトのmainに挿入される -->
    <div layout:fragment="content">
        <h1 class="mb-4">ユーザー一覧</h1>
        
        <div th:replace="~{fragments/messages :: success}"></div>
        
        <div class="mb-3">
            <a th:href="@{/users/new}" class="btn btn-primary">
                <i class="bi bi-plus-circle"></i> 新規登録
            </a>
        </div>
        
        <div class="card user-table">
            <div class="card-body">
                <table class="table table-hover mb-0">
                    <thead class="table-light">
                        <tr>
                            <th>ID</th>
                            <th>名前</th>
                            <th>メールアドレス</th>
                            <th>操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr th:each="user : ${users}">
                            <td th:text="${user.id}">1</td>
                            <td th:text="${user.name}">山田太郎</td>
                            <td th:text="${user.email}">email@example.com</td>
                            <td>
                                <a th:href="@{/users/{id}(id=${user.id})}" 
                                   class="btn btn-sm btn-info">詳細</a>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
    
    <!-- ページ固有のJavaScript -->
    <th:block layout:fragment="extra-js">
        <script>
            console.log('ユーザー一覧ページが読み込まれました');
        </script>
    </th:block>
</body>
</html>
```

---

## 🔍 フラグメントの構文

### 1. フラグメントの定義

```html
<!-- 基本 -->
<div th:fragment="fragmentName">内容</div>

<!-- パラメータ付き -->
<div th:fragment="greeting(name)">
    <p th:text="'こんにちは、' + ${name} + 'さん'"></p>
</div>
```

### 2. フラグメントの使用

```html
<!-- th:replace: 要素ごと置き換え -->
<div th:replace="~{fragments/layout :: header}"></div>
<!-- 結果: <header>...</header> -->

<!-- th:insert: 要素の中に挿入 -->
<div th:insert="~{fragments/layout :: header}"></div>
<!-- 結果: <div><header>...</header></div> -->

<!-- th:include: 内容だけコピー（非推奨） -->
<div th:include="~{fragments/layout :: header}"></div>
```

### 3. パラメータ渡し

```html
<!-- フラグメント定義 -->
<div th:fragment="card(title, content)">
    <div class="card">
        <h5 th:text="${title}"></h5>
        <p th:text="${content}"></p>
    </div>
</div>

<!-- 使用 -->
<div th:replace="~{fragments/card :: card('タイトル', '本文')}"></div>
```

### 4. セレクター式

```html
<!-- ID指定 -->
<div th:replace="~{fragments/layout :: #header}"></div>

<!-- クラス指定 -->
<div th:replace="~{fragments/layout :: .alert}"></div>

<!-- タグ指定 -->
<div th:replace="~{fragments/layout :: nav}"></div>
```

---

## 📊 th:replace vs th:insert の違い

| 記法 | 動作 | 使いどころ |
|------|------|-----------|
| `th:replace` | 要素ごと置き換え | ヘッダー、フッターなど |
| `th:insert` | 要素の中に挿入 | コンテンツを包む場合 |
| `th:include` | 内容だけコピー | **非推奨** |

**例**:

```html
<!-- フラグメント -->
<footer th:fragment="copy">
    &copy; 2025 Example
</footer>

<!-- th:replace -->
<div th:replace="~{:: copy}"></div>
<!-- 結果: <footer>&copy; 2025 Example</footer> -->

<!-- th:insert -->
<div th:insert="~{:: copy}"></div>
<!-- 結果: <div><footer>&copy; 2025 Example</footer></div> -->
```

---

## ✅ 動作確認

### 1. ユーザー一覧にアクセス

```
http://localhost:8080/users
```

確認ポイント:
- ヘッダーが表示されているか
- フッターが表示されているか
- Bootstrapが適用されているか

### 2. ソースコードを表示

ブラウザで「ページのソースを表示」:
- `th:fragment`, `th:replace`などのThymeleaf属性が消えているか
- フラグメントの内容が正しく展開されているか

### 3. 別のページでも同じレイアウト

商品一覧など他のページでも同じヘッダー・フッターが表示されることを確認

---

## 🎨 Bootstrap統合の例

### カード一覧

```html
<div class="row">
    <div class="col-md-4" th:each="product : ${products}">
        <div class="card mb-4">
            <div class="card-body">
                <h5 class="card-title" th:text="${product.name}">商品名</h5>
                <p class="card-text" th:text="${product.description}">説明</p>
                <p class="text-primary fw-bold" 
                   th:text="'¥' + ${#numbers.formatInteger(product.price, 0, 'COMMA')}">
                    ¥1,000
                </p>
                <a th:href="@{/products/{id}(id=${product.id})}" 
                   class="btn btn-primary">詳細</a>
            </div>
        </div>
    </div>
</div>
```

### モーダル

```html
<!-- モーダルトリガー -->
<button type="button" class="btn btn-primary" 
        data-bs-toggle="modal" data-bs-target="#deleteModal"
        th:attr="data-user-id=${user.id}, data-user-name=${user.name}">
    削除
</button>

<!-- モーダル本体 -->
<div class="modal fade" id="deleteModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">削除確認</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <p>本当に削除しますか？</p>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                    キャンセル
                </button>
                <form th:action="@{/users/{id}/delete(id=${user.id})}" method="post">
                    <button type="submit" class="btn btn-danger">削除</button>
                </form>
            </div>
        </div>
    </div>
</div>
```

---

## 📝 理解度チェック

1. **`th:fragment`の役割は何ですか？**
2. **`th:replace`と`th:insert`の違いは何ですか？**
3. **Thymeleaf Layout Dialectのメリットは何ですか？**
4. **フラグメントにパラメータを渡せますか？**
5. **Bootstrapなどの外部CSSをどう読み込みますか？**

---

## 💡 ベストプラクティス

1. **共通部品は積極的にフラグメント化**: DRY原則
2. **レイアウトは階層化**: layouts/default.html → fragments/header.html
3. **パラメータで柔軟に**: 再利用性を高める
4. **命名規則を統一**: fragments/, layouts/など
5. **Bootstrap活用**: 独自CSSは最小限に

---

## 🔄 Gitへのコミットとレビュー依頼

進捗を記録してレビューを受けましょう：

```bash
git add .
git commit -m "Step 23: レイアウトとフラグメント完了"
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

レビューが完了したら、[Step 24: Thymeleaf + REST API連携](STEP_24.md)へ進みましょう！

次のStep 24では、**Thymeleaf + REST API連携**を学びます:
- JavaScriptでREST APIを呼び出す
- 非同期でデータを取得・更新
- DOM操作で動的にページを更新
- ThymeleafとSPAの融合

---

お疲れ様でした！🎉 レイアウトとフラグメントをマスターできたら、次のステップに進みましょう。
