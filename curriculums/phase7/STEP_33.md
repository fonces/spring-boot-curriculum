# Step 33: 非同期処理

## 🎯 このステップの目標

- `@Async`アノテーションで非同期処理を実装できる
- `CompletableFuture`を使って非同期結果を扱える
- スレッドプールを設定してリソースを管理できる
- 非同期処理のエラーハンドリングができる
- 実践的な非同期処理のユースケースを理解できる

**所要時間**: 約50分

---

## 📋 事前準備

- Step 32までの内容を完了していること
- Spring Bootアプリケーションが起動できること
- 非同期処理の基本概念を理解していること

---

## 🚀 ステップ1: 非同期処理とは

### 1-1. 同期処理の問題点

**問題: 重い処理で応答が遅れる**

```java
// ❌ 同期処理（重い処理が完了するまでブロック）
@PostMapping("/send-email")
public String sendEmail(@RequestParam String to) {
    emailService.sendEmail(to);  // 3秒かかる
    return "Email sent";  // 3秒後に返る
}
```

**課題**:
- ユーザーが3秒間待たされる
- サーバーのスレッドが占有される
- 同時リクエスト数に制限

**解決: 非同期処理**

```java
// ✅ 非同期処理（バックグラウンドで実行）
@PostMapping("/send-email")
public String sendEmail(@RequestParam String to) {
    emailService.sendEmailAsync(to);  // すぐに返る
    return "Email sending started";  // 即座に返る
}
```

**メリット**:
- 即座にレスポンスを返せる
- サーバーリソースの効率的な利用
- スループットの向上

---

## 🚀 ステップ2: @Asyncの基本設定

### 2-1. 非同期処理を有効化

メインクラスに`@EnableAsync`を追加します：

```java
// src/main/java/com/example/hellospringboot/HelloSpringBootApplication.java
package com.example.hellospringboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching
@EnableAsync  // 非同期処理を有効化
public class HelloSpringBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelloSpringBootApplication.class, args);
    }

}
```

### 2-2. スレッドプール設定

`src/main/java/com/example/hellospringboot/config/AsyncConfig.java`を作成：

```java
package com.example.hellospringboot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 非同期処理の設定
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {
    
    /**
     * 非同期処理用のスレッドプール
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // コアスレッド数（常に起動しているスレッド数）
        executor.setCorePoolSize(5);
        
        // 最大スレッド数（負荷が高い時の上限）
        executor.setMaxPoolSize(10);
        
        // キューの容量（スレッドが全て使用中の時にタスクを待機させる）
        executor.setQueueCapacity(100);
        
        // スレッド名のプレフィックス
        executor.setThreadNamePrefix("async-");
        
        // 初期化
        executor.initialize();
        
        log.info("Async thread pool initialized: core={}, max={}, queue={}", 
                executor.getCorePoolSize(), 
                executor.getMaxPoolSize(), 
                executor.getQueueCapacity());
        
        return executor;
    }
    
    /**
     * デフォルトのExecutorを設定
     */
    @Override
    public Executor getAsyncExecutor() {
        return taskExecutor();
    }
}
```

### 2-3. コードの解説

#### `@EnableAsync`
```java
@EnableAsync
```
- 非同期処理を有効化
- `@Async`アノテーションが動作するようになる

#### `ThreadPoolTaskExecutor`
```java
executor.setCorePoolSize(5);
executor.setMaxPoolSize(10);
executor.setQueueCapacity(100);
```
- **corePoolSize**: 常に起動している最小スレッド数
- **maxPoolSize**: 負荷が高い時の最大スレッド数
- **queueCapacity**: スレッドが全て使用中の時のキュー容量

**スレッドプールの動作**:
1. タスクが来る → corePoolSize以下ならすぐ実行
2. corePoolSizeを超える → queueに追加
3. queueが満杯 → maxPoolSizeまでスレッド追加
4. maxPoolSizeも超える → エラー（RejectedExecutionException）

---

## 🚀 ステップ3: @Asyncの基本的な使い方

### 3-1. 非同期メールサービスを作成

`src/main/java/com/example/hellospringboot/services/EmailService.java`を作成：

