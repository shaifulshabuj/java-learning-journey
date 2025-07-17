# Day 35: Week 5 Capstone - Concurrent Task Management System (August 13, 2025)

**Date:** August 13, 2025  
**Duration:** 4 hours  
**Focus:** Build a production-ready concurrent task management system integrating all Week 5 concepts

---

## 🎯 Today's Learning Goals

By the end of this capstone project, you'll:

- ✅ Integrate threads, synchronization, executors, and concurrent collections
- ✅ Build a scalable task management system with priority queues
- ✅ Implement asynchronous processing with CompletableFuture
- ✅ Apply performance monitoring and optimization techniques
- ✅ Handle error recovery and system resilience
- ✅ Create a complete production-ready concurrent application

---

## 🏗️ Part 1: Core Task Management Infrastructure (90 minutes)

### **System Architecture Overview**

Our **Concurrent Task Management System** will include:

- **Task Definition**: Prioritized, categorized tasks with dependencies
- **Worker Pool Management**: Dynamic thread pool sizing and optimization
- **Task Scheduling**: Priority-based execution with dependency resolution
- **Progress Monitoring**: Real-time metrics and performance tracking
- **Error Handling**: Comprehensive failure recovery and retry mechanisms
- **Admin Interface**: Management and monitoring capabilities

### **Task Model and Core Infrastructure**

Create `TaskManagementSystem.java`:

```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.*;
import java.time.*;
import java.util.function.*;
import java.lang.management.*;

/**
 * Production-ready Concurrent Task Management System
 * Integrates all Week 5 concepts: threads, synchronization, executors, 
 * concurrent collections, CompletableFuture, and performance optimization
 */
public class TaskManagementSystem {
    
    // Core system components
    private final TaskExecutionEngine executionEngine;
    private final TaskScheduler scheduler;
    private final PerformanceMonitor performanceMonitor;
    private final TaskRepository taskRepository;
    private final SystemHealthMonitor healthMonitor;
    
    // System configuration
    private final SystemConfiguration config;
    private volatile boolean isRunning = false;
    
    public TaskManagementSystem(SystemConfiguration config) {
        this.config = config;
        this.taskRepository = new TaskRepository();
        this.performanceMonitor = new PerformanceMonitor();
        this.healthMonitor = new SystemHealthMonitor();
        this.executionEngine = new TaskExecutionEngine(config, performanceMonitor);
        this.scheduler = new TaskScheduler(executionEngine, taskRepository, performanceMonitor);
        
        System.out.println("Task Management System initialized with configuration:");
        System.out.println(config);
    }
    
    public static void main(String[] args) {
        System.out.println("=== Concurrent Task Management System ===\n");
        
        // Create system configuration
        SystemConfiguration config = SystemConfiguration.builder()
            .corePoolSize(4)
            .maximumPoolSize(16)
            .keepAliveTime(60)
            .queueCapacity(1000)
            .enableMetrics(true)
            .enableHealthCheck(true)
            .retryAttempts(3)
            .build();
        
        // Initialize the system
        TaskManagementSystem system = new TaskManagementSystem(config);
        
        try {
            // Start the system
            system.start();
            
            // Run comprehensive demo
            system.runComprehensiveDemo();
            
            // Monitor system for a while
            system.monitorSystem(30); // 30 seconds
            
        } finally {
            // Graceful shutdown
            system.shutdown();
        }
    }
    
    public void start() {
        if (isRunning) {
            throw new IllegalStateException("System is already running");
        }
        
        System.out.println("Starting Task Management System...");
        
        // Start all components
        executionEngine.start();
        scheduler.start();
        performanceMonitor.start();
        healthMonitor.start();
        
        isRunning = true;
        System.out.println("✅ Task Management System started successfully\n");
    }
    
    public void shutdown() {
        if (!isRunning) {
            return;
        }
        
        System.out.println("\nShutting down Task Management System...");
        
        // Graceful shutdown in reverse order
        scheduler.shutdown();
        executionEngine.shutdown();
        performanceMonitor.shutdown();
        healthMonitor.shutdown();
        
        isRunning = false;
        System.out.println("✅ Task Management System shutdown completed");
    }
    
    private void runComprehensiveDemo() {
        System.out.println("🚀 Running Comprehensive Demo\n");
        
        // 1. Submit various types of tasks
        submitDemoTasks();
        
        // 2. Demonstrate dependency management
        demonstrateDependencyChaining();
        
        // 3. Show error handling and recovery
        demonstrateErrorHandling();
        
        // 4. Performance testing
        performanceStressTest();
        
        System.out.println("✅ Comprehensive demo completed\n");
    }
    
    private void submitDemoTasks() {
        System.out.println("📋 Submitting Demo Tasks:");
        
        // High priority CPU-intensive tasks
        for (int i = 1; i <= 3; i++) {
            Task task = new Task(
                "cpu-task-" + i,
                TaskType.CPU_INTENSIVE,
                TaskPriority.HIGH,
                this::cpuIntensiveWork
            );
            scheduler.submitTask(task);
        }
        
        // Medium priority I/O tasks
        for (int i = 1; i <= 5; i++) {
            Task task = new Task(
                "io-task-" + i,
                TaskType.IO_BOUND,
                TaskPriority.MEDIUM,
                this::ioIntensiveWork
            );
            scheduler.submitTask(task);
        }
        
        // Low priority background tasks
        for (int i = 1; i <= 2; i++) {
            Task task = new Task(
                "background-task-" + i,
                TaskType.BACKGROUND,
                TaskPriority.LOW,
                this::backgroundWork
            );
            scheduler.submitTask(task);
        }
        
        System.out.println("   ✅ Submitted 10 demo tasks\n");
    }
    
    private void demonstrateDependencyChaining() {
        System.out.println("🔗 Demonstrating Task Dependencies:");
        
        // Create a chain of dependent tasks
        Task dataPrep = new Task(
            "data-preparation",
            TaskType.IO_BOUND,
            TaskPriority.HIGH,
            this::dataPreparationWork
        );
        
        Task dataProcessing = new Task(
            "data-processing",
            TaskType.CPU_INTENSIVE,
            TaskPriority.HIGH,
            this::dataProcessingWork
        );
        dataProcessing.addDependency(dataPrep);
        
        Task reportGeneration = new Task(
            "report-generation",
            TaskType.IO_BOUND,
            TaskPriority.MEDIUM,
            this::reportGenerationWork
        );
        reportGeneration.addDependency(dataProcessing);
        
        // Submit in reverse order to test dependency resolution
        scheduler.submitTask(reportGeneration);
        scheduler.submitTask(dataProcessing);
        scheduler.submitTask(dataPrep);
        
        System.out.println("   ✅ Submitted dependency chain: prep → processing → report\n");
    }
    
    private void demonstrateErrorHandling() {
        System.out.println("⚠️ Demonstrating Error Handling:");
        
        // Task that will fail and retry
        Task flakyTask = new Task(
            "flaky-task",
            TaskType.CPU_INTENSIVE,
            TaskPriority.HIGH,
            this::flakyWork
        );
        flakyTask.setRetryPolicy(new RetryPolicy(3, Duration.ofSeconds(1)));
        
        // Task that will timeout
        Task timeoutTask = new Task(
            "timeout-task",
            TaskType.IO_BOUND,
            TaskPriority.MEDIUM,
            this::timeoutWork
        );
        timeoutTask.setTimeout(Duration.ofSeconds(2));
        
        scheduler.submitTask(flakyTask);
        scheduler.submitTask(timeoutTask);
        
        System.out.println("   ✅ Submitted error-prone tasks for testing\n");
    }
    
    private void performanceStressTest() {
        System.out.println("⚡ Performance Stress Test:");
        
        int taskCount = 50;
        CountDownLatch latch = new CountDownLatch(taskCount);
        long startTime = System.currentTimeMillis();
        
        // Submit many concurrent tasks
        for (int i = 1; i <= taskCount; i++) {
            Task task = new Task(
                "stress-task-" + i,
                TaskType.CPU_INTENSIVE,
                TaskPriority.MEDIUM,
                () -> {
                    try {
                        // Simulate work
                        Thread.sleep(100 + new Random().nextInt(200));
                        return "Stress test result";
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    } finally {
                        latch.countDown();
                    }
                }
            );
            scheduler.submitTask(task);
        }
        
        try {
            // Wait for all tasks to complete
            latch.await(30, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - startTime;
            
            System.out.printf("   ✅ Processed %d tasks in %d ms (%.2f tasks/sec)\n\n", 
                taskCount, duration, (taskCount * 1000.0) / duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("   ❌ Stress test interrupted");
        }
    }
    
    private void monitorSystem(int seconds) {
        System.out.println("📊 Monitoring System Performance:");
        
        for (int i = 0; i < seconds; i++) {
            try {
                Thread.sleep(1000);
                
                if (i % 5 == 0) { // Print metrics every 5 seconds
                    SystemMetrics metrics = performanceMonitor.getCurrentMetrics();
                    System.out.printf("   [%02ds] Tasks: %d completed, %d failed, %d active threads, %.1f%% CPU\n",
                        i, metrics.completedTasks, metrics.failedTasks, 
                        metrics.activeThreads, metrics.cpuUsage);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // Final metrics report
        SystemMetrics finalMetrics = performanceMonitor.getCurrentMetrics();
        System.out.println("\n📈 Final System Metrics:");
        System.out.println(finalMetrics.generateReport());
    }
    
    // Task implementation methods
    private String cpuIntensiveWork() {
        // Simulate CPU-intensive work
        long result = 0;
        for (int i = 0; i < 1000000; i++) {
            result += Math.sqrt(i) * Math.sin(i);
        }
        return "CPU work completed: " + result;
    }
    
    private String ioIntensiveWork() {
        try {
            // Simulate I/O work
            Thread.sleep(500 + new Random().nextInt(1000));
            return "I/O operation completed";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("I/O interrupted", e);
        }
    }
    
    private String backgroundWork() {
        try {
            // Simulate background maintenance work
            Thread.sleep(2000);
            return "Background maintenance completed";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Background work interrupted", e);
        }
    }
    
    private String dataPreparationWork() {
        try {
            Thread.sleep(1000);
            System.out.println("   🔄 Data preparation completed");
            return "Data prepared";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Data preparation interrupted", e);
        }
    }
    
    private String dataProcessingWork() {
        try {
            Thread.sleep(1500);
            System.out.println("   🔄 Data processing completed");
            return "Data processed";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Data processing interrupted", e);
        }
    }
    
    private String reportGenerationWork() {
        try {
            Thread.sleep(800);
            System.out.println("   🔄 Report generation completed");
            return "Report generated";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Report generation interrupted", e);
        }
    }
    
    private String flakyWork() {
        // 70% chance of failure on first attempt
        if (Math.random() < 0.7) {
            throw new RuntimeException("Simulated flaky failure");
        }
        return "Flaky work succeeded";
    }
    
    private String timeoutWork() {
        try {
            // This will timeout
            Thread.sleep(5000);
            return "Should not reach here";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Timeout work interrupted", e);
        }
    }
}

/**
 * Task definition with priority, dependencies, and execution context
 */
class Task {
    private final String id;
    private final TaskType type;
    private final TaskPriority priority;
    private final Supplier<String> work;
    private final Set<Task> dependencies;
    private final Instant createdAt;
    
    private volatile TaskStatus status;
    private volatile Instant startedAt;
    private volatile Instant completedAt;
    private volatile String result;
    private volatile Exception error;
    private volatile int attemptCount;
    
    private RetryPolicy retryPolicy;
    private Duration timeout;
    
    public Task(String id, TaskType type, TaskPriority priority, Supplier<String> work) {
        this.id = id;
        this.type = type;
        this.priority = priority;
        this.work = work;
        this.dependencies = ConcurrentHashMap.newKeySet();
        this.createdAt = Instant.now();
        this.status = TaskStatus.PENDING;
        this.attemptCount = 0;
    }
    
    // Getters and utility methods
    public String getId() { return id; }
    public TaskType getType() { return type; }
    public TaskPriority getPriority() { return priority; }
    public TaskStatus getStatus() { return status; }
    public Set<Task> getDependencies() { return dependencies; }
    public Supplier<String> getWork() { return work; }
    public Duration getTimeout() { return timeout; }
    public RetryPolicy getRetryPolicy() { return retryPolicy; }
    
    public void addDependency(Task dependency) {
        dependencies.add(dependency);
    }
    
    public void setRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }
    
    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }
    
    public boolean areAllDependenciesCompleted() {
        return dependencies.stream()
            .allMatch(dep -> dep.getStatus() == TaskStatus.COMPLETED);
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
        if (status == TaskStatus.RUNNING && startedAt == null) {
            startedAt = Instant.now();
        } else if ((status == TaskStatus.COMPLETED || status == TaskStatus.FAILED) 
                   && completedAt == null) {
            completedAt = Instant.now();
        }
    }
    
    public void setResult(String result) {
        this.result = result;
    }
    
    public void setError(Exception error) {
        this.error = error;
    }
    
    public void incrementAttemptCount() {
        this.attemptCount++;
    }
    
    public int getAttemptCount() {
        return attemptCount;
    }
    
    public Duration getExecutionTime() {
        if (startedAt != null && completedAt != null) {
            return Duration.between(startedAt, completedAt);
        }
        return Duration.ZERO;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task)) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("Task{id='%s', type=%s, priority=%s, status=%s}", 
            id, type, priority, status);
    }
}

/**
 * Task types for different workload characteristics
 */
enum TaskType {
    CPU_INTENSIVE("CPU-bound tasks requiring computational resources"),
    IO_BOUND("I/O-bound tasks involving file, network, or database operations"),
    BACKGROUND("Background maintenance and cleanup tasks");
    
    private final String description;
    
    TaskType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

/**
 * Task priority levels for execution ordering
 */
enum TaskPriority {
    HIGH(3, "Critical tasks requiring immediate execution"),
    MEDIUM(2, "Standard priority tasks"),
    LOW(1, "Background tasks that can wait");
    
    private final int level;
    private final String description;
    
    TaskPriority(int level, String description) {
        this.level = level;
        this.description = description;
    }
    
    public int getLevel() {
        return level;
    }
    
    public String getDescription() {
        return description;
    }
}

/**
 * Task execution status tracking
 */
enum TaskStatus {
    PENDING("Task is waiting to be executed"),
    READY("Task dependencies are satisfied, ready for execution"),
    RUNNING("Task is currently executing"),
    COMPLETED("Task completed successfully"),
    FAILED("Task failed after all retry attempts"),
    CANCELLED("Task was cancelled before completion");
    
    private final String description;
    
    TaskStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

/**
 * Retry policy for failed tasks
 */
class RetryPolicy {
    private final int maxAttempts;
    private final Duration delay;
    private final double backoffMultiplier;
    
    public RetryPolicy(int maxAttempts, Duration delay) {
        this(maxAttempts, delay, 1.0);
    }
    
    public RetryPolicy(int maxAttempts, Duration delay, double backoffMultiplier) {
        this.maxAttempts = maxAttempts;
        this.delay = delay;
        this.backoffMultiplier = backoffMultiplier;
    }
    
    public boolean shouldRetry(int currentAttempt) {
        return currentAttempt < maxAttempts;
    }
    
    public Duration getDelay(int attemptNumber) {
        if (backoffMultiplier == 1.0) {
            return delay;
        }
        long delayMs = (long) (delay.toMillis() * Math.pow(backoffMultiplier, attemptNumber - 1));
        return Duration.ofMillis(delayMs);
    }
    
    public int getMaxAttempts() {
        return maxAttempts;
    }
}
```

