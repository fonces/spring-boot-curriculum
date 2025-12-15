# Step 30: ファイルアップロード

## 🎯 このステップの目標

- `MultipartFile`を使ってファイルのアップロードを受け取る方法を理解する
- ファイルをサーバーに保存し、メタデータをデータベースで管理できる
- ファイルダウンロードAPIを実装できる
- ファイルサイズや拡張子の制限を設定できる
- セキュアなファイルアップロード機能を実装できる

**所要時間**: 約45分

---

## 📋 事前準備

- Phase 6までの内容を完了していること
- Spring Bootアプリケーションが起動できること
- データベース（MySQL）が稼働していること
- curlまたはPostmanでファイル送信ができること

---

## 🚀 ステップ1: 依存関係とファイルストレージ設定

### 1-1. application.ymlにファイルアップロード設定を追加

`src/main/resources/application.yaml`にファイルアップロードの設定を追加します：

```yaml
spring:
  application:
    name: hello-spring-boot
  
  # データソース設定
  datasource:
    url: jdbc:mysql://localhost:3306/spring_boot_db
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  # JPA設定
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
  
  # セキュリティ設定
  security:
    user:
      name: admin
      password: admin
  
  # ファイルアップロード設定
  servlet:
    multipart:
      enabled: true
      max-file-size: 10MB        # 1ファイルの最大サイズ
      max-request-size: 20MB     # リクエスト全体の最大サイズ
      file-size-threshold: 2MB   # メモリに保持する閾値

# JWT設定
jwt:
  secret: my-super-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm-to-work-properly
  expiration: 86400000

# ファイル保存先ディレクトリ
file:
  upload-dir: uploads
```

### 1-2. コードの解説

#### ファイルアップロード設定

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 20MB
```

- **max-file-size**: 1つのファイルの最大サイズ（これを超えるとエラー）
- **max-request-size**: リクエスト全体の最大サイズ（複数ファイルの合計）
- **file-size-threshold**: この閾値を超えるとディスクに一時保存

---

## 🚀 ステップ2: ファイルエンティティとリポジトリ作成

### 2-1. FileMetadataエンティティを作成

`src/main/java/com/example/hellospringboot/entities/FileMetadata.java`を作成：

```java
package com.example.hellospringboot.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ファイルメタデータエンティティ
 */
@Entity
@Table(name = "file_metadata")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileMetadata {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 元のファイル名
     */
    @Column(nullable = false)
    private String originalFilename;
    
    /**
     * 保存されたファイル名（UUID + 拡張子）
     */
    @Column(nullable = false, unique = true)
    private String storedFilename;
    
    /**
     * ファイルパス
     */
    @Column(nullable = false)
    private String filePath;
    
    /**
     * ファイルサイズ（バイト）
     */
    @Column(nullable = false)
    private Long fileSize;
    
    /**
     * MIMEタイプ
     */
    @Column(nullable = false)
    private String contentType;
    
    /**
     * アップロード日時
     */
    @Column(nullable = false)
    private LocalDateTime uploadedAt;
    
    /**
     * アップロードしたユーザーID
     */
    @Column(nullable = false)
    private Long uploadedBy;
    
    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}
```

### 2-2. FileMetadataRepositoryを作成

`src/main/java/com/example/hellospringboot/repositories/FileMetadataRepository.java`を作成：

```java
package com.example.hellospringboot.repositories;

import com.example.hellospringboot.entities.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ファイルメタデータリポジトリ
 */
@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    
    /**
     * 保存されたファイル名で検索
     */
    Optional<FileMetadata> findByStoredFilename(String storedFilename);
    
    /**
     * アップロードしたユーザーIDで検索
     */
    List<FileMetadata> findByUploadedBy(Long uploadedBy);
}
```

---

## 🚀 ステップ3: ファイルストレージサービス

### 3-1. FileStorageServiceを作成

`src/main/java/com/example/hellospringboot/services/FileStorageService.java`を作成：

```java
package com.example.hellospringboot.services;

import com.example.hellospringboot.entities.FileMetadata;
import com.example.hellospringboot.exceptions.ResourceNotFoundException;
import com.example.hellospringboot.repositories.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

