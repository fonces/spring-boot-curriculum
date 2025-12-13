# Spring Boot カリキュラム ドキュメント作成ルール

このドキュメントは、Spring Boot 3.5.8カリキュラムのステップガイドを作成する際の統一ルールを定義します。

---

## 🖥️ 対象プラットフォーム

### 基本方針

- **主要対象**: macOS
- **Windows環境**: WSL2（Windows Subsystem for Linux 2）上で完結するように記述
  - PowerShellは使用しない
  - 基本的なコマンド例はUnix系（bash）を前提とする
- 複数ステップを作成する際は**サブエージェント活用**を推奨

### コマンド例の記載ルール

**基本形**（macOS/Linux/WSL2共通）:
```bash
./mvnw spring-boot:run
```

**Windows固有の補足が必要な場合**:
```bash
# macOS/Linux/WSL2
curl http://localhost:8080/api/users
```

> **💡 Windows利用者への配慮**: 
> - 基本的にWSL2環境での実行を推奨
> - どうしてもWindowsネイティブで実行する必要がある場合のみPowerShell例を併記

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
- **事前準備ガイド**: `curriculums/phaseX/PREPARE.md` （Phaseの開始前に必要な環境構築や設定手順）
- **サンプルコード**: `examples/phaseX/step-y-brief-description/` （小文字、ハイフン区切り）
- **Phase内のステップ番号**: 全体通しの番号を使用（Phase 1は1-5、Phase 2は6-11など）

### 事前準備ガイド（PREPARE.md）の作成

各Phaseで特別な環境構築や事前設定が必要な場合は、`phaseX/PREPARE.md` に手順を記載します。

**PREPARE.mdが必要なケース**:
- 新しいツールやミドルウェアのインストール（例: MySQL, Docker）
- 開発環境の設定変更（例: IDE設定、拡張機能）
- Phase全体で使用する共通設定（例: データベース初期化）

**記載例**:
- `phase1/PREPARE.md`: Java、Maven、VSCode環境構築
- `phase2/PREPARE.md`: MySQL環境構築準備
- `phase8/PREPARE.md`: 総合演習用のプロジェクト初期設定

各ステップ（STEP_X.md）の「事前準備」セクションでは、必要に応じて該当Phase のPREPARE.mdへのリンクを記載します。

---

## 🎯 カリキュラム設計の基本方針

### 手戻りを最小限にする段階的構成

学習者が後のステップで「前のステップに戻って修正が必要」という状況を可能な限り避けるため、以下の原則に従います：

**基本原則**:
1. **最初から拡張可能な設計**: 後のステップで機能追加する際に、既存コードの大幅な書き換えが不要な構成
2. **段階的な機能追加**: 各ステップで新しい概念を1つずつ追加し、既存の実装を活かす
3. **後方互換性の維持**: 新しいステップで学ぶ内容が、前のステップの実装を無効化しない

**具体的な実践例**:
- ✅ **良い例**: Step 8でUserエンティティを作成 → Step 19でDTO（UserCreateRequest, UserResponse）を追加し、Controllerでのみ使用
- ❌ **悪い例**: Step 8でUserを直接使用 → Step 19で「Userを使っている箇所をすべて書き換え」

**ステップ作成時のチェックポイント**:
- [ ] 前のステップで作成したコードを再利用できるか
- [ ] 新しい概念を追加する際、既存のファイルの修正は最小限か
- [ ] 後のステップで使う可能性がある部分を考慮した設計か
- [ ] 「Step Xに戻って○○を修正してください」という指示が不要か

**例外的に手戻りが許容されるケース**:
- リファクタリングを学ぶステップ（明示的に「改善」が目的の場合）
- Phase間の移行（Phase 2→Phase 4でアーキテクチャを整理するなど）
- セキュリティやテストの追加（既存機能への影響を最小限に）

---

## 📝 ステップガイド（STEP_X.md）の作成

ステップガイドの詳細な構成ルールは、[`.github/prompts/create-step.prompt.md`](prompts/create-step.prompt.md)を参照してください。

### 概要

各ステップガイドは以下の要素を含む必要があります：

- **8つの必須セクション**: タイトルと目標、事前準備、手順、動作確認、チャレンジ課題、トラブルシューティング、まとめ、次のステップへの誘導
- **文体**: 日本語、敬体（です・ます調）、親しみやすく励ますトーン
- **コード**: 動作する完全なコード、言語指定、ファイルパス明記
- **分量**: 500〜1500行、読了時間10〜15分、実践時間30〜60分

---

## 🛠️ カリキュラム実装の検証ルール

### Copilotによる実装検証

カリキュラムの内容が正確で実行可能であることを保証するため、以下のルールに従って実装検証を行います：

**基本方針**:
1. **実装場所**: `workspace/*` ディレクトリにプロジェクトを配置
2. **検証フロー**: カリキュラム読み取り → 実装 → ビルド → 実行 → 動作確認
3. **完全性の確保**: ドキュメントに記載されたすべてのコードが動作することを確認

**実装検証の手順**:

1. **カリキュラムの読み込み**
   - 対象ステップ（STEP_X.md）の内容を完全に読み取る
   - 事前準備セクションで必要な前提条件を確認

2. **プロジェクトの作成/更新**
   - `workspace/hello-spring-boot/` などの適切なディレクトリにプロジェクトを配置
   - 前のステップからの継続の場合は既存コードを利用

3. **コードの実装**
   - ステップガイドに記載されたコードをそのまま実装
   - ファイルパスを正確に再現

4. **ビルドと実行**
   ```bash
   cd workspace/hello-spring-boot
   ./mvnw clean install
   ./mvnw spring-boot:run
   ```

5. **動作確認**
   - ステップガイドの「動作確認」セクションに記載されたコマンドを実行
   - 期待される結果と一致することを確認

6. **エラーの修正**
   - ビルドエラーや実行エラーが発生した場合、ステップガイドの内容を修正
   - 修正内容をコミットメッセージに記録

**検証時のチェックポイント**:
- [ ] すべてのコードがコンパイルエラーなく動作するか
- [ ] curlコマンドなどの動作確認が成功するか
- [ ] 前のステップからの継続性が保たれているか
- [ ] 環境依存の問題がないか（macOS/WSL2で動作するか）

**workspace構成例**:
```
workspace/
├── hello-spring-boot/          # Phase 1-4の基本実装
├── hello-spring-boot-thymeleaf/ # Phase 5のThymeleaf実装
├── hello-spring-boot-security/  # Phase 6のセキュリティ実装
└── final-project/              # Phase 8の総合演習
```

---

## 📚 参考リソース

### 公式ドキュメント

- [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/reference/3.5.8/)
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

**最終更新**: 2025-12-13
**対象バージョン**: Spring Boot 3.5.8
