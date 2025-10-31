# Step 21: Thymeleafの基礎

## 🎯 このステップの目標

- Thymeleafテンプレートエンジンの基本を理解する
- `@Controller`と`@RestController`の違いを学ぶ
- Modelを使ったデータ渡しを実装する
- 基本的なテンプレート構文（`th:text`, `th:each`など）を習得する
- サーバーサイドレンダリングの仕組みを理解する

**所要時間**: 約2時間

---

## 📋 事前準備

- Phase 1〜4の完了
- JPAまたはMyBatisでのデータ取得の理解
- HTMLとCSSの基礎知識

---

## 💡 Thymeleafとは？

### サーバーサイドレンダリング（SSR）

**Thymeleaf**は、サーバー側でHTMLを生成するテンプレートエンジンです。

```
クライアント → サーバー（Spring Boot + Thymeleaf）
                ↓
         データベースからデータ取得
                ↓
         HTMLに埋め込んで生成
                ↓
クライアント ← 完成したHTML
```

### REST APIとの違い

| 項目 | REST API | Thymeleaf（SSR） |
|------|----------|------------------|
| **レスポンス** | JSON | HTML |
| **描画場所** | クライアント（JavaScript） | サーバー |
| **SEO** | 不利 | 有利 |
| **初回表示** | 遅い（JS読み込み必要） | 速い |
| **リッチなUI** | 得意（React等） | やや不得意 |

### 使い分け

- **REST API**: SPAアプリ、モバイルアプリ
- **Thymeleaf**: 管理画面、SEO重視のサイト、シンプルなWebアプリ

---

## 🏗️ 実装手順

### Step 1: Thymeleaf依存関係の追加

`pom.xml`に以下を追加:

```xml
<dependencies>
    <!-- 既存の依存関係 -->
    
    <!-- Thymeleaf -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
    
    <!-- Thymeleaf Layout Dialect（レイアウト機能） -->
    <dependency>
        <groupId>nz.net.ultraq.thymeleaf</groupId>
        <artifactId>thymeleaf-layout-dialect</artifactId>
    </dependency>
</dependencies>
```

### Step 2: application.ymlにThymeleaf設定を追加

```yaml
spring:
  # 既存の設定...
  
  thymeleaf:
    # テンプレートの場所
    prefix: classpath:/templates/
    suffix: .html
    # 開発時はキャッシュ無効化（本番はtrue）
    cache: false
    # 文字エンコーディング
    encoding: UTF-8
    # テンプレートモード
    mode: HTML
```

**設定の意味**:
- `prefix`: テンプレートファイルの配置場所
- `suffix`: テンプレートファイルの拡張子
- `cache: false`: 開発中は変更がすぐ反映されるように

### Step 3: 最初のテンプレートを作成

`src/main/resources/templates/hello.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Hello Thymeleaf</title>
</head>
<body>
    <h1>Hello, Thymeleaf!</h1>
    <p th:text="${message}">デフォルトメッセージ</p>
</body>
</html>
```

**ポイント**:
- `xmlns:th`: Thymeleafの名前空間宣言
- `th:text="${message}"`: サーバーから渡されたデータを表示
- タグ内のテキストはプレビュー用（実行時は上書きされる）

### Step 4: Controllerの作成

`src/main/java/com/example/demo/controller/HelloController.java`:

```java
package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller  // @RestControllerではない！
public class HelloController {
    
    @GetMapping("/hello")
    public String hello(Model model) {
        // Modelにデータを追加
        model.addAttribute("message", "Thymeleafへようこそ！");
        
        // テンプレート名を返す（templates/hello.html）
        return "hello";
    }
    
    @GetMapping("/greet")
    public String greet(@RequestParam(defaultValue = "ゲスト") String name, Model model) {
        model.addAttribute("message", "こんにちは、" + name + "さん！");
        return "hello";
    }
}
```

**重要な違い**:

```java
// REST API（JSONを返す）
@RestController
public class ApiController {
    @GetMapping("/api/data")
    public Map<String, String> getData() {
        return Map.of("message", "Hello");  // JSON
    }
}

// Thymeleaf（HTMLを返す）
@Controller
public class ViewController {
    @GetMapping("/page")
    public String getPage(Model model) {
        model.addAttribute("message", "Hello");
        return "page";  // HTMLテンプレート名
    }
}
```

### Step 5: 動作確認

アプリケーションを起動して、ブラウザでアクセス:

```
http://localhost:8080/hello
```

**表示されるHTML**:
```html
<h1>Hello, Thymeleaf!</h1>
<p>Thymeleafへようこそ！</p>
```

