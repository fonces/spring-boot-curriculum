# Step 28: 統合テスト

## 🎯 このステップの目標

- `@SpringBootTest`で統合テストを実装できる
- `MockMvc`でHTTPリクエストをテストできる
- `@DataJpaTest`でリポジトリテストを実装できる
- TestContainersで実際のDBを使ったテストができる

**所要時間**: 約60分

---

## 📋 事前準備

- [Step 27: ユニットテスト](STEP_27.md)が完了していること
- Dockerがインストールされていること（TestContainers使用時）

---

## 🧪 ユニットテストと統合テストの違い

### ユニットテスト

**特徴**:
- モックを使用
- 単一のクラスをテスト
- 高速（秒単位）
- DBやネットワーク不要

**例**:
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
}
```

### 統合テスト

**特徴**:
- 実際のSpringコンテキスト
- 複数のコンポーネントを連携テスト
- 低速（分単位）
- DBやネットワークを使用

**例**:
```java
@SpringBootTest
class UserControllerIntegrationTest {
    @Autowired
    private UserController userController;
    
    @Autowired
    private UserRepository userRepository;
}
```

### 使い分け

| テスト種類 | 対象 | 実行頻度 | 速度 |
|---|---|---|---|
| ユニットテスト | ビジネスロジック | コミットごと | 速い |
| 統合テスト | API全体 | プルリクエスト時 | 遅い |
| E2Eテスト | UI含む全体 | リリース前 | 最も遅い |

---

## 🚀 ステップ1: MockMvcで統合テスト

### 1-1. UserControllerの統合テストを作成

`src/test/java/com/example/hellospringboot/controllers/UserControllerIntegrationTest.java`:

```java
package com.example.hellospringboot.controllers;

