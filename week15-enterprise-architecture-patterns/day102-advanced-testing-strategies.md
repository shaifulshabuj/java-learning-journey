# Day 102: Advanced Testing Strategies - Contract Testing, Chaos Engineering & Property-Based Testing

## Learning Objectives
- Master contract testing for microservices communication
- Implement chaos engineering practices for resilience testing
- Understand property-based testing principles and implementation
- Design comprehensive testing strategies for distributed systems
- Build automated testing pipelines with advanced testing techniques

## Topics Covered
1. Contract Testing with Pact and Spring Cloud Contract
2. Consumer-Driven Contract Testing
3. Chaos Engineering Fundamentals and Implementation
4. Property-Based Testing with jqwik
5. Advanced Integration Testing Patterns
6. Performance and Load Testing Strategies
7. Testing in Production Techniques

## 1. Contract Testing Fundamentals

### Consumer-Driven Contract Testing with Pact

Contract testing ensures that services can communicate with each other by verifying that the consumer's expectations match the provider's implementation.

```java
// Consumer side - Order Service testing Payment Service contract
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "payment-service", port = "8080")
class PaymentServiceContractTest {
    
    @Pact(consumer = "order-service")
    public RequestResponsePact processPaymentContract(PactDslWithProvider builder) {
        return builder
                .given("payment processor is available")
                .uponReceiving("a payment request for valid order")
                .path("/api/payments")
                .method("POST")
                .headers(Map.of("Content-Type", "application/json"))
                .body(LambdaDsl.newJsonBody(body -> body
                        .stringType("orderId", "order-123")
                        .stringType("traderId", "trader-456")
                        .decimalType("amount", new BigDecimal("1000.00"))
                        .stringType("currency", "USD")
                        .stringType("paymentMethod", "CREDIT_CARD")
                ).build())
                .willRespondWith()
                .status(201)
                .headers(Map.of("Content-Type", "application/json"))
                .body(LambdaDsl.newJsonBody(body -> body
                        .stringType("paymentId", "payment-789")
                        .stringType("orderId", "order-123")
                        .stringType("status", "APPROVED")
                        .stringType("transactionId", "txn-abc123")
                        .decimalType("amount", new BigDecimal("1000.00"))
                        .datetime("processedAt", "yyyy-MM-dd'T'HH:mm:ss.SSSX")
                ).build())
                .toPact();
    }
    
    @Test
    @PactTestFor(pactMethod = "processPaymentContract")
    void shouldProcessPaymentSuccessfully(MockServer mockServer) {
        // Arrange
        PaymentServiceClient paymentClient = new PaymentServiceClient(
                mockServer.getUrl());
        
        PaymentRequest request = new PaymentRequest(
                "order-123",
                "trader-456", 
                new BigDecimal("1000.00"),
                "USD",
                "CREDIT_CARD"
        );
        
        // Act
        PaymentResponse response = paymentClient.processPayment(request);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getPaymentId()).isEqualTo("payment-789");
        assertThat(response.getOrderId()).isEqualTo("order-123");
        assertThat(response.getStatus()).isEqualTo("APPROVED");
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(response.getTransactionId()).isEqualTo("txn-abc123");
        assertThat(response.getProcessedAt()).isNotNull();
    }
    
    @Pact(consumer = "order-service")
    public RequestResponsePact processPaymentFailureContract(PactDslWithProvider builder) {
        return builder
                .given("payment processor is available")
                .uponReceiving("a payment request with insufficient funds")
                .path("/api/payments")
                .method("POST")
                .headers(Map.of("Content-Type", "application/json"))
                .body(LambdaDsl.newJsonBody(body -> body
                        .stringType("orderId", "order-456")
                        .stringType("traderId", "trader-789")
                        .decimalType("amount", new BigDecimal("10000.00"))
                        .stringType("currency", "USD")
                        .stringType("paymentMethod", "CREDIT_CARD")
                ).build())
                .willRespondWith()
                .status(400)
                .headers(Map.of("Content-Type", "application/json"))
                .body(LambdaDsl.newJsonBody(body -> body
                        .stringType("error", "INSUFFICIENT_FUNDS")
                        .stringType("message", "Insufficient funds for payment")
                        .stringType("orderId", "order-456")
                ).build())
                .toPact();
    }
    
    @Test
    @PactTestFor(pactMethod = "processPaymentFailureContract")
    void shouldHandleInsufficientFundsError(MockServer mockServer) {
        // Arrange
        PaymentServiceClient paymentClient = new PaymentServiceClient(
                mockServer.getUrl());
        
        PaymentRequest request = new PaymentRequest(
                "order-456",
                "trader-789",
                new BigDecimal("10000.00"),
                "USD",
                "CREDIT_CARD"
        );
        
        // Act & Assert
        PaymentException exception = assertThrows(
                PaymentException.class,
                () -> paymentClient.processPayment(request)
        );
        
        assertThat(exception.getErrorCode()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(exception.getMessage()).contains("Insufficient funds");
        assertThat(exception.getOrderId()).isEqualTo("order-456");
    }
}

// Payment Service Client
@Component
public class PaymentServiceClient {
    
    private final WebClient webClient;
    private final Logger logger = LoggerFactory.getLogger(PaymentServiceClient.class);
    
    public PaymentServiceClient(@Value("${payment.service.url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
    
    // Constructor for testing
    public PaymentServiceClient(String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
    
    public PaymentResponse processPayment(PaymentRequest request) {
        try {
            return webClient.post()
                    .uri("/api/payments")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> {
                        return response.bodyToMono(PaymentErrorResponse.class)
                                .map(error -> new PaymentException(
                                        error.getError(),
                                        error.getMessage(),
                                        error.getOrderId()
                                ));
                    })
                    .bodyToMono(PaymentResponse.class)
                    .block();
        } catch (Exception e) {
            logger.error("Payment processing failed for order: {}", request.getOrderId(), e);
            throw new PaymentException("COMMUNICATION_ERROR", 
                    "Failed to communicate with payment service", 
                    request.getOrderId());
        }
    }
}

// DTOs
public class PaymentRequest {
    private String orderId;
    private String traderId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    
    public PaymentRequest() {}
    
    public PaymentRequest(String orderId, String traderId, BigDecimal amount, 
                         String currency, String paymentMethod) {
        this.orderId = orderId;
        this.traderId = traderId;
        this.amount = amount;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
    }
    
    // Getters and setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getTraderId() { return traderId; }
    public void setTraderId(String traderId) { this.traderId = traderId; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}

public class PaymentResponse {
    private String paymentId;
    private String orderId;
    private String status;
    private String transactionId;
    private BigDecimal amount;
    private Instant processedAt;
    
    public PaymentResponse() {}
    
    public PaymentResponse(String paymentId, String orderId, String status, 
                          String transactionId, BigDecimal amount, Instant processedAt) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.status = status;
        this.transactionId = transactionId;
        this.amount = amount;
        this.processedAt = processedAt;
    }
    
    // Getters and setters
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}

public class PaymentErrorResponse {
    private String error;
    private String message;
    private String orderId;
    
    public PaymentErrorResponse() {}
    
    public PaymentErrorResponse(String error, String message, String orderId) {
        this.error = error;
        this.message = message;
        this.orderId = orderId;
    }
    
    // Getters and setters
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
}

public class PaymentException extends RuntimeException {
    private final String errorCode;
    private final String orderId;
    
    public PaymentException(String errorCode, String message, String orderId) {
        super(message);
        this.errorCode = errorCode;
        this.orderId = orderId;
    }
    
    public String getErrorCode() { return errorCode; }
    public String getOrderId() { return orderId; }
}
```

