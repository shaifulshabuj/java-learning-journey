# Day 76: Reactive Messaging and WebSockets

## Overview

Today we'll explore reactive messaging patterns and WebSocket communication. We'll learn about non-blocking message processing with Project Reactor, real-time bidirectional communication with WebSockets, and building responsive, scalable applications with reactive streams.

## Learning Objectives

- Understand reactive messaging patterns and Project Reactor
- Implement WebSocket communication for real-time applications
- Build reactive message processing pipelines
- Handle backpressure and flow control in reactive streams
- Integrate reactive messaging with message brokers
- Implement Server-Sent Events (SSE) for real-time updates

## 1. Reactive Messaging Fundamentals

### Project Reactor Setup

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-r2dbc</artifactId>
</dependency>
<dependency>
    <groupId>io.projectreactor.kafka</groupId>
    <artifactId>reactor-kafka</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-rsocket</artifactId>
</dependency>
<dependency>
    <groupId>io.projectreactor.rabbitmq</groupId>
    <artifactId>reactor-rabbitmq</artifactId>
</dependency>
```

### Reactive Configuration

```java
@Configuration
@EnableWebFlux
public class ReactiveConfig {
    
    @Bean
    public ReactiveKafkaProducerTemplate<String, Object> reactiveKafkaProducerTemplate() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        
        return new ReactiveKafkaProducerTemplate<>(SenderOptions.create(props));
    }
    
    @Bean
    public ReactiveKafkaConsumerTemplate<String, Object> reactiveKafkaConsumerTemplate() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "reactive-order-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        
        return new ReactiveKafkaConsumerTemplate<>(ReceiverOptions.create(props));
    }
    
    @Bean
    public ConnectionFactory rabbitConnectionFactory() {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("admin");
        connectionFactory.setPassword("admin");
        return connectionFactory;
    }
    
    @Bean
    public Sender rabbitSender(ConnectionFactory connectionFactory) {
        return RabbitFlux.createSender(new SenderOptions().connectionFactory(connectionFactory));
    }
    
    @Bean
    public Receiver rabbitReceiver(ConnectionFactory connectionFactory) {
        return RabbitFlux.createReceiver(new ReceiverOptions().connectionFactory(connectionFactory));
    }
}
```

## 2. Reactive Kafka Integration

### Reactive Kafka Producer

```java
@Service
public class ReactiveOrderProducer {
    
    private final ReactiveKafkaProducerTemplate<String, Object> kafkaTemplate;
    
    public ReactiveOrderProducer(ReactiveKafkaProducerTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    public Mono<SendResult<String, Object>> sendOrder(Order order) {
        return kafkaTemplate.send("order-events", order.getId().toString(), order)
            .doOnSuccess(result -> 
                System.out.println("Order sent successfully: " + order.getId()))
            .doOnError(error -> 
                System.err.println("Failed to send order: " + error.getMessage()))
            .onErrorMap(ex -> new MessageSendException("Failed to send order", ex));
    }
    
    public Flux<SendResult<String, Object>> sendOrderBatch(Flux<Order> orders) {
        return orders
            .map(order -> new ProducerRecord<>("order-events", order.getId().toString(), order))
            .as(records -> kafkaTemplate.send(records))
            .doOnNext(result -> 
                System.out.println("Order batch item sent: " + result.recordMetadata().offset()))
            .onErrorContinue((error, item) -> 
                System.err.println("Failed to send order batch item: " + error.getMessage()));
    }
    
    public Mono<Void> sendOrderStream(Flux<Order> orderStream) {
        return orderStream
            .delayElements(Duration.ofMillis(100)) // Throttle to avoid overwhelming
            .flatMap(this::sendOrder, 10) // Concurrency of 10
            .doOnComplete(() -> System.out.println("Order stream processing completed"))
            .then();
    }
    
    public Flux<Order> processOrdersWithBackpressure(Flux<Order> orders) {
        return orders
            .onBackpressureBuffer(1000) // Buffer up to 1000 orders
            .publishOn(Schedulers.boundedElastic())
            .flatMap(this::enrichOrder, 5) // Process 5 orders concurrently
            .doOnNext(order -> System.out.println("Processed order: " + order.getId()));
    }
    
    private Mono<Order> enrichOrder(Order order) {
        return Mono.fromCallable(() -> {
            // Simulate enrichment process
            order.setProcessedAt(LocalDateTime.now());
            order.setEnrichmentVersion("1.0");
            return order;
        })
        .delayElement(Duration.ofMillis(50)) // Simulate processing time
        .subscribeOn(Schedulers.boundedElastic());
    }
}
```

### Reactive Kafka Consumer

```java
@Service
public class ReactiveOrderConsumer {
    
