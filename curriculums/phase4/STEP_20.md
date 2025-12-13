# Step 20: ロギング

## 🎯 このステップの目標

- ロギングの重要性と目的を理解できる
- SLF4J + Logbackを使った適切なログ出力を実装できる
- ログレベル（TRACE, DEBUG, INFO, WARN, ERROR）を正しく使い分けできる
- 環境別（開発/本番）のログ設定を構成できる
- ログのフォーマットやローテーション設定をカスタマイズできる

**所要時間**: 約50分

---

## 📋 事前準備

- [Step 19: DTOとEntityの分離](STEP_19.md)が完了していること
- `application.yml`の基本的な使い方を理解していること
- Spring Bootのアプリケーションを起動・実行できること

---

## 📝 なぜロギングが重要なのか

### Before（System.out.printlnを使用）

```java
@Service
public class UserService {
    
    public User createUser(UserCreateRequest request) {
        System.out.println("Creating user: " + request.getName());  // ❌ 非推奨
        
        User user = userDtoMapper.toEntity(request);
        userRepository.save(user);
        
        System.out.println("User created successfully: " + user.getId());  // ❌ 非推奨
        return user;
    }
}
```

**問題点**:
- ❌ ログレベルがない（重要度が不明）
- ❌ 本番環境でもコンソールに出力される
- ❌ ログファイルに記録されない
- ❌ タイムスタンプやスレッド情報がない
- ❌ ログの集約・分析ができない
- ❌ パフォーマンスに影響（同期処理）

---

### After（SLF4Jを使用）

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserService {
    
    public User createUser(UserCreateRequest request) {
        log.info("Creating user with email: {}", request.getEmail());
        
        User user = userDtoMapper.toEntity(request);
        userRepository.save(user);
        
        log.info("User created successfully. ID: {}, Name: {}", user.getId(), user.getName());
        return user;
    }
}
```

**出力例**:
```
2025-01-15 15:00:00.123 INFO  [http-nio-8080-exec-1] c.e.h.services.UserService : Creating user with email: alice@example.com
2025-01-15 15:00:00.456 INFO  [http-nio-8080-exec-1] c.e.h.services.UserService : User created successfully. ID: 1, Name: Alice
```

**改善点**:
- ✅ タイムスタンプ、スレッド名、クラス名が自動記録
- ✅ ログレベル（INFO）で重要度を明示
- ✅ 環境ごとにログレベルを変更可能
- ✅ ログファイルに保存・ローテーション可能
- ✅ プレースホルダー（`{}`）でパフォーマンス向上
- ✅ 外部ツールで集約・分析可能

---

## 🚀 ステップ1: SLF4Jの基本

### 1-1. 依存関係の確認

Spring Bootには`spring-boot-starter-logging`が標準で含まれています（`spring-boot-starter-web`に内包）。

**pom.xml**（確認のみ、追加不要）:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <!-- この中にSLF4J + Logbackが含まれている -->
</dependency>
```

---

### 1-2. Lombokの`@Slf4j`アノテーション

Lombokを使うと、ロガーのインスタンス生成が不要になります。

**手動でロガーを作成（冗長）**:
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    
    public void createUser(UserCreateRequest request) {
        log.info("Creating user: {}", request.getName());
    }
}
```

**Lombokで簡潔に**:
```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserService {
    // log変数が自動生成される
    
    public void createUser(UserCreateRequest request) {
        log.info("Creating user: {}", request.getName());
    }
}
```

---

## 🚀 ステップ2: ログレベルの使い分け

### 2-1. ログレベルの種類

| レベル | 用途 | 使用例 |
|---|---|---|
| **TRACE** | 最も詳細なデバッグ情報 | メソッドの入出力、ループの各反復 |
| **DEBUG** | 開発時のデバッグ情報 | SQL実行、変数の値、処理の流れ |
| **INFO** | 通常の動作情報 | サービス起動、ユーザー操作、重要な処理 |
| **WARN** | 警告（エラーではないが注意が必要） | 非推奨APIの使用、リトライ処理 |
| **ERROR** | エラー（処理は続行可能） | 例外発生、処理失敗 |

---

### 2-2. 実践例

以下のファイルを`src/main/java/com/example/hellospringboot/services/UserService.java`に修正します：

```java
package com.example.hellospringboot.services;

