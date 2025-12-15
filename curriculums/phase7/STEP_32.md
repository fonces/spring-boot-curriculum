# Step 32: キャッシュ

## 🎯 このステップの目標

- Spring Cacheの基本を理解できる
- `@Cacheable`でメソッドの結果をキャッシュできる
- `@CacheEvict`でキャッシュを削除できる
- `@CachePut`でキャッシュを更新できる
- キャッシュのパフォーマンス効果を測定できる
- （オプション）Redisをキャッシュストアとして使用できる

**所要時間**: 約45分

---

## 📋 事前準備

- Step 31までの内容を完了していること
- Spring Bootアプリケーションが起動できること
- データベースにデータが存在していること

---

## 🚀 ステップ1: キャッシュとは

### 1-1. キャッシュが必要な理由

**問題: 毎回データベースにアクセスすると...**

```java
// ❌ 毎回データベースアクセス
@GetMapping("/users/{id}")
public UserResponse getUser(@PathVariable Long id) {
    return userService.getUserById(id);  // 毎回SELECT文
}
```

**課題**:
- データベースへの負荷が高い
- レスポンスが遅い
- 頻繁に読まれるデータが非効率

**解決: キャッシュ**

```java
// ✅ 2回目以降はキャッシュから取得
@Cacheable("users")
public UserResponse getUserById(Long id) {
    // 1回目: DBからデータ取得 → キャッシュに保存
    // 2回目以降: キャッシュから返す（DBアクセスなし）
    return userRepository.findById(id)...
}
```

**メリット**:
- データベース負荷の軽減
- レスポンス速度の向上
- スケーラビリティの向上

---

## 🚀 ステップ2: Spring Cacheの基本設定

### 2-1. 依存関係を追加

`pom.xml`にSpring Cacheの依存関係を追加します：

```xml
<dependencies>
    <!-- 既存の依存関係 -->
    
    <!-- Spring Cache -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>
    
    <!-- Caffeine Cache（高速なインメモリキャッシュ） -->
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
    </dependency>
</dependencies>
```

### 2-2. キャッシュを有効化

メインクラスに`@EnableCaching`を追加します：

```java
// src/main/java/com/example/hellospringboot/HelloSpringBootApplication.java
package com.example.hellospringboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching  // キャッシュを有効化
public class HelloSpringBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelloSpringBootApplication.class, args);
    }

}
```

### 2-3. キャッシュ設定を追加

`src/main/resources/application.yaml`にキャッシュ設定を追加：

```yaml
spring:
  application:
    name: hello-spring-boot
  
  # データソース設定
  datasource:
    url: jdbc:mysql://localhost:3306/spring_boot_db
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  # JPA設定
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
  
  # キャッシュ設定
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=500,expireAfterWrite=600s
    cache-names:
      - users
      - user
      - files

# JWT設定
jwt:
  secret: my-super-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm-to-work-properly
  expiration: 86400000

# ファイル保存先
file:
  upload-dir: uploads
```

### 2-4. コードの解説

#### `@EnableCaching`
```java
@EnableCaching
```
- Spring Cacheを有効化
- `@Cacheable`、`@CacheEvict`などのアノテーションが動作するようになる

#### Caffeineの設定
```yaml
spring:
  cache:
    caffeine:
      spec: maximumSize=500,expireAfterWrite=600s
```
- **maximumSize=500**: 最大500エントリまでキャッシュ
- **expireAfterWrite=600s**: 書き込みから10分後に期限切れ

---

## 🚀 ステップ3: @Cacheableで読み取りをキャッシュ

### 3-1. UserServiceにキャッシュを適用

`src/main/java/com/example/hellospringboot/services/UserService.java`を以下のように修正：

