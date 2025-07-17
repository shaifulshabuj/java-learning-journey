# Day 43: Performance Monitoring & Profiling (August 21, 2025)

**Date:** August 21, 2025  
**Duration:** 2 hours (Morning + Evening Session)  
**Focus:** Master JVM performance monitoring, profiling tools, and performance metrics analysis

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:

- ✅ Understand JVM performance characteristics and monitoring principles
- ✅ Master built-in Java monitoring tools (JConsole, VisualVM, JFR)
- ✅ Implement custom performance monitoring solutions
- ✅ Analyze memory usage patterns and garbage collection behavior
- ✅ Build performance dashboards with real-time metrics
- ✅ Identify performance bottlenecks using profiling techniques

---

## 📚 Part 1: JVM Performance Fundamentals (30 minutes)

### **Understanding JVM Performance Metrics**

**Key Performance Indicators:**
- **Throughput**: Operations completed per unit time
- **Latency**: Time to complete a single operation
- **Memory Utilization**: Heap and non-heap memory usage
- **GC Overhead**: Time spent in garbage collection
- **CPU Utilization**: Processor usage patterns
- **Thread Utilization**: Concurrent execution efficiency

**JVM Memory Structure:**
```
┌─────────────────────────────────────────┐
│                 Heap Memory             │
│  ┌─────────────┐    ┌─────────────────┐ │
│  │ Young Gen   │    │   Old Gen       │ │
│  │ Eden  S0 S1 │    │ (Tenured Space) │ │
│  └─────────────┘    └─────────────────┘ │
└─────────────────────────────────────────┘
┌─────────────────────────────────────────┐
│              Non-Heap Memory            │
│ Metaspace | Code Cache | Direct Memory  │
└─────────────────────────────────────────┘
```

### **Performance Monitoring Strategy**

**Golden Signals for Java Applications:**
1. **Latency**: Request response times, 95th percentile
2. **Traffic**: Requests per second, concurrent users
3. **Errors**: Error rates, exception counts
4. **Saturation**: Memory usage, CPU usage, GC pressure

---

## 💻 Part 2: Built-in Monitoring Tools (45 minutes)

### **Exercise 1: Performance Monitoring Infrastructure**

Create `PerformanceMonitor.java`:

