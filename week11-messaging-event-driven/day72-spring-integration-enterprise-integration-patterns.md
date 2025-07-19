# Day 72: Spring Integration - Enterprise Integration Patterns

## Overview

Today we'll explore Spring Integration, a powerful framework for building enterprise integration solutions. We'll learn about Enterprise Integration Patterns (EIP), message channels, endpoints, transformers, and how to integrate with various systems and protocols.

## Learning Objectives

- Understand Enterprise Integration Patterns and their implementation
- Master Spring Integration framework components
- Build message flows with channels, gateways, and endpoints
- Implement transformers, filters, and routers
- Integrate with files, databases, web services, and message brokers
- Handle error scenarios and monitoring in integration flows

## 1. Spring Integration Fundamentals

### Core Components

**Message**: Unit of data with headers and payload
**Channel**: Pipe that connects message endpoints
**Endpoint**: Component that handles messages
**Gateway**: Entry point for sending messages
**Adapter**: Connects to external systems

### Integration Patterns

**Message Channel**: Connects message endpoints
**Message Endpoint**: Handles messages at channel ends
**Message Translator**: Transforms message structure
**Message Router**: Routes messages to different channels
**Message Filter**: Selects messages based on criteria

### Dependencies

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.integration</groupId>
    <artifactId>spring-integration-core</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.integration</groupId>
    <artifactId>spring-integration-file</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.integration</groupId>
    <artifactId>spring-integration-jdbc</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.integration</groupId>
    <artifactId>spring-integration-http</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.integration</groupId>
    <artifactId>spring-integration-jms</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.integration</groupId>
    <artifactId>spring-integration-amqp</artifactId>
</dependency>
```

## 2. Basic Integration Configuration

### Java Configuration

```java
@Configuration
@EnableIntegration
@IntegrationComponentScan
public class IntegrationConfig {
    
    @Bean
    public MessageChannel inputChannel() {
        return MessageChannels.direct().get();
    }
    
    @Bean
    public MessageChannel outputChannel() {
        return MessageChannels.publishSubscribe().get();
    }
    
    @Bean
    public MessageChannel errorChannel() {
        return MessageChannels.direct().get();
    }
    
    @Bean
    public QueueChannel priorityChannel() {
        return MessageChannels.priority().get();
    }
    
    @Bean
    public ExecutorChannel asyncChannel() {
        return MessageChannels.executor(Executors.newFixedThreadPool(5)).get();
    }
    
    @Bean
    public IntegrationFlow simpleFlow() {
        return IntegrationFlows
            .from(inputChannel())
            .transform(String.class, String::toUpperCase)
            .handle(m -> System.out.println("Processed: " + m.getPayload()))
            .get();
    }
}
```

### Message Gateway

```java
@MessagingGateway
public interface OrderGateway {
    
    @Gateway(requestChannel = "orderInputChannel")
    void processOrder(Order order);
    
    @Gateway(requestChannel = "orderInputChannel", replyChannel = "orderOutputChannel")
    OrderResult processOrderWithResult(Order order);
    
    @Gateway(requestChannel = "orderInputChannel", replyTimeout = 5000)
    Future<OrderResult> processOrderAsync(Order order);
    
    @Gateway(requestChannel = "orderValidationChannel")
    boolean validateOrder(Order order);
    
    @Gateway(requestChannel = "orderQueryChannel")
    Order getOrderById(Long orderId);
}
```

## 3. Message Channels and Endpoints

### Channel Types Implementation

```java
@Configuration
public class ChannelConfig {
    
    @Bean
    public MessageChannel directChannel() {
        return MessageChannels.direct().get();
    }
    
    @Bean
    public MessageChannel publishSubscribeChannel() {
        return MessageChannels.publishSubscribe()
            .executor(Executors.newFixedThreadPool(3))
            .get();
    }
    
    @Bean
    public QueueChannel queueChannel() {
        return MessageChannels.queue(100).get();
    }
    
    @Bean
    public PriorityChannel priorityChannel() {
        return MessageChannels.priority()
            .capacity(50)
            .comparator((m1, m2) -> {
                Integer p1 = m1.getHeaders().get("priority", Integer.class);
                Integer p2 = m2.getHeaders().get("priority", Integer.class);
                return Integer.compare(p2 != null ? p2 : 0, p1 != null ? p1 : 0);
            })
            .get();
    }
    
    @Bean
    public MessageChannel retryChannel() {
        return MessageChannels.direct()
            .interceptor(new RetryInterceptor())
            .get();
    }
}
```

### Service Activators

```java
@Component
public class OrderServiceActivator {
    
