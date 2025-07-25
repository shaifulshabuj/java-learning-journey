# Day 86: Advanced JVM Tuning - Performance Optimization & Profiling

## Overview
Building on yesterday's JVM deep dive, today we explore advanced JVM tuning techniques, performance optimization strategies, and profiling tools. We'll cover GC tuning parameters, heap sizing strategies, JIT compiler optimizations, and comprehensive profiling approaches for enterprise Java applications.

## Learning Objectives
- Master advanced JVM tuning parameters and optimization techniques
- Implement comprehensive profiling strategies for performance analysis
- Configure optimal GC settings for different application profiles
- Use advanced monitoring and diagnostic tools
- Apply performance optimization methodologies in enterprise contexts

## 1. JVM Performance Tuning Fundamentals

### JVM Tuning Philosophy
```java
package com.javajourney.jvmtuning;

/**
 * JVM Performance Tuning Philosophy and Approach
 * 
 * Key Principles:
 * 1. Measure first, optimize second
 * 2. Understand your application's allocation patterns
 * 3. Choose appropriate GC algorithm for workload
 * 4. Monitor continuously, tune incrementally
 * 5. Balance throughput vs latency requirements
 */
public class JVMTuningPhilosophy {
    
    /**
     * Performance characteristics categorization
     */
    public enum ApplicationProfile {
        HIGH_THROUGHPUT(
            "Batch processing, data analytics",
            "Parallel GC, large heap sizes",
            "Optimize for total throughput"
        ),
        LOW_LATENCY(
            "Real-time trading, gaming",
            "G1GC or ZGC, predictable pause times",
            "Optimize for response time consistency"
        ),
        BALANCED(
            "Web applications, microservices",
            "G1GC with balanced settings",
            "Balance between throughput and latency"
        ),
        MEMORY_CONSTRAINED(
            "Container environments, embedded",
            "Serial GC, aggressive heap sizing",
            "Optimize for memory footprint"
        );
        
        private final String useCase;
        private final String recommendedSettings;
        private final String optimizationFocus;
        
        ApplicationProfile(String useCase, String recommendedSettings, String optimizationFocus) {
            this.useCase = useCase;
            this.recommendedSettings = recommendedSettings;
            this.optimizationFocus = optimizationFocus;
        }
        
        public void printProfile() {
            System.out.printf("Profile: %s%n", name());
            System.out.printf("Use Case: %s%n", useCase);
            System.out.printf("Recommended: %s%n", recommendedSettings);
            System.out.printf("Focus: %s%n%n", optimizationFocus);
        }
    }
    
    /**
     * Systematic tuning approach
     */
    public static class TuningMethodology {
        public static void demonstrateSystematicApproach() {
            System.out.println("=== JVM Tuning Methodology ===");
            System.out.println("1. Baseline Measurement");
            System.out.println("   - Record current performance metrics");
            System.out.println("   - Identify bottlenecks and pain points");
            System.out.println("   - Establish performance goals");
            
            System.out.println("\n2. Analysis Phase");
            System.out.println("   - Analyze GC logs and heap dumps");
            System.out.println("   - Profile CPU usage and allocation patterns");
            System.out.println("   - Identify optimization opportunities");
            
            System.out.println("\n3. Incremental Tuning");
            System.out.println("   - Change one parameter at a time");
            System.out.println("   - Test thoroughly under realistic load");
            System.out.println("   - Document changes and results");
            
            System.out.println("\n4. Validation and Monitoring");
            System.out.println("   - Verify improvements in production");
            System.out.println("   - Set up continuous monitoring");
            System.out.println("   - Plan for ongoing optimization");
        }
    }
}
```

