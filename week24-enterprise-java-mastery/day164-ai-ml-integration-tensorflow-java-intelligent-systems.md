# Day 164: AI/ML Integration in Enterprise Java - TensorFlow Java, ML Pipelines & Intelligent Systems

## Learning Objectives
By the end of this lesson, you will:
- Master TensorFlow Java API integration and model serving
- Build comprehensive ML pipelines with Java and Spring
- Implement real-time inference and batch processing systems
- Apply MLOps practices for Java applications
- Create intelligent, learning enterprise systems with adaptive capabilities

## 1. TensorFlow Java Integration

### 1.1 TensorFlow Java API Fundamentals

```java
// TensorFlow Model Service for Enterprise Integration
package com.enterprise.ml.tensorflow;

@Service
@Slf4j
public class TensorFlowModelService {
    private final ModelRegistry modelRegistry;
    private final TensorFlowConfig config;
    private final MeterRegistry meterRegistry;
    private final ModelCache modelCache;
    
    public TensorFlowModelService(ModelRegistry modelRegistry,
                                 TensorFlowConfig config,
                                 MeterRegistry meterRegistry) {
        this.modelRegistry = modelRegistry;
        this.config = config;
        this.meterRegistry = meterRegistry;
        this.modelCache = new ModelCache(config.getCacheConfig());
    }
    
    public <T> PredictionResult<T> predict(String modelName, 
                                          String version,
                                          PredictionRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            // Load model (with caching)
            TensorFlowModel model = loadModel(modelName, version);
            
            // Validate input
            validateInput(model, request);
            
            // Convert input to tensors
            Map<String, Tensor<?>> inputTensors = convertToTensors(model, request);
            
            // Run inference
            Map<String, Tensor<?>> outputs = runInference(model, inputTensors);
            
            // Convert outputs
            T result = convertFromTensors(model, outputs, request.getOutputType());
            
            // Record metrics
            recordSuccessMetrics(modelName, version, sample);
            
            return PredictionResult.success(result, 
                extractMetadata(model, inputTensors, outputs));
                
        } catch (Exception e) {
            recordFailureMetrics(modelName, version, sample, e);
            throw new ModelInferenceException("Prediction failed for model: " + modelName, e);
        }
    }
    
    public BatchPredictionResult<T> batchPredict(String modelName,
                                               String version,
                                               List<PredictionRequest> requests) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            TensorFlowModel model = loadModel(modelName, version);
            
            // Process in batches for efficiency
            List<T> results = new ArrayList<>();
            List<PredictionMetadata> metadata = new ArrayList<>();
            
            int batchSize = config.getBatchSize();
            for (int i = 0; i < requests.size(); i += batchSize) {
                List<PredictionRequest> batch = requests.subList(
                    i, Math.min(i + batchSize, requests.size()));
                
                BatchInferenceResult batchResult = processBatch(model, batch);
                results.addAll(batchResult.getResults());
                metadata.addAll(batchResult.getMetadata());
            }
            
            recordBatchSuccessMetrics(modelName, version, requests.size(), sample);
            
            return BatchPredictionResult.success(results, metadata);
            
        } catch (Exception e) {
            recordBatchFailureMetrics(modelName, version, requests.size(), sample, e);
            throw new ModelInferenceException("Batch prediction failed", e);
        }
    }
    
    private TensorFlowModel loadModel(String modelName, String version) {
        String cacheKey = modelName + ":" + version;
        
        return modelCache.get(cacheKey, () -> {
            ModelMetadata metadata = modelRegistry.getModelMetadata(modelName, version);
            
            try {
                SavedModelBundle modelBundle = SavedModelBundle.load(
                    metadata.getModelPath(), 
                    metadata.getTags().toArray(new String[0])
                );
                
                return new TensorFlowModel(modelName, version, modelBundle, metadata);
                
            } catch (Exception e) {
                throw new ModelLoadException("Failed to load model: " + modelName, e);
            }
        });
    }
    
    private Map<String, Tensor<?>> convertToTensors(TensorFlowModel model, 
                                                   PredictionRequest request) {
        Map<String, Tensor<?>> tensors = new HashMap<>();
        
        for (InputSpec inputSpec : model.getInputSpecs()) {
            Object inputValue = request.getInputs().get(inputSpec.getName());
            
            if (inputValue == null) {
                throw new InvalidInputException("Missing input: " + inputSpec.getName());
            }
            
            Tensor<?> tensor = createTensor(inputSpec, inputValue);
            tensors.put(inputSpec.getName(), tensor);
        }
        
        return tensors;
    }
    
    private Tensor<?> createTensor(InputSpec inputSpec, Object inputValue) {
        return switch (inputSpec.getDataType()) {
            case FLOAT -> {
                if (inputValue instanceof float[] values) {
                    yield TFloat32.tensorOf(Shape.of(values.length), 
                        DataBuffers.of(values));
                } else if (inputValue instanceof float[][] values) {
                    yield createFloat2DTensor(values);
                } else {
                    throw new InvalidInputException("Invalid float input format");
                }
            }
            case DOUBLE -> {
                if (inputValue instanceof double[] values) {
                    yield TFloat64.tensorOf(Shape.of(values.length),
                        DataBuffers.of(values));
                } else {
                    throw new InvalidInputException("Invalid double input format");
                }
            }
            case INT32 -> {
                if (inputValue instanceof int[] values) {
                    yield TInt32.tensorOf(Shape.of(values.length),
                        DataBuffers.of(values));
                } else {
                    throw new InvalidInputException("Invalid int input format");
                }
            }
            case STRING -> {
                if (inputValue instanceof String[] values) {
                    yield createStringTensor(values);
                } else if (inputValue instanceof String value) {
                    yield TString.tensorOf(value);
                } else {
                    throw new InvalidInputException("Invalid string input format");
                }
            }
            default -> throw new UnsupportedOperationException(
                "Unsupported data type: " + inputSpec.getDataType());
        };
    }
    
    private TFloat32 createFloat2DTensor(float[][] values) {
        int rows = values.length;
        int cols = values[0].length;
        
        FloatNdArray ndArray = NdArrays.ofFloats(Shape.of(rows, cols));
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                ndArray.setFloat(values[i][j], i, j);
            }
        }
        
        return TFloat32.tensorOf(ndArray);
    }
    
    private TString createStringTensor(String[] values) {
        NdArray<String> ndArray = NdArrays.ofObjects(String.class, Shape.of(values.length));
        
        for (int i = 0; i < values.length; i++) {
            ndArray.setObject(values[i], i);
        }
        
        return TString.tensorOf(ndArray);
    }
    
    private Map<String, Tensor<?>> runInference(TensorFlowModel model,
                                               Map<String, Tensor<?>> inputs) {
        Session session = model.getModelBundle().session();
        
        Session.Runner runner = session.runner();
        
        // Feed inputs
        for (Map.Entry<String, Tensor<?>> entry : inputs.entrySet()) {
            runner.feed(entry.getKey(), entry.getValue());
        }
        
        // Specify outputs to fetch
        for (OutputSpec outputSpec : model.getOutputSpecs()) {
            runner.fetch(outputSpec.getName());
        }
        
        // Run inference
        List<Tensor<?>> outputTensors = runner.run();
        
        // Map outputs by name
        Map<String, Tensor<?>> outputs = new HashMap<>();
        List<OutputSpec> outputSpecs = model.getOutputSpecs();
        
        for (int i = 0; i < outputTensors.size(); i++) {
            outputs.put(outputSpecs.get(i).getName(), outputTensors.get(i));
        }
        
        return outputs;
    }
    
    @SuppressWarnings("unchecked")
    private <T> T convertFromTensors(TensorFlowModel model,
                                    Map<String, Tensor<?>> outputs,
                                    Class<T> outputType) {
        if (outputType == Float.class || outputType == float.class) {
            return (T) extractFloatResult(outputs);
        } else if (outputType == Double.class || outputType == double.class) {
            return (T) extractDoubleResult(outputs);
        } else if (outputType == float[].class) {
            return (T) extractFloatArrayResult(outputs);
        } else if (outputType == double[].class) {
            return (T) extractDoubleArrayResult(outputs);
        } else if (outputType == String.class) {
            return (T) extractStringResult(outputs);
        } else if (outputType == ClassificationResult.class) {
            return (T) extractClassificationResult(outputs, model);
        } else if (outputType == RegressionResult.class) {
            return (T) extractRegressionResult(outputs);
        } else {
            throw new UnsupportedOperationException(
                "Unsupported output type: " + outputType.getName());
        }
    }
    
    private ClassificationResult extractClassificationResult(Map<String, Tensor<?>> outputs,
                                                           TensorFlowModel model) {
        // Assume the main output contains class probabilities
        Tensor<?> probsTensor = outputs.values().iterator().next();
        
        if (probsTensor instanceof TFloat32 floatTensor) {
            FloatNdArray probs = floatTensor.asRawTensor().data().asFloats();
            
            // Find the class with highest probability
            int predictedClass = 0;
            float maxProb = 0f;
            List<Float> probabilities = new ArrayList<>();
            
            for (int i = 0; i < probs.size(); i++) {
                float prob = probs.getFloat(i);
                probabilities.add(prob);
                
                if (prob > maxProb) {
                    maxProb = prob;
                    predictedClass = i;
                }
            }
            
            // Get class name if available
            String className = model.getClassNames().size() > predictedClass
                ? model.getClassNames().get(predictedClass)
                : String.valueOf(predictedClass);
            
            return new ClassificationResult(
                predictedClass,
                className,
                maxProb,
                probabilities
            );
        }
        
        throw new ModelInferenceException("Unexpected tensor type for classification");
    }
    
    private RegressionResult extractRegressionResult(Map<String, Tensor<?>> outputs) {
        Tensor<?> valueTensor = outputs.values().iterator().next();
        
        if (valueTensor instanceof TFloat32 floatTensor) {
            float predictedValue = floatTensor.asRawTensor().data().asFloats().getFloat(0);
            
            return new RegressionResult(
                predictedValue,
                calculateConfidenceInterval(predictedValue) // Simplified
            );
        }
        
        throw new ModelInferenceException("Unexpected tensor type for regression");
    }
    
    private ConfidenceInterval calculateConfidenceInterval(float predictedValue) {
        // Simplified confidence interval calculation
        // In practice, this would use model uncertainty estimates
        float margin = Math.abs(predictedValue) * 0.1f;
        return new ConfidenceInterval(
            predictedValue - margin,
            predictedValue + margin,
            0.95f
        );
    }
}

// Model Registry for managing ML models
@Service
public class ModelRegistry {
    private final ModelRepository modelRepository;
    private final ModelVersionRepository versionRepository;
    private final ModelStorageService storageService;
    private final ModelValidator modelValidator;
    private final ApplicationEventPublisher eventPublisher;
    
    public ModelMetadata registerModel(ModelRegistrationRequest request) {
        // Validate model
        ModelValidationResult validation = modelValidator.validate(
            request.getModelPath(), request.getModelType());
            
        if (!validation.isValid()) {
            throw new ModelValidationException(
                "Model validation failed: " + validation.getErrors());
        }
        
        // Create model entity
        Model model = new Model();
        model.setName(request.getName());
        model.setDescription(request.getDescription());
        model.setModelType(request.getModelType());
        model.setFramework(request.getFramework());
        model.setCreatedBy(request.getCreatedBy());
        model.setCreatedAt(Instant.now());
        
        Model savedModel = modelRepository.save(model);
        
        // Create version
        ModelVersion version = new ModelVersion();
        version.setModel(savedModel);
        version.setVersion(request.getVersion());
        version.setModelPath(request.getModelPath());
        version.setModelSize(request.getModelSize());
        version.setAccuracy(request.getAccuracy());
        version.setMetrics(request.getMetrics());
        version.setTags(request.getTags());
        version.setInputSpecs(request.getInputSpecs());
        version.setOutputSpecs(request.getOutputSpecs());
        version.setClassNames(request.getClassNames());
        version.setStatus(ModelStatus.REGISTERED);
        version.setCreatedAt(Instant.now());
        
        ModelVersion savedVersion = versionRepository.save(version);
        
        // Store model files if needed
        if (request.getModelFiles() != null) {
            String storedPath = storageService.storeModel(
                savedModel.getId(), 
                savedVersion.getId(),
                request.getModelFiles()
            );
            savedVersion.setModelPath(storedPath);
            versionRepository.save(savedVersion);
        }
        
        // Publish event
        eventPublisher.publishEvent(new ModelRegisteredEvent(
            savedModel.getId(), savedVersion.getId()));
        
        return ModelMetadata.from(savedModel, savedVersion);
    }
    
    public void promoteModel(UUID modelId, String version, ModelStage targetStage) {
        ModelVersion modelVersion = versionRepository
            .findByModelIdAndVersion(modelId, version)
            .orElseThrow(() -> new ModelNotFoundException(
                "Model version not found: " + modelId + ":" + version));
        
        // Validate promotion
        validatePromotion(modelVersion, targetStage);
        
        // Update stage
        ModelStage previousStage = modelVersion.getStage();
        modelVersion.setStage(targetStage);
        modelVersion.setUpdatedAt(Instant.now());
        
        versionRepository.save(modelVersion);
        
        // Handle stage-specific operations
        handleStageTransition(modelVersion, previousStage, targetStage);
        
        // Publish event
        eventPublisher.publishEvent(new ModelPromotedEvent(
            modelId, version, previousStage, targetStage));
    }
    
    public ModelMetadata getModelMetadata(String modelName, String version) {
        Model model = modelRepository.findByName(modelName)
            .orElseThrow(() -> new ModelNotFoundException("Model not found: " + modelName));
            
        ModelVersion modelVersion;
        if ("latest".equals(version)) {
            modelVersion = versionRepository.findLatestByModelId(model.getId())
                .orElseThrow(() -> new ModelNotFoundException(
                    "No versions found for model: " + modelName));
        } else {
            modelVersion = versionRepository.findByModelIdAndVersion(model.getId(), version)
                .orElseThrow(() -> new ModelNotFoundException(
                    "Model version not found: " + modelName + ":" + version));
        }
        
        return ModelMetadata.from(model, modelVersion);
    }
    
    private void handleStageTransition(ModelVersion modelVersion,
                                     ModelStage previousStage,
                                     ModelStage targetStage) {
        switch (targetStage) {
            case PRODUCTION:
                // Deploy to production
                deployToProduction(modelVersion);
                break;
            case STAGING:
                // Deploy to staging
                deployToStaging(modelVersion);
                break;
            case ARCHIVED:
                // Archive model
                archiveModel(modelVersion);
                break;
        }
    }
    
    private void deployToProduction(ModelVersion modelVersion) {
        // Implementation for production deployment
        // This might involve updating load balancers, 
        // blue-green deployments, etc.
    }
}

// Model entities and DTOs
@Entity
@Table(name = "ml_models")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Model {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(unique = true, nullable = false)
    private String name;
    
    private String description;
    
    @Enumerated(EnumType.STRING)
    private ModelType modelType;
    
    @Enumerated(EnumType.STRING)
    private MLFramework framework;
    
    private String createdBy;
    
    private Instant createdAt;
    
    private Instant updatedAt;
    
    @OneToMany(mappedBy = "model", cascade = CascadeType.ALL)
    private List<ModelVersion> versions = new ArrayList<>();
}

@Entity
@Table(name = "model_versions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id")
    private Model model;
    
    @Column(nullable = false)
    private String version;
    
    private String modelPath;
    
    private Long modelSize;
    
    private Double accuracy;
    
    @Convert(converter = JsonConverter.class)
    private Map<String, Object> metrics;
    
    @ElementCollection
    private Set<String> tags = new HashSet<>();
    
    @Convert(converter = JsonConverter.class)
    private List<InputSpec> inputSpecs;
    
    @Convert(converter = JsonConverter.class)
    private List<OutputSpec> outputSpecs;
    
    @ElementCollection
    private List<String> classNames = new ArrayList<>();
    
    @Enumerated(EnumType.STRING)
    private ModelStatus status;
    
    @Enumerated(EnumType.STRING)
    private ModelStage stage;
    
    private Instant createdAt;
    
    private Instant updatedAt;
}

// Data classes for ML operations
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PredictionRequest {
    private Map<String, Object> inputs;
    private Class<?> outputType;
    private Map<String, Object> parameters;
    private String requestId;
    private Instant timestamp;
    
    public static PredictionRequest create() {
        PredictionRequest request = new PredictionRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setTimestamp(Instant.now());
        request.setInputs(new HashMap<>());
        request.setParameters(new HashMap<>());
        return request;
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PredictionResult<T> {
    private boolean successful;
    private T result;
    private PredictionMetadata metadata;
    private String error;
    private Instant timestamp;
    
    public static <T> PredictionResult<T> success(T result, PredictionMetadata metadata) {
        return new PredictionResult<>(true, result, metadata, null, Instant.now());
    }
    
    public static <T> PredictionResult<T> failure(String error) {
        return new PredictionResult<>(false, null, null, error, Instant.now());
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClassificationResult {
    private int predictedClass;
    private String className;
    private float confidence;
    private List<Float> classProbabilities;
    
    public float getProbability(int classIndex) {
        if (classIndex >= 0 && classIndex < classProbabilities.size()) {
            return classProbabilities.get(classIndex);
        }
        return 0f;
    }
    
    public List<ClassPrediction> getTopKPredictions(int k) {
        return IntStream.range(0, classProbabilities.size())
            .boxed()
            .sorted((i, j) -> Float.compare(classProbabilities.get(j), classProbabilities.get(i)))
            .limit(k)
            .map(i -> new ClassPrediction(i, classProbabilities.get(i)))
            .collect(Collectors.toList());
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegressionResult {
    private float predictedValue;
    private ConfidenceInterval confidenceInterval;
    
    public boolean isWithinInterval(float value) {
        return confidenceInterval.contains(value);
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConfidenceInterval {
    private float lowerBound;
    private float upperBound;
    private float confidenceLevel;
    
    public boolean contains(float value) {
        return value >= lowerBound && value <= upperBound;
    }
    
    public float getWidth() {
        return upperBound - lowerBound;
    }
}
```

