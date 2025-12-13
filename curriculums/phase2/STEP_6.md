# Step 6: Spring BootとMySQLの接続

## 🎯 このステップの目標

- Spring BootプロジェクトにMySQL接続の設定を追加できる
- Spring Data JPAの基本を理解できる
- 最初のエンティティ（Entityクラス）を作成できる
- データベースにテーブルが自動作成されることを確認できる

**所要時間**: 約45分

---

## 📋 事前準備

- Phase 2の[事前準備](PREPARE.md)が完了していること
- Phase 1で作成した`hello-spring-boot`プロジェクト
- MySQLコンテナが起動していること（`docker compose ps`で確認）

---

## 🧩 Spring Data JPAとは

### JPAの役割

**JPA（Java Persistence API）** は、Javaオブジェクトとデータベースのテーブルを対応付ける仕組みです。

```
┌─────────────────────────────────┐
│      Javaアプリケーション         │
│                                 │
│   ┌─────────────────┐           │
│   │  Product.java   │  ←──┐    │
│   │  (Entity)       │      │    │
│   └─────────────────┘      │    │
│           ↕                │    │
│   ┌─────────────────┐      │    │
│   │  Spring Data    │      │ JPA │
│   │  JPA            │      │ が  │
│   └─────────────────┘      │ 変換 │
│           ↕                │    │
│   ┌─────────────────┐      │    │
│   │  JDBC           │  ←──┘    │
│   └─────────────────┘           │
└─────────────────────────────────┘
            ↕
┌─────────────────────────────────┐
│  Docker MySQLコンテナ            │
│   ┌─────────────────┐           │
│   │ productsテーブル  │           │
│   └─────────────────┘           │
└─────────────────────────────────┘
```

**メリット**:
- **SQL不要**: 基本的なCRUD操作はSQLを書かずに実行できる
- **型安全**: Javaのクラスとしてデータをやり取りできるためコンパイル時にエラー検出
- **データベース非依存**: MySQL、PostgreSQL、H2などを簡単に切り替え可能
- **ボイラープレート削減**: DAOの実装を大幅に削減

---

## 🚀 ステップ1: 依存関係の追加

### 1-1. pom.xmlの編集

`pom.xml`に以下の依存関係を追加します。

**ファイルパス**: `pom.xml`

既存の`<dependencies>`セクションに以下を**追加**してください：

```xml
		<!-- Phase 1からの既存の依存関係 -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>

		<!-- ここから追加 -->
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
		<!-- ここまで追加 -->

		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>
```

### 1-2. 依存関係の解説

#### `spring-boot-starter-data-jpa`

Spring Data JPAの機能をまとめたスターターです。以下が含まれます：

- **Hibernate**: JPA実装の標準的なライブラリ
- **Spring Data JPA**: リポジトリの自動実装
- **Jakarta Persistence API**: JPAの仕様

#### `mysql-connector-j`

Java向けのMySQLドライバです。

- **scope: runtime**: コンパイル時には不要、実行時に必要
- Spring Boot 3.x系では、旧名称`mysql-connector-java`から`mysql-connector-j`に変更されました

---

## 🚀 ステップ2: データベース接続設定

### 2-1. application.ymlの編集

`src/main/resources/application.yml`にMySQL接続情報を追加します。

**ファイルパス**: `src/main/resources/application.yml`

```yaml
spring:
  application:
    name: hello-spring-boot
  
  # データソース設定
  datasource:
    url: jdbc:mysql://localhost:3306/spring_boot_db
    username: springuser
    password: springpass
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

# カスタム設定（Phase 1から継続）
app:
  message: "Hello from YAML!"
```

### 2-2. 設定の解説

#### `spring.datasource.*`

| 設定 | 説明 |
|---|---|
| `url` | JDBC接続URL（`jdbc:mysql://ホスト:ポート/データベース名`） |
| `username` | データベースユーザー名（docker-compose.ymlで設定） |
| `password` | データベースパスワード（docker-compose.ymlで設定） |
| `driver-class-name` | JDBCドライバのクラス名 |

**ポイント**:
- `localhost:3306`は、docker-compose.ymlで公開したポート
- ユーザー名とパスワードは、docker-compose.ymlの`environment`で設定した値

#### `spring.jpa.hibernate.ddl-auto`

エンティティからテーブルを自動生成する設定です。

