# Step 4: application.ymlで設定管理

## 🎯 このステップの目標

- `application.properties`と`application.yml`の違いを理解し、YAMLフォーマットのメリットを説明できる
- YAMLの基本的な構文（階層構造、リスト、コメント）を理解できる
- `@Value`アノテーションを使ってカスタム設定値をSpring Beanに注入できる
- プロファイル（dev/prod）を活用して環境別の設定を管理できる
- Spring Bootの設定ファイル読み込み優先順位を理解できる

**所要時間**: 約50分

---

## 📋 事前準備

このステップを始める前に、以下を確認してください：

- [Step 3: POSTリクエストとリクエストボディ](STEP_3.md)が完了している
- `hello-spring-boot`プロジェクトが作成されている
- `HelloController.java`と`UserController.java`が実装されている
- アプリケーションが正常に起動・動作することを確認している

### 環境確認

Step 3で作成したプロジェクトディレクトリに移動し、アプリケーションが起動することを確認しましょう：

```bash
cd ~/workspace/hello-spring-boot
./mvnw spring-boot:run
```

別のターミナルで動作確認：

```bash
curl http://localhost:8080/hello
```

**期待される結果**:
```
Hello, Spring Boot!
```

確認できたら、`Ctrl+C`でアプリケーションを停止してください。

---

## 🚀 ステップ1: application.propertiesとYAMLの違いを理解する

Spring Bootでは、アプリケーションの設定を外部ファイルで管理できます。設定ファイルには主に2つの形式があります：`application.properties`と`application.yml`（または`application.yaml`）です。

### 1-1. application.propertiesとは

`application.properties`は、キー=値の形式で設定を記述する伝統的なJavaの設定ファイル形式です。

**例**: `src/main/resources/application.properties`

```properties
# サーバー設定
server.port=8080

# アプリケーション名
spring.application.name=hello-spring-boot

# カスタム設定
app.name=MyApp
app.version=1.0.0
app.welcome-message=Welcome to Spring Boot
```

### 1-2. application.ymlとは

`application.yml`（または`application.yaml`）は、YAML（YAML Ain't Markup Language）形式で設定を記述するファイルです。階層構造を持ち、可読性が高いのが特徴です。

**例**: `src/main/resources/application.yml`

```yaml
# サーバー設定
server:
  port: 8080

# Spring設定
spring:
  application:
    name: hello-spring-boot

# カスタム設定
app:
  name: MyApp
  version: 1.0.0
  welcome-message: Welcome to Spring Boot
```

### 1-3. YAMLのメリット

#### メリット1: 階層構造が見やすい

**application.properties**:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
spring.datasource.password=secret
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

**application.yml**:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: secret
    driver-class-name: com.mysql.cj.jdbc.Driver
```

YAMLの方が**階層が一目でわかる**ため、設定の構造が理解しやすくなります。

#### メリット2: 重複する接頭辞を省略できる

propertiesでは`spring.datasource.`を毎回書く必要がありますが、YAMLでは`spring:`と`datasource:`を一度書けば済みます。

#### メリット3: リストやマップが記述しやすい

**リストの例**:
```yaml
app:
  allowed-origins:
    - http://localhost:3000
    - http://localhost:4200
    - https://example.com
```

**マップの例**:
```yaml
app:
  admin:
    username: admin
    email: admin@example.com
```

#### メリット4: コメントが読みやすい

```yaml
# データベース接続設定
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb  # MySQL接続URL
    username: root                          # ユーザー名
    password: secret                        # パスワード
