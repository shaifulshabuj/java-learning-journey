# Day 158: Project Panama - Foreign Function & Memory API

## Learning Objectives
By the end of this session, you will:
- Master the Foreign Function Interface (FFI) for calling native libraries from Java
- Implement advanced memory management with off-heap data structures
- Create high-performance native library integrations and binding generation
- Understand cross-platform compatibility and deployment strategies
- Build production-ready native integrations with proper resource management

## Theoretical Foundation

### Project Panama Architecture

**Understanding Foreign Function & Memory API:**
```java
// Introduction to Project Panama components
public class PanamaArchitecture {
    
    /**
     * Project Panama Components:
     * 1. Foreign Function API - Call native functions from Java
     * 2. Foreign Memory API - Manage off-heap memory
     * 3. Vector API - SIMD operations (covered in Day 157)
     * 4. jextract Tool - Generate Java bindings from C headers
     * 
     * Benefits:
     * - Direct native interop without JNI overhead
     * - Type-safe memory access
     * - Automatic resource management
     * - Platform-neutral API design
     */
    
    public void demonstratePanamaCapabilities() {
        System.out.println("=== Project Panama Capabilities ===");
        
        // Check if Panama is available
        try {
            Class.forName("java.lang.foreign.MemorySegment");
            System.out.println("✓ Foreign Memory API available");
        } catch (ClassNotFoundException e) {
            System.out.println("✗ Foreign Memory API not available");
        }
        
        try {
            Class.forName("java.lang.foreign.Linker");
            System.out.println("✓ Foreign Function API available");
        } catch (ClassNotFoundException e) {
            System.out.println("✗ Foreign Function API not available");
        }
        
        // Display system information
        System.out.printf("Operating System: %s%n", System.getProperty("os.name"));
        System.out.printf("Architecture: %s%n", System.getProperty("os.arch"));
        System.out.printf("Java Version: %s%n", System.getProperty("java.version"));
        
        // Memory segment capabilities
        demonstrateMemorySegmentCapabilities();
    }
    
    private void demonstrateMemorySegmentCapabilities() {
        System.out.println("\n=== Memory Segment Capabilities ===");
        
        try (Arena arena = Arena.ofConfined()) {
            // Allocate native memory
            MemorySegment segment = arena.allocate(1024);
            
            System.out.printf("Allocated segment: %s%n", segment);
            System.out.printf("Segment size: %d bytes%n", segment.byteSize());
            System.out.printf("Segment address: 0x%x%n", segment.address());
            System.out.printf("Is native segment: %s%n", segment.isNative());
            
            // Write and read data
            segment.set(ValueLayout.JAVA_INT, 0, 42);
            segment.set(ValueLayout.JAVA_FLOAT, 4, 3.14159f);
            segment.set(ValueLayout.JAVA_DOUBLE, 8, 2.71828);
            
            int intValue = segment.get(ValueLayout.JAVA_INT, 0);
            float floatValue = segment.get(ValueLayout.JAVA_FLOAT, 4);
            double doubleValue = segment.get(ValueLayout.JAVA_DOUBLE, 8);
            
            System.out.printf("Read values: int=%d, float=%.5f, double=%.5f%n", 
                intValue, floatValue, doubleValue);
            
        } catch (Exception e) {
            System.err.println("Memory segment operations failed: " + e.getMessage());
        }
    }
}
```

### Foreign Function Interface Implementation

