# Step 9: @Transactionalでトランザクション管理

## 🎯 このステップの目標

- トランザクションとは何かを理解する
- `@Transactional`アノテーションの使い方を学ぶ
- トランザクションの伝播（Propagation）を理解する
- ロールバックの仕組みを学ぶ
- 複数テーブル更新を1つのトランザクションで実行する

**所要時間**: 約1時間

---

## 📋 事前準備

- Step 8までのCRUD操作が実装されていること
- UserエンティティとUserServiceが動作していること

**Step 8をまだ完了していない場合**: [Step 8: CRUD操作の完成](STEP_8.md)を先に進めてください。

---

## 💡 トランザクションとは？

### トランザクションの定義

**トランザクション** = 複数のデータベース操作を1つのまとまりとして扱う仕組み

**ACID特性**:
- **A**tomicity（原子性）: すべて成功するか、すべて失敗するか
- **C**onsistency（一貫性）: データの整合性が保たれる
- **I**solation（独立性）: 他のトランザクションの影響を受けない
- **D**urability（永続性）: 完了した変更は永続化される

### 実例: 銀行の送金

```
1. A口座から1万円引き出す
2. B口座に1万円入金する
```

**トランザクションなし**:
- ①が成功、②が失敗 → お金が消える！❌

**トランザクションあり**:
- ①②両方成功 → コミット ✅
- どちらか失敗 → ロールバック（元に戻す） ✅

---

## 🚀 ステップ1: 基本的な@Transactional

### 1-1. デフォルトでは自動的にトランザクション管理

Spring Data JPAでは、リポジトリのメソッドは自動的にトランザクション内で実行されます。

```java
userRepository.save(user);  // 自動的にトランザクション内
```

### 1-2. サービス層での@Transactional

複数の操作をまとめる場合、サービス層に`@Transactional`を付けます。

`UserService.java`のメソッドに`@Transactional`を追加：

**ファイルパス**: `src/main/java/com/example/hellospringboot/service/UserService.java`

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
@Transactional(readOnly = true)  // クラスレベル: デフォルトで読み取り専用
public class UserService {

    private final UserRepository userRepository;

    /**
     * ユーザーを作成
     */
    @Transactional  // メソッドレベル: 書き込みトランザクション
    public User createUser(User user) {
        return userRepository.save(user);
    }

    /**
     * 全ユーザーを取得
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * IDでユーザーを取得
     */
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * ユーザーを更新
     */
    @Transactional
    public Optional<User> updateUser(Long id, User userDetails) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    existingUser.setName(userDetails.getName());
                    existingUser.setEmail(userDetails.getEmail());
                    existingUser.setAge(userDetails.getAge());
                    return userRepository.save(existingUser);
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

### 1-3. アノテーションの解説

#### クラスレベルの@Transactional
```java
@Transactional(readOnly = true)
public class UserService {
```
- クラス全体のデフォルト設定
- `readOnly = true`: 読み取り専用（パフォーマンス最適化）

#### メソッドレベルの@Transactional
```java
@Transactional
public User createUser(User user) {
```
- クラスレベルの設定を上書き
- 書き込み操作なので`readOnly`なし

---

## 🚀 ステップ2: ロールバックを理解する

### 2-1. 新しいエンティティの追加

注文を管理する`Order`エンティティを作成します。

**ファイルパス**: `src/main/java/com/example/hellospringboot/entity/Order.java`

```java
package com.example.hellospringboot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer totalPrice;
}
```

### 2-2. OrderRepositoryの作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/repository/OrderRepository.java`

```java
package com.example.hellospringboot.repository;

import com.example.hellospringboot.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
}
```

### 2-3. OrderServiceの作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/service/OrderService.java`

```java
package com.example.hellospringboot.service;

import com.example.hellospringboot.entity.Order;
import com.example.hellospringboot.entity.User;
import com.example.hellospringboot.repository.OrderRepository;
import com.example.hellospringboot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    /**
     * 注文を作成（トランザクションあり）
     * ユーザーが存在しない場合はエラーで全体をロールバック
     */
    @Transactional
    public Order createOrder(Long userId, String productName, Integer quantity, Integer price) {
        // ①ユーザーの存在確認
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // ②注文を作成
        Order order = Order.builder()
                .userId(user.getId())
                .productName(productName)
                .quantity(quantity)
                .totalPrice(price * quantity)
                .build();

        // ③保存
        return orderRepository.save(order);
    }

    /**
     * 注文を作成（意図的にエラーを発生させる例）
     */
    @Transactional
    public Order createOrderWithError(Long userId, String productName, Integer quantity, Integer price) {
        // ①注文を保存
        Order order = Order.builder()
                .userId(userId)
                .productName(productName)
                .quantity(quantity)
                .totalPrice(price * quantity)
                .build();
        orderRepository.save(order);

        // ②意図的にエラーを発生
        if (true) {  // 常にエラー
            throw new RuntimeException("Intentional error for rollback test");
        }

        return order;
    }
}
```

### 2-4. OrderControllerの作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/controller/OrderController.java`

```java
package com.example.hellospringboot.controller;

import com.example.hellospringboot.entity.Order;
import com.example.hellospringboot.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 注文作成
     * POST /api/orders
     */
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String productName = request.get("productName").toString();
            Integer quantity = Integer.valueOf(request.get("quantity").toString());
            Integer price = Integer.valueOf(request.get("price").toString());

            Order order = orderService.createOrder(userId, productName, quantity, price);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * エラーテスト用（ロールバック確認）
     * POST /api/orders/test-rollback
     */
    @PostMapping("/test-rollback")
    public ResponseEntity<?> testRollback(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String productName = request.get("productName").toString();
            Integer quantity = Integer.valueOf(request.get("quantity").toString());
            Integer price = Integer.valueOf(request.get("price").toString());

            orderService.createOrderWithError(userId, productName, quantity, price);
            return ResponseEntity.ok(Map.of("message", "Success"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
```

---

## ✅ ステップ3: ロールバックの動作確認

### 3-1. アプリケーション起動

アプリケーションを起動すると、`orders`テーブルが自動作成されます。

### 3-2. ユーザーを作成

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "email": "test@example.com",
    "age": 30
  }'
