# Step 29: テストカバレッジ

## 🎯 このステップの目標

- テストカバレッジの概念と重要性を理解する
- JaCoCoを使ってコードカバレッジを測定できる
- カバレッジレポートを読み解ける
- 目標カバレッジ（80%以上）を達成できる
- カバレッジを継続的に監視できる

**所要時間**: 約1時間

---

## 📋 事前準備

このステップを始める前に、以下を確認してください：

- Step 28（統合テスト）が完了していること
- ユニットテストと統合テストを実装していること
- JUnit 5とMockitoの使い方を理解していること

---

## 📝 概要

テストカバレッジ（コードカバレッジ）は、テストコードによって実行されたプロダクションコードの割合を示す指標です。カバレッジが高いほど、テストされていないコードが少なく、バグが潜んでいる可能性が低くなります。

## 💡 テストカバレッジとは

### カバレッジの種類

| 種類 | 説明 | 測定単位 |
|---|---|---|
| **Line Coverage（行カバレッジ）** | 実行された行の割合 | 最も一般的 |
| **Branch Coverage（分岐カバレッジ）** | if文などの分岐の割合 | 条件分岐の網羅性 |
| **Method Coverage（メソッドカバレッジ）** | 実行されたメソッドの割合 | メソッド単位 |
| **Class Coverage（クラスカバレッジ）** | 実行されたクラスの割合 | クラス単位 |

### なぜカバレッジが重要か

**カバレッジが低い場合の問題**:
- ❌ テストされていないコードにバグが潜む
- ❌ リファクタリング時の安心感がない
- ❌ 仕様変更の影響範囲が見えない

**カバレッジが高い場合のメリット**:
- ✅ バグの早期発見
- ✅ リファクタリングの安全性向上
- ✅ コードの品質が可視化される

### 目標カバレッジ

| プロジェクト種類 | 推奨カバレッジ |
|---|---|
| **エンタープライズアプリ** | 80%以上 |
| **ライブラリ/フレームワーク** | 90%以上 |
| **プロトタイプ** | 50%〜70% |

**注意**: カバレッジ100%を目指す必要はありません。重要なのは**質の高いテスト**を書くことです。

---

## 🚀 ステップ1: JaCoCoのセットアップ

### 1-1. pom.xmlに依存関係を追加

```xml
<project>
    <!-- 既存の設定 -->
    
    <build>
        <plugins>
            <!-- 既存のプラグイン -->
            
            <!-- JaCoCo Maven Plugin -->
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>0.8.11</version>
                <executions>
                    <!-- テスト実行前にJaCoCoエージェントを準備 -->
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
                    <!-- カバレッジ目標チェック -->
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
                                            <minimum>0.80</minimum>
                                        </limit>
                                    </limits>
                                </rule>
                            </rules>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

### 1-2. 設定の説明

- **prepare-agent**: テスト実行時にJaCoCoエージェントを起動
- **report**: テスト後にHTMLレポートを生成
- **check**: 最小カバレッジをチェック（80%未満でビルド失敗）

---

## 🚀 ステップ2: 既存のテストコードの確認

カバレッジを測定する前に、既存のテストを確認しましょう。

### 2-1. ユニットテストの例（Service層）

```java
package com.example.hellospringboot.service;

