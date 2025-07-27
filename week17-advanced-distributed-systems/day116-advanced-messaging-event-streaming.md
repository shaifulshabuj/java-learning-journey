# Day 116: Advanced Messaging & Event Streaming - Apache Pulsar, Event Mesh & Real-time Communication

## Overview
Day 116 explores advanced messaging and event streaming architectures, focusing on Apache Pulsar, Event Mesh patterns, and real-time communication systems that enable ultra-scalable, fault-tolerant distributed applications.

## Learning Objectives
- Master Apache Pulsar for multi-tenant, cloud-native messaging
- Implement Event Mesh architectures for complex event routing
- Build real-time communication systems with WebSocket and Server-Sent Events
- Design event-driven architectures with complex event processing
- Apply advanced streaming patterns for high-throughput scenarios

## Key Concepts

### 1. Apache Pulsar Architecture
- Multi-tenant messaging platform
- Geo-replication and disaster recovery
- Schema evolution and validation
- Functions and connectors ecosystem
- Tiered storage and infinite retention

### 2. Event Mesh Patterns
- Distributed event routing
- Event transformation and enrichment
- Cross-system event correlation
- Event choreography vs orchestration
- Real-time event processing

### 3. Advanced Streaming
- Complex Event Processing (CEP)
- Event sourcing with streaming
- Real-time analytics and aggregation
- Stream processing optimization
- Backpressure and flow control

## Implementation

### Apache Pulsar Advanced Integration