### 1.2 Model Serving with REST API

```java
// REST Controller for ML Model Serving
@RestController
@RequestMapping("/api/v1/ml")
@Validated
@Slf4j
public class MLModelController {
    private final TensorFlowModelService modelService;
    private final ModelRegistry modelRegistry;
    private final PredictionAuditService auditService;
    private final RateLimitingService rateLimitingService;
    
    @PostMapping("/models/{modelName}/predict")
    public ResponseEntity<PredictionResponseDto> predict(
            @PathVariable String modelName,
            @RequestParam(defaultValue = "latest") String version,
            @Valid @RequestBody PredictionRequestDto request,
            HttpServletRequest httpRequest) {
        
        // Rate limiting
        if (!rateLimitingService.isAllowed(getUserId(httpRequest), modelName)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(PredictionResponseDto.rateLimited());
        }
        
        try {
            // Convert DTO to internal request
            PredictionRequest predictionRequest = convertToInternalRequest(request);
            
            // Make prediction
            PredictionResult<?> result = modelService.predict(
                modelName, version, predictionRequest);
            
            // Audit the prediction
            auditService.auditPrediction(
                getUserId(httpRequest), 
                modelName, 
                version, 
                predictionRequest, 
                result
            );
            
            // Convert result to DTO
            PredictionResponseDto responseDto = convertToResponseDto(result);
            
            return ResponseEntity.ok(responseDto);
            
        } catch (ModelNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (ModelInferenceException e) {
            log.error("Model inference failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(PredictionResponseDto.error(e.getMessage()));
        }
    }
    
    @PostMapping("/models/{modelName}/batch-predict")
    public ResponseEntity<BatchPredictionResponseDto> batchPredict(
            @PathVariable String modelName,
            @RequestParam(defaultValue = "latest") String version,
            @Valid @RequestBody BatchPredictionRequestDto request,
            HttpServletRequest httpRequest) {
        
        // Validate batch size
        if (request.getRequests().size() > 1000) {
            return ResponseEntity.badRequest()
                .body(BatchPredictionResponseDto.error("Batch size exceeds limit"));
        }
        
        try {
            List<PredictionRequest> internalRequests = request.getRequests().stream()
                .map(this::convertToInternalRequest)
                .collect(Collectors.toList());
            
            BatchPredictionResult<?> result = modelService.batchPredict(
                modelName, version, internalRequests);
            
            BatchPredictionResponseDto responseDto = convertToBatchResponseDto(result);
            
            return ResponseEntity.ok(responseDto);
            
        } catch (Exception e) {
            log.error("Batch prediction failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BatchPredictionResponseDto.error(e.getMessage()));
        }
    }
    
    @GetMapping("/models/{modelName}/metadata")
    public ResponseEntity<ModelMetadataDto> getModelMetadata(
            @PathVariable String modelName,
            @RequestParam(defaultValue = "latest") String version) {
        
        try {
            ModelMetadata metadata = modelRegistry.getModelMetadata(modelName, version);
            ModelMetadataDto dto = convertToMetadataDto(metadata);
            
            return ResponseEntity.ok(dto);
            
        } catch (ModelNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/models/{modelName}/explain")
    public ResponseEntity<ExplanationResponseDto> explainPrediction(
            @PathVariable String modelName,
            @RequestParam(defaultValue = "latest") String version,
            @Valid @RequestBody ExplanationRequestDto request) {
        
        try {
            // This would integrate with explainability libraries like LIME or SHAP
            ExplanationResult explanation = modelService.explainPrediction(
                modelName, version, convertToInternalRequest(request.getPredictionRequest()));
            
            ExplanationResponseDto responseDto = convertToExplanationDto(explanation);
            
            return ResponseEntity.ok(responseDto);
            
        } catch (Exception e) {
            log.error("Explanation generation failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ExplanationResponseDto.error(e.getMessage()));
        }
    }
    
    private String getUserId(HttpServletRequest request) {
        // Extract user ID from security context or headers
        return request.getHeader("X-User-ID");
    }
}

// DTOs for API
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PredictionRequestDto {
    @NotNull
    private Map<String, Object> inputs;
    
    private String outputType = "auto";
    
    private Map<String, Object> parameters = new HashMap<>();
    
    @Valid
    private List<@Valid InputValidationRule> validationRules = new ArrayList<>();
}

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PredictionResponseDto {
    private boolean successful;
    private Object result;
    private PredictionMetadataDto metadata;
    private String error;
    private Instant timestamp;
    private Duration processingTime;
    
    public static PredictionResponseDto rateLimited() {
        return new PredictionResponseDto(false, null, null, 
            "Rate limit exceeded", Instant.now(), null);
    }
    
    public static PredictionResponseDto error(String error) {
        return new PredictionResponseDto(false, null, null, 
            error, Instant.now(), null);
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchPredictionRequestDto {
    @NotNull
    @Size(min = 1, max = 1000, message = "Batch size must be between 1 and 1000")
    private List<@Valid PredictionRequestDto> requests;
    
    private boolean stopOnError = false;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExplanationRequestDto {
    @NotNull
    private PredictionRequestDto predictionRequest;
    
    private String explanationMethod = "lime";
    
    private Map<String, Object> explanationParameters = new HashMap<>();
}

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExplanationResponseDto {
    private boolean successful;
    private ExplanationResult explanation;
    private String error;
    
    public static ExplanationResponseDto error(String error) {
        return new ExplanationResponseDto(false, null, error);
    }
}
```

