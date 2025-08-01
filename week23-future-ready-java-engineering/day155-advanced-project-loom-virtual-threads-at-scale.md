# Day 155: Advanced Project Loom - Virtual Threads at Scale

## Learning Objectives
By the end of this session, you will:
- Understand the internal architecture and implementation of virtual threads
- Master structured concurrency patterns and best practices
- Implement virtual thread monitoring and debugging techniques
- Apply performance tuning and optimization strategies for virtual threads
- Build scalable concurrent applications handling millions of virtual threads

## Theoretical Foundation

### Virtual Thread Internals

**Virtual Thread Architecture:**
```java
// Virtual thread lifecycle and management
public class VirtualThreadInternals {
    
    // Understanding carrier thread management
    public void demonstrateCarrierThreads() {
        // Virtual threads are mounted on carrier threads (platform threads)
        System.out.println("Available processors: " + 
            Runtime.getRuntime().availableProcessors());
        
        // Default ForkJoinPool for virtual threads
        ForkJoinPool carrierPool = ForkJoinPool.commonPool();
        System.out.println("Carrier pool parallelism: " + 
            carrierPool.getParallelism());
    }
    
    // Virtual thread state transitions
    public void demonstrateThreadStates() throws InterruptedException {
        Thread virtualThread = Thread.ofVirtual()
            .name("demo-virtual-thread")
            .start(() -> {
                try {
                    System.out.println("Virtual thread running on: " + 
                        Thread.currentThread().getName());
                    
                    // Parking operation - thread becomes unmounted
                    Thread.sleep(1000);
                    
                    System.out.println("Virtual thread resumed on: " + 
                        Thread.currentThread().getName());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        
        virtualThread.join();
    }
}
```

**Memory Model and Virtual Threads:**
```java
// Understanding memory implications of virtual threads
public class VirtualThreadMemory {
    
    private static final AtomicLong threadCounter = new AtomicLong(0);
    
    public void compareMemoryUsage() {
        // Virtual thread stack size is much smaller than platform threads
        long virtualThreadMemory = measureVirtualThreadMemory();
        long platformThreadMemory = measurePlatformThreadMemory();
        
        System.out.printf("Virtual thread memory overhead: %d bytes%n", 
            virtualThreadMemory);
        System.out.printf("Platform thread memory overhead: %d bytes%n", 
            platformThreadMemory);
        System.out.printf("Memory ratio: %.2f%n", 
            (double) platformThreadMemory / virtualThreadMemory);
    }
    
    private long measureVirtualThreadMemory() {
        Runtime runtime = Runtime.getRuntime();
        System.gc();
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
        
        List<Thread> threads = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1000);
        
        for (int i = 0; i < 1000; i++) {
            Thread vt = Thread.ofVirtual().start(() -> {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads.add(vt);
        }
        
        System.gc();
        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        latch.countDown();
        
        threads.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        return (afterMemory - beforeMemory) / 1000;
    }
    
    private long measurePlatformThreadMemory() {
        Runtime runtime = Runtime.getRuntime();
        System.gc();
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
        
        List<Thread> threads = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(100);
        
        for (int i = 0; i < 100; i++) {
            Thread pt = Thread.ofPlatform().start(() -> {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads.add(pt);
        }
        
        System.gc();
        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        latch.countDown();
        
        threads.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        return (afterMemory - beforeMemory) / 100;
    }
}
```

### Structured Concurrency Advanced Patterns

