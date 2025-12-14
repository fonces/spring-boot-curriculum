# Step 1: Hello World REST API

## 🎯 このステップの目標

- Spring Initializrで新しいSpring Bootプロジェクトを作成できる
- Spring Bootプロジェクトの基本構造を理解できる
- `@RestController`と`@GetMapping`を使って最初のREST APIを実装できる
- アプリケーションを起動し、curlコマンドで動作確認ができる
- Spring Bootの自動設定と組み込みTomcatの仕組みを理解できる

**所要時間**: 約40分

---

## 📋 事前準備

このステップを始める前に、以下の準備が完了していることを確認してください：

- Java 21 (OpenJDK)がインストールされている
- 作業用ディレクトリが用意されている
- ターミナル（またはWSL2）が利用可能
- インターネット接続がある

> **💡 ヒント**: まだ環境構築が済んでいない場合は、[Phase 1 事前準備ガイド](PREPARE.md)を先に完了してください。

### 環境確認コマンド

以下のコマンドで環境が整っていることを確認しましょう：

```bash
java -version
```

**期待される結果**:
```
openjdk version "21.0.x" 2024-xx-xx
OpenJDK Runtime Environment (build 21.0.x+xx)
OpenJDK 64-Bit Server VM (build 21.0.x+xx, mixed mode, sharing)
```

---

## 🚀 ステップ1: Spring Initializrでプロジェクトを作成

Spring Bootプロジェクトを作成する最も簡単な方法は、**Spring Initializr**を使うことです。これは、Spring公式が提供するプロジェクト生成ツールで、必要な依存関係を選択するだけで、すぐに開発を始められるプロジェクト構造を作成してくれます。

### 1-1. Spring Initializrにアクセス

ブラウザで以下のURLにアクセスします：

**https://start.spring.io/**

### 1-2. プロジェクト設定を入力

Spring Initializrの画面で、以下の項目を設定します：

| 項目 | 設定値 | 説明 |
|---|---|---|
| **Project** | Maven | ビルドツール（このカリキュラムではMavenを使用） |
| **Language** | Java | プログラミング言語 |
| **Spring Boot** | 3.5.8 | Spring Bootのバージョン |
| **Project Metadata** | | |
| └ Group | `com.example` | パッケージのグループID（会社やプロジェクトのドメイン） |
| └ Artifact | `hello-spring-boot` | プロジェクト名（アプリケーションの識別子） |
| └ Name | `hello-spring-boot` | アプリケーション名 |
| └ Description | `Demo project for Spring Boot` | プロジェクトの説明 |
| └ Package name | `com.example.hellospringboot` | Javaパッケージ名（自動生成） |
| └ Packaging | Jar | パッケージング形式（JarはSpring Boot推奨） |
| └ Java | 21 | Javaバージョン |

### 1-3. 依存関係を追加

画面右側の「ADD DEPENDENCIES」ボタンをクリックし、以下の依存関係を追加します：

- **Spring Web**: REST APIを構築するための基本機能
- **Spring Boot DevTools**: 開発時の自動リスタート・ホットリロード機能

> **💡 なぜSpring Webだけ？**: このステップではREST APIの基本を学ぶため、最小限の依存関係から始めます。データベースやセキュリティなどの機能は、後のステップで追加していきます。

> **💡 DevToolsとは？**: Spring Boot DevToolsは開発効率を向上させるツールです。コードを変更すると自動でアプリケーションを再起動してくれるため、毎回手動で再起動する手間が省けます。

### 1-4. プロジェクトを生成してダウンロード

「GENERATE」ボタン（画面下部）をクリックすると、`hello-spring-boot.zip`というファイルがダウンロードされます。

### 1-5. プロジェクトを展開

ダウンロードしたZIPファイルを作業用ディレクトリに展開します：

```bash
# 作業用ディレクトリに移動（例）
cd ~/workspace

# ZIPファイルを展開
unzip ~/Downloads/hello-spring-boot.zip

# プロジェクトディレクトリに移動
cd hello-spring-boot
```

---

## 🚀 ステップ2: プロジェクト構造を理解する

展開したプロジェクトの構造を確認しましょう。

### 2-1. ディレクトリ構造を確認

```bash
tree -L 3 -I 'target'
```