```java
package com.example.hellospringboot.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * メール送信サービス（非同期処理の例）
 */
@Service
@Slf4j
public class EmailService {
    
    /**
     * 同期的なメール送信（3秒かかる）
     */
    public void sendEmail(String to, String subject, String body) {
        log.info("Sending email to {} (synchronous)", to);
        
        try {
            // メール送信処理のシミュレーション（3秒）
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Email sending interrupted", e);
        }
        
        log.info("Email sent to {}", to);
    }
    
    /**
     * 非同期メール送信（戻り値なし）
     */
    @Async
    public void sendEmailAsync(String to, String subject, String body) {
        log.info("Sending email to {} asynchronously on thread: {}", 
                to, Thread.currentThread().getName());
        
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Email sending interrupted", e);
        }
        
        log.info("Email sent to {} on thread: {}", to, Thread.currentThread().getName());
    }
    
    /**
     * 非同期メール送信（CompletableFuture）
     */
    @Async
    public CompletableFuture<String> sendEmailAsyncWithResult(String to, String subject, String body) {
        log.info("Sending email to {} asynchronously (with result) on thread: {}", 
                to, Thread.currentThread().getName());
        
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.failedFuture(
                new RuntimeException("Email sending interrupted", e));
        }
        
        String result = "Email sent to " + to;
        log.info(result);
        
        return CompletableFuture.completedFuture(result);
    }
    
    /**
     * 複数のメールを並列送信
     */
    @Async
    public CompletableFuture<String> sendBulkEmail(String to) {
        log.info("Sending bulk email to {} on thread: {}", to, Thread.currentThread().getName());
        
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.failedFuture(e);
        }
        
        return CompletableFuture.completedFuture("Email sent to " + to);
    }
}
```

### 3-2. EmailControllerを作成

`src/main/java/com/example/hellospringboot/controllers/EmailController.java`を作成：

```java
package com.example.hellospringboot.controllers;

import com.example.hellospringboot.services.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * メール送信API
 */
@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
@Slf4j
public class EmailController {
    
    private final EmailService emailService;
    
    /**
     * 同期メール送信
     */
    @PostMapping("/send-sync")
    public ResponseEntity<Map<String, String>> sendEmailSync(
            @RequestParam String to,
            @RequestParam(defaultValue = "Test Subject") String subject,
            @RequestParam(defaultValue = "Test Body") String body) {
        
        log.info("Sync email request received for: {}", to);
        long startTime = System.currentTimeMillis();
        
        // 同期処理（3秒待つ）
        emailService.sendEmail(to, subject, body);
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("Sync email request completed in {} ms", duration);
        
        return ResponseEntity.ok(Map.of(
            "message", "Email sent synchronously",
            "duration", duration + " ms"
        ));
    }
    
    /**
     * 非同期メール送信（戻り値なし）
     */
    @PostMapping("/send-async")
    public ResponseEntity<Map<String, String>> sendEmailAsync(
            @RequestParam String to,
            @RequestParam(defaultValue = "Test Subject") String subject,
            @RequestParam(defaultValue = "Test Body") String body) {
        
        log.info("Async email request received for: {}", to);
        long startTime = System.currentTimeMillis();
        
        // 非同期処理（すぐ返る）
        emailService.sendEmailAsync(to, subject, body);
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("Async email request completed in {} ms", duration);
        
        return ResponseEntity.ok(Map.of(
            "message", "Email sending started asynchronously",
            "duration", duration + " ms"
        ));
    }
    
    /**
     * 非同期メール送信（結果を待つ）
     */
    @PostMapping("/send-async-await")
    public ResponseEntity<Map<String, String>> sendEmailAsyncAndWait(
            @RequestParam String to,
            @RequestParam(defaultValue = "Test Subject") String subject,
            @RequestParam(defaultValue = "Test Body") String body) {
        
        log.info("Async email request (with await) received for: {}", to);
        long startTime = System.currentTimeMillis();
        
        try {
            // 非同期処理の結果を待つ
            CompletableFuture<String> future = emailService.sendEmailAsyncWithResult(to, subject, body);
            String result = future.get();  // ここで結果を待つ
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Async email request (with await) completed in {} ms", duration);
            
            return ResponseEntity.ok(Map.of(
                "message", result,
                "duration", duration + " ms"
            ));
        } catch (Exception e) {
            log.error("Error sending email", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * 複数メールを並列送信
     */
    @PostMapping("/send-bulk")
    public ResponseEntity<Map<String, Object>> sendBulkEmails(
            @RequestBody List<String> recipients) {
        
        log.info("Bulk email request for {} recipients", recipients.size());
        long startTime = System.currentTimeMillis();
        
        try {
            // 全ての非同期処理を開始
            List<CompletableFuture<String>> futures = recipients.stream()
                    .map(emailService::sendBulkEmail)
                    .toList();
            
            // 全ての処理が完了するまで待つ
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]));
            
            allFutures.get();  // 全ての処理完了を待つ
            
            // 結果を取得
            List<String> results = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Bulk email request completed in {} ms", duration);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "All emails sent");
            response.put("count", results.size());
            response.put("results", results);
            response.put("duration", duration + " ms");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error sending bulk emails", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
}
```