**Hierarchical Task Management:**
```java
// Advanced structured concurrency patterns
public class AdvancedStructuredConcurrency {
    
    // Hierarchical task decomposition with error propagation
    public class TaskHierarchy {
        
        public CompletableFuture<ProcessingResult> processDataHierarchically(
                List<DataBatch> batches) {
            
            return CompletableFuture.supplyAsync(() -> {
                try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                    
                    // Level 1: Batch processing
                    List<StructuredTaskScope.Subtask<BatchResult>> batchTasks = 
                        batches.stream()
                            .map(batch -> scope.fork(() -> processBatch(batch)))
                            .toList();
                    
                    scope.join();           // Wait for all tasks
                    scope.throwIfFailed();  // Propagate any failures
                    
                    // Level 2: Aggregation
                    List<BatchResult> batchResults = batchTasks.stream()
                        .map(StructuredTaskScope.Subtask::get)
                        .toList();
                    
                    return aggregateResults(batchResults);
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException("Processing failed", e);
                }
            });
        }
        
        private BatchResult processBatch(DataBatch batch) {
            // Nested structured concurrency for batch processing
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                var validationTask = scope.fork(() -> validateBatch(batch));
                var transformationTask = scope.fork(() -> transformBatch(batch));
                var enrichmentTask = scope.fork(() -> enrichBatch(batch));
                
                scope.join();
                scope.throwIfFailed();
                
                return new BatchResult(
                    validationTask.get(),
                    transformationTask.get(),
                    enrichmentTask.get()
                );
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Batch processing failed", e);
            }
        }
    }
    
    // Custom structured concurrency scope for specific patterns
    public class CustomScope extends StructuredTaskScope<Object> {
        private final List<Exception> exceptions = new ArrayList<>();
        private final AtomicInteger completedTasks = new AtomicInteger(0);
        private final int totalTasks;
        
        public CustomScope(int totalTasks) {
            this.totalTasks = totalTasks;
        }
        
        @Override
        protected void handleComplete(Subtask<? extends Object> subtask) {
            if (subtask.state() == Subtask.State.SUCCESS) {
                int completed = completedTasks.incrementAndGet();
                System.out.printf("Task completed: %d/%d%n", completed, totalTasks);
            } else if (subtask.state() == Subtask.State.FAILED) {
                synchronized (exceptions) {
                    exceptions.add((Exception) subtask.exception());
                }
            }
        }
        
        public void joinAndHandleErrors() throws InterruptedException {
            super.join();
            if (!exceptions.isEmpty()) {
                // Custom error handling strategy
                handleMultipleExceptions(exceptions);
            }
        }
        
        private void handleMultipleExceptions(List<Exception> exceptions) {
            // Categorize and handle different types of exceptions
            var criticalErrors = exceptions.stream()
                .filter(this::isCriticalError)
                .toList();
            
            var retryableErrors = exceptions.stream()
                .filter(this::isRetryableError)
                .toList();
            
            if (!criticalErrors.isEmpty()) {
                throw new RuntimeException("Critical errors occurred", 
                    criticalErrors.get(0));
            }
            
            if (!retryableErrors.isEmpty()) {
                System.out.printf("Warning: %d retryable errors occurred%n", 
                    retryableErrors.size());
            }
        }
    }
}
```

### High-Scale Virtual Thread Management

**Million Virtual Thread Architecture:**
```java
// Handling millions of virtual threads efficiently
public class MillionVirtualThreads {
    
    private final ExecutorService virtualExecutor = 
        Executors.newVirtualThreadPerTaskExecutor();
    
    public void simulateMillionConnections() {
        int connectionCount = 1_000_000;
        CountDownLatch completionLatch = new CountDownLatch(connectionCount);
        AtomicInteger activeConnections = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        // Create million virtual threads simulating connections
        for (int i = 0; i < connectionCount; i++) {
            final int connectionId = i;
            
            virtualExecutor.submit(() -> {
                try {
                    activeConnections.incrementAndGet();
                    simulateConnection(connectionId);
                } finally {
                    activeConnections.decrementAndGet();
                    completionLatch.countDown();
                }
            });
            
            // Report progress periodically
            if (i % 100_000 == 0) {
                System.out.printf("Submitted %d connections, active: %d%n", 
                    i, activeConnections.get());
            }
        }
        
        try {
            completionLatch.await();
            long endTime = System.currentTimeMillis();
            
            System.out.printf("Processed %d connections in %d ms%n", 
                connectionCount, endTime - startTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void simulateConnection(int connectionId) {
        try {
            // Simulate I/O-bound operation
            Thread.sleep(Duration.ofMillis(100 + (connectionId % 1000)));
            
            // Simulate some processing
            processRequest(connectionId);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void processRequest(int connectionId) {
        // Simulate request processing with potential blocking operations
        if (connectionId % 10000 == 0) {
            System.out.printf("Processing connection %d on thread %s%n", 
                connectionId, Thread.currentThread().getName());
        }
    }
}
```

