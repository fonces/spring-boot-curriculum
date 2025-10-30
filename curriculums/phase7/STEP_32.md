# Step 32: キャッシング

## 🎯 このステップの目標

- Spring Cacheの仕組みを理解する
- `@Cacheable`、`@CacheEvict`、`@CachePut`を使う
- キャッシュ戦略を学ぶ
- パフォーマンスを測定する

**所要時間**: 約2時間


---


## 📋 事前準備

- Step 31が完了していること
- ロギングの基本概念を理解していること（Phase 4 STEP_20参照）

---
---

## 💡 キャッシングの重要性

### メリット

- ⚡ レスポンス時間の短縮
- 💰 データベース負荷の軽減
- 📈 スケーラビリティの向上
- 💪 システムの安定性向上

---

## 🚀 ステップ1: Redisのセットアップ

### 1-1. docker-compose.ymlにRedisを追加

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: spring-boot-mysql
    environment:
      MYSQL_ROOT_PASSWORD: rootpass
      MYSQL_DATABASE: springbootdb
      MYSQL_USER: springuser
      MYSQL_PASSWORD: springpass
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql

  redis:
    image: redis:7-alpine
    container_name: spring-boot-redis
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - redis-data:/data

volumes:
  mysql-data:
  redis-data:
```

### 1-2. Redisコンテナの起動

```bash
docker-compose up -d redis
```

---

## 🚀 ステップ2: 依存関係の追加

### 2-1. pom.xmlの更新

```xml
<!-- Spring Cache -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>

<!-- Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

---

## 🚀 ステップ3: Redis設定

### 3-1. application.ymlの設定

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password:  # パスワードなし
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0

  cache:
    type: redis
    redis:
      time-to-live: 600000  # 10分（ミリ秒）
      cache-null-values: false
