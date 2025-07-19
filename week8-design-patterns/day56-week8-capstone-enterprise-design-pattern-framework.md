# Day 56: Week 8 Capstone - Enterprise Design Pattern Framework

## Learning Objectives
- Integrate all design patterns into a cohesive enterprise framework
- Build a real-world application using multiple design patterns
- Demonstrate advanced pattern combinations and interactions
- Create a scalable and maintainable enterprise architecture
- Apply design patterns to solve complex business problems

---

## 1. Enterprise Framework Architecture

### 1.1 System Overview

```java
// Main Application Framework
@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableCaching
public class EnterpriseFrameworkApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(EnterpriseFrameworkApplication.class, args);
    }
    
    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.initialize();
        return executor;
    }
}

// Framework Configuration
@Configuration
@EnableConfigurationProperties
public class FrameworkConfig {
    
    @Bean
    @ConfigurationProperties(prefix = "framework")
    public FrameworkProperties frameworkProperties() {
        return new FrameworkProperties();
    }
    
    @Bean
    public ApplicationContext applicationContext() {
        return new AnnotationConfigApplicationContext(FrameworkConfig.class);
    }
}

// Framework Properties
@Data
@ConfigurationProperties(prefix = "framework")
public class FrameworkProperties {
    private String version = "1.0.0";
    private String environment = "development";
    private boolean debugMode = false;
    private int maxConnections = 100;
    private int timeoutSeconds = 30;
    private DatabaseConfig database = new DatabaseConfig();
    private CacheConfig cache = new CacheConfig();
    private SecurityConfig security = new SecurityConfig();
    
    @Data
    public static class DatabaseConfig {
        private String url;
        private String username;
        private String password;
        private int maxPoolSize = 20;
        private int minPoolSize = 5;
    }
    
    @Data
    public static class CacheConfig {
        private boolean enabled = true;
        private int maxSize = 1000;
        private int ttlMinutes = 60;
    }
    
    @Data
    public static class SecurityConfig {
        private boolean enabled = true;
        private String jwtSecret;
        private int jwtExpirationHours = 24;
    }
}
```

---

## 2. Core Framework Components

### 2.1 Factory Pattern - Component Factory

```java
// Abstract Factory for Components
public abstract class ComponentFactory {
    
    public abstract DataProcessor createDataProcessor();
    public abstract NotificationService createNotificationService();
    public abstract SecurityManager createSecurityManager();
    public abstract CacheManager createCacheManager();
    
    // Factory Method for creating complete component sets
    public ComponentBundle createComponentBundle() {
        return new ComponentBundle(
            createDataProcessor(),
            createNotificationService(),
            createSecurityManager(),
            createCacheManager()
        );
    }
}

// Concrete Factory for Production Components
@Component
public class ProductionComponentFactory extends ComponentFactory {
    
    @Override
    public DataProcessor createDataProcessor() {
        return new DatabaseDataProcessor();
    }
    
    @Override
    public NotificationService createNotificationService() {
        return new EmailNotificationService();
    }
    
    @Override
    public SecurityManager createSecurityManager() {
        return new JwtSecurityManager();
    }
    
    @Override
    public CacheManager createCacheManager() {
        return new RedisBasedCacheManager();
    }
}

// Concrete Factory for Development Components
@Component
@Profile("development")
public class DevelopmentComponentFactory extends ComponentFactory {
    
    @Override
    public DataProcessor createDataProcessor() {
        return new InMemoryDataProcessor();
    }
    
    @Override
    public NotificationService createNotificationService() {
        return new ConsoleNotificationService();
    }
    
    @Override
    public SecurityManager createSecurityManager() {
        return new MockSecurityManager();
    }
    
    @Override
    public CacheManager createCacheManager() {
        return new InMemoryCacheManager();
    }
}

// Component Bundle
@Data
@AllArgsConstructor
public class ComponentBundle {
    private DataProcessor dataProcessor;
    private NotificationService notificationService;
    private SecurityManager securityManager;
    private CacheManager cacheManager;
}
```

### 2.2 Singleton Pattern - Configuration Manager

```java
// Thread-Safe Singleton Configuration Manager
public class ConfigurationManager {
    
    private static volatile ConfigurationManager instance;
    private static final Object lock = new Object();
    
    private final Map<String, Object> configuration;
    private final List<ConfigurationChangeListener> listeners;
    
    private ConfigurationManager() {
        this.configuration = new ConcurrentHashMap<>();
        this.listeners = new ArrayList<>();
        initializeDefaultConfiguration();
    }
    
    public static ConfigurationManager getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ConfigurationManager();
                }
            }
        }
        return instance;
    }
    
    private void initializeDefaultConfiguration() {
        configuration.put("max.connections", 100);
        configuration.put("timeout.seconds", 30);
        configuration.put("cache.enabled", true);
        configuration.put("debug.mode", false);
        configuration.put("security.enabled", true);
    }
    
    public <T> T getProperty(String key, Class<T> type) {
        Object value = configuration.get(key);
        if (value == null) {
            return null;
        }
        
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        
        // Type conversion
        if (type == String.class) {
            return type.cast(value.toString());
        } else if (type == Integer.class && value instanceof String) {
            return type.cast(Integer.parseInt((String) value));
        } else if (type == Boolean.class && value instanceof String) {
            return type.cast(Boolean.parseBoolean((String) value));
        }
        
        throw new IllegalArgumentException("Cannot convert " + value + " to " + type);
    }
    
    public void setProperty(String key, Object value) {
        Object oldValue = configuration.put(key, value);
        notifyListeners(key, oldValue, value);
    }
    
    public void addConfigurationChangeListener(ConfigurationChangeListener listener) {
        listeners.add(listener);
    }
    
    public void removeConfigurationChangeListener(ConfigurationChangeListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyListeners(String key, Object oldValue, Object newValue) {
        ConfigurationChangeEvent event = new ConfigurationChangeEvent(key, oldValue, newValue);
        listeners.forEach(listener -> listener.onConfigurationChange(event));
    }
    
    // Prevent cloning
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Singleton cannot be cloned");
    }
}

// Configuration Change Listener
@FunctionalInterface
public interface ConfigurationChangeListener {
    void onConfigurationChange(ConfigurationChangeEvent event);
}

// Configuration Change Event
@Data
@AllArgsConstructor
public class ConfigurationChangeEvent {
    private String key;
    private Object oldValue;
    private Object newValue;
}
```

