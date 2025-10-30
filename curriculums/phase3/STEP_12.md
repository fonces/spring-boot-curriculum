# Step 12: MyBatisの基礎

## 🎯 このステップの目標

- MyBatisとは何かを理解する
- JPAとMyBatisの違いを学ぶ
- MyBatisでCRUD操作を実装する
- Mapper XMLとMapperインターフェースの基本を理解する

**所要時間**: 約1時間

---

## 📋 事前準備

- Phase 2 (Step 6〜11) の完了
- `User`エンティティとJPAでのCRUD実装の理解
- MySQLが起動していること

---

## 💡 MyBatisとは？

### MyBatisの特徴

**MyBatis** は、JavaでSQLを扱うためのORMフレームワークです。

**主な特徴**:
- **SQLを直接記述**: 複雑なクエリも自由に書ける
- **パフォーマンス最適化**: 必要なカラムだけを取得可能
- **動的SQL**: 条件に応じたクエリを柔軟に生成
- **学習コストが低い**: SQLを知っていればすぐに使える

### JPAとMyBatisの比較

| 観点 | JPA (Hibernate) | MyBatis |
|------|----------------|---------|
| **SQL制御** | 自動生成（抽象化） | 手動記述（明示的） |
| **複雑なクエリ** | JPQLやCriteria API（煩雑） | SQL直接記述（柔軟） |
| **学習コスト** | やや高い（概念が多い） | 低い（SQLがわかれば可） |
| **開発速度** | 速い（CRUD自動生成） | やや遅い（SQL手書き） |
| **パフォーマンス最適化** | やや難しい（N+1問題など） | 容易（SQLを直接最適化） |
| **保守性** | エンティティベース | SQLベース |

### 使い分けの目安

**JPAが向いている場合**:
- シンプルなCRUD操作が中心
- エンティティ間のリレーションが複雑
- オブジェクト指向的な設計を重視

**MyBatisが向いている場合**:
- 複雑な検索条件やレポート生成
- パフォーマンスチューニングが必要
- 既存のSQLを活用したい
- レガシーDBスキーマに対応

---

## 🏗️ 実装手順

### Step 1: MyBatis依存関係の追加

`pom.xml`に以下を追加:

```xml
<dependencies>
    <!-- 既存の依存関係 -->
    
    <!-- MyBatis Spring Boot Starter -->
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>3.0.3</version>
    </dependency>
</dependencies>
```

**ポイント**:
- `mybatis-spring-boot-starter`でMyBatisとSpring Bootの統合が簡単に
- バージョン3.0.3はSpring Boot 3系に対応

### Step 2: application.ymlにMyBatis設定を追加

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/spring_curriculum
    username: user
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

# MyBatis設定
mybatis:
  # Mapper XMLファイルの場所
  mapper-locations: classpath:mapper/**/*.xml
  # エイリアスのパッケージ
  type-aliases-package: com.example.demo.entity
  configuration:
    # キャメルケース⇔スネークケース自動変換
    map-underscore-to-camel-case: true
    # SQLログ出力
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

**設定の意味**:
- `mapper-locations`: Mapper XMLファイルの配置場所
- `type-aliases-package`: エンティティクラスのパッケージ（XMLで短縮名が使える）
- `map-underscore-to-camel-case`: `user_name` ⇔ `userName` の自動変換
- `log-impl`: 実行されるSQLをコンソールに出力

### Step 3: Productエンティティの作成

新しいエンティティ `Product` を作成します（MyBatis専用のため、JPAアノテーションは不要）:

```java
package com.example.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**ポイント**:
- `@Entity`などのJPAアノテーションは不要
- POJOとして定義するだけでOK
- Lombokで冗長なコードを削減

### Step 4: MySQLにテーブルを作成

MySQLに接続してテーブルを作成します:

```sql
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- サンプルデータ挿入
INSERT INTO products (name, description, price, stock) VALUES
('ノートPC', '高性能なノートパソコン', 120000.00, 10),
('マウス', 'ワイヤレスマウス', 2500.00, 50),
('キーボード', 'メカニカルキーボード', 15000.00, 20);
```

**実行方法**:
```bash
# MySQLコンテナに接続
docker exec -it mysql-container mysql -u user -p

# パスワード入力後、上記SQLを実行
```

### Step 5: Mapperインターフェースの作成

`src/main/java/com/example/demo/mapper/ProductMapper.java`:

```java
package com.example.demo.mapper;

