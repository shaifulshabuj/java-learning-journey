# Day 74: Event Sourcing and CQRS Implementation

## Overview

Today we'll explore Event Sourcing and Command Query Responsibility Segregation (CQRS) patterns. We'll learn how to build systems that capture all changes as events, separate read and write operations, and implement eventual consistency for scalable, auditable applications.

## Learning Objectives

- Understand Event Sourcing fundamentals and benefits
- Implement CQRS pattern for read/write separation
- Build event stores and event handlers
- Handle eventual consistency and projection building
- Implement sagas for complex business processes
- Design scalable event-driven architectures

## 1. Event Sourcing Fundamentals

### Core Concepts

**Event Sourcing**: Store all changes as a sequence of events
**Event Store**: Database that stores events in append-only manner
**Event**: Immutable record of something that happened
**Aggregate**: Domain object that produces events
**Projection**: Read model built from events
**Snapshot**: Point-in-time state to optimize reconstruction

### Benefits

- **Complete Audit Trail**: Every change is recorded
- **Temporal Queries**: Query state at any point in time
- **Debugging**: Replay events to understand system behavior
- **Scalability**: Separate read and write models
- **Flexibility**: Build new projections from existing events

## 2. Event Store Implementation

### Event Store Entity

```java
@Entity
@Table(name = "event_store")
public class EventStore {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;
    
    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;
    
    @Column(name = "event_type", nullable = false)
    private String eventType;
    
    @Column(name = "event_data", nullable = false, columnDefinition = "TEXT")
    private String eventData;
    
    @Column(name = "event_version", nullable = false)
    private Long eventVersion;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;
    
    @Column(name = "causation_id")
    private String causationId;
    
    @Column(name = "correlation_id")
    private String correlationId;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    // Constructors
    public EventStore() {}
    
    public EventStore(String aggregateId, String aggregateType, String eventType, 
                     String eventData, Long eventVersion) {
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.eventData = eventData;
        this.eventVersion = eventVersion;
        this.createdAt = LocalDateTime.now();
        this.eventId = UUID.randomUUID().toString();
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }
    
    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public String getEventData() { return eventData; }
    public void setEventData(String eventData) { this.eventData = eventData; }
    
    public Long getEventVersion() { return eventVersion; }
    public void setEventVersion(Long eventVersion) { this.eventVersion = eventVersion; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    
    public String getCausationId() { return causationId; }
    public void setCausationId(String causationId) { this.causationId = causationId; }
    
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
```

### Event Store Repository

```java
@Repository
public interface EventStoreRepository extends JpaRepository<EventStore, Long> {
    
    List<EventStore> findByAggregateIdAndAggregateTypeOrderByEventVersion(
        String aggregateId, String aggregateType);
    
    List<EventStore> findByAggregateIdAndAggregateTypeAndEventVersionGreaterThan(
        String aggregateId, String aggregateType, Long eventVersion);
    
    List<EventStore> findByAggregateTypeAndCreatedAtAfter(
        String aggregateType, LocalDateTime timestamp);
    
    Optional<Long> findMaxEventVersionByAggregateIdAndAggregateType(
        String aggregateId, String aggregateType);
    
    @Query("SELECT e FROM EventStore e WHERE e.aggregateType = :aggregateType AND e.eventVersion > :lastVersion ORDER BY e.eventVersion")
    List<EventStore> findEventsAfterVersion(@Param("aggregateType") String aggregateType, 
                                           @Param("lastVersion") Long lastVersion);
    
    @Query("SELECT e FROM EventStore e WHERE e.createdAt BETWEEN :startTime AND :endTime ORDER BY e.createdAt")
    List<EventStore> findEventsBetweenTimestamps(@Param("startTime") LocalDateTime startTime, 
                                                @Param("endTime") LocalDateTime endTime);
}
```

### Event Store Service

