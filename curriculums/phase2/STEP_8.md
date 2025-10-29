# Step 8: CRUD操作の完成

## 🎯 このステップの目標

- Update（更新）機能を実装する
- Delete（削除）機能を実装する
- IDで特定のデータを取得する機能を実装する
- 完全なRESTful APIを完成させる
- 適切なHTTPメソッド（GET, POST, PUT, DELETE）を使い分ける

**所要時間**: 約45分

---

## 📋 事前準備

- Step 7で作成したUserエンティティとリポジトリ
- Create（作成）とRead（読み取り）機能が動作していること

**Step 7をまだ完了していない場合**: [Step 7: Spring Data JPAでCRUDの基本](STEP_7.md)を先に進めてください。

---

## 💡 CRUDとRESTful API

### CRUDとは？

**CRUD** = データベースの基本操作

| 操作 | 意味 | HTTPメソッド | エンドポイント例 |
|------|------|--------------|------------------|
| **C**reate | 作成 | POST | `POST /api/users` |
| **R**ead | 読み取り | GET | `GET /api/users` |
| **U**pdate | 更新 | PUT/PATCH | `PUT /api/users/{id}` |
| **D**elete | 削除 | DELETE | `DELETE /api/users/{id}` |

### RESTful APIの設計

| 操作 | HTTPメソッド | URL | 説明 |
|------|-------------|-----|------|
| 一覧取得 | GET | `/api/users` | 全ユーザー取得 |
| 詳細取得 | GET | `/api/users/{id}` | 特定ユーザー取得 |
| 作成 | POST | `/api/users` | ユーザー作成 |
| 更新 | PUT | `/api/users/{id}` | ユーザー更新 |
| 削除 | DELETE | `/api/users/{id}` | ユーザー削除 |

---

## 🚀 ステップ1: Read - IDで取得

### 1-1. UserServiceにメソッド追加

`UserService.java`に以下のメソッドを追加します。

**ファイルパス**: `src/main/java/com/example/hellospringboot/service/UserService.java`

```java
package com.example.hellospringboot.service;

import com.example.hellospringboot.entity.User;
import com.example.hellospringboot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * ユーザーを作成
     */
    public User createUser(User user) {
        return userRepository.save(user);
    }

    /**
     * 全ユーザーを取得
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * IDでユーザーを取得
     */
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
}
```

### 1-2. Optionalとは？

```java
public Optional<User> getUserById(Long id) {
    return userRepository.findById(id);
}
```

**Optional<T>** = 値が存在するかもしれないし、しないかもしれないことを表す型

**従来の方法（null返却）の問題点**:
```java
User user = getUserById(999);
String name = user.getName();  // NullPointerException発生！
```

**Optionalを使う利点**:
```java
Optional<User> userOpt = getUserById(999);
if (userOpt.isPresent()) {
    User user = userOpt.get();
    String name = user.getName();  // 安全
}
```

### 1-3. UserControllerにエンドポイント追加

`UserController.java`に以下のメソッドを追加します。

**ファイルパス**: `src/main/java/com/example/hellospringboot/controller/UserController.java`

```java
package com.example.hellospringboot.controller;

import com.example.hellospringboot.entity.User;
import com.example.hellospringboot.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * ユーザー作成
     * POST /api/users
     */
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    /**
     * 全ユーザー取得
     * GET /api/users
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * IDでユーザー取得
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
```

### 1-4. コードの解説

```java
@GetMapping("/{id}")
public ResponseEntity<User> getUserById(@PathVariable Long id) {
    return userService.getUserById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
}
```

- `Optional<User>`から値を取り出す
- 値が存在する場合: `ResponseEntity.ok(user)` → 200 OK
- 値が存在しない場合: `ResponseEntity.notFound().build()` → 404 Not Found

### 1-5. 動作確認

まずユーザーを作成：

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Taro",
    "email": "taro@example.com",
    "age": 30
  }'
