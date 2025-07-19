# Day 68: Spring Cloud Sleuth & Zipkin - Distributed Tracing

## Overview
Today we'll explore distributed tracing with Spring Cloud Sleuth and Zipkin. We'll learn how to track requests across multiple services, analyze performance bottlenecks, and troubleshoot issues in microservices architectures.

## Learning Objectives
- Understand distributed tracing concepts and terminology
- Configure Spring Cloud Sleuth for automatic trace instrumentation
- Set up Zipkin server for trace collection and visualization
- Implement custom spans and trace correlation
- Analyze performance and troubleshoot distributed systems
- Integrate tracing with logging and monitoring

## 1. Distributed Tracing Fundamentals

### Core Concepts

**Trace**: A complete request journey through multiple services
**Span**: A single operation within a trace (e.g., HTTP request, database query)
**Trace ID**: Unique identifier for the entire request flow
**Span ID**: Unique identifier for a specific operation
**Parent Span**: The span that initiated the current span

### Spring Cloud Sleuth Setup

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-sleuth-zipkin</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

## 2. Zipkin Server Setup

### Docker Compose for Zipkin

```yaml
# docker-compose.yml
version: '3.8'
services:
  zipkin:
    image: openzipkin/zipkin
    container_name: zipkin
    ports:
      - "9411:9411"
    environment:
      - STORAGE_TYPE=mem
      - JAVA_OPTS=-Xms512m -Xmx512m
    restart: unless-stopped

  zipkin-mysql:
    image: openzipkin/zipkin-mysql
    container_name: zipkin-mysql
    ports:
      - "9412:9411"
    environment:
      - STORAGE_TYPE=mysql
      - MYSQL_HOST=mysql
      - MYSQL_USER=zipkin
      - MYSQL_PASS=zipkin
      - MYSQL_DB=zipkin
    depends_on:
      - mysql
    restart: unless-stopped

  mysql:
    image: mysql:8.0
    container_name: zipkin-mysql-db
    ports:
      - "3306:3306"
    environment:
      - MYSQL_ROOT_PASSWORD=root
      - MYSQL_DATABASE=zipkin
      - MYSQL_USER=zipkin
      - MYSQL_PASSWORD=zipkin
    volumes:
      - mysql_data:/var/lib/mysql
    restart: unless-stopped

volumes:
  mysql_data:
```

### Zipkin Configuration

```yaml
# application.yml
spring:
  application:
    name: order-service
  sleuth:
    zipkin:
      base-url: http://localhost:9411
    sampler:
      probability: 1.0  # Sample 100% of requests (reduce in production)
    web:
      client:
        enabled: true
    http:
      legacy:
        enabled: true
  
logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

## 3. Order Service with Tracing

### Order Controller with Tracing

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private final OrderService orderService;
    private final Tracer tracer;
    
    public OrderController(OrderService orderService, Tracer tracer) {
        this.orderService = orderService;
        this.tracer = tracer;
    }
    
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {
        Span span = tracer.nextSpan().name("create-order").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            span.tag("order.customer-id", request.getCustomerId().toString());
            span.tag("order.product-id", request.getProductId().toString());
            span.tag("order.quantity", request.getQuantity().toString());
            
            Order order = orderService.createOrder(request);
            
            span.tag("order.id", order.getId().toString());
            span.tag("order.status", order.getStatus());
            
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            span.tag("error", e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable Long orderId) {
        return orderService.getOrder(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
```

### Order Service with Custom Spans

