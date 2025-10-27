# Step 12: MySQLへの切り替え

## 🎯 このステップの目標

- H2からMySQLに切り替える方法を理解する
- Docker ComposeでMySQLを起動する
- Spring Bootのプロファイル機能を使って環境を切り替える
- 開発環境（H2）と本番環境（MySQL）を使い分ける

**所要時間**: 約1時間30分

---

## 📋 事前準備

- Step 11までのリレーションシップが理解できていること
- [PREPARE.md](PREPARE.md)のDocker環境が整っていること

**Docker Desktopがインストールされていない場合**: [PREPARE.md](PREPARE.md)を参照してインストールしてください。

---

## 💡 なぜMySQLに切り替えるのか？

### H2データベースの特徴

**メリット**:
- ✅ 設定不要で簡単
- ✅ 開発が高速
- ✅ 軽量

**デメリット**:
- ❌ インメモリなのでアプリ再起動でデータが消える
- ❌ 本番環境では使えない
- ❌ 複数人での開発に不向き

### MySQLの特徴

**メリット**:
- ✅ データが永続化される
- ✅ 本番環境で使われている
- ✅ 複数人でのデータ共有が可能
- ✅ パフォーマンスが高い

**デメリット**:
- ❌ セットアップが必要
- ❌ サーバーを起動する必要がある

### ベストプラクティス

開発環境と本番環境で異なるデータベースを使い分ける：

| 環境 | データベース | 理由 |
|------|--------------|------|
| **開発（ローカル）** | H2 | 高速、簡単 |
| **ステージング** | MySQL（Docker） | 本番に近い環境 |
| **本番（プロダクション）** | MySQL（クラウド） | 信頼性、パフォーマンス |

**このステップでは開発環境でMySQLを使えるようにします。**

---

## 🚀 ステップ1: Docker ComposeでMySQLを起動

### 1-1. docker-compose.ymlの作成

プロジェクトルートに`docker-compose.yml`を作成します。

**ファイルパス**: `docker-compose.yml`（プロジェクトルート）

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: springboot-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: hellospringboot
      MYSQL_USER: springuser
      MYSQL_PASSWORD: springpass
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

volumes:
  mysql_data:
```

### 1-2. docker-compose.ymlの解説

#### `image: mysql:8.0`
- MySQL 8.0の公式Dockerイメージを使用

#### `environment`
- `MYSQL_ROOT_PASSWORD`: rootユーザーのパスワード
- `MYSQL_DATABASE`: 自動作成されるデータベース名
- `MYSQL_USER`: アプリケーション用ユーザー
- `MYSQL_PASSWORD`: アプリケーション用パスワード

#### `ports`
- `3306:3306`: ホスト側の3306ポートをコンテナ側の3306にマッピング

#### `volumes`
- `mysql_data:/var/lib/mysql`: データを永続化（コンテナ削除後もデータが残る）

#### `command`
- `--character-set-server=utf8mb4`: 文字コードをUTF-8に設定
- `--collation-server=utf8mb4_unicode_ci`: 照合順序を設定

### 1-3. MySQLコンテナの起動

```bash
docker-compose up -d
```

**オプション**:
- `-d`: バックグラウンドで起動（デタッチモード）

**出力例**:
```
Creating network "hellospringboot_default" with the default driver
Creating volume "hellospringboot_mysql_data" with default driver
Creating springboot-mysql ... done
```

### 1-4. 起動確認

```bash
docker-compose ps
```

**期待される出力**:
```
      Name                    Command             State           Ports
---------------------------------------------------------------------------------
springboot-mysql   docker-entrypoint.sh mysqld   Up      0.0.0.0:3306->3306/tcp
```

**ログ確認**:
```bash
docker-compose logs mysql
```

MySQLが起動したことを示すログが表示されます：
```
mysqld: ready for connections. Version: '8.0.35'  socket: '/var/run/mysqld/mysqld.sock'
```

---

## 🚀 ステップ2: MySQL接続の設定

### 2-1. pom.xmlにMySQL依存関係を追加

`pom.xml`に以下を追加します。

**ファイルパス**: `pom.xml`

```xml
<dependencies>
    <!-- 既存の依存関係 -->
    
    <!-- H2 Database (開発用) -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- MySQL Database (本番用) -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

