# Day 30: Synchronization & Locks (August 8, 2025)

**Date:** August 8, 2025  
**Duration:** 3 hours  
**Focus:** Master thread synchronization, locks, and race condition prevention for safe concurrent programming

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:
- ✅ Understand race conditions and thread safety issues
- ✅ Master synchronized methods and blocks
- ✅ Learn about intrinsic locks and monitor concept
- ✅ Explore explicit locks (ReentrantLock, ReadWriteLock)
- ✅ Understand deadlock prevention and detection
- ✅ Implement thread-safe classes and data structures

---

## ⚠️ Part 1: Race Conditions & Thread Safety (60 minutes)

### **Understanding Race Conditions**

**Race Condition** occurs when multiple threads access shared data concurrently and the final outcome depends on the timing of their execution.

Create `RaceConditionDemo.java`:

```java
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Comprehensive demonstration of race conditions and their solutions
 */
public class RaceConditionDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Race Condition & Thread Safety Demo ===\n");
        
        // 1. Demonstrate race condition
        System.out.println("1. Race Condition Example:");
        demonstrateRaceCondition();
        
        // 2. Thread-safe solution using synchronization
        System.out.println("\n2. Thread-Safe Solution with Synchronization:");
        demonstrateSynchronizedSolution();
        
        // 3. Thread-safe solution using atomic variables
        System.out.println("\n3. Thread-Safe Solution with Atomic Variables:");
        demonstrateAtomicSolution();
        
        // 4. Banking system with race conditions
        System.out.println("\n4. Banking System Race Condition:");
        demonstrateBankingRaceCondition();
        
        System.out.println("\n=== Race Condition Demo Completed ===");
    }
    
    private static void demonstrateRaceCondition() {
        UnsafeCounter unsafeCounter = new UnsafeCounter();
        
        // Create multiple threads that increment the counter
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    unsafeCounter.increment();
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("Expected count: " + (10 * 1000));
        System.out.println("Actual count: " + unsafeCounter.getCount());
        System.out.println("Lost updates: " + (10000 - unsafeCounter.getCount()));
    }
    
    private static void demonstrateSynchronizedSolution() {
        SafeCounter safeCounter = new SafeCounter();
        
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    safeCounter.increment();
                }
            });
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("Expected count: " + (10 * 1000));
        System.out.println("Actual count: " + safeCounter.getCount());
        System.out.println("Lost updates: " + (10000 - safeCounter.getCount()));
    }
    
    private static void demonstrateAtomicSolution() {
        AtomicCounter atomicCounter = new AtomicCounter();
        
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    atomicCounter.increment();
                }
            });
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("Expected count: " + (10 * 1000));
        System.out.println("Actual count: " + atomicCounter.getCount());
        System.out.println("Lost updates: " + (10000 - atomicCounter.getCount()));
    }
    
    private static void demonstrateBankingRaceCondition() {
        UnsafeBankAccount account = new UnsafeBankAccount(1000.0);
        
        // Create multiple threads that deposit and withdraw
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(100);
        
        for (int i = 0; i < 50; i++) {
            executor.submit(() -> {
                try {
                    account.deposit(10.0);
                } finally {
                    latch.countDown();
                }
            });
            
            executor.submit(() -> {
                try {
                    account.withdraw(5.0);
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
        
        double expectedBalance = 1000.0 + (50 * 10.0) - (50 * 5.0); // Should be 1250.0
        System.out.println("Expected balance: " + expectedBalance);
        System.out.println("Actual balance: " + account.getBalance());
    }
}

/**
 * Unsafe counter with race condition
 */
class UnsafeCounter {
    private int count = 0;
    
    public void increment() {
        count++; // This is not atomic: read, modify, write
    }
    
    public int getCount() {
        return count;
    }
}

/**
 * Thread-safe counter using synchronization
 */
class SafeCounter {
    private int count = 0;
    
    public synchronized void increment() {
        count++;
    }
    
    public synchronized int getCount() {
        return count;
    }
}

/**
 * Thread-safe counter using atomic variables
 */
class AtomicCounter {
    private final AtomicInteger count = new AtomicInteger(0);
    
    public void increment() {
        count.incrementAndGet();
    }
    
    public int getCount() {
        return count.get();
    }
}

/**
 * Unsafe bank account with race conditions
 */
class UnsafeBankAccount {
    private double balance;
    
    public UnsafeBankAccount(double initialBalance) {
        this.balance = initialBalance;
    }
    
    public void deposit(double amount) {
        double newBalance = balance + amount;
        // Simulate some processing time
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        balance = newBalance;
    }
    
    public void withdraw(double amount) {
        if (balance >= amount) {
            double newBalance = balance - amount;
            // Simulate some processing time
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            balance = newBalance;
        }
    }
    
    public double getBalance() {
        return balance;
    }
}
```

