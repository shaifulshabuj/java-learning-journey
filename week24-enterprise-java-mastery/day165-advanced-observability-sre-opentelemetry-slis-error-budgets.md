# Day 165: Advanced Observability & Site Reliability Engineering - OpenTelemetry, SLIs & Error Budgets

## Learning Objectives
By the end of this lesson, you will:
- Master comprehensive observability with OpenTelemetry and custom metrics
- Implement SLIs, SLOs, and error budget practices
- Build advanced monitoring, alerting, and incident response systems
- Apply performance analysis and capacity planning techniques
- Create reliable, observable production systems with automated remediation

## 1. OpenTelemetry Integration

### 1.1 Comprehensive OpenTelemetry Implementation

```java
// OpenTelemetry Configuration for Enterprise Java
package com.enterprise.observability.otel;

@Configuration
@EnableConfigurationProperties(OpenTelemetryProperties.class)
public class OpenTelemetryConfig {
    
    @Bean
    public OpenTelemetry openTelemetry(OpenTelemetryProperties properties) {
        return OpenTelemetryBuilder.create()
            .setTracerProvider(createTracerProvider(properties))
            .setMeterProvider(createMeterProvider(properties))
            .setLoggerProvider(createLoggerProvider(properties))
            .buildAndRegisterGlobal();
    }
    
    private TracerProvider createTracerProvider(OpenTelemetryProperties properties) {
        Resource resource = Resource.getDefault()
            .merge(Resource.create(
                Attributes.of(
                    ResourceAttributes.SERVICE_NAME, properties.getServiceName(),
                    ResourceAttributes.SERVICE_VERSION, properties.getServiceVersion(),
                    ResourceAttributes.SERVICE_INSTANCE_ID, getInstanceId(),
                    ResourceAttributes.DEPLOYMENT_ENVIRONMENT, properties.getEnvironment()
                )
            ));
        
        SdkTracerProviderBuilder builder = SdkTracerProvider.builder()
            .setResource(resource)
            .setSampler(createSampler(properties))
            .addSpanProcessor(BatchSpanProcessor.builder(
                createSpanExporter(properties))
                .setMaxExportBatchSize(properties.getBatchSize())
                .setExportTimeout(properties.getExportTimeout())
                .setScheduleDelay(properties.getScheduleDelay())
                .build());
        
        // Add custom span processors
        builder.addSpanProcessor(new CustomAttributeSpanProcessor());
        builder.addSpanProcessor(new PerformanceSpanProcessor());
        
        return builder.build();
    }
    
    private MeterProvider createMeterProvider(OpenTelemetryProperties properties) {
        Resource resource = Resource.getDefault()
            .merge(Resource.create(
                Attributes.of(
                    ResourceAttributes.SERVICE_NAME, properties.getServiceName(),
                    ResourceAttributes.SERVICE_VERSION, properties.getServiceVersion()
                )
            ));
        
        return SdkMeterProvider.builder()
            .setResource(resource)
            .registerMetricReader(PeriodicMetricReader.builder(
                createMetricExporter(properties))
                .setInterval(properties.getMetricExportInterval())
                .build())
            .registerView(
                InstrumentSelector.builder()
                    .setType(InstrumentType.HISTOGRAM)
                    .setName("http.server.duration")
                    .build(),
                View.builder()
                    .setName("http_request_duration_seconds")
                    .setDescription("Duration of HTTP requests")
                    .setAggregation(Aggregation.explicitBucketHistogram(
                        Arrays.asList(0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1.0, 5.0, 10.0)))
                    .build())
            .build();
    }
    
    private Sampler createSampler(OpenTelemetryProperties properties) {
        return switch (properties.getSamplingStrategy()) {
            case ALWAYS_ON -> Sampler.alwaysOn();
            case ALWAYS_OFF -> Sampler.alwaysOff();
            case TRACE_ID_RATIO -> Sampler.traceIdRatioBased(properties.getSamplingRate());
            case PARENT_BASED -> Sampler.parentBased(
                Sampler.traceIdRatioBased(properties.getSamplingRate()));
            case ADAPTIVE -> new AdaptiveSampler(properties.getAdaptiveSamplingConfig());
        };
    }
    
    private SpanExporter createSpanExporter(OpenTelemetryProperties properties) {
        return switch (properties.getExporterType()) {
            case JAEGER -> JaegerGrpcSpanExporter.builder()
                .setEndpoint(properties.getJaegerEndpoint())
                .build();
            case ZIPKIN -> ZipkinSpanExporter.builder()
                .setEndpoint(properties.getZipkinEndpoint())
                .build();
            case OTLP -> OtlpGrpcSpanExporter.builder()
                .setEndpoint(properties.getOtlpEndpoint())
                .addHeader("api-key", properties.getApiKey())
                .build();
            case LOGGING -> LoggingSpanExporter.create();
            case COMPOSITE -> CompositeSpanExporter.create(
                createPrimaryExporter(properties),
                createSecondaryExporter(properties)
            );
        };
    }
}

// Advanced Tracing Aspects
@Aspect
@Component
@Slf4j
public class AdvancedTracingAspect {
    private final Tracer tracer;
    private final MeterRegistry meterRegistry;
    private final PerformanceAnalyzer performanceAnalyzer;
    
    public AdvancedTracingAspect(OpenTelemetry openTelemetry, 
                                MeterRegistry meterRegistry,
                                PerformanceAnalyzer performanceAnalyzer) {
        this.tracer = openTelemetry.getTracer("enterprise-tracing");
        this.meterRegistry = meterRegistry;
        this.performanceAnalyzer = performanceAnalyzer;
    }
    
    @Around("@annotation(traced)")
    public Object traceMethod(ProceedingJoinPoint joinPoint, Traced traced) throws Throwable {
        String operationName = traced.operationName().isEmpty() 
            ? joinPoint.getSignature().getName() 
            : traced.operationName();
        
        Span span = tracer.spanBuilder(operationName)
            .setSpanKind(traced.spanKind())
            .startSpan();
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try (Scope scope = span.makeCurrent()) {
            // Add method-specific attributes
            addMethodAttributes(span, joinPoint);
            
            // Add business context if available
            addBusinessContext(span, joinPoint);
            
            // Execute method
            Object result = joinPoint.proceed();
            
            // Add result attributes
            addResultAttributes(span, result, traced);
            
            // Analyze performance
            analyzePerformance(span, joinPoint, sample.stop());
            
            span.setStatus(StatusCode.OK);
            return result;
            
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            
            // Record error metrics
            recordErrorMetrics(operationName, e);
            
            throw e;
        } finally {
            span.end();
        }
    }
    
    private void addMethodAttributes(Span span, ProceedingJoinPoint joinPoint) {
        span.setAttribute("method.class", joinPoint.getTarget().getClass().getSimpleName());
        span.setAttribute("method.name", joinPoint.getSignature().getName());
        
        // Add parameter information (be careful with sensitive data)
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            span.setAttribute("method.parameters.count", args.length);
            
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null && !isSensitiveType(args[i].getClass())) {
                    span.setAttribute("method.parameter." + i + ".type", 
                        args[i].getClass().getSimpleName());
                }
            }
        }
    }
    
    private void addBusinessContext(Span span, ProceedingJoinPoint joinPoint) {
        // Extract business context from method parameters
        Object[] args = joinPoint.getArgs();
        
        for (Object arg : args) {
            if (arg instanceof BusinessContextProvider provider) {
                Map<String, String> context = provider.getBusinessContext();
                context.forEach((key, value) -> 
                    span.setAttribute("business." + key, value));
            }
        }
        
        // Add user context if available
        addUserContext(span);
        
        // Add tenant context if available
        addTenantContext(span);
    }
    
    private void addUserContext(Span span) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                span.setAttribute("user.id", auth.getName());
                
                if (auth.getPrincipal() instanceof UserDetails userDetails) {
                    span.setAttribute("user.authorities", 
                        userDetails.getAuthorities().toString());
                }
            }
        } catch (Exception e) {
            log.debug("Failed to add user context to span", e);
        }
    }
    
    private void analyzePerformance(Span span, ProceedingJoinPoint joinPoint, Duration duration) {
        String methodSignature = joinPoint.getSignature().toShortString();
        
        // Record performance metrics
        Timer.builder("method.execution.time")
            .tag("class", joinPoint.getTarget().getClass().getSimpleName())
            .tag("method", joinPoint.getSignature().getName())
            .register(meterRegistry)
            .record(duration);
        
        // Analyze for performance anomalies
        PerformanceAnalysisResult analysis = performanceAnalyzer.analyze(
            methodSignature, duration);
        
        if (analysis.isAnomalous()) {
            span.setAttribute("performance.anomaly", true);
            span.setAttribute("performance.baseline", analysis.getBaseline().toString());
            span.setAttribute("performance.deviation", analysis.getDeviation());
            
            // Add performance warning event
            span.addEvent("performance.warning", 
                Attributes.of(
                    AttributeKey.stringKey("message"), 
                    "Method execution time significantly exceeded baseline",
                    AttributeKey.doubleKey("deviation"), 
                    analysis.getDeviation()
                ));
        }
    }
}

// Custom Span Processor for Enhanced Attributes
public class CustomAttributeSpanProcessor implements SpanProcessor {
    private final SystemMetricsCollector systemMetrics;
    private final ApplicationMetricsCollector appMetrics;
    
    @Override
    public void onStart(Context parentContext, SpanBuilder spanBuilder) {
        // Add system metrics at span start
        SystemSnapshot snapshot = systemMetrics.getCurrentSnapshot();
        
        spanBuilder.setAttribute("system.cpu.usage", snapshot.getCpuUsage());
        spanBuilder.setAttribute("system.memory.usage", snapshot.getMemoryUsage());
        spanBuilder.setAttribute("system.active.threads", snapshot.getActiveThreads());
        
        // Add application metrics
        ApplicationSnapshot appSnapshot = appMetrics.getCurrentSnapshot();
        spanBuilder.setAttribute("app.active.requests", appSnapshot.getActiveRequests());
        spanBuilder.setAttribute("app.database.connections", appSnapshot.getDbConnections());
    }
    
    @Override
    public boolean isStartRequired() {
        return true;
    }
    
    @Override
    public void onEnd(SpanData spanData) {
        // Process completed spans for additional analysis
        if (spanData.hasEnded()) {
            analyzeSpanForPatterns(spanData);
        }
    }
    
    private void analyzeSpanForPatterns(SpanData spanData) {
        Duration duration = Duration.between(
            Instant.ofEpochNano(spanData.getStartEpochNanos()),
            Instant.ofEpochNano(spanData.getEndEpochNanos())
        );
        
        // Detect slow operations
        if (duration.toMillis() > 5000) {
            publishSlowOperationEvent(spanData, duration);
        }
        
        // Detect error patterns
        if (spanData.getStatus().getStatusCode() == StatusCode.ERROR) {
            analyzeErrorPatterns(spanData);
        }
    }
}

// Adaptive Sampler Implementation
public class AdaptiveSampler implements Sampler {
    private final AdaptiveSamplingConfig config;
    private final AtomicReference<Double> currentSamplingRate;
    private final ScheduledExecutorService scheduler;
    private final MeterRegistry meterRegistry;
    
    public AdaptiveSampler(AdaptiveSamplingConfig config) {
        this.config = config;
        this.currentSamplingRate = new AtomicReference<>(config.getInitialSamplingRate());
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.meterRegistry = Metrics.globalRegistry;
        
        startAdaptiveAdjustment();
    }
    
    @Override
    public SamplingResult shouldSample(Context parentContext,
                                     String traceId,
                                     String name,
                                     SpanKind spanKind,
                                     Attributes attributes,
                                     List<LinkData> parentLinks) {
        
        // Check for high-priority traces
        if (isHighPriorityTrace(attributes)) {
            return SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE);
        }
        
        // Check for error traces
        if (isErrorTrace(parentContext)) {
            return SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE);
        }
        
        // Use current adaptive sampling rate
        double samplingRate = currentSamplingRate.get();
        double threshold = Math.abs(traceId.hashCode()) / (double) Integer.MAX_VALUE;
        
        return threshold < samplingRate 
            ? SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE)
            : SamplingResult.create(SamplingDecision.DROP);
    }
    
    private void startAdaptiveAdjustment() {
        scheduler.scheduleAtFixedRate(this::adjustSamplingRate, 
            config.getAdjustmentInterval().toMinutes(),
            config.getAdjustmentInterval().toMinutes(),
            TimeUnit.MINUTES);
    }
    
    private void adjustSamplingRate() {
        try {
            // Get current system load
            double systemLoad = getSystemLoad();
            
            // Get trace throughput
            double traceThroughput = getTraceThroughput();
            
            // Calculate target sampling rate
            double targetRate = calculateTargetSamplingRate(systemLoad, traceThroughput);
            
            // Adjust gradually
            double currentRate = currentSamplingRate.get();
            double adjustedRate = adjustGradually(currentRate, targetRate);
            
            currentSamplingRate.set(adjustedRate);
            
            // Record sampling rate metric
            Gauge.builder("otel.sampling.rate")
                .register(meterRegistry, adjustedRate, Double::doubleValue);
                
        } catch (Exception e) {
            log.warn("Failed to adjust sampling rate", e);
        }
    }
    
    private double calculateTargetSamplingRate(double systemLoad, double traceThroughput) {
        // Decrease sampling rate under high load
        if (systemLoad > 0.8) {
            return Math.max(config.getMinSamplingRate(), 
                config.getBaseSamplingRate() * 0.5);
        }
        
        // Increase sampling rate under low load
        if (systemLoad < 0.3) {
            return Math.min(config.getMaxSamplingRate(),
                config.getBaseSamplingRate() * 1.5);
        }
        
        return config.getBaseSamplingRate();
    }
}
```

