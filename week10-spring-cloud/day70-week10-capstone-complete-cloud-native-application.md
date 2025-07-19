# Day 70: Week 10 Capstone - Complete Cloud-Native E-commerce Application

## Overview
Today we'll integrate all Week 10 Spring Cloud components to build a complete cloud-native e-commerce application. This capstone project demonstrates real-world implementation of microservices patterns, configuration management, service discovery, API gateway, inter-service communication, distributed tracing, and event-driven architecture.

## Learning Objectives
- Integrate all Spring Cloud components into a cohesive system
- Implement comprehensive microservices architecture
- Configure centralized configuration management
- Set up service discovery and load balancing
- Implement API gateway with security and monitoring
- Create resilient inter-service communication
- Add distributed tracing and monitoring
- Build event-driven workflows with messaging

## 1. System Architecture Overview

### Architecture Components

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              Cloud-Native E-commerce Platform                    │
├─────────────────────────────────────────────────────────────────────────────────┤
│  Frontend (React/Angular)                                                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│  API Gateway (Spring Cloud Gateway)                                             │
│  - Rate Limiting, Security, Routing, Circuit Breaker                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│  Microservices                                                                  │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ │
│  │   Order     │ │  Inventory  │ │   Payment   │ │ Notification│ │  Analytics  │ │
│  │   Service   │ │   Service   │ │   Service   │ │   Service   │ │   Service   │ │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ │
├─────────────────────────────────────────────────────────────────────────────────┤
│  Infrastructure Services                                                        │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐               │
│  │   Config    │ │   Eureka    │ │   Zipkin    │ │   Message   │               │
│  │   Server    │ │   Server    │ │   Server    │ │   Broker    │               │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘               │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Technology Stack

- **Configuration**: Spring Cloud Config Server
- **Service Discovery**: Netflix Eureka
- **API Gateway**: Spring Cloud Gateway
- **Communication**: OpenFeign, RestTemplate
- **Tracing**: Spring Cloud Sleuth + Zipkin
- **Messaging**: Spring Cloud Stream + RabbitMQ
- **Security**: Spring Security + JWT
- **Monitoring**: Spring Boot Actuator + Micrometer

## 2. Infrastructure Setup

### Docker Compose for Infrastructure

```yaml
# docker-compose.yml
version: '3.8'

services:
  # Configuration Server
  config-server:
    image: openjdk:17-jdk-slim
    container_name: config-server
    ports:
      - "8888:8888"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    volumes:
      - ./config-server:/app
      - ./config-repo:/config-repo
    command: ["java", "-jar", "/app/config-server.jar"]
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8888/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Service Registry
  eureka-server:
    image: openjdk:17-jdk-slim
    container_name: eureka-server
    ports:
      - "8761:8761"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:
      - config-server
    volumes:
      - ./eureka-server:/app
    command: ["java", "-jar", "/app/eureka-server.jar"]
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8761/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  # API Gateway
  api-gateway:
    image: openjdk:17-jdk-slim
    container_name: api-gateway
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:
      - config-server
      - eureka-server
    volumes:
      - ./api-gateway:/app
    command: ["java", "-jar", "/app/api-gateway.jar"]
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Zipkin Tracing
  zipkin:
    image: openzipkin/zipkin
    container_name: zipkin
    ports:
      - "9411:9411"
    environment:
      - STORAGE_TYPE=mem

  # RabbitMQ Message Broker
  rabbitmq:
    image: rabbitmq:3-management
    container_name: rabbitmq
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      - RABBITMQ_DEFAULT_USER=admin
      - RABBITMQ_DEFAULT_PASS=admin
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq

  # PostgreSQL Database
  postgres:
    image: postgres:13
    container_name: postgres
    ports:
      - "5432:5432"
    environment:
      - POSTGRES_DB=ecommerce
      - POSTGRES_USER=admin
      - POSTGRES_PASSWORD=admin
    volumes:
      - postgres_data:/var/lib/postgresql/data

  # Redis Cache
  redis:
    image: redis:6-alpine
    container_name: redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

volumes:
  rabbitmq_data:
  postgres_data:
  redis_data:
```

## 3. Configuration Server Setup

### Config Server Application

```java
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

### Config Server Configuration

```yaml
# config-server/application.yml
server:
  port: 8888

