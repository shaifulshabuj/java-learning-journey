# Day 47: Load Testing & Performance Benchmarking (August 25, 2025)

**Date:** August 25, 2025  
**Duration:** 3 hours (Extended Session)  
**Focus:** Master JMH microbenchmarking, load testing strategies, and comprehensive performance analysis

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:

- ✅ Master JMH (Java Microbenchmark Harness) for precise performance measurement
- ✅ Implement comprehensive load testing strategies and frameworks
- ✅ Build performance regression detection and continuous benchmarking systems
- ✅ Create scalability testing scenarios with realistic load patterns
- ✅ Develop memory and CPU profiling techniques under load conditions
- ✅ Design enterprise-grade performance testing suites with monitoring integration

---

## 📚 Part 1: JMH Microbenchmarking Foundation (50 minutes)

### **JMH Architecture and Principles**

```
┌─────────────────────────────────────────────────────────────────┐
│                    JMH MICROBENCHMARKING                       │
├─────────────────────────────────────────────────────────────────┤
│ Measurement Strategies                                          │
│ ├─ Warmup Iterations    - JIT compiler optimization            │
│ ├─ Measurement Iterations - Actual performance data            │
│ ├─ Fork Isolation       - Separate JVM processes               │
│ └─ Dead Code Elimination - Preventing compiler optimizations   │
├─────────────────────────────────────────────────────────────────┤
│ Benchmark Modes                                                │
│ ├─ Throughput          - Operations per unit time              │
│ ├─ AverageTime         - Average execution time                │
│ ├─ SampleTime          - Sample execution times                │
│ ├─ SingleShotTime      - Single execution measurement          │
│ └─ All                 - All modes combined                    │
├─────────────────────────────────────────────────────────────────┤
│ Output & Analysis                                              │
│ ├─ Statistical Analysis - Mean, std dev, percentiles           │
│ ├─ Visual Reports      - Graphs and charts                    │
│ ├─ JSON/CSV Export     - Data analysis integration            │
│ └─ CI/CD Integration   - Automated regression detection        │
└─────────────────────────────────────────────────────────────────┘
```

### **JMH Best Practices**

- **Avoid Dead Code Elimination**: Use `Blackhole.consume()` for results
- **Proper State Management**: Use `@State` annotations correctly
- **Sufficient Warmup**: Allow JIT compiler to optimize code
- **Statistical Significance**: Run multiple forks and iterations
- **Realistic Scenarios**: Test with production-like data sizes

---

## 💻 Part 2: JMH Microbenchmark Implementation (45 minutes)

### **Exercise 1: Comprehensive JMH Benchmark Suite**

Create `ComprehensiveBenchmarkSuite.java`:

```java
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/**
 * Comprehensive JMH benchmark suite for performance analysis
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
public class ComprehensiveBenchmarkSuite {
    
    // Test data sizes
    @Param({"100", "1000", "10000", "100000"})
    private int dataSize;
    
    // Collection types for comparison
    private List<Integer> arrayList;
    private List<Integer> linkedList;
    private Set<Integer> hashSet;
    private Set<Integer> treeSet;
    private Map<Integer, String> hashMap;
    private Map<Integer, String> treeMap;
    
    // String data for text processing benchmarks
    private List<String> stringData;
    private String largeText;
    
    // Concurrent data structures
    private ConcurrentHashMap<Integer, String> concurrentHashMap;
    private CopyOnWriteArrayList<Integer> copyOnWriteList;
    
    @Setup(Level.Trial)
    public void setupTrial() {
        System.out.printf("🏗️  Setting up benchmark trial with data size: %d%n", dataSize);
        
        // Initialize collections
        arrayList = new ArrayList<>();
        linkedList = new LinkedList<>();
        hashSet = new HashSet<>();
        treeSet = new TreeSet<>();
        hashMap = new HashMap<>();
        treeMap = new TreeMap<>();
        concurrentHashMap = new ConcurrentHashMap<>();
        copyOnWriteList = new CopyOnWriteArrayList<>();
        stringData = new ArrayList<>();
        
        // Populate with test data
        Random random = new Random(42); // Fixed seed for reproducibility
        
        for (int i = 0; i < dataSize; i++) {
            Integer value = random.nextInt(dataSize * 2);
            String stringValue = "Value_" + value;
            
            arrayList.add(value);
            linkedList.add(value);
            hashSet.add(value);
            treeSet.add(value);
            hashMap.put(value, stringValue);
            treeMap.put(value, stringValue);
            concurrentHashMap.put(value, stringValue);
            copyOnWriteList.add(value);
            stringData.add(stringValue);
        }
        
        // Create large text for string processing
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("The quick brown fox jumps over the lazy dog. ");
        }
        largeText = sb.toString();
        
        // Memory usage info
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        System.out.printf("📊 Memory usage after setup: %.2f MB%n", usedMemory / (1024.0 * 1024.0));
    }
    
    @Setup(Level.Iteration)
    public void setupIteration() {
        // Force garbage collection between iterations for consistent measurements
        System.gc();
    }
    
    // Collection Performance Benchmarks
    
    @Benchmark
    public void arrayListAdd(Blackhole blackhole) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            list.add(i);
        }
        blackhole.consume(list);
    }
    
    @Benchmark
    public void linkedListAdd(Blackhole blackhole) {
        List<Integer> list = new LinkedList<>();
        for (int i = 0; i < 1000; i++) {
            list.add(i);
        }
        blackhole.consume(list);
    }
    
    @Benchmark
    public void arrayListGet(Blackhole blackhole) {
        Random random = new Random(42);
        for (int i = 0; i < 1000; i++) {
            int index = random.nextInt(arrayList.size());
            blackhole.consume(arrayList.get(index));
        }
    }
    
    @Benchmark
    public void linkedListGet(Blackhole blackhole) {
        Random random = new Random(42);
        for (int i = 0; i < Math.min(100, linkedList.size()); i++) { // Limit for LinkedList
            int index = random.nextInt(linkedList.size());
            blackhole.consume(linkedList.get(index));
        }
    }
    
    @Benchmark
    public void hashSetContains(Blackhole blackhole) {
        Random random = new Random(42);
        for (int i = 0; i < 1000; i++) {
            Integer value = random.nextInt(dataSize * 2);
            blackhole.consume(hashSet.contains(value));
        }
    }
    
    @Benchmark
    public void treeSetContains(Blackhole blackhole) {
        Random random = new Random(42);
        for (int i = 0; i < 1000; i++) {
            Integer value = random.nextInt(dataSize * 2);
            blackhole.consume(treeSet.contains(value));
        }
    }
    
    @Benchmark
    public void hashMapGet(Blackhole blackhole) {
        Random random = new Random(42);
        for (int i = 0; i < 1000; i++) {
            Integer key = random.nextInt(dataSize * 2);
            blackhole.consume(hashMap.get(key));
        }
    }
    
    @Benchmark
    public void treeMapGet(Blackhole blackhole) {
        Random random = new Random(42);
        for (int i = 0; i < 1000; i++) {
            Integer key = random.nextInt(dataSize * 2);
            blackhole.consume(treeMap.get(key));
        }
    }
    
    // Stream API Performance Benchmarks
    
    @Benchmark
    public void streamFilterMap(Blackhole blackhole) {
        List<String> result = arrayList.stream()
                .filter(x -> x % 2 == 0)
                .map(x -> "Number: " + x)
                .collect(Collectors.toList());
        blackhole.consume(result);
    }
    
    @Benchmark
    public void streamParallelFilterMap(Blackhole blackhole) {
        List<String> result = arrayList.parallelStream()
                .filter(x -> x % 2 == 0)
                .map(x -> "Number: " + x)
                .collect(Collectors.toList());
        blackhole.consume(result);
    }
    
    @Benchmark
    public void streamReduce(Blackhole blackhole) {
        int sum = arrayList.stream()
                .mapToInt(Integer::intValue)
                .reduce(0, Integer::sum);
        blackhole.consume(sum);
    }
    
    @Benchmark
    public void streamParallelReduce(Blackhole blackhole) {
        int sum = arrayList.parallelStream()
                .mapToInt(Integer::intValue)
                .reduce(0, Integer::sum);
        blackhole.consume(sum);
    }
    
    @Benchmark
    public void streamGrouping(Blackhole blackhole) {
        Map<Boolean, List<Integer>> groups = arrayList.stream()
                .collect(Collectors.groupingBy(x -> x % 2 == 0));
        blackhole.consume(groups);
    }
    
    // String Processing Benchmarks
    
    @Benchmark
    public void stringConcatenation(Blackhole blackhole) {
        String result = "";
        for (int i = 0; i < 100; i++) {
            result += "String " + i + " ";
        }
        blackhole.consume(result);
    }
    
    @Benchmark
    public void stringBuilderAppend(Blackhole blackhole) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("String ").append(i).append(" ");
        }
        blackhole.consume(sb.toString());
    }
    
    @Benchmark
    public void stringJoin(Blackhole blackhole) {
        List<String> parts = IntStream.range(0, 100)
                .mapToObj(i -> "String " + i)
                .collect(Collectors.toList());
        String result = String.join(" ", parts);
        blackhole.consume(result);
    }
    
    @Benchmark
    public void regularExpressionMatching(Blackhole blackhole) {
        String pattern = "\\b\\w{4}\\b"; // Words with 4 characters
        java.util.regex.Pattern compiledPattern = java.util.regex.Pattern.compile(pattern);
        
        int matches = 0;
        java.util.regex.Matcher matcher = compiledPattern.matcher(largeText);
        while (matcher.find()) {
            matches++;
        }
        blackhole.consume(matches);
    }
    
    @Benchmark
    public void stringReplace(Blackhole blackhole) {
        String result = largeText.replace("fox", "cat").replace("dog", "mouse");
        blackhole.consume(result);
    }
    
    // Concurrent Collection Benchmarks
    
    @Benchmark
    @Group("concurrent")
    @GroupThreads(4)
    public void concurrentHashMapPut(Blackhole blackhole) {
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            Integer key = random.nextInt(dataSize);
            String value = "ConcurrentValue_" + key;
            concurrentHashMap.put(key, value);
        }
        blackhole.consume(concurrentHashMap.size());
    }
    
    @Benchmark
    @Group("concurrent")
    @GroupThreads(4)
    public void concurrentHashMapGet(Blackhole blackhole) {
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            Integer key = random.nextInt(dataSize);
            blackhole.consume(concurrentHashMap.get(key));
        }
    }
    
    @Benchmark
    public void copyOnWriteListAdd(Blackhole blackhole) {
        CopyOnWriteArrayList<Integer> list = new CopyOnWriteArrayList<>();
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }
        blackhole.consume(list);
    }
    
    @Benchmark
    public void copyOnWriteListIterate(Blackhole blackhole) {
        int sum = 0;
        for (Integer value : copyOnWriteList) {
            sum += value;
        }
        blackhole.consume(sum);
    }
    
    // Algorithm Performance Benchmarks
    
    @Benchmark
    public void quickSort(Blackhole blackhole) {
        int[] array = arrayList.stream().mapToInt(Integer::intValue).toArray();
        quickSort(array, 0, array.length - 1);
        blackhole.consume(array);
    }
    
    @Benchmark
    public void mergeSort(Blackhole blackhole) {
        int[] array = arrayList.stream().mapToInt(Integer::intValue).toArray();
        mergeSort(array, 0, array.length - 1);
        blackhole.consume(array);
    }
    
    @Benchmark
    public void heapSort(Blackhole blackhole) {
        int[] array = arrayList.stream().mapToInt(Integer::intValue).toArray();
        heapSort(array);
        blackhole.consume(array);
    }
    
    @Benchmark
    public void binarySearch(Blackhole blackhole) {
        int[] sortedArray = arrayList.stream().mapToInt(Integer::intValue).sorted().toArray();
        Random random = new Random(42);
        
        for (int i = 0; i < 100; i++) {
            int target = random.nextInt(dataSize * 2);
            int result = Arrays.binarySearch(sortedArray, target);
            blackhole.consume(result);
        }
    }
    
    // Memory Allocation Benchmarks
    
    @Benchmark
    public void objectAllocation(Blackhole blackhole) {
        List<Object> objects = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            objects.add(new TestObject(i, "Object_" + i));
        }
        blackhole.consume(objects);
    }
    
    @Benchmark
    public void arrayAllocation(Blackhole blackhole) {
        for (int i = 0; i < 100; i++) {
            int[] array = new int[1000];
            Arrays.fill(array, i);
            blackhole.consume(array);
        }
    }
    
    // Helper methods for sorting algorithms
    private void quickSort(int[] arr, int low, int high) {
        if (low < high) {
            int pi = partition(arr, low, high);
            quickSort(arr, low, pi - 1);
            quickSort(arr, pi + 1, high);
        }
    }
    
    private int partition(int[] arr, int low, int high) {
        int pivot = arr[high];
        int i = (low - 1);
        
        for (int j = low; j < high; j++) {
            if (arr[j] <= pivot) {
                i++;
                int temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
            }
        }
        
        int temp = arr[i + 1];
        arr[i + 1] = arr[high];
        arr[high] = temp;
        
        return i + 1;
    }
    
    private void mergeSort(int[] arr, int left, int right) {
        if (left < right) {
            int mid = left + (right - left) / 2;
            
            mergeSort(arr, left, mid);
            mergeSort(arr, mid + 1, right);
            
            merge(arr, left, mid, right);
        }
    }
    
    private void merge(int[] arr, int left, int mid, int right) {
        int n1 = mid - left + 1;
        int n2 = right - mid;
        
        int[] leftArray = new int[n1];
        int[] rightArray = new int[n2];
        
        System.arraycopy(arr, left, leftArray, 0, n1);
        System.arraycopy(arr, mid + 1, rightArray, 0, n2);
        
        int i = 0, j = 0, k = left;
        
        while (i < n1 && j < n2) {
            if (leftArray[i] <= rightArray[j]) {
                arr[k] = leftArray[i];
                i++;
            } else {
                arr[k] = rightArray[j];
                j++;
            }
            k++;
        }
        
        while (i < n1) {
            arr[k] = leftArray[i];
            i++;
            k++;
        }
        
        while (j < n2) {
            arr[k] = rightArray[j];
            j++;
            k++;
        }
    }
    
    private void heapSort(int[] arr) {
        int n = arr.length;
        
        for (int i = n / 2 - 1; i >= 0; i--) {
            heapify(arr, n, i);
        }
        
        for (int i = n - 1; i > 0; i--) {
            int temp = arr[0];
            arr[0] = arr[i];
            arr[i] = temp;
            
            heapify(arr, i, 0);
        }
    }
    
    private void heapify(int[] arr, int n, int i) {
        int largest = i;
        int left = 2 * i + 1;
        int right = 2 * i + 2;
        
        if (left < n && arr[left] > arr[largest]) {
            largest = left;
        }
        
        if (right < n && arr[right] > arr[largest]) {
            largest = right;
        }
        
        if (largest != i) {
            int swap = arr[i];
            arr[i] = arr[largest];
            arr[largest] = swap;
            
            heapify(arr, n, largest);
        }
    }
    
    // Test object for allocation benchmarks
    private static class TestObject {
        private final int id;
        private final String name;
        private final long timestamp;
        
        public TestObject(int id, String name) {
            this.id = id;
            this.name = name;
            this.timestamp = System.currentTimeMillis();
        }
        
        public int getId() { return id; }
        public String getName() { return name; }
        public long getTimestamp() { return timestamp; }
    }
    
    // Main method to run benchmarks
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ComprehensiveBenchmarkSuite.class.getSimpleName())
                .forks(1) // Reduce forks for demo
                .warmupIterations(2)
                .measurementIterations(3)
                .addProfiler("gc") // Add GC profiler
                .addProfiler("stack") // Add stack profiler
                .result("benchmark-results.json")
                .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.JSON)
                .build();
        
        new Runner(opt).run();
    }
}
```

