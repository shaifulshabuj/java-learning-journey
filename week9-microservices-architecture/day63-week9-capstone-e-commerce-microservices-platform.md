# Day 63: Week 9 Capstone - E-commerce Microservices Platform

## Learning Objectives
- Integrate all microservices concepts into a complete system
- Implement a production-ready e-commerce platform
- Apply design patterns, data management, and communication strategies
- Build monitoring, security, and deployment capabilities
- Demonstrate microservices best practices and patterns

---

## 1. System Architecture Overview

### 1.1 High-Level Architecture

```java
// System Architecture Configuration
@Configuration
@EnableConfigurationProperties
public class EcommercePlatformConfig {
    
    @Bean
    @ConfigurationProperties(prefix = "ecommerce.platform")
    public PlatformConfig platformConfig() {
        return new PlatformConfig();
    }
    
    @Bean
    public ServiceRegistry serviceRegistry() {
        return new EurekaServiceRegistry();
    }
    
    @Bean
    public ApiGateway apiGateway() {
        return new SpringCloudGateway();
    }
    
    @Bean
    public MessageBroker messageBroker() {
        return new RabbitMQMessageBroker();
    }
    
    @Bean
    public EventStore eventStore() {
        return new PostgreSQLEventStore();
    }
}

// Platform Configuration
@Data
@ConfigurationProperties(prefix = "ecommerce.platform")
public class PlatformConfig {
    private String version;
    private String environment;
    private DatabaseConfig database;
    private MessagingConfig messaging;
    private SecurityConfig security;
    private MonitoringConfig monitoring;
    
    @Data
    public static class DatabaseConfig {
        private String host;
        private int port;
        private String username;
        private String password;
        private ConnectionPoolConfig connectionPool;
    }
    
    @Data
    public static class MessagingConfig {
        private String brokerUrl;
        private String username;
        private String password;
        private int maxRetries;
        private long retryDelayMs;
    }
    
    @Data
    public static class SecurityConfig {
        private String jwtSecret;
        private long jwtExpirationMs;
        private boolean enableMTLS;
        private String trustedCertificates;
    }
    
    @Data
    public static class MonitoringConfig {
        private boolean enableMetrics;
        private boolean enableTracing;
        private String jaegerEndpoint;
        private String prometheusEndpoint;
    }
}
```

### 1.2 Service Inventory

```java
// Service Registry
@Component
public class ServiceInventory {
    
    private final Map<String, ServiceInfo> services = new HashMap<>();
    
    public ServiceInventory() {
        initializeServices();
    }
    
    private void initializeServices() {
        // Core Services
        services.put("user-service", new ServiceInfo(
            "user-service",
            "User Management Service",
            "Handles user registration, authentication, and profile management",
            ServiceType.CORE,
            Arrays.asList("users", "authentication", "profiles")
        ));
        
        services.put("product-service", new ServiceInfo(
            "product-service",
            "Product Catalog Service",
            "Manages product catalog, inventory, and pricing",
            ServiceType.CORE,
            Arrays.asList("products", "catalog", "inventory")
        ));
        
        services.put("order-service", new ServiceInfo(
            "order-service",
            "Order Management Service",
            "Handles order creation, processing, and fulfillment",
            ServiceType.CORE,
            Arrays.asList("orders", "order-processing", "fulfillment")
        ));
        
        services.put("payment-service", new ServiceInfo(
            "payment-service",
            "Payment Processing Service",
            "Processes payments, refunds, and financial transactions",
            ServiceType.CORE,
            Arrays.asList("payments", "transactions", "refunds")
        ));
        
        // Supporting Services
        services.put("notification-service", new ServiceInfo(
            "notification-service",
            "Notification Service",
            "Sends emails, SMS, and push notifications",
            ServiceType.SUPPORTING,
            Arrays.asList("notifications", "email", "sms", "push")
        ));
        
        services.put("recommendation-service", new ServiceInfo(
            "recommendation-service",
            "Recommendation Engine",
            "Provides personalized product recommendations",
            ServiceType.SUPPORTING,
            Arrays.asList("recommendations", "ml", "personalization")
        ));
        
        services.put("analytics-service", new ServiceInfo(
            "analytics-service",
            "Analytics Service",
            "Collects and analyzes business metrics",
            ServiceType.SUPPORTING,
            Arrays.asList("analytics", "metrics", "reporting")
        ));
        
        // Infrastructure Services
        services.put("api-gateway", new ServiceInfo(
            "api-gateway",
            "API Gateway",
            "Routes requests and handles cross-cutting concerns",
            ServiceType.INFRASTRUCTURE,
            Arrays.asList("routing", "authentication", "rate-limiting")
        ));
        
        services.put("config-service", new ServiceInfo(
            "config-service",
            "Configuration Service",
            "Centralized configuration management",
            ServiceType.INFRASTRUCTURE,
            Arrays.asList("configuration", "feature-flags", "secrets")
        ));
        
        services.put("discovery-service", new ServiceInfo(
            "discovery-service",
            "Service Discovery",
            "Service registration and discovery",
            ServiceType.INFRASTRUCTURE,
            Arrays.asList("discovery", "registry", "health-checks")
        ));
    }
    
    public ServiceInfo getService(String serviceName) {
        return services.get(serviceName);
    }
    
    public List<ServiceInfo> getServicesByType(ServiceType type) {
        return services.values().stream()
                .filter(service -> service.getType() == type)
                .collect(Collectors.toList());
    }
    
    public List<ServiceInfo> getAllServices() {
        return new ArrayList<>(services.values());
    }
}

// Service Information
@Data
@AllArgsConstructor
public class ServiceInfo {
    private String name;
    private String displayName;
    private String description;
    private ServiceType type;
    private List<String> capabilities;
}

// Service Type
public enum ServiceType {
    CORE,
    SUPPORTING,
    INFRASTRUCTURE
}
```

---

## 2. Core Services Implementation

### 2.1 User Service

```java
// User Service Application
@SpringBootApplication
@EnableJpaRepositories
@EnableRabbitMQ
@EnableEurekaClient
@EnableCircuitBreaker
public class UserServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public JwtTokenProvider jwtTokenProvider(@Value("${jwt.secret}") String jwtSecret) {
        return new JwtTokenProvider(jwtSecret);
    }
}

// User Service Implementation
@RestController
@RequestMapping("/api/v1/users")
@Validated
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @PostMapping("/register")
    public ResponseEntity<UserRegistrationResponse> registerUser(
            @Valid @RequestBody UserRegistrationRequest request) {
        
        UserRegistrationResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody LoginRequest request) {
        
        AuthenticationResponse response = authenticationService.authenticate(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        UserResponse response = userService.getUser(id);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/validate")
    public ResponseEntity<UserValidationResponse> validateUser(@PathVariable Long id) {
        UserValidationResponse response = userService.validateUser(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}/orders")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<OrderSummary>> getUserOrders(@PathVariable Long id) {
        List<OrderSummary> orders = userService.getUserOrders(id);
        return ResponseEntity.ok(orders);
    }
}

// User Service Business Logic
@Service
@Transactional
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private EventStore eventStore;
    
    @Autowired
    private OrderServiceClient orderServiceClient;
    
    public UserRegistrationResponse registerUser(UserRegistrationRequest request) {
        // Validate user doesn't exist
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with email already exists");
        }
        
        // Create user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setHashedPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.USER);
        
        User savedUser = userRepository.save(user);
        
        // Publish user created event
        UserEvent event = UserEvent.userCreated(savedUser);
        eventStore.saveAndPublish(event);
        
        return UserRegistrationResponse.fromUser(savedUser);
    }
    
    public UserResponse getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        return UserResponse.fromUser(user);
    }
    
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        // Update user information
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setHashedPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        User savedUser = userRepository.save(user);
        
        // Publish user updated event
        UserEvent event = UserEvent.userUpdated(savedUser, savedUser.getVersion());
        eventStore.saveAndPublish(event);
        
        return UserResponse.fromUser(savedUser);
    }
    
    public UserValidationResponse validateUser(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            boolean isValid = user.getStatus() == UserStatus.ACTIVE;
            
            return new UserValidationResponse(isValid, 
                isValid ? "User is valid" : "User is not active");
        }
        
        return new UserValidationResponse(false, "User not found");
    }
    
    @HystrixCommand(fallbackMethod = "getDefaultUserOrders")
    public List<OrderSummary> getUserOrders(Long userId) {
        return orderServiceClient.getOrdersByUserId(userId);
    }
    
    public List<OrderSummary> getDefaultUserOrders(Long userId) {
        return new ArrayList<>(); // Fallback when order service is down
    }
}
```

