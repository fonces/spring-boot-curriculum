# Step 24: Thymeleaf + REST API連携

## 🎯 このステップの目標

- JavaScriptでREST APIを呼び出す方法を理解する
- Fetch APIを使ったJSON形式のデータ取得・送信ができる
- DOM操作でページを動的に更新できる
- ThymeleafとREST APIの使い分けを理解する

**所要時間**: 約1時間

---

## 📋 事前準備

このステップを始める前に、以下が完了していることを確認してください：

- Step 21: Thymeleafの基礎（テンプレート構文の理解）
- Step 22: フォーム送信とバリデーション
- Step 23: レイアウトとフラグメント
- JavaScriptの基本的な構文理解

---

## 🎨 完成イメージ

このステップでは、**リロードなしで動作するタスク管理画面**を作成します：

### 機能一覧
- ✅ タスク一覧の表示（初回はThymeleafでレンダリング）
- ✅ タスクの追加（Fetch APIでPOST → DOM更新）
- ✅ タスクの完了/未完了切り替え（Fetch APIでPUT → DOM更新）
- ✅ タスクの削除（Fetch APIでDELETE → DOM更新）
- ✅ ページリロードなしですべての操作が完了

**技術構成**:
- **サーバーサイド**: Spring Boot + Thymeleaf（初回表示）
- **クライアントサイド**: JavaScript（Fetch API）+ REST API（動的操作）

---

## 🚀 ステップ1: プロジェクトの準備

### 1-1. 依存関係の確認

`pom.xml`に以下の依存関係が含まれていることを確認：

```xml
<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Thymeleaf -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
    
    <!-- Spring Data JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- MySQL Driver -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    
    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
</dependencies>
```

---

## 🗄️ ステップ2: タスクエンティティとリポジトリの作成

### 2-1. Taskエンティティ

**ファイルパス**: `src/main/java/com/example/taskapp/entity/Task.java`

```java
package com.example.taskapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(length = 1000)
    private String description;
    
    @Column(nullable = false)
    private Boolean completed = false;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### 2-2. Taskリポジトリ

**ファイルパス**: `src/main/java/com/example/taskapp/repository/TaskRepository.java`

```java
package com.example.taskapp.repository;

import com.example.taskapp.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    // 完了状態で絞り込み
    List<Task> findByCompleted(Boolean completed);
    
    // 作成日時の降順で全件取得
    List<Task> findAllByOrderByCreatedAtDesc();
}
```

---

## 🎯 ステップ3: DTO（データ転送オブジェクト）の作成

### 3-1. TaskRequestDTO

**ファイルパス**: `src/main/java/com/example/taskapp/dto/TaskRequestDTO.java`

```java
package com.example.taskapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequestDTO {
    
    @NotBlank(message = "タイトルは必須です")
    @Size(max = 100, message = "タイトルは100文字以内で入力してください")
    private String title;
    
    @Size(max = 1000, message = "説明は1000文字以内で入力してください")
    private String description;
}
```

### 3-2. TaskResponseDTO

**ファイルパス**: `src/main/java/com/example/taskapp/dto/TaskResponseDTO.java`

```java
package com.example.taskapp.dto;

import com.example.taskapp.entity.Task;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponseDTO {
    
    private Long id;
    private String title;
    private String description;
    private Boolean completed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Entityから変換するコンストラクタ
    public TaskResponseDTO(Task task) {
        this.id = task.getId();
        this.title = task.getTitle();
        this.description = task.getDescription();
        this.completed = task.getCompleted();
        this.createdAt = task.getCreatedAt();
        this.updatedAt = task.getUpdatedAt();
    }
}
```

---

## 💼 ステップ4: サービス層の実装

**ファイルパス**: `src/main/java/com/example/taskapp/service/TaskService.java`

```java
package com.example.taskapp.service;

