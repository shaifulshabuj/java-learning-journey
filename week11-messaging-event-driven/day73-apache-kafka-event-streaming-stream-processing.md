# Day 73: Apache Kafka - Event Streaming and Stream Processing

## Overview

Today we'll explore Apache Kafka, a distributed streaming platform for building real-time data pipelines and streaming applications. We'll learn about Kafka's architecture, producers, consumers, stream processing with Kafka Streams, and integration with Spring Kafka.

## Learning Objectives

- Understand Apache Kafka architecture and core concepts
- Implement Kafka producers and consumers with Spring Kafka
- Build stream processing applications with Kafka Streams
- Handle partitioning, replication, and fault tolerance
- Implement exactly-once semantics and transaction support
- Monitor and operate Kafka clusters in production

## 1. Apache Kafka Fundamentals

### Core Concepts

**Topic**: A category or feed of messages
**Partition**: Ordered, immutable sequence of messages
**Producer**: Publishes messages to topics
**Consumer**: Subscribes to topics and processes messages
**Broker**: Kafka server that stores and serves messages
**Consumer Group**: Group of consumers sharing partition consumption

### Kafka Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Kafka Cluster                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  Broker 1          Broker 2          Broker 3                          │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐                   │
│  │ Topic A     │   │ Topic A     │   │ Topic A     │                   │
│  │ Partition 0 │   │ Partition 1 │   │ Partition 2 │                   │
│  └─────────────┘   └─────────────┘   └─────────────┘                   │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐                   │
│  │ Topic B     │   │ Topic B     │   │ Topic B     │                   │
│  │ Partition 0 │   │ Partition 1 │   │ Partition 0 │                   │
│  └─────────────┘   └─────────────┘   └─────────────┘                   │
└─────────────────────────────────────────────────────────────────────────┘
```

### Dependencies

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-streams</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
</dependency>
<dependency>
    <groupId>io.confluent</groupId>
    <artifactId>kafka-avro-serializer</artifactId>
</dependency>
```

## 2. Kafka Configuration

### Spring Kafka Configuration

```java
@Configuration
@EnableKafka
public class KafkaConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    // Producer Configuration
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Performance and reliability settings
        configs.put(ProducerConfig.ACKS_CONFIG, "all");
        configs.put(ProducerConfig.RETRIES_CONFIG, 3);
        configs.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configs.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        configs.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        configs.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        
        // Idempotence and transactions
        configs.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configs.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "order-producer");
        
        return new DefaultKafkaProducerFactory<>(configs);
    }
    
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
    
    // Consumer Configuration
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put(ConsumerConfig.GROUP_ID_CONFIG, "order-consumer-group");
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        
        // Consumer settings
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        configs.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        configs.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        configs.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);
        
        // Isolation level for transactions
        configs.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        
        return new DefaultKafkaConsumerFactory<>(configs);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setErrorHandler(new SeekToCurrentErrorHandler());
        return factory;
    }
    
    // Admin Configuration
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }
    
    @Bean
    public NewTopic orderTopic() {
        return TopicBuilder.name("order-events")
            .partitions(3)
            .replicas(1)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "1")
            .config(TopicConfig.RETENTION_MS_CONFIG, "604800000") // 7 days
            .build();
    }
    
    @Bean
    public NewTopic inventoryTopic() {
        return TopicBuilder.name("inventory-events")
            .partitions(3)
            .replicas(1)
            .build();
    }
    
    @Bean
    public NewTopic paymentTopic() {
        return TopicBuilder.name("payment-events")
            .partitions(3)
            .replicas(1)
            .build();
    }
}
```

### Application Properties

```yaml
# application.yml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
      batch-size: 16384
      linger-ms: 1
      buffer-memory: 33554432
      compression-type: snappy
    consumer:
      group-id: order-service-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset: earliest
      enable-auto-commit: false
      max-poll-records: 500
      properties:
        spring.json.trusted.packages: "*"
```

## 3. Kafka Producers

### Order Event Producer