```java
@Service
@Transactional
public class EventStoreService {
    
    private final EventStoreRepository eventStoreRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    
    public EventStoreService(EventStoreRepository eventStoreRepository,
                            ObjectMapper objectMapper,
                            ApplicationEventPublisher eventPublisher) {
        this.eventStoreRepository = eventStoreRepository;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }
    
    public void saveEvent(String aggregateId, String aggregateType, Object event) {
        saveEvent(aggregateId, aggregateType, event, null, null);
    }
    
    public void saveEvent(String aggregateId, String aggregateType, Object event, 
                         String causationId, String correlationId) {
        try {
            String eventType = event.getClass().getSimpleName();
            String eventData = objectMapper.writeValueAsString(event);
            
            Long nextVersion = getNextVersion(aggregateId, aggregateType);
            
            EventStore eventStore = new EventStore(aggregateId, aggregateType, eventType, eventData, nextVersion);
            eventStore.setCausationId(causationId);
            eventStore.setCorrelationId(correlationId);
            
            // Add metadata
            EventMetadata metadata = new EventMetadata();
            metadata.setUserId(getCurrentUserId());
            metadata.setTimestamp(LocalDateTime.now());
            metadata.setSource("EventStoreService");
            eventStore.setMetadata(objectMapper.writeValueAsString(metadata));
            
            EventStore savedEvent = eventStoreRepository.save(eventStore);
            
            // Publish event for projections
            eventPublisher.publishEvent(new EventStoreEvent(savedEvent, event));
            
            System.out.println("Event saved: " + eventType + " for aggregate: " + aggregateId);
            
        } catch (Exception e) {
            throw new EventStoreException("Failed to save event", e);
        }
    }
    
    public List<EventStore> getEvents(String aggregateId, String aggregateType) {
        return eventStoreRepository.findByAggregateIdAndAggregateTypeOrderByEventVersion(
            aggregateId, aggregateType);
    }
    
    public List<EventStore> getEventsAfterVersion(String aggregateId, String aggregateType, Long version) {
        return eventStoreRepository.findByAggregateIdAndAggregateTypeAndEventVersionGreaterThan(
            aggregateId, aggregateType, version);
    }
    
    public <T> T reconstructAggregate(String aggregateId, String aggregateType, Class<T> aggregateClass) {
        try {
            List<EventStore> events = getEvents(aggregateId, aggregateType);
            
            if (events.isEmpty()) {
                return null;
            }
            
            T aggregate = aggregateClass.getDeclaredConstructor().newInstance();
            
            for (EventStore eventStore : events) {
                Object event = deserializeEvent(eventStore.getEventData(), eventStore.getEventType());
                applyEventToAggregate(aggregate, event);
            }
            
            return aggregate;
            
        } catch (Exception e) {
            throw new EventStoreException("Failed to reconstruct aggregate", e);
        }
    }
    
    public List<Object> getEventsForProjection(String aggregateType, Long lastProcessedVersion) {
        List<EventStore> eventStores = eventStoreRepository.findEventsAfterVersion(
            aggregateType, lastProcessedVersion != null ? lastProcessedVersion : 0L);
        
        return eventStores.stream()
            .map(eventStore -> {
                try {
                    return deserializeEvent(eventStore.getEventData(), eventStore.getEventType());
                } catch (Exception e) {
                    throw new EventStoreException("Failed to deserialize event", e);
                }
            })
            .collect(Collectors.toList());
    }
    
    private Long getNextVersion(String aggregateId, String aggregateType) {
        return eventStoreRepository.findMaxEventVersionByAggregateIdAndAggregateType(
            aggregateId, aggregateType)
            .map(version -> version + 1)
            .orElse(1L);
    }
    
    private Object deserializeEvent(String eventData, String eventType) throws Exception {
        Class<?> eventClass = Class.forName("com.example.events." + eventType);
        return objectMapper.readValue(eventData, eventClass);
    }
    
    private void applyEventToAggregate(Object aggregate, Object event) {
        try {
            Method applyMethod = aggregate.getClass().getMethod("apply", event.getClass());
            applyMethod.invoke(aggregate, event);
        } catch (Exception e) {
            throw new EventStoreException("Failed to apply event to aggregate", e);
        }
    }
    
    private String getCurrentUserId() {
        // Get current user from security context
        return "system";
    }
}
```

## 3. Domain Events and Aggregates

### Order Domain Events

```java
// Base Event
public abstract class DomainEvent {
    private final String eventId;
    private final LocalDateTime occurredAt;
    
    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = LocalDateTime.now();
    }
    
    public String getEventId() { return eventId; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}

// Order Events
public class OrderCreatedEvent extends DomainEvent {
    private String orderId;
    private String customerId;
    private String productId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    
    public OrderCreatedEvent() {}
    
    public OrderCreatedEvent(String orderId, String customerId, String productId, 
                           Integer quantity, BigDecimal totalAmount) {
        super();
        this.orderId = orderId;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

public class OrderStatusChangedEvent extends DomainEvent {
    private String orderId;
    private String oldStatus;
    private String newStatus;
    private String reason;
    private LocalDateTime changedAt;
    
    public OrderStatusChangedEvent() {}
    
    public OrderStatusChangedEvent(String orderId, String oldStatus, String newStatus, String reason) {
        super();
        this.orderId = orderId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.changedAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getOldStatus() { return oldStatus; }
    public void setOldStatus(String oldStatus) { this.oldStatus = oldStatus; }
    
    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}

public class OrderCancelledEvent extends DomainEvent {
    private String orderId;
    private String reason;
    private LocalDateTime cancelledAt;
    
    public OrderCancelledEvent() {}
    
    public OrderCancelledEvent(String orderId, String reason) {
        super();
        this.orderId = orderId;
        this.reason = reason;
        this.cancelledAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
}
```

### Order Aggregate

