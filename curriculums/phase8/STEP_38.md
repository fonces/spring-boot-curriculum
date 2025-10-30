# Step 38: テスト実装とアプリケーションの完成

## 🎯 このステップの目標

これまで実装した機能に対するテストコードを書き、品質を確保します。
また、最終的な調整とデプロイ準備を行い、ミニブログアプリケーションを完成させます。

- ユニットテストと統合テストの実装
- テストカバレッジ80%以上の達成
- Testcontainersでのデータベーステスト
- Docker化とデプロイ準備

**所要時間**: 約1時間30分

## 📋 機能要件
- ユニットテスト（Service層）
- 統合テスト（Controller層、Repository層）
- テストカバレッジ 80%以上
- エラーハンドリングの完全性確認
- パフォーマンス最適化
- デプロイ準備（Docker化）

## 💡 実装のヒント

### 1. プロジェクト構成
以下のテストクラスを追加してください：

```
src/test/java/com/example/blog/
├── service/
│   ├── UserServiceTest.java
│   ├── PostServiceTest.java
│   ├── CommentServiceTest.java
│   └── TagServiceTest.java
├── controller/
│   ├── AuthControllerTest.java
│   ├── PostControllerTest.java
│   ├── CommentControllerTest.java
│   └── TagControllerTest.java
├── repository/
│   ├── UserMapperTest.java
│   ├── PostMapperTest.java
│   └── CommentMapperTest.java
└── integration/
    └── BlogApplicationIntegrationTest.java
```

### 2. テスト用の依存関係（pom.xml）
```xml
<dependencies>
    <!-- JUnit 5 (Spring Boot Starter Test に含まれる) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Spring Security Test -->
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Testcontainers (データベーステスト用) -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
        <version>1.19.0</version>
        <scope>test</scope>
    </dependency>
    
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>mysql</artifactId>
        <version>1.19.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 3. Service層のユニットテスト
Mockitoを使ってRepositoryをモック化し、ビジネスロジックをテストしてください。

**例**: UserServiceTest.java
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserMapper userMapper;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    @DisplayName("ユーザー登録が成功する")
    void testRegisterUser_Success() {
        // Given
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        
        when(userMapper.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userMapper.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        
        // When
        userService.registerUser(request);
        
        // Then
        verify(userMapper).insert(any(User.class));
        verify(passwordEncoder).encode("password123");
    }
    
    @Test
    @DisplayName("ユーザー名が重複している場合は例外が発生する")
    void testRegisterUser_DuplicateUsername() {
        // Given
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setUsername("existinguser");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        
        User existingUser = new User();
        existingUser.setUsername("existinguser");
        when(userMapper.findByUsername("existinguser")).thenReturn(Optional.of(existingUser));
        
        // When & Then
        assertThrows(UserAlreadyExistsException.class, () -> {
            userService.registerUser(request);
        });
        
        verify(userMapper, never()).insert(any(User.class));
    }
    
    // 他のテストケースを実装してください
    // - メールアドレス重複のテスト
    // - ログイン成功のテスト
    // - ログイン失敗（パスワード不一致）のテスト
}
```