    private final ReactiveKafkaConsumerTemplate<String, Object> kafkaTemplate;
    private final OrderService orderService;
    private final NotificationService notificationService;
    
    public ReactiveOrderConsumer(ReactiveKafkaConsumerTemplate<String, Object> kafkaTemplate,
                                OrderService orderService,
                                NotificationService notificationService) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderService = orderService;
        this.notificationService = notificationService;
    }
    
    @PostConstruct
    public void startConsumer() {
        consumeOrders()
            .subscribe(
                order -> System.out.println("Order processed: " + order.getId()),
                error -> System.err.println("Error processing orders: " + error.getMessage()),
                () -> System.out.println("Order consumer completed")
            );
    }
    
    public Flux<Order> consumeOrders() {
        return kafkaTemplate
            .receiveAutoAck()
            .concatMap(record -> {
                try {
                    Order order = (Order) record.value();
                    return processOrder(order)
                        .doOnSuccess(result -> record.receiverOffset().acknowledge())
                        .onErrorResume(error -> {
                            System.err.println("Failed to process order: " + error.getMessage());
                            record.receiverOffset().acknowledge(); // Acknowledge to avoid infinite retry
                            return Mono.empty();
                        });
                } catch (Exception e) {
                    record.receiverOffset().acknowledge();
                    return Mono.empty();
                }
            })
            .onBackpressureBuffer(500, error -> 
                System.err.println("Consumer buffer overflow: " + error.getMessage()));
    }
    
    public Flux<Order> consumeOrdersWithRetry() {
        return kafkaTemplate
            .receiveAutoAck()
            .concatMap(record -> {
                Order order = (Order) record.value();
                return processOrderWithRetry(order)
                    .doFinally(signalType -> record.receiverOffset().acknowledge());
            });
    }
    
    private Mono<Order> processOrder(Order order) {
        return Mono.fromCallable(() -> orderService.processOrder(order))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(processedOrder -> sendNotification(processedOrder)
                .thenReturn(processedOrder));
    }
    
    private Mono<Order> processOrderWithRetry(Order order) {
        return processOrder(order)
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                .filter(throwable -> !(throwable instanceof OrderValidationException))
                .onRetryExhaustedThrow((retrySpec, retrySignal) -> 
                    new OrderProcessingException("Failed to process order after retries", 
                        retrySignal.failure())));
    }
    
    private Mono<Void> sendNotification(Order order) {
        return notificationService.sendOrderNotificationAsync(order)
            .onErrorResume(error -> {
                System.err.println("Failed to send notification: " + error.getMessage());
                return Mono.empty();
            });
    }
}
```

## 3. Reactive RabbitMQ Integration

### Reactive RabbitMQ Producer

```java
@Service
public class ReactiveRabbitProducer {
    
    private final Sender sender;
    
    public ReactiveRabbitProducer(Sender sender) {
        this.sender = sender;
    }
    
    public Mono<Void> sendOrder(Order order) {
        OutboundMessage message = new OutboundMessage(
            "order.exchange",
            "order.created",
            convertToBytes(order)
        );
        
        return sender.send(Mono.just(message))
            .doOnNext(result -> {
                if (result.isAck()) {
                    System.out.println("Order sent successfully: " + order.getId());
                } else {
                    System.err.println("Order send failed: " + order.getId());
                }
            })
            .then();
    }
    
