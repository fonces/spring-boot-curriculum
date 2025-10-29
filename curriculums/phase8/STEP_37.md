# Step 37: 高度な機能実装

## 🎯 このステップの目標

- 統計・ダッシュボード機能を実装する
- ファイル添付機能を追加する
- リマインダー機能を実装する
- キャッシュを活用したパフォーマンス最適化

**所要時間**: 約2時間30分

---

## 📋 実装要件

このステップでは、タスク管理システムをより実用的にする高度な機能を実装します。

### 実装する機能一覧

1. **統計・ダッシュボード機能**
   - プロジェクトの進捗率
   - ステータス別タスク数
   - 優先度別タスク数
   - 期限切れタスク数

2. **タスク添付ファイル機能**
   - ファイルアップロード
   - ファイル一覧取得
   - ファイルダウンロード
   - ファイル削除

3. **リマインダー機能**
   - 期限間近のタスクをメール通知
   - 期限切れタスクをメール通知
   - スケジュール実行

---

## 🚀 ステップ1: 統計・ダッシュボード機能

### 1-1. ProjectStatisticsResponse DTO

**必須フィールド**:
- `projectId` (Long)
- `projectName` (String)
- `totalTasks` (Long) - 総タスク数
- `todoTasks` (Long) - 未着手タスク数
- `inProgressTasks` (Long) - 進行中タスク数
- `doneTasks` (Long) - 完了タスク数
- `completionRate` (Double) - 進捗率（%）
- `tasksByPriority` (Map<String, Long>) - 優先度別タスク数
- `overdueTasks` (Long) - 期限切れタスク数

### 1-2. StatisticsService

**必須メソッド**:
```java
@Cacheable(value = "statistics", key = "'project-' + #projectId")
ProjectStatisticsResponse getProjectStatistics(Long projectId)
```

**実装のポイント**:
- `TaskRepository.getTaskStatisticsByProject()`を使用
- ステータスごとの件数をMapに変換
- 進捗率を計算（完了タスク数 ÷ 総タスク数 × 100）
- 優先度別タスク数を集計
- 期限切れタスク数をカウント（dueDate < 今日 AND status != DONE）
- キャッシュを使ってパフォーマンス向上

### 1-3. StatisticsController

**エンドポイント**:
- `GET /api/statistics/projects/{projectId}` - プロジェクト統計

**配置場所**:
- DTO: `src/main/java/com/example/hellospringboot/dto/response/`
- Service: `src/main/java/com/example/hellospringboot/service/`
- Controller: `src/main/java/com/example/hellospringboot/controller/`

---

## 🚀 ステップ2: タスク添付ファイル機能

### 2-1. TaskAttachment エンティティ

**必須フィールド**:
- `id` (Long)
- `task` (Task) - ManyToOne
- `filename` (String) - サーバー上のファイル名
- `originalFilename` (String) - 元のファイル名
- `contentType` (String) - MIMEタイプ
- `fileSize` (Long) - ファイルサイズ（バイト）
- `uploadedBy` (User) - ManyToOne
- `uploadedAt` (LocalDateTime)

### 2-2. TaskAttachmentRepository

```java
public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, Long> {
    List<TaskAttachment> findByTaskId(Long taskId);
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

## 💡 補足: Thymeleafでのダッシュボード実装

Phase 5でThymeleafを学習した場合、ダッシュボード画面もThymeleafで実装できます。

### Thymeleafダッシュボードコントローラー

```java
@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardWebController {

    private final StatisticsService statisticsService;
    private final ProjectService projectService;
    private final TaskService taskService;

    /**
     * ダッシュボード画面
     */
    @GetMapping
    public String dashboard(Model model, Authentication authentication) {
        String username = authentication.getName();
        
        // ユーザーのプロジェクト一覧取得
        List<ProjectResponse> projects = projectService.getUserProjects(username);
        
        // 各プロジェクトの統計情報取得
        List<ProjectStatisticsResponse> statistics = projects.stream()
                .map(p -> statisticsService.getProjectStatistics(p.getId()))
                .toList();
        
        // 期限切れタスク取得
        List<TaskResponse> overdueTasks = taskService.getOverdueTasks(username);
        
        // 今日が期限のタスク取得
        List<TaskResponse> todayTasks = taskService.getTasksDueToday(username);
        
        model.addAttribute("projects", projects);
        model.addAttribute("statistics", statistics);
        model.addAttribute("overdueTasks", overdueTasks);
        model.addAttribute("todayTasks", todayTasks);
        
        return "dashboard/index";
    }

    /**
     * プロジェクト詳細ダッシュボード
     */
    @GetMapping("/projects/{id}")
    public String projectDashboard(@PathVariable Long id, Model model) {
        ProjectResponse project = projectService.getProjectById(id);
        ProjectStatisticsResponse statistics = statisticsService.getProjectStatistics(id);
        List<TaskResponse> recentTasks = taskService.getRecentTasks(id, 10);
        
        model.addAttribute("project", project);
        model.addAttribute("statistics", statistics);
        model.addAttribute("recentTasks", recentTasks);
        
        return "dashboard/project-detail";
    }
}
```

### ダッシュボードテンプレート

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
