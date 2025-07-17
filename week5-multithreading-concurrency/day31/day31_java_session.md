# Day 31: Executor Framework (August 9, 2025)

**Date:** August 9, 2025  
**Duration:** 3 hours  
**Focus:** Master the Executor Framework for professional thread pool management and task execution

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:
- ✅ Understand the Executor Framework architecture
- ✅ Master different types of thread pools and their use cases
- ✅ Learn Future and Callable for result-returning tasks
- ✅ Implement custom thread pools and rejection policies
- ✅ Handle task scheduling with ScheduledExecutorService
- ✅ Build production-ready concurrent applications

---

## 🏗️ Part 1: Executor Framework Fundamentals (60 minutes)

### **Understanding the Executor Framework**

The **Executor Framework** provides a higher-level abstraction for thread management:
- **Executor**: Simple interface for executing tasks
- **ExecutorService**: Extended interface with lifecycle management
- **ThreadPoolExecutor**: Configurable thread pool implementation
- **Executors**: Factory class for common executor types

### **Basic Executor Usage**

Create `ExecutorBasicsDemo.java`:

```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/**
 * Comprehensive demonstration of Executor Framework basics
 */
public class ExecutorBasicsDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Executor Framework Basics Demo ===\n");
        
        // 1. Single Thread Executor
        System.out.println("1. SingleThreadExecutor:");
        demonstrateSingleThreadExecutor();
        
        // 2. Fixed Thread Pool
        System.out.println("\n2. FixedThreadPool:");
        demonstrateFixedThreadPool();
        
        // 3. Cached Thread Pool
        System.out.println("\n3. CachedThreadPool:");
        demonstrateCachedThreadPool();
        
        // 4. Executor vs Manual Thread Management
        System.out.println("\n4. Executor vs Manual Threads:");
        compareExecutorVsManualThreads();
        
        System.out.println("\n=== Executor Basics Demo Completed ===");
    }
    
    private static void demonstrateSingleThreadExecutor() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        // Submit multiple tasks - they will execute sequentially
        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("SingleThread Task " + taskId + " executing on " + 
                    Thread.currentThread().getName());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("SingleThread Task " + taskId + " completed");
            });
        }
        
        // Graceful shutdown
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    private static void demonstrateFixedThreadPool() {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        
        // Submit multiple tasks - they will execute on 3 threads max
        for (int i = 1; i <= 8; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Fixed Task " + taskId + " executing on " + 
                    Thread.currentThread().getName());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("Fixed Task " + taskId + " completed");
            });
        }
        
        shutdownExecutor(executor, "FixedThreadPool");
    }
    
    private static void demonstrateCachedThreadPool() {
        ExecutorService executor = Executors.newCachedThreadPool();
        
        // Submit tasks with varying delays
        for (int i = 1; i <= 6; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Cached Task " + taskId + " executing on " + 
                    Thread.currentThread().getName());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("Cached Task " + taskId + " completed");
            });
            
            // Add delay to demonstrate thread reuse
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        shutdownExecutor(executor, "CachedThreadPool");
    }
    
    private static void compareExecutorVsManualThreads() {
        System.out.println("Manual Thread Management:");
        long startTime = System.currentTimeMillis();
        
        // Manual thread management
        Thread[] threads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            final int taskId = i + 1;
            threads[i] = new Thread(() -> {
                System.out.println("Manual Task " + taskId + " on " + Thread.currentThread().getName());
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        long manualTime = System.currentTimeMillis() - startTime;
        System.out.println("Manual threads completed in: " + manualTime + "ms");
        
        System.out.println("\nExecutor Management:");
        startTime = System.currentTimeMillis();
        
        // Executor management
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(5);
        
        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    System.out.println("Executor Task " + taskId + " on " + Thread.currentThread().getName());
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long executorTime = System.currentTimeMillis() - startTime;
        System.out.println("Executor completed in: " + executorTime + "ms");
        
        shutdownExecutor(executor, "Comparison");
    }
    
    private static void shutdownExecutor(ExecutorService executor, String name) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                System.out.println(name + " did not terminate gracefully, forcing shutdown");
                executor.shutdownNow();
            } else {
                System.out.println(name + " terminated gracefully");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

### **Future and Callable Interface**

Create `FutureCallableDemo.java`:

```java
import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/**
 * Comprehensive demonstration of Future and Callable
 */
