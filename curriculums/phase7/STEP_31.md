# Step 31: ページネーション

## 🎯 このステップの目標

- Spring Data JPAのページネーション機能を理解する
- `Pageable`と`Page`を使いこなす
- ソート機能を実装する
- カスタムページネーションを作成する

**所要時間**: 約1時間30分


---


## 📋 事前準備

- Step 30が完了していること
- 複数の環境（開発・本番）の概念を理解していること

---
---

## 💡 メール送信のユースケース

- ✉️ ユーザー登録の確認メール
- 🔑 パスワードリセット
- 📧 お知らせメール
- 📊 レポート送信

---

## 🚀 ステップ1: 依存関係の追加

### 1-1. pom.xmlの更新

```xml
<!-- Spring Boot Mail -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>

<!-- Thymeleaf（HTMLメールテンプレート用） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

---

## 🚀 ステップ2: メール設定

### 2-1. application.ymlの設定

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000

# カスタム設定
app:
  mail:
    from: noreply@example.com
    from-name: Spring Boot App
```

> **注意**: Gmailの場合は「アプリパスワード」を使用してください。

---

## 🚀 ステップ3: メールサービス

### 3-1. EmailService

**ファイルパス**: `src/main/java/com/example/hellospringboot/service/EmailService.java`

```java
package com.example.hellospringboot.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    /**
     * シンプルなテキストメールを送信
     */
    public void sendSimpleEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
        log.info("テキストメールを送信しました: {}", to);
    }

    /**
     * HTMLメールを送信
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail, fromName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);

        mailSender.send(message);
        log.info("HTMLメールを送信しました: {}", to);
    }

    /**
     * テンプレートを使ったメール送信
     */
    public void sendTemplateEmail(String to, String subject, String templateName, Map<String, Object> variables) 
            throws MessagingException {
        Context context = new Context();
        context.setVariables(variables);

        String htmlBody = templateEngine.process(templateName, context);

        sendHtmlEmail(to, subject, htmlBody);
        log.info("テンプレートメールを送信しました: {} (template: {})", to, templateName);
    }

    /**
     * 非同期でメール送信
     */
    @Async
    public void sendEmailAsync(String to, String subject, String text) {
        try {
            sendSimpleEmail(to, subject, text);
        } catch (Exception e) {
            log.error("非同期メール送信エラー: {}", e.getMessage(), e);
        }
    }

    /**
     * ウェルカムメールを送信
     */
    public void sendWelcomeEmail(String to, String username) throws MessagingException {
        Map<String, Object> variables = Map.of(
                "username", username
        );
        sendTemplateEmail(to, "ようこそ！", "welcome-email", variables);
    }

    /**
     * パスワードリセットメールを送信
     */
    public void sendPasswordResetEmail(String to, String resetToken) throws MessagingException {
        Map<String, Object> variables = Map.of(
                "resetToken", resetToken,
                "resetUrl", "http://localhost:8080/reset-password?token=" + resetToken
        );
        sendTemplateEmail(to, "パスワードリセット", "password-reset-email", variables);
    }
}
```

---

## 🚀 ステップ4: メールテンプレート

### 4-1. ウェルカムメールテンプレート

**ファイルパス**: `src/main/resources/templates/welcome-email.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ようこそ</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            line-height: 1.6;
            color: #333;
            max-width: 600px;
            margin: 0 auto;
            padding: 20px;
        }
        .header {
            background-color: #4CAF50;
            color: white;
            padding: 20px;
            text-align: center;
        }
        .content {
            padding: 20px;
            background-color: #f9f9f9;
        }
        .footer {
            padding: 10px;
            text-align: center;
            font-size: 12px;
            color: #777;
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>ようこそ！</h1>
    </div>
    <div class="content">
        <p>こんにちは、<strong th:text="${username}">ユーザー名</strong>さん</p>
        <p>登録いただきありがとうございます。</p>
        <p>これからよろしくお願いいたします。</p>
    </div>
    <div class="footer">
        <p>&copy; 2025 Spring Boot App. All rights reserved.</p>
    </div>
</body>
</html>
```

