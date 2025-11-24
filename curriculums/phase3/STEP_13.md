# Step 13: MyBatisで複雑なクエリ

## 🎯 このステップの目標

- 動的SQL（`<if>`, `<choose>`, `<foreach>`）を使った柔軟なクエリを実装する
- JOINを使って複数テーブルのデータを効率的に取得する
- ResultMapで複雑なオブジェクトマッピングを定義する
- ページネーション機能を実装する
- MyBatisの高度な機能を理解し、実務レベルの検索APIを作成する

**所要時間**: 約1.5時間

---

## 📋 事前準備

- Step 12の完了
- MyBatisの基本的な使い方（Mapper XMLとMapperインターフェース）の理解
- 商品管理APIが動作していること
- MySQLが起動していること

**Step 12をまだ完了していない場合**: [Step 12: MyBatisの基礎](STEP_12.md)を先に進めてください。

---

## 1. テーブル設計の拡張

### 1.1 カテゴリテーブルの追加

商品にカテゴリを関連付けるため、新しいテーブルを作成します。

**resources/db/migration/V2__create_category_table.sql**
```sql
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 商品テーブルにカテゴリIDを追加
ALTER TABLE products ADD COLUMN category_id BIGINT;
ALTER TABLE products ADD CONSTRAINT fk_category 
    FOREIGN KEY (category_id) REFERENCES categories(id);

-- サンプルデータ
INSERT INTO categories (name, description) VALUES
('電化製品', '家電製品全般'),
('書籍', '本や雑誌'),
('食品', '食料品'),
('衣類', '服やアクセサリー');

-- 既存の商品にカテゴリを設定
UPDATE products SET category_id = 1 WHERE id = 1;
UPDATE products SET category_id = 2 WHERE id = 2;
```

---

## 2. エンティティクラスの作成

### 2.1 Categoryエンティティ

**src/main/java/com/example/hellospringboot/entity/Category.java**
```java
package com.example.hellospringboot.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 2.2 Productエンティティの拡張

**src/main/java/com/example/hellospringboot/entity/Product.java**
```java
package com.example.hellospringboot.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private Long categoryId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 結合用フィールド
    private Category category;
}
```

---

## 3. 動的SQLの実装

### 3.1 検索条件DTOの作成

**src/main/java/com/example/hellospringboot/dto/ProductSearchRequest.java**
```java
package com.example.hellospringboot.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductSearchRequest {
    private String name;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer minStock;
    private Long categoryId;
    private List<Long> categoryIds;
    private String sortBy; // name, price, stock
    private String sortOrder; // asc, desc
    private Integer page;
    private Integer size;
}
```

### 3.2 Mapperインターフェースの拡張

**src/main/java/com/example/hellospringboot/mapper/ProductMapper.java**
```java
package com.example.hellospringboot.mapper;

