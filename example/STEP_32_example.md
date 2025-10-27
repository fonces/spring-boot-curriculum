# Step 32: 総合テストとデプロイ準備

## 🎯 このステップの目標

- 総合的なテストを実装する
- Dockerイメージを作成する
- 本番環境用の設定を準備する
- CI/CDパイプラインを構築する

**所要時間**: 約3時間

---

## 🚀 ステップ1: 総合テスト

### 1-1. TaskServiceTest（完全版）

**ファイルパス**: `src/test/java/com/example/hellospringboot/service/TaskServiceTest.java`

```java
package com.example.hellospringboot.service;

import com.example.hellospringboot.dto.request.TaskCreateRequest;
import com.example.hellospringboot.dto.response.TaskResponse;
import com.example.hellospringboot.entity.Project;
import com.example.hellospringboot.entity.Task;
import com.example.hellospringboot.entity.User;
import com.example.hellospringboot.enums.Priority;
import com.example.hellospringboot.enums.TaskStatus;
import com.example.hellospringboot.exception.ResourceNotFoundException;
import com.example.hellospringboot.mapper.TaskMapper;
import com.example.hellospringboot.repository.ProjectRepository;
import com.example.hellospringboot.repository.TaskRepository;
import com.example.hellospringboot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService Tests")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TaskService taskService;

    private Project testProject;
    private User testUser;
    private Task testTask;
    private TaskCreateRequest createRequest;
    private TaskResponse taskResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        testProject = Project.builder()
                .id(1L)
                .name("Test Project")
                .owner(testUser)
                .build();

        testTask = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .project(testProject)
                .status(TaskStatus.TODO)
                .priority(Priority.HIGH)
                .dueDate(LocalDate.now().plusDays(7))
                .build();

        createRequest = TaskCreateRequest.builder()
                .title("Test Task")
                .description("Test Description")
                .projectId(1L)
                .priority(Priority.HIGH)
                .dueDate(LocalDate.now().plusDays(7))
                .build();

        taskResponse = TaskResponse.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .projectId(1L)
                .projectName("Test Project")
                .status(TaskStatus.TODO)
                .priority(Priority.HIGH)
                .build();
    }

    @Test
    @DisplayName("タスクを作成できること")
    void testCreateTask() {
        // Given
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(taskMapper.toEntity(any(TaskCreateRequest.class))).thenReturn(testTask);
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        when(taskMapper.toResponse(any(Task.class))).thenReturn(taskResponse);

        // When
        TaskResponse result = taskService.createTask(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Task");
        verify(projectRepository).findById(1L);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("存在しないプロジェクトIDの場合は例外をスローすること")
    void testCreateTaskWithInvalidProject() {
        // Given
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());
        createRequest.setProjectId(999L);

        // When & Then
        assertThatThrownBy(() -> taskService.createTask(createRequest))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(projectRepository).findById(999L);
        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("タスクのステータスを変更できること")
    void testUpdateStatus() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        when(taskMapper.toResponse(any(Task.class))).thenReturn(taskResponse);

        // When
        TaskResponse result = taskService.updateStatus(1L, TaskStatus.IN_PROGRESS);

        // Then
        assertThat(result).isNotNull();
        verify(taskRepository).findById(1L);
        verify(taskRepository).save(testTask);
        assertThat(testTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }
}
```

### 1-2. TaskControllerIntegrationTest

**ファイルパス**: `src/test/java/com/example/hellospringboot/controller/TaskControllerIntegrationTest.java`