**Direct Native Function Calls:**
```java
// Advanced Foreign Function Interface implementations
public class ForeignFunctionInterface {
    
    // C standard library integration
    public static class CStandardLibrary {
        private static final Linker linker = Linker.nativeLinker();
        private static final SymbolLookup stdlib = linker.defaultLookup();
        
        // Function signatures
        private static final FunctionDescriptor MALLOC_DESC = 
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG);
        private static final FunctionDescriptor FREE_DESC = 
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS);
        private static final FunctionDescriptor STRLEN_DESC = 
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS);
        private static final FunctionDescriptor STRCPY_DESC = 
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS);
        private static final FunctionDescriptor PRINTF_DESC = 
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS);
        
        // Method handles for native functions
        private static final MethodHandle malloc;
        private static final MethodHandle free;
        private static final MethodHandle strlen;
        private static final MethodHandle strcpy;
        private static final MethodHandle printf;
        
        static {
            try {
                malloc = linker.downcallHandle(stdlib.find("malloc").orElseThrow(), MALLOC_DESC);
                free = linker.downcallHandle(stdlib.find("free").orElseThrow(), FREE_DESC);
                strlen = linker.downcallHandle(stdlib.find("strlen").orElseThrow(), STRLEN_DESC);
                strcpy = linker.downcallHandle(stdlib.find("strcpy").orElseThrow(), STRCPY_DESC);
                printf = linker.downcallHandle(stdlib.find("printf").orElseThrow(), PRINTF_DESC);
            } catch (Throwable e) {
                throw new RuntimeException("Failed to initialize C standard library bindings", e);
            }
        }
        
        public static MemorySegment allocateMemory(long size) {
            try {
                MemorySegment address = (MemorySegment) malloc.invokeExact(size);
                if (address.address() == 0) {
                    throw new OutOfMemoryError("Native malloc failed");
                }
                return address.reinterpret(size);
            } catch (Throwable e) {
                throw new RuntimeException("Memory allocation failed", e);
            }
        }
        
        public static void freeMemory(MemorySegment memory) {
            try {
                free.invokeExact(memory);
            } catch (Throwable e) {
                throw new RuntimeException("Memory deallocation failed", e);
            }
        }
        
        public static long stringLength(MemorySegment cString) {
            try {
                return (long) strlen.invokeExact(cString);
            } catch (Throwable e) {
                throw new RuntimeException("String length calculation failed", e);
            }
        }
        
        public static void copyString(MemorySegment dest, MemorySegment src) {
            try {
                strcpy.invokeExact(dest, src);
            } catch (Throwable e) {
                throw new RuntimeException("String copy failed", e);
            }
        }
        
        public static void printString(String format, Object... args) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment formatStr = arena.allocateUtf8String(String.format(format, args));
                printf.invokeExact(formatStr);
            } catch (Throwable e) {
                throw new RuntimeException("Printf failed", e);
            }
        }
        
        // Demonstration of C library usage
        public static void demonstrateCLibraryUsage() {
            System.out.println("=== C Standard Library Integration ===");
            
            try (Arena arena = Arena.ofConfined()) {
                // String operations
                String javaString = "Hello from Java via Panama!";
                MemorySegment cString = arena.allocateUtf8String(javaString);
                
                long length = stringLength(cString);
                System.out.printf("String length: %d%n", length);
                
                // Memory allocation
                MemorySegment buffer = allocateMemory(100);
                copyString(buffer, cString);
                
                // Read back the string
                String readBack = buffer.getUtf8String(0);
                System.out.printf("Read back: %s%n", readBack);
                
                // Print using C printf
                printString("C printf: %s (length: %d)%n", javaString, length);
                
                // Clean up manually allocated memory
                freeMemory(buffer);
                
            } catch (Exception e) {
                System.err.println("C library demonstration failed: " + e.getMessage());
            }
        }
    }
    
    // Mathematical library integration (libm)
    public static class MathLibrary {
        private static final Linker linker = Linker.nativeLinker();
        private static final SymbolLookup mathLib = linker.defaultLookup();
        
        // Function descriptors for math functions
        private static final FunctionDescriptor DOUBLE_TO_DOUBLE = 
            FunctionDescriptor.of(ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE);
        private static final FunctionDescriptor DOUBLE_DOUBLE_TO_DOUBLE = 
            FunctionDescriptor.of(ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE);
        
        // Method handles
        private static final MethodHandle sin;
        private static final MethodHandle cos;
        private static final MethodHandle sqrt;
        private static final MethodHandle pow;
        private static final MethodHandle log;
        private static final MethodHandle exp;
        
        static {
            try {
                sin = linker.downcallHandle(mathLib.find("sin").orElseThrow(), DOUBLE_TO_DOUBLE);
                cos = linker.downcallHandle(mathLib.find("cos").orElseThrow(), DOUBLE_TO_DOUBLE);
                sqrt = linker.downcallHandle(mathLib.find("sqrt").orElseThrow(), DOUBLE_TO_DOUBLE);
                pow = linker.downcallHandle(mathLib.find("pow").orElseThrow(), DOUBLE_DOUBLE_TO_DOUBLE);
                log = linker.downcallHandle(mathLib.find("log").orElseThrow(), DOUBLE_TO_DOUBLE);
                exp = linker.downcallHandle(mathLib.find("exp").orElseThrow(), DOUBLE_TO_DOUBLE);
            } catch (Throwable e) {
                throw new RuntimeException("Failed to initialize math library bindings", e);
            }
        }
        
        public static double sine(double x) {
            try {
                return (double) sin.invokeExact(x);
            } catch (Throwable e) {
                throw new RuntimeException("Native sin failed", e);
            }
        }
        
        public static double cosine(double x) {
            try {
                return (double) cos.invokeExact(x);
            } catch (Throwable e) {
                throw new RuntimeException("Native cos failed", e);
            }
        }
        
        public static double squareRoot(double x) {
            try {
                return (double) sqrt.invokeExact(x);
            } catch (Throwable e) {
                throw new RuntimeException("Native sqrt failed", e);
            }
        }
        
        public static double power(double base, double exponent) {
            try {
                return (double) pow.invokeExact(base, exponent);
            } catch (Throwable e) {
                throw new RuntimeException("Native pow failed", e);
            }
        }
        
        public static double naturalLog(double x) {
            try {
                return (double) log.invokeExact(x);
            } catch (Throwable e) {
                throw new RuntimeException("Native log failed", e);
            }
        }
        
        public static double exponential(double x) {
            try {
                return (double) exp.invokeExact(x);
            } catch (Throwable e) {
                throw new RuntimeException("Native exp failed", e);
            }
        }
        
        // Vectorized math operations using native functions
        public static void vectorizedMathOperations(double[] input, double[] output, 
                                                   String operation) {
            if (input.length != output.length) {
                throw new IllegalArgumentException("Input and output arrays must have same length");
            }
            
            MethodHandle mathFunction = switch (operation.toLowerCase()) {
                case "sin" -> sin;
                case "cos" -> cos;
                case "sqrt" -> sqrt;
                case "log" -> log;
                case "exp" -> exp;
                default -> throw new IllegalArgumentException("Unknown operation: " + operation);
            };
            
            try {
                for (int i = 0; i < input.length; i++) {
                    output[i] = (double) mathFunction.invokeExact(input[i]);
                }
            } catch (Throwable e) {
                throw new RuntimeException("Vectorized math operation failed", e);
            }
        }
        
        public static void benchmarkNativeMath() {
            System.out.println("=== Native Math Library Benchmark ===");
            
            int size = 1_000_000;
            double[] input = new double[size];
            double[] nativeOutput = new double[size];
            double[] javaOutput = new double[size];
            
            // Initialize input data
            for (int i = 0; i < size; i++) {
                input[i] = i * 0.001;
            }
            
            // Benchmark native sin
            long startTime = System.nanoTime();
            vectorizedMathOperations(input, nativeOutput, "sin");
            long nativeTime = System.nanoTime() - startTime;
            
            // Benchmark Java sin
            startTime = System.nanoTime();
            for (int i = 0; i < size; i++) {
                javaOutput[i] = Math.sin(input[i]);
            }
            long javaTime = System.nanoTime() - startTime;
            
            System.out.printf("Native sin time: %.2f ms%n", nativeTime / 1_000_000.0);
            System.out.printf("Java sin time: %.2f ms%n", javaTime / 1_000_000.0);
            System.out.printf("Speedup: %.2fx%n", (double) javaTime / nativeTime);
            
            // Verify results are similar
            double maxDifference = 0;
            for (int i = 0; i < size; i++) {
                double diff = Math.abs(nativeOutput[i] - javaOutput[i]);
                maxDifference = Math.max(maxDifference, diff);
            }
            System.out.printf("Maximum difference: %.2e%n", maxDifference);
        }
    }
}
```

### Advanced Memory Management

