# Step 18: バリデーション

## 🎯 このステップの目標

- リクエストデータのバリデーションの重要性を理解できる
- Jakarta Bean Validation（旧Java Bean Validation）の基本アノテーションを使いこなせる
- `@Valid`と`@Validated`を使ったリクエスト検証を実装できる
- カスタムバリデーターを作成し、独自のビジネスルールを検証できる
- バリデーションエラーを適切にハンドリングし、わかりやすいエラーメッセージを返せる

**所要時間**: 約55分

---

## 📋 事前準備

- [Step 17: 例外ハンドリング](STEP_17.md)が完了していること
- `GlobalExceptionHandler`でエラーハンドリングを実装していること
- DTOパターンの基本を理解していること（詳細はStep 19で学習）
- 正規表現の基本知識があると望ましい

---

## 🔍 なぜバリデーションが必要なのか

### Before（バリデーションなし）

現在の実装では、以下のような問題があります：

**UserController.java**:
```java
@PostMapping
public ResponseEntity<User> createUser(@RequestBody User user) {
    User createdUser = userService.createUser(user);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
}
```

**問題のあるリクエスト例**:

```bash
# 名前が空
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"","email":"test@example.com","age":25}'

# メールアドレスの形式が不正
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"invalid-email","age":25}'

# 年齢が異常値
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Bob","email":"bob@example.com","age":999}'
```

**問題点**:
- ❌ 空の名前やメールアドレスがデータベースに保存される
- ❌ 不正なメール形式が通ってしまう
- ❌ 異常な年齢値（999歳）が許容される
- ❌ バリデーションロジックがServiceやControllerに散在
- ❌ エラーメッセージが統一されていない

---

### After（バリデーションあり）

理想的な実装：

```java
@PostMapping
public ResponseEntity<User> createUser(@Valid @RequestBody UserCreateRequest request) {
    User createdUser = userService.createUser(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
}
```

**バリデーションエラーのレスポンス**:

```json
{
  "timestamp": "2025-01-15 13:00:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Input validation failed",
  "path": "/api/users",
  "errors": [
    {
      "field": "name",
      "rejectedValue": "",
      "message": "Name is required"
    },
    {
      "field": "email",
      "rejectedValue": "invalid-email",
      "message": "Email format is invalid"
    },
    {
      "field": "age",
      "rejectedValue": 999,
      "message": "Age must be between 0 and 150"
    }
  ]
}
```

**改善点**:
- ✅ 不正なデータを**Controller層でブロック**
- ✅ **宣言的なバリデーション**（アノテーションだけで完結）
- ✅ エラーメッセージが**わかりやすく統一**
- ✅ 複数のバリデーションエラーを**一度に返却**
- ✅ フィールドごとのエラー詳細を提供

---

## 🚀 ステップ1: 依存関係の確認

### 1-1. pom.xmlの確認

Spring Bootには`spring-boot-starter-web`に`jakarta.validation`が含まれているため、追加の依存関係は不要です。

`pom.xml`を確認します：

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <!-- この中にjakarta.validation-apiが含まれている -->
    </dependency>
    
    <!-- その他の依存関係 -->
