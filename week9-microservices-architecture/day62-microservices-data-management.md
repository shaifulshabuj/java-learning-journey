# Day 62: Microservices Data Management

## Learning Objectives
- Master data management patterns in microservices
- Understand distributed transaction strategies
- Implement event-driven data synchronization
- Design CQRS and Event Sourcing patterns
- Handle data consistency and integrity challenges

---

## 1. Database per Service Pattern

### 1.1 Principle and Benefits

Each microservice should have its own database to ensure loose coupling and service autonomy.

```java
// User Service - PostgreSQL Database
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
    
    @Enumerated(EnumType.STRING)
    private UserStatus status;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Getters and setters
}

// User Service Repository
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByStatus(UserStatus status);
    
    @Query("SELECT u FROM User u WHERE u.createdAt >= :startDate")
    List<User> findRecentUsers(@Param("startDate") LocalDateTime startDate);
}
```

```java
// Product Service - MongoDB Database
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
    
    @Field
    private String category;
    
    @Field
    private String sku;
    
    @Field
    private List<String> tags;
    
    @Field
    private ProductAttributes attributes;
    
    @Field
    private ProductInventory inventory;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Getters and setters
}

// Product Service Repository
@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByCategory(String category);
    List<Product> findByTagsIn(List<String> tags);
    
    @Query("{'name': {$regex: ?0, $options: 'i'}}")
    List<Product> findByNameContainingIgnoreCase(String name);
    
    @Query("{'inventory.quantity': {$gte: ?0}}")
    List<Product> findByQuantityGreaterThanEqual(int quantity);
}
```

```java
// Order Service - PostgreSQL Database
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Reference to User Service (ID only, no foreign key)
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private BigDecimal totalAmount;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();
    
    @Column
    private String shippingAddress;
    
    @Column
    private String paymentMethod;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
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
    
    // Reference to Product Service (ID only, no foreign key)
    @Column(nullable = false)
    private String productId;
    
    @Column(nullable = false)
    private String productName; // Denormalized for performance
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false)
    private BigDecimal unitPrice;
    
    @Column(nullable = false)
    private BigDecimal totalPrice;
    
    // Getters and setters
}
```

---

## 2. Distributed Transaction Management

### 2.1 Saga Pattern Implementation

```java
// Saga Orchestrator for Order Processing
@Service
@Transactional
public class OrderSagaOrchestrator {
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    @Autowired
    private ProductServiceClient productServiceClient;
    
    @Autowired
    private InventoryServiceClient inventoryServiceClient;
    
    @Autowired
    private PaymentServiceClient paymentServiceClient;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private SagaStateRepository sagaStateRepository;
    
    public OrderResponse processOrder(OrderRequest request) {
        String sagaId = UUID.randomUUID().toString();
        
        // Create saga state
        SagaState sagaState = new SagaState();
        sagaState.setSagaId(sagaId);
        sagaState.setOrderRequest(request);
        sagaState.setCurrentStep(SagaStep.STARTED);
        sagaState.setStatus(SagaStatus.IN_PROGRESS);
        sagaStateRepository.save(sagaState);
        
        try {
            // Step 1: Validate User
            validateUser(sagaState);
            
            // Step 2: Validate Products
            validateProducts(sagaState);
            
            // Step 3: Reserve Inventory
            reserveInventory(sagaState);
            
            // Step 4: Process Payment
            processPayment(sagaState);
            
            // Step 5: Create Order
            Order order = createOrder(sagaState);
            
            // Step 6: Confirm all reservations
            confirmReservations(sagaState);
            
            sagaState.setStatus(SagaStatus.COMPLETED);
            sagaState.setOrderId(order.getId());
            sagaStateRepository.save(sagaState);
            
            return OrderResponse.fromOrder(order);
            
        } catch (Exception e) {
            handleSagaFailure(sagaState, e);
            throw new OrderProcessingException("Order processing failed", e);
        }
    }
    
    private void validateUser(SagaState sagaState) {
        try {
            UserValidationResponse response = userServiceClient.validateUser(
                sagaState.getOrderRequest().getUserId());
            
            if (!response.isValid()) {
                throw new UserValidationException("Invalid user");
            }
            
            sagaState.setCurrentStep(SagaStep.USER_VALIDATED);
            sagaState.getUserValidation().setValidated(true);
            sagaStateRepository.save(sagaState);
            
        } catch (Exception e) {
            sagaState.setCurrentStep(SagaStep.USER_VALIDATION_FAILED);
            sagaStateRepository.save(sagaState);
            throw e;
        }
    }
    
    private void validateProducts(SagaState sagaState) {
        try {
            List<String> productIds = sagaState.getOrderRequest().getItems().stream()
                .map(OrderItemRequest::getProductId)
                .collect(Collectors.toList());
            
            List<Product> products = productServiceClient.getProducts(productIds);
            
            if (products.size() != productIds.size()) {
                throw new ProductValidationException("Some products not found");
            }
            
            sagaState.setCurrentStep(SagaStep.PRODUCTS_VALIDATED);
            sagaState.getProductValidation().setValidatedProducts(products);
            sagaStateRepository.save(sagaState);
            
        } catch (Exception e) {
            sagaState.setCurrentStep(SagaStep.PRODUCT_VALIDATION_FAILED);
            sagaStateRepository.save(sagaState);
            throw e;
        }
    }
    
    private void reserveInventory(SagaState sagaState) {
        try {
            List<InventoryReservationRequest> reservations = sagaState.getOrderRequest().getItems().stream()
                .map(item -> new InventoryReservationRequest(
                    item.getProductId(), 
                    item.getQuantity(),
                    sagaState.getSagaId()))
                .collect(Collectors.toList());
            
            List<InventoryReservationResponse> responses = inventoryServiceClient.reserveInventory(reservations);
            
            sagaState.setCurrentStep(SagaStep.INVENTORY_RESERVED);
            sagaState.getInventoryReservation().setReservations(responses);
            sagaStateRepository.save(sagaState);
            
        } catch (Exception e) {
            sagaState.setCurrentStep(SagaStep.INVENTORY_RESERVATION_FAILED);
            sagaStateRepository.save(sagaState);
            throw e;
        }
    }
    
    private void processPayment(SagaState sagaState) {
        try {
            PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.setUserId(sagaState.getOrderRequest().getUserId());
            paymentRequest.setAmount(sagaState.getOrderRequest().getTotalAmount());
            paymentRequest.setPaymentMethod(sagaState.getOrderRequest().getPaymentMethod());
            paymentRequest.setTransactionId(sagaState.getSagaId());
            
            PaymentResponse response = paymentServiceClient.processPayment(paymentRequest);
            
            if (!response.isSuccess()) {
                throw new PaymentProcessingException("Payment failed: " + response.getErrorMessage());
            }
            
            sagaState.setCurrentStep(SagaStep.PAYMENT_PROCESSED);
            sagaState.getPaymentInfo().setPaymentId(response.getPaymentId());
            sagaStateRepository.save(sagaState);
            
        } catch (Exception e) {
            sagaState.setCurrentStep(SagaStep.PAYMENT_PROCESSING_FAILED);
            sagaStateRepository.save(sagaState);
            throw e;
        }
    }
    
    private Order createOrder(SagaState sagaState) {
        try {
            Order order = new Order();
            order.setUserId(sagaState.getOrderRequest().getUserId());
            order.setTotalAmount(sagaState.getOrderRequest().getTotalAmount());
            order.setStatus(OrderStatus.CONFIRMED);
            order.setShippingAddress(sagaState.getOrderRequest().getShippingAddress());
            order.setPaymentMethod(sagaState.getOrderRequest().getPaymentMethod());
            
            // Create order items
            List<OrderItem> items = sagaState.getOrderRequest().getItems().stream()
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
            sagaStateRepository.save(sagaState);
            
            return savedOrder;
            
        } catch (Exception e) {
            sagaState.setCurrentStep(SagaStep.ORDER_CREATION_FAILED);
            sagaStateRepository.save(sagaState);
            throw e;
        }
    }
    
    private void confirmReservations(SagaState sagaState) {
        try {
            List<String> reservationIds = sagaState.getInventoryReservation().getReservations().stream()
                .map(InventoryReservationResponse::getReservationId)
                .collect(Collectors.toList());
            
            inventoryServiceClient.confirmReservations(reservationIds, sagaState.getSagaId());
            
            sagaState.setCurrentStep(SagaStep.RESERVATIONS_CONFIRMED);
            sagaStateRepository.save(sagaState);
            
        } catch (Exception e) {
            sagaState.setCurrentStep(SagaStep.RESERVATION_CONFIRMATION_FAILED);
            sagaStateRepository.save(sagaState);
            throw e;
        }
    }
    
    private void handleSagaFailure(SagaState sagaState, Exception e) {
        sagaState.setStatus(SagaStatus.FAILED);
        sagaState.setErrorMessage(e.getMessage());
        sagaStateRepository.save(sagaState);
        
        // Compensate based on current step
        switch (sagaState.getCurrentStep()) {
            case RESERVATIONS_CONFIRMED:
            case ORDER_CREATED:
                cancelReservations(sagaState);
                // Fall through
            case PAYMENT_PROCESSED:
                refundPayment(sagaState);
                break;
            case INVENTORY_RESERVED:
                cancelReservations(sagaState);
                break;
            default:
                // No compensation needed
                break;
        }
    }
    
    private void cancelReservations(SagaState sagaState) {
        try {
            List<String> reservationIds = sagaState.getInventoryReservation().getReservations().stream()
                .map(InventoryReservationResponse::getReservationId)
                .collect(Collectors.toList());
            
            inventoryServiceClient.cancelReservations(reservationIds, sagaState.getSagaId());
        } catch (Exception e) {
            logger.error("Failed to cancel reservations for saga {}: {}", sagaState.getSagaId(), e.getMessage());
        }
    }
    
    private void refundPayment(SagaState sagaState) {
        try {
            String paymentId = sagaState.getPaymentInfo().getPaymentId();
            paymentServiceClient.refundPayment(paymentId, sagaState.getSagaId());
        } catch (Exception e) {
            logger.error("Failed to refund payment for saga {}: {}", sagaState.getSagaId(), e.getMessage());
        }
    }
}
```

