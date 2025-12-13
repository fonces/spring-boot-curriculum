# Step 3: POSTリクエストとリクエストボディ

## 🎯 このステップの目標

- `@PostMapping`を使ってPOSTリクエストを処理できる
- `@RequestBody`でJSONデータを受け取り、Java POJOに変換できる
- HTTPメソッド（GET、POST）の違いと使い分けを理解できる
- `ResponseEntity<T>`を使って適切なHTTPステータスコードを返せる
- POJOクラス（Plain Old Java Object）とJSONの自動変換の仕組みを理解できる

**所要時間**: 約50分

---

## 📋 事前準備

このステップを始める前に、以下を確認してください：

- [Step 2: パスパラメータとクエリパラメータ](STEP_2.md)が完了している
- `hello-spring-boot`プロジェクトが作成されている
- `HelloController.java`に各種エンドポイントが実装されている
- curlコマンドでAPIの動作確認ができる

### 環境確認

Step 2で作成したプロジェクトディレクトリに移動し、アプリケーションが起動することを確認しましょう：

```bash
cd ~/workspace/hello-spring-boot
./mvnw spring-boot:run
```

別のターミナルで動作確認：

```bash
curl http://localhost:8080/users/123
```

**期待される結果**:
```
User ID: 123
```

確認できたら、`Ctrl+C`でアプリケーションを停止してください。

---

## 🚀 ステップ1: HTTPメソッドの理解

POSTリクエストを実装する前に、HTTPメソッドの基本を理解しましょう。

### 1-1. GETとPOSTの違い

#### GETメソッド
- **目的**: リソースの取得（読み取り）
- **データの送信方法**: URLのクエリパラメータ（`?key=value`）
- **用途**: データの検索、一覧取得、詳細表示
- **特徴**: 
  - ブラウザのアドレスバーに直接入力できる
  - ブックマーク可能
  - 履歴に残る
  - データの変更はしない（冪等性）

**例**: `/users?page=1` - ユーザー一覧を取得

#### POSTメソッド
- **目的**: リソースの作成
- **データの送信方法**: リクエストボディ（HTTPボディ部分）
- **用途**: データの新規作成、フォーム送信
- **特徴**: 
  - 大量のデータを送信できる
  - URLにデータが表示されない（セキュア）
  - ブックマークできない
  - データを変更する

**例**: `POST /users` + JSON - 新しいユーザーを作成

### 1-2. RESTful APIでのHTTPメソッドの使い分け

| HTTPメソッド | 用途 | 例 |
|------------|------|-----|
| **GET** | 取得（Read） | `GET /users` - ユーザー一覧取得 |
| **POST** | 作成（Create） | `POST /users` - ユーザー作成 |
| **PUT** | 更新（Update） | `PUT /users/123` - ユーザー123を更新 |
| **DELETE** | 削除（Delete） | `DELETE /users/123` - ユーザー123を削除 |

このステップでは**POST**を扱い、PUT/DELETEはチャレンジ課題で学びます。

---

## 🚀 ステップ2: POJOクラス（Userクラス）の作成

JSONデータを受け取るために、まずPOJO（Plain Old Java Object）クラスを作成します。

### 2-1. POJOとは

**POJO（Plain Old Java Object）** は、特別なフレームワークに依存しない、シンプルなJavaクラスのことです。データを格納するための入れ物として使います。

**POJOの特徴**:
- フィールド（プロパティ）を持つ
- ゲッター/セッターメソッドを持つ
- コンストラクタを持つ
- フレームワークに依存しない

### 2-2. Userクラスを作成

`src/main/java/com/example/hellospringboot/`ディレクトリに、`User.java`を新規作成します。

**ファイルパス**: `src/main/java/com/example/hellospringboot/User.java`

```java
package com.example.hellospringboot;

public class User {
    private Long id;
    private String name;
    private String email;
    private Integer age;

    // デフォルトコンストラクタ（JSONデシリアライズに必要）
    public User() {
    }

    // すべてのフィールドを持つコンストラクタ
    public User(Long id, String name, String email, Integer age) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
    }

    // ゲッター/セッター
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}
```

### 2-3. コードの解説

#### フィールド（プロパティ）

```java
private Long id;
private String name;
private String email;
private Integer age;
```