```java
import java.lang.management.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import javax.management.*;

/**
 * Comprehensive performance monitoring utility for Java applications
 * Provides real-time metrics collection and analysis
 */
public class PerformanceMonitor {
    
    private final MemoryMXBean memoryMXBean;
    private final OperatingSystemMXBean osMXBean;
    private final RuntimeMXBean runtimeMXBean;
    private final ThreadMXBean threadMXBean;
    private final GarbageCollectorMXBean[] gcMXBeans;
    
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean monitoring;
    private final PerformanceMetrics metrics;
    
    // Performance tracking
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    
    public PerformanceMonitor() {
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.osMXBean = ManagementFactory.getOperatingSystemMXBean();
        this.runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        this.gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans()
                                         .toArray(new GarbageCollectorMXBean[0]);
        
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.monitoring = new AtomicBoolean(false);
        this.metrics = new PerformanceMetrics();
    }
    
    /**
     * Start performance monitoring with specified interval
     */
    public void startMonitoring(Duration interval) {
        if (monitoring.compareAndSet(false, true)) {
            System.out.println("🚀 Starting Performance Monitor...");
            
            scheduler.scheduleAtFixedRate(
                this::collectMetrics,
                0,
                interval.toMillis(),
                TimeUnit.MILLISECONDS
            );
            
            scheduler.scheduleAtFixedRate(
                this::printPerformanceReport,
                interval.toMillis(),
                interval.toMillis() * 3, // Report every 3 collection intervals
                TimeUnit.MILLISECONDS
            );
        }
    }
    
    /**
     * Stop performance monitoring
     */
    public void stopMonitoring() {
        if (monitoring.compareAndSet(true, false)) {
            System.out.println("🛑 Stopping Performance Monitor...");
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
    }
    
    /**
     * Collect comprehensive performance metrics
     */
    private void collectMetrics() {
        try {
            // Memory metrics
            MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
            MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
            
            metrics.updateMemoryMetrics(
                heapUsage.getUsed(),
                heapUsage.getMax(),
                nonHeapUsage.getUsed(),
                nonHeapUsage.getMax()
            );
            
            // CPU metrics
            if (osMXBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsMXBean = 
                    (com.sun.management.OperatingSystemMXBean) osMXBean;
                
                double processCpuLoad = sunOsMXBean.getProcessCpuLoad();
                double systemCpuLoad = sunOsMXBean.getSystemCpuLoad();
                
                metrics.updateCpuMetrics(processCpuLoad, systemCpuLoad);
            }
            
            // Thread metrics
            int threadCount = threadMXBean.getThreadCount();
            int peakThreadCount = threadMXBean.getPeakThreadCount();
            int daemonThreadCount = threadMXBean.getDaemonThreadCount();
            
            metrics.updateThreadMetrics(threadCount, peakThreadCount, daemonThreadCount);
            
            // GC metrics
            for (GarbageCollectorMXBean gcBean : gcMXBeans) {
                long collectionCount = gcBean.getCollectionCount();
                long collectionTime = gcBean.getCollectionTime();
                
                metrics.updateGcMetrics(gcBean.getName(), collectionCount, collectionTime);
            }
            
            // Runtime metrics
            long uptime = runtimeMXBean.getUptime();
            metrics.updateRuntimeMetrics(uptime);
            
        } catch (Exception e) {
            System.err.println("❌ Error collecting metrics: " + e.getMessage());
        }
    }
    
    /**
     * Print comprehensive performance report
     */
    public void printPerformanceReport() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 PERFORMANCE REPORT - " + 
                          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                         .format(LocalDateTime.now()));
        System.out.println("=".repeat(80));
        
        // Memory Report
        System.out.println("\n🧠 MEMORY METRICS:");
        System.out.printf("   Heap Used:     %,d MB / %,d MB (%.1f%%)%n",
                         metrics.heapUsed / (1024 * 1024),
                         metrics.heapMax / (1024 * 1024),
                         (double) metrics.heapUsed / metrics.heapMax * 100);
        
        System.out.printf("   Non-Heap Used: %,d MB / %,d MB (%.1f%%)%n",
                         metrics.nonHeapUsed / (1024 * 1024),
                         metrics.nonHeapMax / (1024 * 1024),
                         (double) metrics.nonHeapUsed / metrics.nonHeapMax * 100);
        
        // CPU Report
        System.out.println("\n⚡ CPU METRICS:");
        System.out.printf("   Process CPU:   %.1f%%%n", metrics.processCpuLoad * 100);
        System.out.printf("   System CPU:    %.1f%%%n", metrics.systemCpuLoad * 100);
        
        // Thread Report
        System.out.println("\n🧵 THREAD METRICS:");
        System.out.printf("   Active Threads:  %d threads%n", metrics.threadCount);
        System.out.printf("   Peak Threads:    %d threads%n", metrics.peakThreadCount);
        System.out.printf("   Daemon Threads:  %d threads%n", metrics.daemonThreadCount);
        
        // GC Report
        System.out.println("\n🗑️  GARBAGE COLLECTION:");
        metrics.gcMetrics.forEach((name, gcData) -> {
            System.out.printf("   %s: %,d collections, %,d ms total%n",
                             name, gcData.collectionCount, gcData.collectionTime);
        });
        
        // Application Metrics
        System.out.println("\n📈 APPLICATION METRICS:");
        long totalReqs = totalRequests.get();
        if (totalReqs > 0) {
            double avgResponseTime = (double) totalResponseTime.get() / totalReqs;
            double errorRate = (double) totalErrors.get() / totalReqs * 100;
            
            System.out.printf("   Total Requests:    %,d%n", totalReqs);
            System.out.printf("   Average Response:  %.2f ms%n", avgResponseTime);
            System.out.printf("   Error Rate:        %.2f%%%n", errorRate);
        } else {
            System.out.println("   No application metrics available");
        }
        
        // Runtime Report
        System.out.println("\n⏱️  RUNTIME:");
        long uptimeHours = metrics.uptime / (1000 * 60 * 60);
        long uptimeMinutes = (metrics.uptime % (1000 * 60 * 60)) / (1000 * 60);
        System.out.printf("   Uptime:           %d hours, %d minutes%n", uptimeHours, uptimeMinutes);
        
        System.out.println("=".repeat(80));
    }
    
    /**
     * Record an application request for metrics
     */
    public void recordRequest(long responseTimeMs, boolean isError) {
        totalRequests.incrementAndGet();
        totalResponseTime.addAndGet(responseTimeMs);
        if (isError) {
            totalErrors.incrementAndGet();
        }
    }
    
    /**
     * Get current performance snapshot
     */
    public PerformanceSnapshot getSnapshot() {
        return new PerformanceSnapshot(
            metrics.heapUsed,
            metrics.heapMax,
            metrics.processCpuLoad,
            metrics.threadCount,
            totalRequests.get(),
            totalErrors.get(),
            Instant.now()
        );
    }
    
    /**
     * Internal metrics storage
     */
    private static class PerformanceMetrics {
        volatile long heapUsed;
        volatile long heapMax;
        volatile long nonHeapUsed;
        volatile long nonHeapMax;
        volatile double processCpuLoad;
        volatile double systemCpuLoad;
        volatile int threadCount;
        volatile int peakThreadCount;
        volatile int daemonThreadCount;
        volatile long uptime;
        
        final ConcurrentHashMap<String, GcMetrics> gcMetrics = new ConcurrentHashMap<>();
        
        void updateMemoryMetrics(long heapUsed, long heapMax, long nonHeapUsed, long nonHeapMax) {
            this.heapUsed = heapUsed;
            this.heapMax = heapMax;
            this.nonHeapUsed = nonHeapUsed;
            this.nonHeapMax = nonHeapMax;
        }
        
        void updateCpuMetrics(double processCpuLoad, double systemCpuLoad) {
            this.processCpuLoad = processCpuLoad;
            this.systemCpuLoad = systemCpuLoad;
        }
        
        void updateThreadMetrics(int threadCount, int peakThreadCount, int daemonThreadCount) {
            this.threadCount = threadCount;
            this.peakThreadCount = peakThreadCount;
            this.daemonThreadCount = daemonThreadCount;
        }
        
        void updateGcMetrics(String name, long collectionCount, long collectionTime) {
            gcMetrics.put(name, new GcMetrics(collectionCount, collectionTime));
        }
        
        void updateRuntimeMetrics(long uptime) {
            this.uptime = uptime;
        }
    }
    
    /**
     * GC metrics data
     */
    private static class GcMetrics {
        final long collectionCount;
        final long collectionTime;
        
        GcMetrics(long collectionCount, long collectionTime) {
            this.collectionCount = collectionCount;
            this.collectionTime = collectionTime;
        }
    }
    
    /**
     * Performance snapshot for point-in-time analysis
     */
    public static class PerformanceSnapshot {
        public final long heapUsed;
        public final long heapMax;
        public final double cpuUsage;
        public final int threadCount;
        public final long totalRequests;
        public final long totalErrors;
        public final Instant timestamp;
        
        public PerformanceSnapshot(long heapUsed, long heapMax, double cpuUsage, 
                                 int threadCount, long totalRequests, long totalErrors,
                                 Instant timestamp) {
            this.heapUsed = heapUsed;
            this.heapMax = heapMax;
            this.cpuUsage = cpuUsage;
            this.threadCount = threadCount;
            this.totalRequests = totalRequests;
            this.totalErrors = totalErrors;
            this.timestamp = timestamp;
        }
        
        public double getMemoryUtilization() {
            return (double) heapUsed / heapMax;
        }
        
        public double getErrorRate() {
            return totalRequests > 0 ? (double) totalErrors / totalRequests : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "PerformanceSnapshot{memory=%.1f%%, cpu=%.1f%%, threads=%d, requests=%d, errors=%d}",
                getMemoryUtilization() * 100, cpuUsage * 100, threadCount, totalRequests, totalErrors
            );
        }
    }
    
    /**
     * Demo application for performance monitoring
     */
    public static void main(String[] args) throws InterruptedException {
        PerformanceMonitor monitor = new PerformanceMonitor();
        
        // Start monitoring
        monitor.startMonitoring(Duration.ofSeconds(2));
        
        // Simulate application workload
        System.out.println("🏃 Simulating application workload...");
        simulateWorkload(monitor);
        
        // Keep monitoring for a while
        Thread.sleep(15000);
        
        // Stop monitoring
        monitor.stopMonitoring();
        
        // Final report
        System.out.println("\n📋 FINAL PERFORMANCE SUMMARY:");
        System.out.println(monitor.getSnapshot());
    }
    
    /**
     * Simulate realistic application workload
     */
    private static void simulateWorkload(PerformanceMonitor monitor) {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        
        for (int i = 0; i < 100; i++) {
            final int requestId = i;
            executor.submit(() -> {
                long startTime = System.currentTimeMillis();
                
                try {
                    // Simulate some work
                    performWork(requestId);
                    
                    long responseTime = System.currentTimeMillis() - startTime;
                    boolean isError = Math.random() < 0.05; // 5% error rate
                    
                    monitor.recordRequest(responseTime, isError);
                    
                } catch (Exception e) {
                    long responseTime = System.currentTimeMillis() - startTime;
                    monitor.recordRequest(responseTime, true);
                }
            });
            
            // Throttle request rate
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        executor.shutdown();
    }
    
    /**
     * Simulate work that uses CPU and memory
     */
    private static void performWork(int requestId) throws InterruptedException {
        // CPU-intensive work
        double result = 0;
        for (int i = 0; i < 10000; i++) {
            result += Math.sin(i) * Math.cos(i);
        }
        
        // Memory allocation
        byte[] data = new byte[1024 * (1 + requestId % 100)];
        Arrays.fill(data, (byte) (requestId % 256));
        
        // I/O simulation
        Thread.sleep(10 + requestId % 50);
        
        // Prevent optimization
        if (result > Double.MAX_VALUE) {
            System.out.println("Impossible result: " + result);
        }
    }
}
```

### **Exercise 2: Memory Profiling Utility**

Create `MemoryProfiler.java`:

```java
import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;
import java.time.*;

/**
 * Advanced memory profiling and leak detection utility
 */
public class MemoryProfiler {
    
    private final MemoryMXBean memoryMXBean;
    private final List<MemoryPoolMXBean> memoryPoolMXBeans;
    private final List<GarbageCollectorMXBean> gcMXBeans;
    
    private final Map<String, MemorySnapshot> snapshots;
    private final ScheduledExecutorService scheduler;
    
    public MemoryProfiler() {
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        this.gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
        this.snapshots = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }
    
    /**
     * Take a memory snapshot
     */
    public MemorySnapshot takeSnapshot(String label) {
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
        
        Map<String, MemoryUsage> poolUsages = new HashMap<>();
        for (MemoryPoolMXBean pool : memoryPoolMXBeans) {
            poolUsages.put(pool.getName(), pool.getUsage());
        }
        
        Map<String, GcStats> gcStats = new HashMap<>();
        for (GarbageCollectorMXBean gc : gcMXBeans) {
            gcStats.put(gc.getName(), new GcStats(gc.getCollectionCount(), gc.getCollectionTime()));
        }
        
        MemorySnapshot snapshot = new MemorySnapshot(
            label,
            Instant.now(),
            heapUsage,
            nonHeapUsage,
            poolUsages,
            gcStats
        );
        
        snapshots.put(label, snapshot);
        
        System.out.printf("📸 Memory snapshot '%s' taken at %s%n", 
                         label, 
                         LocalDateTime.ofInstant(snapshot.timestamp, ZoneId.systemDefault()));
        
        return snapshot;
    }
    
    /**
     * Compare two memory snapshots
     */
    public void compareSnapshots(String snapshot1Label, String snapshot2Label) {
        MemorySnapshot s1 = snapshots.get(snapshot1Label);
        MemorySnapshot s2 = snapshots.get(snapshot2Label);
        
        if (s1 == null || s2 == null) {
            System.err.println("❌ One or both snapshots not found");
            return;
        }
        
        System.out.println("\n" + "=".repeat(80));
        System.out.printf("📊 MEMORY COMPARISON: '%s' vs '%s'%n", snapshot1Label, snapshot2Label);
        System.out.println("=".repeat(80));
        
        // Heap comparison
        long heapDiff = s2.heapUsage.getUsed() - s1.heapUsage.getUsed();
        System.out.printf("🧠 Heap Memory Change: %s%n", formatMemoryDiff(heapDiff));
        
        // Non-heap comparison
        long nonHeapDiff = s2.nonHeapUsage.getUsed() - s1.nonHeapUsage.getUsed();
        System.out.printf("💾 Non-Heap Memory Change: %s%n", formatMemoryDiff(nonHeapDiff));
        
        // Pool-by-pool comparison
        System.out.println("\n📊 Memory Pool Changes:");
        for (String poolName : s1.poolUsages.keySet()) {
            if (s2.poolUsages.containsKey(poolName)) {
                long poolDiff = s2.poolUsages.get(poolName).getUsed() - 
                               s1.poolUsages.get(poolName).getUsed();
                System.out.printf("   %-25s: %s%n", poolName, formatMemoryDiff(poolDiff));
            }
        }
        
        // GC comparison
        System.out.println("\n🗑️  Garbage Collection Changes:");
        for (String gcName : s1.gcStats.keySet()) {
            if (s2.gcStats.containsKey(gcName)) {
                GcStats gc1 = s1.gcStats.get(gcName);
                GcStats gc2 = s2.gcStats.get(gcName);
                
                long countDiff = gc2.collectionCount - gc1.collectionCount;
                long timeDiff = gc2.collectionTime - gc1.collectionTime;
                
                System.out.printf("   %-25s: %+d collections, %+d ms%n", 
                                 gcName, countDiff, timeDiff);
            }
        }
        
        // Duration
        Duration duration = Duration.between(s1.timestamp, s2.timestamp);
        System.out.printf("\n⏱️  Time Duration: %d seconds%n", duration.toSeconds());
    }
    
    /**
     * Detect potential memory leaks
     */
    public void detectMemoryLeaks() {
        if (snapshots.size() < 2) {
            System.out.println("⚠️  Need at least 2 snapshots to detect leaks");
            return;
        }
        
        List<MemorySnapshot> sortedSnapshots = snapshots.values()
                                                       .stream()
                                                       .sorted(Comparator.comparing(s -> s.timestamp))
                                                       .toList();
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🔍 MEMORY LEAK ANALYSIS");
        System.out.println("=".repeat(80));
        
        // Analyze heap growth trend
        analyzeMemoryTrend(sortedSnapshots);
        
        // Analyze specific memory pools
        analyzePoolTrends(sortedSnapshots);
        
        // GC efficiency analysis
        analyzeGcEfficiency(sortedSnapshots);
    }
    
    private void analyzeMemoryTrend(List<MemorySnapshot> snapshots) {
        System.out.println("\n📈 HEAP MEMORY TREND:");
        
        long totalGrowth = 0;
        int positiveGrowthCount = 0;
        
        for (int i = 1; i < snapshots.size(); i++) {
            long currentUsed = snapshots.get(i).heapUsage.getUsed();
            long previousUsed = snapshots.get(i - 1).heapUsage.getUsed();
            long growth = currentUsed - previousUsed;
            
            totalGrowth += growth;
            if (growth > 0) {
                positiveGrowthCount++;
            }
            
            System.out.printf("   %s -> %s: %s%n",
                             snapshots.get(i - 1).label,
                             snapshots.get(i).label,
                             formatMemoryDiff(growth));
        }
        
        double averageGrowth = (double) totalGrowth / (snapshots.size() - 1);
        double positiveGrowthRatio = (double) positiveGrowthCount / (snapshots.size() - 1);
        
        System.out.printf("\n📊 ANALYSIS SUMMARY:%n");
        System.out.printf("   Average Growth: %s per snapshot%n", formatMemoryDiff((long) averageGrowth));
        System.out.printf("   Positive Growth Ratio: %.1f%%%n", positiveGrowthRatio * 100);
        
        // Leak warning
        if (averageGrowth > 1_000_000 && positiveGrowthRatio > 0.7) { // 1MB average growth, 70% positive
            System.out.println("   ⚠️  WARNING: Potential memory leak detected!");
            System.out.println("   📋  Recommendations:");
            System.out.println("      - Review object lifecycle management");
            System.out.println("      - Check for static collections holding references");
            System.out.println("      - Verify proper resource cleanup (close() methods)");
            System.out.println("      - Use heap dump analysis tools");
        } else {
            System.out.println("   ✅  Memory usage appears stable");
        }
    }
    
    private void analyzePoolTrends(List<MemorySnapshot> snapshots) {
        System.out.println("\n🏊 MEMORY POOL ANALYSIS:");
        
        Set<String> allPools = snapshots.get(0).poolUsages.keySet();
        
        for (String poolName : allPools) {
            long totalGrowth = 0;
            int validComparisons = 0;
            
            for (int i = 1; i < snapshots.size(); i++) {
                MemoryUsage current = snapshots.get(i).poolUsages.get(poolName);
                MemoryUsage previous = snapshots.get(i - 1).poolUsages.get(poolName);
                
                if (current != null && previous != null) {
                    totalGrowth += current.getUsed() - previous.getUsed();
                    validComparisons++;
                }
            }
            
            if (validComparisons > 0) {
                double averageGrowth = (double) totalGrowth / validComparisons;
                System.out.printf("   %-30s: %s average growth%n", 
                                 poolName, formatMemoryDiff((long) averageGrowth));
            }
        }
    }
    
    private void analyzeGcEfficiency(List<MemorySnapshot> snapshots) {
        System.out.println("\n🗑️  GARBAGE COLLECTION EFFICIENCY:");
        
        for (String gcName : snapshots.get(0).gcStats.keySet()) {
            long totalCollections = 0;
            long totalTime = 0;
            
            MemorySnapshot first = snapshots.get(0);
            MemorySnapshot last = snapshots.get(snapshots.size() - 1);
            
            GcStats firstGc = first.gcStats.get(gcName);
            GcStats lastGc = last.gcStats.get(gcName);
            
            if (firstGc != null && lastGc != null) {
                totalCollections = lastGc.collectionCount - firstGc.collectionCount;
                totalTime = lastGc.collectionTime - firstGc.collectionTime;
                
                if (totalCollections > 0) {
                    double avgTimePerCollection = (double) totalTime / totalCollections;
                    Duration totalDuration = Duration.between(first.timestamp, last.timestamp);
                    double gcOverheadPercentage = (double) totalTime / totalDuration.toMillis() * 100;
                    
                    System.out.printf("   %-25s: %d collections, %.1f ms avg, %.2f%% overhead%n",
                                     gcName, totalCollections, avgTimePerCollection, gcOverheadPercentage);
                    
                    if (gcOverheadPercentage > 10) {
                        System.out.printf("      ⚠️  High GC overhead for %s!%n", gcName);
                    }
                }
            }
        }
    }
    
    /**
     * Start continuous memory monitoring
     */
    public void startContinuousMonitoring(Duration interval, String baseLabel) {
        AtomicInteger counter = new AtomicInteger(0);
        
        scheduler.scheduleAtFixedRate(() -> {
            String label = baseLabel + "_" + counter.incrementAndGet();
            takeSnapshot(label);
        }, 0, interval.toMillis(), TimeUnit.MILLISECONDS);
        
        System.out.printf("🔄 Started continuous monitoring with %s interval%n", interval);
    }
    
    /**
     * Stop continuous monitoring
     */
    public void stopContinuousMonitoring() {
        scheduler.shutdown();
        System.out.println("🛑 Stopped continuous monitoring");
    }
    
    private String formatMemoryDiff(long bytes) {
        String sign = bytes >= 0 ? "+" : "";
        double mb = bytes / (1024.0 * 1024.0);
        return String.format("%s%.2f MB", sign, mb);
    }
    
    /**
     * Memory snapshot data structure
     */
    public static class MemorySnapshot {
        public final String label;
        public final Instant timestamp;
        public final MemoryUsage heapUsage;
        public final MemoryUsage nonHeapUsage;
        public final Map<String, MemoryUsage> poolUsages;
        public final Map<String, GcStats> gcStats;
        
        public MemorySnapshot(String label, Instant timestamp, MemoryUsage heapUsage,
                             MemoryUsage nonHeapUsage, Map<String, MemoryUsage> poolUsages,
                             Map<String, GcStats> gcStats) {
            this.label = label;
            this.timestamp = timestamp;
            this.heapUsage = heapUsage;
            this.nonHeapUsage = nonHeapUsage;
            this.poolUsages = Map.copyOf(poolUsages);
            this.gcStats = Map.copyOf(gcStats);
        }
    }
    
    /**
     * GC statistics
     */
    public static class GcStats {
        public final long collectionCount;
        public final long collectionTime;
        
        public GcStats(long collectionCount, long collectionTime) {
            this.collectionCount = collectionCount;
            this.collectionTime = collectionTime;
        }
    }
    
    /**
     * Demo application
     */
    public static void main(String[] args) throws InterruptedException {
        MemoryProfiler profiler = new MemoryProfiler();
        
        // Initial snapshot
        profiler.takeSnapshot("startup");
        
        // Simulate some memory usage
        System.out.println("🏃 Creating memory pressure...");
        List<byte[]> memoryHog = new ArrayList<>();
        
        for (int i = 0; i < 100; i++) {
            memoryHog.add(new byte[1024 * 1024]); // 1MB per iteration
            if (i % 25 == 24) {
                profiler.takeSnapshot("phase_" + (i / 25 + 1));
                Thread.sleep(1000);
            }
        }
        
        // Force GC and take final snapshot
        System.gc();
        Thread.sleep(2000);
        profiler.takeSnapshot("after_gc");
        
        // Analysis
        profiler.compareSnapshots("startup", "phase_1");
        profiler.compareSnapshots("phase_2", "phase_4");
        profiler.detectMemoryLeaks();
        
        // Cleanup
        memoryHog.clear();
        System.gc();
        profiler.takeSnapshot("cleanup");
        
        profiler.compareSnapshots("after_gc", "cleanup");
    }
}
```

