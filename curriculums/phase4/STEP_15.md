# Step 15: レイヤードアーキテクチャとDTOパターン

## 🎯 このステップの目標

- レイヤードアーキテクチャの概念を理解する
- DTO (Data Transfer Object) パターンを理解し実装する
- エンティティとDTOを分離する理由を理解する
- MapStructを使ってマッピングを自動化する

**所要時間**: 約2時間

---

## 📋 事前準備

- Phase 2までのデータベース統合が理解できていること
- UserエンティティとPostエンティティが実装されていること

**Phase 2をまだ完了していない場合**: [Phase 2](../phase2/STEP_6.md)を先に進めてください。

---

## 💡 レイヤードアーキテクチャとは？

### アーキテクチャの必要性

小規模なアプリでは問題なくても、成長すると：
- ❌ コードの責任範囲が不明確
- ❌ 変更の影響範囲が大きい
- ❌ テストが困難
- ❌ 再利用性が低い

**解決策**: レイヤー（層）に分けて責任を分離する

### レイヤードアーキテクチャの構造

```
┌─────────────────────────────────┐
│   Presentation Layer            │  ← ユーザーとのやり取り
│   (Controller)                  │
└─────────────────────────────────┘
            ↓↑
┌─────────────────────────────────┐
│   Business Logic Layer          │  ← ビジネスロジック
│   (Service)                     │
└─────────────────────────────────┘
            ↓↑
┌─────────────────────────────────┐
│   Data Access Layer             │  ← データベース操作
│   (Repository)                  │
└─────────────────────────────────┘
            ↓↑
┌─────────────────────────────────┐
│   Database                      │
└─────────────────────────────────┘
```

### 各レイヤーの責任

| レイヤー | 責任 | Spring Bootでの実装 |
|----------|------|---------------------|
| **Presentation** | HTTPリクエスト/レスポンス処理 | `@RestController` |
| **Business Logic** | ビジネスルール、トランザクション | `@Service` |
| **Data Access** | データベースCRUD | `@Repository` (JpaRepository) または `@Mapper` (MyBatis) |
| **Domain** | ビジネスオブジェクト | `@Entity` またはPOJO |

> **💡 Phase 3の復習**: Phase 3でMyBatisを学習しました。Data Access層では**JPA (Spring Data JPA)** と **MyBatis** の両方が選択肢になります。
> - **JPA**: シンプルなCRUD、オブジェクト指向的な設計に向いている
> - **MyBatis**: 複雑なSQL、パフォーマンス最適化が必要な場合に向いている
> 
> このステップではJPAを使った実装例を示しますが、Phase 3で学んだMyBatisでも同じアーキテクチャパターンが適用できます。

---

## 💡 DTOパターンとは？

### エンティティをそのまま返す問題

**現在の実装**:
```java
@GetMapping("/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id) {
    return userService.getUserById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
}
```

**問題点**:
1. **セキュリティリスク**: 内部実装がAPIに露出
2. **循環参照**: `User → Posts → User → ...`
3. **不要なデータ**: パスワードなど返すべきでないデータ
4. **柔軟性の欠如**: API変更がDB変更に直結

### DTOによる解決

**DTO (Data Transfer Object)**:
- レイヤー間でデータを転送するためのオブジェクト
- 必要なデータだけを含む
- APIの契約とDB設計を分離

```
Controller → DTO → Service → Entity/POJO → Repository/Mapper → DB
           ← DTO ←         ← Entity/POJO ←                    ←
```

> **💡 MyBatisとの違い**:
> - **JPA**: `Entity` (JPAアノテーション付き) → `Repository` (JpaRepository)
> - **MyBatis**: POJO (単純なJavaクラス) → `Mapper` (MyBatisインターフェース)
> 
> どちらもDTOパターンは同じように使えます。

---

## 🚀 ステップ1: DTOクラスの作成

### 1-1. User用DTOの作成