import com.example.hellospringboot.dto.request.UserCreateRequest;
import com.example.hellospringboot.dto.response.UserResponse;
import com.example.hellospringboot.entity.User;
import com.example.hellospringboot.exception.ResourceNotFoundException;
import com.example.hellospringboot.mapper.UserMapper;
import com.example.hellospringboot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private UserMapper userMapper;
    
    @InjectMocks
    private UserService userService;
    
    private User testUser;
    private UserCreateRequest createRequest;
    private UserResponse userResponse;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setAge(25);
        
        createRequest = new UserCreateRequest();
        createRequest.setName("New User");
        createRequest.setEmail("new@example.com");
        createRequest.setAge(30);
        
        userResponse = UserResponse.builder()
            .id(1L)
            .name("Test User")
            .email("test@example.com")
            .age(25)
            .build();
    }
    
    @Test
    @DisplayName("全ユーザー取得 - 成功")
    void findAll_Success() {
        // Given
        List<User> users = Arrays.asList(testUser);
        when(userRepository.findAll()).thenReturn(users);
        when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);
        
        // When
        List<UserResponse> result = userService.findAll();
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test User", result.get(0).getName());
        verify(userRepository, times(1)).findAll();
    }
    
    @Test
    @DisplayName("ID指定でユーザー取得 - 成功")
    void findById_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userMapper.toResponse(testUser)).thenReturn(userResponse);
        
        // When
        UserResponse result = userService.findById(1L);
        
        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test User", result.getName());
    }
    
    @Test
    @DisplayName("ID指定でユーザー取得 - ユーザーが存在しない場合")
    void findById_NotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.findById(999L);
        });
    }
    
    @Test
    @DisplayName("ユーザー作成 - 成功")
    void create_Success() {
        // Given
        when(userMapper.toEntity(createRequest)).thenReturn(testUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(userResponse);
        
        // When
        UserResponse result = userService.create(createRequest);
        
        // Then
        assertNotNull(result);
        verify(userRepository, times(1)).save(any(User.class));
    }
    
    @Test
    @DisplayName("ユーザー削除 - 成功")
    void delete_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).delete(testUser);
        
        // When
        assertDoesNotThrow(() -> userService.delete(1L));
        
        // Then
        verify(userRepository, times(1)).delete(testUser);
    }
}
```

### 2-2. 統合テストの例（Controller層）

```java
package com.example.hellospringboot.controller;

import com.example.hellospringboot.dto.request.UserCreateRequest;
import com.example.hellospringboot.dto.response.UserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @DisplayName("全ユーザー取得 - 統合テスト")
    @Sql("/test-data.sql")
    void getAll_Success() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(greaterThan(0))))
            .andExpect(jsonPath("$[0].name").exists());
    }
    
    @Test
    @DisplayName("ユーザー作成 - 統合テスト")
    void create_Success() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setName("Integration Test User");
        request.setEmail("integration@example.com");
        request.setAge(28);
        
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Integration Test User"))
            .andExpect(jsonPath("$.email").value("integration@example.com"));
    }
    
    @Test
    @DisplayName("バリデーションエラー - 統合テスト")
    void create_ValidationError() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setName(""); // 空の名前
        request.setEmail("invalid-email"); // 不正なメール
        request.setAge(10); // 18歳未満
        
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").exists());
    }
}
```

---

## 🚀 ステップ3: カバレッジレポートの生成

### 3-1. テストを実行してレポート生成

```bash
# テスト実行 + カバレッジレポート生成
./mvnw clean test

# または
./mvnw clean verify
```

**出力例**:
```
[INFO] --- jacoco-maven-plugin:0.8.11:report (report) @ demo ---
[INFO] Loading execution data file target/jacoco.exec
[INFO] Analyzed bundle 'demo' with 15 classes
```

### 3-2. レポートの確認

レポートは以下のディレクトリに生成されます：

```
target/site/jacoco/
├── index.html              # カバレッジサマリー
├── com.example.hellospringboot/       # パッケージごとのレポート
│   ├── controller/
│   ├── service/
│   └── repository/
└── jacoco.xml              # CI/CD用XMLレポート
```

**ブラウザで確認**:
```bash
# macOS
open target/site/jacoco/index.html

# Linux
xdg-open target/site/jacoco/index.html

# Windows
start target/site/jacoco/index.html
```

---

## 🚀 ステップ4: カバレッジレポートの読み方

### 4-1. サマリー画面

```
┌─────────────────────────────────────────────────────────┐
│ Element         Instructions  Branches  Lines  Methods  │
├─────────────────────────────────────────────────────────┤
│ Total           85% (425/500) 70% (14/20) 82% (82/100)  │
│ com.example.hellospringboot.controller  90%  75%    88%    90%     │
│ com.example.hellospringboot.service     95%  80%    92%    100%    │
│ com.example.hellospringboot.repository  100% N/A    100%   100%    │
└─────────────────────────────────────────────────────────┘
```

### 4-2. カバレッジの色分け

- 🟢 **緑**: カバーされている（実行された）
- 🟡 **黄色**: 部分的にカバーされている（分岐の一部のみ）
- 🔴 **赤**: カバーされていない（未実行）

### 4-3. ファイル別カバレッジの確認

```java
public class UserService {
    
