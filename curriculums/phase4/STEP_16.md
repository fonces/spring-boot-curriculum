# Step 16: DI/IoCコンテナの深掘り

## 🎯 このステップの目標

- DI（依存性注入）とIoC（制御の反転）の概念を理解する
- Beanのライフサイクルと管理方法を理解する
- コンストラクタインジェクションのメリットを理解する
- `@Primary`と`@Qualifier`で複数Beanを使い分けできる
- Beanのスコープ（singleton、prototypeなど）を理解する

**所要時間**: 約1時間30分

---

## 📋 事前準備

このステップを始める前に、以下を確認してください：

- Step 15（レイヤー化アーキテクチャ）が完了していること
- `@Service`、`@Repository`、`@Controller`の基本を理解していること
- コンストラクタインジェクションを使った経験があること

---

## 📝 概要
Spring Bootを使っていると`@Autowired`や`@Service`を何気なく使っていますが、その裏側で動いているDI/IoCコンテナの仕組みを理解することで、より適切な設計ができるようになります。

## 🧩 依存性注入（DI）とは

### DIがない世界

```java
public class UserController {
    private UserService userService;
    
    public UserController() {
        // コンストラクタ内でnewしている = 密結合
        this.userService = new UserServiceImpl();
    }
}
```

**問題点**:
- `UserController`が`UserServiceImpl`の具象クラスに依存
- テスト時にモックに差し替えられない
- 実装を変更する際にControllerも修正が必要

### DIがある世界

```java
@RestController
public class UserController {
    private final UserService userService;
    
    // コンストラクタで外部から注入される
    public UserController(UserService userService) {
        this.userService = userService;
    }
}
```

**メリット**:
- `UserController`は抽象（インターフェース）に依存
- テスト時に別実装を注入可能
- 疎結合で保守性が高い

## 🏗️ IoCコンテナとは

**IoC（Inversion of Control）= 制御の反転**

従来: アプリケーションコードが依存オブジェクトを作成・管理
↓
IoC: コンテナ（Spring）がオブジェクトを作成・管理

### Springコンテナの役割

```
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
```

## 📦 Beanとは

**Bean** = Springコンテナによって管理されるオブジェクト

### Beanの登録方法

#### 1. アノテーションベース（推奨）

```java
// コンポーネントスキャンで自動検出される
@Component  // 汎用的なBean
public class SomeComponent { }

@Service    // ビジネスロジック層
public class UserService { }

@Repository // データアクセス層
public class UserRepository { }

@Controller // プレゼンテーション層（通常のWeb）
public class UserController { }

@RestController // RESTful API用Controller
public class UserRestController { }

@Configuration // 設定クラス
public class AppConfig { }
```

#### 2. `@Bean`メソッドで手動登録

```java
@Configuration
public class AppConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        // 外部ライブラリなどBeanとして登録
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

## 💉 依存性注入の3つの方法

### 1. コンストラクタインジェクション（推奨⭐⭐⭐）

```java
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
```

**Lombokを使うとさらに簡潔**:

```java
@Service
@RequiredArgsConstructor  // finalフィールドのコンストラクタ自動生成
public class UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;
}
```

**メリット**:
- フィールドを`final`にできる（不変性）
- 必須の依存が明確
- テストでモックを渡しやすい
- 循環依存があるとコンパイルエラー

### 2. セッターインジェクション

```java
@Service
public class UserService {
    private UserRepository userRepository;
    
    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

**使うケース**:
- オプショナルな依存関係
- 後から依存を変更したい場合

### 3. フィールドインジェクション（非推奨）

```java
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;  // ❌ 避けるべき
}
```

**デメリット**:
- `final`にできない
- テストでモックを注入しにくい
- 循環依存に気づきにくい

## 🔍 複数のBeanがある場合の解決方法

### 問題: 同じインターフェースの実装が複数ある

```java
public interface NotificationService {
    void send(String message);
}

@Service
public class EmailNotificationService implements NotificationService {
    public void send(String message) {
        System.out.println("Email: " + message);
    }
}

@Service
public class SmsNotificationService implements NotificationService {
    public void send(String message) {
        System.out.println("SMS: " + message);
    }
}
```

このとき、以下はエラーになります:

```java
@RestController
public class NotificationController {
    private final NotificationService notificationService;
    
    public NotificationController(NotificationService notificationService) {
        // ❌ どっちを注入すればいいか分からない！
        this.notificationService = notificationService;
    }
}
```

### 解決策1: `@Primary`で優先Beanを指定

```java
@Service
@Primary  // ⭐ これがデフォルトで使われる
public class EmailNotificationService implements NotificationService {
    public void send(String message) {
        System.out.println("Email: " + message);
    }
}

@Service
public class SmsNotificationService implements NotificationService {
    public void send(String message) {
        System.out.println("SMS: " + message);
    }
}
```

### 解決策2: `@Qualifier`で指定

```java
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

**Bean名のデフォルト**: クラス名の先頭を小文字にしたもの
- `EmailNotificationService` → `emailNotificationService`
- `SmsNotificationService` → `smsNotificationService`

### 解決策3: カスタムBean名を指定

```java
@Service("email")  // Bean名を明示的に指定
public class EmailNotificationService implements NotificationService {
    // ...
}

@Service("sms")
public class SmsNotificationService implements NotificationService {
    // ...
}

// 使う側
public class NotificationController {
    private final NotificationService notificationService;
    
