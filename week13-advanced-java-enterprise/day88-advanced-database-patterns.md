# Day 88: Advanced Database Patterns - Connection Pooling, Transactions & Optimization

## Overview
Today we explore advanced database patterns and optimization techniques essential for high-performance enterprise applications. We'll cover connection pooling strategies, transaction management patterns, database optimization techniques, caching strategies, and monitoring approaches that ensure scalability and reliability.

## Learning Objectives
- Implement advanced connection pooling with HikariCP and custom configurations
- Master transaction management patterns including distributed transactions
- Apply database optimization techniques for performance and scalability
- Implement effective caching strategies with Redis and application-level caching
- Use database monitoring and profiling tools for performance analysis
- Design resilient database access patterns for enterprise applications

## 1. Advanced Connection Pooling

### HikariCP Configuration and Optimization
```java
package com.javajourney.database.pooling;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Advanced HikariCP connection pool configuration with monitoring and optimization
 */
@Configuration
public class ConnectionPoolConfiguration {
    
    /**
     * Primary database connection pool configuration
     */
    @Bean(name = "primaryDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.primary")
    public HikariDataSource primaryDataSource(HikariConfig primaryConfig) {
        return new HikariDataSource(primaryConfig);
    }
    
    /**
     * Read-only replica connection pool configuration
     */
    @Bean(name = "readOnlyDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.readonly")
    public HikariDataSource readOnlyDataSource(HikariConfig readOnlyConfig) {
        return new HikariDataSource(readOnlyConfig);
    }
    
    /**
     * Primary database pool configuration with advanced settings
     */
    @Bean(name = "primaryConfig")
    public HikariConfig primaryHikariConfig() {
        HikariConfig config = new HikariConfig();
        
        // Basic connection settings
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/enterprise_db");
        config.setUsername("app_user");
        config.setPassword("secure_password");
        config.setDriverClassName("org.postgresql.Driver");
        
        // Pool sizing - based on CPU cores and expected load
        int coreCount = Runtime.getRuntime().availableProcessors();
        config.setMaximumPoolSize(coreCount * 2 + 1); // Conservative sizing
        config.setMinimumIdle(Math.max(2, coreCount / 2)); // Minimum connections
        
        // Connection timing settings
        config.setConnectionTimeout(30000); // 30 seconds
        config.setIdleTimeout(600000); // 10 minutes
        config.setMaxLifetime(1800000); // 30 minutes
        config.setLeakDetectionThreshold(60000); // 1 minute
        
        // Validation and health checks
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000); // 5 seconds
        
        // Performance optimizations
        config.setAllowPoolSuspension(false); // Better performance
        config.setAutoCommit(false); // Explicit transaction control
        config.setReadOnly(false);
        config.setIsolateInternalQueries(false);
        
        // Connection properties for PostgreSQL optimization
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        
        // Monitoring and metrics
        config.setMetricRegistry(null); // Will be set up with Micrometer
        config.setHealthCheckRegistry(null);
        
        // Pool name for monitoring
        config.setPoolName("PrimaryConnectionPool");
        
        return config;
    }
    
    /**
     * Read-only replica configuration optimized for read workloads
     */
    @Bean(name = "readOnlyConfig")
    public HikariConfig readOnlyHikariConfig() {
        HikariConfig config = new HikariConfig();
        
        // Connection settings for read replica
        config.setJdbcUrl("jdbc:postgresql://readonly-replica:5432/enterprise_db");
        config.setUsername("readonly_user");
        config.setPassword("readonly_password");
        config.setDriverClassName("org.postgresql.Driver");
        
        // Larger pool for read operations
        int coreCount = Runtime.getRuntime().availableProcessors();
        config.setMaximumPoolSize(coreCount * 3); // More connections for reads
        config.setMinimumIdle(coreCount);
        
        // Longer connection lifetimes for read-only operations
        config.setConnectionTimeout(20000);
        config.setIdleTimeout(900000); // 15 minutes
        config.setMaxLifetime(3600000); // 1 hour
        
        // Read-only optimizations
        config.setReadOnly(true);
        config.setAutoCommit(true); // Auto-commit for read operations
        
        // Pool name
        config.setPoolName("ReadOnlyConnectionPool");
        
        return config;
    }
    
    /**
     * Connection pool health monitoring
     */
    @Component
    public static class ConnectionPoolMonitor {
        private final HikariDataSource primaryDataSource;
        private final HikariDataSource readOnlyDataSource;
        
        public ConnectionPoolMonitor(@Qualifier("primaryDataSource") HikariDataSource primaryDataSource,
                                   @Qualifier("readOnlyDataSource") HikariDataSource readOnlyDataSource) {
            this.primaryDataSource = primaryDataSource;
            this.readOnlyDataSource = readOnlyDataSource;
        }
        
        @EventListener
        @Async
        public void monitorConnectionPools() {
            logPoolMetrics("Primary", primaryDataSource.getHikariPoolMXBean());
            logPoolMetrics("ReadOnly", readOnlyDataSource.getHikariPoolMXBean());
        }
        
        private void logPoolMetrics(String poolName, HikariPoolMXBean poolMXBean) {
            System.out.printf("=== %s Connection Pool Metrics ===%n", poolName);
            System.out.printf("Active Connections: %d%n", poolMXBean.getActiveConnections());
            System.out.printf("Idle Connections: %d%n", poolMXBean.getIdleConnections());
            System.out.printf("Total Connections: %d%n", poolMXBean.getTotalConnections());
            System.out.printf("Threads Waiting: %d%n", poolMXBean.getThreadsAwaitingConnection());
            System.out.println();
        }
        
        public ConnectionPoolHealth getPoolHealth() {
            var primaryHealth = assessPoolHealth(primaryDataSource.getHikariPoolMXBean());
            var readOnlyHealth = assessPoolHealth(readOnlyDataSource.getHikariPoolMXBean());
            
            return new ConnectionPoolHealth(primaryHealth, readOnlyHealth);
        }
        
        private PoolHealthStatus assessPoolHealth(HikariPoolMXBean poolMXBean) {
            int active = poolMXBean.getActiveConnections();
            int total = poolMXBean.getTotalConnections();
            int waiting = poolMXBean.getThreadsAwaitingConnection();
            
            double utilization = (double) active / total;
            
            if (waiting > 5 || utilization > 0.9) {
                return PoolHealthStatus.CRITICAL;
            } else if (waiting > 2 || utilization > 0.7) {
                return PoolHealthStatus.WARNING;
            } else {
                return PoolHealthStatus.HEALTHY;
            }
        }
    }
    
    public enum PoolHealthStatus {
        HEALTHY, WARNING, CRITICAL
    }
    
    public record ConnectionPoolHealth(PoolHealthStatus primary, PoolHealthStatus readOnly) {}
}
```

