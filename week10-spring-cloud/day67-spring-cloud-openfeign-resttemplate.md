# Day 67: Spring Cloud OpenFeign & RestTemplate - Service Communication

## Overview
Today we'll explore service-to-service communication in Spring Cloud applications using OpenFeign and RestTemplate. We'll cover declarative REST clients, load balancing, error handling, and performance optimization.

## Learning Objectives
- Understand Spring Cloud OpenFeign for declarative REST clients
- Master RestTemplate with service discovery
- Implement load balancing and circuit breakers
- Handle timeouts and error scenarios
- Optimize performance with caching and connection pooling

## 1. OpenFeign Introduction

OpenFeign is a declarative REST client that makes writing HTTP clients easier. It integrates seamlessly with Spring Cloud's service discovery and load balancing.

### Setting Up OpenFeign

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

### Enable OpenFeign

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

## 2. Creating Feign Clients

### Basic Feign Client

```java
@FeignClient(name = "inventory-service")
public interface InventoryClient {
    
    @GetMapping("/api/inventory/{productId}")
    InventoryResponse getInventory(@PathVariable("productId") Long productId);
    
    @PostMapping("/api/inventory/reserve")
    ReservationResponse reserveInventory(@RequestBody ReservationRequest request);
    
    @PutMapping("/api/inventory/release")
    void releaseInventory(@RequestBody ReleaseRequest request);
}
```

### DTOs for Communication

```java
public class InventoryResponse {
    private Long productId;
    private Integer availableQuantity;
    private String status;
    private BigDecimal price;
    
    // Constructors, getters, and setters
    public InventoryResponse() {}
    
    public InventoryResponse(Long productId, Integer availableQuantity, String status, BigDecimal price) {
        this.productId = productId;
        this.availableQuantity = availableQuantity;
        this.status = status;
        this.price = price;
    }
    
    // Getters and setters
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    
    public Integer getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}

public class ReservationRequest {
    private Long productId;
    private Integer quantity;
    private String orderId;
    
    // Constructors, getters, and setters
    public ReservationRequest() {}
    
    public ReservationRequest(Long productId, Integer quantity, String orderId) {
        this.productId = productId;
        this.quantity = quantity;
        this.orderId = orderId;
    }
    
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
}

public class ReservationResponse {
    private String reservationId;
    private boolean success;
    private String message;
    
    // Constructors, getters, and setters
    public ReservationResponse() {}
    
    public ReservationResponse(String reservationId, boolean success, String message) {
        this.reservationId = reservationId;
        this.success = success;
        this.message = message;
    }
    
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
```

## 3. Advanced Feign Configuration

### Custom Feign Configuration

```java
@Configuration
public class FeignConfig {
    
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
    
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(5000, 10000);
    }
    
    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();
    }
    
    @Bean
    public RequestInterceptor requestInterceptor() {
        return new AuthenticationInterceptor();
    }
}
```

### Custom Error Decoder

```java
public class CustomErrorDecoder implements ErrorDecoder {
    
    @Override
    public Exception decode(String methodKey, Response response) {
        switch (response.status()) {
            case 400:
                return new BadRequestException("Bad request");
            case 404:
                return new NotFoundException("Resource not found");
            case 500:
                return new InternalServerException("Internal server error");
            default:
                return new Exception("Generic error");
        }
    }
}
```

### Authentication Interceptor

```java
public class AuthenticationInterceptor implements RequestInterceptor {
    
    @Override
    public void apply(RequestTemplate template) {
        // Add authentication header
        template.header("Authorization", "Bearer " + getCurrentToken());
        
        // Add correlation ID
        template.header("X-Correlation-ID", UUID.randomUUID().toString());
    }
    
    private String getCurrentToken() {
        // Implementation to get current JWT token
        return JwtTokenProvider.getCurrentToken();
    }
}
```

## 4. Service Using Feign Client