### Heap Sizing and Memory Tuning
```java
package com.javajourney.jvmtuning;

import java.lang.management.*;

/**
 * Advanced heap sizing strategies and memory tuning
 */
public class HeapSizingStrategy {
    
    /**
     * Dynamic heap sizing calculator based on application profile
     */
    public static class HeapSizeCalculator {
        
        public static HeapRecommendations calculateOptimalHeapSize(
                ApplicationProfile profile, 
                long expectedDataSize, 
                int concurrentUsers,
                double safetyMargin) {
            
            long baseHeapSize = calculateBaseHeapSize(expectedDataSize, concurrentUsers);
            long adjustedSize = applyProfileAdjustments(baseHeapSize, profile);
            long finalSize = (long) (adjustedSize * (1 + safetyMargin));
            
            return new HeapRecommendations(
                finalSize,
                calculateYoungGenSize(finalSize, profile),
                calculateOldGenSize(finalSize, profile),
                calculateMetaspaceSize(profile)
            );
        }
        
        private static long calculateBaseHeapSize(long dataSize, int users) {
            // Base calculation: data size + user session overhead + JVM overhead
            long userOverhead = users * 10 * 1024 * 1024; // 10MB per user
            long jvmOverhead = 256 * 1024 * 1024; // 256MB JVM overhead
            return dataSize + userOverhead + jvmOverhead;
        }
        
        private static long applyProfileAdjustments(long baseSize, ApplicationProfile profile) {
            return switch (profile) {
                case HIGH_THROUGHPUT -> (long) (baseSize * 1.5); // Larger heap for batch processing
                case LOW_LATENCY -> (long) (baseSize * 0.8); // Smaller heap for predictable GC
                case BALANCED -> baseSize; // Use base calculation
                case MEMORY_CONSTRAINED -> (long) (baseSize * 0.6); // Minimal heap size
            };
        }
        
        private static long calculateYoungGenSize(long totalHeap, ApplicationProfile profile) {
            double youngGenRatio = switch (profile) {
                case HIGH_THROUGHPUT -> 0.4; // Large young gen for high allocation rate
                case LOW_LATENCY -> 0.2; // Small young gen for predictable pauses
                case BALANCED -> 0.33; // Balanced approach
                case MEMORY_CONSTRAINED -> 0.25; // Conservative young gen
            };
            return (long) (totalHeap * youngGenRatio);
        }
        
        private static long calculateOldGenSize(long totalHeap, ApplicationProfile profile) {
            long youngGenSize = calculateYoungGenSize(totalHeap, profile);
            return totalHeap - youngGenSize;
        }
        
        private static long calculateMetaspaceSize(ApplicationProfile profile) {
            return switch (profile) {
                case HIGH_THROUGHPUT -> 512 * 1024 * 1024; // 512MB for complex applications
                case LOW_LATENCY -> 256 * 1024 * 1024; // 256MB for optimized applications
                case BALANCED -> 384 * 1024 * 1024; // 384MB balanced
                case MEMORY_CONSTRAINED -> 128 * 1024 * 1024; // 128MB minimal
            };
        }
    }
    
    /**
     * Heap recommendations data structure
     */
    public static class HeapRecommendations {
        private final long totalHeapSize;
        private final long youngGenSize;
        private final long oldGenSize;
        private final long metaspaceSize;
        
        public HeapRecommendations(long totalHeapSize, long youngGenSize, 
                                 long oldGenSize, long metaspaceSize) {
            this.totalHeapSize = totalHeapSize;
            this.youngGenSize = youngGenSize;
            this.oldGenSize = oldGenSize;
            this.metaspaceSize = metaspaceSize;
        }
        
        public String generateJVMParameters() {
            return String.format(
                "-Xms%dm -Xmx%dm -Xmn%dm -XX:MetaspaceSize=%dm -XX:MaxMetaspaceSize=%dm",
                toMB(totalHeapSize / 2), // Initial heap size (50% of max)
                toMB(totalHeapSize),
                toMB(youngGenSize),
                toMB(metaspaceSize),
                toMB(metaspaceSize * 2)
            );
        }
        
        private long toMB(long bytes) {
            return bytes / (1024 * 1024);
        }
        
        public void printRecommendations() {
            System.out.printf("=== Heap Sizing Recommendations ===%n");
            System.out.printf("Total Heap: %d MB%n", toMB(totalHeapSize));
            System.out.printf("Young Generation: %d MB (%.1f%%)%n", 
                toMB(youngGenSize), (double) youngGenSize / totalHeapSize * 100);
            System.out.printf("Old Generation: %d MB (%.1f%%)%n", 
                toMB(oldGenSize), (double) oldGenSize / totalHeapSize * 100);
            System.out.printf("Metaspace: %d MB%n", toMB(metaspaceSize));
            System.out.printf("JVM Parameters: %s%n", generateJVMParameters());
        }
    }
}
```

## 2. Garbage Collection Tuning

