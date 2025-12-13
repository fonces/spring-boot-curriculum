# Step 38: テストとデプロイ準備

## 🎯 このステップの目標

- ユニットテストでビジネスロジックを検証できる
- 統合テストでAPIエンドポイントをテストできる
- テストカバレッジを測定し、品質を保証できる
- 本番環境用の設定を準備できる
- デプロイメント戦略を理解できる

**所要時間**: 約90分

---

## 📋 事前準備

- Step 37までの内容を完了していること
- Phase 6 Step 27-29のテスト技法を復習していること
- JUnit 5とMockitoの基礎を理解していること

---

## 🚀 ステップ1: ユニットテストの実装

### 1-1. ArticleServiceのテスト

`src/test/java/com/example/bloghub/services/ArticleServiceTest.java`を作成：

```java
package com.example.bloghub.services;

import com.example.bloghub.dto.request.ArticleCreateRequest;
import com.example.bloghub.dto.response.ArticleResponse;
import com.example.bloghub.entities.Article;
import com.example.bloghub.entities.Role;
import com.example.bloghub.entities.User;
import com.example.bloghub.repositories.ArticleRepository;
import com.example.bloghub.repositories.TagRepository;
import com.example.bloghub.repositories.UserRepository;
import com.example.bloghub.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ArticleServiceのユニットテスト
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ArticleService ユニットテスト")
class ArticleServiceTest {
    
    @Mock
    private ArticleRepository articleRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private TagRepository tagRepository;
    
    @Mock
    private SecurityContext securityContext;
    
    @Mock
    private Authentication authentication;
    
    @InjectMocks
    private ArticleService articleService;
    
    private User testUser;
    private UserPrincipal userPrincipal;
    private Article testArticle;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .password("password")
            .role(Role.USER)
            .build();
        
        userPrincipal = UserPrincipal.create(testUser);
        
        testArticle = Article.builder()
            .id(1L)
            .title("Test Article")
            .content("Test Content")
            .author(testUser)
            .viewCount(0)
            .tags(new HashSet<>())
            .build();
        
        // SecurityContextのモック設定
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
    }
    
    @Test
    @DisplayName("記事作成 - 成功")
    void createArticle_Success() {
        // Given
        ArticleCreateRequest request = new ArticleCreateRequest();
        request.setTitle("New Article");
        request.setContent("New Content");
        request.setTags(Set.of("Java", "Spring Boot"));
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(articleRepository.save(any(Article.class))).thenReturn(testArticle);
        
        // When
        ArticleResponse response = articleService.createArticle(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Test Article");
        assertThat(response.getAuthor().getUsername()).isEqualTo("testuser");
        
        verify(articleRepository, times(1)).save(any(Article.class));
    }
    
    @Test
    @DisplayName("記事取得 - 成功")
    void getArticleById_Success() {
        // Given
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));
        when(articleRepository.save(any(Article.class))).thenReturn(testArticle);
        
        // When
        ArticleResponse response = articleService.getArticleById(1L);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getViewCount()).isEqualTo(1); // インクリメントされる
    }
    
    @Test
    @DisplayName("記事取得 - 存在しない場合は例外")
    void getArticleById_NotFound() {
        // Given
        when(articleRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> articleService.getArticleById(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Article not found");
    }
    
    @Test
    @DisplayName("記事削除 - 所有者のみ可能")
    void deleteArticle_OwnerOnly() {
        // Given
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));
        
        // When
        articleService.deleteArticle(1L);
        
        // Then
        verify(articleRepository, times(1)).delete(testArticle);
    }
    
    @Test
    @DisplayName("記事削除 - 所有者以外は失敗")
    void deleteArticle_NotOwner() {
        // Given
        User anotherUser = User.builder()
            .id(2L)
            .username("another")
            .email("another@example.com")
            .role(Role.USER)
            .build();
        
        Article anotherArticle = Article.builder()
            .id(2L)
            .title("Another Article")
            .content("Content")
            .author(anotherUser)
            .build();
        
        when(articleRepository.findById(2L)).thenReturn(Optional.of(anotherArticle));
        
        // When & Then
        assertThatThrownBy(() -> articleService.deleteArticle(2L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("not the owner");
    }
}
```

