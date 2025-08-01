# Day 163: Advanced Resilience Engineering - Circuit Breakers, Bulkheads & Chaos Engineering

## Learning Objectives
By the end of this lesson, you will:
- Master advanced resilience patterns for distributed systems
- Implement circuit breakers, bulkheads, and timeout mechanisms
- Apply chaos engineering principles and fault injection techniques
- Build self-healing systems with automated recovery
- Design observability-driven resilience engineering solutions

## 1. Advanced Circuit Breaker Patterns

### 1.1 Intelligent Circuit Breaker Implementation

```java
// Advanced Circuit Breaker with Machine Learning
package com.enterprise.resilience.circuitbreaker;

@Component
public class IntelligentCircuitBreaker {
    private final String name;
    private volatile CircuitBreakerState state;
    private final AtomicLong failureCount;
    private final AtomicLong successCount;
    private final AtomicLong requestCount;
    private final CircuitBreakerConfig config;
    private final CircuitBreakerMetrics metrics;
    private final FailurePredictor failurePredictor;
    private final Timer stateTimer;
    private final ScheduledExecutorService scheduler;
    
    public IntelligentCircuitBreaker(String name, 
                                   CircuitBreakerConfig config,
                                   MeterRegistry meterRegistry) {
        this.name = name;
        this.state = CircuitBreakerState.CLOSED;
        this.failureCount = new AtomicLong(0);
        this.successCount = new AtomicLong(0);
        this.requestCount = new AtomicLong(0);
        this.config = config;
        this.metrics = new CircuitBreakerMetrics(name, meterRegistry);
        this.failurePredictor = new FailurePredictor(config.getPredictorConfig());
        this.stateTimer = Timer.start();
        this.scheduler = Executors.newScheduledThreadPool(1, 
            r -> new Thread(r, "circuit-breaker-" + name));
        
        scheduleStateCheck();
    }
    
    public <T> T execute(Supplier<T> operation) throws CircuitBreakerOpenException {
        return execute(operation, config.getDefaultTimeout());
    }
    
    public <T> T execute(Supplier<T> operation, Duration timeout) 
            throws CircuitBreakerOpenException {
        
        long startTime = System.nanoTime();
        requestCount.incrementAndGet();
        
        try {
            // Check if circuit breaker should open proactively
            if (shouldPreventExecution()) {
                metrics.recordRejectedCall();
                throw new CircuitBreakerOpenException("Circuit breaker is OPEN for: " + name);
            }
            
            // Execute with timeout
            T result = executeWithTimeout(operation, timeout);
            
            // Record success
            recordSuccess(System.nanoTime() - startTime);
            return result;
            
        } catch (Exception e) {
            recordFailure(e, System.nanoTime() - startTime);
            throw e;
        }
    }
    
    private boolean shouldPreventExecution() {
        if (state == CircuitBreakerState.OPEN) {
            return true;
        }
        
        if (state == CircuitBreakerState.HALF_OPEN) {
            return successCount.get() >= config.getPermittedNumberOfCallsInHalfOpenState();
        }
        
        // Predictive failure prevention
        if (config.isEnablePredictiveFailurePrevention()) {
            double failureProbability = failurePredictor.predictFailureProbability();
            return failureProbability > config.getPredictiveFailureThreshold();
        }
        
        return false;
    }
    
    private <T> T executeWithTimeout(Supplier<T> operation, Duration timeout) {
        CompletableFuture<T> future = CompletableFuture.supplyAsync(operation);
        
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new CircuitBreakerTimeoutException("Operation timed out after: " + timeout);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
    
    private void recordSuccess(long durationNanos) {
        successCount.incrementAndGet();
        failureCount.set(0); // Reset failure count on success
        
        metrics.recordSuccessfulCall(Duration.ofNanos(durationNanos));
        failurePredictor.recordSuccess(durationNanos);
        
        // Transition from HALF_OPEN to CLOSED
        if (state == CircuitBreakerState.HALF_OPEN) {
            if (successCount.get() >= config.getMinimumNumberOfCallsInHalfOpenState()) {
                transitionTo(CircuitBreakerState.CLOSED);
            }
        }
    }
    
    private void recordFailure(Exception exception, long durationNanos) {
        long failures = failureCount.incrementAndGet();
        
        metrics.recordFailedCall(Duration.ofNanos(durationNanos), exception.getClass().getSimpleName());
        failurePredictor.recordFailure(exception, durationNanos);
        
        // Check if we should open the circuit
        if (shouldOpenCircuit(failures)) {
            transitionTo(CircuitBreakerState.OPEN);
        }
    }
    
    private boolean shouldOpenCircuit(long failures) {
        long totalCalls = requestCount.get();
        
        if (totalCalls < config.getMinimumNumberOfCalls()) {
            return false;
        }
        
        double failureRate = (double) failures / totalCalls;
        return failureRate >= config.getFailureRateThreshold();
    }
    
    private void transitionTo(CircuitBreakerState newState) {
        CircuitBreakerState oldState = this.state;
        this.state = newState;
        stateTimer.stop(metrics.getStateTimer(newState));
        stateTimer.start();
        
        metrics.recordStateTransition(oldState, newState);
        
        // Reset counters on state transition
        if (newState == CircuitBreakerState.CLOSED || 
            newState == CircuitBreakerState.HALF_OPEN) {
            resetCounters();
        }
        
        // Schedule automatic transition for OPEN state
        if (newState == CircuitBreakerState.OPEN) {
            scheduleHalfOpenTransition();
        }
        
        publishStateChangeEvent(oldState, newState);
    }
    
    private void scheduleHalfOpenTransition() {
        scheduler.schedule(() -> {
            if (state == CircuitBreakerState.OPEN) {
                transitionTo(CircuitBreakerState.HALF_OPEN);
            }
        }, config.getWaitDurationInOpenState().toMillis(), TimeUnit.MILLISECONDS);
    }
    
    private void scheduleStateCheck() {
        scheduler.scheduleAtFixedRate(this::checkState, 
            config.getStateCheckInterval().toMillis(),
            config.getStateCheckInterval().toMillis(),
            TimeUnit.MILLISECONDS);
    }
    
    private void checkState() {
        // Adaptive threshold adjustment based on system health
        if (config.isEnableAdaptiveThresholds()) {
            adjustThresholds();
        }
        
        // Health-based state transitions
        if (state == CircuitBreakerState.CLOSED && isSystemUnhealthy()) {
            transitionTo(CircuitBreakerState.HALF_OPEN);
        }
    }
    
    private void adjustThresholds() {
        SystemHealthMetrics healthMetrics = failurePredictor.getSystemHealthMetrics();
        
        if (healthMetrics.getCpuUsage() > 0.8 || healthMetrics.getMemoryUsage() > 0.8) {
            // Lower failure threshold when system is under stress
            config.adjustFailureRateThreshold(config.getFailureRateThreshold() * 0.8);
        } else if (healthMetrics.getCpuUsage() < 0.5 && healthMetrics.getMemoryUsage() < 0.5) {
            // Increase threshold when system is healthy
            config.adjustFailureRateThreshold(
                Math.min(config.getOriginalFailureRateThreshold(),
                        config.getFailureRateThreshold() * 1.1));
        }
    }
    
    private boolean isSystemUnhealthy() {
        SystemHealthMetrics healthMetrics = failurePredictor.getSystemHealthMetrics();
        return healthMetrics.getCpuUsage() > 0.9 || 
               healthMetrics.getMemoryUsage() > 0.9 ||
               healthMetrics.getErrorRate() > 0.1;
    }
    
    // Getters for monitoring and metrics
    public CircuitBreakerState getState() { return state; }
    public long getFailureCount() { return failureCount.get(); }
    public long getSuccessCount() { return successCount.get(); }
    public CircuitBreakerMetrics getMetrics() { return metrics; }
}

// Configuration class
@ConfigurationProperties(prefix = "resilience.circuit-breaker")
@Data
public class CircuitBreakerConfig {
    private int minimumNumberOfCalls = 10;
    private int minimumNumberOfCallsInHalfOpenState = 5;
    private int permittedNumberOfCallsInHalfOpenState = 10;
    private double failureRateThreshold = 0.5;
    private double originalFailureRateThreshold = 0.5;
    private Duration waitDurationInOpenState = Duration.ofSeconds(30);
    private Duration defaultTimeout = Duration.ofSeconds(5);
    private Duration stateCheckInterval = Duration.ofSeconds(10);
    
    // Advanced features
    private boolean enablePredictiveFailurePrevention = true;
    private double predictiveFailureThreshold = 0.7;
    private boolean enableAdaptiveThresholds = true;
    private PredictorConfig predictorConfig = new PredictorConfig();
    
    public void adjustFailureRateThreshold(double newThreshold) {
        this.failureRateThreshold = Math.max(0.1, Math.min(0.9, newThreshold));
    }
}

// Failure Predictor using simple ML techniques
@Component
public class FailurePredictor {
    private final Queue<FailureDataPoint> recentFailures;
    private final Queue<SuccessDataPoint> recentSuccesses;
    private final PredictorConfig config;
    private final SystemHealthMonitor healthMonitor;
    
    public FailurePredictor(PredictorConfig config) {
        this.config = config;
        this.recentFailures = new ConcurrentLinkedQueue<>();
        this.recentSuccesses = new ConcurrentLinkedQueue<>();
        this.healthMonitor = new SystemHealthMonitor();
    }
    
    public void recordFailure(Exception exception, long durationNanos) {
        FailureDataPoint dataPoint = new FailureDataPoint(
            Instant.now(),
            durationNanos,
            exception.getClass().getSimpleName(),
            healthMonitor.getCurrentMetrics()
        );
        
        recentFailures.offer(dataPoint);
        
        // Keep only recent data points
        while (recentFailures.size() > config.getMaxDataPoints()) {
            recentFailures.poll();
        }
    }
    
    public void recordSuccess(long durationNanos) {
        SuccessDataPoint dataPoint = new SuccessDataPoint(
            Instant.now(),
            durationNanos,
            healthMonitor.getCurrentMetrics()
        );
        
        recentSuccesses.offer(dataPoint);
        
        while (recentSuccesses.size() > config.getMaxDataPoints()) {
            recentSuccesses.poll();
        }
    }
    
    public double predictFailureProbability() {
        if (recentFailures.size() + recentSuccesses.size() < config.getMinDataPointsForPrediction()) {
            return 0.0;
        }
        
        SystemHealthMetrics currentHealth = healthMonitor.getCurrentMetrics();
        
        // Simple linear regression model
        double failureRate = calculateRecentFailureRate();
        double healthScore = calculateHealthScore(currentHealth);
        double trendScore = calculateTrendScore();
        
        // Weighted combination
        return (failureRate * config.getFailureRateWeight() +
                (1 - healthScore) * config.getHealthScoreWeight() +
                trendScore * config.getTrendScoreWeight()) / 
               (config.getFailureRateWeight() + config.getHealthScoreWeight() + config.getTrendScoreWeight());
    }
    
    private double calculateRecentFailureRate() {
        Instant cutoff = Instant.now().minus(config.getPredictionWindow());
        
        long recentFailureCount = recentFailures.stream()
            .filter(dp -> dp.timestamp().isAfter(cutoff))
            .count();
            
        long recentSuccessCount = recentSuccesses.stream()
            .filter(dp -> dp.timestamp().isAfter(cutoff))
            .count();
            
        long totalRecent = recentFailureCount + recentSuccessCount;
        return totalRecent > 0 ? (double) recentFailureCount / totalRecent : 0.0;
    }
    
    private double calculateHealthScore(SystemHealthMetrics health) {
        // Normalize health metrics to 0-1 score
        double cpuScore = 1.0 - health.getCpuUsage();
        double memoryScore = 1.0 - health.getMemoryUsage();
        double errorScore = 1.0 - health.getErrorRate();
        
        return (cpuScore + memoryScore + errorScore) / 3.0;
    }
    
    private double calculateTrendScore() {
        // Calculate if failures are trending up or down
        List<FailureDataPoint> recentFailuresList = new ArrayList<>(recentFailures);
        if (recentFailuresList.size() < 5) {
            return 0.0;
        }
        
        // Simple trend calculation - more failures recently = higher trend score
        Instant halfwayPoint = Instant.now().minus(config.getPredictionWindow().dividedBy(2));
        
        long earlyFailures = recentFailuresList.stream()
            .filter(dp -> dp.timestamp().isBefore(halfwayPoint))
            .count();
            
        long lateFailures = recentFailuresList.stream()
            .filter(dp -> dp.timestamp().isAfter(halfwayPoint))
            .count();
            
        if (earlyFailures == 0) {
            return lateFailures > 0 ? 1.0 : 0.0;
        }
        
        return Math.max(0.0, Math.min(1.0, (double) lateFailures / earlyFailures - 1.0));
    }
    
    public SystemHealthMetrics getSystemHealthMetrics() {
        return healthMonitor.getCurrentMetrics();
    }
}

// Data classes for ML model
record FailureDataPoint(
    Instant timestamp,
    long durationNanos,
    String exceptionType,
    SystemHealthMetrics healthMetrics
) {}

record SuccessDataPoint(
    Instant timestamp,
    long durationNanos,
    SystemHealthMetrics healthMetrics
) {}

record SystemHealthMetrics(
    double cpuUsage,
    double memoryUsage,
    double errorRate,
    int activeThreads,
    long availableMemory
) {}
```