**Off-Heap Data Structures:**
```java
// Advanced memory management with Foreign Memory API
public class AdvancedMemoryManagement {
    
    // Custom memory allocator
    public static class CustomMemoryAllocator {
        private final Arena arena;
        private final Map<Long, AllocationInfo> allocations = new ConcurrentHashMap<>();
        private final AtomicLong totalAllocated = new AtomicLong(0);
        private final AtomicLong allocationCounter = new AtomicLong(0);
        
        public CustomMemoryAllocator() {
            this.arena = Arena.ofShared();
        }
        
        public MemorySegment allocate(long size, long alignment) {
            MemorySegment segment = arena.allocate(size, alignment);
            
            long allocationId = allocationCounter.incrementAndGet();
            AllocationInfo info = new AllocationInfo(
                allocationId, 
                size, 
                System.currentTimeMillis(),
                Thread.currentThread().getName()
            );
            
            allocations.put(segment.address(), info);
            totalAllocated.addAndGet(size);
            
            System.out.printf("Allocated %d bytes at address 0x%x (ID: %d)%n", 
                size, segment.address(), allocationId);
            
            return segment;
        }
        
        public void deallocate(MemorySegment segment) {
            AllocationInfo info = allocations.remove(segment.address());
            if (info != null) {
                totalAllocated.addAndGet(-info.size);
                System.out.printf("Deallocated %d bytes (ID: %d, lived: %d ms)%n", 
                    info.size, info.id, System.currentTimeMillis() - info.timestamp);
            }
        }
        
        public void printStatistics() {
            System.out.println("=== Memory Allocator Statistics ===");
            System.out.printf("Active allocations: %d%n", allocations.size());
            System.out.printf("Total allocated: %d bytes%n", totalAllocated.get());
            System.out.printf("Total allocation count: %d%n", allocationCounter.get());
            
            if (!allocations.isEmpty()) {
                System.out.println("Active allocations:");
                allocations.values().stream()
                    .sorted((a, b) -> Long.compare(b.size, a.size))
                    .limit(10)
                    .forEach(info -> System.out.printf("  ID %d: %d bytes by %s%n", 
                        info.id, info.size, info.threadName));
            }
        }
        
        @Override
        protected void finalize() throws Throwable {
            arena.close();
            super.finalize();
        }
        
        private record AllocationInfo(long id, long size, long timestamp, String threadName) {}
    }
    
    // Off-heap data structures
    public static class OffHeapDataStructures {
        
        // Off-heap array implementation
        public static class OffHeapIntArray implements AutoCloseable {
            private final MemorySegment segment;
            private final int length;
            private final Arena arena;
            
            public OffHeapIntArray(int length) {
                this.length = length;
                this.arena = Arena.ofConfined();
                this.segment = arena.allocate(ValueLayout.JAVA_INT, length);
            }
            
            public void set(int index, int value) {
                if (index < 0 || index >= length) {
                    throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + length);
                }
                segment.setAtIndex(ValueLayout.JAVA_INT, index, value);
            }
            
            public int get(int index) {
                if (index < 0 || index >= length) {
                    throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + length);
                }
                return segment.getAtIndex(ValueLayout.JAVA_INT, index);
            }
            
            public int length() {
                return length;
            }
            
            public void fill(int value) {
                for (int i = 0; i < length; i++) {
                    set(i, value);
                }
            }
            
            public void copyFrom(int[] source, int sourceOffset, int destOffset, int length) {
                if (sourceOffset + length > source.length || destOffset + length > this.length) {
                    throw new IndexOutOfBoundsException("Copy operation exceeds array bounds");
                }
                
                try (Arena tempArena = Arena.ofConfined()) {
                    MemorySegment sourceSegment = tempArena.allocateArray(ValueLayout.JAVA_INT, source);
                    MemorySegment.copy(sourceSegment, ValueLayout.JAVA_INT, sourceOffset,
                                     segment, ValueLayout.JAVA_INT, destOffset, length);
                }
            }
            
            public int[] toArray() {
                int[] result = new int[length];
                for (int i = 0; i < length; i++) {
                    result[i] = get(i);
                }
                return result;
            }
            
            @Override
            public void close() {
                arena.close();
            }
        }
        
        // Off-heap hash map implementation
        public static class OffHeapHashMap<K, V> implements AutoCloseable {
            private static final int DEFAULT_CAPACITY = 16;
            private static final double LOAD_FACTOR = 0.75;
            
            private final Arena arena;
            private MemorySegment bucketsSegment;
            private int capacity;
            private int size;
            private final Map<K, MemorySegment> keyToEntryMap = new HashMap<>();
            
            // Entry layout: hash(4) + keyHash(4) + keyRef(8) + valueRef(8) + next(8) = 32 bytes
            private static final GroupLayout ENTRY_LAYOUT = MemoryLayout.structLayout(
                ValueLayout.JAVA_INT.withName("hash"),
                ValueLayout.JAVA_INT.withName("keyHash"),
                ValueLayout.JAVA_LONG.withName("keyRef"),
                ValueLayout.JAVA_LONG.withName("valueRef"),
                ValueLayout.ADDRESS.withName("next")
            );
            
            public OffHeapHashMap() {
                this(DEFAULT_CAPACITY);
            }
            
            public OffHeapHashMap(int initialCapacity) {
                this.arena = Arena.ofConfined();
                this.capacity = initialCapacity;
                this.size = 0;
                
                // Allocate bucket array (array of pointers to first entry in each bucket)
                this.bucketsSegment = arena.allocateArray(ValueLayout.ADDRESS, capacity);
                
                // Initialize all buckets to null
                for (int i = 0; i < capacity; i++) {
                    bucketsSegment.setAtIndex(ValueLayout.ADDRESS, i, MemorySegment.NULL);
                }
            }
            
            public V put(K key, V value) {
                if (size >= capacity * LOAD_FACTOR) {
                    resize();
                }
                
                int hash = key.hashCode();
                int bucketIndex = Math.abs(hash) % capacity;
                
                // Create new entry
                MemorySegment entrySegment = arena.allocate(ENTRY_LAYOUT);
                entrySegment.set(ValueLayout.JAVA_INT, 0, hash); // hash
                entrySegment.set(ValueLayout.JAVA_INT, 4, key.hashCode()); // keyHash
                
                // Store references (simplified - in real implementation would need proper serialization)
                entrySegment.set(ValueLayout.JAVA_LONG, 8, System.identityHashCode(key)); // keyRef
                entrySegment.set(ValueLayout.JAVA_LONG, 16, System.identityHashCode(value)); // valueRef
                
                // Link to existing chain
                MemorySegment firstEntry = bucketsSegment.getAtIndex(ValueLayout.ADDRESS, bucketIndex);
                entrySegment.set(ValueLayout.ADDRESS, 24, firstEntry); // next
                
                // Update bucket to point to new entry
                bucketsSegment.setAtIndex(ValueLayout.ADDRESS, bucketIndex, entrySegment);
                
                keyToEntryMap.put(key, entrySegment);
                size++;
                
                return null; // Simplified - would return previous value
            }
            
            public V get(K key) {
                MemorySegment entrySegment = keyToEntryMap.get(key);
                if (entrySegment == null) {
                    return null;
                }
                
                // In real implementation, would deserialize value from memory
                // For now, return null as placeholder
                return null;
            }
            
            public int size() {
                return size;
            }
            
            public boolean isEmpty() {
                return size == 0;
            }
            
            private void resize() {
                // Simplified resize - in real implementation would rebuild hash table
                capacity *= 2;
                MemorySegment newBucketsSegment = arena.allocateArray(ValueLayout.ADDRESS, capacity);
                for (int i = 0; i < capacity; i++) {
                    newBucketsSegment.setAtIndex(ValueLayout.ADDRESS, i, MemorySegment.NULL);
                }
                bucketsSegment = newBucketsSegment;
            }
            
            public void printStatistics() {
                System.out.printf("OffHeapHashMap: size=%d, capacity=%d, load=%.2f%n", 
                    size, capacity, (double) size / capacity);
                
                // Count entries per bucket
                int[] bucketCounts = new int[capacity];
                for (int i = 0; i < capacity; i++) {
                    MemorySegment entry = bucketsSegment.getAtIndex(ValueLayout.ADDRESS, i);
                    int count = 0;
                    while (!entry.equals(MemorySegment.NULL)) {
                        count++;
                        entry = entry.get(ValueLayout.ADDRESS, 24); // next
                    }
                    bucketCounts[i] = count;
                }
                
                int maxChainLength = Arrays.stream(bucketCounts).max().orElse(0);
                int emptyBuckets = (int) Arrays.stream(bucketCounts).filter(c -> c == 0).count();
                
                System.out.printf("Max chain length: %d, Empty buckets: %d%n", 
                    maxChainLength, emptyBuckets);
            }
            
            @Override
            public void close() {
                arena.close();
            }
        }
        
        // Demonstration of off-heap data structures
        public static void demonstrateOffHeapStructures() {
            System.out.println("=== Off-Heap Data Structures ===");
            
            // Test off-heap array
            try (OffHeapIntArray array = new OffHeapIntArray(1000)) {
                // Fill with test data
                for (int i = 0; i < array.length(); i++) {
                    array.set(i, i * i);
                }
                
                // Test access
                System.out.printf("Array[100] = %d%n", array.get(100));
                System.out.printf("Array[500] = %d%n", array.get(500));
                
                // Test copy operation
                int[] sourceArray = {1, 2, 3, 4, 5};
                array.copyFrom(sourceArray, 0, 0, sourceArray.length);
                
                System.out.print("First 5 elements after copy: ");
                for (int i = 0; i < 5; i++) {
                    System.out.printf("%d ", array.get(i));
                }
                System.out.println();
            }
            
            // Test off-heap hash map
            try (OffHeapHashMap<String, String> map = new OffHeapHashMap<>()) {
                // Add test data
                for (int i = 0; i < 100; i++) {
                    map.put("key" + i, "value" + i);
                }
                
                System.out.printf("Map size: %d%n", map.size());
                map.printStatistics();
            }
        }
    }
}
```

