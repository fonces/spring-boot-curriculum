# Step 2: パスパラメータとクエリパラメータ

## 🎯 このステップの目標

- `@PathVariable`を使ってパスパラメータ（URL内の動的な値）を受け取れる
- `@RequestParam`を使ってクエリパラメータ（`?key=value`形式）を受け取れる
- 必須パラメータとオプショナルパラメータの違いを理解し、実装できる
- 複数のパラメータを組み合わせた実践的なREST APIを作成できる
- パスパラメータとクエリパラメータの使い分けを理解できる

**所要時間**: 約45分

---

## 📋 事前準備

このステップを始める前に、以下を確認してください：

- [Step 1: Hello World REST API](STEP_1.md)が完了している
- `hello-spring-boot`プロジェクトが作成されている
- `HelloController.java`に`/hello`エンドポイントが実装されている
- アプリケーションが正常に起動・動作することを確認している

### 環境確認

Step 1で作成したプロジェクトディレクトリに移動し、アプリケーションが起動することを確認しましょう：

```bash
cd ~/workspace/hello-spring-boot
./mvnw spring-boot:run
```

別のターミナルで動作確認：

```bash
curl http://localhost:8080/hello
```

**期待される結果**:
```sh
Hello, Spring Boot!
```

確認できたら、`Ctrl+C`でアプリケーションを停止してください。

---

## 🚀 ステップ1: パスパラメータの実装

パスパラメータは、URL内に埋め込まれた動的な値です。例えば、`/users/123`の`123`や`/products/apple`の`apple`がパスパラメータです。

### 1-1. パスパラメータとは

**パスパラメータ（Path Parameter）** は、URLの一部として渡される変数です。RESTful APIでは、リソースの識別子（IDや名前）を表すために使用されます。

**例**:
- `/users/1` → ユーザーID `1`のユーザー情報を取得
- `/users/2` → ユーザーID `2`のユーザー情報を取得
- `/products/laptop` → 商品名 `laptop`の商品情報を取得

### 1-2. @PathVariableを使った実装

`HelloController.java`に、パスパラメータを受け取る新しいメソッドを追加します。

**ファイルパス**: `src/main/java/com/example/hellospringboot/HelloController.java`

```java
package com.example.hellospringboot;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot!";
    }

    // パスパラメータを使ったエンドポイント
    @GetMapping("/users/{id}")
    public String getUser(@PathVariable Long id) {
        return "User ID: " + id;
    }
}
```

### 1-3. コードの解説

#### `@GetMapping("/users/{id}")`

- `{id}` はパスパラメータのプレースホルダーです
- URLの該当部分に入る値が、メソッドのパラメータに渡されます
- `/users/1`、`/users/999`など、任意の値を受け取れます

#### `@PathVariable Long id`

- `@PathVariable`は、URLのパスパラメータをメソッドの引数にバインドするアノテーションです
- `Long id`は、パスパラメータの値を`Long`型で受け取ることを意味します
- Spring Bootが自動的に文字列を`Long`型に変換してくれます

> **💡 型変換の仕組み**: URLはすべて文字列ですが、Spring Bootが自動的に指定した型（`Long`、`Integer`、`String`など）に変換します。変換できない値（例: `/users/abc`）が渡されると、`400 Bad Request`エラーが返されます。

### 1-4. 複数のパスパラメータ

パスパラメータは複数使用することもできます。以下のメソッドを`HelloController.java`に追加しましょう：

```java
package com.example.hellospringboot;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot!";
    }

    @GetMapping("/users/{id}")
    public String getUser(@PathVariable Long id) {
        return "User ID: " + id;
    }

    // 複数のパスパラメータ
    @GetMapping("/users/{userId}/posts/{postId}")
    public String getUserPost(@PathVariable Long userId, @PathVariable Long postId) {
        return "User ID: " + userId + ", Post ID: " + postId;
    }
}
```

#### コードの解説

