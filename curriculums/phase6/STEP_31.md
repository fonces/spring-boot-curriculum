# Step 31: 高度な機能実装

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
git commit -m "Phase 6: STEP_31完了（高度な機能実装）"
git push origin main
```

---

## ➡️ 次のステップ

次は[Step 32: 総合テストとデプロイ準備](STEP_32.md)へ進みましょう！

---

お疲れさまでした！ 🎉