### 2.3 Builder Pattern - Request Builder

```java
// Complex Request Builder
public class EnterpriseRequest {
    private String requestId;
    private String userId;
    private String operation;
    private Map<String, Object> parameters;
    private Map<String, String> headers;
    private SecurityContext securityContext;
    private ProcessingOptions processingOptions;
    private AuditInfo auditInfo;
    private LocalDateTime timestamp;
    
    private EnterpriseRequest() {
        this.requestId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.parameters = new HashMap<>();
        this.headers = new HashMap<>();
    }
    
    public static RequestBuilder builder() {
        return new RequestBuilder();
    }
    
    public static class RequestBuilder {
        private EnterpriseRequest request;
        
        public RequestBuilder() {
            this.request = new EnterpriseRequest();
        }
        
        public RequestBuilder withUserId(String userId) {
            request.userId = userId;
            return this;
        }
        
        public RequestBuilder withOperation(String operation) {
            request.operation = operation;
            return this;
        }
        
        public RequestBuilder withParameter(String key, Object value) {
            request.parameters.put(key, value);
            return this;
        }
        
        public RequestBuilder withParameters(Map<String, Object> parameters) {
            request.parameters.putAll(parameters);
            return this;
        }
        
        public RequestBuilder withHeader(String key, String value) {
            request.headers.put(key, value);
            return this;
        }
        
        public RequestBuilder withHeaders(Map<String, String> headers) {
            request.headers.putAll(headers);
            return this;
        }
        
        public RequestBuilder withSecurityContext(SecurityContext securityContext) {
            request.securityContext = securityContext;
            return this;
        }
        
        public RequestBuilder withProcessingOptions(ProcessingOptions options) {
            request.processingOptions = options;
            return this;
        }
        
        public RequestBuilder withAuditInfo(AuditInfo auditInfo) {
            request.auditInfo = auditInfo;
            return this;
        }
        
        public RequestBuilder withTimestamp(LocalDateTime timestamp) {
            request.timestamp = timestamp;
            return this;
        }
        
        public EnterpriseRequest build() {
            validateRequest();
            return request;
        }
        
        private void validateRequest() {
            if (request.userId == null || request.userId.isEmpty()) {
                throw new IllegalArgumentException("User ID is required");
            }
            if (request.operation == null || request.operation.isEmpty()) {
                throw new IllegalArgumentException("Operation is required");
            }
            if (request.securityContext == null) {
                request.securityContext = SecurityContext.defaultContext();
            }
            if (request.processingOptions == null) {
                request.processingOptions = ProcessingOptions.defaultOptions();
            }
            if (request.auditInfo == null) {
                request.auditInfo = AuditInfo.create(request.userId);
            }
        }
    }
    
    // Getters
    public String getRequestId() { return requestId; }
    public String getUserId() { return userId; }
    public String getOperation() { return operation; }
    public Map<String, Object> getParameters() { return Collections.unmodifiableMap(parameters); }
    public Map<String, String> getHeaders() { return Collections.unmodifiableMap(headers); }
    public SecurityContext getSecurityContext() { return securityContext; }
    public ProcessingOptions getProcessingOptions() { return processingOptions; }
    public AuditInfo getAuditInfo() { return auditInfo; }
    public LocalDateTime getTimestamp() { return timestamp; }
}

// Supporting Classes
@Data
@AllArgsConstructor
public class SecurityContext {
    private String token;
    private Set<String> roles;
    private Set<String> permissions;
    private boolean authenticated;
    
    public static SecurityContext defaultContext() {
        return new SecurityContext(null, new HashSet<>(), new HashSet<>(), false);
    }
}

@Data
@AllArgsConstructor
public class ProcessingOptions {
    private boolean asynchronous;
    private int timeout;
    private int retryCount;
    private boolean cacheEnabled;
    private Priority priority;
    
    public static ProcessingOptions defaultOptions() {
        return new ProcessingOptions(false, 30, 3, true, Priority.NORMAL);
    }
}

@Data
@AllArgsConstructor
public class AuditInfo {
    private String userId;
    private String sessionId;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
    
    public static AuditInfo create(String userId) {
        return new AuditInfo(userId, UUID.randomUUID().toString(), 
                           "127.0.0.1", "Enterprise-Framework/1.0", LocalDateTime.now());
    }
}

public enum Priority {
    LOW, NORMAL, HIGH, CRITICAL
}
```

---

## 3. Behavioral Patterns Integration

### 3.1 Strategy Pattern - Processing Strategies

