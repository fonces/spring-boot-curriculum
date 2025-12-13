# Step 23: レイアウトとフラグメント

## 🎯 このステップの目標

- Thymeleafフラグメントで共通部品を作成できる
- `th:fragment`と`th:replace`で部品を再利用できる
- レイアウトテンプレートでページ全体の構造を統一できる
- パラメータ付きフラグメントで柔軟な部品化ができる

**所要時間**: 約45分

---

## 📋 事前準備

- [Step 22: フォーム送信とバリデーション](STEP_22.md)が完了していること
- HTMLの構造（header, nav, main, footer）を理解していること

---

## 🎓 フラグメントとレイアウトの必要性

### 問題: コードの重複

**各ページで同じコードを書く**:
```html
<!-- users/list.html -->
<head>
    <meta charset="UTF-8">
    <title>ユーザー一覧</title>
    <style>/* 共通スタイル */</style>
</head>

<!-- users/detail.html -->
<head>
    <meta charset="UTF-8">
    <title>ユーザー詳細</title>
    <style>/* 同じスタイル */</style>
</head>
```

**問題点**:
- 同じコードが複数ファイルに散らばる
- 修正時にすべてのファイルを変更する必要がある
- ヘッダー/フッターの一貫性を保つのが困難

### 解決策: フラグメントとレイアウト

```
layouts/
├── base.html          # 基本レイアウト
└── fragments/
    ├── header.html    # ヘッダー部品
    ├── nav.html       # ナビゲーション部品
    └── footer.html    # フッター部品
```

---

## 🚀 ステップ1: 共通フラグメントの作成

### 1-1. ディレクトリ作成

```bash
mkdir -p src/main/resources/templates/layouts/fragments
```

### 1-2. ヘッダーフラグメント

