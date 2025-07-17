# Day 29: Thread Creation & Lifecycle (August 7, 2025)

**Date:** August 7, 2025  
**Duration:** 3 hours  
**Focus:** Master Java thread creation, lifecycle management, and foundational concurrency concepts

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:
- ✅ Understand thread fundamentals and the Java Thread Model
- ✅ Master three ways to create threads in Java
- ✅ Understand thread states and lifecycle management
- ✅ Learn thread priority, daemon threads, and thread groups
- ✅ Handle thread interruption and graceful shutdown
- ✅ Build multi-threaded applications with proper coordination

---

## 🧵 Part 1: Thread Fundamentals & Creation (60 minutes)

### **Understanding Threads**

**Thread** is the smallest unit of execution in Java. Key concepts:
- **Process**: Independent execution environment with its own memory space
- **Thread**: Lightweight subprocess sharing memory with other threads
- **Multithreading**: Multiple threads executing concurrently
- **Concurrency**: Multiple threads making progress simultaneously
- **Parallelism**: Multiple threads executing simultaneously on multiple cores

### **Thread Creation Methods**

Create `ThreadCreationDemo.java`:

```java
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * Comprehensive demonstration of thread creation methods
 * Shows three different ways to create threads in Java
 */
public class ThreadCreationDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Thread Creation Methods Demo ===\n");
        
        System.out.println("Main thread: " + Thread.currentThread().getName());
        System.out.println("Main thread ID: " + Thread.currentThread().getId());
        System.out.println("Available processors: " + Runtime.getRuntime().availableProcessors());
        System.out.println();
        
        // Method 1: Extending Thread class
        System.out.println("1. Creating threads by extending Thread class:");
        CustomThread thread1 = new CustomThread("Worker-1");
        CustomThread thread2 = new CustomThread("Worker-2");
        
        thread1.start();
        thread2.start();
        
        // Method 2: Implementing Runnable interface
        System.out.println("\n2. Creating threads by implementing Runnable:");
        TaskRunner task1 = new TaskRunner("Task-A");
        TaskRunner task2 = new TaskRunner("Task-B");
        
        Thread thread3 = new Thread(task1);
        Thread thread4 = new Thread(task2);
        
        thread3.start();
        thread4.start();
        
        // Method 3: Using lambda expressions (Java 8+)
        System.out.println("\n3. Creating threads using lambda expressions:");
        Thread thread5 = new Thread(() -> {
            for (int i = 1; i <= 5; i++) {
                System.out.println("Lambda Thread: Iteration " + i + 
                    " at " + getCurrentTime());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println("Lambda thread interrupted");
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        
        thread5.setName("Lambda-Worker");
        thread5.start();
        
        // Method 4: Using anonymous inner class
        System.out.println("\n4. Creating threads using anonymous inner class:");
        Thread thread6 = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 1; i <= 3; i++) {
                    System.out.println("Anonymous Thread: Processing " + i + 
                        " at " + getCurrentTime());
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        System.out.println("Anonymous thread interrupted");
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        });
        
        thread6.setName("Anonymous-Worker");
        thread6.start();
        
        // Wait for all threads to complete
        try {
            thread1.join();
            thread2.join();
            thread3.join();
            thread4.join();
            thread5.join();
            thread6.join();
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted while waiting");
        }
        
        System.out.println("\n=== All threads completed ===");
        System.out.println("Main thread ending at " + getCurrentTime());
    }
    
    private static String getCurrentTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }
}

/**
 * Method 1: Extending Thread class
 * Direct inheritance from Thread class
 */
class CustomThread extends Thread {
    private String taskName;
    
    public CustomThread(String taskName) {
        this.taskName = taskName;
    }
    
    @Override
    public void run() {
        System.out.println(taskName + " started by " + Thread.currentThread().getName());
        
        for (int i = 1; i <= 4; i++) {
            System.out.println(taskName + ": Step " + i + " at " + getCurrentTime());
            
            try {
                Thread.sleep(800); // Simulate work
            } catch (InterruptedException e) {
                System.out.println(taskName + " was interrupted");
                Thread.currentThread().interrupt();
                return;
            }
        }
        
        System.out.println(taskName + " completed!");
    }
    
    private String getCurrentTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }
}

/**
 * Method 2: Implementing Runnable interface
 * More flexible approach allowing inheritance from other classes
 */
class TaskRunner implements Runnable {
    private String taskName;
    private Random random = new Random();
    
    public TaskRunner(String taskName) {
        this.taskName = taskName;
    }
    
    @Override
    public void run() {
        System.out.println(taskName + " started by " + Thread.currentThread().getName());
        
        for (int i = 1; i <= 4; i++) {
            // Simulate variable work time
            int workTime = 500 + random.nextInt(1000);
            
            System.out.println(taskName + ": Processing item " + i + 
                " (estimated " + workTime + "ms) at " + getCurrentTime());
            
            try {
                Thread.sleep(workTime);
            } catch (InterruptedException e) {
                System.out.println(taskName + " was interrupted during processing");
                Thread.currentThread().interrupt();
                return;
            }
        }
        
        System.out.println(taskName + " completed successfully!");
    }
    
    private String getCurrentTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }
}
```

