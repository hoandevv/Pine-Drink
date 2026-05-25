# Plan 10 — DevOps, Testing & Deployment

## Mục tiêu
Testing (unit, integration), CI/CD pipeline với GitHub Actions, Docker hóa ứng dụng, và triển khai lên production server.

## Files cần tạo / sửa

| File | Mô tả |
|------|-------|
| `Dockerfile` | Multi-stage build cho Spring Boot app |
| `docker-compose.prod.yml` | Production Docker Compose |
| `.github/workflows/ci.yml` | GitHub Actions CI/CD pipeline |
| `src/test/...` | Unit + Integration tests |

## Chi tiết implementation

### 1. Dockerfile

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:resolve -q

COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2: Run
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 2. docker-compose.prod.yml

```yaml
version: '3.8'

networks:
  pine-network:
    driver: bridge

volumes:
  mysql-data:
  redis-data:
  rabbitmq-data:
  minio-data:

services:
  mysql:
    image: mysql:8.0
    container_name: pine-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: pine_drink
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
    networks:
      - pine-network
    healthcheck:
      test: [ "CMD", "mysqladmin", "ping", "-h", "localhost" ]
      interval: 10s
      timeout: 5s
      retries: 5
    deploy:
      resources:
        limits:
          memory: 512M

  redis:
    image: redis:7-alpine
    container_name: pine-redis
    restart: unless-stopped
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    networks:
      - pine-network
    healthcheck:
      test: [ "CMD", "redis-cli", "ping" ]
      interval: 10s
      timeout: 3s
      retries: 5
    deploy:
      resources:
        limits:
          memory: 256M

  rabbitmq:
    image: rabbitmq:3-management-alpine
    container_name: pine-rabbitmq
    restart: unless-stopped
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASS}
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - rabbitmq-data:/var/lib/rabbitmq
    networks:
      - pine-network
    healthcheck:
      test: [ "CMD", "rabbitmq-diagnostics", "check_port_connectivity" ]
      interval: 15s
      timeout: 5s
      retries: 5
    deploy:
      resources:
        limits:
          memory: 256M

  minio:
    image: minio/minio:latest
    container_name: pine-minio
    restart: unless-stopped
    environment:
      MINIO_ROOT_USER: ${MINIO_ROOT_USER}
      MINIO_ROOT_PASSWORD: ${MINIO_ROOT_PASSWORD}
    command: server /data --console-address ":9001"
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio-data:/data
    networks:
      - pine-network
    healthcheck:
      test: [ "CMD", "curl", "-f", "http://localhost:9000/minio/health/live" ]
      interval: 15s
      timeout: 5s
      retries: 5
    deploy:
      resources:
        limits:
          memory: 256M

  app:
    build:
      context: ..
      dockerfile: Dockerfile
    container_name: pine-app
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/pine_drink?useSSL=false&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      SPRING_REDIS_HOST: redis
      SPRING_RABBITMQ_HOST: rabbitmq
      MINIO_ENDPOINT: http://minio:9000
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      minio:
        condition: service_healthy
    networks:
      - pine-network
    deploy:
      resources:
        limits:
          memory: 512M
```

### 3. CI/CD — GitHub Actions

```yaml
# .github/workflows/ci.yml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: testpass
          MYSQL_DATABASE: pine_drink_test
        ports:
          - 3306:3306
        options: --health-cmd="mysqladmin ping" --health-interval=10s --health-timeout=5s --health-retries=5
      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379
        options: --health-cmd="redis-cli ping" --health-interval=10s --health-timeout=5s --health-retries=5

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Run tests
        run: mvn verify -Pintegration-test
        env:
          SPRING_DATASOURCE_URL: jdbc:mysql://localhost:3306/pine_drink_test?useSSL=false&allowPublicKeyRetrieval=true
          SPRING_DATASOURCE_USERNAME: root
          SPRING_DATASOURCE_PASSWORD: testpass
          SPRING_REDIS_HOST: localhost

      - name: Upload test reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-reports
          path: target/surefire-reports/

  build-and-push:
    needs: test
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Build with Maven
        run: mvn package -DskipTests

      - name: Log in to GHCR
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push Docker image
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: |
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:latest
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}

  deploy:
    needs: build-and-push
    runs-on: ubuntu-latest
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'

    steps:
      - name: Deploy to server
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.DEPLOY_HOST }}
          username: ${{ secrets.DEPLOY_USER }}
          key: ${{ secrets.DEPLOY_SSH_KEY }}
          script: |
            cd /opt/pine-drink
            docker compose -f docker-compose.prod.yml pull
            docker compose -f docker-compose.prod.yml up -d --force-recreate
            docker image prune -f
```

