# Day 32: Concurrent Collections (August 10, 2025)

**Date:** August 10, 2025  
**Duration:** 3 hours  
**Focus:** Master thread-safe collections and concurrent data structures for high-performance multi-threaded applications

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:
- ✅ Understand thread-safe collection alternatives to standard collections
- ✅ Master ConcurrentHashMap and its advanced features
- ✅ Learn BlockingQueue implementations for producer-consumer patterns
- ✅ Explore CopyOnWriteArrayList and CopyOnWriteArraySet
- ✅ Understand atomic collections and their performance characteristics
- ✅ Build high-performance concurrent applications with proper collection usage

---

## 📚 Part 1: Thread-Safe Collection Fundamentals (60 minutes)

### **Problems with Standard Collections**

Standard Java collections (ArrayList, HashMap, HashSet) are **not thread-safe**. Using them in concurrent environments can lead to:
- **Data corruption**
- **Infinite loops**
- **Lost updates**
- **Inconsistent state**

### **Thread-Safe Collection Comparison**

Create `ConcurrentCollectionsOverview.java`:

```java
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive overview of concurrent collections vs standard collections
 */
public class ConcurrentCollectionsOverview {
    
    public static void main(String[] args) {
        System.out.println("=== Concurrent Collections Overview ===\n");
        
        // 1. Demonstrate problems with non-thread-safe collections
        System.out.println("1. Problems with Standard Collections:");
        demonstrateStandardCollectionProblems();
        
        // 2. Thread-safe alternatives comparison
        System.out.println("\n2. Thread-Safe Alternatives:");
        demonstrateThreadSafeAlternatives();
        
        // 3. Performance comparison
        System.out.println("\n3. Performance Comparison:");
        performanceComparison();
        
        System.out.println("\n=== Concurrent Collections Overview Completed ===");
    }
    
    private static void demonstrateStandardCollectionProblems() {
        System.out.println("Using ArrayList (not thread-safe):");
        
        List<Integer> unsafeList = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);
        
        // Multiple threads adding to ArrayList
        for (int i = 0; i < 10; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        unsafeList.add(threadId * 100 + j);
                    }
                } catch (Exception e) {
                    System.out.println("Exception in thread " + threadId + ": " + e.getMessage());
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
        
        System.out.println("Expected size: 1000, Actual size: " + unsafeList.size());
        System.out.println("Data loss: " + (1000 - unsafeList.size()) + " elements");
        
        executor.shutdown();
    }
    
    private static void demonstrateThreadSafeAlternatives() {
        System.out.println("Thread-Safe Collection Alternatives:");
        
        // CopyOnWriteArrayList
        List<Integer> copyOnWriteList = new CopyOnWriteArrayList<>();
        
        // Vector (synchronized)
        List<Integer> vectorList = new Vector<>();
        
        // Collections.synchronizedList
        List<Integer> synchronizedList = Collections.synchronizedList(new ArrayList<>());
        
        // ConcurrentHashMap
        Map<String, Integer> concurrentMap = new ConcurrentHashMap<>();
        
        // Hashtable (synchronized)
        Map<String, Integer> hashtable = new Hashtable<>();
        
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(15); // 3 tasks per collection
        
        // Test CopyOnWriteArrayList
        for (int i = 0; i < 3; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 50; j++) {
                        copyOnWriteList.add(threadId * 50 + j);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Test Vector
        for (int i = 0; i < 3; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 50; j++) {
                        vectorList.add(threadId * 50 + j);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Test ConcurrentHashMap
        for (int i = 0; i < 3; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 50; j++) {
                        concurrentMap.put("key-" + threadId + "-" + j, threadId * 50 + j);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Additional tests for synchronized collections
        for (int i = 0; i < 6; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    if (threadId < 3) {
                        for (int j = 0; j < 50; j++) {
                            synchronizedList.add(threadId * 50 + j);
                        }
                    } else {
                        for (int j = 0; j < 50; j++) {
                            hashtable.put("key-" + threadId + "-" + j, threadId * 50 + j);
                        }
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
        
        System.out.println("CopyOnWriteArrayList size: " + copyOnWriteList.size() + " (expected: 150)");
        System.out.println("Vector size: " + vectorList.size() + " (expected: 150)");
        System.out.println("SynchronizedList size: " + synchronizedList.size() + " (expected: 150)");
        System.out.println("ConcurrentHashMap size: " + concurrentMap.size() + " (expected: 150)");
        System.out.println("Hashtable size: " + hashtable.size() + " (expected: 150)");
        
        executor.shutdown();
    }
    
    private static void performanceComparison() {
        System.out.println("Performance Comparison (1000 operations per thread):");
        
        int numThreads = 10;
        int operationsPerThread = 1000;
        
        // Test different collections
        testCollectionPerformance("CopyOnWriteArrayList", 
            new CopyOnWriteArrayList<>(), numThreads, operationsPerThread);
        
        testCollectionPerformance("Vector", 
            new Vector<>(), numThreads, operationsPerThread);
        
        testCollectionPerformance("SynchronizedList", 
            Collections.synchronizedList(new ArrayList<>()), numThreads, operationsPerThread);
        
        testMapPerformance("ConcurrentHashMap", 
            new ConcurrentHashMap<>(), numThreads, operationsPerThread);
        
        testMapPerformance("Hashtable", 
            new Hashtable<>(), numThreads, operationsPerThread);
    }
    
    private static void testCollectionPerformance(String name, List<Integer> collection, 
                                                 int numThreads, int operationsPerThread) {
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        collection.add(threadId * operationsPerThread + j);
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
        long durationMs = (endTime - startTime) / 1_000_000;
        
        System.out.println(name + ": " + durationMs + "ms, Size: " + collection.size());
        
        executor.shutdown();
    }
    
    private static void testMapPerformance(String name, Map<String, Integer> map, 
                                          int numThreads, int operationsPerThread) {
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        map.put("key-" + threadId + "-" + j, threadId * operationsPerThread + j);
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
        long durationMs = (endTime - startTime) / 1_000_000;
        
        System.out.println(name + ": " + durationMs + "ms, Size: " + map.size());
        
        executor.shutdown();
    }
}
```

