# Day 89: Reactive Programming - Project Reactor & WebFlux Advanced

## Overview
Today we dive deep into reactive programming with Project Reactor and Spring WebFlux, building high-performance, non-blocking applications. We'll explore advanced reactive patterns, backpressure handling, reactive database integration, error handling strategies, and performance optimization techniques that leverage the database patterns from yesterday.

## Learning Objectives
- Master advanced Project Reactor operators and reactive streams
- Build scalable WebFlux applications with non-blocking I/O
- Implement reactive database access with R2DBC
- Handle backpressure and flow control in reactive systems
- Apply advanced error handling and retry strategies
- Optimize reactive applications for high throughput and low latency

## 1. Advanced Project Reactor Patterns

### Complex Reactive Streams Composition
```java
package com.javajourney.reactive;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Advanced Project Reactor patterns and stream composition
 */
public class AdvancedReactivePatterns {
    
    /**
     * Complex stream transformation with multiple operators
     */
    public static class StreamComposition {
        
        /**
         * Process order events with complex business logic
         */
        public Flux<ProcessedOrder> processOrderEvents(Flux<OrderEvent> orderEvents) {
            return orderEvents
                // Filter valid orders
                .filter(this::isValidOrder)
                
                // Group by customer ID for batch processing
                .groupBy(OrderEvent::getCustomerId)
                
                // Process each customer's orders
                .flatMap(customerOrders -> 
                    customerOrders
                        // Collect orders in time windows
                        .window(Duration.ofSeconds(5))
                        .flatMap(window -> 
                            window
                                .collectList()
                                .flatMap(this::processBatchedOrders)
                        )
                        // Handle individual customer processing errors
                        .onErrorResume(error -> {
                            String customerId = customerOrders.key();
                            return handleCustomerProcessingError(customerId, error);
                        })
                )
                
                // Apply backpressure handling
                .onBackpressureBuffer(1000, BufferOverflowStrategy.DROP_OLDEST)
                
                // Parallel processing for CPU-intensive operations
                .parallel(Runtime.getRuntime().availableProcessors())
                .runOn(Schedulers.parallel())
                
                // Apply business rules validation
                .map(this::applyBusinessRules)
                .filter(Objects::nonNull)
                
                // Sequential processing for database operations
                .sequential()
                .publishOn(Schedulers.boundedElastic())
                
                // Retry with exponential backoff
                .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                    .maxBackoff(Duration.ofSeconds(2))
                    .filter(throwable -> throwable instanceof TransientException))
                
                // Cache results for a short time
                .cache(Duration.ofMinutes(1));
        }
        
        /**
         * Reactive stream merging with different strategies
         */
        public Flux<CombinedData> mergeMultipleStreams() {
            Flux<UserData> userStream = fetchUserData();
            Flux<OrderData> orderStream = fetchOrderData();
            Flux<InventoryData> inventoryStream = fetchInventoryData();
            
            // Strategy 1: Zip streams together (wait for all)
            Flux<CombinedData> zippedData = Flux.zip(
                userStream,
                orderStream,
                inventoryStream,
                CombinedData::new
            );
            
            // Strategy 2: Combine latest values (reactive to any change)
            Flux<CombinedData> combinedLatest = Flux.combineLatest(
                userStream.distinctUntilChanged(),
                orderStream.distinctUntilChanged(),
                inventoryStream.distinctUntilChanged(),
                CombinedData::new
            );
            
            // Strategy 3: Merge streams with timestamps
            Flux<CombinedData> mergedWithTimestamp = Flux.merge(
                userStream.map(data -> new CombinedData(data, null, null)),
                orderStream.map(data -> new CombinedData(null, data, null)),
                inventoryStream.map(data -> new CombinedData(null, null, data))
            )
            .scan(new CombinedData(null, null, null), this::accumulate)
            .filter(this::isComplete);
            
            return zippedData; // Choose strategy based on requirements
        }
        
        /**
         * Advanced error handling with recovery strategies
         */
        public Flux<ProcessResult> processWithAdvancedErrorHandling(Flux<InputData> input) {
            AtomicInteger retryCount = new AtomicInteger(0);
            
            return input
                .flatMap(data -> processData(data)
                    // Handle specific errors differently
                    .onErrorResume(ValidationException.class, error -> 
                        Mono.just(ProcessResult.validationError(data.getId(), error.getMessage())))
                    
                    .onErrorResume(NetworkException.class, error -> {
                        if (retryCount.incrementAndGet() < 3) {
                            return Mono.delay(Duration.ofMillis(1000))
                                .then(processData(data));
                        }
                        return Mono.just(ProcessResult.networkError(data.getId(), error.getMessage()));
                    })
                    
                    .onErrorResume(DatabaseException.class, error -> 
                        // Fallback to cache or alternative data source
                        fallbackDataSource.getData(data.getId())
                            .map(fallbackData -> ProcessResult.success(data.getId(), fallbackData))
                            .switchIfEmpty(Mono.just(ProcessResult.fallbackError(data.getId()))))
                    
                    // Generic error handling
                    .onErrorResume(error -> {
                        logError("Unexpected error processing data", data.getId(), error);
                        return Mono.just(ProcessResult.systemError(data.getId()));
                    })
                )
                
                // Global timeout for each item
                .timeout(Duration.ofSeconds(30))
                
                // Log processing metrics
                .doOnNext(result -> recordMetrics(result))
                
                // Handle downstream backpressure
                .onBackpressureLatest();
        }
        
        /**
         * Reactive batching with dynamic sizing
         */
        public Flux<BatchResult> dynamicBatching(Flux<DataItem> dataStream) {
            return dataStream
                .bufferTimeout(100, Duration.ofSeconds(5)) // Max 100 items or 5 seconds
                .filter(batch -> !batch.isEmpty())
                .flatMap(batch -> {
                    // Adjust batch size based on system load
                    int optimalBatchSize = calculateOptimalBatchSize();
                    
                    if (batch.size() > optimalBatchSize) {
                        // Split large batches
                        return Flux.fromIterable(batch)
                            .buffer(optimalBatchSize)
                            .flatMap(this::processBatch);
                    } else {
                        return processBatch(batch);
                    }
                })
                .publishOn(Schedulers.boundedElastic());
        }
        
        private boolean isValidOrder(OrderEvent order) {
            return order != null && order.getAmount() > 0 && order.getCustomerId() != null;
        }
        
        private Mono<ProcessedOrder> processBatchedOrders(List<OrderEvent> orders) {
            // Simulate batch processing logic
            return Mono.fromCallable(() -> {
                double totalAmount = orders.stream()
                    .mapToDouble(OrderEvent::getAmount)
                    .sum();
                return new ProcessedOrder(orders.get(0).getCustomerId(), totalAmount, orders.size());
            }).subscribeOn(Schedulers.boundedElastic());
        }
        
        private Mono<ProcessedOrder> handleCustomerProcessingError(String customerId, Throwable error) {
            return Mono.just(new ProcessedOrder(customerId, 0.0, 0, error.getMessage()));
        }
        
        private ProcessedOrder applyBusinessRules(ProcessedOrder order) {
            // Apply business validation rules
            if (order.getTotalAmount() > 10000) {
                return order.withFlag("HIGH_VALUE");
            }
            return order;
        }
        
        private int calculateOptimalBatchSize() {
            // Dynamic batch sizing based on system metrics
            return Math.max(10, Math.min(100, getCurrentSystemLoad()));
        }
        
        private int getCurrentSystemLoad() {
            // Simplified load calculation
            return 50;
        }
    }
    
    /**
     * Custom reactive operators
     */
    public static class CustomOperators {
        
        /**
         * Custom operator for rate limiting
         */
        public static <T> Flux<T> rateLimit(Flux<T> source, int requestsPerSecond) {
            Duration interval = Duration.ofMillis(1000 / requestsPerSecond);
            
            return source.zipWith(
                Flux.interval(interval),
                (item, tick) -> item
            );
        }
        
        /**
         * Custom operator for circuit breaker pattern
         */
        public static <T> Flux<T> circuitBreaker(Flux<T> source, 
                                               int failureThreshold, 
                                               Duration timeout) {
            return source.transform(new CircuitBreakerOperator<>(failureThreshold, timeout));
        }
        
        /**
         * Custom operator for metrics collection
         */
        public static <T> Flux<T> collectMetrics(Flux<T> source, String operationName) {
            return source
                .doOnSubscribe(s -> MetricsCollector.recordSubscription(operationName))
                .doOnNext(item -> MetricsCollector.recordItem(operationName))
                .doOnError(error -> MetricsCollector.recordError(operationName, error))
                .doOnComplete(() -> MetricsCollector.recordCompletion(operationName));
        }
    }
    
    // Data classes and supporting types
    public record OrderEvent(String customerId, double amount, String productId) {}
    public record ProcessedOrder(String customerId, double totalAmount, int orderCount, String status) {
        public ProcessedOrder(String customerId, double totalAmount, int orderCount) {
            this(customerId, totalAmount, orderCount, "SUCCESS");
        }
        
        public ProcessedOrder withFlag(String flag) {
            return new ProcessedOrder(customerId, totalAmount, orderCount, flag);
        }
    }
    
    public record CombinedData(UserData userData, OrderData orderData, InventoryData inventoryData) {}
    public record UserData(String userId, String name) {}
    public record OrderData(String orderId, double amount) {}
    public record InventoryData(String productId, int quantity) {}
    public record InputData(String id, String value) {}
    public record DataItem(String id, String data) {}
    public record BatchResult(List<String> processedIds, boolean success) {}
    
    public static class ProcessResult {
        private final String id;
        private final String status;
        private final String message;
        private final Object data;
        
        private ProcessResult(String id, String status, String message, Object data) {
            this.id = id;
            this.status = status;
            this.message = message;
            this.data = data;
        }
        
        public static ProcessResult success(String id, Object data) {
            return new ProcessResult(id, "SUCCESS", null, data);
        }
        
        public static ProcessResult validationError(String id, String message) {
            return new ProcessResult(id, "VALIDATION_ERROR", message, null);
        }
        
        public static ProcessResult networkError(String id, String message) {
            return new ProcessResult(id, "NETWORK_ERROR", message, null);
        }
        
        public static ProcessResult fallbackError(String id) {
            return new ProcessResult(id, "FALLBACK_ERROR", "No fallback data available", null);
        }
        
        public static ProcessResult systemError(String id) {
            return new ProcessResult(id, "SYSTEM_ERROR", "Unexpected system error", null);
        }
        
        // Getters
        public String getId() { return id; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public Object getData() { return data; }
    }
    
    // Exception classes
    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) { super(message); }
    }
    
    public static class NetworkException extends RuntimeException {
        public NetworkException(String message) { super(message); }
    }
    
    public static class DatabaseException extends RuntimeException {
        public DatabaseException(String message) { super(message); }
    }
    
    public static class TransientException extends RuntimeException {
        public TransientException(String message) { super(message); }
    }
}
```