```java
import org.apache.pulsar.client.api.*;
import org.apache.pulsar.client.api.schema.*;
import org.apache.pulsar.functions.api.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.util.concurrent.*;
import java.util.function.*;
import reactor.core.publisher.*;

@Configuration
@ConditionalOnProperty("pulsar.enabled")
public class PulsarConfiguration {
    
    @Bean
    public PulsarClient pulsarClient(@Value("${pulsar.service-url}") String serviceUrl) {
        return PulsarClient.builder()
            .serviceUrl(serviceUrl)
            .authentication(AuthenticationFactory.token("${pulsar.auth-token}"))
            .enableTls(true)
            .tlsTrustCertsFilePath("${pulsar.tls-cert-path}")
            .operationTimeout(30, TimeUnit.SECONDS)
            .connectionTimeout(30, TimeUnit.SECONDS)
            .maxNumberOfRejectedRequestPerConnection(50)
            .keepAliveInterval(30, TimeUnit.SECONDS)
            .build();
    }
    
    @Bean
    public AdvancedPulsarService advancedPulsarService(PulsarClient client) {
        return new AdvancedPulsarService(client);
    }
}

@Service
@Slf4j
public class AdvancedPulsarService {
    
    private final PulsarClient client;
    private final Map<String, Producer<?>> producers = new ConcurrentHashMap<>();
    private final Map<String, Consumer<?>> consumers = new ConcurrentHashMap<>();
    private final SchemaRegistry schemaRegistry;
    
    public AdvancedPulsarService(PulsarClient client) {
        this.client = client;
        this.schemaRegistry = new SchemaRegistry();
    }
    
    /**
     * Multi-tenant topic management with schema evolution
     */
    public <T> CompletableFuture<Producer<T>> createProducer(
            String tenant, 
            String namespace, 
            String topic,
            Schema<T> schema,
            ProducerConfig config) {
        
        String fullTopicName = String.format("persistent://%s/%s/%s", tenant, namespace, topic);
        
        return client.newProducer(schema)
            .topic(fullTopicName)
            .producerName(config.getProducerName())
            .sendTimeout(config.getSendTimeoutMs(), TimeUnit.MILLISECONDS)
            .blockIfQueueFull(config.isBlockIfQueueFull())
            .maxPendingMessages(config.getMaxPendingMessages())
            .batchingMaxMessages(config.getBatchSize())
            .batchingMaxPublishDelay(config.getBatchDelayMs(), TimeUnit.MILLISECONDS)
            .compressionType(config.getCompressionType())
            .hashingScheme(HashingScheme.Murmur3_32Hash)
            .messageRoutingMode(MessageRoutingMode.RoundRobinPartition)
            .enableBatching(config.isBatchingEnabled())
            .createAsync()
            .whenComplete((producer, throwable) -> {
                if (throwable == null) {
                    producers.put(fullTopicName, producer);
                    log.info("Producer created for topic: {}", fullTopicName);
                } else {
                    log.error("Failed to create producer for topic: {}", fullTopicName, throwable);
                }
            });
    }
    
    /**
     * Advanced consumer with dead letter queue and retry policies
     */
    public <T> CompletableFuture<Consumer<T>> createConsumer(
            String tenant,
            String namespace, 
            String topic,
            String subscription,
            Schema<T> schema,
            ConsumerConfig config) {
        
        String fullTopicName = String.format("persistent://%s/%s/%s", tenant, namespace, topic);
        String dlqTopic = fullTopicName + "-dlq";
        String retryTopic = fullTopicName + "-retry";
        
        DeadLetterPolicy deadLetterPolicy = DeadLetterPolicy.builder()
            .maxRedeliverCount(config.getMaxRedeliveryCount())
            .deadLetterTopic(dlqTopic)
            .retryLetterTopic(retryTopic)
            .build();
        
        return client.newConsumer(schema)
            .topic(fullTopicName)
            .subscriptionName(subscription)
            .subscriptionType(config.getSubscriptionType())
            .subscriptionInitialPosition(SubscriptionInitialPosition.Latest)
            .consumerName(config.getConsumerName())
            .receiverQueueSize(config.getReceiverQueueSize())
            .maxTotalReceiverQueueSizeAcrossPartitions(config.getMaxTotalReceiverQueueSize())
            .ackTimeout(config.getAckTimeoutMs(), TimeUnit.MILLISECONDS)
            .ackTimeoutTickTime(1, TimeUnit.SECONDS)
            .negativeAckRedeliveryDelay(config.getNegativeAckRedeliveryDelayMs(), TimeUnit.MILLISECONDS)
            .deadLetterPolicy(deadLetterPolicy)
            .enableRetry(true)
            .maxRedeliverCount(config.getMaxRedeliveryCount())
            .subscribeAsync()
            .whenComplete((consumer, throwable) -> {
                if (throwable == null) {
                    consumers.put(fullTopicName + ":" + subscription, consumer);
                    log.info("Consumer created for topic: {} with subscription: {}", 
                        fullTopicName, subscription);
                } else {
                    log.error("Failed to create consumer for topic: {}", fullTopicName, throwable);
                }
            });
    }
    
    /**
     * Schema-aware message publishing with validation
     */
    public <T> CompletableFuture<MessageId> publishMessage(
            String topicName, 
            T message,
            MessageMetadata metadata) {
        
        Producer<T> producer = (Producer<T>) producers.get(topicName);
        if (producer == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Producer not found for topic: " + topicName));
        }
        
        // Schema validation
        Schema<T> schema = producer.getSchema();
        if (!schemaRegistry.validateMessage(schema, message)) {
            return CompletableFuture.failedFuture(
                new SchemaValidationException("Message validation failed"));
        }
        
        MessageBuilder<T> messageBuilder = producer.newMessage()
            .value(message)
            .key(metadata.getKey())
            .orderingKey(metadata.getOrderingKey().getBytes())
            .eventTime(metadata.getEventTime());
        
        // Add properties
        metadata.getProperties().forEach(messageBuilder::property);
        
        // Add tracing headers
        messageBuilder.property("trace-id", generateTraceId());
        messageBuilder.property("span-id", generateSpanId());
        messageBuilder.property("timestamp", String.valueOf(System.currentTimeMillis()));
        
        return messageBuilder.sendAsync()
            .whenComplete((messageId, throwable) -> {
                if (throwable == null) {
                    log.debug("Message published successfully: {}", messageId);
                    updateMetrics(topicName, "published", 1);
                } else {
                    log.error("Failed to publish message to topic: {}", topicName, throwable);
                    updateMetrics(topicName, "publish_errors", 1);
                }
            });
    }
    
    /**
     * Reactive message consumption with backpressure handling
     */
    public <T> Flux<Message<T>> consumeMessages(String topicName, String subscription) {
        Consumer<T> consumer = (Consumer<T>) consumers.get(topicName + ":" + subscription);
        if (consumer == null) {
            return Flux.error(new IllegalStateException(
                "Consumer not found for topic: " + topicName));
        }
        
        return Flux.<Message<T>>create(sink -> {
            CompletableFuture<Void> receiveLoop = CompletableFuture.runAsync(() -> {
                while (!sink.isCancelled()) {
                    try {
                        Message<T> message = consumer.receive(100, TimeUnit.MILLISECONDS);
                        if (message != null) {
                            sink.next(message);
                        }
                    } catch (PulsarClientException e) {
                        if (!sink.isCancelled()) {
                            sink.error(e);
                        }
                        break;
                    }
                }
            });
            
            sink.onCancel(() -> receiveLoop.cancel(true));
        })
        .onBackpressureBuffer(10000, BufferOverflowStrategy.DROP_OLDEST)
        .doOnNext(message -> updateMetrics(topicName, "consumed", 1))
        .doOnError(error -> {
            log.error("Error consuming messages from topic: {}", topicName, error);
            updateMetrics(topicName, "consume_errors", 1);
        });
    }
    
    /**
     * Pulsar Functions for server-side processing
     */
    public void deployFunction(String functionName, FunctionConfig config) {
        try {
            PulsarAdmin admin = PulsarAdmin.builder()
                .serviceHttpUrl("https://pulsar-admin-url:8443")
                .authentication(AuthenticationFactory.token("admin-token"))
                .build();
            
            admin.functions().createFunction(config, "function.jar");
            log.info("Function deployed successfully: {}", functionName);
            
        } catch (Exception e) {
            log.error("Failed to deploy function: {}", functionName, e);
            throw new RuntimeException("Function deployment failed", e);
        }
    }
    
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    private String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    private void updateMetrics(String topic, String operation, int count) {
        // Metrics implementation
        Metrics.counter("pulsar." + operation, "topic", topic).increment(count);
    }
}

/**
 * Schema registry for managing schema evolution
 */
@Component
public class SchemaRegistry {
    
    private final Map<String, SchemaInfo> schemas = new ConcurrentHashMap<>();
    private final CompatibilityChecker compatibilityChecker = new CompatibilityChecker();
    
    public <T> boolean validateMessage(Schema<T> schema, T message) {
        try {
            byte[] serialized = schema.encode(message);
            T deserialized = schema.decode(serialized);
            return deserialized != null;
        } catch (Exception e) {
            log.error("Schema validation failed", e);
            return false;
        }
    }
    
    public boolean isCompatible(SchemaInfo oldSchema, SchemaInfo newSchema) {
        return compatibilityChecker.isCompatible(oldSchema, newSchema);
    }
    
    public void registerSchema(String topicName, SchemaInfo schemaInfo) {
        SchemaInfo existingSchema = schemas.get(topicName);
        if (existingSchema != null && !isCompatible(existingSchema, schemaInfo)) {
            throw new SchemaIncompatibilityException(
                "New schema is not compatible with existing schema");
        }
        schemas.put(topicName, schemaInfo);
    }
}
```

