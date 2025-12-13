# Step 37: 画像アップロードと検索機能

## 🎯 このステップの目標

- 記事への画像アップロード機能を実装できる
- プロフィール画像のアップロードができる
- MyBatisで複雑な検索クエリを実装できる
- 複数条件での記事検索ができる
- ファイルアップロードのセキュリティ対策ができる

**所要時間**: 約80分

---

## 📋 事前準備

- Step 36までの内容を完了していること
- Phase 7 Step 30のファイルアップロード機能を復習していること
- MyBatis設定が完了していること

---

## 🚀 ステップ1: ファイルストレージサービスの実装

### 1-1. FileStorageServiceの作成

`src/main/java/com/example/bloghub/services/FileStorageService.java`を作成：

```java
package com.example.bloghub.services;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
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
import java.util.UUID;

/**
 * ファイルストレージサービス
 */
@Service
@Slf4j
public class FileStorageService {
    
    @Value("${file.upload-dir}")
    private String uploadDir;
    
    private Path fileStorageLocation;
    
    @PostConstruct
    public void init() {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        
        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("File storage directory created at: {}", this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory for file upload", ex);
        }
    }
    
    /**
     * ファイル保存
     */
    public String storeFile(MultipartFile file) {
        // ファイル名のサニタイズ
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        
        try {
            // パストラバーサル対策
            if (originalFilename.contains("..")) {
                throw new RuntimeException("Invalid path sequence: " + originalFilename);
            }
            
            // ファイルサイズチェック（5MB制限）
            if (file.getSize() > 5 * 1024 * 1024) {
                throw new RuntimeException("File size exceeds maximum limit (5MB)");
            }
            
            // 拡張子チェック
            String extension = getFileExtension(originalFilename);
            if (!isAllowedExtension(extension)) {
                throw new RuntimeException("File type not allowed: " + extension);
            }
            
            // ユニークなファイル名生成
            String storedFilename = UUID.randomUUID().toString() + "." + extension;
            
            // ファイル保存
            Path targetLocation = this.fileStorageLocation.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            log.info("File stored: {}", storedFilename);
            return storedFilename;
            
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFilename, ex);
        }
    }
    
    /**
     * ファイル読み込み
     */
    public Resource loadFileAsResource(String filename) {
        try {
            Path filePath = this.fileStorageLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found: " + filename);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found: " + filename, ex);
        }
    }
    
    /**
     * ファイル削除
     */
    public void deleteFile(String filename) {
        try {
            Path filePath = this.fileStorageLocation.resolve(filename).normalize();
            Files.deleteIfExists(filePath);
            log.info("File deleted: {}", filename);
        } catch (IOException ex) {
            log.error("Could not delete file: {}", filename, ex);
        }
    }
    
    /**
     * 拡張子取得
     */
    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(dotIndex + 1).toLowerCase() : "";
    }
    
    /**
     * 許可する拡張子のチェック
     */
    private boolean isAllowedExtension(String extension) {
        return extension.matches("jpg|jpeg|png|gif|webp");
    }
}
```

### 1-2. FileControllerの作成

`src/main/java/com/example/bloghub/controllers/FileController.java`を作成：

```java
package com.example.bloghub.controllers;

import com.example.bloghub.services.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * ファイルアップロードコントローラー
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {
    
    private final FileStorageService fileStorageService;
    
    /**
     * ファイルアップロード
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        String filename = fileStorageService.storeFile(file);
        
        String fileUrl = "/api/files/" + filename;
        
        Map<String, String> response = new HashMap<>();
        response.put("filename", filename);
        response.put("url", fileUrl);
        response.put("size", String.valueOf(file.getSize()));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * ファイルダウンロード
     */
    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        Resource resource = fileStorageService.loadFileAsResource(filename);
        
        String contentType = "application/octet-stream";
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
```

---

## 🚀 ステップ2: プロフィール画像アップロード

