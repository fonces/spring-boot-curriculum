# Step 14: JPAとMyBatisの使い分け

## 🎯 このステップの目標

- JPAとMyBatisそれぞれの長所と短所を理解できる
- どのような場面でどちらを使うべきか判断できる
- 両方を組み合わせたハイブリッドアーキテクチャを設計できる
- 実務での選択基準とベストプラクティスを習得できる
- パフォーマンス、保守性、開発効率のバランスを考慮できる

**所要時間**: 約40分

---

## 📋 事前準備

- [Step 7-11: Spring Data JPAでCRUD](../phase2/STEP_7.md)が完了していること
- [Step 12: MyBatisの基礎](STEP_12.md)が完了していること
- [Step 13: MyBatisで複雑なクエリ](STEP_13.md)が完了していること
- JPAとMyBatisの両方の実装経験があること

---

## 🔍 JPAとMyBatisの比較

### 概要

| 観点 | JPA/Hibernate | MyBatis |
|------|--------------|---------|
| **アプローチ** | オブジェクト指向（ORM） | SQL中心 |
| **抽象度** | 高い（SQLを意識しない） | 低い（SQLを直接制御） |
| **学習コスト** | 高い（エンティティ、関係、キャッシュ） | 低い（SQLの知識があれば） |
| **ボイラープレート** | 少ない | 中程度（XMLまたはアノテーション） |
| **複雑なクエリ** | 難しい（JPQLやCriteria API） | 簡単（生SQLを書ける） |
| **パフォーマンス** | 自動最適化（遅延ロード、キャッシュ） | 手動最適化（完全制御） |
| **保守性** | 高い（ドメインロジックに集中） | 中（SQLのメンテナンスが必要） |

---

## 🚀 ステップ1: それぞれの得意分野を理解する

### 1-1. JPAが得意な場面

#### ✅ シーン1: シンプルなCRUD操作

**例**: ユーザー管理、商品マスタなど

```java
// JPA: インターフェースだけで完結
public interface UserRepository extends JpaRepository<User, Long> {
}

// 使用例
userRepository.save(user);  // 作成/更新
userRepository.findById(1L);  // 検索
userRepository.deleteById(1L);  // 削除
```

**メリット**:
- コード量が極端に少ない
- SQLを書く必要がない
- トランザクション、キャッシュが自動管理

**MyBatisで同じことをするには**:
```java
// 複数のSQLを手動で定義
@Insert("INSERT INTO users ...")
@Update("UPDATE users ...")
@Delete("DELETE FROM users ...")
@Select("SELECT * FROM users WHERE id = #{id}")
```

#### ✅ シーン2: エンティティ間のリレーション

**例**: 1対多、多対多の関係

```java
@Entity
public class Category {
    @Id
    private Long id;
    
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Product> products;
}

@Entity
public class Product {
    @Id
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
}
```

**メリット**:
- オブジェクトグラフとして自然に扱える
- 遅延ロード（Lazy Loading）で必要な時だけデータ取得
- 双方向の関係を簡単に定義

**MyBatisの場合**:
```xml
<!-- 手動でJOINクエリを書く必要がある -->
<resultMap id="CategoryWithProducts" type="Category">
    <collection property="products" ofType="Product">
        <!-- 詳細なマッピング定義 -->
    </collection>
</resultMap>
```

#### ✅ シーン3: ドメイン駆動設計（DDD）

**例**: ビジネスロジックをエンティティに集約

```java
@Entity
public class Order {
    @Id
    private Long id;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    private LocalDateTime orderDate;
    
    // ビジネスロジック
    public void ship() {
        if (status != OrderStatus.PAID) {
            throw new IllegalStateException("未払いの注文は発送できません");
        }
        this.status = OrderStatus.SHIPPED;
    }
    
    public boolean canCancel() {
        return status == OrderStatus.PENDING || status == OrderStatus.PAID;
    }
}
```

