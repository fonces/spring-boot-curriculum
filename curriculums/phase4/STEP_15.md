# Step 15: レイヤー化アーキテクチャ

## 🎯 このステップの目標

- レイヤー化アーキテクチャの目的と利点を理解できる
- Controller / Service / Repository の責務を明確に区別できる
- 既存のコードを適切なレイヤーに分離してリファクタリングできる
- パッケージ構成のベストプラクティスを習得できる
- 保守性・テスタビリティの高いアプリケーション設計ができる

**所要時間**: 約45分

---

## 📋 事前準備

- [Phase 3のすべてのステップ](../phase3/STEP_14.md)が完了していること
- User, Product, Category, Order などのエンティティが実装済みであること
- JPAとMyBatisの基本的な使い方を理解していること
- `@Service` アノテーションを見たことがあること（Phase 2-3で使用済み）

---

## 🏗️ レイヤー化アーキテクチャとは

### なぜレイヤー化が必要なのか

Phase 1から3まで、私たちは以下のようなパッケージ構成でコードを書いてきました：

```
com.example.hellospringboot/
├── HelloSpringBootApplication.java
├── User.java
├── UserController.java
├── UserService.java
├── UserMapper.java
├── Product.java
├── ProductController.java
├── ProductService.java
├── ProductRepository.java
├── Category.java
├── CategoryController.java
├── CategoryService.java
├── CategoryRepository.java
└── ... (他のクラス)
```

**問題点**:
- すべてのクラスが1つのパッケージに混在している
- クラスが増えると見通しが悪くなる
- どのクラスがどの責務を持つのかが不明確
- 新しいメンバーが参加した時に理解しにくい
- テストを書く時にどこから手をつければいいか分かりにくい

### レイヤー化の基本概念

**レイヤー化アーキテクチャ（Layered Architecture）** は、アプリケーションを責務ごとに分離し、階層構造で整理する設計手法です。

```
┌─────────────────────────────────────┐
│    Presentation Layer (表示層)      │  ← @RestController, @Controller
│  - HTTPリクエスト/レスポンス処理     │     UserController, ProductController
│  - パラメータ検証                    │
│  - JSONへの変換                     │
└─────────────────────────────────────┘
            ↓ 依存
┌─────────────────────────────────────┐
│    Business Logic Layer (業務層)    │  ← @Service
│  - ビジネスルール                   │     UserService, ProductService
│  - トランザクション管理              │
│  - 複数リポジトリの組み合わせ        │
└─────────────────────────────────────┘
            ↓ 依存
┌─────────────────────────────────────┐
│   Data Access Layer (データ層)      │  ← @Repository, Mapper
│  - データベースアクセス              │     UserRepository, UserMapper
│  - SQL実行                         │     ProductRepository
│  - エンティティのCRUD               │
└─────────────────────────────────────┘
            ↓ 依存
┌─────────────────────────────────────┐
│         Database (データベース)      │
│  MySQL, PostgreSQLなど              │
└─────────────────────────────────────┘
```

**重要な原則**:
- **上位レイヤーは下位レイヤーに依存できる**（Controller → Service → Repository）
- **下位レイヤーは上位レイヤーに依存してはいけない**（Repository → Service は ❌）
- **同じレイヤー内での依存は最小限に**

---

## 📁 Phase 4以降の推奨パッケージ構成

Phase 4からは、以下のようなディレクトリ構造でコードを整理します：

```
com.example.hellospringboot/
├── HelloSpringBootApplication.java    # メインクラス
│
├── controllers/                       # 🎯 Presentation Layer
│   ├── UserController.java
│   ├── ProductController.java
│   ├── CategoryController.java
│   └── ReportController.java
│
├── services/                          # 🧩 Business Logic Layer
│   ├── UserService.java
│   ├── ProductService.java
│   ├── CategoryService.java
│   └── ReportService.java
│
├── repositories/                      # 🗄️ Data Access Layer (JPA)
│   ├── UserRepository.java
│   ├── ProductRepository.java
│   ├── CategoryRepository.java
│   └── OrderRepository.java
│
├── mappers/                           # 🗄️ Data Access Layer (MyBatis)
│   ├── UserMapper.java
│   └── ReportMapper.java
│
├── entities/                          # 📦 Domain Model
│   ├── User.java
│   ├── Product.java
│   ├── Category.java
│   ├── Order.java
│   └── Post.java
│
└── dtos/                              # 📄 Data Transfer Objects (後のステップで作成)
    ├── UserCreateRequest.java
    ├── UserResponse.java
    └── ProductResponse.java
```

