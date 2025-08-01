# Day 156: GraalVM Advanced - Native Compilation and Polyglot Programming

## Learning Objectives
By the end of this session, you will:
- Master advanced GraalVM native image configuration and optimization techniques
- Implement polyglot programming with JavaScript, Python, and R integration
- Create custom language implementations using the Truffle framework
- Understand GraalVM enterprise features and cloud-native deployment strategies
- Perform comprehensive performance benchmarking and memory optimization

## Theoretical Foundation

### GraalVM Architecture Deep Dive

**GraalVM Components:**
```java
// Understanding GraalVM architecture components
public class GraalVMArchitecture {
    
    /**
     * GraalVM Stack:
     * 1. Graal Compiler - High-performance JIT compiler
     * 2. Substrate VM - Native image runtime
     * 3. Truffle Framework - Language implementation framework
     * 4. LLVM Runtime - Native code execution
     * 5. Polyglot API - Cross-language interoperability
     */
    
    public void demonstrateGraalComponents() {
        // Check if running on GraalVM
        String vmName = System.getProperty("java.vm.name");
        String vmVersion = System.getProperty("java.vm.version");
        
        System.out.printf("VM Name: %s%n", vmName);
        System.out.printf("VM Version: %s%n", vmVersion);
        System.out.printf("Is GraalVM: %s%n", vmName.contains("GraalVM"));
        
        // Check available languages
        System.out.println("Available languages:");
        if (isPolyglotAvailable()) {
            printAvailableLanguages();
        }
    }
    
    private boolean isPolyglotAvailable() {
        try {
            Class.forName("org.graalvm.polyglot.Context");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private void printAvailableLanguages() {
        try {
            // Using reflection to avoid compile-time dependency
            Class<?> engineClass = Class.forName("org.graalvm.polyglot.Engine");
            Object engine = engineClass.getMethod("create").invoke(null);
            Object languages = engineClass.getMethod("getLanguages").invoke(engine);
            
            System.out.println("Supported languages: " + languages);
        } catch (Exception e) {
            System.out.println("Could not determine available languages");
        }
    }
}
```

### Advanced Native Image Configuration

**Comprehensive Native Image Builder:**
```java
// Advanced native image configuration and optimization
public class AdvancedNativeImageBuilder {
    
    // Reflection configuration for native images
    public static class ReflectionConfigGenerator {
        
        public void generateReflectionConfig(Class<?>... classes) {
            JsonArrayBuilder configArray = Json.createArrayBuilder();
            
            for (Class<?> clazz : classes) {
                JsonObjectBuilder classConfig = Json.createObjectBuilder()
                    .add("name", clazz.getName())
                    .add("allDeclaredConstructors", true)
                    .add("allDeclaredMethods", true)
                    .add("allDeclaredFields", true);
                
                // Add method-specific configurations
                JsonArrayBuilder methods = Json.createArrayBuilder();
                for (Method method : clazz.getDeclaredMethods()) {
                    if (shouldIncludeMethod(method)) {
                        JsonObjectBuilder methodConfig = Json.createObjectBuilder()
                            .add("name", method.getName());
                        
                        // Add parameter types
                        JsonArrayBuilder paramTypes = Json.createArrayBuilder();
                        for (Class<?> paramType : method.getParameterTypes()) {
                            paramTypes.add(paramType.getName());
                        }
                        methodConfig.add("parameterTypes", paramTypes);
                        
                        methods.add(methodConfig);
                    }
                }
                classConfig.add("methods", methods);
                
                configArray.add(classConfig);
            }
            
            // Write configuration to file
            writeConfigurationFile("reflect-config.json", configArray.build());
        }
        
        private boolean shouldIncludeMethod(Method method) {
            // Include public methods and methods with specific annotations
            return Modifier.isPublic(method.getModifiers()) ||
                   method.isAnnotationPresent(JsonProperty.class) ||
                   method.getName().startsWith("get") ||
                   method.getName().startsWith("set");
        }
        
        private void writeConfigurationFile(String filename, JsonValue config) {
            try (FileWriter writer = new FileWriter(filename)) {
                JsonWriter jsonWriter = Json.createWriter(writer);
                jsonWriter.write(config);
                System.out.printf("Generated %s%n", filename);
            } catch (IOException e) {
                System.err.printf("Failed to write %s: %s%n", filename, e.getMessage());
            }
        }
    }
    
    // Resource configuration for native images
    public static class ResourceConfigGenerator {
        
        public void generateResourceConfig(String... resourcePatterns) {
            JsonObjectBuilder config = Json.createObjectBuilder();
            JsonArrayBuilder bundles = Json.createArrayBuilder();
            JsonArrayBuilder resources = Json.createArrayBuilder();
            
            // Add resource bundles
            bundles.add(Json.createObjectBuilder()
                .add("name", "messages")
                .add("locales", Json.createArrayBuilder()
                    .add("en")
                    .add("de")
                    .add("fr")));
            
            // Add resource patterns
            for (String pattern : resourcePatterns) {
                resources.add(Json.createObjectBuilder()
                    .add("pattern", pattern));
            }
            
            config.add("bundles", bundles);
            config.add("resources", Json.createObjectBuilder()
                .add("includes", resources));
            
            writeConfigurationFile("resource-config.json", config.build());
        }
    }
    
    // Native image build configuration
    public static class NativeImageBuilder {
        private final List<String> buildArgs = new ArrayList<>();
        
        public NativeImageBuilder enableOptimizations() {
            buildArgs.add("--optimize=2");
            buildArgs.add("-march=native");
            buildArgs.add("--gc=G1");
            return this;
        }
        
        public NativeImageBuilder enableMemoryOptimizations() {
            buildArgs.add("-H:+UnlockExperimentalVMOptions");
            buildArgs.add("-H:+UseCompressedOops");
            buildArgs.add("-H:MaxHeapSize=1g");
            buildArgs.add("-H:MinHeapSize=256m");
            return this;
        }
        
        public NativeImageBuilder enableSecurityFeatures() {
            buildArgs.add("--enable-all-security-services");
            buildArgs.add("-H:+EnableSecurityServicesFeature");
            return this;
        }
        
        public NativeImageBuilder addReflectionConfig(String configPath) {
            buildArgs.add("-H:ReflectionConfigurationFiles=" + configPath);
            return this;
        }
        
        public NativeImageBuilder addResourceConfig(String configPath) {
            buildArgs.add("-H:ResourceConfigurationFiles=" + configPath);
            return this;
        }
        
        public NativeImageBuilder enableStaticBuild() {
            buildArgs.add("--static");
            buildArgs.add("--libc=musl");
            return this;
        }
        
        public void build(String mainClass, String outputName) {
            List<String> command = new ArrayList<>();
            command.add("native-image");
            command.addAll(buildArgs);
            command.add(mainClass);
            command.add(outputName);
            
            System.out.println("Building native image with command:");
            System.out.println(String.join(" ", command));
            
            // Execute build command
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO();
            
            try {
                Process process = pb.start();
                int exitCode = process.waitFor();
                
                if (exitCode == 0) {
                    System.out.printf("Successfully built native image: %s%n", outputName);
                } else {
                    System.err.printf("Native image build failed with exit code: %d%n", exitCode);
                }
            } catch (IOException | InterruptedException e) {
                System.err.printf("Build process failed: %s%n", e.getMessage());
            }
        }
    }
}
```