- `private`修飾子で外部から直接アクセスできないようにする
- ゲッター/セッターを通してアクセスする（カプセル化）
- `Long`や`Integer`はnullを許容するため、オプショナルな値に適している

#### デフォルトコンストラクタ

```java
public User() {
}
```

- **重要**: Jacksonライブラリ（Spring BootのJSON変換ライブラリ）がJSONからJavaオブジェクトを作成する際に必要
- 引数なしのコンストラクタがないと、JSONのデシリアライズ（変換）に失敗します

#### ゲッター/セッター

```java
public String getName() {
    return name;
}

public void setName(String name) {
    this.name = name;
}
```

- Jacksonは、JSONのキー名とゲッター/セッターのメソッド名を対応させます
- `name`フィールド → `getName()`/`setName()` → JSONの`"name"`キー
- このルールを**JavaBeansの規約**と呼びます

---

## 🚀 ステップ3: POSTエンドポイントの作成

新しいコントローラー`UserController.java`を作成し、POSTリクエストを処理します。

### 3-1. UserControllerを作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/UserController.java`

```java
package com.example.hellospringboot;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        // 実際にはDBに保存する処理がここに入る（今回は省略）
        
        // 仮のIDを設定（通常はDBで自動生成）
        user.setId(1L);
        
        // 201 Created ステータスコードとともにユーザー情報を返す
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
}
```

### 3-2. コードの解説

#### `@RestController`

```java
@RestController
```

- クラスがREST APIのコントローラーであることを示します
- すべてのメソッドの戻り値が自動的にJSON形式に変換されます
- Step 1、Step 2で学んだ`HelloController`と同じアノテーションです

#### `@RequestMapping("/api/users")`

```java
@RequestMapping("/api/users")
```

- クラスレベルで共通のベースパスを定義します
- このコントローラー内のすべてのエンドポイントは`/api/users`から始まります
- メソッドごとに個別のパスを追加できます

#### `@PostMapping`

```java
@PostMapping
public ResponseEntity<User> createUser(@RequestBody User user) {
```

- POSTリクエストを処理するメソッドであることを示します
- `@RequestMapping(method = RequestMethod.POST)`と同じ意味の省略形です
- クラスの`/api/users`と組み合わさって、`POST /api/users`エンドポイントになります

#### `@RequestBody`

```java
@RequestBody User user
```

- **HTTPリクエストボディのJSON**を、**Javaオブジェクト**に自動変換します
- この変換処理を**デシリアライゼーション（Deserialization）** と呼びます
- Spring Bootは内部で**Jackson**ライブラリを使ってJSON→Java変換を行います

**変換例**:

リクエストボディ（JSON）:
```json
{
  "name": "山田太郎",
  "email": "taro@example.com",
  "age": 30
}
```

↓ 自動変換 ↓

Javaオブジェクト:
```java
User user = new User();
user.setName("山田太郎");
user.setEmail("taro@example.com");
user.setAge(30);
```

#### `ResponseEntity<User>`

```java
public ResponseEntity<User> createUser(@RequestBody User user) {
```

- **HTTPレスポンス全体**（ステータスコード、ヘッダー、ボディ）を制御できるクラスです
- `<User>`はジェネリクスで、レスポンスボディの型を指定しています

#### `ResponseEntity.status(HttpStatus.CREATED).body(user)`

```java
return ResponseEntity.status(HttpStatus.CREATED).body(user);
```

- `HttpStatus.CREATED`は**201 Created**ステータスコードを返します
- `body(user)`でレスポンスボディにUserオブジェクトを設定します
- Userオブジェクトは自動的にJSONに変換されます（**シリアライゼーション**）

**HTTPステータスコードの使い分け**:

| ステータスコード | 意味 | 用途 |
|---------------|------|------|
| **200 OK** | 成功 | GETリクエストの成功 |
| **201 Created** | 作成成功 | POSTリクエストでリソース作成成功 |
| **204 No Content** | 成功（ボディなし） | DELETEリクエストの成功 |
| **400 Bad Request** | リクエストエラー | バリデーションエラー |
| **404 Not Found** | 未検出 | 指定したリソースが存在しない |
| **500 Internal Server Error** | サーバーエラー | サーバー側の予期しないエラー |