```java
public class OrderAggregate {
    
    private String orderId;
    private String customerId;
    private String productId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
    
    private List<DomainEvent> uncommittedEvents = new ArrayList<>();
    
    public OrderAggregate() {}
    
    public OrderAggregate(String orderId, String customerId, String productId, 
                         Integer quantity, BigDecimal totalAmount) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.status = OrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.version = 0L;
        
        // Raise domain event
        OrderCreatedEvent event = new OrderCreatedEvent(orderId, customerId, productId, quantity, totalAmount);
        raiseEvent(event);
    }
    
    public void changeStatus(OrderStatus newStatus, String reason) {
        if (this.status == newStatus) {
            return;
        }
        
        OrderStatus oldStatus = this.status;
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
        
        // Raise domain event
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
            orderId, oldStatus.name(), newStatus.name(), reason);
        raiseEvent(event);
    }
    
    public void cancel(String reason) {
        if (this.status == OrderStatus.CANCELLED) {
            return;
        }
        
        if (this.status == OrderStatus.SHIPPED || this.status == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel order that is already shipped or delivered");
        }
        
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
        
        // Raise domain event
        OrderCancelledEvent event = new OrderCancelledEvent(orderId, reason);
        raiseEvent(event);
    }
    
    // Event application methods for reconstruction
    public void apply(OrderCreatedEvent event) {
        this.orderId = event.getOrderId();
        this.customerId = event.getCustomerId();
        this.productId = event.getProductId();
        this.quantity = event.getQuantity();
        this.totalAmount = event.getTotalAmount();
        this.status = OrderStatus.PENDING;
        this.createdAt = event.getCreatedAt();
        this.version = (this.version != null) ? this.version + 1 : 1L;
    }
    
    public void apply(OrderStatusChangedEvent event) {
        this.status = OrderStatus.valueOf(event.getNewStatus());
        this.updatedAt = event.getChangedAt();
        this.version = (this.version != null) ? this.version + 1 : 1L;
    }
    
    public void apply(OrderCancelledEvent event) {
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = event.getCancelledAt();
        this.version = (this.version != null) ? this.version + 1 : 1L;
    }
    
    private void raiseEvent(DomainEvent event) {
        uncommittedEvents.add(event);
    }
    
    public List<DomainEvent> getUncommittedEvents() {
        return new ArrayList<>(uncommittedEvents);
    }
    
    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }
    
    // Getters and setters
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getProductId() { return productId; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}

enum OrderStatus {
    PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
}
```

## 4. CQRS Implementation

### Command Side

```java
// Commands
public abstract class Command {
    private final String commandId;
    private final LocalDateTime issuedAt;
    
    protected Command() {
        this.commandId = UUID.randomUUID().toString();
        this.issuedAt = LocalDateTime.now();
    }
    
    public String getCommandId() { return commandId; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
}

public class CreateOrderCommand extends Command {
    private String orderId;
    private String customerId;
    private String productId;
    private Integer quantity;
    private BigDecimal totalAmount;
    
    public CreateOrderCommand() {}
    
    public CreateOrderCommand(String orderId, String customerId, String productId, 
                            Integer quantity, BigDecimal totalAmount) {
        super();
        this.orderId = orderId;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
    }
    
    // Getters and setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}

public class ChangeOrderStatusCommand extends Command {
    private String orderId;
    private OrderStatus newStatus;
    private String reason;
    
    public ChangeOrderStatusCommand() {}
    
    public ChangeOrderStatusCommand(String orderId, OrderStatus newStatus, String reason) {
        super();
        this.orderId = orderId;
        this.newStatus = newStatus;
        this.reason = reason;
    }
    
    // Getters and setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public OrderStatus getNewStatus() { return newStatus; }
    public void setNewStatus(OrderStatus newStatus) { this.newStatus = newStatus; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
```

### Command Handlers

```java
@Component
public class OrderCommandHandler {
    
    private final EventStoreService eventStoreService;
    private final OrderAggregateRepository orderAggregateRepository;
    
    public OrderCommandHandler(EventStoreService eventStoreService,
                              OrderAggregateRepository orderAggregateRepository) {
        this.eventStoreService = eventStoreService;
        this.orderAggregateRepository = orderAggregateRepository;
    }
    
    @CommandHandler
    public void handle(CreateOrderCommand command) {
        try {
            // Check if order already exists
            OrderAggregate existingOrder = orderAggregateRepository.findById(command.getOrderId());
            if (existingOrder != null) {
                throw new OrderAlreadyExistsException("Order already exists: " + command.getOrderId());
            }
            
            // Create new order aggregate
            OrderAggregate orderAggregate = new OrderAggregate(
                command.getOrderId(),
                command.getCustomerId(),
                command.getProductId(),
                command.getQuantity(),
                command.getTotalAmount()
            );
            
            // Save events
            for (DomainEvent event : orderAggregate.getUncommittedEvents()) {
                eventStoreService.saveEvent(
                    command.getOrderId(),
                    "Order",
                    event,
                    command.getCommandId(),
                    null
                );
            }
            
            orderAggregate.markEventsAsCommitted();
            orderAggregateRepository.save(orderAggregate);
            
            System.out.println("Order created successfully: " + command.getOrderId());
            
        } catch (Exception e) {
            throw new CommandHandlingException("Failed to handle CreateOrderCommand", e);
        }
    }
    
    @CommandHandler
    public void handle(ChangeOrderStatusCommand command) {
        try {
            // Load order aggregate
            OrderAggregate orderAggregate = orderAggregateRepository.findById(command.getOrderId());
            if (orderAggregate == null) {
                throw new OrderNotFoundException("Order not found: " + command.getOrderId());
            }
            
            // Execute command
            orderAggregate.changeStatus(command.getNewStatus(), command.getReason());
            
            // Save events
            for (DomainEvent event : orderAggregate.getUncommittedEvents()) {
                eventStoreService.saveEvent(
                    command.getOrderId(),
                    "Order",
                    event,
                    command.getCommandId(),
                    null
                );
            }
            
            orderAggregate.markEventsAsCommitted();
            orderAggregateRepository.save(orderAggregate);
            
            System.out.println("Order status changed successfully: " + command.getOrderId());
            
        } catch (Exception e) {
            throw new CommandHandlingException("Failed to handle ChangeOrderStatusCommand", e);
        }
    }
}
```

