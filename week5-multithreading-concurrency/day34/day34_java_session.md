# Day 34: Performance & Best Practices (August 12, 2025)

**Date:** August 12, 2025  
**Duration:** 3 hours  
**Focus:** Master performance optimization, monitoring, profiling, and best practices for concurrent applications

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:

- ✅ Master performance measurement and profiling techniques
- ✅ Understand common concurrency anti-patterns and how to avoid them
- ✅ Learn thread pool sizing and configuration optimization
- ✅ Implement monitoring and observability for concurrent applications
- ✅ Apply advanced optimization techniques for high-performance systems
- ✅ Build production-ready concurrent applications with best practices

---

## 📊 Part 1: Performance Measurement & Profiling (60 minutes)

### **Concurrency Performance Fundamentals**

**Key Performance Metrics:**

- **Throughput**: Operations per second
- **Latency**: Time per operation
- **CPU Utilization**: Processor efficiency
- **Memory Usage**: Heap and garbage collection impact
- **Thread Contention**: Lock waiting time
- **Context Switching**: Thread scheduling overhead

### **Performance Measurement Framework**

Create `ConcurrencyPerformanceAnalysis.java`:

```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * Comprehensive performance analysis and measurement framework
 */
public class ConcurrencyPerformanceAnalysis {
    
    private static final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private static final Random random = new Random();
    
    public static void main(String[] args) {
        System.out.println("=== Concurrency Performance Analysis ===\n");
        
        // 1. Throughput vs Latency analysis
        System.out.println("1. Throughput vs Latency Analysis:");
        analyzeThroughputVsLatency();
        
        // 2. Thread contention analysis
        System.out.println("\n2. Thread Contention Analysis:");
        analyzeThreadContention();
        
        // 3. Memory and GC impact
        System.out.println("\n3. Memory and GC Impact Analysis:");
        analyzeMemoryImpact();
        
        // 4. Thread pool optimization
        System.out.println("\n4. Thread Pool Optimization:");
        analyzeThreadPoolSizing();
        
        System.out.println("\n=== Performance Analysis Completed ===");
    }
    
    private static void analyzeThroughputVsLatency() {
        int[] threadCounts = {1, 2, 4, 8, 16, 32};
        int operationsPerThread = 10000;
        
        System.out.println("Thread Count | Throughput (ops/sec) | Avg Latency (μs) | 95th Percentile (μs)");
        System.out.println("-------------|---------------------|------------------|--------------------");
        
        for (int threadCount : threadCounts) {
            PerformanceResult result = measurePerformance(threadCount, operationsPerThread);
            System.out.printf("%11d | %19.0f | %16.2f | %18.2f%n",
                threadCount, result.throughput, result.avgLatency, result.p95Latency);
        }
    }
    
    private static PerformanceResult measurePerformance(int threadCount, int operationsPerThread) {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        
        List<Long> latencies = new CopyOnWriteArrayList<>();
        
        // Submit tasks
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    
                    for (int j = 0; j < operationsPerThread; j++) {
                        long startTime = System.nanoTime();
                        
                        // Simulate work (CPU-bound task)
                        doWork();
                        
                        long endTime = System.nanoTime();
                        latencies.add(endTime - startTime);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        // Start timing
        long overallStart = System.nanoTime();
        startLatch.countDown(); // Start all threads
        
        try {
            endLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long overallEnd = System.nanoTime();
        executor.shutdown();
        
        // Calculate metrics
        long totalOperations = (long) threadCount * operationsPerThread;
        double durationSeconds = (overallEnd - overallStart) / 1_000_000_000.0;
        double throughput = totalOperations / durationSeconds;
        
        latencies.sort(Long::compareTo);
        double avgLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0) / 1000.0; // μs
        double p95Latency = latencies.get((int) (latencies.size() * 0.95)) / 1000.0; // μs
        
        return new PerformanceResult(throughput, avgLatency, p95Latency);
    }
    
    private static void doWork() {
        // Simulate CPU-intensive work
        double result = 0;
        for (int i = 0; i < 1000; i++) {
            result += Math.sqrt(i) * Math.sin(i);
        }
        // Prevent optimization
        if (result == Double.MAX_VALUE) {
            System.out.println("Impossible");
        }
    }
    
    private static void analyzeThreadContention() {
        System.out.println("Analyzing different synchronization mechanisms:");
        
        int numThreads = 8;
        int operationsPerThread = 100000;
        
        // Test different synchronization approaches
        testSynchronization("Synchronized Block", 
            new SynchronizedCounter(), numThreads, operationsPerThread);
        
        testSynchronization("ReentrantLock", 
            new LockBasedCounter(), numThreads, operationsPerThread);
        
        testSynchronization("AtomicLong", 
            new AtomicCounter(), numThreads, operationsPerThread);
        
        testSynchronization("LongAdder", 
            new AdderCounter(), numThreads, operationsPerThread);
        
        testSynchronization("No Synchronization (unsafe)", 
            new UnsafeCounter(), numThreads, operationsPerThread);
    }
    
    private static void testSynchronization(String name, Counter counter, 
                                          int numThreads, int operationsPerThread) {
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        counter.increment();
                    }
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
        
        long endTime = System.nanoTime();
        double durationMs = (endTime - startTime) / 1_000_000.0;
        long expectedValue = (long) numThreads * operationsPerThread;
        long actualValue = counter.getValue();
        
        System.out.printf("%-25s: %6.2f ms, Expected: %8d, Actual: %8d, Lost: %8d%n",
            name, durationMs, expectedValue, actualValue, expectedValue - actualValue);
        
        executor.shutdown();
    }
    
    private static void analyzeMemoryImpact() {
        System.out.println("Memory allocation patterns analysis:");
        
        // Measure memory before
        MemoryUsage beforeHeap = memoryBean.getHeapMemoryUsage();
        System.out.println("Initial heap usage: " + formatBytes(beforeHeap.getUsed()));
        
        // Test different allocation patterns
        testAllocationPattern("Heavy Object Creation", ConcurrencyPerformanceAnalysis::heavyObjectCreation);
        testAllocationPattern("Primitive Operations", ConcurrencyPerformanceAnalysis::primitiveOperations);
        testAllocationPattern("String Concatenation", ConcurrencyPerformanceAnalysis::stringConcatenation);
        testAllocationPattern("StringBuilder Usage", ConcurrencyPerformanceAnalysis::stringBuilderUsage);
        
        // Force GC and measure
        System.gc();
        MemoryUsage afterHeap = memoryBean.getHeapMemoryUsage();
        System.out.println("Final heap usage: " + formatBytes(afterHeap.getUsed()));
    }
    
    private static void testAllocationPattern(String name, Runnable task) {
        MemoryUsage before = memoryBean.getHeapMemoryUsage();
        long startTime = System.nanoTime();
        
        task.run();
        
        long endTime = System.nanoTime();
        MemoryUsage after = memoryBean.getHeapMemoryUsage();
        
        long memoryIncrease = after.getUsed() - before.getUsed();
        double durationMs = (endTime - startTime) / 1_000_000.0;
        
        System.out.printf("%-25s: %6.2f ms, Memory increase: %s%n",
            name, durationMs, formatBytes(memoryIncrease));
    }
    
    private static void heavyObjectCreation() {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(4);
        
        for (int i = 0; i < 4; i++) {
            executor.submit(() -> {
                try {
                    List<Object> objects = new ArrayList<>();
                    for (int j = 0; j < 10000; j++) {
                        objects.add(new Object[] {new String("test" + j), new Double(j), new ArrayList<>()});
                    }
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
        
        executor.shutdown();
    }
    
    private static void primitiveOperations() {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(4);
        
        for (int i = 0; i < 4; i++) {
            executor.submit(() -> {
                try {
                    long sum = 0;
                    for (int j = 0; j < 1000000; j++) {
                        sum += j * j;
                    }
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
        
        executor.shutdown();
    }
    
    private static void stringConcatenation() {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(4);
        
        for (int i = 0; i < 4; i++) {
            executor.submit(() -> {
                try {
                    String result = "";
                    for (int j = 0; j < 1000; j++) {
                        result += "item" + j;
                    }
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
        
        executor.shutdown();
    }
    
    private static void stringBuilderUsage() {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(4);
        
        for (int i = 0; i < 4; i++) {
            executor.submit(() -> {
                try {
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < 1000; j++) {
                        sb.append("item").append(j);
                    }
                    String result = sb.toString();
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
        
        executor.shutdown();
    }
    
    private static void analyzeThreadPoolSizing() {
        System.out.println("Thread pool sizing analysis for different workload types:");
        
        // CPU-intensive workload
        System.out.println("\nCPU-Intensive Workload:");
        testThreadPoolSizing("CPU-Intensive", ConcurrencyPerformanceAnalysis::cpuIntensiveTask);
        
        // I/O-intensive workload
        System.out.println("\nI/O-Intensive Workload:");
        testThreadPoolSizing("I/O-Intensive", ConcurrencyPerformanceAnalysis::ioIntensiveTask);
        
        // Mixed workload
        System.out.println("\nMixed Workload:");
        testThreadPoolSizing("Mixed", ConcurrencyPerformanceAnalysis::mixedTask);
    }
    
    private static void testThreadPoolSizing(String workloadType, Runnable task) {
        int numCores = Runtime.getRuntime().availableProcessors();
        int[] poolSizes = {1, numCores, numCores * 2, numCores * 4};
        int numTasks = 100;
        
        System.out.println("Pool Size | Duration (ms) | CPU Usage | Throughput (tasks/sec)");
        System.out.println("----------|---------------|-----------|---------------------");
        
        for (int poolSize : poolSizes) {
            long startTime = System.nanoTime();
            
            ExecutorService executor = Executors.newFixedThreadPool(poolSize);
            CountDownLatch latch = new CountDownLatch(numTasks);
            
            for (int i = 0; i < numTasks; i++) {
                executor.submit(() -> {
                    try {
                        task.run();
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
            
            long endTime = System.nanoTime();
            double durationMs = (endTime - startTime) / 1_000_000.0;
            double throughput = numTasks / (durationMs / 1000.0);
            
            executor.shutdown();
            
            System.out.printf("%9d | %13.2f | %9s | %19.2f%n",
                poolSize, durationMs, "N/A", throughput);
        }
    }
    
    private static void cpuIntensiveTask() {
        // Simulate CPU-intensive computation
        double result = 0;
        for (int i = 0; i < 100000; i++) {
            result += Math.sqrt(i) * Math.sin(i) * Math.cos(i);
        }
    }
    
    private static void ioIntensiveTask() {
        // Simulate I/O wait
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private static void mixedTask() {
        // Simulate mixed workload
        cpuIntensiveTask();
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}

// Performance result data class
class PerformanceResult {
    final double throughput;
    final double avgLatency;
    final double p95Latency;
    
    PerformanceResult(double throughput, double avgLatency, double p95Latency) {
        this.throughput = throughput;
        this.avgLatency = avgLatency;
        this.p95Latency = p95Latency;
    }
}

// Counter implementations for contention analysis
interface Counter {
    void increment();
    long getValue();
}

class SynchronizedCounter implements Counter {
    private long count = 0;
    
    @Override
    public synchronized void increment() {
        count++;
    }
    
    @Override
    public synchronized long getValue() {
        return count;
    }
}

class LockBasedCounter implements Counter {
    private final ReentrantLock lock = new ReentrantLock();
    private long count = 0;
    
    @Override
    public void increment() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public long getValue() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }
}

class AtomicCounter implements Counter {
    private final AtomicLong count = new AtomicLong(0);
    
    @Override
    public void increment() {
        count.incrementAndGet();
    }
    
    @Override
    public long getValue() {
        return count.get();
    }
}

class AdderCounter implements Counter {
    private final LongAdder count = new LongAdder();
    
    @Override
    public void increment() {
        count.increment();
    }
    
    @Override
    public long getValue() {
        return count.sum();
    }
}

class UnsafeCounter implements Counter {
    private long count = 0;
    
    @Override
    public void increment() {
        count++; // Unsafe - race condition
    }
    
    @Override
    public long getValue() {
        return count;
    }
}
```