### 2.2 Saga State Management

```java
// Saga State Entity
@Entity
@Table(name = "saga_states")
public class SagaState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String sagaId;
    
    @Enumerated(EnumType.STRING)
    private SagaStep currentStep;
    
    @Enumerated(EnumType.STRING)
    private SagaStatus status;
    
    @Column
    private Long orderId;
    
    @Column(columnDefinition = "TEXT")
    private String orderRequestJson;
    
    @Column
    private String errorMessage;
    
    @Embedded
    private UserValidationInfo userValidation;
    
    @Embedded
    private ProductValidationInfo productValidation;
    
    @Embedded
    private InventoryReservationInfo inventoryReservation;
    
    @Embedded
    private PaymentInfo paymentInfo;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Getters and setters
    
    public OrderRequest getOrderRequest() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(orderRequestJson, OrderRequest.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize order request", e);
        }
    }
    
    public void setOrderRequest(OrderRequest orderRequest) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.orderRequestJson = mapper.writeValueAsString(orderRequest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize order request", e);
        }
    }
}

// Saga Step Enum
public enum SagaStep {
    STARTED,
    USER_VALIDATED,
    USER_VALIDATION_FAILED,
    PRODUCTS_VALIDATED,
    PRODUCT_VALIDATION_FAILED,
    INVENTORY_RESERVED,
    INVENTORY_RESERVATION_FAILED,
    PAYMENT_PROCESSED,
    PAYMENT_PROCESSING_FAILED,
    ORDER_CREATED,
    ORDER_CREATION_FAILED,
    RESERVATIONS_CONFIRMED,
    RESERVATION_CONFIRMATION_FAILED
}

// Saga Status Enum
public enum SagaStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    COMPENSATED
}

// Embedded Info Classes
@Embeddable
public class UserValidationInfo {
    private boolean validated;
    private String validationMessage;
    
    // Getters and setters
}

@Embeddable
public class ProductValidationInfo {
    @Column(columnDefinition = "TEXT")
    private String validatedProductsJson;
    
    public List<Product> getValidatedProducts() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(validatedProductsJson, new TypeReference<List<Product>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    public void setValidatedProducts(List<Product> products) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.validatedProductsJson = mapper.writeValueAsString(products);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize products", e);
        }
    }
}

@Embeddable
public class InventoryReservationInfo {
    @Column(columnDefinition = "TEXT")
    private String reservationsJson;
    
    public List<InventoryReservationResponse> getReservations() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(reservationsJson, new TypeReference<List<InventoryReservationResponse>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    public void setReservations(List<InventoryReservationResponse> reservations) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.reservationsJson = mapper.writeValueAsString(reservations);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize reservations", e);
        }
    }
}

@Embeddable
public class PaymentInfo {
    private String paymentId;
    private String transactionId;
    private BigDecimal amount;
    
    // Getters and setters
}
```

---

## 3. Event-Driven Data Synchronization

### 3.1 Domain Events