### 1.2 Custom Metrics and Instrumentation

```java
// Advanced Metrics Collection
@Component
public class EnterpriseMetricsCollector {
    private final MeterProvider meterProvider;
    private final Meter meter;
    private final Counter requestCounter;
    private final Histogram responseTimeHistogram;
    private final Gauge activeConnectionsGauge;
    private final Timer businessOperationTimer;
    
    // SLI Metrics
    private final Counter sliSuccessCounter;
    private final Counter sliTotalCounter;
    private final Histogram sliLatencyHistogram;
    
    public EnterpriseMetricsCollector(OpenTelemetry openTelemetry) {
        this.meterProvider = openTelemetry.getMeterProvider();
        this.meter = meterProvider.get("enterprise-metrics");
        
        initializeMetrics();
    }
    
    private void initializeMetrics() {
        // Request metrics
        this.requestCounter = meter.counterBuilder("http.requests.total")
            .setDescription("Total number of HTTP requests")
            .build();
            
        this.responseTimeHistogram = meter.histogramBuilder("http.request.duration")
            .setDescription("HTTP request duration in seconds")
            .setUnit("s")
            .build();
        
        // Connection metrics
        this.activeConnectionsGauge = meter.gaugeBuilder("db.connections.active")
            .setDescription("Number of active database connections")
            .buildWithCallback(measurement -> {
                measurement.record(getCurrentActiveConnections());
            });
        
        // Business metrics
        this.businessOperationTimer = Timer.builder("business.operation.duration")
            .description("Duration of business operations")
            .publishPercentileHistogram()
            .serviceLevelObjectives(
                Duration.ofMillis(100),
                Duration.ofMillis(500),
                Duration.ofMillis(1000),
                Duration.ofSeconds(5)
            )
            .register(Metrics.globalRegistry);
        
        // SLI Metrics
        this.sliSuccessCounter = meter.counterBuilder("sli.success.total")
            .setDescription("Total successful SLI events")
            .build();
            
        this.sliTotalCounter = meter.counterBuilder("sli.total")
            .setDescription("Total SLI events")
            .build();
            
        this.sliLatencyHistogram = meter.histogramBuilder("sli.latency")
            .setDescription("SLI operation latency")
            .setUnit("ms")
            .build();
    }
    
    public void recordHttpRequest(String method, String endpoint, int statusCode, Duration duration) {
        Attributes attributes = Attributes.of(
            AttributeKey.stringKey("method"), method,
            AttributeKey.stringKey("endpoint"), endpoint,
            AttributeKey.longKey("status_code"), statusCode
        );
        
        requestCounter.add(1, attributes);
        responseTimeHistogram.record(duration.toNanos() / 1_000_000_000.0, attributes);
        
        // Record SLI metrics
        recordSLI("http_request", statusCode < 400, duration.toMillis(), attributes);
    }
    
    public void recordBusinessOperation(String operationType, String outcome, Duration duration) {
        Tags tags = Tags.of(
            "operation", operationType,
            "outcome", outcome
        );
        
        businessOperationTimer.record(duration, tags);
        
        // Record custom business metrics
        Counter.builder("business.operations.total")
            .tags(tags)
            .register(Metrics.globalRegistry)
            .increment();
    }
    
    public void recordSLI(String sliType, boolean success, long latencyMs, Attributes attributes) {
        Attributes sliAttributes = attributes.toBuilder()
            .put("sli_type", sliType)
            .build();
        
        sliTotalCounter.add(1, sliAttributes);
        
        if (success) {
            sliSuccessCounter.add(1, sliAttributes);
        }
        
        sliLatencyHistogram.record(latencyMs, sliAttributes);
    }
    
    public void recordCustomMetric(String metricName, double value, Map<String, String> labels) {
        AttributesBuilder attributesBuilder = Attributes.builder();
        labels.forEach(attributesBuilder::put);
        
        meter.histogramBuilder(metricName)
            .build()
            .record(value, attributesBuilder.build());
    }
    
    private int getCurrentActiveConnections() {
        // Implementation to get active DB connections
        return 0; // Placeholder
    }
}

// Application Performance Monitoring (APM) Integration
@Component
public class APMIntegration {
    private final EnterpriseMetricsCollector metricsCollector;
    private final AlertManager alertManager;
    private final PerformanceBaseline performanceBaseline;
    
    @EventListener
    public void handlePerformanceEvent(PerformanceEvent event) {
        // Record performance metrics
        metricsCollector.recordBusinessOperation(
            event.getOperationType(),
            event.getOutcome(),
            event.getDuration()
        );
        
        // Check against performance baseline
        if (isPerformanceAnomaly(event)) {
            alertManager.sendAlert(createPerformanceAlert(event));
        }
        
        // Update baseline with new data
        performanceBaseline.updateBaseline(event);
    }
    
    @EventListener
    public void handleErrorEvent(ErrorEvent event) {
        // Record error metrics
        Counter.builder("application.errors.total")
            .tag("error_type", event.getErrorType())
            .tag("component", event.getComponent())
            .tag("severity", event.getSeverity().toString())
            .register(Metrics.globalRegistry)
            .increment();
        
        // Check error rate against SLI
        checkErrorRateSLI(event);
    }
    
    private boolean isPerformanceAnomaly(PerformanceEvent event) {
        PerformanceBaseline.Statistics baseline = performanceBaseline
            .getStatistics(event.getOperationType());
        
        if (baseline == null) {
            return false; // No baseline yet
        }
        
        double threshold = baseline.getMean() + (2 * baseline.getStandardDeviation());
        return event.getDuration().toMillis() > threshold;
    }
    
    private void checkErrorRateSLI(ErrorEvent event) {
        // Calculate error rate over last 5 minutes
        double errorRate = calculateErrorRate(event.getComponent(), Duration.ofMinutes(5));
        
        // Check against SLI threshold (e.g., 99.9% success rate = 0.1% error rate)
        if (errorRate > 0.001) {
            alertManager.sendAlert(new SLIViolationAlert(
                "error_rate",
                event.getComponent(),
                errorRate,
                0.001
            ));
        }
    }
}
```