**期待される結果**:
```
.
├── HELP.md
├── mvnw
├── mvnw.cmd
├── pom.xml
└── src
    ├── main
    │   ├── java
    │   │   └── com
    │   │       └── example
    │   │           └── hellospringboot
    │   │               └── HelloSpringBootApplication.java
    │   └── resources
    │       ├── application.properties
    │       ├── static
    │       └── templates
    └── test
        └── java
            └── com
                └── example
                    └── hellospringboot
                        └── HelloSpringBootApplicationTests.java
```

> **💡 treeコマンドがない場合**:  
> macOS: `brew install tree`  
> WSL2/Ubuntu: `sudo apt install tree`  
> または、`ls -R`で代用できます。

### 2-2. 主要なファイルとディレクトリの役割

#### 📁 ルートディレクトリ

| ファイル/ディレクトリ | 役割 |
|---|---|
| `pom.xml` | Mavenプロジェクトの設定ファイル。依存関係やビルド設定を定義 |
| `mvnw` / `mvnw.cmd` | Maven Wrapper。Mavenがインストールされていなくてもビルド可能にするスクリプト |
| `src/main/java/` | アプリケーションのJavaソースコード |
| `src/main/resources/` | 設定ファイルや静的リソース |
| `src/test/java/` | テストコード |

#### 📄 `pom.xml` の中身を確認

`pom.xml`を開いて、重要な部分を確認しましょう：

```bash
cat pom.xml
```

**重要なセクション**:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" 
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <!-- Spring Boot親プロジェクト -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.8</version>
        <relativePath/>
    </parent>
    
    <!-- プロジェクト情報 -->
    <groupId>com.example</groupId>
    <artifactId>hello-spring-boot</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>hello-spring-boot</name>
    <description>Demo project for Spring Boot</description>
    
    <!-- Javaバージョン -->
    <properties>
        <java.version>21</java.version>
    </properties>
    
    <!-- 依存関係 -->
    <dependencies>
        <!-- Spring Web: REST API構築に必要 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- 開発用ツール (Hot Module Replacement) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot Test: テストツール一式 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <!-- ビルド設定 -->
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

**`pom.xml`の重要ポイント**:

1. **`<parent>`**: `spring-boot-starter-parent`を親プロジェクトとして継承
   - Spring Bootの推奨バージョン管理が自動適用される
   - 依存関係のバージョンを個別に指定する必要がない

2. **`<dependencies>`**: 必要なライブラリを宣言
   - `spring-boot-starter-web`: REST APIに必要な全ての依存関係を含む「スターター」
   - `spring-boot-devtools`: 開発時の自動リスタート・LiveReload機能を提供
   - バージョン番号がない（親プロジェクトで管理されている）

3. **`spring-boot-maven-plugin`**: Spring Boot実行可能Jarを作成するプラグイン
   - `./mvnw spring-boot:run`でアプリケーションを起動できる

#### 📄 DevToolsの設定を追加（オプション）

DevToolsの動作をカスタマイズしたい場合は、`src/main/resources/application.properties`に以下を追加します：

```properties
# DevTools設定
spring.devtools.restart.enabled=true
spring.devtools.livereload.enabled=true
```

または、`application.yml`形式で記述する場合：

```yaml
spring:
  devtools:
    restart:
      enabled: true
    livereload:
      enabled: true
```

> **💡 ヒント**: DevToolsはデフォルトで有効なので、この設定は省略可能です。設定ファイルの詳細はStep 4で学びます。

#### 📄 `HelloSpringBootApplication.java` の中身を確認

Spring Initializrが自動生成したメインクラスを見てみましょう：

```bash
cat src/main/java/com/example/hellospringboot/HelloSpringBootApplication.java
```

**生成されたコード**:

```java
package com.example.hellospringboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HelloSpringBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelloSpringBootApplication.class, args);
    }

}
```

**コードの解説**:

##### `@SpringBootApplication`

このアノテーションは、Spring Bootアプリケーションのエントリーポイントを示す**最重要アノテーション**です。実は、以下の3つのアノテーションを組み合わせた便利なアノテーションです：

1. **`@Configuration`**: このクラスがSpringの設定クラスであることを示す
2. **`@EnableAutoConfiguration`**: Spring Bootの自動設定機能を有効化
3. **`@ComponentScan`**: このクラスがあるパッケージ配下を自動スキャンし、`@Component`、`@Service`、`@Repository`、`@Controller`などのアノテーションが付いたクラスを自動登録

##### `SpringApplication.run()`

Spring Bootアプリケーションを起動するメソッドです。このメソッドが実行されると：

