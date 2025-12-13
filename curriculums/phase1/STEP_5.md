# Step 5: Lombokで簡潔なコード

## 🎯 このステップの目標

- Lombokライブラリの目的と利点を理解し、ボイラープレートコード削減のメリットを説明できる
- `@Data`、`@Getter`、`@Setter`、`@NoArgsConstructor`、`@AllArgsConstructor`を使ってPOJOクラスを簡潔に記述できる
- `@RequiredArgsConstructor`を使ってコンストラクタインジェクションを簡潔に実装できる
- Before/Afterのコード比較を通じて、Lombokによる記述量削減の効果を実感できる
- Lombokのメリットとデメリットを理解し、適切に使い分けられる

**所要時間**: 約45分

---

## 📋 事前準備

このステップを始める前に、以下を確認してください：

- [Step 4: application.ymlで設定管理](STEP_4.md)が完了している
- `hello-spring-boot`プロジェクトが作成されている
- `User.java`、`HelloController.java`、`UserController.java`が実装されている
- アプリケーションが正常に起動・動作することを確認している

### 環境確認

Step 4で作成したプロジェクトディレクトリに移動し、アプリケーションが起動することを確認しましょう：

```bash
cd ~/workspace/hello-spring-boot
./mvnw spring-boot:run
```

別のターミナルで動作確認：

```bash
curl http://localhost:8080/api/users
```

**期待される結果**:
```json
[]
```

確認できたら、`Ctrl+C`でアプリケーションを停止してください。

---

## 🚀 ステップ1: Lombokとは？ボイラープレートコードの問題を理解する

### 1-1. ボイラープレートコードとは

**ボイラープレートコード（Boilerplate Code）**とは、プログラムで何度も繰り返し書かなければならない定型的なコードのことです。

Javaでは特に、以下のようなコードが典型的なボイラープレートコードです：

- **getter/setterメソッド**
- **コンストラクタ**
- **toString()メソッド**
- **equals()とhashCode()メソッド**

### 1-2. 現在のUser.javaを確認

現在の`User.java`を見てみましょう：

**ファイルパス**: `src/main/java/com/example/hellospringboot/User.java`

```java
package com.example.hellospringboot;

public class User {
    private Long id;
    private String name;
    private String email;
    private Integer age;

    // デフォルトコンストラクタ（JSONデシリアライズに必要）
    public User() {
    }

    // すべてのフィールドを持つコンストラクタ
    public User(Long id, String name, String email, Integer age) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
    }

    // ゲッター/セッター
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
```

たった4つのフィールド（`id`、`name`、`email`、`age`）を持つシンプルなクラスなのに、**54行**もあります！

実際にビジネスロジックを書いているのは最初の4行だけで、残りはすべて定型的なコードです。

### 1-3. Lombokとは

**Lombok**は、アノテーションを使ってこれらのボイラープレートコードを**コンパイル時に自動生成**してくれるJavaライブラリです。

Lombokを使うと、上記の`User.java`をこのように書けます：

```java
package com.example.hellospringboot;

import lombok.Data;

@Data
public class User {
    private Long id;
    private String name;
    private String email;
    private Integer age;
}
```

たった**10行**で、getter/setter、toString()、equals()、hashCode()などがすべて自動生成されます！

### 1-4. Lombokの仕組み

Lombokは**アノテーションプロセッサ（Annotation Processor）**として動作します。

```
[ソースコード]
   ↓
[Lombokがアノテーションを検出]
   ↓
[コンパイル時にメソッドを自動生成]
   ↓
[.classファイル（バイトコード）]
```

つまり、**ソースコードには書かれていないけど、コンパイル後のクラスには存在する**ということです。

### 1-5. Lombokのメリットとデメリット

#### メリット

- ✅ **コード量が劇的に減る**: 定型的なコードを書かなくて済む
- ✅ **可読性が向上**: ビジネスロジックに集中できる
- ✅ **保守性が高まる**: フィールドを追加してもgetter/setterを書く必要がない
- ✅ **バグが減る**: 自動生成されるので人的ミスが減る

#### デメリット

