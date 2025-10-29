# Step 22: フォーム送信とバリデーション

## 🎯 このステップの目標

- HTMLフォームからデータを受け取る方法を学ぶ
- `th:object`と`th:field`でフォームバインディングを実装する
- バリデーションエラーをThymeleafで表示する
- PRG（Post-Redirect-Get）パターンを理解する
- フラッシュメッセージで成功通知を表示する

**所要時間**: 約2時間30分

---

## 📋 事前準備

- Step 21の完了
- Bean Validationの基礎知識（Phase 4で学習済み）
- フォームの基本的なHTML知識

---

## 💡 フォーム処理の流れ

### 従来のHTMLフォーム

```html
<form action="/users" method="post">
    <input type="text" name="name">
    <input type="email" name="email">
    <button type="submit">送信</button>
</form>
```

**問題点**:
- エラー時に入力値が消える
- バリデーションエラーの表示が面倒
- CSRF対策が必要

### Thymeleafのフォーム

```html
<form th:action="@{/users}" th:object="${userForm}" method="post">
    <input type="text" th:field="*{name}">
    <span th:errors="*{name}"></span>
    <button type="submit">送信</button>
</form>
```

**メリット**:
- 入力値が自動で保持される
- バリデーションエラーが簡単に表示できる
- CSRFトークンが自動で付与される

---

## 🏗️ 実装手順

### Step 1: フォーム用DTOクラスの作成

`src/main/java/com/example/demo/dto/UserForm.java`:

```java
package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserForm {
    
    @NotBlank(message = "名前は必須です")
    @Size(min = 2, max = 50, message = "名前は2文字以上50文字以内で入力してください")
    private String name;
    
    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "正しいメールアドレス形式で入力してください")
    private String email;
}
```

### Step 2: Controllerの実装

`src/main/java/com/example/demo/controller/UserFormController.java`:

```java
package com.example.demo.controller;

import com.example.demo.dto.UserForm;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserFormController {
    
    private final UserService userService;
    
    /**
     * 新規登録フォーム表示（GET）
     */
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        // 空のフォームオブジェクトを渡す
        model.addAttribute("userForm", new UserForm());
        return "users/form";
    }
    
    /**
     * 新規登録処理（POST）
     */
    @PostMapping
    public String createUser(
            @Valid @ModelAttribute UserForm userForm,  // バリデーション実行
            BindingResult bindingResult,               // エラー情報
            RedirectAttributes redirectAttributes) {   // リダイレクト先へのメッセージ
        
        // バリデーションエラーがある場合
        if (bindingResult.hasErrors()) {
            // フォーム画面に戻る（エラー情報を保持）
            return "users/form";
        }
        
        // エンティティに変換して保存
        User user = new User();
        user.setName(userForm.getName());
        user.setEmail(userForm.getEmail());
        userService.createUser(user);
        
        // 成功メッセージをフラッシュスコープに追加
        redirectAttributes.addFlashAttribute("successMessage", 
            "ユーザー「" + user.getName() + "」を登録しました");
        
        // PRGパターン: リダイレクトで二重送信を防止
        return "redirect:/users";
    }
    
    /**
     * 編集フォーム表示（GET）
     */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id);
        
        // EntityをFormに変換
        UserForm userForm = new UserForm();
        userForm.setName(user.getName());
        userForm.setEmail(user.getEmail());
        
        model.addAttribute("userForm", userForm);
        model.addAttribute("userId", id);
        model.addAttribute("isEdit", true);  // 編集モードフラグ
        
        return "users/form";
    }
    
    /**
     * 更新処理（POST）
     */
    @PostMapping("/{id}")
    public String updateUser(
            @PathVariable Long id,
            @Valid @ModelAttribute UserForm userForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("userId", id);
            model.addAttribute("isEdit", true);
            return "users/form";
        }
        
        User user = new User();
        user.setName(userForm.getName());
        user.setEmail(userForm.getEmail());
        userService.updateUser(id, user);
        
        redirectAttributes.addFlashAttribute("successMessage", 
            "ユーザー情報を更新しました");
        
        return "redirect:/users";
    }
    
    /**
     * 削除処理（POST）
     */
    @PostMapping("/{id}/delete")
    public String deleteUser(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        
        User user = userService.getUserById(id);
        userService.deleteUser(id);
        
        redirectAttributes.addFlashAttribute("successMessage", 
            "ユーザー「" + user.getName() + "」を削除しました");
        
        return "redirect:/users";
    }
}
```