---

## 🚀 ステップ4: JacksonによるJSON自動変換の仕組み

Spring Bootでは、**Jackson**ライブラリがJSON⇔Javaオブジェクトの変換を自動で行います。

### 4-1. Jacksonとは

**Jackson**は、JavaとJSONを相互変換するための最も人気のあるライブラリです。Spring Bootには標準で組み込まれているため、特別な設定は不要です。

### 4-2. シリアライゼーション（Java → JSON）

**シリアライゼーション（Serialization）** は、JavaオブジェクトをJSON文字列に変換することです。

```java
User user = new User(1L, "山田太郎", "taro@example.com", 30);
// ↓ Jacksonが自動変換
// {"id":1,"name":"山田太郎","email":"taro@example.com","age":30}
```

Jacksonは以下のルールで変換します：
1. ゲッターメソッド（`getName()`など）を探す
2. メソッド名から`get`を除いて最初を小文字にした名前をJSONキーにする
3. メソッドの戻り値をJSONの値にする

### 4-3. デシリアライゼーション（JSON → Java）

**デシリアライゼーション（Deserialization）** は、JSON文字列をJavaオブジェクトに変換することです。

```json
{"name":"山田太郎","email":"taro@example.com","age":30}
```

↓ Jacksonが自動変換 ↓

```java
User user = new User();
user.setName("山田太郎");
user.setEmail("taro@example.com");
user.setAge(30);
```

Jacksonは以下のルールで変換します：
1. デフォルトコンストラクタ（引数なし）でオブジェクトを作成
2. JSONのキー名に対応するセッターメソッド（`setName()`など）を探す
3. JSONの値をセッターメソッドに渡す

**重要**: デフォルトコンストラクタがないと、デシリアライゼーションに失敗します！

### 4-4. Content-Typeヘッダー

JSON形式のデータを送受信する際は、**Content-Type**ヘッダーで形式を明示します。

- **リクエスト**: `Content-Type: application/json` - 送信するデータがJSON形式
- **レスポンス**: Spring Bootが自動的に`Content-Type: application/json`を設定

---

## ✅ ステップ5: 動作確認

### 5-1. アプリケーションの起動

まず、アプリケーションを起動します：

```bash
cd ~/workspace/hello-spring-boot
./mvnw spring-boot:run
```

起動ログに以下のようなメッセージが表示されればOKです：

```
Started HelloSpringBootApplication in X.XXX seconds
```

### 5-2. POSTリクエストの送信

別のターミナルを開き、curlコマンドでPOSTリクエストを送信します：

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "山田太郎",
    "email": "taro@example.com",
    "age": 30
  }'
```

**コマンドの解説**:
- `-X POST`: POSTメソッドを使用
- `-H "Content-Type: application/json"`: リクエストボディがJSON形式であることを明示
- `-d '{...}'`: リクエストボディのデータ（JSON）

**期待される結果**:

```json
{"id":1,"name":"山田太郎","email":"taro@example.com","age":30}
```

レスポンスに`id`が追加されて返ってきていることを確認してください。

### 5-3. HTTPステータスコードの確認

ステータスコードも確認してみましょう：

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "佐藤花子", "email": "hanako@example.com", "age": 25}' \
  -i
```

`-i`オプションをつけると、レスポンスヘッダーも表示されます。

**期待される結果**:

```
HTTP/1.1 201 
Content-Type: application/json
...

{"id":1,"name":"佐藤花子","email":"hanako@example.com","age":25}
```

**HTTP/1.1 201** が表示されていれば、201 Createdステータスコードが正しく返されています！

### 5-4. 様々なパターンでテスト

#### パターン1: ageを省略

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "鈴木一郎", "email": "ichiro@example.com"}'
```

**期待される結果**:
```json
{"id":1,"name":"鈴木一郎","email":"ichiro@example.com","age":null}
```

省略されたフィールドは`null`になります。

#### パターン2: 余分なフィールドを含める

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "田中次郎", "email": "jiro@example.com", "age": 28, "address": "東京都"}'
```

**期待される結果**:
```json
{"id":1,"name":"田中次郎","email":"jiro@example.com","age":28}
```

Userクラスに存在しない`address`フィールドは無視されます（デフォルト動作）。

