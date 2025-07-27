# Day 97: Machine Learning Integration - ML Pipelines with Java

## Learning Objectives
- Integrate machine learning models with Java applications
- Build ML pipelines using Java ML libraries
- Implement real-time model inference and batch processing
- Create model training and deployment workflows
- Build intelligent features with ML in enterprise Java applications

## 1. Java ML Ecosystem Overview

### Setting Up ML Dependencies

```xml
<!-- pom.xml for Java ML integration -->
<properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <spring-boot.version>3.2.0</spring-boot.version>
    <tensorflow.version>0.5.0</tensorflow.version>
    <deeplearning4j.version>1.0.0-M2.1</deeplearning4j.version>
    <weka.version>3.8.6</weka.version>
</properties>

<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- TensorFlow Java -->
    <dependency>
        <groupId>org.tensorflow</groupId>
        <artifactId>tensorflow-core-platform</artifactId>
        <version>${tensorflow.version}</version>
    </dependency>
    
    <!-- DeepLearning4J -->
    <dependency>
        <groupId>org.deeplearning4j</groupId>
        <artifactId>deeplearning4j-core</artifactId>
        <version>${deeplearning4j.version}</version>
    </dependency>
    
    <dependency>
        <groupId>org.nd4j</groupId>
        <artifactId>nd4j-native-platform</artifactId>
        <version>${deeplearning4j.version}</version>
    </dependency>
    
    <!-- Weka for traditional ML -->
    <dependency>
        <groupId>nz.ac.waikato.cms.weka</groupId>
        <artifactId>weka-stable</artifactId>
        <version>${weka.version}</version>
    </dependency>
    
    <!-- Smile ML library -->
    <dependency>
        <groupId>com.github.haifengl</groupId>
        <artifactId>smile-core</artifactId>
        <version>3.0.2</version>
    </dependency>
    
    <!-- Apache Spark for big data ML -->
    <dependency>
        <groupId>org.apache.spark</groupId>
        <artifactId>spark-mllib_2.12</artifactId>
        <version>3.5.0</version>
    </dependency>
    
    <!-- ONNX Runtime for model deployment -->
    <dependency>
        <groupId>com.microsoft.onnxruntime</groupId>
        <artifactId>onnxruntime</artifactId>
        <version>1.16.3</version>
    </dependency>
</dependencies>
```

### ML Configuration and Setup

```java
package com.javajourney.ml;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@SpringBootApplication
@EnableAsync
public class MLApplication {
    
    public static void main(String[] args) {
        // Set system properties for ML libraries
        System.setProperty("org.bytedeco.javacpp.maxbytes", "8G");
        System.setProperty("org.bytedeco.javacpp.maxphysicalbytes", "8G");
        
        SpringApplication.run(MLApplication.class, args);
    }
}

@Configuration
public class MLConfiguration {
    
    @Bean
    public MLModelRegistry modelRegistry() {
        return new MLModelRegistry();
    }
    
    @Bean
    public DataPreprocessor dataPreprocessor() {
        return new DataPreprocessor();
    }
    
    @Bean
    public ModelInferenceService modelInferenceService() {
        return new ModelInferenceService();
    }
    
    @Bean
    public MLPipelineOrchestrator pipelineOrchestrator() {
        return new MLPipelineOrchestrator();
    }
    
    @Bean(name = "mlExecutor")
    public Executor mlExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ML-");
        executor.initialize();
        return executor;
    }
}

// ML Model Registry for managing multiple models
@Component
public class MLModelRegistry {
    
    private final Map<String, MLModel> models = new ConcurrentHashMap<>();
    private final Map<String, ModelMetadata> metadata = new ConcurrentHashMap<>();
    
    public void registerModel(String modelId, MLModel model, ModelMetadata metadata) {
        models.put(modelId, model);
        this.metadata.put(modelId, metadata);
        System.out.println("Registered model: " + modelId + " - " + metadata.description());
    }
    
    public Optional<MLModel> getModel(String modelId) {
        return Optional.ofNullable(models.get(modelId));
    }
    
    public Optional<ModelMetadata> getModelMetadata(String modelId) {
        return Optional.ofNullable(metadata.get(modelId));
    }
    
    public Set<String> getAvailableModels() {
        return Set.copyOf(models.keySet());
    }
    
    public void unregisterModel(String modelId) {
        MLModel model = models.remove(modelId);
        metadata.remove(modelId);
        
        if (model != null) {
            try {
                model.close();
            } catch (Exception e) {
                System.err.println("Error closing model " + modelId + ": " + e.getMessage());
            }
        }
    }
    
    // Data records
    public record ModelMetadata(
        String name,
        String version,
        String description,
        String modelType,
        List<String> inputFeatures,
        List<String> outputClasses,
        LocalDateTime createdAt,
        Map<String, Object> hyperparameters
    ) {}
    
    // ML Model interface
    public interface MLModel extends AutoCloseable {
        PredictionResult predict(ModelInput input);
        BatchPredictionResult batchPredict(List<ModelInput> inputs);
        ModelMetadata getMetadata();
        boolean isLoaded();
        void reload() throws Exception;
    }
    
    public record ModelInput(Map<String, Object> features) {}
    
    public record PredictionResult(
        Object prediction,
        Map<String, Double> probabilities,
        double confidence,
        long inferenceTimeMs
    ) {}
    
    public record BatchPredictionResult(
        List<PredictionResult> results,
        long totalInferenceTimeMs,
        ModelStatistics statistics
    ) {}
    
    public record ModelStatistics(
        double averageConfidence,
        double minConfidence,
        double maxConfidence,
        Map<String, Integer> predictionCounts
    ) {}
}
```

## 2. Traditional Machine Learning with Weka and Smile

### Classification and Regression Models