**ディレクトリ構造**:
```
src/main/java/com/example/hellospringboot/
├── controller/
├── service/
├── repository/
├── entity/
└── dto/              ← 新規作成
    ├── request/
    │   ├── UserCreateRequest.java
    │   └── UserUpdateRequest.java
    └── response/
        └── UserResponse.java
```

### 1-2. UserCreateRequest

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/request/UserCreateRequest.java`

```java
package com.example.hellospringboot.dto.request;

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
    private String name;

    /**
     * メールアドレス
     */
    private String email;

    /**
     * 年齢
     */
    private Integer age;
}
```

### 1-3. UserUpdateRequest

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/request/UserUpdateRequest.java`

```java
package com.example.hellospringboot.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ユーザー更新リクエストDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {

    /**
     * ユーザー名
     */
    private String name;

    /**
     * メールアドレス
     */
    private String email;

    /**
     * 年齢
     */
    private Integer age;
}
```

### 1-4. UserResponse

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/response/UserResponse.java`

```java
package com.example.hellospringboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ユーザーレスポンスDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    /**
     * ユーザーID
     */
    private Long id;

    /**
     * ユーザー名
     */
    private String name;

    /**
     * メールアドレス
     */
    private String email;

    /**
     * 年齢
     */
    private Integer age;

    /**
     * 投稿数（エンティティには存在しない計算フィールド）
     */
    private Integer postCount;
}
```

---

## 🚀 ステップ2: マッパークラスの作成

### 2-1. 手動マッピングの実装

**ファイルパス**: `src/main/java/com/example/hellospringboot/mapper/UserMapper.java`

```java
package com.example.hellospringboot.mapper;

import com.example.hellospringboot.dto.request.UserCreateRequest;
import com.example.hellospringboot.dto.request.UserUpdateRequest;
import com.example.hellospringboot.dto.response.UserResponse;
import com.example.hellospringboot.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * UserエンティティとDTOの相互変換を行うマッパー
 */
@Component
public class UserMapper {

    /**
     * UserCreateRequest → User エンティティ
     */
    public User toEntity(UserCreateRequest request) {
        return User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .age(request.getAge())
                .build();
    }

    /**
     * UserUpdateRequest → User エンティティ（既存エンティティを更新）
     */
    public void updateEntity(User user, UserUpdateRequest request) {
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setAge(request.getAge());
    }

    /**
     * User エンティティ → UserResponse
     */
    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .age(user.getAge())
                .postCount(user.getPosts() != null ? user.getPosts().size() : 0)
                .build();
    }

    /**
     * User エンティティリスト → UserResponse リスト
     */
    public List<UserResponse> toResponseList(List<User> users) {
        return users.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
```

---

## 🚀 ステップ3: Serviceレイヤーの更新

### 3-1. UserServiceの更新

**ファイルパス**: `src/main/java/com/example/hellospringboot/service/UserService.java`

```java
package com.example.hellospringboot.service;

import com.example.hellospringboot.dto.request.UserCreateRequest;
import com.example.hellospringboot.dto.request.UserUpdateRequest;
import com.example.hellospringboot.dto.response.UserResponse;
import com.example.hellospringboot.entity.User;
import com.example.hellospringboot.mapper.UserMapper;
import com.example.hellospringboot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * ユーザーを作成
     */
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        // DTOからエンティティへ変換
        User user = userMapper.toEntity(request);
        
        // 保存
        User savedUser = userRepository.save(user);
        
        // エンティティからDTOへ変換して返却
        return userMapper.toResponse(savedUser);
    }

    /**
     * 全ユーザーを取得
     */
    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return userMapper.toResponseList(users);
    }

    /**
     * IDでユーザーを取得
     */
    public Optional<UserResponse> getUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toResponse);
    }

    /**
     * ユーザーを更新
     */
    @Transactional
    public Optional<UserResponse> updateUser(Long id, UserUpdateRequest request) {
        return userRepository.findById(id)
                .map(user -> {
                    // 既存エンティティを更新
                    userMapper.updateEntity(user, request);
                    User updatedUser = userRepository.save(user);
                    return userMapper.toResponse(updatedUser);
                });
    }

    /**
     * ユーザーを削除
     */
    @Transactional
    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
