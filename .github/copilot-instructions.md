# Spring Boot カリキュラム ドキュメント作成ルール

このドキュメントは、Spring Boot 3.5.7カリキュラムのステップガイドを作成する際の統一ルールを定義します。

---

## 📁 ファイル構成ルール

### ディレクトリ構造

```
spring-boot-curriculum/
├── README.md                          # カリキュラム全体の概要
├── curriculums/
│   ├── phase1/                        # Phase 1: Spring Boot基礎 (5ステップ)
│   │   ├── PREPARE.md                 # 事前準備ガイド
│   │   ├── STEP_1.md                  # Hello World REST API
│   │   ├── STEP_2.md                  # パスパラメータとクエリパラメータ
│   │   ├── STEP_3.md                  # POSTリクエストとリクエストボディ
│   │   ├── STEP_4.md                  # application.ymlで設定管理
│   │   └── STEP_5.md                  # Lombokで簡潔なコード
│   ├── phase2/                        # Phase 2: データベース連携の基礎 (6ステップ)
│   │   ├── PREPARE.md                 # MySQL環境構築準備
│   │   ├── STEP_6.md                  # MySQL環境構築
│   │   ├── STEP_7.md                  # Spring Data JPAでCRUDの基本
│   │   ├── STEP_8.md                  # CRUD操作の完成
│   │   ├── STEP_9.md                  # @Transactionalでトランザクション管理
│   │   ├── STEP_10.md                 # カスタムクエリ
│   │   └── STEP_11.md                 # リレーションシップ（1対多）
│   ├── phase3/                        # Phase 3: MyBatisによるSQL制御 (3ステップ)
│   │   ├── STEP_12.md                 # MyBatisの基礎
│   │   ├── STEP_13.md                 # MyBatisで複雑なクエリ
│   │   └── STEP_14.md                 # JPAとMyBatisの使い分け
│   ├── phase4/                        # Phase 4: アーキテクチャとベストプラクティス (6ステップ)
│   │   ├── STEP_15.md                 # レイヤー化アーキテクチャ
│   │   ├── STEP_16.md                 # DI/IoCコンテナの深掘り
│   │   ├── STEP_17.md                 # 例外ハンドリング
│   │   ├── STEP_18.md                 # バリデーション
│   │   ├── STEP_19.md                 # DTOとEntityの分離
│   │   └── STEP_20.md                 # ロギング
│   ├── phase5/                        # Phase 5: Thymeleafでサーバーサイドレンダリング (4ステップ)
│   │   ├── STEP_21.md                 # Thymeleafの基礎
│   │   ├── STEP_22.md                 # フォーム送信とバリデーション
│   │   ├── STEP_23.md                 # レイアウトとフラグメント
│   │   └── STEP_24.md                 # Thymeleaf + REST API連携
│   ├── phase6/                        # Phase 6: セキュリティとテスト (5ステップ)
│   │   ├── STEP_25.md                 # Spring Securityの基礎
│   │   ├── STEP_26.md                 # JWTトークン認証
│   │   ├── STEP_27.md                 # ユニットテスト
│   │   ├── STEP_28.md                 # 統合テスト
│   │   └── STEP_29.md                 # テストカバレッジ
│   ├── phase7/                        # Phase 7: 実践的な機能 (4ステップ)
│   │   ├── STEP_30.md                 # ファイルアップロード
│   │   ├── STEP_31.md                 # ページネーション
│   │   ├── STEP_32.md                 # キャッシュ
│   │   └── STEP_33.md                 # 非同期処理
│   └── phase8/                        # Phase 8: 総合演習（最終プロジェクト） (5ステップ)
│       ├── STEP_34.md                 # プロジェクト概要と環境構築
│       ├── STEP_35.md                 # 認証・認可機能の実装
│       ├── STEP_36.md                 # 記事とコメント機能の実装
│       ├── STEP_37.md                 # 画像アップロードと検索機能
│       └── STEP_38.md                 # テストとデプロイ準備
└── .github/
    └── copilot-instructions.md                # このファイル
```

