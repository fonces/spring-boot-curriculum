# Step 39: テストとデプロイ準備

**所要時間**: 約120分

## 🎯 このステップの目標

BlogHubアプリケーションを本番環境にデプロイする準備を整えます。

**学ぶこと**:
- Mockitoを使ったユニットテストの実装
- MockMvcを使った統合テストの実装
- JaCoCoによるテストカバレッジの測定と改善
- 本番環境用の設定ファイルの作成
- Dockerコンテナ化による環境の標準化
- 環境変数による秘密情報の管理

**成果物**:
- 包括的なテストスイート（カバレッジ70%以上）
- 本番環境用の設定ファイル
- Dockerfileとdocker-compose-prod.yml

---

## 📋 事前準備

### 前提条件

- Step 38までの実装が完了していること
- Docker、Docker Composeがインストールされていること
- テスト用データベースの準備ができていること

### 確認事項

```bash
# Dockerのバージョン確認
docker --version

# Docker Composeのバージョン確認
docker-compose --version

# 既存のテストが動作することを確認
cd workspace/bloghub
./mvnw test
```

---

## 🛠️ ステップ1: テスト環境のセットアップ

### 1-1. pom.xmlにテスト用依存関係を追加

以下の依存関係を`pom.xml`の`<dependencies>`セクションに追加します：

```xml
<!-- テスト用依存関係 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

### 1-2. JaCoCoプラグインを追加

`pom.xml`の`<build><plugins>`セクションに以下を追加します：

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.70</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 1-3. テスト用application.ymlを作成

**ファイルパス**: `src/test/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  
  security:
    jwt:
      secret: test-secret-key-for-testing-purposes-must-be-at-least-256-bits
      expiration: 86400000

logging:
  level:
    com.example.bloghub: DEBUG
```

---

## 📝 ステップ2: ユニットテストの実装 【自分で実装】

### 📋 AuthServiceTestの実装要件

**ファイルパス**: `src/test/java/com/example/bloghub/service/AuthServiceTest.java`

### 必要なテストケース

| テストメソッド | 説明 | 期待結果 |
|--------------|------|---------|
| `register_Success` | 新規ユーザー登録が成功 | JWTトークンが返る |
| `register_DuplicateUsername` | 重複ユーザー名で登録 | `IllegalArgumentException` |
| `register_DuplicateEmail` | 重複メールで登録 | `IllegalArgumentException` |
| `login_Success` | 正しい認証情報でログイン | JWTトークンが返る |
| `login_UserNotFound` | 存在しないユーザー | `IllegalArgumentException` |
| `login_WrongPassword` | 間違ったパスワード | `IllegalArgumentException` |

### 使用するアノテーション

| アノテーション | 説明 |
|---------------|------|
| `@ExtendWith(MockitoExtension.class)` | Mockito拡張を有効化 |
| `@Mock` | モックオブジェクトを生成 |
| `@InjectMocks` | モックを注入したテスト対象 |
| `@BeforeEach` | 各テスト前のセットアップ |
| `@Test` | テストメソッドを示す |
| `@DisplayName` | テストの説明 |

### Mockitoの主要メソッド

```java
// モックの振る舞いを定義
when(repository.findById(1L)).thenReturn(Optional.of(entity));
when(repository.save(any(User.class))).thenReturn(savedUser);

// メソッド呼び出しを検証
verify(repository, times(1)).save(any(User.class));
verify(repository, never()).delete(any());

// 例外をスローさせる
when(service.method()).thenThrow(new RuntimeException());
```

### AssertJの主要メソッド

```java
// 値の検証
assertThat(result).isEqualTo("expected");
assertThat(result).isNotNull();
assertThat(list).hasSize(3);

