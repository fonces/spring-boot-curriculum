# Step 15: 例外ハンドリングとカスタム例外

## 🎯 このステップの目標

- カスタム例外を作成する
- 例外の階層構造を理解する
- `@RestControllerAdvice`で統一的な例外ハンドリングを実装する
- HTTPステータスコードを適切に使い分ける

**所要時間**: 約1時間30分

---

## 📋 事前準備

- Step 14のバリデーションが理解できていること
- GlobalExceptionHandlerが実装されていること

**Step 14をまだ完了していない場合**: [Step 14: バリデーション](STEP_14.md)を先に進めてください。

---

## 💡 例外ハンドリングとは？

### 例外ハンドリングの必要性

**例外ハンドリングなしの場合**:
```java
@GetMapping("/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id) {
    User user = userRepository.findById(id).get();  // NoSuchElementException!
    return ResponseEntity.ok(user);
}
```

**問題点**:
- ❌ 500 Internal Server Errorが返る
- ❌ スタックトレースが露出（セキュリティリスク）
- ❌ ユーザーに何が悪いのかわからない

### 適切な例外ハンドリング

```java
@GetMapping("/{id}")
public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
    return userService.getUserById(id)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new UserNotFoundException(id));
}
```

**メリット**:
- ✅ 404 Not Foundを返す
- ✅ わかりやすいエラーメッセージ
- ✅ ログに適切な情報を記録

---

## 🚀 ステップ1: カスタム例外の作成

### 1-1. 基底例外クラス

**ファイルパス**: `src/main/java/com/example/hellospringboot/exception/BusinessException.java`

```java
package com.example.hellospringboot.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * ビジネス例外の基底クラス
 */
@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String errorCode;

    public BusinessException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public BusinessException(String message, HttpStatus httpStatus) {
        this(message, httpStatus, null);
    }
}
```

### 1-2. ResourceNotFoundException

**ファイルパス**: `src/main/java/com/example/hellospringboot/exception/ResourceNotFoundException.java`

```java
package com.example.hellospringboot.exception;

import org.springframework.http.HttpStatus;

/**
 * リソースが見つからない場合の例外
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceName, Long id) {
        super(
            String.format("%s（ID: %d）が見つかりません", resourceName, id),
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND"
        );
    }

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
}
```

### 1-3. UserNotFoundException

**ファイルパス**: `src/main/java/com/example/hellospringboot/exception/UserNotFoundException.java`

```java
package com.example.hellospringboot.exception;

/**
 * ユーザーが見つからない場合の例外
 */
public class UserNotFoundException extends ResourceNotFoundException {

    public UserNotFoundException(Long id) {
        super("ユーザー", id);
    }

    public UserNotFoundException(String email) {
        super(String.format("ユーザー（メール: %s）が見つかりません", email));
    }
}
```

### 1-4. DuplicateResourceException

**ファイルパス**: `src/main/java/com/example/hellospringboot/exception/DuplicateResourceException.java`

```java
package com.example.hellospringboot.exception;

import org.springframework.http.HttpStatus;

/**
 * リソースが既に存在する場合の例外
 */
public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String resourceName, String fieldName, String value) {
        super(
            String.format("%sの%s「%s」は既に登録されています", resourceName, fieldName, value),
            HttpStatus.CONFLICT,
            "DUPLICATE_RESOURCE"
        );
    }

    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT, "DUPLICATE_RESOURCE");
    }
}
```

### 1-5. InvalidOperationException

**ファイルパス**: `src/main/java/com/example/hellospringboot/exception/InvalidOperationException.java`

```java
package com.example.hellospringboot.exception;

import org.springframework.http.HttpStatus;

/**
 * 不正な操作を行った場合の例外
 */
public class InvalidOperationException extends BusinessException {

    public InvalidOperationException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_OPERATION");
    }
}
```

---

## 🚀 ステップ2: GlobalExceptionHandlerの拡張

### 2-1. 詳細なエラーレスポンスDTO

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/response/ErrorResponse.java`

```java
package com.example.hellospringboot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * エラーレスポンスDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * エラーメッセージ
     */
    private String message;

    /**
     * エラーコード
     */
    private String errorCode;

    /**
     * HTTPステータスコード
     */
    private int status;

    /**
     * エラー発生時刻
     */
    private LocalDateTime timestamp;

    /**
     * リクエストパス
     */
    private String path;

    /**
     * フィールドごとのエラー詳細（バリデーションエラー用）
     */
    private Map<String, String> errors;

    /**
     * デバッグ情報（開発環境のみ）
     */
    private String debugMessage;
}
```

### 2-2. GlobalExceptionHandlerの完全版

**ファイルパス**: `src/main/java/com/example/hellospringboot/exception/GlobalExceptionHandler.java`

```java
package com.example.hellospringboot.exception;