---

## 🗺️ Part 2: ConcurrentHashMap Deep Dive (60 minutes)

### **Advanced ConcurrentHashMap Features**

Create `ConcurrentHashMapDemo.java`:

```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Comprehensive demonstration of ConcurrentHashMap features
 */
public class ConcurrentHashMapDemo {
    
    public static void main(String[] args) {
        System.out.println("=== ConcurrentHashMap Deep Dive ===\n");
        
        // 1. Basic thread-safe operations
        System.out.println("1. Basic Thread-Safe Operations:");
        demonstrateBasicOperations();
        
        // 2. Atomic operations
        System.out.println("\n2. Atomic Operations:");
        demonstrateAtomicOperations();
        
        // 3. Bulk operations
        System.out.println("\n3. Bulk Operations:");
        demonstrateBulkOperations();
        
        // 4. Real-world usage patterns
        System.out.println("\n4. Real-World Usage Patterns:");
        demonstrateRealWorldPatterns();
        
        System.out.println("\n=== ConcurrentHashMap Demo Completed ===");
    }
    
    private static void demonstrateBasicOperations() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(4);
        
        // Thread 1: Put operations
        executor.submit(() -> {
            try {
                for (int i = 1; i <= 100; i++) {
                    map.put("key" + i, i);
                }
                System.out.println("Thread 1: Added 100 entries");
            } finally {
                latch.countDown();
            }
        });
        
        // Thread 2: Get operations
        executor.submit(() -> {
            try {
                Thread.sleep(100); // Let some entries be added first
                int found = 0;
                for (int i = 1; i <= 100; i++) {
                    if (map.get("key" + i) != null) {
                        found++;
                    }
                }
                System.out.println("Thread 2: Found " + found + " entries");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });
        
        // Thread 3: Remove operations
        executor.submit(() -> {
            try {
                Thread.sleep(200); // Let entries be added
                int removed = 0;
                for (int i = 50; i <= 75; i++) {
                    if (map.remove("key" + i) != null) {
                        removed++;
                    }
                }
                System.out.println("Thread 3: Removed " + removed + " entries");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });
        
        // Thread 4: Iteration (safe during modifications)
        executor.submit(() -> {
            try {
                Thread.sleep(300); // Let some operations complete
                int count = 0;
                for (Map.Entry<String, Integer> entry : map.entrySet()) {
                    count++;
                }
                System.out.println("Thread 4: Iterated over " + count + " entries");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });
        
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Final map size: " + map.size());
        executor.shutdown();
    }
    
    private static void demonstrateAtomicOperations() {
        ConcurrentHashMap<String, AtomicInteger> counterMap = new ConcurrentHashMap<>();
        
        // Initialize counters
        for (int i = 1; i <= 5; i++) {
            counterMap.put("counter" + i, new AtomicInteger(0));
        }
        
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);
        
        // Multiple threads incrementing counters
        for (int i = 0; i < 10; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        String key = "counter" + ((j % 5) + 1);
                        
                        // Atomic increment using computeIfPresent
                        counterMap.computeIfPresent(key, (k, v) -> {
                            v.incrementAndGet();
                            return v;
                        });
                    }
                    System.out.println("Thread " + threadId + " completed increments");
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
        
        System.out.println("Counter results:");
        counterMap.forEach((key, value) -> {
            System.out.println("  " + key + ": " + value.get());
        });
        
        // Demonstrate other atomic operations
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        
        // putIfAbsent
        Integer previous = map.putIfAbsent("key1", 100);
        System.out.println("putIfAbsent result: " + previous); // null
        
        previous = map.putIfAbsent("key1", 200);
        System.out.println("putIfAbsent result: " + previous); // 100
        
        // replace
        boolean replaced = map.replace("key1", 100, 150);
        System.out.println("replace result: " + replaced); // true
        
        // compute
        map.compute("key2", (key, value) -> value == null ? 1 : value + 1);
        System.out.println("compute result: " + map.get("key2")); // 1
        
        map.compute("key2", (key, value) -> value == null ? 1 : value + 1);
        System.out.println("compute result: " + map.get("key2")); // 2
        
        // merge
        map.merge("key3", 10, Integer::sum);
        System.out.println("merge result: " + map.get("key3")); // 10
        
        map.merge("key3", 20, Integer::sum);
        System.out.println("merge result: " + map.get("key3")); // 30
        
        executor.shutdown();
    }
    
    private static void demonstrateBulkOperations() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        
        // Populate map
        IntStream.range(1, 1001).forEach(i -> map.put("key" + i, i));
        
        System.out.println("Map populated with " + map.size() + " entries");
        
        // forEach - parallel iteration
        System.out.println("Parallel forEach:");
        long startTime = System.nanoTime();
        
        AtomicInteger sumAtomic = new AtomicInteger(0);
        map.forEach(1, (key, value) -> {
            // Simulate some work
            if (value % 2 == 0) {
                sumAtomic.addAndGet(value);
            }
        });
        
        long endTime = System.nanoTime();
        System.out.println("Sum of even values: " + sumAtomic.get());
        System.out.println("Time: " + (endTime - startTime) / 1_000_000 + "ms");
        
        // search - find first match
        System.out.println("\nParallel search:");
        String result = map.search(1, (key, value) -> {
            return value > 500 ? key : null;
        });
        System.out.println("First key with value > 500: " + result);
        
        // reduce - aggregate operation
        System.out.println("\nParallel reduce:");
        Integer maxValue = map.reduce(1,
            (key, value) -> value,                    // transformer
            Integer::max                              // reducer
        );
        System.out.println("Maximum value: " + maxValue);
        
        // forEachKey - keys only
        System.out.println("\nProcessing keys only:");
        AtomicInteger keyCount = new AtomicInteger(0);
        map.forEachKey(1, key -> {
            if (key.contains("10")) {
                keyCount.incrementAndGet();
            }
        });
        System.out.println("Keys containing '10': " + keyCount.get());
        
        // forEachValue - values only
        System.out.println("\nProcessing values only:");
        Integer sumOfSquares = map.reduceValues(1,
            value -> value * value,                   // transformer
            Integer::sum                              // reducer
        );
        System.out.println("Sum of squares (first 10): " + 
            sumOfSquares.toString().substring(0, Math.min(10, sumOfSquares.toString().length())));
    }
    
    private static void demonstrateRealWorldPatterns() {
        // 1. Cache implementation
        System.out.println("Cache Implementation Pattern:");
        Cache<String, String> cache = new Cache<>(3); // 3 second TTL
        
        cache.put("user:123", "John Doe");
        cache.put("user:456", "Jane Smith");
        
        System.out.println("Cache get: " + cache.get("user:123"));
        
        try {
            Thread.sleep(4000); // Wait for expiration
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Cache get after expiration: " + cache.get("user:123"));
        
        // 2. Frequency counter
        System.out.println("\nFrequency Counter Pattern:");
        FrequencyCounter<String> counter = new FrequencyCounter<>();
        
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(5);
        
        String[] words = {"apple", "banana", "cherry", "apple", "banana", "apple"};
        
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    for (String word : words) {
                        counter.increment(word);
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
        
        System.out.println("Word frequencies:");
        counter.getTopEntries(3).forEach((word, count) -> {
            System.out.println("  " + word + ": " + count);
        });
        
        executor.shutdown();
    }
}

/**
 * Simple cache implementation using ConcurrentHashMap
 */
class Cache<K, V> {
    private final ConcurrentHashMap<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
    private final long ttlSeconds;
    
    public Cache(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }
    
    public void put(K key, V value) {
        long expirationTime = System.currentTimeMillis() + (ttlSeconds * 1000);
        cache.put(key, new CacheEntry<>(value, expirationTime));
    }
    
    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        
        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        
        return entry.getValue();
    }
    
    public void remove(K key) {
        cache.remove(key);
    }
    
    public int size() {
        // Clean expired entries
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        return cache.size();
    }
    
    private static class CacheEntry<V> {
        private final V value;
        private final long expirationTime;
        
        public CacheEntry(V value, long expirationTime) {
            this.value = value;
            this.expirationTime = expirationTime;
        }
        
        public V getValue() {
            return value;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }
}

/**
 * Thread-safe frequency counter
 */
class FrequencyCounter<T> {
    private final ConcurrentHashMap<T, AtomicInteger> counts = new ConcurrentHashMap<>();
    
    public void increment(T item) {
        counts.computeIfAbsent(item, k -> new AtomicInteger(0)).incrementAndGet();
    }
    
    public int getCount(T item) {
        AtomicInteger count = counts.get(item);
        return count != null ? count.get() : 0;
    }
    
    public Map<T, Integer> getTopEntries(int limit) {
        return counts.entrySet().stream()
            .sorted(Map.Entry.<T, AtomicInteger>comparingByValue(
                (a, b) -> Integer.compare(b.get(), a.get())))
            .limit(limit)
            .collect(ConcurrentHashMap::new,
                (map, entry) -> map.put(entry.getKey(), entry.getValue().get()),
                ConcurrentHashMap::putAll);
    }
    
    public void reset() {
        counts.clear();
    }
}
```