### Step 3: フォームテンプレートの作成

`src/main/resources/templates/users/form.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="${isEdit} ? 'ユーザー編集' : 'ユーザー登録'">ユーザーフォーム</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            max-width: 600px;
            margin: 50px auto;
            padding: 20px;
        }
        h1 {
            color: #333;
            border-bottom: 2px solid #4CAF50;
            padding-bottom: 10px;
        }
        .form-group {
            margin-bottom: 20px;
        }
        label {
            display: block;
            font-weight: bold;
            margin-bottom: 5px;
            color: #555;
        }
        input[type="text"],
        input[type="email"] {
            width: 100%;
            padding: 10px;
            border: 1px solid #ddd;
            border-radius: 4px;
            font-size: 14px;
            box-sizing: border-box;
        }
        input.error {
            border-color: #f44336;
        }
        .error-message {
            color: #f44336;
            font-size: 13px;
            margin-top: 5px;
            display: block;
        }
        .button-group {
            margin-top: 30px;
            display: flex;
            gap: 10px;
        }
        button {
            padding: 12px 24px;
            border: none;
            border-radius: 4px;
            font-size: 16px;
            cursor: pointer;
        }
        .btn-primary {
            background-color: #4CAF50;
            color: white;
        }
        .btn-primary:hover {
            background-color: #45a049;
        }
        .btn-secondary {
            background-color: #666;
            color: white;
            text-decoration: none;
            display: inline-block;
            line-height: 1.5;
        }
        .btn-secondary:hover {
            background-color: #555;
        }
    </style>
</head>
<body>
    <h1 th:text="${isEdit} ? 'ユーザー編集' : 'ユーザー登録'">ユーザーフォーム</h1>
    
    <!-- 
        th:action: フォーム送信先URL
        th:object: フォームにバインドするオブジェクト
    -->
    <form th:action="${isEdit} ? @{/users/{id}(id=${userId})} : @{/users}" 
          th:object="${userForm}" 
          method="post">
        
        <!-- 名前入力 -->
        <div class="form-group">
            <label for="name">名前 <span style="color: red;">*</span></label>
            <!-- 
                th:field="*{name}": 
                - id="name" name="name" value="${userForm.name}" を自動生成
                - エラー時も入力値を保持
                th:errorclass: エラー時にCSSクラスを追加
            -->
            <input type="text" 
                   th:field="*{name}" 
                   th:errorclass="error"
                   placeholder="山田太郎">
            <!-- 
                th:errors="*{name}": nameフィールドのエラーメッセージを表示
            -->
            <span class="error-message" th:errors="*{name}"></span>
        </div>
        
        <!-- メールアドレス入力 -->
        <div class="form-group">
            <label for="email">メールアドレス <span style="color: red;">*</span></label>
            <input type="email" 
                   th:field="*{email}" 
                   th:errorclass="error"
                   placeholder="example@example.com">
            <span class="error-message" th:errors="*{email}"></span>
        </div>
        
        <!-- ボタン -->
        <div class="button-group">
            <button type="submit" class="btn-primary" 
                    th:text="${isEdit} ? '更新' : '登録'">
                登録
            </button>
            <a th:href="@{/users}" class="btn-secondary">キャンセル</a>
        </div>
    </form>
</body>
</html>
```

### Step 4: 一覧ページの更新（成功メッセージ表示）