### Advanced GC Configuration
```java
package com.javajourney.jvmtuning;

import java.util.Map;
import java.util.HashMap;

/**
 * Advanced Garbage Collection tuning configurations
 */
public class GCTuningConfiguration {
    
    /**
     * GC algorithm selection and configuration
     */
    public enum GCAlgorithm {
        SERIAL("Serial GC", "-XX:+UseSerialGC"),
        PARALLEL("Parallel GC", "-XX:+UseParallelGC"),
        G1("G1 Garbage Collector", "-XX:+UseG1GC"),
        ZGC("ZGC (Low Latency)", "-XX:+UseZGC"),
        SHENANDOAH("Shenandoah GC", "-XX:+UseShenandoahGC");
        
        private final String description;
        private final String basicFlag;
        
        GCAlgorithm(String description, String basicFlag) {
            this.description = description;
            this.basicFlag = basicFlag;
        }
        
        public String getDescription() { return description; }
        public String getBasicFlag() { return basicFlag; }
    }
    
    /**
     * G1GC tuning configuration
     */
    public static class G1GCConfiguration {
        
        public static String generateOptimizedG1Parameters(
                ApplicationProfile profile, 
                long heapSizeMB,
                int targetPauseMs) {
            
            StringBuilder params = new StringBuilder();
            params.append("-XX:+UseG1GC ");
            params.append("-XX:MaxGCPauseMillis=").append(targetPauseMs).append(" ");
            
            // Region size calculation (should be between 1MB and 32MB)
            int regionSize = calculateOptimalRegionSize(heapSizeMB);
            params.append("-XX:G1HeapRegionSize=").append(regionSize).append("m ");
            
            // Concurrent threads
            int concurrentThreads = Math.max(1, Runtime.getRuntime().availableProcessors() / 4);
            params.append("-XX:ConcGCThreads=").append(concurrentThreads).append(" ");
            
            // Profile-specific optimizations
            switch (profile) {
                case HIGH_THROUGHPUT -> {
                    params.append("-XX:G1NewSizePercent=40 ");
                    params.append("-XX:G1MaxNewSizePercent=50 ");
                    params.append("-XX:G1MixedGCLiveThresholdPercent=85 ");
                }
                case LOW_LATENCY -> {
                    params.append("-XX:G1NewSizePercent=20 ");
                    params.append("-XX:G1MaxNewSizePercent=30 ");
                    params.append("-XX:G1MixedGCLiveThresholdPercent=90 ");
                    params.append("-XX:G1HeapWastePercent=5 ");
                }
                case BALANCED -> {
                    params.append("-XX:G1NewSizePercent=30 ");
                    params.append("-XX:G1MaxNewSizePercent=40 ");
                }
                case MEMORY_CONSTRAINED -> {
                    params.append("-XX:G1NewSizePercent=25 ");
                    params.append("-XX:G1MaxNewSizePercent=35 ");
                    params.append("-XX:G1HeapWastePercent=10 ");
                }
            }
            
            return params.toString().trim();
        }
        
        private static int calculateOptimalRegionSize(long heapSizeMB) {
            // Target around 2048 regions for optimal performance
            int regionSize = (int) Math.max(1, Math.min(32, heapSizeMB / 2048));
            // Round to nearest power of 2
            return Integer.highestOneBit(regionSize);
        }
    }
    
    /**
     * Parallel GC tuning configuration
     */
    public static class ParallelGCConfiguration {
        
        public static String generateOptimizedParallelParameters(
                ApplicationProfile profile,
                int availableProcessors) {
            
            StringBuilder params = new StringBuilder();
            params.append("-XX:+UseParallelGC ");
            
            // GC thread configuration
            int gcThreads = calculateOptimalGCThreads(availableProcessors, profile);
            params.append("-XX:ParallelGCThreads=").append(gcThreads).append(" ");
            
            // Adaptive sizing
            params.append("-XX:+UseAdaptiveSizePolicy ");
            
            // Profile-specific optimizations
            switch (profile) {
                case HIGH_THROUGHPUT -> {
                    params.append("-XX:GCTimeRatio=19 "); // 5% GC time target
                    params.append("-XX:MaxGCPauseMillis=200 ");
                    params.append("-XX:NewRatio=2 "); // Old:Young = 2:1
                }
                case BALANCED -> {
                    params.append("-XX:GCTimeRatio=9 "); // 10% GC time target
                    params.append("-XX:MaxGCPauseMillis=100 ");
                    params.append("-XX:NewRatio=3 "); // Old:Young = 3:1
                }
                case MEMORY_CONSTRAINED -> {
                    params.append("-XX:GCTimeRatio=4 "); // 20% GC time acceptable
                    params.append("-XX:MaxGCPauseMillis=50 ");
                    params.append("-XX:NewRatio=4 "); // Smaller young generation
                }
            }
            
            return params.toString().trim();
        }
        
        private static int calculateOptimalGCThreads(int processors, ApplicationProfile profile) {
            return switch (profile) {
                case HIGH_THROUGHPUT -> Math.min(processors, 16); // Use more threads for throughput
                case LOW_LATENCY -> Math.min(processors / 2, 8); // Fewer threads for predictability
                case BALANCED -> Math.min(processors * 3 / 4, 12); // Balanced approach
                case MEMORY_CONSTRAINED -> Math.min(processors / 2, 4); // Conservative thread usage
            };
        }
    }
}
```

