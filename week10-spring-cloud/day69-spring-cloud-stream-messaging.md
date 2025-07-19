# Day 69: Spring Cloud Stream & Messaging

## Overview
Today we'll explore Spring Cloud Stream for building event-driven microservices. We'll learn about message-driven architectures, event sourcing, and implementing resilient messaging patterns with various message brokers.

## Learning Objectives
- Understand Spring Cloud Stream fundamentals and concepts
- Configure message brokers (RabbitMQ, Apache Kafka)
- Implement event-driven communication patterns
- Handle message processing, error handling, and dead letter queues
- Build event sourcing and CQRS patterns
- Implement stream processing and reactive messaging

## 1. Spring Cloud Stream Fundamentals

### Core Concepts

**Binder**: Middleware-specific code that connects to message brokers
**Binding**: Bridge between application and message broker
**Channel**: Abstraction for message communication
**Producer**: Sends messages to channels
**Consumer**: Receives messages from channels

### Dependencies

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-stream-rabbit</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-stream-binder-kafka</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

## 2. Basic Configuration

### Application Configuration

```yaml
# application.yml
spring:
  application:
    name: order-event-service
  cloud:
    stream:
      bindings:
        orderCreated-out-0:
          destination: order-events
          content-type: application/json
          group: order-service
        orderCreated-in-0:
          destination: order-events
          content-type: application/json
          group: inventory-service
        inventoryUpdated-out-0:
          destination: inventory-events
          content-type: application/json
          group: inventory-service
        inventoryUpdated-in-0:
          destination: inventory-events
          content-type: application/json
          group: notification-service
      
      rabbit:
        bindings:
          orderCreated-out-0:
            producer:
              routing-key-expression: headers.routingKey
              declare-exchange: true
              exchange-type: topic
          orderCreated-in-0:
            consumer:
              binding-routing-key: order.created.*
              declare-exchange: true
              exchange-type: topic
              auto-bind-dlq: true
              republish-to-dlq: true
      
      kafka:
        bindings:
          orderCreated-out-0:
            producer:
              configuration:
                key.serializer: org.apache.kafka.common.serialization.StringSerializer
                value.serializer: org.springframework.kafka.support.serializer.JsonSerializer
          orderCreated-in-0:
            consumer:
              configuration:
                key.deserializer: org.apache.kafka.common.serialization.StringDeserializer
                value.deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
                spring.json.trusted.packages: "*"
        
        binder:
          brokers: localhost:9092
          auto-create-topics: true
          
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

## 3. Event Models

### Order Events

```java
public class OrderCreatedEvent {
    private String orderId;
    private Long customerId;
    private Long productId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime createdAt;
    private String reservationId;
    private String paymentId;
    
    // Constructors
    public OrderCreatedEvent() {}
    
    public OrderCreatedEvent(String orderId, Long customerId, Long productId, 
                           Integer quantity, BigDecimal totalAmount, String status,
                           LocalDateTime createdAt, String reservationId, String paymentId) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.reservationId = reservationId;
        this.paymentId = paymentId;
    }
    
    // Getters and setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
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
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
}

public class OrderStatusUpdatedEvent {
    private String orderId;
    private String oldStatus;
    private String newStatus;
    private LocalDateTime updatedAt;
    private String reason;
    
    // Constructors, getters, and setters
    public OrderStatusUpdatedEvent() {}
    
    public OrderStatusUpdatedEvent(String orderId, String oldStatus, String newStatus,
                                  LocalDateTime updatedAt, String reason) {
        this.orderId = orderId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.updatedAt = updatedAt;
        this.reason = reason;
    }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getOldStatus() { return oldStatus; }
    public void setOldStatus(String oldStatus) { this.oldStatus = oldStatus; }
    
    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
```

### Inventory Events

```java
public class InventoryReservedEvent {
    private String reservationId;
    private Long productId;
    private Integer quantity;
    private String orderId;
    private LocalDateTime reservedAt;
    
    // Constructors, getters, and setters
    public InventoryReservedEvent() {}
    