public class FutureCallableDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Future and Callable Demo ===\n");
        
        // 1. Basic Callable and Future
        System.out.println("1. Basic Callable and Future:");
        demonstrateBasicCallable();
        
        // 2. Multiple Futures with different completion times
        System.out.println("\n2. Multiple Futures:");
        demonstrateMultipleFutures();
        
        // 3. Future timeout and cancellation
        System.out.println("\n3. Future Timeout and Cancellation:");
        demonstrateFutureTimeout();
        
        // 4. invokeAll and invokeAny
        System.out.println("\n4. invokeAll and invokeAny:");
        demonstrateInvokeOperations();
        
        System.out.println("\n=== Future and Callable Demo Completed ===");
    }
    
    private static void demonstrateBasicCallable() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        // Create a Callable that returns a result
        Callable<String> task = () -> {
            System.out.println("Callable task starting on " + Thread.currentThread().getName());
            Thread.sleep(2000);
            return "Task completed successfully at " + System.currentTimeMillis();
        };
        
        try {
            // Submit the callable and get a Future
            Future<String> future = executor.submit(task);
            
            System.out.println("Task submitted, doing other work...");
            
            // Simulate doing other work
            Thread.sleep(500);
            System.out.println("Other work completed");
            
            // Get the result (this will block until task completes)
            String result = future.get();
            System.out.println("Result: " + result);
            
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }
    
    private static void demonstrateMultipleFutures() {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        List<Future<Integer>> futures = new ArrayList<>();
        
        // Submit multiple computational tasks
        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            Callable<Integer> task = () -> {
                System.out.println("Computation Task " + taskId + " starting");
                
                // Simulate different computation times
                int sleepTime = 1000 + (taskId * 500);
                Thread.sleep(sleepTime);
                
                int result = taskId * taskId;
                System.out.println("Computation Task " + taskId + " completed: " + result);
                return result;
            };
            
            Future<Integer> future = executor.submit(task);
            futures.add(future);
        }
        
        // Collect results as they become available
        System.out.println("Waiting for results...");
        for (int i = 0; i < futures.size(); i++) {
            try {
                Integer result = futures.get(i).get();
                System.out.println("Got result from task " + (i + 1) + ": " + result);
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Error getting result from task " + (i + 1) + ": " + e.getMessage());
            }
        }
        
        executor.shutdown();
    }
    
    private static void demonstrateFutureTimeout() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        // Create a long-running task
        Callable<String> longTask = () -> {
            System.out.println("Long task starting...");
            Thread.sleep(5000); // 5 seconds
            return "Long task completed";
        };
        
        Future<String> future = executor.submit(longTask);
        
        try {
            // Try to get result with timeout
            System.out.println("Waiting for result with 2-second timeout...");
            String result = future.get(2, TimeUnit.SECONDS);
            System.out.println("Result: " + result);
            
        } catch (TimeoutException e) {
            System.out.println("Task timed out, cancelling...");
            boolean cancelled = future.cancel(true); // interrupt if running
            System.out.println("Cancellation successful: " + cancelled);
            
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error: " + e.getMessage());
        }
        
        // Check final state
        System.out.println("Future cancelled: " + future.isCancelled());
        System.out.println("Future done: " + future.isDone());
        
        executor.shutdown();
    }
    
    private static void demonstrateInvokeOperations() {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        
        // Create a collection of tasks
        List<Callable<String>> tasks = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            final int taskId = i;
            tasks.add(() -> {
                int sleepTime = 1000 + new Random().nextInt(2000);
                System.out.println("InvokeAll Task " + taskId + " starting (will take " + sleepTime + "ms)");
                Thread.sleep(sleepTime);
                return "Result from task " + taskId;
            });
        }
        
        // invokeAll - wait for all tasks to complete
        System.out.println("Using invokeAll - waiting for all tasks:");
        try {
            List<Future<String>> results = executor.invokeAll(tasks);
            
            for (int i = 0; i < results.size(); i++) {
                try {
                    String result = results.get(i).get();
                    System.out.println("invokeAll result " + (i + 1) + ": " + result);
                } catch (ExecutionException e) {
                    System.err.println("Task " + (i + 1) + " failed: " + e.getMessage());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // invokeAny - wait for first successful completion
        System.out.println("\nUsing invokeAny - waiting for first completion:");
        List<Callable<String>> anyTasks = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            final int taskId = i;
            anyTasks.add(() -> {
                int sleepTime = 500 + new Random().nextInt(1500);
                System.out.println("InvokeAny Task " + taskId + " starting (will take " + sleepTime + "ms)");
                Thread.sleep(sleepTime);
                return "First completed result from task " + taskId;
            });
        }
        
        try {
            String firstResult = executor.invokeAny(anyTasks);
            System.out.println("invokeAny result: " + firstResult);
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("invokeAny failed: " + e.getMessage());
        }
        
        executor.shutdown();
    }
}
```

---

## ⚙️ Part 2: Custom Thread Pools & Configuration (60 minutes)

### **ThreadPoolExecutor Configuration**

Create `CustomThreadPoolDemo.java`:

```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive demonstration of custom thread pool configuration
 */