### GC Log Analysis and Monitoring
```java
package com.javajourney.jvmtuning;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Advanced GC log analysis and monitoring utilities
 */
public class GCLogAnalyzer {
    
    /**
     * GC log parser for performance analysis
     */
    public static class GCLogParser {
        private final Pattern gcPattern = Pattern.compile(
            "\\[(\\d+\\.\\d+)s\\].*GC\\(\\d+\\).*pause (\\d+\\.\\d+)ms"
        );
        
        public GCAnalysisReport analyzeGCLog(String logFilePath) throws IOException {
            List<GCEvent> events = new ArrayList<>();
            
            try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = gcPattern.matcher(line);
                    if (matcher.find()) {
                        double timestamp = Double.parseDouble(matcher.group(1));
                        double pauseTime = Double.parseDouble(matcher.group(2));
                        events.add(new GCEvent(timestamp, pauseTime));
                    }
                }
            }
            
            return new GCAnalysisReport(events);
        }
    }
    
    /**
     * GC event data structure
     */
    public static class GCEvent {
        private final double timestamp;
        private final double pauseTimeMs;
        
        public GCEvent(double timestamp, double pauseTimeMs) {
            this.timestamp = timestamp;
            this.pauseTimeMs = pauseTimeMs;
        }
        
        public double getTimestamp() { return timestamp; }
        public double getPauseTimeMs() { return pauseTimeMs; }
    }
    
    /**
     * Comprehensive GC analysis report
     */
    public static class GCAnalysisReport {
        private final List<GCEvent> events;
        private final GCStatistics statistics;
        
        public GCAnalysisReport(List<GCEvent> events) {
            this.events = events;
            this.statistics = calculateStatistics(events);
        }
        
        private GCStatistics calculateStatistics(List<GCEvent> events) {
            if (events.isEmpty()) {
                return new GCStatistics(0, 0, 0, 0, 0, 0);
            }
            
            double totalPauseTime = events.stream()
                .mapToDouble(GCEvent::getPauseTimeMs)
                .sum();
            
            double avgPauseTime = totalPauseTime / events.size();
            
            double maxPauseTime = events.stream()
                .mapToDouble(GCEvent::getPauseTimeMs)
                .max()
                .orElse(0);
            
            double minPauseTime = events.stream()
                .mapToDouble(GCEvent::getPauseTimeMs)
                .min()
                .orElse(0);
            
            // Calculate percentiles
            List<Double> sortedPauseTimes = events.stream()
                .mapToDouble(GCEvent::getPauseTimeMs)
                .sorted()
                .boxed()
                .toList();
            
            double p95PauseTime = calculatePercentile(sortedPauseTimes, 95);
            double p99PauseTime = calculatePercentile(sortedPauseTimes, 99);
            
            return new GCStatistics(
                events.size(),
                totalPauseTime,
                avgPauseTime,
                maxPauseTime,
                p95PauseTime,
                p99PauseTime
            );
        }
        
        private double calculatePercentile(List<Double> sortedValues, int percentile) {
            int index = (int) Math.ceil(sortedValues.size() * percentile / 100.0) - 1;
            return sortedValues.get(Math.max(0, Math.min(index, sortedValues.size() - 1)));
        }
        
        public void printAnalysisReport() {
            System.out.println("=== GC Analysis Report ===");
            System.out.printf("Total GC Events: %d%n", statistics.totalEvents);
            System.out.printf("Total Pause Time: %.2f ms%n", statistics.totalPauseTime);
            System.out.printf("Average Pause Time: %.2f ms%n", statistics.avgPauseTime);
            System.out.printf("Maximum Pause Time: %.2f ms%n", statistics.maxPauseTime);
            System.out.printf("95th Percentile: %.2f ms%n", statistics.p95PauseTime);
            System.out.printf("99th Percentile: %.2f ms%n", statistics.p99PauseTime);
            
            // Performance assessment
            assessPerformance();
        }
        
        private void assessPerformance() {
            System.out.println("\n=== Performance Assessment ===");
            
            if (statistics.avgPauseTime < 10) {
                System.out.println("✅ Excellent: Average pause time < 10ms");
            } else if (statistics.avgPauseTime < 50) {
                System.out.println("✅ Good: Average pause time < 50ms");
            } else if (statistics.avgPauseTime < 100) {
                System.out.println("⚠️ Acceptable: Average pause time < 100ms");
            } else {
                System.out.println("❌ Poor: Average pause time > 100ms - Tuning needed");
            }
            
            if (statistics.p99PauseTime < 100) {
                System.out.println("✅ Consistent: 99th percentile < 100ms");
            } else {
                System.out.println("⚠️ Inconsistent: High tail latency detected");
            }
        }
        
        public GCStatistics getStatistics() { return statistics; }
    }
    
    /**
     * GC statistics data structure
     */
    public static class GCStatistics {
        public final int totalEvents;
        public final double totalPauseTime;
        public final double avgPauseTime;
        public final double maxPauseTime;
        public final double p95PauseTime;
        public final double p99PauseTime;
        
        public GCStatistics(int totalEvents, double totalPauseTime, double avgPauseTime,
                          double maxPauseTime, double p95PauseTime, double p99PauseTime) {
            this.totalEvents = totalEvents;
            this.totalPauseTime = totalPauseTime;
            this.avgPauseTime = avgPauseTime;
            this.maxPauseTime = maxPauseTime;
            this.p95PauseTime = p95PauseTime;
            this.p99PauseTime = p99PauseTime;
        }
    }
}
```

## 3. JIT Compiler Optimization