### 1-2. AuthServiceのテスト

`src/test/java/com/example/bloghub/services/AuthServiceTest.java`を作成：

```java
package com.example.bloghub.services;

import com.example.bloghub.dto.request.LoginRequest;
import com.example.bloghub.dto.request.SignupRequest;
import com.example.bloghub.dto.response.JwtResponse;
import com.example.bloghub.dto.response.UserResponse;
import com.example.bloghub.entities.Role;
import com.example.bloghub.entities.User;
import com.example.bloghub.repositories.UserRepository;
import com.example.bloghub.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuthServiceのユニットテスト
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService ユニットテスト")
class AuthServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private AuthenticationManager authenticationManager;
    
    @Mock
    private JwtTokenProvider tokenProvider;
    
    @InjectMocks
    private AuthService authService;
    
    @Test
    @DisplayName("ユーザー登録 - 成功")
    void signup_Success() {
        // Given
        SignupRequest request = new SignupRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("password123");
        
        User savedUser = User.builder()
            .id(1L)
            .username("newuser")
            .email("new@example.com")
            .password("encodedPassword")
            .role(Role.USER)
            .build();
        
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        // When
        UserResponse response = authService.signup(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("newuser");
        assertThat(response.getEmail()).isEqualTo("new@example.com");
        assertThat(response.getRole()).isEqualTo(Role.USER);
        
        verify(userRepository, times(1)).save(any(User.class));
    }
    
    @Test
    @DisplayName("ユーザー登録 - メール重複エラー")
    void signup_EmailAlreadyExists() {
        // Given
        SignupRequest request = new SignupRequest();
        request.setUsername("newuser");
        request.setEmail("existing@example.com");
        request.setPassword("password123");
        
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> authService.signup(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Email already in use");
        
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    @DisplayName("ログイン - 成功")
    void login_Success() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn("jwt-token");
        
        // When
        JwtResponse response = authService.login(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getType()).isEqualTo("Bearer");
    }
}
```

---

## 🚀 ステップ2: 統合テストの実装

### 2-1. ArticleControllerの統合テスト

`src/test/java/com/example/bloghub/controllers/ArticleControllerIntegrationTest.java`を作成：

```java
package com.example.bloghub.controllers;

import com.example.bloghub.dto.request.ArticleCreateRequest;
import com.example.bloghub.dto.request.LoginRequest;
import com.example.bloghub.dto.request.SignupRequest;
import com.example.bloghub.dto.response.JwtResponse;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ArticleControllerの統合テスト
 */
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
    
    private String jwtToken;
    
    @BeforeEach
    void setUp() throws Exception {
        // テストユーザー登録
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("testuser");
        signupRequest.setEmail("test@example.com");
        signupRequest.setPassword("password123");
        
        mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(signupRequest)))
            .andExpect(status().isCreated());
        
        // ログインしてJWTトークン取得
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");
        
        MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();
        
        String responseBody = result.getResponse().getContentAsString();
        JwtResponse jwtResponse = objectMapper.readValue(responseBody, JwtResponse.class);
        jwtToken = jwtResponse.getToken();
    }
    
    @Test
    @DisplayName("記事投稿 - 成功")
    void createArticle_Success() throws Exception {
        // Given
        ArticleCreateRequest request = new ArticleCreateRequest();
        request.setTitle("Integration Test Article");
        request.setContent("This is a test article");
        request.setTags(Set.of("Test", "Integration"));
        
        // When & Then
        mockMvc.perform(post("/api/articles")
            .header("Authorization", "Bearer " + jwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Integration Test Article"))
            .andExpect(jsonPath("$.content").value("This is a test article"))
            .andExpect(jsonPath("$.author.username").value("testuser"))
            .andExpect(jsonPath("$.tags").isArray())
            .andExpect(jsonPath("$.viewCount").value(0));
    }
    
    @Test
    @DisplayName("記事投稿 - 認証なしは失敗")
    void createArticle_Unauthorized() throws Exception {
        // Given
        ArticleCreateRequest request = new ArticleCreateRequest();
        request.setTitle("Test Article");
        request.setContent("Content");
        
        // When & Then
        mockMvc.perform(post("/api/articles")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("記事一覧取得 - 成功")
    void getAllArticles_Success() throws Exception {
        // 記事を作成
        ArticleCreateRequest request = new ArticleCreateRequest();
        request.setTitle("Test Article");
        request.setContent("Content");
        
        mockMvc.perform(post("/api/articles")
            .header("Authorization", "Bearer " + jwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));
        
        // When & Then
        mockMvc.perform(get("/api/articles?page=0&size=10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.pageNumber").value(0))
            .andExpect(jsonPath("$.pageSize").value(10))
            .andExpect(jsonPath("$.totalElements").exists());
    }
    
    @Test
    @DisplayName("記事詳細取得 - 閲覧数がインクリメントされる")
    void getArticleById_ViewCountIncremented() throws Exception {
        // 記事作成
        ArticleCreateRequest request = new ArticleCreateRequest();
        request.setTitle("View Count Test");
        request.setContent("Content");
        
        MvcResult createResult = mockMvc.perform(post("/api/articles")
            .header("Authorization", "Bearer " + jwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andReturn();
        
        String createResponseBody = createResult.getResponse().getContentAsString();
        Long articleId = objectMapper.readTree(createResponseBody).get("id").asLong();
        
        // 1回目の取得
        mockMvc.perform(get("/api/articles/" + articleId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.viewCount").value(1));
        
        // 2回目の取得
        mockMvc.perform(get("/api/articles/" + articleId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.viewCount").value(2));
    }
}
```

