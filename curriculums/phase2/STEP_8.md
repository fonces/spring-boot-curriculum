# Step 8: CRUD操作の完成

## 🎯 このステップの目標

- 商品の更新（Update）を実装できる
- 商品の削除（Delete）を実装できる
- CRUD操作の全て（Create, Read, Update, Delete）を理解できる
- HTTPメソッドとCRUD操作の対応関係を理解できる
- RESTful APIの設計原則を理解できる

**所要時間**: 約45分

---

## 📋 事前準備

- [Step 7](STEP_7.md)が完了していること
- `ProductRepository`と`ProductController`が作成されていること
- MySQLコンテナが起動していること

---

## 🧩 CRUD操作とHTTPメソッド

### RESTful APIの対応関係

**CRUD**は、データベース操作の4つの基本機能です：

| 操作 | 英語 | HTTPメソッド | エンドポイント | 説明 |
|---|---|---|---|---|
| **C**reate | 作成 | `POST` | `/api/products` | 新しいリソースを作成 |
| **R**ead | 読み取り | `GET` | `/api/products` | リソース一覧を取得 |
| **R**ead | 読み取り | `GET` | `/api/products/{id}` | 特定のリソースを取得 |
| **U**pdate | 更新 | `PUT` | `/api/products/{id}` | リソース全体を更新 |
| **U**pdate | 更新 | `PATCH` | `/api/products/{id}` | リソースの一部を更新 |
| **D**elete | 削除 | `DELETE` | `/api/products/{id}` | リソースを削除 |

### PUTとPATCHの違い

- **PUT**: リソース**全体**を置き換える（全フィールド必須）
- **PATCH**: リソースの**一部**を更新する（変更するフィールドのみ）

このステップでは、より一般的な`PUT`を実装します。

---

## 🚀 ステップ1: 更新（Update）の実装

### 1-1. ProductControllerに更新メソッドを追加

`ProductController.java`に以下のメソッドを**追加**します：

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
    
    // ここまでは既存のメソッド（Step 7で作成済み）
    
    // 商品を更新
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @RequestBody Product productDetails) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setName(productDetails.getName());
                    product.setDescription(productDetails.getDescription());
                    product.setPrice(productDetails.getPrice());
                    Product updated = productRepository.save(product);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