**IntelliJ IDEAで依存関係を更新**:
1. 右サイドバーの「Maven」タブを開く
2. 🔄（Reload All Maven Projects）をクリック

### 2-2. プロファイル別の設定ファイル

Spring Bootはプロファイル機能で環境ごとに設定を切り替えられます。

**ファイル構成**:
```
src/main/resources/
├── application.yml          # 共通設定
├── application-dev.yml      # 開発環境（H2）
└── application-prod.yml     # 本番環境（MySQL）
```

### 2-3. application.ymlの更新

**ファイルパス**: `src/main/resources/application.yml`

```yaml
spring:
  application:
    name: hello-spring-boot

  # デフォルトプロファイル
  profiles:
    active: dev

  # JPA共通設定
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
    hibernate:
      ddl-auto: update

# ログレベル
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### 2-4. application-dev.ymlの作成

**ファイルパス**: `src/main/resources/application-dev.yml`

```yaml
# 開発環境設定（H2）

spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:

  h2:
    console:
      enabled: true
      path: /h2-console

  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
```

### 2-5. application-prod.ymlの作成

**ファイルパス**: `src/main/resources/application-prod.yml`

```yaml
# 本番環境設定（MySQL）

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/hellospringboot?useSSL=false&serverTimezone=Asia/Tokyo
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: springuser
    password: springpass

  jpa:
    database-platform: org.hibernate.dialect.MySQLDialect
    hibernate:
      ddl-auto: update
```

### 2-6. 設定の解説

#### `spring.profiles.active`
デフォルトで使うプロファイルを指定
```yaml
spring:
  profiles:
    active: dev  # devプロファイルを使用
```

#### `spring.datasource.url`
データベース接続URL

**H2**:
```yaml
url: jdbc:h2:mem:testdb
```

**MySQL**:
```yaml
url: jdbc:mysql://localhost:3306/hellospringboot?useSSL=false&serverTimezone=Asia/Tokyo
```
- `localhost:3306`: MySQLサーバーのアドレスとポート
- `hellospringboot`: データベース名
- `useSSL=false`: SSL接続を無効化（開発環境）
- `serverTimezone=Asia/Tokyo`: タイムゾーン設定

#### `spring.jpa.database-platform`
使用するSQLダイアレクト（方言）

**H2**:
```yaml
database-platform: org.hibernate.dialect.H2Dialect
```

**MySQL**:
```yaml
database-platform: org.hibernate.dialect.MySQLDialect
```

#### `spring.jpa.hibernate.ddl-auto`
スキーマの自動生成設定

| 値 | 説明 |
|----|------|
| `none` | 何もしない |
| `validate` | スキーマを検証するのみ |
| `update` | スキーマを更新（推奨：開発環境） |
| `create` | 起動時にスキーマを作成（既存データ削除） |
| `create-drop` | 起動時に作成、終了時に削除 |

---

## ✅ ステップ3: MySQLで動作確認

### 3-1. プロファイルの切り替え

#### 方法1: application.ymlで指定

```yaml
spring:
  profiles:
    active: prod  # prodプロファイルを使用
```

#### 方法2: 環境変数で指定

```bash
export SPRING_PROFILES_ACTIVE=prod
```

#### 方法3: 起動時に指定

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

#### 方法4: IntelliJ IDEAで指定

1. 右上の実行構成（Run Configuration）をクリック
2. 「Edit Configurations...」を選択
3. 「Active profiles」に`prod`を入力
4. 「OK」をクリック
5. アプリケーションを起動

### 3-2. アプリケーション起動（MySQLモード）

**IntelliJ IDEAで**:
1. Active profilesを`prod`に設定
2. ▶️（Run）をクリック

**コマンドラインで**:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

**起動ログで確認**:
```
HikariPool-1 - Starting...
HikariPool-1 - Added connection com.mysql.cj.jdbc.ConnectionImpl@...
HikariPool-1 - Start completed.
Hibernate: create table users (...)
Hibernate: create table posts (...)
```

### 3-3. データ投入

**ユーザー作成**:
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Taro Yamada",
    "email": "taro@example.com",
    "age": 30
  }'
```