```java
@Service
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final NotificationService notificationService;
    private final Tracer tracer;
    
    public OrderService(OrderRepository orderRepository,
                       InventoryClient inventoryClient,
                       PaymentClient paymentClient,
                       NotificationService notificationService,
                       Tracer tracer) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
        this.notificationService = notificationService;
        this.tracer = tracer;
    }
    
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        Span span = tracer.nextSpan().name("order-creation-process").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            
            // Check inventory
            InventoryResponse inventory = checkInventory(request.getProductId(), request.getQuantity());
            
            // Reserve inventory
            ReservationResponse reservation = reserveInventory(request.getProductId(), 
                                                             request.getQuantity());
            
            // Process payment
            PaymentResponse payment = processPayment(request.getCustomerId(), 
                                                   inventory.getPrice(), 
                                                   request.getQuantity());
            
            // Create order
            Order order = createOrderEntity(request, inventory, reservation, payment);
            
            // Send notification
            sendOrderNotification(order);
            
            span.tag("order.total-amount", order.getTotalAmount().toString());
            span.tag("order.created", "true");
            
            return order;
        } catch (Exception e) {
            span.tag("error", e.getMessage());
            span.tag("order.created", "false");
            throw e;
        } finally {
            span.end();
        }
    }
    
    private InventoryResponse checkInventory(Long productId, Integer quantity) {
        Span span = tracer.nextSpan().name("check-inventory").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            span.tag("inventory.product-id", productId.toString());
            span.tag("inventory.requested-quantity", quantity.toString());
            
            InventoryResponse response = inventoryClient.getInventory(productId);
            
            span.tag("inventory.available-quantity", response.getAvailableQuantity().toString());
            span.tag("inventory.status", response.getStatus());
            
            if (response.getAvailableQuantity() < quantity) {
                span.tag("inventory.sufficient", "false");
                throw new InsufficientInventoryException("Not enough inventory");
            }
            
            span.tag("inventory.sufficient", "true");
            return response;
        } finally {
            span.end();
        }
    }
    
    private ReservationResponse reserveInventory(Long productId, Integer quantity) {
        Span span = tracer.nextSpan().name("reserve-inventory").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            span.tag("reservation.product-id", productId.toString());
            span.tag("reservation.quantity", quantity.toString());
            
            ReservationRequest request = new ReservationRequest(productId, quantity, 
                                                              UUID.randomUUID().toString());
            ReservationResponse response = inventoryClient.reserveInventory(request);
            
            span.tag("reservation.id", response.getReservationId());
            span.tag("reservation.success", String.valueOf(response.isSuccess()));
            
            if (!response.isSuccess()) {
                throw new ReservationFailedException("Failed to reserve inventory");
            }
            
            return response;
        } finally {
            span.end();
        }
    }
    
    private PaymentResponse processPayment(Long customerId, BigDecimal unitPrice, Integer quantity) {
        Span span = tracer.nextSpan().name("process-payment").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
            
            span.tag("payment.customer-id", customerId.toString());
            span.tag("payment.amount", totalAmount.toString());
            
            PaymentRequest request = new PaymentRequest(customerId, totalAmount, "CREDIT_CARD");
            PaymentResponse response = paymentClient.processPayment(request);
            
            span.tag("payment.id", response.getPaymentId());
            span.tag("payment.success", String.valueOf(response.isSuccess()));
            
            if (!response.isSuccess()) {
                throw new PaymentFailedException("Payment failed");
            }
            
            return response;
        } finally {
            span.end();
        }
    }
    
    private Order createOrderEntity(CreateOrderRequest request, InventoryResponse inventory,
                                   ReservationResponse reservation, PaymentResponse payment) {
        Span span = tracer.nextSpan().name("create-order-entity").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            Order order = new Order();
            order.setCustomerId(request.getCustomerId());
            order.setProductId(request.getProductId());
            order.setQuantity(request.getQuantity());
            order.setTotalAmount(inventory.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
            order.setStatus("CONFIRMED");
            order.setReservationId(reservation.getReservationId());
            order.setPaymentId(payment.getPaymentId());
            order.setCreatedAt(LocalDateTime.now());
            
            Order savedOrder = orderRepository.save(order);
            
            span.tag("order.id", savedOrder.getId().toString());
            span.tag("order.saved", "true");
            
            return savedOrder;
        } finally {
            span.end();
        }
    }
    
    private void sendOrderNotification(Order order) {
        Span span = tracer.nextSpan().name("send-order-notification").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            span.tag("notification.order-id", order.getId().toString());
            span.tag("notification.customer-id", order.getCustomerId().toString());
            
            notificationService.sendOrderConfirmation(order);
            
            span.tag("notification.sent", "true");
        } catch (Exception e) {
            span.tag("notification.sent", "false");
            span.tag("notification.error", e.getMessage());
            // Don't fail the order for notification failures
        } finally {
            span.end();
        }
    }
}
```

## 4. Inventory Service with Tracing

### Inventory Controller

