# Step 12: MyBatisの基礎

## 🎯 このステップの目標

- MyBatisとは何か、なぜ必要なのかを理解できる
- Spring BootでMyBatisを設定できる
- XMLマッパーとアノテーションベースのクエリを書ける
- MyBatisでCRUD操作を実装できる
- JPAとMyBatisの違いを理解できる

**所要時間**: 約60分

---

## 📋 事前準備

- [Phase 2](../phase2/STEP_11.md)が完了していること
- MySQLコンテナが起動していること
- `Product`エンティティと`ProductRepository`（JPA）が実装済みであること

---

## 🧩 MyBatisとは

### MyBatisの特徴

**MyBatis** は、SQLを直接記述できるJavaの永続化フレームワークです。

```
┌─────────────────────────────────────────┐
│  Spring Data JPA (Hibernate)            │
│  ・メソッド名でクエリ自動生成             │
│  ・抽象度が高い                          │
│  ・SQLを意識しにくい                     │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  MyBatis                                │
│  ・SQLを直接記述                         │
│  ・細かい制御が可能                       │
│  ・パフォーマンスチューニングしやすい      │
└─────────────────────────────────────────┘
```

### JPAとMyBatisの比較

| 観点 | JPA (Hibernate) | MyBatis |
|---|---|---|
| **SQL記述** | 自動生成（JPQL/Criteria） | 手動で記述 |
| **学習コスト** | 高い（ORM概念の理解が必要） | 低い（SQLが書ければOK） |
| **複雑なクエリ** | 難しい | 簡単 |
| **保守性** | エンティティ中心 | SQL中心 |
| **パフォーマンス** | N+1問題に注意 | SQLを最適化しやすい |
| **推奨用途** | シンプルなCRUD | 複雑な検索、集計、レポート |

### MyBatisが適している場面

- ✅ 複雑なJOIN、サブクエリが必要
- ✅ 既存のSQLを流用したい
- ✅ パフォーマンスを細かくチューニングしたい
- ✅ 動的なSQLを構築したい
- ✅ レガシーDBとの連携

### JPAが適している場面

- ✅ シンプルなCRUD操作
- ✅ エンティティ間のリレーションシップが複雑
- ✅ トランザクション管理を簡潔にしたい
- ✅ キャッシュ機能を活用したい

---

## 🚀 ステップ1: MyBatisの依存関係追加

### 1-1. pom.xmlに依存関係を追加

`pom.xml`に以下を追加します：

```xml
<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- MyBatis Spring Boot Starter -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.3</version>
</dependency>

<!-- MySQL Connector -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 1-2. 依存関係の解説

#### `mybatis-spring-boot-starter`

MyBatisをSpring Bootで簡単に使えるようにするスターターです。

**含まれるもの**:
- MyBatisコア
- MyBatis-Spring連携
- 自動設定

**バージョン指定**:
Spring Boot 3.5.8では、MyBatis 3.0.3を使用します。

---

## 🚀 ステップ2: application.yamlの設定

### 2-1. MyBatis設定を追加

`src/main/resources/application.yaml`に以下を追加します：

```yaml
spring:
  application:
    name: hello-spring-boot

  datasource:
    url: jdbc:mysql://localhost:3306/hello_spring_boot?useSSL=false&serverTimezone=Asia/Tokyo&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
    username: springuser
    password: springpass
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect

# MyBatis設定
mybatis:
  # Mapperインターフェースの場所
  mapper-locations: classpath:mapper/**/*.xml
  # 型エイリアスのパッケージ
  type-aliases-package: com.example.hellospringboot
  configuration:
    # キャメルケース ↔ スネークケース自動変換
    map-underscore-to-camel-case: true
    # ログ出力
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

server:
  port: 8080

app:
  name: Hello Spring Boot Application
  version: 1.0.0
  welcome-message: Welcome to Spring Boot Configuration Management!
  description: This is a demo application for learning Spring Boot configuration.