### 2.2 Product Service

```java
// Product Service with CQRS
@SpringBootApplication
@EnableMongoRepositories
@EnableRabbitMQ
@EnableEurekaClient
@EnableCircuitBreaker
public class ProductServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
    
    @Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        return new MongoTemplate(mongoClient, "product_database");
    }
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setDefaultSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}

// Product Command Controller
@RestController
@RequestMapping("/api/v1/products/commands")
@Validated
public class ProductCommandController {
    
    @Autowired
    private ProductCommandService productCommandService;
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductCommandResponse> createProduct(
            @Valid @RequestBody CreateProductCommand command) {
        
        ProductCommandResponse response = productCommandService.createProduct(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductCommandResponse> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody UpdateProductCommand command) {
        
        command.setProductId(id);
        ProductCommandResponse response = productCommandService.updateProduct(command);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/inventory")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryUpdateResponse> updateInventory(
            @PathVariable String id,
            @Valid @RequestBody UpdateInventoryCommand command) {
        
        command.setProductId(id);
        InventoryUpdateResponse response = productCommandService.updateInventory(command);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/reserve")
    public ResponseEntity<ReservationResponse> reserveInventory(
            @PathVariable String id,
            @Valid @RequestBody ReserveInventoryCommand command) {
        
        command.setProductId(id);
        ReservationResponse response = productCommandService.reserveInventory(command);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/release")
    public ResponseEntity<Void> releaseInventory(
            @PathVariable String id,
            @Valid @RequestBody ReleaseInventoryCommand command) {
        
        command.setProductId(id);
        productCommandService.releaseInventory(command);
        return ResponseEntity.ok().build();
    }
}

// Product Query Controller
@RestController
@RequestMapping("/api/v1/products/queries")
public class ProductQueryController {
    
    @Autowired
    private ProductQueryService productQueryService;
    
    @GetMapping("/{id}")
    public ResponseEntity<ProductQueryResponse> getProduct(@PathVariable String id) {
        ProductQueryResponse response = productQueryService.getProduct(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    public ResponseEntity<PagedResponse<ProductQueryResponse>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        
        ProductSearchRequest request = new ProductSearchRequest();
        request.setPage(page);
        request.setSize(size);
        request.setCategory(category);
        request.setSearch(search);
        
        PagedResponse<ProductQueryResponse> response = productQueryService.searchProducts(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        List<CategoryResponse> categories = productQueryService.getCategories();
        return ResponseEntity.ok(categories);
    }
    
    @GetMapping("/recommendations/{userId}")
    public ResponseEntity<List<ProductQueryResponse>> getRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        
        List<ProductQueryResponse> recommendations = productQueryService.getRecommendations(userId, limit);
        return ResponseEntity.ok(recommendations);
    }
}

// Product Command Service
@Service
@Transactional
public class ProductCommandService {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private InventoryRepository inventoryRepository;
    
    @Autowired
    private EventStore eventStore;
    
    @Autowired
    private ReservationRepository reservationRepository;
    
    public ProductCommandResponse createProduct(CreateProductCommand command) {
        // Validate SKU uniqueness
        if (productRepository.existsBySku(command.getSku())) {
            throw new ProductValidationException("Product with SKU already exists");
        }
        
        // Create product
        Product product = new Product();
        product.setName(command.getName());
        product.setDescription(command.getDescription());
        product.setPrice(command.getPrice());
        product.setCategory(command.getCategory());
        product.setSku(command.getSku());
        product.setAttributes(command.getAttributes());
        product.setImages(command.getImages());
        product.setStatus(ProductStatus.ACTIVE);
        
        Product savedProduct = productRepository.save(product);
        
        // Create inventory record
        Inventory inventory = new Inventory();
        inventory.setProductId(savedProduct.getId());
        inventory.setQuantity(command.getInitialQuantity());
        inventory.setReservedQuantity(0);
        inventory.setMinStockLevel(command.getMinStockLevel());
        inventory.setMaxStockLevel(command.getMaxStockLevel());
        
        inventoryRepository.save(inventory);
        
        // Publish product created event
        ProductEvent event = ProductEvent.productCreated(savedProduct, inventory);
        eventStore.saveAndPublish(event);
        
        return ProductCommandResponse.fromProduct(savedProduct);
    }
    
    public ReservationResponse reserveInventory(ReserveInventoryCommand command) {
        // Find product and inventory
        Product product = productRepository.findById(command.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        
        Inventory inventory = inventoryRepository.findByProductId(command.getProductId())
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found"));
        
        // Check availability
        if (inventory.getAvailableQuantity() < command.getQuantity()) {
            throw new InsufficientInventoryException("Insufficient inventory");
        }
        
        // Create reservation
        InventoryReservation reservation = new InventoryReservation();
        reservation.setReservationId(UUID.randomUUID().toString());
        reservation.setProductId(command.getProductId());
        reservation.setQuantity(command.getQuantity());
        reservation.setTransactionId(command.getTransactionId());
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        
        reservationRepository.save(reservation);
        
        // Update inventory
        inventory.setReservedQuantity(inventory.getReservedQuantity() + command.getQuantity());
        inventoryRepository.save(inventory);
        
        // Publish inventory reserved event
        InventoryEvent event = InventoryEvent.inventoryReserved(product, inventory, reservation);
        eventStore.saveAndPublish(event);
        
        return ReservationResponse.fromReservation(reservation);
    }
    
    public void releaseInventory(ReleaseInventoryCommand command) {
        // Find reservation
        InventoryReservation reservation = reservationRepository.findByReservationId(command.getReservationId())
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));
        
        // Update reservation status
        reservation.setStatus(ReservationStatus.RELEASED);
        reservationRepository.save(reservation);
        
        // Update inventory
        Inventory inventory = inventoryRepository.findByProductId(reservation.getProductId())
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found"));
        
        inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
        inventoryRepository.save(inventory);
        
        // Publish inventory released event
        Product product = productRepository.findById(reservation.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        
        InventoryEvent event = InventoryEvent.inventoryReleased(product, inventory, reservation);
        eventStore.saveAndPublish(event);
    }
}
```

### 2.3 Order Service with Saga Pattern

