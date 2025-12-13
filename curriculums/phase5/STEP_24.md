# Step 24: Thymeleaf + REST API連携

## 🎯 このステップの目標

- ThymeleafページからJavaScriptでREST APIを呼び出せる
- Fetch APIを使った非同期通信（AJAX）を実装できる
- サーバーサイドレンダリングとクライアントサイドレンダリングを使い分けられる
- リアルタイムな画面更新（ページリロードなし）を実装できる

**所要時間**: 約50分

---

## 📋 事前準備

- [Step 23: レイアウトとフラグメント](STEP_23.md)が完了していること
- JavaScriptの基礎知識（変数、関数、async/await）があること
- REST APIの概念（Phase 1-4）を理解していること

---

## 🎓 なぜThymeleafとREST APIを組み合わせるのか

### サーバーサイドレンダリング（SSR）の限界

**従来のThymeleafのみの実装**:
```
ユーザー操作 → フォーム送信 → サーバー処理 → ページ全体リロード
```

**問題点**:
- ページ全体がリロードされる（体感速度が遅い）
- 部分的な更新ができない
- リアルタイム性がない

### ハイブリッドアプローチ

**SSR（Thymeleaf）の利点**:
- 初期表示が速い
- SEOに有利
- JavaScriptなしでも動作

**CSR（JavaScript + REST API）の利点**:
- ページリロードなしで部分更新
- リアルタイムな操作性
- リッチなUI

**組み合わせ**:
```
初期表示: Thymeleafで高速レンダリング
動的操作: JavaScriptでREST API呼び出し
```

---

## 🚀 ステップ1: 非同期ユーザー検索の実装

### 1-1. 検索フォーム付きユーザー一覧

`users/list.html`に検索フォームを追加:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{layouts/base :: layout(~{::title}, ~{::main})}">
<head>
    <title>ユーザー一覧</title>
