# Day 103: Production Engineering - SRE Practices, Performance Engineering & Capacity Planning

## Learning Objectives
- Master Site Reliability Engineering (SRE) principles and practices
- Implement comprehensive monitoring, alerting, and observability
- Design and execute performance engineering strategies
- Plan and implement capacity management for distributed systems
- Build production-ready systems with high availability and reliability

## Topics Covered
1. Site Reliability Engineering (SRE) Fundamentals
2. Observability: Metrics, Logging, and Distributed Tracing
3. Performance Engineering and Optimization
4. Capacity Planning and Resource Management
5. Incident Response and Post-Mortem Analysis
6. Service Level Objectives (SLOs) and Error Budgets
7. Production Deployment and Release Management

## 1. Site Reliability Engineering (SRE) Fundamentals

### SRE Principles and Service Level Indicators (SLIs)

```java
// Service Level Indicator (SLI) implementation
public interface ServiceLevelIndicator {
    String getName();
    double getValue();
    String getUnit();
    Instant getTimestamp();
}

// Availability SLI
@Component
public class AvailabilitySLI implements ServiceLevelIndicator {
    
    private final MeterRegistry meterRegistry;
    private final Timer requestTimer;
    private final Counter errorCounter;
    
    public AvailabilitySLI(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.requestTimer = Timer.builder("http.requests")
                .description("HTTP request duration")
                .register(meterRegistry);
        this.errorCounter = Counter.builder("http.requests.errors")
                .description("HTTP request errors")
                .register(meterRegistry);
    }
    
    @Override
    public String getName() {
        return "availability";
    }
    
    @Override
    public double getValue() {
        double totalRequests = requestTimer.count();
        double errorRequests = errorCounter.count();
        
        if (totalRequests == 0) {
            return 1.0; // 100% availability when no requests
        }
        
        return (totalRequests - errorRequests) / totalRequests;
    }
    
    @Override
    public String getUnit() {
        return "percentage";
    }
    
    @Override
    public Instant getTimestamp() {
        return Instant.now();
    }
}

// Latency SLI
@Component
public class LatencySLI implements ServiceLevelIndicator {
    
    private final Timer requestTimer;
    private final double percentile;
    
    public LatencySLI(MeterRegistry meterRegistry, 
                     @Value("${sre.latency.percentile:0.95}") double percentile) {
        this.requestTimer = Timer.builder("http.requests")
                .description("HTTP request duration")
                .register(meterRegistry);
        this.percentile = percentile;
    }
    
    @Override
    public String getName() {
        return "latency_p" + (int)(percentile * 100);
    }
    
    @Override
    public double getValue() {
        return requestTimer.percentile(percentile, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public String getUnit() {
        return "milliseconds";
    }
    
    @Override
    public Instant getTimestamp() {
        return Instant.now();
    }
}

// Throughput SLI
@Component
public class ThroughputSLI implements ServiceLevelIndicator {
    
    private final Timer requestTimer;
    private final TimeWindow timeWindow;
    
    public ThroughputSLI(MeterRegistry meterRegistry) {
        this.requestTimer = Timer.builder("http.requests")
                .description("HTTP request duration")
                .register(meterRegistry);
        this.timeWindow = TimeWindow.of(Duration.ofMinutes(1));
    }
    
    @Override
    public String getName() {
        return "throughput";
    }
    
    @Override
    public double getValue() {
        // Calculate requests per second over the time window
        return requestTimer.count() / timeWindow.getDuration().getSeconds();
    }
    
    @Override
    public String getUnit() {
        return "requests_per_second";
    }
    
    @Override
    public Instant getTimestamp() {
        return Instant.now();
    }
}

// Service Level Objective (SLO) implementation
public class ServiceLevelObjective {
    private final String name;
    private final ServiceLevelIndicator sli;
    private final double target;
    private final Duration timeWindow;
    private final String description;
    
    public ServiceLevelObjective(String name, ServiceLevelIndicator sli, 
                               double target, Duration timeWindow, String description) {
        this.name = name;
        this.sli = sli;
        this.target = target;
        this.timeWindow = timeWindow;
        this.description = description;
    }
    
    public boolean isMet() {
        return sli.getValue() >= target;
    }
    
    public double getCurrentValue() {
        return sli.getValue();
    }
    
    public double getErrorBudget() {
        if ("availability".equals(sli.getName())) {
            return 1.0 - target; // Error budget for availability
        }
        return target - sli.getValue(); // Error budget for latency/throughput
    }
    
    public double getErrorBudgetBurnRate() {
        if ("availability".equals(sli.getName())) {
            double currentErrorRate = 1.0 - sli.getValue();
            double allowedErrorRate = 1.0 - target;
            return currentErrorRate / allowedErrorRate;
        }
        return 0.0; // Simplified for this example
    }
    
    // Getters
    public String getName() { return name; }
    public ServiceLevelIndicator getSli() { return sli; }
    public double getTarget() { return target; }
    public Duration getTimeWindow() { return timeWindow; }
    public String getDescription() { return description; }
}

// SRE Service for managing SLOs
@Service
public class SREService {
    
    private final List<ServiceLevelObjective> slos;
    private final MeterRegistry meterRegistry;
    private final Logger logger = LoggerFactory.getLogger(SREService.class);
    
    public SREService(List<ServiceLevelIndicator> slis, MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.slos = initializeSLOs(slis);
        scheduleMetricsCollection();
    }
    
    private List<ServiceLevelObjective> initializeSLOs(List<ServiceLevelIndicator> slis) {
        List<ServiceLevelObjective> objectives = new ArrayList<>();
        
        for (ServiceLevelIndicator sli : slis) {
            switch (sli.getName()) {
                case "availability" -> objectives.add(new ServiceLevelObjective(
                        "availability_slo",
                        sli,
                        0.999, // 99.9% availability
                        Duration.ofDays(30),
                        "99.9% availability over 30 days"
                ));
                case "latency_p95" -> objectives.add(new ServiceLevelObjective(
                        "latency_slo",
                        sli,
                        200.0, // 200ms
                        Duration.ofMinutes(5),
                        "95th percentile latency under 200ms"
                ));
                case "throughput" -> objectives.add(new ServiceLevelObjective(
                        "throughput_slo",
                        sli,
                        1000.0, // 1000 RPS
                        Duration.ofMinutes(1),
                        "Minimum 1000 requests per second"
                ));
            }
        }
        
        return objectives;
    }
    
    private void scheduleMetricsCollection() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                collectSLOMetrics();
            } catch (Exception e) {
                logger.error("Error collecting SLO metrics", e);
            }
        }, 0, 30, TimeUnit.SECONDS);
    }
    
    private void collectSLOMetrics() {
        for (ServiceLevelObjective slo : slos) {
            double currentValue = slo.getCurrentValue();
            double target = slo.getTarget();
            boolean isMet = slo.isMet();
            double errorBudget = slo.getErrorBudget();
            double burnRate = slo.getErrorBudgetBurnRate();
            
            // Record metrics
            Gauge.builder("slo.current_value")
                    .tag("slo", slo.getName())
                    .register(meterRegistry, slo, ServiceLevelObjective::getCurrentValue);
            
            Gauge.builder("slo.target")
                    .tag("slo", slo.getName())
                    .register(meterRegistry, slo, ServiceLevelObjective::getTarget);
            
            Gauge.builder("slo.error_budget")
                    .tag("slo", slo.getName())
                    .register(meterRegistry, slo, ServiceLevelObjective::getErrorBudget);
            
            Gauge.builder("slo.burn_rate")
                    .tag("slo", slo.getName())
                    .register(meterRegistry, slo, ServiceLevelObjective::getErrorBudgetBurnRate);
            
            // Alert if SLO is at risk
            if (!isMet || burnRate > 1.0) {
                logger.warn("SLO {} is at risk: current={}, target={}, burn_rate={}",
                        slo.getName(), currentValue, target, burnRate);
            }
        }
    }
    
    public List<ServiceLevelObjective> getSLOs() {
        return Collections.unmodifiableList(slos);
    }
    
    public Optional<ServiceLevelObjective> getSLO(String name) {
        return slos.stream()
                .filter(slo -> slo.getName().equals(name))
                .findFirst();
    }
}

// Time window utility
public class TimeWindow {
    private final Duration duration;
    private final Instant start;
    
    private TimeWindow(Duration duration) {
        this.duration = duration;
        this.start = Instant.now().minus(duration);
    }
    
    public static TimeWindow of(Duration duration) {
        return new TimeWindow(duration);
    }
    
    public Duration getDuration() { return duration; }
    public Instant getStart() { return start; }
    public Instant getEnd() { return Instant.now(); }
}
```

