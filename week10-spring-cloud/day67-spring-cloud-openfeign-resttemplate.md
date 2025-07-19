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

## Key Takeaways

- OpenFeign provides a clean, declarative approach to HTTP client creation
- Proper error handling and fallbacks are essential for resilient service communication
- Load balancing and circuit breakers help maintain system stability
- Caching and connection pooling significantly improve performance
- Comprehensive testing ensures reliable service integrations
- Monitoring and metrics provide visibility into service communication health

## Next Steps

Tomorrow we'll explore Spring Cloud Sleuth & Zipkin for distributed tracing, learning how to track requests across multiple services and diagnose performance issues in microservices architectures.