### Event Mesh Architecture

```java
@Service
@Slf4j
public class EventMeshService {
    
    private final AdvancedPulsarService pulsarService;
    private final EventRouter eventRouter;
    private final EventTransformer eventTransformer;
    private final EventCorrelationEngine correlationEngine;
    
    /**
     * Event mesh routing with intelligent topic selection
     */
    @Component
    public static class EventRouter {
        
        private final Map<String, RoutingRule> routingRules = new ConcurrentHashMap<>();
        private final BloomFilter<String> eventFilter = BloomFilter.create(
            Funnels.stringFunnel(Charset.defaultCharset()), 100000, 0.01);
        
        public List<String> routeEvent(DomainEvent event) {
            List<String> destinations = new ArrayList<>();
            
            // Apply routing rules
            for (RoutingRule rule : routingRules.values()) {
                if (rule.matches(event)) {
                    destinations.addAll(rule.getDestinations());
                }
            }
            
            // Apply content-based routing
            destinations.addAll(contentBasedRouting(event));
            
            // Apply machine learning-based routing if available
            destinations.addAll(mlBasedRouting(event));
            
            // Deduplicate destinations
            return destinations.stream().distinct().collect(Collectors.toList());
        }
        
        private List<String> contentBasedRouting(DomainEvent event) {
            List<String> destinations = new ArrayList<>();
            
            // Route based on event type
            String eventType = event.getEventType();
            destinations.add("persistent://events/global/by-type-" + eventType);
            
            // Route based on source system
            String source = event.getMetadata().getSource();
            destinations.add("persistent://events/global/by-source-" + source);
            
            // Route based on business domain
            String domain = extractBusinessDomain(event);
            destinations.add("persistent://events/business/" + domain + "/all");
            
            // Route based on priority
            Priority priority = event.getMetadata().getPriority();
            if (priority == Priority.HIGH || priority == Priority.CRITICAL) {
                destinations.add("persistent://events/priority/high-priority");
            }
            
            return destinations;
        }
        
        private List<String> mlBasedRouting(DomainEvent event) {
            // Placeholder for ML-based routing
            // In practice, this would use a trained model to predict optimal routing
            List<String> destinations = new ArrayList<>();
            
            // Simple heuristics for demonstration
            if (event.getPayload().containsKey("customerId")) {
                destinations.add("persistent://events/customer/customer-events");
            }
            
            if (event.getPayload().containsKey("orderId")) {
                destinations.add("persistent://events/order/order-events");
            }
            
            return destinations;
        }
        
        public void addRoutingRule(String ruleId, RoutingRule rule) {
            routingRules.put(ruleId, rule);
            log.info("Routing rule added: {}", ruleId);
        }
        
        private String extractBusinessDomain(DomainEvent event) {
            // Extract business domain from event type or payload
            String eventType = event.getEventType().toLowerCase();
            
            if (eventType.contains("order") || eventType.contains("payment")) {
                return "commerce";
            } else if (eventType.contains("user") || eventType.contains("customer")) {
                return "customer";
            } else if (eventType.contains("inventory") || eventType.contains("product")) {
                return "catalog";
            } else {
                return "general";
            }
        }
    }
    
    /**
     * Event transformation and enrichment engine
     */
    @Component
    public static class EventTransformer {
        
        private final Map<String, TransformationRule> transformationRules = new ConcurrentHashMap<>();
        private final EnrichmentService enrichmentService;
        
        public EventTransformer(EnrichmentService enrichmentService) {
            this.enrichmentService = enrichmentService;
        }
        
        public CompletableFuture<DomainEvent> transformEvent(
                DomainEvent event, 
                String destinationTopic) {
            
            return CompletableFuture.supplyAsync(() -> {
                // Apply transformation rules
                DomainEvent transformedEvent = applyTransformationRules(event, destinationTopic);
                
                // Enrich with additional data
                transformedEvent = enrichmentService.enrichEvent(transformedEvent);
                
                // Apply schema transformation if needed
                transformedEvent = applySchemaTransformation(transformedEvent, destinationTopic);
                
                return transformedEvent;
            });
        }
        
        private DomainEvent applyTransformationRules(DomainEvent event, String destinationTopic) {
            String ruleKey = event.getEventType() + ":" + destinationTopic;
            TransformationRule rule = transformationRules.get(ruleKey);
            
            if (rule != null) {
                return rule.transform(event);
            }
            
            return event;
        }
        
        private DomainEvent applySchemaTransformation(DomainEvent event, String destinationTopic) {
            // Schema transformation based on destination requirements
            if (destinationTopic.contains("analytics")) {
                return transformForAnalytics(event);
            } else if (destinationTopic.contains("archive")) {
                return transformForArchival(event);
            }
            
            return event;
        }
        
        private DomainEvent transformForAnalytics(DomainEvent event) {
            // Transform for analytics consumption
            Map<String, Object> analyticsPayload = new HashMap<>(event.getPayload());
            analyticsPayload.put("processedTimestamp", System.currentTimeMillis());
            analyticsPayload.put("analyticsVersion", "1.0");
            
            return event.toBuilder()
                .payload(analyticsPayload)
                .build();
        }
        
        private DomainEvent transformForArchival(DomainEvent event) {
            // Transform for long-term storage
            Map<String, Object> archivalPayload = new HashMap<>(event.getPayload());
            archivalPayload.put("archivedTimestamp", System.currentTimeMillis());
            archivalPayload.put("retentionPolicy", "7years");
            
            return event.toBuilder()
                .payload(archivalPayload)
                .build();
        }
        
        public void addTransformationRule(String eventType, String destinationTopic, 
                TransformationRule rule) {
            String key = eventType + ":" + destinationTopic;
            transformationRules.put(key, rule);
        }
    }
    
    /**
     * Event correlation for complex event processing
     */
    @Component
    public static class EventCorrelationEngine {
        
        private final Map<String, CorrelationWindow> correlationWindows = new ConcurrentHashMap<>();
        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
        
        public void processEvent(DomainEvent event) {
            // Extract correlation keys
            List<String> correlationKeys = extractCorrelationKeys(event);
            
            for (String correlationKey : correlationKeys) {
                CorrelationWindow window = correlationWindows.computeIfAbsent(
                    correlationKey, k -> createCorrelationWindow(k));
                
                window.addEvent(event);
                
                // Check for correlation patterns
                List<CorrelatedEventSet> correlatedSets = window.checkCorrelations();
                correlatedSets.forEach(this::publishCorrelatedEvents);
            }
        }
        
        private List<String> extractCorrelationKeys(DomainEvent event) {
            List<String> keys = new ArrayList<>();
            
            // Session-based correlation
            String sessionId = (String) event.getPayload().get("sessionId");
            if (sessionId != null) {
                keys.add("session:" + sessionId);
            }
            
            // User-based correlation
            String userId = (String) event.getPayload().get("userId");
            if (userId != null) {
                keys.add("user:" + userId);
            }
            
            // Transaction-based correlation
            String transactionId = (String) event.getPayload().get("transactionId");
            if (transactionId != null) {
                keys.add("transaction:" + transactionId);
            }
            
            // Temporal correlation (same minute)
            long timestamp = event.getTimestamp();
            long minuteWindow = timestamp / (60 * 1000) * (60 * 1000);
            keys.add("time:" + minuteWindow);
            
            return keys;
        }
        
        private CorrelationWindow createCorrelationWindow(String correlationKey) {
            CorrelationWindow window = new CorrelationWindow(correlationKey, Duration.ofMinutes(5));
            
            // Schedule window cleanup
            scheduler.scheduleAtFixedRate(() -> {
                window.cleanup();
                if (window.isEmpty()) {
                    correlationWindows.remove(correlationKey);
                }
            }, 1, 1, TimeUnit.MINUTES);
            
            return window;
        }
        
        private void publishCorrelatedEvents(CorrelatedEventSet eventSet) {
            DomainEvent correlatedEvent = DomainEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("CorrelatedEvents")
                .timestamp(System.currentTimeMillis())
                .payload(Map.of(
                    "correlationKey", eventSet.getCorrelationKey(),
                    "events", eventSet.getEvents(),
                    "correlationType", eventSet.getCorrelationType(),
                    "confidence", eventSet.getConfidence()
                ))
                .metadata(EventMetadata.builder()
                    .source("EventCorrelationEngine")
                    .version("1.0")
                    .build())
                .build();
            
            // Publish to correlation topic
            String correlationTopic = "persistent://events/correlation/correlated-events";
            // publishEvent(correlationTopic, correlatedEvent);
            
            log.info("Published correlated event set: {} with {} events", 
                eventSet.getCorrelationKey(), eventSet.getEvents().size());
        }
    }
    
    /**
     * Main event mesh processing method
     */
    public CompletableFuture<Void> processEvent(DomainEvent event) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Route the event to appropriate topics
                List<String> destinations = eventRouter.routeEvent(event);
                
                // Process correlation
                correlationEngine.processEvent(event);
                
                // Transform and publish to each destination
                List<CompletableFuture<Void>> publishFutures = destinations.stream()
                    .map(destination -> transformAndPublish(event, destination))
                    .collect(Collectors.toList());
                
                // Wait for all publications to complete
                CompletableFuture.allOf(publishFutures.toArray(new CompletableFuture[0]))
                    .join();
                
                log.debug("Event processed and routed to {} destinations", destinations.size());
                
            } catch (Exception e) {
                log.error("Error processing event in event mesh", e);
                throw new RuntimeException("Event mesh processing failed", e);
            }
        });
    }
    
    private CompletableFuture<Void> transformAndPublish(DomainEvent event, String destination) {
        return eventTransformer.transformEvent(event, destination)
            .thenCompose(transformedEvent -> {
                MessageMetadata metadata = MessageMetadata.builder()
                    .key(transformedEvent.getEventId())
                    .eventTime(transformedEvent.getTimestamp())
                    .orderingKey(extractOrderingKey(transformedEvent))
                    .properties(Map.of(
                        "eventType", transformedEvent.getEventType(),
                        "source", transformedEvent.getMetadata().getSource(),
                        "version", transformedEvent.getMetadata().getVersion()
                    ))
                    .build();
                
                return pulsarService.publishMessage(destination, transformedEvent, metadata);
            })
            .thenApply(messageId -> null);
    }
    
    private String extractOrderingKey(DomainEvent event) {
        // Extract ordering key for partitioning
        String userId = (String) event.getPayload().get("userId");
        if (userId != null) {
            return userId;
        }
        
        String sessionId = (String) event.getPayload().get("sessionId");
        if (sessionId != null) {
            return sessionId;
        }
        
        return event.getEventId();
    }
}
```