```java
// Processing Strategy Interface
public interface ProcessingStrategy {
    ProcessingResult process(EnterpriseRequest request);
    boolean canHandle(String operation);
    Priority getPriority();
}

// Concrete Processing Strategies
@Component
public class DatabaseProcessingStrategy implements ProcessingStrategy {
    
    @Autowired
    private DataProcessor dataProcessor;
    
    @Override
    public ProcessingResult process(EnterpriseRequest request) {
        try {
            Object result = dataProcessor.processData(request.getParameters());
            return ProcessingResult.success(result);
        } catch (Exception e) {
            return ProcessingResult.failure(e.getMessage());
        }
    }
    
    @Override
    public boolean canHandle(String operation) {
        return operation.startsWith("db.") || operation.startsWith("data.");
    }
    
    @Override
    public Priority getPriority() {
        return Priority.HIGH;
    }
}

@Component
public class FileProcessingStrategy implements ProcessingStrategy {
    
    @Autowired
    private FileService fileService;
    
    @Override
    public ProcessingResult process(EnterpriseRequest request) {
        try {
            String fileName = (String) request.getParameters().get("fileName");
            String operation = request.getOperation();
            
            Object result = switch (operation) {
                case "file.read" -> fileService.readFile(fileName);
                case "file.write" -> fileService.writeFile(fileName, request.getParameters().get("content"));
                case "file.delete" -> fileService.deleteFile(fileName);
                default -> throw new UnsupportedOperationException("Unsupported file operation: " + operation);
            };
            
            return ProcessingResult.success(result);
        } catch (Exception e) {
            return ProcessingResult.failure(e.getMessage());
        }
    }
    
    @Override
    public boolean canHandle(String operation) {
        return operation.startsWith("file.");
    }
    
    @Override
    public Priority getPriority() {
        return Priority.NORMAL;
    }
}

@Component
public class NotificationProcessingStrategy implements ProcessingStrategy {
    
    @Autowired
    private NotificationService notificationService;
    
    @Override
    public ProcessingResult process(EnterpriseRequest request) {
        try {
            String recipient = (String) request.getParameters().get("recipient");
            String message = (String) request.getParameters().get("message");
            String type = (String) request.getParameters().get("type");
            
            NotificationRequest notificationRequest = new NotificationRequest(recipient, message, type);
            notificationService.sendNotification(notificationRequest);
            
            return ProcessingResult.success("Notification sent successfully");
        } catch (Exception e) {
            return ProcessingResult.failure(e.getMessage());
        }
    }
    
    @Override
    public boolean canHandle(String operation) {
        return operation.startsWith("notification.");
    }
    
    @Override
    public Priority getPriority() {
        return Priority.NORMAL;
    }
}

// Processing Result
@Data
@AllArgsConstructor
public class ProcessingResult {
    private boolean success;
    private Object result;
    private String errorMessage;
    private LocalDateTime timestamp;
    
    public static ProcessingResult success(Object result) {
        return new ProcessingResult(true, result, null, LocalDateTime.now());
    }
    
    public static ProcessingResult failure(String errorMessage) {
        return new ProcessingResult(false, null, errorMessage, LocalDateTime.now());
    }
}
```

### 3.2 Observer Pattern - Event Management

```java
// Event System
public interface EventListener<T extends Event> {
    void onEvent(T event);
    Class<T> getEventType();
}

// Base Event
@Data
@AllArgsConstructor
public abstract class Event {
    private String eventId;
    private String source;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
    
    protected Event(String source) {
        this.eventId = UUID.randomUUID().toString();
        this.source = source;
        this.timestamp = LocalDateTime.now();
        this.metadata = new HashMap<>();
    }
}

// Specific Events
public class RequestProcessedEvent extends Event {
    private String requestId;
    private String userId;
    private String operation;
    private boolean success;
    private long processingTimeMs;
    
    public RequestProcessedEvent(String source, String requestId, String userId, 
                               String operation, boolean success, long processingTimeMs) {
        super(source);
        this.requestId = requestId;
        this.userId = userId;
        this.operation = operation;
        this.success = success;
        this.processingTimeMs = processingTimeMs;
    }
    
    // Getters
    public String getRequestId() { return requestId; }
    public String getUserId() { return userId; }
    public String getOperation() { return operation; }
    public boolean isSuccess() { return success; }
    public long getProcessingTimeMs() { return processingTimeMs; }
}

public class ConfigurationChangedEvent extends Event {
    private String key;
    private Object oldValue;
    private Object newValue;
    
    public ConfigurationChangedEvent(String source, String key, Object oldValue, Object newValue) {
        super(source);
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
    
    // Getters
    public String getKey() { return key; }
    public Object getOldValue() { return oldValue; }
    public Object getNewValue() { return newValue; }
}

// Event Publisher
@Component
public class EventPublisher {
    
    private final Map<Class<? extends Event>, List<EventListener<? extends Event>>> listeners;
    
    public EventPublisher() {
        this.listeners = new ConcurrentHashMap<>();
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Event> void subscribe(EventListener<T> listener) {
        Class<T> eventType = listener.getEventType();
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }
    
    public <T extends Event> void unsubscribe(EventListener<T> listener) {
        Class<T> eventType = listener.getEventType();
        List<EventListener<? extends Event>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove(listener);
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Event> void publishEvent(T event) {
        Class<? extends Event> eventType = event.getClass();
        List<EventListener<? extends Event>> eventListeners = listeners.get(eventType);
        
        if (eventListeners != null) {
            eventListeners.forEach(listener -> {
                try {
                    ((EventListener<T>) listener).onEvent(event);
                } catch (Exception e) {
                    System.err.println("Error handling event: " + e.getMessage());
                }
            });
        }
    }
}

// Event Listeners
@Component
public class RequestAuditListener implements EventListener<RequestProcessedEvent> {
    
    @Autowired
    private AuditService auditService;
    
    @Override
    public void onEvent(RequestProcessedEvent event) {
        AuditRecord record = new AuditRecord();
        record.setRequestId(event.getRequestId());
        record.setUserId(event.getUserId());
        record.setOperation(event.getOperation());
        record.setSuccess(event.isSuccess());
        record.setProcessingTimeMs(event.getProcessingTimeMs());
        record.setTimestamp(event.getTimestamp());
        
        auditService.saveAuditRecord(record);
    }
    
    @Override
    public Class<RequestProcessedEvent> getEventType() {
        return RequestProcessedEvent.class;
    }
}

@Component
public class MetricsCollectionListener implements EventListener<RequestProcessedEvent> {
    
    @Autowired
    private MetricsService metricsService;
    
    @Override
    public void onEvent(RequestProcessedEvent event) {
        metricsService.recordRequestProcessed(
            event.getOperation(),
            event.isSuccess(),
            event.getProcessingTimeMs()
        );
    }
    
    @Override
    public Class<RequestProcessedEvent> getEventType() {
        return RequestProcessedEvent.class;
    }
}
```