---

## 🔍 各レイヤーの責務

### 1. Controller（コントローラー層）

**責務**:
- HTTPリクエストを受け取る
- リクエストパラメータのバリデーション
- Serviceを呼び出す
- HTTPレスポンスを返す（JSONへの変換など）

**やってはいけないこと** ❌:
- ビジネスロジックを書く
- データベースに直接アクセスする
- 複雑な計算処理を行う

**例**:
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        User user = userService.findById(id);  // ← Serviceを呼ぶだけ
        return ResponseEntity.ok(user);
    }
}
```

---

### 2. Service（サービス層）

**責務**:
- ビジネスロジックの実装
- トランザクション管理（`@Transactional`）
- 複数のRepositoryを組み合わせた処理
- ドメインルールの実施

**やってはいけないこと** ❌:
- HTTPリクエスト/レスポンスに直接触る
- SQLを直接書く（それはRepositoryの仕事）

**例**:
```java
@Service
public class UserService {
    private final UserRepository userRepository;
    
    @Transactional
    public User createUser(User user) {
        // ビジネスルール: メールアドレスの重複チェック
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        return userRepository.save(user);
    }
}
```

---

### 3. Repository / Mapper（データアクセス層）

**責務**:
- データベースへのアクセス
- SQLの実行（MyBatisの場合）
- エンティティのCRUD操作

**やってはいけないこと** ❌:
- ビジネスロジックを含める
- トランザクション管理（それはServiceの仕事）

**例（JPA）**:
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);  // データアクセスのみ
}
```

**例（MyBatis）**:
```java
@Mapper
public interface UserMapper {
    User findById(Long id);  // データアクセスのみ
}
```

---

## 🚀 ステップ1: パッケージ構造のリファクタリング

Phase 1-3で作成したコードを、レイヤー化アーキテクチャに従って整理します。

### 1-1. ディレクトリの作成

まず、必要なパッケージ（ディレクトリ）を作成します。

```bash
cd /path/to/workspace/hello-spring-boot/src/main/java/com/example/hellospringboot
mkdir controllers services repositories mappers entities
```

### 1-2. ファイルの移動とパッケージ変更

次に、既存のファイルを適切なディレクトリに移動し、パッケージ宣言を更新します。

#### Controllerの移動

以下のファイルを `controllers/` ディレクトリに移動します：
- `UserController.java`
- `ProductController.java`
- `CategoryController.java`
- `ReportController.java`

**移動前**: `src/main/java/com/example/hellospringboot/UserController.java`

```java
package com.example.hellospringboot;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    // ... コード
}
```

**移動後**: `src/main/java/com/example/hellospringboot/controllers/UserController.java`

```java
package com.example.hellospringboot.controllers;  // パッケージを変更

import com.example.hellospringboot.entities.User;  // エンティティをインポート
import com.example.hellospringboot.services.UserService;  // サービスをインポート
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @GetMapping
    public List<User> getAllUsers() {
        return userService.findAll();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }
    
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User created = userService.create(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @RequestBody User userDetails) {
        try {
            User updated = userService.update(id, userDetails);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            userService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
```

**同様に** `ProductController.java`, `CategoryController.java`, `ReportController.java` も移動し、パッケージ宣言とインポート文を更新してください。

---

#### Serviceの移動

以下のファイルを `services/` ディレクトリに移動します：
- `UserService.java`
- `ProductService.java`
- `CategoryService.java`
- `ReportService.java`

**移動前**: `src/main/java/com/example/hellospringboot/UserService.java`

**移動後**: `src/main/java/com/example/hellospringboot/services/UserService.java`