</dependencies>
```

**確認方法**:

```bash
cd workspace/hello-spring-boot
./mvnw dependency:tree | grep validation
```

**期待される出力**:
```sh
[INFO] |  +- org.hibernate.validator:hibernate-validator:jar:8.0.1.Final:compile
[INFO] |  |  +- jakarta.validation:jakarta.validation-api:jar:3.0.2:compile
```

---

## 🚀 ステップ2: DTOクラスの作成

### 2-1. dtoパッケージの作成

`src/main/java/com/example/hellospringboot/dto/`ディレクトリを作成します。

---

### 2-2. UserCreateRequestの作成

以下のファイルを`src/main/java/com/example/hellospringboot/dto/UserCreateRequest.java`に作成します：

```java
package com.example.hellospringboot.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ユーザー作成リクエストDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {
    
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    private String email;
    
    @NotNull(message = "Age is required")
    @Min(value = 0, message = "Age must be at least 0")
    @Max(value = 150, message = "Age must be at most 150")
    private Integer age;
}
```

---

### 2-3. バリデーションアノテーションの解説

#### `@NotNull`
- **用途**: nullでないことを検証
- **対象**: すべての型
- **例**: `@NotNull private Integer age;`

#### `@NotEmpty`
- **用途**: nullでなく、空でないことを検証
- **対象**: 文字列、コレクション、配列、Map
- **例**: `@NotEmpty private String name;`
- **注意**: 空白文字のみの文字列（" "）は通ってしまう

#### `@NotBlank`
- **用途**: nullでなく、空でなく、空白のみでもないことを検証
- **対象**: 文字列のみ
- **例**: `@NotBlank private String name;`
- **推奨**: 文字列には`@NotEmpty`より`@NotBlank`を使う

#### `@Size`
- **用途**: 文字列の長さやコレクションのサイズを検証
- **パラメータ**: `min`, `max`
- **例**: `@Size(min = 2, max = 50) private String name;`

#### `@Min` / `@Max`
- **用途**: 数値の最小値/最大値を検証
- **対象**: 数値型（Integer, Long, Double など）
- **例**: `@Min(0) @Max(150) private Integer age;`

#### `@Email`
- **用途**: メールアドレス形式を検証
- **正規表現**: RFC 5322に準拠
- **例**: `@Email private String email;`

#### `@Pattern`
- **用途**: 正規表現パターンに一致することを検証
- **例**: `@Pattern(regexp = "^[0-9]{3}-[0-9]{4}-[0-9]{4}$") private String phoneNumber;`

---

### 2-4. UserUpdateRequestの作成

以下のファイルを`src/main/java/com/example/hellospringboot/dto/UserUpdateRequest.java`に作成します：

```java
package com.example.hellospringboot.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ユーザー更新リクエストDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    private String email;
    
    @NotNull(message = "Age is required")
    @Min(value = 0, message = "Age must be at least 0")
    @Max(value = 150, message = "Age must be at most 150")
    private Integer age;
}
```

---

### 2-5. UserResponseの作成

以下のファイルを`src/main/java/com/example/hellospringboot/dto/UserResponse.java`に作成します：

```java
package com.example.hellospringboot.dto;

import com.example.hellospringboot.entities.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ユーザーレスポンスDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private Integer age;
    
    /**
     * EntityからDTOに変換するファクトリメソッド
     */
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getAge()
        );
    }
}
```

---

## 🚀 ステップ3: Controllerでバリデーションを有効化

### 3-1. UserControllerの修正

既存の`src/main/java/com/example/hellospringboot/controllers/UserController.java`を修正します：

```java
package com.example.hellospringboot.controllers;

