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

## 📝 実装の流れ

このステップでは、以下の機能を自分で実装します：

1. **ファイル保存サービス（FileStorageService）** - セキュアなファイル管理
2. **ファイルコントローラー（FileController）** - アップロード/ダウンロードAPI
3. **ユーザーサービス拡張（UserService）** - プロフィール画像管理
4. **MyBatis検索機能** - 動的SQLによる高度な検索

---

## 🚀 ステップ1: 設定の追加

### 1-1. application.ymlにファイルアップロード設定を追加

`src/main/resources/application.yml`に以下を追加：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

file:
  upload-dir: uploads
  max-file-size: 5MB

mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.example.bloghub.dto
  configuration:
    map-underscore-to-camel-case: true
```

---

## 🚀 ステップ2: FileStorageServiceの実装 【自分で実装】

### 📁 ファイルパス
`src/main/java/com/example/bloghub/service/FileStorageService.java`

### 📋 機能要件

セキュアなファイル保存・読み込み・削除機能を実装します。

**主な機能**:
| メソッド | 引数 | 戻り値 | 説明 |
|---------|------|--------|------|
| `storeFile` | MultipartFile | String | ファイルを保存し、生成されたファイル名を返す |
| `loadFileAsResource` | String fileName | Resource | ファイルをリソースとして読み込む |
| `deleteFile` | String fileName | void | ファイルを削除 |

### 🔒 セキュリティ対策

| 対策 | 説明 | 実装方法 |
|------|------|----------|
| パストラバーサル防止 | `..`を含むファイル名を拒否 | `fileName.contains("..")` でチェック |
| ファイルサイズ制限 | 5MBを超えるファイルを拒否 | `file.getSize() > MAX_SIZE` でチェック |
| 拡張子チェック | jpg, jpeg, png, gifのみ許可 | 許可リストと照合 |
| ユニークファイル名 | ファイル名の衝突を防止 | `UUID.randomUUID()` を使用 |

### 💡 実装のポイント

```java
// コンストラクタでアップロードディレクトリを初期化
public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
    this.fileStorageLocation = Paths.get(uploadDir)
            .toAbsolutePath()
            .normalize();
    // ディレクトリが存在しない場合は作成
    Files.createDirectories(this.fileStorageLocation);
}
```

**使用するクラス**:
- `java.nio.file.Paths`, `java.nio.file.Path` - パス操作
- `java.nio.file.Files` - ファイル操作（copy, delete, exists）
- `java.util.UUID` - ユニークIDの生成
- `org.springframework.core.io.UrlResource` - リソースとしてファイルを返す
- `org.springframework.util.StringUtils` - ファイル名のサニタイズ

---

## 🚀 ステップ3: FileControllerの実装 【自分で実装】

### 📁 ファイルパス
`src/main/java/com/example/bloghub/controller/FileController.java`

### 📋 エンドポイント一覧

| メソッド | パス | 認証 | 説明 |
|---------|------|------|------|
| POST | `/api/files/upload` | 必要 | ファイルをアップロード |
| GET | `/api/files/{fileName}` | 不要 | ファイルをダウンロード |
| DELETE | `/api/files/{fileName}` | 必要 | ファイルを削除 |

### 💡 実装のポイント

**アップロードレスポンス**:
```json
{
  "fileName": "a1b2c3d4-uuid.jpg",
  "fileUrl": "/api/files/a1b2c3d4-uuid.jpg"
}
```

**ダウンロード時の注意点**:
- `@GetMapping("/{fileName:.+}")` - ファイル名に`.`が含まれても正しくパース
- `MediaType.IMAGE_JPEG` を返すか、拡張子から判定
- `HttpHeaders.CONTENT_DISPOSITION` で`inline`を指定（ブラウザで表示）

**使用するアノテーション**:
- `@RestController`, `@RequestMapping("/api/files")`
- `@PreAuthorize("isAuthenticated()")` - 認証必須エンドポイント
- `@RequestParam("file") MultipartFile` - ファイル受け取り
- `@PathVariable String fileName` - パスパラメータ

---

## 🚀 ステップ4: ユーザー機能の拡張 【自分で実装】

### 4-1. UserService拡張

**ファイルパス**: `src/main/java/com/example/bloghub/service/UserService.java`

**追加するメソッド**:
| メソッド | 処理内容 |
|---------|----------|
| `uploadProfileImage` | 1. ユーザー取得 → 2. 既存画像があれば削除 → 3. 新画像を保存 → 4. User更新 |
| `getUser` | ユーザー名で検索してUserResponseを返す |
| `updateUser` | ユーザー情報を更新 |

### 4-2. UserController

**ファイルパス**: `src/main/java/com/example/bloghub/controller/UserController.java`

**エンドポイント**:
| メソッド | パス | 認証 | 説明 |
|---------|------|------|------|
| POST | `/api/users/profile-image` | 必要 | プロフィール画像アップロード |
| GET | `/api/users/{username}` | 不要 | ユーザー情報取得 |
| PUT | `/api/users/me` | 必要 | 自分の情報更新 |

### 4-3. DTO

**UserUpdateRequest** - 更新用リクエスト
**UserResponse** - レスポンス（id, username, email, profileImage, createdAt）

---

## 🚀 ステップ5: MyBatis検索機能の実装 【自分で実装】

### 5-1. ArticleSearchMapperインターフェース

**ファイルパス**: `src/main/java/com/example/bloghub/mapper/ArticleSearchMapper.java`

**メソッド**:
| メソッド | 引数 | 戻り値 | 説明 |
|---------|------|--------|------|
| `searchArticles` | keyword, tagNames, username, offset, limit | `List<ArticleSearchResult>` | 複数条件で検索 |
| `countSearchResults` | keyword, tagNames, username | int | 検索結果の総件数 |

**アノテーション**: `@Mapper`

### 5-2. MyBatis XMLマッパー

**ファイルパス**: `src/main/resources/mapper/ArticleSearchMapper.xml`

以下はXMLの構造例です。動的SQLの書き方を参考にしてください：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.bloghub.mapper.ArticleSearchMapper">
    
    <!-- ResultMap: 記事検索結果のマッピング -->
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
    
    <!-- 記事検索クエリ -->
    <select id="searchArticles" resultMap="ArticleSearchResultMap">
        SELECT DISTINCT
            a.id, a.title, a.content, u.username, a.created_at, t.name as tag_name
        FROM articles a
        INNER JOIN users u ON a.user_id = u.id
        LEFT JOIN article_tags at ON a.id = at.article_id
        LEFT JOIN tags t ON at.tag_id = t.id
        <where>
            <!-- キーワード検索 -->
            <if test="keyword != null and keyword != ''">
                (a.title LIKE CONCAT('%', #{keyword}, '%')
                OR a.content LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <!-- タグ検索 -->
            <if test="tagNames != null and tagNames.size() > 0">
                AND t.name IN
                <foreach collection="tagNames" item="tag" open="(" separator="," close=")">
                    #{tag}
                </foreach>
            </if>
            <!-- ユーザー名検索 -->
            <if test="username != null and username != ''">
                AND u.username = #{username}
            </if>
        </where>
        ORDER BY a.created_at DESC
        LIMIT #{limit} OFFSET #{offset}
    </select>
    
    <!-- 件数カウント -->
    <select id="countSearchResults" resultType="int">
        <!-- 上記と同様のWHERE句を使用 -->
    </select>
</mapper>
```