### Backpressure Handling and Flow Control
```java
package com.javajourney.reactive;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Advanced backpressure handling and flow control strategies
 */
public class BackpressureManagement {
    
    /**
     * Producer with backpressure awareness
     */
    public static class BackpressureAwareProducer {
        private final AtomicLong pendingRequests = new AtomicLong(0);
        private final AtomicLong produced = new AtomicLong(0);
        
        public Flux<DataEvent> createBackpressureAwareStream() {
            return Flux.create(sink -> {
                // Register backpressure request handler
                sink.onRequest(requested -> {
                    long currentPending = pendingRequests.addAndGet(requested);
                    System.out.printf("Requested: %d, Total pending: %d%n", requested, currentPending);
                    
                    // Produce data based on demand
                    produceData(sink, requested);
                });
                
                // Handle cancellation
                sink.onCancel(() -> {
                    System.out.println("Stream cancelled, stopping production");
                    stopProduction();
                });
                
                // Handle disposal
                sink.onDispose(() -> {
                    System.out.println("Stream disposed, cleaning up resources");
                    cleanup();
                });
                
            }, FluxSink.OverflowStrategy.BUFFER);
        }
        
        private void produceData(FluxSink<DataEvent> sink, long requested) {
            // Simulate async data production with backpressure awareness
            Schedulers.boundedElastic().schedule(() -> {
                for (int i = 0; i < requested && !sink.isCancelled(); i++) {
                    long id = produced.incrementAndGet();
                    DataEvent event = new DataEvent(id, "Data-" + id, System.currentTimeMillis());
                    
                    sink.next(event);
                    pendingRequests.decrementAndGet();
                    
                    // Simulate processing time
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }
        
        private void stopProduction() {
            // Stop any ongoing production
        }
        
        private void cleanup() {
            // Clean up resources
        }
    }
    
    /**
     * Adaptive backpressure strategies based on system conditions
     */
    public static class AdaptiveBackpressureStrategy {
        
        public Flux<ProcessResult> adaptiveProcessing(Flux<InputData> input) {
            return input
                // Monitor system load and adapt strategy
                .transform(this::selectBackpressureStrategy)
                .flatMap(this::processWithLoadBalancing, getConcurrency())
                .publishOn(Schedulers.boundedElastic());
        }
        
        private Flux<InputData> selectBackpressureStrategy(Flux<InputData> source) {
            double systemLoad = getCurrentSystemLoad();
            
            if (systemLoad > 0.8) {
                // High load: drop oldest items
                return source.onBackpressureBuffer(100, BufferOverflowStrategy.DROP_OLDEST);
            } else if (systemLoad > 0.6) {
                // Medium load: buffer with limited size
                return source.onBackpressureBuffer(500);
            } else {
                // Low load: latest item only
                return source.onBackpressureLatest();
            }
        }
        
        private Mono<ProcessResult> processWithLoadBalancing(InputData data) {
            // Distribute processing based on current load
            if (getCurrentSystemLoad() > 0.7) {
                return processLightweight(data);
            } else {
                return processFullFeature(data);
            }
        }
        
        private int getConcurrency() {
            double systemLoad = getCurrentSystemLoad();
            int maxConcurrency = Runtime.getRuntime().availableProcessors() * 2;
            
            return (int) (maxConcurrency * (1.0 - systemLoad * 0.5));
        }
        
        private double getCurrentSystemLoad() {
            // Simplified system load calculation
            return Math.random(); // 0.0 to 1.0
        }
        
        private Mono<ProcessResult> processLightweight(InputData data) {
            return Mono.fromCallable(() -> new ProcessResult(data.id(), "LIGHTWEIGHT"))
                .subscribeOn(Schedulers.parallel());
        }
        
        private Mono<ProcessResult> processFullFeature(InputData data) {
            return Mono.fromCallable(() -> new ProcessResult(data.id(), "FULL_FEATURE"))
                .subscribeOn(Schedulers.boundedElastic());
        }
    }
    
    /**
     * Custom backpressure buffer with overflow handling
     */
    public static class CustomBackpressureBuffer<T> {
        private final int maxSize;
        private final OverflowHandler<T> overflowHandler;
        
        public CustomBackpressureBuffer(int maxSize, OverflowHandler<T> overflowHandler) {
            this.maxSize = maxSize;
            this.overflowHandler = overflowHandler;
        }
        
        public Flux<T> apply(Flux<T> source) {
            return source.onBackpressureBuffer(
                maxSize,
                item -> overflowHandler.handle(item),
                BufferOverflowStrategy.DROP_OLDEST
            );
        }
        
        @FunctionalInterface
        public interface OverflowHandler<T> {
            void handle(T droppedItem);
        }
    }
    
    /**
     * Flow control with dynamic rate adjustment
     */
    public static class DynamicRateControl {
        private volatile int currentRate = 100; // requests per second
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong errorCount = new AtomicLong(0);
        
        public Flux<ProcessResult> processWithDynamicRate(Flux<InputData> input) {
            return input
                .sample(Duration.ofMillis(1000 / currentRate))
                .flatMap(this::processWithRateAdjustment)
                .doOnNext(result -> adjustRate(result));
        }
        
        private Mono<ProcessResult> processWithRateAdjustment(InputData data) {
            return processData(data)
                .doOnSuccess(result -> successCount.incrementAndGet())
                .doOnError(error -> errorCount.incrementAndGet());
        }
        
        private void adjustRate(ProcessResult result) {
            long totalRequests = successCount.get() + errorCount.get();
            if (totalRequests > 0 && totalRequests % 100 == 0) {
                double errorRate = (double) errorCount.get() / totalRequests;
                
                if (errorRate > 0.1) {
                    // High error rate, decrease rate
                    currentRate = Math.max(10, currentRate - 10);
                } else if (errorRate < 0.05) {
                    // Low error rate, increase rate
                    currentRate = Math.min(1000, currentRate + 10);
                }
                
                System.out.printf("Adjusted rate to %d req/sec (Error rate: %.2f%%)%n", 
                    currentRate, errorRate * 100);
            }
        }
        
        private Mono<ProcessResult> processData(InputData data) {
            return Mono.fromCallable(() -> {
                // Simulate processing with potential errors
                if (Math.random() < 0.1) {
                    throw new RuntimeException("Processing error");
                }
                return new ProcessResult(data.id(), "SUCCESS");
            }).subscribeOn(Schedulers.boundedElastic());
        }
    }
    
    // Supporting classes
    public record DataEvent(long id, String data, long timestamp) {}
    public record InputData(String id, String value) {}
    public record ProcessResult(String id, String status) {}
}
```