---

## 🚀 ステップ3: テストカバレッジの測定

### 3-1. JaCoCoの設定

`pom.xml`に以下を追加：

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
                    <id>prepare-agent</id>
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
                    <id>jacoco-check</id>
                    <goals>
                        <goal>check</goal>
                    </goals>
                    <configuration>
                        <rules>
                            <rule>
                                <element>PACKAGE</element>
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
    </plugins>
</build>
```

### 3-2. カバレッジレポート生成

```bash
./mvnw clean test jacoco:report
```

レポートは `target/site/jacoco/index.html` に生成されます。

---

## 🚀 ステップ4: 本番環境設定

### 4-1. application-prod.ymlの作成

`src/main/resources/application-prod.yml`を作成：

```yaml
spring:
  application:
    name: BlogHub
  
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: false
        dialect: org.hibernate.dialect.MySQLDialect
  
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=600s
  
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 10MB

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000

file:
  upload-dir: ${FILE_UPLOAD_DIR:/app/uploads}

logging:
  level:
    root: INFO
    com.example.bloghub: INFO
  file:
    name: /var/log/bloghub/application.log
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 4-2. Dockerfileの作成

プロジェクトルートに`Dockerfile`を作成：

```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/bloghub-0.0.1-SNAPSHOT.jar app.jar

RUN mkdir -p /app/uploads && \
    addgroup -g 1000 spring && \
    adduser -u 1000 -G spring -s /bin/sh -D spring && \
    chown -R spring:spring /app

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=prod"]
```