パラメータ付きでアクセス:
```
http://localhost:8080/greet?name=太郎
```

**表示**:
```html
<p>こんにちは、太郎さん！</p>
```

---

## 🎨 基本構文の学習

### Step 6: ユーザー一覧ページの作成

`src/main/resources/templates/users/list.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ユーザー一覧</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            max-width: 800px;
            margin: 50px auto;
            padding: 20px;
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
        .info {
            background-color: #e7f3fe;
            padding: 10px;
            border-left: 4px solid #2196F3;
            margin-bottom: 20px;
        }
    </style>
</head>
<body>
    <h1>ユーザー一覧</h1>
    
    <!-- th:if: 条件分岐 -->
    <div class="info" th:if="${users.isEmpty()}">
        <p>ユーザーが登録されていません。</p>
    </div>
    
    <!-- th:unless: 条件分岐（反対） -->
    <div th:unless="${users.isEmpty()}">
        <p>登録ユーザー数: <span th:text="${users.size()}">0</span>人</p>
        
        <!-- th:each: ループ -->
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>名前</th>
                    <th>メールアドレス</th>
                    <th>登録日時</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="user : ${users}">
                    <!-- th:text: テキスト表示 -->
                    <td th:text="${user.id}">1</td>
                    <td th:text="${user.name}">山田太郎</td>
                    <td th:text="${user.email}">yamada@example.com</td>
                    <!-- 日付フォーマット -->
                    <td th:text="${#temporals.format(user.createdAt, 'yyyy-MM-dd HH:mm')}">
                        2025-10-29 10:00
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
    
    <p style="margin-top: 30px;">
        <a href="/">トップページへ戻る</a>
    </p>
</body>
</html>
```

### Step 7: UserViewController の作成

`src/main/java/com/example/demo/controller/UserViewController.java`:

```java
package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserViewController {
    
    private final UserService userService;
    
    // ユーザー一覧
    @GetMapping
    public String listUsers(Model model) {
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        return "users/list";  // templates/users/list.html
    }
    
    // ユーザー詳細
    @GetMapping("/{id}")
    public String userDetail(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        return "users/detail";  // templates/users/detail.html
    }
}
```

### Step 8: ユーザー詳細ページ

`src/main/resources/templates/users/detail.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>ユーザー詳細</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            max-width: 600px;
            margin: 50px auto;
            padding: 20px;
        }
        .user-card {
            border: 1px solid #ddd;
            border-radius: 8px;
            padding: 20px;
            background-color: #f9f9f9;
        }
        .user-card h2 {
            margin-top: 0;
            color: #333;
        }
        .user-info {
            margin: 15px 0;
        }
        .label {
            font-weight: bold;
            color: #555;
            display: inline-block;
            width: 120px;
        }
        .value {
            color: #333;
        }
        .back-link {
            display: inline-block;
            margin-top: 20px;
            color: #4CAF50;
            text-decoration: none;
        }
        .back-link:hover {
            text-decoration: underline;
        }
    </style>
</head>
<body>
    <div class="user-card">
        <!-- th:text="${user.name}" でデータを表示 -->
        <h2 th:text="${user.name}">ユーザー名</h2>
        
        <div class="user-info">
            <span class="label">ID:</span>
            <span class="value" th:text="${user.id}">1</span>
        </div>
        
        <div class="user-info">
            <span class="label">メール:</span>
            <!-- th:href でリンクを動的生成 -->
            <a th:href="'mailto:' + ${user.email}" 
               th:text="${user.email}">
                email@example.com
            </a>
        </div>
        
        <div class="user-info">
            <span class="label">登録日時:</span>
            <span class="value" 
                  th:text="${#temporals.format(user.createdAt, 'yyyy年MM月dd日 HH:mm:ss')}">
                2025-10-29 10:00:00
            </span>
        </div>
        
        <div class="user-info">
            <span class="label">更新日時:</span>
            <span class="value" 
                  th:text="${#temporals.format(user.updatedAt, 'yyyy年MM月dd日 HH:mm:ss')}">
                2025-10-29 10:00:00
            </span>
        </div>
    </div>
    
    <!-- th:href="@{...}" でURLを生成 -->
    <a th:href="@{/users}" class="back-link">← ユーザー一覧へ戻る</a>
</body>
</html>
```

---

## 🎯 主要な構文まとめ

### 1. テキスト表示

```html
<!-- 基本 -->
<p th:text="${message}">デフォルト</p>

<!-- HTML エスケープなし（危険！XSS対策で通常は使わない） -->
<p th:utext="${htmlContent}"></p>

<!-- インライン記法 -->
<p>こんにちは、[[${name}]]さん！</p>
```