```java
package com.example.hellospringboot.controller;

import com.example.hellospringboot.dto.request.TaskCreateRequest;
import com.example.hellospringboot.entity.Project;
import com.example.hellospringboot.entity.Task;
import com.example.hellospringboot.entity.User;
import com.example.hellospringboot.enums.Priority;
import com.example.hellospringboot.enums.TaskStatus;
import com.example.hellospringboot.repository.ProjectRepository;
import com.example.hellospringboot.repository.TaskRepository;
import com.example.hellospringboot.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("TaskController Integration Tests")
class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Project testProject;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        // テストユーザー作成
        testUser = userRepository.save(User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .build());

        // テストプロジェクト作成
        testProject = projectRepository.save(Project.builder()
                .name("Test Project")
                .description("Test Description")
                .owner(testUser)
                .build());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("タスクを作成できること")
    void testCreateTask() throws Exception {
        // Given
        TaskCreateRequest request = TaskCreateRequest.builder()
                .title("New Task")
                .description("Task Description")
                .projectId(testProject.getId())
                .priority(Priority.HIGH)
                .dueDate(LocalDate.now().plusDays(7))
                .build();

        // When & Then
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New Task"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("タスクを検索できること")
    void testSearchTasks() throws Exception {
        // Given
        taskRepository.save(Task.builder()
                .title("Task 1")
                .project(testProject)
                .status(TaskStatus.TODO)
                .priority(Priority.HIGH)
                .build());

        taskRepository.save(Task.builder()
                .title("Task 2")
                .project(testProject)
                .status(TaskStatus.IN_PROGRESS)
                .priority(Priority.MEDIUM)
                .build());

        // When & Then
        mockMvc.perform(get("/api/tasks/search")
                        .param("projectId", testProject.getId().toString())
                        .param("status", "TODO"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Task 1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("タスクのステータスを変更できること")
    void testUpdateTaskStatus() throws Exception {
        // Given
        Task task = taskRepository.save(Task.builder()
                .title("Test Task")
                .project(testProject)
                .status(TaskStatus.TODO)
                .priority(Priority.MEDIUM)
                .build());

        // When & Then
        mockMvc.perform(patch("/api/tasks/{id}/status", task.getId())
                        .param("status", "IN_PROGRESS"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }
}
```

---

## 🚀 ステップ2: Dockerfile作成

### 2-1. マルチステージDockerfile

**ファイルパス**: `Dockerfile`

```dockerfile
# ビルドステージ
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Mavenラッパーとpom.xmlをコピー
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# 依存関係をダウンロード（キャッシュ活用）
RUN ./mvnw dependency:go-offline

# ソースコードをコピー
COPY src src

# ビルド
RUN ./mvnw clean package -DskipTests

# 実行ステージ
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# ビルドステージから成果物をコピー
COPY --from=builder /app/target/*.jar app.jar

# ヘルスチェック
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 非rootユーザーで実行
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 環境変数
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# 実行
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]

EXPOSE 8080
```

### 2-2. .dockerignore

**ファイルパス**: `.dockerignore`

```
target/
!target/*.jar
.mvn/wrapper/maven-wrapper.jar
.git
.gitignore
README.md
*.md
.idea
.vscode
*.iml
```

---

## 🚀 ステップ3: 本番環境用設定

### 3-1. application-prod.yml

**ファイルパス**: `src/main/resources/application-prod.yml`

```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:mysql://localhost:3306/taskmanager}
    username: ${DATABASE_USERNAME:root}
    password: ${DATABASE_PASSWORD:password}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: false

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}

  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}

# JWT設定
jwt:
  secret: ${JWT_SECRET:your-very-long-and-secure-secret-key-change-in-production}
  expiration: 86400000  # 24時間

# ファイルアップロード
file:
  upload-dir: ${FILE_UPLOAD_DIR:/app/uploads}

# ログ設定
logging:
  level:
    root: INFO
    com.example.hellospringboot: INFO
  file:
    name: /app/logs/application.log
  logback:
    rollingpolicy:
      max-file-size: 10MB
      max-history: 30

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
```

---

## 🚀 ステップ4: Docker Compose（本番）

### 4-1. docker-compose.prod.yml

**ファイルパス**: `docker-compose.prod.yml`

