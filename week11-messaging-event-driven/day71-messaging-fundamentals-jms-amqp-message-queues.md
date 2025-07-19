# Day 71: Messaging Fundamentals - JMS, AMQP, Message Queues

## Overview

Today we'll explore the fundamentals of enterprise messaging systems. We'll cover Java Message Service (JMS), Advanced Message Queuing Protocol (AMQP), and various message queue patterns essential for building distributed, decoupled systems.

## Learning Objectives

- Understand messaging patterns and enterprise integration
- Master Java Message Service (JMS) fundamentals
- Explore AMQP protocol and its advantages
- Implement point-to-point and publish-subscribe messaging
- Handle message persistence, transactions, and error handling
- Build reliable message-driven applications

## 1. Enterprise Messaging Fundamentals

### Core Concepts

**Message**: A unit of data sent between applications
**Message Queue**: A communication method for sending messages between services
**Producer**: Application that sends messages
**Consumer**: Application that receives messages
**Broker**: Middleware that routes messages between producers and consumers

### Messaging Patterns

**Point-to-Point**: One producer sends to one consumer via a queue
**Publish-Subscribe**: One producer sends to multiple consumers via a topic
**Request-Reply**: Synchronous communication pattern
**Message Routing**: Route messages based on content or rules

### Benefits of Messaging

- **Decoupling**: Services don't need to know about each other directly
- **Scalability**: Handle varying loads through queuing
- **Reliability**: Messages can be persisted and guaranteed delivery
- **Flexibility**: Easy to add new consumers or producers

## 2. Java Message Service (JMS) Overview

### JMS Architecture

```java
// JMS Connection Factory
ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");

// JMS Connection
Connection connection = connectionFactory.createConnection();
connection.start();

// JMS Session
Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

// JMS Destination (Queue or Topic)
Queue queue = session.createQueue("order.queue");
Topic topic = session.createTopic("order.topic");

// JMS Message Producer
MessageProducer producer = session.createProducer(queue);

// JMS Message Consumer
MessageConsumer consumer = session.createConsumer(queue);
```

### JMS Message Types

```java
// Text Message
TextMessage textMessage = session.createTextMessage("Hello JMS");

// Object Message
Order order = new Order(1L, "Customer A", BigDecimal.valueOf(100.50));
ObjectMessage objectMessage = session.createObjectMessage(order);

// Map Message
MapMessage mapMessage = session.createMapMessage();
mapMessage.setString("orderId", "12345");
mapMessage.setDouble("amount", 99.99);

// Bytes Message
BytesMessage bytesMessage = session.createBytesMessage();
bytesMessage.writeBytes("Binary data".getBytes());

// Stream Message
StreamMessage streamMessage = session.createStreamMessage();
streamMessage.writeString("Stream data");
streamMessage.writeInt(42);
```

## 3. Spring JMS Configuration

### Dependencies

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-jms</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.activemq</groupId>
    <artifactId>activemq-spring</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-activemq</artifactId>
</dependency>
```

### JMS Configuration

```java
@Configuration
@EnableJms
public class JmsConfig {
    
    @Bean
    public ConnectionFactory connectionFactory() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
        connectionFactory.setBrokerURL("tcp://localhost:61616");
        connectionFactory.setUserName("admin");
        connectionFactory.setPassword("admin");
        
        // Connection pooling
        PooledConnectionFactory pooledFactory = new PooledConnectionFactory();
        pooledFactory.setConnectionFactory(connectionFactory);
        pooledFactory.setMaxConnections(10);
        pooledFactory.setIdleTimeout(30000);
        
        return pooledFactory;
    }
    
    @Bean
    public JmsTemplate jmsTemplate() {
        JmsTemplate template = new JmsTemplate();
        template.setConnectionFactory(connectionFactory());
        template.setDefaultDestinationName("order.queue");
        template.setReceiveTimeout(5000);
        template.setSessionTransacted(true);
        return template;
    }
    
    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory() {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory());
        factory.setConcurrency("1-5");
        factory.setSessionTransacted(true);
        factory.setAutoStartup(true);
        factory.setErrorHandler(new MessageErrorHandler());
        return factory;
    }
    
    @Bean
    public Queue orderQueue() {
        return new ActiveMQQueue("order.queue");
    }
    
    @Bean
    public Topic orderTopic() {
        return new ActiveMQTopic("order.topic");
    }
}
```

## 4. Message Producer Implementation

### Order Message Producer

```java
@Component
public class OrderMessageProducer {
    
