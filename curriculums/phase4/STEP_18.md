# Step 18: ロギング

## 🎯 このステップの目標

- SLF4JとLogbackを使ったロギングを理解する
- ログレベルを使い分ける
- ログの出力形式をカスタマイズする
- ログをファイルに出力する
- 構造化ログ（JSON形式）を実装する

**所要時間**: 約1時間

---

## 📋 事前準備

- Step 15までの例外ハンドリングが理解できていること
- `@Slf4j`アノテーションの基本的な使い方を知っていること

**Step 15をまだ完了していない場合**: [Step 15: 例外ハンドリング](STEP_15.md)を先に進めてください。

---

## 💡 ロギングとは？

### ロギングの重要性

**ログなしの場合**:
- ❌ 本番環境で何が起きているかわからない
- ❌ バグの原因特定に時間がかかる
- ❌ パフォーマンス問題を検出できない

**ログありの場合**:
- ✅ 問題の早期発見
- ✅ デバッグが容易
- ✅ 監視・アラート設定が可能
- ✅ 監査証跡

### ログレベル

| レベル | 用途 | 例 |
|--------|------|-----|
| **ERROR** | エラー・致命的問題 | データベース接続失敗 |
| **WARN** | 警告・想定内の異常 | ユーザーが見つからない |
| **INFO** | 重要な情報 | アプリ起動、ユーザー作成 |
| **DEBUG** | デバッグ情報 | メソッドの引数・戻り値 |
| **TRACE** | 詳細なトレース | SQL文の詳細 |

---

## 🚀 ステップ1: Lombokの@Slf4jを使ったログ出力

### 1-1. 基本的なログ出力

**既存のUserService**（Step 17で作成済み）:
```java
@Service
@RequiredArgsConstructor
@Slf4j  // ← これでloggerが使える
public class UserService {

    public UserResponse createUser(UserCreateRequest request) {
        log.info("Creating user with email: {}", request.getEmail());
        
        // ... 処理 ...
        
        log.info("User created successfully with ID: {}", savedUser.getId());
        return userMapper.toResponse(savedUser);
    }
}
```

### 1-2. ログレベルの使い分け

```java
@Service
@Slf4j
public class UserService {

    public UserResponse createUser(UserCreateRequest request) {
        // デバッグ情報（開発時のみ）
        log.debug("createUser called with request: {}", request);
        
        // 重要な情報
        log.info("Creating user with email: {}", request.getEmail());
        
        // 警告
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Duplicate email detected: {}", request.getEmail());
            throw new DuplicateResourceException(...);
        }
        
        try {
            // ... 処理 ...
        } catch (Exception ex) {
            // エラー
            log.error("Failed to create user: {}", request.getEmail(), ex);
            throw ex;
        }
        
        return response;
    }
}
```

---

## 🚀 ステップ2: application.ymlでログレベル設定

### 2-1. ログレベルの設定

**ファイルパス**: `src/main/resources/application.yml`

```yaml
spring:
  application:
    name: hello-spring-boot

# ログ設定
logging:
  level:
    # ルートロガー
    root: INFO
    
    # 自分のパッケージ
    com.example.hellospringboot: DEBUG
    
    # Hibernate SQL
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
    
    # Spring Framework
    org.springframework.web: DEBUG
    
  # ログパターン
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    
  # ファイル出力
  file:
    name: logs/application.log
    max-size: 10MB
    max-history: 30
```

### 2-2. パターンの説明

| パターン | 説明 | 例 |
|----------|------|-----|
| `%d{...}` | 日時 | `2025-10-27 10:30:00` |
| `%thread` | スレッド名 | `http-nio-8080-exec-1` |
| `%-5level` | ログレベル（5文字幅） | `INFO ` |
| `%logger{36}` | ロガー名（最大36文字） | `c.e.h.service.UserService` |
| `%msg` | ログメッセージ | `User created successfully` |
| `%n` | 改行 | |
| `%ex` | 例外スタックトレース | |

---

## 🚀 ステップ3: Logbackの詳細設定

### 3-1. logback-spring.xmlの作成

