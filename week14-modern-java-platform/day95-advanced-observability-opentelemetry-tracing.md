# Day 95: Advanced Observability - OpenTelemetry & Distributed Tracing

## Learning Objectives
- Master OpenTelemetry instrumentation and configuration
- Implement distributed tracing across microservices
- Build comprehensive monitoring and alerting systems
- Understand metrics, logs, and traces correlation
- Create production-ready observability solutions

## 1. OpenTelemetry Fundamentals

### Understanding OpenTelemetry

OpenTelemetry provides a unified approach to collecting, processing, and exporting telemetry data (metrics, logs, and traces).

```java
package com.javajourney.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@SpringBootApplication
public class ObservabilityApplication {
    
    public static void main(String[] args) {
        // Configure OpenTelemetry before starting the application
        System.setProperty("otel.service.name", "observability-demo");
        System.setProperty("otel.service.version", "1.0.0");
        System.setProperty("otel.resource.attributes", 
            "environment=development,team=platform,region=us-east-1");
        
        SpringApplication.run(ObservabilityApplication.class, args);
    }
}

@Configuration
public class OpenTelemetryConfiguration {
    
    @Bean
    public OpenTelemetry openTelemetry() {
        return AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();
    }
    
    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("com.javajourney.observability");
    }
    
    @Bean
    public Meter meter(OpenTelemetry openTelemetry) {
        return openTelemetry.getMeter("com.javajourney.observability");
    }
    
    @Bean
    public TelemetryService telemetryService(Tracer tracer, Meter meter) {
        return new TelemetryService(tracer, meter);
    }
}

// Core telemetry service
class TelemetryService {
    
    private final Tracer tracer;
    private final Meter meter;
    
    // Metrics
    private final LongCounter requestCounter;
    private final LongCounter errorCounter;
    private final LongCounter databaseQueryCounter;
    
    // Attribute keys
    private static final AttributeKey<String> OPERATION_NAME = AttributeKey.stringKey("operation.name");
    private static final AttributeKey<String> USER_ID = AttributeKey.stringKey("user.id");
    private static final AttributeKey<String> ERROR_TYPE = AttributeKey.stringKey("error.type");
    private static final AttributeKey<Long> DURATION_MS = AttributeKey.longKey("duration.ms");
    
    public TelemetryService(Tracer tracer, Meter meter) {
        this.tracer = tracer;
        this.meter = meter;
        
        // Initialize counters
        this.requestCounter = meter
            .counterBuilder("http_requests_total")
            .setDescription("Total number of HTTP requests")
            .build();
            
        this.errorCounter = meter
            .counterBuilder("errors_total")
            .setDescription("Total number of errors")
            .build();
            
        this.databaseQueryCounter = meter
            .counterBuilder("database_queries_total")
            .setDescription("Total number of database queries")
            .build();
    }
    
    public <T> T traceOperation(String operationName, String userId, TracedOperation<T> operation) {
        Span span = tracer.spanBuilder(operationName)
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute(OPERATION_NAME, operationName)
            .setAttribute(USER_ID, userId)
            .startSpan();
        
        long startTime = System.currentTimeMillis();
        
        try (Scope scope = span.makeCurrent()) {
            requestCounter.add(1, Attributes.of(OPERATION_NAME, operationName));
            
            T result = operation.execute();
            
            span.setStatus(io.opentelemetry.api.trace.StatusCode.OK);
            return result;
            
        } catch (Exception e) {
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            
            errorCounter.add(1, Attributes.of(
                OPERATION_NAME, operationName,
                ERROR_TYPE, e.getClass().getSimpleName()
            ));
            
            throw new RuntimeException("Operation failed: " + operationName, e);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            span.setAttribute(DURATION_MS, duration);
            span.end();
        }
    }
    
    public void recordDatabaseQuery(String query, long durationMs) {
        databaseQueryCounter.add(1);
        
        Span span = tracer.spanBuilder("database.query")
            .setSpanKind(SpanKind.CLIENT)
            .setAttribute("db.statement", query)
            .setAttribute("db.operation", extractOperation(query))
            .setAttribute(DURATION_MS, durationMs)
            .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            // Simulate database operation
            Thread.sleep(durationMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            span.recordException(e);
        } finally {
            span.end();
        }
    }
    
    private String extractOperation(String query) {
        String upperQuery = query.trim().toUpperCase();
        if (upperQuery.startsWith("SELECT")) return "SELECT";
        if (upperQuery.startsWith("INSERT")) return "INSERT";
        if (upperQuery.startsWith("UPDATE")) return "UPDATE";
        if (upperQuery.startsWith("DELETE")) return "DELETE";
        return "UNKNOWN";
    }
    
    @FunctionalInterface
    public interface TracedOperation<T> {
        T execute() throws Exception;
    }
}
```