### **Monitor Pattern and Wait/Notify**

Create `MonitorPatternDemo.java`:

```java
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

/**
 * Demonstration of monitor pattern using wait/notify
 */
public class MonitorPatternDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Monitor Pattern & Wait/Notify Demo ===\n");
        
        // Producer-Consumer with bounded buffer
        BoundedBuffer<String> buffer = new BoundedBuffer<>(5);
        
        // Create producer threads
        Thread producer1 = new Thread(new Producer(buffer, "Producer-1"), "Producer-1");
        Thread producer2 = new Thread(new Producer(buffer, "Producer-2"), "Producer-2");
        
        // Create consumer threads
        Thread consumer1 = new Thread(new Consumer(buffer, "Consumer-1"), "Consumer-1");
        Thread consumer2 = new Thread(new Consumer(buffer, "Consumer-2"), "Consumer-2");
        
        // Start all threads
        producer1.start();
        producer2.start();
        consumer1.start();
        consumer2.start();
        
        // Let them run for a while
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Stop all threads
        producer1.interrupt();
        producer2.interrupt();
        consumer1.interrupt();
        consumer2.interrupt();
        
        // Wait for threads to finish
        try {
            producer1.join();
            producer2.join();
            consumer1.join();
            consumer2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\n=== Monitor Pattern Demo Completed ===");
    }
}

/**
 * Thread-safe bounded buffer using monitor pattern
 */
class BoundedBuffer<T> {
    private final Queue<T> buffer = new LinkedList<>();
    private final int capacity;
    
    public BoundedBuffer(int capacity) {
        this.capacity = capacity;
    }
    
    public synchronized void put(T item) throws InterruptedException {
        while (buffer.size() == capacity) {
            System.out.println(Thread.currentThread().getName() + " waiting - buffer full");
            wait(); // Wait until space is available
        }
        
        buffer.offer(item);
        System.out.println(Thread.currentThread().getName() + " produced: " + item + 
            " (buffer size: " + buffer.size() + ")");
        
        notifyAll(); // Notify waiting consumers
    }
    
    public synchronized T take() throws InterruptedException {
        while (buffer.isEmpty()) {
            System.out.println(Thread.currentThread().getName() + " waiting - buffer empty");
            wait(); // Wait until item is available
        }
        
        T item = buffer.poll();
        System.out.println(Thread.currentThread().getName() + " consumed: " + item + 
            " (buffer size: " + buffer.size() + ")");
        
        notifyAll(); // Notify waiting producers
        return item;
    }
    
    public synchronized int size() {
        return buffer.size();
    }
    
    public synchronized boolean isEmpty() {
        return buffer.isEmpty();
    }
    
    public synchronized boolean isFull() {
        return buffer.size() == capacity;
    }
}

/**
 * Producer that generates items
 */
class Producer implements Runnable {
    private final BoundedBuffer<String> buffer;
    private final String name;
    private final Random random = new Random();
    private int itemCount = 0;
    
    public Producer(BoundedBuffer<String> buffer, String name) {
        this.buffer = buffer;
        this.name = name;
    }
    
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                String item = name + "-Item-" + (++itemCount);
                buffer.put(item);
                
                // Random production time
                Thread.sleep(500 + random.nextInt(1000));
            }
        } catch (InterruptedException e) {
            System.out.println(name + " interrupted");
        }
        System.out.println(name + " stopped");
    }
}

/**
 * Consumer that processes items
 */
class Consumer implements Runnable {
    private final BoundedBuffer<String> buffer;
    private final String name;
    private final Random random = new Random();
    
    public Consumer(BoundedBuffer<String> buffer, String name) {
        this.buffer = buffer;
        this.name = name;
    }
    
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                String item = buffer.take();
                
                // Simulate processing time
                Thread.sleep(800 + random.nextInt(1200));
            }
        } catch (InterruptedException e) {
            System.out.println(name + " interrupted");
        }
        System.out.println(name + " stopped");
    }
}
```

---

## 🔒 Part 2: Explicit Locks & Advanced Synchronization (60 minutes)

### **ReentrantLock and Lock Interface**

Create `ExplicitLocksDemo.java`:

```java
import java.util.concurrent.locks.*;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Comprehensive demonstration of explicit locks
 */
public class ExplicitLocksDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Explicit Locks Demo ===\n");
        
        // 1. ReentrantLock vs synchronized
        System.out.println("1. ReentrantLock vs Synchronized:");
        demonstrateReentrantLock();
        
        // 2. Lock with timeout
        System.out.println("\n2. Lock with Timeout:");
        demonstrateLockTimeout();
        
        // 3. ReadWriteLock
        System.out.println("\n3. ReadWriteLock:");
        demonstrateReadWriteLock();
        
        // 4. Condition variables
        System.out.println("\n4. Condition Variables:");
        demonstrateConditionVariables();
        
        System.out.println("\n=== Explicit Locks Demo Completed ===");
    }
    
    private static void demonstrateReentrantLock() {
        ReentrantLockExample example = new ReentrantLockExample();
        
        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            final int threadId = i + 1;
            threads[i] = new Thread(() -> {
                example.processWithLock("Thread-" + threadId);
            });
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    private static void demonstrateLockTimeout() {
        TimeoutLockExample example = new TimeoutLockExample();
        
        // Start a thread that holds the lock for a long time
        Thread lockHolder = new Thread(() -> example.holdLockLong(), "LockHolder");
        lockHolder.start();
        
        // Give lock holder time to acquire the lock
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Start threads that try to acquire lock with timeout
        Thread[] threads = new Thread[3];
        for (int i = 0; i < threads.length; i++) {
            final int threadId = i + 1;
            threads[i] = new Thread(() -> {
                example.tryLockWithTimeout("Thread-" + threadId);
            });
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        lockHolder.interrupt();
        try {
            lockHolder.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private static void demonstrateReadWriteLock() {
        ReadWriteLockExample example = new ReadWriteLockExample();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        // Start multiple readers
        for (int i = 1; i <= 5; i++) {
            final int readerId = i;
            executor.submit(() -> {
                for (int j = 1; j <= 3; j++) {
                    example.read("Reader-" + readerId);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            });
        }
        
        // Start a few writers
        for (int i = 1; i <= 2; i++) {
            final int writerId = i;
            executor.submit(() -> {
                for (int j = 1; j <= 2; j++) {
                    example.write("Writer-" + writerId, "Data-" + writerId + "-" + j);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            });
        }
        
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private static void demonstrateConditionVariables() {
        ConditionVariableExample example = new ConditionVariableExample();
        
        // Start producer
        Thread producer = new Thread(() -> {
            for (int i = 1; i <= 5; i++) {
                example.produce("Item-" + i);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "Producer");
        
        // Start consumers
        Thread consumer1 = new Thread(() -> {
            for (int i = 1; i <= 3; i++) {
                String item = example.consume("Consumer-1");
                if (item != null) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }, "Consumer-1");
        
        Thread consumer2 = new Thread(() -> {
            for (int i = 1; i <= 2; i++) {
                String item = example.consume("Consumer-2");
                if (item != null) {
                    try {
                        Thread.sleep(800);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }, "Consumer-2");
        
        producer.start();
        consumer1.start();
        consumer2.start();
        
        try {
            producer.join();
            consumer1.join();
            consumer2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

/**
 * Example demonstrating ReentrantLock
 */
class ReentrantLockExample {
    private final ReentrantLock lock = new ReentrantLock();
    private int counter = 0;
    
    public void processWithLock(String threadName) {
        lock.lock();
        try {
            System.out.println(threadName + " acquired lock");
            System.out.println(threadName + " - Lock hold count: " + lock.getHoldCount());
            
            // Simulate some work
            for (int i = 1; i <= 3; i++) {
                counter++;
                System.out.println(threadName + " - Counter: " + counter);
                
                // Demonstrate reentrancy
                if (i == 2) {
                    reentrantMethod(threadName);
                }
                
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            
            System.out.println(threadName + " - Queue length: " + lock.getQueueLength());
            
        } finally {
            System.out.println(threadName + " releasing lock");
            lock.unlock();
        }
    }
    
    private void reentrantMethod(String threadName) {
        lock.lock(); // Reentrant call
        try {
            System.out.println(threadName + " - Reentrant method, hold count: " + lock.getHoldCount());
        } finally {
            lock.unlock();
        }
    }
}

/**
 * Example demonstrating lock timeout
 */
class TimeoutLockExample {
    private final ReentrantLock lock = new ReentrantLock();
    
    public void holdLockLong() {
        lock.lock();
        try {
            System.out.println("LockHolder acquired lock for long operation");
            Thread.sleep(5000); // Hold lock for 5 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            System.out.println("LockHolder releasing lock");
            lock.unlock();
        }
    }
    
    public void tryLockWithTimeout(String threadName) {
        try {
            System.out.println(threadName + " attempting to acquire lock with 2-second timeout");
            
            if (lock.tryLock(2, TimeUnit.SECONDS)) {
                try {
                    System.out.println(threadName + " successfully acquired lock");
                    Thread.sleep(500);
                } finally {
                    System.out.println(threadName + " releasing lock");
                    lock.unlock();
                }
            } else {
                System.out.println(threadName + " failed to acquire lock within timeout");
            }
            
        } catch (InterruptedException e) {
            System.out.println(threadName + " interrupted while waiting for lock");
            Thread.currentThread().interrupt();
        }
    }
}

/**
 * Example demonstrating ReadWriteLock
 */
class ReadWriteLockExample {
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();
    
    private String data = "Initial Data";
    
    public void read(String readerName) {
        readLock.lock();
        try {
            System.out.println(readerName + " reading: " + data);
            // Simulate read operation
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            System.out.println(readerName + " finished reading");
            readLock.unlock();
        }
    }
    
    public void write(String writerName, String newData) {
        writeLock.lock();
        try {
            System.out.println(writerName + " writing: " + newData);
            this.data = newData;
            // Simulate write operation
            Thread.sleep(1500);
            System.out.println(writerName + " finished writing: " + newData);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            writeLock.unlock();
        }
    }
}

/**
 * Example demonstrating Condition variables
 */
class ConditionVariableExample {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();
    
    private final List<String> buffer = new ArrayList<>();
    private final int capacity = 3;
    
    public void produce(String item) {
        lock.lock();
        try {
            while (buffer.size() == capacity) {
                System.out.println("Producer waiting - buffer full");
                notFull.await();
            }
            
            buffer.add(item);
            System.out.println("Produced: " + item + " (buffer size: " + buffer.size() + ")");
            notEmpty.signalAll();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }
    
    public String consume(String consumerName) {
        lock.lock();
        try {
            while (buffer.isEmpty()) {
                System.out.println(consumerName + " waiting - buffer empty");
                notEmpty.await();
            }
            
            String item = buffer.remove(0);
            System.out.println(consumerName + " consumed: " + item + " (buffer size: " + buffer.size() + ")");
            notFull.signalAll();
            return item;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            lock.unlock();
        }
    }
}
```