### 1.2 Circuit Breaker Registry and Factory

```java
// Circuit Breaker Registry for managing multiple circuit breakers
@Component
public class CircuitBreakerRegistry {
    private final ConcurrentHashMap<String, IntelligentCircuitBreaker> circuitBreakers;
    private final CircuitBreakerConfig defaultConfig;
    private final MeterRegistry meterRegistry;
    private final ApplicationEventPublisher eventPublisher;
    
    public CircuitBreakerRegistry(CircuitBreakerConfig defaultConfig,
                                 MeterRegistry meterRegistry,
                                 ApplicationEventPublisher eventPublisher) {
        this.circuitBreakers = new ConcurrentHashMap<>();
        this.defaultConfig = defaultConfig;
        this.meterRegistry = meterRegistry;
        this.eventPublisher = eventPublisher;
    }
    
    public IntelligentCircuitBreaker circuitBreaker(String name) {
        return circuitBreakers.computeIfAbsent(name, 
            key -> createCircuitBreaker(key, defaultConfig));
    }
    
    public IntelligentCircuitBreaker circuitBreaker(String name, CircuitBreakerConfig config) {
        return circuitBreakers.computeIfAbsent(name,
            key -> createCircuitBreaker(key, config));
    }
    
    private IntelligentCircuitBreaker createCircuitBreaker(String name, CircuitBreakerConfig config) {
        var circuitBreaker = new IntelligentCircuitBreaker(name, config, meterRegistry);
        
        // Register event listeners
        circuitBreaker.getEventPublisher().onStateTransition(
            event -> eventPublisher.publishEvent(
                new CircuitBreakerStateTransitionEvent(name, event.getFrom(), event.getTo())
            )
        );
        
        return circuitBreaker;
    }
    
    public Collection<IntelligentCircuitBreaker> getAllCircuitBreakers() {
        return Collections.unmodifiableCollection(circuitBreakers.values());
    }
    
    public CircuitBreakerHealthIndicator getHealthIndicator() {
        return new CircuitBreakerHealthIndicator(circuitBreakers);
    }
}

// Spring Boot Health Indicator
@Component
public class CircuitBreakerHealthIndicator implements HealthIndicator {
    private final Map<String, IntelligentCircuitBreaker> circuitBreakers;
    
    public CircuitBreakerHealthIndicator(Map<String, IntelligentCircuitBreaker> circuitBreakers) {
        this.circuitBreakers = circuitBreakers;
    }
    
    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        
        for (Map.Entry<String, IntelligentCircuitBreaker> entry : circuitBreakers.entrySet()) {
            String name = entry.getKey();
            IntelligentCircuitBreaker cb = entry.getValue();
            
            builder.withDetail(name, Map.of(
                "state", cb.getState(),
                "failureCount", cb.getFailureCount(),
                "successCount", cb.getSuccessCount(),
                "metrics", cb.getMetrics().getSummary()
            ));
            
            if (cb.getState() == CircuitBreakerState.OPEN) {
                builder.down();
            }
        }
        
        return builder.build();
    }
}
```

