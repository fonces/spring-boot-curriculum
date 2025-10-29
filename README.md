# Spring Boot 3.5.7 カリキュラム

## 対象者
- Javaの基礎研修を修了した方
- オブジェクト指向プログラミングの基本を理解している方

## カリキュラムの特徴
- **ミニマムステップ**: 各ステップは30分〜1時間程度で完了
- **達成感重視**: 各ステップで動作するアプリケーションを作成
- **実践的**: 実務で使える知識を段階的に習得
- **包括的**: Spring Boot + MyBatis + Thymeleaf の3つの技術を体系的に学習
  - **Spring Data JPA**: オブジェクト指向でのデータ操作
  - **MyBatis**: SQLを直接制御する柔軟なデータアクセス
  - **Thymeleaf**: サーバーサイドレンダリングによるWebページ構築

---

## 📚 カリキュラム構成

### 🌱 Phase 1: Spring Boot基礎 (5ステップ)

#### Step 1: Hello World REST API
**目標**: 最初のSpring Bootアプリケーションを作成し、起動する
- Spring Initializrでプロジェクト作成
- `@RestController`で簡単なGETエンドポイント作成
- アプリケーション起動とcurlでの動作確認
- **成果物**: `GET /hello` → "Hello, Spring Boot!"

#### Step 2: パスパラメータとクエリパラメータ
**目標**: リクエストパラメータの扱い方を学ぶ
- `@PathVariable`でパスパラメータを受け取る
- `@RequestParam`でクエリパラメータを受け取る
- **成果物**: `GET /greet/{name}?language=ja` → "こんにちは、{name}さん！"

#### Step 3: POSTリクエストとリクエストボディ
**目標**: データの送信と受信を理解する
- `@PostMapping`の実装
- `@RequestBody`でJSONを受け取る
- DTOクラスの作成
- **成果物**: `POST /users` でユーザー情報を受け取り、IDを付与して返す

#### Step 4: application.ymlで設定管理
**目標**: 外部設定の基本を学ぶ
- `application.yml`の基本構文
- サーバーポート変更
- カスタムプロパティの定義と`@Value`での読み込み
- **成果物**: 環境ごとに異なるメッセージを返すAPI

#### Step 5: Lombokで簡潔なコード
**目標**: Lombokで冗長なコードを削減
- `@Data`, `@Getter`, `@Setter`の使い方
- `@Builder`でビルダーパターン実装
- `@AllArgsConstructor`, `@NoArgsConstructor`
- **成果物**: ステップ3のコードをLombokでリファクタリング

---

### 🗄️ Phase 2: データベース連携の基礎 (6ステップ)

#### Step 6: MySQL環境構築
**目標**: Docker ComposeでMySQL開発環境を構築
- Docker Composeの基本
- MySQLコンテナの起動
- データベース接続確認
- **成果物**: MySQLが動作する開発環境

#### Step 7: Spring Data JPAでCRUDの基本
**目標**: JPAでデータベース操作を実装
- `@Entity`でエンティティ定義
- `JpaRepository`の作成
- 基本的なCRUD操作（Create, Read）
- **成果物**: ユーザーを登録し、全件取得するAPI

#### Step 8: CRUD操作の完成
**目標**: 更新・削除機能を実装
- `PUT /users/{id}` で更新
- `DELETE /users/{id}` で削除
- `GET /users/{id}` で1件取得
- **成果物**: 完全なREST APIの実装

#### Step 9: @Transactionalでトランザクション管理
**目標**: データの整合性を保つトランザクション制御を学ぶ
- トランザクションとは何かを理解する
- `@Transactional`の基本的な使い方
- トランザクションの伝播（Propagation）とロールバック
- 複数テーブルの更新を1つのトランザクションで実行
- **成果物**: エラー時に自動的にロールバックされるAPI

#### Step 10: カスタムクエリ
**目標**: 柔軟な検索機能を実装
- `findBy...`メソッドの命名規則
- `@Query`アノテーションでJPQL記述
- 検索条件を持つAPI実装
- **成果物**: 名前で部分一致検索するAPI

#### Step 11: リレーションシップ（1対多）
**目標**: テーブル間の関連を扱う
- `@OneToMany`, `@ManyToOne`の実装
- CascadeとFetch戦略の理解
- **成果物**: ユーザーと投稿（Post）の関連を持つAPI

---

### 🔍 Phase 3: MyBatisによるSQL制御 (3ステップ)