### Distributed Tracing Implementation

```java
package com.javajourney.observability.tracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class DistributedTracingService {
    
    private final Tracer tracer;
    private final RestTemplate restTemplate;
    private final ExecutorService executorService;
    
    // Text map propagators for HTTP headers
    private static final TextMapSetter<HttpHeaders> HTTP_SETTER = HttpHeaders::set;
    private static final TextMapGetter<HttpHeaders> HTTP_GETTER = new TextMapGetter<HttpHeaders>() {
        @Override
        public Iterable<String> keys(HttpHeaders headers) {
            return headers.keySet();
        }
        
        @Override
        public String get(HttpHeaders headers, String key) {
            return headers.getFirst(key);
        }
    };
    
    public DistributedTracingService(Tracer tracer) {
        this.tracer = tracer;
        this.restTemplate = new RestTemplate();
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }
    
    // Service A calls Service B with trace propagation
    public OrderProcessingResult processOrder(OrderRequest request) {
        Span span = tracer.spanBuilder("order.process")
            .setSpanKind(SpanKind.SERVER)
            .setAttribute("order.id", request.orderId())
            .setAttribute("customer.id", request.customerId())
            .setAttribute("item.count", request.items().size())
            .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            
            // Step 1: Validate order
            boolean isValid = validateOrder(request);
            span.setAttribute("order.valid", isValid);
            
            if (!isValid) {
                span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, "Invalid order");
                return new OrderProcessingResult(request.orderId(), "FAILED", "Order validation failed");
            }
            
            // Step 2: Check inventory (external service call)
            boolean inventoryAvailable = checkInventory(request);
            span.setAttribute("inventory.available", inventoryAvailable);
            
            if (!inventoryAvailable) {
                return new OrderProcessingResult(request.orderId(), "FAILED", "Insufficient inventory");
            }
            
            // Step 3: Process payment (external service call)
            PaymentResult paymentResult = processPayment(request);
            span.setAttribute("payment.status", paymentResult.status());
            span.setAttribute("payment.transaction.id", paymentResult.transactionId());
            
            if (!"SUCCESS".equals(paymentResult.status())) {
                return new OrderProcessingResult(request.orderId(), "FAILED", "Payment failed");
            }
            
            // Step 4: Create order asynchronously
            CompletableFuture<String> orderCreation = createOrderAsync(request, paymentResult.transactionId());
            
            // Step 5: Send confirmation (fire and forget)
            sendOrderConfirmation(request.customerId(), request.orderId());
            
            String orderId = orderCreation.join();
            span.setAttribute("created.order.id", orderId);
            
            return new OrderProcessingResult(orderId, "SUCCESS", "Order processed successfully");
            
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
    
    // Child span for order validation
    private boolean validateOrder(OrderRequest request) {
        Span span = tracer.spanBuilder("order.validate")
            .setSpanKind(SpanKind.INTERNAL)
            .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            // Simulate validation logic
            boolean hasItems = !request.items().isEmpty();
            boolean hasCustomer = request.customerId() != null && !request.customerId().isBlank();
            boolean validTotal = request.totalAmount() > 0;
            
            span.setAttribute("validation.has_items", hasItems);
            span.setAttribute("validation.has_customer", hasCustomer);
            span.setAttribute("validation.valid_total", validTotal);
            
            boolean isValid = hasItems && hasCustomer && validTotal;
            span.setAttribute("validation.result", isValid);
            
            return isValid;
        } finally {
            span.end();
        }
    }
    
    // External service call with trace propagation
    private boolean checkInventory(OrderRequest request) {
        Span span = tracer.spanBuilder("inventory.check")
            .setSpanKind(SpanKind.CLIENT)
            .setAttribute("service.name", "inventory-service")
            .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            // Create headers with trace context
            HttpHeaders headers = new HttpHeaders();
            
            // Inject trace context into headers
            io.opentelemetry.api.GlobalOpenTelemetry.getPropagators()
                .getTextMapPropagator()
                .inject(Context.current(), headers, HTTP_SETTER);
            
            // Simulate external service call
            String inventoryServiceUrl = "http://inventory-service/check";
            
            // In real implementation, you would make HTTP call with headers
            simulateServiceCall("inventory-service", 150);
            
            // Simulate inventory check result
            boolean available = ThreadLocalRandom.current().nextBoolean();
            span.setAttribute("inventory.result", available);
            
            return available;
            
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }
    
    // Payment processing with external service
    private PaymentResult processPayment(OrderRequest request) {
        Span span = tracer.spanBuilder("payment.process")
            .setSpanKind(SpanKind.CLIENT)
            .setAttribute("service.name", "payment-service")
            .setAttribute("payment.amount", request.totalAmount())
            .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            // Create headers with trace context
            HttpHeaders headers = new HttpHeaders();
            io.opentelemetry.api.GlobalOpenTelemetry.getPropagators()
                .getTextMapPropagator()
                .inject(Context.current(), headers, HTTP_SETTER);
            
            // Simulate payment processing
            simulateServiceCall("payment-service", 300);
            
            // Generate payment result
            String transactionId = "txn_" + System.currentTimeMillis();
            String status = ThreadLocalRandom.current().nextDouble() > 0.1 ? "SUCCESS" : "FAILED";
            
            span.setAttribute("payment.transaction.id", transactionId);
            span.setAttribute("payment.status", status);
            
            if ("FAILED".equals(status)) {
                span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, "Payment processing failed");
            }
            
            return new PaymentResult(transactionId, status, request.totalAmount());
            
        } finally {
            span.end();
        }
    }
    
    // Async order creation with proper context propagation
    private CompletableFuture<String> createOrderAsync(OrderRequest request, String transactionId) {
        Context currentContext = Context.current();
        
        return CompletableFuture.supplyAsync(() -> {
            try (Scope scope = currentContext.makeCurrent()) {
                Span span = tracer.spanBuilder("order.create")
                    .setSpanKind(SpanKind.INTERNAL)
                    .setAttribute("payment.transaction.id", transactionId)
                    .startSpan();
                
                try (Scope spanScope = span.makeCurrent()) {
                    // Simulate order creation
                    simulateServiceCall("database", 200);
                    
                    String orderId = "order_" + System.currentTimeMillis();
                    span.setAttribute("created.order.id", orderId);
                    
                    return orderId;
                } finally {
                    span.end();
                }
            }
        }, executorService);
    }
    
    // Fire-and-forget notification
    private void sendOrderConfirmation(String customerId, String orderId) {
        Context currentContext = Context.current();
        
        executorService.submit(() -> {
            try (Scope scope = currentContext.makeCurrent()) {
                Span span = tracer.spanBuilder("notification.send")
                    .setSpanKind(SpanKind.PRODUCER)
                    .setAttribute("customer.id", customerId)
                    .setAttribute("order.id", orderId)
                    .startSpan();
                
                try (Scope spanScope = span.makeCurrent()) {
                    // Simulate notification sending
                    simulateServiceCall("notification-service", 100);
                    
                    span.setAttribute("notification.sent", true);
                } finally {
                    span.end();
                }
            }
        });
    }
    
    private void simulateServiceCall(String serviceName, long durationMs) {
        try {
            Thread.sleep(durationMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Service call interrupted: " + serviceName, e);
        }
    }
    
    // Data records
    public record OrderRequest(
        String orderId,
        String customerId,
        java.util.List<OrderItem> items,
        double totalAmount
    ) {}
    
    public record OrderItem(String productId, int quantity, double price) {}
    
    public record PaymentResult(String transactionId, String status, double amount) {}
    
    public record OrderProcessingResult(String orderId, String status, String message) {}
}
```