import com.example.hellospringboot.dto.UserCreateRequest;
import com.example.hellospringboot.dto.UserUpdateRequest;
import com.example.hellospringboot.dto.UserResponse;
import com.example.hellospringboot.entities.User;
import com.example.hellospringboot.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserResponse> responses = users.stream()
            .map(UserResponse::from)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    /**
     * ユーザー詳細取得
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(UserResponse.from(user));
    }
    
    /**
     * ユーザー作成
     * POST /api/users
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        User user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }
    
    /**
     * ユーザー更新
     * PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
        @PathVariable Long id,
        @Valid @RequestBody UserUpdateRequest request
    ) {
        User user = userService.updateUser(id, request);
        return ResponseEntity.ok(UserResponse.from(user));
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

### 3-2. コードの解説

#### `@Valid`
- リクエストボディのバリデーションを有効化
- `@RequestBody`、`@ModelAttribute`、`@PathVariable`などに付与
- バリデーションエラーが発生すると`MethodArgumentNotValidException`がスロー

#### `@Validated`
- Spring独自のアノテーション
- グループバリデーション（後述）で使用
- クラスレベルでも使用可能

**使い分け**:
- 基本的には`@Valid`を使用
- グループバリデーションが必要な場合のみ`@Validated`

---

## 🚀 ステップ4: Serviceの修正

### 4-1. UserServiceの修正

既存の`src/main/java/com/example/hellospringboot/services/UserService.java`を修正します：

```java
package com.example.hellospringboot.services;

import com.example.hellospringboot.dto.UserCreateRequest;
import com.example.hellospringboot.dto.UserUpdateRequest;
import com.example.hellospringboot.entities.User;
import com.example.hellospringboot.exceptions.ResourceNotFoundException;
import com.example.hellospringboot.exceptions.ConflictException;
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
    
    public User createUser(UserCreateRequest request) {
        // メールアドレスの重複チェック
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists: " + request.getEmail());
        }
        
        // DTOからEntityに変換
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setAge(request.getAge());
        
        userRepository.save(user);
        return user;
    }
    
    public User updateUser(Long id, UserUpdateRequest request) {
        User existingUser = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        // 既存ユーザーの情報を更新
        existingUser.setName(request.getName());
        existingUser.setEmail(request.getEmail());
        existingUser.setAge(request.getAge());
        
        userRepository.save(existingUser);
        return existingUser;
    }
    
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", "id", id));
        }
        
        userRepository.deleteById(id);
    }
}
```

**ポイント**:
- バリデーションはControllerで行われるため、Serviceでの年齢チェックは不要
- DTOからEntityへの変換をServiceで実施

---

## 🚀 ステップ5: バリデーションエラーのハンドリング

### 5-1. ValidationErrorResponseの作成

以下のファイルを`src/main/java/com/example/hellospringboot/dto/ValidationErrorResponse.java`に作成します：

```java
package com.example.hellospringboot.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * バリデーションエラーレスポンス
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorResponse {
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private int status;
    
    private String error;
    
    private String message;
    
    private String path;
    
    private List<FieldError> errors = new ArrayList<>();
    
    /**
     * フィールドエラーの詳細
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FieldError {
        private String field;
        private Object rejectedValue;
        private String message;
    }
    
    /**
     * バリデーションエラーレスポンスを生成するファクトリメソッド
     */
    public static ValidationErrorResponse of(int status, String error, String message, String path) {
        ValidationErrorResponse response = new ValidationErrorResponse();
        response.setTimestamp(LocalDateTime.now());
        response.setStatus(status);
        response.setError(error);
        response.setMessage(message);
        response.setPath(path);
        response.setErrors(new ArrayList<>());
        return response;
    }
    
    /**
     * フィールドエラーを追加
     */
    public void addFieldError(String field, Object rejectedValue, String message) {
        errors.add(new FieldError(field, rejectedValue, message));
    }
}
```

---

### 5-2. GlobalExceptionHandlerに追加

既存の`src/main/java/com/example/hellospringboot/exceptions/GlobalExceptionHandler.java`に以下を追加します：

```java
package com.example.hellospringboot.exceptions;