`src/main/resources/templates/users/list.html`を更新:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ユーザー一覧</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            max-width: 900px;
            margin: 50px auto;
            padding: 20px;
        }
        h1 {
            color: #333;
        }
        .success-message {
            background-color: #d4edda;
            color: #155724;
            padding: 15px;
            border: 1px solid #c3e6cb;
            border-radius: 4px;
            margin-bottom: 20px;
        }
        .btn-new {
            display: inline-block;
            padding: 10px 20px;
            background-color: #4CAF50;
            color: white;
            text-decoration: none;
            border-radius: 4px;
            margin-bottom: 20px;
        }
        .btn-new:hover {
            background-color: #45a049;
        }
        table {
            width: 100%;
            border-collapse: collapse;
        }
        th, td {
            border: 1px solid #ddd;
            padding: 12px;
            text-align: left;
        }
        th {
            background-color: #4CAF50;
            color: white;
        }
        tr:nth-child(even) {
            background-color: #f2f2f2;
        }
        .action-links a {
            margin-right: 10px;
            text-decoration: none;
            color: #2196F3;
        }
        .action-links a:hover {
            text-decoration: underline;
        }
        .btn-delete {
            color: #f44336 !important;
            background: none;
            border: none;
            cursor: pointer;
            padding: 0;
            font-size: inherit;
        }
    </style>
</head>
<body>
    <h1>ユーザー一覧</h1>
    
    <!-- 成功メッセージ（フラッシュスコープから取得） -->
    <div class="success-message" th:if="${successMessage}" th:text="${successMessage}"></div>
    
    <a th:href="@{/users/new}" class="btn-new">+ 新規登録</a>
    
    <div th:if="${users.isEmpty()}">
        <p>ユーザーが登録されていません。</p>
    </div>
    
    <table th:unless="${users.isEmpty()}">
        <thead>
            <tr>
                <th>ID</th>
                <th>名前</th>
                <th>メールアドレス</th>
                <th>登録日時</th>
                <th>操作</th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="user : ${users}">
                <td th:text="${user.id}">1</td>
                <td th:text="${user.name}">山田太郎</td>
                <td th:text="${user.email}">yamada@example.com</td>
                <td th:text="${#temporals.format(user.createdAt, 'yyyy-MM-dd HH:mm')}">
                    2025-10-29 10:00
                </td>
                <td class="action-links">
                    <a th:href="@{/users/{id}(id=${user.id})}">詳細</a>
                    <a th:href="@{/users/{id}/edit(id=${user.id})}">編集</a>
                    <!-- 削除フォーム -->
                    <form th:action="@{/users/{id}/delete(id=${user.id})}" 
                          method="post" 
                          style="display: inline;"
                          onsubmit="return confirm('本当に削除しますか？');">
                        <button type="submit" class="btn-delete">削除</button>
                    </form>
                </td>
            </tr>
        </tbody>
    </table>
</body>
</html>
```

---

## ✅ 動作確認

### 1. 新規登録フォーム表示

ブラウザで以下にアクセス:
```
http://localhost:8080/users/new
```

### 2. バリデーションエラーの確認

何も入力せずに「登録」ボタンをクリック:
- 「名前は必須です」
- 「メールアドレスは必須です」

不正なメールアドレスを入力:
- 「正しいメールアドレス形式で入力してください」

### 3. 正常登録

正しいデータを入力して登録:
- 成功メッセージが表示される
- 一覧ページにリダイレクトされる
- 新しいユーザーが表示される

### 4. 編集機能

一覧から「編集」リンクをクリック:
- 既存のデータが入力済み
- 更新後、成功メッセージが表示される

### 5. 削除機能

「削除」ボタンをクリック:
- 確認ダイアログが表示される
- OKで削除、成功メッセージが表示される

---

## 🔍 重要な概念

### 1. フォームバインディング

```html
<!-- th:object でフォームオブジェクトを指定 -->
<form th:object="${userForm}" method="post">
    <!-- th:field="*{プロパティ名}" で自動バインド -->
    <input th:field="*{name}">
    <!-- 以下のように展開される -->
    <!-- <input id="name" name="name" value="入力値"> -->
</form>
```

### 2. バリデーションエラー表示

```html
<!-- フィールドごとのエラー -->
<span th:errors="*{name}"></span>

<!-- 全てのエラー -->
<div th:if="${#fields.hasErrors('*')}">
    <ul>
        <li th:each="err : ${#fields.errors('*')}" th:text="${err}"></li>
    </ul>