---

## 💀 Part 3: Deadlock Prevention & Detection (60 minutes)

### **Deadlock Examples and Solutions**

Create `DeadlockDemo.java`:

```java
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;
import java.util.Random;

/**
 * Comprehensive demonstration of deadlocks and their solutions
 */
public class DeadlockDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Deadlock Prevention & Detection Demo ===\n");
        
        // 1. Classic deadlock scenario
        System.out.println("1. Classic Deadlock (will deadlock - uncomment to see):");
        // demonstrateClassicDeadlock(); // Uncomment with caution!
        System.out.println("   [Commented out to prevent actual deadlock]");
        
        // 2. Deadlock prevention with lock ordering
        System.out.println("\n2. Deadlock Prevention with Lock Ordering:");
        demonstrateLockOrdering();
        
        // 3. Deadlock prevention with timeout
        System.out.println("\n3. Deadlock Prevention with Timeout:");
        demonstrateTimeoutSolution();
        
        // 4. Dining philosophers problem
        System.out.println("\n4. Dining Philosophers Problem:");
        demonstrateDiningPhilosophers();
        
        System.out.println("\n=== Deadlock Demo Completed ===");
    }
    
    // WARNING: This method will cause a deadlock! Only use for demonstration
    @SuppressWarnings("unused")
    private static void demonstrateClassicDeadlock() {
        Object lock1 = new Object();
        Object lock2 = new Object();
        
        Thread thread1 = new Thread(() -> {
            synchronized (lock1) {
                System.out.println("Thread 1: Acquired lock1");
                
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                System.out.println("Thread 1: Waiting for lock2");
                synchronized (lock2) {
                    System.out.println("Thread 1: Acquired lock2");
                }
            }
        });
        
        Thread thread2 = new Thread(() -> {
            synchronized (lock2) {
                System.out.println("Thread 2: Acquired lock2");
                
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                System.out.println("Thread 2: Waiting for lock1");
                synchronized (lock1) {
                    System.out.println("Thread 2: Acquired lock1");
                }
            }
        });
        
        thread1.start();
        thread2.start();
        
        try {
            thread1.join(5000); // Wait max 5 seconds
            thread2.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Deadlock demonstration completed (or timed out)");
    }
    
    private static void demonstrateLockOrdering() {
        Resource resource1 = new Resource("Resource-1", 1);
        Resource resource2 = new Resource("Resource-2", 2);
        
        Thread thread1 = new Thread(new OrderedTask("Thread-1", resource1, resource2));
        Thread thread2 = new Thread(new OrderedTask("Thread-2", resource2, resource1));
        
        thread1.start();
        thread2.start();
        
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private static void demonstrateTimeoutSolution() {
        TimeoutResource resource1 = new TimeoutResource("TimeoutResource-1");
        TimeoutResource resource2 = new TimeoutResource("TimeoutResource-2");
        
        Thread thread1 = new Thread(new TimeoutTask("TimeoutThread-1", resource1, resource2));
        Thread thread2 = new Thread(new TimeoutTask("TimeoutThread-2", resource2, resource1));
        
        thread1.start();
        thread2.start();
        
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private static void demonstrateDiningPhilosophers() {
        int numPhilosophers = 5;
        Philosopher[] philosophers = new Philosopher[numPhilosophers];
        Chopstick[] chopsticks = new Chopstick[numPhilosophers];
        
        // Create chopsticks
        for (int i = 0; i < numPhilosophers; i++) {
            chopsticks[i] = new Chopstick(i);
        }
        
        // Create philosophers
        for (int i = 0; i < numPhilosophers; i++) {
            Chopstick left = chopsticks[i];
            Chopstick right = chopsticks[(i + 1) % numPhilosophers];
            
            // Break symmetry for last philosopher to prevent deadlock
            if (i == numPhilosophers - 1) {
                philosophers[i] = new Philosopher(i, right, left);
            } else {
                philosophers[i] = new Philosopher(i, left, right);
            }
        }
        
        // Start all philosophers
        Thread[] threads = new Thread[numPhilosophers];
        for (int i = 0; i < numPhilosophers; i++) {
            threads[i] = new Thread(philosophers[i]);
            threads[i].start();
        }
        
        // Let them dine for a while
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Stop all philosophers
        for (Philosopher philosopher : philosophers) {
            philosopher.stop();
        }
        
        // Wait for all to finish
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

/**
 * Resource with unique ID for lock ordering
 */
class Resource {
    private final String name;
    private final int id;
    
    public Resource(String name, int id) {
        this.name = name;
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public int getId() {
        return id;
    }
}

/**
 * Task that acquires locks in ordered manner to prevent deadlock
 */
class OrderedTask implements Runnable {
    private final String taskName;
    private final Resource resource1;
    private final Resource resource2;
    
    public OrderedTask(String taskName, Resource resource1, Resource resource2) {
        this.taskName = taskName;
        this.resource1 = resource1;
        this.resource2 = resource2;
    }
    
    @Override
    public void run() {
        // Always acquire locks in order of resource ID
        Resource firstLock = resource1.getId() < resource2.getId() ? resource1 : resource2;
        Resource secondLock = resource1.getId() < resource2.getId() ? resource2 : resource1;
        
        synchronized (firstLock) {
            System.out.println(taskName + " acquired " + firstLock.getName());
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            
            synchronized (secondLock) {
                System.out.println(taskName + " acquired " + secondLock.getName());
                System.out.println(taskName + " working with both resources");
                
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            
            System.out.println(taskName + " released " + secondLock.getName());
        }
        
        System.out.println(taskName + " released " + firstLock.getName());
        System.out.println(taskName + " completed");
    }
}

/**
 * Resource with explicit lock and timeout capability
 */
class TimeoutResource {
    private final String name;
    private final Lock lock = new ReentrantLock();
    
    public TimeoutResource(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        return lock.tryLock(timeout, unit);
    }
    
    public void unlock() {
        lock.unlock();
    }
}

/**
 * Task that uses timeout to prevent deadlock
 */
class TimeoutTask implements Runnable {
    private final String taskName;
    private final TimeoutResource resource1;
    private final TimeoutResource resource2;
    
    public TimeoutTask(String taskName, TimeoutResource resource1, TimeoutResource resource2) {
        this.taskName = taskName;
        this.resource1 = resource1;
        this.resource2 = resource2;
    }
    
    @Override
    public void run() {
        try {
            if (resource1.tryLock(1, TimeUnit.SECONDS)) {
                try {
                    System.out.println(taskName + " acquired " + resource1.getName());
                    
                    Thread.sleep(100);
                    
                    if (resource2.tryLock(1, TimeUnit.SECONDS)) {
                        try {
                            System.out.println(taskName + " acquired " + resource2.getName());
                            System.out.println(taskName + " working with both resources");
                            Thread.sleep(200);
                        } finally {
                            resource2.unlock();
                            System.out.println(taskName + " released " + resource2.getName());
                        }
                    } else {
                        System.out.println(taskName + " failed to acquire " + resource2.getName() + " within timeout");
                    }
                } finally {
                    resource1.unlock();
                    System.out.println(taskName + " released " + resource1.getName());
                }
            } else {
                System.out.println(taskName + " failed to acquire " + resource1.getName() + " within timeout");
            }
        } catch (InterruptedException e) {
            System.out.println(taskName + " interrupted");
            Thread.currentThread().interrupt();
        }
        
        System.out.println(taskName + " completed");
    }
}

/**
 * Chopstick for dining philosophers
 */
class Chopstick {
    private final int id;
    private final Lock lock = new ReentrantLock();
    
    public Chopstick(int id) {
        this.id = id;
    }
    
    public int getId() {
        return id;
    }
    
    public boolean pickUp() {
        return lock.tryLock();
    }
    
    public void putDown() {
        lock.unlock();
    }
}

/**
 * Philosopher for dining philosophers problem
 */
class Philosopher implements Runnable {
    private final int id;
    private final Chopstick left;
    private final Chopstick right;
    private final Random random = new Random();
    private volatile boolean running = true;
    
    public Philosopher(int id, Chopstick left, Chopstick right) {
        this.id = id;
        this.left = left;
        this.right = right;
    }
    
    @Override
    public void run() {
        try {
            while (running) {
                think();
                eat();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Philosopher " + id + " stopped");
    }
    
    private void think() throws InterruptedException {
        System.out.println("Philosopher " + id + " is thinking");
        Thread.sleep(random.nextInt(1000) + 500);
    }
    
    private void eat() throws InterruptedException {
        System.out.println("Philosopher " + id + " is hungry");
        
        if (left.pickUp()) {
            try {
                System.out.println("Philosopher " + id + " picked up left chopstick " + left.getId());
                
                if (right.pickUp()) {
                    try {
                        System.out.println("Philosopher " + id + " picked up right chopstick " + right.getId());
                        System.out.println("Philosopher " + id + " is eating");
                        Thread.sleep(random.nextInt(1000) + 500);
                    } finally {
                        right.putDown();
                        System.out.println("Philosopher " + id + " put down right chopstick " + right.getId());
                    }
                } else {
                    System.out.println("Philosopher " + id + " couldn't get right chopstick");
                }
            } finally {
                left.putDown();
                System.out.println("Philosopher " + id + " put down left chopstick " + left.getId());
            }
        } else {
            System.out.println("Philosopher " + id + " couldn't get left chopstick");
        }
    }
    
    public void stop() {
        running = false;
    }
}
```