```

### 1-4. どちらを使うべきか

| 形式 | メリット | デメリット | 推奨度 |
|---|---|---|---|
| **application.yml** | 階層が見やすい、記述が簡潔 | インデントのミスに注意 | ⭐⭐⭐ |
| **application.properties** | シンプル、エラーが起きにくい | 長くなると読みにくい | ⭐⭐ |

**結論**: 複雑な設定を扱う場合や、チーム開発では**application.yml**が推奨されます。このカリキュラムでも以降はYAMLを使用します。

> **💡 注意**: `application.properties`と`application.yml`を**両方同時に使用しない**でください。両方存在する場合、propertiesが優先され、混乱の原因になります。

---

## 🚀 ステップ2: application.propertiesをapplication.ymlに移行する

既存の`application.properties`を削除し、`application.yml`に移行しましょう。

### 2-1. 現在の設定を確認

まず、現在の`application.properties`の内容を確認します。

**ファイルパス**: `src/main/resources/application.properties`

```properties
spring.application.name=hello-spring-boot
```

### 2-2. application.ymlを作成

`src/main/resources/`ディレクトリに`application.yml`を作成します。

**ファイルパス**: `src/main/resources/application.yml`

```yaml
spring:
  application:
    name: hello-spring-boot

# サーバー設定
server:
  port: 8080

# カスタムアプリケーション設定
app:
  name: Hello Spring Boot Application
  version: 1.0.0
  welcome-message: Welcome to Spring Boot Configuration Management!
  description: This is a demo application for learning Spring Boot configuration.
```

### 2-3. application.propertiesを削除（オプション）

`application.properties`が存在する場合は削除します。

```bash
rm src/main/resources/application.properties
```

または、VSCodeで`src/main/resources/application.properties`を削除してください。

> **💡 ヒント**: もし残しておきたい場合は、ファイル名を`application.properties.bak`などに変更して無効化できます。

### 2-4. YAMLの基本文法

#### インデント（字下げ）

YAMLでは**スペース2つ**でインデントします。タブ文字は使用できません。

**正しい例** ✅:
```yaml
spring:
  application:
    name: hello-spring-boot
```

**間違った例** ❌:
```yaml
spring:
application:  # インデントがない
  name: hello-spring-boot
```

#### 値の記述

```yaml
key: value                    # 文字列（クォート不要）
key: "value with spaces"      # クォートで囲む（スペースや特殊文字がある場合）
key: 'single quote value'     # シングルクォートも可
number: 8080                  # 数値
boolean: true                 # ブール値（true/false）
```

#### リスト

```yaml
# 方法1: ハイフン記法
items:
  - item1
  - item2
  - item3

# 方法2: インライン記法
items: [item1, item2, item3]
```

#### コメント

```yaml
# これはコメントです
server:
  port: 8080  # 行末コメントも可能
```

---

## 🚀 ステップ3: @Valueでカスタム設定値を注入する

`application.yml`に定義したカスタム設定値を、Controllerで使用してみましょう。

### 3-1. @Valueアノテーションとは

`@Value`アノテーションを使うと、設定ファイルの値をSpring Beanのフィールドに**自動的に注入**できます。

**構文**:
```java
@Value("${設定キー}")
private String フィールド名;
```

### 3-2. HelloControllerに設定値を注入

`HelloController.java`を修正して、`application.yml`の設定値を使うようにします。

**ファイルパス**: `src/main/java/com/example/hellospringboot/HelloController.java`

```java
package com.example.hellospringboot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    // application.ymlから設定値を注入
    @Value("${app.name}")
    private String appName;

    @Value("${app.version}")
    private String appVersion;

    @Value("${app.welcome-message}")
    private String welcomeMessage;

    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot!";
    }

    // 新しいエンドポイント: アプリケーション情報を返す
    @GetMapping("/app-info")
    public String getAppInfo() {
        return String.format("Application: %s (Version: %s)%nMessage: %s",
                appName, appVersion, welcomeMessage);
    }

    @GetMapping("/users/{id}")
    public String getUser(@PathVariable Long id) {
        return "User ID: " + id;
    }

    @GetMapping("/search")
    public String search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return String.format("Search: keyword=%s, page=%d, size=%d",
                keyword, page, size);
    }
}
```

### 3-3. コードの解説

#### `@Value("${app.name}")`

- `${...}` は**プレースホルダー記法**です
- `app.name`は`application.yml`の以下の部分に対応します：
  ```yaml
  app:
    name: Hello Spring Boot Application
  ```
- Spring Bootが起動時に`application.yml`を読み込み、値を自動的に注入します

#### フィールドへの注入

```java
@Value("${app.name}")
private String appName;
```

- アプリケーション起動時、`appName`フィールドに`"Hello Spring Boot Application"`が設定されます
- これを**依存性注入（Dependency Injection）** と呼びます

#### `String.format()`

```java
String.format("Application: %s (Version: %s)%nMessage: %s",
        appName, appVersion, welcomeMessage);