## 2. ML Pipeline Implementation

### 2.1 Spring-based ML Pipeline Framework

```java
// ML Pipeline Framework
@Component
public class MLPipelineEngine {
    private final List<PipelineStage> availableStages;
    private final PipelineExecutor executor;
    private final PipelineMetricsCollector metricsCollector;
    private final ApplicationEventPublisher eventPublisher;
    
    public PipelineExecutionResult executePipeline(PipelineDefinition definition) {
        String executionId = UUID.randomUUID().toString();
        
        try {
            // Validate pipeline
            PipelineValidationResult validation = validatePipeline(definition);
            if (!validation.isValid()) {
                throw new PipelineValidationException(
                    "Pipeline validation failed: " + validation.getErrors());
            }
            
            // Create execution context
            PipelineExecutionContext context = new PipelineExecutionContext(
                executionId, definition, Instant.now());
            
            // Start pipeline execution
            eventPublisher.publishEvent(new PipelineStartedEvent(executionId, definition.getName()));
            
            PipelineExecutionResult result = executor.execute(context);
            
            // Collect metrics
            metricsCollector.recordPipelineExecution(result);
            
            // Publish completion event
            eventPublisher.publishEvent(new PipelineCompletedEvent(
                executionId, definition.getName(), result));
            
            return result;
            
        } catch (Exception e) {
            eventPublisher.publishEvent(new PipelineFailedEvent(
                executionId, definition.getName(), e));
            throw e;
        }
    }
    
    public AsyncPipelineExecution executeAsync(PipelineDefinition definition) {
        CompletableFuture<PipelineExecutionResult> future = 
            CompletableFuture.supplyAsync(() -> executePipeline(definition));
            
        return new AsyncPipelineExecution(
            UUID.randomUUID().toString(), 
            future, 
            definition.getName()
        );
    }
}

// Data preprocessing stage
@Component
@PipelineStage("data-preprocessing")
public class DataPreprocessingStage implements MLPipelineStage {
    private final DataValidationService validationService;
    private final DataTransformationService transformationService;
    private final DataQualityService qualityService;
    
    @Override
    public StageExecutionResult execute(PipelineExecutionContext context) {
        StageConfig config = context.getStageConfig(getName());
        
        try {
            // Load data
            Dataset dataset = loadDataset(config);
            
            // Validate data quality
            DataQualityReport qualityReport = qualityService.assessQuality(dataset);
            if (!qualityReport.meetsQualityThreshold(config.getQualityThreshold())) {
                return StageExecutionResult.failure(
                    "Data quality below threshold: " + qualityReport.getScore());
            }
            
            // Apply transformations
            Dataset transformedDataset = transformationService.transform(dataset, config);
            
            // Store processed data
            String processedDataPath = storeProcessedData(transformedDataset, context);
            
            // Update context
            context.setStageOutput(getName(), Map.of(
                "processedDataPath", processedDataPath,
                "recordCount", transformedDataset.size(),
                "qualityScore", qualityReport.getScore()
            ));
            
            return StageExecutionResult.success(Map.of(
                "processedRecords", transformedDataset.size(),
                "qualityScore", qualityReport.getScore()
            ));
            
        } catch (Exception e) {
            return StageExecutionResult.failure("Data preprocessing failed: " + e.getMessage());
        }
    }
    
    private Dataset loadDataset(StageConfig config) {
        String dataSource = config.getParameter("dataSource");
        String query = config.getParameter("query");
        
        return switch (config.getDataSourceType()) {
            case DATABASE -> loadFromDatabase(dataSource, query);
            case FILE -> loadFromFile(dataSource);
            case API -> loadFromAPI(dataSource);
            case STREAM -> loadFromStream(dataSource);
        };
    }
    
    private Dataset loadFromDatabase(String dataSource, String query) {
        // Implementation for database loading
        return new Dataset(); // Simplified
    }
}

// Model training stage
@Component
@PipelineStage("model-training")
public class ModelTrainingStage implements MLPipelineStage {
    private final ModelTrainingService trainingService;
    private final ModelEvaluationService evaluationService;
    private final ModelRegistry modelRegistry;
    
    @Override
    public StageExecutionResult execute(PipelineExecutionContext context) {
        StageConfig config = context.getStageConfig(getName());
        
        try {
            // Get preprocessed data
            String dataPath = (String) context.getStageOutput("data-preprocessing")
                .get("processedDataPath");
            Dataset dataset = loadDataset(dataPath);
            
            // Split data
            DataSplit dataSplit = splitData(dataset, config);
            
            // Train model
            TrainingResult trainingResult = trainingService.train(
                dataSplit.getTrainingSet(),
                config.getModelConfig()
            );
            
            // Evaluate model
            EvaluationResult evaluationResult = evaluationService.evaluate(
                trainingResult.getModel(),
                dataSplit.getValidationSet()
            );
            
            // Check if model meets criteria
            if (!meetsQualityCriteria(evaluationResult, config)) {
                return StageExecutionResult.failure(
                    "Model does not meet quality criteria");
            }
            
            // Register model
            ModelRegistrationRequest registrationRequest = createRegistrationRequest(
                trainingResult, evaluationResult, config);
            ModelMetadata modelMetadata = modelRegistry.registerModel(registrationRequest);
            
            // Update context
            context.setStageOutput(getName(), Map.of(
                "modelId", modelMetadata.getId(),
                "modelVersion", modelMetadata.getVersion(),
                "accuracy", evaluationResult.getAccuracy(),
                "metrics", evaluationResult.getMetrics()
            ));
            
            return StageExecutionResult.success(Map.of(
                "modelId", modelMetadata.getId(),
                "accuracy", evaluationResult.getAccuracy()
            ));
            
        } catch (Exception e) {
            return StageExecutionResult.failure("Model training failed: " + e.getMessage());
        }
    }
    
    private DataSplit splitData(Dataset dataset, StageConfig config) {
        double trainRatio = config.getDoubleParameter("trainRatio", 0.8);
        double validationRatio = config.getDoubleParameter("validationRatio", 0.1);
        double testRatio = config.getDoubleParameter("testRatio", 0.1);
        
        return dataset.split(trainRatio, validationRatio, testRatio);
    }
    
    private boolean meetsQualityCriteria(EvaluationResult result, StageConfig config) {
        double minAccuracy = config.getDoubleParameter("minAccuracy", 0.8);
        return result.getAccuracy() >= minAccuracy;
    }
}

// Model deployment stage
@Component
@PipelineStage("model-deployment")
public class ModelDeploymentStage implements MLPipelineStage {
    private final ModelDeploymentService deploymentService;
    private final LoadBalancerService loadBalancerService;
    private final HealthCheckService healthCheckService;
    
    @Override
    public StageExecutionResult execute(PipelineExecutionContext context) {
        StageConfig config = context.getStageConfig(getName());
        
        try {
            // Get trained model info
            Map<String, Object> trainingOutput = context.getStageOutput("model-training");
            UUID modelId = (UUID) trainingOutput.get("modelId");
            String modelVersion = (String) trainingOutput.get("modelVersion");
            
            // Deploy model
            DeploymentResult deploymentResult = deploymentService.deploy(
                modelId, modelVersion, config.getDeploymentConfig());
            
            // Wait for deployment to be ready
            waitForDeploymentReady(deploymentResult.getDeploymentId());
            
            // Run health checks
            HealthCheckResult healthCheck = healthCheckService.checkHealth(
                deploymentResult.getEndpoint());
            
            if (!healthCheck.isHealthy()) {
                // Rollback deployment
                deploymentService.rollback(deploymentResult.getDeploymentId());
                return StageExecutionResult.failure("Health check failed after deployment");
            }
            
            // Update load balancer if configured
            if (config.getBooleanParameter("updateLoadBalancer", false)) {
                loadBalancerService.updateUpstreams(
                    config.getParameter("loadBalancerName"),
                    List.of(deploymentResult.getEndpoint())
                );
            }
            
            // Update context
            context.setStageOutput(getName(), Map.of(
                "deploymentId", deploymentResult.getDeploymentId(),
                "endpoint", deploymentResult.getEndpoint(),
                "healthStatus", healthCheck.getStatus()
            ));
            
            return StageExecutionResult.success(Map.of(
                "deploymentId", deploymentResult.getDeploymentId(),
                "endpoint", deploymentResult.getEndpoint()
            ));
            
        } catch (Exception e) {
            return StageExecutionResult.failure("Model deployment failed: " + e.getMessage());
        }
    }
    
    private void waitForDeploymentReady(String deploymentId) throws InterruptedException {
        int maxRetries = 30;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            if (deploymentService.isDeploymentReady(deploymentId)) {
                return;
            }
            
            Thread.sleep(10000); // Wait 10 seconds
            retryCount++;
        }
        
        throw new DeploymentTimeoutException("Deployment did not become ready in time");
    }
}

// Pipeline definition and configuration
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PipelineDefinition {
    private String name;
    private String description;
    private String version;
    private List<StageDefinition> stages;
    private Map<String, Object> globalParameters;
    private PipelineSchedule schedule;
    private List<String> tags;
    private String createdBy;
    
    public StageDefinition getStage(String stageName) {
        return stages.stream()
            .filter(stage -> stage.getName().equals(stageName))
            .findFirst()
            .orElse(null);
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StageDefinition {
    private String name;
    private String type;
    private Map<String, Object> parameters;
    private List<String> dependencies;
    private RetryConfig retryConfig;
    private Duration timeout;
    
    public boolean dependsOn(String stageName) {
        return dependencies != null && dependencies.contains(stageName);
    }
}
```