| 値 | 説明 | 使用場面 |
|---|---|---|
| `none` | 何もしない | 本番環境 |
| `validate` | スキーマを検証のみ | 本番環境 |
| `update` | 差分があれば更新（テーブル削除はしない） | 開発環境 ✅ |
| `create` | 起動時に毎回テーブルを作り直す（データ消失） | テスト環境 |
| `create-drop` | 起動時に作成、終了時に削除 | テスト環境 |

**注意**: 本番環境では`validate`または`none`にし、マイグレーションツール（Flyway、Liquibaseなど）を使用します。

#### `spring.jpa.show-sql`

実行されるSQLをコンソールに出力します（開発時のデバッグに便利）。

#### `spring.jpa.properties.hibernate.format_sql`

SQLを整形して見やすく表示します。

#### `spring.jpa.properties.hibernate.dialect`

使用するデータベースの方言を指定します。Spring Boot 3.xでは自動検出されますが、明示的に指定することでエラーを防げます。

---

## 🚀 ステップ3: 最初のエンティティを作成

### 3-1. Productエンティティの作成

Phase 1で作成した`User`クラスはそのままにして、新しく`Product`エンティティを作成します。

**ファイルパス**: `src/main/java/com/example/hellospringboot/Product.java`

```java
package com.example.hellospringboot;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private Integer price;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### 3-2. コードの解説

#### `@Entity`

このクラスがJPAエンティティ（データベーステーブルに対応するクラス）であることを示します。

#### `@Table(name = "products")`

- テーブル名を明示的に指定
- 省略した場合、クラス名を小文字にした`product`がデフォルト
- 複数形にすることで可読性が向上

#### `@Id`

主キー（Primary Key）を示します。

#### `@GeneratedValue(strategy = GenerationType.IDENTITY)`

主キーの生成戦略を指定します。

| 戦略 | 説明 | MySQLでの実装 |
|---|---|---|
| `IDENTITY` | データベースのAUTO_INCREMENTを使用 | ✅ 推奨 |
| `SEQUENCE` | シーケンスを使用 | ❌ MySQLは非対応 |
| `TABLE` | 専用テーブルで管理 | 遅い |
| `AUTO` | データベースに応じて自動選択 | 非推奨 |

#### `@Column`

カラムの詳細設定を行います。

```java
@Column(nullable = false, length = 100)
private String name;
```

| 属性 | 説明 | デフォルト |
|---|---|---|
| `nullable` | NULL許可 | `true` |
| `length` | 文字列の最大長 | 255 |
| `unique` | ユニーク制約 | `false` |
| `name` | カラム名（フィールド名と異なる場合） | フィールド名 |
| `columnDefinition` | カラム定義を直接指定 | - |
| `updatable` | 更新可能か | `true` |

#### `@PrePersist` と `@PreUpdate`

エンティティのライフサイクルイベントに応じた処理を定義します。

- `@PrePersist`: データベースに**INSERT前**に実行
- `@PreUpdate`: データベースに**UPDATE前**に実行

これにより、`createdAt`と`updatedAt`を自動設定できます。

---

## 🚀 ステップ4: MySQLコンテナの起動確認

### 4-1. コンテナの状態確認

```bash
docker compose ps
```

**期待される結果**:

```
NAME                 IMAGE       STATUS         PORTS
spring-boot-mysql    mysql:8.0   Up 10 minutes  0.0.0.0:3306->3306/tcp
```

`STATUS`が`Up`でなければ起動します：

```bash
docker compose up -d
```

---

## 🚀 ステップ5: アプリケーション起動とテーブル確認

### 5-1. 依存関係の更新

```bash
cd /path/to/hello-spring-boot
./mvnw clean install
```

### 5-2. アプリケーションの起動

```bash
./mvnw spring-boot:run
```

**コンソール出力を確認**:

```
Hibernate: create table products (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    description text,
    name varchar(100) not null,
    price integer not null,
    updated_at datetime(6) not null,
    primary key (id)
) engine=InnoDB
```

**ポイント**:
- `show-sql: true`により、実行されるSQLが出力されます
- `ddl-auto: update`により、テーブルが自動作成されます

### 5-3. MySQLで確認

別のターミナルを開いて、MySQLコンテナに接続します。

```bash
docker compose exec mysql mysql -u springuser -p
```

**パスワード**: `springpass`

```sql
USE spring_boot_db;