```

- `%s`: 文字列のプレースホルダー
- `%n`: 改行（プラットフォーム非依存）
- Javaの標準機能で文字列をフォーマットします

### 3-4. デフォルト値の設定

設定値が存在しない場合に備えて、デフォルト値を指定できます。

```java
@Value("${app.description:No description available}")
private String description;
```

- `:`の後ろがデフォルト値です
- `app.description`が`application.yml`に存在しない場合、`"No description available"`が使われます

---

## 🚀 ステップ4: プロファイル（環境別設定）を理解する

実際の開発では、開発環境（development）と本番環境（production）で異なる設定を使い分ける必要があります。Spring Bootの**プロファイル機能**を使うと、環境ごとに設定を切り替えられます。

### 4-1. プロファイルとは

**プロファイル（Profile）** は、環境ごとに異なる設定を管理する仕組みです。

**例**:
- **開発環境（dev）**: ローカルDB、詳細なログ出力
- **本番環境（prod）**: 本番DB、エラーログのみ
- **テスト環境（test）**: テスト用DB、モックサービス

### 4-2. プロファイル別設定ファイルの作成

#### ベース設定（application.yml）

すべての環境で共通の設定を記述します。

**ファイルパス**: `src/main/resources/application.yml`

```yaml
spring:
  application:
    name: hello-spring-boot

# 共通設定
app:
  name: Hello Spring Boot Application
  version: 1.0.0
```

#### 開発環境設定（application-dev.yml）

開発環境固有の設定を記述します。

**ファイルパス**: `src/main/resources/application-dev.yml`

```yaml
# 開発環境設定
server:
  port: 8080

app:
  welcome-message: "[DEV] Welcome to Development Environment!"
  description: "This is a development environment. Debug mode is enabled."
  environment: development

# ログレベル（開発環境では詳細なログを出力）
logging:
  level:
    root: INFO
    com.example.hellospringboot: DEBUG
```

#### 本番環境設定（application-prod.yml）

本番環境固有の設定を記述します。

**ファイルパス**: `src/main/resources/application-prod.yml`

```yaml
# 本番環境設定
server:
  port: 80

app:
  welcome-message: "[PROD] Welcome to Production Environment!"
  description: "This is a production environment. Please use carefully."
  environment: production

# ログレベル（本番環境ではエラーログのみ）
logging:
  level:
    root: WARN
    com.example.hellospringboot: INFO