    public Mono<Void> sendOrdersWithConfirmation(Flux<Order> orders) {
        return orders
            .map(order -> new OutboundMessage(
                "order.exchange",
                "order.created",
                convertToBytes(order)
            ))
            .as(messages -> sender.sendWithTypedPublishConfirms(messages))
            .doOnNext(result -> {
                if (result.isAck()) {
                    System.out.println("Message confirmed");
                } else {
                    System.err.println("Message rejected");
                }
            })
            .then();
    }
    
    public Flux<OutboundMessageResult> sendOrderStreamWithResults(Flux<Order> orders) {
        return orders
            .map(order -> new OutboundMessage(
                "order.exchange",
                "order.created",
                convertToBytes(order)
            ))
            .window(100) // Process in batches of 100
            .flatMap(batch -> sender.sendWithTypedPublishConfirms(batch))
            .doOnNext(result -> {
                if (!result.isAck()) {
                    System.err.println("Message send failed: " + result.getMessage());
                }
            });
    }
    
    private byte[] convertToBytes(Order order) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper.writeValueAsBytes(order);
        } catch (Exception e) {
            throw new MessageConversionException("Failed to convert order to bytes", e);
        }
    }
}
```

### Reactive RabbitMQ Consumer

```java
@Service
public class ReactiveRabbitConsumer {
    
    private final Receiver receiver;
    private final OrderService orderService;
    
    public ReactiveRabbitConsumer(Receiver receiver, OrderService orderService) {
        this.receiver = receiver;
        this.orderService = orderService;
    }
    
    @PostConstruct
    public void startConsumer() {
        consumeOrders()
            .subscribe(
                order -> System.out.println("Order consumed: " + order.getId()),
                error -> System.err.println("Consumer error: " + error.getMessage())
            );
    }
    
    public Flux<Order> consumeOrders() {
        return receiver.consumeAutoAck("order.queue")
            .map(delivery -> convertFromBytes(delivery.getBody()))
            .onBackpressureBuffer(1000)
            .flatMap(this::processOrder, 5)
            .onErrorContinue((error, item) -> 
                System.err.println("Error processing order: " + error.getMessage()));
    }
    
    public Flux<Order> consumeOrdersWithManualAck() {
        return receiver.consumeManualAck("order.queue")
            .concatMap(delivery -> {
                try {
                    Order order = convertFromBytes(delivery.getBody());
                    return processOrder(order)
                        .doOnSuccess(result -> delivery.ack())
                        .doOnError(error -> delivery.nack(false));
                } catch (Exception e) {
                    delivery.nack(false);
                    return Mono.empty();
                }
            });
    }
    
    private Mono<Order> processOrder(Order order) {
        return Mono.fromCallable(() -> orderService.processOrder(order))
            .subscribeOn(Schedulers.boundedElastic())
            .timeout(Duration.ofSeconds(30))
            .onErrorMap(TimeoutException.class, 
                ex -> new OrderProcessingException("Order processing timeout", ex));
    }
    
    private Order convertFromBytes(byte[] data) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper.readValue(data, Order.class);
        } catch (Exception e) {
            throw new MessageConversionException("Failed to convert bytes to order", e);
        }
    }
}
```

## 4. WebSocket Implementation

### WebSocket Configuration

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new OrderWebSocketHandler(), "/orders")
            .addHandler(new NotificationWebSocketHandler(), "/notifications")
            .setAllowedOrigins("*")
            .withSockJS();
    }
    
    @Bean
    public WebSocketSession webSocketSessionManager() {
        return new WebSocketSessionManager();
    }
}
```

### WebSocket Handlers