## 2. WebFlux Advanced Patterns

### Reactive Web Controllers with Advanced Features
```java
package com.javajourney.reactive.web;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Advanced WebFlux controllers with streaming and real-time features
 */
@RestController
@RequestMapping("/api/reactive")
public class AdvancedReactiveController {
    
    private final ReactiveOrderService orderService;
    private final ReactiveNotificationService notificationService;
    private final ReactiveMetricsService metricsService;
    
    /**
     * Server-Sent Events (SSE) for real-time updates
     */
    @GetMapping(value = "/orders/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<OrderEvent>> streamOrderEvents() {
        return orderService.getOrderEventStream()
            .map(event -> ServerSentEvent.<OrderEvent>builder()
                .id(String.valueOf(event.getId()))
                .event("order-update")
                .data(event)
                .comment("Order status update")
                .build())
            .onErrorResume(error -> {
                // Send error event to client
                return Flux.just(ServerSentEvent.<OrderEvent>builder()
                    .event("error")
                    .data(new OrderEvent(null, "ERROR", error.getMessage()))
                    .build());
            });
    }
    
    /**
     * WebSocket-style streaming with backpressure
     */
    @GetMapping(value = "/metrics/live", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<MetricSnapshot> streamLiveMetrics() {
        return metricsService.getLiveMetrics()
            .sample(Duration.ofSeconds(1)) // Limit to 1 update per second
            .onBackpressureLatest() // Keep only latest values under pressure
            .doOnCancel(() -> System.out.println("Client disconnected from metrics stream"));
    }
    
    /**
     * Chunked response for large datasets
     */
    @GetMapping("/orders/export")
    public Flux<String> exportOrdersAsCSV(@RequestParam String format) {
        return orderService.getAllOrders()
            .buffer(1000) // Process in chunks of 1000
            .map(this::convertToCSVChunk)
            .startWith(Mono.just("id,customer,amount,status\n")) // CSV header
            .onErrorResume(error -> Flux.just("ERROR: " + error.getMessage()));
    }
    
    /**
     * Reactive file upload with progress tracking
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<UploadResult> uploadFile(@RequestPart("file") Flux<DataBuffer> fileData) {
        AtomicLong bytesProcessed = new AtomicLong(0);
        
        return fileData
            .doOnNext(buffer -> {
                long bytes = bytesProcessed.addAndGet(buffer.readableByteCount());
                // Emit progress updates (could be sent via SSE to UI)
                notificationService.emitProgress("upload", bytes);
            })
            .reduce(DataBufferUtils.join())
            .flatMap(this::processUploadedFile)
            .map(result -> new UploadResult(true, "Upload successful", bytesProcessed.get()))
            .onErrorReturn(new UploadResult(false, "Upload failed", bytesProcessed.get()));
    }
    
    /**
     * Reactive pagination with cursor-based approach
     */
    @GetMapping("/orders")
    public Mono<PaginatedResponse<Order>> getOrders(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String filter) {
        
        return orderService.getOrdersPaginated(limit, cursor, filter)
            .collectList()
            .zipWith(orderService.hasMoreOrders(cursor, filter))
            .map(tuple -> {
                List<Order> orders = tuple.getT1();
                boolean hasMore = tuple.getT2();
                String nextCursor = orders.isEmpty() ? null : 
                    orders.get(orders.size() - 1).getId();
                
                return new PaginatedResponse<>(orders, nextCursor, hasMore);
            });
    }
    
    /**
     * Reactive request/response with timeout and fallback
     */
    @GetMapping("/orders/{id}")
    public Mono<ResponseEntity<Order>> getOrder(@PathVariable String id) {
        return orderService.findById(id)
            .timeout(Duration.ofSeconds(5))
            .map(ResponseEntity::ok)
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
            .onErrorReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
    }
    
    /**
     * Batch processing endpoint
     */
    @PostMapping("/orders/batch")
    public Flux<BatchProcessResult> processBatchOrders(@RequestBody Flux<OrderRequest> orderRequests) {
        return orderRequests
            .buffer(100, Duration.ofSeconds(5)) // Collect into batches
            .concatMap(batch -> 
                orderService.processBatch(batch)
                    .onErrorResume(error -> 
                        Mono.just(new BatchProcessResult(
                            batch.stream().map(OrderRequest::getId).toList(),
                            false,
                            error.getMessage()
                        ))
                    )
            );
    }
    
    /**
     * Reactive aggregation endpoint
     */
    @GetMapping("/analytics/orders/summary")
    public Mono<OrderSummary> getOrderSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        
        Mono<Long> totalOrders = orderService.countOrdersBetween(from, to);
        Mono<Double> totalRevenue = orderService.sumRevenueBetween(from, to);
        Mono<Double> averageOrderValue = orderService.averageOrderValueBetween(from, to);
        
        return Mono.zip(totalOrders, totalRevenue, averageOrderValue)
            .map(tuple -> new OrderSummary(
                tuple.getT1(),
                tuple.getT2(),
                tuple.getT3(),
                from,
                to
            ));
    }
    
    /**
     * Health check with reactive monitoring
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<HealthStatus>> healthCheck() {
        return Mono.zip(
            orderService.healthCheck().onErrorReturn(false),
            notificationService.healthCheck().onErrorReturn(false),
            metricsService.healthCheck().onErrorReturn(false)
        )
        .map(tuple -> {
            boolean orderServiceHealthy = tuple.getT1();
            boolean notificationServiceHealthy = tuple.getT2();
            boolean metricsServiceHealthy = tuple.getT3();
            
            boolean overall = orderServiceHealthy && notificationServiceHealthy && metricsServiceHealthy;
            
            HealthStatus status = new HealthStatus(
                overall ? "UP" : "DOWN",
                Map.of(
                    "orderService", orderServiceHealthy ? "UP" : "DOWN",
                    "notificationService", notificationServiceHealthy ? "UP" : "DOWN",
                    "metricsService", metricsServiceHealthy ? "UP" : "DOWN"
                )
            );
            
            return ResponseEntity.status(overall ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(status);
        });
    }
    
    private String convertToCSVChunk(List<Order> orders) {
        return orders.stream()
            .map(order -> String.format("%s,%s,%.2f,%s\n", 
                order.getId(), order.getCustomerId(), order.getAmount(), order.getStatus()))
            .collect(Collectors.joining());
    }
    
    private Mono<String> processUploadedFile(DataBuffer dataBuffer) {
        return Mono.fromCallable(() -> {
            // Process the uploaded file
            byte[] bytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(bytes);
            
            // Simulate file processing
            return "Processed " + bytes.length + " bytes";
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
    // Data classes
    public record OrderEvent(String id, String type, String data) {}
    public record MetricSnapshot(String name, double value, long timestamp) {}
    public record UploadResult(boolean success, String message, long bytesProcessed) {}
    public record PaginatedResponse<T>(List<T> data, String nextCursor, boolean hasMore) {}
    public record BatchProcessResult(List<String> orderIds, boolean success, String message) {}
    public record OrderSummary(long totalOrders, double totalRevenue, double averageOrderValue, 
                              LocalDate from, LocalDate to) {}
    public record HealthStatus(String status, Map<String, String> services) {}
    public record OrderRequest(String id, String customerId, double amount) {}
    public record Order(String id, String customerId, double amount, String status) {}
}
```