```

### 4-3. 設定ファイルの命名規則

```
application.yml               # ベース設定（すべての環境で有効）
application-{profile}.yml    # プロファイル別設定
```

**例**:
- `application-dev.yml` → `dev`プロファイル
- `application-prod.yml` → `prod`プロファイル
- `application-test.yml` → `test`プロファイル

### 4-4. 設定の優先順位

Spring Bootは以下の順序で設定を読み込みます（下に行くほど優先度が高い）：

1. `application.yml` （ベース設定）
2. `application-{profile}.yml` （プロファイル別設定）
3. コマンドライン引数
4. 環境変数

**例**: `application-dev.yml`に`server.port=8080`、`application.yml`に`server.port=9090`がある場合、devプロファイルでは**8080**が使われます。

### 4-5. HelloControllerに環境情報を追加

`application-dev.yml`と`application-prod.yml`に追加した`app.environment`を表示できるようにします。

**ファイルパス**: `src/main/java/com/example/hellospringboot/HelloController.java`

```java
package com.example.hellospringboot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @Value("${app.name}")
    private String appName;

    @Value("${app.version}")
    private String appVersion;

    @Value("${app.welcome-message}")
    private String welcomeMessage;

    @Value("${app.description:No description available}")
    private String description;

    @Value("${app.environment:unknown}")
    private String environment;

    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot!";
    }

    @GetMapping("/app-info")
    public String getAppInfo() {
        return String.format(
                "Application: %s (Version: %s)%n" +
                "Environment: %s%n" +
                "Message: %s%n" +
                "Description: %s",
                appName, appVersion, environment, welcomeMessage, description);
    }

    @GetMapping("/users/{id}")
    public String getUser(@PathVariable Long id) {
        return "User ID: " + id;
    }

    @GetMapping("/search")
    public String search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return String.format("Search: keyword=%s, page=%d, size=%d",
                keyword, page, size);
    }
}
```

---

## ✅ ステップ5: 動作確認

### 5-1. デフォルトプロファイルで起動

プロファイルを指定しない場合、`application.yml`のみが読み込まれます。

```bash
./mvnw spring-boot:run
```

別のターミナルで確認：

```bash
curl http://localhost:8080/app-info
```

**期待される結果**:
```
Application: Hello Spring Boot Application (Version: 1.0.0)
Environment: unknown
Message: ${app.welcome-message}
Description: No description available
```

> **💡 注意**: `app.welcome-message`と`app.environment`は`application.yml`に定義していないため、プレースホルダーのまま表示されるか、デフォルト値が使われます。

### 5-2. devプロファイルで起動

開発環境設定を有効化して起動します。

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

別のターミナルで確認：

```bash
curl http://localhost:8080/app-info
```

**期待される結果**:
```
Application: Hello Spring Boot Application (Version: 1.0.0)
Environment: development
Message: [DEV] Welcome to Development Environment!
Description: This is a development environment. Debug mode is enabled.
```

### 5-3. prodプロファイルで起動

本番環境設定を有効化して起動します。

> **⚠️ 注意**: 本番環境設定では`server.port=80`になっていますが、Linuxでポート80を使うには管理者権限が必要です。ここでは動作確認のため、ポート8080で起動します。

まず、`application-prod.yml`のポートを一時的に変更します：

**ファイルパス**: `src/main/resources/application-prod.yml`

```yaml
# 本番環境設定
server:
  port: 8081  # 動作確認用に8081に変更

app:
  welcome-message: "[PROD] Welcome to Production Environment!"
  description: "This is a production environment. Please use carefully."
  environment: production

logging:
  level:
    root: WARN
    com.example.hellospringboot: INFO
```

起動：

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

別のターミナルで確認（ポート8081に注意）：

```bash
curl http://localhost:8081/app-info
```

**期待される結果**:
```
Application: Hello Spring Boot Application (Version: 1.0.0)
Environment: production
Message: [PROD] Welcome to Production Environment!
Description: This is a production environment. Please use carefully.
```

### 5-4. ログレベルの違いを確認

#### devプロファイルでの起動ログ

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

起動ログに以下のような詳細なDEBUGメッセージが表示されます：

```
2024-12-13 12:00:00.123  INFO 12345 --- [           main] c.e.h.HelloSpringBootApplication         : Starting HelloSpringBootApplication
2024-12-13 12:00:00.456 DEBUG 12345 --- [           main] c.e.h.HelloController                    : HelloController initialized
...
```

#### prodプロファイルでの起動ログ

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

起動ログにはWARNレベル以上のメッセージのみが表示され、よりシンプルです。

### 5-5. 複数のプロファイルを同時に有効化

複数のプロファイルをカンマ区切りで指定できます。

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,debug
```

この場合、`application-dev.yml`と`application-debug.yml`の両方が読み込まれます。

---

## 🎨 チャレンジ課題

基本が理解できたら、以下にチャレンジしてみましょう：

### チャレンジ 1: ネストした設定値の読み込み

