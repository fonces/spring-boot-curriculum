# Step 34: プロジェクト初期設定とユーザー認証基盤

## 🎯 このステップの目標

Phase 8では、これまで学んだ全ての技術を統合して**ミニブログアプリケーション**を開発します。
このステップでは、プロジェクトの初期設定とユーザー認証基盤（JWT認証）を実装します。

- MyBatisを使ったユーザー管理機能の実装
- JWTトークン認証の実装
- パスワードのハッシュ化（BCrypt）
- ユーザー登録・ログイン機能の完成

**所要時間**: 約1時間30分

## 📋 機能要件
- 新規ユーザー登録
- ログイン（JWT トークン発行）
- パスワードのハッシュ化（BCrypt）
- ログイン後のユーザー情報取得

## 🗂️ データベース設計

### usersテーブル
```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    bio TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## 💡 実装のヒント

### 1. プロジェクト構成
レイヤー化アーキテクチャを意識して、以下の構成で実装してください。

```
src/main/java/com/example/blog/
├── controller/
│   └── AuthController.java
├── service/
│   └── UserService.java
├── repository/
│   └── UserMapper.java (MyBatis)
├── entity/
│   └── User.java
├── dto/
│   ├── UserRegistrationRequest.java
│   ├── LoginRequest.java
│   └── AuthResponse.java
├── security/
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   └── SecurityConfig.java
└── exception/
    ├── UserAlreadyExistsException.java
    └── InvalidCredentialsException.java
```

### 2. 必要な依存関係（pom.xml）
以下の依存関係を追加してください：
```xml
<!-- 例: Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- 例: JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>

<!-- その他必要な依存関係を考えて追加してください -->
```

### 3. MyBatisでのユーザー管理
`UserMapper.xml`でSQLを定義し、以下の操作を実装してください：
- ユーザー登録（INSERT）
- ユーザー名でユーザー検索（SELECT）
- メールアドレスでユーザー検索（SELECT）
- ユーザーIDでユーザー取得（SELECT）

**例**:
```xml
<!-- UserMapper.xml の例 -->
<mapper namespace="com.example.blog.repository.UserMapper">
    <insert id="insertUser" parameterType="User" useGeneratedKeys="true" keyProperty="id">
        <!-- SQLを考えて実装してください -->
    </insert>
    
    <select id="findByUsername" resultType="User">
        <!-- SQLを考えて実装してください -->
    </select>
</mapper>
```

### 4. JWT トークンの生成と検証
`JwtTokenProvider`クラスを作成し、以下のメソッドを実装してください：
- `generateToken(String username)`: JWTトークンを生成
- `validateToken(String token)`: トークンの有効性を検証
- `getUsernameFromToken(String token)`: トークンからユーザー名を取得

**考えるポイント**:
- トークンの有効期限は何時間にするか？
- シークレットキーはどこに保管するか？（application.yml）

### 5. パスワードのハッシュ化
Spring SecurityのBCryptPasswordEncoderを使用してください。

**例**:
```java
@Service
public class UserService {
    private final PasswordEncoder passwordEncoder;
    
    // コンストラクタインジェクションでPasswordEncoderを注入
    
    public void registerUser(UserRegistrationRequest request) {
        // パスワードをハッシュ化してからデータベースに保存
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        // ...
    }
}
```

### 6. REST APIエンドポイント
以下のエンドポイントを実装してください：

#### ユーザー登録
```
POST /api/auth/register
Content-Type: application/json

{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "displayName": "John Doe"
}

Response (201 Created):
{
  "message": "User registered successfully",
  "username": "johndoe"
}
```

#### ログイン
```
POST /api/auth/login
Content-Type: application/json

{
  "username": "johndoe",
  "password": "SecurePass123!"
}

Response (200 OK):
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "johndoe",
  "displayName": "John Doe"
}
```

#### 現在のユーザー情報取得（認証必要）
```
GET /api/auth/me
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

Response (200 OK):
{
  "id": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "displayName": "John Doe",
  "bio": null,
  "createdAt": "2025-10-29T10:00:00"
}
```

### 7. バリデーション
リクエストDTOに適切なバリデーションを追加してください：
- ユーザー名: 3〜50文字、英数字とアンダースコアのみ
- メールアドレス: 有効な形式
- パスワード: 8文字以上、英数字含む

**例**:
```java
public class UserRegistrationRequest {
    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$")
    private String username;
    
    // 他のフィールドにも適切なバリデーションを追加
}
```

### 8. 例外ハンドリング
`@ControllerAdvice`を使用して、以下のエラーを適切に処理してください：
- ユーザー名/メールアドレスの重複
- 認証情報の不一致
- バリデーションエラー

## ✅ 動作確認

### 1. ユーザー登録
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test1234!",
    "displayName": "Test User"
  }'
```

### 2. ログイン
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test1234!"
  }'