### Reactive Error Handling and Resilience
```java
package com.javajourney.reactive.resilience;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Advanced error handling and resilience patterns for reactive applications
 */
@Service
public class ReactiveResilienceService {
    
    private final ReactiveCircuitBreaker circuitBreaker;
    private final ReactiveRateLimiter rateLimiter;
    private final ReactiveBulkhead bulkhead;
    
    /**
     * Comprehensive error handling with multiple recovery strategies
     */
    public Mono<ProcessResult> processWithResilience(ProcessRequest request) {
        return Mono.just(request)
            // Apply rate limiting
            .flatMap(rateLimiter::acquire)
            
            // Apply bulkhead pattern
            .flatMap(bulkhead::execute)
            
            // Apply circuit breaker
            .flatMap(circuitBreaker::execute)
            
            // Main processing logic
            .flatMap(this::performProcessing)
            
            // Comprehensive retry strategy
            .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                .maxBackoff(Duration.ofSeconds(5))
                .jitter(0.1)
                .filter(this::isRetryableException)
                .doBeforeRetry(retrySignal -> 
                    log.warn("Retrying request {} (attempt {})", 
                        request.getId(), retrySignal.totalRetries() + 1))
            )
            
            // Fallback strategies based on error type
            .onErrorResume(NetworkException.class, error -> 
                handleNetworkError(request, error))
            
            .onErrorResume(ValidationException.class, error -> 
                handleValidationError(request, error))
            
            .onErrorResume(DatabaseException.class, error -> 
                handleDatabaseError(request, error))
            
            // Final fallback
            .onErrorResume(error -> 
                handleGenericError(request, error))
            
            // Global timeout
            .timeout(Duration.ofSeconds(30))
            
            // Log success/failure metrics
            .doOnSuccess(result -> recordSuccessMetrics(request, result))
            .doOnError(error -> recordErrorMetrics(request, error));
    }
    
    /**
     * Reactive circuit breaker implementation
     */
    @Component
    public static class ReactiveCircuitBreaker {
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private volatile CircuitState state = CircuitState.CLOSED;
        private volatile long lastFailureTime = 0;
        
        private final int failureThreshold = 5;
        private final int requestThreshold = 10;
        private final Duration openTimeout = Duration.ofMinutes(1);
        
        public <T> Mono<T> execute(Mono<T> operation) {
            return Mono.defer(() -> {
                if (state == CircuitState.OPEN) {
                    if (System.currentTimeMillis() - lastFailureTime > openTimeout.toMillis()) {
                        state = CircuitState.HALF_OPEN;
                        System.out.println("Circuit breaker moving to HALF_OPEN");
                    } else {
                        return Mono.error(new CircuitBreakerOpenException("Circuit breaker is OPEN"));
                    }
                }
                
                return operation
                    .doOnSuccess(result -> handleSuccess())
                    .doOnError(error -> handleFailure(error));
            });
        }
        
        private void handleSuccess() {
            requestCount.incrementAndGet();
            if (state == CircuitState.HALF_OPEN) {
                state = CircuitState.CLOSED;
                failureCount.set(0);
                System.out.println("Circuit breaker CLOSED");
            }
        }
        
        private void handleFailure(Throwable error) {
            int failures = failureCount.incrementAndGet();
            int requests = requestCount.incrementAndGet();
            
            if (requests >= requestThreshold && 
                failures >= failureThreshold && 
                (double) failures / requests >= 0.5) {
                
                state = CircuitState.OPEN;
                lastFailureTime = System.currentTimeMillis();
                System.out.println("Circuit breaker OPENED due to failures");
            }
        }
        
        public CircuitState getState() { return state; }
        
        public enum CircuitState { CLOSED, OPEN, HALF_OPEN }
    }
    
    /**
     * Reactive rate limiter with token bucket algorithm
     */
    @Component
    public static class ReactiveRateLimiter {
        private final AtomicInteger tokens = new AtomicInteger(100);
        private volatile long lastRefill = System.currentTimeMillis();
        
        private final int maxTokens = 100;
        private final int refillRate = 10; // tokens per second
        
        public <T> Mono<T> acquire(T item) {
            return Mono.defer(() -> {
                refillTokens();
                
                if (tokens.get() > 0) {
                    tokens.decrementAndGet();
                    return Mono.just(item);
                } else {
                    return Mono.error(new RateLimitExceededException("Rate limit exceeded"));
                }
            });
        }
        
        private void refillTokens() {
            long now = System.currentTimeMillis();
            long timeSinceLastRefill = now - lastRefill;
            
            if (timeSinceLastRefill >= 1000) { // 1 second
                int tokensToAdd = (int) (timeSinceLastRefill / 1000 * refillRate);
                int currentTokens = tokens.get();
                int newTokens = Math.min(maxTokens, currentTokens + tokensToAdd);
                
                tokens.set(newTokens);
                lastRefill = now;
            }
        }
    }
    
    /**
     * Reactive bulkhead pattern for resource isolation
     */
    @Component
    public static class ReactiveBulkhead {
        private final Scheduler scheduler;
        private final Semaphore semaphore;
        
        public ReactiveBulkhead(@Value("${bulkhead.max-concurrent:10}") int maxConcurrent) {
            this.scheduler = Schedulers.newBoundedElastic(
                maxConcurrent, 
                Integer.MAX_VALUE, 
                "bulkhead-scheduler"
            );
            this.semaphore = new Semaphore(maxConcurrent);
        }
        
        public <T> Mono<T> execute(Mono<T> operation) {
            return Mono.fromCallable(() -> {
                if (!semaphore.tryAcquire()) {
                    throw new BulkheadFullException("Bulkhead capacity exceeded");
                }
                return true;
            })
            .flatMap(acquired -> operation
                .doFinally(signal -> semaphore.release())
                .subscribeOn(scheduler)
            );
        }
    }
    
    /**
     * Advanced timeout handling with cascading timeouts
     */
    public Mono<CascadeResult> processWithCascadingTimeouts(ProcessRequest request) {
        return performQuickCheck(request)
            .timeout(Duration.ofMillis(500), fallbackQuickCheck(request))
            
            .flatMap(quickResult -> {
                if (quickResult.isValid()) {
                    return performDetailedProcessing(request)
                        .timeout(Duration.ofSeconds(5), fallbackDetailedProcessing(request));
                } else {
                    return Mono.just(new CascadeResult("QUICK_CHECK_FAILED", quickResult));
                }
            })
            
            .flatMap(detailedResult -> {
                if (detailedResult.requiresValidation()) {
                    return performValidation(request)
                        .timeout(Duration.ofSeconds(2), fallbackValidation(request))
                        .map(validationResult -> detailedResult.withValidation(validationResult));
                } else {
                    return Mono.just(detailedResult);
                }
            });
    }
    
    /**
     * Error recovery with compensation patterns
     */
    public Mono<TransactionResult> processTransactionWithCompensation(TransactionRequest request) {
        List<CompensationAction> compensations = new ArrayList<>();
        
        return reserveInventory(request)
            .doOnSuccess(result -> compensations.add(() -> releaseInventory(result)))
            
            .flatMap(inventoryResult -> processPayment(request)
                .doOnSuccess(result -> compensations.add(() -> refundPayment(result)))
            )
            
            .flatMap(paymentResult -> createOrder(request)
                .doOnSuccess(result -> compensations.add(() -> cancelOrder(result)))
            )
            
            .flatMap(orderResult -> sendConfirmation(request))
            
            .onErrorResume(error -> {
                // Execute compensations in reverse order
                Collections.reverse(compensations);
                return executeCompensations(compensations)
                    .then(Mono.error(new TransactionFailedException("Transaction failed and compensated", error)));
            });
    }
    
    private boolean isRetryableException(Throwable throwable) {
        return throwable instanceof NetworkException ||
               throwable instanceof TransientException ||
               (throwable instanceof DatabaseException && 
                ((DatabaseException) throwable).isTransient());
    }
    
    private Mono<ProcessResult> handleNetworkError(ProcessRequest request, NetworkException error) {
        // Try alternative endpoint or cached data
        return getCachedResult(request.getId())
            .switchIfEmpty(Mono.just(ProcessResult.networkError(request.getId(), error.getMessage())));
    }
    
    private Mono<ProcessResult> handleValidationError(ProcessRequest request, ValidationException error) {
        return Mono.just(ProcessResult.validationError(request.getId(), error.getMessage()));
    }
    
    private Mono<ProcessResult> handleDatabaseError(ProcessRequest request, DatabaseException error) {
        if (error.isTransient()) {
            // Use eventual consistency approach
            return queueForLaterProcessing(request)
                .map(queued -> ProcessResult.queued(request.getId()));
        } else {
            return Mono.just(ProcessResult.databaseError(request.getId(), error.getMessage()));
        }
    }
    
    private Mono<ProcessResult> handleGenericError(ProcessRequest request, Throwable error) {
        return Mono.just(ProcessResult.systemError(request.getId(), error.getMessage()));
    }
    
    // Supporting methods and classes would be implemented here
    private Mono<ProcessResult> performProcessing(ProcessRequest request) {
        return Mono.fromCallable(() -> ProcessResult.success(request.getId(), "Processed"))
            .subscribeOn(Schedulers.boundedElastic());
    }
    
    private Mono<Void> executeCompensations(List<CompensationAction> compensations) {
        return Flux.fromIterable(compensations)
            .flatMap(CompensationAction::execute)
            .then();
    }
    
    @FunctionalInterface
    public interface CompensationAction {
        Mono<Void> execute();
    }
    
    // Exception classes
    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) { super(message); }
    }
    
    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) { super(message); }
    }
    
    public static class BulkheadFullException extends RuntimeException {
        public BulkheadFullException(String message) { super(message); }
    }
    
    public static class TransactionFailedException extends RuntimeException {
        public TransactionFailedException(String message, Throwable cause) { super(message, cause); }
    }
    
    // Data classes
    public record ProcessRequest(String id, String data) {}
    public record ProcessResult(String id, String status, String message, Object data) {
        public static ProcessResult success(String id, String data) {
            return new ProcessResult(id, "SUCCESS", null, data);
        }
        
        public static ProcessResult networkError(String id, String message) {
            return new ProcessResult(id, "NETWORK_ERROR", message, null);
        }
        
        public static ProcessResult validationError(String id, String message) {
            return new ProcessResult(id, "VALIDATION_ERROR", message, null);
        }
        
        public static ProcessResult databaseError(String id, String message) {
            return new ProcessResult(id, "DATABASE_ERROR", message, null);
        }
        
        public static ProcessResult systemError(String id, String message) {
            return new ProcessResult(id, "SYSTEM_ERROR", message, null);
        }
        
        public static ProcessResult queued(String id) {
            return new ProcessResult(id, "QUEUED", "Queued for later processing", null);
        }
    }
}
```

