# Step 12: MyBatisの基礎# Step 12: MySQLへの切り替え



## 🎯 このステップの目標## 🎯 このステップの目標



- MyBatisとは何かを理解する- H2からMySQLに切り替える方法を理解する

- JPAとMyBatisの違いを学ぶ- Docker ComposeでMySQLを起動する

- MyBatisでCRUD操作を実装する- Spring Bootのプロファイル機能を使って環境を切り替える

- Mapper XMLとMapperインターフェースの基本を理解する- 開発環境（H2）と本番環境（MySQL）を使い分ける



**所要時間**: 約1時間**所要時間**: 約1時間30分



------



## 📋 事前準備## 📋 事前準備



- Phase 2 (Step 6〜11) の完了- Step 11までのリレーションシップが理解できていること

- `User`エンティティとJPAでのCRUD実装- [PREPARE.md](PREPARE.md)のDocker環境が整っていること



---**Docker Desktopがインストールされていない場合**: [PREPARE.md](PREPARE.md)を参照してインストールしてください。



## 💡 MyBatisとは？---



### MyBatisの特徴## 💡 なぜMySQLに切り替えるのか？



**MyBatis** は、JavaでSQLを扱うためのORMフレームワークです。### H2データベースの特徴



**特徴**:**メリット**:

- ✅ **SQL直接記述**: SQLを完全にコントロールできる- ✅ 設定不要で簡単

- ✅ **パフォーマンス最適化**: 細かいチューニングが可能- ✅ 開発が高速

- ✅ **動的SQL**: 条件によってSQLを変更できる- ✅ 軽量

- ✅ **学習コスト**: SQLの知識があれば理解しやすい

**デメリット**:

### JPAとMyBatisの比較- ❌ インメモリなのでアプリ再起動でデータが消える

- ❌ 本番環境では使えない

| 比較項目 | JPA (Hibernate) | MyBatis |- ❌ 複数人での開発に不向き

|---------|----------------|---------|

| **アプローチ** | オブジェクト指向（ORM） | SQL指向 |### MySQLの特徴

| **SQL記述** | 自動生成（JPQL可） | 手動記述（必須） |

| **学習コスト** | やや高い | SQLができれば低い |**メリット**:

| **パフォーマンス** | 一般的な用途で十分 | 細かい最適化が可能 |- ✅ データが永続化される

| **向いている用途** | シンプルなCRUD、エンティティ中心 | 複雑なクエリ、レポート、統計 |- ✅ 本番環境で使われている

| **リレーション** | アノテーションで自動 | 手動でマッピング |- ✅ 複数人でのデータ共有が可能

- ✅ パフォーマンスが高い

### なぜ両方学ぶのか？

**デメリット**:

**実務では併用が一般的**:- ❌ セットアップが必要

- **JPA**: 基本的なCRUD、トランザクション管理- ❌ サーバーを起動する必要がある

- **MyBatis**: 複雑な検索、集計、レポート生成

### ベストプラクティス

---

開発環境と本番環境で異なるデータベースを使い分ける：

## 🚀 ステップ1: MyBatis依存関係の追加

| 環境 | データベース | 理由 |

### 1-1. pom.xmlに追加|------|--------------|------|

| **開発（ローカル）** | H2 | 高速、簡単 |

**ファイルパス**: `pom.xml`| **ステージング** | MySQL（Docker） | 本番に近い環境 |

| **本番（プロダクション）** | MySQL（クラウド） | 信頼性、パフォーマンス |