### 3-3. コードの解説

#### `@Async`（戻り値なし）
```java
@Async
public void sendEmailAsync(String to, String subject, String body) {
    // バックグラウンドで実行
}
```
- メソッドがバックグラウンドで実行される
- 呼び出し元はすぐに次の処理に進む
- 結果を受け取れない

#### `@Async`（CompletableFuture）
```java
@Async
public CompletableFuture<String> sendEmailAsyncWithResult(...) {
    return CompletableFuture.completedFuture("Email sent");
}
```
- 非同期処理の結果を返せる
- `future.get()`で結果を待つことも可能
- エラーハンドリングが容易

#### 並列処理
```java
List<CompletableFuture<String>> futures = recipients.stream()
    .map(emailService::sendBulkEmail)
    .toList();

CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
```
- 複数の非同期処理を並列実行
- `allOf()`で全ての完了を待つ
- 大幅な時間短縮が可能

---

## 🚀 ステップ4: 実践的な非同期処理例

### 4-1. 画像処理サービス（非同期）

`src/main/java/com/example/hellospringboot/services/ImageProcessingService.java`を作成：

```java
package com.example.hellospringboot.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 画像処理サービス（非同期）
 */
@Service
@Slf4j
public class ImageProcessingService {
    
    /**
     * サムネイル生成（非同期）
     */
    @Async
    public CompletableFuture<String> generateThumbnail(String imageUrl) {
        log.info("Generating thumbnail for: {} on thread: {}", 
                imageUrl, Thread.currentThread().getName());
        
        try {
            // サムネイル生成処理のシミュレーション（2秒）
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.failedFuture(e);
        }
        
        String thumbnailUrl = imageUrl.replace(".jpg", "_thumb.jpg");
        log.info("Thumbnail generated: {}", thumbnailUrl);
        
        return CompletableFuture.completedFuture(thumbnailUrl);
    }
    
    /**
     * 画像リサイズ（非同期）
     */
    @Async
    public CompletableFuture<String> resizeImage(String imageUrl, String size) {
        log.info("Resizing image: {} to {} on thread: {}", 
                imageUrl, size, Thread.currentThread().getName());
        
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.failedFuture(e);
        }
        
        String resizedUrl = imageUrl.replace(".jpg", "_" + size + ".jpg");
        log.info("Image resized: {}", resizedUrl);
        
        return CompletableFuture.completedFuture(resizedUrl);
    }
    
    /**
     * 画像の最適化（サムネイル + 複数サイズ）
     */
    public CompletableFuture<Map<String, String>> optimizeImage(String imageUrl) {
        log.info("Starting image optimization for: {}", imageUrl);
        
        // 並列で複数の処理を実行
        CompletableFuture<String> thumbnailFuture = generateThumbnail(imageUrl);
        CompletableFuture<String> smallFuture = resizeImage(imageUrl, "small");
        CompletableFuture<String> mediumFuture = resizeImage(imageUrl, "medium");
        CompletableFuture<String> largeFuture = resizeImage(imageUrl, "large");
        
        // 全ての処理が完了したら結果をまとめる
        return CompletableFuture.allOf(thumbnailFuture, smallFuture, mediumFuture, largeFuture)
                .thenApply(v -> {
                    Map<String, String> result = new HashMap<>();
                    result.put("thumbnail", thumbnailFuture.join());
                    result.put("small", smallFuture.join());
                    result.put("medium", mediumFuture.join());
                    result.put("large", largeFuture.join());
                    
                    log.info("Image optimization completed for: {}", imageUrl);
                    return result;
                });
    }
}
```