    public InventoryReservedEvent(String reservationId, Long productId, Integer quantity,
                                 String orderId, LocalDateTime reservedAt) {
        this.reservationId = reservationId;
        this.productId = productId;
        this.quantity = quantity;
        this.orderId = orderId;
        this.reservedAt = reservedAt;
    }
    
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public LocalDateTime getReservedAt() { return reservedAt; }
    public void setReservedAt(LocalDateTime reservedAt) { this.reservedAt = reservedAt; }
}

public class InventoryUpdatedEvent {
    private Long productId;
    private Integer previousQuantity;
    private Integer newQuantity;
    private String operation; // RESERVED, RELEASED, RESTOCKED
    private LocalDateTime updatedAt;
    
    // Constructors, getters, and setters
    public InventoryUpdatedEvent() {}
    
    public InventoryUpdatedEvent(Long productId, Integer previousQuantity, Integer newQuantity,
                                String operation, LocalDateTime updatedAt) {
        this.productId = productId;
        this.previousQuantity = previousQuantity;
        this.newQuantity = newQuantity;
        this.operation = operation;
        this.updatedAt = updatedAt;
    }
    
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    
    public Integer getPreviousQuantity() { return previousQuantity; }
    public void setPreviousQuantity(Integer previousQuantity) { this.previousQuantity = previousQuantity; }
    