// 例外の検証
assertThatThrownBy(() -> service.method())
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("error message");
```

---

### 📋 ArticleServiceTestの実装要件

**ファイルパス**: `src/test/java/com/example/bloghub/service/ArticleServiceTest.java`

### 必要なテストケース

| テストメソッド | 説明 | 期待結果 |
|--------------|------|---------|
| `createArticle_Success` | 記事を正常に作成 | 作成された記事が返る |
| `createArticle_UserNotFound` | 存在しないユーザー | `IllegalArgumentException` |
| `getAllArticles_Success` | 記事一覧を取得 | Page<Article>が返る |
| `getArticleById_Success` | IDで記事を取得 | 記事が返る |
| `getArticleById_NotFound` | 存在しないID | `IllegalArgumentException` |
| `updateArticle_Success` | 作成者が記事を更新 | 更新された記事が返る |
| `updateArticle_Forbidden` | 作成者以外が更新 | `IllegalArgumentException` |
| `deleteArticle_Success` | 作成者が記事を削除 | 正常完了 |
| `deleteArticle_Forbidden` | 作成者以外が削除 | `IllegalArgumentException` |

### テストデータのセットアップ例

```java
@BeforeEach
void setUp() {
    testUser = new User();
    testUser.setId(1L);
    testUser.setUsername("testuser");
    testUser.setEmail("test@example.com");

    testArticle = new Article();
    testArticle.setId(1L);
    testArticle.setTitle("Test Article");
    testArticle.setContent("Test Content");
    testArticle.setAuthor(testUser);
    testArticle.setCreatedAt(LocalDateTime.now());
}
```

---

## 📝 ステップ3: 統合テストの実装 【自分で実装】

### 📋 AuthControllerIntegrationTestの実装要件

**ファイルパス**: `src/test/java/com/example/bloghub/controller/AuthControllerIntegrationTest.java`

### 必要なクラスレベルアノテーション

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController 統合テスト")
class AuthControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
}
```

### 必要なテストケース

| テストメソッド | エンドポイント | 期待ステータス |
|--------------|--------------|---------------|
| `register_Success` | `POST /api/auth/register` | 200 OK |
| `register_DuplicateUsername` | `POST /api/auth/register` | 400 Bad Request |
| `register_EmptyUsername` | `POST /api/auth/register` | 400 Bad Request |
| `login_Success` | `POST /api/auth/login` | 200 OK |
| `login_WrongPassword` | `POST /api/auth/login` | 400 Bad Request |
| `login_UserNotFound` | `POST /api/auth/login` | 400 Bad Request |

### MockMvcの基本パターン

```java
mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.token").isNotEmpty());
```

---

### 📋 ArticleControllerIntegrationTestの実装要件

**ファイルパス**: `src/test/java/com/example/bloghub/controller/ArticleControllerIntegrationTest.java`

### 必要なテストケース

| テストメソッド | エンドポイント | 認証 | 期待ステータス |
|--------------|--------------|------|---------------|
| `createArticle_Success` | `POST /api/articles` | 必要 | 200 OK |
| `createArticle_Unauthorized` | `POST /api/articles` | なし | 401 Unauthorized |
| `getAllArticles_Success` | `GET /api/articles` | 不要 | 200 OK |
| `getArticleById_Success` | `GET /api/articles/{id}` | 不要 | 200 OK |
| `getArticleById_NotFound` | `GET /api/articles/{id}` | 不要 | 404 Not Found |
| `updateArticle_Success` | `PUT /api/articles/{id}` | 必要 | 200 OK |
| `updateArticle_Forbidden` | `PUT /api/articles/{id}` | 別ユーザー | 403 Forbidden |
| `deleteArticle_Success` | `DELETE /api/articles/{id}` | 必要 | 204 No Content |
| `deleteArticle_Forbidden` | `DELETE /api/articles/{id}` | 別ユーザー | 403 Forbidden |

### JWT認証付きリクエストのパターン

```java
// JWTトークンを生成
String jwtToken = jwtTokenProvider.generateToken(testUser.getUsername());

// 認証付きリクエスト
mockMvc.perform(post("/api/articles")
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
    .andExpect(status().isOk());
```

### レスポンスの検証パターン

```java
// JSONパスで値を検証
.andExpect(jsonPath("$.title").value("Test Article"))
.andExpect(jsonPath("$.content").value("Test Content"))
.andExpect(jsonPath("$.author.username").value("testuser"));

// 配列サイズを検証
.andExpect(jsonPath("$.content", hasSize(1)));

// 値が存在することを検証
.andExpect(jsonPath("$.token").isNotEmpty());
```

---

## 📝 ステップ4: テストの実行とカバレッジ確認

### 4-1. テストの実行

```bash
# すべてのテストを実行
./mvnw clean test

# 期待される結果:
# Tests run: XX, Failures: 0, Errors: 0, Skipped: 0
```

### 4-2. カバレッジレポートの生成

