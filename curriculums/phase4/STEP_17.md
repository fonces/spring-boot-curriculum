# Step 17: 例外ハンドリング

## 🎯 このステップの目標

- REST APIにおける適切な例外ハンドリングの重要性を理解できる
- `@ControllerAdvice`と`@ExceptionHandler`を使ったグローバル例外ハンドリングを実装できる
- カスタム例外クラスを作成し、ビジネスロジックの異常を適切に表現できる
- HTTPステータスコードを正しく使い分け、クライアントに適切なエラー情報を返せる
- エラーレスポンスを統一されたJSON形式で返却できる

**所要時間**: 約50分

---

## 📋 事前準備

- [Step 16: DI/IoCコンテナの深掘り](STEP_16.md)が完了していること
- REST APIのHTTPステータスコード（200, 404, 400, 500など）の基本を知っていること
- JSONフォーマットの読み書きができること
- 例外（Exception）とtry-catchの基本を理解していること

---

## 🐛 なぜ例外ハンドリングが重要なのか

### Before（例外ハンドリングなし）

現在のコードは、エラーが発生すると以下のような問題があります：

**UserController.java**:
```java
package com.example.hellospringboot.controllers;

import com.example.hellospringboot.entities.User;
import com.example.hellospringboot.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    
    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id);  // ユーザーが存在しない場合は？
    }
}
```

**UserService.java**:
```java
package com.example.hellospringboot.services;

import com.example.hellospringboot.entities.User;
import com.example.hellospringboot.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    
    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }
}
```

**存在しないユーザーをリクエスト**:

```bash
curl http://localhost:8080/api/users/999
```

**エラーレスポンス**（Spring Bootのデフォルト）:

```json
{
  "timestamp": "2025-01-15T12:34:56.789+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/api/users/999"
}
```

**問題点**:
- ❌ HTTPステータスコードが**500（Internal Server Error）**
  - 本来は**404（Not Found）**が適切
- ❌ エラーメッセージ（"User not found: 999"）が**クライアントに返らない**
- ❌ クライアント側で**エラーの原因が分からない**
- ❌ 開発者向けのスタックトレースが**本番環境でも返る**（セキュリティリスク）
- ❌ エラーレスポンスのフォーマットが**統一されていない**

---

### After（適切な例外ハンドリング）

理想的なエラーレスポンス：

```bash
curl http://localhost:8080/api/users/999
```

**期待されるレスポンス**:

```json
{
  "timestamp": "2025-01-15T12:34:56",
  "status": 404,
  "error": "Not Found",
  "message": "User not found with id: 999",
  "path": "/api/users/999"
}
```

**改善点**:
- ✅ HTTPステータスコード**404**で返却
- ✅ クライアントが理解できる**わかりやすいエラーメッセージ**
- ✅ **統一されたJSON形式**
- ✅ 本番環境では**スタックトレースを隠す**
- ✅ 複数のエラーを**一箇所でハンドリング**（重複コード削減）

---

## 🚀 ステップ1: カスタム例外クラスの作成

### 1-1. exceptionsパッケージを作成

`src/main/java/com/example/hellospringboot/exceptions/`ディレクトリを作成します。

---

### 1-2. ResourceNotFoundExceptionの作成

以下のファイルを`src/main/java/com/example/hellospringboot/exceptions/ResourceNotFoundException.java`に作成します：

```java
package com.example.hellospringboot.exceptions;

/**
 * リソースが見つからない場合にスローされる例外
 * HTTPステータス: 404 Not Found
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
```

**ポイント**:
- `RuntimeException`を継承（チェック例外でなく非チェック例外）
- 2つのコンストラクタで柔軟なメッセージ生成
- `String.format()`で読みやすいメッセージを構築

**使用例**:
```java
// パターン1: シンプルなメッセージ
throw new ResourceNotFoundException("User not found");

// パターン2: 詳細な情報
throw new ResourceNotFoundException("User", "id", 999);
// メッセージ: "User not found with id: '999'"
```

---

### 1-3. InvalidRequestExceptionの作成

以下のファイルを`src/main/java/com/example/hellospringboot/exceptions/InvalidRequestException.java`に作成します：