```xml

<dependencies>**このステップでは開発環境でMySQLを使えるようにします。**

    <!-- 既存の依存関係 -->

    <dependency>---

        <groupId>org.springframework.boot</groupId>

        <artifactId>spring-boot-starter-web</artifactId>## 🚀 ステップ1: Docker ComposeでMySQLを起動

    </dependency>

    <dependency>### 1-1. docker-compose.ymlの作成

        <groupId>org.springframework.boot</groupId>

        <artifactId>spring-boot-starter-data-jpa</artifactId>プロジェクトルートに`docker-compose.yml`を作成します。

    </dependency>

    <dependency>**ファイルパス**: `docker-compose.yml`（プロジェクトルート）

        <groupId>com.mysql</groupId>

        <artifactId>mysql-connector-j</artifactId>```yaml

        <scope>runtime</scope>version: '3.8'

    </dependency>

    <dependency>services:

        <groupId>org.projectlombok</groupId>  mysql:

        <artifactId>lombok</artifactId>    image: mysql:8.0

        <optional>true</optional>    container_name: springboot-mysql

    </dependency>    environment:

      MYSQL_ROOT_PASSWORD: root

    <!-- MyBatis -->      MYSQL_DATABASE: hellospringboot

    <dependency>      MYSQL_USER: springuser

        <groupId>org.mybatis.spring.boot</groupId>      MYSQL_PASSWORD: springpass

        <artifactId>mybatis-spring-boot-starter</artifactId>    ports:

        <version>3.0.3</version>      - "3306:3306"

    </dependency>    volumes:

      - mysql_data:/var/lib/mysql

    <!-- Test -->    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

    <dependency>

        <groupId>org.springframework.boot</groupId>volumes:

        <artifactId>spring-boot-starter-test</artifactId>  mysql_data:

        <scope>test</scope>```

    </dependency>

</dependencies>### 1-2. docker-compose.ymlの解説

```

#### `image: mysql:8.0`

### 1-2. Mavenの依存関係を更新- MySQL 8.0の公式Dockerイメージを使用



```bash#### `environment`

./mvnw clean install- `MYSQL_ROOT_PASSWORD`: rootユーザーのパスワード

```- `MYSQL_DATABASE`: 自動作成されるデータベース名

- `MYSQL_USER`: アプリケーション用ユーザー

---- `MYSQL_PASSWORD`: アプリケーション用パスワード



## 🚀 ステップ2: MyBatis設定#### `ports`

- `3306:3306`: ホスト側の3306ポートをコンテナ側の3306にマッピング

### 2-1. application.ymlにMyBatis設定を追加

#### `volumes`

**ファイルパス**: `src/main/resources/application.yml`- `mysql_data:/var/lib/mysql`: データを永続化（コンテナ削除後もデータが残る）



```yaml#### `command`

spring:- `--character-set-server=utf8mb4`: 文字コードをUTF-8に設定

  application:- `--collation-server=utf8mb4_unicode_ci`: 照合順序を設定

    name: hello-spring-boot

  ### 1-3. MySQLコンテナの起動

  datasource:

    url: jdbc:mysql://localhost:3306/hellospringboot?useSSL=false&serverTimezone=Asia/Tokyo&allowPublicKeyRetrieval=true```bash

    username: dbuserdocker-compose up -d

    password: dbpassword```

    driver-class-name: com.mysql.cj.jdbc.Driver

  **オプション**:

  jpa:- `-d`: バックグラウンドで起動（デタッチモード）

    hibernate:

      ddl-auto: update**出力例**:

    show-sql: true```

    properties:Creating network "hellospringboot_default" with the default driver

      hibernate:Creating volume "hellospringboot_mysql_data" with default driver

        format_sql: trueCreating springboot-mysql ... done

        dialect: org.hibernate.dialect.MySQLDialect```



# MyBatis設定### 1-4. 起動確認

mybatis:

  mapper-locations: classpath:mapper/**/*.xml```bash

  type-aliases-package: com.example.hellospringboot.modeldocker-compose ps

  configuration:```

    map-underscore-to-camel-case: true  # スネークケース⇔キャメルケース自動変換

    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl  # ログ出力**期待される出力**:

```

server:      Name                    Command             State           Ports

  port: 8080---------------------------------------------------------------------------------

springboot-mysql   docker-entrypoint.sh mysqld   Up      0.0.0.0:3306->3306/tcp

logging:```

  level:

    org.hibernate.SQL: DEBUG**ログ確認**:

    com.example.hellospringboot.mapper: DEBUG  # MyBatisのログ```bash

```docker-compose logs mysql

```

### 2-2. 設定の説明

MySQLが起動したことを示すログが表示されます：

| 項目 | 説明 |```

|------|------|mysqld: ready for connections. Version: '8.0.35'  socket: '/var/run/mysqld/mysqld.sock'

| `mapper-locations` | Mapper XMLファイルの配置場所 |```

| `type-aliases-package` | モデルクラスのパッケージ |