```java
@Component
public class OrderEventProducer {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    public OrderEventProducer(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }
    
    public void publishOrderCreated(OrderCreatedEvent event) {
        try {
            String key = event.getOrderId().toString();
            
            kafkaTemplate.send("order-events", key, event)
                .addCallback(
                    result -> System.out.println("Order created event sent: " + key),
                    failure -> System.err.println("Failed to send order created event: " + failure.getMessage())
                );
            
        } catch (Exception e) {
            System.err.println("Error publishing order created event: " + e.getMessage());
            throw new EventPublishingException("Failed to publish order created event", e);
        }
    }
    
    public void publishOrderStatusChanged(OrderStatusChangedEvent event) {
        try {
            String key = event.getOrderId().toString();
            
            ProducerRecord<String, Object> record = new ProducerRecord<>("order-events", key, event);
            
            // Add headers
            record.headers().add("eventType", "ORDER_STATUS_CHANGED".getBytes());
            record.headers().add("timestamp", String.valueOf(System.currentTimeMillis()).getBytes());
            record.headers().add("version", "1.0".getBytes());
            
            kafkaTemplate.send(record);
            
        } catch (Exception e) {
            System.err.println("Error publishing order status changed event: " + e.getMessage());
            throw new EventPublishingException("Failed to publish order status changed event", e);
        }
    }
    
    @Transactional
    public void publishOrderEventsTransaction(List<OrderEvent> events) {
        try {
            kafkaTemplate.executeInTransaction(template -> {
                for (OrderEvent event : events) {
                    String key = event.getOrderId().toString();
                    template.send("order-events", key, event);
                }
                return null;
            });
            
            System.out.println("Published " + events.size() + " order events in transaction");
            
        } catch (Exception e) {
            System.err.println("Error publishing order events in transaction: " + e.getMessage());
            throw new EventPublishingException("Failed to publish order events in transaction", e);
        }
    }
}
```

### Batch Producer

```java
@Component
public class BatchOrderEventProducer {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final List<ProducerRecord<String, Object>> recordBatch;
    private final int batchSize;
    
    public BatchOrderEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.recordBatch = new ArrayList<>();
        this.batchSize = 100;
    }
    
    public synchronized void addEvent(OrderEvent event) {
        String key = event.getOrderId().toString();
        ProducerRecord<String, Object> record = new ProducerRecord<>("order-events", key, event);
        
        recordBatch.add(record);
        
        if (recordBatch.size() >= batchSize) {
            sendBatch();
        }
    }
    
    public synchronized void sendBatch() {
        if (recordBatch.isEmpty()) {
            return;
        }
        
        try {
            List<Future<SendResult<String, Object>>> futures = new ArrayList<>();
            
            for (ProducerRecord<String, Object> record : recordBatch) {
                Future<SendResult<String, Object>> future = kafkaTemplate.send(record);
                futures.add(future);
            }
            
            // Wait for all sends to complete
            for (Future<SendResult<String, Object>> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
            
            System.out.println("Batch of " + recordBatch.size() + " events sent successfully");
            recordBatch.clear();
            
        } catch (Exception e) {
            System.err.println("Error sending batch: " + e.getMessage());
            throw new EventPublishingException("Failed to send batch", e);
        }
    }
    
    @Scheduled(fixedDelay = 5000)
    public void flushBatch() {
        sendBatch();
    }
}
```

## 4. Kafka Consumers

### Order Event Consumer