    private final OrderService orderService;
    private final OrderValidator orderValidator;
    
    public OrderServiceActivator(OrderService orderService, OrderValidator orderValidator) {
        this.orderService = orderService;
        this.orderValidator = orderValidator;
    }
    
    @ServiceActivator(inputChannel = "orderProcessingChannel")
    public OrderResult processOrder(Order order) {
        try {
            System.out.println("Processing order: " + order.getId());
            
            // Validate order
            ValidationResult validation = orderValidator.validate(order);
            if (!validation.isValid()) {
                return OrderResult.failure(order.getId(), validation.getErrors());
            }
            
            // Process order
            Order processedOrder = orderService.processOrder(order);
            
            return OrderResult.success(processedOrder.getId(), "Order processed successfully");
            
        } catch (Exception e) {
            System.err.println("Error processing order: " + e.getMessage());
            return OrderResult.failure(order.getId(), e.getMessage());
        }
    }
    
    @ServiceActivator(inputChannel = "orderValidationChannel")
    public boolean validateOrder(Order order) {
        return orderValidator.validate(order).isValid();
    }
    
    @ServiceActivator(inputChannel = "orderQueryChannel")
    public Order getOrderById(Long orderId) {
        return orderService.findById(orderId);
    }
}
```

## 4. Message Transformers

### Transformer Implementation

```java
@Component
public class OrderTransformers {
    
    @Transformer(inputChannel = "orderInputChannel", outputChannel = "orderProcessingChannel")
    public Order enrichOrder(Order order) {
        // Enrich order with additional data
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus("PROCESSING");
        order.setVersion(1);
        
        // Add calculated fields
        BigDecimal tax = order.getTotalAmount().multiply(BigDecimal.valueOf(0.1));
        order.setTaxAmount(tax);
        order.setGrandTotal(order.getTotalAmount().add(tax));
        
        System.out.println("Order enriched: " + order.getId());
        return order;
    }
    
    @Transformer(inputChannel = "orderXmlChannel", outputChannel = "orderProcessingChannel")
    public Order xmlToOrder(String xmlString) {
        try {
            // Parse XML to Order object
            JAXBContext jaxbContext = JAXBContext.newInstance(Order.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            
            StringReader reader = new StringReader(xmlString);
            return (Order) unmarshaller.unmarshal(reader);
            
        } catch (Exception e) {
            throw new MessageTransformationException("Failed to parse XML to Order", e);
        }
    }
    
    @Transformer(inputChannel = "orderJsonChannel", outputChannel = "orderProcessingChannel")
    public Order jsonToOrder(String jsonString) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper.readValue(jsonString, Order.class);
            
        } catch (Exception e) {
            throw new MessageTransformationException("Failed to parse JSON to Order", e);
        }
    }
    
    @Transformer(inputChannel = "orderResultChannel", outputChannel = "orderNotificationChannel")
    public OrderNotification orderToNotification(OrderResult result) {
        OrderNotification notification = new OrderNotification();
        notification.setOrderId(result.getOrderId());
        notification.setMessage(result.getMessage());
        notification.setTimestamp(LocalDateTime.now());
        notification.setRecipient(getCustomerEmail(result.getOrderId()));
        
        return notification;
    }
    
    private String getCustomerEmail(Long orderId) {
        // Lookup customer email by order ID
        return "customer@example.com";
    }
}
```

### Payload Type Transformer

```java
@Component
public class PayloadTypeTransformers {
    
    @PayloadTypeTransformer(inputChannel = "genericInputChannel")
    public String handleString(String payload) {
        return payload.toUpperCase();
    }
    
    @PayloadTypeTransformer(inputChannel = "genericInputChannel")
    public OrderResult handleOrder(Order order) {
        return new OrderResult(order.getId(), true, "Order received");
    }
    
    @PayloadTypeTransformer(inputChannel = "genericInputChannel")
    public String handleByteArray(byte[] payload) {
        return new String(payload, StandardCharsets.UTF_8);
    }
    
    @PayloadTypeTransformer(inputChannel = "genericInputChannel")
    public Map<String, Object> handleMap(Map<String, Object> payload) {
        payload.put("processed", true);
        payload.put("timestamp", LocalDateTime.now());
        return payload;
    }
}
```

## 5. Message Routing and Filtering

### Router Implementation

```java
@Component
public class OrderRouter {
    