- ❌ **IDE/エディタの設定が必要**: プラグインをインストールしないと補完が効かない
- ❌ **学習コスト**: アノテーションの種類と使い方を覚える必要がある
- ❌ **デバッグが難しい場合がある**: 自動生成されたコードはソースに見えない
- ❌ **過度に使うと可読性が下がる**: `@Data`を多用すると何が生成されるか分かりにくい

### 1-6. Lombokの主要なアノテーション

このステップでは、以下のアノテーションを学びます：

| アノテーション | 生成されるもの |
|---|---|
| `@Getter` | すべてのフィールドのgetterメソッド |
| `@Setter` | すべてのフィールドのsetterメソッド |
| `@ToString` | toString()メソッド |
| `@EqualsAndHashCode` | equals()とhashCode()メソッド |
| `@NoArgsConstructor` | 引数なしのコンストラクタ |
| `@AllArgsConstructor` | すべてのフィールドを引数に持つコンストラクタ |
| `@RequiredArgsConstructor` | `final`フィールドのみを引数に持つコンストラクタ |
| `@Data` | `@Getter` + `@Setter` + `@ToString` + `@EqualsAndHashCode` + `@RequiredArgsConstructor` |
| `@Builder` | Builderパターンの実装 |
| `@Slf4j` | ロガーフィールドの自動生成 |

---

## 🚀 ステップ2: Lombokの依存関係を追加する

### 2-1. pom.xmlにLombokを追加

`pom.xml`を開き、`<dependencies>`セクションにLombokの依存関係を追加します。

**ファイルパス**: `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>3.5.8</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>
	<groupId>com.example</groupId>
	<artifactId>hello-spring-boot</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>hello-spring-boot</name>
	<description>Demo project for Spring Boot</description>
	<url/>
	<licenses>
		<license/>
	</licenses>
	<developers>
		<developer/>
	</developers>
	<scm>
		<connection/>
		<developerConnection/>
		<tag/>
		<url/>
	</scm>
	<properties>
		<java.version>21</java.version>
	</properties>
	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>

		<!-- Lombok -->
		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
				<configuration>
					<excludes>
						<exclude>
							<groupId>org.projectlombok</groupId>
							<artifactId>lombok</artifactId>
						</exclude>
					</excludes>
				</configuration>
			</plugin>
		</plugins>
	</build>

</project>
```

### 2-2. Lombokの設定の解説

#### `<optional>true</optional>`

Lombokは**コンパイル時にのみ必要**で、実行時には不要です。`<optional>true</optional>`を指定することで、このプロジェクトを他のプロジェクトから依存する際に、Lombokが伝播しないようにしています。

#### `spring-boot-maven-plugin`の設定

```xml
<configuration>
    <excludes>
        <exclude>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </exclude>
    </excludes>
</configuration>
```

Spring BootのJARファイルを作成する際に、Lombokを含めないようにしています。これは、Lombokが**コンパイル時にのみ必要**で、実行時のJARには不要だからです。

### 2-3. 依存関係のダウンロード

依存関係を追加したら、Mavenで依存関係をダウンロードします：

```bash
./mvnw clean compile
```

**期待される出力**:
```
[INFO] BUILD SUCCESS
```

これで、Lombokが使える準備が整いました！

---

## 🚀 ステップ3: User.javaを@Dataでリファクタリングする

### 3-1. Before: 現在のUser.java（54行）

現在の`User.java`は54行あり、そのほとんどがボイラープレートコードです：

**ファイルパス**: `src/main/java/com/example/hellospringboot/User.java`

```java
package com.example.hellospringboot;

public class User {
    private Long id;
    private String name;
    private String email;
    private Integer age;

    // デフォルトコンストラクタ（JSONデシリアライズに必要）
    public User() {
    }

    // すべてのフィールドを持つコンストラクタ
    public User(Long id, String name, String email, Integer age) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
    }

    // ゲッター/セッター
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
```

### 3-2. After: Lombokを使ったUser.java（13行）

`User.java`を以下のように書き換えましょう：

**ファイルパス**: `src/main/java/com/example/hellospringboot/User.java`

```java
package com.example.hellospringboot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String name;
    private String email;
    private Integer age;
}
```

**54行 → 13行**に削減されました！これがLombokの威力です。

### 3-3. アノテーションの解説

#### `@Data`

`@Data`は、以下のアノテーションをまとめたものです：