### 4-2. レポート生成サービス（非同期）

`src/main/java/com/example/hellospringboot/services/ReportService.java`を作成：

```java
package com.example.hellospringboot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * レポート生成サービス（非同期）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {
    
    private final UserService userService;
    
    /**
     * ユーザーレポート生成（重い処理）
     */
    @Async
    public CompletableFuture<Map<String, Object>> generateUserReport(Long userId) {
        log.info("Generating user report for userId: {} on thread: {}", 
                userId, Thread.currentThread().getName());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // レポート生成処理のシミュレーション（5秒）
            TimeUnit.SECONDS.sleep(5);
            
            Map<String, Object> report = new HashMap<>();
            report.put("userId", userId);
            report.put("generatedAt", System.currentTimeMillis());
            report.put("totalPosts", 42);
            report.put("totalComments", 128);
            report.put("totalLikes", 512);
            
            long duration = System.currentTimeMillis() - startTime;
            report.put("generationTime", duration + " ms");
            
            log.info("User report generated for userId: {} in {} ms", userId, duration);
            
            return CompletableFuture.completedFuture(report);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.failedFuture(e);
        }
    }
}
```

---

## ✅ 動作確認

### 1. アプリケーションを起動

```bash
cd /path/to/hello-spring-boot
./mvnw spring-boot:run
```

### 2. 同期処理と非同期処理の比較

#### 同期処理（3秒待つ）

```bash
time curl -X POST "http://localhost:8080/api/emails/send-sync?to=test@example.com"
```

**期待される結果**:
```json
{
  "message": "Email sent synchronously",
  "duration": "3000 ms"
}

real    0m3.050s  ← 3秒かかった
```

#### 非同期処理（すぐ返る）

```bash
time curl -X POST "http://localhost:8080/api/emails/send-async?to=test@example.com"
```

**期待される結果**:
```json
{
  "message": "Email sending started asynchronously",
  "duration": "5 ms"
}

real    0m0.050s  ← すぐ返る
```

**ログ出力**:
```
Async email request received for: test@example.com
Async email request completed in 5 ms
Sending email to test@example.com asynchronously on thread: async-1
Email sent to test@example.com on thread: async-1
```

#### 非同期処理（結果を待つ）

```bash
time curl -X POST "http://localhost:8080/api/emails/send-async-await?to=test@example.com"
```

**期待される結果**:
```json
{
  "message": "Email sent to test@example.com",
  "duration": "3000 ms"
}

real    0m3.050s  ← 非同期実行だが結果を待つので3秒かかる
```

**解説**:
- `CompletableFuture.get()`で結果を待つため、同期処理と同じ時間がかかる
- ただし、非同期スレッドプールで実行されるため、メインスレッドは占有しない
- 結果を取得したい場合に有効なパターン

### 3. 並列処理の効果確認

#### 順次処理（1人ずつ送信）なら6秒

```bash
# 3人に順次送信すると 3人 × 2秒 = 6秒
```

#### 並列処理（同時送信）なら2秒

```bash
time curl -X POST http://localhost:8080/api/emails/send-bulk \
  -H "Content-Type: application/json" \
  -d '["user1@example.com", "user2@example.com", "user3@example.com"]'
```

**期待される結果**:
```json
{
  "message": "All emails sent",
  "count": 3,
  "results": [
    "Email sent to user1@example.com",
    "Email sent to user2@example.com",
    "Email sent to user3@example.com"
  ],
  "duration": "2050 ms"  ← 並列実行で大幅短縮
}

real    0m2.100s
```

