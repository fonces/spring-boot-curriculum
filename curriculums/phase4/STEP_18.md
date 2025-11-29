# Step 18: バリデーション

## 🎯 このステップの目標

- Bean Validation（Jakarta Bean Validation）の基本を理解する
- `@Valid`と`@Validated`を使い分けられる
- 標準バリデーションアノテーションを活用できる
- カスタムバリデーションを作成できる
- グループバリデーションを実装できる

**所要時間**: 約1時間

---

## 📋 事前準備

このステップを始める前に、以下を確認してください：

- Step 17（例外ハンドリング）が完了していること
- DTOクラスを作成した経験があること
- `@RestControllerAdvice`でバリデーションエラーをハンドリングできること

---

## 📝 概要
Webアプリケーションでは、ユーザーからの入力値を検証することが重要です。Spring Bootでは、Bean Validation（Jakarta Bean Validation）を使って宣言的にバリデーションを実装できます。

## 📦 依存関係の追加

Spring Boot 2.3以降では、`spring-boot-starter-validation`を明示的に追加する必要があります。

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

## 🔍 基本的なバリデーション

### 1. DTOクラスにバリデーションアノテーション

**ファイルパス**: `src/main/java/com/example/hellospringboot/entity/User.java`

```java
package com.example.hellospringboot.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "名前は必須です")
    @Size(min = 2, max = 50, message = "名前は2文字以上50文字以下で入力してください")
    @Column(nullable = false, length = 50)
    private String name;
    
    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    @Column(nullable = false, unique = true)
    private String email;
    
    @NotNull(message = "年齢は必須です")
    @Min(value = 18, message = "18歳以上である必要があります")
    @Max(value = 120, message = "年齢は120歳以下で入力してください")
    private Integer age;
    
    @Pattern(regexp = "^\\d{3}-\\d{4}-\\d{4}$", message = "電話番号は xxx-xxxx-xxxx の形式で入力してください")
    private String phoneNumber;
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

### 2. 主要なバリデーションアノテーション

| アノテーション | 説明 | 例 |
|---|---|---|
| `@NotNull` | nullでないこと | `@NotNull private Integer age;` |
| `@NotEmpty` | nullでなく、空でないこと（文字列、コレクション） | `@NotEmpty private String name;` |
| `@NotBlank` | nullでなく、空白でないこと（文字列のみ） | `@NotBlank private String email;` |
| `@Size` | サイズ制限 | `@Size(min=2, max=50)` |
| `@Min` / `@Max` | 最小値 / 最大値 | `@Min(18) private Integer age;` |
| `@Email` | メールアドレス形式 | `@Email private String email;` |
| `@Pattern` | 正規表現パターン | `@Pattern(regexp="^\\d{3}-\\d{4}$")` |
| `@Positive` | 正の数 | `@Positive private Integer price;` |
| `@PositiveOrZero` | 0または正の数 | `@PositiveOrZero private Integer stock;` |
| `@Past` | 過去の日付 | `@Past private LocalDate birthDate;` |
| `@Future` | 未来の日付 | `@Future private LocalDate eventDate;` |

### 3. Controllerで`@Valid`を使う

**ファイルパス**: `src/main/java/com/example/hellospringboot/controller/UserController.java`

```java
package com.example.hellospringboot.controller;