```java
// Base Domain Event
@MappedSuperclass
public abstract class DomainEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String eventId;
    
    @Column(nullable = false)
    private String eventType;
    
    @Column(nullable = false)
    private String aggregateId;
    
    @Column(nullable = false)
    private String aggregateType;
    
    @Column(nullable = false)
    private Long version;
    
    @Column(columnDefinition = "TEXT")
    private String eventData;
    
    @CreationTimestamp
    private LocalDateTime occurredOn;
    
    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
    }
    
    // Getters and setters
}

// User Domain Events
@Entity
@Table(name = "user_events")
public class UserEvent extends DomainEvent {
    
    public static UserEvent userCreated(User user) {
        UserEvent event = new UserEvent();
        event.setEventType("USER_CREATED");
        event.setAggregateId(user.getId().toString());
        event.setAggregateType("USER");
        event.setVersion(1L);
        
        UserCreatedEventData data = new UserCreatedEventData();
        data.setUserId(user.getId());
        data.setEmail(user.getEmail());
        data.setFirstName(user.getFirstName());
        data.setLastName(user.getLastName());
        data.setCreatedAt(user.getCreatedAt());
        
        event.setEventData(data);
        return event;
    }
    
    public static UserEvent userUpdated(User user, Long version) {
        UserEvent event = new UserEvent();
        event.setEventType("USER_UPDATED");
        event.setAggregateId(user.getId().toString());
        event.setAggregateType("USER");
        event.setVersion(version);
        
        UserUpdatedEventData data = new UserUpdatedEventData();
        data.setUserId(user.getId());
        data.setEmail(user.getEmail());
        data.setFirstName(user.getFirstName());
        data.setLastName(user.getLastName());
        data.setUpdatedAt(user.getUpdatedAt());
        
        event.setEventData(data);
        return event;
    }
    
    private void setEventData(Object data) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            super.setEventData(mapper.writeValueAsString(data));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event data", e);
        }
    }
}

// Event Data Classes
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreatedEventData {
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private LocalDateTime createdAt;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdatedEventData {
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private LocalDateTime updatedAt;
}
```

### 3.2 Event Store and Publishing

```java
// Event Store
@Service
@Transactional
public class EventStore {
    
    @Autowired
    private UserEventRepository userEventRepository;
    
    @Autowired
    private OrderEventRepository orderEventRepository;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    public void saveAndPublish(DomainEvent event) {
        // Save event to event store
        if (event instanceof UserEvent) {
            userEventRepository.save((UserEvent) event);
        } else if (event instanceof OrderEvent) {
            orderEventRepository.save((OrderEvent) event);
        }
        
        // Publish event for immediate processing
        eventPublisher.publishEvent(event);
    }
    
    public List<DomainEvent> getEvents(String aggregateId, String aggregateType) {
        List<DomainEvent> events = new ArrayList<>();
        
        if ("USER".equals(aggregateType)) {
            events.addAll(userEventRepository.findByAggregateIdOrderByVersionAsc(aggregateId));
        } else if ("ORDER".equals(aggregateType)) {
            events.addAll(orderEventRepository.findByAggregateIdOrderByVersionAsc(aggregateId));
        }
        
        return events;
    }
    
    public List<DomainEvent> getEventsAfter(LocalDateTime timestamp) {
        List<DomainEvent> events = new ArrayList<>();
        
        events.addAll(userEventRepository.findByOccurredOnAfterOrderByOccurredOnAsc(timestamp));
        events.addAll(orderEventRepository.findByOccurredOnAfterOrderByOccurredOnAsc(timestamp));
        
        return events.stream()
                .sorted(Comparator.comparing(DomainEvent::getOccurredOn))
                .collect(Collectors.toList());
    }
}

// Event Publishing Service
@Service
public class EventPublishingService {
    
    @Autowired
    private EventStore eventStore;
    
    @Autowired
    private OutboxEventRepository outboxEventRepository;
    
    @Autowired
    private MessagePublisher messagePublisher;
    
    @EventListener
    @Transactional
    public void handleDomainEvent(DomainEvent event) {
        // Create outbox event for reliable messaging
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setEventId(event.getEventId());
        outboxEvent.setEventType(event.getEventType());
        outboxEvent.setAggregateId(event.getAggregateId());
        outboxEvent.setAggregateType(event.getAggregateType());
        outboxEvent.setEventData(event.getEventData());
        outboxEvent.setStatus(OutboxStatus.PENDING);
        outboxEvent.setCreatedAt(LocalDateTime.now());
        
        outboxEventRepository.save(outboxEvent);
    }
    
    @Scheduled(fixedDelay = 5000) // Every 5 seconds
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        
        for (OutboxEvent event : pendingEvents) {
            try {
                messagePublisher.publish(event);
                
                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(LocalDateTime.now());
                outboxEventRepository.save(event);
                
            } catch (Exception e) {
                logger.error("Failed to publish event {}: {}", event.getEventId(), e.getMessage());
                
                event.setStatus(OutboxStatus.FAILED);
                event.setErrorMessage(e.getMessage());
                event.setRetryCount(event.getRetryCount() + 1);
                outboxEventRepository.save(event);
            }
        }
    }
}
```

### 3.3 Event Handlers for Data Synchronization

