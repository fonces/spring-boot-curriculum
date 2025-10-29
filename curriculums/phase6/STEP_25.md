# Step 25: Spring Security基礎

## 🎯 このステップの目標

- Spring Securityの基本概念を理解する
- 認証（Authentication）と認可（Authorization）の違いを理解する
- 基本的なセキュリティ設定を実装する
- インメモリ認証を実装する

**所要時間**: 約2時間

---

## 📋 事前準備

- Phase 4まで Phase 5までのベストプラクティスが理解できていること
- ユーザー管理機能が実装されていること

**Phase 5をまだ完了していない場合**: [Phase 5](../phase5/STEP_21.md)を先に進めてください。

---

## 💡 Spring Securityとは？

### セキュリティの必要性

**セキュリティなしの場合**:
- ❌ 誰でもすべてのAPIにアクセス可能
- ❌ データの改ざん・削除が自由
- ❌ 個人情報の漏洩リスク

**セキュリティありの場合**:
- ✅ 認証されたユーザーのみアクセス可能
- ✅ 権限に応じた機能制限
- ✅ データの保護

### 認証 vs 認可

| 用語 | 英語 | 意味 | 例 |
|------|------|------|-----|
| **認証** | Authentication | 本人確認 | ログイン（ユーザー名+パスワード） |
| **認可** | Authorization | 権限確認 | 管理者のみ削除可能 |

### Spring Securityの仕組み

```
リクエスト → Filter Chain → Controller
           ↓
    SecurityContextHolder
    ├── Authentication
    └── Authorities (権限)
```

---

## 🚀 ステップ1: Spring Securityの依存関係追加

### 1-1. pom.xmlの更新

**ファイルパス**: `pom.xml`

```xml
<dependencies>
    <!-- 既存の依存関係 -->
    
    <!-- Spring Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- テスト用 -->
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 1-2. Maven Reload

IntelliJ IDEAの右サイドバー「Maven」→ 🔄 をクリック

### 1-3. アプリケーション起動

依存関係を追加しただけで、Spring Securityが有効になります。

**起動ログ**:
```
Using generated security password: 12345678-abcd-1234-abcd-123456789abc
```

**デフォルト設定**:
- ユーザー名: `user`
- パスワード: 起動時に生成される（ログに表示）
- すべてのエンドポイントが保護される

---

## 🚀 ステップ2: 基本的なセキュリティ設定

### 2-1. SecurityConfigの作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/config/SecurityConfig.java`

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

/**
 * Spring Securityの設定クラス
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * セキュリティフィルターチェーンの設定
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF保護を無効化（REST APIの場合）
            .csrf(csrf -> csrf.disable())
            
            // エンドポイントのアクセス制御
            .authorizeHttpRequests(auth -> auth
                // 公開エンドポイント
                .requestMatchers("/api/public/**").permitAll()
                
                // 認証が必要なエンドポイント
                .anyRequest().authenticated()
            )
            
            // HTTP Basic認証を有効化
            .httpBasic(basic -> {});

        return http.build();
    }

    /**
     * パスワードエンコーダー（BCrypt）
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * インメモリユーザー詳細サービス
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

### 2-2. 設定の解説

#### CSRF保護

```java
.csrf(csrf -> csrf.disable())
```
- **CSRF (Cross-Site Request Forgery)**: クロスサイトリクエストフォージェリ
- REST APIでは通常無効化（ステートレスのため）
- Webアプリケーション（フォーム）では有効にすべき

#### エンドポイントのアクセス制御

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/public/**").permitAll()  // 誰でもアクセス可
    .anyRequest().authenticated()  // それ以外は認証必須
)
```

#### HTTP Basic認証

```java
.httpBasic(basic -> {})
```
- HTTPヘッダーで認証情報を送信
- `Authorization: Basic base64(username:password)`

#### パスワードエンコーダー

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```
- パスワードをハッシュ化して保存
- BCryptは一方向ハッシュ（復号不可）

---

## ✅ ステップ3: 動作確認

### 3-1. 認証なしでアクセス（失敗）

```bash
curl http://localhost:8080/api/users
```

**期待される結果**: 401 Unauthorized
```json
{
  "timestamp": "2025-10-27T10:30:00.000+00:00",
  "status": 401,
  "error": "Unauthorized",
  "path": "/api/users"
}
```

### 3-2. Basic認証でアクセス（成功）

```bash
curl -u user:user123 http://localhost:8080/api/users
```

**または**:
```bash
curl -H "Authorization: Basic dXNlcjp1c2VyMTIz" http://localhost:8080/api/users
```

**期待される結果**: 200 OK

### 3-3. 管理者でアクセス

```bash
curl -u admin:admin123 http://localhost:8080/api/users
```

### 3-4. 公開エンドポイント

**Controller追加**:
```java
@RestController
@RequestMapping("/api/public")
public class PublicController {

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
```

**アクセス**:
```bash
curl http://localhost:8080/api/public/health
```

**期待される結果**: 認証なしでアクセス可能

---

## 🚀 ステップ4: ロールベースのアクセス制御

### 4-1. ロールによるアクセス制限

**SecurityConfig更新**:
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            // 公開エンドポイント
            .requestMatchers("/api/public/**").permitAll()
            
            // 管理者のみアクセス可能
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            
            // USERまたはADMINがアクセス可能
            .requestMatchers("/api/users/**").hasAnyRole("USER", "ADMIN")
            
            .anyRequest().authenticated()
        )
        .httpBasic(basic -> {});

    return http.build();
}
```

### 4-2. AdminControllerの作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/controller/AdminController.java`

```java
package com.example.hellospringboot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理者専用コントローラー
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, String>> getDashboard() {
        return ResponseEntity.ok(Map.of(
            "message", "Admin Dashboard",
            "role", "ADMIN"
        ));
    }
}
```

### 4-3. 動作確認

**一般ユーザーで管理者エンドポイントにアクセス（失敗）**:
```bash
curl -u user:user123 http://localhost:8080/api/admin/dashboard
```

**期待される結果**: 403 Forbidden

**管理者でアクセス（成功）**:
```bash
curl -u admin:admin123 http://localhost:8080/api/admin/dashboard
```

**期待される結果**: 200 OK

---

## 🚀 ステップ5: メソッドレベルのセキュリティ

### 5-1. @PreAuthorizeの有効化

**SecurityConfig更新**:
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // メソッドレベルのセキュリティを有効化
public class SecurityConfig {
    // ...
}
```

### 5-2. メソッドに権限チェックを追加

**UserService更新**:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * 管理者のみ実行可能
     */
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long id) {
        log.info("Deleting user with ID: {}", id);
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }

    /**
     * 自分自身または管理者のみ更新可能
     */
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        // ...
    }
}
```

---

## 🚀 ステップ6: 現在のユーザー情報取得

### 6-1. Controllerで現在のユーザー取得

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    /**
     * 現在ログインしているユーザー情報を取得
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Map<String, Object> response = Map.of(
            "username", userDetails.getUsername(),
            "authorities", userDetails.getAuthorities()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Authenticationから取得
     */
    @GetMapping("/me/alt")
    public ResponseEntity<Map<String, Object>> getCurrentUserAlt(
            Authentication authentication) {
        
        Map<String, Object> response = Map.of(
            "username", authentication.getName(),
            "authorities", authentication.getAuthorities(),
            "authenticated", authentication.isAuthenticated()
        );
        
        return ResponseEntity.ok(response);
    }
}
```