```java
@Component
public class OrderEventConsumer {
    
    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;
    
    public OrderEventConsumer(OrderService orderService,
                             InventoryService inventoryService,
                             NotificationService notificationService) {
        this.orderService = orderService;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
    }
    
    @KafkaListener(topics = "order-events", groupId = "order-processing-group")
    public void handleOrderEvent(ConsumerRecord<String, OrderEvent> record, Acknowledgment ack) {
        try {
            OrderEvent event = record.value();
            String eventType = new String(record.headers().lastHeader("eventType").value());
            
            System.out.println("Received order event: " + eventType + " for order: " + event.getOrderId());
            
            switch (eventType) {
                case "ORDER_CREATED":
                    handleOrderCreated((OrderCreatedEvent) event);
                    break;
                case "ORDER_STATUS_CHANGED":
                    handleOrderStatusChanged((OrderStatusChangedEvent) event);
                    break;
                case "ORDER_CANCELLED":
                    handleOrderCancelled((OrderCancelledEvent) event);
                    break;
                default:
                    System.out.println("Unknown event type: " + eventType);
            }
            
            ack.acknowledge();
            
        } catch (Exception e) {
            System.err.println("Error processing order event: " + e.getMessage());
            // Don't acknowledge - will be retried
        }
    }
    
    @KafkaListener(topics = "inventory-events", groupId = "inventory-processing-group")
    public void handleInventoryEvent(ConsumerRecord<String, InventoryEvent> record, Acknowledgment ack) {
        try {
            InventoryEvent event = record.value();
            
            System.out.println("Received inventory event for product: " + event.getProductId());
            
            // Process inventory event
            inventoryService.handleInventoryEvent(event);
            
            ack.acknowledge();
            
        } catch (Exception e) {
            System.err.println("Error processing inventory event: " + e.getMessage());
        }
    }
    
    @KafkaListener(topics = "payment-events", groupId = "payment-processing-group")
    public void handlePaymentEvent(ConsumerRecord<String, PaymentEvent> record, Acknowledgment ack) {
        try {
            PaymentEvent event = record.value();
            
            System.out.println("Received payment event for order: " + event.getOrderId());
            
            // Process payment event
            orderService.handlePaymentEvent(event);
            
            ack.acknowledge();
            
        } catch (Exception e) {
            System.err.println("Error processing payment event: " + e.getMessage());
        }
    }
    
    private void handleOrderCreated(OrderCreatedEvent event) {
        // Send notification
        notificationService.sendOrderConfirmation(event);
        
        // Update analytics
        orderService.updateOrderAnalytics(event);
    }
    
    private void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        // Send status update notification
        notificationService.sendOrderStatusUpdate(event);
        
        // Update order tracking
        orderService.updateOrderTracking(event);
    }
    
    private void handleOrderCancelled(OrderCancelledEvent event) {
        // Release inventory
        inventoryService.releaseInventory(event.getOrderId());
        
        // Process refund
        orderService.processRefund(event);
        
        // Send cancellation notification
        notificationService.sendOrderCancellation(event);
    }
}
```

### Batch Consumer

```java
@Component
public class BatchOrderEventConsumer {
    
    private final OrderService orderService;
    
    public BatchOrderEventConsumer(OrderService orderService) {
        this.orderService = orderService;
    }
    
    @KafkaListener(topics = "order-events", groupId = "batch-processing-group")
    public void handleOrderEventBatch(List<ConsumerRecord<String, OrderEvent>> records, Acknowledgment ack) {
        try {
            System.out.println("Processing batch of " + records.size() + " order events");
            
            List<OrderEvent> events = records.stream()
                .map(ConsumerRecord::value)
                .collect(Collectors.toList());
            
            // Process events in batch
            orderService.processOrderEventBatch(events);
            
            ack.acknowledge();
            
        } catch (Exception e) {
            System.err.println("Error processing order event batch: " + e.getMessage());
            // Don't acknowledge - will be retried
        }
    }
}
```

## 5. Kafka Streams

### Stream Processing Configuration

```java
@Configuration
@EnableKafkaStreams
public class KafkaStreamsConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kafkaStreamsConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "order-stream-processing");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);
        
        return new KafkaStreamsConfiguration(props);
    }
    
    @Bean
    public Serde<OrderEvent> orderEventSerde() {
        return Serdes.serdeFrom(new JsonSerializer<>(), new JsonDeserializer<>(OrderEvent.class));
    }
    
    @Bean
    public Serde<OrderAnalytics> orderAnalyticsSerde() {
        return Serdes.serdeFrom(new JsonSerializer<>(), new JsonDeserializer<>(OrderAnalytics.class));
    }
}
```

### Stream Processing Topology