### Polyglot Programming Implementation

**Multi-Language Integration Framework:**
```java
// Advanced polyglot programming with GraalVM
public class PolyglotIntegration {
    
    // JavaScript integration
    public static class JavaScriptIntegration {
        private final Context context;
        
        public JavaScriptIntegration() {
            this.context = Context.newBuilder("js")
                .allowAllAccess(true)
                .allowIO(IOAccess.ALL)
                .build();
        }
        
        public void integrateWithJavaScript() {
            // Define Java objects accessible from JavaScript
            Value bindings = context.getBindings("js");
            bindings.putMember("javaAPI", new JavaScriptAPI());
            
            // Execute JavaScript code that uses Java objects
            String jsCode = """
                // JavaScript accessing Java functionality
                const result = javaAPI.processData(['hello', 'world', 'graalvm']);
                console.log('Processed data:', result);
                
                // Define JavaScript function for Java to call
                function calculateMetrics(data) {
                    return {
                        sum: data.reduce((a, b) => a + b, 0),
                        average: data.reduce((a, b) => a + b, 0) / data.length,
                        max: Math.max(...data),
                        min: Math.min(...data)
                    };
                }
                
                // Return the function to Java
                calculateMetrics;
                """;
            
            Value jsFunction = context.eval("js", jsCode);
            
            // Call JavaScript function from Java
            double[] data = {1.5, 2.3, 4.7, 3.1, 5.9, 2.8};
            Value result = jsFunction.execute(ProxyArray.fromArray(data));
            
            System.out.printf("JavaScript calculated metrics: %s%n", result);
        }
        
        public static class JavaScriptAPI {
            public List<String> processData(String[] input) {
                return Arrays.stream(input)
                    .map(String::toUpperCase)
                    .map(s -> "Processed: " + s)
                    .collect(Collectors.toList());
            }
        }
    }
    
    // Python integration
    public static class PythonIntegration {
        private final Context context;
        
        public PythonIntegration() {
            this.context = Context.newBuilder("python")
                .allowAllAccess(true)
                .allowIO(IOAccess.ALL)
                .build();
        }
        
        public void integrateWithPython() {
            // Python data analysis integration
            String pythonCode = """
                import math
                
                def analyze_data(data):
                    # Statistical analysis using Python
                    n = len(data)
                    mean = sum(data) / n
                    variance = sum((x - mean) ** 2 for x in data) / n
                    std_dev = math.sqrt(variance)
                    
                    return {
                        'count': n,
                        'mean': mean,
                        'variance': variance,
                        'std_dev': std_dev,
                        'skewness': calculate_skewness(data, mean, std_dev)
                    }
                
                def calculate_skewness(data, mean, std_dev):
                    n = len(data)
                    if std_dev == 0:
                        return 0
                    return (sum((x - mean) ** 3 for x in data) / n) / (std_dev ** 3)
                
                # Return the analysis function
                analyze_data
                """;
            
            Value pythonFunction = context.eval("python", pythonCode);
            
            // Pass data from Java to Python
            double[] javaData = {1.2, 2.3, 1.8, 2.9, 3.1, 2.4, 1.9, 2.7, 3.3, 2.1};
            Value analysisResult = pythonFunction.execute(ProxyArray.fromArray(javaData));
            
            System.out.println("Python statistical analysis:");
            System.out.printf("Count: %.0f%n", analysisResult.getMember("count").asDouble());
            System.out.printf("Mean: %.3f%n", analysisResult.getMember("mean").asDouble());
            System.out.printf("Standard Deviation: %.3f%n", 
                analysisResult.getMember("std_dev").asDouble());
            System.out.printf("Skewness: %.3f%n", 
                analysisResult.getMember("skewness").asDouble());
        }
    }
    
    // R integration for statistical computing
    public static class RIntegration {
        private final Context context;
        
        public RIntegration() {
            this.context = Context.newBuilder("R")
                .allowAllAccess(true)
                .allowIO(IOAccess.ALL)
                .build();
        }
        
        public void integrateWithR() {
            // R statistical modeling
            String rCode = """
                # Advanced statistical modeling in R
                perform_regression_analysis <- function(x_data, y_data) {
                    # Create data frame
                    df <- data.frame(x = x_data, y = y_data)
                    
                    # Perform linear regression
                    model <- lm(y ~ x, data = df)
                    
                    # Calculate additional statistics
                    correlation <- cor(x_data, y_data)
                    r_squared <- summary(model)$r.squared
                    p_value <- summary(model)$coefficients[2, 4]
                    
                    # Return comprehensive results
                    list(
                        coefficients = model$coefficients,
                        r_squared = r_squared,
                        correlation = correlation,
                        p_value = p_value,
                        residuals = model$residuals,
                        fitted_values = model$fitted.values
                    )
                }
                
                perform_regression_analysis
                """;
            
            Value rFunction = context.eval("R", rCode);
            
            // Generate sample data in Java
            double[] xData = IntStream.range(1, 21)
                .mapToDouble(i -> i + Math.random() * 2)
                .toArray();
            
            double[] yData = Arrays.stream(xData)
                .map(x -> 2.5 * x + 10 + Math.random() * 5)
                .toArray();
            
            // Perform regression analysis in R
            Value rResult = rFunction.execute(
                ProxyArray.fromArray(xData),
                ProxyArray.fromArray(yData)
            );
            
            System.out.println("R Regression Analysis Results:");
            Value coefficients = rResult.getMember("coefficients");
            System.out.printf("Intercept: %.3f%n", 
                coefficients.getArrayElement(0).asDouble());
            System.out.printf("Slope: %.3f%n", 
                coefficients.getArrayElement(1).asDouble());
            System.out.printf("R-squared: %.3f%n", 
                rResult.getMember("r_squared").asDouble());
            System.out.printf("Correlation: %.3f%n", 
                rResult.getMember("correlation").asDouble());
            System.out.printf("P-value: %.6f%n", 
                rResult.getMember("p_value").asDouble());
        }
    }
    
    // Cross-language data sharing
    public static class CrossLanguageDataSharing {
        
        public void demonstrateDataSharing() {
            try (Context context = Context.newBuilder()
                    .allowAllAccess(true)
                    .build()) {
                
                // Create shared data structure
                Map<String, Object> sharedData = new HashMap<>();
                sharedData.put("numbers", new double[]{1, 2, 3, 4, 5});
                sharedData.put("text", "Hello from Java");
                sharedData.put("timestamp", System.currentTimeMillis());
                
                Value bindings = context.getBindings("js");
                bindings.putMember("shared", ProxyObject.fromMap(sharedData));
                
                // JavaScript processing
                context.eval("js", """
                    console.log('Shared data from Java:', shared);
                    shared.processedNumbers = shared.numbers.map(n => n * 2);
                    shared.processedText = shared.text.toUpperCase();
                    """);
                
                // Python processing (if available)
                if (context.getEngine().getLanguages().containsKey("python")) {
                    context.eval("python", """
                        import polyglot
                        shared = polyglot.import_value("shared")
                        shared['mean'] = sum(shared['numbers']) / len(shared['numbers'])
                        shared['std'] = (sum((x - shared['mean'])**2 for x in shared['numbers']) / len(shared['numbers']))**0.5
                        """);
                }
                
                // Retrieve processed data back to Java
                System.out.println("Data after cross-language processing:");
                System.out.println("Original numbers: " + 
                    Arrays.toString((double[]) sharedData.get("numbers")));
                
                if (sharedData.containsKey("processedNumbers")) {
                    System.out.println("Processed by JS: " + sharedData.get("processedNumbers"));
                }
                if (sharedData.containsKey("mean")) {
                    System.out.println("Mean calculated by Python: " + sharedData.get("mean"));
                }
            }
        }
    }
}
```