spring:
  application:
    name: config-server
  profiles:
    active: native
  cloud:
    config:
      server:
        native:
          search-locations: file:///config-repo
        git:
          uri: https://github.com/your-org/config-repo
          clone-on-start: true
          timeout: 5
          
management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always
```

### Shared Configuration Files

```yaml
# config-repo/application.yml
spring:
  sleuth:
    zipkin:
      base-url: http://zipkin:9411
    sampler:
      probability: 1.0
  
  rabbitmq:
    host: rabbitmq
    port: 5672
    username: admin
    password: admin
  
  datasource:
    url: jdbc:postgresql://postgres:5432/ecommerce
    username: admin
    password: admin
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    database-platform: org.hibernate.dialect.PostgreSQLDialect
  
  redis:
    host: redis
    port: 6379
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0

eureka:
  client:
    service-url:
      defaultZone: http://eureka-server:8761/eureka/
    fetch-registry: true
    register-with-eureka: true

management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true

logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
  level:
    org.springframework.cloud.sleuth: DEBUG
```

## 4. Service Registry (Eureka Server)

### Eureka Server Application

```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

### Eureka Server Configuration

```yaml
# eureka-server/application.yml
server:
  port: 8761

spring:
  application:
    name: eureka-server
  cloud:
    config:
      uri: http://config-server:8888
      fail-fast: true
      retry:
        initial-interval: 1000
        max-attempts: 6
        multiplier: 1.1

eureka:
  instance:
    hostname: eureka-server
  client:
    register-with-eureka: false
    fetch-registry: false
    service-url:
      defaultZone: http://eureka-server:8761/eureka/
  server:
    enable-self-preservation: false
    eviction-interval-timer-in-ms: 5000
```

## 5. API Gateway Implementation

### API Gateway Application

```java
@SpringBootApplication
@EnableEurekaClient
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
```

### Gateway Routes Configuration

```yaml
# config-repo/api-gateway.yml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      routes:
        # Order Service Routes
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**
          filters:
            - name: CircuitBreaker
              args:
                name: order-service-cb
                fallbackUri: forward:/fallback/orders
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenish-rate: 10
                redis-rate-limiter.burst-capacity: 20
                redis-rate-limiter.requested-tokens: 1
            - name: Retry
              args:
                retries: 3
                methods: GET,POST
                backoff:
                  firstBackoff: 50ms
                  maxBackoff: 500ms
        
        # Inventory Service Routes
        - id: inventory-service
          uri: lb://inventory-service
          predicates:
            - Path=/api/inventory/**
          filters:
            - name: CircuitBreaker
              args:
                name: inventory-service-cb
                fallbackUri: forward:/fallback/inventory
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenish-rate: 15
                redis-rate-limiter.burst-capacity: 30
        
        # Payment Service Routes
        - id: payment-service
          uri: lb://payment-service
          predicates:
            - Path=/api/payments/**
          filters:
            - name: CircuitBreaker
              args:
                name: payment-service-cb
                fallbackUri: forward:/fallback/payments
        
        # Notification Service Routes
        - id: notification-service
          uri: lb://notification-service
          predicates:
            - Path=/api/notifications/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenish-rate: 5
                redis-rate-limiter.burst-capacity: 10
        
        # Analytics Service Routes
        - id: analytics-service
          uri: lb://analytics-service
          predicates:
            - Path=/api/analytics/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenish-rate: 20
                redis-rate-limiter.burst-capacity: 40
      
      default-filters:
        - name: GlobalRequestResponseLogging
        - name: AddRequestHeader
          args:
            name: X-Request-ID
            value: "#{T(java.util.UUID).randomUUID().toString()}"
        - name: AddResponseHeader
          args:
            name: X-Response-Time
            value: "#{T(java.lang.System).currentTimeMillis()}"
      
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins: "*"
            allowedMethods: "*"
            allowedHeaders: "*"
            allowCredentials: true

resilience4j:
  circuitbreaker:
    instances:
      order-service-cb:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 3
        minimumNumberOfCalls: 5
      inventory-service-cb:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
      payment-service-cb:
        slidingWindowSize: 10
        failureRateThreshold: 60
        waitDurationInOpenState: 45s
```

### Gateway Filter Configuration