import com.example.hellospringboot.dto.UserCreateRequest;
import com.example.hellospringboot.entities.User;
import com.example.hellospringboot.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional  // 各テスト後にロールバック
class UserControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        // テストデータを準備
        testUser = new User();
        testUser.setName("Integration Test User");
        testUser.setEmail("integration@example.com");
        testUser.setAge(30);
        testUser = userRepository.save(testUser);
    }
    
    @Test
    @DisplayName("全ユーザー取得APIが成功すること")
    @WithMockUser(roles = "ADMIN")  // 管理者として認証
    void getAllUsers_Success() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andDo(print())  // リクエスト・レスポンスを出力
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(greaterThan(0))))
            .andExpect(jsonPath("$[0].name").exists())
            .andExpect(jsonPath("$[0].email").exists());
    }
    
    @Test
    @DisplayName("ID指定でユーザー取得APIが成功すること")
    @WithMockUser(roles = "ADMIN")
    void getUserById_Success() throws Exception {
        mockMvc.perform(get("/api/users/{id}", testUser.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(testUser.getId()))
            .andExpect(jsonPath("$.name").value("Integration Test User"))
            .andExpect(jsonPath("$.email").value("integration@example.com"))
            .andExpect(jsonPath("$.age").value(30));
    }
    
    @Test
    @DisplayName("存在しないIDでユーザー取得APIが404を返すこと")
    @WithMockUser(roles = "ADMIN")
    void getUserById_NotFound() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 99999L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Not Found"));
    }
    
    @Test
    @DisplayName("ユーザー作成APIが成功すること")
    @WithMockUser(roles = "ADMIN")
    void createUser_Success() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
            "New User",
            "newuser@example.com",
            25
        );
        
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("New User"))
            .andExpect(jsonPath("$.email").value("newuser@example.com"))
            .andExpect(jsonPath("$.age").value(25))
            .andExpect(jsonPath("$.id").exists());
    }
    
    @Test
    @DisplayName("バリデーションエラーで400が返ること")
    @WithMockUser(roles = "ADMIN")
    void createUser_ValidationError() throws Exception {
        UserCreateRequest invalidRequest = new UserCreateRequest(
            "",  // 空の名前（バリデーションエラー）
            "invalid-email",  // 不正なメール
            -1  // 不正な年齢
        );
        
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Bad Request"));
    }
    
    @Test
    @DisplayName("ユーザー削除APIが成功すること")
    @WithMockUser(roles = "ADMIN")
    void deleteUser_Success() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", testUser.getId()))
            .andExpect(status().isNoContent());
        
        // 削除されたことを確認
        mockMvc.perform(get("/api/users/{id}", testUser.getId()))
            .andExpect(status().isNotFound());
    }
    
    @Test
    @DisplayName("認証なしで401が返ること")
    void withoutAuth_Returns401() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("一般ユーザーで403が返ること")
    @WithMockUser(roles = "USER")  // ADMINではなくUSER
    void withUserRole_Returns403() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isForbidden());
    }
}
```

### 1-2. コードの解説

#### `@SpringBootTest`
```java
@SpringBootTest
```
- 実際のSpringコンテキストを起動
- すべてのBeanが利用可能

#### `@AutoConfigureMockMvc`
```java
@AutoConfigureMockMvc
```
- `MockMvc`を自動設定
- HTTPリクエストをシミュレート

#### `@Transactional`
```java
@Transactional
```
- 各テスト後に自動ロールバック
- DBが汚れない

#### `@WithMockUser`
```java
@WithMockUser(roles = "ADMIN")
```
- Spring Securityの認証をモック
- 指定したロールでテスト実行

#### `MockMvc`
```java
mockMvc.perform(get("/api/users"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$[0].name").exists());
```
- HTTPリクエストをシミュレート
- レスポンスを検証

#### `jsonPath()`
```java
.andExpect(jsonPath("$.name").value("Test User"))
.andExpect(jsonPath("$[0].email").exists())
.andExpect(jsonPath("$", hasSize(3)))
```
- JSONレスポンスの値を検証
- `$`: ルート
- `$[0]`: 配列の最初の要素
- `$.name`: nameフィールド

---

## 🚀 ステップ2: @DataJpaTestでリポジトリテスト

### 2-1. UserRepositoryのテストを作成

`src/test/java/com/example/hellospringboot/repositories/UserRepositoryTest.java`:

```java
package com.example.hellospringboot.repositories;

import com.example.hellospringboot.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private UserRepository userRepository;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setName("Repository Test User");
        testUser.setEmail("repo@example.com");
        testUser.setAge(28);
    }
    
    @Test
    @DisplayName("ユーザーを保存できること")
    void save_Success() {
        // Act
        User saved = userRepository.save(testUser);
        
        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Repository Test User");
        assertThat(saved.getCreatedAt()).isNotNull();
    }
    
    @Test
    @DisplayName("IDでユーザーを検索できること")
    void findById_Success() {
        // Arrange
        User saved = entityManager.persistAndFlush(testUser);
        
        // Act
        Optional<User> found = userRepository.findById(saved.getId());
        
        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Repository Test User");
    }
    
    @Test
    @DisplayName("全ユーザーを取得できること")
    void findAll_Success() {
        // Arrange
        entityManager.persist(testUser);
        
        User anotherUser = new User();
        anotherUser.setName("Another User");
        anotherUser.setEmail("another@example.com");
        anotherUser.setAge(35);
        entityManager.persist(anotherUser);
        
        entityManager.flush();
        
        // Act
        List<User> users = userRepository.findAll();
        
        // Assert
        assertThat(users).hasSize(2);
    }
    
    @Test
    @DisplayName("ユーザーを削除できること")
    void delete_Success() {
        // Arrange
        User saved = entityManager.persistAndFlush(testUser);
        Long id = saved.getId();
        
        // Act
        userRepository.delete(saved);
        
        // Assert
        Optional<User> deleted = userRepository.findById(id);
        assertThat(deleted).isEmpty();
    }
    
    @Test
    @DisplayName("名前で検索できること")
    void findByNameContaining_Success() {
        // Arrange
        entityManager.persist(testUser);
        entityManager.flush();
        
        // Act
        List<User> users = userRepository.findByNameContaining("Repository");
        
        // Assert
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getName()).contains("Repository");
    }
}
```

### 2-2. コードの解説

#### `@DataJpaTest`
```java
@DataJpaTest
```
- JPA関連のBeanのみロード
- インメモリDBを使用（H2）
- 各テスト後に自動ロールバック

#### `TestEntityManager`
```java
@Autowired
private TestEntityManager entityManager;
```
- テスト用のEntityManager
- `persistAndFlush()`: 即座にDBに反映

---

## 🚀 ステップ3: TestContainersで実際のMySQLを使用

### 3-1. TestContainers依存関係を追加

`pom.xml`に追加：

```xml
<!-- TestContainers -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
```

### 3-2. TestContainersを使った統合テスト

`src/test/java/com/example/hellospringboot/controllers/UserControllerTestContainersTest.java`:

```java
package com.example.hellospringboot.controllers;