## 2. SLIs, SLOs, and Error Budgets

### 2.1 SLI/SLO Framework Implementation

```java
// SLI/SLO Management Framework
@Service
public class SLIService {
    private final SLIRepository sliRepository;
    private final MetricsCollector metricsCollector;
    private final ErrorBudgetCalculator errorBudgetCalculator;
    private final AlertManager alertManager;
    
    public void recordSLIEvent(SLIEvent event) {
        // Record the SLI event
        SLI sli = sliRepository.findByName(event.getSliName())
            .orElseThrow(() -> new SLINotFoundException("SLI not found: " + event.getSliName()));
        
        // Record metrics
        metricsCollector.recordSLI(
            event.getSliName(),
            event.isSuccess(),
            event.getLatency(),
            event.getAttributes()
        );
        
        // Update error budget
        errorBudgetCalculator.updateErrorBudget(sli, event);
        
        // Check SLO compliance
        checkSLOCompliance(sli, event);
    }
    
    public SLOStatus getSLOStatus(String sliName, Duration period) {
        SLI sli = sliRepository.findByName(sliName)
            .orElseThrow(() -> new SLINotFoundException("SLI not found: " + sliName));
        
        SLOMetrics metrics = calculateSLOMetrics(sli, period);
        ErrorBudgetStatus budgetStatus = errorBudgetCalculator.getErrorBudgetStatus(sli, period);
        
        return SLOStatus.builder()
            .sliName(sliName)
            .period(period)
            .target(sli.getTarget())
            .actual(metrics.getActualPerformance())
            .compliance(metrics.getCompliance())
            .errorBudgetRemaining(budgetStatus.getRemainingBudget())
            .errorBudgetBurnRate(budgetStatus.getBurnRate())
            .status(determineStatus(metrics, budgetStatus))
            .build();
    }
    
    private void checkSLOCompliance(SLI sli, SLIEvent event) {
        // Check various time windows
        Duration[] windows = {
            Duration.ofMinutes(5),
            Duration.ofMinutes(30),
            Duration.ofHours(2),
            Duration.ofHours(24)
        };
        
        for (Duration window : windows) {
            SLOMetrics metrics = calculateSLOMetrics(sli, window);
            
            if (metrics.getCompliance() < sli.getTarget()) {
                alertManager.sendAlert(new SLOViolationAlert(
                    sli.getName(),
                    window,
                    metrics.getCompliance(),
                    sli.getTarget()
                ));
            }
        }
    }
    
    private SLOMetrics calculateSLOMetrics(SLI sli, Duration period) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(period);
        
        switch (sli.getType()) {
            case AVAILABILITY:
                return calculateAvailabilityMetrics(sli, startTime, endTime);
            case LATENCY:
                return calculateLatencyMetrics(sli, startTime, endTime);
            case THROUGHPUT:
                return calculateThroughputMetrics(sli, startTime, endTime);
            case ERROR_RATE:
                return calculateErrorRateMetrics(sli, startTime, endTime);
            default:
                throw new UnsupportedOperationException("Unsupported SLI type: " + sli.getType());
        }
    }
    
    private SLOMetrics calculateAvailabilityMetrics(SLI sli, Instant startTime, Instant endTime) {
        // Query metrics for successful vs total requests
        long totalRequests = metricsCollector.getTotalRequests(sli.getSelector(), startTime, endTime);
        long successfulRequests = metricsCollector.getSuccessfulRequests(sli.getSelector(), startTime, endTime);
        
        double availability = totalRequests > 0 ? (double) successfulRequests / totalRequests : 1.0;
        double compliance = availability >= sli.getTarget() ? 1.0 : availability / sli.getTarget();
        
        return SLOMetrics.builder()
            .totalEvents(totalRequests)
            .successfulEvents(successfulRequests)
            .actualPerformance(availability)
            .compliance(compliance)
            .build();
    }
    
    private SLOMetrics calculateLatencyMetrics(SLI sli, Instant startTime, Instant endTime) {
        // Query latency percentiles
        LatencyStatistics stats = metricsCollector.getLatencyStatistics(
            sli.getSelector(), startTime, endTime);
        
        double percentileValue = stats.getPercentile(sli.getPercentile());
        double actualPerformance = percentileValue <= sli.getThreshold() ? 1.0 : 
            sli.getThreshold() / percentileValue;
        
        return SLOMetrics.builder()
            .totalEvents(stats.getTotalRequests())
            .successfulEvents(stats.getRequestsUnderThreshold(sli.getThreshold()))
            .actualPerformance(actualPerformance)
            .compliance(actualPerformance >= sli.getTarget() ? 1.0 : actualPerformance / sli.getTarget())
            .build();
    }
}

// Error Budget Calculator
@Service
public class ErrorBudgetCalculator {
    private final MetricsCollector metricsCollector;
    private final ErrorBudgetRepository errorBudgetRepository;
    
    public void updateErrorBudget(SLI sli, SLIEvent event) {
        ErrorBudget errorBudget = getOrCreateErrorBudget(sli);
        
        if (!event.isSuccess()) {
            // Consume error budget
            errorBudget.consumeBudget(1);
            
            // Check if error budget is exhausted
            if (errorBudget.isExhausted()) {
                publishErrorBudgetExhaustedEvent(sli, errorBudget);
            }
        }
        
        errorBudgetRepository.save(errorBudget);
    }
    
    public ErrorBudgetStatus getErrorBudgetStatus(SLI sli, Duration period) {
        ErrorBudget errorBudget = getOrCreateErrorBudget(sli);
        
        // Calculate burn rate
        double burnRate = calculateBurnRate(sli, period);
        
        // Calculate remaining budget
        double remainingBudget = errorBudget.getRemainingBudgetPercentage();
        
        // Calculate time to exhaustion at current burn rate
        Duration timeToExhaustion = calculateTimeToExhaustion(remainingBudget, burnRate);
        
        return ErrorBudgetStatus.builder()
            .sliName(sli.getName())
            .totalBudget(errorBudget.getTotalBudget())
            .consumedBudget(errorBudget.getConsumedBudget())
            .remainingBudget(remainingBudget)
            .burnRate(burnRate)
            .timeToExhaustion(timeToExhaustion)
            .status(determineErrorBudgetStatus(remainingBudget, burnRate))
            .build();
    }
    
    private double calculateBurnRate(SLI sli, Duration period) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(period);
        
        long totalEvents = metricsCollector.getTotalRequests(sli.getSelector(), startTime, endTime);
        long failedEvents = metricsCollector.getFailedRequests(sli.getSelector(), startTime, endTime);
        
        if (totalEvents == 0) {
            return 0.0;
        }
        
        double actualErrorRate = (double) failedEvents / totalEvents;
        double allowedErrorRate = 1.0 - sli.getTarget();
        
        return actualErrorRate / allowedErrorRate;
    }
    
    private ErrorBudgetStatus.Status determineErrorBudgetStatus(double remainingBudget, double burnRate) {
        if (remainingBudget <= 0) {
            return ErrorBudgetStatus.Status.EXHAUSTED;
        } else if (burnRate > 10) {
            return ErrorBudgetStatus.Status.CRITICAL;
        } else if (burnRate > 2) {
            return ErrorBudgetStatus.Status.WARNING;
        } else {
            return ErrorBudgetStatus.Status.HEALTHY;
        }
    }
}

// SLI Configuration and Management
@Entity
@Table(name = "slis")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SLI {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(unique = true, nullable = false)
    private String name;
    
    private String description;
    
    @Enumerated(EnumType.STRING)
    private SLIType type;
    
    // Target SLO (e.g., 0.999 for 99.9%)
    private double target;
    
    // For latency SLIs
    private Double threshold;
    private Double percentile;
    
    // Metric selector (e.g., service name, endpoint)
    @Convert(converter = JsonConverter.class)
    private Map<String, String> selector;
    
    // Time window for evaluation
    private Duration evaluationWindow;
    
    private boolean enabled;
    
    private Instant createdAt;
    private Instant updatedAt;
    
    @OneToMany(mappedBy = "sli", cascade = CascadeType.ALL)
    private List<ErrorBudget> errorBudgets = new ArrayList<>();
}

@Entity
@Table(name = "error_budgets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorBudget {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sli_id")
    private SLI sli;
    
    // Time period for this error budget (e.g., monthly)
    private LocalDate periodStart;
    private LocalDate periodEnd;
    
    // Total allowed errors for this period
    private long totalBudget;
    
    // Errors consumed so far
    private long consumedBudget;
    
    private Instant createdAt;
    private Instant updatedAt;
    
    public double getRemainingBudgetPercentage() {
        if (totalBudget == 0) {
            return 0.0;
        }
        return (double) (totalBudget - consumedBudget) / totalBudget * 100;
    }
    
    public boolean isExhausted() {
        return consumedBudget >= totalBudget;
    }
    
    public void consumeBudget(long errors) {
        this.consumedBudget = Math.min(totalBudget, consumedBudget + errors);
        this.updatedAt = Instant.now();
    }
}

public enum SLIType {
    AVAILABILITY,
    LATENCY,
    THROUGHPUT,
    ERROR_RATE,
    CUSTOM
}
```

