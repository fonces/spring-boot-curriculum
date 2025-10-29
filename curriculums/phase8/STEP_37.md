# Step 37: 高度な機能実装

## 🎯 このステップの目標

- 統計・ダッシュボード機能を実装する
- ファイル添付機能を追加する（オプション）
- リマインダー機能を実装する（オプション）
- キャッシュを活用したパフォーマンス最適化

**所要時間**: 約2時間30分

> **このステップはチャレンジ課題です！**
> 
> 基本的なCRUD機能にプラスして、実用的な機能を追加しましょう。
> できる範囲で実装してください。

---

## 📋 実装課題

### 課題1: 統計・ダッシュボード機能

ダッシュボードで表示する統計情報を実装してください。

**表示したい情報：**
- プロジェクトの進捗率（完了タスク数 / 全タスク数）
- ステータス別タスク数（TODO, IN_PROGRESS, DONE）
- 優先度別タスク数（HIGH, MEDIUM, LOW）
- 期限切れタスク数

**考えてほしいこと：**
- MyBatisでGROUP BYを使った集計クエリをどう書くか？
- 統計データを効率的に取得するには？
- キャッシュを使ってパフォーマンスを改善できないか？

> **💡 ヒント**: Spring Cacheの`@Cacheable`を使うと簡単にキャッシュできます

---

### 課題2: ファイル添付機能（オプション）

タスクにファイルを添付できる機能を実装してください。

**必要な機能：**
- ファイルアップロード
- ファイル一覧表示
- ファイルダウンロード
- ファイル削除

**考えてほしいこと：**
- ファイルをどこに保存するか？（ローカル？データベース？）
- ファイルサイズ制限をどう設定するか？
- セキュリティ（不正なファイルアップロード対策）は？

> **💡 ヒント**: `MultipartFile`を使います

---

### 課題3: リマインダー機能（オプション）

期限が近いタスクを通知する機能を実装してください。

**必要な機能：**
- 期限1日前のタスクをメール通知
- 期限切れタスクをメール通知
- 定期的に自動実行

**考えてほしいこと：**
- どうやって定期実行するか？（`@Scheduled`？）
- メール送信の実装方法は？
- 通知済みフラグをどう管理するか？

> **💡 ヒント**: Spring Schedulingと`JavaMailSender`を使います
}
```

### 2-3. TaskAttachmentService

**必須メソッド**:
```java
@Transactional
UploadFileResponse attachFile(Long taskId, MultipartFile file, User currentUser)

List<UploadFileResponse> getAttachments(Long taskId)

@Transactional
void deleteAttachment(Long attachmentId)
```

**実装のポイント**:
- `FileStorageService`を使ってファイルを保存
- TaskAttachmentエンティティに情報を保存
- ダウンロードURIを生成

### 2-4. TaskController にエンドポイント追加

**エンドポイント**:
- `POST /api/tasks/{taskId}/attachments` - ファイルアップロード
- `GET /api/tasks/{taskId}/attachments` - 添付ファイル一覧
- `GET /api/tasks/{taskId}/attachments/{attachmentId}` - ファイルダウンロード
- `DELETE /api/tasks/{taskId}/attachments/{attachmentId}` - ファイル削除

**実装のヒント**:
```java
@PostMapping("/{taskId}/attachments")
public ResponseEntity<UploadFileResponse> uploadFile(
        @PathVariable Long taskId,
        @RequestParam("file") MultipartFile file,
        @AuthenticationPrincipal User currentUser) {
    // 実装
}
```

---

## 🚀 ステップ3: リマインダー機能

### 3-1. ReminderService

**必須メソッド**:

#### 期限間近のタスクをリマインド
```java
@Scheduled(cron = "0 0 9 * * *")  // 毎日午前9時
public void sendDueDateReminders()
```

**実装のポイント**:
- 明日が期限のタスクを取得
- ステータスがDONE以外のタスクをフィルタ
- 担当者が設定されているタスクのみ
- 各担当者にメール送信

#### 期限切れタスクをリマインド
```java
@Scheduled(cron = "0 0 10 * * *")  // 毎日午前10時
public void sendOverdueReminders()
```

**実装のポイント**:
- 今日より前が期限のタスクを取得
- ステータスがDONE以外のタスクをフィルタ
- 担当者にメール送信

### 3-2. メールテンプレート

**リマインダーメール内容**:
```
件名: 【リマインダー】タスクの期限が近づいています

タスク「{タスク名}」の期限が近づいています。

