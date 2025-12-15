# Step 9: @Transactionalでトランザクション管理

## 🎯 このステップの目標

- トランザクションの概念と重要性を理解できる
- `@Transactional`アノテーションの使い方を理解できる
- トランザクションのロールバックとコミットの仕組みを理解できる
- 実務でよくあるトランザクションの使用例を実装できる

**所要時間**: 約50分

---

## 📋 事前準備

- [Step 8](STEP_8.md)が完了していること
- CRUD操作が実装されていること
- MySQLコンテナが起動していること

---

## 🧩 トランザクションとは

### トランザクションの定義

**トランザクション**とは、複数のデータベース操作を**1つのまとまり**として扱う仕組みです。

```sh
┌────────────────────────────────────┐
│   トランザクション開始               │
├────────────────────────────────────┤
│  操作1: 在庫から商品を減らす        │
│  操作2: 注文レコードを作成          │
│  操作3: 売上金額を更新              │
├────────────────────────────────────┤
│  全て成功 → コミット（確定）         │
│  1つでも失敗 → ロールバック（取消）   │
└────────────────────────────────────┘
```

### ACID特性

トランザクションは以下の4つの特性を持ちます：

| 特性 | 英語 | 説明 |
|---|---|---|
| **A**tomicity | 原子性 | 全ての操作が成功するか、全て失敗するか（中途半端な状態にならない） |
| **C**onsistency | 一貫性 | データベースの整合性が保たれる |
| **I**solation | 独立性 | 複数のトランザクションが互いに影響しない |
| **D**urability | 永続性 | コミット後のデータは永続化される |

### なぜトランザクションが必要か

**問題のあるコード（トランザクションなし）**:

```java
public void transferMoney(Long fromId, Long toId, Integer amount) {
    Account from = accountRepository.findById(fromId).orElseThrow();
    Account to = accountRepository.findById(toId).orElseThrow();
    
    from.setBalance(from.getBalance() - amount);  // ① 引き落とし
    accountRepository.save(from);
    
    // ここでエラーが発生したら？
    // ① は実行済みだが ② は未実行 → お金が消える！
    
    to.setBalance(to.getBalance() + amount);      // ② 入金
    accountRepository.save(to);
}
```

**トランザクションを使った場合**:

```java
@Transactional  // この1行で全ての操作が1つのトランザクションになる
public void transferMoney(Long fromId, Long toId, Integer amount) {
    // 同じコード
    // エラーが発生したら全てロールバック（①も取り消される）
}
```

---

## 🚀 ステップ1: Serviceクラスの作成

### 1-1. なぜServiceクラスが必要か

**レイヤー化アーキテクチャ**:

```sh
┌────────────────────────────────────┐
│  Controller (HTTPの入出力)          │
│  - リクエストの受け取り              │
│  - レスポンスの返却                  │
└────────────┬───────────────────────┘
             │
             ↓
┌────────────────────────────────────┐
│  Service (ビジネスロジック)          │
│  - トランザクション管理 ← ここ！      │
│  - 複数のリポジトリを組み合わせ       │
└────────────┬───────────────────────┘
             │
             ↓
┌────────────────────────────────────┐
│  Repository (データアクセス)         │
│  - 単純なCRUD操作                    │
└────────────────────────────────────┘
```

**原則**:
- **Controller**: `@Transactional`を付けない
- **Service**: `@Transactional`を付ける ✅
- **Repository**: `@Transactional`は不要（Spring Data JPAが自動管理）

### 1-2. ProductServiceの作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/ProductService.java`

```java
package com.example.hellospringboot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository productRepository;
    
    // 読み取り専用（パフォーマンス最適化）
    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }
    
    // 書き込みトランザクション
    @Transactional
    public Product save(Product product) {
        return productRepository.save(product);
    }
    
    @Transactional
    public Product update(Long id, Product productDetails) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        
        return productRepository.save(product);
    }
    
    @Transactional
    public void deleteById(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }
}
```

### 1-3. コードの解説

#### `@Service`

このクラスがビジネスロジック層であることを示します。

#### `@Transactional`

メソッドをトランザクション境界として扱います。

- **メソッド開始時**: トランザクション開始
- **メソッド正常終了時**: コミット（確定）
- **例外発生時**: ロールバック（取り消し）

#### `@Transactional(readOnly = true)`

読み取り専用トランザクションです。

**メリット**:
- **パフォーマンス向上**: データベースの最適化が効く
- **安全性**: 誤ってデータを変更できない

**注意**: `readOnly = true`なのに`save()`を呼ぶとエラーになります。

#### 例外処理