```java
package com.example.hellospringboot.services;  // パッケージを変更

import com.example.hellospringboot.entities.User;  // エンティティをインポート
import com.example.hellospringboot.mappers.UserMapper;  // Mapperをインポート
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserMapper userMapper;
    
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userMapper.findById(id);
    }
    
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userMapper.findAll();
    }
    
    @Transactional
    public User create(User user) {
        userMapper.insert(user);
        return user;
    }
    
    @Transactional
    public User update(Long id, User userDetails) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        
        user.setName(userDetails.getName());
        user.setEmail(userDetails.getEmail());
        user.setAge(userDetails.getAge());
        
        userMapper.update(user);
        return user;
    }
    
    @Transactional
    public void delete(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        userMapper.delete(id);
    }
}
```

**ProductService** も同様に移動します：

`src/main/java/com/example/hellospringboot/services/ProductService.java`:

```java
package com.example.hellospringboot.services;

import com.example.hellospringboot.entities.Category;
import com.example.hellospringboot.entities.Product;
import com.example.hellospringboot.repositories.CategoryRepository;
import com.example.hellospringboot.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    
    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }
    
    @Transactional
    public Product save(Product product) {
        return productRepository.save(product);
    }
    
    @Transactional
    public Product update(Long id, Product productDetails) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setStock(productDetails.getStock());
        
        return productRepository.save(product);
    }
    
    @Transactional
    public void deleteById(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }
    
    @Transactional
    public Product saveWithCategory(Product product, Long categoryId) {
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            product.setCategory(category);
        }
        return productRepository.save(product);
    }
}
```

**CategoryService** も同様に移動します：

`src/main/java/com/example/hellospringboot/services/CategoryService.java`:

```java
package com.example.hellospringboot.services;

import com.example.hellospringboot.entities.Category;
import com.example.hellospringboot.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryService {
    
    private final CategoryRepository categoryRepository;
    
    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }
    
    @Transactional
    public Category save(Category category) {
        return categoryRepository.save(category);
    }
    
    @Transactional
    public Category update(Long id, Category categoryDetails) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        category.setName(categoryDetails.getName());
        category.setDescription(categoryDetails.getDescription());
        
        return categoryRepository.save(category);
    }
    
    @Transactional
    public void deleteById(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("Category not found");
        }
        categoryRepository.deleteById(id);
    }
}
```

**ReportService** も同様に移動します：

`src/main/java/com/example/hellospringboot/services/ReportService.java`:

```java
package com.example.hellospringboot.services;

import com.example.hellospringboot.entities.PurchaseReport;
import com.example.hellospringboot.mappers.ReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {
    
    private final ReportMapper reportMapper;
    
    @Transactional(readOnly = true)
    public List<PurchaseReport> getUserPurchaseReport(Long userId) {
        return reportMapper.findUserPurchaseReport(userId);
    }
}
```

---

#### Repositoryの移動

以下のファイルを `repositories/` ディレクトリに移動します：
- `UserRepository.java`（JPAの場合のみ。今回はMyBatisを使用しているため該当なし）
- `ProductRepository.java`
- `CategoryRepository.java`
- `OrderRepository.java`

**移動前**: `src/main/java/com/example/hellospringboot/ProductRepository.java`

**移動後**: `src/main/java/com/example/hellospringboot/repositories/ProductRepository.java`

```java
package com.example.hellospringboot.repositories;  // パッケージを変更

import com.example.hellospringboot.entities.Product;  // エンティティをインポート
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}
```

**CategoryRepository** も同様に移動します：

`src/main/java/com/example/hellospringboot/repositories/CategoryRepository.java`:

```java
package com.example.hellospringboot.repositories;

import com.example.hellospringboot.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
}
```

**OrderRepository** も同様に移動します：

`src/main/java/com/example/hellospringboot/repositories/OrderRepository.java`:

```java
package com.example.hellospringboot.repositories;

import com.example.hellospringboot.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
}
```

---

#### Mapper（MyBatis）の移動

以下のファイルを `mappers/` ディレクトリに移動します：
- `UserMapper.java`
- `ReportMapper.java`

**移動前**: `src/main/java/com/example/hellospringboot/UserMapper.java`

