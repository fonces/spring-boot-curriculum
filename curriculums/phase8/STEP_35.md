# Step 35: 認証・認可機能の実装

## 🎯 このステップの目標

- Spring SecurityとJWTで認証システムを構築できる
- ユーザー登録・ログイン機能を実装できる
- JWTトークンの発行と検証ができる
- ロールベースのアクセス制御（RBAC）を実装できる
- セキュアなパスワード管理ができる

**所要時間**: 約90分

---

## 📋 事前準備

- Step 34までの内容を完了していること
- データベース（MySQL）が起動していること
- JWTの基本概念を理解していること（Phase 6 Step 26を復習）

---

## 🚀 ステップ1: JWTトークンプロバイダーの実装

### 1-1. JwtTokenProviderクラスの作成

`src/main/java/com/example/bloghub/security/JwtTokenProvider.java`を作成：

```java
package com.example.bloghub.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWTトークンの生成と検証を行うプロバイダー
 */
@Component
@Slf4j
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration}")
    private long jwtExpiration;
    
    /**
     * 認証情報からJWTトークンを生成
     */
    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        
        return Jwts.builder()
                .subject(Long.toString(userPrincipal.getId()))
                .claim("username", userPrincipal.getUsername())
                .claim("email", userPrincipal.getEmail())
                .claim("role", userPrincipal.getRole().name())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }
    
    /**
     * トークンからユーザーIDを取得
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaims(token);
        return Long.parseLong(claims.getSubject());
    }
    
    /**
     * トークンの有効性を検証
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        }
        return false;
    }
    
    /**
     * トークンからClaimsを取得
     */
    private Claims getClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

### 1-2. UserPrincipalクラスの作成

`src/main/java/com/example/bloghub/security/UserPrincipal.java`を作成：

```java
package com.example.bloghub.security;

import com.example.bloghub.entities.Role;
import com.example.bloghub.entities.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Securityで使用する認証済みユーザー情報
 */
@Data
@AllArgsConstructor
public class UserPrincipal implements UserDetails {
    
    private Long id;
    private String username;
    private String email;
    private String password;
    private Role role;
    
    /**
     * UserエンティティからUserPrincipalを作成
     */
    public static UserPrincipal create(User user) {
        return new UserPrincipal(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getPassword(),
            user.getRole()
        );
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_" + role.name())
        );
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

### 1-3. JwtAuthenticationFilterの作成

`src/main/java/com/example/bloghub/security/JwtAuthenticationFilter.java`を作成：

```java
package com.example.bloghub.security;

import com.example.bloghub.services.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWTトークンを検証し、認証情報をSecurityContextに設定するフィルター
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider tokenProvider;
    private final UserDetailsServiceImpl userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                Long userId = tokenProvider.getUserIdFromToken(jwt);
                
                UserDetails userDetails = userDetailsService.loadUserById(userId);
                
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
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

---

## 🚀 ステップ2: UserDetailsService実装

### 2-1. UserDetailsServiceImplの作成

`src/main/java/com/example/bloghub/services/UserDetailsServiceImpl.java`を作成：

```java
package com.example.bloghub.services;

import com.example.bloghub.entities.User;
import com.example.bloghub.repositories.UserRepository;
import com.example.bloghub.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security用のUserDetailsService実装
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        return UserPrincipal.create(user);
    }
    
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
        
        return UserPrincipal.create(user);
    }
}
```

### 2-2. UserRepositoryの拡張

`src/main/java/com/example/bloghub/repositories/UserRepository.java`を更新：

```java
package com.example.bloghub.repositories;