```java
.orElseThrow(() -> new RuntimeException("Product not found"));
```

`RuntimeException`（非検査例外）をスローすると、トランザクションが**自動的にロールバック**されます。

---

## 🚀 ステップ2: Controllerの修正

### 2-1. ServiceクラスをControllerで使用

`ProductController.java`を修正して、Repositoryの代わりにServiceを使用します。

**ファイルパス**: `src/main/java/com/example/hellospringboot/ProductController.java`

```java
package com.example.hellospringboot;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    
    // 変更前: private final ProductRepository productRepository;
    private final ProductService productService;  // Serviceを使用
    
    @GetMapping
    public List<Product> getAllProducts() {
        return productService.findAll();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product saved = productService.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @RequestBody Product productDetails) {
        try {
            Product updated = productService.update(id, productDetails);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
```

---

## 🚀 ステップ3: トランザクションの動作確認

### 3-1. アプリケーションの再起動

```bash
./mvnw clean install
./mvnw spring-boot:run
```

### 3-2. 基本動作の確認

```bash
# 商品作成
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "テスト商品",
    "description": "トランザクションテスト用",
    "price": 5000
  }'

# 全商品取得
curl http://localhost:8080/api/products
```

### 3-3. ロールバックの確認

エラーを発生させてロールバックを確認します。

**ProductServiceに一時的なテストメソッドを追加**:

```java
@Transactional
public void testRollback() {
    Product product1 = new Product();
    product1.setName("ロールバックテスト1");
    product1.setDescription("これは保存される？");
    product1.setPrice(1000);
    productRepository.save(product1);
    
    // 意図的にエラーを発生させる
    throw new RuntimeException("テスト用エラー");
    
    // この行は実行されない
    // Product product2 = new Product();
    // ...
}
```

**Controllerにテストエンドポイントを追加**:

```java
@PostMapping("/test-rollback")
public ResponseEntity<String> testRollback() {
    try {
        productService.testRollback();
        return ResponseEntity.ok("成功");
    } catch (RuntimeException e) {
        return ResponseEntity.ok("エラー発生: " + e.getMessage());
    }
}
```

**実行**:

```bash
curl -X POST http://localhost:8080/api/products/test-rollback
```

**確認**:

```bash
curl http://localhost:8080/api/products
```

「ロールバックテスト1」という商品が**保存されていないこと**を確認してください。これがロールバックの効果です。

**重要**: テストが終わったら、テストコードは削除してください。

---

## 🚀 ステップ4: 複数操作のトランザクション

### 4-1. 在庫管理の例

実務でよくある「在庫を減らして注文を作成」する処理を実装します。

**Orderエンティティの作成**:

**ファイルパス**: `src/main/java/com/example/hellospringboot/Order.java`

```java
package com.example.hellospringboot;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long productId;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false)
    private Integer totalPrice;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

**OrderRepositoryの作成**:

**ファイルパス**: `src/main/java/com/example/hellospringboot/OrderRepository.java`

```java
package com.example.hellospringboot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
}
```

**Productエンティティに在庫フィールドを追加**:

`Product.java`に以下のフィールドを**追加**:

```java
@Column(nullable = false)
private Integer stock = 0;  // 在庫数
```

**アプリケーションを再起動**して、テーブルを更新:

```bash
./mvnw spring-boot:run
```

### 4-2. 注文処理の実装

**ProductServiceに注文メソッドを追加**:

```java
@Transactional
public Order createOrder(Long productId, Integer quantity) {
    // 商品を取得
    Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
    
    // 在庫チェック
    if (product.getStock() < quantity) {
        throw new RuntimeException("在庫不足: 残り" + product.getStock() + "個");
    }
    
    // 在庫を減らす
    product.setStock(product.getStock() - quantity);
    productRepository.save(product);
    
    // 注文を作成
    Order order = new Order();
    order.setProductId(productId);
    order.setQuantity(quantity);
    order.setTotalPrice(product.getPrice() * quantity);
    
    return orderRepository.save(order);
}
```

**注意**: `OrderRepository`をインジェクションする必要があります:

```java
private final ProductRepository productRepository;
private final OrderRepository orderRepository;  // 追加
```

**Controllerに注文エンドポイントを追加**:

```java
@PostMapping("/{id}/order")
public ResponseEntity<Order> createOrder(
        @PathVariable Long id,
        @RequestParam Integer quantity) {
    try {
        Order order = productService.createOrder(id, quantity);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    } catch (RuntimeException e) {
        return ResponseEntity.badRequest().build();
    }
}
```

### 4-3. トランザクションの動作確認

**在庫付きの商品を作成**:

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "ノートPC",
    "description": "在庫10個",
    "price": 100000,
    "stock": 10
  }'
```