```java
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    
    private final InventoryService inventoryService;
    private final Tracer tracer;
    
    public InventoryController(InventoryService inventoryService, Tracer tracer) {
        this.inventoryService = inventoryService;
        this.tracer = tracer;
    }
    
    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getInventory(@PathVariable Long productId) {
        Span span = tracer.nextSpan().name("get-inventory").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            span.tag("inventory.product-id", productId.toString());
            
            InventoryResponse response = inventoryService.getInventory(productId);
            
            span.tag("inventory.available", response.getAvailableQuantity().toString());
            span.tag("inventory.status", response.getStatus());
            
            return ResponseEntity.ok(response);
        } finally {
            span.end();
        }
    }
    
    @PostMapping("/reserve")
    public ResponseEntity<ReservationResponse> reserveInventory(@RequestBody ReservationRequest request) {
        Span span = tracer.nextSpan().name("reserve-inventory").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            span.tag("reservation.product-id", request.getProductId().toString());
            span.tag("reservation.quantity", request.getQuantity().toString());
            
            ReservationResponse response = inventoryService.reserveInventory(request);
            
            span.tag("reservation.success", String.valueOf(response.isSuccess()));
            span.tag("reservation.id", response.getReservationId());
            
            return ResponseEntity.ok(response);
        } finally {
            span.end();
        }
    }
}
```

### Inventory Service with Database Tracing

```java
@Service
public class InventoryService {
    
    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final Tracer tracer;
    
    public InventoryService(InventoryRepository inventoryRepository,
                          ReservationRepository reservationRepository,
                          Tracer tracer) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.tracer = tracer;
    }
    
    public InventoryResponse getInventory(Long productId) {
        Span span = tracer.nextSpan().name("database-get-inventory").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            span.tag("db.operation", "SELECT");
            span.tag("db.table", "inventory");
            span.tag("db.product-id", productId.toString());
            
            Optional<Inventory> inventory = inventoryRepository.findByProductId(productId);
            
            if (inventory.isPresent()) {
                Inventory inv = inventory.get();
                span.tag("db.found", "true");
                span.tag("db.available-quantity", inv.getAvailableQuantity().toString());
                
                return new InventoryResponse(
                    inv.getProductId(),
                    inv.getAvailableQuantity(),
                    inv.getStatus(),
                    inv.getPrice()
                );
            } else {
                span.tag("db.found", "false");
                throw new ProductNotFoundException("Product not found: " + productId);
            }
        } finally {
            span.end();
        }
    }
    
    @Transactional
    public ReservationResponse reserveInventory(ReservationRequest request) {
        Span span = tracer.nextSpan().name("database-reserve-inventory").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            span.tag("db.operation", "UPDATE");
            span.tag("db.table", "inventory");
            span.tag("db.product-id", request.getProductId().toString());
            span.tag("db.quantity", request.getQuantity().toString());
            
            // Check and update inventory
            Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(request.getProductId());
            
            if (inventoryOpt.isEmpty()) {
                span.tag("db.error", "Product not found");
                return new ReservationResponse(null, false, "Product not found");
            }
            
            Inventory inventory = inventoryOpt.get();
            
            if (inventory.getAvailableQuantity() < request.getQuantity()) {
                span.tag("db.error", "Insufficient inventory");
                return new ReservationResponse(null, false, "Insufficient inventory");
            }
            
            // Update inventory
            inventory.setAvailableQuantity(inventory.getAvailableQuantity() - request.getQuantity());
            inventoryRepository.save(inventory);
            
            // Create reservation record
            Reservation reservation = new Reservation();
            reservation.setReservationId(UUID.randomUUID().toString());
            reservation.setProductId(request.getProductId());
            reservation.setQuantity(request.getQuantity());
            reservation.setOrderId(request.getOrderId());
            reservation.setStatus("RESERVED");
            reservation.setCreatedAt(LocalDateTime.now());
            
            reservationRepository.save(reservation);
            
            span.tag("db.reservation-id", reservation.getReservationId());
            span.tag("db.success", "true");
            
            return new ReservationResponse(
                reservation.getReservationId(),
                true,
                "Inventory reserved successfully"
            );
        } finally {
            span.end();
        }
    }
}
```

## 5. Custom Trace Annotations