```

### 3. ユーザー情報取得（トークンを使用）
```bash
# 上記ログインで取得したトークンを使用
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE"
```

## 🐛 トラブルシューティング

### エラー: "There is no PasswordEncoder mapped for the id 'null'"

**原因**:
- `SecurityConfig`で`PasswordEncoder`の`@Bean`を定義していない
- パスワードのエンコード形式が不正

**解決策**:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();  // BCryptを使用
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}

// Serviceでの使用
@Service
@RequiredArgsConstructor
public class UserService {
    private final PasswordEncoder passwordEncoder;
    
    public void register(UserRegistrationRequest request) {
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        // DBに保存
    }
}
```

### エラー: "Invalid bound statement (not found): com.example.blog.repository.UserMapper.insertUser"

**原因**:
- MyBatis MapperファイルのnamespaceとJavaインターフェースが一致していない
- Mapper XMLの配置場所が間違っている
- `application.yml`でMapper XMLのパスを指定していない

**解決策**:

```yaml
# application.yml
mybatis:
  mapper-locations: classpath:mapper/**/*.xml  # Mapper XMLの場所を明示
  type-aliases-package: com.example.blog.entity
  configuration:
    map-underscore-to-camel-case: true
```

```xml
<!-- UserMapper.xml のnamespaceを確認 -->
<mapper namespace="com.example.blog.repository.UserMapper">
    <insert id="insertUser" parameterType="com.example.blog.entity.User" 
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO users (username, email, password, display_name)
        VALUES (#{username}, #{email}, #{password}, #{displayName})
    </insert>
</mapper>
```

```java
// Javaインターフェース
@Mapper
public interface UserMapper {
    void insertUser(User user);  // メソッド名が一致していること
}
```

### エラー: "JWT signature does not match locally computed signature"

**原因**:
- JWTのシークレットキーが一致していない
- トークン生成時と検証時で異なる鍵を使用している
- シークレットキーが短すぎる（256bit未満）

**解決策**:

```yaml
# application.yml
jwt:
  secret: your-256-bit-secret-key-must-be-long-enough-for-hs256-algorithm  # 最低32文字以上
  expiration: 86400000  # 24時間（ミリ秒）
```

```java
@Component
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {
    private String secret;
    private Long expiration;
}

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    private final JwtProperties jwtProperties;
    
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpiration());
        
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)))
            .compact();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

### エラー: "Data too long for column 'password' at row 1"

**原因**:
- パスワードカラムのVARCHARサイズが小さすぎる
- BCryptのハッシュは60文字程度必要

**解決策**:

```sql
-- パスワードカラムを255文字に拡張
ALTER TABLE users MODIFY password VARCHAR(255) NOT NULL;
```

```sql
-- または最初から適切なサイズで作成
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,  -- BCrypt用に255文字確保
    display_name VARCHAR(100),
    bio TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### エラー: "Duplicate entry 'testuser' for key 'users.username'"

**原因**:
- 同じユーザー名またはメールアドレスで登録しようとしている
- ユニーク制約違反を適切にハンドリングしていない

**解決策**:

```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    
    public void register(UserRegistrationRequest request) {
        // 事前チェック
        if (userMapper.findByUsername(request.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException("ユーザー名は既に使用されています");
        }
        if (userMapper.findByEmail(request.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("メールアドレスは既に使用されています");
        }
        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName());
        
        userMapper.insertUser(user);
    }
}

// 例外クラス
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}

// エラーハンドリング
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException e) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            e.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
}
```

### エラー: "Cannot resolve method 'permitAll()' in 'AuthorizeHttpRequestsConfigurer'"

**原因**:
- Spring Security 6.x系の新しいメソッドチェーン構文を使用していない
- 古いバージョンの書き方が残っている

**解決策**:

```java
// ❌ Spring Security 5.x系の古い書き方
http
    .authorizeRequests()
    .antMatchers("/api/auth/**").permitAll()
    .anyRequest().authenticated();

// ✅ Spring Security 6.x系の新しい書き方
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll()  // antMatchers → requestMatchers
            .anyRequest().authenticated()
        )
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );
    return http.build();
}
```

## 🎓 学習ポイント

1. **MyBatis**: SQLを直接制御してユーザー情報を管理
2. **Spring Security**: 認証・認可の基盤を構築
3. **JWT**: ステートレスな認証方式の実装
4. **バリデーション**: ユーザー入力の検証
5. **例外ハンドリング**: 統一されたエラーレスポンス
6. **パスワードセキュリティ**: BCryptによるハッシュ化

## 📝 追加課題（オプション）

1. メールアドレス確認機能（登録時にメール送信）
2. パスワードリセット機能
3. ユーザープロフィール更新API
4. アカウント削除機能
5. リフレッシュトークンの実装

## ➡️ 次のステップ

[Step 35: 記事投稿機能と認可制御](STEP_35.md)へ進みましょう！

記事（Post）の投稿・編集・削除機能を実装し、認可制御（自分の記事のみ編集可能）を追加します。