1. **Springコンテナ**（アプリケーションコンテキスト）が作成される
2. **自動設定**が適用される（`spring-boot-starter-web`があれば、組み込みTomcatが起動）
3. **Component Scan**により、コントローラーやサービスクラスが登録される
4. **組み込みTomcat**が起動し、HTTPリクエストを受け付ける準備が整う

---

## 🚀 ステップ3: 最初のREST APIを実装する

それでは、実際にREST APIを作成してみましょう！

### 3-1. Controllerクラスを作成

以下のコマンドで`HelloController.java`を作成します：

```bash
# Controllerディレクトリを作成（まだない場合）
mkdir -p src/main/java/com/example/hellospringboot

# ファイルを作成
touch src/main/java/com/example/hellospringboot/HelloController.java
```

または、VSCodeやお好みのエディタで以下のパスにファイルを作成します：

**ファイルパス**: `src/main/java/com/example/hellospringboot/HelloController.java`

```java
package com.example.hellospringboot;

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

### 3-2. コードの解説

#### `@RestController`

このアノテーションは、**RESTful Web Serviceのコントローラー**であることを示します。

**特徴**:
- `@Controller`と`@ResponseBody`を組み合わせたアノテーション
- メソッドの戻り値が**自動的にHTTPレスポンスのボディ**になる
- JSON形式でレスポンスを返す場合に便利（後のステップで学びます）

**Component Scanの対象**:
- `@RestController`が付いたクラスは、Spring起動時に自動的に**Beanとして登録**される
- `HelloSpringBootApplication.java`と同じパッケージ（または配下）にあれば、自動で検出される

#### `@GetMapping("/hello")`

このアノテーションは、**HTTP GETリクエスト**を処理するメソッドであることを示します。

**パラメータ**:
- `"/hello"`: このメソッドが処理するURLパス
- `http://localhost:8080/hello`にGETリクエストが来たら、このメソッドが実行される

**`@RequestMapping`との関係**:
```java
// @GetMappingは以下の省略形
@RequestMapping(value = "/hello", method = RequestMethod.GET)
public String hello() {
    return "Hello, Spring Boot!";
}
```

#### `return "Hello, Spring Boot!";`

- メソッドの戻り値（文字列）が、そのままHTTPレスポンスのボディになる
- `@RestController`が付いているため、自動的に`Content-Type: text/plain`で返される

---

## 🚀 ステップ4: アプリケーションを起動する

### 4-1. Maven Wrapperで起動

プロジェクトルートディレクトリで以下のコマンドを実行します：

```bash
./mvnw spring-boot:run
```

**コマンドの説明**:
- `./mvnw`: Maven Wrapper（プロジェクトに同梱されているMavenスクリプト）
- `spring-boot:run`: Spring Bootアプリケーションを起動するゴール

### 4-2. 起動ログを確認

以下のようなログが表示されれば成功です：

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.5.8)

