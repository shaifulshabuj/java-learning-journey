# Day 90: Enterprise Integration - ESB, Message Transformation & Routing

## Overview
Today we explore enterprise integration patterns, building Enterprise Service Bus (ESB) solutions, message transformation systems, and advanced routing mechanisms. We'll cover Apache Camel, Spring Integration, message brokers, content-based routing, and enterprise integration patterns that enable scalable, maintainable system integration.

## Learning Objectives
- Design and implement Enterprise Service Bus (ESB) architectures
- Master message transformation and content-based routing patterns
- Build integration solutions with Apache Camel and Spring Integration
- Implement reliable message processing with error handling and retries
- Create event-driven architectures with message brokers
- Apply enterprise integration patterns for complex system integration

## 1. Enterprise Service Bus (ESB) Architecture

### ESB Core Components and Design
```java
package com.javajourney.integration.esb;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;

/**
 * Enterprise Service Bus core architecture and components
 */
@Configuration
@EnableIntegration
public class EnterpriseServiceBusConfiguration {
    
    /**
     * ESB Message Bus - Central messaging infrastructure
     */
    @Component
    public static class ESBMessageBus {
        private final MessageChannel inboundChannel;
        private final MessageChannel outboundChannel;
        private final MessageChannel errorChannel;
        private final MessageTransformationService transformationService;
        private final MessageRoutingService routingService;
        
        public ESBMessageBus(MessageChannel inboundChannel,
                           MessageChannel outboundChannel,
                           MessageChannel errorChannel,
                           MessageTransformationService transformationService,
                           MessageRoutingService routingService) {
            this.inboundChannel = inboundChannel;
            this.outboundChannel = outboundChannel;
            this.errorChannel = errorChannel;
            this.transformationService = transformationService;
            this.routingService = routingService;
        }
        
        /**
         * Process message through ESB pipeline
         */
        @ServiceActivator(inputChannel = "inboundChannel")
        public void processMessage(Message<?> message) {
            try {
                // 1. Message validation
                validateMessage(message);
                
                // 2. Message transformation
                Message<?> transformedMessage = transformationService.transform(message);
                
                // 3. Content-based routing
                List<String> targetEndpoints = routingService.determineRoutes(transformedMessage);
                
                // 4. Message delivery
                deliverToEndpoints(transformedMessage, targetEndpoints);
                
                // 5. Audit logging
                auditMessageProcessing(message, transformedMessage, targetEndpoints);
                
            } catch (Exception e) {
                handleProcessingError(message, e);
            }
        }
        
        private void validateMessage(Message<?> message) {
            MessageHeaders headers = message.getHeaders();
            
            // Validate required headers
            if (!headers.containsKey("messageType")) {
                throw new MessageValidationException("Missing required header: messageType");
            }
            
            if (!headers.containsKey("sourceSystem")) {
                throw new MessageValidationException("Missing required header: sourceSystem");
            }
            
            // Validate message payload
            Object payload = message.getPayload();
            if (payload == null) {
                throw new MessageValidationException("Message payload cannot be null");
            }
            
            // Schema validation based on message type
            String messageType = headers.get("messageType", String.class);
            if (!isValidMessageStructure(payload, messageType)) {
                throw new MessageValidationException("Invalid message structure for type: " + messageType);
            }
        }
        
        private void deliverToEndpoints(Message<?> message, List<String> endpoints) {
            for (String endpoint : endpoints) {
                try {
                    MessageChannel targetChannel = getChannelByName(endpoint);
                    targetChannel.send(message);
                } catch (Exception e) {
                    handleDeliveryError(message, endpoint, e);
                }
            }
        }
        
        private void handleProcessingError(Message<?> message, Exception error) {
            ErrorMessage errorMessage = new ErrorMessage(error, message);
            errorChannel.send(errorMessage);
        }
        
        private boolean isValidMessageStructure(Object payload, String messageType) {
            // Implement schema validation logic
            return true; // Simplified
        }
        
        private MessageChannel getChannelByName(String channelName) {
            // Channel registry lookup
            return outboundChannel; // Simplified
        }
        
        private void handleDeliveryError(Message<?> message, String endpoint, Exception error) {
            // Implement delivery error handling
        }
        
        private void auditMessageProcessing(Message<?> original, Message<?> transformed, List<String> endpoints) {
            // Implement audit logging
        }
    }
    
    /**
     * ESB Service Registry for endpoint discovery and management
     */
    @Component
    public static class ESBServiceRegistry {
        private final Map<String, ServiceEndpoint> registeredServices = new ConcurrentHashMap<>();
        private final Map<String, HealthStatus> serviceHealthStatus = new ConcurrentHashMap<>();
        
        /**
         * Register a service endpoint
         */
        public void registerService(String serviceName, ServiceEndpoint endpoint) {
            registeredServices.put(serviceName, endpoint);
            serviceHealthStatus.put(serviceName, HealthStatus.UNKNOWN);
            
            // Start health monitoring
            startHealthMonitoring(serviceName, endpoint);
        }
        
        /**
         * Discover services by capability
         */
        public List<ServiceEndpoint> discoverServices(String capability) {
            return registeredServices.values().stream()
                .filter(endpoint -> endpoint.getCapabilities().contains(capability))
                .filter(endpoint -> serviceHealthStatus.get(endpoint.getServiceName()) == HealthStatus.HEALTHY)
                .collect(Collectors.toList());
        }
        
        /**
         * Get service endpoint with load balancing
         */
        public Optional<ServiceEndpoint> getServiceEndpoint(String serviceName, LoadBalancingStrategy strategy) {
            List<ServiceEndpoint> endpoints = getHealthyEndpoints(serviceName);
            if (endpoints.isEmpty()) {
                return Optional.empty();
            }
            
            return switch (strategy) {
                case ROUND_ROBIN -> Optional.of(selectRoundRobin(endpoints));
                case LEAST_CONNECTIONS -> Optional.of(selectLeastConnections(endpoints));
                case RANDOM -> Optional.of(selectRandom(endpoints));
                case WEIGHTED -> Optional.of(selectWeighted(endpoints));
            };
        }
        
        /**
         * Circuit breaker pattern for service endpoints
         */
        public <T> CompletableFuture<T> invokeWithCircuitBreaker(String serviceName, 
                                                               Function<ServiceEndpoint, CompletableFuture<T>> operation) {
            ServiceEndpoint endpoint = getServiceEndpoint(serviceName, LoadBalancingStrategy.ROUND_ROBIN)
                .orElseThrow(() -> new ServiceUnavailableException("No healthy endpoints for service: " + serviceName));
            
            return operation.apply(endpoint)
                .handle((result, throwable) -> {
                    if (throwable != null) {
                        recordServiceFailure(serviceName);
                        throw new RuntimeException(throwable);
                    } else {
                        recordServiceSuccess(serviceName);
                        return result;
                    }
                });
        }
        
        private void startHealthMonitoring(String serviceName, ServiceEndpoint endpoint) {
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    boolean isHealthy = performHealthCheck(endpoint);
                    serviceHealthStatus.put(serviceName, isHealthy ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY);
                } catch (Exception e) {
                    serviceHealthStatus.put(serviceName, HealthStatus.UNHEALTHY);
                }
            }, 0, 30, TimeUnit.SECONDS);
        }
        
        private boolean performHealthCheck(ServiceEndpoint endpoint) {
            // Implement health check logic
            return true; // Simplified
        }
        
        private List<ServiceEndpoint> getHealthyEndpoints(String serviceName) {
            return registeredServices.values().stream()
                .filter(endpoint -> endpoint.getServiceName().equals(serviceName))
                .filter(endpoint -> serviceHealthStatus.get(serviceName) == HealthStatus.HEALTHY)
                .collect(Collectors.toList());
        }
        
        private ServiceEndpoint selectRoundRobin(List<ServiceEndpoint> endpoints) {
            // Implement round-robin selection
            return endpoints.get(0); // Simplified
        }
        
        private ServiceEndpoint selectLeastConnections(List<ServiceEndpoint> endpoints) {
            return endpoints.stream()
                .min(Comparator.comparingInt(ServiceEndpoint::getActiveConnections))
                .orElse(endpoints.get(0));
        }
        
        private ServiceEndpoint selectRandom(List<ServiceEndpoint> endpoints) {
            return endpoints.get(new Random().nextInt(endpoints.size()));
        }
        
        private ServiceEndpoint selectWeighted(List<ServiceEndpoint> endpoints) {
            // Implement weighted selection based on endpoint capacity
            return endpoints.get(0); // Simplified
        }
        
        private void recordServiceFailure(String serviceName) {
            // Implement failure recording for circuit breaker
        }
        
        private void recordServiceSuccess(String serviceName) {
            // Implement success recording for circuit breaker
        }
    }
    
    /**
     * ESB Configuration and Channel Setup
     */
    @Bean
    public MessageChannel inboundChannel() {
        return MessageChannels.publishSubscribe("inboundChannel")
            .executor(taskExecutor())
            .get();
    }
    
    @Bean
    public MessageChannel outboundChannel() {
        return MessageChannels.publishSubscribe("outboundChannel")
            .executor(taskExecutor())
            .get();
    }
    
    @Bean
    public MessageChannel errorChannel() {
        return MessageChannels.queue("errorChannel", 1000)
            .get();
    }
    
    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("esb-");
        executor.initialize();
        return executor;
    }
    
    // Supporting classes
    public enum LoadBalancingStrategy {
        ROUND_ROBIN, LEAST_CONNECTIONS, RANDOM, WEIGHTED
    }
    
    public enum HealthStatus {
        HEALTHY, UNHEALTHY, UNKNOWN
    }
    
    public static class ServiceEndpoint {
        private final String serviceName;
        private final String url;
        private final Set<String> capabilities;
        private final int weight;
        private volatile int activeConnections;
        
        public ServiceEndpoint(String serviceName, String url, Set<String> capabilities, int weight) {
            this.serviceName = serviceName;
            this.url = url;
            this.capabilities = capabilities;
            this.weight = weight;
            this.activeConnections = 0;
        }
        
        // Getters
        public String getServiceName() { return serviceName; }
        public String getUrl() { return url; }
        public Set<String> getCapabilities() { return capabilities; }
        public int getWeight() { return weight; }
        public int getActiveConnections() { return activeConnections; }
    }
    
    public static class MessageValidationException extends RuntimeException {
        public MessageValidationException(String message) { super(message); }
    }
    
    public static class ServiceUnavailableException extends RuntimeException {
        public ServiceUnavailableException(String message) { super(message); }
    }
}
```