### 3.3 Command Pattern - Request Processing

```java
// Command Interface
public interface Command {
    ProcessingResult execute();
    void undo();
    boolean isUndoable();
    String getCommandId();
}

// Concrete Commands
public class DatabaseCommand implements Command {
    private final String commandId;
    private final DataProcessor dataProcessor;
    private final String operation;
    private final Map<String, Object> parameters;
    private Object previousState;
    
    public DatabaseCommand(DataProcessor dataProcessor, String operation, Map<String, Object> parameters) {
        this.commandId = UUID.randomUUID().toString();
        this.dataProcessor = dataProcessor;
        this.operation = operation;
        this.parameters = parameters;
    }
    
    @Override
    public ProcessingResult execute() {
        try {
            // Save current state for undo
            if (isUndoable()) {
                previousState = dataProcessor.getCurrentState(parameters);
            }
            
            Object result = dataProcessor.processData(parameters);
            return ProcessingResult.success(result);
        } catch (Exception e) {
            return ProcessingResult.failure(e.getMessage());
        }
    }
    
    @Override
    public void undo() {
        if (isUndoable() && previousState != null) {
            dataProcessor.restoreState(previousState);
        }
    }
    
    @Override
    public boolean isUndoable() {
        return operation.startsWith("update") || operation.startsWith("delete");
    }
    
    @Override
    public String getCommandId() {
        return commandId;
    }
}

public class NotificationCommand implements Command {
    private final String commandId;
    private final NotificationService notificationService;
    private final NotificationRequest request;
    
    public NotificationCommand(NotificationService notificationService, NotificationRequest request) {
        this.commandId = UUID.randomUUID().toString();
        this.notificationService = notificationService;
        this.request = request;
    }
    
    @Override
    public ProcessingResult execute() {
        try {
            notificationService.sendNotification(request);
            return ProcessingResult.success("Notification sent");
        } catch (Exception e) {
            return ProcessingResult.failure(e.getMessage());
        }
    }
    
    @Override
    public void undo() {
        // Notifications typically cannot be undone
        // Could log compensation action or send cancellation notification
    }
    
    @Override
    public boolean isUndoable() {
        return false;
    }
    
    @Override
    public String getCommandId() {
        return commandId;
    }
}

// Command Invoker
@Component
public class CommandInvoker {
    
    private final Stack<Command> executedCommands;
    private final Queue<Command> pendingCommands;
    
    public CommandInvoker() {
        this.executedCommands = new Stack<>();
        this.pendingCommands = new LinkedList<>();
    }
    
    public ProcessingResult executeCommand(Command command) {
        ProcessingResult result = command.execute();
        
        if (result.isSuccess()) {
            executedCommands.push(command);
        }
        
        return result;
    }
    
    public void undoLastCommand() {
        if (!executedCommands.isEmpty()) {
            Command lastCommand = executedCommands.pop();
            if (lastCommand.isUndoable()) {
                lastCommand.undo();
            }
        }
    }
    
    public void queueCommand(Command command) {
        pendingCommands.offer(command);
    }
    
    public ProcessingResult executePendingCommands() {
        List<ProcessingResult> results = new ArrayList<>();
        
        while (!pendingCommands.isEmpty()) {
            Command command = pendingCommands.poll();
            ProcessingResult result = executeCommand(command);
            results.add(result);
            
            if (!result.isSuccess()) {
                // Stop execution on first failure
                break;
            }
        }
        
        return aggregateResults(results);
    }
    
    private ProcessingResult aggregateResults(List<ProcessingResult> results) {
        if (results.isEmpty()) {
            return ProcessingResult.success("No commands executed");
        }
        
        boolean allSuccessful = results.stream().allMatch(ProcessingResult::isSuccess);
        
        if (allSuccessful) {
            return ProcessingResult.success("All commands executed successfully");
        } else {
            String errors = results.stream()
                    .filter(r -> !r.isSuccess())
                    .map(ProcessingResult::getErrorMessage)
                    .collect(Collectors.joining(", "));
            return ProcessingResult.failure(errors);
        }
    }
}
```

