# Step 20: ロギング

## 🎯 このステップの目標

- ログレベル（TRACE、DEBUG、INFO、WARN、ERROR）を理解する
- `@Slf4j`を使ってログ出力を実装できる
- `application.yml`でログ設定をカスタマイズできる
- `logback-spring.xml`で詳細なログ設定ができる
- 環境別（開発/本番）のログ設定を分けられる

**所要時間**: 約1時間

---

## 📋 事前準備

このステップを始める前に、以下を確認してください：

- Step 19（DTOとEntityの分離）が完了していること
- Lombokを使った開発経験があること
- アプリケーションの監視やデバッグの必要性を理解していること

---

## 📝 概要
ログは、アプリケーションの動作を追跡し、問題を診断するための重要な手段です。Spring Bootでは、デフォルトで**SLF4J + Logback**が使われます。

## 📚 ログレベルの理解

| レベル | 用途 | 例 |
|---|---|---|
| **TRACE** | 最も詳細な情報（通常は使わない） | メソッドの入出力、ループの各反復 |
| **DEBUG** | デバッグ情報 | SQLクエリ、内部状態の確認 |
| **INFO** | 重要な処理の記録 | アプリ起動、リクエスト処理、重要な処理の開始/完了 |
| **WARN** | 警告（異常ではないが注意が必要） | 非推奨APIの使用、リトライ処理 |
| **ERROR** | エラー（処理は続行可能） | 例外のキャッチ、想定外の入力 |
| **FATAL** | 致命的エラー（Logbackでは使わない） | - |

## 🔧 基本的な使い方

### 1. Lombokの`@Slf4j`を使う（推奨）

```java
package com.example.hellospringboot.service;

import com.example.hellospringboot.dto.request.UserCreateRequest;
import com.example.hellospringboot.dto.response.UserResponse;
import com.example.hellospringboot.entity.User;
import com.example.hellospringboot.exception.ResourceNotFoundException;
import com.example.hellospringboot.mapper.UserMapper;
import com.example.hellospringboot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j  // ⭐ ログ機能を追加
@Transactional(readOnly = true)
public class UserService {
    
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    
    public List<UserResponse> findAll() {
        log.info("全ユーザー取得を開始");
        List<User> users = userRepository.findAll();
        log.info("{}件のユーザーを取得しました", users.size());
        
        return users.stream()
            .map(userMapper::toResponse)
            .toList();
    }
    
    public UserResponse findById(Long id) {
        log.debug("ユーザー取得: id={}", id);
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("ユーザーが見つかりません: id={}", id);
                return new ResourceNotFoundException("User", "id", id);
            });
        
        log.debug("ユーザーを取得しました: {}", user.getName());
        return userMapper.toResponse(user);
    }
    
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        log.info("ユーザー作成を開始: email={}", request.getEmail());
        
        try {
            User user = userMapper.toEntity(request);
            User saved = userRepository.save(user);
            
            log.info("ユーザーを作成しました: id={}, name={}", saved.getId(), saved.getName());
            return userMapper.toResponse(saved);
            
        } catch (Exception e) {
            log.error("ユーザー作成に失敗しました: email={}", request.getEmail(), e);
            throw e;
        }
    }
    
    @Transactional
    public void delete(Long id) {
        log.info("ユーザー削除を開始: id={}", id);
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        userRepository.delete(user);
        log.info("ユーザーを削除しました: id={}, name={}", id, user.getName());
    }
}
```

### 2. SLF4Jを直接使う

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    
    public void someMethod() {
        log.info("処理を開始します");
    }
}
```

## 🎨 ログ出力のベストプラクティス

### 1. プレースホルダーを使う（パフォーマンス向上）

```java
// ❌ 非効率（文字列連結が毎回実行される）
log.debug("User: " + user.getName() + ", Age: " + user.getAge());

// ✅ 推奨（DEBUGレベルが無効なら文字列連結されない）
log.debug("User: {}, Age: {}", user.getName(), user.getAge());
```

### 2. 例外のログ出力

```java
try {
    userRepository.save(user);
} catch (Exception e) {
    // ✅ スタックトレースを含める
    log.error("ユーザー保存に失敗しました: userId={}", user.getId(), e);
    throw e;
}
```

### 3. 個人情報の保護

```java
// ❌ パスワードをログに出力
log.info("User created: {}", user);  // user.toString()にパスワード含む

