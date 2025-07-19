# Day 75: Message Brokers - RabbitMQ and ActiveMQ

## Overview

Today we'll explore advanced message broker implementations with RabbitMQ and ActiveMQ. We'll learn about clustering, high availability, advanced routing patterns, management and monitoring, and production deployment strategies.

## Learning Objectives

- Master RabbitMQ advanced features and clustering
- Implement ActiveMQ with network of brokers
- Configure high availability and failover
- Implement advanced routing and exchange patterns
- Set up monitoring and management tools
- Optimize performance and handle production scenarios

## 1. RabbitMQ Advanced Configuration

### RabbitMQ Cluster Setup

```yaml
# docker-compose.yml for RabbitMQ Cluster
version: '3.8'

services:
  rabbitmq-1:
    image: rabbitmq:3-management
    container_name: rabbitmq-1
    hostname: rabbitmq-1
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      - RABBITMQ_DEFAULT_USER=admin
      - RABBITMQ_DEFAULT_PASS=admin
      - RABBITMQ_ERLANG_COOKIE=SWQOKODSQALRPCLNMEQG
    volumes:
      - ./rabbitmq-1:/var/lib/rabbitmq
      - ./rabbitmq.conf:/etc/rabbitmq/rabbitmq.conf
    networks:
      - rabbitmq-cluster

  rabbitmq-2:
    image: rabbitmq:3-management
    container_name: rabbitmq-2
    hostname: rabbitmq-2
    ports:
      - "5673:5672"
      - "15673:15672"
    environment:
      - RABBITMQ_DEFAULT_USER=admin
      - RABBITMQ_DEFAULT_PASS=admin
      - RABBITMQ_ERLANG_COOKIE=SWQOKODSQALRPCLNMEQG
    volumes:
      - ./rabbitmq-2:/var/lib/rabbitmq
      - ./rabbitmq.conf:/etc/rabbitmq/rabbitmq.conf
    networks:
      - rabbitmq-cluster
    depends_on:
      - rabbitmq-1

  rabbitmq-3:
    image: rabbitmq:3-management
    container_name: rabbitmq-3
    hostname: rabbitmq-3
    ports:
      - "5674:5672"
      - "15674:15672"
    environment:
      - RABBITMQ_DEFAULT_USER=admin
      - RABBITMQ_DEFAULT_PASS=admin
      - RABBITMQ_ERLANG_COOKIE=SWQOKODSQALRPCLNMEQG
    volumes:
      - ./rabbitmq-3:/var/lib/rabbitmq
      - ./rabbitmq.conf:/etc/rabbitmq/rabbitmq.conf
    networks:
      - rabbitmq-cluster
    depends_on:
      - rabbitmq-1

networks:
  rabbitmq-cluster:
    driver: bridge
```

### RabbitMQ Configuration

```conf
# rabbitmq.conf
cluster_formation.peer_discovery_backend = rabbit_peer_discovery_classic_config
cluster_formation.classic_config.nodes.1 = rabbit@rabbitmq-1
cluster_formation.classic_config.nodes.2 = rabbit@rabbitmq-2
cluster_formation.classic_config.nodes.3 = rabbit@rabbitmq-3

# High availability
cluster_partition_handling = pause_minority
vm_memory_high_watermark.relative = 0.6
disk_free_limit.relative = 2.0

# Performance tuning
heartbeat = 60
frame_max = 131072
channel_max = 2047
connection_max = 1000

# Management plugin
management.tcp.port = 15672
management.tcp.ip = 0.0.0.0

# Logging
log.console = true
log.console.level = info
log.file = /var/log/rabbitmq/rabbit.log
log.file.level = info
```

### Spring Configuration for RabbitMQ Cluster

```java
@Configuration
@EnableRabbit
public class RabbitMQClusterConfig {
    
    @Bean
    public CachingConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        
        // Cluster addresses
        connectionFactory.setAddresses("localhost:5672,localhost:5673,localhost:5674");
        connectionFactory.setUsername("admin");
        connectionFactory.setPassword("admin");
        connectionFactory.setVirtualHost("/");
        
        // Connection pooling
        connectionFactory.setChannelCacheSize(25);
        connectionFactory.setChannelCheckoutTimeout(30000);
        connectionFactory.setConnectionTimeout(30000);
        connectionFactory.setRequestedHeartBeat(60);
        
        // Publisher confirms and returns
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connectionFactory.setPublisherReturns(true);
        
        return connectionFactory;
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setRetryTemplate(retryTemplate());
        template.setConfirmCallback(confirmCallback());
        template.setReturnsCallback(returnsCallback());
        template.setMandatory(true);
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
    public RabbitTemplate.ConfirmCallback confirmCallback() {
        return (correlationData, ack, cause) -> {
            if (ack) {
                System.out.println("Message confirmed: " + correlationData);
            } else {
                System.err.println("Message not confirmed: " + correlationData + ", cause: " + cause);
            }
        };
    }
    
    @Bean
    public RabbitTemplate.ReturnsCallback returnsCallback() {
        return returnedMessage -> {
            System.err.println("Message returned: " + returnedMessage.getMessage());
            System.err.println("Reply code: " + returnedMessage.getReplyCode());
            System.err.println("Reply text: " + returnedMessage.getReplyText());
            System.err.println("Exchange: " + returnedMessage.getExchange());
            System.err.println("Routing key: " + returnedMessage.getRoutingKey());
        };
    }
}
```

