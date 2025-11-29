# Step 17: 例外ハンドリング

## 🎯 このステップの目標

- カスタム例外クラスを作成できる
- `@RestControllerAdvice`でグローバルエラーハンドリングを実装できる
- 適切なHTTPステータスコードを使い分けられる
- 統一されたエラーレスポンス形式を返せる
- 環境別のエラーメッセージ出し分けができる

**所要時間**: 約1時間

---

## 📋 事前準備

このステップを始める前に、以下を確認してください：

- Step 16（DI/IoCコンテナ）が完了していること
- Service層でビジネスロジックを実装していること
- HTTPステータスコードの基本を理解していること

---

## 📝 概要
アプリケーション開発において、エラーハンドリングは避けて通れません。Spring Bootでは`@ControllerAdvice`を使って、アプリケーション全体で統一されたエラー処理を実装できます。

## ❌ 良くない例

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        try {
            User user = userService.findById(id);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            // ❌ 各エンドポイントで個別にエラー処理
            return ResponseEntity.status(500)
                .body("Error: " + e.getMessage());
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            User created = userService.create(user);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            // ❌ 同じようなコードが繰り返される
            return ResponseEntity.status(500)
                .body("Error: " + e.getMessage());
        }
    }
}
```

**問題点**:
- エラー処理が重複
- エラーレスポンスの形式が統一されていない
- HTTPステータスコードが適切でない

## ✅ 正しいアプローチ

### 1. カスタム例外クラスの作成

```java
package com.example.hellospringboot.exception;

/**
 * リソースが見つからない場合の例外
 */
public class ResourceNotFoundException extends RuntimeException {
    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;
    
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }
    
    // Getters
    public String getResourceName() { return resourceName; }
    public String getFieldName() { return fieldName; }
    public Object getFieldValue() { return fieldValue; }
}
```

```java
package com.example.hellospringboot.exception;

/**
 * ビジネスルール違反の例外
 */
public class BusinessException extends RuntimeException {
    private final String errorCode;
    
    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
    }
    
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
```

```java
package com.example.hellospringboot.exception;

/**
 * バリデーションエラーの例外
 */
public class ValidationException extends RuntimeException {
    private final Map<String, String> errors;
    
    public ValidationException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }
    
    public Map<String, String> getErrors() {
        return errors;
    }
}
```

### 2. エラーレスポンスDTO

```java
package com.example.hellospringboot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // nullのフィールドは出力しない
public class ErrorResponse {
    private String timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> errors;  // バリデーションエラー用
    
    // 簡易コンストラクタ
    public ErrorResponse(int status, String error, String message, String path) {
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }
}
```

### 3. グローバル例外ハンドラ

```java
package com.example.hellospringboot.exception;