## 2. Observability: Metrics, Logging, and Distributed Tracing

### Comprehensive Observability Implementation

```java
// Custom metrics for business logic
@Component
public class TradingMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter ordersPlaced;
    private final Counter ordersExecuted;
    private final Counter ordersCancelled;
    private final Timer orderProcessingTime;
    private final Gauge activeConnections;
    private final DistributionSummary orderValue;
    
    public TradingMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.ordersPlaced = Counter.builder("orders.placed")
                .description("Total number of orders placed")
                .tag("service", "trading")
                .register(meterRegistry);
        
        this.ordersExecuted = Counter.builder("orders.executed")
                .description("Total number of orders executed")
                .tag("service", "trading")
                .register(meterRegistry);
        
        this.ordersCancelled = Counter.builder("orders.cancelled")
                .description("Total number of orders cancelled")
                .tag("service", "trading")
                .register(meterRegistry);
        
        this.orderProcessingTime = Timer.builder("order.processing.time")
                .description("Time taken to process an order")
                .tag("service", "trading")
                .register(meterRegistry);
        
        this.activeConnections = Gauge.builder("connections.active")
                .description("Number of active connections")
                .tag("service", "trading")
                .register(meterRegistry, this, TradingMetrics::getActiveConnectionsCount);
        
        this.orderValue = DistributionSummary.builder("order.value")
                .description("Distribution of order values")
                .tag("service", "trading")
                .register(meterRegistry);
    }
    
    public void recordOrderPlaced(String symbol, OrderSide side, BigDecimal value) {
        ordersPlaced.increment(
                Tags.of(
                        Tag.of("symbol", symbol),
                        Tag.of("side", side.name())
                )
        );
        orderValue.record(value.doubleValue());
    }
    
    public void recordOrderExecuted(String symbol, OrderSide side) {
        ordersExecuted.increment(
                Tags.of(
                        Tag.of("symbol", symbol),
                        Tag.of("side", side.name())
                )
        );
    }
    
    public void recordOrderCancelled(String symbol, String reason) {
        ordersCancelled.increment(
                Tags.of(
                        Tag.of("symbol", symbol),
                        Tag.of("reason", reason)
                )
        );
    }
    
    public Timer.Sample startOrderProcessing() {
        return Timer.start(meterRegistry);
    }
    
    public void endOrderProcessing(Timer.Sample sample, String symbol, boolean success) {
        sample.stop(Timer.builder("order.processing.time")
                .tag("service", "trading")
                .tag("symbol", symbol)
                .tag("success", String.valueOf(success))
                .register(meterRegistry));
    }
    
    private double getActiveConnectionsCount() {
        // This would typically come from connection pool or similar
        return ThreadLocalRandom.current().nextDouble(50, 200);
    }
}

// Structured logging configuration
@Configuration
public class LoggingConfiguration {
    
    @Bean
    public Logger structuredLogger() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        // JSON encoder for structured logging
        JsonEncoder jsonEncoder = new JsonEncoder();
        jsonEncoder.setContext(context);
        jsonEncoder.start();
        
        // Console appender with JSON format
        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(context);
        consoleAppender.setEncoder(jsonEncoder);
        consoleAppender.start();
        
        // Root logger configuration
        ch.qos.logback.classic.Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(consoleAppender);
        rootLogger.setLevel(Level.INFO);
        
        return rootLogger;
    }
}

// Structured logging service
@Service
public class StructuredLoggingService {
    
    private final Logger logger = LoggerFactory.getLogger(StructuredLoggingService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public void logOrderEvent(String orderId, String traderId, String eventType, 
                             Map<String, Object> context) {
        try {
            Map<String, Object> logEvent = Map.of(
                    "timestamp", Instant.now().toString(),
                    "service", "trading-service",
                    "event_type", eventType,
                    "order_id", orderId,
                    "trader_id", traderId,
                    "context", context,
                    "trace_id", getCurrentTraceId(),
                    "span_id", getCurrentSpanId()
            );
            
            String jsonLog = objectMapper.writeValueAsString(logEvent);
            logger.info(jsonLog);
            
        } catch (Exception e) {
            logger.error("Failed to create structured log", e);
        }
    }
    
    public void logPerformanceMetric(String operation, long durationMs, 
                                   Map<String, Object> metadata) {
        try {
            Map<String, Object> performanceLog = Map.of(
                    "timestamp", Instant.now().toString(),
                    "service", "trading-service",
                    "metric_type", "performance",
                    "operation", operation,
                    "duration_ms", durationMs,
                    "metadata", metadata,
                    "trace_id", getCurrentTraceId()
            );
            
            String jsonLog = objectMapper.writeValueAsString(performanceLog);
            logger.info(jsonLog);
            
        } catch (Exception e) {
            logger.error("Failed to create performance log", e);
        }
    }
    
    public void logBusinessEvent(String eventType, Map<String, Object> businessData) {
        try {
            Map<String, Object> businessLog = Map.of(
                    "timestamp", Instant.now().toString(),
                    "service", "trading-service",
                    "event_category", "business",
                    "event_type", eventType,
                    "data", businessData,
                    "trace_id", getCurrentTraceId()
            );
            
            String jsonLog = objectMapper.writeValueAsString(businessLog);
            logger.info(jsonLog);
            
        } catch (Exception e) {
            logger.error("Failed to create business log", e);
        }
    }
    
    private String getCurrentTraceId() {
        // Get current trace ID from tracing context
        return Span.current().getSpanContext().getTraceId();
    }
    
    private String getCurrentSpanId() {
        // Get current span ID from tracing context
        return Span.current().getSpanContext().getSpanId();
    }
}

// Distributed tracing configuration
@Configuration
@EnableAutoConfiguration
public class TracingConfiguration {
    
    @Bean
    public OpenTelemetry openTelemetry() {
        return OpenTelemetrySdk.builder()
                .setTracerProvider(
                        SdkTracerProvider.builder()
                                .addSpanProcessor(BatchSpanProcessor.builder(
                                        OtlpGrpcSpanExporter.builder()
                                                .setEndpoint("http://jaeger:14250")
                                                .build())
                                        .build())
                                .setResource(Resource.getDefault()
                                        .merge(Resource.create(
                                                Attributes.of(ResourceAttributes.SERVICE_NAME, "trading-service"))))
                                .build())
                .build();
    }
    
    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("trading-service");
    }
}

// Custom tracing service
@Service
public class TracingService {
    
    private final Tracer tracer;
    
    public TracingService(Tracer tracer) {
        this.tracer = tracer;
    }
    
    public Span startSpan(String operationName, Map<String, String> attributes) {
        SpanBuilder spanBuilder = tracer.spanBuilder(operationName);
        
        for (Map.Entry<String, String> attr : attributes.entrySet()) {
            spanBuilder.setAttribute(attr.getKey(), attr.getValue());
        }
        
        return spanBuilder.startSpan();
    }
    
    public void addSpanEvent(Span span, String eventName, Map<String, String> attributes) {
        AttributesBuilder attributesBuilder = Attributes.builder();
        for (Map.Entry<String, String> attr : attributes.entrySet()) {
            attributesBuilder.put(attr.getKey(), attr.getValue());
        }
        
        span.addEvent(eventName, attributesBuilder.build());
    }
    
    public void recordException(Span span, Exception exception) {
        span.recordException(exception);
        span.setStatus(StatusCode.ERROR, exception.getMessage());
    }
    
    public <T> T trace(String operationName, Map<String, String> attributes, 
                      Supplier<T> operation) {
        Span span = startSpan(operationName, attributes);
        
        try (Scope scope = span.makeCurrent()) {
            T result = operation.get();
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            recordException(span, e);
            throw e;
        } finally {
            span.end();
        }
    }
}
```

