# Step 28: 統合テストとAPI テスト

## 🎯 このステップの目標

- `@SpringBootTest`を使った統合テストを理解する
- MockMvcでAPIエンドポイントをテストする
- TestContainersでデータベーステストを実行する
- ✅ E2Eテストの書き方
- ✅ TestRestTemplateの使い方
- ✅ データベースのテストデータ準備

**所要時間**: 約1時間30分

---

## 📋 事前準備

- Step 27のユニットテストが理解できていること

---

## 🚀 ステップ1: Controller統合テスト

### 1-1. UserControllerIntegrationTest

> **💡 前提**: このテストは以下のステップで実装した機能をテストします:
> - [Step 8: CRUD操作の完成](../phase2/STEP_8.md) - UserControllerの基本的なCRUD操作
>   - `POST /api/users` - ユーザー作成
>   - `GET /api/users` - 全ユーザー取得
>   - `GET /api/users/{id}` - IDでユーザー取得
>   - `PUT /api/users/{id}` - ユーザー更新
>   - `DELETE /api/users/{id}` - ユーザー削除
> - [Step 19: DTOとEntityの分離](../phase4/STEP_19.md) - リクエスト/レスポンス用のDTOクラス
>   - `UserCreateRequest` - ユーザー作成リクエスト用DTO
>   - `UserUpdateRequest` - ユーザー更新リクエスト用DTO
>   - `UserResponse` - ユーザーレスポンス用DTO
> - [Step 18: バリデーション](../phase4/STEP_18.md) - リクエストデータのバリデーション
>   - `@Valid`を使ったリクエストボディの検証
>   - `UserCreateRequest`や`UserUpdateRequest`への`@NotBlank`, `@Email`, `@Min`, `@Max`などのアノテーション
>   - バリデーションエラー時の400 Bad Requestレスポンス
> - [Step 17: 例外ハンドリング](../phase4/STEP_17.md) - グローバル例外ハンドラー（オプション）
>   - `@RestControllerAdvice`によるバリデーションエラーのレスポンス整形

**ファイルパス**: `src/test/java/com/example/hellospringboot/controller/UserControllerIntegrationTest.java`

```java
package com.example.hellospringboot.controller;

import com.example.hellospringboot.dto.request.UserCreateRequest;
import com.example.hellospringboot.entity.User;
import com.example.hellospringboot.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController統合テスト
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("UserController Integration Tests")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("ユーザーを作成できること")
    void testCreateUser() throws Exception {
        // Given
        UserCreateRequest request = UserCreateRequest.builder()
                .name("Test User")
                .email("test@example.com")
                .age(25)
                .build();

        // When & Then
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.age").value(25));
    }

    @Test
    @DisplayName("バリデーションエラーの場合は400を返すこと")
    void testCreateUserValidationError() throws Exception {
        // Given - 無効なリクエスト
        UserCreateRequest request = UserCreateRequest.builder()
                .name("")  // 空の名前
                .email("invalid-email")  // 無効なメール
                .age(-1)  // 無効な年齢
                .build();

        // When & Then
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @DisplayName("全ユーザーを取得できること")
    void testGetAllUsers() throws Exception {
        // Given
        userRepository.save(User.builder()
                .name("User1")
                .email("user1@example.com")
                .age(20)
                .build());
        userRepository.save(User.builder()
                .name("User2")
                .email("user2@example.com")
                .age(30)
                .build());

        // When & Then
        mockMvc.perform(get("/api/users"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("User1"))
                .andExpect(jsonPath("$[1].name").value("User2"));
    }

    @Test
    @DisplayName("IDでユーザーを取得できること")
    void testGetUserById() throws Exception {
        // Given
        User user = userRepository.save(User.builder()
                .name("Test User")
                .email("test@example.com")
                .age(25)
                .build());

        // When & Then
        mockMvc.perform(get("/api/users/{id}", user.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.name").value("Test User"));
    }

    @Test
    @DisplayName("存在しないIDの場合は404を返すこと")
    void testGetUserByIdNotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/users/999"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("ユーザーを更新できること")
    void testUpdateUser() throws Exception {
        // Given
        User user = userRepository.save(User.builder()
                .name("Old Name")
                .email("old@example.com")
                .age(20)
                .build());

        UserCreateRequest updateRequest = UserCreateRequest.builder()
                .name("New Name")
                .email("new@example.com")
                .age(30)
                .build();

        // When & Then
        mockMvc.perform(put("/api/users/{id}", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.age").value(30));
    }

    @Test
    @DisplayName("ユーザーを削除できること")
    void testDeleteUser() throws Exception {
        // Given
        User user = userRepository.save(User.builder()
                .name("Test User")
                .email("test@example.com")
                .age(25)
                .build());

        // When & Then
        mockMvc.perform(delete("/api/users/{id}", user.getId()))
                .andDo(print())
                .andExpect(status().isNoContent());
    }
}
```