**Virtual Thread Pool Management:**
```java
// Advanced virtual thread pool patterns
public class VirtualThreadPoolManager {
    
    // Custom virtual thread pool with resource limits
    public static class BoundedVirtualThreadPool implements ExecutorService {
        private final Semaphore permits;
        private final ExecutorService virtualExecutor;
        private volatile boolean shutdown = false;
        
        public BoundedVirtualThreadPool(int maxConcurrency) {
            this.permits = new Semaphore(maxConcurrency);
            this.virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
        }
        
        @Override
        public void execute(Runnable command) {
            if (shutdown) {
                throw new RejectedExecutionException("Pool is shut down");
            }
            
            virtualExecutor.execute(() -> {
                try {
                    permits.acquire();
                    command.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    permits.release();
                }
            });
        }
        
        @Override
        public <T> Future<T> submit(Callable<T> task) {
            if (shutdown) {
                throw new RejectedExecutionException("Pool is shut down");
            }
            
            CompletableFuture<T> future = new CompletableFuture<>();
            
            execute(() -> {
                try {
                    T result = task.call();
                    future.complete(result);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
            
            return future;
        }
        
        @Override
        public void shutdown() {
            shutdown = true;
            virtualExecutor.shutdown();
        }
        
        @Override
        public boolean isShutdown() {
            return shutdown;
        }
        
        public int availablePermits() {
            return permits.availablePermits();
        }
    }
    
    // Adaptive thread pool that adjusts based on system load
    public static class AdaptiveVirtualThreadPool {
        private final AtomicInteger currentLimit = new AtomicInteger(1000);
        private final BoundedVirtualThreadPool pool;
        private final ScheduledExecutorService monitor;
        
        public AdaptiveVirtualThreadPool() {
            this.pool = new BoundedVirtualThreadPool(currentLimit.get());
            this.monitor = Executors.newScheduledThreadPool(1);
            startMonitoring();
        }
        
        private void startMonitoring() {
            monitor.scheduleAtFixedRate(this::adjustPoolSize, 1, 1, TimeUnit.MINUTES);
        }
        
        private void adjustPoolSize() {
            // Monitor system resources and adjust pool size
            double cpuUsage = getSystemCpuUsage();
            long freeMemory = Runtime.getRuntime().freeMemory();
            long totalMemory = Runtime.getRuntime().totalMemory();
            double memoryUsage = 1.0 - (double) freeMemory / totalMemory;
            
            int newLimit = calculateOptimalLimit(cpuUsage, memoryUsage);
            
            if (newLimit != currentLimit.get()) {
                System.out.printf("Adjusting pool size from %d to %d " +
                    "(CPU: %.2f%%, Memory: %.2f%%)%n", 
                    currentLimit.get(), newLimit, cpuUsage * 100, memoryUsage * 100);
                
                currentLimit.set(newLimit);
                // Note: In a real implementation, you'd need to dynamically
                // adjust the semaphore permits
            }
        }
        
        private int calculateOptimalLimit(double cpuUsage, double memoryUsage) {
            // Simple heuristic - adjust based on resource usage
            int baseLimit = 1000;
            
            if (cpuUsage > 0.8 || memoryUsage > 0.9) {
                return Math.max(100, baseLimit / 2);
            } else if (cpuUsage < 0.5 && memoryUsage < 0.7) {
                return Math.min(10000, baseLimit * 2);
            }
            
            return baseLimit;
        }
        
        private double getSystemCpuUsage() {
            // Simplified CPU usage calculation
            return Math.random() * 0.5 + 0.3; // Mock implementation
        }
        
        public void execute(Runnable command) {
            pool.execute(command);
        }
        
        public <T> Future<T> submit(Callable<T> task) {
            return pool.submit(task);
        }
    }
}
```

### Virtual Thread Monitoring and Debugging

