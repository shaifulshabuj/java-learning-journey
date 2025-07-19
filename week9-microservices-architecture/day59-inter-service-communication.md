# Day 59: Inter-Service Communication

## Learning Objectives
- Master synchronous and asynchronous communication patterns
- Implement REST APIs for microservices
- Understand message-based communication
- Apply circuit breaker and retry mechanisms
- Design resilient communication strategies

---

## 1. Communication Patterns Overview

### 1.1 Synchronous vs Asynchronous Communication

```java
// Synchronous Communication Example
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        // This is synchronous - waits for response
        Order order = orderService.createOrder(request);
        return ResponseEntity.ok(OrderResponse.from(order));
    }
}

@Service
public class OrderService {
    
    @Autowired
    private CustomerServiceClient customerServiceClient;
    
    @Autowired
    private InventoryServiceClient inventoryServiceClient;
    
    @Autowired
    private PaymentServiceClient paymentServiceClient;
    
    public Order createOrder(CreateOrderRequest request) {
        // Synchronous calls - each blocks until response
        Customer customer = customerServiceClient.getCustomer(request.getCustomerId());
        
        boolean inventoryAvailable = inventoryServiceClient.checkInventory(
            request.getProductId(), request.getQuantity());
        
        if (!inventoryAvailable) {
            throw new InsufficientInventoryException("Product not available");
        }
        
        PaymentResult payment = paymentServiceClient.processPayment(
            request.getPaymentDetails());
        
        if (!payment.isSuccessful()) {
            throw new PaymentFailedException("Payment processing failed");
        }
        
        // Create and return order
        return createOrderEntity(request, customer, payment);
    }
}

// Asynchronous Communication Example
@Service
public class AsyncOrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Async
    public CompletableFuture<Order> createOrderAsync(CreateOrderRequest request) {
        // Create order immediately
        Order order = Order.builder()
            .customerId(request.getCustomerId())
            .status(OrderStatus.PENDING)
            .totalAmount(request.getTotalAmount())
            .build();
        
        Order savedOrder = orderRepository.save(order);
        
        // Publish event for async processing
        eventPublisher.publishEvent(new OrderCreatedEvent(savedOrder.getId()));
        
        return CompletableFuture.completedFuture(savedOrder);
    }
    
    @EventListener
    @Async
    public void handleOrderCreated(OrderCreatedEvent event) {
        // Process order asynchronously
        processOrderAsync(event.getOrderId());
    }
    
    private void processOrderAsync(Long orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
            
            // Async validation and processing
            validateCustomerAsync(order.getCustomerId())
                .thenCompose(customer -> validateInventoryAsync(order))
                .thenCompose(inventory -> processPaymentAsync(order))
                .thenAccept(payment -> completeOrder(order, payment))
                .exceptionally(ex -> {
                    handleOrderFailure(order, ex);
                    return null;
                });
                
        } catch (Exception e) {
            logger.error("Failed to process order {}: {}", orderId, e.getMessage());
        }
    }
    
    private CompletableFuture<Customer> validateCustomerAsync(Long customerId) {
        return CompletableFuture.supplyAsync(() -> {
            // Async customer validation
            return customerServiceClient.getCustomer(customerId);
        });
    }
    
    private CompletableFuture<Boolean> validateInventoryAsync(Order order) {
        return CompletableFuture.supplyAsync(() -> {
            // Async inventory check
            return inventoryServiceClient.checkInventory(
                order.getProductId(), order.getQuantity());
        });
    }
    
    private CompletableFuture<PaymentResult> processPaymentAsync(Order order) {
        return CompletableFuture.supplyAsync(() -> {
            // Async payment processing
            return paymentServiceClient.processPayment(order.getPaymentDetails());
        });
    }
}
```

---

## 2. REST API Design for Microservices

### 2.1 RESTful Service Design