## 2. Bulkhead Pattern Implementation

### 2.1 Thread Pool Isolation

```java
// Thread Pool Bulkhead Implementation
@Component
public class BulkheadThreadPoolManager {
    private final Map<String, ThreadPoolExecutor> threadPools;
    private final BulkheadConfig config;
    private final MeterRegistry meterRegistry;
    private final ScheduledExecutorService monitoringExecutor;
    
    public BulkheadThreadPoolManager(BulkheadConfig config, MeterRegistry meterRegistry) {
        this.threadPools = new ConcurrentHashMap<>();
        this.config = config;
        this.meterRegistry = meterRegistry;
        this.monitoringExecutor = Executors.newScheduledThreadPool(1);
        
        startMonitoring();
    }
    
    public <T> CompletableFuture<T> execute(String bulkheadName, Supplier<T> operation) {
        ThreadPoolExecutor executor = getOrCreateThreadPool(bulkheadName);
        
        return CompletableFuture.supplyAsync(() -> {
            String originalThreadName = Thread.currentThread().getName();
            Thread.currentThread().setName(bulkheadName + "-" + originalThreadName);
            
            Timer.Sample sample = Timer.start(meterRegistry);
            try {
                T result = operation.get();
                recordSuccess(bulkheadName, sample);
                return result;
            } catch (Exception e) {
                recordFailure(bulkheadName, sample, e);
                throw e;
            } finally {
                Thread.currentThread().setName(originalThreadName);
            }
        }, executor);
    }
    
    private ThreadPoolExecutor getOrCreateThreadPool(String bulkheadName) {
        return threadPools.computeIfAbsent(bulkheadName, name -> {
            BulkheadConfig.ThreadPoolConfig poolConfig = config.getThreadPoolConfig(name);
            
            ThreadPoolExecutor executor = new ThreadPoolExecutor(
                poolConfig.getCorePoolSize(),
                poolConfig.getMaximumPoolSize(),
                poolConfig.getKeepAliveTime().toMillis(),
                TimeUnit.MILLISECONDS,
                createQueue(poolConfig),
                new ThreadFactory() {
                    private final AtomicInteger counter = new AtomicInteger(0);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "bulkhead-" + name + "-" + counter.incrementAndGet());
                        t.setDaemon(true);
                        return t;
                    }
                },
                new ThreadPoolExecutor.AbortPolicy()
            );
            
            // Register metrics
            registerThreadPoolMetrics(name, executor);
            
            return executor;
        });
    }
    
    private BlockingQueue<Runnable> createQueue(BulkheadConfig.ThreadPoolConfig config) {
        return switch (config.getQueueType()) {
            case SYNCHRONOUS -> new SynchronousQueue<>();
            case ARRAY_BLOCKING -> new ArrayBlockingQueue<>(config.getQueueCapacity());
            case LINKED_BLOCKING -> new LinkedBlockingQueue<>(config.getQueueCapacity());
            case PRIORITY -> new PriorityBlockingQueue<>();
        };
    }
    
    private void registerThreadPoolMetrics(String name, ThreadPoolExecutor executor) {
        Gauge.builder("bulkhead.thread.pool.active")
            .tag("name", name)
            .register(meterRegistry, executor, ThreadPoolExecutor::getActiveCount);
            
        Gauge.builder("bulkhead.thread.pool.size")
            .tag("name", name)
            .register(meterRegistry, executor, ThreadPoolExecutor::getPoolSize);
            
        Gauge.builder("bulkhead.thread.pool.queue.size")
            .tag("name", name)
            .register(meterRegistry, executor, e -> e.getQueue().size());
            
        Gauge.builder("bulkhead.thread.pool.completed.tasks")
            .tag("name", name)
            .register(meterRegistry, executor, ThreadPoolExecutor::getCompletedTaskCount);
    }
    
    private void recordSuccess(String bulkheadName, Timer.Sample sample) {
        sample.stop(Timer.builder("bulkhead.execution.time")
            .tag("name", bulkheadName)
            .tag("outcome", "success")
            .register(meterRegistry));
            
        Counter.builder("bulkhead.executions")
            .tag("name", bulkheadName)
            .tag("outcome", "success")
            .register(meterRegistry)
            .increment();
    }
    
    private void recordFailure(String bulkheadName, Timer.Sample sample, Exception e) {
        sample.stop(Timer.builder("bulkhead.execution.time")
            .tag("name", bulkheadName)
            .tag("outcome", "failure")
            .tag("exception", e.getClass().getSimpleName())
            .register(meterRegistry));
            
        Counter.builder("bulkhead.executions")
            .tag("name", bulkheadName)
            .tag("outcome", "failure")
            .tag("exception", e.getClass().getSimpleName())
            .register(meterRegistry)
            .increment();
    }
    
    private void startMonitoring() {
        monitoringExecutor.scheduleAtFixedRate(this::monitorThreadPools, 30, 30, TimeUnit.SECONDS);
    }
    
    private void monitorThreadPools() {
        threadPools.forEach((name, executor) -> {
            ThreadPoolConfig poolConfig = config.getThreadPoolConfig(name);
            
            // Check for thread pool saturation
            double utilizationRate = (double) executor.getActiveCount() / executor.getMaximumPoolSize();
            if (utilizationRate > 0.8) {
                publishAlert(new ThreadPoolSaturationAlert(name, utilizationRate));
            }
            
            // Check queue size
            int queueSize = executor.getQueue().size();
            if (queueSize > poolConfig.getQueueCapacity() * 0.8) {
                publishAlert(new QueueSaturationAlert(name, queueSize, poolConfig.getQueueCapacity()));
            }
            
            // Auto-scaling if enabled
            if (poolConfig.isAutoScalingEnabled()) {
                adjustThreadPoolSize(name, executor, utilizationRate);
            }
        });
    }
    
    private void adjustThreadPoolSize(String name, ThreadPoolExecutor executor, double utilizationRate) {
        ThreadPoolConfig config = this.config.getThreadPoolConfig(name);
        
        if (utilizationRate > 0.8 && executor.getMaximumPoolSize() < config.getMaxAutoScaleSize()) {
            int newSize = Math.min(config.getMaxAutoScaleSize(), 
                                  (int) (executor.getMaximumPoolSize() * 1.2));
            executor.setMaximumPoolSize(newSize);
            
            publishEvent(new ThreadPoolScaledEvent(name, executor.getMaximumPoolSize(), newSize, "UP"));
        } else if (utilizationRate < 0.3 && executor.getMaximumPoolSize() > config.getCorePoolSize()) {
            int newSize = Math.max(config.getCorePoolSize(),
                                  (int) (executor.getMaximumPoolSize() * 0.8));
            executor.setMaximumPoolSize(newSize);
            
            publishEvent(new ThreadPoolScaledEvent(name, executor.getMaximumPoolSize(), newSize, "DOWN"));
        }
    }
    
    public BulkheadMetrics getMetrics(String bulkheadName) {
        ThreadPoolExecutor executor = threadPools.get(bulkheadName);
        if (executor == null) {
            return null;
        }
        
        return new BulkheadMetrics(
            bulkheadName,
            executor.getActiveCount(),
            executor.getPoolSize(),
            executor.getMaximumPoolSize(),
            executor.getQueue().size(),
            executor.getCompletedTaskCount(),
            executor.getTaskCount()
        );
    }
}

// Configuration for Bulkhead
@ConfigurationProperties(prefix = "resilience.bulkhead")
@Data
public class BulkheadConfig {
    private Map<String, ThreadPoolConfig> threadPools = new HashMap<>();
    private ThreadPoolConfig defaultThreadPoolConfig = new ThreadPoolConfig();
    
    public ThreadPoolConfig getThreadPoolConfig(String name) {
        return threadPools.getOrDefault(name, defaultThreadPoolConfig);
    }
    
    @Data
    public static class ThreadPoolConfig {
        private int corePoolSize = 5;
        private int maximumPoolSize = 10;
        private Duration keepAliveTime = Duration.ofMinutes(1);
        private QueueType queueType = QueueType.LINKED_BLOCKING;
        private int queueCapacity = 100;
        private boolean autoScalingEnabled = true;
        private int maxAutoScaleSize = 50;
    }
    
    public enum QueueType {
        SYNCHRONOUS, ARRAY_BLOCKING, LINKED_BLOCKING, PRIORITY
    }
}
```

