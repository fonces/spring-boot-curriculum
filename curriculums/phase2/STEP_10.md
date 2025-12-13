# Step 10: カスタムクエリ

## 🎯 このステップの目標

- `@Query`アノテーションでJPQLを使用できる
- ネイティブSQLクエリを実行できる
- 複雑な検索条件を持つクエリを実装できる
- 集計クエリ（COUNT、SUM、AVGなど）を実行できる

**所要時間**: 約50分

---

## 📋 事前準備

- [Step 9](STEP_9.md)が完了していること
- `ProductService`が作成されていること
- MySQLコンテナが起動していること

---

## 🧩 Spring Data JPAのクエリ方法

### 3つのアプローチ

Spring Data JPAでは、クエリを実装する方法が3つあります：

| 方法 | 例 | 使用場面 |
|---|---|---|
| **メソッド名規則** | `findByNameContaining(String name)` | シンプルな条件 |
| **JPQL** | `@Query("SELECT p FROM Product p WHERE...")` | 複雑な条件、JOIN |
| **ネイティブSQL** | `@Query(value = "SELECT * FROM...", nativeQuery = true)` | DB固有の機能 |

### JPQLとは

**JPQL（Java Persistence Query Language）** は、SQL風の構文でエンティティを操作するクエリ言語です。

**SQLとの違い**:
- テーブル名ではなく、**エンティティ名**を使用
- カラム名ではなく、**フィールド名**を使用
- データベース非依存

```sql
-- SQL
SELECT * FROM products WHERE price > 10000

-- JPQL
SELECT p FROM Product p WHERE p.price > 10000
```

---

## 🚀 ステップ1: JPQLクエリの基本

### 1-1. ProductRepositoryにカスタムクエリを追加

`ProductRepository.java`に以下のメソッドを**追加**します：

**ファイルパス**: `src/main/java/com/example/hellospringboot/ProductRepository.java`

```java
package com.example.hellospringboot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // メソッド名規則（既存）
    List<Product> findByPriceBetween(Integer minPrice, Integer maxPrice);
    List<Product> findByNameContaining(String keyword);
    
    // JPQL: 価格範囲と名前で検索
    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :minPrice AND :maxPrice AND p.name LIKE %:keyword%")
    List<Product> searchProducts(
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("keyword") String keyword
    );
    
    // JPQL: 価格が高い順にN件取得
    @Query("SELECT p FROM Product p ORDER BY p.price DESC")
    List<Product> findTopExpensiveProducts();
    
    // JPQL: 平均価格を取得
    @Query("SELECT AVG(p.price) FROM Product p")
    Double getAveragePrice();
    
    // JPQL: 価格が平均より高い商品
    @Query("SELECT p FROM Product p WHERE p.price > (SELECT AVG(p2.price) FROM Product p2)")
    List<Product> findProductsAboveAveragePrice();
}
```

### 1-2. コードの解説

#### `@Query`

JPQLまたはネイティブSQLを記述します。

```java
@Query("SELECT p FROM Product p WHERE p.price > :minPrice")
```

- `p`: エンティティのエイリアス（別名）
- `Product`: エンティティ名（テーブル名ではない）
- `:minPrice`: 名前付きパラメータ

#### `@Param`

クエリのパラメータ名を明示的に指定します。

```java
List<Product> method(@Param("minPrice") Integer minPrice);
```

**注意**: Java 8以降でコンパイルオプション`-parameters`を付けている場合、`@Param`は省略可能です。ただし、明示的に付けることを推奨します。

#### LIKE演算子

部分一致検索には`LIKE`を使用します。

```java
// 前方一致
p.name LIKE :keyword%

// 後方一致
p.name LIKE %:keyword

// 部分一致
p.name LIKE %:keyword%
```

#### 集計関数

| 関数 | 説明 | 例 |
|---|---|---|
| `COUNT(p)` | 件数 | `SELECT COUNT(p) FROM Product p` |
| `SUM(p.price)` | 合計 | `SELECT SUM(p.price) FROM Product p` |
| `AVG(p.price)` | 平均 | `SELECT AVG(p.price) FROM Product p` |
| `MAX(p.price)` | 最大値 | `SELECT MAX(p.price) FROM Product p` |
| `MIN(p.price)` | 最小値 | `SELECT MIN(p.price) FROM Product p` |

---

## 🚀 ステップ2: ネイティブSQLクエリ

### 2-1. ネイティブSQLの追加

MySQLの機能を直接使う場合は、ネイティブSQLを使用します。

`ProductRepository.java`に以下を**追加**:

```java
// ネイティブSQL: 全文検索（MySQL FULLTEXT）
@Query(value = "SELECT * FROM products WHERE MATCH(name, description) AGAINST (:keyword IN NATURAL LANGUAGE MODE)", nativeQuery = true)
List<Product> fullTextSearch(@Param("keyword") String keyword);

// ネイティブSQL: 月別集計
@Query(value = "SELECT DATE_FORMAT(created_at, '%Y-%m') as month, COUNT(*) as count FROM products GROUP BY month ORDER BY month DESC", nativeQuery = true)
List<Object[]> getMonthlyProductCount();

// ネイティブSQL: 価格帯別の商品数
@Query(value = """
    SELECT 
        CASE 
            WHEN price < 10000 THEN '低価格'
            WHEN price < 50000 THEN '中価格'
            ELSE '高価格'
        END as price_range,
        COUNT(*) as count
    FROM products
    GROUP BY price_range
    """, nativeQuery = true)
List<Object[]> getProductCountByPriceRange();
```

### 2-2. コードの解説

#### `nativeQuery = true`

ネイティブSQLを使用することを示します。

```java
@Query(value = "SELECT * FROM products WHERE ...", nativeQuery = true)
```

**注意**:
- テーブル名とカラム名を使用（エンティティ名ではない）
- データベース依存になる（MySQLからPostgreSQLに移行すると動かない可能性）

#### テキストブロック（`"""`）

Java 15以降で使える複数行文字列リテラルです。

```java
@Query(value = """
    SELECT *
    FROM products
    WHERE price > 10000
    """, nativeQuery = true)
```

---

## 🚀 ステップ3: Serviceクラスの更新

### 3-1. ProductServiceに検索メソッドを追加

`ProductService.java`に以下のメソッドを**追加**:

```java
@Transactional(readOnly = true)
public List<Product> searchProducts(Integer minPrice, Integer maxPrice, String keyword) {
    return productRepository.searchProducts(minPrice, maxPrice, keyword);
}

@Transactional(readOnly = true)
public List<Product> getTopExpensiveProducts() {
    return productRepository.findTopExpensiveProducts();
}

@Transactional(readOnly = true)
public Double getAveragePrice() {
    return productRepository.getAveragePrice();
}

@Transactional(readOnly = true)
public List<Product> getProductsAboveAveragePrice() {
    return productRepository.findProductsAboveAveragePrice();
}
```

---

## 🚀 ステップ4: Controllerの更新

### 4-1. ProductControllerに検索エンドポイントを追加

`ProductController.java`に以下のメソッドを**追加**:

```java
// 複合検索
@GetMapping("/search")
public List<Product> searchProducts(
        @RequestParam(required = false, defaultValue = "0") Integer minPrice,
        @RequestParam(required = false, defaultValue = "999999") Integer maxPrice,
        @RequestParam(required = false, defaultValue = "") String keyword) {
    return productService.searchProducts(minPrice, maxPrice, keyword);
}

// 高額商品
@GetMapping("/expensive")
public List<Product> getTopExpensiveProducts() {
    return productService.getTopExpensiveProducts();
}

// 平均価格
@GetMapping("/average-price")
public Double getAveragePrice() {
    return productService.getAveragePrice();
}

// 平均以上の商品
@GetMapping("/above-average")
public List<Product> getProductsAboveAveragePrice() {
    return productService.getProductsAboveAveragePrice();
}
```

---

## ✅ ステップ5: 動作確認

### 5-1. テストデータの準備

```bash
# 商品1
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name": "格安ノートPC", "description": "エントリーモデル", "price": 50000, "stock": 10}'

# 商品2
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name": "高性能ノートPC", "description": "プロ向けハイスペック", "price": 200000, "stock": 5}'

# 商品3
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name": "ワイヤレスマウス", "description": "シンプルなマウス", "price": 3000, "stock": 50}'

# 商品4
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name": "ゲーミングマウス", "description": "高性能マウス", "price": 12000, "stock": 20}'

# 商品5
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name": "キーボード", "description": "メカニカルキーボード", "price": 15000, "stock": 15}'
```

### 5-2. 複合検索

```bash
# 価格範囲と名前で検索
curl "http://localhost:8080/api/products/search?minPrice=10000&maxPrice=100000&keyword=ノート"
```

**期待される結果**: 「格安ノートPC」（50,000円）のみ

### 5-3. 高額商品取得

```bash
curl http://localhost:8080/api/products/expensive
```

**期待される結果**: 価格が高い順にソートされたリスト

### 5-4. 平均価格取得

```bash
curl http://localhost:8080/api/products/average-price
```

**期待される結果**: 数値（例: `56000.0`）

### 5-5. 平均以上の商品

```bash
curl http://localhost:8080/api/products/above-average
```

**期待される結果**: 平均価格より高い商品のリスト

---

## 🎨 チャレンジ課題

基本が理解できたら、以下にチャレンジしてみましょう：

### チャレンジ 1: ページネーション付きカスタムクエリ

`@Query`と`Pageable`を組み合わせてページネーションを実装してください。

**ヒント**:

```java
@Query("SELECT p FROM Product p WHERE p.price > :minPrice")
Page<Product> findExpensiveProducts(@Param("minPrice") Integer minPrice, Pageable pageable);
```

