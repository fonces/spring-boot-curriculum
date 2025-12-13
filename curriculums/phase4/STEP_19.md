# Step 19: DTOとEntityの分離

## 🎯 このステップの目標

- DTOとEntityの役割と責務の違いを理解できる
- なぜDTOが必要なのか（セキュリティ、柔軟性）を説明できる
- リクエストDTO、レスポンスDTO、Entityを適切に使い分けできる
- MapStructを使ってDTOとEntityの変換を自動化できる
- ネストしたオブジェクトのマッピングを実装できる

**所要時間**: 約60分

---

## 📋 事前準備

- [Step 18: バリデーション](STEP_18.md)が完了していること
- DTOパターンの基本（`UserCreateRequest`, `UserResponse`など）を理解していること
- エンティティ（JPA/MyBatisの`User`, `Product`など）を実装していること

---

## 🤔 なぜDTOとEntityを分離するのか

### Before（Entityを直接使用）

Phase 3までの実装では、EntityをそのままControllerで使用していました：

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id);  // Entityを直接返す
    }
    
    @PostMapping
    public User createUser(@RequestBody User user) {  // Entityを直接受け取る
        return userService.createUser(user);
    }
}
```

**問題点**:

#### 1. セキュリティリスク
```java
@Entity
public class User {
    private Long id;
    private String name;
    private String email;
    private String password;  // ⚠️ パスワードがレスポンスに含まれてしまう
    private String role;      // ⚠️ 内部の権限情報が露出
}
```

レスポンス例：
```json
{
  "id": 1,
  "name": "Alice",
  "email": "alice@example.com",
  "password": "$2a$10$...",  // ❌ ハッシュ化されていても危険
  "role": "ADMIN"            // ❌ 内部情報の露出
}
```

---

#### 2. 柔軟性の欠如
- エンティティのフィールド名変更がAPIの破壊的変更になる
- データベース構造とAPIのレスポンス形式が密結合
- クライアントに不要なフィールドまで返してしまう

```java
@Entity
public class Product {
    private Long id;
    private String productName;  // DB: product_name
    private Integer stock;
    private Integer reserved;    // 内部管理用（クライアントには不要）
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

レスポンス例：
```json
{
  "id": 1,
  "productName": "Laptop",
  "stock": 10,
  "reserved": 3,           // ❌ 内部情報
  "createdAt": "2025-01-15T10:00:00",
  "updatedAt": "2025-01-15T12:30:00"
}
```

---

#### 3. バリデーションの問題
- 作成時と更新時で必要なフィールドが異なる
- 読み取り専用フィールド（IDなど）に値を設定されてしまう

```java
@PostMapping
public User createUser(@RequestBody User user) {
    // クライアントが勝手にidを設定できてしまう
    // user.setId(999L); // ❌ 本来は自動採番されるべき
    return userService.createUser(user);
}
```

---

### After（DTOで分離）

**リクエストDTO（クライアント→サーバー）**:
```java
@Data
public class UserCreateRequest {
    @NotBlank
    private String name;
    
    @Email
    private String email;
    
    @NotBlank
    @Size(min = 8)
    private String password;
    
    // idは含めない（自動採番）
    // roleは含めない（サーバー側で設定）
}
```

**レスポンスDTO（サーバー→クライアント）**:
```java
@Data
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    // passwordは含めない
    // roleは必要に応じて別DTOで返す
}
```

**Entity（データベース層）**:
```java
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String email;
    private String password;  // ハッシュ化済み
    private String role;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

**メリット**:
- ✅ **セキュリティ**: パスワードや内部情報を隠蔽
- ✅ **柔軟性**: APIとDBスキーマを独立して変更可能
- ✅ **バリデーション**: リクエストごとに適切な検証ルール
- ✅ **可読性**: 役割が明確（Request/Response/Entity）
- ✅ **ドキュメント**: APIの入出力が明確

---

## 🚀 ステップ1: MapStructの導入

### 1-1. 依存関係の追加

手動でDTOとEntityを変換するのは冗長なので、MapStructを使って自動化します。

`pom.xml`に以下を追加します：

```xml
<properties>
    <java.version>21</java.version>
    <mapstruct.version>1.5.5.Final</mapstruct.version>
</properties>

<dependencies>
    <!-- MapStruct -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>${mapstruct.version}</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.14.1</version>
            <configuration>
                <source>21</source>
                <target>21</target>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>${lombok.version}</version>
                    </path>
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                        <version>${mapstruct.version}</version>
                    </path>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok-mapstruct-binding</artifactId>
                        <version>0.2.0</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

---

### 1-2. プロジェクトのリビルド

```bash
cd workspace/hello-spring-boot
./mvnw clean compile
```

---

## 🚀 ステップ2: DTOクラスの整備

### 2-1. UserCreateRequestの確認

Step 18で作成した`UserCreateRequest`をそのまま使用します：

```java
package com.example.hellospringboot.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    private String email;
    
    @NotNull(message = "Age is required")
    @Min(value = 0, message = "Age must be at least 0")
    @Max(value = 150, message = "Age must be at most 150")
    private Integer age;
}
```

---

### 2-2. UserResponseの拡張

以下のファイルを`src/main/java/com/example/hellospringboot/dto/UserResponse.java`に作成します：

```java
package com.example.hellospringboot.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ユーザーレスポンスDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private Integer age;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
```

---

### 2-3. ProductDTOの作成

**ProductCreateRequest**:

以下のファイルを`src/main/java/com/example/hellospringboot/dto/ProductCreateRequest.java`に作成します：

```java
package com.example.hellospringboot.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateRequest {
    
    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 100, message = "Product name must be between 2 and 100 characters")
    private String name;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price must be positive")
    private Double price;
    
    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock must be at least 0")
    private Integer stock;
    
    @NotNull(message = "Category ID is required")
    private Long categoryId;
}
```

---

**ProductResponse**:

以下のファイルを`src/main/java/com/example/hellospringboot/dto/ProductResponse.java`に作成します：

```java
package com.example.hellospringboot.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String name;
    private Double price;
    private Integer stock;
    
    // ネストしたオブジェクト（カテゴリ情報）
    private CategorySummary category;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    /**
     * カテゴリのサマリー情報（ネストしたDTO）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummary {
        private Long id;
        private String name;
    }
}
```

**レスポンス例**:
```json
{
  "id": 1,
  "name": "Laptop",
  "price": 1200.00,
  "stock": 10,
  "category": {
    "id": 1,
    "name": "Electronics"
  },
  "createdAt": "2025-01-15 10:00:00"
}
```

---

## 🚀 ステップ3: MapStructマッパーの作成

### 3-1. UserMapperインターフェース

以下のファイルを`src/main/java/com/example/hellospringboot/mappers/UserDtoMapper.java`に作成します：

```java
package com.example.hellospringboot.mappers;

import com.example.hellospringboot.dto.UserCreateRequest;
import com.example.hellospringboot.dto.UserResponse;
import com.example.hellospringboot.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * UserエンティティとDTOの変換マッパー
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserDtoMapper {
    
    /**
     * UserCreateRequest → User Entity
     * idは自動採番されるため、マッピング対象外
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(UserCreateRequest request);
    
    /**
     * User Entity → UserResponse
     */
    UserResponse toResponse(User user);
}
```

**ポイント**:
- `@Mapper(componentModel = "spring")`: Spring Beanとして登録
- `@Mapping(target = "id", ignore = true)`: idフィールドを無視
- MapStructが自動的に同名フィールドをマッピング

---

### 3-2. ProductMapperインターフェース

以下のファイルを`src/main/java/com/example/hellospringboot/mappers/ProductDtoMapper.java`に作成します：

```java
package com.example.hellospringboot.mappers;

import com.example.hellospringboot.dto.ProductCreateRequest;
import com.example.hellospringboot.dto.ProductResponse;
import com.example.hellospringboot.entities.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * ProductエンティティとDTOの変換マッパー
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductDtoMapper {
    
    /**
     * ProductCreateRequest → Product Entity
     * categoryはServiceで設定するため、ここではマッピングしない
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Product toEntity(ProductCreateRequest request);
    
    /**
     * Product Entity → ProductResponse
     * ネストしたcategoryオブジェクトを自動マッピング
     */
    @Mapping(source = "category.id", target = "category.id")
    @Mapping(source = "category.name", target = "category.name")
    ProductResponse toResponse(Product product);
}
```

---

### 3-3. MapStructの自動生成を確認

```bash
./mvnw clean compile
```

**生成されたファイル**:
- `target/generated-sources/annotations/com/example/hellospringboot/mappers/UserDtoMapperImpl.java`
- `target/generated-sources/annotations/com/example/hellospringboot/mappers/ProductDtoMapperImpl.java`

**UserDtoMapperImpl.java**の例:
```java
@Component
public class UserDtoMapperImpl implements UserDtoMapper {
    
    @Override
    public User toEntity(UserCreateRequest request) {
        if (request == null) {
            return null;
        }
        
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setAge(request.getAge());
        
        return user;
    }
    
    @Override
    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setAge(user.getAge());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        
        return response;
    }
}
```

---

## 🚀 ステップ4: Serviceでマッパーを使用

### 4-1. UserServiceの修正

既存の`src/main/java/com/example/hellospringboot/services/UserService.java`を修正します：

```java
package com.example.hellospringboot.services;

import com.example.hellospringboot.dto.UserCreateRequest;
import com.example.hellospringboot.dto.UserUpdateRequest;
import com.example.hellospringboot.entities.User;
import com.example.hellospringboot.exceptions.ResourceNotFoundException;
import com.example.hellospringboot.exceptions.ConflictException;
import com.example.hellospringboot.mappers.UserDtoMapper;
import com.example.hellospringboot.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserDtoMapper userDtoMapper;  // MapStructマッパーを注入
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }
    
    public User createUser(UserCreateRequest request) {
        // メールアドレスの重複チェック
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists: " + request.getEmail());
        }
        
        // MapStructでDTOをEntityに変換
        User user = userDtoMapper.toEntity(request);
        
        userRepository.save(user);
        return user;
    }
    
    public User updateUser(Long id, UserUpdateRequest request) {
        User existingUser = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        // 既存ユーザーの情報を更新
        existingUser.setName(request.getName());
        existingUser.setEmail(request.getEmail());
        existingUser.setAge(request.getAge());
        
        userRepository.save(existingUser);
        return existingUser;
    }
    
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", "id", id);
        }
        
        userRepository.deleteById(id);
    }
}
```

---

### 4-2. ProductServiceの作成

以下のファイルを`src/main/java/com/example/hellospringboot/services/ProductService.java`に作成します：

```java
package com.example.hellospringboot.services;

import com.example.hellospringboot.dto.ProductCreateRequest;
import com.example.hellospringboot.entities.Category;
import com.example.hellospringboot.entities.Product;
import com.example.hellospringboot.exceptions.ResourceNotFoundException;
import com.example.hellospringboot.mappers.ProductDtoMapper;
import com.example.hellospringboot.repositories.CategoryRepository;
import com.example.hellospringboot.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductDtoMapper productDtoMapper;
    
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
    public Product getProductById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }
    
    public Product createProduct(ProductCreateRequest request) {
        // カテゴリの存在確認
        Category category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
        
        // MapStructでDTOをEntityに変換
        Product product = productDtoMapper.toEntity(request);
        product.setCategory(category);
        
        productRepository.save(product);
        return product;
    }
    
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", "id", id);
        }
        
        productRepository.deleteById(id);
    }
}
```

---

## 🚀 ステップ5: Controllerの修正

### 5-1. UserControllerの最終版

既存の`src/main/java/com/example/hellospringboot/controllers/UserController.java`はすでにDTOを使用しているため、変更不要です。

---

### 5-2. ProductControllerの作成

以下のファイルを`src/main/java/com/example/hellospringboot/controllers/ProductController.java`に作成します：

```java
package com.example.hellospringboot.controllers;

import com.example.hellospringboot.dto.ProductCreateRequest;
import com.example.hellospringboot.dto.ProductResponse;
import com.example.hellospringboot.entities.Product;
import com.example.hellospringboot.mappers.ProductDtoMapper;
import com.example.hellospringboot.services.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final ProductDtoMapper productDtoMapper;
    
    /**
     * 全商品取得
     * GET /api/products
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        List<ProductResponse> responses = products.stream()
            .map(productDtoMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    /**
     * 商品詳細取得
     * GET /api/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(productDtoMapper.toResponse(product));
    }
    
    /**
     * 商品作成
     * POST /api/products
     */
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        Product product = productService.createProduct(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(productDtoMapper.toResponse(product));
    }
    
    /**
     * 商品削除
     * DELETE /api/products/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

## ✅ ステップ6: 動作確認

### 6-1. アプリケーション起動

```bash
cd workspace/hello-spring-boot
./mvnw clean compile spring-boot:run
```

---

### 6-2. User APIのテスト

**ユーザー作成**:

```bash
curl -i -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice",
    "email": "alice@example.com",
    "age": 25
  }'