import com.example.hellospringboot.dto.UserCreateRequest;
import com.example.hellospringboot.entities.User;
import com.example.hellospringboot.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class UserControllerTestContainersTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb")
        .withUsername("testuser")
        .withPassword("testpass");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        User testUser = new User();
        testUser.setName("TestContainers User");
        testUser.setEmail("tc@example.com");
        testUser.setAge(40);
        userRepository.save(testUser);
    }
    
    @Test
    @DisplayName("実際のMySQLでユーザー作成が成功すること")
    @WithMockUser(roles = "ADMIN")
    void createUser_WithRealMySQL_Success() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
            "Real MySQL User",
            "mysql@example.com",
            35
        );
        
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Real MySQL User"));
    }
}
```

### 3-3. TestContainersの解説

#### `@Testcontainers`
```java
@Testcontainers
```
- TestContainersを有効化

#### `@Container`
```java
@Container
static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
```
- Dockerコンテナを起動
- テスト終了後に自動停止

#### `@DynamicPropertySource`
```java
@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
}
```
- コンテナのURLを動的に設定
- 実際のMySQLに接続

---

## ✅ 動作確認

### 1. すべてのテストを実行

```bash
./mvnw test
```

### 2. 統合テストのみ実行

```bash
./mvnw test -Dtest=*IntegrationTest
```

### 3. TestContainersテストを実行

```bash
# Dockerが起動していることを確認
docker ps

# テスト実行
./mvnw test -Dtest=*TestContainersTest
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: カスタムクエリのテスト

**目標**: `UserRepository`のカスタムクエリをテスト

**ヒント**:
```java
@Test
void findByAgeGreaterThan_Success() {
    // 30歳以上のユーザーを検索
    List<User> users = userRepository.findByAgeGreaterThan(30);
    assertThat(users).allMatch(u -> u.getAge() > 30);
}
```

### チャレンジ 2: セキュリティ統合テスト

**目標**: JWT認証のエンドツーエンドテスト

**ヒント**:
```java
@Test
void loginAndAccessProtectedEndpoint_Success() throws Exception {
    // 1. ログインしてトークン取得
    String response = mockMvc.perform(post("/api/auth/login")...)
        .andReturn().getResponse().getContentAsString();
    
    String token = JsonPath.parse(response).read("$.token");
    
    // 2. トークンを使ってAPIアクセス
    mockMvc.perform(get("/api/users")
        .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
}
```

### チャレンジ 3: パフォーマンステスト

**目標**: 大量データでのパフォーマンステスト

**ヒント**:
```java
@Test
void loadTest_1000Users() {
    long start = System.currentTimeMillis();
    
    // 1000件のユーザーを保存
    for (int i = 0; i < 1000; i++) {
        User user = new User();
        user.setName("User " + i);
        userRepository.save(user);
    }
    
    long duration = System.currentTimeMillis() - start;
    assertThat(duration).isLessThan(5000);  // 5秒以内
}
```

---

## 🐛 トラブルシューティング

### TestContainersが起動しない

**原因**: Dockerが起動していない

**解決策**:
```bash
# Dockerを起動
sudo service docker start

# 動作確認
docker ps
```

### H2とMySQLで動作が異なる

**問題**: H2（テスト）とMySQL（本番）でSQLの挙動が違う

**解決策**: TestContainersで実際のMySQLを使用
```java
@Testcontainers
class UserRepositoryTest {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
}
```

### テストが遅い

**原因**: 毎回Springコンテキストを起動

**解決策**:
1. ユニットテストを増やす（統合テストを減らす）
2. テストクラスを分割せず、1つのクラスにまとめる
3. `@SpringBootTest`の代わりに`@WebMvcTest`を使う

```java
// 軽量なテスト
@WebMvcTest(UserController.class)
class UserControllerTest {
    @MockBean
    private UserService userService;
}
```

---

## 📚 このステップで学んだこと

- ✅ `@SpringBootTest`で統合テスト
- ✅ `MockMvc`でHTTPリクエストテスト
- ✅ `@WithMockUser`で認証テスト
- ✅ `@DataJpaTest`でリポジトリテスト
- ✅ TestContainersで実際のDBテスト
- ✅ `jsonPath()`でJSON検証
- ✅ `@Transactional`で自動ロールバック

---

## 💡 補足: テスト戦略

### テストピラミッド

```
       /\
      /E2E\       少ない（UIテスト）
     /------\
    /統合テスト\    中程度（API全体）
   /----------\
  /ユニットテスト\  多い（ビジネスロジック）
 /--------------\
```

**推奨比率**:
- ユニットテスト: 70%
- 統合テスト: 20%
- E2Eテスト: 10%

### テストの命名規則

```java
// 方法1: メソッド名_状態_期待結果
@Test
void findById_WithValidId_ReturnsUser() {}

// 方法2: Given_When_Then
@Test
void givenValidId_whenFindById_thenReturnsUser() {}

// 方法3: 日本語
@Test
@DisplayName("有効なIDでユーザーが取得できること")
void test1() {}
```

---

## ➡️ 次のステップ

[Step 29: テストカバレッジ](STEP_29.md)へ進みましょう！

次のステップでは、JaCoCoを使ってテストカバレッジを測定し、テストの網羅性を可視化します。カバレッジレポートの見方と改善方法を学びます。
