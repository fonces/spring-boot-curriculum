# Step 33: 非同期処理

## 🎯 このステップの目標

- @Asyncで非同期処理を実装する
- ThreadPoolの設定を理解する
- CompletableFutureを使いこなす
- 非同期処理のエラーハンドリング

**所要時間**: 約2時間


---


## 📋 事前準備

- Step 32が完了していること
- アプリケーションの動作確認ができること

---
---

## 💡 非同期処理の必要性

### メリット

- ⚡ レスポンス時間の短縮
- 🔄 並列処理による効率化
- 📧 バックグラウンドタスクの実行
- 🎯 リソースの有効活用

---

## 🚀 ステップ1: 非同期処理の有効化

### 1-1. AsyncConfig

**ファイルパス**: `src/main/java/com/example/hellospringboot/config/AsyncConfig.java`

```java
package com.example.hellospringboot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 非同期処理設定
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // コアスレッド数
        executor.setCorePoolSize(5);
        
        // 最大スレッド数
        executor.setMaxPoolSize(10);
        
        // キューのキャパシティ
        executor.setQueueCapacity(100);
        
        // スレッド名のプレフィックス
        executor.setThreadNamePrefix("async-");
        
        // シャットダウン時に実行中のタスクを待つ
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("非同期タスクエグゼキューターを初期化しました");
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("非同期処理でエラーが発生しました: {}, Method: {}", 
                    throwable.getMessage(), method.getName(), throwable);
        };
    }
}
```

---

## 🚀 ステップ2: 非同期サービス

### 2-1. AsyncTaskService

**ファイルパス**: `src/main/java/com/example/hellospringboot/service/AsyncTaskService.java`

```java
package com.example.hellospringboot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class AsyncTaskService {

    /**
     * 非同期タスク（戻り値なし）
     */
    @Async
    public void executeAsyncTask(String taskName) {
        log.info("非同期タスク開始: {} [Thread: {}]", taskName, Thread.currentThread().getName());
        
        try {
            // 重い処理をシミュレート
            Thread.sleep(3000);
            log.info("非同期タスク完了: {}", taskName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("タスクが中断されました: {}", taskName);
        }
    }

    /**
     * 非同期タスク（戻り値あり）
     */
    @Async
    public CompletableFuture<String> executeAsyncTaskWithResult(String taskName) {
        log.info("非同期タスク開始（戻り値あり): {} [Thread: {}]", 
                taskName, Thread.currentThread().getName());
        
        try {
            Thread.sleep(2000);
            String result = "タスク結果: " + taskName;
            log.info("非同期タスク完了: {}", taskName);
            return CompletableFuture.completedFuture(result);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("タスクが中断されました: {}", taskName);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 複数の非同期タスクを並列実行
     */
    public CompletableFuture<String> executeMultipleTasks() {
        CompletableFuture<String> task1 = executeAsyncTaskWithResult("Task1");
        CompletableFuture<String> task2 = executeAsyncTaskWithResult("Task2");
        CompletableFuture<String> task3 = executeAsyncTaskWithResult("Task3");

        // すべてのタスクが完了するまで待つ
        return CompletableFuture.allOf(task1, task2, task3)
                .thenApply(v -> {
                    String result1 = task1.join();
                    String result2 = task2.join();
                    String result3 = task3.join();
                    return String.format("%s, %s, %s", result1, result2, result3);
                });
    }
}
```

---

## 🚀 ステップ3: スケジュール実行

### 3-1. ScheduleConfig

**ファイルパス**: `src/main/java/com/example/hellospringboot/config/ScheduleConfig.java`

```java
package com.example.hellospringboot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * スケジュール実行設定
 */
@Configuration
@EnableScheduling
public class ScheduleConfig {
}
```

### 3-2. ScheduledTaskService

**ファイルパス**: `src/main/java/com/example/hellospringboot/service/ScheduledTaskService.java`

```java
package com.example.hellospringboot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class ScheduledTaskService {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 固定レートで実行（5秒ごと）
     */
    @Scheduled(fixedRate = 5000)
    public void executeFixedRateTask() {
        log.info("[固定レート] タスク実行: {}", LocalDateTime.now().format(formatter));
    }

    /**
     * 固定遅延で実行（前回の終了から3秒後）
     */
    @Scheduled(fixedDelay = 3000)
    public void executeFixedDelayTask() {
        log.info("[固定遅延] タスク実行: {}", LocalDateTime.now().format(formatter));
        try {
            Thread.sleep(2000); // 処理時間
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Cron式で実行（毎分0秒）
     */
    @Scheduled(cron = "0 * * * * *")
    public void executeCronTask() {
        log.info("[Cron] 毎分タスク実行: {}", LocalDateTime.now().format(formatter));
    }

    /**
     * 初期遅延付き実行（起動10秒後から、5秒ごと）
     */
    @Scheduled(initialDelay = 10000, fixedRate = 5000)
    public void executeTaskWithInitialDelay() {
        log.info("[初期遅延] タスク実行: {}", LocalDateTime.now().format(formatter));
    }

    /**
     * 毎日午前2時に実行
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void executeDailyTask() {
        log.info("[日次] バックアップタスク実行: {}", LocalDateTime.now().format(formatter));
        // バックアップ処理など
    }

    /**
     * 平日の午前9時に実行
     */
    @Scheduled(cron = "0 0 9 * * MON-FRI")
    public void executeWeekdayTask() {
        log.info("[平日] レポート送信タスク実行: {}", LocalDateTime.now().format(formatter));
        // レポート送信処理など
    }
}
```

