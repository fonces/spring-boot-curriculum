# Step 6: H2データベース導入

## 🎯 このステップの目標

- H2データベースとは何かを理解する
- Spring BootにH2依存関係を追加する
- H2 Consoleを有効化してブラウザでデータベースを確認する
- データベースの基本的な操作を理解する

**所要時間**: 約30分

---

## 📋 事前準備

- Phase 1で作成した`hello-spring-boot`プロジェクト
- Phase 1 (Step 1〜5) の完了

**Phase 1をまだ完了していない場合**: [Phase 1](../phase1/STEP_1.md)を先に進めてください。

---

## 💡 H2データベースとは？

### H2の特徴

**H2 Database** は、Javaで書かれた軽量なリレーショナルデータベースです。

**特徴**:
- ✅ **インメモリモード**: データをメモリ上に保存（高速）
- ✅ **ファイルモード**: データをファイルに保存（永続化）
- ✅ **組み込み可能**: アプリケーションに組み込んで使用
- ✅ **開発に最適**: セットアップ不要、すぐ使える
- ✅ **ブラウザUI**: H2 Consoleで簡単にデータ確認

### なぜH2から始めるのか？

| 比較項目 | H2 | MySQL/PostgreSQL |
|---------|----|--------------------|
| セットアップ | 依存関係を追加するだけ | インストールまたはDocker必要 |
| 起動速度 | 即座 | 数秒〜数十秒 |
| 学習コスト | 低い | やや高い |
| 本番利用 | ❌ 推奨しない | ✅ 推奨 |

**Phase 2の学習戦略**:
1. **Step 6〜11**: H2で基本を学ぶ
2. **Step 12**: MySQLに切り替える

---

## 🚀 ステップ1: H2依存関係の追加

### 1-1. pom.xmlを編集

プロジェクトのルートにある`pom.xml`を開きます。

**ファイルパス**: `pom.xml`

`<dependencies>`セクション内に以下を追加：

```xml
<dependencies>
    <!-- 既存の依存関係 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- H2 Databaseを追加 -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Spring Data JPAを追加 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- テスト用（既存） -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 1-2. 依存関係の解説

#### H2 Database

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

- **`<scope>runtime</scope>`**: 実行時のみ必要（コンパイル時は不要）
- H2データベースのJDBCドライバを提供

#### Spring Data JPA

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

- **JPA (Java Persistence API)**: Javaでデータベース操作を行う標準仕様
- **Hibernate**: JPAの実装（自動的に含まれる）
- **Spring Data JPA**: JPAをさらに使いやすくするSpringのライブラリ

### 1-3. Mavenプロジェクトの更新

IntelliJ IDEAで：
1. `pom.xml`を保存
2. 右上の「Load Maven Changes」（Mアイコン）をクリック
3. 依存関係がダウンロードされるまで待つ

---

## 🚀 ステップ2: H2 Consoleの有効化

### 2-1. application.ymlの設定

`src/main/resources/application.yml`を開いて、以下を追加：

**ファイルパス**: `src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  # H2 Console設定
  h2:
    console:
      enabled: true  # H2 Consoleを有効化
      path: /h2-console  # アクセスパス

  # データソース設定
  datasource:
    url: jdbc:h2:mem:testdb  # インメモリDB、名前は"testdb"
    driverClassName: org.h2.Driver
    username: sa  # デフォルトユーザー名
    password:     # パスワードなし

  # JPA設定
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: update  # テーブルを自動作成/更新
    show-sql: true  # 実行されるSQLをコンソールに表示
    properties:
      hibernate:
        format_sql: true  # SQLを整形して表示

# アプリケーション情報（Phase 1から継続）
app:
  name: Hello Spring Boot Application
  version: 1.0.0
  description: Spring Bootを学ぶためのサンプルアプリケーション
```

### 2-2. 設定の解説

#### H2 Console設定

```yaml
spring:
  h2:
    console:
      enabled: true
      path: /h2-console