- `/users/{userId}/posts/{postId}` では、2つのパスパラメータを使用しています
- `@PathVariable Long userId` と `@PathVariable Long postId` で、それぞれの値を受け取ります
- パラメータ名（`userId`、`postId`）は、URL内のプレースホルダー名と一致させる必要があります

---

## 🚀 ステップ2: クエリパラメータの実装

クエリパラメータは、URLの`?`以降に`key=value`の形式で渡されるパラメータです。フィルタリング、ソート、ページネーションなどに使用されます。

### 2-1. クエリパラメータとは

**クエリパラメータ（Query Parameter）** は、URLの末尾に`?`で区切って渡される追加の情報です。

**例**:
- `/search?keyword=spring` → 検索キーワード `spring`
- `/users?page=1&size=10` → ページ番号 `1`、表示件数 `10`
- `/products?category=electronics&sort=price` → カテゴリ `electronics`、ソート `price`

### 2-2. @RequestParamを使った実装

`HelloController.java`に、クエリパラメータを受け取るメソッドを追加します：

```java
package com.example.hellospringboot;

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

    @GetMapping("/users/{id}")
    public String getUser(@PathVariable Long id) {
        return "User ID: " + id;
    }

    @GetMapping("/users/{userId}/posts/{postId}")
    public String getUserPost(@PathVariable Long userId, @PathVariable Long postId) {
        return "User ID: " + userId + ", Post ID: " + postId;
    }

    // クエリパラメータを使ったエンドポイント
    @GetMapping("/search")
    public String search(@RequestParam String keyword) {
        return "Search keyword: " + keyword;
    }
}
```

### 2-3. コードの解説

#### `@RequestParam String keyword`

- `@RequestParam`は、クエリパラメータをメソッドの引数にバインドするアノテーションです
- `String keyword`は、URLの`?keyword=xxx`の値を受け取ります
- デフォルトでは**必須パラメータ**となり、パラメータがない場合は`400 Bad Request`エラーになります

---

## 🚀 ステップ3: オプショナルパラメータとデフォルト値

### 3-1. オプショナルなクエリパラメータ

クエリパラメータは、必須ではなくオプション（任意）にすることができます。以下のメソッドを追加しましょう：

```java
package com.example.hellospringboot;

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

    @GetMapping("/users/{id}")
    public String getUser(@PathVariable Long id) {
        return "User ID: " + id;
    }

    @GetMapping("/users/{userId}/posts/{postId}")
    public String getUserPost(@PathVariable Long userId, @PathVariable Long postId) {
        return "User ID: " + userId + ", Post ID: " + postId;
    }

    @GetMapping("/search")
    public String search(@RequestParam String keyword) {
        return "Search keyword: " + keyword;
    }

    // オプショナルなクエリパラメータ（required = false）
    @GetMapping("/greet")
    public String greet(@RequestParam(required = false) String name) {
        if (name == null) {
            return "Hello, Guest!";
        }
        return "Hello, " + name + "!";
    }
}
```

#### コードの解説

- `@RequestParam(required = false)` で、パラメータをオプショナルにしています
- パラメータが指定されなかった場合、`name`の値は`null`になります
- `if (name == null)` で、パラメータがない場合の処理を分岐しています

### 3-2. デフォルト値の設定

オプショナルパラメータには、デフォルト値を設定することもできます：

```java
package com.example.hellospringboot;

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

    @GetMapping("/users/{id}")
    public String getUser(@PathVariable Long id) {
        return "User ID: " + id;
    }

    @GetMapping("/users/{userId}/posts/{postId}")
    public String getUserPost(@PathVariable Long userId, @PathVariable Long postId) {
        return "User ID: " + userId + ", Post ID: " + postId;
    }

    @GetMapping("/search")
    public String search(@RequestParam String keyword) {
        return "Search keyword: " + keyword;
    }

    @GetMapping("/greet")
    public String greet(@RequestParam(required = false) String name) {
        if (name == null) {
            return "Hello, Guest!";
        }
        return "Hello, " + name + "!";
    }

    // デフォルト値を持つクエリパラメータ
    @GetMapping("/page")
    public String getPage(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size) {
        return "Page: " + page + ", Size: " + size;
    }
}
```