### **Exercise 2: Performance Regression Detection**

Create `PerformanceRegressionDetector.java`:

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Performance regression detection system for continuous benchmarking
 */
public class PerformanceRegressionDetector {
    
    private final ObjectMapper objectMapper;
    private final double regressionThreshold;
    private final double improvementThreshold;
    
    public PerformanceRegressionDetector(double regressionThreshold, double improvementThreshold) {
        this.objectMapper = new ObjectMapper();
        this.regressionThreshold = regressionThreshold; // e.g., 0.15 for 15% regression
        this.improvementThreshold = improvementThreshold; // e.g., 0.10 for 10% improvement
    }
    
    /**
     * Compare two benchmark result files and detect regressions
     */
    public RegressionAnalysisReport analyzeBenchmarkResults(String baselineFile, String currentFile) 
            throws IOException {
        
        System.out.println("📊 Analyzing benchmark results for performance regressions...");
        
        Map<String, BenchmarkResult> baselineResults = parseBenchmarkResults(baselineFile);
        Map<String, BenchmarkResult> currentResults = parseBenchmarkResults(currentFile);
        
        List<PerformanceChange> changes = new ArrayList<>();
        
        for (String benchmarkName : baselineResults.keySet()) {
            if (currentResults.containsKey(benchmarkName)) {
                BenchmarkResult baseline = baselineResults.get(benchmarkName);
                BenchmarkResult current = currentResults.get(benchmarkName);
                
                PerformanceChange change = analyzePerformanceChange(benchmarkName, baseline, current);
                if (change != null) {
                    changes.add(change);
                }
            } else {
                System.out.printf("⚠️  Benchmark '%s' missing in current results%n", benchmarkName);
            }
        }
        
        // Check for new benchmarks
        for (String benchmarkName : currentResults.keySet()) {
            if (!baselineResults.containsKey(benchmarkName)) {
                System.out.printf("🆕 New benchmark detected: '%s'%n", benchmarkName);
            }
        }
        
        return new RegressionAnalysisReport(
            baselineFile,
            currentFile,
            changes,
            LocalDateTime.now()
        );
    }
    
    /**
     * Parse JMH JSON benchmark results
     */
    private Map<String, BenchmarkResult> parseBenchmarkResults(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            throw new IOException("Benchmark results file not found: " + filename);
        }
        
        JsonNode rootNode = objectMapper.readTree(file);
        Map<String, BenchmarkResult> results = new HashMap<>();
        
        if (rootNode.isArray()) {
            for (JsonNode benchmarkNode : rootNode) {
                String benchmarkName = benchmarkNode.get("benchmark").asText();
                String mode = benchmarkNode.get("mode").asText();
                
                JsonNode primaryMetric = benchmarkNode.get("primaryMetric");
                double score = primaryMetric.get("score").asDouble();
                double error = primaryMetric.get("scoreError").asDouble();
                String unit = primaryMetric.get("scoreUnit").asText();
                
                // Extract confidence intervals
                JsonNode scoreConfidence = primaryMetric.get("scoreConfidence");
                double lowerBound = scoreConfidence.get(0).asDouble();
                double upperBound = scoreConfidence.get(1).asDouble();
                
                // Extract raw data if available
                List<Double> rawData = new ArrayList<>();
                JsonNode rawDataNode = primaryMetric.get("rawData");
                if (rawDataNode != null && rawDataNode.isArray()) {
                    for (JsonNode dataPoint : rawDataNode) {
                        if (dataPoint.isArray() && dataPoint.size() > 0) {
                            rawData.add(dataPoint.get(0).asDouble());
                        }
                    }
                }
                
                BenchmarkResult result = new BenchmarkResult(
                    benchmarkName,
                    mode,
                    score,
                    error,
                    unit,
                    lowerBound,
                    upperBound,
                    rawData
                );
                
                results.put(benchmarkName, result);
            }
        }
        