### Truffle Framework for Language Implementation

**Custom Language Implementation:**
```java
// Custom language implementation using Truffle framework
@TruffleLanguage.Registration(
    id = "simplelang",
    name = "Simple Language",
    version = "1.0",
    characterMimeTypes = "application/x-simple-lang"
)
public class SimpleLangTruffleLanguage extends TruffleLanguage<SimpleLangContext> {
    
    @Override
    protected SimpleLangContext createContext(Env env) {
        return new SimpleLangContext(env);
    }
    
    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        Source source = request.getSource();
        SimpleLangParser parser = new SimpleLangParser();
        SimpleLangNode programNode = parser.parse(source);
        return Truffle.getRuntime().createCallTarget(new SimpleLangRootNode(this, programNode));
    }
    
    // Language context
    public static class SimpleLangContext {
        private final Env env;
        private final Map<String, Object> globalScope = new HashMap<>();
        
        public SimpleLangContext(Env env) {
            this.env = env;
        }
        
        public Object getGlobalVariable(String name) {
            return globalScope.get(name);
        }
        
        public void setGlobalVariable(String name, Object value) {
            globalScope.put(name, value);
        }
        
        public Env getEnv() {
            return env;
        }
    }
    
    // Root node for execution
    public static class SimpleLangRootNode extends RootNode {
        @Child private SimpleLangNode body;
        
        public SimpleLangRootNode(TruffleLanguage<?> language, SimpleLangNode body) {
            super(language);
            this.body = body;
        }
        
        @Override
        public Object execute(VirtualFrame frame) {
            return body.execute(frame);
        }
    }
    
    // Base node for AST
    public abstract static class SimpleLangNode extends Node {
        public abstract Object execute(VirtualFrame frame);
    }
    
    // Literal node implementation
    public static class SimpleLangLiteralNode extends SimpleLangNode {
        private final Object value;
        
        public SimpleLangLiteralNode(Object value) {
            this.value = value;
        }
        
        @Override
        public Object execute(VirtualFrame frame) {
            return value;
        }
    }
    
    // Binary operation node
    public static class SimpleLangBinaryOpNode extends SimpleLangNode {
        @Child private SimpleLangNode left;
        @Child private SimpleLangNode right;
        private final String operator;
        
        public SimpleLangBinaryOpNode(SimpleLangNode left, SimpleLangNode right, String operator) {
            this.left = left;
            this.right = right;
            this.operator = operator;
        }
        
        @Override
        public Object execute(VirtualFrame frame) {
            Object leftValue = left.execute(frame);
            Object rightValue = right.execute(frame);
            
            if (leftValue instanceof Number && rightValue instanceof Number) {
                double l = ((Number) leftValue).doubleValue();
                double r = ((Number) rightValue).doubleValue();
                
                return switch (operator) {
                    case "+" -> l + r;
                    case "-" -> l - r;
                    case "*" -> l * r;
                    case "/" -> l / r;
                    case "%" -> l % r;
                    default -> throw new RuntimeException("Unknown operator: " + operator);
                };
            }
            
            throw new RuntimeException("Invalid operand types for " + operator);
        }
    }
    
    // Variable access node
    public static class SimpleLangVariableNode extends SimpleLangNode {
        private final String name;
        
        public SimpleLangVariableNode(String name) {
            this.name = name;
        }
        
        @Override
        public Object execute(VirtualFrame frame) {
            SimpleLangContext context = getCurrentContext();
            Object value = context.getGlobalVariable(name);
            if (value == null) {
                throw new RuntimeException("Undefined variable: " + name);
            }
            return value;
        }
        
        private SimpleLangContext getCurrentContext() {
            return SimpleLangTruffleLanguage.getCurrentContext(SimpleLangTruffleLanguage.class);
        }
    }
    
    // Assignment node
    public static class SimpleLangAssignmentNode extends SimpleLangNode {
        private final String name;
        @Child private SimpleLangNode value;
        
        public SimpleLangAssignmentNode(String name, SimpleLangNode value) {
            this.name = name;
            this.value = value;
        }
        
        @Override
        public Object execute(VirtualFrame frame) {
            Object val = value.execute(frame);
            SimpleLangContext context = getCurrentContext();
            context.setGlobalVariable(name, val);
            return val;
        }
    }
    
    // Simple parser for the language
    public static class SimpleLangParser {
        public SimpleLangNode parse(Source source) {
            // Simplified parsing logic
            String code = source.getCharacters().toString().trim();
            
            // Handle assignment: var = expression
            if (code.contains("=")) {
                String[] parts = code.split("=", 2);
                String varName = parts[0].trim();
                String expression = parts[1].trim();
                
                SimpleLangNode valueNode = parseExpression(expression);
                return new SimpleLangAssignmentNode(varName, valueNode);
            }
            
            // Handle expression
            return parseExpression(code);
        }
        
        private SimpleLangNode parseExpression(String expression) {
            expression = expression.trim();
            
            // Handle binary operations (simplified)
            for (String op : new String[]{"+", "-", "*", "/", "%"}) {
                int opIndex = expression.lastIndexOf(op);
                if (opIndex > 0 && opIndex < expression.length() - 1) {
                    String left = expression.substring(0, opIndex).trim();
                    String right = expression.substring(opIndex + 1).trim();
                    
                    return new SimpleLangBinaryOpNode(
                        parseExpression(left),
                        parseExpression(right),
                        op
                    );
                }
            }
            
            // Handle literals and variables
            try {
                double value = Double.parseDouble(expression);
                return new SimpleLangLiteralNode(value);
            } catch (NumberFormatException e) {
                return new SimpleLangVariableNode(expression);
            }
        }
    }
}
```