**メリット**:
- エンティティ = ドメインモデル
- ビジネスルールをオブジェクトにカプセル化
- テストが容易（モックDB不要）

### 1-2. MyBatisが得意な場面

#### ✅ シーン1: 複雑な集計・レポート

**例**: 売上レポート、ダッシュボード

```xml
<select id="getSalesReport" resultType="SalesReport">
    SELECT 
        DATE(o.order_date) AS date,
        c.name AS category_name,
        COUNT(DISTINCT o.id) AS order_count,
        SUM(oi.quantity * oi.price) AS total_sales,
        AVG(oi.quantity * oi.price) AS avg_order_value
    FROM orders o
    INNER JOIN order_items oi ON o.id = oi.order_id
    INNER JOIN products p ON oi.product_id = p.id
    INNER JOIN categories c ON p.category_id = c.id
    WHERE o.order_date BETWEEN #{startDate} AND #{endDate}
    GROUP BY DATE(o.order_date), c.name
    HAVING total_sales > #{minSales}
    ORDER BY date DESC, total_sales DESC
</select>
```

**メリット**:
- SQLの全機能を使える（GROUP BY, HAVING, サブクエリ）
- データベース固有の関数が使える（DATE, CONCAT, IFNULLなど）
- パフォーマンスチューニングが容易

**JPAで同じことをするには**:
```java
// JPQL: 複雑で可読性が低い
@Query("SELECT new SalesReport(DATE(o.orderDate), c.name, COUNT(DISTINCT o.id), " +
       "SUM(oi.quantity * oi.price), AVG(oi.quantity * oi.price)) " +
       "FROM Order o JOIN o.orderItems oi JOIN oi.product p JOIN p.category c " +
       "WHERE o.orderDate BETWEEN :start AND :end " +
       "GROUP BY DATE(o.orderDate), c.name " +
       "HAVING SUM(oi.quantity * oi.price) > :minSales")
```

#### ✅ シーン2: 動的SQL（検索条件が可変）

**例**: 高度な検索フォーム

```xml
<select id="searchProducts" resultType="Product">
    SELECT * FROM products
    <where>
        <if test="keyword != null">
            AND (name LIKE CONCAT('%', #{keyword}, '%')
                 OR description LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="categoryId != null">
            AND category_id = #{categoryId}
        </if>
        <if test="minPrice != null">
            AND price &gt;= #{minPrice}
        </if>
        <if test="maxPrice != null">
            AND price &lt;= #{maxPrice}
        </if>
        <if test="inStock != null and inStock">
            AND stock > 0
        </if>
    </where>
    ORDER BY
    <choose>
        <when test="sortBy == 'price'">price</when>
        <when test="sortBy == 'name'">name</when>
        <otherwise>id</otherwise>
    </choose>
    <if test="sortOrder == 'DESC'">DESC</if>
</select>
```

**メリット**:
- 条件の有無で自動的にSQLが変わる
- 複雑な分岐ロジックが読みやすい

**JPAの場合**:
```java
// Criteria API: 冗長で可読性が低い
CriteriaBuilder cb = em.getCriteriaBuilder();
CriteriaQuery<Product> query = cb.createQuery(Product.class);
Root<Product> root = query.from(Product.class);
List<Predicate> predicates = new ArrayList<>();

if (keyword != null) {
    Predicate nameLike = cb.like(root.get("name"), "%" + keyword + "%");
    Predicate descLike = cb.like(root.get("description"), "%" + keyword + "%");
    predicates.add(cb.or(nameLike, descLike));
}
// ... さらに続く
```

#### ✅ シーン3: レガシーDB、非標準スキーマ

**例**: 既存システムのデータベース