### Message Transformation Service
```java
package com.javajourney.integration.transformation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

/**
 * Advanced message transformation service with multiple transformation strategies
 */
@Service
public class MessageTransformationService {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, MessageTransformer> transformers = new ConcurrentHashMap<>();
    private final TransformationRuleEngine ruleEngine;
    
    public MessageTransformationService(TransformationRuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
        initializeTransformers();
    }
    
    /**
     * Transform message based on type and routing rules
     */
    @Transformer(inputChannel = "transformationChannel", outputChannel = "routingChannel")
    public Message<?> transform(Message<?> inputMessage) {
        String messageType = inputMessage.getHeaders().get("messageType", String.class);
        String sourceSystem = inputMessage.getHeaders().get("sourceSystem", String.class);
        String targetSystem = inputMessage.getHeaders().get("targetSystem", String.class);
        
        // Get transformation rules
        List<TransformationRule> rules = ruleEngine.getRules(messageType, sourceSystem, targetSystem);
        
        // Apply transformations
        Object transformedPayload = inputMessage.getPayload();
        Map<String, Object> transformedHeaders = new HashMap<>(inputMessage.getHeaders());
        
        for (TransformationRule rule : rules) {
            TransformationResult result = applyTransformationRule(rule, transformedPayload, transformedHeaders);
            transformedPayload = result.getPayload();
            transformedHeaders.putAll(result.getHeaders());
        }
        
        // Build transformed message
        return MessageBuilder.withPayload(transformedPayload)
            .copyHeaders(transformedHeaders)
            .setHeader("transformationTimestamp", System.currentTimeMillis())
            .setHeader("transformationRules", rules.stream().map(TransformationRule::getName).collect(Collectors.toList()))
            .build();
    }
    
    /**
     * Content-based transformation using JSON Path expressions
     */
    public static class JsonPathTransformer implements MessageTransformer {
        
        @Override
        public TransformationResult transform(Object payload, Map<String, Object> headers, TransformationRule rule) {
            try {
                JsonNode inputJson = objectMapper.valueToTree(payload);
                JsonNode outputJson = objectMapper.createObjectNode();
                
                // Apply field mappings
                for (FieldMapping mapping : rule.getFieldMappings()) {
                    JsonNode sourceValue = inputJson.at(mapping.getSourcePath());
                    if (!sourceValue.isMissingNode()) {
                        // Apply transformation function if specified
                        Object transformedValue = applyTransformationFunction(sourceValue, mapping.getTransformationFunction());
                        ((ObjectNode) outputJson).set(mapping.getTargetPath(), objectMapper.valueToTree(transformedValue));
                    }
                }
                
                // Apply conditional transformations
                for (ConditionalTransformation conditional : rule.getConditionalTransformations()) {
                    if (evaluateCondition(inputJson, conditional.getCondition())) {
                        applyConditionalMapping(outputJson, conditional.getMapping());
                    }
                }
                
                return new TransformationResult(outputJson, headers);
            } catch (Exception e) {
                throw new TransformationException("JSON transformation failed", e);
            }
        }
        
        private Object applyTransformationFunction(JsonNode value, String function) {
            if (function == null) return value;
            
            return switch (function) {
                case "uppercase" -> value.asText().toUpperCase();
                case "lowercase" -> value.asText().toLowerCase();
                case "trim" -> value.asText().trim();
                case "formatDate" -> formatDate(value.asText());
                case "formatCurrency" -> formatCurrency(value.asDouble());
                case "hash" -> hashValue(value.asText());
                default -> value;
            };
        }
        
        private boolean evaluateCondition(JsonNode json, String condition) {
            // Simple condition evaluation - in practice, use expression language
            return true; // Simplified
        }
        
        private void applyConditionalMapping(JsonNode target, FieldMapping mapping) {
            // Apply conditional field mapping
        }
        
        private String formatDate(String dateString) {
            // Implement date formatting
            return dateString;
        }
        
        private String formatCurrency(double amount) {
            return String.format("$%.2f", amount);
        }
        
        private String hashValue(String value) {
            return String.valueOf(value.hashCode());
        }
    }
    
    /**
     * XML to JSON transformation
     */
    public static class XmlToJsonTransformer implements MessageTransformer {
        private final XmlMapper xmlMapper = new XmlMapper();
        
        @Override
        public TransformationResult transform(Object payload, Map<String, Object> headers, TransformationRule rule) {
            try {
                // Parse XML
                JsonNode xmlAsJson = xmlMapper.readTree(payload.toString());
                
                // Transform according to mapping rules
                ObjectNode result = objectMapper.createObjectNode();
                
                for (FieldMapping mapping : rule.getFieldMappings()) {
                    JsonNode sourceValue = xmlAsJson.at(mapping.getSourcePath());
                    if (!sourceValue.isMissingNode()) {
                        result.set(mapping.getTargetPath(), sourceValue);
                    }
                }
                
                // Update headers
                Map<String, Object> newHeaders = new HashMap<>(headers);
                newHeaders.put("contentType", "application/json");
                newHeaders.put("originalContentType", "application/xml");
                
                return new TransformationResult(result, newHeaders);
            } catch (Exception e) {
                throw new TransformationException("XML to JSON transformation failed", e);
            }
        }
    }
    
    /**
     * Protocol buffer transformation
     */
    public static class ProtobufTransformer implements MessageTransformer {
        
        @Override
        public TransformationResult transform(Object payload, Map<String, Object> headers, TransformationRule rule) {
            // Implement protobuf transformation
            return new TransformationResult(payload, headers);
        }
    }
    
    /**
     * CSV to JSON transformation for batch processing
     */
    public static class CsvToJsonTransformer implements MessageTransformer {
        
        @Override
        public TransformationResult transform(Object payload, Map<String, Object> headers, TransformationRule rule) {
            try {
                String csvData = payload.toString();
                String[] lines = csvData.split("\n");
                
                if (lines.length < 2) {
                    throw new TransformationException("Invalid CSV format");
                }
                
                // Parse header row
                String[] headerColumns = lines[0].split(",");
                
                // Convert data rows to JSON
                ArrayNode jsonArray = objectMapper.createArrayNode();
                for (int i = 1; i < lines.length; i++) {
                    String[] dataColumns = lines[i].split(",");
                    ObjectNode jsonObject = objectMapper.createObjectNode();
                    
                    for (int j = 0; j < Math.min(headerColumns.length, dataColumns.length); j++) {
                        jsonObject.put(headerColumns[j].trim(), dataColumns[j].trim());
                    }
                    
                    jsonArray.add(jsonObject);
                }
                
                Map<String, Object> newHeaders = new HashMap<>(headers);
                newHeaders.put("contentType", "application/json");
                newHeaders.put("recordCount", jsonArray.size());
                
                return new TransformationResult(jsonArray, newHeaders);
            } catch (Exception e) {
                throw new TransformationException("CSV to JSON transformation failed", e);
            }
        }
    }
    
    private void initializeTransformers() {
        transformers.put("JSON_PATH", new JsonPathTransformer());
        transformers.put("XML_TO_JSON", new XmlToJsonTransformer());
        transformers.put("PROTOBUF", new ProtobufTransformer());
        transformers.put("CSV_TO_JSON", new CsvToJsonTransformer());
    }
    
    private TransformationResult applyTransformationRule(TransformationRule rule, Object payload, Map<String, Object> headers) {
        MessageTransformer transformer = transformers.get(rule.getTransformationType());
        if (transformer == null) {
            throw new TransformationException("Unknown transformation type: " + rule.getTransformationType());
        }
        
        return transformer.transform(payload, headers, rule);
    }
    
    // Supporting interfaces and classes
    public interface MessageTransformer {
        TransformationResult transform(Object payload, Map<String, Object> headers, TransformationRule rule);
    }
    
    public static class TransformationResult {
        private final Object payload;
        private final Map<String, Object> headers;
        
        public TransformationResult(Object payload, Map<String, Object> headers) {
            this.payload = payload;
            this.headers = headers;
        }
        
        public Object getPayload() { return payload; }
        public Map<String, Object> getHeaders() { return headers; }
    }
    
    public static class TransformationRule {
        private final String name;
        private final String transformationType;
        private final List<FieldMapping> fieldMappings;
        private final List<ConditionalTransformation> conditionalTransformations;
        
        public TransformationRule(String name, String transformationType, 
                                List<FieldMapping> fieldMappings,
                                List<ConditionalTransformation> conditionalTransformations) {
            this.name = name;
            this.transformationType = transformationType;
            this.fieldMappings = fieldMappings;
            this.conditionalTransformations = conditionalTransformations;
        }
        
        // Getters
        public String getName() { return name; }
        public String getTransformationType() { return transformationType; }
        public List<FieldMapping> getFieldMappings() { return fieldMappings; }
        public List<ConditionalTransformation> getConditionalTransformations() { return conditionalTransformations; }
    }
    
    public static class FieldMapping {
        private final String sourcePath;
        private final String targetPath;
        private final String transformationFunction;
        
        public FieldMapping(String sourcePath, String targetPath, String transformationFunction) {
            this.sourcePath = sourcePath;
            this.targetPath = targetPath;
            this.transformationFunction = transformationFunction;
        }
        
        // Getters
        public String getSourcePath() { return sourcePath; }
        public String getTargetPath() { return targetPath; }
        public String getTransformationFunction() { return transformationFunction; }
    }
    
    public static class ConditionalTransformation {
        private final String condition;
        private final FieldMapping mapping;
        
        public ConditionalTransformation(String condition, FieldMapping mapping) {
            this.condition = condition;
            this.mapping = mapping;
        }
        
        // Getters
        public String getCondition() { return condition; }
        public FieldMapping getMapping() { return mapping; }
    }
    
    public static class TransformationException extends RuntimeException {
        public TransformationException(String message) { super(message); }
        public TransformationException(String message, Throwable cause) { super(message, cause); }
    }
}
```

