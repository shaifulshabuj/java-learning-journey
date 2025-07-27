# Day 104: Enterprise Integration Patterns - ESB, Message Patterns & Legacy System Integration

## Learning Objectives
- Master Enterprise Service Bus (ESB) architecture and implementation
- Understand and implement core Enterprise Integration Patterns (EIP)
- Design strategies for legacy system integration and modernization
- Build robust messaging and communication patterns
- Implement transformation and routing patterns for enterprise systems

## Topics Covered
1. Enterprise Service Bus (ESB) Architecture
2. Message Channel and Endpoint Patterns
3. Message Routing and Transformation Patterns
4. Legacy System Integration Strategies
5. API Gateway and Service Mesh Integration
6. Event-Driven Integration Patterns
7. Data Integration and ETL Patterns

## 1. Enterprise Service Bus (ESB) Architecture

### ESB Core Implementation

```java
// Message interface
public interface Message {
    String getMessageId();
    Map<String, Object> getHeaders();
    Object getPayload();
    String getPayloadType();
    Instant getTimestamp();
    void setHeader(String key, Object value);
    Object getHeader(String key);
}

// Message implementation
public class GenericMessage implements Message {
    private final String messageId;
    private final Map<String, Object> headers;
    private final Object payload;
    private final String payloadType;
    private final Instant timestamp;
    
    public GenericMessage(Object payload, Map<String, Object> headers) {
        this.messageId = UUID.randomUUID().toString();
        this.payload = payload;
        this.payloadType = payload != null ? payload.getClass().getSimpleName() : "null";
        this.headers = new ConcurrentHashMap<>(headers != null ? headers : Map.of());
        this.timestamp = Instant.now();
        
        // Set standard headers
        this.headers.put("messageId", messageId);
        this.headers.put("timestamp", timestamp.toString());
        this.headers.put("payloadType", payloadType);
    }
    
    @Override
    public String getMessageId() { return messageId; }
    
    @Override
    public Map<String, Object> getHeaders() { return Collections.unmodifiableMap(headers); }
    
    @Override
    public Object getPayload() { return payload; }
    
    @Override
    public String getPayloadType() { return payloadType; }
    
    @Override
    public Instant getTimestamp() { return timestamp; }
    
    @Override
    public void setHeader(String key, Object value) { headers.put(key, value); }
    
    @Override
    public Object getHeader(String key) { return headers.get(key); }
}

// Message Channel interface
public interface MessageChannel {
    String getName();
    void send(Message message);
    void send(Message message, long timeout);
    Message receive();
    Message receive(long timeout);
    void subscribe(MessageHandler handler);
    void unsubscribe(MessageHandler handler);
}

// Message Handler interface
public interface MessageHandler {
    void handleMessage(Message message);
    boolean canHandle(Message message);
    String getHandlerName();
}

// Point-to-Point Channel implementation
public class PointToPointChannel implements MessageChannel {
    private final String name;
    private final BlockingQueue<Message> messageQueue;
    private final List<MessageHandler> handlers;
    private final Logger logger = LoggerFactory.getLogger(PointToPointChannel.class);
    
    public PointToPointChannel(String name, int capacity) {
        this.name = name;
        this.messageQueue = new LinkedBlockingQueue<>(capacity);
        this.handlers = new CopyOnWriteArrayList<>();
        startMessageProcessing();
    }
    
    @Override
    public String getName() { return name; }
    
    @Override
    public void send(Message message) {
        try {
            message.setHeader("channel", name);
            message.setHeader("sentAt", Instant.now().toString());
            messageQueue.put(message);
            logger.debug("Message sent to channel {}: {}", name, message.getMessageId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MessageChannelException("Failed to send message", e);
        }
    }
    
    @Override
    public void send(Message message, long timeout) {
        try {
            message.setHeader("channel", name);
            message.setHeader("sentAt", Instant.now().toString());
            boolean sent = messageQueue.offer(message, timeout, TimeUnit.MILLISECONDS);
            if (!sent) {
                throw new MessageChannelException("Failed to send message within timeout");
            }
            logger.debug("Message sent to channel {}: {}", name, message.getMessageId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MessageChannelException("Failed to send message", e);
        }
    }
    
    @Override
    public Message receive() {
        try {
            Message message = messageQueue.take();
            message.setHeader("receivedAt", Instant.now().toString());
            logger.debug("Message received from channel {}: {}", name, message.getMessageId());
            return message;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MessageChannelException("Failed to receive message", e);
        }
    }
    
    @Override
    public Message receive(long timeout) {
        try {
            Message message = messageQueue.poll(timeout, TimeUnit.MILLISECONDS);
            if (message != null) {
                message.setHeader("receivedAt", Instant.now().toString());
                logger.debug("Message received from channel {}: {}", name, message.getMessageId());
            }
            return message;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MessageChannelException("Failed to receive message", e);
        }
    }
    
    @Override
    public void subscribe(MessageHandler handler) {
        handlers.add(handler);
        logger.info("Handler {} subscribed to channel {}", handler.getHandlerName(), name);
    }
    
    @Override
    public void unsubscribe(MessageHandler handler) {
        handlers.remove(handler);
        logger.info("Handler {} unsubscribed from channel {}", handler.getHandlerName(), name);
    }
    
    private void startMessageProcessing() {
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "Channel-" + name + "-Processor");
            thread.setDaemon(true);
            return thread;
        });
        
        executor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Message message = receive();
                    processMessage(message);
                } catch (Exception e) {
                    logger.error("Error processing message in channel {}", name, e);
                }
            }
        });
    }
    
    private void processMessage(Message message) {
        for (MessageHandler handler : handlers) {
            try {
                if (handler.canHandle(message)) {
                    handler.handleMessage(message);
                    break; // Point-to-point: only one handler processes the message
                }
            } catch (Exception e) {
                logger.error("Error in message handler {}", handler.getHandlerName(), e);
            }
        }
    }
}

// Publish-Subscribe Channel implementation
public class PublishSubscribeChannel implements MessageChannel {
    private final String name;
    private final List<MessageHandler> subscribers;
    private final ExecutorService executorService;
    private final Logger logger = LoggerFactory.getLogger(PublishSubscribeChannel.class);
    
    public PublishSubscribeChannel(String name) {
        this.name = name;
        this.subscribers = new CopyOnWriteArrayList<>();
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "PubSub-" + name + "-" + System.currentTimeMillis());
            thread.setDaemon(true);
            return thread;
        });
    }
    
    @Override
    public String getName() { return name; }
    
    @Override
    public void send(Message message) {
        message.setHeader("channel", name);
        message.setHeader("sentAt", Instant.now().toString());
        
        // Publish to all subscribers
        for (MessageHandler subscriber : subscribers) {
            executorService.submit(() -> {
                try {
                    if (subscriber.canHandle(message)) {
                        subscriber.handleMessage(message);
                    }
                } catch (Exception e) {
                    logger.error("Error in subscriber {}", subscriber.getHandlerName(), e);
                }
            });
        }
        
        logger.debug("Message published to {} subscribers on channel {}: {}", 
                subscribers.size(), name, message.getMessageId());
    }
    
    @Override
    public void send(Message message, long timeout) {
        send(message); // Timeout not applicable for pub-sub
    }
    
    @Override
    public Message receive() {
        throw new UnsupportedOperationException("Receive not supported on publish-subscribe channel");
    }
    
    @Override
    public Message receive(long timeout) {
        throw new UnsupportedOperationException("Receive not supported on publish-subscribe channel");
    }
    
    @Override
    public void subscribe(MessageHandler handler) {
        subscribers.add(handler);
        logger.info("Subscriber {} added to channel {}", handler.getHandlerName(), name);
    }
    
    @Override
    public void unsubscribe(MessageHandler handler) {
        subscribers.remove(handler);
        logger.info("Subscriber {} removed from channel {}", handler.getHandlerName(), name);
    }
}

// ESB Core implementation
@Component
public class EnterpriseServiceBus {
    
    private final Map<String, MessageChannel> channels = new ConcurrentHashMap<>();
    private final Map<String, MessageRouter> routers = new ConcurrentHashMap<>();
    private final Map<String, MessageTransformer> transformers = new ConcurrentHashMap<>();
    private final Logger logger = LoggerFactory.getLogger(EnterpriseServiceBus.class);
    
    public void createPointToPointChannel(String name, int capacity) {
        channels.put(name, new PointToPointChannel(name, capacity));
        logger.info("Created point-to-point channel: {}", name);
    }
    
    public void createPublishSubscribeChannel(String name) {
        channels.put(name, new PublishSubscribeChannel(name));
        logger.info("Created publish-subscribe channel: {}", name);
    }
    
    public MessageChannel getChannel(String name) {
        return channels.get(name);
    }
    
    public void registerRouter(String name, MessageRouter router) {
        routers.put(name, router);
        logger.info("Registered message router: {}", name);
    }
    
    public void registerTransformer(String name, MessageTransformer transformer) {
        transformers.put(name, transformer);
        logger.info("Registered message transformer: {}", name);
    }
    
    public void sendMessage(String channelName, Object payload, Map<String, Object> headers) {
        MessageChannel channel = channels.get(channelName);
        if (channel == null) {
            throw new MessageChannelException("Channel not found: " + channelName);
        }
        
        Message message = new GenericMessage(payload, headers);
        channel.send(message);
    }
    
    public void route(String routerName, Message message) {
        MessageRouter router = routers.get(routerName);
        if (router != null) {
            router.route(message);
        } else {
            logger.warn("Router not found: {}", routerName);
        }
    }
    
    public Message transform(String transformerName, Message message) {
        MessageTransformer transformer = transformers.get(transformerName);
        if (transformer != null) {
            return transformer.transform(message);
        } else {
            logger.warn("Transformer not found: {}", transformerName);
            return message;
        }
    }
    
    public void shutdown() {
        channels.values().forEach(channel -> {
            if (channel instanceof PublishSubscribeChannel) {
                // Shutdown executor service
            }
        });
        logger.info("ESB shutdown completed");
    }
}

// Custom exceptions
public class MessageChannelException extends RuntimeException {
    public MessageChannelException(String message) {
        super(message);
    }
    
    public MessageChannelException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

## 2. Message Routing and Transformation Patterns

### Message Router Implementation

```java
// Message Router interface
public interface MessageRouter {
    void route(Message message);
    void addRoute(String condition, String targetChannel);
    void removeRoute(String condition);
    String getRouterName();
}