```xml
<!-- テーブル名や列名がJava命名規則と異なる -->
<select id="findLegacyUser" resultMap="LegacyUserMap">
    SELECT 
        USR_ID,
        USR_NM,
        USR_EMAIL_ADDR,
        CRT_DT,
        UPD_DT
    FROM TBL_USR_MST
    WHERE USR_ID = #{userId}
</select>

<resultMap id="LegacyUserMap" type="User">
    <id property="id" column="USR_ID"/>
    <result property="name" column="USR_NM"/>
    <result property="email" column="USR_EMAIL_ADDR"/>
    <result property="createdAt" column="CRT_DT"/>
    <result property="updatedAt" column="UPD_DT"/>
</resultMap>
```

**メリット**:
- どんなテーブル/カラム名でも対応可能
- 既存DBに手を加えずにマッピング
- ストアドプロシージャも呼び出せる

---

## 🚀 ステップ2: 実務での選択基準

### 2-1. プロジェクト特性による選択

| プロジェクト種類 | 推奨 | 理由 |
|----------------|------|------|
| **新規開発（スタートアップ）** | JPA | 開発速度優先、スキーマ変更が多い |
| **エンタープライズ（既存DB）** | MyBatis | 既存スキーマに合わせる必要がある |
| **データ分析/レポート** | MyBatis | 複雑な集計クエリが多い |
| **CRUD中心のアプリ** | JPA | シンプルな操作が大半 |
| **高トラフィック/高負荷** | MyBatis | SQLチューニングが必要 |
| **マイクロサービス** | JPA | 小さいドメインごとに独立 |

### 2-2. チームスキルによる選択

| チームの状況 | 推奨 | 理由 |
|-------------|------|------|
| **Java経験豊富、SQL苦手** | JPA | オブジェクト指向で開発できる |
| **DB/SQL経験豊富** | MyBatis | SQLスキルを活かせる |
| **初学者中心** | JPA | 自動生成が多く学習コストが低い |
| **保守性重視** | JPA | ビジネスロジックに集中できる |
| **パフォーマンス重視** | MyBatis | SQLを完全制御できる |

### 2-3. ハイブリッドアプローチ（推奨⭐）

**ベストプラクティス**: 両方を組み合わせる

```
┌─────────────────────────────────────┐
│         アプリケーション層           │
├─────────────────────────────────────┤
│  CRUD操作      │  複雑なクエリ      │
│  ↓             │  ↓                │
│  JPA           │  MyBatis          │
│  Repository    │  Mapper           │
├─────────────────────────────────────┤
│         データベース層               │
└─────────────────────────────────────┘
```

**例**:
- **ユーザー管理**: JPA（シンプルなCRUD）
- **売上レポート**: MyBatis（複雑な集計）
- **商品マスタ**: JPA（リレーション）
- **検索機能**: MyBatis（動的SQL）

---

## 🚀 ステップ3: ハイブリッド実装例

### 3-1. 実装シナリオ

現在のプロジェクト構成:
- **Product/Category**: JPA（Phase 2で実装済み）
- **User/Post**: MyBatis（Phase 3で実装済み）

新しい要件: **ユーザーの購入履歴レポート**

### 3-2. Orderエンティティ（JPA）を作成

シンプルなCRUD操作はJPAで実装します。

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
    private Long userId;  // MyBatisのUserを参照（JPAの関連は作らない）
    
    @Column(nullable = false)
    private Long productId;  // JPAのProductを参照
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false)
    private Integer price;
    
    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (orderDate == null) {
            orderDate = LocalDateTime.now();
        }
    }
}
```

**ポイント**:
- `userId`は`Long`型のみ（MyBatisのUserへの関連は作らない）
- `productId`も`Long`型（JPAとMyBatisを混在させない）

### 3-3. OrderRepository（JPA）を作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/OrderRepository.java`

```java
package com.example.hellospringboot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    
    List<Order> findByUserId(Long userId);
    
    List<Order> findByOrderDateBetween(LocalDateTime start, LocalDateTime end);
}
```

### 3-4. PurchaseReportDTO（MyBatis用）を作成

複雑なレポートクエリ用のDTO:

