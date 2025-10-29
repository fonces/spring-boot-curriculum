# Step 16: バリデーション（入力検証）

## 🎯 このステップの目標

- Bean Validationを使った入力検証を実装する
- カスタムバリデーションを作成する
- バリデーションエラーのハンドリングを理解する
- グループバリデーションを使い分ける

**所要時間**: 約1時間30分

---

## 📋 事前準備

- Step 15のDTOパターンが理解できていること
- UserCreateRequest、UserUpdateRequestが実装されていること

**Step 15をまだ完了していない場合**: [Step 15: レイヤードアーキテクチャとDTOパターン](STEP_15.md)を先に進めてください。

---

## 💡 バリデーションとは？

### バリデーションの必要性

**バリデーションなしの場合**:
```bash
# 空のメールアドレス
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Taro", "email": "", "age": 30}'

# データベースエラーが発生！
# SQLIntegrityConstraintViolationException
```

**問題点**:
- ❌ 不正なデータがビジネスロジックに到達
- ❌ データベースレベルでエラー（遅い、わかりにくい）
- ❌ ユーザーフレンドリーなエラーメッセージがない

### バリデーションのレイヤー

```
┌───────────────────────────┐
│ Controller                │ ← バリデーション（@Valid）
│ @Valid UserCreateRequest  │
└───────────────────────────┘
         ↓ 検証済みデータ
┌───────────────────────────┐
│ Service                   │ ← ビジネスロジック
└───────────────────────────┘
         ↓
┌───────────────────────────┐
│ Repository                │ ← データベース操作
└───────────────────────────┘
```

---

## 🚀 ステップ1: Bean Validationアノテーション

### 1-1. 依存関係の確認

Spring Boot 3では`spring-boot-starter-web`に含まれています。

**確認**: `pom.xml`に以下があればOK
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

**ない場合**: 追加してMaven Reload

### 1-2. UserCreateRequestにバリデーション追加

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/request/UserCreateRequest.java`

```java
package com.example.hellospringboot.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ユーザー作成リクエストDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreateRequest {

    /**
     * ユーザー名
     */
    @NotBlank(message = "ユーザー名は必須です")
    @Size(min = 2, max = 100, message = "ユーザー名は2文字以上100文字以内で入力してください")
    private String name;

    /**
     * メールアドレス
     */
    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "メールアドレスの形式が正しくありません")
    @Size(max = 100, message = "メールアドレスは100文字以内で入力してください")
    private String email;

    /**
     * 年齢
     */
    @NotNull(message = "年齢は必須です")
    @Min(value = 0, message = "年齢は0以上で入力してください")
    @Max(value = 150, message = "年齢は150以下で入力してください")
    private Integer age;
}
```

### 1-3. 主要なバリデーションアノテーション

| アノテーション | 説明 | 例 |
|---------------|------|-----|
| `@NotNull` | nullでないこと | `@NotNull Integer age` |
| `@NotEmpty` | nullでも空文字でもないこと | `@NotEmpty String name` |
| `@NotBlank` | nullでも空文字でも空白のみでもないこと | `@NotBlank String name` |
| `@Size` | 長さの制約 | `@Size(min=2, max=100)` |
| `@Min` / `@Max` | 数値の最小/最大 | `@Min(0) @Max(150)` |
| `@Email` | メールアドレス形式 | `@Email String email` |
| `@Pattern` | 正規表現 | `@Pattern(regexp="^[0-9]{3}-[0-9]{4}$")` |
| `@Positive` | 正の数 | `@Positive Integer count` |
| `@Past` | 過去の日付 | `@Past LocalDate birthDate` |
| `@Future` | 未来の日付 | `@Future LocalDate appointmentDate` |

---

## 🚀 ステップ2: Controllerでバリデーション実行

### 2-1. UserControllerの更新

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
     * ユーザー更新
     * PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return userService.updateUser(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ... 他のメソッドは省略
}
```

**ポイント**: `@Valid`アノテーションを追加

---

## 🚀 ステップ3: バリデーションエラーのハンドリング

### 3-1. エラーレスポンスDTOの作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/response/ErrorResponse.java`

```java
package com.example.hellospringboot.dto.response;

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
public class ErrorResponse {

    /**
     * エラーメッセージ
     */
    private String message;

    /**
     * HTTPステータスコード
     */
    private int status;

    /**
     * エラー発生時刻
     */
    private LocalDateTime timestamp;

    /**
     * フィールドごとのエラー詳細
     */
    private Map<String, String> errors;
}
```

