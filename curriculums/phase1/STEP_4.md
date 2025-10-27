# Step 4: application.ymlで設定管理

## 🎯 このステップの目標

- `application.yml`の基本的な書き方を理解する
- サーバーポート番号を変更できるようになる
- カスタムプロパティを定義して使用する
- `@Value`アノテーションでプロパティを読み込む
- 環境ごとに異なる設定を管理する基礎を学ぶ

**所要時間**: 約45分

---

## 📋 事前準備

- Step 3で作成した`hello-spring-boot`プロジェクト
- application.propertiesとapplication.ymlの違いを理解する準備

**Step 3をまだ完了していない場合**: [Step 3: POSTリクエストとリクエストボディ](STEP_3.md)を先に進めてください。

---

## 💡 設定ファイルとは？

Spring Bootでは、アプリケーションの設定を外部ファイルで管理できます。

### 設定ファイルの種類

| ファイル | 形式 | 特徴 |
|---------|------|------|
| `application.properties` | key=value形式 | シンプル、フラットな構造 |
| `application.yml` | YAML形式 | 階層構造、読みやすい |

**このカリキュラムではYML形式を採用します。**

---

## 🚀 ステップ1: application.propertiesからapplication.ymlへ変換

### 1-1. 現在のファイルを確認

プロジェクト作成時に自動生成された`application.properties`を確認します。

**ファイルパス**: `src/main/resources/application.properties`

おそらく空、または以下のような内容です：

```properties
# 空ファイル
```

### 1-2. application.ymlを作成

`application.properties`を削除（またはリネーム）して、`application.yml`を作成します。

**ファイルパス**: `src/main/resources/application.yml`

まずは空のファイルを作成してください。

---

## 🚀 ステップ2: サーバーポート番号の変更

### 2-1. デフォルトポート確認

現在、アプリケーションはポート8080で起動しています。
これを8081に変更してみましょう。

### 2-2. application.ymlに設定を追加

`application.yml`に以下を追加：

```yaml
server:
  port: 8081
```

### 2-3. YAMLの書き方のポイント

- **インデント**: スペース2つ（タブは不可）
- **コロンの後にスペース**: `port: 8081`（`port:8081`はNG）
- **階層構造**: インデントで表現

### 2-4. 動作確認

アプリケーションを再起動すると、ポート8081で起動します。

```bash
# ログで確認
Tomcat started on port 8081 (http)
```

curlで確認：

```bash
# 8080はもう使えない
curl http://localhost:8080/hello
# → Connection refused

# 8081で動作
curl http://localhost:8081/hello
# → Hello, Spring Boot!
```

**元に戻す場合**: `port: 8080`に変更するか、設定自体を削除

---

## 🚀 ステップ3: カスタムプロパティの定義

### 3-1. アプリケーション情報を定義

`application.yml`に以下を追加：

```yaml
server:
  port: 8080

app:
  name: Hello Spring Boot Application
  version: 1.0.0
  description: Spring Bootを学ぶためのサンプルアプリケーション
```

### 3-2. YAMLの階層構造

これは以下のような構造を表しています：

```
app:
  ├── name
  ├── version
  └── description
```

**properties形式だと**:
```properties
app.name=Hello Spring Boot Application
app.version=1.0.0
app.description=Spring Bootを学ぶためのサンプルアプリケーション
```

YAMLの方が見やすく、階層構造が明確です。

---

## 🚀 ステップ4: @Valueでプロパティを読み込む

### 4-1. AppInfoControllerの作成

カスタムプロパティを使用するコントローラーを作成します。

**ファイルパス**: `src/main/java/com/example/hellospringboot/controller/AppInfoController.java`

```java
package com.example.hellospringboot.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class AppInfoController {

    @Value("${app.name}")
    private String appName;

    @Value("${app.version}")
    private String appVersion;

    @Value("${app.description}")
    private String appDescription;

    @GetMapping("/info")
    public Map<String, String> getAppInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("name", appName);
        info.put("version", appVersion);
        info.put("description", appDescription);
        return info;
    }
}
```

### 4-2. コードの解説

#### `@Value("${app.name}")`
- `application.yml`の値を読み込むアノテーション
- `${プロパティ名}`の形式で指定
- フィールドに自動的に値が注入される

#### プロパティの参照
- `app.name` → `${app.name}`
- ドット（`.`）で階層を表現