### Provider Side Contract Testing

```java
// Provider side - Payment Service verifying contracts
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("payment-service")
@PactFolder("target/pacts")
class PaymentServiceProviderTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private PaymentService paymentService;
    
    @MockBean
    private PaymentProcessor paymentProcessor;
    
    @BeforeEach
    void setUp(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }
    
    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }
    
    @State("payment processor is available")
    void paymentProcessorIsAvailable() {
        // Mock successful payment processing
        when(paymentProcessor.processPayment(any()))
                .thenReturn(new ProcessorResponse("txn-abc123", "APPROVED"));
    }
    
    @State("payment processor returns insufficient funds")
    void paymentProcessorReturnsInsufficientFunds() {
        // Mock insufficient funds error
        when(paymentProcessor.processPayment(any()))
                .thenThrow(new ProcessorException("INSUFFICIENT_FUNDS", 
                        "Insufficient funds for payment"));
    }
}

// Payment Service Implementation
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    private final PaymentService paymentService;
    private final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    @PostMapping
    public ResponseEntity<?> processPayment(@RequestBody @Valid PaymentRequest request) {
        try {
            PaymentResponse response = paymentService.processPayment(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (InsufficientFundsException e) {
            PaymentErrorResponse error = new PaymentErrorResponse(
                    "INSUFFICIENT_FUNDS",
                    "Insufficient funds for payment",
                    request.getOrderId()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Payment processing failed", e);
            PaymentErrorResponse error = new PaymentErrorResponse(
                    "PROCESSING_ERROR",
                    "Payment processing failed",
                    request.getOrderId()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}

@Service
public class PaymentService {
    
    private final PaymentProcessor paymentProcessor;
    private final PaymentRepository paymentRepository;
    private final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    
    public PaymentService(PaymentProcessor paymentProcessor, PaymentRepository paymentRepository) {
        this.paymentProcessor = paymentProcessor;
        this.paymentRepository = paymentRepository;
    }
    
    public PaymentResponse processPayment(PaymentRequest request) {
        try {
            ProcessorResponse processorResponse = paymentProcessor.processPayment(request);
            
            Payment payment = new Payment(
                    UUID.randomUUID().toString(),
                    request.getOrderId(),
                    request.getTraderId(),
                    request.getAmount(),
                    request.getCurrency(),
                    processorResponse.getTransactionId(),
                    "APPROVED",
                    Instant.now()
            );
            
            paymentRepository.save(payment);
            
            return new PaymentResponse(
                    payment.getPaymentId(),
                    payment.getOrderId(),
                    payment.getStatus(),
                    payment.getTransactionId(),
                    payment.getAmount(),
                    payment.getProcessedAt()
            );
        } catch (ProcessorException e) {
            if ("INSUFFICIENT_FUNDS".equals(e.getErrorCode())) {
                throw new InsufficientFundsException(e.getMessage());
            }
            throw new PaymentProcessingException("Payment processing failed", e);
        }
    }
}
```