**移動後**: `src/main/java/com/example/hellospringboot/mappers/UserMapper.java`

```java
package com.example.hellospringboot.mappers;  // パッケージを変更

import com.example.hellospringboot.entities.User;  // エンティティをインポート
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {
    
    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(Long id);
    
    @Select("SELECT * FROM users")
    List<User> findAll();
    
    @Insert("INSERT INTO users (name, email, age, created_at, updated_at) " +
            "VALUES (#{name}, #{email}, #{age}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);
    
    @Update("UPDATE users SET name = #{name}, email = #{email}, age = #{age}, " +
            "updated_at = NOW() WHERE id = #{id}")
    void update(User user);
    
    @Delete("DELETE FROM users WHERE id = #{id}")
    void delete(Long id);
}
```

**ReportMapper** も同様に移動します：

`src/main/java/com/example/hellospringboot/mappers/ReportMapper.java`:

```java
package com.example.hellospringboot.mappers;

import com.example.hellospringboot.entities.PurchaseReport;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ReportMapper {
    
    List<PurchaseReport> findUserPurchaseReport(Long userId);
}
```

---

#### Entityの移動

以下のファイルを `entities/` ディレクトリに移動します：
- `User.java`
- `Product.java`
- `Category.java`
- `Order.java`
- `Post.java`
- `PurchaseReport.java`（DTOとして扱う場合もありますが、今回はエンティティとして配置）

**移動前**: `src/main/java/com/example/hellospringboot/User.java`

**移動後**: `src/main/java/com/example/hellospringboot/entities/User.java`

```java
package com.example.hellospringboot.entities;  // パッケージを変更

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    private Long id;
    private String name;
    private String email;
    private Integer age;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**Product** も同様に移動します：

`src/main/java/com/example/hellospringboot/entities/Product.java`:

```java
package com.example.hellospringboot.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
    
    @Column(nullable = false)
    private Integer stock = 0;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @JsonBackReference
    private Category category;
    
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

**Category** も同様に移動します：

`src/main/java/com/example/hellospringboot/entities/Category.java`:

```java
package com.example.hellospringboot.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Product> products = new ArrayList<>();
    
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

**Order** も同様に移動します：

`src/main/java/com/example/hellospringboot/entities/Order.java`:

```java
package com.example.hellospringboot.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

**Post** も同様に移動します：

`src/main/java/com/example/hellospringboot/entities/Post.java`:

```java
package com.example.hellospringboot.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
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

**PurchaseReport** も同様に移動します：

`src/main/java/com/example/hellospringboot/entities/PurchaseReport.java`:

```java
package com.example.hellospringboot.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseReport {
    private Long userId;
    private String userName;
    private Long productId;
    private String productName;
    private Integer quantity;
    private Integer totalPrice;
}
```

---

### 1-3. ProductController と CategoryController の更新

`ProductController` と `CategoryController` も同様に移動し、インポート文を更新します。

`src/main/java/com/example/hellospringboot/controllers/ProductController.java`:

```java
package com.example.hellospringboot.controllers;

