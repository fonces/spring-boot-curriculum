# Step 16: 依存性注入（DI）とIoCコンテナの深掘り

## 🎯 このステップの目標

- 依存性注入（Dependency Injection）の目的と利点を理解できる
- SpringのIoC（Inversion of Control）コンテナの動作原理を説明できる
- コンストラクタインジェクション、セッターインジェクション、フィールドインジェクションの違いを理解できる
- `@Autowired`, `@Qualifier`, `@Primary`, `@RequiredArgsConstructor`の使い分けができる
- Beanのスコープ（singleton, prototype, request, session）を理解し、適切に使い分けできる

**所要時間**: 約50分

---

## 📋 事前準備

- [Step 15: レイヤー化アーキテクチャ](STEP_15.md)が完了していること
- Controller, Service, Repositoryの役割を理解していること
- `@Service`, `@Repository`, `@RestController`アノテーションを使ったことがあること
- Javaのインターフェースと実装クラスの概念を理解していること

---

## 🧩 依存性注入（DI）とは

### DIがない世界

まず、依存性注入が**ない**場合のコードを見てみましょう：

```java
package com.example.hellospringboot.controllers;

import com.example.hellospringboot.services.UserService;

public class UserController {
    private UserService userService;
    
    // コンストラクタ内で直接newしている = 密結合
    public UserController() {
        this.userService = new UserServiceImpl();
    }
    
    public User getUser(Long id) {
        return userService.findById(id);
    }
}
```

**問題点**:
- ❌ `UserController`が`UserServiceImpl`の具象クラスに**直接依存**している
- ❌ テスト時にモック（テスト用のダミー実装）に差し替えられない
- ❌ 実装を変更する際に`UserController`のコードも修正が必要
- ❌ `UserServiceImpl`の依存関係（例: `UserRepository`）もControllerが知る必要がある
- ❌ クラスの生成タイミングを制御できない

---

### DIがある世界

依存性注入を使うと、以下のように改善されます：

```java
package com.example.hellospringboot.controllers;

import com.example.hellospringboot.services.UserService;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
public class UserController {
    private final UserService userService;
    
    // コンストラクタで外部から注入される
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
    }
}
```

**メリット**:
- ✅ `UserController`は**抽象（インターフェース）に依存**
- ✅ テスト時に別実装を注入可能（モック注入）
- ✅ 疎結合で保守性が高い
- ✅ Springコンテナが依存関係を自動解決
- ✅ クラスの生成タイミングをSpringが管理

---

## 🏛️ SpringのIoCコンテナ

### IoCとは

**IoC (Inversion of Control)** = 制御の反転

```
┌───────────────────────────────────────────┐
│    従来の制御フロー（制御が開発者側）         │
│                                           │
│  開発者が明示的に new でオブジェクトを生成   │
│  ↓                                        │
│  必要なタイミングで必要なクラスを作成        │
│  ↓                                        │
│  依存関係を手動で解決                       │
└───────────────────────────────────────────┘

┌───────────────────────────────────────────┐
│    IoC（制御がフレームワーク側）             │
│                                           │
│  Springがオブジェクトのライフサイクルを管理  │
│  ↓                                        │
│  必要なタイミングで自動的に注入              │
│  ↓                                        │
│  依存関係を自動で解決                       │
└───────────────────────────────────────────┘
```

### Springコンテナの役割

```
┌─────────────────────────────────────────┐
│       Spring IoC Container              │
│                                         │
│    ┌───────────┐    ┌───────────┐       │
│    │   Bean    │    │   Bean    │       │
│    │  Service  │    │   Repos   │       │
│    └───────────┘    └───────────┘       │
│         │               │               │
│         └───────────────┘               │
│        自動的に依存関係を解決             │
└─────────────────────────────────────────┘
          ↓  注入
┌─────────────────────┐
│   Controller        │
│  （Beanを受け取る）   │
└─────────────────────┘
```

**Springコンテナがやってくれること**:
1. **Beanの生成**: `@Component`, `@Service`, `@Repository`などをスキャンしてインスタンス化
2. **依存関係の解決**: コンストラクタやセッターを使ってBeanを注入
3. **ライフサイクル管理**: 初期化処理（`@PostConstruct`）や破棄処理（`@PreDestroy`）の実行
4. **スコープ管理**: singleton, prototype, request, sessionなど

---

## 💉 依存性注入の3つの方法

### 比較表

| 方法 | メリット | デメリット | 推奨度 |
|---|---|---|---|
| **コンストラクタ** | finalにできる、必須依存が明確、テストしやすい | - | ⭐⭐⭐ |
| **セッター** | オプショナルな依存に対応 | finalにできない、注入忘れのリスク | ⭐ |
| **フィールド** | 記述が簡潔 | テストしにくい、循環依存に気づきにくい | ❌ |