### Native Library Integration Patterns

**Cross-Platform Native Integration:**
```java
// Comprehensive native library integration framework
public class NativeLibraryIntegration {
    
    // Dynamic library loader with platform detection
    public static class PlatformAwareLibraryLoader {
        private static final Map<String, String> LIBRARY_EXTENSIONS = Map.of(
            "windows", ".dll",
            "linux", ".so",
            "mac", ".dylib"
        );
        
        private static final Map<String, String> LIBRARY_PREFIXES = Map.of(
            "windows", "",
            "linux", "lib",
            "mac", "lib"
        );
        
        public static SymbolLookup loadLibrary(String libraryName) {
            String platform = detectPlatform();
            String prefix = LIBRARY_PREFIXES.get(platform);
            String extension = LIBRARY_EXTENSIONS.get(platform);
            
            String fullLibraryName = prefix + libraryName + extension;
            
            System.out.printf("Loading library: %s for platform: %s%n", fullLibraryName, platform);
            
            try {
                // Try to load from system path first
                return SymbolLookup.libraryLookup(libraryName, Arena.global());
            } catch (Exception e) {
                System.err.printf("Failed to load library %s: %s%n", libraryName, e.getMessage());
                
                // Try alternative loading strategies
                return tryAlternativeLoading(libraryName, fullLibraryName);
            }
        }
        
        private static String detectPlatform() {
            String osName = System.getProperty("os.name").toLowerCase();
            
            if (osName.contains("windows")) {
                return "windows";
            } else if (osName.contains("linux")) {
                return "linux";
            } else if (osName.contains("mac") || osName.contains("darwin")) {
                return "mac";
            } else {
                throw new UnsupportedOperationException("Unsupported platform: " + osName);
            }
        }
        
        private static SymbolLookup tryAlternativeLoading(String libraryName, String fullLibraryName) {
            // Try loading from classpath resources
            try {
                String resourcePath = "/native/" + detectPlatform() + "/" + fullLibraryName;
                
                try (InputStream libraryStream = PlatformAwareLibraryLoader.class
                        .getResourceAsStream(resourcePath)) {
                    
                    if (libraryStream != null) {
                        // Extract to temporary file and load
                        Path tempLibrary = extractLibraryToTemp(libraryStream, fullLibraryName);
                        return SymbolLookup.libraryLookup(tempLibrary, Arena.global());
                    }
                }
            } catch (Exception e) {
                System.err.printf("Alternative loading failed: %s%n", e.getMessage());
            }
            
            throw new RuntimeException("Could not load library: " + libraryName);
        }
        
        private static Path extractLibraryToTemp(InputStream libraryStream, String libraryName) 
                throws IOException {
            Path tempDir = Files.createTempDirectory("native-libs");
            Path tempLibrary = tempDir.resolve(libraryName);
            
            Files.copy(libraryStream, tempLibrary, StandardCopyOption.REPLACE_EXISTING);
            
            // Make executable on Unix-like systems
            if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
                tempLibrary.toFile().setExecutable(true);
            }
            
            // Register for cleanup on JVM exit
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Files.deleteIfExists(tempLibrary);
                    Files.deleteIfExists(tempDir);
                } catch (IOException e) {
                    System.err.println("Failed to cleanup temporary library: " + e.getMessage());
                }
            }));
            
            return tempLibrary;
        }
    }
    
    // High-level native library wrapper
    public static class NativeLibraryWrapper {
        protected final SymbolLookup library;
        protected final Linker linker;
        protected final Map<String, MethodHandle> functionCache = new ConcurrentHashMap<>();
        
        public NativeLibraryWrapper(String libraryName) {
            this.library = PlatformAwareLibraryLoader.loadLibrary(libraryName);
            this.linker = Linker.nativeLinker();
        }
        
        protected MethodHandle getFunction(String functionName, FunctionDescriptor descriptor) {
            return functionCache.computeIfAbsent(functionName, name -> {
                try {
                    MemorySegment symbol = library.find(name)
                        .orElseThrow(() -> new RuntimeException("Function not found: " + name));
                    return linker.downcallHandle(symbol, descriptor);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create method handle for " + name, e);
                }
            });
        }
        
        protected void registerCallback(String functionName, MethodHandle javaMethod, 
                                      FunctionDescriptor descriptor) {
            try {
                MemorySegment callback = linker.upcallStub(javaMethod, descriptor, Arena.global());
                // In a real implementation, you'd register this callback with the native library
                System.out.printf("Registered callback %s at address 0x%x%n", 
                    functionName, callback.address());
            } catch (Exception e) {
                throw new RuntimeException("Failed to register callback: " + functionName, e);
            }
        }
    }
    
    // Example: SQLite integration
    public static class SQLiteIntegration extends NativeLibraryWrapper {
        // SQLite function descriptors
        private static final FunctionDescriptor SQLITE_OPEN_DESC = FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS);
        private static final FunctionDescriptor SQLITE_CLOSE_DESC = FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS);
        private static final FunctionDescriptor SQLITE_EXEC_DESC = FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, 
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS);
        
        // Method handles
        private final MethodHandle sqlite3_open;
        private final MethodHandle sqlite3_close;
        private final MethodHandle sqlite3_exec;
        
        public SQLiteIntegration() {
            super("sqlite3");
            
            this.sqlite3_open = getFunction("sqlite3_open", SQLITE_OPEN_DESC);
            this.sqlite3_close = getFunction("sqlite3_close", SQLITE_CLOSE_DESC);
            this.sqlite3_exec = getFunction("sqlite3_exec", SQLITE_EXEC_DESC);
        }
        
        public SQLiteDatabase openDatabase(String filename) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment filenameSegment = arena.allocateUtf8String(filename);
                MemorySegment dbPtrPtr = arena.allocate(ValueLayout.ADDRESS);
                
                int result = (int) sqlite3_open.invokeExact(filenameSegment, dbPtrPtr);
                
                if (result != 0) { // SQLITE_OK = 0
                    throw new RuntimeException("Failed to open database: " + filename);
                }
                
                MemorySegment dbPtr = dbPtrPtr.get(ValueLayout.ADDRESS, 0);
                return new SQLiteDatabase(dbPtr);
                
            } catch (Throwable e) {
                throw new RuntimeException("Database open failed", e);
            }
        }
        
        public class SQLiteDatabase implements AutoCloseable {
            private final MemorySegment dbHandle;
            private volatile boolean closed = false;
            
            private SQLiteDatabase(MemorySegment dbHandle) {
                this.dbHandle = dbHandle;
            }
            
            public void executeSQL(String sql) {
                if (closed) {
                    throw new IllegalStateException("Database is closed");
                }
                
                try (Arena arena = Arena.ofConfined()) {
                    MemorySegment sqlSegment = arena.allocateUtf8String(sql);
                    
                    int result = (int) sqlite3_exec.invokeExact(
                        dbHandle, sqlSegment, 
                        MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL);
                    
                    if (result != 0) {
                        throw new RuntimeException("SQL execution failed: " + sql);
                    }
                    
                } catch (Throwable e) {
                    throw new RuntimeException("SQL execution error", e);
                }
            }
            
            @Override
            public void close() {
                if (!closed) {
                    try {
                        sqlite3_close.invokeExact(dbHandle);
                        closed = true;
                    } catch (Throwable e) {
                        System.err.println("Failed to close database: " + e.getMessage());
                    }
                }
            }
        }
        
        // Demonstration
        public static void demonstrateSQL() {
            System.out.println("=== SQLite Integration Demo ===");
            
            try {
                SQLiteIntegration sqlite = new SQLiteIntegration();
                
                // Open in-memory database
                try (SQLiteDatabase db = sqlite.openDatabase(":memory:")) {
                    // Create table
                    db.executeSQL("""
                        CREATE TABLE users (
                            id INTEGER PRIMARY KEY,
                            name TEXT NOT NULL,
                            email TEXT UNIQUE
                        )
                        """);
                    
                    // Insert data
                    db.executeSQL("""
                        INSERT INTO users (name, email) VALUES 
                        ('Alice', 'alice@example.com'),
                        ('Bob', 'bob@example.com')
                        """);
                    
                    System.out.println("SQLite operations completed successfully");
                }
            } catch (Exception e) {
                System.err.println("SQLite demo failed: " + e.getMessage());
            }
        }
    }
    
    // Performance-oriented BLAS integration
    public static class BLASIntegration extends NativeLibraryWrapper {
        // BLAS function descriptors
        private static final FunctionDescriptor DGEMM_DESC = FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, // TRANSA, TRANSB
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, // M, N, K
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, // ALPHA, A, LDA
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, // B, LDB
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS // BETA, C, LDC
        );
        
        private final MethodHandle dgemm;
        
        public BLASIntegration() {
            super("blas"); // or "openblas", "mkl", etc.
            this.dgemm = getFunction("dgemm_", DGEMM_DESC);
        }
        
        public void matrixMultiply(double[][] a, double[][] b, double[][] c, 
                                 double alpha, double beta) {
            int m = a.length;
            int n = b[0].length;
            int k = a[0].length;
            
            if (b.length != k || c.length != m || c[0].length != n) {
                throw new IllegalArgumentException("Matrix dimensions don't match");
            }
            
            try (Arena arena = Arena.ofConfined()) {
                // Allocate and copy matrices to native memory
                MemorySegment aSegment = arena.allocateArray(ValueLayout.JAVA_DOUBLE, m * k);
                MemorySegment bSegment = arena.allocateArray(ValueLayout.JAVA_DOUBLE, k * n);
                MemorySegment cSegment = arena.allocateArray(ValueLayout.JAVA_DOUBLE, m * n);
                
                // Copy data to native memory (column-major order for BLAS)
                copyMatrixToNative(a, aSegment, m, k, true);
                copyMatrixToNative(b, bSegment, k, n, true);
                copyMatrixToNative(c, cSegment, m, n, true);
                
                // BLAS parameters
                MemorySegment transA = arena.allocateUtf8String("N");
                MemorySegment transB = arena.allocateUtf8String("N");
                MemorySegment mPtr = arena.allocate(ValueLayout.JAVA_INT);
                MemorySegment nPtr = arena.allocate(ValueLayout.JAVA_INT);
                MemorySegment kPtr = arena.allocate(ValueLayout.JAVA_INT);
                MemorySegment alphaPtr = arena.allocate(ValueLayout.JAVA_DOUBLE);
                MemorySegment betaPtr = arena.allocate(ValueLayout.JAVA_DOUBLE);
                MemorySegment ldaPtr = arena.allocate(ValueLayout.JAVA_INT);
                MemorySegment ldbPtr = arena.allocate(ValueLayout.JAVA_INT);
                MemorySegment ldcPtr = arena.allocate(ValueLayout.JAVA_INT);
                
                mPtr.set(ValueLayout.JAVA_INT, 0, m);
                nPtr.set(ValueLayout.JAVA_INT, 0, n);
                kPtr.set(ValueLayout.JAVA_INT, 0, k);
                alphaPtr.set(ValueLayout.JAVA_DOUBLE, 0, alpha);
                betaPtr.set(ValueLayout.JAVA_DOUBLE, 0, beta);
                ldaPtr.set(ValueLayout.JAVA_INT, 0, m);
                ldbPtr.set(ValueLayout.JAVA_INT, 0, k);
                ldcPtr.set(ValueLayout.JAVA_INT, 0, m);
                
                // Call BLAS dgemm
                dgemm.invokeExact(transA, transB, mPtr, nPtr, kPtr,
                                alphaPtr, aSegment, ldaPtr, bSegment, ldbPtr,
                                betaPtr, cSegment, ldcPtr);
                
                // Copy result back to Java array
                copyMatrixFromNative(cSegment, c, m, n, true);
                
            } catch (Throwable e) {
                throw new RuntimeException("BLAS matrix multiplication failed", e);
            }
        }
        
        private void copyMatrixToNative(double[][] matrix, MemorySegment segment, 
                                      int rows, int cols, boolean columnMajor) {
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    int index = columnMajor ? j * rows + i : i * cols + j;
                    segment.setAtIndex(ValueLayout.JAVA_DOUBLE, index, matrix[i][j]);
                }
            }
        }
        
        private void copyMatrixFromNative(MemorySegment segment, double[][] matrix, 
                                        int rows, int cols, boolean columnMajor) {
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    int index = columnMajor ? j * rows + i : i * cols + j;
                    matrix[i][j] = segment.getAtIndex(ValueLayout.JAVA_DOUBLE, index);
                }
            }
        }
        
        public static void demonstrateBLAS() {
            System.out.println("=== BLAS Integration Demo ===");
            
            try {
                BLASIntegration blas = new BLASIntegration();
                
                // Create test matrices
                double[][] a = {{1, 2}, {3, 4}};
                double[][] b = {{5, 6}, {7, 8}};
                double[][] c = new double[2][2];
                
                // Perform matrix multiplication: C = 1.0 * A * B + 0.0 * C
                blas.matrixMultiply(a, b, c, 1.0, 0.0);
                
                System.out.println("Matrix multiplication result:");
                for (double[] row : c) {
                    System.out.println(Arrays.toString(row));
                }
                
            } catch (Exception e) {
                System.err.println("BLAS demo failed: " + e.getMessage());
            }
        }
    }
}
```

