# Step 22: フォーム送信とバリデーション

## 🎯 このステップの目標

- Thymeleafでフォームを作成し、POSTリクエストを送信できる
- `th:object`と`th:field`を使ったフォームバインディングができる
- バリデーションエラーをThymeleafテンプレートで表示できる
- PRG（Post-Redirect-Get）パターンを実装できる

**所要時間**: 約50分

---

## 📋 事前準備

- [Step 21: Thymeleafの基礎](STEP_21.md)が完了していること
- Phase 4のバリデーション（`@Valid`、`@NotBlank`など）を理解していること

---

## 🎓 フォーム処理の基本パターン

### GETとPOSTの役割分担

```
1. GET /views/users/new  → フォーム表示（空のフォーム）
2. POST /views/users     → フォーム送信（データ登録）
3. GET /views/users/{id} → リダイレクト後の詳細表示
```

### PRG（Post-Redirect-Get）パターン

**問題**: POST後に直接ビューを返すと、リロードで二重送信される

```java
// NG: POST後に直接ビューを返す
@PostMapping("/users")
public String create(@Valid UserForm form) {
    userService.create(form);
    return "users/detail";  // ❌ リロードでPOST再送信
}
```

**解決策**: POSTの後にリダイレクト

```java
// OK: PRGパターン
@PostMapping("/users")
public String create(@Valid UserForm form) {
    UserResponse user = userService.create(form);
    return "redirect:/views/users/" + user.getId();  // ✅ GETにリダイレクト
}
```

---

## 🚀 ステップ1: フォーム用DTOの作成

### 1-1. ユーザー作成フォームクラス

`src/main/java/com/example/hellospringboot/dto/UserForm.java`を作成します:

```java
package com.example.hellospringboot.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ユーザー作成/編集フォーム
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserForm {
    
    @NotBlank(message = "名前を入力してください")
    @Size(min = 2, max = 50, message = "名前は2〜50文字で入力してください")
    private String name;
    
    @NotBlank(message = "メールアドレスを入力してください")
    @Email(message = "正しいメールアドレスを入力してください")
    private String email;
    
    @NotNull(message = "年齢を入力してください")
    @Min(value = 0, message = "年齢は0以上で入力してください")
    @Max(value = 150, message = "年齢は150以下で入力してください")
    private Integer age;
}
```

### 1-2. フォームDTOとリクエストDTOの違い

| クラス | 用途 | 特徴 |
|---|---|---|
| `UserCreateRequest` | REST API用 | JSON受け取り |
| `UserForm` | Thymeleafフォーム用 | HTMLフォームバインディング |

**両方必要な理由**:
- REST APIとThymeleafで異なるバリデーションメッセージを使いたい
- フォームには表示用の追加フィールド（確認用パスワードなど）が必要な場合がある
- 責務の分離（API層とView層）

---

## 🚀 ステップ2: 新規作成フォームの実装

### 2-1. フォーム表示テンプレート

`src/main/resources/templates/users/form.html`を作成します:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>ユーザー登録</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            max-width: 600px;
            margin: 50px auto;
            padding: 20px;
        }
        .form-group {
            margin-bottom: 20px;
        }
        label {
            display: block;
            font-weight: bold;
            margin-bottom: 5px;
            color: #333;
        }
        input[type="text"],
        input[type="email"],
        input[type="number"] {
            width: 100%;
            padding: 10px;
            border: 1px solid #ddd;
            border-radius: 4px;
            box-sizing: border-box;
        }
        .error {
            color: #d32f2f;
            font-size: 14px;
            margin-top: 5px;
        }
        .error-border {
            border-color: #d32f2f !important;
        }
        .btn {
            padding: 10px 20px;
            background-color: #4CAF50;
            color: white;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 16px;
        }
        .btn:hover {
            background-color: #45a049;
        }
        .btn-secondary {
            background-color: #999;
            margin-left: 10px;
        }
        .actions {
            margin-top: 20px;
        }
    </style>