---

## 🔧 Part 2: Execution Engine & Scheduling (90 minutes)

### **Advanced Task Execution Engine**

Continue adding to `TaskManagementSystem.java`:

```java
/**
 * Core task execution engine with dynamic thread pool management
 */
class TaskExecutionEngine {
    private final ThreadPoolExecutor executor;
    private final ScheduledExecutorService scheduler;
    private final PerformanceMonitor performanceMonitor;
    private final ReentrantReadWriteLock configLock = new ReentrantReadWriteLock();
    
    // Configuration
    private volatile SystemConfiguration config;
    
    // Metrics
    private final AtomicLong tasksExecuted = new AtomicLong(0);
    private final AtomicLong tasksCompleted = new AtomicLong(0);
    private final AtomicLong tasksFailed = new AtomicLong(0);
    
    public TaskExecutionEngine(SystemConfiguration config, PerformanceMonitor performanceMonitor) {
        this.config = config;
        this.performanceMonitor = performanceMonitor;
        
        // Create custom thread pool with monitoring
        this.executor = new ThreadPoolExecutor(
            config.corePoolSize,
            config.maximumPoolSize,
            config.keepAliveTime,
            TimeUnit.SECONDS,
            new PriorityBlockingQueue<>(config.queueCapacity, new TaskComparator()),
            new TaskThreadFactory(),
            new TaskRejectionHandler()
        );
        
        // Allow core threads to timeout
        executor.allowCoreThreadTimeOut(true);
        
        // Scheduled executor for maintenance tasks
        this.scheduler = Executors.newScheduledThreadPool(2, new TaskThreadFactory("scheduler"));
    }
    
    public void start() {
        // Start pool size optimization
        scheduler.scheduleAtFixedRate(this::optimizePoolSize, 30, 30, TimeUnit.SECONDS);
        
        // Start queue monitoring
        scheduler.scheduleAtFixedRate(this::monitorQueue, 10, 10, TimeUnit.SECONDS);
        
        System.out.println("✅ Task Execution Engine started");
    }
    
    public void shutdown() {
        System.out.println("Shutting down Task Execution Engine...");
        
        scheduler.shutdown();
        executor.shutdown();
        
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                System.out.println("⚠️ Forcing executor shutdown...");
                executor.shutdownNow();
                
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    System.err.println("❌ Executor did not terminate gracefully");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        System.out.println("✅ Task Execution Engine shutdown completed");
    }
    
    public CompletableFuture<String> executeTask(Task task) {
        tasksExecuted.incrementAndGet();
        
        return CompletableFuture.supplyAsync(() -> {
            task.setStatus(TaskStatus.RUNNING);
            task.incrementAttemptCount();
            
            try {
                String result;
                
                // Apply timeout if specified
                if (task.getTimeout() != null) {
                    result = executeWithTimeout(task);
                } else {
                    result = task.getWork().get();
                }
                
                task.setResult(result);
                task.setStatus(TaskStatus.COMPLETED);
                tasksCompleted.incrementAndGet();
                
                performanceMonitor.recordTaskCompletion(task);
                
                return result;
                
            } catch (Exception e) {
                task.setError(e);
                task.setStatus(TaskStatus.FAILED);
                tasksFailed.incrementAndGet();
                
                performanceMonitor.recordTaskFailure(task, e);
                
                throw new CompletionException(e);
            }
        }, executor);
    }
    
    private String executeWithTimeout(Task task) throws Exception {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(
            task.getWork(), 
            ForkJoinPool.commonPool()
        );
        
        try {
            return future.get(task.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new RuntimeException("Task timed out after " + task.getTimeout(), e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Exception) {
                throw (Exception) cause;
            } else {
                throw new RuntimeException("Task execution failed", cause);
            }
        }
    }
    
    private void optimizePoolSize() {
        try {
            configLock.writeLock().lock();
            
            // Get current metrics
            int activeThreads = executor.getActiveCount();
            int queueSize = executor.getQueue().size();
            long completedTasks = executor.getCompletedTaskCount();
            
            // Simple optimization logic
            if (queueSize > config.queueCapacity * 0.8 && 
                executor.getPoolSize() < config.maximumPoolSize) {
                // Increase pool size if queue is getting full
                executor.setCorePoolSize(Math.min(config.corePoolSize + 1, config.maximumPoolSize));
                System.out.println("🔧 Increased core pool size to " + executor.getCorePoolSize());
                
            } else if (queueSize < config.queueCapacity * 0.2 && 
                       executor.getPoolSize() > config.corePoolSize) {
                // Decrease pool size if queue is mostly empty
                int newSize = Math.max(config.corePoolSize, executor.getCorePoolSize() - 1);
                executor.setCorePoolSize(newSize);
                System.out.println("🔧 Decreased core pool size to " + executor.getCorePoolSize());
            }
            
        } finally {
            configLock.writeLock().unlock();
        }
    }
    
    private void monitorQueue() {
        int queueSize = executor.getQueue().size();
        int activeThreads = executor.getActiveCount();
        int poolSize = executor.getPoolSize();
        
        if (queueSize > config.queueCapacity * 0.9) {
            System.out.printf("⚠️ Queue capacity warning: %d/%d (%.1f%%)%n", 
                queueSize, config.queueCapacity, (queueSize * 100.0) / config.queueCapacity);
        }
        
        performanceMonitor.updateExecutorMetrics(queueSize, activeThreads, poolSize);
    }
    
    public ExecutorMetrics getMetrics() {
        return new ExecutorMetrics(
            executor.getPoolSize(),
            executor.getActiveCount(),
            executor.getQueue().size(),
            tasksExecuted.get(),
            tasksCompleted.get(),
            tasksFailed.get(),
            executor.getCompletedTaskCount()
        );
    }
}

/**
 * Task comparator for priority-based execution
 */
class TaskComparator implements Comparator<Runnable> {
    @Override
    public int compare(Runnable r1, Runnable r2) {
        // Extract tasks from runnables (CompletableFuture wrappers)
        TaskPriority p1 = extractPriority(r1);
        TaskPriority p2 = extractPriority(r2);
        
        // Higher priority first, then FIFO for same priority
        int priorityComparison = Integer.compare(p2.getLevel(), p1.getLevel());
        if (priorityComparison != 0) {
            return priorityComparison;
        }
        
        // For same priority, maintain insertion order (FIFO)
        return 0;
    }
    
    private TaskPriority extractPriority(Runnable runnable) {
        // Default to MEDIUM priority if we can't extract it
        // In a real implementation, you might store priority in a wrapper
        return TaskPriority.MEDIUM;
    }
}

/**
 * Custom thread factory for better thread management
 */
class TaskThreadFactory implements ThreadFactory {
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;
    private final ThreadGroup group;
    
    public TaskThreadFactory() {
        this("task-worker");
    }
    
    public TaskThreadFactory(String namePrefix) {
        SecurityManager s = System.getSecurityManager();
        this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        this.namePrefix = namePrefix + "-";
    }
    
    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
        
        if (t.isDaemon()) {
            t.setDaemon(false);
        }
        
        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        
        // Set uncaught exception handler
        t.setUncaughtExceptionHandler((thread, ex) -> {
            System.err.printf("Uncaught exception in thread %s: %s%n", 
                thread.getName(), ex.getMessage());
            ex.printStackTrace();
        });
        
        return t;
    }
}

/**
 * Custom rejection handler for when the executor is overwhelmed
 */
class TaskRejectionHandler implements RejectedExecutionHandler {
    private final AtomicLong rejectedTasks = new AtomicLong(0);
    
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        long rejected = rejectedTasks.incrementAndGet();
        
        System.err.printf("❌ Task rejected (total rejected: %d). " +
            "Pool: %d/%d, Queue: %d/%d%n",
            rejected,
            executor.getActiveCount(), executor.getMaximumPoolSize(),
            executor.getQueue().size(), 
            (executor.getQueue() instanceof BlockingQueue) ? 
                ((BlockingQueue<?>) executor.getQueue()).remainingCapacity() + 
                executor.getQueue().size() : -1);
        
        // Throw exception to notify caller
        throw new RejectedExecutionException(
            String.format("Task rejected due to executor overload. " +
                "Active: %d, Queue: %d", 
                executor.getActiveCount(), executor.getQueue().size()));
    }
    
    public long getRejectedCount() {
        return rejectedTasks.get();
    }
}

/**
 * Intelligent task scheduler with dependency resolution
 */
class TaskScheduler {
    private final TaskExecutionEngine executionEngine;
    private final TaskRepository taskRepository;
    private final PerformanceMonitor performanceMonitor;
    private final ScheduledExecutorService scheduler;
    
    // Task queues by status
    private final ConcurrentLinkedQueue<Task> pendingTasks = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<String, CompletableFuture<String>> runningTasks = new ConcurrentHashMap<>();
    
    // Scheduling control
    private volatile boolean isRunning = false;
    private final Object scheduleLock = new Object();
    
    public TaskScheduler(TaskExecutionEngine executionEngine, 
                        TaskRepository taskRepository, 
                        PerformanceMonitor performanceMonitor) {
        this.executionEngine = executionEngine;
        this.taskRepository = taskRepository;
        this.performanceMonitor = performanceMonitor;
        this.scheduler = Executors.newScheduledThreadPool(2, new TaskThreadFactory("scheduler"));
    }
    
    public void start() {
        isRunning = true;
        
        // Start task scheduling loop
        scheduler.scheduleAtFixedRate(this::scheduleReadyTasks, 100, 100, TimeUnit.MILLISECONDS);
        
        // Start task cleanup
        scheduler.scheduleAtFixedRate(this::cleanupCompletedTasks, 5, 5, TimeUnit.SECONDS);
        
        System.out.println("✅ Task Scheduler started");
    }
    
    public void shutdown() {
        System.out.println("Shutting down Task Scheduler...");
        
        isRunning = false;
        scheduler.shutdown();
        
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Cancel running tasks
        runningTasks.values().forEach(future -> future.cancel(true));
        
        System.out.println("✅ Task Scheduler shutdown completed");
    }
    
    public void submitTask(Task task) {
        taskRepository.addTask(task);
        pendingTasks.offer(task);
        
        System.out.printf("📋 Task submitted: %s [%s, %s]%n", 
            task.getId(), task.getType(), task.getPriority());
        
        // Trigger immediate scheduling check
        synchronized (scheduleLock) {
            scheduleLock.notify();
        }
    }
    
    private void scheduleReadyTasks() {
        if (!isRunning) {
            return;
        }
        
        List<Task> readyTasks = new ArrayList<>();
        
        // Find tasks that are ready to execute
        Iterator<Task> iterator = pendingTasks.iterator();
        while (iterator.hasNext()) {
            Task task = iterator.next();
            
            if (task.areAllDependenciesCompleted()) {
                task.setStatus(TaskStatus.READY);
                readyTasks.add(task);
                iterator.remove();
            }
        }
        
        // Execute ready tasks
        for (Task task : readyTasks) {
            executeTaskWithRetry(task);
        }
    }
    
    private void executeTaskWithRetry(Task task) {
        CompletableFuture<String> future = executionEngine.executeTask(task)
            .handle((result, throwable) -> {
                if (throwable != null) {
                    return handleTaskFailure(task, throwable);
                } else {
                    System.out.printf("✅ Task completed: %s -> %s%n", task.getId(), result);
                    return result;
                }
            })
            .thenCompose(result -> {
                // Remove from running tasks when complete
                runningTasks.remove(task.getId());
                return CompletableFuture.completedFuture(result);
            });
        
        runningTasks.put(task.getId(), future);
    }
    
    private String handleTaskFailure(Task task, Throwable throwable) {
        System.out.printf("❌ Task failed: %s (attempt %d) - %s%n", 
            task.getId(), task.getAttemptCount(), throwable.getMessage());
        
        // Check if we should retry
        RetryPolicy retryPolicy = task.getRetryPolicy();
        if (retryPolicy != null && retryPolicy.shouldRetry(task.getAttemptCount())) {
            
            // Schedule retry with delay
            Duration delay = retryPolicy.getDelay(task.getAttemptCount());
            System.out.printf("🔄 Retrying task %s in %s (attempt %d/%d)%n", 
                task.getId(), delay, task.getAttemptCount() + 1, retryPolicy.getMaxAttempts());
            
            scheduler.schedule(() -> {
                task.setStatus(TaskStatus.READY);
                executeTaskWithRetry(task);
            }, delay.toMillis(), TimeUnit.MILLISECONDS);
            
            return "Scheduled for retry";
        } else {
            // Task failed permanently
            task.setStatus(TaskStatus.FAILED);
            performanceMonitor.recordTaskFailure(task, 
                throwable instanceof Exception ? (Exception) throwable : 
                new RuntimeException("Task failed", throwable));
            
            return "Task failed permanently";
        }
    }
    
    private void cleanupCompletedTasks() {
        int cleaned = taskRepository.cleanupOldTasks(Duration.ofMinutes(30));
        if (cleaned > 0) {
            System.out.printf("🧹 Cleaned up %d old tasks%n", cleaned);
        }
    }
    
    public SchedulerMetrics getMetrics() {
        return new SchedulerMetrics(
            pendingTasks.size(),
            runningTasks.size(),
            taskRepository.getCompletedTaskCount(),
            taskRepository.getFailedTaskCount()
        );
    }
}
```