**Comprehensive Monitoring System:**
```java
// Advanced monitoring for virtual threads
public class VirtualThreadMonitor {
    
    private final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    private final ScheduledExecutorService monitoringExecutor = 
        Executors.newScheduledThreadPool(1);
    
    public void startMonitoring() {
        monitoringExecutor.scheduleAtFixedRate(
            this::collectMetrics, 0, 5, TimeUnit.SECONDS);
    }
    
    private void collectMetrics() {
        // Collect JVM thread metrics
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        
        System.out.printf("=== Virtual Thread Metrics (Time: %s) ===%n", 
            Instant.now());
        System.out.printf("Total started threads: %d%n", 
            threadBean.getTotalStartedThreadCount());
        System.out.printf("Current thread count: %d%n", 
            threadBean.getThreadCount());
        System.out.printf("Peak thread count: %d%n", 
            threadBean.getPeakThreadCount());
        System.out.printf("Daemon thread count: %d%n", 
            threadBean.getDaemonThreadCount());
        
        // Memory usage
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        
        System.out.printf("Heap memory used: %d MB%n", 
            heapUsage.getUsed() / (1024 * 1024));
        System.out.printf("Heap memory max: %d MB%n", 
            heapUsage.getMax() / (1024 * 1024));
        
        // GC metrics
        collectGCMetrics();
        
        System.out.println("==========================================");
    }
    
    private void collectGCMetrics() {
        List<GarbageCollectorMXBean> gcBeans = 
            ManagementFactory.getGarbageCollectorMXBeans();
        
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            System.out.printf("GC %s: Collections=%d, Time=%d ms%n",
                gcBean.getName(),
                gcBean.getCollectionCount(),
                gcBean.getCollectionTime());
        }
    }
    
    // Custom virtual thread tracker
    public static class VirtualThreadTracker {
        private final Map<String, ThreadMetrics> metrics = 
            new ConcurrentHashMap<>();
        
        public void trackThread(String threadName, Runnable task) {
            long startTime = System.nanoTime();
            
            try {
                task.run();
            } finally {
                long endTime = System.nanoTime();
                long duration = endTime - startTime;
                
                metrics.compute(threadName, (name, existing) -> {
                    if (existing == null) {
                        return new ThreadMetrics(1, duration, duration, duration);
                    } else {
                        return new ThreadMetrics(
                            existing.count + 1,
                            Math.min(existing.minDuration, duration),
                            Math.max(existing.maxDuration, duration),
                            existing.totalDuration + duration
                        );
                    }
                });
            }
        }
        
        public void printStatistics() {
            System.out.println("=== Virtual Thread Statistics ===");
            metrics.forEach((name, metric) -> {
                double avgDuration = (double) metric.totalDuration / metric.count;
                System.out.printf("Thread: %s, Count: %d, Avg: %.2f ms, " +
                    "Min: %.2f ms, Max: %.2f ms%n",
                    name, metric.count,
                    avgDuration / 1_000_000,
                    metric.minDuration / 1_000_000.0,
                    metric.maxDuration / 1_000_000.0);
            });
        }
        
        private record ThreadMetrics(
            int count,
            long minDuration,
            long maxDuration,
            long totalDuration
        ) {}
    }
}
```

## Practical Implementation

### Enterprise-Scale Virtual Thread Application