import com.example.hellospringboot.entity.User;
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
    
    @GetMapping
    public List<User> getAll() {
        return userService.findAll();
    }
    
    @GetMapping("/{id}")
    public User getById(@PathVariable Long id) {
        return userService.findById(id);
    }
    
    /**
     * @Valid によりバリデーション実行
     * バリデーションエラーがあれば MethodArgumentNotValidException がスローされる
     */
    @PostMapping
    public ResponseEntity<User> create(@Valid @RequestBody User user) {
        User created = userService.create(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    public User update(
            @PathVariable Long id,
            @Valid @RequestBody User user) {
        return userService.update(id, user);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### 4. UserServiceの実装

**ファイルパス**: `src/main/java/com/example/hellospringboot/service/UserService.java`

```java
package com.example.hellospringboot.service;

import com.example.hellospringboot.entity.User;
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
    
    public List<User> findAll() {
        return userRepository.findAll();
    }
    
    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }
    
    @Transactional
    public User create(User user) {
        return userRepository.save(user);
    }
    
    @Transactional
    public User update(Long id, User user) {
        User existingUser = findById(id);
        existingUser.setName(user.getName());
        existingUser.setEmail(user.getEmail());
        existingUser.setAge(user.getAge());
        existingUser.setPhoneNumber(user.getPhoneNumber());
        return userRepository.save(existingUser);
    }
    
    @Transactional
    public void delete(Long id) {
        User user = findById(id);
        userRepository.delete(user);
    }
}
```

### 5. パスパラメータとクエリパラメータのバリデーション

```java
@RestController
@RequestMapping("/api/users")
@Validated  // ⭐ クラスレベルに @Validated が必要
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    /**
     * パスパラメータのバリデーション
     */
    @GetMapping("/{id}")
    public User getById(@PathVariable @Positive Long id) {
        return userService.findById(id);
    }
}
```

## 🎨 カスタムバリデーション

### 1. カスタムアノテーションの作成

```java
package com.example.hellospringboot.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = AdultValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Adult {
    String message() default "18歳以上である必要があります";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

### 2. バリデータの実装

```java
package com.example.hellospringboot.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AdultValidator implements ConstraintValidator<Adult, Integer> {
    
    @Override
    public boolean isValid(Integer age, ConstraintValidatorContext context) {
        if (age == null) {
            return true;  // @NotNull と組み合わせて使う
        }
        return age >= 18;
    }
}
```

### 3. カスタムアノテーションの使用

```java
@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    private String name;
    
    @Email
    private String email;
    
    @NotNull
    @Adult  // カスタムバリデーション
    private Integer age;
    
    // その他のフィールド...
}
```

## 🔧 グループバリデーション

### 1. バリデーショングループの定義

```java
package com.example.hellospringboot.validation;

public interface ValidationGroups {
    interface Create {}
    interface Update {}
}
```

### 2. グループごとのバリデーション

```java
import com.example.hellospringboot.validation.ValidationGroups.Create;
import com.example.hellospringboot.validation.ValidationGroups.Update;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Null(groups = Create.class, message = "作成時はIDを指定できません")
    @NotNull(groups = Update.class, message = "更新時はIDが必須です")
    private Long id;
    
    @NotBlank(groups = {Create.class, Update.class}, message = "名前は必須です")
    @Size(min = 2, max = 50, groups = {Create.class, Update.class})
    private String name;
    
    @Email(groups = {Create.class, Update.class}, message = "有効なメールアドレスを入力してください")
    private String email;
    
    @NotNull(groups = Create.class, message = "年齢は必須です")
    @Min(value = 18, groups = {Create.class, Update.class})
    @Adult(groups = {Create.class, Update.class})
    private Integer age;
    
    // その他のフィールド...
}
```

### 3. Controllerでの使用

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @PostMapping
    public ResponseEntity<User> create(
            @Validated(ValidationGroups.Create.class) @RequestBody User user) {
        User created = userService.create(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    public User update(
            @PathVariable Long id,
            @Validated(ValidationGroups.Update.class) @RequestBody User user) {
        return userService.update(id, user);
    }
}
```

## 📊 バリデーションエラーのレスポンス

Step 17で作成したGlobalExceptionHandlerが以下のように処理します：

```java
/**
 * バリデーションエラー（@Validアノテーション）
 */
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleValidationException(
        MethodArgumentNotValidException ex,
        WebRequest request) {
    
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        "Validation Failed",
        "入力値が不正です",
        request.getDescription(false).replace("uri=", "")
    );

        // フィールドエラーをMapに変換（シンプルな形式）
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getFieldErrors().forEach(error -> {
        String fieldName = error.getField();
        String errorMessage = error.getDefaultMessage();
        errors.put(fieldName, errorMessage);
    });
    errorResponse.setErrors(errors);
    
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
}
```

## ✅ 動作確認

### 1. バリデーションエラー（複数項目）

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "A",
    "email": "invalid-email",
    "age": 15
  }'
```

**レスポンス**:
```json
{
  "timestamp": "2024-01-15T11:00:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "入力値が不正です",
  "path": "/api/users",
  "errors": {
    "name": "名前は2文字以上50文字以下で入力してください",
    "email": "有効なメールアドレスを入力してください",
    "age": "18歳以上である必要があります"
  }
}
```

### 2. 正常なリクエスト

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "山田太郎",
    "email": "yamada@example.com",
    "age": 25,
    "phoneNumber": "090-1234-5678"
  }'
```

### 3. パスパラメータのバリデーションエラー

```bash
curl -X GET http://localhost:8080/api/users/-1
```

**レスポンス**:
```json
{
  "timestamp": "2024-01-15T11:05:00",
  "status": 400,
  "error": "Constraint Violation",
  "message": "パラメータが不正です",
  "errors": {
    "getById.id": "0 より大きな値にしてください"
  }
}
```

このエラーには別途ハンドラが必要です：

```java
@ExceptionHandler(ConstraintViolationException.class)
public ResponseEntity<ErrorResponse> handleConstraintViolation(
        ConstraintViolationException ex,
        WebRequest request) {
    
    Map<String, String> errors = new HashMap<>();
    ex.getConstraintViolations().forEach(violation -> {
        String propertyPath = violation.getPropertyPath().toString();
        String message = violation.getMessage();
        errors.put(propertyPath, message);
    });
    
    ErrorResponse error = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        "Constraint Violation",
        "パラメータが不正です",
        request.getDescription(false).replace("uri=", "")
    );
    error.setErrors(errors);
    
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
}
```

## 🎨 チャレンジ課題

### 課題1: 日付バリデーション

`User`エンティティに誕生日フィールドを追加し、過去の日付であることを検証してください。

```java
@Entity
@Table(name = "users")
@Data
public class User {
    // 既存のフィールド...
    
    @Past(message = "誕生日は過去の日付である必要があります")
    private LocalDate birthDate;
}
```