```java
// User Profile Service - handles User events
@Service
@Transactional
public class UserProfileEventHandler {
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    @RabbitListener(queues = "user-profile.user.created")
    public void handleUserCreated(UserCreatedEventData eventData) {
        logger.info("Creating user profile for user {}", eventData.getUserId());
        
        try {
            // Create default profile
            UserProfile profile = new UserProfile();
            profile.setUserId(eventData.getUserId());
            profile.setEmail(eventData.getEmail());
            profile.setFirstName(eventData.getFirstName());
            profile.setLastName(eventData.getLastName());
            profile.setStatus(ProfileStatus.ACTIVE);
            
            userProfileRepository.save(profile);
            
            logger.info("User profile created successfully for user {}", eventData.getUserId());
            
        } catch (Exception e) {
            logger.error("Failed to create user profile for user {}: {}", eventData.getUserId(), e.getMessage());
            throw e;
        }
    }
    
    @RabbitListener(queues = "user-profile.user.updated")
    public void handleUserUpdated(UserUpdatedEventData eventData) {
        logger.info("Updating user profile for user {}", eventData.getUserId());
        
        try {
            Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(eventData.getUserId());
            
            if (profileOpt.isPresent()) {
                UserProfile profile = profileOpt.get();
                profile.setEmail(eventData.getEmail());
                profile.setFirstName(eventData.getFirstName());
                profile.setLastName(eventData.getLastName());
                profile.setUpdatedAt(LocalDateTime.now());
                
                userProfileRepository.save(profile);
                
                logger.info("User profile updated successfully for user {}", eventData.getUserId());
            } else {
                logger.warn("User profile not found for user {}", eventData.getUserId());
            }
            
        } catch (Exception e) {
            logger.error("Failed to update user profile for user {}: {}", eventData.getUserId(), e.getMessage());
            throw e;
        }
    }
}

// Order Analytics Service - handles Order events
@Service
@Transactional
public class OrderAnalyticsEventHandler {
    
    @Autowired
    private OrderAnalyticsRepository orderAnalyticsRepository;
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    @RabbitListener(queues = "order-analytics.order.created")
    public void handleOrderCreated(OrderCreatedEventData eventData) {
        logger.info("Processing order analytics for order {}", eventData.getOrderId());
        
        try {
            // Get user information for analytics
            Optional<User> userOpt = userServiceClient.getUser(eventData.getUserId());
            
            OrderAnalytics analytics = new OrderAnalytics();
            analytics.setOrderId(eventData.getOrderId());
            analytics.setUserId(eventData.getUserId());
            analytics.setTotalAmount(eventData.getTotalAmount());
            analytics.setOrderDate(eventData.getCreatedAt());
            analytics.setItemCount(eventData.getItemCount());
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                analytics.setUserType(determineUserType(user));
                analytics.setUserSegment(determineUserSegment(user));
            }
            
            orderAnalyticsRepository.save(analytics);
            
            logger.info("Order analytics processed successfully for order {}", eventData.getOrderId());
            
        } catch (Exception e) {
            logger.error("Failed to process order analytics for order {}: {}", eventData.getOrderId(), e.getMessage());
            throw e;
        }
    }
    
    private UserType determineUserType(User user) {
        long daysSinceCreation = ChronoUnit.DAYS.between(user.getCreatedAt(), LocalDateTime.now());
        return daysSinceCreation > 365 ? UserType.EXISTING : UserType.NEW;
    }
    
    private UserSegment determineUserSegment(User user) {
        // Logic to determine user segment based on order history
        List<OrderAnalytics> userOrders = orderAnalyticsRepository.findByUserId(user.getId());
        
        if (userOrders.isEmpty()) {
            return UserSegment.FIRST_TIME;
        }
        
        BigDecimal totalSpent = userOrders.stream()
                .map(OrderAnalytics::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalSpent.compareTo(new BigDecimal("1000")) >= 0) {
            return UserSegment.VIP;
        } else if (totalSpent.compareTo(new BigDecimal("500")) >= 0) {
            return UserSegment.REGULAR;
        } else {
            return UserSegment.CASUAL;
        }
    }
}
```

---

## 4. CQRS (Command Query Responsibility Segregation)

### 4.1 Command Side Implementation

```java
// Command Model - Write Side
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private BigDecimal price;
    
    @Column(nullable = false)
    private String category;
    
    @Column(nullable = false)
    private String sku;
    
    @Column(nullable = false)
    private Integer stockQuantity;
    
    @Column(nullable = false)
    private Integer reservedQuantity;
    
    @Version
    private Long version;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Business methods
    public void updateStock(int quantity) {
        if (this.stockQuantity + quantity < 0) {
            throw new InsufficientStockException("Insufficient stock for product " + this.sku);
        }
        this.stockQuantity += quantity;
    }
    
    public void reserveStock(int quantity) {
        if (this.stockQuantity < quantity) {
            throw new InsufficientStockException("Insufficient stock for reservation");
        }
        this.stockQuantity -= quantity;
        this.reservedQuantity += quantity;
    }
    
    public void confirmReservation(int quantity) {
        if (this.reservedQuantity < quantity) {
            throw new InvalidReservationException("Invalid reservation confirmation");
        }
        this.reservedQuantity -= quantity;
    }
    
    public void cancelReservation(int quantity) {
        if (this.reservedQuantity < quantity) {
            throw new InvalidReservationException("Invalid reservation cancellation");
        }
        this.reservedQuantity -= quantity;
        this.stockQuantity += quantity;
    }
    
    // Getters and setters
}

// Command Handlers
@Service
@Transactional
public class ProductCommandHandler {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private EventStore eventStore;
    
    public ProductResponse createProduct(CreateProductCommand command) {
        // Validate command
        validateCreateProductCommand(command);
        
        // Create product
        Product product = new Product();
        product.setName(command.getName());
        product.setDescription(command.getDescription());
        product.setPrice(command.getPrice());
        product.setCategory(command.getCategory());
        product.setSku(command.getSku());
        product.setStockQuantity(command.getInitialStock());
        product.setReservedQuantity(0);
        
        Product savedProduct = productRepository.save(product);
        
        // Create and publish event
        ProductEvent event = ProductEvent.productCreated(savedProduct);
        eventStore.saveAndPublish(event);
        
        return ProductResponse.fromProduct(savedProduct);
    }
    
    public ProductResponse updateProduct(UpdateProductCommand command) {
        Product product = productRepository.findById(command.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        
        // Update product
        product.setName(command.getName());
        product.setDescription(command.getDescription());
        product.setPrice(command.getPrice());
        product.setCategory(command.getCategory());
        
        Product savedProduct = productRepository.save(product);
        
        // Create and publish event
        ProductEvent event = ProductEvent.productUpdated(savedProduct);
        eventStore.saveAndPublish(event);
        
        return ProductResponse.fromProduct(savedProduct);
    }
    
    public void updateStock(UpdateStockCommand command) {
        Product product = productRepository.findById(command.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        
        int oldStock = product.getStockQuantity();
        product.updateStock(command.getQuantityChange());
        
        Product savedProduct = productRepository.save(product);
        
        // Create and publish event
        ProductEvent event = ProductEvent.stockUpdated(savedProduct, oldStock, savedProduct.getStockQuantity());
        eventStore.saveAndPublish(event);
    }
    
    public ReservationResponse reserveStock(ReserveStockCommand command) {
        Product product = productRepository.findById(command.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        
        product.reserveStock(command.getQuantity());
        productRepository.save(product);
        
        // Create reservation record
        StockReservation reservation = new StockReservation();
        reservation.setReservationId(UUID.randomUUID().toString());
        reservation.setProductId(command.getProductId());
        reservation.setQuantity(command.getQuantity());
        reservation.setTransactionId(command.getTransactionId());
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(30)); // 30 minute expiry
        
        // Save reservation and publish event
        ProductEvent event = ProductEvent.stockReserved(product, reservation);
        eventStore.saveAndPublish(event);
        
        return ReservationResponse.fromReservation(reservation);
    }
    
    private void validateCreateProductCommand(CreateProductCommand command) {
        if (productRepository.existsBySku(command.getSku())) {
            throw new ProductValidationException("Product with SKU already exists");
        }
        
        if (command.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ProductValidationException("Price must be positive");
        }
        
        if (command.getInitialStock() < 0) {
            throw new ProductValidationException("Initial stock cannot be negative");
        }
    }
}
```