```java
@Component
public class OrderStreamProcessor {
    
    private final Serde<OrderEvent> orderEventSerde;
    private final Serde<OrderAnalytics> orderAnalyticsSerde;
    
    public OrderStreamProcessor(Serde<OrderEvent> orderEventSerde,
                               Serde<OrderAnalytics> orderAnalyticsSerde) {
        this.orderEventSerde = orderEventSerde;
        this.orderAnalyticsSerde = orderAnalyticsSerde;
    }
    
    @Bean
    public KStream<String, OrderEvent> orderEventStream(StreamsBuilder streamsBuilder) {
        KStream<String, OrderEvent> orderStream = streamsBuilder
            .stream("order-events", Consumed.with(Serdes.String(), orderEventSerde));
        
        // Filter only order created events
        KStream<String, OrderEvent> orderCreatedStream = orderStream
            .filter((key, event) -> event instanceof OrderCreatedEvent);
        
        // Transform and aggregate
        processOrderAnalytics(orderCreatedStream);
        
        // Route to different topics based on order value
        routeOrdersByValue(orderCreatedStream);
        
        // Detect fraud patterns
        detectFraudPatterns(orderCreatedStream);
        
        return orderStream;
    }
    
    private void processOrderAnalytics(KStream<String, OrderEvent> orderStream) {
        KGroupedStream<String, OrderEvent> groupedStream = orderStream
            .groupByKey(Grouped.with(Serdes.String(), orderEventSerde));
        
        // Count orders by customer
        KTable<String, Long> orderCountByCustomer = groupedStream.count();
        
        // Calculate total revenue by customer
        KTable<String, Double> revenueByCustomer = groupedStream
            .aggregate(
                () -> 0.0,
                (key, event, aggregate) -> {
                    if (event instanceof OrderCreatedEvent) {
                        OrderCreatedEvent createdEvent = (OrderCreatedEvent) event;
                        return aggregate + createdEvent.getTotalAmount().doubleValue();
                    }
                    return aggregate;
                },
                Materialized.with(Serdes.String(), Serdes.Double())
            );
        
        // Time-windowed aggregations
        KTable<Windowed<String>, Long> orderCountByWindow = groupedStream
            .windowedBy(TimeWindows.of(Duration.ofMinutes(15)))
            .count();
        
        // Send aggregated results to output topics
        orderCountByCustomer.toStream().to("order-count-by-customer");
        revenueByCustomer.toStream().to("revenue-by-customer");
    }
    
    private void routeOrdersByValue(KStream<String, OrderEvent> orderStream) {
        KStream<String, OrderEvent>[] branches = orderStream.branch(
            (key, event) -> {
                if (event instanceof OrderCreatedEvent) {
                    OrderCreatedEvent createdEvent = (OrderCreatedEvent) event;
                    return createdEvent.getTotalAmount().doubleValue() > 1000.0;
                }
                return false;
            },
            (key, event) -> true // Default branch
        );
        
        // High-value orders
        branches[0].to("high-value-orders");
        
        // Normal orders
        branches[1].to("normal-orders");
    }
    
    private void detectFraudPatterns(KStream<String, OrderEvent> orderStream) {
        KStream<String, OrderEvent> fraudStream = orderStream
            .groupByKey(Grouped.with(Serdes.String(), orderEventSerde))
            .windowedBy(TimeWindows.of(Duration.ofMinutes(5)))
            .aggregate(
                () -> new FraudDetection(),
                (key, event, aggregate) -> {
                    if (event instanceof OrderCreatedEvent) {
                        OrderCreatedEvent createdEvent = (OrderCreatedEvent) event;
                        aggregate.addOrder(createdEvent);
                    }
                    return aggregate;
                },
                Materialized.with(Serdes.String(), 
                    Serdes.serdeFrom(new JsonSerializer<>(), new JsonDeserializer<>(FraudDetection.class)))
            )
            .toStream()
            .filter((windowedKey, fraudDetection) -> fraudDetection.isFraudulent())
            .map((windowedKey, fraudDetection) -> KeyValue.pair(
                windowedKey.key(),
                new FraudAlert(windowedKey.key(), fraudDetection.getReason())
            ));
        
        fraudStream.to("fraud-alerts");
    }
}
```

### Interactive Queries

```java
@RestController
@RequestMapping("/api/analytics")
public class OrderAnalyticsController {
    
    private final KafkaStreams kafkaStreams;
    
    public OrderAnalyticsController(KafkaStreams kafkaStreams) {
        this.kafkaStreams = kafkaStreams;
    }
    
    @GetMapping("/orders/count/{customerId}")
    public ResponseEntity<Long> getOrderCount(@PathVariable String customerId) {
        ReadOnlyKeyValueStore<String, Long> store = kafkaStreams.store(
            "order-count-by-customer",
            QueryableStoreTypes.keyValueStore()
        );
        
        Long count = store.get(customerId);
        return ResponseEntity.ok(count != null ? count : 0L);
    }
    
    @GetMapping("/revenue/{customerId}")
    public ResponseEntity<Double> getRevenue(@PathVariable String customerId) {
        ReadOnlyKeyValueStore<String, Double> store = kafkaStreams.store(
            "revenue-by-customer",
            QueryableStoreTypes.keyValueStore()
        );
        
        Double revenue = store.get(customerId);
        return ResponseEntity.ok(revenue != null ? revenue : 0.0);
    }
    
    @GetMapping("/orders/recent")
    public ResponseEntity<List<OrderAnalytics>> getRecentOrderAnalytics() {
        ReadOnlyWindowStore<String, Long> store = kafkaStreams.store(
            "order-count-by-window",
            QueryableStoreTypes.windowStore()
        );
        
        List<OrderAnalytics> analytics = new ArrayList<>();
        Instant now = Instant.now();
        Instant oneHourAgo = now.minus(Duration.ofHours(1));
        
        try (KeyValueIterator<Windowed<String>, Long> iterator = 
             store.fetchAll(oneHourAgo, now)) {
            
            while (iterator.hasNext()) {
                KeyValue<Windowed<String>, Long> record = iterator.next();
                Windowed<String> windowedKey = record.key;
                Long count = record.value;
                
                OrderAnalytics analytic = new OrderAnalytics();
                analytic.setCustomerId(windowedKey.key());
                analytic.setWindowStart(windowedKey.window().start());
                analytic.setWindowEnd(windowedKey.window().end());
                analytic.setOrderCount(count);
                
                analytics.add(analytic);
            }
        }
        
        return ResponseEntity.ok(analytics);
    }
}
```