## Performance Optimization and Benchmarking

### Native Image Performance Analysis

**Comprehensive Performance Testing Framework:**
```java
// Performance benchmarking for native images
public class NativeImagePerformanceAnalyzer {
    
    public static class StartupTimeBenchmark {
        
        public void benchmarkStartupTime(String nativeImagePath, String jarPath, int iterations) {
            System.out.println("=== Startup Time Benchmark ===");
            
            long[] nativeStartupTimes = new long[iterations];
            long[] jvmStartupTimes = new long[iterations];
            
            // Benchmark native image startup
            for (int i = 0; i < iterations; i++) {
                nativeStartupTimes[i] = measureStartupTime(nativeImagePath, true);
                Thread.sleep(1000); // Cool-down period
            }
            
            // Benchmark JVM startup
            for (int i = 0; i < iterations; i++) {
                jvmStartupTimes[i] = measureStartupTime("java -jar " + jarPath, false);
                Thread.sleep(1000); // Cool-down period
            }
            
            // Calculate statistics
            StatisticalSummary nativeStats = calculateStats(nativeStartupTimes);
            StatisticalSummary jvmStats = calculateStats(jvmStartupTimes);
            
            // Print results
            printStartupResults("Native Image", nativeStats);
            printStartupResults("JVM", jvmStats);
            
            double improvement = (jvmStats.mean - nativeStats.mean) / jvmStats.mean * 100;
            System.out.printf("Native image startup improvement: %.1f%%%n", improvement);
        }
        
        private long measureStartupTime(String command, boolean isNative) {
            try {
                long startTime = System.nanoTime();
                
                ProcessBuilder pb = new ProcessBuilder(command.split(" "));
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                pb.redirectError(ProcessBuilder.Redirect.DISCARD);
                
                Process process = pb.start();
                int exitCode = process.waitFor();
                
                long endTime = System.nanoTime();
                
                if (exitCode != 0) {
                    System.err.printf("Process failed with exit code: %d%n", exitCode);
                    return -1;
                }
                
                return (endTime - startTime) / 1_000_000; // Convert to milliseconds
                
            } catch (IOException | InterruptedException e) {
                System.err.printf("Failed to measure startup time: %s%n", e.getMessage());
                return -1;
            }
        }
        
        private void printStartupResults(String name, StatisticalSummary stats) {
            System.out.printf("%s Startup Times:%n", name);
            System.out.printf("  Mean: %.2f ms%n", stats.mean);
            System.out.printf("  Min: %.2f ms%n", stats.min);
            System.out.printf("  Max: %.2f ms%n", stats.max);
            System.out.printf("  Std Dev: %.2f ms%n", stats.stdDev);
            System.out.println();
        }
    }
    
    public static class MemoryUsageBenchmark {
        
        public void benchmarkMemoryUsage() {
            System.out.println("=== Memory Usage Analysis ===");
            
            // Measure initial memory
            long initialMemory = getUsedMemory();
            System.out.printf("Initial memory usage: %d MB%n", initialMemory / (1024 * 1024));
            
            // Perform memory-intensive operations
            List<Object> memoryConsumers = new ArrayList<>();
            
            for (int i = 0; i < 1000; i++) {
                memoryConsumers.add(createMemoryIntensiveObject());
                
                if (i % 100 == 0) {
                    long currentMemory = getUsedMemory();
                    System.out.printf("Memory after %d objects: %d MB%n", 
                        i, currentMemory / (1024 * 1024));
                }
            }
            
            // Force garbage collection and measure
            System.gc();
            Thread.sleep(1000);
            
            long afterGCMemory = getUsedMemory();
            System.out.printf("Memory after GC: %d MB%n", afterGCMemory / (1024 * 1024));
            
            // Clear references and measure again
            memoryConsumers.clear();
            System.gc();
            Thread.sleep(1000);
            
            long finalMemory = getUsedMemory();
            System.out.printf("Final memory usage: %d MB%n", finalMemory / (1024 * 1024));
        }
        
        private long getUsedMemory() {
            Runtime runtime = Runtime.getRuntime();
            return runtime.totalMemory() - runtime.freeMemory();
        }
        
        private Object createMemoryIntensiveObject() {
            // Create object that consumes significant memory
            Map<String, Object> object = new HashMap<>();
            
            for (int i = 0; i < 100; i++) {
                object.put("key" + i, new byte[1024]); // 1KB per entry
            }
            
            return object;
        }
    }
    
    public static class ThroughputBenchmark {
        
        @Benchmark
        @BenchmarkMode(Mode.Throughput)
        @OutputTimeUnit(TimeUnit.OPERATIONS_PER_SECOND)
        public void measureComputationThroughput() {
            // CPU-intensive computation
            double result = 0;
            for (int i = 0; i < 10000; i++) {
                result += Math.sqrt(i) * Math.sin(i) * Math.cos(i);
            }
            // Use result to prevent optimization
            BlackHole.consumeCPU(Double.doubleToLongBits(result));
        }
        
        @Benchmark
        @BenchmarkMode(Mode.AverageTime)
        @OutputTimeUnit(TimeUnit.MICROSECONDS)
        public String measureStringProcessing() {
            String text = "The quick brown fox jumps over the lazy dog";
            
            return Arrays.stream(text.split(" "))
                .map(String::toUpperCase)
                .map(s -> new StringBuilder(s).reverse().toString())
                .collect(Collectors.joining("-"));
        }
        
        @Benchmark
        @BenchmarkMode(Mode.Throughput)
        @OutputTimeUnit(TimeUnit.OPERATIONS_PER_SECOND)
        public List<Integer> measureCollectionOperations() {
            List<Integer> numbers = IntStream.range(0, 1000)
                .boxed()
                .collect(Collectors.toList());
            
            return numbers.stream()
                .filter(n -> n % 2 == 0)
                .map(n -> n * n)
                .sorted(Comparator.reverseOrder())
                .limit(100)
                .collect(Collectors.toList());
        }
    }
    
    // Statistical summary class
    public static class StatisticalSummary {
        public final double mean;
        public final double min;
        public final double max;
        public final double stdDev;
        
        public StatisticalSummary(double mean, double min, double max, double stdDev) {
            this.mean = mean;
            this.min = min;
            this.max = max;
            this.stdDev = stdDev;
        }
    }
    
    private static StatisticalSummary calculateStats(long[] values) {
        double sum = Arrays.stream(values).sum();
        double mean = sum / values.length;
        
        long min = Arrays.stream(values).min().orElse(0);
        long max = Arrays.stream(values).max().orElse(0);
        
        double variance = Arrays.stream(values)
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0);
        
        double stdDev = Math.sqrt(variance);
        
        return new StatisticalSummary(mean, min, max, stdDev);
    }
}
```