### 4.2 Query Side Implementation

```java
// Query Model - Read Side
@Document(collection = "product_catalog")
public class ProductCatalog {
    @Id
    private String id;
    
    @Field
    private Long productId;
    
    @Field
    private String name;
    
    @Field
    private String description;
    
    @Field
    private BigDecimal price;
    
    @Field
    private String category;
    
    @Field
    private String sku;
    
    @Field
    private Integer availableQuantity;
    
    @Field
    private List<String> tags;
    
    @Field
    private ProductRating rating;
    
    @Field
    private ProductImages images;
    
    @Field
    private ProductAttributes attributes;
    
    @Field
    private LocalDateTime createdAt;
    
    @Field
    private LocalDateTime updatedAt;
    
    // Getters and setters
}

// Query Handlers
@Service
@Transactional(readOnly = true)
public class ProductQueryHandler {
    
    @Autowired
    private ProductCatalogRepository productCatalogRepository;
    
    @Autowired
    private ProductSearchService productSearchService;
    
    public ProductCatalogResponse getProduct(Long productId) {
        ProductCatalog product = productCatalogRepository.findByProductId(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        
        return ProductCatalogResponse.fromProductCatalog(product);
    }
    
    public List<ProductCatalogResponse> getProducts(List<Long> productIds) {
        List<ProductCatalog> products = productCatalogRepository.findByProductIdIn(productIds);
        
        return products.stream()
                .map(ProductCatalogResponse::fromProductCatalog)
                .collect(Collectors.toList());
    }
    
    public PagedResponse<ProductCatalogResponse> getProductsByCategory(
            String category, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductCatalog> products = productCatalogRepository.findByCategory(category, pageable);
        
        List<ProductCatalogResponse> content = products.getContent().stream()
                .map(ProductCatalogResponse::fromProductCatalog)
                .collect(Collectors.toList());
        
        return new PagedResponse<>(content, products.getNumber(), products.getSize(),
                products.getTotalElements(), products.getTotalPages());
    }
    
    public List<ProductCatalogResponse> searchProducts(ProductSearchRequest request) {
        return productSearchService.searchProducts(request);
    }
    
    public List<ProductCatalogResponse> getRecommendedProducts(Long userId, int limit) {
        // Implementation for personalized recommendations
        return productSearchService.getRecommendedProducts(userId, limit);
    }
}

// Query Side Event Handlers
@Service
@Transactional
public class ProductCatalogEventHandler {
    
    @Autowired
    private ProductCatalogRepository productCatalogRepository;
    
    @RabbitListener(queues = "product-catalog.product.created")
    public void handleProductCreated(ProductCreatedEventData eventData) {
        ProductCatalog catalog = new ProductCatalog();
        catalog.setProductId(eventData.getProductId());
        catalog.setName(eventData.getName());
        catalog.setDescription(eventData.getDescription());
        catalog.setPrice(eventData.getPrice());
        catalog.setCategory(eventData.getCategory());
        catalog.setSku(eventData.getSku());
        catalog.setAvailableQuantity(eventData.getInitialStock());
        catalog.setTags(Arrays.asList(eventData.getCategory().toLowerCase()));
        catalog.setCreatedAt(eventData.getCreatedAt());
        catalog.setUpdatedAt(eventData.getCreatedAt());
        
        // Initialize default rating and attributes
        catalog.setRating(new ProductRating());
        catalog.setAttributes(new ProductAttributes());
        catalog.setImages(new ProductImages());
        
        productCatalogRepository.save(catalog);
    }
    
    @RabbitListener(queues = "product-catalog.product.updated")
    public void handleProductUpdated(ProductUpdatedEventData eventData) {
        Optional<ProductCatalog> catalogOpt = productCatalogRepository.findByProductId(eventData.getProductId());
        
        if (catalogOpt.isPresent()) {
            ProductCatalog catalog = catalogOpt.get();
            catalog.setName(eventData.getName());
            catalog.setDescription(eventData.getDescription());
            catalog.setPrice(eventData.getPrice());
            catalog.setCategory(eventData.getCategory());
            catalog.setUpdatedAt(eventData.getUpdatedAt());
            
            productCatalogRepository.save(catalog);
        }
    }
    
    @RabbitListener(queues = "product-catalog.stock.updated")
    public void handleStockUpdated(StockUpdatedEventData eventData) {
        Optional<ProductCatalog> catalogOpt = productCatalogRepository.findByProductId(eventData.getProductId());
        
        if (catalogOpt.isPresent()) {
            ProductCatalog catalog = catalogOpt.get();
            catalog.setAvailableQuantity(eventData.getNewStock());
            catalog.setUpdatedAt(LocalDateTime.now());
            
            productCatalogRepository.save(catalog);
        }
    }
}
```

---

## 5. Event Sourcing Implementation

### 5.1 Event Store