```java
package com.example.hellospringboot.services;

import com.example.hellospringboot.dto.UserCreateRequest;
import com.example.hellospringboot.dto.UserResponse;
import com.example.hellospringboot.dto.UserUpdateRequest;
import com.example.hellospringboot.entities.User;
import com.example.hellospringboot.exceptions.ResourceNotFoundException;
import com.example.hellospringboot.mappers.UserMapper;
import com.example.hellospringboot.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * 全ユーザー取得（ページネーションなし）
     * キャッシュあり
     */
    @Cacheable(value = "users", key = "'all'")
    public List<UserResponse> getAllUsers() {
        log.info("Fetching all users from database (not cached)");
        return userRepository.findAll().stream()
                .map(UserMapper::toResponse)
                .toList();
    }
    
    /**
     * 全ユーザー取得（ページネーションあり）
     */
    public Page<UserResponse> getAllUsersPaginated(Pageable pageable) {
        log.info("Fetching users with pagination: page={}, size={}", 
                pageable.getPageNumber(), pageable.getPageSize());
        
        Page<User> userPage = userRepository.findAll(pageable);
        return userPage.map(UserMapper::toResponse);
    }
    
    /**
     * ID指定でユーザー取得
     * キャッシュあり
     */
    @Cacheable(value = "user", key = "#id")
    public UserResponse getUserById(Long id) {
        log.info("Fetching user by id: {} from database (not cached)", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return UserMapper.toResponse(user);
    }
    
    /**
     * ユーザー作成
     * キャッシュを削除
     */
    @Transactional
    @CacheEvict(value = "users", key = "'all'")
    public UserResponse createUser(UserCreateRequest request) {
        log.info("Creating user: {}", request.getEmail());
        
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        
        User saved = userRepository.save(user);
        return UserMapper.toResponse(saved);
    }
    
    /**
     * ユーザー更新
     * キャッシュを更新
     */
    @Transactional
    @CachePut(value = "user", key = "#id")
    @CacheEvict(value = "users", key = "'all'")
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        log.info("Updating user: id={}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        
        User updated = userRepository.save(user);
        return UserMapper.toResponse(updated);
    }
    
    /**
     * ユーザー削除
     * キャッシュを削除
     */
    @Transactional
    @CacheEvict(value = {"user", "users"}, key = "#id")
    public void deleteUser(Long id) {
        log.info("Deleting user: id={}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        userRepository.delete(user);
    }
    
    /**
     * キーワード検索（ページネーションあり）
     */
    public Page<UserResponse> searchUsers(String keyword, Pageable pageable) {
        log.info("Searching users with keyword: {}, page={}, size={}", 
                keyword, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<User> userPage = userRepository.searchByKeyword(keyword, pageable);
        return userPage.map(UserMapper::toResponse);
    }
    
    /**
     * 名前で検索（ページネーションあり）
     */
    public Page<UserResponse> findUsersByName(String name, Pageable pageable) {
        log.info("Finding users by name: {}", name);
        Page<User> userPage = userRepository.findByNameContaining(name, pageable);
        return userPage.map(UserMapper::toResponse);
    }
}
```

### 3-2. コードの解説

#### `@Cacheable`（キャッシュから取得）
```java
@Cacheable(value = "user", key = "#id")
public UserResponse getUserById(Long id)
```
- **value**: キャッシュ名（application.yamlのcache-namesに対応）
- **key**: キャッシュキー（`#id`はメソッド引数のidを使用）
- **動作**:
  1. 初回: メソッドを実行 → 結果をキャッシュに保存
  2. 2回目以降: キャッシュから取得（メソッド実行なし）

#### `@CacheEvict`（キャッシュを削除）
```java
@CacheEvict(value = "users", key = "'all'")
public UserResponse createUser(UserCreateRequest request)
```
- **動作**: メソッド実行後、指定したキャッシュを削除
- **使用例**: データ追加/削除時にリスト全体のキャッシュを削除

#### `@CachePut`（キャッシュを更新）
```java
@CachePut(value = "user", key = "#id")
public UserResponse updateUser(Long id, UserUpdateRequest request)
```
- **動作**: メソッドを常に実行 → 結果でキャッシュを更新
- **違い**: `@Cacheable`はキャッシュがあれば実行しない、`@CachePut`は常に実行

#### 複数キャッシュの削除
```java
@CacheEvict(value = {"user", "users"}, key = "#id")
public void deleteUser(Long id)
```
- 複数のキャッシュを同時に削除できる

---

## 🚀 ステップ4: カスタムキャッシュ設定