### 2.2 Advanced Alerting and Incident Response

```java
// Multi-channel Alert Manager
@Service
public class AlertManager {
    private final List<AlertChannel> alertChannels;
    private final AlertRepository alertRepository;
    private final EscalationService escalationService;
    private final AlertAggregationService aggregationService;
    
    public void sendAlert(Alert alert) {
        try {
            // Check for alert suppression
            if (shouldSuppressAlert(alert)) {
                log.debug("Alert suppressed: {}", alert.getId());
                return;
            }
            
            // Aggregate similar alerts
            Alert aggregatedAlert = aggregationService.aggregate(alert);
            
            // Save alert
            Alert savedAlert = alertRepository.save(aggregatedAlert);
            
            // Send to configured channels
            sendToChannels(savedAlert);
            
            // Schedule escalation if needed
            if (savedAlert.getEscalationPolicy() != null) {
                escalationService.scheduleEscalation(savedAlert);
            }
            
        } catch (Exception e) {
            log.error("Failed to send alert: {}", alert.getId(), e);
            // Fallback to email or logging
            sendFallbackAlert(alert, e);
        }
    }
    
    private void sendToChannels(Alert alert) {
        List<AlertChannel> applicableChannels = alertChannels.stream()
            .filter(channel -> channel.shouldHandle(alert))
            .collect(Collectors.toList());
        
        for (AlertChannel channel : applicableChannels) {
            try {
                channel.sendAlert(alert);
            } catch (Exception e) {
                log.error("Failed to send alert via channel: {}", channel.getName(), e);
            }
        }
    }
    
    private boolean shouldSuppressAlert(Alert alert) {
        // Check if similar alert was sent recently
        Duration suppressionWindow = Duration.ofMinutes(5);
        Instant cutoff = Instant.now().minus(suppressionWindow);
        
        return alertRepository.existsSimilarAlertSince(
            alert.getType(),
            alert.getSource(),
            alert.getKey(),
            cutoff
        );
    }
}

// Slack Alert Channel
@Component
public class SlackAlertChannel implements AlertChannel {
    private final SlackClient slackClient;
    private final SlackConfig config;
    
    @Override
    public boolean shouldHandle(Alert alert) {
        return alert.getSeverity().ordinal() >= config.getMinSeverityLevel().ordinal();
    }
    
    @Override
    public void sendAlert(Alert alert) {
        String channel = determineChannel(alert);
        SlackMessage message = buildSlackMessage(alert);
        
        slackClient.postMessage(channel, message);
    }
    
    private SlackMessage buildSlackMessage(Alert alert) {
        Color color = switch (alert.getSeverity()) {
            case CRITICAL -> Color.DANGER;
            case WARNING -> Color.WARNING;
            case INFO -> Color.GOOD;
        };
        
        return SlackMessage.builder()
            .text(alert.getTitle())
            .attachments(List.of(
                SlackAttachment.builder()
                    .color(color)
                    .title(alert.getTitle())
                    .text(alert.getDescription())
                    .fields(List.of(
                        SlackField.builder()
                            .title("Severity")
                            .value(alert.getSeverity().toString())
                            .shortField(true)
                            .build(),
                        SlackField.builder()
                            .title("Source")
                            .value(alert.getSource())
                            .shortField(true)
                            .build(),
                        SlackField.builder()
                            .title("Time")
                            .value(alert.getTimestamp().toString())
                            .shortField(true)
                            .build()
                    ))
                    .actions(List.of(
                        SlackAction.builder()
                            .name("acknowledge")
                            .text("Acknowledge")
                            .type("button")
                            .value(alert.getId().toString())
                            .build(),
                        SlackAction.builder()
                            .name("resolve")
                            .text("Resolve")
                            .type("button")
                            .value(alert.getId().toString())
                            .style("primary")
                            .build()
                    ))
                    .build()
            ))
            .build();
    }
}

// PagerDuty Integration
@Component
public class PagerDutyAlertChannel implements AlertChannel {
    private final PagerDutyClient pagerDutyClient;
    private final PagerDutyConfig config;
    
    @Override
    public boolean shouldHandle(Alert alert) {
        return alert.getSeverity() == AlertSeverity.CRITICAL;
    }
    
    @Override
    public void sendAlert(Alert alert) {
        PagerDutyEvent event = PagerDutyEvent.builder()
            .routingKey(config.getIntegrationKey())
            .eventAction(EventAction.TRIGGER)
            .deduplicationKey(alert.getKey())
            .payload(PagerDutyPayload.builder()
                .summary(alert.getTitle())
                .source(alert.getSource())
                .severity(mapSeverity(alert.getSeverity()))
                .component(alert.getComponent())
                .group(alert.getGroup())
                .customDetails(alert.getDetails())
                .build())
            .build();
            
        pagerDutyClient.sendEvent(event);
    }
    
    private PagerDutySeverity mapSeverity(AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> PagerDutySeverity.CRITICAL;
            case WARNING -> PagerDutySeverity.WARNING;
            case INFO -> PagerDutySeverity.INFO;
        };
    }
}

// Incident Management System
@Service
public class IncidentManager {
    private final IncidentRepository incidentRepository;
    private final AlertManager alertManager;
    private final EscalationService escalationService;
    private final CommunicationService communicationService;
    
    @EventListener
    public void handleCriticalAlert(CriticalAlertEvent event) {
        // Create incident for critical alerts
        Incident incident = createIncident(event.getAlert());
        
        // Notify on-call engineer
        escalationService.notifyOnCall(incident);
        
        // Create war room if needed
        if (incident.getSeverity() == IncidentSeverity.SEV1) {
            communicationService.createWarRoom(incident);
        }
        
        // Start incident response timeline
        startIncidentResponse(incident);
    }
    
    private Incident createIncident(Alert alert) {
        Incident incident = new Incident();
        incident.setTitle(generateIncidentTitle(alert));
        incident.setDescription(alert.getDescription());
        incident.setSeverity(mapAlertSeverityToIncidentSeverity(alert.getSeverity()));
        incident.setStatus(IncidentStatus.OPEN);
        incident.setCreatedAt(Instant.now());
        incident.setSource(alert.getSource());
        incident.setAlertId(alert.getId());
        
        return incidentRepository.save(incident);
    }
    
    private void startIncidentResponse(Incident incident) {
        // Create incident response tasks
        List<IncidentTask> tasks = createIncidentTasks(incident);
        
        // Assign tasks to responders
        assignTasks(tasks, incident);
        
        // Start incident timeline
        createTimelineEntry(incident, "Incident created", IncidentActionType.CREATED);
        
        // Schedule status updates
        scheduleStatusUpdates(incident);
    }
    
    public void updateIncidentStatus(UUID incidentId, IncidentStatus newStatus, String reason) {
        Incident incident = incidentRepository.findById(incidentId)
            .orElseThrow(() -> new IncidentNotFoundException("Incident not found: " + incidentId));
        
        IncidentStatus oldStatus = incident.getStatus();
        incident.setStatus(newStatus);
        incident.setUpdatedAt(Instant.now());
        
        incidentRepository.save(incident);
        
        // Create timeline entry
        createTimelineEntry(incident, 
            String.format("Status changed from %s to %s: %s", oldStatus, newStatus, reason),
            IncidentActionType.STATUS_CHANGE);
        
        // Handle status-specific actions
        handleStatusChange(incident, oldStatus, newStatus);
    }
    
    private void handleStatusChange(Incident incident, IncidentStatus oldStatus, IncidentStatus newStatus) {
        switch (newStatus) {
            case RESOLVED:
                handleIncidentResolution(incident);
                break;
            case ESCALATED:
                escalationService.escalateIncident(incident);
                break;
            case CLOSED:
                handleIncidentClosure(incident);
                break;
        }
    }
}
```

