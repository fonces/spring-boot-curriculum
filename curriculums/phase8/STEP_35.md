# Step 35: 認証・認可機能の実装

## 🎯 このステップの目標

- Spring Security + JWT認証でユーザー管理を実装できる
- JwtTokenProviderでトークンの生成と検証ができる
- JwtAuthenticationFilterでリクエストを検証できる
- UserPrincipalでUserDetailsを実装できる
- SecurityConfigでセキュリティ設定を構成できる
- AuthServiceで認証ロジックを実装できる
- AuthControllerで認証APIエンドポイントを提供できる

**所要時間**: 約120分

> **📝 このステップの形式について**
> 
> このステップでは、完全なコードを提示する代わりに**機能要件とヒント**を提示します。
> 各クラスの要件を読み、自分で実装にチャレンジしてください。
> 動作確認セクションのcurlコマンドで正しく動作すれば成功です！

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

## 🚀 ステップ3: JwtTokenProviderの実装 【自分で実装】

**ファイルパス**: `src/main/java/com/example/bloghub/security/JwtTokenProvider.java`

### 機能要件

JWTトークンの生成・検証を担当するユーティリティクラスを実装します。

- JWTトークンの生成
- トークンからユーザー名の抽出
- トークンの有効性検証
- 署名用の秘密鍵管理

### 必要なフィールド

| フィールド | 型 | 説明 |
|-----------|------|------|
| `jwtSecret` | String | `@Value("${jwt.secret}")`で読み込む秘密鍵 |
| `jwtExpiration` | long | `@Value("${jwt.expiration}")`で読み込む有効期限 |

### 必要なメソッド

| メソッド | 引数 | 戻り値 | 説明 |
|---------|------|--------|------|
| `generateToken` | `Authentication` | `String` | 認証情報からJWTを生成 |
| `generateTokenFromUsername` | `String username` | `String` | ユーザー名からJWTを生成 |
| `getUsernameFromToken` | `String token` | `String` | トークンからユーザー名を抽出 |
| `validateToken` | `String token` | `boolean` | トークンの有効性を検証 |
| `getSigningKey` | なし | `SecretKey` | 署名用の秘密鍵を取得（private） |

### 実装ヒント

**トークン生成**:
```java
Jwts.builder()
    .subject(username)
    .issuedAt(now)
    .expiration(expiryDate)
    .signWith(getSigningKey())
    .compact();
```

**トークン検証**:
```java
Jwts.parser()
    .verifyWith(getSigningKey())
    .build()
    .parseSignedClaims(token)
    .getPayload();
```

**秘密鍵の生成**:
```java
byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
return Keys.hmacShaKeyFor(keyBytes);
```

**キャッチすべき例外**:
- `SecurityException`: 無効な署名
- `MalformedJwtException`: 不正なトークン形式
- `ExpiredJwtException`: 有効期限切れ
- `UnsupportedJwtException`: サポートされていない形式
- `IllegalArgumentException`: 空のトークン

### 使用するアノテーション・インポート

```java
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
```

**使用するアノテーション**: `@Component`, `@Value`

---

## 🚀 ステップ4: UserPrincipalの実装 【自分で実装】

**ファイルパス**: `src/main/java/com/example/bloghub/security/UserPrincipal.java`

### 機能要件

Spring SecurityのUserDetailsインターフェースを実装し、UserエンティティをSpring Securityが理解できる形式にラップします。

- `UserDetails`インターフェースの実装
- `User`エンティティからの変換
- 権限（ROLE_USER）の付与

### 必要なフィールド

| フィールド | 型 | 説明 |
|-----------|------|------|
| `id` | Long | ユーザーID |
| `username` | String | ユーザー名 |
| `email` | String | メールアドレス |
| `password` | String | パスワード（ハッシュ化済み） |
| `authorities` | `Collection<? extends GrantedAuthority>` | 権限リスト |

### 必要なメソッド

| メソッド | 引数 | 戻り値 | 説明 |
|---------|------|--------|------|
| `create` | `User user` | `UserPrincipal` | static: UserからUserPrincipalを生成 |
| `getAuthorities` | なし | `Collection<? extends GrantedAuthority>` | 権限リストを返す |
| `getPassword` | なし | `String` | パスワードを返す |
| `getUsername` | なし | `String` | ユーザー名を返す |
| `isAccountNonExpired` | なし | `boolean` | `true`を返す |
| `isAccountNonLocked` | なし | `boolean` | `true`を返す |
| `isCredentialsNonExpired` | なし | `boolean` | `true`を返す |
| `isEnabled` | なし | `boolean` | `true`を返す |