```java
// Customer Service REST API
@RestController
@RequestMapping("/api/v1/customers")
@Validated
public class CustomerController {
    
    @Autowired
    private CustomerService customerService;
    
    // GET /api/v1/customers/{id}
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(
            @PathVariable @Min(1) Long id,
            @RequestHeader(value = "X-Request-ID", required = false) String requestId) {
        
        Customer customer = customerService.getCustomer(id);
        CustomerResponse response = CustomerResponse.from(customer);
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Request-ID", requestId);
        headers.add("X-Service-Name", "customer-service");
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(response);
    }
    
    // POST /api/v1/customers
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request,
            @RequestHeader(value = "X-Request-ID", required = false) String requestId) {
        
        Customer customer = customerService.createCustomer(request);
        CustomerResponse response = CustomerResponse.from(customer);
        
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(customer.getId())
            .toUri();
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Request-ID", requestId);
        headers.add("X-Service-Name", "customer-service");
        
        return ResponseEntity.created(location)
            .headers(headers)
            .body(response);
    }
    
    // PUT /api/v1/customers/{id}
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody UpdateCustomerRequest request,
            @RequestHeader(value = "X-Request-ID", required = false) String requestId) {
        
        Customer customer = customerService.updateCustomer(id, request);
        CustomerResponse response = CustomerResponse.from(customer);
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Request-ID", requestId);
        headers.add("X-Service-Name", "customer-service");
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(response);
    }
    
    // GET /api/v1/customers
    @GetMapping
    public ResponseEntity<PagedResponse<CustomerResponse>> getCustomers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String status,
            @RequestHeader(value = "X-Request-ID", required = false) String requestId) {
        
        CustomerSearchCriteria criteria = CustomerSearchCriteria.builder()
            .email(email)
            .status(status)
            .build();
        
        PageRequest pageRequest = PageRequest.of(page, size, 
            Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
        
        Page<Customer> customers = customerService.findCustomers(criteria, pageRequest);
        PagedResponse<CustomerResponse> response = PagedResponse.from(
            customers, CustomerResponse::from);
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Request-ID", requestId);
        headers.add("X-Service-Name", "customer-service");
        headers.add("X-Total-Count", String.valueOf(customers.getTotalElements()));
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(response);
    }
    
    // PATCH /api/v1/customers/{id}/status
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateCustomerStatus(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody UpdateStatusRequest request,
            @RequestHeader(value = "X-Request-ID", required = false) String requestId) {
        
        customerService.updateCustomerStatus(id, request.getStatus());
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Request-ID", requestId);
        headers.add("X-Service-Name", "customer-service");
        
        return ResponseEntity.ok()
            .headers(headers)
            .build();
    }
    
    // DELETE /api/v1/customers/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(
            @PathVariable @Min(1) Long id,
            @RequestHeader(value = "X-Request-ID", required = false) String requestId) {
        
        customerService.deleteCustomer(id);
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Request-ID", requestId);
        headers.add("X-Service-Name", "customer-service");
        
        return ResponseEntity.noContent()
            .headers(headers)
            .build();
    }
}

// Error Handling
@RestControllerAdvice
public class CustomerControllerAdvice {
    
    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCustomerNotFound(
            CustomerNotFoundException ex, HttpServletRequest request) {
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Customer Not Found")
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(CustomerAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleCustomerAlreadyExists(
            CustomerAlreadyExistsException ex, HttpServletRequest request) {
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.CONFLICT.value())
            .error("Customer Already Exists")
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        List<String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.toList());
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("Invalid request parameters")
            .path(request.getRequestURI())
            .details(errors)
            .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
```

### 2.2 Service Client Implementation