## 2. Advanced RabbitMQ Patterns

### High Availability Queue Configuration

```java
@Configuration
public class HAQueueConfiguration {
    
    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable("order.queue")
                .withArgument("x-ha-policy", "all")
                .withArgument("x-ha-sync-mode", "automatic")
                .withArgument("x-message-ttl", 86400000) // 24 hours
                .withArgument("x-max-length", 10000)
                .withArgument("x-max-priority", 10)
                .build();
    }
    
    @Bean
    public Queue orderDLQ() {
        return QueueBuilder.durable("order.dlq")
                .withArgument("x-ha-policy", "all")
                .build();
    }
    
    @Bean
    public DirectExchange orderExchange() {
        return ExchangeBuilder.directExchange("order.exchange")
                .durable(true)
                .withArgument("x-ha-policy", "all")
                .build();
    }
    
    @Bean
    public DirectExchange orderDLX() {
        return ExchangeBuilder.directExchange("order.dlx")
                .durable(true)
                .build();
    }
    
    @Bean
    public Queue orderQueueWithDLX() {
        return QueueBuilder.durable("order.queue.dlx")
                .withArgument("x-ha-policy", "all")
                .withArgument("x-dead-letter-exchange", "order.dlx")
                .withArgument("x-dead-letter-routing-key", "order.dlq")
                .withArgument("x-message-ttl", 30000)
                .withArgument("x-max-retries", 3)
                .build();
    }
    
    @Bean
    public Binding orderBinding() {
        return BindingBuilder.bind(orderQueue())
                .to(orderExchange())
                .with("order.created");
    }
    
    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(orderDLQ())
                .to(orderDLX())
                .with("order.dlq");
    }
}
```

### Advanced Routing Patterns

```java
@Component
public class AdvancedRabbitMQPatterns {
    
    private final RabbitTemplate rabbitTemplate;
    private final RabbitAdmin rabbitAdmin;
    
    public AdvancedRabbitMQPatterns(RabbitTemplate rabbitTemplate, RabbitAdmin rabbitAdmin) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitAdmin = rabbitAdmin;
    }
    
    // Topic Exchange with wildcard routing
    public void setupTopicExchange() {
        TopicExchange topicExchange = ExchangeBuilder.topicExchange("order.topic.exchange")
                .durable(true)
                .build();
        
        Queue regionQueue = QueueBuilder.durable("order.region.queue").build();
        Queue priorityQueue = QueueBuilder.durable("order.priority.queue").build();
        Queue allOrdersQueue = QueueBuilder.durable("order.all.queue").build();
        
        // Bindings with patterns
        Binding regionBinding = BindingBuilder.bind(regionQueue)
                .to(topicExchange)
                .with("order.*.region.*");
        
        Binding priorityBinding = BindingBuilder.bind(priorityQueue)
                .to(topicExchange)
                .with("order.high.priority.*");
        
        Binding allBinding = BindingBuilder.bind(allOrdersQueue)
                .to(topicExchange)
                .with("order.#");
        
        rabbitAdmin.declareExchange(topicExchange);
        rabbitAdmin.declareQueue(regionQueue);
        rabbitAdmin.declareQueue(priorityQueue);
        rabbitAdmin.declareQueue(allOrdersQueue);
        rabbitAdmin.declareBinding(regionBinding);
        rabbitAdmin.declareBinding(priorityBinding);
        rabbitAdmin.declareBinding(allBinding);
    }
    
    // Headers Exchange
    public void setupHeadersExchange() {
        HeadersExchange headersExchange = ExchangeBuilder.headersExchange("order.headers.exchange")
                .durable(true)
                .build();
        
        Queue urgentQueue = QueueBuilder.durable("order.urgent.queue").build();
        Queue largeOrderQueue = QueueBuilder.durable("order.large.queue").build();
        
        // Headers bindings
        Binding urgentBinding = BindingBuilder.bind(urgentQueue)
                .to(headersExchange)
                .where("priority").matches("urgent");
        
        Binding largeOrderBinding = BindingBuilder.bind(largeOrderQueue)
                .to(headersExchange)
                .whereAll("orderType", "amount")
                .match("large", "high");
        
        rabbitAdmin.declareExchange(headersExchange);
        rabbitAdmin.declareQueue(urgentQueue);
        rabbitAdmin.declareQueue(largeOrderQueue);
        rabbitAdmin.declareBinding(urgentBinding);
        rabbitAdmin.declareBinding(largeOrderBinding);
    }
    
    // Delayed Message Exchange
    public void setupDelayedMessageExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        
        CustomExchange delayedExchange = new CustomExchange(
                "order.delayed.exchange",
                "x-delayed-message",
                true,
                false,
                args);
        
        Queue delayedQueue = QueueBuilder.durable("order.delayed.queue").build();
        
        Binding delayedBinding = BindingBuilder.bind(delayedQueue)
                .to(delayedExchange)
                .with("order.delayed")
                .noargs();
        
        rabbitAdmin.declareExchange(delayedExchange);
        rabbitAdmin.declareQueue(delayedQueue);
        rabbitAdmin.declareBinding(delayedBinding);
    }
    
    public void sendDelayedMessage(Object message, int delaySeconds) {
        rabbitTemplate.convertAndSend(
                "order.delayed.exchange",
                "order.delayed",
                message,
                msg -> {
                    msg.getMessageProperties().setDelay(delaySeconds * 1000);
                    return msg;
                });
    }
}
```