## 3. Performance Engineering and Optimization

### Performance Monitoring and Optimization Framework

```java
// Performance monitoring service
@Service
public class PerformanceMonitoringService {
    
    private final MeterRegistry meterRegistry;
    private final StructuredLoggingService loggingService;
    private final Logger logger = LoggerFactory.getLogger(PerformanceMonitoringService.class);
    
    // Performance thresholds
    private static final long SLOW_QUERY_THRESHOLD_MS = 1000;
    private static final long SLOW_API_THRESHOLD_MS = 500;
    private static final double HIGH_CPU_THRESHOLD = 80.0;
    private static final double HIGH_MEMORY_THRESHOLD = 85.0;
    
    public PerformanceMonitoringService(MeterRegistry meterRegistry, 
                                      StructuredLoggingService loggingService) {
        this.meterRegistry = meterRegistry;
        this.loggingService = loggingService;
        startSystemMonitoring();
    }
    
    public void recordApiCall(String endpoint, String method, long durationMs, 
                            int statusCode, long requestSize, long responseSize) {
        // Record metrics
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("api.request.duration")
                .tag("endpoint", endpoint)
                .tag("method", method)
                .tag("status", String.valueOf(statusCode))
                .register(meterRegistry));
        
        Counter.builder("api.request.total")
                .tag("endpoint", endpoint)
                .tag("method", method)
                .tag("status", String.valueOf(statusCode))
                .register(meterRegistry)
                .increment();
        
        DistributionSummary.builder("api.request.size")
                .tag("endpoint", endpoint)
                .tag("type", "request")
                .register(meterRegistry)
                .record(requestSize);
        
        DistributionSummary.builder("api.response.size")
                .tag("endpoint", endpoint)
                .tag("type", "response")
                .register(meterRegistry)
                .record(responseSize);
        
        // Log slow APIs
        if (durationMs > SLOW_API_THRESHOLD_MS) {
            loggingService.logPerformanceMetric("slow_api", durationMs, Map.of(
                    "endpoint", endpoint,
                    "method", method,
                    "status_code", statusCode,
                    "request_size", requestSize,
                    "response_size", responseSize
            ));
        }
    }
    
    public void recordDatabaseQuery(String query, String table, long durationMs, 
                                  int rowsAffected) {
        Timer.builder("db.query.duration")
                .tag("table", table)
                .tag("operation", extractOperation(query))
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
        
        Counter.builder("db.query.total")
                .tag("table", table)
                .tag("operation", extractOperation(query))
                .register(meterRegistry)
                .increment();
        
        // Log slow queries
        if (durationMs > SLOW_QUERY_THRESHOLD_MS) {
            loggingService.logPerformanceMetric("slow_query", durationMs, Map.of(
                    "query", sanitizeQuery(query),
                    "table", table,
                    "rows_affected", rowsAffected
            ));
        }
    }
    
    public void recordCacheOperation(String cacheName, String operation, 
                                   boolean hit, long durationMs) {
        Timer.builder("cache.operation.duration")
                .tag("cache", cacheName)
                .tag("operation", operation)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
        
        Counter.builder("cache.operation.total")
                .tag("cache", cacheName)
                .tag("operation", operation)
                .tag("result", hit ? "hit" : "miss")
                .register(meterRegistry)
                .increment();
        
        // Calculate and record cache hit ratio
        double hitRatio = calculateCacheHitRatio(cacheName);
        Gauge.builder("cache.hit.ratio")
                .tag("cache", cacheName)
                .register(meterRegistry, this, ignored -> hitRatio);
    }
    
    private void startSystemMonitoring() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                monitorSystemResources();
            } catch (Exception e) {
                logger.error("Error monitoring system resources", e);
            }
        }, 0, 30, TimeUnit.SECONDS);
    }
    
    private void monitorSystemResources() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        
        // Memory monitoring
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
        long usedMemory = heapMemory.getUsed();
        long maxMemory = heapMemory.getMax();
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        
        Gauge.builder("system.memory.usage")
                .tag("type", "heap")
                .register(meterRegistry, this, ignored -> memoryUsagePercent);
        
        // CPU monitoring
        double cpuUsage = osBean.getProcessCpuLoad() * 100;
        Gauge.builder("system.cpu.usage")
                .register(meterRegistry, this, ignored -> cpuUsage);
        
        // Alert on high resource usage
        if (memoryUsagePercent > HIGH_MEMORY_THRESHOLD) {
            logger.warn("High memory usage detected: {}%", memoryUsagePercent);
            loggingService.logBusinessEvent("high_memory_usage", Map.of(
                    "memory_usage_percent", memoryUsagePercent,
                    "used_memory_mb", usedMemory / 1024 / 1024,
                    "max_memory_mb", maxMemory / 1024 / 1024
            ));
        }
        
        if (cpuUsage > HIGH_CPU_THRESHOLD) {
            logger.warn("High CPU usage detected: {}%", cpuUsage);
            loggingService.logBusinessEvent("high_cpu_usage", Map.of(
                    "cpu_usage_percent", cpuUsage
            ));
        }
    }
    
    private String extractOperation(String query) {
        String upperQuery = query.trim().toUpperCase();
        if (upperQuery.startsWith("SELECT")) return "SELECT";
        if (upperQuery.startsWith("INSERT")) return "INSERT";
        if (upperQuery.startsWith("UPDATE")) return "UPDATE";
        if (upperQuery.startsWith("DELETE")) return "DELETE";
        return "OTHER";
    }
    
    private String sanitizeQuery(String query) {
        // Remove sensitive data from query for logging
        return query.replaceAll("'[^']*'", "'?'")
                   .replaceAll("\\d+", "?");
    }
    
    private double calculateCacheHitRatio(String cacheName) {
        // Simplified calculation - in real implementation, track hits/misses
        return 0.85; // 85% hit ratio
    }
}

// Performance optimization aspect
@Aspect
@Component
public class PerformanceOptimizationAspect {
    
    private final PerformanceMonitoringService performanceService;
    private final Logger logger = LoggerFactory.getLogger(PerformanceOptimizationAspect.class);
    
    public PerformanceOptimizationAspect(PerformanceMonitoringService performanceService) {
        this.performanceService = performanceService;
    }
    
    @Around("@annotation(Monitored)")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            // Log method performance
            performanceService.recordApiCall(
                    className + "." + methodName,
                    "METHOD",
                    duration,
                    200,
                    0,
                    0
            );
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            performanceService.recordApiCall(
                    className + "." + methodName,
                    "METHOD",
                    duration,
                    500,
                    0,
                    0
            );
            
            throw e;
        }
    }
}

// Performance monitoring annotation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Monitored {
    String value() default "";
}

// Performance optimization utilities
@Component
public class PerformanceOptimizer {
    
    private final Logger logger = LoggerFactory.getLogger(PerformanceOptimizer.class);
    
    // Connection pooling optimization
    @Bean
    public HikariDataSource optimizedDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/trading");
        config.setUsername("trading_user");
        config.setPassword("trading_password");
        
        // Connection pool optimization
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);
        
        // Performance optimization
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        
        return new HikariDataSource(config);
    }
    
    // Cache optimization
    @Bean
    public CacheManager optimizedCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .recordStats()
                .build());
        
        return cacheManager;
    }
    
    // JVM optimization recommendations
    public void logJVMOptimizationRecommendations() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        Map<String, Object> jvmStats = Map.of(
                "max_memory_mb", maxMemory / 1024 / 1024,
                "total_memory_mb", totalMemory / 1024 / 1024,
                "used_memory_mb", usedMemory / 1024 / 1024,
                "free_memory_mb", freeMemory / 1024 / 1024,
                "memory_usage_percent", (double) usedMemory / maxMemory * 100
        );
        
        logger.info("JVM Memory Statistics: {}", jvmStats);
        
        // Recommendations based on usage patterns
        if (usedMemory > maxMemory * 0.8) {
            logger.warn("High memory usage detected. Consider increasing heap size or optimizing memory usage");
        }
        
        if (totalMemory > maxMemory * 0.9) {
            logger.warn("Close to maximum heap size. Consider tuning GC settings");
        }
    }
}
```