### Dynamic Connection Pool Management
```java
package com.javajourney.database.pooling;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Dynamic connection pool management with auto-scaling and load balancing
 */
@Service
public class DynamicConnectionPoolManager {
    
    private final List<HikariDataSource> dataSources;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final LoadBalancer loadBalancer;
    private final PoolMetricsCollector metricsCollector;
    
    public DynamicConnectionPoolManager(List<HikariDataSource> dataSources) {
        this.dataSources = dataSources;
        this.loadBalancer = new RoundRobinLoadBalancer(dataSources);
        this.metricsCollector = new PoolMetricsCollector(dataSources);
        
        // Start monitoring and auto-scaling
        startPoolMonitoring();
    }
    
    /**
     * Get optimal data source based on current load
     */
    public DataSource getOptimalDataSource(QueryType queryType) {
        return switch (queryType) {
            case READ() -> loadBalancer.getReadOnlyDataSource();
            case write(), update(), delete() -> loadBalancer.getPrimaryDataSource();
        };
    }
    
    /**
     * Auto-scaling based on pool utilization
     */
    private void startPoolMonitoring() {
        scheduler.scheduleAtFixedRate(this::adjustPoolSizes, 30, 30, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::collectMetrics, 10, 10, TimeUnit.SECONDS);
    }
    
    private void adjustPoolSizes() {
        for (HikariDataSource dataSource : dataSources) {
            HikariPoolMXBean poolMXBean = dataSource.getHikariPoolMXBean();
            PoolMetrics metrics = metricsCollector.getMetrics(dataSource.getPoolName());
            
            if (shouldScaleUp(poolMXBean, metrics)) {
                scaleUpPool(dataSource, poolMXBean);
            } else if (shouldScaleDown(poolMXBean, metrics)) {
                scaleDownPool(dataSource, poolMXBean);
            }
        }
    }
    
    private boolean shouldScaleUp(HikariPoolMXBean poolMXBean, PoolMetrics metrics) {
        double utilization = (double) poolMXBean.getActiveConnections() / poolMXBean.getTotalConnections();
        int waitingThreads = poolMXBean.getThreadsAwaitingConnection();
        
        return utilization > 0.8 && waitingThreads > 3 && 
               metrics.getAverageWaitTime() > 100; // 100ms threshold
    }
    
    private boolean shouldScaleDown(HikariPoolMXBean poolMXBean, PoolMetrics metrics) {
        double utilization = (double) poolMXBean.getActiveConnections() / poolMXBean.getTotalConnections();
        int idleConnections = poolMXBean.getIdleConnections();
        
        return utilization < 0.3 && idleConnections > 5 && 
               metrics.getAverageWaitTime() < 10; // Low contention
    }
    
    private void scaleUpPool(HikariDataSource dataSource, HikariPoolMXBean poolMXBean) {
        int currentMax = poolMXBean.getTotalConnections();
        int newMax = Math.min(currentMax + 5, 50); // Cap at 50 connections
        
        dataSource.setMaximumPoolSize(newMax);
        System.out.printf("Scaled up %s pool from %d to %d connections%n", 
                         dataSource.getPoolName(), currentMax, newMax);
    }
    
    private void scaleDownPool(HikariDataSource dataSource, HikariPoolMXBean poolMXBean) {
        int currentMax = poolMXBean.getTotalConnections();
        int newMax = Math.max(currentMax - 2, 5); // Minimum 5 connections
        
        dataSource.setMaximumPoolSize(newMax);
        System.out.printf("Scaled down %s pool from %d to %d connections%n", 
                         dataSource.getPoolName(), currentMax, newMax);
    }
    
    /**
     * Load balancer for distributing connections
     */
    public static class RoundRobinLoadBalancer implements LoadBalancer {
        private final List<HikariDataSource> readOnlyDataSources;
        private final HikariDataSource primaryDataSource;
        private final AtomicInteger readOnlyIndex = new AtomicInteger(0);
        
        public RoundRobinLoadBalancer(List<HikariDataSource> dataSources) {
            this.primaryDataSource = dataSources.stream()
                .filter(ds -> "PrimaryConnectionPool".equals(ds.getPoolName()))
                .findFirst()
                .orElseThrow();
            
            this.readOnlyDataSources = dataSources.stream()
                .filter(ds -> ds.getPoolName().contains("ReadOnly"))
                .collect(Collectors.toList());
        }
        
        @Override
        public DataSource getPrimaryDataSource() {
            return primaryDataSource;
        }
        
        @Override
        public DataSource getReadOnlyDataSource() {
            if (readOnlyDataSources.isEmpty()) {
                return primaryDataSource; // Fallback to primary
            }
            
            int index = readOnlyIndex.getAndIncrement() % readOnlyDataSources.size();
            return readOnlyDataSources.get(index);
        }
    }
    
    /**
     * Pool metrics collector for monitoring
     */
    public static class PoolMetricsCollector {
        private final Map<String, PoolMetrics> metricsMap = new ConcurrentHashMap<>();
        private final List<HikariDataSource> dataSources;
        
        public PoolMetricsCollector(List<HikariDataSource> dataSources) {
            this.dataSources = dataSources;
        }
        
        public void collectMetrics() {
            for (HikariDataSource dataSource : dataSources) {
                String poolName = dataSource.getPoolName();
                HikariPoolMXBean poolMXBean = dataSource.getHikariPoolMXBean();
                
                PoolMetrics metrics = metricsMap.computeIfAbsent(poolName, 
                    k -> new PoolMetrics());
                
                metrics.updateMetrics(
                    poolMXBean.getActiveConnections(),
                    poolMXBean.getIdleConnections(),
                    poolMXBean.getThreadsAwaitingConnection(),
                    System.currentTimeMillis()
                );
            }
        }
        
        public PoolMetrics getMetrics(String poolName) {
            return metricsMap.getOrDefault(poolName, new PoolMetrics());
        }
    }
    
    public interface LoadBalancer {
        DataSource getPrimaryDataSource();
        DataSource getReadOnlyDataSource();
    }
    
    public enum QueryType {
        read, write, update, delete
    }
}
```

## 2. Advanced Transaction Management

