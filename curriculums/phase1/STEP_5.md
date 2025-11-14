# Step 5: Lombokで簡潔なコード

## 🎯 このステップの目標

- Lombokとは何か、なぜ使うのかを理解する
- Lombokの依存関係を追加してIDEにプラグインをインストールする
- `@Data`、`@Getter`、`@Setter`でGetter/Setterを自動生成する
- `@AllArgsConstructor`、`@NoArgsConstructor`でコンストラクタを自動生成する
- `@Builder`でビルダーパターンを実装する
- Step 3で作成したDTOをLombokでリファクタリングする

**所要時間**: 約1時間

---

## 📋 事前準備

- Step 4で作成した`hello-spring-boot`プロジェクト
- Step 3で作成したDTOクラス（`UserRequest`, `UserResponse`）

**Step 4をまだ完了していない場合**: [Step 4: application.ymlで設定管理](STEP_4.md)を先に進めてください。

---

## 💡 Lombokとは？

### Lombokの役割

**Lombok** = ボイラープレートコード（定型的な冗長なコード）を自動生成するライブラリ

**ボイラープレートコードの例**:
- Getter/Setterメソッド
- コンストラクタ
- `toString()`、`equals()`、`hashCode()`

### Lombokなし vs Lombokあり

#### Lombokなし（Step 3で書いたコード）

```java
public class UserRequest {
    private String name;
    private String email;
    private Integer age;

    public UserRequest() {
    }

    public UserRequest(String name, String email, Integer age) {
        this.name = name;
        this.email = email;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}
// 約40行
```

#### Lombokあり

```java
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {
    private String name;
    private String email;
    private Integer age;
}
// たったの12行！
```

**同じ機能を、1/3以下のコードで実現できます。**

---

## 🚀 ステップ1: Lombok依存関係の追加

### 1-1. pom.xmlに依存関係を追加

`pom.xml`を開いて、`<dependencies>`セクション内に以下を追加します。

**ファイルパス**: `pom.xml`

```xml
<dependencies>
    <!-- 既存の依存関係 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Lombokを追加 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- テスト用（既存） -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 1-2. Mavenプロジェクトの更新

VSCodeで：
1. `pom.xml`を保存
2. 右下に表示される「A build file was modified. Do you want to synchronize the Java classpath/configuration?」で「Always」をクリック
3. または、コマンドパレット（`Ctrl + Shift + P`）で「Java: Clean Java Language Server Workspace」を実行

依存関係がダウンロードされるまで待ちます。

---

## 🚀 ステップ2: VSCodeにLombok拡張機能をインストール

### 2-1. 拡張機能のインストール

1. VSCodeの拡張機能ビュー（サイドバーの四角4つのアイコン）を開く
2. 検索ボックスに「Lombok」と入力
3. 「Lombok Annotations Support for VS Code」（GabrielBB作）をインストール
4. VSCodeを再起動（推奨）

### 2-2. 設定の確認

Lombokが正しく動作するように設定を確認します：

1. `Ctrl + ,`（macOSは`⌘,`）で設定を開く
2. 検索ボックスに「java.compile.nullAnalysis.mode」と入力
3. 値が「automatic」になっていることを確認

**これでVSCodeがLombokのアノテーションを認識します！**

---

## 🚀 ステップ3: @Dataでリファクタリング

### 3-1. UserRequestをLombok化

Step 3で作成した`UserRequest.java`を以下のように**書き換え**ます。

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/UserRequest.java`

```java
package com.example.hellospringboot.dto;

import lombok.Data;

@Data
public class UserRequest {
    private String name;
    private String email;
    private Integer age;
}
```

### 3-2. @Dataの機能

`@Data`は以下を自動生成します：

- すべてのフィールドの**Getter**
- すべてのフィールドの**Setter**
- `toString()`
- `equals()`と`hashCode()`
- **引数を持つコンストラクタ**（全フィールド）

### 3-3. 動作確認

アプリケーションを再起動して、POSTリクエストを送信：

```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Lombok User","email":"lombok@example.com","age":30}'
```

**期待される結果**:
```json
{
  "id": 1,
  "name": "Lombok User",
  "email": "lombok@example.com",
  "age": 30,
  "createdAt": "2025-10-27 15:30:45"
}
```

Getter/Setterを書かなくても動作します！

---

## 🚀 ステップ4: @NoArgsConstructorと@AllArgsConstructor