import com.example.taskapp.dto.TaskRequestDTO;
import com.example.taskapp.dto.TaskResponseDTO;
import com.example.taskapp.entity.Task;
import com.example.taskapp.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {
    
    private final TaskRepository taskRepository;
    
    // 全件取得（新しい順）
    public List<TaskResponseDTO> getAllTasks() {
        return taskRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(TaskResponseDTO::new)
                .collect(Collectors.toList());
    }
    
    // ID指定で1件取得
    public TaskResponseDTO getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("タスクが見つかりません: ID=" + id));
        return new TaskResponseDTO(task);
    }
    
    // タスク作成
    @Transactional
    public TaskResponseDTO createTask(TaskRequestDTO requestDTO) {
        Task task = new Task();
        task.setTitle(requestDTO.getTitle());
        task.setDescription(requestDTO.getDescription());
        task.setCompleted(false);
        
        Task savedTask = taskRepository.save(task);
        return new TaskResponseDTO(savedTask);
    }
    
    // タスク更新
    @Transactional
    public TaskResponseDTO updateTask(Long id, TaskRequestDTO requestDTO) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("タスクが見つかりません: ID=" + id));
        
        task.setTitle(requestDTO.getTitle());
        task.setDescription(requestDTO.getDescription());
        
        Task updatedTask = taskRepository.save(task);
        return new TaskResponseDTO(updatedTask);
    }
    
    // 完了状態の切り替え
    @Transactional
    public TaskResponseDTO toggleTaskCompletion(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("タスクが見つかりません: ID=" + id));
        
        task.setCompleted(!task.getCompleted());
        
        Task updatedTask = taskRepository.save(task);
        return new TaskResponseDTO(updatedTask);
    }
    
    // タスク削除
    @Transactional
    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new RuntimeException("タスクが見つかりません: ID=" + id);
        }
        taskRepository.deleteById(id);
    }
}
```

---

## 🎮 ステップ5: コントローラーの実装

### 5-1. REST APIコントローラー

**ファイルパス**: `src/main/java/com/example/taskapp/controller/TaskApiController.java`

```java
package com.example.taskapp.controller;

