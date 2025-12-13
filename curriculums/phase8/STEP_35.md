# Step 35: 認証・認可機能の実装

## 🎯 このステップの目標

- Spring Security + JWT認証でユーザー管理を実装できる
- JwtTokenProviderでトークンの生成と検証ができる
- JwtAuthenticationFilterでリクエストを検証できる
- UserPrincipalでUserDetailsを実装できる
- SecurityConfigでセキュリティ設定を構成できる
- AuthServiceで認証ロジックを実装できる
- AuthControllerで認証APIエンドポイントを提供できる

**所要時間**: 約90分

---

## 📋 事前準備

このステップを始める前に、以下を確認してください：

- [Step 34: プロジェクト概要と環境構築](STEP_34.md)が完了していること
- `bloghub`プロジェクトが正常に動作していること
- Docker ComposeでMySQLが起動していること
- JWT認証の基本概念を理解していること（推奨）

**必須の環境**:
```bash
# プロジェクトディレクトリに移動
cd workspace/bloghub

# MySQLコンテナが起動していることを確認
docker-compose ps

# アプリケーションが起動できることを確認
./mvnw spring-boot:run
```

---

## 🔐 JWT認証の仕組み

### なぜJWT認証が必要か

**従来のセッション認証の問題点**:
- サーバー側でセッション情報を保持（メモリ消費）
- スケールアウトが困難（セッション共有が必要）
- モバイルアプリとの相性が悪い

**JWT認証のメリット**:
- **ステートレス**: サーバーに状態を保存しない
- **スケーラブル**: 複数サーバーで並列処理可能
- **自己完結**: トークン内にユーザー情報を含む
- **柔軟性**: REST API、SPA、モバイルアプリに最適

### BlogHubの認証フロー

```
┌─────────┐                        ┌──────────┐
│ Client  │                        │  Server  │
└────┬────┘                        └─────┬────┘
     │                                   │
     │ 1. POST /api/auth/signup          │
     │   {username, email, password}     │
     ├──────────────────────────────────>│
     │                                   │ ユーザー登録
     │                                   │ パスワードハッシュ化
     │                                   │
     │ 2. {message: "登録成功"}          │
     │<──────────────────────────────────┤
     │                                   │
     │ 3. POST /api/auth/login           │
     │   {username, password}            │
     ├──────────────────────────────────>│
     │                                   │ 認証チェック
     │                                   │ JWT発行
     │                                   │
     │ 4. {token: "eyJhbGc...", ...}     │
     │<──────────────────────────────────┤
     │                                   │
     │ 5. GET /api/auth/me               │
     │   Header: Authorization: Bearer eyJhbGc...
     ├──────────────────────────────────>│
     │                                   │ トークン検証
     │                                   │ ユーザー情報取得
     │                                   │
     │ 6. {id, username, email, ...}     │
     │<──────────────────────────────────┤
```

---

## 🚀 ステップ1: JWT依存関係の追加

### 1-1. pom.xmlに依存関係を追加

`pom.xml`の`<dependencies>`セクションに以下を追加：

```xml
<!-- Spring Security (Step 34で追加済みの場合はスキップ) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT -->
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

**ポイント**:
- `jjwt-api`: JWT操作のインターフェース
- `jjwt-impl`: JWT実装（ランタイムのみ）
- `jjwt-jackson`: JSON処理（ランタイムのみ）

---

## 🚀 ステップ2: JWT設定をapplication.ymlに追加

### 2-1. application.ymlを更新

`src/main/resources/application.yml`に以下を追加：

```yaml
spring:
  application:
    name: bloghub
  
  datasource:
    url: jdbc:mysql://localhost:3306/bloghub
    username: user
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect

# JWT設定
jwt:
  secret: 3f8b2c7e9a1d5f4e6b8c2a9d7e5f3b1c8e6a4d2f9b7e5c3a1d8f6b4e2c9a7d5f3b
  expiration: 86400000  # 24時間（ミリ秒）

# ファイルアップロード設定
file:
  upload-dir: uploads
  max-file-size: 5MB