```

### 3-3. 正常な注文作成

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "productName": "Laptop",
    "quantity": 2,
    "price": 1000
  }'
```

**期待される結果**:
```json
{
  "id": 1,
  "userId": 1,
  "productName": "Laptop",
  "quantity": 2,
  "totalPrice": 2000
}
```

H2 Consoleで確認：
```sql
SELECT * FROM orders;
```

データが保存されています。

### 3-4. ロールバックのテスト

```bash
curl -X POST http://localhost:8080/api/orders/test-rollback \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "productName": "Mouse",
    "quantity": 5,
    "price": 20
  }'
```

**期待される結果**:
```json
{
  "error": "Intentional error for rollback test"
}
```

H2 Consoleで確認：
```sql
SELECT * FROM orders;
```

**Mouse の注文は保存されていません！** → ロールバックされました。

---

## 🚀 ステップ4: トランザクションの伝播（Propagation）

### 4-1. Propagationとは？

トランザクション内で別のトランザクションメソッドを呼ぶ場合の振る舞いを制御します。

**主な伝播レベル**:

| Propagation | 説明 |
|-------------|------|
| `REQUIRED` (デフォルト) | トランザクションがあれば参加、なければ新規作成 |
| `REQUIRES_NEW` | 常に新しいトランザクションを作成 |
| `SUPPORTS` | トランザクションがあれば参加、なくてもOK |
| `NOT_SUPPORTED` | トランザクションを使用しない |
| `MANDATORY` | トランザクションが必須（ないとエラー） |
| `NEVER` | トランザクション内で呼ばれるとエラー |

### 4-2. REQUIRES_NEWの例

`UserService.java`に以下を追加：

```java
/**
 * 別のトランザクションでログ保存（ロールバックの影響を受けない）
 */
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void logAction(String action) {
    // 実際にはログテーブルに保存
    System.out.println("LOG: " + action);
}
```

**使用例**:
```java
@Transactional
public Order createOrderWithLogging(Long userId, String productName, Integer quantity, Integer price) {
    // ログ保存（別トランザクション）
    userService.logAction("Creating order for user: " + userId);
    
    // 注文作成（メイントランザクション）
    Order order = // ...
    orderRepository.save(order);
    
    // エラーが発生してもログは残る
    if (someCondition) {
        throw new RuntimeException("Error!");
    }
    
    return order;
}
```

---

## 🚀 ステップ5: ロールバック条件のカスタマイズ

### 5-1. デフォルトのロールバック条件

デフォルトでは、**RuntimeException（非検査例外）** でロールバックされます。

```java
@Transactional
public void method() {
    // RuntimeException → ロールバック
    throw new RuntimeException("Error");
    
    // Exception（検査例外） → ロールバックされない！
    throw new Exception("Error");
}
```

### 5-2. ロールバック条件の指定