// Content-Based Router implementation
public class ContentBasedRouter implements MessageRouter {
    
    private final String routerName;
    private final Map<String, String> routes = new ConcurrentHashMap<>();
    private final Map<String, Predicate<Message>> routePredicates = new ConcurrentHashMap<>();
    private final EnterpriseServiceBus esb;
    private final String defaultChannel;
    private final Logger logger = LoggerFactory.getLogger(ContentBasedRouter.class);
    
    public ContentBasedRouter(String routerName, EnterpriseServiceBus esb, String defaultChannel) {
        this.routerName = routerName;
        this.esb = esb;
        this.defaultChannel = defaultChannel;
    }
    
    @Override
    public void route(Message message) {
        String targetChannel = determineTargetChannel(message);
        
        if (targetChannel != null) {
            MessageChannel channel = esb.getChannel(targetChannel);
            if (channel != null) {
                message.setHeader("routedBy", routerName);
                message.setHeader("routedTo", targetChannel);
                channel.send(message);
                logger.debug("Routed message {} to channel {}", 
                        message.getMessageId(), targetChannel);
            } else {
                logger.error("Target channel not found: {}", targetChannel);
            }
        } else {
            logger.warn("No route found for message {}, using default channel", 
                    message.getMessageId());
            if (defaultChannel != null) {
                MessageChannel channel = esb.getChannel(defaultChannel);
                if (channel != null) {
                    channel.send(message);
                }
            }
        }
    }
    
    @Override
    public void addRoute(String condition, String targetChannel) {
        routes.put(condition, targetChannel);
        routePredicates.put(condition, createPredicate(condition));
        logger.info("Added route: {} -> {}", condition, targetChannel);
    }
    
    @Override
    public void removeRoute(String condition) {
        routes.remove(condition);
        routePredicates.remove(condition);
        logger.info("Removed route: {}", condition);
    }
    
    @Override
    public String getRouterName() { return routerName; }
    