### ファイル命名規則

- **ステップガイド**: `curriculums/phaseX/STEP_Y.md` （XはPhase番号、YはStep番号）
- **サンプルコード**: `examples/phaseX/step-y-brief-description/` （小文字、ハイフン区切り）
- **Phase内のステップ番号**: 全体通しの番号を使用（Phase 1は1-5、Phase 2は6-11など）

---

## 📝 ステップガイド（STEP_X.md）の構成

各ステップのドキュメントは以下の構成で作成してください。

### 必須セクション

#### 1. タイトルと目標 (Header)

```markdown
# Step X: [ステップ名]

## 🎯 このステップの目標

- 箇条書きで3〜5個の学習目標
- 具体的で測定可能な目標を記載
- 「〜できる」の形式で記述

**所要時間**: 約XX分
```

**ルール**:
- タイトルはH1 (`#`) を使用
- 目標は明確で具体的に
- 所要時間は30分〜1時間を目安

---

#### 2. 事前準備 (Prerequisites)

```markdown
## 📋 事前準備

- 前のステップで作成したプロジェクト
- 必要なツール/ライブラリのインストール確認
- （必要に応じて）前提知識の確認
```

**ルール**:
- 前のステップとの依存関係を明記
- 新しいツールが必要な場合は確認コマンドも記載

---

#### 3. ステップバイステップの手順

```markdown
## 🚀 ステップ1: [サブタスク名]

### 1-1. [具体的な作業]

（説明文）

**ファイルパス**: `src/.../ClassName.java`

​```java
// コード例
​```

### 1-2. コードの解説

#### `@アノテーション名`
- 説明
- なぜ使うのか
- どう動作するのか
```

**ルール**:
- 大きなステップは `## 🚀 ステップN` で区切る
- 小さなタスクは `### N-1.` で番号付け
- コードブロックには必ず言語指定（```java など）
- ファイルパスは相対パスで明記
- 重要なアノテーションや概念は必ず解説

---

#### 4. 動作確認

```markdown
## ✅ ステップX: 動作確認

### X-1. コマンドで確認

​```bash
curl http://localhost:8080/endpoint
​```

**期待される結果**:
​```
Expected output
​```

### X-2. ブラウザで確認

- URLにアクセス
- 期待される画面・レスポンスを記載
```

**ルール**:
- 必ずcurlコマンドでの確認方法を記載
- 期待される結果を明示
- スクリーンショットの代わりに期待されるレスポンスを文字で記載

---

#### 5. チャレンジ課題（オプション）

```markdown
## 🎨 チャレンジ課題

基本が理解できたら、以下にチャレンジしてみましょう：

### チャレンジ 1: [課題名]

（説明と期待される結果）

​```java
// ヒントコード（完全な解答は避ける）
​```
```

**ルール**:
- 2〜3個のチャレンジを用意
- 難易度は段階的に上げる
- 完全な解答は書かず、ヒントに留める
- 学習者が考える余地を残す

---

#### 6. トラブルシューティング

```markdown
## 🐛 トラブルシューティング

### エラー: "エラーメッセージ"

**原因**: エラーの原因

**解決策**: 具体的な解決方法
```

**ルール**:
- よくあるエラーを3〜5個記載
- エラーメッセージは引用符で囲む
- 原因と解決策をセットで記載
- 解決策は具体的な手順を示す

---

#### 7. まとめ

```markdown
## 📚 このステップで学んだこと

- ✅ 学んだ内容1
- ✅ 学んだ内容2
- ✅ 学んだ内容3
```

**ルール**:
- チェックマーク（✅）で箇条書き
- 3〜7個程度
- 技術用語を含める

---

#### 8. 次のステップへの誘導

```markdown
## ➡️ 次のステップ