```

---

## 🚀 ステップ4: キャッシュ設定

### 4-1. CacheConfig

**ファイルパス**: `src/main/java/com/example/hellospringboot/config/CacheConfig.java`

```java
package com.example.hellospringboot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * キャッシュ設定
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // ObjectMapperの設定
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // デフォルト設定
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer(objectMapper)))
                .disableCachingNullValues();

        // キャッシュごとの個別設定
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // usersキャッシュ: 5分
        cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // postsキャッシュ: 15分
        cacheConfigurations.put("posts", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        
        // statisticsキャッシュ: 1時間
        cacheConfigurations.put("statistics", defaultConfig.entryTtl(Duration.ofHours(1)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
```

---

## 🚀 ステップ5: キャッシュアノテーションの使用

### 5-1. UserServiceにキャッシュを追加

```java
package com.example.hellospringboot.service;

import com.example.hellospringboot.dto.response.UserResponse;
import com.example.hellospringboot.entity.User;
import com.example.hellospringboot.exception.UserNotFoundException;
import com.example.hellospringboot.mapper.UserMapper;
import com.example.hellospringboot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * ユーザー取得（キャッシュあり）
     */
    @Cacheable(value = "users", key = "#id")
    public UserResponse getUserById(Long id) {
        log.info("データベースからユーザーを取得します: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return userMapper.toResponse(user);
    }

    /**
     * 全ユーザー取得（キャッシュあり）
     */
    @Cacheable(value = "users", key = "'all'")
    public List<UserResponse> getAllUsers() {
        log.info("データベースから全ユーザーを取得します");
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    /**
     * ユーザー作成（キャッシュクリア）
     */
    @Transactional
    @CacheEvict(value = "users", key = "'all'")
    public UserResponse createUser(UserCreateRequest request) {
        log.info("ユーザーを作成します: {}", request.getEmail());
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("メールアドレス", request.getEmail());
        }

        User user = userMapper.toEntity(request);
        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    /**
     * ユーザー更新（キャッシュ更新）
     */
    @Transactional
    @CachePut(value = "users", key = "#id")
    @CacheEvict(value = "users", key = "'all'")
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        log.info("ユーザーを更新します: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setAge(request.getAge());

        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }

    /**
     * ユーザー削除（キャッシュクリア）
     */
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public void deleteUser(Long id) {
        log.info("ユーザーを削除します: {}", id);
        
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        
        userRepository.deleteById(id);
    }
}
```

---

## 🚀 ステップ6: キャッシュアノテーションの種類

| アノテーション | 用途 | 説明 |
|------------|------|------|
| **@Cacheable** | 読み取り | キャッシュから取得、なければDBから取得してキャッシュ |
| **@CachePut** | 更新 | 常にDBから取得してキャッシュを更新 |
| **@CacheEvict** | 削除 | キャッシュをクリア |
| **@Caching** | 複合 | 複数のキャッシュ操作を組み合わせ |

---

## 🚀 ステップ7: RedisTemplate（手動キャッシュ）

### 7-1. RedisService

**ファイルパス**: `src/main/java/com/example/hellospringboot/service/RedisService.java`

```java
package com.example.hellospringboot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 値を保存
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 値を保存（有効期限付き）
     */
    public void set(String key, Object value, Duration timeout) {
        redisTemplate.opsForValue().set(key, value, timeout);
    }

    /**
     * 値を取得
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 値を削除
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * キーが存在するかチェック
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 有効期限を設定
     */
    public void expire(String key, Duration timeout) {
        redisTemplate.expire(key, timeout);
    }
}
```

---

## ✅ 動作確認

### キャッシュの確認

```bash
# ユーザー取得（1回目: DBアクセス）
curl http://localhost:8080/api/users/1

# ユーザー取得（2回目: キャッシュから）
curl http://localhost:8080/api/users/1

# ログを確認
# 1回目: "データベースからユーザーを取得します: 1"
# 2回目: ログなし（キャッシュから取得）
```

### Redis CLIで確認

```bash
docker exec -it spring-boot-redis redis-cli

# キャッシュキー一覧
KEYS *

# 特定のキャッシュ確認
GET users::1

# キャッシュクリア
FLUSHDB
```

---

## 🔧 補足: MyBatisでのキャッシング

Phase 3でMyBatisを学習した場合、MyBatis Mapperでもキャッシュを使用できます。

### MyBatis Mapperでのキャッシュ

**UserMapper（MyBatis版）**:
```java
@Mapper
public interface UserMapper {
    
    @Select("SELECT * FROM users WHERE id = #{id}")
    Optional<User> findById(Long id);
    
    @Select("SELECT * FROM users")
    List<User> findAll();
    
    @Insert("INSERT INTO users (name, email, age, created_at, updated_at) " +
            "VALUES (#{name}, #{email}, #{age}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);
    
    @Update("UPDATE users SET name = #{name}, email = #{email}, age = #{age}, updated_at = NOW() " +
            "WHERE id = #{id}")
    void update(User user);
    
    @Delete("DELETE FROM users WHERE id = #{id}")
    void deleteById(Long id);
}
```

### MyBatis使用時のService層キャッシング

```java
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserMapper userMapper;  // MyBatis Mapper
    private final UserDtoMapper dtoMapper;  // DTO変換用

    /**
     * ユーザー取得（キャッシュあり）
     */
    @Cacheable(value = "users", key = "#id")
    public UserResponse getUserById(Long id) {
        log.info("データベースからユーザーを取得します: {}", id);
        User user = userMapper.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return dtoMapper.toResponse(user);
    }

    /**
     * 全ユーザー取得（キャッシュあり）
     */
    @Cacheable(value = "users", key = "'all'")
    public List<UserResponse> getAllUsers() {
        log.info("データベースから全ユーザーを取得します");
        return userMapper.findAll().stream()
                .map(dtoMapper::toResponse)
                .toList();
    }

    /**
     * ユーザー作成（キャッシュクリア）
     */
    @Transactional
    @CacheEvict(value = "users", key = "'all'")
    public UserResponse createUser(UserCreateRequest request) {
        log.info("ユーザーを作成します: {}", request.getEmail());
        
        User user = dtoMapper.toEntity(request);
        userMapper.insert(user);
        return dtoMapper.toResponse(user);
    }

    /**
     * ユーザー更新（キャッシュ更新）
     */
    @Transactional
    @CachePut(value = "users", key = "#id")
    @CacheEvict(value = "users", key = "'all'")
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        log.info("ユーザーを更新します: {}", id);
        
        User user = userMapper.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setAge(request.getAge());

        userMapper.update(user);
        return dtoMapper.toResponse(user);
    }

    /**
     * ユーザー削除（キャッシュクリア）
     */
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public void deleteUser(Long id) {
        log.info("ユーザーを削除します: {}", id);
        userMapper.deleteById(id);
    }
}
```

### MyBatis独自のキャッシュ機能

MyBatisには**Second Level Cache**という独自のキャッシュ機能もあります。

**Mapper XMLでのキャッシュ設定**:
```xml
<mapper namespace="com.example.hellospringboot.mapper.UserMapper">
    <!-- Second Level Cache有効化 -->
    <cache eviction="LRU" flushInterval="60000" size="512" readOnly="true"/>
    
    <select id="findById" resultType="User">
        SELECT * FROM users WHERE id = #{id}
    </select>
</mapper>
```

### Spring Cache vs MyBatis Cache

| 観点 | Spring Cache | MyBatis Second Level Cache |
|------|-------------|---------------------------|
| **スコープ** | アプリケーション全体 | Mapperごと |
| **柔軟性** | 高い（Redis等と連携） | 中程度 |
| **設定** | アノテーションベース | XMLベース |
| **推奨** | ✅ REST API、マイクロサービス | 単一アプリ、シンプルなケース |

> **💡 推奨**: モダンなSpring Bootアプリケーションでは、**Spring Cache + Redis**の組み合わせを推奨します。MyBatis Cacheは小規模アプリやレガシーシステムで検討してください。

---

## 🎨 チャレンジ課題

### チャレンジ 1: キャッシュ統計

CacheManagerを使ってキャッシュヒット率を表示してください。

### チャレンジ 2: 条件付きキャッシュ

`condition`や`unless`を使って条件付きキャッシングを実装してください。

### チャレンジ 3: 分散キャッシュ

複数のアプリケーションインスタンスでキャッシュを共有してください。

---

## 📚 このステップで学んだこと

- ✅ Spring Cacheの使い方
- ✅ Redisの導入
- ✅ キャッシュアノテーション
- ✅ RedisCacheManager設定
- ✅ RedisTemplateによる手動キャッシュ

---

## 🐛 トラブルシューティング

### エラー: "@Cacheable"が効かない

**原因**: `@EnableCaching`が付いていない、またはプロキシが機能していない

**解決策**:
```java
// ✅ メインクラスに@EnableCachingを追加
@SpringBootApplication
@EnableCaching  // ← これを追加
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}

// ❌ NG: 同じクラス内のメソッド呼び出しではキャッシュされない
@Service
public class UserService {
    @Cacheable("users")
    public User findById(Long id) { ... }
    
    public void someMethod() {
        User user = this.findById(1L);  // キャッシュされない
    }
}

// ✅ OK: 外部から呼び出す
@Service
@RequiredArgsConstructor
public class OrderService {
    private final UserService userService;
    
    public void someMethod() {
        User user = userService.findById(1L);  // キャッシュされる
    }
}
```

### エラー: "Cannot connect to Redis"

**原因**: Redisサーバーが起動していない、または接続設定が間違っている

**解決策**:
```bash
# Redisが起動しているか確認
docker ps | grep redis

# 起動していなければ起動
docker-compose up -d redis

# Redisに接続できるか確認
docker exec -it redis redis-cli ping
# PONGが返ればOK
```

```yaml
# application.yml の設定確認
spring:
  data:
    redis:
      host: localhost  # Dockerの場合は確認
      port: 6379
      password: # パスワード設定がある場合
```

### 問題: キャッシュが更新されない

**原因**: `@CacheEvict`または`@CachePut`を使っていない

**解決策**:
```java
@Service
public class UserService {
    @Cacheable(value = "users", key = "#id")
    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow();
    }
    
    // ✅ 更新時にキャッシュも更新
    @CachePut(value = "users", key = "#user.id")
    public User update(User user) {
        return userRepository.save(user);
    }
    
    // ✅ 削除時にキャッシュも削除
    @CacheEvict(value = "users", key = "#id")
    public void delete(Long id) {
        userRepository.deleteById(id);
    }
    
    // ✅ 全キャッシュをクリア
    @CacheEvict(value = "users", allEntries = true)
    public void deleteAll() {
        userRepository.deleteAll();
    }
}
```

### 問題: シリアライズエラー

**原因**: キャッシュするオブジェクトがシリアライズ可能でない

**解決策**:
```java
// ❌ NG: Serializableを実装していない
@Entity
public class User {
    private Long id;
    private String name;
}