### RabbitMQ Producer with High Availability

```java
@Component
public class HAOrderProducer {
    
    private final RabbitTemplate rabbitTemplate;
    private final ConfirmationTracker confirmationTracker;
    
    public HAOrderProducer(RabbitTemplate rabbitTemplate, ConfirmationTracker confirmationTracker) {
        this.rabbitTemplate = rabbitTemplate;
        this.confirmationTracker = confirmationTracker;
    }
    
    public void sendOrderWithConfirmation(Order order) {
        try {
            String correlationId = UUID.randomUUID().toString();
            CorrelationData correlationData = new CorrelationData(correlationId);
            
            // Track confirmation
            confirmationTracker.addPendingConfirmation(correlationId, order);
            
            rabbitTemplate.convertAndSend(
                    "order.exchange",
                    "order.created",
                    order,
                    message -> {
                        message.getMessageProperties().setMessageId(correlationId);
                        message.getMessageProperties().setTimestamp(new Date());
                        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        message.getMessageProperties().setPriority(getPriority(order));
                        return message;
                    },
                    correlationData
            );
            
            System.out.println("Order sent with correlation ID: " + correlationId);
            
        } catch (Exception e) {
            System.err.println("Failed to send order: " + e.getMessage());
            throw new MessageSendException("Failed to send order", e);
        }
    }
    
    public void sendBatchOrders(List<Order> orders) {
        try {
            rabbitTemplate.execute(channel -> {
                channel.confirmSelect();
                
                for (Order order : orders) {
                    AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                            .messageId(UUID.randomUUID().toString())
                            .timestamp(new Date())
                            .deliveryMode(2) // Persistent
                            .priority(getPriority(order))
                            .build();
                    
                    byte[] messageBody = convertToBytes(order);
                    channel.basicPublish("order.exchange", "order.created", props, messageBody);
                }
                
                boolean allConfirmed = channel.waitForConfirms(5000);
                if (!allConfirmed) {
                    throw new MessageSendException("Not all messages were confirmed");
                }
                
                return null;
            });
            
            System.out.println("Batch of " + orders.size() + " orders sent successfully");
            
        } catch (Exception e) {
            System.err.println("Failed to send batch orders: " + e.getMessage());
            throw new MessageSendException("Failed to send batch orders", e);
        }
    }
    
    private int getPriority(Order order) {
        switch (order.getPriority()) {
            case "HIGH": return 10;
            case "MEDIUM": return 5;
            default: return 1;
        }
    }
    
    private byte[] convertToBytes(Order order) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsBytes(order);
        } catch (Exception e) {
            throw new MessageConversionException("Failed to convert order to bytes", e);
        }
    }
}
```

## 3. ActiveMQ Advanced Configuration

### ActiveMQ Network of Brokers

```xml
<!-- activemq.xml -->
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                          http://www.springframework.org/schema/beans/spring-beans.xsd">

    <broker xmlns="http://activemq.apache.org/schema/core" 
            brokerName="broker1" 
            dataDirectory="${activemq.data}"
            schedulerSupport="true">
        
        <!-- Network connectors for clustering -->
        <networkConnectors>
            <networkConnector 
                name="broker1-broker2" 
                uri="static:(tcp://broker2:61616)"
                duplex="true"
                decreaseNetworkConsumerPriority="true"/>
            <networkConnector 
                name="broker1-broker3" 
                uri="static:(tcp://broker3:61616)"
                duplex="true"
                decreaseNetworkConsumerPriority="true"/>
        </networkConnectors>
        
        <!-- Destinations -->
        <destinationPolicy>
            <policyMap>
                <policyEntries>
                    <policyEntry queue="order.>" producerFlowControl="true" memoryLimit="64mb">
                        <networkBridgeFilterFactory>
                            <conditionalNetworkBridgeFilterFactory replayWhenNoConsumers="true"/>
                        </networkBridgeFilterFactory>
                        <deadLetterStrategy>
                            <individualDeadLetterStrategy queuePrefix="DLQ." useQueueForQueueMessages="true"/>
                        </deadLetterStrategy>
                    </policyEntry>
                    <policyEntry topic="order.>" producerFlowControl="true" memoryLimit="64mb"/>
                </policyEntries>
            </policyMap>
        </destinationPolicy>
        
        <!-- Persistence adapter -->
        <persistenceAdapter>
            <kahaDB directory="${activemq.data}/kahadb" 
                    journalMaxFileLength="16mb"
                    indexWriteBatchSize="1000"
                    enableIndexWriteAsync="true"/>
        </persistenceAdapter>
        
        <!-- Transport connectors -->
        <transportConnectors>
            <transportConnector name="openwire" uri="tcp://0.0.0.0:61616?maximumConnections=1000&amp;wireFormat.maxFrameSize=104857600"/>
            <transportConnector name="amqp" uri="amqp://0.0.0.0:5672?maximumConnections=1000"/>
            <transportConnector name="stomp" uri="stomp://0.0.0.0:61613?maximumConnections=1000"/>
            <transportConnector name="mqtt" uri="mqtt://0.0.0.0:1883?maximumConnections=1000"/>
            <transportConnector name="ws" uri="ws://0.0.0.0:61614?maximumConnections=1000"/>
        </transportConnectors>
        
        <!-- JMX management -->
        <managementContext>
            <managementContext createConnector="true" connectorPort="1099"/>
        </managementContext>
        
        <!-- Plugins -->
        <plugins>
            <statisticsBrokerPlugin/>
            <timeStampingBrokerPlugin ttlCeiling="86400000" zeroExpirationOverride="86400000"/>
        </plugins>
        
    </broker>
</beans>
```

