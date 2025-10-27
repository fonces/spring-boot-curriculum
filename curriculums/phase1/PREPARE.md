# Phase 1: 事前準備

Phase 1のカリキュラムを始める前に、開発環境を整えましょう。

---

## 🎯 このドキュメントの目的

- 必要な開発ツールをインストールする
- 各ツールが正しく動作することを確認する
- Spring Bootの開発に最適な環境を構築する

**所要時間**: 約1時間

---

## 📋 必要なツール一覧

Phase 1では以下のツールを使用します：

- ✅ **OpenJDK 21**: Javaの実行環境
- ✅ **Maven 3.8+**: ビルドツール
- ✅ **IntelliJ IDEA Community Edition**: 統合開発環境
- ✅ **curl**: APIテスト用コマンドラインツール
- ✅ **Git**: バージョン管理（オプションだが推奨）

---

## ☕ OpenJDK 21のインストール

### Windows

#### 方法1: Microsoft Build of OpenJDK（推奨）

1. [Microsoft OpenJDK 21](https://learn.microsoft.com/ja-jp/java/openjdk/download#openjdk-21)にアクセス

2. 「Windows x64」の`.msi`インストーラーをダウンロード

3. ダウンロードした`.msi`ファイルを実行

4. インストールウィザードの指示に従う（デフォルト設定でOK）

5. インストール完了後、環境変数が自動設定される

#### 方法2: Chocolatey（パッケージマネージャー使用）

PowerShellを管理者権限で開いて実行：

```powershell
choco install microsoft-openjdk21
```

### macOS

#### 方法1: Homebrew（推奨）

ターミナルで以下を実行：

```bash
# Homebrewがインストールされていない場合は先にインストール
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# OpenJDK 21をインストール
brew install openjdk@21

# シンボリックリンクを作成
sudo ln -sfn $(brew --prefix)/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk
```

#### 方法2: SDKMAN!

```bash
# SDKMAN!をインストール
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# OpenJDK 21をインストール
sdk install java 21-open
```

### Linux (Ubuntu/Debian)

```bash
# パッケージリストを更新
sudo apt update

# OpenJDK 21をインストール
sudo apt install openjdk-21-jdk -y
```

### Linux (Fedora/RHEL/CentOS)

```bash
# OpenJDK 21をインストール
sudo dnf install java-21-openjdk-devel -y
```

### インストール確認

ターミナル/コマンドプロンプトで以下を実行：

```bash
java -version
```

**期待される出力例**:
```
openjdk version "21.0.x" 2024-xx-xx
OpenJDK Runtime Environment (build 21.0.x+xx)
OpenJDK 64-Bit Server VM (build 21.0.x+xx, mixed mode, sharing)
```

バージョンが`21.0.x`と表示されればOK！

---

## 🔧 Mavenのインストール

Maven（メイヴン）は、Javaプロジェクトのビルド、依存関係管理、プロジェクト管理を行うツールです。
Spring Bootプロジェクトでは、ライブラリの管理やアプリケーションのビルドに使用します。

### Windows

1. [Maven公式サイト](https://maven.apache.org/download.cgi)から`apache-maven-3.9.x-bin.zip`をダウンロード

2. `C:\Program Files\Apache\maven`に解凍

3. 環境変数を設定：
   - システム環境変数に`MAVEN_HOME`を追加: `C:\Program Files\Apache\maven`
   - システム環境変数の`Path`に追加: `%MAVEN_HOME%\bin`

4. コマンドプロンプトを**再起動**して確認

#### Chocolateyを使う方法（推奨）

PowerShellを管理者権限で開いて実行：

```powershell
choco install maven
```

### macOS

#### Homebrew（推奨）

```bash
brew install maven
```

### Linux

#### Ubuntu/Debian

```bash
sudo apt update
sudo apt install maven -y
```

#### Fedora/RHEL/CentOS

```bash
sudo dnf install maven -y
```

### インストール確認

ターミナル/コマンドプロンプトで以下を実行：

```bash
mvn -v
```

**期待される出力例**:
```
Apache Maven 3.9.x (xxxxx)
Maven home: /usr/share/maven
Java version: 21.0.x, vendor: Oracle Corporation, runtime: /path/to/jdk-21
Default locale: ja_JP, platform encoding: UTF-8
OS name: "mac os x", version: "14.x", arch: "aarch64", family: "mac"
```

**重要**: Java versionが`21.0.x`と表示されていることを確認してください！

---

## 💻 IntelliJ IDEA Community Editionのインストール

IntelliJ IDEA Community Editionは、Spring Boot開発に最適な統合開発環境（IDE）です。
無料で使用でき、Spring Bootの開発に必要な機能がすべて揃っています。

### インストール手順

#### Windows/macOS/Linux共通

1. [IntelliJ IDEA公式サイト](https://www.jetbrains.com/idea/download/)にアクセス

2. **Community Edition**（無料版）の「Download」ボタンをクリック
   - ⚠️ Ultimate版（有料）ではなく、Community版を選んでください

3. ダウンロードしたインストーラーを実行

4. インストールウィザードの指示に従う
   - **Windows**: デフォルト設定でOK
   - **macOS**: Applicationsフォルダにドラッグアンドドロップ
   - **Linux**: 解凍して`bin/idea.sh`を実行

### 初回起動設定

1. IntelliJ IDEAを起動

2. **データ共有**: お好みで選択（「Don't Send」でもOK）

3. **テーマ選択**: IntelliJ Light または Darcula（ダークテーマ）を選択

4. **プラグイン**: デフォルトのままで「Start using IntelliJ IDEA」をクリック

### Spring Bootプラグインの確認

IntelliJ IDEA Community Editionには、Spring Boot開発に必要なプラグインが標準で含まれています。

#### 確認手順

1. メニューから `File` → `Settings`（macOSは`IntelliJ IDEA` → `Preferences`）を開く

2. 左側で `Plugins` を選択

3. 検索ボックスに「Spring」と入力

4. 以下のプラグインがインストール済み（Enabled）であることを確認：
   - **Spring Boot**
   - **Spring Core**
   - **Spring MVC**

すべて標準でインストールされているので、追加作業は不要です！

### JDKの設定確認

IntelliJ IDEAでOpenJDK 21が認識されているか確認します。

1. `File` → `Project Structure`（macOSは`⌘;`）を開く

2. `Platform Settings` → `SDKs`を確認

3. OpenJDK 21が表示されていればOK

#### 表示されていない場合

1. `+` ボタンをクリック → `Add JDK`

2. OpenJDK 21をインストールしたディレクトリを選択
   - **Windows**: `C:\Program Files\Java\jdk-21`
   - **macOS (Homebrew)**: `/opt/homebrew/opt/openjdk@21`
   - **Linux**: `/usr/lib/jvm/java-21-openjdk`

3. 「OK」をクリック

### 便利な設定（オプション）

#### 自動インポートの有効化

1. `File` → `Settings` → `Editor` → `General` → `Auto Import`

2. 以下にチェック：
   - ✅ `Add unambiguous imports on the fly`
   - ✅ `Optimize imports on the fly`

#### Mavenの自動リロード

1. `File` → `Settings` → `Build, Execution, Deployment` → `Build Tools` → `Maven`

2. 以下にチェック：
   - ✅ `Reload project after changes in the build scripts`
   - `Any changes`を選択

これで、`pom.xml`を編集すると自動的に依存関係がダウンロードされます。

### IntelliJ IDEAの基本的な使い方

#### プロジェクトを開く

- `File` → `Open` → プロジェクトディレクトリを選択
- `pom.xml`がある場合、自動的にMavenプロジェクトとして認識されます

#### アプリケーションの実行

- メインクラス（`@SpringBootApplication`付き）を開く
- `main`メソッドの左側にある緑の▶ボタンをクリック
- または `Shift + F10`（macOSは`⌃R`）

#### ショートカットキー（覚えると便利）

| 操作 | Windows/Linux | macOS |
|------|---------------|-------|
| クラス検索 | `Ctrl + N` | `⌘O` |
| ファイル検索 | `Ctrl + Shift + N` | `⌘⇧O` |
| 実行 | `Shift + F10` | `⌃R` |
| デバッグ | `Shift + F9` | `⌃D` |
| コード補完 | `Ctrl + Space` | `⌃Space` |
| コードフォーマット | `Ctrl + Alt + L` | `⌘⌥L` |

---

## 🌐 curlのインストール

### Windows

Windows 10/11には標準でcurlがインストールされています。

#### 確認

```powershell
curl --version
```

インストールされていない場合：

```powershell
# Chocolatey経由
choco install curl
```

### macOS

macOSには標準でcurlがインストールされています。

```bash
curl --version
```

### Linux

ほとんどのディストリビューションに標準インストール済み。

```bash
# Ubuntu/Debianでインストールされていない場合
sudo apt install curl -y

# Fedora/RHEL/CentOS
sudo dnf install curl -y
```

---

## 📦 Git（バージョン管理）のインストール（推奨）

各ステップの成果をGitで管理することを推奨します。

### Windows

1. [Git公式サイト](https://git-scm.com/download/win)からダウンロード

2. インストーラーを実行（デフォルト設定でOK）

3. Git Bashも一緒にインストールされます

### macOS

```bash
# Homebrew経由
brew install git

# またはXcode Command Line Tools
xcode-select --install
```

### Linux

```bash
# Ubuntu/Debian
sudo apt install git -y

# Fedora/RHEL/CentOS
sudo dnf install git -y
```

### 初期設定

インストール後、以下の設定を行います：

```bash
git config --global user.name "あなたの名前"
git config --global user.email "your.email@example.com"
```

### 確認

```bash
git --version
```

**期待される出力例**:
```
git version 2.x.x
```

---

## ✅ 環境確認チェックリスト

すべてのツールが正しくインストールされているか確認しましょう。

ターミナル/コマンドプロンプトで以下を順番に実行：

```bash
# Java確認
java -version

# Maven確認
mvn -v

# curl確認
curl --version

# Git確認（オプション）
git --version
```

### チェックリスト

- [ ] `java -version`が21.0.xを表示
- [ ] `mvn -v`が正常に表示（Java version: 21.0.xも確認）
- [ ] IDEが起動できる
- [ ] `curl --version`が表示される
- [ ] `git --version`が表示される（オプション）

**すべてチェックできたら準備完了です！** 🎉

---

## 🐛 トラブルシューティング

### Javaのバージョンが違う

**症状**: `java -version`で21以外が表示される

**原因**: 複数のJavaがインストールされている

**解決策**:

#### Windows
1. 「システム環境変数の編集」を開く
2. 「環境変数」ボタンをクリック
3. システム環境変数の`JAVA_HOME`を確認
4. OpenJDK 21のパスに設定（例: `C:\Program Files\Java\jdk-21`）
5. `Path`の先頭に`%JAVA_HOME%\bin`があることを確認

#### macOS/Linux
`~/.bashrc`または`~/.zshrc`に以下を追加：

```bash
# Homebrewでインストールした場合
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# 手動でパスを指定する場合
export JAVA_HOME=/path/to/jdk-21
export PATH=$JAVA_HOME/bin:$PATH
```

設定を反映：
```bash
source ~/.bashrc  # または source ~/.zshrc
```

### Mavenが認識されない

**症状**: `mvn -v`で「コマンドが見つかりません」

**原因**: 環境変数のPathが正しく設定されていない

**解決策**: 上記の「Mavenのインストール」セクションを再確認

### IDEでJDKが認識されない

**症状**: IntelliJ IDEAでプロジェクトを開いてもJDKエラーが出る

**解決策**:

1. `File` → `Project Structure`（`Ctrl + Alt + Shift + S`）

2. `Project Settings` → `Project`

3. `SDK`で「Add SDK」→「Download JDK」

4. Vendor: `Oracle OpenJDK`, Version: `21`を選択

5. 「Download」をクリック

または、既にインストールしたJDKを指定：

1. `SDK`で「Add SDK」→「Add JDK」

2. OpenJDK 21のインストールパスを選択

---

## 🎓 補足: 開発環境について

### なぜOpenJDK 21？

- **長期サポート（LTS）版**: 2029年9月までサポート
- **最新機能**: レコード、パターンマッチング、仮想スレッドなど
- **Spring Boot 3.x対応**: 最小要件はJava 17ですが、21で最新機能を活用

### Mavenとは？

**Maven（メイヴン）** は、Javaプロジェクトのビルド自動化ツールです。

主な機能：
- **依存関係管理**: ライブラリを自動ダウンロード・管理
- **プロジェクト構造の標準化**: `src/main/java`などの決まった構造
- **ビルドライフサイクル**: コンパイル、テスト、パッケージングを自動化

**pom.xml**というファイルに設定を記述します。Spring Bootでは、必要なライブラリを`pom.xml`に書くだけで自動的にダウンロードされます。

### IntelliJ IDEA Community Editionの利点

**Spring Boot開発に最適な理由**：
- **Spring専用サポート**: Spring Bootプロジェクトを自動認識
- **コード補完**: アノテーションや設定の自動補完が強力
- **リファクタリング**: 安全なコード変更をサポート
- **デバッガ**: ブレークポイントを使った効率的なデバッグ
- **Maven統合**: `pom.xml`の編集で自動的に依存関係をダウンロード
- **Git統合**: バージョン管理がIDE内で完結

Community Edition（無料版）でも、Spring Boot開発に必要な機能はすべて揃っています。

---

## 📚 参考リンク

### 公式ドキュメント

- [OpenJDK公式サイト](https://openjdk.org/)
- [Maven公式サイト](https://maven.apache.org/)
- [IntelliJ IDEA](https://www.jetbrains.com/idea/)
- [Spring Initializr](https://start.spring.io/)

### インストールガイド

- [Microsoft OpenJDK](https://learn.microsoft.com/ja-jp/java/openjdk/download)
- [IntelliJ IDEA ドキュメント](https://www.jetbrains.com/help/idea/)
- [Maven入門ガイド](https://maven.apache.org/guides/getting-started/)

---

## ➡️ 次のステップ

環境構築が完了したら、いよいよSpring Bootの学習を開始しましょう！

[Step 1: Hello World REST API](STEP_1.md)へ進む

最初のSpring Bootアプリケーションを作成し、REST APIを動かしてみます。

---

**準備完了おめでとうございます！** 🚀

ここからが本当のスタートです。楽しんで学んでいきましょう！