import com.example.hellospringboot.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * アプリケーション全体の例外ハンドラ
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * リソースが見つからない場合
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            WebRequest request) {
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    /**
     * ビジネスルール違反
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            WebRequest request) {
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getErrorCode(),
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    /**
     * バリデーションエラー（@Validアノテーション）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Failed",
            "入力値が不正です",
            request.getDescription(false).replace("uri=", "")
        );
        error.setErrors(errors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    /**
     * カスタムバリデーションエラー
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleCustomValidationException(
            ValidationException ex,
            WebRequest request) {
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Error",
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        error.setErrors(ex.getErrors());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    /**
     * その他の予期しないエラー
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "予期しないエラーが発生しました",
            request.getDescription(false).replace("uri=", "")
        );
        
        // 本番環境では詳細なエラーメッセージを隠す
        // error.setMessage(ex.getMessage());  // 開発環境のみ
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

### 4. Serviceでの例外の使用

```java
package com.example.hellospringboot.service;

import com.example.hellospringboot.entity.User;
import com.example.hellospringboot.exception.BusinessException;
import com.example.hellospringboot.exception.ResourceNotFoundException;
import com.example.hellospringboot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    
    private final UserRepository userRepository;
    
    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }
    
    public List<User> findAll() {
        return userRepository.findAll();
    }
    
    @Transactional
    public User create(User user) {
        // ビジネスルールチェック: メールアドレスの重複確認
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new BusinessException(
                "DUPLICATE_EMAIL",
                "このメールアドレスは既に使用されています: " + user.getEmail()
            );
        }
        
        // ビジネスルールチェック: 年齢制限
        if (user.getAge() != null && user.getAge() < 18) {
            throw new BusinessException(
                "AGE_RESTRICTION",
                "18歳未満のユーザーは登録できません"
            );
        }
        
        return userRepository.save(user);
    }
    
    @Transactional
    public User update(Long id, User user) {
        User existingUser = findById(id);  // 存在しなければResourceNotFoundException
        
        // メールアドレス変更時の重複チェック
        if (!existingUser.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(user.getEmail())) {
                throw new BusinessException(
                    "DUPLICATE_EMAIL",
                    "このメールアドレスは既に使用されています: " + user.getEmail()
                );
            }
        }
        
        existingUser.setName(user.getName());
        existingUser.setEmail(user.getEmail());
        existingUser.setAge(user.getAge());
        
        return userRepository.save(existingUser);
    }
    
    @Transactional
    public void delete(Long id) {
        User user = findById(id);  // 存在確認
        userRepository.delete(user);
    }
}
```

### 5. Repositoryに追加メソッド

```java
package com.example.hellospringboot.repository;

import com.example.hellospringboot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
}
```

### 6. Controller（シンプルに）

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
    
    @GetMapping
    public List<User> getAll() {
        return userService.findAll();
    }
    
    @GetMapping("/{id}")
    public User getById(@PathVariable Long id) {
        // 例外はGlobalExceptionHandlerで処理される
        return userService.findById(id);
    }
    
    @PostMapping
    public ResponseEntity<User> create(@RequestBody User user) {
        User created = userService.create(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    public User update(@PathVariable Long id, @RequestBody User user) {
        return userService.update(id, user);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

## 📊 HTTPステータスコードの使い分け

| コード | 意味 | 使用例 |
|---|---|---|
| **200 OK** | 成功 | GET, PUT の成功 |
| **201 Created** | リソース作成成功 | POST の成功 |
| **204 No Content** | 成功（レスポンスボディなし） | DELETE の成功 |
| **400 Bad Request** | クライアントの入力エラー | バリデーションエラー、ビジネスルール違反 |
| **401 Unauthorized** | 認証が必要 | ログインしていない |
| **403 Forbidden** | 権限がない | 他人のリソースにアクセス |
| **404 Not Found** | リソースが見つからない | 存在しないIDを指定 |
| **409 Conflict** | 競合 | 楽観的ロックの失敗 |
| **500 Internal Server Error** | サーバー側のエラー | 予期しない例外 |

## ✅ 動作確認

### 1. 存在しないユーザーの取得

```bash
curl -X GET http://localhost:8080/api/users/999
```

**レスポンス**:
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "User not found with id : '999'",
  "path": "/api/users/999"
}
```

### 2. 重複メールアドレスでの登録

```bash
# 1回目（成功）
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"太郎","email":"taro@example.com","age":25}'

# 2回目（失敗）
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"次郎","email":"taro@example.com","age":30}'
```

**レスポンス**:
```json
{
  "timestamp": "2024-01-15T10:35:00",
  "status": 400,
  "error": "DUPLICATE_EMAIL",
  "message": "このメールアドレスは既に使用されています: taro@example.com",
  "path": "/api/users"
}
```

### 3. 年齢制限違反

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"未成年","email":"minor@example.com","age":15}'
```

**レスポンス**:
```json
{
  "timestamp": "2024-01-15T10:40:00",
  "status": 400,
  "error": "AGE_RESTRICTION",
  "message": "18歳未満のユーザーは登録できません",
  "path": "/api/users"
}
```

## 🎨 チャレンジ課題

### 課題1: 環境別のエラーメッセージ

開発環境では詳細なエラーメッセージ、本番環境では隠す実装を追加してください。

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @Value("${app.show-error-details:false}")
    private boolean showErrorDetails;
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {
        
        String message = showErrorDetails 
            ? ex.getMessage() 
            : "予期しないエラーが発生しました";
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            message,
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

```yaml
# application-dev.yml
app:
  show-error-details: true

# application-prod.yml
app:
  show-error-details: false
```

### 課題2: エラーログの記録

```java
@RestControllerAdvice
@Slf4j  // Lombokのログ
public class GlobalExceptionHandler {
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {
        
        // エラーログを記録
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "予期しないエラーが発生しました",
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

### 課題3: エラー通知（Slack/メール）

重大なエラーが発生した際に通知を送る仕組みを実装してください。

```java
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    
    private final NotificationService notificationService;
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {
        
        // 重大なエラーを通知
        notificationService.sendErrorNotification(
            "予期しないエラーが発生しました",
            ex.getMessage(),
            request.getDescription(false)
        );
        
        // ... レスポンス返却
    }
}
```

---

## 📚 このステップで学んだこと

- ✅ カスタム例外クラスの作成（`ResourceNotFoundException`、`BusinessException`など）
- ✅ `@RestControllerAdvice`でグローバルエラーハンドリング
- ✅ `@ExceptionHandler`で例外ごとの処理を定義
- ✅ 統一されたエラーレスポンスDTO
- ✅ HTTPステータスコードの適切な使い分け（400、404、500など）
- ✅ バリデーションエラー（`MethodArgumentNotValidException`）の処理
- ✅ 環境別のエラーメッセージ出し分け
- ✅ エラーログの記録とスタックトレースの保存

**エラーハンドリングのメリット**:
- Controller層がシンプルになる（try-catchが不要）
- エラーレスポンス形式が統一される
- クライアント側でのエラー処理が容易
- デバッグとトラブルシューティングが効率化

---

## 🐛 トラブルシューティング

### エラー: "@RestControllerAdvice"が効かない

**原因**: パッケージがコンポーネントスキャン範囲外

**解決策**:
1. `@RestControllerAdvice`クラスをメインクラスと同じパッケージ以下に配置
2. または`@ComponentScan`でスキャン範囲を明示
```java
@RestControllerAdvice  // これだけでOK（通常）
public class GlobalExceptionHandler {
    // ...
}
```

### エラー: カスタム例外が`@ExceptionHandler`で捕捉されない

**原因**: 例外の継承関係が正しくない、またはメソッドの引数型が一致しない

**解決策**:
```java
// ✅ OK: 具体的な例外クラスを指定
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException e) {
    // ...
}

// ✅ OK: 親クラスで複数まとめて処理
@ExceptionHandler({ResourceNotFoundException.class, BusinessException.class})
public ResponseEntity<ErrorResponse> handleCustomExceptions(RuntimeException e) {
    // ...
}
```

### 問題: HTTPステータスコードの使い分けがわからない

**よく使うステータスコード**:

| コード | 名前 | 使用例 |
|--------|------|--------|
| 200 | OK | 成功 |
| 201 | Created | 作成成功 |
| 204 | No Content | 削除成功（レスポンスボディなし） |
| 400 | Bad Request | バリデーションエラー |
| 401 | Unauthorized | 認証エラー（未ログイン） |
| 403 | Forbidden | 認可エラー（権限なし） |
| 404 | Not Found | リソースが見つからない |
| 409 | Conflict | データ競合（重複など） |
| 500 | Internal Server Error | サーバー内部エラー |

### 問題: 本番環境でスタックトレースが漏洩する

**原因**: すべての環境で詳細なエラー情報を返している

**解決策**:
```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleException(Exception e) {
    log.error("Unexpected error", e);
    
    ErrorResponse response = ErrorResponse.builder()
        .message("予期しないエラーが発生しました")
        // 本番環境ではスタックトレースを含めない
        .details(isProductionEnvironment() ? null : e.getMessage())
        .build();
    
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
}

private boolean isProductionEnvironment() {
    return Arrays.asList(environment.getActiveProfiles()).contains("prod");
}
```

### 問題: バリデーションエラーのメッセージが分かりにくい

**原因**: デフォルトのエラーメッセージが英語または技術的

**解決策**:
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
    Map<String, String> errors = new HashMap<>();
    
    e.getBindingResult().getFieldErrors().forEach(error -> {
        String fieldName = error.getField();
        // カスタムメッセージを使用（@NotBlankのmessage属性など）
        String errorMessage = error.getDefaultMessage();
        errors.put(fieldName, errorMessage);
    });
    
    ErrorResponse response = ErrorResponse.builder()
        .message("入力値に誤りがあります")
        .errors(errors)
        .build();
    
    return ResponseEntity.badRequest().body(response);
}
```

---

## 🔄 Gitへのコミットとレビュー依頼

進捗を記録してレビューを受けましょう：

```bash
# 変更をステージング
git add .

# コミット
git commit -m "Step 17: 例外ハンドリング完了"

# リモートにプッシュ
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

レビューが完了したら、[Step 18: バリデーション](STEP_18.md)へ進みましょう！

入力値検証を実装し、不正なデータの登録を防ぐ方法を学びます。