### 4-1. 問題: デフォルトコンストラクタがない

`@Data`だけでは、引数なしのデフォルトコンストラクタが生成されません。
JSON → Javaオブジェクトの変換時にエラーになる場合があります。

### 4-2. コンストラクタアノテーションの追加

`UserRequest.java`を修正：

```java
package com.example.hellospringboot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor   // 引数なしコンストラクタ
@AllArgsConstructor  // 全フィールドを引数に持つコンストラクタ
public class UserRequest {
    private String name;
    private String email;
    private Integer age;
}
```

### 4-3. コンストラクタアノテーションの解説

#### `@NoArgsConstructor`
```java
public UserRequest() {
}
```
を自動生成

#### `@AllArgsConstructor`
```java
public UserRequest(String name, String email, Integer age) {
    this.name = name;
    this.email = email;
    this.age = age;
}
```
を自動生成

---

## 🚀 ステップ5: UserResponseもLombok化

### 5-1. UserResponseの書き換え

`UserResponse.java`も同様にリファクタリング：

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/UserResponse.java`

```java
package com.example.hellospringboot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private Integer age;
    private String createdAt;
}
```

**約60行 → 約15行になりました！**

### 5-2. 動作確認

```bash
# ユーザー作成
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Hanako","email":"hanako@example.com","age":25}'

# 一覧取得
curl http://localhost:8080/users
```

問題なく動作するはずです。

---

## 🚀 ステップ6: @Builderパターン

### 6-1. Builderパターンとは？

オブジェクトを段階的に構築するデザインパターン。

**通常の方法**:
```java
UserResponse response = new UserResponse();
response.setId(1L);
response.setName("Taro");
response.setEmail("taro@example.com");
response.setAge(30);
response.setCreatedAt("2025-10-27 15:30:45");
```

**Builderパターン**:
```java
UserResponse response = UserResponse.builder()
    .id(1L)
    .name("Taro")
    .email("taro@example.com")
    .age(30)
    .createdAt("2025-10-27 15:30:45")
    .build();
```

**メリット**:
- 可読性が高い
- 設定し忘れを防げる
- 不変オブジェクトを作りやすい

### 6-2. @Builderの追加

`UserResponse.java`に`@Builder`を追加：

```java
package com.example.hellospringboot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private Integer age;
    private String createdAt;
}
```

### 6-3. Controllerでの使用

`UserController.java`を修正：

```java
@PostMapping("/users")
public UserResponse createUser(@RequestBody UserRequest request) {
    Long id = counter.getAndIncrement();
    
    String createdAt = LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    
    // Builderパターンで構築
    UserResponse response = UserResponse.builder()
        .id(id)
        .name(request.getName())
        .email(request.getEmail())
        .age(request.getAge())
        .createdAt(createdAt)
        .build();
    
    users.add(response);
    
    return response;
}
```

### 6-4. 動作確認

```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Builder User","email":"builder@example.com","age":28}'
```

同じように動作しますが、コードがより読みやすくなりました。

---

## 🚀 ステップ7: その他の便利なLombokアノテーション

### 7-1. @Getter / @Setterの個別使用

`@Data`はすべてを生成しますが、個別に制御したい場合：

```java
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Product {
    private String name;
    private Integer price;
    
    @Setter(lombok.AccessLevel.NONE)  // Setterを生成しない
    private Long id;
}
```

### 7-2. @ToString

`@ToString`は、オブジェクトの内容を文字列で表現する`toString()`メソッドを自動生成します。

#### 基本的な使い方

```java
import lombok.ToString;

@ToString
public class User {
    private String name;
    private String email;
}
```

このクラスのインスタンスを出力すると：

```java
User user = new User();
user.setName("Taro");
user.setEmail("taro@example.com");

System.out.println(user);
// 出力: User(name=Taro, email=taro@example.com)
```

<details>
<summary><strong>📖 主な用途: ログ出力でのデバッグ（クリックして展開）</strong></summary>

`@ToString`は**ログ出力**で非常に便利です。オブジェクトの状態を簡単に確認できます：

```java
@RestController
@Slf4j  // Lombokのログアノテーション
public class UserController {
    