import com.example.hellospringboot.dto.ProductSearchRequest;
import com.example.hellospringboot.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {
    // 既存のメソッド
    List<Product> findAll();
    Product findById(Long id);
    void insert(Product product);
    void update(Product product);
    void deleteById(Long id);
    
    // 新規追加
    List<Product> search(ProductSearchRequest request);
    int countByCondition(ProductSearchRequest request);
    List<Product> findWithCategory();
    Product findByIdWithCategory(Long id);
    List<Product> findByCategoryIds(@Param("categoryIds") List<Long> categoryIds);
}
```

### 3.3 動的SQL（XML Mapper）

**src/main/resources/mapper/ProductMapper.xml**
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.hellospringboot.mapper.ProductMapper">

    <!-- ResultMap: 基本的な商品 -->
    <resultMap id="productResultMap" type="com.example.hellospringboot.entity.Product">
        <id property="id" column="id"/>
        <result property="name" column="name"/>
        <result property="price" column="price"/>
        <result property="stock" column="stock"/>
        <result property="categoryId" column="category_id"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
    </resultMap>

    <!-- ResultMap: カテゴリを含む商品 -->
    <resultMap id="productWithCategoryResultMap" type="com.example.hellospringboot.entity.Product">
        <id property="id" column="product_id"/>
        <result property="name" column="product_name"/>
        <result property="price" column="price"/>
        <result property="stock" column="stock"/>
        <result property="categoryId" column="category_id"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
        <association property="category" javaType="com.example.hellospringboot.entity.Category">
            <id property="id" column="category_id"/>
            <result property="name" column="category_name"/>
            <result property="description" column="category_description"/>
        </association>
    </resultMap>

    <!-- 既存のSQL -->
    <select id="findAll" resultMap="productResultMap">
        SELECT * FROM products
        ORDER BY id
    </select>

    <select id="findById" resultMap="productResultMap">
        SELECT * FROM products WHERE id = #{id}
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO products (name, price, stock, category_id)
        VALUES (#{name}, #{price}, #{stock}, #{categoryId})
    </insert>

    <update id="update">
        UPDATE products
        SET name = #{name},
            price = #{price},
            stock = #{stock},
            category_id = #{categoryId}
        WHERE id = #{id}
    </update>

    <delete id="deleteById">
        DELETE FROM products WHERE id = #{id}
    </delete>

    <!-- 動的SQL: 条件付き検索 -->
    <select id="search" resultMap="productResultMap">
        SELECT * FROM products
        <where>
            <if test="name != null and name != ''">
                AND name LIKE CONCAT('%', #{name}, '%')
            </if>
            <if test="minPrice != null">
                AND price &gt;= #{minPrice}
            </if>
            <if test="maxPrice != null">
                AND price &lt;= #{maxPrice}
            </if>
            <if test="minStock != null">
                AND stock &gt;= #{minStock}
            </if>
            <if test="categoryId != null">
                AND category_id = #{categoryId}
            </if>
            <if test="categoryIds != null and categoryIds.size() > 0">
                AND category_id IN
                <foreach collection="categoryIds" item="id" open="(" separator="," close=")">
                    #{id}
                </foreach>
            </if>
        </where>
        <if test="sortBy != null">
            ORDER BY
            <choose>
                <when test="sortBy == 'name'">name</when>
                <when test="sortBy == 'price'">price</when>
                <when test="sortBy == 'stock'">stock</when>
                <otherwise>id</otherwise>
            </choose>
            <choose>
                <when test="sortOrder == 'desc'">DESC</when>
                <otherwise>ASC</otherwise>
            </choose>
        </if>
        <if test="page != null and size != null">
            LIMIT #{size} OFFSET #{page}
        </if>
    </select>

    <!-- 検索結果のカウント -->
    <select id="countByCondition" resultType="int">
        SELECT COUNT(*) FROM products
        <where>
            <if test="name != null and name != ''">
                AND name LIKE CONCAT('%', #{name}, '%')
            </if>
            <if test="minPrice != null">
                AND price &gt;= #{minPrice}
            </if>
            <if test="maxPrice != null">
                AND price &lt;= #{maxPrice}
            </if>
            <if test="minStock != null">
                AND stock &gt;= #{minStock}
            </if>
            <if test="categoryId != null">
                AND category_id = #{categoryId}
            </if>
            <if test="categoryIds != null and categoryIds.size() > 0">
                AND category_id IN
                <foreach collection="categoryIds" item="id" open="(" separator="," close=")">
                    #{id}
                </foreach>
            </if>
        </where>
    </select>

    <!-- JOIN: カテゴリ情報を含む商品一覧 -->
    <select id="findWithCategory" resultMap="productWithCategoryResultMap">
        SELECT 
            p.id as product_id,
            p.name as product_name,
            p.price,
            p.stock,
            p.category_id,
            p.created_at,
            p.updated_at,
            c.name as category_name,
            c.description as category_description
        FROM products p
        LEFT JOIN categories c ON p.category_id = c.id
        ORDER BY p.id
    </select>

    <!-- JOIN: 特定商品のカテゴリ情報 -->
    <select id="findByIdWithCategory" resultMap="productWithCategoryResultMap">
        SELECT 
            p.id as product_id,
            p.name as product_name,
            p.price,
            p.stock,
            p.category_id,
            p.created_at,
            p.updated_at,
            c.name as category_name,
            c.description as category_description
        FROM products p
        LEFT JOIN categories c ON p.category_id = c.id
        WHERE p.id = #{id}
    </select>

    <!-- foreach: 複数カテゴリIDで検索 -->
    <select id="findByCategoryIds" resultMap="productResultMap">
        SELECT * FROM products
        WHERE category_id IN
        <foreach collection="categoryIds" item="id" open="(" separator="," close=")">
            #{id}
        </foreach>
    </select>

</mapper>
```

