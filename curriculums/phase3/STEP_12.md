# Step 12: MyBatisの基礎

## 🎯 このステップの目標

- MyBatisとは何か、なぜ必要なのかを理解できる
- Spring BootでMyBatisを設定できる
- XMLマッパーとアノテーションベースのクエリを書ける
- MyBatisでCRUD操作を実装できる
- JPAとMyBatisの違いを理解できる

**所要時間**: 約60分

---

## 📋 事前準備

- [Phase 2](../phase2/STEP_11.md)が完了していること
- MySQLコンテナが起動していること
- `Product`エンティティと`ProductRepository`（JPA）が実装済みであること

---

## 🧩 MyBatisとは

### MyBatisの特徴

**MyBatis** は、SQLを直接記述できるJavaの永続化フレームワークです。

```
┌─────────────────────────────────────────┐
│  Spring Data JPA (Hibernate)            │
│  ・メソッド名でクエリ自動生成             │
│  ・抽象度が高い                          │
│  ・SQLを意識しにくい                     │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  MyBatis                                │
│  ・SQLを直接記述                         │
│  ・細かい制御が可能                       │
│  ・パフォーマンスチューニングしやすい      │
└─────────────────────────────────────────┘
```

### JPAとMyBatisの比較

| 観点 | JPA (Hibernate) | MyBatis |
|---|---|---|
| **SQL記述** | 自動生成（JPQL/Criteria） | 手動で記述 |
| **学習コスト** | 高い（ORM概念の理解が必要） | 低い（SQLが書ければOK） |
| **複雑なクエリ** | 難しい | 簡単 |
| **保守性** | エンティティ中心 | SQL中心 |
| **パフォーマンス** | N+1問題に注意 | SQLを最適化しやすい |
| **推奨用途** | シンプルなCRUD | 複雑な検索、集計、レポート |

### MyBatisが適している場面

- ✅ 複雑なJOIN、サブクエリが必要
- ✅ 既存のSQLを流用したい
- ✅ パフォーマンスを細かくチューニングしたい
- ✅ 動的なSQLを構築したい
- ✅ レガシーDBとの連携

### JPAが適している場面

- ✅ シンプルなCRUD操作
- ✅ エンティティ間のリレーションシップが複雑
- ✅ トランザクション管理を簡潔にしたい
- ✅ キャッシュ機能を活用したい

---

## 🚀 ステップ1: MyBatisの依存関係追加

### 1-1. pom.xmlに依存関係を追加

`pom.xml`に以下を追加します：

```xml
<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- MyBatis Spring Boot Starter -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.3</version>
</dependency>

<!-- MySQL Connector -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 1-2. 依存関係の解説

#### `mybatis-spring-boot-starter`

MyBatisをSpring Bootで簡単に使えるようにするスターターです。

**含まれるもの**:
- MyBatisコア
- MyBatis-Spring連携
- 自動設定

**バージョン指定**:
Spring Boot 3.5.8では、MyBatis 3.0.3を使用します。

---

## 🚀 ステップ2: application.yamlの設定

### 2-1. MyBatis設定を追加

`src/main/resources/application.yaml`に以下を追加します：

```yaml
spring:
  application:
    name: hello-spring-boot

  datasource:
    url: jdbc:mysql://localhost:3306/spring_boot_db?useSSL=false&serverTimezone=Asia/Tokyo&characterEncoding=UTF-8
    username: springuser
    password: springpass
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect

# MyBatis設定
mybatis:
  # Mapperインターフェースの場所
  mapper-locations: classpath:mapper/**/*.xml
  # 型エイリアスのパッケージ
  type-aliases-package: com.example.hellospringboot
  configuration:
    # キャメルケース ↔ スネークケース自動変換
    map-underscore-to-camel-case: true
    # ログ出力
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

server:
  port: 8080

app:
  name: Hello Spring Boot Application
  version: 1.0.0
  welcome-message: Welcome to Spring Boot Configuration Management!
  description: This is a demo application for learning Spring Boot configuration.