```java
// Feign Client for Service-to-Service Communication
@FeignClient(name = "customer-service", url = "${customer-service.url}")
public interface CustomerServiceClient {
    
    @GetMapping("/api/v1/customers/{id}")
    CustomerResponse getCustomer(@PathVariable("id") Long id);
    
    @PostMapping("/api/v1/customers")
    CustomerResponse createCustomer(@RequestBody CreateCustomerRequest request);
    
    @PutMapping("/api/v1/customers/{id}")
    CustomerResponse updateCustomer(@PathVariable("id") Long id, 
                                   @RequestBody UpdateCustomerRequest request);
    
    @GetMapping("/api/v1/customers")
    PagedResponse<CustomerResponse> getCustomers(
        @RequestParam("page") int page,
        @RequestParam("size") int size,
        @RequestParam("email") String email,
        @RequestParam("status") String status
    );
    
    @PatchMapping("/api/v1/customers/{id}/status")
    void updateCustomerStatus(@PathVariable("id") Long id, 
                             @RequestBody UpdateStatusRequest request);
    
    @DeleteMapping("/api/v1/customers/{id}")
    void deleteCustomer(@PathVariable("id") Long id);
}

// RestTemplate based Client
@Component
public class CustomerRestClient {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${customer-service.url}")
    private String customerServiceUrl;
    
    public CustomerResponse getCustomer(Long id) {
        String url = customerServiceUrl + "/api/v1/customers/" + id;
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Request-ID", UUID.randomUUID().toString());
        headers.add("Content-Type", "application/json");
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<CustomerResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, CustomerResponse.class);
            
            return response.getBody();
            
        } catch (HttpClientErrorException.NotFound e) {
            throw new CustomerNotFoundException("Customer not found: " + id);
        } catch (HttpServerErrorException e) {
            throw new CustomerServiceException("Customer service error: " + e.getMessage());
        }
    }
    
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        String url = customerServiceUrl + "/api/v1/customers";
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Request-ID", UUID.randomUUID().toString());
        headers.add("Content-Type", "application/json");
        
        HttpEntity<CreateCustomerRequest> entity = new HttpEntity<>(request, headers);
        
        try {
            ResponseEntity<CustomerResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, CustomerResponse.class);
            
            return response.getBody();
            
        } catch (HttpClientErrorException.Conflict e) {
            throw new CustomerAlreadyExistsException("Customer already exists");
        } catch (HttpClientErrorException.BadRequest e) {
            throw new InvalidCustomerDataException("Invalid customer data");
        } catch (HttpServerErrorException e) {
            throw new CustomerServiceException("Customer service error: " + e.getMessage());
        }
    }
}

// WebClient based Client (Reactive)
@Component
public class CustomerWebClient {
    
    private final WebClient webClient;
    
    public CustomerWebClient(@Value("${customer-service.url}") String baseUrl) {
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Content-Type", "application/json")
            .build();
    }
    
    public Mono<CustomerResponse> getCustomer(Long id) {
        return webClient.get()
            .uri("/api/v1/customers/{id}", id)
            .header("X-Request-ID", UUID.randomUUID().toString())
            .retrieve()
            .onStatus(HttpStatus::is4xxClientError, response -> {
                if (response.statusCode() == HttpStatus.NOT_FOUND) {
                    return Mono.error(new CustomerNotFoundException("Customer not found: " + id));
                }
                return Mono.error(new CustomerServiceException("Client error: " + response.statusCode()));
            })
            .onStatus(HttpStatus::is5xxServerError, response -> 
                Mono.error(new CustomerServiceException("Server error: " + response.statusCode())))
            .bodyToMono(CustomerResponse.class);
    }
    
    public Mono<CustomerResponse> createCustomer(CreateCustomerRequest request) {
        return webClient.post()
            .uri("/api/v1/customers")
            .header("X-Request-ID", UUID.randomUUID().toString())
            .body(Mono.just(request), CreateCustomerRequest.class)
            .retrieve()
            .onStatus(HttpStatus::is4xxClientError, response -> {
                if (response.statusCode() == HttpStatus.CONFLICT) {
                    return Mono.error(new CustomerAlreadyExistsException("Customer already exists"));
                }
                return Mono.error(new CustomerServiceException("Client error: " + response.statusCode()));
            })
            .onStatus(HttpStatus::is5xxServerError, response -> 
                Mono.error(new CustomerServiceException("Server error: " + response.statusCode())))
            .bodyToMono(CustomerResponse.class);
    }
}
```

