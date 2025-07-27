# Day 92: Modern Java Platform - Project Loom & Virtual Threads

## Learning Objectives
- Understand Project Loom and virtual threads fundamentals
- Master structured concurrency and scoped values
- Implement high-performance concurrent applications
- Compare virtual threads vs traditional threads performance
- Build scalable server applications with virtual threads

## 1. Project Loom Fundamentals

### What is Project Loom?

Project Loom introduces lightweight virtual threads to Java, enabling massive concurrency without the traditional overhead of platform threads.

### Virtual Threads vs Platform Threads

```java
package com.javajourney.loom;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;

public class ThreadComparison {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Platform Threads Performance ===");
        measurePlatformThreads();
        
        System.out.println("\n=== Virtual Threads Performance ===");
        measureVirtualThreads();
    }
    
    private static void measurePlatformThreads() throws InterruptedException {
        Instant start = Instant.now();
        
        try (var executor = Executors.newFixedThreadPool(200)) {
            for (int i = 0; i < 10000; i++) {
                final int taskId = i;
                executor.submit(() -> {
                    try {
                        Thread.sleep(1000); // Simulate I/O
                        System.out.printf("Platform task %d completed%n", taskId);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        }
        
        Duration duration = Duration.between(start, Instant.now());
        System.out.printf("Platform threads completed in: %d ms%n", duration.toMillis());
    }
    
    private static void measureVirtualThreads() throws InterruptedException {
        Instant start = Instant.now();
        
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 10000; i++) {
                final int taskId = i;
                executor.submit(() -> {
                    try {
                        Thread.sleep(Duration.ofSeconds(1)); // Simulate I/O
                        System.out.printf("Virtual task %d completed%n", taskId);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        }
        
        Duration duration = Duration.between(start, Instant.now());
        System.out.printf("Virtual threads completed in: %d ms%n", duration.toMillis());
    }
}
```

### Virtual Thread Creation and Management

