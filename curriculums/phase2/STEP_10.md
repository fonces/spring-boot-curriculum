# Step 10: カスタムクエリ

## 🎯 このステップの目標

- Spring Data JPAのクエリメソッド命名規則を理解する
- `findBy...`メソッドで自動的にクエリを生成する
- `@Query`アノテーションでJPQLを記述する
- ネイティブSQLクエリを使用する
- 複雑な検索条件を持つAPIを実装する

**所要時間**: 約1時間

---

## 📋 事前準備

- Step 9までのトランザクション管理が理解できていること
- UserエンティティとUserRepositoryが動作していること

**Step 9をまだ完了していない場合**: [Step 9: @Transactionalでトランザクション管理](STEP_9.md)を先に進めてください。

---

## 💡 Spring Data JPAのクエリメソッド

### クエリメソッドとは？

**メソッド名から自動的にクエリを生成する機能**

```java
// メソッド名だけでクエリが生成される
List<User> findByName(String name);
// → SELECT * FROM users WHERE name = ?
```

**メリット**:
- ✅ SQLを書かなくてOK
- ✅ タイプセーフ（コンパイル時にエラー検出）
- ✅ 可読性が高い

---

## 🚀 ステップ1: 基本的なクエリメソッド

### 1-1. UserRepositoryにメソッド追加

`UserRepository.java`に以下のメソッドを追加します。

**ファイルパス**: `src/main/java/com/example/hellospringboot/repository/UserRepository.java`

```java
package com.example.hellospringboot.repository;

import com.example.hellospringboot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 名前で検索（完全一致）
    Optional<User> findByName(String name);

    // メールアドレスで検索
    Optional<User> findByEmail(String email);

    // 年齢で検索
    List<User> findByAge(Integer age);

    // 年齢が指定値より大きいユーザーを検索
    List<User> findByAgeGreaterThan(Integer age);

    // 年齢が指定値以上のユーザーを検索
    List<User> findByAgeGreaterThanEqual(Integer age);

    // 年齢が範囲内のユーザーを検索
    List<User> findByAgeBetween(Integer minAge, Integer maxAge);

    // 名前に特定の文字列を含むユーザーを検索
    List<User> findByNameContaining(String keyword);

    // 名前が特定の文字列で始まるユーザーを検索
    List<User> findByNameStartingWith(String prefix);

    // 複数条件（AND）
    List<User> findByNameAndAge(String name, Integer age);

    // 複数条件（OR）
    List<User> findByNameOrEmail(String name, String email);

    // 並び替え
    List<User> findByAgeGreaterThanOrderByNameAsc(Integer age);
}
```

### 1-2. 命名規則の解説

| キーワード | 例 | 生成されるSQL |
|-----------|----|--------------| 
| `findBy` | `findByName(String name)` | `WHERE name = ?` |
| `GreaterThan` | `findByAgeGreaterThan(Integer age)` | `WHERE age > ?` |
| `LessThan` | `findByAgeLessThan(Integer age)` | `WHERE age < ?` |
| `Between` | `findByAgeBetween(Integer min, Integer max)` | `WHERE age BETWEEN ? AND ?` |
| `Like` / `Containing` | `findByNameContaining(String keyword)` | `WHERE name LIKE %?%` |
| `StartingWith` | `findByNameStartingWith(String prefix)` | `WHERE name LIKE ?%` |
| `EndingWith` | `findByNameEndingWith(String suffix)` | `WHERE name LIKE %?` |
| `And` | `findByNameAndAge(String name, Integer age)` | `WHERE name = ? AND age = ?` |
| `Or` | `findByNameOrEmail(String name, String email)` | `WHERE name = ? OR email = ?` |
| `OrderBy` | `findByAgeOrderByNameAsc(Integer age)` | `WHERE age = ? ORDER BY name ASC` |
| `IsNull` | `findByAgeIsNull()` | `WHERE age IS NULL` |
| `IsNotNull` | `findByAgeIsNotNull()` | `WHERE age IS NOT NULL` |

### 1-3. UserServiceにメソッド追加

`UserService.java`に以下を追加：

**ファイルパス**: `src/main/java/com/example/hellospringboot/service/UserService.java`

```java
/**
 * 名前で検索
 */
public Optional<User> getUserByName(String name) {
    return userRepository.findByName(name);
}

/**
 * 名前に特定の文字列を含むユーザーを検索
 */
public List<User> searchUsersByName(String keyword) {
    return userRepository.findByNameContaining(keyword);
}

/**
 * 年齢が指定値以上のユーザーを検索
 */
public List<User> getUsersByMinAge(Integer minAge) {
    return userRepository.findByAgeGreaterThanEqual(minAge);
}

/**
 * 年齢範囲でユーザーを検索
 */
public List<User> getUsersByAgeRange(Integer minAge, Integer maxAge) {
    return userRepository.findByAgeBetween(minAge, maxAge);
}
```