### JIT Compiler Tuning
```java
package com.javajourney.jvmtuning;

import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;

/**
 * JIT (Just-In-Time) Compiler optimization and tuning
 */
public class JITCompilerOptimization {
    
    /**
     * JIT compilation monitoring and analysis
     */
    public static class JITMonitor {
        
        public static void analyzeJITCompilation() {
            CompilationMXBean compilationBean = ManagementFactory.getCompilationMXBean();
            
            System.out.println("=== JIT Compiler Analysis ===");
            System.out.printf("Compiler Name: %s%n", compilationBean.getName());
            System.out.printf("Total Compilation Time: %d ms%n", compilationBean.getTotalCompilationTime());
            System.out.printf("Compilation Time Monitoring Supported: %s%n", 
                compilationBean.isCompilationTimeMonitoringSupported());
        }
        
        public static void demonstrateCompilationTiers() {
            System.out.println("\n=== JIT Compilation Tiers ===");
            System.out.println("Tier 0: Interpreter");
            System.out.println("  - Direct bytecode interpretation");
            System.out.println("  - Slowest execution, gathers profiling info");
            
            System.out.println("\nTier 1: C1 Compiler (Client)");
            System.out.println("  - Fast compilation, basic optimizations");
            System.out.println("  - Used for startup performance");
            
            System.out.println("\nTier 2: C1 with Limited Profiling");
            System.out.println("  - C1 with invocation and back-edge counters");
            System.out.println("  - Prepares for C2 compilation");
            
            System.out.println("\nTier 3: C1 with Full Profiling");
            System.out.println("  - C1 with all profiling information");
            System.out.println("  - Detailed branch and type profiling");
            
            System.out.println("\nTier 4: C2 Compiler (Server)");
            System.out.println("  - Aggressive optimizations");
            System.out.println("  - Best performance for long-running code");
        }
    }
    
    /**
     * JIT optimization strategies for different scenarios
     */
    public static class JITOptimizationStrategies {
        
        /**
         * Startup performance optimization
         */
        public static String generateStartupOptimizedParameters() {
            return """
                # Startup Performance Optimization
                -XX:TieredStopAtLevel=1    # Use only C1 compiler for faster startup
                -XX:+TieredCompilation     # Enable tiered compilation
                -XX:CompileThreshold=100   # Lower compilation threshold
                -XX:+UseStringDeduplication # Reduce memory for string-heavy apps
                -XX:+OptimizeStringConcat  # Optimize string concatenation
                """;
        }
        
        /**
         * Throughput optimization
         */
        public static String generateThroughputOptimizedParameters() {
            return """
                # Throughput Performance Optimization
                -XX:+TieredCompilation     # Use all compilation tiers
                -XX:TieredStopAtLevel=4    # Allow C2 compiler optimizations
                -XX:CompileThreshold=10000 # Higher threshold for C2 compilation
                -XX:+UseCodeCacheFlushing  # Enable code cache management
                -XX:ReservedCodeCacheSize=256m # Larger code cache
                -XX:+AggressiveOpts        # Enable aggressive optimizations
                """;
        }
        
        /**
         * Memory-constrained optimization
         */
        public static String generateMemoryConstrainedParameters() {
            return """
                # Memory-Constrained Optimization
                -XX:TieredStopAtLevel=1    # Use only C1 to save memory
                -XX:ReservedCodeCacheSize=64m # Smaller code cache
                -XX:CompileThreshold=1000  # Moderate compilation threshold
                -XX:+UseStringDeduplication # Reduce string memory overhead
                """;
        }
    }
    
    /**
     * Code cache monitoring and management
     */
    public static class CodeCacheMonitor {
        
        public static void analyzeCodeCache() {
            var memoryBeans = ManagementFactory.getMemoryPoolMXBeans();
            
            System.out.println("=== Code Cache Analysis ===");
            for (var bean : memoryBeans) {
                if (bean.getName().contains("Code")) {
                    var usage = bean.getUsage();
                    System.out.printf("Pool: %s%n", bean.getName());
                    System.out.printf("  Used: %d MB%n", usage.getUsed() / 1024 / 1024);
                    System.out.printf("  Committed: %d MB%n", usage.getCommitted() / 1024 / 1024);
                    System.out.printf("  Max: %d MB%n", usage.getMax() / 1024 / 1024);
                    System.out.printf("  Usage: %.1f%%%n", 
                        (double) usage.getUsed() / usage.getMax() * 100);
                }
            }
        }
        
        public static void recommendCodeCacheSettings(ApplicationProfile profile) {
            System.out.println("\n=== Code Cache Recommendations ===");
            
            String recommendations = switch (profile) {
                case HIGH_THROUGHPUT -> """
                    -XX:ReservedCodeCacheSize=512m
                    -XX:InitialCodeCacheSize=128m
                    -XX:+UseCodeCacheFlushing
                    """;
                case LOW_LATENCY -> """
                    -XX:ReservedCodeCacheSize=256m
                    -XX:InitialCodeCacheSize=64m
                    -XX:+SegmentedCodeCache
                    """;
                case BALANCED -> """
                    -XX:ReservedCodeCacheSize=256m
                    -XX:InitialCodeCacheSize=64m
                    -XX:+UseCodeCacheFlushing
                    """;
                case MEMORY_CONSTRAINED -> """
                    -XX:ReservedCodeCacheSize=64m
                    -XX:InitialCodeCacheSize=16m
                    -XX:+UseCodeCacheFlushing
                    """;
            };
            
            System.out.println(recommendations);
        }
    }
}
```

## 4. Advanced Profiling Techniques