```

IDで取得：

```bash
# 存在するID
curl http://localhost:8080/api/users/1
```

**期待される結果**:
```json
{
  "id": 1,
  "name": "Taro",
  "email": "taro@example.com",
  "age": 30
}
```

存在しないIDで試す：

```bash
curl -i http://localhost:8080/api/users/999
```

**期待される結果**:
```
HTTP/1.1 404
```

---

## 🚀 ステップ2: Update - 更新

### 2-1. UserServiceにupdateメソッド追加

`UserService.java`に以下を追加：

```java
/**
 * ユーザーを更新
 */
public Optional<User> updateUser(Long id, User userDetails) {
    return userRepository.findById(id)
            .map(existingUser -> {
                existingUser.setName(userDetails.getName());
                existingUser.setEmail(userDetails.getEmail());
                existingUser.setAge(userDetails.getAge());
                return userRepository.save(existingUser);
            });
}
```

### 2-2. コードの解説

```java
public Optional<User> updateUser(Long id, User userDetails) {
    return userRepository.findById(id)  // ①IDでユーザーを検索
            .map(existingUser -> {      // ②存在する場合
                existingUser.setName(userDetails.getName());
                existingUser.setEmail(userDetails.getEmail());
                existingUser.setAge(userDetails.getAge());
                return userRepository.save(existingUser);  // ③保存
            });  // ④存在しない場合はOptional.empty()を返す
}
```

**処理の流れ**:
1. IDでユーザーを検索
2. 見つかった場合、フィールドを更新
3. データベースに保存
4. 見つからない場合、`Optional.empty()`を返す

### 2-3. UserControllerにupdateエンドポイント追加

`UserController.java`に以下を追加：

```java
/**
 * ユーザー更新
 * PUT /api/users/{id}
 */
@PutMapping("/{id}")
public ResponseEntity<User> updateUser(
        @PathVariable Long id,
        @RequestBody User userDetails) {
    return userService.updateUser(id, userDetails)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
}
```

### 2-4. 動作確認

ユーザーを更新：

```bash
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Taro Yamada Updated",
    "email": "taro.updated@example.com",
    "age": 31
  }'
```

**期待される結果**:
```json
{
  "id": 1,
  "name": "Taro Yamada Updated",
  "email": "taro.updated@example.com",
  "age": 31
}
```

確認：

```bash
curl http://localhost:8080/api/users/1
```

更新された内容が返ってきます。

存在しないIDで試す：

```bash
curl -i -X PUT http://localhost:8080/api/users/999 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test",
    "email": "test@example.com",
    "age": 20
  }'
```

**期待される結果**: `404 Not Found`

---

## 🚀 ステップ3: Delete - 削除

### 3-1. UserServiceにdeleteメソッド追加

`UserService.java`に以下を追加：

```java
/**
 * ユーザーを削除
 */
public boolean deleteUser(Long id) {
    if (userRepository.existsById(id)) {
        userRepository.deleteById(id);
        return true;
    }
    return false;
}
```

### 3-2. コードの解説

```java
public boolean deleteUser(Long id) {
    if (userRepository.existsById(id)) {  // ①存在チェック
        userRepository.deleteById(id);     // ②削除
        return true;                        // ③成功
    }
    return false;                          // ④存在しない
}
```

**なぜ存在チェックが必要？**
- `deleteById()`は存在しないIDでもエラーにならない
- 削除成功/失敗を呼び出し側に伝えるため

### 3-3. UserControllerにdeleteエンドポイント追加

`UserController.java`に以下を追加：

```java
/**
 * ユーザー削除
 * DELETE /api/users/{id}
 */
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    if (userService.deleteUser(id)) {
        return ResponseEntity.noContent().build();  // 204 No Content
    }
    return ResponseEntity.notFound().build();  // 404 Not Found
}
```

### 3-4. HTTPステータスコードの使い分け

| ステータスコード | 意味 | 使用場面 |
|-----------------|------|----------|
| 200 OK | 成功（レスポンスボディあり） | 取得、更新成功 |
| 201 Created | 作成成功 | ユーザー作成 |
| 204 No Content | 成功（レスポンスボディなし） | 削除成功 |
| 404 Not Found | リソースが存在しない | ID不一致 |

### 3-5. 動作確認

ユーザーを作成：

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Delete Test",
    "email": "delete@example.com",
    "age": 20
  }'
```