---

## 🚫 Part 2: Common Anti-Patterns & Best Practices (60 minutes)

### **Concurrency Anti-Patterns and Solutions**

Create `ConcurrencyAntiPatterns.java`:

```java
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * Comprehensive demonstration of concurrency anti-patterns and their solutions
 */
public class ConcurrencyAntiPatterns {
    
    public static void main(String[] args) {
        System.out.println("=== Concurrency Anti-Patterns & Best Practices ===\n");
        
        // 1. Double-checked locking anti-pattern
        System.out.println("1. Double-Checked Locking Anti-Pattern:");
        demonstrateDoubleCheckedLocking();
        
        // 2. Busy waiting anti-pattern
        System.out.println("\n2. Busy Waiting Anti-Pattern:");
        demonstrateBusyWaiting();
        
        // 3. Shared mutable state anti-pattern
        System.out.println("\n3. Shared Mutable State Anti-Pattern:");
        demonstrateSharedMutableState();
        
        // 4. Thread pool misuse anti-pattern
        System.out.println("\n4. Thread Pool Misuse Anti-Pattern:");
        demonstrateThreadPoolMisuse();
        
        // 5. Deadlock-prone patterns
        System.out.println("\n5. Deadlock-Prone Patterns:");
        demonstrateDeadlockPatterns();
        
        System.out.println("\n=== Anti-Patterns Demo Completed ===");
    }
    
    private static void demonstrateDoubleCheckedLocking() {
        System.out.println("❌ BROKEN: Unsafe double-checked locking");
        
        // This is broken due to memory model issues
        class BrokenSingleton {
            private static BrokenSingleton instance;
            
            public static BrokenSingleton getInstance() {
                if (instance == null) { // First check (unsafe)
                    synchronized (BrokenSingleton.class) {
                        if (instance == null) { // Second check
                            instance = new BrokenSingleton(); // Can be reordered!
                        }
                    }
                }
                return instance;
            }
        }
        
        System.out.println("✅ CORRECT: Proper singleton patterns");
        
        // Solution 1: Eager initialization
        class EagerSingleton {
            private static final EagerSingleton INSTANCE = new EagerSingleton();
            
            public static EagerSingleton getInstance() {
                return INSTANCE;
            }
        }
        
        // Solution 2: Initialization-on-demand holder
        class LazyHolderSingleton {
            private static class Holder {
                static final LazyHolderSingleton INSTANCE = new LazyHolderSingleton();
            }
            
            public static LazyHolderSingleton getInstance() {
                return Holder.INSTANCE;
            }
        }
        
        // Solution 3: Enum singleton
        enum EnumSingleton {
            INSTANCE;
            
            public void doSomething() {
                System.out.println("Enum singleton is thread-safe by design");
            }
        }
        
        // Solution 4: Volatile double-checked locking (correct)
        class CorrectSingleton {
            private static volatile CorrectSingleton instance;
            
            public static CorrectSingleton getInstance() {
                if (instance == null) {
                    synchronized (CorrectSingleton.class) {
                        if (instance == null) {
                            instance = new CorrectSingleton();
                        }
                    }
                }
                return instance;
            }
        }
        
        // Test thread safety
        testSingletonThreadSafety();
    }
    
    private static void testSingletonThreadSafety() {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);
        List<Object> instances = Collections.synchronizedList(new ArrayList<>());
        
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    // Test LazyHolderSingleton
                    instances.add(LazyHolderSingleton.getInstance());
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
        
        long uniqueInstances = instances.stream().distinct().count();
        System.out.println("Unique singleton instances created: " + uniqueInstances + " (should be 1)");
        
        executor.shutdown();
    }
    
    private static void demonstrateBusyWaiting() {
        System.out.println("❌ BROKEN: Busy waiting (CPU-intensive polling)");
        
        class BusyWaitingExample {
            private volatile boolean flag = false;
            
            public void waitForFlag() {
                while (!flag) {
                    // Busy waiting - wastes CPU cycles
                    Thread.yield(); // Slightly better, but still wasteful
                }
                System.out.println("Flag became true (busy waiting)");
            }
            
            public void setFlag() {
                flag = true;
            }
        }
        
        System.out.println("✅ CORRECT: Proper waiting mechanisms");
        
        // Solution 1: CountDownLatch
        class CountDownLatchExample {
            private final CountDownLatch latch = new CountDownLatch(1);
            
            public void waitForSignal() throws InterruptedException {
                latch.await(); // Blocks efficiently
                System.out.println("Signal received (CountDownLatch)");
            }
            
            public void sendSignal() {
                latch.countDown();
            }
        }
        
        // Solution 2: CompletableFuture
        class CompletableFutureExample {
            private final CompletableFuture<Void> future = new CompletableFuture<>();
            
            public void waitForCompletion() {
                future.join(); // Blocks efficiently
                System.out.println("Completion received (CompletableFuture)");
            }
            
            public void complete() {
                future.complete(null);
            }
        }
        
        // Solution 3: BlockingQueue
        class BlockingQueueExample {
            private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
            
            public void waitForMessage() throws InterruptedException {
                String message = queue.take(); // Blocks efficiently
                System.out.println("Message received: " + message);
            }
            
            public void sendMessage(String message) {
                queue.offer(message);
            }
        }
        
        // Test proper waiting
        testProperWaiting();
    }
    
    private static void testProperWaiting() {
        CountDownLatch latch = new CountDownLatch(1);
        
        Thread waiter = new Thread(() -> {
            try {
                System.out.println("Waiting for signal...");
                latch.await();
                System.out.println("Signal received efficiently!");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        Thread signaler = new Thread(() -> {
            try {
                Thread.sleep(1000);
                System.out.println("Sending signal...");
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        waiter.start();
        signaler.start();
        
        try {
            waiter.join();
            signaler.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private static void demonstrateSharedMutableState() {
        System.out.println("❌ BROKEN: Uncontrolled shared mutable state");
        
        // Anti-pattern: Shared mutable state without proper synchronization
        class UnsafeSharedState {
            private Map<String, Integer> sharedMap = new HashMap<>(); // Not thread-safe
            
            public void updateCounter(String key) {
                Integer value = sharedMap.get(key);
                if (value == null) {
                    sharedMap.put(key, 1);
                } else {
                    sharedMap.put(key, value + 1); // Race condition!
                }
            }
            
            public int getCounter(String key) {
                Integer value = sharedMap.get(key);
                return value != null ? value : 0;
            }
        }
        
        System.out.println("✅ CORRECT: Proper state management patterns");
        
        // Solution 1: Immutable objects
        class ImmutableCounter {
            private final int value;
            
            public ImmutableCounter(int value) {
                this.value = value;
            }
            
            public ImmutableCounter increment() {
                return new ImmutableCounter(value + 1);
            }
            
            public int getValue() {
                return value;
            }
        }
        
        // Solution 2: Thread-safe collections
        class ThreadSafeSharedState {
            private final ConcurrentHashMap<String, AtomicReference<Integer>> counters = new ConcurrentHashMap<>();
            
            public void updateCounter(String key) {
                counters.computeIfAbsent(key, k -> new AtomicReference<>(0))
                        .updateAndGet(current -> current + 1);
            }
            
            public int getCounter(String key) {
                AtomicReference<Integer> ref = counters.get(key);
                return ref != null ? ref.get() : 0;
            }
        }
        
        // Solution 3: Actor-like pattern with single-threaded access
        class ActorLikeCounter {
            private final ExecutorService executor = Executors.newSingleThreadExecutor();
            private final Map<String, Integer> counters = new HashMap<>();
            
            public CompletableFuture<Void> updateCounter(String key) {
                return CompletableFuture.runAsync(() -> {
                    counters.merge(key, 1, Integer::sum);
                }, executor);
            }
            
            public CompletableFuture<Integer> getCounter(String key) {
                return CompletableFuture.supplyAsync(() -> {
                    return counters.getOrDefault(key, 0);
                }, executor);
            }
            
            public void shutdown() {
                executor.shutdown();
            }
        }
        
        testSharedStatePatterns();
    }
    
    private static void testSharedStatePatterns() {
        ThreadSafeSharedState safeState = new ThreadSafeSharedState();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(100);
        
        // 100 concurrent updates
        for (int i = 0; i < 100; i++) {
            executor.submit(() -> {
                try {
                    safeState.updateCounter("test");
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
        
        System.out.println("Thread-safe counter result: " + safeState.getCounter("test") + " (should be 100)");
        executor.shutdown();
    }
    
    private static void demonstrateThreadPoolMisuse() {
        System.out.println("❌ BROKEN: Thread pool anti-patterns");
        
        // Anti-pattern 1: Creating new thread pool for each task
        class ThreadPoolAntiPattern1 {
            public void processTasks(List<Runnable> tasks) {
                for (Runnable task : tasks) {
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    executor.submit(task);
                    executor.shutdown(); // Wasteful!
                }
            }
        }
        
        // Anti-pattern 2: Not shutting down thread pools
        class ThreadPoolAntiPattern2 {
            public void processTasks(List<Runnable> tasks) {
                ExecutorService executor = Executors.newFixedThreadPool(10);
                for (Runnable task : tasks) {
                    executor.submit(task);
                }
                // Missing: executor.shutdown() - resource leak!
            }
        }
        
        // Anti-pattern 3: Wrong pool size for workload
        class ThreadPoolAntiPattern3 {
            public void processIOTasks(List<Runnable> ioTasks) {
                // CPU cores for I/O tasks - underutilization!
                ExecutorService executor = Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors());
                
                for (Runnable task : ioTasks) {
                    executor.submit(task);
                }
                executor.shutdown();
            }
        }
        
        System.out.println("✅ CORRECT: Proper thread pool usage");
        
        class ProperThreadPoolUsage {
            private final ExecutorService cpuIntensivePool;
            private final ExecutorService ioIntensivePool;
            
            public ProperThreadPoolUsage() {
                int cores = Runtime.getRuntime().availableProcessors();
                
                // CPU-intensive: cores or cores + 1
                this.cpuIntensivePool = Executors.newFixedThreadPool(cores);
                
                // I/O-intensive: higher count to handle blocking
                this.ioIntensivePool = Executors.newFixedThreadPool(cores * 4);
            }
            
            public CompletableFuture<Void> processCpuTasks(List<Runnable> tasks) {
                List<CompletableFuture<Void>> futures = tasks.stream()
                    .map(task -> CompletableFuture.runAsync(task, cpuIntensivePool))
                    .toList();
                
                return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            }
            
            public CompletableFuture<Void> processIoTasks(List<Runnable> tasks) {
                List<CompletableFuture<Void>> futures = tasks.stream()
                    .map(task -> CompletableFuture.runAsync(task, ioIntensivePool))
                    .toList();
                
                return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            }
            
            public void shutdown() {
                cpuIntensivePool.shutdown();
                ioIntensivePool.shutdown();
                
                try {
                    if (!cpuIntensivePool.awaitTermination(60, TimeUnit.SECONDS)) {
                        cpuIntensivePool.shutdownNow();
                    }
                    if (!ioIntensivePool.awaitTermination(60, TimeUnit.SECONDS)) {
                        ioIntensivePool.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    cpuIntensivePool.shutdownNow();
                    ioIntensivePool.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        // Demonstrate proper usage
        ProperThreadPoolUsage properUsage = new ProperThreadPoolUsage();
        
        List<Runnable> cpuTasks = List.of(
            () -> { /* CPU work */ },
            () -> { /* CPU work */ },
            () -> { /* CPU work */ }
        );
        
        properUsage.processCpuTasks(cpuTasks).join();
        properUsage.shutdown();
        
        System.out.println("Proper thread pool usage completed");
    }
    
    private static void demonstrateDeadlockPatterns() {
        System.out.println("❌ BROKEN: Deadlock-prone patterns");
        
        // Anti-pattern: Inconsistent lock ordering
        class DeadlockPronePattern {
            private final Object lock1 = new Object();
            private final Object lock2 = new Object();
            
            public void method1() {
                synchronized (lock1) {
                    synchronized (lock2) {
                        // Work
                    }
                }
            }
            
            public void method2() {
                synchronized (lock2) { // Different order - deadlock risk!
                    synchronized (lock1) {
                        // Work
                    }
                }
            }
        }
        
        System.out.println("✅ CORRECT: Deadlock prevention patterns");
        
        // Solution 1: Consistent lock ordering
        class ConsistentLockOrdering {
            private final Object lock1 = new Object();
            private final Object lock2 = new Object();
            
            public void method1() {
                synchronized (lock1) {
                    synchronized (lock2) {
                        // Work
                    }
                }
            }
            
            public void method2() {
                synchronized (lock1) { // Same order - no deadlock
                    synchronized (lock2) {
                        // Work
                    }
                }
            }
        }
        
        // Solution 2: Lock timeout
        class TimeoutBasedSolution {
            private final ReentrantLock lock1 = new ReentrantLock();
            private final ReentrantLock lock2 = new ReentrantLock();
            
            public boolean method1() throws InterruptedException {
                if (lock1.tryLock(1, TimeUnit.SECONDS)) {
                    try {
                        if (lock2.tryLock(1, TimeUnit.SECONDS)) {
                            try {
                                // Work
                                return true;
                            } finally {
                                lock2.unlock();
                            }
                        }
                    } finally {
                        lock1.unlock();
                    }
                }
                return false; // Failed to acquire locks
            }
        }
        
        // Solution 3: Single lock for related operations
        class SingleLockSolution {
            private final Object combinedLock = new Object();
            
            public void method1() {
                synchronized (combinedLock) {
                    // All related work under single lock
                }
            }
            
            public void method2() {
                synchronized (combinedLock) {
                    // All related work under single lock
                }
            }
        }
        
        System.out.println("Deadlock prevention patterns demonstrated");
    }
}
```