## Practical Implementation

### Complete Native Integration Framework

**Production-Ready Native Library Manager:**
```java
// Enterprise-grade native library integration framework
public class EnterpriseNativeFramework {
    
    // Centralized native library manager
    public static class NativeLibraryManager {
        private static final NativeLibraryManager INSTANCE = new NativeLibraryManager();
        private final Map<String, LibraryDescriptor> loadedLibraries = new ConcurrentHashMap<>();
        private final ExecutorService backgroundExecutor = Executors.newFixedThreadPool(2);
        
        public static NativeLibraryManager getInstance() {
            return INSTANCE;
        }
        
        public synchronized <T extends NativeLibraryWrapper> T loadLibrary(
                Class<T> wrapperClass, String libraryName, String version) {
            
            String key = libraryName + ":" + version;
            LibraryDescriptor descriptor = loadedLibraries.get(key);
            
            if (descriptor != null && descriptor.wrapperClass.equals(wrapperClass)) {
                @SuppressWarnings("unchecked")
                T cached = (T) descriptor.wrapper;
                return cached;
            }
            
            try {
                // Load library
                SymbolLookup symbolLookup = PlatformAwareLibraryLoader.loadLibrary(libraryName);
                
                // Create wrapper instance
                Constructor<T> constructor = wrapperClass.getDeclaredConstructor(SymbolLookup.class);
                T wrapper = constructor.newInstance(symbolLookup);
                
                // Register
                LibraryDescriptor newDescriptor = new LibraryDescriptor(
                    wrapperClass, wrapper, libraryName, version, System.currentTimeMillis());
                loadedLibraries.put(key, newDescriptor);
                
                System.out.printf("Loaded native library: %s:%s%n", libraryName, version);
                return wrapper;
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to load library: " + key, e);
            }
        }
        
        public void preloadLibraries(List<LibraryConfig> configs) {
            List<CompletableFuture<Void>> futures = configs.stream()
                .map(config -> CompletableFuture.runAsync(() -> {
                    try {
                        loadLibrary(config.wrapperClass(), config.name(), config.version());
                    } catch (Exception e) {
                        System.err.printf("Failed to preload %s: %s%n", 
                            config.name(), e.getMessage());
                    }
                }, backgroundExecutor))
                .toList();
            
            // Wait for all preloads to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .orTimeout(30, TimeUnit.SECONDS)
                .join();
        }
        
        public void printStatistics() {
            System.out.println("=== Native Library Manager Statistics ===");
            System.out.printf("Loaded libraries: %d%n", loadedLibraries.size());
            
            loadedLibraries.forEach((key, descriptor) -> {
                long ageMs = System.currentTimeMillis() - descriptor.loadTime();
                System.out.printf("  %s: %s (loaded %d ms ago)%n", 
                    key, descriptor.wrapperClass().getSimpleName(), ageMs);
            });
        }
        
        public void shutdown() {
            backgroundExecutor.shutdown();
            try {
                if (!backgroundExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    backgroundExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                backgroundExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        private record LibraryDescriptor(
            Class<?> wrapperClass,
            Object wrapper,
            String name,
            String version,
            long loadTime
        ) {}
        
        public record LibraryConfig(
            Class<? extends NativeLibraryWrapper> wrapperClass,
            String name,
            String version
        ) {}
    }
    
    // Resource management for native memory
    public static class NativeResourceManager implements AutoCloseable {
        private final Set<AutoCloseable> managedResources = ConcurrentHashMap.newKeySet();
        private final AtomicBoolean closed = new AtomicBoolean(false);
        
        public <T extends AutoCloseable> T manage(T resource) {
            if (closed.get()) {
                throw new IllegalStateException("Resource manager is closed");
            }
            
            managedResources.add(resource);
            return resource;
        }
        
        public void unmanage(AutoCloseable resource) {
            managedResources.remove(resource);
        }
        
        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                List<Exception> exceptions = new ArrayList<>();
                
                for (AutoCloseable resource : managedResources) {
                    try {
                        resource.close();
                    } catch (Exception e) {
                        exceptions.add(e);
                    }
                }
                
                managedResources.clear();
                
                if (!exceptions.isEmpty()) {
                    RuntimeException composite = new RuntimeException("Failed to close some resources");
                    exceptions.forEach(composite::addSuppressed);
                    throw composite;
                }
            }
        }
        
        public int getManagedResourceCount() {
            return managedResources.size();
        }
    }
    
    // High-performance image processing library integration
    public static class ImageProcessingLibrary extends NativeLibraryWrapper {
        // Function descriptors for image processing
        private static final FunctionDescriptor GAUSSIAN_BLUR_DESC = FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // input image data
            ValueLayout.ADDRESS, // output image data  
            ValueLayout.JAVA_INT, // width
            ValueLayout.JAVA_INT, // height
            ValueLayout.JAVA_INT, // channels
            ValueLayout.JAVA_FLOAT // sigma
        );
        
        private static final FunctionDescriptor EDGE_DETECTION_DESC = FunctionDescriptor.ofVoid(
            ValueLayout.ADDRESS, // input image data
            ValueLayout.ADDRESS, // output image data
            ValueLayout.JAVA_INT, // width
            ValueLayout.JAVA_INT, // height
            ValueLayout.JAVA_FLOAT // threshold
        );
        
        private final MethodHandle gaussianBlur;
        private final MethodHandle edgeDetection;
        
        public ImageProcessingLibrary(SymbolLookup library) {
            super(library);
            this.gaussianBlur = getFunction("gaussian_blur", GAUSSIAN_BLUR_DESC);
            this.edgeDetection = getFunction("edge_detection", EDGE_DETECTION_DESC);
        }
        
        public ImageProcessingLibrary() {
            this(PlatformAwareLibraryLoader.loadLibrary("imageproc"));
        }
        
        public void applyGaussianBlur(NativeImage input, NativeImage output, float sigma) {
            if (!input.getDimensions().equals(output.getDimensions())) {
                throw new IllegalArgumentException("Input and output dimensions must match");
            }
            
            try {
                gaussianBlur.invokeExact(
                    input.getDataSegment(),
                    output.getDataSegment(),
                    input.getWidth(),
                    input.getHeight(),
                    input.getChannels(),
                    sigma
                );
            } catch (Throwable e) {
                throw new RuntimeException("Gaussian blur failed", e);
            }
        }
        
        public void applyEdgeDetection(NativeImage input, NativeImage output, float threshold) {
            if (!input.getDimensions().equals(output.getDimensions())) {
                throw new IllegalArgumentException("Input and output dimensions must match");
            }
            
            try {
                edgeDetection.invokeExact(
                    input.getDataSegment(),
                    output.getDataSegment(),
                    input.getWidth(),
                    input.getHeight(),
                    threshold
                );
            } catch (Throwable e) {
                throw new RuntimeException("Edge detection failed", e);
            }
        }
        
        // Native image representation
        public static class NativeImage implements AutoCloseable {
            private final MemorySegment dataSegment;
            private final int width;
            private final int height;
            private final int channels;
            private final Arena arena;
            private volatile boolean closed = false;
            
            public NativeImage(int width, int height, int channels) {
                this.width = width;
                this.height = height;
                this.channels = channels;
                this.arena = Arena.ofConfined();
                
                long totalBytes = (long) width * height * channels;
                this.dataSegment = arena.allocate(totalBytes);
            }
            
            public NativeImage(byte[] imageData, int width, int height, int channels) {
                this(width, height, channels);
                
                if (imageData.length != width * height * channels) {
                    throw new IllegalArgumentException("Image data size doesn't match dimensions");
                }
                
                MemorySegment.copy(MemorySegment.ofArray(imageData), 0, 
                                 dataSegment, ValueLayout.JAVA_BYTE, 0, imageData.length);
            }
            
            public MemorySegment getDataSegment() {
                checkNotClosed();
                return dataSegment;
            }
            
            public int getWidth() { return width; }
            public int getHeight() { return height; }
            public int getChannels() { return channels; }
            
            public Dimension getDimensions() {
                return new Dimension(width, height);
            }
            
            public byte[] toByteArray() {
                checkNotClosed();
                
                int totalBytes = width * height * channels;
                byte[] result = new byte[totalBytes];
                
                MemorySegment.copy(dataSegment, ValueLayout.JAVA_BYTE, 0,
                                 MemorySegment.ofArray(result), 0, totalBytes);
                
                return result;
            }
            
            private void checkNotClosed() {
                if (closed) {
                    throw new IllegalStateException("Image has been closed");
                }
            }
            
            @Override
            public void close() {
                if (!closed) {
                    arena.close();
                    closed = true;
                }
            }
        }
        
        // Demonstration
        public static void demonstrateImageProcessing() {
            System.out.println("=== Image Processing Library Demo ===");
            
            try (NativeResourceManager resourceManager = new NativeResourceManager()) {
                ImageProcessingLibrary imageLib = NativeLibraryManager.getInstance()
                    .loadLibrary(ImageProcessingLibrary.class, "imageproc", "1.0");
                
                // Create test image (256x256 RGB)
                int width = 256, height = 256, channels = 3;
                byte[] testImageData = generateTestImage(width, height, channels);
                
                NativeImage inputImage = resourceManager.manage(
                    new NativeImage(testImageData, width, height, channels));
                NativeImage blurredImage = resourceManager.manage(
                    new NativeImage(width, height, channels));
                NativeImage edgeImage = resourceManager.manage(
                    new NativeImage(width, height, channels));
                
                // Apply Gaussian blur
                long startTime = System.nanoTime();
                imageLib.applyGaussianBlur(inputImage, blurredImage, 2.0f);
                long blurTime = System.nanoTime() - startTime;
                
                // Apply edge detection
                startTime = System.nanoTime();
                imageLib.applyEdgeDetection(inputImage, edgeImage, 0.1f);
                long edgeTime = System.nanoTime() - startTime;
                
                System.out.printf("Gaussian blur: %.2f ms%n", blurTime / 1_000_000.0);
                System.out.printf("Edge detection: %.2f ms%n", edgeTime / 1_000_000.0);
                
                // Verify results
                byte[] blurredData = blurredImage.toByteArray();
                byte[] edgeData = edgeImage.toByteArray();
                
                System.out.printf("Processed %d pixels successfully%n", width * height);
                
            } catch (Exception e) {
                System.err.println("Image processing demo failed: " + e.getMessage());
            }
        }
        
        private static byte[] generateTestImage(int width, int height, int channels) {
            byte[] imageData = new byte[width * height * channels];
            Random random = new Random(42);
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int baseIndex = (y * width + x) * channels;
                    
                    // Generate gradient pattern with noise
                    int value = (x + y) % 256;
                    
                    for (int c = 0; c < channels; c++) {
                        imageData[baseIndex + c] = (byte) (value + random.nextInt(50) - 25);
                    }
                }
            }
            
            return imageData;
        }
    }
}
```