### Declarative Transaction Management with Patterns
```java
package com.javajourney.database.transactions;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Isolation;

/**
 * Advanced transaction management patterns and strategies
 */
@Service
public class TransactionService {
    
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    
    /**
     * Saga pattern implementation for distributed transactions
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public OrderResult processOrderWithSaga(OrderRequest orderRequest) {
        SagaTransaction saga = new SagaTransaction();
        
        try {
            // Step 1: Reserve inventory
            InventoryReservation reservation = inventoryService.reserveItems(
                orderRequest.getItems());
            saga.addCompensation(() -> inventoryService.releaseReservation(reservation));
            
            // Step 2: Process payment
            PaymentResult payment = paymentService.processPayment(
                orderRequest.getPaymentInfo(), orderRequest.getTotalAmount());
            saga.addCompensation(() -> paymentService.refundPayment(payment));
            
            // Step 3: Create order
            Order order = orderRepository.save(new Order(orderRequest, reservation, payment));
            saga.addCompensation(() -> orderRepository.delete(order));
            
            // Step 4: Send confirmation (non-transactional)
            notificationService.sendOrderConfirmation(order);
            
            saga.markCompleted();
            return new OrderResult(true, order.getId(), "Order processed successfully");
            
        } catch (Exception e) {
            // Execute compensating transactions in reverse order
            saga.compensate();
            throw new OrderProcessingException("Order processing failed", e);
        }
    }
    
    /**
     * Read-only transaction with optimizations
     */
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<OrderSummary> getOrderSummaries(String customerId, Pageable pageable) {
        // Read-only transactions can use read replicas
        return orderRepository.findOrderSummariesByCustomerId(customerId, pageable);
    }
    
    /**
     * Batch processing with transaction chunking
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public BatchProcessResult processBulkOrders(List<OrderRequest> orderRequests) {
        int batchSize = 100;
        int successCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (int i = 0; i < orderRequests.size(); i += batchSize) {
            List<OrderRequest> batch = orderRequests.subList(i, 
                Math.min(i + batchSize, orderRequests.size()));
            
            try {
                successCount += processBatch(batch);
            } catch (Exception e) {
                errors.add("Batch " + (i / batchSize + 1) + ": " + e.getMessage());
            }
        }
        
        return new BatchProcessResult(successCount, errors);
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private int processBatch(List<OrderRequest> batch) {
        return batch.stream()
            .mapToInt(order -> processOrder(order) ? 1 : 0)
            .sum();
    }
    
    /**
     * Two-phase commit pattern for distributed transactions
     */
    public class TwoPhaseCommitManager {
        private final List<TransactionParticipant> participants;
        
        public TwoPhaseCommitManager(List<TransactionParticipant> participants) {
            this.participants = participants;
        }
        
        public boolean executeDistributedTransaction(TransactionContext context) {
            // Phase 1: Prepare
            List<TransactionParticipant> preparedParticipants = new ArrayList<>();
            
            for (TransactionParticipant participant : participants) {
                try {
                    if (participant.prepare(context)) {
                        preparedParticipants.add(participant);
                    } else {
                        // Abort all prepared participants
                        abortParticipants(preparedParticipants);
                        return false;
                    }
                } catch (Exception e) {
                    abortParticipants(preparedParticipants);
                    throw new DistributedTransactionException("Prepare phase failed", e);
                }
            }
            
            // Phase 2: Commit
            try {
                for (TransactionParticipant participant : preparedParticipants) {
                    participant.commit(context);
                }
                return true;
            } catch (Exception e) {
                // Emergency: some participants may have committed
                // Need manual intervention or compensation
                throw new DistributedTransactionException("Commit phase failed", e);
            }
        }
        
        private void abortParticipants(List<TransactionParticipant> participants) {
            for (TransactionParticipant participant : participants) {
                try {
                    participant.abort();
                } catch (Exception e) {
                    // Log error but continue aborting others
                    System.err.println("Failed to abort participant: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Saga transaction coordinator
     */
    public static class SagaTransaction {
        private final List<Runnable> compensations = new ArrayList<>();
        private boolean completed = false;
        
        public void addCompensation(Runnable compensation) {
            compensations.add(compensation);
        }
        
        public void markCompleted() {
            this.completed = true;
        }
        
        public void compensate() {
            if (completed) return;
            
            // Execute compensations in reverse order
            Collections.reverse(compensations);
            for (Runnable compensation : compensations) {
                try {
                    compensation.run();
                } catch (Exception e) {
                    // Log compensation failure but continue
                    System.err.println("Compensation failed: " + e.getMessage());
                }
            }
        }
    }
    
    public interface TransactionParticipant {
        boolean prepare(TransactionContext context);
        void commit(TransactionContext context);
        void abort();
    }
}
```