    public Integer getNewQuantity() { return newQuantity; }
    public void setNewQuantity(Integer newQuantity) { this.newQuantity = newQuantity; }
    
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

## 4. Event Publishers

### Order Event Publisher

```java
@Component
public class OrderEventPublisher {
    
    private final StreamBridge streamBridge;
    
    public OrderEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }
    
    public void publishOrderCreated(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
            order.getId().toString(),
            order.getCustomerId(),
            order.getProductId(),
            order.getQuantity(),
            order.getTotalAmount(),
            order.getStatus(),
            order.getCreatedAt(),
            order.getReservationId(),
            order.getPaymentId()
        );
        
        Message<OrderCreatedEvent> message = MessageBuilder
            .withPayload(event)
            .setHeader("routingKey", "order.created." + order.getCustomerId())
            .setHeader("orderId", order.getId().toString())
            .setHeader("eventType", "OrderCreated")
            .setHeader("timestamp", Instant.now().toString())
            .build();
        
        boolean sent = streamBridge.send("orderCreated-out-0", message);
        
        if (sent) {
            System.out.println("Order created event published: " + event.getOrderId());
        } else {
            System.err.println("Failed to publish order created event: " + event.getOrderId());
        }
    }
    
    public void publishOrderStatusUpdated(String orderId, String oldStatus, String newStatus, String reason) {
        OrderStatusUpdatedEvent event = new OrderStatusUpdatedEvent(
            orderId,
            oldStatus,
            newStatus,
            LocalDateTime.now(),
            reason
        );
        
        Message<OrderStatusUpdatedEvent> message = MessageBuilder
            .withPayload(event)
            .setHeader("routingKey", "order.status.updated." + orderId)
            .setHeader("orderId", orderId)
            .setHeader("eventType", "OrderStatusUpdated")
            .setHeader("timestamp", Instant.now().toString())
            .build();
        
        streamBridge.send("orderStatusUpdated-out-0", message);
        System.out.println("Order status updated event published: " + orderId);
    }
}
```

### Inventory Event Publisher

```java
@Component
public class InventoryEventPublisher {
    
    private final StreamBridge streamBridge;
    
    public InventoryEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }
    
    public void publishInventoryReserved(String reservationId, Long productId, Integer quantity, String orderId) {
        InventoryReservedEvent event = new InventoryReservedEvent(
            reservationId,
            productId,
            quantity,
            orderId,
            LocalDateTime.now()
        );
        
        Message<InventoryReservedEvent> message = MessageBuilder
            .withPayload(event)
            .setHeader("routingKey", "inventory.reserved." + productId)
            .setHeader("productId", productId.toString())
            .setHeader("orderId", orderId)
            .setHeader("eventType", "InventoryReserved")
            .setHeader("timestamp", Instant.now().toString())
            .build();
        
        streamBridge.send("inventoryReserved-out-0", message);
        System.out.println("Inventory reserved event published: " + reservationId);
    }
    
    public void publishInventoryUpdated(Long productId, Integer previousQuantity, Integer newQuantity, String operation) {
        InventoryUpdatedEvent event = new InventoryUpdatedEvent(
            productId,
            previousQuantity,
            newQuantity,
            operation,
            LocalDateTime.now()
        );
        
        Message<InventoryUpdatedEvent> message = MessageBuilder
            .withPayload(event)
            .setHeader("routingKey", "inventory.updated." + productId)
            .setHeader("productId", productId.toString())
            .setHeader("operation", operation)
            .setHeader("eventType", "InventoryUpdated")
            .setHeader("timestamp", Instant.now().toString())
            .build();
        
        streamBridge.send("inventoryUpdated-out-0", message);
        System.out.println("Inventory updated event published: " + productId);
    }
}
```

## 5. Event Consumers

### Order Event Consumer

```java
@Component
public class OrderEventConsumer {
    
    private final NotificationService notificationService;
    private final AnalyticsService analyticsService;
    
    public OrderEventConsumer(NotificationService notificationService,
                             AnalyticsService analyticsService) {
        this.notificationService = notificationService;
        this.analyticsService = analyticsService;
    }
    
    @Bean
    public Consumer<Message<OrderCreatedEvent>> orderCreated() {
        return message -> {
            OrderCreatedEvent event = message.getPayload();
            MessageHeaders headers = message.getHeaders();
            
            System.out.println("Received order created event: " + event.getOrderId());
            System.out.println("Headers: " + headers);
            
            try {
                // Send notification
                notificationService.sendOrderConfirmation(event);
                
                // Update analytics
                analyticsService.recordOrderCreated(event);
                
                // Log for audit
                System.out.println("Order created event processed successfully: " + event.getOrderId());
                
            } catch (Exception e) {
                System.err.println("Error processing order created event: " + e.getMessage());
                throw new RuntimeException("Failed to process order created event", e);
            }
        };
    }
    
    @Bean
    public Consumer<Message<OrderStatusUpdatedEvent>> orderStatusUpdated() {
        return message -> {
            OrderStatusUpdatedEvent event = message.getPayload();
            
            System.out.println("Received order status updated event: " + event.getOrderId());
            System.out.println("Status changed from " + event.getOldStatus() + " to " + event.getNewStatus());
            
            try {
                // Send status update notification
                notificationService.sendOrderStatusUpdate(event);
                
                // Update analytics
                analyticsService.recordOrderStatusChange(event);
                
                System.out.println("Order status updated event processed successfully: " + event.getOrderId());
                
            } catch (Exception e) {
                System.err.println("Error processing order status updated event: " + e.getMessage());
                throw new RuntimeException("Failed to process order status updated event", e);
            }
        };
    }
}
```

### Inventory Event Consumer

```java
@Component
public class InventoryEventConsumer {
    
    private final ReportingService reportingService;
    private final AlertingService alertingService;
    
    public InventoryEventConsumer(ReportingService reportingService,
                                 AlertingService alertingService) {
        this.reportingService = reportingService;
        this.alertingService = alertingService;
    }
    
    @Bean
    public Consumer<Message<InventoryReservedEvent>> inventoryReserved() {
        return message -> {
            InventoryReservedEvent event = message.getPayload();
            
            System.out.println("Received inventory reserved event: " + event.getReservationId());
            
            try {
                // Update reporting
                reportingService.recordInventoryReservation(event);
                
                // Check for low stock alert
                alertingService.checkLowStockAlert(event.getProductId());
                
                System.out.println("Inventory reserved event processed successfully: " + event.getReservationId());
                
            } catch (Exception e) {
                System.err.println("Error processing inventory reserved event: " + e.getMessage());
                throw new RuntimeException("Failed to process inventory reserved event", e);
            }
        };
    }
    
    @Bean
    public Consumer<Message<InventoryUpdatedEvent>> inventoryUpdated() {
        return message -> {
            InventoryUpdatedEvent event = message.getPayload();
            
            System.out.println("Received inventory updated event: " + event.getProductId());
            System.out.println("Operation: " + event.getOperation() + 
                             ", Previous: " + event.getPreviousQuantity() + 
                             ", New: " + event.getNewQuantity());
            
            try {
                // Update reporting
                reportingService.recordInventoryUpdate(event);
                
                // Check for alerts
                if ("RESERVED".equals(event.getOperation()) && event.getNewQuantity() < 10) {
                    alertingService.sendLowStockAlert(event.getProductId(), event.getNewQuantity());
                }
                
                System.out.println("Inventory updated event processed successfully: " + event.getProductId());
                
            } catch (Exception e) {
                System.err.println("Error processing inventory updated event: " + e.getMessage());
                throw new RuntimeException("Failed to process inventory updated event", e);
            }
        };
    }
}
```

## 6. Error Handling and Dead Letter Queues

### Error Handling Configuration

```java
@Configuration
public class StreamErrorHandlingConfig {
    
    @Bean
    public ListenerContainerCustomizer<MessageListenerContainer> customizer() {
        return (container, destination, group) -> {
            container.setErrorHandler(new ConditionalRejectingErrorHandler(
                new MyFatalExceptionStrategy()));
        };
    }
    
    public static class MyFatalExceptionStrategy 
            extends ConditionalRejectingErrorHandler.DefaultExceptionStrategy {
        
        @Override
        public boolean isFatal(Throwable t) {
            return !(t.getCause() instanceof RequeueCurrentMessageException);
        }
    }
}
```

### Dead Letter Queue Handler

```java
@Component
public class DeadLetterQueueHandler {
    
    private final StreamBridge streamBridge;
    
    public DeadLetterQueueHandler(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }
    
    @Bean
    public Consumer<Message<Object>> orderCreatedDlq() {
        return message -> {
            System.err.println("Processing message from DLQ: " + message.getPayload());
            
            // Log the failed message
            logFailedMessage(message);
            
            // Attempt to parse and fix the message
            try {
                Object payload = message.getPayload();
                if (payload instanceof OrderCreatedEvent) {
                    handleFailedOrderCreatedEvent((OrderCreatedEvent) payload, message);
                }
            } catch (Exception e) {
                System.err.println("Failed to process DLQ message: " + e.getMessage());
                // Send to manual review queue or alert system
                sendToManualReview(message);
            }
        };
    }
    
    private void logFailedMessage(Message<Object> message) {
        System.err.println("Failed message headers: " + message.getHeaders());
        System.err.println("Failed message payload: " + message.getPayload());
        
        // Log to database or file for later analysis
        // auditService.logFailedMessage(message);
    }
    
    private void handleFailedOrderCreatedEvent(OrderCreatedEvent event, Message<Object> message) {
        System.out.println("Attempting to reprocess failed order event: " + event.getOrderId());
        
        // Attempt to fix known issues
        if (event.getCustomerId() == null) {
            System.err.println("Cannot process order event without customer ID");
            return;
        }
        
        // Try to reprocess with corrected data
        try {
            // republish the event with corrections
            Message<OrderCreatedEvent> correctedMessage = MessageBuilder
                .withPayload(event)
                .setHeader("reprocessed", true)
                .setHeader("originalFailureTime", Instant.now().toString())
                .build();
            
            streamBridge.send("orderCreated-out-0", correctedMessage);
            System.out.println("Reprocessed failed order event: " + event.getOrderId());
        } catch (Exception e) {
            System.err.println("Failed to reprocess order event: " + e.getMessage());
        }
    }
    
    private void sendToManualReview(Message<Object> message) {
        // Send to manual review queue or alert system administrators
        System.err.println("Sending message to manual review: " + message.getPayload());
    }
}
```

## 7. Event Sourcing Pattern

### Event Store

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
    