```

### 2-2. 設定の解説

#### `mapper-locations`

XMLマッパーファイルの配置場所を指定します。

- `classpath:mapper/**/*.xml`: `src/main/resources/mapper/`以下の全XMLファイル
- ワイルドカード`**`でサブディレクトリも対象

#### `type-aliases-package`

Javaクラスの型エイリアスを自動設定します。

**例**:
- `com.example.hellospringboot.User` → `User`
- XML内で完全修飾名を書かずに済む

#### `map-underscore-to-camel-case`

データベースのスネークケース（`user_name`）とJavaのキャメルケース（`userName`）を自動変換します。

| DB | Java |
|---|---|
| `user_name` | `userName` |
| `created_at` | `createdAt` |
| `category_id` | `categoryId` |

---

## 🚀 ステップ3: Userエンティティの作成

### 3-1. Userエンティティ

MyBatis用の`User`クラスを作成します（JPAアノテーションは不要）。

**ファイルパス**: `src/main/java/com/example/hellospringboot/User.java`

```java
package com.example.hellospringboot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    private Long id;
    private String name;
    private String email;
    private Integer age;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 3-2. JPAエンティティとの違い

| JPA Entity | MyBatis Model |
|---|---|
| `@Entity` | 不要 |
| `@Table` | 不要 |
| `@Id`, `@GeneratedValue` | 不要 |
| `@Column` | 不要 |
| `@PrePersist`, `@PreUpdate` | 不要 |

**理由**:
MyBatisはSQLを直接記述するため、ORマッピングのアノテーションは不要です。

---

## 🚀 ステップ4: UserMapperインターフェースの作成

### 4-1. Mapperインターフェース

**ファイルパス**: `src/main/java/com/example/hellospringboot/UserMapper.java`

```java
package com.example.hellospringboot;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {
    
    // アノテーションベースのクエリ
    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(Long id);
    
    @Select("SELECT * FROM users")
    List<User> findAll();
    
    @Insert("INSERT INTO users (name, email, age, created_at, updated_at) " +
            "VALUES (#{name}, #{email}, #{age}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);
    
    @Update("UPDATE users SET name = #{name}, email = #{email}, age = #{age}, " +
            "updated_at = NOW() WHERE id = #{id}")
    void update(User user);
    
    @Delete("DELETE FROM users WHERE id = #{id}")
    void delete(Long id);
    
    // XMLマッパーで定義（次のステップ）
    List<User> searchByName(String name);
    
    List<User> findByAgeGreaterThan(Integer age);
}
```

### 4-2. コードの解説

#### `@Mapper`

MyBatisのMapperインターフェースであることを示します。

**Spring Bootの自動設定**:
- `@Mapper`を付けると、Spring Bootが自動的にBeanとして登録
- `@MapperScan`でパッケージをスキャンさせることも可能

#### `@Select`, `@Insert`, `@Update`, `@Delete`

SQLを直接アノテーションに記述します。

**パラメータバインディング**:
- `#{id}`: プリペアドステートメントのパラメータ
- メソッド引数の名前と一致させる

#### `@Options(useGeneratedKeys = true, keyProperty = "id")`

INSERT時に自動生成されたIDを取得します。

- `useGeneratedKeys = true`: 自動生成キーを使用
- `keyProperty = "id"`: `User`オブジェクトの`id`フィールドにセット

---

## 🚀 ステップ5: XMLマッパーファイルの作成

### 5-1. ディレクトリ構成

```
src/main/resources/
└── mapper/
    └── UserMapper.xml
```

### 5-2. UserMapper.xmlの作成

**ファイルパス**: `src/main/resources/mapper/UserMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.example.hellospringboot.UserMapper">

    <!-- ResultMap定義 -->
    <resultMap id="userResultMap" type="User">
        <id property="id" column="id"/>
        <result property="name" column="name"/>
        <result property="email" column="email"/>
        <result property="age" column="age"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
    </resultMap>

    <!-- 名前で検索 -->
    <select id="searchByName" resultMap="userResultMap">
        SELECT * FROM users
        WHERE name LIKE CONCAT('%', #{name}, '%')
    </select>

    <!-- 年齢で絞り込み -->
    <select id="findByAgeGreaterThan" resultMap="userResultMap">
        SELECT * FROM users
        WHERE age &gt; #{age}
        ORDER BY age ASC
    </select>

</mapper>
```