import com.example.hellospringboot.dto.ErrorResponse;
import com.example.hellospringboot.dto.ValidationErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * アプリケーション全体の例外をハンドリングするグローバルハンドラー
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * バリデーションエラーをハンドリング
     * HTTPステータス: 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        ValidationErrorResponse errorResponse = ValidationErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Failed",
            "Input validation failed",
            request.getRequestURI()
        );
        
        // すべてのフィールドエラーを追加
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            Object rejectedValue = ((FieldError) error).getRejectedValue();
            String errorMessage = error.getDefaultMessage();
            errorResponse.addFieldError(fieldName, rejectedValue, errorMessage);
        });
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse);
    }
    
    /**
     * ResourceNotFoundException をハンドリング
     * HTTPステータス: 404 Not Found
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
        ResourceNotFoundException ex,
        HttpServletRequest request
    ) {
        ErrorResponse errorResponse = ErrorResponse.of(
            HttpStatus.NOT_FOUND.value(),
            HttpStatus.NOT_FOUND.getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(errorResponse);
    }
    
    /**
     * BadRequestException をハンドリング
     * HTTPステータス: 400 Bad Request
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(
        BadRequestException ex,
        HttpServletRequest request
    ) {
        ErrorResponse errorResponse = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse);
    }
    
    /**
     * ConflictException をハンドリング
     * HTTPステータス: 409 Conflict
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(
        ConflictException ex,
        HttpServletRequest request
    ) {
        ErrorResponse errorResponse = ErrorResponse.of(
            HttpStatus.CONFLICT.value(),
            HttpStatus.CONFLICT.getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
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
        ErrorResponse errorResponse = ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
            "An unexpected error occurred",
            request.getRequestURI()
        );
        
        ex.printStackTrace();
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorResponse);
    }
}
```

---

### 5-3. コードの解説

#### `MethodArgumentNotValidException`
- `@Valid`でバリデーションエラーが発生した際にスローされる例外
- `BindingResult`オブジェクトにすべてのエラー情報が含まれる

#### `ex.getBindingResult().getAllErrors()`
- すべてのバリデーションエラーを取得
- 各エラーは`FieldError`型

#### `FieldError`
- フィールド名（`getField()`）
- 拒否された値（`getRejectedValue()`）
- エラーメッセージ（`getDefaultMessage()`）

---

## ✅ ステップ6: 動作確認

### 6-1. アプリケーション起動

```bash
cd workspace/hello-spring-boot
./mvnw spring-boot:run
```

---

### 6-2. 正常系のテスト

**正常なユーザー作成（201 Created）**:

```bash
curl -i -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice",
    "email": "alice@example.com",
    "age": 25
  }'
```

**期待される結果**:
```sh
HTTP/1.1 201 
Content-Type: application/json

{
  "id": 1,
  "name": "Alice",
  "email": "alice@example.com",
  "age": 25
}
```

---

### 6-3. バリデーションエラーのテスト

**名前が空（400 Bad Request）**:

```bash
curl -i -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "",
    "email": "test@example.com",
    "age": 25
  }'
```

**期待される結果**:
```sh
HTTP/1.1 400 
Content-Type: application/json

{
  "timestamp": "2025-01-15 13:10:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Input validation failed",
  "path": "/api/users",
  "errors": [
    {
      "field": "name",
      "rejectedValue": "",
      "message": "Name is required"
    }
  ]
}
```

---

**メールアドレス形式が不正（400 Bad Request）**:

```bash
curl -i -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Bob",
    "email": "invalid-email",
    "age": 30
  }'
```

**期待される結果**:
```json
{
  "timestamp": "2025-01-15 13:12:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Input validation failed",
  "path": "/api/users",
  "errors": [
    {
      "field": "email",
      "rejectedValue": "invalid-email",
      "message": "Email format is invalid"
    }
  ]
}
```

---

**年齢が範囲外（400 Bad Request）**:

```bash
curl -i -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Charlie",
    "email": "charlie@example.com",
    "age": 999
  }'
```

**期待される結果**:
```json
{
  "timestamp": "2025-01-15 13:15:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Input validation failed",
  "path": "/api/users",
  "errors": [
    {
      "field": "age",
      "rejectedValue": 999,
      "message": "Age must be at most 150"
    }
  ]
}
```

---

**複数のバリデーションエラー（400 Bad Request）**:

```bash
curl -i -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "",
    "email": "invalid",
    "age": -10
  }'
```

**期待される結果**:
```json
{
  "timestamp": "2025-01-15 13:20:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Input validation failed",
  "path": "/api/users",
  "errors": [
    {
      "field": "name",
      "rejectedValue": "",
      "message": "Name is required"
    },
    {
      "field": "email",
      "rejectedValue": "invalid",
      "message": "Email format is invalid"
    },
    {
      "field": "age",
      "rejectedValue": -10,
      "message": "Age must be at least 0"
    }
  ]
}
```

---

## 🚀 ステップ7: カスタムバリデーターの作成

### 7-1. カスタムアノテーションの作成

電話番号のフォーマット検証を例に、カスタムバリデーターを作成します。

以下のファイルを`src/main/java/com/example/hellospringboot/validation/PhoneNumber.java`に作成します：

```java
package com.example.hellospringboot.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 電話番号フォーマットを検証するカスタムアノテーション
 * 形式: XXX-XXXX-XXXX
 */