プロジェクト: {プロジェクト名}
期限: {期限日}
優先度: {優先度}
```

**期限切れメール内容**:
```
件名: 【重要】タスクの期限が過ぎています

タスク「{タスク名}」の期限が過ぎています。

プロジェクト: {プロジェクト名}
期限: {期限日}
優先度: {優先度}

至急対応をお願いします。
```

**配置場所**: `src/main/java/com/example/hellospringboot/service/`

---

## ✅ 動作確認

### 統計情報の取得

```bash
curl http://localhost:8080/api/statistics/projects/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**期待されるレスポンス**:
```json
{
  "projectId": 1,
  "projectName": "新規Webサイト開発",
  "totalTasks": 25,
  "todoTasks": 10,
  "inProgressTasks": 8,
  "doneTasks": 7,
  "completionRate": 28.0,
  "tasksByPriority": {
    "HIGH": 8,
    "MEDIUM": 12,
    "LOW": 5
  },
  "overdueTasks": 2
}
```

### ファイルアップロード

```bash
curl -X POST http://localhost:8080/api/tasks/1/attachments \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@/path/to/document.pdf"
```

### リマインダーの手動実行（テスト用）

Scheduledメソッドを一時的にpublicにして、別のエンドポイントから呼び出すか、
テストクラスで直接実行してください。

---

## 💡 実装のポイント

### 統計機能
- **キャッシュ**: `@Cacheable`で統計データをキャッシュ
- **集計**: JPQLの`GROUP BY`と`COUNT`を活用
- **計算**: サービス層で進捗率を計算

### ファイル添付
- **セキュリティ**: ファイルタイプとサイズの検証
- **一意性**: UUIDでファイル名を生成
- **関連付け**: TaskAttachmentエンティティでタスクと関連付け

### リマインダー
- **スケジュール**: `@Scheduled`でCron式を使用
- **非同期**: メール送信は非同期で実行
- **エラーハンドリング**: メール送信失敗時もログを記録

### パフォーマンス
- **N+1問題**: JOIN FETCHで関連データを一度に取得
- **インデックス**: dueDateにインデックスを設定
- **キャッシュ**: 頻繁にアクセスされる統計データをキャッシュ

---

## 💡 統計機能のMyBatis実装

Phase 3で学習したMyBatisを活用して、効率的な統計クエリを実装します。

### MyBatis統計Mapper

**ファイルパス**: `src/main/java/com/example/taskapp/mapper/StatisticsMapper.java`

```java
package com.example.taskapp.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.Map;

@Mapper
public interface StatisticsMapper {
    
    /**
     * プロジェクト統計取得
     */
    @Select("""
        SELECT 
            COUNT(*) as total_tasks,
            SUM(CASE WHEN status = 'TODO' THEN 1 ELSE 0 END) as todo_tasks,
            SUM(CASE WHEN status = 'IN_PROGRESS' THEN 1 ELSE 0 END) as in_progress_tasks,
            SUM(CASE WHEN status = 'DONE' THEN 1 ELSE 0 END) as done_tasks,
            SUM(CASE WHEN priority = 'HIGH' THEN 1 ELSE 0 END) as high_priority,
            SUM(CASE WHEN priority = 'MEDIUM' THEN 1 ELSE 0 END) as medium_priority,
            SUM(CASE WHEN priority = 'LOW' THEN 1 ELSE 0 END) as low_priority,
            SUM(CASE WHEN due_date < CURDATE() AND status != 'DONE' THEN 1 ELSE 0 END) as overdue_tasks
        FROM tasks
        WHERE project_id = #{projectId}
    """)
    Map<String, Long> getProjectStatistics(@Param("projectId") Long projectId);
    
    /**
     * ユーザーの全プロジェクト統計
     */
    @Select("""
        SELECT 
            p.id as project_id,
            p.name as project_name,
            COUNT(t.id) as total_tasks,
            SUM(CASE WHEN t.status = 'DONE' THEN 1 ELSE 0 END) as done_tasks
        FROM projects p
        LEFT JOIN tasks t ON p.id = t.project_id
        WHERE p.owner_id = #{userId} OR p.id IN (
            SELECT project_id FROM project_members WHERE user_id = #{userId}
        )
        GROUP BY p.id, p.name
    """)
    List<Map<String, Object>> getUserProjectsStatistics(@Param("userId") Long userId);
    
    /**
     * 期限切れタスク数（ユーザー別）
     */
    @Select("""
        SELECT COUNT(*) 
        FROM tasks t
        JOIN projects p ON t.project_id = p.id
        WHERE t.due_date < CURDATE()
          AND t.status != 'DONE'
          AND (t.assignee_id = #{userId} OR p.owner_id = #{userId})
    """)
    long getOverdueTasksCount(@Param("userId") Long userId);
}
```