### 1-4. UserControllerにエンドポイント追加

`UserController.java`に以下を追加：

```java
/**
 * 名前で検索
 * GET /api/users/search/name?name=Taro
 */
@GetMapping("/search/name")
public ResponseEntity<List<User>> searchByName(@RequestParam String name) {
    List<User> users = userService.searchUsersByName(name);
    return ResponseEntity.ok(users);
}

/**
 * 年齢範囲で検索
 * GET /api/users/search/age-range?min=20&max=30
 */
@GetMapping("/search/age-range")
public ResponseEntity<List<User>> searchByAgeRange(
        @RequestParam Integer min,
        @RequestParam Integer max) {
    List<User> users = userService.getUsersByAgeRange(min, max);
    return ResponseEntity.ok(users);
}

/**
 * 最小年齢で検索
 * GET /api/users/search/min-age?age=25
 */
@GetMapping("/search/min-age")
public ResponseEntity<List<User>> searchByMinAge(@RequestParam Integer age) {
    List<User> users = userService.getUsersByMinAge(age);
    return ResponseEntity.ok(users);
}
```

### 1-5. 動作確認

テスト用ユーザーを作成：

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Taro Yamada","email":"taro@example.com","age":30}'

curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Hanako Tanaka","email":"hanako@example.com","age":25}'

curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Taro Suzuki","email":"taro.s@example.com","age":28}'
```

名前で部分一致検索：

```bash
curl "http://localhost:8080/api/users/search/name?name=Taro"
```

**期待される結果**: 名前に "Taro" を含むユーザーが返る

年齢範囲で検索：

```bash
curl "http://localhost:8080/api/users/search/age-range?min=25&max=30"
```

**期待される結果**: 年齢が25〜30のユーザーが返る

---

## 🚀 ステップ2: @Queryアノテーション（JPQL）

### 2-1. JPQLとは？

**JPQL (Java Persistence Query Language)** = エンティティに対するクエリ言語

**SQLとの違い**:
- SQL: テーブルとカラムを指定
- JPQL: エンティティとフィールドを指定

```sql
-- SQL
SELECT * FROM users WHERE name LIKE '%Taro%'

-- JPQL
SELECT u FROM User u WHERE u.name LIKE '%Taro%'
```

### 2-2. @Queryを使った実装

`UserRepository.java`に以下を追加：

```java
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ... 既存のメソッド ...

    /**
     * JPQLでメールドメインを検索
     */
    @Query("SELECT u FROM User u WHERE u.email LIKE %:domain%")
    List<User> findByEmailDomain(@Param("domain") String domain);

    /**
     * JPQLで年齢の平均を取得
     */
    @Query("SELECT AVG(u.age) FROM User u")
    Double getAverageAge();

    /**
     * JPQLで特定の年齢以上のユーザー数を取得
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.age >= :minAge")
    Long countByMinAge(@Param("minAge") Integer minAge);

    /**
     * JPQLで名前とメールアドレスのみを取得（射影）
     */
    @Query("SELECT u.name, u.email FROM User u WHERE u.age >= :minAge")
    List<Object[]> findNameAndEmailByMinAge(@Param("minAge") Integer minAge);

    /**
     * JPQLで複雑な検索条件
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:name IS NULL OR u.name LIKE %:name%) AND " +
           "(:minAge IS NULL OR u.age >= :minAge) AND " +
           "(:maxAge IS NULL OR u.age <= :maxAge)")
    List<User> searchUsers(
        @Param("name") String name,
        @Param("minAge") Integer minAge,
        @Param("maxAge") Integer maxAge
    );
}
```

### 2-3. @Paramアノテーション

```java
@Query("SELECT u FROM User u WHERE u.email LIKE %:domain%")
List<User> findByEmailDomain(@Param("domain") String domain);
```

- `@Param("domain")`: パラメータ名を明示的に指定
- `:domain`: JPQL内でパラメータを参照

### 2-4. UserServiceに追加

```java
/**
 * メールドメインで検索
 */
public List<User> getUsersByEmailDomain(String domain) {
    return userRepository.findByEmailDomain(domain);
}

/**
 * ユーザーの平均年齢を取得
 */
public Double getAverageAge() {
    return userRepository.getAverageAge();
}

/**
 * 複雑な検索条件
 */
public List<User> searchUsers(String name, Integer minAge, Integer maxAge) {
    return userRepository.searchUsers(name, minAge, maxAge);
}
```

### 2-5. UserControllerに追加

```java
/**
 * メールドメインで検索
 * GET /api/users/search/email-domain?domain=example.com
 */
