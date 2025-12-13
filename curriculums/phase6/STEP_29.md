# Step 29: テストカバレッジ

## 🎯 このステップの目標

- JaCoCoでテストカバレッジを測定できる
- カバレッジレポートを読み解ける
- カバレッジの目標値を設定できる
- 未テストのコードを特定して改善できる

**所要時間**: 約50分

---

## 📋 事前準備

- [Step 28: 統合テスト](STEP_28.md)が完了していること

---

## 📊 テストカバレッジとは

### カバレッジの種類

| カバレッジ | 説明 | 例 |
|---|---|---|
| **行カバレッジ** | 実行された行の割合 | 100行中80行 = 80% |
| **分岐カバレッジ** | 実行された分岐の割合 | if文の両パターン |
| **メソッドカバレッジ** | 実行されたメソッドの割合 | 10メソッド中8メソッド = 80% |

### なぜカバレッジが重要か

**Before（カバレッジなし）**:
```java
public class UserService {
    public User findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID is null");  // ❌ テストされていない
        }
        return userRepository.findById(id).orElse(null);
    }
}
```

**After（カバレッジあり）**:
```java
// カバレッジレポートで「id == null」が未テストと判明
// → テストを追加

@Test
void findById_WithNullId_ThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> {
        userService.findById(null);
    });
}
```

**メリット**:
- 🎯 テストの抜け漏れを発見
- 📈 テスト品質の可視化
- 🔒 リファクタリング時の安全性向上

---

## 🚀 ステップ1: JaCoCoの設定

### 1-1. JaCoCoプラグインを追加

`pom.xml`の`<build><plugins>`に追加：

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <!-- テスト実行前に準備 -->
        <execution>
            <id>prepare-agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        
        <!-- テスト実行後にレポート生成 -->
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        
        <!-- カバレッジチェック -->
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>PACKAGE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>  <!-- 80%以上 -->
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 1-2. コードの解説

#### `prepare-agent`
```xml
<goal>prepare-agent</goal>
```
- テスト実行前にJavaエージェントを準備
- テスト実行時のコード実行を記録

#### `report`
```xml
<goal>report</goal>
```
- テスト後にHTMLレポートを生成
- `target/site/jacoco/index.html`に出力

#### `check`
```xml
<goal>check</goal>
```
- カバレッジが基準を満たしているか確認
- 基準未達ならビルド失敗

---

## 🚀 ステップ2: カバレッジレポートの生成

### 2-1. テストを実行してレポート生成

```bash
./mvnw clean test
```

### 2-2. レポートを確認

```bash
# macOS/Linux
open target/site/jacoco/index.html

# WSL2
explorer.exe target/site/jacoco/index.html
```

### 2-3. レポートの見方

#### 全体サマリー

```
Element         Missed Instructions    Cov.    Missed Branches    Cov.    Missed    Cxty    Missed    Lines    Missed    Methods    Missed    Classes
com.example     120 of 600             80%     10 of 50           80%     5         25      60        300      3         15         1         10
```

**重要な指標**:
- **Instructions**: バイトコード命令のカバレッジ
- **Branches**: 分岐（if, switch）のカバレッジ
- **Lines**: 行のカバレッジ
- **Methods**: メソッドのカバレッジ

#### 色分け

- 🟢 **緑**: カバーされている
- 🟡 **黄**: 部分的にカバー（分岐の一部のみ）
- 🔴 **赤**: カバーされていない

---

## 🚀 ステップ3: 未テストコードの改善

### 3-1. カバレッジが低いクラスを特定

レポートで`UserService`のカバレッジを確認：

```
UserService
Lines: 15/20 (75%)
Branches: 6/10 (60%)
```

### 3-2. 未カバーの行を確認

レポートの`UserService.java`をクリック：

```java
public User findById(Long id) {
    if (id == null) {  // 🔴 赤（未テスト）
        throw new IllegalArgumentException("ID is null");  // 🔴 赤
    }
    
    return userRepository.findById(id)  // 🟢 緑（テスト済み）
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));  // 🟡 黄（部分カバー）
}
```