```java
@Component
public class OrderWebSocketHandler extends TextWebSocketHandler {
    
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("WebSocket connection established: " + session.getId());
        
        // Send welcome message
        WelcomeMessage welcome = new WelcomeMessage("Connected to Order WebSocket", LocalDateTime.now());
        sendMessage(session, welcome);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("WebSocket connection closed: " + session.getId());
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            WebSocketMessage wsMessage = objectMapper.readValue(payload, WebSocketMessage.class);
            
            switch (wsMessage.getType()) {
                case "SUBSCRIBE_ORDER":
                    handleOrderSubscription(session, wsMessage);
                    break;
                case "UNSUBSCRIBE_ORDER":
                    handleOrderUnsubscription(session, wsMessage);
                    break;
                case "GET_ORDER_STATUS":
                    handleOrderStatusRequest(session, wsMessage);
                    break;
                default:
                    sendErrorMessage(session, "Unknown message type: " + wsMessage.getType());
            }
            
        } catch (Exception e) {
            sendErrorMessage(session, "Error processing message: " + e.getMessage());
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket transport error: " + exception.getMessage());
        sessions.remove(session);
    }
    
    private void handleOrderSubscription(WebSocketSession session, WebSocketMessage message) throws Exception {
        String orderId = message.getData().get("orderId").toString();
        session.getAttributes().put("subscribedOrderId", orderId);
        
        SubscriptionResponse response = new SubscriptionResponse(
            "ORDER_SUBSCRIPTION_CONFIRMED", 
            orderId, 
            "Subscribed to order updates"
        );
        sendMessage(session, response);
    }
    
    private void handleOrderUnsubscription(WebSocketSession session, WebSocketMessage message) throws Exception {
        session.getAttributes().remove("subscribedOrderId");
        
        SubscriptionResponse response = new SubscriptionResponse(
            "ORDER_UNSUBSCRIPTION_CONFIRMED", 
            null, 
            "Unsubscribed from order updates"
        );
        sendMessage(session, response);
    }
    
    private void handleOrderStatusRequest(WebSocketSession session, WebSocketMessage message) throws Exception {
        String orderId = message.getData().get("orderId").toString();
        
        // Simulate order status lookup
        OrderStatusResponse statusResponse = new OrderStatusResponse(
            orderId, 
            "PROCESSING", 
            LocalDateTime.now()
        );
        sendMessage(session, statusResponse);
    }
    
    public void broadcastOrderUpdate(OrderUpdateEvent event) {
        sessions.parallelStream()
            .filter(session -> {
                String subscribedOrderId = (String) session.getAttributes().get("subscribedOrderId");
                return event.getOrderId().equals(subscribedOrderId);
            })
            .forEach(session -> {
                try {
                    sendMessage(session, event);
                } catch (Exception e) {
                    System.err.println("Failed to send order update: " + e.getMessage());
                }
            });
    }
    
    public void broadcastToAll(Object message) {
        sessions.parallelStream()
            .forEach(session -> {
                try {
                    sendMessage(session, message);
                } catch (Exception e) {
                    System.err.println("Failed to broadcast message: " + e.getMessage());
                }
            });
    }
    
    private void sendMessage(WebSocketSession session, Object message) throws Exception {
        if (session.isOpen()) {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        }
    }
    
    private void sendErrorMessage(WebSocketSession session, String error) throws Exception {
        ErrorMessage errorMessage = new ErrorMessage("ERROR", error, LocalDateTime.now());
        sendMessage(session, errorMessage);
    }
}
```

### WebSocket Message Models

```java
public class WebSocketMessage {
    private String type;
    private Map<String, Object> data;
    private LocalDateTime timestamp;
    
    public WebSocketMessage() {
        this.timestamp = LocalDateTime.now();
    }
    
    public WebSocketMessage(String type, Map<String, Object> data) {
        this.type = type;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}

public class OrderUpdateEvent {
    private String orderId;
    private String oldStatus;
    private String newStatus;
    private String reason;
    private LocalDateTime timestamp;
    
    public OrderUpdateEvent() {}
    
    public OrderUpdateEvent(String orderId, String oldStatus, String newStatus, String reason) {
        this.orderId = orderId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.timestamp = LocalDateTime.now();
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
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}

public class WelcomeMessage {
    private String message;
    private LocalDateTime timestamp;
    
    public WelcomeMessage(String message, LocalDateTime timestamp) {
        this.message = message;
        this.timestamp = timestamp;
    }
    
    // Getters and setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}

public class ErrorMessage {
    private String type;
    private String message;
    private LocalDateTime timestamp;
    
    public ErrorMessage(String type, String message, LocalDateTime timestamp) {
        this.type = type;
        this.message = message;
        this.timestamp = timestamp;
    }
    
    // Getters and setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
```