#### パターン3: Content-Typeヘッダーを省略した場合

```bash
curl -X POST http://localhost:8080/api/users \
  -d '{"name": "テスト", "email": "test@example.com"}'
```

**期待される結果**:
```
{"timestamp":"2025-12-13T...","status":415,"error":"Unsupported Media Type",...}
```

`Content-Type: application/json`がないと、Spring Bootはリクエストボディを解釈できず、**415 Unsupported Media Type**エラーになります。

---

## 🎨 チャレンジ課題

基本が理解できたら、以下にチャレンジしてみましょう：

### チャレンジ 1: 複数ユーザーの一括作成

`POST /api/users/batch`エンドポイントを作成し、ユーザーの配列を受け取って一括作成してみましょう。

**ヒント**:
```java
@PostMapping("/batch")
public ResponseEntity<List<User>> createUsers(@RequestBody List<User> users) {
    // ユーザーリストを処理
    // ...
    return ResponseEntity.status(HttpStatus.CREATED).body(users);
}
```

**テスト用curlコマンド**:
```bash
curl -X POST http://localhost:8080/api/users/batch \
  -H "Content-Type: application/json" \
  -d '[
    {"name": "ユーザー1", "email": "user1@example.com", "age": 20},
    {"name": "ユーザー2", "email": "user2@example.com", "age": 30}
  ]'
```

### チャレンジ 2: GETエンドポイントの追加

`UserController`に、特定のユーザーを取得する`GET /api/users/{id}`エンドポイントを追加してみましょう。

**ヒント**:
```java
@GetMapping("/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id) {
    // 仮データを返す（実際にはDBから取得）
    User user = new User(id, "サンプルユーザー", "sample@example.com", 25);
    return ResponseEntity.ok(user);
}
```

**テスト用curlコマンド**:
```bash
curl http://localhost:8080/api/users/123
```

### チャレンジ 3: PUT（更新）エンドポイントの実装

`PUT /api/users/{id}`エンドポイントを作成し、既存ユーザー情報を更新してみましょう。

**ヒント**:
```java
@PutMapping("/{id}")
public ResponseEntity<User> updateUser(
    @PathVariable Long id,
    @RequestBody User user
) {
    // IDを設定
    user.setId(id);
    
    // 実際にはDBを更新する処理がここに入る
    
    // 200 OK で更新後のユーザー情報を返す
    return ResponseEntity.ok(user);
}
```

**テスト用curlコマンド**:
```bash
curl -X PUT http://localhost:8080/api/users/123 \
  -H "Content-Type: application/json" \
  -d '{"name": "更新太郎", "email": "updated@example.com", "age": 35}'
```

### チャレンジ 4: DELETE（削除）エンドポイントの実装

`DELETE /api/users/{id}`エンドポイントを作成し、ユーザーを削除してみましょう。

**ヒント**:
```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    // 実際にはDBから削除する処理がここに入る
    
    // 204 No Content（成功、ボディなし）を返す
    return ResponseEntity.noContent().build();
}
```

**テスト用curlコマンド**:
```bash
curl -X DELETE http://localhost:8080/api/users/123 -i
```

**期待される結果**: `HTTP/1.1 204` ステータスコード、ボディなし

---

## 🐛 トラブルシューティング

### エラー: "HTTP Status 415 - Unsupported Media Type"

**原因**: リクエストヘッダーに`Content-Type: application/json`が設定されていません。

**解決策**: curlコマンドに`-H "Content-Type: application/json"`を追加してください。

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "テスト", "email": "test@example.com"}'
```

### エラー: "Cannot construct instance of User (no Creators, like default constructor, exist)"

**原因**: `User.java`にデフォルトコンストラクタ（引数なし）が定義されていません。

**解決策**: `User.java`に以下を追加してください。

```java
public User() {
}
```

Jacksonは、JSONからJavaオブジェクトを作成する際にデフォルトコンストラクタを使用します。

### エラー: "Required request body is missing"

**原因**: リクエストボディ（`-d`オプション）が指定されていません。

**解決策**: `-d`オプションでJSONデータを指定してください。

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "山田太郎", "email": "taro@example.com"}'
```

### エラー: JSONのパース失敗（Unexpected character...）