## 3. Performance Analysis and Capacity Planning

### 3.1 Advanced Performance Monitoring

```java
// Performance Analysis Engine
@Service
public class PerformanceAnalysisEngine {
    private final PerformanceDataCollector dataCollector;
    private final StatisticalAnalyzer statisticalAnalyzer;
    private final AnomalyDetector anomalyDetector;
    private final CapacityPredictor capacityPredictor;
    
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void analyzePerformance() {
        try {
            // Collect current performance data
            PerformanceSnapshot snapshot = dataCollector.collectSnapshot();
            
            // Analyze for anomalies
            List<PerformanceAnomaly> anomalies = anomalyDetector.detectAnomalies(snapshot);
            
            // Process any found anomalies
            anomalies.forEach(this::handleAnomaly);
            
            // Update performance baselines
            statisticalAnalyzer.updateBaselines(snapshot);
            
            // Capacity planning analysis
            performCapacityAnalysis(snapshot);
            
        } catch (Exception e) {
            log.error("Performance analysis failed", e);
        }
    }
    
    private void performCapacityAnalysis(PerformanceSnapshot snapshot) {
        // Analyze resource utilization trends
        ResourceTrend cpuTrend = capacityPredictor.analyzeCpuTrend();
        ResourceTrend memoryTrend = capacityPredictor.analyzeMemoryTrend();
        ResourceTrend diskTrend = capacityPredictor.analyzeDiskTrend();
        
        // Predict future capacity needs
        CapacityForecast forecast = capacityPredictor.generateForecast(
            Duration.ofDays(30), // 30-day forecast
            cpuTrend,
            memoryTrend,
            diskTrend
        );
        
        // Check if capacity limits will be exceeded
        checkCapacityLimits(forecast);
    }
    
    private void handleAnomaly(PerformanceAnomaly anomaly) {
        // Create performance alert
        Alert alert = Alert.builder()
            .title("Performance Anomaly Detected")
            .description(anomaly.getDescription())
            .severity(anomaly.getSeverity())
            .source("performance-monitor")
            .component(anomaly.getComponent())
            .details(anomaly.getDetails())
            .build();
        
        alertManager.sendAlert(alert);
        
        // Trigger automated remediation if configured
        if (anomaly.isRemediationAvailable()) {
            performanceRemediationService.remediate(anomaly);
        }
    }
    
    private void checkCapacityLimits(CapacityForecast forecast) {
        // Check CPU capacity
        if (forecast.getCpuForecast().getMaxUtilization() > 0.8) {
            alertManager.sendAlert(Alert.builder()
                .title("CPU Capacity Warning")
                .description("CPU utilization predicted to exceed 80% within 30 days")
                .severity(AlertSeverity.WARNING)
                .source("capacity-planner")
                .build());
        }
        
        // Check memory capacity
        if (forecast.getMemoryForecast().getMaxUtilization() > 0.85) {
            alertManager.sendAlert(Alert.builder()
                .title("Memory Capacity Warning")
                .description("Memory utilization predicted to exceed 85% within 30 days")
                .severity(AlertSeverity.WARNING)
                .source("capacity-planner")
                .build());
        }
    }
}

// Statistical Performance Analyzer
@Component
public class StatisticalAnalyzer {
    private final PerformanceHistoryRepository historyRepository;
    private final BaselineRepository baselineRepository;
    
    public void updateBaselines(PerformanceSnapshot snapshot) {
        // Update rolling statistics for each metric
        updateMetricBaseline("cpu.usage", snapshot.getCpuUsage());
        updateMetricBaseline("memory.usage", snapshot.getMemoryUsage());
        updateMetricBaseline("response.time", snapshot.getAverageResponseTime());
        updateMetricBaseline("throughput", snapshot.getThroughput());
        updateMetricBaseline("error.rate", snapshot.getErrorRate());
    }
    
    private void updateMetricBaseline(String metricName, double value) {
        MetricBaseline baseline = baselineRepository.findByName(metricName)
            .orElse(new MetricBaseline(metricName));
        
        // Update statistics using exponential moving average
        double alpha = 0.1; // Smoothing factor
        
        if (baseline.getCount() == 0) {
            baseline.setMean(value);
            baseline.setVariance(0.0);
        } else {
            double oldMean = baseline.getMean();
            double newMean = (1 - alpha) * oldMean + alpha * value;
            
            double oldVariance = baseline.getVariance();
            double newVariance = (1 - alpha) * oldVariance + 
                alpha * Math.pow(value - oldMean, 2);
            
            baseline.setMean(newMean);
            baseline.setVariance(newVariance);
        }
        
        baseline.setStandardDeviation(Math.sqrt(baseline.getVariance()));
        baseline.setCount(baseline.getCount() + 1);
        baseline.setLastUpdated(Instant.now());
        
        baselineRepository.save(baseline);
    }
    
    public AnomalyScore calculateAnomalyScore(String metricName, double value) {
        MetricBaseline baseline = baselineRepository.findByName(metricName)
            .orElse(null);
        
        if (baseline == null || baseline.getCount() < 30) {
            return AnomalyScore.INSUFFICIENT_DATA;
        }
        
        // Calculate z-score
        double zScore = Math.abs(value - baseline.getMean()) / baseline.getStandardDeviation();
        
        // Convert z-score to anomaly score
        if (zScore > 3.0) {
            return AnomalyScore.HIGH_ANOMALY;
        } else if (zScore > 2.0) {
            return AnomalyScore.MEDIUM_ANOMALY;
        } else if (zScore > 1.5) {
            return AnomalyScore.LOW_ANOMALY;
        } else {
            return AnomalyScore.NORMAL;
        }
    }
}

// Capacity Predictor using Time Series Analysis
@Component
public class CapacityPredictor {
    private final TimeSeriesAnalyzer timeSeriesAnalyzer;
    private final MetricsRepository metricsRepository;
    
    public CapacityForecast generateForecast(Duration forecastPeriod,
                                           ResourceTrend... trends) {
        CapacityForecast.Builder forecastBuilder = CapacityForecast.builder()
            .forecastPeriod(forecastPeriod)
            .generatedAt(Instant.now());
        
        for (ResourceTrend trend : trends) {
            ResourceForecast resourceForecast = generateResourceForecast(trend, forecastPeriod);
            
            switch (trend.getResourceType()) {
                case CPU -> forecastBuilder.cpuForecast(resourceForecast);
                case MEMORY -> forecastBuilder.memoryForecast(resourceForecast);
                case DISK -> forecastBuilder.diskForecast(resourceForecast);
                case NETWORK -> forecastBuilder.networkForecast(resourceForecast);
            }
        }
        
        return forecastBuilder.build();
    }
    
    private ResourceForecast generateResourceForecast(ResourceTrend trend, Duration forecastPeriod) {
        // Use linear regression for trend prediction
        List<DataPoint> historicalData = getHistoricalData(trend.getResourceType());
        
        LinearRegressionModel model = timeSeriesAnalyzer.fitLinearModel(historicalData);
        
        // Generate forecast points
        List<ForecastPoint> forecastPoints = new ArrayList<>();
        Instant currentTime = Instant.now();
        
        long intervalMillis = forecastPeriod.toMillis() / 100; // 100 forecast points
        
        for (int i = 1; i <= 100; i++) {
            Instant forecastTime = currentTime.plusMillis(intervalMillis * i);
            double predictedValue = model.predict(forecastTime.toEpochMilli());
            
            // Add confidence interval
            double confidenceInterval = model.getConfidenceInterval(0.95);
            
            forecastPoints.add(new ForecastPoint(
                forecastTime,
                predictedValue,
                predictedValue - confidenceInterval,
                predictedValue + confidenceInterval
            ));
        }
        
        return ResourceForecast.builder()
            .resourceType(trend.getResourceType())
            .forecastPoints(forecastPoints)
            .maxUtilization(forecastPoints.stream()
                .mapToDouble(ForecastPoint::getPredictedValue)
                .max()
                .orElse(0.0))
            .confidence(model.getR2Score())
            .build();
    }
}
```

