# Step 37: 画像アップロードと検索機能

## 🎯 このステップの目標

ファイル管理と高度な検索機能を実装します。セキュアなファイルアップロード、プロフィール画像管理、MyBatisによる複雑な検索クエリを学びます。

**所要時間**: 約90分

**学ぶこと**:
- ファイルアップロードのセキュリティ対策
- パストラバーサル攻撃の防止
- ファイルサイズと拡張子の検証
- MyBatisによる動的SQL
- 複数条件での高度な検索機能
- プロフィール画像の管理

---

## 📋 事前準備

このステップを始める前に、以下が完了していることを確認してください：

- ✅ Step 36で記事とコメント機能を実装済み
- ✅ Spring Securityによる認証が動作している
- ✅ MySQLデータベースが起動している
- ✅ MyBatisの基本的な使い方を理解している

---

## 📝 ステップバイステップの手順

### 1. application.ymlにファイルアップロード設定を追加

`src/main/resources/application.yml`にファイルアップロードの設定を追加します。

```yaml
spring:
  application:
    name: bloghub
  
  datasource:
    url: jdbc:mysql://localhost:3307/bloghub?useSSL=false&serverTimezone=Asia/Tokyo&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&useUnicode=true
    username: bloghub_user
    password: bloghub_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect
  
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

jwt:
  secret: 3f8b2c7e9a1d5f4e6b8c2a9d7e5f3b1c8e6a4d2f9b7e5c3a1d8f6b4e2c9a7d5f3b
  expiration: 86400000

file:
  upload-dir: uploads
  max-file-size: 5MB

mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.example.bloghub.dto
  configuration:
    map-underscore-to-camel-case: true
```

**ポイント**:
- `spring.servlet.multipart.max-file-size`: アップロード可能なファイルの最大サイズ（5MB）
- `spring.servlet.multipart.max-request-size`: リクエスト全体の最大サイズ
- `file.upload-dir`: ファイルの保存先ディレクトリ

---

### 2. FileStorageServiceの実装

セキュアなファイル保存機能を実装します。

`src/main/java/com/example/bloghub/service/FileStorageService.java`を作成：

```java
package com.example.bloghub.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {
    
    private final Path fileStorageLocation;
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    
    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize();
        
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException e) {
            throw new RuntimeException("ファイル保存用ディレクトリの作成に失敗しました", e);
        }
    }
    
    /**
     * ファイルを保存します
     * @param file アップロードされたファイル
     * @return 保存されたファイル名
     */
    public String storeFile(MultipartFile file) {
        // ファイル名のサニタイズ
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        
        try {
            // パストラバーサル攻撃の防止
            if (originalFileName.contains("..")) {
                throw new RuntimeException("不正なファイル名が含まれています: " + originalFileName);
            }
            
            // ファイルサイズチェック
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new RuntimeException("ファイルサイズが5MBを超えています");
            }
            
            // 拡張子チェック
            String extension = getFileExtension(originalFileName);
            if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
                throw new RuntimeException("許可されていないファイル形式です。jpg、png、gifのみアップロード可能です");
            }
            
            // ユニークなファイル名を生成
            String fileName = UUID.randomUUID().toString() + "." + extension;
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            
            // ファイルをコピー
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("ファイルの保存に失敗しました: " + originalFileName, e);
        }
    }
    
    /**
     * ファイルをリソースとして読み込みます
     * @param fileName ファイル名
     * @return ファイルリソース
     */
    public Resource loadFileAsResource(String fileName) {
        try {
            // ファイル名のサニタイズ
            String cleanFileName = StringUtils.cleanPath(fileName);
            
            // パストラバーサル攻撃の防止
            if (cleanFileName.contains("..")) {
                throw new RuntimeException("不正なファイル名が含まれています: " + cleanFileName);
            }
            
            Path filePath = this.fileStorageLocation.resolve(cleanFileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("ファイルが見つかりません: " + fileName);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("ファイルが見つかりません: " + fileName, e);
        }
    }
    
    /**
     * ファイルを削除します
     * @param fileName ファイル名
     */
    public void deleteFile(String fileName) {
        try {
            // ファイル名のサニタイズ
            String cleanFileName = StringUtils.cleanPath(fileName);
            
            // パストラバーサル攻撃の防止
            if (cleanFileName.contains("..")) {
                throw new RuntimeException("不正なファイル名が含まれています: " + cleanFileName);
            }
            
            Path filePath = this.fileStorageLocation.resolve(cleanFileName).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("ファイルの削除に失敗しました: " + fileName, e);
        }
    }
    
    /**
     * ファイル拡張子を取得します
     * @param fileName ファイル名
     * @return 拡張子
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
}
```