    private final JmsTemplate jmsTemplate;
    
    public OrderMessageProducer(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }
    
    public void sendOrder(Order order) {
        try {
            jmsTemplate.convertAndSend("order.queue", order, message -> {
                message.setStringProperty("orderId", order.getId().toString());
                message.setStringProperty("customerId", order.getCustomerId().toString());
                message.setStringProperty("priority", order.getPriority());
                message.setLongProperty("timestamp", System.currentTimeMillis());
                return message;
            });
            
            System.out.println("Order sent to queue: " + order.getId());
        } catch (Exception e) {
            System.err.println("Failed to send order: " + e.getMessage());
            throw new MessageSendException("Failed to send order message", e);
        }
    }
    
    public void sendOrderWithDelay(Order order, long delayMs) {
        try {
            jmsTemplate.convertAndSend("order.queue", order, message -> {
                message.setStringProperty("orderId", order.getId().toString());
                message.setLongProperty("_AMQ_SCHED_DELAY", delayMs);
                return message;
            });
            
            System.out.println("Delayed order sent: " + order.getId() + " (delay: " + delayMs + "ms)");
        } catch (Exception e) {
            System.err.println("Failed to send delayed order: " + e.getMessage());
            throw new MessageSendException("Failed to send delayed order message", e);
        }
    }
    
    public void sendOrderToTopic(Order order) {
        try {
            jmsTemplate.convertAndSend("order.topic", order, message -> {
                message.setStringProperty("orderId", order.getId().toString());
                message.setStringProperty("eventType", "ORDER_CREATED");
                return message;
            });
            
            System.out.println("Order event published to topic: " + order.getId());
        } catch (Exception e) {
            System.err.println("Failed to publish order event: " + e.getMessage());
            throw new MessageSendException("Failed to publish order event", e);
        }
    }
}
```

### Batch Message Producer

```java
@Component
public class BatchOrderProducer {
    
    private final JmsTemplate jmsTemplate;
    
    public BatchOrderProducer(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }
    
    public void sendOrderBatch(List<Order> orders) {
        jmsTemplate.execute(session -> {
            MessageProducer producer = session.createProducer(session.createQueue("order.queue"));
            
            for (Order order : orders) {
                ObjectMessage message = session.createObjectMessage(order);
                message.setStringProperty("orderId", order.getId().toString());
                message.setStringProperty("batchId", UUID.randomUUID().toString());
                producer.send(message);
            }
            
            return null;
        });
        
        System.out.println("Batch of " + orders.size() + " orders sent");
    }
}
```

## 5. Message Consumer Implementation

### Order Message Consumer

```java
@Component
public class OrderMessageConsumer {
    
    private final OrderService orderService;
    private final MessageErrorHandler errorHandler;
    
    public OrderMessageConsumer(OrderService orderService, MessageErrorHandler errorHandler) {
        this.orderService = orderService;
        this.errorHandler = errorHandler;
    }
    
    @JmsListener(destination = "order.queue")
    public void handleOrder(Order order, 
                           @Header("orderId") String orderId,
                           @Header("customerId") String customerId,
                           @Header(value = "priority", defaultValue = "NORMAL") String priority,
                           Message message) {
        try {
            System.out.println("Received order: " + orderId + " for customer: " + customerId);
            
            // Process the order
            orderService.processOrder(order);
            
            // Acknowledge message
            message.acknowledge();
            
            System.out.println("Order processed successfully: " + orderId);
            
        } catch (Exception e) {
            System.err.println("Error processing order: " + orderId + " - " + e.getMessage());
            errorHandler.handleError(message, e);
        }
    }
    
    @JmsListener(destination = "order.topic")
    public void handleOrderEvent(Order order,
                                @Header("orderId") String orderId,
                                @Header("eventType") String eventType,
                                Message message) {
        try {
            System.out.println("Received order event: " + eventType + " for order: " + orderId);
            
            switch (eventType) {
                case "ORDER_CREATED":
                    orderService.handleOrderCreated(order);
                    break;
                case "ORDER_UPDATED":
                    orderService.handleOrderUpdated(order);
                    break;
                case "ORDER_CANCELLED":
                    orderService.handleOrderCancelled(order);
                    break;
                default:
                    System.out.println("Unknown event type: " + eventType);
            }
            
            message.acknowledge();
            
        } catch (Exception e) {
            System.err.println("Error handling order event: " + e.getMessage());
            errorHandler.handleError(message, e);
        }
    }
}
```

### Priority Message Consumer

```java
@Component
public class PriorityOrderConsumer {
    