### **Thread Information & Properties**

Create `ThreadPropertiesDemo.java`:

```java
import java.util.Map;

/**
 * Comprehensive demonstration of thread properties and information
 */
public class ThreadPropertiesDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Thread Properties & Information Demo ===\n");
        
        // Current thread information
        Thread currentThread = Thread.currentThread();
        displayThreadInfo("Current Thread", currentThread);
        
        // Create threads with different properties
        System.out.println("\n--- Creating threads with different properties ---");
        
        // Thread with custom name and priority
        Thread highPriorityThread = new Thread(new PriorityTask("High Priority Task"));
        highPriorityThread.setName("HighPriorityWorker");
        highPriorityThread.setPriority(Thread.MAX_PRIORITY);
        
        Thread normalPriorityThread = new Thread(new PriorityTask("Normal Priority Task"));
        normalPriorityThread.setName("NormalPriorityWorker");
        normalPriorityThread.setPriority(Thread.NORM_PRIORITY);
        
        Thread lowPriorityThread = new Thread(new PriorityTask("Low Priority Task"));
        lowPriorityThread.setName("LowPriorityWorker");
        lowPriorityThread.setPriority(Thread.MIN_PRIORITY);
        
        // Daemon thread
        Thread daemonThread = new Thread(new DaemonTask());
        daemonThread.setName("DaemonWorker");
        daemonThread.setDaemon(true);
        
        // Display thread information before starting
        displayThreadInfo("High Priority Thread", highPriorityThread);
        displayThreadInfo("Normal Priority Thread", normalPriorityThread);
        displayThreadInfo("Low Priority Thread", lowPriorityThread);
        displayThreadInfo("Daemon Thread", daemonThread);
        
        // Start threads
        System.out.println("\n--- Starting threads ---");
        highPriorityThread.start();
        normalPriorityThread.start();
        lowPriorityThread.start();
        daemonThread.start();
        
        // Monitor thread states
        System.out.println("\n--- Monitoring thread states ---");
        monitorThreadStates(highPriorityThread, normalPriorityThread, lowPriorityThread);
        
        // Display all threads in current process
        System.out.println("\n--- All threads in current process ---");
        displayAllThreads();
        
        // Wait for non-daemon threads to complete
        try {
            highPriorityThread.join();
            normalPriorityThread.join();
            lowPriorityThread.join();
            // Note: We don't join daemon thread as it will stop when main thread ends
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted while waiting");
        }
        
        System.out.println("\n=== Thread Properties Demo Completed ===");
    }
    
    private static void displayThreadInfo(String label, Thread thread) {
        System.out.println(label + ":");
        System.out.println("  Name: " + thread.getName());
        System.out.println("  ID: " + thread.getId());
        System.out.println("  Priority: " + thread.getPriority());
        System.out.println("  State: " + thread.getState());
        System.out.println("  Is Daemon: " + thread.isDaemon());
        System.out.println("  Is Alive: " + thread.isAlive());
        System.out.println("  Thread Group: " + thread.getThreadGroup().getName());
        System.out.println();
    }
    
    private static void monitorThreadStates(Thread... threads) {
        for (int i = 0; i < 10; i++) {
            System.out.println("--- State Check " + (i + 1) + " ---");
            for (Thread thread : threads) {
                System.out.println(thread.getName() + ": " + thread.getState());
            }
            System.out.println();
            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                System.out.println("Monitor thread interrupted");
                return;
            }
        }
    }
    
    private static void displayAllThreads() {
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while (rootGroup.getParent() != null) {
            rootGroup = rootGroup.getParent();
        }
        
        Thread[] threads = new Thread[rootGroup.activeCount()];
        int count = rootGroup.enumerate(threads, true);
        
        System.out.println("Active threads (" + count + "):");
        for (int i = 0; i < count; i++) {
            Thread thread = threads[i];
            System.out.println("  " + thread.getName() + " - " + thread.getState() + 
                " (Priority: " + thread.getPriority() + ", Daemon: " + thread.isDaemon() + ")");
        }
        System.out.println();
    }
}

/**
 * Task that demonstrates priority effects
 */
class PriorityTask implements Runnable {
    private String taskName;
    private static final AtomicInteger counter = new AtomicInteger(0);
    
    public PriorityTask(String taskName) {
        this.taskName = taskName;
    }
    
    @Override
    public void run() {
        System.out.println(taskName + " started at " + getCurrentTime());
        
        // CPU-intensive task to demonstrate priority
        long startTime = System.currentTimeMillis();
        long endTime = startTime + 2000; // Run for 2 seconds
        
        int iterations = 0;
        while (System.currentTimeMillis() < endTime) {
            iterations++;
            
            // Simulate some work
            Math.sqrt(iterations);
            
            // Periodically yield to other threads
            if (iterations % 10000 == 0) {
                System.out.println(taskName + " - Iterations: " + iterations + 
                    " at " + getCurrentTime());
                Thread.yield();
            }
        }
        
        System.out.println(taskName + " completed with " + iterations + 
            " iterations in " + (System.currentTimeMillis() - startTime) + "ms");
    }
    
    private String getCurrentTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }
}

/**
 * Daemon thread task
 */
class DaemonTask implements Runnable {
    @Override
    public void run() {
        System.out.println("Daemon thread started - will run until main thread ends");
        
        try {
            while (true) {
                System.out.println("Daemon thread working... " + getCurrentTime());
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            System.out.println("Daemon thread interrupted");
        }
    }
    
    private String getCurrentTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }
}
```