## 2. Apache Camel Integration Routes

### Advanced Camel Route Configurations
```java
package com.javajourney.integration.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.camel.Exchange;

/**
 * Advanced Apache Camel routes for enterprise integration patterns
 */
@Component
public class EnterpriseIntegrationRoutes extends RouteBuilder {
    
    @Override
    public void configure() throws Exception {
        
        // Configure error handling
        errorHandler(deadLetterChannel("activemq:queue:error")
            .maximumRedeliveries(3)
            .redeliveryDelay(5000)
            .backOffMultiplier(2.0)
            .maximumRedeliveryDelay(60000)
            .logStackTrace(true)
            .logRetryAttempted(true));
        
        // Order processing pipeline
        configureOrderProcessingRoute();
        
        // Content-based routing
        configureContentBasedRouting();
        
        // Message aggregation patterns
        configureMessageAggregation();
        
        // File processing with error recovery
        configureFileProcessingRoute();
        
        // Web service integration
        configureWebServiceIntegration();
        
        // Database integration patterns
        configureDatabaseIntegration();
        
        // Event-driven architecture routes
        configureEventDrivenRoutes();
    }
    
    /**
     * Order processing pipeline with multiple stages
     */
    private void configureOrderProcessingRoute() {
        from("activemq:queue:orders.incoming")
            .routeId("order-processing-pipeline")
            .log("Received order: ${body}")
            
            // Validate order
            .to("bean:orderValidator?method=validate")
            .choice()
                .when(header("validationResult").isEqualTo("INVALID"))
                    .to("activemq:queue:orders.invalid")
                    .stop()
            .end()
            
            // Enrich order with customer data
            .enrich("direct:customer-lookup", new CustomerEnrichmentStrategy())
            
            // Transform to internal format
            .marshal().json(JsonLibrary.Jackson)
            .to("bean:orderTransformer?method=transformToInternal")
            
            // Split order items for individual processing
            .split(jsonpath("$.items"))
                .streaming()
                .parallelProcessing()
                .executorService("orderProcessingExecutor")
                
                // Check inventory for each item
                .enrich("direct:inventory-check", new InventoryEnrichmentStrategy())
                
                // Reserve inventory
                .choice()
                    .when(jsonpath("$.availableQuantity >= $.requestedQuantity"))
                        .to("direct:reserve-inventory")
                        .setHeader("itemStatus", constant("RESERVED"))
                    .otherwise()
                        .setHeader("itemStatus", constant("BACKORDERED"))
                        .to("activemq:queue:backorders")
                .end()
            
            .end() // End split
            
            // Aggregate processed items back into order
            .aggregate(header("orderId"), new OrderAggregationStrategy())
                .completionTimeout(30000)
                .completionSize(header("itemCount"))
            
            // Final processing
            .choice()
                .when(simple("${body.allItemsReserved}"))
                    .to("direct:process-payment")
                    .to("activemq:queue:orders.confirmed")
                .otherwise()
                    .to("activemq:queue:orders.partial")
            .end()
            
            .log("Order processing completed: ${body}");
    }
    
    /**
     * Content-based routing with dynamic destinations
     */
    private void configureContentBasedRouting() {
        from("activemq:queue:messages.incoming")
            .routeId("content-based-routing")
            
            // Route based on message type
            .choice()
                .when(header("messageType").isEqualTo("ORDER"))
                    .to("direct:handle-order")
                
                .when(header("messageType").isEqualTo("PAYMENT"))
                    .to("direct:handle-payment")
                
                .when(header("messageType").isEqualTo("SHIPMENT"))
                    .to("direct:handle-shipment")
                
                .when(header("messageType").isEqualTo("NOTIFICATION"))
                    // Route notifications based on priority
                    .choice()
                        .when(header("priority").isEqualTo("HIGH"))
                            .to("activemq:queue:notifications.high")
                        .when(header("priority").isEqualTo("MEDIUM"))
                            .to("activemq:queue:notifications.medium")
                        .otherwise()
                            .to("activemq:queue:notifications.low")
                    .end()
                
                .otherwise()
                    .log("Unknown message type: ${header.messageType}")
                    .to("activemq:queue:messages.unknown")
            .end();
        
        // Dynamic routing based on recipient list
        from("activemq:queue:broadcast.messages")
            .routeId("dynamic-recipient-routing")
            .recipientList(method("routingService", "determineRecipients"))
            .parallelProcessing()
            .stopOnException()
            .timeout(10000);
    }
    
    /**
     * Message aggregation for batch processing
     */
    private void configureMessageAggregation() {
        from("activemq:queue:transactions.incoming")
            .routeId("transaction-aggregation")
            
            // Aggregate transactions by account ID
            .aggregate(jsonpath("$.accountId"), new TransactionAggregationStrategy())
                .completionSize(10) // Process in batches of 10
                .completionTimeout(60000) // Or after 1 minute
                .completionPredicate(method("aggregationService", "isReadyForProcessing"))
                
                // Process aggregated batch
                .to("bean:batchProcessor?method=processTransactions")
                .to("activemq:queue:transactions.processed")
            
            .end();
    }
    
    /**
     * File processing with error recovery and retry logic
     */
    private void configureFileProcessingRoute() {
        from("file:input?move=processed&moveFailed=error&readLock=changed&readLockTimeout=10000")
            .routeId("file-processing")
            .log("Processing file: ${header.CamelFileName}")
            
            // Validate file format
            .choice()
                .when(header("CamelFileName").endsWith(".csv"))
                    .to("direct:process-csv")
                .when(header("CamelFileName").endsWith(".xml"))
                    .to("direct:process-xml")
                .when(header("CamelFileName").endsWith(".json"))
                    .to("direct:process-json")
                .otherwise()
                    .log("Unsupported file format: ${header.CamelFileName}")
                    .to("file:unsupported")
                    .stop()
            .end();
        
        // CSV processing route
        from("direct:process-csv")
            .routeId("csv-processing")
            .unmarshal().csv()
            .split(body())
                .streaming()
                .to("bean:csvProcessor?method=processRecord")
                .to("activemq:queue:csv.records.processed")
            .end();
        
        // XML processing route with schema validation
        from("direct:process-xml")
            .routeId("xml-processing")
            .to("validator:schema/order.xsd")
            .unmarshal().jacksonXml()
            .to("bean:xmlProcessor?method=processXml")
            .to("activemq:queue:xml.processed");
    }
    
    /**
     * Web service integration with circuit breaker pattern
     */
    private void configureWebServiceIntegration() {
        from("activemq:queue:external.api.requests")
            .routeId("external-api-integration")
            
            // Circuit breaker configuration
            .circuitBreaker()
                .resilience4jConfiguration()
                    .circuitBreakerRef("externalApiCircuitBreaker")
                .end()
                
                // Main API call
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .to("http://external-api.example.com/api/process?bridgeEndpoint=true")
                
                // Success handling
                .log("External API call successful")
                .to("activemq:queue:external.api.responses")
                
            // Fallback when circuit breaker is open
            .onFallback()
                .log("Circuit breaker fallback - using cached response")
                .to("bean:cacheService?method=getCachedResponse")
                .to("activemq:queue:external.api.responses.cached")
            
            .end();
    }
    
    /**
     * Database integration with transaction management
     */
    private void configureDatabaseIntegration() {
        from("activemq:queue:database.operations")
            .routeId("database-integration")
            .transacted("txManager")
            
            .choice()
                .when(header("operation").isEqualTo("INSERT"))
                    .to("sql:INSERT INTO orders (id, customer_id, amount) VALUES (:#id, :#customerId, :#amount)?dataSource=dataSource")
                
                .when(header("operation").isEqualTo("UPDATE"))
                    .to("sql:UPDATE orders SET status = :#status WHERE id = :#id?dataSource=dataSource")
                
                .when(header("operation").isEqualTo("SELECT"))
                    .to("sql:SELECT * FROM orders WHERE customer_id = :#customerId?dataSource=dataSource")
                    .marshal().json()
                    .to("activemq:queue:database.results")
                
                .otherwise()
                    .log("Unknown database operation: ${header.operation}")
                    .throwException(new IllegalArgumentException("Invalid operation"))
            .end()
            
            .log("Database operation completed successfully");
    }
    
    /**
     * Event-driven architecture with publish-subscribe pattern
     */
    private void configureEventDrivenRoutes() {
        // Event publisher
        from("direct:publish-event")
            .routeId("event-publisher")
            .setHeader("eventId", method("uuidGenerator", "generate"))
            .setHeader("timestamp", method("timestampGenerator", "now"))
            .setHeader("eventType", simple("${body.eventType}"))
            
            // Publish to multiple subscribers
            .multicast()
                .parallelProcessing()
                .to("activemq:topic:events.all")
                .to("activemq:topic:events.${header.eventType}")
            .end();
        
        // Event subscribers
        from("activemq:topic:events.ORDER_CREATED")
            .routeId("order-created-subscriber")
            .log("Handling ORDER_CREATED event: ${body}")
            .to("bean:orderEventHandler?method=handleOrderCreated");
        
        from("activemq:topic:events.PAYMENT_PROCESSED")
            .routeId("payment-processed-subscriber")
            .log("Handling PAYMENT_PROCESSED event: ${body}")
            .to("bean:paymentEventHandler?method=handlePaymentProcessed");
        
        from("activemq:topic:events.SHIPMENT_DISPATCHED")
            .routeId("shipment-dispatched-subscriber")
            .log("Handling SHIPMENT_DISPATCHED event: ${body}")
            .to("bean:shipmentEventHandler?method=handleShipmentDispatched");
    }
    
    // Aggregation strategies
    private static class CustomerEnrichmentStrategy implements AggregationStrategy {
        @Override
        public Exchange aggregate(Exchange original, Exchange resource) {
            String customerData = resource.getIn().getBody(String.class);
            original.getIn().setHeader("customerData", customerData);
            return original;
        }
    }
    
    private static class InventoryEnrichmentStrategy implements AggregationStrategy {
        @Override
        public Exchange aggregate(Exchange original, Exchange resource) {
            Integer availableQuantity = resource.getIn().getBody(Integer.class);
            original.getIn().setHeader("availableQuantity", availableQuantity);
            return original;
        }
    }
    
    private static class OrderAggregationStrategy implements AggregationStrategy {
        @Override
        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            if (oldExchange == null) {
                return newExchange;
            }
            
            // Combine order items
            List<Object> items = oldExchange.getIn().getBody(List.class);
            Object newItem = newExchange.getIn().getBody();
            items.add(newItem);
            
            return oldExchange;
        }
    }
    
    private static class TransactionAggregationStrategy implements AggregationStrategy {
        @Override
        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            if (oldExchange == null) {
                List<Object> transactions = new ArrayList<>();
                transactions.add(newExchange.getIn().getBody());
                newExchange.getIn().setBody(transactions);
                return newExchange;
            }
            
            List<Object> transactions = oldExchange.getIn().getBody(List.class);
            transactions.add(newExchange.getIn().getBody());
            
            return oldExchange;
        }
    }
}
```