    private final OrderService orderService;
    
    public PriorityOrderConsumer(OrderService orderService) {
        this.orderService = orderService;
    }
    
    @JmsListener(destination = "order.queue", 
                 selector = "priority = 'HIGH'",
                 concurrency = "1-3")
    public void handleHighPriorityOrder(Order order, Message message) {
        try {
            System.out.println("Processing HIGH priority order: " + order.getId());
            
            // Fast track processing
            orderService.processHighPriorityOrder(order);
            
            message.acknowledge();
            
        } catch (Exception e) {
            System.err.println("Error processing high priority order: " + e.getMessage());
            throw new RuntimeException("Failed to process high priority order", e);
        }
    }
    
    @JmsListener(destination = "order.queue", 
                 selector = "priority = 'NORMAL' OR priority IS NULL",
                 concurrency = "1-5")
    public void handleNormalPriorityOrder(Order order, Message message) {
        try {
            System.out.println("Processing NORMAL priority order: " + order.getId());
            
            orderService.processNormalPriorityOrder(order);
            
            message.acknowledge();
            
        } catch (Exception e) {
            System.err.println("Error processing normal priority order: " + e.getMessage());
            throw new RuntimeException("Failed to process normal priority order", e);
        }
    }
}
```

## 6. AMQP Implementation with RabbitMQ

### RabbitMQ Configuration

```java
@Configuration
@EnableRabbit
public class RabbitConfig {
    
    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost("localhost");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("admin");
        connectionFactory.setPassword("admin");
        connectionFactory.setVirtualHost("/");
        connectionFactory.setChannelCacheSize(25);
        connectionFactory.setChannelCheckoutTimeout(30000);
        return connectionFactory;
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate template = new RabbitTemplate(connectionFactory());
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setRoutingKey("order.routing.key");
        template.setReplyTimeout(5000);
        template.setRetryTemplate(retryTemplate());
        return template;
    }
    
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10000);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        return retryTemplate;
    }
    
    @Bean
    public RabbitListenerContainerFactory<SimpleMessageListenerContainer> rabbitListenerContainerFactory() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory());
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(5);
        factory.setDefaultRequeueRejected(false);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        return factory;
    }
}
```

### Queue and Exchange Declaration

```java
@Configuration
public class RabbitMQDeclarations {
    
    // Direct Exchange
    @Bean
    public DirectExchange orderExchange() {
        return ExchangeBuilder.directExchange("order.exchange")
                .durable(true)
                .build();
    }
    
    // Topic Exchange
    @Bean
    public TopicExchange orderTopicExchange() {
        return ExchangeBuilder.topicExchange("order.topic.exchange")
                .durable(true)
                .build();
    }
    
    // Fanout Exchange
    @Bean
    public FanoutExchange orderFanoutExchange() {
        return ExchangeBuilder.fanoutExchange("order.fanout.exchange")
                .durable(true)
                .build();
    }
    
    // Order Queue
    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable("order.queue")
                .withArgument("x-message-ttl", 60000)
                .withArgument("x-max-length", 1000)
                .build();
    }
    
    // Priority Queue
    @Bean
    public Queue priorityOrderQueue() {
        return QueueBuilder.durable("order.priority.queue")
                .withArgument("x-max-priority", 10)
                .build();
    }
    
    // Dead Letter Queue
    @Bean
    public Queue orderDeadLetterQueue() {
        return QueueBuilder.durable("order.dlq").build();
    }
    
    // Queue with DLQ
    @Bean
    public Queue orderQueueWithDLQ() {
        return QueueBuilder.durable("order.queue.dlq")
                .withArgument("x-dead-letter-exchange", "order.dlx")
                .withArgument("x-dead-letter-routing-key", "order.dlq")
                .withArgument("x-message-ttl", 30000)
                .build();
    }
    
    // Bindings
    @Bean
    public Binding orderBinding() {
        return BindingBuilder.bind(orderQueue())
                .to(orderExchange())
                .with("order.routing.key");
    }
    
    @Bean
    public Binding orderTopicBinding() {
        return BindingBuilder.bind(orderQueue())
                .to(orderTopicExchange())
                .with("order.created.*");
    }
}
```

## 7. Advanced Message Patterns

### Request-Reply Pattern

```java
@Service
public class OrderQueryService {
    