SHOW TABLES;
```

**期待される結果**:

```
+---------------------------+
| Tables_in_spring_boot_db  |
+---------------------------+
| products                  |
+---------------------------+
```

### 5-4. テーブル構造の確認

```sql
DESCRIBE products;
```

**期待される結果**:

```
+-------------+--------------+------+-----+---------+----------------+
| Field       | Type         | Null | Key | Default | Extra          |
+-------------+--------------+------+-----+---------+----------------+
| id          | bigint       | NO   | PRI | NULL    | auto_increment |
| created_at  | datetime(6)  | NO   |     | NULL    |                |
| description | text         | YES  |     | NULL    |                |
| name        | varchar(100) | NO   |     | NULL    |                |
| price       | int          | NO   |     | NULL    |                |
| updated_at  | datetime(6)  | NO   |     | NULL    |                |
+-------------+--------------+------+-----+---------+----------------+
```

完璧です！Javaのエンティティがデータベースのテーブルに自動変換されました。

```sql
EXIT;
```

---

## ✅ ステップ6: 動作確認

### 6-1. アプリケーションの再起動

`Ctrl + C`でアプリケーションを停止し、再度起動します。

```bash
./mvnw spring-boot:run
```

**確認ポイント**:
- 再起動時に`create table`が実行されない（`update`モードのため）
- テーブルが保持されている

### 6-2. データの永続性確認

MySQLで手動でデータを挿入します。

```bash
docker compose exec mysql mysql -u springuser -p
```

```sql
USE spring_boot_db;

INSERT INTO products (name, description, price, created_at, updated_at)
VALUES ('ノートPC', '高性能なノートパソコン', 150000, NOW(), NOW());

SELECT * FROM products;
```

**期待される結果**:

```
+----+------------+---------------------------+--------+----------------------------+----------------------------+
| id | name       | description               | price  | created_at                 | updated_at                 |
+----+------------+---------------------------+--------+----------------------------+----------------------------+
|  1 | ノートPC    | 高性能なノートパソコン      | 150000 | 2025-12-13 10:00:00.000000 | 2025-12-13 10:00:00.000000 |
+----+------------+---------------------------+--------+----------------------------+----------------------------+
```

アプリケーションを再起動しても、データが残っていることを確認してください。

```bash
# MySQLクライアントを抜ける
EXIT;

# アプリケーション停止（Ctrl + C）
# 再起動
./mvnw spring-boot:run
```

MySQLで再度確認:

```bash
docker compose exec mysql mysql -u springuser -pspringpass spring_boot_db -e "SELECT * FROM products;"
```

データが保持されていれば成功です！

---

## 🎨 チャレンジ課題

基本が理解できたら、以下にチャレンジしてみましょう：

### チャレンジ 1: Categoryエンティティを作成

商品カテゴリを管理する`Category`エンティティを作成してください。

**要件**:
- テーブル名: `categories`
- フィールド:
  - `id`: Long型、主キー、自動採番
  - `name`: String型、必須、最大50文字、ユニーク
  - `displayOrder`: Integer型、表示順序
  - `createdAt`: LocalDateTime型、作成日時
  - `updatedAt`: LocalDateTime型、更新日時

**ヒント**:

```java
@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 50, unique = true)
    private String name;
    
    // 残りのフィールドを追加
}
```

### チャレンジ 2: プロファイル別のデータソース設定

`application-dev.yml`と`application-prod.yml`で異なるデータベースに接続できるようにしてください。

**手順**:

1. 2つのデータベースを作成

```bash
docker compose exec mysql mysql -u root -p
```

```sql
CREATE DATABASE spring_boot_db_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE spring_boot_db_prod CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON spring_boot_db_dev.* TO 'springuser'@'%';
GRANT ALL PRIVILEGES ON spring_boot_db_prod.* TO 'springuser'@'%';
FLUSH PRIVILEGES;
EXIT;
```

2. `application-dev.yml`を作成:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/spring_boot_db_dev
```

3. `application-prod.yml`を作成:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/spring_boot_db_prod
  jpa:
    hibernate:
      ddl-auto: validate  # 本番環境では自動作成しない
    show-sql: false       # 本番環境ではSQL出力しない
```

4. プロファイルを切り替えて起動:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

### チャレンジ 3: Enumを使った商品ステータス

`Product`エンティティに、商品の販売状態を管理する`status`フィールドを追加してください。

**ヒント**:

```java
public enum ProductStatus {
    AVAILABLE,    // 販売中
    SOLD_OUT,     // 売り切れ
    DISCONTINUED  // 販売終了
}