### Performance Benchmarking and Optimization

**Comprehensive Performance Analysis:**
```java
// Performance benchmarking framework for Panama FFI
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.OPERATIONS_PER_SECOND)
@State(Scope.Benchmark)
public class PanamaPerformanceBenchmarks {
    
    @Param({"1000", "10000", "100000", "1000000"})
    private int dataSize;
    
    private double[] javaArray;
    private MemorySegment nativeSegment;
    private Arena benchmarkArena;
    
    // Method handles for native math functions
    private static final MethodHandle nativeSin;
    private static final MethodHandle nativeCos;
    private static final MethodHandle nativeSqrt;
    
    static {
        try {
            Linker linker = Linker.nativeLinker();
            SymbolLookup mathLib = linker.defaultLookup();
            
            FunctionDescriptor doubleToDouble = FunctionDescriptor.of(
                ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE);
            
            nativeSin = linker.downcallHandle(mathLib.find("sin").orElseThrow(), doubleToDouble);
            nativeCos = linker.downcallHandle(mathLib.find("cos").orElseThrow(), doubleToDouble);
            nativeSqrt = linker.downcallHandle(mathLib.find("sqrt").orElseThrow(), doubleToDouble);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize native math functions", e);
        }
    }
    
    @Setup
    public void setup() {
        benchmarkArena = Arena.ofConfined();
        javaArray = new double[dataSize];
        nativeSegment = benchmarkArena.allocateArray(ValueLayout.JAVA_DOUBLE, dataSize);
        
        // Initialize with test data
        Random random = new Random(42);
        for (int i = 0; i < dataSize; i++) {
            double value = random.nextDouble() * Math.PI;
            javaArray[i] = value;
            nativeSegment.setAtIndex(ValueLayout.JAVA_DOUBLE, i, value);
        }
    }
    
    @TearDown
    public void tearDown() {
        benchmarkArena.close();
    }
    
    // Memory access benchmarks
    @Benchmark
    public double javaArrayAccess() {
        double sum = 0;
        for (int i = 0; i < dataSize; i++) {
            sum += javaArray[i];
        }
        return sum;
    }
    
    @Benchmark
    public double nativeMemoryAccess() {
        double sum = 0;
        for (int i = 0; i < dataSize; i++) {
            sum += nativeSegment.getAtIndex(ValueLayout.JAVA_DOUBLE, i);
        }
        return sum;
    }
    
    @Benchmark
    public void javaArrayWrite() {
        for (int i = 0; i < dataSize; i++) {
            javaArray[i] = i * 0.001;
        }
    }
    
    @Benchmark
    public void nativeMemoryWrite() {
        for (int i = 0; i < dataSize; i++) {
            nativeSegment.setAtIndex(ValueLayout.JAVA_DOUBLE, i, i * 0.001);
        }
    }
    
    // Math function benchmarks
    @Benchmark
    public void javaMathSin() {
        for (int i = 0; i < dataSize; i++) {
            javaArray[i] = Math.sin(javaArray[i]);
        }
    }
    
    @Benchmark
    public void nativeMathSin() {
        try {
            for (int i = 0; i < dataSize; i++) {
                double input = nativeSegment.getAtIndex(ValueLayout.JAVA_DOUBLE, i);
                double result = (double) nativeSin.invokeExact(input);
                nativeSegment.setAtIndex(ValueLayout.JAVA_DOUBLE, i, result);
            }
        } catch (Throwable e) {
            throw new RuntimeException("Native sin benchmark failed", e);
        }
    }
    
    @Benchmark
    public void javaMathCombined() {
        for (int i = 0; i < dataSize; i++) {
            double x = javaArray[i];
            javaArray[i] = Math.sin(x) * Math.cos(x) + Math.sqrt(Math.abs(x));
        }
    }
    
    @Benchmark
    public void nativeMathCombined() {
        try {
            for (int i = 0; i < dataSize; i++) {
                double x = nativeSegment.getAtIndex(ValueLayout.JAVA_DOUBLE, i);
                double sin = (double) nativeSin.invokeExact(x);
                double cos = (double) nativeCos.invokeExact(x);
                double sqrt = (double) nativeSqrt.invokeExact(Math.abs(x));
                double result = sin * cos + sqrt;
                nativeSegment.setAtIndex(ValueLayout.JAVA_DOUBLE, i, result);
            }
        } catch (Throwable e) {
            throw new RuntimeException("Native combined math benchmark failed", e);
        }
    }
    
    // Memory copy benchmarks
    @Benchmark
    public void javaArrayCopy() {
        double[] temp = new double[dataSize];
        System.arraycopy(javaArray, 0, temp, 0, dataSize);
    }
    
    @Benchmark
    public void nativeMemoryCopy() {
        MemorySegment temp = benchmarkArena.allocateArray(ValueLayout.JAVA_DOUBLE, dataSize);
        MemorySegment.copy(nativeSegment, ValueLayout.JAVA_DOUBLE, 0,
                          temp, ValueLayout.JAVA_DOUBLE, 0, dataSize);
    }
    
    // Cross-boundary benchmarks
    @Benchmark
    public void javaToNativeCopy() {
        MemorySegment.copy(MemorySegment.ofArray(javaArray), 0,
                          nativeSegment, ValueLayout.JAVA_DOUBLE, 0, dataSize);
    }
    
    @Benchmark
    public void nativeToJavaCopy() {
        double[] temp = new double[dataSize];
        MemorySegment.copy(nativeSegment, ValueLayout.JAVA_DOUBLE, 0,
                          MemorySegment.ofArray(temp), 0, dataSize);
    }
    
    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(PanamaPerformanceBenchmarks.class.getSimpleName())
                .forks(1)
                .warmupIterations(3)
                .measurementIterations(5)
                .build();
        
        new Runner(opt).run();
    }
}
```