public class CustomThreadPoolDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Custom Thread Pool Configuration Demo ===\n");
        
        // 1. Basic ThreadPoolExecutor configuration
        System.out.println("1. Basic ThreadPoolExecutor:");
        demonstrateBasicThreadPoolExecutor();
        
        // 2. Different rejection policies
        System.out.println("\n2. Rejection Policies:");
        demonstrateRejectionPolicies();
        
        // 3. Custom ThreadFactory
        System.out.println("\n3. Custom ThreadFactory:");
        demonstrateCustomThreadFactory();
        
        // 4. Monitoring thread pool
        System.out.println("\n4. Thread Pool Monitoring:");
        demonstrateThreadPoolMonitoring();
        
        System.out.println("\n=== Custom Thread Pool Demo Completed ===");
    }
    
    private static void demonstrateBasicThreadPoolExecutor() {
        // Create custom thread pool
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2,                              // corePoolSize
            4,                              // maximumPoolSize
            60L,                            // keepAliveTime
            TimeUnit.SECONDS,               // timeUnit
            new LinkedBlockingQueue<>(5)    // workQueue with capacity 5
        );
        
        System.out.println("Created ThreadPoolExecutor:");
        System.out.println("  Core pool size: " + executor.getCorePoolSize());
        System.out.println("  Maximum pool size: " + executor.getMaximumPoolSize());
        System.out.println("  Keep alive time: " + executor.getKeepAliveTime(TimeUnit.SECONDS) + " seconds");
        System.out.println("  Queue capacity: " + ((LinkedBlockingQueue<?>) executor.getQueue()).remainingCapacity());
        
        // Submit tasks to see pool behavior
        for (int i = 1; i <= 12; i++) {
            final int taskId = i;
            try {
                executor.submit(() -> {
                    System.out.println("Task " + taskId + " executing on " + 
                        Thread.currentThread().getName());
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    System.out.println("Task " + taskId + " completed");
                });
                
                System.out.println("Submitted task " + taskId + 
                    " - Active threads: " + executor.getActiveCount() + 
                    ", Queue size: " + executor.getQueue().size());
                
            } catch (RejectedExecutionException e) {
                System.out.println("Task " + taskId + " rejected: " + e.getMessage());
            }
            
            // Small delay to observe behavior
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        shutdownExecutor(executor, "Custom ThreadPoolExecutor");
    }
    
    private static void demonstrateRejectionPolicies() {
        System.out.println("Testing different rejection policies:");
        
        // 1. AbortPolicy (default) - throws RejectedExecutionException
        testRejectionPolicy("AbortPolicy", new ThreadPoolExecutor.AbortPolicy());
        
        // 2. CallerRunsPolicy - runs task in caller thread
        testRejectionPolicy("CallerRunsPolicy", new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 3. DiscardPolicy - silently discards task
        testRejectionPolicy("DiscardPolicy", new ThreadPoolExecutor.DiscardPolicy());
        
        // 4. DiscardOldestPolicy - discards oldest task in queue
        testRejectionPolicy("DiscardOldestPolicy", new ThreadPoolExecutor.DiscardOldestPolicy());
    }
    
    private static void testRejectionPolicy(String policyName, RejectedExecutionHandler policy) {
        System.out.println("\n  Testing " + policyName + ":");
        
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(2),
            policy
        );
        
        // Submit tasks to fill queue and trigger rejection
        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            try {
                executor.submit(() -> {
                    System.out.println("    " + policyName + " Task " + taskId + 
                        " executing on " + Thread.currentThread().getName());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                System.out.println("    Submitted task " + taskId);
            } catch (RejectedExecutionException e) {
                System.out.println("    Task " + taskId + " rejected: " + e.getClass().getSimpleName());
            }
        }
        
        shutdownExecutor(executor, policyName);
    }
    
    private static void demonstrateCustomThreadFactory() {
        CustomThreadFactory factory = new CustomThreadFactory("CustomWorker");
        
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2, 4, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            factory
        );
        
        // Submit some tasks to see custom thread names
        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Custom factory task " + taskId + 
                    " executing on " + Thread.currentThread().getName());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        shutdownExecutor(executor, "Custom ThreadFactory");
        
        // Display thread factory statistics
        factory.printStatistics();
    }
    
    private static void demonstrateThreadPoolMonitoring() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2, 5, 30L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
        );
        
        // Start monitoring thread
        Thread monitor = new Thread(new ThreadPoolMonitor(executor));
        monitor.setDaemon(true);
        monitor.start();
        
        // Submit various tasks
        for (int i = 1; i <= 10; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    Thread.sleep(1000 + (taskId % 3) * 500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        shutdownExecutor(executor, "Monitored ThreadPool");
    }
    
    private static void shutdownExecutor(ExecutorService executor, String name) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                System.out.println(name + " did not terminate gracefully, forcing shutdown");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

/**
 * Custom thread factory for creating named threads
 */
class CustomThreadFactory implements ThreadFactory {
    private final String namePrefix;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final AtomicInteger createdThreads = new AtomicInteger(0);
    
    public CustomThreadFactory(String namePrefix) {
        this.namePrefix = namePrefix;
    }
    
    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, namePrefix + "-" + threadNumber.getAndIncrement());
        thread.setDaemon(false);
        thread.setPriority(Thread.NORM_PRIORITY);
        createdThreads.incrementAndGet();
        
        System.out.println("Created thread: " + thread.getName());
        return thread;
    }
    
    public void printStatistics() {
        System.out.println("CustomThreadFactory Statistics:");
        System.out.println("  Total threads created: " + createdThreads.get());
        System.out.println("  Name prefix: " + namePrefix);
    }
}