## 6. Exactly-Once Semantics

### Transactional Producer

```java
@Component
public class TransactionalOrderProducer {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderRepository orderRepository;
    
    public TransactionalOrderProducer(KafkaTemplate<String, Object> kafkaTemplate,
                                     OrderRepository orderRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderRepository = orderRepository;
    }
    
    @Transactional
    public void processOrderWithExactlyOnce(OrderCreatedEvent event) {
        try {
            // Save to database
            Order order = convertToOrder(event);
            Order savedOrder = orderRepository.save(order);
            
            // Send to Kafka (part of the same transaction)
            kafkaTemplate.executeInTransaction(template -> {
                template.send("order-events", savedOrder.getId().toString(), event);
                template.send("order-analytics", savedOrder.getCustomerId().toString(), 
                    createAnalyticsEvent(savedOrder));
                return null;
            });
            
            System.out.println("Order processed with exactly-once semantics: " + savedOrder.getId());
            
        } catch (Exception e) {
            System.err.println("Error processing order with exactly-once: " + e.getMessage());
            throw new OrderProcessingException("Failed to process order with exactly-once semantics", e);
        }
    }
    
    private Order convertToOrder(OrderCreatedEvent event) {
        Order order = new Order();
        order.setId(event.getOrderId());
        order.setCustomerId(event.getCustomerId());
        order.setProductId(event.getProductId());
        order.setQuantity(event.getQuantity());
        order.setTotalAmount(event.getTotalAmount());
        order.setStatus("CREATED");
        order.setCreatedAt(event.getCreatedAt());
        return order;
    }
    
    private OrderAnalyticsEvent createAnalyticsEvent(Order order) {
        OrderAnalyticsEvent analyticsEvent = new OrderAnalyticsEvent();
        analyticsEvent.setCustomerId(order.getCustomerId());
        analyticsEvent.setOrderValue(order.getTotalAmount());
        analyticsEvent.setTimestamp(LocalDateTime.now());
        return analyticsEvent;
    }
}
```

### Idempotent Consumer

```java
@Component
public class IdempotentOrderConsumer {
    
    private final OrderService orderService;
    private final ProcessedMessageRepository processedMessageRepository;
    
    public IdempotentOrderConsumer(OrderService orderService,
                                  ProcessedMessageRepository processedMessageRepository) {
        this.orderService = orderService;
        this.processedMessageRepository = processedMessageRepository;
    }
    
    @KafkaListener(topics = "order-events", groupId = "idempotent-processing-group")
    public void handleOrderEventIdempotent(ConsumerRecord<String, OrderEvent> record, Acknowledgment ack) {
        try {
            String messageId = generateMessageId(record);
            
            // Check if message already processed
            if (processedMessageRepository.existsByMessageId(messageId)) {
                System.out.println("Message already processed: " + messageId);
                ack.acknowledge();
                return;
            }
            
            // Process the message
            OrderEvent event = record.value();
            orderService.processOrderEvent(event);
            
            // Mark as processed
            ProcessedMessage processedMessage = new ProcessedMessage();
            processedMessage.setMessageId(messageId);
            processedMessage.setTopic(record.topic());
            processedMessage.setPartition(record.partition());
            processedMessage.setOffset(record.offset());
            processedMessage.setProcessedAt(LocalDateTime.now());
            
            processedMessageRepository.save(processedMessage);
            
            ack.acknowledge();
            
            System.out.println("Message processed idempotently: " + messageId);
            
        } catch (Exception e) {
            System.err.println("Error processing message idempotently: " + e.getMessage());
            // Don't acknowledge - will be retried
        }
    }
    
    private String generateMessageId(ConsumerRecord<String, OrderEvent> record) {
        return record.topic() + "-" + record.partition() + "-" + record.offset();
    }
}
```