```java
package com.javajourney.ml.traditional;

import smile.classification.RandomForest;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.io.Read;
import smile.regression.LinearRegression;
import smile.validation.CrossValidation;
import smile.validation.metric.Accuracy;
import smile.validation.metric.RMSE;
import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomTree;
import weka.core.Instances;
import weka.core.converters.CSVLoader;

import java.io.File;
import java.util.concurrent.CompletableFuture;

@Component
public class TraditionalMLService {
    
    private final MLModelRegistry modelRegistry;
    private final DataPreprocessor dataPreprocessor;
    
    public TraditionalMLService(MLModelRegistry modelRegistry, DataPreprocessor dataPreprocessor) {
        this.modelRegistry = modelRegistry;
        this.dataPreprocessor = dataPreprocessor;
    }
    
    // Customer churn prediction using Smile Random Forest
    public CompletableFuture<ChurnPredictionModel> trainChurnPredictionModel(String dataPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("Training churn prediction model...");
                
                // Load data
                DataFrame data = Read.csv(dataPath);
                
                // Prepare features and target
                Formula formula = Formula.lhs("churn");
                
                // Split data into training and testing
                int trainSize = (int) (data.nrow() * 0.8);
                DataFrame trainData = data.slice(0, trainSize);
                DataFrame testData = data.slice(trainSize, data.nrow());
                
                // Train Random Forest
                RandomForest model = RandomForest.fit(formula, trainData);
                
                // Evaluate model
                int[] testPredictions = model.predict(testData);
                int[] testLabels = testData.column("churn").toIntArray();
                double accuracy = Accuracy.of(testLabels, testPredictions);
                
                System.out.printf("Churn prediction model trained with accuracy: %.3f%n", accuracy);
                
                // Create model wrapper
                ChurnPredictionModel churnModel = new ChurnPredictionModel(model, accuracy);
                
                // Register model
                MLModelRegistry.ModelMetadata metadata = new MLModelRegistry.ModelMetadata(
                    "Customer Churn Prediction",
                    "1.0",
                    "Predicts customer churn using Random Forest",
                    "CLASSIFICATION",
                    List.of("tenure", "monthly_charges", "total_charges", "contract_type", "payment_method"),
                    List.of("churn", "no_churn"),
                    LocalDateTime.now(),
                    Map.of("n_trees", 100, "max_depth", 10)
                );
                
                modelRegistry.registerModel("churn_prediction", churnModel, metadata);
                
                return churnModel;
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to train churn prediction model", e);
            }
        });
    }
    
    // Price prediction using Smile Linear Regression
    public CompletableFuture<PricePredictionModel> trainPricePredictionModel(String dataPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("Training price prediction model...");
                
                // Load housing data
                DataFrame data = Read.csv(dataPath);
                
                // Prepare features and target
                Formula formula = Formula.lhs("price");
                
                // Split data
                int trainSize = (int) (data.nrow() * 0.8);
                DataFrame trainData = data.slice(0, trainSize);
                DataFrame testData = data.slice(trainSize, data.nrow());
                
                // Train Linear Regression
                LinearRegression model = LinearRegression.fit(formula, trainData);
                
                // Evaluate model
                double[] testPredictions = model.predict(testData);
                double[] testLabels = testData.column("price").toDoubleArray();
                double rmse = RMSE.of(testLabels, testPredictions);
                
                System.out.printf("Price prediction model trained with RMSE: %.2f%n", rmse);
                
                // Create model wrapper
                PricePredictionModel priceModel = new PricePredictionModel(model, rmse);
                
                // Register model
                MLModelRegistry.ModelMetadata metadata = new MLModelRegistry.ModelMetadata(
                    "House Price Prediction",
                    "1.0",
                    "Predicts house prices using Linear Regression",
                    "REGRESSION",
                    List.of("bedrooms", "bathrooms", "sqft_living", "sqft_lot", "floors", "condition", "grade"),
                    List.of("price"),
                    LocalDateTime.now(),
                    Map.of("regularization", "none")
                );
                
                modelRegistry.registerModel("price_prediction", priceModel, metadata);
                
                return priceModel;
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to train price prediction model", e);
            }
        });
    }
    
    // Anomaly detection using Weka
    public CompletableFuture<AnomalyDetectionModel> trainAnomalyDetectionModel(String dataPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("Training anomaly detection model...");
                
                // Load data using Weka
                CSVLoader loader = new CSVLoader();
                loader.setSource(new File(dataPath));
                Instances data = loader.getDataSet();
                
                // Set class index (last attribute)
                if (data.classIndex() == -1) {
                    data.setClassIndex(data.numAttributes() - 1);
                }
                
                // Train classifier (using Random Tree for simplicity)
                Classifier classifier = new RandomTree();
                classifier.buildClassifier(data);
                
                // Evaluate using cross-validation
                double accuracy = CrossValidation.classification(10, (trainData, testData) -> {
                    try {
                        Classifier model = new RandomTree();
                        model.buildClassifier(trainData);
                        
                        int correct = 0;
                        for (int i = 0; i < testData.numInstances(); i++) {
                            double predicted = model.classifyInstance(testData.instance(i));
                            double actual = testData.instance(i).classValue();
                            if (predicted == actual) correct++;
                        }
                        return (double) correct / testData.numInstances();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, data);
                
                System.out.printf("Anomaly detection model trained with accuracy: %.3f%n", accuracy);
                
                // Create model wrapper
                AnomalyDetectionModel anomalyModel = new AnomalyDetectionModel(classifier, accuracy);
                
                // Register model
                MLModelRegistry.ModelMetadata metadata = new MLModelRegistry.ModelMetadata(
                    "Network Anomaly Detection",
                    "1.0",
                    "Detects network anomalies using Random Tree",
                    "CLASSIFICATION",
                    List.of("duration", "protocol_type", "service", "flag", "src_bytes", "dst_bytes"),
                    List.of("normal", "anomaly"),
                    LocalDateTime.now(),
                    Map.of("min_num_obj", 1)
                );
                
                modelRegistry.registerModel("anomaly_detection", anomalyModel, metadata);
                
                return anomalyModel;
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to train anomaly detection model", e);
            }
        });
    }
    
    // Model implementations
    public static class ChurnPredictionModel implements MLModelRegistry.MLModel {
        private final RandomForest model;
        private final double accuracy;
        
        public ChurnPredictionModel(RandomForest model, double accuracy) {
            this.model = model;
            this.accuracy = accuracy;
        }
        
        @Override
        public MLModelRegistry.PredictionResult predict(MLModelRegistry.ModelInput input) {
            long startTime = System.currentTimeMillis();
            
            try {
                // Convert input to format expected by Smile
                double[] features = extractFeatures(input);
                
                // Make prediction
                int prediction = model.predict(features);
                String churnLabel = prediction == 1 ? "churn" : "no_churn";
                
                // Get probabilities (simplified)
                Map<String, Double> probabilities = Map.of(
                    "churn", prediction == 1 ? 0.8 : 0.2,
                    "no_churn", prediction == 0 ? 0.8 : 0.2
                );
                
                long inferenceTime = System.currentTimeMillis() - startTime;
                
                return new MLModelRegistry.PredictionResult(
                    churnLabel,
                    probabilities,
                    Math.max(probabilities.get("churn"), probabilities.get("no_churn")),
                    inferenceTime
                );
                
            } catch (Exception e) {
                throw new RuntimeException("Prediction failed", e);
            }
        }
        
        @Override
        public MLModelRegistry.BatchPredictionResult batchPredict(List<MLModelRegistry.ModelInput> inputs) {
            long startTime = System.currentTimeMillis();
            
            List<MLModelRegistry.PredictionResult> results = inputs.stream()
                .map(this::predict)
                .toList();
            
            long totalTime = System.currentTimeMillis() - startTime;
            
            // Calculate statistics
            double avgConfidence = results.stream()
                .mapToDouble(MLModelRegistry.PredictionResult::confidence)
                .average()
                .orElse(0.0);
            
            double minConfidence = results.stream()
                .mapToDouble(MLModelRegistry.PredictionResult::confidence)
                .min()
                .orElse(0.0);
            
            double maxConfidence = results.stream()
                .mapToDouble(MLModelRegistry.PredictionResult::confidence)
                .max()
                .orElse(0.0);
            
            Map<String, Integer> predictionCounts = results.stream()
                .collect(Collectors.groupingBy(
                    r -> r.prediction().toString(),
                    Collectors.summingInt(r -> 1)
                ));
            
            MLModelRegistry.ModelStatistics stats = new MLModelRegistry.ModelStatistics(
                avgConfidence, minConfidence, maxConfidence, predictionCounts
            );
            
            return new MLModelRegistry.BatchPredictionResult(results, totalTime, stats);
        }
        
        private double[] extractFeatures(MLModelRegistry.ModelInput input) {
            Map<String, Object> features = input.features();
            return new double[] {
                ((Number) features.get("tenure")).doubleValue(),
                ((Number) features.get("monthly_charges")).doubleValue(),
                ((Number) features.get("total_charges")).doubleValue(),
                encodeContract((String) features.get("contract_type")),
                encodePayment((String) features.get("payment_method"))
            };
        }
        
        private double encodeContract(String contract) {
            return switch (contract.toLowerCase()) {
                case "month-to-month" -> 0.0;
                case "one year" -> 1.0;
                case "two year" -> 2.0;
                default -> 0.0;
            };
        }
        
        private double encodePayment(String payment) {
            return switch (payment.toLowerCase()) {
                case "electronic check" -> 0.0;
                case "mailed check" -> 1.0;
                case "bank transfer" -> 2.0;
                case "credit card" -> 3.0;
                default -> 0.0;
            };
        }
        
        @Override
        public MLModelRegistry.ModelMetadata getMetadata() {
            return new MLModelRegistry.ModelMetadata(
                "Customer Churn Prediction",
                "1.0",
                "Random Forest model for predicting customer churn",
                "CLASSIFICATION",
                List.of("tenure", "monthly_charges", "total_charges", "contract_type", "payment_method"),
                List.of("churn", "no_churn"),
                LocalDateTime.now(),
                Map.of("accuracy", accuracy)
            );
        }
        
        @Override
        public boolean isLoaded() {
            return model != null;
        }
        
        @Override
        public void reload() throws Exception {
            // Model is already in memory, nothing to reload
        }
        
        @Override
        public void close() throws Exception {
            // Smile models don't require explicit cleanup
        }
    }
    
    // Similar implementations for PricePredictionModel and AnomalyDetectionModel
    public static class PricePredictionModel implements MLModelRegistry.MLModel {
        private final LinearRegression model;
        private final double rmse;
        
        public PricePredictionModel(LinearRegression model, double rmse) {
            this.model = model;
            this.rmse = rmse;
        }
        
        @Override
        public MLModelRegistry.PredictionResult predict(MLModelRegistry.ModelInput input) {
            long startTime = System.currentTimeMillis();
            
            try {
                double[] features = extractHouseFeatures(input);
                double prediction = model.predict(features);
                
                long inferenceTime = System.currentTimeMillis() - startTime;
                
                return new MLModelRegistry.PredictionResult(
                    prediction,
                    Map.of("price", 1.0), // Regression doesn't have probabilities
                    1.0, // Confidence based on model performance
                    inferenceTime
                );
                
            } catch (Exception e) {
                throw new RuntimeException("Price prediction failed", e);
            }
        }
        
        private double[] extractHouseFeatures(MLModelRegistry.ModelInput input) {
            Map<String, Object> features = input.features();
            return new double[] {
                ((Number) features.get("bedrooms")).doubleValue(),
                ((Number) features.get("bathrooms")).doubleValue(),
                ((Number) features.get("sqft_living")).doubleValue(),
                ((Number) features.get("sqft_lot")).doubleValue(),
                ((Number) features.get("floors")).doubleValue(),
                ((Number) features.get("condition")).doubleValue(),
                ((Number) features.get("grade")).doubleValue()
            };
        }
        
        // Implement other required methods...
        @Override
        public MLModelRegistry.BatchPredictionResult batchPredict(List<MLModelRegistry.ModelInput> inputs) {
            return null; // Implementation similar to ChurnPredictionModel
        }
        
        @Override
        public MLModelRegistry.ModelMetadata getMetadata() {
            return null; // Implementation
        }
        
        @Override
        public boolean isLoaded() {
            return model != null;
        }
        
        @Override
        public void reload() throws Exception {}
        
        @Override
        public void close() throws Exception {}
    }
    
    public static class AnomalyDetectionModel implements MLModelRegistry.MLModel {
        private final Classifier model;
        private final double accuracy;
        
        public AnomalyDetectionModel(Classifier model, double accuracy) {
            this.model = model;
            this.accuracy = accuracy;
        }
        
        // Implement required methods similar to above...
        @Override
        public MLModelRegistry.PredictionResult predict(MLModelRegistry.ModelInput input) {
            return null; // Implementation
        }
        
        @Override
        public MLModelRegistry.BatchPredictionResult batchPredict(List<MLModelRegistry.ModelInput> inputs) {
            return null; // Implementation
        }
        
        @Override
        public MLModelRegistry.ModelMetadata getMetadata() {
            return null; // Implementation
        }
        
        @Override
        public boolean isLoaded() {
            return model != null;
        }
        
        @Override
        public void reload() throws Exception {}
        
        @Override
        public void close() throws Exception {}
    }
}
```