### 2-1. UserServiceの拡張

`src/main/java/com/example/bloghub/services/UserService.java`を作成：

```java
package com.example.bloghub.services;

import com.example.bloghub.dto.response.UserResponse;
import com.example.bloghub.entities.User;
import com.example.bloghub.repositories.UserRepository;
import com.example.bloghub.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * ユーザーサービス
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    
    /**
     * プロフィール画像アップロード
     */
    @Transactional
    public UserResponse uploadProfileImage(MultipartFile file) {
        UserPrincipal userPrincipal = getCurrentUserPrincipal();
        User user = userRepository.findById(userPrincipal.getId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // 古い画像を削除
        if (user.getProfileImage() != null) {
            fileStorageService.deleteFile(extractFilename(user.getProfileImage()));
        }
        
        // 新しい画像を保存
        String filename = fileStorageService.storeFile(file);
        String imageUrl = "/api/files/" + filename;
        
        user.setProfileImage(imageUrl);
        User updatedUser = userRepository.save(user);
        
        log.info("Profile image updated for user: {}", user.getUsername());
        
        return UserResponse.from(updatedUser);
    }
    
    /**
     * URLからファイル名を抽出
     */
    private String extractFilename(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }
    
    /**
     * 現在のユーザー取得
     */
    private UserPrincipal getCurrentUserPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();
    }
}
```

### 2-2. UserControllerの拡張

`src/main/java/com/example/bloghub/controllers/UserController.java`を作成：

```java
package com.example.bloghub.controllers;

import com.example.bloghub.dto.response.UserResponse;
import com.example.bloghub.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * ユーザーコントローラー
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    /**
     * プロフィール画像アップロード
     */
    @PostMapping("/profile-image")
    public ResponseEntity<UserResponse> uploadProfileImage(@RequestParam("file") MultipartFile file) {
        UserResponse response = userService.uploadProfileImage(file);
        return ResponseEntity.ok(response);
    }
}
```

---

## 🚀 ステップ3: MyBatisで複雑な検索クエリ

### 3-1. ArticleSearchMapperの作成

`src/main/java/com/example/bloghub/mappers/ArticleSearchMapper.java`を作成：

```java
package com.example.bloghub.mappers;

import com.example.bloghub.dto.response.ArticleResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 記事検索用MyBatisマッパー
 */
@Mapper
public interface ArticleSearchMapper {
    
    /**
     * 複合条件検索
     */
    List<ArticleResponse> searchArticles(
        @Param("keyword") String keyword,
        @Param("authorId") Long authorId,
        @Param("tagName") String tagName,
        @Param("minViewCount") Integer minViewCount,
        @Param("offset") Integer offset,
        @Param("limit") Integer limit
    );
    
    /**
     * 検索結果の総件数
     */
    Long countSearchResults(
        @Param("keyword") String keyword,
        @Param("authorId") Long authorId,
        @Param("tagName") String tagName,
        @Param("minViewCount") Integer minViewCount
    );
}
```

### 3-2. MyBatisマッパーXMLの作成

