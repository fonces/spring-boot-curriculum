# Step 13: MyBatisで複雑なクエリ

## 🎯 このステップの目標

- 動的SQLを使って柔軟な検索条件を実装できる
- JOIN句を使った複数テーブルの結合クエリを記述できる
- サブクエリやネストした結果マッピングを理解できる
- `<if>`, `<where>`, `<foreach>`, `<choose>`などの動的SQL要素を使いこなせる
- 実務でよくある複雑な検索・集計クエリを実装できる

**所要時間**: 約45分

---

## 📋 事前準備

- [Step 12: MyBatisの基礎](STEP_12.md)が完了していること
- `User`エンティティとMyBatis環境が構築済みであること
- MySQLコンテナが起動していること
- Spring Bootアプリケーションが起動できる状態であること

---

## 🧩 動的SQLとは

### 静的SQLの問題点

Step 12では、固定的なSQL文を使いました：

```java
@Select("SELECT * FROM users WHERE name LIKE CONCAT('%', #{name}, '%')")
List<User> searchByName(@Param("name") String name);
```

**問題点**:
- 検索条件が必須（nameがnullでもLIKE検索が実行される）
- 複数条件の組み合わせに対応できない
- 条件の有無で異なるメソッドを作る必要がある

### 動的SQLの利点

```xml
<select id="searchUsers" resultType="User">
    SELECT * FROM users
    <where>
        <if test="name != null and name != ''">
            AND name LIKE CONCAT('%', #{name}, '%')
        </if>
        <if test="minAge != null">
            AND age &gt;= #{minAge}
        </if>
        <if test="maxAge != null">
            AND age &lt;= #{maxAge}
        </if>
    </where>
    ORDER BY id
</select>
```

**メリット**:
- **条件の有無を動的に判定**: nullや空文字の場合は条件を追加しない
- **複数条件の組み合わせ**: name、minAge、maxAgeを自由に組み合わせ可能
- **SQLインジェクション対策**: プリペアドステートメントで安全
- **可読性**: XML内でロジックが明確

---

## 🚀 ステップ1: 動的検索機能の実装

### 1-1. 検索条件用のDTOを作成

複雑な検索条件を扱うため、専用のDTOを作成します。

**ファイルパス**: `src/main/java/com/example/hellospringboot/UserSearchCriteria.java`

```java
package com.example.hellospringboot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchCriteria {
    
    private String name;
    private String email;
    private Integer minAge;
    private Integer maxAge;
    private String sortBy;  // "name", "age", "createdAt"など
    private String sortOrder;  // "ASC" or "DESC"
}
```

### 1-2. UserMapper.xmlに動的検索クエリを追加

**ファイルパス**: `src/main/resources/mapper/UserMapper.xml`