```java
@Component
public class GlobalRequestResponseLoggingFilter implements GlobalFilter, Ordered {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalRequestResponseLoggingFilter.class);
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        logger.info("Request: {} {}", request.getMethod(), request.getURI());
        logger.info("Headers: {}", request.getHeaders());
        
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            logger.info("Response Status: {}", response.getStatusCode());
            logger.info("Response Headers: {}", response.getHeaders());
        }));
    }
    
    @Override
    public int getOrder() {
        return -1; // High priority
    }
}

@RestController
public class FallbackController {
    
    @GetMapping("/fallback/orders")
    public ResponseEntity<Map<String, Object>> orderFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Order service is temporarily unavailable");
        response.put("status", "SERVICE_UNAVAILABLE");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(503).body(response);
    }
    
    @GetMapping("/fallback/inventory")
    public ResponseEntity<Map<String, Object>> inventoryFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Inventory service is temporarily unavailable");
        response.put("status", "SERVICE_UNAVAILABLE");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(503).body(response);
    }
    
    @GetMapping("/fallback/payments")
    public ResponseEntity<Map<String, Object>> paymentFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Payment service is temporarily unavailable");
        response.put("status", "SERVICE_UNAVAILABLE");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(503).body(response);
    }
}
```

## 6. Order Service Implementation

### Order Service Application