### 5-3. XMLマッパーの解説

#### `<mapper namespace="...">`

Mapperインターフェースの完全修飾名を指定します。

#### `<resultMap>`

DBカラムとJavaオブジェクトのプロパティをマッピングします。

```xml
<resultMap id="userResultMap" type="User">
    <id property="id" column="id"/>           <!-- 主キー -->
    <result property="name" column="name"/>   <!-- 通常カラム -->
</resultMap>
```

**`map-underscore-to-camel-case`が有効なら省略可能**:
- `created_at` → `createdAt` は自動変換される

#### `<select id="searchByName">`

- `id`: Mapperインターフェースのメソッド名
- `resultMap`: 使用するResultMap
- `#{name}`: パラメータバインディング

#### XML内でのエスケープ

| 記号 | XML表記 |
|---|---|
| `<` | `&lt;` |
| `>` | `&gt;` |
| `&` | `&amp;` |

**例**:
```xml
WHERE age &gt; #{age}  <!-- age > #{age} -->
```

---

## 🚀 ステップ6: UserServiceとControllerの作成

### 6-1. UserService

**ファイルパス**: `src/main/java/com/example/hellospringboot/UserService.java`

```java
package com.example.hellospringboot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserMapper userMapper;
    
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userMapper.findById(id);
    }
    
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userMapper.findAll();
    }
    
    @Transactional
    public User create(User user) {
        userMapper.insert(user);
        return user;  // IDが自動セットされている
    }
    
    @Transactional
    public User update(Long id, User userDetails) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        
        user.setName(userDetails.getName());
        user.setEmail(userDetails.getEmail());
        user.setAge(userDetails.getAge());
        
        userMapper.update(user);
        return user;
    }
    
    @Transactional
    public void delete(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        userMapper.delete(id);
    }
    
    @Transactional(readOnly = true)
    public List<User> searchByName(String name) {
        return userMapper.searchByName(name);
    }
    
    @Transactional(readOnly = true)
    public List<User> findByAgeGreaterThan(Integer age) {
        return userMapper.findByAgeGreaterThan(age);
    }
}
```

### 6-2. UserController

**ファイルパス**: `src/main/java/com/example/hellospringboot/UserController.java`

```java
package com.example.hellospringboot;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @GetMapping
    public List<User> getAllUsers() {
        return userService.findAll();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }
    
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User created = userService.create(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @RequestBody User userDetails) {
        try {
            User updated = userService.update(id, userDetails);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            userService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/search")
    public List<User> searchUsers(@RequestParam String name) {
        return userService.searchByName(name);
    }
    
    @GetMapping("/age-greater-than")
    public List<User> findByAge(@RequestParam Integer age) {
        return userService.findByAgeGreaterThan(age);
    }
}
```

---

## ✅ ステップ7: 動作確認

### 7-1. usersテーブルを作成

```bash
docker compose exec mysql mysql -u springuser -pspringpass spring_boot_db -e "
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    age INT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
"
```

### 7-2. アプリケーションを再起動

```bash
./mvnw spring-boot:run
```

### 7-3. ユーザーを作成

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "田中太郎",
    "email": "tanaka@example.com",
    "age": 30
  }'
```

**期待される結果**:

```json
{
  "id": 1,
  "name": "田中太郎",
  "email": "tanaka@example.com",
  "age": 30,
  "createdAt": "2025-12-13T15:00:00",
  "updatedAt": "2025-12-13T15:00:00"
}
```

### 7-4. 全ユーザーを取得

```bash
curl http://localhost:8080/api/users
```

### 7-5. 名前で検索

```bash
curl "http://localhost:8080/api/users/search?name=田中"
```

### 7-6. 年齢で絞り込み

```bash
curl "http://localhost:8080/api/users/age-greater-than?age=25"
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: 動的SQLで複数条件検索

名前、メール、年齢の範囲で検索できるメソッドを実装してください。

**ヒント**: `<where>`と`<if>`を使います。