#### コードの解説

- `@RequestParam(defaultValue = "1")` で、デフォルト値を設定しています
- パラメータが指定されなかった場合、デフォルト値が使用されます
- デフォルト値を設定すると、自動的に`required = false`となります
- 複数のクエリパラメータを組み合わせることができます

---

## 🚀 ステップ4: 複数パラメータの組み合わせ

実際のREST APIでは、パスパラメータとクエリパラメータを組み合わせて使用することが多くあります。

### 4-1. パスパラメータとクエリパラメータの組み合わせ

以下のメソッドを`HelloController.java`に追加します：

```java
package com.example.hellospringboot;

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

    @GetMapping("/users/{id}")
    public String getUser(@PathVariable Long id) {
        return "User ID: " + id;
    }

    @GetMapping("/users/{userId}/posts/{postId}")
    public String getUserPost(@PathVariable Long userId, @PathVariable Long postId) {
        return "User ID: " + userId + ", Post ID: " + postId;
    }

    @GetMapping("/search")
    public String search(@RequestParam String keyword) {
        return "Search keyword: " + keyword;
    }

    @GetMapping("/greet")
    public String greet(@RequestParam(required = false) String name) {
        if (name == null) {
            return "Hello, Guest!";
        }
        return "Hello, " + name + "!";
    }

    @GetMapping("/page")
    public String getPage(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size) {
        return "Page: " + page + ", Size: " + size;
    }

    // パスパラメータとクエリパラメータの組み合わせ
    @GetMapping("/users/{id}/posts")
    public String getUserPosts(@PathVariable Long id,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int size) {
        return "User ID: " + id + ", Page: " + page + ", Size: " + size;
    }
}
```

#### コードの解説

- `/users/{id}/posts` では、ユーザーIDをパスパラメータで受け取っています
- `page` と `size` をクエリパラメータで受け取り、ページネーション情報を指定できます
- リクエスト例: `/users/123/posts?page=2&size=20`

### 4-2. 実践的な例：複数条件での検索

さらに実践的な検索エンドポイントを追加しましょう：

```java
package com.example.hellospringboot;

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

    @GetMapping("/users/{id}")
    public String getUser(@PathVariable Long id) {
        return "User ID: " + id;
    }

    @GetMapping("/users/{userId}/posts/{postId}")
    public String getUserPost(@PathVariable Long userId, @PathVariable Long postId) {
        return "User ID: " + userId + ", Post ID: " + postId;
    }

    @GetMapping("/search")
    public String search(@RequestParam String keyword) {
        return "Search keyword: " + keyword;
    }

    @GetMapping("/greet")
    public String greet(@RequestParam(required = false) String name) {
        if (name == null) {
            return "Hello, Guest!";
        }
        return "Hello, " + name + "!";
    }

    @GetMapping("/page")
    public String getPage(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size) {
        return "Page: " + page + ", Size: " + size;
    }

    @GetMapping("/users/{id}/posts")
    public String getUserPosts(@PathVariable Long id,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int size) {
        return "User ID: " + id + ", Page: " + page + ", Size: " + size;
    }

    // 複数の条件を持つ検索エンドポイント
    @GetMapping("/products/search")
    public String searchProducts(@RequestParam String keyword,
                                 @RequestParam(required = false) String category,
                                 @RequestParam(defaultValue = "10") int limit) {
        StringBuilder result = new StringBuilder("Search - Keyword: " + keyword);
        if (category != null) {
            result.append(", Category: ").append(category);
        }
        result.append(", Limit: ").append(limit);
        return result.toString();
    }
}
```

#### コードの解説

- `keyword` は必須パラメータ
- `category` はオプショナル（指定されていない場合は`null`）
- `limit` はデフォルト値`10`を持つオプショナルパラメータ
- `StringBuilder`を使って、動的に結果文字列を構築しています