---

## 🚀 ステップ2: テスト用設定ファイル

### 2-1. application-test.yml

**ファイルパス**: `src/test/resources/application-test.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/hello_db_test?useSSL=false&serverTimezone=Asia/Tokyo
    username: appuser
    password: apppassword
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect

logging:
  level:
    com.example.hellospringboot: DEBUG
    org.springframework.test: INFO
```

> **💡 ヒント**: テスト用に別のデータベース（`hello_db_test`）を使用することで、本番データと分離できます。テスト前に以下のコマンドでテスト用DBを作成してください：
> ```sql
> CREATE DATABASE IF NOT EXISTS hello_db_test;
> ```

---

## 🚀 ステップ3: 認証付きAPIテスト

### 3-1. AuthControllerIntegrationTest

> **💡 前提**: このテストは [Step 26: JWTトークン認証](STEP_26.md) で実装したAuthControllerをテストします。
> 以下の機能が実装されている必要があります:
> - `POST /api/auth/login` - ログインしてJWTトークンを取得
> - Step 26のStep 0でデータベース認証の準備（User entity、UserRepository.findByEmail()、ユーザー登録API）
> - Step 26のStep 6-2でCustomUserDetailsServiceの実装

**ファイルパス**: `src/test/java/com/example/hellospringboot/controller/AuthControllerIntegrationTest.java`

```java
package com.example.hellospringboot.controller;

import com.example.hellospringboot.dto.request.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("正しい認証情報でログインできること")
    void testLoginSuccess() throws Exception {
        // Given
        LoginRequest request = LoginRequest.builder()
                .username("user")
                .password("user123")
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.username").value("user"));
    }

    @Test
    @DisplayName("誤った認証情報の場合は401を返すこと")
    void testLoginFailure() throws Exception {
        // Given
        LoginRequest request = LoginRequest.builder()
                .username("user")
                .password("wrongpassword")
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
}
```

---

## 🚀 ステップ4: テスト実行とレポート

### 4-1. すべてのテストを実行

```bash
./mvnw clean test
```

### 4-2. 特定のテストクラスのみ実行

```bash
./mvnw test -Dtest=UserControllerIntegrationTest
```

### 4-3. カバレッジレポート確認

```bash
./mvnw clean test jacoco:report
open target/site/jacoco/index.html
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: PostController統合テスト

PostControllerの統合テストを作成してください。

### チャレンジ 2: TestContainersの導入

本物のMySQLを使ったテストを実装してください。

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
```

### チャレンジ 3: REST Assuredの使用

REST Assuredライブラリを使ったテストを書いてください。

---

## 📚 このステップで学んだこと

- ✅ @SpringBootTestによる統合テスト
- ✅ MockMvcによるAPIテスト
- ✅ JSONパスアサーション
- ✅ テスト用設定ファイル
- ✅ トランザクションロールバック

---

## � トラブルシューティング

### エラー1: "Failed to load ApplicationContext"

```
java.lang.IllegalStateException: Failed to load ApplicationContext
Caused by: org.springframework.beans.factory.BeanCreationException
```

**原因**: テスト実行時にSpring Bootアプリケーションコンテキストの起動に失敗している（設定ミス、Bean定義エラーなど）

**解決策**:

1. `application-test.yml`の設定を確認：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/hello_db_test  # ← テスト用DBが存在するか確認
    username: appuser
    password: apppassword