## 2. Comprehensive Metrics Collection

### Application Metrics

```java
package com.javajourney.observability.metrics;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.*;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ApplicationMetrics {
    
    private final Meter meter;
    
    // Counters
    private LongCounter httpRequestsTotal;
    private LongCounter httpRequestsErrors;
    private LongCounter databaseConnectionsTotal;
    private LongCounter cacheHitsTotal;
    private LongCounter cacheMissesTotal;
    
    // Gauges
    private ObservableLongGauge activeConnections;
    private ObservableLongGauge memoryUsed;
    private ObservableLongGauge cpuUsage;
    
    // Histograms
    private LongHistogram httpRequestDuration;
    private LongHistogram databaseQueryDuration;
    private LongHistogram cacheOperationDuration;
    
    // State tracking
    private final AtomicLong activeConnectionsCount = new AtomicLong(0);
    private final ConcurrentHashMap<String, RequestMetrics> activeRequests = new ConcurrentHashMap<>();
    
    // Attribute keys
    private static final AttributeKey<String> HTTP_METHOD = AttributeKey.stringKey("http.method");
    private static final AttributeKey<String> HTTP_STATUS = AttributeKey.stringKey("http.status");
    private static final AttributeKey<String> HTTP_ROUTE = AttributeKey.stringKey("http.route");
    private static final AttributeKey<String> DB_OPERATION = AttributeKey.stringKey("db.operation");
    private static final AttributeKey<String> CACHE_TYPE = AttributeKey.stringKey("cache.type");
    
    public ApplicationMetrics(Meter meter) {
        this.meter = meter;
    }
    
    @PostConstruct
    public void initializeMetrics() {
        // Initialize counters
        httpRequestsTotal = meter
            .counterBuilder("http_requests_total")
            .setDescription("Total number of HTTP requests")
            .build();
            
        httpRequestsErrors = meter
            .counterBuilder("http_requests_errors_total")
            .setDescription("Total number of HTTP request errors")
            .build();
            
        databaseConnectionsTotal = meter
            .counterBuilder("database_connections_total")
            .setDescription("Total number of database connections created")
            .build();
            
        cacheHitsTotal = meter
            .counterBuilder("cache_hits_total")
            .setDescription("Total number of cache hits")
            .build();
            
        cacheMissesTotal = meter
            .counterBuilder("cache_misses_total")
            .setDescription("Total number of cache misses")
            .build();
        
        // Initialize gauges
        activeConnections = meter
            .gaugeBuilder("database_connections_active")
            .setDescription("Number of active database connections")
            .ofLongs()
            .buildWithCallback(measurement -> 
                measurement.record(activeConnectionsCount.get()));
                
        memoryUsed = meter
            .gaugeBuilder("jvm_memory_used_bytes")
            .setDescription("JVM memory usage in bytes")
            .ofLongs()
            .buildWithCallback(measurement -> {
                Runtime runtime = Runtime.getRuntime();
                long used = runtime.totalMemory() - runtime.freeMemory();
                measurement.record(used);
            });
            
        cpuUsage = meter
            .gaugeBuilder("system_cpu_usage_percent")
            .setDescription("System CPU usage percentage")
            .ofLongs()
            .buildWithCallback(measurement -> {
                // Simplified CPU usage calculation
                double cpuUsage = getSystemCpuUsage();
                measurement.record((long) (cpuUsage * 100));
            });
        
        // Initialize histograms
        httpRequestDuration = meter
            .histogramBuilder("http_request_duration_ms")
            .setDescription("HTTP request duration in milliseconds")
            .ofLongs()
            .build();
            
        databaseQueryDuration = meter
            .histogramBuilder("database_query_duration_ms")
            .setDescription("Database query duration in milliseconds")
            .ofLongs()
            .build();
            
        cacheOperationDuration = meter
            .histogramBuilder("cache_operation_duration_ms")
            .setDescription("Cache operation duration in milliseconds")
            .ofLongs()
            .build();
    }
    
    // HTTP request metrics
    public void recordHttpRequest(String method, String route, int statusCode, long durationMs) {
        Attributes attributes = Attributes.of(
            HTTP_METHOD, method,
            HTTP_ROUTE, route,
            HTTP_STATUS, String.valueOf(statusCode)
        );
        
        httpRequestsTotal.add(1, attributes);
        httpRequestDuration.record(durationMs, attributes);
        
        if (statusCode >= 400) {
            httpRequestsErrors.add(1, attributes);
        }
    }
    
    // Database metrics
    public void recordDatabaseConnection() {
        activeConnectionsCount.incrementAndGet();
        databaseConnectionsTotal.add(1);
    }
    
    public void releaseDatabaseConnection() {
        activeConnectionsCount.decrementAndGet();
    }
    
    public void recordDatabaseQuery(String operation, long durationMs) {
        Attributes attributes = Attributes.of(DB_OPERATION, operation);
        databaseQueryDuration.record(durationMs, attributes);
    }
    
    // Cache metrics
    public void recordCacheHit(String cacheType, long durationMs) {
        Attributes attributes = Attributes.of(CACHE_TYPE, cacheType);
        cacheHitsTotal.add(1, attributes);
        cacheOperationDuration.record(durationMs, attributes);
    }
    
    public void recordCacheMiss(String cacheType, long durationMs) {
        Attributes attributes = Attributes.of(CACHE_TYPE, cacheType);
        cacheMissesTotal.add(1, attributes);
        cacheOperationDuration.record(durationMs, attributes);
    }
    
    // Request tracking
    public String startRequest(String method, String route) {
        String requestId = "req_" + System.nanoTime();
        RequestMetrics metrics = new RequestMetrics(method, route, Instant.now());
        activeRequests.put(requestId, metrics);
        return requestId;
    }
    
    public void endRequest(String requestId, int statusCode) {
        RequestMetrics metrics = activeRequests.remove(requestId);
        if (metrics != null) {
            long durationMs = Duration.between(metrics.startTime(), Instant.now()).toMillis();
            recordHttpRequest(metrics.method(), metrics.route(), statusCode, durationMs);
        }
    }
    
    // Business metrics
    public void recordBusinessMetric(String metricName, long value, Attributes attributes) {
        LongCounter counter = meter
            .counterBuilder(metricName)
            .setDescription("Business metric: " + metricName)
            .build();
        counter.add(value, attributes);
    }
    
    public void recordBusinessHistogram(String metricName, long value, Attributes attributes) {
        LongHistogram histogram = meter
            .histogramBuilder(metricName)
            .setDescription("Business histogram: " + metricName)
            .ofLongs()
            .build();
        histogram.record(value, attributes);
    }
    
    // Utility methods
    private double getSystemCpuUsage() {
        // Simplified CPU usage - in production, use proper system monitoring
        return Math.random() * 100; // Mock implementation
    }
    
    public MetricsSummary getCurrentMetrics() {
        Runtime runtime = Runtime.getRuntime();
        return new MetricsSummary(
            activeConnectionsCount.get(),
            runtime.totalMemory() - runtime.freeMemory(),
            runtime.maxMemory(),
            activeRequests.size(),
            getSystemCpuUsage()
        );
    }
    
    // Data records
    private record RequestMetrics(String method, String route, Instant startTime) {}
    
    public record MetricsSummary(
        long activeConnections,
        long memoryUsed,
        long memoryMax,
        int activeRequests,
        double cpuUsage
    ) {}
}
```

