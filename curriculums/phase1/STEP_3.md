# Step 3: POSTリクエストとリクエストボディ

## 🎯 このステップの目標

- `@PostMapping`でPOSTリクエストを処理する方法を理解する
- `@RequestBody`でJSONデータを受け取る方法を学ぶ
- DTOクラスを作成してデータを構造化する
- POSTリクエストでデータを送信し、レスポンスを返す

**所要時間**: 約1時間

---

## 📋 事前準備

- Step 2で作成した`hello-spring-boot`プロジェクト
- Postmanまたはcurlでのリクエスト送信方法の理解

**Step 2をまだ完了していない場合**: [Step 2: パスパラメータとクエリパラメータ](STEP_2.md)を先に進めてください。

---

## 💡 GETとPOSTの違い

### GET（これまで使用）

- データの**取得**に使用
- URLにパラメータを含める
- リクエストボディは使用しない
- ブラウザで直接アクセス可能

### POST（これから学ぶ）

- データの**作成・送信**に使用
- リクエストボディにデータを含める
- 大量のデータを送信できる
- JSON形式でデータを送受信

---

## 🚀 ステップ1: 最初のPOSTエンドポイント

### 1-1. UserControllerの作成

新しいコントローラーを作成します。

**ファイルパス**: `src/main/java/com/example/hellospringboot/controller/UserController.java`

```java
package com.example.hellospringboot.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class UserController {

    @PostMapping("/users")
    public String createUser(@RequestBody Map<String, String> user) {
        String name = user.get("name");
        String email = user.get("email");
        return "User created: " + name + " (" + email + ")";
    }
}
```

### 1-2. コードの解説

#### `@PostMapping("/users")`
- HTTPのPOSTリクエストを受け付ける
- `/users`というパスで受け付ける
- `@GetMapping`のPOST版

#### `@RequestBody`
- リクエストボディのJSONをJavaオブジェクトに変換
- Spring Bootが自動的にJSON→オブジェクトに変換（デシリアライズ）

#### `Map<String, String>`
- シンプルなキー・バリュー形式でデータを受け取る
- とりあえず動かすには便利だが、実用的ではない

### 1-3. 動作確認（curl）

アプリケーションを起動して以下を実行：

```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Taro","email":"taro@example.com"}'
```

**期待される結果**:
```
User created: Taro (taro@example.com)
```

### 1-4. 動作確認（Postman）

1. Postmanを開く
2. メソッド: `POST`
3. URL: `http://localhost:8080/users`
4. Headers: `Content-Type: application/json`
5. Body → raw → JSON を選択
6. 以下を入力：
```json
{
  "name": "Taro",
  "email": "taro@example.com"
}
```
7. 「Send」をクリック

---

## 🚀 ステップ2: DTOクラスの作成

### 2-1. DTOとは？

**DTO (Data Transfer Object)** = データ転送用オブジェクト

**メリット**:
- 型安全性: コンパイル時にエラーを検出
- 可読性: どんなデータが必要か明確
- 保守性: フィールド追加・変更が容易

### 2-2. UserRequestDTOの作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/UserRequest.java`

```java
package com.example.hellospringboot.dto;

public class UserRequest {
    private String name;
    private String email;
    private Integer age;

    // デフォルトコンストラクタ（必須）
    public UserRequest() {
    }

    // コンストラクタ
    public UserRequest(String name, String email, Integer age) {
        this.name = name;
        this.email = email;
        this.age = age;
    }

    // Getter
    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Integer getAge() {
        return age;
    }

    // Setter
    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}
```

### 2-3. UserResponseDTOの作成

レスポンス用のDTOも作成します。

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/UserResponse.java`

```java
package com.example.hellospringboot.dto;