---

## 🚫 Part 3: BlockingQueue and Producer-Consumer Patterns (60 minutes)

### **BlockingQueue Implementations**

Create `BlockingQueueDemo.java`:

```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;

/**
 * Comprehensive demonstration of BlockingQueue implementations
 */
public class BlockingQueueDemo {
    
    public static void main(String[] args) {
        System.out.println("=== BlockingQueue Comprehensive Demo ===\n");
        
        // 1. ArrayBlockingQueue
        System.out.println("1. ArrayBlockingQueue (bounded):");
        demonstrateArrayBlockingQueue();
        
        // 2. LinkedBlockingQueue
        System.out.println("\n2. LinkedBlockingQueue (optionally bounded):");
        demonstrateLinkedBlockingQueue();
        
        // 3. PriorityBlockingQueue
        System.out.println("\n3. PriorityBlockingQueue (unbounded, ordered):");
        demonstratePriorityBlockingQueue();
        
        // 4. SynchronousQueue
        System.out.println("\n4. SynchronousQueue (zero capacity):");
        demonstrateSynchronousQueue();
        
        // 5. DelayQueue
        System.out.println("\n5. DelayQueue (delayed elements):");
        demonstrateDelayQueue();
        
        System.out.println("\n=== BlockingQueue Demo Completed ===");
    }
    
    private static void demonstrateArrayBlockingQueue() {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(5);
        
        // Producer thread
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    String item = "Item-" + i;
                    queue.put(item); // Blocks when queue is full
                    System.out.println("Produced: " + item + " (queue size: " + queue.size() + ")");
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Consumer thread
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    String item = queue.take(); // Blocks when queue is empty
                    System.out.println("Consumed: " + item + " (queue size: " + queue.size() + ")");
                    Thread.sleep(300); // Slower consumer
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        producer.start();
        consumer.start();
        
        try {
            producer.join();
            consumer.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("ArrayBlockingQueue demo completed");
    }
    
    private static void demonstrateLinkedBlockingQueue() {
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(3); // Bounded to 3
        
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(4);
        
        // Fast producer
        executor.submit(() -> {
            try {
                for (int i = 1; i <= 8; i++) {
                    queue.put(i);
                    System.out.println("FastProducer put: " + i);
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });
        
        // Slow producer
        executor.submit(() -> {
            try {
                for (int i = 100; i <= 103; i++) {
                    queue.put(i);
                    System.out.println("SlowProducer put: " + i);
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });
        
        // Consumer 1
        executor.submit(() -> {
            try {
                for (int i = 0; i < 6; i++) {
                    Integer item = queue.take();
                    System.out.println("Consumer1 took: " + item);
                    Thread.sleep(150);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });
        
        // Consumer 2
        executor.submit(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    Integer item = queue.take();
                    System.out.println("Consumer2 took: " + item);
                    Thread.sleep(180);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });
        
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        executor.shutdown();
        System.out.println("LinkedBlockingQueue demo completed, remaining: " + queue.size());
    }
    
    private static void demonstratePriorityBlockingQueue() {
        BlockingQueue<Task> queue = new PriorityBlockingQueue<>();
        
        // Producer adding tasks with different priorities
        Thread producer = new Thread(() -> {
            Random random = new Random();
            try {
                for (int i = 1; i <= 10; i++) {
                    Priority priority = Priority.values()[random.nextInt(Priority.values().length)];
                    Task task = new Task("Task-" + i, priority);
                    queue.put(task);
                    System.out.println("Added: " + task);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Consumer processing tasks by priority
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    Task task = queue.take();
                    System.out.println("Processing: " + task);
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        producer.start();
        consumer.start();
        
        try {
            producer.join();
            consumer.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("PriorityBlockingQueue demo completed");
    }
    
    private static void demonstrateSynchronousQueue() {
        SynchronousQueue<String> queue = new SynchronousQueue<>();
        
        // Producer (must wait for consumer)
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    String item = "SyncItem-" + i;
                    System.out.println("Producer offering: " + item);
                    queue.put(item); // Blocks until consumer takes
                    System.out.println("Producer successfully handed off: " + item);
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Consumer (must be ready to receive)
        Thread consumer = new Thread(() -> {
            try {
                Thread.sleep(200); // Start slightly later
                for (int i = 1; i <= 5; i++) {
                    System.out.println("Consumer ready to take...");
                    String item = queue.take(); // Blocks until producer offers
                    System.out.println("Consumer received: " + item);
                    Thread.sleep(800);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        producer.start();
        consumer.start();
        
        try {
            producer.join();
            consumer.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("SynchronousQueue demo completed");
    }
    
    private static void demonstrateDelayQueue() {
        DelayQueue<DelayedTask> queue = new DelayQueue<>();
        
        // Add tasks with different delays
        queue.put(new DelayedTask("Task-1", 1000)); // 1 second
        queue.put(new DelayedTask("Task-2", 3000)); // 3 seconds
        queue.put(new DelayedTask("Task-3", 2000)); // 2 seconds
        queue.put(new DelayedTask("Task-4", 500));  // 0.5 seconds
        
        System.out.println("Added 4 delayed tasks at " + System.currentTimeMillis());
        
        // Consumer that processes tasks only when delay expires
        Thread consumer = new Thread(() -> {
            try {
                while (!queue.isEmpty()) {
                    DelayedTask task = queue.take(); // Blocks until delay expires
                    System.out.println("Executed: " + task.getName() + 
                        " at " + System.currentTimeMillis());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        consumer.start();
        
        try {
            consumer.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("DelayQueue demo completed");
    }
}

/**
 * Task with priority for PriorityBlockingQueue
 */
class Task implements Comparable<Task> {
    private final String name;
    private final Priority priority;
    
    public Task(String name, Priority priority) {
        this.name = name;
        this.priority = priority;
    }
    
    @Override
    public int compareTo(Task other) {
        // Higher priority tasks come first
        return Integer.compare(other.priority.ordinal(), this.priority.ordinal());
    }
    
    @Override
    public String toString() {
        return name + " (" + priority + ")";
    }
    
    public String getName() {
        return name;
    }
    
    public Priority getPriority() {
        return priority;
    }
}

enum Priority {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * Delayed task for DelayQueue
 */
class DelayedTask implements Delayed {
    private final String name;
    private final long startTime;
    
    public DelayedTask(String name, long delayInMillis) {
        this.name = name;
        this.startTime = System.currentTimeMillis() + delayInMillis;
    }
    
    @Override
    public long getDelay(TimeUnit unit) {
        long delay = startTime - System.currentTimeMillis();
        return unit.convert(delay, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public int compareTo(Delayed other) {
        return Long.compare(this.getDelay(TimeUnit.MILLISECONDS), 
                           other.getDelay(TimeUnit.MILLISECONDS));
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return name + " (delay: " + getDelay(TimeUnit.MILLISECONDS) + "ms)";
    }
}
```