    private String determineTargetChannel(Message message) {
        for (Map.Entry<String, Predicate<Message>> entry : routePredicates.entrySet()) {
            if (entry.getValue().test(message)) {
                return routes.get(entry.getKey());
            }
        }
        return null;
    }
    
    private Predicate<Message> createPredicate(String condition) {
        // Simple condition parsing - in production, use a proper expression parser
        if (condition.startsWith("payloadType=")) {
            String expectedType = condition.substring("payloadType=".length());
            return message -> expectedType.equals(message.getPayloadType());
        } else if (condition.startsWith("header.")) {
            String[] parts = condition.split("=");
            if (parts.length == 2) {
                String headerName = parts[0].substring("header.".length());
                String expectedValue = parts[1];
                return message -> expectedValue.equals(String.valueOf(message.getHeader(headerName)));
            }
        } else if (condition.startsWith("payload.")) {
            // Handle payload property conditions
            return createPayloadPredicate(condition);
        }
        
        return message -> false; // Default: no match
    }
    
    private Predicate<Message> createPayloadPredicate(String condition) {
        // Simple payload property checking
        String[] parts = condition.split("=");
        if (parts.length == 2) {
            String propertyPath = parts[0].substring("payload.".length());
            String expectedValue = parts[1];
            
            return message -> {
                try {
                    Object payload = message.getPayload();
                    if (payload instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> payloadMap = (Map<String, Object>) payload;
                        return expectedValue.equals(String.valueOf(payloadMap.get(propertyPath)));
                    }
                    // Handle other payload types using reflection
                    return checkPropertyValue(payload, propertyPath, expectedValue);
                } catch (Exception e) {
                    logger.error("Error evaluating payload predicate", e);
                    return false;
                }
            };
        }
        
        return message -> false;
    }
    
    private boolean checkPropertyValue(Object payload, String propertyPath, String expectedValue) {
        try {
            Field field = payload.getClass().getDeclaredField(propertyPath);
            field.setAccessible(true);
            Object value = field.get(payload);
            return expectedValue.equals(String.valueOf(value));
        } catch (Exception e) {
            return false;
        }
    }
}

// Message Transformer interface
public interface MessageTransformer {
    Message transform(Message message);
    String getTransformerName();
    boolean canTransform(Message message);
}

// Data Format Transformer
public class DataFormatTransformer implements MessageTransformer {
    
    private final String transformerName;
    private final String sourceFormat;
    private final String targetFormat;
    private final ObjectMapper objectMapper;
    private final Logger logger = LoggerFactory.getLogger(DataFormatTransformer.class);
    
    public DataFormatTransformer(String transformerName, String sourceFormat, String targetFormat) {
        this.transformerName = transformerName;
        this.sourceFormat = sourceFormat;
        this.targetFormat = targetFormat;
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public Message transform(Message message) {
        try {
            Object transformedPayload = performTransformation(message.getPayload());
            
            Map<String, Object> newHeaders = new HashMap<>(message.getHeaders());
            newHeaders.put("originalFormat", sourceFormat);
            newHeaders.put("transformedFormat", targetFormat);
            newHeaders.put("transformedBy", transformerName);
            newHeaders.put("transformedAt", Instant.now().toString());
            
            Message transformedMessage = new GenericMessage(transformedPayload, newHeaders);
            
            logger.debug("Transformed message {} from {} to {}", 
                    message.getMessageId(), sourceFormat, targetFormat);
            
            return transformedMessage;
            
        } catch (Exception e) {
            logger.error("Transformation failed for message {}", message.getMessageId(), e);
            throw new TransformationException("Failed to transform message", e);
        }
    }
    
    @Override
    public String getTransformerName() { return transformerName; }
    
    @Override
    public boolean canTransform(Message message) {
        String messageFormat = (String) message.getHeader("format");
        return sourceFormat.equals(messageFormat) || 
               (messageFormat == null && sourceFormat.equals(message.getPayloadType()));
    }
    
    private Object performTransformation(Object payload) throws Exception {
        if ("JSON".equals(sourceFormat) && "XML".equals(targetFormat)) {
            return jsonToXml(payload);
        } else if ("XML".equals(sourceFormat) && "JSON".equals(targetFormat)) {
            return xmlToJson(payload);
        } else if ("CSV".equals(sourceFormat) && "JSON".equals(targetFormat)) {
            return csvToJson(payload);
        }
        
        return payload; // No transformation
    }
    
    private String jsonToXml(Object payload) throws Exception {
        if (payload instanceof String) {
            JsonNode jsonNode = objectMapper.readTree((String) payload);
            return "<root>" + jsonToXmlElement(jsonNode) + "</root>";
        }
        return objectMapper.writeValueAsString(payload);
    }
    
    private String jsonToXmlElement(JsonNode node) {
        StringBuilder xml = new StringBuilder();
        
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                xml.append("<").append(key).append(">")
                   .append(jsonToXmlElement(value))
                   .append("</").append(key).append(">");
            });
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                xml.append("<item>").append(jsonToXmlElement(item)).append("</item>");
            }
        } else {
            xml.append(node.asText());
        }
        
        return xml.toString();
    }
    
    private String xmlToJson(Object payload) throws Exception {
        // Simplified XML to JSON conversion
        // In production, use a proper XML parsing library
        return "{\"converted\": \"" + payload.toString() + "\"}";
    }
    
    private String csvToJson(Object payload) throws Exception {
        if (payload instanceof String) {
            String[] lines = ((String) payload).split("\n");
            if (lines.length > 0) {
                String[] headers = lines[0].split(",");
                List<Map<String, String>> records = new ArrayList<>();
                
                for (int i = 1; i < lines.length; i++) {
                    String[] values = lines[i].split(",");
                    Map<String, String> record = new HashMap<>();
                    for (int j = 0; j < Math.min(headers.length, values.length); j++) {
                        record.put(headers[j].trim(), values[j].trim());
                    }
                    records.add(record);
                }
                
                return objectMapper.writeValueAsString(records);
            }
        }
        return "[]";
    }
}

// Message Enricher
public class MessageEnricher implements MessageTransformer {
    
    private final String transformerName;
    private final EnrichmentService enrichmentService;
    private final Logger logger = LoggerFactory.getLogger(MessageEnricher.class);
    
    public MessageEnricher(String transformerName, EnrichmentService enrichmentService) {
        this.transformerName = transformerName;
        this.enrichmentService = enrichmentService;
    }
    