---

## 🔄 Part 2: Thread Lifecycle & States (60 minutes)

### **Thread States and Transitions**

Create `ThreadLifecycleDemo.java`:

```java
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Comprehensive demonstration of thread lifecycle and states
 * Shows all possible thread states and transitions
 */
public class ThreadLifecycleDemo {
    
    private static final Lock sharedLock = new ReentrantLock();
    private static final Object monitor = new Object();
    
    public static void main(String[] args) {
        System.out.println("=== Thread Lifecycle & States Demo ===\n");
        
        // 1. NEW state
        Thread newThread = new Thread(new StateTask("NEW-Thread"));
        System.out.println("1. NEW state: " + newThread.getState());
        
        // 2. RUNNABLE state
        Thread runnableThread = new Thread(new StateTask("RUNNABLE-Thread"));
        runnableThread.start();
        System.out.println("2. RUNNABLE state: " + runnableThread.getState());
        
        // 3. BLOCKED state
        Thread blockedThread1 = new Thread(new BlockedTask("BLOCKED-Thread-1"));
        Thread blockedThread2 = new Thread(new BlockedTask("BLOCKED-Thread-2"));
        
        blockedThread1.start();
        sleep(100); // Let first thread acquire the lock
        blockedThread2.start();
        sleep(100); // Let second thread get blocked
        
        System.out.println("3. BLOCKED state: " + blockedThread2.getState());
        
        // 4. WAITING state
        Thread waitingThread = new Thread(new WaitingTask("WAITING-Thread"));
        waitingThread.start();
        sleep(100); // Let thread start waiting
        
        System.out.println("4. WAITING state: " + waitingThread.getState());
        
        // 5. TIMED_WAITING state
        Thread timedWaitingThread = new Thread(new TimedWaitingTask("TIMED_WAITING-Thread"));
        timedWaitingThread.start();
        sleep(100); // Let thread start timed waiting
        
        System.out.println("5. TIMED_WAITING state: " + timedWaitingThread.getState());
        
        // 6. TERMINATED state will be shown after threads complete
        
        // Monitor state changes
        System.out.println("\n--- Monitoring state changes ---");
        StateMonitor monitor1 = new StateMonitor(newThread, "NEW-Thread");
        StateMonitor monitor2 = new StateMonitor(runnableThread, "RUNNABLE-Thread");
        StateMonitor monitor3 = new StateMonitor(blockedThread1, "BLOCKED-Thread-1");
        StateMonitor monitor4 = new StateMonitor(blockedThread2, "BLOCKED-Thread-2");
        StateMonitor monitor5 = new StateMonitor(waitingThread, "WAITING-Thread");
        StateMonitor monitor6 = new StateMonitor(timedWaitingThread, "TIMED_WAITING-Thread");
        
        Thread[] monitors = {
            new Thread(monitor1),
            new Thread(monitor2),
            new Thread(monitor3),
            new Thread(monitor4),
            new Thread(monitor5),
            new Thread(monitor6)
        };
        
        for (Thread monitor : monitors) {
            monitor.start();
        }
        
        // Start the NEW thread after monitoring begins
        sleep(500);
        newThread.start();
        
        // Release waiting thread after some time
        sleep(2000);
        synchronized (monitor) {
            monitor.notify();
        }
        
        // Wait for all threads to complete
        try {
            newThread.join();
            runnableThread.join();
            blockedThread1.join();
            blockedThread2.join();
            waitingThread.join();
            timedWaitingThread.join();
            
            // Stop monitors
            for (Thread monitorThread : monitors) {
                monitorThread.interrupt();
            }
            
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted");
        }
        
        // Show TERMINATED state
        System.out.println("\n6. TERMINATED state: " + newThread.getState());
        
        System.out.println("\n=== Thread Lifecycle Demo Completed ===");
    }
    
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

/**
 * Basic task for demonstrating states
 */
class StateTask implements Runnable {
    private String taskName;
    
    public StateTask(String taskName) {
        this.taskName = taskName;
    }
    
    @Override
    public void run() {
        System.out.println(taskName + " started - State: " + Thread.currentThread().getState());
        
        for (int i = 1; i <= 3; i++) {
            System.out.println(taskName + " - Working iteration " + i);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                System.out.println(taskName + " interrupted");
                Thread.currentThread().interrupt();
                return;
            }
        }
        
        System.out.println(taskName + " completed");
    }
}

/**
 * Task that will be blocked waiting for a lock
 */
class BlockedTask implements Runnable {
    private String taskName;
    
    public BlockedTask(String taskName) {
        this.taskName = taskName;
    }
    
    @Override
    public void run() {
        System.out.println(taskName + " attempting to acquire lock...");
        
        synchronized (ThreadLifecycleDemo.class) {
            System.out.println(taskName + " acquired lock - State: " + Thread.currentThread().getState());
            
            try {
                Thread.sleep(2000); // Hold lock for 2 seconds
            } catch (InterruptedException e) {
                System.out.println(taskName + " interrupted");
                Thread.currentThread().interrupt();
                return;
            }
            
            System.out.println(taskName + " releasing lock");
        }
        
        System.out.println(taskName + " completed");
    }
}

/**
 * Task that waits indefinitely
 */
class WaitingTask implements Runnable {
    private String taskName;
    
    public WaitingTask(String taskName) {
        this.taskName = taskName;
    }
    
    @Override
    public void run() {
        System.out.println(taskName + " started");
        
        synchronized (ThreadLifecycleDemo.monitor) {
            try {
                System.out.println(taskName + " entering WAITING state...");
                ThreadLifecycleDemo.monitor.wait();
                System.out.println(taskName + " resumed from WAITING state");
            } catch (InterruptedException e) {
                System.out.println(taskName + " interrupted while waiting");
                Thread.currentThread().interrupt();
                return;
            }
        }
        
        System.out.println(taskName + " completed");
    }
}

/**
 * Task that waits for a specific time
 */
class TimedWaitingTask implements Runnable {
    private String taskName;
    
    public TimedWaitingTask(String taskName) {
        this.taskName = taskName;
    }
    
    @Override
    public void run() {
        System.out.println(taskName + " started");
        
        try {
            System.out.println(taskName + " entering TIMED_WAITING state for 3 seconds...");
            Thread.sleep(3000);
            System.out.println(taskName + " resumed from TIMED_WAITING state");
        } catch (InterruptedException e) {
            System.out.println(taskName + " interrupted while in timed waiting");
            Thread.currentThread().interrupt();
            return;
        }
        
        System.out.println(taskName + " completed");
    }
}

/**
 * Monitor to track thread state changes
 */
class StateMonitor implements Runnable {
    private Thread targetThread;
    private String threadName;
    
    public StateMonitor(Thread targetThread, String threadName) {
        this.targetThread = targetThread;
        this.threadName = threadName;
    }
    
    @Override
    public void run() {
        Thread.State previousState = targetThread.getState();
        System.out.println("Monitor: " + threadName + " initial state: " + previousState);
        
        try {
            while (!Thread.currentThread().isInterrupted() && targetThread.getState() != Thread.State.TERMINATED) {
                Thread.sleep(100);
                
                Thread.State currentState = targetThread.getState();
                if (currentState != previousState) {
                    System.out.println("Monitor: " + threadName + " state changed: " + 
                        previousState + " → " + currentState);
                    previousState = currentState;
                }
            }
            
            System.out.println("Monitor: " + threadName + " final state: " + targetThread.getState());
        } catch (InterruptedException e) {
            System.out.println("Monitor for " + threadName + " interrupted");
        }
    }
}
```