/**
 * Thread pool monitor for real-time statistics
 */
class ThreadPoolMonitor implements Runnable {
    private final ThreadPoolExecutor executor;
    
    public ThreadPoolMonitor(ThreadPoolExecutor executor) {
        this.executor = executor;
    }
    
    @Override
    public void run() {
        while (!executor.isShutdown()) {
            System.out.println("ThreadPool Status:");
            System.out.println("  Pool size: " + executor.getPoolSize());
            System.out.println("  Active threads: " + executor.getActiveCount());
            System.out.println("  Queue size: " + executor.getQueue().size());
            System.out.println("  Completed tasks: " + executor.getCompletedTaskCount());
            System.out.println("  Total tasks: " + executor.getTaskCount());
            System.out.println("  ---");
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
```

---

## 📅 Part 3: Scheduled Execution & Real-World Applications (60 minutes)

### **ScheduledExecutorService**

Create `ScheduledExecutorDemo.java`:

```java
import java.util.concurrent.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive demonstration of scheduled task execution
 */
public class ScheduledExecutorDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Scheduled Executor Demo ===\n");
        
        // 1. Basic scheduled execution
        System.out.println("1. Basic Scheduled Execution:");
        demonstrateBasicScheduling();
        
        // 2. Periodic execution
        System.out.println("\n2. Periodic Execution:");
        demonstratePeriodicExecution();
        
        // 3. Real-world scheduled tasks
        System.out.println("\n3. Real-World Scheduled Tasks:");
        demonstrateRealWorldTasks();
        
        System.out.println("\n=== Scheduled Executor Demo Completed ===");
    }
    
    private static void demonstrateBasicScheduling() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        
        System.out.println("Current time: " + getCurrentTime());
        
        // Schedule a task to run after a delay
        ScheduledFuture<?> future1 = scheduler.schedule(() -> {
            System.out.println("Delayed task executed at: " + getCurrentTime());
        }, 2, TimeUnit.SECONDS);
        
        // Schedule a callable with delay
        ScheduledFuture<String> future2 = scheduler.schedule(() -> {
            return "Delayed callable result at: " + getCurrentTime();
        }, 3, TimeUnit.SECONDS);
        
        try {
            // Wait for results
            future1.get();
            String result = future2.get();
            System.out.println(result);
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error: " + e.getMessage());
        }
        
        scheduler.shutdown();
    }
    