### 4. Testing Strategy

#### Unit Test — Service Layer (JUnit 5 + Mockito)

```java
@ExtendWith(MockitoExtension.class)
class StaffOrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderStatusHistoryRepository statusHistoryRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private StaffOrderServiceImpl staffOrderService;

    @Test
    void confirmOrder_shouldChangeStatusToConfirmed() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.PENDING)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        OrderResponse response = staffOrderService.confirmOrder(orderId);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.CONFIRMED));
        verify(statusHistoryRepository).save(any());
        verify(messagingTemplate, times(3)).convertAndSend(anyString(), any());
    }

    @Test
    void confirmOrder_shouldThrowIfNotPending() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.COMPLETED)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class,
            () -> staffOrderService.confirmOrder(orderId));
    }
}
```

#### Integration Test — Repository Layer (Testcontainers)

```java
@SpringBootTest
@Testcontainers
class OrderRepositoryTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private OrderRepository orderRepository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Test
    void shouldSaveAndFindOrder() {
        Order order = Order.builder()
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(100))
                .build();

        Order saved = orderRepository.save(order);

        Optional<Order> found = orderRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(OrderStatus.PENDING);
    }
}
```

#### Controller Test — @WebMvcTest + MockMvc

```java
@WebMvcTest(StaffOrderController.class)
class StaffOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StaffOrderService staffOrderService;

    @Test
    void confirmOrder_shouldReturn200() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(staffOrderService.confirmOrder(orderId))
            .thenReturn(OrderResponse.builder().id(orderId).status(OrderStatus.CONFIRMED).build());

        mockMvc.perform(patch("/staff/orders/{id}/confirm", orderId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }
}
```

## Deployment Options

### VPS — Docker Compose (single server)
- Ubuntu 22.04 / 24.04
- Docker + Docker Compose
- Nginx reverse proxy + Let's Encrypt SSL

```nginx
# /etc/nginx/sites-available/pine-drink
server {
    listen 443 ssl;
    server_name pine-drink.example.com;

    ssl_certificate /etc/letsencrypt/live/pine-drink.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/pine-drink.example.com/privkey.pem;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket support
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

## Monitoring

- **Spring Boot Actuator**: `/actuator/health`, `/actuator/metrics`, `/actuator/info`
- **Prometheus + Grafana**: Optional, expose `/actuator/prometheus`
- **Log aggregation**: ELK stack (optional) — Filebeat → Logstash → Elasticsearch → Kibana

## Checklist

- [ ] Tạo Dockerfile với multi-stage build
- [ ] Tạo docker-compose.prod.yml với healthcheck + resource limits
- [ ] Tạo .env.example cho production variables
- [ ] Cấu hình Spring Boot Actuator
- [ ] Viết unit test cho Service layer (JUnit 5 + Mockito)
- [ ] Viết integration test với Testcontainers (MySQL, Redis)
- [ ] Viết controller test với @WebMvcTest + MockMvc
- [ ] Tạo CI workflow (.github/workflows/ci.yml)
- [ ] Build → test → Docker image → push GHCR
- [ ] Deploy step với SSH action
- [ ] Cấu hình Nginx reverse proxy
- [ ] Let's Encrypt SSL certificate
- [ ] WebSocket proxy config trong Nginx
- [ ] (Optional) Prometheus + Grafana monitoring
- [ ] (Optional) ELK stack log aggregation