import com.example.hellospringboot.entities.Product;
import com.example.hellospringboot.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    
    private final ProductService productService;
    
    @GetMapping
    public List<Product> getAllProducts() {
        return productService.findAll();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<Product> createProduct(
            @RequestBody Product product,
            @RequestParam(required = false) Long categoryId) {
        Product saved = productService.saveWithCategory(product, categoryId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @RequestBody Product productDetails) {
        try {
            Product updated = productService.update(id, productDetails);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
```

`src/main/java/com/example/hellospringboot/controllers/CategoryController.java`:

```java
package com.example.hellospringboot.controllers;

import com.example.hellospringboot.entities.Category;
import com.example.hellospringboot.services.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    
    private final CategoryService categoryService;
    
    @GetMapping
    public List<Category> getAllCategories() {
        return categoryService.findAll();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Long id) {
        return categoryService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        Category saved = categoryService.save(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(
            @PathVariable Long id,
            @RequestBody Category categoryDetails) {
        try {
            Category updated = categoryService.update(id, categoryDetails);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        try {
            categoryService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
```

`src/main/java/com/example/hellospringboot/controllers/ReportController.java`:

```java
package com.example.hellospringboot.controllers;

import com.example.hellospringboot.entities.PurchaseReport;
import com.example.hellospringboot.services.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    
    private final ReportService reportService;
    
    @GetMapping("/orders/user/{userId}")
    public List<PurchaseReport> getUserPurchaseReport(@PathVariable Long userId) {
        return reportService.getUserPurchaseReport(userId);
    }
}
```

---

### 1-4. MyBatis Mapper XMLファイルの名前空間更新

MyBatisのMapper XMLファイルでは、`namespace` 属性がMapperインターフェースのパッケージを参照しています。
パッケージを変更したため、XMLファイルも更新する必要があります。

`src/main/resources/mapper/UserMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<!-- namespace を変更 -->
<mapper namespace="com.example.hellospringboot.mappers.UserMapper">
    
    <!-- 既存のマッピング定義はそのまま -->
    
</mapper>
```

`src/main/resources/mapper/ReportMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<!-- namespace を変更 -->
<mapper namespace="com.example.hellospringboot.mappers.ReportMapper">
    
    <resultMap id="PurchaseReportResult" type="com.example.hellospringboot.entities.PurchaseReport">
        <result property="userId" column="user_id"/>
        <result property="userName" column="user_name"/>
        <result property="productId" column="product_id"/>
        <result property="productName" column="product_name"/>
        <result property="quantity" column="quantity"/>
        <result property="totalPrice" column="total_price"/>
    </resultMap>
    
    <select id="findUserPurchaseReport" resultMap="PurchaseReportResult">
        SELECT
            u.id AS user_id,
            u.name AS user_name,
            p.id AS product_id,
            p.name AS product_name,
            o.quantity,
            o.total_price
        FROM orders o
        INNER JOIN users u ON o.user_id = u.id
        INNER JOIN products p ON o.product_id = p.id
        WHERE u.id = #{userId}
        ORDER BY o.created_at DESC
    </select>
</mapper>
```

---

### 1-5. 不要なファイルの削除

元のパッケージ直下に残っている古いファイルを削除します。

```bash
cd /path/to/workspace/hello-spring-boot/src/main/java/com/example/hellospringboot

# 移動済みのファイルを削除
rm UserController.java ProductController.java CategoryController.java ReportController.java
rm UserService.java ProductService.java CategoryService.java ReportService.java
rm ProductRepository.java CategoryRepository.java OrderRepository.java
rm UserMapper.java ReportMapper.java
rm User.java Product.java Category.java Order.java Post.java PurchaseReport.java
```

**注意**: `HelloController.java` と `HelloSpringBootApplication.java` は残しておきます。

---

## 🚀 ステップ2: コンパイルと動作確認

### 2-1. プロジェクトのビルド

パッケージ構成を変更したので、プロジェクトをクリーンビルドします。

```bash
cd /path/to/workspace/hello-spring-boot
./mvnw clean compile
```

**期待される結果**:
```
[INFO] BUILD SUCCESS
```

もしコンパイルエラーが出た場合、以下を確認してください：
- パッケージ宣言が正しいか
- インポート文が正しいか
- 古いファイルが削除されているか

---

### 2-2. アプリケーションの起動

```bash
./mvnw spring-boot:run
```

**期待される結果**:
```
Started HelloSpringBootApplication in X.XXX seconds
```

---

## ✅ ステップ3: 動作確認

### 3-1. ユーザー一覧の取得

```bash
curl http://localhost:8080/api/users
```

**期待される結果**:
```json
[
  {
    "id": 1,
    "name": "Alice",
    "email": "alice@example.com",
    "age": 30,
    "createdAt": "2025-12-13T10:00:00",
    "updatedAt": "2025-12-13T10:00:00"
  },
  {
    "id": 2,
    "name": "Bob",
    "email": "bob@example.com",
    "age": 25,
    "createdAt": "2025-12-13T10:00:00",
    "updatedAt": "2025-12-13T10:00:00"
  }
]
```

---

### 3-2. 商品一覧の取得

```bash
curl http://localhost:8080/api/products
```

**期待される結果**:
```json
[
  {
    "id": 1,
    "name": "Laptop",
    "description": "High performance laptop",
    "price": 150000,
    "stock": 10,
    "createdAt": "2025-12-13T10:00:00",
    "updatedAt": "2025-12-13T10:00:00",
    "category": {
      "id": 1,
      "name": "Electronics"
    }
  }
]
```

---

### 3-3. カテゴリ一覧の取得

```bash
curl http://localhost:8080/api/categories
```

**期待される結果**:
```json
[
  {
    "id": 1,
    "name": "Electronics",
    "description": "Electronic devices",
    "createdAt": "2025-12-13T10:00:00",
    "updatedAt": "2025-12-13T10:00:00",
    "products": [...]
  }
]
```

---

### 3-4. レポートの取得（MyBatis）

```bash
curl http://localhost:8080/api/reports/orders/user/1
```

**期待される結果**:
```json
[
  {
    "userId": 1,
    "userName": "Alice",
    "productId": 1,
    "productName": "Laptop",
    "quantity": 2,
    "totalPrice": 300000
  }
]
```

すべてのエンドポイントが正常に動作すれば、リファクタリング成功です！🎉

---

## 🎨 チャレンジ課題

基本が理解できたら、以下にチャレンジしてみましょう：

### チャレンジ 1: HelloController の整理

Phase 1で作成した `HelloController.java` も `controllers/` ディレクトリに移動してください。

**ヒント**:
- パッケージ宣言を `package com.example.hellospringboot.controllers;` に変更
- インポート文が必要かどうか確認

---

### チャレンジ 2: 共通の例外ハンドリング用パッケージを作成

現在、各Serviceで `RuntimeException` をスローしていますが、より適切な例外処理を行うために `exceptions/` パッケージを作成してみましょう。

**ヒント**:
```java
package com.example.hellospringboot.exceptions;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

次のステップ（Step 17）で詳しく学びますが、先取りして実装してみるのも良いでしょう。

---

### チャレンジ 3: パッケージ構成図の作成

リファクタリング後のパッケージ構成を図にしてみましょう（テキストで構いません）。

**ヒント**:
```
com.example.hellospringboot/
├── controllers/ (表示層)
├── services/ (業務層)
├── repositories/ (データ層 - JPA)
├── mappers/ (データ層 - MyBatis)
└── entities/ (ドメインモデル)
```

依存関係の矢印（→）も書くとより理解が深まります。

---

## 🐛 トラブルシューティング

### エラー: "Could not find or load main class"

**原因**: パッケージ構成を変更した後、古いビルド成果物が残っている

**解決策**:
```bash
./mvnw clean
./mvnw compile
./mvnw spring-boot:run
```

---

### エラー: "No qualifying bean of type 'UserService' found"

**原因**: `UserService` のパッケージが変わったため、Springがスキャンできていない

**解決策**:

`HelloSpringBootApplication.java` で `@ComponentScan` を明示的に指定します：

```java
package com.example.hellospringboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// 以下のアノテーションは通常不要ですが、問題がある場合は明示的に指定
// @ComponentScan(basePackages = "com.example.hellospringboot")
public class HelloSpringBootApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(HelloSpringBootApplication.class, args);
    }
}
```

通常、`@SpringBootApplication` アノテーションには `@ComponentScan` が含まれており、
同じパッケージ以下をすべてスキャンするため、明示的な指定は不要です。

---

### エラー: "Invalid bound statement (not found): UserMapper.findById"

**原因**: MyBatis Mapper XMLの `namespace` が古いパッケージを参照している

**解決策**:

`src/main/resources/mapper/UserMapper.xml` の先頭を確認：

```xml
<mapper namespace="com.example.hellospringboot.mappers.UserMapper">
```

パッケージ名が正しいことを確認してください。

---

### エラー: "cannot find symbol" でコンパイルエラー

**原因**: インポート文が古いパッケージを参照している

**解決策**:

各Javaファイルのインポート文を確認し、新しいパッケージ構成に合わせて修正してください。

**例**:
```java
// 誤り
import com.example.hellospringboot.User;

// 正しい
import com.example.hellospringboot.entities.User;
```

IDEの自動インポート機能（IntelliJ IDEAなら `Ctrl+Shift+O`、VSCodeなら `Shift+Alt+O`）を使うと便利です。

---

### 警告: "Warning: The requested profile 'dev' could not be activated"

**原因**: この警告はリファクタリングとは無関係で、`application-dev.yml` が存在しない可能性があります

**解決策**:

この警告は無視しても問題ありませんが、気になる場合は `application.yaml` でプロファイルを確認してください：

```yaml
spring:
  profiles:
    active: dev  # ← これを削除するか、application-dev.yml を作成
```

---

## 📚 このステップで学んだこと

- ✅ レイヤー化アーキテクチャの基本概念（Controller / Service / Repository）
- ✅ 各レイヤーの責務と依存関係の方向
- ✅ パッケージ構成のベストプラクティス（Phase 4以降）
- ✅ 既存コードのリファクタリング手法
- ✅ `@RestController`, `@Service`, `@Repository` の役割
- ✅ パッケージ変更に伴うインポート文の修正
- ✅ MyBatis Mapper XMLの名前空間設定
- ✅ 保守性の高いコード構成の実現方法

---

## 💡 補足: レイヤー化アーキテクチャの利点

### 1. 関心の分離（Separation of Concerns）

各レイヤーが明確な責務を持つことで、コードの見通しが良くなります。

**例**:
- Controllerは「HTTPリクエストの処理」だけに集中
- Serviceは「ビジネスロジック」だけに集中
- Repositoryは「データアクセス」だけに集中

---

### 2. テスタビリティの向上

各レイヤーを独立してテストできます。

**例**:
```java
@Test
void testUserService() {
    // ServiceだけをテストするためにRepositoryをモック化
    UserMapper mockMapper = mock(UserMapper.class);
    UserService service = new UserService(mockMapper);
    
    when(mockMapper.findById(1L)).thenReturn(new User(...));
    
    User result = service.findById(1L);
    assertNotNull(result);
}
```

---

### 3. 変更の影響範囲の限定

データアクセス方法を変更する場合、Repositoryレイヤーだけを修正すればOK。

**例**:
- JPAからMyBatisに変更 → Repositoryだけ修正
- ビジネスルールの変更 → Serviceだけ修正
- レスポンス形式の変更 → Controllerだけ修正

---

### 4. 複数人での並行開発

各レイヤーが独立しているため、チームで分担して開発できます。

**例**:
- Aさん: Controller（API設計）を担当
- Bさん: Service（ビジネスロジック）を担当
- Cさん: Repository（データアクセス）を担当

---

### 5. 再利用性の向上

Serviceレイヤーのビジネスロジックを複数のControllerから呼び出せます。

**例**:
```java
// REST APIのController
@RestController
public class UserRestController {
    private final UserService userService;
    
    @GetMapping("/api/users/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);  // 同じServiceを使用
    }
}

// Web画面のController
@Controller
public class UserWebController {
    private final UserService userService;
    
    @GetMapping("/users/{id}")
    public String getUser(@PathVariable Long id, Model model) {
        model.addAttribute("user", userService.findById(id));  // 同じServiceを使用
        return "user-detail";
    }
}
```

---

## 💡 補足: よくある質問

### Q1: ServiceとRepositoryの違いは何ですか？

**A**: Serviceは「ビジネスロジック」、Repositoryは「データアクセス」です。

**例**:
```java
// Repository: データベースから取得するだけ
public interface UserRepository extends JpaRepository<User, Long> {
}

// Service: ビジネスルールを実装
@Service
public class UserService {
    
    @Transactional
    public User createUser(User user) {
        // ビジネスルール: メールアドレスの重複チェック
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        return userRepository.save(user);
    }
}
```

---

### Q2: Controllerに直接Repositoryを注入してはいけないのですか？

**A**: 技術的には可能ですが、推奨されません。

**理由**:
- ビジネスロジックがControllerに漏れる
- トランザクション管理が難しい
- 複数のRepositoryを組み合わせる時に複雑になる

**悪い例** ❌:
```java
@RestController
public class UserController {
    private final UserRepository userRepository;
    
    @PostMapping("/users")
    public User createUser(@RequestBody User user) {
        // ❌ Controllerにビジネスロジックが漏れている
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        return userRepository.save(user);
    }
}
```

**良い例** ✅:
```java
@RestController
public class UserController {
    private final UserService userService;
    
    @PostMapping("/users")
    public User createUser(@RequestBody User user) {
        // ✅ Serviceに委譲
        return userService.createUser(user);
    }
}
```

---

### Q3: 小さなアプリケーションでもレイヤー化は必要ですか？

**A**: 小規模でも最初からレイヤー化しておくことをおすすめします。

**理由**:
- 後からリファクタリングするのは大変
- 機能が増えた時にスムーズに拡張できる
- 学習教材として適切な構造を身につけられる

ただし、**プロトタイプや実験的なコード**では簡潔さを優先しても構いません。

---

### Q4: JPAとMyBatisで構成が変わりますか？

**A**: 基本的には同じですが、以下のように使い分けます：

| データアクセス技術 | 配置場所 | 役割 |
|---|---|---|
| **JPA** | `repositories/` | インターフェースのみ（`JpaRepository` を継承） |
| **MyBatis** | `mappers/` | インターフェース + XML（SQLを記述） |

**両方を使う場合**:
```
com.example.hellospringboot/
├── repositories/     # JPA用
│   ├── ProductRepository.java
│   └── CategoryRepository.java
├── mappers/          # MyBatis用
│   ├── UserMapper.java
│   └── ReportMapper.java
```

---

## 💡 補足: アーキテクチャの発展形

レイヤー化アーキテクチャはソフトウェア設計の基本ですが、さらに発展した設計パターンも存在します。

### ヘキサゴナルアーキテクチャ（Ports and Adapters）

```
┌─────────────────────────────────────┐
│        Domain Layer (Core)          │
│   - ビジネスロジックとドメインモデル │
└─────────────────────────────────────┘
       ↑ Port (Interface)
┌──────────────┐            ┌──────────────┐
│   Adapter    │            │   Adapter    │
│  (Controller)│            │ (Repository) │
│  REST API    │            │   Database   │
└──────────────┘            └──────────────┘
```

**特徴**:
- ドメインロジックが完全に独立
- データベースやフレームワークへの依存がない
- テストが非常に容易

---

### クリーンアーキテクチャ

```
┌─────────────────────────────────────┐
│         Entities (Core)             │
└─────────────────────────────────────┘
            ↓
┌─────────────────────────────────────┐
│       Use Cases (Application)       │
└─────────────────────────────────────┘
            ↓
┌─────────────────────────────────────┐
│  Interface Adapters (Controllers)   │
└─────────────────────────────────────┘
            ↓
┌─────────────────────────────────────┐
│  Frameworks & Drivers (DB, Web)     │
└─────────────────────────────────────┘
```

**特徴**:
- より詳細な責務の分離
- 依存関係の方向が明確（外側→内側のみ）
- 大規模アプリケーションに適している

---

これらの発展形は、**Phase 8の総合演習**で触れる機会があります。
まずは基本のレイヤー化アーキテクチャをしっかりと理解しましょう！

---

## ➡️ 次のステップ

[Step 16: DI/IoCコンテナの深掘り](STEP_16.md)へ進みましょう！

次のステップでは、Spring Frameworkの**コアである依存性注入（DI）とIoCコンテナ**を深く理解します。

**Step 16で学ぶこと**:
- 依存性注入（DI）とは何か、なぜ必要か
- `@Component`, `@Service`, `@Repository` の違い
- コンストラクタインジェクション vs フィールドインジェクション
- `@Autowired`, `@Qualifier`, `@Primary` の使い方
- Beanのスコープ（Singleton, Prototype等）
- `@Configuration` と `@Bean` で手動Bean登録

レイヤー化アーキテクチャの理解をさらに深め、Springの強力な機能を活用できるようになります。お疲れさまでした！🎉