---

## 🛑 Part 3: Thread Interruption & Coordination (60 minutes)

### **Thread Interruption and Graceful Shutdown**

Create `ThreadInterruptionDemo.java`:

```java
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive demonstration of thread interruption and graceful shutdown
 */
public class ThreadInterruptionDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Thread Interruption & Coordination Demo ===\n");
        
        // 1. Basic interruption handling
        System.out.println("1. Basic interruption handling:");
        Thread basicTask = new Thread(new BasicInterruptibleTask("BasicTask"));
        basicTask.start();
        
        try {
            Thread.sleep(2000);
            System.out.println("Main thread interrupting BasicTask...");
            basicTask.interrupt();
            basicTask.join();
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted");
        }
        
        // 2. Handling interruption during blocking operations
        System.out.println("\n2. Interruption during blocking operations:");
        Thread blockingTask = new Thread(new BlockingInterruptibleTask("BlockingTask"));
        blockingTask.start();
        
        try {
            Thread.sleep(1000);
            System.out.println("Main thread interrupting BlockingTask...");
            blockingTask.interrupt();
            blockingTask.join();
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted");
        }
        
        // 3. Proper cleanup during interruption
        System.out.println("\n3. Proper cleanup during interruption:");
        Thread cleanupTask = new Thread(new CleanupTask("CleanupTask"));
        cleanupTask.start();
        
        try {
            Thread.sleep(2000);
            System.out.println("Main thread interrupting CleanupTask...");
            cleanupTask.interrupt();
            cleanupTask.join();
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted");
        }
        
        // 4. Cooperative cancellation
        System.out.println("\n4. Cooperative cancellation:");
        CooperativeTask cooperativeTask = new CooperativeTask("CooperativeTask");
        Thread cooperativeThread = new Thread(cooperativeTask);
        cooperativeThread.start();
        
        try {
            Thread.sleep(2000);
            System.out.println("Main thread requesting cooperative cancellation...");
            cooperativeTask.cancel();
            cooperativeThread.join();
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted");
        }
        
        // 5. Thread coordination with join
        System.out.println("\n5. Thread coordination with join:");
        demonstrateJoinCoordination();
        
        // 6. Producer-Consumer coordination
        System.out.println("\n6. Producer-Consumer coordination:");
        demonstrateProducerConsumer();
        
        System.out.println("\n=== Thread Interruption Demo Completed ===");
    }
    
    private static void demonstrateJoinCoordination() {
        Thread[] workers = new Thread[3];
        
        for (int i = 0; i < workers.length; i++) {
            final int workerId = i + 1;
            workers[i] = new Thread(() -> {
                System.out.println("Worker " + workerId + " starting...");
                try {
                    Thread.sleep(1000 + (workerId * 500));
                    System.out.println("Worker " + workerId + " completed");
                } catch (InterruptedException e) {
                    System.out.println("Worker " + workerId + " interrupted");
                }
            });
            workers[i].setName("Worker-" + workerId);
            workers[i].start();
        }
        
        // Wait for all workers to complete
        try {
            for (Thread worker : workers) {
                worker.join();
            }
            System.out.println("All workers completed - main thread continuing");
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted while waiting for workers");
        }
    }
    
    private static void demonstrateProducerConsumer() {
        SharedBuffer buffer = new SharedBuffer();
        
        Thread producer = new Thread(new Producer(buffer), "Producer");
        Thread consumer = new Thread(new Consumer(buffer), "Consumer");
        
        producer.start();
        consumer.start();
        
        try {
            Thread.sleep(5000);
            producer.interrupt();
            consumer.interrupt();
            
            producer.join();
            consumer.join();
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted during producer-consumer demo");
        }
    }
}

/**
 * Basic task that handles interruption properly
 */
class BasicInterruptibleTask implements Runnable {
    private String taskName;
    
    public BasicInterruptibleTask(String taskName) {
        this.taskName = taskName;
    }
    
    @Override
    public void run() {
        System.out.println(taskName + " started");
        
        try {
            int counter = 0;
            while (!Thread.currentThread().isInterrupted()) {
                counter++;
                System.out.println(taskName + " - Counter: " + counter);
                
                // Check interruption periodically
                if (counter % 5 == 0) {
                    System.out.println(taskName + " - Checking interruption status...");
                    if (Thread.currentThread().isInterrupted()) {
                        System.out.println(taskName + " - Interruption detected!");
                        break;
                    }
                }
                
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            System.out.println(taskName + " - Interrupted during sleep");
            Thread.currentThread().interrupt(); // Restore interrupt status
        }
        
        System.out.println(taskName + " - Shutting down gracefully");
    }
}

/**
 * Task that handles interruption during blocking operations
 */
class BlockingInterruptibleTask implements Runnable {
    private String taskName;
    
    public BlockingInterruptibleTask(String taskName) {
        this.taskName = taskName;
    }
    
    @Override
    public void run() {
        System.out.println(taskName + " started");
        
        try {
            System.out.println(taskName + " - Entering blocking operation...");
            Thread.sleep(5000); // Long sleep to demonstrate interruption
            System.out.println(taskName + " - Blocking operation completed");
        } catch (InterruptedException e) {
            System.out.println(taskName + " - Interrupted during blocking operation");
            System.out.println(taskName + " - Interrupt status: " + Thread.currentThread().isInterrupted());
            Thread.currentThread().interrupt(); // Restore interrupt status
        }
        
        System.out.println(taskName + " - Completed");
    }
}

/**
 * Task that performs proper cleanup during interruption
 */
class CleanupTask implements Runnable {
    private String taskName;
    
    public CleanupTask(String taskName) {
        this.taskName = taskName;
    }
    
    @Override
    public void run() {
        System.out.println(taskName + " started with cleanup responsibility");
        
        try {
            // Simulate resource acquisition
            System.out.println(taskName + " - Acquiring resources...");
            Thread.sleep(500);
            
            // Simulate work
            for (int i = 1; i <= 10; i++) {
                System.out.println(taskName + " - Processing item " + i);
                Thread.sleep(300);
            }
            
        } catch (InterruptedException e) {
            System.out.println(taskName + " - Interrupted! Performing cleanup...");
            Thread.currentThread().interrupt();
        } finally {
            // Cleanup resources
            System.out.println(taskName + " - Cleaning up resources...");
            try {
                Thread.sleep(200); // Simulate cleanup time
            } catch (InterruptedException e) {
                System.out.println(taskName + " - Cleanup interrupted");
            }
            System.out.println(taskName + " - Cleanup completed");
        }
        
        System.out.println(taskName + " - Shutdown complete");
    }
}

/**
 * Task that supports cooperative cancellation
 */
class CooperativeTask implements Runnable {
    private String taskName;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    
    public CooperativeTask(String taskName) {
        this.taskName = taskName;
    }
    
    public void cancel() {
        cancelled.set(true);
    }
    
    @Override
    public void run() {
        System.out.println(taskName + " started with cooperative cancellation");
        
        int counter = 0;
        while (!cancelled.get() && !Thread.currentThread().isInterrupted()) {
            counter++;
            System.out.println(taskName + " - Working... " + counter);
            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                System.out.println(taskName + " - Sleep interrupted");
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        if (cancelled.get()) {
            System.out.println(taskName + " - Cancelled cooperatively");
        } else {
            System.out.println(taskName + " - Interrupted");
        }
        
        System.out.println(taskName + " - Completed");
    }
}

/**
 * Simple shared buffer for producer-consumer demonstration
 */
class SharedBuffer {
    private String data;
    private boolean hasData = false;
    
    public synchronized void put(String data) throws InterruptedException {
        while (hasData) {
            wait();
        }
        this.data = data;
        this.hasData = true;
        System.out.println("Produced: " + data);
        notifyAll();
    }
    
    public synchronized String get() throws InterruptedException {
        while (!hasData) {
            wait();
        }
        String result = this.data;
        this.hasData = false;
        System.out.println("Consumed: " + result);
        notifyAll();
        return result;
    }
}

/**
 * Producer for producer-consumer demonstration
 */
class Producer implements Runnable {
    private SharedBuffer buffer;
    private AtomicInteger counter = new AtomicInteger(1);
    
    public Producer(SharedBuffer buffer) {
        this.buffer = buffer;
    }
    
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                String data = "Item-" + counter.getAndIncrement();
                buffer.put(data);
                Thread.sleep(800);
            }
        } catch (InterruptedException e) {
            System.out.println("Producer interrupted");
        }
        System.out.println("Producer stopped");
    }
}

/**
 * Consumer for producer-consumer demonstration
 */
class Consumer implements Runnable {
    private SharedBuffer buffer;
    
    public Consumer(SharedBuffer buffer) {
        this.buffer = buffer;
    }
    
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                String data = buffer.get();
                // Simulate processing time
                Thread.sleep(1200);
            }
        } catch (InterruptedException e) {
            System.out.println("Consumer interrupted");
        }
        System.out.println("Consumer stopped");
    }
}
```