### CPU and Memory Profiling
```java
package com.javajourney.jvmtuning;

import java.lang.management.*;
import java.util.concurrent.*;

/**
 * Advanced profiling techniques for performance analysis
 */
public class AdvancedProfiling {
    
    /**
     * CPU profiling and hotspot detection
     */
    public static class CPUProfiler {
        private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        public void startCPUProfiling(int intervalSeconds, int durationMinutes) {
            System.out.println("=== Starting CPU Profiling ===");
            
            scheduler.scheduleAtFixedRate(() -> {
                ThreadInfo[] threadInfos = threadBean.dumpAllThreads(false, false);
                analyzeCPUUsage(threadInfos);
            }, 0, intervalSeconds, TimeUnit.SECONDS);
            
            // Stop profiling after specified duration
            scheduler.schedule(() -> {
                scheduler.shutdown();
                System.out.println("CPU Profiling completed");
            }, durationMinutes, TimeUnit.MINUTES);
        }
        
        private void analyzeCPUUsage(ThreadInfo[] threadInfos) {
            System.out.printf("=== CPU Profile Snapshot - %s ===%n", 
                java.time.LocalTime.now());
            
            for (ThreadInfo info : threadInfos) {
                if (info != null && threadBean.isThreadCpuTimeSupported()) {
                    long cpuTime = threadBean.getThreadCpuTime(info.getThreadId());
                    if (cpuTime > 0) {
                        System.out.printf("Thread: %s, CPU Time: %d ms%n", 
                            info.getThreadName(), cpuTime / 1000000);
                    }
                }
            }
            System.out.println();
        }
        
        public void detectHotMethods() {
            System.out.println("=== Hot Method Detection ===");
            System.out.println("Enable with JVM flags:");
            System.out.println("-XX:+PrintCompilation");
            System.out.println("-XX:+UnlockDiagnosticVMOptions");
            System.out.println("-XX:+PrintInlining");
            System.out.println("-XX:+LogCompilation");
        }
    }
    
    /**
     * Memory allocation profiling
     */
    public static class MemoryProfiler {
        
        public static void profileMemoryAllocation() {
            System.out.println("=== Memory Allocation Profiling ===");
            
            // Get all memory pool beans
            var memoryPools = ManagementFactory.getMemoryPoolMXBeans();
            
            for (var pool : memoryPools) {
                var usage = pool.getUsage();
                var peakUsage = pool.getPeakUsage();
                
                System.out.printf("Memory Pool: %s%n", pool.getName());
                System.out.printf("  Type: %s%n", pool.getType());
                System.out.printf("  Current Usage: %.1f MB%n", 
                    usage.getUsed() / 1024.0 / 1024.0);
                System.out.printf("  Peak Usage: %.1f MB%n", 
                    peakUsage.getUsed() / 1024.0 / 1024.0);
                System.out.printf("  Usage Threshold Supported: %s%n", 
                    pool.isUsageThresholdSupported());
                
                if (pool.isCollectionUsageThresholdSupported()) {
                    System.out.printf("  Collection Usage Count: %d%n", 
                        pool.getCollectionUsageThresholdCount());
                }
                System.out.println();
            }
        }
        
        public static void demonstrateAllocationProfiling() {
            System.out.println("=== Allocation Profiling Techniques ===");
            
            System.out.println("1. JFR (Java Flight Recorder):");
            System.out.println("   -XX:+FlightRecorder");
            System.out.println("   -XX:StartFlightRecording=duration=60s,filename=profile.jfr");
            
            System.out.println("\n2. Native Memory Tracking:");
            System.out.println("   -XX:NativeMemoryTracking=detail");
            System.out.println("   jcmd <pid> VM.native_memory");
            
            System.out.println("\n3. Allocation Sampling:");
            System.out.println("   -XX:+UseBiasedLocking");
            System.out.println("   -XX:BiasedLockingStartupDelay=0");
            
            System.out.println("\n4. TLAB Analysis:");
            System.out.println("   -XX:+PrintTLAB");
            System.out.println("   -XX:TLABWasteTargetPercent=1");
            System.out.println("   -XX:ResizeTLAB");
        }
    }
    
    /**
     * Lock contention profiling
     */
    public static class LockContentionProfiler {
        private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        
        public void analyzeLockContention() {
            if (!threadBean.isThreadContentionMonitoringSupported()) {
                System.out.println("Thread contention monitoring not supported");
                return;
            }
            
            threadBean.setThreadContentionMonitoringEnabled(true);
            
            System.out.println("=== Lock Contention Analysis ===");
            
            long[] threadIds = threadBean.getAllThreadIds();
            ThreadInfo[] threadInfos = threadBean.getThreadInfo(threadIds);
            
            for (ThreadInfo info : threadInfos) {
                if (info != null) {
                    System.out.printf("Thread: %s%n", info.getThreadName());
                    System.out.printf("  Blocked Count: %d%n", info.getBlockedCount());
                    System.out.printf("  Blocked Time: %d ms%n", info.getBlockedTime());
                    System.out.printf("  Waited Count: %d%n", info.getWaitedCount());
                    System.out.printf("  Waited Time: %d ms%n", info.getWaitedTime());
                    
                    if (info.getLockInfo() != null) {
                        System.out.printf("  Lock: %s%n", info.getLockInfo());
                    }
                    System.out.println();
                }
            }
        }
        
        public void recommendLockOptimizations() {
            System.out.println("=== Lock Optimization Recommendations ===");
            System.out.println("1. Reduce lock scope");
            System.out.println("2. Use lock-free data structures");
            System.out.println("3. Consider read-write locks");
            System.out.println("4. Implement lock ordering");
            System.out.println("5. Use concurrent collections");
            System.out.println("6. Enable biased locking for single-threaded access");
        }
    }
}
```