```java
@SpringBootApplication
@EnableEurekaClient
@EnableFeignClients
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

### Order Entity

```java
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(name = "status", nullable = false)
    private String status;
    
    @Column(name = "reservation_id")
    private String reservationId;
    
    @Column(name = "payment_id")
    private String paymentId;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors, getters, and setters
    public Order() {}
    
    public Order(Long customerId, Long productId, Integer quantity, BigDecimal totalAmount, String status) {
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.status = status;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

### Order Service Implementation

```java
@Service
@Transactional
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final OrderEventPublisher eventPublisher;
    private final EventSourcingService eventSourcingService;
    
    public OrderService(OrderRepository orderRepository,
                       InventoryClient inventoryClient,
                       PaymentClient paymentClient,
                       OrderEventPublisher eventPublisher,
                       EventSourcingService eventSourcingService) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
        this.eventPublisher = eventPublisher;
        this.eventSourcingService = eventSourcingService;
    }
    
    public Order createOrder(CreateOrderRequest request) {
        try {
            // 1. Validate request
            validateOrderRequest(request);
            
            // 2. Check inventory availability
            InventoryResponse inventory = inventoryClient.checkInventory(request.getProductId());
            if (inventory.getAvailableQuantity() < request.getQuantity()) {
                throw new InsufficientInventoryException("Insufficient inventory for product: " + request.getProductId());
            }
            
            // 3. Reserve inventory
            ReservationRequest reservationRequest = new ReservationRequest(
                request.getProductId(),
                request.getQuantity(),
                UUID.randomUUID().toString()
            );
            ReservationResponse reservation = inventoryClient.reserveInventory(reservationRequest);
            
            if (!reservation.isSuccess()) {
                throw new InventoryReservationException("Failed to reserve inventory: " + reservation.getMessage());
            }
            
            // 4. Process payment
            PaymentRequest paymentRequest = new PaymentRequest(
                request.getCustomerId(),
                inventory.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())),
                request.getPaymentMethod()
            );
            PaymentResponse payment = paymentClient.processPayment(paymentRequest);
            
            if (!payment.isSuccess()) {
                // Compensate - release inventory
                inventoryClient.releaseInventory(new ReleaseInventoryRequest(reservation.getReservationId()));
                throw new PaymentException("Payment failed: " + payment.getMessage());
            }
            
            // 5. Create order
            Order order = new Order(
                request.getCustomerId(),
                request.getProductId(),
                request.getQuantity(),
                inventory.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())),
                "CONFIRMED"
            );
            order.setReservationId(reservation.getReservationId());
            order.setPaymentId(payment.getPaymentId());
            
            Order savedOrder = orderRepository.save(order);
            
            // 6. Publish event
            OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId().toString(),
                savedOrder.getCustomerId(),
                savedOrder.getProductId(),
                savedOrder.getQuantity(),
                savedOrder.getTotalAmount(),
                savedOrder.getStatus(),
                savedOrder.getCreatedAt(),
                savedOrder.getReservationId(),
                savedOrder.getPaymentId()
            );
            
            // Save event to event store
            eventSourcingService.saveEvent(savedOrder.getId().toString(), "Order", event);
            
            // Publish to message broker
            eventPublisher.publishOrderCreated(savedOrder);
            
            return savedOrder;
            
        } catch (Exception e) {
            throw new OrderCreationException("Failed to create order: " + e.getMessage(), e);
        }
    }
    
    @Cacheable(value = "orders", key = "#orderId")
    public Optional<Order> getOrder(Long orderId) {
        return orderRepository.findById(orderId);
    }
    
    public List<Order> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }
    
    public Order updateOrderStatus(Long orderId, String newStatus, String reason) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        
        String oldStatus = order.getStatus();
        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);
        
        // Publish status update event
        OrderStatusUpdatedEvent event = new OrderStatusUpdatedEvent(
            orderId.toString(),
            oldStatus,
            newStatus,
            LocalDateTime.now(),
            reason
        );
        
        eventSourcingService.saveEvent(orderId.toString(), "Order", event);
        eventPublisher.publishOrderStatusUpdated(orderId.toString(), oldStatus, newStatus, reason);
        
        return updatedOrder;
    }
    
    private void validateOrderRequest(CreateOrderRequest request) {
        if (request.getCustomerId() == null) {
            throw new InvalidOrderException("Customer ID is required");
        }
        if (request.getProductId() == null) {
            throw new InvalidOrderException("Product ID is required");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new InvalidOrderException("Quantity must be greater than 0");
        }
        if (request.getPaymentMethod() == null) {
            throw new InvalidOrderException("Payment method is required");
        }
    }
}
```

### Order Controller

```java
@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {
    
    private final OrderService orderService;
    
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    @PostMapping
    public ResponseEntity<Order> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Order order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable Long orderId) {
        return orderService.getOrder(orderId)
            .map(order -> ResponseEntity.ok(order))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Order>> getOrdersByCustomer(@PathVariable Long customerId) {
        List<Order> orders = orderService.getOrdersByCustomer(customerId);
        return ResponseEntity.ok(orders);
    }
    
    @PutMapping("/{orderId}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody UpdateOrderStatusRequest request) {
        Order order = orderService.updateOrderStatus(orderId, request.getStatus(), request.getReason());
        return ResponseEntity.ok(order);
    }
}
```

## 7. Inventory Service Implementation

### Inventory Service Application

```java
@SpringBootApplication
@EnableEurekaClient
public class InventoryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
```

### Inventory Entity

```java
@Entity
@Table(name = "inventory")
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;
    
    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;
    
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;
    
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(name = "status", nullable = false)
    private String status;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors, getters, and setters
    public Inventory() {}
    
    public Inventory(Long productId, Integer availableQuantity, BigDecimal price, String status) {
        this.productId = productId;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = 0;
        this.price = price;
        this.status = status;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    
    public Integer getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }
    
    public Integer getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(Integer reservedQuantity) { this.reservedQuantity = reservedQuantity; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

### Inventory Service Implementation

```java
@Service
@Transactional
public class InventoryService {
    
    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final InventoryEventPublisher eventPublisher;
    
    public InventoryService(InventoryRepository inventoryRepository,
                           ReservationRepository reservationRepository,
                           InventoryEventPublisher eventPublisher) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.eventPublisher = eventPublisher;
    }
    
    @Cacheable(value = "inventory", key = "#productId")
    public InventoryResponse checkInventory(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productId));
        
        return new InventoryResponse(
            inventory.getProductId(),
            inventory.getAvailableQuantity(),
            inventory.getStatus(),
            inventory.getPrice()
        );
    }
    
    @CacheEvict(value = "inventory", key = "#request.productId")
    public ReservationResponse reserveInventory(ReservationRequest request) {
        Inventory inventory = inventoryRepository.findByProductId(request.getProductId())
            .orElseThrow(() -> new ProductNotFoundException("Product not found: " + request.getProductId()));
        
        if (inventory.getAvailableQuantity() < request.getQuantity()) {
            return new ReservationResponse(null, false, "Insufficient inventory");
        }
        
        // Update inventory
        Integer previousQuantity = inventory.getAvailableQuantity();
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() - request.getQuantity());
        inventory.setReservedQuantity(inventory.getReservedQuantity() + request.getQuantity());
        inventoryRepository.save(inventory);
        
        // Create reservation record
        Reservation reservation = new Reservation(
            UUID.randomUUID().toString(),
            request.getProductId(),
            request.getQuantity(),
            request.getOrderId(),
            "RESERVED"
        );
        reservationRepository.save(reservation);
        
        // Publish events
        eventPublisher.publishInventoryReserved(
            reservation.getReservationId(),
            request.getProductId(),
            request.getQuantity(),
            request.getOrderId()
        );
        
        eventPublisher.publishInventoryUpdated(
            request.getProductId(),
            previousQuantity,
            inventory.getAvailableQuantity(),
            "RESERVED"
        );
        
        return new ReservationResponse(
            reservation.getReservationId(),
            true,
            "Inventory reserved successfully"
        );
    }
    
    @CacheEvict(value = "inventory", key = "#reservation.productId")
    public void releaseInventory(ReleaseInventoryRequest request) {
        Reservation reservation = reservationRepository.findByReservationId(request.getReservationId())
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found: " + request.getReservationId()));
        
        Inventory inventory = inventoryRepository.findByProductId(reservation.getProductId())
            .orElseThrow(() -> new ProductNotFoundException("Product not found: " + reservation.getProductId()));
        
        // Update inventory
        Integer previousQuantity = inventory.getAvailableQuantity();
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() + reservation.getQuantity());
        inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
        inventoryRepository.save(inventory);
        
        // Update reservation
        reservation.setStatus("RELEASED");
        reservationRepository.save(reservation);
        
        // Publish event
        eventPublisher.publishInventoryUpdated(
            reservation.getProductId(),
            previousQuantity,
            inventory.getAvailableQuantity(),
            "RELEASED"
        );
    }
    
    public List<Inventory> getAllInventory() {
        return inventoryRepository.findAll();
    }
    
    public List<Inventory> getLowStockProducts(Integer threshold) {
        return inventoryRepository.findByAvailableQuantityLessThan(threshold);
    }
}
```

## 8. Event-Driven Communication

### Event Consumer for Cross-Service Communication

```java
@Component
public class OrderEventConsumer {
    