### StatisticsService（MyBatis版）

```java
package com.example.taskapp.service;

import com.example.taskapp.mapper.StatisticsMapper;
import com.example.taskapp.dto.ProjectStatisticsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

    private final StatisticsMapper statisticsMapper;

    /**
     * プロジェクト統計取得（キャッシュ有効）
     */
    @Cacheable(value = "statistics", key = "'project-' + #projectId")
    public ProjectStatisticsResponse getProjectStatistics(Long projectId) {
        Map<String, Long> stats = statisticsMapper.getProjectStatistics(projectId);
        
        long totalTasks = stats.get("total_tasks");
        long doneTasks = stats.get("done_tasks");
        
        double completionRate = totalTasks > 0 ? (doneTasks * 100.0 / totalTasks) : 0.0;
        
        ProjectStatisticsResponse response = new ProjectStatisticsResponse();
        response.setProjectId(projectId);
        response.setTotalTasks(totalTasks);
        response.setTodoTasks(stats.get("todo_tasks"));
        response.setInProgressTasks(stats.get("in_progress_tasks"));
        response.setDoneTasks(doneTasks);
        response.setCompletionRate(Math.round(completionRate * 10.0) / 10.0);
        response.setHighPriority(stats.get("high_priority"));
        response.setMediumPriority(stats.get("medium_priority"));
        response.setLowPriority(stats.get("low_priority"));
        response.setOverdueTasks(stats.get("overdue_tasks"));
        
        return response;
    }
}
```

### DashboardController（Thymeleaf + MyBatis）

```java
@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final StatisticsService statisticsService;
    private final ProjectService projectService;
    private final TaskService taskService;

    /**
     * ダッシュボード画面
     */
    @GetMapping
    public String dashboard(Model model, Authentication authentication) {
        String username = authentication.getName();
        Long userId = userService.getUserIdByUsername(username);
        
        // ユーザーのプロジェクト一覧取得
        List<Project> projects = projectService.getUserProjects(username);
        
        // 各プロジェクトの統計情報取得
        List<ProjectStatisticsResponse> statistics = projects.stream()
                .map(p -> statisticsService.getProjectStatistics(p.getId()))
                .toList();
        
        // 期限切れタスク取得
        List<Task> overdueTasks = taskService.getOverdueTasks(userId);
        
        model.addAttribute("projects", projects);
        model.addAttribute("statistics", statistics);
        model.addAttribute("overdueTasks", overdueTasks);
        
        return "dashboard/index";
    }
}
```

### ダッシュボードテンプレート（Chart.js統合）