    @PostMapping("/users")
    public UserResponse createUser(@RequestBody UserRequest request) {
        // リクエスト内容をログ出力（デバッグ用）
        log.info("Received user request: {}", request);
        
        // ... 処理 ...
        
        return response;
    }
}
```

**ログ出力例**:
```
2025-11-14 10:30:45.123  INFO 12345 --- [nio-8080-exec-1] c.e.h.controller.UserController : Received user request: UserRequest(name=Taro, email=taro@example.com, age=30)
```

`@ToString`がなければ、以下のように表示されてしまいます：
```
Received user request: com.example.hellospringboot.dto.UserRequest@5a2e4553
```

#### ⚠️ 重要: 機密情報の除外

**パスワードやトークンなどの機密情報は、ログに出力してはいけません！**

`exclude`パラメータで特定フィールドを除外できます：

```java
import lombok.ToString;

@ToString(exclude = {"password", "creditCardNumber"})
public class UserRegistration {
    private String username;
    private String email;
    private String password;           // ログに出力されない
    private String creditCardNumber;   // ログに出力されない
}
```

**出力例**:
```java
UserRegistration user = new UserRegistration();
user.setUsername("taro");
user.setEmail("taro@example.com");
user.setPassword("secret123");
user.setCreditCardNumber("1234-5678-9012-3456");

System.out.println(user);
// 出力: UserRegistration(username=taro, email=taro@example.com)
// passwordとcreditCardNumberは出力されない
```

#### 💡 セキュリティのベストプラクティス

**機密情報を含むクラスの例**:

```java
@ToString(exclude = {"password", "token", "secret"})
@Data
public class LoginRequest {
    private String username;
    private String password;  // 絶対にログに出さない！
    private String deviceId;
}
```

```java
@ToString(exclude = "apiKey")
@Data
public class ExternalApiConfig {
    private String endpoint;
    private String apiKey;    // APIキーは機密情報
    private Integer timeout;
}
```

#### その他の便利なオプション

**特定フィールドのみを含める**:

```java
@ToString(of = {"id", "name"})  // idとnameだけ出力
public class User {
    private Long id;
    private String name;
    private String email;
    private String internalNote;  // 出力されない
}
```

**スーパークラスのフィールドも含める**:

```java
@ToString(callSuper = true)
public class AdminUser extends User {
    private String role;
}
// 出力: AdminUser(super=User(name=Taro), role=ADMIN)
```

**フィールド名を表示しない**:

```java
@ToString(includeFieldNames = false)
public class Point {
    private Integer x;
    private Integer y;
}
// 出力: Point(10, 20) ← フィールド名なし
```

#### 実践例: ログ出力での活用

```java
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class OrderController {
    
    @PostMapping("/orders")
    public OrderResponse createOrder(@RequestBody OrderRequest request) {
        // リクエストの内容をログに記録
        log.info("Creating order: {}", request);
        
        try {
            // 処理...
            OrderResponse response = processOrder(request);
            
            // 成功時のログ
            log.info("Order created successfully: {}", response);
            
            return response;
        } catch (Exception e) {
            // エラー時にリクエスト内容を記録（デバッグに有用）
            log.error("Failed to create order. Request: {}", request, e);
            throw e;
        }
    }
}

@ToString(exclude = {"creditCardNumber", "cvv"})  // 決済情報は除外！
@Data
class OrderRequest {
    private String productId;
    private Integer quantity;
    private String creditCardNumber;  // ログに出力しない
    private String cvv;               // ログに出力しない
}

@ToString
@Data
class OrderResponse {
    private String orderId;
    private String status;
    private LocalDateTime createdAt;
}
```

**重要ポイント**:
- ✅ ログ出力でオブジェクトの状態を簡単に確認できる
- ✅ デバッグ作業が効率的になる
- ⚠️ パスワード、トークン、カード番号などは必ず`exclude`で除外
- ⚠️ 個人情報（メールアドレス、電話番号等）の取り扱いにも注意

</details>

### 7-3. @EqualsAndHashCode

```java
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(of = "id")
public class User {
    private Long id;
    private String name;
}
// IDが同じなら同じオブジェクトと判定
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: ProductDTOの作成

商品を扱うDTOをLombokで作成してください。

**要件**:
- `ProductRequest`: name, price, category
- `ProductResponse`: id, name, price, category, createdAt
- すべてLombokを使用（`@Data`, `@Builder`など）

### チャレンジ 2: 不変オブジェクトの作成