#### Step 12: MyBatisの基礎
**目標**: MyBatisでSQLを直接扱う方法を学ぶ
- MyBatisとJPAの違いを理解
- MyBatis依存関係の追加
- Mapper XMLとMapperインターフェース
- 基本的なCRUD操作をMyBatisで実装
- **成果物**: MyBatisを使った商品管理API

#### Step 13: MyBatisで複雑なクエリ
**目標**: 動的SQLと結合クエリを実装
- `<if>`, `<choose>`, `<foreach>`で動的SQL
- JOINを使った複数テーブルの結合
- ResultMapで複雑なマッピング
- ページネーションの実装
- **成果物**: 高度な検索機能とレポート生成API

#### Step 14: JPAとMyBatisの使い分け
**目標**: 2つのORM技術を適材適所で使う
- JPA向きの処理とMyBatis向きの処理を理解
- 同一プロジェクトでの併用パターン
- トランザクション管理の統一
- パフォーマンス比較
- **成果物**: JPAとMyBatisを併用したアプリケーション

---

### 🏗️ Phase 4: アーキテクチャとベストプラクティス (6ステップ)

#### Step 15: レイヤー化アーキテクチャ
**目標**: 責務を分離した設計を学ぶ
- Controller / Service / Repositoryの分離
- `@Service`の役割
- ビジネスロジックの配置
- **成果物**: 3層アーキテクチャにリファクタリング

#### Step 16: DI/IoCコンテナの深掘り
**目標**: Spring FrameworkのコアであるDI/IoCを深く理解する
- 依存性注入（DI）とは何か、なぜ必要か
- `@Component`, `@Service`, `@Repository`の違いと使い分け
- コンストラクタインジェクション vs フィールドインジェクション
- `@Autowired`, `@Qualifier`, `@Primary`の使い方
- Bean のスコープ（Singleton, Prototype, Request等）
- `@Configuration`と`@Bean`で手動Bean登録
- **成果物**: DIを意識したリファクタリングと各種インジェクション手法の実装

#### Step 17: 例外ハンドリング
**目標**: エラー処理を適切に実装
- カスタム例外クラスの作成
- `@ControllerAdvice`でグローバルエラーハンドリング
- HTTPステータスコードの適切な使い分け
- **成果物**: 統一されたエラーレスポンスを返すAPI

#### Step 18: バリデーション
**目標**: 入力値検証を実装
- `@Valid`と`@Validated`
- `@NotNull`, `@NotBlank`, `@Size`などの活用
- カスタムバリデーションの作成
- **成果物**: 不正なデータを弾くAPI

#### Step 19: DTOとEntityの分離
**目標**: レイヤー間のデータ変換
- リクエストDTO / レスポンスDTO / Entity
- MapStructまたは手動マッピング
- 不要な情報の隠蔽
- **成果物**: セキュアなデータ転送を実現

#### Step 20: ロギング
**目標**: トラブルシューティングの基礎
- SLF4J + Logbackの使い方
- ログレベルの使い分け（INFO, DEBUG, ERROR）
- `logback-spring.xml`でのカスタマイズ
- **成果物**: 適切なログ出力を実装

---

### 🎨 Phase 5: Thymeleafでサーバーサイドレンダリング (4ステップ)

#### Step 21: Thymeleafの基礎
**目標**: Thymeleafテンプレートエンジンの基本を学ぶ
- Thymeleaf依存関係の追加
- `@Controller`と`@RestController`の違い
- Modelを使ったデータ渡し
- 基本的なテンプレート構文（th:text, th:each）
- **成果物**: ユーザー一覧を表示するWebページ

#### Step 22: フォーム送信とバリデーション
**目標**: HTMLフォームからデータを受け取る
- `th:object`と`th:field`でフォームバインディング
- POSTリクエストの処理
- バリデーションエラーの表示
- リダイレクトとフラッシュメッセージ
- **成果物**: ユーザー登録フォーム

#### Step 23: レイアウトとフラグメント
**目標**: テンプレートの再利用性を高める
- `th:fragment`で共通部品を定義
- `th:replace`と`th:insert`でフラグメント利用
- レイアウトダイアレクトの使用
- BootstrapなどのCSSフレームワーク統合
- **成果物**: 共通ヘッダー・フッターを持つWebサイト

#### Step 24: Thymeleaf + REST API連携
**目標**: Ajax通信でSPAライクな操作を実装
- JavaScriptでREST APIを呼び出す
- JSON形式でのデータ取得・送信
- DOM操作でページを動的に更新
- ThymeleafとREST APIの使い分け
- **成果物**: リロードなしで動作するタスク管理画面

