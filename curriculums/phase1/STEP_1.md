# Step 1: Hello World REST API

## 🎯 このステップの目標

- Spring Initializrを使ってプロジェクトを作成できる
- `@RestController`の基本的な使い方を理解する
- Spring Bootアプリケーションを起動できる
- curlコマンドでAPIの動作確認ができる

**所要時間**: 約30分

---

## 📋 事前準備

以下がインストールされていることを確認してください：

- **OpenJDK 21** (確認: `java -version`)
- **Maven 3.8+** (確認: `mvn -v`)
- **IntelliJ IDEA Community Edition**
- **curl** (確認: `curl --version`)

**まだインストールしていない場合**: [Phase 1 事前準備ガイド](PREPARE.md)を参照してセットアップしてください。

---

## 🚀 ステップ1: プロジェクトの作成

### 方法A: Spring Initializr (Web)

1. ブラウザで https://start.spring.io/ にアクセス

2. 以下の設定を選択：
   - **Project**: Maven
   - **Language**: Java
   - **Spring Boot**: 3.5.7
   - **Project Metadata**:
     - Group: `com.example`
     - Artifact: `hello-spring-boot`
     - Name: `hello-spring-boot`
     - Package name: `com.example.hellospringboot`
     - Packaging: `Jar`
     - Java: `21`

3. **Dependencies**（右側の「ADD DEPENDENCIES」から追加）:
   - `Spring Web` （重要！これを必ず追加）

4. 「GENERATE」ボタンをクリックしてZIPファイルをダウンロード

5. ZIPを解凍してIDEで開く

### 方法B: IntelliJ IDEAから作成

1. IntelliJ IDEAを起動
2. `File` → `New` → `Project`
3. 左側で「Spring Initializr」を選択
4. 上記と同じ設定を入力
5. Next → Dependencies で「Spring Web」を追加
6. Create

---

## 📁 プロジェクト構造の確認

作成されたプロジェクトの構造を確認しましょう：

```
hello-spring-boot/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           └── hellospringboot/
│   │   │               └── HelloSpringBootApplication.java  ← メインクラス
│   │   └── resources/
│   │       ├── application.properties  ← 設定ファイル
│   │       ├── static/
│   │       └── templates/
│   └── test/
│       └── java/
├── pom.xml  ← Maven依存関係管理
└── README.md
```

**重要なファイル**：
- `HelloSpringBootApplication.java`: アプリケーションのエントリーポイント
- `pom.xml`: 依存関係（ライブラリ）の定義
- `application.properties`: アプリケーションの設定

---

## 🔧 ステップ2: REST Controllerの作成

### 2-1. Controllerクラスを作成

`src/main/java/com/example/hellospringboot/` に新しいパッケージとクラスを作成します。

**ファイルパス**: `src/main/java/com/example/hellospringboot/controller/HelloController.java`

```java
package com.example.hellospringboot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot!";
    }
}
```

### 2-2. コードの解説

#### `@RestController`
- このクラスがREST APIのコントローラーであることを示す
- `@Controller` + `@ResponseBody` を組み合わせたもの
- メソッドの戻り値が自動的にHTTPレスポンスのボディになる

#### `@GetMapping("/hello")`
- HTTPのGETリクエストを受け付ける
- パス `/hello` にアクセスされたときにこのメソッドが実行される
- `@RequestMapping(value = "/hello", method = RequestMethod.GET)` の省略形

#### `public String hello()`
- 実際の処理を行うメソッド
- 戻り値の文字列がそのままレスポンスとして返される

---

## ▶️ ステップ3: アプリケーションの起動

### 3-1. IDEから起動

**IntelliJ IDEA / Eclipse**:
1. `HelloSpringBootApplication.java` を開く
2. `main` メソッドの左側にある緑の再生ボタンをクリック
3. または右クリック → `Run 'HelloSpringBootApplication'`

**VS Code**:
1. 拡張機能「Extension Pack for Java」をインストール
2. `HelloSpringBootApplication.java` を開く
3. `main` メソッド上の「Run」リンクをクリック

### 3-2. コマンドラインから起動

ターミナルでプロジェクトのルートディレクトリに移動して：

```bash
# Mavenの場合
./mvnw spring-boot:run

# Gradleの場合
./gradlew bootRun
```

### 3-3. 起動確認

コンソールに以下のようなログが表示されれば起動成功です：

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.5.7)

