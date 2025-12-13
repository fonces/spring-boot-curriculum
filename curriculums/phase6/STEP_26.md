# Step 26: JWTトークン認証

## 🎯 このステップの目標

- JWT（JSON Web Token）の仕組みを理解できる
- ログインエンドポイントでトークンを発行できる
- トークン検証フィルターを実装できる
- ステートレスな認証を実現できる

**所要時間**: 約60分

---

## 📋 事前準備

- [Step 25: Spring Securityの基礎](STEP_25.md)が完了していること
- JWTの概念を理解していること（推奨）

---

## 🔑 JWTとは

### Basic認証の問題点

**問題1**: 毎回認証情報を送信
```bash
# 毎回username:passwordを送る
curl -u admin:admin123 http://localhost:8080/api/users
curl -u admin:admin123 http://localhost:8080/api/users/1
curl -u admin:admin123 http://localhost:8080/api/users/2
```

**問題2**: サーバーに状態を保存
- セッション管理が必要
- スケールアウトしにくい

### JWTの仕組み

**ステップ1**: ログインでトークン取得
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# レスポンス
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**ステップ2**: トークンを使ってアクセス
```bash
curl -H "Authorization: Bearer eyJhbGci..." http://localhost:8080/api/users
```

### JWTのメリット

1. **ステートレス**: サーバーに状態を保存しない
2. **スケーラブル**: 複数サーバーで動作
3. **自己完結**: トークン内にユーザー情報を含む
4. **有効期限**: トークンの期限切れを設定可能