    @Override
    public Message transform(Message message) {
        try {
            Map<String, Object> enrichmentData = enrichmentService.enrich(message.getPayload());
            
            Map<String, Object> newHeaders = new HashMap<>(message.getHeaders());
            newHeaders.put("enrichedBy", transformerName);
            newHeaders.put("enrichedAt", Instant.now().toString());
            
            // Combine original payload with enrichment data
            Map<String, Object> enrichedPayload = new HashMap<>();
            enrichedPayload.put("original", message.getPayload());
            enrichedPayload.put("enrichment", enrichmentData);
            
            Message enrichedMessage = new GenericMessage(enrichedPayload, newHeaders);
            
            logger.debug("Enriched message {} with {} data elements", 
                    message.getMessageId(), enrichmentData.size());
            
            return enrichedMessage;
            
        } catch (Exception e) {
            logger.error("Enrichment failed for message {}", message.getMessageId(), e);
            return message; // Return original message if enrichment fails
        }
    }
    
    @Override
    public String getTransformerName() { return transformerName; }
    
    @Override
    public boolean canTransform(Message message) {
        return enrichmentService.canEnrich(message.getPayload());
    }
}

// Enrichment Service interface
public interface EnrichmentService {
    Map<String, Object> enrich(Object payload);
    boolean canEnrich(Object payload);
}

// Trading Data Enrichment Service
@Service
public class TradingDataEnrichmentService implements EnrichmentService {
    
    private final Logger logger = LoggerFactory.getLogger(TradingDataEnrichmentService.class);
    
    @Override
    public Map<String, Object> enrich(Object payload) {
        Map<String, Object> enrichmentData = new HashMap<>();
        
        if (payload instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> payloadMap = (Map<String, Object>) payload;
            
            // Enrich with market data
            String symbol = (String) payloadMap.get("symbol");
            if (symbol != null) {
                enrichmentData.put("marketData", getMarketData(symbol));
                enrichmentData.put("companyInfo", getCompanyInfo(symbol));
                enrichmentData.put("riskMetrics", calculateRiskMetrics(symbol));
            }
            
            // Enrich with trader data
            String traderId = (String) payloadMap.get("traderId");
            if (traderId != null) {
                enrichmentData.put("traderProfile", getTraderProfile(traderId));
                enrichmentData.put("traderLimits", getTraderLimits(traderId));
            }
        }
        
        enrichmentData.put("enrichmentTimestamp", Instant.now().toString());
        enrichmentData.put("enrichmentSource", "TradingDataEnrichmentService");
        
        return enrichmentData;
    }
    
    @Override
    public boolean canEnrich(Object payload) {
        if (payload instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> payloadMap = (Map<String, Object>) payload;
            return payloadMap.containsKey("symbol") || payloadMap.containsKey("traderId");
        }
        return false;
    }
    
    private Map<String, Object> getMarketData(String symbol) {
        // Simulate market data lookup
        return Map.of(
                "currentPrice", BigDecimal.valueOf(150.25),
                "volume", 1_000_000L,
                "dayHigh", BigDecimal.valueOf(152.50),
                "dayLow", BigDecimal.valueOf(148.75),
                "change", BigDecimal.valueOf(2.15),
                "changePercent", BigDecimal.valueOf(1.45)
        );
    }
    
    private Map<String, Object> getCompanyInfo(String symbol) {
        // Simulate company info lookup
        return Map.of(
                "companyName", "Example Corp",
                "sector", "Technology",
                "marketCap", BigDecimal.valueOf(500_000_000_000L),
                "peRatio", BigDecimal.valueOf(25.5)
        );
    }
    
    private Map<String, Object> calculateRiskMetrics(String symbol) {
        // Simulate risk calculations
        return Map.of(
                "beta", BigDecimal.valueOf(1.2),
                "volatility", BigDecimal.valueOf(0.25),
                "var95", BigDecimal.valueOf(5.5),
                "sharpeRatio", BigDecimal.valueOf(1.8)
        );
    }
    
    private Map<String, Object> getTraderProfile(String traderId) {
        // Simulate trader profile lookup
        return Map.of(
                "name", "John Trader",
                "level", "Senior",
                "region", "US",
                "desk", "Equity Trading"
        );
    }
    
    private Map<String, Object> getTraderLimits(String traderId) {
        // Simulate trader limits lookup
        return Map.of(
                "dailyLimit", BigDecimal.valueOf(10_000_000),
                "positionLimit", BigDecimal.valueOf(5_000_000),
                "riskLimit", BigDecimal.valueOf(500_000),
                "availableLimit", BigDecimal.valueOf(8_500_000)
        );
    }
}