    @Router(inputChannel = "orderRoutingChannel")
    public String routeOrder(Order order) {
        if (order.getTotalAmount().compareTo(BigDecimal.valueOf(1000)) > 0) {
            return "highValueOrderChannel";
        } else if (order.getPriority().equals("HIGH")) {
            return "highPriorityOrderChannel";
        } else {
            return "normalOrderChannel";
        }
    }
    
    @Router(inputChannel = "orderStatusChannel")
    public Collection<String> routeByStatus(Order order) {
        List<String> channels = new ArrayList<>();
        
        // Always route to processing channel
        channels.add("orderProcessingChannel");
        
        // Route to notification channel if confirmed
        if ("CONFIRMED".equals(order.getStatus())) {
            channels.add("orderNotificationChannel");
        }
        
        // Route to audit channel for high-value orders
        if (order.getTotalAmount().compareTo(BigDecimal.valueOf(1000)) > 0) {
            channels.add("orderAuditChannel");
        }
        
        return channels;
    }
    
    @Router(inputChannel = "orderErrorChannel")
    public String routeError(Message<?> message) {
        Exception exception = (Exception) message.getHeaders().get("exception");
        
        if (exception instanceof OrderValidationException) {
            return "orderValidationErrorChannel";
        } else if (exception instanceof OrderProcessingException) {
            return "orderProcessingErrorChannel";
        } else {
            return "orderGeneralErrorChannel";
        }
    }
}
```

### Filter Implementation

```java
@Component
public class OrderFilters {
    
    @Filter(inputChannel = "orderInputChannel", outputChannel = "orderProcessingChannel")
    public boolean filterValidOrders(Order order) {
        return order.getCustomerId() != null && 
               order.getProductId() != null && 
               order.getQuantity() > 0 && 
               order.getTotalAmount().compareTo(BigDecimal.ZERO) > 0;
    }
    
    @Filter(inputChannel = "orderProcessingChannel", outputChannel = "highValueOrderChannel")
    public boolean filterHighValueOrders(Order order) {
        return order.getTotalAmount().compareTo(BigDecimal.valueOf(1000)) > 0;
    }
    
    @Filter(inputChannel = "orderProcessingChannel", outputChannel = "priorityOrderChannel")
    public boolean filterPriorityOrders(Order order) {
        return "HIGH".equals(order.getPriority()) || "URGENT".equals(order.getPriority());
    }
    
    @Filter(inputChannel = "orderProcessingChannel", outputChannel = "businessHoursChannel")
    public boolean filterBusinessHours(Order order) {
        LocalTime now = LocalTime.now();
        return now.isAfter(LocalTime.of(9, 0)) && now.isBefore(LocalTime.of(17, 0));
    }
}
```

## 6. Integration Flows

### Complex Integration Flow

```java
@Configuration
public class OrderIntegrationFlows {
    
    @Bean
    public IntegrationFlow orderProcessingFlow() {
        return IntegrationFlows
            .from("orderInputChannel")
            .filter(Order.class, order -> order.getCustomerId() != null)
            .transform(Order.class, this::enrichOrder)
            .route(Order.class, order -> {
                if (order.getTotalAmount().compareTo(BigDecimal.valueOf(1000)) > 0) {
                    return "highValueOrderChannel";
                } else {
                    return "normalOrderChannel";
                }
            })
            .get();
    }
    
    @Bean
    public IntegrationFlow highValueOrderFlow() {
        return IntegrationFlows
            .from("highValueOrderChannel")
            .handle(Order.class, (order, headers) -> {
                // Require approval for high-value orders
                order.setStatus("PENDING_APPROVAL");
                return order;
            })
            .channel("orderApprovalChannel")
            .get();
    }
    
    @Bean
    public IntegrationFlow normalOrderFlow() {
        return IntegrationFlows
            .from("normalOrderChannel")
            .handle(Order.class, (order, headers) -> {
                // Auto-approve normal orders
                order.setStatus("APPROVED");
                return order;
            })
            .channel("orderFulfillmentChannel")
            .get();
    }
    
    @Bean
    public IntegrationFlow errorHandlingFlow() {
        return IntegrationFlows
            .from("errorChannel")
            .handle(ErrorMessage.class, (errorMessage, headers) -> {
                System.err.println("Error occurred: " + errorMessage.getPayload().getMessage());
                // Log error, send alert, etc.
                return null;
            })
            .get();
    }
    