**投稿作成**:
```bash
curl -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "title": "First Post on MySQL",
    "content": "Now using MySQL database!"
  }'
```

**データ確認**:
```bash
curl http://localhost:8080/api/users
curl http://localhost:8080/api/posts
```

### 3-4. MySQLコンテナに直接接続して確認

```bash
docker exec -it springboot-mysql mysql -u springuser -p
# パスワード: springpass
```

**SQL実行**:
```sql
USE hellospringboot;

SHOW TABLES;

SELECT * FROM users;
SELECT * FROM posts;

-- JOINでデータ確認
SELECT p.id, p.title, u.name as user_name 
FROM posts p 
JOIN users u ON p.user_id = u.id;

EXIT;
```

---

## 🚀 ステップ4: データ永続化の確認

### 4-1. アプリケーションの再起動

1. アプリケーションを停止（Ctrl+C）
2. 再度起動

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

### 4-2. データ確認

```bash
curl http://localhost:8080/api/users
curl http://localhost:8080/api/posts
```

**結果**: データが残っている！（MySQLはデータを永続化）

### 4-3. H2との比較

**H2の場合（devプロファイル）**:
```bash
# H2で起動
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# データ投入
curl -X POST http://localhost:8080/api/users ...

# アプリ再起動
# データが消える！
```

---

## 🚀 ステップ5: Docker Composeの管理

### 5-1. 基本コマンド

#### 起動
```bash
docker-compose up -d
```

#### 停止
```bash
docker-compose stop
```

#### 停止して削除
```bash
docker-compose down
```

#### ログ確認
```bash
docker-compose logs mysql
docker-compose logs -f mysql  # リアルタイム表示
```

#### コンテナの状態確認
```bash
docker-compose ps
```

### 5-2. データの削除

**警告**: ボリュームを削除するとすべてのデータが消えます！

```bash
docker-compose down -v
```

オプション:
- `-v`: ボリュームも削除

### 5-3. 開発時の推奨ワークフロー

```bash
# 朝（作業開始）
docker-compose up -d

# 開発作業
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod

# 夜（作業終了）
docker-compose stop
```

**PCをシャットダウンする場合**:
```bash
docker-compose down  # コンテナ削除（ボリュームは保持）
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: 環境変数での設定

`application-prod.yml`を環境変数で上書きできるようにしてください。

**ヒント**:
```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:mysql://localhost:3306/hellospringboot}
    username: ${DATABASE_USERNAME:springuser}
    password: ${DATABASE_PASSWORD:springpass}
```

### チャレンジ 2: テスト用プロファイル

テスト用のプロファイル（`application-test.yml`）を作成してください。

**要件**:
- H2を使用
- テストごとにデータベースをクリーンにする（`ddl-auto: create-drop`）

### チャレンジ 3: PostgreSQLへの対応

MySQLの代わりにPostgreSQLを使えるようにしてください。

**ヒント**:
```yaml
# docker-compose.yml
postgres:
  image: postgres:15
  environment:
    POSTGRES_DB: hellospringboot
    POSTGRES_USER: springuser
    POSTGRES_PASSWORD: springpass
```

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

## 🐛 トラブルシューティング

### "Communications link failure"

**エラー**:
```
com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure
```

**原因**: MySQLコンテナが起動していない

**解決策**:
```bash
# コンテナ確認
docker-compose ps

# 起動
docker-compose up -d

# ログ確認
docker-compose logs mysql
```

### "Access denied for user"

**エラー**:
```
Access denied for user 'springuser'@'localhost' (using password: YES)
```

**原因**: ユーザー名またはパスワードが間違っている

**解決策**: `application-prod.yml`と`docker-compose.yml`の設定を確認

```yaml
# application-prod.yml
username: springuser
password: springpass

# docker-compose.yml
MYSQL_USER: springuser
MYSQL_PASSWORD: springpass
```

### "Table doesn't exist"

**エラー**:
```
Table 'hellospringboot.users' doesn't exist
```

**原因**: テーブルが自動作成されていない

**解決策1**: `ddl-auto`を確認

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # createまたはupdate
```