## 3. Reactive Database Integration with R2DBC

### R2DBC Configuration and Repository Patterns
```java
package com.javajourney.reactive.database;

import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;

/**
 * Advanced R2DBC configuration with connection pooling and optimization
 */
@Configuration
@EnableR2dbcRepositories
public class ReactiveDataConfiguration extends AbstractR2dbcConfiguration {
    
    @Override
    @Bean
    public ConnectionFactory connectionFactory() {
        // Primary database connection factory
        PostgresqlConnectionFactory primaryFactory = new PostgresqlConnectionFactory(
            PostgresqlConnectionConfiguration.builder()
                .host("localhost")
                .port(5432)
                .database("reactive_db")
                .username("reactive_user")
                .password("reactive_password")
                
                // Performance optimizations
                .preparedStatementCacheQueries(256)
                .tcpKeepAlive(true)
                .tcpNoDelay(true)
                
                // Connection timeout settings
                .connectTimeout(Duration.ofSeconds(10))
                .socketTimeout(Duration.ofSeconds(30))
                
                .build()
        );
        
        // Connection pooling configuration
        ConnectionPoolConfiguration poolConfiguration = ConnectionPoolConfiguration.builder(primaryFactory)
            .maxIdleTime(Duration.ofMinutes(30))
            .maxLifeTime(Duration.ofHours(2))
            .maxAcquireTime(Duration.ofSeconds(60))
            .maxCreateConnectionTime(Duration.ofSeconds(30))
            .initialSize(5)
            .maxSize(20)
            .validationQuery("SELECT 1")
            .build();
        
        return new ConnectionPool(poolConfiguration);
    }
    
    /**
     * Read-only connection factory for read replicas
     */
    @Bean("readOnlyConnectionFactory")
    public ConnectionFactory readOnlyConnectionFactory() {
        PostgresqlConnectionFactory readOnlyFactory = new PostgresqlConnectionFactory(
            PostgresqlConnectionConfiguration.builder()
                .host("readonly-replica")
                .port(5432)
                .database("reactive_db")
                .username("readonly_user")
                .password("readonly_password")
                .preparedStatementCacheQueries(512) // Higher cache for reads
                .build()
        );
        
        return new ConnectionPool(
            ConnectionPoolConfiguration.builder(readOnlyFactory)
                .maxSize(30) // More connections for read operations
                .initialSize(10)
                .build()
        );
    }
    
    /**
     * Custom R2DBC template for advanced operations
     */
    @Bean
    public R2dbcEntityTemplate r2dbcEntityTemplate() {
        return new R2dbcEntityTemplate(connectionFactory());
    }
    
    /**
     * Read-only R2DBC template
     */
    @Bean("readOnlyR2dbcTemplate")
    public R2dbcEntityTemplate readOnlyR2dbcTemplate(@Qualifier("readOnlyConnectionFactory") ConnectionFactory readOnlyConnectionFactory) {
        return new R2dbcEntityTemplate(readOnlyConnectionFactory);
    }
    
    /**
     * Custom converter for complex data types
     */
    @Bean
    @Override
    public R2dbcCustomConversions r2dbcCustomConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new JsonNodeToStringConverter());
        converters.add(new StringToJsonNodeConverter());
        converters.add(new LocalDateTimeToTimestampConverter());
        converters.add(new TimestampToLocalDateTimeConverter());
        
        return new R2dbcCustomConversions(getStoreConversions(), converters);
    }
    
    // Custom converters
    @ReadingConverter
    public static class StringToJsonNodeConverter implements Converter<String, JsonNode> {
        private final ObjectMapper objectMapper = new ObjectMapper();
        
        @Override
        public JsonNode convert(String source) {
            try {
                return objectMapper.readTree(source);
            } catch (Exception e) {
                throw new ConversionFailedException(TypeDescriptor.valueOf(String.class),
                    TypeDescriptor.valueOf(JsonNode.class), source, e);
            }
        }
    }
    
    @WritingConverter
    public static class JsonNodeToStringConverter implements Converter<JsonNode, String> {
        @Override
        public String convert(JsonNode source) {
            return source.toString();
        }
    }
}
```