```java
@Service
public class OrderService {
    
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final OrderRepository orderRepository;
    
    public OrderService(InventoryClient inventoryClient, 
                       PaymentClient paymentClient,
                       OrderRepository orderRepository) {
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
        this.orderRepository = orderRepository;
    }
    
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        try {
            // Check inventory
            InventoryResponse inventory = inventoryClient.getInventory(request.getProductId());
            
            if (inventory.getAvailableQuantity() < request.getQuantity()) {
                throw new InsufficientInventoryException("Not enough inventory");
            }
            
            // Reserve inventory
            ReservationRequest reservationRequest = new ReservationRequest(
                request.getProductId(), 
                request.getQuantity(), 
                UUID.randomUUID().toString()
            );
            
            ReservationResponse reservation = inventoryClient.reserveInventory(reservationRequest);
            
            if (!reservation.isSuccess()) {
                throw new ReservationFailedException("Failed to reserve inventory");
            }
            
            // Process payment
            PaymentRequest paymentRequest = new PaymentRequest(
                request.getCustomerId(),
                inventory.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())),
                request.getPaymentMethod()
            );
            
            PaymentResponse payment = paymentClient.processPayment(paymentRequest);
            
            if (!payment.isSuccess()) {
                // Release inventory
                inventoryClient.releaseInventory(new ReleaseRequest(reservation.getReservationId()));
                throw new PaymentFailedException("Payment failed");
            }
            
            // Create order
            Order order = new Order();
            order.setCustomerId(request.getCustomerId());
            order.setProductId(request.getProductId());
            order.setQuantity(request.getQuantity());
            order.setTotalAmount(inventory.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
            order.setStatus("CONFIRMED");
            order.setReservationId(reservation.getReservationId());
            order.setPaymentId(payment.getPaymentId());
            
            return orderRepository.save(order);
            
        } catch (Exception e) {
            throw new OrderCreationException("Failed to create order", e);
        }
    }
}
```

## 5. RestTemplate with Service Discovery

### RestTemplate Configuration

```java
@Configuration
public class RestTemplateConfig {
    
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // Configure timeout
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        
        restTemplate.setRequestFactory(factory);
        
        // Add interceptors
        restTemplate.getInterceptors().add(new LoggingInterceptor());
        restTemplate.getInterceptors().add(new AuthenticationInterceptor());
        
        return restTemplate;
    }
    
    @Bean
    public RestTemplate plainRestTemplate() {
        return new RestTemplate();
    }
}
```

### Service Using RestTemplate

```java
@Service
public class NotificationService {
    
    private final RestTemplate restTemplate;
    
    public NotificationService(@Qualifier("restTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public void sendOrderConfirmation(Order order) {
        String url = "http://notification-service/api/notifications/order-confirmation";
        
        NotificationRequest request = new NotificationRequest(
            order.getCustomerId(),
            "ORDER_CONFIRMATION",
            createOrderConfirmationMessage(order)
        );
        
        try {
            ResponseEntity<NotificationResponse> response = restTemplate.postForEntity(
                url, 
                request, 
                NotificationResponse.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Notification sent successfully");
            }
        } catch (Exception e) {
            System.err.println("Failed to send notification: " + e.getMessage());
        }
    }
    
    private String createOrderConfirmationMessage(Order order) {
        return String.format("Your order #%s has been confirmed. Total: $%.2f", 
                           order.getId(), order.getTotalAmount());
    }
}
```

## 6. Circuit Breaker Integration

### Feign with Circuit Breaker

```java
@FeignClient(name = "inventory-service", fallback = InventoryClientFallback.class)
public interface InventoryClient {
    
    @GetMapping("/api/inventory/{productId}")
    InventoryResponse getInventory(@PathVariable("productId") Long productId);
    
    @PostMapping("/api/inventory/reserve")
    ReservationResponse reserveInventory(@RequestBody ReservationRequest request);
}

@Component
public class InventoryClientFallback implements InventoryClient {
    
    @Override
    public InventoryResponse getInventory(Long productId) {
        return new InventoryResponse(productId, 0, "UNAVAILABLE", BigDecimal.ZERO);
    }
    
    @Override
    public ReservationResponse reserveInventory(ReservationRequest request) {
        return new ReservationResponse(null, false, "Service temporarily unavailable");
    }
}
```

### Circuit Breaker Configuration

```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      inventory-service:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
        minimumNumberOfCalls: 5
        automaticTransitionFromOpenToHalfOpenEnabled: true
  
  retry:
    instances:
      inventory-service:
        maxAttempts: 3
        waitDuration: 1s
        retryExceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
        ignoreExceptions:
          - java.lang.IllegalArgumentException
```

## 7. Performance Optimization

### Connection Pooling

```java
@Configuration
public class HttpClientConfig {
    
    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
    
    @Bean
    public CloseableHttpClient apacheHttpClient() {
        PoolingHttpClientConnectionManager connectionManager = 
                new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(200);
        connectionManager.setDefaultMaxPerRoute(20);
        
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setRetryHandler(new DefaultHttpRequestRetryHandler(3, true))
                .build();
    }
}
```

