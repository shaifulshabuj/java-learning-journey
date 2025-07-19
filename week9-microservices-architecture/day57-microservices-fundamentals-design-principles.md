# Day 57: Microservices Fundamentals & Design Principles

## Learning Objectives
- Understand microservices architecture fundamentals
- Master key design principles for microservices
- Learn advantages and challenges of microservices
- Analyze monolith vs microservices tradeoffs
- Build foundational knowledge for microservices ecosystem

---

## 1. What are Microservices?

### Definition
Microservices architecture is an approach to developing software applications as a suite of small, independent, and loosely coupled services that communicate over well-defined APIs.

### Key Characteristics
- **Single Responsibility**: Each service has a specific business function
- **Decentralized**: Services manage their own data and business logic
- **Independent Deployment**: Services can be deployed independently
- **Technology Agnostic**: Different services can use different technologies
- **Fault Isolation**: Failure in one service doesn't cascade to others

### Microservices vs Monolith Comparison

```java
// Monolithic Architecture Example
@RestController
public class ECommerceController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private PaymentService paymentService;
    
    @PostMapping("/orders")
    public ResponseEntity<Order> createOrder(@RequestBody OrderRequest request) {
        // All services in same application
        User user = userService.validateUser(request.getUserId());
        Product product = productService.checkInventory(request.getProductId());
        Order order = orderService.createOrder(user, product);
        Payment payment = paymentService.processPayment(order);
        
        return ResponseEntity.ok(order);
    }
}
```

```java
// Microservices Architecture Example
// User Service
@RestController
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(user);
    }
    
    @PostMapping("/users/{id}/validate")
    public ResponseEntity<Boolean> validateUser(@PathVariable Long id) {
        boolean valid = userService.validateUser(id);
        return ResponseEntity.ok(valid);
    }
}

// Product Service
@RestController
public class ProductController {
    
    @Autowired
    private ProductService productService;
    
    @GetMapping("/products/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        Product product = productService.findById(id);
        return ResponseEntity.ok(product);
    }
    
    @PostMapping("/products/{id}/reserve")
    public ResponseEntity<Boolean> reserveProduct(@PathVariable Long id, @RequestBody int quantity) {
        boolean reserved = productService.reserveInventory(id, quantity);
        return ResponseEntity.ok(reserved);
    }
}

// Order Service
@RestController
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    @Autowired
    private ProductServiceClient productServiceClient;
    
    @PostMapping("/orders")
    public ResponseEntity<Order> createOrder(@RequestBody OrderRequest request) {
        // Validate user via User Service
        boolean userValid = userServiceClient.validateUser(request.getUserId());
        if (!userValid) {
            throw new InvalidUserException("User validation failed");
        }
        
        // Reserve product via Product Service
        boolean productReserved = productServiceClient.reserveProduct(
            request.getProductId(), request.getQuantity());
        if (!productReserved) {
            throw new ProductUnavailableException("Product not available");
        }
        
        // Create order
        Order order = orderService.createOrder(request);
        return ResponseEntity.ok(order);
    }
}
```

---

## 2. Core Microservices Design Principles

### 2.1 Single Responsibility Principle (SRP)

Each microservice should have one reason to change - it should be responsible for a single business capability.

```java
// ❌ Bad: Multiple responsibilities
@Service
public class UserManagementService {
    
    // User management
    public User createUser(User user) { /* ... */ }
    public User updateUser(User user) { /* ... */ }
    
    // Notification handling
    public void sendWelcomeEmail(User user) { /* ... */ }
    public void sendPasswordResetEmail(User user) { /* ... */ }
    
    // Audit logging
    public void logUserActivity(User user, String activity) { /* ... */ }
    
    // Payment processing
    public PaymentResult processPayment(User user, Payment payment) { /* ... */ }
}

// ✅ Good: Single responsibility per service
@Service
public class UserService {
    public User createUser(User user) { /* ... */ }
    public User updateUser(User user) { /* ... */ }
    public User findById(Long id) { /* ... */ }
    public boolean validateUser(Long id) { /* ... */ }
}

@Service
public class NotificationService {
    public void sendWelcomeEmail(User user) { /* ... */ }
    public void sendPasswordResetEmail(User user) { /* ... */ }
    public void sendOrderConfirmation(Order order) { /* ... */ }
}

@Service
public class AuditService {
    public void logUserActivity(Long userId, String activity) { /* ... */ }
    public void logSystemEvent(String event, Map<String, Object> context) { /* ... */ }
}
```