---

## 🔧 Part 3: Custom Performance Dashboards (30 minutes)

### **Exercise 3: Real-time Performance Dashboard**

Create `PerformanceDashboard.java`:

```java
import java.util.concurrent.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * Real-time performance dashboard with live metrics visualization
 */
public class PerformanceDashboard {
    
    private final PerformanceMonitor monitor;
    private final ScheduledExecutorService dashboardScheduler;
    private final Queue<PerformanceMonitor.PerformanceSnapshot> snapshots;
    private final AtomicBoolean running;
    
    private static final int MAX_SNAPSHOTS = 60; // Keep last 60 data points
    
    public PerformanceDashboard() {
        this.monitor = new PerformanceMonitor();
        this.dashboardScheduler = Executors.newScheduledThreadPool(1);
        this.snapshots = new ConcurrentLinkedQueue<>();
        this.running = new AtomicBoolean(false);
    }
    
    /**
     * Start the live dashboard
     */
    public void startDashboard() {
        if (running.compareAndSet(false, true)) {
            // Start monitoring
            monitor.startMonitoring(Duration.ofSeconds(1));
            
            // Start dashboard updates
            dashboardScheduler.scheduleAtFixedRate(
                this::updateDashboard,
                1000,
                1000,
                TimeUnit.MILLISECONDS
            );
            
            System.out.println("🚀 Performance Dashboard Started");
            System.out.println("Press Ctrl+C to stop...\n");
        }
    }
    
    /**
     * Stop the dashboard
     */
    public void stopDashboard() {
        if (running.compareAndSet(true, false)) {
            monitor.stopMonitoring();
            dashboardScheduler.shutdown();
            System.out.println("\n🛑 Performance Dashboard Stopped");
        }
    }
    
    /**
     * Update dashboard display
     */
    private void updateDashboard() {
        try {
            // Capture current snapshot
            PerformanceMonitor.PerformanceSnapshot snapshot = monitor.getSnapshot();
            snapshots.offer(snapshot);
            
            // Keep only recent snapshots
            while (snapshots.size() > MAX_SNAPSHOTS) {
                snapshots.poll();
            }
            
            // Clear screen (ANSI escape codes)
            System.out.print("\033[2J\033[H");
            
            // Display dashboard
            displayHeader();
            displayCurrentMetrics(snapshot);
            displayTrends();
            displayCharts();
            
        } catch (Exception e) {
            System.err.println("Dashboard update error: " + e.getMessage());
        }
    }
    
    /**
     * Display dashboard header
     */
    private void displayHeader() {
        String timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        );
        
        System.out.println("┌" + "─".repeat(78) + "┐");
        System.out.printf("│ 📊 JAVA PERFORMANCE DASHBOARD - %s │%n", timestamp);
        System.out.println("└" + "─".repeat(78) + "┘");
    }
    
    /**
     * Display current metrics
     */
    private void displayCurrentMetrics(PerformanceMonitor.PerformanceSnapshot snapshot) {
        System.out.println("\n📈 CURRENT METRICS:");
        System.out.println("┌─────────────────┬─────────────────┬─────────────────┬─────────────────┐");
        System.out.println("│ Memory Usage    │ CPU Usage       │ Thread Count    │ Error Rate      │");
        System.out.println("├─────────────────┼─────────────────┼─────────────────┼─────────────────┤");
        
        String memoryBar = createProgressBar(snapshot.getMemoryUtilization(), 15);
        String cpuBar = createProgressBar(snapshot.cpuUsage, 15);
        String errorRateDisplay = String.format("%.2f%%", snapshot.getErrorRate() * 100);
        
        System.out.printf("│ %s │ %s │ %-15d │ %-15s │%n",
                         memoryBar,
                         cpuBar,
                         snapshot.threadCount,
                         errorRateDisplay);
        
        System.out.printf("│ %.1f%% (%d MB)   │ %.1f%%           │ Active          │ %d/%d           │%n",
                         snapshot.getMemoryUtilization() * 100,
                         snapshot.heapUsed / (1024 * 1024),
                         snapshot.cpuUsage * 100,
                         snapshot.totalErrors,
                         snapshot.totalRequests);
        
        System.out.println("└─────────────────┴─────────────────┴─────────────────┴─────────────────┘");
    }
    
    /**
     * Display performance trends
     */
    private void displayTrends() {
        if (snapshots.size() < 2) return;
        
        List<PerformanceMonitor.PerformanceSnapshot> recentSnapshots = 
            new ArrayList<>(snapshots);
        
        System.out.println("\n📊 TRENDS (Last 60 seconds):");
        
        // Memory trend
        displayMemoryTrend(recentSnapshots);
        
        // CPU trend  
        displayCpuTrend(recentSnapshots);
        
        // Request rate trend
        displayRequestTrend(recentSnapshots);
    }
    
    /**
     * Display memory usage trend
     */
    private void displayMemoryTrend(List<PerformanceMonitor.PerformanceSnapshot> snapshots) {
        System.out.print("Memory: ");
        
        for (int i = Math.max(0, snapshots.size() - 30); i < snapshots.size(); i++) {
            double memUsage = snapshots.get(i).getMemoryUtilization();
            System.out.print(getMemoryTrendChar(memUsage));
        }
        
        double currentMem = snapshots.get(snapshots.size() - 1).getMemoryUtilization();
        System.out.printf(" (%.1f%%)%n", currentMem * 100);
    }
    
    /**
     * Display CPU usage trend
     */
    private void displayCpuTrend(List<PerformanceMonitor.PerformanceSnapshot> snapshots) {
        System.out.print("CPU:    ");
        
        for (int i = Math.max(0, snapshots.size() - 30); i < snapshots.size(); i++) {
            double cpuUsage = snapshots.get(i).cpuUsage;
            System.out.print(getCpuTrendChar(cpuUsage));
        }
        
        double currentCpu = snapshots.get(snapshots.size() - 1).cpuUsage;
        System.out.printf(" (%.1f%%)%n", currentCpu * 100);
    }
    
    /**
     * Display request rate trend
     */
    private void displayRequestTrend(List<PerformanceMonitor.PerformanceSnapshot> snapshots) {
        System.out.print("Req/s:  ");
        
        for (int i = Math.max(1, snapshots.size() - 30); i < snapshots.size(); i++) {
            long currentReqs = snapshots.get(i).totalRequests;
            long previousReqs = snapshots.get(i - 1).totalRequests;
            long requestRate = currentReqs - previousReqs;
            
            System.out.print(getRequestTrendChar(requestRate));
        }
        
        if (snapshots.size() > 1) {
            long currentReqs = snapshots.get(snapshots.size() - 1).totalRequests;
            long previousReqs = snapshots.get(snapshots.size() - 2).totalRequests;
            long currentRate = currentReqs - previousReqs;
            System.out.printf(" (%d req/s)%n", currentRate);
        } else {
            System.out.println(" (0 req/s)");
        }
    }
    
    /**
     * Display ASCII charts
     */
    private void displayCharts() {
        if (snapshots.size() < 5) return;
        
        System.out.println("\n📉 PERFORMANCE CHARTS:");
        
        List<PerformanceMonitor.PerformanceSnapshot> recentSnapshots = 
            new ArrayList<>(snapshots);
        
        // Memory chart
        System.out.println("\nMemory Usage (%):");
        displayChart(recentSnapshots, s -> s.getMemoryUtilization() * 100, 0, 100);
        
        // CPU chart
        System.out.println("\nCPU Usage (%):");
        displayChart(recentSnapshots, s -> s.cpuUsage * 100, 0, 100);
    }
    
    /**
     * Display ASCII chart for a metric
     */
    private void displayChart(List<PerformanceMonitor.PerformanceSnapshot> snapshots,
                             Function<PerformanceMonitor.PerformanceSnapshot, Double> extractor,
                             double minValue, double maxValue) {
        
        int chartHeight = 10;
        int chartWidth = Math.min(60, snapshots.size());
        
        // Get data points
        List<Double> values = snapshots.stream()
                                     .skip(Math.max(0, snapshots.size() - chartWidth))
                                     .map(extractor)
                                     .toList();
        
        // Draw chart
        for (int row = chartHeight - 1; row >= 0; row--) {
            double threshold = minValue + (maxValue - minValue) * row / (chartHeight - 1);
            
            System.out.printf("%6.1f │", threshold);
            
            for (double value : values) {
                if (value >= threshold) {
                    System.out.print("█");
                } else if (value >= threshold - (maxValue - minValue) / chartHeight / 2) {
                    System.out.print("▄");
                } else {
                    System.out.print(" ");
                }
            }
            System.out.println();
        }
        
        System.out.print("       └");
        System.out.print("─".repeat(chartWidth));
        System.out.println(">");
    }
    
    /**
     * Create progress bar
     */
    private String createProgressBar(double percentage, int width) {
        int filled = (int) (percentage * width);
        StringBuilder bar = new StringBuilder();
        
        for (int i = 0; i < width; i++) {
            if (i < filled) {
                bar.append("█");
            } else {
                bar.append("░");
            }
        }
        
        return bar.toString();
    }
    
    /**
     * Get trend character for memory usage
     */
    private char getMemoryTrendChar(double usage) {
        if (usage < 0.5) return '▁';
        if (usage < 0.7) return '▃';
        if (usage < 0.9) return '▅';
        return '▇';
    }
    
    /**
     * Get trend character for CPU usage  
     */
    private char getCpuTrendChar(double usage) {
        if (usage < 0.3) return '▁';
        if (usage < 0.6) return '▃';
        if (usage < 0.8) return '▅';
        return '▇';
    }
    
    /**
     * Get trend character for request rate
     */
    private char getRequestTrendChar(long rate) {
        if (rate == 0) return '▁';
        if (rate < 5) return '▃';
        if (rate < 10) return '▅';
        return '▇';
    }
    
    /**
     * Demo application
     */
    public static void main(String[] args) throws InterruptedException {
        PerformanceDashboard dashboard = new PerformanceDashboard();
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(dashboard::stopDashboard));
        
        // Start dashboard
        dashboard.startDashboard();
        
        // Simulate workload
        simulateApplicationLoad(dashboard.monitor);
        
        // Keep running until interrupted
        Thread.sleep(60000); // Run for 1 minute
        
        dashboard.stopDashboard();
    }
    
    /**
     * Simulate varying application load
     */
    private static void simulateApplicationLoad(PerformanceMonitor monitor) {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        
        // Background load simulation
        executor.submit(() -> {
            Random random = new Random();
            
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Variable load simulation
                    int requests = 1 + random.nextInt(10);
                    
                    for (int i = 0; i < requests; i++) {
                        long startTime = System.currentTimeMillis();
                        
                        // Simulate work
                        performSimulatedWork(random);
                        
                        long responseTime = System.currentTimeMillis() - startTime;
                        boolean isError = random.nextDouble() < 0.02; // 2% error rate
                        
                        monitor.recordRequest(responseTime, isError);
                    }
                    
                    // Variable sleep time
                    Thread.sleep(500 + random.nextInt(1000));
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
    
    /**
     * Simulate work with variable CPU and memory usage
     */
    private static void performSimulatedWork(Random random) throws InterruptedException {
        // CPU work
        double result = 0;
        int iterations = 1000 + random.nextInt(5000);
        for (int i = 0; i < iterations; i++) {
            result += Math.sin(i) * Math.cos(i);
        }
        
        // Memory allocation
        if (random.nextDouble() < 0.3) { // 30% chance of memory allocation
            byte[] data = new byte[1024 * (10 + random.nextInt(100))];
            Arrays.fill(data, (byte) random.nextInt(256));
        }
        
        // Sleep simulation
        Thread.sleep(random.nextInt(20));
        
        // Prevent optimization
        if (result > Double.MAX_VALUE) {
            System.out.println("Impossible: " + result);
        }
    }
}
```