## 2. Chaos Engineering

### Chaos Engineering Framework

```java
// Chaos Engineering annotations and framework
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ChaosTest {
    ChaosType[] value() default {};
    double probability() default 0.1;
    long duration() default 5000; // milliseconds
}

public enum ChaosType {
    LATENCY_INJECTION,
    ERROR_INJECTION,
    RESOURCE_EXHAUSTION,
    NETWORK_PARTITION,
    SERVICE_UNAVAILABLE
}

// Chaos Engineering Aspect
@Aspect
@Component
public class ChaosEngineeringAspect {
    
    private final Random random = new Random();
    private final Logger logger = LoggerFactory.getLogger(ChaosEngineeringAspect.class);
    
    @Around("@annotation(chaosTest)")
    public Object applyChaos(ProceedingJoinPoint joinPoint, ChaosTest chaosTest) throws Throwable {
        if (!shouldApplyChaos(chaosTest.probability())) {
            return joinPoint.proceed();
        }
        
        ChaosType chaosType = selectChaosType(chaosTest.value());
        logger.info("Applying chaos: {} to method: {}", chaosType, 
                joinPoint.getSignature().getName());
        
        return switch (chaosType) {
            case LATENCY_INJECTION -> applyLatencyInjection(joinPoint, chaosTest.duration());
            case ERROR_INJECTION -> applyErrorInjection(joinPoint);
            case RESOURCE_EXHAUSTION -> applyResourceExhaustion(joinPoint);
            case SERVICE_UNAVAILABLE -> applyServiceUnavailable(joinPoint);
            default -> joinPoint.proceed();
        };
    }
    
    private boolean shouldApplyChaos(double probability) {
        return random.nextDouble() < probability;
    }
    
    private ChaosType selectChaosType(ChaosType[] types) {
        if (types.length == 0) {
            ChaosType[] allTypes = ChaosType.values();
            return allTypes[random.nextInt(allTypes.length)];
        }
        return types[random.nextInt(types.length)];
    }
    
    private Object applyLatencyInjection(ProceedingJoinPoint joinPoint, long duration) 
            throws Throwable {
        long delay = random.nextLong(duration);
        logger.info("Injecting latency: {}ms", delay);
        
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return joinPoint.proceed();
    }
    
    private Object applyErrorInjection(ProceedingJoinPoint joinPoint) throws Throwable {
        logger.info("Injecting error");
        throw new ChaosEngineeringException("Chaos engineering error injection");
    }
    
    private Object applyResourceExhaustion(ProceedingJoinPoint joinPoint) throws Throwable {
        logger.info("Simulating resource exhaustion");
        
        // Simulate memory pressure
        List<byte[]> memoryConsumer = new ArrayList<>();
        try {
            for (int i = 0; i < 100; i++) {
                memoryConsumer.add(new byte[1024 * 1024]); // 1MB each
            }
            return joinPoint.proceed();
        } finally {
            memoryConsumer.clear();
        }
    }
    
    private Object applyServiceUnavailable(ProceedingJoinPoint joinPoint) throws Throwable {
        logger.info("Simulating service unavailable");
        throw new ServiceUnavailableException("Service temporarily unavailable");
    }
}

// Chaos Engineering Configuration
@Configuration
@EnableAspectJAutoProxy
public class ChaosEngineeringConfiguration {
    
    @Bean
    @ConditionalOnProperty(name = "chaos.engineering.enabled", havingValue = "true")
    public ChaosEngineeringAspect chaosEngineeringAspect() {
        return new ChaosEngineeringAspect();
    }
}

// Chaos Engineering Tests
@SpringBootTest
@TestPropertySource(properties = {"chaos.engineering.enabled=true"})
class OrderServiceChaosTest {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private PaymentServiceClient paymentServiceClient;
    
    @Test
    @RepeatedTest(10)
    void shouldHandlePaymentServiceLatency() {
        // Arrange
        String orderId = UUID.randomUUID().toString();
        PlaceOrderRequest request = new PlaceOrderRequest(
                "trader-123", "AAPL", new BigDecimal("100"), 
                new BigDecimal("150.00"), OrderSide.BUY, OrderType.LIMIT
        );
        
        // Act & Assert - Service should handle latency gracefully
        assertDoesNotThrow(() -> {
            OrderResponse response = orderService.placeOrder(orderId, request);
            assertThat(response).isNotNull();
            assertThat(response.getOrderId()).isEqualTo(orderId);
        });
    }
    
    @Test
    @RepeatedTest(10)
    void shouldHandlePaymentServiceErrors() {
        // Arrange
        String orderId = UUID.randomUUID().toString();
        PlaceOrderRequest request = new PlaceOrderRequest(
                "trader-456", "GOOGL", new BigDecimal("50"),
                new BigDecimal("2500.00"), OrderSide.BUY, OrderType.LIMIT
        );
        
        // Act & Assert - Service should handle errors with retry/fallback
        try {
            OrderResponse response = orderService.placeOrder(orderId, request);
            assertThat(response).isNotNull();
        } catch (Exception e) {
            // Verify proper error handling
            assertThat(e).isInstanceOfAny(
                    ChaosEngineeringException.class,
                    ServiceUnavailableException.class,
                    PaymentException.class
            );
        }
    }
}

// Resilient Order Service with Circuit Breaker
@Service
public class ResilientOrderService {
    
    private final PaymentServiceClient paymentServiceClient;
    private final OrderRepository orderRepository;
    private final CircuitBreaker circuitBreaker;
    private final RetryTemplate retryTemplate;
    private final Logger logger = LoggerFactory.getLogger(ResilientOrderService.class);
    
    public ResilientOrderService(PaymentServiceClient paymentServiceClient,
                               OrderRepository orderRepository,
                               CircuitBreakerRegistry circuitBreakerRegistry) {
        this.paymentServiceClient = paymentServiceClient;
        this.orderRepository = orderRepository;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("payment-service");
        this.retryTemplate = createRetryTemplate();
    }
    
    @ChaosTest(value = {ChaosType.LATENCY_INJECTION, ChaosType.ERROR_INJECTION}, 
               probability = 0.2)
    public OrderResponse placeOrder(String orderId, PlaceOrderRequest request) {
        Order order = new Order(orderId, request.getTraderId(), request.getSymbol(),
                request.getQuantity(), request.getPrice(), request.getSide(), 
                request.getOrderType());
        
        order = orderRepository.save(order);
        
        try {
            // Process payment with circuit breaker and retry
            PaymentResponse paymentResponse = circuitBreaker.executeSupplier(() ->
                    retryTemplate.execute(context -> {
                        PaymentRequest paymentRequest = new PaymentRequest(
                                orderId, request.getTraderId(),
                                request.getQuantity().multiply(request.getPrice()),
                                "USD", "CREDIT_CARD"
                        );
                        return paymentServiceClient.processPayment(paymentRequest);
                    })
            );
            
            order.setPaymentId(paymentResponse.getPaymentId());
            order.setStatus(OrderStatus.PAID);
            order = orderRepository.save(order);
            
            return new OrderResponse(order.getOrderId(), order.getStatus().name(), 
                    paymentResponse.getPaymentId());
            
        } catch (Exception e) {
            logger.error("Payment processing failed for order: {}", orderId, e);
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            orderRepository.save(order);
            
            // Fallback: Queue for manual processing
            queueForManualProcessing(order);
            
            return new OrderResponse(order.getOrderId(), order.getStatus().name(), null);
        }
    }
    
    private RetryTemplate createRetryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(3)
                .exponentialBackoff(1000, 2, 10000)
                .retryOn(PaymentException.class)
                .retryOn(ChaosEngineeringException.class)
                .build();
    }
    
    private void queueForManualProcessing(Order order) {
        // Queue order for manual processing
        logger.info("Queuing order for manual processing: {}", order.getOrderId());
    }
}

// Custom exceptions
public class ChaosEngineeringException extends RuntimeException {
    public ChaosEngineeringException(String message) {
        super(message);
    }
}

public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}
```