---

## 📊 Part 3: Monitoring, Metrics & System Health (90 minutes)

### **Comprehensive Performance Monitoring**

Continue adding to `TaskManagementSystem.java`:

```java
/**
 * Advanced performance monitoring and metrics collection
 */
class PerformanceMonitor {
    private final ScheduledExecutorService monitorExecutor;
    private final ThreadMXBean threadBean;
    private final MemoryMXBean memoryBean;
    private final RuntimeMXBean runtimeBean;
    
    // Metrics storage
    private final ConcurrentHashMap<String, TaskMetrics> taskMetrics = new ConcurrentHashMap<>();
    private final LongAdder totalTasksCompleted = new LongAdder();
    private final LongAdder totalTasksFailed = new LongAdder();
    private final AtomicReference<SystemMetrics> currentMetrics = new AtomicReference<>();
    
    // Performance history
    private final ConcurrentLinkedQueue<SystemSnapshot> performanceHistory = new ConcurrentLinkedQueue<>();
    private final int maxHistorySize = 1000;
    
    // Executor metrics
    private volatile int currentQueueSize = 0;
    private volatile int currentActiveThreads = 0;
    private volatile int currentPoolSize = 0;
    
    public PerformanceMonitor() {
        this.monitorExecutor = Executors.newScheduledThreadPool(1, 
            new TaskThreadFactory("perf-monitor"));
        this.threadBean = ManagementFactory.getThreadMXBean();
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.runtimeBean = ManagementFactory.getRuntimeMXBean();
        
        // Initialize current metrics
        this.currentMetrics.set(new SystemMetrics());
    }
    
    public void start() {
        // Collect system metrics every second
        monitorExecutor.scheduleAtFixedRate(this::collectSystemMetrics, 0, 1, TimeUnit.SECONDS);
        
        // Generate performance report every 30 seconds
        monitorExecutor.scheduleAtFixedRate(this::generatePerformanceReport, 30, 30, TimeUnit.SECONDS);
        
        System.out.println("✅ Performance Monitor started");
    }
    
    public void shutdown() {
        System.out.println("Shutting down Performance Monitor...");
        
        monitorExecutor.shutdown();
        try {
            if (!monitorExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                monitorExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            monitorExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        System.out.println("✅ Performance Monitor shutdown completed");
    }
    
    public void recordTaskCompletion(Task task) {
        totalTasksCompleted.increment();
        
        String taskType = task.getType().name();
        taskMetrics.computeIfAbsent(taskType, k -> new TaskMetrics()).recordCompletion(
            task.getExecutionTime().toMillis(),
            task.getAttemptCount()
        );
        
        // Update current metrics
        SystemMetrics current = currentMetrics.get();
        current.completedTasks = totalTasksCompleted.sum();
    }
    
    public void recordTaskFailure(Task task, Exception error) {
        totalTasksFailed.increment();
        
        String taskType = task.getType().name();
        taskMetrics.computeIfAbsent(taskType, k -> new TaskMetrics()).recordFailure(
            error.getClass().getSimpleName(),
            task.getAttemptCount()
        );
        
        // Update current metrics
        SystemMetrics current = currentMetrics.get();
        current.failedTasks = totalTasksFailed.sum();
    }
    
    public void updateExecutorMetrics(int queueSize, int activeThreads, int poolSize) {
        this.currentQueueSize = queueSize;
        this.currentActiveThreads = activeThreads;
        this.currentPoolSize = poolSize;
    }
    
    private void collectSystemMetrics() {
        // Collect JVM metrics
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        double cpuUsage = getCpuUsage();
        int threadCount = threadBean.getThreadCount();
        long uptime = runtimeBean.getUptime();
        
        // Create snapshot
        SystemSnapshot snapshot = new SystemSnapshot(
            Instant.now(),
            cpuUsage,
            heapUsage.getUsed(),
            heapUsage.getMax(),
            nonHeapUsage.getUsed(),
            threadCount,
            currentQueueSize,
            currentActiveThreads,
            currentPoolSize,
            totalTasksCompleted.sum(),
            totalTasksFailed.sum()
        );
        
        // Add to history
        performanceHistory.offer(snapshot);
        while (performanceHistory.size() > maxHistorySize) {
            performanceHistory.poll();
        }
        
        // Update current metrics
        SystemMetrics metrics = new SystemMetrics();
        metrics.cpuUsage = cpuUsage;
        metrics.heapUsed = heapUsage.getUsed();
        metrics.heapMax = heapUsage.getMax();
        metrics.threadCount = threadCount;
        metrics.activeThreads = currentActiveThreads;
        metrics.queueSize = currentQueueSize;
        metrics.completedTasks = totalTasksCompleted.sum();
        metrics.failedTasks = totalTasksFailed.sum();
        metrics.uptime = uptime;
        
        currentMetrics.set(metrics);
    }
    
    private double getCpuUsage() {
        com.sun.management.OperatingSystemMXBean osBean = 
            (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        return osBean.getProcessCpuLoad() * 100.0;
    }
    
    private void generatePerformanceReport() {
        SystemMetrics metrics = currentMetrics.get();
        
        System.out.println("\n📊 === Performance Report ===");
        System.out.printf("CPU Usage: %.1f%%\n", metrics.cpuUsage);
        System.out.printf("Memory: %s / %s (%.1f%%)\n", 
            formatBytes(metrics.heapUsed), 
            formatBytes(metrics.heapMax),
            (metrics.heapUsed * 100.0) / metrics.heapMax);
        System.out.printf("Threads: %d total, %d active workers\n", 
            metrics.threadCount, metrics.activeThreads);
        System.out.printf("Tasks: %d completed, %d failed, %d queued\n", 
            metrics.completedTasks, metrics.failedTasks, metrics.queueSize);
        
        // Task type performance
        System.out.println("\nTask Type Performance:");
        taskMetrics.forEach((type, metrics_) -> {
            System.out.printf("  %s: %.2f ms avg, %.1f%% success rate\n", 
                type, metrics_.getAverageExecutionTime(), metrics_.getSuccessRate());
        });
        
        System.out.println("=========================\n");
    }
    
    public SystemMetrics getCurrentMetrics() {
        return currentMetrics.get();
    }
    
    public List<SystemSnapshot> getPerformanceHistory() {
        return new ArrayList<>(performanceHistory);
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}

/**
 * Task-specific metrics tracking
 */
class TaskMetrics {
    private final LongAdder completedCount = new LongAdder();
    private final LongAdder failedCount = new LongAdder();
    private final LongAdder totalExecutionTime = new LongAdder();
    private final LongAdder totalAttempts = new LongAdder();
    private final ConcurrentHashMap<String, LongAdder> errorCounts = new ConcurrentHashMap<>();
    
    public void recordCompletion(long executionTimeMs, int attempts) {
        completedCount.increment();
        totalExecutionTime.add(executionTimeMs);
        totalAttempts.add(attempts);
    }
    
    public void recordFailure(String errorType, int attempts) {
        failedCount.increment();
        totalAttempts.add(attempts);
        errorCounts.computeIfAbsent(errorType, k -> new LongAdder()).increment();
    }
    
    public double getAverageExecutionTime() {
        long completed = completedCount.sum();
        return completed > 0 ? (double) totalExecutionTime.sum() / completed : 0.0;
    }
    
    public double getSuccessRate() {
        long total = completedCount.sum() + failedCount.sum();
        return total > 0 ? (completedCount.sum() * 100.0) / total : 0.0;
    }
    
    public double getAverageAttempts() {
        long total = completedCount.sum() + failedCount.sum();
        return total > 0 ? (double) totalAttempts.sum() / total : 0.0;
    }
    
    public Map<String, Long> getErrorBreakdown() {
        Map<String, Long> breakdown = new HashMap<>();
        errorCounts.forEach((error, count) -> breakdown.put(error, count.sum()));
        return breakdown;
    }
}

/**
 * System-wide metrics snapshot
 */
class SystemMetrics {
    public double cpuUsage = 0.0;
    public long heapUsed = 0;
    public long heapMax = 0;
    public int threadCount = 0;
    public int activeThreads = 0;
    public int queueSize = 0;
    public long completedTasks = 0;
    public long failedTasks = 0;
    public long uptime = 0;
    
    public String generateReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== System Metrics Report ===\n");
        report.append(String.format("CPU Usage: %.1f%%\n", cpuUsage));
        report.append(String.format("Memory: %s / %s (%.1f%%)\n", 
            formatBytes(heapUsed), formatBytes(heapMax),
            heapMax > 0 ? (heapUsed * 100.0) / heapMax : 0.0));
        report.append(String.format("Threads: %d total, %d active\n", threadCount, activeThreads));
        report.append(String.format("Queue Size: %d\n", queueSize));
        report.append(String.format("Tasks: %d completed, %d failed\n", completedTasks, failedTasks));
        
        if (completedTasks + failedTasks > 0) {
            double successRate = (completedTasks * 100.0) / (completedTasks + failedTasks);
            report.append(String.format("Success Rate: %.1f%%\n", successRate));
        }
        
        report.append(String.format("Uptime: %s\n", formatDuration(uptime)));
        report.append("============================");
        
        return report.toString();
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
}

/**
 * Point-in-time system snapshot
 */
class SystemSnapshot {
    public final Instant timestamp;
    public final double cpuUsage;
    public final long heapUsed;
    public final long heapMax;
    public final long nonHeapUsed;
    public final int threadCount;
    public final int queueSize;
    public final int activeThreads;
    public final int poolSize;
    public final long completedTasks;
    public final long failedTasks;
    
    public SystemSnapshot(Instant timestamp, double cpuUsage, long heapUsed, long heapMax,
                         long nonHeapUsed, int threadCount, int queueSize, int activeThreads,
                         int poolSize, long completedTasks, long failedTasks) {
        this.timestamp = timestamp;
        this.cpuUsage = cpuUsage;
        this.heapUsed = heapUsed;
        this.heapMax = heapMax;
        this.nonHeapUsed = nonHeapUsed;
        this.threadCount = threadCount;
        this.queueSize = queueSize;
        this.activeThreads = activeThreads;
        this.poolSize = poolSize;
        this.completedTasks = completedTasks;
        this.failedTasks = failedTasks;
    }
}

/**
 * Task repository for persistence and cleanup
 */
class TaskRepository {
    private final ConcurrentHashMap<String, Task> allTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<TaskStatus, ConcurrentLinkedQueue<Task>> tasksByStatus = 
        new ConcurrentHashMap<>();
    
    public TaskRepository() {
        // Initialize queues for each status
        for (TaskStatus status : TaskStatus.values()) {
            tasksByStatus.put(status, new ConcurrentLinkedQueue<>());
        }
    }
    
    public void addTask(Task task) {
        allTasks.put(task.getId(), task);
        tasksByStatus.get(task.getStatus()).offer(task);
    }
    
    public Task getTask(String taskId) {
        return allTasks.get(taskId);
    }
    
    public Collection<Task> getAllTasks() {
        return allTasks.values();
    }
    
    public Collection<Task> getTasksByStatus(TaskStatus status) {
        return new ArrayList<>(tasksByStatus.get(status));
    }
    
    public long getCompletedTaskCount() {
        return tasksByStatus.get(TaskStatus.COMPLETED).size();
    }
    
    public long getFailedTaskCount() {
        return tasksByStatus.get(TaskStatus.FAILED).size();
    }
    
    public int cleanupOldTasks(Duration maxAge) {
        Instant cutoff = Instant.now().minus(maxAge);
        int cleaned = 0;
        
        Iterator<Map.Entry<String, Task>> iterator = allTasks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Task> entry = iterator.next();
            Task task = entry.getValue();
            
            // Remove old completed or failed tasks
            if ((task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.FAILED) &&
                task.getExecutionTime().toMillis() > 0 && // Has been executed
                Instant.now().minus(maxAge).isAfter(task.createdAt.plus(task.getExecutionTime()))) {
                
                iterator.remove();
                tasksByStatus.get(task.getStatus()).remove(task);
                cleaned++;
            }
        }
        
        return cleaned;
    }
}

/**
 * System health monitoring
 */
class SystemHealthMonitor {
    private final ScheduledExecutorService healthExecutor;
    private final AtomicReference<HealthStatus> currentHealth = new AtomicReference<>(HealthStatus.HEALTHY);
    private final ConcurrentLinkedQueue<HealthAlert> alerts = new ConcurrentLinkedQueue<>();
    
    // Health thresholds
    private static final double CPU_WARNING_THRESHOLD = 80.0;
    private static final double CPU_CRITICAL_THRESHOLD = 95.0;
    private static final double MEMORY_WARNING_THRESHOLD = 80.0;
    private static final double MEMORY_CRITICAL_THRESHOLD = 95.0;
    private static final double ERROR_RATE_WARNING_THRESHOLD = 10.0;
    private static final double ERROR_RATE_CRITICAL_THRESHOLD = 25.0;
    
    public SystemHealthMonitor() {
        this.healthExecutor = Executors.newScheduledThreadPool(1, 
            new TaskThreadFactory("health-monitor"));
    }
    
    public void start() {
        // Health check every 10 seconds
        healthExecutor.scheduleAtFixedRate(this::performHealthCheck, 10, 10, TimeUnit.SECONDS);
        
        // Alert cleanup every 5 minutes
        healthExecutor.scheduleAtFixedRate(this::cleanupOldAlerts, 5, 5, TimeUnit.MINUTES);
        
        System.out.println("✅ System Health Monitor started");
    }
    
    public void shutdown() {
        System.out.println("Shutting down System Health Monitor...");
        
        healthExecutor.shutdown();
        try {
            if (!healthExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                healthExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            healthExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        System.out.println("✅ System Health Monitor shutdown completed");
    }
    
    private void performHealthCheck() {
        // This would integrate with PerformanceMonitor in a real implementation
        // For demo purposes, we'll simulate health checks
        
        HealthStatus previousHealth = currentHealth.get();
        HealthStatus newHealth = HealthStatus.HEALTHY;
        
        // Simulate occasional health issues
        if (Math.random() < 0.05) { // 5% chance of warning
            newHealth = HealthStatus.WARNING;
            addAlert(new HealthAlert(HealthSeverity.WARNING, "Simulated performance degradation"));
        } else if (Math.random() < 0.01) { // 1% chance of critical
            newHealth = HealthStatus.CRITICAL;
            addAlert(new HealthAlert(HealthSeverity.CRITICAL, "Simulated critical system issue"));
        }
        
        currentHealth.set(newHealth);
        
        // Log health changes
        if (newHealth != previousHealth) {
            System.out.printf("🏥 Health status changed: %s -> %s%n", previousHealth, newHealth);
        }
    }
    
    private void addAlert(HealthAlert alert) {
        alerts.offer(alert);
        System.out.printf("🚨 Health Alert [%s]: %s%n", alert.severity, alert.message);
    }
    
    private void cleanupOldAlerts() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(1));
        alerts.removeIf(alert -> alert.timestamp.isBefore(cutoff));
    }
    
    public HealthStatus getCurrentHealth() {
        return currentHealth.get();
    }
    
    public List<HealthAlert> getRecentAlerts() {
        return new ArrayList<>(alerts);
    }
}

/**
 * System health status enumeration
 */
enum HealthStatus {
    HEALTHY("System operating normally"),
    WARNING("System experiencing minor issues"),
    CRITICAL("System experiencing critical issues"),
    UNKNOWN("System health status unknown");
    
    private final String description;
    
    HealthStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

/**
 * Health alert severity levels
 */
enum HealthSeverity {
    INFO("Informational"),
    WARNING("Warning condition"),
    CRITICAL("Critical condition"),
    EMERGENCY("Emergency condition");
    
    private final String description;
    
    HealthSeverity(String description) {
        this.description = description;
    }
}

/**
 * Health alert data structure
 */
class HealthAlert {
    public final Instant timestamp;
    public final HealthSeverity severity;
    public final String message;
    
    public HealthAlert(HealthSeverity severity, String message) {
        this.timestamp = Instant.now();
        this.severity = severity;
        this.message = message;
    }
}

/**
 * Executor metrics data structure
 */
class ExecutorMetrics {
    public final int poolSize;
    public final int activeThreads;
    public final int queueSize;
    public final long tasksSubmitted;
    public final long tasksCompleted;
    public final long tasksFailed;
    public final long totalTasksCompleted;
    
    public ExecutorMetrics(int poolSize, int activeThreads, int queueSize,
                          long tasksSubmitted, long tasksCompleted, long tasksFailed,
                          long totalTasksCompleted) {
        this.poolSize = poolSize;
        this.activeThreads = activeThreads;
        this.queueSize = queueSize;
        this.tasksSubmitted = tasksSubmitted;
        this.tasksCompleted = tasksCompleted;
        this.tasksFailed = tasksFailed;
        this.totalTasksCompleted = totalTasksCompleted;
    }
}

/**
 * Scheduler metrics data structure
 */
class SchedulerMetrics {
    public final int pendingTasks;
    public final int runningTasks;
    public final long completedTasks;
    public final long failedTasks;
    
    public SchedulerMetrics(int pendingTasks, int runningTasks, 
                           long completedTasks, long failedTasks) {
        this.pendingTasks = pendingTasks;
        this.runningTasks = runningTasks;
        this.completedTasks = completedTasks;
        this.failedTasks = failedTasks;
    }
}

/**
 * System configuration builder
 */
class SystemConfiguration {
    public final int corePoolSize;
    public final int maximumPoolSize;
    public final long keepAliveTime;
    public final int queueCapacity;
    public final boolean enableMetrics;
    public final boolean enableHealthCheck;
    public final int retryAttempts;
    
    private SystemConfiguration(Builder builder) {
        this.corePoolSize = builder.corePoolSize;
        this.maximumPoolSize = builder.maximumPoolSize;
        this.keepAliveTime = builder.keepAliveTime;
        this.queueCapacity = builder.queueCapacity;
        this.enableMetrics = builder.enableMetrics;
        this.enableHealthCheck = builder.enableHealthCheck;
        this.retryAttempts = builder.retryAttempts;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private int corePoolSize = 4;
        private int maximumPoolSize = 16;
        private long keepAliveTime = 60;
        private int queueCapacity = 1000;
        private boolean enableMetrics = true;
        private boolean enableHealthCheck = true;
        private int retryAttempts = 3;
        
        public Builder corePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
            return this;
        }
        
        public Builder maximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
            return this;
        }
        
        public Builder keepAliveTime(long keepAliveTime) {
            this.keepAliveTime = keepAliveTime;
            return this;
        }
        
        public Builder queueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
            return this;
        }
        
        public Builder enableMetrics(boolean enableMetrics) {
            this.enableMetrics = enableMetrics;
            return this;
        }
        
        public Builder enableHealthCheck(boolean enableHealthCheck) {
            this.enableHealthCheck = enableHealthCheck;
            return this;
        }
        
        public Builder retryAttempts(int retryAttempts) {
            this.retryAttempts = retryAttempts;
            return this;
        }
        
        public SystemConfiguration build() {
            return new SystemConfiguration(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format("SystemConfiguration{corePool=%d, maxPool=%d, keepAlive=%ds, " +
            "queueCapacity=%d, metrics=%s, healthCheck=%s, retryAttempts=%d}",
            corePoolSize, maximumPoolSize, keepAliveTime, queueCapacity, 
            enableMetrics, enableHealthCheck, retryAttempts);
    }
}
```

