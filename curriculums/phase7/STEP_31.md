# Step 31: ページネーション

## 🎯 このステップの目標

- `Pageable`を使ってページネーションを実装できる
- `Page<T>`レスポンスの構造を理解できる
- ソート機能を追加できる
- パフォーマンスの良いリストAPIを作成できる
- フロントエンドと連携できるページネーションAPIを設計できる

**所要時間**: 約40分

---

## 📋 事前準備

- Step 30までの内容を完了していること
- データベースにユーザーデータが複数件存在していること
- Spring Data JPAの基本を理解していること

---

## 🚀 ステップ1: ページネーションとは

### 1-1. ページネーションが必要な理由

**問題: すべてのデータを一度に取得すると...**

```java
// ❌ 10万件のユーザーを一度に取得
@GetMapping("/users")
public List<User> getAllUsers() {
    return userRepository.findAll();  // メモリ不足、レスポンス遅延
}
```

**課題**:
- メモリ使用量が膨大
- レスポンスが遅い
- ネットワーク帯域を圧迫
- フロントエンドでの表示が困難

**解決: ページネーション**

```java
// ✅ 10件ずつ取得
@GetMapping("/users")
public Page<User> getAllUsers(Pageable pageable) {
    return userRepository.findAll(pageable);  // 必要なデータだけ取得
}
```

**メリット**:
- メモリ効率が良い
- レスポンスが速い
- ユーザー体験の向上

---

## 🚀 ステップ2: ページネーションの基本実装

### 2-1. リポジトリの準備

`UserRepository`は既にページネーションをサポートしています（`JpaRepository`が提供）：

```java
// src/main/java/com/example/hellospringboot/repositories/UserRepository.java
package com.example.hellospringboot.repositories;

import com.example.hellospringboot.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    
    // JpaRepositoryが提供するページネーションメソッド:
    // - Page<User> findAll(Pageable pageable)
    // - Page<User> findByXxx(条件, Pageable pageable)
}
```

### 2-2. ページネーション対応のDTOを作成

`src/main/java/com/example/hellospringboot/dto/PageResponse.java`を作成：

```java
package com.example.hellospringboot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ページネーションレスポンス
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {
    
    /**
     * コンテンツ（データ本体）
     */
    private List<T> content;
    
    /**
     * 現在のページ番号（0始まり）
     */
    private int pageNumber;
    
    /**
     * ページサイズ
     */
    private int pageSize;
    
    /**
     * 総要素数
     */
    private long totalElements;
    
    /**
     * 総ページ数
     */
    private int totalPages;
    
    /**
     * 最初のページか
     */
    private boolean first;
    
    /**
     * 最後のページか
     */
    private boolean last;
    
    /**
     * 空のページか
     */
    private boolean empty;
    
    /**
     * Spring DataのPageオブジェクトから変換
     */
    public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }
}
```

### 2-3. UserServiceにページネーション機能を追加

`src/main/java/com/example/hellospringboot/services/UserService.java`に以下のメソッドを追加：

```java
package com.example.hellospringboot.services;

import com.example.hellospringboot.dto.UserCreateRequest;
import com.example.hellospringboot.dto.UserResponse;
import com.example.hellospringboot.dto.UserUpdateRequest;
import com.example.hellospringboot.entities.User;
import com.example.hellospringboot.exceptions.ResourceNotFoundException;
import com.example.hellospringboot.mappers.UserMapper;
import com.example.hellospringboot.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * 全ユーザー取得（ページネーションなし）
     */
    public List<UserResponse> getAllUsers() {
        log.info("Fetching all users");
        return userRepository.findAll().stream()
                .map(UserMapper::toResponse)
                .toList();
    }
    
    /**
     * 全ユーザー取得（ページネーションあり）
     */
    public Page<UserResponse> getAllUsersPaginated(Pageable pageable) {
        log.info("Fetching users with pagination: page={}, size={}", 
                pageable.getPageNumber(), pageable.getPageSize());
        
        Page<User> userPage = userRepository.findAll(pageable);
        return userPage.map(UserMapper::toResponse);
    }
    
    /**
     * ID指定でユーザー取得
     */
    public UserResponse getUserById(Long id) {
        log.info("Fetching user by id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return UserMapper.toResponse(user);
    }
    
    /**
     * ユーザー作成
     */
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        log.info("Creating user: {}", request.getEmail());
        
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        
        User saved = userRepository.save(user);
        return UserMapper.toResponse(saved);
    }
    
    /**
     * ユーザー更新
     */
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        log.info("Updating user: id={}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        
        User updated = userRepository.save(user);
        return UserMapper.toResponse(updated);
    }
    
    /**
     * ユーザー削除
     */
    @Transactional
    public void deleteUser(Long id) {
        log.info("Deleting user: id={}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        userRepository.delete(user);
    }
}
```