</head>
<body>
    <h1>ユーザー登録</h1>
    
    <form th:action="@{/views/users}" th:object="${userForm}" method="post">
        
        <div class="form-group">
            <label for="name">名前</label>
            <input type="text" 
                   id="name" 
                   th:field="*{name}"
                   th:errorclass="error-border"
                   placeholder="山田太郎">
            <div class="error" th:if="${#fields.hasErrors('name')}" th:errors="*{name}">
                名前のエラーメッセージ
            </div>
        </div>
        
        <div class="form-group">
            <label for="email">メールアドレス</label>
            <input type="email" 
                   id="email" 
                   th:field="*{email}"
                   th:errorclass="error-border"
                   placeholder="yamada@example.com">
            <div class="error" th:if="${#fields.hasErrors('email')}" th:errors="*{email}">
                メールアドレスのエラーメッセージ
            </div>
        </div>
        
        <div class="form-group">
            <label for="age">年齢</label>
            <input type="number" 
                   id="age" 
                   th:field="*{age}"
                   th:errorclass="error-border"
                   placeholder="25">
            <div class="error" th:if="${#fields.hasErrors('age')}" th:errors="*{age}">
                年齢のエラーメッセージ
            </div>
        </div>
        
        <div class="actions">
            <button type="submit" class="btn">登録</button>
            <a th:href="@{/views/users}" class="btn btn-secondary">キャンセル</a>
        </div>
    </form>
</body>
</html>
```

### 2-2. フォーム構文の解説

#### `th:object="${userForm}"`
```html
<form th:object="${userForm}" method="post">
```
- フォーム全体で使用するオブジェクトを指定
- Modelに`userForm`という名前で格納されている必要がある
- 子要素で`*{fieldName}`として参照可能

#### `th:field="*{name}"`
```html
<input type="text" th:field="*{name}">
```
- **選択変数式**: `*{...}`は`th:object`で指定したオブジェクトのフィールド
- 以下の3つを自動生成:
  1. `name="name"` 属性
  2. `id="name"` 属性
  3. `value="${userForm.name}"` 属性（値があれば）

#### `th:errorclass="error-border"`
```html
<input th:field="*{name}" th:errorclass="error-border">
```
- バリデーションエラーがある場合、指定したCSSクラスを追加
- `#fields.hasErrors('name')`が真の場合に適用

#### `th:errors="*{name}"`
```html
<div th:if="${#fields.hasErrors('name')}" th:errors="*{name}">
    デフォルトメッセージ
</div>
```
- `#fields.hasErrors('name')`: フィールドにエラーがあるか確認
- `th:errors="*{name}"`: エラーメッセージを表示
- 複数エラーがある場合は`<br>`区切りで表示

---

## 🚀 ステップ3: Controller実装（フォーム表示と送信）

### 3-1. ViewControllerにフォーム処理を追加

`src/main/java/com/example/hellospringboot/controllers/ViewController.java`に以下を追加:

```java
package com.example.hellospringboot.controllers;

import com.example.hellospringboot.dto.UserForm;
import com.example.hellospringboot.dto.UserResponse;
import com.example.hellospringboot.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/views")
@RequiredArgsConstructor
public class ViewController {
    
    private final UserService userService;
    
    // ... 既存のメソッド（hello, listUsers, userDetail）...
    
    /**
     * 新規作成フォーム表示
     */
    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        model.addAttribute("userForm", new UserForm());
        return "users/form";
    }
    
    /**
     * ユーザー作成処理
     */
    @PostMapping("/users")
    public String createUser(
            @Valid @ModelAttribute UserForm userForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        
        log.info("Creating user: {}", userForm);
        
        // バリデーションエラーがある場合
        if (bindingResult.hasErrors()) {
            log.warn("Validation errors: {}", bindingResult.getAllErrors());
            return "users/form";  // エラー情報を保持したままフォームに戻る
        }
        
        // DTOを変換してサービスを呼び出し
        UserCreateRequest request = new UserCreateRequest(
            userForm.getName(),
            userForm.getEmail(),
            userForm.getAge()
        );
        
        UserResponse user = userService.create(request);
        log.info("User created successfully: {}", user.getId());
        
        // フラッシュメッセージを追加
        redirectAttributes.addFlashAttribute("message", "ユーザーを登録しました");
        
        // PRGパターン: 詳細画面にリダイレクト
        return "redirect:/views/users/" + user.getId();
    }
}
```