### Advanced Reactive Repository Patterns
```java
package com.javajourney.reactive.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Advanced reactive repository with custom queries and optimizations
 */
public interface ReactiveOrderRepository extends R2dbcRepository<Order, String>, CustomOrderRepository {
    
    // Basic reactive queries
    Flux<Order> findByCustomerIdOrderByCreatedAtDesc(String customerId);
    
    Mono<Long> countByStatusAndCreatedAtBetween(String status, LocalDateTime start, LocalDateTime end);
    
    // Complex queries with joins
    @Query("""
        SELECT o.*, c.name as customer_name, c.email as customer_email
        FROM orders o 
        JOIN customers c ON o.customer_id = c.id 
        WHERE o.status = :status 
        ORDER BY o.created_at DESC 
        LIMIT :limit OFFSET :offset
        """)
    Flux<OrderWithCustomer> findOrdersWithCustomerByStatus(String status, int limit, int offset);
    
    // Aggregation queries
    @Query("""
        SELECT 
            DATE_TRUNC('day', created_at) as date,
            COUNT(*) as order_count,
            SUM(amount) as total_amount,
            AVG(amount) as average_amount
        FROM orders 
        WHERE created_at BETWEEN :startDate AND :endDate
        GROUP BY DATE_TRUNC('day', created_at)
        ORDER BY date
        """)
    Flux<DailyOrderSummary> getDailyOrderSummary(LocalDateTime startDate, LocalDateTime endDate);
    
    // Streaming large datasets
    @Query("SELECT * FROM orders WHERE created_at >= :since ORDER BY created_at")
    Flux<Order> streamOrdersSince(LocalDateTime since);
    
    // Window functions for analytics
    @Query("""
        SELECT 
            *,
            ROW_NUMBER() OVER (PARTITION BY customer_id ORDER BY created_at DESC) as row_num,
            LAG(amount) OVER (PARTITION BY customer_id ORDER BY created_at) as previous_amount
        FROM orders 
        WHERE customer_id = :customerId
        """)
    Flux<OrderWithAnalytics> getOrdersWithAnalytics(String customerId);
}

/**
 * Custom repository implementation for complex operations
 */
@Repository
public class CustomOrderRepositoryImpl implements CustomOrderRepository {
    
    private final R2dbcEntityTemplate template;
    private final R2dbcEntityTemplate readOnlyTemplate;
    
    public CustomOrderRepositoryImpl(R2dbcEntityTemplate template,
                                   @Qualifier("readOnlyR2dbcTemplate") R2dbcEntityTemplate readOnlyTemplate) {
        this.template = template;
        this.readOnlyTemplate = readOnlyTemplate;
    }
    
    @Override
    public Flux<Order> findOrdersWithDynamicFilters(OrderSearchCriteria criteria) {
        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM orders WHERE 1=1");
        List<Object> parameters = new ArrayList<>();
        
        if (criteria.getCustomerId() != null) {
            queryBuilder.append(" AND customer_id = $").append(parameters.size() + 1);
            parameters.add(criteria.getCustomerId());
        }
        
        if (criteria.getStatus() != null) {
            queryBuilder.append(" AND status = $").append(parameters.size() + 1);
            parameters.add(criteria.getStatus());
        }
        
        if (criteria.getMinAmount() != null) {
            queryBuilder.append(" AND amount >= $").append(parameters.size() + 1);
            parameters.add(criteria.getMinAmount());
        }
        
        if (criteria.getMaxAmount() != null) {
            queryBuilder.append(" AND amount <= $").append(parameters.size() + 1);
            parameters.add(criteria.getMaxAmount());
        }
        
        if (criteria.getDateFrom() != null) {
            queryBuilder.append(" AND created_at >= $").append(parameters.size() + 1);
            parameters.add(criteria.getDateFrom());
        }
        
        if (criteria.getDateTo() != null) {
            queryBuilder.append(" AND created_at <= $").append(parameters.size() + 1);
            parameters.add(criteria.getDateTo());
        }
        
        queryBuilder.append(" ORDER BY created_at DESC");
        
        if (criteria.getLimit() > 0) {
            queryBuilder.append(" LIMIT $").append(parameters.size() + 1);
            parameters.add(criteria.getLimit());
        }
        
        return readOnlyTemplate
            .getDatabaseClient()
            .sql(queryBuilder.toString())
            .bindValues(parameters.toArray())
            .map((row, metadata) -> mapRowToOrder(row))
            .all();
    }
    
    @Override
    public Mono<BatchUpdateResult> updateOrderStatusBatch(List<String> orderIds, String status) {
        String sql = "UPDATE orders SET status = $1, updated_at = CURRENT_TIMESTAMP WHERE id = ANY($2)";
        
        return template
            .getDatabaseClient()
            .sql(sql)
            .bind(0, status)
            .bind(1, orderIds.toArray(new String[0]))
            .fetch()
            .rowsUpdated()
            .map(updatedRows -> new BatchUpdateResult(updatedRows, orderIds.size()));
    }
    
    @Override
    public Flux<Order> findOrdersWithCustomPagination(String cursor, int limit) {
        String sql;
        Object[] parameters;
        
        if (cursor != null) {
            sql = """
                SELECT * FROM orders 
                WHERE created_at < (SELECT created_at FROM orders WHERE id = $1)
                ORDER BY created_at DESC 
                LIMIT $2
                """;
            parameters = new Object[]{cursor, limit};
        } else {
            sql = "SELECT * FROM orders ORDER BY created_at DESC LIMIT $1";
            parameters = new Object[]{limit};
        }
        
        return readOnlyTemplate
            .getDatabaseClient()
            .sql(sql)
            .bindValues(parameters)
            .map((row, metadata) -> mapRowToOrder(row))
            .all();
    }
    
    @Override
    public Mono<Void> bulkInsertOrders(Flux<Order> orders) {
        return orders
            .buffer(1000) // Process in batches of 1000
            .flatMap(batch -> {
                String sql = """
                    INSERT INTO orders (id, customer_id, amount, status, created_at, updated_at) 
                    VALUES ($1, $2, $3, $4, $5, $6)
                    """;
                
                return template
                    .getDatabaseClient()
                    .inConnectionMany(connection -> {
                        Statement statement = connection.createStatement(sql);
                        
                        for (Order order : batch) {
                            statement
                                .bind("$1", order.getId())
                                .bind("$2", order.getCustomerId())
                                .bind("$3", order.getAmount())
                                .bind("$4", order.getStatus())
                                .bind("$5", order.getCreatedAt())
                                .bind("$6", order.getUpdatedAt())
                                .add();
                        }
                        
                        return Flux.from(statement.execute())
                            .flatMap(Result::getRowsUpdated);
                    });
            })
            .then();
    }
    
    @Override
    public Flux<OrderMetrics> getOrderMetricsStream() {
        return template
            .getDatabaseClient()
            .sql("""
                SELECT 
                    COUNT(*) as total_orders,
                    SUM(amount) as total_revenue,
                    AVG(amount) as average_order_value,
                    COUNT(DISTINCT customer_id) as unique_customers,
                    CURRENT_TIMESTAMP as calculated_at
                FROM orders 
                WHERE created_at >= CURRENT_DATE
                """)
            .map((row, metadata) -> new OrderMetrics(
                row.get("total_orders", Long.class),
                row.get("total_revenue", BigDecimal.class),
                row.get("average_order_value", BigDecimal.class),
                row.get("unique_customers", Long.class),
                row.get("calculated_at", LocalDateTime.class)
            ))
            .first()
            .repeat(Duration.ofSeconds(30)); // Emit every 30 seconds
    }
    
    private Order mapRowToOrder(Row row) {
        return Order.builder()
            .id(row.get("id", String.class))
            .customerId(row.get("customer_id", String.class))
            .amount(row.get("amount", BigDecimal.class))
            .status(row.get("status", String.class))
            .createdAt(row.get("created_at", LocalDateTime.class))
            .updatedAt(row.get("updated_at", LocalDateTime.class))
            .build();
    }
}

// Supporting interfaces and classes
public interface CustomOrderRepository {
    Flux<Order> findOrdersWithDynamicFilters(OrderSearchCriteria criteria);
    Mono<BatchUpdateResult> updateOrderStatusBatch(List<String> orderIds, String status);
    Flux<Order> findOrdersWithCustomPagination(String cursor, int limit);
    Mono<Void> bulkInsertOrders(Flux<Order> orders);
    Flux<OrderMetrics> getOrderMetricsStream();
}

// Data classes
@Data
@Builder
@Table("orders")
public class Order {
    @Id
    private String id;
    
    @Column("customer_id")
    private String customerId;
    
    private BigDecimal amount;
    private String status;
    
    @Column("created_at")
    private LocalDateTime createdAt;
    
    @Column("updated_at")
    private LocalDateTime updatedAt;
    
    // Additional fields for analytics
    @Transient
    private String customerName;
    
    @Transient
    private String customerEmail;
}

public record OrderSearchCriteria(
    String customerId,
    String status,
    BigDecimal minAmount,
    BigDecimal maxAmount,
    LocalDateTime dateFrom,
    LocalDateTime dateTo,
    int limit
) {}

public record OrderWithCustomer(
    String id,
    String customerId,
    BigDecimal amount,
    String status,
    LocalDateTime createdAt,
    String customerName,
    String customerEmail
) {}

public record DailyOrderSummary(
    LocalDate date,
    Long orderCount,
    BigDecimal totalAmount,
    BigDecimal averageAmount
) {}

public record OrderWithAnalytics(
    String id,
    String customerId,
    BigDecimal amount,
    String status,
    LocalDateTime createdAt,
    Integer rowNum,
    BigDecimal previousAmount
) {}

public record BatchUpdateResult(long updatedRows, int totalRequested) {}

public record OrderMetrics(
    Long totalOrders,
    BigDecimal totalRevenue,
    BigDecimal averageOrderValue,
    Long uniqueCustomers,
    LocalDateTime calculatedAt
) {}
```