---

## 🎯 Today's Challenges

### **Challenge 1: Application Performance Monitor**

Create `ApplicationMonitor.java` that monitors a sample web service:

```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.*;
import java.time.*;

/**
 * Comprehensive application performance monitoring solution
 */
public class ApplicationMonitor {
    
    private final PerformanceMonitor systemMonitor;
    private final Map<String, EndpointMetrics> endpointMetrics;
    private final ScheduledExecutorService scheduler;
    
    public ApplicationMonitor() {
        this.systemMonitor = new PerformanceMonitor();
        this.endpointMetrics = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
    }
    
    /**
     * Record endpoint performance
     */
    public void recordEndpointCall(String endpoint, long responseTimeMs, int statusCode) {
        endpointMetrics.computeIfAbsent(endpoint, k -> new EndpointMetrics())
                      .recordCall(responseTimeMs, statusCode);
        
        // Record in system monitor
        systemMonitor.recordRequest(responseTimeMs, statusCode >= 400);
    }
    
    /**
     * Start comprehensive monitoring
     */
    public void startMonitoring() {
        systemMonitor.startMonitoring(Duration.ofSeconds(5));
        
        // Endpoint report every 30 seconds
        scheduler.scheduleAtFixedRate(
            this::generateEndpointReport,
            30, 30, TimeUnit.SECONDS
        );
    }
    
    /**
     * Generate endpoint performance report
     */
    private void generateEndpointReport() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🔗 ENDPOINT PERFORMANCE REPORT");
        System.out.println("=".repeat(80));
        
        endpointMetrics.forEach((endpoint, metrics) -> {
            EndpointStats stats = metrics.getStats();
            
            System.out.printf("\n📍 Endpoint: %s%n", endpoint);
            System.out.printf("   Total Requests: %,d%n", stats.totalRequests);
            System.out.printf("   Success Rate: %.2f%% (%d/%d)%n", 
                             stats.getSuccessRate() * 100, 
                             stats.successCount, 
                             stats.totalRequests);
            System.out.printf("   Avg Response Time: %.2f ms%n", stats.getAverageResponseTime());
            System.out.printf("   P95 Response Time: %.2f ms%n", stats.getP95ResponseTime());
            System.out.printf("   Max Response Time: %d ms%n", stats.maxResponseTime);
            
            if (stats.getSuccessRate() < 0.95) {
                System.out.println("   ⚠️  WARNING: Low success rate!");
            }
            if (stats.getAverageResponseTime() > 1000) {
                System.out.println("   ⚠️  WARNING: High response times!");
            }
        });
    }
    
    /**
     * Endpoint metrics collection
     */
    private static class EndpointMetrics {
        private final AtomicLong totalRequests = new AtomicLong(0);
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong totalResponseTime = new AtomicLong(0);
        private final AtomicLong maxResponseTime = new AtomicLong(0);
        private final Queue<Long> recentResponseTimes = new ConcurrentLinkedQueue<>();
        
        void recordCall(long responseTimeMs, int statusCode) {
            totalRequests.incrementAndGet();
            totalResponseTime.addAndGet(responseTimeMs);
            
            if (statusCode < 400) {
                successCount.incrementAndGet();
            }
            
            // Update max response time
            maxResponseTime.updateAndGet(max -> Math.max(max, responseTimeMs));
            
            // Keep recent response times for percentile calculation
            recentResponseTimes.offer(responseTimeMs);
            if (recentResponseTimes.size() > 1000) {
                recentResponseTimes.poll();
            }
        }
        
        EndpointStats getStats() {
            return new EndpointStats(
                totalRequests.get(),
                successCount.get(),
                totalResponseTime.get(),
                maxResponseTime.get(),
                new ArrayList<>(recentResponseTimes)
            );
        }
    }
    
    /**
     * Endpoint statistics
     */
    private static class EndpointStats {
        final long totalRequests;
        final long successCount;
        final long totalResponseTime;
        final long maxResponseTime;
        final List<Long> responseTimes;
        
        EndpointStats(long totalRequests, long successCount, long totalResponseTime,
                     long maxResponseTime, List<Long> responseTimes) {
            this.totalRequests = totalRequests;
            this.successCount = successCount;
            this.totalResponseTime = totalResponseTime;
            this.maxResponseTime = maxResponseTime;
            this.responseTimes = responseTimes;
        }
        
        double getSuccessRate() {
            return totalRequests > 0 ? (double) successCount / totalRequests : 0.0;
        }
        
        double getAverageResponseTime() {
            return totalRequests > 0 ? (double) totalResponseTime / totalRequests : 0.0;
        }
        
        double getP95ResponseTime() {
            if (responseTimes.isEmpty()) return 0.0;
            
            List<Long> sorted = new ArrayList<>(responseTimes);
            sorted.sort(Long::compareTo);
            
            int p95Index = (int) Math.ceil(sorted.size() * 0.95) - 1;
            return sorted.get(Math.max(0, p95Index));
        }
    }
    
    /**
     * Demo application simulating a web service
     */
    public static void main(String[] args) throws InterruptedException {
        ApplicationMonitor monitor = new ApplicationMonitor();
        monitor.startMonitoring();
        
        // Simulate web service traffic
        ExecutorService executor = Executors.newFixedThreadPool(10);
        Random random = new Random();
        
        String[] endpoints = {
            "/api/users", "/api/products", "/api/orders", 
            "/api/auth", "/api/reports", "/api/settings"
        };
        
        // Generate traffic for 2 minutes
        for (int i = 0; i < 1000; i++) {
            final int requestId = i;
            
            executor.submit(() -> {
                String endpoint = endpoints[random.nextInt(endpoints.length)];
                
                // Simulate endpoint processing
                long startTime = System.currentTimeMillis();
                
                try {
                    // Simulate varying response times
                    if (endpoint.equals("/api/reports")) {
                        Thread.sleep(200 + random.nextInt(800)); // Slower endpoint
                    } else {
                        Thread.sleep(20 + random.nextInt(100)); // Fast endpoints
                    }
                    
                    long responseTime = System.currentTimeMillis() - startTime;
                    
                    // Simulate error rates
                    int statusCode = 200;
                    if (random.nextDouble() < 0.05) { // 5% error rate
                        statusCode = 500;
                    } else if (random.nextDouble() < 0.02) { // 2% client error rate
                        statusCode = 404;
                    }
                    
                    monitor.recordEndpointCall(endpoint, responseTime, statusCode);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            
            // Control request rate
            if (i % 10 == 0) {
                Thread.sleep(100);
            }
        }
        
        // Wait for completion and final reports
        Thread.sleep(30000);
        
        executor.shutdown();
        monitor.scheduler.shutdown();
        monitor.systemMonitor.stopMonitoring();
    }
}
```