</head>
<body>
    <main>
        <h1>ユーザー一覧</h1>
        
        <!-- 検索フォーム -->
        <div style="margin-bottom: 20px;">
            <input type="text" 
                   id="searchInput" 
                   placeholder="名前で検索..."
                   style="padding: 10px; width: 300px; border: 1px solid #ddd; border-radius: 4px;">
            <button onclick="searchUsers()" 
                    style="padding: 10px 20px; background-color: #4CAF50; color: white; border: none; border-radius: 4px; cursor: pointer;">
                検索
            </button>
            <button onclick="resetSearch()" 
                    style="padding: 10px 20px; background-color: #999; color: white; border: none; border-radius: 4px; cursor: pointer; margin-left: 10px;">
                リセット
            </button>
        </div>
        
        <!-- 検索結果表示エリア -->
        <div id="loading" style="display: none; text-align: center; padding: 20px; color: #999;">
            <p>検索中...</p>
        </div>
        
        <div id="userTableContainer">
            <table th:unless="${users.empty}" style="width: 100%; border-collapse: collapse; margin-top: 20px;">
                <thead>
                    <tr>
                        <th style="border: 1px solid #ddd; padding: 12px; background-color: #4CAF50; color: white;">ID</th>
                        <th style="border: 1px solid #ddd; padding: 12px; background-color: #4CAF50; color: white;">名前</th>
                        <th style="border: 1px solid #ddd; padding: 12px; background-color: #4CAF50; color: white;">メールアドレス</th>
                        <th style="border: 1px solid #ddd; padding: 12px; background-color: #4CAF50; color: white;">年齢</th>
                        <th style="border: 1px solid #ddd; padding: 12px; background-color: #4CAF50; color: white;">操作</th>
                    </tr>
                </thead>
                <tbody id="userTableBody">
                    <tr th:each="user : ${users}">
                        <td style="border: 1px solid #ddd; padding: 12px;" th:text="${user.id}">1</td>
                        <td style="border: 1px solid #ddd; padding: 12px;" th:text="${user.name}">田中太郎</td>
                        <td style="border: 1px solid #ddd; padding: 12px;" th:text="${user.email}">tanaka@example.com</td>
                        <td style="border: 1px solid #ddd; padding: 12px;" th:text="${user.age}">25</td>
                        <td style="border: 1px solid #ddd; padding: 12px;">
                            <a href="javascript:void(0)" 
                               th:data-user-id="${user.id}"
                               onclick="showUserDetail(this.getAttribute('data-user-id'))" 
                               style="color: #4CAF50; text-decoration: none;">
                                詳細
                            </a>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
        
        <p>合計: <span id="userCount" th:text="${users.size()}">0</span> 件</p>
        
        <script>
            // 非同期でユーザー検索
            async function searchUsers() {
                const searchInput = document.getElementById('searchInput');
                const keyword = searchInput.value.trim();
                
                if (!keyword) {
                    alert('検索キーワードを入力してください');
                    return;
                }
                
                // ローディング表示
                document.getElementById('loading').style.display = 'block';
                document.getElementById('userTableContainer').style.display = 'none';
                
                try {
                    // REST APIを呼び出し
                    const response = await fetch(`/api/users/search?name=${encodeURIComponent(keyword)}`);
                    
                    if (!response.ok) {
                        throw new Error('検索に失敗しました');
                    }
                    
                    const users = await response.json();
                    
                    // テーブルを更新
                    updateUserTable(users);
                    
                } catch (error) {
                    console.error('Error:', error);
                    alert('検索中にエラーが発生しました');
                } finally {
                    // ローディング非表示
                    document.getElementById('loading').style.display = 'none';
                    document.getElementById('userTableContainer').style.display = 'block';
                }
            }
            
            // テーブルを更新
            function updateUserTable(users) {
                const tbody = document.getElementById('userTableBody');
                const countSpan = document.getElementById('userCount');
                
                // テーブルをクリア
                tbody.innerHTML = '';
                
                // 検索結果がない場合
                if (users.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="5" style="text-align: center; padding: 20px; color: #999;">該当するユーザーが見つかりませんでした</td></tr>';
                    countSpan.textContent = '0';
                    return;
                }
                
                // 各ユーザーを行として追加
                users.forEach(user => {
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td style="border: 1px solid #ddd; padding: 12px;">${user.id}</td>
                        <td style="border: 1px solid #ddd; padding: 12px;">${escapeHtml(user.name)}</td>
                        <td style="border: 1px solid #ddd; padding: 12px;">${escapeHtml(user.email)}</td>
                        <td style="border: 1px solid #ddd; padding: 12px;">${user.age}</td>
                        <td style="border: 1px solid #ddd; padding: 12px;">
                            <a href="/views/users/${user.id}" style="color: #4CAF50; text-decoration: none;">詳細</a>
                        </td>
                    `;
                    tbody.appendChild(row);
                });
                
                // 件数を更新
                countSpan.textContent = users.length;
            }
            
            // XSS対策: HTMLエスケープ
            function escapeHtml(text) {
                const div = document.createElement('div');
                div.textContent = text;
                return div.innerHTML;
            }
            
            // 検索をリセット
            function resetSearch() {
                document.getElementById('searchInput').value = '';
                window.location.reload();
            }
            
            // Enterキーで検索
            document.getElementById('searchInput').addEventListener('keypress', function(event) {
                if (event.key === 'Enter') {
                    searchUsers();
                }
            });
        </script>
    </main>
</body>
</html>
```

### 1-2. JavaScriptコードの解説

#### Fetch API
```javascript
const response = await fetch('/api/users/search?name=' + keyword);
const users = await response.json();
```
- `fetch()`: 非同期HTTPリクエストを送信
- `await`: Promiseの結果を待つ（async関数内でのみ使用可能）
- `response.json()`: レスポンスをJSON形式でパース

#### DOM操作
```javascript
tbody.innerHTML = '';  // 既存の行を削除
tbody.appendChild(row);  // 新しい行を追加
```

#### XSS対策
```javascript
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
```
- ユーザー入力をそのままHTMLに挿入すると危険
- `textContent`を使ってエスケープ

---

## 🚀 ステップ2: インライン削除機能

### 2-1. 削除ボタンの追加

`users/list.html`の操作列を修正:

```html
<td style="border: 1px solid #ddd; padding: 12px;">
    <a href="javascript:void(0)" 
       th:data-user-id="${user.id}"
       onclick="showUserDetail(this.getAttribute('data-user-id'))" 
       style="color: #4CAF50; text-decoration: none; margin-right: 10px;">
        詳細
    </a>
    <button th:data-user-id="${user.id}"
            th:data-user-name="${user.name}"
            onclick="deleteUser(this.getAttribute('data-user-id'), this.getAttribute('data-user-name'))" 
            style="background-color: #d32f2f; color: white; border: none; padding: 5px 10px; border-radius: 4px; cursor: pointer;">
        削除
    </button>
</td>
```

### 2-2. 削除処理JavaScript

`<script>`タグに以下を追加:

```javascript
// ユーザーを削除
async function deleteUser(userId, userName) {
    // 確認ダイアログ
    if (!confirm(`${userName} を削除しますか？`)) {
        return;
    }
    
    try {
        const response = await fetch(`/api/users/${userId}`, {
            method: 'DELETE'
        });
        
        if (!response.ok) {
            throw new Error('削除に失敗しました');
        }
        
        // 成功: 該当行を削除
        alert('削除しました');
        
        // テーブルから行を削除（DOM操作）
        const row = event.target.closest('tr');
        row.remove();
        
        // 件数を更新
        const countSpan = document.getElementById('userCount');
        const currentCount = parseInt(countSpan.textContent);
        countSpan.textContent = currentCount - 1;
        
    } catch (error) {
        console.error('Error:', error);
        alert('削除中にエラーが発生しました');
    }
}
```

### 2-3. Thymeleafインライン式の解説

#### `th:data-*`属性を使う理由

Thymeleaf 3.xでは、セキュリティ上の理由から`th:onclick`内で変数を文字列連結することが禁止されています。

**NG: エラーになる**:
```html
<!-- ❌ Thymeleafがセキュリティエラーを出す -->
<button th:onclick="'deleteUser(' + ${user.id} + ', \'' + ${user.name} + '\')'">
```

**エラーメッセージ**:
```
TemplateProcessingException: Only variable expressions returning numbers or booleans 
are allowed in this context
```

**OK: data属性を使う**:
```html
<!-- ✅ data属性でデータを渡し、JavaScriptで取得 -->
<button th:data-user-id="${user.id}"
        th:data-user-name="${user.name}"
        onclick="deleteUser(this.getAttribute('data-user-id'), this.getAttribute('data-user-name'))">
```

#### data属性のメリット

1. **XSS対策**: HTMLエスケープが自動的に適用される
2. **セキュリティ**: Thymeleafの厳格なチェックを通過
3. **可読性**: データと処理が分離される

#### JavaScriptでの取得方法

```javascript
// 方法1: getAttribute()
const userId = this.getAttribute('data-user-id');

// 方法2: dataset API（推奨）
const userId = this.dataset.userId;  // data-user-id → userId
```

---

## 🚀 ステップ3: モーダルダイアログで詳細表示

### 3-1. モーダルHTML構造

`users/list.html`の`<main>`内に追加:

```html
<!-- モーダルダイアログ -->
<div id="userModal" style="display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background-color: rgba(0,0,0,0.5); z-index: 1000;">
    <div style="background-color: white; width: 500px; margin: 100px auto; padding: 30px; border-radius: 8px; position: relative;">
        <button onclick="closeModal()" 
                style="position: absolute; top: 10px; right: 10px; background: none; border: none; font-size: 24px; cursor: pointer;">
            &times;
        </button>
        
        <h2>ユーザー詳細</h2>
        
        <div id="modalContent" style="margin-top: 20px;">
            <p><strong>ID:</strong> <span id="modalId"></span></p>
            <p><strong>名前:</strong> <span id="modalName"></span></p>
            <p><strong>メールアドレス:</strong> <span id="modalEmail"></span></p>
            <p><strong>年齢:</strong> <span id="modalAge"></span></p>
            <p><strong>登録日時:</strong> <span id="modalCreatedAt"></span></p>
            <p><strong>更新日時:</strong> <span id="modalUpdatedAt"></span></p>
        </div>
        
        <div style="margin-top: 20px; text-align: right;">
            <button onclick="closeModal()" 
                    style="padding: 10px 20px; background-color: #999; color: white; border: none; border-radius: 4px; cursor: pointer;">
                閉じる
            </button>
        </div>
    </div>
</div>
```

### 3-2. モーダル制御JavaScript

```javascript
// モーダルでユーザー詳細を表示
async function showUserDetail(userId) {
    try {
        const response = await fetch(`/api/users/${userId}`);
        
        if (!response.ok) {
            throw new Error('ユーザー情報の取得に失敗しました');
        }
        
        const user = await response.json();
        
        // モーダルに値をセット
        document.getElementById('modalId').textContent = user.id;
        document.getElementById('modalName').textContent = user.name;
        document.getElementById('modalEmail').textContent = user.email;
        document.getElementById('modalAge').textContent = user.age;
        document.getElementById('modalCreatedAt').textContent = formatDateTime(user.createdAt);
        document.getElementById('modalUpdatedAt').textContent = formatDateTime(user.updatedAt);
        
        // モーダルを表示
        document.getElementById('userModal').style.display = 'block';
        
    } catch (error) {
        console.error('Error:', error);
        alert('ユーザー情報の取得に失敗しました');
    }
}

// モーダルを閉じる
function closeModal() {
    document.getElementById('userModal').style.display = 'none';
}

// 日時フォーマット
function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return '-';
    const date = new Date(dateTimeStr);
    return date.toLocaleString('ja-JP');
}

// モーダル外クリックで閉じる
document.getElementById('userModal').addEventListener('click', function(event) {
    if (event.target === this) {
        closeModal();
    }
});
```

### 3-3. テーブルの詳細リンクを修正

```html
<td style="border: 1px solid #ddd; padding: 12px;">
    <a href="javascript:void(0)" 
       onclick="showUserDetail([[${user.id}]])" 
       style="color: #4CAF50; text-decoration: none; margin-right: 10px;">
        詳細
    </a>
    <button onclick="deleteUser([[${user.id}]], '[[${user.name}]]')" 
            style="background-color: #d32f2f; color: white; border: none; padding: 5px 10px; border-radius: 4px; cursor: pointer;">
        削除
    </button>
</td>
```

---

## 🚀 ステップ4: リアルタイムバリデーション

### 4-1. フォームにリアルタイムチェック追加

`users/form.html`の名前フィールドを修正:

```html
<div class="form-group">
    <label for="name">名前</label>
    <input type="text" 
           id="name" 
           th:field="*{name}"
           th:errorclass="error-border"
           placeholder="山田太郎"
           oninput="validateName()">
    <div class="error" th:if="${#fields.hasErrors('name')}" th:errors="*{name}"></div>
    <div id="nameValidation" class="error" style="display: none;"></div>
</div>

<script>
    let nameCheckTimeout;
    
    // 名前のリアルタイムバリデーション
    function validateName() {
        const nameInput = document.getElementById('name');
        const validationDiv = document.getElementById('nameValidation');
        const name = nameInput.value.trim();
        
        // 入力がクリアされた場合
        if (!name) {
            validationDiv.style.display = 'none';
            return;
        }
        
        // 文字数チェック（ローカル）
        if (name.length < 2 || name.length > 50) {
            validationDiv.textContent = '名前は2〜50文字で入力してください';
            validationDiv.style.display = 'block';
            nameInput.classList.add('error-border');
            return;
        }
        
        // デバウンス処理（連続入力を待つ）
        clearTimeout(nameCheckTimeout);
        nameCheckTimeout = setTimeout(async () => {
            try {
                // サーバーサイドで重複チェック
                const response = await fetch(`/api/users/check-name?name=${encodeURIComponent(name)}`);
                const result = await response.json();
                
                if (result.exists) {
                    validationDiv.textContent = 'この名前は既に使用されています';
                    validationDiv.style.display = 'block';
                    nameInput.classList.add('error-border');
                } else {
                    validationDiv.style.display = 'none';
                    nameInput.classList.remove('error-border');
                }
                
            } catch (error) {
                console.error('Validation error:', error);
            }
        }, 500);  // 500ms待ってからチェック
    }
</script>
```

### 4-2. チェック用APIエンドポイント（参考）

`UserController.java`に追加（実装は任意）:

```java
@GetMapping("/check-name")
public ResponseEntity<Map<String, Boolean>> checkNameExists(
        @RequestParam String name) {
    // 簡易実装（実際はDBチェック）
    boolean exists = name.equals("admin") || name.equals("test");
    return ResponseEntity.ok(Map.of("exists", exists));
}
```

---

## ✅ 動作確認

### 1. 非同期検索の確認

1. ユーザー一覧にアクセス: `http://localhost:8080/views/users`
2. 検索フォームに名前の一部を入力
3. 「検索」ボタンをクリック

**期待される結果**:
- ページがリロードされない
- 該当するユーザーのみが表示される
- 件数が更新される

### 2. 削除機能の確認

1. ユーザー一覧で「削除」ボタンをクリック
2. 確認ダイアログで「OK」

**期待される結果**:
- ページがリロードされない
- 該当の行がテーブルから消える
- 件数が1減る

### 3. モーダル表示の確認

1. ユーザー一覧で「詳細」リンクをクリック

**期待される結果**:
- モーダルダイアログが表示される
- ユーザー情報が表示される
- 背景がグレーアウトされる
- モーダル外をクリックで閉じる

---

## 🎨 チャレンジ課題

### チャレンジ 1: オートコンプリート検索

入力中に候補を表示するオートコンプリートを実装してみましょう。

**ヒント**:
- `oninput`イベントで入力を検知
- デバウンス処理で無駄なリクエストを減らす
- ドロップダウンリストで候補を表示

### チャレンジ 2: インライン編集

テーブルの行をダブルクリックでそのまま編集できる機能を追加してみましょう。

**ヒント**:
- `contenteditable="true"`属性を使用
- 編集完了時にPUT APIを呼び出し

### チャレンジ 3: ページネーション

大量データをページング表示する機能を実装してみましょう。

**ヒント**:
- URLパラメータ`?page=1&size=10`
- ページ番号ボタンで非同期にデータ取得

---

## 🐛 トラブルシューティング

### CORSエラー: "Access-Control-Allow-Origin"

**原因**: 異なるオリジンからのリクエストがブロックされる

**解決策**:
同じサーバーからHTMLとAPIを配信している場合は発生しない。外部APIを呼び出す場合は`@CrossOrigin`が必要:

```java
@CrossOrigin(origins = "http://localhost:8080")
@RestController
public class UserController {
    // ...
}
```

### Thymeleafセキュリティエラー: "Only variable expressions returning numbers or booleans"

**原因**: `th:onclick`内で文字列を連結している

**NGコード**:
```html
<!-- ❌ エラー: 文字列連結は許可されない -->
<button th:onclick="'deleteUser(' + ${user.id} + ')'">削除</button>
```

**解決策**: `data-*`属性を使う
```html
<!-- ✅ data属性でデータを渡す -->
<button th:data-user-id="${user.id}"
        onclick="deleteUser(this.getAttribute('data-user-id'))">
    削除
</button>
```

### GlobalExceptionHandlerとの競合

**原因**: `@ControllerAdvice`がThymeleafコントローラーにも適用され、HTML応答できない

**解決策**: REST APIのみに適用するよう制限
```java
// ✅ @RestControllerのみに適用
@ControllerAdvice(annotations = org.springframework.web.bind.annotation.RestController.class)
public class GlobalExceptionHandler {
    // ...
}
```

### Promiseエラー: "Uncaught (in promise)"

**原因**: async/awaitのエラーハンドリング不足

**解決策**:
```javascript
try {
    const response = await fetch('/api/users');
    // ...
} catch (error) {
    console.error('Error:', error);  // 必ずログ出力
    alert('エラーが発生しました');
}
```

### DOM要素が見つからない

**原因**: `document.getElementById()`が`null`を返す

**解決策**:
1. IDが正しいか確認
2. スクリプトがDOMより後に配置されているか確認
3. `DOMContentLoaded`イベントで初期化

```javascript
document.addEventListener('DOMContentLoaded', function() {
    // 初期化処理
});
```

### XSS脆弱性

**原因**: ユーザー入力をエスケープせずにHTMLに挿入

**解決策**:
```javascript
// NG: XSS脆弱性あり
element.innerHTML = user.name;

// OK: エスケープして安全に
element.textContent = user.name;

// または
element.innerHTML = escapeHtml(user.name);
```

**重要**: Thymeleafの`data-*`属性は自動的にエスケープされるので安全です。

---

## 📚 このステップで学んだこと

- ✅ Fetch APIを使った非同期通信
- ✅ async/awaitでPromiseを処理
- ✅ Thymeleafインライン式でJavaScriptに値を渡す
- ✅ DOM操作で動的にテーブルを更新
- ✅ モーダルダイアログの実装
- ✅ デバウンス処理でパフォーマンス最適化
- ✅ XSS対策（HTMLエスケープ）

---

## 💡 補足: SSRとCSRのハイブリッド戦略

### 使い分けの基準

| 機能 | 推奨手法 | 理由 |
|---|---|---|
| 初期表示 | SSR（Thymeleaf） | SEO、高速表示 |
| 検索・フィルタ | CSR（JavaScript） | リアルタイム性 |
| フォーム送信 | SSR（POST） | セキュリティ、確実性 |
| 削除・部分更新 | CSR（AJAX） | UX向上 |
| 一覧ページング | ハイブリッド | 状況に応じて |

### パフォーマンス最適化

1. **デバウンス**: 連続入力を待ってからリクエスト送信
2. **キャッシュ**: 一度取得したデータを再利用
3. **楽観的UI更新**: サーバー応答を待たずにUIを更新

---

## ➡️ 次のステップ

これでPhase 5（Thymeleafでサーバーサイドレンダリング）は完了です！

次は[Phase 6: セキュリティとテスト](../phase6/STEP_25.md)へ進みましょう。

Phase 6では、Spring Securityを使った認証・認可、JWTトークン、ユニットテスト、統合テストを学びます。