### Query Side - Read Models

```java
// Order Read Model
@Entity
@Table(name = "order_read_model")
public class OrderReadModel {
    
    @Id
    private String orderId;
    
    @Column(name = "customer_id")
    private String customerId;
    
    @Column(name = "product_id")
    private String productId;
    
    @Column(name = "quantity")
    private Integer quantity;
    
    @Column(name = "total_amount")
    private BigDecimal totalAmount;
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "version")
    private Long version;
    
    // Constructors, getters, and setters
    public OrderReadModel() {}
    
    public OrderReadModel(String orderId, String customerId, String productId, 
                         Integer quantity, BigDecimal totalAmount, OrderStatus status) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        this.version = 1L;
    }
    
    // Getters and setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    
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
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}

// Customer Summary Read Model
@Entity
@Table(name = "customer_summary_read_model")
public class CustomerSummaryReadModel {
    
    @Id
    private String customerId;
    
    @Column(name = "total_orders")
    private Integer totalOrders;
    
    @Column(name = "total_amount")
    private BigDecimal totalAmount;
    
    @Column(name = "last_order_date")
    private LocalDateTime lastOrderDate;
    
    @Column(name = "average_order_value")
    private BigDecimal averageOrderValue;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors, getters, and setters
    public CustomerSummaryReadModel() {}
    
    public CustomerSummaryReadModel(String customerId) {
        this.customerId = customerId;
        this.totalOrders = 0;
        this.totalAmount = BigDecimal.ZERO;
        this.averageOrderValue = BigDecimal.ZERO;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void addOrder(BigDecimal orderAmount, LocalDateTime orderDate) {
        this.totalOrders++;
        this.totalAmount = this.totalAmount.add(orderAmount);
        this.lastOrderDate = orderDate;
        this.averageOrderValue = this.totalAmount.divide(
            BigDecimal.valueOf(this.totalOrders), 2, RoundingMode.HALF_UP);
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public Integer getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Integer totalOrders) { this.totalOrders = totalOrders; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public LocalDateTime getLastOrderDate() { return lastOrderDate; }
    public void setLastOrderDate(LocalDateTime lastOrderDate) { this.lastOrderDate = lastOrderDate; }
    
    public BigDecimal getAverageOrderValue() { return averageOrderValue; }
    public void setAverageOrderValue(BigDecimal averageOrderValue) { this.averageOrderValue = averageOrderValue; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

### Projection Handlers

```java
@Component
public class OrderProjectionHandler {
    
    private final OrderReadModelRepository orderReadModelRepository;
    private final CustomerSummaryReadModelRepository customerSummaryRepository;
    
    public OrderProjectionHandler(OrderReadModelRepository orderReadModelRepository,
                                 CustomerSummaryReadModelRepository customerSummaryRepository) {
        this.orderReadModelRepository = orderReadModelRepository;
        this.customerSummaryRepository = customerSummaryRepository;
    }
    
    @EventHandler
    public void handle(OrderCreatedEvent event) {
        try {
            // Update order read model
            OrderReadModel orderReadModel = new OrderReadModel(
                event.getOrderId(),
                event.getCustomerId(),
                event.getProductId(),
                event.getQuantity(),
                event.getTotalAmount(),
                OrderStatus.PENDING
            );
            
            orderReadModelRepository.save(orderReadModel);
            
            // Update customer summary
            CustomerSummaryReadModel customerSummary = customerSummaryRepository
                .findById(event.getCustomerId())
                .orElse(new CustomerSummaryReadModel(event.getCustomerId()));
            
            customerSummary.addOrder(event.getTotalAmount(), event.getCreatedAt());
            customerSummaryRepository.save(customerSummary);
            
            System.out.println("Order created projection updated: " + event.getOrderId());
            
        } catch (Exception e) {
            throw new ProjectionHandlingException("Failed to handle OrderCreatedEvent", e);
        }
    }
    
    @EventHandler
    public void handle(OrderStatusChangedEvent event) {
        try {
            OrderReadModel orderReadModel = orderReadModelRepository.findById(event.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException("Order read model not found: " + event.getOrderId()));
            
            orderReadModel.setStatus(OrderStatus.valueOf(event.getNewStatus()));
            orderReadModel.setUpdatedAt(event.getChangedAt());
            orderReadModel.setVersion(orderReadModel.getVersion() + 1);
            
            orderReadModelRepository.save(orderReadModel);
            
            System.out.println("Order status changed projection updated: " + event.getOrderId());
            
        } catch (Exception e) {
            throw new ProjectionHandlingException("Failed to handle OrderStatusChangedEvent", e);
        }
    }
    
