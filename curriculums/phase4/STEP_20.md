# Step 20: JWT認証

## 🎯 このステップの目標

- JWT（JSON Web Token）の仕組みを理解する
- JWTベースの認証システムを実装する
- ログイン・ログアウト機能を実装する
- JWTフィルターを作成してリクエストを検証する

**所要時間**: 約2時間30分

---

## 📋 事前準備

- Step 19のSpring Security基礎が理解できていること
- 認証と認可の違いを理解していること

**Step 19をまだ完了していない場合**: [Step 19: Spring Security基礎](STEP_19.md)を先に進めてください。

---

## 💡 JWTとは？

### JWT (JSON Web Token)

**構造**:
```
Header.Payload.Signature

例:
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.
eyJzdWIiOiJ1c2VyMTIzIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.
SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

**3つの部分**:

1. **Header（ヘッダー）**:
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

2. **Payload（ペイロード）**:
```json
{
  "sub": "user123",
  "name": "John Doe",
  "role": "USER",
  "iat": 1516239022,
  "exp": 1516242622
}
```

3. **Signature（署名）**:
```
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret
)
```

### JWTのメリット

| 比較項目 | セッション認証 | JWT認証 |
|----------|---------------|---------|
| **ステート** | ステートフル | ステートレス |
| **保存場所** | サーバー | クライアント |
| **スケーラビリティ** | 低い | 高い |
| **REST API** | 不向き | 最適 |

---

## 🚀 ステップ1: JWT依存関係の追加

### 1-1. pom.xmlの更新

**ファイルパス**: `pom.xml`

```xml
<dependencies>
    <!-- 既存の依存関係 -->
    
    <!-- JWT Library -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.3</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.3</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.3</version>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

### 1-2. application.ymlにJWT設定追加

**ファイルパス**: `src/main/resources/application.yml`

```yaml
# JWT設定
jwt:
  secret: mySecretKeyForJWT1234567890123456789012345678901234567890
  expiration: 86400000  # 24時間（ミリ秒）
```

**注意**: 本番環境では環境変数を使用
```yaml
jwt:
  secret: ${JWT_SECRET:defaultSecretKey}
  expiration: ${JWT_EXPIRATION:86400000}
```

---

## 🚀 ステップ2: JWTユーティリティクラスの作成

### 2-1. JwtUtil

**ファイルパス**: `src/main/java/com/example/hellospringboot/security/JwtUtil.java`

```java
package com.example.hellospringboot.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWTトークンの生成・検証を行うユーティリティクラス
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * 秘密鍵を取得
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * ユーザー名を取得
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 有効期限を取得
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * クレームを抽出
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * すべてのクレームを抽出
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * トークンが期限切れかチェック
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * トークンを生成
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", userDetails.getAuthorities());
        return createToken(claims, userDetails.getUsername());
    }

    /**
     * トークンを作成
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * トークンを検証
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
```

---

## 🚀 ステップ3: 認証用DTOの作成

### 3-1. LoginRequest

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/request/LoginRequest.java`

```java
package com.example.hellospringboot.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ログインリクエストDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "ユーザー名は必須です")
    private String username;

    @NotBlank(message = "パスワードは必須です")
    private String password;
}
```

### 3-2. AuthResponse

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/response/AuthResponse.java`

```java
package com.example.hellospringboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 認証レスポンスDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    /**
     * JWTトークン
     */
    private String token;

    /**
     * トークンタイプ（通常は"Bearer"）
     */
    private String type;

    /**
     * ユーザー名
     */
    private String username;

    /**
     * ロール
     */
    private String role;
}
```

---

## 🚀 ステップ4: JWTフィルターの作成

### 4-1. JwtAuthenticationFilter

**ファイルパス**: `src/main/java/com/example/hellospringboot/security/JwtAuthenticationFilter.java`

```java
package com.example.hellospringboot.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWTトークンを検証するフィルター
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Authorizationヘッダーを取得
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // トークンを抽出
            final String jwt = authHeader.substring(7);
            final String username = jwtUtil.extractUsername(jwt);

            // ユーザーが未認証の場合
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // トークンを検証
                if (jwtUtil.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                        );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // SecurityContextに認証情報を設定
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.debug("User {} authenticated successfully", username);
                }
            }
        } catch (Exception ex) {
            log.error("JWT authentication failed: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
```

---

## 🚀 ステップ5: AuthControllerの作成

### 5-1. AuthController

**ファイルパス**: `src/main/java/com/example/hellospringboot/controller/AuthController.java`

```java
package com.example.hellospringboot.controller;

import com.example.hellospringboot.dto.request.LoginRequest;
import com.example.hellospringboot.dto.response.AuthResponse;
import com.example.hellospringboot.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 認証コントローラー
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    /**
     * ログイン
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());

        try {
            // 認証
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
                )
            );

            // UserDetails取得
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // JWTトークン生成
            String token = jwtUtil.generateToken(userDetails);

            // ロール取得（最初の1つ）
            String role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(GrantedAuthority::getAuthority)
                    .orElse("ROLE_USER");

            log.info("Login successful for user: {}", request.getUsername());

            AuthResponse response = AuthResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .username(userDetails.getUsername())
                    .role(role)
                    .build();

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException ex) {
            log.warn("Login failed for user: {} - Bad credentials", request.getUsername());
            throw new BadCredentialsException("ユーザー名またはパスワードが正しくありません");
        }
    }
}
```

---

## 🚀 ステップ6: SecurityConfigの更新