### **Challenge 2: Performance Alerting System**

Create `PerformanceAlerting.java`:

```java
import java.util.concurrent.*;
import java.util.*;
import java.time.*;
import java.util.function.*;

/**
 * Performance alerting system with configurable thresholds and notifications
 */
public class PerformanceAlerting {
    
    private final PerformanceMonitor monitor;
    private final Map<String, AlertRule> alertRules;
    private final Queue<Alert> alertHistory;
    private final ScheduledExecutorService alertScheduler;
    
    public PerformanceAlerting(PerformanceMonitor monitor) {
        this.monitor = monitor;
        this.alertRules = new ConcurrentHashMap<>();
        this.alertHistory = new ConcurrentLinkedQueue<>();
        this.alertScheduler = Executors.newScheduledThreadPool(1);
        
        setupDefaultAlerts();
    }
    
    /**
     * Setup default performance alerts
     */
    private void setupDefaultAlerts() {
        // Memory usage alert
        addAlert("high_memory_usage", 
                snapshot -> snapshot.getMemoryUtilization() > 0.85,
                "Memory usage above 85%",
                AlertSeverity.WARNING);
        
        // Critical memory alert
        addAlert("critical_memory_usage",
                snapshot -> snapshot.getMemoryUtilization() > 0.95,
                "Memory usage above 95%",
                AlertSeverity.CRITICAL);
        
        // High CPU usage
        addAlert("high_cpu_usage",
                snapshot -> snapshot.cpuUsage > 0.80,
                "CPU usage above 80%",
                AlertSeverity.WARNING);
        
        // High error rate
        addAlert("high_error_rate",
                snapshot -> snapshot.getErrorRate() > 0.05,
                "Error rate above 5%",
                AlertSeverity.ERROR);
        
        // Too many threads
        addAlert("thread_count_high",
                snapshot -> snapshot.threadCount > 100,
                "Thread count above 100",
                AlertSeverity.WARNING);
    }
    
    /**
     * Add custom alert rule
     */
    public void addAlert(String name, 
                        Predicate<PerformanceMonitor.PerformanceSnapshot> condition,
                        String message, 
                        AlertSeverity severity) {
        alertRules.put(name, new AlertRule(name, condition, message, severity));
    }
    
    /**
     * Start alert monitoring
     */
    public void startAlerting(Duration checkInterval) {
        alertScheduler.scheduleAtFixedRate(
            this::checkAlerts,
            0,
            checkInterval.toMillis(),
            TimeUnit.MILLISECONDS
        );
        
        System.out.println("🚨 Performance alerting started");
    }
    
    /**
     * Stop alert monitoring
     */
    public void stopAlerting() {
        alertScheduler.shutdown();
        System.out.println("🔕 Performance alerting stopped");
    }
    
    /**
     * Check all alert conditions
     */
    private void checkAlerts() {
        PerformanceMonitor.PerformanceSnapshot snapshot = monitor.getSnapshot();
        
        for (AlertRule rule : alertRules.values()) {
            if (rule.condition.test(snapshot)) {
                if (!rule.isCurrentlyFiring) {
                    fireAlert(rule, snapshot);
                }
            } else {
                if (rule.isCurrentlyFiring) {
                    clearAlert(rule, snapshot);
                }
            }
        }
    }
    
    /**
     * Fire an alert
     */
    private void fireAlert(AlertRule rule, PerformanceMonitor.PerformanceSnapshot snapshot) {
        rule.isCurrentlyFiring = true;
        rule.lastFiredTime = Instant.now();
        
        Alert alert = new Alert(
            rule.name,
            rule.message,
            rule.severity,
            snapshot,
            Instant.now(),
            AlertState.FIRING
        );
        
        alertHistory.offer(alert);
        
        // Keep only recent alerts
        if (alertHistory.size() > 1000) {
            alertHistory.poll();
        }
        
        // Send notification
        sendNotification(alert);
    }
    
    /**
     * Clear an alert
     */
    private void clearAlert(AlertRule rule, PerformanceMonitor.PerformanceSnapshot snapshot) {
        rule.isCurrentlyFiring = false;
        
        Alert clearAlert = new Alert(
            rule.name,
            rule.message + " - RESOLVED",
            rule.severity,
            snapshot,
            Instant.now(),
            AlertState.RESOLVED
        );
        
        alertHistory.offer(clearAlert);
        sendNotification(clearAlert);
    }
    
    /**
     * Send alert notification
     */
    private void sendNotification(Alert alert) {
        String emoji = getAlertEmoji(alert.severity);
        String stateEmoji = alert.state == AlertState.FIRING ? "🔥" : "✅";
        
        System.out.printf("%s %s [%s] %s - %s%n",
                         stateEmoji,
                         emoji,
                         alert.severity,
                         alert.ruleName,
                         alert.message);
        
        // Additional details for critical alerts
        if (alert.severity == AlertSeverity.CRITICAL) {
            System.out.printf("   📊 Memory: %.1f%%, CPU: %.1f%%, Threads: %d%n",
                             alert.snapshot.getMemoryUtilization() * 100,
                             alert.snapshot.cpuUsage * 100,
                             alert.snapshot.threadCount);
        }
    }
    
    /**
     * Get alert summary
     */
    public void printAlertSummary() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🚨 ALERT SUMMARY");
        System.out.println("=".repeat(80));
        
        Map<AlertSeverity, Long> severityCount = new EnumMap<>(AlertSeverity.class);
        Map<String, Long> ruleCount = new HashMap<>();
        
        for (Alert alert : alertHistory) {
            if (alert.state == AlertState.FIRING) {
                severityCount.merge(alert.severity, 1L, Long::sum);
                ruleCount.merge(alert.ruleName, 1L, Long::sum);
            }
        }
        
        System.out.println("\n📊 Alerts by Severity:");
        for (AlertSeverity severity : AlertSeverity.values()) {
            long count = severityCount.getOrDefault(severity, 0L);
            System.out.printf("   %s %-10s: %d alerts%n", 
                             getAlertEmoji(severity), severity, count);
        }
        
        if (!ruleCount.isEmpty()) {
            System.out.println("\n📋 Most Frequent Alerts:");
            ruleCount.entrySet().stream()
                     .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                     .limit(5)
                     .forEach(entry -> 
                         System.out.printf("   %-25s: %d times%n", 
                                          entry.getKey(), entry.getValue()));
        }
        
        // Current firing alerts
        List<AlertRule> firingRules = alertRules.values().stream()
                                                .filter(rule -> rule.isCurrentlyFiring)
                                                .toList();
        
        if (!firingRules.isEmpty()) {
            System.out.println("\n🔥 Currently Firing Alerts:");
            for (AlertRule rule : firingRules) {
                Duration duration = Duration.between(rule.lastFiredTime, Instant.now());
                System.out.printf("   %s %s - %s (firing for %s)%n",
                                 getAlertEmoji(rule.severity),
                                 rule.name,
                                 rule.message,
                                 formatDuration(duration));
            }
        } else {
            System.out.println("\n✅ No alerts currently firing");
        }
    }
    
    private String getAlertEmoji(AlertSeverity severity) {
        return switch (severity) {
            case INFO -> "ℹ️";
            case WARNING -> "⚠️";
            case ERROR -> "❌";
            case CRITICAL -> "🆘";
        };
    }
    
    private String formatDuration(Duration duration) {
        long seconds = duration.toSeconds();
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m " + (seconds % 60) + "s";
        return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
    }
    
    /**
     * Alert rule definition
     */
    private static class AlertRule {
        final String name;
        final Predicate<PerformanceMonitor.PerformanceSnapshot> condition;
        final String message;
        final AlertSeverity severity;
        volatile boolean isCurrentlyFiring = false;
        volatile Instant lastFiredTime;
        
        AlertRule(String name, 
                 Predicate<PerformanceMonitor.PerformanceSnapshot> condition,
                 String message, 
                 AlertSeverity severity) {
            this.name = name;
            this.condition = condition;
            this.message = message;
            this.severity = severity;
        }
    }
    
    /**
     * Alert instance
     */
    public static class Alert {
        public final String ruleName;
        public final String message;
        public final AlertSeverity severity;
        public final PerformanceMonitor.PerformanceSnapshot snapshot;
        public final Instant timestamp;
        public final AlertState state;
        
        Alert(String ruleName, String message, AlertSeverity severity,
              PerformanceMonitor.PerformanceSnapshot snapshot, Instant timestamp,
              AlertState state) {
            this.ruleName = ruleName;
            this.message = message;
            this.severity = severity;
            this.snapshot = snapshot;
            this.timestamp = timestamp;
            this.state = state;
        }
    }
    
    /**
     * Alert severity levels
     */
    public enum AlertSeverity {
        INFO, WARNING, ERROR, CRITICAL
    }
    
    /**
     * Alert states
     */
    public enum AlertState {
        FIRING, RESOLVED
    }
    
    /**
     * Demo application
     */
    public static void main(String[] args) throws InterruptedException {
        PerformanceMonitor monitor = new PerformanceMonitor();
        PerformanceAlerting alerting = new PerformanceAlerting(monitor);
        
        // Start monitoring and alerting
        monitor.startMonitoring(Duration.ofSeconds(1));
        alerting.startAlerting(Duration.ofSeconds(2));
        
        // Simulate problematic workload
        System.out.println("🏃 Simulating problematic workload...");
        
        ExecutorService executor = Executors.newFixedThreadPool(20);
        List<byte[]> memoryHog = new ArrayList<>();
        
        // Phase 1: Normal load
        System.out.println("Phase 1: Normal operation");
        for (int i = 0; i < 20; i++) {
            executor.submit(() -> simulateNormalWork());
            Thread.sleep(100);
        }
        
        Thread.sleep(5000);
        
        // Phase 2: Memory pressure
        System.out.println("Phase 2: Creating memory pressure");
        for (int i = 0; i < 50; i++) {
            memoryHog.add(new byte[10 * 1024 * 1024]); // 10MB chunks
            Thread.sleep(200);
        }
        
        Thread.sleep(10000);
        
        // Phase 3: High error rate
        System.out.println("Phase 3: Simulating high error rate");
        for (int i = 0; i < 50; i++) {
            executor.submit(() -> {
                monitor.recordRequest(100, Math.random() < 0.3); // 30% error rate
            });
            Thread.sleep(100);
        }
        
        Thread.sleep(5000);
        
        // Cleanup and summary
        memoryHog.clear();
        System.gc();
        Thread.sleep(5000);
        
        alerting.printAlertSummary();
        
        // Shutdown
        executor.shutdown();
        alerting.stopAlerting();
        monitor.stopMonitoring();
    }
    
    private static void simulateNormalWork() {
        try {
            double result = 0;
            for (int i = 0; i < 1000; i++) {
                result += Math.sin(i);
            }
            Thread.sleep(50);
            
            // Prevent optimization
            if (result > Double.MAX_VALUE) {
                System.out.println("Impossible: " + result);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

---

## 📝 Key Takeaways from Day 43

### **Performance Monitoring Concepts:**
- ✅ **JVM Metrics**: Memory, CPU, threads, garbage collection monitoring
- ✅ **Built-in Tools**: ManagementFactory MXBeans for system introspection
- ✅ **Custom Monitoring**: Building application-specific performance tracking
- ✅ **Real-time Dashboards**: Live visualization of performance metrics
- ✅ **Alert Systems**: Proactive notification of performance issues

### **Practical Skills:**
- ✅ **MBean Usage**: Leveraging JMX for performance data collection
- ✅ **Metric Collection**: Systematic approach to performance data gathering
- ✅ **Trend Analysis**: Identifying performance patterns and anomalies
- ✅ **Dashboard Creation**: Building real-time monitoring interfaces
- ✅ **Alert Configuration**: Setting up intelligent performance alerts

### **Performance Analysis:**
- ✅ **Memory Profiling**: Understanding heap usage patterns and leak detection
- ✅ **CPU Monitoring**: Tracking processor utilization and identifying bottlenecks
- ✅ **Thread Analysis**: Monitoring concurrent execution and thread pool health
- ✅ **GC Analysis**: Understanding garbage collection impact on performance
- ✅ **Application Metrics**: Tracking business-relevant performance indicators

---

## 🚀 Tomorrow's Preview (Day 44 - August 22)

**Focus:** Memory Management & JVM Tuning
- Deep dive into JVM memory model and garbage collection
- Hands-on GC algorithm analysis and tuning
- Memory leak detection and prevention strategies
- JVM parameter optimization techniques
- Building memory-efficient applications

**Preparation:**
- Review memory monitoring concepts from today
- Install JVisualVM or JProfiler for tomorrow's exercises
- Think about memory-intensive applications you've worked with

---

## ✅ Day 43 Checklist

**Completed Learning Objectives:**
- [ ] Built comprehensive performance monitoring system
- [ ] Implemented memory profiling and leak detection
- [ ] Created real-time performance dashboard
- [ ] Developed performance alerting system
- [ ] Understood JVM performance characteristics
- [ ] Mastered MBean-based monitoring approaches
- [ ] Practiced performance trend analysis
- [ ] Built application-specific monitoring solutions

**Bonus Achievements:**
- [ ] Extended monitoring to include custom metrics
- [ ] Created performance baseline measurements
- [ ] Implemented automated performance reporting
- [ ] Explored JConsole and VisualVM integration

---

## 🎉 Excellent Progress!

You've mastered the fundamentals of Java performance monitoring and built professional-grade monitoring solutions. Tomorrow we'll dive deeper into memory management and JVM tuning to optimize application performance at the system level.

**Keep the momentum going!** 🚀