## 3. Property-Based Testing with jqwik

### Property-Based Testing Implementation

```java
// Property-based testing for order validation
class OrderValidationPropertyTest {
    
    @Property
    void orderQuantityMustBePositive(@ForAll @Positive BigDecimal quantity) {
        // Given
        Order order = createOrderWithQuantity(quantity);
        
        // When & Then
        assertThat(order.getQuantity()).isPositive();
        assertThat(order.isValid()).isTrue();
    }
    
    @Property
    void orderPriceMustBePositive(@ForAll @Positive BigDecimal price) {
        // Given
        Order order = createOrderWithPrice(price);
        
        // When & Then
        assertThat(order.getPrice()).isPositive();
        assertThat(order.isValid()).isTrue();
    }
    
    @Property
    void orderWithZeroOrNegativeQuantityShouldBeInvalid(
            @ForAll @BigRange(max = "0") BigDecimal quantity) {
        // Given
        Order order = createOrderWithQuantity(quantity);
        
        // When & Then
        assertThat(order.isValid()).isFalse();
    }
    
    @Property
    void orderTotalValueCalculation(
            @ForAll @Positive @BigRange(max = "1000") BigDecimal quantity,
            @ForAll @Positive @BigRange(max = "10000") BigDecimal price) {
        // Given
        Order order = createOrder(quantity, price);
        
        // When
        BigDecimal totalValue = order.getTotalValue();
        
        // Then
        BigDecimal expectedValue = quantity.multiply(price);
        assertThat(totalValue).isEqualByComparingTo(expectedValue);
        assertThat(totalValue).isPositive();
    }
    
    @Property
    void executionsShouldNotExceedOrderQuantity(
            @ForAll @Positive @BigRange(max = "1000") BigDecimal orderQuantity,
            @ForAll("executionQuantities") List<@Positive BigDecimal> executionQuantities) {
        
        // Given
        Order order = createOrderWithQuantity(orderQuantity);
        
        // When - Apply executions
        BigDecimal totalExecuted = BigDecimal.ZERO;
        for (BigDecimal execQty : executionQuantities) {
            if (order.canExecute(execQty)) {
                order.addExecution(createExecution(execQty));
                totalExecuted = totalExecuted.add(execQty);
            } else {
                break;
            }
        }
        
        // Then
        assertThat(order.getFilledQuantity()).isEqualByComparingTo(totalExecuted);
        assertThat(order.getFilledQuantity()).isLessThanOrEqualTo(orderQuantity);
        assertThat(order.getRemainingQuantity()).isEqualByComparingTo(
                orderQuantity.subtract(totalExecuted));
    }
    
    @Provide
    Arbitrary<List<BigDecimal>> executionQuantities() {
        return Arbitraries.integers()
                .between(1, 100)
                .map(BigDecimal::new)
                .list()
                .ofMaxSize(20);
    }
    
    @Property
    void orderStatusTransitionsShouldBeValid(
            @ForAll("validOrderStatusTransitions") List<OrderStatus> statusTransitions) {
        // Given
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING);
        
        // When & Then
        OrderStatus currentStatus = OrderStatus.PENDING;
        for (OrderStatus nextStatus : statusTransitions) {
            if (isValidTransition(currentStatus, nextStatus)) {
                assertDoesNotThrow(() -> order.setStatus(nextStatus));
                currentStatus = nextStatus;
            } else {
                assertThrows(IllegalStateException.class, 
                        () -> order.setStatus(nextStatus));
            }
        }
    }
    
    @Provide
    Arbitrary<List<OrderStatus>> validOrderStatusTransitions() {
        return Arbitraries.of(OrderStatus.class)
                .list()
                .ofMaxSize(10);
    }
    
    @Property
    void portfolioPositionCalculationShouldBeConsistent(
            @ForAll("portfolioTrades") List<Trade> trades) {
        // Given
        Portfolio portfolio = new Portfolio("trader-123");
        
        // When
        trades.forEach(portfolio::addTrade);
        
        // Then
        Map<String, BigDecimal> positions = portfolio.getPositions();
        
        // Verify position calculations
        for (Map.Entry<String, BigDecimal> position : positions.entrySet()) {
            String symbol = position.getKey();
            BigDecimal calculatedPosition = calculateExpectedPosition(trades, symbol);
            assertThat(position.getValue()).isEqualByComparingTo(calculatedPosition);
        }
        
        // Verify portfolio balance
        BigDecimal totalValue = portfolio.getTotalValue();
        assertThat(totalValue).isNotNull();
    }
    
    @Provide
    Arbitrary<List<Trade>> portfolioTrades() {
        return Arbitraries.of("AAPL", "GOOGL", "MSFT", "TSLA")
                .flatMap(symbol -> 
                        Combinators.combine(
                                Arbitraries.just(symbol),
                                Arbitraries.integers().between(1, 1000).map(BigDecimal::new),
                                Arbitraries.bigDecimals().between(
                                        BigDecimal.valueOf(10), BigDecimal.valueOf(1000)),
                                Arbitraries.of(OrderSide.class)
                        ).as(Trade::new)
                )
                .list()
                .ofMaxSize(50);
    }
    
    // Helper methods
    private Order createOrderWithQuantity(BigDecimal quantity) {
        return createOrder(quantity, BigDecimal.valueOf(100));
    }
    
    private Order createOrderWithPrice(BigDecimal price) {
        return createOrder(BigDecimal.valueOf(100), price);
    }
    
    private Order createOrder(BigDecimal quantity, BigDecimal price) {
        return new Order(
                UUID.randomUUID().toString(),
                "trader-123",
                "AAPL",
                quantity,
                price,
                OrderSide.BUY,
                OrderType.LIMIT
        );
    }
    
    private Execution createExecution(BigDecimal quantity) {
        return new Execution(
                UUID.randomUUID().toString(),
                quantity,
                BigDecimal.valueOf(100),
                Instant.now()
        );
    }
    
    private boolean isValidTransition(OrderStatus from, OrderStatus to) {
        return switch (from) {
            case PENDING -> to == OrderStatus.PARTIALLY_FILLED || 
                          to == OrderStatus.FILLED || 
                          to == OrderStatus.CANCELLED;
            case PARTIALLY_FILLED -> to == OrderStatus.FILLED || 
                                   to == OrderStatus.CANCELLED;
            case FILLED, CANCELLED -> false; // Terminal states
            default -> false;
        };
    }
    
    private BigDecimal calculateExpectedPosition(List<Trade> trades, String symbol) {
        return trades.stream()
                .filter(trade -> trade.getSymbol().equals(symbol))
                .map(trade -> trade.getSide() == OrderSide.BUY ? 
                        trade.getQuantity() : trade.getQuantity().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

// Supporting classes for property-based testing
public class Trade {
    private final String symbol;
    private final BigDecimal quantity;
    private final BigDecimal price;
    private final OrderSide side;
    
    public Trade(String symbol, BigDecimal quantity, BigDecimal price, OrderSide side) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.side = side;
    }
    
    // Getters
    public String getSymbol() { return symbol; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public OrderSide getSide() { return side; }
}

public class Portfolio {
    private final String traderId;
    private final Map<String, BigDecimal> positions = new HashMap<>();
    
    public Portfolio(String traderId) {
        this.traderId = traderId;
    }
    
    public void addTrade(Trade trade) {
        String symbol = trade.getSymbol();
        BigDecimal quantity = trade.getSide() == OrderSide.BUY ? 
                trade.getQuantity() : trade.getQuantity().negate();
        
        positions.merge(symbol, quantity, BigDecimal::add);
    }
    
    public Map<String, BigDecimal> getPositions() {
        return Collections.unmodifiableMap(positions);
    }
    
    public BigDecimal getTotalValue() {
        // Simplified calculation
        return positions.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public String getTraderId() { return traderId; }
}
```