[Step X+1: タイトル](STEP_X+1.md)へ進みましょう！

（次のステップで学ぶことの簡単な紹介）
```

**ルール**:
- 相対リンクで次のステップに誘導
- 同じPhase内のステップは `[Step 2](STEP_2.md)` のように相対パス
- 別のPhaseに移る場合は `[Step 6](../phase2/STEP_6.md)` のように記載
- 次に学ぶことを1〜2文で紹介
- 学習意欲を高める文言を使用

---

### オプションセクション

#### 補足説明

```markdown
## 💡 補足: [トピック名]

（より深い理解のための補足情報）
```

**使用例**:
- Spring Bootの内部動作
- アーキテクチャの背景
- ベストプラクティス
- 歴史的経緯

---

## ✍️ 文体とトーン

### 基本方針

- **日本語で記載** する（すべてのドキュメントは日本語で執筆）
- **敬体（です・ます調）** を使用
- **親しみやすく、励ます** トーン
- **専門用語は必ず説明** する
- **ポジティブな表現** を心がける

### 技術スタックに関する方針

このカリキュラムでは以下の技術スタックを採用しています：

- **ビルドツール**: Maven（Gradleは扱いません）
- **IDE**: IntelliJ IDEA Community Edition（Eclipse、VS Codeは扱いません）
- **JDK**: OpenJDK 21

**Mavenに関する記述の注意点**:
- Mavenの基本的な使い方（pom.xml、依存関係管理など）は**カリキュラム内で必要に応じて説明**します
- 学習者がMavenの知識を事前に持っていることを前提としません
- ただし、Maven自体の深堀り（ライフサイクル、プラグイン開発など）は**このカリキュラムのスコープ外**とします
- 必要最小限の使い方（依存関係追加、ビルド、実行）のみ説明し、それ以上は公式ドキュメントを参照させます

### 良い例 ✅

```markdown
このアノテーションを使うことで、コードがシンプルになります。
試しに実行してみましょう！
```

### 悪い例 ❌

```markdown
このアノテーションを使え。
実行しろ。
```

---

## 🎨 フォーマットルール

### 絵文字の使用

各セクションの見出しには絵文字を使用して視覚的にわかりやすくします：

- 🎯 目標
- 📋 事前準備
- 🚀 ステップ/手順
- ✅ 動作確認
- 🎨 チャレンジ課題
- 🐛 トラブルシューティング
- 📚 まとめ/学んだこと
- ➡️ 次のステップ
- 💡 補足
- 📁 ファイル構成
- 🔧 設定/実装
- ▶️ 起動/実行
- 🔒 セキュリティ
- 🧪 テスト
- 📖 参考資料

### コードブロック

- **言語指定は必須**: ` ```java`、` ```bash`、` ```xml` など
- **ファイルパスをコメントまたは直前に記載**
- **重要な部分には inline code を使用**: `@RestController`

### 強調

- **太字**: 重要な用語、キーワード
- *斜体*: 補足的な説明（控えめに使用）
- `インラインコード`: クラス名、メソッド名、ファイル名、コマンド

### リンク

- **内部リンク**: 相対パスで記載 `[Step 2](STEP_2.md)`
- **外部リンク**: 公式ドキュメント優先 `[Spring Boot Reference](https://...)`

---

## 📊 コード例のルール

### コードの完全性

- **動作するコード** を記載（コンパイルエラーがないこと）
- **省略しない**: `// ... 省略` は避け、完全なコードを記載
- **インポート文**: 初出時は含める、以降は省略可

### コード例の構成

```java
package com.example.demo;  // パッケージ宣言

import org.springframework.web.bind.annotation.GetMapping;  // インポート（初回のみ）
import org.springframework.web.bind.annotation.RestController;

@RestController  // アノテーション
public class HelloController {  // クラス定義

    @GetMapping("/hello")  // メソッドアノテーション
    public String hello() {  // メソッド定義
        return "Hello, World!";  // ロジック
    }
}
```