| `map-underscore-to-camel-case` | `user_name` → `userName` に自動変換 |---

| `log-impl` | SQL実行ログの出力設定 |

## 🚀 ステップ2: MySQL接続の設定

---

### 2-1. pom.xmlにMySQL依存関係を追加

## 🚀 ステップ3: Modelクラスの作成

`pom.xml`に以下を追加します。

### 3-1. Productモデルの作成

**ファイルパス**: `pom.xml`

**ファイルパス**: `src/main/java/com/example/hellospringboot/model/Product.java`

```xml

```java<dependencies>

package com.example.hellospringboot.model;    <!-- 既存の依存関係 -->

    

import lombok.AllArgsConstructor;    <!-- H2 Database (開発用) -->

import lombok.Builder;    <dependency>

import lombok.Data;        <groupId>com.h2database</groupId>

import lombok.NoArgsConstructor;        <artifactId>h2</artifactId>

        <scope>runtime</scope>

import java.math.BigDecimal;    </dependency>

import java.time.LocalDateTime;

    <!-- MySQL Database (本番用) -->

@Data    <dependency>

@Builder        <groupId>com.mysql</groupId>

@NoArgsConstructor        <artifactId>mysql-connector-j</artifactId>

@AllArgsConstructor        <scope>runtime</scope>

public class Product {    </dependency>

    private Long id;</dependencies>

    private String name;```

    private String description;

    private BigDecimal price;**IntelliJ IDEAで依存関係を更新**:

    private Integer stock;1. 右サイドバーの「Maven」タブを開く

    private LocalDateTime createdAt;2. 🔄（Reload All Maven Projects）をクリック

    private LocalDateTime updatedAt;

}### 2-2. プロファイル別の設定ファイル

```

Spring Bootはプロファイル機能で環境ごとに設定を切り替えられます。

**注意点**:

- `@Entity`は不要（MyBatisはPOJOを使用）**ファイル構成**:

- Lombokは使用可能```

- カラム名とプロパティ名は`map-underscore-to-camel-case`で自動変換src/main/resources/

├── application.yml          # 共通設定

### 3-2. テーブル作成SQL├── application-dev.yml      # 開発環境（H2）

└── application-prod.yml     # 本番環境（MySQL）

**ファイルパス**: `src/main/resources/schema.sql`（オプション）```



```sql### 2-3. application.ymlの更新

CREATE TABLE IF NOT EXISTS products (

    id BIGINT AUTO_INCREMENT PRIMARY KEY,**ファイルパス**: `src/main/resources/application.yml`

    name VARCHAR(100) NOT NULL,

    description TEXT,```yaml

    price DECIMAL(10, 2) NOT NULL,spring:

    stock INT NOT NULL DEFAULT 0,  application:

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,    name: hello-spring-boot

    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

);  # デフォルトプロファイル

```  profiles:

    active: dev

**application.ymlに追加**（起動時にSQLを実行する場合）:

  # JPA共通設定

```yaml  jpa:

spring:    show-sql: true

  sql:    properties:

    init:      hibernate:

      mode: always        format_sql: true

```    hibernate:

      ddl-auto: update

---

# ログレベル

## 🚀 ステップ4: Mapperインターフェースの作成logging:

  level:

### 4-1. ProductMapperインターフェース    org.hibernate.SQL: DEBUG

    org.hibernate.type.descriptor.sql.BasicBinder: TRACE

**ファイルパス**: `src/main/java/com/example/hellospringboot/mapper/ProductMapper.java````



```java### 2-4. application-dev.ymlの作成

package com.example.hellospringboot.mapper;

**ファイルパス**: `src/main/resources/application-dev.yml`

import com.example.hellospringboot.model.Product;

import org.apache.ibatis.annotations.*;```yaml

# 開発環境設定（H2）

import java.util.List;

spring:

@Mapper  datasource:

public interface ProductMapper {    url: jdbc:h2:mem:testdb

    driver-class-name: org.h2.Driver

    /**    username: sa

     * 全商品取得    password:

     */

    @Select("SELECT * FROM products ORDER BY id DESC")  h2:

    List<Product> findAll();    console:

      enabled: true

    /**      path: /h2-console

     * ID指定で商品取得

     */  jpa:

    @Select("SELECT * FROM products WHERE id = #{id}")    database-platform: org.hibernate.dialect.H2Dialect

    Product findById(Long id);```



    /**### 2-5. application-prod.ymlの作成

     * 商品登録

     */**ファイルパス**: `src/main/resources/application-prod.yml`

    @Insert("INSERT INTO products (name, description, price, stock) " +

            "VALUES (#{name}, #{description}, #{price}, #{stock})")```yaml

    @Options(useGeneratedKeys = true, keyProperty = "id")# 本番環境設定（MySQL）

    int insert(Product product);

spring:

    /**  datasource:

     * 商品更新    url: jdbc:mysql://localhost:3306/hellospringboot?useSSL=false&serverTimezone=Asia/Tokyo

     */    driver-class-name: com.mysql.cj.jdbc.Driver

    @Update("UPDATE products SET name = #{name}, description = #{description}, " +    username: springuser

            "price = #{price}, stock = #{stock} WHERE id = #{id}")    password: springpass

    int update(Product product);

  jpa:

    /**    database-platform: org.hibernate.dialect.MySQLDialect

     * 商品削除    hibernate:

     */      ddl-auto: update

    @Delete("DELETE FROM products WHERE id = #{id}")```

    int delete(Long id);

### 2-6. 設定の解説

    /**

     * 名前で検索（部分一致）#### `spring.profiles.active`

     */デフォルトで使うプロファイルを指定

    @Select("SELECT * FROM products WHERE name LIKE CONCAT('%', #{keyword}, '%')")```yaml

    List<Product> searchByName(String keyword);spring:

}  profiles:

```    active: dev  # devプロファイルを使用

```

### 4-2. アノテーションの説明

#### `spring.datasource.url`

| アノテーション | 説明 |データベース接続URL

|-------------|------|

| `@Mapper` | MyBatisのMapperとして認識 |**H2**:

| `@Select` | SELECT文を記述 |```yaml

| `@Insert` | INSERT文を記述 |url: jdbc:h2:mem:testdb

| `@Update` | UPDATE文を記述 |```

| `@Delete` | DELETE文を記述 |

| `@Options` | 自動生成されたIDを取得 |**MySQL**:

| `#{変数名}` | パラメータのバインド |```yaml

url: jdbc:mysql://localhost:3306/hellospringboot?useSSL=false&serverTimezone=Asia/Tokyo

---```

- `localhost:3306`: MySQLサーバーのアドレスとポート

## 🚀 ステップ5: Serviceクラスの作成- `hellospringboot`: データベース名

- `useSSL=false`: SSL接続を無効化（開発環境）

### 5-1. ProductService- `serverTimezone=Asia/Tokyo`: タイムゾーン設定



**ファイルパス**: `src/main/java/com/example/hellospringboot/service/ProductService.java`#### `spring.jpa.database-platform`

使用するSQLダイアレクト（方言）

```java

package com.example.hellospringboot.service;**H2**:

```yaml

import com.example.hellospringboot.mapper.ProductMapper;database-platform: org.hibernate.dialect.H2Dialect

import com.example.hellospringboot.model.Product;```

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;**MySQL**:

import org.springframework.transaction.annotation.Transactional;```yaml

database-platform: org.hibernate.dialect.MySQLDialect

import java.util.List;```



@Service#### `spring.jpa.hibernate.ddl-auto`

@RequiredArgsConstructorスキーマの自動生成設定

@Transactional(readOnly = true)

public class ProductService {| 値 | 説明 |

|----|------|

    private final ProductMapper productMapper;| `none` | 何もしない |

| `validate` | スキーマを検証するのみ |

    public List<Product> getAllProducts() {| `update` | スキーマを更新（推奨：開発環境） |

        return productMapper.findAll();| `create` | 起動時にスキーマを作成（既存データ削除） |

    }| `create-drop` | 起動時に作成、終了時に削除 |



    public Product getProductById(Long id) {---

        return productMapper.findById(id);

    }## ✅ ステップ3: MySQLで動作確認



    @Transactional### 3-1. プロファイルの切り替え

    public Product createProduct(Product product) {

        productMapper.insert(product);#### 方法1: application.ymlで指定

        return product;  // idが自動設定される

    }```yaml