```

- `enabled: true`: H2の管理画面を有効化
- `path: /h2-console`: ブラウザでアクセスするパス

#### データソース設定

```yaml
datasource:
  url: jdbc:h2:mem:testdb
  driverClassName: org.h2.Driver
  username: sa
  password:
```

- `jdbc:h2:mem:testdb`: インメモリDBを使用、DB名は`testdb`
- `username: sa`: デフォルトユーザー（System Administrator）
- `password:` (空): パスワードなし

#### JPA設定

```yaml
jpa:
  hibernate:
    ddl-auto: update
  show-sql: true
```

- `ddl-auto: update`: エンティティクラスからテーブルを自動生成
  - `create`: 起動時に毎回テーブルを作り直す
  - `update`: テーブルがなければ作成、あれば更新
  - `none`: 何もしない（本番環境推奨）
- `show-sql: true`: SQLログを出力（学習用）

---

## 🚀 ステップ3: アプリケーションの起動と確認

### 3-1. アプリケーション起動

`HelloSpringBootApplication.java`を実行して、アプリケーションを起動します。

コンソールに以下のようなログが表示されることを確認：

```
Hibernate: 
    
    drop table if exists users CASCADE 
Hibernate: 
    
    create table users (
       ...
```

（まだエンティティを作成していないので、テーブルは作成されませんが、H2は起動しています）

### 3-2. H2 Consoleにアクセス

ブラウザで以下のURLにアクセス：

```
http://localhost:8080/h2-console
```

**H2ログイン画面**が表示されます。

### 3-3. H2 Consoleでログイン

ログイン画面で以下を入力：

- **Saved Settings**: Generic H2 (Embedded)
- **Setting Name**: Generic H2 (Embedded)
- **Driver Class**: `org.h2.Driver`（自動入力済み）
- **JDBC URL**: `jdbc:h2:mem:testdb`
- **User Name**: `sa`
- **Password**: （空欄のまま）

「Connect」ボタンをクリックします。

### 3-4. H2 Consoleの画面確認

ログインに成功すると、以下の画面が表示されます：

- **左側**: テーブル一覧（現在は空）
- **右上**: SQLクエリ入力欄
- **右下**: 実行結果表示エリア

試しにSQLを実行してみましょう：

```sql
SELECT 1;
```

「Run」ボタン（または Ctrl+Enter）をクリック。

**結果**:
```
1
-
1
```

が表示されればOKです！

---

## 🚀 ステップ4: 簡単なテーブルを作成してみる

### 4-1. SQLで直接テーブルを作成

H2 Consoleで以下のSQLを実行：

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL
);
```

「Run」をクリック。

**結果**: 左側のテーブル一覧に「USERS」が表示されます。

### 4-2. データを挿入

```sql
INSERT INTO users (name, email) VALUES ('Taro', 'taro@example.com');
INSERT INTO users (name, email) VALUES ('Hanako', 'hanako@example.com');
INSERT INTO users (name, email) VALUES ('Jiro', 'jiro@example.com');
```

### 4-3. データを確認

```sql
SELECT * FROM users;
```

**結果**:
```
ID  NAME     EMAIL
1   Taro     taro@example.com
2   Hanako   hanako@example.com
3   Jiro     jiro@example.com
```

おめでとうございます！データベースの基本操作ができました！

---

## 💡 インメモリDBの特性を理解する

### 重要な注意点

**インメモリDB（`jdbc:h2:mem:testdb`）の特徴**:

- ✅ 高速
- ✅ セットアップ不要
- ❌ **アプリケーションを停止するとデータが消える**

### 実験: データの永続性を確認

1. アプリケーションを**停止**する
2. 再度**起動**する
3. H2 Consoleにアクセスして`SELECT * FROM users;`を実行

**結果**: テーブルもデータも消えています。

**これはバグではなく、インメモリDBの仕様です。**

### ファイルモードに変更（オプション）

データを永続化したい場合は、`application.yml`を以下のように変更：

```yaml
spring:
  datasource:
    # インメモリモード（データは揮発）
    # url: jdbc:h2:mem:testdb
    
    # ファイルモード（データを永続化）
    url: jdbc:h2:file:./data/testdb
```

`./data/testdb.mv.db`というファイルにデータが保存されます。

**Phase 2ではインメモリモードを使用します**（学習目的のため）。

---

## 🎨 チャレンジ課題

### チャレンジ 1: 別のテーブルを作成

`products`テーブルを作成して、データを挿入・取得してください。

**テーブル定義**:
- `id` (BIGINT, PRIMARY KEY, AUTO_INCREMENT)
- `name` (VARCHAR(100))
- `price` (INT)
- `category` (VARCHAR(50))

**ヒント**:
```sql
CREATE TABLE products (
    -- ここに定義
);

INSERT INTO products (name, price, category) VALUES (...);

SELECT * FROM products;
```

### チャレンジ 2: SQLの練習

以下のSQLを実行してみてください：

```sql
-- 特定のユーザーを検索
SELECT * FROM users WHERE name = 'Taro';

-- メールアドレスが'hanako'を含むユーザー
SELECT * FROM users WHERE email LIKE '%hanako%';

-- ユーザー数をカウント
SELECT COUNT(*) FROM users;

-- ユーザーを削除
DELETE FROM users WHERE id = 1;

-- 確認
SELECT * FROM users;
```

### チャレンジ 3: JOINを試す

`orders`テーブルを作成して、`users`テーブルとJOINしてみましょう。

```sql
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_name VARCHAR(100),
    amount INT
);

INSERT INTO orders (user_id, product_name, amount) VALUES (2, 'Laptop', 1200);
INSERT INTO orders (user_id, product_name, amount) VALUES (3, 'Mouse', 25);

-- JOINでユーザー名と注文を一緒に表示
SELECT u.name, o.product_name, o.amount
FROM users u
JOIN orders o ON u.id = o.user_id;
```

---

## 🐛 トラブルシューティング

### H2 Consoleにアクセスできない

**エラー**: ブラウザで404 Not Found

**原因**: H2 Consoleが有効化されていない

**解決策**:
1. `application.yml`で`spring.h2.console.enabled: true`を確認
2. アプリケーションを再起動
3. URLが`http://localhost:8080/h2-console`であることを確認

### "Database may be already in use"

**エラー**: `Database "testdb" may be already in use`

**原因**: 別のプロセスがH2を使用中、またはアプリケーションが多重起動

**解決策**:
1. すべてのアプリケーションを停止
2. IntelliJ IDEAで実行中のプロセスを確認（停止ボタンをクリック）
3. 再起動

### ログインできない

**症状**: H2 Consoleで「Wrong user name or password」

**解決策**:
- **JDBC URL**: `jdbc:h2:mem:testdb`（application.ymlと一致させる）
- **User Name**: `sa`
- **Password**: 空欄

### SQLが実行されない

**症状**: SQLを入力しても何も起こらない

**解決策**:
- 「Run」ボタンをクリック（または Ctrl+Enter / Cmd+Enter）
- SQL末尾にセミコロン（`;`）があるか確認

### テーブルが見つからない

**エラー**: `Table "USERS" not found`

**原因**: テーブルをまだ作成していない、またはアプリケーションを再起動してデータが消えた

**解決策**:
- `CREATE TABLE`文を再実行
- 次のステップ（Step 7）でエンティティクラスから自動生成する方法を学びます

---

## 📚 このステップで学んだこと

- ✅ H2データベースの特徴（インメモリDB）
- ✅ H2とSpring Data JPAの依存関係追加
- ✅ `application.yml`でデータソース設定
- ✅ H2 Consoleの有効化とブラウザでのアクセス
- ✅ SQLでのテーブル作成・データ挿入・検索
- ✅ インメモリDBとファイルDBの違い
- ✅ `ddl-auto`設定の理解

---

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