// Productエンティティに追加
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private ProductStatus status = ProductStatus.AVAILABLE;
```

---

## 🐛 トラブルシューティング

### エラー: "Communications link failure"

**エラーメッセージ**:

```
com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure
```

**原因**: MySQLコンテナが起動していない、または接続情報が間違っている

**解決策**:

```bash
# コンテナの状態確認
docker compose ps

# 起動していなければ起動
docker compose up -d

# ログを確認
docker compose logs mysql
```

接続情報を再確認:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/spring_boot_db  # ポート番号が正しいか
    username: springuser  # ユーザー名が正しいか
    password: springpass  # パスワードが正しいか
```

### エラー: "Access denied for user 'springuser'@'localhost'"

**原因**: ユーザー名またはパスワードが間違っている、または権限がない

**解決策**:

docker-compose.ymlを確認:

```yaml
environment:
  MYSQL_USER: springuser
  MYSQL_PASSWORD: springpass
```

権限を確認:

```bash
docker compose exec mysql mysql -u root -prootpassword
```

```sql
SHOW GRANTS FOR 'springuser'@'%';
```

権限がなければ付与:

```sql
GRANT ALL PRIVILEGES ON spring_boot_db.* TO 'springuser'@'%';
FLUSH PRIVILEGES;
```

### エラー: "Unknown database 'spring_boot_db'"

**原因**: データベースが存在しない

**解決策**:

docker-compose.ymlの`MYSQL_DATABASE`を確認:

```yaml
environment:
  MYSQL_DATABASE: spring_boot_db
```

コンテナを再作成:

```bash
docker compose down
docker compose up -d
```

または手動で作成:

```bash
docker compose exec mysql mysql -u root -prootpassword
```

```sql
CREATE DATABASE spring_boot_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### エラー: "Table 'spring_boot_db.products' doesn't exist"

**原因**: テーブルが自動作成されていない

**解決策**:

`application.yml`の設定を確認:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # 'none'になっていないか確認
```

アプリケーションを再起動し、コンソールに`create table`のSQLが出力されるか確認してください。

### 警告: "HHH000424: Disabling contextual LOB creation"

**原因**: MySQL Connector/Jのバージョンと設定の互換性

**解決策**:

この警告は無視しても問題ありませんが、気になる場合は以下を追加:

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          lob:
            non_contextual_creation: true
```

---

## 📚 このステップで学んだこと

- ✅ Spring Data JPAとHibernateの役割を理解した
- ✅ `pom.xml`にJPA関連の依存関係を追加した
- ✅ `application.yml`でデータベース接続情報を設定した
- ✅ DockerコンテナのMySQLに接続する方法を理解した
- ✅ `@Entity`と`@Table`でエンティティクラスを作成した
- ✅ `@Id`と`@GeneratedValue`で主キーを定義した
- ✅ `@Column`でカラムの詳細設定を行った
- ✅ `@PrePersist`と`@PreUpdate`でタイムスタンプを自動設定した
- ✅ `ddl-auto: update`でテーブルが自動作成されることを確認した
- ✅ MySQLコンテナでテーブル構造とデータを確認した

---

## 💡 補足: JPAの歴史とHibernate

### JPAとは

**JPA（Java Persistence API）** は、Java EE（現Jakarta EE）の一部として策定された**標準仕様**です。

- **策定**: 2006年（JPA 1.0）
- **現在**: Jakarta Persistence 3.1（2023年）
- **目的**: O/Rマッピングの標準化

### Hibernateとは

**Hibernate** は、JPAの**実装**の一つです。

- **登場**: 2001年（JPA策定前）
- **位置づけ**: JPAの代表的な実装（事実上の標準）
- **Spring Bootデフォルト**: Hibernateが採用されている

### その他のJPA実装

- **EclipseLink**: Oracle公式のJPA実装
- **OpenJPA**: Apacheプロジェクト

Spring Bootでは、依存関係を変更することで実装を切り替えられますが、通常はHibernateで十分です。

---

## ➡️ 次のステップ

[Step 7: Spring Data JPAでCRUDの基本](STEP_7.md)へ進みましょう！

次のステップでは、`JpaRepository`を使って、SQL不要でデータベース操作（作成、読み取り、更新、削除）を実装します。