### 実装ヒント

**createメソッド**:
```java
public static UserPrincipal create(User user) {
    Collection<GrantedAuthority> authorities = Collections.singletonList(
        new SimpleGrantedAuthority("ROLE_USER")
    );
    // UserPrincipalのコンストラクタを呼び出す
}
```

### 使用するアノテーション・インポート

```java
import com.example.bloghub.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.Collections;
```

**使用するアノテーション**: `@Getter`, `@AllArgsConstructor`

**実装するインターフェース**: `UserDetails`

---

## 🚀 ステップ5: CustomUserDetailsServiceの実装 【自分で実装】

**ファイルパス**: `src/main/java/com/example/bloghub/security/CustomUserDetailsService.java`

### 機能要件

Spring Securityの認証で使用するUserDetailsServiceを実装します。

- `UserDetailsService`インターフェースの実装
- usernameからユーザー情報をデータベースから取得
- 取得したUserエンティティをUserPrincipalに変換

### 必要なフィールド

| フィールド | 型 | 説明 |
|-----------|------|------|
| `userRepository` | UserRepository | `@RequiredArgsConstructor`でDI |

### 必要なメソッド

| メソッド | 引数 | 戻り値 | 説明 |
|---------|------|--------|------|
| `loadUserByUsername` | `String username` | `UserDetails` | usernameからUserDetailsを取得 |
| `loadUserById` | `Long id` | `UserDetails` | IDからUserDetailsを取得（オプション） |

### 実装ヒント

**loadUserByUsernameメソッド**:
```java
@Override
@Transactional(readOnly = true)
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(
                    "User not found with username: " + username
            ));
    return UserPrincipal.create(user);
}
```

### 使用するアノテーション・インポート

```java
import com.example.bloghub.entity.User;
import com.example.bloghub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
```

**使用するアノテーション**: `@Service`, `@RequiredArgsConstructor`, `@Transactional(readOnly = true)`

**実装するインターフェース**: `UserDetailsService`

---

## 🚀 ステップ6: JwtAuthenticationFilterの実装 【自分で実装】

**ファイルパス**: `src/main/java/com/example/bloghub/security/JwtAuthenticationFilter.java`

### 機能要件

HTTPリクエストのAuthorizationヘッダーからJWTトークンを抽出し、検証してSecurityContextに認証情報を設定するフィルターを実装します。

- `OncePerRequestFilter`を継承（リクエストごとに1回だけ実行）
- `Authorization: Bearer <token>`ヘッダーからトークンを抽出
- トークンを検証し、認証情報をSecurityContextHolderに設定

### 必要なフィールド

| フィールド | 型 | 説明 |
|-----------|------|------|
| `tokenProvider` | JwtTokenProvider | トークン操作 |
| `customUserDetailsService` | CustomUserDetailsService | ユーザー情報取得 |

### 必要なメソッド

| メソッド | 引数 | 戻り値 | 説明 |
|---------|------|--------|------|
| `doFilterInternal` | `HttpServletRequest`, `HttpServletResponse`, `FilterChain` | `void` | フィルター処理のメイン |
| `getJwtFromRequest` | `HttpServletRequest` | `String` | リクエストからトークンを抽出（private） |

### 実装ヒント

**doFilterInternalメソッドの流れ**:
1. リクエストからJWTトークンを取得
2. トークンが存在し、有効な場合:
   - トークンからusernameを取得
   - UserDetailsServiceでユーザー情報を取得
   - `UsernamePasswordAuthenticationToken`を作成
   - `SecurityContextHolder.getContext().setAuthentication()`で設定
3. 例外発生時はログ出力のみ（認証失敗として続行）
4. `filterChain.doFilter(request, response)`で次のフィルターへ

**getJwtFromRequestメソッド**:
```java
private String getJwtFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
        return bearerToken.substring(7);
    }
    return null;
}
```

**認証情報の設定**:
```java
UsernamePasswordAuthenticationToken authentication = 
    new UsernamePasswordAuthenticationToken(
        userDetails, 
        null, 
        userDetails.getAuthorities()
    );
authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
SecurityContextHolder.getContext().setAuthentication(authentication);
```

### 使用するアノテーション・インポート

```java
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
```

**使用するアノテーション**: `@Component`, `@RequiredArgsConstructor`

**継承するクラス**: `OncePerRequestFilter`

---

## 🚀 ステップ7: SecurityConfigの実装 【自分で実装】

**ファイルパス**: `src/main/java/com/example/bloghub/config/SecurityConfig.java`