import com.example.hellospringboot.dto.UserCreateRequest;
import com.example.hellospringboot.dto.UserUpdateRequest;
import com.example.hellospringboot.entities.User;
import com.example.hellospringboot.exceptions.ResourceNotFoundException;
import com.example.hellospringboot.exceptions.ConflictException;
import com.example.hellospringboot.mappers.UserDtoMapper;
import com.example.hellospringboot.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserDtoMapper userDtoMapper;
    
    public List<User> getAllUsers() {
        log.debug("Fetching all users");
        List<User> users = userRepository.findAll();
        log.info("Retrieved {} users", users.size());
        return users;
    }
    
    public User getUserById(Long id) {
        log.debug("Fetching user with ID: {}", id);
        
        return userRepository.findById(id)
            .map(user -> {
                log.info("User found: ID={}, Name={}", user.getId(), user.getName());
                return user;
            })
            .orElseThrow(() -> {
                log.warn("User not found with ID: {}", id);
                return new ResourceNotFoundException("User", "id", id);
            });
    }
    
    public User createUser(UserCreateRequest request) {
        log.info("Creating new user with email: {}", request.getEmail());
        
        // メールアドレスの重複チェック
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Email already exists: {}", request.getEmail());
            throw new ConflictException("Email already exists: " + request.getEmail());
        }
        
        User user = userDtoMapper.toEntity(request);
        userRepository.save(user);
        
        log.info("User created successfully. ID: {}, Name: {}", user.getId(), user.getName());
        return user;
    }
    
    public User updateUser(Long id, UserUpdateRequest request) {
        log.info("Updating user with ID: {}", id);
        
        User existingUser = userRepository.findById(id)
            .orElseThrow(() -> {
                log.error("User not found for update. ID: {}", id);
                return new ResourceNotFoundException("User", "id", id);
            });
        
        existingUser.setName(request.getName());
        existingUser.setEmail(request.getEmail());
        existingUser.setAge(request.getAge());
        
        userRepository.save(existingUser);
        log.info("User updated successfully. ID: {}", id);
        return existingUser;
    }
    
    public void deleteUser(Long id) {
        log.info("Deleting user with ID: {}", id);
        
        if (!userRepository.existsById(id)) {
            log.error("User not found for deletion. ID: {}", id);
            throw new ResourceNotFoundException("User", "id", id);
        }
        
        userRepository.deleteById(id);
        log.info("User deleted successfully. ID: {}", id);
    }
}
```

---

### 2-3. 例外発生時のログ

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    
    public Product getProductById(Long id) {
        try {
            log.debug("Attempting to fetch product with ID: {}", id);
            
            Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
            
            log.info("Product found: {}", product.getName());
            return product;
            
        } catch (ResourceNotFoundException e) {
            log.error("Failed to fetch product. ID: {}", id, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while fetching product. ID: {}", id, e);
            throw e;
        }
    }
}
```

**ポイント**:
- `log.error("message", e)`: 例外のスタックトレースも記録
- `log.error("message: {}", value, e)`: プレースホルダーと例外を併用

---

## 🚀 ステップ3: ログ設定のカスタマイズ

### 3-1. application.ymlでログレベル設定

`src/main/resources/application.yml`に以下を追加します：

```yaml
logging:
  level:
    root: INFO                                      # デフォルトはINFO
    com.example.hellospringboot: DEBUG              # 自分のパッケージはDEBUG
    org.springframework.web: INFO                   # Spring WebはINFO
    org.hibernate.SQL: DEBUG                        # SQLログを出力（JPA使用時）
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE  # SQLパラメータ（JPA使用時）
    
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} %5p [%t] %-40.40logger{39} : %m%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} %5p [%t] %-40.40logger{39} : %m%n"
  
  file:
    name: logs/spring-boot-app.log
    max-size: 10MB
    max-history: 30
```

**設定の説明**:
- `logging.level.root`: すべてのロガーのデフォルトレベル
- `logging.level.com.example.hellospringboot`: 自分のアプリのログレベル
- `logging.pattern.console`: コンソール出力フォーマット
- `logging.pattern.file`: ファイル出力フォーマット
- `logging.file.name`: ログファイルのパス
- `logging.file.max-size`: ログファイルの最大サイズ
- `logging.file.max-history`: 保持する過去のログファイル数

---

### 3-2. 環境別のログ設定

**application-dev.yml**（開発環境）:
```yaml
logging:
  level:
    root: INFO
    com.example.hellospringboot: DEBUG  # 開発環境では詳細ログ
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
  
  file:
    name: logs/dev-app.log
```

**application-prod.yml**（本番環境）:
```yaml
logging:
  level:
    root: WARN                           # 本番環境では警告以上のみ
    com.example.hellospringboot: INFO    # 自分のアプリはINFO
    org.hibernate.SQL: WARN              # SQLログは出力しない
  
  file:
    name: /var/log/spring-boot-app.log
    max-size: 50MB
    max-history: 90
```