## 7. Kafka Connect Integration

### Custom Source Connector

```java
@Component
public class DatabaseSourceConnector extends SourceConnector {
    
    private Map<String, String> configProps;
    
    @Override
    public String version() {
        return "1.0.0";
    }
    
    @Override
    public void start(Map<String, String> props) {
        this.configProps = props;
    }
    
    @Override
    public Class<? extends Task> taskClass() {
        return DatabaseSourceTask.class;
    }
    
    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        List<Map<String, String>> configs = new ArrayList<>();
        
        for (int i = 0; i < maxTasks; i++) {
            Map<String, String> config = new HashMap<>(configProps);
            config.put("task.id", String.valueOf(i));
            configs.add(config);
        }
        
        return configs;
    }
    
    @Override
    public void stop() {
        // Cleanup resources
    }
    
    @Override
    public ConfigDef config() {
        return new ConfigDef()
            .define("connection.url", ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, "Database URL")
            .define("connection.user", ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, "Database user")
            .define("connection.password", ConfigDef.Type.PASSWORD, ConfigDef.Importance.HIGH, "Database password")
            .define("table.name", ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, "Table name")
            .define("poll.interval.ms", ConfigDef.Type.INT, 5000, ConfigDef.Importance.LOW, "Poll interval");
    }
}
```

### Source Task Implementation

```java
public class DatabaseSourceTask extends SourceTask {
    
    private DataSource dataSource;
    private String tableName;
    private long pollInterval;
    private long lastOffset;
    
    @Override
    public String version() {
        return "1.0.0";
    }
    
    @Override
    public void start(Map<String, String> props) {
        String url = props.get("connection.url");
        String user = props.get("connection.user");
        String password = props.get("connection.password");
        this.tableName = props.get("table.name");
        this.pollInterval = Long.parseLong(props.get("poll.interval.ms"));
        
        // Initialize data source
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setJdbcUrl(url);
        hikariDataSource.setUsername(user);
        hikariDataSource.setPassword(password);
        this.dataSource = hikariDataSource;
        
        // Get last offset from Kafka Connect
        Map<String, Object> offset = context.offsetStorageReader().offset(
            Collections.singletonMap("table", tableName));
        this.lastOffset = offset != null ? (Long) offset.get("offset") : 0;
    }
    
    @Override
    public List<SourceRecord> poll() throws InterruptedException {
        List<SourceRecord> records = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                 "SELECT * FROM " + tableName + " WHERE id > ? ORDER BY id LIMIT 100")) {
            
            stmt.setLong(1, lastOffset);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                long id = rs.getLong("id");
                String data = rs.getString("data");
                
                Map<String, Object> sourcePartition = Collections.singletonMap("table", tableName);
                Map<String, Object> sourceOffset = Collections.singletonMap("offset", id);
                
                SourceRecord record = new SourceRecord(
                    sourcePartition,
                    sourceOffset,
                    "database-events",
                    Schema.STRING_SCHEMA,
                    String.valueOf(id),
                    Schema.STRING_SCHEMA,
                    data
                );
                
                records.add(record);
                lastOffset = id;
            }
            
        } catch (SQLException e) {
            throw new ConnectException("Error polling database", e);
        }
        
        if (records.isEmpty()) {
            Thread.sleep(pollInterval);
        }
        
        return records;
    }
    
    @Override
    public void stop() {
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
        }
    }
}
```

## 8. Error Handling and Dead Letter Topics

### Error Handling Configuration

```java
@Configuration
public class KafkaErrorHandlingConfig {
    
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaTemplate<String, Object> template) {
        return new DeadLetterPublishingRecoverer(template, 
            (record, exception) -> {
                String originalTopic = record.topic();
                return new TopicPartition(originalTopic + ".DLT", record.partition());
            });
    }
    
    @Bean
    public ErrorHandler kafkaErrorHandler(DeadLetterPublishingRecoverer recoverer) {
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            ErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
```

### Dead Letter Topic Handler

