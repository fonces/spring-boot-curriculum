# Phase 2 事前準備: Docker環境のセットアップ

## 🎯 この準備の目標

Phase 2では、データベース（H2、MySQL）を使ったアプリケーション開発を学びます。
特にStep 12でMySQLに切り替える際に、Dockerを使用します。

このガイドでは、以下の環境をセットアップします：

- ✅ Docker Desktopのインストール
- ✅ Dockerの動作確認
- ✅ Docker Composeの理解

**所要時間**: 約30分

---

## 📋 必要な環境

### Phase 1の完了

Phase 2を始める前に、Phase 1（Step 1〜5）を完了していることを確認してください。

- [x] Spring Bootプロジェクトの作成ができる
- [x] REST APIの実装ができる
- [x] DTOとLombokを使える

### 開発環境

Phase 1で構築した環境：
- OpenJDK 21
- Maven 3.8+
- IntelliJ IDEA Community Edition

---

## 🐳 Docker Desktopのインストール

### Dockerとは？

**Docker** は、アプリケーションとその実行環境をコンテナとして管理するプラットフォームです。

**メリット**:
- 環境構築が簡単（MySQLを数秒で起動）
- 環境の再現性が高い
- ローカル環境を汚さない

### インストール手順

#### Windows の場合

1. **システム要件の確認**
   - Windows 10 64bit: Pro, Enterprise, Education (Build 16299以降)
   - または Windows 11
   - WSL 2が有効になっていること

2. **Docker Desktopのダウンロード**
   - https://www.docker.com/products/docker-desktop にアクセス
   - 「Download for Windows」をクリック

3. **インストール**
   - ダウンロードした`Docker Desktop Installer.exe`を実行
   - 「Use WSL 2 instead of Hyper-V」にチェックを入れる
   - インストール完了後、PCを再起動

4. **Docker Desktopの起動**
   - Windowsメニューから「Docker Desktop」を起動
   - 利用規約に同意
   - サインインはスキップ可能（オプション）

#### macOS の場合

1. **チップの確認**
   - Apple Menu > About This Mac
   - 「Chip」を確認（Intel または Apple Silicon）

2. **Docker Desktopのダウンロード**
   - https://www.docker.com/products/docker-desktop にアクセス
   - 自分のチップに合ったバージョンをダウンロード
     - Intel Chip → Docker Desktop for Mac (Intel)
     - Apple Silicon → Docker Desktop for Mac (Apple Silicon)

3. **インストール**
   - ダウンロードした`.dmg`ファイルを開く
   - Docker.appをApplicationsフォルダにドラッグ
   - Applicationsフォルダから「Docker」を起動

4. **初回起動**
   - 権限の許可を求められたら「OK」
   - サインインはスキップ可能

#### Linux の場合

Linuxディストリビューションによって手順が異なります。

**Ubuntu/Debianの例**:

```bash
# 古いバージョンの削除
sudo apt-get remove docker docker-engine docker.io containerd runc

# 依存関係のインストール
sudo apt-get update
sudo apt-get install \
    ca-certificates \
    curl \
    gnupg \
    lsb-release

# Docker公式GPGキーの追加
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# リポジトリの設定
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Dockerのインストール
sudo apt-get update
sudo apt-get install docker-ce docker-ce-cli containerd.io docker-compose-plugin

# 現在のユーザーをdockerグループに追加（sudoなしで実行するため）
sudo usermod -aG docker $USER

# ログアウトして再ログイン（またはPC再起動）
```

詳細は公式ドキュメントを参照：
- Ubuntu: https://docs.docker.com/engine/install/ubuntu/
- CentOS: https://docs.docker.com/engine/install/centos/

---

## ✅ Dockerの動作確認

### 1. バージョン確認

ターミナル（コマンドプロンプト）を開いて以下を実行：

```bash
docker --version
```

**期待される出力**:
```
Docker version 24.0.x, build xxxxxxx
```