// ✅ 必要な情報のみ
log.info("User created: id={}, email={}", user.getId(), user.getEmail());
```

### 4. 条件付きログ

```java
// ❌ 重い処理が毎回実行される
log.debug("Result: " + expensiveOperation());

// ✅ DEBUGレベルが有効な場合のみ実行
if (log.isDebugEnabled()) {
    log.debug("Result: {}", expensiveOperation());
}
```

## ⚙️ application.yml でのログ設定

### 基本設定

```yaml
# application.yml
logging:
  level:
    root: INFO                                    # デフォルト
    com.example.hellospringboot: DEBUG                       # 自分のパッケージはDEBUG
    com.example.hellospringboot.controller: INFO             # Controller層はINFO
    com.example.hellospringboot.repository: DEBUG            # Repository層はDEBUG
    org.springframework.web: DEBUG                # Spring WebのDEBUG情報
    org.hibernate.SQL: DEBUG                      # SQL出力
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE  # バインド変数の値

  # ログファイル出力
  file:
    name: logs/application.log                    # ログファイル名
    max-size: 10MB                                # ファイルサイズ上限
    max-history: 30                               # 保持日数

  # コンソール出力のパターン
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 環境別設定

```yaml
# application-dev.yml（開発環境）
logging:
  level:
    com.example.hellospringboot: DEBUG
    org.hibernate.SQL: DEBUG
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"

# application-prod.yml（本番環境）
logging:
  level:
    com.example.hellospringboot: INFO
    org.hibernate.SQL: WARN
  file:
    name: /var/log/myapp/application.log
```

## 📄 logback-spring.xml でのカスタマイズ

より詳細な設定が必要な場合は、`src/main/resources/logback-spring.xml`を作成します。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- コンソール出力 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- ファイル出力（全レベル） -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/application.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- 日次でローテーション -->
            <fileNamePattern>logs/application-%d{yyyy-MM-dd}.log</fileNamePattern>
            <!-- 30日間保持 -->
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>

    <!-- エラーログのみ別ファイル -->
    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/error.log</file>
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>ERROR</level>
        </filter>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/error-%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>90</maxHistory>
        </rollingPolicy>
    </appender>

    <!-- 開発環境のみ適用 -->
    <springProfile name="dev">
        <root level="DEBUG">
            <appender-ref ref="CONSOLE"/>
            <appender-ref ref="FILE"/>
        </root>
    </springProfile>

    <!-- 本番環境 -->
    <springProfile name="prod">
        <root level="INFO">
            <appender-ref ref="FILE"/>
            <appender-ref ref="ERROR_FILE"/>
        </root>
    </springProfile>

    <!-- パッケージ別のログレベル -->
    <logger name="com.example.hellospringboot" level="DEBUG"/>
    <logger name="org.springframework.web" level="INFO"/>
    <logger name="org.hibernate.SQL" level="DEBUG"/>
</configuration>
```

## 🔍 実践例: リクエスト/レスポンスのロギング

### 1. Interceptorでリクエストログ

```java
package com.example.hellospringboot.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class LoggingInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.info("==> {} {}", request.getMethod(), request.getRequestURI());
        log.debug("Query String: {}", request.getQueryString());
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        log.info("<== {} {} - Status: {}", 
            request.getMethod(), 
            request.getRequestURI(), 
            response.getStatus());
        
        if (ex != null) {
            log.error("リクエスト処理中にエラーが発生しました", ex);
        }
    }
}
```

### 2. Interceptorの登録

```java
package com.example.hellospringboot.config;

import com.example.hellospringboot.interceptor.LoggingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    
    private final LoggingInterceptor loggingInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor)
            .addPathPatterns("/api/**");  // /api/配下のみ
    }
}
```

### 3. 実行時間の計測

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    public List<UserResponse> findAll() {
        long startTime = System.currentTimeMillis();
        
        List<User> users = userRepository.findAll();
        List<UserResponse> responses = users.stream()
            .map(userMapper::toResponse)
            .toList();
        
        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("全ユーザー取得完了: {}件, 処理時間: {}ms", responses.size(), elapsedTime);
        
        return responses;
    }
}
```