---

## 🚀 ステップ4: AsyncController

**ファイルパス**: `src/main/java/com/example/hellospringboot/controller/AsyncController.java`

```java
package com.example.hellospringboot.controller;

import com.example.hellospringboot.service.AsyncTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/async")
@RequiredArgsConstructor
@Tag(name = "Async", description = "非同期処理API")
public class AsyncController {

    private final AsyncTaskService asyncTaskService;

    @Operation(summary = "非同期タスク実行", description = "非同期でタスクを実行します（戻り値なし）")
    @PostMapping("/task")
    public ResponseEntity<String> executeTask(@RequestParam String taskName) {
        log.info("非同期タスクをキックします: {}", taskName);
        asyncTaskService.executeAsyncTask(taskName);
        return ResponseEntity.ok("タスクを開始しました: " + taskName);
    }

    @Operation(summary = "非同期タスク実行（結果取得）", description = "非同期でタスクを実行して結果を取得します")
    @PostMapping("/task-with-result")
    public CompletableFuture<ResponseEntity<String>> executeTaskWithResult(@RequestParam String taskName) {
        log.info("非同期タスクをキックします（結果取得）: {}", taskName);
        
        return asyncTaskService.executeAsyncTaskWithResult(taskName)
                .thenApply(result -> ResponseEntity.ok(result))
                .exceptionally(ex -> {
                    log.error("タスク実行エラー: {}", ex.getMessage());
                    return ResponseEntity.internalServerError()
                            .body("エラーが発生しました: " + ex.getMessage());
                });
    }

    @Operation(summary = "複数タスク並列実行", description = "複数のタスクを並列実行します")
    @PostMapping("/multiple-tasks")
    public CompletableFuture<ResponseEntity<String>> executeMultipleTasks() {
        log.info("複数タスクを並列実行します");
        
        return asyncTaskService.executeMultipleTasks()
                .thenApply(result -> ResponseEntity.ok(result))
                .exceptionally(ex -> {
                    log.error("タスク実行エラー: {}", ex.getMessage());
                    return ResponseEntity.internalServerError()
                            .body("エラーが発生しました: " + ex.getMessage());
                });
    }
}
```

---

## 🚀 ステップ5: Cron式の解説

### Cron式の構造

```
秒 分 時 日 月 曜日

例:
0 0 12 * * *        # 毎日12時
0 */5 * * * *       # 5分ごと
0 0 9-17 * * MON-FRI # 平日9時〜17時の毎時0分
```

| フィールド | 必須 | 値の範囲 | 特殊文字 |
|----------|------|---------|---------|
| 秒 | はい | 0-59 | , - * / |
| 分 | はい | 0-59 | , - * / |
| 時 | はい | 0-23 | , - * / |
| 日 | はい | 1-31 | , - * ? / L W |
| 月 | はい | 1-12 or JAN-DEC | , - * / |
| 曜日 | はい | 0-7 or SUN-SAT | , - * ? / L # |

---

## ✅ 動作確認

### 非同期タスク実行

```bash
# 非同期タスク（戻り値なし）
curl -X POST "http://localhost:8080/api/async/task?taskName=TestTask"

# 非同期タスク（戻り値あり）
curl -X POST "http://localhost:8080/api/async/task-with-result?taskName=TestTask"

# 複数タスク並列実行
curl -X POST "http://localhost:8080/api/async/multiple-tasks"
```

### ログ確認

```
非同期タスクをキックします: TestTask
非同期タスク開始: TestTask [Thread: async-1]
非同期タスク完了: TestTask
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: リトライ機能

`@Retryable`を使って失敗時の再試行を実装してください。

### チャレンジ 2: バッチ処理

大量データを非同期で分割処理してください。

### チャレンジ 3: タスクキャンセル

実行中のタスクをキャンセルできるようにしてください。

---

## 🐛 トラブルシューティング

### エラー: "@Asyncが効かない（同期的に実行される）"

**原因**:
- `@EnableAsync`を付け忘れている
- 同じクラス内からメソッドを呼んでいる（プロキシが効かない）
- メソッドが`public`でない

**解決策**:

```java
// ❌ 同じクラス内から呼ぶと同期実行になる
@Service
public class BadAsyncService {
    @Async
    public void asyncMethod() {
        // 非同期処理
    }
    
    public void caller() {
        this.asyncMethod();  // ❌ 同期実行される！
    }
}

// ✅ 別のクラスから呼ぶ
@Service
public class AsyncService {
    @Async
    public void asyncMethod() {
        // 非同期処理
    }
}

@Service
@RequiredArgsConstructor
public class CallerService {
    private final AsyncService asyncService;
    