---

## ✅ 動作確認

アプリケーションを起動して、すべてのエンドポイントを確認しましょう。

### 起動

```bash
./mvnw spring-boot:run
```

### 1. パスパラメータの確認

```bash
# 単一のパスパラメータ
curl http://localhost:8080/users/123
```

**期待される結果**:
```sh
User ID: 123
```

```bash
# 複数のパスパラメータ
curl http://localhost:8080/users/1/posts/456
```

**期待される結果**:
```sh
User ID: 1, Post ID: 456
```

### 2. クエリパラメータの確認

```bash
# 必須のクエリパラメータ
curl http://localhost:8080/search?keyword=spring
```

**期待される結果**:
```sh
Search keyword: spring
```

```bash
# オプショナルなクエリパラメータ（パラメータあり）
curl http://localhost:8080/greet?name=Taro
```

**期待される結果**:
```sh
Hello, Taro!
```

```bash
# オプショナルなクエリパラメータ（パラメータなし）
curl http://localhost:8080/greet
```

**期待される結果**:
```sh
Hello, Guest!
```

### 3. デフォルト値の確認

```bash
# デフォルト値を使用
curl http://localhost:8080/page
```

**期待される結果**:
```sh
Page: 1, Size: 10
```

```bash
# カスタム値を指定
curl "http://localhost:8080/page?page=3&size=20"
```

**期待される結果**:
```sh
Page: 3, Size: 20
```

> **💡 ヒント**: URLに`&`が含まれる場合、シェルが誤解釈しないようにダブルクォートで囲みます。

### 4. パスパラメータとクエリパラメータの組み合わせ

```bash
# 組み合わせ（デフォルト値を使用）
curl http://localhost:8080/users/5/posts
```

**期待される結果**:
```sh
User ID: 5, Page: 1, Size: 10
```

```bash
# 組み合わせ（カスタム値を指定）
curl "http://localhost:8080/users/5/posts?page=2&size=25"
```

**期待される結果**:
```sh
User ID: 5, Page: 2, Size: 25
```

### 5. 複数条件の検索

```bash
# すべてのパラメータを指定
curl "http://localhost:8080/products/search?keyword=laptop&category=electronics&limit=5"
```

**期待される結果**:
```sh
Search - Keyword: laptop, Category: electronics, Limit: 5
```

```bash
# オプショナルなcategoryを省略
curl "http://localhost:8080/products/search?keyword=laptop&limit=20"
```

**期待される結果**:
```sh
Search - Keyword: laptop, Limit: 20
```

```bash
# limitもデフォルト値を使用
curl "http://localhost:8080/products/search?keyword=laptop"
```

**期待される結果**:
```sh
Search - Keyword: laptop, Limit: 10
```

---

## 🎨 チャレンジ課題

基本が理解できたら、以下にチャレンジしてみましょう！

### チャレンジ 1: ブログ記事のURL

ブログの記事を年月日で取得するエンドポイントを作成してください：

- URL: `/posts/{year}/{month}/{day}`
- 例: `/posts/2024/12/25` → `"Article Date: 2024-12-25"`

**ヒント**:
```java
@GetMapping("/posts/{year}/{month}/{day}")
public String getArticleByDate(/* ここにパラメータを追加 */) {
    // 実装してみましょう
}
```

### チャレンジ 2: ソート機能付きユーザー一覧

ユーザー一覧を取得するエンドポイントで、ソート機能を追加してください：

- URL: `/users`
- クエリパラメータ:
  - `sort`: ソートフィールド（デフォルト: `id`）
  - `order`: ソート順（`asc` または `desc`、デフォルト: `asc`）
- 例: `/users?sort=name&order=desc` → `"Users sorted by name (desc)"`

**ヒント**:
```java
@GetMapping("/users")
public String getUsers(@RequestParam(/* デフォルト値を設定 */) String sort,
                       @RequestParam(/* デフォルト値を設定 */) String order) {
    // 実装してみましょう
}
```

