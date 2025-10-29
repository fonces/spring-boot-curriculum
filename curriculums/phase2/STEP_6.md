# Step 6: MySQL環境構築# Step 6: MySQL環境構築# Step 6: H2データベース導入



## 🎯 このステップの目標



- Docker Composeを使ってMySQL環境を構築する## 🎯 このステップの目標## 🎯 このステップの目標

- Spring BootからMySQLに接続する

- データベースの基本的な操作を理解する

- application.ymlでデータベース接続設定を管理する

- Docker Composeを使ってMySQL環境を構築する- H2データベースとは何かを理解する

**所要時間**: 約40分

- Spring BootからMySQLに接続する- Spring BootにH2依存関係を追加する

---

- データベースの基本的な操作を理解する- H2 Consoleを有効化してブラウザでデータベースを確認する

## 📋 事前準備

- application.ymlでデータベース接続設定を管理する- データベースの基本的な操作を理解する

- Phase 1で作成した`hello-spring-boot`プロジェクト

- Phase 1 (Step 1〜5) の完了

- **Docker Desktopのインストール**: [PREPARE.md](PREPARE.md)を参照

**所要時間**: 約40分**所要時間**: 約30分

**Phase 1をまだ完了していない場合**: [Phase 1](../phase1/STEP_1.md)を先に進めてください。



---

------

## 💡 なぜMySQLなのか？



### MySQL の特徴

## 📋 事前準備## 📋 事前準備

**MySQL** は、世界で最も人気のあるオープンソースのリレーショナルデータベースです。



**特徴**:

- ✅ **本番環境で広く使用**: 実務で最も使われるDB- Phase 1で作成した`hello-spring-boot`プロジェクト- Phase 1で作成した`hello-spring-boot`プロジェクト

- ✅ **豊富なドキュメント**: 情報が多く学習しやすい

- ✅ **高いパフォーマンス**: 大規模なデータにも対応- Phase 1 (Step 1〜5) の完了- Phase 1 (Step 1〜5) の完了

- ✅ **Docker対応**: 開発環境の構築が簡単