### 4. AOPでメソッド実行ログ

```java
package com.example.hellospringboot.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {
    
    @Around("execution(* com.example.hellospringboot.service.*.*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();
        
        log.debug(">>> {}", methodName);
        
        try {
            Object result = joinPoint.proceed();
            long elapsedTime = System.currentTimeMillis() - start;
            log.debug("<<< {} - {}ms", methodName, elapsedTime);
            return result;
        } catch (Exception e) {
            log.error("!!! {} - エラー発生", methodName, e);
            throw e;
        }
    }
}
```

**依存関係**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

## 📊 ログ出力例

### コンソール出力

```
2024-01-15 10:30:00.123 [http-nio-8080-exec-1] INFO  c.e.d.controller.UserController - ==> GET /api/users
2024-01-15 10:30:00.125 [http-nio-8080-exec-1] DEBUG c.e.d.service.UserService - >>> UserService.findAll()
2024-01-15 10:30:00.150 [http-nio-8080-exec-1] DEBUG o.h.SQL - select user0_.id as id1_0_, user0_.name as name2_0_ from users user0_
2024-01-15 10:30:00.180 [http-nio-8080-exec-1] INFO  c.e.d.service.UserService - 全ユーザー取得完了: 10件, 処理時間: 55ms
2024-01-15 10:30:00.181 [http-nio-8080-exec-1] DEBUG c.e.d.service.UserService - <<< UserService.findAll() - 56ms
2024-01-15 10:30:00.185 [http-nio-8080-exec-1] INFO  c.e.d.controller.UserController - <== GET /api/users - Status: 200
```

## ✅ 動作確認

### 1. アプリケーション起動

```bash
./mvnw spring-boot:run
```

### 2. APIリクエスト

```bash
curl http://localhost:8080/api/users
```

### 3. ログファイルの確認

```bash
# 全ログ
tail -f logs/application.log

# エラーログのみ
tail -f logs/error.log

# 特定の文字列を含むログ
grep "UserService" logs/application.log
```

## 🎨 チャレンジ課題

### 課題1: 構造化ログ（JSON形式）

ログ解析ツール（ELK Stack、Splunkなど）で処理しやすいJSON形式のログ。

```xml
<dependency>
    <groupId>ch.qos.logback.contrib</groupId>
    <artifactId>logback-json-classic</artifactId>
    <version>0.1.5</version>
</dependency>
<dependency>
    <groupId>ch.qos.logback.contrib</groupId>
    <artifactId>logback-jackson</artifactId>
    <version>0.1.5</version>
</dependency>
```

```xml
<!-- logback-spring.xml -->
<appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/application.json</file>
    <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
        <layout class="ch.qos.logback.contrib.json.classic.JsonLayout">
            <timestampFormat>yyyy-MM-dd'T'HH:mm:ss.SSS</timestampFormat>
        </layout>
    </encoder>
</appender>
```

### 課題2: MDC（Mapped Diagnostic Context）でリクエストIDを追加

```java
@Component
@Slf4j
public class LoggingInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        log.info("Request started");
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        log.info("Request completed");
        MDC.clear();
    }
}
```

```xml
<!-- logback-spring.xml -->
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{requestId}] %-5level %logger{36} - %msg%n</pattern>
```

### 課題3: Slack通知

エラー発生時にSlackに通知。

```java
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {
    
    private final SlackNotifier slackNotifier;
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("予期しないエラーが発生しました", ex);
        
        // Slackに通知
        slackNotifier.sendError(
            "予期しないエラー",
            ex.getMessage(),
            ExceptionUtils.getStackTrace(ex)
        );
        
        return ResponseEntity.status(500).body(new ErrorResponse("Internal Server Error"));
    }
}
```

---

## 📚 このステップで学んだこと

- ✅ ログレベル（TRACE、DEBUG、INFO、WARN、ERROR）の使い分け
- ✅ `@Slf4j`を使った簡潔なログ実装
- ✅ プレースホルダー`{}`によるパフォーマンス向上
- ✅ 例外のスタックトレースの記録方法
- ✅ `application.yml`でのログ設定（レベル、ファイル出力）
- ✅ `logback-spring.xml`での詳細なカスタマイズ
- ✅ 環境別のログ設定（開発/本番）
- ✅ Interceptorを使ったリクエスト/レスポンスログ
- ✅ AOPによるメソッド実行時間の計測
- ✅ MDC（Mapped Diagnostic Context）でリクエストIDの追加