    @EventHandler
    public void handle(OrderCancelledEvent event) {
        try {
            OrderReadModel orderReadModel = orderReadModelRepository.findById(event.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException("Order read model not found: " + event.getOrderId()));
            
            orderReadModel.setStatus(OrderStatus.CANCELLED);
            orderReadModel.setUpdatedAt(event.getCancelledAt());
            orderReadModel.setVersion(orderReadModel.getVersion() + 1);
            
            orderReadModelRepository.save(orderReadModel);
            
            System.out.println("Order cancelled projection updated: " + event.getOrderId());
            
        } catch (Exception e) {
            throw new ProjectionHandlingException("Failed to handle OrderCancelledEvent", e);
        }
    }
}
```

## 5. Query Handlers

### Query Implementation

```java
// Queries
public abstract class Query {
    private final String queryId;
    private final LocalDateTime issuedAt;
    
    protected Query() {
        this.queryId = UUID.randomUUID().toString();
        this.issuedAt = LocalDateTime.now();
    }
    
    public String getQueryId() { return queryId; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
}

public class GetOrderByIdQuery extends Query {
    private String orderId;
    
    public GetOrderByIdQuery() {}
    
    public GetOrderByIdQuery(String orderId) {
        super();
        this.orderId = orderId;
    }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
}

public class GetOrdersByCustomerQuery extends Query {
    private String customerId;
    private int page;
    private int size;
    
    public GetOrdersByCustomerQuery() {}
    
    public GetOrdersByCustomerQuery(String customerId, int page, int size) {
        super();
        this.customerId = customerId;
        this.page = page;
        this.size = size;
    }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}

public class GetCustomerSummaryQuery extends Query {
    private String customerId;
    
    public GetCustomerSummaryQuery() {}
    
    public GetCustomerSummaryQuery(String customerId) {
        super();
        this.customerId = customerId;
    }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
}
```

### Query Handlers

```java
@Component
public class OrderQueryHandler {
    
    private final OrderReadModelRepository orderReadModelRepository;
    private final CustomerSummaryReadModelRepository customerSummaryRepository;
    
    public OrderQueryHandler(OrderReadModelRepository orderReadModelRepository,
                            CustomerSummaryReadModelRepository customerSummaryRepository) {
        this.orderReadModelRepository = orderReadModelRepository;
        this.customerSummaryRepository = customerSummaryRepository;
    }
    
    @QueryHandler
    public OrderReadModel handle(GetOrderByIdQuery query) {
        return orderReadModelRepository.findById(query.getOrderId())
            .orElseThrow(() -> new OrderNotFoundException("Order not found: " + query.getOrderId()));
    }
    
    @QueryHandler
    public Page<OrderReadModel> handle(GetOrdersByCustomerQuery query) {
        Pageable pageable = PageRequest.of(query.getPage(), query.getSize());
        return orderReadModelRepository.findByCustomerIdOrderByCreatedAtDesc(
            query.getCustomerId(), pageable);
    }
    
    @QueryHandler
    public CustomerSummaryReadModel handle(GetCustomerSummaryQuery query) {
        return customerSummaryRepository.findById(query.getCustomerId())
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + query.getCustomerId()));
    }
    
    @QueryHandler
    public List<OrderReadModel> handle(GetOrdersByStatusQuery query) {
        return orderReadModelRepository.findByStatusOrderByCreatedAtDesc(query.getStatus());
    }
    
    @QueryHandler
    public List<OrderReadModel> handle(GetOrdersByDateRangeQuery query) {
        return orderReadModelRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
            query.getStartDate(), query.getEndDate());
    }
}
```

## 6. Saga Pattern Implementation

### Order Processing Saga

```java
@Component
public class OrderProcessingSaga {
    
    private final CommandGateway commandGateway;
    private final SagaRepository sagaRepository;
    
    public OrderProcessingSaga(CommandGateway commandGateway, SagaRepository sagaRepository) {
        this.commandGateway = commandGateway;
        this.sagaRepository = sagaRepository;
    }
    
    @SagaOrchestrationStart
    public void handle(OrderCreatedEvent event) {
        try {
            OrderProcessingSagaState sagaState = new OrderProcessingSagaState();
            sagaState.setOrderId(event.getOrderId());
            sagaState.setCustomerId(event.getCustomerId());
            sagaState.setProductId(event.getProductId());
            sagaState.setQuantity(event.getQuantity());
            sagaState.setTotalAmount(event.getTotalAmount());
            sagaState.setStatus(SagaStatus.STARTED);
            
            sagaRepository.save(sagaState);
            
            // Step 1: Reserve inventory
            ReserveInventoryCommand reserveCommand = new ReserveInventoryCommand(
                event.getOrderId(), event.getProductId(), event.getQuantity());
            commandGateway.send(reserveCommand);
            
            System.out.println("Order processing saga started: " + event.getOrderId());
            
        } catch (Exception e) {
            throw new SagaException("Failed to start order processing saga", e);
        }
    }
    