    private static void demonstratePeriodicExecution() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
        
        System.out.println("Starting periodic tasks at: " + getCurrentTime());
        
        // scheduleAtFixedRate - fixed rate execution
        AtomicInteger fixedRateCounter = new AtomicInteger(0);
        ScheduledFuture<?> fixedRateTask = scheduler.scheduleAtFixedRate(() -> {
            int count = fixedRateCounter.incrementAndGet();
            System.out.println("FixedRate task " + count + " at: " + getCurrentTime());
            
            // Simulate variable execution time
            try {
                Thread.sleep(500 + (count % 3) * 200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, 0, 1, TimeUnit.SECONDS);
        
        // scheduleWithFixedDelay - fixed delay between executions
        AtomicInteger fixedDelayCounter = new AtomicInteger(0);
        ScheduledFuture<?> fixedDelayTask = scheduler.scheduleWithFixedDelay(() -> {
            int count = fixedDelayCounter.incrementAndGet();
            System.out.println("FixedDelay task " + count + " at: " + getCurrentTime());
            
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, 1, 1, TimeUnit.SECONDS);
        
        // Let tasks run for a while
        try {
            Thread.sleep(8000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Cancel periodic tasks
        System.out.println("Cancelling periodic tasks...");
        fixedRateTask.cancel(false);
        fixedDelayTask.cancel(false);
        
        scheduler.shutdown();
    }
    
    private static void demonstrateRealWorldTasks() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
        
        // 1. System Health Monitor
        HealthMonitor healthMonitor = new HealthMonitor();
        scheduler.scheduleAtFixedRate(healthMonitor, 0, 2, TimeUnit.SECONDS);
        
        // 2. Cache Cleanup Task
        CacheCleanupTask cleanupTask = new CacheCleanupTask();
        scheduler.scheduleWithFixedDelay(cleanupTask, 1, 3, TimeUnit.SECONDS);
        
        // 3. Backup Task (runs once after delay)
        BackupTask backupTask = new BackupTask();
        scheduler.schedule(backupTask, 5, TimeUnit.SECONDS);
        
        // 4. Report Generator (periodic)
        ReportGenerator reportGenerator = new ReportGenerator();
        scheduler.scheduleAtFixedRate(reportGenerator, 2, 4, TimeUnit.SECONDS);
        
        // Let tasks run
        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Shutting down scheduler...");
        scheduler.shutdown();
        
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    private static String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }
}

/**
 * Health monitoring task
 */
class HealthMonitor implements Runnable {
    private final AtomicInteger checkCount = new AtomicInteger(0);
    
    @Override
    public void run() {
        int count = checkCount.incrementAndGet();
        
        // Simulate health checks
        double cpuUsage = Math.random() * 100;
        double memoryUsage = Math.random() * 100;
        
        System.out.println("Health Check #" + count + " at " + getCurrentTime() + 
            " - CPU: " + String.format("%.1f", cpuUsage) + "%, " +
            "Memory: " + String.format("%.1f", memoryUsage) + "%");
        
        if (cpuUsage > 80 || memoryUsage > 85) {
            System.out.println("  WARNING: High resource usage detected!");
        }
    }
    
    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }
}

/**
 * Cache cleanup task
 */
class CacheCleanupTask implements Runnable {
    private final AtomicInteger cleanupCount = new AtomicInteger(0);
    
    @Override
    public void run() {
        int count = cleanupCount.incrementAndGet();
        
        // Simulate cache cleanup
        int expiredEntries = (int) (Math.random() * 10);
        
        System.out.println("Cache Cleanup #" + count + " at " + getCurrentTime() + 
            " - Removed " + expiredEntries + " expired entries");
        
        // Simulate cleanup work
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }
}

/**
 * Backup task (one-time execution)
 */
class BackupTask implements Runnable {
    @Override
    public void run() {
        System.out.println("Starting backup process at " + getCurrentTime());
        
        try {
            // Simulate backup work
            for (int i = 1; i <= 3; i++) {
                System.out.println("  Backing up data chunk " + i + "/3...");
                Thread.sleep(800);
            }
            
            System.out.println("Backup completed successfully at " + getCurrentTime());
            
        } catch (InterruptedException e) {
            System.out.println("Backup interrupted");
            Thread.currentThread().interrupt();
        }
    }
    
    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }
}

/**
 * Report generation task
 */
class ReportGenerator implements Runnable {
    private final AtomicInteger reportCount = new AtomicInteger(0);
    
    @Override
    public void run() {
        int count = reportCount.incrementAndGet();
        
        System.out.println("Generating Report #" + count + " at " + getCurrentTime());
        
        try {
            // Simulate report generation
            Thread.sleep(500);
            
            // Simulate random report data
            int transactions = (int) (Math.random() * 1000);
            double revenue = Math.random() * 10000;
            
            System.out.println("  Report #" + count + " completed - " +
                "Transactions: " + transactions + ", " +
                "Revenue: $" + String.format("%.2f", revenue));
                
        } catch (InterruptedException e) {
            System.out.println("Report generation interrupted");
            Thread.currentThread().interrupt();
        }
    }
    
    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }
}
```

### **Production-Ready Task Management System**

Create `TaskManagementSystem.java`:

```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Production-ready task management system using Executor Framework
 */
public class TaskManagementSystem {
    
