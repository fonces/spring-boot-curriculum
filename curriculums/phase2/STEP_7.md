# Step 7: Spring Data JPAでCRUDの基本

## 🎯 このステップの目標

- `JpaRepository`インターフェースの基本を理解できる
- リポジトリを作成してデータベース操作ができる
- REST APIでCRUDのうち作成（Create）と読み取り（Read）を実装できる
- Spring Data JPAの自動実装の仕組みを理解できる

**所要時間**: 約50分

---

## 📋 事前準備

- [Step 6](STEP_6.md)が完了していること
- `Product`エンティティが作成されていること
- MySQLコンテナが起動していること（`docker compose ps`で確認）

---

## 🧩 JpaRepositoryとは

### リポジトリパターン

**Repository（リポジトリ）**は、データアクセスロジックをカプセル化するデザインパターンです。

```
┌──────────────────────────────────────┐
│         Controller                   │
│  (HTTPリクエストを受け取る)            │
└──────────────┬───────────────────────┘
               │
               ↓
┌──────────────────────────────────────┐
│         Repository                   │
│  (データアクセスロジック)               │
│  ┌────────────────────────────────┐  │
│  │ save()                         │  │
│  │ findById()                     │  │
│  │ findAll()                      │  │
│  │ deleteById()                   │  │
│  └────────────────────────────────┘  │
└──────────────┬───────────────────────┘
               │
               ↓
┌──────────────────────────────────────┐
│         Database (MySQL)             │
└──────────────────────────────────────┘
```

### Spring Data JPAの魔法

`JpaRepository`を継承するだけで、基本的なCRUD操作が**自動実装**されます。

**従来のやり方（JDBC）**:
```java
public class ProductDao {
    public Product findById(Long id) {
        String sql = "SELECT * FROM products WHERE id = ?";
        // PreparedStatement、ResultSet、例外処理...
        // 100行以上のコード
    }
}
```

**Spring Data JPAの場合**:
```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    // これだけ！実装は自動生成される
}
```

**メリット**:
- **ボイラープレートの削減**: 繰り返しコードを書く必要がない
- **タイプセーフ**: コンパイル時に型チェックされる
- **保守性の向上**: SQLの記述ミスを防げる
- **テストしやすい**: モックに差し替えやすい

---

## 🚀 ステップ1: ProductRepositoryの作成

### 1-1. リポジトリインターフェースを作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/ProductRepository.java`

```java
package com.example.hellospringboot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // メソッドは自動実装されるため、ここには何も書かない
}
```

### 1-2. コードの解説

#### `JpaRepository<Product, Long>`

型パラメータを指定します：

- 第1引数（`Product`）: エンティティクラス
- 第2引数（`Long`）: 主キーの型

#### `@Repository`

このインターフェースがリポジトリであることを示すアノテーションです。

- Spring Beanとして登録される
- データアクセス例外を変換してくれる

**注意**: `JpaRepository`を継承している場合、`@Repository`は省略可能です（推奨は付ける）。

#### 自動実装されるメソッド

`JpaRepository`を継承すると、以下のメソッドが自動的に使えるようになります：

| メソッド | 説明 | SQL |
|---|---|---|
| `save(Product product)` | 保存または更新 | `INSERT` / `UPDATE` |
| `findById(Long id)` | IDで検索 | `SELECT ... WHERE id = ?` |
| `findAll()` | 全件取得 | `SELECT * FROM products` |
| `deleteById(Long id)` | IDで削除 | `DELETE ... WHERE id = ?` |
| `count()` | 件数取得 | `SELECT COUNT(*) FROM products` |
| `existsById(Long id)` | 存在確認 | `SELECT ... WHERE id = ? LIMIT 1` |

---

## 🚀 ステップ2: ProductControllerの作成

### 2-1. REST APIコントローラーを作成

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
    
    private final ProductRepository productRepository;
    
    // 全商品取得
    @GetMapping
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
    // 商品をIDで取得
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // 新しい商品を作成
    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product saved = productRepository.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