**ファイルパス**: `src/main/java/com/example/hellospringboot/PurchaseReport.java`

```java
package com.example.hellospringboot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseReport {
    
    private Long userId;
    private String userName;
    private String userEmail;
    
    private Long totalOrders;
    private Long totalQuantity;
    private Long totalAmount;
    private Double averageOrderValue;
    
    private String favoriteProductName;
    private Long favoriteProductCount;
}
```

### 3-5. ReportMapper（MyBatis）を作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/ReportMapper.java`

```java
package com.example.hellospringboot;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReportMapper {
    
    List<PurchaseReport> getPurchaseReportByPeriod(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
```

### 3-6. ReportMapper.xml（MyBatis）を作成

**ファイルパス**: `src/main/resources/mapper/ReportMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.example.hellospringboot.ReportMapper">

    <resultMap id="PurchaseReportResultMap" type="PurchaseReport">
        <result property="userId" column="user_id"/>
        <result property="userName" column="user_name"/>
        <result property="userEmail" column="user_email"/>
        <result property="totalOrders" column="total_orders"/>
        <result property="totalQuantity" column="total_quantity"/>
        <result property="totalAmount" column="total_amount"/>
        <result property="averageOrderValue" column="average_order_value"/>
        <result property="favoriteProductName" column="favorite_product_name"/>
        <result property="favoriteProductCount" column="favorite_product_count"/>
    </resultMap>

    <select id="getPurchaseReportByPeriod" resultMap="PurchaseReportResultMap">
        SELECT 
            u.id AS user_id,
            u.name AS user_name,
            u.email AS user_email,
            COUNT(DISTINCT o.id) AS total_orders,
            SUM(o.quantity) AS total_quantity,
            SUM(o.quantity * o.price) AS total_amount,
            AVG(o.quantity * o.price) AS average_order_value,
            (
                SELECT p.name
                FROM orders o2
                INNER JOIN products p ON o2.product_id = p.id
                WHERE o2.user_id = u.id
                  AND o2.order_date BETWEEN #{startDate} AND #{endDate}
                GROUP BY p.id, p.name
                ORDER BY COUNT(*) DESC
                LIMIT 1
            ) AS favorite_product_name,
            (
                SELECT COUNT(*)
                FROM orders o3
                WHERE o3.user_id = u.id
                  AND o3.product_id = (
                      SELECT product_id
                      FROM orders o4
                      WHERE o4.user_id = u.id
                        AND o4.order_date BETWEEN #{startDate} AND #{endDate}
                      GROUP BY product_id
                      ORDER BY COUNT(*) DESC
                      LIMIT 1
                  )
                  AND o3.order_date BETWEEN #{startDate} AND #{endDate}
            ) AS favorite_product_count
        FROM users u
        INNER JOIN orders o ON u.id = o.user_id
        WHERE o.order_date BETWEEN #{startDate} AND #{endDate}
        GROUP BY u.id, u.name, u.email
        ORDER BY total_amount DESC
    </select>

</mapper>
```

**解説**:
- メインクエリ: ユーザーごとの購入統計
- サブクエリ1: 最も購入した商品名
- サブクエリ2: その商品の購入回数
- JPA（`products`）とMyBatis（`users`, `orders`）の両方のテーブルをJOIN

### 3-7. ReportService（ハイブリッド）を作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/ReportService.java`

```java
package com.example.hellospringboot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {
    
    private final OrderRepository orderRepository;  // JPA
    private final ReportMapper reportMapper;  // MyBatis
    
    /**
     * シンプルな操作はJPAを使用
     */
    public List<Order> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId);
    }
    
    /**
     * 複雑な集計はMyBatisを使用
     */
    public List<PurchaseReport> getPurchaseReport(LocalDateTime startDate, LocalDateTime endDate) {
        return reportMapper.getPurchaseReportByPeriod(startDate, endDate);
    }
}
```