**セキュリティ対策のポイント**:
- **パストラバーサル防止**: `..`を含むファイル名を拒否
- **ファイルサイズ制限**: 5MBを超えるファイルを拒否
- **拡張子チェック**: jpg、png、gifのみ許可
- **ユニークなファイル名**: UUID付与で衝突を防止

---

### 3. FileControllerの実装

ファイルアップロード・ダウンロードのエンドポイントを作成します。

`src/main/java/com/example/bloghub/controller/FileController.java`を作成：

```java
package com.example.bloghub.controller;

import com.example.bloghub.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
    
    private final FileStorageService fileStorageService;
    
    /**
     * ファイルをアップロードします
     */
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file) {
        
        String fileName = fileStorageService.storeFile(file);
        
        Map<String, String> response = new HashMap<>();
        response.put("fileName", fileName);
        response.put("fileUrl", "/api/files/" + fileName);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * ファイルをダウンロードします
     */
    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        Resource resource = fileStorageService.loadFileAsResource(fileName);
        
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
    
    /**
     * ファイルを削除します
     */
    @DeleteMapping("/{fileName:.+}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteFile(@PathVariable String fileName) {
        fileStorageService.deleteFile(fileName);
        return ResponseEntity.noContent().build();
    }
}
```

**ポイント**:
- `@PreAuthorize("isAuthenticated()")`: 認証済みユーザーのみアップロード・削除可能
- `/{fileName:.+}`: ファイル名に`.`が含まれても正しく処理
- `MediaType.IMAGE_JPEG`: 画像として返す

---

### 4. Userエンティティにプロフィール画像フィールドを追加

`src/main/java/com/example/bloghub/entity/User.java`を更新：

```java
package com.example.bloghub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(name = "profile_image")
    private String profileImage;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Article> articles = new ArrayList<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();
}
```

---

### 5. UserService拡張

プロフィール画像アップロード機能を追加します。

`src/main/java/com/example/bloghub/service/UserService.java`を更新：

```java
package com.example.bloghub.service;

import com.example.bloghub.dto.user.UserResponse;
import com.example.bloghub.dto.user.UserUpdateRequest;
import com.example.bloghub.entity.User;
import com.example.bloghub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    
    /**
     * プロフィール画像をアップロードします
     */
    @Transactional
    public UserResponse uploadProfileImage(String username, MultipartFile file) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("ユーザーが見つかりません"));
        
        // 既存のプロフィール画像を削除
        if (user.getProfileImage() != null) {
            fileStorageService.deleteFile(user.getProfileImage());
        }
        
        // 新しい画像を保存
        String fileName = fileStorageService.storeFile(file);
        user.setProfileImage(fileName);
        
        User savedUser = userRepository.save(user);
        return convertToResponse(savedUser);
    }
    
    /**
     * ユーザー情報を取得します
     */
    public UserResponse getUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("ユーザーが見つかりません"));
        return convertToResponse(user);
    }
    
    /**
     * ユーザー情報を更新します
     */
    @Transactional
    public UserResponse updateUser(String username, UserUpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("ユーザーが見つかりません"));
        
        User savedUser = userRepository.save(user);
        return convertToResponse(savedUser);
    }
    
    private UserResponse convertToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setProfileImage(user.getProfileImage());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
```

---

### 6. UserUpdateRequestとUserResponseの作成

`src/main/java/com/example/bloghub/dto/user/UserUpdateRequest.java`を作成：

```java
package com.example.bloghub.dto.user;

import lombok.Data;

@Data
public class UserUpdateRequest {
}
```

`src/main/java/com/example/bloghub/dto/user/UserResponse.java`を作成：

```java
package com.example.bloghub.dto.user;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String profileImage;
    private LocalDateTime createdAt;
}
```

---

### 7. UserControllerの拡張

`src/main/java/com/example/bloghub/controller/UserController.java`を作成：