---

## 4. Structural Patterns Integration

### 4.1 Facade Pattern - Service Facade

```java
// Enterprise Service Facade
@Service
public class EnterpriseServiceFacade {
    
    @Autowired
    private ComponentFactory componentFactory;
    
    @Autowired
    private EventPublisher eventPublisher;
    
    @Autowired
    private CommandInvoker commandInvoker;
    
    @Autowired
    private List<ProcessingStrategy> processingStrategies;
    
    private final ConfigurationManager configManager;
    
    public EnterpriseServiceFacade() {
        this.configManager = ConfigurationManager.getInstance();
    }
    
    public ProcessingResult processRequest(EnterpriseRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Validate request
            validateRequest(request);
            
            // Apply security
            if (!isAuthorized(request)) {
                return ProcessingResult.failure("Unauthorized access");
            }
            
            // Find appropriate processing strategy
            ProcessingStrategy strategy = findStrategy(request.getOperation());
            if (strategy == null) {
                return ProcessingResult.failure("No handler found for operation: " + request.getOperation());
            }
            
            // Process request
            ProcessingResult result;
            if (request.getProcessingOptions().isAsynchronous()) {
                result = processAsynchronously(request, strategy);
            } else {
                result = strategy.process(request);
            }
            
            // Publish event
            long processingTime = System.currentTimeMillis() - startTime;
            publishRequestProcessedEvent(request, result.isSuccess(), processingTime);
            
            return result;
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            publishRequestProcessedEvent(request, false, processingTime);
            return ProcessingResult.failure(e.getMessage());
        }
    }
    
    public ProcessingResult processRequestWithCommand(EnterpriseRequest request) {
        Command command = createCommand(request);
        return commandInvoker.executeCommand(command);
    }
    
    public void queueRequest(EnterpriseRequest request) {
        Command command = createCommand(request);
        commandInvoker.queueCommand(command);
    }
    
    public ProcessingResult processPendingRequests() {
        return commandInvoker.executePendingCommands();
    }
    
    public void undoLastOperation() {
        commandInvoker.undoLastCommand();
    }
    
    public SystemStatus getSystemStatus() {
        ComponentBundle components = componentFactory.createComponentBundle();
        
        return SystemStatus.builder()
                .version(configManager.getProperty("framework.version", String.class))
                .environment(configManager.getProperty("framework.environment", String.class))
                .timestamp(LocalDateTime.now())
                .dataProcessorStatus(components.getDataProcessor().getStatus())
                .cacheManagerStatus(components.getCacheManager().getStatus())
                .securityManagerStatus(components.getSecurityManager().getStatus())
                .notificationServiceStatus(components.getNotificationService().getStatus())
                .build();
    }
    
    private void validateRequest(EnterpriseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (request.getUserId() == null || request.getUserId().isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (request.getOperation() == null || request.getOperation().isEmpty()) {
            throw new IllegalArgumentException("Operation is required");
        }
    }
    
    private boolean isAuthorized(EnterpriseRequest request) {
        if (!configManager.getProperty("security.enabled", Boolean.class)) {
            return true;
        }
        
        ComponentBundle components = componentFactory.createComponentBundle();
        return components.getSecurityManager().isAuthorized(request.getSecurityContext(), request.getOperation());
    }
    
    private ProcessingStrategy findStrategy(String operation) {
        return processingStrategies.stream()
                .filter(strategy -> strategy.canHandle(operation))
                .max(Comparator.comparing(ProcessingStrategy::getPriority))
                .orElse(null);
    }
    
    @Async
    private ProcessingResult processAsynchronously(EnterpriseRequest request, ProcessingStrategy strategy) {
        return strategy.process(request);
    }
    
    private Command createCommand(EnterpriseRequest request) {
        ComponentBundle components = componentFactory.createComponentBundle();
        
        if (request.getOperation().startsWith("db.") || request.getOperation().startsWith("data.")) {
            return new DatabaseCommand(components.getDataProcessor(), request.getOperation(), request.getParameters());
        } else if (request.getOperation().startsWith("notification.")) {
            String recipient = (String) request.getParameters().get("recipient");
            String message = (String) request.getParameters().get("message");
            String type = (String) request.getParameters().get("type");
            NotificationRequest notificationRequest = new NotificationRequest(recipient, message, type);
            return new NotificationCommand(components.getNotificationService(), notificationRequest);
        } else {
            throw new UnsupportedOperationException("Unsupported operation: " + request.getOperation());
        }
    }
    
    private void publishRequestProcessedEvent(EnterpriseRequest request, boolean success, long processingTime) {
        RequestProcessedEvent event = new RequestProcessedEvent(
                "EnterpriseServiceFacade",
                request.getRequestId(),
                request.getUserId(),
                request.getOperation(),
                success,
                processingTime
        );
        eventPublisher.publishEvent(event);
    }
}
```

### 4.2 Decorator Pattern - Request Enhancement

