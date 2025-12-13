# Step 27: ユニットテスト

## 🎯 このステップの目標

- JUnit 5を使ったユニットテストを理解する
- Mockitoでモックを作成する
- Service層とRepository層のテストを書く
- テストカバレッジを意識する

**所要時間**: 約1時間30分

---

## 📋 事前準備

- **[Step 8: CRUD操作の完成](../phase2/STEP_8.md)** が完了していること
- **UserService**、**UserRepository**が実装されていること

> **💡 ヒント**: UserServiceとUserRepositoryは [Step 8](../phase2/STEP_8.md) で実装しています。
> 
> 以下のメソッドが実装されている必要があります:
> - `UserRepository`: `save()`, `findById()`, `existsById()`, `deleteById()`, `findAll()`
> - `UserService`: 
>   - `createUser(User user)` → `User`
>   - `getAllUsers()` → `List<User>`
>   - `getUserById(Long id)` → `Optional<User>`
>   - `updateUser(Long id, User userDetails)` → `Optional<User>`
>   - `deleteUser(Long id)` → `boolean`

---

## 💡 テストの重要性

### テストの目的

- ✅ バグの早期発見
- ✅ リファクタリングの安全性確保
- ✅ ドキュメントとしての役割
- ✅ コードの品質向上

### テストの種類

| 種類 | 範囲 | 速度 | 例 |
|------|------|------|-----|
| **ユニットテスト** | 1つのクラス/メソッド | 高速 | UserServiceのテスト |
| **統合テスト** | 複数のコンポーネント | 中速 | Controller+Service+Repository |
| **E2Eテスト** | システム全体 | 低速 | ブラウザテスト |

---

## 🚀 ステップ1: テスト依存関係の確認

### 1-1. pom.xmlの確認

Spring Boot Starterに含まれています：

```xml
<!-- H2 Database (テスト用) -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

**含まれるライブラリ**:
- JUnit 5
- Mockito
- AssertJ
- Hamcrest
- Spring Test

### 1-2. テスト用データベース設定

テスト実行時にH2インメモリデータベースを使用するため、設定ファイルを作成します。

**ファイルパス**: `src/test/resources/application-test.yml`

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:
  
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        # MySQL方言を明示的に無効化
        dialect: org.hibernate.dialect.H2Dialect
```

**設定の説明**:

| 設定項目 | 値 | 説明 |
|---------|-----|------|
| `url` | `jdbc:h2:mem:testdb` | インメモリH2データベースを使用 |
| `driver-class-name` | `org.h2.Driver` | H2データベースドライバー |
| `ddl-auto` | `create-drop` | テスト開始時にテーブル作成、終了時に削除 |
| `show-sql` | `true` | 実行されるSQLをコンソールに出力 |
| `format_sql` | `true` | SQLを整形して見やすく表示 |

> **💡 ポイント**: 本番環境ではMySQLを使い、テスト環境ではH2を使うことで、高速なテストが可能になります。

---

## 🚀 ステップ2: Repository層のテスト

### 2-1. UserRepositoryTest

**ファイルパス**: `src/test/java/com/example/hellospringboot/repository/UserRepositoryTest.java`

```java
package com.example.hellospringboot.repository;

import com.example.hellospringboot.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserRepositoryのテスト
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository Tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("ユーザーを保存できること")
    void testSaveUser() {
        // Given
        User user = new User();
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setAge(25);
        user.setPassword("password123");
        user.setRole("USER");

        // When
        User savedUser = userRepository.save(user);

        // Then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getName()).isEqualTo("Test User");
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("IDでユーザーを検索できること")
    void testFindById() {
        // Given
        User user = new User();
        user.setName("John Doe");
        user.setEmail("john@example.com");
        user.setAge(30);
        user.setPassword("password456");
        user.setRole("USER");
        User savedUser = userRepository.save(user);

        // When
        Optional<User> foundUser = userRepository.findById(savedUser.getId());

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("存在しないIDで検索した場合は空を返すこと")
    void testFindByIdNotFound() {
        // When
        Optional<User> foundUser = userRepository.findById(999L);

        // Then
        assertThat(foundUser).isEmpty();
    }
}
```

---

## 🚀 ステップ3: Service層のテスト（Mockitoを使用）

### 3-1. UserServiceTest

**ファイルパス**: `src/test/java/com/example/hellospringboot/service/UserServiceTest.java`