**ファイルパス**: `templates/dashboard/index.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ダッシュボード - タスク管理</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
</head>
<body>
    <nav class="navbar navbar-expand-lg navbar-dark bg-primary">
        <div class="container-fluid">
            <a class="navbar-brand" href="#">📋 タスク管理</a>
            <div class="navbar-nav ms-auto">
                <span class="navbar-text text-white me-3">
                    <i class="bi bi-person-circle"></i> 
                    <span sec:authentication="name">ユーザー</span>
                </span>
                <a class="nav-link" th:href="@{/logout}">ログアウト</a>
            </div>
        </div>
    </nav>

    <div class="container-fluid mt-4">
        <div class="row">
            <!-- サイドバー -->
            <div class="col-md-2">
                <div class="list-group">
                    <a th:href="@{/dashboard}" class="list-group-item list-group-item-action active">
                        <i class="bi bi-speedometer2"></i> ダッシュボード
                    </a>
                    <a th:href="@{/projects}" class="list-group-item list-group-item-action">
                        <i class="bi bi-folder"></i> プロジェクト
                    </a>
                    <a th:href="@{/tasks}" class="list-group-item list-group-item-action">
                        <i class="bi bi-list-task"></i> タスク
                    </a>
                </div>
            </div>

            <!-- メインコンテンツ -->
            <div class="col-md-10">
                <h1 class="mb-4">ダッシュボード</h1>

                <!-- アラート: 期限切れタスク -->
                <div th:if="${!#lists.isEmpty(overdueTasks)}" class="alert alert-danger" role="alert">
                    <i class="bi bi-exclamation-triangle-fill"></i>
                    <strong>期限切れタスク:</strong> <span th:text="${#lists.size(overdueTasks)}">0</span>件
                    <a th:href="@{/tasks?filter=overdue}" class="alert-link">確認する</a>
                </div>

                <!-- アラート: 今日が期限のタスク -->
                <div th:if="${!#lists.isEmpty(todayTasks)}" class="alert alert-warning" role="alert">
                    <i class="bi bi-calendar-check"></i>
                    <strong>今日が期限のタスク:</strong> <span th:text="${#lists.size(todayTasks)}">0</span>件
                </div>

                <!-- プロジェクト統計カード -->
                <div class="row">
                    <div class="col-md-4 mb-4" th:each="stat : ${statistics}">
                        <div class="card h-100">
                            <div class="card-body">
                                <h5 class="card-title" th:text="${stat.projectName}">プロジェクト名</h5>
                                
                                <!-- 進捗バー -->
                                <div class="mb-3">
                                    <div class="d-flex justify-content-between mb-1">
                                        <small>進捗率</small>
                                        <small><strong th:text="${#numbers.formatDecimal(stat.completionRate, 0, 1)}">75.5</strong>%</small>
                                    </div>
                                    <div class="progress" style="height: 20px;">
                                        <div class="progress-bar" role="progressbar" 
                                             th:style="'width: ' + ${stat.completionRate} + '%'"
                                             th:classappend="${stat.completionRate >= 100 ? 'bg-success' : (stat.completionRate >= 50 ? 'bg-primary' : 'bg-warning')}">
                                        </div>
                                    </div>
                                </div>

                                <!-- タスク統計 -->
                                <div class="row text-center">
                                    <div class="col-4">
                                        <div class="text-muted small">TODO</div>
                                        <div class="h4" th:text="${stat.todoTasks}">5</div>
                                    </div>
                                    <div class="col-4">
                                        <div class="text-muted small">進行中</div>
                                        <div class="h4 text-primary" th:text="${stat.inProgressTasks}">3</div>
                                    </div>
                                    <div class="col-4">
                                        <div class="text-muted small">完了</div>
                                        <div class="h4 text-success" th:text="${stat.doneTasks}">12</div>
                                    </div>
                                </div>

                                <!-- 期限切れタスク -->
                                <div th:if="${stat.overdueTasks > 0}" class="mt-3">
                                    <span class="badge bg-danger">
                                        ⚠️ 期限切れ: <span th:text="${stat.overdueTasks}">2</span>件
                                    </span>
                                </div>
                            </div>
                            <div class="card-footer">
                                <a th:href="@{/dashboard/projects/{id}(id=${stat.projectId})}" class="btn btn-sm btn-outline-primary">
                                    詳細を見る <i class="bi bi-arrow-right"></i>
                                </a>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 全体統計グラフ -->
                <div class="row mt-4">
                    <div class="col-md-6">
                        <div class="card">
                            <div class="card-header">
                                <h5>ステータス別タスク数</h5>
                            </div>
                            <div class="card-body">
                                <canvas id="statusChart"></canvas>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="card">
                            <div class="card-header">
                                <h5>優先度別タスク数</h5>
                            </div>
                            <div class="card-body">
                                <canvas id="priorityChart"></canvas>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script th:inline="javascript">
        // ステータス別グラフ
        const totalTodo = /*[[${statistics.stream().mapToLong(s -> s.todoTasks).sum()}]]*/ 0;
        const totalInProgress = /*[[${statistics.stream().mapToLong(s -> s.inProgressTasks).sum()}]]*/ 0;
        const totalDone = /*[[${statistics.stream().mapToLong(s -> s.doneTasks).sum()}]]*/ 0;

        new Chart(document.getElementById('statusChart'), {
            type: 'doughnut',
            data: {
                labels: ['TODO', '進行中', '完了'],
                datasets: [{
                    data: [totalTodo, totalInProgress, totalDone],
                    backgroundColor: ['#6c757d', '#0d6efd', '#198754']
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: true
            }
        });

        // 優先度別グラフ（例）
        new Chart(document.getElementById('priorityChart'), {
            type: 'bar',
            data: {
                labels: ['高', '中', '低'],
                datasets: [{
                    label: 'タスク数',
                    data: [8, 15, 5],
                    backgroundColor: ['#dc3545', '#ffc107', '#6c757d']
                }]
            },
            options: {
                responsive: true,
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        });
    </script>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
```

### ファイルアップロード機能のThymeleaf実装