```java
// Request Processor Interface
public interface RequestProcessor {
    ProcessingResult process(EnterpriseRequest request);
}

// Base Request Processor
@Component
public class BaseRequestProcessor implements RequestProcessor {
    
    @Autowired
    private EnterpriseServiceFacade serviceFacade;
    
    @Override
    public ProcessingResult process(EnterpriseRequest request) {
        return serviceFacade.processRequest(request);
    }
}

// Abstract Request Processor Decorator
public abstract class RequestProcessorDecorator implements RequestProcessor {
    
    protected final RequestProcessor requestProcessor;
    
    public RequestProcessorDecorator(RequestProcessor requestProcessor) {
        this.requestProcessor = requestProcessor;
    }
    
    @Override
    public ProcessingResult process(EnterpriseRequest request) {
        return requestProcessor.process(request);
    }
}

// Logging Decorator
@Component
public class LoggingRequestProcessor extends RequestProcessorDecorator {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingRequestProcessor.class);
    
    public LoggingRequestProcessor(RequestProcessor requestProcessor) {
        super(requestProcessor);
    }
    
    @Override
    public ProcessingResult process(EnterpriseRequest request) {
        logger.info("Processing request: {} for user: {}", request.getOperation(), request.getUserId());
        
        long startTime = System.currentTimeMillis();
        ProcessingResult result = super.process(request);
        long endTime = System.currentTimeMillis();
        
        logger.info("Request processed in {} ms. Success: {}", (endTime - startTime), result.isSuccess());
        
        if (!result.isSuccess()) {
            logger.error("Request failed: {}", result.getErrorMessage());
        }
        
        return result;
    }
}

// Caching Decorator
@Component
public class CachingRequestProcessor extends RequestProcessorDecorator {
    
    @Autowired
    private CacheManager cacheManager;
    
    public CachingRequestProcessor(RequestProcessor requestProcessor) {
        super(requestProcessor);
    }
    
    @Override
    public ProcessingResult process(EnterpriseRequest request) {
        if (!isCacheable(request)) {
            return super.process(request);
        }
        
        String cacheKey = generateCacheKey(request);
        ProcessingResult cachedResult = cacheManager.get(cacheKey, ProcessingResult.class);
        
        if (cachedResult != null) {
            return cachedResult;
        }
        
        ProcessingResult result = super.process(request);
        
        if (result.isSuccess()) {
            cacheManager.put(cacheKey, result);
        }
        
        return result;
    }
    
    private boolean isCacheable(EnterpriseRequest request) {
        return request.getProcessingOptions().isCacheEnabled() &&
               request.getOperation().startsWith("data.read");
    }
    
    private String generateCacheKey(EnterpriseRequest request) {
        return String.format("request:%s:%s:%s", 
                request.getUserId(), 
                request.getOperation(), 
                request.getParameters().hashCode());
    }
}

// Security Decorator
@Component
public class SecurityRequestProcessor extends RequestProcessorDecorator {
    
    @Autowired
    private SecurityManager securityManager;
    
    public SecurityRequestProcessor(RequestProcessor requestProcessor) {
        super(requestProcessor);
    }
    
    @Override
    public ProcessingResult process(EnterpriseRequest request) {
        // Validate security context
        if (!securityManager.isValid(request.getSecurityContext())) {
            return ProcessingResult.failure("Invalid security context");
        }
        
        // Check authorization
        if (!securityManager.isAuthorized(request.getSecurityContext(), request.getOperation())) {
            return ProcessingResult.failure("Unauthorized operation");
        }
        
        // Audit security event
        auditSecurityEvent(request);
        
        return super.process(request);
    }
    
    private void auditSecurityEvent(EnterpriseRequest request) {
        SecurityAuditEvent event = new SecurityAuditEvent(
                request.getUserId(),
                request.getOperation(),
                request.getSecurityContext(),
                LocalDateTime.now()
        );
        securityManager.auditSecurityEvent(event);
    }
}

// Rate Limiting Decorator
@Component
public class RateLimitingRequestProcessor extends RequestProcessorDecorator {
    
    private final Map<String, RateLimiter> rateLimiters;
    
    public RateLimitingRequestProcessor(RequestProcessor requestProcessor) {
        super(requestProcessor);
        this.rateLimiters = new ConcurrentHashMap<>();
    }
    
    @Override
    public ProcessingResult process(EnterpriseRequest request) {
        String rateLimitKey = generateRateLimitKey(request);
        RateLimiter rateLimiter = rateLimiters.computeIfAbsent(rateLimitKey, k -> createRateLimiter());
        
        if (!rateLimiter.tryAcquire()) {
            return ProcessingResult.failure("Rate limit exceeded");
        }
        
        return super.process(request);
    }
    
    private String generateRateLimitKey(EnterpriseRequest request) {
        return String.format("ratelimit:%s:%s", request.getUserId(), request.getOperation());
    }
    
    private RateLimiter createRateLimiter() {
        // 10 requests per minute per user per operation
        return RateLimiter.create(10.0 / 60.0);
    }
}
```

---

## 5. Integration and Usage Examples

### 5.1 Framework Usage Examples

