# Step 39: テストとデプロイ準備

**所要時間**: 約120分

## 🎯 このステップの目標

BlogHubアプリケーションを本番環境にデプロイする準備を整えます。

**学ぶこと**:
- Mockitoを使ったユニットテストの実装
- MockMvcを使った統合テストの実装
- JaCoCoによるテストカバレッジの測定と改善
- 本番環境用の設定ファイルの作成
- Dockerコンテナ化による環境の標準化
- 環境変数による秘密情報の管理
- マルチステージビルドによる効率的なコンテナイメージ作成
- ヘルスチェックの実装とモニタリング

**成果物**:
- 包括的なテストスイート（カバレッジ70%以上）
- 本番環境用の設定ファイル
- Dockerfileとdocker-compose-prod.yml
- 本番デプロイ可能な高品質アプリケーション

---

## 📋 事前準備

### 前提条件

- Step 38までの実装が完了していること
- Docker、Docker Composeがインストールされていること
- テスト用データベースの準備ができていること

### 確認事項

```bash
# Dockerのバージョン確認
docker --version

# Docker Composeのバージョン確認
docker-compose --version

# 既存のテストが動作することを確認
cd workspace/bloghub
./mvnw test
```

---

## 📝 ステップバイステップの手順

### Step 1: テスト用の依存関係を追加

まず、`pom.xml`にテスト用のライブラリとJaCoCoプラグインを追加します。

**pom.xml** (dependenciesセクション):

```xml
<!-- 既存のdependenciesに以下を追加 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

**pom.xml** (buildセクションのpluginsに追加):

```xml
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
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.70</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

依存関係を更新します：

```bash
./mvnw clean install
```

---

### Step 2: テスト用のapplication.ymlを作成

テスト実行時はH2データベースを使用するように設定します。

**src/test/resources/application.yml**:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  
  security:
    jwt:
      secret: test-secret-key-for-testing-purposes-must-be-at-least-256-bits
      expiration: 86400000

logging:
  level:
    org.springframework.security: DEBUG
    com.example.bloghub: DEBUG
```

---

### Step 3: AuthServiceのユニットテストを作成

Mockitoを使って依存関係をモック化し、AuthServiceのロジックをテストします。

**src/test/java/com/example/bloghub/service/AuthServiceTest.java**:

```java
package com.example.bloghub.service;

import com.example.bloghub.dto.auth.LoginRequest;
import com.example.bloghub.dto.auth.RegisterRequest;
import com.example.bloghub.entity.User;
import com.example.bloghub.repository.UserRepository;
import com.example.bloghub.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService ユニットテスト")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
    }

    @Test
    @DisplayName("新規ユーザー登録が成功する")
    void register_Success() {
        // Given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateToken(anyString())).thenReturn("jwt-token");

        // When
        String token = authService.register(registerRequest);

        // Then
        assertThat(token).isEqualTo("jwt-token");
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode("password123");
    }

    @Test
    @DisplayName("重複するユーザー名で登録失敗する")
    void register_DuplicateUsername() {
        // Given
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists");
        
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("重複するメールアドレスで登録失敗する")
    void register_DuplicateEmail() {
        // Given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    @DisplayName("正しい認証情報でログイン成功する")
    void login_Success() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateToken("testuser")).thenReturn("jwt-token");

        // When
        String token = authService.login(loginRequest);

        // Then
        assertThat(token).isEqualTo("jwt-token");
    }

    @Test
    @DisplayName("存在しないユーザーでログイン失敗する")
    void login_UserNotFound() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    @DisplayName("間違ったパスワードでログイン失敗する")
    void login_WrongPassword() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid username or password");
    }
}
```

---

### Step 4: ArticleServiceのユニットテストを作成

記事管理ロジックのテストを実装します。

**src/test/java/com/example/bloghub/service/ArticleServiceTest.java**:

```java
package com.example.bloghub.service;