spring:

    @Transactional  profiles:

    public Product updateProduct(Long id, Product product) {    active: prod  # prodプロファイルを使用

        Product existing = productMapper.findById(id);```

        if (existing == null) {

            throw new IllegalArgumentException("Product not found: " + id);#### 方法2: 環境変数で指定

        }

        ```bash

        product.setId(id);export SPRING_PROFILES_ACTIVE=prod

        productMapper.update(product);```

        return productMapper.findById(id);

    }#### 方法3: 起動時に指定



    @Transactional```bash

    public void deleteProduct(Long id) {./mvnw spring-boot:run -Dspring-boot.run.profiles=prod

        productMapper.delete(id);```

    }

#### 方法4: IntelliJ IDEAで指定

    public List<Product> searchProducts(String keyword) {

        return productMapper.searchByName(keyword);1. 右上の実行構成（Run Configuration）をクリック

    }2. 「Edit Configurations...」を選択

}3. 「Active profiles」に`prod`を入力

```4. 「OK」をクリック

5. アプリケーションを起動

---

### 3-2. アプリケーション起動（MySQLモード）

## 🚀 ステップ6: Controllerクラスの作成

**IntelliJ IDEAで**:

### 6-1. ProductController1. Active profilesを`prod`に設定

2. ▶️（Run）をクリック

**ファイルパス**: `src/main/java/com/example/hellospringboot/controller/ProductController.java`

**コマンドラインで**:

```java```bash

package com.example.hellospringboot.controller;./mvnw spring-boot:run -Dspring-boot.run.profiles=prod

```

import com.example.hellospringboot.model.Product;

import com.example.hellospringboot.service.ProductService;**起動ログで確認**:

import lombok.RequiredArgsConstructor;```

import org.springframework.http.HttpStatus;HikariPool-1 - Starting...

import org.springframework.http.ResponseEntity;HikariPool-1 - Added connection com.mysql.cj.jdbc.ConnectionImpl@...

import org.springframework.web.bind.annotation.*;HikariPool-1 - Start completed.

Hibernate: create table users (...)

import java.util.List;Hibernate: create table posts (...)

```

@RestController

@RequestMapping("/api/products")### 3-3. データ投入

@RequiredArgsConstructor

public class ProductController {**ユーザー作成**:

```bash

    private final ProductService productService;curl -X POST http://localhost:8080/api/users \

  -H "Content-Type: application/json" \

    @GetMapping  -d '{

    public ResponseEntity<List<Product>> getAllProducts() {    "name": "Taro Yamada",

        List<Product> products = productService.getAllProducts();    "email": "taro@example.com",

        return ResponseEntity.ok(products);    "age": 30

    }  }'

```

    @GetMapping("/{id}")

    public ResponseEntity<Product> getProduct(@PathVariable Long id) {**投稿作成**:

        Product product = productService.getProductById(id);```bash

        if (product == null) {curl -X POST http://localhost:8080/api/posts \

            return ResponseEntity.notFound().build();  -H "Content-Type: application/json" \

        }  -d '{

        return ResponseEntity.ok(product);    "userId": 1,

    }    "title": "First Post on MySQL",

    "content": "Now using MySQL database!"

    @PostMapping  }'

    public ResponseEntity<Product> createProduct(@RequestBody Product product) {```

        Product created = productService.createProduct(product);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);**データ確認**:

    }```bash

curl http://localhost:8080/api/users

    @PutMapping("/{id}")curl http://localhost:8080/api/posts

    public ResponseEntity<Product> updateProduct(```

            @PathVariable Long id,

            @RequestBody Product product) {### 3-4. MySQLコンテナに直接接続して確認

        Product updated = productService.updateProduct(id, product);

        return ResponseEntity.ok(updated);```bash

    }docker exec -it springboot-mysql mysql -u springuser -p

# パスワード: springpass

    @DeleteMapping("/{id}")```

    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {

        productService.deleteProduct(id);**SQL実行**:

        return ResponseEntity.noContent().build();```sql

    }USE hellospringboot;



    @GetMapping("/search")SHOW TABLES;

    public ResponseEntity<List<Product>> searchProducts(@RequestParam String keyword) {

        List<Product> products = productService.searchProducts(keyword);SELECT * FROM users;

        return ResponseEntity.ok(products);SELECT * FROM posts;

    }

}-- JOINでデータ確認