```xml
<select id="searchUsers" resultMap="userResultMap">
    SELECT * FROM users
    <where>
        <if test="name != null">
            AND name LIKE CONCAT('%', #{name}, '%')
        </if>
        <if test="email != null">
            AND email LIKE CONCAT('%', #{email}, '%')
        </if>
        <if test="minAge != null">
            AND age &gt;= #{minAge}
        </if>
        <if test="maxAge != null">
            AND age &lt;= #{maxAge}
        </if>
    </where>
</select>
```

### チャレンジ 2: 一括INSERT

複数のユーザーを一度に登録するメソッドを実装してください。

**ヒント**: `<foreach>`を使います。

```xml
<insert id="insertBatch">
    INSERT INTO users (name, email, age, created_at, updated_at)
    VALUES
    <foreach collection="users" item="user" separator=",">
        (#{user.name}, #{user.email}, #{user.age}, NOW(), NOW())
    </foreach>
</insert>
```

### チャレンジ 3: ページネーション

`LIMIT`と`OFFSET`を使ってページネーションを実装してください。

```java
@Select("SELECT * FROM users ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
List<User> findWithPagination(@Param("limit") int limit, @Param("offset") int offset);
```

---

## 🐛 トラブルシューティング

### エラー: "Invalid bound statement (not found)"

**原因**: XMLマッパーが見つからない、またはnamespaceが間違っている

**解決策**:

1. `application.yaml`の`mapper-locations`を確認
2. XMLの`namespace`がMapperインターフェースの完全修飾名と一致しているか確認
3. XMLの`<select id="..."/>`のidがメソッド名と一致しているか確認

```yaml
mybatis:
  mapper-locations: classpath:mapper/**/*.xml  # 正しいパス
```

### エラー: "Parameter 'XXX' not found"

**原因**: 複数パラメータの場合、`@Param`アノテーションが必要

**解決策**:

```java
// ❌ 悪い例
List<User> searchByNameAndAge(String name, Integer age);

// ✅ 良い例
List<User> searchByNameAndAge(@Param("name") String name, @Param("age") Integer age);
```

### エラー: "There is no getter for property named 'XXX'"

**原因**: ResultMapのプロパティ名がクラスのフィールド名と一致していない

**解決策**:

```xml
<!-- ❌ 悪い例 -->
<result property="userName" column="name"/>  <!-- UserクラスにuserNameフィールドが存在しない -->

<!-- ✅ 良い例 -->
<result property="name" column="name"/>
```

### SQLがログに出ない

**原因**: ログレベルが設定されていない

**解決策**: `application.yaml`に追加

```yaml
logging:
  level:
    com.example.hellospringboot: DEBUG
```

---

## 📚 このステップで学んだこと

- ✅ MyBatisはSQLを直接記述できる永続化フレームワークである
- ✅ JPAとMyBatisは用途によって使い分ける
- ✅ `@Mapper`でMapperインターフェースを定義できる
- ✅ `@Select`, `@Insert`, `@Update`, `@Delete`でアノテーションベースのクエリを書ける
- ✅ XMLマッパーで複雑なクエリを記述できる
- ✅ `<resultMap>`でDBカラムとJavaオブジェクトをマッピングできる
- ✅ `map-underscore-to-camel-case`でスネークケースとキャメルケースを自動変換できる
- ✅ MyBatisでも`@Transactional`によるトランザクション管理が可能

---

## 💡 補足: MyBatisとJPAの共存

### 同じプロジェクトで併用可能

Spring BootではMyBatisとJPAを同時に使用できます。

**推奨パターン**:
- **JPA**: シンプルなCRUD（Product, Category）
- **MyBatis**: 複雑な検索、レポート、集計（User）

### トランザクション管理

MyBatisとJPAは同じSpringのトランザクション管理を使用します。

```java
@Service
public class OrderService {
    private final ProductRepository productRepository;  // JPA
    private final UserMapper userMapper;                // MyBatis
    
    @Transactional  // 両方を同じトランザクションで管理
    public void createOrder(Long userId, Long productId) {
        User user = userMapper.findById(userId);
        Product product = productRepository.findById(productId).orElseThrow();
        // 処理
    }
}
```

---

## ➡️ 次のステップ

[Step 13: MyBatisで複雑なクエリ](STEP_13.md)へ進みましょう！

次のステップでは、動的SQL、JOINクエリ、集計関数など、MyBatisの強力な機能を学びます。