@Documented
@Constraint(validatedBy = PhoneNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PhoneNumber {
    
    String message() default "Phone number format is invalid (expected: XXX-XXXX-XXXX)";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
```

---

### 7-2. Validatorクラスの作成

以下のファイルを`src/main/java/com/example/hellospringboot/validation/PhoneNumberValidator.java`に作成します：

```java
package com.example.hellospringboot.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * PhoneNumberアノテーションのValidator実装
 */
public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {
    
    // 電話番号の正規表現パターン（XXX-XXXX-XXXX）
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{3}-\\d{4}-\\d{4}$");
    
    @Override
    public void initialize(PhoneNumber constraintAnnotation) {
        // 初期化処理（必要に応じて）
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // nullは@NotNullで検証するため、ここではtrueを返す
        if (value == null) {
            return true;
        }
        
        // 正規表現パターンに一致するか検証
        return PHONE_PATTERN.matcher(value).matches();
    }
}
```

---

### 7-3. DTOで使用

以下のファイルを`src/main/java/com/example/hellospringboot/dto/UserProfileUpdateRequest.java`に作成します：

```java
package com.example.hellospringboot.dto;

import com.example.hellospringboot.validation.PhoneNumber;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ユーザープロフィール更新リクエストDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {
    
    @NotBlank(message = "Name is required")
    private String name;
    
    @PhoneNumber  // カスタムバリデーション
    private String phoneNumber;
}
```

---

### 7-4. 動作確認

**正常な電話番号**:

```bash
curl -i -X PUT http://localhost:8080/api/users/1/profile \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice",
    "phoneNumber": "090-1234-5678"
  }'
```

**不正な電話番号**:

```bash
curl -i -X PUT http://localhost:8080/api/users/1/profile \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice",
    "phoneNumber": "090-12345678"
  }'
```

**期待される結果**:
```json
{
  "timestamp": "2025-01-15 13:30:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Input validation failed",
  "path": "/api/users/1/profile",
  "errors": [
    {
      "field": "phoneNumber",
      "rejectedValue": "090-12345678",
      "message": "Phone number format is invalid (expected: XXX-XXXX-XXXX)"
    }
  ]
}
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: パスワード強度の検証

以下の要件を満たすパスワードバリデーターを作成してください：

**要件**:
- 最低8文字以上
- 英大文字、英小文字、数字、記号をそれぞれ1文字以上含む

**ヒント**:

```java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StrongPasswordValidator.class)
public @interface StrongPassword {
    String message() default "Password must contain at least 8 characters, including uppercase, lowercase, number, and special character";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

```java
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return true;
        }
        
        return password.length() >= 8
            && password.matches(".*[A-Z].*")  // 英大文字
            && password.matches(".*[a-z].*")  // 英小文字
            && password.matches(".*\\d.*")    // 数字
            && password.matches(".*[!@#$%^&*].*");  // 記号
    }
}
```

---

### チャレンジ 2: グループバリデーション

作成時と更新時で異なるバリデーションルールを適用してください。

**要件**:
- 作成時: パスワード必須
- 更新時: パスワード任意

**ヒント**:

```java
public interface CreateGroup {}
public interface UpdateGroup {}