    @SagaOrchestrationStep
    public void handle(InventoryReservedEvent event) {
        try {
            OrderProcessingSagaState sagaState = sagaRepository.findByOrderId(event.getOrderId())
                .orElseThrow(() -> new SagaNotFoundException("Saga not found for order: " + event.getOrderId()));
            
            sagaState.setInventoryReserved(true);
            sagaState.setReservationId(event.getReservationId());
            
            // Step 2: Process payment
            ProcessPaymentCommand paymentCommand = new ProcessPaymentCommand(
                event.getOrderId(), sagaState.getCustomerId(), sagaState.getTotalAmount());
            commandGateway.send(paymentCommand);
            
            sagaRepository.save(sagaState);
            
            System.out.println("Inventory reserved for order: " + event.getOrderId());
            
        } catch (Exception e) {
            throw new SagaException("Failed to handle inventory reserved event", e);
        }
    }
    
    @SagaOrchestrationStep
    public void handle(PaymentProcessedEvent event) {
        try {
            OrderProcessingSagaState sagaState = sagaRepository.findByOrderId(event.getOrderId())
                .orElseThrow(() -> new SagaNotFoundException("Saga not found for order: " + event.getOrderId()));
            
            sagaState.setPaymentProcessed(true);
            sagaState.setPaymentId(event.getPaymentId());
            
            // Step 3: Confirm order
            ChangeOrderStatusCommand confirmCommand = new ChangeOrderStatusCommand(
                event.getOrderId(), OrderStatus.CONFIRMED, "Payment processed successfully");
            commandGateway.send(confirmCommand);
            
            sagaState.setStatus(SagaStatus.COMPLETED);
            sagaRepository.save(sagaState);
            
            System.out.println("Order processing saga completed: " + event.getOrderId());
            
        } catch (Exception e) {
            throw new SagaException("Failed to handle payment processed event", e);
        }
    }
    
    @SagaOrchestrationStep
    public void handle(PaymentFailedEvent event) {
        try {
            OrderProcessingSagaState sagaState = sagaRepository.findByOrderId(event.getOrderId())
                .orElseThrow(() -> new SagaNotFoundException("Saga not found for order: " + event.getOrderId()));
            
            // Compensate: Release inventory
            if (sagaState.isInventoryReserved()) {
                ReleaseInventoryCommand releaseCommand = new ReleaseInventoryCommand(
                    event.getOrderId(), sagaState.getReservationId());
                commandGateway.send(releaseCommand);
            }
            
            // Cancel order
            ChangeOrderStatusCommand cancelCommand = new ChangeOrderStatusCommand(
                event.getOrderId(), OrderStatus.CANCELLED, "Payment failed: " + event.getReason());
            commandGateway.send(cancelCommand);
            
            sagaState.setStatus(SagaStatus.COMPENSATED);
            sagaRepository.save(sagaState);
            
            System.out.println("Order processing saga compensated: " + event.getOrderId());
            
        } catch (Exception e) {
            throw new SagaException("Failed to handle payment failed event", e);
        }
    }
    
    @SagaOrchestrationStep
    public void handle(InventoryReservationFailedEvent event) {
        try {
            OrderProcessingSagaState sagaState = sagaRepository.findByOrderId(event.getOrderId())
                .orElseThrow(() -> new SagaNotFoundException("Saga not found for order: " + event.getOrderId()));
            
            // Cancel order
            ChangeOrderStatusCommand cancelCommand = new ChangeOrderStatusCommand(
                event.getOrderId(), OrderStatus.CANCELLED, "Inventory reservation failed: " + event.getReason());
            commandGateway.send(cancelCommand);
            
            sagaState.setStatus(SagaStatus.FAILED);
            sagaRepository.save(sagaState);
            
            System.out.println("Order processing saga failed: " + event.getOrderId());
            
        } catch (Exception e) {
            throw new SagaException("Failed to handle inventory reservation failed event", e);
        }
    }
}
```

### Saga State Management

```java
@Entity
@Table(name = "saga_state")
public class OrderProcessingSagaState {
    
    @Id
    private String sagaId;
    
    @Column(name = "order_id")
    private String orderId;
    
    @Column(name = "customer_id")
    private String customerId;
    
    @Column(name = "product_id")
    private String productId;
    
    @Column(name = "quantity")
    private Integer quantity;
    
    @Column(name = "total_amount")
    private BigDecimal totalAmount;
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private SagaStatus status;
    
    @Column(name = "inventory_reserved")
    private boolean inventoryReserved;
    
    @Column(name = "reservation_id")
    private String reservationId;
    
    @Column(name = "payment_processed")
    private boolean paymentProcessed;
    