## 4. Performance Optimization

### WebFlux Performance Tuning
```java
package com.javajourney.reactive.performance;

import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

/**
 * WebFlux performance optimization configuration
 */
@Configuration
public class WebFluxPerformanceConfiguration {
    
    /**
     * Optimized Netty server configuration
     */
    @Bean
    public NettyReactiveWebServerFactory nettyReactiveWebServerFactory() {
        NettyReactiveWebServerFactory factory = new NettyReactiveWebServerFactory();
        
        factory.addServerCustomizers(httpServer -> 
            httpServer
                // Connection pool optimization
                .runOn(optimizedLoopResources())
                
                // TCP optimization
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_RCVBUF, 32 * 1024)
                .childOption(ChannelOption.SO_SNDBUF, 32 * 1024)
                
                // HTTP optimization
                .httpRequestDecoder(spec -> spec
                    .maxInitialLineLength(8192)
                    .maxHeaderSize(16384)
                    .maxChunkSize(16384)
                    .validateHeaders(false)
                )
                
                // Request timeout
                .idleTimeout(Duration.ofSeconds(60))
                
                // Compression
                .compress(true)
        );
        
        return factory;
    }
    
    /**
     * Optimized loop resources for better thread management
     */
    @Bean
    public LoopResources optimizedLoopResources() {
        int workerThreads = Math.max(4, Runtime.getRuntime().availableProcessors());
        
        return LoopResources.create("webflux-http", 1, workerThreads, true);
    }
    
    /**
     * Connection provider for HTTP client optimization
     */
    @Bean
    public ConnectionProvider optimizedConnectionProvider() {
        return ConnectionProvider.builder("optimized")
            .maxConnections(1000)
            .maxIdleTime(Duration.ofSeconds(30))
            .maxLifeTime(Duration.ofMinutes(5))
            .pendingAcquireMaxCount(2000)
            .pendingAcquireTimeout(Duration.ofSeconds(30))
            .evictInBackground(Duration.ofSeconds(60))
            .build();
    }
    
    /**
     * Optimized WebClient for external API calls
     */
    @Bean
    public WebClient optimizedWebClient(ConnectionProvider connectionProvider) {
        HttpClient httpClient = HttpClient.create(connectionProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .responseTimeout(Duration.ofSeconds(30))
            .keepAlive(true)
            .compress(true)
            .followRedirect(true);
        
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .codecs(configurer -> {
                configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024); // 10MB
                configurer.defaultCodecs().enableLoggingRequestDetails(false);
            })
            .build();
    }
    
    /**
     * Custom scheduler configuration for different workload types
     */
    @Configuration
    public static class SchedulerConfiguration {
        
        @Bean("ioScheduler")
        public Scheduler ioScheduler() {
            return Schedulers.newBoundedElastic(
                100, // max threads
                Integer.MAX_VALUE,
                "io-scheduler",
                60 // TTL in seconds
            );
        }
        
        @Bean("cpuScheduler")
        public Scheduler cpuScheduler() {
            int parallelism = Runtime.getRuntime().availableProcessors();
            return Schedulers.newParallel("cpu-scheduler", parallelism);
        }
        
        @Bean("databaseScheduler")
        public Scheduler databaseScheduler() {
            return Schedulers.newBoundedElastic(
                50, // max threads for DB operations
                Integer.MAX_VALUE,
                "database-scheduler",
                60
            );
        }
    }
    
    /**
     * Request/Response optimization filters
     */
    @Component
    public static class PerformanceFilters {
        
        /**
         * Request timing and metrics filter
         */
        @Bean
        public WebFilter requestTimingFilter(MeterRegistry meterRegistry) {
            return (exchange, chain) -> {
                long startTime = System.nanoTime();
                String path = exchange.getRequest().getPath().value();
                String method = exchange.getRequest().getMethod().name();
                
                return chain.filter(exchange)
                    .doFinally(signalType -> {
                        long duration = System.nanoTime() - startTime;
                        
                        Timer.Sample sample = Timer.start(meterRegistry);
                        sample.stop(Timer.builder("http.request.duration")
                            .tag("method", method)
                            .tag("path", path)
                            .tag("status", String.valueOf(exchange.getResponse().getStatusCode().value()))
                            .register(meterRegistry));
                        
                        // Log slow requests
                        if (duration > 1_000_000_000) { // 1 second
                            System.out.printf("Slow request: %s %s took %d ms%n", 
                                method, path, duration / 1_000_000);
                        }
                    });
            };
        }
        
        /**
         * Response caching filter for static content
         */
        @Bean
        public WebFilter responseCachingFilter() {
            Map<String, String> cacheHeaders = Map.of(
                "/api/static/", "public, max-age=3600",
                "/api/config/", "public, max-age=300",
                "/api/health", "no-cache"
            );
            
            return (exchange, chain) -> {
                String path = exchange.getRequest().getPath().value();
                
                // Set cache headers based on path
                cacheHeaders.forEach((pathPrefix, cacheControl) -> {
                    if (path.startsWith(pathPrefix)) {
                        exchange.getResponse().getHeaders().add("Cache-Control", cacheControl);
                    }
                });
                
                return chain.filter(exchange);
            };
        }
        
        /**
         * Request size limiting filter
         */
        @Bean
        public WebFilter requestSizeLimitFilter() {
            long maxRequestSize = 10 * 1024 * 1024; // 10MB
            
            return (exchange, chain) -> {
                ServerHttpRequest request = exchange.getRequest();
                
                // Check content length
                if (request.getHeaders().getContentLength() > maxRequestSize) {
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.PAYLOAD_TOO_LARGE);
                    return response.setComplete();
                }
                
                return chain.filter(exchange);
            };
        }
    }
    
    /**
     * Memory and GC optimization
     */
    @Component
    public static class MemoryOptimization {
        
        /**
         * Monitor and optimize memory usage
         */
        @EventListener
        @Async
        public void monitorMemoryUsage(ApplicationReadyEvent event) {
            Flux.interval(Duration.ofMinutes(5))
                .doOnNext(tick -> {
                    Runtime runtime = Runtime.getRuntime();
                    long totalMemory = runtime.totalMemory();
                    long freeMemory = runtime.freeMemory();
                    long usedMemory = totalMemory - freeMemory;
                    long maxMemory = runtime.maxMemory();
                    
                    double memoryUsage = (double) usedMemory / maxMemory * 100;
                    
                    if (memoryUsage > 80) {
                        System.out.printf("High memory usage: %.1f%% (%d MB / %d MB)%n",
                            memoryUsage, usedMemory / 1024 / 1024, maxMemory / 1024 / 1024);
                        
                        // Suggest GC
                        System.gc();
                    }
                })
                .subscribe();
        }
        
        /**
         * Optimize object allocation patterns
         */
        public <T> Flux<T> optimizeForMemory(Flux<T> source) {
            return source
                // Process in smaller chunks to reduce memory pressure
                .buffer(100)
                .flatMap(chunk -> 
                    Flux.fromIterable(chunk)
                        .subscribeOn(Schedulers.parallel())
                )
                // Release references promptly
                .doOnNext(item -> {
                    // Process item immediately
                })
                // Use backpressure to prevent memory buildup
                .onBackpressureBuffer(1000, BufferOverflowStrategy.DROP_OLDEST);
        }
    }
}
```

