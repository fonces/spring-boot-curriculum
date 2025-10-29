# Step 6: MySQL環境構築

## 🎯 このステップの目標

- DockerでMySQLコンテナを起動する
- Spring BootからMySQLに接続する設定を行う
- データベースとテーブルを作成する
- 接続確認を行う

**所要時間**: 約1時間

---

## 📋 事前準備

- Step 5で作成した`hello-spring-boot`プロジェクト
- Docker Desktopのインストールと起動
- Phase 2事前準備が完了していること

**Phase 2事前準備をまだ完了していない場合**: [Phase 2 事前準備](PREPARE.md)を先に進めてください。

---

## 💡 なぜMySQLを使うのか？

### データベースの必要性

これまでのステップでは、データをメモリ上でのみ扱っていました。
しかし、実際のアプリケーションでは：

- ❌ アプリケーションを再起動するとデータが消える
- ❌ 大量のデータを扱えない
- ❌ 複数のユーザーが同時にアクセスできない

**データベースを使うと**:
- ✅ データを永続的に保存できる
- ✅ 大量のデータを効率的に管理できる
- ✅ トランザクション処理ができる
- ✅ 複数のユーザーが同時にアクセスできる

### なぜDockerを使うのか？

**従来の方法**: MySQLを直接PCにインストール
- 環境を汚す可能性がある
- アンインストールが面倒
- バージョン管理が難しい

**Dockerを使う方法**:
- ✅ 数秒でMySQLを起動できる
- ✅ 使い終わったら簡単に削除できる
- ✅ 他の環境と競合しない
- ✅ 本番環境と同じ構成を再現しやすい

---

## 🚀 ステップ1: MySQLコンテナの起動

### 1-1. docker-compose.ymlの作成

プロジェクトのルートディレクトリ（`pom.xml`と同じ階層）に`docker-compose.yml`を作成します。

**ファイルパス**: `hello-spring-boot/docker-compose.yml`

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: hello-spring-boot-mysql
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: hello_db
      MYSQL_USER: appuser
      MYSQL_PASSWORD: apppassword
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

volumes:
  mysql_data:
```

### 1-2. 設定内容の解説

| 設定項目 | 説明 |
|---------|------|
| `image: mysql:8.0` | MySQL 8.0のDockerイメージを使用 |
| `container_name` | コンテナの名前を指定 |
| `MYSQL_ROOT_PASSWORD` | rootユーザーのパスワード |
| `MYSQL_DATABASE` | 自動作成するデータベース名 |
| `MYSQL_USER` | アプリケーション用ユーザー名 |
| `MYSQL_PASSWORD` | アプリケーション用パスワード |
| `ports` | ホストの3306ポートをコンテナの3306にマッピング |
| `volumes` | データを永続化（コンテナを削除してもデータが残る） |
| `command` | 文字コードをUTF-8に設定 |

### 1-3. MySQLコンテナの起動

ターミナルでプロジェクトのルートディレクトリに移動し、以下のコマンドを実行します。

```bash
cd hello-spring-boot
docker-compose up -d
```

**コマンドの説明**:
- `up`: コンテナを起動
- `-d`: バックグラウンドで実行（デタッチモード）

### 1-4. 起動確認

```bash
docker ps
```

**期待される出力**:
```
CONTAINER ID   IMAGE       COMMAND                  CREATED         STATUS         PORTS                    NAMES
abc123def456   mysql:8.0   "docker-entrypoint.s…"   10 seconds ago  Up 9 seconds   0.0.0.0:3306->3306/tcp   hello-spring-boot-mysql
```

**STATUS** が `Up` になっていればOKです。

---

## 🚀 ステップ2: Spring Bootの依存関係追加

### 2-1. pom.xmlに依存関係を追加

`pom.xml`の`<dependencies>`セクションに以下を追加します。

**ファイルパス**: `hello-spring-boot/pom.xml`

```xml
<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- MySQL Connector -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

**依存関係の説明**:
- `spring-boot-starter-data-jpa`: JPAを使ってデータベース操作を行う
- `mysql-connector-j`: MySQLに接続するためのドライバ

### 2-2. Mavenの依存関係を更新

IntelliJ IDEAで、`pom.xml`を開いた状態で：

1. エディタ右上の **🔄 Load Maven Changes** アイコンをクリック
2. または、`pom.xml`を右クリック → **Maven** → **Reload project**