@GetMapping("/search/email-domain")
public ResponseEntity<List<User>> searchByEmailDomain(@RequestParam String domain) {
    List<User> users = userService.getUsersByEmailDomain(domain);
    return ResponseEntity.ok(users);
}

/**
 * 平均年齢を取得
 * GET /api/users/stats/average-age
 */
@GetMapping("/stats/average-age")
public ResponseEntity<Map<String, Double>> getAverageAge() {
    Double avgAge = userService.getAverageAge();
    return ResponseEntity.ok(Map.of("averageAge", avgAge != null ? avgAge : 0.0));
}

/**
 * 複合検索
 * GET /api/users/search?name=Taro&minAge=20&maxAge=40
 */
@GetMapping("/search")
public ResponseEntity<List<User>> search(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Integer minAge,
        @RequestParam(required = false) Integer maxAge) {
    List<User> users = userService.searchUsers(name, minAge, maxAge);
    return ResponseEntity.ok(users);
}
```

### 2-6. 動作確認

メールドメインで検索：

```bash
curl "http://localhost:8080/api/users/search/email-domain?domain=example.com"
```

平均年齢取得：

```bash
curl http://localhost:8080/api/users/stats/average-age
```

複合検索：

```bash
# 名前に"Taro"を含み、年齢が25〜35のユーザー
curl "http://localhost:8080/api/users/search?name=Taro&minAge=25&maxAge=35"

# 名前のみで検索
curl "http://localhost:8080/api/users/search?name=Tanaka"

# 年齢のみで検索
curl "http://localhost:8080/api/users/search?minAge=28"
```

---

## 🚀 ステップ3: ネイティブSQLクエリ

### 3-1. ネイティブクエリとは？

**ネイティブクエリ** = データベース固有のSQLを直接実行

**使用場面**:
- JPQLで表現できない複雑なクエリ
- データベース固有の機能を使いたい場合
- パフォーマンスチューニング

### 3-2. 実装例

`UserRepository.java`に追加：

```java
/**
 * ネイティブSQLで年齢の統計情報を取得
 */
@Query(value = "SELECT MIN(age) as min_age, MAX(age) as max_age, AVG(age) as avg_age FROM users",
       nativeQuery = true)
Map<String, Object> getAgeStatistics();

/**
 * ネイティブSQLで名前の一部を更新
 */
@Query(value = "UPDATE users SET name = CONCAT(:prefix, ' ', name) WHERE age >= :minAge",
       nativeQuery = true)
@Modifying
int addPrefixToNames(@Param("prefix") String prefix, @Param("minAge") Integer minAge);
```

### 3-3. @Modifyingアノテーション

```java
@Modifying
int addPrefixToNames(@Param("prefix") String prefix, @Param("minAge") Integer minAge);
```

- `@Modifying`: 更新・削除クエリに必須
- 戻り値: 影響を受けた行数

**重要**: `@Modifying`クエリは`@Transactional`と一緒に使用する必要があります。

---

## 🎨 完全な検索APIの実装例

### 検索条件DTOの作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/UserSearchCriteria.java`

```java
package com.example.hellospringboot.dto;

import lombok.Data;

@Data
public class UserSearchCriteria {
    private String name;
    private String email;
    private Integer minAge;
    private Integer maxAge;
}
```

### Specificationを使った動的クエリ（高度）

`UserRepository.java`を拡張：

```java
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, 
                                         JpaSpecificationExecutor<User> {
    // ... 既存のメソッド ...
}
```

`UserService.java`に動的検索メソッドを追加：

```java
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;

/**
 * 動的検索（Specification使用）
 */
public List<User> searchUsersDynamic(UserSearchCriteria criteria) {
    return userRepository.findAll((Specification<User>) (root, query, criteriaBuilder) -> {
        List<Predicate> predicates = new ArrayList<>();

        if (criteria.getName() != null && !criteria.getName().isEmpty()) {
            predicates.add(criteriaBuilder.like(root.get("name"), "%" + criteria.getName() + "%"));
        }

        if (criteria.getEmail() != null && !criteria.getEmail().isEmpty()) {
            predicates.add(criteriaBuilder.like(root.get("email"), "%" + criteria.getEmail() + "%"));
        }

        if (criteria.getMinAge() != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("age"), criteria.getMinAge()));
        }

        if (criteria.getMaxAge() != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("age"), criteria.getMaxAge()));
        }

        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    });
}
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: 削除されていないユーザーのみ取得

論理削除フラグ（`deleted`フィールド）を追加し、削除されていないユーザーのみを取得するメソッドを実装してください。

**ヒント**:
```java
// Userエンティティに追加
@Column(nullable = false)
private Boolean deleted = false;