---

### 1. コンストラクタインジェクション（推奨⭐⭐⭐）

**基本形**:

```java
package com.example.hellospringboot.services;

import com.example.hellospringboot.repositories.UserRepository;
import com.example.hellospringboot.repositories.EmailService;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;
    
    // コンストラクタが1つだけなら@Autowired省略可能（Spring 4.3以降）
    public UserService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }
    
    public User createUser(User user) {
        User savedUser = userRepository.save(user);
        emailService.sendWelcomeEmail(savedUser.getEmail());
        return savedUser;
    }
}
```

**Lombokを使うとさらに簡潔**:

```java
package com.example.hellospringboot.services;

import com.example.hellospringboot.repositories.UserRepository;
import com.example.hellospringboot.repositories.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor  // finalフィールドのコンストラクタを自動生成
public class UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;
    
    public User createUser(User user) {
        User savedUser = userRepository.save(user);
        emailService.sendWelcomeEmail(savedUser.getEmail());
        return savedUser;
    }
}
```

**なぜ推奨されるのか**:
- ✅ `final`キーワードで**不変性を保証**（注入後に変更不可）
- ✅ 必須依存が**コンストラクタのシグネチャで明確**
- ✅ テスト時に**new UserService(mockRepo, mockEmail)** で簡単にモック注入可能
- ✅ **循環依存があるとコンパイルエラー**になるため早期発見

---

### 2. セッターインジェクション（オプショナル依存⭐）

```java
package com.example.hellospringboot.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private SmsService smsService;  // オプショナル（SMSが無効でもアプリは動作）
    
    // オプショナルな依存はセッターで注入
    @Autowired(required = false)  // 必須でない場合はrequired=false
    public void setSmsService(SmsService smsService) {
        this.smsService = smsService;
    }
    
    public void notify(String message) {
        if (smsService != null) {
            smsService.send(message);
        } else {
            // SMSサービスがない場合はスキップ
            System.out.println("SMS service not available");
        }
    }
}
```

**使用例**:
- 環境によって使う/使わないが変わる依存（例: 開発環境ではメール送信しない）
- デフォルト実装があり、カスタム実装がオプショナルな場合

---

### 3. フィールドインジェクション（非推奨❌）

```java
package com.example.hellospringboot.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired  // フィールドに直接注入
    private UserRepository userRepository;
    
    @Autowired
    private EmailService emailService;
    
    public User createUser(User user) {
        User savedUser = userRepository.save(user);
        emailService.sendWelcomeEmail(savedUser.getEmail());
        return savedUser;
    }
}
```

**なぜ非推奨か**:
- ❌ `final`にできない（注入後に変更可能）
- ❌ テスト時にリフレクションを使わないと注入できない
- ❌ 循環依存があっても実行時まで気づかない
- ❌ 必須依存が不明確（フィールド宣言を見ないとわからない）

**唯一許容されるケース**:
- Spring BootのテストクラスでのみOK（`@SpringBootTest`使用時）

```java
@SpringBootTest
class UserServiceTest {
    @Autowired  // テストクラスではフィールド注入が許容される
    private UserService userService;
}
```

---

## 🔍 複数のBeanがある場合の解決方法

### 問題: 同じインターフェースの実装が複数ある

```java
package com.example.hellospringboot.services;

public interface NotificationService {
    void send(String message);
}
```

```java
package com.example.hellospringboot.services;

import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService implements NotificationService {
    @Override
    public void send(String message) {
        System.out.println("Email: " + message);
    }
}
```

```java
package com.example.hellospringboot.services;

import org.springframework.stereotype.Service;

@Service
public class SmsNotificationService implements NotificationService {
    @Override
    public void send(String message) {
        System.out.println("SMS: " + message);
    }
}
```

このとき、以下はエラーになります:

```java
package com.example.hellospringboot.controllers;

import com.example.hellospringboot.services.NotificationService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {
    private final NotificationService notificationService;
    
    // ❌ エラー: どっちを注入すればいいか分からない！
    // Field notificationService in NotificationController required a single bean, but 2 were found
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
}
```

---

### 解決策1: `@Primary`で優先Beanを指定

```java
package com.example.hellospringboot.services;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary  // このBeanを優先的に注入
public class EmailNotificationService implements NotificationService {
    @Override
    public void send(String message) {
        System.out.println("Email: " + message);
    }
}
```

これで`NotificationController`には`EmailNotificationService`が注入されます。

**使用例**:
- デフォルト実装を明確にしたい場合
- 複数実装があるが、ほとんどの場合1つを使う場合

---

### 解決策2: `@Qualifier`で明示的に指定