public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private Integer age;
    private String createdAt;

    public UserResponse() {
    }

    public UserResponse(Long id, String name, String email, Integer age, String createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
        this.createdAt = createdAt;
    }

    // Getter
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Integer getAge() {
        return age;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    // Setter
    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
```

---

## 🚀 ステップ3: DTOを使ったコントローラーの改良

### 3-1. UserControllerの修正

`UserController.java`を以下のように**修正**します：

```java
package com.example.hellospringboot.controller;

import com.example.hellospringboot.dto.UserRequest;
import com.example.hellospringboot.dto.UserResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@RestController
public class UserController {

    // IDを自動採番するためのカウンター
    private final AtomicLong counter = new AtomicLong(1);

    @PostMapping("/users")
    public UserResponse createUser(@RequestBody UserRequest request) {
        // IDを自動生成
        Long id = counter.getAndIncrement();
        
        // 現在時刻を取得
        String createdAt = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        // レスポンスを作成
        UserResponse response = new UserResponse(
            id,
            request.getName(),
            request.getEmail(),
            request.getAge(),
            createdAt
        );
        
        return response;
    }
}
```

### 3-2. コードの解説

#### `AtomicLong counter`
- スレッドセーフなカウンター
- `getAndIncrement()`で1ずつ増加するIDを生成
- 実際のアプリではデータベースが自動採番

#### `@RequestBody UserRequest request`
- JSONが`UserRequest`オブジェクトに自動変換される
- `request.getName()`でアクセス可能

#### 戻り値が`UserResponse`
- Javaオブジェクトが自動的にJSONに変換される（シリアライズ）
- Spring Bootが`@RestController`で自動処理

### 3-3. 動作確認

```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Hanako","email":"hanako@example.com","age":25}'
```

**期待される結果**（JSON形式で返る）:
```json
{
  "id": 1,
  "name": "Hanako",
  "email": "hanako@example.com",
  "age": 25,
  "createdAt": "2025-10-27 14:30:45"
}
```

複数回実行すると、IDが増えていくことを確認できます。

---

## 🚀 ステップ4: GETエンドポイントの追加

### 4-1. ユーザー一覧取得APIの実装

作成したユーザーを保存して取得できるようにします。

`UserController.java`に以下を追加：

```java
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@RestController
public class UserController {

    private final AtomicLong counter = new AtomicLong(1);
    // ユーザーを保存するリスト（本来はデータベース）
    private final List<UserResponse> users = new ArrayList<>();

    @PostMapping("/users")
    public UserResponse createUser(@RequestBody UserRequest request) {
        Long id = counter.getAndIncrement();
        
        String createdAt = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        UserResponse response = new UserResponse(
            id,
            request.getName(),
            request.getEmail(),
            request.getAge(),
            createdAt
        );
        
        // リストに追加
        users.add(response);
        
        return response;
    }

    @GetMapping("/users")
    public List<UserResponse> getUsers() {
        return users;
    }
}
```

### 4-2. 動作確認

```bash
# ユーザーを3人作成
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Taro","email":"taro@example.com","age":30}'

curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Hanako","email":"hanako@example.com","age":25}'

curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Jiro","email":"jiro@example.com","age":28}'

# 一覧取得
curl http://localhost:8080/users
```

**期待される結果**:
```json
[
  {
    "id": 1,
    "name": "Taro",
    "email": "taro@example.com",
    "age": 30,
    "createdAt": "2025-10-27 14:30:45"
  },
  {
    "id": 2,
    "name": "Hanako",
    "email": "hanako@example.com",
    "age": 25,
    "createdAt": "2025-10-27 14:31:02"
  },
  {
    "id": 3,
    "name": "Jiro",
    "email": "jiro@example.com",
    "age": 28,
    "createdAt": "2025-10-27 14:31:15"
  }
]
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: IDで特定のユーザーを取得

`GET /users/{id}`で特定のユーザーを取得するエンドポイントを追加してください。

**ヒント**:
```java
@GetMapping("/users/{id}")
public UserResponse getUser(@PathVariable Long id) {
    // usersリストから該当IDを探す
    // Optional: 見つからない場合のエラーハンドリング
}
```

### チャレンジ 2: 商品登録API

ユーザーと同様に、商品を登録するAPIを作成してください。

**必要なフィールド**:
- `name` (商品名)
- `price` (価格)
- `category` (カテゴリ)

**エンドポイント**:
- `POST /products`: 商品登録
- `GET /products`: 商品一覧

### チャレンジ 3: レスポンスにメッセージを追加

登録成功時に以下のような構造のレスポンスを返してください：

```json
{
  "success": true,
  "message": "User created successfully",
  "data": {
    "id": 1,
    "name": "Taro",
    ...
  }
}
```

**ヒント**: 新しいレスポンスDTOを作成します。

---

## 🐛 トラブルシューティング

### エラー: "HttpMediaTypeNotSupportedException"

**原因**: `Content-Type: application/json`ヘッダーが設定されていない

**解決策**:
```bash
# ヘッダーを追加
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Taro","email":"taro@example.com"}'
```

### エラー: JSONのパースエラー

**原因**: JSON形式が正しくない

**よくある間違い**:
```json
// NG: シングルクォート
{'name':'Taro'}

// NG: ダブルクォート忘れ
{name:"Taro"}

// OK: ダブルクォートで囲む
{"name":"Taro"}
```

### Getterが呼ばれない / JSONが空

**原因**: DTOにGetterがない、またはpublicでない

**解決策**: すべてのフィールドにpublicなGetterを用意

```java
public String getName() {
    return name;
}
```

### デフォルトコンストラクタエラー

**エラー**: "Cannot construct instance of..."

**原因**: DTOにデフォルトコンストラクタ（引数なし）がない

**解決策**:
```java
public UserRequest() {
    // 引数なしのコンストラクタを追加
}
```

---

## 📚 このステップで学んだこと

- ✅ `@PostMapping`でPOSTリクエストを処理
- ✅ `@RequestBody`でJSONをJavaオブジェクトに変換
- ✅ DTOクラスを作成してデータを構造化
- ✅ リクエストDTOとレスポンスDTOの分離
- ✅ JavaオブジェクトをJSONに自動変換
- ✅ POSTとGETを組み合わせたCRUD操作の基礎
- ✅ インメモリでのデータ保存（リスト使用）

---

## 💡 補足: JSONとJavaオブジェクトの変換

### デシリアライズ（JSON → Java）

Spring Bootは**Jackson**というライブラリを使ってJSONをJavaオブジェクトに変換します。

```
リクエスト:
{"name":"Taro","email":"taro@example.com"}
    ↓ Spring Bootが自動変換
UserRequest {
    name = "Taro"
    email = "taro@example.com"
}
```

**変換ルール**:
- JSONのキーとJavaのフィールド名が一致
- SetterまたはフィールドにJacksonがアクセス
- デフォルトコンストラクタが必要

### シリアライズ（Java → JSON）

逆にJavaオブジェクトをJSONに変換します。

```
UserResponse {
    id = 1
    name = "Taro"
    email = "taro@example.com"
}
    ↓ Spring Bootが自動変換
レスポンス:
{"id":1,"name":"Taro","email":"taro@example.com"}
```

**変換ルール**:
- Getterメソッドが必要
- `getXxx()`の`Xxx`がJSONのキー名になる
- `getId()` → `"id"`

### なぜDTOにGetter/Setterが必要？

- **Setter**: JSON → Java変換時にJacksonが使用
- **Getter**: Java → JSON変換時にJacksonが使用

**次のステップ（Step 5）でLombokを使うと、これらを自動生成できます！**

---

## 🔄 Gitへのコミットとレビュー依頼

進捗を記録してレビューを受けましょう：

```bash
git add .
git commit -m "Step 3: POSTリクエストとDTO実装完了"
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

レビューが完了したら、[Step 4: application.ymlで設定管理](STEP_4.md)へ進みましょう！

次のステップでは、アプリケーションの設定を外部ファイル（application.yml）で管理する方法を学びます。
ポート番号の変更や、カスタムプロパティの定義方法を習得します。

---

お疲れさまでした！ 🎉

POSTリクエストとDTOの基本をマスターしました。
Step 5ではLombokを使ってDTO作成を劇的に簡略化します。お楽しみに！