```

### 2-2. 設定の解説

#### `mapper-locations`

XMLマッパーファイルの配置場所を指定します。

- `classpath:mapper/**/*.xml`: `src/main/resources/mapper/`以下の全XMLファイル
- ワイルドカード`**`でサブディレクトリも対象

#### `type-aliases-package`

Javaクラスの型エイリアスを自動設定します。

**例**:
- `com.example.hellospringboot.User` → `User`
- XML内で完全修飾名を書かずに済む

#### `map-underscore-to-camel-case`

データベースのスネークケース（`user_name`）とJavaのキャメルケース（`userName`）を自動変換します。

| DB | Java |
|---|---|
| `user_name` | `userName` |
| `created_at` | `createdAt` |
| `category_id` | `categoryId` |

---

## 🚀 ステップ3: Orderエンティティの作成

### 3-1. Orderエンティティ

MyBatis用の`Order`クラスを作成します（JPAアノテーションは不要）。

**ファイルパス**: `src/main/java/com/example/hellospringboot/mybatis/Order.java`

```java
package com.example.hellospringboot.mybatis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    private Long id;
    private Long userId;
    private BigDecimal totalAmount;
    private String status;  // PENDING, PAID, SHIPPED, DELIVERED, CANCELLED
    private LocalDateTime orderDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 検索結果用の追加フィールド（JOINで取得）
    private String userName;
    private String userEmail;
}
```

### 3-2. JPAエンティティとの違い

| JPA Entity | MyBatis Model |
|---|---|
| `@Entity` | 不要 |
| `@Table` | 不要 |
| `@Id`, `@GeneratedValue` | 不要 |
| `@Column` | 不要 |
| `@PrePersist`, `@PreUpdate` | 不要 |

**理由**:
MyBatisはSQLを直接記述するため、ORマッピングのアノテーションは不要です。

---

## 🚀 ステップ4: OrderMapperインターフェースの作成

### 4-1. Mapperインターフェース

**ファイルパス**: `src/main/java/com/example/hellospringboot/mybatis/OrderMapper.java`

```java
package com.example.hellospringboot.mybatis;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderMapper {
    
    // アノテーションベースのクエリ
    @Select("SELECT * FROM orders WHERE id = #{id}")
    Order findById(Long id);
    
    @Select("SELECT * FROM orders ORDER BY order_date DESC")
    List<Order> findAll();
    
    @Insert("INSERT INTO orders (user_id, total_amount, status, order_date, created_at, updated_at) " +
            "VALUES (#{userId}, #{totalAmount}, #{status}, #{orderDate}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Order order);
    
    @Update("UPDATE orders SET user_id = #{userId}, total_amount = #{totalAmount}, " +
            "status = #{status}, order_date = #{orderDate}, updated_at = NOW() WHERE id = #{id}")
    void update(Order order);
    
    @Delete("DELETE FROM orders WHERE id = #{id}")
    void delete(Long id);
    
    // XMLマッパーで定義（次のステップ）
    List<Order> findByUserId(Long userId);
    
    List<Order> findByStatus(String status);
}
```

### 4-2. コードの解説

#### `@Mapper`

MyBatisのMapperインターフェースであることを示します。

**Spring Bootの自動設定**:
- `@Mapper`を付けると、Spring Bootが自動的にBeanとして登録
- `@MapperScan`でパッケージをスキャンさせることも可能

#### `@Select`, `@Insert`, `@Update`, `@Delete`

SQLを直接アノテーションに記述します。

**パラメータバインディング**:
- `#{id}`: プリペアドステートメントのパラメータ
- メソッド引数の名前と一致させる

#### `@Options(useGeneratedKeys = true, keyProperty = "id")`

INSERT時に自動生成されたIDを取得します。

- `useGeneratedKeys = true`: 自動生成キーを使用
- `keyProperty = "id"`: `User`オブジェクトの`id`フィールドにセット

---

## 🚀 ステップ5: XMLマッパーファイルの作成

### 5-1. ディレクトリ構成

```
src/main/resources/
└── mapper/
    └── UserMapper.xml
```

### 5-2. UserMapper.xmlの作成

**ファイルパス**: `src/main/resources/mapper/OrderMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.example.hellospringboot.mybatis.OrderMapper">

    <!-- ResultMap定義 -->
    <resultMap id="OrderResultMap" type="Order">
        <id property="id" column="id"/>
        <result property="userId" column="user_id"/>
        <result property="totalAmount" column="total_amount"/>
        <result property="status" column="status"/>
        <result property="orderDate" column="order_date"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
    </resultMap>

    <!-- ユーザーIDで検索 -->
    <select id="findByUserId" resultMap="OrderResultMap">
        SELECT * FROM orders
        WHERE user_id = #{userId}
        ORDER BY order_date DESC
    </select>

    <!-- ステータスで検索 -->
    <select id="findByStatus" resultMap="OrderResultMap">
        SELECT * FROM orders
        WHERE status = #{status}
        ORDER BY order_date DESC
    </select>

</mapper>
```

### 5-3. XMLマッパーの解説

#### `<mapper namespace="...">`

Mapperインターフェースの完全修飾名を指定します。

#### `<resultMap>`

DBカラムとJavaオブジェクトのプロパティをマッピングします。

```xml
<resultMap id="OrderResultMap" type="Order">
    <id property="id" column="id"/>                     <!-- 主キー -->
    <result property="userId" column="user_id"/>        <!-- 通常カラム -->
    <result property="totalAmount" column="total_amount"/>
</resultMap>
```

**`map-underscore-to-camel-case`が有効なら省略可能**:
- `user_id` → `userId` は自動変換される
- `total_amount` → `totalAmount` は自動変換される
- `created_at` → `createdAt` は自動変換される

#### `<select id="findByUserId">`

- `id`: Mapperインターフェースのメソッド名
- `resultMap`: 使用するResultMap
- `#{userId}`: パラメータバインディング

#### XML内でのエスケープ

| 記号 | XML表記 |
|---|---|
| `<` | `&lt;` |
| `>` | `&gt;` |
| `&` | `&amp;` |

**例**:
```xml
WHERE age &gt; #{age}  <!-- age > #{age} -->
```

---

## 🚀 ステップ6: OrderServiceとControllerの作成

### 6-1. OrderService

**ファイルパス**: `src/main/java/com/example/hellospringboot/mybatis/OrderService.java`

```java
package com.example.hellospringboot.mybatis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final OrderMapper orderMapper;
    
    @Transactional(readOnly = true)
    public Order findById(Long id) {
        log.info("Finding order by id: {}", id);
        return orderMapper.findById(id);
    }
    
    @Transactional(readOnly = true)
    public List<Order> findAll() {
        log.info("Finding all orders");
        return orderMapper.findAll();
    }
    
    @Transactional
    public Order create(Order order) {
        log.info("Creating order: {}", order);
        orderMapper.insert(order);
        return order;  // IDが自動セットされている
    }
    
    @Transactional
    public Order update(Long id, Order orderDetails) {
        log.info("Updating order id: {}", id);
        Order order = orderMapper.findById(id);
        if (order == null) {
            throw new RuntimeException("Order not found with id: " + id);
        }
        
        order.setUserId(orderDetails.getUserId());
        order.setTotalAmount(orderDetails.getTotalAmount());
        order.setStatus(orderDetails.getStatus());
        order.setOrderDate(orderDetails.getOrderDate());
        
        orderMapper.update(order);
        return order;
    }
    
    @Transactional
    public void delete(Long id) {
        log.info("Deleting order id: {}", id);
        Order order = orderMapper.findById(id);
        if (order == null) {
            throw new RuntimeException("Order not found with id: " + id);
        }
        orderMapper.delete(id);
    }
    
    @Transactional(readOnly = true)
    public List<Order> findByUserId(Long userId) {
        log.info("Finding orders by userId: {}", userId);
        return orderMapper.findByUserId(userId);
    }
    
    @Transactional(readOnly = true)
    public List<Order> findByStatus(String status) {
        log.info("Finding orders by status: {}", status);
        return orderMapper.findByStatus(status);
    }
}
```

### 6-2. OrderController

**ファイルパス**: `src/main/java/com/example/hellospringboot/mybatis/OrderController.java`

```java
package com.example.hellospringboot.mybatis;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    
    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.findAll();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        Order order = orderService.findById(id);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(order);
    }
    
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        Order created = orderService.create(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(
            @PathVariable Long id,
            @RequestBody Order orderDetails) {
        try {
            Order updated = orderService.update(id, orderDetails);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        try {
            orderService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/user/{userId}")
    public List<Order> getOrdersByUserId(@PathVariable Long userId) {
        return orderService.findByUserId(userId);
    }
    
    @GetMapping("/status/{status}")
    public List<Order> getOrdersByStatus(@PathVariable String status) {
        return orderService.findByStatus(status);
    }
}
```

---

## ✅ ステップ7: 動作確認

### 7-1. ordersテーブルを作成

```bash
docker exec -it hello-spring-boot-mysql mysql -uroot -prootpassword -e "
USE hello_spring_boot;
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    order_date DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
"
```

### 7-2. アプリケーションを再起動

```bash
./mvnw spring-boot:run
```

### 7-3. 注文を作成

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "totalAmount": 15000.00,
    "status": "PENDING",
    "orderDate": "2025-12-13T10:00:00"
  }'