2025-10-27T10:30:00.123+09:00  INFO 12345 --- [           main] c.e.h.HelloSpringBootApplication         : Starting HelloSpringBootApplication...
2025-10-27T10:30:01.234+09:00  INFO 12345 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port 8080 (http)
2025-10-27T10:30:02.345+09:00  INFO 12345 --- [           main] c.e.h.HelloSpringBootApplication         : Started HelloSpringBootApplication in 2.5 seconds
```

**重要**: `Tomcat started on port(s): 8080` → アプリケーションは **http://localhost:8080** で起動しています

---

## ✅ ステップ4: 動作確認

### 4-1. curlコマンドで確認

新しいターミナルを開いて、以下のコマンドを実行：

```bash
curl http://localhost:8080/hello
```

**期待される結果**:
```
Hello, Spring Boot!
```

### 4-2. ブラウザで確認

ブラウザで以下のURLにアクセス：

```
http://localhost:8080/hello
```

画面に `Hello, Spring Boot!` と表示されればOK！

### 4-3. Postmanで確認（オプション）

1. Postmanを開く
2. 新しいリクエストを作成
3. メソッド: `GET`
4. URL: `http://localhost:8080/hello`
5. 「Send」をクリック
6. レスポンスボディに `Hello, Spring Boot!` が表示される

---

## 🎨 チャレンジ課題

基本が理解できたら、以下にチャレンジしてみましょう：

### チャレンジ 1: 別のエンドポイントを追加

`HelloController.java` に以下のメソッドを追加：

```java
@GetMapping("/goodbye")
public String goodbye() {
    return "Goodbye, see you later!";
}
```

確認: `curl http://localhost:8080/goodbye`

### チャレンジ 2: 現在時刻を返すエンドポイント

```java
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@GetMapping("/time")
public String currentTime() {
    LocalDateTime now = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    return "Current time: " + now.format(formatter);
}
```

確認: `curl http://localhost:8080/time`

### チャレンジ 3: JSON形式で返す

```java
import java.util.Map;

@GetMapping("/info")
public Map<String, String> info() {
    return Map.of(
        "application", "Hello Spring Boot",
        "version", "1.0.0",
        "status", "running"
    );
}
```

確認: `curl http://localhost:8080/info`

**結果** (自動的にJSON形式に変換される):
```json
{
  "application": "Hello Spring Boot",
  "version": "1.0.0",
  "status": "running"
}
```

---

## 🐛 トラブルシューティング

### エラー: "Port 8080 was already in use"

**原因**: 既に8080ポートを使用しているアプリケーションがある

**解決策1**: 他のアプリケーションを停止

**解決策2**: ポート番号を変更
- `src/main/resources/application.properties` に以下を追加：
  ```properties
  server.port=8081
  ```
- アクセス先も変更: `http://localhost:8081/hello`

### エラー: "Web server failed to start"

**原因**: Spring Web依存関係が追加されていない

**解決策**: `pom.xml` に以下が含まれているか確認：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

### curlで "Connection refused"

**原因**: アプリケーションが起動していない

**解決策**: 
1. アプリケーションが起動しているか確認
2. ログで `Started HelloSpringBootApplication` が表示されているか確認
3. ポート番号が正しいか確認

---

## 📚 このステップで学んだこと

- ✅ Spring Initializrでプロジェクトを作成
- ✅ `@RestController`でREST APIを作成
- ✅ `@GetMapping`でGETリクエストを処理
- ✅ Spring Bootアプリケーションの起動方法
- ✅ curlでAPIの動作確認
- ✅ 文字列やJSONのレスポンス返却

---

## 🔄 Gitへのコミット（推奨）

進捗を記録するためにGitにコミットしましょう：

```bash
# プロジェクトディレクトリで
git init
git add .
git commit -m "Step 1: Hello World REST API完了"
```

---

## ➡️ 次のステップ

[Step 2: パスパラメータとクエリパラメータ](STEP_2.md)へ進みましょう！

パスパラメータ (`/greet/{name}`) やクエリパラメータ (`?language=ja`) の扱い方を学びます。

---

## 💡 補足: Spring Bootの仕組み

### なぜ設定なしで動くのか？

Spring Bootは **自動設定（Auto Configuration）** という機能を持っています：

1. `spring-boot-starter-web`が依存関係にある
2. → Spring Bootが「Webアプリケーションを作りたいんだな」と判断
3. → Tomcatサーバーを自動で組み込む
4. → コントローラーを自動でスキャン
5. → 8080ポートでサーバーを起動

**従来のSpring Frameworkとの違い**:
- 従来: XML設定ファイルやJava設定クラスを大量に記述
- Spring Boot: 必要最小限の設定で動作

### `@SpringBootApplication`の中身

`HelloSpringBootApplication.java`の`@SpringBootApplication`は以下3つを含みます：

- `@Configuration`: 設定クラスとして扱う
- `@EnableAutoConfiguration`: 自動設定を有効化
- `@ComponentScan`: コンポーネント（Controller, Serviceなど）を自動検出

これらのおかげで、シンプルなコードでアプリケーションが動作します！

---

お疲れさまでした！ 🎉

次のステップでさらにSpring Bootの世界を探索していきましょう！