## 5. Server-Sent Events (SSE)

### SSE Configuration

```java
@RestController
@RequestMapping("/api/events")
public class ServerSentEventsController {
    
    private final OrderService orderService;
    private final Sinks.Many<OrderEvent> orderEventSink;
    
    public ServerSentEventsController(OrderService orderService) {
        this.orderService = orderService;
        this.orderEventSink = Sinks.many().multicast().onBackpressureBuffer();
    }
    
    @GetMapping(value = "/orders", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<OrderEvent>> streamOrderEvents() {
        return orderEventSink.asFlux()
            .map(event -> ServerSentEvent.<OrderEvent>builder()
                .id(event.getEventId())
                .event(event.getEventType())
                .data(event)
                .comment("Order event stream")
                .build())
            .doOnSubscribe(subscription -> 
                System.out.println("New SSE subscription for order events"))
            .doOnCancel(() -> 
                System.out.println("SSE subscription cancelled for order events"));
    }
    
    @GetMapping(value = "/orders/{orderId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<OrderEvent>> streamOrderEvents(@PathVariable String orderId) {
        return orderEventSink.asFlux()
            .filter(event -> orderId.equals(event.getOrderId()))
            .map(event -> ServerSentEvent.<OrderEvent>builder()
                .id(event.getEventId())
                .event(event.getEventType())
                .data(event)
                .build())
            .doOnSubscribe(subscription -> 
                System.out.println("New SSE subscription for order: " + orderId));
    }
    
    @GetMapping(value = "/heartbeat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> heartbeat() {
        return Flux.interval(Duration.ofSeconds(30))
            .map(sequence -> ServerSentEvent.<String>builder()
                .id(String.valueOf(sequence))
                .event("heartbeat")
                .data("ping")
                .build());
    }
    
    @GetMapping(value = "/metrics", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<SystemMetrics>> streamMetrics() {
        return Flux.interval(Duration.ofSeconds(5))
            .map(tick -> collectSystemMetrics())
            .map(metrics -> ServerSentEvent.<SystemMetrics>builder()
                .id(String.valueOf(System.currentTimeMillis()))
                .event("metrics")
                .data(metrics)
                .build());
    }
    
    public void publishOrderEvent(OrderEvent event) {
        orderEventSink.tryEmitNext(event);
    }
    
    private SystemMetrics collectSystemMetrics() {
        Runtime runtime = Runtime.getRuntime();
        return new SystemMetrics(
            runtime.totalMemory() - runtime.freeMemory(),
            runtime.totalMemory(),
            runtime.availableProcessors(),
            LocalDateTime.now()
        );
    }
}
```

### SSE Event Models

```java
public class OrderEvent {
    private String eventId;
    private String orderId;
    private String eventType;
    private String status;
    private LocalDateTime timestamp;
    private Map<String, Object> data;
    
    public OrderEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
    }
    
    public OrderEvent(String orderId, String eventType, String status) {
        this();
        this.orderId = orderId;
        this.eventType = eventType;
        this.status = status;
    }
    
    // Getters and setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
}

public class SystemMetrics {
    private long usedMemory;
    private long totalMemory;
    private int availableProcessors;
    private LocalDateTime timestamp;
    
    public SystemMetrics(long usedMemory, long totalMemory, int availableProcessors, LocalDateTime timestamp) {
        this.usedMemory = usedMemory;
        this.totalMemory = totalMemory;
        this.availableProcessors = availableProcessors;
        this.timestamp = timestamp;
    }
    
    // Getters and setters
    public long getUsedMemory() { return usedMemory; }
    public void setUsedMemory(long usedMemory) { this.usedMemory = usedMemory; }
    
    public long getTotalMemory() { return totalMemory; }
    public void setTotalMemory(long totalMemory) { this.totalMemory = totalMemory; }
    
    public int getAvailableProcessors() { return availableProcessors; }
    public void setAvailableProcessors(int availableProcessors) { this.availableProcessors = availableProcessors; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
```