```java
package com.example.hellospringboot.controllers;

import com.example.hellospringboot.services.NotificationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {
    private final NotificationService emailService;
    private final NotificationService smsService;
    
    public NotificationController(
        @Qualifier("emailNotificationService") NotificationService emailService,
        @Qualifier("smsNotificationService") NotificationService smsService
    ) {
        this.emailService = emailService;
        this.smsService = smsService;
    }
}
```

**デフォルトのBean名**:
- クラス名の先頭を小文字にしたもの
- `EmailNotificationService` → `emailNotificationService`
- `SmsNotificationService` → `smsNotificationService`

---

### 解決策3: カスタムBean名を指定

```java
package com.example.hellospringboot.services;

import org.springframework.stereotype.Service;

@Service("email")  // Bean名を"email"に指定
public class EmailNotificationService implements NotificationService {
    @Override
    public void send(String message) {
        System.out.println("Email: " + message);
    }
}
```

```java
package com.example.hellospringboot.services;

import org.springframework.stereotype.Service;

@Service("sms")  // Bean名を"sms"に指定
public class SmsNotificationService implements NotificationService {
    @Override
    public void send(String message) {
        System.out.println("SMS: " + message);
    }
}
```

```java
package com.example.hellospringboot.controllers;

import com.example.hellospringboot.services.NotificationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {
    private final NotificationService emailService;
    private final NotificationService smsService;
    
    public NotificationController(
        @Qualifier("email") NotificationService emailService,
        @Qualifier("sms") NotificationService smsService
    ) {
        this.emailService = emailService;
        this.smsService = smsService;
    }
}
```

---

## 🏗️ 実践例: 戦略パターンとDI

### シナリオ: 支払い方法の切り替え

複数の支払い方法（クレジットカード、PayPal、銀行振込）をDIで実装します。

#### 支払いインターフェース

以下のファイルを`src/main/java/com/example/hellospringboot/services/PaymentService.java`に作成します：

```java
package com.example.hellospringboot.services;

public interface PaymentService {
    void pay(Long orderId, Integer amount);
}
```

---

#### 各実装クラス

**クレジットカード**:

以下のファイルを`src/main/java/com/example/hellospringboot/services/CreditCardPaymentService.java`に作成します：

```java
package com.example.hellospringboot.services;

import org.springframework.stereotype.Service;

@Service("credit")
public class CreditCardPaymentService implements PaymentService {
    @Override
    public void pay(Long orderId, Integer amount) {
        System.out.println("Credit Card Payment: Order=" + orderId + ", Amount=" + amount);
        // 実際にはクレジットカード決済APIを呼び出す
    }
}
```

**PayPal**:

以下のファイルを`src/main/java/com/example/hellospringboot/services/PayPalPaymentService.java`に作成します：

```java
package com.example.hellospringboot.services;

import org.springframework.stereotype.Service;

@Service("paypal")
public class PayPalPaymentService implements PaymentService {
    @Override
    public void pay(Long orderId, Integer amount) {
        System.out.println("PayPal Payment: Order=" + orderId + ", Amount=" + amount);
        // 実際にはPayPal APIを呼び出す
    }
}
```

**銀行振込**:

以下のファイルを`src/main/java/com/example/hellospringboot/services/BankTransferPaymentService.java`に作成します：

```java
package com.example.hellospringboot.services;

import org.springframework.stereotype.Service;

@Service("bank")
public class BankTransferPaymentService implements PaymentService {
    @Override
    public void pay(Long orderId, Integer amount) {
        System.out.println("Bank Transfer Payment: Order=" + orderId + ", Amount=" + amount);
        // 実際には銀行振込APIを呼び出す
    }
}
```

---

#### 戦略パターンで動的に切り替え

以下のファイルを`src/main/java/com/example/hellospringboot/services/OrderService.java`に作成します：

```java
package com.example.hellospringboot.services;

import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class OrderService {
    // Spring が自動的に PaymentService の全実装を Map に注入
    // Key = Bean名 ("credit", "paypal", "bank")
    // Value = 実装インスタンス
    private final Map<String, PaymentService> paymentServices;
    
    public OrderService(Map<String, PaymentService> paymentServices) {
        this.paymentServices = paymentServices;
    }
    
    public void checkout(Long orderId, Integer amount, String paymentMethod) {
        PaymentService paymentService = paymentServices.get(paymentMethod);
        
        if (paymentService == null) {
            throw new IllegalArgumentException("Unknown payment method: " + paymentMethod);
        }
        
        paymentService.pay(orderId, amount);
    }
}
```

**ポイント**:
- `Map<String, PaymentService>`と宣言するだけで、Springがすべての実装をMapに詰めて注入
- Bean名（"credit", "paypal", "bank"）がMapのキーになる
- 実行時に`paymentMethod`パラメータで支払い方法を動的に切り替え

---

#### Controller

以下のファイルを`src/main/java/com/example/hellospringboot/controllers/OrderController.java`に作成します：