**Controllerで使用**:

```java
@GetMapping("/expensive-paged")
public Page<Product> getExpensiveProducts(
        @RequestParam Integer minPrice,
        Pageable pageable) {
    return productService.findExpensiveProducts(minPrice, pageable);
}
```

**テスト**:

```bash
curl "http://localhost:8080/api/products/expensive-paged?minPrice=10000&page=0&size=10&sort=price,desc"
```

### チャレンジ 2: 動的クエリ

条件によってクエリを変える動的クエリを実装してください。

**ヒント**: Specificationを使用

```java
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
}
```

```java
public static Specification<Product> hasName(String name) {
    return (root, query, criteriaBuilder) -> 
        name == null ? null : criteriaBuilder.like(root.get("name"), "%" + name + "%");
}

public static Specification<Product> hasPriceBetween(Integer min, Integer max) {
    return (root, query, criteriaBuilder) -> 
        criteriaBuilder.between(root.get("price"), min, max);
}
```

**使用例**:

```java
Specification<Product> spec = Specification
    .where(hasName("ノート"))
    .and(hasPriceBetween(10000, 100000));
List<Product> results = productRepository.findAll(spec);
```

### チャレンジ 3: DTO プロジェクション

エンティティ全体ではなく、必要なフィールドのみを取得してください。

**DTO作成**:

```java
public interface ProductSummary {
    Long getId();
    String getName();
    Integer getPrice();
}
```

**Repository**:

```java
@Query("SELECT p.id as id, p.name as name, p.price as price FROM Product p")
List<ProductSummary> findAllSummaries();
```

---

## 🐛 トラブルシューティング

### エラー: "QuerySyntaxException: unexpected token"

**原因**: JPQLの構文エラー

**よくあるミス**:

```java
// ❌ テーブル名を使用している
@Query("SELECT * FROM products WHERE price > :price")

// ✅ エンティティ名を使用
@Query("SELECT p FROM Product p WHERE p.price > :price")
```

### エラー: "Named parameter not bound"

**原因**: `@Param`の名前とクエリのパラメータ名が一致していない

```java
// ❌ 名前が違う
@Query("SELECT p FROM Product p WHERE p.price > :minPrice")
List<Product> method(@Param("price") Integer price);

// ✅ 名前を一致させる
@Query("SELECT p FROM Product p WHERE p.price > :minPrice")
List<Product> method(@Param("minPrice") Integer minPrice);
```

### ネイティブクエリで日本語が文字化け

**原因**: MySQLの文字コード設定

**確認**:

```bash
docker compose exec mysql mysql -u springuser -pspringpass hello_spring_boot -e "SHOW VARIABLES LIKE 'character%';"
```

**解決策**: docker-compose.ymlで文字コードを設定（既に設定済みのはず）

### クエリ結果が期待と異なる

**デバッグ方法**:

`application.yml`でSQLログを有効化:

```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

これにより、実行されるSQLとバインドパラメータが全て表示されます。

---

## 📚 このステップで学んだこと

- ✅ `@Query`アノテーションでJPQLを記述できるようになった
- ✅ JPQLとSQLの違いを理解した
- ✅ ネイティブSQLクエリを使用できるようになった
- ✅ 集計関数（AVG、COUNT、SUMなど）を使えるようになった
- ✅ サブクエリを含む複雑なクエリを実装できるようになった
- ✅ 名前付きパラメータ（`@Param`）の使い方を学んだ
- ✅ JPQLとネイティブSQLの使い分けを理解した

---

## 💡 補足: JPQLの高度な機能

### JOIN

```java
@Query("SELECT p FROM Product p JOIN p.category c WHERE c.name = :categoryName")
List<Product> findByCategory(@Param("categoryName") String categoryName);
```

### GROUP BY と HAVING

```java
@Query("SELECT p.category, COUNT(p) FROM Product p GROUP BY p.category HAVING COUNT(p) > :minCount")
List<Object[]> getCategoriesWithMinProducts(@Param("minCount") Long minCount);
```

### DISTINCT

```java
@Query("SELECT DISTINCT p.name FROM Product p")
List<String> findDistinctNames();
```

### ORDER BY

```java
@Query("SELECT p FROM Product p ORDER BY p.price DESC, p.name ASC")
List<Product> findAllOrdered();
```

### CASE WHEN

```java
@Query("""
    SELECT p.name,
        CASE 
            WHEN p.price < 10000 THEN '安い'
            WHEN p.price < 50000 THEN '普通'
            ELSE '高い'
        END as priceCategory
    FROM Product p
    """)
List<Object[]> getProductsWithPriceCategory();
```

---

## ➡️ 次のステップ

[Step 11: リレーションシップ（1対多）](STEP_11.md)へ進みましょう！

次のステップでは、複数のエンティティ間の関連を定義し、JOIN操作を実装します。商品とカテゴリの関係を例に、実務でよく使う1対多の関連を学びます。