---

## 4. Serviceレイヤーの実装

### 4.1 ProductService

**src/main/java/com/example/hellospringboot/service/ProductService.java**
```java
package com.example.hellospringboot.service;

import com.example.hellospringboot.dto.ProductSearchRequest;
import com.example.hellospringboot.entity.Product;
import com.example.hellospringboot.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductMapper productMapper;

    public List<Product> getAllProducts() {
        return productMapper.findAll();
    }

    public Product getProductById(Long id) {
        return productMapper.findById(id);
    }

    @Transactional
    public Product createProduct(Product product) {
        productMapper.insert(product);
        return product;
    }

    @Transactional
    public Product updateProduct(Long id, Product product) {
        Product existing = productMapper.findById(id);
        if (existing == null) {
            throw new RuntimeException("Product not found");
        }
        product.setId(id);
        productMapper.update(product);
        return productMapper.findById(id);
    }

    @Transactional
    public void deleteProduct(Long id) {
        productMapper.deleteById(id);
    }

    // 新規追加
    public List<Product> searchProducts(ProductSearchRequest request) {
        // ページネーション計算
        if (request.getPage() != null && request.getSize() != null) {
            int offset = request.getPage() * request.getSize();
            request.setPage(offset);
        }
        return productMapper.search(request);
    }

    public int countProducts(ProductSearchRequest request) {
        return productMapper.countByCondition(request);
    }

    public List<Product> getProductsWithCategory() {
        return productMapper.findWithCategory();
    }

    public Product getProductByIdWithCategory(Long id) {
        return productMapper.findByIdWithCategory(id);
    }

    public List<Product> getProductsByCategoryIds(List<Long> categoryIds) {
        return productMapper.findByCategoryIds(categoryIds);
    }
}
```

---

## 5. Controllerの実装

### 5.1 ProductController

**src/main/java/com/example/hellospringboot/controller/ProductController.java**
```java
package com.example.hellospringboot.controller;

import com.example.hellospringboot.dto.ProductSearchRequest;
import com.example.hellospringboot.entity.Product;
import com.example.hellospringboot.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public Product getProduct(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        return productService.createProduct(product);
    }

    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable Long id, @RequestBody Product product) {
        return productService.updateProduct(id, product);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // 新規追加
    @GetMapping("/search")
    public Map<String, Object> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(required = false) Integer minStock,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) List<Long> categoryIds,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setName(name);
        request.setMinPrice(minPrice);
        request.setMaxPrice(maxPrice);
        request.setMinStock(minStock);
        request.setCategoryId(categoryId);
        request.setCategoryIds(categoryIds);
        request.setSortBy(sortBy);
        request.setSortOrder(sortOrder);
        request.setPage(page);
        request.setSize(size);

        List<Product> products = productService.searchProducts(request);
        int totalCount = productService.countProducts(request);

        Map<String, Object> response = new HashMap<>();
        response.put("products", products);
        response.put("totalCount", totalCount);
        response.put("page", page);
        response.put("size", size);
        response.put("totalPages", (int) Math.ceil((double) totalCount / size));

        return response;
    }

    @GetMapping("/with-category")
    public List<Product> getProductsWithCategory() {
        return productService.getProductsWithCategory();
    }

    @GetMapping("/{id}/with-category")
    public Product getProductByIdWithCategory(@PathVariable Long id) {
        return productService.getProductByIdWithCategory(id);
    }

    @GetMapping("/by-categories")
    public List<Product> getProductsByCategoryIds(@RequestParam List<Long> categoryIds) {
        return productService.getProductsByCategoryIds(categoryIds);
    }
}
```

---