- `@Getter`: すべてのフィールドにgetterメソッドを生成
- `@Setter`: すべてのfinalでないフィールドにsetterメソッドを生成
- `@ToString`: toString()メソッドを生成
- `@EqualsAndHashCode`: equals()とhashCode()メソッドを生成
- `@RequiredArgsConstructor`: finalフィールドのみを引数に持つコンストラクタを生成

#### `@NoArgsConstructor`

引数なしのデフォルトコンストラクタを生成します。

```java
public User() {
}
```

Spring BootのJSONデシリアライズ（JSONからJavaオブジェクトへの変換）では、デフォルトコンストラクタが必要なため、明示的に指定しています。

#### `@AllArgsConstructor`

すべてのフィールドを引数に持つコンストラクタを生成します。

```java
public User(Long id, String name, String email, Integer age) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.age = age;
}
```

テストコードなどで、オブジェクトを簡単に作成できるようになります。

### 3-4. コンパイルして確認

コードを保存したら、コンパイルして確認しましょう：

```bash
./mvnw clean compile
```

**期待される出力**:
```
[INFO] BUILD SUCCESS
```

エラーが出なければ、Lombokが正しく動作しています！

---

## 🚀 ステップ4: @RequiredArgsConstructorでコンストラクタインジェクションを簡潔にする

Spring Bootでは、**コンストラクタインジェクション（Constructor Injection）**が推奨されています。Lombokの`@RequiredArgsConstructor`を使うと、これを簡潔に書けます。

### 4-1. コンストラクタインジェクションとは

Spring Bootでは、依存オブジェクト（Bean）を注入する方法として、以下の3つがあります：

1. **コンストラクタインジェクション**（推奨）
2. フィールドインジェクション（`@Autowired`をフィールドに付ける）
3. セッターインジェクション（setterメソッドに`@Autowired`を付ける）

コンストラクタインジェクションが推奨される理由：
- ✅ **不変性（Immutability）**: フィールドを`final`にできる
- ✅ **テストしやすい**: モックオブジェクトを簡単に渡せる
- ✅ **必須依存が明確**: コンストラクタで渡さないとオブジェクトが作れない

### 4-2. Before: 従来のコンストラクタインジェクション

例として、新しいサービスクラス`UserService`を作成してみましょう。

まず、Lombokを使わない場合のコードを見てみます：

```java
package com.example.hellospringboot;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {
    private final List<User> users;

    // コンストラクタインジェクション
    public UserService() {
        this.users = new ArrayList<>();
    }

    public List<User> getAllUsers() {
        return users;
    }

    public void addUser(User user) {
        users.add(user);
    }
}
```

この例では依存が単純なので問題ありませんが、複数の依存がある場合は以下のようになります：

```java
@Service
public class UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    public UserService(UserRepository userRepository, 
                       EmailService emailService,
                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.notificationService = notificationService;
    }
    
    // ... メソッド
}
```

フィールドが3つあると、コンストラクタだけで7行必要です。

### 4-3. After: @RequiredArgsConstructorを使った簡潔な書き方

実際に`UserService`クラスを作成しましょう。

**ファイルパス**: `src/main/java/com/example/hellospringboot/UserService.java`

```java
package com.example.hellospringboot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    // finalフィールドはコンストラクタで初期化される必要がある
    // @RequiredArgsConstructorが自動的にコンストラクタを生成
    private final List<User> users = new ArrayList<>();

    public List<User> getAllUsers() {
        return new ArrayList<>(users); // 防御的コピー
    }

    public User addUser(User user) {
        // 簡易的なID生成
        if (user.getId() == null) {
            user.setId((long) (users.size() + 1));
        }
        users.add(user);
        return user;
    }

    public User getUserById(Long id) {
        return users.stream()
                .filter(user -> user.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}
```

### 4-4. @RequiredArgsConstructorの解説

`@RequiredArgsConstructor`は、**finalフィールドと@NonNullフィールド**を引数に持つコンストラクタを自動生成します。

上記の例では、`users`フィールドは初期化式があるため、実際にはコンストラクタの引数にはなりませんが、`@RequiredArgsConstructor`を付けることで、将来的に依存を追加した際に自動的にコンストラクタが更新されます。

より実践的な例：