    @Bean
    public IntegrationFlow pollingFlow() {
        return IntegrationFlows
            .from(Amqp.inboundGateway(rabbitConnectionFactory(), "order.queue"))
            .transform(String.class, String::toUpperCase)
            .handle(String.class, (payload, headers) -> {
                System.out.println("Received from RabbitMQ: " + payload);
                return payload + " - processed";
            })
            .get();
    }
    
    private Order enrichOrder(Order order) {
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus("PROCESSING");
        return order;
    }
}
```

### File Integration Flow

```java
@Configuration
public class FileIntegrationFlow {
    
    @Bean
    public IntegrationFlow fileReadingFlow() {
        return IntegrationFlows
            .from(Files.inboundAdapter(new File("/tmp/orders"))
                .patternFilter("*.csv")
                .autoCreateDirectory(false))
            .transform(Files.toStringTransformer())
            .split(s -> s.delimiters("\n"))
            .filter(String.class, line -> !line.trim().isEmpty())
            .transform(String.class, this::csvToOrder)
            .channel("orderProcessingChannel")
            .get();
    }
    
    @Bean
    public IntegrationFlow fileWritingFlow() {
        return IntegrationFlows
            .from("orderOutputChannel")
            .transform(Order.class, this::orderToCsv)
            .handle(Files.outboundAdapter(new File("/tmp/processed"))
                .fileNameGenerator(message -> "order_" + System.currentTimeMillis() + ".csv")
                .deleteSourceFiles(false))
            .get();
    }
    
    private Order csvToOrder(String csvLine) {
        String[] fields = csvLine.split(",");
        Order order = new Order();
        order.setId(Long.parseLong(fields[0]));
        order.setCustomerId(Long.parseLong(fields[1]));
        order.setProductId(Long.parseLong(fields[2]));
        order.setQuantity(Integer.parseInt(fields[3]));
        order.setTotalAmount(new BigDecimal(fields[4]));
        return order;
    }
    
    private String orderToCsv(Order order) {
        return String.format("%d,%d,%d,%d,%.2f",
            order.getId(),
            order.getCustomerId(),
            order.getProductId(),
            order.getQuantity(),
            order.getTotalAmount());
    }
}
```

## 7. Database Integration

### JDBC Integration

```java
@Configuration
public class DatabaseIntegrationConfig {
    
    @Bean
    public IntegrationFlow jdbcInboundFlow() {
        return IntegrationFlows
            .from(Jdbc.inboundChannelAdapter(dataSource())
                .query("SELECT * FROM orders WHERE status = 'PENDING'")
                .updateSql("UPDATE orders SET status = 'PROCESSING' WHERE id = :id")
                .rowMapper(new OrderRowMapper()),
                e -> e.poller(Pollers.fixedDelay(Duration.ofSeconds(5))))
            .channel("orderProcessingChannel")
            .get();
    }
    
    @Bean
    public IntegrationFlow jdbcOutboundFlow() {
        return IntegrationFlows
            .from("orderUpdateChannel")
            .handle(Jdbc.outboundAdapter(dataSource())
                .messageSqlParameterSourceFactory(new BeanPropertySqlParameterSourceFactory())
                .sql("UPDATE orders SET status = :status, updated_at = NOW() WHERE id = :id"))
            .get();
    }
    
    @Bean
    public IntegrationFlow jdbcStoredProcFlow() {
        return IntegrationFlows
            .from("orderCalculationChannel")
            .handle(Jdbc.storedProcOutboundAdapter(dataSource())
                .storedProcedureName("calculate_order_total")
                .sqlParameterSourceFactory(new BeanPropertySqlParameterSourceFactory())
                .declareParameters(
                    new SqlParameter("order_id", Types.BIGINT),
                    new SqlOutParameter("total_amount", Types.DECIMAL)
                ))
            .get();
    }
    
    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:postgresql://localhost:5432/orders");
        dataSource.setUsername("admin");
        dataSource.setPassword("admin");
        return dataSource;
    }
}
```

### JPA Integration

```java
@Configuration
public class JpaIntegrationConfig {
    
    @Bean
    public IntegrationFlow jpaInboundFlow() {
        return IntegrationFlows
            .from(Jpa.inboundChannelAdapter(entityManagerFactory())
                .entityClass(Order.class)
                .jpaQuery("SELECT o FROM Order o WHERE o.status = 'PENDING'")
                .expectSingleResult(false)
                .deleteAfterPoll(false)
                .deleteInBatch(true),
                e -> e.poller(Pollers.fixedDelay(Duration.ofSeconds(10))))
            .channel("orderProcessingChannel")
            .get();
    }
    