---

## 3. Circuit Breaker Pattern

### 3.1 Circuit Breaker Implementation

```java
// Circuit Breaker Configuration
@Configuration
public class CircuitBreakerConfig {
    
    @Bean
    public CircuitBreaker customerServiceCircuitBreaker() {
        return CircuitBreaker.ofDefaults("customer-service");
    }
    
    @Bean
    public CircuitBreaker paymentServiceCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .slowCallRateThreshold(50)
            .slowCallDurationThreshold(Duration.ofSeconds(2))
            .recordExceptions(PaymentServiceException.class, TimeoutException.class)
            .ignoreExceptions(PaymentValidationException.class)
            .build();
        
        return CircuitBreaker.of("payment-service", config);
    }
}

// Circuit Breaker Service
@Service
public class ResilientCustomerService {
    
    @Autowired
    private CustomerServiceClient customerServiceClient;
    
    @Autowired
    private CircuitBreaker customerServiceCircuitBreaker;
    
    @Autowired
    private CustomerCacheService customerCacheService;
    
    public CustomerResponse getCustomer(Long id) {
        return customerServiceCircuitBreaker.executeSupplier(() -> {
            return customerServiceClient.getCustomer(id);
        });
    }
    
    public CustomerResponse getCustomerWithFallback(Long id) {
        Supplier<CustomerResponse> decoratedSupplier = CircuitBreaker
            .decorateSupplier(customerServiceCircuitBreaker, () -> {
                return customerServiceClient.getCustomer(id);
            });
        
        return Try.ofSupplier(decoratedSupplier)
            .recover(throwable -> {
                logger.warn("Circuit breaker fallback for customer {}: {}", id, throwable.getMessage());
                return customerCacheService.getCachedCustomer(id)
                    .orElse(createDefaultCustomer(id));
            })
            .get();
    }
    
    private CustomerResponse createDefaultCustomer(Long id) {
        return CustomerResponse.builder()
            .id(id)
            .email("unknown@example.com")
            .firstName("Unknown")
            .lastName("Customer")
            .status("UNKNOWN")
            .build();
    }
}

// Circuit Breaker Annotations
@Service
public class AnnotatedResilientService {
    
    @Autowired
    private CustomerServiceClient customerServiceClient;
    
    @CircuitBreaker(name = "customer-service", fallbackMethod = "getCustomerFallback")
    @TimeLimiter(name = "customer-service")
    @Retry(name = "customer-service")
    public CompletableFuture<CustomerResponse> getCustomerAsync(Long id) {
        return CompletableFuture.supplyAsync(() -> {
            return customerServiceClient.getCustomer(id);
        });
    }
    
    public CompletableFuture<CustomerResponse> getCustomerFallback(Long id, Exception ex) {
        logger.warn("Fallback triggered for customer {}: {}", id, ex.getMessage());
        
        CustomerResponse fallbackResponse = CustomerResponse.builder()
            .id(id)
            .email("fallback@example.com")
            .firstName("Fallback")
            .lastName("Customer")
            .status("UNKNOWN")
            .build();
        
        return CompletableFuture.completedFuture(fallbackResponse);
    }
}

// Circuit Breaker Monitoring
@Component
public class CircuitBreakerMonitor {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @EventListener
    public void handleCircuitBreakerEvent(CircuitBreakerOnStateTransitionEvent event) {
        CircuitBreaker.StateTransition transition = event.getStateTransition();
        
        logger.info("Circuit breaker {} state changed from {} to {}", 
            event.getCircuitBreakerName(),
            transition.getFromState(),
            transition.getToState());
        
        // Record metrics
        meterRegistry.counter("circuit_breaker_state_transitions",
            "circuit_breaker", event.getCircuitBreakerName(),
            "from_state", transition.getFromState().name(),
            "to_state", transition.getToState().name())
            .increment();
    }
    
    @EventListener
    public void handleCircuitBreakerSuccess(CircuitBreakerOnSuccessEvent event) {
        meterRegistry.counter("circuit_breaker_calls",
            "circuit_breaker", event.getCircuitBreakerName(),
            "result", "success")
            .increment();
    }
    
    @EventListener
    public void handleCircuitBreakerError(CircuitBreakerOnErrorEvent event) {
        meterRegistry.counter("circuit_breaker_calls",
            "circuit_breaker", event.getCircuitBreakerName(),
            "result", "error",
            "exception", event.getThrowable().getClass().getSimpleName())
            .increment();
    }
}
```