---

### 🔒 Phase 6: セキュリティとテスト (5ステップ)

#### Step 25: Spring Securityの基礎
**目標**: 認証・認可の基本を理解
- Spring Security依存関係の追加
- Basic認証の実装
- インメモリユーザー管理
- **成果物**: 認証が必要なAPI

#### Step 26: JWTトークン認証
**目標**: モダンな認証方式を実装
- JWTライブラリの導入
- ログインでトークン発行
- トークン検証フィルター実装
- **成果物**: JWT認証を使ったREST API

#### Step 27: ユニットテスト
**目標**: サービス層のテストを書く
- JUnit 5の基本
- Mockitoでモック作成
- `@ExtendWith(MockitoExtension.class)`
- **成果物**: Serviceクラスのテストコード

#### Step 28: 統合テスト
**目標**: APIの統合テストを実装
- `@SpringBootTest`の使い方
- `MockMvc`でHTTPリクエストテスト
- `@DataJpaTest`でリポジトリテスト
- **成果物**: Controllerの統合テスト

#### Step 29: テストカバレッジ
**目標**: テストの網羅性を確認
- JaCoCoの導入
- カバレッジレポートの見方
- テストの品質向上
- **成果物**: 80%以上のカバレッジ達成

---

### 🚀 Phase 7: 実践的な機能 (4ステップ)

#### Step 30: ファイルアップロード
**目標**: マルチパートリクエストを扱う
- `MultipartFile`の受け取り
- ファイルの保存とメタデータ管理
- ファイルダウンロードAPI
- **成果物**: 画像アップロード機能

#### Step 31: ページネーション
**目標**: 大量データの効率的な取得
- `Pageable`の使い方
- `Page<T>`のレスポンス
- ソート機能の追加
- **成果物**: ページング付きリストAPI

#### Step 32: キャッシュ
**目標**: パフォーマンス最適化の基礎
- Spring Cacheの有効化
- `@Cacheable`, `@CacheEvict`
- Redisとの連携（オプション）
- **成果物**: キャッシュされたAPI

#### Step 33: 非同期処理
**目標**: 時間のかかる処理を非同期化
- `@Async`の使い方
- `CompletableFuture`
- スレッドプールの設定
- **成果物**: 非同期メール送信機能

---

### 🎯 Phase 8: 総合演習（最終プロジェクト）

#### Step 34-38: ミニブログアプリケーション開発
**目標**: これまでの知識を統合して実践的なアプリを作る

**機能要件**:
- ユーザー登録・ログイン（JWT認証）
- 記事の投稿・編集・削除（認可制御）
- コメント機能（1対多リレーション）
- 画像アップロード
- ページネーション付き記事一覧
- タグによる検索
- ユニットテスト・統合テスト

**推奨期間**: 5日間（各ステップ1日）

---

## 🛠️ 推奨開発環境

- **JDK**: OpenJDK 21
- **IDE**: IntelliJ IDEA Community Edition
- **ビルドツール**: Maven 3.8+
- **データベース**: Docker Desktop（MySQL用）
- **APIテスト**: Postman / curl

## 📖 学習のヒント

1. **各ステップで必ず動かす**: コードを書いたら必ず実行して動作確認
2. **エラーを恐れない**: エラーメッセージをよく読んで原因を理解する
3. **公式ドキュメント活用**: [Spring Boot Reference](https://spring.pleiades.io/spring-boot/)
4. **コードをコミット**: Gitで各ステップの成果をコミットして振り返り可能に
5. **理解 > 暗記**: なぜそう書くのかを理解することが重要

## 🎓 修了後の学習パス

- Spring Cloud（マイクロサービス）
- Spring Batch（バッチ処理）
- Kotlin + Spring Boot
- GraphQL with Spring Boot
- Native Image（GraalVM）

---

## 📝 各ステップの進め方（例）

```
1. ステップの目標を確認
2. 必要な依存関係を追加
3. コードを実装
4. アプリケーションを起動
5. PostmanやcurlでAPIを確認
6. コードをGitにコミット
7. プッシュしてSlackにてレビュー依頼をする
8. 次のステップへ
```

**推奨学習ペース**: 
- 平日1時間 → 1〜2ステップ/日
- 全体で3〜4週間で修了

---

Happy Coding! 🚀