        System.out.printf("📈 Parsed %d benchmark results from %s%n", results.size(), filename);
        return results;
    }
    
    /**
     * Analyze performance change between baseline and current results
     */
    private PerformanceChange analyzePerformanceChange(String benchmarkName, 
                                                      BenchmarkResult baseline, 
                                                      BenchmarkResult current) {
        
        if (!baseline.unit.equals(current.unit)) {
            System.out.printf("⚠️  Unit mismatch for %s: %s vs %s%n", 
                             benchmarkName, baseline.unit, current.unit);
            return null;
        }
        
        double baselineScore = baseline.score;
        double currentScore = current.score;
        
        // Calculate percentage change
        double percentageChange;
        ChangeType changeType;
        
        if (baseline.mode.equals("thrpt")) { // Throughput - higher is better
            percentageChange = (currentScore - baselineScore) / baselineScore;
            if (percentageChange < -regressionThreshold) {
                changeType = ChangeType.REGRESSION;
            } else if (percentageChange > improvementThreshold) {
                changeType = ChangeType.IMPROVEMENT;
            } else {
                changeType = ChangeType.STABLE;
            }
        } else { // Time-based metrics - lower is better
            percentageChange = (baselineScore - currentScore) / baselineScore;
            if (percentageChange < -regressionThreshold) {
                changeType = ChangeType.REGRESSION;
            } else if (percentageChange > improvementThreshold) {
                changeType = ChangeType.IMPROVEMENT;
            } else {
                changeType = ChangeType.STABLE;
            }
        }
        
        // Statistical significance test
        boolean isStatisticallySignificant = isStatisticallySignificant(baseline, current);
        
        return new PerformanceChange(
            benchmarkName,
            baseline,
            current,
            percentageChange,
            changeType,
            isStatisticallySignificant
        );
    }
    
    /**
     * Simple statistical significance test using confidence intervals
     */
    private boolean isStatisticallySignificant(BenchmarkResult baseline, BenchmarkResult current) {
        // Check if confidence intervals overlap
        boolean baselineOverlapsCurrent = baseline.lowerBound <= current.upperBound && 
                                         baseline.upperBound >= current.lowerBound;
        
        return !baselineOverlapsCurrent; // No overlap means statistically significant
    }
    
    /**
     * Generate comprehensive regression report
     */
    public void generateReport(RegressionAnalysisReport report) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 PERFORMANCE REGRESSION ANALYSIS REPORT");
        System.out.println("=".repeat(80));
        System.out.printf("Baseline: %s%n", report.baselineFile);
        System.out.printf("Current:  %s%n", report.currentFile);
        System.out.printf("Analysis Time: %s%n", 
                         report.analysisTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        // Categorize changes
        Map<ChangeType, List<PerformanceChange>> changesByType = report.changes.stream()
                .collect(Collectors.groupingBy(change -> change.changeType));
        
        // Summary statistics
        int totalBenchmarks = report.changes.size();
        int regressions = changesByType.getOrDefault(ChangeType.REGRESSION, Collections.emptyList()).size();
        int improvements = changesByType.getOrDefault(ChangeType.IMPROVEMENT, Collections.emptyList()).size();
        int stable = changesByType.getOrDefault(ChangeType.STABLE, Collections.emptyList()).size();
        
        System.out.printf("\n📈 SUMMARY:%n");
        System.out.printf("   Total Benchmarks: %d%n", totalBenchmarks);
        System.out.printf("   Regressions: %d (%.1f%%)%n", regressions, (double) regressions / totalBenchmarks * 100);
        System.out.printf("   Improvements: %d (%.1f%%)%n", improvements, (double) improvements / totalBenchmarks * 100);
        System.out.printf("   Stable: %d (%.1f%%)%n", stable, (double) stable / totalBenchmarks * 100);
        
        // Detailed regression analysis
        if (!changesByType.getOrDefault(ChangeType.REGRESSION, Collections.emptyList()).isEmpty()) {
            System.out.printf("\n🚨 PERFORMANCE REGRESSIONS:%n");
            changesByType.get(ChangeType.REGRESSION).stream()
                        .sorted(Comparator.comparingDouble(c -> Math.abs(c.percentageChange)))
                        .forEach(this::printChangeDetails);
        }
        
        // Improvements
        if (!changesByType.getOrDefault(ChangeType.IMPROVEMENT, Collections.emptyList()).isEmpty()) {
            System.out.printf("\n🚀 PERFORMANCE IMPROVEMENTS:%n");
            changesByType.get(ChangeType.IMPROVEMENT).stream()
                        .sorted(Comparator.comparingDouble(c -> Math.abs(c.percentageChange)))
                        .forEach(this::printChangeDetails);
        }
        
        // Recommendations
        generateRecommendations(report);
    }
    
    private void printChangeDetails(PerformanceChange change) {
        String significance = change.isStatisticallySignificant ? "✓" : "?";
        String emoji = switch (change.changeType) {
            case REGRESSION -> "🔴";
            case IMPROVEMENT -> "🟢";
            case STABLE -> "🟡";
        };
        
        System.out.printf("   %s %s [%s] %s%n", emoji, significance, 
                         String.format("%+.1f%%", change.percentageChange * 100), 
                         change.benchmarkName);
        System.out.printf("      Baseline: %.3f ± %.3f %s%n", 
                         change.baseline.score, change.baseline.error, change.baseline.unit);
        System.out.printf("      Current:  %.3f ± %.3f %s%n", 
                         change.current.score, change.current.error, change.current.unit);
    }
    
    private void generateRecommendations(RegressionAnalysisReport report) {
        System.out.printf("\n💡 RECOMMENDATIONS:%n");
        
        List<PerformanceChange> significantRegressions = report.changes.stream()
                .filter(c -> c.changeType == ChangeType.REGRESSION && c.isStatisticallySignificant)
                .toList();
        
        if (!significantRegressions.isEmpty()) {
            System.out.println("   🔍 Investigate the following significant regressions:");
            significantRegressions.stream()
                                .limit(5)
                                .forEach(change -> System.out.printf("      - %s (%.1f%% regression)%n", 
                                                                   change.benchmarkName, 
                                                                   Math.abs(change.percentageChange * 100)));
            
            System.out.println("   📋 Recommended actions:");
            System.out.println("      - Review recent code changes");
            System.out.println("      - Check for algorithmic complexity increases");
            System.out.println("      - Analyze memory allocation patterns");
            System.out.println("      - Profile CPU and memory usage");
            System.out.println("      - Consider JVM tuning parameters");
        } else {
            System.out.println("   ✅ No significant performance regressions detected");
        }
        
        List<PerformanceChange> improvements = report.changes.stream()
                .filter(c -> c.changeType == ChangeType.IMPROVEMENT && c.isStatisticallySignificant)
                .toList();
        
        if (!improvements.isEmpty()) {
            System.out.printf("   🎉 %d significant performance improvements detected%n", improvements.size());
        }
    }
    
    /**
     * Export report to JSON for further analysis
     */
    public void exportToJson(RegressionAnalysisReport report, String filename) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), report);
        System.out.printf("📄 Report exported to: %s%n", filename);
    }
    
    // Data classes
    public enum ChangeType {
        REGRESSION, IMPROVEMENT, STABLE
    }
    
    public static class BenchmarkResult {
        public final String name;
        public final String mode;
        public final double score;
        public final double error;
        public final String unit;
        public final double lowerBound;
        public final double upperBound;
        public final List<Double> rawData;
        
        public BenchmarkResult(String name, String mode, double score, double error, String unit,
                             double lowerBound, double upperBound, List<Double> rawData) {
            this.name = name;
            this.mode = mode;
            this.score = score;
            this.error = error;
            this.unit = unit;
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            this.rawData = rawData;
        }
    }
    
    public static class PerformanceChange {
        public final String benchmarkName;
        public final BenchmarkResult baseline;
        public final BenchmarkResult current;
        public final double percentageChange;
        public final ChangeType changeType;
        public final boolean isStatisticallySignificant;
        
        public PerformanceChange(String benchmarkName, BenchmarkResult baseline, BenchmarkResult current,
                               double percentageChange, ChangeType changeType, boolean isStatisticallySignificant) {
            this.benchmarkName = benchmarkName;
            this.baseline = baseline;
            this.current = current;
            this.percentageChange = percentageChange;
            this.changeType = changeType;
            this.isStatisticallySignificant = isStatisticallySignificant;
        }
    }
    
    public static class RegressionAnalysisReport {
        public final String baselineFile;
        public final String currentFile;
        public final List<PerformanceChange> changes;
        public final LocalDateTime analysisTime;
        
        public RegressionAnalysisReport(String baselineFile, String currentFile, 
                                      List<PerformanceChange> changes, LocalDateTime analysisTime) {
            this.baselineFile = baselineFile;
            this.currentFile = currentFile;
            this.changes = changes;
            this.analysisTime = analysisTime;
        }
    }
    
    /**
     * Demo application
     */
    public static void main(String[] args) {
        PerformanceRegressionDetector detector = new PerformanceRegressionDetector(0.15, 0.10);
        
        try {
            // Simulate comparing benchmark results
            String baselineFile = "baseline-results.json";
            String currentFile = "current-results.json";
            
            // Create sample data files for demonstration
            createSampleBenchmarkResults();
            
            RegressionAnalysisReport report = detector.analyzeBenchmarkResults(baselineFile, currentFile);
            detector.generateReport(report);
            detector.exportToJson(report, "regression-analysis-report.json");
            
        } catch (IOException e) {
            System.err.printf("Error analyzing benchmark results: %s%n", e.getMessage());
        }
    }
    
    private static void createSampleBenchmarkResults() throws IOException {
        // This would normally be created by JMH - creating sample data for demo
        ObjectMapper mapper = new ObjectMapper();
        
        // Sample baseline results
        String baselineJson = """
            [
                {
                    "benchmark": "ArrayListAdd",
                    "mode": "thrpt",
                    "primaryMetric": {
                        "score": 1000.0,
                        "scoreError": 50.0,
                        "scoreUnit": "ops/ms",
                        "scoreConfidence": [950.0, 1050.0],
                        "rawData": [[1000.0], [1020.0], [980.0]]
                    }
                }
            ]
            """;
        
        // Sample current results with regression
        String currentJson = """
            [
                {
                    "benchmark": "ArrayListAdd",
                    "mode": "thrpt",
                    "primaryMetric": {
                        "score": 800.0,
                        "scoreError": 40.0,
                        "scoreUnit": "ops/ms",
                        "scoreConfidence": [760.0, 840.0],
                        "rawData": [[800.0], [820.0], [780.0]]
                    }
                }
            ]
            """;
        
        mapper.writeValue(new File("baseline-results.json"), mapper.readTree(baselineJson));
        mapper.writeValue(new File("current-results.json"), mapper.readTree(currentJson));
    }
}
```

---

## 🔧 Part 3: Load Testing Framework (60 minutes)

### **Exercise 3: Comprehensive Load Testing Suite**

Create `LoadTestingSuite.java`:

```java
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;