import com.example.demo.entity.Product;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProductMapper {
    
    // 全件取得
    @Select("SELECT * FROM products ORDER BY id")
    List<Product> findAll();
    
    // ID検索
    @Select("SELECT * FROM products WHERE id = #{id}")
    Product findById(Long id);
    
    // 新規作成
    @Insert("INSERT INTO products (name, description, price, stock) " +
            "VALUES (#{name}, #{description}, #{price}, #{stock})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Product product);
    
    // 更新
    @Update("UPDATE products SET name = #{name}, description = #{description}, " +
            "price = #{price}, stock = #{stock} WHERE id = #{id}")
    int update(Product product);
    
    // 削除
    @Delete("DELETE FROM products WHERE id = #{id}")
    int deleteById(Long id);
    
    // 名前で検索（部分一致）
    @Select("SELECT * FROM products WHERE name LIKE CONCAT('%', #{keyword}, '%')")
    List<Product> searchByName(String keyword);
}
```

**重要なアノテーション**:
- `@Mapper`: MyBatisのMapperであることを示す
- `@Select`, `@Insert`, `@Update`, `@Delete`: SQL文を直接記述
- `#{変数名}`: プレースホルダー（SQLインジェクション対策済み）
- `@Options(useGeneratedKeys = true)`: 自動生成されたIDを取得

### Step 6: Serviceクラスの作成

`src/main/java/com/example/demo/service/ProductService.java`:

```java
package com.example.demo.service;

import com.example.demo.entity.Product;
import com.example.demo.mapper.ProductMapper;
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
        return product; // IDが自動設定される
    }
    
    @Transactional
    public Product updateProduct(Long id, Product product) {
        product.setId(id);
        int updated = productMapper.update(product);
        if (updated == 0) {
            throw new RuntimeException("Product not found: " + id);
        }
        return productMapper.findById(id);
    }
    
    @Transactional
    public void deleteProduct(Long id) {
        int deleted = productMapper.deleteById(id);
        if (deleted == 0) {
            throw new RuntimeException("Product not found: " + id);
        }
    }
    
    public List<Product> searchProducts(String keyword) {
        return productMapper.searchByName(keyword);
    }
}
```

**ポイント**:
- Mapperを直接使ってビジネスロジックを実装
- `@Transactional`でトランザクション管理

### Step 7: Controllerクラスの作成

`src/main/java/com/example/demo/controller/ProductController.java`:

```java
package com.example.demo.controller;

import com.example.demo.entity.Product;
import com.example.demo.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    
    private final ProductService productService;
    
    // 全件取得
    @GetMapping
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }
    
    // ID検索
    @GetMapping("/{id}")
    public Product getProduct(@PathVariable Long id) {
        return productService.getProductById(id);
    }
    
    // 新規作成
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product createProduct(@RequestBody Product product) {
        return productService.createProduct(product);
    }
    
    // 更新
    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable Long id, @RequestBody Product product) {
        return productService.updateProduct(id, product);
    }
    
    // 削除
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
    }
    
    // 名前で検索
    @GetMapping("/search")
    public List<Product> searchProducts(@RequestParam String keyword) {
        return productService.searchProducts(keyword);
    }
}
```

---

## ✅ 動作確認

### 1. アプリケーション起動

```bash
mvn spring-boot:run
```

起動ログで以下を確認:
```
o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http)
```

### 2. 全件取得

```bash
curl http://localhost:8080/api/products
```

**期待される結果**:
```json
[
  {
    "id": 1,
    "name": "ノートPC",
    "description": "高性能なノートパソコン",
    "price": 120000.00,
    "stock": 10,
    "createdAt": "2025-10-29T10:00:00",
    "updatedAt": "2025-10-29T10:00:00"
  },
  {
    "id": 2,
    "name": "マウス",
    "description": "ワイヤレスマウス",
    "price": 2500.00,
    "stock": 50,
    "createdAt": "2025-10-29T10:00:00",
    "updatedAt": "2025-10-29T10:00:00"
  }
]
```

### 3. 新規作成

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "モニター",
    "description": "27インチ4Kモニター",
    "price": 45000.00,
    "stock": 15
  }'
```

### 4. ID検索

```bash
curl http://localhost:8080/api/products/1
```

### 5. 更新

```bash
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "ノートPC",
    "description": "超高性能なノートパソコン",
    "price": 150000.00,
    "stock": 8
  }'