### Custom Transaction Manager
```java
package com.javajourney.database.transactions;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

/**
 * Custom transaction manager with advanced features
 */
public class CustomTransactionManager extends AbstractPlatformTransactionManager {
    
    private final DataSource dataSource;
    private final TransactionMetrics metrics;
    private final CircuitBreaker circuitBreaker;
    
    public CustomTransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
        this.metrics = new TransactionMetrics();
        this.circuitBreaker = new TransactionCircuitBreaker();
    }
    
    @Override
    protected Object doGetTransaction() throws TransactionException {
        CustomTransactionObject txObject = new CustomTransactionObject();
        
        try {
            Connection connection = dataSource.getConnection();
            txObject.setConnection(connection);
            txObject.setTransactionStartTime(System.currentTimeMillis());
            
            return txObject;
        } catch (SQLException e) {
            throw new TransactionException("Failed to obtain database connection", e);
        }
    }
    
    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) 
            throws TransactionException {
        
        CustomTransactionObject txObject = (CustomTransactionObject) transaction;
        Connection connection = txObject.getConnection();
        
        try {
            // Check circuit breaker
            if (!circuitBreaker.allowRequest()) {
                throw new TransactionException("Circuit breaker is open");
            }
            
            // Set isolation level
            if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
                connection.setTransactionIsolation(definition.getIsolationLevel());
            }
            
            // Set read-only flag
            connection.setReadOnly(definition.isReadOnly());
            
            // Disable auto-commit
            connection.setAutoCommit(false);
            
            // Set timeout if specified
            if (definition.getTimeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
                // Implementation depends on driver support
                connection.setNetworkTimeout(Executors.newSingleThreadExecutor(), 
                                           definition.getTimeout() * 1000);
            }
            
            txObject.setTransactionActive(true);
            
        } catch (SQLException e) {
            circuitBreaker.recordFailure();
            throw new TransactionException("Failed to begin transaction", e);
        }
    }
    
    @Override
    protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
        CustomTransactionObject txObject = (CustomTransactionObject) status.getTransaction();
        Connection connection = txObject.getConnection();
        
        try {
            connection.commit();
            
            // Record success metrics
            long duration = System.currentTimeMillis() - txObject.getTransactionStartTime();
            metrics.recordTransactionSuccess(duration);
            circuitBreaker.recordSuccess();
            
        } catch (SQLException e) {
            metrics.recordTransactionFailure();
            circuitBreaker.recordFailure();
            throw new TransactionException("Failed to commit transaction", e);
        }
    }
    
    @Override
    protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
        CustomTransactionObject txObject = (CustomTransactionObject) status.getTransaction();
        Connection connection = txObject.getConnection();
        
        try {
            connection.rollback();
            
            // Record rollback metrics
            long duration = System.currentTimeMillis() - txObject.getTransactionStartTime();
            metrics.recordTransactionRollback(duration);
            
        } catch (SQLException e) {
            throw new TransactionException("Failed to rollback transaction", e);
        }
    }
    
    @Override
    protected void doCleanupAfterCompletion(Object transaction) {
        CustomTransactionObject txObject = (CustomTransactionObject) transaction;
        Connection connection = txObject.getConnection();
        
        try {
            connection.setAutoCommit(true);
            connection.close();
        } catch (SQLException e) {
            // Log error but don't throw exception during cleanup
            System.err.println("Failed to cleanup transaction: " + e.getMessage());
        }
    }
    
    /**
     * Custom transaction object
     */
    private static class CustomTransactionObject {
        private Connection connection;
        private boolean transactionActive = false;
        private long transactionStartTime;
        
        // Getters and setters
        public Connection getConnection() { return connection; }
        public void setConnection(Connection connection) { this.connection = connection; }
        public boolean isTransactionActive() { return transactionActive; }
        public void setTransactionActive(boolean active) { this.transactionActive = active; }
        public long getTransactionStartTime() { return transactionStartTime; }
        public void setTransactionStartTime(long startTime) { this.transactionStartTime = startTime; }
    }
    
    /**
     * Transaction metrics collector
     */
    public static class TransactionMetrics {
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong failureCount = new AtomicLong(0);
        private final AtomicLong rollbackCount = new AtomicLong(0);
        private final AtomicLong totalDuration = new AtomicLong(0);
        
        public void recordTransactionSuccess(long duration) {
            successCount.incrementAndGet();
            totalDuration.addAndGet(duration);
        }
        
        public void recordTransactionFailure() {
            failureCount.incrementAndGet();
        }
        
        public void recordTransactionRollback(long duration) {
            rollbackCount.incrementAndGet();
            totalDuration.addAndGet(duration);
        }
        
        public TransactionStatistics getStatistics() {
            long success = successCount.get();
            long failure = failureCount.get();
            long rollback = rollbackCount.get();
            long total = success + failure + rollback;
            
            return new TransactionStatistics(
                success, failure, rollback, total,
                total > 0 ? totalDuration.get() / total : 0
            );
        }
    }
    
    public record TransactionStatistics(
        long successCount,
        long failureCount,
        long rollbackCount,
        long totalCount,
        long averageDuration
    ) {}
}
```

## 3. Database Optimization Techniques