/**
 * Comprehensive load testing framework with performance monitoring
 */
public class LoadTestingSuite {
    
    private final HttpClient httpClient;
    private final LoadTestMetrics metrics;
    private final ScheduledExecutorService monitoringExecutor;
    
    public LoadTestingSuite() {
        this.httpClient = HttpClient.newBuilder()
                                   .connectTimeout(Duration.ofSeconds(30))
                                   .build();
        this.metrics = new LoadTestMetrics();
        this.monitoringExecutor = Executors.newScheduledThreadPool(2);
    }
    
    /**
     * Execute load test with specified configuration
     */
    public LoadTestResult executeLoadTest(LoadTestConfiguration config) {
        System.out.printf("🚀 Starting load test: %s%n", config.testName);
        System.out.printf("   Target URL: %s%n", config.targetUrl);
        System.out.printf("   Concurrent Users: %d%n", config.concurrentUsers);
        System.out.printf("   Duration: %d seconds%n", config.durationSeconds);
        System.out.printf("   Ramp-up Time: %d seconds%n", config.rampUpSeconds);
        
        // Start monitoring
        startPerformanceMonitoring();
        
        ExecutorService loadTestExecutor = Executors.newFixedThreadPool(config.concurrentUsers);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(config.concurrentUsers);
        
        AtomicBoolean testRunning = new AtomicBoolean(true);
        long testStartTime = System.currentTimeMillis();
        
        // Start load test threads
        for (int i = 0; i < config.concurrentUsers; i++) {
            final int userId = i;
            loadTestExecutor.submit(() -> 
                executeUserSession(config, userId, startLatch, endLatch, testRunning));
        }
        
        // Start the test
        startLatch.countDown();
        
        // Run for specified duration
        try {
            Thread.sleep(config.durationSeconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Stop the test
        testRunning.set(false);
        
        // Wait for all threads to complete
        try {
            endLatch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long testEndTime = System.currentTimeMillis();
        loadTestExecutor.shutdown();
        
        // Stop monitoring
        stopPerformanceMonitoring();
        
        // Calculate results
        return calculateResults(config, testStartTime, testEndTime);
    }
    
    /**
     * Execute individual user session
     */
    private void executeUserSession(LoadTestConfiguration config, int userId, 
                                   CountDownLatch startLatch, CountDownLatch endLatch, 
                                   AtomicBoolean testRunning) {
        try {
            // Wait for test to start
            startLatch.await();
            
            // Ramp-up delay
            if (config.rampUpSeconds > 0) {
                long rampUpDelay = (long) (config.rampUpSeconds * 1000.0 * userId / config.concurrentUsers);
                Thread.sleep(rampUpDelay);
            }
            
            Random random = new Random(userId); // Deterministic randomness per user
            
            while (testRunning.get()) {
                try {
                    // Execute user scenario
                    executeUserScenario(config, userId, random);
                    
                    // Think time between requests
                    if (config.thinkTimeMs > 0) {
                        Thread.sleep(config.thinkTimeMs + random.nextInt(config.thinkTimeMs));
                    }
                    
                } catch (Exception e) {
                    metrics.recordError(e.getClass().getSimpleName());
                }
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            endLatch.countDown();
        }
    }
    
    /**
     * Execute user scenario (sequence of requests)
     */
    private void executeUserScenario(LoadTestConfiguration config, int userId, Random random) 
            throws Exception {
        
        for (HttpRequestTemplate template : config.requestTemplates) {
            long requestStart = System.nanoTime();
            
            try {
                HttpRequest request = buildRequest(template, userId, random);
                HttpResponse<String> response = httpClient.send(request, 
                        HttpResponse.BodyHandlers.ofString());
                
                long responseTime = System.nanoTime() - requestStart;
                
                metrics.recordRequest(
                    template.name,
                    responseTime / 1_000_000, // Convert to milliseconds
                    response.statusCode(),
                    response.body().length()
                );
                
                // Validate response if needed
                if (template.responseValidator != null) {
                    boolean valid = template.responseValidator.test(response);
                    if (!valid) {
                        metrics.recordError("ValidationFailure");
                    }
                }
                
            } catch (Exception e) {
                long responseTime = System.nanoTime() - requestStart;
                metrics.recordRequest(
                    template.name,
                    responseTime / 1_000_000,
                    0, // Error status
                    0
                );
                throw e;
            }
        }
    }
    
    /**
     * Build HTTP request from template
     */
    private HttpRequest buildRequest(HttpRequestTemplate template, int userId, Random random) {
        String url = template.url;
        
        // Replace placeholders
        url = url.replace("{userId}", String.valueOf(userId));
        url = url.replace("{random}", String.valueOf(random.nextInt(10000)));
        url = url.replace("{timestamp}", String.valueOf(System.currentTimeMillis()));
        
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30));
        
        // Add headers
        template.headers.forEach(builder::header);
        
        // Set method and body
        switch (template.method.toUpperCase()) {
            case "GET":
                builder.GET();
                break;
            case "POST":
                builder.POST(HttpRequest.BodyPublishers.ofString(template.body != null ? template.body : ""));
                break;
            case "PUT":
                builder.PUT(HttpRequest.BodyPublishers.ofString(template.body != null ? template.body : ""));
                break;
            case "DELETE":
                builder.DELETE();
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + template.method);
        }
        
        return builder.build();
    }
    
    /**
     * Start performance monitoring during load test
     */
    private void startPerformanceMonitoring() {
        monitoringExecutor.scheduleAtFixedRate(() -> {
            try {
                collectSystemMetrics();
            } catch (Exception e) {
                System.err.printf("Error collecting system metrics: %s%n", e.getMessage());
            }
        }, 1, 5, TimeUnit.SECONDS);
    }
    
    /**
     * Stop performance monitoring
     */
    private void stopPerformanceMonitoring() {
        monitoringExecutor.shutdown();
        try {
            if (!monitoringExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                monitoringExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            monitoringExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Collect system performance metrics
     */
    private void collectSystemMetrics() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        int threadCount = threadBean.getThreadCount();
        
        metrics.recordSystemMetrics(heapUsed, heapMax, threadCount);
    }
    
    /**
     * Calculate load test results
     */
    private LoadTestResult calculateResults(LoadTestConfiguration config, long startTime, long endTime) {
        long totalDuration = endTime - startTime;
        
        LoadTestResult result = new LoadTestResult(
            config.testName,
            config.concurrentUsers,
            totalDuration,
            metrics.getTotalRequests(),
            metrics.getSuccessfulRequests(),
            metrics.getErrorCount(),
            metrics.getAverageResponseTime(),
            metrics.getMedianResponseTime(),
            metrics.getP95ResponseTime(),
            metrics.getP99ResponseTime(),
            metrics.getMaxResponseTime(),
            metrics.getThroughput(totalDuration),
            metrics.getErrorRate(),
            metrics.getRequestsByEndpoint(),
            metrics.getErrorsByType(),
            metrics.getSystemMetricsHistory()
        );
        
        return result;
    }
    
    /**
     * Generate comprehensive load test report
     */
    public void generateReport(LoadTestResult result) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 LOAD TEST RESULTS REPORT");
        System.out.println("=".repeat(80));
        System.out.printf("Test Name: %s%n", result.testName);
        System.out.printf("Concurrent Users: %d%n", result.concurrentUsers);
        System.out.printf("Test Duration: %.1f seconds%n", result.totalDuration / 1000.0);
        
        System.out.println("\n📈 REQUEST STATISTICS:");
        System.out.printf("   Total Requests: %,d%n", result.totalRequests);
        System.out.printf("   Successful: %,d (%.1f%%)%n", 
                         result.successfulRequests, 
                         (double) result.successfulRequests / result.totalRequests * 100);
        System.out.printf("   Errors: %,d (%.1f%%)%n", 
                         result.errorCount, result.errorRate * 100);
        System.out.printf("   Throughput: %.1f req/sec%n", result.throughput);
        
        System.out.println("\n⏱️  RESPONSE TIME ANALYSIS:");
        System.out.printf("   Average: %.1f ms%n", result.averageResponseTime);
        System.out.printf("   Median:  %.1f ms%n", result.medianResponseTime);
        System.out.printf("   95th percentile: %.1f ms%n", result.p95ResponseTime);
        System.out.printf("   99th percentile: %.1f ms%n", result.p99ResponseTime);
        System.out.printf("   Maximum: %.1f ms%n", result.maxResponseTime);
        
        if (!result.requestsByEndpoint.isEmpty()) {
            System.out.println("\n🔗 REQUESTS BY ENDPOINT:");
            result.requestsByEndpoint.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(entry -> System.out.printf("   %-30s: %,d requests%n", 
                                                       entry.getKey(), entry.getValue()));
        }
        
        if (!result.errorsByType.isEmpty()) {
            System.out.println("\n❌ ERRORS BY TYPE:");
            result.errorsByType.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(entry -> System.out.printf("   %-30s: %,d errors%n", 
                                                       entry.getKey(), entry.getValue()));
        }
        
        // System resource analysis
        if (!result.systemMetricsHistory.isEmpty()) {
            System.out.println("\n💻 SYSTEM RESOURCE USAGE:");
            
            DoubleSummaryStatistics memoryStats = result.systemMetricsHistory.stream()
                    .mapToDouble(m -> (double) m.heapUsed / m.heapMax * 100)
                    .summaryStatistics();
            
            IntSummaryStatistics threadStats = result.systemMetricsHistory.stream()
                    .mapToInt(m -> m.threadCount)
                    .summaryStatistics();
            
            System.out.printf("   Memory Usage: %.1f%% avg, %.1f%% max%n", 
                             memoryStats.getAverage(), memoryStats.getMax());
            System.out.printf("   Thread Count: %.0f avg, %d max%n", 
                             threadStats.getAverage(), threadStats.getMax());
        }
        
        // Performance assessment
        assessPerformance(result);
    }
    
    /**
     * Assess performance and provide recommendations
     */
    private void assessPerformance(LoadTestResult result) {
        System.out.println("\n💡 PERFORMANCE ASSESSMENT:");
        
        List<String> issues = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        
        // Error rate assessment
        if (result.errorRate > 0.05) { // 5%
            issues.add(String.format("High error rate: %.1f%%", result.errorRate * 100));
            recommendations.add("Investigate error causes and improve error handling");
        }
        
        // Response time assessment
        if (result.p95ResponseTime > 2000) { // 2 seconds
            issues.add(String.format("High P95 response time: %.1f ms", result.p95ResponseTime));
            recommendations.add("Optimize slow endpoints and database queries");
        }
        
        // Throughput assessment
        double expectedThroughput = result.concurrentUsers * 0.5; // Expected 0.5 req/sec per user
        if (result.throughput < expectedThroughput) {
            issues.add(String.format("Low throughput: %.1f req/sec (expected: %.1f)", 
                                   result.throughput, expectedThroughput));
            recommendations.add("Investigate bottlenecks and optimize resource utilization");
        }
        
        if (issues.isEmpty()) {
            System.out.println("   ✅ Performance appears to be within acceptable limits");
        } else {
            System.out.println("   ⚠️  Performance Issues Identified:");
            issues.forEach(issue -> System.out.println("      - " + issue));
            
            System.out.println("\n   📋 Recommendations:");
            recommendations.forEach(rec -> System.out.println("      - " + rec));
        }
    }
    
    /**
     * Load test configuration builder
     */
    public static class LoadTestConfigurationBuilder {
        private String testName;
        private String targetUrl;
        private int concurrentUsers = 10;
        private int durationSeconds = 60;
        private int rampUpSeconds = 10;
        private int thinkTimeMs = 1000;
        private List<HttpRequestTemplate> requestTemplates = new ArrayList<>();
        
        public LoadTestConfigurationBuilder testName(String testName) {
            this.testName = testName;
            return this;
        }
        
        public LoadTestConfigurationBuilder targetUrl(String targetUrl) {
            this.targetUrl = targetUrl;
            return this;
        }
        
        public LoadTestConfigurationBuilder concurrentUsers(int concurrentUsers) {
            this.concurrentUsers = concurrentUsers;
            return this;
        }
        
        public LoadTestConfigurationBuilder duration(int seconds) {
            this.durationSeconds = seconds;
            return this;
        }
        
        public LoadTestConfigurationBuilder rampUp(int seconds) {
            this.rampUpSeconds = seconds;
            return this;
        }
        
        public LoadTestConfigurationBuilder thinkTime(int milliseconds) {
            this.thinkTimeMs = milliseconds;
            return this;
        }
        
        public LoadTestConfigurationBuilder addRequest(HttpRequestTemplate template) {
            this.requestTemplates.add(template);
            return this;
        }
        
        public LoadTestConfiguration build() {
            if (requestTemplates.isEmpty()) {
                // Add default GET request
                requestTemplates.add(new HttpRequestTemplate("default", "GET", targetUrl));
            }
            
            return new LoadTestConfiguration(
                testName, targetUrl, concurrentUsers, durationSeconds, 
                rampUpSeconds, thinkTimeMs, requestTemplates
            );
        }
    }
    
    // Data classes
    public static class LoadTestConfiguration {
        public final String testName;
        public final String targetUrl;
        public final int concurrentUsers;
        public final int durationSeconds;
        public final int rampUpSeconds;
        public final int thinkTimeMs;
        public final List<HttpRequestTemplate> requestTemplates;
        
        public LoadTestConfiguration(String testName, String targetUrl, int concurrentUsers,
                                   int durationSeconds, int rampUpSeconds, int thinkTimeMs,
                                   List<HttpRequestTemplate> requestTemplates) {
            this.testName = testName;
            this.targetUrl = targetUrl;
            this.concurrentUsers = concurrentUsers;
            this.durationSeconds = durationSeconds;
            this.rampUpSeconds = rampUpSeconds;
            this.thinkTimeMs = thinkTimeMs;
            this.requestTemplates = requestTemplates;
        }
    }
    
    public static class HttpRequestTemplate {
        public final String name;
        public final String method;
        public final String url;
        public final Map<String, String> headers;
        public final String body;
        public final java.util.function.Predicate<HttpResponse<String>> responseValidator;
        
        public HttpRequestTemplate(String name, String method, String url) {
            this(name, method, url, new HashMap<>(), null, null);
        }
        
        public HttpRequestTemplate(String name, String method, String url, 
                                 Map<String, String> headers, String body,
                                 java.util.function.Predicate<HttpResponse<String>> responseValidator) {
            this.name = name;
            this.method = method;
            this.url = url;
            this.headers = headers;
            this.body = body;
            this.responseValidator = responseValidator;
        }
    }
    
    public static class LoadTestResult {
        public final String testName;
        public final int concurrentUsers;
        public final long totalDuration;
        public final long totalRequests;
        public final long successfulRequests;
        public final long errorCount;
        public final double averageResponseTime;
        public final double medianResponseTime;
        public final double p95ResponseTime;
        public final double p99ResponseTime;
        public final double maxResponseTime;
        public final double throughput;
        public final double errorRate;
        public final Map<String, Long> requestsByEndpoint;
        public final Map<String, Long> errorsByType;
        public final List<SystemMetrics> systemMetricsHistory;
        
        public LoadTestResult(String testName, int concurrentUsers, long totalDuration,
                            long totalRequests, long successfulRequests, long errorCount,
                            double averageResponseTime, double medianResponseTime,
                            double p95ResponseTime, double p99ResponseTime, double maxResponseTime,
                            double throughput, double errorRate,
                            Map<String, Long> requestsByEndpoint, Map<String, Long> errorsByType,
                            List<SystemMetrics> systemMetricsHistory) {
            this.testName = testName;
            this.concurrentUsers = concurrentUsers;
            this.totalDuration = totalDuration;
            this.totalRequests = totalRequests;
            this.successfulRequests = successfulRequests;
            this.errorCount = errorCount;
            this.averageResponseTime = averageResponseTime;
            this.medianResponseTime = medianResponseTime;
            this.p95ResponseTime = p95ResponseTime;
            this.p99ResponseTime = p99ResponseTime;
            this.maxResponseTime = maxResponseTime;
            this.throughput = throughput;
            this.errorRate = errorRate;
            this.requestsByEndpoint = requestsByEndpoint;
            this.errorsByType = errorsByType;
            this.systemMetricsHistory = systemMetricsHistory;
        }
    }
    
    public static class SystemMetrics {
        public final long timestamp;
        public final long heapUsed;
        public final long heapMax;
        public final int threadCount;
        
        public SystemMetrics(long timestamp, long heapUsed, long heapMax, int threadCount) {
            this.timestamp = timestamp;
            this.heapUsed = heapUsed;
            this.heapMax = heapMax;
            this.threadCount = threadCount;
        }
    }
    
    /**
     * Demo application
     */
    public static void main(String[] args) {
        LoadTestingSuite loadTester = new LoadTestingSuite();
        
        // Configure load test
        LoadTestConfiguration config = new LoadTestConfigurationBuilder()
                .testName("API Load Test")
                .targetUrl("https://httpbin.org/delay/1") // Simulated slow endpoint
                .concurrentUsers(20)
                .duration(30)
                .rampUp(5)
                .thinkTime(500)
                .addRequest(new HttpRequestTemplate("get-delay", "GET", "https://httpbin.org/delay/1"))
                .addRequest(new HttpRequestTemplate("get-json", "GET", "https://httpbin.org/json"))
                .build();
        
        // Execute load test
        LoadTestResult result = loadTester.executeLoadTest(config);
        
        // Generate report
        loadTester.generateReport(result);
        
        System.out.println("\n✅ Load test completed successfully!");
    }
}

/**
 * Load test metrics collector
 */
class LoadTestMetrics {
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final ConcurrentHashMap<String, AtomicLong> errorsByType = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> requestsByEndpoint = new ConcurrentHashMap<>();
    private final List<Double> responseTimes = Collections.synchronizedList(new ArrayList<>());
    private final List<LoadTestingSuite.SystemMetrics> systemMetrics = Collections.synchronizedList(new ArrayList<>());
    
    public void recordRequest(String endpoint, double responseTimeMs, int statusCode, int responseSize) {
        totalRequests.incrementAndGet();
        requestsByEndpoint.computeIfAbsent(endpoint, k -> new AtomicLong(0)).incrementAndGet();
        responseTimes.add(responseTimeMs);
        
        if (statusCode >= 200 && statusCode < 400) {
            successfulRequests.incrementAndGet();
        } else {
            errorsByType.computeIfAbsent("HTTP_" + statusCode, k -> new AtomicLong(0)).incrementAndGet();
        }
    }
    
    public void recordError(String errorType) {
        errorsByType.computeIfAbsent(errorType, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    public void recordSystemMetrics(long heapUsed, long heapMax, int threadCount) {
        systemMetrics.add(new LoadTestingSuite.SystemMetrics(
            System.currentTimeMillis(), heapUsed, heapMax, threadCount
        ));
    }
    
    public long getTotalRequests() { return totalRequests.get(); }
    public long getSuccessfulRequests() { return successfulRequests.get(); }
    public long getErrorCount() { return totalRequests.get() - successfulRequests.get(); }
    
    public double getAverageResponseTime() {
        return responseTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
    
    public double getMedianResponseTime() {
        List<Double> sorted = new ArrayList<>(responseTimes);
        sorted.sort(Double::compareTo);
        int size = sorted.size();
        if (size == 0) return 0.0;
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
        } else {
            return sorted.get(size / 2);
        }
    }
    
    public double getP95ResponseTime() {
        return getPercentile(95);
    }
    
    public double getP99ResponseTime() {
        return getPercentile(99);
    }
    
    public double getMaxResponseTime() {
        return responseTimes.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    }
    
    private double getPercentile(int percentile) {
        List<Double> sorted = new ArrayList<>(responseTimes);
        sorted.sort(Double::compareTo);
        if (sorted.isEmpty()) return 0.0;
        
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }
    
    public double getThroughput(long durationMs) {
        return durationMs > 0 ? (double) totalRequests.get() * 1000 / durationMs : 0.0;
    }
    
    public double getErrorRate() {
        long total = totalRequests.get();
        return total > 0 ? (double) getErrorCount() / total : 0.0;
    }
    
    public Map<String, Long> getRequestsByEndpoint() {
        return requestsByEndpoint.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey, 
                    entry -> entry.getValue().get()
                ));
    }
    
    public Map<String, Long> getErrorsByType() {
        return errorsByType.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey, 
                    entry -> entry.getValue().get()
                ));
    }
    