    // Constructors, getters, and setters
    public EventStore() {}
    
    public EventStore(String aggregateId, String aggregateType, String eventType,
                     String eventData, Long eventVersion) {
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.eventData = eventData;
        this.eventVersion = eventVersion;
        this.createdAt = LocalDateTime.now();
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
}
```

### Event Sourcing Service

```java
@Service
public class EventSourcingService {
    
    private final EventStoreRepository eventStoreRepository;
    private final ObjectMapper objectMapper;
    private final OrderEventPublisher orderEventPublisher;
    
    public EventSourcingService(EventStoreRepository eventStoreRepository,
                               ObjectMapper objectMapper,
                               OrderEventPublisher orderEventPublisher) {
        this.eventStoreRepository = eventStoreRepository;
        this.objectMapper = objectMapper;
        this.orderEventPublisher = orderEventPublisher;
    }
    
    @Transactional
    public void saveEvent(String aggregateId, String aggregateType, Object event) {
        try {
            String eventType = event.getClass().getSimpleName();
            String eventData = objectMapper.writeValueAsString(event);
            
            // Get next version number
            Long nextVersion = getNextVersion(aggregateId, aggregateType);
            
            // Save event
            EventStore eventStore = new EventStore(
                aggregateId,
                aggregateType,
                eventType,
                eventData,
                nextVersion
            );
            
            eventStoreRepository.save(eventStore);
            
            // Publish event to message broker
            publishEvent(event, eventType);
            
            System.out.println("Event saved: " + eventType + " for aggregate: " + aggregateId);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to save event", e);
        }
    }
    