IDが2とします。削除実行：

```bash
curl -i -X DELETE http://localhost:8080/api/users/2
```

**期待される結果**:
```
HTTP/1.1 204
```

確認：

```bash
curl -i http://localhost:8080/api/users/2
```

**期待される結果**: `404 Not Found`

全ユーザーを確認：

```bash
curl http://localhost:8080/api/users
```

ID=2のユーザーが削除されています。

---

## ✅ 完成したコード全体

### UserService.java

```java
package com.example.hellospringboot.service;

import com.example.hellospringboot.entity.User;
import com.example.hellospringboot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * ユーザーを作成
     */
    public User createUser(User user) {
        return userRepository.save(user);
    }

    /**
     * 全ユーザーを取得
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * IDでユーザーを取得
     */
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * ユーザーを更新
     */
    public Optional<User> updateUser(Long id, User userDetails) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    existingUser.setName(userDetails.getName());
                    existingUser.setEmail(userDetails.getEmail());
                    existingUser.setAge(userDetails.getAge());
                    return userRepository.save(existingUser);
                });
    }

    /**
     * ユーザーを削除
     */
    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
```

### UserController.java

```java
package com.example.hellospringboot.controller;

import com.example.hellospringboot.entity.User;
import com.example.hellospringboot.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * ユーザー作成
     * POST /api/users
     */
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    /**
     * 全ユーザー取得
     * GET /api/users
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * IDでユーザー取得
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * ユーザー更新
     * PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @RequestBody User userDetails) {
        return userService.updateUser(id, userDetails)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * ユーザー削除
     * DELETE /api/users/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userService.deleteUser(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
```

---

## 🎨 完全なCRUD操作テスト

一連の流れをテストしてみましょう：

```bash
# 1. ユーザー作成
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com","age":28}'

# 2. 全ユーザー取得
curl http://localhost:8080/api/users

# 3. IDで取得
curl http://localhost:8080/api/users/1

# 4. 更新
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice Smith","email":"alice.smith@example.com","age":29}'

# 5. 更新確認
curl http://localhost:8080/api/users/1

# 6. 削除
curl -X DELETE http://localhost:8080/api/users/1

# 7. 削除確認（404になる）
curl -i http://localhost:8080/api/users/1
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: 部分更新（PATCH）

現在の実装では、更新時にすべてのフィールドを送る必要があります。
一部のフィールドだけを更新できるように改良してください。

**ヒント**:
```java
public Optional<User> patchUser(Long id, User userDetails) {
    return userRepository.findById(id)
            .map(existingUser -> {
                if (userDetails.getName() != null) {
                    existingUser.setName(userDetails.getName());
                }
                if (userDetails.getEmail() != null) {
                    existingUser.setEmail(userDetails.getEmail());
                }
                if (userDetails.getAge() != null) {
                    existingUser.setAge(userDetails.getAge());
                }
                return userRepository.save(existingUser);
            });
}