### 4-2. パスワードリセットメールテンプレート

**ファイルパス**: `src/main/resources/templates/password-reset-email.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>パスワードリセット</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            line-height: 1.6;
            color: #333;
            max-width: 600px;
            margin: 0 auto;
            padding: 20px;
        }
        .header {
            background-color: #2196F3;
            color: white;
            padding: 20px;
            text-align: center;
        }
        .content {
            padding: 20px;
            background-color: #f9f9f9;
        }
        .button {
            display: inline-block;
            padding: 10px 20px;
            background-color: #2196F3;
            color: white;
            text-decoration: none;
            border-radius: 5px;
        }
        .footer {
            padding: 10px;
            text-align: center;
            font-size: 12px;
            color: #777;
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>パスワードリセット</h1>
    </div>
    <div class="content">
        <p>パスワードリセットのリクエストを受け付けました。</p>
        <p>以下のリンクをクリックしてパスワードを再設定してください。</p>
        <p>
            <a th:href="${resetUrl}" class="button">パスワードをリセット</a>
        </p>
        <p>または、以下のトークンを使用してください：</p>
        <p><code th:text="${resetToken}">トークン</code></p>
        <p><small>このリンクは24時間有効です。</small></p>
    </div>
    <div class="footer">
        <p>&copy; 2025 Spring Boot App. All rights reserved.</p>
    </div>
</body>
</html>
```

---

## 🚀 ステップ5: EmailController

**ファイルパス**: `src/main/java/com/example/hellospringboot/controller/EmailController.java`

```java
package com.example.hellospringboot.controller;

import com.example.hellospringboot.dto.request.EmailRequest;
import com.example.hellospringboot.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
@Tag(name = "Email", description = "メール送信API")
public class EmailController {

    private final EmailService emailService;

    @Operation(summary = "シンプルメール送信", description = "テキストメールを送信します")
    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(@Valid @RequestBody EmailRequest request) {
        emailService.sendSimpleEmail(request.getTo(), request.getSubject(), request.getText());
        return ResponseEntity.ok("メールを送信しました");
    }

    @Operation(summary = "ウェルカムメール送信", description = "ウェルカムメールを送信します")
    @PostMapping("/welcome")
    public ResponseEntity<String> sendWelcomeEmail(
            @RequestParam String to,
            @RequestParam String username) throws MessagingException {
        emailService.sendWelcomeEmail(to, username);
        return ResponseEntity.ok("ウェルカムメールを送信しました");
    }

    @Operation(summary = "パスワードリセットメール送信", description = "パスワードリセットメールを送信します")
    @PostMapping("/password-reset")
    public ResponseEntity<String> sendPasswordResetEmail(
            @RequestParam String to,
            @RequestParam String resetToken) throws MessagingException {
        emailService.sendPasswordResetEmail(to, resetToken);
        return ResponseEntity.ok("パスワードリセットメールを送信しました");
    }
}
```

---

## 🚀 ステップ6: EmailRequest DTO

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/request/EmailRequest.java`

```java
package com.example.hellospringboot.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "メール送信リクエスト")
public class EmailRequest {

    @Schema(description = "送信先メールアドレス", example = "user@example.com", required = true)
    @NotBlank(message = "送信先メールアドレスは必須です")
    @Email(message = "メールアドレスの形式が正しくありません")
    private String to;

    @Schema(description = "件名", example = "お知らせ", required = true)
    @NotBlank(message = "件名は必須です")
    private String subject;

    @Schema(description = "本文", example = "これはテストメールです", required = true)
    @NotBlank(message = "本文は必須です")
    private String text;
}
```

---

## ✅ 動作確認

### シンプルメール送信

```bash
curl -X POST http://localhost:8080/api/emails/send \
  -H "Content-Type: application/json" \
  -d '{
    "to": "test@example.com",
    "subject": "テストメール",
    "text": "これはテストメールです"
  }'
