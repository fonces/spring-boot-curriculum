# Step 25: Spring Securityの基礎

## 🎯 このステップの目標

- Spring Securityの役割と基本概念を理解できる
- Basic認証を実装できる
- インメモリでユーザー管理を設定できる
- 認証が必要なエンドポイントと不要なエンドポイントを区別できる

**所要時間**: 約50分

---

## 📋 事前準備

- [Step 24: Thymeleaf + REST API連携](../phase5/STEP_24.md)が完了していること
- Phase 1-5で作成したプロジェクトが動作していること
- HTTP認証の基本概念を理解していること（推奨）

---

## 🔐 Spring Securityとは

### セキュリティがない世界

**問題**: 誰でもすべてのAPIにアクセスできる

```bash
# 誰でもユーザー情報を取得できる
curl http://localhost:8080/api/users
# 誰でもユーザーを削除できる
curl -X DELETE http://localhost:8080/api/users/1
```

### Spring Securityがある世界

**改善**: 認証されたユーザーのみがアクセス可能

```bash
# 認証なし → 401 Unauthorized
curl http://localhost:8080/api/users

# 認証あり → 200 OK
curl -u username:password http://localhost:8080/api/users
```

### Spring Securityの3つの柱

1. **認証（Authentication）**: 「あなたは誰ですか？」
   - ユーザー名とパスワードでログイン
   - トークンでの認証
   
2. **認可（Authorization）**: 「あなたは何ができますか？」
   - 管理者のみ削除可能
   - 一般ユーザーは閲覧のみ
   
3. **保護（Protection）**: 「攻撃から守る」
   - CSRF対策
   - セッション固定攻撃対策

---

## 🚀 ステップ1: Spring Security依存関係の追加

### 1-1. pom.xmlに依存関係を追加

`pom.xml`の`<dependencies>`セクションに以下を追加：