```bash
# JaCoCoレポートを生成
./mvnw jacoco:report

# ブラウザでレポートを開く（macOS）
open target/site/jacoco/index.html

# ブラウザでレポートを開く（Linux/WSL2）
xdg-open target/site/jacoco/index.html
```

### 4-3. カバレッジの確認ポイント

| レイヤー | 目標カバレッジ |
|---------|---------------|
| Service | 80%以上 |
| Controller | 70%以上 |
| 全体 | 70%以上 |

カバレッジが不足している場合は、追加のテストを実装してください。

---

## 📝 ステップ5: 本番環境設定 【自分で実装】

### 5-1. application-prod.ymlの作成

**ファイルパス**: `src/main/resources/application-prod.yml`

### 設定項目一覧

| 設定項目 | 説明 | 環境変数 |
|---------|------|---------|
| `spring.datasource.url` | DB接続URL | `${DB_URL}` |
| `spring.datasource.username` | DBユーザー名 | `${DB_USERNAME}` |
| `spring.datasource.password` | DBパスワード | `${DB_PASSWORD}` |
| `spring.jpa.hibernate.ddl-auto` | DDL自動生成 | `validate` |
| `spring.jpa.show-sql` | SQLログ | `false` |
| `spring.security.jwt.secret` | JWTシークレット | `${JWT_SECRET}` |
| `logging.level.root` | ログレベル | `INFO` |
| `logging.file.name` | ログファイル | `logs/bloghub.log` |

### 本番環境での注意点

- `ddl-auto: validate`でスキーマ変更を防ぐ
- SQLログは無効化してパフォーマンス向上
- 環境変数で秘密情報を管理
- ファイルログを設定

---

## 📝 ステップ6: Docker化 【自分で実装】

### 6-1. Dockerfileの作成

**ファイルパス**: `Dockerfile`（プロジェクトルート）

### Dockerfile構成要件

| ステージ | ベースイメージ | 目的 |
|---------|---------------|------|
| builder | `eclipse-temurin:21-jdk-alpine` | アプリケーションビルド |
| runtime | `eclipse-temurin:21-jre-alpine` | 実行環境 |

### ビルドステージの処理

1. Maven Wrapperとpom.xmlをコピー
2. 依存関係をダウンロード（`./mvnw dependency:go-offline`）
3. ソースコードをコピー
4. アプリケーションをビルド（`./mvnw clean package -DskipTests`）

### 実行ステージの処理

1. ビルドステージからJARファイルをコピー
2. ログディレクトリを作成
3. ヘルスチェックを設定
4. ポート8080を公開
5. `java -jar app.jar`で起動

### ヘルスチェックの設定

```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1
```

### 6-2. docker-compose-prod.ymlの作成

**ファイルパス**: `docker-compose-prod.yml`

### サービス構成

| サービス | イメージ | 役割 |
|---------|---------|------|
| mysql | `mysql:8.0` | データベース |
| app | `build: .` | アプリケーション |

### MySQLサービスの設定要件

- ボリュームでデータ永続化
- init.sqlで初期化
- ヘルスチェック設定
- 環境変数でパスワード管理

### Appサービスの設定要件

- MySQLのヘルスチェック完了後に起動（`depends_on`）
- 環境変数でSpring設定を注入
- ログディレクトリをボリュームマウント

---

## 📝 ステップ7: 環境変数ファイルの作成

### 7-1. .env.exampleの作成

```env
# MySQL設定
MYSQL_ROOT_PASSWORD=your_strong_root_password_here
DB_USERNAME=bloghub_user
DB_PASSWORD=your_strong_db_password_here

# JWT設定（最低256ビット必要）
JWT_SECRET=your_jwt_secret_key_must_be_at_least_256_bits_long

# Spring Boot設定
SPRING_PROFILES_ACTIVE=prod
```

### 7-2. .gitignoreへの追加

```
# 環境変数ファイル
.env
```

### 7-3. 強力なシークレットキーの生成

```bash
# 64文字のランダムキーを生成
openssl rand -base64 48
```

---

## ✅ 動作確認

### 1. テストの実行

```bash
./mvnw clean test

# 期待される結果:
# Tests run: XX, Failures: 0, Errors: 0, Skipped: 0
```

### 2. カバレッジの確認