```

2. テスト用データベースを作成：

```sql
CREATE DATABASE IF NOT EXISTS hello_db_test;
```

3. MySQLが起動しているか確認：

```bash
sudo systemctl status mysql
```

---

### エラー2: "No qualifying bean of type 'MockMvc' available"

```
org.springframework.beans.factory.NoSuchBeanDefinitionException: No qualifying bean of type 'org.springframework.test.web.servlet.MockMvc' available
```

**原因**: `@AutoConfigureMockMvc`アノテーションが不足している

**解決策**:

```java
@SpringBootTest
@AutoConfigureMockMvc  // ← これが必要
class UserControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
}
```

---

### エラー3: テスト実行時に "401 Unauthorized" が返る

```
MockHttpServletResponse:
           Status = 401
    Error message = Unauthorized
```

**原因**: Spring Securityが有効で、テストがトークンなしでAPIにアクセスしている

**解決策1**: テスト用にセキュリティを無効化

```java
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@TestPropertySource(properties = {
    "spring.security.enabled=false"  // ← セキュリティ無効化
})
class UserControllerIntegrationTest {
    // ...
}
```

**解決策2**: `@WithMockUser`を使う

```java
import org.springframework.security.test.context.support.WithMockUser;

@Test
@WithMockUser(username = "testuser", roles = {"USER"})  // ← モックユーザーでテスト
void testGetAllUsers() throws Exception {
    mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk());
}
```

**解決策3**: JWTトークンを生成してヘッダーに含める

```java
@Test
void testGetAllUsersWithToken() throws Exception {
    // トークン生成（実際のログインAPIを呼ぶか、JwtUtilで直接生成）
    String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
    
    mockMvc.perform(get("/api/users")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
}
```

---

### エラー4: JSONパスアサーションが失敗する

```
java.lang.AssertionError: JSON path "$.name" expected:<Test User> but was:<null>
```

**原因**: レスポンスボディのJSONフィールド名が期待と異なる、またはnullが返っている

**解決策**:

1. レスポンスボディをデバッグ出力で確認：

```java
mockMvc.perform(get("/api/users/1"))
        .andDo(print())  // ← レスポンス全体を出力
        .andExpect(status().isOk());
```

2. 実際のJSONフィールド名を確認：

```json
{
  "userId": 1,        // ← フィールド名が "name" ではなく "userId"
  "userName": "Test"  // ← "name" ではなく "userName"
}
```

3. JSONパスを修正：

```java
mockMvc.perform(get("/api/users/1"))
        .andExpect(jsonPath("$.userId").value(1))
        .andExpect(jsonPath("$.userName").value("Test"));  // ← 正しいフィールド名
```

---

### エラー5: トランザクションロールバックされない

```java
@Test
void testCreateUser() {
    // テスト実行後もDBにデータが残っている
}
```

**原因**: `@Transactional`アノテーションが不足しているか、テストメソッドがトランザクション外で実行されている

**解決策**:

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional  // ← クラスレベルで追加（各テスト後に自動ロールバック）
class UserControllerIntegrationTest {
    
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();  // ← 念のため明示的にクリア
    }
    
    @Test
    void testCreateUser() {
        // テスト実行
    }
}
```

**または**、各テスト前にDBをクリーンアップ：

```java
@BeforeEach
void setUp() {
    userRepository.deleteAll();
}

@AfterEach
void tearDown() {
    userRepository.deleteAll();
}
```

---

### エラー6: MockMvcでPOST/PUTリクエストが "415 Unsupported Media Type" になる

```
MockHttpServletResponse:
           Status = 415
    Error message = Unsupported Media Type
```

**原因**: リクエストに`Content-Type`ヘッダーが設定されていない

**解決策**:

```java
// ❌ Content-Typeなし
mockMvc.perform(post("/api/users")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

// ✅ Content-Typeを指定
mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)  // ← 必須
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
```

---

## �🔄 Gitへのコミットとレビュー依頼

```bash
git add .
git commit -m "Step 28: 統合テスト完了"
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

次は[Step 29: テストカバレッジ](STEP_29.md)へ進みましょう！

JaCoCoを使ってテストの網羅性を確認し、品質を向上させます。

---

お疲れさまでした！ 🎉

統合テストを習得しました！実際のHTTPリクエストをテストする方法が