### 4-3. 動作確認

```bash
curl http://localhost:8080/info
```

**期待される結果**:
```json
{
  "name": "Hello Spring Boot Application",
  "version": "1.0.0",
  "description": "Spring Bootを学ぶためのサンプルアプリケーション"
}
```

---

## 🚀 ステップ5: デフォルト値の設定

### 5-1. デフォルト値の指定

プロパティが定義されていない場合のデフォルト値を設定できます。

`AppInfoController.java`に以下を追加：

```java
@Value("${app.admin-email:admin@example.com}")
private String adminEmail;

@GetMapping("/info")
public Map<String, String> getAppInfo() {
    Map<String, String> info = new HashMap<>();
    info.put("name", appName);
    info.put("version", appVersion);
    info.put("description", appDescription);
    info.put("adminEmail", adminEmail);  // 追加
    return info;
}
```

### 5-2. コードの解説

#### `${app.admin-email:admin@example.com}`
- コロン（`:`）の後がデフォルト値
- `app.admin-email`が定義されていなければ`admin@example.com`を使用

### 5-3. 動作確認

```bash
curl http://localhost:8080/info
```

**期待される結果**（`admin-email`は定義していないのでデフォルト値）:
```json
{
  "name": "Hello Spring Boot Application",
  "version": "1.0.0",
  "description": "Spring Bootを学ぶためのサンプルアプリケーション",
  "adminEmail": "admin@example.com"
}
```

`application.yml`に追加してみましょう：

```yaml
app:
  name: Hello Spring Boot Application
  version: 1.0.0
  description: Spring Bootを学ぶためのサンプルアプリケーション
  admin-email: contact@myapp.com
```

再起動後：

```json
{
  ...
  "adminEmail": "contact@myapp.com"
}
```

---

## 🚀 ステップ6: 複雑なプロパティ構造

### 6-1. ネストした設定

`application.yml`に以下を追加：

```yaml
server:
  port: 8080

app:
  name: Hello Spring Boot Application
  version: 1.0.0
  description: Spring Bootを学ぶためのサンプルアプリケーション
  admin-email: contact@myapp.com
  
  features:
    user-registration: true
    email-notification: false
    max-upload-size: 10485760  # 10MB in bytes
```

### 6-2. ネストしたプロパティの読み込み

```java
@Value("${app.features.user-registration}")
private boolean userRegistrationEnabled;

@Value("${app.features.email-notification}")
private boolean emailNotificationEnabled;

@Value("${app.features.max-upload-size}")
private long maxUploadSize;

@GetMapping("/features")
public Map<String, Object> getFeatures() {
    Map<String, Object> features = new HashMap<>();
    features.put("userRegistration", userRegistrationEnabled);
    features.put("emailNotification", emailNotificationEnabled);
    features.put("maxUploadSize", maxUploadSize);
    return features;
}
```

### 6-3. 動作確認

```bash
curl http://localhost:8080/features
```

**期待される結果**:
```json
{
  "userRegistration": true,
  "emailNotification": false,
  "maxUploadSize": 10485760
}
```

---

## 🚀 ステップ7: よく使うSpring Bootプロパティ

### 7-1. 便利な設定例

`application.yml`でよく使う設定：

```yaml
server:
  port: 8080
  servlet:
    context-path: /api  # アプリケーションのベースパス

spring:
  application:
    name: hello-spring-boot  # アプリケーション名
  
  # JSON設定
  jackson:
    property-naming-strategy: SNAKE_CASE  # JSONのキー名をスネークケースに
    serialization:
      indent-output: true  # JSONを整形して出力

logging:
  level:
    root: INFO
    com.example.hellospringboot: DEBUG  # 自分のパッケージはDEBUGレベル
```

### 7-2. context-pathの確認

`context-path`を設定すると、すべてのエンドポイントの前に`/api`が付きます。

```yaml
server:
  servlet:
    context-path: /api
```

再起動後：

```bash
# /helloではアクセスできない
curl http://localhost:8080/hello
# → 404 Not Found

# /api/helloでアクセス
curl http://localhost:8080/api/hello
# → Hello, Spring Boot!
```

**学習中は設定しない方が良いです**（混乱を避けるため）。

---

## 🎨 チャレンジ課題

### チャレンジ 1: ウェルカムメッセージの国際化

