# Step 21: Thymeleafの基礎

## 🎯 このステップの目標

- Thymeleafテンプレートエンジンの基本的な仕組みを理解できる
- Spring BootでThymeleafを使用したHTMLレンダリングができる
- Thymeleaf式（変数式、選択変数式）を使ったデータバインディングができる
- 条件分岐とループ処理を使った動的なHTMLを生成できる

**所要時間**: 約45分

---

## 📋 事前準備

- [Phase 4](../phase4/STEP_20.md)までのプロジェクトが完成していること
- Spring Boot 3.5.8の環境が構築済みであること
- HTMLとCSSの基礎知識があること

---

## 🎓 Thymeleafとは

### サーバーサイドテンプレートエンジンの必要性

**REST APIだけでは不十分な場合**:
- 管理画面など、JavaScriptフレームワークを使うほどでもないシンプルなUI
- SEOが重要なWebサイト（サーバーサイドレンダリングが有利）
- フォーム入力とバリデーションが中心の業務アプリケーション

**Thymeleafの特徴**:
- **Natural Templates**: HTMLとして正しい形式（ブラウザで直接開いても表示可能）
- **Spring統合**: Spring Bootと深く統合され、設定不要で使える
- **表現力**: 条件分岐、ループ、フラグメントなど豊富な機能
- **国際化対応**: メッセージプロパティとの連携が簡単

### ThymeleafとReact/Vueの比較

| 観点 | Thymeleaf | React/Vue |
|---|---|---|
| レンダリング | サーバーサイド | クライアントサイド |
| 初期表示速度 | 速い（HTMLが完成済み） | 遅い（JavaScriptが実行必要） |
| SEO | 有利（クローラーがHTML取得） | 工夫が必要（SSR必須） |
| リアルタイム性 | 不向き（ページリロード必要） | 得意（仮想DOM更新） |
| 学習コスト | 低い（HTMLベース） | 高い（JSフレームワーク） |
| 適用範囲 | 管理画面、フォーム中心 | SPA、リッチなUI |

---

## 🚀 ステップ1: Thymeleafの依存関係追加

### 1-1. pom.xmlにThymeleafを追加

`pom.xml`に以下の依存関係を追加します:

```xml
<dependencies>
    <!-- 既存の依存関係... -->
    
    <!-- Thymeleaf -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
</dependencies>
```

### 1-2. 依存関係の説明

#### `spring-boot-starter-thymeleaf`
- Thymeleafテンプレートエンジンの自動設定を有効化
- デフォルトで`src/main/resources/templates/`配下のHTMLをテンプレートとして認識
- `.html`拡張子のファイルを自動的にThymeleafテンプレートとして処理

---

## 🚀 ステップ2: 最初のThymeleafテンプレート作成

### 2-1. テンプレートディレクトリ作成

```bash
mkdir -p src/main/resources/templates
```

### 2-2. シンプルなHTMLテンプレート作成

`src/main/resources/templates/hello.html`を作成します:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Hello Thymeleaf</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            max-width: 800px;
            margin: 50px auto;
            padding: 20px;
        }
        .greeting {
            background-color: #f0f0f0;
            padding: 20px;
            border-radius: 8px;
            margin-bottom: 20px;
        }
        .info {
            color: #666;
        }
    </style>
</head>
<body>
    <div class="greeting">
        <h1>Hello, <span th:text="${name}">Guest</span>!</h1>
        <p class="info">現在時刻: <span th:text="${currentTime}">2024-01-01 00:00:00</span></p>
    </div>
