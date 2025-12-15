# Step 27: ユニットテスト

## 🎯 このステップの目標

- JUnit 5の基本的な使い方を理解できる
- Mockitoでモックオブジェクトを作成できる
- サービス層のユニットテストを実装できる
- テストの重要性とベストプラクティスを理解できる

**所要時間**: 約50分

---

## 📋 事前準備

- [Step 26: JWTトークン認証](STEP_26.md)が完了していること
- JUnit 5の基本概念を理解していること（推奨）

---

## 🧪 なぜテストが必要か

### テストがない世界

**問題1**: バグを見逃す
```java
public UserResponse update(Long id, UserUpdateRequest request) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    
    // バグ: nameがnullの場合の処理がない
    user.setName(request.getName());
    
    return userMapper.toResponse(userRepository.save(user));
}
```

**問題2**: リファクタリングが怖い
- コード変更後に動作確認が大変
- どこが壊れたか分からない

**問題3**: 仕様書がない
- コードの意図が分からない
- 新しいメンバーが理解しにくい

### テストがある世界

**改善1**: バグを早期発見
```java
@Test
void update_WithNullName_ShouldThrowException() {
    // Nullの場合の動作を確認
}
```

**改善2**: 安心してリファクタリング
- テストが通れば動作保証
- 自動化されたテスト実行

**改善3**: テストが仕様書
- テストコードを読めば仕様が分かる
- ドキュメントより正確

---

## 🚀 ステップ1: JUnit 5の基本

### 1-1. テスト依存関係の確認

`pom.xml`に以下が含まれていることを確認（Spring Boot Starterに含まれています）：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

これに以下が含まれます：
- **JUnit 5**: テストフレームワーク
- **Mockito**: モックライブラリ
- **AssertJ**: アサーションライブラリ
- **Hamcrest**: マッチャーライブラリ

### 1-2. 最初のテストクラスを作成

`src/test/java/com/example/hellospringboot/services/UserServiceTest.java`:

```java
package com.example.hellospringboot.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {
    
    @Test
    void simpleTest() {
        // Arrange（準備）
        int a = 2;
        int b = 3;
        
        // Act（実行）
        int result = a + b;
        
        // Assert（検証）
        assertEquals(5, result);
    }
}
```

### 1-3. テストを実行

```bash
./mvnw test
```

**期待される結果**:
```sh
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

### 1-4. JUnit 5の基本アノテーション

| アノテーション | 説明 | 実行タイミング |
|---|---|---|
| `@Test` | テストメソッドを定義 | 各テスト |
| `@BeforeEach` | 各テスト前に実行 | テスト前（共通） |
| `@AfterEach` | 各テスト後に実行 | テスト後（共通） |
| `@BeforeAll` | 全テスト前に1回実行 | クラス初期化時 |
| `@AfterAll` | 全テスト後に1回実行 | クラス終了時 |
| `@DisplayName` | テストの表示名 | - |
| `@Disabled` | テストを無効化 | - |

**例**:
```java
@BeforeEach
void setUp() {
    // 各テスト前に実行される初期化処理
}

@Test
@DisplayName("ユーザー作成が成功すること")
void createUser_Success() {
    // テスト内容
}
```

### 1-5. AssertJを使った流暢なアサーション

このカリキュラムでは**AssertJ**を推奨します。JUnit標準のアサーションより読みやすく、エラーメッセージも詳細です。

**JUnit標準のアサーション**:
```java
assertEquals(expected, actual);
assertNotNull(result);
assertTrue(condition);
```

**AssertJ（推奨）**:
```java
assertThat(actual).isEqualTo(expected);
assertThat(result).isNotNull();
assertThat(condition).isTrue();
```

**AssertJのメリット**:
- メソッドチェーンで読みやすい
- IDEの補完が効きやすい
- エラーメッセージが詳細で分かりやすい
- より多くのアサーションメソッドが利用可能

---

## 🚀 ステップ2: Mockitoでモックを作成

### 2-1. Mockitoとは

**モック（Mock）**: 本物の代わりになる偽物のオブジェクト

**例**: `UserRepository`をモックする理由
- **実際のDB不要**: テストが高速
- **データ準備不要**: テストが簡単
- **単体テスト**: Serviceのみをテスト

### 2-2. UserServiceのテストクラスを作成

`src/test/java/com/example/hellospringboot/services/UserServiceTest.java`を更新：

```java
package com.example.hellospringboot.services;