### 3-2. GlobalExceptionHandlerの作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/exception/GlobalExceptionHandler.java`

```java
package com.example.hellospringboot.exception;

import com.example.hellospringboot.dto.response.ErrorResponse;
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
public class GlobalExceptionHandler {

    /**
     * バリデーションエラーのハンドリング
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex) {
        
        // フィールドごとのエラーメッセージを収集
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("入力値が正しくありません")
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .errors(errors)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * その他の例外のハンドリング
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("サーバーエラーが発生しました")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }
}
```

---

## ✅ ステップ4: 動作確認

### 4-1. 正常なリクエスト

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Taro Yamada",
    "email": "taro@example.com",
    "age": 30
  }'
```

**期待される結果**: 201 Created

### 4-2. バリデーションエラー（空のname）

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "",
    "email": "taro@example.com",
    "age": 30
  }'
```

**期待されるエラーレスポンス**:
```json
{
  "message": "入力値が正しくありません",
  "status": 400,
  "timestamp": "2025-10-27T10:30:00",
  "errors": {
    "name": "ユーザー名は必須です"
  }
}
```

### 4-3. 複数のバリデーションエラー

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "A",
    "email": "invalid-email",
    "age": 200
  }'
```

**期待されるエラーレスポンス**:
```json
{
  "message": "入力値が正しくありません",
  "status": 400,
  "timestamp": "2025-10-27T10:30:00",
  "errors": {
    "name": "ユーザー名は2文字以上100文字以内で入力してください",
    "email": "メールアドレスの形式が正しくありません",
    "age": "年齢は150以下で入力してください"
  }
}
```

---

## 🚀 ステップ5: カスタムバリデーション

### 5-1. カスタムアノテーションの作成

電話番号のバリデーションを作成します。

**ファイルパス**: `src/main/java/com/example/hellospringboot/validation/PhoneNumber.java`

```java
package com.example.hellospringboot.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 電話番号バリデーション
 */
@Documented
@Constraint(validatedBy = PhoneNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PhoneNumber {

    String message() default "電話番号の形式が正しくありません（例: 090-1234-5678）";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
```

### 5-2. Validatorの実装

**ファイルパス**: `src/main/java/com/example/hellospringboot/validation/PhoneNumberValidator.java`

```java
package com.example.hellospringboot.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * 電話番号バリデータ
 */
public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^0\\d{1,4}-\\d{1,4}-\\d{4}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // nullの場合は@NotBlankなどに任せる
        if (value == null || value.isEmpty()) {
            return true;
        }
        
        return PHONE_PATTERN.matcher(value).matches();
    }
}
```

### 5-3. カスタムバリデーションの使用

```java
public class UserCreateRequest {
    
    @PhoneNumber
    private String phoneNumber;
    
    // ... 他のフィールド
}
```

---

## 🚀 ステップ6: グループバリデーション

### 6-1. バリデーショングループの定義

**ファイルパス**: `src/main/java/com/example/hellospringboot/validation/ValidationGroups.java`

```java
package com.example.hellospringboot.validation;

/**
 * バリデーショングループ
 */
public class ValidationGroups {

    /**
     * 作成時のバリデーション
     */
    public interface Create {}

    /**
     * 更新時のバリデーション
     */
    public interface Update {}
}
```

### 6-2. グループ別バリデーションの適用

```java
public class UserRequest {

    @NotNull(groups = ValidationGroups.Update.class)
    private Long id;

