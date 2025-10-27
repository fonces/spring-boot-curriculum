# Step 2: パスパラメータとクエリパラメータ

## 🎯 このステップの目標

- `@PathVariable`でパスパラメータを受け取る方法を理解する
- `@RequestParam`でクエリパラメータを受け取る方法を理解する
- パラメータのオプション設定（必須/任意、デフォルト値）を学ぶ
- 実用的な挨拶APIを実装する

**所要時間**: 約45分

---

## 📋 事前準備

- Step 1で作成した`hello-spring-boot`プロジェクト
- アプリケーションが起動できること

**Step 1をまだ完了していない場合**: [Step 1: Hello World REST API](STEP_1.md)を先に進めてください。

---

## 💡 パスパラメータとクエリパラメータとは？

REST APIでは、URLを通じてデータを受け取る方法が2つあります。

### パスパラメータ（Path Variable）

URLのパスの一部としてデータを埋め込む方式：

```
GET /users/123
GET /products/apple
GET /greet/Taro
```

**特徴**:
- リソースの識別に使用
- 必須パラメータとして扱われる
- URLの一部として明確

### クエリパラメータ（Query Parameter）

URLの末尾に`?`以降で渡す方式：

```
GET /users?page=1&size=10
GET /search?keyword=spring&sort=date
GET /greet?language=ja&formal=true
```

**特徴**:
- フィルタリングやオプション設定に使用
- 任意パラメータにできる
- 複数のパラメータを簡単に渡せる

---

## 🚀 ステップ1: パスパラメータの実装

### 1-1. 名前を受け取る挨拶API

`HelloController.java`に以下のメソッドを追加します：

**ファイルパス**: `src/main/java/com/example/hellospringboot/controller/HelloController.java`

```java
import org.springframework.web.bind.annotation.PathVariable;

@GetMapping("/greet/{name}")
public String greet(@PathVariable String name) {
    return "Hello, " + name + "!";
}
```

### 1-2. コードの解説

#### `@GetMapping("/greet/{name}")`
- URLパスに`{name}`というプレースホルダーを定義
- `{}`で囲まれた部分がパスパラメータとなる

#### `@PathVariable String name`
- URLの`{name}`部分の値を`name`変数で受け取る
- 変数名とプレースホルダー名が一致している必要がある

### 1-3. 動作確認

アプリケーションを起動して以下を実行：

```bash
curl http://localhost:8080/greet/Taro
```

**期待される結果**:
```
Hello, Taro!
```

別の名前でも試してみましょう：

```bash
curl http://localhost:8080/greet/Hanako
curl http://localhost:8080/greet/Spring
```

---

## 🚀 ステップ2: クエリパラメータの実装

### 2-1. 言語を指定できる挨拶API

パスパラメータとクエリパラメータを組み合わせます。

`HelloController.java`の`greet`メソッドを以下のように**修正**します：

```java
import org.springframework.web.bind.annotation.RequestParam;

@GetMapping("/greet/{name}")
public String greet(
    @PathVariable String name,
    @RequestParam(required = false, defaultValue = "en") String language
) {
    if (language.equals("ja")) {
        return "こんにちは、" + name + "さん！";
    } else if (language.equals("es")) {
        return "¡Hola, " + name + "!";
    } else {
        return "Hello, " + name + "!";
    }
}
```

### 2-2. コードの解説

#### `@RequestParam`
- クエリパラメータを受け取るアノテーション
- `?language=ja`の`language`パラメータの値を受け取る

#### `required = false`
- このパラメータは任意（省略可能）であることを示す
- デフォルトは`required = true`（必須）

#### `defaultValue = "en"`
- パラメータが省略された場合のデフォルト値
- `required = false`と組み合わせて使用

### 2-3. 動作確認

```bash
# 英語（デフォルト）
curl http://localhost:8080/greet/Taro

# 日本語
curl http://localhost:8080/greet/Taro?language=ja

# スペイン語
curl http://localhost:8080/greet/Taro?language=es
```

**期待される結果**:
```
Hello, Taro!
こんにちは、Taroさん！
¡Hola, Taro!
```

---

## 🚀 ステップ3: 複数のパスパラメータ

### 3-1. ユーザーIDと投稿IDを受け取るAPI

`HelloController.java`に以下のメソッドを**追加**します：

```java
@GetMapping("/users/{userId}/posts/{postId}")
public String getUserPost(
    @PathVariable Long userId,
    @PathVariable Long postId
) {
    return "User ID: " + userId + ", Post ID: " + postId;
}
```

### 3-2. コードの解説

#### 複数のパスパラメータ
- URLに複数の`{}`プレースホルダーを定義可能
- それぞれに対応する`@PathVariable`を用意

#### `Long`型の使用
- 数値のIDを受け取る場合は`Long`型を使用
- Spring Bootが自動的に文字列から数値に変換