// Controller
@PatchMapping("/{id}")
public ResponseEntity<User> patchUser(@PathVariable Long id, @RequestBody User userDetails) {
    // ...
}
```

### チャレンジ 2: 削除時にメッセージを返す

削除成功時に、以下のようなメッセージを返すようにしてください。

```json
{
  "message": "User deleted successfully",
  "id": 1
}
```

**ヒント**: レスポンス用のDTOを作成します。

### チャレンジ 3: バルク削除

複数のユーザーを一度に削除するAPIを追加してください。

**エンドポイント**: `DELETE /api/users`  
**リクエストボディ**: `{"ids": [1, 2, 3]}`

---

## 🐛 トラブルシューティング

### "Detached entity passed to persist"

**症状**: 更新時にエラー

**原因**: エンティティがデタッチ状態

**解決策**: `save()`を呼び出す

### 削除後もデータが残っている

**症状**: `DELETE`後も`GET`でデータが取得できる

**原因**: トランザクションがコミットされていない（通常は自動）

**解決策**: アプリケーションを再起動、またはDBeaverで確認

### PUTとPATCHの違いがわからない

**PUT**: リソース全体を置き換え（全フィールド必須）  
**PATCH**: リソースの一部を更新（一部フィールドのみ）

```bash
# PUT - 全フィールド必要
curl -X PUT http://localhost:8080/api/users/1 \
  -d '{"name":"New Name","email":"new@example.com","age":30}'

# PATCH - 一部だけでOK
curl -X PATCH http://localhost:8080/api/users/1 \
  -d '{"name":"New Name"}'
```

### 404 vs 204の使い分け

**削除成功**: 204 No Content（レスポンスボディなし）  
**削除失敗（IDが存在しない）**: 404 Not Found

---

## 📚 このステップで学んだこと

- ✅ `findById()`でIDによる検索
- ✅ `Optional<T>`での安全なnull処理
- ✅ `save()`を使った更新処理
- ✅ `deleteById()`による削除
- ✅ `existsById()`での存在チェック
- ✅ RESTful APIの完全な実装（CRUD）
- ✅ HTTPステータスコードの適切な使い分け
- ✅ PUT, DELETE, GETの使い分け

---

## 💡 補足: saveメソッドの動作

### saveは作成と更新の両方に使える

```java
User user = new User();
user.setName("Taro");
userRepository.save(user);  // INSERT（新規作成）

user.setName("Taro Updated");
userRepository.save(user);  // UPDATE（更新）
```

**どうやって区別？**
- IDが`null`または存在しない → INSERT
- IDが存在する → UPDATE

**内部の動作**:
```java
public <S extends T> S save(S entity) {
    if (entity.isNew()) {
        entityManager.persist(entity);  // INSERT
    } else {
        entity = entityManager.merge(entity);  // UPDATE
    }
    return entity;
}
```

---

## 💡 補足: RESTful APIのベストプラクティス

### URLの設計

**良い例** ✅:
```
GET    /api/users       # 一覧
GET    /api/users/1     # 詳細
POST   /api/users       # 作成
PUT    /api/users/1     # 更新
DELETE /api/users/1     # 削除
```

**悪い例** ❌:
```
GET    /api/getUsers
GET    /api/getUserById?id=1
POST   /api/createUser
POST   /api/updateUser
POST   /api/deleteUser
```

### ステータスコードの使い分け

| 操作 | 成功時 | 失敗時 |
|------|--------|--------|
| GET | 200 OK | 404 Not Found |
| POST | 201 Created | 400 Bad Request |
| PUT | 200 OK | 404 Not Found |
| DELETE | 204 No Content | 404 Not Found |

---

## 🔄 Gitへのコミットとレビュー依頼

進捗を記録してレビューを受けましょう：

```bash
git add .
git commit -m "Step 8: CRUD操作完成（Update, Delete実装）"
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

レビューが完了したら、[Step 9: @Transactionalでトランザクション管理](STEP_9.md)へ進みましょう！

次のステップでは、複数のデータベース操作を1つのトランザクションとしてまとめて実行し、
エラー時に自動的にロールバックする方法を学びます。データの整合性を保つ重要な技術です！

---

お疲れさまでした！ 🎉

完全なCRUD操作ができるREST APIが完成しました！
これで基本的なデータベース操作はすべてマスターです。
次はトランザクション管理という、さらに実践的なテーマに進みましょう！