// Custom exception
public class TransformationException extends RuntimeException {
    public TransformationException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

## 3. Legacy System Integration Strategies

### Legacy Integration Framework

```java
// Legacy System Adapter interface
public interface LegacySystemAdapter {
    String getSystemName();
    boolean isAvailable();
    Object invoke(String operation, Map<String, Object> parameters);
    void configure(Map<String, Object> configuration);
}

// COBOL System Adapter (via File-based integration)
@Component
public class CobolSystemAdapter implements LegacySystemAdapter {
    
    private final String systemName = "COBOL-MainFrame";
    private final String inputDirectory;
    private final String outputDirectory;
    private final String errorDirectory;
    private final Logger logger = LoggerFactory.getLogger(CobolSystemAdapter.class);
    
    public CobolSystemAdapter(@Value("${legacy.cobol.input.dir}") String inputDirectory,
                             @Value("${legacy.cobol.output.dir}") String outputDirectory,
                             @Value("${legacy.cobol.error.dir}") String errorDirectory) {
        this.inputDirectory = inputDirectory;
        this.outputDirectory = outputDirectory;
        this.errorDirectory = errorDirectory;
    }
    
    @Override
    public String getSystemName() { return systemName; }
    
    @Override
    public boolean isAvailable() {
        // Check if directories are accessible
        try {
            Path inputPath = Paths.get(inputDirectory);
            Path outputPath = Paths.get(outputDirectory);
            return Files.exists(inputPath) && Files.isWritable(inputPath) &&
                   Files.exists(outputPath) && Files.isReadable(outputPath);
        } catch (Exception e) {
            logger.error("Error checking COBOL system availability", e);
            return false;
        }
    }
    
    @Override
    public Object invoke(String operation, Map<String, Object> parameters) {
        try {
            switch (operation) {
                case "ACCOUNT_LOOKUP" -> {
                    return performAccountLookup(parameters);
                }
                case "TRANSACTION_POST" -> {
                    return performTransactionPost(parameters);
                }
                case "BALANCE_INQUIRY" -> {
                    return performBalanceInquiry(parameters);
                }
                default -> throw new IllegalArgumentException("Unknown operation: " + operation);
            }
        } catch (Exception e) {
            logger.error("Error invoking COBOL operation: {}", operation, e);
            throw new LegacySystemException("COBOL operation failed", e);
        }
    }
    
    @Override
    public void configure(Map<String, Object> configuration) {
        // Update configuration if needed
        logger.info("COBOL system adapter configured with: {}", configuration);
    }
    
    private Object performAccountLookup(Map<String, Object> parameters) throws IOException {
        String accountId = (String) parameters.get("accountId");
        
        // Create input file for COBOL system
        String requestId = UUID.randomUUID().toString();
        String inputFileName = "ACCT_LOOKUP_" + requestId + ".txt";
        Path inputFile = Paths.get(inputDirectory, inputFileName);
        
        // Write fixed-width record (COBOL style)
        String record = String.format("%-20s%-10s%-30s%n", 
                "ACCOUNT_LOOKUP", accountId, requestId);
        Files.write(inputFile, record.getBytes());
        
        // Wait for output file
        String outputFileName = "ACCT_RESP_" + requestId + ".txt";
        Path outputFile = Paths.get(outputDirectory, outputFileName);
        
        // Poll for response (with timeout)
        long startTime = System.currentTimeMillis();
        long timeout = 30000; // 30 seconds
        
        while (System.currentTimeMillis() - startTime < timeout) {
            if (Files.exists(outputFile)) {
                String response = Files.readString(outputFile);
                Files.delete(outputFile); // Cleanup
                Files.delete(inputFile);  // Cleanup
                
                return parseAccountLookupResponse(response);
            }
            
            try {
                Thread.sleep(500); // Wait 500ms before next check
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LegacySystemException("COBOL operation interrupted", e);
            }
        }
        
        throw new LegacySystemException("COBOL operation timeout");
    }
    
    private Object performTransactionPost(Map<String, Object> parameters) throws IOException {
        String accountId = (String) parameters.get("accountId");
        BigDecimal amount = (BigDecimal) parameters.get("amount");
        String transactionType = (String) parameters.get("type");
        
        String requestId = UUID.randomUUID().toString();
        String inputFileName = "TXN_POST_" + requestId + ".txt";
        Path inputFile = Paths.get(inputDirectory, inputFileName);
        
        // Format transaction record
        String record = String.format("%-20s%-10s%-15s%-10s%-30s%n",
                "TRANSACTION_POST", accountId, amount.toString(), transactionType, requestId);
        Files.write(inputFile, record.getBytes());
        
        // Wait for response
        return waitForCobolResponse("TXN_RESP_" + requestId + ".txt", inputFile);
    }
    
    private Object performBalanceInquiry(Map<String, Object> parameters) throws IOException {
        String accountId = (String) parameters.get("accountId");
        
        String requestId = UUID.randomUUID().toString();
        String inputFileName = "BAL_INQ_" + requestId + ".txt";
        Path inputFile = Paths.get(inputDirectory, inputFileName);
        
        String record = String.format("%-20s%-10s%-30s%n",
                "BALANCE_INQUIRY", accountId, requestId);
        Files.write(inputFile, record.getBytes());
        
        return waitForCobolResponse("BAL_RESP_" + requestId + ".txt", inputFile);
    }
    
    private Object waitForCobolResponse(String responseFileName, Path inputFile) throws IOException {
        Path outputFile = Paths.get(outputDirectory, responseFileName);
        
        long startTime = System.currentTimeMillis();
        long timeout = 30000; // 30 seconds
        
        while (System.currentTimeMillis() - startTime < timeout) {
            if (Files.exists(outputFile)) {
                String response = Files.readString(outputFile);
                Files.delete(outputFile); // Cleanup
                Files.delete(inputFile);  // Cleanup
                
                return parseCobolResponse(response);
            }
            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LegacySystemException("COBOL operation interrupted", e);
            }
        }
        
        throw new LegacySystemException("COBOL operation timeout");
    }
    
    private Map<String, Object> parseAccountLookupResponse(String response) {
        // Parse fixed-width COBOL response
        String[] lines = response.split("\n");
        if (lines.length > 0) {
            String statusCode = lines[0].substring(0, 2);
            if ("00".equals(statusCode)) {
                return Map.of(
                        "status", "SUCCESS",
                        "accountId", lines[0].substring(2, 12).trim(),
                        "accountName", lines[0].substring(12, 42).trim(),
                        "accountType", lines[0].substring(42, 52).trim(),
                        "balance", new BigDecimal(lines[0].substring(52, 67).trim())
                );
            } else {
                return Map.of(
                        "status", "ERROR",
                        "errorCode", statusCode,
                        "errorMessage", lines[0].substring(2).trim()
                );
            }
        }
        
        return Map.of("status", "ERROR", "errorMessage", "Invalid response format");
    }
    
    private Map<String, Object> parseCobolResponse(String response) {
        // Generic COBOL response parser
        String[] lines = response.split("\n");
        if (lines.length > 0) {
            String statusCode = lines[0].substring(0, 2);
            if ("00".equals(statusCode)) {
                return Map.of(
                        "status", "SUCCESS",
                        "data", lines[0].substring(2).trim()
                );
            } else {
                return Map.of(
                        "status", "ERROR",
                        "errorCode", statusCode,
                        "errorMessage", lines[0].substring(2).trim()
                );
            }
        }
        
        return Map.of("status", "ERROR", "errorMessage", "Invalid response format");
    }
}

// Database Legacy System Adapter
@Component
public class DatabaseLegacyAdapter implements LegacySystemAdapter {
    
    private final String systemName = "Legacy-Database";
    private final JdbcTemplate jdbcTemplate;
    private final Logger logger = LoggerFactory.getLogger(DatabaseLegacyAdapter.class);
    
    public DatabaseLegacyAdapter(@Qualifier("legacyDataSource") DataSource legacyDataSource) {
        this.jdbcTemplate = new JdbcTemplate(legacyDataSource);
    }
    
    @Override
    public String getSystemName() { return systemName; }
    
    @Override
    public boolean isAvailable() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            logger.error("Legacy database not available", e);
            return false;
        }
    }
    
    @Override
    public Object invoke(String operation, Map<String, Object> parameters) {
        try {
            return switch (operation) {
                case "GET_CUSTOMER" -> getCustomer(parameters);
                case "UPDATE_CUSTOMER" -> updateCustomer(parameters);
                case "GET_TRANSACTIONS" -> getTransactions(parameters);
                default -> throw new IllegalArgumentException("Unknown operation: " + operation);
            };
        } catch (Exception e) {
            logger.error("Error invoking database operation: {}", operation, e);
            throw new LegacySystemException("Database operation failed", e);
        }
    }
    
    @Override
    public void configure(Map<String, Object> configuration) {
        // Database configuration updates
        logger.info("Database adapter configured with: {}", configuration);
    }
    
    private Object getCustomer(Map<String, Object> parameters) {
        String customerId = (String) parameters.get("customerId");
        
        String sql = """
                SELECT customer_id, customer_name, account_type, 
                       balance, status, last_updated
                FROM legacy_customers 
                WHERE customer_id = ?
                """;
        
        try {
            return jdbcTemplate.queryForMap(sql, customerId);
        } catch (EmptyResultDataAccessException e) {
            return Map.of("status", "NOT_FOUND");
        }
    }
    
    private Object updateCustomer(Map<String, Object> parameters) {
        String customerId = (String) parameters.get("customerId");
        String customerName = (String) parameters.get("customerName");
        BigDecimal balance = (BigDecimal) parameters.get("balance");
        
        String sql = """
                UPDATE legacy_customers 
                SET customer_name = ?, balance = ?, last_updated = CURRENT_TIMESTAMP 
                WHERE customer_id = ?
                """;
        
        int rowsAffected = jdbcTemplate.update(sql, customerName, balance, customerId);
        
        return Map.of(
                "status", rowsAffected > 0 ? "SUCCESS" : "NOT_FOUND",
                "rowsAffected", rowsAffected
        );
    }
    
    private Object getTransactions(Map<String, Object> parameters) {
        String customerId = (String) parameters.get("customerId");
        LocalDate fromDate = (LocalDate) parameters.get("fromDate");
        LocalDate toDate = (LocalDate) parameters.get("toDate");
        
        String sql = """
                SELECT transaction_id, customer_id, transaction_date, 
                       amount, transaction_type, description
                FROM legacy_transactions 
                WHERE customer_id = ? AND transaction_date BETWEEN ? AND ?
                ORDER BY transaction_date DESC
                """;
        
        List<Map<String, Object>> transactions = jdbcTemplate.queryForList(
                sql, customerId, fromDate, toDate);
        
        return Map.of(
                "status", "SUCCESS",
                "transactions", transactions,
                "count", transactions.size()
        );
    }
}

// Legacy Integration Service
@Service
public class LegacyIntegrationService {
    
    private final Map<String, LegacySystemAdapter> adapters = new ConcurrentHashMap<>();
    private final StructuredLoggingService loggingService;
    private final Logger logger = LoggerFactory.getLogger(LegacyIntegrationService.class);
    
    public LegacyIntegrationService(List<LegacySystemAdapter> legacyAdapters,
                                  StructuredLoggingService loggingService) {
        this.loggingService = loggingService;
        
        // Register all adapters
        for (LegacySystemAdapter adapter : legacyAdapters) {
            adapters.put(adapter.getSystemName(), adapter);
            logger.info("Registered legacy adapter: {}", adapter.getSystemName());
        }
    }
    
    public Object invokeLegacySystem(String systemName, String operation, 
                                   Map<String, Object> parameters) {
        LegacySystemAdapter adapter = adapters.get(systemName);
        if (adapter == null) {
            throw new LegacySystemException("Legacy system not found: " + systemName);
        }
        
        if (!adapter.isAvailable()) {
            throw new LegacySystemException("Legacy system not available: " + systemName);
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = adapter.invoke(operation, parameters);
            long duration = System.currentTimeMillis() - startTime;
            
            loggingService.logBusinessEvent("legacy_system_call", Map.of(
                    "system", systemName,
                    "operation", operation,
                    "duration_ms", duration,
                    "status", "SUCCESS"
            ));
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            loggingService.logBusinessEvent("legacy_system_call", Map.of(
                    "system", systemName,
                    "operation", operation,
                    "duration_ms", duration,
                    "status", "ERROR",
                    "error", e.getMessage()
            ));
            
            throw e;
        }
    }
    
    public Map<String, Boolean> getSystemAvailability() {
        Map<String, Boolean> availability = new HashMap<>();
        
        for (Map.Entry<String, LegacySystemAdapter> entry : adapters.entrySet()) {
            try {
                availability.put(entry.getKey(), entry.getValue().isAvailable());
            } catch (Exception e) {
                availability.put(entry.getKey(), false);
                logger.error("Error checking availability for {}", entry.getKey(), e);
            }
        }
        
        return availability;
    }
    
    public void configureSystem(String systemName, Map<String, Object> configuration) {
        LegacySystemAdapter adapter = adapters.get(systemName);
        if (adapter != null) {
            adapter.configure(configuration);
            logger.info("Configured legacy system: {}", systemName);
        } else {
            throw new LegacySystemException("Legacy system not found: " + systemName);
        }
    }
}

// Legacy System Exception
public class LegacySystemException extends RuntimeException {
    public LegacySystemException(String message) {
        super(message);
    }
    
    public LegacySystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

## 4. API Gateway and Service Mesh Integration

### API Gateway Implementation

```java
// API Gateway Route Configuration
@Configuration
public class APIGatewayConfiguration {
    
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Trading Service Routes
                .route("trading-service", r -> r
                        .path("/api/trading/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .addRequestHeader("X-Gateway", "API-Gateway")
                                .addResponseHeader("X-Response-Time", String.valueOf(System.currentTimeMillis()))
                                .circuitBreaker(c -> c
                                        .setName("trading-circuit-breaker")
                                        .setFallbackUri("forward:/fallback/trading")))
                        .uri("lb://trading-service"))
                