```yaml
version: '3.8'

services:
  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: taskmanager-app
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DATABASE_URL: jdbc:mysql://mysql:3306/taskmanager?useSSL=false&allowPublicKeyRetrieval=true
      DATABASE_USERNAME: taskuser
      DATABASE_PASSWORD: taskpass
      REDIS_HOST: redis
      REDIS_PORT: 6379
      JWT_SECRET: ${JWT_SECRET}
      MAIL_USERNAME: ${MAIL_USERNAME}
      MAIL_PASSWORD: ${MAIL_PASSWORD}
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started
    networks:
      - taskmanager-network
    restart: unless-stopped

  mysql:
    image: mysql:8.0
    container_name: taskmanager-mysql
    environment:
      MYSQL_ROOT_PASSWORD: rootpass
      MYSQL_DATABASE: taskmanager
      MYSQL_USER: taskuser
      MYSQL_PASSWORD: taskpass
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - taskmanager-network
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    container_name: taskmanager-redis
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    command: redis-server --appendonly yes
    networks:
      - taskmanager-network
    restart: unless-stopped

  nginx:
    image: nginx:alpine
    container_name: taskmanager-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./ssl:/etc/nginx/ssl:ro
    depends_on:
      - app
    networks:
      - taskmanager-network
    restart: unless-stopped

volumes:
  mysql-data:
  redis-data:

networks:
  taskmanager-network:
    driver: bridge
```

---

## 🚀 ステップ5: GitHub Actions CI/CD

### 5-1. GitHub Actions ワークフロー