```java
// Order Service Application
@SpringBootApplication
@EnableJpaRepositories
@EnableRabbitMQ
@EnableEurekaClient
@EnableCircuitBreaker
@EnableScheduling
public class OrderServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
    
    @Bean
    public SagaManager sagaManager() {
        return new OrderProcessingSagaManager();
    }
}

// Order Controller
@RestController
@RequestMapping("/api/v1/orders")
@Validated
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private OrderQueryService orderQueryService;
    
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<OrderCreationResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        
        OrderCreationResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        OrderResponse response = orderQueryService.getOrder(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<OrderResponse>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) OrderStatus status) {
        
        OrderSearchRequest request = new OrderSearchRequest();
        request.setPage(page);
        request.setSize(size);
        request.setUserId(userId);
        request.setStatus(status);
        
        PagedResponse<OrderResponse> response = orderQueryService.searchOrders(request);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        
        OrderResponse response = orderService.updateOrderStatus(id, request.getStatus());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return ResponseEntity.ok().build();
    }
}

// Order Saga Manager
@Component
public class OrderProcessingSagaManager {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private SagaStateRepository sagaStateRepository;
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    @Autowired
    private ProductServiceClient productServiceClient;
    
    @Autowired
    private PaymentServiceClient paymentServiceClient;
    
    @Autowired
    private NotificationServiceClient notificationServiceClient;
    
    @Autowired
    private EventStore eventStore;
    
    public SagaExecutionResult processOrder(CreateOrderRequest request) {
        String sagaId = UUID.randomUUID().toString();
        
        // Create saga state
        SagaState sagaState = new SagaState();
        sagaState.setSagaId(sagaId);
        sagaState.setSagaType(SagaType.ORDER_PROCESSING);
        sagaState.setCurrentStep(SagaStep.STARTED);
        sagaState.setStatus(SagaStatus.IN_PROGRESS);
        sagaState.setOrderRequest(request);
        
        sagaStateRepository.save(sagaState);
        
        try {
            // Step 1: Validate User
            executeStep(sagaState, this::validateUser);
            
            // Step 2: Validate and Reserve Products
            executeStep(sagaState, this::validateAndReserveProducts);
            
            // Step 3: Calculate Order Total
            executeStep(sagaState, this::calculateOrderTotal);
            
            // Step 4: Process Payment
            executeStep(sagaState, this::processPayment);
            
            // Step 5: Create Order
            executeStep(sagaState, this::createOrder);
            
            // Step 6: Send Notifications
            executeStep(sagaState, this::sendNotifications);
            
            // Step 7: Complete Saga
            completeSaga(sagaState);
            
            return SagaExecutionResult.success(sagaState.getOrderId());
            
        } catch (Exception e) {
            handleSagaFailure(sagaState, e);
            return SagaExecutionResult.failure(sagaId, e.getMessage());
        }
    }
    
    private void executeStep(SagaState sagaState, SagaStepExecutor executor) {
        try {
            executor.execute(sagaState);
            sagaStateRepository.save(sagaState);
        } catch (Exception e) {
            sagaState.setStatus(SagaStatus.FAILED);
            sagaState.setErrorMessage(e.getMessage());
            sagaStateRepository.save(sagaState);
            throw e;
        }
    }
    
    private void validateUser(SagaState sagaState) {
        CreateOrderRequest request = sagaState.getOrderRequest();
        
        UserValidationResponse validation = userServiceClient.validateUser(request.getUserId());
        
        if (!validation.isValid()) {
            throw new SagaExecutionException("User validation failed: " + validation.getReason());
        }
        
        sagaState.setCurrentStep(SagaStep.USER_VALIDATED);
        sagaState.getUserValidation().setValidated(true);
        sagaState.getUserValidation().setUserId(request.getUserId());
    }
    
    private void validateAndReserveProducts(SagaState sagaState) {
        CreateOrderRequest request = sagaState.getOrderRequest();
        List<ProductReservation> reservations = new ArrayList<>();
        
        for (CreateOrderItemRequest item : request.getItems()) {
            // Validate product exists
            ProductQueryResponse product = productServiceClient.getProduct(item.getProductId());
            if (product == null) {
                throw new SagaExecutionException("Product not found: " + item.getProductId());
            }
            
            // Reserve inventory
            ReserveInventoryCommand reserveCommand = new ReserveInventoryCommand();
            reserveCommand.setProductId(item.getProductId());
            reserveCommand.setQuantity(item.getQuantity());
            reserveCommand.setTransactionId(sagaState.getSagaId());
            
            ReservationResponse reservation = productServiceClient.reserveInventory(reserveCommand);
            reservations.add(new ProductReservation(
                reservation.getReservationId(),
                item.getProductId(),
                item.getQuantity(),
                reservation.getExpiresAt()
            ));
        }
        
        sagaState.setCurrentStep(SagaStep.PRODUCTS_RESERVED);
        sagaState.getProductReservation().setReservations(reservations);
    }
    
    private void calculateOrderTotal(SagaState sagaState) {
        CreateOrderRequest request = sagaState.getOrderRequest();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (CreateOrderItemRequest item : request.getItems()) {
            BigDecimal itemTotal = item.getUnitPrice().multiply(new BigDecimal(item.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
        }
        
        // Add tax and shipping
        BigDecimal tax = totalAmount.multiply(new BigDecimal("0.08")); // 8% tax
        BigDecimal shipping = new BigDecimal("9.99"); // Flat shipping
        totalAmount = totalAmount.add(tax).add(shipping);
        
        sagaState.setCurrentStep(SagaStep.ORDER_TOTAL_CALCULATED);
        sagaState.getOrderCalculation().setSubtotal(totalAmount.subtract(tax).subtract(shipping));
        sagaState.getOrderCalculation().setTax(tax);
        sagaState.getOrderCalculation().setShipping(shipping);
        sagaState.getOrderCalculation().setTotal(totalAmount);
    }
    
    private void processPayment(SagaState sagaState) {
        CreateOrderRequest request = sagaState.getOrderRequest();
        BigDecimal totalAmount = sagaState.getOrderCalculation().getTotal();
        
        ProcessPaymentRequest paymentRequest = new ProcessPaymentRequest();
        paymentRequest.setUserId(request.getUserId());
        paymentRequest.setAmount(totalAmount);
        paymentRequest.setPaymentMethod(request.getPaymentMethod());
        paymentRequest.setTransactionId(sagaState.getSagaId());
        
        PaymentResponse paymentResponse = paymentServiceClient.processPayment(paymentRequest);
        
        if (!paymentResponse.isSuccess()) {
            throw new SagaExecutionException("Payment failed: " + paymentResponse.getErrorMessage());
        }
        
        sagaState.setCurrentStep(SagaStep.PAYMENT_PROCESSED);
        sagaState.getPaymentInfo().setPaymentId(paymentResponse.getPaymentId());
        sagaState.getPaymentInfo().setTransactionId(paymentResponse.getTransactionId());
        sagaState.getPaymentInfo().setAmount(totalAmount);
        sagaState.getPaymentInfo().setStatus(PaymentStatus.COMPLETED);
    }
    
    private void createOrder(SagaState sagaState) {
        CreateOrderRequest request = sagaState.getOrderRequest();
        
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setStatus(OrderStatus.CONFIRMED);
        order.setSubtotal(sagaState.getOrderCalculation().getSubtotal());
        order.setTax(sagaState.getOrderCalculation().getTax());
        order.setShipping(sagaState.getOrderCalculation().getShipping());
        order.setTotalAmount(sagaState.getOrderCalculation().getTotal());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setShippingAddress(request.getShippingAddress());
        order.setBillingAddress(request.getBillingAddress());
        order.setSagaId(sagaState.getSagaId());
        
        // Create order items
        List<OrderItem> items = request.getItems().stream()
                .map(itemRequest -> {
                    OrderItem item = new OrderItem();
                    item.setOrder(order);
                    item.setProductId(itemRequest.getProductId());
                    item.setProductName(itemRequest.getProductName());
                    item.setQuantity(itemRequest.getQuantity());
                    item.setUnitPrice(itemRequest.getUnitPrice());
                    item.setTotalPrice(itemRequest.getUnitPrice().multiply(new BigDecimal(itemRequest.getQuantity())));
                    return item;
                })
                .collect(Collectors.toList());
        
        order.setItems(items);
        
        Order savedOrder = orderRepository.save(order);
        
        sagaState.setCurrentStep(SagaStep.ORDER_CREATED);
        sagaState.setOrderId(savedOrder.getId());
        
        // Publish order created event
        OrderEvent event = OrderEvent.orderCreated(savedOrder);
        eventStore.saveAndPublish(event);
    }
    
    private void sendNotifications(SagaState sagaState) {
        CreateOrderRequest request = sagaState.getOrderRequest();
        
        // Send order confirmation notification
        NotificationRequest notificationRequest = new NotificationRequest();
        notificationRequest.setUserId(request.getUserId());
        notificationRequest.setType(NotificationType.ORDER_CONFIRMATION);
        notificationRequest.setOrderId(sagaState.getOrderId());
        notificationRequest.setContent(createOrderConfirmationContent(sagaState));
        
        notificationServiceClient.sendNotification(notificationRequest);
        
        sagaState.setCurrentStep(SagaStep.NOTIFICATIONS_SENT);
    }
    
    private void completeSaga(SagaState sagaState) {
        sagaState.setStatus(SagaStatus.COMPLETED);
        sagaState.setCurrentStep(SagaStep.COMPLETED);
        sagaState.setCompletedAt(LocalDateTime.now());
        sagaStateRepository.save(sagaState);
    }
    
    private void handleSagaFailure(SagaState sagaState, Exception e) {
        sagaState.setStatus(SagaStatus.FAILED);
        sagaState.setErrorMessage(e.getMessage());
        sagaState.setFailedAt(LocalDateTime.now());
        sagaStateRepository.save(sagaState);
        
        // Execute compensating actions
        compensate(sagaState);
    }
    
    private void compensate(SagaState sagaState) {
        // Compensate based on how far we got
        switch (sagaState.getCurrentStep()) {
            case NOTIFICATIONS_SENT:
            case ORDER_CREATED:
                // Cancel order if created
                if (sagaState.getOrderId() != null) {
                    try {
                        orderRepository.deleteById(sagaState.getOrderId());
                    } catch (Exception e) {
                        logger.error("Failed to cancel order {}: {}", sagaState.getOrderId(), e.getMessage());
                    }
                }
                // Fall through to refund payment
            case PAYMENT_PROCESSED:
                // Refund payment
                if (sagaState.getPaymentInfo().getPaymentId() != null) {
                    try {
                        RefundPaymentRequest refundRequest = new RefundPaymentRequest();
                        refundRequest.setPaymentId(sagaState.getPaymentInfo().getPaymentId());
                        refundRequest.setAmount(sagaState.getPaymentInfo().getAmount());
                        refundRequest.setReason("Order processing failed");
                        
                        paymentServiceClient.refundPayment(refundRequest);
                    } catch (Exception e) {
                        logger.error("Failed to refund payment {}: {}", sagaState.getPaymentInfo().getPaymentId(), e.getMessage());
                    }
                }
                // Fall through to release inventory
            case ORDER_TOTAL_CALCULATED:
            case PRODUCTS_RESERVED:
                // Release inventory reservations
                if (sagaState.getProductReservation().getReservations() != null) {
                    for (ProductReservation reservation : sagaState.getProductReservation().getReservations()) {
                        try {
                            ReleaseInventoryCommand releaseCommand = new ReleaseInventoryCommand();
                            releaseCommand.setProductId(reservation.getProductId());
                            releaseCommand.setReservationId(reservation.getReservationId());
                            releaseCommand.setTransactionId(sagaState.getSagaId());
                            
                            productServiceClient.releaseInventory(releaseCommand);
                        } catch (Exception e) {
                            logger.error("Failed to release inventory reservation {}: {}", 
                                reservation.getReservationId(), e.getMessage());
                        }
                    }
                }
                break;
            default:
                // No compensation needed
                break;
        }
    }
    
    private String createOrderConfirmationContent(SagaState sagaState) {
        return String.format(
            "Your order #%d has been confirmed. Total: $%.2f. Thank you for your purchase!",
            sagaState.getOrderId(),
            sagaState.getOrderCalculation().getTotal()
        );
    }
    
    @FunctionalInterface
    private interface SagaStepExecutor {
        void execute(SagaState sagaState) throws Exception;
    }
}
```