### 2-4. UserControllerにページネーションエンドポイントを追加

`src/main/java/com/example/hellospringboot/controllers/UserController.java`に以下を追加：

```java
package com.example.hellospringboot.controllers;

import com.example.hellospringboot.dto.PageResponse;
import com.example.hellospringboot.dto.UserCreateRequest;
import com.example.hellospringboot.dto.UserResponse;
import com.example.hellospringboot.dto.UserUpdateRequest;
import com.example.hellospringboot.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final UserService userService;
    
    /**
     * 全ユーザー取得（ページネーションなし）
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    /**
     * 全ユーザー取得（ページネーションあり）
     */
    @GetMapping("/paginated")
    public ResponseEntity<PageResponse<UserResponse>> getAllUsersPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {
        
        // Sortオブジェクトを作成
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Sort sort = Sort.by(direction, sortBy);
        
        // Pageableオブジェクトを作成
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // ページネーション実行
        Page<UserResponse> userPage = userService.getAllUsersPaginated(pageable);
        
        // カスタムレスポンスに変換
        PageResponse<UserResponse> response = PageResponse.of(userPage);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * ID指定でユーザー取得
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    
    /**
     * ユーザー作成
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserResponse created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    /**
     * ユーザー更新
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        UserResponse updated = userService.updateUser(id, request);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * ユーザー削除
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
```

### 2-5. コードの解説

#### `Pageable`パラメータ
```java
public ResponseEntity<PageResponse<UserResponse>> getAllUsersPaginated(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "id") String sortBy,
    @RequestParam(defaultValue = "ASC") String sortDirection)
```
- **page**: ページ番号（0始まり）
- **size**: 1ページあたりの件数
- **sortBy**: ソート対象のフィールド名
- **sortDirection**: ソート方向（ASC/DESC）

#### `PageRequest.of()`
```java
Pageable pageable = PageRequest.of(page, size, sort);
```
- `Pageable`の実装クラス
- ページ番号、サイズ、ソート条件を指定

#### `Page<T>.map()`
```java
Page<User> userPage = userRepository.findAll(pageable);
return userPage.map(UserMapper::toResponse);
```
- `Page`オブジェクトの各要素を変換
- EntityからDTOへの変換に便利

---

## 🚀 ステップ3: カスタムクエリでのページネーション

### 3-1. UserRepositoryにカスタムクエリを追加

`src/main/java/com/example/hellospringboot/repositories/UserRepository.java`に以下を追加：

```java
package com.example.hellospringboot.repositories;

import com.example.hellospringboot.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    /**
     * 名前で検索（部分一致、ページネーションあり）
     */
    Page<User> findByNameContaining(String name, Pageable pageable);
    
    /**
     * メールアドレスで検索（部分一致、ページネーションあり）
     */
    Page<User> findByEmailContaining(String email, Pageable pageable);
    
    /**
     * JPQL: 名前またはメールアドレスで検索
     */
    @Query("SELECT u FROM User u WHERE u.name LIKE %:keyword% OR u.email LIKE %:keyword%")
    Page<User> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
```

### 3-2. UserServiceに検索機能を追加

`UserService`に以下のメソッドを追加：