**ロギングのベストプラクティス**:
- 個人情報（パスワード等）をログに出力しない
- プレースホルダーを使って効率的にログ出力
- 環境に応じてログレベルを調整
- ファイルローテーションで容量を管理
- 本番環境では構造化ログ（JSON）を検討

---

## 🐛 トラブルシューティング

### エラー: ログが出力されない

**原因**: ログレベルが高すぎる、またはロガー名が間違っている

**解決策**:
```yaml
# application.yml
logging:
  level:
    # ✅ パッケージ名を正確に指定
    com.example.hellospringboot: DEBUG  # プロジェクトのパッケージ
    root: INFO  # デフォルトレベル
```

```java
// ✅ @Slf4jを使う（推奨）
@Slf4j
@Service
public class UserService {
    public void method() {
        log.debug("Debug message");  // logging.level.com.example.hellospringboot=DEBUG以上で出力
    }
}
```

### エラー: "SLF4J: Failed to load class org.slf4j.impl.StaticLoggerBinder"

**原因**: SLF4Jの実装（Logback）が見つからない

**解決策**:
Spring Boot Starterを使っていれば自動的に含まれています。手動で追加した場合：
```xml
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
</dependency>
```

### 問題: ログファイルが肥大化する

**原因**: ログローテーションが設定されていない

**解決策**:
```yaml
# application.yml
logging:
  file:
    name: logs/app.log
  logback:
    rollingpolicy:
      max-file-size: 10MB  # ファイルサイズが10MBを超えたらローテーション
      max-history: 30  # 30日分保持
      total-size-cap: 1GB  # 合計1GBまで
```

### 問題: パスワードやトークンがログに出力されてしまう

**原因**: オブジェクト全体をログ出力している

**解決策**:
```java
// ❌ NG: オブジェクトをそのまま出力
log.info("User created: {}", user);  // パスワードも出力される

// ✅ OK: 必要な情報だけ出力
log.info("User created: id={}, name={}", user.getId(), user.getName());

// ✅ OK: DTOにtoString()をカスタマイズ（Lombokの場合）
@ToString(exclude = {"password"})  // パスワードをtoString()から除外
public class User {
    private String name;
    private String password;
}
```

### 問題: 本番環境でDEBUGログが出すぎる

**原因**: 環境別の設定がされていない

**解決策**:
```yaml
# application-dev.yml（開発環境）
logging:
  level:
    com.example.hellospringboot: DEBUG

# application-prod.yml（本番環境）
logging:
  level:
    com.example.hellospringboot: INFO  # 本番はINFO以上
    root: WARN
```

### 問題: どのログレベルを使うべきかわからない

**ログレベルの使い分け**:

| レベル | 用途 | 例 |
|--------|------|-----|
| **ERROR** | エラー発生、処理継続不可 | `log.error("Failed to save user", e)` |
| **WARN** | 警告、処理は継続可能 | `log.warn("Deprecated API called")` |
| **INFO** | 重要な処理の記録 | `log.info("User registered: id={}", userId)` |
| **DEBUG** | 開発時のデバッグ情報 | `log.debug("Query result: {}", result)` |
| **TRACE** | 非常に詳細な情報 | `log.trace("Entering method with params: {}", params)` |

**迷ったら**:
- 本番環境で必要な情報 → INFO
- 開発時のみ必要 → DEBUG
- エラー → ERROR（例外も一緒にログ出力）

---

## 🔄 Gitへのコミットとレビュー依頼

Phase 4の学習が完了しました！進捗を記録してレビューを受けましょう：

```bash
# 変更をステージング
git add .

# コミット
git commit -m "Step 20: ロギング完了 - Phase 4完了"

# リモートにプッシュ
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

おめでとうございます！🎉 **Phase 4: アーキテクチャとベストプラクティス**が完了しました！

レビューが完了したら、**[Phase 5: Thymeleafでサーバーサイドレンダリング](../phase5/STEP_21.md)** へ進みましょう！

Thymeleafテンプレートエンジンを使って、サーバーサイドでHTMLをレンダリングする方法を学びます。