### 2.4 Payment Service

```java
// Payment Service Application
@SpringBootApplication
@EnableJpaRepositories
@EnableRabbitMQ
@EnableEurekaClient
@EnableCircuitBreaker
public class PaymentServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
    
    @Bean
    public PaymentGatewayProvider paymentGatewayProvider() {
        return new StripePaymentGatewayProvider();
    }
}

// Payment Controller
@RestController
@RequestMapping("/api/v1/payments")
@Validated
public class PaymentController {
    
    @Autowired
    private PaymentService paymentService;
    
    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(
            @Valid @RequestBody ProcessPaymentRequest request) {
        
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/refund")
    public ResponseEntity<RefundResponse> refundPayment(
            @Valid @RequestBody RefundPaymentRequest request) {
        
        RefundResponse response = paymentService.refundPayment(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PaymentDetailsResponse> getPayment(@PathVariable String id) {
        PaymentDetailsResponse response = paymentService.getPaymentDetails(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentHistoryResponse>> getPaymentHistory(@PathVariable Long userId) {
        List<PaymentHistoryResponse> history = paymentService.getPaymentHistory(userId);
        return ResponseEntity.ok(history);
    }
}

// Payment Service Implementation
@Service
@Transactional
public class PaymentService {
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private PaymentGatewayProvider paymentGatewayProvider;
    
    @Autowired
    private EventStore eventStore;
    
    @Autowired
    private FraudDetectionService fraudDetectionService;
    
    public PaymentResponse processPayment(ProcessPaymentRequest request) {
        // Validate payment request
        validatePaymentRequest(request);
        
        // Check for fraud
        FraudAssessment fraudAssessment = fraudDetectionService.assessPayment(request);
        if (fraudAssessment.isFraudulent()) {
            throw new PaymentFraudException("Payment flagged as fraudulent");
        }
        
        // Create payment record
        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID().toString());
        payment.setUserId(request.getUserId());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setTransactionId(request.getTransactionId());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setGatewayTransactionId(null);
        payment.setFraudScore(fraudAssessment.getFraudScore());
        
        Payment savedPayment = paymentRepository.save(payment);
        
        try {
            // Process payment through gateway
            GatewayPaymentRequest gatewayRequest = new GatewayPaymentRequest();
            gatewayRequest.setPaymentId(payment.getPaymentId());
            gatewayRequest.setAmount(request.getAmount());
            gatewayRequest.setPaymentMethod(request.getPaymentMethod());
            gatewayRequest.setCurrency("USD");
            gatewayRequest.setDescription("Order payment");
            
            GatewayPaymentResponse gatewayResponse = paymentGatewayProvider.processPayment(gatewayRequest);
            
            if (gatewayResponse.isSuccess()) {
                // Update payment status
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setGatewayTransactionId(gatewayResponse.getTransactionId());
                payment.setProcessedAt(LocalDateTime.now());
                
                Payment finalPayment = paymentRepository.save(payment);
                
                // Publish payment processed event
                PaymentEvent event = PaymentEvent.paymentProcessed(finalPayment);
                eventStore.saveAndPublish(event);
                
                return PaymentResponse.success(finalPayment);
                
            } else {
                // Payment failed
                payment.setStatus(PaymentStatus.FAILED);
                payment.setErrorMessage(gatewayResponse.getErrorMessage());
                payment.setProcessedAt(LocalDateTime.now());
                
                Payment finalPayment = paymentRepository.save(payment);
                
                // Publish payment failed event
                PaymentEvent event = PaymentEvent.paymentFailed(finalPayment);
                eventStore.saveAndPublish(event);
                
                return PaymentResponse.failure(finalPayment, gatewayResponse.getErrorMessage());
            }
            
        } catch (Exception e) {
            // Handle payment processing error
            payment.setStatus(PaymentStatus.ERROR);
            payment.setErrorMessage(e.getMessage());
            payment.setProcessedAt(LocalDateTime.now());
            
            Payment finalPayment = paymentRepository.save(payment);
            
            // Publish payment error event
            PaymentEvent event = PaymentEvent.paymentError(finalPayment);
            eventStore.saveAndPublish(event);
            
            throw new PaymentProcessingException("Payment processing failed", e);
        }
    }
    
    public RefundResponse refundPayment(RefundPaymentRequest request) {
        // Find original payment
        Payment originalPayment = paymentRepository.findByPaymentId(request.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));
        
        // Validate refund request
        validateRefundRequest(request, originalPayment);
        
        // Create refund record
        PaymentRefund refund = new PaymentRefund();
        refund.setRefundId(UUID.randomUUID().toString());
        refund.setOriginalPaymentId(originalPayment.getPaymentId());
        refund.setAmount(request.getAmount());
        refund.setReason(request.getReason());
        refund.setStatus(RefundStatus.PENDING);
        
        try {
            // Process refund through gateway
            GatewayRefundRequest gatewayRequest = new GatewayRefundRequest();
            gatewayRequest.setRefundId(refund.getRefundId());
            gatewayRequest.setOriginalTransactionId(originalPayment.getGatewayTransactionId());
            gatewayRequest.setAmount(request.getAmount());
            gatewayRequest.setReason(request.getReason());
            
            GatewayRefundResponse gatewayResponse = paymentGatewayProvider.processRefund(gatewayRequest);
            
            if (gatewayResponse.isSuccess()) {
                refund.setStatus(RefundStatus.COMPLETED);
                refund.setGatewayRefundId(gatewayResponse.getRefundId());
                refund.setProcessedAt(LocalDateTime.now());
                
                // Update original payment
                originalPayment.setRefundedAmount(
                    originalPayment.getRefundedAmount().add(request.getAmount()));
                
                paymentRepository.save(originalPayment);
                
                // Publish refund processed event
                PaymentEvent event = PaymentEvent.refundProcessed(originalPayment, refund);
                eventStore.saveAndPublish(event);
                
                return RefundResponse.success(refund);
                
            } else {
                refund.setStatus(RefundStatus.FAILED);
                refund.setErrorMessage(gatewayResponse.getErrorMessage());
                refund.setProcessedAt(LocalDateTime.now());
                
                return RefundResponse.failure(refund, gatewayResponse.getErrorMessage());
            }
            
        } catch (Exception e) {
            refund.setStatus(RefundStatus.ERROR);
            refund.setErrorMessage(e.getMessage());
            refund.setProcessedAt(LocalDateTime.now());
            
            throw new RefundProcessingException("Refund processing failed", e);
        }
    }
    
    private void validatePaymentRequest(ProcessPaymentRequest request) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentValidationException("Payment amount must be positive");
        }
        
        if (request.getAmount().compareTo(new BigDecimal("10000")) > 0) {
            throw new PaymentValidationException("Payment amount exceeds maximum limit");
        }
        
        if (request.getPaymentMethod() == null) {
            throw new PaymentValidationException("Payment method is required");
        }
    }
    
    private void validateRefundRequest(RefundPaymentRequest request, Payment originalPayment) {
        if (originalPayment.getStatus() != PaymentStatus.COMPLETED) {
            throw new RefundValidationException("Cannot refund non-completed payment");
        }
        
        BigDecimal maxRefundAmount = originalPayment.getAmount().subtract(originalPayment.getRefundedAmount());
        if (request.getAmount().compareTo(maxRefundAmount) > 0) {
            throw new RefundValidationException("Refund amount exceeds available amount");
        }
        
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RefundValidationException("Refund amount must be positive");
        }
    }
}
```