```java
package com.example.hellospringboot.exceptions;

/**
 * 不正なリクエストの場合にスローされる例外
 * HTTPステータス: 400 Bad Request
 */
public class InvalidRequestException extends RuntimeException {
    
    public InvalidRequestException(String message) {
        super(message);
    }
    
    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**使用例**:
```java
if (product.getStock() < quantity) {
    throw new InvalidRequestException("Insufficient stock. Available: " + product.getStock() + ", Requested: " + quantity);
}
```

---

## 🚀 ステップ2: エラーレスポンスDTOの作成

### 2-1. FieldErrorの作成（バリデーションエラー詳細用）

まず、バリデーションエラーの詳細を表すDTOを作成します。

以下のファイルを`src/main/java/com/example/hellospringboot/dto/FieldError.java`に作成します：

```java
package com.example.hellospringboot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * バリデーションエラーの詳細情報
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldError {
    
    /**
     * エラーが発生したフィールド名
     */
    private String field;
    
    /**
     * 拒否された値
     */
    private Object rejectedValue;
    
    /**
     * エラーメッセージ
     */
    private String message;
}
```

---

### 2-2. ErrorResponseの作成

以下のファイルを`src/main/java/com/example/hellospringboot/dto/ErrorResponse.java`に作成します：

```java
package com.example.hellospringboot.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * エラーレスポンスの統一フォーマット
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private Integer status;
    
    private String error;
    
    private String message;
    
    private String path;
    
    /**
     * バリデーションエラーの詳細リスト（オプション）
     */
    private List<FieldError> errors;
}
```

**ポイント**:
- `@JsonFormat`で日付のフォーマットを指定
- `@Builder`アノテーションでビルダーパターンを使用可能に
- `errors`フィールドはバリデーションエラー時に使用（オプション）
- すべてのエラーで統一されたJSON形式

**レスポンス例**:
```json
{
  "timestamp": "2025-01-15 12:34:56",
  "status": 404,
  "error": "Not Found",
  "message": "User not found with id: '999'",
  "path": "/api/users/999"
}
```

---

## 🚀 ステップ3: GlobalExceptionHandlerの作成

### 3-1. configパッケージを作成

`src/main/java/com/example/hellospringboot/config/`ディレクトリを作成します。

---

### 3-2. GlobalExceptionHandlerクラス

以下のファイルを`src/main/java/com/example/hellospringboot/config/GlobalExceptionHandler.java`に作成します：

```java
package com.example.hellospringboot.config;

import com.example.hellospringboot.dto.ErrorResponse;
import com.example.hellospringboot.dto.FieldError;
import com.example.hellospringboot.exceptions.InvalidRequestException;
import com.example.hellospringboot.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