</div>

<!-- 特定フィールドのエラー有無チェック -->
<input th:field="*{name}" 
       th:classappend="${#fields.hasErrors('name')} ? 'error' : ''">
```

### 3. PRG（Post-Redirect-Get）パターン

```java
@PostMapping
public String create(@Valid UserForm form, BindingResult result) {
    if (result.hasErrors()) {
        return "form";  // エラー時: ビュー直接返却
    }
    
    // 処理実行
    
    return "redirect:/users";  // 成功時: リダイレクト
}
```

**なぜPRGが必要？**
- ブラウザの「更新」ボタンで二重送信を防止
- ブックマークしても安全

### 4. フラッシュスコープ

```java
// Controller
redirectAttributes.addFlashAttribute("message", "成功！");
return "redirect:/users";
```

```html
<!-- Template（リダイレクト先） -->
<div th:if="${message}" th:text="${message}"></div>
```

**特徴**:
- リダイレクト先でのみ参照可能
- リロードしても消える（1回限り）

---

## 🎨 応用: 複雑なフォーム

### チェックボックス

```java
// Form
private List<String> hobbies;
```

```html
<div>
    <label><input type="checkbox" th:field="*{hobbies}" value="読書"> 読書</label>
    <label><input type="checkbox" th:field="*{hobbies}" value="スポーツ"> スポーツ</label>
    <label><input type="checkbox" th:field="*{hobbies}" value="音楽"> 音楽</label>
</div>
```

### ラジオボタン

```java
// Form
private String gender;
```

```html
<div>
    <label><input type="radio" th:field="*{gender}" value="male"> 男性</label>
    <label><input type="radio" th:field="*{gender}" value="female"> 女性</label>
</div>
```

### セレクトボックス

```java
// Form
private Long categoryId;
```

```html
<select th:field="*{categoryId}">
    <option value="">選択してください</option>
    <option th:each="cat : ${categories}" 
            th:value="${cat.id}" 
            th:text="${cat.name}">
        カテゴリー名
    </option>
</select>
```

### 日付入力

```java
// Form
@DateTimeFormat(pattern = "yyyy-MM-dd")
private LocalDate birthDate;
```

```html
<input type="date" th:field="*{birthDate}">
```

---

## 📝 理解度チェック

1. **`th:object`と`th:field`の役割は何ですか？**
2. **PRGパターンとは何ですか？なぜ必要ですか？**
3. **フラッシュスコープとセッションスコープの違いは？**
4. **`BindingResult`は何のために使いますか？**
5. **CSRFトークンは自動で付与されますか？**

---

## 💡 ベストプラクティス

1. **常にバリデーション**: サーバー側で必ず検証
2. **PRGパターン**: POST後は必ずリダイレクト
3. **ユーザーフィードバック**: 成功・エラーメッセージを明確に
4. **入力値の保持**: エラー時も入力値を消さない
5. **CSRF対策**: Spring Securityで自動対応（後のPhaseで学習）

---

## 📚 このステップで学んだこと

このステップでは、Thymeleafフォーム連携について学びました：

- ✅ th:objectとth:fieldでフォームオブジェクトをバインド
- ✅ Jakarta Validation（@Valid）とThymeleafの連携
- ✅ バリデーションエラーメッセージの表示（th:errors）
- ✅ PRG（Post-Redirect-Get）パターンの実装
- ✅ RedirectAttributesでフラッシュメッセージを渡す
- ✅ CRUD操作のフォーム実装パターン

---

## 🔄 Gitへのコミットとレビュー依頼

進捗を記録してレビューを受けましょう：

```bash
git add .
git commit -m "Step 22: フォーム送信とバリデーション完了"
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

レビューが完了したら、[Step 23: レイアウトとフラグメント](STEP_23.md)へ進みましょう！

次のStep 23では、**レイアウトとフラグメント**を学びます:
- 共通ヘッダー・フッターの作成
- `th:fragment`で部品化
- `th:replace`で再利用
- Bootstrapの統合

---

お疲れ様でした！🎉 フォーム処理がマスターできたら、次のステップに進みましょう。