### Cloud-Native Deployment Optimization

**Container-Optimized Native Images:**
```java
// Cloud-native deployment optimization
public class CloudNativeOptimizer {
    
    public static class ContainerizedNativeImage {
        
        public void generateDockerfile(String imageName, String nativeExecutable) {
            String dockerfile = String.format("""
                # Multi-stage build for optimized native image
                FROM ghcr.io/graalvm/graalvm-ce:latest AS build
                
                # Install native-image
                RUN gu install native-image
                
                # Copy source code
                COPY . /app
                WORKDIR /app
                
                # Build native image with optimizations
                RUN native-image \\
                    --no-fallback \\
                    --enable-preview \\
                    --optimize=2 \\
                    --gc=G1 \\
                    -march=x86-64 \\
                    --static \\
                    --libc=musl \\
                    -H:+UnlockExperimentalVMOptions \\
                    -H:+UseCompressedOops \\
                    -H:MaxHeapSize=256m \\
                    -H:+ReportExceptionStackTraces \\
                    -H:+PrintGCDetails \\
                    -jar target/%s.jar \\
                    %s
                
                # Production stage with minimal base image
                FROM scratch
                
                # Copy native executable
                COPY --from=build /app/%s /app
                
                # Set up non-root user
                USER 1000:1000
                
                # Expose port
                EXPOSE 8080
                
                # Health check
                HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \\
                    CMD ["/app", "--health-check"]
                
                # Run the application
                ENTRYPOINT ["/app"]
                """, imageName, nativeExecutable, nativeExecutable);
            
            writeToFile("Dockerfile", dockerfile);
            System.out.println("Generated optimized Dockerfile for native image");
        }
        
        public void generateKubernetesManifests(String appName, String imageName) {
            // Deployment manifest
            String deployment = String.format("""
                apiVersion: apps/v1
                kind: Deployment
                metadata:
                  name: %s
                  labels:
                    app: %s
                spec:
                  replicas: 3
                  selector:
                    matchLabels:
                      app: %s
                  template:
                    metadata:
                      labels:
                        app: %s
                    spec:
                      containers:
                      - name: %s
                        image: %s:latest
                        ports:
                        - containerPort: 8080
                        resources:
                          requests:
                            memory: "32Mi"
                            cpu: "10m"
                          limits:
                            memory: "128Mi"
                            cpu: "100m"
                        livenessProbe:
                          httpGet:
                            path: /health
                            port: 8080
                          initialDelaySeconds: 5
                          periodSeconds: 10
                        readinessProbe:
                          httpGet:
                            path: /ready
                            port: 8080
                          initialDelaySeconds: 2
                          periodSeconds: 5
                        env:
                        - name: JAVA_OPTS
                          value: "-XX:MaxRAMPercentage=80"
                """, appName, appName, appName, appName, appName, imageName);
            
            // Service manifest
            String service = String.format("""
                ---
                apiVersion: v1
                kind: Service
                metadata:
                  name: %s-service
                spec:
                  selector:
                    app: %s
                  ports:
                  - protocol: TCP
                    port: 80
                    targetPort: 8080
                  type: ClusterIP
                """, appName, appName);
            
            writeToFile("k8s-manifests.yaml", deployment + service);
            System.out.println("Generated Kubernetes manifests for native image deployment");
        }
        
        public void generateHelmChart(String appName) {
            // Chart.yaml
            String chartYaml = String.format("""
                apiVersion: v2
                name: %s
                description: A Helm chart for GraalVM native image application
                type: application
                version: 0.1.0
                appVersion: "1.0"
                """, appName);
            
            // values.yaml
            String valuesYaml = """
                replicaCount: 3
                
                image:
                  repository: localhost/myapp
                  pullPolicy: IfNotPresent
                  tag: "latest"
                
                service:
                  type: ClusterIP
                  port: 80
                
                ingress:
                  enabled: false
                
                resources:
                  limits:
                    cpu: 100m
                    memory: 128Mi
                  requests:
                    cpu: 10m
                    memory: 32Mi
                
                autoscaling:
                  enabled: true
                  minReplicas: 2
                  maxReplicas: 10
                  targetCPUUtilizationPercentage: 80
                
                nodeSelector: {}
                tolerations: []
                affinity: {}
                """;
            
            writeToFile("Chart.yaml", chartYaml);
            writeToFile("values.yaml", valuesYaml);
            System.out.println("Generated Helm chart for native image deployment");
        }
    }
    
    public static class PerformanceMonitoring {
        
        public void setupPrometheusMetrics() {
            String prometheusConfig = """
                # Prometheus configuration for GraalVM native image monitoring
                global:
                  scrape_interval: 15s
                
                scrape_configs:
                  - job_name: 'graalvm-native-app'
                    static_configs:
                      - targets: ['localhost:8080']
                    metrics_path: /metrics
                    scrape_interval: 5s
                    
                  - job_name: 'graalvm-jvm-metrics'
                    static_configs:
                      - targets: ['localhost:8080']
                    metrics_path: /actuator/prometheus
                    scrape_interval: 10s
                """;
            
            writeToFile("prometheus.yml", prometheusConfig);
            
            String grafanaDashboard = """
                {
                  "dashboard": {
                    "title": "GraalVM Native Image Performance",
                    "panels": [
                      {
                        "title": "Memory Usage",
                        "type": "graph",
                        "targets": [
                          {
                            "expr": "process_resident_memory_bytes",
                            "legendFormat": "Resident Memory"
                          }
                        ]
                      },
                      {
                        "title": "CPU Usage",
                        "type": "graph",
                        "targets": [
                          {
                            "expr": "rate(process_cpu_seconds_total[5m])",
                            "legendFormat": "CPU Usage"
                          }
                        ]
                      },
                      {
                        "title": "Response Time",
                        "type": "graph",
                        "targets": [
                          {
                            "expr": "http_request_duration_seconds",
                            "legendFormat": "Response Time"
                          }
                        ]
                      }
                    ]
                  }
                }
                """;
            
            writeToFile("grafana-dashboard.json", grafanaDashboard);
            System.out.println("Generated monitoring configuration");
        }
    }
    
    private static void writeToFile(String filename, String content) {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(content);
        } catch (IOException e) {
            System.err.printf("Failed to write %s: %s%n", filename, e.getMessage());
        }
    }
}
```