```

---

## 🚀 ステップ4: Controllerレイヤーの更新

### 4-1. UserControllerの更新

**ファイルパス**: `src/main/java/com/example/hellospringboot/controller/UserController.java`

```java
package com.example.hellospringboot.controller;

import com.example.hellospringboot.dto.request.UserCreateRequest;
import com.example.hellospringboot.dto.request.UserUpdateRequest;
import com.example.hellospringboot.dto.response.UserResponse;
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
    public ResponseEntity<UserResponse> createUser(@RequestBody UserCreateRequest request) {
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
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * ユーザー更新
     * PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @RequestBody UserUpdateRequest request) {
        return userService.updateUser(id, request)
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

## ✅ ステップ5: 動作確認

### 5-1. アプリケーション起動

### 5-2. ユーザー作成（DTOを使用）

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Taro Yamada",
    "email": "taro@example.com",
    "age": 30
  }'
```

**期待されるレスポンス**:
```json
{
  "id": 1,
  "name": "Taro Yamada",
  "email": "taro@example.com",
  "age": 30,
  "postCount": 0
}
```

### 5-3. 投稿を追加してpostCountを確認

```bash
# 投稿作成
curl -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "title": "First Post",
    "content": "Hello World"
  }'

# ユーザー取得（postCountが1になる）
curl http://localhost:8080/api/users/1
```

**期待されるレスポンス**:
```json
{
  "id": 1,
  "name": "Taro Yamada",
  "email": "taro@example.com",
  "age": 30,
  "postCount": 1
}
```

---

## 🚀 ステップ6: Post用DTOの作成（演習）

### 6-1. PostCreateRequest

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/request/PostCreateRequest.java`

```java
package com.example.hellospringboot.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostCreateRequest {

    /**
     * 投稿するユーザーのID
     */
    private Long userId;

    /**
     * タイトル
     */
    private String title;

    /**
     * 本文
     */
    private String content;
}
```