```

**期待される結果**:

```json
{
  "id": 1,
  "userId": 1,
  "totalAmount": 15000.00,
  "status": "PENDING",
  "orderDate": "2025-12-13T10:00:00",
  "createdAt": "2025-12-13T17:16:33",
  "updatedAt": "2025-12-13T17:16:33"
}
```

### 7-4. 全注文を取得

```bash
curl http://localhost:8080/api/orders
```

### 7-5. ユーザーIDで検索

```bash
curl "http://localhost:8080/api/orders/user/1"
```

### 7-6. ステータスで検索

```bash
curl "http://localhost:8080/api/orders/status/PENDING"
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: 動的SQLで複数条件検索

ユーザーID、ステータス、金額範囲で検索できるメソッドを実装してください（Step 13で詳しく学びます）。

**ヒント**: `<where>`と`<if>`を使います。

```xml
<select id="searchOrders" resultMap="OrderResultMap">
    SELECT * FROM orders
    <where>
        <if test="userId != null">
            AND user_id = #{userId}
        </if>
        <if test="status != null and status != ''">
            AND status = #{status}
        </if>
        <if test="minAmount != null">
            AND total_amount &gt;= #{minAmount}
        </if>
        <if test="maxAmount != null">
            AND total_amount &lt;= #{maxAmount}
        </if>
    </where>
</select>
```

