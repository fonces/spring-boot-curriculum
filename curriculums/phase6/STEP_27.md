# Step 27: ユニットテスト

## 🎯 このステップの目標

- JUnit 5を使ったユニットテストを理解する
- Mockitoでモックを作成する
- Service層とRepository層のテストを書く
- テストカバレッジを意識する

**所要時間**: 約1時間30分

---

## 📋 事前準備

- Phase 4までのレイヤードアーキテクチャが理解できていること
- UserService、UserRepositoryが実装されていること

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
        User user = User.builder()
                .name("Test User")
                .email("test@example.com")
                .age(25)
                .build();

        // When
        User savedUser = userRepository.save(user);

        // Then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getName()).isEqualTo("Test User");
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("メールアドレスでユーザーを検索できること")
    void testFindByEmail() {
        // Given
        User user = User.builder()
                .name("John Doe")
                .email("john@example.com")
                .age(30)
                .build();
        userRepository.save(user);

        // When
        Optional<User> foundUser = userRepository.findByEmail("john@example.com");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("存在しないメールアドレスで検索した場合は空を返すこと")
    void testFindByEmailNotFound() {
        // When
        Optional<User> foundUser = userRepository.findByEmail("notexist@example.com");

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("メールアドレスの重複をチェックできること")
    void testExistsByEmail() {
        // Given
        User user = User.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .age(28)
                .build();
        userRepository.save(user);

        // When
        boolean exists = userRepository.existsByEmail("jane@example.com");
        boolean notExists = userRepository.existsByEmail("notexist@example.com");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
}
```

---

## 🚀 ステップ3: Service層のテスト（Mockitoを使用）

### 3-1. UserServiceTest

**ファイルパス**: `src/test/java/com/example/hellospringboot/service/UserServiceTest.java`

```java
package com.example.hellospringboot.service;

import com.example.hellospringboot.dto.request.UserCreateRequest;
import com.example.hellospringboot.dto.response.UserResponse;
import com.example.hellospringboot.entity.User;
import com.example.hellospringboot.exception.DuplicateResourceException;
import com.example.hellospringboot.exception.UserNotFoundException;
import com.example.hellospringboot.mapper.UserMapper;
import com.example.hellospringboot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * UserServiceのテスト
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserCreateRequest createRequest;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .age(25)
                .build();

        createRequest = UserCreateRequest.builder()
                .name("Test User")
                .email("test@example.com")
                .age(25)
                .build();

        userResponse = UserResponse.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .age(25)
                .build();
    }

    @Test
    @DisplayName("ユーザーを作成できること")
    void testCreateUser() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userMapper.toEntity(any(UserCreateRequest.class))).thenReturn(testUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);

        // When
        UserResponse result = userService.createUser(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test User");
        assertThat(result.getEmail()).isEqualTo("test@example.com");

        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository).save(any(User.class));
        verify(userMapper).toResponse(any(User.class));
    }

    @Test
    @DisplayName("メールアドレスが重複している場合は例外をスローすること")
    void testCreateUserDuplicateEmail() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.createUser(createRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("メールアドレス");

        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("IDでユーザーを取得できること")
    void testGetUserById() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);

        // When
        UserResponse result = userService.getUserById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("存在しないIDの場合は例外をスローすること")
    void testGetUserByIdNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(999L);
    }

    @Test
    @DisplayName("ユーザーを削除できること")
    void testDeleteUser() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(true);
        doNothing().when(userRepository).deleteById(1L);

        // When
        userService.deleteUser(1L);

        // Then
        verify(userRepository).existsById(1L);
        verify(userRepository).deleteById(1L);
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
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 🚀 ステップ5: テストカバレッジ

### 5-1. JaCoCoプラグインの追加

**ファイルパス**: `pom.xml`

```xml
<build>
    <plugins>
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

## 🔧 補足: MyBatisのテスト

Phase 3でMyBatisを学習した場合、MyBatis Mapperのテストも重要です。

### MyBatis Mapperのテスト

**ファイルパス**: `src/test/java/com/example/hellospringboot/mapper/UserMapperTest.java`

```java
package com.example.hellospringboot.mapper;