### 2.2 Decentralized Data Management

Each microservice should own its data and expose it only through its API.

```java
// User Service - owns user data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
    @Column(nullable = false)
    private String hashedPassword;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Getters and setters
}

// Product Service - owns product data
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String description;
    
    @Column(nullable = false)
    private BigDecimal price;
    
    @Column(nullable = false)
    private Integer stockQuantity;
    
    @Column(nullable = false)
    private String category;
    
    @Column(nullable = false)
    private String sku;
    
    // Getters and setters
}

// Order Service - owns order data but references other services
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Reference to User Service (by ID, not direct relationship)
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private BigDecimal totalAmount;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    // Getters and setters
}

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;
    
    // Reference to Product Service (by ID, not direct relationship)
    @Column(nullable = false)
    private Long productId;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false)
    private BigDecimal unitPrice;
    
    // Getters and setters
}
```

### 2.3 Service Independence & Autonomy

Services should be deployable and scalable independently.

```java
// Independent service configuration
@SpringBootApplication
@EnableJpaRepositories
@EnableScheduling
public class UserServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

// Service-specific configuration
@Configuration
@ConfigurationProperties(prefix = "user-service")
public class UserServiceConfig {
    private String jwtSecret;
    private int jwtExpirationHours;
    private String emailServiceUrl;
    private String auditServiceUrl;
    
    // Getters and setters
}

// Independent health check
@Component
public class UserServiceHealthIndicator implements HealthIndicator {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public Health health() {
        try {
            long userCount = userRepository.count();
            return Health.up()
                .withDetail("userCount", userCount)
                .withDetail("status", "User service is healthy")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("status", "User service is unhealthy")
                .build();
        }
    }
}
```

### 2.4 API-First Design

Services should expose well-defined APIs and hide implementation details.

```java
// API Contract Definition
@RestController
@RequestMapping("/api/v1/users")
@Validated
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(request);
        UserResponse response = UserResponse.fromUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        UserResponse response = UserResponse.fromUser(user);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id, 
            @Valid @RequestBody UpdateUserRequest request) {
        User user = userService.updateUser(id, request);
        UserResponse response = UserResponse.fromUser(user);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{id}/validate")
    public ResponseEntity<ValidationResponse> validateUser(@PathVariable Long id) {
        boolean valid = userService.validateUser(id);
        ValidationResponse response = new ValidationResponse(valid);
        return ResponseEntity.ok(response);
    }
}

// Request/Response DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static UserResponse fromUser(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
    }
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResponse {
    private boolean valid;
    private String reason;
    
    public ValidationResponse(boolean valid) {
        this.valid = valid;
        this.reason = valid ? "User is valid" : "User validation failed";
    }
}
```

---

## 3. Microservices Advantages

### 3.1 Scalability

```java
// Independent scaling configuration
@Configuration
public class ScalingConfig {
    
    // User Service - CPU intensive (authentication, validation)
    @Bean
    @Profile("user-service")
    public TaskExecutor userServiceTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("user-service-");
        executor.initialize();
        return executor;
    }
    
    // Product Service - Memory intensive (catalog, search)
    @Bean
    @Profile("product-service")
    public TaskExecutor productServiceTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("product-service-");
        executor.initialize();
        return executor;
    }
    
    // Order Service - I/O intensive (payment processing)
    @Bean
    @Profile("order-service")
    public TaskExecutor orderServiceTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(15);
        executor.setMaxPoolSize(75);
        executor.setQueueCapacity(150);
        executor.setThreadNamePrefix("order-service-");
        executor.initialize();
        return executor;
    }
}
```