    private Long getNextVersion(String aggregateId, String aggregateType) {
        return eventStoreRepository.findMaxVersionByAggregateId(aggregateId, aggregateType)
                .map(version -> version + 1)
                .orElse(1L);
    }
    
    private void publishEvent(Object event, String eventType) {
        if (event instanceof OrderCreatedEvent) {
            orderEventPublisher.publishOrderCreated(reconstructOrderFromEvent((OrderCreatedEvent) event));
        } else if (event instanceof OrderStatusUpdatedEvent) {
            OrderStatusUpdatedEvent statusEvent = (OrderStatusUpdatedEvent) event;
            orderEventPublisher.publishOrderStatusUpdated(
                statusEvent.getOrderId(),
                statusEvent.getOldStatus(),
                statusEvent.getNewStatus(),
                statusEvent.getReason()
            );
        }
    }
    
    public List<EventStore> getEventsByAggregateId(String aggregateId, String aggregateType) {
        return eventStoreRepository.findByAggregateIdAndAggregateTypeOrderByEventVersion(
            aggregateId, aggregateType);
    }
    
    public Order reconstructOrderFromEvents(String orderId) {
        List<EventStore> events = getEventsByAggregateId(orderId, "Order");
        
        Order order = new Order();
        
        for (EventStore eventStore : events) {
            try {
                switch (eventStore.getEventType()) {
                    case "OrderCreatedEvent":
                        OrderCreatedEvent createdEvent = objectMapper.readValue(
                            eventStore.getEventData(), OrderCreatedEvent.class);
                        applyOrderCreatedEvent(order, createdEvent);
                        break;
                    case "OrderStatusUpdatedEvent":
                        OrderStatusUpdatedEvent statusEvent = objectMapper.readValue(
                            eventStore.getEventData(), OrderStatusUpdatedEvent.class);
                        applyOrderStatusUpdatedEvent(order, statusEvent);
                        break;
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to reconstruct order from events", e);
            }
        }
        
        return order;
    }
    
    private void applyOrderCreatedEvent(Order order, OrderCreatedEvent event) {
        order.setId(Long.valueOf(event.getOrderId()));
        order.setCustomerId(event.getCustomerId());
        order.setProductId(event.getProductId());
        order.setQuantity(event.getQuantity());
        order.setTotalAmount(event.getTotalAmount());
        order.setStatus(event.getStatus());
        order.setCreatedAt(event.getCreatedAt());
        order.setReservationId(event.getReservationId());
        order.setPaymentId(event.getPaymentId());
    }
    
    private void applyOrderStatusUpdatedEvent(Order order, OrderStatusUpdatedEvent event) {
        order.setStatus(event.getNewStatus());
        order.setUpdatedAt(event.getUpdatedAt());
    }
    
    private Order reconstructOrderFromEvent(OrderCreatedEvent event) {
        Order order = new Order();
        order.setId(Long.valueOf(event.getOrderId()));
        order.setCustomerId(event.getCustomerId());
        order.setProductId(event.getProductId());
        order.setQuantity(event.getQuantity());
        order.setTotalAmount(event.getTotalAmount());
        order.setStatus(event.getStatus());
        order.setCreatedAt(event.getCreatedAt());
        order.setReservationId(event.getReservationId());
        order.setPaymentId(event.getPaymentId());
        return order;
    }
}
```

## 8. Stream Processing

### Stream Processing Configuration

```java
@Configuration
public class StreamProcessingConfig {
    