### ActiveMQ Cluster Configuration

```java
@Configuration
public class ActiveMQClusterConfig {
    
    @Bean
    @Primary
    public ConnectionFactory activeMQConnectionFactory() {
        PooledConnectionFactory pooled = new PooledConnectionFactory();
        
        // Failover transport with cluster members
        String failoverUrl = "failover:(tcp://broker1:61616,tcp://broker2:61616,tcp://broker3:61616)" +
                "?randomize=true&timeout=30000&initialReconnectDelay=1000&maxReconnectDelay=30000";
        
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(failoverUrl);
        factory.setUserName("admin");
        factory.setPassword("admin");
        
        // Optimizations
        factory.setUseAsyncSend(true);
        factory.setOptimizeAcknowledge(true);
        factory.setAlwaysSessionAsync(true);
        factory.setOptimizedMessageDispatch(true);
        
        pooled.setConnectionFactory(factory);
        pooled.setMaxConnections(10);
        pooled.setIdleTimeout(30000);
        pooled.setReconnectOnException(true);
        
        return pooled;
    }
    
    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setDefaultDestinationName("order.queue");
        template.setSessionTransacted(true);
        template.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
        template.setReceiveTimeout(5000);
        template.setDeliveryPersistent(true);
        template.setExplicitQosEnabled(true);
        template.setPriority(4);
        template.setTimeToLive(86400000); // 24 hours
        return template;
    }
    
    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrency("3-10");
        factory.setSessionTransacted(true);
        factory.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
        factory.setErrorHandler(new ActiveMQErrorHandler());
        factory.setReceiveTimeout(5000L);
        factory.setRecoveryInterval(5000L);
        return factory;
    }
}
```

### ActiveMQ Producer with Clustering

```java
@Component
public class ClusteredActiveMQProducer {
    
    private final JmsTemplate jmsTemplate;
    private final LoadBalancingDestinationSelector destinationSelector;
    
    public ClusteredActiveMQProducer(JmsTemplate jmsTemplate,
                                    LoadBalancingDestinationSelector destinationSelector) {
        this.jmsTemplate = jmsTemplate;
        this.destinationSelector = destinationSelector;
    }
    
    public void sendOrderWithLoadBalancing(Order order) {
        try {
            String destination = destinationSelector.selectDestination(order);
            
            jmsTemplate.send(destination, session -> {
                ObjectMessage message = session.createObjectMessage(order);
                message.setStringProperty("orderId", order.getId().toString());
                message.setStringProperty("customerId", order.getCustomerId().toString());
                message.setStringProperty("region", order.getRegion());
                message.setIntProperty("priority", getPriority(order));
                message.setLongProperty("timestamp", System.currentTimeMillis());
                
                // Set TTL based on order type
                if ("EXPRESS".equals(order.getType())) {
                    message.setJMSExpiration(System.currentTimeMillis() + 3600000); // 1 hour
                } else {
                    message.setJMSExpiration(System.currentTimeMillis() + 86400000); // 24 hours
                }
                
                return message;
            });
            
            System.out.println("Order sent to destination: " + destination);
            
        } catch (Exception e) {
            System.err.println("Failed to send order: " + e.getMessage());
            throw new MessageSendException("Failed to send order with load balancing", e);
        }
    }
    
    public void sendOrderWithTransaction(Order order, List<OrderEvent> events) {
        try {
            jmsTemplate.execute(session -> {
                try {
                    session.getTransacted();
                    
                    // Send order
                    MessageProducer orderProducer = session.createProducer(
                        session.createQueue("order.queue"));
                    ObjectMessage orderMessage = session.createObjectMessage(order);
                    orderProducer.send(orderMessage);
                    
                    // Send events
                    MessageProducer eventProducer = session.createProducer(
                        session.createTopic("order.events"));
                    for (OrderEvent event : events) {
                        ObjectMessage eventMessage = session.createObjectMessage(event);
                        eventProducer.send(eventMessage);
                    }
                    
                    session.commit();
                    System.out.println("Order and events sent in transaction");
                    
                } catch (Exception e) {
                    session.rollback();
                    throw new MessageSendException("Transaction failed", e);
                }
                
                return null;
            });
            
        } catch (Exception e) {
            System.err.println("Failed to send order with transaction: " + e.getMessage());
            throw new MessageSendException("Failed to send order with transaction", e);
        }
    }
    
    private int getPriority(Order order) {
        switch (order.getPriority()) {
            case "HIGH": return 9;
            case "MEDIUM": return 5;
            default: return 1;
        }
    }
}
```