```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;
    
    // Lombokが以下のコンストラクタを自動生成
    // public UserService(UserRepository userRepository, EmailService emailService) {
    //     this.userRepository = userRepository;
    //     this.emailService = emailService;
    // }
}
```

### 4-5. UserControllerをUserServiceを使うように更新

既存の`UserController`を、作成した`UserService`を使うように更新しましょう。

**ファイルパス**: `src/main/java/com/example/hellospringboot/UserController.java`

```java
package com.example.hellospringboot;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.addUser(user);
    }
}
```

**変更点**:
- ✅ `@RequiredArgsConstructor`を追加
- ✅ `private final UserService userService;`でサービスを注入
- ✅ ビジネスロジックを`UserService`に委譲
- ✅ インメモリのリストを削除（UserServiceに移動）

これで、**Controller（APIの窓口） → Service（ビジネスロジック）** という責任分離ができました！

---

## ✅ ステップ5: 動作確認

### 5-1. アプリケーションを起動

アプリケーションを起動します：

```bash
./mvnw spring-boot:run
```

### 5-2. ユーザーを作成

POSTリクエストでユーザーを作成します：

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "山田太郎",
    "email": "yamada@example.com",
    "age": 30
  }'
```

**期待される結果**:
```json
{"id":1,"name":"山田太郎","email":"yamada@example.com","age":30}
```

### 5-3. すべてのユーザーを取得

```bash
curl http://localhost:8080/api/users
```

**期待される結果**:
```json
[{"id":1,"name":"山田太郎","email":"yamada@example.com","age":30}]
```

### 5-4. IDでユーザーを取得

```bash
curl http://localhost:8080/api/users/1
```

**期待される結果**:
```json
{"id":1,"name":"山田太郎","email":"yamada@example.com","age":30}
```

すべて正常に動作すれば成功です！ 🎉

---

## 🚀 ステップ6: Lombokの他のアノテーションを理解する

### 6-1. @Getter/@Setter（個別指定）

`@Data`はすべてのフィールドにgetter/setterを生成しますが、特定のフィールドだけに適用したい場合は、`@Getter`と`@Setter`を個別に使います。

```java
import lombok.Getter;
import lombok.Setter;

public class Product {
    @Getter @Setter
    private Long id;
    
    @Getter @Setter
    private String name;
    
    @Getter  // getterのみ（setterは生成しない）
    private Double price;
    
    public Product(Long id, String name, Double price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }
}
```

**使い分け**:
- **不変フィールド**: `@Getter`のみ（setterを生成しない）
- **可変フィールド**: `@Getter @Setter`

### 6-2. @ToString

`toString()`メソッドを生成します。

```java
import lombok.ToString;

@ToString
public class User {
    private Long id;
    private String name;
    private String password;  // パスワードは表示したくない
}
```

デフォルトでは、すべてのフィールドが含まれます：

```
User(id=1, name=山田太郎, password=secret123)
```

**特定のフィールドを除外する**:

```java
@ToString(exclude = "password")
public class User {
    private Long id;
    private String name;
    private String password;
}
```

結果：
```
User(id=1, name=山田太郎)
```

### 6-3. @EqualsAndHashCode

`equals()`と`hashCode()`メソッドを生成します。

```java
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class User {
    private Long id;
    private String name;
}
```

**特定のフィールドだけで比較する**:

```java
@EqualsAndHashCode(of = "id")  // idだけで比較
public class User {
    private Long id;
    private String name;
}
```

### 6-4. @Builder

**Builderパターン**を自動生成します。オブジェクトの生成を流暢（fluent）に書けるようになります。

```java
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class User {
    private Long id;
    private String name;
    private String email;
    private Integer age;
}
```

**使い方**:

```java
User user = User.builder()
    .id(1L)
    .name("山田太郎")
    .email("yamada@example.com")
    .age(30)
    .build();
```

**メリット**:
- ✅ 可読性が高い（どのフィールドに何を設定しているか明確）
- ✅ 順序を気にしなくて良い
- ✅ オプショナルなフィールドを扱いやすい

### 6-5. @Slf4j

ロガー（Logger）フィールドを自動生成します。

```java
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping
    public List<User> getAllUsers() {
        log.info("すべてのユーザーを取得");  // ログ出力
        // ...
    }
}
```

Lombokが以下のコードを自動生成します：

```java
private static final org.slf4j.Logger log = 
    org.slf4j.LoggerFactory.getLogger(UserController.class);