```java
package com.example.hellospringboot.service;

import com.example.hellospringboot.entity.User;
import com.example.hellospringboot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UserServiceのテスト
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setAge(25);
    }

    @Test
    @DisplayName("ユーザーを作成できること")
    void testCreateUser() {
        // Given
        User newUser = new User();
        newUser.setName("New User");
        newUser.setEmail("new@example.com");
        newUser.setAge(30);
        
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.createUser(newUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test User");

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("全ユーザーを取得できること")
    void testGetAllUsers() {
        // Given
        User user2 = new User();
        user2.setId(2L);
        user2.setName("User 2");
        user2.setEmail("user2@example.com");
        user2.setAge(28);
        
        List<User> users = Arrays.asList(testUser, user2);
        when(userRepository.findAll()).thenReturn(users);

        // When
        List<User> result = userService.getAllUsers();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Test User");
        assertThat(result.get(1).getName()).isEqualTo("User 2");

        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("IDでユーザーを取得できること")
    void testGetUserById() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.getUserById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getName()).isEqualTo("Test User");
        
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("存在しないIDの場合は空のOptionalを返すこと")
    void testGetUserByIdNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.getUserById(999L);

        // Then
        assertThat(result).isEmpty();

        verify(userRepository).findById(999L);
    }

    @Test
    @DisplayName("ユーザーを更新できること")
    void testUpdateUser() {
        // Given
        User updateDetails = new User();
        updateDetails.setName("Updated Name");
        updateDetails.setEmail("updated@example.com");
        updateDetails.setAge(26);
        
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setName("Updated Name");
        updatedUser.setEmail("updated@example.com");
        updatedUser.setAge(26);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // When
        Optional<User> result = userService.updateUser(1L, updateDetails);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Updated Name");
        assertThat(result.get().getEmail()).isEqualTo("updated@example.com");
        
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("存在しないIDの更新は空のOptionalを返すこと")
    void testUpdateUserNotFound() {
        // Given
        User updateDetails = new User();
        updateDetails.setName("Updated Name");
        
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.updateUser(999L, updateDetails);

        // Then
        assertThat(result).isEmpty();
        
        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("ユーザーを削除できること")
    void testDeleteUser() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(true);
        doNothing().when(userRepository).deleteById(1L);

        // When
        boolean result = userService.deleteUser(1L);

        // Then
        assertThat(result).isTrue();
        verify(userRepository).existsById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("存在しないIDの削除はfalseを返すこと")
    void testDeleteUserNotFound() {
        // Given
        when(userRepository.existsById(999L)).thenReturn(false);

        // When
        boolean result = userService.deleteUser(999L);

        // Then
        assertThat(result).isFalse();
        verify(userRepository).existsById(999L);
        verify(userRepository, never()).deleteById(999L);
    }
}
```

---

## 🚀 ステップ4: テストの実行

### 4-1. Maven経由でテスト実行

```bash
./mvnw test
```

### 4-2. VSCodeでテスト実行

1. テストクラスを開く
2. クラス名やメソッド名の左側に表示される「Run Test」アイコンをクリック
3. または、Testing サイドバー（フラスコアイコン）からテストを実行

### 4-3. テスト結果

```
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 🚀 ステップ5: テストカバレッジ

### 5-1. JaCoCoプラグインの追加

**ファイルパス**: `pom.xml`

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>

        <!-- 以下を追記 -->
        <!-- JaCoCo Maven Plugin -->
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.11</version>
            <executions>
                <execution>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### 5-2. カバレッジレポート生成

```bash
./mvnw clean test

# レポート確認
open target/site/jacoco/index.html
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: 境界値テスト

年齢が0、負の数、200など境界値でのバリデーションテストを追加してください。

```java
@Test
@DisplayName("年齢が負の数の場合の動作確認")
void testCreateUserWithNegativeAge() {
    // Given
    User user = new User();
    user.setAge(-1);
    
    // When & Then
    // 適切な処理を実装
}
```

### チャレンジ 2: パラメータ化テスト

`@ParameterizedTest`を使って複数のユーザーデータでテストしてください。

```java
@ParameterizedTest
@CsvSource({
    "Alice, alice@example.com, 25",
    "Bob, bob@example.com, 30",
    "Charlie, charlie@example.com, 35"
})
void testCreateMultipleUsers(String name, String email, int age) {
    // Given
    User user = new User();
    user.setName(name);
    user.setEmail(email);
    user.setAge(age);
    
    when(userRepository.save(any(User.class))).thenReturn(user);
    
    // When
    User result = userService.createUser(user);
    
    // Then
    assertThat(result.getName()).isEqualTo(name);
}
```