import com.example.taskapp.dto.TaskRequestDTO;
import com.example.taskapp.dto.TaskResponseDTO;
import com.example.taskapp.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskApiController {
    
    private final TaskService taskService;
    
    // 全件取得
    @GetMapping
    public ResponseEntity<List<TaskResponseDTO>> getAllTasks() {
        List<TaskResponseDTO> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }
    
    // ID指定で取得
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> getTaskById(@PathVariable Long id) {
        TaskResponseDTO task = taskService.getTaskById(id);
        return ResponseEntity.ok(task);
    }
    
    // タスク作成
    @PostMapping
    public ResponseEntity<TaskResponseDTO> createTask(@Valid @RequestBody TaskRequestDTO requestDTO) {
        TaskResponseDTO createdTask = taskService.createTask(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }
    
    // タスク更新
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequestDTO requestDTO) {
        TaskResponseDTO updatedTask = taskService.updateTask(id, requestDTO);
        return ResponseEntity.ok(updatedTask);
    }
    
    // 完了状態の切り替え
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<TaskResponseDTO> toggleTaskCompletion(@PathVariable Long id) {
        TaskResponseDTO updatedTask = taskService.toggleTaskCompletion(id);
        return ResponseEntity.ok(updatedTask);
    }
    
    // タスク削除
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
```

### 5-2. Thymeleafコントローラー

**ファイルパス**: `src/main/java/com/example/taskapp/controller/TaskViewController.java`

```java
package com.example.taskapp.controller;

import com.example.taskapp.dto.TaskResponseDTO;
import com.example.taskapp.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class TaskViewController {
    
    private final TaskService taskService;
    
    @GetMapping("/tasks")
    public String taskPage(Model model) {
        List<TaskResponseDTO> tasks = taskService.getAllTasks();
        model.addAttribute("tasks", tasks);
        return "tasks";
    }
}
```

---

## 🎨 ステップ6: Thymeleafテンプレートの作成

**ファイルパス**: `src/main/resources/templates/tasks.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>タスク管理アプリ</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            padding: 20px;
        }
        
        .container {
            max-width: 800px;
            margin: 0 auto;
            background: white;
            border-radius: 20px;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
            padding: 30px;
        }
        
        h1 {
            color: #333;
            text-align: center;
            margin-bottom: 30px;
            font-size: 2.5rem;
        }
        
        .task-form {
            display: flex;
            gap: 10px;
            margin-bottom: 30px;
        }
        
        #task-title {
            flex: 1;
            padding: 12px 20px;
            border: 2px solid #e0e0e0;
            border-radius: 10px;
            font-size: 16px;
            transition: border-color 0.3s;
        }
        
        #task-title:focus {
            outline: none;
            border-color: #667eea;
        }
        
        #add-task-btn {
            padding: 12px 30px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border: none;
            border-radius: 10px;
            font-size: 16px;
            font-weight: bold;
            cursor: pointer;
            transition: transform 0.2s;
        }
        
        #add-task-btn:hover {
            transform: translateY(-2px);
        }
        
        #add-task-btn:active {
            transform: translateY(0);
        }
        
        .task-list {
            list-style: none;
        }
        
        .task-item {
            background: #f8f9fa;
            padding: 15px;
            margin-bottom: 10px;
            border-radius: 10px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            transition: all 0.3s;
        }
        
        .task-item:hover {
            transform: translateX(5px);
            box-shadow: 0 5px 15px rgba(0,0,0,0.1);
        }
        
        .task-item.completed {
            background: #e8f5e9;
        }
        
        .task-content {
            flex: 1;
            display: flex;
            align-items: center;
            gap: 15px;
        }
        
        .task-checkbox {
            width: 24px;
            height: 24px;
            cursor: pointer;
        }
        
        .task-title {
            font-size: 18px;
            color: #333;
        }
        
        .task-item.completed .task-title {
            text-decoration: line-through;
            color: #999;
        }
        
        .task-actions {
            display: flex;
            gap: 10px;
        }
        
        .delete-btn {
            padding: 8px 16px;
            background: #f44336;
            color: white;
            border: none;
            border-radius: 5px;
            cursor: pointer;
            transition: background 0.3s;
        }
        
        .delete-btn:hover {
            background: #d32f2f;
        }
        
        .empty-message {
            text-align: center;
            color: #999;
            font-size: 18px;
            padding: 40px;
        }
        
        .error-message {
            background: #ffebee;
            color: #c62828;
            padding: 12px;
            border-radius: 8px;
            margin-bottom: 20px;
            display: none;
        }
        
        .success-message {
            background: #e8f5e9;
            color: #2e7d32;
            padding: 12px;
            border-radius: 8px;
            margin-bottom: 20px;
            display: none;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>📝 タスク管理</h1>
        
        <!-- メッセージ表示エリア -->
        <div id="error-message" class="error-message"></div>
        <div id="success-message" class="success-message"></div>
        
        <!-- タスク追加フォーム -->
        <div class="task-form">
            <input 
                type="text" 
                id="task-title" 
                placeholder="新しいタスクを入力..."
                maxlength="100">
            <button id="add-task-btn">追加</button>
        </div>
        
        <!-- タスク一覧 -->
        <ul id="task-list" class="task-list">
            <!-- 初回表示: Thymeleafでレンダリング -->
            <li th:each="task : ${tasks}" 
                th:id="'task-' + ${task.id}"
                th:class="${task.completed} ? 'task-item completed' : 'task-item'"
                th:data-task-id="${task.id}">
                <div class="task-content">
                    <input 
                        type="checkbox" 
                        class="task-checkbox"
                        th:checked="${task.completed}"
                        th:onchange="'toggleTask(' + ${task.id} + ')'">
                    <span class="task-title" th:text="${task.title}"></span>
                </div>
                <div class="task-actions">
                    <button 
                        class="delete-btn" 
                        th:onclick="'deleteTask(' + ${task.id} + ')'">
                        削除
                    </button>
                </div>
            </li>
            
            <!-- タスクがない場合のメッセージ -->
            <li th:if="${#lists.isEmpty(tasks)}" class="empty-message">
                タスクがありません。上のフォームから追加してください！
            </li>
        </ul>
    </div>
    
    <script>
        // タスク追加
        document.getElementById('add-task-btn').addEventListener('click', addTask);
        document.getElementById('task-title').addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                addTask();
            }
        });
        
        async function addTask() {
            const titleInput = document.getElementById('task-title');
            const title = titleInput.value.trim();
            
            if (!title) {
                showError('タスクのタイトルを入力してください');
                return;
            }
            
            try {
                const response = await fetch('/api/tasks', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ title: title, description: '' })
                });
                
                if (!response.ok) {
                    throw new Error('タスクの追加に失敗しました');
                }
                
                const task = await response.json();
                
                // DOMに追加
                addTaskToDOM(task);
                
                // 入力欄をクリア
                titleInput.value = '';
                
                // 空メッセージを削除
                removeEmptyMessage();
                
                showSuccess('タスクを追加しました！');
                
            } catch (error) {
                showError(error.message);
            }
        }
        
        // タスクの完了/未完了切り替え
        async function toggleTask(taskId) {
            try {
                const response = await fetch(`/api/tasks/${taskId}/toggle`, {
                    method: 'PATCH'
                });
                
                if (!response.ok) {
                    throw new Error('ステータスの更新に失敗しました');
                }
                
                const task = await response.json();
                
                // DOMを更新
                const taskElement = document.getElementById(`task-${taskId}`);
                if (task.completed) {
                    taskElement.classList.add('completed');
                } else {
                    taskElement.classList.remove('completed');
                }
                
            } catch (error) {
                showError(error.message);
            }
        }
        
        // タスク削除
        async function deleteTask(taskId) {
            if (!confirm('このタスクを削除してもよろしいですか？')) {
                return;
            }
            
            try {
                const response = await fetch(`/api/tasks/${taskId}`, {
                    method: 'DELETE'
                });
                
                if (!response.ok) {
                    throw new Error('タスクの削除に失敗しました');
                }
                
                // DOMから削除
                const taskElement = document.getElementById(`task-${taskId}`);
                taskElement.style.transition = 'all 0.3s';
                taskElement.style.opacity = '0';
                taskElement.style.transform = 'translateX(-100%)';
                
                setTimeout(() => {
                    taskElement.remove();
                    
                    // タスクが0件になったら空メッセージを表示
                    const taskList = document.getElementById('task-list');
                    if (taskList.children.length === 0) {
                        showEmptyMessage();
                    }
                }, 300);
                
                showSuccess('タスクを削除しました');
                
            } catch (error) {
                showError(error.message);
            }
        }
        
        // DOMにタスクを追加
        function addTaskToDOM(task) {
            const taskList = document.getElementById('task-list');
            
            const li = document.createElement('li');
            li.id = `task-${task.id}`;
            li.className = task.completed ? 'task-item completed' : 'task-item';
            li.dataset.taskId = task.id;
            
            li.innerHTML = `
                <div class="task-content">
                    <input 
                        type="checkbox" 
                        class="task-checkbox"
                        ${task.completed ? 'checked' : ''}
                        onchange="toggleTask(${task.id})">
                    <span class="task-title">${escapeHtml(task.title)}</span>
                </div>
                <div class="task-actions">
                    <button class="delete-btn" onclick="deleteTask(${task.id})">削除</button>
                </div>
            `;
            
            // リストの先頭に追加（新しいタスクを上に表示）
            taskList.insertBefore(li, taskList.firstChild);
        }
        
        // 空メッセージを削除
        function removeEmptyMessage() {
            const emptyMessage = document.querySelector('.empty-message');
            if (emptyMessage) {
                emptyMessage.remove();
            }
        }
        
        // 空メッセージを表示
        function showEmptyMessage() {
            const taskList = document.getElementById('task-list');
            const li = document.createElement('li');
            li.className = 'empty-message';
            li.textContent = 'タスクがありません。上のフォームから追加してください！';
            taskList.appendChild(li);
        }
        
        // エラーメッセージ表示
        function showError(message) {
            const errorDiv = document.getElementById('error-message');
            errorDiv.textContent = message;
            errorDiv.style.display = 'block';
            
            setTimeout(() => {
                errorDiv.style.display = 'none';
            }, 3000);
        }
        
        // 成功メッセージ表示
        function showSuccess(message) {
            const successDiv = document.getElementById('success-message');
            successDiv.textContent = message;
            successDiv.style.display = 'block';
            
            setTimeout(() => {
                successDiv.style.display = 'none';
            }, 3000);
        }
        
        // XSS対策: HTMLエスケープ
        function escapeHtml(text) {
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        }
    </script>