**High-Performance Web Server:**
```java
// Enterprise-grade virtual thread web server
public class VirtualThreadWebServer {
    
    private final int port;
    private final BoundedVirtualThreadPool threadPool;
    private final VirtualThreadMonitor monitor;
    private volatile boolean running = false;
    
    public VirtualThreadWebServer(int port, int maxConcurrency) {
        this.port = port;
        this.threadPool = new BoundedVirtualThreadPool(maxConcurrency);
        this.monitor = new VirtualThreadMonitor();
    }
    
    public void start() throws IOException {
        running = true;
        monitor.startMonitoring();
        
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.printf("Virtual Thread Web Server started on port %d%n", port);
            
            while (running) {
                Socket clientSocket = serverSocket.accept();
                
                // Handle each connection in a virtual thread
                threadPool.execute(() -> handleConnection(clientSocket));
            }
        }
    }
    
    private void handleConnection(Socket clientSocket) {
        try (var input = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
             var output = new PrintWriter(
                clientSocket.getOutputStream(), true)) {
            
            // Read HTTP request
            String requestLine = input.readLine();
            if (requestLine == null) return;
            
            // Parse request
            String[] parts = requestLine.split(" ");
            if (parts.length >= 2) {
                String method = parts[0];
                String path = parts[1];
                
                // Route request
                HttpResponse response = routeRequest(method, path);
                
                // Send response
                sendResponse(output, response);
            }
            
        } catch (IOException e) {
            System.err.printf("Error handling connection: %s%n", e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Log error
            }
        }
    }
    
    private HttpResponse routeRequest(String method, String path) {
        // Simulate various endpoints with different processing times
        return switch (path) {
            case "/fast" -> {
                // Quick response
                yield new HttpResponse(200, "Fast response");
            }
            case "/slow" -> {
                // Simulate slow I/O operation
                try {
                    Thread.sleep(1000);
                    yield new HttpResponse(200, "Slow response after 1s");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    yield new HttpResponse(500, "Interrupted");
                }
            }
            case "/cpu-intensive" -> {
                // CPU-intensive operation
                long result = fibonacci(40);
                yield new HttpResponse(200, "Fibonacci result: " + result);
            }
            case "/database" -> {
                // Simulate database operation
                simulateDatabaseQuery();
                yield new HttpResponse(200, "Database query completed");
            }
            default -> new HttpResponse(404, "Not Found");
        };
    }
    
    private void sendResponse(PrintWriter output, HttpResponse response) {
        output.printf("HTTP/1.1 %d %s\r\n", 
            response.statusCode, getStatusMessage(response.statusCode));
        output.printf("Content-Length: %d\r\n", response.body.length());
        output.println("Content-Type: text/plain");
        output.println();
        output.println(response.body);
    }
    
    private String getStatusMessage(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            default -> "Unknown";
        };
    }
    
    private long fibonacci(int n) {
        if (n <= 1) return n;
        return fibonacci(n - 1) + fibonacci(n - 2);
    }
    
    private void simulateDatabaseQuery() {
        try {
            // Simulate database I/O
            Thread.sleep(Duration.ofMillis(50 + (int)(Math.random() * 200)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private record HttpResponse(int statusCode, String body) {}
    
    public void stop() {
        running = false;
        threadPool.shutdown();
        monitor.shutdown();
    }
}
```

### Load Testing Framework