    @NotBlank(groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    private String name;

    @NotBlank(groups = ValidationGroups.Create.class)
    @Email(groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    private String email;
}
```

### 6-3. Controllerで使用

```java
@PostMapping
public ResponseEntity<UserResponse> createUser(
        @Validated(ValidationGroups.Create.class) @RequestBody UserRequest request) {
    // ...
}

@PutMapping("/{id}")
public ResponseEntity<UserResponse> updateUser(
        @PathVariable Long id,
        @Validated(ValidationGroups.Update.class) @RequestBody UserRequest request) {
    // ...
}
```

---

## 🚀 ステップ7: ネストしたオブジェクトのバリデーション

### 7-1. ネストしたDTOの例

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/request/AddressRequest.java`

```java
package com.example.hellospringboot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressRequest {

    @NotBlank(message = "郵便番号は必須です")
    @Pattern(regexp = "^\\d{3}-\\d{4}$", message = "郵便番号の形式が正しくありません（例: 123-4567）")
    private String postalCode;

    @NotBlank(message = "都道府県は必須です")
    private String prefecture;

    @NotBlank(message = "市区町村は必須です")
    private String city;

    @NotBlank(message = "番地は必須です")
    private String street;

    private String building;  // 建物名は任意
}
```

### 7-2. ネストしたバリデーション

```java
public class UserCreateRequest {

    @NotBlank
    private String name;

    @Valid  // ネストしたオブジェクトもバリデーション
    private AddressRequest address;
}
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: パスワード強度バリデーション

パスワードの強度をチェックするカスタムバリデーションを作成してください。

**要件**:
- 8文字以上
- 大文字、小文字、数字を含む
- 特殊文字を1つ以上含む

### チャレンジ 2: ユニークメール検証

データベースに既に存在するメールアドレスを拒否するバリデーションを作成してください。

**ヒント**:
```java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueEmailValidator.class)
public @interface UniqueEmail {
    String message() default "このメールアドレスは既に登録されています";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

### チャレンジ 3: クロスフィールドバリデーション

2つのフィールドを比較するバリデーションを作成してください。

**例**: パスワードと確認用パスワードが一致すること

---

## 🐛 トラブルシューティング

### バリデーションが動作しない

**症状**: `@Valid`を付けてもバリデーションされない

**原因1**: `spring-boot-starter-validation`が不足

**解決策**: `pom.xml`に追加
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

**原因2**: `@Valid`を忘れている

**解決策**: Controllerのメソッド引数に`@Valid`追加

### カスタムバリデーションが動作しない

**症状**: カスタムバリデーションアノテーションが無視される

**解決策**: `@Constraint`でValidatorクラスを指定
```java
@Constraint(validatedBy = PhoneNumberValidator.class)
public @interface PhoneNumber {
    // ...
}
```

### エラーメッセージが日本語にならない

**解決策**: `ValidationMessages.properties`を作成

**ファイルパス**: `src/main/resources/ValidationMessages.properties`
```properties
jakarta.validation.constraints.NotBlank.message = {0}は必須です
jakarta.validation.constraints.Email.message = メールアドレスの形式が正しくありません
```

---

## 📚 このステップで学んだこと

- ✅ Bean Validationアノテーション
- ✅ `@Valid`によるバリデーション実行
- ✅ `@RestControllerAdvice`による例外ハンドリング
- ✅ カスタムバリデーションアノテーション
- ✅ グループバリデーション
- ✅ ネストしたオブジェクトのバリデーション
- ✅ ユーザーフレンドリーなエラーレスポンス

---

## 💡 補足: バリデーションのベストプラクティス

### レイヤー別のバリデーション

| レイヤー | バリデーション内容 |
|----------|-------------------|
| **Controller** | 形式チェック（@Valid） |
| **Service** | ビジネスルール |
| **Database** | データ整合性（制約） |

### メッセージの国際化

```properties
# messages.properties
user.name.required=ユーザー名は必須です

# messages_en.properties
user.name.required=Name is required
```

```java
@NotBlank(message = "{user.name.required}")
private String name;
```

### パフォーマンス考慮

```java
// ❌ 悪い例: データベースアクセスを伴うバリデーションを全フィールドで実行
@UniqueEmail
private String email;

@UniqueUsername
private String username;

// ✅ 良い例: Serviceレイヤーで1回だけチェック
public void createUser(UserCreateRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
        throw new DuplicateEmailException();
    }
    // ...
}
```

---

## 🔄 Gitへのコミット

進捗を記録しましょう：

```bash
git add .
git commit -m "Step 16: バリデーション完了"
git push origin main
```

---

## ➡️ 次のステップ

次は[Step 17: 例外ハンドリング](STEP_17.md)へ進みましょう！

カスタム例外を作成し、統一されたエラーレスポンスを返すAPIを実装します。

---

お疲れさまでした！ 🎉

DI/IoCはSpring Frameworkの心臓部です。
不正なデータを早期に検出し、ユーザーにわかりやすいエラーメッセージを返すことで、
UXとセキュリティの両方を向上させることができます！