```bash
docker compose version
```

**期待される出力**:
```
Docker Compose version v2.20.x
```

> **注意**: 古いバージョンでは`docker-compose`（ハイフン付き）でしたが、
> 現在は`docker compose`（スペース区切り）が推奨です。

### 2. Hello Worldコンテナの実行

Dockerが正しく動作するか確認します：

```bash
docker run hello-world
```

**期待される出力**:
```
Unable to find image 'hello-world:latest' locally
latest: Pulling from library/hello-world
...
Hello from Docker!
This message shows that your installation appears to be working correctly.
...
```

このメッセージが表示されればDocker is ready!

### 3. Docker Desktopの確認

Docker Desktop（GUIアプリ）を開いて：
- 左下の「Engine running」が緑色になっていることを確認
- Containersタブで実行中/停止中のコンテナを確認できます

---

## 🚀 MySQLコンテナを試してみる（オプション）

Phase 2のStep 12で使用するMySQLを事前に試してみましょう。

### 1. MySQLコンテナの起動

```bash
docker run --name test-mysql \
  -e MYSQL_ROOT_PASSWORD=password \
  -e MYSQL_DATABASE=testdb \
  -p 3306:3306 \
  -d mysql:8.0
```

**解説**:
- `--name test-mysql`: コンテナに名前を付ける
- `-e MYSQL_ROOT_PASSWORD=password`: rootパスワードを設定
- `-e MYSQL_DATABASE=testdb`: 初期データベースを作成
- `-p 3306:3306`: ホストの3306ポートをコンテナの3306ポートにマッピング
- `-d`: バックグラウンドで実行
- `mysql:8.0`: MySQL 8.0のイメージを使用

### 2. コンテナの確認

```bash
docker ps
```

**期待される出力**:
```
CONTAINER ID   IMAGE       COMMAND                  CREATED          STATUS          PORTS                               NAMES
abc123def456   mysql:8.0   "docker-entrypoint.s…"   10 seconds ago   Up 9 seconds    0.0.0.0:3306->3306/tcp, 33060/tcp   test-mysql
```

### 3. MySQLに接続（オプション）

```bash
docker exec -it test-mysql mysql -uroot -ppassword
```

MySQLプロンプトが表示されたら成功です：

```sql
mysql> SHOW DATABASES;
+--------------------+
| Database           |
+--------------------+
| information_schema |
| mysql              |
| performance_schema |
| sys                |
| testdb             |
+--------------------+
5 rows in set (0.00 sec)

mysql> exit
```

### 4. コンテナの停止と削除

テストが終わったら、コンテナを停止・削除します：

```bash
# 停止
docker stop test-mysql

# 削除
docker rm test-mysql

# イメージも削除する場合（オプション）
docker rmi mysql:8.0
```

---

## 📚 Docker Composeとは？

**Docker Compose** は、複数のコンテナを定義・管理するツールです。

### 通常のdocker runコマンド

```bash
docker run --name mysql \
  -e MYSQL_ROOT_PASSWORD=password \
  -e MYSQL_DATABASE=mydb \
  -p 3306:3306 \
  -d mysql:8.0
```

長くて覚えにくい...

### Docker Composeを使う場合

`docker-compose.yml`ファイルに設定を記述：

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: password
      MYSQL_DATABASE: mydb
    ports:
      - "3306:3306"
```

起動は簡単：

```bash
docker compose up -d
```

**メリット**:
- 設定をファイルで管理（Git管理可能）
- 複数コンテナを一括管理
- チーム全員が同じ環境を構築できる

**Phase 2のStep 12で実際に使用します！**

---

## 🎨 Docker基本コマンド早見表

Phase 2で使うコマンドをまとめました：

### イメージ関連

```bash
# イメージ一覧
docker images

# イメージのダウンロード
docker pull mysql:8.0

# イメージの削除
docker rmi mysql:8.0
```

### コンテナ関連

```bash
# 実行中のコンテナ一覧
docker ps