### Query Optimization and Performance Analysis
```java
package com.javajourney.database.optimization;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.concurrent.CompletableFuture;

/**
 * Database optimization techniques and performance analysis
 */
@Service
public class DatabaseOptimizationService {
    
    private final JdbcTemplate jdbcTemplate;
    private final QueryAnalyzer queryAnalyzer;
    private final IndexOptimizer indexOptimizer;
    
    /**
     * Query performance analyzer
     */
    @Component
    public static class QueryAnalyzer {
        private final JdbcTemplate jdbcTemplate;
        private final Map<String, QueryMetrics> queryMetricsMap = new ConcurrentHashMap<>();
        
        public QueryAnalyzer(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }
        
        /**
         * Analyze query execution plan
         */
        public QueryExecutionPlan analyzeQuery(String sql, Object... parameters) {
            String explainQuery = "EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) " + sql;
            
            long startTime = System.nanoTime();
            List<Map<String, Object>> result = jdbcTemplate.queryForList(explainQuery, parameters);
            long executionTime = System.nanoTime() - startTime;
            
            // Parse execution plan (simplified)
            Map<String, Object> plan = result.get(0);
            String planJson = (String) plan.get("QUERY PLAN");
            
            // Update metrics
            String queryHash = calculateQueryHash(sql);
            updateQueryMetrics(queryHash, executionTime / 1_000_000); // Convert to milliseconds
            
            return new QueryExecutionPlan(sql, planJson, executionTime / 1_000_000);
        }
        
        /**
         * Identify slow queries
         */
        public List<SlowQuery> getSlowQueries(long thresholdMs) {
            return queryMetricsMap.entrySet().stream()
                .filter(entry -> entry.getValue().getAverageExecutionTime() > thresholdMs)
                .map(entry -> new SlowQuery(
                    entry.getKey(),
                    entry.getValue().getAverageExecutionTime(),
                    entry.getValue().getExecutionCount()
                ))
                .sorted((a, b) -> Long.compare(b.averageTime(), a.averageTime()))
                .collect(Collectors.toList());
        }
        
        /**
         * Generate optimization recommendations
         */
        public List<OptimizationRecommendation> generateRecommendations(String sql) {
            List<OptimizationRecommendation> recommendations = new ArrayList<>();
            
            // Check for missing indexes
            if (sql.toLowerCase().contains("where") && !hasOptimalIndex(sql)) {
                recommendations.add(new OptimizationRecommendation(
                    RecommendationType.ADD_INDEX,
                    "Consider adding an index on frequently queried columns",
                    generateIndexSuggestion(sql)
                ));
            }
            
            // Check for SELECT *
            if (sql.toLowerCase().contains("select *")) {
                recommendations.add(new OptimizationRecommendation(
                    RecommendationType.OPTIMIZE_PROJECTION,
                    "Avoid SELECT *, specify only needed columns",
                    "SELECT specific_column1, specific_column2 FROM table"
                ));
            }
            
            // Check for LIMIT usage in pagination
            if (sql.toLowerCase().contains("offset") && !sql.toLowerCase().contains("order by")) {
                recommendations.add(new OptimizationRecommendation(
                    RecommendationType.ADD_ORDER_BY,
                    "Add ORDER BY clause when using OFFSET for consistent results",
                    "ORDER BY primary_key"
                ));
            }
            
            return recommendations;
        }
        
        private String calculateQueryHash(String sql) {
            // Normalize query and create hash for grouping similar queries
            String normalized = sql.replaceAll("\\b\\d+\\b", "?")
                                  .replaceAll("'[^']*'", "?")
                                  .toLowerCase()
                                  .trim();
            return Integer.toHexString(normalized.hashCode());
        }
        
        private void updateQueryMetrics(String queryHash, long executionTime) {
            queryMetricsMap.compute(queryHash, (key, metrics) -> {
                if (metrics == null) {
                    return new QueryMetrics(executionTime, 1);
                } else {
                    return metrics.addExecution(executionTime);
                }
            });
        }
        
        private boolean hasOptimalIndex(String sql) {
            // Simplified index check - in practice, analyze EXPLAIN output
            return false; // Placeholder
        }
        
        private String generateIndexSuggestion(String sql) {
            // Extract WHERE conditions and suggest index
            return "CREATE INDEX idx_table_column ON table_name (column_name);";
        }
    }
    
    /**
     * Index optimization service
     */
    @Service
    public static class IndexOptimizer {
        private final JdbcTemplate jdbcTemplate;
        
        public IndexOptimizer(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }
        
        /**
         * Analyze index usage statistics
         */
        public List<IndexUsageStats> analyzeIndexUsage() {
            String query = """
                SELECT 
                    schemaname,
                    tablename,
                    attname,
                    n_distinct,
                    correlation,
                    idx_scan,
                    idx_tup_read,
                    idx_tup_fetch
                FROM pg_stats 
                JOIN pg_stat_user_indexes ON pg_stats.tablename = pg_stat_user_indexes.relname
                WHERE schemaname = 'public'
                ORDER BY idx_scan DESC
                """;
            
            return jdbcTemplate.query(query, (rs, rowNum) -> 
                new IndexUsageStats(
                    rs.getString("tablename"),
                    rs.getString("attname"),
                    rs.getLong("idx_scan"),
                    rs.getLong("idx_tup_read"),
                    rs.getDouble("correlation")
                )
            );
        }
        
        /**
         * Identify unused indexes
         */
        public List<UnusedIndex> findUnusedIndexes() {
            String query = """
                SELECT 
                    schemaname,
                    tablename,
                    indexname,
                    idx_scan
                FROM pg_stat_user_indexes 
                WHERE idx_scan = 0
                AND schemaname = 'public'
                ORDER BY tablename, indexname
                """;
            
            return jdbcTemplate.query(query, (rs, rowNum) ->
                new UnusedIndex(
                    rs.getString("tablename"),
                    rs.getString("indexname"),
                    calculateIndexSize(rs.getString("indexname"))
                )
            );
        }
        
        /**
         * Suggest composite indexes based on query patterns
         */
        public List<IndexSuggestion> suggestCompositeIndexes(List<String> frequentQueries) {
            List<IndexSuggestion> suggestions = new ArrayList<>();
            
            for (String query : frequentQueries) {
                List<String> whereColumns = extractWhereColumns(query);
                List<String> orderByColumns = extractOrderByColumns(query);
                
                if (whereColumns.size() > 1) {
                    // Suggest composite index: WHERE columns + ORDER BY columns
                    List<String> indexColumns = new ArrayList<>(whereColumns);
                    indexColumns.addAll(orderByColumns);
                    
                    suggestions.add(new IndexSuggestion(
                        extractTableName(query),
                        indexColumns,
                        "Composite index for multi-column WHERE clause"
                    ));
                }
            }
            
            return suggestions;
        }
        
        private long calculateIndexSize(String indexName) {
            String query = "SELECT pg_size_pretty(pg_total_relation_size(?))";
            return jdbcTemplate.queryForObject(query, Long.class, indexName);
        }
        
        private List<String> extractWhereColumns(String sql) {
            // Simplified extraction - use SQL parser in practice
            return List.of(); // Placeholder
        }
        
        private List<String> extractOrderByColumns(String sql) {
            // Simplified extraction - use SQL parser in practice
            return List.of(); // Placeholder
        }
        
        private String extractTableName(String sql) {
            // Simplified extraction - use SQL parser in practice
            return "table_name"; // Placeholder
        }
    }
    
    /**
     * Batch optimization for bulk operations
     */
    public void optimizedBatchInsert(List<Entity> entities) {
        String sql = "INSERT INTO entities (name, value, created_at) VALUES (?, ?, ?)";
        
        jdbcTemplate.batchUpdate(sql, entities, entities.size(), (ps, entity) -> {
            ps.setString(1, entity.getName());
            ps.setString(2, entity.getValue());
            ps.setTimestamp(3, Timestamp.from(entity.getCreatedAt()));
        });
    }
    
    /**
     * Parallel query execution for independent queries
     */
    public CompletableFuture<CombinedResult> executeParallelQueries(
            String query1, String query2, String query3) {
        
        CompletableFuture<List<Object>> result1 = CompletableFuture.supplyAsync(() ->
            jdbcTemplate.queryForList(query1));
        
        CompletableFuture<List<Object>> result2 = CompletableFuture.supplyAsync(() ->
            jdbcTemplate.queryForList(query2));
        
        CompletableFuture<List<Object>> result3 = CompletableFuture.supplyAsync(() ->
            jdbcTemplate.queryForList(query3));
        
        return CompletableFuture.allOf(result1, result2, result3)
            .thenApply(v -> new CombinedResult(
                result1.join(), result2.join(), result3.join()));
    }
    
    // Data classes
    public record QueryExecutionPlan(String sql, String executionPlan, long executionTimeMs) {}
    public record SlowQuery(String queryHash, long averageTime, long executionCount) {}
    public record OptimizationRecommendation(RecommendationType type, String description, String suggestion) {}
    public record IndexUsageStats(String tableName, String columnName, long scanCount, long tuplesRead, double correlation) {}
    public record UnusedIndex(String tableName, String indexName, long sizeBytes) {}
    public record IndexSuggestion(String tableName, List<String> columns, String reason) {}
    public record CombinedResult(List<Object> result1, List<Object> result2, List<Object> result3) {}
    
    public enum RecommendationType {
        ADD_INDEX, OPTIMIZE_PROJECTION, ADD_ORDER_BY, REWRITE_QUERY
    }
    
    public static class QueryMetrics {
        private final long totalExecutionTime;
        private final long executionCount;
        
        public QueryMetrics(long executionTime, long count) {
            this.totalExecutionTime = executionTime;
            this.executionCount = count;
        }
        
        public QueryMetrics addExecution(long executionTime) {
            return new QueryMetrics(
                this.totalExecutionTime + executionTime,
                this.executionCount + 1
            );
        }
        
        public long getAverageExecutionTime() {
            return executionCount > 0 ? totalExecutionTime / executionCount : 0;
        }
        
        public long getExecutionCount() { return executionCount; }
    }
}
```

