# Step 37: タグ機能と画像アップロード

## 🎯 このステップの目標

記事のタグ付け機能と、画像アップロード機能を実装します。
多対多のリレーションシップとファイル管理を学びます。

- 多対多リレーション（タグと記事）の実装
- ファイルアップロード機能
- 中間テーブルの操作
- 画像のバリデーションとセキュリティ

**所要時間**: 約1時間30分

## 📋 機能要件

### タグ機能
- 記事へのタグ付け（複数可）
- タグによる記事検索
- タグ一覧とタグごとの記事数表示
- 人気タグのランキング

### 画像アップロード機能
- 記事に画像を添付
- ユーザーアバター画像のアップロード
- 画像のプレビュー表示
- 画像の最適化（サイズ制限）

## 🗂️ データベース設計

### tagsテーブル
```sql
CREATE TABLE tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    slug VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_name (name)
);
```

### post_tagsテーブル（中間テーブル）
```sql
CREATE TABLE post_tags (
    post_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (post_id, tag_id),
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);
```

### imagesテーブル
```sql
CREATE TABLE images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    uploaded_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE
);
```

### postsテーブルに追加
```sql
ALTER TABLE posts
ADD COLUMN featured_image_id BIGINT,
ADD FOREIGN KEY (featured_image_id) REFERENCES images(id) ON DELETE SET NULL;
```

### usersテーブルに追加
```sql
ALTER TABLE users
ADD COLUMN avatar_image_id BIGINT,
ADD FOREIGN KEY (avatar_image_id) REFERENCES images(id) ON DELETE SET NULL;
```

## 💡 実装のヒント（タグ機能）

### 1. プロジェクト構成
以下のクラスを追加してください：

```
src/main/java/com/example/blog/
├── controller/
│   └── TagController.java
├── service/
│   └── TagService.java
├── repository/
│   ├── TagMapper.java (MyBatis)
│   └── PostTagMapper.java (MyBatis)
├── entity/
│   └── Tag.java
└── dto/
    ├── TagResponse.java
    └── TagWithCountResponse.java
```

### 2. MyBatisでの多対多リレーション
`PostMapper.xml`と`TagMapper.xml`で多対多の関連を扱ってください。

**例**:
```xml
<!-- PostMapper.xml にタグ情報も含める -->
<resultMap id="PostWithTagsAndAuthor" type="Post">
    <id property="id" column="id"/>
    <result property="title" column="title"/>
    <!-- 他のフィールド -->
    
    <association property="author" javaType="User">
        <id property="id" column="author_id"/>
        <result property="username" column="username"/>
    </association>
    
    <collection property="tags" ofType="Tag">
        <id property="id" column="tag_id"/>
        <result property="name" column="tag_name"/>
        <result property="slug" column="tag_slug"/>
    </collection>
</resultMap>

<select id="findByIdWithTags" resultMap="PostWithTagsAndAuthor">
    SELECT 
        p.*,
        u.username,
        u.display_name,
        t.id as tag_id,
        t.name as tag_name,
        t.slug as tag_slug
    FROM posts p
    INNER JOIN users u ON p.author_id = u.id
    LEFT JOIN post_tags pt ON p.id = pt.post_id
    LEFT JOIN tags t ON pt.tag_id = t.id
    WHERE p.id = #{id}
</select>

<!-- TagMapper.xml -->
<select id="findPopularTags" resultType="TagWithCountResponse">
    SELECT 
        t.id,
        t.name,
        t.slug,
        COUNT(pt.post_id) as post_count
    FROM tags t
    LEFT JOIN post_tags pt ON t.id = pt.tag_id
    GROUP BY t.id, t.name, t.slug
    ORDER BY post_count DESC
    LIMIT #{limit}
</select>
```

### 3. タグの自動作成と関連付け
記事投稿時にタグ名のリストを受け取り、存在しないタグは自動作成してください。