```java
package com.example.hellospringboot.controllers;

import com.example.hellospringboot.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    
    @PostMapping("/{orderId}/checkout")
    public String checkout(
        @PathVariable Long orderId,
        @RequestParam Integer amount,
        @RequestParam String paymentMethod
    ) {
        orderService.checkout(orderId, amount, paymentMethod);
        return "Payment processed with " + paymentMethod;
    }
}
```

---

### 動作確認

```bash
# クレジットカード決済
curl -X POST "http://localhost:8080/api/orders/1/checkout?amount=5000&paymentMethod=credit"
# 出力: Credit Card Payment: Order=1, Amount=5000

# PayPal決済
curl -X POST "http://localhost:8080/api/orders/1/checkout?amount=5000&paymentMethod=paypal"
# 出力: PayPal Payment: Order=1, Amount=5000

# 銀行振込
curl -X POST "http://localhost:8080/api/orders/1/checkout?amount=5000&paymentMethod=bank"
# 出力: Bank Transfer Payment: Order=1, Amount=5000

# 存在しない支払い方法
curl -X POST "http://localhost:8080/api/orders/1/checkout?amount=5000&paymentMethod=bitcoin"
# エラー: Unknown payment method: bitcoin
```

---

## 📦 Beanのスコープ

### スコープの種類

| スコープ | 説明 | インスタンス生成タイミング | 使用例 |
|---|---|---|---|
| **singleton** | アプリケーション全体で1つのインスタンス（デフォルト） | アプリ起動時 | Service, Repository |
| **prototype** | 要求のたびに新しいインスタンスを生成 | Bean取得時 | ステートフルなオブジェクト |
| **request** | HTTPリクエストごとに1つ（Web環境のみ） | リクエスト時 | リクエストスコープのデータ保持 |
| **session** | HTTPセッションごとに1つ（Web環境のみ） | セッション開始時 | ユーザーセッション情報 |

---

### 1. Singleton（デフォルト）

```java
package com.example.hellospringboot.services;

import org.springframework.stereotype.Service;

@Service  // デフォルトでsingleton
public class UserService {
    private int counter = 0;
    
    public void incrementCounter() {
        counter++;
        System.out.println("Counter: " + counter);
    }
}
```

**特徴**:
- アプリケーション全体で1つのインスタンス
- 複数のリクエストで**同じインスタンス**を共有
- **ステートレス**（状態を持たない）にすべき

**注意**:
- singletonスコープのBeanに**インスタンス変数**を持つと、複数リクエストで状態が共有される
- 上記の`counter`は全リクエストで共有されるため、想定外の動作になる可能性

---

### 2. Prototype

```java
package com.example.hellospringboot.services;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")  // 毎回新しいインスタンスを生成
public class ReportGenerator {
    private String reportData;
    
    public void setReportData(String data) {
        this.reportData = data;
    }
    
    public String generateReport() {
        return "Report: " + reportData;
    }
}
```

**使用例**:

```java
package com.example.hellospringboot.services;

import org.springframework.stereotype.Service;

@Service
public class ReportService {
    private final ApplicationContext context;
    
    public ReportService(ApplicationContext context) {
        this.context = context;
    }
    
    public String createReport(String data) {
        // 毎回新しいインスタンスを取得
        ReportGenerator generator = context.getBean(ReportGenerator.class);
        generator.setReportData(data);
        return generator.generateReport();
    }
}
```

**特徴**:
- Bean取得のたびに**新しいインスタンス**を生成
- **ステートフル**（状態を持つ）オブジェクトに使用
- 使用頻度は低い（ほとんどの場合singletonで十分）

---

### 3. Request（Web環境専用）

```java
package com.example.hellospringboot.services;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestContext {
    private String requestId;
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public String getRequestId() {
        return requestId;
    }
}
```

**特徴**:
- HTTPリクエストごとに新しいインスタンス
- リクエスト内で状態を共有したい場合に使用
- リクエスト終了時に破棄される

**使用例**:
- リクエストIDの管理
- ユーザー認証情報の一時保持
- リクエストスコープのログ情報

---

### 4. Session（Web環境専用）

```java
package com.example.hellospringboot.services;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ShoppingCart {
    private List<Product> items = new ArrayList<>();
    
    public void addItem(Product product) {
        items.add(product);
    }
    
    public List<Product> getItems() {
        return items;
    }
}
```

**特徴**:
- HTTPセッションごとに1つのインスタンス
- ユーザーごとに状態を保持
- セッション終了時に破棄される

**使用例**:
- ショッピングカート
- ユーザー設定（言語、テーマなど）
- ログイン状態の管理

---

## 🔄 Beanのライフサイクル

### 初期化と破棄