## Practical Implementation

### Complete GraalVM Enterprise Application

**Enterprise-Grade Polyglot Application:**
```java
// Complete enterprise application demonstrating GraalVM capabilities
@SpringBootApplication
public class GraalVMEnterpriseApplication {
    
    public static void main(String[] args) {
        // Configure for native image
        System.setProperty("spring.aot.enabled", "true");
        SpringApplication.run(GraalVMEnterpriseApplication.class, args);
    }
    
    @RestController
    @RequestMapping("/api")
    public static class PolyglotController {
        
        private final PolyglotService polyglotService;
        
        public PolyglotController(PolyglotService polyglotService) {
            this.polyglotService = polyglotService;
        }
        
        @PostMapping("/analyze")
        public ResponseEntity<AnalysisResult> analyzeData(@RequestBody DataRequest request) {
            try {
                AnalysisResult result = polyglotService.performAnalysis(request);
                return ResponseEntity.ok(result);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AnalysisResult("Error: " + e.getMessage()));
            }
        }
        
        @GetMapping("/languages")
        public ResponseEntity<Set<String>> getSupportedLanguages() {
            return ResponseEntity.ok(polyglotService.getSupportedLanguages());
        }
        
        @PostMapping("/execute/{language}")
        public ResponseEntity<ExecutionResult> executeCode(
                @PathVariable String language,
                @RequestBody CodeRequest request) {
            
            try {
                ExecutionResult result = polyglotService.executeCode(language, request.code);
                return ResponseEntity.ok(result);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ExecutionResult(null, "Error: " + e.getMessage()));
            }
        }
    }
    
    @Service
    public static class PolyglotService {
        
        private final Context polyglotContext;
        
        public PolyglotService() {
            this.polyglotContext = Context.newBuilder()
                .allowAllAccess(true)
                .allowIO(IOAccess.ALL)
                .option("engine.WarnInterpreterOnly", "false")
                .build();
        }
        
        public AnalysisResult performAnalysis(DataRequest request) {
            // Use different languages for different types of analysis
            
            // JavaScript for data transformation
            String jsTransformation = """
                function transformData(data) {
                    return data.map(item => ({
                        ...item,
                        normalized: item.value / Math.max(...data.map(d => d.value)),
                        category: item.value > 50 ? 'high' : 'low'
                    }));
                }
                transformData
                """;
            
            Value jsFunction = polyglotContext.eval("js", jsTransformation);
            Value transformedData = jsFunction.execute(ProxyArray.fromArray(request.data));
            
            // Python for statistical analysis (if available)
            String pythonAnalysis = """
                def analyze_statistics(data):
                    values = [item['value'] for item in data]
                    return {
                        'mean': sum(values) / len(values),
                        'min': min(values),
                        'max': max(values),
                        'count': len(values)
                    }
                analyze_statistics
                """;
            
            Value pythonFunction = polyglotContext.eval("python", pythonAnalysis);
            Value statistics = pythonFunction.execute(transformedData);
            
            return new AnalysisResult(
                "Analysis completed successfully",
                transformedData,
                statistics
            );
        }
        
        public ExecutionResult executeCode(String language, String code) {
            try {
                Value result = polyglotContext.eval(language, code);
                return new ExecutionResult(result.toString(), null);
            } catch (Exception e) {
                return new ExecutionResult(null, e.getMessage());
            }
        }
        
        public Set<String> getSupportedLanguages() {
            return polyglotContext.getEngine().getLanguages().keySet();
        }
        
        @PreDestroy
        public void cleanup() {
            if (polyglotContext != null) {
                polyglotContext.close();
            }
        }
    }
    
    // Data models
    public record DataRequest(DataPoint[] data) {}
    public record DataPoint(String name, double value) {}
    public record CodeRequest(String code) {}
    public record AnalysisResult(String message, Object transformedData, Object statistics) {}
    public record ExecutionResult(String result, String error) {}
}
```