### 3.2 Retry Mechanism

```java
// Retry Configuration
@Configuration
public class RetryConfig {
    
    @Bean
    public Retry customerServiceRetry() {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofSeconds(1))
            .retryExceptions(CustomerServiceException.class, TimeoutException.class)
            .ignoreExceptions(CustomerNotFoundException.class, CustomerValidationException.class)
            .build();
        
        return Retry.of("customer-service", config);
    }
    
    @Bean
    public Retry paymentServiceRetry() {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(5)
            .waitDuration(Duration.ofSeconds(2))
            .exponentialBackoffMultiplier(2)
            .retryExceptions(PaymentServiceException.class, ConnectException.class)
            .ignoreExceptions(PaymentValidationException.class)
            .build();
        
        return Retry.of("payment-service", config);
    }
}

// Retry Service
@Service
public class RetryableCustomerService {
    
    @Autowired
    private CustomerServiceClient customerServiceClient;
    
    @Autowired
    private Retry customerServiceRetry;
    
    public CustomerResponse getCustomerWithRetry(Long id) {
        Supplier<CustomerResponse> decoratedSupplier = Retry
            .decorateSupplier(customerServiceRetry, () -> {
                logger.info("Attempting to get customer {}", id);
                return customerServiceClient.getCustomer(id);
            });
        
        return Try.ofSupplier(decoratedSupplier)
            .recover(throwable -> {
                logger.error("All retry attempts failed for customer {}: {}", id, throwable.getMessage());
                throw new CustomerServiceUnavailableException("Customer service unavailable after retries");
            })
            .get();
    }
}

// Retry with Annotations
@Service
public class AnnotatedRetryService {
    
    @Autowired
    private CustomerServiceClient customerServiceClient;
    
    @Retryable(
        value = {CustomerServiceException.class, TimeoutException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public CustomerResponse getCustomerWithAnnotation(Long id) {
        logger.info("Attempting to get customer {} (with annotation)", id);
        return customerServiceClient.getCustomer(id);
    }
    
    @Recover
    public CustomerResponse recover(CustomerServiceException ex, Long id) {
        logger.error("Recovery method called for customer {}: {}", id, ex.getMessage());
        throw new CustomerServiceUnavailableException("Customer service unavailable");
    }
}
```

---

## 4. Message-Based Communication

### 4.1 Event-Driven Communication