## 4. Practical Exercises

### Exercise 1: Implement Comprehensive Observability Stack
Create a complete observability solution:
- Set up OpenTelemetry with custom instrumentation
- Implement distributed tracing across microservices
- Add custom metrics and SLI tracking
- Create observability dashboards

### Exercise 2: Build SLI/SLO Management System
Develop an SLI/SLO framework:
- Define SLIs for different service types
- Implement error budget calculations
- Create automated alerting based on SLO violations
- Build error budget burn rate monitoring

### Exercise 3: Create Advanced Alerting System
Implement intelligent alerting:
- Multi-channel alert routing
- Alert aggregation and deduplication
- Escalation policies and procedures
- Integration with incident management

## 5. Assessment Questions

1. **Observability Architecture**: Design a comprehensive observability strategy for a microservices architecture. Include tracing, metrics, logging, and alerting.

2. **SLI/SLO Implementation**: Implement an SLI/SLO framework for an e-commerce platform. Define appropriate SLIs and calculate error budgets.

3. **Performance Analysis**: Create a performance analysis system that can detect anomalies and predict capacity needs using statistical methods.

4. **Incident Response**: Design an automated incident response system that can detect, escalate, and coordinate resolution of production issues.

## Summary

Today you mastered advanced observability and site reliability engineering including:

- **OpenTelemetry Integration**: Comprehensive tracing, metrics, and logging
- **SLI/SLO Framework**: Error budgets and reliability engineering practices
- **Advanced Alerting**: Multi-channel, intelligent alerting systems
- **Performance Analysis**: Statistical analysis and capacity planning
- **Incident Management**: Automated response and escalation systems

These practices are essential for building and maintaining reliable, observable production systems at enterprise scale.

## Next Steps

Tomorrow, we'll explore **Enterprise Security Architecture** focusing on zero trust implementation, advanced authentication patterns, compliance frameworks, and building secure, compliant enterprise systems.