**動的SQLのポイント**:
| タグ | 説明 |
|------|------|
| `<where>` | 条件がない場合は`WHERE`句自体を省略 |
| `<if>` | 条件が満たされた場合のみSQL追加 |
| `<foreach>` | リストをループして IN句を生成 |

### 5-3. DTO

**ArticleSearchRequest**:
| フィールド | 型 | 説明 |
|-----------|-----|------|
| keyword | String | 検索キーワード |
| tags | List<String> | タグ名のリスト |
| username | String | ユーザー名 |
| page | int | ページ番号（デフォルト0） |
| size | int | ページサイズ（デフォルト10） |

**ArticleSearchResult**:
| フィールド | 型 |
|-----------|-----|
| id | Long |
| title | String |
| content | String |
| username | String |
| tags | List<String> |
| createdAt | LocalDateTime |

**ArticleSearchResponse**:
| フィールド | 型 |
|-----------|-----|
| articles | List<ArticleSearchResult> |
| totalPages | int |
| totalElements | long |
| currentPage | int |

### 5-4. ArticleSearchService

**ファイルパス**: `src/main/java/com/example/bloghub/service/ArticleSearchService.java`

**処理フロー**:
1. リクエストからoffsetを計算: `page * size`
2. `articleSearchMapper.searchArticles()` で記事を検索
3. `articleSearchMapper.countSearchResults()` で総件数を取得
4. 総ページ数を計算: `Math.ceil(totalElements / size)`
5. `ArticleSearchResponse` を生成して返却

### 5-5. 検索エンドポイント（ArticleControllerに追加）

**エンドポイント**: `GET /api/articles/search`