import com.example.hellospringboot.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * グローバル例外ハンドラー
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * ビジネス例外のハンドリング
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request) {
        
        log.warn("Business exception occurred: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .status(ex.getHttpStatus().value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(errorResponse);
    }

    /**
     * バリデーションエラーのハンドリング
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        log.warn("Validation error occurred: {}", ex.getMessage());

        // フィールドごとのエラーメッセージを収集
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("入力値が正しくありません")
                .errorCode("VALIDATION_ERROR")
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .errors(errors)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * リソースが見つからない場合のハンドリング
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        
        log.warn("Resource not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .status(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }

    /**
     * その他の例外のハンドリング
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        
        log.error("Unexpected error occurred", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("サーバーエラーが発生しました")
                .errorCode("INTERNAL_SERVER_ERROR")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .debugMessage(ex.getMessage())  // 開発環境のみ含める
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }
}
```

---

## 🚀 ステップ3: Serviceレイヤーでの例外使用

### 3-1. UserServiceの更新

**ファイルパス**: `src/main/java/com/example/hellospringboot/service/UserService.java`

```java
package com.example.hellospringboot.service;

import com.example.hellospringboot.dto.request.UserCreateRequest;
import com.example.hellospringboot.dto.request.UserUpdateRequest;
import com.example.hellospringboot.dto.response.UserResponse;
import com.example.hellospringboot.entity.User;
import com.example.hellospringboot.exception.DuplicateResourceException;
import com.example.hellospringboot.exception.UserNotFoundException;
import com.example.hellospringboot.mapper.UserMapper;
import com.example.hellospringboot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * ユーザーを作成
     */
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        log.info("Creating user with email: {}", request.getEmail());

        // メールアドレスの重複チェック
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("ユーザー", "メールアドレス", request.getEmail());
        }

        User user = userMapper.toEntity(request);
        User savedUser = userRepository.save(user);
        
        log.info("User created successfully with ID: {}", savedUser.getId());
        return userMapper.toResponse(savedUser);
    }

    /**
     * 全ユーザーを取得
     */
    public List<UserResponse> getAllUsers() {
        log.info("Fetching all users");
        List<User> users = userRepository.findAll();
        return userMapper.toResponseList(users);
    }

    /**
     * IDでユーザーを取得
     */
    public UserResponse getUserById(Long id) {
        log.info("Fetching user with ID: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        
        return userMapper.toResponse(user);
    }

    /**
     * メールアドレスでユーザーを取得
     */
    public UserResponse getUserByEmail(String email) {
        log.info("Fetching user with email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        
        return userMapper.toResponse(user);
    }

    /**
     * ユーザーを更新
     */
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        log.info("Updating user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        // メールアドレスを変更する場合、重複チェック
        if (!user.getEmail().equals(request.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("ユーザー", "メールアドレス", request.getEmail());
            }
        }

        userMapper.updateEntity(user, request);
        User updatedUser = userRepository.save(user);
        
        log.info("User updated successfully with ID: {}", updatedUser.getId());
        return userMapper.toResponse(updatedUser);
    }

    /**
     * ユーザーを削除
     */
    @Transactional
    public void deleteUser(Long id) {
        log.info("Deleting user with ID: {}", id);

        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }

        userRepository.deleteById(id);
        log.info("User deleted successfully with ID: {}", id);
    }
}
```

### 3-2. UserRepositoryの追加メソッド

**ファイルパス**: `src/main/java/com/example/hellospringboot/repository/UserRepository.java`

```java
package com.example.hellospringboot.repository;

import com.example.hellospringboot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * メールアドレスでユーザーを検索
     */
    Optional<User> findByEmail(String email);

    /**
     * メールアドレスが存在するかチェック
     */
    boolean existsByEmail(String email);
}
```

---

## 🚀 ステップ4: Controllerの更新

### 4-1. UserControllerの更新

**ファイルパス**: `src/main/java/com/example/hellospringboot/controller/UserController.java`

```java
package com.example.hellospringboot.controller;