### 3-2. コードの解説

#### `@ModelAttribute UserForm userForm`
```java
@PostMapping("/users")
public String createUser(@Valid @ModelAttribute UserForm userForm) {
```
- `@ModelAttribute`: リクエストパラメータをオブジェクトにバインド
- フォームの`name`属性とフィールド名を自動マッピング
- `@Valid`: バリデーション実行

#### `BindingResult bindingResult`
```java
public String createUser(
    @Valid @ModelAttribute UserForm userForm,
    BindingResult bindingResult) {
```
- **必ず`@Valid`の直後に配置**
- バリデーション結果を格納
- `hasErrors()`: エラーの有無を確認
- `getAllErrors()`: すべてのエラーを取得

**重要**: `BindingResult`がないと、バリデーションエラー時に例外がスローされる

#### `RedirectAttributes redirectAttributes`
```java
redirectAttributes.addFlashAttribute("message", "登録しました");
return "redirect:/views/users/" + user.getId();
```
- リダイレクト先のリクエストでのみ有効なデータを渡す
- セッションに一時保存され、次のリクエストで取得後に削除される
- URLに表示されない（`addAttribute`との違い）

---

## 🚀 ステップ4: フラッシュメッセージの表示

### 4-1. 詳細画面にメッセージ表示を追加

`users/detail.html`の`<h1>`の後に以下を追加:

```html
<h1>ユーザー詳細</h1>

<!-- フラッシュメッセージ表示 -->
<div th:if="${message}" class="alert alert-success">
    <p th:text="${message}">メッセージ</p>
</div>

<div class="card">
    <!-- 既存の詳細情報... -->
</div>
```

CSSも追加:

```html
<style>
    /* 既存のスタイル... */
    
    .alert {
        padding: 15px;
        margin-bottom: 20px;
        border-radius: 4px;
    }
    .alert-success {
        background-color: #d4edda;
        border: 1px solid #c3e6cb;
        color: #155724;
    }
</style>
```

---

## 🚀 ステップ5: 編集フォームの実装

### 5-1. 編集フォーム表示

`ViewController.java`に編集フォーム表示メソッドを追加:

```java
/**
 * 編集フォーム表示
 */
@GetMapping("/users/{id}/edit")
public String editUserForm(@PathVariable Long id, Model model) {
    UserResponse user = userService.findById(id);
    
    // UserResponseをUserFormに変換
    UserForm userForm = new UserForm(
        user.getName(),
        user.getEmail(),
        user.getAge()
    );
    
    model.addAttribute("userForm", userForm);
    model.addAttribute("userId", id);  // 更新用にIDを保持
    
    return "users/edit";
}
```

### 5-2. 編集フォームテンプレート

`src/main/resources/templates/users/edit.html`を作成:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>ユーザー編集</title>
    <style>
        /* form.htmlと同じスタイル */
        body {
            font-family: Arial, sans-serif;
            max-width: 600px;
            margin: 50px auto;
            padding: 20px;
        }
        .form-group {
            margin-bottom: 20px;
        }
        label {
            display: block;
            font-weight: bold;
            margin-bottom: 5px;
        }
        input[type="text"],
        input[type="email"],
        input[type="number"] {
            width: 100%;
            padding: 10px;
            border: 1px solid #ddd;
            border-radius: 4px;
            box-sizing: border-box;
        }
        .error {
            color: #d32f2f;
            font-size: 14px;
            margin-top: 5px;
        }
        .error-border {
            border-color: #d32f2f !important;
        }
        .btn {
            padding: 10px 20px;
            color: white;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            text-decoration: none;
            display: inline-block;
        }
        .btn-primary {
            background-color: #4CAF50;
        }
        .btn-secondary {
            background-color: #999;
            margin-left: 10px;
        }
    </style>