### Trace Annotation

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Traced {
    String value() default "";
    String[] tags() default {};
}
```

### Trace Aspect

```java
@Aspect
@Component
public class TracingAspect {
    
    private final Tracer tracer;
    
    public TracingAspect(Tracer tracer) {
        this.tracer = tracer;
    }
    
    @Around("@annotation(traced)")
    public Object trace(ProceedingJoinPoint joinPoint, Traced traced) throws Throwable {
        String spanName = traced.value().isEmpty() ? 
                         joinPoint.getSignature().getName() : 
                         traced.value();
        
        Span span = tracer.nextSpan().name(spanName).start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            
            // Add method parameters as tags
            Object[] args = joinPoint.getArgs();
            String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
            
            for (int i = 0; i < args.length && i < paramNames.length; i++) {
                if (args[i] != null) {
                    span.tag("method.param." + paramNames[i], args[i].toString());
                }
            }
            
            // Add custom tags
            for (String tag : traced.tags()) {
                String[] keyValue = tag.split("=");
                if (keyValue.length == 2) {
                    span.tag(keyValue[0], keyValue[1]);
                }
            }
            
            Object result = joinPoint.proceed();
            
            if (result != null) {
                span.tag("method.result.type", result.getClass().getSimpleName());
            }
            
            return result;
        } catch (Exception e) {
            span.tag("error", e.getMessage());
            span.tag("error.type", e.getClass().getSimpleName());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

### Using Custom Annotations

```java
@Service
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final ExternalPaymentGateway paymentGateway;
    
    public PaymentService(PaymentRepository paymentRepository,
                         ExternalPaymentGateway paymentGateway) {
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
    }
    
    @Traced(value = "process-payment", tags = {"service=payment", "operation=process"})
    public PaymentResponse processPayment(PaymentRequest request) {
        // Validate payment request
        validatePaymentRequest(request);
        
        // Process with external gateway
        PaymentResponse response = processWithGateway(request);
        
        // Save payment record
        savePaymentRecord(request, response);
        
        return response;
    }
    
    @Traced("validate-payment-request")
    private void validatePaymentRequest(PaymentRequest request) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentException("Invalid payment amount");
        }
        
        if (request.getCustomerId() == null) {
            throw new InvalidPaymentException("Customer ID is required");
        }
    }
    
    @Traced("external-payment-gateway")
    private PaymentResponse processWithGateway(PaymentRequest request) {
        // Simulate external payment processing
        try {
            Thread.sleep(100); // Simulate network delay
            
            // Simulate success/failure
            boolean success = Math.random() > 0.1; // 90% success rate
            
            if (success) {
                return new PaymentResponse(
                    UUID.randomUUID().toString(),
                    true,
                    "Payment processed successfully"
                );
            } else {
                return new PaymentResponse(
                    null,
                    false,
                    "Payment failed"
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaymentProcessingException("Payment processing interrupted");
        }
    }
    
    @Traced("save-payment-record")
    private void savePaymentRecord(PaymentRequest request, PaymentResponse response) {
        Payment payment = new Payment();
        payment.setPaymentId(response.getPaymentId());
        payment.setCustomerId(request.getCustomerId());
        payment.setAmount(request.getAmount());
        payment.setStatus(response.isSuccess() ? "SUCCESS" : "FAILED");
        payment.setCreatedAt(LocalDateTime.now());
        
        paymentRepository.save(payment);
    }
}
```

## 6. Asynchronous Processing with Tracing

### Async Configuration

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
```

### Async Service with Tracing

```java
@Service
public class AsyncNotificationService {
    
    private final Tracer tracer;
    private final EmailService emailService;
    private final SmsService smsService;
    
    public AsyncNotificationService(Tracer tracer, 
                                  EmailService emailService,
                                  SmsService smsService) {
        this.tracer = tracer;
        this.emailService = emailService;
        this.smsService = smsService;
    }
    
    @Async
    @Traced("async-send-notifications")
    public CompletableFuture<Void> sendOrderNotifications(Order order) {
        // Create a new span for async operation
        Span span = tracer.nextSpan().name("async-order-notifications").start();
        
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            span.tag("order.id", order.getId().toString());
            span.tag("order.customer-id", order.getCustomerId().toString());
            
            // Send email notification
            CompletableFuture<Void> emailFuture = sendEmailNotification(order);
            
            // Send SMS notification
            CompletableFuture<Void> smsFuture = sendSmsNotification(order);
            
            // Wait for both to complete
            return CompletableFuture.allOf(emailFuture, smsFuture);
            
        } catch (Exception e) {
            span.tag("error", e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
    
    @Async
    @Traced("async-send-email")
    public CompletableFuture<Void> sendEmailNotification(Order order) {
        Span span = tracer.nextSpan().name("email-notification").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            span.tag("notification.type", "email");
            span.tag("notification.order-id", order.getId().toString());
            
            emailService.sendOrderConfirmation(order);
            
            span.tag("notification.sent", "true");
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            span.tag("notification.sent", "false");
            span.tag("error", e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
    
    @Async
    @Traced("async-send-sms")
    public CompletableFuture<Void> sendSmsNotification(Order order) {
        Span span = tracer.nextSpan().name("sms-notification").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            span.tag("notification.type", "sms");
            span.tag("notification.order-id", order.getId().toString());
            
            smsService.sendOrderConfirmation(order);
            
            span.tag("notification.sent", "true");
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            span.tag("notification.sent", "false");
            span.tag("error", e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

## 7. Database Tracing Enhancement

### JPA Repository with Tracing

```java
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    @Query("SELECT o FROM Order o WHERE o.customerId = :customerId AND o.status = :status")
    List<Order> findByCustomerIdAndStatus(@Param("customerId") Long customerId, 
                                         @Param("status") String status);
    
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findByDateRange(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);
}
```

### Custom Repository Implementation

```java
@Repository
public class CustomOrderRepositoryImpl {
    
    private final EntityManager entityManager;
    private final Tracer tracer;
    
    public CustomOrderRepositoryImpl(EntityManager entityManager, Tracer tracer) {
        this.entityManager = entityManager;
        this.tracer = tracer;
    }
    
    @Traced("complex-order-search")
    public List<Order> findOrdersWithComplexCriteria(OrderSearchCriteria criteria) {
        Span span = tracer.nextSpan().name("database-complex-query").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            span.tag("db.operation", "SELECT");
            span.tag("db.table", "orders");
            span.tag("db.criteria.customer-id", 
                    criteria.getCustomerId() != null ? criteria.getCustomerId().toString() : "null");
            span.tag("db.criteria.status", criteria.getStatus());
            
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Order> query = cb.createQuery(Order.class);
            Root<Order> root = query.from(Order.class);
            
            List<Predicate> predicates = new ArrayList<>();
            
            if (criteria.getCustomerId() != null) {
                predicates.add(cb.equal(root.get("customerId"), criteria.getCustomerId()));
            }
            
            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }
            
            if (criteria.getStartDate() != null && criteria.getEndDate() != null) {
                predicates.add(cb.between(root.get("createdAt"), 
                                        criteria.getStartDate(), 
                                        criteria.getEndDate()));
            }
            
            query.where(predicates.toArray(new Predicate[0]));
            query.orderBy(cb.desc(root.get("createdAt")));
            
            List<Order> results = entityManager.createQuery(query).getResultList();
            
            span.tag("db.results.count", String.valueOf(results.size()));
            
            return results;
        } finally {
            span.end();
        }
    }
}
```

## 8. Monitoring and Alerting

### Custom Metrics with Tracing

```java
@Component
public class TracingMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Tracer tracer;
    
    public TracingMetrics(MeterRegistry meterRegistry, Tracer tracer) {
        this.meterRegistry = meterRegistry;
        this.tracer = tracer;
    }
    
    @EventListener
    public void onSpanFinished(FinishedSpan span) {
        String serviceName = span.getRemoteServiceName();
        String operationName = span.getName();
        
        // Record span duration
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("trace.span.duration")
                .tag("service", serviceName != null ? serviceName : "unknown")
                .tag("operation", operationName)
                .register(meterRegistry));
        
        // Record error count
        if (span.getTags().containsKey("error")) {
            meterRegistry.counter("trace.span.errors",
                    "service", serviceName != null ? serviceName : "unknown",
                    "operation", operationName).increment();
        }
    }
    
    @Scheduled(fixedRate = 60000) // Every minute
    public void recordActiveSpans() {
        // Record active spans count
        meterRegistry.gauge("trace.spans.active", getCurrentActiveSpans());
    }
    
    private double getCurrentActiveSpans() {
        // Implementation to count active spans
        return 0.0;
    }
}
```

### Health Check with Tracing

```java
@Component
public class TracingHealthIndicator implements HealthIndicator {
    
    private final Tracer tracer;
    
    public TracingHealthIndicator(Tracer tracer) {
        this.tracer = tracer;
    }
    
    @Override
    public Health health() {
        Span span = tracer.nextSpan().name("health-check-tracing").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            
            // Check if tracing is working
            span.tag("health.check", "tracing");
            
            // Test creating a span
            Span testSpan = tracer.nextSpan().name("test-span").start();
            testSpan.tag("test", "true");
            testSpan.end();
            
            return Health.up()
                    .withDetail("tracing", "enabled")
                    .withDetail("tracer", tracer.getClass().getSimpleName())
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        } finally {
            span.end();
        }
    }
}
```

## 9. Testing with Tracing

### Integration Test with Tracing

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderServiceTracingIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private Tracer tracer;
    
    @MockBean
    private InventoryClient inventoryClient;
    
    @MockBean
    private PaymentClient paymentClient;
    
    @Test
    void testCreateOrderWithTracing() {
        // Given
        given(inventoryClient.getInventory(1L))
                .willReturn(new InventoryResponse(1L, 100, "AVAILABLE", new BigDecimal("29.99")));
        
        given(inventoryClient.reserveInventory(any()))
                .willReturn(new ReservationResponse("res-123", true, "Success"));
        
        given(paymentClient.processPayment(any()))
                .willReturn(new PaymentResponse("pay-123", true, "Success"));
        
        CreateOrderRequest request = new CreateOrderRequest(1L, 1L, 2, "CREDIT_CARD");
        
        // When
        Span testSpan = tracer.nextSpan().name("test-create-order").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(testSpan)) {
            testSpan.tag("test.name", "create-order");
            testSpan.tag("test.customer-id", "1");
            
            ResponseEntity<Order> response = restTemplate.postForEntity(
                    "/api/orders", request, Order.class);
            
            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCustomerId()).isEqualTo(1L);
            
            testSpan.tag("test.result", "success");
        } finally {
            testSpan.end();
        }
    }
}
```

## 10. Performance Analysis with Zipkin

### Analyzing Traces

The Zipkin UI (http://localhost:9411) provides several views for analyzing traces:

1. **Service Dependencies**: Visual representation of service interactions
2. **Trace Timeline**: Detailed view of request flow and timing
3. **Service Performance**: Latency percentiles and error rates
4. **Span Analysis**: Individual operation performance

### Performance Optimization Tips

1. **Identify Slow Services**: Look for services with high latency
2. **Database Query Analysis**: Find slow database operations
3. **Network Latency**: Identify network bottlenecks between services
4. **Error Patterns**: Correlate errors with performance issues
5. **Load Distribution**: Analyze load balancing effectiveness

## Summary

Today we covered:

1. **Distributed Tracing Fundamentals**: Understanding traces, spans, and correlation
2. **Spring Cloud Sleuth Setup**: Automatic instrumentation configuration
3. **Zipkin Integration**: Setting up trace collection and visualization
4. **Custom Spans**: Creating detailed operational visibility
5. **Service Integration**: Tracing across multiple microservices
6. **Async Processing**: Maintaining trace context in asynchronous operations
7. **Database Tracing**: Monitoring database operation performance
8. **Custom Annotations**: Simplified tracing with aspects
9. **Monitoring Integration**: Metrics and health checks with tracing
10. **Performance Analysis**: Using Zipkin for system optimization

## Key Takeaways

- Distributed tracing is essential for understanding microservices behavior
- Spring Cloud Sleuth provides automatic instrumentation for common operations
- Custom spans and tags provide detailed operational insights
- Zipkin offers powerful visualization and analysis capabilities
- Proper sampling strategies are crucial for production performance
- Trace correlation enables end-to-end request tracking
- Performance bottlenecks are easily identified through trace analysis

## Next Steps

Tomorrow we'll explore Spring Cloud Stream & Messaging, learning how to implement event-driven architectures with reliable message processing and stream processing capabilities.