```java
package com.example.hellospringboot.services;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

@Service
public class CacheService {
    private Map<String, String> cache;
    
    // Bean生成直後に呼ばれる
    @PostConstruct
    public void init() {
        System.out.println("CacheService: Initializing cache...");
        cache = new HashMap<>();
        cache.put("key1", "value1");
    }
    
    public String get(String key) {
        return cache.get(key);
    }
    
    // Bean破棄直前に呼ばれる（アプリケーション終了時）
    @PreDestroy
    public void cleanup() {
        System.out.println("CacheService: Cleaning up cache...");
        cache.clear();
    }
}
```

**ライフサイクルの流れ**:

```
1. コンストラクタ呼び出し
   ↓
2. 依存性注入（@Autowiredなど）
   ↓
3. @PostConstruct 実行（初期化処理）
   ↓
4. Bean使用可能
   ↓
5. @PreDestroy 実行（破棄処理）
   ↓
6. インスタンス破棄
```

**使用例**:
- データベース接続の初期化/クローズ
- キャッシュの初期化/クリア
- スレッドプールの起動/停止

---

## 🔧 実践: UserServiceの完全なリファクタリング

### Before（Phase 3までの実装）

```java
package com.example.hellospringboot;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    List<User> findAll();
    User findById(@Param("id") Long id);
    void insert(User user);
}
```

```java
package com.example.hellospringboot;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserService {
    private final UserMapper userMapper;
    
    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }
    
    public List<User> findAll() {
        return userMapper.findAll();
    }
    
    public User findById(Long id) {
        return userMapper.findById(id);
    }
    
    public void createUser(User user) {
        userMapper.insert(user);
    }
}
```

---

### After（レイヤー化 + DI最適化）

**1. UserRepositoryインターフェース**:

以下のファイルを`src/main/java/com/example/hellospringboot/repositories/UserRepository.java`に作成します：

```java
package com.example.hellospringboot.repositories;

import com.example.hellospringboot.entities.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    List<User> findAll();
    Optional<User> findById(Long id);
    void save(User user);
    void deleteById(Long id);
}
```

**2. UserMapperインターフェース（MyBatis実装）**:

以下のファイルを`src/main/java/com/example/hellospringboot/mappers/UserMapper.java`に作成します：

```java
package com.example.hellospringboot.mappers;

import com.example.hellospringboot.entities.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {
    @Select("SELECT id, name, email, age FROM users")
    List<User> findAll();
    
    @Select("SELECT id, name, email, age FROM users WHERE id = #{id}")
    User findById(@Param("id") Long id);
    
    @Insert("INSERT INTO users (name, email, age) VALUES (#{name}, #{email}, #{age})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);
    
    @Delete("DELETE FROM users WHERE id = #{id}")
    void deleteById(@Param("id") Long id);
}
```

**3. UserRepositoryImpl（MyBatis実装クラス）**:

以下のファイルを`src/main/java/com/example/hellospringboot/repositories/UserRepositoryImpl.java`に作成します：

```java
package com.example.hellospringboot.repositories;

import com.example.hellospringboot.entities.User;
import com.example.hellospringboot.mappers.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final UserMapper userMapper;
    
    @Override
    public List<User> findAll() {
        return userMapper.findAll();
    }
    
    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userMapper.findById(id));
    }
    
    @Override
    public void save(User user) {
        userMapper.insert(user);
    }
    
    @Override
    public void deleteById(Long id) {
        userMapper.deleteById(id);
    }
}
```

**4. UserService（ビジネスロジック層）**:

以下のファイルを`src/main/java/com/example/hellospringboot/services/UserService.java`に作成します：

```java
package com.example.hellospringboot.services;

import com.example.hellospringboot.entities.User;
import com.example.hellospringboot.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }
    
    public User createUser(User user) {
        // ビジネスルール: メールアドレスの重複チェックなど
        userRepository.save(user);
        return user;
    }
    
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
```

**5. UserController（プレゼンテーション層）**:

以下のファイルを`src/main/java/com/example/hellospringboot/controllers/UserController.java`に作成します：

```java
package com.example.hellospringboot.controllers;

import com.example.hellospringboot.entities.User;
import com.example.hellospringboot.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }
    
    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }
    
    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }
    
    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }
}
```

---

### 改善ポイント

#### 1. Repositoryインターフェース導入

**Before**:
```java
@Service
public class UserService {
    private final UserMapper userMapper;  // MyBatisに直接依存
}
```

**After**:
```java
@Service
public class UserService {
    private final UserRepository userRepository;  // 抽象に依存
}
```

**メリット**:
- ServiceがMyBatisの実装詳細を知る必要がない
- 将来JPAに変更する場合も、Serviceは変更不要
- テスト時にモックRepositoryを簡単に注入できる

---

#### 2. Lombokで冗長なコード削減