**解決策2**: 手動でテーブル作成

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    age INT
);
```

### ポート3306が使用中

**エラー**:
```
Bind for 0.0.0.0:3306 failed: port is already allocated
```

**原因**: 他のMySQLがポート3306を使用している

**解決策1**: 別のポートを使用

```yaml
# docker-compose.yml
ports:
  - "3307:3306"  # ホスト側を3307に変更

# application-prod.yml
url: jdbc:mysql://localhost:3307/hellospringboot...
```

**解決策2**: 既存のMySQLを停止

```bash
# macOS/Linux
sudo service mysql stop

# Windows
net stop MySQL
```

---

## 📚 このステップで学んだこと

- ✅ Docker ComposeでMySQLを起動する方法
- ✅ Spring Bootのプロファイル機能
- ✅ 環境別の設定ファイル（dev/prod）
- ✅ H2とMySQLの切り替え
- ✅ データの永続化
- ✅ Docker Composeの基本コマンド
- ✅ 開発ワークフローのベストプラクティス

---

## 💡 補足: 本番環境での注意事項

### ddl-autoの設定

**開発環境**:
```yaml
ddl-auto: update  # スキーマを自動更新（便利）
```

**本番環境**:
```yaml
ddl-auto: validate  # スキーマを検証のみ（安全）
```

**推奨しない**:
```yaml
ddl-auto: create  # 起動時にデータが全削除される！
```

### パスワード管理

**開発環境**:
```yaml
# ファイルに直接記述してもOK
password: springpass
```

**本番環境**:
```yaml
# 環境変数を使用
password: ${DATABASE_PASSWORD}
```

### SSLの使用

**開発環境**:
```yaml
url: jdbc:mysql://localhost:3306/hellospringboot?useSSL=false
```

**本番環境**:
```yaml
url: jdbc:mysql://db.example.com:3306/hellospringboot?useSSL=true&requireSSL=true
```

### 接続プールの設定

**本番環境での推奨設定**:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 20000
```

---

## 🔄 Gitへのコミットとレビュー依頼

Phase 2の完成です！進捗を記録してレビューを受けましょう：

```bash
git add .
git commit -m "Phase 2完了: MySQL切り替え実装（Docker Compose、プロファイル、環境別設定）"
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## 🎓 Phase 2の総まとめ

### Phase 2で学んだこと

#### STEP 6: H2データベース
- ✅ インメモリデータベースの基本
- ✅ H2 Consoleの使い方
- ✅ 基本的なSQL操作

#### STEP 7: Spring Data JPA
- ✅ @Entityアノテーション
- ✅ JpaRepositoryの基本
- ✅ CRUD操作（Create、Read）

#### STEP 8: CRUD完成
- ✅ Update、Delete操作
- ✅ Optional<T>でのnull安全
- ✅ RESTful APIの設計

#### STEP 9: トランザクション
- ✅ @Transactionalアノテーション
- ✅ ACID特性
- ✅ トランザクション伝播

#### STEP 10: カスタムクエリ
- ✅ クエリメソッド
- ✅ JPQL
- ✅ ネイティブSQL
- ✅ Specificationによる動的クエリ

#### STEP 11: リレーションシップ
- ✅ @OneToMany/@ManyToOne
- ✅ Cascade設定
- ✅ Fetch戦略（LAZY/EAGER）
- ✅ N+1問題の解決

#### STEP 12: MySQL移行
- ✅ Docker Compose
- ✅ プロファイル機能
- ✅ データ永続化
- ✅ 環境別設定

### 次のフェーズ: Phase 3

Phase 3では以下を学びます：
- レイヤードアーキテクチャ
- DTOパターン
- バリデーション
- 例外ハンドリング
- ロギング
- ベストプラクティス

---

## ➡️ 次のステップ

レビューが完了したら、**Phase 3: アーキテクチャとベストプラクティス**へ進みましょう！

より実践的なアプリケーション設計を学びます。

---

お疲れさまでした！ 🎉

Phase 2を完了し、データベース統合の基礎を習得しました！
H2での開発、Spring Data JPAによるCRUD操作、リレーションシップ、そしてMySQLへの移行まで、
実務で必須のスキルを身につけました。自信を持って次のフェーズに進みましょう！