```java
package com.javajourney.loom;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class VirtualThreadCreation {
    
    public static void demonstrateVirtualThreadCreation() {
        // Method 1: Thread.ofVirtual()
        Thread virtualThread1 = Thread.ofVirtual()
            .name("virtual-worker-1")
            .start(() -> {
                System.out.println("Running in: " + Thread.currentThread());
                performWork();
            });
        
        // Method 2: Thread.startVirtualThread()
        Thread virtualThread2 = Thread.startVirtualThread(() -> {
            System.out.println("Running in: " + Thread.currentThread());
            performWork();
        });
        
        // Method 3: Virtual thread executor
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 1000; i++) {
                final int taskId = i;
                executor.submit(() -> {
                    System.out.printf("Task %d running in: %s%n", 
                        taskId, Thread.currentThread());
                    performWork();
                });
            }
        }
        
        // Method 4: Custom virtual thread factory
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
            .name("custom-virtual-", 0)
            .factory();
        
        try (ExecutorService customExecutor = Executors.newThreadPerTaskExecutor(virtualThreadFactory)) {
            for (int i = 0; i < 100; i++) {
                customExecutor.submit(() -> {
                    System.out.println("Custom virtual thread: " + Thread.currentThread());
                    performWork();
                });
            }
        }
    }
    
    private static void performWork() {
        try {
            // Simulate I/O bound work
            Thread.sleep(100);
            // Some CPU work
            double result = Math.random() * 1000;
            System.out.printf("Work completed with result: %.2f%n", result);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

## 2. Structured Concurrency

### Understanding Structured Concurrency

Structured concurrency treats groups of related concurrent tasks as a single unit of work, providing better error handling and resource management.

```java
package com.javajourney.loom;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public class StructuredConcurrencyExample {
    
    public static void main(String[] args) {
        try {
            // Example 1: All tasks must succeed
            String result1 = handleAllTasksSucceed();
            System.out.println("All tasks result: " + result1);
            
            // Example 2: First successful result
            String result2 = handleFirstSuccess();
            System.out.println("First success result: " + result2);
            
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    // All subtasks must succeed
    public static String handleAllTasksSucceed() throws InterruptedException, ExecutionException {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            // Launch multiple subtasks
            var userTask = scope.fork(() -> fetchUserData("user123"));
            var profileTask = scope.fork(() -> fetchUserProfile("user123"));
            var preferencesTask = scope.fork(() -> fetchUserPreferences("user123"));
            
            // Wait for all tasks to complete or fail
            scope.join();           // Wait for all subtasks
            scope.throwIfFailed();  // Throw if any failed
            
            // All tasks succeeded, collect results
            return String.format("User: %s, Profile: %s, Preferences: %s",
                userTask.get(), profileTask.get(), preferencesTask.get());
        }
    }
    
    // Return first successful result
    public static String handleFirstSuccess() throws InterruptedException, ExecutionException {
        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<String>()) {
            
            // Launch multiple alternative approaches
            scope.fork(() -> fetchFromPrimaryService());
            scope.fork(() -> fetchFromSecondaryService());
            scope.fork(() -> fetchFromBackupService());
            
            // Wait for first success
            scope.join();
            
            return scope.result();
        }
    }
    
    // Simulated service calls
    private static String fetchUserData(String userId) {
        simulateNetworkCall();
        return "UserData[" + userId + "]";
    }
    
    private static String fetchUserProfile(String userId) {
        simulateNetworkCall();
        return "Profile[" + userId + "]";
    }
    
    private static String fetchUserPreferences(String userId) {
        simulateNetworkCall();
        return "Preferences[" + userId + "]";
    }
    
    private static String fetchFromPrimaryService() {
        simulateNetworkCall(Duration.ofMillis(ThreadLocalRandom.current().nextInt(100, 500)));
        return "Primary Service Response";
    }
    
    private static String fetchFromSecondaryService() {
        simulateNetworkCall(Duration.ofMillis(ThreadLocalRandom.current().nextInt(200, 800)));
        return "Secondary Service Response";
    }
    
    private static String fetchFromBackupService() {
        simulateNetworkCall(Duration.ofMillis(ThreadLocalRandom.current().nextInt(300, 1000)));
        return "Backup Service Response";
    }
    
    private static void simulateNetworkCall() {
        simulateNetworkCall(Duration.ofMillis(100));
    }
    
    private static void simulateNetworkCall(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Network call interrupted", e);
        }
    }
}
```

### Advanced Structured Concurrency Patterns

```java
package com.javajourney.loom;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeoutException;

public class AdvancedStructuredConcurrency {
    
    // Custom task scope with timeout and resource management
    public static class TimedTaskScope<T> extends StructuredTaskScope<T> {
        private final Duration timeout;
        private final Instant deadline;
        
        public TimedTaskScope(Duration timeout) {
            super();
            this.timeout = timeout;
            this.deadline = Instant.now().plus(timeout);
        }
        
        @Override
        protected void handleComplete(Subtask<? extends T> subtask) {
            if (Instant.now().isAfter(deadline)) {
                shutdown(); // Cancel remaining tasks if timeout exceeded
            }
            super.handleComplete(subtask);
        }
        
        public void joinWithTimeout() throws InterruptedException, TimeoutException {
            try {
                join(deadline);
            } catch (InterruptedException e) {
                shutdown();
                throw e;
            }
        }
    }
    