`@Builder`と`@Getter`のみを使用して、変更不可能なレスポンスDTOを作成してください。

**ヒント**:
```java
@Getter
@Builder
public class ImmutableResponse {
    private final Long id;
    private final String name;
}
```

### チャレンジ 3: カスタムビルダー

デフォルト値を持つBuilderを作成してください。

**ヒント**:
```java
@Builder
public class UserRequest {
    private String name;
    
    @Builder.Default
    private String role = "USER";
    
    @Builder.Default
    private Boolean active = true;
}
```

---

## 🐛 トラブルシューティング

### Lombokアノテーションが認識されない

**症状**: `@Data`などに赤い波線が出る

**原因**: Lombokプラグインがインストールされていない

**解決策**:
1. ステップ2を再確認
**解決策**:
1. `pom.xml`からLombokの依存関係を削除
2. VSCodeを再起動
3. `pom.xml`のLombok依存関係を確認

### Getter/Setterが見つからないエラー

**エラー**: "Cannot resolve method 'getName()'"

**原因**: Lombokの拡張機能がインストールされていない、または認識されていない

**解決策**:
1. VSCodeを再起動
2. 「Lombok Annotations Support for VS Code」拡張機能がインストール済みか確認
3. コマンドパレットで「Java: Clean Java Language Server Workspace」を実行
4. プロジェクトを再度開く

### @Builderと@NoArgsConstructorの競合

**エラー**: コンストラクタエラー

**解決策**: 3つセットで使用
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyClass {
    // ...
}
```

### JSONへの変換で値がnull

**原因**: Getterが生成されていない

**解決策**:
- `@Data`または`@Getter`が付いているか確認
- クラス名、フィールド名を確認（typoがないか）

---

## 📚 このステップで学んだこと

- ✅ Lombokの役割と利点
- ✅ Lombok依存関係の追加とプラグインインストール
- ✅ `@Data`でGetter/Setter/toString等を自動生成
- ✅ `@NoArgsConstructor`でデフォルトコンストラクタ生成
- ✅ `@AllArgsConstructor`で全フィールドコンストラクタ生成
- ✅ `@Builder`でビルダーパターン実装
- ✅ DTOクラスのリファクタリング
- ✅ コード量の大幅削減（約70%減）

---

## 💡 補足: Lombokの使用上の注意点

### メリット

- ✅ **コード量削減**: ボイラープレートコードを削減
- ✅ **保守性向上**: フィールド追加時にGetter/Setter追記不要
- ✅ **可読性向上**: 本質的なコードに集中できる

### デメリット・注意点

- ❌ **Lombok依存**: プロジェクトがLombokに依存
- ❌ **学習コスト**: チーム全員がLombokを理解する必要
- ❌ **デバッグ**: 生成されたコードが見えない
- ❌ **IDE依存**: IDEプラグインが必要

### 使い分けのガイドライン

#### Lombokを使うべき場面
- DTO/Entity（データクラス）
- 単純なPOJO（Plain Old Java Object）
- 内部的なモデルクラス

#### Lombokを避けるべき場面
- 複雑なビジネスロジックを持つクラス
- 公開APIのモデル（外部ライブラリ等）
- カスタムGetter/Setterが必要な場合

### Spring Bootでの推奨設定

```java
// DTOやEntityでの標準的な使い方
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long id;
    private String name;
    private String email;
}
```

---

## 🔄 Gitへのコミットとレビュー依頼

進捗を記録してレビューを受けましょう：

```bash
git add .
git commit -m "Step 5: LombokでDTOをリファクタリング完了"
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

**🎉 Phase 1 完了おめでとうございます！**

Phase 1の5つのステップをすべて完了しました！

レビューが完了したら、**Phase 2: データベース連携**へ進みましょう！

[Step 6: MySQLとの接続](../phase2/STEP_6.md)で、データベース連携の基礎を学びます。

---

### Phase 1で学んだこと

- ✅ Spring Bootプロジェクトの作成
- ✅ REST APIの基本（GET/POST）
- ✅ パスパラメータとクエリパラメータ
- ✅ リクエストボディとDTO
- ✅ 設定ファイル（application.yml）
- ✅ Lombokによるコード簡略化

---

お疲れさまでした！ 🚀

Lombokを使いこなせるようになると、Spring Boot開発が劇的に快適になります。
次のPhaseでさらに実践的なスキルを身につけていきましょう！