**ファイルパス**: `.github/workflows/ci-cd.yml`

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven
      
      - name: Run tests
        run: ./mvnw clean test
      
      - name: Generate test report
        if: always()
        uses: dorny/test-reporter@v1
        with:
          name: Maven Tests
          path: target/surefire-reports/*.xml
          reporter: java-junit

  build:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven
      
      - name: Build with Maven
        run: ./mvnw clean package -DskipTests
      
      - name: Build Docker image
        run: docker build -t taskmanager:latest .
      
      - name: Log in to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}
      
      - name: Push Docker image
        run: |
          docker tag taskmanager:latest ${{ secrets.DOCKER_USERNAME }}/taskmanager:latest
          docker push ${{ secrets.DOCKER_USERNAME }}/taskmanager:latest
```

---

## ✅ 動作確認

### Dockerビルドとテスト

```bash
# Dockerイメージをビルド
docker build -t taskmanager:latest .

# コンテナを起動
docker-compose -f docker-compose.prod.yml up -d

# ログを確認
docker-compose -f docker-compose.prod.yml logs -f app

# ヘルスチェック
curl http://localhost:8080/actuator/health
```

---

## 🎨 チャレンジ課題

### チャレンジ 1: Kubernetes デプロイ

Kubernetes用のマニフェストファイルを作成します。

**ファイルパス**: `k8s/namespace.yaml`

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: taskmanagement
```

**ファイルパス**: `k8s/configmap.yaml`

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: taskmanagement
data:
  SPRING_PROFILES_ACTIVE: "prod"
  SPRING_DATASOURCE_URL: "jdbc:mysql://mysql-service:3306/taskmanagement"
  SPRING_DATA_REDIS_HOST: "redis-service"
  SPRING_DATA_REDIS_PORT: "6379"
```

**ファイルパス**: `k8s/secret.yaml`

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secret
  namespace: taskmanagement
type: Opaque
data:
  # Base64エンコードされた値
  SPRING_DATASOURCE_USERNAME: ZGJ1c2Vy  # dbuser
  SPRING_DATASOURCE_PASSWORD: cGFzc3dvcmQ=  # password
  JWT_SECRET: bXktc2VjcmV0LWtleS1mb3Itand0LXRva2Vu  # my-secret-key-for-jwt-token
  SMTP_PASSWORD: c210cC1wYXNzd29yZA==  # smtp-password
```

**ファイルパス**: `k8s/mysql-deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mysql
  namespace: taskmanagement
spec:
  replicas: 1
  selector:
    matchLabels:
      app: mysql
  template:
    metadata:
      labels:
        app: mysql
    spec:
      containers:
        - name: mysql
          image: mysql:8.0
          env:
            - name: MYSQL_ROOT_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: app-secret
                  key: SPRING_DATASOURCE_PASSWORD
            - name: MYSQL_DATABASE
              value: taskmanagement
            - name: MYSQL_USER
              valueFrom:
                secretKeyRef:
                  name: app-secret
                  key: SPRING_DATASOURCE_USERNAME
            - name: MYSQL_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: app-secret
                  key: SPRING_DATASOURCE_PASSWORD
          ports:
            - containerPort: 3306
          volumeMounts:
            - name: mysql-storage
              mountPath: /var/lib/mysql
      volumes:
        - name: mysql-storage
          persistentVolumeClaim:
            claimName: mysql-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: mysql-service
  namespace: taskmanagement
spec:
  selector:
    app: mysql
  ports:
    - protocol: TCP
      port: 3306
      targetPort: 3306
  type: ClusterIP
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: mysql-pvc
  namespace: taskmanagement
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi
```

**ファイルパス**: `k8s/redis-deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
  namespace: taskmanagement
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
        - name: redis
          image: redis:7-alpine
          ports:
            - containerPort: 6379
---
apiVersion: v1
kind: Service
metadata:
  name: redis-service
  namespace: taskmanagement
spec:
  selector:
    app: redis
  ports:
    - protocol: TCP
      port: 6379
      targetPort: 6379
  type: ClusterIP
```

**ファイルパス**: `k8s/app-deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: taskmanagement-app
  namespace: taskmanagement
spec:
  replicas: 3
  selector:
    matchLabels:
      app: taskmanagement-app
  template:
    metadata:
      labels:
        app: taskmanagement-app
    spec:
      containers:
        - name: app
          image: your-registry/taskmanagement:latest
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              valueFrom:
                configMapKeyRef:
                  name: app-config
                  key: SPRING_PROFILES_ACTIVE
            - name: SPRING_DATASOURCE_URL
              valueFrom:
                configMapKeyRef:
                  name: app-config
                  key: SPRING_DATASOURCE_URL
            - name: SPRING_DATASOURCE_USERNAME
              valueFrom:
                secretKeyRef:
                  name: app-secret
                  key: SPRING_DATASOURCE_USERNAME
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: app-secret
                  key: SPRING_DATASOURCE_PASSWORD
            - name: JWT_SECRET
              valueFrom:
                secretKeyRef:
                  name: app-secret
                  key: JWT_SECRET
            - name: SPRING_DATA_REDIS_HOST
              valueFrom:
                configMapKeyRef:
                  name: app-config
                  key: SPRING_DATA_REDIS_HOST
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 5
          resources:
            requests:
              memory: "512Mi"
              cpu: "500m"
            limits:
              memory: "1Gi"
              cpu: "1000m"
---
apiVersion: v1
kind: Service
metadata:
  name: taskmanagement-service
  namespace: taskmanagement
spec:
  selector:
    app: taskmanagement-app
  ports:
    - protocol: TCP
      port: 80
      targetPort: 8080
  type: LoadBalancer
```

**ファイルパス**: `k8s/ingress.yaml`

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: taskmanagement-ingress
  namespace: taskmanagement
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  ingressClassName: nginx
  tls:
    - hosts:
        - taskmanagement.example.com
      secretName: taskmanagement-tls
  rules:
    - host: taskmanagement.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: taskmanagement-service
                port:
                  number: 80
```

**デプロイコマンド**:

```bash
# Namespaceの作成
kubectl apply -f k8s/namespace.yaml

# ConfigMapとSecretの作成
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml

# データベースとRedisのデプロイ
kubectl apply -f k8s/mysql-deployment.yaml
kubectl apply -f k8s/redis-deployment.yaml

# アプリケーションのデプロイ
kubectl apply -f k8s/app-deployment.yaml

# Ingressの設定
kubectl apply -f k8s/ingress.yaml

# 確認
kubectl get pods -n taskmanagement
kubectl get services -n taskmanagement
```

### チャレンジ 2: モニタリング

Prometheus + Grafanaでモニタリングを実装します。

**依存関係追加** (`pom.xml`):

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**application-prod.yml に追加**:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
```

**ファイルパス**: `monitoring/prometheus.yml`

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'spring-boot-app'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['taskmanagement-service:80']
        labels:
          application: 'taskmanagement'
          
  - job_name: 'mysql'
    static_configs:
      - targets: ['mysql-exporter:9104']
      
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']
```

**ファイルパス**: `monitoring/docker-compose.monitoring.yml`

```yaml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
    networks:
      - monitoring

  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_USERS_ALLOW_SIGN_UP=false
    volumes:
      - grafana-data:/var/lib/grafana
      - ./grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./grafana/datasources:/etc/grafana/provisioning/datasources
    networks:
      - monitoring

  mysql-exporter:
    image: prom/mysqld-exporter:latest
    container_name: mysql-exporter
    environment:
      - DATA_SOURCE_NAME=exporter:password@(mysql:3306)/
    ports:
      - "9104:9104"
    networks:
      - monitoring

  redis-exporter:
    image: oliver006/redis_exporter:latest
    container_name: redis-exporter
    environment:
      - REDIS_ADDR=redis:6379
    ports:
      - "9121:9121"
    networks:
      - monitoring

volumes:
  prometheus-data:
  grafana-data:

networks:
  monitoring:
    driver: bridge
```

**ファイルパス**: `monitoring/grafana/datasources/prometheus.yml`

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
```

**ファイルパス**: `src/main/java/com/example/hellospringboot/config/MetricsConfig.java`

```java
package com.example.hellospringboot.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags("application", "taskmanagement");
    }

    @Bean
    public Timer.Builder taskExecutionTimer(MeterRegistry registry) {
        return Timer.builder("task.execution.time")
                .description("Task execution time")
                .register(registry)
                .toBuilder();
    }
}
```

**カスタムメトリクスの例**:

```java
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final MeterRegistry meterRegistry;

    @Transactional
    public TaskResponse createTask(TaskCreateRequest request, User currentUser) {
        // カウンターメトリクス
        meterRegistry.counter("tasks.created", 
                "priority", request.getPriority().name()).increment();
        
        // タイマーメトリクス
        return Timer.builder("task.creation.time")
                .register(meterRegistry)
                .record(() -> {
                    // タスク作成処理
                    Task task = taskMapper.toEntity(request);
                    // ... 省略
                    return taskMapper.toResponse(savedTask);
                });
    }
}
```

### チャレンジ 3: 負荷テスト

JMeterやGatlingで負荷テストを実施します。

**JMeterテストプラン作成**:

**ファイルパス**: `load-test/taskmanagement-load-test.jmx`（GUI で作成するか、以下のスクリプトで生成）

**Gatlingを使った負荷テスト**:

**依存関係追加** (`pom.xml`):

```xml
<dependency>
    <groupId>io.gatling.highcharts</groupId>
    <artifactId>gatling-charts-highcharts</artifactId>
    <version>3.9.5</version>
    <scope>test</scope>
</dependency>
```

**ファイルパス**: `src/test/scala/LoadTestSimulation.scala`

```scala
package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class TaskManagementLoadTest extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  // ログインしてJWTトークンを取得
  val loginScenario = scenario("Login and Get Token")
    .exec(http("Login")
      .post("/api/auth/login")
      .body(StringBody("""{"username":"testuser","password":"password"}"""))
      .check(jsonPath("$.token").saveAs("token")))

  // タスク作成
  val createTaskScenario = scenario("Create Tasks")
    .exec(session => {
      val token = session("token").as[String]
      session.set("authHeader", s"Bearer $token")
    })
    .repeat(10) {
      exec(http("Create Task")
        .post("/api/tasks")
        .header("Authorization", "${authHeader}")
        .body(StringBody("""{
          "projectId": 1,
          "title": "Load Test Task ${__RandomString(10,abcdefghijklmnopqrstuvwxyz)}",
          "description": "This is a load test task",
          "priority": "MEDIUM",
          "dueDate": "2025-12-31"
        }"""))
        .check(status.is(201)))
        .pause(1)
    }

  // タスク一覧取得
  val listTasksScenario = scenario("List Tasks")
    .exec(session => {
      val token = session("token").as[String]
      session.set("authHeader", s"Bearer $token")
    })
    .repeat(20) {
      exec(http("Get Tasks")
        .get("/api/tasks")
        .header("Authorization", "${authHeader}")
        .queryParam("page", "0")
        .queryParam("size", "20")
        .check(status.is(200)))
        .pause(2)
    }

  // 負荷テストシナリオ
  setUp(
    loginScenario.inject(atOnceUsers(1)),
    createTaskScenario.inject(
      rampUsers(50) during (30 seconds)
    ).protocols(httpProtocol),
    listTasksScenario.inject(
      constantUsersPerSec(10) during (60 seconds)
    ).protocols(httpProtocol)
  ).assertions(
    global.responseTime.max.lt(3000),
    global.successfulRequests.percent.gt(95)
  )
}
```

**負荷テスト実行**:

```bash
# Gatlingテスト実行
mvn gatling:test -Dgatling.simulationClass=simulations.TaskManagementLoadTest

# レポート確認
open target/gatling/taskmanagemementloadtest-*/index.html
```

**JMeterでのテスト**:

```bash
# JMeterのインストール
brew install jmeter  # macOS
# または https://jmeter.apache.org/download_jmeter.cgi からダウンロード

