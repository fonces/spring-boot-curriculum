# Step 6: MySQL環境構築# Step 6: H2データベース導入



## 🎯 このステップの目標## 🎯 このステップの目標



- Docker Composeを使ってMySQL環境を構築する- H2データベースとは何かを理解する

- Spring BootからMySQLに接続する- Spring BootにH2依存関係を追加する

- データベースの基本的な操作を理解する- H2 Consoleを有効化してブラウザでデータベースを確認する

- application.ymlでデータベース接続設定を管理する- データベースの基本的な操作を理解する



**所要時間**: 約40分**所要時間**: 約30分



------



## 📋 事前準備## 📋 事前準備



- Phase 1で作成した`hello-spring-boot`プロジェクト- Phase 1で作成した`hello-spring-boot`プロジェクト

- Phase 1 (Step 1〜5) の完了- Phase 1 (Step 1〜5) の完了

- **Docker Desktopのインストール**: [Docker Desktop](https://www.docker.com/products/docker-desktop/)

**Phase 1をまだ完了していない場合**: [Phase 1](../phase1/STEP_1.md)を先に進めてください。

**Phase 1をまだ完了していない場合**: [Phase 1](../phase1/STEP_1.md)を先に進めてください。

---

---

## 💡 H2データベースとは？

## 💡 なぜMySQLから始めるのか？

### H2の特徴

### MySQL の特徴

**H2 Database** は、Javaで書かれた軽量なリレーショナルデータベースです。

**MySQL** は、世界で最も人気のあるオープンソースのリレーショナルデータベースです。

**特徴**:

**特徴**:- ✅ **インメモリモード**: データをメモリ上に保存（高速）

- ✅ **本番環境で広く使用**: 実務で最も使われるDB- ✅ **ファイルモード**: データをファイルに保存（永続化）

- ✅ **豊富なドキュメント**: 情報が多く学習しやすい- ✅ **組み込み可能**: アプリケーションに組み込んで使用

- ✅ **高いパフォーマンス**: 大規模なデータにも対応- ✅ **開発に最適**: セットアップ不要、すぐ使える

- ✅ **Docker対応**: 開発環境の構築が簡単- ✅ **ブラウザUI**: H2 Consoleで簡単にデータ確認



### H2ではなくMySQLで学ぶ理由### なぜH2から始めるのか？



| 比較項目 | MySQL | H2 || 比較項目 | H2 | MySQL/PostgreSQL |

|---------|-------|----||---------|----|--------------------|

| 本番利用 | ✅ 推奨 | ❌ 非推奨 || セットアップ | 依存関係を追加するだけ | インストールまたはDocker必要 |

| 実務スキル | 直接役立つ | 学習専用 || 起動速度 | 即座 | 数秒〜数十秒 |

| SQL方言 | 本物のMySQL SQL | 標準SQL寄り || 学習コスト | 低い | やや高い |

| データ永続化 | ✅ 標準 | 設定が必要 || 本番利用 | ❌ 推奨しない | ✅ 推奨 |



**Phase 2の学習戦略**:**Phase 2の学習戦略**:

1. **Step 6**: MySQLのDocker環境構築1. **Step 6〜11**: H2で基本を学ぶ

2. **Step 7〜11**: Spring Data JPAでCRUD操作2. **Step 12**: MySQLに切り替える

3. **Step 12〜13**: MyBatisで直接SQL操作

4. **Step 14**: JPAとMyBatisの使い分け---



---## 🚀 ステップ1: H2依存関係の追加



## 🚀 ステップ1: Docker Composeファイルの作成### 1-1. pom.xmlを編集



### 1-1. プロジェクトルートにdocker-compose.ymlを作成プロジェクトのルートにある`pom.xml`を開きます。



**ファイルパス**: `docker-compose.yml`**ファイルパス**: `pom.xml`



```yaml`<dependencies>`セクション内に以下を追加：

version: '3.8'

```xml

services:<dependencies>

  mysql:    <!-- 既存の依存関係 -->

    image: mysql:8.0    <dependency>

    container_name: spring-boot-mysql        <groupId>org.springframework.boot</groupId>

    environment:        <artifactId>spring-boot-starter-web</artifactId>

      MYSQL_ROOT_PASSWORD: rootpassword    </dependency>

      MYSQL_DATABASE: hellospringboot

      MYSQL_USER: dbuser    <dependency>

      MYSQL_PASSWORD: dbpassword        <groupId>org.projectlombok</groupId>

    ports:        <artifactId>lombok</artifactId>

      - "3306:3306"        <optional>true</optional>

    volumes:    </dependency>

      - mysql-data:/var/lib/mysql

    command: --default-authentication-plugin=mysql_native_password    <!-- H2 Databaseを追加 -->

    <dependency>

volumes:        <groupId>com.h2database</groupId>

  mysql-data:        <artifactId>h2</artifactId>

```        <scope>runtime</scope>

    </dependency>

### 1-2. 設定の説明

    <!-- Spring Data JPAを追加 -->

| 項目 | 説明 |    <dependency>

|------|------|        <groupId>org.springframework.boot</groupId>

| `image: mysql:8.0` | MySQL 8.0の公式イメージを使用 |        <artifactId>spring-boot-starter-data-jpa</artifactId>

| `MYSQL_ROOT_PASSWORD` | rootユーザーのパスワード |    </dependency>

| `MYSQL_DATABASE` | 初期作成されるデータベース名 |

| `MYSQL_USER` | アプリケーション用のユーザー名 |    <!-- テスト用（既存） -->

| `MYSQL_PASSWORD` | アプリケーション用のパスワード |    <dependency>

| `ports: 3306:3306` | ホストの3306ポートをコンテナの3306に接続 |        <groupId>org.springframework.boot</groupId>

| `volumes` | データを永続化（コンテナ削除後もデータが残る） |        <artifactId>spring-boot-starter-test</artifactId>

        <scope>test</scope>

---    </dependency>

</dependencies>

## 🚀 ステップ2: MySQLコンテナの起動```



### 2-1. Docker Composeでコンテナを起動### 1-2. 依存関係の解説



```bash#### H2 Database

docker-compose up -d

``````xml

<dependency>

**出力例**:    <groupId>com.h2database</groupId>

```    <artifactId>h2</artifactId>

[+] Running 2/2    <scope>runtime</scope>

 ✔ Network hello-spring-boot_default       Created</dependency>

 ✔ Container spring-boot-mysql             Started```

```

- **`<scope>runtime</scope>`**: 実行時のみ必要（コンパイル時は不要）

### 2-2. コンテナの状態確認- H2データベースのJDBCドライバを提供



```bash#### Spring Data JPA

docker-compose ps

``````xml

<dependency>

**出力例**:    <groupId>org.springframework.boot</groupId>

```    <artifactId>spring-boot-starter-data-jpa</artifactId>

NAME                 IMAGE       STATUS       PORTS</dependency>

spring-boot-mysql    mysql:8.0   Up 10 sec    0.0.0.0:3306->3306/tcp```

```

- **JPA (Java Persistence API)**: Javaでデータベース操作を行う標準仕様

### 2-3. MySQLログの確認- **Hibernate**: JPAの実装（自動的に含まれる）

- **Spring Data JPA**: JPAをさらに使いやすくするSpringのライブラリ

```bash

docker-compose logs mysql### 1-3. Mavenプロジェクトの更新

```

IntelliJ IDEAで：

`ready for connections` というメッセージが表示されればOKです。1. `pom.xml`を保存

2. 右上の「Load Maven Changes」（Mアイコン）をクリック

---3. 依存関係がダウンロードされるまで待つ



## 🚀 ステップ3: Spring BootにMySQL依存関係を追加---



### 3-1. pom.xmlを編集## 🚀 ステップ2: H2 Consoleの有効化



**ファイルパス**: `pom.xml`### 2-1. application.ymlの設定



```xml`src/main/resources/application.yml`を開いて、以下を追加：

<dependencies>

    <!-- 既存の依存関係 -->**ファイルパス**: `src/main/resources/application.yml`

    <dependency>

        <groupId>org.springframework.boot</groupId>```yaml

        <artifactId>spring-boot-starter-web</artifactId>server:

    </dependency>  port: 8080

    <dependency>

        <groupId>org.projectlombok</groupId>spring:

        <artifactId>lombok</artifactId>  # H2 Console設定

        <optional>true</optional>  h2:

    </dependency>    console:

      enabled: true  # H2 Consoleを有効化

    <!-- Spring Data JPA -->      path: /h2-console  # アクセスパス

    <dependency>

        <groupId>org.springframework.boot</groupId>  # データソース設定

        <artifactId>spring-boot-starter-data-jpa</artifactId>  datasource:

    </dependency>    url: jdbc:h2:mem:testdb  # インメモリDB、名前は"testdb"

    driverClassName: org.h2.Driver

    <!-- MySQL Driver -->    username: sa  # デフォルトユーザー名

    <dependency>    password:     # パスワードなし

        <groupId>com.mysql</groupId>

        <artifactId>mysql-connector-j</artifactId>  # JPA設定

        <scope>runtime</scope>  jpa:

    </dependency>    database-platform: org.hibernate.dialect.H2Dialect

    hibernate:

    <!-- Test -->      ddl-auto: update  # テーブルを自動作成/更新

    <dependency>    show-sql: true  # 実行されるSQLをコンソールに表示

        <groupId>org.springframework.boot</groupId>    properties:

        <artifactId>spring-boot-starter-test</artifactId>      hibernate:

        <scope>test</scope>        format_sql: true  # SQLを整形して表示

    </dependency>

</dependencies># アプリケーション情報（Phase 1から継続）

```app:

  name: Hello Spring Boot Application

### 3-2. Mavenの依存関係を更新  version: 1.0.0

  description: Spring Bootを学ぶためのサンプルアプリケーション

IDEでMavenを再読み込みするか、以下を実行：```



```bash### 2-2. 設定の解説

./mvnw clean install

```#### H2 Console設定



---```yaml

spring:

## 🚀 ステップ4: データベース接続設定  h2:

    console:

### 4-1. application.ymlにMySQL接続情報を追加      enabled: true

      path: /h2-console

**ファイルパス**: `src/main/resources/application.yml````



```yaml- `enabled: true`: H2の管理画面を有効化

spring:- `path: /h2-console`: ブラウザでアクセスするパス

  application:

    name: hello-spring-boot#### データソース設定

  

  datasource:```yaml

    url: jdbc:mysql://localhost:3306/hellospringboot?useSSL=false&serverTimezone=Asia/Tokyo&allowPublicKeyRetrieval=truedatasource:

    username: dbuser  url: jdbc:h2:mem:testdb

    password: dbpassword  driverClassName: org.h2.Driver

    driver-class-name: com.mysql.cj.jdbc.Driver  username: sa

    password:

  jpa:```

    hibernate:

      ddl-auto: update- `jdbc:h2:mem:testdb`: インメモリDBを使用、DB名は`testdb`

    show-sql: true- `username: sa`: デフォルトユーザー（System Administrator）

    properties:- `password:` (空): パスワードなし

      hibernate:

        format_sql: true#### JPA設定

        dialect: org.hibernate.dialect.MySQLDialect

```yaml

server:jpa:

  port: 8080  hibernate:

    ddl-auto: update

logging:  show-sql: true

  level:```

    org.hibernate.SQL: DEBUG

    org.hibernate.type.descriptor.sql.BasicBinder: TRACE- `ddl-auto: update`: エンティティクラスからテーブルを自動生成

```  - `create`: 起動時に毎回テーブルを作り直す

  - `update`: テーブルがなければ作成、あれば更新

### 4-2. 設定の説明  - `none`: 何もしない（本番環境推奨）

- `show-sql: true`: SQLログを出力（学習用）

#### DataSource設定

---

| 項目 | 説明 |

|------|------|## 🚀 ステップ3: アプリケーションの起動と確認

| `url` | MySQL接続URL |

| `username` | データベースユーザー名 |### 3-1. アプリケーション起動

| `password` | データベースパスワード |

| `driver-class-name` | MySQLドライバークラス |`HelloSpringBootApplication.java`を実行して、アプリケーションを起動します。



#### JPA/Hibernate設定コンソールに以下のようなログが表示されることを確認：



| 項目 | 説明 |```

|------|------|Hibernate: 

| `ddl-auto: update` | エンティティからテーブルを自動生成・更新 |    

| `show-sql: true` | 実行されるSQLをログ出力 |    drop table if exists users CASCADE 

| `format_sql: true` | SQLを整形して表示 |Hibernate: 

| `dialect` | MySQL用のSQL方言 |    

    create table users (

**ddl-autoの値**:       ...

- `create`: 起動時にテーブルを削除して再作成```

- `create-drop`: 起動時に作成、終了時に削除

- `update`: テーブルがなければ作成、あれば更新（本番非推奨）（まだエンティティを作成していないので、テーブルは作成されませんが、H2は起動しています）

- `validate`: スキーマ検証のみ

- `none`: 何もしない（本番推奨）### 3-2. H2 Consoleにアクセス



---ブラウザで以下のURLにアクセス：



## 🚀 ステップ5: 簡単なエンティティで接続確認```

http://localhost:8080/h2-console

### 5-1. Userエンティティの作成```



**ファイルパス**: `src/main/java/com/example/hellospringboot/entity/User.java`**H2ログイン画面**が表示されます。



```java### 3-3. H2 Consoleでログイン

package com.example.hellospringboot.entity;

ログイン画面で以下を入力：

import jakarta.persistence.*;

import lombok.*;- **Saved Settings**: Generic H2 (Embedded)

- **Setting Name**: Generic H2 (Embedded)

@Entity- **Driver Class**: `org.h2.Driver`（自動入力済み）

@Table(name = "users")- **JDBC URL**: `jdbc:h2:mem:testdb`

@Getter- **User Name**: `sa`

@Setter- **Password**: （空欄のまま）

@NoArgsConstructor

@AllArgsConstructor「Connect」ボタンをクリックします。

@Builder

public class User {### 3-4. H2 Consoleの画面確認



    @Idログインに成功すると、以下の画面が表示されます：

    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;- **左側**: テーブル一覧（現在は空）

- **右上**: SQLクエリ入力欄

    @Column(nullable = false, length = 50)- **右下**: 実行結果表示エリア

    private String name;

試しにSQLを実行してみましょう：

    @Column(nullable = false, unique = true, length = 100)

    private String email;```sql

}SELECT 1;

