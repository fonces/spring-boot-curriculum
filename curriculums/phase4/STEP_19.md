# Step 19: ベストプラクティスとコーディング規約

## 🎯 このステップの目標

- Spring Bootのベストプラクティスを理解する
- クリーンコードの原則を学ぶ
- コーディング規約を適用する
- リファクタリング手法を実践する

**所要時間**: 約1時間30分

---

## 📋 事前準備

- Phase 4のStep 15-18が完了していること
- 基本的なSpring Bootアプリケーションが動作していること

---

## 💡 クリーンコードの原則

### SOLID原則

| 原則 | 説明 | Spring Bootでの適用 |
|------|------|-------------------|
| **S**ingle Responsibility | 単一責任の原則 | 1クラス1責任 |
| **O**pen/Closed | 開放/閉鎖の原則 | インターフェースで拡張 |
| **L**iskov Substitution | リスコフの置換原則 | 継承の適切な使用 |
| **I**nterface Segregation | インターフェース分離の原則 | 小さなインターフェース |
| **D**ependency Inversion | 依存性逆転の原則 | DIコンテナの活用 |

---

## 🚀 ステップ1: レイヤー分離のベストプラクティス

### 1-1. Controller層の責務

**✅ 良い例**:
```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserCreateRequest request) {
        // Controller: HTTPリクエスト/レスポンスのみ
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

**❌ 悪い例**:
```java
@RestController
public class UserController {

    @Autowired  // ❌ @Autowiredではなく@RequiredArgsConstructor推奨
    private UserRepository userRepository;

    @PostMapping("/users")  // ❌ @RequestMappingがない
    public User createUser(@RequestBody User user) {  // ❌ DTOを使うべき
        // ❌ Controllerでビジネスロジック
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Duplicate email");
        }
        return userRepository.save(user);  // ❌ エンティティを直接返す
    }
}
```

### 1-2. Service層の責務

**✅ 良い例**:
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        log.info("Creating user: {}", request.getEmail());
        
        // ビジネスロジック
        validateUniqueEmail(request.getEmail());
        
        User user = userMapper.toEntity(request);
        User savedUser = userRepository.save(user);
        
        return userMapper.toResponse(savedUser);
    }

    private void validateUniqueEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("ユーザー", "メールアドレス", email);
        }
    }
}
```

**❌ 悪い例**:
```java
@Service
public class UserService {

    @Autowired  // ❌ フィールドインジェクション
    private UserRepository userRepository;

    // ❌ @Transactionalがない
    public User createUser(User user) {  // ❌ DTOを使うべき
        // ❌ バリデーションなし
        // ❌ ログなし
        return userRepository.save(user);
    }
}
```

---

## 🚀 ステップ2: 依存性注入のベストプラクティス

### 2-1. コンストラクタインジェクション（推奨）

**✅ 良い例**:
```java
@Service
@RequiredArgsConstructor  // Lombokで自動生成
public class UserService {

    private final UserRepository userRepository;  // finalで不変
    private final UserMapper userMapper;

    // コンストラクタは@RequiredArgsConstructorが生成
}
```

**または明示的なコンストラクタ**:
```java
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }
}
```

### 2-2. フィールドインジェクション（非推奨）

**❌ 避けるべき**:
```java
@Service
public class UserService {

    @Autowired  // ❌ テストしにくい、循環参照に気づきにくい
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;
}
```

### 2-3. セッターインジェクション（特殊な場合のみ）

```java
@Service
public class UserService {

    private UserRepository userRepository;

    @Autowired  // オプショナルな依存性の場合のみ使用
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

---

## 🚀 ステップ3: 例外処理のベストプラクティス

### 3-1. カスタム例外の使用

**✅ 良い例**:
```java
public class UserService {

    public UserResponse getUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
```

**❌ 悪い例**:
```java
public class UserService {