**Before**:
```java
@Service
public class UserService {
    private final UserRepository userRepository;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

**After**:
```java
@Service
@RequiredArgsConstructor  // コンストラクタ自動生成
public class UserService {
    private final UserRepository userRepository;
}
```

---

#### 3. Optional導入でnullセーフティ向上

**Before**:
```java
public User getUserById(Long id) {
    User user = userMapper.findById(id);
    if (user == null) {
        throw new RuntimeException("User not found");
    }
    return user;
}
```

**After**:
```java
public User getUserById(Long id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("User not found: " + id));
}
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: 通知サービスの実装

以下の要件を満たす通知システムを実装してください：

**要件**:
1. `NotificationService`インターフェースを作成
2. `EmailNotificationService`と`SmsNotificationService`を実装
3. `UserService.createUser()`でユーザー作成時に両方の通知を送信
4. 通知サービスは`List<NotificationService>`で一括注入

**ヒント**:

```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final List<NotificationService> notificationServices;  // すべての実装を注入
    
    public User createUser(User user) {
        userRepository.save(user);
        
        // すべての通知サービスに送信
        notificationServices.forEach(service -> 
            service.send("Welcome, " + user.getName())
        );
        
        return user;
    }
}
```

---

### チャレンジ 2: Factory PatternとDI

以下の要件を満たすReportFactoryを実装してください：

**要件**:
1. `ReportGenerator`インターフェース（`generate(String data)`メソッド）
2. `PdfReportGenerator`, `ExcelReportGenerator`, `CsvReportGenerator`を実装
3. `ReportFactory`でレポートタイプに応じた生成器を返す
4. Mapを使った動的Bean取得

**ヒント**:

```java
@Service
public class ReportFactory {
    private final Map<String, ReportGenerator> generators;
    
    public ReportFactory(Map<String, ReportGenerator> generators) {
        this.generators = generators;
    }
    
    public ReportGenerator getGenerator(String type) {
        return generators.get(type);
    }
}
```

---

### チャレンジ 3: 循環依存の解決

次のような循環依存を解決してください：

```java
@Service
public class ServiceA {
    private final ServiceB serviceB;  // ❌ ServiceA → ServiceB
    
    public ServiceA(ServiceB serviceB) {
        this.serviceB = serviceB;
    }
}

@Service
public class ServiceB {
    private final ServiceA serviceA;  // ❌ ServiceB → ServiceA
    
    public ServiceB(ServiceA serviceA) {
        this.serviceA = serviceA;
    }
}
```

**エラーメッセージ**:
```
The dependencies of some of the beans in the application context form a cycle:
   serviceA defined in file [.../ServiceA.class]
   ↓
   serviceB defined in file [.../ServiceB.class]
   ↓
   serviceA
```

**解決方法のヒント**:
1. 設計を見直し、共通のServiceCを抽出する
2. `@Lazy`アノテーションを使う（推奨しない）
3. セッターインジェクションに変更（推奨しない）

**推奨される設計**:

```java
// 共通ロジックを抽出
@Service
public class CommonService {
    public void doCommonTask() {
        // 共通処理
    }
}

@Service
public class ServiceA {
    private final CommonService commonService;
    
    public ServiceA(CommonService commonService) {
        this.commonService = commonService;
    }
}

@Service
public class ServiceB {
    private final CommonService commonService;
    
    public ServiceB(CommonService commonService) {
        this.commonService = commonService;
    }
}
```

---

## 🐛 トラブルシューティング

### エラー 1: "required a single bean, but 2 were found"

**エラーメッセージ**:
```
Field notificationService in NotificationController required a single bean, but 2 were found:
	- emailNotificationService: defined in file [...]
	- smsNotificationService: defined in file [...]
```

**原因**: 同じインターフェースの実装が複数あり、どれを注入すればいいか分からない

**解決策**:
1. `@Primary`で優先Beanを指定
2. `@Qualifier`で明示的に指定
3. カスタムBean名 + `@Qualifier`

---

### エラー 2: "The dependencies of some of the beans form a cycle"

**エラーメッセージ**:
```
The dependencies of some of the beans in the application context form a cycle:
   serviceA
   ↓
   serviceB
   ↓
   serviceA
```

**原因**: 循環依存（ServiceA → ServiceB → ServiceA）

**解決策**:
1. **設計を見直す**（推奨）: 共通の依存先を抽出
2. `@Lazy`アノテーション: 初回使用時まで注入を遅延（根本的解決にならない）

```java
@Service
public class ServiceA {
    private final ServiceB serviceB;
    
    public ServiceA(@Lazy ServiceB serviceB) {  // 遅延注入
        this.serviceB = serviceB;
    }
}
```

---

### エラー 3: "No qualifying bean of type 'UserRepository'"