```

### 1-2. コードの解説

#### 更新の流れ

1. `findById(id)`で既存のデータを取得
2. `.map(product -> { ... })`で値が存在する場合の処理
3. セッターで新しい値を設定
4. `save()`で保存（`@PreUpdate`が実行され`updatedAt`が更新される）
5. 200 OKで返却

#### なぜ`save()`で更新できるのか

`save()`メソッドは以下のロジックで動作します：

```
IDが設定されていない → INSERT
IDが設定されている → UPDATE（既存レコードを更新）
```

---

## 🚀 ステップ2: 削除（Delete）の実装

### 2-1. ProductControllerに削除メソッドを追加

`ProductController.java`に以下のメソッドを**追加**します：

```java
    // 商品を削除
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(product -> {
                    productRepository.delete(product);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
```

### 2-2. コードの解説

#### `ResponseEntity<Void>`

削除成功時は本文（body）を返す必要がないため、`Void`を指定します。

#### `ResponseEntity.noContent().build()`

削除成功時は`204 No Content`を返すのが一般的です。

- `200 OK`: 削除したリソースの情報を返す場合
- `204 No Content`: 削除のみで本文を返さない場合（推奨）

#### 削除の方法

`JpaRepository`には2つの削除メソッドがあります：

```java
productRepository.delete(product);      // エンティティを渡す
productRepository.deleteById(id);       // IDを渡す
```

今回は「存在確認→削除」の流れのため、`delete()`を使用しています。

---

## ✅ ステップ3: 動作確認

### 3-1. アプリケーションの再起動

```bash
./mvnw clean install
./mvnw spring-boot:run
```

### 3-2. テストデータの作成

まず、テスト用の商品を作成します：

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "テストノートPC",
    "description": "更新・削除のテスト用",
    "price": 100000
  }'
```

**期待される結果**:

```json
{
  "id": 1,
  "name": "テストノートPC",
  "description": "更新・削除のテスト用",
  "price": 100000,
  "createdAt": "2025-12-13T11:00:00.123456",
  "updatedAt": "2025-12-13T11:00:00.123456"
}
```

**重要**: 返却された`id`をメモしてください（以下の例では`id=1`として進めます）。

### 3-3. 商品を更新（PUT）

```bash
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "更新後ノートPC",
    "description": "価格を値下げしました",
    "price": 80000
  }'
```

**期待される結果**:

```json
{
  "id": 1,
  "name": "更新後ノートPC",
  "description": "価格を値下げしました",
  "price": 80000,
  "createdAt": "2025-12-13T11:00:00.123456",
  "updatedAt": "2025-12-13T11:05:00.654321"
}
```

**ポイント**:
- `createdAt`は変わらない（`updatable = false`のため）
- `updatedAt`は現在時刻に更新される（`@PreUpdate`のおかげ）

### 3-4. 更新内容の確認（GET）

```bash
curl http://localhost:8080/api/products/1
```

更新が反映されていることを確認してください。

### 3-5. MySQLで確認

```bash
docker compose exec mysql mysql -u springuser -pspringpass hello_spring_boot \
  -e "SELECT * FROM products WHERE id = 1;"
```

`updated_at`が`created_at`より新しくなっていることを確認してください。

### 3-6. 商品を削除（DELETE）

```bash
curl -i -X DELETE http://localhost:8080/api/products/1
```

**期待される結果**:

```
HTTP/1.1 204 
Date: Fri, 13 Dec 2025 11:10:00 GMT
```

`204 No Content`が返却されればOKです。

### 3-7. 削除の確認（GET）

```bash
curl -i http://localhost:8080/api/products/1
```

**期待される結果**:

```
HTTP/1.1 404 
Content-Length: 0
```

`404 Not Found`が返却されればOKです。

### 3-8. 存在しない商品の更新

```bash
curl -i -X PUT http://localhost:8080/api/products/999 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "存在しない商品",
    "description": "test",
    "price": 1000
  }'
```

**期待される結果**:

```
HTTP/1.1 404 
Content-Length: 0
```

### 3-9. 存在しない商品の削除

```bash
curl -i -X DELETE http://localhost:8080/api/products/999
```

**期待される結果**:

```
HTTP/1.1 404 
Content-Length: 0
```

---

## 🚀 ステップ4: 完全なCRUDの確認

全ての操作をまとめて確認しましょう。

### 4-1. Create（作成）

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "マウス",
    "description": "ワイヤレスマウス",
    "price": 3000
  }'
```

### 4-2. Read（読み取り - 全件）

```bash
curl http://localhost:8080/api/products
```

### 4-3. Read（読み取り - 単一）

```bash
curl http://localhost:8080/api/products/2
```

### 4-4. Update（更新）

```bash
curl -X PUT http://localhost:8080/api/products/2 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "マウス",
    "description": "ワイヤレスマウス（値下げ！）",
    "price": 2500
  }'