```

### 2-2. コードの解説

#### `@RequestMapping("/api/products")`

このコントローラー内の全てのエンドポイントに共通のベースパスを指定します。

- `/api/products` → 全商品取得
- `/api/products/1` → ID=1の商品取得

#### `@RequiredArgsConstructor`

Lombokのアノテーションで、`final`フィールドのコンストラクタを自動生成します。

これにより、以下のコードが自動生成されます：

```java
public ProductController(ProductRepository productRepository) {
    this.productRepository = productRepository;
}
```

#### `ResponseEntity<Product>`

HTTPレスポンスを柔軟に制御できるクラスです。

```java
ResponseEntity.ok(product)           // 200 OK
ResponseEntity.notFound().build()    // 404 Not Found
ResponseEntity.status(HttpStatus.CREATED).body(saved)  // 201 Created
```

#### `productRepository.findById(id)`

`Optional<Product>`を返します。

- **値が存在する場合**: `Optional.of(product)`
- **値が存在しない場合**: `Optional.empty()`

`.map(ResponseEntity::ok)`は、値が存在する場合に200 OKレスポンスに変換します。

`.orElse(ResponseEntity.notFound().build())`は、値が存在しない場合に404 Not Foundを返します。

---

## 🚀 ステップ3: アプリケーションの起動

### 3-1. ビルドと起動

```bash
./mvnw clean install
./mvnw spring-boot:run
```

**起動確認**:

コンソールに以下のようなログが出力されればOKです：

```
Started HelloSpringBootApplication in 2.345 seconds
```

---

## ✅ ステップ4: 動作確認

### 4-1. 商品を作成（POST）

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "ノートPC",
    "description": "高性能なノートパソコン",
    "price": 150000
  }'
```

**期待される結果**:

```json
{
  "id": 1,
  "name": "ノートPC",
  "description": "高性能なノートパソコン",
  "price": 150000,
  "createdAt": "2025-12-13T10:00:00.123456",
  "updatedAt": "2025-12-13T10:00:00.123456"
}
```

**ポイント**:
- `id`は自動採番される（AUTO_INCREMENT）
- `createdAt`と`updatedAt`は`@PrePersist`で自動設定される

### 4-2. さらに商品を追加

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "マウス",
    "description": "ワイヤレスマウス",
    "price": 3000
  }'

curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "キーボード",
    "description": "メカニカルキーボード",
    "price": 12000
  }'
```

### 4-3. 全商品を取得（GET）

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
    "price": 150000,
    "createdAt": "2025-12-13T10:00:00.123456",
    "updatedAt": "2025-12-13T10:00:00.123456"
  },
  {
    "id": 2,
    "name": "マウス",
    "description": "ワイヤレスマウス",
    "price": 3000,
    "createdAt": "2025-12-13T10:01:00.654321",
    "updatedAt": "2025-12-13T10:01:00.654321"
  },
  {
    "id": 3,
    "name": "キーボード",
    "description": "メカニカルキーボード",
    "price": 12000,
    "createdAt": "2025-12-13T10:02:00.987654",
    "updatedAt": "2025-12-13T10:02:00.987654"
  }
]
```

### 4-4. 特定の商品を取得（GET by ID）

```bash
curl http://localhost:8080/api/products/1
```

**期待される結果**:

```json
{
  "id": 1,
  "name": "ノートPC",
  "description": "高性能なノートパソコン",
  "price": 150000,
  "createdAt": "2025-12-13T10:00:00.123456",
  "updatedAt": "2025-12-13T10:00:00.123456"
}
```

### 4-5. 存在しないIDを取得

```bash
curl -i http://localhost:8080/api/products/999
```

**期待される結果**:

```
HTTP/1.1 404 
Content-Length: 0
```

`404 Not Found`が返却されればOKです。

### 4-6. MySQLで確認

別のターミナルで、データベースを直接確認します：

```bash
docker compose exec mysql mysql -u springuser -pspringpass hello_spring_boot -e "SELECT * FROM products;"
```

**期待される結果**:

```
+----+------------+---------------------------+--------+----------------------------+----------------------------+
| id | name       | description               | price  | created_at                 | updated_at                 |
+----+------------+---------------------------+--------+----------------------------+----------------------------+
|  1 | ノートPC    | 高性能なノートパソコン      | 150000 | 2025-12-13 10:00:00.123456 | 2025-12-13 10:00:00.123456 |
|  2 | マウス      | ワイヤレスマウス           |   3000 | 2025-12-13 10:01:00.654321 | 2025-12-13 10:01:00.654321 |
|  3 | キーボード   | メカニカルキーボード        |  12000 | 2025-12-13 10:02:00.987654 | 2025-12-13 10:02:00.987654 |
+----+------------+---------------------------+--------+----------------------------+----------------------------+
```

