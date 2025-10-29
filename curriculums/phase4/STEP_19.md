# Step 19: DTOとEntityの分離

## 🎯 このステップの目標

- DTO（Data Transfer Object）の役割を理解する
- リクエストDTOとレスポンスDTOを分離できる
- MapperクラスでEntity⇔DTO変換を実装できる
- MapStructを使った自動マッピングができる
- セキュアなAPI設計を実践できる

**所要時間**: 約1時間30分

---

## 📋 事前準備

このステップを始める前に、以下を確認してください：

- Step 18（バリデーション）が完了していること
- Entityクラスを理解していること
- パスワードハッシュ化などセキュリティの基本を理解していること

---

## 📝 概要
これまでのステップでは、Entityを直接Controllerで受け取ったり返したりしていました。しかし実務では、**DTO（Data Transfer Object）**を使ってレイヤー間でデータを受け渡すことが推奨されます。

## ❌ DTOを使わない問題点

### 1. セキュリティリスク

```java
@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    private String password;  // ⚠️ パスワードがレスポンスに含まれる！
    private String role;      // ⚠️ 内部的な役割情報も漏洩
    private LocalDateTime createdAt;
}
```

```java
@GetMapping("/{id}")
public User getUser(@PathVariable Long id) {
    return userService.findById(id);  // ❌ パスワードなど全て返ってしまう
}
```

**レスポンス**:
```json
{
  "id": 1,
  "name": "山田太郎",
  "email": "yamada@example.com",
  "password": "$2a$10$xyz...",  // ⚠️ ハッシュ化されていても返すべきでない
  "role": "ADMIN",
  "createdAt": "2024-01-01T10:00:00"
}
```

### 2. 柔軟性の欠如

- クライアントが必要な情報だけを返せない
- 複数テーブルの情報を組み合わせたレスポンスが難しい
- APIバージョン管理が困難

## ✅ DTO を使った設計

### プロジェクト構造

```
src/main/java/com/example/demo/
├── controller/
│   └── UserController.java
├── dto/
│   ├── request/
│   │   ├── UserCreateRequest.java
│   │   └── UserUpdateRequest.java
│   └── response/
│       ├── UserResponse.java
│       └── UserDetailResponse.java
├── entity/
│   └── User.java
├── mapper/
│   └── UserMapper.java
├── service/
│   └── UserService.java
└── repository/
    └── UserRepository.java
```

## 📦 実装例

### 1. Entity（内部データ）

```java
package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String password;  // ハッシュ化されたパスワード
    
    @Column(nullable = false, length = 20)
    private String role;  // "USER", "ADMIN"
    
    private Integer age;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### 2. リクエストDTO

```java
package com.example.demo.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * ユーザー作成リクエスト
 */
@Data
public class UserCreateRequest {
    @NotBlank(message = "名前は必須です")
    @Size(min = 2, max = 100, message = "名前は2文字以上100文字以下で入力してください")
    private String name;
    
    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    private String email;
    
    @NotBlank(message = "パスワードは必須です")
    @Size(min = 8, message = "パスワードは8文字以上で入力してください")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", 
             message = "パスワードは大文字、小文字、数字を含む必要があります")
    private String password;
    
    @NotNull(message = "年齢は必須です")
    @Min(value = 18, message = "18歳以上である必要があります")
    @Max(value = 120, message = "年齢は120歳以下で入力してください")
    private Integer age;
}
```

```java
package com.example.demo.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * ユーザー更新リクエスト
 */
@Data
public class UserUpdateRequest {
    @NotBlank(message = "名前は必須です")
    @Size(min = 2, max = 100)
    private String name;
    
    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    private String email;
    
    @Min(value = 18, message = "18歳以上である必要があります")
    @Max(value = 120, message = "年齢は120歳以下で入力してください")
    private Integer age;
}
```

### 3. レスポンスDTO

```java
package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ユーザー基本情報レスポンス（一覧用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private Integer age;
    // パスワード、役割、タイムスタンプは含まない
}
```

```java
package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * ユーザー詳細レスポンス（個別取得用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailResponse {
    private Long id;
    private String name;
    private String email;
    private Integer age;
    private String role;  // 詳細情報には含める
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // パスワードは含まない
}
```

### 4. Mapper（変換ロジック）

#### 手動マッピング

```java
package com.example.demo.mapper;

import com.example.demo.dto.request.UserCreateRequest;
import com.example.demo.dto.request.UserUpdateRequest;
import com.example.demo.dto.response.UserDetailResponse;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    
    /**
     * Entity → 基本レスポンスDTO
     */
    public UserResponse toResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .age(user.getAge())
            .build();
    }
    
    /**
     * Entity → 詳細レスポンスDTO
     */
    public UserDetailResponse toDetailResponse(User user) {
        return UserDetailResponse.builder()
            .id(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .age(user.getAge())
            .role(user.getRole())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
    }
    
    /**
     * 作成リクエストDTO → Entity
     */
    public User toEntity(UserCreateRequest request) {
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());  // Serviceでハッシュ化される
        user.setAge(request.getAge());
        user.setRole("USER");  // デフォルトは一般ユーザー
        return user;
    }
    
    /**
     * 更新リクエストDTO → 既存Entityに反映
     */
    public void updateEntity(User user, UserUpdateRequest request) {
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setAge(request.getAge());
    }
}
```

#### MapStruct（自動マッピング）

**依存関係追加**:
```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>1.5.5.Final</version>
    <scope>provided</scope>
</dependency>
```

```java
package com.example.demo.mapper;

import com.example.demo.dto.request.UserCreateRequest;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapperMapStruct {
    
    UserResponse toResponse(User user);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "role", constant = "USER")
    User toEntity(UserCreateRequest request);
}
```

### 5. Service層（DTO/Entity変換含む）

```java
package com.example.demo.service;