import com.example.bloghub.dto.article.ArticleCreateRequest;
import com.example.bloghub.dto.article.ArticleUpdateRequest;
import com.example.bloghub.entity.Article;
import com.example.bloghub.entity.User;
import com.example.bloghub.repository.ArticleRepository;
import com.example.bloghub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArticleService ユニットテスト")
class ArticleServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ArticleService articleService;

    private User testUser;
    private Article testArticle;
    private ArticleCreateRequest createRequest;
    private ArticleUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testArticle = new Article();
        testArticle.setId(1L);
        testArticle.setTitle("Test Article");
        testArticle.setContent("Test Content");
        testArticle.setAuthor(testUser);
        testArticle.setCreatedAt(LocalDateTime.now());
        testArticle.setUpdatedAt(LocalDateTime.now());

        createRequest = new ArticleCreateRequest();
        createRequest.setTitle("New Article");
        createRequest.setContent("New Content");

        updateRequest = new ArticleUpdateRequest();
        updateRequest.setTitle("Updated Title");
        updateRequest.setContent("Updated Content");
    }

    @Test
    @DisplayName("記事を正常に作成できる")
    void createArticle_Success() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(articleRepository.save(any(Article.class))).thenReturn(testArticle);

        // When
        Article result = articleService.createArticle(createRequest, "testuser");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Article");
        verify(articleRepository, times(1)).save(any(Article.class));
    }

    @Test
    @DisplayName("存在しないユーザーで記事作成に失敗する")
    void createArticle_UserNotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> articleService.createArticle(createRequest, "nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
        
        verify(articleRepository, never()).save(any(Article.class));
    }

    @Test
    @DisplayName("記事一覧を取得できる")
    void getAllArticles_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Article> articlePage = new PageImpl<>(Arrays.asList(testArticle));
        when(articleRepository.findAll(pageable)).thenReturn(articlePage);

        // When
        Page<Article> result = articleService.getAllArticles(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Test Article");
    }

    @Test
    @DisplayName("IDで記事を取得できる")
    void getArticleById_Success() {
        // Given
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));

        // When
        Article result = articleService.getArticleById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Article");
    }

    @Test
    @DisplayName("存在しないIDで記事取得に失敗する")
    void getArticleById_NotFound() {
        // Given
        when(articleRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> articleService.getArticleById(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Article not found");
    }

    @Test
    @DisplayName("作成者が記事を更新できる")
    void updateArticle_Success() {
        // Given
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));
        when(articleRepository.save(any(Article.class))).thenReturn(testArticle);

        // When
        Article result = articleService.updateArticle(1L, updateRequest, "testuser");

        // Then
        assertThat(result).isNotNull();
        verify(articleRepository, times(1)).save(any(Article.class));
    }

    @Test
    @DisplayName("作成者以外が記事を更新しようとして失敗する")
    void updateArticle_Forbidden() {
        // Given
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));

        // When & Then
        assertThatThrownBy(() -> articleService.updateArticle(1L, updateRequest, "otheruser"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("You are not the author");
        
        verify(articleRepository, never()).save(any(Article.class));
    }

    @Test
    @DisplayName("作成者が記事を削除できる")
    void deleteArticle_Success() {
        // Given
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));
        doNothing().when(articleRepository).delete(any(Article.class));

        // When
        articleService.deleteArticle(1L, "testuser");

        // Then
        verify(articleRepository, times(1)).delete(testArticle);
    }

    @Test
    @DisplayName("作成者以外が記事を削除しようとして失敗する")
    void deleteArticle_Forbidden() {
        // Given
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));

        // When & Then
        assertThatThrownBy(() -> articleService.deleteArticle(1L, "otheruser"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("You are not the author");
        
        verify(articleRepository, never()).delete(any(Article.class));
    }
}
```

---

### Step 5: AuthControllerの統合テストを作成

MockMvcを使ってエンドツーエンドのAPIテストを実装します。

**src/test/java/com/example/bloghub/controller/AuthControllerIntegrationTest.java**:

```java
package com.example.bloghub.controller;