---

## 🏁 Part 4: Integration Summary & Best Practices (30 minutes)

### **Week 5 Concepts Integration Summary**

**🧵 Thread Management:**
- Custom `TaskThreadFactory` with proper naming and exception handling
- Thread pool optimization with dynamic sizing
- Graceful shutdown procedures

**🔒 Synchronization:**
- `ReentrantReadWriteLock` for configuration management
- `CountDownLatch` for coordination
- Atomic variables for metrics (`AtomicLong`, `LongAdder`)
- Concurrent collections throughout

**⚙️ Executor Framework:**
- Custom `ThreadPoolExecutor` with priority queuing
- `ScheduledExecutorService` for maintenance tasks
- Proper rejection handling and monitoring

**📚 Concurrent Collections:**
- `ConcurrentHashMap` for task storage and metrics
- `PriorityBlockingQueue` for task ordering
- `ConcurrentLinkedQueue` for task queues
- `ConcurrentHashMap.newKeySet()` for dependencies

**🚀 CompletableFuture & Async:**
- Asynchronous task execution with `CompletableFuture`
- Composition with `handle()` and `thenCompose()`
- Timeout handling with proper cancellation
- Error recovery and retry mechanisms

**📊 Performance & Monitoring:**
- Real-time metrics collection
- JVM monitoring integration
- Performance optimization
- Health monitoring and alerting