### 3-3. 動作確認

```bash
curl http://localhost:8080/users/123/posts/456
```

**期待される結果**:
```
User ID: 123, Post ID: 456
```

---

## 🚀 ステップ4: 複数のクエリパラメータ

### 4-1. 検索APIの実装

`HelloController.java`に以下のメソッドを**追加**します：

```java
@GetMapping("/search")
public String search(
    @RequestParam String keyword,
    @RequestParam(required = false, defaultValue = "10") int limit,
    @RequestParam(required = false, defaultValue = "date") String sort
) {
    return String.format("Searching for '%s' (limit: %d, sort by: %s)", 
                         keyword, limit, sort);
}
```

### 4-2. コードの解説

#### 必須パラメータと任意パラメータの混在
- `keyword`: 必須（`required`を指定しないとデフォルトで`true`）
- `limit`と`sort`: 任意（`required = false`）

#### `int`型の使用
- 数値パラメータは`int`型で受け取れる
- 自動的に型変換される

### 4-3. 動作確認

```bash
# 必須パラメータのみ
curl "http://localhost:8080/search?keyword=spring"

# すべてのパラメータを指定
curl "http://localhost:8080/search?keyword=spring&limit=20&sort=relevance"

# 必須パラメータを省略（エラーになる）
curl http://localhost:8080/search
```

**期待される結果**:
```
Searching for 'spring' (limit: 10, sort by: date)
Searching for 'spring' (limit: 20, sort by: relevance)
{"timestamp":"2025-10-27T...","status":400,"error":"Bad Request",...}
```

---

## 🚀 ステップ5: パラメータ名のカスタマイズ

### 5-1. パラメータ名と変数名を別にする

URLのパラメータ名とJavaの変数名を別にしたい場合：

```java
@GetMapping("/greet-formal/{userName}")
public String greetFormal(
    @PathVariable(name = "userName") String name,
    @RequestParam(name = "lang", defaultValue = "en") String language
) {
    if (language.equals("ja")) {
        return name + "様、ようこそ。";
    } else {
        return "Welcome, Mr./Ms. " + name + ".";
    }
}
```

### 5-2. コードの解説

#### `@PathVariable(name = "userName")`
- URLでは`userName`というパラメータ名
- Javaのコードでは`name`という変数名で受け取る

#### `@RequestParam(name = "lang")`
- URLでは`lang`というパラメータ名
- Javaのコードでは`language`という変数名で受け取る

**使い分け**:
- URLは短く簡潔に（`lang`）
- コードは読みやすく（`language`）

### 5-3. 動作確認

```bash
curl "http://localhost:8080/greet-formal/Tanaka?lang=ja"
curl "http://localhost:8080/greet-formal/Smith?lang=en"
```

**期待される結果**:
```
Tanaka様、ようこそ。
Welcome, Mr./Ms. Smith.
```

---

## ✅ 完成したコード全体

`HelloController.java`の最終形：

```java
package com.example.hellospringboot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot!";
    }

    @GetMapping("/greet/{name}")
    public String greet(
        @PathVariable String name,
        @RequestParam(required = false, defaultValue = "en") String language
    ) {
        if (language.equals("ja")) {
            return "こんにちは、" + name + "さん！";
        } else if (language.equals("es")) {
            return "¡Hola, " + name + "!";
        } else {
            return "Hello, " + name + "!";
        }
    }

    @GetMapping("/users/{userId}/posts/{postId}")
    public String getUserPost(
        @PathVariable Long userId,
        @PathVariable Long postId
    ) {
        return "User ID: " + userId + ", Post ID: " + postId;
    }

    @GetMapping("/search")
    public String search(
        @RequestParam String keyword,
        @RequestParam(required = false, defaultValue = "10") int limit,
        @RequestParam(required = false, defaultValue = "date") String sort
    ) {
        return String.format("Searching for '%s' (limit: %d, sort by: %s)", 
                             keyword, limit, sort);
    }

    @GetMapping("/greet-formal/{userName}")
    public String greetFormal(
        @PathVariable(name = "userName") String name,
        @RequestParam(name = "lang", defaultValue = "en") String language
    ) {
        if (language.equals("ja")) {
            return name + "様、ようこそ。";
        } else {
            return "Welcome, Mr./Ms. " + name + ".";
        }
    }
}
```

---

## 🎨 チャレンジ課題

基本が理解できたら、以下にチャレンジしてみましょう：

### チャレンジ 1: 計算API

2つの数値をパスパラメータで受け取り、計算結果を返すAPIを作成してください。

**エンドポイント例**:
```
GET /calc/add/5/3  → "Result: 8"
GET /calc/multiply/4/7  → "Result: 28"
```