**例**:
```java
@Service
public class PostService {
    
    @Transactional
    public PostResponse createPost(PostCreateRequest request, String username) {
        // 記事を保存
        Post post = // ...
        postMapper.insert(post);
        
        // タグを処理
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            for (String tagName : request.getTags()) {
                // タグが存在するか確認
                Tag tag = tagMapper.findByName(tagName)
                    .orElseGet(() -> {
                        // 存在しなければ新規作成
                        Tag newTag = new Tag();
                        newTag.setName(tagName);
                        newTag.setSlug(generateSlug(tagName));
                        tagMapper.insert(newTag);
                        return newTag;
                    });
                
                // 記事とタグを関連付け
                postTagMapper.insert(post.getId(), tag.getId());
            }
        }
        
        return toPostResponse(post);
    }
}
```

### 4. REST APIエンドポイント（タグ）

#### タグ一覧の取得
```
GET /api/tags

Response (200 OK):
{
  "tags": [
    {
      "id": 1,
      "name": "Spring Boot",
      "slug": "spring-boot",
      "postCount": 15
    },
    {
      "id": 2,
      "name": "Java",
      "slug": "java",
      "postCount": 23
    }
  ]
}
```

#### タグによる記事検索
```
GET /api/tags/{slug}/posts?page=0&size=10

Response (200 OK):
{
  "tag": {
    "id": 1,
    "name": "Spring Boot",
    "slug": "spring-boot"
  },
  "posts": [
    {
      "id": 1,
      "title": "Spring Bootの基礎",
      ...
    }
  ],
  "totalElements": 15
}
```

#### 人気タグの取得
```
GET /api/tags/popular?limit=10

Response (200 OK):
{
  "tags": [
    {
      "id": 2,
      "name": "Java",
      "slug": "java",
      "postCount": 23
    },
    {
      "id": 1,
      "name": "Spring Boot",
      "slug": "spring-boot",
      "postCount": 15
    }
  ]
}
```

## 💡 実装のヒント（画像アップロード）

### 1. プロジェクト構成
以下のクラスを追加してください：

```
src/main/java/com/example/blog/
├── controller/
│   └── ImageController.java
├── service/
│   └── ImageService.java
├── repository/
│   └── ImageMapper.java (MyBatis)
├── entity/
│   └── Image.java
├── dto/
│   └── ImageUploadResponse.java
└── config/
    └── FileUploadConfig.java
```

### 2. application.ymlでファイルアップロード設定
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
      
blog:
  upload:
    directory: ${user.home}/blog-uploads
    allowed-extensions: jpg,jpeg,png,gif,webp
```

### 3. 画像アップロードの実装
`MultipartFile`を受け取り、ファイルシステムに保存してください。

**例**:
```java
@Service
public class ImageService {
    
    @Value("${blog.upload.directory}")
    private String uploadDirectory;
    
    @Value("${blog.upload.allowed-extensions}")
    private List<String> allowedExtensions;
    
    @Transactional
    public ImageUploadResponse uploadImage(MultipartFile file, Long userId) {
        // ファイル検証
        validateFile(file);
        
        // ファイル名を生成（UUID使用で重複を防ぐ）
        String filename = generateFilename(file.getOriginalFilename());
        
        // ファイルを保存
        Path filePath = Paths.get(uploadDirectory, filename);
        Files.createDirectories(filePath.getParent());
        file.transferTo(filePath.toFile());
        
        // データベースに記録
        Image image = new Image();
        image.setFilename(filename);
        image.setOriginalFilename(file.getOriginalFilename());
        image.setFilePath(filePath.toString());
        image.setFileSize(file.getSize());
        image.setMimeType(file.getContentType());
        image.setUploadedBy(userId);
        
        imageMapper.insert(image);
        
        return new ImageUploadResponse(image.getId(), filename, "/api/images/" + image.getId());
    }
    
    private void validateFile(MultipartFile file) {
        // ファイルサイズチェック
        // 拡張子チェック
        // MIMEタイプチェック
        // 画像かどうかの検証（ImageIO等を使用）
    }
    