**エラーメッセージ**:
```
No qualifying bean of type 'com.example.hellospringboot.repositories.UserRepository' available
```

**原因**: `UserRepository`インターフェースの実装クラスに`@Repository`がない

**解決策**:
```java
@Repository  // これを忘れずに！
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final UserMapper userMapper;
}
```

---

### エラー 4: "Field injection is not recommended"

**警告メッセージ**（IntelliJ IDEA）:
```
Field injection is not recommended
```

**原因**: フィールドインジェクション（`@Autowired private ...`）を使用

**解決策**: コンストラクタインジェクションに変更

**Before**:
```java
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
}
```

**After**:
```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
}
```

---

### エラー 5: "Cannot resolve symbol 'RequiredArgsConstructor'"

**エラーメッセージ**:
```
Cannot resolve symbol 'RequiredArgsConstructor'
```

**原因**: Lombokの依存関係が不足、またはアノテーション処理が有効になっていない

**解決策1**: pom.xmlにLombokを追加

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

**解決策2**: IDEでアノテーション処理を有効化

IntelliJ IDEAの場合:
1. `Settings` → `Build, Execution, Deployment` → `Compiler` → `Annotation Processors`
2. `Enable annotation processing` にチェック
3. プロジェクトをリビルド

---

## 📚 このステップで学んだこと

- ✅ **依存性注入（DI）** の目的と利点（疎結合、テスタビリティ向上）
- ✅ **IoCコンテナ** の役割（Bean生成、依存関係解決、ライフサイクル管理）
- ✅ **コンストラクタインジェクション** が推奨される理由（final化、テスト容易性）
- ✅ **`@Autowired`, `@Qualifier`, `@Primary`** の使い分け
- ✅ **複数Bean問題** の解決方法（Map注入、Qualifier指定）
- ✅ **戦略パターン** とDIの組み合わせ（支払い方法の動的切り替え）
- ✅ **Beanのスコープ** （singleton, prototype, request, session）
- ✅ **Beanのライフサイクル** （`@PostConstruct`, `@PreDestroy`）
- ✅ **Lombok** による冗長なコード削減（`@RequiredArgsConstructor`）
- ✅ **循環依存** の問題と解決方法（設計見直し、共通サービス抽出）

---

## 💡 補足: DIとテスタビリティ

### DIがテストを簡単にする理由

**DIなし（テストしにくい）**:

```java
public class UserService {
    private UserRepository userRepository;
    
    public UserService() {
        this.userRepository = new UserRepositoryImpl();  // 実装に直接依存
    }
}
```

テストコード:
```java
@Test
void testGetUserById() {
    UserService service = new UserService();
    // 実際のDBに接続してしまう！モックに差し替えられない
    User user = service.getUserById(1L);
}
```

---

**DIあり（テストしやすい）**:

```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;  // インターフェースに依存
}
```

テストコード:
```java
@Test
void testGetUserById() {
    // モックRepositoryを作成
    UserRepository mockRepository = mock(UserRepository.class);
    when(mockRepository.findById(1L))
        .thenReturn(Optional.of(new User(1L, "Test User", "test@example.com", 30)));
    
    // モックを注入
    UserService service = new UserService(mockRepository);
    
    // テスト実行（実際のDBに接続しない）
    User user = service.getUserById(1L);
    assertEquals("Test User", user.getName());
}
```

**DIのメリット**:
- ✅ 実際のDBに接続せずにテスト可能
- ✅ テストデータを自由に設定できる
- ✅ テストの実行速度が速い（DB不要）
- ✅ テストの独立性が高い（他のテストに影響されない）

---

## 💡 補足: Spring BootのComponent Scanの仕組み

### どうやってBeanを見つけるのか

Spring Bootは以下の手順でBeanを登録します：

**1. メインクラスのパッケージを起点にスキャン**:

```java
package com.example.hellospringboot;  // このパッケージ以下をスキャン

@SpringBootApplication
public class HelloSpringBootApplication {
    public static void main(String[] args) {
        SpringApplication.run(HelloSpringBootApplication.class, args);
    }
}
```

**2. `@Component`とその派生アノテーションを探す**:

| アノテーション | 用途 | 意味 |
|---|---|---|
| `@Component` | 汎用的なBean | コンポーネント |
| `@Service` | ビジネスロジック層 | サービス |
| `@Repository` | データアクセス層 | リポジトリ |
| `@Controller` | プレゼンテーション層 | コントローラー |
| `@RestController` | REST API層 | RESTコントローラー |
| `@Configuration` | 設定クラス | 設定 |

**3. 依存関係を解決してBean登録**:

```
1. @Service, @Repository, @RestController をスキャン
   ↓
2. 各クラスのコンストラクタを確認
   ↓
3. 必要な依存Bean（引数）を探す
   ↓
4. すべての依存Beanが見つかったら登録
   ↓
5. 依存Beanを注入してインスタンス化
```