### 2.2 Semaphore-based Bulkhead

```java
// Semaphore Bulkhead for limiting concurrent executions
@Component
public class SemaphoreBulkhead {
    private final ConcurrentHashMap<String, Semaphore> semaphores;
    private final BulkheadConfig config;
    private final MeterRegistry meterRegistry;
    
    public SemaphoreBulkhead(BulkheadConfig config, MeterRegistry meterRegistry) {
        this.semaphores = new ConcurrentHashMap<>();
        this.config = config;
        this.meterRegistry = meterRegistry;
    }
    
    public <T> T execute(String bulkheadName, Supplier<T> operation) 
            throws BulkheadFullException {
        return execute(bulkheadName, operation, Duration.ofSeconds(1));
    }
    
    public <T> T execute(String bulkheadName, Supplier<T> operation, Duration maxWaitTime) 
            throws BulkheadFullException {
        
        Semaphore semaphore = getOrCreateSemaphore(bulkheadName);
        
        Timer.Sample sample = Timer.start(meterRegistry);
        boolean acquired = false;
        
        try {
            acquired = semaphore.tryAcquire(maxWaitTime.toMillis(), TimeUnit.MILLISECONDS);
            
            if (!acquired) {
                recordRejection(bulkheadName);
                throw new BulkheadFullException("Could not acquire permit for bulkhead: " + bulkheadName);
            }
            
            T result = operation.get();
            recordSuccess(bulkheadName, sample);
            return result;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            recordFailure(bulkheadName, sample, e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            recordFailure(bulkheadName, sample, e);
            throw e;
        } finally {
            if (acquired) {
                semaphore.release();
            }
        }
    }
    
    private Semaphore getOrCreateSemaphore(String bulkheadName) {
        return semaphores.computeIfAbsent(bulkheadName, name -> {
            int permits = config.getSemaphoreConfig(name).getMaxConcurrentCalls();
            Semaphore semaphore = new Semaphore(permits, true); // Fair semaphore
            
            // Register metrics
            Gauge.builder("bulkhead.semaphore.available.permits")
                .tag("name", name)
                .register(meterRegistry, semaphore, Semaphore::availablePermits);
                
            Gauge.builder("bulkhead.semaphore.queue.length")
                .tag("name", name)
                .register(meterRegistry, semaphore, Semaphore::getQueueLength);
            
            return semaphore;
        });
    }
    
    private void recordSuccess(String bulkheadName, Timer.Sample sample) {
        sample.stop(Timer.builder("bulkhead.semaphore.execution.time")
            .tag("name", bulkheadName)
            .tag("outcome", "success")
            .register(meterRegistry));
    }
    
    private void recordFailure(String bulkheadName, Timer.Sample sample, Exception e) {
        sample.stop(Timer.builder("bulkhead.semaphore.execution.time")
            .tag("name", bulkheadName)
            .tag("outcome", "failure")
            .tag("exception", e.getClass().getSimpleName())
            .register(meterRegistry));
    }
    
    private void recordRejection(String bulkheadName) {
        Counter.builder("bulkhead.semaphore.rejections")
            .tag("name", bulkheadName)
            .register(meterRegistry)
            .increment();
    }
}
```