    public static void main(String[] args) {
        System.out.println("=== Production Task Management System ===\n");
        
        TaskManager taskManager = new TaskManager();
        
        // Start the task manager
        taskManager.start();
        
        // Submit various types of tasks
        submitSampleTasks(taskManager);
        
        // Let tasks run
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Shutdown gracefully
        taskManager.shutdown();
        
        System.out.println("\n=== Task Management System Demo Completed ===");
    }
    
    private static void submitSampleTasks(TaskManager taskManager) {
        // Submit high priority tasks
        for (int i = 1; i <= 3; i++) {
            taskManager.submitTask(new SampleTask("HighPriority-" + i, TaskPriority.HIGH, 1000));
        }
        
        // Submit normal priority tasks
        for (int i = 1; i <= 5; i++) {
            taskManager.submitTask(new SampleTask("Normal-" + i, TaskPriority.NORMAL, 2000));
        }
        
        // Submit low priority tasks
        for (int i = 1; i <= 4; i++) {
            taskManager.submitTask(new SampleTask("LowPriority-" + i, TaskPriority.LOW, 3000));
        }
        
        // Submit a long-running task
        taskManager.submitTask(new SampleTask("LongRunning", TaskPriority.NORMAL, 10000));
        
        // Schedule a recurring task
        taskManager.scheduleRecurringTask(
            new SampleTask("Recurring", TaskPriority.LOW, 500),
            2, TimeUnit.SECONDS
        );
    }
}

/**
 * Comprehensive task management system
 */
class TaskManager {
    private final ExecutorService highPriorityExecutor;
    private final ExecutorService normalPriorityExecutor;
    private final ExecutorService lowPriorityExecutor;
    private final ScheduledExecutorService scheduledExecutor;
    