### Real-time Communication System

```java
@RestController
@RequestMapping("/api/realtime")
@Slf4j
public class RealTimeController {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final RealTimeConnectionManager connectionManager;
    private final EventStreamingService streamingService;
    
    /**
     * WebSocket endpoint for real-time communication
     */
    @MessageMapping("/connect")
    @SendToUser("/queue/notifications")
    public ConnectionResponse connect(ConnectionRequest request, Principal principal) {
        String userId = principal.getName();
        
        connectionManager.addConnection(userId, request.getSessionId());
        
        // Subscribe to user-specific events
        streamingService.subscribeToUserEvents(userId, event -> {
            sendEventToUser(userId, event);
        });
        
        return ConnectionResponse.builder()
            .status("CONNECTED")
            .userId(userId)
            .sessionId(request.getSessionId())
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * Server-Sent Events endpoint for real-time updates
     */
    @GetMapping(value = "/events/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamEvents(@PathVariable String userId) {
        return streamingService.getUserEventStream(userId)
            .map(event -> ServerSentEvent.<String>builder()
                .id(event.getEventId())
                .event(event.getEventType())
                .data(objectMapper.writeValueAsString(event))
                .retry(Duration.ofSeconds(1))
                .build())
            .onErrorResume(error -> {
                log.error("Error in event stream for user: {}", userId, error);
                return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("Stream error occurred")
                    .build());
            });
    }
    
    /**
     * WebRTC signaling for peer-to-peer communication
     */
    @MessageMapping("/webrtc/signal")
    @SendToUser("/queue/webrtc")
    public void handleWebRTCSignal(WebRTCSignal signal, Principal principal) {
        String userId = principal.getName();
        
        // Route signaling message to target user
        String targetUserId = signal.getTargetUserId();
        if (connectionManager.isUserConnected(targetUserId)) {
            messagingTemplate.convertAndSendToUser(
                targetUserId, 
                "/queue/webrtc", 
                signal.toBuilder().fromUserId(userId).build()
            );
        } else {
            // Store signaling message for later delivery
            connectionManager.storeSignalingMessage(targetUserId, signal);
        }
    }
    
    private void sendEventToUser(String userId, DomainEvent event) {
        if (connectionManager.isUserConnected(userId)) {
            messagingTemplate.convertAndSendToUser(
                userId, 
                "/queue/notifications", 
                event
            );
        }
    }
}

@Service
@Slf4j
public class RealTimeConnectionManager {
    
    private final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>();
    private final Map<String, UserConnection> connections = new ConcurrentHashMap<>();
    private final Map<String, Queue<WebRTCSignal>> pendingSignals = new ConcurrentHashMap<>();
    
    public void addConnection(String userId, String sessionId) {
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
            .add(sessionId);
        
        connections.put(sessionId, UserConnection.builder()
            .userId(userId)
            .sessionId(sessionId)
            .connectedAt(System.currentTimeMillis())
            .lastActivity(System.currentTimeMillis())
            .build());
        
        log.info("User {} connected with session {}", userId, sessionId);
        
        // Deliver pending signaling messages
        deliverPendingSignals(userId);
    }
    
    public void removeConnection(String userId, String sessionId) {
        Set<String> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
            }
        }
        
        connections.remove(sessionId);
        log.info("User {} disconnected session {}", userId, sessionId);
    }
    
    public boolean isUserConnected(String userId) {
        Set<String> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }
    
    public void storeSignalingMessage(String userId, WebRTCSignal signal) {
        pendingSignals.computeIfAbsent(userId, k -> new ConcurrentLinkedQueue<>())
            .offer(signal);
        
        // Limit queue size to prevent memory issues
        Queue<WebRTCSignal> queue = pendingSignals.get(userId);
        while (queue.size() > 100) {
            queue.poll();
        }
    }
    
    private void deliverPendingSignals(String userId) {
        Queue<WebRTCSignal> signals = pendingSignals.remove(userId);
        if (signals != null) {
            signals.forEach(signal -> {
                // Deliver pending WebRTC signaling messages
                messagingTemplate.convertAndSendToUser(
                    userId, "/queue/webrtc", signal);
            });
        }
    }
    
    @Scheduled(fixedRate = 60000) // Every minute
    public void cleanupInactiveConnections() {
        long cutoffTime = System.currentTimeMillis() - Duration.ofMinutes(10).toMillis();
        
        connections.entrySet().removeIf(entry -> {
            UserConnection connection = entry.getValue();
            if (connection.getLastActivity() < cutoffTime) {
                removeConnection(connection.getUserId(), connection.getSessionId());
                return true;
            }
            return false;
        });
    }
}

@Service
@Slf4j
public class EventStreamingService {
    
    private final AdvancedPulsarService pulsarService;
    private final Map<String, Flux<DomainEvent>> userStreams = new ConcurrentHashMap<>();
    private final Map<String, Disposable> streamSubscriptions = new ConcurrentHashMap<>();
    
    public void subscribeToUserEvents(String userId, Consumer<DomainEvent> eventHandler) {
        String subscriptionName = "user-events-" + userId;
        String topicName = "persistent://events/users/" + userId + "/notifications";
        
        Flux<Message<DomainEvent>> messageFlux = pulsarService.consumeMessages(
            topicName, subscriptionName);
        
        Disposable subscription = messageFlux
            .map(Message::getValue)
            .doOnNext(event -> {
                eventHandler.accept(event);
                log.debug("Event delivered to user {}: {}", userId, event.getEventType());
            })
            .doOnError(error -> log.error("Error in user event stream for {}", userId, error))
            .retry(3)
            .subscribe();
        
        streamSubscriptions.put(userId, subscription);
        log.info("Subscribed to events for user: {}", userId);
    }
    
    public Flux<DomainEvent> getUserEventStream(String userId) {
        return userStreams.computeIfAbsent(userId, this::createUserEventStream);
    }
    
    private Flux<DomainEvent> createUserEventStream(String userId) {
        String subscriptionName = "sse-" + userId;
        String topicName = "persistent://events/users/" + userId + "/notifications";
        
        return pulsarService.consumeMessages(topicName, subscriptionName)
            .map(Message::getValue)
            .onBackpressureLatest()
            .doOnCancel(() -> {
                log.info("User {} event stream cancelled", userId);
                streamSubscriptions.remove(userId);
            })
            .share(); // Share the stream among multiple subscribers
    }
    
    public void unsubscribeUser(String userId) {
        Disposable subscription = streamSubscriptions.remove(userId);
        if (subscription != null) {
            subscription.dispose();
        }
        
        userStreams.remove(userId);
        log.info("Unsubscribed user: {}", userId);
    }
    
    /**
     * Broadcast event to all connected users
     */
    public CompletableFuture<Void> broadcastEvent(DomainEvent event) {
        String broadcastTopic = "persistent://events/broadcast/all-users";
        
        MessageMetadata metadata = MessageMetadata.builder()
            .key("broadcast")
            .eventTime(event.getTimestamp())
            .properties(Map.of(
                "eventType", event.getEventType(),
                "broadcast", "true"
            ))
            .build();
        
        return pulsarService.publishMessage(broadcastTopic, event, metadata)
            .thenApply(messageId -> null);
    }
    
    /**
     * Send targeted event to specific user group
     */
    public CompletableFuture<Void> sendToUserGroup(
            String groupId, 
            DomainEvent event, 
            List<String> userIds) {
        
        List<CompletableFuture<MessageId>> publishFutures = userIds.stream()
            .map(userId -> {
                String userTopic = "persistent://events/users/" + userId + "/notifications";
                MessageMetadata metadata = MessageMetadata.builder()
                    .key(userId)
                    .eventTime(event.getTimestamp())
                    .orderingKey(userId)
                    .properties(Map.of(
                        "eventType", event.getEventType(),
                        "groupId", groupId,
                        "targeted", "true"
                    ))
                    .build();
                
                return pulsarService.publishMessage(userTopic, event, metadata);
            })
            .collect(Collectors.toList());
        
        return CompletableFuture.allOf(publishFutures.toArray(new CompletableFuture[0]));
    }
}
```