    public NotificationController(@Qualifier("email") NotificationService notificationService) {
        this.notificationService = notificationService;
    }
}
```

## 🔄 Beanのスコープ

| スコープ | 説明 | 使用例 |
|---|---|---|
| **singleton** | アプリケーション全体で1つのインスタンス（デフォルト） | Service, Repository |
| **prototype** | 要求のたびに新しいインスタンスを生成 | ステートフルなオブジェクト |
| **request** | HTTPリクエストごとに1つ（Web環境のみ） | リクエストスコープのデータ保持 |
| **session** | HTTPセッションごとに1つ（Web環境のみ） | ユーザーセッション情報 |

### 実装例

```java
@Service
@Scope("singleton")  // デフォルト、省略可能
public class UserService {
    // アプリ全体で1つのインスタンス
}

@Component
@Scope("prototype")
public class TaskProcessor {
    // 毎回新しいインスタンスが生成される
}

@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestContext {
    // HTTPリクエストごとに1つ
}
```

### スコープの確認

```java
@RestController
@RequestMapping("/api/scope")
public class ScopeTestController {
    private final ApplicationContext context;
    
    public ScopeTestController(ApplicationContext context) {
        this.context = context;
    }
    
    @GetMapping("/singleton")
    public String testSingleton() {
        UserService service1 = context.getBean(UserService.class);
        UserService service2 = context.getBean(UserService.class);
        
        // true（同じインスタンス）
        return "Same instance: " + (service1 == service2);
    }
    
    @GetMapping("/prototype")
    public String testPrototype() {
        TaskProcessor processor1 = context.getBean(TaskProcessor.class);
        TaskProcessor processor2 = context.getBean(TaskProcessor.class);
        
        // false（異なるインスタンス）
        return "Same instance: " + (processor1 == processor2);
    }
}
```

## 🛠️ 実践例: 戦略パターンとDI

### シナリオ: 支払い方法の切り替え

```java
// 支払いインターフェース
public interface PaymentService {
    void pay(Long orderId, Integer amount);
}

// クレジットカード決済
@Service("credit")
public class CreditCardPaymentService implements PaymentService {
    @Override
    public void pay(Long orderId, Integer amount) {
        System.out.println("Credit card payment: " + amount);
        // クレジットカード決済処理
    }
}

// PayPal決済
@Service("paypal")
public class PayPalPaymentService implements PaymentService {
    @Override
    public void pay(Long orderId, Integer amount) {
        System.out.println("PayPal payment: " + amount);
        // PayPal決済処理
    }
}

// 銀行振込
@Service("bank")
public class BankTransferPaymentService implements PaymentService {
    @Override
    public void pay(Long orderId, Integer amount) {
        System.out.println("Bank transfer: " + amount);
        // 銀行振込処理
    }
}
```

### 戦略パターンで動的に切り替え

```java
@Service
public class OrderService {
    private final Map<String, PaymentService> paymentServices;
    