    @Bean
    public Function<Flux<OrderCreatedEvent>, Flux<OrderAnalytics>> orderAnalytics() {
        return orderStream -> orderStream
            .window(Duration.ofMinutes(5))
            .flatMap(window -> window
                .collectList()
                .map(orders -> {
                    OrderAnalytics analytics = new OrderAnalytics();
                    analytics.setWindowStart(LocalDateTime.now().minusMinutes(5));
                    analytics.setWindowEnd(LocalDateTime.now());
                    analytics.setTotalOrders(orders.size());
                    analytics.setTotalRevenue(orders.stream()
                        .map(OrderCreatedEvent::getTotalAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
                    analytics.setAverageOrderValue(
                        orders.isEmpty() ? BigDecimal.ZERO : 
                        analytics.getTotalRevenue().divide(
                            BigDecimal.valueOf(orders.size()), 
                            2, 
                            RoundingMode.HALF_UP
                        )
                    );
                    return analytics;
                })
            );
    }
    
    @Bean
    public Function<Flux<InventoryUpdatedEvent>, Flux<InventoryAlert>> inventoryAlerts() {
        return inventoryStream -> inventoryStream
            .filter(event -> "RESERVED".equals(event.getOperation()))
            .groupBy(InventoryUpdatedEvent::getProductId)
            .flatMap(groupedFlux -> groupedFlux
                .window(Duration.ofMinutes(1))
                .flatMap(window -> window
                    .reduce(0, (count, event) -> count + event.getQuantity())
                    .filter(totalReserved -> totalReserved > 100) // Alert threshold
                    .map(totalReserved -> {
                        InventoryAlert alert = new InventoryAlert();
                        alert.setProductId(groupedFlux.key());
                        alert.setAlertType("HIGH_RESERVATION_RATE");
                        alert.setMessage("High reservation rate detected: " + totalReserved + " units in 1 minute");
                        alert.setTimestamp(LocalDateTime.now());
                        return alert;
                    })
                )
            );
    }
}
```

### Analytics Models

```java
public class OrderAnalytics {
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;
    private Integer totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;
    
    // Constructors, getters, and setters
    public OrderAnalytics() {}
    
    public LocalDateTime getWindowStart() { return windowStart; }
    public void setWindowStart(LocalDateTime windowStart) { this.windowStart = windowStart; }
    
    public LocalDateTime getWindowEnd() { return windowEnd; }
    public void setWindowEnd(LocalDateTime windowEnd) { this.windowEnd = windowEnd; }
    
    public Integer getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Integer totalOrders) { this.totalOrders = totalOrders; }
    
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    
    public BigDecimal getAverageOrderValue() { return averageOrderValue; }
    public void setAverageOrderValue(BigDecimal averageOrderValue) { this.averageOrderValue = averageOrderValue; }
}

public class InventoryAlert {
    private Long productId;
    private String alertType;
    private String message;
    private LocalDateTime timestamp;
    