### 3.2 Technology Diversity

```java
// User Service - Spring Boot + PostgreSQL
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    // PostgreSQL specific JSON column
    @Column(columnDefinition = "jsonb")
    private String preferences;
    
    // Getters and setters
}

// Product Service - Spring Boot + MongoDB
@Document(collection = "products")
public class Product {
    @Id
    private String id;
    
    @Field
    private String name;
    
    @Field
    private String description;
    
    @Field
    private BigDecimal price;
    
    // MongoDB specific nested document
    @Field
    private List<ProductAttribute> attributes;
    
    // Getters and setters
}

// Search Service - Spring Boot + Elasticsearch
@Document(indexName = "products")
public class ProductSearchDocument {
    @Id
    private String id;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;
    
    @Field(type = FieldType.Keyword)
    private String category;
    
    @Field(type = FieldType.Double)
    private BigDecimal price;
    
    // Elasticsearch specific fields
    @Field(type = FieldType.Nested)
    private List<String> tags;
    
    // Getters and setters
}
```

### 3.3 Fault Isolation

```java
// Circuit Breaker Pattern Implementation
@Component
public class UserServiceClient {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${user-service.url}")
    private String userServiceUrl;
    
    private CircuitBreaker circuitBreaker = CircuitBreaker.create("user-service");
    
    public Optional<User> getUser(Long userId) {
        try {
            return circuitBreaker.executeSupplier(() -> {
                String url = userServiceUrl + "/users/" + userId;
                User user = restTemplate.getForObject(url, User.class);
                return Optional.ofNullable(user);
            });
        } catch (Exception e) {
            logger.warn("Failed to fetch user {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }
    
    public boolean validateUser(Long userId) {
        try {
            return circuitBreaker.executeSupplier(() -> {
                String url = userServiceUrl + "/users/" + userId + "/validate";
                ValidationResponse response = restTemplate.postForObject(url, null, ValidationResponse.class);
                return response != null && response.isValid();
            });
        } catch (Exception e) {
            logger.warn("Failed to validate user {}: {}", userId, e.getMessage());
            return false; // Fail safe
        }
    }
    
    @EventListener
    public void handleCircuitBreakerStateChange(CircuitBreakerOnStateTransitionEvent event) {
        logger.info("Circuit breaker state changed: {} -> {}", 
            event.getStateTransition().getFromState(), 
            event.getStateTransition().getToState());
    }
}
```

---

## 4. Microservices Challenges

### 4.1 Distributed System Complexity

```java
// Distributed transaction coordination
@Service
public class OrderProcessingService {
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    @Autowired
    private ProductServiceClient productServiceClient;
    
    @Autowired
    private PaymentServiceClient paymentServiceClient;
    
    @Autowired
    private OrderRepository orderRepository;
    
    public Order processOrder(OrderRequest request) {
        String transactionId = UUID.randomUUID().toString();
        
        try {
            // Step 1: Validate user
            if (!userServiceClient.validateUser(request.getUserId())) {
                throw new OrderProcessingException("Invalid user");
            }
            
            // Step 2: Reserve products
            List<ProductReservation> reservations = new ArrayList<>();
            for (OrderItem item : request.getItems()) {
                ProductReservation reservation = productServiceClient.reserveProduct(
                    item.getProductId(), item.getQuantity(), transactionId);
                if (reservation == null) {
                    // Rollback previous reservations
                    rollbackReservations(reservations, transactionId);
                    throw new OrderProcessingException("Product not available: " + item.getProductId());
                }
                reservations.add(reservation);
            }
            
            // Step 3: Process payment
            PaymentResult paymentResult = paymentServiceClient.processPayment(
                request.getPaymentDetails(), request.getTotalAmount(), transactionId);
            if (!paymentResult.isSuccess()) {
                // Rollback reservations
                rollbackReservations(reservations, transactionId);
                throw new OrderProcessingException("Payment failed: " + paymentResult.getErrorMessage());
            }
            
            // Step 4: Create order
            Order order = new Order();
            order.setUserId(request.getUserId());
            order.setTotalAmount(request.getTotalAmount());
            order.setStatus(OrderStatus.CONFIRMED);
            order.setTransactionId(transactionId);
            order.setItems(createOrderItems(request.getItems()));
            
            return orderRepository.save(order);
            
        } catch (Exception e) {
            // Comprehensive rollback
            rollbackTransaction(transactionId);
            throw new OrderProcessingException("Order processing failed", e);
        }
    }
    
    private void rollbackReservations(List<ProductReservation> reservations, String transactionId) {
        for (ProductReservation reservation : reservations) {
            try {
                productServiceClient.cancelReservation(reservation.getId(), transactionId);
            } catch (Exception e) {
                logger.error("Failed to cancel reservation {}: {}", reservation.getId(), e.getMessage());
            }
        }
    }
    
    private void rollbackTransaction(String transactionId) {
        try {
            productServiceClient.rollbackTransaction(transactionId);
            paymentServiceClient.rollbackTransaction(transactionId);
        } catch (Exception e) {
            logger.error("Failed to rollback transaction {}: {}", transactionId, e.getMessage());
        }
    }
}
```