    private final Map<String, Future<?>> runningTasks;
    private final AtomicLong taskIdGenerator;
    
    public TaskManager() {
        // Different thread pools for different priorities
        this.highPriorityExecutor = Executors.newFixedThreadPool(2);
        this.normalPriorityExecutor = Executors.newFixedThreadPool(3);
        this.lowPriorityExecutor = Executors.newFixedThreadPool(2);
        this.scheduledExecutor = Executors.newScheduledThreadPool(2);
        
        this.runningTasks = new ConcurrentHashMap<>();
        this.taskIdGenerator = new AtomicLong(0);
    }
    
    public void start() {
        System.out.println("Task Manager started");
        
        // Start monitoring thread
        scheduledExecutor.scheduleAtFixedRate(this::printStatistics, 5, 5, TimeUnit.SECONDS);
    }
    
    public String submitTask(Task task) {
        String taskId = "TASK-" + taskIdGenerator.incrementAndGet();
        
        ExecutorService executor = getExecutorForPriority(task.getPriority());
        
        Future<?> future = executor.submit(() -> {
            try {
                System.out.println("Starting task: " + taskId + " (" + task.getName() + 
                    ") Priority: " + task.getPriority() + " on " + Thread.currentThread().getName());
                
                task.execute();
                
                System.out.println("Completed task: " + taskId + " (" + task.getName() + ")");
            } catch (Exception e) {
                System.err.println("Task failed: " + taskId + " - " + e.getMessage());
            } finally {
                runningTasks.remove(taskId);
            }
        });
        
        runningTasks.put(taskId, future);
        
        System.out.println("Submitted task: " + taskId + " (" + task.getName() + 
            ") Priority: " + task.getPriority());
        
        return taskId;
    }
    
    public String scheduleTask(Task task, long delay, TimeUnit unit) {
        String taskId = "SCHEDULED-" + taskIdGenerator.incrementAndGet();
        
        ScheduledFuture<?> future = scheduledExecutor.schedule(() -> {
            try {
                System.out.println("Executing scheduled task: " + taskId + " (" + task.getName() + ")");
                task.execute();
                System.out.println("Completed scheduled task: " + taskId + " (" + task.getName() + ")");
            } catch (Exception e) {
                System.err.println("Scheduled task failed: " + taskId + " - " + e.getMessage());
            } finally {
                runningTasks.remove(taskId);
            }
        }, delay, unit);
        
        runningTasks.put(taskId, future);
        
        System.out.println("Scheduled task: " + taskId + " (" + task.getName() + 
            ") to run in " + delay + " " + unit);
        
        return taskId;
    }
    