### Custom Metrics Dashboard

```java
package com.javajourney.observability.dashboard;

import com.javajourney.observability.metrics.ApplicationMetrics;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/metrics")
public class MetricsDashboardController {
    
    private final ApplicationMetrics applicationMetrics;
    
    public MetricsDashboardController(ApplicationMetrics applicationMetrics) {
        this.applicationMetrics = applicationMetrics;
    }
    
    @GetMapping("/dashboard")
    public Map<String, Object> getDashboard() {
        var metrics = applicationMetrics.getCurrentMetrics();
        
        return Map.of(
            "timestamp", Instant.now(),
            "system", Map.of(
                "activeConnections", metrics.activeConnections(),
                "memoryUsed", formatBytes(metrics.memoryUsed()),
                "memoryMax", formatBytes(metrics.memoryMax()),
                "memoryUtilization", String.format("%.1f%%", 
                    (metrics.memoryUsed() * 100.0) / metrics.memoryMax()),
                "cpuUsage", String.format("%.1f%%", metrics.cpuUsage()),
                "activeRequests", metrics.activeRequests()
            ),
            "health", getHealthStatus(),
            "uptime", getUptime()
        );
    }
    
    @GetMapping("/simulate-load")
    public Map<String, Object> simulateLoad() {
        // Simulate various operations to generate metrics
        
        // HTTP requests
        for (int i = 0; i < 10; i++) {
            String requestId = applicationMetrics.startRequest("GET", "/api/users");
            
            // Simulate processing time
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(50, 200));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            int statusCode = ThreadLocalRandom.current().nextDouble() > 0.1 ? 200 : 500;
            applicationMetrics.endRequest(requestId, statusCode);
        }
        
        // Database operations
        for (int i = 0; i < 5; i++) {
            applicationMetrics.recordDatabaseConnection();
            
            long queryDuration = ThreadLocalRandom.current().nextLong(10, 100);
            applicationMetrics.recordDatabaseQuery("SELECT", queryDuration);
            
            applicationMetrics.releaseDatabaseConnection();
        }
        
        // Cache operations
        for (int i = 0; i < 20; i++) {
            long cacheDuration = ThreadLocalRandom.current().nextLong(1, 10);
            if (ThreadLocalRandom.current().nextDouble() > 0.2) {
                applicationMetrics.recordCacheHit("redis", cacheDuration);
            } else {
                applicationMetrics.recordCacheMiss("redis", cacheDuration);
            }
        }
        
        return Map.of(
            "message", "Load simulation completed",
            "timestamp", Instant.now(),
            "operations", Map.of(
                "httpRequests", 10,
                "databaseQueries", 5,
                "cacheOperations", 20
            )
        );
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    private String getHealthStatus() {
        var metrics = applicationMetrics.getCurrentMetrics();
        
        if (metrics.cpuUsage() > 90 || 
            (metrics.memoryUsed() * 100.0) / metrics.memoryMax() > 90) {
            return "CRITICAL";
        } else if (metrics.cpuUsage() > 70 || 
                   (metrics.memoryUsed() * 100.0) / metrics.memoryMax() > 70) {
            return "WARNING";
        } else {
            return "HEALTHY";
        }
    }
    
    private String getUptime() {
        long uptimeMs = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
```