    private String generateFilename(String originalFilename) {
        String extension = // 拡張子を取得
        return UUID.randomUUID().toString() + "." + extension;
    }
}
```

### 4. REST APIエンドポイント（画像）

#### 画像のアップロード（認証必須）
```
POST /api/images
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: multipart/form-data

FormData:
- file: (binary)

Response (201 Created):
{
  "id": 1,
  "filename": "a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg",
  "url": "/api/images/1",
  "size": 245678
}
```

#### 画像の取得
```
GET /api/images/{id}

Response: (画像ファイル)
Content-Type: image/jpeg
```

#### ユーザーアバターの設定（認証必須）
```
PUT /api/users/me/avatar
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: multipart/form-data

FormData:
- file: (binary)

Response (200 OK):
{
  "avatarUrl": "/api/images/5"
}
```

### 5. 画像取得のコントローラー
保存した画像ファイルをHTTPレスポンスとして返してください。

**例**:
```java
@RestController
@RequestMapping("/api/images")
public class ImageController {
    
    private final ImageService imageService;
    
    @GetMapping("/{id}")
    public ResponseEntity<Resource> getImage(@PathVariable Long id) {
        Image image = imageMapper.findById(id)
            .orElseThrow(() -> new ImageNotFoundException("Image not found"));
        
        Path filePath = Paths.get(image.getFilePath());
        Resource resource = new FileSystemResource(filePath);
        
        if (!resource.exists()) {
            throw new ImageNotFoundException("Image file not found");
        }
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(image.getMimeType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, 
                   "inline; filename=\"" + image.getOriginalFilename() + "\"")
            .body(resource);
    }
    
    // アップロードエンドポイントを実装
}
```

### 6. Thymeleafでの画像表示と投稿フォーム
記事作成・編集フォームに画像アップロード機能を追加してください。

**例**:
```html
<form id="postForm" enctype="multipart/form-data">
    <div class="mb-3">
        <label for="title" class="form-label">タイトル</label>
        <input type="text" class="form-control" id="title" required>
    </div>
    
    <div class="mb-3">
        <label for="content" class="form-label">内容</label>
        <textarea class="form-control" id="content" rows="10" required></textarea>
    </div>
    
    <div class="mb-3">
        <label for="featuredImage" class="form-label">アイキャッチ画像</label>
        <input type="file" class="form-control" id="featuredImage" accept="image/*">
        <div id="imagePreview" class="mt-2"></div>
    </div>
    
    <div class="mb-3">
        <label for="tags" class="form-label">タグ（カンマ区切り）</label>
        <input type="text" class="form-control" id="tags" 
               placeholder="例: Spring Boot, Java, MyBatis">
    </div>
    
    <button type="submit" class="btn btn-primary">投稿</button>
</form>

