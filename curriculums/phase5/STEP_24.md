# Step 24: ファイルアップロード・ダウンロード

## 🎯 このステップの目標

- ファイルアップロード機能を実装する
- ファイルダウンロード機能を実装する
- ファイルストレージの管理方法を学ぶ
- セキュリティ対策を理解する

**所要時間**: 約2時間

---

## 💡 ファイルアップロードのユースケース

- 📷 プロフィール画像
- 📄 PDFドキュメント
- 📊 CSVファイルのインポート
- 🎵 メディアファイル

---

## 🚀 ステップ1: 設定ファイル

### 1-1. application.ymlでファイル設定

```yaml
spring:
  servlet:
    multipart:
      enabled: true
      max-file-size: 10MB
      max-request-size: 10MB

# カスタム設定
file:
  upload-dir: ./uploads
  allowed-extensions: jpg,jpeg,png,gif,pdf,doc,docx
```

### 1-2. FileStorageProperties

**ファイルパス**: `src/main/java/com/example/hellospringboot/config/FileStorageProperties.java`

```java
package com.example.hellospringboot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "file")
public class FileStorageProperties {
    private String uploadDir;
    private String allowedExtensions;
}
```

---

## 🚀 ステップ2: ファイルストレージサービス

### 2-1. FileStorageService

**ファイルパス**: `src/main/java/com/example/hellospringboot/service/FileStorageService.java`

```java
package com.example.hellospringboot.service;

import com.example.hellospringboot.config.FileStorageProperties;
import com.example.hellospringboot.exception.FileStorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private final Path fileStorageLocation;
    private final String[] allowedExtensions;

    @Autowired
    public FileStorageService(FileStorageProperties fileStorageProperties) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath().normalize();
        this.allowedExtensions = fileStorageProperties.getAllowedExtensions().split(",");

        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("アップロードディレクトリを作成しました: {}", this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("アップロードディレクトリの作成に失敗しました。", ex);
        }
    }

    /**
     * ファイルをアップロードする
     */
    public String storeFile(MultipartFile file) {
        // ファイル名を取得
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            // ファイル名のバリデーション
            if (originalFilename.contains("..")) {
                throw new FileStorageException("不正なファイル名: " + originalFilename);
            }

            // 拡張子チェック
            String extension = getFileExtension(originalFilename);
            if (!isAllowedExtension(extension)) {
                throw new FileStorageException("許可されていない拡張子です: " + extension);
            }

            // ユニークなファイル名を生成
            String filename = UUID.randomUUID().toString() + "." + extension;

            // ファイルを保存
            Path targetLocation = this.fileStorageLocation.resolve(filename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("ファイルをアップロードしました: {}", filename);
            return filename;

        } catch (IOException ex) {
            throw new FileStorageException("ファイルの保存に失敗しました: " + originalFilename, ex);
        }
    }

    /**
     * ファイルを読み込む
     */
    public Resource loadFileAsResource(String filename) {
        try {
            Path filePath = this.fileStorageLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new FileStorageException("ファイルが見つかりません: " + filename);
            }
        } catch (MalformedURLException ex) {
            throw new FileStorageException("ファイルが見つかりません: " + filename, ex);
        }
    }

    /**
     * ファイルを削除する
     */
    public void deleteFile(String filename) {
        try {
            Path filePath = this.fileStorageLocation.resolve(filename).normalize();
            Files.deleteIfExists(filePath);
            log.info("ファイルを削除しました: {}", filename);
        } catch (IOException ex) {
            throw new FileStorageException("ファイルの削除に失敗しました: " + filename, ex);
        }
    }

    /**
     * 拡張子を取得
     */
    private String getFileExtension(String filename) {
        int lastIndexOf = filename.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return filename.substring(lastIndexOf + 1).toLowerCase();
    }

    /**
     * 許可された拡張子かチェック
     */
    private boolean isAllowedExtension(String extension) {
        return Arrays.asList(allowedExtensions).contains(extension);
    }
}
```

---

## 🚀 ステップ3: カスタム例外

### 3-1. FileStorageException

**ファイルパス**: `src/main/java/com/example/hellospringboot/exception/FileStorageException.java`