</body>
</html>
```

---

## ▶️ ステップ7: アプリケーションの起動と確認

### 7-1. MySQLの起動

Docker Composeを使用している場合：

```bash
docker-compose up -d
```

### 7-2. アプリケーションの起動

```bash
./mvnw spring-boot:run
```

### 7-3. 動作確認

ブラウザで以下にアクセス：

```
http://localhost:8080/tasks
```

**確認すべき動作**:
1. ✅ ページが表示される
2. ✅ タスクを追加できる（リロードなし）
3. ✅ チェックボックスで完了/未完了を切り替えられる
4. ✅ 削除ボタンでタスクが削除される
5. ✅ すべての操作でページリロードが発生しない

---

## 🧪 ステップ8: REST APIの単体テスト

### 8-1. curlでAPIテスト

```bash
# 全件取得
curl http://localhost:8080/api/tasks

# タスク作成
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"カリキュラムを完了する","description":"Step 24まで頑張る"}'

# 完了状態の切り替え（IDは作成時のレスポンスから取得）
curl -X PATCH http://localhost:8080/api/tasks/1/toggle

# タスク削除
curl -X DELETE http://localhost:8080/api/tasks/1
```

### 8-2. ブラウザの開発者ツールで確認

1. ブラウザでF12キーを押して開発者ツールを開く
2. 「Network」タブを選択
3. タスクを追加・削除してみる
4. 送信されたリクエストとレスポンスを確認

**確認ポイント**:
- リクエストメソッド（POST, PATCH, DELETE）
- リクエストボディ（JSON形式）
- レスポンスステータス（200, 201, 204）
- レスポンスボディ（JSON形式）

---

## 🎨 チャレンジ課題

基本が理解できたら、以下にチャレンジしてみましょう：

### チャレンジ 1: タスクの編集機能を追加

タスクのタイトルをクリックすると、インラインで編集できるようにしてみましょう。

**ヒント**:
- `contenteditable="true"` 属性を使う
- `blur` イベントで変更を検知
- PUT `/api/tasks/{id}` で更新

### チャレンジ 2: フィルター機能を追加

「全て」「未完了」「完了済み」でタスクを絞り込めるボタンを追加してみましょう。

**ヒント**:
- クライアントサイドで配列をフィルタリング
- または、APIに `?completed=true` のようなクエリパラメータを追加

### チャレンジ 3: ローディング表示を追加

API通信中に「読み込み中...」というメッセージを表示してみましょう。

**ヒント**:
```javascript
async function addTask() {
    showLoading(true);
    try {
        // API呼び出し
    } finally {
        showLoading(false);
    }
}
```

### チャレンジ 4: タスクの詳細情報を表示

タスクをクリックすると、モーダルで説明文や作成日時を表示してみましょう。

---

## 🐛 トラブルシューティング

### エラー: "Failed to fetch"

**原因**: APIサーバーが起動していない、またはURLが間違っている

**解決策**:
1. Spring Bootアプリケーションが起動しているか確認
2. ブラウザのコンソールでエラー詳細を確認
3. `/api/tasks` のパスが正しいか確認

### タスクが追加されない

**原因**: バリデーションエラー、またはデータベース接続エラー

**解決策**:
1. ブラウザの開発者ツールでレスポンスを確認
2. サーバー側のログを確認
3. MySQLコンテナが起動しているか確認

### CORSエラーが発生する

**原因**: 異なるオリジン（ポート）からのアクセス

**解決策**: 今回は同一オリジンなので発生しないはずですが、もし発生したら以下を追加：

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:8080")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH");
    }
}
```

