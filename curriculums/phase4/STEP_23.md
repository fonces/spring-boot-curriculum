# Step 23: OpenAPI/Swagger によるAPI仕様書

## 🎯 このステップの目標

- OpenAPI 3.0を理解する
- SpringDocを使ってAPI仕様書を自動生成する
- Swagger UIでAPIドキュメントを提供する
- カスタムアノテーションでドキュメントを充実させる

**所要時間**: 約1時間30分

---

## 💡 OpenAPIとは？

### API仕様書の重要性

- ✅ APIの使い方が明確になる
- ✅ フロントエンドとの連携がスムーズ
- ✅ テストが容易になる
- ✅ コードから自動生成できる

---

## 🚀 ステップ1: SpringDoc依存関係の追加

### 1-1. pom.xmlの更新

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

### 1-2. アプリケーション起動

依存関係を追加すると、自動的にSwagger UIが有効になります。

**アクセス**:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

---

## 🚀 ステップ2: OpenAPI設定

### 2-1. OpenAPIConfig

**ファイルパス**: `src/main/java/com/example/hellospringboot/config/OpenAPIConfig.java`

```java
package com.example.hellospringboot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI設定
 */
@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hello Spring Boot API")
                        .version("1.0.0")
                        .description("Spring Boot 3.5.7 カリキュラム用API")
                        .contact(new Contact()
                                .name("Your Name")
                                .email("your.email@example.com")
                                .url("https://example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
```

---

## 🚀 ステップ3: コントローラーのドキュメント化

### 3-1. UserControllerにアノテーション追加

```java
package com.example.hellospringboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "ユーザー管理API")
public class UserController {

    private final UserService userService;

    @Operation(summary = "ユーザー作成", description = "新しいユーザーを作成します")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "作成成功",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "バリデーションエラー"),
        @ApiResponse(responseCode = "409", description = "メールアドレス重複")
    })
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "ユーザー作成リクエスト",
                required = true,
                content = @Content(schema = @Schema(implementation = UserCreateRequest.class)))
            @Valid @RequestBody UserCreateRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "全ユーザー取得", description = "全ユーザーのリストを取得します")
    @ApiResponse(responseCode = "200", description = "取得成功")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "ユーザー取得", description = "IDを指定してユーザーを取得します")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "取得成功"),
        @ApiResponse(responseCode = "404", description = "ユーザーが見つかりません")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "ユーザーID", required = true, example = "1")
            @PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }
}
```

---

## 🚀 ステップ4: DTOのドキュメント化

### 4-1. UserCreateRequestにスキーマ情報追加

```java
package com.example.hellospringboot.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "ユーザー作成リクエスト")
public class UserCreateRequest {

    @Schema(description = "ユーザー名", example = "山田太郎", required = true)
    @NotBlank(message = "ユーザー名は必須です")
    @Size(min = 2, max = 100, message = "ユーザー名は2文字以上100文字以内で入力してください")
    private String name;

    @Schema(description = "メールアドレス", example = "taro@example.com", required = true)
    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "メールアドレスの形式が正しくありません")
    private String email;

    @Schema(description = "年齢", example = "30", minimum = "0", maximum = "150", required = true)
    @NotNull(message = "年齢は必須です")
    @Min(value = 0, message = "年齢は0以上で入力してください")
    @Max(value = 150, message = "年齢は150以下で入力してください")
    private Integer age;
}
```

---

## 🚀 ステップ5: application.ymlでカスタマイズ

### 5-1. SpringDoc設定

**ファイルパス**: `src/main/resources/application.yml`

```yaml
# SpringDoc設定
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: alpha
    display-request-duration: true
    doc-expansion: none
  show-actuator: false
```

---

## ✅ 動作確認

### Swagger UIにアクセス

```
http://localhost:8080/swagger-ui.html
```

**確認項目**:
- ✅ API一覧が表示される
- ✅ 各エンドポイントの説明が表示される
- ✅ リクエスト/レスポンスのスキーマが表示される
- ✅ 「Try it out」でAPIを直接テストできる

### OpenAPI JSONダウンロード

```bash
curl http://localhost:8080/v3/api-docs -o openapi.json
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: 認証の統合

Swagger UIでJWTトークンを使えるようにしてください。

### チャレンジ 2: グループ化

`@Tag`を使ってAPIをグループ化してください。

### チャレンジ 3: OpenAPIクライアント生成

OpenAPI Generatorで自動的にクライアントコードを生成してください。

---

## 📚 このステップで学んだこと

- ✅ SpringDocの導入
- ✅ Swagger UIの使用
- ✅ @Operationによるエンドポイント説明
- ✅ @Schemaによるモデル説明
- ✅ セキュリティスキームの設定

---

## 🔄 Phase 4完了！

```bash
git add .
git commit -m "Phase 4完了: STEP_21-23（テスト、統合テスト、OpenAPI）"
git push origin main
```

---

## ➡️ 次のPhase

Phase 4お疲れさまでした！次は**Phase 5: 実践機能実装**に進みます。

---

お疲れさまでした！ 🎉

Phase 4では、セキュリティ、テスト、API仕様書の作成方法を学びました。
これで実務レベルのSpring Bootアプリケーション開発の基礎が身につきました！