### 6-1. JWT用のSecurityConfig

**ファイルパス**: `src/main/java/com/example/hellospringboot/config/SecurityConfig.java`

```java
package com.example.hellospringboot.config;

import com.example.hellospringboot.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Securityの設定クラス（JWT対応）
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF無効化
            .csrf(csrf -> csrf.disable())
            
            // エンドポイントのアクセス制御
            .authorizeHttpRequests(auth -> auth
                // 公開エンドポイント
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                
                // 管理者のみ
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // 認証が必要
                .anyRequest().authenticated()
            )
            
            // セッションをステートレスに設定
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // 認証プロバイダー
            .authenticationProvider(authenticationProvider())
            
            // JWTフィルターを追加
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // H2 Consoleのフレーム表示を許可
        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) 
            throws Exception {
        return config.getAuthenticationManager();
    }
}
```

---

## ✅ ステップ7: 動作確認

### 7-1. ログイン

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user",
    "password": "user123"
  }'
```

**期待されるレスポンス**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJhdXRob3JpdGllcyI6W3siYXV0aG9yaXR5IjoiUk9MRV9VU0VSIn1dLCJzdWIiOiJ1c2VyIiwiaWF0IjoxNjk4NDEyMzAwLCJleHAiOjE2OTg0OTg3MDB9.xxx",
  "type": "Bearer",
  "username": "user",
  "role": "ROLE_USER"
}
```

### 7-2. トークンを使ってAPIアクセス

```bash
# トークンを変数に保存
TOKEN="eyJhbGciOiJIUzI1NiJ9.xxx..."

# APIリクエスト
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/users
```

**期待される結果**: 200 OK

### 7-3. 無効なトークンでアクセス

```bash
curl -H "Authorization: Bearer invalid-token" \
  http://localhost:8080/api/users
```

**期待される結果**: 401 Unauthorized

### 7-4. トークンなしでアクセス

```bash
curl http://localhost:8080/api/users
```

**期待される結果**: 401 Unauthorized

---

## 🚀 ステップ8: データベースユーザー認証への移行

### 8-1. UserDetailsServiceの実装

**ファイルパス**: `src/main/java/com/example/hellospringboot/security/CustomUserDetailsService.java`

```java
package com.example.hellospringboot.security;

import com.example.hellospringboot.entity.User;
import com.example.hellospringboot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * データベースからユーザー情報を取得するUserDetailsService
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                    "ユーザーが見つかりません: " + email
                ));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())  // 暗号化されたパスワード
                .authorities(Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + user.getRole())
                ))
                .build();
    }
}
```

### 8-2. Userエンティティの更新

```java
@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    private Integer age;

    // パスワード追加
    @Column(nullable = false)
    private String password;

    // ロール追加（USER, ADMIN）
    @Column(nullable = false)
    private String role = "USER";
}
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: リフレッシュトークン

アクセストークンの有効期限が切れた際に、リフレッシュトークンで再発行できるようにしてください。

### チャレンジ 2: ログアウト機能

トークンをブラックリストに追加してログアウトを実装してください。

### チャレンジ 3: トークン情報取得API

`/api/auth/me`エンドポイントでトークンからユーザー情報を取得できるようにしてください。

---

## 🐛 トラブルシューティング

### "SignatureException: JWT signature does not match"

**原因**: 秘密鍵が異なる、またはトークンが改ざんされている

**解決策**: `application.yml`の`jwt.secret`を確認

### "ExpiredJwtException"

**原因**: トークンの有効期限が切れている

**解決策**: 再ログインしてトークンを再発行

### 認証後もSecurityContextがnull

**原因**: JwtAuthenticationFilterが実行されていない

**解決策**: SecurityConfigでフィルターが登録されているか確認
```java
.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
```

---

## 📚 このステップで学んだこと

- ✅ JWTの構造と仕組み
- ✅ JwtUtilによるトークン生成・検証
- ✅ JwtAuthenticationFilterの実装
- ✅ ログインAPIの実装
- ✅ ステートレス認証
- ✅ Bearerトークンの使用

---

## 💡 補足: JWTのベストプラクティス

### トークンの保存場所

| 保存場所 | メリット | デメリット |
|----------|---------|-----------|
| **LocalStorage** | 簡単 | XSS攻撃に脆弱 |
| **Cookie (HttpOnly)** | XSS対策 | CSRF対策が必要 |
| **SessionStorage** | タブ閉じると消える | XSS攻撃に脆弱 |

**推奨**: HttpOnly Cookieまたはメモリ内

### トークンの有効期限

```yaml
jwt:
  # アクセストークン: 15分〜1時間
  expiration: 900000  # 15分

  # リフレッシュトークン: 7日〜30日
  refresh-expiration: 604800000  # 7日
```

### セキュリティ強化

```java
// ✅ 良い例: 強力な秘密鍵（256bit以上）
jwt.secret=myVeryLongSecretKeyThatIsAtLeast256BitsLong12345678901234567890

// ❌ 悪い例: 短い秘密鍵
jwt.secret=secret
```

---

## 🔄 Gitへのコミット

```bash
git add .
git commit -m "Phase 4: STEP_20完了（JWT認証実装）"
git push origin main
```

---

## ➡️ 次のステップ

次は[Step 21: ユニットテスト](STEP_21.md)へ進みましょう！

JUnit 5とMockitoを使ったテストコードの書き方を学びます。

---

お疲れさまでした！ 🎉

JWT認証を習得しました！これで本格的なRESTful APIの
セキュリティが実装できるようになりました！