import com.example.bloghub.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUsername(String username);
    
    Boolean existsByEmail(String email);
    
    Boolean existsByUsername(String username);
}
```

---

## 🚀 ステップ3: 認証サービスの実装

### 3-1. DTOクラスの作成

`src/main/java/com/example/bloghub/dto/request/SignupRequest.java`を作成：

```java
package com.example.bloghub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupRequest {
    
    @NotBlank(message = "ユーザー名は必須です")
    @Size(min = 3, max = 50, message = "ユーザー名は3〜50文字で入力してください")
    private String username;
    
    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    @Size(max = 100)
    private String email;
    
    @NotBlank(message = "パスワードは必須です")
    @Size(min = 8, max = 100, message = "パスワードは8文字以上で入力してください")
    private String password;
}
```

`src/main/java/com/example/bloghub/dto/request/LoginRequest.java`を作成：

```java
package com.example.bloghub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    
    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    private String email;
    
    @NotBlank(message = "パスワードは必須です")
    private String password;
}
```

`src/main/java/com/example/bloghub/dto/response/JwtResponse.java`を作成：

```java
package com.example.bloghub.dto.response;

import com.example.bloghub.entities.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    
    private String token;
    
    @Builder.Default
    private String type = "Bearer";
    
    private Long id;
    private String username;
    private String email;
    private Role role;
}
```

`src/main/java/com/example/bloghub/dto/response/UserResponse.java`を作成：

```java
package com.example.bloghub.dto.response;

import com.example.bloghub.entities.Role;
import com.example.bloghub.entities.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    
    private Long id;
    private String username;
    private String email;
    private String profileImage;
    private Role role;
    private LocalDateTime createdAt;
    
    public static UserResponse from(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .profileImage(user.getProfileImage())
            .role(user.getRole())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
```

### 3-2. AuthServiceの作成

`src/main/java/com/example/bloghub/services/AuthService.java`を作成：

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
import com.example.bloghub.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 認証・認可サービス
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    
    /**
     * ユーザー登録
     */
    @Transactional
    public UserResponse signup(SignupRequest request) {
        // メールアドレスの重複チェック
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }
        
        // ユーザー名の重複チェック
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already in use");
        }
        
        // ユーザー作成
        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(Role.USER)
            .build();
        
        User savedUser = userRepository.save(user);
        log.info("New user registered: {}", savedUser.getUsername());
        
        return UserResponse.from(savedUser);
    }
    
    /**
     * ログイン
     */
    public JwtResponse login(LoginRequest request) {
        // 認証
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword()
            )
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // JWTトークン生成
        String jwt = tokenProvider.generateToken(authentication);
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        log.info("User logged in: {}", userPrincipal.getUsername());
        
        return JwtResponse.builder()
            .token(jwt)
            .id(userPrincipal.getId())
            .username(userPrincipal.getUsername())
            .email(userPrincipal.getEmail())
            .role(userPrincipal.getRole())
            .build();
    }
    
    /**
     * 現在のユーザー情報取得
     */
    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        User user = userRepository.findById(userPrincipal.getId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        return UserResponse.from(user);
    }
}
```

---

## 🚀 ステップ4: Security設定

### 4-1. SecurityConfigの作成

`src/main/java/com/example/bloghub/config/SecurityConfig.java`を作成：

```java
package com.example.bloghub.config;

import com.example.bloghub.security.JwtAuthenticationFilter;
import com.example.bloghub.services.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security設定
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsServiceImpl userDetailsService;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // 公開エンドポイント
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/", "/index").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                
                // 記事一覧・詳細は認証不要
                .requestMatchers("/api/articles", "/api/articles/*").permitAll()
                
                // それ以外は認証が必要
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );
        
        // JWTフィルターを追加
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        
        return new ProviderManager(authProvider);
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:8080", "http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

---

## 🚀 ステップ5: 認証コントローラーの実装

### 5-1. AuthControllerの作成

`src/main/java/com/example/bloghub/controllers/AuthController.java`を作成：

```java
package com.example.bloghub.controllers;

import com.example.bloghub.dto.request.LoginRequest;
import com.example.bloghub.dto.request.SignupRequest;
import com.example.bloghub.dto.response.JwtResponse;
import com.example.bloghub.dto.response.UserResponse;
import com.example.bloghub.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 認証コントローラー
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * ユーザー登録
     */
    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest request) {
        UserResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * ログイン
     */
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        JwtResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 現在のユーザー情報取得
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        UserResponse response = authService.getCurrentUser();
        return ResponseEntity.ok(response);
    }
}
```

---

## ✅ 動作確認

### 1. アプリケーション起動

```bash
./mvnw spring-boot:run
```

### 2. ユーザー登録

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
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "profileImage": null,
  "role": "USER",
  "createdAt": "2025-12-13T10:30:00"
}
```