## 3. Spring Integration Patterns

### Advanced Spring Integration Configuration
```java
package com.javajourney.integration.spring;

import org.springframework.integration.annotation.*;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.*;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.handler.LoggingHandler;

/**
 * Advanced Spring Integration patterns and DSL configurations
 */
@Configuration
@EnableIntegration
public class SpringIntegrationConfiguration {
    
    /**
     * File processing integration flow with error handling
     */
    @Bean
    public IntegrationFlow fileProcessingFlow() {
        return IntegrationFlows
            .from(Files.inboundAdapter(new File("input"))
                .autoCreateDirectory(true)
                .preventDuplicates(true)
                .patternFilter("*.csv,*.json,*.xml"))
            
            // Add headers for processing
            .enrichHeaders(h -> h
                .header("processingTimestamp", System.currentTimeMillis())
                .headerExpression("fileSize", "payload.length()")
                .headerExpression("fileType", "headers['file_name'].substring(headers['file_name'].lastIndexOf('.') + 1)"))
            
            // Content-based routing
            .route("headers['fileType']", r -> r
                .subFlowMapping("csv", sf -> sf
                    .transform(Transformers.converter(String.class))
                    .split(s -> s.delimiters("\n"))
                    .channel("csvProcessingChannel"))
                
                .subFlowMapping("json", sf -> sf
                    .transform(Transformers.fromJson())
                    .channel("jsonProcessingChannel"))
                
                .subFlowMapping("xml", sf -> sf
                    .transform(Transformers.unmarshaller(jaxb2Marshaller()))
                    .channel("xmlProcessingChannel"))
                
                .defaultOutputToParentFlow())
            
            // Error handling  
            .handle(message -> {
                throw new ProcessingException("Unsupported file type");
            }, e -> e.advice(retryAdvice()))
            
            .get();
    }
    
    /**
     * Message aggregation and correlation flow
     */
    @Bean
    public IntegrationFlow messageAggregationFlow() {
        return IntegrationFlows
            .from("aggregationInputChannel")
            
            // Correlate messages by order ID
            .aggregate(a -> a
                .correlationStrategy(message -> message.getHeaders().get("orderId"))
                .releaseStrategy(new OrderCompletionStrategy())
                .messageStore(messageStore())
                .sendPartialResultOnExpiry(true)
                .expireGroupsUponCompletion(true)
                .groupTimeout(60000))
            
            // Process aggregated order
            .handle("orderProcessor", "processCompleteOrder")
            
            // Route based on processing result
            .<OrderProcessingResult>route(OrderProcessingResult::getStatus, r -> r
                .subFlowMapping("SUCCESS", sf -> sf.channel("successChannel"))
                .subFlowMapping("PARTIAL", sf -> sf.channel("partialChannel"))
                .subFlowMapping("FAILED", sf -> sf.channel("errorChannel")))
            
            .get();
    }
    
    /**
     * Web service integration with circuit breaker
     */
    @Bean
    public IntegrationFlow webServiceIntegrationFlow() {
        return IntegrationFlows
            .from("webServiceRequestChannel")
            
            // Prepare request
            .enrichHeaders(h -> h
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + getAuthToken()))
            
            // Circuit breaker pattern
            .handle(Http.outboundGateway("http://external-service/api")
                .httpMethod(HttpMethod.POST)
                .expectedResponseType(String.class), 
                e -> e.advice(circuitBreakerAdvice()))
            
            // Handle response
            .transform(Transformers.fromJson(ApiResponse.class))
            
            // Route based on response status
            .<ApiResponse>route(ApiResponse::isSuccess, r -> r
                .subFlowMapping(true, sf -> sf
                    .handle("responseProcessor", "processSuccess")
                    .channel("responseSuccessChannel"))
                .subFlowMapping(false, sf -> sf
                    .handle("responseProcessor", "processError")
                    .channel("responseErrorChannel")))
            
            .get();
    }
    
    /**
     * Event-driven message processing with pub/sub
     */
    @Bean
    public IntegrationFlow eventProcessingFlow() {
        return IntegrationFlows
            .from("eventInputChannel")
            
            // Validate event
            .filter("eventValidator", "isValidEvent")
            
            // Transform to standard format
            .transform("eventTransformer", "standardizeEvent")
            
            // Publish to multiple subscribers
            .publishSubscribeChannel(p -> p
                .subscribe(s -> s
                    .channel("auditChannel")
                    .handle("auditService", "logEvent"))
                
                .subscribe(s -> s
                    .channel("notificationChannel")
                    .handle("notificationService", "sendNotification"))
                
                .subscribe(s -> s
                    .channel("analyticsChannel")
                    .handle("analyticsService", "processEvent")))
            
            .get();
    }
    
    /**
     * Database integration with transaction management
     */
    @Bean
    public IntegrationFlow databaseIntegrationFlow() {
        return IntegrationFlows
            .from("databaseOperationChannel")
            
            // Start transaction
            .handle(message -> message, e -> e.transactional(transactionManager()))
            
            // Route based on operation type
            .<DatabaseOperation>route(DatabaseOperation::getType, r -> r
                .subFlowMapping("INSERT", sf -> sf
                    .handle(Jpa.outboundAdapter(entityManagerFactory())
                        .entityClass(Order.class)
                        .persistMode(PersistMode.PERSIST)))
                
                .subFlowMapping("UPDATE", sf -> sf
                    .handle(Jpa.outboundAdapter(entityManagerFactory())
                        .entityClass(Order.class)
                        .persistMode(PersistMode.MERGE)))
                
                .subFlowMapping("SELECT", sf -> sf
                    .handle(Jpa.retrievingOutboundGateway(entityManagerFactory())
                        .jpaQuery("SELECT o FROM Order o WHERE o.customerId = :customerId")
                        .parameterSourceFactory(new BeanPropertyParameterSourceFactory()))))
            
            // Handle database results
            .handle("databaseResultHandler", "handleResult")
            
            .get();
    }
    
    /**
     * Batch processing flow for high-volume data
     */
    @Bean
    public IntegrationFlow batchProcessingFlow() {
        return IntegrationFlows
            .from("batchInputChannel")
            
            // Split into manageable chunks
            .split(s -> s
                .applySequence(true)
                .delimiters("\n"))
            
            // Parallel processing
            .channel(MessageChannels.executor("batchProcessingExecutor"))
            
            // Process each record
            .handle("recordProcessor", "processRecord")
            
            // Aggregate results
            .aggregate(a -> a
                .correlationStrategy(message -> message.getHeaders().get("correlationId"))
                .releaseStrategy(new SequenceCompletionStrategy())
                .outputProcessor(group -> {
                    List<Object> results = group.getMessages().stream()
                        .map(Message::getPayload)
                        .collect(Collectors.toList());
                    return MessageBuilder.withPayload(results).build();
                }))
            
            // Generate batch report
            .handle("batchReportGenerator", "generateReport")
            
            .get();
    }
    
    /**
     * Message routing service activator
     */
    @ServiceActivator(inputChannel = "routingChannel")
    public void routeMessage(Message<?> message) {
        String destination = determineDestination(message);
        MessageChannel destinationChannel = getChannelByName(destination);
        destinationChannel.send(message);
    }
    
    /**
     * Message transformation service activator
     */
    @Transformer(inputChannel = "transformationInputChannel", outputChannel = "transformationOutputChannel")
    public Object transformMessage(Message<?> message) {
        String transformationType = message.getHeaders().get("transformationType", String.class);
        return applyTransformation(message.getPayload(), transformationType);
    }
    
    /**
     * Message filtering service activator
     */
    @Filter(inputChannel = "filterInputChannel", outputChannel = "filterOutputChannel")
    public boolean filterMessage(Message<?> message) {
        return isMessageValid(message);
    }
    
    /**
     * Message splitter service activator
     */
    @Splitter(inputChannel = "splitterInputChannel", outputChannel = "splitterOutputChannel")
    public List<Object> splitMessage(Message<?> message) {
        return splitMessagePayload(message.getPayload());
    }
    
    /**
     * Message aggregator service activator
     */
    @Aggregator(inputChannel = "aggregatorInputChannel", outputChannel = "aggregatorOutputChannel")
    public Object aggregateMessages(List<Message<?>> messages) {
        return combineMessagePayloads(messages);
    }
    
    // Supporting beans and configurations
    @Bean
    public MessageStore messageStore() {
        return new SimpleMessageStore();
    }
    
    @Bean
    public TaskExecutor batchProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("batch-");
        executor.initialize();
        return executor;
    }
    
    @Bean
    public RetryTemplate retryAdvice() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(2000);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        return retryTemplate;
    }
    
    @Bean
    public CircuitBreakerConfigBuilder circuitBreakerAdvice() {
        return new CircuitBreakerConfigBuilder("webServiceCircuitBreaker")
            .threshold(5)
            .timeout(10000);
    }
    
    // Supporting classes
    public static class OrderCompletionStrategy implements ReleaseStrategy {
        @Override
        public boolean canRelease(MessageGroup group) {
            // Check if all order items are present
            return group.size() >= getExpectedItemCount(group);
        }
        
        private int getExpectedItemCount(MessageGroup group) {
            return group.getMessages().stream()
                .findFirst()
                .map(msg -> msg.getHeaders().get("itemCount", Integer.class))
                .orElse(1);
        }
    }
    
    public static class SequenceCompletionStrategy implements ReleaseStrategy {
        @Override
        public boolean canRelease(MessageGroup group) {
            return group.isComplete();
        }
    }
    
    public record DatabaseOperation(String type, Object data) {}
    public record OrderProcessingResult(String status, Object data) {}
    public record ApiResponse(boolean success, String message, Object data) {
        public boolean isSuccess() { return success; }
    }
    
    // Helper methods
    private String determineDestination(Message<?> message) {
        // Implement routing logic
        return "defaultDestination";
    }
    
    private MessageChannel getChannelByName(String channelName) {
        // Channel registry lookup
        return null; // Placeholder
    }
    
    private Object applyTransformation(Object payload, String transformationType) {
        // Implement transformation logic
        return payload;
    }
    
    private boolean isMessageValid(Message<?> message) {
        // Implement validation logic
        return true;
    }
    
    private List<Object> splitMessagePayload(Object payload) {
        // Implement splitting logic
        return List.of(payload);
    }
    
    private Object combineMessagePayloads(List<Message<?>> messages) {
        // Implement aggregation logic
        return messages.stream()
            .map(Message::getPayload)
            .collect(Collectors.toList());
    }
    
    private String getAuthToken() {
        return "sample-token";
    }
}
```