    @Column(name = "payment_id")
    private String paymentId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public OrderProcessingSagaState() {
        this.sagaId = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public String getSagaId() { return sagaId; }
    public void setSagaId(String sagaId) { this.sagaId = sagaId; }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public SagaStatus getStatus() { return status; }
    public void setStatus(SagaStatus status) { this.status = status; }
    
    public boolean isInventoryReserved() { return inventoryReserved; }
    public void setInventoryReserved(boolean inventoryReserved) { this.inventoryReserved = inventoryReserved; }
    
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    
    public boolean isPaymentProcessed() { return paymentProcessed; }
    public void setPaymentProcessed(boolean paymentProcessed) { this.paymentProcessed = paymentProcessed; }
    
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

enum SagaStatus {
    STARTED, COMPLETED, COMPENSATED, FAILED
}
```

## 7. Event Replay and Snapshots

### Snapshot Implementation

```java
@Entity
@Table(name = "aggregate_snapshot")
public class AggregateSnapshot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;
    
    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;
    
    @Column(name = "snapshot_data", nullable = false, columnDefinition = "TEXT")
    private String snapshotData;
    
    @Column(name = "version", nullable = false)
    private Long version;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    public AggregateSnapshot() {}
    
    public AggregateSnapshot(String aggregateId, String aggregateType, String snapshotData, Long version) {
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.snapshotData = snapshotData;
        this.version = version;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }
    
    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }
    