```

### ウェルカムメール送信

```bash
curl -X POST "http://localhost:8080/api/emails/welcome?to=test@example.com&username=山田太郎"
```

---

## 💡 補足: Thymeleafテンプレートエンジンの活用

Phase 5でThymeleafの基礎を学習しました。メール送信では、**HTMLメールテンプレート**としてThymeleafが非常に有効です。

### Thymeleafをメールに使うメリット

1. **再利用性**: テンプレートを使い回せる
2. **保守性**: HTMLとロジックを分離
3. **デザイン性**: プロフェッショナルなHTMLメール
4. **変数バインディング**: 動的なコンテンツ生成

### テンプレート設計のベストプラクティス

#### 1. レイアウトの共通化

**ファイルパス**: `src/main/resources/templates/email/layout.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="${subject}">メール</title>
    <style>
        /* 共通スタイル */
        body {
            font-family: 'Helvetica Neue', Arial, sans-serif;
            line-height: 1.6;
            color: #333;
            max-width: 600px;
            margin: 0 auto;
            padding: 0;
            background-color: #f4f4f4;
        }
        .email-container {
            background-color: white;
            margin: 20px;
            border-radius: 8px;
            overflow: hidden;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px 20px;
            text-align: center;
        }
        .header h1 {
            margin: 0;
            font-size: 24px;
        }
        .content {
            padding: 30px 20px;
        }
        .button {
            display: inline-block;
            padding: 12px 30px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white !important;
            text-decoration: none;
            border-radius: 5px;
            margin: 20px 0;
            font-weight: bold;
        }
        .footer {
            padding: 20px;
            text-align: center;
            font-size: 12px;
            color: #777;
            background-color: #f9f9f9;
        }
        .footer a {
            color: #667eea;
            text-decoration: none;
        }
    </style>
</head>
<body>
    <div class="email-container">
        <div class="header">
            <h1 th:text="${headerTitle}">タイトル</h1>
        </div>
        <div class="content" th:insert="~{::content}">
            <!-- 個別コンテンツがここに挿入される -->
        </div>
        <div class="footer">
            <p>&copy; <span th:text="${#dates.year(#dates.createNow())}">2025</span> Spring Boot App. All rights reserved.</p>
            <p>
                <a href="#">利用規約</a> | 
                <a href="#">プライバシーポリシー</a> | 
                <a href="#">お問い合わせ</a>
            </p>
        </div>
    </div>
</body>
</html>
```

#### 2. コンポーネント化されたテンプレート

**ファイルパス**: `src/main/resources/templates/email/user-activation.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>アカウント有効化</title>
    <style th:replace="~{email/layout :: style}"></style>
</head>
<body>
    <div class="email-container">
        <div class="header">
            <h1>🎉 アカウント有効化</h1>
        </div>
        <div class="content">
            <p>こんにちは、<strong th:text="${username}">ユーザー名</strong>さん</p>
            
            <p>アカウント登録ありがとうございます！<br>
            以下のボタンをクリックしてアカウントを有効化してください。</p>
            
            <div style="text-align: center;">
                <a th:href="${activationUrl}" class="button">
                    アカウントを有効化する
                </a>
            </div>
            
            <p style="margin-top: 30px;">
                <small>ボタンが機能しない場合は、以下のURLをコピーしてブラウザに貼り付けてください：<br>
                <code th:text="${activationUrl}" style="word-break: break-all;">URL</code></small>
            </p>
            
            <p style="margin-top: 30px; padding: 15px; background-color: #fff3cd; border-left: 4px solid #ffc107;">
                ⚠️ このリンクは<strong th:text="${expirationHours}">24</strong>時間有効です。<br>
                心当たりがない場合は、このメールを無視してください。
            </p>
        </div>
        <div class="footer">
            <p>&copy; 2025 Spring Boot App. All rights reserved.</p>
        </div>
    </div>