**ファイルパス**: `src/main/resources/logback-spring.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- プロパティ定義 -->
    <property name="LOG_PATTERN" 
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"/>
    <property name="LOG_FILE" value="logs/application.log"/>

    <!-- コンソール出力 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- ファイル出力 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_FILE}</file>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <!-- ローテーション設定 -->
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/application-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>10MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>1GB</totalSizeCap>
        </rollingPolicy>
    </appender>

    <!-- エラーログ専用ファイル -->
    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/error.log</file>
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>ERROR</level>
        </filter>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/error-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>10MB</maxFileSize>
            <maxHistory>90</maxHistory>
        </rollingPolicy>
    </appender>

    <!-- パッケージ別のログレベル -->
    <logger name="com.example.hellospringboot" level="DEBUG"/>
    <logger name="org.hibernate.SQL" level="DEBUG"/>
    <logger name="org.springframework.web" level="INFO"/>

    <!-- ルートロガー -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
        <appender-ref ref="ERROR_FILE"/>
    </root>

    <!-- プロファイル別設定 -->
    <springProfile name="dev">
        <logger name="com.example.hellospringboot" level="DEBUG"/>
    </springProfile>

    <springProfile name="prod">
        <logger name="com.example.hellospringboot" level="INFO"/>
    </springProfile>
</configuration>
```

---

## 🚀 ステップ4: リクエスト/レスポンスログ

### 4-1. ログフィルターの作成

**ファイルパス**: `src/main/java/com/example/hellospringboot/filter/RequestLoggingFilter.java`

```java
package com.example.hellospringboot.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * HTTPリクエスト/レスポンスをログ出力するフィルター
 */
@Component
@Slf4j
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // リクエストの開始時刻
        long startTime = System.currentTimeMillis();

        // リクエストログ
        log.info("Request: {} {} from {}",
                httpRequest.getMethod(),
                httpRequest.getRequestURI(),
                httpRequest.getRemoteAddr());

        try {
            chain.doFilter(request, response);
        } finally {
            // レスポンスログ
            long duration = System.currentTimeMillis() - startTime;
            log.info("Response: {} {} - Status: {} - Duration: {}ms",
                    httpRequest.getMethod(),
                    httpRequest.getRequestURI(),
                    httpResponse.getStatus(),
                    duration);
        }
    }
}
```

### 4-2. 詳細なリクエストログ

```java
@Component
@Slf4j
public class DetailedRequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        ContentCachingRequestWrapper requestWrapper = 
            new ContentCachingRequestWrapper((HttpServletRequest) request);
        ContentCachingResponseWrapper responseWrapper = 
            new ContentCachingResponseWrapper((HttpServletResponse) response);

        long startTime = System.currentTimeMillis();

        try {
            chain.doFilter(requestWrapper, responseWrapper);
        } finally {
            logRequestDetails(requestWrapper);
            logResponseDetails(responseWrapper, System.currentTimeMillis() - startTime);
            
            // レスポンスボディをコピー
            responseWrapper.copyBodyToResponse();
        }
    }

    private void logRequestDetails(ContentCachingRequestWrapper request) {
        String requestBody = new String(request.getContentAsByteArray(), StandardCharsets.UTF_8);
        
        log.debug("Request Details - Method: {}, URI: {}, Headers: {}, Body: {}",
                request.getMethod(),
                request.getRequestURI(),
                getHeadersAsString(request),
                requestBody);
    }

    private void logResponseDetails(ContentCachingResponseWrapper response, long duration) {
        String responseBody = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
        
        log.debug("Response Details - Status: {}, Duration: {}ms, Body: {}",
                response.getStatus(),
                duration,
                responseBody);
    }

    private String getHeadersAsString(HttpServletRequest request) {
        StringBuilder headers = new StringBuilder();
        request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
            headers.append(headerName).append(": ")
                   .append(request.getHeader(headerName)).append(", ");
        });
        return headers.toString();
    }
}
```

---

## 🚀 ステップ5: 構造化ログ（JSON形式）

### 5-1. Logstash Encoderの追加

**ファイルパス**: `pom.xml`

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

### 5-2. JSON形式のログ設定

**ファイルパス**: `src/main/resources/logback-spring.xml`

```xml
<!-- JSON形式でのファイル出力 -->
<appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/application.json</file>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <customFields>{"app":"hello-spring-boot"}</customFields>
    </encoder>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>logs/application-%d{yyyy-MM-dd}.%i.json</fileNamePattern>
        <maxFileSize>10MB</maxFileSize>
        <maxHistory>30</maxHistory>
    </rollingPolicy>
</appender>

<root level="INFO">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="JSON_FILE"/>
</root>
```

### 5-3. JSON形式のログ例

```json
{
  "@timestamp": "2025-10-27T10:30:00.123+09:00",
  "level": "INFO",
  "thread_name": "http-nio-8080-exec-1",
  "logger_name": "c.e.h.service.UserService",
  "message": "User created successfully with ID: 1",
  "app": "hello-spring-boot",
  "stack_trace": null
}
```

---

## ✅ ステップ6: 動作確認

### 6-1. アプリケーション起動