### 機能要件

Spring Securityの設定クラスを実装します。セキュリティフィルターチェーン、認証プロバイダ、パスワードエンコーダー、CORS設定を構成します。

- SecurityFilterChainの構成
- 認証不要パスと認証必須パスの定義
- フォームログインとAPI認証の両立
- JWTフィルターの追加

### 必要なフィールド

| フィールド | 型 | 説明 |
|-----------|------|------|
| `jwtAuthenticationFilter` | JwtAuthenticationFilter | JWTフィルター |
| `userDetailsService` | UserDetailsService | ユーザー情報取得 |

### 必要なBeanメソッド

| メソッド | 戻り値 | 説明 |
|---------|--------|------|
| `securityFilterChain` | `SecurityFilterChain` | セキュリティ設定のメイン |
| `authenticationProvider` | `DaoAuthenticationProvider` | 認証プロバイダ |
| `authenticationManager` | `AuthenticationManager` | 認証マネージャー |
| `passwordEncoder` | `PasswordEncoder` | BCryptエンコーダー |
| `corsConfigurationSource` | `CorsConfigurationSource` | CORS設定 |

### SecurityFilterChainの設定要件

**無効化する機能**:
- CSRF（REST APIではステートレスなため）

**セッション管理**:
- `SessionCreationPolicy.STATELESS`（セッションを使わない）

**認証不要パス**（`permitAll()`）:
- `/`, `/login`, `/signup`
- `/css/**`, `/js/**`, `/images/**`, `/error`
- `/api/auth/**`（サインアップ、ログイン）
- `/api/files/**`（ファイル取得）
- `GET /articles/{id}`, `/tags`, `/tags/**`, `/users/{username}`（閲覧）

**認証必須パス**（`authenticated()`）:
- `/articles/new`, `/articles/*/edit`, `/articles/*/delete`
- `/profile`, `/articles/*/comments`, `/comments/*/delete`
- その他すべて（`anyRequest().authenticated()`）

**フォームログイン設定**:
- `loginPage("/login")`
- `defaultSuccessUrl("/", true)`
- `failureUrl("/login?error=true")`

**JWTフィルター**:
- `UsernamePasswordAuthenticationFilter`の前に`JwtAuthenticationFilter`を配置

### 実装ヒント

**SecurityFilterChainのスケルトン**:
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            // パスの設定
            .requestMatchers("/api/auth/**").permitAll()
            // ... その他のパス設定
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            // フォームログイン設定
        )
        .logout(logout -> logout
            // ログアウト設定
        )
        .authenticationProvider(authenticationProvider())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    
    return http.build();
}
```

**CORS設定**:
```java
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
```

### 使用するアノテーション・インポート

```java
import com.example.bloghub.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
```

**使用するアノテーション**: `@Configuration`, `@EnableWebSecurity`, `@EnableMethodSecurity`, `@RequiredArgsConstructor`

---

## 🚀 ステップ8: UserRepositoryの実装 【自分で実装】

**ファイルパス**: `src/main/java/com/example/bloghub/repository/UserRepository.java`

### 機能要件

Userエンティティのデータアクセスを担当するリポジトリインターフェースを実装します。

- `JpaRepository`を継承
- usernameとemailでの検索
- 重複チェック用のexistsメソッド

### 必要なメソッド

| メソッド | 引数 | 戻り値 | 説明 |
|---------|------|--------|------|
| `findByUsername` | `String username` | `Optional<User>` | usernameでユーザーを検索 |
| `findByEmail` | `String email` | `Optional<User>` | emailでユーザーを検索 |
| `existsByUsername` | `String username` | `boolean` | usernameの重複チェック |
| `existsByEmail` | `String email` | `boolean` | emailの重複チェック |

### 実装ヒント

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Spring Data JPAがメソッド名から自動でクエリを生成します
    Optional<User> findByUsername(String username);
    // ... 他のメソッドも同様
}
```

### 使用するアノテーション・インポート

```java
import com.example.bloghub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
```

**使用するアノテーション**: `@Repository`

**継承するインターフェース**: `JpaRepository<User, Long>`

---

## 🚀 ステップ9: DTOクラスの作成 【自分で実装】

認証機能で使用する4つのDTOクラスを実装します。

### 9-1. SignupRequest

**ファイルパス**: `src/main/java/com/example/bloghub/dto/auth/SignupRequest.java`

**フィールドと検証**:

| フィールド | 型 | バリデーション |
|-----------|------|----------------|
| `username` | String | `@NotBlank`, `@Size(min=3, max=20)` |
| `email` | String | `@NotBlank`, `@Email` |
| `password` | String | `@NotBlank`, `@Size(min=6, max=40)` |