## 4. Message Broker Performance Optimization

### RabbitMQ Performance Tuning

```java
@Configuration
public class RabbitMQPerformanceConfig {
    
    @Bean
    public CachingConnectionFactory performanceConnectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setAddresses("localhost:5672");
        factory.setUsername("admin");
        factory.setPassword("admin");
        
        // Performance optimizations
        factory.setChannelCacheSize(50);
        factory.setConnectionCacheSize(5);
        factory.setChannelCheckoutTimeout(5000);
        factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        factory.setPublisherReturns(true);
        
        // Connection tuning
        factory.getRabbitConnectionFactory().setRequestedChannelMax(2047);
        factory.getRabbitConnectionFactory().setRequestedFrameMax(131072);
        factory.getRabbitConnectionFactory().setRequestedHeartBeat(60);
        factory.getRabbitConnectionFactory().setConnectionTimeout(30000);
        factory.getRabbitConnectionFactory().setHandshakeTimeout(10000);
        
        return factory;
    }
    
    @Bean
    public SimpleRabbitListenerContainerFactory highThroughputFactory(
            CachingConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        
        // Performance settings
        factory.setConcurrentConsumers(10);
        factory.setMaxConcurrentConsumers(20);
        factory.setPrefetchCount(250);
        factory.setTxSize(10);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setAdviceChain(retryInterceptor());
        
        // Batch listener
        factory.setBatchListener(true);
        factory.setBatchSize(50);
        factory.setConsumerBatchEnabled(true);
        factory.setReceiveTimeout(5000L);
        
        return factory;
    }
    
    @Bean
    public RetryOperationsInterceptor retryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(1000, 2.0, 10000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }
}
```

### ActiveMQ Performance Tuning

```java
@Configuration
public class ActiveMQPerformanceConfig {
    
    @Bean
    public ConnectionFactory performanceActiveMQConnectionFactory() {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(
            "failover:(tcp://localhost:61616)?jms.prefetchPolicy.all=1000&jms.optimizeAcknowledge=true");
        
        // Performance optimizations
        factory.setUseAsyncSend(true);
        factory.setOptimizeAcknowledge(true);
        factory.setOptimizedMessageDispatch(true);
        factory.setAlwaysSessionAsync(true);
        factory.setUseCompression(true);
        factory.setObjectMessageSerializationDefered(true);
        
        // Prefetch policies
        ActiveMQPrefetchPolicy prefetchPolicy = new ActiveMQPrefetchPolicy();
        prefetchPolicy.setQueuePrefetch(1000);
        prefetchPolicy.setTopicPrefetch(32766);
        prefetchPolicy.setDurableTopicPrefetch(100);
        prefetchPolicy.setQueueBrowserPrefetch(500);
        factory.setPrefetchPolicy(prefetchPolicy);
        
        // Redelivery policy
        RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
        redeliveryPolicy.setMaximumRedeliveries(3);
        redeliveryPolicy.setInitialRedeliveryDelay(1000);
        redeliveryPolicy.setBackOffMultiplier(2.0);
        redeliveryPolicy.setUseExponentialBackOff(true);
        redeliveryPolicy.setMaximumRedeliveryDelay(60000);
        factory.setRedeliveryPolicy(redeliveryPolicy);
        
        return factory;
    }
    
    @Bean
    public DefaultJmsListenerContainerFactory performanceJmsFactory(
            ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        
        // Performance settings
        factory.setConcurrency("5-20");
        factory.setSessionTransacted(false);
        factory.setSessionAcknowledgeMode(Session.DUPS_OK_ACKNOWLEDGE);
        factory.setCacheLevel(DefaultMessageListenerContainer.CACHE_CONSUMER);
        factory.setReceiveTimeout(1000L);
        factory.setIdleConsumerLimit(5);
        factory.setIdleTaskExecutionLimit(10);
        
        return factory;
    }
}
```

## 5. Monitoring and Management

### RabbitMQ Monitoring