### **Real-World Producer-Consumer Applications**

Create `ProducerConsumerPatterns.java`:

```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Random;

/**
 * Real-world producer-consumer patterns and applications
 */
public class ProducerConsumerPatterns {
    
    public static void main(String[] args) {
        System.out.println("=== Producer-Consumer Patterns ===\n");
        
        // 1. File Processing System
        System.out.println("1. File Processing System:");
        demonstrateFileProcessing();
        
        // 2. Web Request Processing
        System.out.println("\n2. Web Request Processing:");
        demonstrateWebRequestProcessing();
        
        // 3. Event Processing System
        System.out.println("\n3. Event Processing System:");
        demonstrateEventProcessing();
        
        System.out.println("\n=== Producer-Consumer Patterns Completed ===");
    }
    
    private static void demonstrateFileProcessing() {
        BlockingQueue<FileTask> fileQueue = new ArrayBlockingQueue<>(10);
        AtomicInteger processedFiles = new AtomicInteger(0);
        
        // File scanner (producer)
        Thread fileScanner = new Thread(() -> {
            try {
                for (int i = 1; i <= 15; i++) {
                    FileTask task = new FileTask("file" + i + ".txt", FileType.values()[i % 3]);
                    fileQueue.put(task);
                    System.out.println("Scanned: " + task.getFileName());
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("File scanning completed");
        });
        
        // File processors (consumers)
        ExecutorService processors = Executors.newFixedThreadPool(3);
        for (int i = 1; i <= 3; i++) {
            final int processorId = i;
            processors.submit(() -> {
                try {
                    while (true) {
                        FileTask task = fileQueue.poll(2, TimeUnit.SECONDS);
                        if (task == null) break; // Timeout, assume no more files
                        
                        System.out.println("Processor-" + processorId + 
                            " processing: " + task.getFileName() + 
                            " (type: " + task.getType() + ")");
                        
                        // Simulate processing time based on file type
                        Thread.sleep(task.getType().getProcessingTime());
                        
                        processedFiles.incrementAndGet();
                        System.out.println("Processor-" + processorId + 
                            " completed: " + task.getFileName());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        fileScanner.start();
        
        try {
            fileScanner.join();
            processors.shutdown();
            processors.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Total files processed: " + processedFiles.get());
    }
    
    private static void demonstrateWebRequestProcessing() {
        BlockingQueue<WebRequest> requestQueue = new LinkedBlockingQueue<>(20);
        AtomicInteger processedRequests = new AtomicInteger(0);
        AtomicInteger totalResponseTime = new AtomicInteger(0);
        
        // Request generator (simulating web traffic)
        Thread requestGenerator = new Thread(() -> {
            Random random = new Random();
            try {
                for (int i = 1; i <= 25; i++) {
                    RequestType type = RequestType.values()[random.nextInt(RequestType.values().length)];
                    WebRequest request = new WebRequest("REQ-" + i, type);
                    
                    if (requestQueue.offer(request, 1, TimeUnit.SECONDS)) {
                        System.out.println("Received request: " + request.getId() + 
                            " (" + request.getType() + ")");
                    } else {
                        System.out.println("Request queue full, dropping: " + request.getId());
                    }
                    
                    Thread.sleep(50 + random.nextInt(100)); // Variable request rate
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("Request generation completed");
        });
        
        // Request processors (worker threads)
        ExecutorService workers = Executors.newFixedThreadPool(4);
        for (int i = 1; i <= 4; i++) {
            final int workerId = i;
            workers.submit(() -> {
                try {
                    while (true) {
                        WebRequest request = requestQueue.poll(3, TimeUnit.SECONDS);
                        if (request == null) break; // Timeout
                        
                        long startTime = System.currentTimeMillis();
                        System.out.println("Worker-" + workerId + 
                            " processing: " + request.getId());
                        
                        // Simulate request processing
                        Thread.sleep(request.getType().getProcessingTime());
                        
                        long responseTime = System.currentTimeMillis() - startTime;
                        totalResponseTime.addAndGet((int) responseTime);
                        processedRequests.incrementAndGet();
                        
                        System.out.println("Worker-" + workerId + 
                            " completed: " + request.getId() + 
                            " (response time: " + responseTime + "ms)");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        requestGenerator.start();
        
        try {
            requestGenerator.join();
            workers.shutdown();
            workers.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Processed requests: " + processedRequests.get());
        if (processedRequests.get() > 0) {
            System.out.println("Average response time: " + 
                (totalResponseTime.get() / processedRequests.get()) + "ms");
        }
    }
    
    private static void demonstrateEventProcessing() {
        // Use DelayQueue for event scheduling
        DelayQueue<ScheduledEvent> eventQueue = new DelayQueue<>();
        AtomicInteger processedEvents = new AtomicInteger(0);
        
        // Event scheduler
        Thread scheduler = new Thread(() -> {
            Random random = new Random();
            long currentTime = System.currentTimeMillis();
            
            // Schedule events at different times
            for (int i = 1; i <= 10; i++) {
                long delay = random.nextInt(5000) + 500; // 0.5 to 5.5 seconds
                EventType type = EventType.values()[random.nextInt(EventType.values().length)];
                ScheduledEvent event = new ScheduledEvent("EVENT-" + i, type, delay);
                
                eventQueue.put(event);
                System.out.println("Scheduled: " + event.getId() + 
                    " (" + event.getType() + ") at +" + delay + "ms");
            }
            System.out.println("Event scheduling completed");
        });
        
        // Event processor
        Thread processor = new Thread(() -> {
            try {
                int processed = 0;
                while (processed < 10) {
                    ScheduledEvent event = eventQueue.take();
                    
                    System.out.println("Processing event: " + event.getId() + 
                        " (" + event.getType() + ") at " + System.currentTimeMillis());
                    
                    // Simulate event processing
                    Thread.sleep(event.getType().getProcessingTime());
                    
                    processedEvents.incrementAndGet();
                    processed++;
                    
                    System.out.println("Completed event: " + event.getId());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        scheduler.start();
        processor.start();
        
        try {
            scheduler.join();
            processor.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Total events processed: " + processedEvents.get());
    }
}

// Supporting classes for file processing
class FileTask {
    private final String fileName;
    private final FileType type;
    
    public FileTask(String fileName, FileType type) {
        this.fileName = fileName;
        this.type = type;
    }
    
    public String getFileName() { return fileName; }
    public FileType getType() { return type; }
}

enum FileType {
    TEXT(200), IMAGE(800), VIDEO(2000);
    
    private final int processingTime;
    
    FileType(int processingTime) {
        this.processingTime = processingTime;
    }
    
    public int getProcessingTime() { return processingTime; }
}

// Supporting classes for web request processing
class WebRequest {
    private final String id;
    private final RequestType type;
    private final long timestamp;
    
    public WebRequest(String id, RequestType type) {
        this.id = id;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getId() { return id; }
    public RequestType getType() { return type; }
    public long getTimestamp() { return timestamp; }
}

enum RequestType {
    GET(100), POST(300), PUT(250), DELETE(150);
    
    private final int processingTime;
    
    RequestType(int processingTime) {
        this.processingTime = processingTime;
    }
    
    public int getProcessingTime() { return processingTime; }
}

// Supporting classes for event processing
class ScheduledEvent implements Delayed {
    private final String id;
    private final EventType type;
    private final long executeTime;
    
    public ScheduledEvent(String id, EventType type, long delayMs) {
        this.id = id;
        this.type = type;
        this.executeTime = System.currentTimeMillis() + delayMs;
    }
    
    @Override
    public long getDelay(TimeUnit unit) {
        long delay = executeTime - System.currentTimeMillis();
        return unit.convert(delay, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public int compareTo(Delayed other) {
        return Long.compare(this.getDelay(TimeUnit.MILLISECONDS),
                           other.getDelay(TimeUnit.MILLISECONDS));
    }
    
    public String getId() { return id; }
    public EventType getType() { return type; }
}

enum EventType {
    NOTIFICATION(100), BACKUP(500), CLEANUP(300), REPORT(400);
    
    private final int processingTime;
    
    EventType(int processingTime) {
        this.processingTime = processingTime;
    }
    
    public int getProcessingTime() { return processingTime; }
}
```