```java
// Event Publisher
@Component
public class OrderEventPublisher {
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public void publishOrderCreated(Order order) {
        // Local event
        eventPublisher.publishEvent(new OrderCreatedEvent(order));
        
        // Distributed event
        OrderCreatedMessage message = OrderCreatedMessage.builder()
            .orderId(order.getId())
            .customerId(order.getCustomerId())
            .totalAmount(order.getTotalAmount())
            .timestamp(LocalDateTime.now())
            .build();
        
        rabbitTemplate.convertAndSend("order.exchange", "order.created", message);
    }
    
    public void publishOrderConfirmed(Order order) {
        OrderConfirmedMessage message = OrderConfirmedMessage.builder()
            .orderId(order.getId())
            .customerId(order.getCustomerId())
            .confirmedAt(LocalDateTime.now())
            .build();
        
        rabbitTemplate.convertAndSend("order.exchange", "order.confirmed", message);
    }
    
    public void publishOrderCancelled(Order order, String reason) {
        OrderCancelledMessage message = OrderCancelledMessage.builder()
            .orderId(order.getId())
            .customerId(order.getCustomerId())
            .reason(reason)
            .cancelledAt(LocalDateTime.now())
            .build();
        
        rabbitTemplate.convertAndSend("order.exchange", "order.cancelled", message);
    }
}

// Event Listener
@Component
public class OrderEventListener {
    
    @Autowired
    private InventoryService inventoryService;
    
    @Autowired
    private NotificationService notificationService;
    
    @RabbitListener(queues = "inventory.order.created")
    public void handleOrderCreated(OrderCreatedMessage message) {
        try {
            logger.info("Processing order created event for order {}", message.getOrderId());
            
            // Reserve inventory
            inventoryService.reserveInventory(message.getOrderId(), message.getItems());
            
            // Publish inventory reserved event
            InventoryReservedMessage inventoryMessage = InventoryReservedMessage.builder()
                .orderId(message.getOrderId())
                .reservedAt(LocalDateTime.now())
                .build();
            
            rabbitTemplate.convertAndSend("inventory.exchange", "inventory.reserved", inventoryMessage);
            
        } catch (Exception e) {
            logger.error("Failed to process order created event for order {}: {}", 
                message.getOrderId(), e.getMessage());
            
            // Publish failure event
            InventoryReservationFailedMessage failureMessage = InventoryReservationFailedMessage.builder()
                .orderId(message.getOrderId())
                .reason(e.getMessage())
                .failedAt(LocalDateTime.now())
                .build();
            
            rabbitTemplate.convertAndSend("inventory.exchange", "inventory.reservation.failed", failureMessage);
        }
    }
    
    @RabbitListener(queues = "notification.order.confirmed")
    public void handleOrderConfirmed(OrderConfirmedMessage message) {
        try {
            logger.info("Processing order confirmed event for order {}", message.getOrderId());
            
            // Send confirmation notification
            notificationService.sendOrderConfirmationNotification(
                message.getCustomerId(), message.getOrderId());
            
        } catch (Exception e) {
            logger.error("Failed to send order confirmation notification for order {}: {}", 
                message.getOrderId(), e.getMessage());
        }
    }
}

// Message Configuration
@Configuration
@EnableRabbit
public class MessageConfig {
    
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange("order.exchange");
    }
    
    @Bean
    public TopicExchange inventoryExchange() {
        return new TopicExchange("inventory.exchange");
    }
    
    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange("notification.exchange");
    }
    
    @Bean
    public Queue inventoryOrderCreatedQueue() {
        return QueueBuilder.durable("inventory.order.created")
            .deadLetterExchange("dlx.exchange")
            .deadLetterRoutingKey("inventory.order.created.dlq")
            .build();
    }
    
    @Bean
    public Queue notificationOrderConfirmedQueue() {
        return QueueBuilder.durable("notification.order.confirmed")
            .deadLetterExchange("dlx.exchange")
            .deadLetterRoutingKey("notification.order.confirmed.dlq")
            .build();
    }
    
    @Bean
    public Binding inventoryOrderCreatedBinding() {
        return BindingBuilder.bind(inventoryOrderCreatedQueue())
            .to(orderExchange())
            .with("order.created");
    }
    
    @Bean
    public Binding notificationOrderConfirmedBinding() {
        return BindingBuilder.bind(notificationOrderConfirmedQueue())
            .to(orderExchange())
            .with("order.confirmed");
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setReplyTimeout(5000);
        template.setReceiveTimeout(5000);
        return template;
    }
}
```

### 4.2 Request-Reply Pattern