```java
@Transactional(rollbackFor = Exception.class)
public void method() throws Exception {
    // Exceptionでもロールバック
    throw new Exception("Error");
}
```

```java
@Transactional(noRollbackFor = IllegalArgumentException.class)
public void method() {
    // IllegalArgumentExceptionではロールバックしない
    throw new IllegalArgumentException("Invalid argument");
}
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: 複数操作のトランザクション

ユーザー作成時に、ウェルカムメッセージを同時に作成するサービスを実装してください。
どちらかが失敗したら、両方ロールバックされることを確認してください。

**ヒント**:
```java
@Transactional
public User createUserWithWelcomeMessage(User user) {
    User savedUser = userRepository.save(user);
    
    // ウェルカムメッセージ作成（別テーブル）
    Message message = new Message();
    message.setUserId(savedUser.getId());
    message.setContent("Welcome!");
    messageRepository.save(message);
    
    return savedUser;
}
```

### チャレンジ 2: 在庫管理

商品の在庫を管理するシステムで、注文時に在庫を減らす処理を実装してください。
在庫が足りない場合はエラーを投げてロールバックしてください。

### チャレンジ 3: 伝播レベルの実験

`REQUIRES_NEW`と`REQUIRED`の違いを実際に試してみてください。

---

## 🐛 トラブルシューティング

### @Transactionalが効かない

**原因1**: 同じクラス内のメソッド呼び出し

```java
// NG: @Transactionalが効かない
public void methodA() {
    methodB();  // 同じクラスの@Transactionalメソッド
}

@Transactional
public void methodB() {
    // トランザクションが開始されない
}
```

**解決策**: 別のサービスから呼び出す

**原因2**: privateメソッド

```java
// NG: privateメソッドではプロキシが作れない
@Transactional
private void method() {
    // 効かない
}
```

**解決策**: publicメソッドにする

### トランザクションが自動コミットされない

**原因**: `@Transactional`を付け忘れ

**解決策**: サービス層のメソッドに`@Transactional`を付ける

### readOnly=trueで更新できない

**エラー**: "Connection is read-only"

**原因**: 読み取り専用トランザクションで更新操作

**解決策**: 更新メソッドには`@Transactional`（readOnlyなし）を使用

---

## 📚 このステップで学んだこと

- ✅ トランザクションの概念とACID特性
- ✅ `@Transactional`アノテーションの基本的な使い方
- ✅ ロールバックの仕組みと動作確認
- ✅ `readOnly`による最適化
- ✅ トランザクションの伝播（Propagation）
- ✅ ロールバック条件のカスタマイズ
- ✅ 複数テーブル操作を1つのトランザクションで管理

---

## 💡 補足: トランザクションの分離レベル

### Isolation Level

複数のトランザクションが同時実行される際の振る舞いを制御します。

| レベル | 説明 |
|--------|------|
| `READ_UNCOMMITTED` | コミット前のデータも読める（ダーティリード） |
| `READ_COMMITTED` | コミット済みデータのみ読める |
| `REPEATABLE_READ` | 同じ読み取りは同じ結果を返す |
| `SERIALIZABLE` | 完全に直列実行（最も安全、最も遅い） |

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public void method() {
    // ...
}
```

**通常はデフォルト設定で問題ありません。**

---

## 💡 補足: Spring Bootのトランザクション管理の仕組み

### AOPとプロキシ

Spring Bootは**AOP（Aspect Oriented Programming）**でトランザクション管理を実装しています。

```
呼び出し元 → プロキシ（トランザクション開始） → 実際のメソッド → プロキシ（コミット/ロールバック）
```

**プロキシが行うこと**:
1. トランザクション開始
2. 実際のメソッド実行
3. 例外がなければコミット
4. 例外があればロールバック

**だから同じクラス内の呼び出しでは効かない！**

---

## 🔄 Gitへのコミットとレビュー依頼

進捗を記録してレビューを受けましょう：

```bash
git add .
git commit -m "Step 9: @Transactionalでトランザクション管理実装完了"
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

レビューが完了したら、[Step 10: カスタムクエリ](STEP_10.md)へ進みましょう！

次のステップでは、`findByName`のようなメソッド名でクエリを自動生成したり、
`@Query`アノテーションでJPQLを書いたりする方法を学びます。
より柔軟なデータ検索ができるようになります！

---

お疲れさまでした！ 🎉

トランザクション管理はデータの整合性を保つ重要な技術です。
`@Transactional`を正しく使えるようになると、堅牢なアプリケーションが作れます！