**Virtual Thread Load Tester:**
```java
// Load testing framework using virtual threads
public class VirtualThreadLoadTester {
    
    public static class LoadTestConfig {
        public final int virtualUsers;
        public final Duration testDuration;
        public final String targetUrl;
        public final int requestsPerSecond;
        
        public LoadTestConfig(int virtualUsers, Duration testDuration, 
                            String targetUrl, int requestsPerSecond) {
            this.virtualUsers = virtualUsers;
            this.testDuration = testDuration;
            this.targetUrl = targetUrl;
            this.requestsPerSecond = requestsPerSecond;
        }
    }
    
    public static class LoadTestResults {
        private final AtomicLong totalRequests = new AtomicLong(0);
        private final AtomicLong successfulRequests = new AtomicLong(0);
        private final AtomicLong failedRequests = new AtomicLong(0);
        private final LongAdder totalResponseTime = new LongAdder();
        private final Map<Integer, AtomicLong> statusCodes = new ConcurrentHashMap<>();
        
        public void recordRequest(int statusCode, long responseTimeMs) {
            totalRequests.incrementAndGet();
            totalResponseTime.add(responseTimeMs);
            
            statusCodes.computeIfAbsent(statusCode, k -> new AtomicLong(0))
                      .incrementAndGet();
            
            if (statusCode >= 200 && statusCode < 300) {
                successfulRequests.incrementAndGet();
            } else {
                failedRequests.incrementAndGet();
            }
        }
        
        public void printResults() {
            long total = totalRequests.get();
            long successful = successfulRequests.get();
            long failed = failedRequests.get();
            double avgResponseTime = total > 0 ? 
                (double) totalResponseTime.sum() / total : 0;
            
            System.out.println("=== Load Test Results ===");
            System.out.printf("Total requests: %d%n", total);
            System.out.printf("Successful requests: %d (%.2f%%)%n", 
                successful, (double) successful / total * 100);
            System.out.printf("Failed requests: %d (%.2f%%)%n", 
                failed, (double) failed / total * 100);
            System.out.printf("Average response time: %.2f ms%n", avgResponseTime);
            
            System.out.println("Status code distribution:");
            statusCodes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> 
                    System.out.printf("  %d: %d requests%n", 
                        entry.getKey(), entry.getValue().get()));
        }
    }
    
    public LoadTestResults runLoadTest(LoadTestConfig config) {
        LoadTestResults results = new LoadTestResults();
        
        System.out.printf("Starting load test: %d virtual users, %s duration%n",
            config.virtualUsers, config.testDuration);
        
        long startTime = System.currentTimeMillis();
        long endTime = startTime + config.testDuration.toMillis();
        
        // Create virtual threads for each virtual user
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            for (int i = 0; i < config.virtualUsers; i++) {
                final int userId = i;
                scope.fork(() -> {
                    runVirtualUser(userId, config, results, endTime);
                    return null;
                });
            }
            
            scope.join();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Load test interrupted");
        } catch (ExecutionException e) {
            System.err.printf("Load test failed: %s%n", e.getMessage());
        }
        
        return results;
    }
    
    private void runVirtualUser(int userId, LoadTestConfig config, 
                               LoadTestResults results, long endTime) {
        
        HttpClient client = HttpClient.newHttpClient();
        int requestDelay = config.requestsPerSecond > 0 ? 
            1000 / config.requestsPerSecond : 1000;
        
        while (System.currentTimeMillis() < endTime) {
            try {
                long requestStart = System.currentTimeMillis();
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.targetUrl))
                    .timeout(Duration.ofSeconds(30))
                    .build();
                
                HttpResponse<String> response = client.send(request, 
                    HttpResponse.BodyHandlers.ofString());
                
                long requestEnd = System.currentTimeMillis();
                long responseTime = requestEnd - requestStart;
                
                results.recordRequest(response.statusCode(), responseTime);
                
                // Rate limiting
                if (requestDelay > 0) {
                    Thread.sleep(requestDelay);
                }
                
            } catch (Exception e) {
                results.recordRequest(0, 0); // Record as failure
            }
        }
    }
    
    public static void main(String[] args) {
        VirtualThreadLoadTester tester = new VirtualThreadLoadTester();
        
        LoadTestConfig config = new LoadTestConfig(
            1000,                           // 1000 virtual users
            Duration.ofMinutes(2),          // 2 minute test
            "http://localhost:8080/fast",   // Target URL
            10                              // 10 requests per second per user
        );
        
        LoadTestResults results = tester.runLoadTest(config);
        results.printResults();
    }
}
```

## Performance Analysis and Optimization

### Benchmarking Virtual vs Platform Threads

**Comprehensive Performance Comparison:**
```java
// Performance benchmarking framework
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.OPERATIONS_PER_SECOND)
@State(Scope.Benchmark)
public class VirtualThreadBenchmark {
    
    private static final int THREAD_COUNT = 10_000;
    private static final int WORK_DURATION_MS = 100;
    
    @Benchmark
    public void platformThreads() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        
        for (int i = 0; i < THREAD_COUNT; i++) {
            Thread.ofPlatform().start(() -> {
                try {
                    doWork();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
    }
    
    @Benchmark
    public void virtualThreads() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        
        for (int i = 0; i < THREAD_COUNT; i++) {
            Thread.ofVirtual().start(() -> {
                try {
                    doWork();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
    }
    
    @Benchmark
    public void virtualThreadsWithExecutor() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.submit(() -> {
                    try {
                        doWork();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await();
        }
    }
    
    private void doWork() {
        try {
            // Simulate I/O-bound work
            Thread.sleep(WORK_DURATION_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

## Best Practices and Guidelines

### Virtual Thread Best Practices

1. **Use for I/O-bound tasks**: Virtual threads excel at I/O-bound operations
2. **Avoid CPU-intensive work**: Use platform threads for CPU-bound tasks
3. **Don't pool virtual threads**: Create them as needed
4. **Use structured concurrency**: Leverage structured task scopes
5. **Monitor resource usage**: Implement comprehensive monitoring
6. **Handle interruption properly**: Always check for interruption
7. **Avoid thread-local variables**: Use with caution as they can leak memory

### Common Pitfalls and Solutions

```java
// Common pitfalls and their solutions
public class VirtualThreadPitfalls {
    