**使用するアノテーション**: `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`

---

### 9-2. LoginRequest

**ファイルパス**: `src/main/java/com/example/bloghub/dto/auth/LoginRequest.java`

**フィールドと検証**:

| フィールド | 型 | バリデーション |
|-----------|------|----------------|
| `username` | String | `@NotBlank` |
| `password` | String | `@NotBlank` |

**使用するアノテーション**: `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`

---

### 9-3. AuthResponse

**ファイルパス**: `src/main/java/com/example/bloghub/dto/auth/AuthResponse.java`

**フィールド**:

| フィールド | 型 | 説明 |
|-----------|------|------|
| `token` | String | JWTトークン |
| `username` | String | ユーザー名 |
| `email` | String | メールアドレス |

**使用するアノテーション**: `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`

---

### 9-4. UserResponse

**ファイルパス**: `src/main/java/com/example/bloghub/dto/user/UserResponse.java`

**フィールド**:

| フィールド | 型 | 説明 |
|-----------|------|------|
| `id` | Long | ユーザーID |
| `username` | String | ユーザー名 |
| `email` | String | メールアドレス |
| `profileImage` | String | プロフィール画像パス |
| `createdAt` | LocalDateTime | 作成日時 |

**必要なメソッド**:

| メソッド | 引数 | 戻り値 | 説明 |
|---------|------|--------|------|
| `fromEntity` | `User user` | `UserResponse` | static: UserからUserResponseを生成 |

**使用するアノテーション**: `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`

---

### 実装ヒント

**バリデーションのインポート**:
```java
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
```

**fromEntityメソッドの例**:
```java
public static UserResponse fromEntity(User user) {
    return new UserResponse(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getProfileImage(),
        user.getCreatedAt()
    );
}
```

> **⚠️ セキュリティ注意**: レスポンスDTOにはパスワードを含めないでください！

---

## 🚀 ステップ10: AuthServiceの実装 【自分で実装】

**ファイルパス**: `src/main/java/com/example/bloghub/service/AuthService.java`

### 機能要件

認証に関するビジネスロジックを担当するサービスクラスを実装します。

- ユーザー登録（サインアップ）
- ログイン（JWT発行）
- 現在のユーザー情報取得

### 必要なフィールド

| フィールド | 型 | 説明 |
|-----------|------|------|
| `userRepository` | UserRepository | ユーザーデータアクセス |
| `passwordEncoder` | PasswordEncoder | パスワードハッシュ化 |
| `tokenProvider` | JwtTokenProvider | JWT生成 |
| `authenticationManager` | AuthenticationManager | 認証処理 |

### 必要なメソッド

| メソッド | 引数 | 戻り値 | 説明 |
|---------|------|--------|------|
| `signup` | `SignupRequest` | `void` | ユーザー登録 |
| `login` | `LoginRequest` | `AuthResponse` | ログイン、JWT発行 |
| `getCurrentUser` | `String username` | `UserResponse` | 現在のユーザー情報取得 |

### 実装ヒント

**signupメソッドの流れ**:
1. `userRepository.existsByUsername()`で重複チェック
2. `userRepository.existsByEmail()`で重複チェック
3. 重複があれば`IllegalArgumentException`をスロー
4. 新しい`User`エンティティを作成
5. `passwordEncoder.encode()`でパスワードをハッシュ化
6. `createdAt`と`updatedAt`を設定
7. `userRepository.save()`で保存

```java
@Transactional
public void signup(SignupRequest request) {
    if (userRepository.existsByUsername(request.getUsername())) {
        throw new IllegalArgumentException("Username is already taken");
    }
    // emailの重複チェックも同様に実装
    
    User user = new User();
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    
    userRepository.save(user);
}
```

**loginメソッドの流れ**:
1. `authenticationManager.authenticate()`で認証
2. `SecurityContextHolder`に認証情報を設定
3. `tokenProvider.generateTokenFromUsername()`でJWT生成
4. `userRepository.findByUsername()`でユーザー情報取得
5. `AuthResponse`を返す

```java
@Transactional(readOnly = true)
public AuthResponse login(LoginRequest request) {
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.getUsername(),
            request.getPassword()
        )
    );
    SecurityContextHolder.getContext().setAuthentication(authentication);
    
    String token = tokenProvider.generateTokenFromUsername(request.getUsername());
    
    User user = userRepository.findByUsername(request.getUsername())
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    
    return new AuthResponse(token, user.getUsername(), user.getEmail());
}
```