## 3. Log Correlation and Structured Logging

### Structured Logging Configuration

```java
package com.javajourney.observability.logging;

import io.opentelemetry.api.trace.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class StructuredLogger {
    
    private static final Logger logger = LoggerFactory.getLogger(StructuredLogger.class);
    
    // MDC keys for structured logging
    private static final String TRACE_ID = "traceId";
    private static final String SPAN_ID = "spanId";
    private static final String USER_ID = "userId";
    private static final String REQUEST_ID = "requestId";
    private static final String OPERATION = "operation";
    private static final String COMPONENT = "component";
    
    public void logInfo(String message, Map<String, String> context) {
        setMDCContext(context);
        try {
            logger.info("message={}", message);
        } finally {
            clearMDC();
        }
    }
    
    public void logError(String message, Throwable throwable, Map<String, String> context) {
        setMDCContext(context);
        try {
            logger.error("message={} error={}", message, throwable.getMessage(), throwable);
        } finally {
            clearMDC();
        }
    }
    
    public void logWarn(String message, Map<String, String> context) {
        setMDCContext(context);
        try {
            logger.warn("message={}", message);
        } finally {
            clearMDC();
        }
    }
    
    public void logDebug(String message, Map<String, String> context) {
        setMDCContext(context);
        try {
            logger.debug("message={}", message);
        } finally {
            clearMDC();
        }
    }
    
    // Business event logging
    public void logBusinessEvent(BusinessEvent event) {
        Map<String, String> context = Map.of(
            USER_ID, event.userId(),
            OPERATION, event.eventType(),
            COMPONENT, event.component(),
            "eventId", event.eventId(),
            "severity", event.severity().name()
        );
        
        setMDCContext(context);
        try {
            switch (event.severity()) {
                case INFO -> logger.info("business_event={} details={}", 
                    event.eventType(), event.details());
                case WARN -> logger.warn("business_event={} details={}", 
                    event.eventType(), event.details());
                case ERROR -> logger.error("business_event={} details={}", 
                    event.eventType(), event.details());
                case DEBUG -> logger.debug("business_event={} details={}", 
                    event.eventType(), event.details());
            }
        } finally {
            clearMDC();
        }
    }
    
    // Automatic trace correlation
    public void logWithTrace(String message, LogLevel level) {
        Span currentSpan = Span.current();
        
        Map<String, String> context = Map.of(
            TRACE_ID, currentSpan.getSpanContext().getTraceId(),
            SPAN_ID, currentSpan.getSpanContext().getSpanId()
        );
        
        switch (level) {
            case INFO -> logInfo(message, context);
            case WARN -> logWarn(message, context);
            case ERROR -> logError(message, new RuntimeException(message), context);
            case DEBUG -> logDebug(message, context);
        }
    }
    
    // Performance logging
    public void logPerformance(String operation, long durationMs, Map<String, String> context) {
        Map<String, String> perfContext = new java.util.HashMap<>(context);
        perfContext.put(OPERATION, operation);
        perfContext.put("duration_ms", String.valueOf(durationMs));
        perfContext.put("performance", "true");
        
        setMDCContext(perfContext);
        try {
            if (durationMs > 1000) {
                logger.warn("slow_operation={} duration={}ms", operation, durationMs);
            } else {
                logger.info("operation_completed={} duration={}ms", operation, durationMs);
            }
        } finally {
            clearMDC();
        }
    }
    
    // Security event logging
    public void logSecurityEvent(SecurityEvent event) {
        Map<String, String> context = Map.of(
            USER_ID, event.userId(),
            "security_event", event.eventType(),
            "severity", event.severity().name(),
            "source_ip", event.sourceIp(),
            "user_agent", event.userAgent()
        );
        
        setMDCContext(context);
        try {
            logger.warn("security_event={} user={} ip={} details={}", 
                event.eventType(), event.userId(), event.sourceIp(), event.details());
        } finally {
            clearMDC();
        }
    }
    
    private void setMDCContext(Map<String, String> context) {
        // Add current trace information if available
        Span currentSpan = Span.current();
        if (currentSpan.getSpanContext().isValid()) {
            MDC.put(TRACE_ID, currentSpan.getSpanContext().getTraceId());
            MDC.put(SPAN_ID, currentSpan.getSpanContext().getSpanId());
        }
        
        // Add custom context
        context.forEach(MDC::put);
    }
    
    private void clearMDC() {
        MDC.clear();
    }
    
    // Utility method for request correlation
    public String generateRequestId() {
        return UUID.randomUUID().toString();
    }
    
    // Data classes
    public record BusinessEvent(
        String eventId,
        String eventType,
        String userId,
        String component,
        String details,
        LogLevel severity
    ) {}
    
    public record SecurityEvent(
        String eventType,
        String userId,
        String sourceIp,
        String userAgent,
        String details,
        LogLevel severity
    ) {}
    
    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
}
```