**ポイント**:
- 同じServiceクラスでJPAとMyBatisを併用
- 用途に応じて使い分ける

### 3-8. ReportControllerを作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/ReportController.java`

```java
package com.example.hellospringboot;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    
    private final ReportService reportService;
    
    /**
     * 特定ユーザーの注文履歴（JPA）
     */
    @GetMapping("/orders/user/{userId}")
    public List<Order> getUserOrders(@PathVariable Long userId) {
        return reportService.getOrdersByUser(userId);
    }
    
    /**
     * 期間別購入レポート（MyBatis）
     */
    @GetMapping("/purchases")
    public List<PurchaseReport> getPurchaseReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return reportService.getPurchaseReport(startDate, endDate);
    }
}
```

---

## ✅ 動作確認

### 1. ordersテーブルを作成

```bash
docker compose exec mysql mysql -u springuser -pspringpass spring_boot_db
```

```sql
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    price INT NOT NULL,
    order_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- サンプルデータ
INSERT INTO orders (user_id, product_id, quantity, price, order_date) VALUES
(1, 1, 2, 29800, '2025-12-01 10:00:00'),
(1, 1, 1, 29800, '2025-12-05 14:30:00'),
(1, 2, 3, 1980, '2025-12-10 09:15:00'),
(2, 2, 1, 1980, '2025-12-03 11:20:00'),
(2, 3, 2, 15000, '2025-12-08 16:45:00');
```

### 2. アプリケーションを起動

```bash
./mvnw spring-boot:run
```

### 3. ユーザーの注文履歴を取得（JPA）

```bash
curl "http://localhost:8080/api/reports/orders/user/1"
```

**期待される結果**:
```json
[
  {
    "id": 1,
    "userId": 1,
    "productId": 1,
    "quantity": 2,
    "price": 29800,
    "orderDate": "2025-12-01T10:00:00",
    "createdAt": "..."
  },
  ...
]
```

### 4. 購入レポートを取得（MyBatis）

```bash
curl "http://localhost:8080/api/reports/purchases?startDate=2025-12-01T00:00:00&endDate=2025-12-31T23:59:59"
```

**期待される結果**:
```json
[
  {
    "userId": 1,
    "userName": "田中太郎",
    "userEmail": "tanaka.updated@example.com",
    "totalOrders": 3,
    "totalQuantity": 6,
    "totalAmount": 65540,
    "averageOrderValue": 21846.67,
    "favoriteProductName": "ワイヤレスキーボード",
    "favoriteProductCount": 2
  },
  {
    "userId": 2,
    "userName": "佐藤花子",
    "userEmail": "sato@example.com",
    "totalOrders": 2,
    "totalQuantity": 3,
    "totalAmount": 31980,
    "averageOrderValue": 15990.0,
    "favoriteProductName": "マウス",
    "favoriteProductCount": 1
  }
]
```

---

## 💡 補足: 使い分けのチートシート

### JPA推奨

✅ **基本的なCRUD操作**
```java
repository.save(entity);
repository.findById(id);
repository.deleteById(id);
```

✅ **エンティティ間のリレーション**
```java
@OneToMany, @ManyToOne, @ManyToMany
```

✅ **ドメインロジック中心**
```java
entity.calculateTotal();
entity.isValid();
```

✅ **トランザクション境界が明確**
```java
@Transactional
public void createOrder(Order order) { ... }
```

### MyBatis推奨

✅ **複雑な集計・レポート**
```xml
SELECT ..., COUNT(...), SUM(...), AVG(...)
GROUP BY ... HAVING ...
```

✅ **動的な検索条件**
```xml
<where>
    <if test="name != null">...</if>
</where>
```

✅ **既存DB、非標準スキーマ**
```xml
<resultMap> で柔軟にマッピング
```

✅ **SQLチューニングが必要**
```xml
EXPLAIN で実行計画を確認しながらSQL最適化
```

