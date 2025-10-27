# Step 25: メール送信機能

## 🎯 このステップの目標

- Spring Boot Mailを使ったメール送信を学ぶ
- HTMLメールを送信する
- テンプレートエンジンを使う
- 非同期メール送信を実装する

**所要時間**: 約1時間30分

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

## 🔄 Gitへのコミット

```bash
git add .
git commit -m "Phase 5: STEP_25完了（メール送信機能）"
git push origin main
```

---

## ➡️ 次のステップ

次は[Step 26: キャッシング](STEP_26.md)へ進みましょう！

---

お疲れさまでした！ 🎉