# すべてのコンテナ一覧（停止中も含む）
docker ps -a

# コンテナの起動
docker start <コンテナ名>

# コンテナの停止
docker stop <コンテナ名>

# コンテナの削除
docker rm <コンテナ名>

# コンテナのログ表示
docker logs <コンテナ名>

# コンテナ内でコマンド実行
docker exec -it <コンテナ名> bash
```

### Docker Compose関連

```bash
# コンテナの起動（バックグラウンド）
docker compose up -d

# コンテナの停止
docker compose down

# ログ表示
docker compose logs

# コンテナの状態確認
docker compose ps
```

---

## 🐛 トラブルシューティング

### Docker Desktopが起動しない（Windows）

**原因**: WSL 2が有効になっていない

**解決策**:
1. PowerShellを管理者として実行
2. 以下を実行：
   ```powershell
   wsl --install
   ```
3. PCを再起動

### "Cannot connect to the Docker daemon"

**原因**: Docker Desktopが起動していない

**解決策**:
- Docker Desktopアプリを起動
- 左下が「Engine running」になるまで待つ

### ポート3306が既に使用されている

**エラー**: `Bind for 0.0.0.0:3306 failed: port is already allocated`

**原因**: 既にMySQLがローカルで動いている

**解決策1**: ローカルのMySQLを停止

```bash
# Windowsの場合
net stop MySQL80

# macOSの場合
brew services stop mysql

# Linuxの場合
sudo systemctl stop mysql
```

**解決策2**: 異なるポートを使用

```yaml
ports:
  - "3307:3306"  # ホスト側を3307に変更
```

### Dockerコンテナが起動しない

**確認方法**:

```bash
# ログを確認
docker logs <コンテナ名>
```

よくある原因：
- メモリ不足（Docker Desktopの設定でメモリを増やす）
- ディスク容量不足
- イメージの破損（イメージを削除して再ダウンロード）

---

## ✅ 準備完了チェックリスト

Phase 2を始める前に、以下を確認してください：

- [ ] Docker Desktopがインストールされている
- [ ] `docker --version`でバージョンが表示される
- [ ] `docker compose version`でバージョンが表示される
- [ ] `docker run hello-world`が成功する
- [ ] Docker Desktopで「Engine running」が緑色
- [ ] Phase 1（Step 1〜5）が完了している

---

## 📖 参考リソース

### 公式ドキュメント

- Docker Desktop: https://docs.docker.com/desktop/
- Docker Compose: https://docs.docker.com/compose/
- MySQL Docker Image: https://hub.docker.com/_/mysql

### 学習リソース

- Docker公式チュートリアル: https://docs.docker.com/get-started/
- Docker Compose入門: https://docs.docker.com/compose/gettingstarted/

---

## 💡 補足: なぜDockerを使うのか？

### 従来の方法（ローカルにMySQLインストール）

**デメリット**:
- インストールが面倒
- バージョン管理が難しい
- 環境の再現が困難
- アンインストールが面倒
- 複数バージョンの共存が難しい

### Dockerを使う方法

**メリット**:
- ✅ 数秒で起動・停止
- ✅ 環境を汚さない（削除も簡単）
- ✅ チーム全員が同じ環境
- ✅ バージョンの切り替えが簡単
- ✅ 設定をコードで管理（docker-compose.yml）

**実務でもDockerは標準的に使われています！**

---

## ➡️ 次のステップ

Docker環境の準備が整ったら、Phase 2のStep 6へ進みましょう！

[Step 6: H2データベース導入](STEP_6.md)

まずはインメモリデータベース（H2）から始めて、
データベースの基本を学んでから、Step 12でMySQLに切り替えます。

---

お疲れさまでした！ 🐳

Docker環境が整ったら、いよいよデータベースを使った開発に入ります。
Phase 2で実践的なアプリケーション開発のスキルを身につけましょう！