### Complex Event Processing Engine

```java
@Service
@Slf4j
public class ComplexEventProcessingEngine {
    
    private final Map<String, CEPPattern> patterns = new ConcurrentHashMap<>();
    private final Map<String, EventWindow> windows = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);
    
    /**
     * Register CEP pattern for complex event detection
     */
    public void registerPattern(String patternId, CEPPattern pattern) {
        patterns.put(patternId, pattern);
        log.info("CEP pattern registered: {}", patternId);
    }
    
    /**
     * Process incoming event against all registered patterns
     */
    public CompletableFuture<List<PatternMatch>> processEvent(DomainEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            List<PatternMatch> matches = new ArrayList<>();
            
            for (Map.Entry<String, CEPPattern> entry : patterns.entrySet()) {
                String patternId = entry.getKey();
                CEPPattern pattern = entry.getValue();
                
                // Get or create event window for this pattern
                EventWindow window = windows.computeIfAbsent(patternId, 
                    k -> createEventWindow(pattern));
                
                // Add event to window
                window.addEvent(event);
                
                // Check for pattern matches
                List<PatternMatch> patternMatches = checkPatternMatches(pattern, window);
                matches.addAll(patternMatches);
            }
            
            return matches;
        });
    }
    
    private EventWindow createEventWindow(CEPPattern pattern) {
        EventWindow window = new EventWindow(
            pattern.getWindowSize(), 
            pattern.getWindowType()
        );
        
        // Schedule window maintenance
        scheduler.scheduleAtFixedRate(
            window::maintainWindow, 
            1, 1, TimeUnit.SECONDS
        );
        
        return window;
    }
    
    private List<PatternMatch> checkPatternMatches(CEPPattern pattern, EventWindow window) {
        List<PatternMatch> matches = new ArrayList<>();
        
        switch (pattern.getPatternType()) {
            case SEQUENCE:
                matches.addAll(checkSequencePattern(pattern, window));
                break;
            case CONJUNCTION:
                matches.addAll(checkConjunctionPattern(pattern, window));
                break;
            case DISJUNCTION:
                matches.addAll(checkDisjunctionPattern(pattern, window));
                break;
            case ABSENCE:
                matches.addAll(checkAbsencePattern(pattern, window));
                break;
            case THRESHOLD:
                matches.addAll(checkThresholdPattern(pattern, window));
                break;
        }
        
        return matches;
    }
    
    /**
     * Check for sequence patterns (A followed by B followed by C)
     */
    private List<PatternMatch> checkSequencePattern(CEPPattern pattern, EventWindow window) {
        List<PatternMatch> matches = new ArrayList<>();
        List<DomainEvent> events = window.getEvents();
        
        List<EventCondition> conditions = pattern.getConditions();
        if (conditions.size() < 2) return matches;
        
        for (int i = 0; i <= events.size() - conditions.size(); i++) {
            List<DomainEvent> candidateSequence = new ArrayList<>();
            int conditionIndex = 0;
            
            for (int j = i; j < events.size() && conditionIndex < conditions.size(); j++) {
                DomainEvent event = events.get(j);
                EventCondition condition = conditions.get(conditionIndex);
                
                if (condition.matches(event)) {
                    candidateSequence.add(event);
                    conditionIndex++;
                    
                    if (conditionIndex == conditions.size()) {
                        // Complete sequence found
                        matches.add(PatternMatch.builder()
                            .patternId(pattern.getPatternId())
                            .matchedEvents(new ArrayList<>(candidateSequence))
                            .matchType(PatternType.SEQUENCE)
                            .confidence(calculateConfidence(candidateSequence, pattern))
                            .timestamp(System.currentTimeMillis())
                            .build());
                        break;
                    }
                } else if (pattern.isStrictSequence()) {
                    // Break if strict sequence is required
                    break;
                }
            }
        }
        
        return matches;
    }
    
    /**
     * Check for conjunction patterns (A AND B within time window)
     */
    private List<PatternMatch> checkConjunctionPattern(CEPPattern pattern, EventWindow window) {
        List<PatternMatch> matches = new ArrayList<>();
        List<DomainEvent> events = window.getEvents();
        List<EventCondition> conditions = pattern.getConditions();
        
        // Group events by condition
        Map<EventCondition, List<DomainEvent>> eventGroups = new HashMap<>();
        for (EventCondition condition : conditions) {
            eventGroups.put(condition, events.stream()
                .filter(condition::matches)
                .collect(Collectors.toList()));
        }
        
        // Check if all conditions have matching events
        boolean allConditionsMet = eventGroups.values().stream()
            .allMatch(group -> !group.isEmpty());
        
        if (allConditionsMet) {
            // Create Cartesian product of all matching events
            List<List<DomainEvent>> combinations = cartesianProduct(
                new ArrayList<>(eventGroups.values()));
            
            for (List<DomainEvent> combination : combinations) {
                if (isWithinTimeWindow(combination, pattern.getMaxTimespan())) {
                    matches.add(PatternMatch.builder()
                        .patternId(pattern.getPatternId())
                        .matchedEvents(combination)
                        .matchType(PatternType.CONJUNCTION)
                        .confidence(calculateConfidence(combination, pattern))
                        .timestamp(System.currentTimeMillis())
                        .build());
                }
            }
        }
        
        return matches;
    }
    
    /**
     * Check for threshold patterns (at least N events of type X)
     */
    private List<PatternMatch> checkThresholdPattern(CEPPattern pattern, EventWindow window) {
        List<PatternMatch> matches = new ArrayList<>();
        
        if (pattern.getConditions().size() != 1) return matches;
        
        EventCondition condition = pattern.getConditions().get(0);
        List<DomainEvent> matchingEvents = window.getEvents().stream()
            .filter(condition::matches)
            .collect(Collectors.toList());
        
        if (matchingEvents.size() >= pattern.getThreshold()) {
            matches.add(PatternMatch.builder()
                .patternId(pattern.getPatternId())
                .matchedEvents(matchingEvents)
                .matchType(PatternType.THRESHOLD)
                .confidence(1.0) // Threshold patterns have binary confidence
                .timestamp(System.currentTimeMillis())
                .metadata(Map.of(
                    "threshold", pattern.getThreshold(),
                    "actualCount", matchingEvents.size()
                ))
                .build());
        }
        
        return matches;
    }
    
    /**
     * Check for absence patterns (A occurs without B within time window)
     */
    private List<PatternMatch> checkAbsencePattern(CEPPattern pattern, EventWindow window) {
        List<PatternMatch> matches = new ArrayList<>();
        
        if (pattern.getConditions().size() != 2) return matches;
        
        EventCondition presentCondition = pattern.getConditions().get(0);
        EventCondition absentCondition = pattern.getConditions().get(1);
        
        List<DomainEvent> presentEvents = window.getEvents().stream()
            .filter(presentCondition::matches)
            .collect(Collectors.toList());
        
        List<DomainEvent> absentEvents = window.getEvents().stream()
            .filter(absentCondition::matches)
            .collect(Collectors.toList());
        
        for (DomainEvent presentEvent : presentEvents) {
            long windowStart = presentEvent.getTimestamp();
            long windowEnd = windowStart + pattern.getMaxTimespan();
            
            boolean hasAbsentEvent = absentEvents.stream()
                .anyMatch(event -> event.getTimestamp() >= windowStart && 
                                  event.getTimestamp() <= windowEnd);
            
            if (!hasAbsentEvent) {
                matches.add(PatternMatch.builder()
                    .patternId(pattern.getPatternId())
                    .matchedEvents(List.of(presentEvent))
                    .matchType(PatternType.ABSENCE)
                    .confidence(1.0)
                    .timestamp(System.currentTimeMillis())
                    .build());
            }
        }
        
        return matches;
    }
    
    private double calculateConfidence(List<DomainEvent> events, CEPPattern pattern) {
        // Basic confidence calculation based on timing and completeness
        if (events.isEmpty()) return 0.0;
        
        // Time-based confidence (events closer in time = higher confidence)
        long timeSpan = events.get(events.size() - 1).getTimestamp() - 
                       events.get(0).getTimestamp();
        double timeConfidence = Math.max(0.1, 1.0 - (timeSpan / (double) pattern.getMaxTimespan()));
        
        // Completeness confidence
        double completenessConfidence = (double) events.size() / pattern.getConditions().size();
        
        return (timeConfidence + completenessConfidence) / 2.0;
    }
    
    private boolean isWithinTimeWindow(List<DomainEvent> events, long maxTimespan) {
        if (events.size() < 2) return true;
        
        long minTimestamp = events.stream().mapToLong(DomainEvent::getTimestamp).min().orElse(0);
        long maxTimestamp = events.stream().mapToLong(DomainEvent::getTimestamp).max().orElse(0);
        
        return (maxTimestamp - minTimestamp) <= maxTimespan;
    }
    
    private List<List<DomainEvent>> cartesianProduct(List<List<DomainEvent>> lists) {
        List<List<DomainEvent>> result = new ArrayList<>();
        if (lists.isEmpty()) return result;
        
        result.add(new ArrayList<>());
        
        for (List<DomainEvent> list : lists) {
            List<List<DomainEvent>> temp = new ArrayList<>();
            for (List<DomainEvent> resultList : result) {
                for (DomainEvent event : list) {
                    List<DomainEvent> newList = new ArrayList<>(resultList);
                    newList.add(event);
                    temp.add(newList);
                }
            }
            result = temp;
        }
        
        return result;
    }
}

/**
 * Event window for maintaining events within specified constraints
 */
class EventWindow {
    private final int maxSize;
    private final Duration maxAge;
    private final WindowType windowType;
    private final List<DomainEvent> events = new ArrayList<>();
    
    public EventWindow(int maxSize, WindowType windowType) {
        this.maxSize = maxSize;
        this.maxAge = Duration.ofMinutes(10); // Default 10 minutes
        this.windowType = windowType;
    }
    
    public synchronized void addEvent(DomainEvent event) {
        events.add(event);
        maintainWindow();
    }
    
    public synchronized List<DomainEvent> getEvents() {
        return new ArrayList<>(events);
    }
    
    public synchronized void maintainWindow() {
        // Remove events based on window type
        switch (windowType) {
            case SIZE_BASED:
                while (events.size() > maxSize) {
                    events.remove(0);
                }
                break;
            case TIME_BASED:
                long cutoffTime = System.currentTimeMillis() - maxAge.toMillis();
                events.removeIf(event -> event.getTimestamp() < cutoffTime);
                break;
            case SLIDING:
                // Remove old events and maintain size
                long timeCutoff = System.currentTimeMillis() - maxAge.toMillis();
                events.removeIf(event -> event.getTimestamp() < timeCutoff);
                while (events.size() > maxSize) {
                    events.remove(0);
                }
                break;
        }
    }
}

// Supporting classes and enums
enum WindowType {
    SIZE_BASED, TIME_BASED, SLIDING
}

enum PatternType {
    SEQUENCE, CONJUNCTION, DISJUNCTION, ABSENCE, THRESHOLD
}
```