```java
// Event Store for Event Sourcing
@Service
@Transactional
public class EventSourcingStore {
    
    @Autowired
    private EventStreamRepository eventStreamRepository;
    
    @Autowired
    private SnapshotRepository snapshotRepository;
    
    public void saveEvents(String aggregateId, List<DomainEvent> events, long expectedVersion) {
        // Check for concurrency conflicts
        long currentVersion = getLastVersion(aggregateId);
        if (currentVersion != expectedVersion) {
            throw new ConcurrencyException("Aggregate version mismatch");
        }
        
        // Save events
        for (DomainEvent event : events) {
            EventStream eventStream = new EventStream();
            eventStream.setAggregateId(aggregateId);
            eventStream.setAggregateType(event.getAggregateType());
            eventStream.setEventType(event.getEventType());
            eventStream.setEventData(event.getEventData());
            eventStream.setVersion(++currentVersion);
            eventStream.setOccurredOn(event.getOccurredOn());
            
            eventStreamRepository.save(eventStream);
        }
    }
    
    public List<DomainEvent> getEvents(String aggregateId, long fromVersion) {
        List<EventStream> eventStreams = eventStreamRepository
                .findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(aggregateId, fromVersion);
        
        return eventStreams.stream()
                .map(this::toDomainEvent)
                .collect(Collectors.toList());
    }
    
    public <T> T getAggregate(String aggregateId, Class<T> aggregateType) {
        // Try to load from snapshot first
        Optional<Snapshot> snapshotOpt = snapshotRepository.findByAggregateIdAndAggregateType(
                aggregateId, aggregateType.getSimpleName());
        
        T aggregate;
        long fromVersion;
        
        if (snapshotOpt.isPresent()) {
            Snapshot snapshot = snapshotOpt.get();
            aggregate = deserializeSnapshot(snapshot.getSnapshotData(), aggregateType);
            fromVersion = snapshot.getVersion();
        } else {
            try {
                aggregate = aggregateType.getDeclaredConstructor().newInstance();
                fromVersion = 0;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create aggregate instance", e);
            }
        }
        
        // Load and apply events since snapshot
        List<DomainEvent> events = getEvents(aggregateId, fromVersion);
        applyEvents(aggregate, events);
        
        return aggregate;
    }
    
    public void saveSnapshot(String aggregateId, Object aggregate, long version) {
        String aggregateType = aggregate.getClass().getSimpleName();
        String snapshotData = serializeSnapshot(aggregate);
        
        Snapshot snapshot = new Snapshot();
        snapshot.setAggregateId(aggregateId);
        snapshot.setAggregateType(aggregateType);
        snapshot.setVersion(version);
        snapshot.setSnapshotData(snapshotData);
        snapshot.setCreatedAt(LocalDateTime.now());
        
        snapshotRepository.save(snapshot);
    }
    
    private long getLastVersion(String aggregateId) {
        return eventStreamRepository.findTopByAggregateIdOrderByVersionDesc(aggregateId)
                .map(EventStream::getVersion)
                .orElse(0L);
    }
    
    private DomainEvent toDomainEvent(EventStream eventStream) {
        // Convert EventStream to appropriate DomainEvent based on event type
        switch (eventStream.getEventType()) {
            case "USER_CREATED":
                return createUserEvent(eventStream);
            case "ORDER_CREATED":
                return createOrderEvent(eventStream);
            default:
                throw new UnsupportedEventTypeException("Unsupported event type: " + eventStream.getEventType());
        }
    }
    
    private void applyEvents(Object aggregate, List<DomainEvent> events) {
        for (DomainEvent event : events) {
            try {
                Method applyMethod = aggregate.getClass().getMethod("apply", event.getClass());
                applyMethod.invoke(aggregate, event);
            } catch (Exception e) {
                throw new RuntimeException("Failed to apply event", e);
            }
        }
    }
    
    private String serializeSnapshot(Object aggregate) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(aggregate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize snapshot", e);
        }
    }
    
    private <T> T deserializeSnapshot(String snapshotData, Class<T> aggregateType) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(snapshotData, aggregateType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize snapshot", e);
        }
    }
}
```

### 5.2 Event-Sourced Aggregate

```java
// Event-Sourced Order Aggregate
public class OrderAggregate {
    private String orderId;
    private Long userId;
    private List<OrderItemAggregate> items;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long version;
    
    // Uncommitted events
    private List<DomainEvent> uncommittedEvents = new ArrayList<>();
    
    public OrderAggregate() {
        this.items = new ArrayList<>();
    }
    
    public static OrderAggregate create(String orderId, Long userId, List<OrderItemRequest> itemRequests) {
        OrderAggregate aggregate = new OrderAggregate();
        
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(orderId);
        event.setUserId(userId);
        event.setItemRequests(itemRequests);
        event.setCreatedAt(LocalDateTime.now());
        
        aggregate.apply(event);
        aggregate.markEventAsUncommitted(event);
        
        return aggregate;
    }
    
    public void addItem(String productId, String productName, int quantity, BigDecimal unitPrice) {
        OrderItemAddedEvent event = new OrderItemAddedEvent();
        event.setOrderId(this.orderId);
        event.setProductId(productId);
        event.setProductName(productName);
        event.setQuantity(quantity);
        event.setUnitPrice(unitPrice);
        event.setTotalPrice(unitPrice.multiply(new BigDecimal(quantity)));
        event.setOccurredOn(LocalDateTime.now());
        
        apply(event);
        markEventAsUncommitted(event);
    }
    
    public void updateStatus(OrderStatus newStatus) {
        if (this.status == newStatus) {
            return; // No change needed
        }
        
        OrderStatusUpdatedEvent event = new OrderStatusUpdatedEvent();
        event.setOrderId(this.orderId);
        event.setOldStatus(this.status);
        event.setNewStatus(newStatus);
        event.setOccurredOn(LocalDateTime.now());
        
        apply(event);
        markEventAsUncommitted(event);
    }
    
    public void confirmPayment(String paymentId, BigDecimal amount) {
        PaymentConfirmedEvent event = new PaymentConfirmedEvent();
        event.setOrderId(this.orderId);
        event.setPaymentId(paymentId);
        event.setAmount(amount);
        event.setOccurredOn(LocalDateTime.now());
        
        apply(event);
        markEventAsUncommitted(event);
    }
    
    // Event application methods
    public void apply(OrderCreatedEvent event) {
        this.orderId = event.getOrderId();
        this.userId = event.getUserId();
        this.items = new ArrayList<>();
        this.totalAmount = BigDecimal.ZERO;
        this.status = OrderStatus.PENDING;
        this.createdAt = event.getCreatedAt();
        this.updatedAt = event.getCreatedAt();
        this.version++;
    }
    
    public void apply(OrderItemAddedEvent event) {
        OrderItemAggregate item = new OrderItemAggregate();
        item.setProductId(event.getProductId());
        item.setProductName(event.getProductName());
        item.setQuantity(event.getQuantity());
        item.setUnitPrice(event.getUnitPrice());
        item.setTotalPrice(event.getTotalPrice());
        
        this.items.add(item);
        this.totalAmount = this.totalAmount.add(event.getTotalPrice());
        this.updatedAt = event.getOccurredOn();
        this.version++;
    }
    
    public void apply(OrderStatusUpdatedEvent event) {
        this.status = event.getNewStatus();
        this.updatedAt = event.getOccurredOn();
        this.version++;
    }
    
    public void apply(PaymentConfirmedEvent event) {
        this.status = OrderStatus.PAID;
        this.updatedAt = event.getOccurredOn();
        this.version++;
    }
    
    // Utility methods
    public List<DomainEvent> getUncommittedEvents() {
        return new ArrayList<>(uncommittedEvents);
    }
    
    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }
    
    private void markEventAsUncommitted(DomainEvent event) {
        uncommittedEvents.add(event);
    }
    
    // Getters and setters
    public String getOrderId() { return orderId; }
    public Long getUserId() { return userId; }
    public List<OrderItemAggregate> getItems() { return items; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}

// Order Aggregate Repository
@Service
@Transactional
public class OrderAggregateRepository {
    
    @Autowired
    private EventSourcingStore eventSourcingStore;
    
    public void save(OrderAggregate aggregate) {
        List<DomainEvent> uncommittedEvents = aggregate.getUncommittedEvents();
        
        if (!uncommittedEvents.isEmpty()) {
            eventSourcingStore.saveEvents(aggregate.getOrderId(), uncommittedEvents, aggregate.getVersion() - uncommittedEvents.size());
            aggregate.markEventsAsCommitted();
            
            // Create snapshot every 10 events
            if (aggregate.getVersion() % 10 == 0) {
                eventSourcingStore.saveSnapshot(aggregate.getOrderId(), aggregate, aggregate.getVersion());
            }
        }
    }
    
    public Optional<OrderAggregate> findById(String orderId) {
        try {
            OrderAggregate aggregate = eventSourcingStore.getAggregate(orderId, OrderAggregate.class);
            return Optional.of(aggregate);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
```