## 6. Reactive Stream Processing

### Stream Processing Pipeline

```java
@Service
public class ReactiveStreamProcessor {
    
    private final ReactiveOrderProducer producer;
    private final ReactiveOrderConsumer consumer;
    
    public ReactiveStreamProcessor(ReactiveOrderProducer producer, ReactiveOrderConsumer consumer) {
        this.producer = producer;
        this.consumer = consumer;
    }
    
    public Flux<OrderAnalytics> processOrderAnalytics(Flux<Order> orders) {
        return orders
            .window(Duration.ofMinutes(5)) // 5-minute windows
            .flatMap(window -> window
                .collectList()
                .map(this::calculateAnalytics)
            )
            .onBackpressureBuffer(100);
    }
    
    public Flux<Order> processOrdersWithEnrichment(Flux<Order> orders) {
        return orders
            .groupBy(Order::getCustomerId)
            .flatMap(groupedFlux -> groupedFlux
                .bufferTimeout(10, Duration.ofSeconds(5))
                .flatMap(this::enrichOrderBatch)
            )
            .onBackpressureLatest();
    }
    
    public Flux<AlertEvent> detectFraudulentOrders(Flux<Order> orders) {
        return orders
            .groupBy(Order::getCustomerId)
            .flatMap(groupedFlux -> groupedFlux
                .window(Duration.ofMinutes(10))
                .flatMap(window -> window
                    .collectList()
                    .filter(orderList -> orderList.size() > 5) // More than 5 orders in 10 minutes
                    .map(orderList -> new AlertEvent(
                        "FRAUD_ALERT",
                        "Multiple orders detected",
                        orderList.get(0).getCustomerId(),
                        LocalDateTime.now()
                    ))
                )
            );
    }
    
    public Flux<Order> retryFailedOrders(Flux<Order> failedOrders) {
        return failedOrders
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                .maxBackoff(Duration.ofSeconds(20))
                .jitter(0.5)
                .filter(throwable -> !(throwable instanceof OrderValidationException))
            )
            .onErrorContinue((error, item) -> 
                System.err.println("Permanently failed order: " + item + ", error: " + error.getMessage()));
    }
    
    public Mono<Void> processOrderStreamWithRateLimiting(Flux<Order> orders) {
        return orders
            .limitRate(100) // Limit to 100 orders per second
            .delayElements(Duration.ofMillis(10))
            .flatMap(producer::sendOrder, 10)
            .then();
    }
    
    private OrderAnalytics calculateAnalytics(List<Order> orders) {
        OrderAnalytics analytics = new OrderAnalytics();
        analytics.setWindowStart(LocalDateTime.now().minusMinutes(5));
        analytics.setWindowEnd(LocalDateTime.now());
        analytics.setTotalOrders(orders.size());
        analytics.setTotalRevenue(orders.stream()
            .map(Order::getTotalAmount)
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
    }
    
    private Flux<Order> enrichOrderBatch(List<Order> orders) {
        return Flux.fromIterable(orders)
            .flatMap(this::enrichOrder)
            .onErrorContinue((error, order) -> 
                System.err.println("Failed to enrich order: " + order + ", error: " + error.getMessage()));
    }
    
    private Mono<Order> enrichOrder(Order order) {
        return Mono.fromCallable(() -> {
            // Simulate enrichment
            order.setEnrichmentTimestamp(LocalDateTime.now());
            order.setRiskScore(calculateRiskScore(order));
            return order;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .timeout(Duration.ofSeconds(5));
    }
    
    private Double calculateRiskScore(Order order) {
        // Simple risk calculation
        double score = 0.0;
        if (order.getTotalAmount().compareTo(BigDecimal.valueOf(1000)) > 0) {
            score += 0.3;
        }
        if ("HIGH".equals(order.getPriority())) {
            score += 0.2;
        }
        return Math.min(score, 1.0);
    }
}
```

## 7. Reactive Controllers

### Reactive REST Controller