import com.example.hellospringboot.dto.UserCreateRequest;
import com.example.hellospringboot.dto.UserResponse;
import com.example.hellospringboot.entities.User;
import com.example.hellospringboot.exceptions.ResourceNotFoundException;
import com.example.hellospringboot.mappers.UserMapper;
import com.example.hellospringboot.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
        // テストデータの準備
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setAge(25);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        
        createRequest = new UserCreateRequest("Test User", "test@example.com", 25);
        
        userResponse = new UserResponse(
            1L,
            "Test User",
            "test@example.com",
            25,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }
    
    @Test
    @DisplayName("全ユーザー取得が成功すること")
    void findAll_Success() {
        // Arrange: モックの動作を定義
        List<User> users = Arrays.asList(testUser);
        when(userRepository.findAll()).thenReturn(users);
        when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);
        
        // Act: メソッドを実行
        List<UserResponse> result = userService.findAll();
        
        // Assert: 結果を検証
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test User", result.get(0).getName());
        
        // モックメソッドが呼ばれたことを検証
        verify(userRepository, times(1)).findAll();
        verify(userMapper, times(1)).toResponse(any(User.class));
    }
    
    @Test
    @DisplayName("ID指定でユーザー取得が成功すること")
    void findById_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userMapper.toResponse(testUser)).thenReturn(userResponse);
        
        // Act
        UserResponse result = userService.findById(1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test User", result.getName());
        
        verify(userRepository, times(1)).findById(1L);
    }
    
    @Test
    @DisplayName("存在しないIDで例外がスローされること")
    void findById_NotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.findById(999L);
        });
        
        verify(userRepository, times(1)).findById(999L);
    }
    
    @Test
    @DisplayName("ユーザー作成が成功すること")
    void create_Success() {
        // Arrange
        User newUser = new User();
        newUser.setName("Test User");
        newUser.setEmail("test@example.com");
        newUser.setAge(25);
        
        when(userMapper.toEntity(createRequest)).thenReturn(newUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(userResponse);
        
        // Act
        UserResponse result = userService.create(createRequest);
        
        // Assert
        assertNotNull(result);
        assertEquals("Test User", result.getName());
        assertEquals("test@example.com", result.getEmail());
        
        verify(userMapper, times(1)).toEntity(createRequest);
        verify(userRepository, times(1)).save(any(User.class));
    }
    
    @Test
    @DisplayName("ユーザー削除が成功すること")
    void delete_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).delete(testUser);
        
        // Act
        assertDoesNotThrow(() -> userService.delete(1L));
        
        // Assert
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).delete(testUser);
    }
}
```

### 2-3. コードの解説

#### `@ExtendWith(MockitoExtension.class)`
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
```
- Mockitoを有効化
- `@Mock`、`@InjectMocks`を使用可能に

#### `@Mock`
```java
@Mock
private UserRepository userRepository;
```
- モックオブジェクトを作成
- 実際のDBにアクセスしない

#### `@InjectMocks`
```java
@InjectMocks
private UserService userService;
```
- モックを自動注入
- `UserService`に`userRepository`を注入

#### `when().thenReturn()`
```java
when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
```
- モックの動作を定義
- 「〜が呼ばれたら、〜を返す」

#### `verify()`
```java
verify(userRepository, times(1)).findById(1L);
```
- メソッドが呼ばれたことを検証
- `times(1)`: 1回呼ばれた

#### `assertThrows()`
```java
assertThrows(ResourceNotFoundException.class, () -> {
    userService.findById(999L);
});
```
- 例外がスローされることを検証

---

## 🚀 ステップ3: AssertJで読みやすいアサーション

### 3-1. AssertJとは

JUnit 5の`assertEquals`より読みやすいアサーションライブラリ

**JUnit 5**:
```java
assertEquals(5, result);
assertEquals("Test User", user.getName());
assertNotNull(user);
```

**AssertJ**:
```java
assertThat(result).isEqualTo(5);
assertThat(user.getName()).isEqualTo("Test User");
assertThat(user).isNotNull();
```

### 3-2. AssertJを使ったテスト

`UserServiceTest.java`にテストを追加：

```java
import static org.assertj.core.api.Assertions.*;

@Test
@DisplayName("全ユーザー取得（AssertJ版）")
void findAll_WithAssertJ() {
    // Arrange
    List<User> users = Arrays.asList(testUser);
    when(userRepository.findAll()).thenReturn(users);
    when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);
    
    // Act
    List<UserResponse> result = userService.findAll();
    
    // Assert
    assertThat(result)
        .isNotNull()
        .hasSize(1)
        .extracting(UserResponse::getName)
        .containsExactly("Test User");
}

@Test
@DisplayName("ユーザー作成（AssertJ版）")
void create_WithAssertJ() {
    // Arrange
    User newUser = new User();
    newUser.setName("Test User");
    newUser.setEmail("test@example.com");
    newUser.setAge(25);
    
    when(userMapper.toEntity(createRequest)).thenReturn(newUser);
    when(userRepository.save(any(User.class))).thenReturn(testUser);
    when(userMapper.toResponse(testUser)).thenReturn(userResponse);
    
    // Act
    UserResponse result = userService.create(createRequest);
    
    // Assert
    assertThat(result)
        .isNotNull()
        .satisfies(response -> {
            assertThat(response.getName()).isEqualTo("Test User");
            assertThat(response.getEmail()).isEqualTo("test@example.com");
            assertThat(response.getAge()).isEqualTo(25);
        });
}
```