**例**: PostServiceTest.java
```java
@ExtendWith(MockitoExtension.class)
class PostServiceTest {
    
    @Mock
    private PostMapper postMapper;
    
    @Mock
    private UserMapper userMapper;
    
    @Mock
    private TagService tagService;
    
    @InjectMocks
    private PostService postService;
    
    @Test
    @DisplayName("記事の投稿が成功する")
    void testCreatePost_Success() {
        // Given
        PostCreateRequest request = new PostCreateRequest();
        request.setTitle("Test Post");
        request.setContent("Test Content");
        request.setStatus(PostStatus.PUBLISHED);
        request.setTags(Arrays.asList("Java", "Spring Boot"));
        
        User author = new User();
        author.setId(1L);
        author.setUsername("testuser");
        
        when(userMapper.findByUsername("testuser")).thenReturn(Optional.of(author));
        
        // When
        PostResponse response = postService.createPost(request, "testuser");
        
        // Then
        assertNotNull(response);
        verify(postMapper).insert(any(Post.class));
    }
    
    @Test
    @DisplayName("自分の記事のみ更新できる")
    void testUpdatePost_Success() {
        // Given
        Long postId = 1L;
        String username = "author";
        
        User author = new User();
        author.setUsername(username);
        
        Post existingPost = new Post();
        existingPost.setId(postId);
        existingPost.setAuthor(author);
        existingPost.setTitle("Old Title");
        
        when(postMapper.findById(postId)).thenReturn(Optional.of(existingPost));
        
        PostUpdateRequest request = new PostUpdateRequest();
        request.setTitle("New Title");
        request.setContent("New Content");
        
        // When
        postService.updatePost(postId, request, username);
        
        // Then
        verify(postMapper).update(any(Post.class));
    }
    
    @Test
    @DisplayName("他人の記事は更新できない")
    void testUpdatePost_Unauthorized() {
        // Given
        Long postId = 1L;
        String username = "otheruser";
        
        User author = new User();
        author.setUsername("originalauthor");
        
        Post existingPost = new Post();
        existingPost.setId(postId);
        existingPost.setAuthor(author);
        
        when(postMapper.findById(postId)).thenReturn(Optional.of(existingPost));
        
        PostUpdateRequest request = new PostUpdateRequest();
        request.setTitle("Hacked Title");
        
        // When & Then
        assertThrows(UnauthorizedException.class, () -> {
            postService.updatePost(postId, request, username);
        });
        
        verify(postMapper, never()).update(any(Post.class));
    }
    
    // 他のテストケースを実装してください
}
```

### 4. Controller層の統合テスト
MockMvcを使ってHTTPリクエストをテストしてください。

**例**: PostControllerTest.java
```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PostControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private UserMapper userMapper;
    
    private String authToken;
    private User testUser;
    
    @BeforeEach
    void setUp() {
        // テストユーザーを作成
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedPassword");
        userMapper.insert(testUser);
        
        // JWTトークンを生成
        authToken = jwtTokenProvider.generateToken(testUser.getUsername());
    }
    
    @Test
    @DisplayName("認証済みユーザーは記事を投稿できる")
    void testCreatePost_Authenticated() throws Exception {
        // Given
        PostCreateRequest request = new PostCreateRequest();
        request.setTitle("Test Post");
        request.setContent("Test Content");
        request.setStatus(PostStatus.PUBLISHED);
        
        // When & Then
        mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Test Post"))
            .andExpect(jsonPath("$.author.username").value("testuser"));
    }
    
    @Test
    @DisplayName("認証なしでは記事を投稿できない")
    void testCreatePost_Unauthenticated() throws Exception {
        // Given
        PostCreateRequest request = new PostCreateRequest();
        request.setTitle("Test Post");
        request.setContent("Test Content");
        
        // When & Then
        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("記事一覧を取得できる")
    void testGetPosts() throws Exception {
        // Given
        // テストデータを作成
        
        // When & Then
        mockMvc.perform(get("/api/posts")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").exists());
    }
    
    @Test
    @DisplayName("バリデーションエラー時は400エラーを返す")
    void testCreatePost_ValidationError() throws Exception {
        // Given
        PostCreateRequest request = new PostCreateRequest();
        request.setTitle(""); // 空のタイトル（バリデーションエラー）
        request.setContent("Test Content");
        
        // When & Then
        mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
    
    // 他のテストケースを実装してください
}
```

### 5. Repository層のテスト（MyBatis）
Testcontainersを使って実際のデータベースでテストしてください。

**例**: PostMapperTest.java
```java
@SpringBootTest
@Testcontainers
@Transactional
class PostMapperTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
    
    @Autowired
    private PostMapper postMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Test
    @DisplayName("記事をINSERTし、IDが自動採番される")
    void testInsertPost() {
        // Given
        User author = new User();
        author.setUsername("testauthor");
        author.setEmail("author@example.com");
        author.setPassword("password");
        userMapper.insert(author);
        
        Post post = new Post();
        post.setTitle("Test Post");
        post.setContent("Test Content");
        post.setStatus(PostStatus.PUBLISHED);
        post.setAuthorId(author.getId());
        
        // When
        postMapper.insert(post);
        
        // Then
        assertNotNull(post.getId());
        assertTrue(post.getId() > 0);
    }
    
    @Test
    @DisplayName("公開済み記事のみを取得できる")
    void testFindAllPublished() {
        // Given
        // 公開記事と下書き記事を作成
        
        // When
        List<Post> posts = postMapper.findAllPublished(10, 0);
        
        // Then
        assertFalse(posts.isEmpty());
        posts.forEach(post -> {
            assertEquals(PostStatus.PUBLISHED, post.getStatus());
        });
    }
    
    // 他のテストケースを実装してください
}
```