```

**期待される結果**:
```json
{
  "id": 1,
  "name": "Alice",
  "email": "alice@example.com",
  "age": 25,
  "createdAt": "2025-01-15 14:00:00",
  "updatedAt": null
}
```

---

### 6-3. Product APIのテスト

**商品作成**:

```bash
curl -i -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop",
    "price": 1200.00,
    "stock": 10,
    "categoryId": 1
  }'
```

**期待される結果**:
```json
{
  "id": 1,
  "name": "Laptop",
  "price": 1200.0,
  "stock": 10,
  "category": {
    "id": 1,
    "name": "Electronics"
  },
  "createdAt": "2025-01-15 14:05:00"
}
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: ネストしたDTOの深いマッピング

OrderエンティティとOrderResponseの変換を実装してください。

**要件**:
- OrderはUserとProductを持つ
- OrderResponseにはUser情報とProduct情報をネストして含める

**ヒント**:

```java
@Data
public class OrderResponse {
    private Long id;
    private Integer quantity;
    
    private UserSummary user;
    private ProductSummary product;
    
    @Data
    public static class UserSummary {
        private Long id;
        private String name;
    }
    
    @Data
    public static class ProductSummary {
        private Long id;
        private String name;
        private Double price;
    }
}
```

```java
@Mapper(componentModel = "spring")
public interface OrderDtoMapper {
    
    @Mapping(source = "user.id", target = "user.id")
    @Mapping(source = "user.name", target = "user.name")
    @Mapping(source = "product.id", target = "product.id")
    @Mapping(source = "product.name", target = "product.name")
    @Mapping(source = "product.price", target = "product.price")
    OrderResponse toResponse(Order order);
}
```