### 4-1. CacheConfigクラスを作成

より詳細な設定をするために、設定クラスを作成します：

```java
// src/main/java/com/example/hellospringboot/config/CacheConfig.java
package com.example.hellospringboot.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * キャッシュ設定
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    /**
     * Caffeineキャッシュマネージャー
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("users", "user", "files");
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }
    
    /**
     * Caffeineビルダー
     */
    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(1000)                    // 最大1000エントリ
                .expireAfterWrite(10, TimeUnit.MINUTES)  // 書き込み後10分で期限切れ
                .recordStats();                       // 統計情報を記録
    }
}
```

### 4-2. キャッシュ統計を取得するエンドポイント

`src/main/java/com/example/hellospringboot/controllers/CacheController.java`を作成：

```java
package com.example.hellospringboot.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * キャッシュ管理API
 */
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
@Slf4j
public class CacheController {
    
    private final CacheManager cacheManager;
    
    /**
     * キャッシュ統計情報を取得
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache) {
                CaffeineCache caffeineCache = (CaffeineCache) cache;
                com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = 
                    caffeineCache.getNativeCache();
                
                Map<String, Object> cacheStats = new HashMap<>();
                cacheStats.put("size", nativeCache.estimatedSize());
                cacheStats.put("stats", nativeCache.stats());
                
                stats.put(cacheName, cacheStats);
            }
        });
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 特定のキャッシュをクリア
     */
    @DeleteMapping("/{cacheName}")
    public ResponseEntity<Map<String, String>> clearCache(@PathVariable String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        
        if (cache != null) {
            cache.clear();
            log.info("Cache cleared: {}", cacheName);
            return ResponseEntity.ok(Map.of("message", "Cache cleared: " + cacheName));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * すべてのキャッシュをクリア
     */
    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearAllCaches() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
        
        log.info("All caches cleared");
        return ResponseEntity.ok(Map.of("message", "All caches cleared"));
    }
}
```

---

## ✅ 動作確認

### 1. アプリケーションを起動

```bash
cd /path/to/hello-spring-boot
./mvnw clean install
./mvnw spring-boot:run
```

### 2. キャッシュの効果を確認

#### 初回リクエスト（キャッシュなし）

```bash
# 1回目: DBからデータ取得
curl http://localhost:8080/api/users/1
```

**ログ出力**:
```sh
Fetching user by id: 1 from database (not cached)
Hibernate: select ... from users where id=?
```

#### 2回目リクエスト（キャッシュあり）

```bash
# 2回目: キャッシュから取得
curl http://localhost:8080/api/users/1
```

**ログ出力**:
```sh
（ログなし = キャッシュから返している）
```

### 3. キャッシュ統計を確認

```bash
curl http://localhost:8080/api/cache/stats
```

**期待される結果**:
```json
{
  "user": {
    "size": 1,
    "stats": {
      "hitCount": 1,
      "missCount": 1,
      "loadSuccessCount": 1,
      "loadFailureCount": 0,
      "totalLoadTime": 123456,
      "evictionCount": 0,
      "hitRate": 0.5
    }
  },
  "users": {
    "size": 0,
    "stats": {...}
  }
}
```

**stats解説**:
- **hitCount**: キャッシュヒット回数
- **missCount**: キャッシュミス回数
- **hitRate**: ヒット率（hitCount / (hitCount + missCount)）

### 4. データ更新時のキャッシュ削除を確認

```bash
# ユーザーを更新
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Name",
    "email": "updated@example.com"
  }'

# 再度取得（キャッシュが更新されている）
curl http://localhost:8080/api/users/1
```

**期待される結果**: 更新後のデータが返る

### 5. キャッシュを手動でクリア

```bash
# userキャッシュをクリア
curl -X DELETE http://localhost:8080/api/cache/user

# すべてのキャッシュをクリア
curl -X DELETE http://localhost:8080/api/cache
```

---

## 🎨 チャレンジ課題

基本が理解できたら、以下にチャレンジしてみましょう：

### チャレンジ 1: Redisをキャッシュストアとして使用

**目標**: インメモリキャッシュ（Caffeine）ではなく、Redisを使ってキャッシュを永続化

