# Step 36: サービスとコントローラー実装（Thymeleaf + MyBatis）

## 🎯 このステップの目標

- **MyBatis Mapper XML**で動的SQLを実装する
- **Service層**でビジネスロジックを実装する
- **Thymeleafコントローラー**で画面制御を実装する
- DTOを使ったデータ変換を理解する

**所要時間**: 約4時間

> **このステップはあなたが実装します！**
> 
> STEP_35で作成したMapperを使って、実際のビジネスロジックと画面を実装してください。
> `example/STEP_36_*.java`と`example/STEP_36_*.html`に実装例があります。

---

## 📋 実装課題

### 課題1: MyBatis XML Mapper（動的SQL）

タスク検索など複雑なクエリはXMLで実装します。

**考えてほしいこと：**
- どんな条件で検索できるようにするか？
- プロジェクトID、ステータス、優先度、担当者、キーワード...
- `<if>`タグを使った動的WHERE句の書き方
- JOINでプロジェクト名や担当者名も取得するには？

`src/main/resources/mapper/TaskMapper.xml`を作成して実装してください。

> **💡 ヒント**: `example/STEP_36_TaskMapper.xml`に完全な実装例があります

---

### 課題2: DTOクラスの実装

検索条件やリクエストデータを扱うDTOを実装してください。

**必要なDTO：**
- `TaskSearchCriteria` - 検索条件を保持
- `TaskCreateRequest` - タスク作成リクエスト
- `ProjectCreateRequest` - プロジェクト作成リクエスト

**考えてほしいこと：**
- どんなフィールドが必要か？
- バリデーション（`@NotBlank`, `@Size`など）をどう設定するか？

> **💡 ヒント**: `example/STEP_36_service_example.java`のDTO部分に実装例があります

---

### 課題3: Service層の実装

ビジネスロジックを実装してください。

**実装するService：**
- `TaskService` - タスクのCRUD、検索、ステータス更新
- `ProjectService` - プロジェクトのCRUD
- `CommentService` - コメントの追加・取得

**考えてほしいこと：**
- どのメソッドに`@Transactional`が必要か？
- エラー処理（存在しないIDなど）をどうするか？
- Mapperからのデータをどう加工するか？

> **💡 ヒント**: `example/STEP_36_service_example.java`に完全な実装例があります

---

### 課題4: Thymeleafコントローラーの実装

画面制御を行うコントローラーを実装してください。

**実装するController：**
- `TaskController` - タスク一覧、詳細、作成、更新
- `ProjectController` - プロジェクト一覧、詳細、作成
- `DashboardController` - ダッシュボード画面

**考えてほしいこと：**
- どんなURLパスを設定するか？（`/tasks`, `/projects/{id}`など）
- Modelにどんなデータを詰めて画面に渡すか？
- フォーム送信後のリダイレクト先は？

> **💡 ヒント**: `example/STEP_36_controller_example.java`に完全な実装例があります
```
    /**
     * GET /tasks/new
     */
    @GetMapping("/new")
    public String newTaskForm(/* パラメータを追加 */) {
        // 実装してください
        return "tasks/form";
    }
    
    /**
     * タスク作成処理
     * POST /tasks
     */
    @PostMapping
    public String createTask(/* パラメータを追加 */) {
        // 実装してください
        return "redirect:/tasks/{id}";
    }
```

同様に`ProjectController`, `DashboardController`も実装してください。

> **💡 ヒント**: `example/STEP_36_controller_example.java`に実装例があります

---

### 課題5: Thymeleafテンプレートの実装

**タスク一覧画面** (`src/main/resources/templates/tasks/list.html`)

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>タスク一覧</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <div class="container mt-4">
        <h1>タスク一覧</h1>
        
        <!-- TODO: 検索フォームを実装 -->
        
        <!-- TODO: タスク一覧を表示 -->
        <div th:each="task : ${tasks}" class="card mb-3">
            <!-- 実装してください -->
        </div>
    </div>
</body>
</html>
```

以下のテンプレートも実装してください：
- `tasks/detail.html` - タスク詳細
- `tasks/form.html` - タスク作成/編集フォーム
- `tasks/kanban.html` - カンバンボード
- `projects/list.html` - プロジェクト一覧
- `dashboard/index.html` - ダッシュボード

> **💡 ヒント**: `example/STEP_36_templates/`に実装例があります

---

## ✅ チェックリスト

- [ ] TaskMapper.xmlで動的SQLを実装
- [ ] DTOクラス（Request, Criteria）を実装
- [ ] Service層（TaskService等）を実装
- [ ] Controller層（TaskController等）を実装
- [ ] Thymeleafテンプレート（6画面以上）を実装
- [ ] バリデーション（@Valid, @NotBlank等）を追加
- [ ] 例外ハンドリング（@ExceptionHandler）を実装

---

## 🧪 動作確認

### 1. アプリケーション起動

```bash
./mvnw spring-boot:run
```

### 2. ブラウザでアクセス

```
http://localhost:8080/dashboard
```

### 3. 動作確認項目

- [ ] ログイン画面が表示される
- [ ] ダッシュボードが表示される
- [ ] プロジェクト作成ができる
- [ ] タスク作成ができる
- [ ] タスク検索ができる
- [ ] カンバンボードでドラッグ&ドロップできる

---

## 💡 参考実装例

- `example/STEP_36_TaskMapper.xml` - 動的SQL完全実装
- `example/STEP_36_dto_example.java` - DTO実装例
- `example/STEP_36_service_example.java` - Service実装例
- `example/STEP_36_controller_example.java` - Controller実装例
- `example/STEP_36_templates/` - Thymeleafテンプレート集

---

## 📚 このステップで学んだこと

- ✅ MyBatis動的SQLの実践的な使い方
- ✅ Service層でのビジネスロジック実装
- ✅ Thymeleafコントローラーのパターン
- ✅ DTOを使ったデータ変換
- ✅ バリデーションと例外ハンドリング

---

## 🔄 Gitへのコミット

```bash
git add .
git commit -m "Step 36: サービスとコントローラー実装完了

- MyBatis動的SQL（TaskMapper.xml等）
- Service層（TaskService, ProjectService等）
- Controller層（Thymeleaf）
- テンプレート（6画面）"
git push origin main
```

---

## ➡️ 次のステップ

次は[Step 37: 高度な機能実装](STEP_37.md)へ進みましょう！

---

お疲れさまでした！ 🎉