```java
@Component
public class RabbitMQMonitoring {
    
    private final MeterRegistry meterRegistry;
    private final RabbitAdmin rabbitAdmin;
    
    public RabbitMQMonitoring(MeterRegistry meterRegistry, RabbitAdmin rabbitAdmin) {
        this.meterRegistry = meterRegistry;
        this.rabbitAdmin = rabbitAdmin;
    }
    
    @Scheduled(fixedDelay = 30000)
    public void collectMetrics() {
        try {
            // Queue metrics
            Properties queueProperties = rabbitAdmin.getQueueProperties("order.queue");
            if (queueProperties != null) {
                int messageCount = (Integer) queueProperties.get("QUEUE_MESSAGE_COUNT");
                int consumerCount = (Integer) queueProperties.get("QUEUE_CONSUMER_COUNT");
                
                meterRegistry.gauge("rabbitmq.queue.message.count", 
                    Tags.of("queue", "order.queue"), messageCount);
                meterRegistry.gauge("rabbitmq.queue.consumer.count", 
                    Tags.of("queue", "order.queue"), consumerCount);
            }
            
            // Connection metrics
            collectConnectionMetrics();
            
        } catch (Exception e) {
            System.err.println("Failed to collect RabbitMQ metrics: " + e.getMessage());
        }
    }
    
    private void collectConnectionMetrics() {
        // Use RabbitMQ Management API to collect detailed metrics
        RestTemplate restTemplate = new RestTemplate();
        String managementUrl = "http://localhost:15672/api";
        
        try {
            // Get overview
            ResponseEntity<String> overview = restTemplate.exchange(
                managementUrl + "/overview",
                HttpMethod.GET,
                createAuthEntity(),
                String.class
            );
            
            // Parse and record metrics
            ObjectMapper mapper = new ObjectMapper();
            JsonNode overviewNode = mapper.readTree(overview.getBody());
            
            JsonNode messageStats = overviewNode.get("message_stats");
            if (messageStats != null) {
                int publishRate = messageStats.get("publish_details").get("rate").asInt();
                int deliverRate = messageStats.get("deliver_get_details").get("rate").asInt();
                
                meterRegistry.gauge("rabbitmq.message.publish.rate", publishRate);
                meterRegistry.gauge("rabbitmq.message.deliver.rate", deliverRate);
            }
            
        } catch (Exception e) {
            System.err.println("Failed to collect connection metrics: " + e.getMessage());
        }
    }
    
    private HttpEntity<String> createAuthEntity() {
        HttpHeaders headers = new HttpHeaders();
        String auth = "admin:admin";
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
        String authHeader = "Basic " + new String(encodedAuth);
        headers.set("Authorization", authHeader);
        return new HttpEntity<>(headers);
    }
    
    @EventListener
    public void handleRabbitConnectionEvent(RabbitConnectionFailureEvent event) {
        meterRegistry.counter("rabbitmq.connection.failures").increment();
        System.err.println("RabbitMQ connection failure: " + event.getCause());
    }
}
```

### ActiveMQ Monitoring

```java
@Component
public class ActiveMQMonitoring {
    
    private final MeterRegistry meterRegistry;
    private MBeanServerConnection mBeanConnection;
    
    public ActiveMQMonitoring(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        initJMXConnection();
    }
    
    private void initJMXConnection() {
        try {
            JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi");
            JMXConnector connector = JMXConnectorFactory.connect(url);
            mBeanConnection = connector.getMBeanServerConnection();
        } catch (Exception e) {
            System.err.println("Failed to initialize JMX connection: " + e.getMessage());
        }
    }
    
    @Scheduled(fixedDelay = 30000)
    public void collectActiveMQMetrics() {
        try {
            collectBrokerMetrics();
            collectQueueMetrics();
            collectTopicMetrics();
        } catch (Exception e) {
            System.err.println("Failed to collect ActiveMQ metrics: " + e.getMessage());
        }
    }
    
    private void collectBrokerMetrics() throws Exception {
        ObjectName brokerObjectName = new ObjectName("org.apache.activemq:type=Broker,brokerName=*");
        Set<ObjectInstance> brokers = mBeanConnection.queryMBeans(brokerObjectName, null);
        
        for (ObjectInstance broker : brokers) {
            ObjectName name = broker.getObjectName();
            
            Long totalMessageCount = (Long) mBeanConnection.getAttribute(name, "TotalMessageCount");
            Long totalConsumerCount = (Long) mBeanConnection.getAttribute(name, "TotalConsumerCount");
            Long totalProducerCount = (Long) mBeanConnection.getAttribute(name, "TotalProducerCount");
            Long memoryUsage = (Long) mBeanConnection.getAttribute(name, "MemoryPercentUsage");
            Long storeUsage = (Long) mBeanConnection.getAttribute(name, "StorePercentUsage");
            
            String brokerName = name.getKeyProperty("brokerName");
            
            meterRegistry.gauge("activemq.broker.total.messages", 
                Tags.of("broker", brokerName), totalMessageCount);
            meterRegistry.gauge("activemq.broker.total.consumers", 
                Tags.of("broker", brokerName), totalConsumerCount);
            meterRegistry.gauge("activemq.broker.total.producers", 
                Tags.of("broker", brokerName), totalProducerCount);
            meterRegistry.gauge("activemq.broker.memory.usage", 
                Tags.of("broker", brokerName), memoryUsage);
            meterRegistry.gauge("activemq.broker.store.usage", 
                Tags.of("broker", brokerName), storeUsage);
        }
    }
    
    private void collectQueueMetrics() throws Exception {
        ObjectName queueObjectName = new ObjectName("org.apache.activemq:type=Broker,brokerName=*,destinationType=Queue,destinationName=*");
        Set<ObjectInstance> queues = mBeanConnection.queryMBeans(queueObjectName, null);
        
        for (ObjectInstance queue : queues) {
            ObjectName name = queue.getObjectName();
            
            Long queueSize = (Long) mBeanConnection.getAttribute(name, "QueueSize");
            Long consumerCount = (Long) mBeanConnection.getAttribute(name, "ConsumerCount");
            Long producerCount = (Long) mBeanConnection.getAttribute(name, "ProducerCount");
            Long enqueueCount = (Long) mBeanConnection.getAttribute(name, "EnqueueCount");
            Long dequeueCount = (Long) mBeanConnection.getAttribute(name, "DequeueCount");
            
            String queueName = name.getKeyProperty("destinationName");
            String brokerName = name.getKeyProperty("brokerName");
            
            meterRegistry.gauge("activemq.queue.size", 
                Tags.of("queue", queueName, "broker", brokerName), queueSize);
            meterRegistry.gauge("activemq.queue.consumers", 
                Tags.of("queue", queueName, "broker", brokerName), consumerCount);
            meterRegistry.gauge("activemq.queue.producers", 
                Tags.of("queue", queueName, "broker", brokerName), producerCount);
            meterRegistry.gauge("activemq.queue.enqueue.count", 
                Tags.of("queue", queueName, "broker", brokerName), enqueueCount);
            meterRegistry.gauge("activemq.queue.dequeue.count", 
                Tags.of("queue", queueName, "broker", brokerName), dequeueCount);
        }
    }
    
    private void collectTopicMetrics() throws Exception {
        ObjectName topicObjectName = new ObjectName("org.apache.activemq:type=Broker,brokerName=*,destinationType=Topic,destinationName=*");
        Set<ObjectInstance> topics = mBeanConnection.queryMBeans(topicObjectName, null);
        
        for (ObjectInstance topic : topics) {
            ObjectName name = topic.getObjectName();
            
            Long consumerCount = (Long) mBeanConnection.getAttribute(name, "ConsumerCount");
            Long producerCount = (Long) mBeanConnection.getAttribute(name, "ProducerCount");
            Long enqueueCount = (Long) mBeanConnection.getAttribute(name, "EnqueueCount");
            Long dequeueCount = (Long) mBeanConnection.getAttribute(name, "DequeueCount");
            
            String topicName = name.getKeyProperty("destinationName");
            String brokerName = name.getKeyProperty("brokerName");
            
            meterRegistry.gauge("activemq.topic.consumers", 
                Tags.of("topic", topicName, "broker", brokerName), consumerCount);
            meterRegistry.gauge("activemq.topic.producers", 
                Tags.of("topic", topicName, "broker", brokerName), producerCount);
            meterRegistry.gauge("activemq.topic.enqueue.count", 
                Tags.of("topic", topicName, "broker", brokerName), enqueueCount);
            meterRegistry.gauge("activemq.topic.dequeue.count", 
                Tags.of("topic", topicName, "broker", brokerName), dequeueCount);
        }
    }
}
```