**手順**:

1. **Redisの依存関係を追加**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

2. **Docker ComposeでRedisを起動**:
```yaml
# docker-compose.ymlに追加
redis:
  image: redis:7-alpine
  ports:
    - "6379:6379"
```

3. **application.yamlにRedis設定**:
```yaml
spring:
  cache:
    type: redis
  data:
    redis:
      host: localhost
      port: 6379
```

### チャレンジ 2: 条件付きキャッシュ

**目標**: 特定の条件下でのみキャッシュする

**ヒント**:
```java
@Cacheable(value = "user", key = "#id", condition = "#id > 100")
public UserResponse getUserById(Long id) {
    // id > 100の場合のみキャッシュ
}

@Cacheable(value = "user", key = "#id", unless = "#result.name == null")
public UserResponse getUserById(Long id) {
    // 結果のnameがnullでない場合のみキャッシュ
}
```

### チャレンジ 3: キャッシュのTTL（有効期限）を動的に設定

**目標**: キャッシュごとに異なるTTLを設定

**ヒント**:
```java
@Bean
public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    
    // userキャッシュ: 10分
    cacheManager.registerCustomCache("user", 
        Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build());
    
    // usersキャッシュ: 5分
    cacheManager.registerCustomCache("users", 
        Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build());
    
    return cacheManager;
}
```

---

## 🐛 トラブルシューティング

### エラー: "Cannot find cache named 'xxx'"

**原因**: キャッシュ名がapplication.yamlに登録されていない

**解決策**:
```yaml
spring:
  cache:
    cache-names:
      - users
      - user
      - files
      - xxx  # 追加
```

### キャッシュが効かない

**原因1**: `@EnableCaching`を付け忘れている

**解決策**:
```java
@SpringBootApplication
@EnableCaching  // 追加
public class HelloSpringBootApplication {...}
```

**原因2**: 同じクラス内のメソッド呼び出し

```java
// ❌ キャッシュが効かない
public class UserService {
    public void someMethod() {
        getUserById(1);  // 同じクラス内の呼び出し
    }
    
    @Cacheable("user")
    public UserResponse getUserById(Long id) {...}
}
```

**解決策**: 別のBeanから呼び出す

### キャッシュキーが衝突する

**原因**: 複数のメソッドで同じキーを使っている

```java
// ❌ 衝突
@Cacheable(value = "data", key = "#id")
public User getUser(Long id) {...}

@Cacheable(value = "data", key = "#id")
public Post getPost(Long id) {...}  // 同じキー
```

**解決策**: キャッシュ名を分ける、またはキーに接頭辞を付ける
```java
@Cacheable(value = "users", key = "#id")
public User getUser(Long id) {...}

@Cacheable(value = "posts", key = "#id")
public Post getPost(Long id) {...}
```

### メモリリーク

**原因**: キャッシュサイズが無制限

**解決策**: 最大サイズを設定
```yaml
spring:
  cache:
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=10m
```

---

## 📚 このステップで学んだこと

- ✅ Spring Cacheの基本概念と利点
- ✅ `@EnableCaching`でキャッシュを有効化
- ✅ `@Cacheable`でメソッド結果をキャッシュ
- ✅ `@CacheEvict`でキャッシュを削除
- ✅ `@CachePut`でキャッシュを更新
- ✅ Caffeineキャッシュの設定（サイズ、TTL）
- ✅ キャッシュキーの指定方法（`#id`、`'all'`）
- ✅ キャッシュ統計の取得と分析
- ✅ 複数キャッシュの管理
- ✅ キャッシュのクリア方法

---

## 💡 補足: キャッシュのベストプラクティス

### 1. キャッシュすべきデータ

| データ種類 | キャッシュ推奨度 | 理由 |
|---|---|---|
| **読み取り頻度が高い** | ⭐⭐⭐ | パフォーマンス向上 |
| **更新頻度が低い** | ⭐⭐⭐ | キャッシュ無効化が少ない |
| **計算コストが高い** | ⭐⭐⭐ | レスポンス時間短縮 |
| **リアルタイム性不要** | ⭐⭐ | 多少古くても問題ない |
| **更新頻度が高い** | ❌ | キャッシュ無効化が頻繁 |
| **リアルタイム性必須** | ❌ | 在庫数、残高など |