```

### 6. 名前で検索

```bash
curl "http://localhost:8080/api/products/search?keyword=マウス"
```

### 7. 削除

```bash
curl -X DELETE http://localhost:8080/api/products/3
```

---

## 🔍 アノテーションベースとXMLベースの比較

### アノテーションベース（現在の実装）

**メリット**:
- Javaコード内で完結
- シンプルなSQLに最適
- IDEの補完が効く

**デメリット**:
- 複雑なSQLは見づらい
- 動的SQLが書きにくい

### XMLベース（次のステップで学習）

**メリット**:
- 複雑なSQLが書きやすい
- 動的SQLが得意
- SQLとJavaコードの分離

**デメリット**:
- ファイルが分散する
- XMLの記述が必要

**例**: `src/main/resources/mapper/ProductMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.demo.mapper.ProductMapper">
    
    <select id="findAll" resultType="Product">
        SELECT * FROM products ORDER BY id
    </select>
    
    <select id="findById" resultType="Product">
        SELECT * FROM products WHERE id = #{id}
    </select>
    
    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO products (name, description, price, stock)
        VALUES (#{name}, #{description}, #{price}, #{stock})
    </insert>
</mapper>
```

---

## 📝 理解度チェック

以下の質問に答えられるか確認しましょう:

1. **MyBatisとJPAの主な違いは何ですか？**
2. **`@Mapper`アノテーションの役割は何ですか？**
3. **`#{変数名}`の記法は何のためにありますか？**
4. **MyBatisでトランザクション管理は必要ですか？**
5. **どのような場合にMyBatisを選ぶべきですか？**

---

## 💡 ベストプラクティス

1. **Mapperは薄く保つ**: ビジネスロジックはServiceに
2. **SQLインジェクション対策**: 必ず`#{}`を使う（`${}`は危険）
3. **適切なトランザクション**: 更新系は`@Transactional`で囲む
4. **命名規則**: MapperメソッドはSQL操作を表す名前に
5. **ログ確認**: 実行されるSQLを確認して最適化

---

## 📚 このステップで学んだこと

このステップでは、MyBatis基礎について学びました：

- ✅ MyBatisとは何か、JPAとの違いを理解
- ✅ MyBatisの依存関係追加とセットアップ
- ✅ @Mapperアノテーションでマッパーインターフェース作成
- ✅ @Select, @Insert, @Update, @Deleteアノテーションの基本
- ✅ アノテーションベースとXMLベースの比較
- ✅ MyBatis設定（mybatis.configuration）のカスタマイズ

---

## 🐛 トラブルシューティング

### エラー: "Invalid bound statement (not found)"

**原因**: Mapperインターフェースのメソッドとマッピングが見つからない

**解決策**:
1. `@Mapper`アノテーションが付いているか確認
2. `@MapperScan`のパッケージパスが正しいか確認
3. メソッド名とアノテーション内のSQLが対応しているか確認
4. プロジェクトをクリーンビルド: `mvn clean install`

### エラー: "There is no getter for property named 'xxx'"

**原因**: エンティティクラスにgetterがない、またはプロパティ名が一致しない

**解決策**:
1. エンティティクラスに`@Data`または`@Getter`を追加（Lombok使用時）
2. プロパティ名とSQL内の`#{propertyName}`が一致しているか確認
3. キャメルケース変換が有効か確認: `mybatis.configuration.map-underscore-to-camel-case=true`

### エラー: "Error updating database. Cause: java.sql.SQLSyntaxErrorException"

**原因**: SQLの構文エラー

**解決策**:
1. `logging.level.com.example.demo.mapper=DEBUG`でSQLログを確認
2. SQLをMySQLクライアントで直接実行してみる
3. テーブル名、カラム名のスペルミスを確認
4. `#{}`と`${}`を間違えていないか確認（基本は`#{}`を使用）

### エラー: "MyBatis configuration error: Property 'sqlSessionFactory' or 'sqlSessionTemplate' are required"

**原因**: MyBatisの自動設定が正しく動作していない

**解決策**:
1. `mybatis-spring-boot-starter`の依存関係が正しく追加されているか確認
2. `application.yml`のデータソース設定を確認
3. Spring Bootのバージョンとの互換性を確認

### エラー: トランザクションがロールバックされない

**原因**: `@Transactional`がないか、スコープが間違っている

**解決策**:
1. Service層のメソッドに`@Transactional`を追加
2. `@Transactional`はpublicメソッドに付ける（privateは効かない）
3. 例外がRuntimeException継承でない場合は`@Transactional(rollbackFor = Exception.class)`を指定

---

## 🔄 Gitへのコミットとレビュー依頼

進捗を記録してレビューを受けましょう：

```bash
git add .
git commit -m "Step 12: MyBatisの基礎完了"
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

レビューが完了したら、[Step 13: MyBatisで複雑なクエリ](STEP_13.md)へ進みましょう！

次のStep 13では、MyBatisの真骨頂である**動的SQL**と**複雑なクエリ**を学びます:
- `<if>`, `<choose>`, `<foreach>`を使った条件分岐
- JOINを使った複数テーブルの結合
- ResultMapでの複雑なマッピング
- ページネーションの実装

---

## 🔖 参考リンク

- [MyBatis公式ドキュメント](https://mybatis.org/mybatis-3/ja/)
- [MyBatis-Spring-Boot-Starter](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)
- [MyBatis Dynamic SQL](https://mybatis.org/mybatis-dynamic-sql/docs/introduction.html)

---

お疲れ様でした！🎉 MyBatisの基礎が理解できたら、次のステップに進みましょう。