`application.yml`に複数言語の挨拶メッセージを定義し、言語パラメータで切り替えるAPIを作成してください。

**ヒント**:
```yaml
app:
  messages:
    welcome-ja: ようこそ
    welcome-en: Welcome
    welcome-es: Bienvenido
```

### チャレンジ 2: 環境情報API

以下の情報を返すエンドポイント`/env`を作成してください：
- Javaバージョン（`System.getProperty("java.version")`）
- OSの名前（`System.getProperty("os.name")`）
- アプリケーション名（`@Value`で取得）

### チャレンジ 3: 機能フラグの実装

`app.features.user-registration`が`true`のときだけユーザー登録を許可するロジックを実装してください。

**ヒント**:
```java
@PostMapping("/users")
public ResponseEntity<?> createUser(@RequestBody UserRequest request) {
    if (!userRegistrationEnabled) {
        return ResponseEntity.status(403)
            .body("User registration is disabled");
    }
    // 登録処理
}
```

---

## 🐛 トラブルシューティング

### エラー: "Could not resolve placeholder"

**エラーメッセージ**:
```
Could not resolve placeholder 'app.name' in value "${app.name}"
```

**原因**: `application.yml`にプロパティが定義されていない

**解決策**:
1. `application.yml`に該当プロパティを追加
2. またはデフォルト値を設定: `@Value("${app.name:Default Name}")`

### YAMLのパースエラー

**原因**: YAMLの書き方が間違っている

**よくある間違い**:
```yaml
# NG: コロンの後にスペースがない
app:
  name:Hello Spring Boot

# NG: タブを使用
app:
	name: Hello Spring Boot

# OK: スペース2つ、コロンの後にスペース
app:
  name: Hello Spring Boot
```

### 設定が反映されない

**原因**: アプリケーションを再起動していない

**解決策**:
- `application.yml`を変更したら必ず再起動
- IntelliJ IDEAの再起動ボタン（緑の矢印）をクリック

### プロパティ名の大文字小文字

Spring Bootは以下をすべて同じものとして扱います：

```yaml
app.user-name: Taro      # ケバブケース（推奨）
app.userName: Taro       # キャメルケース
app.user_name: Taro      # スネークケース
```

**推奨**: ケバブケース（`user-name`）を使用

---

## 📚 このステップで学んだこと

- ✅ `application.yml`の基本的な書き方
- ✅ YAMLの階層構造とインデントルール
- ✅ サーバーポート番号の変更
- ✅ カスタムプロパティの定義
- ✅ `@Value`アノテーションでプロパティ読み込み
- ✅ デフォルト値の設定
- ✅ ネストしたプロパティの扱い方
- ✅ よく使うSpring Boot標準プロパティ

---

## 💡 補足: プロパティファイルのベストプラクティス

### プロパティの命名規則

```yaml
# 良い例: ケバブケース、階層構造
app:
  database:
    max-connections: 10
    timeout-seconds: 30

# 避けるべき: フラット、長い名前
app.database.max.connections: 10
```

### 機密情報の扱い

**application.ymlに書いてはいけないもの**:
- データベースのパスワード
- APIキー
- シークレットトークン

**代わりに使うべきもの**:
- 環境変数
- Secret管理サービス（AWS Secrets Manager等）
- プロパティファイルの暗号化

```yaml
# NG: パスワードをそのまま書く
spring:
  datasource:
    password: mySecretPassword123

# OK: 環境変数を参照
spring:
  datasource:
    password: ${DB_PASSWORD}
```

### プロファイル別設定（次のステップ以降で学習）

環境ごとに設定を分ける：

```
application.yml          # 共通設定
application-dev.yml      # 開発環境
application-prod.yml     # 本番環境
```

---

## 🔄 Gitへのコミットとレビュー依頼

進捗を記録してレビューを受けましょう：

```bash
git add .
git commit -m "Step 4: application.ymlで設定管理実装完了"
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

レビューが完了したら、[Step 5: Lombokで簡潔なコード](STEP_5.md)へ進みましょう！

次のステップでは、Lombokを使ってDTOクラスのGetter/Setterを自動生成し、
冗長なコードを劇的に削減する方法を学びます。Step 3で大量に書いたコードが数行になりますよ！

---

お疲れさまでした！ 🎉

設定ファイルでアプリケーションの挙動を制御できるようになりました。
次はコードをもっとシンプルに書く方法を学びましょう！
