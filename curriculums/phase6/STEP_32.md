# Step 32: 総合テストとデプロイ準備

## 🎯 このステップの目標

- 総合的なテストを実装する
- E2Eテストを作成する
- Dockerコンテナ化
- CI/CDパイプラインの構築
- 本番環境へのデプロイ

**所要時間**: 約3時間

---

## 📋 実装要件

このステップでは、タスク管理システムを本番環境にデプロイするための準備を行います。

### 実装する内容

1. **総合テスト**
   - 統合テスト（複数のコンポーネントを連携）
   - E2Eテスト（ユーザーシナリオベース）
   - パフォーマンステスト

2. **Docker化**
   - Dockerfile作成
   - docker-compose.yml作成
   - マルチステージビルド

3. **CI/CDパイプライン**
   - GitHub Actionsの設定
   - 自動テスト
   - 自動デプロイ

4. **本番環境設定**
   - application-prod.yml
   - セキュリティ設定
   - モニタリング設定

---

## 🚀 ステップ1: 総合的なテスト実装

### 1-1. TaskManagementIntegrationTest

**テストシナリオ**:
1. プロジェクト作成
2. タスク作成
3. タスク更新
4. コメント追加
5. ファイル添付
6. タスク完了
7. 統計情報確認

**実装要件**:
- `@SpringBootTest`を使用
- `TestRestTemplate`または`MockMvc`を使用
- トランザクションをロールバック（`@Transactional`）
- テストデータは各テストメソッドで作成
- アサーションで期待値を検証

**テストメソッド例**:
```java
@Test
void タスク管理の一連の流れをテストする()

@Test  
void 権限がない場合はアクセスできない()

@Test
void 期限切れタスクのリマインダーが送信される()
```

**配置場所**: `src/test/java/com/example/hellospringboot/integration/`

### 1-2. E2Eテスト（オプション）

**要件**:
- Selenium WebDriverを使用
- 実際のブラウザでテスト
- ログイン → タスク作成 → 完了までのフロー

**依存関係**（必要な場合のみ追加）:
```xml
<dependency>
    <groupId>org.seleniumhq.selenium</groupId>
    <artifactId>selenium-java</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 🚀 ステップ2: Dockerコンテナ化

### 2-1. Dockerfile

**要件**:
- マルチステージビルドを使用
- ステージ1: ビルドステージ（Maven）
- ステージ2: ランタイムステージ（JRE）
- 最終イメージサイズを最小化

**実装のポイント**:
```dockerfile
# ビルドステージ
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# ランタイムステージ  
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**配置場所**: プロジェクトルートに`Dockerfile`

### 2-2. docker-compose.yml

**必須サービス**:
- `app` - Spring Bootアプリケーション
- `mysql` - MySQLデータベース
- `redis` - Redisキャッシュ

**実装要件**:
- 環境変数でDB接続情報を設定
- ボリュームでデータ永続化
- ネットワークでサービス間通信
- ヘルスチェック設定

**主要な設定項目**:
```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD
      - MYSQL_DATABASE
      - MYSQL_USER
      - MYSQL_PASSWORD
    volumes:
      - mysql-data:/var/lib/mysql
    healthcheck: # ping -uroot -p$$MYSQL_ROOT_PASSWORD

  redis:
    image: redis:7-alpine
    
  app:
    build: .
    depends_on:
      mysql: condition: service_healthy
      redis: condition: service_started
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL
      - SPRING_DATASOURCE_USERNAME
      - SPRING_DATASOURCE_PASSWORD
      - SPRING_DATA_REDIS_HOST=redis
```

**配置場所**: プロジェクトルートに`docker-compose.yml`

### 2-3. .dockerignore

**除外するファイル**:
```
target/
.git/
.idea/
*.iml
.env
```

**配置場所**: プロジェクトルートに`.dockerignore`

---

## 🚀 ステップ3: 本番環境設定

### 3-1. application-prod.yml

**必須設定項目**:

#### データソース
```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate  # 本番ではvalidateまたはnone
    show-sql: false  # 本番ではfalse
```

#### セキュリティ
```yaml
app:
  jwt:
    secret: ${JWT_SECRET}  # 環境変数から取得
    expiration: 86400000  # 24時間
```

#### ログ設定
```yaml
logging:
  level:
    root: INFO
    com.example.hellospringboot: INFO
  file:
    name: /var/log/app/application.log
```

#### メール設定（SMTP）
```yaml
spring:
  mail:
    host: ${SMTP_HOST}
    port: ${SMTP_PORT}
    username: ${SMTP_USERNAME}
    password: ${SMTP_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
```

**配置場所**: `src/main/resources/application-prod.yml`

### 3-2. 環境変数の設定

**.env.example**（Gitにコミット）:
```
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/taskmanagement
SPRING_DATASOURCE_USERNAME=dbuser
SPRING_DATASOURCE_PASSWORD=change-me
JWT_SECRET=change-me-to-very-long-secret-key
SMTP_HOST=smtp.example.com
SMTP_PORT=587
SMTP_USERNAME=noreply@example.com
SMTP_PASSWORD=change-me
```

**.env**（.gitignoreに追加、実際の値を設定）

**配置場所**: プロジェクトルート

---

## 🚀 ステップ4: CI/CDパイプライン

### 4-1. GitHub Actions ワークフロー

**ファイル名**: `.github/workflows/ci-cd.yml`

**必須ジョブ**:

#### 1. テストジョブ
```yaml
test:
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v3
    - name: Set up JDK 21
      uses: actions/setup-java@v3
    - name: Run tests
      run: mvn test
```