`application.yml`に以下の設定を追加し、`@Value`で読み込んでください。

```yaml
app:
  admin:
    username: admin
    email: admin@example.com
  database:
    host: localhost
    port: 3306
    name: mydb
```

**ヒント**:
```java
@Value("${app.admin.username}")
private String adminUsername;

@Value("${app.database.host}")
private String dbHost;
```

新しいエンドポイント`/admin-info`を作成し、管理者情報を返してください。

**期待される結果**:
```
Admin: admin (admin@example.com)
Database: localhost:3306/mydb
```

### チャレンジ 2: @ConfigurationPropertiesの利用

`@Value`は便利ですが、設定値が多くなると煩雑になります。`@ConfigurationProperties`を使うと、設定値をPOJOクラスにまとめて管理できます。

#### AppPropertiesクラスを作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/AppProperties.java`

```java
package com.example.hellospringboot;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String name;
    private String version;
    private String welcomeMessage;
    private String description;
    private String environment;

    // ゲッター・セッター
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }
}
```

#### Controllerで使用

```java
@RestController
public class HelloController {

    private final AppProperties appProperties;

    // コンストラクタインジェクション
    public HelloController(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @GetMapping("/app-info")
    public String getAppInfo() {
        return String.format(
                "Application: %s (Version: %s)%n" +
                "Environment: %s%n" +
                "Message: %s",
                appProperties.getName(),
                appProperties.getVersion(),
                appProperties.getEnvironment(),
                appProperties.getWelcomeMessage());
    }
}
```

**メリット**:
- 設定値が型安全（タイプセーフ）
- IDEの補完が効く
- 設定値をまとめて管理できる

### チャレンジ 3: リスト型の設定値

`application.yml`にリスト型の設定を追加してください。

```yaml
app:
  allowed-origins:
    - http://localhost:3000
    - http://localhost:4200
    - https://example.com
```

`@ConfigurationProperties`を使って読み込み、エンドポイント`/allowed-origins`で表示してください。

**ヒント**:
```java
private List<String> allowedOrigins;

public List<String> getAllowedOrigins() {
    return allowedOrigins;
}

public void setAllowedOrigins(List<String> allowedOrigins) {
    this.allowedOrigins = allowedOrigins;
}
```

**期待される結果**:
```
Allowed Origins: [http://localhost:3000, http://localhost:4200, https://example.com]
```

---

## 🐛 トラブルシューティング

### エラー: "Could not resolve placeholder 'app.name' in value \"${app.name}\""

**原因**: `application.yml`に`app.name`が定義されていない、または設定ファイルが読み込まれていません。

**解決策**:
1. `application.yml`に設定値が正しく記述されているか確認
2. YAMLのインデントが正しいか確認（スペース2つ）
3. `src/main/resources/`ディレクトリに配置されているか確認
4. アプリケーションを再起動する

### エラー: "Caused by: org.yaml.snakeyaml.scanner.ScannerException: while scanning for the next token"

**原因**: YAMLの構文エラー（インデントミス、タブ文字の使用など）です。

**解決策**:
1. インデントがスペース2つで統一されているか確認
2. タブ文字を使っていないか確認（VSCodeでタブをスペースに変換）
3. コロン（`:`）の後にスペースがあるか確認（`key: value`）
4. オンラインのYAMLバリデーターで構文をチェック

### エラー: "The server time zone value 'JST' is unrecognized"（データベース関連）

**原因**: （Step 6以降でMySQLを使用する際に発生）JDBCドライバーがタイムゾーンを認識できません。