## 6. 動作確認

### 6.1 アプリケーションの起動

```bash
./mvnw spring-boot:run
```

### 6.2 API テスト

#### 6.2.1 カテゴリ付き商品一覧を取得

```bash
curl http://localhost:8080/api/products/with-category | jq
```

**期待されるレスポンス:**
```json
[
  {
    "id": 1,
    "name": "ノートPC",
    "price": 89800,
    "stock": 5,
    "categoryId": 1,
    "category": {
      "id": 1,
      "name": "電化製品",
      "description": "家電製品全般"
    }
  }
]
```

#### 6.2.2 動的検索（名前と価格範囲）

```bash
curl "http://localhost:8080/api/products/search?name=ノート&minPrice=50000&maxPrice=100000&sortBy=price&sortOrder=asc" | jq
```

#### 6.2.3 複数カテゴリで検索

```bash
curl "http://localhost:8080/api/products/by-categories?categoryIds=1&categoryIds=2" | jq
```

#### 6.2.4 ページネーション付き検索

```bash
curl "http://localhost:8080/api/products/search?page=0&size=5" | jq
```

**期待されるレスポンス:**
```json
{
  "products": [...],
  "totalCount": 15,
  "page": 0,
  "size": 5,
  "totalPages": 3
}
```

#### 6.2.5 複合条件検索

```bash
curl "http://localhost:8080/api/products/search?name=PC&minStock=3&categoryId=1&sortBy=price&sortOrder=desc" | jq
```

---

## 7. MyBatis動的SQLの要素

### 7.1 `<if>` 条件分岐

```xml
<if test="name != null and name != ''">
    AND name LIKE CONCAT('%', #{name}, '%')
</if>
```

- 条件が真の場合のみSQLに含まれる
- `test`属性で条件を指定

### 7.2 `<choose>`, `<when>`, `<otherwise>` (switch-case)

```xml
<choose>
    <when test="sortBy == 'name'">name</when>
    <when test="sortBy == 'price'">price</when>
    <otherwise>id</otherwise>
</choose>
```

- 複数条件から1つを選択
- Javaの`switch`文に相当

### 7.3 `<where>` 自動WHERE句生成

```xml
<where>
    <if test="name != null">AND name = #{name}</if>
    <if test="price != null">AND price = #{price}</if>
</where>
```

- 最初の`AND`や`OR`を自動削除
- 条件が1つもない場合、`WHERE`自体を出力しない

### 7.4 `<foreach>` ループ処理

```xml
<foreach collection="categoryIds" item="id" open="(" separator="," close=")">
    #{id}
</foreach>
```

- コレクションを繰り返し処理
- `IN`句の生成に便利
- 出力例: `(1, 2, 3)`

### 7.5 `<set>` 自動SET句生成

```xml
<set>
    <if test="name != null">name = #{name},</if>
    <if test="price != null">price = #{price},</if>
</set>
```

- UPDATE文で便利
- 最後の`,`を自動削除

---

## 8. ResultMapの活用

### 8.1 基本的なマッピング

```xml
<resultMap id="productResultMap" type="com.example.hellospringboot.entity.Product">
    <id property="id" column="id"/>
    <result property="name" column="name"/>
</resultMap>
```

### 8.2 Association（1対1）

```xml
<association property="category" javaType="com.example.hellospringboot.entity.Category">
    <id property="id" column="category_id"/>
    <result property="name" column="category_name"/>
</association>
```

### 8.3 Collection（1対多）

```xml
<collection property="products" ofType="com.example.hellospringboot.entity.Product">
    <id property="id" column="product_id"/>
    <result property="name" column="product_name"/>
</collection>
```

---

## 9. パフォーマンス最適化

### 9.1 N+1問題の回避

❌ **悪い例（N+1問題）**
```java
// 商品を取得（1回）
List<Product> products = productMapper.findAll();

// 各商品のカテゴリを取得（N回）
for (Product product : products) {
    Category category = categoryMapper.findById(product.getCategoryId());
    product.setCategory(category);
}
```

✅ **良い例（JOIN使用）**
```java
// 1回のクエリで商品とカテゴリを取得
List<Product> products = productMapper.findWithCategory();
```