---

## 📈 Part 3: Monitoring & Production Readiness (60 minutes)

### **Comprehensive Monitoring and Observability**

Create `ConcurrencyMonitoring.java`:

```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.lang.management.*;
import javax.management.*;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Production-ready monitoring and observability for concurrent applications
 */
public class ConcurrencyMonitoring {
    
    public static void main(String[] args) {
        System.out.println("=== Concurrency Monitoring & Observability ===\n");
        
        // 1. JVM and thread monitoring
        System.out.println("1. JVM and Thread Monitoring:");
        demonstrateJVMMonitoring();
        
        // 2. Custom metrics collection
        System.out.println("\n2. Custom Metrics Collection:");
        demonstrateCustomMetrics();
        
        // 3. Performance monitoring framework
        System.out.println("\n3. Performance Monitoring Framework:");
        demonstratePerformanceFramework();
        
        // 4. Health checks and circuit breakers
        System.out.println("\n4. Health Checks and Circuit Breakers:");
        demonstrateHealthChecks();
        
        System.out.println("\n=== Monitoring Demo Completed ===");
    }
    
    private static void demonstrateJVMMonitoring() {
        JVMMonitor monitor = new JVMMonitor();
        monitor.printCurrentState();
        
        // Run some concurrent work to see changes
        ExecutorService executor = Executors.newFixedThreadPool(8);
        
        for (int i = 0; i < 20; i++) {
            executor.submit(() -> {
                try {
                    // Simulate work
                    Thread.sleep(1000);
                    
                    // Create some garbage
                    List<Object> garbage = new ArrayList<>();
                    for (int j = 0; j < 1000; j++) {
                        garbage.add(new Object());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        try {
            Thread.sleep(2000);
            System.out.println("\nAfter creating concurrent load:");
            monitor.printCurrentState();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        executor.shutdown();
    }
    
    private static void demonstrateCustomMetrics() {
        MetricsCollector metrics = new MetricsCollector();
        
        // Simulate application with metrics
        ExecutorService executor = Executors.newFixedThreadPool(4);
        
        for (int i = 0; i < 100; i++) {
            final int taskId = i;
            executor.submit(() -> {
                long startTime = System.nanoTime();
                
                try {
                    // Simulate work
                    Thread.sleep(50 + (taskId % 100));
                    
                    metrics.recordSuccess();
                    
                } catch (InterruptedException e) {
                    metrics.recordError();
                    Thread.currentThread().interrupt();
                } finally {
                    long duration = System.nanoTime() - startTime;
                    metrics.recordLatency(duration / 1_000_000); // Convert to ms
                }
            });
        }
        
        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        metrics.printReport();
    }
    
    private static void demonstratePerformanceFramework() {
        PerformanceMonitor perfMonitor = new PerformanceMonitor();
        
        // Start monitoring
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        ScheduledFuture<?> monitoringTask = scheduler.scheduleAtFixedRate(
            perfMonitor::collectMetrics, 0, 1, TimeUnit.SECONDS);
        
        // Simulate varying load
        ExecutorService workExecutor = Executors.newFixedThreadPool(10);
        
        for (int i = 0; i < 50; i++) {
            final int iteration = i;
            workExecutor.submit(() -> {
                try {
                    // Variable work simulation
                    if (iteration % 10 == 0) {
                        // Heavy work every 10th iteration
                        Thread.sleep(500);
                    } else {
                        Thread.sleep(50);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Run for 10 seconds then stop
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        monitoringTask.cancel(false);
        perfMonitor.printSummary();
        
        workExecutor.shutdown();
        scheduler.shutdown();
    }
    
    private static void demonstrateHealthChecks() {
        HealthCheckSystem healthSystem = new HealthCheckSystem();
        
        // Register health checks
        healthSystem.registerHealthCheck("database", new DatabaseHealthCheck());
        healthSystem.registerHealthCheck("cache", new CacheHealthCheck());
        healthSystem.registerHealthCheck("threadpool", new ThreadPoolHealthCheck());
        
        // Simulate health monitoring
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        scheduler.scheduleAtFixedRate(() -> {
            HealthStatus overallHealth = healthSystem.checkOverallHealth();
            System.out.println("Overall health: " + overallHealth.status + 
                " (" + overallHealth.healthyChecks + "/" + overallHealth.totalChecks + " healthy)");
            
            if (!overallHealth.status.equals("HEALTHY")) {
                System.out.println("Unhealthy components: " + overallHealth.unhealthyComponents);
            }
        }, 0, 2, TimeUnit.SECONDS);
        
        // Run health checks for 10 seconds
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        scheduler.shutdown();
    }
}

/**
 * JVM monitoring utility
 */
class JVMMonitor {
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
    private final GarbageCollectorMXBean[] gcBeans = ManagementFactory.getGarbageCollectorMXBeans()
        .toArray(new GarbageCollectorMXBean[0]);
    
    public void printCurrentState() {
        System.out.println("=== JVM State ===");
        
        // Thread information
        System.out.println("Threads:");
        System.out.println("  Total: " + threadBean.getThreadCount());
        System.out.println("  Peak: " + threadBean.getPeakThreadCount());
        System.out.println("  Daemon: " + threadBean.getDaemonThreadCount());
        
        // Memory information
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        System.out.println("Memory:");
        System.out.println("  Heap Used: " + formatBytes(heapUsage.getUsed()) + 
            " / " + formatBytes(heapUsage.getMax()));
        System.out.println("  Non-Heap Used: " + formatBytes(nonHeapUsage.getUsed()) + 
            " / " + formatBytes(nonHeapUsage.getMax()));
        
        // GC information
        System.out.println("Garbage Collection:");
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            System.out.println("  " + gcBean.getName() + ": " + 
                gcBean.getCollectionCount() + " collections, " +
                gcBean.getCollectionTime() + "ms total");
        }
        
        // Runtime information
        System.out.println("Runtime:");
        System.out.println("  Uptime: " + runtimeBean.getUptime() + "ms");
        System.out.println("  Available Processors: " + Runtime.getRuntime().availableProcessors());
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 0) return "N/A";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}

/**
 * Custom metrics collection system
 */
class MetricsCollector {
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final LongAdder totalLatency = new LongAdder();
    private final AtomicLong operationCount = new AtomicLong(0);
    private final AtomicLong minLatency = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxLatency = new AtomicLong(0);
    
    public void recordSuccess() {
        successCount.incrementAndGet();
    }
    
    public void recordError() {
        errorCount.incrementAndGet();
    }
    
    public void recordLatency(long latencyMs) {
        totalLatency.add(latencyMs);
        operationCount.incrementAndGet();
        
        // Update min/max
        long current;
        do {
            current = minLatency.get();
        } while (latencyMs < current && !minLatency.compareAndSet(current, latencyMs));
        
        do {
            current = maxLatency.get();
        } while (latencyMs > current && !maxLatency.compareAndSet(current, latencyMs));
    }
    
    public void printReport() {
        long successes = successCount.get();
        long errors = errorCount.get();
        long total = successes + errors;
        long operations = operationCount.get();
        
        System.out.println("=== Metrics Report ===");
        System.out.println("Total Operations: " + total);
        System.out.println("Successes: " + successes + " (" + 
            String.format("%.2f", (successes * 100.0) / total) + "%)");
        System.out.println("Errors: " + errors + " (" + 
            String.format("%.2f", (errors * 100.0) / total) + "%)");
        
        if (operations > 0) {
            double avgLatency = totalLatency.sum() / (double) operations;
            System.out.println("Latency:");
            System.out.println("  Average: " + String.format("%.2f", avgLatency) + "ms");
            System.out.println("  Min: " + minLatency.get() + "ms");
            System.out.println("  Max: " + maxLatency.get() + "ms");
        }
    }
}

/**
 * Performance monitoring framework
 */
class PerformanceMonitor {
    private final List<PerformanceSnapshot> snapshots = new ArrayList<>();
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    
    public void collectMetrics() {
        PerformanceSnapshot snapshot = new PerformanceSnapshot(
            System.currentTimeMillis(),
            threadBean.getThreadCount(),
            memoryBean.getHeapMemoryUsage().getUsed(),
            getCurrentCpuUsage()
        );
        
        synchronized (snapshots) {
            snapshots.add(snapshot);
            System.out.println("Performance snapshot: Threads=" + snapshot.threadCount + 
                ", Memory=" + formatBytes(snapshot.memoryUsed) + 
                ", CPU=" + String.format("%.2f", snapshot.cpuUsage) + "%");
        }
    }
    
    public void printSummary() {
        synchronized (snapshots) {
            if (snapshots.isEmpty()) return;
            
            System.out.println("\n=== Performance Summary ===");
            System.out.println("Total snapshots: " + snapshots.size());
            
            double avgThreads = snapshots.stream().mapToInt(s -> s.threadCount).average().orElse(0);
            double avgMemory = snapshots.stream().mapToLong(s -> s.memoryUsed).average().orElse(0);
            double avgCpu = snapshots.stream().mapToDouble(s -> s.cpuUsage).average().orElse(0);
            
            System.out.println("Average threads: " + String.format("%.2f", avgThreads));
            System.out.println("Average memory: " + formatBytes((long) avgMemory));
            System.out.println("Average CPU: " + String.format("%.2f", avgCpu) + "%");
        }
    }
    
    private double getCurrentCpuUsage() {
        // Simplified CPU usage calculation
        return Math.random() * 100; // In real implementation, use OperatingSystemMXBean
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    private static class PerformanceSnapshot {
        final long timestamp;
        final int threadCount;
        final long memoryUsed;
        final double cpuUsage;
        
        PerformanceSnapshot(long timestamp, int threadCount, long memoryUsed, double cpuUsage) {
            this.timestamp = timestamp;
            this.threadCount = threadCount;
            this.memoryUsed = memoryUsed;
            this.cpuUsage = cpuUsage;
        }
    }
}

/**
 * Health check system
 */
class HealthCheckSystem {
    private final Map<String, HealthCheck> healthChecks = new ConcurrentHashMap<>();
    
    public void registerHealthCheck(String name, HealthCheck healthCheck) {
        healthChecks.put(name, healthCheck);
    }
    
    public HealthStatus checkOverallHealth() {
        int total = healthChecks.size();
        int healthy = 0;
        List<String> unhealthy = new ArrayList<>();
        
        for (Map.Entry<String, HealthCheck> entry : healthChecks.entrySet()) {
            try {
                if (entry.getValue().isHealthy()) {
                    healthy++;
                } else {
                    unhealthy.add(entry.getKey());
                }
            } catch (Exception e) {
                unhealthy.add(entry.getKey() + " (error: " + e.getMessage() + ")");
            }
        }
        
        String status = (healthy == total) ? "HEALTHY" : 
                       (healthy > total / 2) ? "DEGRADED" : "UNHEALTHY";
        
        return new HealthStatus(status, healthy, total, unhealthy);
    }
}

interface HealthCheck {
    boolean isHealthy();
}

class DatabaseHealthCheck implements HealthCheck {
    private int failureCount = 0;
    
    @Override
    public boolean isHealthy() {
        // Simulate occasional database issues
        if (Math.random() < 0.1) { // 10% failure rate
            failureCount++;
            return failureCount < 3; // Fail after 3 consecutive failures
        } else {
            failureCount = 0;
            return true;
        }
    }
}

class CacheHealthCheck implements HealthCheck {
    @Override
    public boolean isHealthy() {
        // Simulate cache health check
        return Math.random() > 0.05; // 5% failure rate
    }
}

class ThreadPoolHealthCheck implements HealthCheck {
    @Override
    public boolean isHealthy() {
        // Check thread pool health based on active threads
        int activeThreads = Thread.activeCount();
        return activeThreads < 100; // Arbitrary threshold
    }
}

class HealthStatus {
    final String status;
    final int healthyChecks;
    final int totalChecks;
    final List<String> unhealthyComponents;
    
    HealthStatus(String status, int healthyChecks, int totalChecks, List<String> unhealthyComponents) {
        this.status = status;
        this.healthyChecks = healthyChecks;
        this.totalChecks = totalChecks;
        this.unhealthyComponents = unhealthyComponents;
    }
}
```