**ファイルパス**: `templates/tasks/attachments.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>タスク添付ファイル</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <div class="container mt-5">
        <h2>タスク添付ファイル: <span th:text="${task.title}">タスク名</span></h2>

        <!-- ファイルアップロードフォーム -->
        <div class="card mb-4">
            <div class="card-header">
                <h5>ファイルをアップロード</h5>
            </div>
            <div class="card-body">
                <form th:action="@{/tasks/{id}/attachments(id=${task.id})}" 
                      method="post" 
                      enctype="multipart/form-data">
                    <div class="mb-3">
                        <label for="file" class="form-label">ファイル選択</label>
                        <input class="form-control" type="file" id="file" name="file" required>
                        <div class="form-text">最大サイズ: 10MB</div>
                    </div>
                    <button type="submit" class="btn btn-primary">
                        <i class="bi bi-upload"></i> アップロード
                    </button>
                </form>
            </div>
        </div>

        <!-- 添付ファイル一覧 -->
        <div class="card">
            <div class="card-header">
                <h5>添付ファイル一覧</h5>
            </div>
            <ul class="list-group list-group-flush">
                <li class="list-group-item" th:each="attachment : ${attachments}">
                    <div class="d-flex justify-content-between align-items-center">
                        <div>
                            <i class="bi bi-file-earmark"></i>
                            <strong th:text="${attachment.originalFilename}">ファイル名.pdf</strong>
                            <br>
                            <small class="text-muted">
                                サイズ: <span th:text="${#numbers.formatDecimal(attachment.fileSize / 1024, 0, 2)}">1.5</span> KB |
                                アップロード日時: <span th:text="${#temporals.format(attachment.uploadedAt, 'yyyy/MM/dd HH:mm')}">2025/01/01 12:00</span> |
                                アップロード者: <span th:text="${attachment.uploadedBy.name}">ユーザー名</span>
                            </small>
                        </div>
                        <div>
                            <a th:href="@{/tasks/{taskId}/attachments/{attachmentId}/download(taskId=${task.id}, attachmentId=${attachment.id})}" 
                               class="btn btn-sm btn-outline-primary">
                                <i class="bi bi-download"></i> ダウンロード
                            </a>
                            <form th:action="@{/tasks/{taskId}/attachments/{attachmentId}(taskId=${task.id}, attachmentId=${attachment.id})}" 
                                  method="post" 
                                  style="display: inline;"
                                  onsubmit="return confirm('本当に削除しますか？');">
                                <input type="hidden" name="_method" value="delete">
                                <button type="submit" class="btn btn-sm btn-outline-danger">
                                    <i class="bi bi-trash"></i> 削除
                                </button>
                            </form>
                        </div>
                    </div>
                </li>
                <li th:if="${#lists.isEmpty(attachments)}" class="list-group-item text-muted">
                    添付ファイルはありません
                </li>
            </ul>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
```

> **💡 実装のポイント**:
> - **Chart.js**: グラフ表示にChart.jsを使用
> - **Bootstrap Icons**: アイコンでUIを分かりやすく
> - **Thymeleaf Inline JavaScript**: サーバーサイドのデータをJavaScriptに渡す
> - **Progress Bar**: 進捗状況を視覚的に表示
> - **ファイルアップロード**: `multipart/form-data`でファイルを送信
>
> Phase 5で学んだThymeleafの知識を活かして、データビジュアライゼーションやファイル管理機能も実装できます！

---

## 🎨 チャレンジ課題

### チャレンジ 1: ユーザーダッシュボード

ユーザーごとの統計（担当タスク数、完了率、期限切れタスク数など）を表示するAPIを実装してください。

### チャレンジ 2: Export機能

タスク一覧をCSVファイルとしてエクスポートする機能を実装してください。

**ヒント**: Apache Commons CSVを使用

### チャレンジ 3: WebSocket通知

期限間近のタスクをリアルタイムでブラウザに通知する機能を実装してください。

**ヒント**: Spring WebSocketを使用

### チャレンジ 4: 全文検索

Elasticsearchを使ってタスクの全文検索を実装してください。

---

## 📚 このステップで学んだこと

- ✅ 統計情報の集計とキャッシング
- ✅ ファイルアップロード・ダウンロード
- ✅ スケジュールタスクの実装
- ✅ 非同期メール送信
- ✅ パフォーマンス最適化

---

## 🔄 Gitへのコミット

```bash
git add .
git commit -m "Step 37: 高度な機能実装完了"
git push origin main
```

---

## ➡️ 次のステップ

次は[Step 38: 総合テストとデプロイ準備](STEP_38.md)へ進みましょう！

---

お疲れさまでした！ 🎉