```java
// Request-Reply Service
@Service
public class RequestReplyService {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public CustomerValidationResponse validateCustomer(Long customerId) {
        CustomerValidationRequest request = CustomerValidationRequest.builder()
            .customerId(customerId)
            .requestId(UUID.randomUUID().toString())
            .timestamp(LocalDateTime.now())
            .build();
        
        try {
            CustomerValidationResponse response = (CustomerValidationResponse) rabbitTemplate
                .convertSendAndReceive("customer.exchange", "customer.validate", request);
            
            if (response == null) {
                throw new CustomerValidationTimeoutException("Customer validation timed out");
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to validate customer {}: {}", customerId, e.getMessage());
            throw new CustomerValidationException("Customer validation failed", e);
        }
    }
    
    public InventoryCheckResponse checkInventory(List<Long> productIds, List<Integer> quantities) {
        InventoryCheckRequest request = InventoryCheckRequest.builder()
            .productIds(productIds)
            .quantities(quantities)
            .requestId(UUID.randomUUID().toString())
            .timestamp(LocalDateTime.now())
            .build();
        
        try {
            InventoryCheckResponse response = (InventoryCheckResponse) rabbitTemplate
                .convertSendAndReceive("inventory.exchange", "inventory.check", request);
            
            if (response == null) {
                throw new InventoryCheckTimeoutException("Inventory check timed out");
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to check inventory: {}", e.getMessage());
            throw new InventoryCheckException("Inventory check failed", e);
        }
    }
}

// Request Handler
@Component
public class CustomerValidationHandler {
    
    @Autowired
    private CustomerService customerService;
    
    @RabbitListener(queues = "customer.validation.requests")
    public CustomerValidationResponse handleCustomerValidation(CustomerValidationRequest request) {
        try {
            logger.info("Processing customer validation request for customer {}", request.getCustomerId());
            
            Customer customer = customerService.getCustomer(request.getCustomerId());
            
            return CustomerValidationResponse.builder()
                .requestId(request.getRequestId())
                .customerId(request.getCustomerId())
                .valid(customer.isActive())
                .customerStatus(customer.getStatus())
                .validatedAt(LocalDateTime.now())
                .build();
            
        } catch (CustomerNotFoundException e) {
            return CustomerValidationResponse.builder()
                .requestId(request.getRequestId())
                .customerId(request.getCustomerId())
                .valid(false)
                .error("Customer not found")
                .validatedAt(LocalDateTime.now())
                .build();
        } catch (Exception e) {
            logger.error("Failed to validate customer {}: {}", request.getCustomerId(), e.getMessage());
            
            return CustomerValidationResponse.builder()
                .requestId(request.getRequestId())
                .customerId(request.getCustomerId())
                .valid(false)
                .error("Validation failed: " + e.getMessage())
                .validatedAt(LocalDateTime.now())
                .build();
        }
    }
}
```

---

## 5. Timeout and Bulkhead Patterns

### 5.1 Timeout Configuration

```java
// Timeout Configuration
@Configuration
public class TimeoutConfig {
    
    @Bean
    public TimeLimiter customerServiceTimeLimiter() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(5))
            .cancelRunningFuture(true)
            .build();
        
        return TimeLimiter.of("customer-service", config);
    }
    
    @Bean
    public TimeLimiter paymentServiceTimeLimiter() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(10))
            .cancelRunningFuture(true)
            .build();
        
        return TimeLimiter.of("payment-service", config);
    }
}

// Timeout Service
@Service
public class TimeoutService {
    
    @Autowired
    private CustomerServiceClient customerServiceClient;
    
    @Autowired
    private TimeLimiter customerServiceTimeLimiter;
    
    @Autowired
    private ScheduledExecutorService executorService;
    
    public CustomerResponse getCustomerWithTimeout(Long id) {
        CompletableFuture<CustomerResponse> future = CompletableFuture.supplyAsync(() -> {
            return customerServiceClient.getCustomer(id);
        }, executorService);
        
        Supplier<CompletableFuture<CustomerResponse>> decoratedSupplier = 
            TimeLimiter.decorateCompletionStage(customerServiceTimeLimiter, () -> future);
        
        return Try.of(decoratedSupplier)
            .get()
            .exceptionally(throwable -> {
                if (throwable instanceof TimeoutException) {
                    logger.warn("Customer service call timed out for customer {}", id);
                    throw new CustomerServiceTimeoutException("Customer service timeout");
                }
                throw new CustomerServiceException("Customer service error", throwable);
            })
            .join();
    }
}
```