</head>
<body>
    <h1>ユーザー編集</h1>
    
    <form th:action="@{/views/users/{id}(id=${userId})}" 
          th:object="${userForm}" 
          method="post">
        
        <div class="form-group">
            <label for="name">名前</label>
            <input type="text" 
                   id="name" 
                   th:field="*{name}"
                   th:errorclass="error-border">
            <div class="error" th:if="${#fields.hasErrors('name')}" th:errors="*{name}"></div>
        </div>
        
        <div class="form-group">
            <label for="email">メールアドレス</label>
            <input type="email" 
                   id="email" 
                   th:field="*{email}"
                   th:errorclass="error-border">
            <div class="error" th:if="${#fields.hasErrors('email')}" th:errors="*{email}"></div>
        </div>
        
        <div class="form-group">
            <label for="age">年齢</label>
            <input type="number" 
                   id="age" 
                   th:field="*{age}"
                   th:errorclass="error-border">
            <div class="error" th:if="${#fields.hasErrors('age')}" th:errors="*{age}"></div>
        </div>
        
        <div class="actions">
            <button type="submit" class="btn btn-primary">更新</button>
            <a th:href="@{/views/users/{id}(id=${userId})}" class="btn btn-secondary">キャンセル</a>
        </div>
    </form>
</body>
</html>
```

### 5-3. 更新処理

`ViewController.java`に更新メソッドを追加:

```java
/**
 * ユーザー更新処理
 */
@PostMapping("/users/{id}")
public String updateUser(
        @PathVariable Long id,
        @Valid @ModelAttribute UserForm userForm,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes) {
    
    log.info("Updating user {}: {}", id, userForm);
    
    if (bindingResult.hasErrors()) {
        log.warn("Validation errors: {}", bindingResult.getAllErrors());
        model.addAttribute("userId", id);
        return "users/edit";
    }
    
    UserUpdateRequest request = new UserUpdateRequest(
        userForm.getName(),
        userForm.getEmail(),
        userForm.getAge()
    );
    
    userService.update(id, request);
    log.info("User {} updated successfully", id);
    
    redirectAttributes.addFlashAttribute("message", "ユーザー情報を更新しました");
    
    return "redirect:/views/users/" + id;
}
```

### 5-4. 詳細画面に編集リンクを追加

`users/detail.html`のアクション部分を修正:

```html
<div class="actions">
    <a th:href="@{/views/users/{id}/edit(id=${user.id})}" class="btn">編集</a>
    <a th:href="@{/views/users}" class="btn btn-secondary">一覧に戻る</a>
</div>
```

---

## ✅ 動作確認

### 1. 新規作成フォームの表示

ブラウザで以下にアクセス:
```
http://localhost:8080/views/users/new
```

**期待される結果**:
- 空のフォームが表示される
- 名前、メールアドレス、年齢の入力欄がある

### 2. バリデーションエラーの確認

すべて空欄のまま「登録」ボタンをクリック

**期待される結果**:
- フォームが再表示される
- 各フィールドに赤い枠が表示される
- エラーメッセージが表示される:
  - "名前を入力してください"
  - "メールアドレスを入力してください"
  - "年齢を入力してください"

### 3. 正常な登録

正しいデータを入力して登録:
- 名前: `山田太郎`
- メールアドレス: `yamada@example.com`
- 年齢: `30`

**期待される結果**:
- 詳細画面にリダイレクトされる
- "ユーザーを登録しました"というメッセージが表示される
- 登録したユーザー情報が表示される

### 4. 編集機能の確認

詳細画面から「編集」をクリック:
```
http://localhost:8080/views/users/1/edit
```

**期待される結果**:
- 既存のデータがフォームに表示される
- 名前を変更して「更新」をクリック
- "ユーザー情報を更新しました"というメッセージが表示される

---

## 🎨 チャレンジ課題

### チャレンジ 1: 削除機能の実装

削除確認ダイアログ付きの削除機能を実装してみましょう。

**ヒント**:
```html
<form th:action="@{/views/users/{id}/delete(id=${user.id})}" method="post" 
      onsubmit="return confirm('本当に削除しますか?');">
    <button type="submit" class="btn btn-danger">削除</button>