### 段階的な追加

同じファイルに複数のメソッドを追加する場合：

```markdown
以下のメソッドを`HelloController.java`に**追加**します：

​```java
@GetMapping("/goodbye")
public String goodbye() {
    return "Goodbye!";
}
​```
```

**ルール**:
- 「追加」を明記
- 既存コードを再掲しない（混乱を避ける）

---

## 🎓 対象者への配慮

### 前提知識

- **Javaの基礎**: 理解していることを前提
- **オブジェクト指向**: 軽く復習程度は可
- **Spring Boot固有の概念**: 必ず説明

### 説明の粒度

- **初めての概念**: 丁寧に、例を交えて説明
- **Javaの基礎文法**: 簡潔に、または説明不要
- **応用的な内容**: 補足セクションで深掘り

### 例示の重要性

- **抽象的な説明の後には必ず具体例**
- **ユースケースを示す**
- **なぜ必要なのか** を明確に

---

## 📏 分量のガイドライン

### ページ長

- **1ステップ**: 500〜1500行程度（Markdown）
- **読了時間**: 10〜15分
- **実践時間**: 30〜60分

### セクションごとの目安

- **目標**: 3〜5項目
- **手順**: 3〜5ステップ
- **チャレンジ**: 2〜3課題
- **トラブルシューティング**: 3〜5項目

---

## ✅ レビューチェックリスト

ドキュメント作成後、以下を確認してください：

### 内容

- [ ] 目標が明確で測定可能か
- [ ] 手順が順番通りに実行できるか
- [ ] コードが動作するか（実際に試す）
- [ ] エラーメッセージが正確か
- [ ] 専門用語に説明があるか

### 形式

- [ ] 見出しレベルが適切か（H1は1つ、H2で大分類）
- [ ] コードブロックに言語指定があるか
- [ ] ファイルパスが記載されているか
- [ ] 絵文字が適切に使われているか
- [ ] リンクが正しいか（404でないか）

### 文体

- [ ] 敬体（です・ます調）で統一されているか
- [ ] 励ましのトーンがあるか
- [ ] 専門用語の説明があるか
- [ ] 誤字脱字がないか

---

## 🔄 更新とメンテナンス

### バージョン管理

- Spring Bootのバージョンアップ時は全体を見直す
- 非推奨APIが使われていないか確認
- 新機能が活用できるか検討

### フィードバック対応

- 学習者からのフィードバックを積極的に反映
- よくある質問はトラブルシューティングに追加
- わかりにくい箇所は補足説明を追加

---

## 🌟 良い例: Step 16の構成

Step 16（DI/IoCコンテナの深掘り）は、このカリキュラムの理想的な構成を示す良い例です。

### 優れている点

#### 1. 導入部分で「なぜ必要か」を明確に説明

```markdown
## 🧩 依存性注入（DI）とは

### DIがない世界

​```java
public class UserController {
    private UserService userService;
    
    public UserController() {
        // コンストラクタ内でnewしている = 密結合
        this.userService = new UserServiceImpl();
    }
}
​```

**問題点**:
- `UserController`が`UserServiceImpl`の具象クラスに依存
- テスト時にモックに差し替えられない
- 実装を変更する際にControllerも修正が必要

### DIがある世界

​```java
@RestController
public class UserController {
    private final UserService userService;
    
    // コンストラクタで外部から注入される
    public UserController(UserService userService) {
        this.userService = userService;
    }
}
​```

**メリット**:
- `UserController`は抽象（インターフェース）に依存
- テスト時に別実装を注入可能
- 疎結合で保守性が高い
```

**なぜ良いか**:
- 技術を学ぶ**動機**を与える
- **Before/After**のコード比較で違いが明確
- 具体的な**問題とメリット**を示す

#### 2. 視覚的な図解でアーキテクチャを説明

