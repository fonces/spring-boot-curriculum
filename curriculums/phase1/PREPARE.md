# Phase 1 事前準備: 開発環境のセットアップ

## 🎯 この準備ガイドの目標

Phase 1のステップを始める前に、以下の開発環境を整えます：

- Java 21 (OpenJDK) をインストールし、動作確認ができる
- Mavenの基本を理解し、mvnwラッパーで実行できる
- VSCodeとSpring Boot開発に必要な拡張機能をセットアップできる
- 開発環境が正しく動作していることを確認できる

**所要時間**: 約30分〜1時間（ダウンロード速度により変動）

---

## 📋 前提条件

このガイドでは、以下の環境を前提とします：

- **macOS** または **Windows + WSL2 (Ubuntu)** が利用可能
- インターネット接続がある
- ターミナル（macOSのTerminal.app、WSL2のbash）の基本操作ができる

> **💡 Windows利用者へ**:  
> このカリキュラムはWSL2（Windows Subsystem for Linux 2）での実行を推奨します。  
> WSL2のインストールがまだの場合は、[Microsoft公式ガイド](https://learn.microsoft.com/ja-jp/windows/wsl/install)を参照してください。

---

## 🚀 ステップ1: Java 21のインストール

Spring Boot 3.5.8はJava 21を推奨バージョンとしています。OpenJDK 21をインストールしましょう。

### 1-1. macOSでのインストール (Homebrew使用)

#### Homebrewのインストール確認

まず、Homebrewがインストールされているか確認します：

```bash
brew --version
```

**期待される結果**:
```sh
Homebrew 4.x.x
```

Homebrewがインストールされていない場合は、以下のコマンドでインストールします：

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

#### OpenJDK 21のインストール

Homebrewを使ってOpenJDK 21をインストールします：

```bash
brew install openjdk@21
```

インストールが完了したら、シンボリックリンクを作成してシステムがJavaを認識できるようにします：

```bash
sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk
```

> **💡 Intel Macの場合**: `/opt/homebrew/`の代わりに`/usr/local/`を使用します。
> ```bash
> sudo ln -sfn /usr/local/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk
> ```

#### JAVA_HOMEの設定（推奨）

`.zshrc`（または`.bash_profile`）に以下を追加します：

```bash
# .zshrcを開く
nano ~/.zshrc
```

ファイルの最後に以下を追加：

```bash
# Java 21
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
```

変更を反映：

```bash
source ~/.zshrc
```

---

### 1-2. WSL2 (Ubuntu)でのインストール

#### パッケージリストの更新

まず、パッケージリストを最新にします：

```bash
sudo apt update
```

#### OpenJDK 21のインストール

```bash
sudo apt install openjdk-21-jdk -y
```

#### デフォルトJavaバージョンの設定

複数のJavaバージョンがインストールされている場合は、以下のコマンドでJava 21をデフォルトに設定します：

```bash
sudo update-alternatives --config java
```

表示されるリストから、`/usr/lib/jvm/java-21-openjdk-amd64/bin/java`を選択します。

#### JAVA_HOMEの設定（推奨）

`.bashrc`に以下を追加します：

```bash
# .bashrcを開く
nano ~/.bashrc
```

ファイルの最後に以下を追加：

```bash
# Java 21
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
```

変更を反映：

```bash
source ~/.bashrc
```

---

### 1-3. Javaのインストール確認

以下のコマンドでJavaが正しくインストールされているか確認します：

```bash
java -version
```

**期待される結果**:
```sh
openjdk version "21.0.x" 2024-xx-xx
OpenJDK Runtime Environment (build 21.0.x+xx)
OpenJDK 64-Bit Server VM (build 21.0.x+xx, mixed mode, sharing)
```

バージョンが`21.x.x`であることを確認してください。

次に、Javaコンパイラも確認します：

```bash
javac -version
```

**期待される結果**:
```sh
javac 21.0.x
```

環境変数も確認：

```bash
echo $JAVA_HOME
```

**期待される結果（macOSの例）**:
```sh
/Library/Java/JavaVirtualMachines/openjdk-21.jdk/Contents/Home
```

**期待される結果（WSL2の例）**:
```sh
/usr/lib/jvm/java-21-openjdk-amd64
```

---

## 🚀 ステップ2: Mavenの理解とmvnwの確認

### 2-1. Mavenとは

**Maven**は、Javaプロジェクトのビルドツールです。主な役割は以下の通りです：

- **依存関係の管理**: ライブラリ（Spring Boot、MySQLドライバなど）を自動でダウンロード
- **ビルドの自動化**: コンパイル、テスト、パッケージングを統一的に実行
- **プロジェクト構成の標準化**: ディレクトリ構造を統一

### 2-2. Maven Wrapper（mvnw）の利点

このカリキュラムでは、**Maven Wrapper**（`mvnw`）を使用します。

```sh
📁 プロジェクトのルートディレクトリ
├── mvnw          # Unix/Linux/macOS用の実行スクリプト
├── mvnw.cmd      # Windows用（WSL2では使用しません）
└── .mvn/         # Mavenラッパーの設定
```

**Maven Wrapperのメリット**:
- Mavenをシステムにインストールする必要がない
- プロジェクトごとに指定されたMavenバージョンを自動使用
- チーム開発でバージョンの統一が簡単

**通常のMaven vs Maven Wrapper**:

| コマンド | 通常のMaven | Maven Wrapper |
|---------|------------|---------------|
| ビルド | `mvn clean install` | `./mvnw clean install` |
| 実行 | `mvn spring-boot:run` | `./mvnw spring-boot:run` |
| テスト | `mvn test` | `./mvnw test` |

> **💡 重要**: カリキュラムでは常に`./mvnw`を使用します。`mvn`コマンドは使いません。

### 2-3. Mavenのインストール（オプション）

Maven Wrapper（`mvnw`）を使う場合、Mavenのインストールは**必須ではありません**。

ただし、新規プロジェクトの作成などで`mvn`コマンドが必要になる場合があります。その際は以下でインストールできます：

#### macOSの場合:

```bash
brew install maven
```

#### WSL2の場合:

```bash
sudo apt install maven -y
```

#### 確認:

```bash
mvn -version
```

**期待される結果**:
```sh
Apache Maven 3.9.x (xxxxx)
Maven home: /usr/share/maven
Java version: 21.0.x, vendor: Oracle Corporation
```

---

## 🚀 ステップ3: VSCodeのインストールと設定

### 3-1. VSCodeのインストール

#### macOSの場合

[Visual Studio Code公式サイト](https://code.visualstudio.com/)からmacOS版をダウンロードしてインストールします。

または、Homebrewでインストール：

```bash
brew install --cask visual-studio-code
```

#### WSL2の場合

Windows側にVSCodeをインストールします（WSL2内ではなくWindows側）：

1. [Visual Studio Code公式サイト](https://code.visualstudio.com/)からWindows版をダウンロード
2. インストーラーを実行
3. インストール完了後、WSL2のターミナルから以下のコマンドでVSCodeを開けます：

```bash
code .
```

初回実行時、「WSL: Ubuntu」などの拡張機能が自動でインストールされます。

---

### 3-2. 必須拡張機能のインストール

VSCodeを起動したら、以下の拡張機能をインストールします。

#### Extension Pack for Java（必須）

Java開発に必要な拡張機能のセットです。以下が含まれます：

- Language Support for Java (Red Hat)
- Debugger for Java
- Test Runner for Java
- Maven for Java
- Project Manager for Java
- IntelliCode

**インストール方法**:

1. VSCodeのサイドバーから拡張機能アイコンをクリック（またはCmd+Shift+X / Ctrl+Shift+X）
2. 検索ボックスに`Extension Pack for Java`と入力
3. Microsoft提供のものを選択して「Install」をクリック

または、コマンドラインでインストール：

```bash
code --install-extension vscjava.vscode-java-pack
```

#### Spring Boot Extension Pack（必須）

Spring Boot開発に特化した拡張機能のセットです：

- Spring Boot Tools
- Spring Initializr Java Support
- Spring Boot Dashboard

**インストール方法**:

VSCodeの拡張機能タブで`Spring Boot Extension Pack`を検索してインストール。

または、コマンドラインで：

```bash
code --install-extension vmware.vscode-boot-dev-pack
```

---

### 3-3. VSCodeの推奨設定

#### settings.jsonの設定

VSCodeの設定ファイル（`settings.json`）に以下を追加すると、より快適に開発できます：

1. VSCodeで`Cmd+Shift+P`（macOS）または`Ctrl+Shift+P`（Windows/WSL2）を押す
2. `Preferences: Open User Settings (JSON)`を選択
3. 以下の設定を追加：

```json
{
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-21",
      "path": "/Library/Java/JavaVirtualMachines/openjdk-21.jdk/Contents/Home",
      "default": true
    }
  ],
  "java.jdt.ls.java.home": "/Library/Java/JavaVirtualMachines/openjdk-21.jdk/Contents/Home",
  "spring-boot.ls.java.home": "/Library/Java/JavaVirtualMachines/openjdk-21.jdk/Contents/Home",
  "files.exclude": {
    "**/.classpath": true,
    "**/.project": true,
    "**/.settings": true,
    "**/.factorypath": true
  }
}
```

> **💡 WSL2の場合**: `"path"`の値を`"/usr/lib/jvm/java-21-openjdk-amd64"`に変更してください。

---

### 3-4. VSCodeでJavaプロジェクトが認識されるか確認

#### テストプロジェクトの作成

VSCodeでSpring Initializrを使って簡単なプロジェクトを作成してみましょう：

1. `Cmd+Shift+P`（macOS）または`Ctrl+Shift+P`（Windows/WSL2）を押す
2. `Spring Initializr: Create a Maven Project`を選択
3. 以下を選択：
   - Spring Boot version: `3.5.8` (最新の3.5系)
   - Language: `Java`
   - Group Id: `com.example`
   - Artifact Id: `demo`
   - Packaging type: `Jar`
   - Java version: `21`
4. Dependencies: `Spring Web`を選択
5. プロジェクトの保存先を選択

#### プロジェクトを開く

VSCodeで作成したプロジェクトフォルダを開きます。

**左下に「Java Projects」ペインが表示されていればOK**です。

---

## ✅ ステップ4: 環境の最終確認

すべてのツールが正しくインストールされているか、最終確認を行います。

### 4-1. Javaの確認

```bash
java -version
javac -version
echo $JAVA_HOME
```

すべてのコマンドでJava 21が表示されることを確認してください。

---

### 4-2. Mavenの確認（オプション）

Mavenをインストールした場合：

```bash
mvn -version
```

Maven 3.9系が表示されることを確認してください。

---

### 4-3. VSCodeの確認

VSCodeを起動し、以下を確認します：

- [ ] 拡張機能タブで「Extension Pack for Java」がインストール済み
- [ ] 拡張機能タブで「Spring Boot Extension Pack」がインストール済み
- [ ] Java Projectsペインが表示される

---

### 4-4. Spring Bootプロジェクトのビルドと実行

先ほど作成したテストプロジェクト（`demo`）で動作確認を行います。

#### ターミナルでプロジェクトディレクトリに移動

```bash
cd /path/to/demo
```

#### ビルドの実行

```bash
./mvnw clean install
```

**期待される結果**:
```sh
[INFO] BUILD SUCCESS
```

> **💡 初回実行時**: 依存関係のダウンロードに時間がかかる場合があります（数分程度）。

#### Spring Bootアプリケーションの起動

```bash
./mvnw spring-boot:run
```

**期待される結果**:
```sh
...
2025-12-13T10:00:00.000+09:00  INFO 12345 --- [  restartedMain] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 8080 (http) with context path '/'
2025-12-13T10:00:00.000+09:00  INFO 12345 --- [  restartedMain] com.example.demo.DemoApplication         : Started DemoApplication in 2.5 seconds
```

「Started DemoApplication」と表示されれば成功です！

#### 動作確認

別のターミナルウィンドウを開いて、以下のコマンドを実行：

```bash
curl http://localhost:8080
```

**期待される結果**:
```json
{"timestamp":"2025-12-13T01:00:00.000+00:00","status":404,"error":"Not Found","path":"/"}
```

エラーページが表示されますが、これは正常です。サーバーが起動していて、HTTPリクエストに応答していることが確認できました。

#### アプリケーションの停止

`Ctrl+C`でアプリケーションを停止します。

---

## 🐛 トラブルシューティング

### エラー: `JAVA_HOME is not set`

**原因**: 環境変数`JAVA_HOME`が設定されていない

**解決策**:

#### macOSの場合:

```bash
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zshrc
source ~/.zshrc
```

#### WSL2の場合:

```bash
echo 'export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64' >> ~/.bashrc
source ~/.bashrc
```

---

### エラー: `./mvnw: Permission denied`

**原因**: `mvnw`スクリプトに実行権限がない

**解決策**:

```bash
chmod +x mvnw
```

---

### エラー: `Could not find or load main class`

**原因**: プロジェクトが正しくビルドされていない

**解決策**:

```bash
./mvnw clean install
```

ビルドが成功したら、再度実行してください。

---

### エラー: `Port 8080 is already in use`

**原因**: ポート8080が他のプロセスで使用されている

**解決策**:

#### macOSの場合:

ポート8080を使用しているプロセスを確認：

```bash
lsof -i :8080
```

表示されたプロセスIDを使って停止：

```bash
kill -9 <PID>
```

#### WSL2の場合:

```bash
sudo lsof -i :8080
sudo kill -9 <PID>
```

---

### エラー: `mvnw: No such file or directory`

**原因**: プロジェクトのルートディレクトリにいない

**解決策**:

プロジェクトのルートディレクトリ（`pom.xml`があるディレクトリ）に移動してから実行してください：

```bash
cd /path/to/your/project
ls -la mvnw  # mvnwファイルが存在することを確認
./mvnw spring-boot:run
```

---

### VSCodeでJavaプロジェクトが認識されない

**原因**: Java拡張機能が正しくインストールされていない、またはJavaランタイムが認識されていない

**解決策**:

1. VSCodeを再起動
2. `Cmd+Shift+P` → `Java: Clean Java Language Server Workspace`を実行
3. `settings.json`で`java.jdt.ls.java.home`が正しく設定されているか確認
4. VSCodeを再度再起動

---

### Homebrewでインストールしたパッケージが見つからない

**原因**: Homebrewのパスが環境変数に設定されていない（Apple Silicon Macの場合）

**解決策**:

```bash
echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zshrc
source ~/.zshrc
```

Intel Macの場合は`/usr/local/bin/brew`を使用します。

---

## 📚 環境構築で学んだこと

- ✅ Java 21 (OpenJDK) をインストールし、`JAVA_HOME`を設定した
- ✅ Maven Wrapper（`mvnw`）の役割と使い方を理解した
- ✅ VSCodeにJavaとSpring Boot開発に必要な拡張機能をインストールした
- ✅ Spring Bootプロジェクトのビルドと実行ができることを確認した
- ✅ 環境変数の設定方法（`.zshrc`、`.bashrc`）を理解した

---

## ➡️ 次のステップ

環境構築が完了しました！次は、Spring Bootで初めてのREST APIを作成しましょう。

[Step 1: Hello World REST API](STEP_1.md)へ進みましょう！

次のステップでは、以下を学びます：

- Spring Bootプロジェクトの作成
- `@RestController`を使った簡単なREST API
- アプリケーションの起動とcurlでの動作確認

---

## 💡 補足: よくある質問

### Q1: Java 21ではなくJava 17を使っても良いですか？

**A**: Spring Boot 3.5.8はJava 17もサポートしていますが、このカリキュラムではJava 21の機能（レコード型、パターンマッチングなど）を活用するため、Java 21を推奨します。

---

### Q2: IntelliJ IDEAやEclipseを使っても良いですか？

**A**: 使用可能ですが、このカリキュラムはVSCodeを前提に記載されています。他のIDEを使う場合、一部の手順が異なる場合があります。

---

### Q3: Dockerを使ってJava環境を構築できますか？

**A**: 可能ですが、Phase 2以降でMySQLをDockerで構築する際に混乱を避けるため、ローカルにJavaをインストールすることを推奨します。

---

### Q4: mvnwとmvnの違いは何ですか？

**A**:

| 項目 | `mvn` | `./mvnw` |
|------|-------|----------|
| インストール | システムにMavenをインストール必要 | プロジェクトに含まれる |
| バージョン管理 | システムのMavenバージョンに依存 | プロジェクトごとに指定可能 |
| チーム開発 | メンバー間でバージョンの統一が必要 | 自動的に統一される |

---

### Q5: WSL2でファイルの保存場所はどこが推奨ですか？

**A**: WSL2のホームディレクトリ（`~/`）配下をおすすめします。Windowsのファイルシステム（`/mnt/c/`）ではファイルI/Oが遅くなる場合があります。

**推奨**:
```bash
~/projects/spring-boot-curriculum/
```

**非推奨**:
```bash
/mnt/c/Users/YourName/Documents/spring-boot-curriculum/
```

---

### Q6: macOSでIntel MacとApple Silicon（M1/M2/M3）の違いは？

**A**: 主な違いはHomebrewのインストールパスです：

| Mac種類 | Homebrewパス | JDKパス（例） |
|---------|-------------|--------------|
| Intel Mac | `/usr/local/` | `/usr/local/opt/openjdk@21/` |
| Apple Silicon | `/opt/homebrew/` | `/opt/homebrew/opt/openjdk@21/` |

このガイドではApple Siliconを基準に記載していますが、Intel Macの場合はパスを読み替えてください。

---

### Q7: Java 21で追加された新機能は使いますか？

**A**: はい、カリキュラムの後半（Phase 4以降）で以下を活用します：

- **レコード型（Records）**: DTOの簡潔な定義
- **パターンマッチング**: `instanceof`の改善
- **シーケンスコレクション**: Listの新しいメソッド

---

**最終更新**: 2025-12-13  
**対象バージョン**: Spring Boot 3.5.8