`src/main/resources/templates/layouts/fragments/header.html`を作成:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
</head>
<body>
    <!-- ヘッダーフラグメント -->
    <header th:fragment="header">
        <div class="header-container">
            <h1 class="logo">
                <a th:href="@{/views/users}">Spring Boot App</a>
            </h1>
            <nav class="main-nav">
                <ul>
                    <li><a th:href="@{/views/users}">ユーザー一覧</a></li>
                    <li><a th:href="@{/views/users/new}">新規登録</a></li>
                    <li><a th:href="@{/views/hello}">Hello</a></li>
                </ul>
            </nav>
        </div>
    </header>
    
    <style>
        .header-container {
            background-color: #4CAF50;
            color: white;
            padding: 1rem 2rem;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .logo a {
            color: white;
            text-decoration: none;
            font-size: 1.5rem;
        }
        .main-nav ul {
            list-style: none;
            display: flex;
            gap: 2rem;
            margin: 0;
            padding: 0;
        }
        .main-nav a {
            color: white;
            text-decoration: none;
        }
        .main-nav a:hover {
            text-decoration: underline;
        }
    </style>
</body>
</html>
```

### 1-3. フッターフラグメント

`src/main/resources/templates/layouts/fragments/footer.html`を作成:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
</head>
<body>
    <!-- フッターフラグメント -->
    <footer th:fragment="footer">
        <div class="footer-container">
            <p>&copy; 2024 Spring Boot Application. All rights reserved.</p>
            <p>Powered by Spring Boot 3.5.8 & Thymeleaf</p>
        </div>
    </footer>
    
    <style>
        .footer-container {
            background-color: #333;
            color: white;
            text-align: center;
            padding: 2rem;
            margin-top: 3rem;
        }
        .footer-container p {
            margin: 0.5rem 0;
        }
    </style>
</body>
</html>
```

### 1-4. フラグメント構文の解説

#### `th:fragment="fragmentName"`
```html
<header th:fragment="header">
    <!-- ヘッダーの内容 -->
</header>
```
- フラグメントの定義
- `fragmentName`で他のテンプレートから参照可能
- 1つのファイルに複数のフラグメントを定義可能

---

## 🚀 ステップ2: フラグメントの使用

### 2-1. ユーザー一覧ページにフラグメントを適用

`users/list.html`を以下のように修正:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>ユーザー一覧</title>
    <style>
        body {
            margin: 0;
            font-family: Arial, sans-serif;
        }
        .container {
            max-width: 1000px;
            margin: 0 auto;
            padding: 2rem;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
        }
        th, td {
            border: 1px solid #ddd;
            padding: 12px;
            text-align: left;
        }
        th {
            background-color: #4CAF50;
            color: white;
        }
        tr:nth-child(even) {
            background-color: #f2f2f2;
        }
    </style>
</head>
<body>
    <!-- ヘッダーを挿入 -->
    <div th:replace="~{layouts/fragments/header :: header}"></div>
    
    <div class="container">
        <h1>ユーザー一覧</h1>
        
        <div th:if="${users.empty}" class="no-users">
            <p>ユーザーが登録されていません</p>
        </div>
        
        <table th:unless="${users.empty}">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>名前</th>
                    <th>メールアドレス</th>
                    <th>年齢</th>
                    <th>登録日時</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="user : ${users}">
                    <td th:text="${user.id}">1</td>
                    <td>
                        <a th:href="@{/views/users/{id}(id=${user.id})}" 
                           th:text="${user.name}">田中太郎</a>
                    </td>
                    <td th:text="${user.email}">tanaka@example.com</td>
                    <td th:text="${user.age}">25</td>
                    <td th:text="${#temporals.format(user.createdAt, 'yyyy-MM-dd HH:mm')}">2024-01-01 12:00</td>
                </tr>
            </tbody>
        </table>
        
        <p>合計: <span th:text="${users.size()}">0</span> 件</p>
    </div>
    
    <!-- フッターを挿入 -->
    <div th:replace="~{layouts/fragments/footer :: footer}"></div>
</body>
</html>
```

### 2-2. フラグメント挿入構文の解説

#### `th:replace`
```html
<div th:replace="~{layouts/fragments/header :: header}"></div>
```
- **フラグメント式**: `~{テンプレートパス :: フラグメント名}`
- `th:replace`: 要素ごと置き換える（`<div>`タグも置換される）
- パス: `templates/`からの相対パス（拡張子`.html`は省略可能）

#### `th:insert` vs `th:replace`

```html
<!-- 元のHTML -->
<div th:insert="~{fragments/header :: header}">挿入先</div>
<div th:replace="~{fragments/header :: header}">置換先</div>

<!-- 結果 -->
<!-- th:insert: 子要素として挿入 -->
<div>
    <header>ヘッダーの内容</header>
</div>

<!-- th:replace: 要素ごと置換 -->
<header>ヘッダーの内容</header>
```

**推奨**: 通常は`th:replace`を使用（余分なタグが増えない）

---

## 🚀 ステップ3: 共通レイアウトの作成

### 3-1. 基本レイアウトテンプレート

`src/main/resources/templates/layouts/base.html`を作成:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" th:fragment="layout(title, content)">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:replace="${title}">Default Title</title>
    
    <!-- 共通CSS -->
    <style>
        * {
            box-sizing: border-box;
        }
        body {
            margin: 0;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: #f5f5f5;
        }
        .container {
            max-width: 1200px;
            margin: 0 auto;
            padding: 2rem;
            background-color: white;
            min-height: calc(100vh - 200px);
        }
        .alert {
            padding: 15px;
            margin-bottom: 20px;
            border-radius: 4px;
        }
        .alert-success {
            background-color: #d4edda;
            border: 1px solid #c3e6cb;
            color: #155724;
        }
        .btn {
            padding: 10px 20px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            text-decoration: none;
            display: inline-block;
        }
        .btn-primary {
            background-color: #4CAF50;
            color: white;
        }
        .btn-secondary {
            background-color: #999;
            color: white;
        }
    </style>
</head>
<body>
    <!-- ヘッダー -->
    <div th:replace="~{layouts/fragments/header :: header}"></div>
    
    <!-- メインコンテンツ -->
    <div class="container">
        <!-- フラッシュメッセージ -->
        <div th:if="${message}" class="alert alert-success">
            <p th:text="${message}">メッセージ</p>
        </div>
        
        <!-- ページ固有のコンテンツ -->
        <div th:replace="${content}">
            <p>コンテンツがここに入ります</p>
        </div>
    </div>
    
    <!-- フッター -->
    <div th:replace="~{layouts/fragments/footer :: footer}"></div>
</body>
</html>
```

### 3-2. レイアウトを使用したページ

`users/list.html`をレイアウトベースに書き換えます:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{layouts/base :: layout(~{::title}, ~{::main})}">
<head>
    <title>ユーザー一覧</title>
</head>
<body>
    <main>
        <h1>ユーザー一覧</h1>
        
        <div th:if="${users.empty}" style="text-align: center; padding: 40px; color: #999;">
            <p>ユーザーが登録されていません</p>
        </div>
        
        <table th:unless="${users.empty}" style="width: 100%; border-collapse: collapse; margin-top: 20px;">
            <thead>
                <tr>
                    <th style="border: 1px solid #ddd; padding: 12px; background-color: #4CAF50; color: white;">ID</th>
                    <th style="border: 1px solid #ddd; padding: 12px; background-color: #4CAF50; color: white;">名前</th>
                    <th style="border: 1px solid #ddd; padding: 12px; background-color: #4CAF50; color: white;">メールアドレス</th>
                    <th style="border: 1px solid #ddd; padding: 12px; background-color: #4CAF50; color: white;">年齢</th>
                    <th style="border: 1px solid #ddd; padding: 12px; background-color: #4CAF50; color: white;">登録日時</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="user, iterStat : ${users}" 
                    th:style="${iterStat.even} ? 'background-color: #f2f2f2;' : ''">
                    <td style="border: 1px solid #ddd; padding: 12px;" th:text="${user.id}">1</td>
                    <td style="border: 1px solid #ddd; padding: 12px;">
                        <a th:href="@{/views/users/{id}(id=${user.id})}" 
                           th:text="${user.name}">田中太郎</a>
                    </td>
                    <td style="border: 1px solid #ddd; padding: 12px;" th:text="${user.email}">tanaka@example.com</td>
                    <td style="border: 1px solid #ddd; padding: 12px;" th:text="${user.age}">25</td>
                    <td style="border: 1px solid #ddd; padding: 12px;" 
                        th:text="${#temporals.format(user.createdAt, 'yyyy-MM-dd HH:mm')}">2024-01-01 12:00</td>
                </tr>
            </tbody>
        </table>
        
        <p>合計: <span th:text="${users.size()}">0</span> 件</p>
    </main>
</body>
</html>
```

### 3-3. レイアウト構文の解説

#### パラメータ付きフラグメント
```html
th:fragment="layout(title, content)"
```
- フラグメントにパラメータを渡せる
- `title`と`content`をプレースホルダーとして受け取る

#### フラグメント式でセレクタ指定
```html
th:replace="~{layouts/base :: layout(~{::title}, ~{::main})}"
```
- `~{::title}`: 現在のテンプレートの`<title>`要素
- `~{::main}`: 現在のテンプレートの`<main>`要素
- これらを`layout`フラグメントのパラメータとして渡す

**動作**:
1. `users/list.html`の`<title>`タグを抽出
2. `users/list.html`の`<main>`タグを抽出
3. `layouts/base.html`の`${title}`と`${content}`に挿入

---

## 🚀 ステップ4: パラメータ付きフラグメント

### 4-1. ボタンコンポーネントフラグメント

`src/main/resources/templates/layouts/fragments/components.html`を作成:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
</head>
<body>
    <!-- ボタンコンポーネント -->
    <a th:fragment="button(url, text, type)" 
       th:href="${url}" 
       th:classappend="'btn btn-' + ${type}"
       th:text="${text}">
        Button
    </a>
    
    <!-- カードコンポーネント -->
    <div th:fragment="card(title, content)" class="card">
        <div class="card-header">
            <h3 th:text="${title}">タイトル</h3>
        </div>
        <div class="card-body" th:utext="${content}">
            コンテンツ
        </div>
    </div>
    
    <style>
        .card {
            border: 1px solid #ddd;
            border-radius: 8px;
            margin-bottom: 20px;
            overflow: hidden;
        }
        .card-header {
            background-color: #f5f5f5;
            padding: 15px;
            border-bottom: 1px solid #ddd;
        }
        .card-header h3 {
            margin: 0;
        }
        .card-body {
            padding: 20px;
        }
    </style>
</body>
</html>
```

### 4-2. パラメータ付きフラグメントの使用

`users/detail.html`を修正:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{layouts/base :: layout(~{::title}, ~{::main})}">
<head>
    <title>ユーザー詳細</title>
</head>
<body>
    <main>
        <h1>ユーザー詳細</h1>
        
        <!-- カードコンポーネントを使用 -->
        <div th:replace="~{layouts/fragments/components :: card(
            'ユーザー情報',
            ~{::user-info}
        )}">
            <div th:fragment="user-info">
                <p><strong>ID:</strong> <span th:text="${user.id}">1</span></p>
                <p><strong>名前:</strong> <span th:text="${user.name}">田中太郎</span></p>
                <p><strong>メールアドレス:</strong> <span th:text="${user.email}">tanaka@example.com</span></p>
                <p><strong>年齢:</strong> <span th:text="${user.age}">25</span></p>
                <p><strong>登録日時:</strong> 
                   <span th:text="${#temporals.format(user.createdAt, 'yyyy-MM-dd HH:mm:ss')}">2024-01-01 12:00:00</span>
                </p>
            </div>
        </div>
        
        <!-- ボタンコンポーネントを使用 -->
        <div style="margin-top: 20px;">
            <span th:replace="~{layouts/fragments/components :: button(
                @{/views/users/{id}/edit(id=${user.id})},
                '編集',
                'primary'
            )}"></span>
            
            <span th:replace="~{layouts/fragments/components :: button(
                @{/views/users},
                '一覧に戻る',
                'secondary'
            )}"></span>
        </div>
    </main>