## 4. Message Brokers and Event-Driven Architecture

### Advanced Message Broker Configuration
```java
package com.javajourney.integration.messaging;

import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageType;

/**
 * Advanced message broker configuration with multiple providers
 */
@Configuration
@EnableJms
public class MessageBrokerConfiguration {
    
    /**
     * ActiveMQ configuration for reliable messaging
     */
    @Bean
    public ConnectionFactory activeMQConnectionFactory() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
        connectionFactory.setBrokerURL("tcp://localhost:61616");
        connectionFactory.setUserName("admin");
        connectionFactory.setPassword("admin");
        
        // Connection pool configuration
        PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory();
        pooledConnectionFactory.setConnectionFactory(connectionFactory);
        pooledConnectionFactory.setMaxConnections(10);
        pooledConnectionFactory.setExpiryTimeout(0);
        pooledConnectionFactory.setIdleTimeout(30000);
        
        return pooledConnectionFactory;
    }
    
    /**
     * RabbitMQ configuration for high-throughput messaging
     */
    @Bean
    public ConnectionFactory rabbitMQConnectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory("localhost");
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");
        connectionFactory.setChannelCacheSize(10);
        connectionFactory.setChannelCheckoutTimeout(5000);
        
        return connectionFactory;
    }
    
    /**
     * Apache Kafka configuration for event streaming
     */
    @Bean
    public ProducerFactory<String, Object> kafkaProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Performance optimizations
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        
        // Reliability settings
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }
    
    @Bean
    public ConsumerFactory<String, Object> kafkaConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "enterprise-integration");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        
        // Performance settings
        configProps.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);
        configProps.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        configProps.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 1048576);
        
        // Reliability settings
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }
    
    /**
     * JMS listener container factory with error handling
     */
    @Bean
    public JmsListenerContainerFactory<?> jmsListenerContainerFactory(
            ConnectionFactory connectionFactory,
            DefaultJmsListenerContainerFactoryConfigurer configurer) {
        
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        
        // Configure basic settings
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(jacksonMessageConverter());
        factory.setErrorHandler(jmsErrorHandler());
        
        // Performance settings
        factory.setConcurrency("3-10");
        factory.setSessionTransacted(true);
        factory.setCacheLevelName("CACHE_CONSUMER");
        
        // Retry configuration
        factory.setBackOff(new ExponentialBackOff(1000, 2.0));
        
        return factory;
    }
    
    /**
     * Message converter for JSON serialization
     */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }
    
    /**
     * Advanced message routing service
     */
    @Service
    public static class EnterpriseMessageRouter {
        private final JmsTemplate jmsTemplate;
        private final KafkaTemplate<String, Object> kafkaTemplate;
        private final RabbitTemplate rabbitTemplate;
        private final MessageRoutingRules routingRules;
        
        public EnterpriseMessageRouter(JmsTemplate jmsTemplate,
                                     KafkaTemplate<String, Object> kafkaTemplate,
                                     RabbitTemplate rabbitTemplate,
                                     MessageRoutingRules routingRules) {
            this.jmsTemplate = jmsTemplate;
            this.kafkaTemplate = kafkaTemplate;
            this.rabbitTemplate = rabbitTemplate;
            this.routingRules = routingRules;
        }
        
        /**
         * Route message based on content and routing rules
         */
        public void routeMessage(EnterpriseMessage message) {
            List<RoutingDestination> destinations = routingRules.determineDestinations(message);
            
            for (RoutingDestination destination : destinations) {
                try {
                    switch (destination.getProtocol()) {
                        case JMS -> routeToJMS(message, destination);
                        case KAFKA -> routeToKafka(message, destination);
                        case RABBITMQ -> routeToRabbitMQ(message, destination);
                        case HTTP -> routeToHTTP(message, destination);
                    }
                    
                    recordSuccessfulRouting(message, destination);
                } catch (Exception e) {
                    handleRoutingError(message, destination, e);
                }
            }
        }
        
        private void routeToJMS(EnterpriseMessage message, RoutingDestination destination) {
            jmsTemplate.convertAndSend(destination.getEndpoint(), message, messagePostProcessor -> {
                // Add routing headers
                messagePostProcessor.setStringProperty("routingKey", destination.getRoutingKey());
                messagePostProcessor.setStringProperty("sourceSystem", message.getSourceSystem());
                messagePostProcessor.setLongProperty("timestamp", System.currentTimeMillis());
                return messagePostProcessor;
            });
        }
        
        private void routeToKafka(EnterpriseMessage message, RoutingDestination destination) {
            ProducerRecord<String, Object> record = new ProducerRecord<>(
                destination.getEndpoint(),
                message.getCorrelationId(),
                message
            );
            
            // Add headers
            record.headers().add("routingKey", destination.getRoutingKey().getBytes());
            record.headers().add("sourceSystem", message.getSourceSystem().getBytes());
            
            kafkaTemplate.send(record);
        }
        
        private void routeToRabbitMQ(EnterpriseMessage message, RoutingDestination destination) {
            MessageProperties properties = new MessageProperties();
            properties.setHeader("routingKey", destination.getRoutingKey());
            properties.setHeader("sourceSystem", message.getSourceSystem());
            properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            
            Message rabbitMessage = new Message(
                objectMapper.writeValueAsBytes(message),
                properties
            );
            
            rabbitTemplate.send(destination.getExchange(), destination.getRoutingKey(), rabbitMessage);
        }
        
        private void routeToHTTP(EnterpriseMessage message, RoutingDestination destination) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Routing-Key", destination.getRoutingKey());
            headers.set("X-Source-System", message.getSourceSystem());
            
            HttpEntity<EnterpriseMessage> request = new HttpEntity<>(message, headers);
            
            restTemplate.postForEntity(destination.getEndpoint(), request, String.class);
        }
        
        private void recordSuccessfulRouting(EnterpriseMessage message, RoutingDestination destination) {
            // Record metrics and audit logs
        }
        
        private void handleRoutingError(EnterpriseMessage message, RoutingDestination destination, Exception error) {
            // Implement error handling and retry logic
        }
    }
    
    /**
     * Message listeners for different protocols
     */
    @Component
    public static class EnterpriseMessageListeners {
        
        /**
         * JMS message listener with error handling
         */
        @JmsListener(destination = "enterprise.orders", containerFactory = "jmsListenerContainerFactory")
        public void handleOrderMessage(EnterpriseMessage message, 
                                     @Header Map<String, Object> headers,
                                     Session session) throws JMSException {
            try {
                processOrderMessage(message);
                // Manual acknowledgment for transaction control
            } catch (BusinessException e) {
                // Send to dead letter queue
                sendToDeadLetterQueue(message, e);
            } catch (Exception e) {
                // Rollback transaction
                session.rollback();
                throw e;
            }
        }
        
        /**
         * Kafka message listener with batch processing
         */
        @KafkaListener(topics = "enterprise-events", 
                      containerFactory = "kafkaListenerContainerFactory")
        public void handleEventMessage(List<ConsumerRecord<String, EnterpriseMessage>> records,
                                     Acknowledgment acknowledgment) {
            try {
                List<EnterpriseMessage> messages = records.stream()
                    .map(ConsumerRecord::value)
                    .collect(Collectors.toList());
                
                processBatchMessages(messages);
                acknowledgment.acknowledge();
                
            } catch (Exception e) {
                // Handle batch processing error
                handleBatchProcessingError(records, e);
            }
        }
        
        /**
         * RabbitMQ message listener
         */
        @RabbitListener(queues = "enterprise.notifications")
        public void handleNotificationMessage(EnterpriseMessage message,
                                            @Header Map<String, Object> headers,
                                            Channel channel,
                                            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
            try {
                processNotificationMessage(message);
                channel.basicAck(deliveryTag, false);
                
            } catch (Exception e) {
                // Negative acknowledgment with requeue
                channel.basicNack(deliveryTag, false, shouldRequeue(e));
            }
        }
        
        private void processOrderMessage(EnterpriseMessage message) {
            // Implement order processing logic
        }
        
        private void processBatchMessages(List<EnterpriseMessage> messages) {
            // Implement batch processing logic
        }
        
        private void processNotificationMessage(EnterpriseMessage message) {
            // Implement notification processing logic
        }
        
        private void sendToDeadLetterQueue(EnterpriseMessage message, Exception error) {
            // Implement dead letter queue logic
        }
        
        private void handleBatchProcessingError(List<ConsumerRecord<String, EnterpriseMessage>> records, Exception error) {
            // Implement batch error handling
        }
        
        private boolean shouldRequeue(Exception error) {
            // Determine if message should be requeued based on error type
            return !(error instanceof BusinessException);
        }
    }
    
    /**
     * JMS error handler
     */
    @Bean
    public ErrorHandler jmsErrorHandler() {
        return new DefaultErrorHandler((exception, message) -> {
            System.err.println("JMS Error: " + exception.getMessage());
            // Implement error logging and alerting
        });
    }
    
    // Supporting classes
    public enum MessageProtocol {
        JMS, KAFKA, RABBITMQ, HTTP
    }
    
    public static class RoutingDestination {
        private final MessageProtocol protocol;
        private final String endpoint;
        private final String routingKey;
        private final String exchange; // For RabbitMQ
        
        public RoutingDestination(MessageProtocol protocol, String endpoint, String routingKey, String exchange) {
            this.protocol = protocol;
            this.endpoint = endpoint;
            this.routingKey = routingKey;
            this.exchange = exchange;
        }
        
        // Getters
        public MessageProtocol getProtocol() { return protocol; }
        public String getEndpoint() { return endpoint; }
        public String getRoutingKey() { return routingKey; }
        public String getExchange() { return exchange; }
    }
    
    public static class EnterpriseMessage {
        private String messageId;
        private String correlationId;
        private String sourceSystem;
        private String messageType;
        private Object payload;
        private Map<String, Object> headers;
        private long timestamp;
        
        // Constructors, getters, and setters
        public EnterpriseMessage() {
            this.messageId = UUID.randomUUID().toString();
            this.timestamp = System.currentTimeMillis();
            this.headers = new HashMap<>();
        }
        
        // Getters and setters
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        public String getCorrelationId() { return correlationId; }
        public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
        public String getSourceSystem() { return sourceSystem; }
        public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
        public String getMessageType() { return messageType; }
        public void setMessageType(String messageType) { this.messageType = messageType; }
        public Object getPayload() { return payload; }
        public void setPayload(Object payload) { this.payload = payload; }
        public Map<String, Object> getHeaders() { return headers; }
        public void setHeaders(Map<String, Object> headers) { this.headers = headers; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
    
    public static class BusinessException extends Exception {
        public BusinessException(String message) { super(message); }
        public BusinessException(String message, Throwable cause) { super(message, cause); }
    }
}
```