```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Spring Security Test -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

### 1-2. 依存関係を反映

```bash
./mvnw clean compile
```

### 1-3. アプリケーションを起動

```bash
./mvnw spring-boot:run
```

**重要**: Spring Securityを追加すると、**自動的にすべてのエンドポイントが保護されます**。

---

## 🚀 ステップ2: デフォルトのセキュリティを確認

### 2-1. 保護されたエンドポイントにアクセス

```bash
curl http://localhost:8080/api/users
```

**結果**:
```json
{
  "timestamp": "2025-12-13T10:00:00.000+00:00",
  "status": 401,
  "error": "Unauthorized",
  "path": "/api/users"
}
```

### 2-2. デフォルトユーザーで認証

Spring Securityは起動時にランダムなパスワードを生成します。

**ログから確認**:
```
Using generated security password: 8e557245-73e2-4286-969a-ff57fe326d53
```

**認証付きリクエスト**:
```bash
# ユーザー名: user
# パスワード: ログに表示されたパスワード
curl -u user:8e557245-73e2-4286-969a-ff57fe326d53 http://localhost:8080/api/users
```

**結果**: ユーザー一覧が取得できる

### 2-3. デフォルトセキュリティの問題点

- **パスワードが毎回変わる**: 再起動のたびに新しいパスワード
- **ユーザーが1人だけ**: 複数ユーザー管理ができない
- **ロールがない**: 権限の区別ができない

---

## 🚀 ステップ3: カスタムセキュリティ設定

### 3-1. SecurityConfigクラスを作成

`src/main/java/com/example/hellospringboot/config/SecurityConfig.java`を作成：

```java
package com.example.hellospringboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    /**
     * セキュリティフィルターチェーンの設定
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // 公開エンドポイント（認証不要）
                .requestMatchers("/", "/public/**").permitAll()
                // 管理者のみアクセス可能
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                // その他はすべて認証が必要
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> {})  // Basic認証を有効化
            .csrf(csrf -> csrf.disable());  // REST APIのためCSRFを無効化
        
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
    public UserDetailsService userDetailsService() {
        // 管理者ユーザー
        UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder().encode("admin123"))
            .roles("ADMIN")
            .build();
        
        // 一般ユーザー
        UserDetails user = User.builder()
            .username("user")
            .password(passwordEncoder().encode("user123"))
            .roles("USER")
            .build();
        
        return new InMemoryUserDetailsManager(admin, user);
    }
}
```

### 3-2. コードの解説

#### `@EnableWebSecurity`
- Spring Securityの設定を有効化
- カスタム設定を適用

#### `SecurityFilterChain`
```java
.requestMatchers("/", "/public/**").permitAll()
```
- **permitAll()**: 認証なしでアクセス可能
- パターンマッチング: `/public/**`は`/public`配下すべて

```java
.requestMatchers("/api/users/**").hasRole("ADMIN")
```
- **hasRole("ADMIN")**: ADMINロールを持つユーザーのみ
- `ROLE_ADMIN`として内部保存される

```java
.anyRequest().authenticated()
```
- 上記以外はすべて認証が必要

#### `PasswordEncoder`
```java
new BCryptPasswordEncoder()
```
- **BCrypt**: パスワードを安全にハッシュ化
- 同じパスワードでも毎回異なるハッシュ値

#### `UserDetailsService`
```java
UserDetails admin = User.builder()
    .username("admin")
    .password(passwordEncoder().encode("admin123"))
    .roles("ADMIN")
    .build();
```
- **User.builder()**: ユーザー情報を構築
- **password()**: エンコードされたパスワードを設定
- **roles()**: ロール（権限）を設定

---

## 🚀 ステップ4: 公開エンドポイントの追加

### 4-1. PublicControllerを作成

`src/main/java/com/example/hellospringboot/controllers/PublicController.java`を作成：

```java
package com.example.hellospringboot.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/public")
public class PublicController {
    
    /**
     * 認証不要のエンドポイント
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now()
        );
    }
    
    /**
     * 認証不要の情報取得
     */
    @GetMapping("/info")
    public Map<String, String> info() {
        return Map.of(
            "application", "Spring Boot App",
            "version", "1.0.0",
            "description", "Spring Security Basic Authentication Demo"
        );
    }
}
```

---

## ✅ 動作確認

### 1. 公開エンドポイントへのアクセス

```bash
# 認証なしでアクセス可能
curl http://localhost:8080/public/health
```

**期待される結果**:
```json
{
  "status": "UP",
  "timestamp": "2025-12-13T10:30:00"
}
```

### 2. 保護されたエンドポイントへのアクセス（認証なし）

```bash
curl http://localhost:8080/api/users
```

**期待される結果**:
```json
{
  "timestamp": "2025-12-13T10:31:00.000+00:00",
  "status": 401,
  "error": "Unauthorized",
  "path": "/api/users"
}
```

### 3. 管理者ユーザーで認証

```bash
curl -u admin:admin123 http://localhost:8080/api/users
```

**期待される結果**: ユーザー一覧が取得できる

### 4. 一般ユーザーで認証（権限不足）

```bash
curl -u user:user123 http://localhost:8080/api/users
```

**期待される結果**:
```json
{
  "timestamp": "2025-12-13T10:32:00.000+00:00",
  "status": 403,
  "error": "Forbidden",
  "path": "/api/users"
}
```

**403 Forbidden**: 認証は成功したが、権限が不足

---

## 🚀 ステップ5: ロール別エンドポイントの実装

### 5-1. 一般ユーザー向けエンドポイントを追加

`PublicController.java`に追加：

```java
package com.example.hellospringboot.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/public")
public class PublicController {
    
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now()
        );
    }
    
    @GetMapping("/info")
    public Map<String, String> info() {
        return Map.of(
            "application", "Spring Boot App",
            "version", "1.0.0",
            "description", "Spring Security Basic Authentication Demo"
        );
    }
}
```

新しい`UserProfileController`を作成：

`src/main/java/com/example/hellospringboot/controllers/UserProfileController.java`:

```java
package com.example.hellospringboot.controllers;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/profile")
public class UserProfileController {
    
    /**
     * ログイン中のユーザー情報を取得
     */
    @GetMapping("/me")
    public Map<String, Object> getCurrentUser(Authentication authentication) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("username", authentication.getName());
        profile.put("roles", authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()));
        profile.put("authenticated", authentication.isAuthenticated());
        
        return profile;
    }
}
```

### 5-2. SecurityConfigを更新

`SecurityConfig.java`の`securityFilterChain`メソッドを修正：

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(authorize -> authorize
            // 公開エンドポイント（認証不要）
            .requestMatchers("/", "/public/**").permitAll()
            // 管理者のみアクセス可能
            .requestMatchers("/api/users/**").hasRole("ADMIN")
            // 認証済みユーザーならアクセス可能
            .requestMatchers("/api/profile/**").authenticated()
            // その他はすべて認証が必要
            .anyRequest().authenticated()
        )
        .httpBasic(basic -> {})
        .csrf(csrf -> csrf.disable());
    
    return http.build();
}
```

---

## ✅ ロール別動作確認

### 1. 管理者でプロフィール取得

```bash
curl -u admin:admin123 http://localhost:8080/api/profile/me
```

**期待される結果**:
```json
{
  "username": "admin",
  "roles": ["ROLE_ADMIN"],
  "authenticated": true
}
```