// ✅ OK: Serializableを実装
@Entity
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
}
```

または、JSON シリアライザーを使用:
```java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()  // JSONシリアライザー
                )
            );
    }
}
```

### 問題: キャッシュキーの衝突

**原因**: 同じキーで異なるデータをキャッシュしている

**解決策**:
```java
// ❌ NG: 複数のメソッドで同じキャッシュ名とキーを使用
@Cacheable(value = "data", key = "#id")
public User findUserById(Long id) { ... }

@Cacheable(value = "data", key = "#id")
public Product findProductById(Long id) { ... }
// User(id=1) と Product(id=1) が衝突

// ✅ OK: 異なるキャッシュ名を使用
@Cacheable(value = "users", key = "#id")
public User findUserById(Long id) { ... }

@Cacheable(value = "products", key = "#id")
public Product findProductById(Long id) { ... }

// ✅ OK: プレフィックスを含める
@Cacheable(value = "data", key = "'user:' + #id")
public User findUserById(Long id) { ... }

@Cacheable(value = "data", key = "'product:' + #id")
public Product findProductById(Long id) { ... }
```

### 問題: メモリ不足

**原因**: TTLが設定されておらず、キャッシュが無限に増える

**解決策**:
```java
@Bean
public RedisCacheConfiguration cacheConfiguration() {
    return RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofHours(1))  // 1時間でキャッシュ削除
        .disableCachingNullValues();  // null値はキャッシュしない
}

// または、キャッシュごとにTTLを設定
@Bean
public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
    
    // usersキャッシュ: 1時間
    cacheConfigurations.put("users", 
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1)));
    
    // productsキャッシュ: 30分
    cacheConfigurations.put("products", 
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30)));
    
    return RedisCacheManager.builder(connectionFactory)
        .withInitialCacheConfigurations(cacheConfigurations)
        .build();
}
```

---

## 🔄 Gitへのコミットとレビュー依頼

```bash
git add .
git commit -m "Step 32: キャッシング完了"
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

次は[Step 33: 非同期処理](STEP_33.md)へ進みましょう！

`@Async`を使った非同期処理の実装方法を学びます。

---

お疲れさまでした！ 🎉

キャッシュ機能を習得しました！