<script>
    // 画像プレビュー
    document.getElementById('featuredImage').addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = (e) => {
                document.getElementById('imagePreview').innerHTML = 
                    `<img src="${e.target.result}" class="img-thumbnail" style="max-width: 300px;">`;
            };
            reader.readAsDataURL(file);
        }
    });
    
    // フォーム送信
    document.getElementById('postForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        
        let featuredImageId = null;
        
        // 1. 画像をアップロード
        const imageFile = document.getElementById('featuredImage').files[0];
        if (imageFile) {
            const formData = new FormData();
            formData.append('file', imageFile);
            
            const imageResponse = await fetch('/api/images', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${localStorage.getItem('jwt_token')}`
                },
                body: formData
            });
            
            const imageData = await imageResponse.json();
            featuredImageId = imageData.id;
        }
        
        // 2. 記事を投稿
        const tags = document.getElementById('tags').value
            .split(',')
            .map(tag => tag.trim())
            .filter(tag => tag.length > 0);
        
        const postData = {
            title: document.getElementById('title').value,
            content: document.getElementById('content').value,
            featuredImageId: featuredImageId,
            tags: tags,
            status: 'PUBLISHED'
        };
        
        // REST APIに送信
        // ...
    });
</script>
```

## ✅ 動作確認

### 1. タグ付き記事の投稿
```bash
TOKEN="YOUR_JWT_TOKEN_HERE"

curl -X POST http://localhost:8080/api/posts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Spring Bootでタグ機能を実装",
    "content": "多対多リレーションの実装方法...",
    "tags": ["Spring Boot", "MyBatis", "Java"],
    "status": "PUBLISHED"
  }'
```

### 2. タグによる検索
```bash
curl -X GET "http://localhost:8080/api/tags/spring-boot/posts?page=0&size=10"
```

### 3. 画像のアップロード
```bash
curl -X POST http://localhost:8080/api/images \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/image.jpg"
```

### 4. 画像付き記事の投稿
ブラウザで記事作成フォームから画像を選択して投稿

## 🐛 トラブルシューティング

### エラー: "Maximum upload size exceeded"

**原因**:
- アップロードファイルサイズが設定値を超えている
- `spring.servlet.multipart.max-file-size`の設定が不足

**解決策**:

```yaml
# application.yml
spring:
  servlet:
    multipart:
      enabled: true
      max-file-size: 10MB       # 1ファイルの最大サイズ
      max-request-size: 20MB    # リクエスト全体の最大サイズ
      file-size-threshold: 2MB  # メモリに保持する閾値
```

```java
// Controllerでエラーハンドリング
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxSizeException(MaxUploadSizeExceededException e) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.PAYLOAD_TOO_LARGE.value(),
            "ファイルサイズが大きすぎます。10MB以下のファイルをアップロードしてください。"
        );
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }
}
```

### エラー: "java.nio.file.NoSuchFileException: ディレクトリが存在しません"

**原因**:
- アップロード先ディレクトリが作成されていない
- パスの権限が不足している

**解決策**:

```java
@Service
public class ImageService {
    
    @Value("${blog.upload.directory}")
    private String uploadDirectory;
    
    @PostConstruct
    public void init() {
        try {
            Path uploadPath = Paths.get(uploadDirectory);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("アップロードディレクトリを作成しました: {}", uploadPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("アップロードディレクトリの作成に失敗しました", e);
        }
    }
    
    public ImageUploadResponse uploadImage(MultipartFile file, Long userId) {
        Path filePath = Paths.get(uploadDirectory, filename);
        
        // 親ディレクトリが存在しない場合は作成
        Files.createDirectories(filePath.getParent());
        
        file.transferTo(filePath.toFile());
        // ...
    }
}
```

### エラー: "The requested resource is not available"（画像が404）

**原因**:
- 静的リソースの公開設定が不足
- ファイルパスが間違っている
- セキュリティ設定でブロックされている

**解決策**:

```java
// 静的リソースの設定
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Value("${blog.upload.directory}")
    private String uploadDirectory;
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:" + uploadDirectory + "/")
            .setCachePeriod(3600);  // 1時間キャッシュ
    }
}

// Spring Securityで許可
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/uploads/**").permitAll()  // 画像は認証不要
            .requestMatchers("/api/auth/**").permitAll()
            .anyRequest().authenticated()
        );
    return http.build();
}
```

### エラー: "Duplicate entry for key 'post_tags.PRIMARY'"

**原因**:
- 同じタグを複数回追加しようとしている
- タグの重複チェックが不足

**解決策**:

```java
@Service
@RequiredArgsConstructor
public class PostService {
    private final PostMapper postMapper;
    private final TagMapper tagMapper;
    private final PostTagMapper postTagMapper;
    
    @Transactional
    public void createPost(PostCreateRequest request, String username) {
        // 記事を保存
        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        postMapper.insertPost(post);
        
        // タグを処理（重複を除去）
        Set<String> uniqueTags = new HashSet<>(request.getTags());
        
        for (String tagName : uniqueTags) {
            // タグが既に存在するか確認
            Tag tag = tagMapper.findByName(tagName)
                .orElseGet(() -> {
                    Tag newTag = new Tag();
                    newTag.setName(tagName);
                    newTag.setSlug(generateSlug(tagName));
                    tagMapper.insertTag(newTag);
                    return newTag;
                });
            
            // 記事とタグを関連付け（重複チェック）
            if (!postTagMapper.existsByPostIdAndTagId(post.getId(), tag.getId())) {
                postTagMapper.insertPostTag(post.getId(), tag.getId());
            }
        }
    }
}
```

### エラー: "Unsupported Media Type" (画像アップロード時)

**原因**:
- Content-Typeが`multipart/form-data`でない
- Controllerの`@RequestParam`の型が間違っている

**解決策**:

```java
// Controllerで MultipartFile を受け取る
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {
    private final ImageService imageService;
    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponse> uploadImage(
            @RequestParam("file") MultipartFile file,  // "file" パラメータ名を明示
            @AuthenticationPrincipal UserDetails userDetails) {
        
        if (file.isEmpty()) {
            throw new IllegalArgumentException("ファイルが選択されていません");
        }
        
        ImageUploadResponse response = imageService.uploadImage(file, getUserId(userDetails));
        return ResponseEntity.ok(response);
    }
}
```

```javascript
// JavaScriptでのファイル送信
async function uploadImage(file) {
    const formData = new FormData();
    formData.append('file', file);  // パラメータ名は "file"
    
    const token = localStorage.getItem('jwt_token');
    
    const response = await fetch('/api/images', {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`
            // Content-Type は自動設定されるので指定しない
        },
        body: formData
    });
    
    return await response.json();
}
```

### エラー: "タグの検索結果が0件（実際はデータがある）"

**原因**:
- タグのslugが正しく生成されていない
- 検索時のslugと保存時のslugが一致していない
- 大文字小文字の問題

**解決策**:

```java
// Tag生成時のslugを統一
public String generateSlug(String tagName) {
    return tagName.toLowerCase()
        .replaceAll("[^a-z0-9\\s-]", "")  // 英数字とスペース、ハイフンのみ
        .replaceAll("\\s+", "-")  // スペースをハイフンに
        .replaceAll("-+", "-")  // 連続ハイフンを1つに
        .replaceAll("^-|-$", "");  // 先頭と末尾のハイフンを削除
}