</form>
```

Controllerで`@PostMapping("/users/{id}/delete")`を実装します。

### チャレンジ 2: グローバルエラー表示

フィールド単位ではなく、フォーム全体のエラーを表示してみましょう。

**ヒント**:
```html
<div th:if="${#fields.hasErrors('global')}" class="error">
    <ul>
        <li th:each="err : ${#fields.errors('global')}" th:text="${err}"></li>
    </ul>
</div>
```

### チャレンジ 3: パスワード確認フィールド

パスワードと確認用パスワードが一致するかチェックする機能を追加してみましょう。

**ヒント**:
- `UserForm`に`password`と`passwordConfirmation`を追加
- カスタムバリデーション`@PasswordMatch`を作成
- `ConstraintValidator`を実装

---

## 🐛 トラブルシューティング

### エラー: "Neither BindingResult nor plain target object"

**原因**: `@ModelAttribute`の直後に`BindingResult`がない

**解決策**:
```java
// NG: BindingResultの位置が間違っている
public String create(@Valid UserForm form, Model model, BindingResult result)

// OK: @Validの直後にBindingResult
public String create(@Valid UserForm form, BindingResult result, Model model)
```

### フォーム送信後に値が保持されない

**原因**: バリデーションエラー時に`Model`に再度追加していない

**解決策**:
```java
if (bindingResult.hasErrors()) {
    // フォームオブジェクトは自動的にModelに追加される
    return "users/form";  // OK
}
```

Thymeleafは自動的に`@ModelAttribute`のオブジェクトをModelに追加します。

### エラーメッセージが日本語にならない

**原因**: デフォルトメッセージが英語

**解決策**:
`src/main/resources/messages.properties`を作成:
```properties
NotBlank.userForm.name=名前を入力してください
Email.userForm.email=正しいメールアドレスを入力してください
```

### フラッシュメッセージが表示されない

**原因**: `addAttribute`を使用している（URLパラメータになる）

**解決策**:
```java
// NG: URLに表示される
redirectAttributes.addAttribute("message", "登録しました");

// OK: セッションに一時保存
redirectAttributes.addFlashAttribute("message", "登録しました");
```

### リダイレクト後に404エラー

**原因**: リダイレクト先のURLが間違っている

**解決策**:
```java
// 相対パスの場合、現在のパスから相対的に解決される
return "redirect:users/" + id;  // NG: /views/users/users/1 になる

// 絶対パス（/から始まる）を使用
return "redirect:/views/users/" + id;  // OK
```

---

## 📚 このステップで学んだこと

- ✅ `th:object`と`th:field`を使ったフォームバインディング
- ✅ `@ModelAttribute`でリクエストパラメータをオブジェクトに変換
- ✅ `BindingResult`でバリデーション結果を取得
- ✅ `th:errors`でエラーメッセージを表示
- ✅ PRG（Post-Redirect-Get）パターンの実装
- ✅ `RedirectAttributes`でフラッシュメッセージを渡す
- ✅ 新規作成と編集フォームの使い分け

---

## 💡 補足: Spring MVCのデータバインディング

Thymeleafのフォームは、Spring MVCのデータバインディング機能を活用しています:

```
1. フォーム表示時
   Controller → UserForm（空） → Model → Thymeleaf → HTML

2. フォーム送信時
   HTML → HTTPリクエスト → DataBinder → UserForm（値入り） → Validation
                                                               ↓
                                                        BindingResult
```

`th:field`が生成する`name`属性を使って、Spring MVCが自動的にオブジェクトにバインドします。

---

## ➡️ 次のステップ

[Step 23: レイアウトとフラグメント](STEP_23.md)へ進みましょう！

次のステップでは、共通レイアウトの作成、ヘッダー/フッターの部品化、フラグメントの再利用方法を学びます。