```java
// Enterprise Application Controller
@RestController
@RequestMapping("/api/v1/enterprise")
public class EnterpriseController {
    
    @Autowired
    private EnterpriseServiceFacade serviceFacade;
    
    @PostMapping("/process")
    public ResponseEntity<ProcessingResult> processRequest(@RequestBody EnterpriseRequestDto requestDto) {
        
        EnterpriseRequest request = EnterpriseRequest.builder()
                .withUserId(requestDto.getUserId())
                .withOperation(requestDto.getOperation())
                .withParameters(requestDto.getParameters())
                .withHeaders(requestDto.getHeaders())
                .withSecurityContext(createSecurityContext(requestDto))
                .withProcessingOptions(createProcessingOptions(requestDto))
                .build();
        
        ProcessingResult result = serviceFacade.processRequest(request);
        
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/queue")
    public ResponseEntity<String> queueRequest(@RequestBody EnterpriseRequestDto requestDto) {
        
        EnterpriseRequest request = EnterpriseRequest.builder()
                .withUserId(requestDto.getUserId())
                .withOperation(requestDto.getOperation())
                .withParameters(requestDto.getParameters())
                .withHeaders(requestDto.getHeaders())
                .withSecurityContext(createSecurityContext(requestDto))
                .withProcessingOptions(createProcessingOptions(requestDto))
                .build();
        
        serviceFacade.queueRequest(request);
        
        return ResponseEntity.ok("Request queued successfully");
    }
    
    @PostMapping("/process-pending")
    public ResponseEntity<ProcessingResult> processPendingRequests() {
        ProcessingResult result = serviceFacade.processPendingRequests();
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/undo")
    public ResponseEntity<String> undoLastOperation() {
        serviceFacade.undoLastOperation();
        return ResponseEntity.ok("Last operation undone");
    }
    
    @GetMapping("/status")
    public ResponseEntity<SystemStatus> getSystemStatus() {
        SystemStatus status = serviceFacade.getSystemStatus();
        return ResponseEntity.ok(status);
    }
    
    private SecurityContext createSecurityContext(EnterpriseRequestDto requestDto) {
        return new SecurityContext(
                requestDto.getToken(),
                requestDto.getRoles(),
                requestDto.getPermissions(),
                requestDto.getToken() != null
        );
    }
    
    private ProcessingOptions createProcessingOptions(EnterpriseRequestDto requestDto) {
        return new ProcessingOptions(
                requestDto.isAsynchronous(),
                requestDto.getTimeout(),
                requestDto.getRetryCount(),
                requestDto.isCacheEnabled(),
                requestDto.getPriority()
        );
    }
}

// Configuration Management Controller
@RestController
@RequestMapping("/api/v1/config")
public class ConfigurationController {
    
    private final ConfigurationManager configManager;
    
    public ConfigurationController() {
        this.configManager = ConfigurationManager.getInstance();
    }
    
    @GetMapping("/{key}")
    public ResponseEntity<Object> getConfiguration(@PathVariable String key) {
        Object value = configManager.getProperty(key, Object.class);
        return ResponseEntity.ok(value);
    }
    
    @PostMapping("/{key}")
    public ResponseEntity<String> setConfiguration(@PathVariable String key, @RequestBody Object value) {
        configManager.setProperty(key, value);
        return ResponseEntity.ok("Configuration updated");
    }
}
```

### 5.2 Complete Usage Example

```java
// Complete Enterprise Application Demo
@Component
public class EnterpriseFrameworkDemo {
    
    @Autowired
    private EnterpriseServiceFacade serviceFacade;
    
    @Autowired
    private EventPublisher eventPublisher;
    
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        demonstrateFramework();
    }
    
    private void demonstrateFramework() {
        System.out.println("=== Enterprise Framework Demo ===");
        
        // Subscribe to events
        subscribeToEvents();
        
        // Demonstrate different operations
        demonstrateDataOperation();
        demonstrateFileOperation();
        demonstrateNotificationOperation();
        
        // Demonstrate async processing
        demonstrateAsyncProcessing();
        
        // Demonstrate command pattern
        demonstrateCommandPattern();
        
        // Show system status
        showSystemStatus();
        
        System.out.println("=== Demo Complete ===");
    }
    
    private void subscribeToEvents() {
        eventPublisher.subscribe(new EventListener<RequestProcessedEvent>() {
            @Override
            public void onEvent(RequestProcessedEvent event) {
                System.out.println("Event: Request " + event.getRequestId() + 
                                 " processed in " + event.getProcessingTimeMs() + "ms");
            }
            
            @Override
            public Class<RequestProcessedEvent> getEventType() {
                return RequestProcessedEvent.class;
            }
        });
    }
    
    private void demonstrateDataOperation() {
        System.out.println("\n--- Data Operation Demo ---");
        
        EnterpriseRequest request = EnterpriseRequest.builder()
                .withUserId("user123")
                .withOperation("data.read")
                .withParameter("table", "users")
                .withParameter("id", "123")
                .withSecurityContext(createSecurityContext())
                .withProcessingOptions(ProcessingOptions.defaultOptions())
                .build();
        
        ProcessingResult result = serviceFacade.processRequest(request);
        System.out.println("Data operation result: " + result.isSuccess());
    }
    
    private void demonstrateFileOperation() {
        System.out.println("\n--- File Operation Demo ---");
        
        EnterpriseRequest request = EnterpriseRequest.builder()
                .withUserId("user123")
                .withOperation("file.read")
                .withParameter("fileName", "test.txt")
                .withSecurityContext(createSecurityContext())
                .withProcessingOptions(ProcessingOptions.defaultOptions())
                .build();
        
        ProcessingResult result = serviceFacade.processRequest(request);
        System.out.println("File operation result: " + result.isSuccess());
    }
    
    private void demonstrateNotificationOperation() {
        System.out.println("\n--- Notification Operation Demo ---");
        
        EnterpriseRequest request = EnterpriseRequest.builder()
                .withUserId("user123")
                .withOperation("notification.send")
                .withParameter("recipient", "user@example.com")
                .withParameter("message", "Hello from Enterprise Framework!")
                .withParameter("type", "email")
                .withSecurityContext(createSecurityContext())
                .withProcessingOptions(ProcessingOptions.defaultOptions())
                .build();
        
        ProcessingResult result = serviceFacade.processRequest(request);
        System.out.println("Notification operation result: " + result.isSuccess());
    }
    
    private void demonstrateAsyncProcessing() {
        System.out.println("\n--- Async Processing Demo ---");
        
        ProcessingOptions asyncOptions = new ProcessingOptions(
                true, 30, 3, true, Priority.HIGH
        );
        
        EnterpriseRequest request = EnterpriseRequest.builder()
                .withUserId("user123")
                .withOperation("data.process")
                .withParameter("operation", "batch_process")
                .withParameter("size", 1000)
                .withSecurityContext(createSecurityContext())
                .withProcessingOptions(asyncOptions)
                .build();
        
        ProcessingResult result = serviceFacade.processRequest(request);
        System.out.println("Async operation result: " + result.isSuccess());
    }
    
    private void demonstrateCommandPattern() {
        System.out.println("\n--- Command Pattern Demo ---");
        
        // Queue multiple requests
        for (int i = 0; i < 3; i++) {
            EnterpriseRequest request = EnterpriseRequest.builder()
                    .withUserId("user123")
                    .withOperation("data.update")
                    .withParameter("id", i)
                    .withParameter("value", "test" + i)
                    .withSecurityContext(createSecurityContext())
                    .withProcessingOptions(ProcessingOptions.defaultOptions())
                    .build();
            
            serviceFacade.queueRequest(request);
        }
        
        // Process all queued requests
        ProcessingResult batchResult = serviceFacade.processPendingRequests();
        System.out.println("Batch processing result: " + batchResult.isSuccess());
        
        // Demonstrate undo
        serviceFacade.undoLastOperation();
        System.out.println("Undo operation completed");
    }
    
    private void showSystemStatus() {
        System.out.println("\n--- System Status ---");
        
        SystemStatus status = serviceFacade.getSystemStatus();
        System.out.println("Framework Version: " + status.getVersion());
        System.out.println("Environment: " + status.getEnvironment());
        System.out.println("Timestamp: " + status.getTimestamp());
    }
    
    private SecurityContext createSecurityContext() {
        Set<String> roles = new HashSet<>();
        roles.add("USER");
        
        Set<String> permissions = new HashSet<>();
        permissions.add("READ");
        permissions.add("WRITE");
        
        return new SecurityContext("valid-token", roles, permissions, true);
    }
}
```