---

## 📚 このステップで学んだこと

### ThymeleafとREST APIの使い分け

| 用途 | Thymeleaf | REST API + JavaScript |
|------|-----------|------------------------|
| **初回ページ表示** | ⭕ 適している | ❌ 遅い（2往復必要） |
| **動的な更新** | ❌ ページリロードが必要 | ⭕ リロードなし |
| **SEO** | ⭕ サーバーサイドレンダリング | ❌ 初回はコンテンツなし |
| **開発の容易さ** | ⭕ シンプル | △ JavaScriptの知識が必要 |
| **保守性** | ⭕ テンプレートで一元管理 | △ フロントとバックで分離 |

**ベストプラクティス**:
- **初回表示**: Thymeleafでサーバーサイドレンダリング
- **動的操作**: JavaScriptでREST APIを呼び出し

### Fetch APIの基本

```javascript
// GET
const response = await fetch('/api/tasks');
const data = await response.json();

// POST
const response = await fetch('/api/tasks', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ title: 'タスク' })
});

// PATCH
await fetch(`/api/tasks/${id}/toggle`, { method: 'PATCH' });

// DELETE
await fetch(`/api/tasks/${id}`, { method: 'DELETE' });
```

### DOM操作のポイント

- `createElement()`: 新しい要素を作成
- `appendChild()`: 子要素として追加
- `insertBefore()`: 指定位置に挿入
- `remove()`: 要素を削除
- `classList.add/remove()`: クラスの追加・削除