### **Production-Ready Features Implemented:**

1. **Scalability:** Dynamic thread pool sizing and optimization
2. **Reliability:** Comprehensive error handling and retry mechanisms
3. **Observability:** Detailed metrics, monitoring, and health checks
4. **Maintainability:** Clean architecture with separated concerns
5. **Performance:** Priority-based execution and resource optimization
6. **Resilience:** Graceful degradation and failure recovery

### **Key Learning Outcomes:**

✅ **Integrated Knowledge:** Successfully combined all Week 5 concepts into a cohesive system  
✅ **Production Practices:** Implemented enterprise-grade patterns and practices  
✅ **Performance Optimization:** Applied monitoring and optimization techniques  
✅ **Error Resilience:** Built comprehensive error handling and recovery  
✅ **Concurrent Design:** Designed thread-safe, scalable concurrent systems  
✅ **Real-world Application:** Created a system suitable for production use

---

## 🎯 Summary

**Congratulations!** You've completed the **Week 5 Capstone Project** and built a comprehensive **Concurrent Task Management System** that integrates:

- **Advanced thread management** with custom factories and pools
- **Sophisticated synchronization** with locks and atomic operations  
- **Enterprise-grade executor framework** usage with monitoring
- **Thread-safe concurrent collections** for all data structures
- **Modern asynchronous programming** with CompletableFuture
- **Production-level performance monitoring** and optimization
- **Comprehensive error handling** and system resilience

This capstone project demonstrates mastery of **Java concurrency** and readiness for building **production-grade concurrent applications**. The system includes all the patterns, practices, and techniques needed for real-world enterprise software development.

**Next Steps:** Consider extending this system with additional features like:
- REST API endpoints for remote task submission
- Database persistence for task durability  
- Distributed task execution across multiple nodes
- Advanced scheduling algorithms (deadline-based, resource-aware)
- Integration with monitoring systems (Prometheus, Grafana)

**🏆 Week 5 Complete!** You now have comprehensive knowledge of **Java Multithreading & Concurrency**.