### 2.2 Real-time Streaming ML Pipeline

```java
// Streaming ML Pipeline with Kafka
@Component
public class StreamingMLPipeline {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TensorFlowModelService modelService;
    private final StreamingMetricsCollector metricsCollector;
    private final FeatureStore featureStore;
    
    @KafkaListener(topics = "raw-events", groupId = "ml-pipeline")
    public void processEvent(@Payload EventData eventData,
                           @Header Map<String, Object> headers) {
        Timer.Sample sample = Timer.start();
        
        try {
            // Extract features
            FeatureVector features = extractFeatures(eventData);
            
            // Enrich with historical features if needed
            if (requiresEnrichment(eventData)) {
                features = featureStore.enrichFeatures(features, eventData.getUserId());
            }
            
            // Make prediction
            PredictionRequest request = createPredictionRequest(features);
            PredictionResult<?> result = modelService.predict(
                getModelName(eventData.getEventType()), 
                "latest", 
                request
            );
            
            // Create enriched event
            EnrichedEvent enrichedEvent = new EnrichedEvent(
                eventData,
                features,
                result,
                Instant.now()
            );
            
            // Send to downstream topic
            kafkaTemplate.send("enriched-events", 
                eventData.getUserId(), 
                enrichedEvent);
            
            // Update feature store if needed
            if (shouldUpdateFeatureStore(result)) {
                featureStore.updateFeatures(eventData.getUserId(), features);
            }
            
            // Record metrics
            metricsCollector.recordEventProcessed(
                eventData.getEventType(), 
                sample.stop()
            );
            
        } catch (Exception e) {
            log.error("Failed to process event: {}", eventData.getId(), e);
            
            // Send to dead letter queue
            kafkaTemplate.send("ml-pipeline-dlq", 
                eventData.getUserId(),
                new FailedEvent(eventData, e.getMessage(), Instant.now()));
            
            metricsCollector.recordEventFailed(eventData.getEventType(), e);
        }
    }
    
    private FeatureVector extractFeatures(EventData eventData) {
        return switch (eventData.getEventType()) {
            case USER_ACTION -> extractUserActionFeatures(eventData);
            case TRANSACTION -> extractTransactionFeatures(eventData);
            case SESSION_EVENT -> extractSessionFeatures(eventData);
            default -> throw new UnsupportedOperationException(
                "Unsupported event type: " + eventData.getEventType());
        };
    }
    
    private FeatureVector extractUserActionFeatures(EventData eventData) {
        Map<String, Object> rawData = eventData.getData();
        
        return FeatureVector.builder()
            .addFeature("action_type", (String) rawData.get("actionType"))
            .addFeature("timestamp_hour", Instant.now().atZone(ZoneOffset.UTC).getHour())
            .addFeature("page_url", (String) rawData.get("pageUrl"))
            .addFeature("user_agent", (String) rawData.get("userAgent"))
            .addFeature("session_duration", calculateSessionDuration(eventData))
            .build();
    }
    
    private FeatureVector extractTransactionFeatures(EventData eventData) {
        Map<String, Object> rawData = eventData.getData();
        
        return FeatureVector.builder()
            .addFeature("amount", ((Number) rawData.get("amount")).doubleValue())
            .addFeature("currency", (String) rawData.get("currency"))
            .addFeature("merchant_category", (String) rawData.get("merchantCategory"))
            .addFeature("transaction_hour", Instant.now().atZone(ZoneOffset.UTC).getHour())
            .addFeature("is_weekend", isWeekend(Instant.now()))
            .build();
    }
}

// Feature Store implementation
@Service
public class FeatureStoreService implements FeatureStore {
    private final RedisTemplate<String, Object> redisTemplate;
    private final FeatureRepository featureRepository;
    private final FeatureTransformationService transformationService;
    
    @Override
    public FeatureVector enrichFeatures(FeatureVector currentFeatures, String entityId) {
        // Get historical features from cache
        Map<String, Object> historicalFeatures = getHistoricalFeatures(entityId);
        
        // Get aggregate features
        Map<String, Object> aggregateFeatures = getAggregateFeatures(entityId);
        
        // Combine features
        FeatureVector.Builder enrichedBuilder = currentFeatures.toBuilder();
        
        historicalFeatures.forEach(enrichedBuilder::addFeature);
        aggregateFeatures.forEach(enrichedBuilder::addFeature);
        
        return enrichedBuilder.build();
    }
    
    @Override
    public void updateFeatures(String entityId, FeatureVector features) {
        // Update real-time features in cache
        String cacheKey = "features:" + entityId;
        Map<String, Object> featureMap = features.toMap();
        
        redisTemplate.opsForHash().putAll(cacheKey, featureMap);
        redisTemplate.expire(cacheKey, Duration.ofHours(24));
        
        // Update aggregate features asynchronously
        CompletableFuture.runAsync(() -> updateAggregateFeatures(entityId, features));
    }
    
    private Map<String, Object> getHistoricalFeatures(String entityId) {
        String cacheKey = "features:" + entityId;
        
        Map<Object, Object> cachedFeatures = redisTemplate.opsForHash().entries(cacheKey);
        
        if (cachedFeatures.isEmpty()) {
            // Load from persistent storage
            return loadFeaturesFromDatabase(entityId);
        }
        
        return cachedFeatures.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey().toString(),
                Map.Entry::getValue
            ));
    }
    
    private Map<String, Object> getAggregateFeatures(String entityId) {
        // Calculate aggregate features like:
        // - Average transaction amount last 30 days
        // - Count of actions last 7 days
        // - Most frequent action type
        
        return Map.of(
            "avg_transaction_30d", calculateAverageTransaction(entityId, 30),
            "action_count_7d", calculateActionCount(entityId, 7),
            "most_frequent_action", getMostFrequentAction(entityId)
        );
    }
}

// Model performance monitoring
@Component
public class ModelPerformanceMonitor {
    private final MeterRegistry meterRegistry;
    private final ModelRegistry modelRegistry;
    private final AlertingService alertingService;
    private final ScheduledExecutorService scheduler;
    
    @PostConstruct
    public void startMonitoring() {
        scheduler.scheduleAtFixedRate(this::checkModelPerformance, 5, 5, TimeUnit.MINUTES);
    }
    
    @EventListener
    public void handlePredictionResult(PredictionCompletedEvent event) {
        // Record prediction metrics
        recordPredictionMetrics(event);
        
        // Check for drift
        checkDataDrift(event);
        
        // Update model performance statistics
        updatePerformanceStats(event);
    }
    
    private void checkModelPerformance() {
        List<ModelMetadata> models = modelRegistry.getActiveModels();
        
        for (ModelMetadata model : models) {
            ModelPerformanceMetrics metrics = calculatePerformanceMetrics(model);
            
            // Check accuracy degradation
            if (metrics.getAccuracyDegradation() > 0.1) {
                alertingService.sendAlert(new ModelPerformanceAlert(
                    model.getName(),
                    "Accuracy degradation detected",
                    metrics.getAccuracyDegradation()
                ));
            }
            
            // Check prediction latency
            if (metrics.getAverageLatency().toMillis() > 1000) {
                alertingService.sendAlert(new ModelLatencyAlert(
                    model.getName(),
                    "High prediction latency",
                    metrics.getAverageLatency()
                ));
            }
            
            // Check data drift
            if (metrics.getDataDriftScore() > 0.3) {
                alertingService.sendAlert(new DataDriftAlert(
                    model.getName(),
                    "Data drift detected",
                    metrics.getDataDriftScore()
                ));
            }
        }
    }
    
    private void checkDataDrift(PredictionCompletedEvent event) {
        // Implement statistical tests for data drift detection
        // Using techniques like KS test, PSI (Population Stability Index)
        
        FeatureVector currentFeatures = event.getFeatures();
        FeatureDistribution referenceDistribution = getReferenceDistribution(
            event.getModelName());
        
        double driftScore = calculateDriftScore(currentFeatures, referenceDistribution);
        
        // Record drift metrics
        Gauge.builder("model.data.drift.score")
            .tag("model", event.getModelName())
            .register(meterRegistry, driftScore, Double::doubleValue);
            
        if (driftScore > 0.3) {
            // Trigger model retraining
            triggerModelRetraining(event.getModelName(), driftScore);
        }
    }
}
```