---

### スキャン対象外のパッケージ

**問題**:

```
com.example.hellospringboot/  ← メインクラス
com.external.library/         ← スキャン対象外！
```

**解決策**: `@ComponentScan`で明示的に指定

```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.example.hellospringboot", "com.external.library"})
public class HelloSpringBootApplication {
}
```

---

## 💡 補足: `@Autowired`の省略ルール

### Spring 4.3以降の省略ルール

**ルール**: コンストラクタが1つだけなら`@Autowired`は省略可能

**省略可能**:
```java
@Service
public class UserService {
    private final UserRepository userRepository;
    
    // コンストラクタが1つだけ → @Autowired不要
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

**省略不可**:
```java
@Service
public class UserService {
    private final UserRepository userRepository;
    
    public UserService() {
        this.userRepository = null;
    }
    
    // コンストラクタが複数ある → @Autowired必要
    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

---

## 🎓 発展: デザインパターンとDI

### 1. Factory Pattern

```java
public interface ReportGenerator {
    String generate(String data);
}

@Service("pdf")
public class PdfReportGenerator implements ReportGenerator {
    public String generate(String data) {
        return "PDF: " + data;
    }
}

@Service("excel")
public class ExcelReportGenerator implements ReportGenerator {
    public String generate(String data) {
        return "Excel: " + data;
    }
}

@Service
public class ReportFactory {
    private final Map<String, ReportGenerator> generators;
    
    public ReportFactory(Map<String, ReportGenerator> generators) {
        this.generators = generators;
    }
    
    public String createReport(String type, String data) {
        ReportGenerator generator = generators.get(type);
        return generator.generate(data);
    }
}
```

---

### 2. Strategy Pattern

```java
public interface SortStrategy {
    List<User> sort(List<User> users);
}

@Service
public class NameSortStrategy implements SortStrategy {
    public List<User> sort(List<User> users) {
        return users.stream()
            .sorted(Comparator.comparing(User::getName))
            .collect(Collectors.toList());
    }
}

@Service
public class AgeSortStrategy implements SortStrategy {
    public List<User> sort(List<User> users) {
        return users.stream()
            .sorted(Comparator.comparing(User::getAge))
            .collect(Collectors.toList());
    }
}

@Service
public class UserSorter {
    private final Map<String, SortStrategy> strategies;
    
    public UserSorter(Map<String, SortStrategy> strategies) {
        this.strategies = strategies;
    }
    
    public List<User> sort(List<User> users, String strategy) {
        return strategies.get(strategy).sort(users);
    }
}
```

---

### 3. Observer Pattern

```java
public interface UserEventListener {
    void onUserCreated(User user);
}

@Service
public class EmailNotificationListener implements UserEventListener {
    public void onUserCreated(User user) {
        System.out.println("Sending email to " + user.getEmail());
    }
}

@Service
public class LoggingListener implements UserEventListener {
    public void onUserCreated(User user) {
        System.out.println("User created: " + user.getName());
    }
}

@Service
public class UserService {
    private final UserRepository userRepository;
    private final List<UserEventListener> listeners;
    
    public UserService(UserRepository userRepository, List<UserEventListener> listeners) {
        this.userRepository = userRepository;
        this.listeners = listeners;
    }
    
    public User createUser(User user) {
        userRepository.save(user);
        
        // すべてのリスナーに通知
        listeners.forEach(listener -> listener.onUserCreated(user));
        
        return user;
    }
}
```

---

## 📖 参考資料

### 公式ドキュメント

- [Spring Framework Core - Dependency Injection](https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html)
- [Spring Boot Reference - Dependency Injection](https://docs.spring.io/spring-boot/reference/using/spring-beans-and-dependency-injection.html)
- [Spring Framework - Bean Scopes](https://docs.spring.io/spring-framework/reference/core/beans/factory-scopes.html)

### 関連記事

- [Constructor Injection vs Field Injection](https://www.baeldung.com/constructor-injection-in-spring)
- [Spring @Autowired Annotation](https://www.baeldung.com/spring-autowire)
- [Spring @Qualifier Annotation](https://www.baeldung.com/spring-qualifier-annotation)

---

## ➡️ 次のステップ

[Step 17: 例外ハンドリング](STEP_17.md)へ進みましょう！

次のステップでは、Spring Bootで例外を適切にハンドリングする方法を学びます：

- `@ControllerAdvice`でグローバル例外ハンドリング
- カスタム例外クラスの作成
- HTTPステータスコードの適切な使い分け
- エラーレスポンスのJSON化
- バリデーションエラーの詳細な返却

例外処理を適切に実装することで、エラー発生時にもユーザーにわかりやすいメッセージを返せるAPIを作りましょう！