## 4. Advanced Integration Testing

### Test Containers Integration

```java
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("trading_test")
            .withUsername("test")
            .withPassword("test");
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);
    
    @Container
    static MockServerContainer mockServer = new MockServerContainer(
            DockerImageName.parse("mockserver/mockserver:5.15.0"));
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private OrderRepository orderRepository;
    
    private MockServerClient mockServerClient;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
        registry.add("payment.service.url", 
                () -> "http://localhost:" + mockServer.getServerPort());
    }
    
    @BeforeEach
    void setUp() {
        mockServerClient = new MockServerClient(
                mockServer.getHost(), mockServer.getServerPort());
        
        // Set up payment service mock
        mockServerClient
                .when(request()
                        .withMethod("POST")
                        .withPath("/api/payments")
                        .withHeader("Content-Type", "application/json"))
                .respond(response()
                        .withStatusCode(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "paymentId": "payment-123",
                                    "orderId": "${json-unit.matches:id}",
                                    "status": "APPROVED",
                                    "transactionId": "txn-456",
                                    "amount": "${json-unit.matches:amount}",
                                    "processedAt": "${json-unit.matches:processedAt}"
                                }
                                """));
    }
    
    @Test
    @Transactional
    @Rollback
    void shouldPlaceOrderWithPaymentProcessing() {
        // Given
        PlaceOrderRequest request = new PlaceOrderRequest(
                "trader-123", "AAPL", new BigDecimal("100"),
                new BigDecimal("150.00"), OrderSide.BUY, OrderType.LIMIT
        );
        
        // When
        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
                "/api/orders", request, OrderResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getOrderId()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("PAID");
        
        // Verify order persisted
        Optional<Order> savedOrder = orderRepository.findById(response.getBody().getOrderId());
        assertThat(savedOrder).isPresent();
        assertThat(savedOrder.get().getStatus()).isEqualTo(OrderStatus.PAID);
        
        // Verify payment service was called
        mockServerClient.verify(
                request()
                        .withMethod("POST")
                        .withPath("/api/payments"),
                VerificationTimes.exactly(1)
        );
    }
    
    @Test
    void shouldHandlePaymentServiceFailure() {
        // Given
        mockServerClient.reset();
        mockServerClient
                .when(request()
                        .withMethod("POST")
                        .withPath("/api/payments"))
                .respond(response()
                        .withStatusCode(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "error": "INSUFFICIENT_FUNDS",
                                    "message": "Insufficient funds for payment",
                                    "orderId": "${json-unit.matches:orderId}"
                                }
                                """));
        
        PlaceOrderRequest request = new PlaceOrderRequest(
                "trader-456", "GOOGL", new BigDecimal("50"),
                new BigDecimal("2500.00"), OrderSide.BUY, OrderType.LIMIT
        );
        
        // When
        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
                "/api/orders", request, OrderResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getStatus()).isEqualTo("PAYMENT_FAILED");
    }
}
```