---

## 🏆 Day 30 Summary

### **Key Concepts Mastered:**

✅ **Race Conditions & Thread Safety:**
- Understanding data races and their consequences
- Lost updates and inconsistent state problems
- Thread-safe programming principles

✅ **Synchronization Mechanisms:**
- synchronized methods and blocks
- Intrinsic locks and monitor pattern
- wait(), notify(), and notifyAll() coordination

✅ **Explicit Locks:**
- ReentrantLock advantages over synchronized
- Lock timeout and interruption handling
- ReadWriteLock for reader-writer scenarios
- Condition variables for advanced coordination

✅ **Deadlock Prevention:**
- Lock ordering strategy
- Timeout-based approaches
- Breaking circular wait conditions
- Dining philosophers problem solutions

✅ **Best Practices:**
- Always use try-finally with explicit locks
- Prefer java.util.concurrent utilities
- Test for race conditions and deadlocks
- Monitor lock contention and performance

### **Practical Applications:**
- Thread-safe data structures
- Producer-consumer systems
- Resource pooling
- Database connection management
- Cache implementations

### **Next Steps:**
Tomorrow we'll explore the **Executor Framework** - learning how to manage thread pools and execute tasks efficiently without manual thread management.

You now have solid foundations in thread synchronization and can write safe concurrent programs! 🔒✨