    // Constructors, getters, and setters
    public InventoryAlert() {}
    
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
```

## 9. Testing Event-Driven Architecture

### Integration Tests

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cloud.stream.bindings.orderCreated-out-0.destination=test-order-events",
    "spring.cloud.stream.bindings.orderCreated-in-0.destination=test-order-events"
})
class EventDrivenIntegrationTest {
    
    @Autowired
    private OrderEventPublisher orderEventPublisher;
    
    @Autowired
    private TestBinder testBinder;
    
    @Test
    void testOrderCreatedEventFlow() {
        // Given
        Order order = new Order();
        order.setId(1L);
        order.setCustomerId(100L);
        order.setProductId(200L);
        order.setQuantity(2);
        order.setTotalAmount(new BigDecimal("59.98"));
        order.setStatus("CONFIRMED");
        order.setCreatedAt(LocalDateTime.now());
        order.setReservationId("res-123");
        order.setPaymentId("pay-123");
        
        // When
        orderEventPublisher.publishOrderCreated(order);
        
        // Then
        Message<OrderCreatedEvent> received = testBinder.receive("test-order-events");
        assertThat(received).isNotNull();
        assertThat(received.getPayload().getOrderId()).isEqualTo("1");
        assertThat(received.getPayload().getCustomerId()).isEqualTo(100L);
        assertThat(received.getPayload().getProductId()).isEqualTo(200L);
    }
}
```

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {
    
    @Mock
    private NotificationService notificationService;
    
    @Mock
    private AnalyticsService analyticsService;
    
    @InjectMocks
    private OrderEventConsumer orderEventConsumer;
    
    @Test
    void testOrderCreatedEventProcessing() {
        // Given
        OrderCreatedEvent event = new OrderCreatedEvent(
            "1", 100L, 200L, 2, new BigDecimal("59.98"), "CONFIRMED",
            LocalDateTime.now(), "res-123", "pay-123"
        );
        
        Message<OrderCreatedEvent> message = MessageBuilder
            .withPayload(event)
            .setHeader("orderId", "1")
            .build();
        
        // When
        orderEventConsumer.orderCreated().accept(message);
        
        // Then
        verify(notificationService).sendOrderConfirmation(event);
        verify(analyticsService).recordOrderCreated(event);
    }
}
```

## 10. Monitoring and Observability

### Event Metrics

```java
@Component
public class EventMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter eventPublishedCounter;
    private final Counter eventConsumedCounter;
    private final Timer eventProcessingTimer;
    
    public EventMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.eventPublishedCounter = Counter.builder("events.published")
            .description("Number of events published")
            .register(meterRegistry);
        this.eventConsumedCounter = Counter.builder("events.consumed")
            .description("Number of events consumed")
            .register(meterRegistry);
        this.eventProcessingTimer = Timer.builder("events.processing.time")
            .description("Event processing time")
            .register(meterRegistry);
    }
    
    public void recordEventPublished(String eventType) {
        eventPublishedCounter.increment(Tags.of("event.type", eventType));
    }
    
    public void recordEventConsumed(String eventType) {
        eventConsumedCounter.increment(Tags.of("event.type", eventType));
    }
    
    public Timer.Sample startProcessingTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordProcessingTime(Timer.Sample sample, String eventType) {
        sample.stop(Timer.builder("events.processing.time")
            .tag("event.type", eventType)
            .register(meterRegistry));
    }
}
```

## Summary

Today we covered:

1. **Spring Cloud Stream Fundamentals**: Core concepts and architecture
2. **Message Broker Configuration**: RabbitMQ and Apache Kafka setup
3. **Event Models**: Designing event structures for domain events
4. **Event Publishers**: Publishing events to message brokers
5. **Event Consumers**: Processing events with functional programming
6. **Error Handling**: Dead letter queues and retry mechanisms
7. **Event Sourcing**: Implementing event sourcing patterns
8. **Stream Processing**: Real-time stream processing with reactive programming
9. **Testing**: Unit and integration testing for event-driven systems
10. **Monitoring**: Metrics and observability for event processing

## Key Takeaways

- Spring Cloud Stream provides abstraction over message brokers
- Event-driven architecture enables loose coupling between services
- Dead letter queues are essential for handling failed message processing
- Event sourcing provides audit trails and system reconstruction capabilities
- Stream processing enables real-time analytics and monitoring
- Proper error handling and monitoring are crucial for production systems

## Next Steps

Tomorrow we'll complete Week 10 with a comprehensive capstone project that integrates all Spring Cloud components into a complete cloud-native e-commerce application.