### チャレンジ 3: テストカバレッジ90%以上

JaCoCoカバレッジレポートで90%以上を達成してください。特にService層とRepository層に注力しましょう。

---

## 📚 このステップで学んだこと

- ✅ JUnit 5の基本（@Test、@BeforeEach、@DisplayName）
- ✅ @DataJpaTestによるRepositoryテスト
- ✅ Mockitoによるモック作成（@Mock、@InjectMocks）
- ✅ Given-When-Then パターンでのテスト構造化
- ✅ AssertJによる流暢なアサーション
- ✅ Optional<T>のテスト方法
- ✅ boolean戻り値のテスト方法
- ✅ JaCoCoによるテストカバレッジ測定

---

## � トラブルシューティング

### エラー1: "NoSuchBeanDefinitionException"

```
org.springframework.beans.factory.NoSuchBeanDefinitionException: No qualifying bean of type 'UserRepository' available
```

**原因**: テストクラスで`@ExtendWith(MockitoExtension.class)`を使っているのに、モックを作成していない

**解決策**:

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock  // ← これが必要
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
}
```

---

### エラー2: "NullPointerException in test"

```
java.lang.NullPointerException: Cannot invoke "UserRepository.findById()" because "this.userRepository" is null
```

**原因**: `@InjectMocks`アノテーションをつけ忘れている

**解決策**:

```java
@Mock
private UserRepository userRepository;

@InjectMocks  // ← これが必要
private UserService userService;
```

---

### エラー3: テストで期待した値が返ってこない

```java
@Test
void testFindById() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    
    User result = userService.findById(2L);  // ← 違うIDで呼び出している
    
    assertThat(result).isNotNull();  // ← テスト失敗
}
```

**原因**: モックの設定と実際の呼び出しで引数が異なる

**解決策**:

```java
@Test
void testFindById() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    
    User result = userService.findById(1L);  // ← 同じIDで呼び出す
    
    assertThat(result).isNotNull();
}
```

---

### エラー4: "UnnecessaryStubbingException"

```
org.mockito.exceptions.misusing.UnnecessaryStubbingException: Unnecessary stubbings detected.
```

**原因**: モックの設定をしたのに、テスト内でそのメソッドを呼び出していない

**解決策**:

```java
@Test
void testCreateUser() {
    // when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));  // ← 使わないモックは削除
    
    when(userRepository.save(any(User.class))).thenReturn(testUser);
    
    User result = userService.createUser(testUser);
    assertThat(result).isNotNull();
}
```

または、Mockitoの厳格性を下げる：

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)  // ← 追加
class UserServiceTest {
    // ...
}
```

---

### エラー5: "Wanted but not invoked" エラー

```
Wanted but not invoked:
userRepository.save(<any>);
```

**原因**: `verify()`でメソッド呼び出しを検証しているが、実際には呼ばれていない

**解決策**:

```java
@Test
void testCreateUser() {
    when(userRepository.save(any(User.class))).thenReturn(testUser);
    
    userService.createUser(testUser);  // ← このメソッド内でsave()が呼ばれることを確認
    
    verify(userRepository).save(any(User.class));  // ← 検証
}
```

デバッグ時は`verifyNoMoreInteractions()`で予期しない呼び出しを検出：

```java
verify(userRepository).save(any(User.class));
verifyNoMoreInteractions(userRepository);  // ← 他のメソッドが呼ばれていないことを確認
```

---

### エラー6: AssertJのアサーションが読みにくい

```java
// ❌ JUnitのアサーションは読みにくい
assertEquals(expected, actual);
assertTrue(result > 0);
```

**解決策**: AssertJの流暢なAPIを使う

```java
// ✅ AssertJは読みやすい
assertThat(actual).isEqualTo(expected);
assertThat(result).isGreaterThan(0);

// 複数のアサーションをまとめて
assertThat(user)
    .isNotNull()
    .extracting(User::getName, User::getEmail)
    .containsExactly("Alice", "alice@example.com");
```

---

## �🔄 Gitへのコミットとレビュー依頼

```bash
git add .
git commit -m "Step 27: ユニットテスト完了"
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

次は[Step 28: 統合テスト](STEP_28.md)へ進みましょう！

---

お疲れさまでした！ 🎉