/**
 * ファイルストレージサービス
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {
    
    @Value("${file.upload-dir}")
    private String uploadDir;
    
    private final FileMetadataRepository fileMetadataRepository;
    
    private Path fileStorageLocation;
    
    /**
     * 初期化: アップロードディレクトリを作成
     */
    @PostConstruct
    public void init() {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        
        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("File storage directory created: {}", this.fileStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create upload directory!", ex);
        }
    }
    
    /**
     * ファイルを保存
     */
    public FileMetadata storeFile(MultipartFile file, Long userId) {
        // ファイル名の正規化
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        
        try {
            // ファイル名のバリデーション
            if (originalFilename.contains("..")) {
                throw new IllegalArgumentException("Invalid file path: " + originalFilename);
            }
            
            // ユニークなファイル名を生成
            String fileExtension = getFileExtension(originalFilename);
            String storedFilename = UUID.randomUUID().toString() + fileExtension;
            
            // ファイルを保存
            Path targetLocation = this.fileStorageLocation.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            // メタデータをDBに保存
            FileMetadata metadata = FileMetadata.builder()
                    .originalFilename(originalFilename)
                    .storedFilename(storedFilename)
                    .filePath(targetLocation.toString())
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .uploadedBy(userId)
                    .build();
            
            FileMetadata saved = fileMetadataRepository.save(metadata);
            log.info("File stored: {} -> {}", originalFilename, storedFilename);
            
            return saved;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFilename, ex);
        }
    }
    
    /**
     * ファイルをロード
     */
    public Resource loadFileAsResource(String filename) {
        try {
            Path filePath = this.fileStorageLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File", "filename", filename);
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("File", "filename", filename);
        }
    }
    
    /**
     * ファイルメタデータを取得
     */
    public FileMetadata getFileMetadata(Long id) {
        return fileMetadataRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FileMetadata", "id", id));
    }
    
    /**
     * ユーザーがアップロードした全ファイルを取得
     */
    public List<FileMetadata> getUserFiles(Long userId) {
        return fileMetadataRepository.findByUploadedBy(userId);
    }
    
    /**
     * ファイル削除
     */
    public void deleteFile(Long id, Long userId) {
        FileMetadata metadata = getFileMetadata(id);
        
        // 権限チェック（自分がアップロードしたファイルのみ削除可能）
        if (!metadata.getUploadedBy().equals(userId)) {
            throw new IllegalArgumentException("You don't have permission to delete this file");
        }
        
        try {
            // ファイルシステムから削除
            Path filePath = Paths.get(metadata.getFilePath());
            Files.deleteIfExists(filePath);
            
            // DBから削除
            fileMetadataRepository.delete(metadata);
            
            log.info("File deleted: {}", metadata.getStoredFilename());
        } catch (IOException ex) {
            throw new RuntimeException("Could not delete file: " + metadata.getOriginalFilename(), ex);
        }
    }
    
    /**
     * ファイル拡張子を取得
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
```

### 3-2. コードの解説

#### `@PostConstruct`
```java
@PostConstruct
public void init() {
    Files.createDirectories(this.fileStorageLocation);
}
```
- Bean作成後に自動的に実行される
- アップロードディレクトリを作成

#### `MultipartFile`
```java
public FileMetadata storeFile(MultipartFile file, Long userId)
```
- ファイルアップロードを受け取るSpring標準のインターフェース
- `getInputStream()`, `getSize()`, `getContentType()`などのメソッドを提供

#### セキュリティ対策
```java
if (originalFilename.contains("..")) {
    throw new IllegalArgumentException("Invalid file path");
}
```
- **パストラバーサル攻撃**を防ぐ
- `../../../etc/passwd`のような危険なパスを拒否

#### ユニークなファイル名生成
```java
String storedFilename = UUID.randomUUID().toString() + fileExtension;
```
- 同名ファイルの上書きを防ぐ
- ファイル名の衝突を回避

---

## 🚀 ステップ4: ファイルアップロードAPI

### 4-1. FileControllerを作成

`src/main/java/com/example/hellospringboot/controllers/FileController.java`を作成：

```java
package com.example.hellospringboot.controllers;

import com.example.hellospringboot.entities.FileMetadata;
import com.example.hellospringboot.services.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ファイルアップロード/ダウンロードAPI
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
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", defaultValue = "1") Long userId) {
        
        log.info("File upload request: {}, size: {} bytes", 
                file.getOriginalFilename(), file.getSize());
        
        // ファイル保存
        FileMetadata metadata = fileStorageService.storeFile(file, userId);
        
        // ダウンロードURLを生成
        String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/files/download/")
                .path(metadata.getStoredFilename())
                .toUriString();
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", metadata.getId());
        response.put("filename", metadata.getOriginalFilename());
        response.put("storedFilename", metadata.getStoredFilename());
        response.put("size", metadata.getFileSize());
        response.put("contentType", metadata.getContentType());
        response.put("downloadUrl", downloadUrl);
        response.put("uploadedAt", metadata.getUploadedAt());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 複数ファイルアップロード
     */
    @PostMapping(value = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadMultipleFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "userId", defaultValue = "1") Long userId) {
        
        log.info("Multiple file upload request: {} files", files.length);
        
        List<FileMetadata> uploadedFiles = java.util.Arrays.stream(files)
                .map(file -> fileStorageService.storeFile(file, userId))
                .toList();
        
        Map<String, Object> response = new HashMap<>();
        response.put("count", uploadedFiles.size());
        response.put("files", uploadedFiles);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * ファイルダウンロード
     */
    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        log.info("File download request: {}", filename);
        
        // ファイルをロード
        Resource resource = fileStorageService.loadFileAsResource(filename);
        
        // Content-Dispositionヘッダーを設定（ブラウザにダウンロードさせる）
        String contentDisposition = "attachment; filename=\"" + resource.getFilename() + "\"";
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(resource);
    }
    
    /**
     * ファイルメタデータ取得
     */
    @GetMapping("/{id}")
    public ResponseEntity<FileMetadata> getFileMetadata(@PathVariable Long id) {
        FileMetadata metadata = fileStorageService.getFileMetadata(id);
        return ResponseEntity.ok(metadata);
    }
    
    /**
     * ユーザーのファイル一覧取得
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<FileMetadata>> getUserFiles(@PathVariable Long userId) {
        List<FileMetadata> files = fileStorageService.getUserFiles(userId);
        return ResponseEntity.ok(files);
    }
    
    /**
     * ファイル削除
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable Long id,
            @RequestParam(value = "userId", defaultValue = "1") Long userId) {
        
        log.info("File delete request: id={}, userId={}", id, userId);
        fileStorageService.deleteFile(id, userId);
        
        return ResponseEntity.noContent().build();
    }
}
```

### 4-2. コードの解説

#### `consumes = MediaType.MULTIPART_FORM_DATA_VALUE`
```java
@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
```
- ファイルアップロードは`multipart/form-data`形式
- このContent-Typeのみ受け付ける

#### `@RequestParam("file") MultipartFile file`
```java
public ResponseEntity<Map<String, Object>> uploadFile(
    @RequestParam("file") MultipartFile file)
```
- フォームフィールド名`file`でファイルを受け取る
- `MultipartFile`はSpring標準のファイルインターフェース

#### ダウンロードURL生成
```java
String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
    .path("/api/files/download/")
    .path(metadata.getStoredFilename())
    .toUriString();
```
- 現在のドメインとポートを取得
- 完全なダウンロードURLを生成

#### ファイルダウンロードレスポンス
```java
return ResponseEntity.ok()
    .contentType(MediaType.APPLICATION_OCTET_STREAM)
    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
    .body(resource);
```
- `APPLICATION_OCTET_STREAM`: バイナリデータ
- `Content-Disposition: attachment`: ブラウザにダウンロードさせる

---

## ✅ 動作確認

### 1. アプリケーションを起動

```bash
cd /path/to/hello-spring-boot
./mvnw spring-boot:run
```

### 2. ファイルアップロード

```bash
# テスト用の画像ファイルを作成
echo "test file content" > test.txt

# ファイルをアップロード
curl -X POST http://localhost:8080/api/files \
  -H "Content-Type: multipart/form-data" \
  -F "file=@test.txt" \
  -F "userId=1"
```

**期待される結果**:
```json
{
  "id": 1,
  "filename": "test.txt",
  "storedFilename": "a1b2c3d4-e5f6-7890-abcd-ef1234567890.txt",
  "size": 17,
  "contentType": "text/plain",
  "downloadUrl": "http://localhost:8080/api/files/download/a1b2c3d4-e5f6-7890-abcd-ef1234567890.txt",
  "uploadedAt": "2025-12-13T18:30:00"
}
```

### 3. ファイルダウンロード

```bash
# アップロードしたファイルをダウンロード
curl -O http://localhost:8080/api/files/download/a1b2c3d4-e5f6-7890-abcd-ef1234567890.txt

# ダウンロードしたファイルの内容を確認
cat a1b2c3d4-e5f6-7890-abcd-ef1234567890.txt
```

**期待される結果**:
```sh
test file content
```

### 4. ファイル一覧取得

```bash
curl http://localhost:8080/api/files/user/1
```

**期待される結果**:
```json
[
  {
    "id": 1,
    "originalFilename": "test.txt",
    "storedFilename": "a1b2c3d4-e5f6-7890-abcd-ef1234567890.txt",
    "filePath": "/path/to/uploads/a1b2c3d4-e5f6-7890-abcd-ef1234567890.txt",
    "fileSize": 17,
    "contentType": "text/plain",
    "uploadedAt": "2025-12-13T18:30:00",
    "uploadedBy": 1
  }
]
```

### 5. 複数ファイルアップロード

```bash
# 複数ファイルをまとめてアップロード
curl -X POST http://localhost:8080/api/files/batch \
  -H "Content-Type: multipart/form-data" \
  -F "files=@test1.txt" \
  -F "files=@test2.txt" \
  -F "userId=1"
```

### 6. ファイル削除

```bash
curl -X DELETE "http://localhost:8080/api/files/1?userId=1"
```

**期待される結果**: HTTPステータス204 No Content

---

## 🎨 チャレンジ課題

基本が理解できたら、以下にチャレンジしてみましょう：

### チャレンジ 1: 画像のサムネイル生成

**目標**: アップロードされた画像のサムネイルを自動生成

**ヒント**:
```java
// Java標準のImageIOを使用
BufferedImage originalImage = ImageIO.read(file.getInputStream());
BufferedImage thumbnail = Scalr.resize(originalImage, 200);
ImageIO.write(thumbnail, "jpg", thumbnailFile);
```

**必要な依存関係**:
```xml
<dependency>
    <groupId>org.imgscalr</groupId>
    <artifactId>imgscalr-lib</artifactId>
    <version>4.2</version>
</dependency>
```

### チャレンジ 2: ファイルタイプのバリデーション

**目標**: 画像ファイル（JPG, PNG, GIF）のみアップロード可能にする

**ヒント**:
```java
private static final List<String> ALLOWED_TYPES = 
    Arrays.asList("image/jpeg", "image/png", "image/gif");

if (!ALLOWED_TYPES.contains(file.getContentType())) {
    throw new IllegalArgumentException("Only image files are allowed");
}
```

### チャレンジ 3: S3へのアップロード（AWS）

**目標**: ローカルファイルシステムではなくAmazon S3にファイルを保存

**ヒント**:
```java
// AWS SDK for Java 2.x
S3Client s3Client = S3Client.builder().build();

PutObjectRequest putObjectRequest = PutObjectRequest.builder()
    .bucket("your-bucket-name")
    .key(storedFilename)
    .build();

s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(
    file.getInputStream(), file.getSize()));
```

---

## 🐛 トラブルシューティング

### エラー: "Maximum upload size exceeded"

**原因**: ファイルサイズが`max-file-size`を超えている

**解決策**:
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 50MB  # サイズを増やす
      max-request-size: 50MB
```

### エラー: "Could not create upload directory"

**原因**: ディレクトリ作成権限がない、またはパスが不正

**解決策**:
1. ディレクトリの権限を確認
```bash
ls -ld uploads/
chmod 755 uploads/
```

2. 絶対パスを指定
```yaml
file:
  upload-dir: /var/uploads  # 絶対パス
```

### エラー: "The filename, directory name, or volume label syntax is incorrect"

**原因**: Windowsで不正なパス文字が使われている

**解決策**:
```java
// ファイル名から不正な文字を除去
String sanitizedFilename = originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
```

### ファイルが見つからない（404）

**原因**: ファイルパスが間違っている、またはファイルが削除されている

**デバッグ**:
```java
log.info("Looking for file at: {}", filePath);
log.info("File exists: {}", Files.exists(filePath));
```

### メモリ不足エラー

**原因**: 大きなファイルをメモリで処理している

**解決策**:
```yaml
spring:
  servlet:
    multipart:
      file-size-threshold: 1MB  # 1MBを超えたらディスクに一時保存
```

---

## 📚 このステップで学んだこと

- ✅ `MultipartFile`を使ったファイルアップロードの基本
- ✅ ファイルサイズとリクエストサイズの制限設定
- ✅ UUIDを使ったユニークなファイル名生成
- ✅ ファイルメタデータのデータベース管理
- ✅ ファイルダウンロードAPIの実装
- ✅ パストラバーサル攻撃の防止
- ✅ `@PostConstruct`での初期化処理
- ✅ `Resource`を使ったファイルレスポンス
- ✅ 複数ファイルのバッチアップロード
- ✅ セキュアなファイル管理（権限チェック）

---

## 💡 補足: ファイルアップロードのベストプラクティス

### 1. ファイルサイズ制限

**理由**: サーバーリソースの保護、DoS攻撃防止

**推奨設定**:
```yaml
max-file-size: 10MB    # 一般的な用途
max-file-size: 100MB   # 動画アップロード
max-file-size: 1GB     # 大容量ファイル
```

### 2. ファイルタイプ検証

**MIMEタイプだけでは不十分**:
```java
// ❌ 不十分（偽装可能）
if (file.getContentType().equals("image/jpeg")) { }

// ✅ 拡張子もチェック
String extension = getFileExtension(file.getOriginalFilename());
if (!extension.equals(".jpg") && !extension.equals(".jpeg")) {
    throw new IllegalArgumentException("Invalid file type");
}

// ✅✅ ファイル内容を検証（最も安全）
BufferedImage image = ImageIO.read(file.getInputStream());
if (image == null) {
    throw new IllegalArgumentException("Not a valid image file");
}
```

### 3. ウイルススキャン

本番環境では、アップロードされたファイルをウイルススキャンすることを推奨：

```java
// ClamAVなどのアンチウイルスライブラリを使用
if (virusScanner.isInfected(file)) {
    throw new SecurityException("File contains virus");
}
```

### 4. ストレージの選択

| ストレージ | メリット | デメリット | 使用例 |
|---|---|---|---|
| **ローカルファイルシステム** | シンプル、コスト0 | スケーラビリティ低、バックアップ必要 | 開発環境 |
| **Amazon S3** | スケーラブル、CDN統合 | コストがかかる | 本番環境 |
| **Azure Blob Storage** | Azureとの統合 | Azureベンダーロックイン | Azure環境 |
| **Google Cloud Storage** | GCPとの統合 | GCPベンダーロックイン | GCP環境 |

### 5. ファイル名の処理

**セキュリティ上の注意点**:
```java
// ❌ 危険（パストラバーサル攻撃）
String filename = file.getOriginalFilename();
Path targetPath = Paths.get(uploadDir, filename);  // ../../../etc/passwdが可能

// ✅ 安全（ファイル名を正規化）
String sanitizedFilename = StringUtils.cleanPath(file.getOriginalFilename());
if (sanitizedFilename.contains("..")) {
    throw new IllegalArgumentException("Invalid filename");
}

// ✅✅ 最も安全（元のファイル名を使わない）
String storedFilename = UUID.randomUUID().toString() + getFileExtension(filename);
```

### 6. ファイルの有効期限

**一時ファイルは定期的に削除**:
```java
@Scheduled(cron = "0 0 2 * * *")  // 毎日午前2時
public void cleanupExpiredFiles() {
    LocalDateTime expirationDate = LocalDateTime.now().minusDays(30);
    List<FileMetadata> expiredFiles = fileMetadataRepository
        .findByUploadedAtBefore(expirationDate);
    
    expiredFiles.forEach(file -> {
        deleteFile(file.getId(), file.getUploadedBy());
    });
}
```

---

## ➡️ 次のステップ

[Step 31: ページネーション](STEP_31.md)へ進みましょう！

次のステップでは、大量データを効率的に取得するためのページネーション機能を実装します。`Pageable`と`Page<T>`を使って、パフォーマンスの良いリストAPIを作成しましょう。