    private final NotificationService notificationService;
    private final AnalyticsService analyticsService;
    private final InventoryService inventoryService;
    
    public OrderEventConsumer(NotificationService notificationService,
                             AnalyticsService analyticsService,
                             InventoryService inventoryService) {
        this.notificationService = notificationService;
        this.analyticsService = analyticsService;
        this.inventoryService = inventoryService;
    }
    
    @Bean
    public Consumer<Message<OrderCreatedEvent>> orderCreatedConsumer() {
        return message -> {
            OrderCreatedEvent event = message.getPayload();
            
            try {
                // Send notifications
                notificationService.sendOrderConfirmationEmail(event);
                notificationService.sendOrderConfirmationSMS(event);
                
                // Update analytics
                analyticsService.recordOrderCreated(event);
                
                // Check inventory levels
                inventoryService.checkLowStockAlert(event.getProductId());
                
                System.out.println("Order created event processed: " + event.getOrderId());
                
            } catch (Exception e) {
                System.err.println("Error processing order created event: " + e.getMessage());
                throw new RuntimeException("Failed to process order created event", e);
            }
        };
    }
    
    @Bean
    public Consumer<Message<OrderStatusUpdatedEvent>> orderStatusUpdatedConsumer() {
        return message -> {
            OrderStatusUpdatedEvent event = message.getPayload();
            
            try {
                // Send status update notifications
                notificationService.sendOrderStatusUpdateEmail(event);
                
                // Update analytics
                analyticsService.recordOrderStatusChange(event);
                
                System.out.println("Order status updated event processed: " + event.getOrderId());
                
            } catch (Exception e) {
                System.err.println("Error processing order status updated event: " + e.getMessage());
                throw new RuntimeException("Failed to process order status updated event", e);
            }
        };
    }
}
```

## 9. Monitoring and Health Checks

### Health Check Configuration

```java
@Component
public class SystemHealthIndicator implements HealthIndicator {
    
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final DataSource dataSource;
    
    public SystemHealthIndicator(InventoryClient inventoryClient,
                                PaymentClient paymentClient,
                                DataSource dataSource) {
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
        this.dataSource = dataSource;
    }
    
    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        
        // Check database connectivity
        try {
            try (Connection connection = dataSource.getConnection()) {
                builder.withDetail("database", "Connected");
            }
        } catch (Exception e) {
            builder.down().withDetail("database", "Connection failed: " + e.getMessage());
        }
        