## 6. Health Checks and Alerting

### Comprehensive Health Checks

```java
@Component
public class MessageBrokerHealthIndicator implements HealthIndicator {
    
    private final RabbitTemplate rabbitTemplate;
    private final JmsTemplate jmsTemplate;
    private final MeterRegistry meterRegistry;
    
    public MessageBrokerHealthIndicator(RabbitTemplate rabbitTemplate,
                                       JmsTemplate jmsTemplate,
                                       MeterRegistry meterRegistry) {
        this.rabbitTemplate = rabbitTemplate;
        this.jmsTemplate = jmsTemplate;
        this.meterRegistry = meterRegistry;
    }
    
    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        
        // Check RabbitMQ
        try {
            checkRabbitMQHealth(builder);
        } catch (Exception e) {
            builder.down().withDetail("rabbitmq.error", e.getMessage());
        }
        
        // Check ActiveMQ
        try {
            checkActiveMQHealth(builder);
        } catch (Exception e) {
            builder.down().withDetail("activemq.error", e.getMessage());
        }
        
        return builder.build();
    }
    
    private void checkRabbitMQHealth(Health.Builder builder) {
        // Test RabbitMQ connection
        rabbitTemplate.execute(channel -> {
            channel.queueDeclarePassive("order.queue");
            return null;
        });
        
        builder.withDetail("rabbitmq.status", "UP");
        
        // Check queue depths
        Properties queueProps = ((RabbitAdmin) rabbitTemplate.getConnectionFactory())
                .getQueueProperties("order.queue");
        if (queueProps != null) {
            int messageCount = (Integer) queueProps.get("QUEUE_MESSAGE_COUNT");
            builder.withDetail("rabbitmq.queue.order.messages", messageCount);
            
            if (messageCount > 10000) {
                builder.withDetail("rabbitmq.queue.order.warning", "High message count");
            }
        }
    }
    
    private void checkActiveMQHealth(Health.Builder builder) {
        // Test ActiveMQ connection
        jmsTemplate.execute(session -> {
            session.createQueue("order.queue");
            return null;
        });
        
        builder.withDetail("activemq.status", "UP");
        
        // Additional ActiveMQ health checks would go here
        builder.withDetail("activemq.broker", "localhost:61616");
    }
}
```

### Alerting Configuration