**getCurrentUserメソッド**:
```java
@Transactional(readOnly = true)
public UserResponse getCurrentUser(String username) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    return UserResponse.fromEntity(user);
}
```

### 使用するアノテーション・インポート

```java
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
```

**使用するアノテーション**: `@Service`, `@RequiredArgsConstructor`, `@Transactional`

---

## 🚀 ステップ11: AuthControllerの実装 【自分で実装】

**ファイルパス**: `src/main/java/com/example/bloghub/controller/AuthController.java`

### 機能要件

認証APIエンドポイントを提供するRESTコントローラーを実装します。

- ユーザー登録エンドポイント（認証不要）
- ログインエンドポイント（認証不要）
- 現在のユーザー情報取得エンドポイント（認証必要）

### 必要なフィールド

| フィールド | 型 | 説明 |
|-----------|------|------|
| `authService` | AuthService | 認証サービス |

### 必要なメソッド

| メソッド | HTTPメソッド | パス | 引数 | 戻り値 | 説明 |
|---------|-------------|------|------|--------|------|
| `signup` | POST | `/api/auth/signup` | `@Valid @RequestBody SignupRequest` | `ResponseEntity<?>` | ユーザー登録 |
| `login` | POST | `/api/auth/login` | `@Valid @RequestBody LoginRequest` | `ResponseEntity<?>` | ログイン |
| `getCurrentUser` | GET | `/api/auth/me` | `@AuthenticationPrincipal UserPrincipal` | `ResponseEntity<UserResponse>` | 現在のユーザー情報 |

### 実装ヒント

**signupメソッド**:
```java
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
```

**loginメソッド**:
```java
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
```

**getCurrentUserメソッド**:
```java
@GetMapping("/me")
public ResponseEntity<UserResponse> getCurrentUser(
        @AuthenticationPrincipal UserPrincipal userPrincipal) {
    UserResponse response = authService.getCurrentUser(userPrincipal.getUsername());
    return ResponseEntity.ok(response);
}
```

### 使用するアノテーション・インポート

```java
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
```

**使用するアノテーション**: `@RestController`, `@RequestMapping("/api/auth")`, `@RequiredArgsConstructor`

### ポイント

- `@Valid`: リクエストボディのバリデーション
- `@AuthenticationPrincipal`: SecurityContextから認証済みユーザーを取得
- `Map.of()`: 簡潔なJSONレスポンス作成

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
    "http://localhost:8080" // localhost:8080 からのアクセス許可がされていることを確認
));
```

3. アプリケーションを再起動

---

## 📝 まとめ

このステップでは、以下のクラスを**自分で実装**しました：

### 実装したクラス一覧

| クラス | パッケージ | 役割 |
|--------|-----------|------|
| `JwtTokenProvider` | security | JWTトークンの生成・検証 |
| `UserPrincipal` | security | UserDetailsの実装 |
| `CustomUserDetailsService` | security | ユーザー情報のロード |
| `JwtAuthenticationFilter` | security | リクエストからJWT検証 |
| `SecurityConfig` | config | セキュリティ設定 |
| `UserRepository` | repository | ユーザーデータアクセス |
| `SignupRequest` | dto.auth | サインアップリクエスト |
| `LoginRequest` | dto.auth | ログインリクエスト |
| `AuthResponse` | dto.auth | 認証レスポンス |
| `UserResponse` | dto.user | ユーザー情報レスポンス |
| `AuthService` | service | 認証ビジネスロジック |
| `AuthController` | controller | 認証APIエンドポイント |

### 学んだ概念

1. **JWT認証の仕組み**: ステートレスな認証でスケーラブルなAPI
2. **Spring Securityの構成**: SecurityFilterChain、認証プロバイダ、パスワードエンコーダー
3. **UserDetailsの実装**: Spring Securityが理解できるユーザー情報形式
4. **フィルターチェーン**: リクエストごとにトークンを検証
5. **BCryptPasswordEncoder**: パスワードの安全なハッシュ化
6. **@AuthenticationPrincipal**: 認証済みユーザーの取得

### 動作確認チェックリスト

以下がすべて動作すれば成功です：

- [ ] ユーザー登録 (`POST /api/auth/signup`) が成功する
- [ ] ログイン (`POST /api/auth/login`) でトークンが返される
- [ ] トークン付きで `GET /api/auth/me` にアクセスできる
- [ ] トークンなしで `GET /api/auth/me` にアクセスすると403エラー
- [ ] 重複ユーザー名でサインアップするとエラー

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