</body>
</html>
```

### 2-3. テンプレートの構造解説

#### `xmlns:th="http://www.thymeleaf.org"`
- Thymeleaf名前空間の宣言
- `th:*`属性を使用可能にする
- IDEの補完機能を有効化

#### `th:text="${name}"`
- **変数式**: `${...}`でModelに格納されたデータにアクセス
- `th:text`属性で要素のテキストコンテンツを置換
- デフォルト値（"Guest"）はThymeleafが処理しない場合に表示される

---

## 🚀 ステップ3: Controllerでテンプレートを返す

### 3-1. ビューを返すController作成

`src/main/java/com/example/hellospringboot/controllers/ViewController.java`を作成します:

```java
package com.example.hellospringboot.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class ViewController {
    
    @GetMapping("/hello")
    public String hello(
            @RequestParam(defaultValue = "World") String name,
            Model model) {
        
        model.addAttribute("name", name);
        model.addAttribute("currentTime", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        return "hello";  // templates/hello.html を返す
    }
}
```

### 3-2. コードの解説

#### `@Controller` vs `@RestController`

| アノテーション | 戻り値の扱い | 用途 |
|---|---|---|
| `@Controller` | ビュー名（文字列） → テンプレートレンダリング | HTML画面を返す |
| `@RestController` | JSON/XMLにシリアライズ | REST API |

#### `Model model`
- Spring MVCが提供するデータコンテナ
- `addAttribute(key, value)`でテンプレートに渡すデータを格納
- Thymeleafテンプレートで`${key}`として参照可能

#### `return "hello"`
- テンプレート名を返す（拡張子`.html`は省略可能）
- `templates/hello.html`が自動的に選択される
- `ViewResolver`がテンプレートパスを解決

---

## 🚀 ステップ4: ユーザー一覧画面の作成

### 4-1. ユーザー一覧テンプレート

`src/main/resources/templates/users/list.html`を作成します:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>ユーザー一覧</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            max-width: 1000px;
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
        .no-users {
            text-align: center;
            padding: 40px;
            color: #999;
        }
        .actions a {
            margin-right: 10px;
            color: #4CAF50;
            text-decoration: none;
        }
    </style>
</head>
<body>
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
                <td th:text="${user.name}">田中太郎</td>
                <td th:text="${user.email}">tanaka@example.com</td>
                <td th:text="${user.age}">25</td>
                <td th:text="${#temporals.format(user.createdAt, 'yyyy-MM-dd HH:mm')}">2024-01-01 12:00</td>
            </tr>
        </tbody>
    </table>
    
    <p>合計: <span th:text="${users.size()}">0</span> 件</p>
</body>
</html>
```

### 4-2. テンプレート構文の解説

#### `th:if` と `th:unless`
```html
<div th:if="${users.empty}">ユーザーなし</div>
<table th:unless="${users.empty}">...</table>
```
- `th:if`: 条件が真の場合に要素を表示
- `th:unless`: 条件が偽の場合に表示（`th:if`の逆）
- `${users.empty}`: Listの`isEmpty()`メソッドを呼び出し

#### `th:each`ループ
```html
<tr th:each="user : ${users}">
    <td th:text="${user.id}">1</td>
</tr>
```
- `user : ${users}`: Javaの拡張for文と同じ構文
- 各要素を`user`変数に格納して繰り返し処理
- デフォルト値（"1"）はプレビュー時に表示される

#### Thymeleaf式ユーティリティ `#temporals`
```html
th:text="${#temporals.format(user.createdAt, 'yyyy-MM-dd HH:mm')}"
```
- `#temporals`: 日付・時刻処理のユーティリティオブジェクト
- `format()`: LocalDateTimeをフォーマット
- その他: `#strings`, `#numbers`, `#dates`, `#lists`, `#maps`など

### 4-3. Controller実装

`ViewController.java`に以下を追加します:

```java
package com.example.hellospringboot.controllers;

import com.example.hellospringboot.dto.UserResponse;
import com.example.hellospringboot.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/views")
@RequiredArgsConstructor
public class ViewController {
    
    private final UserService userService;
    
    @GetMapping("/hello")
    public String hello(
            @RequestParam(defaultValue = "World") String name,
            Model model) {
        
        model.addAttribute("name", name);
        model.addAttribute("currentTime", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        return "hello";
    }
    
    @GetMapping("/users")
    public String listUsers(Model model) {
        List<UserResponse> users = userService.findAll();
        model.addAttribute("users", users);
        return "users/list";
    }
}
```

---

## 🚀 ステップ5: 詳細画面とリンク

### 5-1. ユーザー詳細テンプレート

`src/main/resources/templates/users/detail.html`を作成します:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>ユーザー詳細</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            max-width: 800px;
            margin: 50px auto;
            padding: 20px;
        }
        .card {
            border: 1px solid #ddd;
            border-radius: 8px;
            padding: 20px;
            margin-bottom: 20px;
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
        .row {
            margin-bottom: 10px;
        }
        .actions {
            margin-top: 20px;
        }
        .btn {
            display: inline-block;
            padding: 10px 20px;
            background-color: #4CAF50;
            color: white;
            text-decoration: none;
            border-radius: 4px;
        }
    </style>
</head>
<body>
    <h1>ユーザー詳細</h1>
    
    <div class="card">
        <div class="row">
            <span class="label">ID:</span>
            <span class="value" th:text="${user.id}">1</span>
        </div>
        <div class="row">
            <span class="label">名前:</span>
            <span class="value" th:text="${user.name}">田中太郎</span>
        </div>
        <div class="row">
            <span class="label">メールアドレス:</span>
            <span class="value" th:text="${user.email}">tanaka@example.com</span>
        </div>
        <div class="row">
            <span class="label">年齢:</span>
            <span class="value" th:text="${user.age}">25</span>
        </div>
        <div class="row">
            <span class="label">登録日時:</span>
            <span class="value" th:text="${#temporals.format(user.createdAt, 'yyyy-MM-dd HH:mm:ss')}">2024-01-01 12:00:00</span>
        </div>
        <div class="row">
            <span class="label">更新日時:</span>
            <span class="value" th:text="${#temporals.format(user.updatedAt, 'yyyy-MM-dd HH:mm:ss')}">2024-01-01 12:00:00</span>
        </div>
    </div>
    
    <div class="actions">
        <a th:href="@{/views/users}" class="btn">一覧に戻る</a>
    </div>
</body>
</html>
```

### 5-2. リンク式の解説

#### `th:href="@{...}"`
```html
<a th:href="@{/views/users}">一覧に戻る</a>
<a th:href="@{/views/users/{id}(id=${user.id})}">詳細</a>
```
- `@{...}`: **リンク式**（URL構築）
- コンテキストパスを自動的に付与
- パスパラメータ: `{id}` でプレースホルダー、`(id=${user.id})`で値を渡す
- クエリパラメータ: `@{/users(name=${name}, age=${age})}`

### 5-3. 一覧画面にリンクを追加

`users/list.html`の`<tr>`を以下のように修正します:

```html
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
```

### 5-4. Controller実装

`ViewController.java`に詳細画面メソッドを追加します:

```java
@GetMapping("/users/{id}")
public String userDetail(@PathVariable Long id, Model model) {
    UserResponse user = userService.findById(id);
    model.addAttribute("user", user);
    return "users/detail";
}
```

---

## ✅ 動作確認

### 1. アプリケーションの起動

```bash
./mvnw spring-boot:run
```

### 2. Hello画面の確認

ブラウザで以下にアクセス:
```
http://localhost:8080/views/hello
http://localhost:8080/views/hello?name=Taro
```

**期待される結果**:
- デフォルト: "Hello, World!"
- クエリパラメータ付き: "Hello, Taro!"
- 現在時刻が表示される

### 3. ユーザー一覧画面の確認

```
http://localhost:8080/views/users
```

**期待される結果**:
- データベースのユーザーが一覧表示される
- 名前がクリック可能なリンクになっている

### 4. ユーザー詳細画面の確認

一覧画面から任意のユーザー名をクリック:
```
http://localhost:8080/views/users/1
```

**期待される結果**:
- 選択したユーザーの詳細情報が表示される
- 日時がフォーマットされている
- 「一覧に戻る」リンクで一覧画面に戻れる

---

## 🎨 チャレンジ課題

### チャレンジ 1: 検索フォーム追加

ユーザー一覧画面に名前検索フォームを追加してみましょう。

**ヒント**:
```html
<form method="get" th:action="@{/views/users}">
    <input type="text" name="name" placeholder="名前で検索">
    <button type="submit">検索</button>
</form>
```

Controllerで`@RequestParam`を受け取り、ServiceのsearchByNameメソッドを呼び出します。

### チャレンジ 2: 条件付きスタイル

年齢が30歳以上のユーザーを別の色で表示してみましょう。

**ヒント**:
```html
<td th:text="${user.age}" 
    th:classappend="${user.age >= 30} ? 'senior' : ''">25</td>
```

### チャレンジ 3: ページネーション

ユーザーが多い場合にページネーションを実装してみましょう。

**ヒント**:
- Spring Data JPAの`Pageable`を使用
- Thymeleafで`th:each="i : ${#numbers.sequence(1, totalPages)}"`でページ番号生成

---

## 🐛 トラブルシューティング

### エラー: "Error resolving template"

**原因**: テンプレートファイルが見つからない

**解決策**:
1. ファイルが`src/main/resources/templates/`配下にあるか確認
2. ファイル名の拡張子が`.html`か確認
3. Controllerの返すビュー名とファイル名が一致しているか確認

```java
// NG: return "user"; → templates/user.html を探す
// OK: return "users/list"; → templates/users/list.html を探す
```

### エラー: "PropertyNotFoundException: Property 'name' not found"

**原因**: Modelに`name`属性が追加されていない

**解決策**:
```java
@GetMapping("/hello")
public String hello(Model model) {
    // 必ずModelに値を追加
    model.addAttribute("name", "World");
    return "hello";
}
```

### エラー: テンプレートが真っ白で表示されない

**原因**: `@RestController`を使用している（JSON化される）

**解決策**:
```java
// NG: @RestController
@Controller  // OK: ビューを返す場合は@Controller
public class ViewController {
    // ...
}
```

### 警告: Thymeleaf式の自動補完が効かない

**原因**: `xmlns:th`名前空間の宣言がない

**解決策**:
```html
<!-- 必ず<html>タグに追加 -->
<html xmlns:th="http://www.thymeleaf.org">
```

### ページが404エラー

**原因**: URLパスが間違っている

**解決策**:
1. Controllerの`@GetMapping`と一致しているか確認
2. コンテキストパスを確認（`server.servlet.context-path`）
3. 起動ログで"Mapped"を検索してマッピング確認

---

## 📚 このステップで学んだこと

- ✅ Thymeleafテンプレートエンジンの基本的な仕組み
- ✅ `@Controller`でHTMLビューを返す方法
- ✅ Modelを使ったデータ受け渡し
- ✅ `th:text`変数式でデータバインディング
- ✅ `th:if`/`th:unless`で条件分岐
- ✅ `th:each`でリスト要素の繰り返し処理
- ✅ `th:href`と`@{...}`でリンク生成
- ✅ `#temporals`など式ユーティリティの使用

---

## 💡 補足: Natural Templatesとは

Thymeleafの大きな特徴が**Natural Templates**です:

```html
<td th:text="${user.name}">田中太郎</td>
```

この書き方により:
1. **Thymeleaf処理後**: `${user.name}`の値が表示される（例: "山田花子"）
2. **HTMLとして直接開く**: "田中太郎"が表示される（デザイン確認可能）

他のテンプレートエンジン（JSPなど）では:
```jsp
<td><%= user.getName() %></td>  <!-- ブラウザで開くと何も表示されない -->
```

このため、Thymeleafは**デザイナーとの協業**や**プロトタイプ作成**に適しています。

---

## ➡️ 次のステップ

[Step 22: フォーム送信とバリデーション](STEP_22.md)へ進みましょう！

次のステップでは、Thymeleafを使ったフォーム作成、POST送信、バリデーションエラーの表示方法を学びます。