## 5. Production Monitoring and Alerting

### JVM Monitoring Framework
```java
package com.javajourney.jvmtuning;

import javax.management.*;
import java.lang.management.*;
import java.util.concurrent.*;

/**
 * Production JVM monitoring and alerting system
 */
public class ProductionMonitoring {
    
    /**
     * Comprehensive JVM health monitor
     */
    public static class JVMHealthMonitor {
        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        private final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        
        public void startMonitoring() {
            System.out.println("Starting JVM Health Monitoring...");
            
            // Memory monitoring
            scheduler.scheduleAtFixedRate(this::monitorMemory, 0, 30, TimeUnit.SECONDS);
            
            // GC monitoring  
            scheduler.scheduleAtFixedRate(this::monitorGC, 0, 60, TimeUnit.SECONDS);
            
            // Thread monitoring
            scheduler.scheduleAtFixedRate(this::monitorThreads, 0, 60, TimeUnit.SECONDS);
        }
        
        private void monitorMemory() {
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
            
            double heapUtilization = (double) heapUsage.getUsed() / heapUsage.getMax() * 100;
            double nonHeapUtilization = (double) nonHeapUsage.getUsed() / nonHeapUsage.getMax() * 100;
            
            System.out.printf("[MEMORY] Heap: %.1f%% (%d MB), Non-Heap: %.1f%% (%d MB)%n",
                heapUtilization, heapUsage.getUsed() / 1024 / 1024,
                nonHeapUtilization, nonHeapUsage.getUsed() / 1024 / 1024);
            
            // Alert thresholds
            if (heapUtilization > 85) {
                alertHighMemoryUsage("Heap", heapUtilization);
            }
            if (nonHeapUtilization > 90) {
                alertHighMemoryUsage("Non-Heap", nonHeapUtilization);
            }
        }
        
        private void monitorGC() {
            for (GarbageCollectorMXBean gcBean : gcBeans) {
                long collections = gcBean.getCollectionCount();
                long time = gcBean.getCollectionTime();
                
                System.out.printf("[GC] %s: %d collections, %d ms total%n",
                    gcBean.getName(), collections, time);
                
                // Calculate GC frequency and duration
                if (collections > 0) {
                    double avgGCTime = (double) time / collections;
                    if (avgGCTime > 100) {
                        alertLongGCPause(gcBean.getName(), avgGCTime);
                    }
                }
            }
        }
        
        private void monitorThreads() {
            int threadCount = threadBean.getThreadCount();
            int daemonThreadCount = threadBean.getDaemonThreadCount();
            long totalStartedThreads = threadBean.getTotalStartedThreadCount();
            
            System.out.printf("[THREADS] Active: %d, Daemon: %d, Total Started: %d%n",
                threadCount, daemonThreadCount, totalStartedThreads);
            
            // Alert on high thread count
            if (threadCount > 1000) {
                alertHighThreadCount(threadCount);
            }
            
            // Detect deadlocks
            long[] deadlockedThreads = threadBean.findDeadlockedThreads();
            if (deadlockedThreads != null) {
                alertDeadlock(deadlockedThreads.length);
            }
        }
        
        private void alertHighMemoryUsage(String type, double utilization) {
            System.err.printf("🚨 ALERT: High %s memory usage: %.1f%%%n", type, utilization);
        }
        
        private void alertLongGCPause(String gcName, double avgTime) {
            System.err.printf("🚨 ALERT: Long GC pause in %s: %.1f ms average%n", gcName, avgTime);
        }
        
        private void alertHighThreadCount(int count) {
            System.err.printf("🚨 ALERT: High thread count: %d threads%n", count);
        }
        
        private void alertDeadlock(int threadCount) {
            System.err.printf("🚨 CRITICAL: Deadlock detected involving %d threads%n", threadCount);
        }
        
        public void stopMonitoring() {
            scheduler.shutdown();
            System.out.println("JVM Health Monitoring stopped");
        }
    }
    
    /**
     * Performance metrics collector
     */
    public static class PerformanceMetricsCollector {
        
        public static void collectAndExportMetrics() {
            System.out.println("=== Performance Metrics Export ===");
            
            // JVM metrics
            Runtime runtime = Runtime.getRuntime();
            System.out.printf("jvm.memory.used %d%n", runtime.totalMemory() - runtime.freeMemory());
            System.out.printf("jvm.memory.free %d%n", runtime.freeMemory());
            System.out.printf("jvm.memory.total %d%n", runtime.totalMemory());
            System.out.printf("jvm.memory.max %d%n", runtime.maxMemory());
            System.out.printf("jvm.processors %d%n", runtime.availableProcessors());
            
            // GC metrics
            for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
                String name = gcBean.getName().replaceAll("\\s+", "_").toLowerCase();
                System.out.printf("jvm.gc.%s.collections %d%n", name, gcBean.getCollectionCount());
                System.out.printf("jvm.gc.%s.time %d%n", name, gcBean.getCollectionTime());
            }
            
            // Thread metrics
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            System.out.printf("jvm.threads.count %d%n", threadBean.getThreadCount());
            System.out.printf("jvm.threads.daemon %d%n", threadBean.getDaemonThreadCount());
            System.out.printf("jvm.threads.peak %d%n", threadBean.getPeakThreadCount());
            System.out.printf("jvm.threads.started %d%n", threadBean.getTotalStartedThreadCount());
        }
    }
}
```