---

## 🏆 Day 32 Summary

### **Key Concepts Mastered:**

✅ **Thread-Safe Collections:**
- Problems with standard collections in concurrent environments
- CopyOnWriteArrayList for read-heavy scenarios
- Vector and Collections.synchronizedList() limitations
- Performance characteristics and trade-offs

✅ **ConcurrentHashMap Mastery:**
- Lock-free read operations and segment-based locking
- Atomic operations (putIfAbsent, replace, compute, merge)
- Bulk operations for parallel processing
- Real-world patterns (caching, frequency counting)

✅ **BlockingQueue Implementations:**
- ArrayBlockingQueue (bounded, FIFO)
- LinkedBlockingQueue (optionally bounded)
- PriorityBlockingQueue (unbounded, ordered)
- SynchronousQueue (zero capacity, handoff)
- DelayQueue (time-based delays)

✅ **Producer-Consumer Patterns:**
- File processing systems
- Web request handling
- Event scheduling and processing
- Performance optimization techniques

✅ **Best Practices:**
- Choose appropriate collection for use case
- Understand performance characteristics
- Proper exception handling in concurrent code
- Monitor queue sizes and processing rates

### **Production Applications:**
- High-throughput web servers
- Batch processing systems
- Real-time event processing
- Caching solutions
- Background job queues

### **Next Steps:**
Tomorrow we'll explore **CompletableFuture & Async Programming** - learning modern asynchronous programming patterns and reactive programming concepts.

You're now equipped to build high-performance concurrent applications with thread-safe data structures! 🚀📊