## 3. Deep Learning with DeepLearning4J

### Neural Network Implementation

```java
package com.javajourney.ml.deeplearning;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.util.concurrent.CompletableFuture;

@Component
public class DeepLearningService {
    
    private final MLModelRegistry modelRegistry;
    
    public DeepLearningService(MLModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry;
    }
    
    // Sentiment analysis neural network
    public CompletableFuture<SentimentAnalysisModel> trainSentimentModel(DataSetIterator trainData, DataSetIterator testData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("Training sentiment analysis neural network...");
                
                // Build neural network configuration
                MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
                    .seed(12345)
                    .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                    .updater(new Adam(0.001))
                    .l2(1e-4)
                    .list()
                    .layer(0, new DenseLayer.Builder()
                        .nIn(1000) // Input features (e.g., word embeddings)
                        .nOut(256)
                        .activation(Activation.RELU)
                        .weightInit(WeightInit.XAVIER)
                        .build())
                    .layer(1, new DenseLayer.Builder()
                        .nIn(256)
                        .nOut(128)
                        .activation(Activation.RELU)
                        .weightInit(WeightInit.XAVIER)
                        .dropOut(0.5)
                        .build())
                    .layer(2, new DenseLayer.Builder()
                        .nIn(128)
                        .nOut(64)
                        .activation(Activation.RELU)
                        .weightInit(WeightInit.XAVIER)
                        .dropOut(0.3)
                        .build())
                    .layer(3, new OutputLayer.Builder()
                        .nIn(64)
                        .nOut(3) // 3 classes: positive, negative, neutral
                        .activation(Activation.SOFTMAX)
                        .weightInit(WeightInit.XAVIER)
                        .lossFunction(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .build())
                    .build();
                
                // Create and initialize network
                MultiLayerNetwork model = new MultiLayerNetwork(config);
                model.init();
                model.setListeners(new ScoreIterationListener(100));
                
                // Train the model
                int numEpochs = 50;
                for (int epoch = 0; epoch < numEpochs; epoch++) {
                    System.out.printf("Training epoch %d/%d%n", epoch + 1, numEpochs);
                    model.fit(trainData);
                    trainData.reset();
                    
                    // Evaluate on test data every 10 epochs
                    if ((epoch + 1) % 10 == 0) {
                        evaluateModel(model, testData);
                        testData.reset();
                    }
                }
                
                // Final evaluation
                double accuracy = evaluateModel(model, testData);
                testData.reset();
                
                System.out.printf("Sentiment analysis model trained with accuracy: %.3f%n", accuracy);
                
                // Create model wrapper
                SentimentAnalysisModel sentimentModel = new SentimentAnalysisModel(model, accuracy);
                
                // Register model
                MLModelRegistry.ModelMetadata metadata = new MLModelRegistry.ModelMetadata(
                    "Sentiment Analysis Neural Network",
                    "1.0",
                    "Deep neural network for sentiment analysis",
                    "CLASSIFICATION",
                    List.of("text_embeddings"),
                    List.of("positive", "negative", "neutral"),
                    LocalDateTime.now(),
                    Map.of(
                        "epochs", numEpochs,
                        "learning_rate", 0.001,
                        "layers", 4,
                        "dropout", 0.5
                    )
                );
                
                modelRegistry.registerModel("sentiment_analysis", sentimentModel, metadata);
                
                return sentimentModel;
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to train sentiment analysis model", e);
            }
        });
    }
    
    // Fraud detection neural network
    public CompletableFuture<FraudDetectionModel> trainFraudDetectionModel(DataSetIterator trainData, DataSetIterator testData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("Training fraud detection neural network...");
                
                // Build configuration for fraud detection
                MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
                    .seed(12345)
                    .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                    .updater(new Adam(0.0001)) // Lower learning rate for imbalanced data
                    .l2(1e-3)
                    .list()
                    .layer(0, new DenseLayer.Builder()
                        .nIn(30) // Transaction features
                        .nOut(64)
                        .activation(Activation.RELU)
                        .weightInit(WeightInit.XAVIER)
                        .build())
                    .layer(1, new DenseLayer.Builder()
                        .nIn(64)
                        .nOut(32)
                        .activation(Activation.RELU)
                        .weightInit(WeightInit.XAVIER)
                        .dropOut(0.3)
                        .build())
                    .layer(2, new DenseLayer.Builder()
                        .nIn(32)
                        .nOut(16)
                        .activation(Activation.RELU)
                        .weightInit(WeightInit.XAVIER)
                        .dropOut(0.2)
                        .build())
                    .layer(3, new OutputLayer.Builder()
                        .nIn(16)
                        .nOut(2) // Binary classification: fraud/legitimate
                        .activation(Activation.SOFTMAX)
                        .weightInit(WeightInit.XAVIER)
                        .lossFunction(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .build())
                    .build();
                
                MultiLayerNetwork model = new MultiLayerNetwork(config);
                model.init();
                model.setListeners(new ScoreIterationListener(50));
                
                // Train with early stopping
                int numEpochs = 100;
                double bestAccuracy = 0.0;
                int patienceCounter = 0;
                int patience = 10;
                
                for (int epoch = 0; epoch < numEpochs; epoch++) {
                    model.fit(trainData);
                    trainData.reset();
                    
                    if ((epoch + 1) % 5 == 0) {
                        double accuracy = evaluateModel(model, testData);
                        testData.reset();
                        
                        System.out.printf("Epoch %d - Accuracy: %.4f%n", epoch + 1, accuracy);
                        
                        if (accuracy > bestAccuracy) {
                            bestAccuracy = accuracy;
                            patienceCounter = 0;
                        } else {
                            patienceCounter++;
                            if (patienceCounter >= patience) {
                                System.out.println("Early stopping triggered");
                                break;
                            }
                        }
                    }
                }
                
                System.out.printf("Fraud detection model trained with accuracy: %.3f%n", bestAccuracy);
                
                // Create model wrapper
                FraudDetectionModel fraudModel = new FraudDetectionModel(model, bestAccuracy);
                
                // Register model
                MLModelRegistry.ModelMetadata metadata = new MLModelRegistry.ModelMetadata(
                    "Fraud Detection Neural Network",
                    "1.0",
                    "Deep neural network for transaction fraud detection",
                    "CLASSIFICATION",
                    List.of("transaction_amount", "merchant_category", "time_features", "user_features"),
                    List.of("fraud", "legitimate"),
                    LocalDateTime.now(),
                    Map.of(
                        "best_accuracy", bestAccuracy,
                        "early_stopping", true,
                        "patience", patience
                    )
                );
                
                modelRegistry.registerModel("fraud_detection", fraudModel, metadata);
                
                return fraudModel;
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to train fraud detection model", e);
            }
        });
    }
    
    private double evaluateModel(MultiLayerNetwork model, DataSetIterator testData) {
        int correct = 0;
        int total = 0;
        
        while (testData.hasNext()) {
            DataSet batch = testData.next();
            INDArray predictions = model.output(batch.getFeatures());
            INDArray labels = batch.getLabels();
            
            for (int i = 0; i < predictions.rows(); i++) {
                int predicted = Nd4j.argMax(predictions.getRow(i), 1).getInt(0);
                int actual = Nd4j.argMax(labels.getRow(i), 1).getInt(0);
                
                if (predicted == actual) {
                    correct++;
                }
                total++;
            }
        }
        
        return (double) correct / total;
    }
    
    // Model implementations
    public static class SentimentAnalysisModel implements MLModelRegistry.MLModel {
        private final MultiLayerNetwork model;
        private final double accuracy;
        
        public SentimentAnalysisModel(MultiLayerNetwork model, double accuracy) {
            this.model = model;
            this.accuracy = accuracy;
        }
        
        @Override
        public MLModelRegistry.PredictionResult predict(MLModelRegistry.ModelInput input) {
            long startTime = System.currentTimeMillis();
            
            try {
                // Convert input to INDArray
                INDArray features = convertToINDArray(input);
                
                // Make prediction
                INDArray output = model.output(features);
                
                // Get predicted class
                int predictedClass = Nd4j.argMax(output, 1).getInt(0);
                String[] classes = {"negative", "neutral", "positive"};
                String prediction = classes[predictedClass];
                
                // Get probabilities
                double[] probs = output.toDoubleVector();
                Map<String, Double> probabilities = Map.of(
                    "negative", probs[0],
                    "neutral", probs[1],
                    "positive", probs[2]
                );
                
                double confidence = probs[predictedClass];
                long inferenceTime = System.currentTimeMillis() - startTime;
                
                return new MLModelRegistry.PredictionResult(
                    prediction,
                    probabilities,
                    confidence,
                    inferenceTime
                );
                
            } catch (Exception e) {
                throw new RuntimeException("Sentiment prediction failed", e);
            }
        }
        
        private INDArray convertToINDArray(MLModelRegistry.ModelInput input) {
            // Convert text embeddings from input to INDArray
            @SuppressWarnings("unchecked")
            List<Double> embeddings = (List<Double>) input.features().get("text_embeddings");
            
            double[] array = embeddings.stream().mapToDouble(Double::doubleValue).toArray();
            return Nd4j.create(array).reshape(1, array.length);
        }
        
        @Override
        public MLModelRegistry.BatchPredictionResult batchPredict(List<MLModelRegistry.ModelInput> inputs) {
            long startTime = System.currentTimeMillis();
            
            List<MLModelRegistry.PredictionResult> results = inputs.parallelStream()
                .map(this::predict)
                .toList();
            
            long totalTime = System.currentTimeMillis() - startTime;
            
            // Calculate statistics
            double avgConfidence = results.stream()
                .mapToDouble(MLModelRegistry.PredictionResult::confidence)
                .average()
                .orElse(0.0);
            
            double minConfidence = results.stream()
                .mapToDouble(MLModelRegistry.PredictionResult::confidence)
                .min()
                .orElse(0.0);
            
            double maxConfidence = results.stream()
                .mapToDouble(MLModelRegistry.PredictionResult::confidence)
                .max()
                .orElse(0.0);
            
            Map<String, Integer> predictionCounts = results.stream()
                .collect(Collectors.groupingBy(
                    r -> r.prediction().toString(),
                    Collectors.summingInt(r -> 1)
                ));
            
            MLModelRegistry.ModelStatistics stats = new MLModelRegistry.ModelStatistics(
                avgConfidence, minConfidence, maxConfidence, predictionCounts
            );
            
            return new MLModelRegistry.BatchPredictionResult(results, totalTime, stats);
        }
        
        @Override
        public MLModelRegistry.ModelMetadata getMetadata() {
            return new MLModelRegistry.ModelMetadata(
                "Sentiment Analysis Neural Network",
                "1.0",
                "Deep neural network for sentiment analysis",
                "CLASSIFICATION",
                List.of("text_embeddings"),
                List.of("positive", "negative", "neutral"),
                LocalDateTime.now(),
                Map.of("accuracy", accuracy, "model_type", "deep_learning")
            );
        }
        
        @Override
        public boolean isLoaded() {
            return model != null;
        }
        
        @Override
        public void reload() throws Exception {
            // Model is already in memory
        }
        
        @Override
        public void close() throws Exception {
            // DL4J models don't require explicit cleanup
        }
    }
    
    // Similar implementation for FraudDetectionModel
    public static class FraudDetectionModel implements MLModelRegistry.MLModel {
        private final MultiLayerNetwork model;
        private final double accuracy;
        
        public FraudDetectionModel(MultiLayerNetwork model, double accuracy) {
            this.model = model;
            this.accuracy = accuracy;
        }
        
        // Implement required methods similar to SentimentAnalysisModel
        @Override
        public MLModelRegistry.PredictionResult predict(MLModelRegistry.ModelInput input) {
            // Implementation for fraud detection
            return null;
        }
        
        @Override
        public MLModelRegistry.BatchPredictionResult batchPredict(List<MLModelRegistry.ModelInput> inputs) {
            return null;
        }
        
        @Override
        public MLModelRegistry.ModelMetadata getMetadata() {
            return null;
        }
        
        @Override
        public boolean isLoaded() {
            return model != null;
        }
        
        @Override
        public void reload() throws Exception {}
        
        @Override
        public void close() throws Exception {}
    }
}
```