## Assessment and Exercises

### Exercise 1: Native Image Optimization
Create a Spring Boot application and optimize it for native image compilation. Measure startup time and memory usage improvements.

### Exercise 2: Polyglot Data Pipeline
Build a data processing pipeline that uses Java for orchestration, JavaScript for data transformation, Python for analysis, and R for statistical modeling.

### Exercise 3: Custom Language Implementation
Implement a domain-specific language using the Truffle framework. Include basic arithmetic, variables, and function definitions.

### Exercise 4: Cloud-Native Deployment
Deploy a GraalVM native image application to Kubernetes with proper monitoring, scaling, and health checks.

## Best Practices and Guidelines

### Native Image Best Practices
1. **Configuration Management**: Use configuration files for reflection, resources, and JNI
2. **Dependency Analysis**: Regularly analyze and minimize dependencies
3. **Testing Strategy**: Test both JVM and native image versions
4. **Build Optimization**: Use appropriate build flags for target environment
5. **Monitoring**: Implement comprehensive monitoring for native applications

### Polyglot Programming Guidelines
1. **Language Selection**: Choose languages based on their strengths
2. **Data Exchange**: Use appropriate data structures for cross-language communication
3. **Error Handling**: Implement proper error handling across language boundaries
4. **Performance**: Consider the overhead of language switches
5. **Security**: Validate data when crossing language boundaries

## Summary

Day 156 provided comprehensive coverage of advanced GraalVM features, including native image optimization, polyglot programming, and Truffle framework usage. Students learned to build high-performance, cloud-native applications that leverage multiple programming languages and achieve significant improvements in startup time and memory usage.

Key achievements:
- Advanced native image configuration and optimization techniques
- Multi-language integration with JavaScript, Python, and R
- Custom language implementation using Truffle framework
- Cloud-native deployment strategies for GraalVM applications
- Performance benchmarking and optimization methodologies

This knowledge prepares students for the next lesson on Java Vector API and SIMD programming for high-performance computing applications.