    // Map<String, PaymentService>で全実装を受け取る
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

### Controller

```java
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    
    @PostMapping("/{id}/checkout")
    public ResponseEntity<String> checkout(
            @PathVariable Long id,
            @RequestParam Integer amount,
            @RequestParam String method) {  // "credit", "paypal", "bank"
        
        orderService.checkout(id, amount, method);
        return ResponseEntity.ok("Payment processed");
    }
}
```

### 動作確認

```bash
# クレジットカード決済
curl -X POST "http://localhost:8080/api/orders/1/checkout?amount=10000&method=credit"

# PayPal決済
curl -X POST "http://localhost:8080/api/orders/1/checkout?amount=10000&method=paypal"

# 銀行振込
curl -X POST "http://localhost:8080/api/orders/1/checkout?amount=10000&method=bank"
```

## 🏗️ `@Configuration`と`@Bean`の詳細

### 外部ライブラリのBean登録

```java
@Configuration
public class AppConfig {
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        return mapper;
    }
    
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // タイムアウト設定など
        return restTemplate;
    }
}
```

### 条件付きBean登録

```java
@Configuration
public class DataSourceConfig {
    
    @Bean
    @Profile("dev")  // 開発環境のみ
    public DataSource devDataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .build();
    }
    
    @Bean
    @Profile("prod")  // 本番環境のみ
    public DataSource prodDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://prod-db:3306/myapp");
        return dataSource;
    }
}
```

## ✅ 動作確認

### 1. Bean一覧の確認

```java
@RestController
@RequestMapping("/api/beans")
public class BeanListController {
    private final ApplicationContext context;
    
    public BeanListController(ApplicationContext context) {
        this.context = context;
    }
    
    @GetMapping
    public List<String> listBeans() {
        return Arrays.asList(context.getBeanDefinitionNames());
    }
    
    @GetMapping("/services")
    public List<String> listServices() {
        return context.getBeansWithAnnotation(Service.class)
            .keySet()
            .stream()
            .sorted()
            .toList();
    }
}
```

### 2. 起動ログでBean生成を確認

```
Creating shared instance of singleton bean 'userService'
Creating shared instance of singleton bean 'userRepository'
```

## 🚀 発展課題

### 課題1: Factory PatternとDI

プロダクト種別に応じた処理を行うファクトリーを実装してください。

```java
public interface ProductProcessor {
    void process(Product product);
}

@Service("book")
public class BookProcessor implements ProductProcessor { }

@Service("electronic")
public class ElectronicProcessor implements ProductProcessor { }

@Service
public class ProductProcessorFactory {
    private final Map<String, ProductProcessor> processors;
    
    public ProductProcessorFactory(Map<String, ProductProcessor> processors) {
        this.processors = processors;
    }
    
    public ProductProcessor getProcessor(String type) {
        return processors.get(type);
    }
}
```

### 課題2: 循環依存の解決

次のような循環依存を解決してください。

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

**解決策**: 設計を見直し、共通のServiceを抽出する

---

## 📚 このステップで学んだこと

- ✅ DI（依存性注入）とIoC（制御の反転）の概念
- ✅ コンストラクタインジェクション、セッターインジェクション、フィールドインジェクションの違い
- ✅ `@Component`、`@Service`、`@Repository`、`@Controller`の使い分け
- ✅ Beanの登録方法（アノテーションベース、`@Bean`メソッド）
- ✅ `@Primary`と`@Qualifier`による複数Bean対応
- ✅ Beanのスコープ（singleton、prototype、request、session）
- ✅ `@Configuration`と`@Bean`での手動Bean登録
- ✅ 戦略パターンとDIの組み合わせ

**ベストプラクティス**:
- コンストラクタインジェクション + `@RequiredArgsConstructor`を使う
- フィールドは`final`にして不変性を保つ
- 複数実装がある場合は`@Primary`または`@Qualifier`で明示
- デフォルトのsingletonスコープを基本とする

---

## 🔄 Gitへのコミットとレビュー依頼

進捗を記録してレビューを受けましょう：

```bash
# 変更をステージング
git add .

# コミット
git commit -m "Step 16: DI/IoCコンテナの深掘り完了"

# リモートにプッシュ
git push origin main
```

コミット後、**Slackでレビュー依頼**を出してフィードバックをもらいましょう！

---

## ➡️ 次のステップ

レビューが完了したら、[Step 17: 例外ハンドリング](STEP_17.md)へ進みましょう！

エラー処理を適切に実装し、クライアントに分かりやすいエラーレスポンスを返す方法を学びます。