---

## 🎨 チャレンジ課題

基本が理解できたら、以下にチャレンジしてみましょう：

### チャレンジ 1: 価格範囲で検索

`ProductRepository`にカスタムメソッドを追加して、価格範囲で商品を検索できるようにしてください。

**ヒント**:

```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByPriceBetween(Integer minPrice, Integer maxPrice);
}
```

**Controller**:

```java
@GetMapping("/search")
public List<Product> searchByPriceRange(
        @RequestParam Integer minPrice,
        @RequestParam Integer maxPrice) {
    return productRepository.findByPriceBetween(minPrice, maxPrice);
}
```

**テスト**:

```bash
curl "http://localhost:8080/api/products/search?minPrice=5000&maxPrice=50000"
```

### チャレンジ 2: 商品名で検索（部分一致）

商品名に特定の文字列が含まれる商品を検索できるようにしてください。

**ヒント**:

```java
List<Product> findByNameContaining(String keyword);
```

**Controller**:

```java
@GetMapping("/search/name")
public List<Product> searchByName(@RequestParam String keyword) {
    return productRepository.findByNameContaining(keyword);
}
```

### チャレンジ 3: ページネーション

大量のデータを扱う場合、全件取得ではなくページネーションを実装してください。

**ヒント**:

```java
@GetMapping
public Page<Product> getAllProducts(Pageable pageable) {
    return productRepository.findAll(pageable);
}
```

**テスト**:

```bash
# 1ページ目、10件ずつ
curl "http://localhost:8080/api/products?page=0&size=10"

# 2ページ目
curl "http://localhost:8080/api/products?page=1&size=10"
```

---

## 🐛 トラブルシューティング

### エラー: "No property 'xxx' found for type 'Product'"

**原因**: リポジトリのメソッド名が命名規則に従っていない

**例**:

```java
List<Product> findByPrices(Integer price);  // ❌ Productにはpricesフィールドがない
```

**解決策**:

フィールド名と一致させる：

```java
List<Product> findByPrice(Integer price);  // ✅ Productにはpriceフィールドがある
```

### エラー: "Could not commit JPA transaction"

**原因**: データベース制約違反、またはトランザクション設定の問題

**例**:

```json
{
  "name": null,  // ❌ nameはnullable=falseなのでエラー
  "price": 1000
}
```

**解決策**:

必須フィールドを埋める、またはバリデーションを追加（次のステップで説明）。

### POSTで送信した値が保存されない

**原因**: `@RequestBody`が付いていない、またはJSON形式が間違っている

**確認ポイント**:

1. `Content-Type: application/json`ヘッダーが付いているか
2. JSONの構文が正しいか（ダブルクォート、カンマ位置など）
3. フィールド名がエンティティと一致しているか

### 日本語が文字化けする

**原因**: MySQLの文字コード設定

**解決策**:

docker-compose.ymlを確認：

```yaml
command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
```

コンテナを再作成：

```bash
docker compose down
docker compose up -d
```

---

## 🚀 ステップ5: Userエンティティの作成（演習）

これまで学んだ内容を踏まえて、`User`エンティティとリポジトリを作成してみましょう。

### 5-1. Userエンティティの作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/User.java`

```java
package com.example.hellospringboot;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @Column
    private Integer age;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

**ポイント**:
- `Product`と同じ構成のエンティティ
- `email`フィールドに`unique = true`を指定（重複を防ぐ）
- `age`はオプション（nullを許可）

---

### 5-2. UserRepositoryの作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/UserRepository.java`

```java
package com.example.hellospringboot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // メールアドレスでユーザーを検索
    Optional<User> findByEmail(String email);
    
    // 名前の一部でユーザーを検索
    List<User> findByNameContaining(String name);
    
    // 年齢範囲でユーザーを検索（カスタムクエリ）
    @Query("SELECT u FROM User u WHERE u.age >= :minAge AND u.age <= :maxAge")
    List<User> findByAgeRange(@Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge);
    
    // メールアドレスの存在確認
    boolean existsByEmail(String email);
}
```