**ヒント**:
```java
@GetMapping("/calc/add/{a}/{b}")
public String add(@PathVariable int a, @PathVariable int b) {
    // ここに実装
}
```

### チャレンジ 2: フィルタリングAPI

商品一覧を取得するAPIで、カテゴリと価格範囲でフィルタリングできるようにしてください。

**エンドポイント例**:
```
GET /products?category=electronics&minPrice=1000&maxPrice=5000
```

**ヒント**:
```java
@GetMapping("/products")
public String getProducts(
    @RequestParam(required = false) String category,
    @RequestParam(required = false, defaultValue = "0") int minPrice,
    @RequestParam(required = false, defaultValue = "999999") int maxPrice
) {
    // ここに実装
}
```

### チャレンジ 3: 日付範囲検索

開始日と終了日をクエリパラメータで受け取り、その範囲を表示するAPIを作成してください。

**エンドポイント例**:
```
GET /reports?startDate=2025-01-01&endDate=2025-12-31
```

**ヒント**: まずは文字列で受け取ってOKです（日付型への変換は後のステップで学びます）。

---

## 🐛 トラブルシューティング

### エラー: "Required request parameter 'xxx' is not present"

**原因**: 必須パラメータ（`required = true`）が指定されていない

**解決策**:
1. クエリパラメータを付けてリクエストする
2. または`required = false`にする

```java
// 修正前（必須）
@RequestParam String keyword

// 修正後（任意）
@RequestParam(required = false) String keyword
```

### エラー: "Failed to convert value of type 'java.lang.String' to required type 'int'"

**原因**: 数値型のパラメータに数値以外が渡された

**例**: `?limit=abc` → `int`型に変換できない

**解決策**:
- 正しい数値を渡す: `?limit=10`
- より高度なバリデーションは後のステップで学びます

### 日本語が文字化けする

**原因**: URLエンコードの問題

**解決策**:
curlで日本語を渡す場合は`--data-urlencode`を使うか、手動でエンコード：

```bash
# 方法1: ブラウザで確認（自動エンコードされる）
# 方法2: エンコード済みURLを使用
curl "http://localhost:8080/greet/%E5%A4%AA%E9%83%8E?language=ja"
```

### パスパラメータが認識されない

**原因**: プレースホルダー名と変数名が一致していない

```java
// NG: 名前が一致していない
@GetMapping("/users/{id}")
public String getUser(@PathVariable Long userId) { ... }

// OK: 名前を一致させる
@GetMapping("/users/{id}")
public String getUser(@PathVariable Long id) { ... }

// OK: name属性で明示的に指定
@GetMapping("/users/{id}")
public String getUser(@PathVariable(name = "id") Long userId) { ... }
```

---

## 📚 このステップで学んだこと

- ✅ `@PathVariable`でURLパスの一部を変数として受け取る
- ✅ `@RequestParam`でクエリパラメータを受け取る
- ✅ `required`属性で必須/任意を制御
- ✅ `defaultValue`でデフォルト値を設定
- ✅ 複数のパラメータを組み合わせて使用
- ✅ パラメータの型変換（String, int, Long）
- ✅ パスパラメータとクエリパラメータの使い分け

---

## 💡 補足: パラメータの使い分けガイドライン

### パスパラメータを使うべき場合

- **リソースの識別**: `/users/{id}`, `/products/{productId}`
- **階層構造**: `/users/{userId}/orders/{orderId}`
- **必須の情報**: URLの一部として意味を持つもの

### クエリパラメータを使うべき場合

- **フィルタリング**: `/products?category=electronics`
- **ソート順**: `/users?sort=name&order=asc`
- **ページネーション**: `/items?page=2&size=20`
- **任意の情報**: 省略可能なオプション

### REST APIの設計例

```
# 良い設計
GET /users/123                    # ユーザー詳細取得
GET /users?role=admin&active=true # ユーザー一覧（フィルタ付き）
GET /users/123/posts              # 特定ユーザーの投稿一覧
GET /users/123/posts?status=published  # フィルタリング

# 避けるべき設計
GET /users?id=123                 # IDはパスパラメータにすべき
GET /getUser/123                  # 動詞は不要（HTTPメソッドで表現）
```

---

## 🔄 Gitへのコミット（推奨）

進捗を記録しましょう：

```bash
git add .
git commit -m "Step 2: パスパラメータとクエリパラメータの実装完了"
```

---

## ➡️ 次のステップ

[Step 3: POSTリクエストとリクエストボディ](STEP_3.md)へ進みましょう！

次のステップでは、GETだけでなくPOSTメソッドを使って、JSONデータを送受信する方法を学びます。
ユーザー登録APIを作成して、より実践的なREST APIの実装に挑戦します！

---

お疲れさまでした！ 🎉

パラメータの扱い方は、REST API開発の基本中の基本です。
しっかりマスターして次のステップへ進みましょう！
