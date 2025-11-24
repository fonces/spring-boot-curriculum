# Step 15: レイヤー化アーキテクチャ

## 🎯 このステップの目標

- Controller / Service / Repository の3層アーキテクチャを理解する
- 各レイヤーの責務を明確に分離できる
- `@Service`アノテーションの役割を理解する
- ビジネスロジックの適切な配置場所を判断できる

**所要時間**: 約1時間

---

## 📋 事前準備

このステップを始める前に、以下を確認してください：

- Step 14までが完了していること
- Spring Data JPAまたはMyBatisの基本を理解していること
- `@RestController`と`JpaRepository`の使い方を理解していること

---

## 📝 概要
これまでのステップでは、Controllerにビジネスロジックとデータアクセスロジックが混在していた可能性があります。このステップでは、**Controller / Service / Repository**の3層アーキテクチャに分離し、それぞれの責務を明確にします。

## 🏗️ レイヤー化アーキテクチャとは

### 3層アーキテクチャの責務

```
┌─────────────────────┐
│   Controller層      │ ← HTTPリクエスト/レスポンスの制御
│   (Presentation)    │   パラメータのバリデーション
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│   Service層         │ ← ビジネスロジック
│   (Business Logic)  │   トランザクション制御
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│   Repository層      │ ← データアクセス
│   (Data Access)     │   CRUD操作
└─────────────────────┘
```

### 各層の役割

| 層 | 役割 | アノテーション |
|---|---|---|
| **Controller** | HTTPリクエストの受付、レスポンスの返却 | `@RestController`, `@Controller` |
| **Service** | ビジネスロジックの実装、複数Repositoryの調整 | `@Service` |
| **Repository** | データベースとのやり取り | `@Repository`, `JpaRepository` |

## 📦 実装例

### 1. プロジェクト構成

```
src/main/java/com/example/hellospringboot/
├── controller/
│   └── UserController.java
├── service/
│   ├── UserService.java        # インターフェース
│   └── UserServiceImpl.java    # 実装クラス
├── repository/
│   └── UserRepository.java
└── entity/
    └── User.java
```

### 2. Entity（変更なし）

```java
package com.example.hellospringboot.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String email;
    private Integer age;
}
```

### 3. Repository層

```java
package com.example.hellospringboot.repository;

import com.example.hellospringboot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByNameContaining(String name);
    List<User> findByAgeGreaterThanEqual(Integer age);
}
```

### 4. Service層（インターフェース）

```java
package com.example.hellospringboot.service;

import com.example.hellospringboot.entity.User;
import java.util.List;
import java.util.Optional;

public interface UserService {
    /**
     * 全ユーザーを取得
     */
    List<User> findAll();
    
    /**
     * IDでユーザーを取得
     */
    Optional<User> findById(Long id);
    
    /**
     * ユーザーを作成
     */
    User create(User user);
    
    /**
     * ユーザーを更新
     */
    User update(Long id, User user);
    
    /**
     * ユーザーを削除
     */
    void delete(Long id);
    
    /**
     * 名前で検索
     */
    List<User> searchByName(String name);
    
    /**
     * 成人ユーザーを取得（ビジネスロジックの例）
     */
    List<User> findAdultUsers();
}
```

### 5. Service層（実装クラス）

```java
package com.example.hellospringboot.service;

import com.example.hellospringboot.entity.User;
import com.example.hellospringboot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // デフォルトで読み取り専用
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    
    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }
    
    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    @Override
    @Transactional  // 更新系はトランザクション有効
    public User create(User user) {
        // ビジネスロジック: 年齢のデフォルト値設定
        if (user.getAge() == null) {
            user.setAge(0);
        }
        return userRepository.save(user);
    }
    
    @Override
    @Transactional
    public User update(Long id, User user) {
        User existingUser = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found: " + id));
        
        // ビジネスロジック: 更新可能な項目のみ反映
        existingUser.setName(user.getName());
        existingUser.setEmail(user.getEmail());
        existingUser.setAge(user.getAge());
        
        return userRepository.save(existingUser);
    }
    
    @Override
    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found: " + id);
        }
        userRepository.deleteById(id);
    }
    
    @Override
    public List<User> searchByName(String name) {
        return userRepository.findByNameContaining(name);
    }
    
    @Override
    public List<User> findAdultUsers() {
        // ビジネスロジック: 20歳以上を成人とする
        return userRepository.findByAgeGreaterThanEqual(20);
    }
}
```