---

## 3. Infrastructure Services

### 3.1 API Gateway Configuration

```java
// API Gateway Main Application
@SpringBootApplication
@EnableEurekaClient
@EnableZuulProxy
public class ApiGatewayApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
    
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // User Service Routes
            .route("user-service", r -> r.path("/api/v1/users/**")
                .filters(f -> f
                    .addRequestHeader("X-Service", "user-service")
                    .addResponseHeader("X-Response-Time", String.valueOf(System.currentTimeMillis()))
                    .circuitBreaker(config -> config
                        .setName("user-service")
                        .setFallbackUri("forward:/fallback/user-service")))
                .uri("lb://user-service"))
            
            // Product Service Routes
            .route("product-service", r -> r.path("/api/v1/products/**")
                .filters(f -> f
                    .addRequestHeader("X-Service", "product-service")
                    .addResponseHeader("X-Response-Time", String.valueOf(System.currentTimeMillis()))
                    .circuitBreaker(config -> config
                        .setName("product-service")
                        .setFallbackUri("forward:/fallback/product-service")))
                .uri("lb://product-service"))
            
            // Order Service Routes
            .route("order-service", r -> r.path("/api/v1/orders/**")
                .filters(f -> f
                    .addRequestHeader("X-Service", "order-service")
                    .addResponseHeader("X-Response-Time", String.valueOf(System.currentTimeMillis()))
                    .requestRateLimiter(config -> config
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(userKeyResolver()))
                    .circuitBreaker(config -> config
                        .setName("order-service")
                        .setFallbackUri("forward:/fallback/order-service")))
                .uri("lb://order-service"))
            
            // Payment Service Routes
            .route("payment-service", r -> r.path("/api/v1/payments/**")
                .filters(f -> f
                    .addRequestHeader("X-Service", "payment-service")
                    .addResponseHeader("X-Response-Time", String.valueOf(System.currentTimeMillis()))
                    .requestRateLimiter(config -> config
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(userKeyResolver()))
                    .circuitBreaker(config -> config
                        .setName("payment-service")
                        .setFallbackUri("forward:/fallback/payment-service")))
                .uri("lb://payment-service"))
            
            .build();
    }
    
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20); // 10 requests per second, burst of 20
    }
    
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> exchange.getRequest().getHeaders().getFirst("X-User-ID") != null
            ? Mono.just(exchange.getRequest().getHeaders().getFirst("X-User-ID"))
            : Mono.just("anonymous");
    }
}

// Authentication Filter
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        
        // Skip authentication for public endpoints
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }
        
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorizedResponse(exchange);
        }
        
        String token = authHeader.substring(7);
        
        try {
            if (jwtTokenProvider.validateToken(token)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                String userRole = jwtTokenProvider.getUserRoleFromToken(token);
                
                // Add user info to request headers
                ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-ID", userId.toString())
                    .header("X-User-Role", userRole)
                    .build();
                
                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            } else {
                return unauthorizedResponse(exchange);
            }
        } catch (Exception e) {
            return unauthorizedResponse(exchange);
        }
    }
    
    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/v1/users/register") ||
               path.startsWith("/api/v1/users/login") ||
               path.startsWith("/api/v1/products/queries") ||
               path.startsWith("/health") ||
               path.startsWith("/actuator");
    }
    
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        
        String body = "{\"error\":\"Unauthorized\",\"message\":\"Invalid or missing authentication token\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes());
        
        return response.writeWith(Mono.just(buffer));
    }
    
    @Override
    public int getOrder() {
        return -1; // High priority
    }
}

// Fallback Controller
@RestController
@RequestMapping("/fallback")
public class FallbackController {
    
    @GetMapping("/user-service")
    public ResponseEntity<Map<String, Object>> userServiceFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "User service is temporarily unavailable");
        response.put("message", "Please try again later");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
    
    @GetMapping("/product-service")
    public ResponseEntity<Map<String, Object>> productServiceFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Product service is temporarily unavailable");
        response.put("message", "Please try again later");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
    
    @GetMapping("/order-service")
    public ResponseEntity<Map<String, Object>> orderServiceFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Order service is temporarily unavailable");
        response.put("message", "Please try again later");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
    
    @GetMapping("/payment-service")
    public ResponseEntity<Map<String, Object>> paymentServiceFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Payment service is temporarily unavailable");
        response.put("message", "Please try again later");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
```

---

## 4. Monitoring and Observability

### 4.1 Distributed Tracing