## Practice Exercises

### Exercise 1: Build Reactive Event Streaming System
Create a complete event streaming system with WebFlux, SSE, and backpressure handling.

### Exercise 2: Implement Reactive Saga Pattern
Build a distributed transaction system using reactive saga pattern with compensation logic.

### Exercise 3: Optimize Reactive Database Access
Create an optimized R2DBC repository with advanced querying and performance monitoring.

### Exercise 4: Build Reactive Microservice
Implement a complete reactive microservice with resilience patterns, monitoring, and performance optimization.

## Key Takeaways

1. **Reactive Streams**: Master reactive composition patterns for complex business logic
2. **Backpressure**: Implement proper flow control to handle varying load conditions
3. **Error Handling**: Use comprehensive error handling with retry and fallback strategies
4. **Database Integration**: Leverage R2DBC for non-blocking database operations
5. **Performance**: Optimize WebFlux applications for high throughput and low latency
6. **Resilience**: Implement circuit breakers, bulkheads, and compensation patterns

## Next Steps
Tomorrow we'll explore Enterprise Integration patterns with ESB, message transformation, and routing - applying reactive principles to enterprise integration scenarios.

<function_calls>
<invoke name="TodoWrite">
<parameter name="todos">[{"content": "Create Day 85: Java Virtual Machine (JVM) Deep Dive - Memory Management & Garbage Collection", "status": "completed", "priority": "high", "id": "day85-jvm-deep-dive"}, {"content": "Create Day 86: Advanced JVM Tuning - Performance Optimization & Profiling", "status": "completed", "priority": "high", "id": "day86-jvm-tuning"}, {"content": "Create Day 87: Java Security - Authentication, Authorization & Cryptography", "status": "completed", "priority": "high", "id": "day87-java-security"}, {"content": "Create Day 88: Advanced Database Patterns - Connection Pooling, Transactions & Optimization", "status": "completed", "priority": "high", "id": "day88-database-patterns"}, {"content": "Create Day 89: Reactive Programming - Project Reactor & WebFlux Advanced", "status": "completed", "priority": "high", "id": "day89-reactive-programming"}, {"content": "Create Day 90: Enterprise Integration - ESB, Message Transformation & Routing", "status": "in_progress", "priority": "high", "id": "day90-enterprise-integration"}, {"content": "Create Day 91: Week 13 Capstone - High-Performance Enterprise Financial System", "status": "pending", "priority": "high", "id": "day91-week13-capstone"}]