## 4. Capacity Planning and Resource Management

### Capacity Planning Framework

```java
// Capacity planning service
@Service
public class CapacityPlanningService {
    
    private final MeterRegistry meterRegistry;
    private final StructuredLoggingService loggingService;
    private final Logger logger = LoggerFactory.getLogger(CapacityPlanningService.class);
    
    // Capacity thresholds
    private static final double CPU_SCALE_UP_THRESHOLD = 70.0;
    private static final double CPU_SCALE_DOWN_THRESHOLD = 30.0;
    private static final double MEMORY_SCALE_UP_THRESHOLD = 80.0;
    private static final long THROUGHPUT_SCALE_UP_THRESHOLD = 1000; // RPS
    private static final double RESPONSE_TIME_THRESHOLD = 200.0; // ms
    
    private final AtomicInteger currentInstances = new AtomicInteger(3);
    private final AtomicInteger targetInstances = new AtomicInteger(3);
    
    public CapacityPlanningService(MeterRegistry meterRegistry, 
                                 StructuredLoggingService loggingService) {
        this.meterRegistry = meterRegistry;
        this.loggingService = loggingService;
        startCapacityMonitoring();
    }
    
    public CapacityRecommendation analyzeCurrentCapacity() {
        CapacityMetrics metrics = collectCapacityMetrics();
        
        return CapacityRecommendation.builder()
                .currentInstances(currentInstances.get())
                .recommendedInstances(calculateRecommendedInstances(metrics))
                .cpuUtilization(metrics.getCpuUtilization())
                .memoryUtilization(metrics.getMemoryUtilization())
                .throughput(metrics.getThroughput())
                .averageResponseTime(metrics.getAverageResponseTime())
                .reasoning(generateRecommendationReasoning(metrics))
                .confidence(calculateConfidence(metrics))
                .build();
    }
    
    public void executeCapacityChange(int newInstanceCount, String reason) {
        int oldCount = currentInstances.get();
        
        if (newInstanceCount != oldCount) {
            logger.info("Scaling from {} to {} instances. Reason: {}", 
                    oldCount, newInstanceCount, reason);
            
            // Simulate scaling operation
            simulateScaling(oldCount, newInstanceCount);
            
            currentInstances.set(newInstanceCount);
            
            loggingService.logBusinessEvent("capacity_change", Map.of(
                    "old_instances", oldCount,
                    "new_instances", newInstanceCount,
                    "reason", reason,
                    "scaling_direction", newInstanceCount > oldCount ? "up" : "down"
            ));
            
            // Update metrics
            Gauge.builder("cluster.instances.current")
                    .register(meterRegistry, currentInstances, AtomicInteger::get);
        }
    }
    
    private CapacityMetrics collectCapacityMetrics() {
        // Collect metrics from various sources
        double cpuUtilization = getCpuUtilization();
        double memoryUtilization = getMemoryUtilization();
        long throughput = getThroughput();
        double responseTime = getAverageResponseTime();
        long errorRate = getErrorRate();
        
        return new CapacityMetrics(cpuUtilization, memoryUtilization, 
                throughput, responseTime, errorRate);
    }
    
    private int calculateRecommendedInstances(CapacityMetrics metrics) {
        int current = currentInstances.get();
        double cpuFactor = calculateScalingFactor(metrics.getCpuUtilization(), 
                CPU_SCALE_UP_THRESHOLD, CPU_SCALE_DOWN_THRESHOLD);
        double memoryFactor = calculateScalingFactor(metrics.getMemoryUtilization(), 
                MEMORY_SCALE_UP_THRESHOLD, 40.0);
        double throughputFactor = metrics.getThroughput() > THROUGHPUT_SCALE_UP_THRESHOLD ? 1.2 : 1.0;
        double responseTimeFactor = metrics.getAverageResponseTime() > RESPONSE_TIME_THRESHOLD ? 1.3 : 1.0;
        
        // Take the maximum scaling factor
        double scalingFactor = Math.max(Math.max(cpuFactor, memoryFactor), 
                Math.max(throughputFactor, responseTimeFactor));
        
        int recommended = (int) Math.ceil(current * scalingFactor);
        
        // Apply constraints
        return Math.max(2, Math.min(20, recommended)); // Min 2, Max 20 instances
    }
    
    private double calculateScalingFactor(double utilization, double scaleUpThreshold, 
                                        double scaleDownThreshold) {
        if (utilization > scaleUpThreshold) {
            return 1.0 + (utilization - scaleUpThreshold) / 100.0;
        } else if (utilization < scaleDownThreshold) {
            return Math.max(0.5, utilization / scaleUpThreshold);
        }
        return 1.0;
    }
    
    private String generateRecommendationReasoning(CapacityMetrics metrics) {
        List<String> reasons = new ArrayList<>();
        
        if (metrics.getCpuUtilization() > CPU_SCALE_UP_THRESHOLD) {
            reasons.add("High CPU utilization: " + metrics.getCpuUtilization() + "%");
        }
        if (metrics.getMemoryUtilization() > MEMORY_SCALE_UP_THRESHOLD) {
            reasons.add("High memory utilization: " + metrics.getMemoryUtilization() + "%");
        }
        if (metrics.getThroughput() > THROUGHPUT_SCALE_UP_THRESHOLD) {
            reasons.add("High throughput: " + metrics.getThroughput() + " RPS");
        }
        if (metrics.getAverageResponseTime() > RESPONSE_TIME_THRESHOLD) {
            reasons.add("High response time: " + metrics.getAverageResponseTime() + "ms");
        }
        
        if (reasons.isEmpty()) {
            if (metrics.getCpuUtilization() < CPU_SCALE_DOWN_THRESHOLD) {
                reasons.add("Low resource utilization allows scale-down");
            } else {
                reasons.add("Current capacity is adequate");
            }
        }
        
        return String.join("; ", reasons);
    }
    
    private double calculateConfidence(CapacityMetrics metrics) {
        // Simple confidence calculation based on metric stability
        double confidence = 0.8; // Base confidence
        
        // Increase confidence if multiple metrics point in same direction
        boolean needsScaleUp = metrics.getCpuUtilization() > CPU_SCALE_UP_THRESHOLD ||
                              metrics.getMemoryUtilization() > MEMORY_SCALE_UP_THRESHOLD ||
                              metrics.getAverageResponseTime() > RESPONSE_TIME_THRESHOLD;
        
        if (needsScaleUp) {
            confidence += 0.15;
        }
        
        return Math.min(1.0, confidence);
    }
    
    private void startCapacityMonitoring() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                CapacityRecommendation recommendation = analyzeCurrentCapacity();
                
                // Auto-scaling logic
                if (recommendation.getConfidence() > 0.8 && 
                    Math.abs(recommendation.getRecommendedInstances() - 
                    recommendation.getCurrentInstances()) >= 1) {
                    
                    executeCapacityChange(recommendation.getRecommendedInstances(), 
                            recommendation.getReasoning());
                }
                
                // Log capacity metrics
                loggingService.logBusinessEvent("capacity_analysis", Map.of(
                        "current_instances", recommendation.getCurrentInstances(),
                        "recommended_instances", recommendation.getRecommendedInstances(),
                        "cpu_utilization", recommendation.getCpuUtilization(),
                        "memory_utilization", recommendation.getMemoryUtilization(),
                        "throughput", recommendation.getThroughput(),
                        "response_time", recommendation.getAverageResponseTime(),
                        "confidence", recommendation.getConfidence()
                ));
                
            } catch (Exception e) {
                logger.error("Error in capacity monitoring", e);
            }
        }, 0, 2, TimeUnit.MINUTES);
    }
    
    private void simulateScaling(int oldCount, int newCount) {
        // Simulate scaling delay
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // Metric collection methods (simplified for example)
    private double getCpuUtilization() {
        return ThreadLocalRandom.current().nextDouble(20, 90);
    }
    
    private double getMemoryUtilization() {
        return ThreadLocalRandom.current().nextDouble(30, 85);
    }
    
    private long getThroughput() {
        return ThreadLocalRandom.current().nextLong(500, 1500);
    }
    
    private double getAverageResponseTime() {
        return ThreadLocalRandom.current().nextDouble(100, 300);
    }
    
    private long getErrorRate() {
        return ThreadLocalRandom.current().nextLong(0, 10);
    }
}

// Capacity metrics data class
public class CapacityMetrics {
    private final double cpuUtilization;
    private final double memoryUtilization;
    private final long throughput;
    private final double averageResponseTime;
    private final long errorRate;
    
    public CapacityMetrics(double cpuUtilization, double memoryUtilization, 
                          long throughput, double averageResponseTime, long errorRate) {
        this.cpuUtilization = cpuUtilization;
        this.memoryUtilization = memoryUtilization;
        this.throughput = throughput;
        this.averageResponseTime = averageResponseTime;
        this.errorRate = errorRate;
    }
    
    // Getters
    public double getCpuUtilization() { return cpuUtilization; }
    public double getMemoryUtilization() { return memoryUtilization; }
    public long getThroughput() { return throughput; }
    public double getAverageResponseTime() { return averageResponseTime; }
    public long getErrorRate() { return errorRate; }
}

// Capacity recommendation data class
public class CapacityRecommendation {
    private final int currentInstances;
    private final int recommendedInstances;
    private final double cpuUtilization;
    private final double memoryUtilization;
    private final long throughput;
    private final double averageResponseTime;
    private final String reasoning;
    private final double confidence;
    
    private CapacityRecommendation(Builder builder) {
        this.currentInstances = builder.currentInstances;
        this.recommendedInstances = builder.recommendedInstances;
        this.cpuUtilization = builder.cpuUtilization;
        this.memoryUtilization = builder.memoryUtilization;
        this.throughput = builder.throughput;
        this.averageResponseTime = builder.averageResponseTime;
        this.reasoning = builder.reasoning;
        this.confidence = builder.confidence;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public int getCurrentInstances() { return currentInstances; }
    public int getRecommendedInstances() { return recommendedInstances; }
    public double getCpuUtilization() { return cpuUtilization; }
    public double getMemoryUtilization() { return memoryUtilization; }
    public long getThroughput() { return throughput; }
    public double getAverageResponseTime() { return averageResponseTime; }
    public String getReasoning() { return reasoning; }
    public double getConfidence() { return confidence; }
    
    public static class Builder {
        private int currentInstances;
        private int recommendedInstances;
        private double cpuUtilization;
        private double memoryUtilization;
        private long throughput;
        private double averageResponseTime;
        private String reasoning;
        private double confidence;
        
        public Builder currentInstances(int currentInstances) {
            this.currentInstances = currentInstances;
            return this;
        }
        
        public Builder recommendedInstances(int recommendedInstances) {
            this.recommendedInstances = recommendedInstances;
            return this;
        }
        
        public Builder cpuUtilization(double cpuUtilization) {
            this.cpuUtilization = cpuUtilization;
            return this;
        }
        
        public Builder memoryUtilization(double memoryUtilization) {
            this.memoryUtilization = memoryUtilization;
            return this;
        }
        
        public Builder throughput(long throughput) {
            this.throughput = throughput;
            return this;
        }
        
        public Builder averageResponseTime(double averageResponseTime) {
            this.averageResponseTime = averageResponseTime;
            return this;
        }
        
        public Builder reasoning(String reasoning) {
            this.reasoning = reasoning;
            return this;
        }
        
        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }
        
        public CapacityRecommendation build() {
            return new CapacityRecommendation(this);
        }
    }
}
```