**クエリパラメータ**:
| パラメータ | 必須 | 説明 |
|-----------|------|------|
| keyword | No | 検索キーワード |
| tags | No | タグ名（複数可） |
| username | No | ユーザー名 |
| page | No | ページ番号（デフォルト0） |
| size | No | ページサイズ（デフォルト10） |

---

## ✅ 動作確認

### 1. ユーザー登録とログイン

```bash
# ログイン
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "password123"}'

# トークンを保存
export TOKEN="取得したトークン"
```

### 2. プロフィール画像のアップロード

```bash
# テスト用画像を用意してアップロード
curl -X POST http://localhost:8080/api/users/profile-image \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@profile.jpg"
```

**期待される結果**:
```json
{
  "id": 1,
  "username": "alice",
  "email": "alice@example.com",
  "profileImage": "a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg"
}
```

### 3. プロフィール画像のダウンロード

```bash
curl http://localhost:8080/api/files/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg -o downloaded.jpg
```

### 4. 記事の作成（検索テスト用）

```bash
# Spring Boot記事
curl -X POST http://localhost:8080/api/articles \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title": "Spring Boot入門", "content": "Spring Bootの基礎", "tags": ["Spring", "Tutorial"]}'

# Java記事
curl -X POST http://localhost:8080/api/articles \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title": "Javaプログラミング", "content": "Javaの基本", "tags": ["Java", "Programming"]}'
```

### 5. 検索機能のテスト

```bash
# キーワード検索
curl "http://localhost:8080/api/articles/search?keyword=Spring"

# タグ検索
curl "http://localhost:8080/api/articles/search?tags=Java&tags=Programming"

# ユーザー名検索
curl "http://localhost:8080/api/articles/search?username=alice"

# 複合条件検索
curl "http://localhost:8080/api/articles/search?keyword=Spring&tags=Tutorial&username=alice"

# ページネーション
curl "http://localhost:8080/api/articles/search?keyword=Java&page=0&size=5"
```

### 6. エラーケースのテスト

```bash
# 大きすぎるファイル（エラーになるはず）
dd if=/dev/zero of=large.jpg bs=1M count=10
curl -X POST http://localhost:8080/api/users/profile-image \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@large.jpg"

# 許可されていない拡張子
echo "test" > test.txt
curl -X POST http://localhost:8080/api/users/profile-image \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@test.txt"
```

---

## 🎯 チャレンジ課題

### 課題1: 画像のサムネイル生成

画像アップロード時に自動的にサムネイルを生成する機能を実装してください。

**ヒント**: thumbnailatorライブラリを使用
```xml
<dependency>
    <groupId>net.coobird</groupId>
    <artifactId>thumbnailator</artifactId>
    <version>0.4.20</version>
</dependency>
```

### 課題2: ファイルメタデータの管理

アップロードされたファイルのメタデータ（サイズ、MIMEタイプ、アップロード日時）をデータベースで管理する機能を実装してください。

### 課題3: 全文検索エンジンの統合

Elasticsearchを使った高速な全文検索機能を実装してください。

---

## 🔧 トラブルシューティング

### 問題1: 413 Payload Too Large

**原因**: ファイルサイズが制限を超えている

**解決策**: application.ymlの`max-file-size`を確認

### 問題2: MyBatisマッパーが見つからない

**エラー**: `Invalid bound statement (not found)`

**解決策**:
1. `mybatis.mapper-locations`が正しいか確認
2. XMLのnamespaceがインターフェースのFQCNと一致しているか確認

### 問題3: 動的SQLで結果が空

**原因**: `<if>`条件の書き方が間違っている

**解決策**: `test="keyword != null and keyword != ''"` のように空文字チェックも追加

### 問題4: プロフィール画像が404

**原因**: ファイルパスの解決に失敗

**解決策**:
1. `uploads/`ディレクトリが存在するか確認
2. ファイル名が正しいか確認

---

## 📚 このステップで学んだこと

- ✅ ファイルアップロードのセキュリティ対策（パストラバーサル、サイズ制限、拡張子チェック）
- ✅ FileStorageServiceによるセキュアなファイル管理
- ✅ プロフィール画像のアップロード・ダウンロード機能
- ✅ MyBatisの動的SQL（`<where>`, `<if>`, `<foreach>`）
- ✅ 複数条件での高度な検索機能
- ✅ ResultMapによる複雑なオブジェクトマッピング

---

## ➡️ 次のステップ

[Step 38: Thymeleafでブログ画面実装](STEP_38.md)へ進みましょう！

次は、Thymeleafを使ってブラウザで操作できるWeb画面を実装します。