    public String scheduleRecurringTask(Task task, long period, TimeUnit unit) {
        String taskId = "RECURRING-" + taskIdGenerator.incrementAndGet();
        
        ScheduledFuture<?> future = scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                System.out.println("Executing recurring task: " + taskId + " (" + task.getName() + ")");
                task.execute();
            } catch (Exception e) {
                System.err.println("Recurring task failed: " + taskId + " - " + e.getMessage());
            }
        }, 0, period, unit);
        
        runningTasks.put(taskId, future);
        
        System.out.println("Scheduled recurring task: " + taskId + " (" + task.getName() + 
            ") every " + period + " " + unit);
        
        return taskId;
    }
    
    public boolean cancelTask(String taskId) {
        Future<?> future = runningTasks.get(taskId);
        if (future != null) {
            boolean cancelled = future.cancel(true);
            if (cancelled) {
                runningTasks.remove(taskId);
                System.out.println("Cancelled task: " + taskId);
            }
            return cancelled;
        }
        return false;
    }
    
    private ExecutorService getExecutorForPriority(TaskPriority priority) {
        switch (priority) {
            case HIGH:
                return highPriorityExecutor;
            case NORMAL:
                return normalPriorityExecutor;
            case LOW:
                return lowPriorityExecutor;
            default:
                return normalPriorityExecutor;
        }
    }
    
    private void printStatistics() {
        System.out.println("\n--- Task Manager Statistics ---");
        System.out.println("Running tasks: " + runningTasks.size());
        
        if (highPriorityExecutor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) highPriorityExecutor;
            System.out.println("High Priority Pool - Active: " + tpe.getActiveCount() + 
                ", Queue: " + tpe.getQueue().size());
        }
        
        if (normalPriorityExecutor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) normalPriorityExecutor;
            System.out.println("Normal Priority Pool - Active: " + tpe.getActiveCount() + 
                ", Queue: " + tpe.getQueue().size());
        }
        
        if (lowPriorityExecutor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) lowPriorityExecutor;
            System.out.println("Low Priority Pool - Active: " + tpe.getActiveCount() + 
                ", Queue: " + tpe.getQueue().size());
        }
        
        System.out.println("-------------------------------\n");
    }
    
    public void shutdown() {
        System.out.println("Shutting down Task Manager...");
        
        // Cancel all running tasks
        for (Map.Entry<String, Future<?>> entry : runningTasks.entrySet()) {
            entry.getValue().cancel(true);
        }
        
        // Shutdown executors
        List<ExecutorService> executors = List.of(
            highPriorityExecutor,
            normalPriorityExecutor,
            lowPriorityExecutor,
            scheduledExecutor
        );
        
        for (ExecutorService executor : executors) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("Task Manager shutdown completed");
    }
}

/**
 * Task interface
 */
interface Task {
    String getName();
    TaskPriority getPriority();
    void execute() throws Exception;
}

/**
 * Task priority levels
 */
enum TaskPriority {
    HIGH, NORMAL, LOW
}

/**
 * Sample task implementation
 */
class SampleTask implements Task {
    private final String name;
    private final TaskPriority priority;
    private final long executionTimeMs;
    
    public SampleTask(String name, TaskPriority priority, long executionTimeMs) {
        this.name = name;
        this.priority = priority;
        this.executionTimeMs = executionTimeMs;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public TaskPriority getPriority() {
        return priority;
    }
    
    @Override
    public void execute() throws Exception {
        // Simulate work
        Thread.sleep(executionTimeMs);
    }
}
```

---

## 🏆 Day 31 Summary

### **Key Concepts Mastered:**

✅ **Executor Framework Architecture:**
- Executor, ExecutorService, and ScheduledExecutorService interfaces
- Different types of thread pools and their use cases
- Lifecycle management and graceful shutdown

✅ **Future and Callable:**
- Result-returning tasks with Callable interface
- Future for async result retrieval
- Timeout handling and task cancellation
- invokeAll() and invokeAny() for batch operations

✅ **Custom Thread Pool Configuration:**
- ThreadPoolExecutor with custom parameters
- Queue types and rejection policies
- Custom ThreadFactory for named threads
- Thread pool monitoring and statistics

✅ **Scheduled Execution:**
- One-time delayed execution
- Periodic execution patterns (fixed rate vs fixed delay)
- Real-world scheduled task examples
- Production-ready task management system

✅ **Best Practices:**
- Proper executor shutdown procedures
- Resource management and monitoring
- Priority-based task execution
- Error handling and recovery

### **Production Applications:**
- Background job processing
- Scheduled maintenance tasks
- Web server request handling
- Data processing pipelines
- System monitoring and alerts

### **Next Steps:**
Tomorrow we'll explore **Concurrent Collections** - learning about thread-safe data structures that eliminate the need for manual synchronization in many scenarios.

You're now equipped to build professional concurrent applications using the Executor Framework! 🚀⚡