# GUI起動
jmeter

# CLIでテスト実行
jmeter -n -t load-test/taskmanagement-load-test.jmx -l results.jtl -e -o report/
```

**シンプルなシェルスクリプトでの負荷テスト**:

**ファイルパス**: `load-test/simple-load-test.sh`

```bash
#!/bin/bash

# 設定
BASE_URL="http://localhost:8080"
CONCURRENT_REQUESTS=50
TOTAL_REQUESTS=1000

# ログイン
TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password"}' \
  | jq -r '.token')

echo "Token: $TOKEN"

# タスク作成の並列実行
create_task() {
  curl -s -X POST "$BASE_URL/api/tasks" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
      "projectId": 1,
      "title": "Load Test Task",
      "description": "This is a load test",
      "priority": "MEDIUM",
      "dueDate": "2025-12-31"
    }' > /dev/null
}

export -f create_task
export TOKEN BASE_URL

echo "Starting load test..."
start_time=$(date +%s)

# GNU parallelを使用（インストール: brew install parallel）
seq 1 $TOTAL_REQUESTS | parallel -j $CONCURRENT_REQUESTS create_task

end_time=$(date +%s)
duration=$((end_time - start_time))

echo "Load test completed!"
echo "Total requests: $TOTAL_REQUESTS"
echo "Concurrent requests: $CONCURRENT_REQUESTS"
echo "Duration: ${duration}s"
echo "Throughput: $((TOTAL_REQUESTS / duration)) req/s"
```

**パフォーマンス分析用のSQL**:

```sql
-- 遅いクエリの確認
SELECT * FROM mysql.slow_log 
ORDER BY query_time DESC 
LIMIT 10;

-- インデックスの使用状況
SHOW INDEX FROM tasks;

-- テーブルサイズ確認
SELECT 
    table_name,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'Size (MB)'
FROM information_schema.TABLES
WHERE table_schema = 'taskmanagement'
ORDER BY (data_length + index_length) DESC;
```

---

## 🎓 Phase 6完了おめでとうございます！

これで、タスク管理システムの開発が完了しました！

### 実装した機能

- ✅ ユーザー認証・認可（JWT）
- ✅ プロジェクト管理
- ✅ タスク管理（CRUD、検索、フィルタ）
- ✅ コメント機能
- ✅ タグ機能
- ✅ ファイル添付
- ✅ メール通知
- ✅ リマインダー
- ✅ 統計・ダッシュボード
- ✅ キャッシング（Redis）
- ✅ 非同期処理
- ✅ テスト（ユニット・統合）
- ✅ API仕様書（Swagger）
- ✅ Dockerコンテナ化
- ✅ CI/CD