### Caching Configuration

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats());
        return cacheManager;
    }
}

@Service
public class ProductService {
    
    private final ProductClient productClient;
    
    public ProductService(ProductClient productClient) {
        this.productClient = productClient;
    }
    
    @Cacheable(value = "products", key = "#productId")
    public Product getProduct(Long productId) {
        return productClient.getProduct(productId);
    }
    
    @CacheEvict(value = "products", key = "#productId")
    public void evictProduct(Long productId) {
        // Method to evict cache
    }
}
```

## 8. Error Handling and Resilience

### Comprehensive Error Handling

```java
@Service
public class ResilientOrderService {
    
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    
    public ResilientOrderService(InventoryClient inventoryClient, PaymentClient paymentClient) {
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
    }
    
    @CircuitBreaker(name = "inventory-service", fallbackMethod = "fallbackGetInventory")
    @Retry(name = "inventory-service")
    @TimeLimiter(name = "inventory-service")
    public CompletableFuture<InventoryResponse> getInventoryAsync(Long productId) {
        return CompletableFuture.supplyAsync(() -> inventoryClient.getInventory(productId));
    }
    
    public CompletableFuture<InventoryResponse> fallbackGetInventory(Long productId, Exception ex) {
        return CompletableFuture.completedFuture(
            new InventoryResponse(productId, 0, "UNAVAILABLE", BigDecimal.ZERO)
        );
    }
    
    @Bulkhead(name = "payment-service", type = Bulkhead.Type.THREAD_POOL)
    public PaymentResponse processPayment(PaymentRequest request) {
        return paymentClient.processPayment(request);
    }
}
```

### Health Check Integration

```java
@Component
public class ExternalServiceHealthIndicator implements HealthIndicator {
    
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    
    public ExternalServiceHealthIndicator(InventoryClient inventoryClient, 
                                        PaymentClient paymentClient) {
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
    }
    