### 6. 統合テスト（E2Eシナリオ）
実際のユーザーシナリオをテストしてください。

**例**: BlogApplicationIntegrationTest.java
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class BlogApplicationIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @DisplayName("ユーザー登録→ログイン→記事投稿→コメント投稿の一連の流れ")
    void testCompleteUserJourney() {
        // 1. ユーザー登録
        UserRegistrationRequest registerRequest = new UserRegistrationRequest();
        registerRequest.setUsername("journeyuser");
        registerRequest.setEmail("journey@example.com");
        registerRequest.setPassword("Password123!");
        registerRequest.setDisplayName("Journey User");
        
        ResponseEntity<String> registerResponse = restTemplate.postForEntity(
            "/api/auth/register",
            registerRequest,
            String.class
        );
        
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());
        
        // 2. ログイン
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("journeyuser");
        loginRequest.setPassword("Password123!");
        
        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
            "/api/auth/login",
            loginRequest,
            AuthResponse.class
        );
        
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        String token = loginResponse.getBody().getToken();
        assertNotNull(token);
        
        // 3. 記事投稿
        PostCreateRequest postRequest = new PostCreateRequest();
        postRequest.setTitle("Integration Test Post");
        postRequest.setContent("This is an integration test");
        postRequest.setStatus(PostStatus.PUBLISHED);
        postRequest.setTags(Arrays.asList("Test", "Integration"));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<PostCreateRequest> postEntity = new HttpEntity<>(postRequest, headers);
        
        ResponseEntity<PostResponse> postResponse = restTemplate.exchange(
            "/api/posts",
            HttpMethod.POST,
            postEntity,
            PostResponse.class
        );
        
        assertEquals(HttpStatus.CREATED, postResponse.getStatusCode());
        Long postId = postResponse.getBody().getId();
        
        // 4. コメント投稿
        CommentRequest commentRequest = new CommentRequest();
        commentRequest.setContent("Great post!");
        
        HttpEntity<CommentRequest> commentEntity = new HttpEntity<>(commentRequest, headers);
        
        ResponseEntity<CommentResponse> commentResponse = restTemplate.exchange(
            "/api/posts/" + postId + "/comments",
            HttpMethod.POST,
            commentEntity,
            CommentResponse.class
        );
        
        assertEquals(HttpStatus.CREATED, commentResponse.getStatusCode());
        assertEquals("Great post!", commentResponse.getBody().getContent());
    }
}
```

### 7. テストカバレッジの確認
JaCoCoプラグインを追加してカバレッジを測定してください。

**pom.xml**:
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.10</version>
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
                                        <minimum>0.80</minimum>
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

### 8. Docker化
アプリケーションをDocker化してデプロイ準備を整えてください。

**Dockerfile**:
```dockerfile
# マルチステージビルド
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**docker-compose.yml**:
```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: blogdb
      MYSQL_USER: bloguser
      MYSQL_PASSWORD: blogpass
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
    networks:
      - blog-network

  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/blogdb
      SPRING_DATASOURCE_USERNAME: bloguser
      SPRING_DATASOURCE_PASSWORD: blogpass
      JWT_SECRET: your-secret-key-change-this-in-production
    depends_on:
      - mysql
    networks:
      - blog-network

volumes:
  mysql-data:

networks:
  blog-network:
```

## ✅ 動作確認

### 1. テストの実行
```bash
# 全テストを実行
mvn test

# カバレッジレポートを生成
mvn clean test jacoco:report

# レポートを確認
open target/site/jacoco/index.html
```

### 2. Dockerでの起動
```bash
# イメージをビルドして起動
docker-compose up --build

# アプリケーションにアクセス
curl http://localhost:8080/api/posts
```

### 3. カバレッジ目標達成の確認
```bash
# 80%以上のカバレッジを確認
mvn verify
```

## 🐛 トラブルシューティング

### エラー: "No tests found for given includes" (Maven)

**原因**:
- テストクラスの命名規則が間違っている
- Surefire/Failsafeプラグインの設定が不足

**解決策**:

```xml
<!-- pom.xml -->
<build>
    <plugins>
        <!-- ユニットテスト用 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.0.0-M9</version>
            <configuration>
                <includes>
                    <include>**/*Test.java</include>      <!-- XxxTest.java -->
                    <include>**/*Tests.java</include>     <!-- XxxTests.java -->
                </includes>
            </configuration>
        </plugin>
        
        <!-- 統合テスト用 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-failsafe-plugin</artifactId>
            <version>3.0.0-M9</version>
            <configuration>
                <includes>
                    <include>**/*IT.java</include>        <!-- XxxIT.java -->
                    <include>**/*IntegrationTest.java</include>
                </includes>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>integration-test</goal>
                        <goal>verify</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

```java
// テストクラスの命名規則を守る
// ✅ ユニットテスト
public class UserServiceTest { }
public class PostMapperTest { }

// ✅ 統合テスト
public class PostControllerIT { }
public class UserAuthenticationIntegrationTest { }
```

### エラー: "Failed to load ApplicationContext" (Spring Bootテスト起動失敗)

**原因**:
- テスト用の設定ファイルが不足
- データベース接続情報が間違っている
- Bean定義の問題

**解決策**:

```yaml
# src/test/resources/application-test.yml を作成
spring:
  datasource:
    url: jdbc:h2:mem:testdb  # H2インメモリDBを使用
    driver-class-name: org.h2.Driver
    username: sa
    password:
  
  jpa:
    hibernate:
      ddl-auto: create-drop  # テストごとにスキーマを再作成
    show-sql: true
    
mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
    
jwt:
  secret: test-secret-key-for-testing-purposes-only-minimum-256-bits
  expiration: 3600000
```

```java
// テストクラスでプロファイルを指定
@SpringBootTest
@ActiveProfiles("test")  // application-test.yml を読み込む
class UserServiceTest {
    // ...
}
```

### エラー: "Testcontainers: Could not find a valid Docker environment"

**原因**:
- Dockerが起動していない
- DockerのバージョンがTestcontainersに対応していない
- Dockerソケットへのアクセス権限がない

**解決策**:

```bash
# Dockerが起動しているか確認
docker ps

# Dockerが起動していない場合は起動
sudo systemctl start docker

# ユーザーをdockerグループに追加（Linux）
sudo usermod -aG docker $USER
newgrp docker

# 権限を再確認
docker ps
```

```java
// Testcontainersの設定を確認
@SpringBootTest
@Testcontainers
class PostMapperTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test")
        .withReuse(true);  // コンテナを再利用してテスト高速化
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
}
```

### エラー: "MockHttpServletRequest cannot be cast to HttpServletRequest"

**原因**:
- MockMvcのセットアップが間違っている
- `@WebMvcTest`と`@SpringBootTest`を併用している

**解決策**:

```java
// コントローラーだけをテストする場合は @WebMvcTest
@WebMvcTest(PostController.class)
class PostControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean  // サービス層はモック化
    private PostService postService;
    
    @Test
    void testGetPosts() throws Exception {
        // モックの振る舞いを定義
        when(postService.getPosts(0, 10))
            .thenReturn(new Page<>(List.of(), 0, 10, 0));
        
        mockMvc.perform(get("/api/posts"))
            .andExpect(status().isOk());
    }
}

// 統合テストの場合は @SpringBootTest + @AutoConfigureMockMvc
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    // サービス層は実際のBeanを使用
    @Test
    void testCreatePost() throws Exception {
        String token = getAuthToken();  // 実際にログインしてトークン取得
        
        mockMvc.perform(post("/api/posts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"Test\", \"content\": \"Test\"}"))
            .andExpect(status().isCreated());
    }
}
```

### エラー: "JaCoCo coverage check failed: Line coverage is 45%, but expected minimum is 80%"

**原因**:
- テストカバレッジが目標値に達していない
- テストケースが不足している

**解決策**:

```java
// カバレッジを上げるために重要な箇所をテスト

// ❌ カバレッジが低い例
@Test
void testCreatePost() {
    postService.createPost(request, "user");
    // アサーションがない、例外ケースがない
}