- **Docker Desktopのインストール**: [Docker Desktop](https://www.docker.com/products/docker-desktop/)

### PostgreSQLも選択肢

**Phase 1をまだ完了していない場合**: [Phase 1](../phase1/STEP_1.md)を先に進めてください。

PostgreSQLも優れたデータベースです。MySQLの代わりにPostgreSQLを使用しても構いません。

このカリキュラムではMySQLを例に説明しますが、PostgreSQLでも同様の手順で進められます。**Phase 1をまだ完了していない場合**: [Phase 1](../phase1/STEP_1.md)を先に進めてください。



------



## 🚀 ステップ1: Docker ComposeでMySQL起動---



### 1-1. docker-compose.ymlの作成## 💡 H2データベースとは？



プロジェクトのルートディレクトリに`docker-compose.yml`を作成します。## 💡 なぜMySQLから始めるのか？



**ファイルパス**: `docker-compose.yml`（`pom.xml`と同じ階層）### H2の特徴



```yaml### MySQL の特徴

version: '3.8'

**H2 Database** は、Javaで書かれた軽量なリレーショナルデータベースです。

services:

  mysql:**MySQL** は、世界で最も人気のあるオープンソースのリレーショナルデータベースです。

    image: mysql:8.0

    container_name: spring-boot-mysql**特徴**:

    environment:

      MYSQL_ROOT_PASSWORD: rootpassword**特徴**:- ✅ **インメモリモード**: データをメモリ上に保存（高速）

      MYSQL_DATABASE: hellospringboot

      MYSQL_USER: dbuser- ✅ **本番環境で広く使用**: 実務で最も使われるDB- ✅ **ファイルモード**: データをファイルに保存（永続化）

      MYSQL_PASSWORD: dbpassword

    ports:- ✅ **豊富なドキュメント**: 情報が多く学習しやすい- ✅ **組み込み可能**: アプリケーションに組み込んで使用

      - "3306:3306"

    volumes:- ✅ **高いパフォーマンス**: 大規模なデータにも対応- ✅ **開発に最適**: セットアップ不要、すぐ使える

      - mysql-data:/var/lib/mysql

    command: --default-authentication-plugin=mysql_native_password- ✅ **Docker対応**: 開発環境の構築が簡単- ✅ **ブラウザUI**: H2 Consoleで簡単にデータ確認



volumes:

  mysql-data:

```### H2ではなくMySQLで学ぶ理由### なぜH2から始めるのか？



**設定の説明**:



| 項目 | 説明 || 比較項目 | MySQL | H2 || 比較項目 | H2 | MySQL/PostgreSQL |

|------|------|

| `image: mysql:8.0` | MySQL 8.0のDockerイメージを使用 ||---------|-------|----||---------|----|--------------------|

| `MYSQL_DATABASE` | 作成するデータベース名 |

| `MYSQL_USER` | アプリケーション用のユーザー名 || 本番利用 | ✅ 推奨 | ❌ 非推奨 || セットアップ | 依存関係を追加するだけ | インストールまたはDocker必要 |

| `MYSQL_PASSWORD` | ユーザーのパスワード |

| `ports: "3306:3306"` | ホストの3306番ポートをコンテナの3306番にマッピング || 実務スキル | 直接役立つ | 学習専用 || 起動速度 | 即座 | 数秒〜数十秒 |

| `volumes` | データの永続化 |

| SQL方言 | 本物のMySQL SQL | 標準SQL寄り || 学習コスト | 低い | やや高い |

### 1-2. MySQLコンテナの起動

| データ永続化 | ✅ 標準 | 設定が必要 || 本番利用 | ❌ 推奨しない | ✅ 推奨 |

```bash

docker compose up -d

```

**Phase 2の学習戦略**:**Phase 2の学習戦略**:

**オプション**:

- `-d`: バックグラウンドで実行（デタッチモード）1. **Step 6**: MySQLのDocker環境構築1. **Step 6〜11**: H2で基本を学ぶ



**出力例**:2. **Step 7〜11**: Spring Data JPAでCRUD操作2. **Step 12**: MySQLに切り替える

```

[+] Running 2/23. **Step 12〜13**: MyBatisで直接SQL操作

 ✔ Network hello-spring-boot_default  Created

 ✔ Container spring-boot-mysql        Started4. **Step 14**: JPAとMyBatisの使い分け---

```



### 1-3. コンテナの状態確認

---## 🚀 ステップ1: H2依存関係の追加

```bash

docker compose ps

```

## 🚀 ステップ1: Docker Composeファイルの作成### 1-1. pom.xmlを編集

**出力例**:

```

NAME                 IMAGE       STATUS        PORTS

spring-boot-mysql    mysql:8.0   Up 10 seconds 0.0.0.0:3306->3306/tcp### 1-1. プロジェクトルートにdocker-compose.ymlを作成プロジェクトのルートにある`pom.xml`を開きます。

```



`STATUS`が`Up`になっていればOKです！

**ファイルパス**: `docker-compose.yml`**ファイルパス**: `pom.xml`

### 1-4. MySQLに接続してみる



```bash

docker exec -it spring-boot-mysql mysql -udbuser -pdbpassword```yaml`<dependencies>`セクション内に以下を追加：

```

version: '3.8'

MySQLプロンプトが表示されます：

```xml

```

mysql> services:<dependencies>

```

  mysql:    <!-- 既存の依存関係 -->

以下のコマンドでデータベースを確認：

    image: mysql:8.0    <dependency>

```sql

SHOW DATABASES;    container_name: spring-boot-mysql        <groupId>org.springframework.boot</groupId>

```

    environment:        <artifactId>spring-boot-starter-web</artifactId>

**出力例**:

```      MYSQL_ROOT_PASSWORD: rootpassword    </dependency>

+--------------------+

| Database           |      MYSQL_DATABASE: hellospringboot

+--------------------+

| hellospringboot    |      MYSQL_USER: dbuser    <dependency>

| information_schema |

| performance_schema |      MYSQL_PASSWORD: dbpassword        <groupId>org.projectlombok</groupId>

+--------------------+

```    ports:        <artifactId>lombok</artifactId>



`hellospringboot`データベースが作成されていることを確認できました！      - "3306:3306"        <optional>true</optional>



```sql    volumes:    </dependency>

exit;

```      - mysql-data:/var/lib/mysql



で終了します。    command: --default-authentication-plugin=mysql_native_password    <!-- H2 Databaseを追加 -->



---    <dependency>



## 🚀 ステップ2: Spring BootにMySQL依存関係を追加volumes:        <groupId>com.h2database</groupId>



### 2-1. pom.xmlを編集  mysql-data:        <artifactId>h2</artifactId>



`pom.xml`の`<dependencies>`セクションに以下を追加します。```        <scope>runtime</scope>



**ファイルパス**: `pom.xml`    </dependency>



```xml### 1-2. 設定の説明

<dependencies>

    <!-- 既存の依存関係 -->    <!-- Spring Data JPAを追加 -->

    <dependency>

        <groupId>org.springframework.boot</groupId>| 項目 | 説明 |    <dependency>

        <artifactId>spring-boot-starter-web</artifactId>

    </dependency>|------|------|        <groupId>org.springframework.boot</groupId>

    <dependency>

        <groupId>org.projectlombok</groupId>| `image: mysql:8.0` | MySQL 8.0の公式イメージを使用 |        <artifactId>spring-boot-starter-data-jpa</artifactId>

        <artifactId>lombok</artifactId>

        <optional>true</optional>| `MYSQL_ROOT_PASSWORD` | rootユーザーのパスワード |    </dependency>

    </dependency>

| `MYSQL_DATABASE` | 初期作成されるデータベース名 |

    <!-- Spring Data JPA -->

    <dependency>| `MYSQL_USER` | アプリケーション用のユーザー名 |    <!-- テスト用（既存） -->

        <groupId>org.springframework.boot</groupId>

        <artifactId>spring-boot-starter-data-jpa</artifactId>| `MYSQL_PASSWORD` | アプリケーション用のパスワード |    <dependency>

    </dependency>

| `ports: 3306:3306` | ホストの3306ポートをコンテナの3306に接続 |        <groupId>org.springframework.boot</groupId>

    <!-- MySQL Driver -->

    <dependency>| `volumes` | データを永続化（コンテナ削除後もデータが残る） |        <artifactId>spring-boot-starter-test</artifactId>

        <groupId>com.mysql</groupId>

        <artifactId>mysql-connector-j</artifactId>        <scope>test</scope>

        <scope>runtime</scope>

    </dependency>---    </dependency>



    <dependency></dependencies>

        <groupId>org.springframework.boot</groupId>

        <artifactId>spring-boot-starter-test</artifactId>## 🚀 ステップ2: MySQLコンテナの起動```

        <scope>test</scope>

    </dependency>

</dependencies>

```### 2-1. Docker Composeでコンテナを起動### 1-2. 依存関係の解説



**追加した依存関係**:



#### Spring Data JPA```bash#### H2 Database

```xml

<dependency>docker-compose up -d

    <groupId>org.springframework.boot</groupId>

    <artifactId>spring-boot-starter-data-jpa</artifactId>``````xml

</dependency>

```<dependency>

- JPAとHibernateを含むSpring Dataのスターター

- データベース操作を簡単にする**出力例**:    <groupId>com.h2database</groupId>



#### MySQL Connector```    <artifactId>h2</artifactId>

```xml

<dependency>[+] Running 2/2    <scope>runtime</scope>

    <groupId>com.mysql</groupId>

    <artifactId>mysql-connector-j</artifactId> ✔ Network hello-spring-boot_default       Created</dependency>

    <scope>runtime</scope>

</dependency> ✔ Container spring-boot-mysql             Started```

```

- MySQLデータベースのJDBCドライバを提供```

- `<scope>runtime</scope>`: 実行時のみ必要（コンパイル時は不要）

- **`<scope>runtime</scope>`**: 実行時のみ必要（コンパイル時は不要）

### 2-2. 依存関係の更新

### 2-2. コンテナの状態確認- H2データベースのJDBCドライバを提供

IDEで自動的に依存関係がダウンロードされますが、手動で更新する場合：



```bash

./mvnw clean install```bash#### Spring Data JPA

```

docker-compose ps

---

``````xml

## 🚀 ステップ3: データベース接続設定

<dependency>

### 3-1. application.ymlの作成

**出力例**:    <groupId>org.springframework.boot</groupId>

`src/main/resources/application.properties`を削除して、代わりに`application.yml`を作成します。

```    <artifactId>spring-boot-starter-data-jpa</artifactId>

**ファイルパス**: `src/main/resources/application.yml`

NAME                 IMAGE       STATUS       PORTS</dependency>

```yaml

spring:spring-boot-mysql    mysql:8.0   Up 10 sec    0.0.0.0:3306->3306/tcp```

  datasource:

    url: jdbc:mysql://localhost:3306/hellospringboot```

    username: dbuser

    password: dbpassword- **JPA (Java Persistence API)**: Javaでデータベース操作を行う標準仕様

    driver-class-name: com.mysql.cj.jdbc.Driver

### 2-3. MySQLログの確認- **Hibernate**: JPAの実装（自動的に含まれる）

  jpa:

    hibernate:- **Spring Data JPA**: JPAをさらに使いやすくするSpringのライブラリ

      ddl-auto: update

    show-sql: true```bash

    properties:

      hibernate:docker-compose logs mysql### 1-3. Mavenプロジェクトの更新

        format_sql: true

        dialect: org.hibernate.dialect.MySQLDialect```

```

IntelliJ IDEAで：

### 3-2. 設定の説明

`ready for connections` というメッセージが表示されればOKです。1. `pom.xml`を保存

#### データソース設定

2. 右上の「Load Maven Changes」（Mアイコン）をクリック

```yaml

spring:---3. 依存関係がダウンロードされるまで待つ

  datasource:

    url: jdbc:mysql://localhost:3306/hellospringboot

    username: dbuser

    password: dbpassword## 🚀 ステップ3: Spring BootにMySQL依存関係を追加---

    driver-class-name: com.mysql.cj.jdbc.Driver

```



- `url`: MySQL接続URL### 3-1. pom.xmlを編集## 🚀 ステップ2: H2 Consoleの有効化

  - `localhost:3306`: Dockerで公開したポート

  - `hellospringboot`: データベース名

- `username`: MySQLのユーザー名（docker-compose.ymlで設定）

- `password`: パスワード**ファイルパス**: `pom.xml`### 2-1. application.ymlの設定



#### JPA/Hibernate設定



```yaml```xml`src/main/resources/application.yml`を開いて、以下を追加：

  jpa:

    hibernate:<dependencies>

      ddl-auto: update

    show-sql: true    <!-- 既存の依存関係 -->**ファイルパス**: `src/main/resources/application.yml`

    properties:

      hibernate:    <dependency>

        format_sql: true

        dialect: org.hibernate.dialect.MySQLDialect        <groupId>org.springframework.boot</groupId>```yaml

```

        <artifactId>spring-boot-starter-web</artifactId>server:

**ddl-autoの値**:

    </dependency>  port: 8080

| 値 | 説明 | 推奨環境 |

|----|------|---------|    <dependency>

| `create` | 起動時にテーブルを削除して再作成 | テスト |

| `create-drop` | 起動時に作成、終了時に削除 | テスト |        <groupId>org.projectlombok</groupId>spring:

| `update` | テーブルがなければ作成、あれば更新 | 開発 |

| `validate` | スキーマ検証のみ | 本番 |        <artifactId>lombok</artifactId>  # H2 Console設定

| `none` | 何もしない | 本番 |

        <optional>true</optional>  h2:

**その他の設定**:

- `show-sql: true`: 実行されるSQLをコンソールに表示    </dependency>    console:

- `format_sql: true`: SQLを整形して表示

- `dialect`: MySQL用のSQL方言を指定      enabled: true  # H2 Consoleを有効化



---    <!-- Spring Data JPA -->      path: /h2-console  # アクセスパス



## 🚀 ステップ4: エンティティの作成    <dependency>



### 4-1. Userエンティティの作成        <groupId>org.springframework.boot</groupId>  # データソース設定



新しいパッケージ`entity`を作成し、`User`エンティティを作成します。        <artifactId>spring-boot-starter-data-jpa</artifactId>  datasource:



**ファイルパス**: `src/main/java/com/example/hellospringboot/entity/User.java`    </dependency>    url: jdbc:h2:mem:testdb  # インメモリDB、名前は"testdb"



```java    driverClassName: org.h2.Driver

package com.example.hellospringboot.entity;

    <!-- MySQL Driver -->    username: sa  # デフォルトユーザー名

import jakarta.persistence.*;

import lombok.*;    <dependency>    password:     # パスワードなし



@Entity        <groupId>com.mysql</groupId>

@Table(name = "users")

@Getter        <artifactId>mysql-connector-j</artifactId>  # JPA設定

@Setter

@NoArgsConstructor        <scope>runtime</scope>  jpa:

@AllArgsConstructor

@Builder    </dependency>    database-platform: org.hibernate.dialect.H2Dialect

public class User {

    hibernate:

    @Id

    @GeneratedValue(strategy = GenerationType.IDENTITY)    <!-- Test -->      ddl-auto: update  # テーブルを自動作成/更新

    private Long id;

    <dependency>    show-sql: true  # 実行されるSQLをコンソールに表示

    @Column(nullable = false, length = 50)

    private String name;        <groupId>org.springframework.boot</groupId>    properties:



    @Column(nullable = false, unique = true, length = 100)        <artifactId>spring-boot-starter-test</artifactId>      hibernate:

    private String email;

        <scope>test</scope>        format_sql: true  # SQLを整形して表示

    @Column

    private Integer age;    </dependency>

}

```</dependencies># アプリケーション情報（Phase 1から継続）



**アノテーションの説明**:```app:



| アノテーション | 説明 |  name: Hello Spring Boot Application

|--------------|------|

| `@Entity` | JPAエンティティとして認識 |### 3-2. Mavenの依存関係を更新  version: 1.0.0

| `@Table(name = "users")` | テーブル名を指定 |

| `@Id` | 主キー |  description: Spring Bootを学ぶためのサンプルアプリケーション

| `@GeneratedValue` | 自動採番（AUTO_INCREMENT） |

| `@Column` | カラムの制約を指定 |IDEでMavenを再読み込みするか、以下を実行：```



### 4-2. アプリケーションの起動



```bash```bash### 2-2. 設定の解説

./mvnw spring-boot:run

```./mvnw clean install



**ログ確認ポイント**:```#### H2 Console設定

```

Hibernate: create table users (...)

```

が表示されればOKです！---```yaml



このログが表示されれば、テーブルが自動生成されています。spring:



---## 🚀 ステップ4: データベース接続設定  h2:



## 🚀 ステップ5: データベースの確認    console:



### 5-1. DBeaverで確認（推奨）### 4-1. application.ymlにMySQL接続情報を追加      enabled: true



**DBeaver**は無料で使いやすいデータベースビューアーです。      path: /h2-console



1. [https://dbeaver.io/](https://dbeaver.io/)からダウンロード＆インストール**ファイルパス**: `src/main/resources/application.yml````

2. 新しい接続を作成:

   - Database: MySQL

   - Host: localhost

   - Port: 3306```yaml- `enabled: true`: H2の管理画面を有効化

   - Database: hellospringboot

   - Username: dbuserspring:- `path: /h2-console`: ブラウザでアクセスするパス

   - Password: dbpassword

3. 接続して`users`テーブルを確認  application:



### 5-2. MySQL CLIで確認    name: hello-spring-boot#### データソース設定



```bash  

docker exec -it spring-boot-mysql mysql -udbuser -pdbpassword hellospringboot

```  datasource:```yaml



以下のSQLを実行：    url: jdbc:mysql://localhost:3306/hellospringboot?useSSL=false&serverTimezone=Asia/Tokyo&allowPublicKeyRetrieval=truedatasource:



```sql    username: dbuser  url: jdbc:h2:mem:testdb

SHOW TABLES;

```    password: dbpassword  driverClassName: org.h2.Driver



**出力例**:    driver-class-name: com.mysql.cj.jdbc.Driver  username: sa

```

+---------------------------+    password:

| Tables_in_hellospringboot |

+---------------------------+  jpa:```

| users                     |

+---------------------------+    hibernate:

```

      ddl-auto: update- `jdbc:h2:mem:testdb`: インメモリDBを使用、DB名は`testdb`

テーブル構造を確認：

    show-sql: true- `username: sa`: デフォルトユーザー（System Administrator）

```sql

DESC users;    properties:- `password:` (空): パスワードなし

```

      hibernate:

**出力例**:

```        format_sql: true#### JPA設定

+-------+--------------+------+-----+---------+----------------+

| Field | Type         | Null | Key | Default | Extra          |        dialect: org.hibernate.dialect.MySQLDialect

+-------+--------------+------+-----+---------+----------------+

| id    | bigint       | NO   | PRI | NULL    | auto_increment |```yaml

| name  | varchar(50)  | NO   |     | NULL    |                |

| email | varchar(100) | NO   | UNI | NULL    |                |server:jpa:

| age   | int          | YES  |     | NULL    |                |

+-------+--------------+------+-----+---------+----------------+  port: 8080  hibernate:

```

    ddl-auto: update

期待通りのテーブルが作成されています！

logging:  show-sql: true

```sql

exit;  level:```

```

    org.hibernate.SQL: DEBUG

---

    org.hibernate.type.descriptor.sql.BasicBinder: TRACE- `ddl-auto: update`: エンティティクラスからテーブルを自動生成

## ✅ チェックリスト

```  - `create`: 起動時に毎回テーブルを作り直す

- [ ] Docker ComposeでMySQLを起動できた

- [ ] MySQLコンテナが正常に動いている（`docker compose ps`）  - `update`: テーブルがなければ作成、あれば更新

- [ ] Spring BootからMySQLに接続できた

- [ ] `users`テーブルが自動作成された### 4-2. 設定の説明  - `none`: 何もしない（本番環境推奨）

- [ ] DBeaverまたはMySQL CLIでテーブルを確認できた

- `show-sql: true`: SQLログを出力（学習用）

---

#### DataSource設定

## 🎨 チャレンジ課題

---

### チャレンジ 1: PostgreSQLに切り替え

| 項目 | 説明 |

MySQLの代わりにPostgreSQLを使ってみましょう。

|------|------|## 🚀 ステップ3: アプリケーションの起動と確認

**ヒント**:

- `docker-compose.yml`でPostgreSQLイメージに変更| `url` | MySQL接続URL |

- `pom.xml`でPostgreSQLドライバに変更

- `application.yml`の接続情報を変更| `username` | データベースユーザー名 |### 3-1. アプリケーション起動



### チャレンジ 2: データベースGUIツールを使ってみる| `password` | データベースパスワード |



DBeaver以外のGUIツールも試してみましょう：| `driver-class-name` | MySQLドライバークラス |`HelloSpringBootApplication.java`を実行して、アプリケーションを起動します。

- MySQL Workbench

- DataGrip（有料）

- TablePlus

#### JPA/Hibernate設定コンソールに以下のようなログが表示されることを確認：

---



## 🔧 トラブルシューティング

| 項目 | 説明 |```

### MySQLコンテナが起動しない

|------|------|Hibernate: 

**症状**: `docker compose up -d`でエラー

| `ddl-auto: update` | エンティティからテーブルを自動生成・更新 |    

**原因**: ポート3306が既に使用されている

| `show-sql: true` | 実行されるSQLをログ出力 |    drop table if exists users CASCADE 

**解決策**:

```bash| `format_sql: true` | SQLを整形して表示 |Hibernate: 

# ポート3306を使用しているプロセスを確認

lsof -i :3306| `dialect` | MySQL用のSQL方言 |    



# 既存のMySQLを停止、またはdocker-compose.ymlでポートを変更    create table users (

ports:

  - "3307:3306"**ddl-autoの値**:       ...

```

- `create`: 起動時にテーブルを削除して再作成```

### Spring Bootが起動しない

- `create-drop`: 起動時に作成、終了時に削除

**症状**: `Connection refused`エラー

- `update`: テーブルがなければ作成、あれば更新（本番非推奨）（まだエンティティを作成していないので、テーブルは作成されませんが、H2は起動しています）

**原因**: MySQLコンテナが起動していない

- `validate`: スキーマ検証のみ

**解決策**:

```bash- `none`: 何もしない（本番推奨）### 3-2. H2 Consoleにアクセス

docker compose ps  # コンテナの状態を確認

docker compose up -d  # コンテナを起動

```

---ブラウザで以下のURLにアクセス：

### テーブルが作成されない



**症状**: `users`テーブルが見つからない

## 🚀 ステップ5: 簡単なエンティティで接続確認```

**原因**: `ddl-auto`が`none`または`validate`になっている

http://localhost:8080/h2-console

**解決策**: `application.yml`で`ddl-auto: update`に設定

### 5-1. Userエンティティの作成```

---



## 📚 このステップで学んだこと

**ファイルパス**: `src/main/java/com/example/hellospringboot/entity/User.java`**H2ログイン画面**が表示されます。

- ✅ Docker ComposeでMySQLを起動する方法

- ✅ Spring BootからMySQLに接続する設定

- ✅ JPAエンティティからテーブルを自動生成

- ✅ application.ymlでの設定管理```java### 3-3. H2 Consoleでログイン

- ✅ データベースGUIツール（DBeaver）の使い方

package com.example.hellospringboot.entity;

---

ログイン画面で以下を入力：

## 🔄 Gitへのコミット

import jakarta.persistence.*;

```bash

git add .import lombok.*;- **Saved Settings**: Generic H2 (Embedded)

git commit -m "Step 6: MySQL環境構築完了

- **Setting Name**: Generic H2 (Embedded)

- Docker ComposeでMySQL起動

- Spring Data JPA依存関係追加@Entity- **Driver Class**: `org.h2.Driver`（自動入力済み）

- MySQL接続設定

- Userエンティティ作成"@Table(name = "users")- **JDBC URL**: `jdbc:h2:mem:testdb`

git push origin main

```@Getter- **User Name**: `sa`



---@Setter- **Password**: （空欄のまま）



## ➡️ 次のステップ@NoArgsConstructor



次は[Step 7: Spring Data JPAの基本](STEP_7.md)へ進みましょう！@AllArgsConstructor「Connect」ボタンをクリックします。



Spring Data JPAを使って、データベースのCRUD操作を実装します。@Builder



---public class User {### 3-4. H2 Consoleの画面確認



お疲れさまでした！ 🎉



MySQLとSpring Bootの接続ができました。    @Idログインに成功すると、以下の画面が表示されます：

次のステップでは、実際にデータの作成・読み取り・更新・削除を実装していきます。

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