</body>
</html>
```

#### 3. 条件分岐とループ

**ファイルパス**: `src/main/resources/templates/email/order-confirmation.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>ご注文確認</title>
</head>
<body>
    <div class="email-container">
        <div class="header">
            <h1>📦 ご注文確認</h1>
        </div>
        <div class="content">
            <p>こんにちは、<strong th:text="${customerName}">お客様名</strong>さん</p>
            
            <p>ご注文ありがとうございます。注文番号: <strong th:text="${orderNumber}">ORDER-001</strong></p>
            
            <h3>注文内容</h3>
            <table style="width: 100%; border-collapse: collapse;">
                <thead>
                    <tr style="background-color: #f9f9f9;">
                        <th style="padding: 10px; text-align: left; border-bottom: 2px solid #ddd;">商品名</th>
                        <th style="padding: 10px; text-align: center; border-bottom: 2px solid #ddd;">数量</th>
                        <th style="padding: 10px; text-align: right; border-bottom: 2px solid #ddd;">金額</th>
                    </tr>
                </thead>
                <tbody>
                    <tr th:each="item : ${orderItems}">
                        <td style="padding: 10px; border-bottom: 1px solid #eee;" th:text="${item.productName}">商品名</td>
                        <td style="padding: 10px; text-align: center; border-bottom: 1px solid #eee;" th:text="${item.quantity}">1</td>
                        <td style="padding: 10px; text-align: right; border-bottom: 1px solid #eee;">
                            ¥<span th:text="${#numbers.formatInteger(item.price, 0, 'COMMA')}">1,000</span>
                        </td>
                    </tr>
                </tbody>
                <tfoot>
                    <tr th:if="${shippingFee > 0}">
                        <td colspan="2" style="padding: 10px; text-align: right;">送料:</td>
                        <td style="padding: 10px; text-align: right;">
                            ¥<span th:text="${#numbers.formatInteger(shippingFee, 0, 'COMMA')}">500</span>
                        </td>
                    </tr>
                    <tr style="font-weight: bold; font-size: 16px;">
                        <td colspan="2" style="padding: 10px; text-align: right; border-top: 2px solid #ddd;">合計:</td>
                        <td style="padding: 10px; text-align: right; border-top: 2px solid #ddd;">
                            ¥<span th:text="${#numbers.formatInteger(totalAmount, 0, 'COMMA')}">10,500</span>
                        </td>
                    </tr>
                </tfoot>
            </table>
            
            <div style="margin-top: 30px; padding: 15px; background-color: #e8f5e9; border-radius: 5px;">
                <p style="margin: 0;"><strong>配送先住所:</strong></p>
                <p style="margin: 5px 0 0 0;" th:text="${shippingAddress}">住所</p>
            </div>
            
            <p style="margin-top: 30px;">発送完了後、追跡番号をお送りいたします。</p>
        </div>
        <div class="footer">
            <p>&copy; 2025 Spring Boot App. All rights reserved.</p>
        </div>
    </div>
</body>
</html>
```

### EmailServiceの拡張

```java
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    /**
     * アカウント有効化メール送信
     */
    public void sendActivationEmail(String to, String username, String activationToken) 
            throws MessagingException {
        Map<String, Object> variables = Map.of(
                "username", username,
                "activationUrl", "http://localhost:8080/activate?token=" + activationToken,
                "expirationHours", 24
        );
        sendTemplateEmail(to, "アカウント有効化のお願い", "email/user-activation", variables);
    }

    /**
     * 注文確認メール送信
     */
    public void sendOrderConfirmationEmail(String to, String customerName, 
                                          String orderNumber, 
                                          List<OrderItem> orderItems,
                                          int shippingFee,
                                          int totalAmount,
                                          String shippingAddress) throws MessagingException {
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerName", customerName);
        variables.put("orderNumber", orderNumber);
        variables.put("orderItems", orderItems);
        variables.put("shippingFee", shippingFee);
        variables.put("totalAmount", totalAmount);
        variables.put("shippingAddress", shippingAddress);
        
        sendTemplateEmail(to, "ご注文確認 - " + orderNumber, "email/order-confirmation", variables);
    }

    /**
     * テンプレートメール送信（共通メソッド）
     */
    private void sendTemplateEmail(String to, String subject, 
                                  String templateName, 
                                  Map<String, Object> variables) throws MessagingException {
        Context context = new Context();
        context.setVariables(variables);

        String htmlBody = templateEngine.process(templateName, context);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom("noreply@example.com", "Spring Boot App");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);

        mailSender.send(message);
        log.info("テンプレートメールを送信: {} (template: {})", to, templateName);
    }
}
```

### テンプレートテストのベストプラクティス

```java
@SpringBootTest
class EmailTemplateTest {