```bash
./mvnw jacoco:report
open target/site/jacoco/index.html  # macOS
xdg-open target/site/jacoco/index.html  # Linux
```

### 3. Dockerビルド

```bash
docker build -t bloghub:latest .
docker images | grep bloghub
```

### 4. 本番環境起動

```bash
cp .env.example .env
# .envを編集して実際の値を設定

docker-compose -f docker-compose-prod.yml up -d
docker-compose -f docker-compose-prod.yml logs -f app
```

### 5. ヘルスチェック

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

### 6. API動作確認

```bash
# ユーザー登録
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"produser","email":"prod@example.com","password":"password123"}'
```

---

## 🐛 トラブルシューティング

### エラー: テストでデータベース接続エラー

**原因**: H2データベースの設定が正しくない

**解決策**:
1. `src/test/resources/application.yml`が存在することを確認
2. H2依存関係が`pom.xml`に含まれていることを確認
3. `@ActiveProfiles("test")`が付いていることを確認

### エラー: JaCoCoレポートが生成されない

**解決策**:
```bash
./mvnw clean test jacoco:report
```

### エラー: Dockerビルドでmvnwに実行権限がない

**解決策**:
```bash
chmod +x mvnw
```

### エラー: JWT_SECRETが短すぎる

**解決策**:
```bash
openssl rand -base64 48
# 生成されたキーを.envに設定
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: E2Eテストの実装

SeleniumまたはPlaywrightを使って、ブラウザベースのE2Eテストを実装してください。

**テストシナリオ**:
1. ユーザー登録フォームから新規登録
2. ログインフォームでログイン
3. 記事作成フォームから新規記事を投稿
4. 記事一覧ページで投稿した記事を確認

### チャレンジ 2: GitHub Actions CI/CD

GitHub Actionsで自動テスト・自動デプロイを実装してください。

**設定ファイル**: `.github/workflows/ci.yml`

### チャレンジ 3: クラウドデプロイ

AWS ECS、Google Cloud Run、またはAzure App Serviceにデプロイしてください。

---

## 📚 まとめ

お疲れさまでした！このステップでは、本番環境にデプロイ可能な高品質なアプリケーションを構築するための重要なスキルを学びました。

**学んだこと**:
- ✅ Mockitoによるユニットテストの設計と実装
- ✅ MockMvcによる統合テストの設計と実装
- ✅ JaCoCoによるカバレッジ測定と改善
- ✅ テスト駆動開発の重要性
- ✅ 本番環境設定の分離
- ✅ 環境変数による秘密情報管理
- ✅ Dockerによるコンテナ化
- ✅ マルチステージビルドによる最適化
- ✅ ヘルスチェックの実装

---

## 🎓 Phase 8 完了！

### 🎉 おめでとうございます！

**Phase 8: 総合演習（BlogHub）の全ステップを完了しました！**

あなたはSpring Boot 3.5を使った本格的なWebアプリケーション開発のすべてを学び、実践しました。

### これまでの成果

| Phase | 内容 |
|-------|------|
| Phase 1-2 | Spring Bootの基礎とデータベース連携 |
| Phase 3 | MyBatisによるSQL制御 |
| Phase 4 | アーキテクチャとベストプラクティス |
| Phase 5 | Thymeleafでサーバーサイドレンダリング |
| Phase 6 | セキュリティとテスト |
| Phase 7 | 実践的な機能 |
| Phase 8 | 総合演習（BlogHub） |

### 次の学習パス

| パス | 説明 |
|------|------|
| Spring Cloud | マイクロサービスアーキテクチャ |
| Kotlin + Spring Boot | 簡潔な文法でSpring Boot開発 |
| GraphQL with Spring Boot | RESTの代替としてのGraphQL |
| Spring WebFlux | リアクティブプログラミング |

**あなたのSpring Boot開発の旅は、ここから新たな章を迎えます。** 🚀

---

**参考リソース**:
- [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/reference/3.5.8/)
- [Testing in Spring Boot](https://docs.spring.io/spring-boot/reference/testing/index.html)
- [JaCoCo Documentation](https://www.jacoco.org/jacoco/trunk/doc/)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)

---

**前のステップ**: [Step 38: Thymeleafでブログ画面実装](STEP_38.md)  
**Phase 8完了**: すべてのステップを完了しました！おめでとうございます！🎉