## 5. Monitoring and Management

### Integration Monitoring and Metrics
```java
package com.javajourney.integration.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Comprehensive monitoring and management for enterprise integration
 */
@Component
public class IntegrationMonitoringService implements HealthIndicator {
    
    private final MeterRegistry meterRegistry;
    private final IntegrationMetricsCollector metricsCollector;
    private final IntegrationAuditService auditService;
    
    public IntegrationMonitoringService(MeterRegistry meterRegistry,
                                      IntegrationMetricsCollector metricsCollector,
                                      IntegrationAuditService auditService) {
        this.meterRegistry = meterRegistry;
        this.metricsCollector = metricsCollector;
        this.auditService = auditService;
    }
    
    @Override
    public Health health() {
        IntegrationHealthStatus status = assessIntegrationHealth();
        
        if (status.isHealthy()) {
            return Health.up()
                .withDetail("messageProcessingRate", status.getMessageProcessingRate())
                .withDetail("errorRate", status.getErrorRate())
                .withDetail("activeConnections", status.getActiveConnections())
                .withDetail("queueDepths", status.getQueueDepths())
                .build();
        } else {
            return Health.down()
                .withDetail("issues", status.getIssues())
                .withDetail("criticalErrors", status.getCriticalErrors())
                .build();
        }
    }
    
    /**
     * Real-time integration metrics dashboard
     */
    @Component
    public static class IntegrationMetricsCollector {
        private final MeterRegistry meterRegistry;
        private final Map<String, Timer> routeTimers = new ConcurrentHashMap<>();
        private final Map<String, AtomicLong> messageCounters = new ConcurrentHashMap<>();
        private final Map<String, AtomicLong> errorCounters = new ConcurrentHashMap<>();
        
        public IntegrationMetricsCollector(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            initializeMetrics();
        }
        
        /**
         * Record message processing metrics
         */
        public void recordMessageProcessed(String routeName, long processingTimeMs, boolean success) {
            // Record processing time
            Timer timer = routeTimers.computeIfAbsent(routeName, 
                name -> Timer.builder("integration.message.processing.time")
                    .tag("route", name)
                    .register(meterRegistry));
            timer.record(processingTimeMs, TimeUnit.MILLISECONDS);
            
            // Record message count
            String counterKey = routeName + (success ? ".success" : ".error");
            AtomicLong counter = messageCounters.computeIfAbsent(counterKey,
                key -> meterRegistry.gauge("integration.message.count", 
                    Tags.of("route", routeName, "status", success ? "success" : "error"),
                    new AtomicLong(0)));
            counter.incrementAndGet();
            
            // Record error count if failed
            if (!success) {
                errorCounters.computeIfAbsent(routeName,
                    name -> meterRegistry.gauge("integration.error.count",
                        Tags.of("route", name), new AtomicLong(0)))
                    .incrementAndGet();
            }
        }
        
        /**
         * Record queue depth metrics
         */
        public void recordQueueDepth(String queueName, int depth) {
            meterRegistry.gauge("integration.queue.depth", 
                Tags.of("queue", queueName), depth);
        }
        
        /**
         * Record connection metrics
         */
        public void recordConnectionStatus(String connectionName, boolean active) {
            meterRegistry.gauge("integration.connection.status",
                Tags.of("connection", connectionName), active ? 1 : 0);
        }
        
        /**
         * Get integration performance summary
         */
        public IntegrationPerformanceSummary getPerformanceSummary() {
            double totalMessages = messageCounters.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
            
            double totalErrors = errorCounters.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
            
            double errorRate = totalMessages > 0 ? (totalErrors / totalMessages) * 100 : 0;
            
            double averageProcessingTime = routeTimers.values().stream()
                .mapToDouble(timer -> timer.mean(TimeUnit.MILLISECONDS))
                .average()
                .orElse(0.0);
            
            return new IntegrationPerformanceSummary(
                totalMessages, totalErrors, errorRate, averageProcessingTime);
        }
        
        private void initializeMetrics() {
            // Initialize custom metrics
            meterRegistry.gauge("integration.system.status", 1);
        }
    }
    
    /**
     * Integration audit service for compliance and traceability
     */
    @Service
    public static class IntegrationAuditService {
        private final AuditEventRepository auditRepository;
        
        public IntegrationAuditService(AuditEventRepository auditRepository) {
            this.auditRepository = auditRepository;
        }
        
        /**
         * Audit message processing events
         */
        public void auditMessageProcessing(AuditEvent event) {
            try {
                auditRepository.save(event);
                
                // Real-time alerting for critical events
                if (event.getSeverity() == AuditSeverity.CRITICAL) {
                    sendAlert(event);
                }
                
            } catch (Exception e) {
                // Fallback audit logging
                logAuditFailure(event, e);
            }
        }
        
        /**
         * Generate compliance reports
         */
        public ComplianceReport generateComplianceReport(LocalDate from, LocalDate to) {
            List<AuditEvent> events = auditRepository.findByTimestampBetween(
                from.atStartOfDay(), to.atTime(LocalTime.MAX));
            
            long totalEvents = events.size();
            long errorEvents = events.stream()
                .mapToLong(event -> event.getSeverity() == AuditSeverity.ERROR ? 1 : 0)
                .sum();
            
            long criticalEvents = events.stream()
                .mapToLong(event -> event.getSeverity() == AuditSeverity.CRITICAL ? 1 : 0)
                .sum();
            
            Map<String, Long> eventsByType = events.stream()
                .collect(Collectors.groupingBy(
                    AuditEvent::getEventType,
                    Collectors.counting()));
            
            return new ComplianceReport(from, to, totalEvents, errorEvents, criticalEvents, eventsByType);
        }
        
        /**
         * Message traceability tracking
         */
        public MessageTrace traceMessage(String messageId) {
            List<AuditEvent> trace = auditRepository.findByMessageIdOrderByTimestamp(messageId);
            
            return new MessageTrace(messageId, trace);
        }
        
        private void sendAlert(AuditEvent event) {
            // Implement alerting logic (email, Slack, PagerDuty, etc.)
        }
        
        private void logAuditFailure(AuditEvent event, Exception error) {
            // Fallback logging when audit repository fails
            System.err.printf("Audit failure for event %s: %s%n", 
                event.getEventId(), error.getMessage());
        }
    }
    
    /**
     * Integration dashboard controller
     */
    @RestController
    @RequestMapping("/api/integration/monitoring")
    public static class IntegrationDashboardController {
        private final IntegrationMetricsCollector metricsCollector;
        private final IntegrationAuditService auditService;
        
        public IntegrationDashboardController(IntegrationMetricsCollector metricsCollector,
                                            IntegrationAuditService auditService) {
            this.metricsCollector = metricsCollector;
            this.auditService = auditService;
        }
        
        @GetMapping("/performance")
        public ResponseEntity<IntegrationPerformanceSummary> getPerformanceSummary() {
            IntegrationPerformanceSummary summary = metricsCollector.getPerformanceSummary();
            return ResponseEntity.ok(summary);
        }
        
        @GetMapping("/compliance")
        public ResponseEntity<ComplianceReport> getComplianceReport(
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
            
            ComplianceReport report = auditService.generateComplianceReport(from, to);
            return ResponseEntity.ok(report);
        }
        
        @GetMapping("/trace/{messageId}")
        public ResponseEntity<MessageTrace> traceMessage(@PathVariable String messageId) {
            MessageTrace trace = auditService.traceMessage(messageId);
            return ResponseEntity.ok(trace);
        }
        
        @GetMapping("/health/detailed")
        public ResponseEntity<IntegrationHealthStatus> getDetailedHealth() {
            IntegrationHealthStatus status = assessIntegrationHealth();
            return ResponseEntity.ok(status);
        }
    }
    
    private IntegrationHealthStatus assessIntegrationHealth() {
        List<String> issues = new ArrayList<>();
        List<String> criticalErrors = new ArrayList<>();
        
        // Check message processing rates
        IntegrationPerformanceSummary performance = metricsCollector.getPerformanceSummary();
        
        if (performance.getErrorRate() > 5.0) {
            issues.add("High error rate: " + performance.getErrorRate() + "%");
        }
        
        if (performance.getErrorRate() > 10.0) {
            criticalErrors.add("Critical error rate: " + performance.getErrorRate() + "%");
        }
        
        if (performance.getAverageProcessingTime() > 5000) {
            issues.add("Slow processing time: " + performance.getAverageProcessingTime() + "ms");
        }
        
        // Check queue depths (placeholder)
        Map<String, Integer> queueDepths = getCurrentQueueDepths();
        for (Map.Entry<String, Integer> entry : queueDepths.entrySet()) {
            if (entry.getValue() > 1000) {
                issues.add("High queue depth: " + entry.getKey() + " = " + entry.getValue());
            }
        }
        
        // Check active connections (placeholder)
        int activeConnections = getActiveConnectionCount();
        
        boolean isHealthy = criticalErrors.isEmpty() && issues.size() < 3;
        
        return new IntegrationHealthStatus(
            isHealthy, performance.getTotalMessages(), performance.getErrorRate(),
            activeConnections, queueDepths, issues, criticalErrors);
    }
    
    private Map<String, Integer> getCurrentQueueDepths() {
        // Placeholder implementation
        return Map.of(
            "orders.incoming", 50,
            "notifications.outgoing", 25
        );
    }
    
    private int getActiveConnectionCount() {
        // Placeholder implementation
        return 10;
    }
    
    // Data classes
    public record IntegrationPerformanceSummary(
        double totalMessages,
        double totalErrors,
        double errorRate,
        double averageProcessingTime
    ) {}
    
    public record IntegrationHealthStatus(
        boolean isHealthy,
        double messageProcessingRate,
        double errorRate,
        int activeConnections,
        Map<String, Integer> queueDepths,
        List<String> issues,
        List<String> criticalErrors
    ) {}
    
    public record ComplianceReport(
        LocalDate fromDate,
        LocalDate toDate,
        long totalEvents,
        long errorEvents,
        long criticalEvents,
        Map<String, Long> eventsByType
    ) {}
    
    public record MessageTrace(
        String messageId,
        List<AuditEvent> events
    ) {}
    
    public static class AuditEvent {
        private String eventId;
        private String messageId;
        private String eventType;
        private AuditSeverity severity;
        private LocalDateTime timestamp;
        private String description;
        private Map<String, Object> metadata;
        
        // Constructors, getters, and setters
        public AuditEvent() {
            this.eventId = UUID.randomUUID().toString();
            this.timestamp = LocalDateTime.now();
            this.metadata = new HashMap<>();
        }
        
        // Getters and setters
        public String getEventId() { return eventId; }
        public void setEventId(String eventId) { this.eventId = eventId; }
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public AuditSeverity getSeverity() { return severity; }
        public void setSeverity(AuditSeverity severity) { this.severity = severity; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
    
    public enum AuditSeverity {
        INFO, WARNING, ERROR, CRITICAL
    }
    
    public interface AuditEventRepository {
        AuditEvent save(AuditEvent event);
        List<AuditEvent> findByTimestampBetween(LocalDateTime from, LocalDateTime to);
        List<AuditEvent> findByMessageIdOrderByTimestamp(String messageId);
    }
}
```