    private final RabbitTemplate rabbitTemplate;
    
    public OrderQueryService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }
    
    public Order getOrderById(Long orderId) {
        try {
            OrderRequest request = new OrderRequest(orderId);
            
            Object response = rabbitTemplate.convertSendAndReceive(
                "order.query.exchange",
                "order.query.routing.key",
                request
            );
            
            if (response instanceof Order) {
                return (Order) response;
            } else {
                throw new OrderNotFoundException("Order not found: " + orderId);
            }
            
        } catch (Exception e) {
            throw new OrderQueryException("Failed to query order: " + orderId, e);
        }
    }
    
    @RabbitListener(queues = "order.query.queue")
    public Order handleOrderQuery(OrderRequest request) {
        System.out.println("Received order query: " + request.getOrderId());
        
        // Simulate database lookup
        Order order = findOrderById(request.getOrderId());
        
        if (order != null) {
            return order;
        } else {
            throw new OrderNotFoundException("Order not found: " + request.getOrderId());
        }
    }
    
    private Order findOrderById(Long orderId) {
        // Simulate database lookup
        return new Order(orderId, 123L, 456L, 2, BigDecimal.valueOf(99.99), "CONFIRMED");
    }
}
```

### Message Routing Pattern

```java
@Service
public class OrderRoutingService {
    
    private final RabbitTemplate rabbitTemplate;
    
    public OrderRoutingService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }
    
    public void routeOrder(Order order) {
        String routingKey = determineRoutingKey(order);
        
        rabbitTemplate.convertAndSend(
            "order.routing.exchange",
            routingKey,
            order,
            message -> {
                message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                message.getMessageProperties().setPriority(getPriority(order));
                return message;
            }
        );
        
        System.out.println("Order routed with key: " + routingKey);
    }
    
    private String determineRoutingKey(Order order) {
        if (order.getTotalAmount().compareTo(BigDecimal.valueOf(1000)) > 0) {
            return "order.high.value";
        } else if (order.getPriority().equals("HIGH")) {
            return "order.high.priority";
        } else {
            return "order.normal";
        }
    }
    
    private int getPriority(Order order) {
        switch (order.getPriority()) {
            case "HIGH": return 10;
            case "MEDIUM": return 5;
            default: return 1;
        }
    }
}
```

## 8. Message Transactions and Error Handling

### Transactional Message Processing

```java
@Service
@Transactional
public class TransactionalOrderService {
    
    private final OrderRepository orderRepository;
    private final OrderMessageProducer messageProducer;
    
    public TransactionalOrderService(OrderRepository orderRepository,
                                   OrderMessageProducer messageProducer) {
        this.orderRepository = orderRepository;
        this.messageProducer = messageProducer;
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void processOrderWithTransaction(Order order) {
        try {
            // Save order to database
            Order savedOrder = orderRepository.save(order);
            
            // Send message (will be part of transaction)
            messageProducer.sendOrder(savedOrder);
            
            // Simulate business logic
            if (order.getTotalAmount().compareTo(BigDecimal.valueOf(10000)) > 0) {
                throw new OrderValidationException("Order amount too high");
            }
            
            System.out.println("Order processed successfully: " + savedOrder.getId());
            
        } catch (Exception e) {
            System.err.println("Transaction rolled back: " + e.getMessage());
            throw e;
        }
    }
}
```

### Error Handling and Dead Letter Queues

```java
@Component
public class MessageErrorHandler implements ErrorHandler {
    
    private final RabbitTemplate rabbitTemplate;
    
    public MessageErrorHandler(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }
    
    @Override
    public void handleError(Throwable t) {
        System.err.println("Message processing error: " + t.getMessage());
        
        if (t instanceof ListenerExecutionFailedException) {
            ListenerExecutionFailedException ex = (ListenerExecutionFailedException) t;
            Message failedMessage = ex.getFailedMessage();
            
            // Send to dead letter queue
            sendToDeadLetterQueue(failedMessage, t);
        }
    }
    