</body>
</html>
```

---

## 🚀 ステップ5: 条件付きフラグメント

### 5-1. アラートコンポーネント

`components.html`にアラートフラグメントを追加:

```html
<!-- アラートコンポーネント -->
<div th:fragment="alert(type, message)" 
     th:if="${message != null and !message.isEmpty()}"
     th:classappend="'alert alert-' + ${type}">
    <p th:text="${message}">メッセージ</p>
</div>
```

### 5-2. 複数メッセージタイプに対応したレイアウト

`layouts/base.html`のメッセージ部分を修正:

```html
<div class="container">
    <!-- 成功メッセージ -->
    <div th:replace="~{layouts/fragments/components :: alert('success', ${successMessage})}"></div>
    
    <!-- エラーメッセージ -->
    <div th:replace="~{layouts/fragments/components :: alert('danger', ${errorMessage})}"></div>
    
    <!-- 情報メッセージ -->
    <div th:replace="~{layouts/fragments/components :: alert('info', ${infoMessage})}"></div>
    
    <!-- ページ固有のコンテンツ -->
    <div th:replace="${content}">
        <p>コンテンツがここに入ります</p>
    </div>
</div>
```

CSS追加:

```css
.alert-danger {
    background-color: #f8d7da;
    border: 1px solid #f5c2c7;
    color: #842029;
}
.alert-info {
    background-color: #d1ecf1;
    border: 1px solid #b9dadf;
    color: #055160;
}
```

---

## ✅ 動作確認

### 1. レイアウトの適用確認

ブラウザで以下にアクセス:
```
http://localhost:8080/views/users
```

**期待される結果**:
- 緑色のヘッダーが表示される
- ナビゲーションメニューが表示される
- フッターが表示される
- ページ全体が統一されたレイアウトになっている

### 2. フラグメントの再利用確認

複数のページ（一覧、詳細、フォーム）にアクセス:
```
http://localhost:8080/views/users
http://localhost:8080/views/users/1
http://localhost:8080/views/users/new
```

**期待される結果**:
- すべてのページで同じヘッダー/フッターが表示される
- ナビゲーションメニューが共通

### 3. パラメータ付きコンポーネントの確認

詳細画面で「編集」と「一覧に戻る」ボタンを確認:

**期待される結果**:
- ボタンの色が異なる（primary: 緑、secondary: グレー）
- ボタンのスタイルが統一されている

---

## 🎨 チャレンジ課題

### チャレンジ 1: パンくずリスト

パンくずリスト（breadcrumb）コンポーネントを作成してみましょう。

**ヒント**:
```html
<nav th:fragment="breadcrumb(items)">
    <ol>
        <li th:each="item, iterStat : ${items}">
            <a th:if="${!iterStat.last}" 
               th:href="${item.url}" 
               th:text="${item.text}"></a>
            <span th:if="${iterStat.last}" th:text="${item.text}"></span>
        </li>
    </ol>