### チャレンジ 2: 一括INSERT

複数の注文を一度に登録するメソッドを実装してください。

**ヒント**: `<foreach>`を使います。

```xml
<insert id="insertBatch">
    INSERT INTO orders (user_id, total_amount, status, order_date, created_at, updated_at)
    VALUES
    <foreach collection="orders" item="order" separator=",">
        (#{order.userId}, #{order.totalAmount}, #{order.status}, #{order.orderDate}, NOW(), NOW())
    </foreach>
</insert>
```

### チャレンジ 3: ページネーション

`LIMIT`と`OFFSET`を使ってページネーションを実装してください。

```java
@Select("SELECT * FROM orders ORDER BY order_date DESC LIMIT #{limit} OFFSET #{offset}")
List<Order> findWithPagination(@Param("limit") int limit, @Param("offset") int offset);
```

---

## 🐛 トラブルシューティング

### エラー: "Invalid bound statement (not found)"

**原因**: XMLマッパーが見つからない、またはnamespaceが間違っている

**解決策**:

1. `application.yaml`の`mapper-locations`を確認
2. XMLの`namespace`がMapperインターフェースの完全修飾名と一致しているか確認
3. XMLの`<select id="..."/>`のidがメソッド名と一致しているか確認

```yaml
mybatis:
  mapper-locations: classpath:mapper/**/*.xml  # 正しいパス
```