**ポイント**:
- `findByEmail()`: Spring Data JPAのメソッド名規則で自動生成
- `findByNameContaining()`: 部分一致検索（`LIKE %name%`）
- `@Query`: JPQLで複雑なクエリを記述
- `existsByEmail()`: 存在確認（boolean型を返す）

---

### 5-3. テーブルの自動作成確認

アプリケーションを起動すると、`users`テーブルが自動作成されます。

```bash
./mvnw spring-boot:run
```

MySQLで確認：

```bash
docker compose exec mysql mysql -u springuser -pspringpass hello_spring_boot
```

```sql
DESC users;
```

**期待される結果**:

```
+------------+--------------+------+-----+---------+----------------+
| Field      | Type         | Null | Key | Default | Extra          |
+------------+--------------+------+-----+---------+----------------+
| id         | bigint       | NO   | PRI | NULL    | auto_increment |
| name       | varchar(100) | NO   |     | NULL    |                |
| email      | varchar(100) | NO   | UNI | NULL    |                |
| age        | int          | YES  |     | NULL    |                |
| created_at | datetime(6)  | NO   |     | NULL    |                |
| updated_at | datetime(6)  | NO   |     | NULL    |                |
+------------+--------------+------+-----+---------+----------------+
```

**確認ポイント**:
- ✅ `email`に`UNI`（UNIQUE制約）が付いている
- ✅ `age`が`NULL`許可（YES）になっている
- ✅ `created_at`と`updated_at`が`datetime(6)`型（マイクロ秒まで記録）

---

## 📚 このステップで学んだこと

- ✅ `JpaRepository`インターフェースの役割を理解した
- ✅ リポジトリを継承するだけでCRUD操作が自動実装されることを学んだ
- ✅ `save()`メソッドで商品を保存できるようになった
- ✅ `findAll()`メソッドで全商品を取得できるようになった
- ✅ `findById()`メソッドで特定の商品を取得できるようになった
- ✅ `Optional`型と`ResponseEntity`を使ってエラーハンドリングを実装した
- ✅ curlコマンドでPOSTリクエストとGETリクエストをテストした
- ✅ MySQLで実際にデータが保存されていることを確認した
- ✅ `User`エンティティと`UserRepository`を作成した
- ✅ カスタムクエリメソッド（`findByEmail`, `findByNameContaining`など）を実装した
- ✅ `@Query`アノテーションでJPQLを記述できるようになった

---

## 💡 補足: Spring Data JPAのクエリメソッド命名規則

### 命名規則

Spring Data JPAは、メソッド名からSQLを自動生成します。

| メソッド名 | 生成されるSQL |
|---|---|
| `findByName(String name)` | `WHERE name = ?` |
| `findByPriceLessThan(Integer price)` | `WHERE price < ?` |
| `findByPriceGreaterThanEqual(Integer price)` | `WHERE price >= ?` |
| `findByNameAndPrice(String name, Integer price)` | `WHERE name = ? AND price = ?` |
| `findByNameOrPrice(String name, Integer price)` | `WHERE name = ? OR price = ?` |
| `findByNameContaining(String keyword)` | `WHERE name LIKE %keyword%` |
| `findByNameStartingWith(String prefix)` | `WHERE name LIKE prefix%` |
| `findByNameOrderByPriceAsc(String name)` | `WHERE name = ? ORDER BY price ASC` |

### キーワード一覧

- `And`, `Or`
- `Is`, `Equals`
- `Between`
- `LessThan`, `LessThanEqual`
- `GreaterThan`, `GreaterThanEqual`
- `After`, `Before`
- `IsNull`, `IsNotNull`
- `Like`, `NotLike`
- `StartingWith`, `EndingWith`, `Containing`
- `OrderBy...Asc`, `OrderBy...Desc`
- `Not`, `In`, `NotIn`
- `True`, `False`
- `IgnoreCase`

---

## ➡️ 次のステップ

[Step 8: CRUD操作の完成](STEP_8.md)へ進みましょう！

次のステップでは、更新（Update）と削除（Delete）を実装し、CRUDの全操作を完成させます。