    public void handleError(Message message, Exception e) {
        System.err.println("Handling message error: " + e.getMessage());
        
        try {
            // Get retry count
            Integer retryCount = (Integer) message.getMessageProperties().getHeaders().get("x-retry-count");
            if (retryCount == null) {
                retryCount = 0;
            }
            
            if (retryCount < 3) {
                // Retry message
                retryMessage(message, retryCount + 1);
            } else {
                // Send to dead letter queue
                sendToDeadLetterQueue(message, e);
            }
            
        } catch (Exception ex) {
            System.err.println("Error handling failed message: " + ex.getMessage());
        }
    }
    
    private void retryMessage(Message message, int retryCount) {
        message.getMessageProperties().getHeaders().put("x-retry-count", retryCount);
        
        // Add delay before retry
        long delay = retryCount * 1000; // 1 second per retry
        message.getMessageProperties().setDelay((int) delay);
        
        rabbitTemplate.send("order.retry.exchange", "order.retry.key", message);
        
        System.out.println("Message scheduled for retry #" + retryCount);
    }
    
    private void sendToDeadLetterQueue(Message message, Throwable error) {
        message.getMessageProperties().getHeaders().put("x-error-message", error.getMessage());
        message.getMessageProperties().getHeaders().put("x-error-timestamp", System.currentTimeMillis());
        
        rabbitTemplate.send("order.dlq.exchange", "order.dlq.key", message);
        
        System.out.println("Message sent to dead letter queue");
    }
}
```

## 9. Message Monitoring and Metrics

### Message Metrics

```java
@Component
public class MessageMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter messagesSent;
    private final Counter messagesReceived;
    private final Counter messagesProcessed;
    private final Counter messagesFailed;
    private final Timer messageProcessingTime;
    
    public MessageMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.messagesSent = Counter.builder("messages.sent")
                .description("Number of messages sent")
                .register(meterRegistry);
        this.messagesReceived = Counter.builder("messages.received")
                .description("Number of messages received")
                .register(meterRegistry);
        this.messagesProcessed = Counter.builder("messages.processed")
                .description("Number of messages processed successfully")
                .register(meterRegistry);
        this.messagesFailed = Counter.builder("messages.failed")
                .description("Number of messages failed to process")
                .register(meterRegistry);
        this.messageProcessingTime = Timer.builder("messages.processing.time")
                .description("Message processing time")
                .register(meterRegistry);
    }
    
    public void recordMessageSent(String destination) {
        messagesSent.increment(Tags.of("destination", destination));
    }
    
    public void recordMessageReceived(String destination) {
        messagesReceived.increment(Tags.of("destination", destination));
    }
    
    public void recordMessageProcessed(String destination) {
        messagesProcessed.increment(Tags.of("destination", destination));
    }
    
    public void recordMessageFailed(String destination, String errorType) {
        messagesFailed.increment(Tags.of("destination", destination, "error.type", errorType));
    }
    
    public Timer.Sample startProcessingTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordProcessingTime(Timer.Sample sample, String destination) {
        sample.stop(Timer.builder("messages.processing.time")
                .tag("destination", destination)
                .register(meterRegistry));
    }
}
```

### Health Check for Messaging

```java
@Component
public class MessagingHealthIndicator implements HealthIndicator {
    
    private final ConnectionFactory jmsConnectionFactory;
    private final org.springframework.amqp.rabbit.connection.ConnectionFactory rabbitConnectionFactory;
    
    public MessagingHealthIndicator(ConnectionFactory jmsConnectionFactory,
                                   org.springframework.amqp.rabbit.connection.ConnectionFactory rabbitConnectionFactory) {
        this.jmsConnectionFactory = jmsConnectionFactory;
        this.rabbitConnectionFactory = rabbitConnectionFactory;
    }
    
    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        
        // Check JMS connection
        try {
            Connection jmsConnection = jmsConnectionFactory.createConnection();
            jmsConnection.start();
            jmsConnection.close();
            builder.withDetail("jms", "Available");
        } catch (Exception e) {
            builder.down().withDetail("jms", "Connection failed: " + e.getMessage());
        }
        