### 3-3. テストを追加

`UserServiceTest.java`に追加：

```java
@Test
@DisplayName("nullのIDで例外がスローされること")
void findById_WithNullId_ThrowsException() {
    // Act & Assert
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> userService.findById(null)
    );
    
    assertThat(exception.getMessage()).isEqualTo("ID is null");
}

@Test
@DisplayName("存在しないIDで例外がスローされること")
void findById_NotFound_ThrowsException() {
    // Arrange
    when(userRepository.findById(999L)).thenReturn(Optional.empty());
    
    // Act & Assert
    assertThrows(ResourceNotFoundException.class, () -> {
        userService.findById(999L);
    });
    
    verify(userRepository).findById(999L);
}
```

### 3-4. 再度レポート生成

```bash
./mvnw clean test
open target/site/jacoco/index.html
```

**結果**:
```
UserService
Lines: 20/20 (100%) ✅
Branches: 10/10 (100%) ✅
```

---

## 🚀 ステップ4: カバレッジ基準の設定

### 4-1. プロジェクト全体の基準

`pom.xml`のJaCoCoプラグインに設定：

```xml
<execution>
    <id>check</id>
    <goals>
        <goal>check</goal>
    </goals>
    <configuration>
        <rules>
            <!-- パッケージレベルの基準 -->
            <rule>
                <element>PACKAGE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>  <!-- 行カバレッジ80%以上 -->
                    </limit>
                    <limit>
                        <counter>BRANCH</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.70</minimum>  <!-- 分岐カバレッジ70%以上 -->
                    </limit>
                </limits>
            </rule>
            
            <!-- クラスレベルの基準 -->
            <rule>
                <element>CLASS</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.75</minimum>  <!-- クラスごとに75%以上 -->
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</execution>
```

### 4-2. 除外設定

テスト不要なクラスを除外：

```xml
<configuration>
    <excludes>
        <!-- DTOを除外 -->
        <exclude>**/dto/**</exclude>
        
        <!-- エンティティを除外 -->
        <exclude>**/entities/**</exclude>
        
        <!-- メインクラスを除外 -->
        <exclude>**/*Application.class</exclude>
        
        <!-- 設定クラスを除外 -->
        <exclude>**/config/**</exclude>
    </excludes>
</configuration>
```

### 4-3. カバレッジチェック実行

```bash
# テスト + カバレッジチェック
./mvnw clean verify
```

**カバレッジ不足の場合**:
```
[ERROR] Rule violated for package com.example.hellospringboot.services:
lines covered ratio is 0.65, but expected minimum is 0.80
```

---

## ✅ 動作確認

### 1. カバレッジレポート生成

```bash
./mvnw clean test
```

**期待される結果**:
```
[INFO] --- jacoco-maven-plugin:0.8.11:report (report) @ hello-spring-boot ---
[INFO] Loading execution data file target/jacoco.exec
[INFO] Analyzed bundle 'hello-spring-boot' with 15 classes
```

### 2. レポート確認

```bash
open target/site/jacoco/index.html
```

### 3. カバレッジチェック

```bash
./mvnw verify
```

**成功時**:
```
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: 100%カバレッジを達成

**目標**: `UserService`の行カバレッジを100%にする

**ヒント**:
1. レポートで赤い行を確認
2. 未カバーの分岐をテスト
3. 例外ケースをテスト

### チャレンジ 2: カスタムレポートの作成

**目標**: カバレッジレポートをXML形式で出力

**ヒント**:
```xml
<execution>
    <id>report-xml</id>
    <phase>test</phase>
    <goals>
        <goal>report</goal>
    </goals>
    <configuration>
        <formats>
            <format>XML</format>
        </formats>
    </configuration>