### 4. スレッド名の確認

ログを見ると、非同期処理が異なるスレッドで実行されていることがわかります：

```
Sending email to user1@example.com on thread: async-1
Sending email to user2@example.com on thread: async-2
Sending email to user3@example.com on thread: async-3
```

---

## 🎨 チャレンジ課題

基本が理解できたら、以下にチャレンジしてみましょう：

### チャレンジ 1: タイムアウト処理

**目標**: 非同期処理が一定時間で完了しない場合にタイムアウト

**ヒント**:
```java
try {
    String result = future.get(5, TimeUnit.SECONDS);  // 5秒でタイムアウト
} catch (TimeoutException e) {
    log.error("Operation timed out");
    throw new RuntimeException("Email sending timed out", e);
}
```

### チャレンジ 2: リトライ機能

**目標**: 失敗時に自動的に再試行

**ヒント**:
```java
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
</dependency>

@Retryable(
    value = {RuntimeException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000)
)
@Async
public CompletableFuture<String> sendEmailWithRetry(String to) {
    // 失敗したら3回まで再試行
}
```

### チャレンジ 3: 非同期処理の進捗管理

**目標**: 非同期処理の進捗を追跡できるようにする

**ヒント**:
```java
@Service
public class TaskProgressService {
    private final Map<String, Integer> progressMap = new ConcurrentHashMap<>();
    
    public void updateProgress(String taskId, int progress) {
        progressMap.put(taskId, progress);
    }
    
    public Integer getProgress(String taskId) {
        return progressMap.getOrDefault(taskId, 0);
    }
}

@Async
public CompletableFuture<String> processWithProgress(String taskId) {
    taskProgressService.updateProgress(taskId, 0);
    // 処理...
    taskProgressService.updateProgress(taskId, 50);
    // 処理...
    taskProgressService.updateProgress(taskId, 100);
    return CompletableFuture.completedFuture("Done");
}
```

---

## 🐛 トラブルシューティング

### エラー: 非同期処理が動作しない

**原因1**: `@EnableAsync`を付け忘れ

**解決策**:
```java
@SpringBootApplication
@EnableAsync  // 追加
public class HelloSpringBootApplication {...}
```

**原因2**: 同じクラス内のメソッド呼び出し

```java
// ❌ 非同期にならない
public class UserService {
    public void someMethod() {
        sendEmailAsync("test@example.com");  // 同じクラス内
    }
    
    @Async
    public void sendEmailAsync(String to) {...}
}
```

**解決策**: 別のBeanから呼び出す

### エラー: "TaskRejectedException"

**原因**: スレッドプールの容量を超えた

```
org.springframework.core.task.TaskRejectedException: Executor [taskExecutor] did not accept task
```

**解決策**: スレッドプールのサイズを増やす
```java
executor.setMaxPoolSize(20);  // 10 → 20
executor.setQueueCapacity(200);  // 100 → 200
```

### 非同期処理のエラーが見えない

**原因**: デフォルトではエラーがログに出力されない

**解決策**: AsyncConfigurerでエラーハンドラを設定
```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("Async method {} threw exception: {}", 
                method.getName(), ex.getMessage(), ex);
        };
    }
}
```

### メモリリーク

**原因**: CompletableFutureが完了しないまま保持される

**解決策**: タイムアウトを設定
```java
CompletableFuture<String> future = emailService.sendEmailAsync(to)
    .orTimeout(10, TimeUnit.SECONDS);  // 10秒でタイムアウト
```

---

## 📚 このステップで学んだこと

- ✅ `@EnableAsync`で非同期処理を有効化
- ✅ `@Async`アノテーションの使い方
- ✅ `CompletableFuture`で非同期結果を扱う
- ✅ `ThreadPoolTaskExecutor`でスレッドプールを設定
- ✅ 並列処理で複数タスクを同時実行
- ✅ `CompletableFuture.allOf()`で複数の完了を待つ
- ✅ 非同期処理のエラーハンドリング
- ✅ スレッド名のカスタマイズ
- ✅ 同期処理と非同期処理のパフォーマンス比較
- ✅ 実践的な非同期処理のユースケース