## 4. Model Serving and Real-time Inference

### REST API for Model Serving

```java
package com.javajourney.ml.serving;

import com.javajourney.ml.MLModelRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/ml")
public class ModelServingController {
    
    private final ModelInferenceService inferenceService;
    private final MLModelRegistry modelRegistry;
    
    public ModelServingController(ModelInferenceService inferenceService, 
                                MLModelRegistry modelRegistry) {
        this.inferenceService = inferenceService;
        this.modelRegistry = modelRegistry;
    }
    
    @GetMapping("/models")
    public ResponseEntity<ModelListResponse> listModels() {
        var models = modelRegistry.getAvailableModels().stream()
            .map(modelId -> {
                var metadata = modelRegistry.getModelMetadata(modelId).orElse(null);
                return new ModelInfo(modelId, metadata);
            })
            .toList();
        
        return ResponseEntity.ok(new ModelListResponse(models));
    }
    
    @GetMapping("/models/{modelId}")
    public ResponseEntity<ModelInfo> getModelInfo(@PathVariable String modelId) {
        var metadata = modelRegistry.getModelMetadata(modelId);
        if (metadata.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(new ModelInfo(modelId, metadata.get()));
    }
    
    @PostMapping("/models/{modelId}/predict")
    public CompletableFuture<ResponseEntity<PredictionResponse>> predict(
            @PathVariable String modelId,
            @RequestBody PredictionRequest request) {
        
        return inferenceService.predict(modelId, request.input())
            .thenApply(result -> ResponseEntity.ok(new PredictionResponse(
                modelId,
                result.prediction(),
                result.probabilities(),
                result.confidence(),
                result.inferenceTimeMs(),
                System.currentTimeMillis()
            )))
            .exceptionally(throwable -> ResponseEntity.badRequest()
                .body(new PredictionResponse(
                    modelId,
                    null,
                    Map.of(),
                    0.0,
                    0L,
                    System.currentTimeMillis()
                )));
    }
    
    @PostMapping("/models/{modelId}/batch-predict")
    public CompletableFuture<ResponseEntity<BatchPredictionResponse>> batchPredict(
            @PathVariable String modelId,
            @RequestBody BatchPredictionRequest request) {
        
        return inferenceService.batchPredict(modelId, request.inputs())
            .thenApply(result -> ResponseEntity.ok(new BatchPredictionResponse(
                modelId,
                result.results(),
                result.totalInferenceTimeMs(),
                result.statistics(),
                System.currentTimeMillis()
            )))
            .exceptionally(throwable -> ResponseEntity.badRequest().build());
    }
    
    @PostMapping("/models/{modelId}/explain")
    public CompletableFuture<ResponseEntity<ExplanationResponse>> explainPrediction(
            @PathVariable String modelId,
            @RequestBody PredictionRequest request) {
        
        return inferenceService.explainPrediction(modelId, request.input())
            .thenApply(explanation -> ResponseEntity.ok(new ExplanationResponse(
                modelId,
                explanation.prediction(),
                explanation.featureImportances(),
                explanation.explanation(),
                System.currentTimeMillis()
            )))
            .exceptionally(throwable -> ResponseEntity.badRequest().build());
    }
    
    // Business-specific endpoints
    @PostMapping("/churn/predict")
    public CompletableFuture<ResponseEntity<ChurnPredictionResponse>> predictChurn(
            @RequestBody CustomerData customerData) {
        
        var input = new MLModelRegistry.ModelInput(Map.of(
            "tenure", customerData.tenure(),
            "monthly_charges", customerData.monthlyCharges(),
            "total_charges", customerData.totalCharges(),
            "contract_type", customerData.contractType(),
            "payment_method", customerData.paymentMethod()
        ));
        
        return inferenceService.predict("churn_prediction", input)
            .thenApply(result -> {
                boolean willChurn = "churn".equals(result.prediction());
                double churnProbability = result.probabilities().getOrDefault("churn", 0.0);
                
                String riskLevel = getRiskLevel(churnProbability);
                List<String> recommendations = getChurnRecommendations(willChurn, customerData);
                
                return ResponseEntity.ok(new ChurnPredictionResponse(
                    customerData.customerId(),
                    willChurn,
                    churnProbability,
                    riskLevel,
                    recommendations,
                    result.confidence(),
                    System.currentTimeMillis()
                ));
            });
    }
    
    @PostMapping("/fraud/detect")
    public CompletableFuture<ResponseEntity<FraudDetectionResponse>> detectFraud(
            @RequestBody TransactionData transactionData) {
        
        var input = new MLModelRegistry.ModelInput(Map.of(
            "amount", transactionData.amount(),
            "merchant_category", transactionData.merchantCategory(),
            "time_of_day", transactionData.timeOfDay(),
            "day_of_week", transactionData.dayOfWeek(),
            "user_history_features", transactionData.userHistoryFeatures()
        ));
        
        return inferenceService.predict("fraud_detection", input)
            .thenApply(result -> {
                boolean isFraud = "fraud".equals(result.prediction());
                double fraudProbability = result.probabilities().getOrDefault("fraud", 0.0);
                
                String riskScore = getFraudRiskScore(fraudProbability);
                String action = getRecommendedAction(isFraud, fraudProbability);
                
                return ResponseEntity.ok(new FraudDetectionResponse(
                    transactionData.transactionId(),
                    isFraud,
                    fraudProbability,
                    riskScore,
                    action,
                    result.confidence(),
                    System.currentTimeMillis()
                ));
            });
    }
    
    @PostMapping("/sentiment/analyze")
    public CompletableFuture<ResponseEntity<SentimentAnalysisResponse>> analyzeSentiment(
            @RequestBody TextData textData) {
        
        // In a real implementation, you would preprocess text to embeddings
        var input = new MLModelRegistry.ModelInput(Map.of(
            "text_embeddings", preprocessText(textData.text())
        ));
        
        return inferenceService.predict("sentiment_analysis", input)
            .thenApply(result -> {
                String sentiment = result.prediction().toString();
                Map<String, Double> sentimentScores = result.probabilities();
                
                return ResponseEntity.ok(new SentimentAnalysisResponse(
                    textData.text(),
                    sentiment,
                    sentimentScores,
                    result.confidence(),
                    System.currentTimeMillis()
                ));
            });
    }
    
    // Helper methods
    private String getRiskLevel(double churnProbability) {
        if (churnProbability > 0.8) return "HIGH";
        if (churnProbability > 0.5) return "MEDIUM";
        return "LOW";
    }
    
    private List<String> getChurnRecommendations(boolean willChurn, CustomerData customerData) {
        if (!willChurn) {
            return List.of("Customer is likely to stay", "Continue current engagement strategy");
        }
        
        List<String> recommendations = new ArrayList<>();
        recommendations.add("Immediate intervention required");
        
        if (customerData.contractType().equals("month-to-month")) {
            recommendations.add("Offer long-term contract incentives");
        }
        
        if (customerData.monthlyCharges() > 70) {
            recommendations.add("Consider discount or service bundle");
        }
        
        recommendations.add("Schedule retention call within 24 hours");
        
        return recommendations;
    }
    
    private String getFraudRiskScore(double fraudProbability) {
        if (fraudProbability > 0.9) return "CRITICAL";
        if (fraudProbability > 0.7) return "HIGH";
        if (fraudProbability > 0.5) return "MEDIUM";
        return "LOW";
    }
    
    private String getRecommendedAction(boolean isFraud, double fraudProbability) {
        if (isFraud && fraudProbability > 0.9) {
            return "BLOCK_TRANSACTION";
        } else if (isFraud && fraudProbability > 0.7) {
            return "REQUIRE_ADDITIONAL_VERIFICATION";
        } else if (fraudProbability > 0.5) {
            return "FLAG_FOR_REVIEW";
        } else {
            return "APPROVE";
        }
    }
    
    private List<Double> preprocessText(String text) {
        // Simplified text preprocessing - in reality, use word embeddings
        // This would involve tokenization, embedding lookup, etc.
        return java.util.stream.IntStream.range(0, 1000)
            .mapToObj(i -> Math.random())
            .toList();
    }
    
    // Data records for API
    public record ModelListResponse(List<ModelInfo> models) {}
    
    public record ModelInfo(String id, MLModelRegistry.ModelMetadata metadata) {}
    
    public record PredictionRequest(MLModelRegistry.ModelInput input) {}
    
    public record PredictionResponse(
        String modelId,
        Object prediction,
        Map<String, Double> probabilities,
        double confidence,
        long inferenceTimeMs,
        long timestamp
    ) {}
    
    public record BatchPredictionRequest(List<MLModelRegistry.ModelInput> inputs) {}
    
    public record BatchPredictionResponse(
        String modelId,
        List<MLModelRegistry.PredictionResult> results,
        long totalInferenceTimeMs,
        MLModelRegistry.ModelStatistics statistics,
        long timestamp
    ) {}
    
    public record ExplanationResponse(
        String modelId,
        Object prediction,
        Map<String, Double> featureImportances,
        String explanation,
        long timestamp
    ) {}
    
    // Business-specific data records
    public record CustomerData(
        String customerId,
        int tenure,
        double monthlyCharges,
        double totalCharges,
        String contractType,
        String paymentMethod
    ) {}
    
    public record ChurnPredictionResponse(
        String customerId,
        boolean willChurn,
        double churnProbability,
        String riskLevel,
        List<String> recommendations,
        double confidence,
        long timestamp
    ) {}
    
    public record TransactionData(
        String transactionId,
        double amount,
        String merchantCategory,
        int timeOfDay,
        int dayOfWeek,
        Map<String, Double> userHistoryFeatures
    ) {}
    
    public record FraudDetectionResponse(
        String transactionId,
        boolean isFraud,
        double fraudProbability,
        String riskScore,
        String recommendedAction,
        double confidence,
        long timestamp
    ) {}
    
    public record TextData(String text) {}
    
    public record SentimentAnalysisResponse(
        String text,
        String sentiment,
        Map<String, Double> sentimentScores,
        double confidence,
        long timestamp
    ) {}
}
```