```

---

## 🎨 チャレンジ課題

基本が理解できたら、以下にチャレンジしてみましょう：

### チャレンジ 1: @Builderパターンでユーザーを作成

`User`クラスに`@Builder`アノテーションを追加し、Builderパターンでユーザーを作成してみましょう。

**ヒント**:

1. `User.java`に`@Builder`を追加
2. `UserService.java`で以下のようにオブジェクトを作成

```java
User newUser = User.builder()
    .id(1L)
    .name("佐藤花子")
    .email("sato@example.com")
    .age(25)
    .build();
```

**期待される動作**:
- Builderパターンでユーザーを作成できる
- 可読性が向上する

### チャレンジ 2: カスタムtoString()の実装

`User`クラスの`toString()`で、パスワードフィールドを除外してみましょう。

**手順**:

1. `User`クラスに`password`フィールドを追加
2. `@ToString(exclude = "password")`を使う
3. ログ出力で確認

**ヒント**:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "password")
public class User {
    private Long id;
    private String name;
    private String email;
    private Integer age;
    private String password;  // 追加
}
```

### チャレンジ 3: @Slf4jでロギング機能を追加

`UserController`に`@Slf4j`を追加し、各エンドポイントでログを出力してみましょう。

**手順**:

1. `UserController`に`@Slf4j`を追加
2. 各メソッドで`log.info()`を使ってログを出力
3. アプリケーションを起動してログを確認

**ヒント**:

```java
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public List<User> getAllUsers() {
        log.info("GET /api/users - すべてのユーザーを取得");
        return userService.getAllUsers();
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        log.info("POST /api/users - ユーザーを作成: {}", user.getName());
        return userService.addUser(user);
    }
}
```

**期待されるログ出力**:
```
INFO  c.e.h.UserController - GET /api/users - すべてのユーザーを取得
INFO  c.e.h.UserController - POST /api/users - ユーザーを作成: 山田太郎
```

---

## 🐛 トラブルシューティング

### エラー: "cannot find symbol: class Data"

**原因**: Lombokの依存関係が正しくインストールされていない

**解決策**:

1. `pom.xml`にLombokの依存関係が追加されているか確認
2. Mavenの依存関係を再取得

```bash
./mvnw clean install
```

### エラー: "User()' in 'com.example.hellospringboot.User' cannot be applied"

**原因**: `@NoArgsConstructor`が不足している

**解決策**:

JSONデシリアライズにはデフォルトコンストラクタが必要です。`@NoArgsConstructor`を追加してください：

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    // ...
}
```

### エラー: IDEでgetter/setterが認識されない

**原因**: IDEにLombokプラグインがインストールされていない

**解決策（VSCode）**:

1. 拡張機能「Lombok Annotations Support for VS Code」をインストール
2. VSCodeを再起動
3. Java Language Serverをリロード（`Cmd+Shift+P` → "Java: Clean Java Language Server Workspace"）

**解決策（IntelliJ IDEA）**:

1. Settings → Plugins → "Lombok"で検索してインストール
2. Settings → Build, Execution, Deployment → Compiler → Annotation Processors → "Enable annotation processing"にチェック
3. IDEを再起動

### エラー: "log cannot be resolved"

**原因**: `@Slf4j`を付けたが、SLF4Jの依存関係がない

**解決策**:

Spring Boot Starterには既にSLF4Jが含まれています。以下を確認してください：

1. `@Slf4j`アノテーションが正しくインポートされているか

```java
import lombok.extern.slf4j.Slf4j;
```

2. コンパイルエラーが出る場合は、クリーンビルド

```bash
./mvnw clean compile
```

### 警告: "Generating equals/hashCode implementation but without a call to superclass"

**原因**: 継承しているクラスで`@EqualsAndHashCode`を使っている場合、親クラスのフィールドが考慮されない

**解決策**:

親クラスのequals/hashCodeを呼び出すように設定：

```java
@EqualsAndHashCode(callSuper = true)
public class AdminUser extends User {
    private String role;
}
```

---

## 💡 補足: Lombokを使う際のベストプラクティス

### 1. @Dataは慎重に使う

`@Data`は便利ですが、すべてのフィールドにsetterが生成されるため、不変性が損なわれます。

**推奨**:

- **不変オブジェクト**（変更されないデータ）: `@Value`を使う
- **可変オブジェクト**（変更されるデータ）: `@Data`を使う

```java
import lombok.Value;