```java
@Component
public class DeadLetterTopicHandler {
    
    private final OrderService orderService;
    private final NotificationService notificationService;
    
    public DeadLetterTopicHandler(OrderService orderService, NotificationService notificationService) {
        this.orderService = orderService;
        this.notificationService = notificationService;
    }
    
    @KafkaListener(topics = "order-events.DLT", groupId = "dead-letter-handler-group")
    public void handleDeadLetterMessage(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        try {
            System.out.println("Processing dead letter message from topic: " + record.topic());
            
            // Extract original exception information
            Header exceptionHeader = record.headers().lastHeader("kafka_dlt-exception-message");
            String exceptionMessage = exceptionHeader != null ? 
                new String(exceptionHeader.value()) : "Unknown error";
            
            // Log the failed message
            logFailedMessage(record, exceptionMessage);
            
            // Attempt to fix and reprocess
            if (canReprocess(record, exceptionMessage)) {
                reprocessMessage(record);
            } else {
                // Send to manual review
                sendToManualReview(record, exceptionMessage);
            }
            
            ack.acknowledge();
            
        } catch (Exception e) {
            System.err.println("Error handling dead letter message: " + e.getMessage());
        }
    }
    
    private void logFailedMessage(ConsumerRecord<String, Object> record, String exceptionMessage) {
        System.err.println("Failed message details:");
        System.err.println("Topic: " + record.topic());
        System.err.println("Partition: " + record.partition());
        System.err.println("Offset: " + record.offset());
        System.err.println("Key: " + record.key());
        System.err.println("Value: " + record.value());
        System.err.println("Exception: " + exceptionMessage);
    }
    
    private boolean canReprocess(ConsumerRecord<String, Object> record, String exceptionMessage) {
        // Implement logic to determine if message can be reprocessed
        return !exceptionMessage.contains("ValidationException");
    }
    
    private void reprocessMessage(ConsumerRecord<String, Object> record) {
        // Attempt to fix the message and reprocess
        System.out.println("Attempting to reprocess message: " + record.key());
        
        // Send corrected message back to original topic
        // This would involve the KafkaTemplate to send to original topic
    }
    
    private void sendToManualReview(ConsumerRecord<String, Object> record, String exceptionMessage) {
        // Send notification for manual review
        notificationService.sendFailedMessageAlert(record, exceptionMessage);
    }
}
```

## 9. Monitoring and Metrics

### Kafka Metrics Configuration

```java
@Configuration
public class KafkaMetricsConfig {
    
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> kafkaMetricsCustomizer() {
        return registry -> registry.config().commonTags("application", "kafka-order-service");
    }
    
    @Bean
    public KafkaMetrics kafkaMetrics(MeterRegistry meterRegistry) {
        return new KafkaMetrics(meterRegistry);
    }
}

@Component
public class KafkaMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter messagesProduced;
    private final Counter messagesConsumed;
    private final Counter messagesFailed;
    private final Timer messageProcessingTime;
    
    public KafkaMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.messagesProduced = Counter.builder("kafka.messages.produced")
            .description("Number of messages produced")
            .register(meterRegistry);
        this.messagesConsumed = Counter.builder("kafka.messages.consumed")
            .description("Number of messages consumed")
            .register(meterRegistry);
        this.messagesFailed = Counter.builder("kafka.messages.failed")
            .description("Number of failed messages")
            .register(meterRegistry);
        this.messageProcessingTime = Timer.builder("kafka.message.processing.time")
            .description("Message processing time")
            .register(meterRegistry);
    }
    
    public void recordMessageProduced(String topic) {
        messagesProduced.increment(Tags.of("topic", topic));
    }
    
    public void recordMessageConsumed(String topic) {
        messagesConsumed.increment(Tags.of("topic", topic));
    }
    
    public void recordMessageFailed(String topic, String errorType) {
        messagesFailed.increment(Tags.of("topic", topic, "error.type", errorType));
    }
    
    public Timer.Sample startProcessingTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordProcessingTime(Timer.Sample sample, String topic) {
        sample.stop(Timer.builder("kafka.message.processing.time")
            .tag("topic", topic)
            .register(meterRegistry));
    }
}
```

### Health Check