### 9.2 必要なカラムだけ取得

```xml
<select id="findNamesOnly" resultType="String">
    SELECT name FROM products
</select>
```

---

## 10. まとめ

### できるようになったこと
✅ 動的SQLで柔軟な検索機能を実装  
✅ JOINで複数テーブルのデータを効率的に取得  
✅ ResultMapで複雑なマッピングを定義  
✅ ページネーションで大量データを扱う  
✅ MyBatisの主要な機能を理解

---

## 📚 このステップで学んだこと

このステップでは、MyBatis動的SQLについて学びました：

- ✅ XMLベースのマッパーファイルの作成
- ✅ <if>, <where>, <set>タグで条件分岐
- ✅ <foreach>タグで繰り返し処理（IN句など）
- ✅ <choose><when><otherwise>で複数条件分岐
- ✅ ResultMapを使った複雑なマッピング
- ✅ 動的SQLでの検索条件の柔軟な組み立て

---

## 🐛 トラブルシューティング

### エラー: "Error parsing Mapper XML. The XML location is 'classpath:mapper/ProductMapper.xml'"

**原因**: XMLファイルのパスが間違っているか、XML構文エラー

**解決策**:
1. XMLファイルが`src/main/resources/mapper/`に配置されているか確認
2. `application.yml`の`mybatis.mapper-locations`が正しいか確認
3. XML宣言とDOCTYPE宣言が正しいか確認
4. タグの閉じ忘れがないかチェック

### エラー: "Parameter 'xxx' not found. Available parameters are [param1, arg0]"

**原因**: `@Param`アノテーションが付いていない

**解決策**:
```java
// ❌ NG: @Paramなし
List<Product> search(String name, Integer minPrice, Integer maxPrice);

// ✅ OK: @Paramあり
List<Product> search(
    @Param("name") String name,
    @Param("minPrice") Integer minPrice,
    @Param("maxPrice") Integer maxPrice
);
```

### エラー: "There is no getter for property named 'items' in 'class java.util.ArrayList'"

**原因**: `<foreach>`のcollectionプロパティ名が間違っている

**解決策**:
```xml
<!-- ❌ NG -->
<foreach collection="items" item="id" separator=",">
    #{id}
</foreach>

<!-- ✅ OK: @Param("ids")を使った場合 -->
<foreach collection="ids" item="id" separator=",">
    #{id}
</foreach>
```

### エラー: ResultMapでネストしたオブジェクトがnull

**原因**: `<association>`や`<collection>`の設定が不足

**解決策**:
1. `resultMap`のIDが正しく指定されているか確認
2. `<association>`に`javaType`が指定されているか確認
3. `<collection>`に`ofType`が指定されているか確認
4. SQLでJOINしているか確認

### エラー: 動的SQLが期待通りに動作しない

**原因**: `<where>`や`<if>`の条件式が間違っている

**解決策**:
1. SQLログを有効化して実際のSQLを確認: `logging.level.com.example.hellospringboot.mapper=DEBUG`
2. `test`属性の条件式を確認（`test="name != null and name != ''"`など）
3. `<where>`タグを使うとANDやORの前置詞を自動削除してくれる
4. MyBatisのOGNL式を理解する（`!= null`、`!= ''`など）

---

## 🔄 Gitへのコミットとレビュー依頼

進捗を記録してレビューを受けましょう：

```bash
git add .
git commit -m "Step 13: MyBatisで複雑なクエリ完了"
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

レビューが完了したら、[Step 14: JPAとMyBatisの使い分け](STEP_14.md)へ進みましょう！

次は **Step 14: JPAとMyBatisの使い分け** で、2つのORM技術を適材適所で使う方法を学びます。

---

## 参考資料
- [MyBatis Dynamic SQL](https://mybatis.org/mybatis-3/dynamic-sql.html)
- [MyBatis XML Mapper](https://mybatis.org/mybatis-3/sqlmap-xml.html)
- [MyBatis ResultMap](https://mybatis.org/mybatis-3/sqlmap-xml.html#Result_Maps)