import com.example.bloghub.dto.auth.LoginRequest;
import com.example.bloghub.dto.auth.RegisterRequest;
import com.example.bloghub.repository.UserRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController 統合テスト")
class AuthControllerIntegrationTest {

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
    @DisplayName("新規ユーザー登録が成功する")
    void register_Success() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("newuser@example.com");
        request.setPassword("password123");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("重複するユーザー名で登録が失敗する")
    void register_DuplicateUsername() throws Exception {
        // Given - 最初のユーザーを登録
        RegisterRequest firstRequest = new RegisterRequest();
        firstRequest.setUsername("testuser");
        firstRequest.setEmail("test1@example.com");
        firstRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)));

        // 同じユーザー名で再度登録
        RegisterRequest secondRequest = new RegisterRequest();
        secondRequest.setUsername("testuser");
        secondRequest.setEmail("test2@example.com");
        secondRequest.setPassword("password123");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("バリデーションエラー - ユーザー名が空")
    void register_EmptyUsername() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("ログインが成功する")
    void login_Success() throws Exception {
        // Given - ユーザーを登録
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("loginuser");
        registerRequest.setEmail("login@example.com");
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        // ログインリクエスト
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("loginuser");
        loginRequest.setPassword("password123");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("間違ったパスワードでログインが失敗する")
    void login_WrongPassword() throws Exception {
        // Given - ユーザーを登録
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("loginuser");
        registerRequest.setEmail("login@example.com");
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        // 間違ったパスワードでログイン
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("loginuser");
        loginRequest.setPassword("wrongpassword");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("存在しないユーザーでログインが失敗する")
    void login_UserNotFound() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("nonexistent");
        loginRequest.setPassword("password123");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }
}
```

---

### Step 6: ArticleControllerの統合テストを作成

JWTトークンを使った認証テストを含む統合テストを実装します。

**src/test/java/com/example/bloghub/controller/ArticleControllerIntegrationTest.java**:

```java
package com.example.bloghub.controller;