#### 2. ビルドジョブ
```yaml
build:
  needs: test
  steps:
    - name: Build with Maven
      run: mvn clean package -DskipTests
    - name: Upload artifact
```

#### 3. Dockerイメージビルド＆プッシュ
```yaml
docker:
  needs: build
  steps:
    - name: Build Docker image
    - name: Push to Docker Hub
```

**実装のポイント**:
- テスト → ビルド → Dockerイメージ作成の順序
- キャッシュを使ってビルド時間短縮
- シークレット管理（GitHub Secrets）
- mainブランチへのpush時に実行

**配置場所**: `.github/workflows/ci-cd.yml`

### 4-2. GitHub Secretsの設定

**必要なシークレット**:
- `DOCKER_USERNAME` - Docker Hubユーザー名
- `DOCKER_PASSWORD` - Docker Hubパスワード
- `JWT_SECRET` - JWT署名キー
- その他DB接続情報など

**設定方法**: GitHubリポジトリ → Settings → Secrets and variables → Actions

---

## ✅ 動作確認

### Dockerで起動

```bash
# イメージビルド
docker-compose build

# コンテナ起動
docker-compose up -d

# ログ確認
docker-compose logs -f app

# 起動確認
curl http://localhost:8080/actuator/health
```

### テストの実行

```bash
# 全テスト実行
mvn test

# 統合テストのみ
mvn test -Dtest=*IntegrationTest

# カバレッジレポート生成
mvn jacoco:report
```

### CI/CDの確認

1. GitHubにpush
2. Actionsタブで実行状況確認
3. 全てのジョブがグリーンになることを確認

---

## 💡 実装のポイント

### テスト
- **統合テスト**: 実際のDBを使用してテスト
- **テストデータ**: `@Sql`アノテーションでSQLスクリプト実行
- **モック**: 外部サービス（メール送信など）はモック化
- **カバレッジ**: 80%以上を目標

### Docker
- **マルチステージビルド**: 最終イメージを小さく
- **ヘルスチェック**: アプリケーションの起動確認
- **環境変数**: 設定を外部化
- **ボリューム**: データの永続化

### CI/CD
- **自動テスト**: プッシュごとにテスト実行
- **並列実行**: ジョブを並列化して高速化
- **通知**: Slackなどへの通知設定
- **ロールバック**: デプロイ失敗時の対策

### セキュリティ
- **シークレット管理**: 環境変数やGitHub Secretsを使用
- **HTTPS**: 本番環境では必須
- **Rate Limiting**: API呼び出し回数制限
- **監視**: ログとメトリクスの収集

---

## 🎨 チャレンジ課題

### チャレンジ 1: Kubernetes対応

Kubernetes用のマニフェストファイル（Deployment, Service, Ingress）を作成してください。

### チャレンジ 2: Blue-Greenデプロイ

ダウンタイムゼロでデプロイできるBlue-Greenデプロイメント戦略を実装してください。

### チャレンジ 3: モニタリング

Prometheus + Grafanaでメトリクスを可視化してください。

**ヒント**: Spring Boot Actuator + Micrometer

### チャレンジ 4: 負荷テスト

JMeterやGatlingを使って負荷テストを実施し、パフォーマンスのボトルネックを特定してください。

---

## 📊 デプロイチェックリスト

本番デプロイ前に以下を確認してください：

- [ ] 全テストがパス
- [ ] セキュリティ設定（HTTPS, CORS, CSRFなど）
- [ ] 環境変数が正しく設定されている
- [ ] データベースマイグレーション計画
- [ ] ログ収集・監視設定
- [ ] バックアップ戦略
- [ ] ロールバック手順
- [ ] ドキュメント整備（README, API仕様書）
- [ ] パフォーマンステスト実施
- [ ] セキュリティ診断

---

## 📚 このステップで学んだこと

- ✅ 統合テストとE2Eテスト
- ✅ Dockerコンテナ化
- ✅ マルチステージビルド
- ✅ docker-composeでの環境構築
- ✅ CI/CDパイプライン
- ✅ 本番環境設定
- ✅ セキュリティベストプラクティス

---

## 🔄 Gitへのコミット

```bash
git add .
git commit -m "Phase 6完了: STEP_32（総合テストとデプロイ準備）"
git push origin main
```

---

## 🎓 Phase 6 完了！

おめでとうございます！🎉

これでSpring Boot カリキュラムの全ステップが完了しました。

### 学習した内容の総まとめ

**Phase 2**: データベース連携の基礎
- JPA, Spring Data JPA, H2/MySQL

**Phase 3**: アーキテクチャとベストプラクティス
- レイヤードアーキテクチャ, DTO, 例外処理, ロギング

**Phase 4**: セキュリティとテスト
- Spring Security, JWT, 単体テスト, 統合テスト, OpenAPI

**Phase 5**: 実践的な機能
- ファイルアップロード, メール送信, キャッシング, 非同期処理

**Phase 6**: 最終プロジェクト
- タスク管理システムの設計と実装
- エンティティ設計, サービス・API実装
- 高度な機能（統計, ファイル添付, リマインダー）
- テストとデプロイ

### 次のステップ

- 実際のプロジェクトで実践
- Spring CloudやMicroservicesアーキテクチャの学習
- Kotlin + Spring Bootの学習
- リアクティブプログラミング（Spring WebFlux）
- GraphQL APIの実装

---

お疲れさまでした！ 🎉✨

素晴らしいSpring Boot開発者になってください！