``````



### 5-2. アプリケーションの起動「Run」ボタン（または Ctrl+Enter）をクリック。



```bash**結果**:

./mvnw spring-boot:run```

```1

-

**ログ確認ポイント**:1

``````

Hibernate: create table users (...)

```が表示されればOKです！



このログが表示されれば、テーブルが自動生成されています。---



---## 🚀 ステップ4: 簡単なテーブルを作成してみる



## 🚀 ステップ6: MySQLクライアントでデータベース確認### 4-1. SQLで直接テーブルを作成



### 6-1. MySQL CLIでの確認（方法1）H2 Consoleで以下のSQLを実行：



```bash```sql

docker exec -it spring-boot-mysql mysql -udbuser -pdbpassword hellospringbootCREATE TABLE users (

```    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    name VARCHAR(100) NOT NULL,

MySQLプロンプトが表示されたら：    email VARCHAR(100) NOT NULL

);

```sql```

SHOW TABLES;

```「Run」をクリック。



**出力例**:**結果**: 左側のテーブル一覧に「USERS」が表示されます。

```

+---------------------------+### 4-2. データを挿入

| Tables_in_hellospringboot |

+---------------------------+```sql

| users                     |INSERT INTO users (name, email) VALUES ('Taro', 'taro@example.com');

+---------------------------+INSERT INTO users (name, email) VALUES ('Hanako', 'hanako@example.com');

```INSERT INTO users (name, email) VALUES ('Jiro', 'jiro@example.com');

```

テーブル構造を確認：

### 4-3. データを確認

```sql

DESC users;```sql

```SELECT * FROM users;

```

**出力例**:

```**結果**:

+-------+--------------+------+-----+---------+----------------+```

| Field | Type         | Null | Key | Default | Extra          |ID  NAME     EMAIL

+-------+--------------+------+-----+---------+----------------+1   Taro     taro@example.com

| id    | bigint       | NO   | PRI | NULL    | auto_increment |2   Hanako   hanako@example.com

| name  | varchar(50)  | NO   |     | NULL    |                |3   Jiro     jiro@example.com

| email | varchar(100) | NO   | UNI | NULL    |                |```

+-------+--------------+------+-----+---------+----------------+

```おめでとうございます！データベースの基本操作ができました！



終了：---

```sql

EXIT;## 💡 インメモリDBの特性を理解する

```

### 重要な注意点

### 6-2. GUI ツールでの確認（方法2）

**インメモリDB（`jdbc:h2:mem:testdb`）の特徴**:

**推奨ツール**:

- **MySQL Workbench**: [ダウンロード](https://www.mysql.com/products/workbench/)- ✅ 高速

- **DBeaver**: [ダウンロード](https://dbeaver.io/)- ✅ セットアップ不要

- **TablePlus**: [ダウンロード](https://tableplus.com/)- ❌ **アプリケーションを停止するとデータが消える**



**接続情報**:### 実験: データの永続性を確認

- Host: `localhost`

- Port: `3306`1. アプリケーションを**停止**する

- Database: `hellospringboot`2. 再度**起動**する

- Username: `dbuser`3. H2 Consoleにアクセスして`SELECT * FROM users;`を実行

- Password: `dbpassword`

**結果**: テーブルもデータも消えています。

---

**これはバグではなく、インメモリDBの仕様です。**

## ✅ 動作確認

### ファイルモードに変更（オプション）

### 確認項目

データを永続化したい場合は、`application.yml`を以下のように変更：

- [ ] Docker Composeでコンテナが起動している

- [ ] Spring Bootアプリケーションが起動できる```yaml

- [ ] ログに`create table users`が表示されるspring:

- [ ] MySQLで`users`テーブルが確認できる  datasource:

    # インメモリモード（データは揮発）

### トラブルシューティング    # url: jdbc:h2:mem:testdb

    

#### 問題1: `Port 3306 is already in use`    # ファイルモード（データを永続化）

    url: jdbc:h2:file:./data/testdb

**原因**: 既にMySQLがローカルで起動している```



**解決策**:`./data/testdb.mv.db`というファイルにデータが保存されます。

```bash

# ローカルのMySQLを停止**Phase 2ではインメモリモードを使用します**（学習目的のため）。

sudo service mysql stop  # Linux

brew services stop mysql # macOS---



# または、docker-compose.ymlのポートを変更## 🎨 チャレンジ課題

ports:

  - "3307:3306"  # ホスト側を3307に変更### チャレンジ 1: 別のテーブルを作成



# application.ymlのURLも修正`products`テーブルを作成して、データを挿入・取得してください。

url: jdbc:mysql://localhost:3307/hellospringboot...

```**テーブル定義**:

- `id` (BIGINT, PRIMARY KEY, AUTO_INCREMENT)

#### 問題2: `Access denied for user 'dbuser'@'localhost'`- `name` (VARCHAR(100))

- `price` (INT)

**原因**: パスワードの不一致- `category` (VARCHAR(50))



**解決策**:**ヒント**:

1. `docker-compose.yml`のパスワードを確認```sql

2. `application.yml`のパスワードが一致しているか確認CREATE TABLE products (

3. コンテナを再作成    -- ここに定義

```bash);

docker-compose down -v

docker-compose up -dINSERT INTO products (name, price, category) VALUES (...);

```

SELECT * FROM products;

#### 問題3: `Unknown database 'hellospringboot'````



**原因**: データベースが作成されていない### チャレンジ 2: SQLの練習



**解決策**:以下のSQLを実行してみてください：

```bash

# コンテナに入ってデータベースを手動作成```sql

docker exec -it spring-boot-mysql mysql -uroot -prootpassword-- 特定のユーザーを検索

SELECT * FROM users WHERE name = 'Taro';

CREATE DATABASE hellospringboot;

GRANT ALL PRIVILEGES ON hellospringboot.* TO 'dbuser'@'%';-- メールアドレスが'hanako'を含むユーザー

FLUSH PRIVILEGES;SELECT * FROM users WHERE email LIKE '%hanako%';

EXIT;

```-- ユーザー数をカウント

SELECT COUNT(*) FROM users;

---

-- ユーザーを削除

## 💡 補足知識DELETE FROM users WHERE id = 1;



### Docker Composeの基本コマンド-- 確認

SELECT * FROM users;

```bash```

# コンテナ起動（バックグラウンド）

docker-compose up -d### チャレンジ 3: JOINを試す



# コンテナ停止`orders`テーブルを作成して、`users`テーブルとJOINしてみましょう。

docker-compose stop

```sql

# コンテナ停止＆削除CREATE TABLE orders (

docker-compose down    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,

# コンテナ停止＆削除＆ボリューム削除（データも削除）    product_name VARCHAR(100),

docker-compose down -v    amount INT

);

# ログ表示

docker-compose logs -fINSERT INTO orders (user_id, product_name, amount) VALUES (2, 'Laptop', 1200);

INSERT INTO orders (user_id, product_name, amount) VALUES (3, 'Mouse', 25);

# コンテナの状態確認

docker-compose ps-- JOINでユーザー名と注文を一緒に表示

```SELECT u.name, o.product_name, o.amount

FROM users u

### データの永続化についてJOIN orders o ON u.id = o.user_id;

```

`volumes`を設定することで、コンテナを削除してもデータが残ります。

---

```yaml

volumes:## 🐛 トラブルシューティング

  - mysql-data:/var/lib/mysql

```### H2 Consoleにアクセスできない



データを完全に削除したい場合：**エラー**: ブラウザで404 Not Found

```bash

docker-compose down -v**原因**: H2 Consoleが有効化されていない

```

**解決策**:

### 本番環境との違い1. `application.yml`で`spring.h2.console.enabled: true`を確認

2. アプリケーションを再起動

| 項目 | 開発環境（このステップ） | 本番環境 |3. URLが`http://localhost:8080/h2-console`であることを確認

|------|----------------------|----------|

| ddl-auto | `update` | `none`または`validate` |### "Database may be already in use"

| show-sql | `true` | `false` |

| パスワード管理 | ファイルに記述 | 環境変数・シークレット管理 |**エラー**: `Database "testdb" may be already in use`

| ボリューム | ローカル | クラウドストレージ |

**原因**: 別のプロセスがH2を使用中、またはアプリケーションが多重起動

---

**解決策**:

## 🎨 チャレンジ課題1. すべてのアプリケーションを停止

2. IntelliJ IDEAで実行中のプロセスを確認（停止ボタンをクリック）

### チャレンジ 1: 別のエンティティを追加3. 再起動



`Product`エンティティを作成して、MySQLにテーブルが自動生成されることを確認してください。### ログインできない



### チャレンジ 2: 環境別プロファイル**症状**: H2 Consoleで「Wrong user name or password」



`application-dev.yml`と`application-prod.yml`を分けて、開発環境と本番環境で異なるDB設定を管理してください。**解決策**:

- **JDBC URL**: `jdbc:h2:mem:testdb`（application.ymlと一致させる）

### チャレンジ 3: データベースの初期データ投入- **User Name**: `sa`

- **Password**: 空欄

`data.sql`ファイルを使って、アプリケーション起動時に初期データを投入してください。

### SQLが実行されない

---

**症状**: SQLを入力しても何も起こらない

## 📚 このステップで学んだこと

**解決策**:

- ✅ Docker ComposeでMySQL環境を構築- 「Run」ボタンをクリック（または Ctrl+Enter / Cmd+Enter）

- ✅ Spring BootからMySQLへの接続- SQL末尾にセミコロン（`;`）があるか確認

- ✅ application.ymlでのDB設定管理

- ✅ JPAによるテーブル自動生成（ddl-auto）### テーブルが見つからない

- ✅ MySQLクライアントでのデータベース操作

**エラー**: `Table "USERS" not found`

---

**原因**: テーブルをまだ作成していない、またはアプリケーションを再起動してデータが消えた

## 🔄 Gitへのコミット

**解決策**:

```bash- `CREATE TABLE`文を再実行

git add .- 次のステップ（Step 7）でエンティティクラスから自動生成する方法を学びます

git commit -m "Phase 2: STEP_6完了（MySQL環境構築）"

git push origin main---

```

## 📚 このステップで学んだこと

---

- ✅ H2データベースの特徴（インメモリDB）

## ➡️ 次のステップ- ✅ H2とSpring Data JPAの依存関係追加

- ✅ `application.yml`でデータソース設定

次は[Step 7: Spring Data JPAでCRUDの基本](STEP_7.md)へ進みましょう！- ✅ H2 Consoleの有効化とブラウザでのアクセス

- ✅ SQLでのテーブル作成・データ挿入・検索

MySQLの準備が整ったので、次はSpring Data JPAを使ってデータベース操作を学びます。- ✅ インメモリDBとファイルDBの違い

- ✅ `ddl-auto`設定の理解

---

---

お疲れさまでした！ 🎉

## 💡 補足: Spring BootのデータベースAutoConfiguration

### なぜ設定が少ないのか？

Spring Bootは**AutoConfiguration**で多くを自動設定します：

1. `pom.xml`にH2とJPAの依存関係がある
2. → Spring Bootが「データベースを使うんだな」と判断
3. → H2のJDBCドライバを自動設定
4. → データソースを自動作成
5. → Hibernateを自動設定

**従来のSpring Frameworkとの違い**:
- 従来: XML設定ファイルで大量の設定が必要
- Spring Boot: `application.yml`で最小限の設定のみ

### show-sqlの出力例

`show-sql: true`を設定すると、実行されるSQLがコンソールに表示されます：

```
Hibernate: 
    insert 
    into
        users
        (email, name, id) 
    values
        (?, ?, ?)
```

**学習には便利ですが、本番環境では`false`にすることを推奨します。**

---

## 🔄 Gitへのコミットとレビュー依頼

進捗を記録してレビューを受けましょう：

```bash
git add .
git commit -m "Step 6: H2データベース導入完了"
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

レビューが完了したら、[Step 7: Spring Data JPAでCRUDの基本](STEP_7.md)へ進みましょう！

次のステップでは、SQLを書かずに、Javaのコードだけでデータベース操作を行う方法を学びます。
`@Entity`でテーブル定義、`JpaRepository`でCRUD操作を実装します！

---

お疲れさまでした！ 🎉

H2データベースの環境が整いました。
次はいよいよJPAを使って、Javaでデータベース操作を行います！