### Model Inference Service

```java
package com.javajourney.ml.serving;

import com.javajourney.ml.MLModelRegistry;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ModelInferenceService {
    
    private final MLModelRegistry modelRegistry;
    private final Map<String, InferenceMetrics> inferenceMetrics = new ConcurrentHashMap<>();
    
    public ModelInferenceService(MLModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry;
    }
    
    @Async("mlExecutor")
    public CompletableFuture<MLModelRegistry.PredictionResult> predict(String modelId, MLModelRegistry.ModelInput input) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                MLModelRegistry.MLModel model = modelRegistry.getModel(modelId)
                    .orElseThrow(() -> new RuntimeException("Model not found: " + modelId));
                
                if (!model.isLoaded()) {
                    throw new RuntimeException("Model is not loaded: " + modelId);
                }
                
                MLModelRegistry.PredictionResult result = model.predict(input);
                
                // Update metrics
                updateMetrics(modelId, result.inferenceTimeMs(), true);
                
                return result;
                
            } catch (Exception e) {
                long inferenceTime = System.currentTimeMillis() - startTime;
                updateMetrics(modelId, inferenceTime, false);
                throw new RuntimeException("Prediction failed for model " + modelId, e);
            }
        });
    }
    
    @Async("mlExecutor") 
    public CompletableFuture<MLModelRegistry.BatchPredictionResult> batchPredict(String modelId, List<MLModelRegistry.ModelInput> inputs) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                MLModelRegistry.MLModel model = modelRegistry.getModel(modelId)
                    .orElseThrow(() -> new RuntimeException("Model not found: " + modelId));
                
                if (!model.isLoaded()) {
                    throw new RuntimeException("Model is not loaded: " + modelId);
                }
                
                MLModelRegistry.BatchPredictionResult result = model.batchPredict(inputs);
                
                // Update metrics for batch
                updateBatchMetrics(modelId, result.totalInferenceTimeMs(), inputs.size(), true);
                
                return result;
                
            } catch (Exception e) {
                long inferenceTime = System.currentTimeMillis() - startTime;
                updateBatchMetrics(modelId, inferenceTime, inputs.size(), false);
                throw new RuntimeException("Batch prediction failed for model " + modelId, e);
            }
        });
    }
    
    @Async("mlExecutor")
    public CompletableFuture<ModelExplanation> explainPrediction(String modelId, MLModelRegistry.ModelInput input) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get prediction first
                MLModelRegistry.PredictionResult result = predict(modelId, input).get();
                
                // Generate explanation (simplified)
                Map<String, Double> featureImportances = calculateFeatureImportances(modelId, input);
                String explanation = generateExplanation(result, featureImportances);
                
                return new ModelExplanation(
                    result.prediction(),
                    featureImportances,
                    explanation
                );
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to explain prediction for model " + modelId, e);
            }
        });
    }
    
    // Health check for models
    public Map<String, ModelHealth> checkModelHealth() {
        Map<String, ModelHealth> healthStatus = new HashMap<>();
        
        for (String modelId : modelRegistry.getAvailableModels()) {
            try {
                MLModelRegistry.MLModel model = modelRegistry.getModel(modelId).orElse(null);
                InferenceMetrics metrics = inferenceMetrics.get(modelId);
                
                boolean isHealthy = model != null && model.isLoaded();
                String status = isHealthy ? "HEALTHY" : "UNHEALTHY";
                
                long totalRequests = metrics != null ? metrics.totalRequests.get() : 0;
                long errorCount = metrics != null ? metrics.errorCount.get() : 0;
                double errorRate = totalRequests > 0 ? (double) errorCount / totalRequests : 0.0;
                
                healthStatus.put(modelId, new ModelHealth(
                    modelId,
                    status,
                    isHealthy,
                    totalRequests,
                    errorCount,
                    errorRate,
                    metrics != null ? metrics.getAverageInferenceTime() : 0.0
                ));
                
            } catch (Exception e) {
                healthStatus.put(modelId, new ModelHealth(
                    modelId,
                    "ERROR",
                    false,
                    0L,
                    1L,
                    1.0,
                    0.0
                ));
            }
        }
        
        return healthStatus;
    }
    
    // Model warming for better performance
    public void warmUpModel(String modelId, int warmupRequests) {
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("Warming up model: " + modelId);
                
                MLModelRegistry.MLModel model = modelRegistry.getModel(modelId)
                    .orElseThrow(() -> new RuntimeException("Model not found: " + modelId));
                
                // Generate dummy data for warmup
                List<MLModelRegistry.ModelInput> dummyInputs = generateDummyInputs(modelId, warmupRequests);
                
                for (MLModelRegistry.ModelInput input : dummyInputs) {
                    try {
                        model.predict(input);
                    } catch (Exception e) {
                        // Ignore warmup errors
                    }
                }
                
                System.out.println("Model warmup completed: " + modelId);
                
            } catch (Exception e) {
                System.err.println("Model warmup failed for " + modelId + ": " + e.getMessage());
            }
        });
    }
    
    private void updateMetrics(String modelId, long inferenceTimeMs, boolean success) {
        InferenceMetrics metrics = inferenceMetrics.computeIfAbsent(modelId, k -> new InferenceMetrics());
        
        metrics.totalRequests.incrementAndGet();
        metrics.totalInferenceTime.addAndGet(inferenceTimeMs);
        
        if (!success) {
            metrics.errorCount.incrementAndGet();
        }
    }
    
    private void updateBatchMetrics(String modelId, long totalInferenceTimeMs, int batchSize, boolean success) {
        InferenceMetrics metrics = inferenceMetrics.computeIfAbsent(modelId, k -> new InferenceMetrics());
        
        metrics.totalRequests.addAndGet(batchSize);
        metrics.totalInferenceTime.addAndGet(totalInferenceTimeMs);
        
        if (!success) {
            metrics.errorCount.addAndGet(batchSize);
        }
    }
    
    private Map<String, Double> calculateFeatureImportances(String modelId, MLModelRegistry.ModelInput input) {
        // Simplified feature importance calculation
        // In reality, you would use techniques like SHAP, LIME, or permutation importance
        Map<String, Double> importances = new HashMap<>();
        
        for (String feature : input.features().keySet()) {
            importances.put(feature, Math.random()); // Random importance for demo
        }
        
        return importances;
    }
    
    private String generateExplanation(MLModelRegistry.PredictionResult result, Map<String, Double> featureImportances) {
        StringBuilder explanation = new StringBuilder();
        explanation.append("The model predicted: ").append(result.prediction())
                  .append(" with confidence: ").append(String.format("%.2f", result.confidence()));
        
        explanation.append("\n\nKey factors:");
        featureImportances.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(3)
            .forEach(entry -> explanation.append("\n- ")
                .append(entry.getKey())
                .append(": ")
                .append(String.format("%.3f", entry.getValue())));
        
        return explanation.toString();
    }
    
    private List<MLModelRegistry.ModelInput> generateDummyInputs(String modelId, int count) {
        // Generate dummy inputs based on model metadata
        MLModelRegistry.ModelMetadata metadata = modelRegistry.getModelMetadata(modelId).orElse(null);
        if (metadata == null) {
            return List.of();
        }
        
        List<MLModelRegistry.ModelInput> inputs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> features = new HashMap<>();
            
            for (String feature : metadata.inputFeatures()) {
                // Generate random values based on feature name
                if (feature.contains("amount") || feature.contains("price") || feature.contains("charges")) {
                    features.put(feature, Math.random() * 1000);
                } else if (feature.contains("count") || feature.contains("tenure")) {
                    features.put(feature, (int) (Math.random() * 100));
                } else {
                    features.put(feature, Math.random());
                }
            }
            
            inputs.add(new MLModelRegistry.ModelInput(features));
        }
        
        return inputs;
    }
    
    // Data classes
    private static class InferenceMetrics {
        final AtomicLong totalRequests = new AtomicLong(0);
        final AtomicLong totalInferenceTime = new AtomicLong(0);
        final AtomicLong errorCount = new AtomicLong(0);
        
        double getAverageInferenceTime() {
            long requests = totalRequests.get();
            return requests > 0 ? (double) totalInferenceTime.get() / requests : 0.0;
        }
    }
    
    public record ModelExplanation(
        Object prediction,
        Map<String, Double> featureImportances,
        String explanation
    ) {}
    
    public record ModelHealth(
        String modelId,
        String status,
        boolean isHealthy,
        long totalRequests,
        long errorCount,
        double errorRate,
        double averageInferenceTimeMs
    ) {}
}
```

## Practice Exercises

### Exercise 1: Complete ML Pipeline

Build an end-to-end ML pipeline from data preprocessing to model deployment with monitoring.

### Exercise 2: Real-time Recommendation System

Implement a real-time recommendation system using collaborative filtering and content-based approaches.

### Exercise 3: Time Series Forecasting

Create a time series forecasting system for business metrics using LSTM or ARIMA models.

### Exercise 4: Model A/B Testing

Implement A/B testing framework for comparing different ML models in production.

## Key Takeaways

1. **Ecosystem Integration**: Java provides rich ML libraries for different use cases and complexity levels
2. **Production Ready**: Focus on model serving, monitoring, and reliability for enterprise deployment
3. **Performance Optimization**: Use async processing, batching, and model warming for better performance
4. **Business Integration**: Integrate ML seamlessly into business workflows with domain-specific APIs
5. **Scalability**: Design ML systems to handle high throughput and multiple models simultaneously

Machine learning integration in Java enables building intelligent enterprise applications that can make data-driven decisions and provide valuable insights to users.