</execution>
```

### チャレンジ 3: CIでカバレッジチェック

**目標**: GitHub Actionsでカバレッジチェックを自動化

**ヒント**:
```yaml
# .github/workflows/test.yml
name: Test with Coverage
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
      - name: Run tests with coverage
        run: ./mvnw verify
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
```

---

## 🐛 トラブルシューティング

### カバレッジが0%になる

**原因**: `prepare-agent`が実行されていない

**解決策**:
```bash
# cleanから実行
./mvnw clean test
```

### レポートが生成されない

**原因**: `report`ゴールが実行されていない

**解決策**:
```bash
# 明示的にreportゴールを実行
./mvnw jacoco:report
```

### カバレッジチェックが厳しすぎる

**問題**: DTOやEntityもカバレッジ計算に含まれる

**解決策**: 除外設定を追加
```xml
<configuration>
    <excludes>
        <exclude>**/dto/**</exclude>
        <exclude>**/entities/**</exclude>
    </excludes>
</configuration>
```

### Lombokのコードがカバレッジに含まれる

**問題**: Lombokの自動生成コードがカバレッジを下げる

**解決策**: Lombok生成コードを除外
```xml
<configuration>
    <excludes>
        <exclude>**/*_*</exclude>  <!-- Lombok生成クラス -->
    </excludes>
</configuration>
```

---

## 📚 このステップで学んだこと

- ✅ JaCoCoプラグインの設定
- ✅ カバレッジレポートの生成
- ✅ HTMLレポートの読み方
- ✅ 未カバーコードの特定と改善
- ✅ カバレッジ基準の設定（`<limits>`）
- ✅ 除外設定で精度向上
- ✅ `mvn verify`でチェック実行

---

## 💡 補足: カバレッジのベストプラクティス

### 適切なカバレッジ目標

| プロジェクト種類 | 推奨カバレッジ |
|---|---|
| ビジネスロジック（Service層） | 90%以上 |
| コントローラー（Controller層） | 80%以上 |
| リポジトリ（Repository層） | 70%以上 |
| DTO・Entity | 除外（テスト不要） |

### カバレッジ100%を目指すべきか？

**❌ 100%にこだわりすぎない**:
- Getterのテストは不要
- 自動生成コードのテストは不要
- トライアンドエラーのコードは除外

**⭐ 意味のあるカバレッジを追求**:
- ビジネスロジックの分岐を網羅
- 例外処理をテスト
- エッジケースをカバー

### カバレッジが低くても許容されるケース

1. **設定クラス**: `@Configuration`クラス
2. **DTOクラス**: Getterのみのシンプルなクラス
3. **メインクラス**: `public static void main()`
4. **Lombokクラス**: 自動生成されたコード

---

## 🎓 Phase 6のまとめ

### Phase 6で学んだこと

#### Step 25: Spring Securityの基礎
- 認証と認可の違い
- Basic認証の実装
- インメモリユーザー管理
- ロールベースのアクセス制御

#### Step 26: JWTトークン認証
- JWTの構造（Header.Payload.Signature）
- トークン生成と検証
- ステートレス認証
- Bearer認証

#### Step 27: ユニットテスト
- JUnit 5の基礎
- Mockitoでモック化
- AAA（Arrange-Act-Assert）パターン
- AssertJで流暢な検証

#### Step 28: 統合テスト
- `@SpringBootTest`で統合テスト
- `MockMvc`でHTTPテスト
- `@DataJpaTest`でリポジトリテスト
- TestContainersで実際のDB

#### Step 29: テストカバレッジ（今回）
- JaCoCoの設定
- カバレッジレポートの読み方
- カバレッジ基準の設定
- 未テストコードの改善

### 次のPhaseへの準備

Phase 7では、実践的な機能を実装します：
- ファイルアップロード
- ページネーション
- キャッシュ
- 非同期処理

セキュリティとテストの基礎ができたので、より高度な機能に挑戦できます！

---

## ➡️ 次のステップ

[Phase 7: 実践的な機能](../../curriculums/phase7/STEP_30.md)へ進みましょう！

Phase 7では、実際のアプリケーションでよく使われる機能を実装します。ファイルアップロード、ページネーション、キャッシュ、非同期処理など、実務で役立つテクニックを学びます。

---

**Phase 6完了おめでとうございます！🎉**

セキュリティとテストの基礎をマスターしました。これで安全で品質の高いアプリケーションを開発できます！