    public UserResponse findById(Long id) {  // 🟢 カバー済み
        return userRepository.findById(id)
            .map(userMapper::toResponse)     // 🟢 カバー済み
            .orElseThrow(() ->               // 🟢 カバー済み
                new ResourceNotFoundException("User", "id", id)
            );
    }
    
    public void deleteInactive() {           // 🔴 未カバー
        // このメソッドはテストされていない
        userRepository.deleteByActiveIsFalse();
    }
}
```

---

## 🚀 ステップ5: カバレッジを改善する

### 5-1. カバーされていないコードの特定

```bash
# カバレッジチェックを実行
./mvnw clean verify
```

**カバレッジ不足の例**:
```
[WARNING] Rule violated for package com.example.hellospringboot.service:
lines covered ratio is 0.65, but expected minimum is 0.80
```

### 5-2. 不足しているテストを追加

```java
@Test
@DisplayName("非アクティブユーザーの削除 - 成功")
void deleteInactive_Success() {
    // Given
    doNothing().when(userRepository).deleteByActiveIsFalse();
    
    // When
    assertDoesNotThrow(() -> userService.deleteInactive());
    
    // Then
    verify(userRepository, times(1)).deleteByActiveIsFalse();
}
```

### 5-3. エッジケースのテスト

```java
@Test
@DisplayName("年齢がnullの場合のユーザー作成")
void create_WithNullAge() {
    // Given
    UserCreateRequest request = new UserCreateRequest();
    request.setName("Test User");
    request.setEmail("test@example.com");
    request.setAge(null);  // nullケース
    
    // When & Then
    // バリデーションエラーになることを確認
}

@Test
@DisplayName("重複メールでのユーザー作成")
void create_DuplicateEmail() {
    // Given
    when(userRepository.existsByEmail("duplicate@example.com"))
        .thenReturn(true);
    
    // When & Then
    assertThrows(BusinessException.class, () -> {
        // 重複エラーが発生することを確認
    });
}
```

---

## 🚀 ステップ6: カバレッジの継続的監視

### 6-1. ビルド時に自動チェック

`pom.xml`の設定により、カバレッジが80%未満の場合はビルドが失敗します：

```bash
./mvnw clean verify
```

**カバレッジ不足時の出力**:
```
[ERROR] Failed to execute goal org.jacoco:jacoco-maven-plugin:0.8.11:check
[ERROR] Rule violated for package com.example.hellospringboot: 
        lines covered ratio is 0.75, but expected minimum is 0.80
```

### 6-2. 特定のクラスを除外

設定クラスやDTOなど、テスト不要なクラスを除外：

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <configuration>
        <excludes>
            <!-- 設定クラス -->
            <exclude>**/config/**</exclude>
            <!-- DTO -->
            <exclude>**/dto/**</exclude>
            <!-- メインクラス -->
            <exclude>**/DemoApplication.class</exclude>
        </excludes>
    </configuration>
    <!-- executions は同じ -->
</plugin>
```

### 6-3. パッケージ別の目標設定

```xml
<rules>
    <!-- Service層は90%以上 -->
    <rule>
        <element>PACKAGE</element>
        <includes>
            <include>com.example.hellospringboot.service.*</include>
        </includes>
        <limits>
            <limit>
                <counter>LINE</counter>
                <value>COVEREDRATIO</value>
                <minimum>0.90</minimum>
            </limit>
        </limits>
    </rule>
    <!-- Controller層は80%以上 -->
    <rule>
        <element>PACKAGE</element>
        <includes>
            <include>com.example.hellospringboot.controller.*</include>
        </includes>
        <limits>
            <limit>
                <counter>LINE</counter>
                <value>COVEREDRATIO</value>
                <minimum>0.80</minimum>
            </limit>
        </limits>
    </rule>
</rules>
```

---

## ✅ 動作確認

### 1. カバレッジレポートの生成

```bash
./mvnw clean test
```

### 2. レポートをブラウザで確認

```bash
open target/site/jacoco/index.html
```

**確認項目**:
- 全体のカバレッジが80%以上か
- 各パッケージのカバレッジが適切か
- 赤く表示されている（未カバー）のコードがあるか

### 3. カバレッジチェック