## 5. Performance and Load Testing

### JMH Microbenchmarks

```java
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 3)
@Measurement(iterations = 5)
public class OrderProcessingBenchmark {
    
    private OrderService orderService;
    private List<PlaceOrderRequest> orderRequests;
    
    @Setup
    public void setup() {
        // Initialize order service with mocked dependencies
        orderService = new OrderService(
                mock(PaymentServiceClient.class),
                mock(OrderRepository.class)
        );
        
        // Generate test data
        orderRequests = generateOrderRequests(1000);
    }
    
    @Benchmark
    public void benchmarkOrderPlacement() {
        PlaceOrderRequest request = orderRequests.get(
                ThreadLocalRandom.current().nextInt(orderRequests.size()));
        
        String orderId = UUID.randomUUID().toString();
        orderService.placeOrder(orderId, request);
    }
    
    @Benchmark
    public void benchmarkOrderValidation() {
        PlaceOrderRequest request = orderRequests.get(
                ThreadLocalRandom.current().nextInt(orderRequests.size()));
        
        Order order = new Order(
                UUID.randomUUID().toString(),
                request.getTraderId(),
                request.getSymbol(),
                request.getQuantity(),
                request.getPrice(),
                request.getSide(),
                request.getOrderType()
        );
        
        order.isValid();
    }
    
    @Benchmark
    public void benchmarkPortfolioCalculation() {
        Portfolio portfolio = new Portfolio("trader-123");
        
        // Add random trades
        for (int i = 0; i < 100; i++) {
            Trade trade = generateRandomTrade();
            portfolio.addTrade(trade);
        }
        
        portfolio.getTotalValue();
    }
    
    private List<PlaceOrderRequest> generateOrderRequests(int count) {
        List<PlaceOrderRequest> requests = new ArrayList<>();
        String[] symbols = {"AAPL", "GOOGL", "MSFT", "TSLA", "AMZN"};
        
        for (int i = 0; i < count; i++) {
            requests.add(new PlaceOrderRequest(
                    "trader-" + (i % 100),
                    symbols[i % symbols.length],
                    BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(1, 1000)),
                    BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(10, 1000)),
                    ThreadLocalRandom.current().nextBoolean() ? OrderSide.BUY : OrderSide.SELL,
                    OrderType.LIMIT
            ));
        }
        
        return requests;
    }
    
    private Trade generateRandomTrade() {
        String[] symbols = {"AAPL", "GOOGL", "MSFT", "TSLA", "AMZN"};
        return new Trade(
                symbols[ThreadLocalRandom.current().nextInt(symbols.length)],
                BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(1, 1000)),
                BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(10, 1000)),
                ThreadLocalRandom.current().nextBoolean() ? OrderSide.BUY : OrderSide.SELL
        );
    }
}
```