---

## 🚀 ステップ3: データベース接続設定

### 3-1. application.ymlの編集

`application.yml`にデータベース接続設定を追加します。

**ファイルパス**: `src/main/resources/application.yml`

```yaml
spring:
  application:
    name: hello-spring-boot
  
  # データソース設定
  datasource:
    url: jdbc:mysql://localhost:3306/hello_db?useSSL=false&serverTimezone=Asia/Tokyo
    username: appuser
    password: apppassword
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  # JPA設定
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect

# サーバー設定
server:
  port: 8080

# アプリケーション設定
app:
  name: Hello Spring Boot Application
  version: 1.0.0
```

### 3-2. 設定内容の解説

#### データソース設定（`spring.datasource`）

| 設定項目 | 説明 |
|---------|------|
| `url` | データベース接続URL |
| `username` | データベースユーザー名 |
| `password` | データベースパスワード |
| `driver-class-name` | MySQLドライバのクラス名 |

**URL詳細**:
- `jdbc:mysql://`: MySQLへの接続プロトコル
- `localhost:3306`: 接続先（ホスト:ポート）
- `hello_db`: データベース名
- `useSSL=false`: SSL接続を無効化（開発環境用）
- `serverTimezone=Asia/Tokyo`: タイムゾーンの設定

#### JPA設定（`spring.jpa`）

| 設定項目 | 説明 |
|---------|------|
| `hibernate.ddl-auto: update` | エンティティクラスからテーブルを自動作成・更新 |
| `show-sql: true` | 実行されるSQLをログに出力 |
| `properties.hibernate.format_sql: true` | SQLを読みやすく整形 |
| `properties.hibernate.dialect` | MySQL用の方言（SQL方言）を指定 |

**ddl-autoの選択肢**:
- `none`: 何もしない
- `validate`: スキーマの妥当性を検証
- `update`: スキーマを更新（カラム追加は行うが削除はしない）
- `create`: 起動時にテーブルを削除して再作成
- `create-drop`: 起動時に作成、終了時に削除

> ⚠️ **注意**: 本番環境では`ddl-auto: validate`または`none`を使用し、マイグレーションツール（Flyway, Liquibase）を使うのが一般的です。

---

## 🚀 ステップ4: 接続確認

### 4-1. アプリケーションの起動

Spring Bootアプリケーションを起動します。

**IntelliJ IDEA**:
1. `HelloSpringBootApplication.java`を開く
2. クラス名の横の▶️アイコンをクリック
3. **Run 'HelloSpringBootApplication'** を選択

**または、Mavenコマンド**:
```bash
mvn spring-boot:run
```

### 4-2. ログの確認

起動時のログで以下のような行があればOKです：

```
2024-XX-XX XX:XX:XX.XXX  INFO 12345 --- [main] o.hibernate.dialect.Dialect: HHH000400: Using dialect: org.hibernate.dialect.MySQLDialect
2024-XX-XX XX:XX:XX.XXX  INFO 12345 --- [main] o.h.e.t.j.p.i.JtaPlatformInitiator: HHH000490: Using JtaPlatform implementation: [org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform]
2024-XX-XX XX:XX:XX.XXX  INFO 12345 --- [main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2024-XX-XX XX:XX:XX.XXX  INFO 12345 --- [main] c.e.h.HelloSpringBootApplication: Started HelloSpringBootApplication in X.XXX seconds
```

**エラーが出る場合**:
- MySQLコンテナが起動しているか確認: `docker ps`
- `application.yml`の設定を確認（ユーザー名、パスワード、DB名）
- ポート3306が他のプロセスで使われていないか確認

---

## 🚀 ステップ5: データベースツールで接続確認

### 5-1. DBeaverのインストール

**DBeaver** は、無料で使える高機能なデータベース管理ツールです。
MySQL、PostgreSQL、Oracle、SQL Serverなど、多数のデータベースに対応しています。