    public String getSnapshotData() { return snapshotData; }
    public void setSnapshotData(String snapshotData) { this.snapshotData = snapshotData; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

### Snapshot Service

```java
@Service
@Transactional
public class SnapshotService {
    
    private final AggregateSnapshotRepository snapshotRepository;
    private final EventStoreService eventStoreService;
    private final ObjectMapper objectMapper;
    
    public SnapshotService(AggregateSnapshotRepository snapshotRepository,
                          EventStoreService eventStoreService,
                          ObjectMapper objectMapper) {
        this.snapshotRepository = snapshotRepository;
        this.eventStoreService = eventStoreService;
        this.objectMapper = objectMapper;
    }
    
    public void createSnapshot(String aggregateId, String aggregateType, Object aggregate, Long version) {
        try {
            String snapshotData = objectMapper.writeValueAsString(aggregate);
            
            AggregateSnapshot snapshot = new AggregateSnapshot(
                aggregateId, aggregateType, snapshotData, version);
            
            snapshotRepository.save(snapshot);
            
            // Clean up old snapshots (keep only the latest 3)
            cleanupOldSnapshots(aggregateId, aggregateType);
            
            System.out.println("Snapshot created for aggregate: " + aggregateId + " at version: " + version);
            
        } catch (Exception e) {
            throw new SnapshotException("Failed to create snapshot", e);
        }
    }
    
    public <T> T loadFromSnapshot(String aggregateId, String aggregateType, Class<T> aggregateClass) {
        try {
            Optional<AggregateSnapshot> latestSnapshot = snapshotRepository
                .findLatestByAggregateIdAndAggregateType(aggregateId, aggregateType);
            
            if (latestSnapshot.isEmpty()) {
                return null;
            }
            
            AggregateSnapshot snapshot = latestSnapshot.get();
            T aggregate = objectMapper.readValue(snapshot.getSnapshotData(), aggregateClass);
            
            // Apply events after snapshot
            List<EventStore> eventsAfterSnapshot = eventStoreService.getEventsAfterVersion(
                aggregateId, aggregateType, snapshot.getVersion());
            
            for (EventStore eventStore : eventsAfterSnapshot) {
                Object event = deserializeEvent(eventStore.getEventData(), eventStore.getEventType());
                applyEventToAggregate(aggregate, event);
            }
            
            return aggregate;
            
        } catch (Exception e) {
            throw new SnapshotException("Failed to load from snapshot", e);
        }
    }
    
    @Scheduled(fixedDelay = 3600000) // Run every hour
    public void createSnapshotsForActiveAggregates() {
        // Find aggregates that need snapshots (e.g., version > last snapshot version + 10)
        List<String> aggregatesNeedingSnapshots = findAggregatesNeedingSnapshots();
        
        for (String aggregateId : aggregatesNeedingSnapshots) {
            try {
                OrderAggregate aggregate = eventStoreService.reconstructAggregate(
                    aggregateId, "Order", OrderAggregate.class);
                
                if (aggregate != null) {
                    createSnapshot(aggregateId, "Order", aggregate, aggregate.getVersion());
                }
            } catch (Exception e) {
                System.err.println("Failed to create snapshot for aggregate: " + aggregateId);
            }
        }
    }
    
    private void cleanupOldSnapshots(String aggregateId, String aggregateType) {
        List<AggregateSnapshot> oldSnapshots = snapshotRepository
            .findOldSnapshots(aggregateId, aggregateType, 3);
        
        if (!oldSnapshots.isEmpty()) {
            snapshotRepository.deleteAll(oldSnapshots);
        }
    }
    
    private List<String> findAggregatesNeedingSnapshots() {
        // Implementation to find aggregates that need snapshots
        return new ArrayList<>();
    }
    
    private Object deserializeEvent(String eventData, String eventType) throws Exception {
        Class<?> eventClass = Class.forName("com.example.events." + eventType);
        return objectMapper.readValue(eventData, eventClass);
    }
    
    private void applyEventToAggregate(Object aggregate, Object event) {
        try {
            Method applyMethod = aggregate.getClass().getMethod("apply", event.getClass());
            applyMethod.invoke(aggregate, event);
        } catch (Exception e) {
            throw new SnapshotException("Failed to apply event to aggregate", e);
        }
    }
}
```

## 8. Testing Event Sourcing and CQRS

### Integration Testing

```java
@SpringBootTest
@Transactional
class EventSourcingIntegrationTest {
    
    @Autowired
    private EventStoreService eventStoreService;
    
    @Autowired
    private OrderCommandHandler commandHandler;
    
    @Autowired
    private OrderQueryHandler queryHandler;
    
    @Autowired
    private OrderProjectionHandler projectionHandler;
    
    @Test
    void testCompleteEventSourcingFlow() {
        // Given
        String orderId = UUID.randomUUID().toString();
        CreateOrderCommand createCommand = new CreateOrderCommand(
            orderId, "customer-123", "product-456", 2, BigDecimal.valueOf(99.99));
        
        // When - Execute command
        commandHandler.handle(createCommand);
        
        // Then - Verify events were stored
        List<EventStore> events = eventStoreService.getEvents(orderId, "Order");
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType()).isEqualTo("OrderCreatedEvent");
        
        // And - Verify projection was updated
        GetOrderByIdQuery query = new GetOrderByIdQuery(orderId);
        OrderReadModel readModel = queryHandler.handle(query);
        assertThat(readModel.getOrderId()).isEqualTo(orderId);
        assertThat(readModel.getStatus()).isEqualTo(OrderStatus.PENDING);
    }
    
    @Test
    void testEventReplay() {
        // Given
        String orderId = UUID.randomUUID().toString();
        
        // Create order
        CreateOrderCommand createCommand = new CreateOrderCommand(
            orderId, "customer-123", "product-456", 2, BigDecimal.valueOf(99.99));
        commandHandler.handle(createCommand);
        
        // Change status
        ChangeOrderStatusCommand statusCommand = new ChangeOrderStatusCommand(
            orderId, OrderStatus.CONFIRMED, "Payment confirmed");
        commandHandler.handle(statusCommand);
        
        // When - Reconstruct aggregate from events
        OrderAggregate aggregate = eventStoreService.reconstructAggregate(
            orderId, "Order", OrderAggregate.class);
        
        // Then
        assertThat(aggregate.getOrderId()).isEqualTo(orderId);
        assertThat(aggregate.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(aggregate.getVersion()).isEqualTo(2L);
    }
}
```

### Unit Testing

```java
@ExtendWith(MockitoExtension.class)
class OrderAggregateTest {
    
    @Test
    void testOrderCreation() {
        // Given
        String orderId = "order-123";
        String customerId = "customer-456";
        String productId = "product-789";
        Integer quantity = 2;
        BigDecimal totalAmount = BigDecimal.valueOf(99.99);
        
        // When
        OrderAggregate aggregate = new OrderAggregate(
            orderId, customerId, productId, quantity, totalAmount);
        
        // Then
        assertThat(aggregate.getOrderId()).isEqualTo(orderId);
        assertThat(aggregate.getCustomerId()).isEqualTo(customerId);
        assertThat(aggregate.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(aggregate.getUncommittedEvents()).hasSize(1);
        
        DomainEvent event = aggregate.getUncommittedEvents().get(0);
        assertThat(event).isInstanceOf(OrderCreatedEvent.class);
    }
    
    @Test
    void testOrderStatusChange() {
        // Given
        OrderAggregate aggregate = new OrderAggregate(
            "order-123", "customer-456", "product-789", 2, BigDecimal.valueOf(99.99));
        aggregate.markEventsAsCommitted();
        
        // When
        aggregate.changeStatus(OrderStatus.CONFIRMED, "Payment confirmed");
        
        // Then
        assertThat(aggregate.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(aggregate.getUncommittedEvents()).hasSize(1);
        
        DomainEvent event = aggregate.getUncommittedEvents().get(0);
        assertThat(event).isInstanceOf(OrderStatusChangedEvent.class);
    }
}
```

## Summary

Today we covered:

1. **Event Sourcing Fundamentals**: Core concepts and benefits of event sourcing
2. **Event Store Implementation**: Persistent storage for events with metadata
3. **Domain Events and Aggregates**: Domain-driven design with event sourcing
4. **CQRS Implementation**: Separate command and query models
5. **Query Handlers**: Read model projections and query processing
6. **Saga Pattern**: Distributed transaction management with compensating actions
7. **Event Replay and Snapshots**: Performance optimization and state reconstruction
8. **Testing**: Comprehensive testing strategies for event-sourced systems

## Key Takeaways

- Event sourcing provides complete audit trails and temporal queries
- CQRS enables scalable separation of read and write operations
- Eventual consistency requires careful handling of projection updates
- Sagas provide distributed transaction management with compensating actions
- Snapshots optimize performance for aggregates with many events
- Testing event-sourced systems requires verifying events, projections, and aggregate state

## Next Steps

Tomorrow we'll explore Message Brokers (RabbitMQ and ActiveMQ) in detail, learning about advanced messaging patterns, clustering, and production deployment strategies.