---

## 🎨 チャレンジ課題

### チャレンジ1: 検索機能をJPAからMyBatisに移行

現在JPAで実装されている`ProductRepository.findByNameContaining()`を、MyBatisの動的SQLで再実装してください。

**ヒント**:
```xml
<select id="searchProducts">
    SELECT * FROM products
    <where>
        <if test="keyword != null">
            name LIKE CONCAT('%', #{keyword}, '%')
        </if>
    </where>
</select>
```

### チャレンジ2: N+1問題の解決

JPAで`Category`と`Product`を取得する際のN+1問題を、`@EntityGraph`またはMyBatisのJOINで解決してください。

**ヒント（JPA）**:
```java
@EntityGraph(attributePaths = {"products"})
List<Category> findAll();
```

**ヒント（MyBatis）**:
```xml
<resultMap id="CategoryWithProducts">
    <collection property="products" ofType="Product">
        ...
    </collection>
</resultMap>
```

### チャレンジ3: ハイブリッドサービスの設計

新しい機能「在庫管理」を追加する際、どの部分をJPAで、どの部分をMyBatisで実装すべきか設計してください。

**考慮点**:
- CRUD操作（在庫の追加/更新/削除）
- 在庫アラート（在庫が少ない商品一覧）
- 在庫推移レポート（月別の入出庫履歴）

---

## 🐛 トラブルシューティング

### エラー1: "Duplicate key 'id' in result map"

**原因**: JPAとMyBatisで同じテーブルを異なるマッピングで参照している

**解決策**:
- 同じテーブルは一方のフレームワークで統一
- または、明確に役割を分ける（CRUDはJPA、レポートはMyBatis）

### エラー2: トランザクション境界が不明確

**原因**: JPAとMyBatisを同じトランザクション内で混在

**解決策**:
```java
@Transactional
public void mixedOperation() {
    // JPA操作
    orderRepository.save(order);
    
    // MyBatis操作（同じトランザクション内）
    reportMapper.updateStatistics();
}
```

両方とも同じ`DataSource`を使っていれば、`@Transactional`で一貫性を保てます。

### エラー3: 遅延ロード（LazyInitializationException）

**原因**: JPAのLazy Fetchingがトランザクション外で実行された

**解決策**:
```java
@Transactional(readOnly = true)
public Category getCategoryWithProducts(Long id) {
    Category category = categoryRepository.findById(id).orElseThrow();
    category.getProducts().size();  // トランザクション内で強制ロード
    return category;
}
```

または、MyBatisで明示的にJOIN:
```xml
<select id="findCategoryWithProducts">
    SELECT c.*, p.*
    FROM categories c
    LEFT JOIN products p ON c.id = p.category_id
    WHERE c.id = #{id}
</select>
```

---

## 📚 このステップで学んだこと

- ✅ **JPAの強み**: CRUD、リレーション、ドメイン駆動設計
- ✅ **MyBatisの強み**: 複雑なクエリ、動的SQL、レガシーDB対応
- ✅ **選択基準**: プロジェクト特性、チームスキル、用途別の使い分け
- ✅ **ハイブリッドアプローチ**: 両方を組み合わせて最適化
- ✅ **実装パターン**: 同じServiceでJPAとMyBatisを併用
- ✅ **トランザクション管理**: 同じDataSourceなら一貫性を保てる

---

## ➡️ 次のステップ

Phase 3（MyBatis）が完了しました！お疲れさまでした🎉

次は**Phase 4: アーキテクチャとベストプラクティス**に進みましょう：

- [Step 15: レイヤー化アーキテクチャ](../phase4/STEP_15.md)
- [Step 16: DI/IoCコンテナの深掘り](../phase4/STEP_16.md)
- [Step 17: 例外ハンドリング](../phase4/STEP_17.md)

これまでに学んだJPAとMyBatisの知識を活かして、より実践的なアプリケーション設計を学びます！