### 6-2. 動作確認

```bash
curl -u user:user123 http://localhost:8080/api/users/me
```

**期待される結果**:
```json
{
  "username": "user",
  "authorities": [
    {
      "authority": "ROLE_USER"
    }
  ]
}
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: カスタム権限

`READ`, `WRITE`, `DELETE`といった細かい権限を設定してください。

**ヒント**:
```java
UserDetails user = User.builder()
    .username("editor")
    .password(passwordEncoder().encode("editor123"))
    .authorities("READ", "WRITE")  // ROLEではなくAuthority
    .build();
```

### チャレンジ 2: 動的な権限チェック

データベースのデータに基づいて権限をチェックしてください。

**例**: 投稿の作成者のみ編集・削除可能

### チャレンジ 3: ログインAPI

`/api/auth/login`エンドポイントを作成してください。

---

## 🐛 トラブルシューティング

### 401 Unauthorizedが常に返される

**原因**: パスワードが間違っている、または設定が正しくない

**解決策**:
```java
// パスワードエンコーダーを確認
System.out.println(passwordEncoder().encode("user123"));

// ユーザーが登録されているか確認
UserDetails user = userDetailsService().loadUserByUsername("user");
```

### 403 Forbiddenが返される

**原因**: 認証は成功したが、権限が不足している

**解決策**: ロール/権限を確認
```bash
curl -u user:user123 http://localhost:8080/api/users/me
# authoritiesを確認
```

### CSRFトークンエラー

**原因**: CSRF保護が有効になっている

**解決策**: REST APIではCSRFを無効化
```java
.csrf(csrf -> csrf.disable())
```

---

## 📚 このステップで学んだこと

- ✅ Spring Securityの基本概念
- ✅ 認証（Authentication）と認可（Authorization）
- ✅ HTTP Basic認証
- ✅ インメモリユーザー管理
- ✅ ロールベースのアクセス制御
- ✅ `@PreAuthorize`によるメソッドレベルのセキュリティ
- ✅ 現在のユーザー情報取得

---

## 💡 補足: セキュリティのベストプラクティス

### パスワード保存

```java
// ❌ 悪い例: 平文で保存
String password = "user123";

// ✅ 良い例: ハッシュ化して保存
String hashedPassword = passwordEncoder.encode("user123");
```

### ロールの命名規則

Spring Securityでは`ROLE_`プレフィックスが自動付加されます：

```java
// 登録時
.roles("ADMIN")  // 内部的には"ROLE_ADMIN"

// チェック時
.hasRole("ADMIN")  // "ROLE_ADMIN"をチェック
// または
.hasAuthority("ROLE_ADMIN")
```

### HTTPSの使用

**本番環境では必須**:
```yaml
# application-prod.yml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: password
    key-store-type: PKCS12
```

---

## 🔄 Gitへのコミットとレビュー依頼

進捗を記録しましょう：

```bash
git add .
git commit -m "Step 25: Spring Securityの基礎完了"
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

次は[Step 26: JWTトークン認証](STEP_26.md)へ進みましょう！

ステートレスなJWT（JSON Web Token）認証を実装して、
よりRESTfulなAPIセキュリティを実現します。

---

お疲れさまでした！ 🎉

Spring Securityの基礎を習得しました！
次はJWTを使った本格的な認証システムを構築します！