## 3. Chaos Engineering Implementation

### 3.1 Chaos Engineering Framework

```java
// Chaos Engineering Framework
@Component
@ConditionalOnProperty(name = "chaos.engineering.enabled", havingValue = "true")
public class ChaosEngineeringEngine {
    private final List<ChaosExperiment> experiments;
    private final ChaosConfig config;
    private final MeterRegistry meterRegistry;
    private final ApplicationEventPublisher eventPublisher;
    private final ScheduledExecutorService scheduler;
    
    public ChaosEngineeringEngine(List<ChaosExperiment> experiments,
                                 ChaosConfig config,
                                 MeterRegistry meterRegistry,
                                 ApplicationEventPublisher eventPublisher) {
        this.experiments = experiments;
        this.config = config;
        this.meterRegistry = meterRegistry;
        this.eventPublisher = eventPublisher;
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        scheduleExperiments();
    }
    
    private void scheduleExperiments() {
        experiments.forEach(experiment -> {
            if (experiment.isEnabled()) {
                scheduleExperiment(experiment);
            }
        });
    }
    
    private void scheduleExperiment(ChaosExperiment experiment) {
        Duration interval = experiment.getConfig().getExecutionInterval();
        
        scheduler.scheduleAtFixedRate(() -> {
            if (shouldRunExperiment(experiment)) {
                executeExperiment(experiment);
            }
        }, interval.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);
    }
    
    private boolean shouldRunExperiment(ChaosExperiment experiment) {
        // Check if current time is within allowed execution window
        if (!isInExecutionWindow(experiment.getConfig().getExecutionWindow())) {
            return false;
        }
        
        // Check probability
        double random = Math.random();
        if (random > experiment.getConfig().getProbability()) {
            return false;
        }
        
        // Check system health - don't run chaos during incidents
        if (config.isSkipDuringIncidents() && isSystemUnhealthy()) {
            return false;
        }
        
        return true;
    }
    
    private void executeExperiment(ChaosExperiment experiment) {
        String experimentId = UUID.randomUUID().toString();
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            eventPublisher.publishEvent(new ChaosExperimentStartedEvent(
                experimentId, experiment.getName()));
            
            ChaosExperimentResult result = experiment.execute();
            
            sample.stop(Timer.builder("chaos.experiment.duration")
                .tag("experiment", experiment.getName())
                .tag("outcome", result.isSuccessful() ? "success" : "failure")
                .register(meterRegistry));
            
            recordExperimentResult(experiment, result);
            
            eventPublisher.publishEvent(new ChaosExperimentCompletedEvent(
                experimentId, experiment.getName(), result));
                
        } catch (Exception e) {
            sample.stop(Timer.builder("chaos.experiment.duration")
                .tag("experiment", experiment.getName())
                .tag("outcome", "error")
                .register(meterRegistry));
                
            eventPublisher.publishEvent(new ChaosExperimentFailedEvent(
                experimentId, experiment.getName(), e));
        }
    }
    
    private boolean isInExecutionWindow(ExecutionWindow window) {
        LocalTime now = LocalTime.now();
        return !now.isBefore(window.getStartTime()) && !now.isAfter(window.getEndTime());
    }
    
    private boolean isSystemUnhealthy() {
        // Check various system health indicators
        // This would integrate with your monitoring system
        return false; // Simplified for example
    }
    
    private void recordExperimentResult(ChaosExperiment experiment, ChaosExperimentResult result) {
        Counter.builder("chaos.experiment.executions")
            .tag("experiment", experiment.getName())
            .tag("outcome", result.isSuccessful() ? "success" : "failure")
            .register(meterRegistry)
            .increment();
            
        if (result.getMetrics() != null) {
            result.getMetrics().forEach((key, value) -> {
                Gauge.builder("chaos.experiment.metric")
                    .tag("experiment", experiment.getName())
                    .tag("metric", key)
                    .register(meterRegistry, value, Double::doubleValue);
            });
        }
    }
}

// Base interface for chaos experiments
public interface ChaosExperiment {
    String getName();
    String getDescription();
    ChaosExperimentConfig getConfig();
    boolean isEnabled();
    ChaosExperimentResult execute();
}

// Latency injection experiment
@Component
@ConditionalOnProperty(name = "chaos.experiments.latency.enabled", havingValue = "true")
public class LatencyInjectionExperiment implements ChaosExperiment {
    private final RestTemplate restTemplate;
    private final ChaosExperimentConfig config;
    
    public LatencyInjectionExperiment(@Qualifier("chaosRestTemplate") RestTemplate restTemplate,
                                     @Value("${chaos.experiments.latency.config}") ChaosExperimentConfig config) {
        this.restTemplate = restTemplate;
        this.config = config;
    }
    
    @Override
    public String getName() {
        return "latency-injection";
    }
    
    @Override
    public String getDescription() {
        return "Injects artificial latency into HTTP calls to test timeout handling";
    }
    
    @Override
    public ChaosExperimentConfig getConfig() {
        return config;
    }
    
    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }
    
    @Override
    public ChaosExperimentResult execute() {
        Duration injectedLatency = Duration.ofMillis(
            config.getLatencyRange().getMin() + 
            (long) (Math.random() * (config.getLatencyRange().getMax() - config.getLatencyRange().getMin()))
        );
        
        // Install latency interceptor
        LatencyInterceptor interceptor = new LatencyInterceptor(injectedLatency);
        restTemplate.getInterceptors().add(interceptor);
        
        try {
            // Let the experiment run for the specified duration
            Thread.sleep(config.getDuration().toMillis());
            
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("injected_latency_ms", injectedLatency.toMillis());
            metrics.put("affected_requests", interceptor.getAffectedRequestCount());
            
            return ChaosExperimentResult.success(metrics);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ChaosExperimentResult.failure("Experiment interrupted", null);
        } finally {
            // Remove interceptor
            restTemplate.getInterceptors().remove(interceptor);
        }
    }
}

// Exception injection experiment
@Component
@ConditionalOnProperty(name = "chaos.experiments.exception.enabled", havingValue = "true")
public class ExceptionInjectionExperiment implements ChaosExperiment {
    private final ApplicationContext applicationContext;
    private final ChaosExperimentConfig config;
    
    @Override
    public ChaosExperimentResult execute() {
        String targetService = config.getTargetService();
        double errorRate = config.getErrorRate();
        
        // Install exception aspect
        ExceptionInjectionAspect aspect = new ExceptionInjectionAspect(errorRate);
        
        try {
            // Register aspect
            AspectJProxyFactory proxyFactory = new AspectJProxyFactory();
            proxyFactory.addAspect(aspect);
            
            // Let experiment run
            Thread.sleep(config.getDuration().toMillis());
            
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("injected_exceptions", aspect.getExceptionCount());
            metrics.put("total_calls", aspect.getTotalCallCount());
            metrics.put("error_rate", aspect.getActualErrorRate());
            
            return ChaosExperimentResult.success(metrics);
            
        } catch (Exception e) {
            return ChaosExperimentResult.failure("Failed to execute exception injection", null);
        }
    }
}

// Resource exhaustion experiment
@Component
@ConditionalOnProperty(name = "chaos.experiments.resource.enabled", havingValue = "true")
public class ResourceExhaustionExperiment implements ChaosExperiment {
    
    @Override
    public ChaosExperimentResult execute() {
        ResourceType resourceType = config.getResourceType();
        
        switch (resourceType) {
            case MEMORY:
                return executeMemoryExhaustion();
            case CPU:
                return executeCpuExhaustion();
            case DISK:
                return executeDiskExhaustion();
            default:
                return ChaosExperimentResult.failure("Unknown resource type", null);
        }
    }
    
    private ChaosExperimentResult executeMemoryExhaustion() {
        List<byte[]> memoryConsumers = new ArrayList<>();
        long targetMemory = config.getMemorySize().toBytes();
        long consumedMemory = 0;
        
        try {
            while (consumedMemory < targetMemory) {
                byte[] chunk = new byte[1024 * 1024]; // 1MB chunks
                memoryConsumers.add(chunk);
                consumedMemory += chunk.length;
            }
            
            // Hold memory for experiment duration
            Thread.sleep(config.getDuration().toMillis());
            
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("consumed_memory_bytes", consumedMemory);
            
            return ChaosExperimentResult.success(metrics);
            
        } catch (OutOfMemoryError e) {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("consumed_memory_bytes", consumedMemory);
            metrics.put("oom_triggered", true);
            
            return ChaosExperimentResult.success(metrics);
        } catch (Exception e) {
            return ChaosExperimentResult.failure("Memory exhaustion failed", null);
        } finally {
            // Clean up memory
            memoryConsumers.clear();
            System.gc();
        }
    }
    
    private ChaosExperimentResult executeCpuExhaustion() {
        int threadCount = config.getCpuThreadCount();
        List<Thread> cpuThreads = new ArrayList<>();
        
        for (int i = 0; i < threadCount; i++) {
            Thread cpuThread = new Thread(() -> {
                long startTime = System.currentTimeMillis();
                long duration = config.getDuration().toMillis();
                
                while (System.currentTimeMillis() - startTime < duration) {
                    // CPU intensive calculation
                    Math.pow(Math.random(), Math.random());
                }
            });
            
            cpuThread.start();
            cpuThreads.add(cpuThread);
        }
        
        try {
            // Wait for all threads to complete
            for (Thread thread : cpuThreads) {
                thread.join();
            }
            
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("cpu_threads", threadCount);
            
            return ChaosExperimentResult.success(metrics);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ChaosExperimentResult.failure("CPU exhaustion interrupted", null);
        }
    }
}
```