**注文を作成（成功パターン）**:

```bash
curl -X POST "http://localhost:8080/api/products/1/order?quantity=3"
```

**商品の在庫を確認**:

```bash
curl http://localhost:8080/api/products/1
```

`stock`が`7`（10 - 3）になっていることを確認してください。

**在庫以上の注文（失敗パターン）**:

```bash
curl -i -X POST "http://localhost:8080/api/products/1/order?quantity=100"
```

`400 Bad Request`が返却され、在庫が減っていないことを確認してください。

---

## ✅ ステップ5: 動作確認

### 5-1. トランザクションのロールバック確認

MySQLのログを見ながら実行します。

**ターミナル1**: アプリケーションのログを表示

```bash
./mvnw spring-boot:run
```

**ターミナル2**: 在庫不足の注文を実行

```bash
curl -X POST "http://localhost:8080/api/products/1/order?quantity=999"
```

**ターミナル1**のログに`Transaction rolled back`などのメッセージが出力されることを確認してください。

---

## 🎨 チャレンジ課題

基本が理解できたら、以下にチャレンジしてみましょう：

### チャレンジ 1: トランザクションの伝播

`@Transactional(propagation = ...)`で伝播レベルを変更してみてください。

**ヒント**:

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void method() {
    // 常に新しいトランザクションを開始
}
```

### チャレンジ 2: 検査例外でもロールバック

デフォルトでは`RuntimeException`のみロールバックされます。検査例外でもロールバックするには：

```java
@Transactional(rollbackFor = Exception.class)
public void method() throws Exception {
    // 全ての例外でロールバック
}
```

### チャレンジ 3: ロールバックしない例外

特定の例外ではロールバックしないようにする：

```java
@Transactional(noRollbackFor = IllegalArgumentException.class)
public void method() {
    // IllegalArgumentException発生時はロールバックしない
}
```

---

## 🐛 トラブルシューティング

### @Transactionalが効かない

**原因1**: プロキシが効いていない

`@Transactional`は**プロキシ経由**でのみ有効です。

```java
// ❌ 同じクラス内での呼び出しは効かない
public void methodA() {
    methodB();  // トランザクションは効かない
}

@Transactional
public void methodB() {
    // ...
}
```

**解決策**: 別のServiceクラスに分ける。

**原因2**: クラスレベルで`@Transactional`を付けていない

Serviceクラスにクラスレベルで付けるのも一般的です：

```java
@Service
@Transactional  // クラス全体に適用
public class ProductService {
    // 全メソッドがトランザクション管理される
}
```

### ロールバックしたのにデータが残っている

**原因**: `readOnly = true`を付けたメソッドで更新している

**解決策**: `readOnly = true`を削除。

### LazyInitializationException

**エラーメッセージ**:

```sh
org.hibernate.LazyInitializationException: could not initialize proxy
```

**原因**: トランザクション外で遅延ロードしようとしている

**解決策**: `@Transactional`を付ける、またはEAGERフェッチに変更。

---

## 📚 このステップで学んだこと

- ✅ トランザクションの概念とACID特性を理解した
- ✅ `@Transactional`アノテーションの使い方を学んだ
- ✅ Serviceクラスを作成してビジネスロジックを分離した
- ✅ トランザクションのロールバックとコミットを確認した
- ✅ 複数のリポジトリ操作を1つのトランザクションにまとめられるようになった
- ✅ `readOnly = true`でパフォーマンスを最適化できることを学んだ
- ✅ 実務でよくある在庫管理のパターンを実装した

---

## 💡 補足: トランザクションの分離レベル

### Isolation Level

複数のトランザクションが同時実行される際の動作を制御します。

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public void method() {
    // ...
}
```

| レベル | 説明 | ダーティリード | ノンリピータブルリード | ファントムリード |
|---|---|---|---|---|
| `READ_UNCOMMITTED` | 最も緩い | 発生 | 発生 | 発生 |
| `READ_COMMITTED` | デフォルト（MySQL） | 防げる | 発生 | 発生 |
| `REPEATABLE_READ` | MySQL InnoDB デフォルト | 防げる | 防げる | 発生 |
| `SERIALIZABLE` | 最も厳しい | 防げる | 防げる | 防げる |

通常は`READ_COMMITTED`または`REPEATABLE_READ`で十分です。

---

## ➡️ 次のステップ

[Step 10: カスタムクエリ](STEP_10.md)へ進みましょう！

次のステップでは、Spring Data JPAの命名規則だけでは表現できない複雑なクエリを実装する方法を学びます。