```java
/**
 * キーワード検索（ページネーションあり）
 */
public Page<UserResponse> searchUsers(String keyword, Pageable pageable) {
    log.info("Searching users with keyword: {}, page={}, size={}", 
            keyword, pageable.getPageNumber(), pageable.getPageSize());
    
    Page<User> userPage = userRepository.searchByKeyword(keyword, pageable);
    return userPage.map(UserMapper::toResponse);
}

/**
 * 名前で検索（ページネーションあり）
 */
public Page<UserResponse> findUsersByName(String name, Pageable pageable) {
    log.info("Finding users by name: {}", name);
    Page<User> userPage = userRepository.findByNameContaining(name, pageable);
    return userPage.map(UserMapper::toResponse);
}
```

### 3-3. UserControllerに検索エンドポイントを追加

`UserController`に以下を追加：

```java
/**
 * キーワード検索（名前またはメールアドレス）
 */
@GetMapping("/search")
public ResponseEntity<PageResponse<UserResponse>> searchUsers(
        @RequestParam String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "ASC") String sortDirection) {
    
    Sort.Direction direction = Sort.Direction.fromString(sortDirection);
    Sort sort = Sort.by(direction, sortBy);
    Pageable pageable = PageRequest.of(page, size, sort);
    
    Page<UserResponse> userPage = userService.searchUsers(keyword, pageable);
    PageResponse<UserResponse> response = PageResponse.of(userPage);
    
    return ResponseEntity.ok(response);
}
```

---

## ✅ 動作確認

### 1. テストデータを準備

まず、複数のユーザーを作成します：

```bash
# ユーザー1
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice Johnson",
    "email": "alice@example.com",
    "password": "password123"
  }'

# ユーザー2
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Bob Smith",
    "email": "bob@example.com",
    "password": "password123"
  }'

# ユーザー3
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Charlie Brown",
    "email": "charlie@example.com",
    "password": "password123"
  }'

# ... 合計15人くらい作成
```

### 2. ページネーション（デフォルト設定）

```bash
# 1ページ目（デフォルト: 10件、IDの昇順）
curl "http://localhost:8080/api/users/paginated"
```

**期待される結果**:
```json
{
  "content": [
    {
      "id": 1,
      "name": "Alice Johnson",
      "email": "alice@example.com"
    },
    {
      "id": 2,
      "name": "Bob Smith",
      "email": "bob@example.com"
    }
    // ... 最大10件
  ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 15,
  "totalPages": 2,
  "first": true,
  "last": false,
  "empty": false
}
```

### 3. ページ番号とサイズを指定

```bash
# 2ページ目、1ページあたり5件
curl "http://localhost:8080/api/users/paginated?page=1&size=5"
```

**期待される結果**:
```json
{
  "content": [
    {
      "id": 6,
      "name": "Frank Wilson",
      "email": "frank@example.com"
    }
    // ... 5件
  ],
  "pageNumber": 1,
  "pageSize": 5,
  "totalElements": 15,
  "totalPages": 3,
  "first": false,
  "last": false,
  "empty": false
}
```

### 4. ソート条件を指定

```bash
# 名前の昇順
curl "http://localhost:8080/api/users/paginated?sortBy=name&sortDirection=ASC"

# 名前の降順
curl "http://localhost:8080/api/users/paginated?sortBy=name&sortDirection=DESC"

# メールアドレスの昇順
curl "http://localhost:8080/api/users/paginated?sortBy=email&sortDirection=ASC"
```

### 5. 複数条件を組み合わせ

```bash
# 2ページ目、3件ずつ、名前の降順
curl "http://localhost:8080/api/users/paginated?page=1&size=3&sortBy=name&sortDirection=DESC"
```

### 6. キーワード検索

```bash
# "john"を含むユーザーを検索
curl "http://localhost:8080/api/users/search?keyword=john&page=0&size=10"
```

**期待される結果**:
```json
{
  "content": [
    {
      "id": 1,
      "name": "Alice Johnson",
      "email": "alice@example.com"
    },
    {
      "id": 5,
      "name": "John Doe",
      "email": "john@example.com"
    }
  ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 2,
  "totalPages": 1,
  "first": true,
  "last": true,
  "empty": false
}
```