```bash
./mvnw clean verify
```

**成功例**:
```
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
```

---

## 🎨 チャレンジ課題

### 課題1: Branch Coverageの改善

分岐カバレッジを90%以上にしてください。

```java
public class UserService {
    public String getUserStatus(User user) {
        if (user.getAge() < 18) {
            return "未成年";
        } else if (user.getAge() < 65) {
            return "成人";
        } else {
            return "高齢者";
        }
    }
}
```

**必要なテスト**:
- 18歳未満のケース
- 18歳以上65歳未満のケース
- 65歳以上のケース

### 課題2: Mutation Testing

PITestを導入してミューテーションテストを実施してください。

```xml
<plugin>
    <groupId>org.pitest</groupId>
    <artifactId>pitest-maven</artifactId>
    <version>1.15.3</version>
    <dependencies>
        <dependency>
            <groupId>org.pitest</groupId>
            <artifactId>pitest-junit5-plugin</artifactId>
            <version>1.2.1</version>
        </dependency>
    </dependencies>
    <configuration>
        <targetClasses>
            <param>com.example.hellospringboot.service.*</param>
        </targetClasses>
        <targetTests>
            <param>com.example.hellospringboot.service.*Test</param>
        </targetTests>
    </configuration>
</plugin>
```

```bash
./mvnw org.pitest:pitest-maven:mutationCoverage
```

### 課題3: CI/CDでのカバレッジ監視

GitHub Actionsでカバレッジを自動チェック：

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
          distribution: 'temurin'
      - name: Run tests with coverage
        run: ./mvnw clean verify
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          file: ./target/site/jacoco/jacoco.xml
```

---

## 🐛 トラブルシューティング

### エラー: "Rule violated for package"

**原因**: カバレッジが目標値（80%）に達していない

**解決策**:
1. カバーされていないコードを特定
2. 不足しているテストケースを追加
3. 必要に応じて除外設定を追加

### エラー: "Could not find artifact org.jacoco:jacoco-maven-plugin"

**原因**: ネットワーク問題またはMavenリポジトリの設定

**解決策**:
```bash
# Maven キャッシュをクリア
./mvnw dependency:purge-local-repository

# 再度ビルド
./mvnw clean install
```

### レポートが生成されない

**原因**: テストが実行されていない

**解決策**:
```bash
# テストをスキップせずに実行
./mvnw clean test

# target/site/jacoco/ が生成されているか確認
ls -la target/site/jacoco/
```

### カバレッジが100%にならない

**原因**: Lombokやフレームワーク生成コードが含まれている

**解決策**: 除外設定を追加
```xml
<configuration>
    <excludes>
        <!-- Lombok生成コード -->
        <exclude>**/*$*</exclude>
    </excludes>
</configuration>
```

---

## 📚 このステップで学んだこと

- ✅ テストカバレッジの概念と種類（行、分岐、メソッド、クラス）
- ✅ JaCoCoのセットアップと設定方法
- ✅ カバレッジレポートの生成と確認方法
- ✅ カバレッジレポートの読み方（色分け、パーセンテージ）
- ✅ カバーされていないコードの特定方法
- ✅ 目標カバレッジ（80%以上）の設定と自動チェック
- ✅ 特定のクラス・パッケージの除外設定
- ✅ ビルド時の自動カバレッジチェック
- ✅ カバレッジの継続的監視

**カバレッジのベストプラクティス**:
- 目標は80%〜90%が現実的
- 100%を目指す必要はない（コストと効果のバランス）
- 重要なビジネスロジックは必ず高カバレッジに
- 設定クラスやDTOは除外してOK
- カバレッジよりもテストの質が重要

---

## 🔄 Gitへのコミットとレビュー依頼

進捗を記録してレビューを受けましょう：

```bash
# 変更をステージング
git add .

# コミット
git commit -m "Step 29: テストカバレッジ完了 - JaCoCoで80%以上達成"

# リモートにプッシュ
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

おめでとうございます！🎉 **Phase 6: セキュリティとテスト**が完了しました！

レビューが完了したら、**[Phase 7: 実践的な機能](../phase7/STEP_30.md)** へ進みましょう！

ファイルアップロード、ページネーション、キャッシュ、非同期処理など、実務で必要となる機能を実装します。