    @Override
    public Health health() {
        try {
            // Check inventory service
            inventoryClient.getInventory(1L);
            
            // Check payment service
            paymentClient.checkHealth();
            
            return Health.up()
                    .withDetail("inventory-service", "Available")
                    .withDetail("payment-service", "Available")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
```

## 9. Testing Feign Clients

### Unit Testing with WireMock

```java
@SpringBootTest
@AutoConfigureWireMock(port = 0)
class InventoryClientTest {
    
    @Autowired
    private InventoryClient inventoryClient;
    
    @Test
    void testGetInventory() {
        // Given
        stubFor(get(urlEqualTo("/api/inventory/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "productId": 1,
                                "availableQuantity": 100,
                                "status": "AVAILABLE",
                                "price": 29.99
                            }
                            """)));
        
        // When
        InventoryResponse response = inventoryClient.getInventory(1L);
        
        // Then
        assertThat(response.getProductId()).isEqualTo(1L);
        assertThat(response.getAvailableQuantity()).isEqualTo(100);
        assertThat(response.getStatus()).isEqualTo("AVAILABLE");
        assertThat(response.getPrice()).isEqualTo(new BigDecimal("29.99"));
    }
    
    @Test
    void testReserveInventory() {
        // Given
        stubFor(post(urlEqualTo("/api/inventory/reserve"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "reservationId": "res-123",
                                "success": true,
                                "message": "Inventory reserved successfully"
                            }
                            """)));
        
        // When
        ReservationRequest request = new ReservationRequest(1L, 5, "order-123");
        ReservationResponse response = inventoryClient.reserveInventory(request);
        
        // Then
        assertThat(response.getReservationId()).isEqualTo("res-123");
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Inventory reserved successfully");
    }
}
```

### Integration Testing

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "eureka.client.enabled=false"
})
class OrderServiceIntegrationTest {
    
    @Autowired
    private OrderService orderService;
    
    @MockBean
    private InventoryClient inventoryClient;
    
    @MockBean
    private PaymentClient paymentClient;
    
    @Test
    void testCreateOrder() {
        // Given
        given(inventoryClient.getInventory(1L))
                .willReturn(new InventoryResponse(1L, 100, "AVAILABLE", new BigDecimal("29.99")));
        
        given(inventoryClient.reserveInventory(any()))
                .willReturn(new ReservationResponse("res-123", true, "Success"));
        
        given(paymentClient.processPayment(any()))
                .willReturn(new PaymentResponse("pay-123", true, "Success"));
        
        CreateOrderRequest request = new CreateOrderRequest(1L, 1L, 2, "CREDIT_CARD");
        
        // When
        Order order = orderService.createOrder(request);
        
        // Then
        assertThat(order).isNotNull();
        assertThat(order.getProductId()).isEqualTo(1L);
        assertThat(order.getQuantity()).isEqualTo(2);
        assertThat(order.getStatus()).isEqualTo("CONFIRMED");
    }
}
```

## 10. Configuration and Best Practices

### Application Configuration

```yaml
# application.yml
spring:
  application:
    name: order-service
  
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 10000
        loggerLevel: basic
        decode404: false
      inventory-service:
        connectTimeout: 3000
        readTimeout: 8000
        loggerLevel: full
        errorDecoder: com.example.CustomErrorDecoder
        requestInterceptors:
          - com.example.AuthenticationInterceptor
  
  compression:
    request:
      enabled: true
      mime-types: application/json,application/xml
      min-request-size: 2048
    response:
      enabled: true
  
  hystrix:
    enabled: true

hystrix:
  command:
    default:
      execution:
        timeout:
          enabled: true
        isolation:
          thread:
            timeoutInMilliseconds: 10000
    inventory-service:
      execution:
        isolation:
          thread:
            timeoutInMilliseconds: 5000
```

### Monitoring and Metrics

```java
@Component
public class FeignMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public FeignMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        // Register custom metrics
        Gauge.builder("feign.client.connections")
                .description("Number of active Feign client connections")
                .register(meterRegistry, this, obj -> getActiveConnections());
    }
    
    private double getActiveConnections() {
        // Implementation to get active connections
        return 0.0;
    }
}
```

## Summary

Today we covered:

1. **OpenFeign Basics**: Declarative REST client setup and configuration
2. **Advanced Configuration**: Custom error decoders, interceptors, and request options
3. **Service Communication**: Implementing robust service-to-service communication
4. **RestTemplate Integration**: Load-balanced RestTemplate with service discovery
5. **Circuit Breaker Patterns**: Implementing resilience with fallbacks
6. **Performance Optimization**: Connection pooling and caching strategies
7. **Error Handling**: Comprehensive error handling and resilience patterns
8. **Testing Strategies**: Unit and integration testing approaches
9. **Configuration Management**: Best practices for Feign and RestTemplate configuration
10. **Monitoring**: Metrics and health checks for external service communication

---

## 🚀 Hands-On Project: E-Commerce Order Processing System

Let's build a complete order processing system using OpenFeign and RestTemplate to demonstrate service-to-service communication patterns.

### Project Architecture

```
order-service (port 8080)
├── UserClient (Feign)
├── ProductClient (Feign)  
├── PaymentClient (RestTemplate)
└── NotificationClient (Feign)

user-service (port 8081)
product-service (port 8082)
payment-service (port 8083)
notification-service (port 8084)
```

### 1. Order Service Implementation

**OrderServiceApplication.java**:
```java
@SpringBootApplication
@EnableEurekaClient
@EnableFeignClients
@EnableCircuitBreaker
public class OrderServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
    
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    }
    
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("users", "products");
    }
}
```

**Order Domain Model**:
```java
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "product_id")
    private Long productId;
    
    private Integer quantity;
    
    @Column(name = "total_amount")
    private BigDecimal totalAmount;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors, getters, setters
    public Order() {}
    
    public Order(Long userId, Long productId, Integer quantity, BigDecimal totalAmount) {
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.status = OrderStatus.PENDING;
    }
    
    public enum OrderStatus {
        PENDING, CONFIRMED, PAID, SHIPPED, DELIVERED, CANCELLED
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

### 2. Feign Clients with Advanced Configuration

**UserClient.java**:
```java
@FeignClient(
    name = "user-service",
    configuration = UserClientConfig.class,
    fallback = UserClientFallback.class
)
public interface UserClient {
    
    @GetMapping("/api/users/{id}")
    @Cacheable("users")
    UserResponse getUser(@PathVariable("id") Long id);
    
    @PostMapping("/api/users/{id}/validate")
    ValidationResponse validateUser(@PathVariable("id") Long id);
    
    @GetMapping("/api/users/{id}/address")
    AddressResponse getUserAddress(@PathVariable("id") Long id);
}

@Component
public class UserClientFallback implements UserClient {
    
    @Override
    public UserResponse getUser(Long id) {
        return new UserResponse(id, "Unknown User", "unknown@example.com", false);
    }
    
    @Override
    public ValidationResponse validateUser(Long id) {
        return new ValidationResponse(false, "Service unavailable");
    }
    
    @Override
    public AddressResponse getUserAddress(Long id) {
        return new AddressResponse("Unknown", "Unknown", "Unknown", "00000");
    }
}

@Configuration
public class UserClientConfig {
    
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
    
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(3000, 5000); // 3s connect, 5s read timeout
    }
    
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(100, 1000, 3);
    }
    