### Logback Configuration

```xml
<!-- logback-spring.xml -->
<configuration>
    <springProfile name="!local">
        <!-- JSON logging for production -->
        <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp>
                        <timeZone>UTC</timeZone>
                    </timestamp>
                    <version/>
                    <logLevel/>
                    <loggerName/>
                    <mdc/>
                    <arguments/>
                    <message/>
                    <stackTrace/>
                </providers>
            </encoder>
        </appender>
    </springProfile>
    
    <springProfile name="local">
        <!-- Human-readable logging for local development -->
        <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level [%X{traceId:-},%X{spanId:-}] %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
    </springProfile>
    
    <!-- File appender for persistent logs -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/application.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/application.%d{yyyy-MM-dd}.%i.gz</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>3GB</totalSizeCap>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp>
                    <timeZone>UTC</timeZone>
                </timestamp>
                <version/>
                <logLevel/>
                <loggerName/>
                <mdc/>
                <arguments/>
                <message/>
                <stackTrace/>
            </providers>
        </encoder>
    </appender>
    
    <!-- Async appender for better performance -->
    <appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="FILE"/>
        <queueSize>1024</queueSize>
        <discardingThreshold>0</discardingThreshold>
        <includeCallerData>true</includeCallerData>
    </appender>
    
    <!-- Root logger -->
    <root level="INFO">
        <appender-ref ref="STDOUT"/>
        <appender-ref ref="ASYNC"/>
    </root>
    
    <!-- Application-specific loggers -->
    <logger name="com.javajourney.observability" level="DEBUG"/>
    <logger name="org.springframework.web" level="INFO"/>
    <logger name="org.springframework.security" level="DEBUG"/>
    <logger name="io.opentelemetry" level="INFO"/>
    
    <!-- Reduce noise from common libraries -->
    <logger name="org.apache.http" level="WARN"/>
    <logger name="org.springframework.boot.autoconfigure" level="WARN"/>
</configuration>
```