// ✅ カバレッジを上げる例
@Test
@DisplayName("正常な記事作成")
void testCreatePost_Success() {
    PostCreateRequest request = new PostCreateRequest();
    request.setTitle("Test Title");
    request.setContent("Test Content");
    
    assertDoesNotThrow(() -> {
        postService.createPost(request, "testuser");
    });
    
    verify(postMapper, times(1)).insertPost(any(Post.class));
}

@Test
@DisplayName("タイトルが空の場合にエラー")
void testCreatePost_EmptyTitle() {
    PostCreateRequest request = new PostCreateRequest();
    request.setTitle("");
    request.setContent("Test Content");
    
    assertThrows(ValidationException.class, () -> {
        postService.createPost(request, "testuser");
    });
}

@Test
@DisplayName("権限がない場合にエラー")
void testCreatePost_Unauthorized() {
    PostCreateRequest request = new PostCreateRequest();
    request.setTitle("Test");
    request.setContent("Test");
    
    when(userMapper.findByUsername("hacker")).thenReturn(Optional.empty());
    
    assertThrows(UnauthorizedException.class, () -> {
        postService.createPost(request, "hacker");
    });
}
```

```xml
<!-- カバレッジ目標を調整する場合（一時的に） -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.60</minimum>  <!-- 80% → 60% に一時的に緩和 -->
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>
```

### エラー: "Port 8080 is already in use" (Dockerコンテナ起動時)

**原因**:
- ローカルでアプリケーションが既に起動している
- 他のコンテナが8080ポートを使用している

**解決策**:

```bash
# 8080ポートを使用しているプロセスを確認
lsof -i :8080  # macOS/Linux
netstat -ano | findstr :8080  # Windows

# プロセスを停止
kill -9 <PID>

# または docker-compose.yml でポート番号を変更
services:
  app:
    ports:
      - "8081:8080"  # ホスト側のポートを8081に変更
```

```bash
# 既存のコンテナを停止してから再起動
docker-compose down
docker-compose up --build
```

### エラー: "Cannot connect to the Docker daemon at unix:///var/run/docker.sock"

**原因**:
- Dockerデーモンが起動していない
- ソケットファイルへのアクセス権限がない

**解決策**:

```bash
# Dockerデーモンを起動
sudo systemctl start docker

# 自動起動を有効化
sudo systemctl enable docker

# 現在のユーザーをdockerグループに追加
sudo usermod -aG docker $USER

# 再ログインまたは
newgrp docker

# 確認
docker ps
```

## 🎓 学習ポイント

1. **ユニットテスト**: Mockitoでの依存関係のモック化
2. **統合テスト**: MockMvcでのHTTPリクエストテスト
3. **データベーステスト**: Testcontainersでの実データベーステスト
4. **E2Eテスト**: 実際のユーザーシナリオのテスト
5. **テストカバレッジ**: JaCoCoでの品質測定
6. **Docker化**: コンテナ化とデプロイ準備

## 📝 追加課題（オプション）

1. パフォーマンステスト（JMeter等）
2. セキュリティテスト（OWASP ZAP等）
3. CI/CDパイプラインの構築（GitHub Actions等）
4. 本番環境へのデプロイ（AWS, Heroku等）
5. モニタリング設定（Prometheus, Grafana）
6. APIドキュメント生成（Swagger/OpenAPI）

## 🎉 完成！

おめでとうございます！これでミニブログアプリケーションが完成しました。

### これまでに学んだこと
- ✅ Spring Bootの基礎（REST API、DI/IoC）
- ✅ MyBatisでのSQL制御
- ✅ Thymeleafでのサーバーサイドレンダリング
- ✅ Spring Securityでの認証・認可
- ✅ JWTトークン認証
- ✅ ファイルアップロード
- ✅ トランザクション管理
- ✅ 例外ハンドリング
- ✅ バリデーション
- ✅ ページネーション
- ✅ 多対多リレーション
- ✅ テスト実装（ユニット、統合、E2E）
- ✅ Docker化

### 次のステップ
このアプリケーションをベースに、以下の機能を追加してさらにスキルアップしてください：

1. **ソーシャル機能**: フォロー/フォロワー、いいね
2. **通知機能**: WebSocketでのリアルタイム通知
3. **全文検索**: Elasticsearch連携
4. **キャッシュ**: Redisでのパフォーマンス最適化
5. **非同期処理**: メール送信、画像処理
6. **マイクロサービス化**: Spring Cloudでのサービス分割

Happy Coding! 🚀
