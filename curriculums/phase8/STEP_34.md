# Step 34: プロジェクト初期設定とユーザー認証基盤

## 🎯 目標
Phase 8では、これまで学んだ全ての技術を統合して**ミニブログアプリケーション**を開発します。
このステップでは、プロジェクトの初期設定とユーザー認証基盤（JWT認証）を実装します。

## 📋 機能要件
- 新規ユーザー登録
- ログイン（JWT トークン発行）
- パスワードのハッシュ化（BCrypt）
- ログイン後のユーザー情報取得

## 🗂️ データベース設計

### usersテーブル
```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    bio TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## 💡 実装のヒント

### 1. プロジェクト構成
レイヤー化アーキテクチャを意識して、以下の構成で実装してください。

```
src/main/java/com/example/blog/
├── controller/
│   └── AuthController.java
├── service/
│   └── UserService.java
├── repository/
│   └── UserMapper.java (MyBatis)
├── entity/
│   └── User.java
├── dto/
│   ├── UserRegistrationRequest.java
│   ├── LoginRequest.java
│   └── AuthResponse.java
├── security/
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   └── SecurityConfig.java
└── exception/
    ├── UserAlreadyExistsException.java
    └── InvalidCredentialsException.java
```

### 2. 必要な依存関係（pom.xml）
以下の依存関係を追加してください：
```xml
<!-- 例: Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- 例: JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>

<!-- その他必要な依存関係を考えて追加してください -->
```

### 3. MyBatisでのユーザー管理
`UserMapper.xml`でSQLを定義し、以下の操作を実装してください：
- ユーザー登録（INSERT）
- ユーザー名でユーザー検索（SELECT）
- メールアドレスでユーザー検索（SELECT）
- ユーザーIDでユーザー取得（SELECT）

**例**:
```xml
<!-- UserMapper.xml の例 -->
<mapper namespace="com.example.blog.repository.UserMapper">
    <insert id="insertUser" parameterType="User" useGeneratedKeys="true" keyProperty="id">
        <!-- SQLを考えて実装してください -->
    </insert>
    
    <select id="findByUsername" resultType="User">
        <!-- SQLを考えて実装してください -->
    </select>
</mapper>
```

### 4. JWT トークンの生成と検証
`JwtTokenProvider`クラスを作成し、以下のメソッドを実装してください：
- `generateToken(String username)`: JWTトークンを生成
- `validateToken(String token)`: トークンの有効性を検証
- `getUsernameFromToken(String token)`: トークンからユーザー名を取得

**考えるポイント**:
- トークンの有効期限は何時間にするか？
- シークレットキーはどこに保管するか？（application.yml）

### 5. パスワードのハッシュ化
Spring SecurityのBCryptPasswordEncoderを使用してください。

**例**:
```java
@Service
public class UserService {
    private final PasswordEncoder passwordEncoder;
    
    // コンストラクタインジェクションでPasswordEncoderを注入
    
    public void registerUser(UserRegistrationRequest request) {
        // パスワードをハッシュ化してからデータベースに保存
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        // ...
    }
}
```

### 6. REST APIエンドポイント
以下のエンドポイントを実装してください：

#### ユーザー登録
```
POST /api/auth/register
Content-Type: application/json

{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "displayName": "John Doe"
}

Response (201 Created):
{
  "message": "User registered successfully",
  "username": "johndoe"
}
```

#### ログイン
```
POST /api/auth/login
Content-Type: application/json

{
  "username": "johndoe",
  "password": "SecurePass123!"
}

Response (200 OK):
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "johndoe",
  "displayName": "John Doe"
}
```

#### 現在のユーザー情報取得（認証必要）
```
GET /api/auth/me
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

Response (200 OK):
{
  "id": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "displayName": "John Doe",
  "bio": null,
  "createdAt": "2025-10-29T10:00:00"
}
```

### 7. バリデーション
リクエストDTOに適切なバリデーションを追加してください：
- ユーザー名: 3〜50文字、英数字とアンダースコアのみ
- メールアドレス: 有効な形式
- パスワード: 8文字以上、英数字含む

**例**:
```java
public class UserRegistrationRequest {
    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$")
    private String username;
    
    // 他のフィールドにも適切なバリデーションを追加
}
```

### 8. 例外ハンドリング
`@ControllerAdvice`を使用して、以下のエラーを適切に処理してください：
- ユーザー名/メールアドレスの重複
- 認証情報の不一致
- バリデーションエラー

## ✅ 動作確認

### 1. ユーザー登録
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test1234!",
    "displayName": "Test User"
  }'
```

### 2. ログイン
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test1234!"
  }'
```

### 3. ユーザー情報取得（トークンを使用）
```bash
# 上記ログインで取得したトークンを使用
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE"
```

## 🎓 学習ポイント

1. **MyBatis**: SQLを直接制御してユーザー情報を管理
2. **Spring Security**: 認証・認可の基盤を構築
3. **JWT**: ステートレスな認証方式の実装
4. **バリデーション**: ユーザー入力の検証
5. **例外ハンドリング**: 統一されたエラーレスポンス
6. **パスワードセキュリティ**: BCryptによるハッシュ化

## 📝 追加課題（オプション）

1. メールアドレス確認機能（登録時にメール送信）
2. パスワードリセット機能
3. ユーザープロフィール更新API
4. アカウント削除機能
5. リフレッシュトークンの実装

## 🚀 次のステップ
Step 35では、記事（Post）の投稿・編集・削除機能を実装し、認可制御（自分の記事のみ編集可能）を追加します。