### 3.2 Chaos Monkey for Spring Boot

```java
// Chaos Monkey Integration
@Component
@ConditionalOnProperty(prefix = "chaos.monkey", name = "enabled")
public class SpringBootChaosMonkey {
    private final ChaosMonkeyProperties properties;
    private final ApplicationEventPublisher publisher;
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void handleMethodExecution(MethodExecutionEvent event) {
        if (shouldActivate()) {
            ChaosType chaosType = selectChaosType();
            applyChaos(chaosType, event);
        }
    }
    
    private boolean shouldActivate() {
        return Math.random() < properties.getLevel();
    }
    
    private ChaosType selectChaosType() {
        List<ChaosType> enabledTypes = properties.getEnabledChaosTypes();
        return enabledTypes.get((int) (Math.random() * enabledTypes.size()));
    }
    
    private void applyChaos(ChaosType chaosType, MethodExecutionEvent event) {
        switch (chaosType) {
            case LATENCY:
                applyLatency(event);
                break;
            case EXCEPTION:
                applyException(event);
                break;
            case KILL_APPLICATION:
                killApplication(event);
                break;
            case MEMORY_LEAK:
                applyMemoryLeak(event);
                break;
        }
    }
    
    private void applyLatency(MethodExecutionEvent event) {
        Duration latency = properties.getLatencyRange().random();
        
        try {
            Thread.sleep(latency.toMillis());
            
            Counter.builder("chaos.monkey.latency")
                .tag("method", event.getMethodName())
                .tag("class", event.getClassName())
                .register(meterRegistry)
                .increment();
                
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void applyException(MethodExecutionEvent event) {
        RuntimeException exception = properties.getExceptionTypes().random();
        
        Counter.builder("chaos.monkey.exceptions")
            .tag("method", event.getMethodName())
            .tag("class", event.getClassName())
            .tag("exception", exception.getClass().getSimpleName())
            .register(meterRegistry)
            .increment();
            
        throw exception;
    }
    
    private void killApplication(MethodExecutionEvent event) {
        publisher.publishEvent(new ChaosKillApplicationEvent(
            "Chaos Monkey killed application during: " + event.getMethodName()));
            
        // Give some time for graceful shutdown
        new Thread(() -> {
            try {
                Thread.sleep(Duration.ofSeconds(5).toMillis());
                System.exit(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}

// Aspect for method interception
@Aspect
@Component
@ConditionalOnProperty(prefix = "chaos.monkey", name = "enabled")
public class ChaosMonkeyAspect {
    private final SpringBootChaosMonkey chaosMonkey;
    
    @Around("@annotation(ChaosMonkeyTarget) || within(@ChaosMonkeyTarget *)")
    public Object aroundMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodExecutionEvent event = new MethodExecutionEvent(
            joinPoint.getSignature().getName(),
            joinPoint.getTarget().getClass().getSimpleName()
        );
        
        chaosMonkey.handleMethodExecution(event);
        
        return joinPoint.proceed();
    }
}

// Annotation for targeting specific methods/classes
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ChaosMonkeyTarget {
    ChaosType[] value() default {ChaosType.LATENCY, ChaosType.EXCEPTION};
}
```