    @Autowired
    private TemplateEngine templateEngine;

    @Test
    void testWelcomeEmailTemplate() {
        Context context = new Context();
        context.setVariable("username", "山田太郎");
        
        String html = templateEngine.process("welcome-email", context);
        
        assertThat(html).contains("山田太郎");
        assertThat(html).contains("ようこそ");
    }

    @Test
    void testActivationEmailTemplate() {
        Context context = new Context();
        context.setVariable("username", "田中花子");
        context.setVariable("activationUrl", "http://localhost:8080/activate?token=abc123");
        context.setVariable("expirationHours", 24);
        
        String html = templateEngine.process("email/user-activation", context);
        
        assertThat(html).contains("田中花子");
        assertThat(html).contains("abc123");
        assertThat(html).contains("24時間");
    }
}
```

> **💡 Phase 5の復習**: Thymeleafテンプレートの基本構文（`th:text`, `th:each`, `th:if`など）は[STEP_21](../../phase5/STEP_21.md)で学習しました。メールテンプレートでも同じ構文が使えます！

---

## 🎨 チャレンジ課題

### チャレンジ 1: 添付ファイル送信

MimeMessageHelperを使って添付ファイル付きメールを送信してください。

### チャレンジ 2: メール送信履歴

EmailHistoryエンティティを作成して送信履歴を保存してください。

### チャレンジ 3: SendGridの使用

SendGridを使ったメール送信を実装してください。

---

## 📚 このステップで学んだこと

- ✅ Spring Boot Mailの使用
- ✅ JavaMailSenderによるメール送信
- ✅ Thymeleafテンプレートエンジン
- ✅ HTMLメール作成
- ✅ 非同期メール送信

---

## 🐛 トラブルシューティング

### エラー: "Page index must not be less than zero"

**原因**: ページ番号が0未満になっている

**解決策**:
```java
// ❌ NG: ユーザー入力をそのまま使用
@GetMapping("/users")
public String list(@RequestParam(defaultValue = "1") int page, Model model) {
    Page<User> users = userService.findAll(PageRequest.of(page, 10));  // page=0でもエラー
}

// ✅ OK: ページ番号を調整（1始まり→0始まり）
@GetMapping("/users")
public String list(@RequestParam(defaultValue = "1") int page, Model model) {
    int pageIndex = Math.max(0, page - 1);  // 1ページ目 → インデックス0
    Page<User> users = userService.findAll(PageRequest.of(pageIndex, 10));
    model.addAttribute("currentPage", page);  // ビューには1始まりで渡す
}
```

### エラー: ページネーションリンクが正しく動作しない

**原因**: URLパラメータが正しく生成されていない

**解決策**:
```html
<!-- ❌ NG: page番号だけ -->
<a th:href="@{/users(page=${page.number})}">次へ</a>

<!-- ✅ OK: 他のパラメータも保持 -->
<a th:href="@{/users(
    page=${page.number + 1},
    size=${page.size},
    sort=${sortParam}
)}">次へ</a>

<!-- ✅ OK: 1始まりに変換 -->
<a th:href="@{/users(page=${page.number + 2})}">次へ</a>
<!-- page.numberは0始まりなので、次ページは+2 -->
```

### 問題: ソート機能が効かない

**原因**: ソートパラメータの形式が間違っている

**解決策**:
```java
// ❌ NG: 文字列のソート指定
@GetMapping("/users")
public String list(@RequestParam(defaultValue = "id") String sort) {
    Sort sortBy = Sort.by(sort);  // 昇順のみ
}

