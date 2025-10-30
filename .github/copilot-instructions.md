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

## 📝 ステップガイド（STEP_X.md）の作成

ステップガイドの詳細な構成ルールは、[`.github/prompts/create-step.prompt.md`](prompts/create-step.prompt.md)を参照してください。

### 概要

各ステップガイドは以下の要素を含む必要があります：

- **8つの必須セクション**: タイトルと目標、事前準備、手順、動作確認、チャレンジ課題、トラブルシューティング、まとめ、次のステップへの誘導
- **文体**: 日本語、敬体（です・ます調）、親しみやすく励ますトーン
- **コード**: 動作する完全なコード、言語指定、ファイルパス明記
- **分量**: 500〜1500行、読了時間10〜15分、実践時間30〜60分

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