```java
@Component
public class MessageBrokerAlerting {
    
    private final MeterRegistry meterRegistry;
    private final NotificationService notificationService;
    
    public MessageBrokerAlerting(MeterRegistry meterRegistry, NotificationService notificationService) {
        this.meterRegistry = meterRegistry;
        this.notificationService = notificationService;
    }
    
    @EventListener
    public void handleHighQueueDepth(MeterRegistryEvent event) {
        // Custom event for high queue depth
        if (event.getMeter().getId().getName().equals("rabbitmq.queue.message.count")) {
            double messageCount = event.getMeter().measure().iterator().next().getValue();
            
            if (messageCount > 10000) {
                Alert alert = new Alert(
                    AlertLevel.WARNING,
                    "High RabbitMQ queue depth",
                    String.format("Queue depth: %.0f messages", messageCount),
                    LocalDateTime.now()
                );
                
                notificationService.sendAlert(alert);
            }
        }
    }
    
    @Scheduled(fixedDelay = 60000)
    public void checkBrokerHealth() {
        // Check RabbitMQ connection failures
        Counter rabbitFailures = meterRegistry.find("rabbitmq.connection.failures").counter();
        if (rabbitFailures != null && rabbitFailures.count() > 5) {
            Alert alert = new Alert(
                AlertLevel.CRITICAL,
                "Multiple RabbitMQ connection failures",
                String.format("Failure count: %.0f", rabbitFailures.count()),
                LocalDateTime.now()
            );
            
            notificationService.sendAlert(alert);
        }
        
        // Check message processing rates
        checkMessageProcessingRates();
    }
    
    private void checkMessageProcessingRates() {
        Gauge publishRate = meterRegistry.find("rabbitmq.message.publish.rate").gauge();
        Gauge deliverRate = meterRegistry.find("rabbitmq.message.deliver.rate").gauge();
        
        if (publishRate != null && deliverRate != null) {
            double publishValue = publishRate.value();
            double deliverValue = deliverRate.value();
            
            // Alert if delivery rate is significantly lower than publish rate
            if (publishValue > 0 && deliverValue < publishValue * 0.5) {
                Alert alert = new Alert(
                    AlertLevel.WARNING,
                    "Message processing lag detected",
                    String.format("Publish rate: %.2f, Deliver rate: %.2f", publishValue, deliverValue),
                    LocalDateTime.now()
                );
                
                notificationService.sendAlert(alert);
            }
        }
    }
}
```

## 7. Testing Message Broker Implementations

### Integration Testing

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.rabbitmq.host=localhost",
    "spring.rabbitmq.port=5672",
    "spring.activemq.broker-url=tcp://localhost:61616"
})
class MessageBrokerIntegrationTest {
    
    @Autowired
    private HAOrderProducer rabbitProducer;
    
    @Autowired
    private ClusteredActiveMQProducer activeMQProducer;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private JmsTemplate jmsTemplate;
    
    @Test
    void testRabbitMQHighAvailability() throws InterruptedException {
        // Given
        Order order = new Order(1L, "customer-123", "product-456", 2, BigDecimal.valueOf(99.99));
        
        // When
        rabbitProducer.sendOrderWithConfirmation(order);
        
        // Then
        Thread.sleep(2000); // Wait for processing
        
        // Verify message was processed
        Object received = rabbitTemplate.receiveAndConvert("order.queue", 5000);
        assertThat(received).isNotNull();
    }
    
    @Test
    void testActiveMQClustering() {
        // Given
        Order order = new Order(2L, "customer-456", "product-789", 1, BigDecimal.valueOf(49.99));
        List<OrderEvent> events = Arrays.asList(
            new OrderCreatedEvent(order.getId().toString(), order.getCustomerId().toString())
        );
        
        // When
        activeMQProducer.sendOrderWithTransaction(order, events);
        
        // Then
        // Verify both order and events were sent
        Order receivedOrder = (Order) jmsTemplate.receiveAndConvert("order.queue");
        assertThat(receivedOrder).isNotNull();
        assertThat(receivedOrder.getId()).isEqualTo(2L);
    }
    
    @Test
    void testMessageBrokerFailover() {
        // Test failover scenarios
        // This would require actually stopping broker instances
        // and verifying that the application continues to work
    }
}
```

## Summary

Today we covered:

1. **RabbitMQ Advanced Configuration**: Clustering, high availability, and performance tuning
2. **Advanced RabbitMQ Patterns**: Topic routing, headers exchange, delayed messages
3. **ActiveMQ Advanced Configuration**: Network of brokers and clustering
4. **Performance Optimization**: Tuning for high throughput and low latency
5. **Monitoring and Management**: Comprehensive metrics collection and analysis
6. **Health Checks and Alerting**: Proactive monitoring and alerting systems
7. **Testing**: Integration testing strategies for clustered environments

## Key Takeaways

- Message broker clustering provides high availability and fault tolerance
- Advanced routing patterns enable sophisticated message distribution
- Performance tuning is crucial for production deployments
- Comprehensive monitoring and alerting are essential for operational visibility
- Proper testing strategies ensure reliability in clustered environments
- Both RabbitMQ and ActiveMQ offer robust clustering and high availability features

## Next Steps

Tomorrow we'll explore Reactive Messaging and WebSockets, learning how to build real-time, reactive applications with non-blocking message processing and bidirectional communication channels.