### 6. Controller層

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
     * 全ユーザー取得
     */
    @GetMapping
    public List<User> getAll() {
        return userService.findAll();
    }
    
    /**
     * ID指定でユーザー取得
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getById(@PathVariable Long id) {
        return userService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * ユーザー作成
     */
    @PostMapping
    public ResponseEntity<User> create(@RequestBody User user) {
        User created = userService.create(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    /**
     * ユーザー更新
     */
    @PutMapping("/{id}")
    public ResponseEntity<User> update(
            @PathVariable Long id,
            @RequestBody User user) {
        try {
            User updated = userService.update(id, user);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * ユーザー削除
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            userService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 名前で検索
     */
    @GetMapping("/search")
    public List<User> searchByName(@RequestParam String name) {
        return userService.searchByName(name);
    }
    
    /**
     * 成人ユーザー取得
     */
    @GetMapping("/adults")
    public List<User> getAdults() {
        return userService.findAdultUsers();
    }
}
```

## 🔑 ポイント

### 1. なぜServiceインターフェースを作るのか？

**メリット**:
- テスト時にモック実装に差し替えやすい
- 複数の実装を持つことができる（例: `UserServiceImpl`, `UserServiceCachedImpl`）
- 依存関係が具象クラスではなく抽象に依存する（SOLID原則）

**小規模プロジェクトの場合**:
- インターフェースなしで`UserService`クラスのみでもOK
- プロジェクトの成長に応じて後からインターフェース化

### 2. `@RequiredArgsConstructor`の利点

Lombokの`@RequiredArgsConstructor`は`final`フィールドのコンストラクタを自動生成します。

```java
// これが
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
}

// こうなる
public class UserController {
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
}
```

**コンストラクタインジェクションのメリット**:
- フィールドが`final`にできる（不変性）
- テストでモックを注入しやすい
- 循環依存があるとコンパイルエラーになる

### 3. `@Transactional`の配置

| 配置場所 | 推奨度 | 理由 |
|---|---|---|
| Service層 | ⭐⭐⭐ | ビジネスロジック単位でトランザクション管理 |
| Repository層 | ❌ | 細かすぎる粒度、Serviceで制御すべき |
| Controller層 | ❌ | HTTPの責務と混在してしまう |

```java
@Service
@Transactional(readOnly = true)  // クラスレベル: 読み取り専用
public class UserServiceImpl implements UserService {
    
    @Transactional  // メソッドレベル: 書き込み許可
    public User create(User user) {
        // 複数のRepository操作を1トランザクションで
        User saved = userRepository.save(user);
        auditRepository.save(new Audit("CREATE", saved.getId()));
        return saved;
    }
}
```

## ✅ 動作確認

### 1. アプリケーション起動

```bash
./mvnw spring-boot:run
```

### 2. APIテスト

```bash
# ユーザー作成
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"太郎","email":"taro@example.com","age":25}'

# 全ユーザー取得
curl http://localhost:8080/api/users

# 成人ユーザー取得（ビジネスロジック）
curl http://localhost:8080/api/users/adults

# 名前検索
curl "http://localhost:8080/api/users/search?name=太郎"
```

## 🎨 チャレンジ課題

### 課題1: 複数Repositoryの協調
`Order`（注文）と`Product`（商品）のエンティティを作成し、注文時に在庫を減らすServiceを実装してください。

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    
    @Transactional
    public Order createOrder(Long productId, Integer quantity) {
        // 1. 商品の在庫確認
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        if (product.getStock() < quantity) {
            throw new RuntimeException("Insufficient stock");
        }
        
        // 2. 在庫を減らす
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);
        
        // 3. 注文を作成
        Order order = new Order();
        order.setProductId(productId);
        order.setQuantity(quantity);
        return orderRepository.save(order);
    }
}
```

### 課題2: Service層のテスト
次のステップで学びますが、先にServiceのユニットテストに挑戦してみましょう。

```java
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    @Test
    void ユーザー作成時に年齢がnullならゼロが設定される() {
        // Given
        User user = new User();
        user.setName("Test");
        user.setAge(null);
        
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        
        // When
        User result = userService.create(user);
        
        // Then
        assertEquals(0, result.getAge());
    }
}
```

---

## 📚 このステップで学んだこと

- ✅ Controller / Service / Repository の3層アーキテクチャ
- ✅ 各レイヤーの責務と役割の明確化
- ✅ `@Service`アノテーションの使い方
- ✅ ビジネスロジックの適切な配置（Service層）
- ✅ `@Transactional`の配置場所（Service層が推奨）
- ✅ コンストラクタインジェクションと`@RequiredArgsConstructor`
- ✅ 複数Repositoryの協調処理

**アーキテクチャのメリット**:
- **可読性**: 各クラスの責務が明確
- **テスト容易性**: Service層を独立してテスト可能
- **再利用性**: Serviceを複数のControllerから利用可能
- **保守性**: 変更の影響範囲を局所化

---

## 🐛 トラブルシューティング

### エラー: "Could not autowire. No beans of 'UserService' type found."

**原因**: Serviceクラスに`@Service`アノテーションが付いていない

**解決策**:
```java
@Service  // ← これを忘れずに
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    // ...
}
```

### エラー: "Field injection is not recommended"

**原因**: フィールドインジェクションを使用している（IntelliJの警告）

**解決策**:
```java
### エラー: "Field injection is not recommended"

**原因**: フィールドインジェクションを使用している（IDEの警告）

**解決策**:
```java
// ❌ フィールドインジェクション（非推奨）
@Autowired
private UserRepository userRepository;

// ✅ コンストラクタインジェクション（推奨）
private final UserRepository userRepository;

public UserServiceImpl(UserRepository userRepository) {
    this.userRepository = userRepository;
}
```

// ✅ Lombok使用でさらに簡潔
@RequiredArgsConstructor
public class UserServiceImpl {
    private final UserRepository userRepository;
}
```

### エラー: "org.springframework.dao.InvalidDataAccessApiUsageException: No EntityManager"

**原因**: `@Transactional`が付いていないか、トランザクション外でLazy Fetchを実行

**解決策**:
1. Service層のメソッドに`@Transactional`を追加
2. Lazy Fetchは避けるか、Eager Fetchに変更
3. DTOに変換してからトランザクション外で使用

### 問題: ビジネスロジックをどこに書くべきか迷う

**判断基準**:

**Controller層**:
- リクエストのバリデーション
- レスポンスの整形
- HTTPステータスの決定
- 薄く保つ（ビジネスロジックは書かない）

**Service層**:
- ビジネスロジック
- トランザクション制御
- 複数Repositoryの協調
- ドメインルールの実装

**Repository層**:
- データベースアクセスのみ
- CRUD操作
- カスタムクエリ
- ビジネスロジックは書かない

### 問題: Service層が肥大化してしまう

**解決策**:
1. **1つのServiceクラスに1つのエンティティ**: `UserService`, `OrderService`など
2. **共通処理はヘルパークラスに**: `EmailHelper`, `ValidationHelper`など
3. **ドメインロジックはエンティティに**: リッチドメインモデル
4. **大きいServiceは分割**: `UserRegistrationService`, `UserAuthenticationService`など

---

## 🔄 Gitへのコミットとレビュー依頼

進捗を記録してレビューを受けましょう：

```bash
# 変更をステージング
git add .

# コミット
git commit -m "Step 15: レイヤー化アーキテクチャ完了"

# リモートにプッシュ
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

レビューが完了したら、[Step 16: DI/IoCコンテナの深掘り](STEP_16.md)へ進みましょう！

Spring FrameworkのコアであるDI（依存性注入）とIoC（制御の反転）について、より深く理解します。