### 4.2 Data Consistency

```java
// Eventual consistency implementation
@Service
public class OrderEventService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @EventListener
    @Transactional
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        Optional<Order> orderOpt = orderRepository.findByTransactionId(event.getTransactionId());
        
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            
            if (event.isSuccess()) {
                order.setStatus(OrderStatus.PAID);
                order.setPaymentId(event.getPaymentId());
                orderRepository.save(order);
                
                // Publish order confirmed event
                eventPublisher.publishEvent(new OrderConfirmedEvent(order.getId(), order.getUserId()));
            } else {
                order.setStatus(OrderStatus.PAYMENT_FAILED);
                order.setFailureReason(event.getFailureReason());
                orderRepository.save(order);
                
                // Publish order failed event
                eventPublisher.publishEvent(new OrderFailedEvent(order.getId(), event.getFailureReason()));
            }
        }
    }
    
    @EventListener
    @Transactional
    public void handleInventoryReserved(InventoryReservedEvent event) {
        Optional<Order> orderOpt = orderRepository.findByTransactionId(event.getTransactionId());
        
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            
            if (event.isSuccess()) {
                order.setStatus(OrderStatus.INVENTORY_RESERVED);
                orderRepository.save(order);
            } else {
                order.setStatus(OrderStatus.INVENTORY_FAILED);
                order.setFailureReason(event.getFailureReason());
                orderRepository.save(order);
                
                // Publish order failed event
                eventPublisher.publishEvent(new OrderFailedEvent(order.getId(), event.getFailureReason()));
            }
        }
    }
}
```

### 4.3 Network Latency & Failures

```java
// Retry mechanism with exponential backoff
@Component
public class ResilientServiceClient {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Retryable(
        value = {RestClientException.class, ResourceAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public <T> T executeWithRetry(String url, Class<T> responseType) {
        try {
            return restTemplate.getForObject(url, responseType);
        } catch (RestClientException e) {
            logger.warn("Service call failed for URL: {}, attempt will be retried", url);
            throw e;
        }
    }
    
    @Recover
    public <T> T recover(RestClientException e, String url, Class<T> responseType) {
        logger.error("All retry attempts failed for URL: {}", url);
        throw new ServiceUnavailableException("Service temporarily unavailable", e);
    }
}

// Timeout configuration
@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // Configure timeout
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 5 seconds
        factory.setReadTimeout(10000);    // 10 seconds
        
        restTemplate.setRequestFactory(factory);
        
        // Add error handler
        restTemplate.setErrorHandler(new CustomResponseErrorHandler());
        
        return restTemplate;
    }
}

public class CustomResponseErrorHandler implements ResponseErrorHandler {
    
    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return response.getStatusCode().series() == HttpStatus.Series.CLIENT_ERROR ||
               response.getStatusCode().series() == HttpStatus.Series.SERVER_ERROR;
    }
    
    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        HttpStatus statusCode = response.getStatusCode();
        
        switch (statusCode.series()) {
            case CLIENT_ERROR:
                if (statusCode == HttpStatus.NOT_FOUND) {
                    throw new ResourceNotFoundException("Resource not found");
                } else if (statusCode == HttpStatus.BAD_REQUEST) {
                    throw new BadRequestException("Bad request");
                } else {
                    throw new ClientException("Client error: " + statusCode);
                }
            case SERVER_ERROR:
                throw new ServiceUnavailableException("Service error: " + statusCode);
            default:
                throw new RestClientException("Unknown error: " + statusCode);
        }
    }
}
```