        // Check external service connectivity
        try {
            inventoryClient.healthCheck();
            builder.withDetail("inventory-service", "Available");
        } catch (Exception e) {
            builder.withDetail("inventory-service", "Unavailable: " + e.getMessage());
        }
        
        try {
            paymentClient.healthCheck();
            builder.withDetail("payment-service", "Available");
        } catch (Exception e) {
            builder.withDetail("payment-service", "Unavailable: " + e.getMessage());
        }
        
        return builder.build();
    }
}
```

### Metrics Configuration

```java
@Configuration
public class MetricsConfiguration {
    
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
    
    @Bean
    public CountedAspect countedAspect(MeterRegistry registry) {
        return new CountedAspect(registry);
    }
    
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags("application", "ecommerce-platform");
    }
}
```

## 10. Testing the Complete System

### Integration Test

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class EcommerceSystemIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("test_ecommerce")
            .withUsername("test")
            .withPassword("test");
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:6-alpine")
            .withExposedPorts(6379);
    
    @Container
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3-management")
            .withUser("test", "test");
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private OrderService orderService;
    
    @MockBean
    private InventoryClient inventoryClient;
    
    @MockBean
    private PaymentClient paymentClient;
    
    @Test
    void testCompleteOrderFlow() {
        // Given
        given(inventoryClient.checkInventory(1L))
                .willReturn(new InventoryResponse(1L, 100, "AVAILABLE", new BigDecimal("29.99")));
        
        given(inventoryClient.reserveInventory(any()))
                .willReturn(new ReservationResponse("res-123", true, "Success"));
        
        given(paymentClient.processPayment(any()))
                .willReturn(new PaymentResponse("pay-123", true, "Success"));
        
        CreateOrderRequest request = new CreateOrderRequest(1L, 1L, 2, "CREDIT_CARD");
        
        // When
        ResponseEntity<Order> response = restTemplate.postForEntity("/api/orders", request, Order.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCustomerId()).isEqualTo(1L);
        assertThat(response.getBody().getProductId()).isEqualTo(1L);
        assertThat(response.getBody().getQuantity()).isEqualTo(2);
        assertThat(response.getBody().getStatus()).isEqualTo("CONFIRMED");
    }
}
```

## Summary

This capstone project demonstrates a complete cloud-native e-commerce application with:

1. **Configuration Management**: Centralized configuration with Spring Cloud Config
2. **Service Discovery**: Eureka server for service registration and discovery
3. **API Gateway**: Spring Cloud Gateway with routing, load balancing, and resilience
4. **Microservices**: Order, Inventory, Payment, Notification, and Analytics services
5. **Inter-Service Communication**: OpenFeign clients with circuit breakers
6. **Distributed Tracing**: Spring Cloud Sleuth and Zipkin integration
7. **Event-Driven Architecture**: Spring Cloud Stream with RabbitMQ
8. **Event Sourcing**: Complete audit trail of domain events
9. **Caching**: Redis for performance optimization
10. **Monitoring**: Comprehensive health checks and metrics

## Key Architecture Patterns Implemented

- **Microservices Architecture**: Domain-driven service decomposition
- **Event Sourcing**: Complete event history for audit and reconstruction
- **CQRS**: Separate read and write models
- **Saga Pattern**: Distributed transaction management
- **Circuit Breaker**: Resilience against service failures
- **API Gateway**: Single entry point with cross-cutting concerns
- **Service Discovery**: Dynamic service registration and lookup
- **Distributed Tracing**: Request flow visibility across services

## Production Considerations

1. **Security**: Implement OAuth2/JWT authentication and authorization
2. **Monitoring**: Add Prometheus, Grafana, and ELK stack
3. **Deployment**: Use Kubernetes with Helm charts
4. **Database**: Implement proper database per service
5. **CI/CD**: Automated testing and deployment pipelines
6. **Scaling**: Horizontal pod autoscaling and load testing
7. **Backup**: Database backup and disaster recovery strategies

This completes Week 10 of the Java learning journey, providing a comprehensive foundation in Spring Cloud and cloud-native application development.