    // Pitfall 1: Using synchronized blocks (pinning)
    // BAD:
    public synchronized void badSynchronized() {
        // This pins the virtual thread to carrier thread
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // GOOD: Use ReentrantLock instead
    private final ReentrantLock lock = new ReentrantLock();
    
    public void goodLocking() {
        lock.lock();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }
    
    // Pitfall 2: CPU-intensive work in virtual threads
    // BAD:
    public void badCpuIntensive() {
        Thread.ofVirtual().start(() -> {
            // CPU-intensive calculation
            for (int i = 0; i < 1_000_000_000; i++) {
                Math.sqrt(i);
            }
        });
    }
    
    // GOOD: Use platform thread for CPU work
    public void goodCpuIntensive() {
        Thread.ofPlatform().start(() -> {
            for (int i = 0; i < 1_000_000_000; i++) {
                Math.sqrt(i);
            }
        });
    }
    
    // Pitfall 3: ThreadLocal memory leaks
    private static final ThreadLocal<ExpensiveObject> threadLocal = 
        new ThreadLocal<>();
    
    // BAD: Using ThreadLocal without cleanup
    public void badThreadLocal() {
        threadLocal.set(new ExpensiveObject());
        // No cleanup - memory leak!
    }
    
    // GOOD: Proper ThreadLocal usage
    public void goodThreadLocal() {
        try {
            threadLocal.set(new ExpensiveObject());
            // Use the thread-local value
        } finally {
            threadLocal.remove(); // Always clean up
        }
    }
    
    private static class ExpensiveObject {
        private final byte[] data = new byte[1024 * 1024]; // 1MB
    }
}
```

## Exercises and Practice Problems

### Exercise 1: Virtual Thread Web Crawler
Create a web crawler that uses virtual threads to crawl websites concurrently. Implement rate limiting, error handling, and monitoring.

### Exercise 2: High-Frequency Trading Simulator
Build a trading simulator that processes millions of trades using virtual threads. Include order matching, position tracking, and real-time reporting.

### Exercise 3: Structured Concurrency File Processor
Implement a file processing system that uses structured concurrency to process large files in parallel while maintaining data consistency.

## Assessment Criteria

### Technical Implementation (40%)
- Correct use of virtual threads and structured concurrency
- Proper resource management and error handling
- Performance optimization and monitoring
- Scalability and efficiency

### Code Quality (30%)
- Clean, readable, and well-documented code
- Proper exception handling and resource cleanup
- Following virtual thread best practices
- Avoiding common pitfalls

### Performance Analysis (20%)
- Comprehensive benchmarking and profiling
- Memory usage analysis
- Scalability testing
- Performance optimization strategies

### Innovation and Problem-Solving (10%)
- Creative use of virtual thread features
- Novel approaches to concurrency problems
- Integration with other Java technologies
- Real-world application scenarios

## Summary

Day 155 provided an in-depth exploration of Project Loom's virtual threads at enterprise scale. Students learned about virtual thread internals, structured concurrency patterns, million-thread architectures, monitoring systems, and performance optimization techniques. The practical implementations demonstrate how to build scalable, high-performance concurrent applications that can handle massive workloads efficiently.

Key takeaways:
- Virtual threads enable unprecedented concurrency scales
- Structured concurrency provides safe and maintainable concurrent programming
- Proper monitoring and resource management are crucial at scale
- Understanding virtual thread internals helps optimize performance
- Best practices prevent common pitfalls and ensure robust applications

This foundation prepares students for the next lesson on GraalVM advanced features and native compilation techniques.