import com.example.demo.dto.request.UserCreateRequest;
import com.example.demo.dto.request.UserUpdateRequest;
import com.example.demo.dto.response.UserDetailResponse;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.UserMapper;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;  // 後のステップで実装
    
    /**
     * 全ユーザー取得（基本情報のみ）
     */
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
            .map(userMapper::toResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * ID指定でユーザー取得（詳細情報）
     */
    public UserDetailResponse findById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return userMapper.toDetailResponse(user);
    }
    
    /**
     * ユーザー作成
     */
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        // DTOをEntityに変換
        User user = userMapper.toEntity(request);
        
        // パスワードのハッシュ化
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // 保存
        User saved = userRepository.save(user);
        
        // EntityをDTOに変換して返す
        return userMapper.toResponse(saved);
    }
    
    /**
     * ユーザー更新
     */
    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        // 更新内容を既存Entityに反映
        userMapper.updateEntity(user, request);
        
        User updated = userRepository.save(user);
        return userMapper.toResponse(updated);
    }
    
    /**
     * ユーザー削除
     */
    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        userRepository.delete(user);
    }
}
```

### 6. Controller（すっきり）

```java
package com.example.demo.controller;

import com.example.demo.dto.request.UserCreateRequest;
import com.example.demo.dto.request.UserUpdateRequest;
import com.example.demo.dto.response.UserDetailResponse;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.service.UserService;
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
     * 全ユーザー取得
     */
    @GetMapping
    public List<UserResponse> getAll() {
        return userService.findAll();
    }
    
    /**
     * ID指定でユーザー取得
     */
    @GetMapping("/{id}")
    public UserDetailResponse getById(@PathVariable Long id) {
        return userService.findById(id);
    }
    
    /**
     * ユーザー作成
     */
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * ユーザー更新
     */
    @PutMapping("/{id}")
    public UserResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return userService.update(id, request);
    }
    
    /**
     * ユーザー削除
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### 7. PasswordEncoderの設定（仮実装）

```java
package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**依存関係**:
```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
```

## 🎯 DTOのメリット

| メリット | 説明 |
|---|---|
| **セキュリティ** | パスワードや内部情報をレスポンスから除外 |
| **柔軟性** | クライアントが必要な情報だけを返せる |
| **バージョン管理** | APIのバージョンごとに異なるDTOを使える |
| **バリデーション分離** | Entityとバリデーションルールを分離 |
| **ドキュメント性** | APIの入出力が明確になる |
| **テスト容易性** | レイヤー間の依存が減る |

## ✅ 動作確認

### 1. ユーザー作成

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "山田太郎",
    "email": "yamada@example.com",
    "password": "SecurePass123",
    "age": 25
  }'
```

**レスポンス（パスワードなし）**:
```json
{
  "id": 1,
  "name": "山田太郎",
  "email": "yamada@example.com",
  "age": 25
}
```

### 2. ユーザー一覧

```bash
curl http://localhost:8080/api/users
```

**レスポンス（基本情報のみ）**:
```json
[
  {
    "id": 1,
    "name": "山田太郎",
    "email": "yamada@example.com",
    "age": 25
  }
]
```

### 3. ユーザー詳細

```bash
curl http://localhost:8080/api/users/1
```

**レスポンス（詳細情報だがパスワードなし）**:
```json
{
  "id": 1,
  "name": "山田太郎",
  "email": "yamada@example.com",
  "age": 25,
  "role": "USER",
  "createdAt": "2024-01-15T10:00:00",
  "updatedAt": "2024-01-15T10:00:00"
}
```

## 🚀 発展課題

### 課題1: 複数テーブルの結合レスポンス

ユーザーと投稿を結合したレスポンス。

```java
@Data
@Builder
public class UserWithPostsResponse {
    private Long id;
    private String name;
    private String email;
    private List<PostSummary> posts;
    
    @Data
    @Builder
    public static class PostSummary {
        private Long id;
        private String title;
        private LocalDateTime createdAt;
    }
}
```

### 課題2: ページネーション対応のレスポンス

```java
@Data
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
```

### 課題3: HATEOAS対応

```java
@Data
public class UserResponseWithLinks extends UserResponse {
    private Map<String, String> links;
    
    public void addLink(String rel, String href) {
        if (links == null) {
            links = new HashMap<>();
        }
        links.put(rel, href);
    }
}

// 使用例
UserResponseWithLinks response = new UserResponseWithLinks();
response.addLink("self", "/api/users/1");
response.addLink("posts", "/api/users/1/posts");
```

---

## 📚 このステップで学んだこと

- ✅ DTO（Data Transfer Object）の役割と重要性
- ✅ リクエストDTO、レスポンスDTO、Entityの分離
- ✅ セキュリティリスクの軽減（パスワード漏洩防止）
- ✅ Mapperクラスによる手動マッピング
- ✅ MapStructによる自動マッピング
- ✅ 用途別のレスポンスDTO（基本情報/詳細情報）
- ✅ PasswordEncoderの設定と使用
- ✅ レイヤー間のデータ変換パターン

**DTOを使うメリット**:
- セキュリティ向上（不要な情報を返さない）
- 柔軟なAPI設計（必要な情報だけ返せる）
- バージョン管理の容易さ
- バリデーションルールの分離

---

## 🔄 Gitへのコミットとレビュー依頼

進捗を記録してレビューを受けましょう：

```bash
# 変更をステージング
git add .

# コミット
git commit -m "Step 19: DTOとEntityの分離完了"

# リモートにプッシュ
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

レビューが完了したら、[Step 20: ロギング](STEP_20.md)へ進みましょう！

適切なログ出力を実装し、トラブルシューティングとアプリケーションの監視を可能にします。