**ダウンロード**: [https://dbeaver.io/download/](https://dbeaver.io/download/)

1. OSに合わせたインストーラーをダウンロード
2. インストーラーを実行してインストール
3. DBeaverを起動

### 5-2. DBeaverでMySQLに接続

1. **新しい接続を作成**:
   - メニューバーから **Database** → **New Database Connection** をクリック
   - または、左側のツールバーの **プラグアイコン** をクリック

2. **MySQL を選択**:
   - データベース一覧から **MySQL** を選択
   - **Next** をクリック

3. **接続情報を入力**:
   - **Server Host**: `localhost`
   - **Port**: `3306`
   - **Database**: `hello_db`
   - **Username**: `appuser`
   - **Password**: `apppassword`
   - **Save password locally** にチェック

4. **ドライバーのダウンロード**（初回のみ）:
   - **Download driver files** と表示されたら **Download** をクリック
   - ドライバーのダウンロードが完了するまで待つ

5. **接続テスト**:
   - **Test Connection** をクリック
   - "Connected" と表示されればOK
   - **Finish** をクリック

### 5-3. DBeaverでデータベースを確認

接続が完了すると、左側のナビゲーターに `hello_db` が表示されます。

1. **データベース構造を確認**:
   - `hello_db` → **Schemas** → **hello_db** を展開
   - **Tables** を展開（まだテーブルはありません）

2. **SQLエディタを開く**:
   - `hello_db` を右クリック → **SQL Editor** → **New SQL script**
   - または、ツールバーの **SQL** アイコンをクリック

3. **SQLを実行してみる**:
   ```sql
   -- データベース一覧
   SHOW DATABASES;
   
   -- 現在のデータベース
   SELECT DATABASE();
   
   -- テーブル一覧（まだ空のはず）
   SHOW TABLES;
   ```
   - SQLを入力して、**Ctrl + Enter**（macOSは **Cmd + Enter**）で実行

### 5-4. MySQLクライアントで接続（オプション）

コマンドラインから接続する場合：

```bash
docker exec -it hello-spring-boot-mysql mysql -u appuser -p
```

パスワードを聞かれたら `apppassword` を入力します。

**MySQLプロンプトでの操作**:
```sql
-- データベース一覧
SHOW DATABASES;

-- hello_dbを選択
USE hello_db;

-- テーブル一覧（まだ空のはず）
SHOW TABLES;

-- 終了
EXIT;
```

---

## 🚀 ステップ6: 簡単なテストエンティティで確認

### 6-1. テスト用エンティティクラスの作成

データベース接続が正しく動作するか確認するため、簡単なエンティティクラスを作成します。

**ファイルパス**: `src/main/java/com/example/hellospringboot/entity/TestEntity.java`

```java
package com.example.hellospringboot.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "test_table")
@Data
public class TestEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
}
```

### 6-2. アプリケーションの再起動

Spring Bootアプリケーションを再起動します。

### 6-3. テーブル作成の確認

ログに以下のようなCREATE TABLE文が出力されればOKです：

```sql
Hibernate: 
    create table test_table (
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB
```

### 6-4. DBeaverでテーブルを確認

DBeaverでテーブルが作成されたことを確認します。

**方法1: ナビゲーターから確認**

1. 左側のナビゲーターで `hello_db` → **Schemas** → **hello_db** → **Tables** を右クリック
2. **Refresh** を選択
3. **Tables** を展開すると `test_table` が表示される
4. `test_table` をクリックすると、右側にテーブル構造が表示される

**方法2: SQLで確認**

SQLエディタで以下を実行：

```sql
-- テーブル一覧
SHOW TABLES;
```

**期待される出力**:
```
+--------------------+
| Tables_in_hello_db |
+--------------------+
| test_table         |
+--------------------+
```

**テーブル構造の確認**:

```sql
-- テーブル構造を表示
DESCRIBE test_table;
```

**期待される出力**:
```
+-------+--------------+------+-----+---------+----------------+
| Field | Type         | Null | Key | Default | Extra          |
+-------+--------------+------+-----+---------+----------------+
| id    | bigint       | NO   | PRI | NULL    | auto_increment |
| name  | varchar(255) | NO   |     | NULL    |                |
+-------+--------------+------+-----+---------+----------------+
```

**ERダイアグラムで確認（視覚的）**:

1. `test_table` を右クリック → **View Diagram**
2. テーブル構造が図として表示される
3. カラム名、型、制約が視覚的に確認できる

---

## 🧹 Dockerコンテナの管理

### コンテナの停止

```bash
docker-compose stop
```

### コンテナの起動（停止後）

```bash
docker-compose start
```

### コンテナの停止と削除

```bash
docker-compose down
```

> ⚠️ **注意**: `docker-compose down`を実行してもボリュームは残るため、データは保持されます。

### ボリュームも含めて完全に削除

```bash
docker-compose down -v
```

> ⚠️ **警告**: ボリュームを削除するとデータが消えます。

---

## ✅ 確認チェックリスト

このステップを完了したら、以下を確認してください：

- [ ] MySQLコンテナが起動している（`docker ps`で確認）
- [ ] `pom.xml`にJPAとMySQL Connectorの依存関係を追加した
- [ ] `application.yml`にデータベース接続設定を追加した
- [ ] Spring Bootアプリケーションが正常に起動する
- [ ] ログにHibernateの起動メッセージが表示される
- [ ] テストエンティティからテーブルが自動作成される
- [ ] DBeaverでデータベースに接続できる
- [ ] DBeaverでtest_tableが表示される

---

## 🎉 お疲れ様でした！

MySQL環境の構築が完了しました！

次のステップでは、Spring Data JPAを使って実際にデータの登録・取得を行います。

---

## 📚 次のステップ

- [Step 7: Spring Data JPAでCRUDの基本](STEP_7.md) - エンティティとリポジトリでデータベース操作

---

## 🔍 トラブルシューティング

### MySQLコンテナが起動しない

**ポート3306が既に使われている場合**:

```bash
# ポートを使用しているプロセスを確認
lsof -i :3306  # macOS/Linux
netstat -ano | findstr :3306  # Windows
```

**解決策**:
1. 既存のMySQLを停止する
2. または、`docker-compose.yml`のポートを変更（例: `"3307:3306"`）して`application.yml`のURLも変更

### 接続エラー: Communications link failure

**原因**: MySQLコンテナが完全に起動していない可能性があります。

**解決策**:
```bash
# コンテナのログを確認
docker-compose logs mysql

# 起動完了まで待つ（30秒〜1分程度）
```

### Access denied for user 'appuser'

**原因**: `application.yml`のユーザー名またはパスワードが間違っています。

**解決策**:
- `docker-compose.yml`と`application.yml`の設定を確認
- 設定を修正したら、コンテナを再起動: `docker-compose restart`

### テーブルが作成されない

**原因**: エンティティクラスが認識されていない可能性があります。

**解決策**:
1. エンティティクラスに`@Entity`アノテーションがあるか確認
2. パッケージが`com.example.hellospringboot`またはそのサブパッケージにあるか確認
3. `application.yml`の`ddl-auto`が`update`または`create`になっているか確認

---

## 📖 参考資料

- [Spring Boot - Database Initialization](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization)
- [Spring Data JPA - Reference Documentation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [MySQL Docker Hub](https://hub.docker.com/_/mysql)
- [Hibernate ORM Documentation](https://hibernate.org/orm/documentation/)

---

## 📚 このステップで学んだこと

このステップでは、Spring BootアプリケーションからMySQLデータベースへの接続方法を学びました：

- ✅ DockerでMySQLコンテナを起動する方法
- ✅ Spring Bootの依存関係にMySQL DriverとJPAを追加
- ✅ application.ymlでデータベース接続情報を設定
- ✅ spring.jpa.hibernate.ddl-autoでテーブル自動生成
- ✅ エンティティクラスの作成とテーブルマッピング
- ✅ データベースツール（IntelliJ Database Tool）での接続確認
- ✅ Dockerコンテナの基本操作（起動・停止・削除）

**重要ポイント**:
- `spring.jpa.hibernate.ddl-auto=update`: 開発環境用。本番環境では`none`または`validate`を使用
- パスワードなどの機密情報は環境変数や外部設定ファイルで管理する
- Dockerコンテナは`docker-compose`で管理すると便利

---

## 🔄 Gitへのコミットとレビュー依頼

進捗を記録してレビューを受けましょう：

```bash
git add .
git commit -m "Step 6: MySQLデータベース接続完了

- DockerでMySQLコンテナをセットアップ
- Spring BootアプリケーションからMySQLへ接続
- テストエンティティで接続確認
"
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

MySQL接続ができたので、次は [Step 7: JPAでCRUD操作](STEP_7.md) で実際にデータの登録・取得・更新・削除を学びましょう！

JPAのRepositoryパターンを使って、シンプルにデータベース操作を行います。

---

