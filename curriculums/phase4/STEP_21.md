# Step 21: ユニットテスト

## 🎯 このステップの目標

- JUnit 5を使ったユニットテストを理解する
- Mockitoでモックを作成する
- Service層とRepository層のテストを書く
- テストカバレッジを意識する

**所要時間**: 約2時間

---

## 📋 事前準備

- Phase 3までのレイヤードアーキテクチャが理解できていること
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

### 4-2. IntelliJ IDEAでテスト実行

1. テストクラスを右クリック
2. 「Run 'UserServiceTest'」を選択

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

## 🔄 Gitへのコミット

```bash
git add .
git commit -m "Phase 4: STEP_21完了（ユニットテスト実装）"
git push origin main
```

---

## ➡️ 次のステップ

次は[Step 22: 統合テスト](STEP_22.md)へ進みましょう！

---

お疲れさまでした！ 🎉