```SELECT p.id, p.title, u.name as user_name 

FROM posts p 

---JOIN users u ON p.user_id = u.id;



## ✅ 動作確認EXIT;

```

### アプリケーションの起動

---

```bash

./mvnw spring-boot:run## 🚀 ステップ4: データ永続化の確認

```

### 4-1. アプリケーションの再起動

### 1. 商品登録

1. アプリケーションを停止（Ctrl+C）

```bash2. 再度起動

curl -X POST http://localhost:8080/api/products \

  -H "Content-Type: application/json" \```bash

  -d '{./mvnw spring-boot:run -Dspring-boot.run.profiles=prod

    "name": "ノートパソコン",```

    "description": "高性能なビジネス向けノートPC",

    "price": 129800,### 4-2. データ確認

    "stock": 50

  }'```bash

```curl http://localhost:8080/api/users

curl http://localhost:8080/api/posts

**レスポンス例**:```

```json

{**結果**: データが残っている！（MySQLはデータを永続化）

  "id": 1,

  "name": "ノートパソコン",### 4-3. H2との比較

  "description": "高性能なビジネス向けノートPC",

  "price": 129800,**H2の場合（devプロファイル）**:

  "stock": 50,```bash

  "createdAt": "2025-10-29T10:00:00",# H2で起動

  "updatedAt": "2025-10-29T10:00:00"./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

}

```# データ投入

curl -X POST http://localhost:8080/api/users ...

### 2. 全商品取得

# アプリ再起動

```bash# データが消える！

curl http://localhost:8080/api/products```

```

---

### 3. 商品検索

## 🚀 ステップ5: Docker Composeの管理

```bash

curl http://localhost:8080/api/products/search?keyword=ノート### 5-1. 基本コマンド

```

#### 起動

### 4. 商品更新```bash

docker-compose up -d

```bash```

curl -X PUT http://localhost:8080/api/products/1 \

  -H "Content-Type: application/json" \#### 停止

  -d '{```bash

    "name": "ノートパソコン（セール中）",docker-compose stop

    "description": "高性能なビジネス向けノートPC - 特別価格",```

    "price": 99800,

    "stock": 45#### 停止して削除

  }'```bash

```docker-compose down

```

### 5. 商品削除

#### ログ確認

```bash```bash

curl -X DELETE http://localhost:8080/api/products/1docker-compose logs mysql

```docker-compose logs -f mysql  # リアルタイム表示

```

---

#### コンテナの状態確認

## 💡 XMLベースのMapper（オプション）```bash

docker-compose ps

アノテーションの代わりにXMLでSQLを記述することもできます。```



### Mapper XMLファイルの作成### 5-2. データの削除



**ファイルパス**: `src/main/resources/mapper/ProductMapper.xml`**警告**: ボリュームを削除するとすべてのデータが消えます！



```xml```bash

<?xml version="1.0" encoding="UTF-8" ?>docker-compose down -v

<!DOCTYPE mapper```

        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"

        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">オプション:

- `-v`: ボリュームも削除

<mapper namespace="com.example.hellospringboot.mapper.ProductMapper">