## 4. Caching Strategies

### Multi-Level Caching Implementation
```java
package com.javajourney.database.caching;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Multi-level caching strategy with L1 (application) and L2 (Redis) caches
 */
@Service
public class MultiLevelCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final LoadingCache<String, Object> l1Cache;
    private final CacheMetrics cacheMetrics;
    
    public MultiLevelCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.l1Cache = buildL1Cache();
        this.cacheMetrics = new CacheMetrics();
    }
    
    private LoadingCache<String, Object> buildL1Cache() {
        return Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .recordStats()
            .removalListener((key, value, cause) -> {
                System.out.printf("L1 Cache eviction: %s, cause: %s%n", key, cause);
            })
            .build(key -> {
                // Load from L2 cache (Redis) if not in L1
                return loadFromL2Cache(key);
            });
    }
    
    /**
     * Get value with multi-level cache lookup
     */
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            // Try L1 cache first
            Object value = l1Cache.get(key);
            if (value != null) {
                cacheMetrics.recordL1Hit();
                return Optional.of(type.cast(value));
            }
            
            cacheMetrics.recordL1Miss();
            return Optional.empty();
            
        } catch (Exception e) {
            cacheMetrics.recordError();
            return Optional.empty();
        }
    }
    
    /**
     * Put value in both cache levels
     */
    public void put(String key, Object value, Duration expiration) {
        // Store in L1 cache
        l1Cache.put(key, value);
        
        // Store in L2 cache (Redis) with expiration
        redisTemplate.opsForValue().set(key, value, expiration);
        
        cacheMetrics.recordWrite();
    }
    
    /**
     * Invalidate from both cache levels
     */
    public void evict(String key) {
        l1Cache.invalidate(key);
        redisTemplate.delete(key);
        cacheMetrics.recordEviction();
    }
    
    /**
     * Load from L2 cache (Redis)
     */
    private Object loadFromL2Cache(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            cacheMetrics.recordL2Hit();
        } else {
            cacheMetrics.recordL2Miss();
        }
        return value;
    }
    
    /**
     * Cache-aside pattern implementation
     */
    public <T> T getOrLoad(String key, Class<T> type, Supplier<T> loader, Duration expiration) {
        Optional<T> cached = get(key, type);
        if (cached.isPresent()) {
            return cached.get();
        }
        
        // Load from source
        T value = loader.get();
        if (value != null) {
            put(key, value, expiration);
        }
        
        return value;
    }
    
    /**
     * Write-through cache pattern
     */
    public <T> T saveAndCache(String key, T value, Duration expiration, Consumer<T> persister) {
        // Write to persistent store first
        persister.accept(value);
        
        // Then update cache
        put(key, value, expiration);
        
        return value;
    }
    
    /**
     * Cache warming for frequently accessed data
     */
    @EventListener
    @Async
    public void warmCache(ApplicationReadyEvent event) {
        System.out.println("Starting cache warming...");
        
        // Warm frequently accessed data
        List<String> popularKeys = getPopularKeys();
        
        popularKeys.parallelStream().forEach(key -> {
            try {
                Object value = loadDataFromDatabase(key);
                if (value != null) {
                    put(key, value, Duration.ofHours(1));
                }
            } catch (Exception e) {
                System.err.println("Failed to warm cache for key: " + key);
            }
        });
        
        System.out.println("Cache warming completed");
    }
    
    /**
     * Distributed cache invalidation using Redis pub/sub
     */
    @Component
    public static class DistributedCacheInvalidation {
        private final RedisTemplate<String, String> redisTemplate;
        private final MultiLevelCacheService cacheService;
        
        public DistributedCacheInvalidation(RedisTemplate<String, String> redisTemplate,
                                          MultiLevelCacheService cacheService) {
            this.redisTemplate = redisTemplate;
            this.cacheService = cacheService;
            
            // Subscribe to cache invalidation messages
            redisTemplate.getConnectionFactory().getConnection()
                .subscribe((message, pattern) -> {
                    String key = new String(message.getBody());
                    cacheService.evictLocal(key);
                }, "cache-invalidation".getBytes());
        }
        
        public void broadcastInvalidation(String key) {
            redisTemplate.convertAndSend("cache-invalidation", key);
        }
    }
    
    /**
     * Cache metrics and monitoring
     */
    public static class CacheMetrics {
        private final AtomicLong l1Hits = new AtomicLong(0);
        private final AtomicLong l1Misses = new AtomicLong(0);
        private final AtomicLong l2Hits = new AtomicLong(0);
        private final AtomicLong l2Misses = new AtomicLong(0);
        private final AtomicLong writes = new AtomicLong(0);
        private final AtomicLong evictions = new AtomicLong(0);
        private final AtomicLong errors = new AtomicLong(0);
        
        public void recordL1Hit() { l1Hits.incrementAndGet(); }
        public void recordL1Miss() { l1Misses.incrementAndGet(); }
        public void recordL2Hit() { l2Hits.incrementAndGet(); }
        public void recordL2Miss() { l2Misses.incrementAndGet(); }
        public void recordWrite() { writes.incrementAndGet(); }
        public void recordEviction() { evictions.incrementAndGet(); }
        public void recordError() { errors.incrementAndGet(); }
        
        public CacheStatistics getStatistics() {
            long totalL1 = l1Hits.get() + l1Misses.get();
            long totalL2 = l2Hits.get() + l2Misses.get();
            
            double l1HitRate = totalL1 > 0 ? (double) l1Hits.get() / totalL1 : 0.0;
            double l2HitRate = totalL2 > 0 ? (double) l2Hits.get() / totalL2 : 0.0;
            double overallHitRate = totalL1 > 0 ? 
                (double) (l1Hits.get() + l2Hits.get()) / (totalL1 + l2Misses.get()) : 0.0;
            
            return new CacheStatistics(
                l1HitRate, l2HitRate, overallHitRate,
                writes.get(), evictions.get(), errors.get()
            );
        }
    }
    
    public record CacheStatistics(
        double l1HitRate,
        double l2HitRate, 
        double overallHitRate,
        long writeCount,
        long evictionCount,
        long errorCount
    ) {}
    
    private void evictLocal(String key) {
        l1Cache.invalidate(key);
    }
    
    private List<String> getPopularKeys() {
        // Return list of frequently accessed keys
        return List.of(); // Placeholder
    }
    
    private Object loadDataFromDatabase(String key) {
        // Load data from database for cache warming
        return null; // Placeholder
    }
}
```