## Practical Exercises

### Exercise 1: Multi-Tenant Event Streaming
Build a complete multi-tenant event streaming system that can:
- Handle tenant isolation and resource quotas
- Implement schema evolution with backward compatibility
- Provide real-time monitoring and alerting
- Handle geo-replication for disaster recovery

### Exercise 2: Complex Event Processing
Create a CEP system that can:
- Detect complex patterns across multiple event streams
- Handle real-time fraud detection scenarios
- Implement sliding window aggregations
- Provide pattern debugging and visualization

### Exercise 3: Event Mesh Implementation
Implement a complete event mesh that can:
- Route events intelligently based on content and context
- Transform events for different consumers
- Handle event correlation across systems
- Provide event lineage and audit trails

## Performance Benchmarks

### Target Metrics
- **Message Throughput**: >1M messages/second per topic
- **Latency**: <5ms end-to-end latency for real-time events
- **Scalability**: Linear scaling with cluster size
- **Availability**: 99.99% uptime with automatic failover

### Monitoring Points
- Message production and consumption rates
- Queue depths and backlog monitoring
- Consumer lag and processing times
- Network bandwidth and connection counts

## Integration with Previous Concepts

### High-Performance Computing (Day 115)
- **GPU Acceleration**: Use GPU computing for real-time stream processing
- **Parallel Processing**: Apply parallel algorithms to event processing
- **Memory Optimization**: Optimize memory usage for high-throughput messaging

### Distributed Systems (Day 114)
- **Consensus Integration**: Use Raft consensus for message ordering
- **CAP Theorem**: Apply consistency patterns to event streaming
- **Distributed Transactions**: Coordinate events across multiple systems

## Key Takeaways

1. **Event Mesh Architecture**: Modern applications require intelligent event routing and transformation
2. **Schema Evolution**: Plan for schema changes from the beginning to avoid breaking changes
3. **Multi-Tenancy**: Design for isolation and resource sharing in cloud environments
4. **Real-Time Processing**: Combine batch and stream processing for comprehensive solutions
5. **Monitoring and Observability**: Comprehensive monitoring is essential for production systems
6. **Fault Tolerance**: Design for failures at every level of the system

## Next Steps
Tomorrow we'll explore Blockchain & Web3 Integration, covering smart contracts, DeFi protocols, and cryptocurrency trading systems that build upon today's advanced messaging and event streaming foundation.