// Repository
List<User> findByDeletedFalse();
```

### チャレンジ 2: 最新登録ユーザーを取得

最新5件のユーザーを作成日時順で取得するAPIを実装してください。

**ヒント**:
```java
// Userエンティティに作成日時を追加
@Column(nullable = false, updatable = false)
private LocalDateTime createdAt;

// Repository
List<User> findTop5ByOrderByCreatedAtDesc();
```

### チャレンジ 3: 集計API

以下の統計情報を返すAPIを作成してください：
- ユーザー総数
- 平均年齢
- 最年少/最年長
- 年代別人数（20代、30代、など）

---

## 🐛 トラブルシューティング

### "No property 'xxx' found"

**エラー**: `No property 'name' found for type 'User'`

**原因**: エンティティにフィールドが存在しない、またはスペルミス

**解決策**: フィールド名を確認

### JPQLで構文エラー

**エラー**: `org.hibernate.hql.internal.ast.QuerySyntaxException`

**原因**: JPQL構文が間違っている

**よくある間違い**:
```java
// NG: テーブル名を使用
@Query("SELECT u FROM users u WHERE ...")

// OK: エンティティ名を使用
@Query("SELECT u FROM User u WHERE ...")
```

### @Modifyingで"Executing an update/delete query"

**エラー**: 更新クエリが実行されない

**原因**: `@Transactional`がない

**解決策**:
```java
@Transactional
public void updateUsers() {
    userRepository.addPrefixToNames("Mr.", 30);
}
```

### パラメータが正しくバインドされない

**症状**: クエリが空の結果を返す

**原因**: `@Param`アノテーションの名前が間違っている

**解決策**:
```java
// パラメータ名とJPQL内の名前を一致させる
@Query("SELECT u FROM User u WHERE u.age >= :minAge")
List<User> findByMinAge(@Param("minAge") Integer minAge);
```

---

## 📚 このステップで学んだこと

- ✅ Spring Data JPAのクエリメソッド命名規則
- ✅ `findBy`, `GreaterThan`, `Between`, `Containing`などのキーワード
- ✅ `@Query`アノテーションでJPQLを記述
- ✅ `@Param`でパラメータをバインド
- ✅ ネイティブSQLクエリの使用
- ✅ `@Modifying`で更新・削除クエリ
- ✅ 集計関数（COUNT, AVG, MIN, MAX）の使用
- ✅ 複雑な検索条件の実装

---

## 💡 補足: クエリメソッド vs @Query vs Specification

### 使い分けガイドライン

| 方法 | 適用場面 | メリット | デメリット |
|------|---------|---------|-----------|
| **クエリメソッド** | シンプルな検索 | 簡潔、タイプセーフ | 複雑な条件は困難 |
| **@Query (JPQL)** | 複雑な検索、集計 | 柔軟、読みやすい | 文字列なのでエラー検出が遅い |
| **@Query (Native)** | DB固有機能 | 何でもできる | DB依存、JPAの恩恵少ない |
| **Specification** | 動的検索 | 条件を動的に組み立て | コードが複雑 |

### 推奨アプローチ

```
1. まずクエリメソッドを検討
2. 複雑ならJPQLを使用
3. 動的条件が必要ならSpecification
4. 最終手段としてネイティブSQL
```

---

## 💡 補足: クエリのパフォーマンス

### N+1問題に注意

```java
// NG: N+1問題が発生
List<User> users = userRepository.findAll();
for (User user : users) {
    // ループ内でクエリが発生
    List<Order> orders = orderRepository.findByUserId(user.getId());
}
```

**解決策**: JOINまたは`@EntityGraph`を使用（Step 11で学習）

### インデックスの活用

頻繁に検索するフィールドにはインデックスを追加：

```java
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_age", columnList = "age")
})
public class User {
    // ...
}
```

---

## 🔄 Gitへのコミットとレビュー依頼

進捗を記録してレビューを受けましょう：

```bash
git add .
git commit -m "Step 10: カスタムクエリ実装完了（クエリメソッド、JPQL、ネイティブSQL）"
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

レビューが完了したら、[Step 11: リレーションシップ（1対多）](STEP_11.md)へ進みましょう！

次のステップでは、ユーザーと投稿のような1対多の関係をエンティティで表現する方法を学びます。
`@OneToMany`、`@ManyToOne`アノテーションを使って、関連するデータを効率的に扱えるようになります！

---

お疲れさまでした！ 🎉

クエリメソッドと@Queryを使いこなせるようになると、柔軟なデータ検索が実装できます。
次はテーブル間のリレーションシップという、データベース設計の重要なテーマに進みましょう！