    @Bean
    public IntegrationFlow jpaOutboundFlow() {
        return IntegrationFlows
            .from("orderPersistChannel")
            .handle(Jpa.outboundAdapter(entityManagerFactory())
                .entityClass(Order.class)
                .persistMode(PersistMode.MERGE))
            .get();
    }
    
    @Bean
    public IntegrationFlow jpaUpdateFlow() {
        return IntegrationFlows
            .from("orderUpdateChannel")
            .handle(Jpa.updatingOutboundAdapter(entityManagerFactory())
                .entityClass(Order.class)
                .jpaQuery("UPDATE Order o SET o.status = :status WHERE o.id = :id")
                .parameterSourceFactory(new BeanPropertyParameterSourceFactory()))
            .get();
    }
    
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource());
        em.setPackagesToScan("com.example.order.entity");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        return em;
    }
}
```

## 8. Web Service Integration

### HTTP Integration

```java
@Configuration
public class HttpIntegrationConfig {
    
    @Bean
    public IntegrationFlow httpInboundFlow() {
        return IntegrationFlows
            .from(Http.inboundChannelAdapter("/api/orders")
                .requestMapping(r -> r.methods(HttpMethod.POST))
                .requestPayloadType(Order.class)
                .statusCodeExpression("T(org.springframework.http.HttpStatus).CREATED"))
            .channel("orderProcessingChannel")
            .get();
    }
    
    @Bean
    public IntegrationFlow httpOutboundFlow() {
        return IntegrationFlows
            .from("orderNotificationChannel")
            .handle(Http.outboundGateway("http://notification-service/api/notify")
                .httpMethod(HttpMethod.POST)
                .expectedResponseType(String.class)
                .charset("UTF-8")
                .extractPayload(true))
            .get();
    }
    
    @Bean
    public IntegrationFlow restTemplateFlow() {
        return IntegrationFlows
            .from("orderValidationChannel")
            .handle(Http.outboundGateway("http://validation-service/api/validate/{orderId}")
                .httpMethod(HttpMethod.GET)
                .uriVariableExpressions("orderId", "payload.id")
                .expectedResponseType(ValidationResult.class)
                .restTemplate(restTemplate()))
            .get();
    }
    
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate template = new RestTemplate();
        template.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        return template;
    }
}
```

### WebSocket Integration

```java
@Configuration
@EnableWebSocket
public class WebSocketIntegrationConfig implements WebSocketConfigurer {
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new OrderWebSocketHandler(), "/orders").setAllowedOrigins("*");
    }
    
    @Bean
    public IntegrationFlow webSocketInboundFlow() {
        return IntegrationFlows
            .from(WebSockets.inboundChannelAdapter("/orders"))
            .channel("orderInputChannel")
            .get();
    }
    
    @Bean
    public IntegrationFlow webSocketOutboundFlow() {
        return IntegrationFlows
            .from("orderNotificationChannel")
            .handle(WebSockets.outboundChannelAdapter("/orders")
                .autoStartup(true))
            .get();
    }
}
```

## 9. Error Handling and Monitoring

### Error Handling Configuration

```java
@Configuration
public class ErrorHandlingConfig {
    
    @Bean
    public IntegrationFlow errorHandlingFlow() {
        return IntegrationFlows
            .from("errorChannel")
            .route(ErrorMessage.class, message -> {
                Throwable cause = message.getPayload().getCause();
                if (cause instanceof OrderValidationException) {
                    return "validationErrorChannel";
                } else if (cause instanceof OrderProcessingException) {
                    return "processingErrorChannel";
                } else {
                    return "generalErrorChannel";
                }
            })
            .get();
    }
    
    @Bean
    public IntegrationFlow retryFlow() {
        return IntegrationFlows
            .from("retryChannel")
            .handle(Order.class, (order, headers) -> {
                Integer retryCount = (Integer) headers.get("retry_count");
                if (retryCount == null) {
                    retryCount = 0;
                }
                
                if (retryCount < 3) {
                    // Retry processing
                    return MessageBuilder.withPayload(order)
                        .setHeader("retry_count", retryCount + 1)
                        .build();
                } else {
                    // Send to DLQ
                    return MessageBuilder.withPayload(order)
                        .setHeader("failed_after_retries", true)
                        .build();
                }
            })
            .channel("orderProcessingChannel")
            .get();
    }
    
    @ServiceActivator(inputChannel = "validationErrorChannel")
    public void handleValidationError(ErrorMessage errorMessage) {
        System.err.println("Validation error: " + errorMessage.getPayload().getMessage());
        // Log error, send alert, etc.
    }
    