### 6-2. PostResponse

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/response/PostResponse.java`

```java
package com.example.hellospringboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponse {

    /**
     * 投稿ID
     */
    private Long id;

    /**
     * タイトル
     */
    private String title;

    /**
     * 本文
     */
    private String content;

    /**
     * 作成日時
     */
    private LocalDateTime createdAt;

    /**
     * 投稿者情報
     */
    private UserSummary user;

    /**
     * 投稿者サマリー（循環参照を防ぐ）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserSummary {
        private Long id;
        private String name;
        private String email;
    }
}
```

### 6-3. PostMapper

**ファイルパス**: `src/main/java/com/example/hellospringboot/mapper/PostMapper.java`

```java
package com.example.hellospringboot.mapper;

import com.example.hellospringboot.dto.response.PostResponse;
import com.example.hellospringboot.entity.Post;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PostMapper {

    /**
     * Post エンティティ → PostResponse
     */
    public PostResponse toResponse(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .createdAt(post.getCreatedAt())
                .user(PostResponse.UserSummary.builder()
                        .id(post.getUser().getId())
                        .name(post.getUser().getName())
                        .email(post.getUser().getEmail())
                        .build())
                .build();
    }

    /**
     * Post エンティティリスト → PostResponse リスト
     */
    public List<PostResponse> toResponseList(List<Post> posts) {
        return posts.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
```

---

## 🚀 ステップ7: MapStructによる自動マッピング（オプション）

### 7-1. MapStructとは？

手動マッピングは面倒：
```java
return UserResponse.builder()
    .id(user.getId())
    .name(user.getName())
    .email(user.getEmail())
    .age(user.getAge())
    .build();
```

**MapStruct**:
- コンパイル時にマッピングコードを自動生成
- 型安全
- 高速（リフレクションなし）

### 7-2. MapStructの追加

**ファイルパス**: `pom.xml`

```xml
<properties>
    <org.mapstruct.version>1.5.5.Final</org.mapstruct.version>
</properties>

<dependencies>
    <!-- MapStruct -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>${org.mapstruct.version}</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <source>21</source>
                <target>21</target>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>${lombok.version}</version>
                    </path>
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                        <version>${org.mapstruct.version}</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 7-3. MapStructマッパーインターフェース

**ファイルパス**: `src/main/java/com/example/hellospringboot/mapper/UserMapperMapStruct.java`

```java
package com.example.hellospringboot.mapper;

import com.example.hellospringboot.dto.request.UserCreateRequest;
import com.example.hellospringboot.dto.response.UserResponse;
import com.example.hellospringboot.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapperMapStruct {

    /**
     * UserCreateRequest → User エンティティ
     */
    User toEntity(UserCreateRequest request);

    /**
     * User エンティティ → UserResponse
     */
    @Mapping(target = "postCount", expression = "java(user.getPosts() != null ? user.getPosts().size() : 0)")
    UserResponse toResponse(User user);

    /**
     * User エンティティリスト → UserResponse リスト
     */
    List<UserResponse> toResponseList(List<User> users);
}
```

### 7-4. 生成されたコード確認

Maven Reloadすると、`target/generated-sources/annotations/`に実装クラスが生成されます。

---

## 🎨 チャレンジ課題

### チャレンジ 1: PostService/PostControllerのDTO化

PostServiceとPostControllerをDTO対応にしてください。

**要件**:
- `PostCreateRequest`、`PostUpdateRequest`、`PostResponse`を使用
- 循環参照が発生しないこと

### チャレンジ 2: ページネーション対応DTO

ページネーション情報を含むレスポンスDTOを作成してください。

**ヒント**:
```java
@Data
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
```

### チャレンジ 3: MapStructの高度な使い方

カスタムマッピングを実装してください。

**例**: 名前を大文字に変換
```java
@Mapping(target = "name", expression = "java(user.getName().toUpperCase())")
UserResponse toResponse(User user);
```

---

## � 補足: MyBatisでの実装

Phase 3でMyBatisを学習した場合、同じアーキテクチャパターンをMyBatisでも実装できます。

### MyBatisでのData Access層

**Mapper Interface**:
```java
package com.example.hellospringboot.mapper;

import com.example.hellospringboot.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserMapper {
    
    @Insert("INSERT INTO users (name, email, created_at, updated_at) " +
            "VALUES (#{name}, #{email}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);
    
    @Select("SELECT * FROM users WHERE id = #{id}")
    Optional<User> findById(Long id);
    
    @Select("SELECT * FROM users")
    List<User> findAll();
    
    @Update("UPDATE users SET name = #{name}, email = #{email}, updated_at = NOW() " +
            "WHERE id = #{id}")
    void update(User user);
    
    @Delete("DELETE FROM users WHERE id = #{id}")
    void deleteById(Long id);
    
    @Select("SELECT COUNT(*) > 0 FROM users WHERE email = #{email}")
    boolean existsByEmail(String email);
}
```

### Service層での利用

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;  // MyBatis Mapper
    private final UserDtoMapper dtoMapper;  // MapStructのマッパー

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        User user = dtoMapper.toEntity(request);
        userMapper.insert(user);  // MyBatisのinsert
        return dtoMapper.toResponse(user);
    }

    public List<UserResponse> getAllUsers() {
        List<User> users = userMapper.findAll();  // MyBatisのselect
        return dtoMapper.toResponseList(users);
    }

    public Optional<UserResponse> getUserById(Long id) {
        return userMapper.findById(id)
                .map(dtoMapper::toResponse);
    }
}
```

### JPA vs MyBatisの選択基準

| 観点 | JPA | MyBatis |
|------|-----|---------|
| **CRUD操作** | 自動生成で簡単 | SQL手書きが必要 |
| **複雑なクエリ** | JPQLやCriteria APIが煩雑 | SQL直接記述で柔軟 |
| **動的クエリ** | Specification API | XMLの動的SQL |
| **パフォーマンス** | N+1問題に注意 | 必要なカラムだけ取得可能 |
| **テスト** | インメモリDBなどで簡単 | 本番DBと同じ構造が必要 |

**推奨**:
- **シンプルなCRUD**: JPA
- **複雑な検索・集計**: MyBatis
- **大規模プロジェクト**: 両方を併用（用途に応じて使い分け）

> このカリキュラムでは主にJPAを使用しますが、Phase 3で学んだMyBatisの知識も活かせることを覚えておいてください！

---

## �🐛 トラブルシューティング

### MapStructが動作しない

**症状**: 実装クラスが生成されない

**解決策**:
1. Maven Reload
2. `mvn clean compile`を実行
3. IntelliJ IDEAの場合: Annotation Processingを有効化
   - Settings → Build → Compiler → Annotation Processors
   - "Enable annotation processing"にチェック

### 循環参照エラー

**エラー**: `StackOverflowError`または`JsonMappingException`

**原因**: User → Posts → User → ...

**解決策**: ネストしたDTOを使用
```java
public class PostResponse {
    private UserSummary user;  // User全体ではなくサマリー
}
```

---

## 📚 このステップで学んだこと

- ✅ レイヤードアーキテクチャの概念
- ✅ 各レイヤーの責任分離
- ✅ DTOパターンの重要性
- ✅ Request DTO（入力）とResponse DTO（出力）
- ✅ マッパークラスによる変換
- ✅ MapStructによる自動マッピング
- ✅ 循環参照の回避方法

---

## 💡 補足: DTOのベストプラクティス

### 命名規則

| 用途 | 命名パターン | 例 |
|------|-------------|-----|
| 作成リクエスト | `{Entity}CreateRequest` | `UserCreateRequest` |
| 更新リクエスト | `{Entity}UpdateRequest` | `UserUpdateRequest` |
| レスポンス | `{Entity}Response` | `UserResponse` |
| サマリー | `{Entity}Summary` | `UserSummary` |
| 検索条件 | `{Entity}SearchCriteria` | `UserSearchCriteria` |

### DTO設計の原則

1. **単一責任**: 1つのDTOは1つの目的
2. **不変性**: 可能な限りイミュータブル（finalフィールド）
3. **バリデーション**: 次のステップで追加
4. **ドキュメント**: Javadocで説明

### パフォーマンス考慮

```java
// ❌ 悪い例: N+1問題
public List<UserResponse> getAllUsers() {
    List<User> users = userRepository.findAll();
    return users.stream()
        .map(user -> {
            user.getPosts().size();  // 各ユーザーでSQLが発行される
            return mapper.toResponse(user);
        })
        .collect(Collectors.toList());
}

// ✅ 良い例: JOIN FETCHで一括取得
@Query("SELECT u FROM User u LEFT JOIN FETCH u.posts")
List<User> findAllWithPosts();
```

---

## 🔄 Gitへのコミット

進捗を記録しましょう：

```bash
git add .
git commit -m "Step 15: レイヤードアーキテクチャとDTOパターン完了"
git push origin main
```

---

## ➡️ 次のステップ

次は[Step 16: DI/IoCコンテナの深掘り](STEP_16.md)へ進みましょう！

Spring FrameworkのコアであるDI/IoCを深く理解します。

---

お疲れさまでした！ 🎉

レイヤー化アーキテクチャは、保守性の高いアプリケーションの基礎です。
これからは、より実践的な機能を追加していきます！