### エラー: "Parameter 'XXX' not found"

**原因**: 複数パラメータの場合、`@Param`アノテーションが必要

**解決策**:

```java
// ❌ 悪い例
List<Order> searchByUserIdAndStatus(Long userId, String status);

// ✅ 良い例
List<Order> searchByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);
```

### エラー: "There is no getter for property named 'XXX'"

**原因**: ResultMapのプロパティ名がクラスのフィールド名と一致していない

**解決策**:

```xml
<!-- ❌ 悪い例 -->
<result property="amount" column="total_amount"/>  <!-- Orderクラスにamountフィールドが存在しない -->

<!-- ✅ 良い例 -->
<result property="totalAmount" column="total_amount"/>
```

### SQLがログに出ない

**原因**: ログレベルが設定されていない

**解決策**: `application.yaml`に追加

```yaml
logging:
  level:
    com.example.hellospringboot.mybatis: DEBUG
```

### エラー: "Foreign key constraint fails"

**原因**: 存在しないユーザーIDで注文を作成しようとしている

**解決策**: 先にユーザーを作成してから注文を作成する

```bash
# まずユーザーを作成（JPA）
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "田中太郎", "email": "tanaka@example.com", "age": 30}'

# 次に注文を作成（MyBatis）
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "totalAmount": 15000.00, "status": "PENDING", "orderDate": "2025-12-13T10:00:00"}'
```

---

## 📚 このステップで学んだこと

- ✅ MyBatisはSQLを直接記述できる永続化フレームワークである
- ✅ JPAとMyBatisは用途によって使い分ける
- ✅ `@Mapper`でMapperインターフェースを定義できる
- ✅ `@Select`, `@Insert`, `@Update`, `@Delete`でアノテーションベースのクエリを書ける
- ✅ XMLマッパーで複雑なクエリを記述できる
- ✅ `<resultMap>`でDBカラムとJavaオブジェクトをマッピングできる
- ✅ `map-underscore-to-camel-case`でスネークケースとキャメルケースを自動変換できる
- ✅ MyBatisでも`@Transactional`によるトランザクション管理が可能

---

## 💡 補足: MyBatisとJPAの共存

### 同じプロジェクトで併用可能

Spring BootではMyBatisとJPAを同時に使用できます。

**このプロジェクトでの実装例**:
- **JPA**: Product, User, Post（Phase 2で実装）
- **MyBatis**: Order（Phase 3で実装）

### トランザクション管理

MyBatisとJPAは同じSpringのトランザクション管理を使用します。

```java
@Service
public class SomeService {
    private final ProductRepository productRepository;  // JPA
    private final UserRepository userRepository;        // JPA
    private final OrderMapper orderMapper;              // MyBatis
    
    @Transactional  // 両方を同じトランザクションで管理
    public void createOrderWithProduct(Long userId, Long productId, BigDecimal amount) {
        // JPAでユーザーと商品を取得
        User user = userRepository.findById(userId).orElseThrow();
        Product product = productRepository.findById(productId).orElseThrow();
        
        // MyBatisで注文を作成
        Order order = new Order();
        order.setUserId(userId);
        order.setTotalAmount(amount);
        order.setStatus("PENDING");
        order.setOrderDate(LocalDateTime.now());
        orderMapper.insert(order);
    }
}
```

**利点**:
- シンプルなCRUDはJPAで簡潔に記述
- 複雑な検索・集計はMyBatisで柔軟に実装
- 両方を同じトランザクション内で安全に使用可能

---

## ➡️ 次のステップ

[Step 13: MyBatisで複雑なクエリ](STEP_13.md)へ進みましょう！

次のステップでは、動的SQL、JOINクエリ、集計関数など、MyBatisの強力な機能を学びます。