```

**ポイント**:
- `jwt.secret`: トークン署名用の秘密鍵（64文字以上推奨）
- `jwt.expiration`: トークンの有効期限（24時間 = 86400000ミリ秒）
- **本番環境では環境変数から秘密鍵を読み込むこと**

---

## 🚀 ステップ3: JwtTokenProviderの実装

### 3-1. JwtTokenProviderクラスを作成

`src/main/java/com/example/bloghub/security/JwtTokenProvider.java`を作成：

```java
package com.example.bloghub.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * JWTトークンを生成
     */
    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * usernameからJWTトークンを生成（ログイン時に使用）
     */
    public String generateTokenFromUsername(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * JWTトークンからusernameを取得
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    /**
     * JWTトークンを検証
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException e) {
            System.err.println("Invalid JWT signature: " + e.getMessage());
        } catch (MalformedJwtException e) {
            System.err.println("Invalid JWT token: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            System.err.println("Expired JWT token: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.err.println("Unsupported JWT token: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("JWT claims string is empty: " + e.getMessage());
        }
        return false;
    }

    /**
     * 署名用の秘密鍵を取得
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

**ポイント**:
- `generateToken()`: Authenticationからトークン生成
- `generateTokenFromUsername()`: usernameから直接トークン生成
- `getUsernameFromToken()`: トークンからusernameを抽出
- `validateToken()`: トークンの有効性を検証
- `getSigningKey()`: Base64デコードした秘密鍵でHMAC-SHA署名

---

## 🚀 ステップ4: UserPrincipalの実装

### 4-1. UserPrincipalクラスを作成

`src/main/java/com/example/bloghub/security/UserPrincipal.java`を作成：

```java
package com.example.bloghub.security;

import com.example.bloghub.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private Long id;
    private String username;
    private String email;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;

    /**
     * UserエンティティからUserPrincipalを生成
     */
    public static UserPrincipal create(User user) {
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_USER")
        );

        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
```

**ポイント**:
- `UserDetails`インターフェースを実装
- `User`エンティティをラップし、Spring Securityが理解できる形式に変換
- すべてのユーザーに`ROLE_USER`権限を付与
- アカウントの有効性は常に`true`（将来的に拡張可能）

---

## 🚀 ステップ5: CustomUserDetailsServiceの実装

### 5-1. CustomUserDetailsServiceクラスを作成

`src/main/java/com/example/bloghub/security/CustomUserDetailsService.java`を作成：

```java
package com.example.bloghub.security;

import com.example.bloghub.entity.User;
import com.example.bloghub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username: " + username
                ));

        return UserPrincipal.create(user);
    }

    /**
     * IDでユーザーを読み込む（オプション）
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with id: " + id
                ));

        return UserPrincipal.create(user);
    }
}
```

**ポイント**:
- `UserDetailsService`を実装し、Spring Securityの認証で使用
- `loadUserByUsername()`: usernameからUserDetailsを取得
- `@Transactional(readOnly = true)`: 読み取り専用トランザクション

---

## 🚀 ステップ6: JwtAuthenticationFilterの実装

### 6-1. JwtAuthenticationFilterクラスを作成

`src/main/java/com/example/bloghub/security/JwtAuthenticationFilter.java`を作成：

```java
package com.example.bloghub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.getUsernameFromToken(jwt);

                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                                userDetails, 
                                null, 
                                userDetails.getAuthorities()
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Could not set user authentication in security context", e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * リクエストヘッダーからJWTトークンを取得
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

**ポイント**:
- `OncePerRequestFilter`を継承（リクエストごとに1回だけ実行）
- `Authorization: Bearer <token>`ヘッダーからトークンを抽出
- トークンを検証し、`SecurityContextHolder`に認証情報を設定
- 認証に失敗してもフィルターチェーンを継続（後続の処理で401エラー）

---

## 🚀 ステップ7: SecurityConfigの実装

### 7-1. SecurityConfigクラスを作成

`src/main/java/com/example/bloghub/config/SecurityConfig.java`を作成：

```java
package com.example.bloghub.config;

import com.example.bloghub.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 公開パス
                        .requestMatchers("/", "/login", "/signup", 
                                "/css/**", "/js/**", "/images/**", "/error",
                                "/api/files/**").permitAll()
                        // 記事詳細とタグ、ユーザープロフィールは公開
                        .requestMatchers(HttpMethod.GET, "/articles/{id:[0-9]+}", "/tags", "/tags/**", 
                                "/users/{username}").permitAll()
                        // 記事作成・編集・削除、コメント投稿などは認証必須
                        .requestMatchers("/articles/new", "/articles/*/edit", "/articles/*/delete",
                                "/profile", "/articles/*/comments", "/comments/*/delete").authenticated()
                        // その他すべて認証必須
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/?logout")
                        .permitAll()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // H2 Consoleのフレーム表示を許可（開発環境のみ）
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:8080"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

**ポイント**:
- **CSRF無効化**: REST APIではステートレスなため不要
- **CORS設定**: フロントエンド（React等）からのアクセスを許可
- **セッション管理**: `STATELESS`（セッションを使わない）
- **認証不要パス**: 
  - `/api/auth/**`（サインアップ、ログイン、現在のユーザー情報取得）
  - `/api/articles`, `/api/articles/**`（記事の閲覧・作成・更新・削除）
  - `/api/tags`, `/api/tags/**`（タグ一覧取得）
  - `/h2-console/**`（H2データベースコンソール）
- **フィルター順序**: `JwtAuthenticationFilter`を`UsernamePasswordAuthenticationFilter`の前に配置
- **パスワードエンコーダー**: BCryptで暗号化

**注意**: 記事とタグのエンドポイントは認証なしでアクセス可能ですが、記事の作成・更新・削除はControllerで`Authentication`を要求するため、実質的には認証が必要です。

---

## 🚀 ステップ8: UserRepositoryの実装

### 8-1. UserRepositoryを作成

`src/main/java/com/example/bloghub/repository/UserRepository.java`を作成：

```java
package com.example.bloghub.repository;

import com.example.bloghub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
}
```

**ポイント**:
- `findByUsername()`: ログイン認証で使用
- `existsByUsername()`, `existsByEmail()`: 重複チェック

---

## 🚀 ステップ9: DTOクラスの作成

### 9-1. SignupRequestを作成

`src/main/java/com/example/bloghub/dto/auth/SignupRequest.java`を作成：

```java
package com.example.bloghub.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 40, message = "Password must be between 6 and 40 characters")
    private String password;
}
```

### 9-2. LoginRequestを作成

`src/main/java/com/example/bloghub/dto/auth/LoginRequest.java`を作成：

```java
package com.example.bloghub.dto.auth;

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

### 9-3. AuthResponseを作成

`src/main/java/com/example/bloghub/dto/auth/AuthResponse.java`を作成：

```java
package com.example.bloghub.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String username;
    private String email;
}
```

### 9-4. UserResponseを作成

`src/main/java/com/example/bloghub/dto/user/UserResponse.java`を作成：

```java
package com.example.bloghub.dto.user;

import com.example.bloghub.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String profileImage;
    private LocalDateTime createdAt;

    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getProfileImage(),
                user.getCreatedAt()
        );
    }
}
```

**ポイント**:
- `@NotBlank`, `@Email`, `@Size`でバリデーション
- パスワードはレスポンスDTOに含めない（セキュリティ）
- `fromEntity()`メソッドでEntityからDTOへの変換

---

## 🚀 ステップ10: AuthServiceの実装

### 10-1. AuthServiceを作成

`src/main/java/com/example/bloghub/service/AuthService.java`を作成：

```java
package com.example.bloghub.service;

import com.example.bloghub.dto.auth.AuthResponse;
import com.example.bloghub.dto.auth.LoginRequest;
import com.example.bloghub.dto.auth.SignupRequest;
import com.example.bloghub.dto.user.UserResponse;
import com.example.bloghub.entity.User;
import com.example.bloghub.repository.UserRepository;
import com.example.bloghub.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    /**
     * ユーザー登録
     */
    @Transactional
    public void signup(SignupRequest request) {
        // 重複チェック
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        // ユーザー作成
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    /**
     * ログイン
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // 認証
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // JWTトークン生成
        String token = tokenProvider.generateTokenFromUsername(request.getUsername());

        // ユーザー情報取得
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username: " + request.getUsername()
                ));

        return new AuthResponse(token, user.getUsername(), user.getEmail());
    }

    /**
     * 現在のユーザー情報を取得
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username: " + username
                ));

        return UserResponse.fromEntity(user);
    }
}
```

**ポイント**:
- `signup()`: ユーザー登録、パスワードをBCryptでハッシュ化
- `login()`: 認証成功後にJWTトークンを発行
- `getCurrentUser()`: トークンから取得したusernameでユーザー情報を返す
- 重複チェックで`IllegalArgumentException`をスロー

---

## 🚀 ステップ11: AuthControllerの実装

### 11-1. AuthControllerを作成

`src/main/java/com/example/bloghub/controller/AuthController.java`を作成：

```java
package com.example.bloghub.controller;

import com.example.bloghub.dto.auth.AuthResponse;
import com.example.bloghub.dto.auth.LoginRequest;
import com.example.bloghub.dto.auth.SignupRequest;
import com.example.bloghub.dto.user.UserResponse;
import com.example.bloghub.security.UserPrincipal;
import com.example.bloghub.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * ユーザー登録
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        try {
            authService.signup(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "User registered successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ログイン
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        }
    }

    /**
     * 現在のユーザー情報を取得
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        UserResponse response = authService.getCurrentUser(userPrincipal.getUsername());
        return ResponseEntity.ok(response);
    }
}
```

**ポイント**:
- `POST /api/auth/signup`: ユーザー登録（認証不要）
- `POST /api/auth/login`: ログイン（認証不要）
- `GET /api/auth/me`: 現在のユーザー情報取得（認証必要）
- `@AuthenticationPrincipal`: SecurityContextから認証済みユーザーを取得
- `@Valid`: リクエストボディのバリデーション

---

## 🚀 ステップ12: 動作確認

### 12-1. アプリケーションを起動

```bash
./mvnw spring-boot:run
```

### 12-2. ユーザー登録

```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123"
  }'
```

**期待される結果**:
```json
{
  "message": "User registered successfully"
}
```

### 12-3. ログイン

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

**期待される結果**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImlhdCI6MTcwMjQ2ODgwMCwiZXhwIjoxNzAyNTU1MjAwfQ...",
  "username": "testuser",
  "email": "test@example.com"
}
```

### 12-4. 現在のユーザー情報を取得

**トークンをコピー**してから以下を実行：

```bash
TOKEN="eyJhbGciOiJIUzI1NiJ9..."

curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN"
```

**期待される結果**:
```json
{
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "profileImage": null,
  "createdAt": "2025-12-13T12:00:00"
}
```

### 12-5. 認証なしでアクセス（エラー確認）

```bash
curl http://localhost:8080/api/auth/me
```

**期待される結果**:
```
HTTP/1.1 403 Forbidden
```

### 12-6. データベースを確認

```bash
docker-compose exec mysql mysql -u user -ppassword bloghub

mysql> SELECT id, username, email, created_at FROM users;
```

**期待される結果**:
```
+----+----------+-------------------+---------------------+
| id | username | email             | created_at          |
+----+----------+-------------------+---------------------+
|  1 | testuser | test@example.com  | 2025-12-13 12:00:00 |
+----+----------+-------------------+---------------------+
```

---

## 🎯 チャレンジ課題

### チャレンジ1: トークンの有効期限切れをテストする

**目標**: トークンの有効期限が切れた後の動作を確認する

**ヒント**:
- `application.yml`の`jwt.expiration`を`60000`（1分）に変更
- ログインしてトークンを取得
- 1分待ってから`/api/auth/me`にアクセス

**期待される結果**: 401 Unauthorizedエラー

---

### チャレンジ2: リフレッシュトークンを実装する

**目標**: アクセストークンの有効期限が切れた時に再発行できるようにする

**実装内容**:
1. `RefreshToken`エンティティを作成
2. `RefreshTokenRepository`を作成
3. ログイン時にリフレッシュトークンも発行
4. `POST /api/auth/refresh`エンドポイントを追加

**サンプルエンドポイント**:
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "..."}'
```

---

### チャレンジ3: パスワード変更機能を追加する

**目標**: ログイン済みユーザーがパスワードを変更できるようにする

**実装内容**:
1. `ChangePasswordRequest` DTOを作成（現在のパスワード、新しいパスワード）
2. `AuthService`に`changePassword()`メソッドを追加
3. `PUT /api/auth/password`エンドポイントを追加

**バリデーション**:
- 現在のパスワードが正しいこと
- 新しいパスワードが6文字以上であること

---

## ❓ トラブルシューティング

### エラー1: `401 Unauthorized` が返される

**原因**: トークンが無効、または認証情報が間違っている

**解決策**:
1. トークンが正しくコピーされているか確認
```bash
echo $TOKEN
```

2. トークンの有効期限を確認
```bash
# JWTデコードサイト: https://jwt.io/
# Payloadの"exp"が現在時刻より後であることを確認
```

3. ログインし直して新しいトークンを取得
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}'
```

---

### エラー2: `Bad credentials` エラー

**原因**: ユーザー名またはパスワードが間違っている

**解決策**:
1. データベースでユーザーが登録されているか確認
```bash
docker-compose exec mysql mysql -u user -ppassword bloghub -e "SELECT username FROM users;"
```

2. パスワードが正しいか確認（サインアップ時のパスワードを再確認）

3. 新しいユーザーを登録してテスト
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"newuser","email":"new@example.com","password":"newpass123"}'
```

---

### エラー3: `JWT signature does not match` エラー

**原因**: `jwt.secret`が変更された、または間違っている

**解決策**:
1. `application.yml`の`jwt.secret`を確認
```yaml
jwt:
  secret: 3f8b2c7e9a1d5f4e6b8c2a9d7e5f3b1c8e6a4d2f9b7e5c3a1d8f6b4e2c9a7d5f3b
```

2. アプリケーションを再起動
```bash
./mvnw spring-boot:run
```

3. 再ログインして新しいトークンを取得

---

### エラー4: `JWT token is expired` エラー

**原因**: トークンの有効期限が切れている

**解決策**:
1. `application.yml`の`jwt.expiration`を確認（デフォルト24時間）
```yaml
jwt:
  expiration: 86400000  # 24時間
```

2. 再ログインして新しいトークンを取得
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}'
```

---

### エラー5: CORS エラー（ブラウザから）

**症状**: ブラウザのコンソールに以下のエラー
```
Access to XMLHttpRequest at 'http://localhost:8080/api/auth/login' 
from origin 'http://localhost:3000' has been blocked by CORS policy
```

**原因**: フロントエンドのオリジンが許可されていない

**解決策**:
1. `SecurityConfig`のCORS設定を確認
```java
configuration.setAllowedOrigins(Arrays.asList(
    "http://localhost:3000",  // React開発サーバー
    "http://localhost:8080"
));
```

2. フロントエンドのURLを追加
```java
configuration.setAllowedOrigins(Arrays.asList(
    "http://localhost:3000",
    "http://localhost:4200",  // Angular
    "http://localhost:5173"   // Vite
));
```

3. アプリケーションを再起動

---

## 📝 まとめ

このステップでは、以下を学びました：

1. **JWT認証の仕組み**: ステートレスな認証でスケーラブルなAPI
2. **JwtTokenProvider**: トークンの生成、検証、ユーザー名抽出
3. **UserPrincipal**: Spring SecurityのUserDetailsを実装
4. **CustomUserDetailsService**: データベースからユーザー情報を取得
5. **JwtAuthenticationFilter**: リクエストヘッダーからトークンを検証
6. **SecurityConfig**: SecurityFilterChain、CORS、パスワードエンコーダーの設定
7. **AuthService**: ユーザー登録、ログイン、認証済みユーザー情報取得
8. **AuthController**: REST APIエンドポイントの実装
9. **BCryptPasswordEncoder**: パスワードの暗号化
10. **@AuthenticationPrincipal**: SecurityContextから認証済みユーザーを取得

**重要なポイント**:
- JWTはサーバー側に状態を保持しない（ステートレス）
- トークンには有効期限を設定する
- パスワードは必ずハッシュ化して保存
- 秘密鍵（jwt.secret）は環境変数から読み込む（本番環境）
- SecurityFilterChainの設定順序が重要
- CORS設定で許可するオリジンを明示

---

## 🚀 次のステップへ

認証・認可機能が実装できました！次のステップでは、認証が必要な記事とコメント機能を実装します。

👉 **[Step 36: 記事とコメント機能の実装](STEP_36.md)**

次のステップで学ぶこと：
- Articleエンティティの作成（User との1対多リレーション）
- Commentエンティティの作成（Article、User との関連）
- 記事のCRUD操作（認証必須）
- コメント投稿機能
- 投稿者のみが編集・削除できる認可制御

---

**お疲れさまでした！** 🎉

JWT認証を実装することで、セキュアなREST APIの基盤ができました。次はこの認証機能を活用して、実際の記事投稿・コメント機能を実装していきましょう！