### チャレンジ 3: パラメータの範囲チェック

`/page` エンドポイントを改良して、以下の条件をチェックしてください：

- `page` は 1 以上
- `size` は 1 以上 100 以下

範囲外の値が渡された場合は、エラーメッセージを返すようにしましょう。

**ヒント**:
```java
@GetMapping("/page/validated")
public String getPageValidated(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int size) {
    if (page < 1) {
        return "Error: page must be >= 1";
    }
    // sizeのバリデーションも追加してみましょう
}
```

> **💡 次のステップで学ぶこと**: このチャレンジでは手動でバリデーションを実装しましたが、後のステップ（Step 18）で、Spring Bootの**バリデーション機能**を使ったより洗練された方法を学びます。

---

## 🐛 トラブルシューティング

### エラー: "Required request parameter 'xxx' is not present"

**症状**: クエリパラメータを指定せずにリクエストすると、エラーが返される

```bash
curl http://localhost:8080/search
# {"timestamp":"...","status":400,"error":"Bad Request","message":"Required request parameter 'keyword' is not present",...}
```

**原因**: `@RequestParam`はデフォルトで必須パラメータとなります

**解決策**:
1. **クエリパラメータを指定する**:
   ```bash
   curl http://localhost:8080/search?keyword=test
   ```

2. **オプショナルにする**（コードを修正）:
   ```java
   @GetMapping("/search")
   public String search(@RequestParam(required = false) String keyword) {
       if (keyword == null) {
           return "Please provide a keyword";
       }
       return "Search keyword: " + keyword;
   }
   ```

3. **デフォルト値を設定する**（コードを修正）:
   ```java
   @GetMapping("/search")
   public String search(@RequestParam(defaultValue = "all") String keyword) {
       return "Search keyword: " + keyword;
   }
   ```

### エラー: "Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long'"

**症状**: パスパラメータに数値以外を指定すると、エラーが返される

```bash
curl http://localhost:8080/users/abc
# {"timestamp":"...","status":400,"error":"Bad Request",...}
```

**原因**: `@PathVariable Long id` で`Long`型を期待していますが、文字列`"abc"`を変換できません

**解決策**:
1. **正しい型の値を指定する**:
   ```bash
   curl http://localhost:8080/users/123
   ```

2. **String型で受け取り、手動でバリデーション**（コードを修正）:
   ```java
   @GetMapping("/users/{id}")
   public String getUser(@PathVariable String id) {
       try {
           Long userId = Long.parseLong(id);
           return "User ID: " + userId;
       } catch (NumberFormatException e) {
           return "Error: Invalid user ID format";
       }
   }
   ```

### クエリパラメータに日本語を含む場合

**症状**: 日本語のクエリパラメータが文字化けする

```bash
curl http://localhost:8080/search?keyword=こんにちは
```

**解決策**: URLエンコードを使用します

```bash
# macOS/Linux
curl "http://localhost:8080/search?keyword=$(echo -n 'こんにちは' | jq -sRr @uri)"

# より簡単な方法（最近のcurlでは自動エンコードされることが多い）
curl --get --data-urlencode "keyword=こんにちは" http://localhost:8080/search

# または、手動でURLエンコード
curl "http://localhost:8080/search?keyword=%E3%81%93%E3%82%93%E3%81%AB%E3%81%A1%E3%81%AF"
```

> **💡 ヒント**: ブラウザでアクセスする場合、日本語は自動的にURLエンコードされるため、問題なく動作します。

### パラメータ名のミスマッチ

**症状**: パラメータを指定しているのに、受け取れない

```bash
curl "http://localhost:8080/page?pageNumber=2"
# Page: 1, Size: 10  ← pageNumberが反映されていない
```

**原因**: メソッドのパラメータ名（`page`）とクエリパラメータ名（`pageNumber`）が一致していません