```java
// Tracing Configuration
@Configuration
@EnableZipkinServer
public class TracingConfig {
    
    @Bean
    public Sender sender() {
        return OkHttpSender.create("http://zipkin:9411/api/v2/spans");
    }
    
    @Bean
    public AsyncReporter<Span> spanReporter() {
        return AsyncReporter.create(sender());
    }
    
    @Bean
    public Tracing tracing() {
        return Tracing.newBuilder()
            .localServiceName("ecommerce-platform")
            .spanReporter(spanReporter())
            .sampler(Sampler.create(1.0f)) // Sample all requests
            .build();
    }
    
    @Bean
    public HttpTracing httpTracing(Tracing tracing) {
        return HttpTracing.create(tracing);
    }
}

// Custom Tracing Interceptor
@Component
public class TracingInterceptor implements HandlerInterceptor {
    
    private final Tracing tracing;
    private final TraceContext.Injector<HttpServletRequest> injector;
    
    public TracingInterceptor(Tracing tracing) {
        this.tracing = tracing;
        this.injector = tracing.propagation().injector(HttpServletRequest::getHeader);
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Span span = tracing.tracer().nextSpan()
            .name(request.getMethod() + " " + request.getRequestURI())
            .tag("http.method", request.getMethod())
            .tag("http.url", request.getRequestURL().toString())
            .tag("service.name", getServiceName())
            .start();
        
        try (Tracer.SpanInScope ws = tracing.tracer().withSpanInScope(span)) {
            request.setAttribute("tracing.span", span);
            return true;
        }
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Span span = (Span) request.getAttribute("tracing.span");
        if (span != null) {
            span.tag("http.status_code", String.valueOf(response.getStatus()));
            if (ex != null) {
                span.tag("error", ex.getMessage());
            }
            span.end();
        }
    }
    
    private String getServiceName() {
        return System.getProperty("spring.application.name", "unknown-service");
    }
}
```

### 4.2 Metrics Collection

```java
// Metrics Configuration
@Configuration
@EnablePrometheusEndpoint
@EnableMetricsEndpoint
public class MetricsConfig {
    
    @Bean
    public MeterRegistry meterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }
    
    @Bean
    public TimedAspect timedAspect(MeterRegistry meterRegistry) {
        return new TimedAspect(meterRegistry);
    }
    
    @Bean
    public CountedAspect countedAspect(MeterRegistry meterRegistry) {
        return new CountedAspect(meterRegistry);
    }
}

// Custom Metrics Service
@Service
public class MetricsService {
    
    private final MeterRegistry meterRegistry;
    private final Counter orderCreatedCounter;
    private final Counter paymentProcessedCounter;
    private final Timer orderProcessingTimer;
    private final Gauge activeUsersGauge;
    
    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.orderCreatedCounter = Counter.builder("orders.created")
            .description("Number of orders created")
            .tag("service", "order-service")
            .register(meterRegistry);
        
        this.paymentProcessedCounter = Counter.builder("payments.processed")
            .description("Number of payments processed")
            .tag("service", "payment-service")
            .register(meterRegistry);
        
        this.orderProcessingTimer = Timer.builder("order.processing.time")
            .description("Order processing time")
            .tag("service", "order-service")
            .register(meterRegistry);
        
        this.activeUsersGauge = Gauge.builder("users.active")
            .description("Number of active users")
            .tag("service", "user-service")
            .register(meterRegistry, this, MetricsService::getActiveUsersCount);
    }
    
    public void recordOrderCreated(String status) {
        orderCreatedCounter.increment(Tags.of("status", status));
    }
    
    public void recordPaymentProcessed(String method, String status) {
        paymentProcessedCounter.increment(Tags.of("method", method, "status", status));
    }
    
    public Timer.Sample startOrderProcessingTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordOrderProcessingTime(Timer.Sample sample, String status) {
        sample.stop(Timer.builder("order.processing.time")
            .tag("status", status)
            .register(meterRegistry));
    }
    
    public void recordCustomMetric(String name, double value, String... tags) {
        meterRegistry.gauge(name, Tags.of(tags), value);
    }
    
    private double getActiveUsersCount() {
        // Implementation to get active users count
        return 1000.0; // Placeholder
    }
}

// Metrics Event Listener
@Component
public class MetricsEventListener {
    
    @Autowired
    private MetricsService metricsService;
    
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        metricsService.recordOrderCreated("created");
    }
    
    @EventListener
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        metricsService.recordPaymentProcessed(
            event.getPaymentMethod(),
            event.isSuccess() ? "success" : "failed"
        );
    }
    
    @EventListener
    public void handleOrderCompleted(OrderCompletedEvent event) {
        metricsService.recordOrderCreated("completed");
    }
}
```

---

## 5. Deployment and Configuration

### 5.1 Docker Configuration

```dockerfile
# Base Docker image for Java services
FROM openjdk:11-jre-slim

# Set working directory
WORKDIR /app

# Copy application jar
COPY target/*.jar app.jar

# Add application user
RUN addgroup --system --gid 1001 appuser
RUN adduser --system --uid 1001 --gid 1001 appuser

# Change ownership of the app directory
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Set JVM options
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

```yaml
# docker-compose.yml
version: '3.8'

services:
  # Infrastructure Services
  eureka-server:
    image: ecommerce/eureka-server:latest
    ports:
      - "8761:8761"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    networks:
      - ecommerce-network

  api-gateway:
    image: ecommerce/api-gateway:latest
    ports:
      - "8080:8080"
    depends_on:
      - eureka-server
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka
    networks:
      - ecommerce-network

  # Core Services
  user-service:
    image: ecommerce/user-service:latest
    depends_on:
      - eureka-server
      - postgres-user
      - rabbitmq
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-user:5432/userdb
      - SPRING_RABBITMQ_HOST=rabbitmq
    networks:
      - ecommerce-network

  product-service:
    image: ecommerce/product-service:latest
    depends_on:
      - eureka-server
      - mongodb
      - rabbitmq
      - redis
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka
      - SPRING_DATA_MONGODB_URI=mongodb://mongodb:27017/productdb
      - SPRING_RABBITMQ_HOST=rabbitmq
      - SPRING_REDIS_HOST=redis
    networks:
      - ecommerce-network

  order-service:
    image: ecommerce/order-service:latest
    depends_on:
      - eureka-server
      - postgres-order
      - rabbitmq
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-order:5432/orderdb
      - SPRING_RABBITMQ_HOST=rabbitmq
    networks:
      - ecommerce-network

  payment-service:
    image: ecommerce/payment-service:latest
    depends_on:
      - eureka-server
      - postgres-payment
      - rabbitmq
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-payment:5432/paymentdb
      - SPRING_RABBITMQ_HOST=rabbitmq
    networks:
      - ecommerce-network

  notification-service:
    image: ecommerce/notification-service:latest
    depends_on:
      - eureka-server
      - rabbitmq
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka
      - SPRING_RABBITMQ_HOST=rabbitmq
    networks:
      - ecommerce-network

  # Databases
  postgres-user:
    image: postgres:13
    environment:
      - POSTGRES_DB=userdb
      - POSTGRES_USER=userservice
      - POSTGRES_PASSWORD=userpass
    volumes:
      - postgres-user-data:/var/lib/postgresql/data
    networks:
      - ecommerce-network

  postgres-order:
    image: postgres:13
    environment:
      - POSTGRES_DB=orderdb
      - POSTGRES_USER=orderservice
      - POSTGRES_PASSWORD=orderpass
    volumes:
      - postgres-order-data:/var/lib/postgresql/data
    networks:
      - ecommerce-network

  postgres-payment:
    image: postgres:13
    environment:
      - POSTGRES_DB=paymentdb
      - POSTGRES_USER=paymentservice
      - POSTGRES_PASSWORD=paymentpass
    volumes:
      - postgres-payment-data:/var/lib/postgresql/data
    networks:
      - ecommerce-network

  mongodb:
    image: mongo:4.4
    environment:
      - MONGO_INITDB_ROOT_USERNAME=admin
      - MONGO_INITDB_ROOT_PASSWORD=adminpass
      - MONGO_INITDB_DATABASE=productdb
    volumes:
      - mongodb-data:/data/db
    networks:
      - ecommerce-network

  # Message Broker
  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "15672:15672"
    environment:
      - RABBITMQ_DEFAULT_USER=admin
      - RABBITMQ_DEFAULT_PASS=adminpass
    volumes:
      - rabbitmq-data:/var/lib/rabbitmq
    networks:
      - ecommerce-network

  # Cache
  redis:
    image: redis:6-alpine
    volumes:
      - redis-data:/data
    networks:
      - ecommerce-network

  # Monitoring
  zipkin:
    image: openzipkin/zipkin
    ports:
      - "9411:9411"
    networks:
      - ecommerce-network

  prometheus:
    image: prom/prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    networks:
      - ecommerce-network

  grafana:
    image: grafana/grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana-data:/var/lib/grafana
    networks:
      - ecommerce-network