### 5.2 Bulkhead Pattern

```java
// Bulkhead Configuration
@Configuration
public class BulkheadConfig {
    
    @Bean
    public Bulkhead customerServiceBulkhead() {
        BulkheadConfig config = BulkheadConfig.custom()
            .maxConcurrentCalls(10)
            .maxWaitDuration(Duration.ofSeconds(5))
            .build();
        
        return Bulkhead.of("customer-service", config);
    }
    
    @Bean
    public ThreadPoolBulkhead paymentServiceBulkhead() {
        ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(20)
            .coreThreadPoolSize(10)
            .queueCapacity(50)
            .keepAliveDuration(Duration.ofSeconds(20))
            .build();
        
        return ThreadPoolBulkhead.of("payment-service", config);
    }
    
    @Bean
    public ScheduledExecutorService executorService() {
        return Executors.newScheduledThreadPool(10);
    }
}

// Bulkhead Service
@Service
public class BulkheadService {
    
    @Autowired
    private CustomerServiceClient customerServiceClient;
    
    @Autowired
    private PaymentServiceClient paymentServiceClient;
    
    @Autowired
    private Bulkhead customerServiceBulkhead;
    
    @Autowired
    private ThreadPoolBulkhead paymentServiceBulkhead;
    
    public CustomerResponse getCustomerWithBulkhead(Long id) {
        Supplier<CustomerResponse> decoratedSupplier = Bulkhead
            .decorateSupplier(customerServiceBulkhead, () -> {
                return customerServiceClient.getCustomer(id);
            });
        
        return Try.ofSupplier(decoratedSupplier)
            .recover(BulkheadFullException.class, throwable -> {
                logger.warn("Customer service bulkhead is full for customer {}", id);
                throw new CustomerServiceOverloadedException("Customer service overloaded");
            })
            .get();
    }
    
    public CompletableFuture<PaymentResponse> processPaymentWithBulkhead(PaymentRequest request) {
        Supplier<CompletableFuture<PaymentResponse>> decoratedSupplier = 
            ThreadPoolBulkhead.decorateSupplier(paymentServiceBulkhead, () -> {
                return CompletableFuture.supplyAsync(() -> {
                    return paymentServiceClient.processPayment(request);
                });
            });
        
        return decoratedSupplier.get()
            .exceptionally(throwable -> {
                if (throwable instanceof BulkheadFullException) {
                    logger.warn("Payment service bulkhead is full for payment {}", request.getPaymentId());
                    throw new PaymentServiceOverloadedException("Payment service overloaded");
                }
                throw new PaymentServiceException("Payment service error", throwable);
            });
    }
}
```

---

## 6. Key Takeaways

### Communication Patterns:
- **Synchronous**: Direct request-response, simpler but creates coupling
- **Asynchronous**: Event-driven, better resilience but more complex
- **Hybrid**: Combine both patterns based on use case requirements

### Resilience Patterns:
- **Circuit Breaker**: Prevent cascading failures
- **Retry**: Handle transient failures
- **Timeout**: Prevent hanging requests
- **Bulkhead**: Isolate critical resources

### Best Practices:
1. **Design for Failure**: Assume services will fail
2. **Implement Fallbacks**: Provide graceful degradation
3. **Monitor Everything**: Track metrics and health
4. **Use Correlation IDs**: Enable request tracing
5. **Version APIs**: Enable backward compatibility

---

## 7. Next Steps

Tomorrow we'll explore **API Gateway Patterns**, learning:
- Centralized API management
- Request routing and load balancing
- Authentication and authorization
- Rate limiting and throttling

**Practice Assignment**: Implement a resilient service client with circuit breaker, retry, and timeout patterns.

---

*"In distributed systems, network failures are not exceptions - they are the norm. Design your communication patterns accordingly."*

*"The best microservices architecture is one where services can fail gracefully without bringing down the entire system."*