```java
@RestController
@RequestMapping("/api/reactive/orders")
public class ReactiveOrderController {
    
    private final ReactiveOrderService orderService;
    private final ReactiveOrderProducer producer;
    
    public ReactiveOrderController(ReactiveOrderService orderService, ReactiveOrderProducer producer) {
        this.orderService = orderService;
        this.producer = producer;
    }
    
    @PostMapping
    public Mono<ResponseEntity<Order>> createOrder(@RequestBody Order order) {
        return orderService.createOrder(order)
            .flatMap(createdOrder -> producer.sendOrder(createdOrder)
                .thenReturn(createdOrder))
            .map(createdOrder -> ResponseEntity.status(HttpStatus.CREATED).body(createdOrder))
            .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
    
    @GetMapping("/{orderId}")
    public Mono<ResponseEntity<Order>> getOrder(@PathVariable String orderId) {
        return orderService.getOrder(orderId)
            .map(order -> ResponseEntity.ok(order))
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    @GetMapping(produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<Order> getAllOrders() {
        return orderService.getAllOrders()
            .onBackpressureBuffer(1000);
    }
    
    @GetMapping(value = "/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<Order> streamOrders() {
        return orderService.getOrderStream()
            .delayElements(Duration.ofMillis(100))
            .take(Duration.ofMinutes(5));
    }
    
    @PostMapping("/batch")
    public Mono<ResponseEntity<BatchResult>> createOrderBatch(@RequestBody Flux<Order> orders) {
        return orders
            .flatMap(orderService::createOrder, 10)
            .collectList()
            .map(createdOrders -> {
                BatchResult result = new BatchResult(createdOrders.size(), 0);
                return ResponseEntity.ok(result);
            })
            .onErrorResume(error -> {
                BatchResult result = new BatchResult(0, 1);
                result.setErrorMessage(error.getMessage());
                return Mono.just(ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(result));
            });
    }
    
    @PutMapping("/{orderId}/status")
    public Mono<ResponseEntity<Order>> updateOrderStatus(@PathVariable String orderId, 
                                                        @RequestBody StatusUpdateRequest request) {
        return orderService.updateOrderStatus(orderId, request.getStatus())
            .map(updatedOrder -> ResponseEntity.ok(updatedOrder))
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
```

### Reactive Service

```java
@Service
public class ReactiveOrderService {
    
    private final R2dbcEntityTemplate template;
    private final ReactiveRedisTemplate<String, Order> redisTemplate;
    
    public ReactiveOrderService(R2dbcEntityTemplate template, 
                               ReactiveRedisTemplate<String, Order> redisTemplate) {
        this.template = template;
        this.redisTemplate = redisTemplate;
    }
    
    public Mono<Order> createOrder(Order order) {
        order.setId(UUID.randomUUID().toString());
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus("PENDING");
        
        return template.insert(order)
            .flatMap(savedOrder -> 
                redisTemplate.opsForValue()
                    .set("order:" + savedOrder.getId(), savedOrder, Duration.ofHours(1))
                    .thenReturn(savedOrder)
            );
    }
    
    public Mono<Order> getOrder(String orderId) {
        return redisTemplate.opsForValue()
            .get("order:" + orderId)
            .switchIfEmpty(
                template.selectOne(Query.query(Criteria.where("id").is(orderId)), Order.class)
                    .flatMap(order -> 
                        redisTemplate.opsForValue()
                            .set("order:" + orderId, order, Duration.ofHours(1))
                            .thenReturn(order)
                    )
            );
    }
    
    public Flux<Order> getAllOrders() {
        return template.select(Order.class).all();
    }
    
    public Flux<Order> getOrderStream() {
        return Flux.interval(Duration.ofSeconds(1))
            .flatMap(tick -> template.select(Order.class).all())
            .distinct(Order::getId);
    }
    
    public Mono<Order> updateOrderStatus(String orderId, String status) {
        return template.selectOne(Query.query(Criteria.where("id").is(orderId)), Order.class)
            .flatMap(order -> {
                order.setStatus(status);
                order.setUpdatedAt(LocalDateTime.now());
                return template.update(order);
            })
            .flatMap(updatedOrder -> 
                redisTemplate.opsForValue()
                    .set("order:" + orderId, updatedOrder, Duration.ofHours(1))
                    .thenReturn(updatedOrder)
            );
    }
}
```