```java
package com.example.bloghub.controller;

import com.example.bloghub.dto.user.UserResponse;
import com.example.bloghub.dto.user.UserUpdateRequest;
import com.example.bloghub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    /**
     * プロフィール画像をアップロードします
     */
    @PostMapping("/profile-image")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> uploadProfileImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        
        UserResponse response = userService.uploadProfileImage(userDetails.getUsername(), file);
        return ResponseEntity.ok(response);
    }
    
    /**
     * ユーザー情報を取得します
     */
    @GetMapping("/{username}")
    public ResponseEntity<UserResponse> getUser(@PathVariable String username) {
        UserResponse response = userService.getUser(username);
        return ResponseEntity.ok(response);
    }
    
    /**
     * ユーザー情報を更新します
     */
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> updateUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UserUpdateRequest request) {
        
        UserResponse response = userService.updateUser(userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }
}
```

---

### 8. MyBatisによる高度な検索機能の実装

#### 8.1 ArticleSearchMapperインターフェースの作成

`src/main/java/com/example/bloghub/mapper/ArticleSearchMapper.java`を作成：

```java
package com.example.bloghub.mapper;

import com.example.bloghub.dto.article.ArticleSearchResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ArticleSearchMapper {
    
    /**
     * 複数条件で記事を検索します
     */
    List<ArticleSearchResult> searchArticles(
            @Param("keyword") String keyword,
            @Param("tagNames") List<String> tagNames,
            @Param("username") String username,
            @Param("offset") int offset,
            @Param("limit") int limit
    );
    
    /**
     * 検索結果の件数を取得します
     */
    int countSearchResults(
            @Param("keyword") String keyword,
            @Param("tagNames") List<String> tagNames,
            @Param("username") String username
    );
}
```

---

#### 8.2 ArticleSearchMapper.xmlの作成