```markdown
### Springコンテナの役割

​```
┌─────────────────────────────────┐
│     Spring IoC Container        │
│                                 │
│    ┌─────────┐  ┌─────────┐     │
│    │  Bean   │  │  Bean   │     │
│    │ Service │  │  Repos  │     │
│    └─────────┘  └─────────┘     │
│         │          │            │
│         └──────────┘            │
│       自動的に依存関係を解決      │
└─────────────────────────────────┘
​```
```

**なぜ良いか**:
- **全体像**が一目でわかる
- Spring Frameworkの**中核概念**が理解しやすい
- テキストだけより**記憶に残る**

#### 3. 比較表で選択肢を明確化

```markdown
## � 依存性注入の3つの方法

| 方法 | メリット | デメリット | 推奨度 |
|---|---|---|---|
| コンストラクタ | finalにできる、必須依存が明確 | - | ⭐⭐⭐ |
| セッター | オプショナルな依存に対応 | finalにできない | ⭐ |
| フィールド | 記述が簡潔 | テストしにくい、循環依存に気づきにくい | ❌ |
```

**なぜ良いか**:
- **3つの方法を網羅的に比較**
- **推奨度**が一目で分かる
- 初学者が**適切な選択**をしやすい

#### 4. 実践的なコード例と段階的な改善

```markdown
### 1. コンストラクタインジェクション（推奨⭐⭐⭐）

​```java
@Service
public class UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;
    
    // コンストラクタが1つだけなら@Autowired省略可能
    public UserService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }
}
​```

**Lombokを使うとさらに簡潔**:

​```java
@Service
@RequiredArgsConstructor  // finalフィールドのコンストラクタ自動生成
public class UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;
}
​```
```

**なぜ良いか**:
- **基本→改善**の流れを示す
- **実務でよく使うパターン**（Lombok活用）を提示
- ボイラープレートを減らす**実践的テクニック**

#### 5. 実務でよくある問題とその解決方法

```markdown
## 🔍 複数のBeanがある場合の解決方法

### 問題: 同じインターフェースの実装が複数ある

​```java
@Service
public class EmailNotificationService implements NotificationService { }

@Service
public class SmsNotificationService implements NotificationService { }
​```

このとき、以下はエラーになります:

​```java
public NotificationController(NotificationService notificationService) {
    // ❌ どっちを注入すればいいか分からない！
}
​```

### 解決策1: `@Primary`で優先Beanを指定
### 解決策2: `@Qualifier`で指定
### 解決策3: カスタムBean名を指定
```

**なぜ良いか**:
- **実務で必ず遭遇する問題**を扱う
- **複数の解決策**を提示
- エラーメッセージと原因を明確に

#### 6. 実践的なデザインパターンの適用例

```markdown
## �️ 実践例: 戦略パターンとDI

### シナリオ: 支払い方法の切り替え

​```java
// 支払いインターフェース
public interface PaymentService {
    void pay(Long orderId, Integer amount);
}

@Service("credit")
public class CreditCardPaymentService implements PaymentService { }

@Service("paypal")
public class PayPalPaymentService implements PaymentService { }

@Service("bank")
public class BankTransferPaymentService implements PaymentService { }

// 戦略パターンで動的に切り替え
@Service
public class OrderService {
    private final Map<String, PaymentService> paymentServices;
    
    public OrderService(Map<String, PaymentService> paymentServices) {
        this.paymentServices = paymentServices;
    }
    
    public void checkout(Long orderId, Integer amount, String paymentMethod) {
        PaymentService paymentService = paymentServices.get(paymentMethod);
        paymentService.pay(orderId, amount);
    }
}
​```
```

**なぜ良いか**:
- **実務でよく使うデザインパターン**（戦略パターン）
- DIの**強力な活用例**
- 拡張性の高い**設計パターン**の実践

#### 7. Beanのスコープを表形式で整理