### 5-3. 開発時の推奨ワークフロー

    <!-- 全商品取得 -->

    <select id="findAll" resultType="Product">```bash

        SELECT * FROM products ORDER BY id DESC# 朝（作業開始）

    </select>docker-compose up -d



    <!-- ID指定で商品取得 --># 開発作業

    <select id="findById" resultType="Product">./mvnw spring-boot:run -Dspring-boot.run.profiles=prod

        SELECT * FROM products WHERE id = #{id}

    </select># 夜（作業終了）

docker-compose stop

    <!-- 商品登録 -->```

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">

        INSERT INTO products (name, description, price, stock)**PCをシャットダウンする場合**:

        VALUES (#{name}, #{description}, #{price}, #{stock})```bash

    </insert>docker-compose down  # コンテナ削除（ボリュームは保持）

```

    <!-- 商品更新 -->

    <update id="update">---

        UPDATE products

        SET name = #{name},## 🎨 チャレンジ課題

            description = #{description},

            price = #{price},### チャレンジ 1: 環境変数での設定

            stock = #{stock}

        WHERE id = #{id}`application-prod.yml`を環境変数で上書きできるようにしてください。

    </update>

**ヒント**:

    <!-- 商品削除 -->```yaml

    <delete id="delete">spring:

        DELETE FROM products WHERE id = #{id}  datasource:

    </delete>    url: ${DATABASE_URL:jdbc:mysql://localhost:3306/hellospringboot}

    username: ${DATABASE_USERNAME:springuser}

    <!-- 名前で検索 -->    password: ${DATABASE_PASSWORD:springpass}

    <select id="searchByName" resultType="Product">```

        SELECT * FROM products

        WHERE name LIKE CONCAT('%', #{keyword}, '%')### チャレンジ 2: テスト用プロファイル

    </select>

テスト用のプロファイル（`application-test.yml`）を作成してください。

</mapper>

```**要件**:

- H2を使用

### Mapperインターフェース（XML使用時）- テストごとにデータベースをクリーンにする（`ddl-auto: create-drop`）



```java### チャレンジ 3: PostgreSQLへの対応

@Mapper

public interface ProductMapper {MySQLの代わりにPostgreSQLを使えるようにしてください。

    List<Product> findAll();

    Product findById(Long id);**ヒント**:

    int insert(Product product);```yaml

    int update(Product product);# docker-compose.yml

    int delete(Long id);postgres:

    List<Product> searchByName(String keyword);  image: postgres:15

}  environment:

```    POSTGRES_DB: hellospringboot

    POSTGRES_USER: springuser

**メリット**:    POSTGRES_PASSWORD: springpass

- 複雑なSQLが読みやすい```

- SQLとJavaコードを分離

- 動的SQLが書きやすい```xml

<!-- pom.xml -->

---<dependency>

    <groupId>org.postgresql</groupId>

## 🎨 チャレンジ課題    <artifactId>postgresql</artifactId>

    <scope>runtime</scope>

### チャレンジ 1: 価格範囲で検索</dependency>

```

指定した価格範囲の商品を検索するメソッドを追加してください。

---

```java

@Select("SELECT * FROM products WHERE price BETWEEN #{minPrice} AND #{maxPrice}")## 🐛 トラブルシューティング

List<Product> findByPriceRange(@Param("minPrice") BigDecimal minPrice, 

                                @Param("maxPrice") BigDecimal maxPrice);### "Communications link failure"

```

**エラー**:

### チャレンジ 2: 在庫切れ商品の取得```

com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure

在庫が0の商品を取得するメソッドを実装してください。```



### チャレンジ 3: ページネーション**原因**: MySQLコンテナが起動していない



ページネーション機能を追加してください（LIMIT, OFFSET使用）。**解決策**:

```bash

---# コンテナ確認

docker-compose ps

## 📚 このステップで学んだこと

# 起動

- ✅ MyBatisとJPAの違いdocker-compose up -d

- ✅ MyBatis Spring Boot Starterの導入

- ✅ `@Mapper`と`@Select`, `@Insert`などのアノテーション# ログ確認

- ✅ MapperインターフェースでのCRUD操作docker-compose logs mysql

- ✅ MyBatisのトランザクション管理```

- ✅ アノテーションベースとXMLベースの2つの方式

### "Access denied for user"

---

**エラー**:

## 🔄 Gitへのコミット```

Access denied for user 'springuser'@'localhost' (using password: YES)

```bash```

git add .

git commit -m "Phase 2: STEP_12完了（MyBatisの基礎）"**原因**: ユーザー名またはパスワードが間違っている

git push origin main

```**解決策**: `application-prod.yml`と`docker-compose.yml`の設定を確認



---```yaml

# application-prod.yml

## ➡️ 次のステップusername: springuser

password: springpass

次は[Step 13: MyBatisで複雑なクエリ](STEP_13.md)へ進みましょう！

# docker-compose.yml

動的SQLやJOIN、ResultMapなど、MyBatisの高度な機能を学びます。MYSQL_USER: springuser

MYSQL_PASSWORD: springpass

---```



お疲れさまでした！ 🎉### "Table doesn't exist"


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