---

### チャレンジ 2: 条件付きマッピング

特定の条件下でのみフィールドをマッピングする実装をしてください。

**要件**:
- 管理者ユーザーの場合のみ`role`フィールドを含める
- 一般ユーザーには`role`を返さない

**ヒント**:

```java
@Mapper(componentModel = "spring")
public interface UserDtoMapper {
    
    @Mapping(target = "role", expression = "java(isAdmin ? user.getRole() : null)")
    UserResponse toResponse(User user, boolean isAdmin);
}
```

---

## 🐛 トラブルシューティング

### エラー 1: "No property named 'category' exists in source parameter(s)"

**原因**: MapStructがネストしたオブジェクトのマッピングに失敗

**解決策**: `@Mapping`でソースとターゲットを明示

```java
// ❌ 間違い
@Mapping(source = "category", target = "category")
ProductResponse toResponse(Product product);

// ✅ 正しい
@Mapping(source = "category.id", target = "category.id")
@Mapping(source = "category.name", target = "category.name")
ProductResponse toResponse(Product product);
```

---

### エラー 2: "No implementation was created for UserDtoMapper"

**原因**: MapStructの自動生成が失敗

**解決策**: プロジェクトをクリーン＆リビルド

```bash
./mvnw clean compile
```

---

## 📚 このステップで学んだこと

- ✅ **DTOの役割**: セキュリティ、柔軟性、バリデーション
- ✅ **DTO/Entityの分離**: API層とDB層の独立性
- ✅ **MapStruct**: DTOとEntityの自動マッピング
- ✅ **ネストしたDTO**: 複雑なオブジェクト構造の表現
- ✅ **リクエスト/レスポンスDTO**: 入出力の明確な分離

---

## ➡️ 次のステップ

[Step 20: ロギング](STEP_20.md)へ進みましょう！

ログを適切に出力し、アプリケーションの動作を監視・デバッグしやすくする仕組みを作りましょう！