---

## 🏆 Day 29 Summary

### **Key Concepts Mastered:**

✅ **Thread Creation Methods:**
- Extending Thread class
- Implementing Runnable interface
- Lambda expressions and anonymous classes

✅ **Thread Properties:**
- Thread names, IDs, and priorities
- Daemon threads vs user threads
- Thread groups and monitoring

✅ **Thread Lifecycle:**
- Six thread states (NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED)
- State transitions and monitoring
- Thread state debugging techniques

✅ **Thread Coordination:**
- join() method for thread synchronization
- Thread interruption and graceful shutdown
- Cooperative cancellation patterns

✅ **Best Practices:**
- Proper exception handling in threads
- Resource cleanup in finally blocks
- Interrupt status preservation
- Producer-consumer coordination

### **Practical Applications:**
- Multi-threaded file processing
- Background task execution
- Producer-consumer systems
- Graceful application shutdown
- Real-time data processing

### **Next Steps:**
Tomorrow we'll explore **Synchronization & Locks** - learning how to coordinate thread access to shared resources and prevent race conditions.

---

## 🚀 Practice Exercises

1. **Thread Pool Simulation**: Create a simple thread pool that manages a fixed number of worker threads
2. **Download Manager**: Build a multi-threaded download manager that can handle multiple concurrent downloads
3. **Background Service**: Implement a background service that processes tasks from a queue
4. **Thread Monitor**: Create a utility that monitors and reports on thread states and performance

The foundation of Java multithreading is now solid - you're ready to tackle synchronization challenges! 🧵✨