---

## 6. Data Consistency Patterns

### 6.1 Eventually Consistent Reads

```java
// Eventually Consistent Read Service
@Service
public class EventuallyConsistentReadService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String USER_CACHE_KEY = "user:";
    private static final String USER_PROFILE_CACHE_KEY = "user_profile:";
    
    public CompletedUserView getCompletedUserView(Long userId) {
        String cacheKey = "completed_user:" + userId;
        
        // Try cache first
        CompletedUserView cachedView = (CompletedUserView) redisTemplate.opsForValue().get(cacheKey);
        if (cachedView != null) {
            return cachedView;
        }
        
        // Load from multiple sources
        CompletableFuture<User> userFuture = CompletableFuture.supplyAsync(() -> {
            return userRepository.findById(userId).orElse(null);
        });
        
        CompletableFuture<UserProfile> profileFuture = CompletableFuture.supplyAsync(() -> {
            return userProfileRepository.findByUserId(userId).orElse(null);
        });
        
        try {
            User user = userFuture.get(2, TimeUnit.SECONDS);
            UserProfile profile = profileFuture.get(2, TimeUnit.SECONDS);
            
            CompletedUserView view = new CompletedUserView();
            
            if (user != null) {
                view.setUserId(user.getId());
                view.setEmail(user.getEmail());
                view.setFirstName(user.getFirstName());
                view.setLastName(user.getLastName());
                view.setCreatedAt(user.getCreatedAt());
            }
            
            if (profile != null) {
                view.setPhoneNumber(profile.getPhoneNumber());
                view.setAddress(profile.getAddress());
                view.setCity(profile.getCity());
                view.setCountry(profile.getCountry());
            }
            
            view.setDataConsistency(determineDataConsistency(user, profile));
            
            // Cache the result
            redisTemplate.opsForValue().set(cacheKey, view, Duration.ofMinutes(5));
            
            return view;
            
        } catch (TimeoutException e) {
            throw new ServiceUnavailableException("Timeout loading user data");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load completed user view", e);
        }
    }
    
    private DataConsistency determineDataConsistency(User user, UserProfile profile) {
        if (user == null || profile == null) {
            return DataConsistency.INCONSISTENT;
        }
        
        // Check timestamp consistency
        if (user.getUpdatedAt().isAfter(profile.getUpdatedAt().plusMinutes(5))) {
            return DataConsistency.EVENTUAL;
        }
        
        return DataConsistency.CONSISTENT;
    }
}

// Data Consistency Enum
public enum DataConsistency {
    CONSISTENT,
    EVENTUAL,
    INCONSISTENT
}

// Completed User View DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompletedUserView {
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;
    private String city;
    private String country;
    private LocalDateTime createdAt;
    private DataConsistency dataConsistency;
}
```

### 6.2 Conflict Resolution

```java
// Conflict Resolution Service
@Service
@Transactional
public class ConflictResolutionService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    @Autowired
    private ConflictLogRepository conflictLogRepository;
    
    public void resolveUserDataConflicts(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        
        if (userOpt.isPresent() && profileOpt.isPresent()) {
            User user = userOpt.get();
            UserProfile profile = profileOpt.get();
            
            List<DataConflict> conflicts = detectConflicts(user, profile);
            
            if (!conflicts.isEmpty()) {
                resolveConflicts(user, profile, conflicts);
            }
        }
    }
    
    private List<DataConflict> detectConflicts(User user, UserProfile profile) {
        List<DataConflict> conflicts = new ArrayList<>();
        
        // Check email consistency
        if (!user.getEmail().equals(profile.getEmail())) {
            conflicts.add(new DataConflict(
                ConflictType.EMAIL_MISMATCH,
                user.getEmail(),
                profile.getEmail(),
                user.getUpdatedAt(),
                profile.getUpdatedAt()
            ));
        }
        
        // Check name consistency
        if (!user.getFirstName().equals(profile.getFirstName()) ||
            !user.getLastName().equals(profile.getLastName())) {
            conflicts.add(new DataConflict(
                ConflictType.NAME_MISMATCH,
                user.getFirstName() + " " + user.getLastName(),
                profile.getFirstName() + " " + profile.getLastName(),
                user.getUpdatedAt(),
                profile.getUpdatedAt()
            ));
        }
        
        return conflicts;
    }
    
    private void resolveConflicts(User user, UserProfile profile, List<DataConflict> conflicts) {
        for (DataConflict conflict : conflicts) {
            switch (conflict.getType()) {
                case EMAIL_MISMATCH:
                    resolveEmailConflict(user, profile, conflict);
                    break;
                case NAME_MISMATCH:
                    resolveNameConflict(user, profile, conflict);
                    break;
                default:
                    logUnresolvedConflict(conflict);
                    break;
            }
        }
    }
    
    private void resolveEmailConflict(User user, UserProfile profile, DataConflict conflict) {
        // Use Last-Writer-Wins strategy
        if (conflict.getSourceTimestamp().isAfter(conflict.getTargetTimestamp())) {
            // User service has newer data
            profile.setEmail(user.getEmail());
            profile.setUpdatedAt(LocalDateTime.now());
            userProfileRepository.save(profile);
            
            logConflictResolution(conflict, ConflictResolution.SOURCE_WINS);
        } else {
            // Profile service has newer data
            user.setEmail(profile.getEmail());
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            
            logConflictResolution(conflict, ConflictResolution.TARGET_WINS);
        }
    }
    
    private void resolveNameConflict(User user, UserProfile profile, DataConflict conflict) {
        // Use Last-Writer-Wins strategy
        if (conflict.getSourceTimestamp().isAfter(conflict.getTargetTimestamp())) {
            profile.setFirstName(user.getFirstName());
            profile.setLastName(user.getLastName());
            profile.setUpdatedAt(LocalDateTime.now());
            userProfileRepository.save(profile);
            
            logConflictResolution(conflict, ConflictResolution.SOURCE_WINS);
        } else {
            user.setFirstName(profile.getFirstName());
            user.setLastName(profile.getLastName());
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            
            logConflictResolution(conflict, ConflictResolution.TARGET_WINS);
        }
    }
    
    private void logConflictResolution(DataConflict conflict, ConflictResolution resolution) {
        ConflictLog log = new ConflictLog();
        log.setConflictType(conflict.getType());
        log.setSourceValue(conflict.getSourceValue());
        log.setTargetValue(conflict.getTargetValue());
        log.setResolution(resolution);
        log.setResolvedAt(LocalDateTime.now());
        
        conflictLogRepository.save(log);
    }
    
    private void logUnresolvedConflict(DataConflict conflict) {
        ConflictLog log = new ConflictLog();
        log.setConflictType(conflict.getType());
        log.setSourceValue(conflict.getSourceValue());
        log.setTargetValue(conflict.getTargetValue());
        log.setResolution(ConflictResolution.UNRESOLVED);
        log.setResolvedAt(LocalDateTime.now());
        
        conflictLogRepository.save(log);
    }
}
```