2024-12-13T10:00:00.000+09:00  INFO 12345 --- [           main] c.e.h.HelloSpringBootApplication         : Starting HelloSpringBootApplication using Java 21.0.1
2024-12-13T10:00:00.000+09:00  INFO 12345 --- [           main] c.e.h.HelloSpringBootApplication         : No active profile set, falling back to 1 default profile: "default"
2024-12-13T10:00:01.000+09:00  INFO 12345 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port 8080 (http)
2024-12-13T10:00:01.500+09:00  INFO 12345 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2024-12-13T10:00:01.500+09:00  INFO 12345 --- [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/10.1.33]
2024-12-13T10:00:02.000+09:00  INFO 12345 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 8080 (http) with context path '/'
2024-12-13T10:00:02.500+09:00  INFO 12345 --- [           main] c.e.h.HelloSpringBootApplication         : Started HelloSpringBootApplication in 2.5 seconds (process running for 3.0)
```

**重要なログメッセージ**:

- `Starting HelloSpringBootApplication using Java 21.0.1`: アプリケーションが起動開始
- `Tomcat initialized with port 8080 (http)`: 組み込みTomcatがポート8080で初期化
- `Tomcat started on port 8080`: Tomcatが起動完了
- `Started HelloSpringBootApplication in 2.5 seconds`: アプリケーションが起動完了

> **💡 ヒント**: `Started HelloSpringBootApplication`というメッセージが表示されたら、アプリケーションが正常に起動しています！

---

## ✅ ステップ5: 動作確認

### 5-1. curlコマンドで確認

別のターミナルを開いて、以下のコマンドを実行します：

```bash
curl http://localhost:8080/hello
```

**期待される結果**:
```
Hello, Spring Boot!
```

### 5-2. ブラウザで確認

ブラウザで以下のURLにアクセスしてみましょう：

**http://localhost:8080/hello**

画面に「**Hello, Spring Boot!**」と表示されれば成功です🎉

### 5-3. レスポンスヘッダーを確認（オプション）

curlの`-v`オプションで詳細なレスポンスを確認できます：

```bash
curl -v http://localhost:8080/hello
```

**期待される結果**:
```
*   Trying 127.0.0.1:8080...
* Connected to localhost (127.0.0.1) port 8080 (#0)
> GET /hello HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/8.4.0
> Accept: */*
> 
< HTTP/1.1 200 
< Content-Type: text/plain;charset=UTF-8
< Content-Length: 19
< Date: Fri, 13 Dec 2024 01:00:00 GMT
< 
Hello, Spring Boot!
```

**重要なポイント**:
- `HTTP/1.1 200`: HTTPステータスコード（成功）
- `Content-Type: text/plain;charset=UTF-8`: レスポンスの形式（プレーンテキスト）
- `Content-Length: 19`: レスポンスボディの長さ

### 5-4. アプリケーションの停止

ターミナルで`Ctrl + C`を押すと、アプリケーションが停止します。

---

## 💡 補足: Spring Bootの自動設定の仕組み

### なぜコードを少し書くだけでWebサーバーが動くのか？

従来のJavaでWebアプリケーションを作る場合、以下のような煩雑な設定が必要でした：

- Tomcatなどのアプリケーションサーバーを別途インストール
- XML形式の設定ファイル（`web.xml`など）を記述
- サーバーにWARファイルをデプロイ

しかし、**Spring Bootの自動設定**により、これらが不要になりました！

### 自動設定が働く仕組み

#### 1. `spring-boot-starter-web`の依存関係

`pom.xml`に`spring-boot-starter-web`を追加すると、以下が自動的に含まれます：

- **Spring MVC**: Webアプリケーションフレームワーク
- **Jackson**: JSON変換ライブラリ
- **組み込みTomcat**: Webサーバー
- その他、Web開発に必要なライブラリ一式

#### 2. `@EnableAutoConfiguration`の働き

`@SpringBootApplication`に含まれる`@EnableAutoConfiguration`が、クラスパス上の依存関係を検出し、適切な設定を自動的に適用します：

```
spring-boot-starter-webがある
  ↓
Spring MVCを有効化
  ↓
組み込みTomcatを起動
  ↓
ポート8080でHTTPリクエスト待ち受け
```

#### 3. Component Scanの働き

`@SpringBootApplication`のあるパッケージ配下を自動スキャンし、`@RestController`などのアノテーションを見つけると、自動的にSpringコンテナに登録します：

```
HelloSpringBootApplication.java（エントリーポイント）
  ↓
com.example.hellospringbootパッケージをスキャン
  ↓
@RestController付きのHelloControllerを発見
  ↓
Springコンテナに登録
  ↓
/helloエンドポイントが利用可能に
```

### 設定のカスタマイズ（次のステップで詳しく学びます）

デフォルトでは以下のような設定が自動適用されます：

- **ポート番号**: 8080
- **コンテキストパス**: `/`（ルート）
- **ログレベル**: INFO

これらは`application.properties`や`application.yml`で変更できます（Step 4で学びます）。

---

## 🎨 チャレンジ課題

基本が理解できたら、以下にチャレンジしてみましょう：

### チャレンジ 1: 新しいエンドポイントを追加

`/goodbye`というエンドポイントを追加して、「Goodbye, Spring Boot!」と返すようにしてみましょう。

**ヒント**:
```java
@GetMapping("/goodbye")
public String goodbye() {
    // ここに実装
}
```

**確認コマンド**:
```bash
curl http://localhost:8080/goodbye
```

**期待される結果**:
```
Goodbye, Spring Boot!
```

### チャレンジ 2: 時刻を返すエンドポイント

`/time`というエンドポイントを追加して、現在時刻を返すようにしてみましょう。

**ヒント**:
```java
import java.time.LocalDateTime;

@GetMapping("/time")
public String currentTime() {
    // LocalDateTime.now()で現在時刻を取得
    // toString()で文字列に変換して返す
}
```

**確認コマンド**:
```bash
curl http://localhost:8080/time
```

**期待される結果（例）**:
```
2024-12-13T10:30:45.123456
```

### チャレンジ 3: ルートパスのエンドポイント

`/`（ルートパス）にアクセスしたときに、ウェルカムメッセージを返すようにしてみましょう。

**ヒント**:
```java
@GetMapping("/")
public String welcome() {
    return "Welcome to Spring Boot Application!";
}
```

**確認方法**:
```bash
curl http://localhost:8080/
```

または、ブラウザで`http://localhost:8080/`にアクセス。

---

## 🐛 トラブルシューティング

### エラー: "Port 8080 was already in use"

**エラーメッセージ**:
```
***************************
APPLICATION FAILED TO START
***************************

Description:

Web server failed to start. Port 8080 was already in use.
```

**原因**: ポート8080が既に別のプロセスで使用されている

**解決策**:

#### 方法1: 既存のプロセスを停止

ポート8080を使用しているプロセスを確認：

```bash
# macOS/Linux/WSL2
lsof -i :8080
```

プロセスIDを確認して停止：

```bash
kill -9 <プロセスID>
```

#### 方法2: 別のポートを使用

`src/main/resources/application.properties`を作成（または編集）して、以下を追加：

```properties
server.port=8081
```

これで、ポート8081で起動するようになります。

### エラー: "Could not find or load main class"

**エラーメッセージ**:
```
Error: Could not find or load main class com.example.hellospringboot.HelloSpringBootApplication
```

**原因**: クラスパスの問題、またはビルドが失敗している

**解決策**:

```bash
# プロジェクトをクリーンビルド
./mvnw clean install

# 再度起動
./mvnw spring-boot:run
```

### エラー: "/hello にアクセスしても404 Not Found"

**エラーメッセージ（curlの場合）**:
```
{"timestamp":"2024-12-13T01:00:00.000+00:00","status":404,"error":"Not Found","path":"/hello"}
```

**原因**: コントローラーがSpringコンテナに登録されていない

**解決策**:

1. **パッケージ構成を確認**: `HelloController.java`が`com.example.hellospringboot`パッケージにあるか確認
2. **`@RestController`アノテーションがあるか確認**
3. **アプリケーションを再起動**:
   ```bash
   # Ctrl+Cで停止
   ./mvnw spring-boot:run
   ```

### エラー: "java: invalid source release: 21"

**エラーメッセージ**:
```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.11.0:compile (default-compile) on project hello-spring-boot: Fatal error compiling: error: invalid source release: 21
```

**原因**: JavaコンパイラがJava 21を認識できていない

**解決策**:

1. **Java 21がインストールされているか確認**:
   ```bash
   java -version
   ```

2. **JAVA_HOMEを確認**:
   ```bash
   echo $JAVA_HOME
   ```

3. **JAVA_HOMEを設定** (未設定の場合):
   ```bash
   # macOS
   export JAVA_HOME=$(/usr/libexec/java_home -v 21)
   
   # WSL2/Ubuntu
   export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
   ```

### 警告: "Application run failed"（起動時のStackTrace）

**症状**: アプリケーションが起動せず、長いスタックトレースが表示される

**解決策**:

1. **スタックトレースの最初の行を確認**（根本原因が記載されている）
2. **よくある原因**:
   - 依存関係の競合 → `./mvnw clean install`で再ビルド
   - 設定ファイルの構文エラー → `application.properties`を確認
   - ポート競合 → 上記「Port 8080 was already in use」を参照

---

## 📚 このステップで学んだこと

- ✅ **Spring Initializr**を使ってプロジェクトを生成できる
- ✅ **pom.xml**の役割と基本構造を理解できる
- ✅ **@SpringBootApplication**がアプリケーションのエントリーポイントであることを理解できる
- ✅ **@RestController**でREST APIコントローラーを作成できる
- ✅ **@GetMapping**でHTTP GETリクエストを処理できる
- ✅ **./mvnw spring-boot:run**でアプリケーションを起動できる
- ✅ **curlコマンド**でAPIの動作確認ができる
- ✅ Spring Bootの**自動設定**と**組み込みTomcat**の仕組みを理解できる
- ✅ **Component Scan**により、コントローラーが自動登録されることを理解できる

---

## ➡️ 次のステップ

[Step 2: パスパラメータとクエリパラメータ](STEP_2.md)へ進みましょう！

次のステップでは、以下を学びます：

- **パスパラメータ**（`/users/{id}`）で動的なURLを扱う方法
- **クエリパラメータ**（`/users?page=1&size=10`）でフィルタリングする方法
- `@PathVariable`と`@RequestParam`の使い方
- RESTful APIの設計における基本的なベストプラクティス

これらを学ぶことで、より実践的なREST APIを構築できるようになります！