### 4-3. docker-compose-prod.ymlの作成

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: bloghub-mysql-prod
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: bloghub
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
    volumes:
      - mysql_data:/var/lib/mysql
    networks:
      - bloghub-network
    restart: unless-stopped
  
  app:
    build: .
    container_name: bloghub-app
    environment:
      DATABASE_URL: jdbc:mysql://mysql:3306/bloghub?useSSL=false&serverTimezone=Asia/Tokyo
      DATABASE_USERNAME: ${MYSQL_USER}
      DATABASE_PASSWORD: ${MYSQL_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      FILE_UPLOAD_DIR: /app/uploads
    ports:
      - "8080:8080"
    depends_on:
      - mysql
    networks:
      - bloghub-network
    volumes:
      - app_uploads:/app/uploads
      - app_logs:/var/log/bloghub
    restart: unless-stopped

networks:
  bloghub-network:
    driver: bridge

volumes:
  mysql_data:
  app_uploads:
  app_logs:
```

### 4-4. .envファイルの作成

```env
MYSQL_ROOT_PASSWORD=your_root_password
MYSQL_USER=bloghub_user
MYSQL_PASSWORD=your_password
JWT_SECRET=YourVeryLongAndSecureSecretKeyForProductionUseAtLeast64Characters
```

---

## 🚀 ステップ5: デプロイメント

### 5-1. アプリケーションのビルド

```bash
./mvnw clean package -DskipTests
```

### 5-2. Dockerイメージのビルド

```bash
docker build -t bloghub:latest .
```

### 5-3. 本番環境での起動

```bash
docker-compose -f docker-compose-prod.yml --env-file .env up -d
```

### 5-4. ヘルスチェック

```bash
curl http://localhost:8080/actuator/health
```

---

## ✅ 動作確認

### 1. テスト実行

```bash
# 全テスト実行
./mvnw test

# カバレッジレポート生成
./mvnw clean test jacoco:report

# カバレッジ確認
open target/site/jacoco/index.html
```

### 2. 統合テスト実行

```bash
./mvnw verify
```

### 3. 本番ビルド確認

```bash
./mvnw clean package -Pprod
java -jar target/bloghub-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: CI/CDパイプライン

**目標**: GitHub Actionsで自動テスト・デプロイを設定

**ヒント**:
``yaml
# .github/workflows/ci.yml
name: CI

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 21
        uses: actions/setup-java@v2
        with:
          java-version: '21'
      - name: Run tests
        run: ./mvnw test
```

### チャレンジ 2: Kubernetes デプロイ

**目標**: Kubernetesマニフェストを作成してデプロイ

### チャレンジ 3: モニタリング

**目標**: Prometheus + Grafanaで監視ダッシュボードを構築

---

## 🐛 トラブルシューティング

### エラー: テストがFailする

**原因**: データベース接続設定が間違っている

**解決策**: `application-test.yml`でH2インメモリDBを使用

### エラー: カバレッジが70%未満

**原因**: テストが不足している

**解決策**: 重要なビジネスロジックのテストを追加

### エラー: Docker起動時にポートが使用中

**原因**: ポート8080が既に使用されている

**解決策**:
```bash
lsof -ti:8080 | xargs -r kill -9
```

---

## 📚 このステップで学んだこと

- ✅ ユニットテストでビジネスロジックを検証した
- ✅ 統合テストでAPIエンドポイントをテストした
- ✅ JaCoCoでテストカバレッジを測定した
- ✅ 本番環境用の設定を準備した
- ✅ Dockerでアプリケーションをコンテナ化した
- ✅ デプロイメント戦略を理解した
- ✅ 品質保証の重要性を学んだ

---

## 🎉 Phase 8 総合演習 完了！

おめでとうございます！Phase 8の総合演習を完了しました！

### 🏆 達成したこと

- ✅ **認証・認可**: Spring Security + JWT認証
- ✅ **CRUD機能**: 記事・コメントの完全なCRUD
- ✅ **ファイル管理**: 画像アップロード・ダウンロード
- ✅ **検索機能**: MyBatisで複雑な検索クエリ
- ✅ **パフォーマンス**: キャッシュとページネーション
- ✅ **テスト**: ユニットテスト・統合テスト・カバレッジ
- ✅ **デプロイ**: Docker化と本番環境設定

### 📖 次のステップ

カリキュラムを修了したあなたは、以下のスキルを習得しています：

1. **Spring Boot**: アプリケーション開発の基礎から応用まで
2. **データアクセス**: JPA/MyBatisの使い分け
3. **セキュリティ**: JWT認証、認可制御
4. **Web開発**: REST API、Thymeleaf
5. **テスト**: ユニットテスト、統合テスト
6. **デプロイ**: Docker、本番環境設定

### 🚀 さらなる学習のために

- **Spring Cloud**: マイクロサービスアーキテクチャ
- **Spring Batch**: バッチ処理
- **Kotlin**: KotlinでSpring Boot開発
- **GraphQL**: GraphQL APIの実装
- **Reactive Programming**: Spring WebFlux

---

**Spring Boot学習カリキュラム、お疲れ様でした！** 🎓

これからもSpring Bootで素晴らしいアプリケーションを作り続けてください！