## 5. Database Monitoring and Health Checks

### Comprehensive Database Monitoring
```java
package com.javajourney.database.monitoring;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Comprehensive database monitoring and health check system
 */
@Component
public class DatabaseMonitoringService implements HealthIndicator {
    
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final MeterRegistry meterRegistry;
    private final DatabaseMetricsCollector metricsCollector;
    
    public DatabaseMonitoringService(DataSource dataSource, 
                                   JdbcTemplate jdbcTemplate,
                                   MeterRegistry meterRegistry) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        this.meterRegistry = meterRegistry;
        this.metricsCollector = new DatabaseMetricsCollector(jdbcTemplate);
        
        // Start periodic monitoring
        startPeriodicMonitoring();
    }
    
    @Override
    public Health health() {
        try {
            DatabaseHealthStatus status = performHealthCheck();
            
            if (status.isHealthy()) {
                return Health.up()
                    .withDetail("connectionPool", status.getConnectionPoolStatus())
                    .withDetail("queryPerformance", status.getQueryPerformanceStatus())
                    .withDetail("diskSpace", status.getDiskSpaceStatus())
                    .withDetail("replicationLag", status.getReplicationLagMs())
                    .build();
            } else {
                return Health.down()
                    .withDetail("issues", status.getIssues())
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
    
    /**
     * Perform comprehensive database health check
     */
    private DatabaseHealthStatus performHealthCheck() {
        List<String> issues = new ArrayList<>();
        
        // Check connection pool health
        ConnectionPoolHealth poolHealth = checkConnectionPoolHealth();
        if (poolHealth.getStatus() != PoolHealthStatus.HEALTHY) {
            issues.add("Connection pool issues: " + poolHealth.getStatus());
        }
        
        // Check query performance
        QueryPerformanceHealth queryHealth = checkQueryPerformance();
        if (queryHealth.getAverageResponseTime() > 1000) { // 1 second threshold
            issues.add("Slow query performance: " + queryHealth.getAverageResponseTime() + "ms");
        }
        
        // Check disk space
        DiskSpaceHealth diskHealth = checkDiskSpace();
        if (diskHealth.getUsagePercentage() > 85) {
            issues.add("High disk usage: " + diskHealth.getUsagePercentage() + "%");
        }
        
        // Check replication lag (if applicable)
        long replicationLag = checkReplicationLag();
        if (replicationLag > 30000) { // 30 seconds threshold
            issues.add("High replication lag: " + replicationLag + "ms");
        }
        
        return new DatabaseHealthStatus(
            issues.isEmpty(),
            poolHealth,
            queryHealth,
            diskHealth,
            replicationLag,
            issues
        );
    }
    
    private ConnectionPoolHealth checkConnectionPoolHealth() {
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
            
            int active = poolMXBean.getActiveConnections();
            int total = poolMXBean.getTotalConnections();
            int waiting = poolMXBean.getThreadsAwaitingConnection();
            
            double utilization = (double) active / total;
            PoolHealthStatus status;
            
            if (waiting > 5 || utilization > 0.9) {
                status = PoolHealthStatus.CRITICAL;
            } else if (waiting > 2 || utilization > 0.7) {
                status = PoolHealthStatus.WARNING;
            } else {
                status = PoolHealthStatus.HEALTHY;
            }
            
            return new ConnectionPoolHealth(status, active, total, waiting, utilization);
        }
        
        return new ConnectionPoolHealth(PoolHealthStatus.UNKNOWN, 0, 0, 0, 0.0);
    }
    
    private QueryPerformanceHealth checkQueryPerformance() {
        // Sample query to test performance
        String testQuery = "SELECT 1";
        
        long startTime = System.currentTimeMillis();
        jdbcTemplate.queryForObject(testQuery, Integer.class);
        long responseTime = System.currentTimeMillis() - startTime;
        
        // Get average from recent executions
        double averageResponseTime = metricsCollector.getAverageQueryTime();
        
        return new QueryPerformanceHealth(responseTime, averageResponseTime);
    }
    
    private DiskSpaceHealth checkDiskSpace() {
        try {
            String query = """
                SELECT 
                    pg_size_pretty(pg_database_size(current_database())) as size,
                    pg_database_size(current_database()) as size_bytes
                """;
            
            Map<String, Object> result = jdbcTemplate.queryForMap(query);
            long sizeBytes = (Long) result.get("size_bytes");
            
            // Get available disk space (simplified)
            File dbPath = new File("/var/lib/postgresql/data");
            long totalSpace = dbPath.getTotalSpace();
            long freeSpace = dbPath.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;
            
            double usagePercentage = (double) usedSpace / totalSpace * 100;
            
            return new DiskSpaceHealth(usagePercentage, sizeBytes, freeSpace);
        } catch (Exception e) {
            return new DiskSpaceHealth(0.0, 0L, 0L);
        }
    }
    
    private long checkReplicationLag() {
        try {
            // Check replication lag for PostgreSQL
            String query = """
                SELECT EXTRACT(EPOCH FROM (now() - pg_last_xact_replay_timestamp())) * 1000 
                as lag_ms
                """;
            
            Double lag = jdbcTemplate.queryForObject(query, Double.class);
            return lag != null ? lag.longValue() : 0L;
        } catch (Exception e) {
            // Not a replica or query not supported
            return 0L;
        }
    }
    
    /**
     * Start periodic monitoring and metrics collection
     */
    private void startPeriodicMonitoring() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        
        // Collect metrics every 30 seconds
        scheduler.scheduleAtFixedRate(() -> {
            try {
                collectAndPublishMetrics();
            } catch (Exception e) {
                System.err.println("Failed to collect metrics: " + e.getMessage());
            }
        }, 0, 30, TimeUnit.SECONDS);
        
        // Check for slow queries every 5 minutes
        scheduler.scheduleAtFixedRate(() -> {
            try {
                analyzeSlowQueries();
            } catch (Exception e) {
                System.err.println("Failed to analyze slow queries: " + e.getMessage());
            }
        }, 0, 5, TimeUnit.MINUTES);
    }
    
    private void collectAndPublishMetrics() {
        // Connection pool metrics
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
            
            meterRegistry.gauge("db.connections.active", poolMXBean.getActiveConnections());
            meterRegistry.gauge("db.connections.idle", poolMXBean.getIdleConnections());
            meterRegistry.gauge("db.connections.total", poolMXBean.getTotalConnections());
            meterRegistry.gauge("db.connections.waiting", poolMXBean.getThreadsAwaitingConnection());
        }
        
        // Database size metrics
        try {
            Long dbSize = jdbcTemplate.queryForObject(
                "SELECT pg_database_size(current_database())", Long.class);
            meterRegistry.gauge("db.size.bytes", dbSize != null ? dbSize : 0);
        } catch (Exception e) {
            // Ignore if query fails
        }
        
        // Query performance metrics
        meterRegistry.gauge("db.query.average_time", metricsCollector.getAverageQueryTime());
        meterRegistry.gauge("db.query.count", metricsCollector.getQueryCount());
    }
    
    private void analyzeSlowQueries() {
        try {
            // Get slow queries from PostgreSQL's pg_stat_statements
            String query = """
                SELECT query, calls, total_time, mean_time, rows
                FROM pg_stat_statements 
                WHERE mean_time > 1000 
                ORDER BY mean_time DESC 
                LIMIT 10
                """;
            
            List<SlowQueryInfo> slowQueries = jdbcTemplate.query(query, (rs, rowNum) ->
                new SlowQueryInfo(
                    rs.getString("query"),
                    rs.getLong("calls"),
                    rs.getDouble("total_time"),
                    rs.getDouble("mean_time"),
                    rs.getLong("rows")
                )
            );
            
            if (!slowQueries.isEmpty()) {
                System.out.println("=== Slow Queries Detected ===");
                slowQueries.forEach(q -> 
                    System.out.printf("Query: %s, Mean Time: %.2f ms, Calls: %d%n",
                        q.query().substring(0, Math.min(50, q.query().length())),
                        q.meanTime(), q.calls())
                );
            }
        } catch (Exception e) {
            // pg_stat_statements extension might not be available
            System.out.println("Slow query analysis not available: " + e.getMessage());
        }
    }
    
    // Data classes
    public record DatabaseHealthStatus(
        boolean isHealthy,
        ConnectionPoolHealth connectionPoolStatus,
        QueryPerformanceHealth queryPerformanceStatus,
        DiskSpaceHealth diskSpaceStatus,
        long replicationLagMs,
        List<String> issues
    ) {}
    
    public record ConnectionPoolHealth(
        PoolHealthStatus status,
        int activeConnections,
        int totalConnections,
        int waitingThreads,
        double utilization
    ) {}
    
    public record QueryPerformanceHealth(
        long lastQueryTime,
        double averageResponseTime
    ) {}
    
    public record DiskSpaceHealth(
        double usagePercentage,
        long databaseSizeBytes,
        long freeSpaceBytes
    ) {}
    
    public record SlowQueryInfo(
        String query,
        long calls,
        double totalTime,
        double meanTime,
        long rows
    ) {}
    
    /**
     * Database metrics collector
     */
    public static class DatabaseMetricsCollector {
        private final JdbcTemplate jdbcTemplate;
        private final AtomicLong queryCount = new AtomicLong(0);
        private final AtomicLong totalQueryTime = new AtomicLong(0);
        
        public DatabaseMetricsCollector(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }
        
        public void recordQueryExecution(long executionTimeMs) {
            queryCount.incrementAndGet();
            totalQueryTime.addAndGet(executionTimeMs);
        }
        
        public double getAverageQueryTime() {
            long count = queryCount.get();
            return count > 0 ? (double) totalQueryTime.get() / count : 0.0;
        }
        
        public long getQueryCount() {
            return queryCount.get();
        }
    }
}
```