---

## 🎨 チャレンジ課題

基本が理解できたら、以下にチャレンジしてみましょう：

### チャレンジ 1: 複数フィールドでのソート

**目標**: 名前の昇順、同じ名前の場合はメールアドレスの昇順でソート

**ヒント**:
```java
// 複数のソート条件を組み合わせ
Sort sort = Sort.by(
    Sort.Order.asc("name"),
    Sort.Order.asc("email")
);
Pageable pageable = PageRequest.of(page, size, sort);
```

### チャレンジ 2: 動的なフィルタリング

**目標**: 複数の条件を動的に組み合わせて検索

**ヒント**:
```java
// Specificationを使った動的クエリ
public interface UserRepository extends JpaRepository<User, Long>, 
                                        JpaSpecificationExecutor<User> {
}

// 動的条件の構築
Specification<User> spec = (root, query, cb) -> {
    List<Predicate> predicates = new ArrayList<>();
    
    if (name != null) {
        predicates.add(cb.like(root.get("name"), "%" + name + "%"));
    }
    if (email != null) {
        predicates.add(cb.like(root.get("email"), "%" + email + "%"));
    }
    
    return cb.and(predicates.toArray(new Predicate[0]));
};

Page<User> result = userRepository.findAll(spec, pageable);
```

### チャレンジ 3: カーソルベースのページネーション

**目標**: オフセットベースではなく、カーソル（ID）ベースのページネーション

**ヒント**:
```java
@Query("SELECT u FROM User u WHERE u.id > :cursor ORDER BY u.id ASC")
List<User> findAfterCursor(@Param("cursor") Long cursor, Pageable pageable);

// 使用例
List<User> users = userRepository.findAfterCursor(lastSeenId, PageRequest.of(0, 10));
```

**メリット**:
- データ追加時にページがズレない
- 大規模データで高速

---

## 🐛 トラブルシューティング

### エラー: "No property 'xxx' found for type User"

**原因**: ソート対象のフィールド名が間違っている

**解決策**:
```java
// ❌ フィールド名が間違っている
Sort.by("userName")  // UserエンティティにはuserNameフィールドがない

// ✅ 正しいフィールド名
Sort.by("name")  // Userエンティティのnameフィールド
```

### ページ番号が範囲外でも200 OKが返る

**原因**: Spring Data JPAの仕様（エラーにならない）

**対応**:
```java
Page<User> userPage = userRepository.findAll(pageable);

if (userPage.isEmpty() && pageable.getPageNumber() > 0) {
    throw new IllegalArgumentException("Page number out of range");
}
```

### 総件数（totalElements）が遅い

**原因**: `COUNT(*)`クエリが発行されるため、大規模データでは遅い

**解決策**:
```java
// Slice<T>を使う（totalElementsを計算しない）
Slice<User> userSlice = userRepository.findAll(pageable);

// または、カウントクエリを最適化
@Query(value = "SELECT u FROM User u",
       countQuery = "SELECT COUNT(u.id) FROM User u")  // 最適化されたCOUNT
Page<User> findAllOptimized(Pageable pageable);
```

### ソート方向の大文字小文字エラー

**原因**: `Sort.Direction.fromString()`は大文字小文字を区別

**解決策**:
```java
// ❌ エラー
Sort.Direction.fromString("asc")  // 小文字はNG

// ✅ 正しい
Sort.Direction.fromString(sortDirection.toUpperCase())

// または
Sort.Direction direction = sortDirection.equalsIgnoreCase("DESC") 
    ? Sort.Direction.DESC 
    : Sort.Direction.ASC;
```

---

## 📚 このステップで学んだこと

- ✅ `Pageable`インターフェースの使い方
- ✅ `PageRequest`でページ番号、サイズ、ソートを指定
- ✅ `Page<T>`レスポンスの構造（content, totalElements, totalPagesなど）
- ✅ `Page<T>.map()`でEntityからDTOへ変換
- ✅ カスタムクエリでのページネーション実装
- ✅ 複数のソート条件の指定
- ✅ キーワード検索とページネーションの組み合わせ
- ✅ `@RequestParam`でのデフォルト値設定
- ✅ カスタムPageResponseの設計
- ✅ パフォーマンスを考慮したリストAPI設計