        // Check RabbitMQ connection
        try {
            org.springframework.amqp.rabbit.connection.Connection rabbitConnection = 
                rabbitConnectionFactory.createConnection();
            rabbitConnection.close();
            builder.withDetail("rabbitmq", "Available");
        } catch (Exception e) {
            builder.down().withDetail("rabbitmq", "Connection failed: " + e.getMessage());
        }
        
        return builder.build();
    }
}
```

## 10. Testing Message-Driven Applications

### Integration Testing

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.activemq.broker-url=vm://localhost?broker.persistent=false",
    "spring.rabbitmq.host=localhost",
    "spring.rabbitmq.port=5672"
})
class MessagingIntegrationTest {
    
    @Autowired
    private OrderMessageProducer orderProducer;
    
    @Autowired
    private JmsTemplate jmsTemplate;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Test
    void testJMSMessageSendAndReceive() throws InterruptedException {
        // Given
        Order order = new Order(1L, 123L, 456L, 2, BigDecimal.valueOf(99.99), "PENDING");
        
        // When
        orderProducer.sendOrder(order);
        
        // Then
        Thread.sleep(1000); // Wait for message processing
        
        // Verify message was consumed
        // This would typically be verified through database state or mock interactions
    }
    
    @Test
    void testRabbitMQMessageSendAndReceive() {
        // Given
        Order order = new Order(2L, 123L, 456L, 1, BigDecimal.valueOf(49.99), "PENDING");
        
        // When
        rabbitTemplate.convertAndSend("order.exchange", "order.routing.key", order);
        
        // Then
        Object receivedMessage = rabbitTemplate.receiveAndConvert("order.queue", 5000);
        assertThat(receivedMessage).isNotNull();
        assertThat(receivedMessage).isInstanceOf(Order.class);
        
        Order receivedOrder = (Order) receivedMessage;
        assertThat(receivedOrder.getId()).isEqualTo(2L);
    }
}
```

### Unit Testing Message Handlers

```java
@ExtendWith(MockitoExtension.class)
class OrderMessageConsumerTest {
    
    @Mock
    private OrderService orderService;
    
    @Mock
    private MessageErrorHandler errorHandler;
    
    @Mock
    private Message message;
    
    @InjectMocks
    private OrderMessageConsumer orderConsumer;
    
    @Test
    void testHandleOrderSuccess() throws Exception {
        // Given
        Order order = new Order(1L, 123L, 456L, 2, BigDecimal.valueOf(99.99), "PENDING");
        
        // When
        orderConsumer.handleOrder(order, "1", "123", "HIGH", message);
        
        // Then
        verify(orderService).processOrder(order);
        verify(message).acknowledge();
        verifyNoInteractions(errorHandler);
    }
    
    @Test
    void testHandleOrderFailure() throws Exception {
        // Given
        Order order = new Order(1L, 123L, 456L, 2, BigDecimal.valueOf(99.99), "PENDING");
        doThrow(new OrderProcessingException("Processing failed")).when(orderService).processOrder(order);
        
        // When
        orderConsumer.handleOrder(order, "1", "123", "HIGH", message);
        
        // Then
        verify(orderService).processOrder(order);
        verify(errorHandler).handleError(eq(message), any(OrderProcessingException.class));
        verify(message, never()).acknowledge();
    }
}
```

## Summary

Today we covered:

1. **Messaging Fundamentals**: Core concepts and patterns for enterprise messaging
2. **JMS Overview**: Java Message Service architecture and message types
3. **Spring JMS**: Configuration and integration with Spring Framework
4. **Message Producers**: Implementing reliable message sending with various patterns
5. **Message Consumers**: Building robust message processing with error handling
6. **AMQP with RabbitMQ**: Advanced messaging patterns and queue management
7. **Advanced Patterns**: Request-reply, routing, and priority messaging
8. **Transactions and Error Handling**: Ensuring message reliability and consistency
9. **Monitoring and Metrics**: Observability for message-driven applications
10. **Testing**: Comprehensive testing strategies for messaging systems

## Key Takeaways

- Messaging enables loose coupling and scalability in distributed systems
- JMS provides standardized messaging API for Java applications
- AMQP offers advanced routing and reliability features
- Proper error handling and dead letter queues are essential for production systems
- Message transactions ensure consistency between database and messaging operations
- Monitoring and metrics provide visibility into message processing performance

## Next Steps

Tomorrow we'll explore Spring Integration and Enterprise Integration Patterns, learning how to build complex message flows and integrate with various systems and protocols.