@Data
public class UserRequest {
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotBlank(message = "Password is required", groups = CreateGroup.class)
    @StrongPassword(groups = {CreateGroup.class, UpdateGroup.class})
    private String password;
}

// Controller
@PostMapping
public ResponseEntity<UserResponse> createUser(
    @Validated(CreateGroup.class) @RequestBody UserRequest request
) {
    // ...
}

@PutMapping("/{id}")
public ResponseEntity<UserResponse> updateUser(
    @PathVariable Long id,
    @Validated(UpdateGroup.class) @RequestBody UserRequest request
) {
    // ...
}
```

---

### チャレンジ 3: 条件付きバリデーション

特定の条件下でのみバリデーションを実行する実装をしてください。

**要件**:
- `paymentMethod`が"credit"の場合のみ`creditCardNumber`を必須にする

**ヒント**:

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ConditionalValidator.class)
public @interface ConditionalValidation {
    String message() default "Validation failed";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

@ConditionalValidation
@Data
public class PaymentRequest {
    private String paymentMethod;
    private String creditCardNumber;
}

public class ConditionalValidator implements ConstraintValidator<ConditionalValidation, PaymentRequest> {
    @Override
    public boolean isValid(PaymentRequest request, ConstraintValidatorContext context) {
        if ("credit".equals(request.getPaymentMethod())) {
            return request.getCreditCardNumber() != null && !request.getCreditCardNumber().isBlank();
        }
        return true;
    }
}
```

---

## 🐛 トラブルシューティング

### エラー 1: "jakarta.validation.ValidationException: HV000030: No validator could be found"

**エラーメッセージ**:
```sh
jakarta.validation.ValidationException: HV000030: No validator could be found for constraint 'jakarta.validation.constraints.Email'
```

**原因**: `hibernate-validator`が依存関係に含まれていない

**解決策**: `pom.xml`に追加（通常は`spring-boot-starter-web`に含まれる）

```xml
<dependency>
    <groupId>org.hibernate.validator</groupId>
    <artifactId>hibernate-validator</artifactId>
</dependency>
```

---

### エラー 2: "Field error in object 'userCreateRequest' on field 'age': rejected value [null]"

**原因**: `@NotNull`が付いているフィールドにnullが渡された

**解決策**: リクエストにフィールドを含めるか、`@NotNull`を外す

```bash
# ❌ ageフィールドがない
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com"}'

# ✅ ageフィールドを含める
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com","age":25}'
```

---

### エラー 3: "カスタムバリデーションが動作しない"

**原因**: `@Constraint`アノテーションの`validatedBy`に正しいValidatorクラスを指定していない

**解決策**:

```java
// ✅ 正しい
@Constraint(validatedBy = PhoneNumberValidator.class)
public @interface PhoneNumber {
}

// ❌ 間違い（validatedByがない）
public @interface PhoneNumber {
}
```

---

### エラー 4: "バリデーションエラーが返らず、500エラーになる"

**原因**: `@Valid`をControllerに付けていない

**解決策**:

```java
// ❌ 間違い
@PostMapping
public ResponseEntity<UserResponse> createUser(@RequestBody UserCreateRequest request) {
}

// ✅ 正しい
@PostMapping
public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
}
```

---

## 📚 このステップで学んだこと

- ✅ **Jakarta Bean Validation**: 標準的なバリデーションAPI
- ✅ **`@Valid`アノテーション**: リクエストボディのバリデーション有効化
- ✅ **基本的なバリデーションアノテーション**: `@NotNull`, `@NotBlank`, `@Size`, `@Email`, `@Min`, `@Max`
- ✅ **カスタムバリデーター**: 独自のビジネスルールを実装
- ✅ **ValidationErrorResponse**: バリデーションエラーの統一フォーマット
- ✅ **MethodArgumentNotValidException**: バリデーションエラーのハンドリング
- ✅ **グループバリデーション**: 作成時と更新時で異なるルール
- ✅ **条件付きバリデーション**: 特定条件下でのみ検証
- ✅ **エラーメッセージのカスタマイズ**: わかりやすいメッセージ
- ✅ **DTOパターン**: Entityと分離したリクエスト/レスポンス