### 2. キャッシュ戦略の選択

**Cache-Aside（Lazy Loading）**:
```java
@Cacheable("users")
public User getUser(Long id) {
    // キャッシュミス時のみDBアクセス
    return userRepository.findById(id).orElseThrow();
}
```
- **メリット**: 必要なデータだけキャッシュ
- **デメリット**: 初回は遅い

**Write-Through**:
```java
@CachePut(value = "users", key = "#user.id")
public User saveUser(User user) {
    // 保存と同時にキャッシュ更新
    return userRepository.save(user);
}
```
- **メリット**: 常に最新データ
- **デメリット**: 書き込み時のオーバーヘッド

**Write-Behind（Write-Back）**:
- キャッシュに書き込み、非同期でDBに反映
- **メリット**: 書き込みが高速
- **デメリット**: 実装が複雑、データロスのリスク

### 3. キャッシュキーの設計

**基本ルール**:
```java
// ✅ 良い例
@Cacheable(value = "user", key = "#id")              // シンプル
@Cacheable(value = "users", key = "'all'")           // 固定キー
@Cacheable(value = "user", key = "#email")           // ユニークな値

// ❌ 悪い例
@Cacheable(value = "user", key = "#user")            // オブジェクト全体（hashCodeに依存）
@Cacheable(value = "data")                           // キー指定なし（全引数が対象）
```

**複合キー**:
```java
@Cacheable(value = "userPosts", key = "#userId + '-' + #postType")
public List<Post> getUserPostsByType(Long userId, String postType) {...}
```

### 4. キャッシュのTTL設定

**推奨設定**:
```java
// マスターデータ: 長め
Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.HOURS)

// ユーザーデータ: 中程度
Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES)

// リアルタイム性が必要: 短め
Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS)
```

### 5. キャッシュ無効化のタイミング

**即時無効化**:
```java
@CacheEvict(value = "user", key = "#id")
public void deleteUser(Long id) {
    userRepository.deleteById(id);
}
```

**バッチ無効化**:
```java
@CacheEvict(value = "users", allEntries = true)
public void importUsers(List<User> users) {
    userRepository.saveAll(users);
}
```

**スケジュール無効化**:
```java
@Scheduled(cron = "0 0 2 * * *")  // 毎日午前2時
@CacheEvict(value = "users", allEntries = true)
public void evictUsersCache() {
    log.info("Users cache evicted");
}
```

### 6. 分散環境でのキャッシュ

**単一サーバー**: Caffeine（インメモリ）でOK

**複数サーバー**: Redis、Memcachedなどの分散キャッシュが必要

```yaml
# Redis使用例
spring:
  cache:
    type: redis
  data:
    redis:
      host: localhost
      port: 6379
```

**メリット**:
- サーバー間でキャッシュを共有
- スケールアウトに対応

**デメリット**:
- ネットワークオーバーヘッド
- Redisサーバーの管理が必要

### 7. キャッシュのモニタリング

**重要な指標**:
- **ヒット率**: 80%以上が目標
- **キャッシュサイズ**: メモリ使用量を監視
- **エビクション数**: 頻繁ならサイズ不足

**ログ出力**:
```java
@Scheduled(fixedRate = 60000)  // 1分ごと
public void logCacheStats() {
    cacheManager.getCacheNames().forEach(cacheName -> {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache instanceof CaffeineCache) {
            com.github.benmanes.caffeine.cache.stats.CacheStats stats = 
                ((CaffeineCache) cache).getNativeCache().stats();
            log.info("Cache [{}] - Hit Rate: {}, Size: {}", 
                cacheName, stats.hitRate(), cache.estimatedSize());
        }
    });
}
```

---

## ➡️ 次のステップ

[Step 33: 非同期処理](STEP_33.md)へ進みましょう！

次のステップでは、`@Async`アノテーションを使った非同期処理の実装方法を学びます。重い処理をバックグラウンドで実行し、レスポンス速度を向上させる方法をマスターしましょう。