---

## 5. Monolith to Microservices Migration Strategy

### 5.1 Strangler Fig Pattern

```java
// Legacy monolith service
@Service
public class LegacyUserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private AuditService auditService;
    
    public User createUser(User user) {
        // Validation
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new UserAlreadyExistsException("User already exists");
        }
        
        // Save user
        User savedUser = userRepository.save(user);
        
        // Send welcome email
        emailService.sendWelcomeEmail(savedUser);
        
        // Log audit
        auditService.logUserCreation(savedUser);
        
        return savedUser;
    }
}

// Migration proxy service
@Service
public class UserServiceProxy {
    
    @Autowired
    private LegacyUserService legacyUserService;
    
    @Autowired
    private MicroserviceUserService microserviceUserService;
    
    @Value("${migration.user-service.enabled:false}")
    private boolean migrationEnabled;
    
    @Value("${migration.user-service.percentage:0}")
    private int migrationPercentage;
    
    public User createUser(User user) {
        if (shouldUseMicroservice()) {
            try {
                return microserviceUserService.createUser(user);
            } catch (Exception e) {
                logger.error("Microservice failed, falling back to legacy", e);
                return legacyUserService.createUser(user);
            }
        } else {
            return legacyUserService.createUser(user);
        }
    }
    
    private boolean shouldUseMicroservice() {
        if (!migrationEnabled) {
            return false;
        }
        
        // Percentage-based rollout
        return ThreadLocalRandom.current().nextInt(100) < migrationPercentage;
    }
}
```

### 5.2 Database Decomposition

```java
// Shared database approach (initial)
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String email;
    private String firstName;
    private String lastName;
    
    // Profile information (will be moved to profile service)
    private String phoneNumber;
    private String address;
    private String city;
    private String country;
    
    // Preferences (will be moved to preferences service)
    private String language;
    private String timezone;
    private boolean emailNotifications;
    
    // Getters and setters
}

// Database per service approach (after migration)
// User Service Database
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
    @Column(nullable = false)
    private String hashedPassword;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Only core user data
}

// Profile Service Database
@Entity
@Table(name = "user_profiles")
public class UserProfile {
    @Id
    private Long userId; // Reference to User Service
    
    private String phoneNumber;
    private String address;
    private String city;
    private String country;
    private String postalCode;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Getters and setters
}

// Preferences Service Database
@Entity
@Table(name = "user_preferences")
public class UserPreferences {
    @Id
    private Long userId; // Reference to User Service
    
    private String language;
    private String timezone;
    private boolean emailNotifications;
    private boolean smsNotifications;
    private boolean pushNotifications;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Getters and setters
}
```

---

## 6. Practical Exercise: E-commerce Decomposition Analysis

### Current Monolith Structure