### 3. ログイン

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

**期待される結果**:
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "role": "USER"
}
```

### 4. 認証情報の取得

```bash
TOKEN="<上記で取得したトークン>"

curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN"
```

**期待される結果**:
```json
{
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "profileImage": null,
  "role": "USER",
  "createdAt": "2025-12-13T10:30:00"
}
```

### 5. トークンなしでアクセス拒否確認

```bash
curl -X GET http://localhost:8080/api/auth/me
```

**期待される結果**: 401 Unauthorized

---

## 🎨 チャレンジ課題

### チャレンジ 1: リフレッシュトークンの実装

**目標**: アクセストークンの有効期限が切れても、リフレッシュトークンで再発行

**ヒント**:
```java
// RefreshTokenエンティティを作成
@Entity
public class RefreshToken {
    @Id
    private String token;
    private Long userId;
    private LocalDateTime expiryDate;
}

// リフレッシュトークン発行・検証メソッド
public String createRefreshToken(Long userId) { ... }
public String refreshAccessToken(String refreshToken) { ... }
```

### チャレンジ 2: パスワードリセット機能

**目標**: メールでリセットリンクを送信し、パスワードを再設定

**ヒント**:
1. パスワードリセットトークンの生成（UUID）
2. メールでリセットリンク送信（Step 33の非同期処理活用）
3. トークン検証とパスワード更新

### チャレンジ 3: メール認証機能

**目標**: ユーザー登録時にメール認証を必須にする

**ヒント**:
```java
@Entity
public class User {
    private Boolean emailVerified = false;
    private String emailVerificationToken;
}

// 認証メール送信
public void sendVerificationEmail(User user) { ... }

// メール認証
public void verifyEmail(String token) { ... }
```

---

## 🐛 トラブルシューティング

### エラー: "JWT signature does not match locally computed signature"

**原因**: `application.yml`のjwt.secretが短すぎる、または一致しない

**解決策**:
```yaml
jwt:
  secret: YourVeryLongSecretKeyForHS512AlgorithmMustBeAtLeast64Characters
```

### エラー: "Access Denied"

**原因**: 認証されているが、権限が不足

**解決策**:
1. ロールを確認: `ROLE_USER`または`ROLE_ADMIN`
2. `@PreAuthorize`で必要な権限を確認
3. JWTクレームにroleが含まれているか確認

### エラー: "User already exists"

**原因**: メールアドレスまたはユーザー名が既に登録されている

**解決策**:
- 別のメールアドレスで登録
- データベースから既存ユーザーを削除

### エラー: "CORS policy: No 'Access-Control-Allow-Origin' header"

**原因**: CORS設定が不足

**解決策**:
`SecurityConfig`の`corsConfigurationSource()`を確認し、フロントエンドのオリジンを追加

### エラー: "Token expired"

**原因**: JWTトークンの有効期限切れ

**解決策**:
1. 新しくログインして新しいトークンを取得
2. `jwt.expiration`を延長（開発時のみ）
3. リフレッシュトークン機能を実装

---

## 📚 このステップで学んだこと

- ✅ Spring SecurityとJWTの統合方法を理解した
- ✅ トークンベース認証の仕組みを実装した
- ✅ パスワードをBCryptで安全に暗号化する方法を学んだ
- ✅ ロールベースのアクセス制御（RBAC）を実装した
- ✅ カスタムフィルターでJWTトークンを検証する方法を理解した
- ✅ UserDetailsServiceを実装してSpring Securityと連携した
- ✅ バリデーションとエラーハンドリングを実装した

---

## ➡️ 次のステップ

[Step 36: 記事とコメント機能の実装](STEP_36.md)へ進みましょう！

次のステップでは、認証済みユーザーのみが投稿できる記事・コメント機能を実装します：
- 記事のCRUD操作（認可制御付き）
- コメント機能
- タグ管理
- 画像アップロード連携

認証基盤が整ったので、いよいよメイン機能の実装です！✨