// 検索時も同じロジックでslugを生成
@GetMapping("/tags/{slug}/posts")
public ResponseEntity<PageResponse<PostListResponse>> getPostsByTag(
        @PathVariable String slug,
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "10") Integer size) {
    
    // slugを正規化してから検索
    String normalizedSlug = generateSlug(slug);
    
    Tag tag = tagMapper.findBySlug(normalizedSlug)
        .orElseThrow(() -> new ResourceNotFoundException("タグが見つかりません"));
    
    Page<Post> posts = postService.getPostsByTagId(tag.getId(), page, size);
    return ResponseEntity.ok(PageResponse.from(posts));
}
```

```xml
<!-- TagMapper.xml で大文字小文字を区別しない検索 -->
<select id="findBySlug" resultType="Tag">
    SELECT id, name, slug, post_count, created_at
    FROM tags
    WHERE LOWER(slug) = LOWER(#{slug})
</select>
```

## 🎓 学習ポイント

1. **多対多リレーション**: タグと記事の関連（中間テーブル）
2. **ファイルアップロード**: MultipartFileの扱い
3. **ファイル管理**: ファイルシステムへの保存とメタデータ管理
4. **トランザクション**: 画像アップロードと記事投稿の連携
5. **バリデーション**: ファイルサイズ、形式のチェック
6. **JavaScript**: FormDataでのファイル送信

## 📝 追加課題（オプション）

1. 画像のリサイズ・サムネイル生成
2. 画像のクラウドストレージ保存（AWS S3等）
3. タグの自動補完機能
4. 記事内への画像の埋め込み（マークダウンエディタ）
5. 画像の遅延読み込み（Lazy Loading）
6. タグの編集・削除機能

## ➡️ 次のステップ

[Step 38: テスト実装とアプリケーションの完成](STEP_38.md)へ進みましょう！

テスト実装とデプロイ準備を行い、ミニブログアプリケーションを完成させます。