---

## 🔄 Gitへのコミットとレビュー依頼

進捗を記録してレビューを受けましょう：

```bash
git add .
git commit -m "Step 24: Thymeleaf + REST API連携完了"
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

レビューが完了したら、**Phase 6: セキュリティとテスト**へ進みましょう！

[Step 25: Spring Securityの基礎](../phase6/STEP_25.md)で、認証・認可の基本を学びます。

---

## 💡 補足: SPAとSSRの違い

### SPA (Single Page Application)
- **例**: React, Vue.js, Angular
- **特徴**: 初回にHTMLを1つ読み込み、以降はAPIでデータのみ取得
- **メリット**: 高速、UX良好
- **デメリット**: SEO対策が必要、初回読み込みが遅い

### SSR (Server Side Rendering)
- **例**: Thymeleaf, JSP, PHP
- **特徴**: サーバーでHTMLを生成してクライアントに送信
- **メリット**: SEO対策不要、初回表示が速い
- **デメリット**: ページ遷移で全体リロード

### ハイブリッド（今回の実装）
- **初回**: ThymeleafでSSR
- **以降**: JavaScriptでAPI呼び出し
- **メリット**: 両方の良いとこ取り
- **ユースケース**: ブログ、EC サイト、管理画面など

---

お疲れさまでした！ 🎉

Thymeleaf + REST APIの連携により、モダンなWebアプリケーションの開発手法を習得できました！

次のPhase 6では、セキュリティとテストについて学んでいきます。