## Practice Exercises

### Exercise 1: Build Complete ESB Solution
Create a full ESB implementation with message transformation, routing, and error handling.

### Exercise 2: Implement Apache Camel Integration
Build complex integration routes with Camel including file processing, database integration, and web services.

### Exercise 3: Design Event-Driven Architecture
Create an event-driven system with multiple message brokers and reliable event processing.

### Exercise 4: Build Integration Monitoring Dashboard
Implement comprehensive monitoring and alerting for integration processes.

## Key Takeaways

1. **ESB Architecture**: Centralized integration hub enables scalable system integration
2. **Message Transformation**: Flexible transformation strategies handle diverse data formats
3. **Content-Based Routing**: Dynamic routing based on message content and business rules
4. **Error Handling**: Comprehensive error handling with retry, fallback, and compensation patterns
5. **Monitoring**: Continuous monitoring and auditing ensure reliability and compliance
6. **Performance**: Optimization techniques handle high-volume message processing

## Next Steps
Tomorrow we'll complete Week 13 with the capstone project: High-Performance Enterprise Financial System - integrating all the advanced concepts we've learned this week into a comprehensive enterprise application.

<function_calls>
<invoke name="TodoWrite">
<parameter name="todos">[{"content": "Create Day 85: Java Virtual Machine (JVM) Deep Dive - Memory Management & Garbage Collection", "status": "completed", "priority": "high", "id": "day85-jvm-deep-dive"}, {"content": "Create Day 86: Advanced JVM Tuning - Performance Optimization & Profiling", "status": "completed", "priority": "high", "id": "day86-jvm-tuning"}, {"content": "Create Day 87: Java Security - Authentication, Authorization & Cryptography", "status": "completed", "priority": "high", "id": "day87-java-security"}, {"content": "Create Day 88: Advanced Database Patterns - Connection Pooling, Transactions & Optimization", "status": "completed", "priority": "high", "id": "day88-database-patterns"}, {"content": "Create Day 89: Reactive Programming - Project Reactor & WebFlux Advanced", "status": "completed", "priority": "high", "id": "day89-reactive-programming"}, {"content": "Create Day 90: Enterprise Integration - ESB, Message Transformation & Routing", "status": "completed", "priority": "high", "id": "day90-enterprise-integration"}, {"content": "Create Day 91: Week 13 Capstone - High-Performance Enterprise Financial System", "status": "pending", "priority": "high", "id": "day91-week13-capstone"}]