### 2. 属性の設定

```html
<!-- href属性 -->
<a th:href="@{/users/{id}(id=${user.id})}">詳細</a>
<!-- 結果: <a href="/users/123">詳細</a> -->

<!-- src属性 -->
<img th:src="@{/images/logo.png}" alt="ロゴ">

<!-- class属性 -->
<div th:class="${isActive} ? 'active' : 'inactive'">...</div>

<!-- 複数の属性 -->
<input type="text" th:value="${user.name}" th:readonly="${readOnly}">
```

### 3. 条件分岐

```html
<!-- th:if -->
<p th:if="${user != null}">ユーザーが存在します</p>

<!-- th:unless（ifの反対） -->
<p th:unless="${users.isEmpty()}">ユーザーがいます</p>

<!-- 三項演算子 -->
<p th:text="${age >= 20} ? '成人' : '未成年'"></p>

<!-- th:switch -->
<div th:switch="${user.role}">
    <p th:case="'ADMIN'">管理者</p>
    <p th:case="'USER'">一般ユーザー</p>
    <p th:case="*">その他</p>
</div>
```

### 4. ループ

```html
<!-- 基本的なループ -->
<tr th:each="user : ${users}">
    <td th:text="${user.name}"></td>
</tr>

<!-- インデックス付き -->
<tr th:each="user, stat : ${users}">
    <td th:text="${stat.index}"></td>  <!-- 0から -->
    <td th:text="${stat.count}"></td>  <!-- 1から -->
    <td th:text="${user.name}"></td>
    <td th:if="${stat.first}">最初</td>
    <td th:if="${stat.last}">最後</td>
    <td th:if="${stat.even}">偶数行</td>
</tr>
```

### 5. URL生成

```html
<!-- 基本 -->
<a th:href="@{/users}">ユーザー一覧</a>

<!-- パスパラメータ -->
<a th:href="@{/users/{id}(id=${user.id})}">詳細</a>
<!-- 結果: /users/123 -->

<!-- クエリパラメータ -->
<a th:href="@{/search(keyword=${keyword}, page=1)}">検索</a>
<!-- 結果: /search?keyword=test&page=1 -->

<!-- 複合 -->
<a th:href="@{/users/{id}/edit(id=${user.id}, mode='edit')}">編集</a>
<!-- 結果: /users/123/edit?mode=edit -->
```

### 6. フォーム

```html
<form th:action="@{/users}" method="post">
    <input type="text" name="name" th:value="${user.name}">
    <button type="submit">送信</button>
</form>
```

---

## ✅ 動作確認

### 1. ユーザー一覧ページ

ブラウザで以下にアクセス:
```
http://localhost:8080/users
```

### 2. ユーザー詳細ページ

```
http://localhost:8080/users/1
```

### 3. デベロッパーツールで確認

ブラウザのデベロッパーツール（F12）で、以下を確認:
- HTMLが正しく生成されているか
- Thymeleaf属性（`th:*`）は削除されているか
- データが正しく埋め込まれているか

---

## 🔍 よくある間違い

### ❌ 間違い1: @RestControllerを使用

```java
@RestController  // ← これだとJSONが返る！
public class UserController {
    @GetMapping("/users")
    public String listUsers(Model model) {
        return "users/list";  // "users/list" という文字列が返る
    }
}
```

### ✅ 正解: @Controllerを使用

```java
@Controller  // ← HTMLを返す
public class UserController {
    @GetMapping("/users")
    public String listUsers(Model model) {
        return "users/list";  // templates/users/list.html が返る
    }
}
```

### ❌ 間違い2: テンプレートの場所が違う

```
src/main/resources/static/users/list.html  ← 静的ファイル用（間違い）
```

### ✅ 正解: templatesディレクトリ

```
src/main/resources/templates/users/list.html  ← Thymeleaf用（正解）
```

### ❌ 間違い3: 名前空間の宣言忘れ

```html
<!DOCTYPE html>
<html>  <!-- xmlns:th がない -->
<body>
    <p th:text="${message}">...</p>  <!-- IDEで警告が出る -->
</body>
</html>
```

### ✅ 正解: 名前空間を宣言

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
    <p th:text="${message}">...</p>