    public void caller() {
        asyncService.asyncMethod();  // ✅ 非同期実行される
    }
}
```

### エラー: "TaskRejectedException: Executor has been shut down"

**原因**:
- スレッドプールのキューが満杯
- アプリケーションシャットダウン中にタスク実行

**解決策**:

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);  // キュー容量を拡大
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());  // リジェクト時は呼び出し元で実行
        executor.setWaitForTasksToCompleteOnShutdown(true);  // シャットダウン時にタスク完了を待つ
        executor.setAwaitTerminationSeconds(60);  // 最大60秒待つ
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
```

### エラー: "非同期メソッドで発生した例外が握りつぶされる"

**原因**:
- 非同期メソッドの例外はデフォルトで呼び出し元に伝わらない
- 戻り値が`void`の場合、例外ハンドラが必要

**解決策**:

```java
// AsyncUncaughtExceptionHandlerを設定
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("非同期処理でエラー発生: method={}, params={}", 
                method.getName(), Arrays.toString(params), ex);
            // エラー通知やアラート送信などの処理
        };
    }
}

// または CompletableFuture を使って例外をキャッチ
@Async
public CompletableFuture<String> asyncMethodWithException() {
    try {
        // 処理
        return CompletableFuture.completedFuture("Success");
    } catch (Exception e) {
        return CompletableFuture.failedFuture(e);
    }
}

// 呼び出し側
asyncService.asyncMethodWithException()
    .exceptionally(ex -> {
        log.error("非同期処理エラー", ex);
        return "Fallback value";
    });
```

### エラー: "@Scheduledタスクが実行されない"

**原因**:
- `@EnableScheduling`を付け忘れている
- Cron式の文法エラー
- タイムゾーンの問題

**解決策**:

```java
// @EnableScheduling を忘れずに
@SpringBootApplication
@EnableScheduling
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

// Cron式を確認（6つのフィールド）
@Scheduled(cron = "0 0 9 * * *")  // ❌ 5フィールドしかない
@Scheduled(cron = "0 0 9 * * ?")  // ✅ 6フィールド（秒 分 時 日 月 曜日）

// タイムゾーンを明示
@Scheduled(cron = "0 0 9 * * ?", zone = "Asia/Tokyo")
public void scheduledTask() {
    // 処理
}
```

### エラー: "CompletableFuture.allOf()で結果を取得できない"

**原因**:
- `allOf()`は`CompletableFuture<Void>`を返すため、結果にアクセスできない
- 各Futureから個別に結果を取得する必要がある

**解決策**:

```java
// ❌ allOf() だけでは結果が取れない
CompletableFuture<Void> allFutures = CompletableFuture.allOf(future1, future2, future3);
allFutures.join();  // 結果が取れない

// ✅ 各Futureから結果を取得
CompletableFuture<String> future1 = asyncService.task1();
CompletableFuture<String> future2 = asyncService.task2();
CompletableFuture<String> future3 = asyncService.task3();

// すべて完了を待つ
CompletableFuture.allOf(future1, future2, future3).join();

// 個別に結果を取得
String result1 = future1.join();
String result2 = future2.join();
String result3 = future3.join();

// または Stream で一気に取得
List<CompletableFuture<String>> futures = List.of(future1, future2, future3);
List<String> results = futures.stream()
    .map(CompletableFuture::join)
    .collect(Collectors.toList());
```

### エラー: "スレッドプールがリークして OOM (Out of Memory) が発生"

**原因**:
- `Executor`を毎回newしている
- カスタムExecutorを`@Bean`登録していない
- スレッドが終了しない

**解決策**:

```java
// ❌ 毎回newすると古いスレッドプールが残り続ける
public void badMethod() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.initialize();  // これを繰り返すとリークする
}

// ✅ @Bean で1つだけ作成
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}

// 使用時は@Bean名を指定
@Async("taskExecutor")
public void asyncMethod() {
    // 処理
}
```

---

## 📚 このステップで学んだこと

- ✅ @Asyncによる非同期処理
- ✅ ThreadPoolTaskExecutorの設定
- ✅ CompletableFutureの使用
- ✅ @Scheduledによるスケジュール実行
- ✅ Cron式の書き方

---

## 🔄 Phase 7完了！

```bash
git add .
git commit -m "Step 33: 非同期処理完了 - Phase 7完了"
git push origin main
```

---



## 🔄 Gitへのコミットとレビュー依頼

進捗を記録してレビューを受けましょう：

```bash
git add .
git commit -m "このステップのタイトルを記載してコミット"
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

**🎉 Phase 7お疲れさまでした！**

Phase 7では、本番運用を見据えた設定管理と監視の基礎を学びました。

次は**Phase 8: 総合演習（最終プロジェクト）**に進みます。

[Step 34: プロジェクト初期設定とユーザー認証基盤](../phase8/STEP_34.md)で、これまでの学習を統合したミニブログアプリケーションを開発します。

---

お疲れさまでした！ 🎉

Phase 7では、実践的な機能を多数実装しました。
これで本格的なWebアプリケーション開発の準備が整いました！