</nav>
```

### チャレンジ 2: サイドバー付きレイアウト

2カラムレイアウト（メインコンテンツ + サイドバー）を作成してみましょう。

**ヒント**:
```html
th:fragment="layout-with-sidebar(title, main, sidebar)"
```

### チャレンジ 3: テーブルコンポーネント

データテーブルを共通化してみましょう。

**ヒント**:
- ヘッダー配列とデータリストをパラメータで受け取る
- `th:each`でヘッダーとデータを動的に生成

---

## 🐛 トラブルシューティング

### エラー: "Error resolving template fragment"

**原因**: フラグメント名やパスが間違っている

**解決策**:
```html
<!-- NG: スペルミス -->
<div th:replace="~{layouts/fragments/header :: hedar}"></div>

<!-- OK -->
<div th:replace="~{layouts/fragments/header :: header}"></div>
```

### レイアウトが適用されない

**原因**: `th:replace`の位置が間違っている

**解決策**:
```html
<!-- NG: bodyタグに配置 -->
<body th:replace="~{layouts/base :: layout(...)}">

<!-- OK: htmlタグに配置 -->
<html th:replace="~{layouts/base :: layout(...)}">
```

### パラメータが渡らない

**原因**: フラグメント式の構文エラー

**解決策**:
```html
<!-- NG: クォートが不足 -->
<div th:replace="~{components :: button(@{/users}, 編集, primary)}"></div>