### 2. 一般ユーザーでプロフィール取得

```bash
curl -u user:user123 http://localhost:8080/api/profile/me
```

**期待される結果**:
```json
{
  "username": "user",
  "roles": ["ROLE_USER"],
  "authenticated": true
}
```

### 3. 一般ユーザーで管理者エンドポイントにアクセス

```bash
curl -u user:user123 http://localhost:8080/api/users
```

**期待される結果**: 403 Forbidden

---

## 🎨 チャレンジ課題

### チャレンジ 1: 閲覧専用ロールの追加

**目標**: ユーザーの閲覧のみ可能な`VIEWER`ロールを作成

**ヒント**:
```java
// UserDetailsService
UserDetails viewer = User.builder()
    .username("viewer")
    .password(passwordEncoder().encode("viewer123"))
    .roles("VIEWER")
    .build();

// SecurityFilterChain
.requestMatchers(HttpMethod.GET, "/api/users/**").hasAnyRole("ADMIN", "VIEWER")
.requestMatchers("/api/users/**").hasRole("ADMIN")
```

### チャレンジ 2: メソッドレベルセキュリティ

**目標**: `@PreAuthorize`アノテーションでメソッドレベルの権限制御

**ヒント**:
```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    // ...
}

@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long id) {
    // 管理者のみ実行可能
}
```

### チャレンジ 3: カスタムエラーレスポンス

**目標**: 401/403エラー時にカスタムJSONを返す

**ヒント**:
```java
http
    .exceptionHandling(exception -> exception
        .authenticationEntryPoint(customAuthenticationEntryPoint)
        .accessDeniedHandler(customAccessDeniedHandler)
    );
```

---

## 🐛 トラブルシューティング

### エラー: "There is no PasswordEncoder mapped for the id \"null\""

**原因**: パスワードがエンコードされていない

**解決策**:
```java
// NG: 平文パスワード
.password("admin123")

// OK: エンコードされたパスワード
.password(passwordEncoder().encode("admin123"))
```

### エラー: "Access Denied" (403)

**原因**: ロールが不足している、またはロール名が間違っている

**確認事項**:
1. ユーザーに正しいロールが設定されているか
2. `hasRole("ADMIN")`の場合、実際には`ROLE_ADMIN`として保存される
3. `authentication.getAuthorities()`でロールを確認

**デバッグ**:
```java
@GetMapping("/debug")
public String debug(Authentication auth) {
    return "Roles: " + auth.getAuthorities();
}
```

### CSRF対策を無効化する理由

**問題**: REST APIでCSRFトークンが必要になる

**REST APIの場合**:
```java
.csrf(csrf -> csrf.disable())  // ステートレスなのでCSRF不要
```

**Thymeleafの場合**:
```java
.csrf(csrf -> {})  // CSRFを有効化（デフォルト）
```

### Basic認証のBase64エンコード

**仕組み**:
```bash
# "username:password" をBase64エンコード
echo -n "admin:admin123" | base64
# 結果: YWRtaW46YWRtaW4xMjM=

# Authorizationヘッダー
curl -H "Authorization: Basic YWRtaW46YWRtaW4xMjM=" http://localhost:8080/api/users
```

---

## 📚 このステップで学んだこと

- ✅ Spring Securityの基本概念（認証・認可・保護）
- ✅ Basic認証の実装方法
- ✅ インメモリユーザー管理
- ✅ ロールベースのアクセス制御
- ✅ エンドポイントごとの権限設定
- ✅ パスワードのエンコーディング（BCrypt）
- ✅ 公開エンドポイントと保護エンドポイントの区別

---

## 💡 補足: Basic認証の限界

### Basic認証の特徴

**メリット**:
- 実装が簡単
- 設定がシンプル
- デバッグしやすい

**デメリット**:
- **セキュリティが低い**: Base64エンコードは暗号化ではない
- **HTTPS必須**: 平文で送信されるため盗聴リスク
- **ログアウト不可**: ブラウザがパスワードをキャッシュ
- **状態管理**: 毎回認証情報を送信

### 本番環境では

- **JWT認証**: ステートレスなトークンベース認証
- **OAuth 2.0**: サードパーティ認証（Google, Githubなど）
- **HTTPS必須**: すべての通信を暗号化

次のステップでJWT認証を実装します！

---

## ➡️ 次のステップ

[Step 26: JWTトークン認証](STEP_26.md)へ進みましょう！

次のステップでは、モダンなトークンベース認証であるJWT（JSON Web Token）を実装します。Basic認証の限界を克服し、よりセキュアで柔軟な認証方式を学びます。