import com.example.hellospringboot.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserMapper のテスト
 */
@MybatisTest  // MyBatis専用のテストアノテーション
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("UserMapper Tests")
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    @DisplayName("ユーザーを挿入できること")
    void testInsert() {
        // Given
        User user = User.builder()
                .name("Test User")
                .email("test@example.com")
                .age(25)
                .build();

        // When
        userMapper.insert(user);

        // Then
        assertThat(user.getId()).isNotNull();  // IDが自動生成される
    }

    @Test
    @DisplayName("IDでユーザーを検索できること")
    void testFindById() {
        // Given
        User user = User.builder()
                .name("John Doe")
                .email("john@example.com")
                .age(30)
                .build();
        userMapper.insert(user);

        // When
        Optional<User> foundUser = userMapper.findById(user.getId());

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("全ユーザーを取得できること")
    void testFindAll() {
        // Given
        User user1 = User.builder().name("User 1").email("user1@example.com").age(25).build();
        User user2 = User.builder().name("User 2").email("user2@example.com").age(30).build();
        userMapper.insert(user1);
        userMapper.insert(user2);

        // When
        List<User> users = userMapper.findAll();

        // Then
        assertThat(users).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("ユーザーを更新できること")
    void testUpdate() {
        // Given
        User user = User.builder()
                .name("Original Name")
                .email("original@example.com")
                .age(25)
                .build();
        userMapper.insert(user);

        // When
        user.setName("Updated Name");
        user.setAge(26);
        userMapper.update(user);

        // Then
        Optional<User> updatedUser = userMapper.findById(user.getId());
        assertThat(updatedUser).isPresent();
        assertThat(updatedUser.get().getName()).isEqualTo("Updated Name");
        assertThat(updatedUser.get().getAge()).isEqualTo(26);
    }

    @Test
    @DisplayName("ユーザーを削除できること")
    void testDelete() {
        // Given
        User user = User.builder()
                .name("To Delete")
                .email("delete@example.com")
                .age(25)
                .build();
        userMapper.insert(user);
        Long userId = user.getId();

        // When
        userMapper.deleteById(userId);

        // Then
        Optional<User> deletedUser = userMapper.findById(userId);
        assertThat(deletedUser).isEmpty();
    }
}
```

### MyBatisテストの依存関係

**pom.xml**:
```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter-test</artifactId>
    <version>3.0.3</version>
    <scope>test</scope>
</dependency>
```

### JPA vs MyBatisのテスト比較

| 観点 | JPA (@DataJpaTest) | MyBatis (@MybatisTest) |
|------|-------------------|------------------------|
| **テストアノテーション** | `@DataJpaTest` | `@MybatisTest` |
| **トランザクション** | 自動ロールバック | 自動ロールバック |
| **テストDB** | インメモリDBなどで可能 | 本番と同じDBを推奨 |
| **設定** | application-test.yml | application-test.yml |
| **依存関係** | spring-boot-starter-test | mybatis-spring-boot-starter-test |

> **💡 推奨**: MyBatisはSQLを直接記述するため、本番環境と同じデータベース（Docker MySQL等）でテストすることを推奨します。

---

## 🎨 チャレンジ課題

### チャレンジ 1: PostServiceのテスト

PostServiceのテストを作成してください。

### チャレンジ 2: パラメータ化テスト

`@ParameterizedTest`を使って複数のケースをテストしてください。

```java
@ParameterizedTest
@ValueSource(strings = {"test@example.com", "user@test.com"})
void testValidEmail(String email) {
    // テストコード
}
```

### チャレンジ 3: テストカバレッジ80%以上

カバレッジレポートで80%以上を達成してください。

---

## 📚 このステップで学んだこと

- ✅ JUnit 5の基本
- ✅ @DataJpaTestによるRepositoryテスト
- ✅ Mockitoによるモック作成
- ✅ Given-When-Then パターン
- ✅ AssertJによるアサーション
- ✅ テストカバレッジ

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