### Load Testing with JUnit and Gatling

```java
@Test
@Timeout(60)
void loadTestOrderPlacement() throws InterruptedException {
    int numberOfThreads = 10;
    int ordersPerThread = 100;
    CountDownLatch latch = new CountDownLatch(numberOfThreads);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger errorCount = new AtomicInteger(0);
    
    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
    
    for (int i = 0; i < numberOfThreads; i++) {
        executor.submit(() -> {
            try {
                for (int j = 0; j < ordersPerThread; j++) {
                    try {
                        PlaceOrderRequest request = createRandomOrderRequest();
                        String orderId = UUID.randomUUID().toString();
                        
                        OrderResponse response = orderService.placeOrder(orderId, request);
                        assertThat(response).isNotNull();
                        successCount.incrementAndGet();
                        
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    }
                }
            } finally {
                latch.countDown();
            }
        });
    }
    
    boolean finished = latch.await(50, TimeUnit.SECONDS);
    executor.shutdown();
    
    assertThat(finished).isTrue();
    assertThat(successCount.get()).isGreaterThan(numberOfThreads * ordersPerThread * 0.9);
    assertThat(errorCount.get()).isLessThan(numberOfThreads * ordersPerThread * 0.1);
}
```

## Key Takeaways

1. **Contract Testing**: Ensures service compatibility and prevents integration failures
2. **Chaos Engineering**: Tests system resilience under adverse conditions
3. **Property-Based Testing**: Validates behavior across wide range of inputs
4. **Test Containers**: Enables realistic integration testing with real services
5. **Performance Testing**: Identifies bottlenecks and validates performance requirements
6. **Load Testing**: Verifies system behavior under high concurrency
7. **Testing in Production**: Validates real-world system behavior

These advanced testing strategies ensure robust, resilient, and performant distributed systems that can handle production workloads and failure scenarios effectively.