## Best Practices and Guidelines

### Foreign Function Interface Best Practices
1. **Resource Management**: Always use Arena for automatic memory cleanup
2. **Error Handling**: Implement robust error handling for native calls
3. **Type Safety**: Use appropriate ValueLayout for type-safe memory access
4. **Platform Compatibility**: Design for cross-platform deployment
5. **Performance**: Cache MethodHandles and minimize boundary crossings

### Memory Management Guidelines
1. **Arena Selection**: Choose appropriate arena scope (confined, shared, global)
2. **Memory Alignment**: Consider alignment requirements for optimal performance
3. **Bounds Checking**: Implement proper bounds checking for safety
4. **Resource Cleanup**: Ensure proper cleanup of native resources
5. **Memory Reuse**: Reuse memory segments when possible to reduce allocation overhead

## Assessment and Exercises

### Exercise 1: Custom Native Library Integration
Create a complete integration with a native library of your choice, including proper error handling, resource management, and performance optimization.

### Exercise 2: Cross-Platform Deployment
Design and implement a cross-platform deployment strategy for Panama-based applications, including library packaging and loading.

### Exercise 3: High-Performance Data Processing
Build a high-performance data processing pipeline that leverages native libraries for computational tasks.

### Exercise 4: Memory-Mapped File Processing
Implement a memory-mapped file processing system using Foreign Memory API for handling large datasets.

## Summary

Day 158 provided comprehensive coverage of Project Panama's Foreign Function & Memory API, enabling seamless integration with native libraries and efficient off-heap memory management. Students learned to build high-performance native integrations, implement cross-platform compatibility, and manage resources effectively.

Key achievements:
- Mastery of Foreign Function Interface for native library integration
- Advanced off-heap memory management with MemorySegment and Arena
- Cross-platform native library loading and deployment strategies
- Performance optimization techniques for native interoperability
- Production-ready resource management and error handling

This knowledge prepares students for the next lesson on Project Valhalla's value types and specialized generics, enabling memory-efficient and high-performance Java applications.