```java
package com.example.hellospringboot.exception;

public class FileStorageException extends RuntimeException {
    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

---

## 🚀 ステップ4: FileController

### 4-1. ファイルアップロード・ダウンロードAPI

**ファイルパス**: `src/main/java/com/example/hellospringboot/controller/FileController.java`

```java
package com.example.hellospringboot.controller;

import com.example.hellospringboot.dto.response.UploadFileResponse;
import com.example.hellospringboot.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "File", description = "ファイル管理API")
public class FileController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "ファイルアップロード", description = "単一ファイルをアップロードします")
    @PostMapping("/upload")
    public ResponseEntity<UploadFileResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        String filename = fileStorageService.storeFile(file);

        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/files/download/")
                .path(filename)
                .toUriString();

        return ResponseEntity.ok(UploadFileResponse.builder()
                .filename(filename)
                .fileDownloadUri(fileDownloadUri)
                .fileType(file.getContentType())
                .size(file.getSize())
                .build());
    }

    @Operation(summary = "複数ファイルアップロード", description = "複数ファイルをアップロードします")
    @PostMapping("/upload/multiple")
    public ResponseEntity<List<UploadFileResponse>> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
        List<UploadFileResponse> responses = Arrays.stream(files)
                .map(file -> {
                    String filename = fileStorageService.storeFile(file);
                    String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("/api/files/download/")
                            .path(filename)
                            .toUriString();

                    return UploadFileResponse.builder()
                            .filename(filename)
                            .fileDownloadUri(fileDownloadUri)
                            .fileType(file.getContentType())
                            .size(file.getSize())
                            .build();
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "ファイルダウンロード", description = "指定されたファイルをダウンロードします")
    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename, HttpServletRequest request) {
        Resource resource = fileStorageService.loadFileAsResource(filename);

        // Content-Typeを決定
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            log.info("ファイルタイプを判定できませんでした。");
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
```

---

## 🚀 ステップ5: UploadFileResponse DTO

**ファイルパス**: `src/main/java/com/example/hellospringboot/dto/response/UploadFileResponse.java`

```java
package com.example.hellospringboot.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "ファイルアップロードレスポンス")
public class UploadFileResponse {

    @Schema(description = "ファイル名", example = "550e8400-e29b-41d4-a716-446655440000.jpg")
    private String filename;

    @Schema(description = "ダウンロードURI", example = "http://localhost:8080/api/files/download/550e8400.jpg")
    private String fileDownloadUri;

    @Schema(description = "ファイルタイプ", example = "image/jpeg")
    private String fileType;

    @Schema(description = "ファイルサイズ（バイト）", example = "2048576")
    private long size;
}
```

---

## ✅ 動作確認

### ファイルアップロード

```bash
curl -X POST http://localhost:8080/api/files/upload \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/image.jpg"
```

**レスポンス例**:
```json
{
  "filename": "550e8400-e29b-41d4-a716-446655440000.jpg",
  "fileDownloadUri": "http://localhost:8080/api/files/download/550e8400-e29b-41d4-a716-446655440000.jpg",
  "fileType": "image/jpeg",
  "size": 2048576
}
```

### ファイルダウンロード

```bash
curl -O http://localhost:8080/api/files/download/550e8400-e29b-41d4-a716-446655440000.jpg
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: ユーザープロフィール画像

UserエンティティにprofileImageフィールドを追加してください。

### チャレンジ 2: 画像リサイズ

Thumbnailatorライブラリを使って画像をリサイズしてください。

### チャレンジ 3: AWS S3連携

ローカルストレージの代わりにAWS S3を使ってください。

---

## 📚 このステップで学んだこと

- ✅ MultipartFileの処理
- ✅ ファイルストレージの実装
- ✅ ファイルアップロード/ダウンロードAPI
- ✅ セキュリティ対策（拡張子チェック、パストラバーサル対策）

---

## 🔄 Gitへのコミット

```bash
git add .
git commit -m "Phase 5: STEP_24完了（ファイルアップロード・ダウンロード）"
git push origin main
```

---

## ➡️ 次のステップ

次は[Step 25: メール送信](STEP_25.md)へ進みましょう！

---

お疲れさまでした！ 🎉