// ✅ OK: 昇順・降順を指定
@GetMapping("/users")
public String list(
    @RequestParam(defaultValue = "id") String sort,
    @RequestParam(defaultValue = "asc") String direction) {
    
    Sort.Direction dir = "desc".equalsIgnoreCase(direction) 
        ? Sort.Direction.DESC 
        : Sort.Direction.ASC;
    Sort sortBy = Sort.by(dir, sort);
    
    Pageable pageable = PageRequest.of(page, size, sortBy);
}
```

### 問題: 大量データでページネーションが遅い

**原因**: COUNT クエリが重い、またはインデックスがない

**解決策**:

**1. インデックスを追加**
```sql
CREATE INDEX idx_users_created_at ON users(created_at);
CREATE INDEX idx_users_name ON users(name);
```

**2. COUNTクエリを最適化**
```java
// MyBatisの場合
@Select("SELECT * FROM users ORDER BY id LIMIT #{offset}, #{limit}")
List<User> findWithOffset(@Param("offset") int offset, @Param("limit") int limit);

// 総件数はキャッシュ
@Cacheable("userCount")
@Select("SELECT COUNT(*) FROM users")
long count();
```

**3. Cursorベースのページネーション（無限スクロール）**
```java
@GetMapping("/api/users")
public List<User> list(@RequestParam(required = false) Long cursor, 
                       @RequestParam(defaultValue = "20") int size) {
    if (cursor == null) {
        return userRepository.findTop(size);
    } else {
        return userRepository.findByIdGreaterThan(cursor, PageRequest.of(0, size));
    }
}
```

### 問題: Thymeleafテンプレートでページネーション情報が取得できない

**原因**: Pageオブジェクトをそのまま渡していない

**解決策**:
```java
// ✅ PageオブジェクトをModelに追加
@GetMapping("/users")
public String list(Pageable pageable, Model model) {
    Page<User> page = userService.findAll(pageable);
    model.addAttribute("page", page);  // Pageオブジェクト全体
    model.addAttribute("users", page.getContent());  // リストのみ
    return "users/list";
}
```

```html
<!-- Thymeleafで利用可能なプロパティ -->
<p>全 <span th:text="${page.totalElements}"></span> 件</p>
<p>ページ <span th:text="${page.number + 1}"></span> / <span th:text="${page.totalPages}"></span></p>

<!-- 前へ/次へボタン -->
<a th:if="${!page.first}" th:href="@{/users(page=${page.number})}">前へ</a>
<a th:if="${!page.last}" th:href="@{/users(page=${page.number + 2})}">次へ</a>
```

### 問題: REST APIでページネーションのメタ情報を返したい

**原因**: Contentのみ返している

**解決策**:
```java
// ❌ NG: リストのみ返す
@GetMapping("/api/users")
public List<User> list(Pageable pageable) {
    return userService.findAll(pageable).getContent();  // メタ情報がない
}

// ✅ OK: Pageオブジェクトをそのまま返す
@GetMapping("/api/users")
public Page<User> list(Pageable pageable) {
    return userService.findAll(pageable);  // メタ情報も含まれる
}

// ✅ OK: カスタムレスポンスDTOで返す
@GetMapping("/api/users")
public PageResponse<UserDto> list(Pageable pageable) {
    Page<User> page = userService.findAll(pageable);
    return new PageResponse<>(
        page.getContent().stream().map(this::toDto).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages()
    );
}
```

---

## 🔄 Gitへのコミットとレビュー依頼

```bash
git add .
git commit -m "Step 31: ページネーション完了"
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

次は[Step 32: キャッシュ](STEP_32.md)へ進みましょう！

Spring Cacheを使ったパフォーマンス最適化の基礎を学びます。

---

お疲れさまでした！ 🎉

ページネーション機能を習得しました！