## 6. Practical Application

### Complete Tuning Example
```java
package com.javajourney.jvmtuning;

/**
 * Complete JVM tuning example for different application types
 */
public class JVMTuningExample {
    
    public static void main(String[] args) {
        System.out.println("=== JVM Tuning Examples ===");
        
        // Web application tuning
        demonstrateWebApplicationTuning();
        
        // Batch processing tuning
        demonstrateBatchProcessingTuning();
        
        // Microservice tuning
        demonstrateMicroserviceTuning();
        
        // Analysis and recommendations
        JVMTuningPhilosophy.TuningMethodology.demonstrateSystematicApproach();
    }
    
    private static void demonstrateWebApplicationTuning() {
        System.out.println("\n=== Web Application Tuning ===");
        
        ApplicationProfile profile = ApplicationProfile.BALANCED;
        HeapSizingStrategy.HeapRecommendations recommendations = 
            HeapSizingStrategy.HeapSizeCalculator.calculateOptimalHeapSize(
                profile, 2L * 1024 * 1024 * 1024, 1000, 0.2);
        
        recommendations.printRecommendations();
        
        String gcParams = GCTuningConfiguration.G1GCConfiguration
            .generateOptimizedG1Parameters(profile, 4096, 100);
        System.out.println("GC Parameters: " + gcParams);
        
        String jitParams = JITCompilerOptimization.JITOptimizationStrategies
            .generateThroughputOptimizedParameters();
        System.out.println("JIT Parameters: " + jitParams);
    }
    
    private static void demonstrateBatchProcessingTuning() {
        System.out.println("\n=== Batch Processing Tuning ===");
        
        ApplicationProfile profile = ApplicationProfile.HIGH_THROUGHPUT;
        HeapSizingStrategy.HeapRecommendations recommendations = 
            HeapSizingStrategy.HeapSizeCalculator.calculateOptimalHeapSize(
                profile, 8L * 1024 * 1024 * 1024, 10, 0.1);
        
        recommendations.printRecommendations();
        
        String gcParams = GCTuningConfiguration.ParallelGCConfiguration
            .generateOptimizedParallelParameters(profile, 16);
        System.out.println("GC Parameters: " + gcParams);
    }
    
    private static void demonstrateMicroserviceTuning() {
        System.out.println("\n=== Microservice Tuning ===");
        
        ApplicationProfile profile = ApplicationProfile.MEMORY_CONSTRAINED;
        HeapSizingStrategy.HeapRecommendations recommendations = 
            HeapSizingStrategy.HeapSizeCalculator.calculateOptimalHeapSize(
                profile, 512L * 1024 * 1024, 100, 0.3);
        
        recommendations.printRecommendations();
        
        String jitParams = JITCompilerOptimization.JITOptimizationStrategies
            .generateMemoryConstrainedParameters();
        System.out.println("JIT Parameters: " + jitParams);
    }
}
```

## Practice Exercises

### Exercise 1: GC Log Analysis
Create a GC log analyzer that identifies performance bottlenecks and suggests optimizations.

### Exercise 2: JVM Parameter Generator
Build a tool that generates optimal JVM parameters based on application characteristics.

### Exercise 3: Real-time Monitoring Dashboard
Implement a monitoring system that tracks JVM health metrics and alerts on issues.

### Exercise 4: Performance Regression Detection
Create a system that compares performance metrics over time and detects regressions.

## Key Takeaways

1. **Systematic Approach**: Always measure before optimizing and change one parameter at a time
2. **Application-Specific Tuning**: Different application profiles require different optimization strategies
3. **Monitoring is Essential**: Continuous monitoring helps maintain optimal performance
4. **GC Selection Matters**: Choose the right garbage collector for your workload characteristics
5. **JIT Optimization**: Understanding JIT compilation helps optimize both startup and steady-state performance
6. **Production Ready**: Always validate tuning changes in production-like environments

## Next Steps
Tomorrow we'll explore Java Security, covering authentication, authorization, and cryptography - essential topics for enterprise applications that build upon the performance foundations we've established today.