## Practice Exercises

### Exercise 1: Connection Pool Optimization
Configure and optimize HikariCP for a high-throughput application with specific performance requirements.

### Exercise 2: Transaction Saga Implementation
Implement a complete saga pattern for a multi-service transaction involving inventory, payment, and order services.

### Exercise 3: Query Performance Analysis
Build a query performance analyzer that identifies slow queries and suggests optimizations.

### Exercise 4: Multi-Level Cache Implementation
Create a complete multi-level caching system with Redis and application-level caching.

## Key Takeaways

1. **Connection Pooling**: Proper pool configuration is critical for application performance and resource utilization
2. **Transaction Management**: Choose appropriate transaction patterns based on consistency and performance requirements
3. **Query Optimization**: Regular query analysis and index optimization significantly improve performance
4. **Caching Strategy**: Multi-level caching provides optimal balance between performance and consistency
5. **Monitoring**: Comprehensive monitoring enables proactive performance management and issue detection
6. **Scalability**: Design database access patterns that scale with application growth

## Next Steps
Tomorrow we'll explore Reactive Programming with Project Reactor and WebFlux, building on the database optimization techniques we've learned to create highly scalable, non-blocking applications.

<function_calls>
<invoke name="TodoWrite">
<parameter name="todos">[{"content": "Create Day 85: Java Virtual Machine (JVM) Deep Dive - Memory Management & Garbage Collection", "status": "completed", "priority": "high", "id": "day85-jvm-deep-dive"}, {"content": "Create Day 86: Advanced JVM Tuning - Performance Optimization & Profiling", "status": "completed", "priority": "high", "id": "day86-jvm-tuning"}, {"content": "Create Day 87: Java Security - Authentication, Authorization & Cryptography", "status": "completed", "priority": "high", "id": "day87-java-security"}, {"content": "Create Day 88: Advanced Database Patterns - Connection Pooling, Transactions & Optimization", "status": "completed", "priority": "high", "id": "day88-database-patterns"}, {"content": "Create Day 89: Reactive Programming - Project Reactor & WebFlux Advanced", "status": "in_progress", "priority": "high", "id": "day89-reactive-programming"}, {"content": "Create Day 90: Enterprise Integration - ESB, Message Transformation & Routing", "status": "pending", "priority": "high", "id": "day90-enterprise-integration"}, {"content": "Create Day 91: Week 13 Capstone - High-Performance Enterprise Financial System", "status": "pending", "priority": "high", "id": "day91-week13-capstone"}]