```markdown
## � Beanのスコープ

| スコープ | 説明 | 使用例 |
|---|---|---|
| **singleton** | アプリケーション全体で1つのインスタンス（デフォルト） | Service, Repository |
| **prototype** | 要求のたびに新しいインスタンスを生成 | ステートフルなオブジェクト |
| **request** | HTTPリクエストごとに1つ（Web環境のみ） | リクエストスコープのデータ保持 |
| **session** | HTTPセッションごとに1つ（Web環境のみ） | ユーザーセッション情報 |
```

**なぜ良いか**:
- **複数の概念を体系的に比較**
- **デフォルト値**を明示
- **使用例**で具体的なイメージを提供

#### 8. 発展課題でさらなる理解を促す

```markdown
## � 発展課題

### 課題1: Factory PatternとDI

プロダクト種別に応じた処理を行うファクトリーを実装してください。

### 課題2: 循環依存の解決

次のような循環依存を解決してください。

​```java
@Service
public class ServiceA {
    private final ServiceB serviceB;  // ❌ ServiceA → ServiceB
}

@Service
public class ServiceB {
    private final ServiceA serviceA;  // ❌ ServiceB → ServiceA
}
​```

**解決策**: 設計を見直し、共通のServiceを抽出する
```

**なぜ良いか**:
- **よくあるアンチパターン**を課題として提示
- **設計力**を養う内容
- 解決策のヒントを示しつつ、考える余地を残す

### Step 16から学ぶべき構成のポイント

1. **Why（なぜ）→ What（何を）→ How（どうやって）** の順序
2. **Before/Afterの比較**で問題と解決策を明確に
3. **視覚的な図解**（アスキーアート、表）で概念を説明
4. **比較表**で複数の選択肢を整理
5. **実務でよくある問題**とその解決パターンを提示
6. **段階的な改善**（基本→Lombok活用など）
7. **デザインパターン**との組み合わせ例
8. **発展課題**で設計力を養う

### 他のステップ作成時のチェックリスト

- [ ] 導入部分で「なぜこの技術が必要か」を説明しているか
- [ ] Before/Afterのコード比較で違いを明確にしているか
- [ ] 図解やテーブルで視覚的に理解しやすくしているか
- [ ] 複数の選択肢がある場合、比較表で整理しているか
- [ ] 実務でよくある問題とその解決方法を示しているか
- [ ] 基本→改善の流れで段階的に説明しているか
- [ ] デザインパターンなど実践的な応用例があるか
- [ ] 発展課題で深い理解を促しているか
- [ ] ベストプラクティスを明示しているか
- [ ] よくあるアンチパターンや注意点を示しているか
- [ ] よくあるエラーとその解決策を記載しているか

---

## 📚 参考リソース

### 公式ドキュメント

- [Spring Boot Reference Documentation](https://spring.pleiades.io/spring-boot/)
- [Spring Framework API](https://docs.spring.io/spring-framework/docs/current/javadoc-api/)
- [MyBatis 3 Documentation](https://mybatis.org/mybatis-3/ja/)
- [MyBatis-Spring Documentation](https://mybatis.org/spring/ja/)
- [Thymeleaf Documentation](https://www.thymeleaf.org/documentation.html)
- [Thymeleaf Tutorial](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html)

### スタイルガイド

- [Google Developer Documentation Style Guide](https://developers.google.com/style)
- [Microsoft Writing Style Guide](https://learn.microsoft.com/en-us/style-guide/welcome/)

### Markdown

- [GitHub Flavored Markdown Spec](https://github.github.com/gfm/)

---

## 💬 コントリビューション

カリキュラムの改善提案は大歓迎です！

1. このルールに従ってドキュメントを作成
2. Pull Requestを作成
3. レビューチェックリストを確認済みであることを明記

---

**最終更新**: 2025-10-27
**対象バージョン**: Spring Boot 3.5.7