```java
// Monolithic E-commerce Application
@RestController
@RequestMapping("/api")
public class ECommerceController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private InventoryService inventoryService;
    
    @Autowired
    private ShippingService shippingService;
    
    @Autowired
    private NotificationService notificationService;
    
    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        // Complex business logic spanning multiple domains
        User user = userService.validateUser(request.getUserId());
        
        List<Product> products = productService.getProducts(request.getProductIds());
        
        // Check inventory
        for (Product product : products) {
            if (!inventoryService.checkAvailability(product.getId(), request.getQuantity())) {
                throw new ProductUnavailableException("Product not available");
            }
        }
        
        // Calculate total
        BigDecimal total = orderService.calculateTotal(products, request.getQuantities());
        
        // Process payment
        PaymentResult paymentResult = paymentService.processPayment(
            request.getPaymentDetails(), total);
        
        if (!paymentResult.isSuccess()) {
            throw new PaymentFailedException("Payment failed");
        }
        
        // Create order
        Order order = orderService.createOrder(user, products, total);
        
        // Update inventory
        inventoryService.reserveProducts(request.getProductIds(), request.getQuantities());
        
        // Calculate shipping
        ShippingCost shippingCost = shippingService.calculateShipping(
            order, user.getAddress());
        
        // Send notifications
        notificationService.sendOrderConfirmation(user, order);
        
        return ResponseEntity.ok(OrderResponse.fromOrder(order));
    }
}
```

### Proposed Microservices Decomposition

```java
// 1. User Service
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        // Handle user management
    }
    
    @PostMapping("/{id}/validate")
    public ResponseEntity<Boolean> validateUser(@PathVariable Long id) {
        // Handle user validation
    }
}

// 2. Product Catalog Service
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        // Handle product information
    }
    
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts(@RequestParam List<Long> ids) {
        // Handle bulk product retrieval
    }
}

// 3. Inventory Service
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {
    
    @PostMapping("/check")
    public ResponseEntity<InventoryCheckResponse> checkAvailability(
            @RequestBody InventoryCheckRequest request) {
        // Handle inventory checks
    }
    
    @PostMapping("/reserve")
    public ResponseEntity<ReservationResponse> reserveProducts(
            @RequestBody ReservationRequest request) {
        // Handle inventory reservation
    }
}

// 4. Order Service
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        // Orchestrate order creation with other services
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        // Handle order retrieval
    }
}

// 5. Payment Service
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    
    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(
            @RequestBody PaymentRequest request) {
        // Handle payment processing
    }
}

// 6. Shipping Service
@RestController
@RequestMapping("/api/v1/shipping")
public class ShippingController {
    
    @PostMapping("/calculate")
    public ResponseEntity<ShippingCostResponse> calculateShipping(
            @RequestBody ShippingCalculationRequest request) {
        // Handle shipping calculations
    }
}

// 7. Notification Service
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    
    @PostMapping("/send")
    public ResponseEntity<Void> sendNotification(
            @RequestBody NotificationRequest request) {
        // Handle notification sending
    }
}
```

---

## 7. Key Takeaways

### When to Use Microservices:
✅ **Large, complex applications** with multiple teams  
✅ **Rapid scaling requirements** for different components  
✅ **Technology diversity** needs  
✅ **Independent deployment** requirements  
✅ **High availability** and fault tolerance needs  

### When to Avoid Microservices:
❌ **Small applications** with simple business logic  
❌ **Single team** development  
❌ **Tight coupling** between business functions  
❌ **Limited operational expertise**  
❌ **Strong consistency** requirements  

### Design Principles Summary:
1. **Single Responsibility**: One service, one business capability
2. **Decentralized Data**: Each service owns its data
3. **Independence**: Services can be developed, deployed, and scaled independently
4. **API-First**: Well-defined contracts between services
5. **Fault Isolation**: Failures don't cascade across services
6. **Eventual Consistency**: Accept eventual consistency for better availability

---

## 8. Next Steps

Tomorrow we'll explore **Service Decomposition & Domain Modeling**, learning how to:
- Apply Domain-Driven Design principles
- Identify service boundaries
- Model complex business domains
- Design data consistency strategies

**Practice Assignment**: Analyze a monolithic application in your domain and identify potential microservice boundaries using the principles learned today.

---

*"Microservices are not a silver bullet - they solve certain problems while introducing others. The key is understanding when the benefits outweigh the complexity."*

*"Good microservices architecture is about boundaries - both in terms of business capabilities and technical implementation."*