既存のXMLファイルに以下のクエリを追加します：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.example.hellospringboot.UserMapper">

    <!-- 既存のresultMapとクエリはそのまま -->
    <resultMap id="UserResultMap" type="User">
        <id property="id" column="id"/>
        <result property="name" column="name"/>
        <result property="email" column="email"/>
        <result property="age" column="age"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
    </resultMap>

    <select id="searchByName" resultMap="UserResultMap">
        SELECT * FROM users
        WHERE name LIKE CONCAT('%', #{name}, '%')
        ORDER BY id
    </select>

    <select id="findByAgeGreaterThan" resultMap="UserResultMap">
        SELECT * FROM users
        WHERE age &gt; #{age}
        ORDER BY age DESC
    </select>

    <!-- 🆕 動的検索クエリ -->
    <select id="searchUsers" resultMap="UserResultMap">
        SELECT * FROM users
        <where>
            <if test="name != null and name != ''">
                AND name LIKE CONCAT('%', #{name}, '%')
            </if>
            <if test="email != null and email != ''">
                AND email LIKE CONCAT('%', #{email}, '%')
            </if>
            <if test="minAge != null">
                AND age &gt;= #{minAge}
            </if>
            <if test="maxAge != null">
                AND age &lt;= #{maxAge}
            </if>
        </where>
        <choose>
            <when test="sortBy != null and sortBy == 'name'">
                ORDER BY name
            </when>
            <when test="sortBy != null and sortBy == 'age'">
                ORDER BY age
            </when>
            <when test="sortBy != null and sortBy == 'createdAt'">
                ORDER BY created_at
            </when>
            <otherwise>
                ORDER BY id
            </otherwise>
        </choose>
        <if test="sortOrder != null and sortOrder == 'DESC'">
            DESC
        </if>
    </select>

</mapper>
```

### 1-3. コードの解説

#### `<where>` 要素
```xml
<where>
    <if test="name != null and name != ''">
        AND name LIKE CONCAT('%', #{name}, '%')
    </if>
</where>
```

**役割**:
- 自動的に`WHERE`句を生成
- 最初の`AND`を自動削除（`WHERE AND name`→`WHERE name`に）
- すべての`<if>`がfalseなら`WHERE`句自体を削除

**なぜ必要か**:
手動で`WHERE 1=1 AND ...`と書く必要がなくなり、SQLがすっきりします。

#### `<if>` 要素
```xml
<if test="name != null and name != ''">
    AND name LIKE CONCAT('%', #{name}, '%')
</if>
```

**test属性の条件式**:
- `name != null`: nameがnullでない
- `name != ''`: nameが空文字でない
- `and`: 両方の条件を満たす場合のみ

**動作**:
- 条件がtrueの場合のみ、タグ内のSQLが追加される
- falseの場合は無視される

#### `<choose>`, `<when>`, `<otherwise>`
```xml
<choose>
    <when test="sortBy == 'name'">
        ORDER BY name
    </when>
    <when test="sortBy == 'age'">
        ORDER BY age
    </when>
    <otherwise>
        ORDER BY id
    </otherwise>
</choose>
```

**役割**: Javaの`switch`文やif-else-ifと同じ

- `<when>`: 最初にtrueになった条件のSQLを使用
- `<otherwise>`: すべての`<when>`がfalseの場合のデフォルト

#### XMLでの比較演算子

| 記号 | 意味 | XML表記 |
|------|------|---------|
| `<` | 未満 | `&lt;` |
| `>` | より大きい | `&gt;` |
| `<=` | 以下 | `&lt;=` |
| `>=` | 以上 | `&gt;=` |

**理由**: XMLでは`<`や`>`がタグの一部と認識されるため、エスケープが必要

### 1-4. UserMapper.javaにメソッドを追加

**ファイルパス**: `src/main/java/com/example/hellospringboot/UserMapper.java`

```java
package com.example.hellospringboot;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {
    
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
    
    // XMLマッパーで定義されたメソッド
    List<User> searchByName(@Param("name") String name);
    
    List<User> findByAgeGreaterThan(@Param("age") Integer age);
    
    // 🆕 動的検索メソッド
    List<User> searchUsers(UserSearchCriteria criteria);
}
```

**ポイント**: 
- `UserSearchCriteria`オブジェクトを引数に取る
- MyBatisはオブジェクトのフィールド名を使ってXML内の`#{name}`などにマッピング

### 1-5. UserServiceに動的検索メソッドを追加

**ファイルパス**: `src/main/java/com/example/hellospringboot/UserService.java`

既存のサービスに以下のメソッドを追加します：

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
    
    // 既存のメソッドはそのまま
    public User findById(Long id) {
        return userMapper.findById(id);
    }
    
    public List<User> findAll() {
        return userMapper.findAll();
    }
    
    @Transactional
    public User create(User user) {
        userMapper.insert(user);
        return user;
    }
    
    @Transactional
    public User update(Long id, User user) {
        user.setId(id);
        userMapper.update(user);
        return userMapper.findById(id);
    }
    
    @Transactional
    public void delete(Long id) {
        userMapper.delete(id);
    }
    
    public List<User> searchByName(String name) {
        return userMapper.searchByName(name);
    }
    
    public List<User> findByAgeGreaterThan(Integer age) {
        return userMapper.findByAgeGreaterThan(age);
    }
    
    // 🆕 動的検索メソッド
    public List<User> searchUsers(UserSearchCriteria criteria) {
        return userMapper.searchUsers(criteria);
    }
}
```

### 1-6. UserControllerに動的検索エンドポイントを追加

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
    
    // 既存のエンドポイントはそのまま
    @GetMapping
    public List<User> getAllUsers() {
        return userService.findAll();
    }
    
    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.findById(id);
    }
    
    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.create(user);
    }
    
    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user) {
        return userService.update(id, user);
    }
    
    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.delete(id);
    }
    
    @GetMapping("/search")
    public List<User> searchByName(@RequestParam String name) {
        return userService.searchByName(name);
    }
    
    @GetMapping("/age-greater-than")
    public List<User> findByAgeGreaterThan(@RequestParam Integer age) {
        return userService.findByAgeGreaterThan(age);
    }
    
    // 🆕 動的検索エンドポイント
    @GetMapping("/advanced-search")
    public List<User> advancedSearch(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder) {
        
        UserSearchCriteria criteria = new UserSearchCriteria(
            name, email, minAge, maxAge, sortBy, sortOrder
        );
        return userService.searchUsers(criteria);
    }
}
```

**ポイント**:
- すべてのパラメータが`required = false`（オプショナル）
- 条件を組み合わせて柔軟な検索が可能

---

## 🚀 ステップ2: JOINクエリの実装

### 2-1. シナリオ: ユーザーの投稿記事を取得

ユーザーが複数の記事（Post）を持つ1対多の関係を実装します。

### 2-2. Postエンティティを作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/Post.java`

```java
package com.example.hellospringboot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 2-3. postsテーブルを作成

MySQLコンテナに接続してテーブルを作成します：

```bash
docker compose exec mysql mysql -u springuser -pspringpass spring_boot_db
```

```sql
CREATE TABLE posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

サンプルデータを挿入：

```sql
INSERT INTO posts (user_id, title, content) VALUES
(1, 'Spring Bootの始め方', 'Spring Bootは便利なフレームワークです'),
(1, 'MyBatisの使い方', '動的SQLが強力です'),
(2, '初めての投稿', 'よろしくお願いします'),
(2, 'データベース設計', '正規化が重要です');
```

### 2-4. ユーザーと投稿を結合したDTOを作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/UserWithPosts.java`

```java
package com.example.hellospringboot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserWithPosts {
    
    private Long id;
    private String name;
    private String email;
    private Integer age;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 投稿リスト
    private List<Post> posts;
}
```

### 2-5. UserMapper.xmlにJOINクエリを追加

**ファイルパス**: `src/main/resources/mapper/UserMapper.xml`

```xml
<!-- 投稿を含むユーザーのresultMap -->
<resultMap id="UserWithPostsResultMap" type="UserWithPosts">
    <id property="id" column="id"/>
    <result property="name" column="name"/>
    <result property="email" column="email"/>
    <result property="age" column="age"/>
    <result property="createdAt" column="created_at"/>
    <result property="updatedAt" column="updated_at"/>
    
    <!-- ネストした結果マッピング -->
    <collection property="posts" ofType="Post">
        <id property="id" column="post_id"/>
        <result property="userId" column="user_id"/>
        <result property="title" column="title"/>
        <result property="content" column="content"/>
        <result property="createdAt" column="post_created_at"/>
        <result property="updatedAt" column="post_updated_at"/>
    </collection>
</resultMap>

<!-- JOINクエリ -->
<select id="findUserWithPosts" resultMap="UserWithPostsResultMap">
    SELECT 
        u.id,
        u.name,
        u.email,
        u.age,
        u.created_at,
        u.updated_at,
        p.id AS post_id,
        p.user_id,
        p.title,
        p.content,
        p.created_at AS post_created_at,
        p.updated_at AS post_updated_at
    FROM users u
    LEFT JOIN posts p ON u.id = p.user_id
    WHERE u.id = #{userId}
    ORDER BY p.created_at DESC
</select>
```

### 2-6. コードの解説

#### `<collection>` 要素
```xml
<collection property="posts" ofType="Post">
    <id property="id" column="post_id"/>
    <result property="title" column="title"/>
</collection>
```

**役割**:
- **1対多の関係**をマッピング
- `property="posts"`: UserWithPostsクラスのpostsフィールド
- `ofType="Post"`: Listの要素型

**動作**:
同じuser.idを持つ複数の行を1つのUserWithPostsオブジェクトにまとめ、postsリストに格納します。

#### カラム名のエイリアス
```sql
p.id AS post_id,
p.created_at AS post_created_at
```

**なぜ必要か**:
- usersテーブルとpostsテーブルの両方に`id`, `created_at`が存在
- エイリアスで区別しないと、どちらの値かわからない

**マッピング**:
```xml
<id property="id" column="post_id"/>  <!-- Post.idにマッピング -->
<result property="createdAt" column="post_created_at"/>
```

### 2-7. UserMapper.javaにメソッドを追加

**ファイルパス**: `src/main/java/com/example/hellospringboot/UserMapper.java`

```java
@Mapper
public interface UserMapper {
    
    // 既存のメソッド...
    
    // 🆕 JOINクエリ
    UserWithPosts findUserWithPosts(@Param("userId") Long userId);
}
```

### 2-8. UserServiceにメソッドを追加

**ファイルパス**: `src/main/java/com/example/hellospringboot/UserService.java`

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserMapper userMapper;
    
    // 既存のメソッド...
    
    // 🆕 投稿付きユーザーを取得
    public UserWithPosts getUserWithPosts(Long userId) {
        return userMapper.findUserWithPosts(userId);
    }
}
```

### 2-9. UserControllerにエンドポイントを追加

**ファイルパス**: `src/main/java/com/example/hellospringboot/UserController.java`

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    // 既存のエンドポイント...
    
    // 🆕 投稿付きユーザーを取得
    @GetMapping("/{id}/with-posts")
    public UserWithPosts getUserWithPosts(@PathVariable Long id) {
        return userService.getUserWithPosts(id);
    }
}
```

---

## 🚀 ステップ3: `<foreach>`で複数IDを検索

### 3-1. シナリオ: 複数のユーザーを一括取得

```
GET /api/users/batch?ids=1,2,3
```

で複数ユーザーをまとめて取得します。

### 3-2. UserMapper.xmlに一括検索クエリを追加

**ファイルパス**: `src/main/resources/mapper/UserMapper.xml`

```xml
<!-- 複数IDで一括検索 -->
<select id="findByIds" resultMap="UserResultMap">
    SELECT * FROM users
    WHERE id IN
    <foreach collection="ids" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
    ORDER BY id
</select>
```

### 3-3. コードの解説

#### `<foreach>` 要素
```xml
<foreach collection="ids" item="id" open="(" separator="," close=")">
    #{id}
</foreach>
```

**属性**:
- `collection="ids"`: パラメータ名（リストや配列）
- `item="id"`: 繰り返しの各要素の変数名
- `open="("`: ループ開始前に追加する文字列
- `separator=","`: 要素間の区切り文字
- `close=")"`: ループ終了後に追加する文字列

**生成されるSQL**:
```sql
WHERE id IN (1, 2, 3)
```

**なぜ安全か**:
`#{id}`がプリペアドステートメントになるため、SQLインジェクションのリスクがありません。

### 3-4. UserMapper.javaにメソッドを追加

**ファイルパス**: `src/main/java/com/example/hellospringboot/UserMapper.java`

```java
@Mapper
public interface UserMapper {
    
    // 既存のメソッド...
    
    // 🆕 複数IDで一括検索
    List<User> findByIds(@Param("ids") List<Long> ids);
}
```

### 3-5. UserServiceにメソッドを追加

**ファイルパス**: `src/main/java/com/example/hellospringboot/UserService.java`

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserMapper userMapper;
    
    // 既存のメソッド...
    
    // 🆕 複数IDで一括取得
    public List<User> findByIds(List<Long> ids) {
        return userMapper.findByIds(ids);
    }
}
```

### 3-6. UserControllerにエンドポイントを追加

**ファイルパス**: `src/main/java/com/example/hellospringboot/UserController.java`

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    // 既存のエンドポイント...
    
    // 🆕 複数IDで一括取得
    @GetMapping("/batch")
    public List<User> getUsersByIds(@RequestParam List<Long> ids) {
        return userService.findByIds(ids);
    }
}
```

---

## 🚀 ステップ4: サブクエリで集計情報を取得

### 4-1. シナリオ: 投稿数付きユーザー一覧

各ユーザーの投稿数をサブクエリで取得します。

### 4-2. UserWithPostCountを作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/UserWithPostCount.java`

```java
package com.example.hellospringboot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserWithPostCount {
    
    private Long id;
    private String name;
    private String email;
    private Integer age;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 投稿数
    private Long postCount;
}
```

### 4-3. UserMapper.xmlにサブクエリを追加

**ファイルパス**: `src/main/resources/mapper/UserMapper.xml`

```xml
<!-- 投稿数付きユーザーのresultMap -->
<resultMap id="UserWithPostCountResultMap" type="UserWithPostCount">
    <id property="id" column="id"/>
    <result property="name" column="name"/>
    <result property="email" column="email"/>
    <result property="age" column="age"/>
    <result property="createdAt" column="created_at"/>
    <result property="updatedAt" column="updated_at"/>
    <result property="postCount" column="post_count"/>
</resultMap>

<!-- サブクエリで投稿数を取得 -->
<select id="findAllUsersWithPostCount" resultMap="UserWithPostCountResultMap">
    SELECT 
        u.id,
        u.name,
        u.email,
        u.age,
        u.created_at,
        u.updated_at,
        (SELECT COUNT(*) FROM posts p WHERE p.user_id = u.id) AS post_count
    FROM users u
    ORDER BY post_count DESC, u.id
</select>
```

### 4-4. コードの解説

#### サブクエリ
```sql
(SELECT COUNT(*) FROM posts p WHERE p.user_id = u.id) AS post_count
```

**役割**:
- 各ユーザーの投稿数を計算
- メインクエリの各行ごとにサブクエリが実行される

**エイリアス**:
`AS post_count`でカラム名を指定し、resultMapでマッピングします。

**パフォーマンス考慮**:
- ユーザー数が多い場合、JOINとGROUP BYの方が効率的な場合もあります
- このクエリは可読性を優先した例です

### 4-5. UserMapper.javaにメソッドを追加

**ファイルパス**: `src/main/java/com/example/hellospringboot/UserMapper.java`

```java
@Mapper
public interface UserMapper {
    
    // 既存のメソッド...
    
    // 🆕 投稿数付きユーザー一覧
    List<UserWithPostCount> findAllUsersWithPostCount();
}
```

### 4-6. UserServiceにメソッドを追加

**ファイルパス**: `src/main/java/com/example/hellospringboot/UserService.java`

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserMapper userMapper;
    
    // 既存のメソッド...
    
    // 🆕 投稿数付きユーザー一覧
    public List<UserWithPostCount> findAllUsersWithPostCount() {
        return userMapper.findAllUsersWithPostCount();
    }
}
```

### 4-7. UserControllerにエンドポイントを追加

**ファイルパス**: `src/main/java/com/example/hellospringboot/UserController.java`

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    // 既存のエンドポイント...
    
    // 🆕 投稿数付きユーザー一覧
    @GetMapping("/with-post-count")
    public List<UserWithPostCount> getUsersWithPostCount() {
        return userService.findAllUsersWithPostCount();
    }
}
```

---

## ✅ 動作確認

### 1. アプリケーションを起動

```bash
cd workspace/hello-spring-boot
./mvnw spring-boot:run
```

### 2. 動的検索をテスト

#### 2-1. 名前と年齢範囲で検索

```bash
curl -G "http://localhost:8080/api/users/advanced-search" \
  --data-urlencode "name=太郎" \
  --data-urlencode "minAge=25" \
  --data-urlencode "maxAge=35"
```

**期待される結果**:
```json
[
  {
    "id": 1,
    "name": "田中太郎",
    "email": "tanaka.updated@example.com",
    "age": 31,
    "createdAt": "2025-12-13T05:43:02",
    "updatedAt": "2025-12-13T05:46:19"
  }
]
```

#### 2-2. ソート順を指定

```bash
curl -G "http://localhost:8080/api/users/advanced-search" \
  --data-urlencode "sortBy=age" \
  --data-urlencode "sortOrder=DESC"
```

**期待される結果**: 年齢の降順でユーザーが返される

#### 2-3. 条件なしで全件取得

```bash
curl "http://localhost:8080/api/users/advanced-search"
```

### 3. JOINクエリをテスト

```bash
curl "http://localhost:8080/api/users/1/with-posts"
```

**期待される結果**:
```json
{
  "id": 1,
  "name": "田中太郎",
  "email": "tanaka.updated@example.com",
  "age": 31,
  "createdAt": "2025-12-13T05:43:02",
  "updatedAt": "2025-12-13T05:46:19",
  "posts": [
    {
      "id": 2,
      "userId": 1,
      "title": "MyBatisの使い方",
      "content": "動的SQLが強力です",
      "createdAt": "2025-12-13T...",
      "updatedAt": "2025-12-13T..."
    },
    {
      "id": 1,
      "userId": 1,
      "title": "Spring Bootの始め方",
      "content": "Spring Bootは便利なフレームワークです",
      "createdAt": "2025-12-13T...",
      "updatedAt": "2025-12-13T..."
    }
  ]
}
```

### 4. 一括検索をテスト

```bash
curl "http://localhost:8080/api/users/batch?ids=1,2"
```

**期待される結果**: ID 1と2のユーザーが返される

### 5. 投稿数付きユーザー一覧をテスト

```bash
curl "http://localhost:8080/api/users/with-post-count"
```

**期待される結果**:
```json
[
  {
    "id": 1,
    "name": "田中太郎",
    "email": "tanaka.updated@example.com",
    "age": 31,
    "createdAt": "2025-12-13T05:43:02",
    "updatedAt": "2025-12-13T05:46:19",
    "postCount": 2
  },
  {
    "id": 2,
    "name": "佐藤花子",
    "email": "sato@example.com",
    "age": 25,
    "createdAt": "2025-12-13T05:43:08",
    "updatedAt": "2025-12-13T05:43:08",
    "postCount": 2
  }
]
```

---

## 🎨 チャレンジ課題

基本が理解できたら、以下にチャレンジしてみましょう：

### チャレンジ1: ページネーション付き動的検索

`searchUsers`にLIMITとOFFSETを追加してページング機能を実装してください。

**ヒント**:
```xml
<if test="limit != null">
    LIMIT #{limit}
</if>
<if test="offset != null">
    OFFSET #{offset}
</if>
```

`UserSearchCriteria`に`limit`と`offset`フィールドを追加します。

### チャレンジ2: 投稿のタグ検索

`posts`テーブルに`tags`カラム（カンマ区切り）を追加し、特定のタグを含む投稿を検索してください。

**ヒント**:
```xml
<if test="tag != null">
    AND FIND_IN_SET(#{tag}, tags) > 0
</if>
```

### チャレンジ3: 複雑なJOIN（3テーブル結合）

`users` → `posts` → `comments`の3テーブルをJOINし、ユーザーと投稿とコメントをまとめて取得してください。

**ヒント**:
```xml
<collection property="posts" ofType="Post">
    <id property="id" column="post_id"/>
    <collection property="comments" ofType="Comment">
        <id property="id" column="comment_id"/>
    </collection>
</collection>
```

---

## 🐛 トラブルシューティング

### エラー1: "There is no getter for property named 'name' in 'class UserSearchCriteria'"

**原因**: Lombokの`@Data`が正しく動作していない、またはフィールド名のスペルミス

**解決策**:
1. `UserSearchCriteria`に`@Data`がついているか確認
2. XML内の`#{name}`とJavaのフィールド名が一致しているか確認
3. プロジェクトをクリーンビルド: `./mvnw clean compile`

### エラー2: XMLのパースエラー "The content of elements must consist of well-formed character data"

**原因**: XML内で`<`や`>`を直接使っている

**解決策**:
```xml
<!-- ❌ NG -->
age >= #{minAge}

<!-- ✅ OK -->
age &gt;= #{minAge}
```

### エラー3: "Invalid bound statement (not found): UserMapper.searchUsers"

**原因**:
- XMLのnamespaceが間違っている
- メソッド名とXMLのid属性が一致していない
- XMLファイルが`src/main/resources/mapper/`以下にない

**解決策**:
1. XMLの`namespace`を確認: `com.example.hellospringboot.UserMapper`
2. `<select id="searchUsers">`とメソッド名が一致しているか確認
3. `application.yaml`の`mapper-locations`を確認

### エラー4: JOINで重複したデータが返される

**原因**: resultMapの`<id>`要素が不足している

**解決策**:
```xml
<resultMap id="UserWithPostsResultMap" type="UserWithPosts">
    <id property="id" column="id"/>  <!-- 必須！ -->
    <collection property="posts" ofType="Post">
        <id property="id" column="post_id"/>  <!-- 必須！ -->
    </collection>
</resultMap>
```

`<id>`はMyBatisが行をグループ化するためのキーとして使います。

### エラー5: `<foreach>`で "Parameter 'ids' not found"

**原因**: `@Param("ids")`アノテーションが不足している

**解決策**:
```java
// ❌ NG
List<User> findByIds(List<Long> ids);

// ✅ OK
List<User> findByIds(@Param("ids") List<Long> ids);
```

---

## 💡 補足: 動的SQLのベストプラクティス

### 1. `<where>`と`<trim>`の使い分け

#### `<where>` - 基本的な条件分岐
```xml
<where>
    <if test="name != null">AND name = #{name}</if>
    <if test="age != null">AND age = #{age}</if>
</where>
```

#### `<trim>` - より柔軟な制御
```xml
<trim prefix="WHERE" prefixOverrides="AND |OR ">
    <if test="name != null">AND name = #{name}</if>
    <if test="age != null">AND age = #{age}</if>
</trim>
```

**使い分け**:
- 単純なWHERE句: `<where>`
- UPDATEのSET句など: `<trim>`

### 2. `<set>`でUPDATE文を動的化

```xml
<update id="updateUserSelective">
    UPDATE users
    <set>
        <if test="name != null">name = #{name},</if>
        <if test="email != null">email = #{email},</if>
        <if test="age != null">age = #{age},</if>
        updated_at = NOW()
    </set>
    WHERE id = #{id}
</update>
```

**メリット**: 指定されたフィールドだけ更新（部分更新）

### 3. `<sql>`で再利用可能なSQL片を定義

```xml
<sql id="userColumns">
    id, name, email, age, created_at, updated_at
</sql>

<select id="findAll" resultMap="UserResultMap">
    SELECT <include refid="userColumns"/>
    FROM users
</select>

<select id="findById" resultMap="UserResultMap">
    SELECT <include refid="userColumns"/>
    FROM users
    WHERE id = #{id}
</select>
```

**メリット**: DRY原則（Don't Repeat Yourself）

### 4. N+1問題を避ける

#### ❌ N+1問題が発生する例
```java
// ユーザー一覧を取得（1回のクエリ）
List<User> users = userMapper.findAll();

// 各ユーザーの投稿を取得（N回のクエリ）
for (User user : users) {
    List<Post> posts = postMapper.findByUserId(user.getId());
}
```

**問題**: ユーザーが100人いると、合計101回のクエリが実行される

#### ✅ JOINで1回のクエリに
```xml
<select id="findAllUsersWithPosts" resultMap="UserWithPostsResultMap">
    SELECT 
        u.id, u.name, u.email,
        p.id AS post_id, p.title, p.content
    FROM users u
    LEFT JOIN posts p ON u.id = p.user_id
    ORDER BY u.id, p.created_at DESC
</select>
```

**メリット**: 1回のクエリで全データを取得

---

## 📚 このステップで学んだこと

- ✅ **動的SQL**: `<if>`, `<where>`, `<choose>`で柔軟な条件分岐
- ✅ **JOIN**: 複数テーブルを結合してリレーションデータを取得
- ✅ **`<collection>`**: 1対多の関係をネストしたオブジェクトにマッピング
- ✅ **`<foreach>`**: IN句で複数の値を扱う
- ✅ **サブクエリ**: 集計情報を効率的に取得
- ✅ **エイリアス**: カラム名の衝突を回避
- ✅ **ベストプラクティス**: N+1問題の回避、SQL片の再利用

---

## ➡️ 次のステップ

[Step 14: JPAとMyBatisの使い分け](STEP_14.md)へ進みましょう！

次のステップでは、JPAとMyBatisそれぞれの長所短所を学び、実務でどのように使い分けるかを理解します。