**原因**: JSON形式が正しくありません（カンマ忘れ、引用符の不一致など）。

**解決策**: JSON形式を確認してください。以下の点に注意：

- キーと値は**ダブルクォート**（`"key": "value"`）
- 複数のフィールドはカンマで区切る
- 最後のフィールドの後にカンマをつけない

**良い例**:
```json
{"name": "山田太郎", "email": "taro@example.com", "age": 30}
```

**悪い例**:
```json
{name: '山田太郎', email: 'taro@example.com', age: 30,}
```

### エラー: シェルでのJSON記述エラー（bash: syntax error）

**原因**: シングルクォート内で日本語やエスケープ文字を使用した際に、シェルがJSON文字列を正しく解釈できていません。

**解決策**: JSONデータをファイルに保存して送信するか、エスケープを調整してください。

**方法1: JSONファイルを使用**:

`user.json`:
```json
{
  "name": "山田太郎",
  "email": "taro@example.com",
  "age": 30
}
```

curlコマンド:
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d @user.json
```

**方法2: ヒアドキュメントを使用**:
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d @- << EOF
{
  "name": "山田太郎",
  "email": "taro@example.com",
  "age": 30
}
EOF
```

### コンパイルエラー: "package org.springframework.http does not exist"

**原因**: Spring Webの依存関係が不足している可能性があります（通常は発生しません）。

**解決策**: `pom.xml`に以下の依存関係が含まれていることを確認してください。

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

依存関係を追加した後、再度ビルドしてください：

```bash
./mvnw clean install
```

---

## 💡 補足: RESTful APIの設計原則

### REST（Representational State Transfer）とは

**REST**は、Web APIを設計するためのアーキテクチャスタイルです。以下の原則に従います：

#### 1. リソース指向

URLはリソース（データの集合）を表します。

- **良い例**: `/api/users` - ユーザーリソース
- **悪い例**: `/api/getUsers` - 動詞を含む

#### 2. HTTPメソッドで操作を表現

リソースに対する操作はHTTPメソッドで表現します。

| 操作 | HTTPメソッド | エンドポイント | 説明 |
|-----|------------|--------------|------|
| 一覧取得 | GET | `/api/users` | すべてのユーザーを取得 |
| 詳細取得 | GET | `/api/users/{id}` | 特定のユーザーを取得 |
| 作成 | POST | `/api/users` | 新しいユーザーを作成 |
| 更新 | PUT | `/api/users/{id}` | ユーザー情報を更新 |
| 削除 | DELETE | `/api/users/{id}` | ユーザーを削除 |

#### 3. ステートレス

各リクエストは独立しており、サーバーはクライアントの状態を保持しません。

#### 4. 適切なHTTPステータスコードを使用

- **2xx**: 成功（200 OK、201 Created、204 No Content）
- **4xx**: クライアントエラー（400 Bad Request、404 Not Found）
- **5xx**: サーバーエラー（500 Internal Server Error）

---

## 📚 このステップで学んだこと

- ✅ HTTPメソッド（GET、POST、PUT、DELETE）の違いと使い分け
- ✅ `@PostMapping`を使ったPOSTリクエストの処理
- ✅ POJO（Plain Old Java Object）クラスの作成とJavaBeans規約
- ✅ `@RequestBody`によるJSON→Javaオブジェクトの自動変換（デシリアライゼーション）
- ✅ Jacksonライブラリの役割とJSON自動変換の仕組み
- ✅ `ResponseEntity<T>`によるHTTPステータスコードとレスポンスボディの制御
- ✅ `Content-Type: application/json`ヘッダーの重要性
- ✅ curlコマンドでのPOSTリクエスト送信とテスト方法
- ✅ RESTful APIの基本設計原則（リソース指向、HTTPメソッドの使い分け）

---

## ➡️ 次のステップ

[Step 4: application.ymlで設定管理](STEP_4.md)へ進みましょう！

次のステップでは、アプリケーションの設定を外部ファイル（`application.yml`）で管理する方法を学びます。ポート番号、データベース接続情報、カスタム設定値などを、コードから分離して管理できるようになります。環境ごとに設定を切り替える実践的なテクニックも学びましょう！

---

**作成日**: 2025-12-13  
**対象バージョン**: Spring Boot 3.5.8