---

## 🚀 ステップ4: テストカバレッジの確認

### 4-1. すべてのテストを実行

```bash
./mvnw test
```

### 4-2. カバレッジレポートの表示

```bash
./mvnw test jacoco:report
```

**レポート場所**:
```sh
target/site/jacoco/index.html
```

ブラウザで開いて確認してください。

---

## ✅ 動作確認

### 1. テストを実行

```bash
./mvnw test
```

**期待される結果**:
```sh
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### 2. 特定のテストクラスのみ実行

```bash
./mvnw test -Dtest=UserServiceTest
```

### 3. 特定のテストメソッドのみ実行

```bash
./mvnw test -Dtest=UserServiceTest#findById_Success
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: パラメータ化テスト

**目標**: 複数の入力値で同じテストを実行

**ヒント**:
```java
@ParameterizedTest
@ValueSource(strings = {"", "  ", "a"})
@DisplayName("名前が不正な場合にバリデーションエラー")
void create_WithInvalidName_ThrowsException(String name) {
    UserCreateRequest request = new UserCreateRequest(name, "test@example.com", 25);
    // テスト実行
}
```

### チャレンジ 2: カスタムマッチャー

**目標**: AssertJでカスタムアサーションを作成

**ヒント**:
```java
assertThat(user).satisfies(u -> {
    assertThat(u.getName()).isNotBlank();
    assertThat(u.getEmail()).contains("@");
    assertThat(u.getAge()).isBetween(0, 150);
});
```

### チャレンジ 3: ArgumentCaptorの使用

**目標**: モックに渡された引数を検証

**ヒント**:
```java
ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
verify(userRepository).save(userCaptor.capture());
User savedUser = userCaptor.getValue();
assertThat(savedUser.getName()).isEqualTo("Test User");
```

---

## 🐛 トラブルシューティング

### エラー: "NullPointerException in test"

**原因**: モックが正しく注入されていない

**解決策**:
```java
// NG
@ExtendWith(SpringExtension.class)  // 間違ったExtension

// OK
@ExtendWith(MockitoExtension.class)  // Mockitoを使う
```

### テストが遅い

**原因**: 統合テスト（@SpringBootTest）を使っている

**解決策**: ユニットテストには`@ExtendWith(MockitoExtension.class)`を使用
```java
// 遅い（Spring起動）
@SpringBootTest
class UserServiceTest {
}

// 速い（モックのみ）
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
}
```

### モックが機能しない

**原因**: `when()`の条件が一致しない

**デバッグ**:
```java
// 引数が一致しているか確認
when(userRepository.findById(eq(1L))).thenReturn(Optional.of(testUser));

// 任意の引数を許可
when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
```

### テストが他のテストに依存する

**問題**: テストの実行順序に依存
```java
static User sharedUser;  // NG: 共有状態

@Test
void test1() {
    sharedUser = new User();  // 他のテストに影響
}
```

**解決策**: 各テストで独立したデータを使用
```java
@BeforeEach
void setUp() {
    testUser = new User();  // 各テストで新しいインスタンス
}
```

---

## 📚 このステップで学んだこと

- ✅ JUnit 5の基本アノテーション
- ✅ Mockitoでモックオブジェクトを作成
- ✅ `when().thenReturn()`でモックの動作を定義
- ✅ `verify()`でメソッド呼び出しを検証
- ✅ AssertJで読みやすいアサーション
- ✅ テストの3A原則（Arrange, Act, Assert）
- ✅ 例外のテスト方法

---

## 💡 補足: テストのベストプラクティス

### 1. テスト名は日本語でOK

```java
@Test
@DisplayName("存在しないIDで例外がスローされること")
void findById_NotFound_ThrowsException() {
}
```

### 2. Given-When-Then パターン

```java
@Test
void create_Success() {
    // Given (Arrange)
    UserCreateRequest request = new UserCreateRequest(...);
    
    // When (Act)
    UserResponse result = userService.create(request);
    
    // Then (Assert)
    assertThat(result).isNotNull();
}
```

### 3. 1テスト1検証

```java
// NG: 複数のことをテスト
@Test
void testEverything() {
    userService.create(...);
    userService.update(...);
    userService.delete(...);
}

// OK: 1つのことをテスト
@Test
void create_Success() {
    userService.create(...);
}

@Test
void update_Success() {
    userService.update(...);
}
```

### 4. テストの独立性

各テストは他のテストに依存せず、独立して実行できること。

---

## ➡️ 次のステップ

[Step 28: 統合テスト](STEP_28.md)へ進みましょう！

次のステップでは、`@SpringBootTest`を使った統合テストを実装します。実際のSpringコンテキストを起動し、エンドツーエンドでのテストを学びます。