## 4. Alerting and Monitoring

### Alert Configuration

```java
package com.javajourney.observability.alerting;

import com.javajourney.observability.metrics.ApplicationMetrics;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AlertingService {
    
    private final ApplicationMetrics applicationMetrics;
    private final List<AlertListener> alertListeners = new CopyOnWriteArrayList<>();
    private final AtomicInteger alertCounter = new AtomicInteger(0);
    
    // Alert thresholds
    private static final double HIGH_CPU_THRESHOLD = 80.0;
    private static final double HIGH_MEMORY_THRESHOLD = 85.0;
    private static final long HIGH_RESPONSE_TIME_THRESHOLD = 2000; // ms
    private static final int MAX_ACTIVE_CONNECTIONS = 100;
    
    public AlertingService(ApplicationMetrics applicationMetrics) {
        this.applicationMetrics = applicationMetrics;
        
        // Register default alert listeners
        registerAlertListener(new LoggingAlertListener());
        registerAlertListener(new MetricsAlertListener());
    }
    
    public void registerAlertListener(AlertListener listener) {
        alertListeners.add(listener);
    }
    
    @Scheduled(fixedRate = 30000) // Check every 30 seconds
    public void checkAlerts() {
        var metrics = applicationMetrics.getCurrentMetrics();
        
        // CPU usage alert
        if (metrics.cpuUsage() > HIGH_CPU_THRESHOLD) {
            fireAlert(new Alert(
                "HIGH_CPU_USAGE",
                AlertSeverity.WARNING,
                String.format("CPU usage is %.1f%%, threshold is %.1f%%", 
                    metrics.cpuUsage(), HIGH_CPU_THRESHOLD),
                Instant.now(),
                Map.of("cpu_usage", String.valueOf(metrics.cpuUsage()))
            ));
        }
        
        // Memory usage alert
        double memoryUsagePercent = (metrics.memoryUsed() * 100.0) / metrics.memoryMax();
        if (memoryUsagePercent > HIGH_MEMORY_THRESHOLD) {
            fireAlert(new Alert(
                "HIGH_MEMORY_USAGE",
                AlertSeverity.CRITICAL,
                String.format("Memory usage is %.1f%%, threshold is %.1f%%", 
                    memoryUsagePercent, HIGH_MEMORY_THRESHOLD),
                Instant.now(),
                Map.of(
                    "memory_usage_percent", String.valueOf(memoryUsagePercent),
                    "memory_used", String.valueOf(metrics.memoryUsed()),
                    "memory_max", String.valueOf(metrics.memoryMax())
                )
            ));
        }
        
        // Active connections alert
        if (metrics.activeConnections() > MAX_ACTIVE_CONNECTIONS) {
            fireAlert(new Alert(
                "HIGH_ACTIVE_CONNECTIONS",
                AlertSeverity.WARNING,
                String.format("Active connections: %d, threshold: %d", 
                    metrics.activeConnections(), MAX_ACTIVE_CONNECTIONS),
                Instant.now(),
                Map.of("active_connections", String.valueOf(metrics.activeConnections()))
            ));
        }
    }
    
    public void checkResponseTime(long responseTimeMs, String endpoint) {
        if (responseTimeMs > HIGH_RESPONSE_TIME_THRESHOLD) {
            fireAlert(new Alert(
                "SLOW_RESPONSE_TIME",
                AlertSeverity.WARNING,
                String.format("Slow response time: %dms for endpoint %s, threshold: %dms", 
                    responseTimeMs, endpoint, HIGH_RESPONSE_TIME_THRESHOLD),
                Instant.now(),
                Map.of(
                    "response_time_ms", String.valueOf(responseTimeMs),
                    "endpoint", endpoint
                )
            ));
        }
    }
    
    public void fireBusinessAlert(String alertType, AlertSeverity severity, String message, 
                                 Map<String, String> context) {
        fireAlert(new Alert(alertType, severity, message, Instant.now(), context));
    }
    
    private void fireAlert(Alert alert) {
        int alertId = alertCounter.incrementAndGet();
        Alert alertWithId = new Alert(
            alert.type(),
            alert.severity(),
            alert.message(),
            alert.timestamp(),
            Map.of("alert_id", String.valueOf(alertId))
        );
        
        alertListeners.forEach(listener -> {
            try {
                listener.onAlert(alertWithId);
            } catch (Exception e) {
                System.err.println("Error notifying alert listener: " + e.getMessage());
            }
        });
    }
    
    // Data classes
    public record Alert(
        String type,
        AlertSeverity severity,
        String message,
        Instant timestamp,
        Map<String, String> context
    ) {}
    
    public enum AlertSeverity {
        INFO, WARNING, CRITICAL
    }
    
    // Alert listener interface
    public interface AlertListener {
        void onAlert(Alert alert);
    }
    
    // Default alert listeners
    private static class LoggingAlertListener implements AlertListener {
        private static final org.slf4j.Logger logger = 
            org.slf4j.LoggerFactory.getLogger(LoggingAlertListener.class);
        
        @Override
        public void onAlert(Alert alert) {
            switch (alert.severity()) {
                case INFO -> logger.info("ALERT: {} - {}", alert.type(), alert.message());
                case WARNING -> logger.warn("ALERT: {} - {}", alert.type(), alert.message());
                case CRITICAL -> logger.error("ALERT: {} - {}", alert.type(), alert.message());
            }
        }
    }
    
    private class MetricsAlertListener implements AlertListener {
        @Override
        public void onAlert(Alert alert) {
            // Record alert as a metric
            applicationMetrics.recordBusinessMetric(
                "alerts_total",
                1,
                io.opentelemetry.api.common.Attributes.of(
                    io.opentelemetry.api.common.AttributeKey.stringKey("alert_type"), alert.type(),
                    io.opentelemetry.api.common.AttributeKey.stringKey("severity"), alert.severity().name()
                )
            );
        }
    }
}
```

## Practice Exercises

### Exercise 1: Complete Observability Stack

Implement a full observability stack with OpenTelemetry, including traces, metrics, and logs for a microservices application.

### Exercise 2: Custom Metrics Dashboard

Build a real-time metrics dashboard that displays application performance, business metrics, and system health.

### Exercise 3: Distributed Tracing Analysis

Implement complex distributed tracing scenarios and analyze performance bottlenecks across service boundaries.

### Exercise 4: Alerting System

Create a comprehensive alerting system with multiple notification channels and intelligent alert correlation.

## Key Takeaways

1. **Unified Observability**: OpenTelemetry provides a standardized approach to collecting telemetry data
2. **Distributed Tracing**: Essential for understanding request flows in microservices architectures
3. **Comprehensive Metrics**: Collect both technical and business metrics for complete visibility
4. **Structured Logging**: JSON-formatted logs with trace correlation enable powerful analysis
5. **Proactive Alerting**: Automated monitoring and alerting prevent issues from impacting users

Advanced observability is crucial for maintaining reliable, performant applications in production environments, providing the insights needed for effective troubleshooting and optimization.