```java
@Component
public class KafkaHealthIndicator implements HealthIndicator {
    
    private final KafkaAdmin kafkaAdmin;
    private final KafkaStreams kafkaStreams;
    
    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin, KafkaStreams kafkaStreams) {
        this.kafkaAdmin = kafkaAdmin;
        this.kafkaStreams = kafkaStreams;
    }
    
    @Override
    public Health health() {
        try {
            // Check Kafka cluster connectivity
            AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());
            DescribeClusterResult clusterResult = adminClient.describeCluster();
            clusterResult.clusterId().get(5, TimeUnit.SECONDS);
            
            Health.Builder builder = Health.up();
            builder.withDetail("kafka.cluster", "Available");
            
            // Check Kafka Streams state
            if (kafkaStreams != null) {
                KafkaStreams.State state = kafkaStreams.state();
                builder.withDetail("kafka.streams.state", state.name());
                
                if (state == KafkaStreams.State.ERROR) {
                    builder.down();
                }
            }
            
            return builder.build();
            
        } catch (Exception e) {
            return Health.down()
                .withDetail("kafka.error", e.getMessage())
                .build();
        }
    }
}
```

## 10. Testing Kafka Applications

### Integration Testing

```java
@SpringBootTest
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
@TestPropertySource(properties = {"spring.kafka.bootstrap-servers=localhost:9092"})
class KafkaIntegrationTest {
    
    @Autowired
    private OrderEventProducer orderEventProducer;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Autowired
    private OrderService orderService;
    
    @Test
    void testOrderEventProducerAndConsumer() throws InterruptedException {
        // Given
        OrderCreatedEvent event = new OrderCreatedEvent(
            1L, 123L, 456L, 2, BigDecimal.valueOf(99.99), 
            "CREATED", LocalDateTime.now()
        );
        
        // When
        orderEventProducer.publishOrderCreated(event);
        
        // Then
        Thread.sleep(2000); // Wait for message processing
        
        // Verify the message was processed
        verify(orderService, times(1)).processOrderEvent(any(OrderCreatedEvent.class));
    }
    
    @Test
    void testKafkaStreamsProcessing() {
        // Given
        OrderCreatedEvent event = new OrderCreatedEvent(
            2L, 123L, 456L, 1, BigDecimal.valueOf(1500.0), 
            "CREATED", LocalDateTime.now()
        );
        
        // When
        kafkaTemplate.send("order-events", "2", event);
        
        // Then
        // Verify the message was routed to high-value orders topic
        ConsumerRecord<String, Object> record = KafkaTestUtils.getSingleRecord(
            consumer, "high-value-orders", 5000);
        
        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo("2");
    }
}
```

### Unit Testing

```java
@ExtendWith(MockitoExtension.class)
class OrderEventProducerTest {
    
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @InjectMocks
    private OrderEventProducer orderEventProducer;
    
    @Test
    void testPublishOrderCreated() {
        // Given
        OrderCreatedEvent event = new OrderCreatedEvent(
            1L, 123L, 456L, 2, BigDecimal.valueOf(99.99), 
            "CREATED", LocalDateTime.now()
        );
        
        when(kafkaTemplate.send(anyString(), anyString(), any()))
            .thenReturn(mock(ListenableFuture.class));
        
        // When
        orderEventProducer.publishOrderCreated(event);
        
        // Then
        verify(kafkaTemplate).send("order-events", "1", event);
    }
}
```

## Summary

Today we covered:

1. **Kafka Fundamentals**: Architecture, core concepts, and distributed streaming platform
2. **Configuration**: Spring Kafka configuration and topic management
3. **Producers**: Event publishing with reliability and performance optimizations
4. **Consumers**: Event consumption with acknowledgment and error handling
5. **Kafka Streams**: Stream processing, aggregations, and real-time analytics
6. **Exactly-Once Semantics**: Transactional producers and idempotent consumers
7. **Kafka Connect**: Custom connectors for database integration
8. **Error Handling**: Dead letter topics and recovery mechanisms
9. **Monitoring**: Metrics, health checks, and operational visibility
10. **Testing**: Integration and unit testing strategies

## Key Takeaways

- Kafka provides high-throughput, fault-tolerant event streaming
- Partitioning enables horizontal scaling and parallel processing
- Kafka Streams enables real-time stream processing and analytics
- Exactly-once semantics ensure data consistency in distributed systems
- Proper error handling and monitoring are essential for production deployments
- Testing requires embedded Kafka for integration testing

## Next Steps

Tomorrow we'll explore Event Sourcing and CQRS implementation, learning how to build systems that capture all changes as events and separate read and write operations for optimal performance and scalability.