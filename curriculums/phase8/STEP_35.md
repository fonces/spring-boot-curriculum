# Step 35: エンティティとMapper実装

## 🎯 このステップの目標

- 全エンティティクラスを実装する
- Enumクラスを定義する
- **MyBatis Mapperインターフェース**を作成する
- 基本的なCRUD操作を実装する

**所要時間**: 約3時間

> **このステップはあなたが実装します！**
> 
> STEP_34で設計した内容をもとに、実際にコードを書いてください。
> わからない場合は`example/STEP_35_*.java`の実装例を参考にしてください。

---

## 📋 実装課題

### 課題1: Enumクラスの実装（3つ）

以下のEnumクラスを実装してください：

**TaskStatus.java** (`src/main/java/com/example/taskapp/entity/enums/`)

必要なEnum：
- `TaskStatus` - タスクの状態（TODO, IN_PROGRESS, DONE）
- `Priority` - 優先度（LOW, MEDIUM, HIGH）
- `ProjectRole` - プロジェクトでの役割（OWNER, MEMBER）

各Enumには表示用の日本語名や色情報を持たせると便利です。

> **💡 ヒント**: `example/STEP_35_entity_example.java`に完全な実装例があります

---

### 課題2: エンティティクラスの実装

STEP_34で設計したテーブル定義をもとに、POJOクラス（Plain Old Java Object）を実装してください。

**実装するエンティティ：**
- `User.java`
- `Project.java`
- `Task.java`
- `Comment.java`
- `Tag.java`

**考えてほしいこと：**
- MyBatisではJPAの`@Entity`アノテーションは不要
- Lombokの`@Data`を使うとgetter/setterを自動生成できる
- JOIN結果を受け取るためのフィールド（例：`projectName`, `assigneeName`）も追加できる

> **💡 ヒント**: `example/STEP_35_entity_example.java`に完全な実装例があります

---

### 課題3: MyBatis Mapperインターフェースの実装

各エンティティに対応するMapperインターフェースを実装してください。

**実装するMapper：**
- `TaskMapper.java`
- `ProjectMapper.java`
- `CommentMapper.java`
- `TagMapper.java`
- `UserMapper.java`

**考えてほしいこと：**
- どんなCRUD操作が必要か？（insert, findById, update, delete...）
- どんな検索メソッドが必要か？（findByProjectId, findByStatus...）
- アノテーション（`@Select`, `@Insert`, `@Update`, `@Delete`）をどう使うか？

> **💡 ヒント**: `example/STEP_35_mapper_example.java`に完全な実装例があります

---

### 課題4: MyBatis設定ファイル

**application.yml**にMyBatis設定を追加してください：

**考えてほしいこと：**
- XMLマッパーの配置場所を指定
- スネークケース→キャメルケース変換の有効化
- タイプエイリアスパッケージの設定

**schema.sql**（STEP_34で作成したもの）を`src/main/resources/`に配置してください。

---

## ✅ チェックリスト

実装が完了したら確認してください：

- [ ] Enumクラス3つ（TaskStatus, Priority, ProjectRole）を実装
- [ ] Entityクラス5つ（User, Project, Task, Comment, Tag）を実装
- [ ] Mapperインターフェース5つを実装
- [ ] 各MapperにCRUDメソッドを定義
- [ ] application.ymlにMyBatis設定を追加
- [ ] schema.sqlを配置


---

## 🧪 動作確認

### アプリケーション起動確認

```bash
./mvnw spring-boot:run
```

エラーなく起動し、テーブルが作成されればOKです！

**データベースの確認方法：**
- **DBeaver** (推奨): 無料で使いやすいDBビューアー
  - [https://dbeaver.io/](https://dbeaver.io/)
  - MySQL、PostgreSQL、H2など多数のDBに対応
- **その他のツール**: DataGrip、MySQL Workbench、pgAdminなど

> **💡 ヒント**: DBeaverで接続情報を設定して、テーブル構造とデータを確認しましょう

---

## 💡 参考実装例

- `example/STEP_35_entity_example.java` - Enum・Entity完全実装
- `example/STEP_35_mapper_example.java` - Mapper完全実装

---

## 📚 このステップで学んだこと

- ✅ MyBatis用POJOエンティティの実装
- ✅ Enumクラスの活用
- ✅ MyBatis Mapperインターフェースの作成
- ✅ アノテーションベースのSQL定義
- ✅ MyBatisの設定方法

---

## 🔄 Gitへのコミット

```bash
git add .
git commit -m "Step 35: エンティティとMapper実装完了"
git push origin main
```

---

## ➡️ 次のステップ

次は[Step 36: サービスとコントローラー実装](STEP_36.md)へ進みましょう！

---

お疲れさまでした！ 🎉