<!-- OK: 文字列は引用符で囲む -->
<div th:replace="~{components :: button(@{/users}, '編集', 'primary')}"></div>
```

### CSSが適用されない

**原因**: フラグメント内のCSSがスコープ外

**解決策**:
1. 共通CSSは`layouts/base.html`に配置
2. または、外部CSSファイルに分離

```html
<!-- static/css/style.css -->
<link rel="stylesheet" th:href="@{/css/style.css}">
```

---

## 📚 このステップで学んだこと

- ✅ `th:fragment`でフラグメント（部品）を定義
- ✅ `th:replace`でフラグメントを挿入
- ✅ パラメータ付きフラグメントで柔軟な部品化
- ✅ レイアウトテンプレートでページ構造を統一
- ✅ フラグメント式`~{...}`の使い方
- ✅ 共通コンポーネント（ボタン、カード、アラート）の作成

---

## 💡 補足: フラグメントのベストプラクティス

### ディレクトリ構成

```
templates/
├── layouts/
│   ├── base.html              # 基本レイアウト
│   ├── base-admin.html        # 管理画面用レイアウト
│   └── fragments/
│       ├── header.html        # ヘッダー
│       ├── footer.html        # フッター
│       └── components.html    # 再利用可能な部品
├── users/
│   ├── list.html
│   ├── detail.html
│   └── form.html
└── errors/
    ├── 404.html
    └── 500.html
```

### 命名規則

- **レイアウト**: `base-*.html`
- **フラグメント**: 機能単位で分割（`header.html`, `nav.html`）
- **コンポーネント**: `components.html`にまとめる

### パフォーマンス

- フラグメントはキャッシュされる（開発時は`spring.thymeleaf.cache=false`で無効化）
- 過度なネストは避ける（可読性とパフォーマンス）

---

## ➡️ 次のステップ

[Step 24: Thymeleaf + REST API連携](STEP_24.md)へ進みましょう！

次のステップでは、ThymeleafとREST APIを組み合わせて、非同期通信（AJAX）を実装する方法を学びます。