---

## 💡 補足: ページネーションのベストプラクティス

### 1. オフセットベース vs カーソルベース

| 種類 | メリット | デメリット | 使用例 |
|---|---|---|---|
| **オフセットベース** | シンプル、ページ番号ジャンプ可能 | 大規模データで遅い、データ挿入でズレる | 管理画面、検索結果 |
| **カーソルベース** | 高速、データ追加に強い | ページ番号ジャンプ不可 | SNSフィード、無限スクロール |

**オフセットベース（Pageable）**:
```sql
-- page=2, size=10の場合
SELECT * FROM users ORDER BY id LIMIT 10 OFFSET 20
```

**カーソルベース**:
```sql
-- cursor=100の場合
SELECT * FROM users WHERE id > 100 ORDER BY id LIMIT 10
```

### 2. デフォルト値の設定

**推奨設定**:
```java
@RequestParam(defaultValue = "0") int page       // 最初のページ
@RequestParam(defaultValue = "10") int size      // 適度なサイズ
@RequestParam(defaultValue = "id") String sortBy // 主キーでソート
```

**サイズの上限を設定**:
```java
if (size > 100) {
    size = 100;  // 最大100件まで
}
```

### 3. フロントエンドとの連携

**React例**:
```javascript
const fetchUsers = async (page = 0, size = 10) => {
  const response = await fetch(
    `http://localhost:8080/api/users/paginated?page=${page}&size=${size}`
  );
  const data = await response.json();
  
  return {
    users: data.content,
    currentPage: data.pageNumber,
    totalPages: data.totalPages,
    hasNext: !data.last,
    hasPrev: !data.first
  };
};
```

### 4. ページネーションレスポンスの標準化

**一貫したレスポンス構造**:
```json
{
  "content": [...],          // データ本体
  "pageNumber": 0,           // 現在のページ
  "pageSize": 10,            // ページサイズ
  "totalElements": 100,      // 総件数
  "totalPages": 10,          // 総ページ数
  "first": true,             // 最初のページか
  "last": false,             // 最後のページか
  "empty": false             // 空か
}
```

### 5. パフォーマンス最適化

**インデックスの作成**:
```sql
-- ソート対象のカラムにインデックス
CREATE INDEX idx_user_name ON users(name);
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_created_at ON users(created_at);
```

**N+1問題の回避**:
```java
// ❌ N+1問題
@GetMapping("/users/posts")
public Page<UserResponse> getUsersWithPosts(Pageable pageable) {
    Page<User> users = userRepository.findAll(pageable);
    // 各ユーザーのPostを取得 → N回のクエリ発生
}

// ✅ フェッチジョイン
@Query("SELECT u FROM User u LEFT JOIN FETCH u.posts")
Page<User> findAllWithPosts(Pageable pageable);
```

### 6. ソートのセキュリティ

**ホワイトリスト方式**:
```java
private static final Set<String> ALLOWED_SORT_FIELDS = 
    Set.of("id", "name", "email", "createdAt");

public Pageable createPageable(int page, int size, String sortBy, String sortDirection) {
    // ソートフィールドのバリデーション
    if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
        throw new IllegalArgumentException("Invalid sort field: " + sortBy);
    }
    
    Sort.Direction direction = Sort.Direction.fromString(sortDirection);
    Sort sort = Sort.by(direction, sortBy);
    
    return PageRequest.of(page, size, sort);
}
```

---

## ➡️ 次のステップ

[Step 32: キャッシュ](STEP_32.md)へ進みましょう！

次のステップでは、Spring Cacheを使ってアプリケーションのパフォーマンスを向上させる方法を学びます。`@Cacheable`、`@CacheEvict`などのアノテーションを使って、効率的なキャッシング戦略を実装しましょう。