`src/main/resources/mybatis/mapper/ArticleSearchMapper.xml`を作成：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.example.bloghub.mappers.ArticleSearchMapper">
    
    <!-- 結果マッピング -->
    <resultMap id="ArticleResponseMap" type="com.example.bloghub.dto.response.ArticleResponse">
        <id property="id" column="article_id"/>
        <result property="title" column="title"/>
        <result property="content" column="content"/>
        <result property="imageUrl" column="image_url"/>
        <result property="viewCount" column="view_count"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
        
        <association property="author" javaType="com.example.bloghub.dto.response.ArticleResponse$AuthorResponse">
            <id property="id" column="author_id"/>
            <result property="username" column="username"/>
            <result property="profileImage" column="profile_image"/>
        </association>
    </resultMap>
    
    <!-- 複合条件検索 -->
    <select id="searchArticles" resultMap="ArticleResponseMap">
        SELECT DISTINCT
            a.id AS article_id,
            a.title,
            a.content,
            a.image_url,
            a.view_count,
            a.created_at,
            a.updated_at,
            u.id AS author_id,
            u.username,
            u.profile_image
        FROM article a
        INNER JOIN user u ON a.author_id = u.id
        <if test="tagName != null">
            INNER JOIN article_tag at ON a.id = at.article_id
            INNER JOIN tag t ON at.tag_id = t.id
        </if>
        <where>
            <if test="keyword != null and keyword != ''">
                (a.title LIKE CONCAT('%', #{keyword}, '%')
                OR a.content LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="authorId != null">
                AND a.author_id = #{authorId}
            </if>
            <if test="tagName != null and tagName != ''">
                AND t.name = #{tagName}
            </if>
            <if test="minViewCount != null">
                AND a.view_count >= #{minViewCount}
            </if>
        </where>
        ORDER BY a.created_at DESC
        <if test="limit != null">
            LIMIT #{limit}
        </if>
        <if test="offset != null">
            OFFSET #{offset}
        </if>
    </select>
    
    <!-- 検索結果件数 -->
    <select id="countSearchResults" resultType="long">
        SELECT COUNT(DISTINCT a.id)
        FROM article a
        INNER JOIN user u ON a.author_id = u.id
        <if test="tagName != null">
            INNER JOIN article_tag at ON a.id = at.article_id
            INNER JOIN tag t ON at.tag_id = t.id
        </if>
        <where>
            <if test="keyword != null and keyword != ''">
                (a.title LIKE CONCAT('%', #{keyword}, '%')
                OR a.content LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="authorId != null">
                AND a.author_id = #{authorId}
            </if>
            <if test="tagName != null and tagName != ''">
                AND t.name = #{tagName}
            </if>
            <if test="minViewCount != null">
                AND a.view_count >= #{minViewCount}
            </if>
        </where>
    </select>
</mapper>
```

### 3-3. 検索サービスの作成

`src/main/java/com/example/bloghub/services/ArticleSearchService.java`を作成：

```java
package com.example.bloghub.services;

import com.example.bloghub.dto.response.ArticleResponse;
import com.example.bloghub.dto.response.PageResponse;
import com.example.bloghub.mappers.ArticleSearchMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 記事検索サービス
 */
@Service
@RequiredArgsConstructor
public class ArticleSearchService {
    
    private final ArticleSearchMapper articleSearchMapper;
    
    /**
     * 複合条件検索
     */
    public PageResponse<ArticleResponse> advancedSearch(
            String keyword,
            Long authorId,
            String tagName,
            Integer minViewCount,
            int page,
            int size) {
        
        int offset = page * size;
        
        List<ArticleResponse> articles = articleSearchMapper.searchArticles(
            keyword, authorId, tagName, minViewCount, offset, size);
        
        Long totalElements = articleSearchMapper.countSearchResults(
            keyword, authorId, tagName, minViewCount);
        
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        return PageResponse.<ArticleResponse>builder()
            .content(articles)
            .pageNumber(page)
            .pageSize(size)
            .totalElements(totalElements)
            .totalPages(totalPages)
            .first(page == 0)
            .last(page >= totalPages - 1)
            .empty(articles.isEmpty())
            .build();
    }
}
```

### 3-4. 検索コントローラーの拡張

`ArticleController`に検索エンドポイントを追加：

```java
// ArticleController.java に追加

private final ArticleSearchService articleSearchService;

/**
 * 高度な検索
 */
@GetMapping("/advanced-search")
public ResponseEntity<PageResponse<ArticleResponse>> advancedSearch(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long authorId,
        @RequestParam(required = false) String tag,
        @RequestParam(required = false) Integer minViews,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
    
    PageResponse<ArticleResponse> response = articleSearchService.advancedSearch(
        keyword, authorId, tag, minViews, page, size);
    
    return ResponseEntity.ok(response);
}
```

---

## ✅ 動作確認

### 1. 画像アップロード

```bash
TOKEN="<JWTトークン>"

curl -X POST http://localhost:8080/api/files/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/image.jpg"
```

**期待される結果**:
```json
{
  "filename": "a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg",
  "url": "/api/files/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg",
  "size": "245678"
}
```

### 2. プロフィール画像アップロード

```bash
curl -X POST http://localhost:8080/api/users/profile-image \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/profile.jpg"
```

### 3. 画像付き記事投稿

```bash
# まず画像をアップロード
IMAGE_URL=$(curl -X POST http://localhost:8080/api/files/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/image.jpg" | jq -r '.url')

# 記事投稿
curl -X POST http://localhost:8080/api/articles \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"画像付き記事\",
    \"content\": \"画像を含む記事です\",
    \"imageUrl\": \"$IMAGE_URL\",
    \"tags\": [\"開発\", \"Tips\"]
  }"
```

### 4. 高度な検索

```bash
# キーワード + タグで検索
curl "http://localhost:8080/api/articles/advanced-search?keyword=Spring&tag=Java&page=0&size=10"

# 閲覧数100以上の記事を検索
curl "http://localhost:8080/api/articles/advanced-search?minViews=100"
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: サムネイル生成

**目標**: アップロードされた画像から自動的にサムネイルを生成

**ヒント**:
```java
// imgscalr ライブラリを使用
<dependency>
    <groupId>org.imgscalr</groupId>
    <artifactId>imgscalr-lib</artifactId>
    <version>4.2</version>
</dependency>

BufferedImage thumbnail = Scalr.resize(originalImage, 300);
```

### チャレンジ 2: 全文検索エンジン

**目標**: MySQLのFULLTEXT INDEXを使った高速な全文検索

**ヒント**:
```sql
ALTER TABLE article ADD FULLTEXT INDEX idx_fulltext (title, content);

SELECT * FROM article 
WHERE MATCH(title, content) AGAINST('Spring Boot' IN NATURAL LANGUAGE MODE);
```

### チャレンジ 3: 画像圧縮

**目標**: アップロード時に画像を自動圧縮してストレージを節約

---

## 🐛 トラブルシューティング

### エラー: "File type not allowed"

**原因**: 許可されていない拡張子のファイルをアップロード

**解決策**: jpg, jpeg, png, gif, webpのみ許可されています

### エラー: "File size exceeds maximum limit"

**原因**: 5MBを超えるファイルをアップロード

**解決策**: ファイルサイズを5MB以下に縮小してください

### エラー: "Could not create the directory"

**原因**: アップロードディレクトリの作成権限がない

**解決策**: アプリケーション実行ユーザーに書き込み権限を付与

### エラー: MyBatisマッパーが見つからない

**原因**: `@MapperScan`が設定されていない、またはXMLのパスが間違っている

**解決策**:
```java
@SpringBootApplication
@MapperScan("com.example.bloghub.mappers")
public class BlogHubApplication { }
```

---

## 📚 このステップで学んだこと

- ✅ ファイルアップロードのセキュリティ対策を実装した
- ✅ プロフィール画像のアップロード機能を実装した
- ✅ MyBatisで動的SQLを使った複雑な検索クエリを実装した
- ✅ 複数条件での記事検索を実装した
- ✅ ファイルストレージの管理方法を理解した
- ✅ パストラバーサル攻撃への対策を学んだ

---

## ➡️ 次のステップ

[Step 38: テストとデプロイ準備](STEP_38.md)へ進みましょう！

最終ステップでは、アプリケーションの品質を保証するテストと、本番環境へのデプロイ準備を行います：
- ユニットテスト
- 統合テスト
- テストカバレッジ
- 本番環境設定
- デプロイメント戦略

プロジェクトの完成まであと一歩です！🚀