## 3. Practical Exercises

### Exercise 1: Implement Image Classification Service
Create a complete image classification service using TensorFlow Java:
- Load and serve a pre-trained image classification model
- Implement REST API for image uploads and predictions
- Add batch processing capabilities
- Include confidence scoring and top-K predictions

### Exercise 2: Build Real-time Fraud Detection Pipeline
Implement a streaming fraud detection system:
- Create Kafka-based event processing pipeline
- Implement feature extraction and enrichment
- Build real-time scoring with TensorFlow models
- Add alert generation and response mechanisms

### Exercise 3: Develop MLOps Pipeline
Create a comprehensive MLOps pipeline:
- Implement automated model training and validation
- Build model registry with versioning
- Create deployment automation with rollback capabilities
- Add comprehensive monitoring and alerting

## 4. Assessment Questions

1. **Model Integration Architecture**: Design a scalable architecture for serving multiple ML models in a microservices environment. Include load balancing, caching, and monitoring.

2. **Real-time ML Pipeline**: Implement a real-time ML pipeline that processes streaming data, applies feature transformations, and serves predictions with sub-second latency.

3. **MLOps Implementation**: Create a complete MLOps workflow including automated training, testing, deployment, and monitoring with proper CI/CD integration.

4. **Performance Optimization**: Optimize an ML inference service for high throughput and low latency, including batching, model optimization, and resource management.

## Summary

Today you mastered AI/ML integration in enterprise Java including:

- **TensorFlow Java**: Advanced model serving and inference capabilities
- **ML Pipelines**: Comprehensive pipeline frameworks for training and deployment
- **Real-time Processing**: Streaming ML with Kafka and feature stores
- **MLOps Practices**: Model registry, versioning, and deployment automation
- **Performance Monitoring**: Data drift detection and model performance monitoring

These capabilities enable you to build intelligent, learning enterprise systems that can adapt and improve over time.

## Next Steps

Tomorrow, we'll explore **Advanced Observability & Site Reliability Engineering** focusing on OpenTelemetry integration, SLIs/SLOs implementation, error budgets, and building highly reliable, observable production systems.