## 8. Testing Reactive Applications

### Reactive Testing

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReactiveOrderServiceTest {
    
    @Autowired
    private ReactiveOrderService orderService;
    
    @Autowired
    private ReactiveOrderProducer producer;
    
    @Test
    void testCreateOrder() {
        // Given
        Order order = new Order();
        order.setCustomerId("customer-123");
        order.setProductId("product-456");
        order.setQuantity(2);
        order.setTotalAmount(BigDecimal.valueOf(99.99));
        
        // When & Then
        StepVerifier.create(orderService.createOrder(order))
            .assertNext(createdOrder -> {
                assertThat(createdOrder.getId()).isNotNull();
                assertThat(createdOrder.getStatus()).isEqualTo("PENDING");
                assertThat(createdOrder.getCreatedAt()).isNotNull();
            })
            .verifyComplete();
    }
    
    @Test
    void testOrderStream() {
        // When & Then
        StepVerifier.create(orderService.getOrderStream().take(3))
            .expectNextCount(3)
            .verifyComplete();
    }
    
    @Test
    void testReactiveProducer() {
        // Given
        Order order = new Order();
        order.setId("test-order-1");
        order.setCustomerId("customer-123");
        
        // When & Then
        StepVerifier.create(producer.sendOrder(order))
            .assertNext(result -> {
                assertThat(result.getRecordMetadata()).isNotNull();
                assertThat(result.getRecordMetadata().topic()).isEqualTo("order-events");
            })
            .verifyComplete();
    }
    
    @Test
    void testBackpressureHandling() {
        // Given
        Flux<Order> orders = Flux.range(1, 1000)
            .map(i -> {
                Order order = new Order();
                order.setId("order-" + i);
                order.setCustomerId("customer-" + i);
                return order;
            });
        
        // When & Then
        StepVerifier.create(
            producer.processOrdersWithBackpressure(orders)
                .onBackpressureBuffer(100)
                .take(100)
        )
        .expectNextCount(100)
        .verifyComplete();
    }
}
```

### WebSocket Testing

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Test
    void testWebSocketConnection() throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(
            Arrays.asList(new WebSocketTransport(new StandardWebSocketClient()))));
        
        CompletableFuture<String> future = new CompletableFuture<>();
        
        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                session.subscribe("/topic/orders", new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return String.class;
                    }
                    
                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        future.complete((String) payload);
                    }
                });
            }
        };
        
        StompSession session = stompClient.connect(
            "ws://localhost:" + port + "/orders", sessionHandler).get();
        
        // Send test message
        session.send("/app/orders", "test message");
        
        String result = future.get(10, TimeUnit.SECONDS);
        assertThat(result).isNotNull();
    }
}
```

## Summary

Today we covered:

1. **Reactive Messaging Fundamentals**: Project Reactor and reactive streams
2. **Reactive Kafka Integration**: Non-blocking Kafka producers and consumers
3. **Reactive RabbitMQ Integration**: Reactive messaging with RabbitMQ
4. **WebSocket Implementation**: Real-time bidirectional communication
5. **Server-Sent Events**: One-way real-time updates from server to client
6. **Reactive Stream Processing**: Complex stream processing pipelines
7. **Reactive Controllers**: Building reactive REST APIs
8. **Testing**: Comprehensive testing strategies for reactive applications

## Key Takeaways

- Reactive messaging provides non-blocking, scalable message processing
- Backpressure handling is crucial for reactive stream stability
- WebSockets enable real-time bidirectional communication
- Server-Sent Events provide efficient one-way real-time updates
- Reactive streams offer powerful composition and transformation capabilities
- Proper testing with StepVerifier ensures reactive application reliability

## Next Steps

Tomorrow we'll complete Week 11 with a comprehensive capstone project: building an Event-Driven Financial Trading System that integrates all the messaging and reactive patterns we've learned throughout the week.