## 5. Incident Response and Post-Mortem Analysis

### Incident Management Framework

```java
// Incident severity levels
public enum IncidentSeverity {
    SEV1("Critical - Complete service outage"),
    SEV2("High - Major feature unavailable"),
    SEV3("Medium - Minor feature impacted"),
    SEV4("Low - Cosmetic or documentation issue");
    
    private final String description;
    
    IncidentSeverity(String description) {
        this.description = description;
    }
    
    public String getDescription() { return description; }
}

// Incident status
public enum IncidentStatus {
    OPEN, INVESTIGATING, IDENTIFIED, MONITORING, RESOLVED, CLOSED
}

// Incident model
public class Incident {
    private final String incidentId;
    private final String title;
    private final String description;
    private final IncidentSeverity severity;
    private IncidentStatus status;
    private final Instant createdAt;
    private Instant resolvedAt;
    private final String reportedBy;
    private String assignedTo;
    private final List<String> affectedServices;
    private final List<IncidentUpdate> updates;
    private final Map<String, String> metadata;
    
    public Incident(String title, String description, IncidentSeverity severity, 
                   String reportedBy, List<String> affectedServices) {
        this.incidentId = "INC-" + System.currentTimeMillis();
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.status = IncidentStatus.OPEN;
        this.createdAt = Instant.now();
        this.reportedBy = reportedBy;
        this.affectedServices = new ArrayList<>(affectedServices);
        this.updates = new ArrayList<>();
        this.metadata = new HashMap<>();
    }
    
    public void addUpdate(String message, String updatedBy) {
        IncidentUpdate update = new IncidentUpdate(message, updatedBy, Instant.now());
        updates.add(update);
    }
    
    public void updateStatus(IncidentStatus newStatus, String updatedBy, String reason) {
        IncidentStatus oldStatus = this.status;
        this.status = newStatus;
        
        String message = String.format("Status changed from %s to %s. Reason: %s", 
                oldStatus, newStatus, reason);
        addUpdate(message, updatedBy);
        
        if (newStatus == IncidentStatus.RESOLVED && resolvedAt == null) {
            resolvedAt = Instant.now();
        }
    }
    
    public Duration getTimeToResolution() {
        if (resolvedAt != null) {
            return Duration.between(createdAt, resolvedAt);
        }
        return Duration.between(createdAt, Instant.now());
    }
    
    // Getters and setters
    public String getIncidentId() { return incidentId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public IncidentSeverity getSeverity() { return severity; }
    public IncidentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public String getReportedBy() { return reportedBy; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public List<String> getAffectedServices() { return Collections.unmodifiableList(affectedServices); }
    public List<IncidentUpdate> getUpdates() { return Collections.unmodifiableList(updates); }
    public Map<String, String> getMetadata() { return Collections.unmodifiableMap(metadata); }
    public void addMetadata(String key, String value) { metadata.put(key, value); }
}

// Incident update
public class IncidentUpdate {
    private final String message;
    private final String updatedBy;
    private final Instant timestamp;
    
    public IncidentUpdate(String message, String updatedBy, Instant timestamp) {
        this.message = message;
        this.updatedBy = updatedBy;
        this.timestamp = timestamp;
    }
    
    // Getters
    public String getMessage() { return message; }
    public String getUpdatedBy() { return updatedBy; }
    public Instant getTimestamp() { return timestamp; }
}

// Incident management service
@Service
public class IncidentManagementService {
    
    private final Map<String, Incident> incidents = new ConcurrentHashMap<>();
    private final StructuredLoggingService loggingService;
    private final NotificationService notificationService;
    private final Logger logger = LoggerFactory.getLogger(IncidentManagementService.class);
    
    public IncidentManagementService(StructuredLoggingService loggingService,
                                   NotificationService notificationService) {
        this.loggingService = loggingService;
        this.notificationService = notificationService;
    }
    
    public Incident createIncident(String title, String description, 
                                 IncidentSeverity severity, String reportedBy, 
                                 List<String> affectedServices) {
        Incident incident = new Incident(title, description, severity, 
                reportedBy, affectedServices);
        
        incidents.put(incident.getIncidentId(), incident);
        
        // Log incident creation
        loggingService.logBusinessEvent("incident_created", Map.of(
                "incident_id", incident.getIncidentId(),
                "title", title,
                "severity", severity.name(),
                "reported_by", reportedBy,
                "affected_services", affectedServices
        ));
        
        // Send notifications based on severity
        notifyIncidentStakeholders(incident, "created");
        
        logger.info("Incident created: {} - {}", incident.getIncidentId(), title);
        
        return incident;
    }
    
    public void updateIncidentStatus(String incidentId, IncidentStatus newStatus, 
                                   String updatedBy, String reason) {
        Incident incident = incidents.get(incidentId);
        if (incident != null) {
            IncidentStatus oldStatus = incident.getStatus();
            incident.updateStatus(newStatus, updatedBy, reason);
            
            loggingService.logBusinessEvent("incident_status_updated", Map.of(
                    "incident_id", incidentId,
                    "old_status", oldStatus.name(),
                    "new_status", newStatus.name(),
                    "updated_by", updatedBy,
                    "reason", reason
            ));
            
            notifyIncidentStakeholders(incident, "status_updated");
        }
    }
    
    public void addIncidentUpdate(String incidentId, String message, String updatedBy) {
        Incident incident = incidents.get(incidentId);
        if (incident != null) {
            incident.addUpdate(message, updatedBy);
            
            loggingService.logBusinessEvent("incident_updated", Map.of(
                    "incident_id", incidentId,
                    "message", message,
                    "updated_by", updatedBy
            ));
        }
    }
    
    public List<Incident> getActiveIncidents() {
        return incidents.values().stream()
                .filter(incident -> incident.getStatus() != IncidentStatus.CLOSED)
                .sorted(Comparator.comparing(Incident::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }
    
    public Optional<Incident> getIncident(String incidentId) {
        return Optional.ofNullable(incidents.get(incidentId));
    }
    
    public IncidentStatistics getIncidentStatistics(Duration timeWindow) {
        Instant since = Instant.now().minus(timeWindow);
        
        List<Incident> recentIncidents = incidents.values().stream()
                .filter(incident -> incident.getCreatedAt().isAfter(since))
                .collect(Collectors.toList());
        
        Map<IncidentSeverity, Long> severityBreakdown = recentIncidents.stream()
                .collect(Collectors.groupingBy(Incident::getSeverity, Collectors.counting()));
        
        double averageResolutionTime = recentIncidents.stream()
                .filter(incident -> incident.getResolvedAt() != null)
                .mapToLong(incident -> incident.getTimeToResolution().toMinutes())
                .average()
                .orElse(0.0);
        
        return new IncidentStatistics(
                recentIncidents.size(),
                severityBreakdown,
                averageResolutionTime,
                timeWindow
        );
    }
    
    private void notifyIncidentStakeholders(Incident incident, String action) {
        // Determine notification recipients based on severity
        List<String> recipients = determineNotificationRecipients(incident.getSeverity());
        
        String subject = String.format("Incident %s: %s - %s", 
                action.toUpperCase(), incident.getIncidentId(), incident.getTitle());
        
        String message = formatIncidentNotification(incident, action);
        
        for (String recipient : recipients) {
            notificationService.sendNotification(recipient, subject, message);
        }
    }
    
    private List<String> determineNotificationRecipients(IncidentSeverity severity) {
        return switch (severity) {
            case SEV1 -> List.of("oncall-engineer", "team-lead", "director");
            case SEV2 -> List.of("oncall-engineer", "team-lead");
            case SEV3 -> List.of("oncall-engineer");
            case SEV4 -> List.of(); // No immediate notifications for low severity
        };
    }
    
    private String formatIncidentNotification(Incident incident, String action) {
        return String.format("""
                Incident %s has been %s
                
                ID: %s
                Title: %s
                Severity: %s (%s)
                Status: %s
                Affected Services: %s
                Reported By: %s
                Created At: %s
                
                Description:
                %s
                """,
                incident.getIncidentId(), action,
                incident.getIncidentId(),
                incident.getTitle(),
                incident.getSeverity().name(), incident.getSeverity().getDescription(),
                incident.getStatus().name(),
                String.join(", ", incident.getAffectedServices()),
                incident.getReportedBy(),
                incident.getCreatedAt(),
                incident.getDescription()
        );
    }
}

// Incident statistics
public class IncidentStatistics {
    private final int totalIncidents;
    private final Map<IncidentSeverity, Long> severityBreakdown;
    private final double averageResolutionTimeMinutes;
    private final Duration timeWindow;
    
    public IncidentStatistics(int totalIncidents, 
                            Map<IncidentSeverity, Long> severityBreakdown,
                            double averageResolutionTimeMinutes, 
                            Duration timeWindow) {
        this.totalIncidents = totalIncidents;
        this.severityBreakdown = severityBreakdown;
        this.averageResolutionTimeMinutes = averageResolutionTimeMinutes;
        this.timeWindow = timeWindow;
    }
    
    // Getters
    public int getTotalIncidents() { return totalIncidents; }
    public Map<IncidentSeverity, Long> getSeverityBreakdown() { return severityBreakdown; }
    public double getAverageResolutionTimeMinutes() { return averageResolutionTimeMinutes; }
    public Duration getTimeWindow() { return timeWindow; }
}

// Post-mortem analysis
@Service
public class PostMortemService {
    
    private final StructuredLoggingService loggingService;
    private final Logger logger = LoggerFactory.getLogger(PostMortemService.class);
    
    public PostMortemService(StructuredLoggingService loggingService) {
        this.loggingService = loggingService;
    }
    
    public PostMortem generatePostMortem(Incident incident, PostMortemData data) {
        PostMortem postMortem = new PostMortem(
                incident.getIncidentId(),
                incident.getTitle(),
                incident.getSeverity(),
                incident.getCreatedAt(),
                incident.getResolvedAt(),
                data.getRootCause(),
                data.getImpactDescription(),
                data.getDetectionDetails(),
                data.getResponseActions(),
                data.getLessonsLearned(),
                data.getActionItems()
        );
        
        loggingService.logBusinessEvent("post_mortem_created", Map.of(
                "incident_id", incident.getIncidentId(),
                "root_cause", data.getRootCause(),
                "lessons_learned_count", data.getLessonsLearned().size(),
                "action_items_count", data.getActionItems().size()
        ));
        
        logger.info("Post-mortem created for incident: {}", incident.getIncidentId());
        
        return postMortem;
    }
}

// Post-mortem data structures
public class PostMortem {
    private final String incidentId;
    private final String title;
    private final IncidentSeverity severity;
    private final Instant incidentStart;
    private final Instant incidentEnd;
    private final String rootCause;
    private final String impactDescription;
    private final String detectionDetails;
    private final List<String> responseActions;
    private final List<String> lessonsLearned;
    private final List<ActionItem> actionItems;
    private final Instant createdAt;
    
    public PostMortem(String incidentId, String title, IncidentSeverity severity,
                     Instant incidentStart, Instant incidentEnd, String rootCause,
                     String impactDescription, String detectionDetails,
                     List<String> responseActions, List<String> lessonsLearned,
                     List<ActionItem> actionItems) {
        this.incidentId = incidentId;
        this.title = title;
        this.severity = severity;
        this.incidentStart = incidentStart;
        this.incidentEnd = incidentEnd;
        this.rootCause = rootCause;
        this.impactDescription = impactDescription;
        this.detectionDetails = detectionDetails;
        this.responseActions = new ArrayList<>(responseActions);
        this.lessonsLearned = new ArrayList<>(lessonsLearned);
        this.actionItems = new ArrayList<>(actionItems);
        this.createdAt = Instant.now();
    }
    
    // Getters
    public String getIncidentId() { return incidentId; }
    public String getTitle() { return title; }
    public IncidentSeverity getSeverity() { return severity; }
    public Instant getIncidentStart() { return incidentStart; }
    public Instant getIncidentEnd() { return incidentEnd; }
    public String getRootCause() { return rootCause; }
    public String getImpactDescription() { return impactDescription; }
    public String getDetectionDetails() { return detectionDetails; }
    public List<String> getResponseActions() { return Collections.unmodifiableList(responseActions); }
    public List<String> getLessonsLearned() { return Collections.unmodifiableList(lessonsLearned); }
    public List<ActionItem> getActionItems() { return Collections.unmodifiableList(actionItems); }
    public Instant getCreatedAt() { return createdAt; }
}

public class PostMortemData {
    private final String rootCause;
    private final String impactDescription;
    private final String detectionDetails;
    private final List<String> responseActions;
    private final List<String> lessonsLearned;
    private final List<ActionItem> actionItems;
    
    public PostMortemData(String rootCause, String impactDescription, String detectionDetails,
                         List<String> responseActions, List<String> lessonsLearned,
                         List<ActionItem> actionItems) {
        this.rootCause = rootCause;
        this.impactDescription = impactDescription;
        this.detectionDetails = detectionDetails;
        this.responseActions = responseActions;
        this.lessonsLearned = lessonsLearned;
        this.actionItems = actionItems;
    }
    
    // Getters
    public String getRootCause() { return rootCause; }
    public String getImpactDescription() { return impactDescription; }
    public String getDetectionDetails() { return detectionDetails; }
    public List<String> getResponseActions() { return responseActions; }
    public List<String> getLessonsLearned() { return lessonsLearned; }
    public List<ActionItem> getActionItems() { return actionItems; }
}

public class ActionItem {
    private final String description;
    private final String assignee;
    private final Instant dueDate;
    private final String priority;
    
    public ActionItem(String description, String assignee, Instant dueDate, String priority) {
        this.description = description;
        this.assignee = assignee;
        this.dueDate = dueDate;
        this.priority = priority;
    }
    
    // Getters
    public String getDescription() { return description; }
    public String getAssignee() { return assignee; }
    public Instant getDueDate() { return dueDate; }
    public String getPriority() { return priority; }
}
```

## Key Takeaways

1. **SRE Principles**: Implement measurable reliability targets with SLIs/SLOs and error budgets
2. **Comprehensive Observability**: Use metrics, logs, and traces for complete system visibility
3. **Performance Engineering**: Proactive monitoring and optimization of system performance
4. **Capacity Planning**: Data-driven scaling decisions with automated capacity management
5. **Incident Response**: Structured approach to incident management and resolution
6. **Post-Mortem Analysis**: Learn from failures to improve system reliability
7. **Production Readiness**: Build systems designed for production operation and maintenance

These practices ensure reliable, observable, and scalable production systems that can handle real-world operational challenges while maintaining high availability and performance.