## 4. Self-Healing Systems

### 4.1 Auto-Recovery Mechanisms

```java
// Self-Healing System Manager
@Component
public class SelfHealingSystemManager {
    private final List<HealthChecker> healthCheckers;
    private final List<RecoveryStrategy> recoveryStrategies;
    private final RecoveryExecutor recoveryExecutor;
    private final ApplicationEventPublisher eventPublisher;
    private final ScheduledExecutorService scheduler;
    
    @PostConstruct
    public void startHealthMonitoring() {
        scheduler.scheduleAtFixedRate(this::performHealthCheck, 30, 30, TimeUnit.SECONDS);
    }
    
    private void performHealthCheck() {
        for (HealthChecker checker : healthCheckers) {
            HealthStatus status = checker.checkHealth();
            
            if (status.isUnhealthy()) {
                triggerRecovery(checker.getComponentName(), status);
            }
        }
    }
    
    private void triggerRecovery(String componentName, HealthStatus status) {
        List<RecoveryStrategy> applicableStrategies = recoveryStrategies.stream()
            .filter(strategy -> strategy.canHandle(componentName, status))
            .sorted(Comparator.comparing(RecoveryStrategy::getPriority))
            .collect(Collectors.toList());
            
        if (applicableStrategies.isEmpty()) {
            eventPublisher.publishEvent(new NoRecoveryStrategyFoundEvent(componentName, status));
            return;
        }
        
        for (RecoveryStrategy strategy : applicableStrategies) {
            try {
                RecoveryResult result = recoveryExecutor.execute(strategy, componentName, status);
                
                if (result.isSuccessful()) {
                    eventPublisher.publishEvent(new RecoverySuccessfulEvent(
                        componentName, strategy.getName(), result));
                    break;
                } else {
                    eventPublisher.publishEvent(new RecoveryFailedEvent(
                        componentName, strategy.getName(), result));
                }
                
            } catch (Exception e) {
                eventPublisher.publishEvent(new RecoveryExceptionEvent(
                    componentName, strategy.getName(), e));
            }
        }
    }
}

// Database Connection Recovery Strategy
@Component
public class DatabaseConnectionRecoveryStrategy implements RecoveryStrategy {
    private final DataSourceManager dataSourceManager;
    
    @Override
    public boolean canHandle(String componentName, HealthStatus status) {
        return componentName.contains("database") && 
               status.getDetails().containsKey("connection_failed");
    }
    
    @Override
    public RecoveryResult execute(String componentName, HealthStatus status) {
        try {
            // Step 1: Test current connection
            if (testConnection()) {
                return RecoveryResult.success("Connection is already healthy");
            }
            
            // Step 2: Refresh connection pool
            dataSourceManager.refreshConnectionPool();
            
            if (testConnection()) {
                return RecoveryResult.success("Connection pool refresh successful");
            }
            
            // Step 3: Recreate datasource
            dataSourceManager.recreateDataSource();
            
            if (testConnection()) {
                return RecoveryResult.success("DataSource recreation successful");
            }
            
            return RecoveryResult.failure("All recovery attempts failed");
            
        } catch (Exception e) {
            return RecoveryResult.failure("Recovery failed with exception: " + e.getMessage());
        }
    }
    
    private boolean testConnection() {
        try {
            dataSourceManager.getDataSource().getConnection().close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public int getPriority() {
        return 1;
    }
    
    @Override
    public String getName() {
        return "database-connection-recovery";
    }
}

// Circuit Breaker Recovery Strategy
@Component
public class CircuitBreakerRecoveryStrategy implements RecoveryStrategy {
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Override
    public boolean canHandle(String componentName, HealthStatus status) {
        return status.getDetails().containsKey("circuit_breaker_open");
    }
    
    @Override
    public RecoveryResult execute(String componentName, HealthStatus status) {
        String circuitBreakerName = (String) status.getDetails().get("circuit_breaker_name");
        IntelligentCircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
        
        // Force circuit breaker to half-open state for recovery testing
        circuitBreaker.transitionToHalfOpenState();
        
        // Perform test calls
        int successfulTests = 0;
        int totalTests = 5;
        
        for (int i = 0; i < totalTests; i++) {
            try {
                // Perform a test call through the circuit breaker
                circuitBreaker.execute(() -> performHealthCheck(componentName));
                successfulTests++;
            } catch (Exception e) {
                // Test call failed
            }
            
            try {
                Thread.sleep(1000); // Wait between tests
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        double successRate = (double) successfulTests / totalTests;
        
        if (successRate >= 0.6) {
            return RecoveryResult.success(
                String.format("Circuit breaker recovery successful. Success rate: %.2f", successRate));
        } else {
            return RecoveryResult.failure(
                String.format("Circuit breaker recovery failed. Success rate: %.2f", successRate));
        }
    }
    
    private String performHealthCheck(String componentName) {
        // Perform actual health check for the component
        return "OK";
    }
}
```