```

### 4-5. Delete（削除）

```bash
curl -X DELETE http://localhost:8080/api/products/2
```

---

## 🎨 チャレンジ課題

基本が理解できたら、以下にチャレンジしてみましょう：

### チャレンジ 1: PATCH（部分更新）の実装

全フィールドではなく、変更があったフィールドのみ更新できるようにしてください。

**ヒント**:

```java
@PatchMapping("/{id}")
public ResponseEntity<Product> patchProduct(
        @PathVariable Long id,
        @RequestBody Map<String, Object> updates) {
    return productRepository.findById(id)
            .map(product -> {
                updates.forEach((key, value) -> {
                    switch (key) {
                        case "name" -> product.setName((String) value);
                        case "description" -> product.setName((String) value);
                        case "price" -> product.setPrice((Integer) value);
                    }
                });
                Product updated = productRepository.save(product);
                return ResponseEntity.ok(updated);
            })
            .orElse(ResponseEntity.notFound().build());
}
```

**テスト**:

```bash
# 価格のみ更新
curl -X PATCH http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{"price": 90000}'
```

### チャレンジ 2: 一括削除

複数の商品をまとめて削除できるようにしてください。

**ヒント**:

```java
@DeleteMapping
public ResponseEntity<Void> deleteProducts(@RequestBody List<Long> ids) {
    productRepository.deleteAllById(ids);
    return ResponseEntity.noContent().build();
}
```

**テスト**:

```bash
curl -X DELETE http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '[1, 2, 3]'
```

### チャレンジ 3: 論理削除

物理削除ではなく、`deleted`フラグで論理削除を実装してください。

**手順**:

1. `Product`エンティティに`deleted`フィールドを追加

```java
@Column(nullable = false)
private Boolean deleted = false;
```

2. 削除時は`deleted = true`に更新

```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
    return productRepository.findById(id)
            .map(product -> {
                product.setDeleted(true);
                productRepository.save(product);
                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
}
```

3. 取得時は`deleted = false`のものだけ取得

```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByDeletedFalse();
}
```

---

## 🐛 トラブルシューティング

### PUT/DELETEが405 Method Not Allowedになる

**原因**: リバースプロキシやファイアウォールがPUT/DELETEを制限している

**確認**:

```bash
curl -i -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{}'
```

**解決策**:

ローカル環境（localhost）では通常問題ありませんが、デプロイ先で発生する場合は、サーバー設定を確認してください。

### 更新しても`updatedAt`が変わらない

**原因**: `@PreUpdate`が実行されていない

**確認ポイント**:

1. `@PreUpdate`メソッドが`protected`または`public`であるか
2. エンティティが`@Entity`アノテーションを持っているか
3. 実際にフィールドの値が変更されているか（同じ値なら更新されない）

### 削除後にGETすると404ではなく200が返る

**原因**: キャッシュが有効になっている、またはコードが正しく実装されていない

**解決策**:

`findById()`の実装を確認:

```java
return productRepository.findById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
```

### PUTで一部フィールドを省略するとnullになる

**原因**: PUTは全フィールド必須

**解決策**:

1. 全フィールドを送信する
2. PATCH（部分更新）を実装する（チャレンジ課題参照）

---

## 📚 このステップで学んだこと

- ✅ 商品の更新（Update）を実装した
- ✅ 商品の削除（Delete）を実装した
- ✅ CRUD操作の全て（Create, Read, Update, Delete）を完成させた
- ✅ HTTPメソッド（POST, GET, PUT, DELETE）とCRUD操作の対応を理解した
- ✅ `save()`が新規作成と更新の両方に使えることを学んだ
- ✅ `@PreUpdate`で更新時の処理を自動実行できることを学んだ
- ✅ RESTful APIの基本的な設計パターンを理解した
- ✅ 適切なHTTPステータスコード（200, 204, 404）を返せるようになった

---

## 💡 補足: RESTful APIのベストプラクティス

### HTTPステータスコードの使い分け

| コード | 名前 | 使用場面 |
|---|---|---|
| `200 OK` | 成功 | GET, PUT, PATCHの成功時 |
| `201 Created` | 作成成功 | POSTでリソース作成成功時 |
| `204 No Content` | 成功（本文なし） | DELETE成功時 |
| `400 Bad Request` | 不正なリクエスト | バリデーションエラー |
| `404 Not Found` | 見つからない | リソースが存在しない |
| `409 Conflict` | 競合 | 一意制約違反など |
| `500 Internal Server Error` | サーバーエラー | 予期しないエラー |

### エンドポイント設計の原則

1. **リソース指向**: URLはリソース（名詞）を表す
   - ✅ `/api/products`
   - ❌ `/api/getProducts`

2. **複数形を使用**: コレクションは複数形
   - ✅ `/api/products`
   - ❌ `/api/product`

3. **階層構造**: 関連リソースは階層で表現
   - ✅ `/api/products/1/reviews`
   - ❌ `/api/product-reviews?productId=1`

4. **動詞を避ける**: HTTPメソッドで操作を表現
   - ✅ `POST /api/products`
   - ❌ `POST /api/products/create`

---

## ➡️ 次のステップ

[Step 9: @Transactionalでトランザクション管理](STEP_9.md)へ進みましょう！

次のステップでは、複数のデータベース操作をまとめてロールバックできるトランザクション管理を学びます。