---

## 7. Practical Exercise: Data Management Implementation

### Order Processing with Event-Driven Architecture

```java
// Complete Order Processing Service
@Service
@Transactional
public class EventDrivenOrderService {
    
    @Autowired
    private OrderAggregateRepository orderAggregateRepository;
    
    @Autowired
    private EventStore eventStore;
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    @Autowired
    private ProductServiceClient productServiceClient;
    
    @Autowired
    private PaymentServiceClient paymentServiceClient;
    
    public OrderProcessingResult processOrder(OrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        
        try {
            // Create order aggregate
            OrderAggregate orderAggregate = OrderAggregate.create(orderId, request.getUserId(), request.getItems());
            
            // Process each item
            for (OrderItemRequest itemRequest : request.getItems()) {
                orderAggregate.addItem(
                    itemRequest.getProductId(),
                    itemRequest.getProductName(),
                    itemRequest.getQuantity(),
                    itemRequest.getUnitPrice()
                );
            }
            
            // Save aggregate (this will publish events)
            orderAggregateRepository.save(orderAggregate);
            
            // Return processing result
            return new OrderProcessingResult(orderId, OrderProcessingStatus.INITIATED);
            
        } catch (Exception e) {
            logger.error("Failed to process order: {}", e.getMessage());
            return new OrderProcessingResult(null, OrderProcessingStatus.FAILED);
        }
    }
    
    @EventListener
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        // Validate user asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                UserValidationResponse validation = userServiceClient.validateUser(event.getUserId());
                
                if (validation.isValid()) {
                    publishUserValidatedEvent(event.getOrderId(), event.getUserId());
                } else {
                    publishOrderFailedEvent(event.getOrderId(), "User validation failed");
                }
            } catch (Exception e) {
                publishOrderFailedEvent(event.getOrderId(), "User validation error: " + e.getMessage());
            }
        });
    }
    
    @EventListener
    @Transactional
    public void handleUserValidated(UserValidatedEvent event) {
        // Load order aggregate and update status
        Optional<OrderAggregate> orderOpt = orderAggregateRepository.findById(event.getOrderId());
        
        if (orderOpt.isPresent()) {
            OrderAggregate order = orderOpt.get();
            order.updateStatus(OrderStatus.USER_VALIDATED);
            orderAggregateRepository.save(order);
        }
    }
    
    @EventListener
    @Transactional
    public void handlePaymentConfirmed(PaymentConfirmedEvent event) {
        Optional<OrderAggregate> orderOpt = orderAggregateRepository.findById(event.getOrderId());
        
        if (orderOpt.isPresent()) {
            OrderAggregate order = orderOpt.get();
            order.confirmPayment(event.getPaymentId(), event.getAmount());
            orderAggregateRepository.save(order);
        }
    }
    
    private void publishUserValidatedEvent(String orderId, Long userId) {
        UserValidatedEvent event = new UserValidatedEvent();
        event.setOrderId(orderId);
        event.setUserId(userId);
        event.setValidatedAt(LocalDateTime.now());
        
        eventStore.saveAndPublish(event);
    }
    
    private void publishOrderFailedEvent(String orderId, String reason) {
        OrderFailedEvent event = new OrderFailedEvent();
        event.setOrderId(orderId);
        event.setFailureReason(reason);
        event.setFailedAt(LocalDateTime.now());
        
        eventStore.saveAndPublish(event);
    }
}
```

---

## 8. Key Takeaways

### Data Management Principles:
1. **Database per Service**: Each service owns its data completely
2. **Event-Driven Synchronization**: Use events to maintain consistency across services
3. **Eventual Consistency**: Accept that data will be eventually consistent
4. **Saga Pattern**: Handle distributed transactions with compensating actions
5. **CQRS**: Separate read and write models for better performance
6. **Event Sourcing**: Store all changes as events for complete audit trail

### Best Practices:
✅ **Design for Failure**: Always plan for compensation and rollback  
✅ **Monitor Data Consistency**: Implement consistency checks and alerts  
✅ **Use Idempotent Operations**: Ensure operations can be safely retried  
✅ **Implement Circuit Breakers**: Prevent cascading failures  
✅ **Cache Read Models**: Use caching for better query performance  
✅ **Version Your Events**: Support event schema evolution  

### Common Pitfalls to Avoid:
❌ **Distributed Transactions**: Avoid two-phase commit across services  
❌ **Shared Databases**: Don't share databases between services  
❌ **Synchronous Dependencies**: Minimize synchronous service calls  
❌ **Ignoring Compensations**: Always implement saga compensations  
❌ **Poor Event Design**: Design events for backward compatibility  

---

## 9. Next Steps

Tomorrow we'll complete Week 9 with a **Capstone Project** that combines all microservices concepts into a comprehensive e-commerce platform, including:
- Complete microservices architecture
- Event-driven data management
- Saga pattern implementation
- CQRS and Event Sourcing
- Monitoring and observability

**Practice Assignment**: Implement a simple saga pattern for a book ordering system with inventory management and payment processing.

---

*"In microservices, data consistency is not about preventing all conflicts, but about detecting and resolving them gracefully."*

*"Event-driven architecture is the backbone of resilient microservices - it enables loose coupling while maintaining business consistency."*