    @Bean
    public ErrorDecoder errorDecoder() {
        return new UserServiceErrorDecoder();
    }
}

public class UserServiceErrorDecoder implements ErrorDecoder {
    
    @Override
    public Exception decode(String methodKey, Response response) {
        String message = String.format("Error occurred in %s", methodKey);
        
        switch (response.status()) {
            case 400:
                return new BadRequestException(message);
            case 404:
                return new UserNotFoundException("User not found");
            case 500:
                return new UserServiceException("User service internal error");
            default:
                return new Exception(message);
        }
    }
}
```

**ProductClient.java**:
```java
@FeignClient(
    name = "product-service",
    configuration = ProductClientConfig.class,
    fallback = ProductClientFallback.class
)
public interface ProductClient {
    
    @GetMapping("/api/products/{id}")
    @Cacheable("products")
    ProductResponse getProduct(@PathVariable("id") Long id);
    
    @PostMapping("/api/products/{id}/reserve")
    ReservationResponse reserveProduct(
        @PathVariable("id") Long id,
        @RequestBody ReservationRequest request
    );
    
    @PostMapping("/api/products/{id}/release")
    void releaseReservation(
        @PathVariable("id") Long id,
        @RequestBody String reservationId
    );
    
    @GetMapping("/api/products/{id}/availability")
    AvailabilityResponse checkAvailability(
        @PathVariable("id") Long id,
        @RequestParam("quantity") Integer quantity
    );
}

@Component
public class ProductClientFallback implements ProductClient {
    
    @Override
    public ProductResponse getProduct(Long id) {
        return new ProductResponse(id, "Product Unavailable", BigDecimal.ZERO, 0);
    }
    
    @Override
    public ReservationResponse reserveProduct(Long id, ReservationRequest request) {
        return new ReservationResponse(null, false, "Service unavailable");
    }
    
    @Override
    public void releaseReservation(Long id, String reservationId) {
        // Log the failure to release reservation
        System.err.println("Failed to release reservation: " + reservationId);
    }
    
    @Override
    public AvailabilityResponse checkAvailability(Long id, Integer quantity) {
        return new AvailabilityResponse(false, 0, "Service unavailable");
    }
}
```

### 3. RestTemplate Implementation with Circuit Breaker

**PaymentService.java**:
```java
@Service
public class PaymentService {
    
    private final RestTemplate restTemplate;
    private final CircuitBreakerFactory circuitBreakerFactory;
    private static final String PAYMENT_SERVICE_URL = "http://payment-service/api/payments";
    
    public PaymentService(RestTemplate restTemplate, CircuitBreakerFactory circuitBreakerFactory) {
        this.restTemplate = restTemplate;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }
    
    public PaymentResponse processPayment(PaymentRequest request) {
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("payment-service");
        
        return circuitBreaker.executeSupplier(() -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + getAuthToken());
            
            HttpEntity<PaymentRequest> entity = new HttpEntity<>(request, headers);
            
            try {
                ResponseEntity<PaymentResponse> response = restTemplate.postForEntity(
                    PAYMENT_SERVICE_URL + "/process",
                    entity,
                    PaymentResponse.class
                );
                
                return response.getBody();
                
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                throw new PaymentServiceException("Payment processing failed: " + e.getMessage());
            }
        });
    }
    
    public PaymentStatus getPaymentStatus(String paymentId) {
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("payment-service");
        
        return circuitBreaker.executeSupplier(() -> {
            try {
                ResponseEntity<PaymentStatus> response = restTemplate.getForEntity(
                    PAYMENT_SERVICE_URL + "/status/{paymentId}",
                    PaymentStatus.class,
                    paymentId
                );
                
                return response.getBody();
                
            } catch (HttpClientErrorException.NotFound e) {
                throw new PaymentNotFoundException("Payment not found: " + paymentId);
            } catch (Exception e) {
                // Circuit breaker fallback
                return new PaymentStatus(paymentId, "UNKNOWN", "Service unavailable");
            }
        });
    }
    
    private String getAuthToken() {
        // In real implementation, this would retrieve from secure storage
        return "dummy-token-for-demo";
    }
}