</body>
</html>
```

---

## 📝 理解度チェック

1. **`@Controller`と`@RestController`の違いは何ですか？**
2. **Modelオブジェクトの役割は何ですか？**
3. **`th:text`と`th:utext`の違いは何ですか？**
4. **`@{...}`記法は何のために使いますか？**
5. **テンプレートファイルはどこに配置しますか？**

---

## 💡 ベストプラクティス

1. **セキュリティ**: `th:text`を使う（`th:utext`は避ける）
2. **可読性**: HTMLは静的にも見やすくする
3. **テンプレート名**: 階層構造を活用（users/list.html）
4. **エラーハンドリング**: null チェックを忘れずに
5. **キャッシュ**: 開発中は`cache: false`、本番は`true`

---

## 📚 このステップで学んだこと

このステップでは、Thymeleaf基礎について学びました：

- ✅ Thymeleafの基本概念とSpring Bootでの統合
- ✅ Thymeleafのテンプレート構文（th:text, th:attr等）
- ✅ Controllerから@Modelでデータをビューに渡す
- ✅ テンプレートファイルの配置場所（resources/templates）
- ✅ 条件分岐（th:if）と繰り返し（th:each）
- ✅ フォームとの連携（th:action, th:field）

---

## 🐛 トラブルシューティング

### エラー: "Error resolving template [users/list], template might not exist"

**原因**: テンプレートファイルが見つからない、またはパスが間違っている

**解決策**:
1. ファイルが`src/main/resources/templates/`以下にあるか確認
2. Controllerの戻り値とファイル名が一致しているか確認
```java
// ❌ NG
return "user/list";  // ファイル: templates/users/list.html

// ✅ OK
return "users/list";  // ファイル: templates/users/list.html
```
3. 拡張子`.html`はControllerの戻り値に含めない

### エラー: "Exception evaluating SpringEL expression"

**原因**: Thymeleaf式の構文エラー、またはnullオブジェクトへのアクセス

**解決策**:
```html
<!-- ❌ NG: userがnullの場合エラー -->
<p th:text="${user.name}"></p>

<!-- ✅ OK: Safe Navigation Operator使用 -->
<p th:text="${user?.name}"></p>

<!-- ✅ OK: 条件分岐で回避 -->
<p th:if="${user != null}" th:text="${user.name}"></p>

<!-- ✅ OK: デフォルト値を設定 -->
<p th:text="${user?.name ?: 'Unknown'}"></p>
```

### エラー: ブラウザに`${user.name}`がそのまま表示される

**原因**: Thymeleafが動作していない、またはテンプレートエンジンの設定ミス

**解決策**:
1. `spring-boot-starter-thymeleaf`の依存関係を確認
2. `@Controller`を使う（`@RestController`では動作しない）
```java
// ❌ NG: @RestControllerはJSON返却
@RestController
public class UserController {
    @GetMapping("/users")
    public String list() {
        return "users/list";  // "users/list"という文字列が返る
    }
}

// ✅ OK: @Controllerでビューを返す
@Controller
public class UserController {
    @GetMapping("/users")
    public String list() {
        return "users/list";  // templates/users/list.htmlをレンダリング
    }
}
```

### 問題: 変更したHTMLが反映されない

**原因**: Thymeleafのキャッシュが有効になっている

**解決策**:
```yaml
# application.yml（開発環境）
spring:
  thymeleaf:
    cache: false  # キャッシュを無効化
```

または、アプリケーションを再起動してブラウザのキャッシュをクリア（`Ctrl + Shift + R`）

### 問題: CSSやJavaScriptが読み込まれない

**原因**: 静的リソースのパスが間違っている

**解決策**:
```html
<!-- ❌ NG: 絶対パス -->
<link rel="stylesheet" href="/static/css/style.css">

<!-- ✅ OK: Thymeleafのth:hrefを使用 -->
<link rel="stylesheet" th:href="@{/css/style.css}">

<!-- ✅ OK: Webjarsを使用 -->
<link rel="stylesheet" th:href="@{/webjars/bootstrap/5.3.0/css/bootstrap.min.css}">
```

静的ファイルは`src/main/resources/static/`以下に配置

---

## 🔄 Gitへのコミットとレビュー依頼

進捗を記録してレビューを受けましょう：

```bash
git add .
git commit -m "Step 21: Thymeleafの基礎完了"
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

レビューが完了したら、[Step 22: フォーム送信とバリデーション](STEP_22.md)へ進みましょう！

次のStep 22では、**フォーム送信とバリデーション**を学びます:
- `th:object`と`th:field`でフォームバインディング
- バリデーションエラーの表示
- POSTリクエストの処理
- リダイレクトとフラッシュメッセージ

---

お疲れ様でした！🎉 Thymeleafの基礎が理解できたら、次のステップに進みましょう。