**解決策**:
1. **クエリパラメータ名を合わせる**:
   ```bash
   curl "http://localhost:8080/page?page=2"
   ```

2. **@RequestParamで明示的に指定する**（コードを修正）:
   ```java
   @GetMapping("/page")
   public String getPage(@RequestParam(name = "pageNumber", defaultValue = "1") int page,
                         @RequestParam(defaultValue = "10") int size) {
       return "Page: " + page + ", Size: " + size;
   }
   ```

---

## 💡 補足: パスパラメータ vs クエリパラメータの使い分け

RESTful APIを設計する際、パスパラメータとクエリパラメータのどちらを使うべきか迷うことがあります。以下のガイドラインを参考にしてください。

### パスパラメータを使うべきケース

**リソースの識別子** を表す場合に使用します：

- ✅ `/users/{id}` - 特定のユーザーを識別
- ✅ `/products/{productId}` - 特定の商品を識別
- ✅ `/posts/{year}/{month}/{day}` - 特定の日付の記事を識別

**特徴**:
- URLの一部としてリソースを表す
- 階層構造を表現できる
- 必須の値（省略できない）

### クエリパラメータを使うべきケース

**フィルタリング、ソート、ページネーション** など、リソースの取得方法を指定する場合に使用します：

- ✅ `/users?role=admin` - 管理者ユーザーでフィルタリング
- ✅ `/products?sort=price&order=asc` - 価格でソート
- ✅ `/posts?page=2&size=20` - ページネーション
- ✅ `/search?keyword=spring&category=books` - 検索条件

**特徴**:
- リソースの取得方法を調整
- オプショナルな値が多い
- 複数の条件を組み合わせやすい

### 実践例

以下の例で使い分けを確認しましょう：

```java
// 良い設計 ✅
@GetMapping("/users/{id}")          // ユーザーID: パスパラメータ
@GetMapping("/users")                // ユーザー一覧
@GetMapping("/users")                // フィルタリング: クエリパラメータ
// 例: /users?role=admin&status=active

// 悪い設計 ❌
@GetMapping("/users")                // ユーザーIDをクエリで渡す
// 例: /users?id=123  ← リソースの識別子はパスパラメータにすべき

@GetMapping("/users/{role}")         // フィルタ条件をパスに含める
// 例: /users/admin  ← フィルタはクエリパラメータにすべき
```

### 組み合わせの例

実際には、両方を組み合わせることが多くあります：

```java
// ユーザーIDで特定し、その投稿をページネーション
@GetMapping("/users/{id}/posts")
// 例: /users/123/posts?page=1&size=10

// 商品IDで特定し、レビューを評価でフィルタリング
@GetMapping("/products/{id}/reviews")
// 例: /products/456/reviews?rating=5
```

---

## 📚 このステップで学んだこと

- ✅ **@PathVariable**を使って、URLのパスパラメータを受け取れる
- ✅ **@RequestParam**を使って、クエリパラメータを受け取れる
- ✅ **required = false**で、オプショナルなパラメータを実装できる
- ✅ **defaultValue**で、デフォルト値を設定できる
- ✅ **複数のパラメータ**を組み合わせた実践的なエンドポイントを作成できる
- ✅ **パスパラメータとクエリパラメータの使い分け**を理解できる
- ✅ Spring Bootの**自動型変換**により、文字列を数値型などに変換できる
- ✅ RESTful APIの設計における**基本的なベストプラクティス**を理解できる

---

## ➡️ 次のステップ

[Step 3: POSTリクエストとリクエストボディ](STEP_3.md)へ進みましょう！

次のステップでは、以下を学びます：

- **POSTリクエスト**でデータを送信する方法
- **@PostMapping**と**@RequestBody**の使い方
- **JSON形式**でデータを送受信する方法
- ユーザー登録や商品作成など、**データを作成するAPI**の実装

これまではGETリクエストで**データを取得**する方法を学びました。次はPOSTリクエストで**データを作成**する方法を学び、より実践的なREST APIを構築しましょう！