/**
 * アプリケーション全体の例外をハンドリングするグローバルハンドラー
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * ResourceNotFoundException をハンドリング
     * HTTPステータス: 404 Not Found
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
        ResourceNotFoundException ex,
        HttpServletRequest request
    ) {
        log.error("ResourceNotFoundException: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error(HttpStatus.NOT_FOUND.getReasonPhrase())
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .build();
        
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(errorResponse);
    }
    
    /**
     * InvalidRequestException をハンドリング
     * HTTPステータス: 400 Bad Request
     */
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequestException(
        InvalidRequestException ex,
        HttpServletRequest request
    ) {
        log.error("InvalidRequestException: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .build();
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse);
    }
    
    /**
     * バリデーションエラーをハンドリング（Step 18で使用）
     * HTTPステータス: 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        log.error("Validation failed: {}", ex.getMessage());
        
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldError(
                error.getField(),
                error.getRejectedValue(),
                error.getDefaultMessage()
            ))
            .toList();
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("Input validation failed")
            .path(request.getRequestURI())
            .errors(fieldErrors)
            .build();
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse);
    }
    
    /**
     * その他すべての例外をハンドリング
     * HTTPステータス: 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
        Exception ex,
        HttpServletRequest request
    ) {
        log.error("Unexpected error occurred", ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
            .message("An unexpected error occurred")  // 本番環境では詳細を隠す
            .path(request.getRequestURI())
            .build();
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorResponse);
    }
}
```

---

### 3-3. コードの解説

#### `@RestControllerAdvice`
- `@ControllerAdvice` + `@ResponseBody`の組み合わせ
- すべてのController（`@RestController`）に適用される
- グローバルな例外ハンドリングを実現
- レスポンスボディをJSONに自動変換

#### `@ExceptionHandler(XxxException.class)`
- 特定の例外をキャッチするメソッドに付与
- 複数の例外を指定可能: `@ExceptionHandler({Ex1.class, Ex2.class})`
- メソッドの引数で例外オブジェクトとHttpServletRequestを受け取れる

#### `@Slf4j`
- Lombokのロギングアノテーション
- `log.error()`でエラーログを出力
- Step 20で詳しく学習

#### `ErrorResponse.builder()`
- Lombokの`@Builder`アノテーションにより使用可能
- 読みやすくメンテナンスしやすいコード

#### `MethodArgumentNotValidException`
- Spring MVCのバリデーションエラー
- `@Valid`アノテーション使用時に発生
- Step 18で詳しく学習

#### `ResponseEntity<ErrorResponse>`
- HTTPステータスコードとボディを自由に設定できる
- `ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)`でレスポンス生成

#### `HttpServletRequest`
- リクエストの詳細情報を取得
- `request.getRequestURI()`でリクエストパスを取得

---

## 🚀 ステップ4: Serviceでカスタム例外を使用

### 4-1. UserServiceの修正

既存の`src/main/java/com/example/hellospringboot/services/UserService.java`を修正します：

**Before**:
```java
package com.example.hellospringboot.services;

import com.example.hellospringboot.entities.User;
import com.example.hellospringboot.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    
    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }
}
```

**After**:
```java
package com.example.hellospringboot.services;

import com.example.hellospringboot.entities.User;
import com.example.hellospringboot.exceptions.ResourceNotFoundException;
import com.example.hellospringboot.exceptions.InvalidRequestException;
import com.example.hellospringboot.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }
    
    public User createUser(User user) {
        // バリデーション: 年齢が負の数でないか
        if (user.getAge() != null && user.getAge() < 0) {
            throw new InvalidRequestException("Age must be positive");
        }
        
        // バリデーション: メールアドレスの重複チェック
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new InvalidRequestException("Email already exists: " + user.getEmail());
        }
        
        return userRepository.save(user);
    }
    
    public User updateUser(Long id, User updatedUser) {
        User existingUser = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        // 年齢のバリデーション
        if (updatedUser.getAge() != null && updatedUser.getAge() < 0) {
            throw new InvalidRequestException("Age must be positive");
        }
        
        // 既存ユーザーの情報を更新
        existingUser.setName(updatedUser.getName());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setAge(updatedUser.getAge());
        
        return userRepository.save(existingUser);
    }
    
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", "id", id);
        }
        
        userRepository.deleteById(id);
    }
}
```

---

### 4-2. UserRepositoryにメソッド追加

JPA Repositoryの`UserRepository`に以下のメソッドを追加します：

**src/main/java/com/example/hellospringboot/mappers/UserMapper.java**:

```java
package com.example.hellospringboot.mappers;

import com.example.hellospringboot.entities.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {
    @Select("SELECT id, name, email, age FROM users")
    List<User> findAll();
    
    @Select("SELECT id, name, email, age FROM users WHERE id = #{id}")
    User findById(@Param("id") Long id);
    
    @Insert("INSERT INTO users (name, email, age) VALUES (#{name}, #{email}, #{age})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);
    
    @Update("UPDATE users SET name = #{name}, email = #{email}, age = #{age} WHERE id = #{id}")
    void update(User user);
    
    @Delete("DELETE FROM users WHERE id = #{id}")
    void deleteById(@Param("id") Long id);
    
    // 新規追加: メールアドレスの存在チェック
    @Select("SELECT COUNT(*) FROM users WHERE email = #{email}")
    int countByEmail(@Param("email") String email);
    
    // 新規追加: IDの存在チェック
    @Select("SELECT COUNT(*) FROM users WHERE id = #{id}")
    int countById(@Param("id") Long id);
}
```

**UserRepositoryImplにメソッド追加**:

```java
package com.example.hellospringboot.repositories;

import com.example.hellospringboot.entities.User;
import com.example.hellospringboot.mappers.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final UserMapper userMapper;
    
    @Override
    public List<User> findAll() {
        return userMapper.findAll();
    }
    
    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userMapper.findById(id));
    }
    
    @Override
    public void save(User user) {
        if (user.getId() == null) {
            userMapper.insert(user);
        } else {
            userMapper.update(user);
        }
    }
    
    @Override
    public void deleteById(Long id) {
        userMapper.deleteById(id);
    }
    
    // 新規追加
    @Override
    public boolean existsByEmail(String email) {
        return userMapper.countByEmail(email) > 0;
    }
    
    // 新規追加
    @Override
    public boolean existsById(Long id) {
        return userMapper.countById(id) > 0;
    }
}
```

**UserRepositoryインターフェースに追加**:

```java
package com.example.hellospringboot.repositories;

import com.example.hellospringboot.entities.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    List<User> findAll();
    Optional<User> findById(Long id);
    void save(User user);
    void deleteById(Long id);
    boolean existsByEmail(String email);  // 追加
    boolean existsById(Long id);          // 追加
}
```

---

## 🚀 ステップ5: ControllerでHTTPメソッドを実装

### 5-1. UserControllerの完全実装

既存の`src/main/java/com/example/hellospringboot/controllers/UserController.java`を以下に修正します：

```java
package com.example.hellospringboot.controllers;

import com.example.hellospringboot.entities.User;
import com.example.hellospringboot.services.UserService;
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
     * 全ユーザー取得
     * GET /api/users
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    /**
     * ユーザー詳細取得
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    
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
     * ユーザー更新
     * PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        User updatedUser = userService.updateUser(id, user);
        return ResponseEntity.ok(updatedUser);
    }
    
    /**
     * ユーザー削除
     * DELETE /api/users/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
```

**ポイント**:
- `ResponseEntity<T>`で明示的にHTTPステータスを制御
- `ResponseEntity.ok()` → 200 OK
- `ResponseEntity.status(HttpStatus.CREATED)` → 201 Created
- `ResponseEntity.noContent()` → 204 No Content
- 例外はServiceでスローし、GlobalExceptionHandlerがキャッチ

---

## ✅ ステップ6: 動作確認

### 6-1. アプリケーション起動

```bash
cd workspace/hello-spring-boot
./mvnw spring-boot:run
```

---

### 6-2. 正常系のテスト

**全ユーザー取得（200 OK）**:

```bash
curl http://localhost:8080/api/users
```

**期待される結果**:
```json
[
  {
    "id": 1,
    "name": "Alice",
    "email": "alice@example.com",
    "age": 25
  },
  {
    "id": 2,
    "name": "Bob",
    "email": "bob@example.com",
    "age": 30
  }
]
```

---

**ユーザー作成（201 Created）**:

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Charlie",
    "email": "charlie@example.com",
    "age": 28
  }'
```

**期待される結果**:
```json
{
  "id": 3,
  "name": "Charlie",
  "email": "charlie@example.com",
  "age": 28
}
```

---

### 6-3. エラーケースのテスト

**存在しないユーザー取得（404 Not Found）**:

```bash
curl -i http://localhost:8080/api/users/999
```

**期待される結果**:
```
HTTP/1.1 404 
Content-Type: application/json

{
  "timestamp": "2025-01-15 12:34:56",
  "status": 404,
  "error": "Not Found",
  "message": "User not found with id: '999'",
  "path": "/api/users/999"
}
```

---

**不正な年齢でユーザー作成（400 Bad Request）**:

```bash
curl -i -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Invalid User",
    "email": "invalid@example.com",
    "age": -5
  }'
```

**期待される結果**:
```
HTTP/1.1 400 
Content-Type: application/json

{
  "timestamp": "2025-01-15 12:35:10",
  "status": 400,
  "error": "Bad Request",
  "message": "Age must be positive",
  "path": "/api/users"
}
```

---

**重複メールアドレスでユーザー作成（409 Conflict）**:

```bash
# 1回目（成功）
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "David",
    "email": "david@example.com",
    "age": 32
  }'

# 2回目（同じメールアドレス → エラー）
curl -i -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Another David",
    "email": "david@example.com",
    "age": 40
  }'
```

**期待される結果（2回目）**:
```
HTTP/1.1 409 
Content-Type: application/json

{
  "timestamp": "2025-01-15 12:36:20",
  "status": 409,
  "error": "Conflict",
  "message": "Email already exists: david@example.com",
  "path": "/api/users"
}
```

---

**存在しないユーザーを削除（404 Not Found）**:

```bash
curl -i -X DELETE http://localhost:8080/api/users/999
```

**期待される結果**:
```
HTTP/1.1 404 
Content-Type: application/json

{
  "timestamp": "2025-01-15 12:37:00",
  "status": 404,
  "error": "Not Found",
  "message": "User not found with id: '999'",
  "path": "/api/users/999"
}
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: バリデーションエラーの詳細化

現在の実装では、エラーメッセージが1つしか返りません。複数のバリデーションエラーを同時に返すように改善してください。

**要件**:
1. `ValidationErrorResponse`クラスを作成
2. 複数のフィールドエラーをリストで返す
3. エラーごとに`field`と`message`を含める

**期待されるレスポンス**:

```json
{
  "timestamp": "2025-01-15 12:40:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Input validation failed",
  "path": "/api/users",
  "errors": [
    {
      "field": "name",
      "message": "Name is required"
    },
    {
      "field": "age",
      "message": "Age must be positive"
    },
    {
      "field": "email",
      "message": "Email format is invalid"
    }
  ]
}
```

**ヒント**:

```java
@Data
@AllArgsConstructor
public class ValidationErrorResponse extends ErrorResponse {
    private List<FieldError> errors;
    
    @Data
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String message;
    }
}
```

---

### チャレンジ 2: 環境別のエラーメッセージ切り替え

本番環境（`application-prod.yml`）ではスタックトレースを隠し、開発環境（`application-dev.yml`）では詳細を表示するように実装してください。

**要件**:
1. `application.yml`に`app.show-stack-trace`プロパティを追加
2. 開発環境では`true`、本番環境では`false`
3. `GlobalExceptionHandler`でプロパティを読み取り、条件分岐

**ヒント**:

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @Value("${app.show-stack-trace:false}")
    private boolean showStackTrace;
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, HttpServletRequest request) {
        String message = showStackTrace ? ex.getMessage() : "An unexpected error occurred";
        
        // ...
    }
}
```

**application-dev.yml**:
```yaml
app:
  show-stack-trace: true
```

**application-prod.yml**:
```yaml
app:
  show-stack-trace: false
```

---

### チャレンジ 3: カスタムHTTPステータスコード

以下のビジネスロジック例外を作成し、適切なHTTPステータスコードを返してください：

| 例外クラス | HTTPステータス | 使用例 |
|---|---|---|
| `UnauthorizedException` | 401 Unauthorized | ログインしていない |
| `ForbiddenException` | 403 Forbidden | 権限がない |
| `UnprocessableEntityException` | 422 Unprocessable Entity | 論理的に処理不可 |

**ヒント**:

```java
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}

@ExceptionHandler(UnauthorizedException.class)
public ResponseEntity<ErrorResponse> handleUnauthorizedException(
    UnauthorizedException ex,
    HttpServletRequest request
) {
    ErrorResponse errorResponse = ErrorResponse.of(
        HttpStatus.UNAUTHORIZED.value(),
        HttpStatus.UNAUTHORIZED.getReasonPhrase(),
        ex.getMessage(),
        request.getRequestURI()
    );
    
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
}
```

---

## 🐛 トラブルシューティング

### エラー 1: "No qualifying bean of type 'ErrorResponse'"

**エラーメッセージ**:
```
No qualifying bean of type 'com.example.hellospringboot.dto.ErrorResponse' available
```

**原因**: `ErrorResponse`はDTOなので、Beanとして登録する必要はない

**解決策**: `@Component`などのアノテーションを付けない

```java
// ❌ 間違い
@Component
public class ErrorResponse { }

// ✅ 正しい
public class ErrorResponse { }
```

---

### エラー 2: "Handler dispatch failed: java.lang.StackOverflowError"

**原因**: `GlobalExceptionHandler`内で例外が発生し、無限ループになっている

**解決策**: `@ExceptionHandler(Exception.class)`メソッド内で例外をスローしない

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, HttpServletRequest request) {
    // ❌ ここで例外をスローすると無限ループ
    // throw new RuntimeException("Error handling failed");
    
    // ✅ ログ出力に留める
    ex.printStackTrace();
    
    ErrorResponse errorResponse = ErrorResponse.of(...);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
}
```

---

### エラー 3: "Content type 'application/json' not supported"

**エラーメッセージ**（curlでPOSTリクエスト時）:
```json
{
  "timestamp": "2025-01-15T12:50:00.123+00:00",
  "status": 415,
  "error": "Unsupported Media Type",
  "message": "Content type 'application/x-www-form-urlencoded' not supported",
  "path": "/api/users"
}
```

**原因**: `Content-Type`ヘッダーが指定されていない

**解決策**: curlに`-H "Content-Type: application/json"`を追加

```bash
# ❌ 間違い
curl -X POST http://localhost:8080/api/users -d '{"name":"Alice"}'

# ✅ 正しい
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com","age":25}'
```

---

### エラー 4: "Required request body is missing"

**エラーメッセージ**:
```json
{
  "timestamp": "2025-01-15 12:55:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Required request body is missing",
  "path": "/api/users"
}
```

**原因**: リクエストボディが空

**解決策**: `-d`オプションでJSONデータを送信

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com","age":25}'
```

---

### エラー 5: "NullPointerException at UserService.createUser"

**原因**: `user.getAge()`が`null`の場合、`user.getAge() < 0`でNullPointerException

**解決策**: null チェックを追加

```java
// ❌ 間違い
if (user.getAge() < 0) {
    throw new InvalidRequestException("Age must be positive");
}

// ✅ 正しい
if (user.getAge() != null && user.getAge() < 0) {
    throw new InvalidRequestException("Age must be positive");
}
```

---

## 📚 このステップで学んだこと

- ✅ **例外ハンドリングの重要性**: クライアントにわかりやすいエラーメッセージを返す
- ✅ **`@ControllerAdvice`**: アプリケーション全体の例外を一箇所でハンドリング
- ✅ **`@ExceptionHandler`**: 特定の例外をキャッチするメソッドに付与
- ✅ **カスタム例外クラス**: ビジネスロジックの異常を明確に表現
- ✅ **HTTPステータスコードの使い分け**: 404, 400, 409, 500など
- ✅ **ErrorResponse DTO**: 統一されたエラーレスポンス形式
- ✅ **ResponseEntity**: HTTPステータスとボディを自由に設定
- ✅ **環境別のエラーメッセージ**: 本番環境では詳細を隠す
- ✅ **バリデーションの実装**: 不正なデータを早期に検出
- ✅ **デバッグの効率化**: 明確なエラーメッセージでトラブルシューティングが容易

---

## 💡 補足: HTTPステータスコード一覧

### 成功（2xx）

| コード | 名前 | 意味 | 使用例 |
|---|---|---|---|
| 200 | OK | 成功 | GET, PUT, PATCHの成功 |
| 201 | Created | リソース作成成功 | POSTでの新規作成 |
| 204 | No Content | 成功（ボディなし） | DELETEの成功 |

---

### クライアントエラー（4xx）

| コード | 名前 | 意味 | 使用例 |
|---|---|---|---|
| 400 | Bad Request | 不正なリクエスト | バリデーションエラー |
| 401 | Unauthorized | 認証が必要 | ログインしていない |
| 403 | Forbidden | 権限不足 | アクセス権がない |
| 404 | Not Found | リソースが存在しない | 存在しないIDを指定 |
| 409 | Conflict | リソースの競合 | メールアドレス重複 |
| 422 | Unprocessable Entity | 論理的に処理不可 | 在庫不足で注文不可 |

---

### サーバーエラー（5xx）

| コード | 名前 | 意味 | 使用例 |
|---|---|---|---|
| 500 | Internal Server Error | サーバー内部エラー | 予期しない例外 |
| 503 | Service Unavailable | サービス利用不可 | メンテナンス中 |

---

## 💡 補足: 例外の設計指針

### 1. ビジネスロジックの例外 vs システムの例外

**ビジネスロジックの例外**:
- ユーザーの操作によって発生する予測可能な異常
- 例: ユーザーが存在しない、メールアドレスが重複、在庫不足
- 対応: カスタム例外クラスを作成し、適切なHTTPステータスコードを返す

**システムの例外**:
- プログラムのバグやインフラ障害による予測不可能な異常
- 例: NullPointerException, OutOfMemoryError, DB接続エラー
- 対応: 500 Internal Server Errorを返し、詳細はログに記録

---

### 2. チェック例外 vs 非チェック例外

**チェック例外（checked exception）**:
- `Exception`を継承
- メソッドシグネチャに`throws`宣言が必要
- 呼び出し側で必ず`try-catch`または`throws`が必要
- Spring Bootでは**推奨されない**（トランザクション自動ロールバックの対象外）

**非チェック例外（unchecked exception）**:
- `RuntimeException`を継承
- `throws`宣言不要
- 呼び出し側で`try-catch`は任意
- Spring Bootでは**推奨される**（トランザクション自動ロールバック）

**推奨**:

```java
// ✅ 推奨: RuntimeExceptionを継承
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

// ❌ 非推奨: Exceptionを継承
public class ResourceNotFoundException extends Exception {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

---

### 3. 例外メッセージの設計

**良い例外メッセージ**:
- 何が問題なのかが明確
- どう修正すればいいかのヒント
- 具体的な値を含む

**例**:

```java
// ❌ 悪い例
throw new ResourceNotFoundException("Not found");

// ✅ 良い例
throw new ResourceNotFoundException("User", "id", 999);
// メッセージ: "User not found with id: '999'"

// ✅ より良い例（修正方法のヒント）
throw new InvalidRequestException("Age must be positive. Provided: " + user.getAge());
```

---

## 🎓 発展: Spring Bootの例外ハンドリングの仕組み

### 1. 例外ハンドリングの優先順位

Spring Bootは以下の順序で例外ハンドラーを探します：

```
1. Controller内の@ExceptionHandler
   ↓（見つからなければ）
2. @ControllerAdvice内の@ExceptionHandler
   ↓（見つからなければ）
3. Spring BootのデフォルトエラーハンドリングD
   (/error エンドポイント)
```

**例**:

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    // このController内でのResourceNotFoundExceptionを処理
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        // UserController固有の処理
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(...);
    }
}
```

---

### 2. `@ControllerAdvice`の適用範囲を限定

特定のパッケージやControllerにのみ適用したい場合：

```java
// 特定のパッケージにのみ適用
@ControllerAdvice(basePackages = "com.example.hellospringboot.controllers")
public class GlobalExceptionHandler {
    // ...
}

// 特定のアノテーションが付いたControllerにのみ適用
@ControllerAdvice(annotations = RestController.class)
public class RestApiExceptionHandler {
    // ...
}

// 特定のControllerにのみ適用
@ControllerAdvice(assignableTypes = {UserController.class, ProductController.class})
public class UserProductExceptionHandler {
    // ...
}
```

---

### 3. `ResponseEntityExceptionHandler`を継承

Spring MVCが提供する標準的な例外を自動的にハンドリング：

```java
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    
    // Spring MVCの標準例外（HttpMessageNotReadableExceptionなど）は
    // 自動的にハンドリングされる
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
        ResourceNotFoundException ex,
        HttpServletRequest request
    ) {
        // カスタム例外のみ明示的にハンドリング
        ErrorResponse errorResponse = ErrorResponse.of(...);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
}
```

---

## 📖 参考資料

### 公式ドキュメント

- [Spring Boot - Error Handling](https://docs.spring.io/spring-boot/reference/web/servlet.html#web.servlet.spring-mvc.error-handling)
- [Spring Framework - Exception Handling](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html)
- [HTTP Status Codes](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status)

### 関連記事

- [Exception Handling in Spring Boot REST API](https://www.baeldung.com/exception-handling-for-rest-with-spring)
- [Custom Error Messages in Spring REST API](https://www.baeldung.com/global-error-handler-in-a-spring-rest-api)

---

## ➡️ 次のステップ

[Step 18: バリデーション](STEP_18.md)へ進みましょう！

次のステップでは、リクエストデータのバリデーションを学びます：

- `@Valid`と`@Validated`でリクエスト検証
- `@NotNull`, `@NotBlank`, `@Size`, `@Email`などのバリデーションアノテーション
- カスタムバリデーターの作成
- バリデーションエラーの詳細なレスポンス
- グループバリデーション（作成時と更新時で異なるルール）

入力データの妥当性を自動的にチェックし、不正なデータを早期に検出する仕組みを作りましょう！