---

### 3-3. logback-spring.xmlでの詳細設定

より高度な設定が必要な場合は、`src/main/resources/logback-spring.xml`を作成します：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- コンソール出力 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %5p [%t] %-40.40logger{39} : %m%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>
    
    <!-- ファイル出力（ローテーション） -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/spring-boot-app.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- 日次ローテーション -->
            <fileNamePattern>logs/spring-boot-app-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxHistory>30</maxHistory>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>10MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %5p [%t] %-40.40logger{39} : %m%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>
    
    <!-- エラーログのみを別ファイルに出力 -->
    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/error.log</file>
        <filter class="ch.qos.logback.classic.filter.LevelFilter">
            <level>ERROR</level>
            <onMatch>ACCEPT</onMatch>
            <onMismatch>DENY</onMismatch>
        </filter>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/error-%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>90</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %5p [%t] %-40.40logger{39} : %m%n%ex{full}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>
    
    <!-- ロガー設定 -->
    <logger name="com.example.hellospringboot" level="DEBUG" />
    <logger name="org.springframework.web" level="INFO" />
    <logger name="org.hibernate.SQL" level="DEBUG" />
    
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="FILE" />
        <appender-ref ref="ERROR_FILE" />
    </root>
</configuration>
```

---

## 🚀 ステップ4: リクエスト/レスポンスのログ

### 4-1. リクエストログのインターセプター

以下のファイルを`src/main/java/com/example/hellospringboot/config/LoggingInterceptor.java`に作成します：

```java
package com.example.hellospringboot.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class LoggingInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long startTime = System.currentTimeMillis();
        request.setAttribute("startTime", startTime);
        
        log.info("Request Start: {} {}", request.getMethod(), request.getRequestURI());
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        long startTime = (Long) request.getAttribute("startTime");
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        log.info("Request End: {} {} - Status: {} - Duration: {}ms",
            request.getMethod(),
            request.getRequestURI(),
            response.getStatus(),
            duration
        );
        
        if (ex != null) {
            log.error("Request failed with exception", ex);
        }
    }
}
```

---

### 4-2. インターセプター登録

以下のファイルを`src/main/java/com/example/hellospringboot/config/WebMvcConfig.java`に作成します：

```java
package com.example.hellospringboot.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    
    private final LoggingInterceptor loggingInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor)
            .addPathPatterns("/api/**");  // /api/**のパスに適用
    }
}
```

---

## ✅ ステップ5: 動作確認

### 5-1. アプリケーション起動

```bash
cd workspace/hello-spring-boot
./mvnw spring-boot:run
```

---

### 5-2. ログ出力の確認

**ユーザー作成リクエスト**:

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice",
    "email": "alice@example.com",
    "age": 25
  }'
```

**期待されるログ出力**:

```
2025-01-15 15:30:00.123 INFO  [http-nio-8080-exec-1] c.e.h.config.LoggingInterceptor         : Request Start: POST /api/users
2025-01-15 15:30:00.125 INFO  [http-nio-8080-exec-1] c.e.h.services.UserService              : Creating new user with email: alice@example.com
2025-01-15 15:30:00.250 DEBUG [http-nio-8080-exec-1] org.hibernate.SQL                       : insert into users (name, email, age) values (?, ?, ?)
2025-01-15 15:30:00.255 INFO  [http-nio-8080-exec-1] c.e.h.services.UserService              : User created successfully. ID: 1, Name: Alice
2025-01-15 15:30:00.260 INFO  [http-nio-8080-exec-1] c.e.h.config.LoggingInterceptor         : Request End: POST /api/users - Status: 201 - Duration: 137ms
```

---

### 5-3. エラーログの確認

**存在しないユーザーを取得**:

```bash
curl http://localhost:8080/api/users/999
```

**期待されるログ出力**:

```
2025-01-15 15:32:00.100 INFO  [http-nio-8080-exec-2] c.e.h.config.LoggingInterceptor         : Request Start: GET /api/users/999
2025-01-15 15:32:00.102 DEBUG [http-nio-8080-exec-2] c.e.h.services.UserService              : Fetching user with ID: 999
2025-01-15 15:32:00.120 WARN  [http-nio-8080-exec-2] c.e.h.services.UserService              : User not found with ID: 999
2025-01-15 15:32:00.125 INFO  [http-nio-8080-exec-2] c.e.h.config.LoggingInterceptor         : Request End: GET /api/users/999 - Status: 404 - Duration: 25ms
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: 構造化ログ（JSON形式）

ログをJSON形式で出力し、外部ツール（Elasticsearch, Splunkなど）で解析しやすくしてください。

**ヒント**:

`pom.xml`に依存関係追加：
```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