**解決策**:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb?serverTimezone=Asia/Tokyo
```

### プロファイルが切り替わらない

**原因**: プロファイル指定のコマンドが間違っている、または設定ファイル名が間違っています。

**解決策**:
1. コマンドを確認: `-Dspring-boot.run.profiles=dev`
2. ファイル名を確認: `application-dev.yml`（ハイフンが必要）
3. 起動ログで以下のメッセージを確認:
   ```
   The following profiles are active: dev
   ```

### @Valueの値がnullになる

**原因**: `@Value`を使っているクラスがSpring Beanとして管理されていません。

**解決策**:
1. クラスに`@RestController`、`@Service`、`@Component`などのアノテーションを付ける
2. `@Value`はSpring Beanでのみ機能します（通常のクラスでは使えません）

### デフォルト値が効かない

**原因**: デフォルト値の構文が間違っています。

**正しい構文**:
```java
@Value("${app.unknown-key:default value}")
private String value;
```

コロン（`:`）の前後にスペースを入れないように注意してください。

---

## 📚 このステップで学んだこと

- ✅ `application.properties`と`application.yml`の違いと、YAMLのメリット（階層構造の可読性）
- ✅ YAMLの基本文法（インデント、コメント、リスト、マップ）
- ✅ `@Value`アノテーションを使った設定値の注入（`${...}`プレースホルダー）
- ✅ デフォルト値の設定方法（`${key:default}`）
- ✅ プロファイル（dev/prod）を使った環境別設定の管理
- ✅ プロファイル別設定ファイルの命名規則（`application-{profile}.yml`）
- ✅ Spring Bootの設定ファイル読み込み優先順位
- ✅ `@ConfigurationProperties`を使った型安全な設定管理（チャレンジ課題）

---

## 💡 補足: Spring Bootの設定ファイル読み込み順序

Spring Bootは、以下の順序で設定ファイルを読み込みます（下に行くほど優先度が高い）：

1. **jarファイル内の`application.yml`** （パッケージ化されたアプリケーション）
2. **jarファイル内の`application-{profile}.yml`** （プロファイル別設定）
3. **カレントディレクトリの`config/application.yml`** （アプリケーション実行ディレクトリ）
4. **カレントディレクトリの`application.yml`**
5. **コマンドライン引数** （`--server.port=9090`）
6. **環境変数** （`SPRING_APPLICATION_NAME=myapp`）

この仕組みにより、本番環境では**jarファイルの外部に設定ファイルを配置**して、コードを変更せずに設定を上書きできます。

**本番環境での配置例**:
```
/opt/myapp/
  ├── myapp.jar
  └── config/
      └── application-prod.yml  # 本番環境固有の設定（DB接続情報など）
```

起動コマンド:
```bash
java -jar myapp.jar --spring.profiles.active=prod
```

この場合、`config/application-prod.yml`がjarファイル内の設定を上書きします。

---

## 💡 補足: 環境変数による設定上書き

設定値は**環境変数**でも上書きできます。これは、Dockerコンテナやクラウド環境で便利です。

### 環境変数の命名規則

YAMLの階層構造を**アンダースコア（_）** と**大文字**に変換します。

**例**:
- `app.name` → `APP_NAME`
- `spring.datasource.url` → `SPRING_DATASOURCE_URL`
- `server.port` → `SERVER_PORT`

### 使用例

```bash
export APP_NAME="My Custom App Name"
export SERVER_PORT=9090
./mvnw spring-boot:run
```

アプリケーションは環境変数の値を優先的に使用します。

**Dockerでの使用例**:
```bash
docker run -e APP_NAME="Docker App" -e SERVER_PORT=8080 myapp:latest
```

---

## ➡️ 次のステップ

[Step 5: Lombokで簡潔なコード](STEP_5.md)へ進みましょう！

次のステップでは、**Lombok**というライブラリを使って、ボイラープレートコード（getter/setter、コンストラクタなど）を自動生成し、Javaコードをより簡潔に書く方法を学びます。

現在の`User`クラスや`AppProperties`クラスには、多くのgetter/setterが必要でした。Lombokを使うと、これらを**アノテーション1つで自動生成**できます。例えば：

```java
@Data  // getter/setter/toString/equals/hashCodeを自動生成
public class User {
    private Long id;
    private String name;
    private String email;
}
```

これにより、コードの記述量が大幅に減り、保守性が向上します。次のステップで実際に試してみましょう！