    public List<LoadTestingSuite.SystemMetrics> getSystemMetricsHistory() {
        return new ArrayList<>(systemMetrics);
    }
}
```

---

## 📝 Key Takeaways from Day 47

### **JMH Microbenchmarking:**
- ✅ **Scientific Measurement**: Precise performance measurement with statistical analysis
- ✅ **JIT Compiler Awareness**: Proper warmup and dead code elimination prevention
- ✅ **Multiple Measurement Modes**: Throughput, average time, sampling, and single-shot
- ✅ **Statistical Significance**: Fork isolation and confidence intervals for reliable results
- ✅ **Regression Detection**: Automated performance regression analysis and reporting

### **Load Testing Mastery:**
- ✅ **Realistic Load Simulation**: Concurrent users with ramp-up and think time patterns
- ✅ **Comprehensive Metrics**: Response times, throughput, error rates, and percentiles
- ✅ **System Resource Monitoring**: Memory, CPU, and thread utilization during load
- ✅ **Scenario-Based Testing**: Multi-step user journeys and realistic usage patterns
- ✅ **Performance Assessment**: Automated analysis and recommendations

### **Performance Analysis:**
- ✅ **Trend Analysis**: Historical performance tracking and regression detection
- ✅ **Bottleneck Identification**: Systematic approach to finding performance issues
- ✅ **Scalability Testing**: Understanding system behavior under increasing load
- ✅ **Resource Utilization**: Monitoring system resources during performance tests
- ✅ **CI/CD Integration**: Automated performance testing in development pipelines

---

## 🚀 Tomorrow's Preview (Day 48 - August 26)

**Focus:** Testing Best Practices & TDD
- Advanced test design patterns and architectural testing
- Test-driven development for complex systems
- Property-based testing and mutation testing
- Testing strategy for microservices and distributed systems
- Quality metrics and testing best practices

**Preparation:**
- Review today's performance testing concepts
- Think about testing strategies for complex applications
- Consider how to measure and improve test quality

---

## ✅ Day 47 Checklist

**Completed Learning Objectives:**
- [ ] Mastered JMH microbenchmarking for precise performance measurement
- [ ] Implemented comprehensive load testing strategies and frameworks
- [ ] Built performance regression detection and continuous benchmarking systems
- [ ] Created scalability testing scenarios with realistic load patterns
- [ ] Developed memory and CPU profiling techniques under load conditions
- [ ] Designed enterprise-grade performance testing suites with monitoring integration
- [ ] Built automated performance analysis and reporting systems
- [ ] Practiced scientific approach to performance measurement

**Bonus Achievements:**
- [ ] Extended load testing with complex user scenarios
- [ ] Implemented automated performance CI/CD pipeline integration
- [ ] Created performance testing dashboards and visualization
- [ ] Explored advanced JMH features and profiling integrations

---

## 🎉 Performance Testing Mastery!

You've mastered comprehensive performance testing and benchmarking, building enterprise-grade tools for measuring and optimizing system performance. Tomorrow we'll focus on testing best practices and advanced testing methodologies.

**Your performance engineering skills are world-class!** 🚀