### JWTの構造

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsInJvbGVzIjpbIlJPTEVfQURNSU4iXX0.signature
│        Header (Base64)        │       Payload (Base64)        │ Signature │
```

**Header**: アルゴリズム情報
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

**Payload**: ユーザー情報
```json
{
  "sub": "admin",
  "roles": ["ROLE_ADMIN"],
  "exp": 1702468800
}
```

**Signature**: 改ざん防止
```
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret
)
```

---

## 🚀 ステップ1: JWT依存関係の追加

### 1-1. pom.xmlに追加

`pom.xml`の`<dependencies>`セクションに追加：

```xml
<!-- JWT (JSON Web Token) -->
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
```

### 1-2. 依存関係を反映

```bash
./mvnw clean compile
```

---

## 🚀 ステップ2: JWTユーティリティクラスの作成

### 2-1. JwtUtilsクラスを作成

`src/main/java/com/example/hellospringboot/security/JwtUtils.java`を作成：

```java
package com.example.hellospringboot.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JwtUtils {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private Long expiration;  // ミリ秒
    
    /**
     * 秘密鍵を取得
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * トークンからユーザー名を取得
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }
    
    /**
     * トークンから有効期限を取得
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }
    
    /**
     * トークンからクレーム（属性）を取得
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * トークンからすべてのクレームを取得
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
    
    /**
     * トークンの有効期限が切れているかチェック
     */
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }
    
    /**
     * トークンを生成
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        // ロール情報をクレームに追加
        claims.put("roles", userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()));
        
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
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
```

### 2-2. application.ymlに設定を追加

`src/main/resources/application.yml`に追加：

```yaml
jwt:
  secret: "my-very-secure-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm"
  expiration: 86400000  # 24時間（ミリ秒）
```

**重要**: 
- `secret`は**256ビット（32文字）以上**必要
- 本番環境では環境変数から読み込む

---

## 🚀 ステップ3: ログインエンドポイントの実装

### 3-1. LoginRequestとLoginResponseを作成

`src/main/java/com/example/hellospringboot/dto/LoginRequest.java`:

```java
package com.example.hellospringboot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Password is required")
    private String password;
}
```

`src/main/java/com/example/hellospringboot/dto/LoginResponse.java`:

```java
package com.example.hellospringboot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    
    private String token;
    private String type = "Bearer";
    private String username;
    private List<String> roles;
    
    public LoginResponse(String token, String username, List<String> roles) {
        this.token = token;
        this.username = username;
        this.roles = roles;
    }
}
```

### 3-2. AuthControllerを作成

`src/main/java/com/example/hellospringboot/controllers/AuthController.java`:

```java
package com.example.hellospringboot.controllers;

import com.example.hellospringboot.dto.LoginRequest;
import com.example.hellospringboot.dto.LoginResponse;
import com.example.hellospringboot.security.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    
    /**
     * ログイン
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login attempt for user: {}", loginRequest.getUsername());
        
        // 認証
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(),
                loginRequest.getPassword()
            )
        );
        
        // SecurityContextに認証情報を設定
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // JWT生成
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtils.generateToken(userDetails);
        
        // ロール情報を取得
        List<String> roles = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());
        
        log.info("Login successful for user: {}", loginRequest.getUsername());
        
        return ResponseEntity.ok(new LoginResponse(token, userDetails.getUsername(), roles));
    }
}
```

---

## 🚀 ステップ4: JWT認証フィルターの実装

### 4-1. JwtAuthenticationFilterを作成

`src/main/java/com/example/hellospringboot/security/JwtAuthenticationFilter.java`:

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
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Authorizationヘッダーからトークンを取得
            String jwt = parseJwt(request);
            
            if (jwt != null) {
                // トークンからユーザー名を取得
                String username = jwtUtils.getUsernameFromToken(jwt);
                
                // ユーザー詳細を取得
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                // トークンを検証
                if (jwtUtils.validateToken(jwt, userDetails)) {
                    // 認証情報を作成
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                        );
                    
                    authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    
                    // SecurityContextに認証情報を設定
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("JWT authentication successful for user: {}", username);
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }
        
        // 次のフィルターへ
        filterChain.doFilter(request, response);
    }
    
    /**
     * リクエストからJWTトークンを抽出
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);  // "Bearer " を除去
        }
        
        return null;
    }
}
```

---

## 🚀 ステップ5: SecurityConfigの更新

### 5-1. SecurityConfigを修正

`src/main/java/com/example/hellospringboot/config/SecurityConfig.java`を更新：

```java
package com.example.hellospringboot.config;

import com.example.hellospringboot.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security設定
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    /**
     * セキュリティフィルターチェーンの設定
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
            // CSRF保護を無効化（REST APIのため）
            .csrf(csrf -> csrf.disable())
            
            // 認可設定
            .authorizeHttpRequests(auth -> auth
                // 公開エンドポイント（認証不要）
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/views/**").permitAll()
                .requestMatchers("/hello").permitAll()
                
                // それ以外は認証が必要
                .anyRequest().authenticated()
            )
            
            // Basic認証を有効化
            .httpBasic(basic -> {})
            
            // セッションをステートレスに（JWTを使うため）
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );
        
        // JWTフィルターを追加
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    /**
     * パスワードエンコーダー
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * インメモリユーザー管理
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder.encode("admin123"))
            .roles("ADMIN")
            .build();
        
        UserDetails user = User.builder()
            .username("user")
            .password(passwordEncoder.encode("user123"))
            .roles("USER")
            .build();
        
        return new InMemoryUserDetailsManager(admin, user);
    }
    
    /**
     * AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(passwordEncoder);
        authenticationProvider.setUserDetailsService(userDetailsService);
        
        return new ProviderManager(authenticationProvider);
    }
}
```

### 5-2. コードの解説

#### メソッドパラメータインジェクション
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter)
```
- `JwtAuthenticationFilter`をメソッドパラメータとして注入
- コンストラクタインジェクションではなく、メソッドパラメータで注入することで**循環依存を回避**
- Spring が自動的に Bean を解決して注入

#### `SessionCreationPolicy.STATELESS`
```java
.sessionManagement(session -> 
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)
```
- **STATELESS**: セッションを作成しない
- JWTはステートレスなのでセッション不要

#### JWTフィルターの追加
```java
.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
```
- `UsernamePasswordAuthenticationFilter`の**前**に実行
- リクエストごとにトークンを検証

#### AuthenticationManager
```java
@Bean
public AuthenticationManager authenticationManager(
        UserDetailsService userDetailsService,
        PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(passwordEncoder);
    authenticationProvider.setUserDetailsService(userDetailsService);
    
    return new ProviderManager(authenticationProvider);
}
```
- ログイン時の認証に必要
- `DaoAuthenticationProvider`を使用してユーザー認証を実行
- `AuthController`で使用

---

## ✅ 動作確認

### 1. ログインしてトークン取得

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

**期待される結果**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlcyI6WyJST0xFX0FETUlOIl0sInN1YiI6ImFkbWluIiwiaWF0IjoxNzAyNDY1MjAwLCJleHAiOjE3MDI1NTE2MDB9.signature",
  "type": "Bearer",
  "username": "admin",
  "roles": ["ROLE_ADMIN"]
}
```

### 2. トークンを使ってAPIアクセス

**トークンをコピー**して以下を実行：

```bash
TOKEN="eyJhbGciOiJIUzI1NiJ9..."

curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/users
```

**期待される結果**: ユーザー一覧が取得できる

### 3. トークンなしでアクセス

```bash
curl http://localhost:8080/api/users
```

**期待される結果**: 401 Unauthorized

### 4. 無効なトークンでアクセス

```bash
curl -H "Authorization: Bearer invalid-token" \
  http://localhost:8080/api/users
```

**期待される結果**: 401 Unauthorized

### 5. 一般ユーザーでログイン

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"user123"}'
```

取得したトークンで管理者エンドポイントにアクセス：

```bash
curl -H "Authorization: Bearer $USER_TOKEN" \
  http://localhost:8080/api/users
```

**期待される結果**: 403 Forbidden（権限不足）

---

## 🎨 チャレンジ課題

### チャレンジ 1: リフレッシュトークン

**目標**: アクセストークンの有効期限が切れた際に、リフレッシュトークンで再発行

**ヒント**:
```java
@PostMapping("/refresh")
public ResponseEntity<LoginResponse> refreshToken(@RequestHeader("Refresh-Token") String refreshToken) {
    // リフレッシュトークンを検証
    // 新しいアクセストークンを発行
}
```

### チャレンジ 2: ログアウト（トークンのブラックリスト）

**目標**: トークンを無効化してログアウト機能を実装

**ヒント**:
- Redisにブラックリストを保存
- フィルターでブラックリストをチェック

### チャレンジ 3: トークンに追加情報を含める

**目標**: ユーザーIDやメールアドレスをトークンに含める

**ヒント**:
```java
claims.put("userId", user.getId());
claims.put("email", user.getEmail());
```

---

## 🐛 トラブルシューティング

### エラー: "The dependencies of some of the beans in the application context form a cycle"

**原因**: `SecurityConfig`と`JwtAuthenticationFilter`の循環依存

**問題のコード**:
```java
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;  // ❌ フィールド注入で循環依存
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) { ... }
}
```

**解決策**: メソッドパラメータで注入
```java
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, 
            JwtAuthenticationFilter jwtAuthenticationFilter) {  // ✅ メソッドパラメータで注入
        // ...
    }
}
```

### エラー: "The specified key byte array is X bits which is not secure enough"

**原因**: JWT秘密鍵が短すぎる（HS256は256ビット以上必要）

**解決策**:
```yaml
jwt:
  secret: "my-very-secure-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm"
```

32文字以上の文字列を使用してください。

### エラー: "Cannot invoke AuthenticationManager.authenticate()"

**原因**: `AuthenticationManager`がBean登録されていない

**解決策**:
```java
@Bean
public AuthenticationManager authenticationManager(
        UserDetailsService userDetailsService,
        PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(passwordEncoder);
    authenticationProvider.setUserDetailsService(userDetailsService);
    
    return new ProviderManager(authenticationProvider);
}
```

### トークンが検証されない

**デバッグ**:
```java
@GetMapping("/debug-token")
public String debugToken(@RequestHeader("Authorization") String authHeader) {
    String token = authHeader.substring(7);
    String username = jwtUtils.getUsernameFromToken(token);
    Date expiration = jwtUtils.getExpirationDateFromToken(token);
    return "Username: " + username + ", Expiration: " + expiration;
}
```

### CORSエラー（フロントエンドから呼び出す場合）

**解決策**:
```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}
```

---

## 📚 このステップで学んだこと

- ✅ JWTの仕組み（Header, Payload, Signature）
- ✅ トークンの生成と検証
- ✅ ログインエンドポイントの実装
- ✅ JWT認証フィルターの作成
- ✅ ステートレスな認証
- ✅ トークンベースのロール管理
- ✅ セッションレスなセキュリティ設定
- ✅ **循環依存の回避**（メソッドパラメータインジェクション）
- ✅ **AuthenticationManagerの適切な設定**

---

## 💡 補足: JWTのベストプラクティス

### 1. 秘密鍵の管理

**NG**: コードにハードコード
```java
private static final String SECRET = "mysecret";
```

**OK**: 環境変数から読み込み
```java
@Value("${JWT_SECRET}")
private String secret;
```

### 2. 有効期限の設定

- **アクセストークン**: 15分〜1時間
- **リフレッシュトークン**: 7日〜30日

### 3. HTTPS必須

JWTはBase64エンコードのみ（暗号化ではない）ため、**必ずHTTPS**を使用してください。

### 4. 機密情報を含めない

トークンは簡単にデコードできるため、パスワードやクレジットカード情報などは含めないでください。

### 5. トークンのサイズ

大きすぎるトークンはHTTPヘッダーサイズ制限に引っかかる可能性があります。

---

## ➡️ 次のステップ

[Step 27: ユニットテスト](STEP_27.md)へ進みましょう！

次のステップでは、JUnit 5とMockitoを使ってサービス層のユニットテストを実装します。テストコードを書くことで、コードの品質と保守性を高めます。