---

## 💡 補足: 非同期処理のベストプラクティス

### 1. 非同期処理が適している場面

| ユースケース | 推奨度 | 理由 |
|---|---|---|
| **メール送信** | ⭐⭐⭐ | 時間がかかる外部API呼び出し |
| **画像処理** | ⭐⭐⭐ | CPU集約的な処理 |
| **レポート生成** | ⭐⭐⭐ | 重い処理をバックグラウンドで |
| **ログ記録** | ⭐⭐ | I/O処理を非同期化 |
| **データ取得** | ❌ | 結果が必要な場合は意味がない |

### 2. 同期 vs 非同期の選択基準

**同期処理が適している**:
- 結果がすぐに必要
- 処理が軽い（< 100ms）
- トランザクションが必要

**非同期処理が適している**:
- 結果を待つ必要がない
- 処理が重い（> 1秒）
- 外部APIの呼び出し

### 3. スレッドプールのサイジング

**CPUバウンド（計算処理）**:
```java
// CPU数 + 1 が目安
int corePoolSize = Runtime.getRuntime().availableProcessors() + 1;
```

**I/Oバウンド（ネットワーク、ディスク）**:
```java
// CPU数 * 2 〜 CPU数 * 4 が目安
int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
```

**推奨設定例**:
```java
ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
executor.setCorePoolSize(10);      // 常時10スレッド
executor.setMaxPoolSize(50);       // 最大50スレッド
executor.setQueueCapacity(100);    // キュー100
executor.setKeepAliveSeconds(60);  // アイドル60秒で削除
```

### 4. CompletableFutureのパターン

**チェーン処理**:
```java
CompletableFuture.supplyAsync(() -> fetchUser(id))
    .thenApply(user -> user.getName())
    .thenAccept(name -> log.info("User name: {}", name));
```

**並列処理**:
```java
CompletableFuture<String> future1 = fetchData1();
CompletableFuture<String> future2 = fetchData2();

CompletableFuture<Void> combined = CompletableFuture.allOf(future1, future2);
combined.thenRun(() -> {
    String result1 = future1.join();
    String result2 = future2.join();
    // 両方完了後の処理
});
```

**エラーハンドリング**:
```java
CompletableFuture<String> future = fetchData()
    .exceptionally(ex -> {
        log.error("Error occurred", ex);
        return "default value";
    });
```

### 5. @Asyncの制約と注意点

**❌ 同じクラス内の呼び出しは非同期にならない**:
```java
@Service
public class UserService {
    public void method1() {
        method2Async();  // 非同期にならない
    }
    
    @Async
    public void method2Async() {...}
}
```

**✅ 別のBeanから呼び出す**:
```java
@Service
public class UserService {
    @Autowired
    private AsyncTaskService asyncTaskService;
    
    public void method1() {
        asyncTaskService.method2Async();  // 非同期になる
    }
}

@Service
public class AsyncTaskService {
    @Async
    public void method2Async() {...}
}
```

### 6. 非同期処理のモニタリング

**Actuatorでスレッドプールを監視**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: metrics, health
  metrics:
    enable:
      executor: true
```

**カスタムメトリクス**:
```java
@Bean
public ThreadPoolTaskExecutor taskExecutor(MeterRegistry meterRegistry) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);
    executor.setMaxPoolSize(50);
    executor.initialize();
    
    // メトリクスを登録
    meterRegistry.gauge("async.pool.size", executor, ThreadPoolTaskExecutor::getPoolSize);
    meterRegistry.gauge("async.active.count", executor, ThreadPoolTaskExecutor::getActiveCount);
    
    return executor;
}
```

---

## ➡️ 次のステップ

Phase 7が完了しました！お疲れ様でした！

次は[Phase 8: 総合演習（最終プロジェクト）](../phase8/STEP_34.md)に進みましょう。

Phase 8では、これまで学んだすべての知識を活かして、本格的なWebアプリケーション（ミニブログシステム）を構築します。認証・認可、記事投稿、コメント機能、画像アップロード、検索機能など、実践的な機能を実装していきます。