                // Payment Service Routes
                .route("payment-service", r -> r
                        .path("/api/payments/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .addRequestHeader("X-Gateway", "API-Gateway")
                                .retry(retryConfig -> retryConfig
                                        .setRetries(3)
                                        .setStatuses(HttpStatus.INTERNAL_SERVER_ERROR)))
                        .uri("lb://payment-service"))
                
                // Legacy System Routes
                .route("legacy-integration", r -> r
                        .path("/api/legacy/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .addRequestHeader("X-Gateway", "API-Gateway")
                                .addRequestHeader("X-Legacy-Source", "API-Gateway")
                                .modifyRequestBody(String.class, String.class, this::transformLegacyRequest))
                        .uri("lb://legacy-integration-service"))
                
                .build();
    }
    
    private String transformLegacyRequest(String body) {
        // Transform modern API request to legacy format
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode requestNode = mapper.readTree(body);
            
            // Add legacy-specific headers and transform payload
            ObjectNode legacyRequest = mapper.createObjectNode();
            legacyRequest.put("legacy_format", true);
            legacyRequest.put("timestamp", Instant.now().toString());
            legacyRequest.set("data", requestNode);
            
            return mapper.writeValueAsString(legacyRequest);
        } catch (Exception e) {
            return body; // Return original if transformation fails
        }
    }
    
    @Bean
    public GlobalFilter customGlobalFilter() {
        return new CustomGlobalFilter();
    }
}