    // Parallel processing with structured concurrency
    public static List<String> processDataInParallel(List<String> data) 
            throws InterruptedException, ExecutionException {
        
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            // Create subtasks for each data item
            var subtasks = data.stream()
                .map(item -> scope.fork(() -> processItem(item)))
                .toList();
            
            // Wait for all to complete
            scope.join();
            scope.throwIfFailed();
            
            // Collect results
            return subtasks.stream()
                .map(task -> {
                    try {
                        return task.get();
                    } catch (ExecutionException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
        }
    }
    
    // Aggregation pattern with structured concurrency
    public static AggregatedResult aggregateFromMultipleSources(String key) 
            throws InterruptedException, ExecutionException {
        
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            var source1Task = scope.fork(() -> fetchFromSource1(key));
            var source2Task = scope.fork(() -> fetchFromSource2(key));
            var source3Task = scope.fork(() -> fetchFromSource3(key));
            
            scope.join();
            scope.throwIfFailed();
            
            return new AggregatedResult(
                source1Task.get(),
                source2Task.get(),
                source3Task.get()
            );
        }
    }
    
    // Fan-out/Fan-in pattern
    public static String fanOutFanIn(List<String> inputs) 
            throws InterruptedException, ExecutionException {
        
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            // Fan-out: Process each input in parallel
            var processingTasks = inputs.stream()
                .map(input -> scope.fork(() -> intensiveProcessing(input)))
                .toList();
            
            scope.join();
            scope.throwIfFailed();
            
            // Fan-in: Combine results
            var results = processingTasks.stream()
                .map(task -> {
                    try {
                        return task.get();
                    } catch (ExecutionException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
            
            return combineResults(results);
        }
    }
    
    // Supporting methods
    private static String processItem(String item) {
        try {
            Thread.sleep(100); // Simulate processing
            return "Processed: " + item.toUpperCase();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
    
    private static String fetchFromSource1(String key) {
        try {
            Thread.sleep(200);
            return "Source1[" + key + "]";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
    
    private static String fetchFromSource2(String key) {
        try {
            Thread.sleep(150);
            return "Source2[" + key + "]";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
    
    private static String fetchFromSource3(String key) {
        try {
            Thread.sleep(300);
            return "Source3[" + key + "]";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
    
    private static String intensiveProcessing(String input) {
        try {
            Thread.sleep(500); // Simulate intensive work
            return input.toLowerCase().replace(" ", "_");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
    
    private static String combineResults(List<String> results) {
        return String.join(" | ", results);
    }
    
    // Data classes
    public record AggregatedResult(String source1, String source2, String source3) {}
}
```

## 3. Scoped Values

### Understanding Scoped Values

Scoped values provide a better alternative to ThreadLocal for sharing immutable data across a thread's execution scope.

```java
package com.javajourney.loom;

import java.util.concurrent.StructuredTaskScope;

public class ScopedValuesExample {
    
    // Define scoped values
    public static final ScopedValue<String> USER_ID = ScopedValue.newInstance();
    public static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();
    public static final ScopedValue<SecurityContext> SECURITY_CONTEXT = ScopedValue.newInstance();
    
    public static void main(String[] args) throws Exception {
        // Bind scoped values and execute
        ScopedValue.where(USER_ID, "user123")
            .where(REQUEST_ID, "req-456")
            .where(SECURITY_CONTEXT, new SecurityContext("admin", "full-access"))
            .run(() -> {
                try {
                    handleUserRequest();
                } catch (Exception e) {
                    System.err.println("Error handling request: " + e.getMessage());
                }
            });
    }
    
    private static void handleUserRequest() throws Exception {
        System.out.println("Processing request for user: " + USER_ID.get());
        System.out.println("Request ID: " + REQUEST_ID.get());
        System.out.println("Security context: " + SECURITY_CONTEXT.get());
        
        // Structured concurrency with scoped values
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            var userDataTask = scope.fork(() -> fetchUserData());
            var auditTask = scope.fork(() -> auditUserActivity());
            var notificationTask = scope.fork(() -> sendNotification());
            
            scope.join();
            scope.throwIfFailed();
            
            System.out.println("All tasks completed successfully");
            System.out.println("User data: " + userDataTask.get());
            System.out.println("Audit result: " + auditTask.get());
            System.out.println("Notification result: " + notificationTask.get());
        }
    }
    
    private static String fetchUserData() {
        // Scoped values are automatically inherited by child threads
        String userId = USER_ID.get();
        String requestId = REQUEST_ID.get();
        SecurityContext context = SECURITY_CONTEXT.get();
        
        System.out.printf("fetchUserData: userId=%s, requestId=%s, context=%s%n", 
            userId, requestId, context);
        
        // Simulate database access
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        
        return "UserData{id=" + userId + ", name='John Doe', email='john@example.com'}";
    }
    
    private static String auditUserActivity() {
        String userId = USER_ID.get();
        String requestId = REQUEST_ID.get();
        
        System.out.printf("auditUserActivity: userId=%s, requestId=%s%n", userId, requestId);
        
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        
        return "AuditEntry{userId=" + userId + ", action='DATA_ACCESS', timestamp=" + 
               System.currentTimeMillis() + "}";
    }
    
    private static String sendNotification() {
        String userId = USER_ID.get();
        SecurityContext context = SECURITY_CONTEXT.get();
        
        System.out.printf("sendNotification: userId=%s, role=%s%n", userId, context.role());
        
        try {
            Thread.sleep(75);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        
        return "Notification sent to user " + userId;
    }
    
    // Nested scoped value binding
    public static void demonstrateNestedScoping() {
        ScopedValue.where(USER_ID, "user123").run(() -> {
            System.out.println("Outer scope - User ID: " + USER_ID.get());
            
            // Nested binding overrides outer value
            ScopedValue.where(USER_ID, "user456").run(() -> {
                System.out.println("Inner scope - User ID: " + USER_ID.get());
            });
            
            // Back to outer scope value
            System.out.println("Back to outer scope - User ID: " + USER_ID.get());
        });
    }
    
    // Security context record
    public record SecurityContext(String role, String permissions) {}
}
```

## 4. Virtual Thread Web Server

### High-Performance Web Server with Virtual Threads

```java
package com.javajourney.loom;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;

public class VirtualThreadWebServer {
    
    private static final int PORT = 8080;
    private static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();
    private static final ScopedValue<Instant> REQUEST_START_TIME = ScopedValue.newInstance();
    
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        
        // Use virtual thread executor for handling requests
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        
        // Define endpoints
        server.createContext("/", new HomeHandler());
        server.createContext("/user", new UserHandler());
        server.createContext("/data", new DataHandler());
        server.createContext("/heavy", new HeavyProcessingHandler());
        
        server.start();
        System.out.println("Virtual Thread Web Server started on port " + PORT);
        System.out.println("Try: http://localhost:" + PORT + "/");
    }
    
    // Base handler with request tracking
    static abstract class BaseHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestId = "req-" + System.nanoTime();
            Instant startTime = Instant.now();
            
            ScopedValue.where(REQUEST_ID, requestId)
                .where(REQUEST_START_TIME, startTime)
                .run(() -> {
                    try {
                        handleRequest(exchange);
                    } catch (Exception e) {
                        try {
                            sendErrorResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
                        } catch (IOException ioException) {
                            System.err.println("Error sending error response: " + ioException.getMessage());
                        }
                    } finally {
                        logRequest(exchange);
                    }
                });
        }
        
        protected abstract void handleRequest(HttpExchange exchange) throws Exception;
        
        protected void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
        
        protected void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
            sendResponse(exchange, statusCode, message);
        }
        
        private void logRequest(HttpExchange exchange) {
            Duration duration = Duration.between(REQUEST_START_TIME.get(), Instant.now());
            System.out.printf("[%s] %s %s - %d ms - Thread: %s%n",
                REQUEST_ID.get(),
                exchange.getRequestMethod(),
                exchange.getRequestURI(),
                duration.toMillis(),
                Thread.currentThread().getName()
            );
        }
    }
    
    // Simple home handler
    static class HomeHandler extends BaseHandler {
        @Override
        protected void handleRequest(HttpExchange exchange) throws IOException {
            String response = """
                <html>
                <body>
                    <h1>Virtual Thread Web Server</h1>
                    <p>Request ID: %s</p>
                    <p>Thread: %s</p>
                    <p>Time: %s</p>
                    <ul>
                        <li><a href="/user">User Endpoint</a></li>
                        <li><a href="/data">Data Endpoint</a></li>
                        <li><a href="/heavy">Heavy Processing</a></li>
                    </ul>
                </body>
                </html>
                """.formatted(
                    REQUEST_ID.get(),
                    Thread.currentThread().getName(),
                    Instant.now()
                );
            
            sendResponse(exchange, 200, response);
        }
    }
    
    // User data handler with structured concurrency
    static class UserHandler extends BaseHandler {
        @Override
        protected void handleRequest(HttpExchange exchange) throws Exception {
            String userId = extractUserId(exchange);
            
            // Use structured concurrency to fetch user data
            String userData = fetchUserDataWithStructuredConcurrency(userId);
            
            String response = """
                {
                    "requestId": "%s",
                    "thread": "%s",
                    "userData": %s
                }
                """.formatted(
                    REQUEST_ID.get(),
                    Thread.currentThread().getName(),
                    userData
                );
            
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            sendResponse(exchange, 200, response);
        }
        
        private String extractUserId(HttpExchange exchange) {
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.startsWith("id=")) {
                return query.substring(3);
            }
            return "default-user";
        }
        
        private String fetchUserDataWithStructuredConcurrency(String userId) throws Exception {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                var profileTask = scope.fork(() -> fetchUserProfile(userId));
                var preferencesTask = scope.fork(() -> fetchUserPreferences(userId));
                var activityTask = scope.fork(() -> fetchUserActivity(userId));
                
                scope.join();
                scope.throwIfFailed();
                
                return """
                    {
                        "profile": "%s",
                        "preferences": "%s",
                        "activity": "%s"
                    }
                    """.formatted(
                        profileTask.get(),
                        preferencesTask.get(),
                        activityTask.get()
                    );
            }
        }
        
        private String fetchUserProfile(String userId) {
            simulateIOOperation(Duration.ofMillis(100));
            return "Profile data for " + userId;
        }
        
        private String fetchUserPreferences(String userId) {
            simulateIOOperation(Duration.ofMillis(150));
            return "Preferences for " + userId;
        }
        
        private String fetchUserActivity(String userId) {
            simulateIOOperation(Duration.ofMillis(200));
            return "Recent activity for " + userId;
        }
    }
    
    // Data processing handler
    static class DataHandler extends BaseHandler {
        @Override
        protected void handleRequest(HttpExchange exchange) throws IOException {
            // Simulate data processing
            simulateIOOperation(Duration.ofMillis(300));
            
            String response = """
                {
                    "requestId": "%s",
                    "thread": "%s",
                    "data": "Processed data result",
                    "processingTime": "300ms"
                }
                """.formatted(
                    REQUEST_ID.get(),
                    Thread.currentThread().getName()
                );
            
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            sendResponse(exchange, 200, response);
        }
    }
    
    // Heavy processing handler
    static class HeavyProcessingHandler extends BaseHandler {
        @Override
        protected void handleRequest(HttpExchange exchange) throws IOException {
            // Simulate heavy processing
            simulateIOOperation(Duration.ofSeconds(2));
            
            String response = """
                {
                    "requestId": "%s",
                    "thread": "%s",
                    "result": "Heavy processing completed",
                    "processingTime": "2000ms"
                }
                """.formatted(
                    REQUEST_ID.get(),
                    Thread.currentThread().getName()
                );
            
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            sendResponse(exchange, 200, response);
        }
    }
    
    private static void simulateIOOperation(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Operation interrupted", e);
        }
    }
}
```

## 5. Performance Monitoring and Benchmarks

### Virtual Thread Performance Analysis

```java
package com.javajourney.loom;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class VirtualThreadBenchmark {
    
    public static void main(String[] args) throws InterruptedException {
        int[] taskCounts = {1000, 10000, 100000};
        Duration[] ioDelays = {
            Duration.ofMillis(10),
            Duration.ofMillis(100),
            Duration.ofMillis(1000)
        };
        
        for (int taskCount : taskCounts) {
            for (Duration ioDelay : ioDelays) {
                System.out.printf("%n=== Benchmark: %d tasks, %d ms I/O delay ===%n", 
                    taskCount, ioDelay.toMillis());
                
                benchmarkPlatformThreads(taskCount, ioDelay);
                benchmarkVirtualThreads(taskCount, ioDelay);
                benchmarkVirtualThreadsWithStructuredConcurrency(taskCount, ioDelay);
            }
        }
    }
    
    private static void benchmarkPlatformThreads(int taskCount, Duration ioDelay) 
            throws InterruptedException {
        
        Instant start = Instant.now();
        CountDownLatch latch = new CountDownLatch(taskCount);
        AtomicLong completedTasks = new AtomicLong(0);
        
        // Limit platform threads to avoid resource exhaustion
        int threadPoolSize = Math.min(taskCount, 200);
        
        try (ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize)) {
            for (int i = 0; i < taskCount; i++) {
                executor.submit(() -> {
                    try {
                        Thread.sleep(ioDelay);
                        completedTasks.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await();
        }
        
        Duration duration = Duration.between(start, Instant.now());
        System.out.printf("Platform Threads: %d ms, %d tasks completed%n", 
            duration.toMillis(), completedTasks.get());
    }
    
    private static void benchmarkVirtualThreads(int taskCount, Duration ioDelay) 
            throws InterruptedException {
        
        Instant start = Instant.now();
        CountDownLatch latch = new CountDownLatch(taskCount);
        AtomicLong completedTasks = new AtomicLong(0);
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < taskCount; i++) {
                executor.submit(() -> {
                    try {
                        Thread.sleep(ioDelay);
                        completedTasks.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await();
        }
        
        Duration duration = Duration.between(start, Instant.now());
        System.out.printf("Virtual Threads: %d ms, %d tasks completed%n", 
            duration.toMillis(), completedTasks.get());
    }
    
    private static void benchmarkVirtualThreadsWithStructuredConcurrency(int taskCount, Duration ioDelay) 
            throws InterruptedException {
        
        Instant start = Instant.now();
        
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            // Launch all tasks
            for (int i = 0; i < taskCount; i++) {
                scope.fork(() -> {
                    try {
                        Thread.sleep(ioDelay);
                        return "completed";
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                });
            }
            
            scope.join();
            // Note: not calling throwIfFailed() to handle any failures gracefully
        } catch (Exception e) {
            System.err.println("Structured concurrency benchmark failed: " + e.getMessage());
            return;
        }
        
        Duration duration = Duration.between(start, Instant.now());
        System.out.printf("Virtual Threads (Structured): %d ms, %d tasks completed%n", 
            duration.toMillis(), taskCount);
    }
    
    // Memory usage analysis
    public static void analyzeMemoryUsage() throws InterruptedException {
        System.out.println("\n=== Memory Usage Analysis ===");
        
        Runtime runtime = Runtime.getRuntime();
        
        // Baseline memory
        System.gc();
        long baselineMemory = runtime.totalMemory() - runtime.freeMemory();
        System.out.printf("Baseline memory: %d MB%n", baselineMemory / (1024 * 1024));
        
        // Platform threads memory usage
        try (ExecutorService executor = Executors.newFixedThreadPool(1000)) {
            System.gc();
            long platformThreadMemory = runtime.totalMemory() - runtime.freeMemory();
            System.out.printf("Platform threads (1000): %d MB%n", 
                platformThreadMemory / (1024 * 1024));
        }
        
        // Virtual threads memory usage
        CountDownLatch latch = new CountDownLatch(100000);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 100000; i++) {
                executor.submit(() -> {
                    try {
                        Thread.sleep(Duration.ofSeconds(10));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            Thread.sleep(1000); // Let threads start
            System.gc();
            long virtualThreadMemory = runtime.totalMemory() - runtime.freeMemory();
            System.out.printf("Virtual threads (100000): %d MB%n", 
                virtualThreadMemory / (1024 * 1024));
        }
        
        latch.await();
    }
}
```

## Practice Exercises

### Exercise 1: Virtual Thread Migration

Convert an existing multithreaded application to use virtual threads and measure the performance improvement.

### Exercise 2: Structured Concurrency Implementation

Implement a web scraper using structured concurrency that fetches data from multiple sources and aggregates the results.

### Exercise 3: Scoped Values Application

Build a request processing system that uses scoped values to pass context (user, session, permissions) through the entire request lifecycle.

### Exercise 4: High-Concurrency Server

Create a server that can handle 100,000+ concurrent connections using virtual threads and compare it with traditional approaches.

## Key Takeaways

1. **Virtual Threads**: Lightweight threads that enable massive concurrency without the overhead of platform threads
2. **Structured Concurrency**: Better error handling and resource management for concurrent tasks
3. **Scoped Values**: Thread-safe context passing that's more efficient than ThreadLocal
4. **Performance**: Virtual threads excel in I/O-bound scenarios with minimal memory overhead
5. **Migration**: Existing code can often be migrated to virtual threads with minimal changes

Virtual threads represent a paradigm shift in Java concurrency, enabling applications to handle millions of concurrent tasks efficiently while maintaining simplicity and reliability.