volumes:
  postgres-user-data:
  postgres-order-data:
  postgres-payment-data:
  mongodb-data:
  rabbitmq-data:
  redis-data:
  grafana-data:

networks:
  ecommerce-network:
    driver: bridge
```

---

## 6. Testing Strategy

### 6.1 Integration Tests

```java
// Integration Test Base Class
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
@Testcontainers
public abstract class IntegrationTestBase {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
    
    @Container
    static MongoDBContainer mongodb = new MongoDBContainer("mongo:4.4")
        .withExposedPorts(27017);
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:6-alpine")
        .withExposedPorts(6379);
    
    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management")
        .withExposedPorts(5672, 15672);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        registry.add("spring.data.mongodb.uri", mongodb::getReplicaSetUrl);
        
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
        
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getFirstMappedPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);
    }
}

// Order Service Integration Test
@SpringBootTest
class OrderServiceIntegrationTest extends IntegrationTestBase {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @MockBean
    private UserServiceClient userServiceClient;
    
    @MockBean
    private ProductServiceClient productServiceClient;
    
    @MockBean
    private PaymentServiceClient paymentServiceClient;
    
    @Test
    void shouldCreateOrderSuccessfully() {
        // Given
        CreateOrderRequest request = createValidOrderRequest();
        
        // Mock external service calls
        when(userServiceClient.validateUser(anyLong()))
            .thenReturn(new UserValidationResponse(true, "Valid user"));
        
        when(productServiceClient.getProduct(anyString()))
            .thenReturn(createValidProductResponse());
        
        when(productServiceClient.reserveInventory(any()))
            .thenReturn(createValidReservationResponse());
        
        when(paymentServiceClient.processPayment(any()))
            .thenReturn(createValidPaymentResponse());
        
        // When
        ResponseEntity<OrderCreationResponse> response = restTemplate.postForEntity(
            "/api/v1/orders", request, OrderCreationResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getOrderId()).isNotNull();
        
        // Verify order was created in database
        Optional<Order> savedOrder = orderRepository.findById(response.getBody().getOrderId());
        assertThat(savedOrder).isPresent();
        assertThat(savedOrder.get().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }
    
    @Test
    void shouldFailOrderCreationWhenUserInvalid() {
        // Given
        CreateOrderRequest request = createValidOrderRequest();
        
        when(userServiceClient.validateUser(anyLong()))
            .thenReturn(new UserValidationResponse(false, "Invalid user"));
        
        // When
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            "/api/v1/orders", request, ErrorResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("User validation failed");
    }
    
    @Test
    void shouldHandlePaymentFailureGracefully() {
        // Given
        CreateOrderRequest request = createValidOrderRequest();
        
        when(userServiceClient.validateUser(anyLong()))
            .thenReturn(new UserValidationResponse(true, "Valid user"));
        
        when(productServiceClient.getProduct(anyString()))
            .thenReturn(createValidProductResponse());
        
        when(productServiceClient.reserveInventory(any()))
            .thenReturn(createValidReservationResponse());
        
        when(paymentServiceClient.processPayment(any()))
            .thenReturn(createFailedPaymentResponse());
        
        // When
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            "/api/v1/orders", request, ErrorResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("Payment failed");
        
        // Verify compensating actions were called
        verify(productServiceClient).releaseInventory(any());
    }
    
    private CreateOrderRequest createValidOrderRequest() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(1L);
        request.setPaymentMethod("CREDIT_CARD");
        request.setShippingAddress("123 Main St, City, State 12345");
        request.setBillingAddress("123 Main St, City, State 12345");
        
        CreateOrderItemRequest item = new CreateOrderItemRequest();
        item.setProductId("product-1");
        item.setProductName("Test Product");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("29.99"));
        
        request.setItems(Arrays.asList(item));
        
        return request;
    }
    
    private ProductQueryResponse createValidProductResponse() {
        ProductQueryResponse response = new ProductQueryResponse();
        response.setId("product-1");
        response.setName("Test Product");
        response.setPrice(new BigDecimal("29.99"));
        response.setAvailableQuantity(100);
        return response;
    }
    
    private ReservationResponse createValidReservationResponse() {
        ReservationResponse response = new ReservationResponse();
        response.setReservationId("reservation-1");
        response.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        response.setSuccess(true);
        return response;
    }
    
    private PaymentResponse createValidPaymentResponse() {
        PaymentResponse response = new PaymentResponse();
        response.setPaymentId("payment-1");
        response.setSuccess(true);
        return response;
    }
    
    private PaymentResponse createFailedPaymentResponse() {
        PaymentResponse response = new PaymentResponse();
        response.setSuccess(false);
        response.setErrorMessage("Payment processing failed");
        return response;
    }
}
```

### 6.2 Contract Testing

```java
// Contract Test for User Service
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@PactTestFor(providerName = "user-service")
public class UserServiceContractTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }
    
    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", 8080, "/"));
    }
    
    @State("user exists")
    public void userExists() {
        // Setup test data for user exists scenario
        // This would typically involve creating test data in the database
    }
    
    @State("user does not exist")
    public void userDoesNotExist() {
        // Setup test data for user does not exist scenario
        // This would typically involve ensuring no user data exists
    }
}