@Value  // すべてのフィールドがfinalになり、setterは生成されない
public class UserDto {
    Long id;
    String name;
    String email;
}
```

### 2. @RequiredArgsConstructorでDI

Spring Bootでは、`@Autowired`よりもコンストラクタインジェクションが推奨されます。

```java
@Service
@RequiredArgsConstructor  // finalフィールドのコンストラクタを自動生成
public class UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;
}
```

### 3. @Builderで可読性向上

多くのフィールドを持つオブジェクトを作成する際は、`@Builder`を使うと可読性が向上します。

```java
@Data
@Builder
public class SearchCriteria {
    private String keyword;
    private Integer minAge;
    private Integer maxAge;
    private String sortBy;
}

// 使用例
SearchCriteria criteria = SearchCriteria.builder()
    .keyword("Spring Boot")
    .minAge(20)
    .maxAge(40)
    .sortBy("name")
    .build();
```

### 4. @Slf4jでロギング

ロガーフィールドを手動で書く代わりに、`@Slf4j`を使いましょう。

```java
@Slf4j
@Service
public class UserService {
    public void processUser(User user) {
        log.debug("ユーザー処理開始: {}", user.getId());
        // 処理
        log.info("ユーザー処理完了: {}", user.getId());
    }
}
```

### 5. Lombokを使わない方が良い場面

以下の場合は、Lombokを使わずに手動で書くことを検討してください：

- ❌ **複雑なロジックを持つメソッド**: カスタムのequals/hashCode/toString
- ❌ **パフォーマンスが重要な部分**: 自動生成されたコードが最適でない可能性
- ❌ **チームがLombokに不慣れ**: 学習コストを考慮

---

## 📚 このステップで学んだこと

- ✅ **Lombokの目的**: ボイラープレートコードを削減し、コードを簡潔にする
- ✅ **@Data**: getter/setter/toString/equals/hashCodeをまとめて生成
- ✅ **@NoArgsConstructor/@AllArgsConstructor**: コンストラクタを自動生成
- ✅ **@RequiredArgsConstructor**: finalフィールドのコンストラクタを生成し、DIを簡潔に
- ✅ **@Builder**: Builderパターンでオブジェクト生成を流暢に
- ✅ **@Slf4j**: ロガーフィールドを自動生成
- ✅ **コード量削減**: 54行→13行（76%削減）の実例
- ✅ **責任分離**: Controller → Service の設計パターン

---

## 🎉 Phase 1完了おめでとうございます！

お疲れ様でした！これで**Phase 1: Spring Boot基礎**の全5ステップが完了しました！

### Phase 1で学んだこと

- ✅ **Step 1**: Hello World REST APIでSpring Bootの基本
- ✅ **Step 2**: パスパラメータとクエリパラメータ
- ✅ **Step 3**: POSTリクエストとリクエストボディ
- ✅ **Step 4**: application.ymlで設定管理
- ✅ **Step 5**: Lombokで簡潔なコード

あなたはもう、**REST APIを構築する基本的なスキル**を身につけました！

---

## ➡️ 次のPhaseへ

さあ、次は**Phase 2: データベース連携の基礎**に進みましょう！

### [Phase 2の準備ガイド](../phase2/PREPARE.md)

Phase 2では、以下を学びます：

1. **MySQL環境構築**: データベースのセットアップ
2. **Spring Data JPA**: データベース操作を簡単に
3. **CRUD操作**: Create/Read/Update/Delete
4. **トランザクション管理**: データの整合性を保つ
5. **カスタムクエリ**: 複雑な検索処理
6. **リレーションシップ**: 1対多の関係を扱う

これまでインメモリ（メモリ上）でデータを管理していましたが、Phase 2からは**本物のデータベース（MySQL）**を使います。アプリケーションを再起動してもデータが消えなくなり、実用的なアプリケーションに近づきます！

準備ができたら、[Phase 2の準備ガイド](../phase2/PREPARE.md)に進んでください。

Happy Coding! 🚀