### 4.2 Automated Scaling and Load Balancing

```java
// Automated Scaling Manager
@Component
public class AutoScalingManager {
    private final LoadBalancer loadBalancer;
    private final InstanceManager instanceManager;
    private final MetricsCollector metricsCollector;
    private final ScalingConfig config;
    
    @Scheduled(fixedRate = 60000) // Check every minute
    public void performScalingDecision() {
        ScalingMetrics metrics = metricsCollector.getCurrentMetrics();
        ScalingDecision decision = makeScalingDecision(metrics);
        
        if (decision.shouldScale()) {
            executeScaling(decision);
        }
    }
    
    private ScalingDecision makeScalingDecision(ScalingMetrics metrics) {
        // CPU-based scaling
        if (metrics.getAverageCpuUsage() > config.getCpuScaleUpThreshold()) {
            int targetInstances = calculateTargetInstances(metrics, ScaleDirection.UP);
            return ScalingDecision.scaleUp(targetInstances, "High CPU usage");
        }
        
        if (metrics.getAverageCpuUsage() < config.getCpuScaleDownThreshold() &&
            metrics.getCurrentInstances() > config.getMinInstances()) {
            int targetInstances = calculateTargetInstances(metrics, ScaleDirection.DOWN);
            return ScalingDecision.scaleDown(targetInstances, "Low CPU usage");
        }
        
        // Memory-based scaling
        if (metrics.getAverageMemoryUsage() > config.getMemoryScaleUpThreshold()) {
            int targetInstances = calculateTargetInstances(metrics, ScaleDirection.UP);
            return ScalingDecision.scaleUp(targetInstances, "High memory usage");
        }
        
        // Request rate-based scaling
        if (metrics.getRequestsPerSecond() > config.getRequestRateThreshold()) {
            int targetInstances = calculateTargetInstances(metrics, ScaleDirection.UP);
            return ScalingDecision.scaleUp(targetInstances, "High request rate");
        }
        
        return ScalingDecision.noScaling();
    }
    
    private void executeScaling(ScalingDecision decision) {
        if (decision.getDirection() == ScaleDirection.UP) {
            scaleUp(decision.getTargetInstances(), decision.getReason());
        } else {
            scaleDown(decision.getTargetInstances(), decision.getReason());
        }
    }
    
    private void scaleUp(int targetInstances, String reason) {
        int currentInstances = instanceManager.getCurrentInstanceCount();
        int instancesToAdd = targetInstances - currentInstances;
        
        for (int i = 0; i < instancesToAdd; i++) {
            ServiceInstance newInstance = instanceManager.startNewInstance();
            
            // Wait for instance to be ready
            waitForInstanceReady(newInstance);
            
            // Add to load balancer
            loadBalancer.addInstance(newInstance);
        }
        
        eventPublisher.publishEvent(new ScalingEvent(
            ScaleDirection.UP, currentInstances, targetInstances, reason));
    }
    
    private void scaleDown(int targetInstances, String reason) {
        int currentInstances = instanceManager.getCurrentInstanceCount();
        int instancesToRemove = currentInstances - targetInstances;
        
        List<ServiceInstance> instancesToShutdown = selectInstancesForShutdown(instancesToRemove);
        
        for (ServiceInstance instance : instancesToShutdown) {
            // Remove from load balancer first
            loadBalancer.removeInstance(instance);
            
            // Graceful shutdown
            instanceManager.shutdownInstance(instance);
        }
        
        eventPublisher.publishEvent(new ScalingEvent(
            ScaleDirection.DOWN, currentInstances, targetInstances, reason));
    }
}
```

## 5. Practical Exercises

### Exercise 1: Implement Custom Circuit Breaker
Create a custom circuit breaker with the following features:
- Configurable failure thresholds
- Multiple failure types handling
- Metrics collection and reporting
- Integration with Spring Boot Actuator

### Exercise 2: Build Chaos Engineering Experiment
Design and implement a chaos engineering experiment that:
- Tests your application's resilience to database failures
- Measures system recovery time
- Provides detailed metrics and reporting
- Includes automated rollback mechanisms

### Exercise 3: Create Self-Healing System
Implement a self-healing system that:
- Monitors application health continuously
- Automatically detects and recovers from common failures
- Provides recovery strategy plugins
- Includes comprehensive logging and alerting

## 6. Assessment Questions

1. **Circuit Breaker Design**: Design a circuit breaker system for a microservices architecture. Explain your failure detection strategy and recovery mechanisms.

2. **Bulkhead Implementation**: Implement bulkhead isolation for different types of operations in a Spring Boot application. Show how you would configure and monitor the isolation.

3. **Chaos Engineering Strategy**: Create a comprehensive chaos engineering strategy for a production system, including experiment design, safety measures, and rollback procedures.

4. **Self-Healing Architecture**: Design a self-healing system architecture that can automatically detect and recover from various types of failures without human intervention.

## Summary

Today you mastered advanced resilience engineering patterns including:

- **Circuit Breakers**: Intelligent failure detection and recovery with predictive capabilities
- **Bulkhead Patterns**: Thread pool and semaphore-based isolation strategies
- **Chaos Engineering**: Systematic failure injection and system resilience testing
- **Self-Healing Systems**: Automated detection, recovery, and scaling mechanisms
- **Observability**: Comprehensive metrics and monitoring for resilience patterns

These patterns are essential for building robust, fault-tolerant distributed systems that can handle failures gracefully and recover automatically.

## Next Steps

Tomorrow, we'll explore **AI/ML Integration in Enterprise Java** focusing on TensorFlow Java integration, ML pipelines, real-time inference systems, and building intelligent, learning enterprise applications.