@Configuration
public class RestTemplateConfig {
    
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(Arrays.asList(new LoggingInterceptor()));
        
        return restTemplate;
    }
    
    @Bean
    public CircuitBreakerFactory circuitBreakerFactory() {
        return CircuitBreakerFactory.create(
            CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(30000))
                .slidingWindowSize(10)
                .build()
        );
    }
}

public class LoggingInterceptor implements ClientHttpRequestInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);
    
    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {
        
        logger.info("Outgoing request: {} {}", request.getMethod(), request.getURI());
        
        long startTime = System.currentTimeMillis();
        ClientHttpResponse response = execution.execute(request, body);
        long duration = System.currentTimeMillis() - startTime;
        
        logger.info("Response: {} in {}ms", response.getStatusCode(), duration);
        
        return response;
    }
}
```

### 4. Order Processing Service

**OrderService.java**:
```java
@Service
@Transactional
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final UserClient userClient;
    private final ProductClient productClient;
    private final PaymentService paymentService;
    private final NotificationClient notificationClient;
    
    public OrderService(
            OrderRepository orderRepository,
            UserClient userClient,
            ProductClient productClient,
            PaymentService paymentService,
            NotificationClient notificationClient) {
        this.orderRepository = orderRepository;
        this.userClient = userClient;
        this.productClient = productClient;
        this.paymentService = paymentService;
        this.notificationClient = notificationClient;
    }
    
    @Async
    public CompletableFuture<OrderResponse> createOrder(CreateOrderRequest request) {
        try {
            // Step 1: Validate user
            ValidationResponse userValidation = userClient.validateUser(request.getUserId());
            if (!userValidation.isValid()) {
                throw new InvalidUserException("User validation failed: " + userValidation.getMessage());
            }
            
            // Step 2: Check product availability
            AvailabilityResponse availability = productClient.checkAvailability(
                request.getProductId(), 
                request.getQuantity()
            );
            if (!availability.isAvailable()) {
                throw new ProductUnavailableException("Product not available");
            }
            
            // Step 3: Get product details for pricing
            ProductResponse product = productClient.getProduct(request.getProductId());
            BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
            
            // Step 4: Create order entity
            Order order = new Order(request.getUserId(), request.getProductId(), 
                                  request.getQuantity(), totalAmount);
            order = orderRepository.save(order);
            
            // Step 5: Reserve product inventory
            ReservationRequest reservationRequest = new ReservationRequest(
                request.getProductId(), 
                request.getQuantity(), 
                order.getId().toString()
            );
            
            ReservationResponse reservation = productClient.reserveProduct(
                request.getProductId(), 
                reservationRequest
            );
            
            if (!reservation.isSuccess()) {
                order.setStatus(Order.OrderStatus.CANCELLED);
                orderRepository.save(order);
                throw new ReservationFailedException("Failed to reserve inventory");
            }
            
            // Step 6: Process payment
            PaymentRequest paymentRequest = new PaymentRequest(
                order.getId().toString(),
                request.getUserId(),
                totalAmount,
                request.getPaymentMethod()
            );
            
            PaymentResponse paymentResponse = paymentService.processPayment(paymentRequest);
            
            if (paymentResponse.isSuccess()) {
                order.setStatus(Order.OrderStatus.PAID);
                orderRepository.save(order);
                
                // Step 7: Send notification
                NotificationRequest notification = new NotificationRequest(
                    request.getUserId(),
                    "Order Confirmation",
                    "Your order #" + order.getId() + " has been confirmed.",
                    NotificationType.ORDER_CONFIRMATION
                );
                
                notificationClient.sendNotification(notification);
                
                return CompletableFuture.completedFuture(
                    new OrderResponse(order, "Order created successfully")
                );
                
            } else {
                // Payment failed - release reservation
                productClient.releaseReservation(request.getProductId(), reservation.getReservationId());
                order.setStatus(Order.OrderStatus.CANCELLED);
                orderRepository.save(order);
                
                throw new PaymentFailedException("Payment processing failed");
            }
            
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    public List<OrderResponse> getUserOrders(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return orders.stream()
                    .map(order -> new OrderResponse(order, "Success"))
                    .collect(Collectors.toList());
    }
    
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        
        return new OrderResponse(order, "Success");
    }
}
```

### 5. Exception Handling and Resilience

**Global Exception Handler**:
```java
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException e) {
        log.error("User not found: {}", e.getMessage());
        ErrorResponse error = new ErrorResponse("USER_NOT_FOUND", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(ProductUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleProductUnavailable(ProductUnavailableException e) {
        log.error("Product unavailable: {}", e.getMessage());
        ErrorResponse error = new ErrorResponse("PRODUCT_UNAVAILABLE", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentFailed(PaymentFailedException e) {
        log.error("Payment failed: {}", e.getMessage());
        ErrorResponse error = new ErrorResponse("PAYMENT_FAILED", e.getMessage());
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(error);
    }
    
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(FeignException e) {
        log.error("Feign client error: {}", e.getMessage());
        ErrorResponse error = new ErrorResponse("SERVICE_COMMUNICATION_ERROR", 
                                              "External service communication failed");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        ErrorResponse error = new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

public class ErrorResponse {
    private String code;
    private String message;
    private LocalDateTime timestamp;
    
    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
```

### 6. Testing Strategies

**Integration Test for Order Service**:
```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class OrderServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @Autowired
    private OrderService orderService;
    
    @MockBean
    private UserClient userClient;
    
    @MockBean
    private ProductClient productClient;
    
    @MockBean
    private PaymentService paymentService;
    
    @MockBean
    private NotificationClient notificationClient;
    
    @Test
    void shouldCreateOrderSuccessfully() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(1L, 1L, 2, "CREDIT_CARD");
        
        when(userClient.validateUser(1L))
            .thenReturn(new ValidationResponse(true, "Valid user"));
        
        when(productClient.checkAvailability(1L, 2))
            .thenReturn(new AvailabilityResponse(true, 10, "Available"));
        
        when(productClient.getProduct(1L))
            .thenReturn(new ProductResponse(1L, "Test Product", new BigDecimal("29.99"), 10));
        
        when(productClient.reserveProduct(eq(1L), any(ReservationRequest.class)))
            .thenReturn(new ReservationResponse("res-123", true, "Reserved"));
        
        when(paymentService.processPayment(any(PaymentRequest.class)))
            .thenReturn(new PaymentResponse("pay-123", true, "Payment successful"));
        
        // When
        CompletableFuture<OrderResponse> result = orderService.createOrder(request);
        
        // Then
        assertThat(result).succeedsWithin(Duration.ofSeconds(5));
        OrderResponse response = result.join();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getOrder().getStatus()).isEqualTo(Order.OrderStatus.PAID);
    }
    
    @Test
    void shouldHandlePaymentFailure() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(1L, 1L, 2, "CREDIT_CARD");
        
        when(userClient.validateUser(1L))
            .thenReturn(new ValidationResponse(true, "Valid user"));
        
        when(productClient.checkAvailability(1L, 2))
            .thenReturn(new AvailabilityResponse(true, 10, "Available"));
        
        when(productClient.getProduct(1L))
            .thenReturn(new ProductResponse(1L, "Test Product", new BigDecimal("29.99"), 10));
        
        ReservationResponse reservation = new ReservationResponse("res-123", true, "Reserved");
        when(productClient.reserveProduct(eq(1L), any(ReservationRequest.class)))
            .thenReturn(reservation);
        
        when(paymentService.processPayment(any(PaymentRequest.class)))
            .thenReturn(new PaymentResponse(null, false, "Payment failed"));
        
        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(request).join())
            .hasCauseInstanceOf(PaymentFailedException.class);
        
        // Verify reservation was released
        verify(productClient).releaseReservation(1L, "res-123");
    }
}

@WebMvcTest(OrderController.class)
class OrderControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private OrderService orderService;
    
    @Test
    void shouldCreateOrder() throws Exception {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(1L, 1L, 2, "CREDIT_CARD");
        OrderResponse response = new OrderResponse(
            new Order(1L, 1L, 2, new BigDecimal("59.98")), 
            "Order created successfully"
        );
        
        when(orderService.createOrder(any(CreateOrderRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(response));
        
        // When & Then
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "userId": 1,
                        "productId": 1,
                        "quantity": 2,
                        "paymentMethod": "CREDIT_CARD"
                    }
                    """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.order.status").value("PAID"));
    }
}
```

### 7. Configuration and Monitoring

**application.yml**:
```yaml
server:
  port: 8080

spring:
  application:
    name: order-service
  
  datasource:
    url: jdbc:postgresql://localhost:5432/orderdb
    username: orderuser
    password: orderpass
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=300s

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
    fetch-registry: true
    register-with-eureka: true
  instance:
    prefer-ip-address: true

feign:
  hystrix:
    enabled: true
  client:
    config:
      default:
        connectTimeout: 3000
        readTimeout: 5000
        loggerLevel: full
      user-service:
        connectTimeout: 2000
        readTimeout: 4000
      product-service:
        connectTimeout: 3000
        readTimeout: 6000

hystrix:
  command:
    default:
      execution:
        timeout:
          enabled: true
        isolation:
          thread:
            timeoutInMilliseconds: 10000
      circuitBreaker:
        requestVolumeThreshold: 20
        errorThresholdPercentage: 50
        sleepWindowInMilliseconds: 30000

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true

logging:
  level:
    com.example.orderservice: DEBUG
    org.springframework.cloud.openfeign: DEBUG
    feign: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 8. Performance Optimization

**Connection Pool Configuration**:
```java
@Configuration
@EnableConfigurationProperties({HttpClientProperties.class})
public class HttpClientConfig {
    
    @Bean
    public HttpComponentsClientHttpRequestFactory httpRequestFactory(HttpClientProperties properties) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(properties.getConnectionRequestTimeout())
                .setConnectTimeout(properties.getConnectTimeout())
                .setSocketTimeout(properties.getSocketTimeout())
                .build();
        
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setMaxConnTotal(properties.getMaxConnTotal())
                .setMaxConnPerRoute(properties.getMaxConnPerRoute())
                .setDefaultRequestConfig(requestConfig)
                .setRetryHandler(new DefaultHttpRequestRetryHandler(3, true))
                .build();
        
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(httpClient);
        
        return factory;
    }
}

@ConfigurationProperties(prefix = "http.client")
@Data
public class HttpClientProperties {
    private int connectTimeout = 3000;
    private int socketTimeout = 5000;
    private int connectionRequestTimeout = 1000;
    private int maxConnTotal = 200;
    private int maxConnPerRoute = 50;
}
```

**Async Processing Configuration**:
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "orderProcessingExecutor")
    public Executor orderProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("OrderProcessor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

---

## 📊 Performance Metrics and Monitoring

### Custom Metrics
```java
@Component
public class OrderServiceMetrics {
    
    private final Counter orderCreatedCounter;
    private final Counter orderFailedCounter;
    private final Timer orderProcessingTimer;
    private final Gauge activeOrdersGauge;
    
    public OrderServiceMetrics(MeterRegistry meterRegistry, OrderRepository orderRepository) {
        this.orderCreatedCounter = Counter.builder("orders.created")
                .description("Number of orders created")
                .register(meterRegistry);
        
        this.orderFailedCounter = Counter.builder("orders.failed")
                .description("Number of failed orders")
                .register(meterRegistry);
        
        this.orderProcessingTimer = Timer.builder("orders.processing.time")
                .description("Order processing time")
                .register(meterRegistry);
        
        this.activeOrdersGauge = Gauge.builder("orders.active")
                .description("Number of active orders")
                .register(meterRegistry, orderRepository, repo -> repo.countByStatus(Order.OrderStatus.PENDING));
    }
    
    public void incrementOrderCreated() {
        orderCreatedCounter.increment();
    }
    
    public void incrementOrderFailed() {
        orderFailedCounter.increment();
    }
    
    public Timer.Sample startOrderProcessingTimer() {
        return Timer.start(orderProcessingTimer);
    }
}
```

## Key Takeaways

- **OpenFeign** provides a clean, declarative approach to HTTP client creation with powerful configuration options
- **Circuit breakers and fallbacks** are essential for resilient service communication in distributed systems
- **RestTemplate** with load balancing offers fine-grained control over HTTP interactions
- **Proper error handling** and retry mechanisms prevent cascading failures
- **Caching strategies** significantly improve performance and reduce network calls
- **Connection pooling** optimizes resource usage and improves throughput
- **Comprehensive testing** ensures reliable service integrations with proper mocking
- **Monitoring and metrics** provide visibility into service communication health and performance
- **Async processing** improves response times and system scalability

## Next Steps

Tomorrow we'll explore **Spring Cloud Sleuth & Zipkin** for distributed tracing, learning how to track requests across multiple services and diagnose performance issues in complex microservices architectures.