---

## 🏆 Day 34 Summary

### **Key Concepts Mastered:**

✅ **Performance Measurement:**

- Throughput vs latency analysis
- Thread contention measurement
- Memory allocation impact assessment
- Thread pool sizing optimization

✅ **Anti-Pattern Recognition:**

- Double-checked locking pitfalls
- Busy waiting inefficiencies
- Shared mutable state problems
- Thread pool misuse patterns
- Deadlock-prone designs

✅ **Best Practices Implementation:**

- Proper singleton patterns
- Efficient waiting mechanisms
- Thread-safe state management
- Correct thread pool configuration
- Deadlock prevention strategies

✅ **Production Monitoring:**

- JVM metrics collection
- Custom application metrics
- Performance trend analysis
- Health check systems
- Circuit breaker patterns

✅ **Optimization Techniques:**

- Lock-free programming
- Memory-conscious design
- CPU utilization optimization
- Resource leak prevention
- Scalability planning

### **Production Applications:**

- High-performance web services
- Real-time data processing systems
- Microservices architectures
- Enterprise application monitoring
- System reliability engineering

### **Next Steps:**

Tomorrow we'll tackle the **Week 5 Capstone Project** - building a comprehensive concurrent task management system that integrates all the concepts learned throughout the week.

You're now equipped with the knowledge to build, monitor, and optimize production-ready concurrent applications! 🚀📊