// Custom Global Filter for cross-cutting concerns
public class CustomGlobalFilter implements GlobalFilter, Ordered {
    
    private final Logger logger = LoggerFactory.getLogger(CustomGlobalFilter.class);
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Add correlation ID
        String correlationId = request.getHeaders().getFirst("X-Correlation-ID");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        
        // Add request ID
        String requestId = UUID.randomUUID().toString();
        
        // Log request
        logger.info("API Gateway Request: {} {} - Correlation: {}, Request: {}", 
                request.getMethod(), request.getURI(), correlationId, requestId);
        
        // Modify request headers
        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-Correlation-ID", correlationId)
                .header("X-Request-ID", requestId)
                .header("X-Gateway-Timestamp", Instant.now().toString())
                .build();
        
        // Record start time
        long startTime = System.currentTimeMillis();
        exchange.getAttributes().put("startTime", startTime);
        
        return chain.filter(exchange.mutate().request(modifiedRequest).build())
                .doOnSuccess(aVoid -> {
                    long duration = System.currentTimeMillis() - startTime;
                    ServerHttpResponse response = exchange.getResponse();
                    
                    logger.info("API Gateway Response: {} - Status: {}, Duration: {}ms", 
                            requestId, response.getStatusCode(), duration);
                })
                .doOnError(throwable -> {
                    long duration = System.currentTimeMillis() - startTime;
                    logger.error("API Gateway Error: {} - Duration: {}ms, Error: {}", 
                            requestId, duration, throwable.getMessage());
                });
    }
    
    @Override
    public int getOrder() {
        return -1; // High priority
    }
}

// Fallback Controller
@RestController
public class FallbackController {
    
    @GetMapping("/fallback/{service}")
    public ResponseEntity<Map<String, Object>> fallback(@PathVariable String service,
                                                       HttpServletRequest request) {
        Map<String, Object> fallbackResponse = Map.of(
                "status", "SERVICE_UNAVAILABLE",
                "service", service,
                "message", "Service temporarily unavailable. Please try again later.",
                "timestamp", Instant.now().toString(),
                "path", request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(fallbackResponse);
    }
    
    @PostMapping("/fallback/{service}")
    public ResponseEntity<Map<String, Object>> fallbackPost(@PathVariable String service,
                                                           HttpServletRequest request) {
        return fallback(service, request);
    }
}
```

## 5. Event-Driven Integration Patterns

### Event-Driven Architecture Implementation

```java
// Event Bus implementation
@Component
public class EventBus {
    
    private final Map<Class<?>, List<EventHandler<?>>> handlers = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Logger logger = LoggerFactory.getLogger(EventBus.class);
    
    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> eventType, EventHandler<T> handler) {
        handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add((EventHandler<Object>) handler);
        logger.info("Subscribed handler {} for event type {}", 
                handler.getClass().getSimpleName(), eventType.getSimpleName());
    }
    
    @SuppressWarnings("unchecked")
    public <T> void unsubscribe(Class<T> eventType, EventHandler<T> handler) {
        List<EventHandler<?>> eventHandlers = handlers.get(eventType);
        if (eventHandlers != null) {
            eventHandlers.remove(handler);
            logger.info("Unsubscribed handler {} for event type {}", 
                    handler.getClass().getSimpleName(), eventType.getSimpleName());
        }
    }
    
    public <T> void publish(T event) {
        Class<?> eventType = event.getClass();
        List<EventHandler<?>> eventHandlers = handlers.get(eventType);
        
        if (eventHandlers != null && !eventHandlers.isEmpty()) {
            for (EventHandler<?> handler : eventHandlers) {
                executorService.submit(() -> {
                    try {
                        @SuppressWarnings("unchecked")
                        EventHandler<T> typedHandler = (EventHandler<T>) handler;
                        typedHandler.handle(event);
                    } catch (Exception e) {
                        logger.error("Error handling event {} with handler {}", 
                                eventType.getSimpleName(), handler.getClass().getSimpleName(), e);
                    }
                });
            }
            
            logger.debug("Published event {} to {} handlers", 
                    eventType.getSimpleName(), eventHandlers.size());
        } else {
            logger.warn("No handlers found for event type {}", eventType.getSimpleName());
        }
    }
    
    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

// Event Handler interface
public interface EventHandler<T> {
    void handle(T event);
    Class<T> getEventType();
}

// Business Events
public class OrderPlacedEvent {
    private final String orderId;
    private final String traderId;
    private final String symbol;
    private final BigDecimal quantity;
    private final BigDecimal price;
    private final OrderSide side;
    private final Instant timestamp;
    
    public OrderPlacedEvent(String orderId, String traderId, String symbol,
                           BigDecimal quantity, BigDecimal price, OrderSide side) {
        this.orderId = orderId;
        this.traderId = traderId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.side = side;
        this.timestamp = Instant.now();
    }
    