`src/main/resources/mapper/ArticleSearchMapper.xml`を作成：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.bloghub.mapper.ArticleSearchMapper">
    
    <resultMap id="ArticleSearchResultMap" type="com.example.bloghub.dto.article.ArticleSearchResult">
        <id property="id" column="id"/>
        <result property="title" column="title"/>
        <result property="content" column="content"/>
        <result property="username" column="username"/>
        <result property="createdAt" column="created_at"/>
        <collection property="tags" ofType="String">
            <result column="tag_name"/>
        </collection>
    </resultMap>
    
    <!-- 記事検索 -->
    <select id="searchArticles" resultMap="ArticleSearchResultMap">
        SELECT DISTINCT
            a.id,
            a.title,
            a.content,
            u.username,
            a.created_at,
            t.name as tag_name
        FROM articles a
        INNER JOIN users u ON a.user_id = u.id
        LEFT JOIN article_tags at ON a.id = at.article_id
        LEFT JOIN tags t ON at.tag_id = t.id
        <where>
            <if test="keyword != null and keyword != ''">
                (a.title LIKE CONCAT('%', #{keyword}, '%')
                OR a.content LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="tagNames != null and tagNames.size() > 0">
                AND t.name IN
                <foreach collection="tagNames" item="tag" open="(" separator="," close=")">
                    #{tag}
                </foreach>
            </if>
            <if test="username != null and username != ''">
                AND u.username = #{username}
            </if>
        </where>
        ORDER BY a.created_at DESC
        LIMIT #{limit} OFFSET #{offset}
    </select>
    
    <!-- 検索結果の件数取得 -->
    <select id="countSearchResults" resultType="int">
        SELECT COUNT(DISTINCT a.id)
        FROM articles a
        INNER JOIN users u ON a.user_id = u.id
        LEFT JOIN article_tags at ON a.id = at.article_id
        LEFT JOIN tags t ON at.tag_id = t.id
        <where>
            <if test="keyword != null and keyword != ''">
                (a.title LIKE CONCAT('%', #{keyword}, '%')
                OR a.content LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="tagNames != null and tagNames.size() > 0">
                AND t.name IN
                <foreach collection="tagNames" item="tag" open="(" separator="," close=")">
                    #{tag}
                </foreach>
            </if>
            <if test="username != null and username != ''">
                AND u.username = #{username}
            </if>
        </where>
    </select>
</mapper>
```

**動的SQLのポイント**:
- `<where>`: 条件がない場合は`WHERE`句を省略
- `<if>`: 条件が満たされた場合のみSQL追加
- `<foreach>`: リストを展開してIN句を生成

---

#### 8.3 DTOの作成

`src/main/java/com/example/bloghub/dto/article/ArticleSearchRequest.java`を作成：

```java
package com.example.bloghub.dto.article;

import lombok.Data;

import java.util.List;

@Data
public class ArticleSearchRequest {
    private String keyword;
    private List<String> tags;
    private String username;
    private int page = 0;
    private int size = 10;
}
```

`src/main/java/com/example/bloghub/dto/article/ArticleSearchResult.java`を作成：

```java
package com.example.bloghub.dto.article;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ArticleSearchResult {
    private Long id;
    private String title;
    private String content;
    private String username;
    private List<String> tags = new ArrayList<>();
    private LocalDateTime createdAt;
}
```

`src/main/java/com/example/bloghub/dto/article/ArticleSearchResponse.java`を作成：

```java
package com.example.bloghub.dto.article;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ArticleSearchResponse {
    private List<ArticleSearchResult> articles;
    private int totalPages;
    private long totalElements;
    private int currentPage;
}
```

---

#### 8.4 ArticleSearchServiceの作成

`src/main/java/com/example/bloghub/service/ArticleSearchService.java`を作成：

```java
package com.example.bloghub.service;

import com.example.bloghub.dto.article.ArticleSearchRequest;
import com.example.bloghub.dto.article.ArticleSearchResponse;
import com.example.bloghub.dto.article.ArticleSearchResult;
import com.example.bloghub.mapper.ArticleSearchMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleSearchService {
    
    private final ArticleSearchMapper articleSearchMapper;
    
    /**
     * 複数条件で記事を検索します
     */
    public ArticleSearchResponse searchArticles(ArticleSearchRequest request) {
        // オフセットを計算
        int offset = request.getPage() * request.getSize();
        
        // 記事を検索
        List<ArticleSearchResult> articles = articleSearchMapper.searchArticles(
                request.getKeyword(),
                request.getTags(),
                request.getUsername(),
                offset,
                request.getSize()
        );
        
        // 総件数を取得
        int totalElements = articleSearchMapper.countSearchResults(
                request.getKeyword(),
                request.getTags(),
                request.getUsername()
        );
        
        // 総ページ数を計算
        int totalPages = (int) Math.ceil((double) totalElements / request.getSize());
        
        return new ArticleSearchResponse(
                articles,
                totalPages,
                totalElements,
                request.getPage()
        );
    }
}
```

---

#### 8.5 ArticleControllerに検索エンドポイントを追加

`src/main/java/com/example/bloghub/controller/ArticleController.java`を更新：

```java
package com.example.bloghub.controller;

import com.example.bloghub.dto.article.*;
import com.example.bloghub.service.ArticleSearchService;
import com.example.bloghub.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {
    
    private final ArticleService articleService;
    private final ArticleSearchService articleSearchService;
    
    /**
     * 記事を作成します
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ArticleResponse> createArticle(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ArticleCreateRequest request) {
        
        ArticleResponse response = articleService.createArticle(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * 記事一覧を取得します
     */
    @GetMapping
    public ResponseEntity<List<ArticleResponse>> getAllArticles() {
        List<ArticleResponse> articles = articleService.getAllArticles();
        return ResponseEntity.ok(articles);
    }
    
    /**
     * 記事を取得します
     */
    @GetMapping("/{id}")
    public ResponseEntity<ArticleDetailResponse> getArticle(@PathVariable Long id) {
        ArticleDetailResponse response = articleService.getArticle(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 記事を更新します
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ArticleResponse> updateArticle(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ArticleUpdateRequest request) {
        
        ArticleResponse response = articleService.updateArticle(id, userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 記事を削除します
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteArticle(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        articleService.deleteArticle(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 記事を検索します
     */
    @GetMapping("/search")
    public ResponseEntity<ArticleSearchResponse> searchArticles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        ArticleSearchRequest request = new ArticleSearchRequest();
        request.setKeyword(keyword);
        request.setTags(tags);
        request.setUsername(username);
        request.setPage(page);
        request.setSize(size);
        
        ArticleSearchResponse response = articleSearchService.searchArticles(request);
        return ResponseEntity.ok(response);
    }
}
```

---

### 9. アプリケーションのビルドと起動

```bash
cd workspace/bloghub
./mvnw clean install
./mvnw spring-boot:run
```

---

## ✅ 動作確認

### 1. ユーザー登録とログイン

```bash
# ユーザー登録
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "email": "alice@example.com",
    "password": "password123"
  }'

# ログイン
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "password": "password123"
  }'
```

**レスポンス例**:
```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "username": "alice",
  "email": "alice@example.com"
}
```

トークンを環境変数に保存：
```bash
export TOKEN="eyJhbGciOiJIUzI1NiJ9..."
```

---

### 2. プロフィール画像のアップロード

```bash
# 画像ファイルを作成（テスト用）
echo "fake image data" > profile.jpg

# プロフィール画像をアップロード
curl -X POST http://localhost:8080/api/users/profile-image \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@profile.jpg"
```

**レスポンス例**:
```json
{
  "id": 1,
  "username": "alice",
  "email": "alice@example.com",
  "profileImage": "a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg",
  "createdAt": "2025-12-13T10:00:00"
}
```

---

### 3. ユーザー情報の取得

```bash
curl http://localhost:8080/api/users/alice
```

**レスポンス例**:
```json
{
  "id": 1,
  "username": "alice",
  "email": "alice@example.com",
  "profileImage": "a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg",
  "createdAt": "2025-12-13T10:00:00"
}
```

---

### 4. プロフィール画像のダウンロード

```bash
curl http://localhost:8080/api/files/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg -o downloaded.jpg
```

---

### 5. 記事の作成（検索テスト用）

```bash
# Spring Boot記事を作成
curl -X POST http://localhost:8080/api/articles \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Spring Boot入門",
    "content": "Spring Bootの基礎を学びます",
    "tags": ["Spring", "Tutorial"]
  }'

# Java記事を作成
curl -X POST http://localhost:8080/api/articles \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Javaプログラミング",
    "content": "Javaの基本文法を学びます",
    "tags": ["Java", "Programming"]
  }'
```

---

### 6. 高度な検索機能のテスト

#### 6.1 キーワード検索

```bash
curl "http://localhost:8080/api/articles/search?keyword=Spring"
```

**レスポンス例**:
```json
{
  "articles": [
    {
      "id": 1,
      "title": "Spring Boot入門",
      "content": "Spring Bootの基礎を学びます",
      "username": "alice",
      "tags": ["Spring", "Tutorial"],
      "createdAt": "2025-12-13T10:00:00"
    }
  ],
  "totalPages": 1,
  "totalElements": 1,
  "currentPage": 0
}
```

---

#### 6.2 タグ検索

```bash
curl "http://localhost:8080/api/articles/search?tags=Java&tags=Programming"
```

---

#### 6.3 ユーザー名検索

```bash
curl "http://localhost:8080/api/articles/search?username=alice"
```

---

#### 6.4 複合条件検索

```bash
curl "http://localhost:8080/api/articles/search?keyword=Spring&tags=Tutorial&username=alice"
```

---

#### 6.5 ページネーション付き検索

```bash
curl "http://localhost:8080/api/articles/search?keyword=Java&page=0&size=5"
```

---

### 7. ファイルアップロードのエラーテスト

#### 7.1 大きすぎるファイル

```bash
# 10MBのファイルを作成
dd if=/dev/zero of=large.jpg bs=1M count=10

# アップロード（失敗するはず）
curl -X POST http://localhost:8080/api/users/profile-image \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@large.jpg"
```

**エラーレスポンス**:
```
ファイルサイズが5MBを超えています
```

---

#### 7.2 許可されていない拡張子

```bash
# txtファイルを作成
echo "test" > test.txt

# アップロード（失敗するはず）
curl -X POST http://localhost:8080/api/users/profile-image \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@test.txt"
```

**エラーレスポンス**:
```
許可されていないファイル形式です。jpg、png、gifのみアップロード可能です
```

---

## 🎯 チャレンジ課題

### 課題1: 画像のサムネイル生成

画像アップロード時に自動的にサムネイルを生成する機能を実装してください。

**ヒント**:
```java
// 依存関係追加（pom.xml）
<dependency>
    <groupId>net.coobird</groupId>
    <artifactId>thumbnailator</artifactId>
    <version>0.4.20</version>
</dependency>

// サムネイル生成
Thumbnails.of(file.getInputStream())
    .size(200, 200)
    .toFile(thumbnailPath.toFile());
```

---

### 課題2: ファイルメタデータの管理

アップロードされたファイルのメタデータ（サイズ、MIMEタイプ、アップロード日時など）をデータベースで管理する機能を実装してください。

**実装のヒント**:
1. `FileMetadata`エンティティを作成
2. `FileMetadataRepository`を作成
3. `FileStorageService`でメタデータを保存
4. 管理画面でファイル一覧を表示

---

### 課題3: 全文検索エンジンの統合

Elasticsearchを使った高速な全文検索機能を実装してください。

**実装のヒント**:
1. Docker ComposeでElasticsearchを起動
2. Spring Data Elasticsearchを依存関係に追加
3. `ArticleDocument`を作成してインデックス化
4. `ArticleSearchRepository`で検索

---

## 🔧 トラブルシューティング

### 問題1: ファイルアップロードで413エラー

**エラー**:
```
413 Payload Too Large
```

**原因**: ファイルサイズが制限を超えている

**解決方法**:
```yaml
# application.yml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```

---

### 問題2: パストラバーサル攻撃が検出される

**エラー**:
```
不正なファイル名が含まれています: ../../../etc/passwd
```

**原因**: ファイル名に`..`が含まれている

**解決方法**:
- `FileStorageService`の検証ロジックが正しく動作している証拠
- クライアント側でファイル名をサニタイズする

---

### 問題3: MyBatisマッパーが見つからない

**エラー**:
```
org.apache.ibatis.binding.BindingException: Invalid bound statement (not found)
```

**原因**: マッパーXMLのnamespaceが間違っている、またはファイルが読み込まれていない

**解決方法**:
```yaml
# application.yml
mybatis:
  mapper-locations: classpath:mapper/**/*.xml
```

```xml
<!-- ArticleSearchMapper.xml -->
<mapper namespace="com.example.bloghub.mapper.ArticleSearchMapper">
```

---

### 問題4: 動的SQLで結果が空になる

**症状**: 検索条件を指定しても結果が0件

**原因**: `<where>`タグや`<if>`タグの条件が正しくない

**解決方法**:
```xml
<!-- パラメータのnullチェックと空文字チェック -->
<if test="keyword != null and keyword != ''">
    (a.title LIKE CONCAT('%', #{keyword}, '%'))
</if>
```

**デバッグ方法**:
```yaml
# application.yml
mybatis:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

---

### 問題5: プロフィール画像が表示されない

**症状**: 画像URLにアクセスすると404エラー

**原因**: ファイルパスの解決に失敗している

**解決方法**:
1. `uploads/`ディレクトリが存在するか確認
2. ファイル名が正しいか確認
3. `FileController`の`/{fileName:.+}`パターンが正しいか確認

```bash
# アップロードディレクトリの確認
ls -la uploads/
```

---

## 📚 まとめ

このステップでは、以下の内容を学びました：

1. **ファイルアップロードのセキュリティ対策**
   - パストラバーサル攻撃の防止
   - ファイルサイズと拡張子の検証
   - ユニークなファイル名の生成

2. **FileStorageServiceの実装**
   - ファイルの保存、読み込み、削除
   - セキュアなファイル管理

3. **プロフィール画像機能**
   - 画像のアップロードとダウンロード
   - 既存画像の自動削除

4. **MyBatisによる動的SQL**
   - `<where>`、`<if>`、`<foreach>`タグ
   - 複数条件での検索

5. **高度な検索機能**
   - キーワード検索（LIKE句）
   - タグ検索（IN句）
   - ユーザー名検索
   - ページネーション

6. **ResultMapによる結果マッピング**
   - 複雑なオブジェクト構造の変換
   - コレクションのマッピング

7. **セキュリティのベストプラクティス**
   - 入力値のサニタイズ
   - ファイル検証
   - 所有者チェック

8. **エラーハンドリング**
   - ファイル操作の例外処理
   - わかりやすいエラーメッセージ

9. **パフォーマンス最適化**
   - ページネーションによるデータ量制限
   - インデックスを活用した検索

10. **テストと検証**
    - curlコマンドでの動作確認
    - エラーケースのテスト

これで、BlogHubプロジェクトの主要機能がほぼ完成しました！次のステップでは、テストとデプロイの準備を行います。

---

## 🚀 次のステップへ

次は**Step 38: テストとデプロイ準備**に進みましょう！

**Step 38で学ぶこと**:
- ユニットテストの追加
- 統合テストの実装
- テストカバレッジの確認
- Docker化
- デプロイの準備

[→ Step 38に進む](STEP_38.md)