`logback-spring.xml`:
```xml
<appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/app.json</file>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder" />
</appender>
```

---

### チャレンジ 2: MDC（Mapped Diagnostic Context）

リクエストIDを全ログに含めて、リクエストをトレースしやすくしてください。

**ヒント**:

```java
@Component
public class RequestIdInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        response.setHeader("X-Request-ID", requestId);
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MDC.clear();
    }
}
```

`logback-spring.xml`:
```xml
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %5p [%X{requestId}] [%t] %-40.40logger{39} : %m%n</pattern>
```

---

## 🐛 トラブルシューティング

### エラー 1: "Cannot resolve symbol 'log'"

**原因**: Lombokの`@Slf4j`が認識されていない

**解決策**: Lombokのアノテーション処理を有効化

---

### エラー 2: "ログファイルが作成されない"

**原因**: ディレクトリの書き込み権限がない

**解決策**: `logging.file.name`を書き込み可能なパスに変更

```yaml
logging:
  file:
    name: ./logs/app.log  # カレントディレクトリのlogsフォルダ
```

---

## 📚 このステップで学んだこと

- ✅ **SLF4J + Logback**: Spring Bootの標準ロギングフレームワーク
- ✅ **ログレベル**: TRACE, DEBUG, INFO, WARN, ERROR
- ✅ **`@Slf4j`**: Lombokによるロガー自動生成
- ✅ **ログ設定**: application.ymlとlogback-spring.xml
- ✅ **環境別設定**: 開発環境と本番環境でログレベルを切り替え
- ✅ **ログローテーション**: ファイルサイズ・日付によるローテーション
- ✅ **リクエストログ**: インターセプターでリクエスト/レスポンスを記録

---

## 💡 補足: ログのベストプラクティス

### 1. ログレベルの使い分け

```java
// ✅ 良い例
log.debug("User search query: {}", query);          // デバッグ情報
log.info("User logged in: {}", username);           // 通常の操作
log.warn("Deprecated API used: {}", apiName);       // 警告
log.error("Failed to send email", exception);       // エラー

// ❌ 悪い例
log.info("x = {}, y = {}, z = {}", x, y, z);        // 過剰な詳細（DEBUGにすべき）
log.error("User not found");                        // エラーでなくWARN
```

---

### 2. プレースホルダーを使う

```java
// ✅ 良い例
log.info("User created: {}", user.getName());

// ❌ 悪い例（文字列連結はパフォーマンスに影響）
log.info("User created: " + user.getName());
```

---

### 3. 例外をログに含める

```java
// ✅ 良い例
try {
    userService.createUser(request);
} catch (Exception e) {
    log.error("Failed to create user", e);  // スタックトレースも記録
    throw e;
}

// ❌ 悪い例（例外情報が失われる）
try {
    userService.createUser(request);
} catch (Exception e) {
    log.error("Failed to create user");
}
```

---

## 📖 参考資料

### 公式ドキュメント

- [Spring Boot - Logging](https://docs.spring.io/spring-boot/reference/features/logging.html)
- [Logback Documentation](https://logback.qos.ch/documentation.html)
- [SLF4J Manual](https://www.slf4j.org/manual.html)

---

## 🎉 Phase 4 完了！

おめでとうございます！Phase 4「アーキテクチャとベストプラクティス」を完了しました。

**このPhaseで学んだこと**:
- ✅ レイヤー化アーキテクチャ（Controller/Service/Repository）
- ✅ 依存性注入（DI）とIoCコンテナ
- ✅ 例外ハンドリング（`@ControllerAdvice`, カスタム例外）
- ✅ バリデーション（Jakarta Bean Validation, カスタムバリデーター）
- ✅ DTOとEntityの分離（MapStruct）
- ✅ ロギング（SLF4J + Logback）

---

## ➡️ 次のPhaseへ

[Phase 5: Thymeleafでサーバーサイドレンダリング](../phase5/STEP_21.md)へ進みましょう！

次のPhaseでは、Spring BootでWebページを生成するThymeleafテンプレートエンジンを学びます：

- Thymeleafの基本構文
- フォーム送信とバリデーション
- レイアウトとフラグメント
- REST APIとの連携

サーバーサイドレンダリングの基礎を習得し、フルスタックなWebアプリケーション開発に挑戦しましょう！