import com.example.hellospringboot.dto.request.UserCreateRequest;
import com.example.hellospringboot.dto.request.UserUpdateRequest;
import com.example.hellospringboot.dto.response.UserResponse;
import com.example.hellospringboot.service.UserService;
import jakarta.validation.Valid;
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
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 全ユーザー取得
     * GET /api/users
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * IDでユーザー取得
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * メールアドレスでユーザー取得
     * GET /api/users/email/{email}
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        UserResponse response = userService.getUserByEmail(email);
        return ResponseEntity.ok(response);
    }

    /**
     * ユーザー更新
     * PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(response);
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

---

## ✅ ステップ5: 動作確認

### 5-1. リソースが見つからない場合

```bash
curl http://localhost:8080/api/users/999
```

**期待されるレスポンス**:
```json
{
  "message": "ユーザー（ID: 999）が見つかりません",
  "errorCode": "RESOURCE_NOT_FOUND",
  "status": 404,
  "timestamp": "2025-10-27T10:30:00",
  "path": "/api/users/999"
}
```

### 5-2. 重複リソースの場合

```bash
# 同じメールアドレスで2回作成
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Taro", "email": "taro@example.com", "age": 30}'

curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Jiro", "email": "taro@example.com", "age": 25}'
```

**期待されるレスポンス（2回目）**:
```json
{
  "message": "ユーザーのメールアドレス「taro@example.com」は既に登録されています",
  "errorCode": "DUPLICATE_RESOURCE",
  "status": 409,
  "timestamp": "2025-10-27T10:30:00",
  "path": "/api/users"
}
```

---

## 🚀 ステップ6: HTTPステータスコードの使い分け

### 6-1. 主要なHTTPステータスコード

| コード | 意味 | 使用場面 |
|--------|------|----------|
| **200 OK** | 成功 | GET、PUT成功時 |
| **201 Created** | 作成成功 | POST成功時 |
| **204 No Content** | 成功（内容なし） | DELETE成功時 |
| **400 Bad Request** | 不正なリクエスト | バリデーションエラー |
| **401 Unauthorized** | 認証が必要 | ログインが必要 |
| **403 Forbidden** | 権限なし | アクセス権がない |
| **404 Not Found** | リソースなし | 存在しないID |
| **409 Conflict** | 競合 | 重複データ |
| **500 Internal Server Error** | サーバーエラー | 予期しないエラー |

### 6-2. 例外とステータスコードのマッピング

```java
// 404 Not Found
throw new UserNotFoundException(id);

// 409 Conflict
throw new DuplicateResourceException("ユーザー", "email", email);

// 400 Bad Request
throw new InvalidOperationException("削除できないユーザーです");
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: UnauthorizedException

認証エラー用の例外を作成してください。

**ヒント**:
```java
public class UnauthorizedException extends BusinessException {
    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }
}
```

### チャレンジ 2: RateLimitExceededException

APIレート制限用の例外を作成してください（HTTP 429 Too Many Requests）。

### チャレンジ 3: 環境別のエラーレスポンス

開発環境ではスタックトレースを含め、本番環境では除外するようにしてください。

**ヒント**:
```java
@Value("${spring.profiles.active}")
private String activeProfile;

if ("dev".equals(activeProfile)) {
    errorResponse.setDebugMessage(ex.getStackTrace());
}
```

---

## 🐛 トラブルシューティング

### 例外がハンドリングされない

**症状**: カスタム例外を投げても500エラーになる

**原因**: `@RestControllerAdvice`が認識されていない

**解決策**: パッケージスキャンの確認
```java
@SpringBootApplication
@ComponentScan(basePackages = "com.example.hellospringboot")
public class Application {
    // ...
}
```

### ログが出力されない

**解決策**: `application.yml`でログレベル設定
```yaml
logging:
  level:
    com.example.hellospringboot: DEBUG
```

---

## 📚 このステップで学んだこと

- ✅ カスタム例外の作成
- ✅ 例外の階層構造
- ✅ `@RestControllerAdvice`による統一的な例外ハンドリング
- ✅ HTTPステータスコードの使い分け
- ✅ ログ出力
- ✅ ユーザーフレンドリーなエラーレスポンス

---

## 💡 補足: 例外処理のベストプラクティス

### 例外の設計原則

1. **具体的な例外**: `UserNotFoundException` > `ResourceNotFoundException` > `Exception`
2. **ビジネス例外と技術例外を分離**: `BusinessException` vs `SQLException`
3. **チェック例外よりも非チェック例外**: Spring Bootでは`RuntimeException`を推奨

### ログレベルの使い分け

```java
log.error("致命的エラー", ex);       // システム障害
log.warn("想定内の異常", ex);         // ビジネス例外
log.info("通常の処理");              // 正常系ログ
log.debug("デバッグ情報");           // 開発時のみ
```

### パフォーマンス考慮

```java
// ❌ 悪い例: 例外を制御フローに使う
try {
    User user = userRepository.findById(id).get();
} catch (NoSuchElementException ex) {
    // 例外は遅い
}

// ✅ 良い例: Optional を活用
User user = userRepository.findById(id)
    .orElseThrow(() -> new UserNotFoundException(id));
```

---

## 🔄 Gitへのコミット

Phase 3の前半が完了です！

```bash
git add .
git commit -m "Phase 3: STEP_13-15完了（アーキテクチャ、バリデーション、例外ハンドリング）"
git push origin main
```

---

## ➡️ 次のステップ

次は[Step 16: ロギング](STEP_16.md)へ進みましょう！

効果的なログ出力と監視の基礎を学びます。

---

お疲れさまでした！ 🎉

例外ハンドリングは、ユーザー体験とデバッグ効率の両方を向上させる重要な要素です。
適切なHTTPステータスコードとわかりやすいエラーメッセージで、
より使いやすいAPIを提供できるようになりました！