    public User getUserById(Long id) {
        Optional<User> user = userRepository.findById(id);
        if (!user.isPresent()) {
            throw new RuntimeException("User not found");  // ❌ 汎用例外
        }
        return user.get();  // ❌ Optionalの.get()は避ける
    }
}
```

### 3-2. 適切なログ出力

**✅ 良い例**:
```java
try {
    processPayment(order);
} catch (PaymentException ex) {
    log.error("Payment failed for order {}: {}", order.getId(), ex.getMessage(), ex);
    throw new OrderProcessingException("決済処理に失敗しました", ex);
}
```

**❌ 悪い例**:
```java
try {
    processPayment(order);
} catch (Exception ex) {  // ❌ 汎用例外をキャッチ
    ex.printStackTrace();  // ❌ printStackTraceは使わない
    // ❌ 例外を握りつぶす（再スローしない）
}
```

---

## 🚀 ステップ4: トランザクション管理のベストプラクティス

### 4-1. @Transactionalの適切な使用

**✅ 良い例**:
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // デフォルトは読み取り専用
public class OrderService {

    @Transactional  // 書き込み時のみ上書き
    public OrderResponse createOrder(OrderCreateRequest request) {
        // トランザクション内で複数の操作
        Order order = orderMapper.toEntity(request);
        Order savedOrder = orderRepository.save(order);
        
        // 在庫を減らす
        inventoryService.decreaseStock(request.getProductId(), request.getQuantity());
        
        // 通知を送信（別トランザクション）
        notificationService.sendOrderConfirmation(savedOrder.getId());
        
        return orderMapper.toResponse(savedOrder);
    }

    // 読み取り専用（クラスレベルの設定を使用）
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }
}
```

### 4-2. トランザクション境界

**❌ 悪い例**:
```java
@Service
public class OrderService {

    @Transactional
    public void processOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        
        // ❌ 外部API呼び出しをトランザクション内で
        externalPaymentService.charge(order.getAmount());  // 遅い！
        
        order.setStatus("PAID");
        orderRepository.save(order);
    }
}
```

**✅ 良い例**:
```java
@Service
public class OrderService {

    public void processOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        
        // 外部API呼び出しはトランザクション外で
        PaymentResult result = externalPaymentService.charge(order.getAmount());
        
        // トランザクションは最小限に
        updateOrderStatus(orderId, result);
    }

    @Transactional
    private void updateOrderStatus(Long orderId, PaymentResult result) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(result.isSuccess() ? "PAID" : "FAILED");
        orderRepository.save(order);
    }
}
```

---

## 🚀 ステップ5: DTOとエンティティの使い分け

### 5-1. レイヤー間のデータフロー

```
Client → Controller → Service → Repository → Database
  DTO  →    DTO     →  Entity →   Entity   →  Table
  DTO  ←    DTO     ←  Entity ←   Entity   ←  Table
```

**✅ 良い例**:
```java
@RestController
public class UserController {

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserCreateRequest request) {  // DTO
        UserResponse response = userService.createUser(request);  // DTO
        return ResponseEntity.ok(response);  // DTO
    }
}

@Service
public class UserService {

    public UserResponse createUser(UserCreateRequest request) {  // DTO
        User entity = userMapper.toEntity(request);  // DTO → Entity
        User savedEntity = userRepository.save(entity);  // Entity
        return userMapper.toResponse(savedEntity);  // Entity → DTO
    }
}
```

---

## 🚀 ステップ6: パフォーマンス最適化

### 6-1. N+1問題の回避

**❌ 悪い例**:
```java
// 1 + N クエリが発生
public List<PostResponse> getAllPosts() {
    List<Post> posts = postRepository.findAll();  // 1クエリ
    return posts.stream()
            .map(post -> {
                post.getUser().getName();  // Nクエリ（投稿ごと）
                return postMapper.toResponse(post);
            })
            .collect(Collectors.toList());
}
```

**✅ 良い例**:
```java
// 1クエリで取得
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    @Query("SELECT p FROM Post p JOIN FETCH p.user")
    List<Post> findAllWithUser();
}

public List<PostResponse> getAllPosts() {
    List<Post> posts = postRepository.findAllWithUser();  // 1クエリ
    return postMapper.toResponseList(posts);
}
```

### 6-2. ページネーション

**✅ 良い例**:
```java
@Service
public class UserService {

    public Page<UserResponse> getUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return users.map(userMapper::toResponse);
    }
}

@RestController
public class UserController {

    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.by(sort[0]).with(Direction.fromString(sort[1]))));
        Page<UserResponse> users = userService.getUsers(pageable);
        return ResponseEntity.ok(users);
    }
}
```

---

## 🚀 ステップ7: コーディング規約

### 7-1. 命名規則

| 要素 | 規則 | 例 |
|------|------|-----|
| クラス | PascalCase | `UserService`, `OrderRepository` |
| メソッド | camelCase | `createUser()`, `findById()` |
| 変数 | camelCase | `userId`, `userName` |
| 定数 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT`, `DEFAULT_PAGE_SIZE` |
| パッケージ | lowercase | `com.example.hellospringboot` |

### 7-2. メソッドの長さ

**✅ 良い例**:
```java
public UserResponse createUser(UserCreateRequest request) {
    validateUniqueEmail(request.getEmail());
    User user = buildUser(request);
    User savedUser = saveUser(user);
    return mapToResponse(savedUser);
}

private void validateUniqueEmail(String email) {
    if (userRepository.existsByEmail(email)) {
        throw new DuplicateResourceException("ユーザー", "メールアドレス", email);
    }
}

private User buildUser(UserCreateRequest request) {
    return userMapper.toEntity(request);
}

private User saveUser(User user) {
    return userRepository.save(user);
}

private UserResponse mapToResponse(User user) {
    return userMapper.toResponse(user);
}
```

### 7-3. コメント

**✅ 良い例**:
```java
/**
 * ユーザーを作成します。
 * 
 * @param request ユーザー作成リクエスト
 * @return 作成されたユーザー
 * @throws DuplicateResourceException メールアドレスが既に存在する場合
 */
public UserResponse createUser(UserCreateRequest request) {
    // ...
}
```

**❌ 悪い例**:
```java
// ユーザーを作成する  ← コードと同じ内容
public UserResponse createUser(UserCreateRequest request) {
    // iを1から10まで繰り返す  ← 自明なコメント
    for (int i = 1; i <= 10; i++) {
        // ...
    }
}
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: リファクタリング

以下のコードをリファクタリングしてください：

```java
@RestController
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/user")
    public User create(@RequestBody User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email exists");
        }
        return userRepository.save(user);
    }
}
```

### チャレンジ 2: パフォーマンス改善

N+1問題が発生しているコードを見つけて修正してください。

### チャレンジ 3: テストコード

ユニットテストとintegrationテストを追加してください。

---

## 📚 このステップで学んだこと

- ✅ SOLID原則
- ✅ レイヤー分離
- ✅ 依存性注入のベストプラクティス
- ✅ 例外処理のパターン
- ✅ トランザクション管理
- ✅ パフォーマンス最適化
- ✅ コーディング規約

---

## 💡 補足: チェックリスト

### コードレビュー時のチェックリスト

- [ ] コンストラクタインジェクションを使用している
- [ ] DTOとエンティティを分離している
- [ ] カスタム例外を使用している
- [ ] @Transactionalを適切に使用している
- [ ] ログを適切に出力している
- [ ] N+1問題が発生していない
- [ ] メソッドは単一責任を持っている
- [ ] マジックナンバーを使用していない
- [ ] テストコードがある

---

## 🔄 Gitへのコミット

```bash
git add .
git commit -m "Step 19: DTOとEntityの分離完了"
git push origin main
```

---

## ➡️ 次のステップ

次は[Step 20: ロギング](STEP_20.md)へ進みましょう！

Phase 3で学んだことを振り返り、実践的な演習を行います。

---

お疲れさまでした！ 🎉

ベストプラクティスを身につけることで、保守性・拡張性の高い
コードを書けるようになります。最初は大変ですが、
習慣化すれば自然と良いコードが書けるようになります！