### 課題2: 条件付きバリデーション

年齢が18歳未満の場合は保護者の同意が必要、といった条件付きバリデーション。

```java
@Entity
@Table(name = "users")
@Data
public class User {
    // 既存のフィールド...
    
    @NotNull
    @Min(0)
    private Integer age;
    
    private Boolean parentalConsent;
    
    @AssertTrue(message = "18歳未満の場合は保護者の同意が必要です")
    public boolean isParentalConsentValid() {
        if (age == null || age >= 18) {
            return true;
        }
        return Boolean.TRUE.equals(parentalConsent);
    }
}
```

---

## 📚 このステップで学んだこと

- ✅ Bean Validation（Jakarta Bean Validation）の基本
- ✅ 標準バリデーションアノテーション（`@NotBlank`、`@Email`、`@Min`など）
- ✅ `@Valid`でリクエストDTOのバリデーション実行
- ✅ `@Validated`によるパスパラメータ・クエリパラメータのバリデーション
- ✅ カスタムバリデーションアノテーションの作成
- ✅ クラスレベルのバリデーション（パスワード確認など）
- ✅ グループバリデーション（Create/Update別ルール）
- ✅ `MethodArgumentNotValidException`と`ConstraintViolationException`の処理

**バリデーションのメリット**:
- データの整合性を保つ
- セキュリティリスクの軽減
- 不正なデータによるエラーを事前に防ぐ
- クライアント側へ明確なエラーメッセージを返せる

---

## 🐛 トラブルシューティング

### エラー: バリデーションが動作しない

**原因**: `@Valid`または`@Validated`が付いていない

**解決策**:
```java
// ❌ NG: アノテーションなし
@PostMapping
public ResponseEntity<User> create(@RequestBody UserCreateRequest request) {
    // バリデーションされない
}

// ✅ OK: @Validを付ける
@PostMapping
public ResponseEntity<User> create(@Valid @RequestBody UserCreateRequest request) {
    // バリデーションされる
}
```

### エラー: "HV000030: No validator could be found for constraint"

**原因**: カスタムバリデーターの実装クラスが見つからない、またはジェネリクス型が一致しない

**解決策**:
```java
// ✅ アノテーションとバリデーターのジェネリクス型を一致させる
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AdultValidator.class)  // ← ここを指定
public @interface Adult {
    // ...
}

// ✅ バリデーターのジェネリクス型をStringに
public class AdultValidator implements ConstraintValidator<Adult, Integer> {
    //                                                      ↑アノテーション ↑検証対象の型
}
```

### エラー: パスパラメータのバリデーションが効かない

**原因**: コントローラークラスに`@Validated`が付いていない

**解決策**:
```java
@RestController
@RequestMapping("/api/users")
@Validated  // ← クラスレベルに必須
public class UserController {
    
    @GetMapping("/{id}")
    public User getUser(@PathVariable @Min(1) Long id) {
        // これでバリデーションが効く
    }
}
```

### 問題: エラーメッセージが英語で表示される

**原因**: デフォルトメッセージが使用されている

**解決策**:
```java
// ✅ message属性で日本語メッセージを指定
@NotBlank(message = "名前は必須です")
@Size(min = 2, max = 50, message = "名前は2文字以上50文字以内で入力してください")
private String name;

@Email(message = "メールアドレスの形式が正しくありません")
private String email;

@Min(value = 0, message = "年齢は0以上で入力してください")
@Max(value = 150, message = "年齢は150以下で入力してください")
private Integer age;
```

### 問題: 複数のバリデーションエラーが1つしか表示されない

**原因**: エラーハンドラーが最初のエラーだけ返している

**解決策**:
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
    // ✅ すべてのフィールドエラーを収集
    Map<String, String> errors = new HashMap<>();
    e.getBindingResult().getFieldErrors().forEach(error -> {
        errors.put(error.getField(), error.getDefaultMessage());
    });
    
    ErrorResponse response = ErrorResponse.builder()
        .message("入力値に誤りがあります")
        .errors(errors)  // すべてのエラーを返す
        .build();
    
    return ResponseEntity.badRequest().body(response);
}
```

### 問題: ネストしたオブジェクトのバリデーションが効かない

**原因**: ネストしたフィールドに`@Valid`が付いていない

**解決策**:
```java
public class OrderRequest {
    @NotBlank
    private String orderNumber;
    
    @Valid  // ← ネストしたオブジェクトにも@Validが必要
    @NotNull
    private AddressRequest address;
}

public class AddressRequest {
    @NotBlank
    private String street;
    
    @NotBlank
    private String city;
}
```

---

## 🔄 Gitへのコミットとレビュー依頼

進捗を記録してレビューを受けましょう：

```bash
# 変更をステージング
git add .

# コミット
git commit -m "Step 18: バリデーション完了"

# リモートにプッシュ
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

レビューが完了したら、[Step 19: DTOとEntityの分離](STEP_19.md)へ進みましょう！

レイヤー間のデータ変換を理解し、セキュアで保守性の高いアプリケーション構造を構築します。