---

## 💡 補足: バリデーションアノテーション一覧

### 文字列検証

| アノテーション | 説明 | 例 |
|---|---|---|
| `@NotNull` | nullでない | `@NotNull private String name;` |
| `@NotEmpty` | nullでなく、空でない | `@NotEmpty private String name;` |
| `@NotBlank` | nullでなく、空白のみでもない | `@NotBlank private String name;` |
| `@Size(min, max)` | 長さ制約 | `@Size(min=2, max=50) private String name;` |
| `@Email` | メール形式 | `@Email private String email;` |
| `@Pattern(regexp)` | 正規表現一致 | `@Pattern(regexp="^[A-Z].*") private String code;` |

---

### 数値検証

| アノテーション | 説明 | 例 |
|---|---|---|
| `@Min(value)` | 最小値 | `@Min(0) private Integer age;` |
| `@Max(value)` | 最大値 | `@Max(150) private Integer age;` |
| `@Positive` | 正の数 | `@Positive private Integer count;` |
| `@PositiveOrZero` | 0または正の数 | `@PositiveOrZero private Integer stock;` |
| `@Negative` | 負の数 | `@Negative private Integer debt;` |
| `@NegativeOrZero` | 0または負の数 | `@NegativeOrZero private Integer balance;` |
| `@DecimalMin(value)` | 小数の最小値 | `@DecimalMin("0.0") private Double price;` |
| `@DecimalMax(value)` | 小数の最大値 | `@DecimalMax("100.0") private Double discount;` |
| `@Digits(integer, fraction)` | 整数部と小数部の桁数 | `@Digits(integer=5, fraction=2) private Double amount;` |

---

### 日付検証

| アノテーション | 説明 | 例 |
|---|---|---|
| `@Past` | 過去の日付 | `@Past private LocalDate birthDate;` |
| `@PastOrPresent` | 過去または現在 | `@PastOrPresent private LocalDate registeredAt;` |
| `@Future` | 未来の日付 | `@Future private LocalDate expiryDate;` |
| `@FutureOrPresent` | 未来または現在 | `@FutureOrPresent private LocalDate deliveryDate;` |

---

### コレクション検証

| アノテーション | 説明 | 例 |
|---|---|---|
| `@Size(min, max)` | 要素数制約 | `@Size(min=1, max=10) private List<String> tags;` |
| `@NotEmpty` | 空でない | `@NotEmpty private List<String> items;` |

---

### ブール検証

| アノテーション | 説明 | 例 |
|---|---|---|
| `@AssertTrue` | trueであること | `@AssertTrue private boolean accepted;` |
| `@AssertFalse` | falseであること | `@AssertFalse private boolean deleted;` |

---

## 📖 参考資料

### 公式ドキュメント

- [Jakarta Bean Validation](https://beanvalidation.org/)
- [Hibernate Validator Documentation](https://hibernate.org/validator/documentation/)
- [Spring Boot - Validation](https://docs.spring.io/spring-boot/reference/io/validation.html)

### 関連記事

- [Validation in Spring Boot](https://www.baeldung.com/spring-boot-bean-validation)
- [Custom Validation in Spring](https://www.baeldung.com/spring-mvc-custom-validator)

---

## ➡️ 次のステップ

[Step 19: DTOとEntityの分離](STEP_19.md)へ進みましょう！

次のステップでは、DTOとEntityの分離について深く学びます：

- なぜDTOが必要なのか（セキュリティ、柔軟性）
- DTOとEntityの責務の違い
- MapStructによる自動マッピング
- ネストしたオブジェクトのマッピング
- レスポンス用DTOとリクエスト用DTOの使い分け

データの入出力を適切に制御し、安全で保守性の高いAPIを設計しましょう！