import com.example.bloghub.dto.article.ArticleCreateRequest;
import com.example.bloghub.dto.article.ArticleUpdateRequest;
import com.example.bloghub.dto.auth.RegisterRequest;
import com.example.bloghub.entity.Article;
import com.example.bloghub.entity.User;
import com.example.bloghub.repository.ArticleRepository;
import com.example.bloghub.repository.UserRepository;
import com.example.bloghub.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ArticleController 統合テスト")
class ArticleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String jwtToken;
    private User testUser;
    private Article testArticle;

    @BeforeEach
    void setUp() {
        articleRepository.deleteAll();
        userRepository.deleteAll();

        // テストユーザーを作成
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser = userRepository.save(testUser);

        // JWTトークンを生成
        jwtToken = jwtTokenProvider.generateToken(testUser.getUsername());

        // テスト用の記事を作成
        testArticle = new Article();
        testArticle.setTitle("Test Article");
        testArticle.setContent("Test Content");
        testArticle.setAuthor(testUser);
        testArticle.setCreatedAt(LocalDateTime.now());
        testArticle.setUpdatedAt(LocalDateTime.now());
        testArticle = articleRepository.save(testArticle);
    }

    @Test
    @DisplayName("認証済みユーザーが記事を作成できる")
    void createArticle_Success() throws Exception {
        // Given
        ArticleCreateRequest request = new ArticleCreateRequest();
        request.setTitle("New Article");
        request.setContent("New Content");

        // When & Then
        mockMvc.perform(post("/api/articles")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Article"))
                .andExpect(jsonPath("$.content").value("New Content"))
                .andExpect(jsonPath("$.author.username").value("testuser"));
    }

    @Test
    @DisplayName("認証なしで記事作成が失敗する")
    void createArticle_Unauthorized() throws Exception {
        // Given
        ArticleCreateRequest request = new ArticleCreateRequest();
        request.setTitle("New Article");
        request.setContent("New Content");

        // When & Then
        mockMvc.perform(post("/api/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("記事一覧を取得できる")
    void getAllArticles_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/articles")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Test Article"));
    }

    @Test
    @DisplayName("IDで記事を取得できる")
    void getArticleById_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/articles/" + testArticle.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Article"))
                .andExpect(jsonPath("$.content").value("Test Content"));
    }

    @Test
    @DisplayName("存在しないIDで記事取得が失敗する")
    void getArticleById_NotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/articles/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("作成者が記事を更新できる")
    void updateArticle_Success() throws Exception {
        // Given
        ArticleUpdateRequest request = new ArticleUpdateRequest();
        request.setTitle("Updated Title");
        request.setContent("Updated Content");

        // When & Then
        mockMvc.perform(put("/api/articles/" + testArticle.getId())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.content").value("Updated Content"));
    }

    @Test
    @DisplayName("作成者以外が記事を更新しようとして失敗する")
    void updateArticle_Forbidden() throws Exception {
        // Given - 別のユーザーを作成
        User otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword(passwordEncoder.encode("password123"));
        otherUser = userRepository.save(otherUser);

        String otherToken = jwtTokenProvider.generateToken(otherUser.getUsername());

        ArticleUpdateRequest request = new ArticleUpdateRequest();
        request.setTitle("Updated Title");
        request.setContent("Updated Content");

        // When & Then
        mockMvc.perform(put("/api/articles/" + testArticle.getId())
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("作成者が記事を削除できる")
    void deleteArticle_Success() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/articles/" + testArticle.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        // 削除されたことを確認
        mockMvc.perform(get("/api/articles/" + testArticle.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("作成者以外が記事を削除しようとして失敗する")
    void deleteArticle_Forbidden() throws Exception {
        // Given - 別のユーザーを作成
        User otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword(passwordEncoder.encode("password123"));
        otherUser = userRepository.save(otherUser);

        String otherToken = jwtTokenProvider.generateToken(otherUser.getUsername());

        // When & Then
        mockMvc.perform(delete("/api/articles/" + testArticle.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("キーワードで記事を検索できる")
    void searchArticles_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/articles/search")
                        .param("keyword", "Test")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Test Article"));
    }
}
```

---

### Step 7: テストを実行してカバレッジを確認

すべてのテストを実行し、JaCoCoでカバレッジレポートを生成します。

```bash
# テストを実行
./mvnw clean test

# JaCoCoレポートを生成
./mvnw jacoco:report
```

レポートを確認します：

```bash
# ブラウザでレポートを開く（macOS）
open target/site/jacoco/index.html

# ブラウザでレポートを開く（Linux/WSL2）
xdg-open target/site/jacoco/index.html
```

**カバレッジの確認ポイント**:
- 全体のカバレッジが70%以上であることを確認
- Serviceレイヤーのカバレッジが高いことを確認
- 未テストの重要なメソッドがないかチェック

カバレッジが不足している場合は、追加のテストを作成してください。

---

### Step 8: 本番環境用の設定ファイルを作成

環境変数を使った本番環境設定を作成します。

**src/main/resources/application-prod.yml**:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://mysql:3306/bloghub}
    username: ${DB_USERNAME:bloghub_user}
    password: ${DB_PASSWORD:bloghub_password}
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: false
  
  security:
    jwt:
      secret: ${JWT_SECRET}
      expiration: 86400000
  
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

logging:
  level:
    root: INFO
    com.example.bloghub: INFO
  file:
    name: logs/bloghub.log
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

server:
  port: 8080
  error:
    include-message: always
    include-stacktrace: never
```

**src/main/resources/application.yml**を更新して、デフォルトプロファイルを設定：

```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  
  application:
    name: bloghub

# 他の設定は既存のまま
```

---

### Step 9: Dockerfileを作成

マルチステージビルドでコンテナイメージを最適化します。

**Dockerfile** (プロジェクトルート):

```dockerfile
# ビルドステージ
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Mavenラッパーとpom.xmlをコピー
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# 依存関係をダウンロード（キャッシュ活用）
RUN ./mvnw dependency:go-offline

# ソースコードをコピー
COPY src src

# アプリケーションをビルド
RUN ./mvnw clean package -DskipTests

# 実行ステージ
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# ビルドステージから成果物をコピー
COPY --from=builder /app/target/*.jar app.jar

# ログディレクトリを作成
RUN mkdir -p /app/logs

# ヘルスチェック用のエンドポイントを設定
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# ポートを公開
EXPOSE 8080

# アプリケーションを実行
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**pom.xml**にActuatorを追加（ヘルスチェック用）：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**application-prod.yml**にActuator設定を追加：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: when-authorized
```

---

### Step 10: 本番環境用のDocker Compose設定を作成

本番環境用の`docker-compose-prod.yml`を作成します。

**docker-compose-prod.yml** (プロジェクトルート):

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: bloghub-mysql-prod
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: bloghub
      MYSQL_USER: ${DB_USERNAME}
      MYSQL_PASSWORD: ${DB_PASSWORD}
    volumes:
      - mysql_data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "3306:3306"
    networks:
      - bloghub-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD}"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: bloghub-app-prod
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:mysql://mysql:3306/bloghub?useSSL=false&allowPublicKeyRetrieval=true
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
    depends_on:
      mysql:
        condition: service_healthy
    networks:
      - bloghub-network
    volumes:
      - ./logs:/app/logs
    restart: unless-stopped

volumes:
  mysql_data:
    driver: local

networks:
  bloghub-network:
    driver: bridge
```

**.env.example** (プロジェクトルート):

```env
# MySQL設定
MYSQL_ROOT_PASSWORD=your_strong_root_password_here
DB_USERNAME=bloghub_user
DB_PASSWORD=your_strong_db_password_here

# JWT設定（最低256ビット必要）
JWT_SECRET=your_jwt_secret_key_must_be_at_least_256_bits_long_please_change_this_in_production

# Spring Boot設定
SPRING_PROFILES_ACTIVE=prod
```

**.gitignore**に環境変数ファイルを追加：

```
# 環境変数ファイル
.env
```

実際の環境変数ファイルを作成：

```bash
# .env.exampleをコピーして実際の環境変数ファイルを作成
cp .env.example .env

# .envファイルを編集して実際の値を設定
# エディタで.envを開いて強力なパスワードとシークレットキーに変更してください
```

---

## ✅ 動作確認

### 1. テストの実行

すべてのテストが成功することを確認します：

```bash
cd workspace/bloghub

# すべてのテストを実行
./mvnw clean test

# 期待される結果:
# Tests run: XX, Failures: 0, Errors: 0, Skipped: 0
```

### 2. カバレッジの確認

JaCoCoレポートで70%以上のカバレッジを確認：

```bash
# カバレッジレポートを生成
./mvnw jacoco:report

# ブラウザでレポートを開く（macOS）
open target/site/jacoco/index.html

# ブラウザでレポートを開く（Linux/WSL2）
xdg-open target/site/jacoco/index.html
```

**確認ポイント**:
- 全体のカバレッジが70%以上
- Serviceレイヤーのカバレッジが80%以上
- Controllerレイヤーのカバレッジが70%以上

### 3. Dockerビルドの確認

Dockerイメージをビルドして動作確認：

```bash
# Dockerイメージをビルド
docker build -t bloghub:latest .

# イメージが作成されたことを確認
docker images | grep bloghub
```

### 4. 本番環境でのアプリケーション起動

Docker Composeで本番環境を起動：

```bash
# .envファイルが存在することを確認
ls -la .env

# Docker Composeで起動
docker-compose -f docker-compose-prod.yml up -d

# ログを確認
docker-compose -f docker-compose-prod.yml logs -f app
```

### 5. ヘルスチェック

アプリケーションが正常に起動したことを確認：

```bash
# ヘルスチェックエンドポイントを確認
curl http://localhost:8080/actuator/health

# 期待される結果:
# {"status":"UP"}
```

### 6. APIの動作確認

本番環境でAPIが動作することを確認：

```bash
# ユーザー登録
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "produser",
    "email": "prod@example.com",
    "password": "password123"
  }'

# レスポンスからトークンを取得してログイン確認
TOKEN="取得したトークン"

# 記事作成
curl -X POST http://localhost:8080/api/articles \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "title": "本番環境テスト",
    "content": "本番環境で正常に動作しています"
  }'

# 記事一覧取得
curl http://localhost:8080/api/articles
```

### 7. 環境のクリーンアップ

テスト完了後、環境を停止：

```bash
# コンテナを停止して削除
docker-compose -f docker-compose-prod.yml down

# データも削除する場合（注意！）
docker-compose -f docker-compose-prod.yml down -v
```

---

## 🎯 チャレンジ課題

基本的な実装ができたら、以下の課題に挑戦してみましょう。

### 課題1: E2Eテストの実装

SeleniumまたはPlaywrightを使って、ブラウザベースのE2Eテストを実装してください。

**ヒント**:
```xml
<!-- pom.xmlに追加 -->
<dependency>
    <groupId>org.seleniumhq.selenium</groupId>
    <artifactId>selenium-java</artifactId>
    <scope>test</scope>
</dependency>
```

**テストシナリオ**:
1. ユーザー登録フォームから新規登録
2. ログインフォームでログイン
3. 記事作成フォームから新規記事を投稿
4. 記事一覧ページで投稿した記事を確認

### 課題2: CI/CDパイプラインの構築

GitHub Actionsを使って自動テスト・自動デプロイを実装してください。

**.github/workflows/ci.yml**:
```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Run tests
        run: ./mvnw clean test
      - name: Generate coverage report
        run: ./mvnw jacoco:report
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
```

### 課題3: クラウドデプロイ

AWS ECS、Google Cloud Run、またはAzure App Serviceにデプロイしてください。

**Google Cloud Runの例**:
```bash
# Google Cloud SDKのインストール後

# プロジェクトを設定
gcloud config set project YOUR_PROJECT_ID

# コンテナイメージをビルドしてプッシュ
gcloud builds submit --tag gcr.io/YOUR_PROJECT_ID/bloghub

# Cloud Runにデプロイ
gcloud run deploy bloghub \
  --image gcr.io/YOUR_PROJECT_ID/bloghub \
  --platform managed \
  --region asia-northeast1 \
  --allow-unauthenticated \
  --set-env-vars DB_URL=$DB_URL,DB_USERNAME=$DB_USERNAME,DB_PASSWORD=$DB_PASSWORD,JWT_SECRET=$JWT_SECRET
```

---

## 🔧 トラブルシューティング

### 問題1: テストが失敗する（データベース接続エラー）

**症状**:
```
org.springframework.jdbc.CannotGetJdbcConnectionException: Failed to obtain JDBC Connection
```

**原因**: H2データベースの設定が正しくない

**解決策**:
1. `src/test/resources/application.yml`が存在することを確認
2. H2依存関係が`pom.xml`に含まれていることを確認
3. テストクラスに`@ActiveProfiles("test")`が付いていることを確認

### 問題2: JaCoCoレポートが生成されない

**症状**:
```bash
./mvnw jacoco:report
# target/site/jacoco/ディレクトリが存在しない
```

**原因**: テストが実行されていない、またはJaCoCoエージェントが起動していない

**解決策**:
```bash
# テストを実行してからレポートを生成
./mvnw clean test jacoco:report

# またはまとめて実行
./mvnw clean verify
```

### 問題3: Dockerビルドエラー

**症状**:
```
ERROR: failed to solve: process "/bin/sh -c ./mvnw clean package -DskipTests" did not complete successfully
```

**原因**: Mavenラッパーに実行権限がない

**解決策**:
```bash
# mvnwに実行権限を付与
chmod +x mvnw

# Dockerfileを修正して権限を付与
# Dockerfileに以下を追加
RUN chmod +x mvnw
```

### 問題4: 本番環境で環境変数が読み込まれない

**症状**:
アプリケーションが起動時に環境変数が見つからないエラーを出す

**原因**: `.env`ファイルが存在しない、または値が設定されていない

**解決策**:
```bash
# .envファイルを作成
cp .env.example .env

# .envファイルを編集して実際の値を設定
vim .env

# Docker Composeで環境変数を確認
docker-compose -f docker-compose-prod.yml config

# コンテナ内で環境変数を確認
docker-compose -f docker-compose-prod.yml exec app env | grep DB_
```

### 問題5: JWT_SECRETが短すぎるエラー

**症状**:
```
The specified key byte array is 128 bits which is not secure enough
```

**原因**: JWT_SECRETが256ビット（32文字）未満

**解決策**:
```bash
# 強力なシークレットキーを生成（64文字）
openssl rand -base64 48

# .envファイルに設定
JWT_SECRET=生成されたキー
```

---

## 📚 まとめ

お疲れさまでした！このステップでは、本番環境にデプロイ可能な高品質なアプリケーションを構築するための重要なスキルを学びました。

**学んだこと**:

1. **Mockitoによるユニットテスト**: 依存関係をモック化して単体のロジックをテスト
2. **MockMvcによる統合テスト**: エンドツーエンドのAPIリクエストをテスト
3. **JaCoCoによるカバレッジ測定**: テストの網羅性を定量的に評価
4. **テスト駆動開発の重要性**: バグを早期に発見し、リファクタリングを安全に実施
5. **本番環境設定の分離**: 環境ごとに異なる設定を管理
6. **環境変数による秘密情報管理**: パスワードやシークレットキーをコードに埋め込まない
7. **Dockerによるコンテナ化**: 環境差異を排除し、再現性を確保
8. **マルチステージビルド**: コンテナイメージサイズを最適化
9. **ヘルスチェックの実装**: アプリケーションの稼働状況を監視
10. **本番環境での運用準備**: ログ、モニタリング、エラーハンドリングの設定

**重要なポイント**:

- **カバレッジ70%以上**: 重要なロジックが確実にテストされていることを保証
- **環境変数の適切な管理**: `.env`ファイルは`.gitignore`に追加し、コミットしない
- **本番環境とローカル環境の分離**: `application-prod.yml`で本番専用の設定を管理
- **セキュリティの強化**: JWT_SECRETは十分に長く、ランダムな値を使用
- **ヘルスチェック**: コンテナオーケストレーションでの自動再起動に活用

---

## 🎓 次のステップへの誘導

### 🎉 おめでとうございます！

**Phase 8: 総合演習（BlogHub）の全38ステップを完了しました！**

あなたはSpring Boot 3.5を使った本格的なWebアプリケーション開発のすべてを学び、実践しました。ここまでの学習で身につけたスキルは、実務プロジェクトで即座に活用できるレベルに達しています。

### これまでの成果を振り返りましょう

**Phase 1-2**: Spring Bootの基礎とデータベース連携
- REST APIの作成
- Spring Data JPAによるCRUD操作
- トランザクション管理とリレーションシップ

**Phase 3**: MyBatisによるSQL制御
- カスタムクエリの実装
- JPAとMyBatisの使い分け

**Phase 4**: アーキテクチャとベストプラクティス
- レイヤー化アーキテクチャ
- DI/IoCコンテナ
- 例外ハンドリングとバリデーション

**Phase 5**: Thymeleafでサーバーサイドレンダリング
- テンプレートエンジンの活用
- フォーム送信とバリデーション

**Phase 6**: セキュリティとテスト
- Spring Security
- JWTトークン認証
- ユニットテスト・統合テスト

**Phase 7**: 実践的な機能
- ファイルアップロード
- ページネーション
- キャッシュと非同期処理

**Phase 8**: 総合演習（BlogHub）
- 認証・認可機能
- 記事とコメント機能
- 画像アップロード
- 検索機能
- **本番デプロイ準備とテスト**

### 次の学習パス

Spring Bootをマスターしたあなたには、さらなる挑戦の道が開かれています：

#### 1. Spring Cloud（マイクロサービス）
- サービスディスカバリ（Eureka）
- API Gateway
- 分散トレーシング（Zipkin）
- サーキットブレーカー（Resilience4j）

**参考リソース**:
- [Spring Cloud公式ドキュメント](https://spring.io/projects/spring-cloud)

#### 2. Kotlin + Spring Boot
- Kotlinの簡潔な文法でSpring Boot開発
- コルーチンによる非同期処理
- Kotlin DSLによる設定

**参考リソース**:
- [Spring Boot with Kotlin](https://spring.io/guides/tutorials/spring-boot-kotlin/)

#### 3. GraphQL with Spring Boot
- RESTの代替としてのGraphQL
- Spring for GraphQLによる実装
- N+1問題の解決

**参考リソース**:
- [Spring for GraphQL](https://spring.io/projects/spring-graphql)

#### 4. リアクティブプログラミング
- Spring WebFlux
- R2DBC（リアクティブデータベース接続）
- Project Reactorによる非同期ストリーム処理

**参考リソース**:
- [Spring WebFlux](https://docs.spring.io/spring-framework/reference/web/webflux.html)

#### 5. 実務プロジェクトへの応用
- オープンソースプロジェクトへの貢献
- 自分のアイデアをアプリケーション化
- ポートフォリオの充実

### コミュニティへの参加

**Spring Boot開発者コミュニティ**:
- [Spring公式Slack](https://spring.io/community)
- [Stack Overflow - Spring Boot](https://stackoverflow.com/questions/tagged/spring-boot)
- [GitHub - Spring Projects](https://github.com/spring-projects)

**日本語コミュニティ**:
- [JJUG（日本Javaユーザーグループ）](https://www.java-users.jp/)
- [Spring Fest](https://springfest.connpass.com/)

### 最後に

このカリキュラムを完走したあなたは、Spring Boot開発者としての確かな基盤を築きました。学んだ知識とスキルを活かして、素晴らしいアプリケーションを作り続けてください。

**あなたのSpring Boot開発の旅は、ここから新たな章を迎えます。**

頑張ってください！ 🚀

---

**参考リソース**:
- [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/reference/3.5.8/)
- [Testing in Spring Boot](https://docs.spring.io/spring-boot/reference/testing/index.html)
- [JaCoCo Documentation](https://www.jacoco.org/jacoco/trunk/doc/)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [The Twelve-Factor App](https://12factor.net/)

---

**前のステップ**: [Step 38: 画像アップロードと検索機能](STEP_38.md)  
**Phase 8完了**: すべてのステップを完了しました！おめでとうございます！🎉