---

## 6. Key Design Patterns Summary

### Creational Patterns Used:
1. **Factory Pattern**: ComponentFactory for creating service components
2. **Abstract Factory**: Environment-specific component factories
3. **Builder Pattern**: Complex request building with validation
4. **Singleton Pattern**: Thread-safe configuration management

### Structural Patterns Used:
1. **Facade Pattern**: Simplified interface for complex enterprise operations
2. **Decorator Pattern**: Request processing enhancement with cross-cutting concerns
3. **Composite Pattern**: Hierarchical configuration structure

### Behavioral Patterns Used:
1. **Strategy Pattern**: Pluggable processing strategies
2. **Observer Pattern**: Event-driven architecture
3. **Command Pattern**: Request queuing and undo functionality
4. **State Pattern**: Request processing state management

---

## 7. Benefits and Best Practices

### Framework Benefits:
✅ **Modularity**: Clean separation of concerns through patterns  
✅ **Extensibility**: Easy to add new processing strategies and decorators  
✅ **Maintainability**: Clear structure and well-defined interfaces  
✅ **Testability**: Loosely coupled components for easy testing  
✅ **Scalability**: Async processing and caching support  
✅ **Observability**: Built-in event system for monitoring  

### Best Practices Demonstrated:
1. **Single Responsibility**: Each pattern has a focused purpose
2. **Open/Closed Principle**: Framework is open for extension, closed for modification
3. **Dependency Inversion**: Depends on abstractions, not concrete implementations
4. **Composition over Inheritance**: Uses composition for building complex behaviors
5. **Fail-Fast Design**: Early validation and clear error messages
6. **Thread Safety**: Proper synchronization in shared components

---

## 8. Week 8 Summary

This capstone project demonstrates the integration of all major design patterns into a cohesive enterprise framework:

### Patterns Mastered:
- **Creational**: Factory, Abstract Factory, Builder, Singleton
- **Structural**: Facade, Decorator, Composite
- **Behavioral**: Strategy, Observer, Command, State

### Real-World Application:
The framework provides a production-ready foundation for enterprise applications with:
- **Configurable Processing**: Multiple strategies for different operations
- **Cross-cutting Concerns**: Logging, caching, security, rate limiting
- **Event-Driven Architecture**: Loose coupling through events
- **Command Processing**: Queuing and undo capabilities
- **Monitoring and Observability**: Built-in metrics and auditing

### Key Takeaways:
1. Design patterns work best when combined thoughtfully
2. Patterns should solve real problems, not be used for their own sake
3. A well-designed framework reduces complexity for end users
4. Enterprise applications need cross-cutting concerns handled systematically
5. Proper abstraction makes systems more maintainable and extensible

---

**Practice Assignment**: Extend this framework by adding a new processing strategy for REST API calls and implement a retry decorator with exponential backoff.

---

*"Design patterns are not just about code structure - they're about creating a common vocabulary for discussing and solving design problems."*

*"The true power of design patterns emerges when they work together to create elegant solutions to complex problems."*