起動すると`logs/`ディレクトリが作成されます：
```
logs/
├── application.log
├── application.json
└── error.log
```

### 6-2. APIリクエスト

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Taro Yamada",
    "email": "taro@example.com",
    "age": 30
  }'
```

### 6-3. ログ確認

**コンソール**:
```
2025-10-27 10:30:00 [http-nio-8080-exec-1] INFO  c.e.h.filter.RequestLoggingFilter - Request: POST /api/users from 127.0.0.1
2025-10-27 10:30:00 [http-nio-8080-exec-1] DEBUG c.e.h.service.UserService - Creating user with email: taro@example.com
2025-10-27 10:30:00 [http-nio-8080-exec-1] INFO  c.e.h.service.UserService - User created successfully with ID: 1
2025-10-27 10:30:00 [http-nio-8080-exec-1] INFO  c.e.h.filter.RequestLoggingFilter - Response: POST /api/users - Status: 201 - Duration: 123ms
```

**application.log**:
```
2025-10-27 10:30:00.123 [http-nio-8080-exec-1] INFO  c.e.h.service.UserService - User created successfully with ID: 1
```

**error.log** （エラー時のみ）:
```
2025-10-27 10:31:00.456 [http-nio-8080-exec-2] ERROR c.e.h.service.UserService - Failed to create user: duplicate@example.com
java.lang.RuntimeException: Duplicate email
    at com.example.hellospringboot.service.UserService.createUser(UserService.java:45)
    ...
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: MDC（Mapped Diagnostic Context）

リクエストごとにユニークIDを付けてログを追跡しやすくしてください。

**ヒント**:
```java
import org.slf4j.MDC;

@Component
public class RequestIdFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
        }
    }
}
```

### チャレンジ 2: パフォーマンスログ

処理時間が一定時間を超えた場合に警告を出してください。

### チャレンジ 3: メトリクス収集

ログからメトリクス（リクエスト数、エラー率など）を収集できるようにしてください。

---

## 🐛 トラブルシューティング

### ログファイルが作成されない

**原因**: ディレクトリの書き込み権限がない

**解決策**: ディレクトリを手動で作成
```bash
mkdir logs
```

### 日本語が文字化けする

**解決策**: エンコーディングをUTF-8に設定
```xml
<encoder>
    <pattern>${LOG_PATTERN}</pattern>
    <charset>UTF-8</charset>
</encoder>
```

### ログが多すぎる

**解決策**: ログレベルを上げる
```yaml
logging:
  level:
    com.example.hellospringboot: INFO  # DEBUGからINFOへ
```

---

## 📚 このステップで学んだこと

- ✅ SLF4JとLogbackの基本
- ✅ ログレベルの使い分け
- ✅ application.ymlでのログ設定
- ✅ logback-spring.xmlでの詳細設定
- ✅ ファイルローテーション
- ✅ リクエスト/レスポンスログ
- ✅ 構造化ログ（JSON形式）

---

## 💡 補足: ロギングのベストプラクティス

### ログレベルの使い分け

```java
// ✅ 良い例
log.info("User {} logged in", username);  // 重要なイベント
log.debug("Request parameters: {}", params);  // デバッグ情報
log.error("Failed to connect to database", ex);  // エラー

// ❌ 悪い例
log.info("Method entered");  // 冗長
log.error("User not found");  // WARNであるべき
System.out.println("Debug: " + value);  // System.out使用NG
```

### パフォーマンス考慮

```java
// ❌ 悪い例: 文字列結合は常に実行される
log.debug("User: " + user.toString());

// ✅ 良い例: プレースホルダーを使用
log.debug("User: {}", user);

// ✅ 良い例: ログレベルチェック
if (log.isDebugEnabled()) {
    log.debug("Expensive operation result: {}", expensiveMethod());
}
```

### 機密情報の除外

```java
// ❌ 悪い例
log.info("User password: {}", password);

// ✅ 良い例
log.info("User logged in: {}", username);  // パスワードは記録しない

// ✅ 良い例: マスキング
log.info("Credit card: {}", maskCreditCard(cardNumber));
```

---

## 🔄 Gitへのコミット

進捗を記録しましょう：

```bash
git add .
git commit -m "Step 18: ロギング完了"
git push origin main
```

---

## ➡️ 次のステップ

次は[Step 19: DTOとEntityの分離](STEP_19.md)へ進みましょう！

コーディング規約、設計パターン、パフォーマンス最適化などを学びます。

---

お疲れさまでした！ 🎉

適切なログは、本番環境でのトラブルシューティングに不可欠です。
ログレベルを使い分け、必要な情報を適切に記録することで、
問題の早期発見と迅速な解決が可能になります！