    // Getters
    public String getOrderId() { return orderId; }
    public String getTraderId() { return traderId; }
    public String getSymbol() { return symbol; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public OrderSide getSide() { return side; }
    public Instant getTimestamp() { return timestamp; }
}

public class PaymentProcessedEvent {
    private final String paymentId;
    private final String orderId;
    private final String traderId;
    private final BigDecimal amount;
    private final String status;
    private final Instant timestamp;
    
    public PaymentProcessedEvent(String paymentId, String orderId, String traderId,
                               BigDecimal amount, String status) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.traderId = traderId;
        this.amount = amount;
        this.status = status;
        this.timestamp = Instant.now();
    }
    
    // Getters
    public String getPaymentId() { return paymentId; }
    public String getOrderId() { return orderId; }
    public String getTraderId() { return traderId; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public Instant getTimestamp() { return timestamp; }
}

// Event Handlers
@Component
public class OrderEventHandler implements EventHandler<OrderPlacedEvent> {
    
    private final LegacyIntegrationService legacyIntegrationService;
    private final EventBus eventBus;
    private final Logger logger = LoggerFactory.getLogger(OrderEventHandler.class);
    
    public OrderEventHandler(LegacyIntegrationService legacyIntegrationService, EventBus eventBus) {
        this.legacyIntegrationService = legacyIntegrationService;
        this.eventBus = eventBus;
    }
    
    @Override
    public void handle(OrderPlacedEvent event) {
        logger.info("Processing order placed event: {}", event.getOrderId());
        
        try {
            // Check trader limits in legacy system
            Map<String, Object> traderParams = Map.of("customerId", event.getTraderId());
            Object traderResult = legacyIntegrationService.invokeLegacySystem(
                    "Legacy-Database", "GET_CUSTOMER", traderParams);
            
            // Validate against COBOL risk system
            Map<String, Object> riskParams = Map.of(
                    "accountId", event.getTraderId(),
                    "amount", event.getQuantity().multiply(event.getPrice()),
                    "type", "TRADE"
            );
            Object riskResult = legacyIntegrationService.invokeLegacySystem(
                    "COBOL-MainFrame", "BALANCE_INQUIRY", riskParams);
            
            // Process payment if validations pass
            BigDecimal orderValue = event.getQuantity().multiply(event.getPrice());
            PaymentProcessedEvent paymentEvent = new PaymentProcessedEvent(
                    UUID.randomUUID().toString(),
                    event.getOrderId(),
                    event.getTraderId(),
                    orderValue,
                    "APPROVED"
            );
            
            eventBus.publish(paymentEvent);
            
        } catch (Exception e) {
            logger.error("Error processing order event: {}", event.getOrderId(), e);
            
            // Publish failure event
            PaymentProcessedEvent failureEvent = new PaymentProcessedEvent(
                    UUID.randomUUID().toString(),
                    event.getOrderId(),
                    event.getTraderId(),
                    BigDecimal.ZERO,
                    "FAILED"
            );
            
            eventBus.publish(failureEvent);
        }
    }
    
    @Override
    public Class<OrderPlacedEvent> getEventType() {
        return OrderPlacedEvent.class;
    }
}

@Component
public class PaymentEventHandler implements EventHandler<PaymentProcessedEvent> {
    
    private final LegacyIntegrationService legacyIntegrationService;
    private final Logger logger = LoggerFactory.getLogger(PaymentEventHandler.class);
    
    public PaymentEventHandler(LegacyIntegrationService legacyIntegrationService) {
        this.legacyIntegrationService = legacyIntegrationService;
    }
    
    @Override
    public void handle(PaymentProcessedEvent event) {
        logger.info("Processing payment event: {} for order: {}", 
                event.getPaymentId(), event.getOrderId());
        
        try {
            if ("APPROVED".equals(event.getStatus())) {
                // Post transaction to legacy accounting system
                Map<String, Object> transactionParams = Map.of(
                        "accountId", event.getTraderId(),
                        "amount", event.getAmount(),
                        "type", "DEBIT"
                );
                
                Object result = legacyIntegrationService.invokeLegacySystem(
                        "COBOL-MainFrame", "TRANSACTION_POST", transactionParams);
                
                logger.info("Transaction posted to legacy system: {}", result);
                
                // Update customer record in legacy database
                Map<String, Object> updateParams = Map.of(
                        "customerId", event.getTraderId(),
                        "lastTransactionAmount", event.getAmount()
                );
                
                legacyIntegrationService.invokeLegacySystem(
                        "Legacy-Database", "UPDATE_CUSTOMER", updateParams);
                
            } else {
                logger.warn("Payment failed for order: {}", event.getOrderId());
            }
            
        } catch (Exception e) {
            logger.error("Error processing payment event: {}", event.getPaymentId(), e);
        }
    }
    
    @Override
    public Class<PaymentProcessedEvent> getEventType() {
        return PaymentProcessedEvent.class;
    }
}

// Event Bus Configuration
@Configuration
public class EventBusConfiguration {
    
    @Bean
    public EventBus eventBus() {
        return new EventBus();
    }
    
    @EventListener
    public void configureEventHandlers(ContextRefreshedEvent event) {
        ApplicationContext context = event.getApplicationContext();
        EventBus eventBus = context.getBean(EventBus.class);
        
        // Register all event handlers
        Map<String, EventHandler> handlers = context.getBeansOfType(EventHandler.class);
        
        for (EventHandler<?> handler : handlers.values()) {
            Class<?> eventType = handler.getEventType();
            eventBus.subscribe(eventType, handler);
        }
    }
}
```

## Key Takeaways

1. **ESB Architecture**: Provides centralized message routing and transformation capabilities
2. **Message Patterns**: Enable decoupled communication between enterprise systems
3. **Legacy Integration**: Strategies for integrating with existing systems without major changes
4. **API Gateway**: Centralized entry point for managing cross-cutting concerns
5. **Event-Driven Integration**: Asynchronous, scalable integration patterns
6. **Transformation Patterns**: Enable communication between systems with different data formats
7. **Enterprise Patterns**: Proven patterns for building robust, maintainable integration solutions

These patterns enable building flexible, scalable integration architectures that can evolve with changing business requirements while maintaining reliable communication between diverse enterprise systems.