    @ServiceActivator(inputChannel = "processingErrorChannel")
    public void handleProcessingError(ErrorMessage errorMessage) {
        System.err.println("Processing error: " + errorMessage.getPayload().getMessage());
        // Log error, send alert, etc.
    }
}
```

### Monitoring and Metrics

```java
@Component
public class IntegrationMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public IntegrationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    @EventListener
    public void handleIntegrationEvent(MessagingEvent event) {
        if (event instanceof MessageHandlingEvent) {
            MessageHandlingEvent handlingEvent = (MessageHandlingEvent) event;
            recordMessageHandling(handlingEvent);
        }
    }
    
    private void recordMessageHandling(MessageHandlingEvent event) {
        String componentName = event.getSource().getClass().getSimpleName();
        String messageType = event.getMessage().getPayload().getClass().getSimpleName();
        
        meterRegistry.counter("integration.messages.processed",
            "component", componentName,
            "message.type", messageType).increment();
    }
    
    @Bean
    public IntegrationFlow metricsFlow() {
        return IntegrationFlows
            .from("metricsChannel")
            .handle(Message.class, (message, headers) -> {
                // Record custom metrics
                meterRegistry.counter("integration.custom.metric").increment();
                return message;
            })
            .get();
    }
}
```

## 10. Testing Integration Flows

### Integration Testing

```java
@SpringBootTest
@SpringIntegrationTest
@DirtiesContext
class OrderIntegrationFlowTest {
    
    @Autowired
    private OrderGateway orderGateway;
    
    @Autowired
    private MessageChannel orderInputChannel;
    
    @Autowired
    private QueueChannel orderOutputChannel;
    
    @Test
    void testOrderProcessingFlow() {
        // Given
        Order order = new Order(1L, 123L, 456L, 2, BigDecimal.valueOf(99.99), "PENDING");
        
        // When
        orderGateway.processOrder(order);
        
        // Then
        Message<?> result = orderOutputChannel.receive(5000);
        assertThat(result).isNotNull();
        assertThat(result.getPayload()).isInstanceOf(OrderResult.class);
        
        OrderResult orderResult = (OrderResult) result.getPayload();
        assertThat(orderResult.isSuccess()).isTrue();
        assertThat(orderResult.getOrderId()).isEqualTo(1L);
    }
    
    @Test
    void testOrderValidationFlow() {
        // Given
        Order invalidOrder = new Order(null, null, null, 0, BigDecimal.ZERO, "PENDING");
        
        // When
        boolean isValid = orderGateway.validateOrder(invalidOrder);
        
        // Then
        assertThat(isValid).isFalse();
    }
}
```

### Mock Integration Testing

```java
@SpringBootTest
@MockIntegrationContext
class MockIntegrationTest {
    
    @Autowired
    private OrderGateway orderGateway;
    
    @MockBean
    private OrderService orderService;
    
    @Test
    void testMockIntegration() {
        // Given
        Order order = new Order(1L, 123L, 456L, 2, BigDecimal.valueOf(99.99), "PENDING");
        given(orderService.processOrder(any(Order.class)))
            .willReturn(order);
        
        // When
        OrderResult result = orderGateway.processOrderWithResult(order);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        verify(orderService).processOrder(order);
    }
}
```

## Summary

Today we covered:

1. **Spring Integration Fundamentals**: Core components and Enterprise Integration Patterns
2. **Configuration**: Java-based configuration and message gateways
3. **Channels and Endpoints**: Various channel types and service activators
4. **Message Transformation**: Payload transformation and type conversion
5. **Routing and Filtering**: Message routing and filtering patterns
6. **Integration Flows**: Complex message flows and DSL usage
7. **Database Integration**: JDBC and JPA integration patterns
8. **Web Service Integration**: HTTP, REST, and WebSocket integration
9. **Error Handling**: Comprehensive error handling and retry mechanisms
10. **Testing**: Integration testing strategies and mock integration

## Key Takeaways

- Spring Integration provides a comprehensive framework for Enterprise Integration Patterns
- Integration flows enable declarative message processing pipelines
- Channel types provide different messaging semantics (direct, queue, publish-subscribe)
- Transformers, filters, and routers enable sophisticated message processing
- Error handling and monitoring are crucial for production integration solutions
- Testing integration flows requires specialized techniques and tools

## Next Steps

Tomorrow we'll explore Apache Kafka for event streaming and stream processing, learning how to build scalable, real-time event-driven applications with high throughput and low latency.