// Contract Test for Product Service
@SpringBootTest
@PactTestFor(providerName = "product-service")
public class ProductServiceContractTest {
    
    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }
    
    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", 8081, "/"));
    }
    
    @State("product exists")
    public void productExists() {
        // Setup test data for product exists scenario
    }
    
    @State("product has sufficient inventory")
    public void productHasSufficientInventory() {
        // Setup test data for sufficient inventory scenario
    }
    
    @State("product has insufficient inventory")
    public void productHasInsufficientInventory() {
        // Setup test data for insufficient inventory scenario
    }
}
```

---

## 7. Performance and Load Testing

### 7.1 Load Testing with JMeter

```xml
<!-- JMeter Test Plan for Order Creation -->
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2" properties="5.0" jmeter="5.4.1">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="E-commerce Load Test" enabled="true">
      <stringProp name="TestPlan.comments">Load test for e-commerce platform</stringProp>
      <boolProp name="TestPlan.functional_mode">false</boolProp>
      <boolProp name="TestPlan.serialize_threadgroups">false</boolProp>
      <elementProp name="TestPlan.arguments" elementType="Arguments" guiclass="ArgumentsPanel" testclass="Arguments" testname="User Defined Variables" enabled="true">
        <collectionProp name="Arguments.arguments">
          <elementProp name="base_url" elementType="Argument">
            <stringProp name="Argument.name">base_url</stringProp>
            <stringProp name="Argument.value">http://localhost:8080</stringProp>
          </elementProp>
        </collectionProp>
      </elementProp>
      <stringProp name="TestPlan.user_define_classpath"></stringProp>
    </TestPlan>
    <hashTree>
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="Order Creation Load Test" enabled="true">
        <stringProp name="ThreadGroup.on_sample_error">continue</stringProp>
        <elementProp name="ThreadGroup.main_controller" elementType="LoopController" guiclass="LoopControlPanel" testclass="LoopController" testname="Loop Controller" enabled="true">
          <boolProp name="LoopController.continue_forever">false</boolProp>
          <stringProp name="LoopController.loops">10</stringProp>
        </elementProp>
        <stringProp name="ThreadGroup.num_threads">100</stringProp>
        <stringProp name="ThreadGroup.ramp_time">60</stringProp>
        <longProp name="ThreadGroup.start_time">1</longProp>
        <longProp name="ThreadGroup.end_time">1</longProp>
        <boolProp name="ThreadGroup.scheduler">false</boolProp>
        <stringProp name="ThreadGroup.duration"></stringProp>
        <stringProp name="ThreadGroup.delay"></stringProp>
      </ThreadGroup>
      <hashTree>
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="Create Order" enabled="true">
          <elementProp name="HTTPsampler.Arguments" elementType="Arguments" guiclass="HTTPArgumentsPanel" testclass="Arguments" testname="User Defined Variables" enabled="true">
            <collectionProp name="Arguments.arguments">
              <elementProp name="" elementType="HTTPArgument">
                <boolProp name="HTTPArgument.always_encode">false</boolProp>
                <stringProp name="Argument.value">{
  "userId": 1,
  "paymentMethod": "CREDIT_CARD",
  "shippingAddress": "123 Main St, City, State 12345",
  "billingAddress": "123 Main St, City, State 12345",
  "items": [
    {
      "productId": "product-1",
      "productName": "Test Product",
      "quantity": 2,
      "unitPrice": 29.99
    }
  ]
}</stringProp>
                <stringProp name="Argument.metadata">=</stringProp>
              </elementProp>
            </collectionProp>
          </elementProp>
          <stringProp name="HTTPSampler.domain">localhost</stringProp>
          <stringProp name="HTTPSampler.port">8080</stringProp>
          <stringProp name="HTTPSampler.protocol">http</stringProp>
          <stringProp name="HTTPSampler.contentEncoding"></stringProp>
          <stringProp name="HTTPSampler.path">/api/v1/orders</stringProp>
          <stringProp name="HTTPSampler.method">POST</stringProp>
          <boolProp name="HTTPSampler.follow_redirects">true</boolProp>
          <boolProp name="HTTPSampler.auto_redirects">false</boolProp>
          <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
          <boolProp name="HTTPSampler.DO_MULTIPART_POST">false</boolProp>
          <stringProp name="HTTPSampler.embedded_url_re"></stringProp>
          <stringProp name="HTTPSampler.connect_timeout"></stringProp>
          <stringProp name="HTTPSampler.response_timeout"></stringProp>
        </HTTPSamplerProxy>
        <hashTree>
          <HeaderManager guiclass="HeaderPanel" testclass="HeaderManager" testname="HTTP Header Manager" enabled="true">
            <collectionProp name="HeaderManager.headers">
              <elementProp name="" elementType="Header">
                <stringProp name="Header.name">Content-Type</stringProp>
                <stringProp name="Header.value">application/json</stringProp>
              </elementProp>
              <elementProp name="" elementType="Header">
                <stringProp name="Header.name">Authorization</stringProp>
                <stringProp name="Header.value">Bearer ${access_token}</stringProp>
              </elementProp>
            </collectionProp>
          </HeaderManager>
          <hashTree/>
          <ResponseAssertion guiclass="AssertionGui" testclass="ResponseAssertion" testname="Response Assertion" enabled="true">
            <collectionProp name="Asserion.test_strings">
              <stringProp name="49586">201</stringProp>
            </collectionProp>
            <stringProp name="Assertion.test_field">Assertion.response_code</stringProp>
            <boolProp name="Assertion.assume_success">false</boolProp>
            <intProp name="Assertion.test_type">1</intProp>
          </ResponseAssertion>
          <hashTree/>
        </hashTree>
      </hashTree>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
```

---

## 8. Security Implementation

### 8.1 OAuth2 and JWT Security

```java
// Security Configuration
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    
    @Autowired
    private JwtRequestFilter jwtRequestFilter;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors().and().csrf().disable()
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/v1/users/register", "/api/v1/users/login").permitAll()
                .requestMatchers("/api/v1/products/queries/**").permitAll()
                .requestMatchers("/health", "/actuator/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/orders").hasRole("USER")
                .requestMatchers("/api/v1/orders/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/v1/payments/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/v1/products/commands/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}

// JWT Token Provider
@Component
public class JwtTokenProvider {
    
    private String jwtSecret;
    private int jwtExpirationInMs;
    
    public JwtTokenProvider(@Value("${jwt.secret}") String jwtSecret,
                           @Value("${jwt.expiration:86400000}") int jwtExpirationInMs) {
        this.jwtSecret = jwtSecret;
        this.jwtExpirationInMs = jwtExpirationInMs;
    }
    
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", ((UserPrincipal) userDetails).getId());
        claims.put("role", userDetails.getAuthorities().iterator().next().getAuthority());
        return createToken(claims, userDetails.getUsername());
    }
    
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
            return true;
        } catch (SignatureException | MalformedJwtException | ExpiredJwtException | 
                 UnsupportedJwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody();
        return claims.getSubject();
    }
    
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody();
        return Long.parseLong(claims.get("userId").toString());
    }
    
    public String getUserRoleFromToken(String token) {
        Claims claims = Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody();
        return claims.get("role").toString();
    }
}
```

---

## 9. Key Architecture Decisions and Patterns

### 9.1 Design Patterns Used

1. **Saga Pattern**: For distributed transaction management
2. **CQRS**: For separating read and write operations
3. **Event Sourcing**: For audit trail and data consistency
4. **Circuit Breaker**: For fault tolerance and resilience
5. **API Gateway**: For centralized routing and cross-cutting concerns
6. **Event-Driven Architecture**: For loose coupling and async communication

### 9.2 Technology Stack

- **Spring Boot**: Microservice framework
- **Spring Cloud**: Service discovery, circuit breakers, API gateway
- **PostgreSQL**: Relational database for transactional data
- **MongoDB**: Document database for product catalog
- **Redis**: Caching and session storage
- **RabbitMQ**: Message broker for async communication
- **Docker**: Containerization
- **Kubernetes**: Orchestration (production deployment)
- **Zipkin**: Distributed tracing
- **Prometheus + Grafana**: Monitoring and alerting

### 9.3 Best Practices Implemented

✅ **Database per Service**: Each service has its own database  
✅ **API Versioning**: All APIs are versioned (/api/v1/)  
✅ **Circuit Breaker Pattern**: Implemented for external service calls  
✅ **Centralized Logging**: Structured logging with correlation IDs  
✅ **Health Checks**: Comprehensive health monitoring  
✅ **Security**: JWT-based authentication and authorization  
✅ **Testing**: Unit, integration, and contract testing  
✅ **Monitoring**: Metrics, tracing, and observability  

---

## 10. Week 9 Summary and Key Takeaways

### Architecture Principles Mastered:
1. **Microservices Decomposition**: Breaking monoliths into focused services
2. **Data Management**: Database per service with event-driven synchronization
3. **Communication Patterns**: Sync/async communication with fallback strategies
4. **Resilience**: Circuit breakers, retries, and compensation patterns
5. **Observability**: Distributed tracing, metrics, and centralized logging

### Real-World Application:
This capstone project demonstrates a production-ready e-commerce platform that handles:
- **100+ concurrent users** with responsive performance
- **Fault tolerance** with graceful degradation
- **Data consistency** across distributed services
- **Scalability** through independent service scaling
- **Security** with enterprise-grade authentication

### Next Steps for Weeks 10-12:
- **Week 10**: Spring Cloud ecosystem deep dive
- **Week 11**: Advanced messaging and event streaming
- **Week 12**: Production deployment and DevOps practices

---

**Practice Assignment**: Deploy this e-commerce platform locally using Docker Compose and simulate various failure scenarios to test the resilience patterns.

---

*"A well-designed microservices architecture is like a symphony orchestra - each service plays its part independently, but together they create something greater than the sum of their parts."*

*"The true test of microservices architecture is not how it performs when everything works, but how gracefully it handles failures and unexpected conditions."*