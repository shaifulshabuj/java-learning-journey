# Day 109: Machine Learning Integration - ML Pipelines, Feature Engineering & Model Deployment

## Learning Objectives
- Integrate machine learning models with Java applications using modern frameworks
- Build automated ML pipelines with feature engineering and model training
- Deploy ML models at scale with real-time inference capabilities
- Implement MLOps practices for model versioning, monitoring, and continuous deployment
- Create feature stores and data pipelines for ML workflows
- Build real-time prediction services with high performance and low latency

## Part 1: ML Framework Integration with Java

### Deep Java Library (DJL) Integration

```java
package com.securetrading.ml.config;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Configuration
public class MLModelConfiguration {

    @Value("${ml.models.path:/models}")
    private String modelsPath;

    @Value("${ml.models.cache.size:10}")
    private int cacheSize;

    private final Map<String, ZooModel<?, ?>> modelCache = new ConcurrentHashMap<>();

    @Bean
    public MLModelManager modelManager() {
        return new MLModelManager(modelsPath, cacheSize);
    }

    @PreDestroy
    public void cleanup() {
        modelCache.values().forEach(model -> {
            try {
                model.close();
            } catch (Exception e) {
                // Log error but continue cleanup
            }
        });
    }

    public static class MLModelManager {
        private final String modelsPath;
        private final int cacheSize;
        private final Map<String, ZooModel<?, ?>> loadedModels = new ConcurrentHashMap<>();

        public MLModelManager(String modelsPath, int cacheSize) {
            this.modelsPath = modelsPath;
            this.cacheSize = cacheSize;
        }

        @SuppressWarnings("unchecked")
        public <I, O> ZooModel<I, O> loadModel(String modelName, Class<I> inputClass, Class<O> outputClass) 
                throws ModelNotFoundException, MalformedModelException, IOException {
            
            String cacheKey = modelName + "_" + inputClass.getSimpleName() + "_" + outputClass.getSimpleName();
            
            return (ZooModel<I, O>) loadedModels.computeIfAbsent(cacheKey, key -> {
                try {
                    Criteria<I, O> criteria = Criteria.builder()
                            .setTypes(inputClass, outputClass)
                            .optModelPath(Path.of(modelsPath, modelName))
                            .optProgress(new ProgressBar())
                            .build();
                    
                    return ModelZoo.loadModel(criteria);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to load model: " + modelName, e);
                }
            });
        }

        public void unloadModel(String modelName) {
            ZooModel<?, ?> model = loadedModels.remove(modelName);
            if (model != null) {
                model.close();
            }
        }

        public int getCacheSize() {
            return loadedModels.size();
        }
    }
}
```

### Financial ML Features Service

```java
package com.securetrading.ml.service;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FinancialFeatureService {

    private static final Logger logger = LoggerFactory.getLogger(FinancialFeatureService.class);

    @Autowired
    private MarketDataService marketDataService;

    @Autowired
    private TechnicalIndicatorService technicalIndicatorService;

    public FeatureVector extractFeatures(String symbol, LocalDateTime timestamp, int lookbackPeriod) {
        logger.debug("Extracting features for symbol: {} at timestamp: {}", symbol, timestamp);

        // Get historical market data
        List<MarketDataPoint> historicalData = marketDataService.getHistoricalData(
                symbol, timestamp.minus(lookbackPeriod, ChronoUnit.DAYS), timestamp);

        if (historicalData.isEmpty()) {
            throw new IllegalArgumentException("No historical data available for symbol: " + symbol);
        }

        // Extract price-based features
        PriceFeatures priceFeatures = extractPriceFeatures(historicalData);
        
        // Extract technical indicators
        TechnicalFeatures technicalFeatures = extractTechnicalFeatures(historicalData);
        
        // Extract volume features
        VolumeFeatures volumeFeatures = extractVolumeFeatures(historicalData);
        
        // Extract volatility features
        VolatilityFeatures volatilityFeatures = extractVolatilityFeatures(historicalData);
        
        // Extract momentum features
        MomentumFeatures momentumFeatures = extractMomentumFeatures(historicalData);

        return new FeatureVector(
                symbol,
                timestamp,
                priceFeatures,
                technicalFeatures,
                volumeFeatures,
                volatilityFeatures,
                momentumFeatures
        );
    }

    private PriceFeatures extractPriceFeatures(List<MarketDataPoint> data) {
        if (data.isEmpty()) {
            return new PriceFeatures();
        }

        List<Double> prices = data.stream()
                .map(MarketDataPoint::getClosePrice)
                .collect(Collectors.toList());

        double currentPrice = prices.get(prices.size() - 1);
        double minPrice = prices.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double maxPrice = prices.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        double avgPrice = prices.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        // Price ratios and normalized values
        double priceToMin = minPrice > 0 ? currentPrice / minPrice : 1.0;
        double priceToMax = maxPrice > 0 ? currentPrice / maxPrice : 1.0;
        double priceToAvg = avgPrice > 0 ? currentPrice / avgPrice : 1.0;

        // Price percentiles
        List<Double> sortedPrices = prices.stream().sorted().collect(Collectors.toList());
        double p25 = calculatePercentile(sortedPrices, 25);
        double p50 = calculatePercentile(sortedPrices, 50);
        double p75 = calculatePercentile(sortedPrices, 75);

        return new PriceFeatures(
                currentPrice, minPrice, maxPrice, avgPrice,
                priceToMin, priceToMax, priceToAvg,
                p25, p50, p75
        );
    }

    private TechnicalFeatures extractTechnicalFeatures(List<MarketDataPoint> data) {
        List<Double> prices = data.stream()
                .map(MarketDataPoint::getClosePrice)
                .collect(Collectors.toList());

        // Simple Moving Averages
        double sma5 = technicalIndicatorService.calculateSMA(prices, 5);
        double sma10 = technicalIndicatorService.calculateSMA(prices, 10);
        double sma20 = technicalIndicatorService.calculateSMA(prices, 20);
        double sma50 = technicalIndicatorService.calculateSMA(prices, 50);

        // Exponential Moving Averages
        double ema12 = technicalIndicatorService.calculateEMA(prices, 12);
        double ema26 = technicalIndicatorService.calculateEMA(prices, 26);

        // MACD
        double macd = ema12 - ema26;
        double macdSignal = technicalIndicatorService.calculateEMA(
                Collections.singletonList(macd), 9);
        double macdHistogram = macd - macdSignal;

        // RSI
        double rsi = technicalIndicatorService.calculateRSI(prices, 14);

        // Bollinger Bands
        BollingerBands bb = technicalIndicatorService.calculateBollingerBands(prices, 20, 2.0);

        // Price position relative to moving averages
        double currentPrice = prices.get(prices.size() - 1);
        double priceToSMA20 = sma20 > 0 ? currentPrice / sma20 : 1.0;
        double priceToSMA50 = sma50 > 0 ? currentPrice / sma50 : 1.0;

        return new TechnicalFeatures(
                sma5, sma10, sma20, sma50,
                ema12, ema26,
                macd, macdSignal, macdHistogram,
                rsi,
                bb.getMiddleBand(), bb.getUpperBand(), bb.getLowerBand(),
                priceToSMA20, priceToSMA50
        );
    }

    private VolumeFeatures extractVolumeFeatures(List<MarketDataPoint> data) {
        List<Long> volumes = data.stream()
                .map(MarketDataPoint::getVolume)
                .collect(Collectors.toList());

        if (volumes.isEmpty()) {
            return new VolumeFeatures();
        }

        long currentVolume = volumes.get(volumes.size() - 1);
        double avgVolume = volumes.stream().mapToDouble(Long::doubleValue).average().orElse(0.0);
        long maxVolume = volumes.stream().mapToLong(Long::longValue).max().orElse(0L);
        long minVolume = volumes.stream().mapToLong(Long::longValue).min().orElse(0L);

        // Volume ratios
        double volumeToAvg = avgVolume > 0 ? currentVolume / avgVolume : 1.0;
        double volumeToMax = maxVolume > 0 ? (double) currentVolume / maxVolume : 1.0;

        // Volume moving averages
        double volumeSMA5 = volumes.size() >= 5 ? 
                volumes.subList(volumes.size() - 5, volumes.size()).stream()
                        .mapToDouble(Long::doubleValue).average().orElse(0.0) : 0.0;
        double volumeSMA10 = volumes.size() >= 10 ? 
                volumes.subList(volumes.size() - 10, volumes.size()).stream()
                        .mapToDouble(Long::doubleValue).average().orElse(0.0) : 0.0;

        return new VolumeFeatures(
                currentVolume, avgVolume, maxVolume, minVolume,
                volumeToAvg, volumeToMax,
                volumeSMA5, volumeSMA10
        );
    }

    private VolatilityFeatures extractVolatilityFeatures(List<MarketDataPoint> data) {
        if (data.size() < 2) {
            return new VolatilityFeatures();
        }

        List<Double> returns = calculateReturns(data);
        
        // Historical volatility (standard deviation of returns)
        double volatility = calculateStandardDeviation(returns);
        
        // Volatility over different periods
        double volatility5d = data.size() >= 5 ? 
                calculateStandardDeviation(returns.subList(returns.size() - 5, returns.size())) : 0.0;
        double volatility10d = data.size() >= 10 ? 
                calculateStandardDeviation(returns.subList(returns.size() - 10, returns.size())) : 0.0;
        double volatility20d = data.size() >= 20 ? 
                calculateStandardDeviation(returns.subList(returns.size() - 20, returns.size())) : 0.0;

        // High-Low volatility
        double avgHighLowRatio = data.stream()
                .mapToDouble(point -> {
                    double low = point.getLowPrice();
                    return low > 0 ? (point.getHighPrice() - low) / low : 0.0;
                })
                .average()
                .orElse(0.0);

        return new VolatilityFeatures(
                volatility, volatility5d, volatility10d, volatility20d,
                avgHighLowRatio
        );
    }

    private MomentumFeatures extractMomentumFeatures(List<MarketDataPoint> data) {
        if (data.size() < 2) {
            return new MomentumFeatures();
        }

        List<Double> prices = data.stream()
                .map(MarketDataPoint::getClosePrice)
                .collect(Collectors.toList());

        // Price momentum over different periods
        double momentum1d = calculatePriceMomentum(prices, 1);
        double momentum5d = calculatePriceMomentum(prices, 5);
        double momentum10d = calculatePriceMomentum(prices, 10);
        double momentum20d = calculatePriceMomentum(prices, 20);

        // Rate of change
        double roc5 = calculateRateOfChange(prices, 5);
        double roc10 = calculateRateOfChange(prices, 10);

        // Momentum oscillator
        double momentumOscillator = technicalIndicatorService.calculateMomentumOscillator(prices, 14);

        return new MomentumFeatures(
                momentum1d, momentum5d, momentum10d, momentum20d,
                roc5, roc10, momentumOscillator
        );
    }

    public NDArray featureVectorToNDArray(FeatureVector featureVector, NDManager manager) {
        List<Double> features = new ArrayList<>();

        // Price features
        PriceFeatures priceFeatures = featureVector.getPriceFeatures();
        features.addAll(Arrays.asList(
                priceFeatures.getCurrentPrice(),
                priceFeatures.getPriceToMin(),
                priceFeatures.getPriceToMax(),
                priceFeatures.getPriceToAvg(),
                priceFeatures.getP25(),
                priceFeatures.getP50(),
                priceFeatures.getP75()
        ));

        // Technical features
        TechnicalFeatures technicalFeatures = featureVector.getTechnicalFeatures();
        features.addAll(Arrays.asList(
                technicalFeatures.getSma20(),
                technicalFeatures.getSma50(),
                technicalFeatures.getRsi(),
                technicalFeatures.getMacd(),
                technicalFeatures.getMacdHistogram(),
                technicalFeatures.getPriceToSMA20(),
                technicalFeatures.getPriceToSMA50()
        ));

        // Volume features
        VolumeFeatures volumeFeatures = featureVector.getVolumeFeatures();
        features.addAll(Arrays.asList(
                (double) volumeFeatures.getCurrentVolume(),
                volumeFeatures.getVolumeToAvg(),
                volumeFeatures.getVolumeToMax()
        ));

        // Volatility features
        VolatilityFeatures volatilityFeatures = featureVector.getVolatilityFeatures();
        features.addAll(Arrays.asList(
                volatilityFeatures.getVolatility(),
                volatilityFeatures.getVolatility5d(),
                volatilityFeatures.getVolatility20d()
        ));

        // Momentum features
        MomentumFeatures momentumFeatures = featureVector.getMomentumFeatures();
        features.addAll(Arrays.asList(
                momentumFeatures.getMomentum5d(),
                momentumFeatures.getMomentum10d(),
                momentumFeatures.getRoc5(),
                momentumFeatures.getMomentumOscillator()
        ));

        // Convert to NDArray
        float[] featureArray = features.stream()
                .map(Double::floatValue)
                .collect(Collectors.toList())
                .toArray(new Float[0]);

        return manager.create(featureArray, new Shape(1, featureArray.length));
    }

    // Helper methods
    private List<Double> calculateReturns(List<MarketDataPoint> data) {
        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < data.size(); i++) {
            double previousPrice = data.get(i - 1).getClosePrice();
            double currentPrice = data.get(i).getClosePrice();
            if (previousPrice > 0) {
                returns.add((currentPrice - previousPrice) / previousPrice);
            }
        }
        return returns;
    }

    private double calculateStandardDeviation(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }

    private double calculatePercentile(List<Double> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) return 0.0;
        
        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }

    private double calculatePriceMomentum(List<Double> prices, int period) {
        if (prices.size() <= period) return 0.0;
        
        double currentPrice = prices.get(prices.size() - 1);
        double pastPrice = prices.get(prices.size() - 1 - period);
        
        return pastPrice > 0 ? (currentPrice - pastPrice) / pastPrice : 0.0;
    }

    private double calculateRateOfChange(List<Double> prices, int period) {
        return calculatePriceMomentum(prices, period) * 100;
    }

    // Data classes
    public static class FeatureVector {
        private final String symbol;
        private final LocalDateTime timestamp;
        private final PriceFeatures priceFeatures;
        private final TechnicalFeatures technicalFeatures;
        private final VolumeFeatures volumeFeatures;
        private final VolatilityFeatures volatilityFeatures;
        private final MomentumFeatures momentumFeatures;

        public FeatureVector(String symbol, LocalDateTime timestamp,
                PriceFeatures priceFeatures, TechnicalFeatures technicalFeatures,
                VolumeFeatures volumeFeatures, VolatilityFeatures volatilityFeatures,
                MomentumFeatures momentumFeatures) {
            this.symbol = symbol;
            this.timestamp = timestamp;
            this.priceFeatures = priceFeatures;
            this.technicalFeatures = technicalFeatures;
            this.volumeFeatures = volumeFeatures;
            this.volatilityFeatures = volatilityFeatures;
            this.momentumFeatures = momentumFeatures;
        }

        // Getters
        public String getSymbol() { return symbol; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public PriceFeatures getPriceFeatures() { return priceFeatures; }
        public TechnicalFeatures getTechnicalFeatures() { return technicalFeatures; }
        public VolumeFeatures getVolumeFeatures() { return volumeFeatures; }
        public VolatilityFeatures getVolatilityFeatures() { return volatilityFeatures; }
        public MomentumFeatures getMomentumFeatures() { return momentumFeatures; }
    }

    public static class PriceFeatures {
        private final double currentPrice;
        private final double minPrice;
        private final double maxPrice;
        private final double avgPrice;
        private final double priceToMin;
        private final double priceToMax;
        private final double priceToAvg;
        private final double p25;
        private final double p50;
        private final double p75;

        public PriceFeatures() {
            this(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }

        public PriceFeatures(double currentPrice, double minPrice, double maxPrice, double avgPrice,
                double priceToMin, double priceToMax, double priceToAvg,
                double p25, double p50, double p75) {
            this.currentPrice = currentPrice;
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
            this.avgPrice = avgPrice;
            this.priceToMin = priceToMin;
            this.priceToMax = priceToMax;
            this.priceToAvg = priceToAvg;
            this.p25 = p25;
            this.p50 = p50;
            this.p75 = p75;
        }

        // Getters
        public double getCurrentPrice() { return currentPrice; }
        public double getMinPrice() { return minPrice; }
        public double getMaxPrice() { return maxPrice; }
        public double getAvgPrice() { return avgPrice; }
        public double getPriceToMin() { return priceToMin; }
        public double getPriceToMax() { return priceToMax; }
        public double getPriceToAvg() { return priceToAvg; }
        public double getP25() { return p25; }
        public double getP50() { return p50; }
        public double getP75() { return p75; }
    }

    public static class TechnicalFeatures {
        private final double sma5;
        private final double sma10;
        private final double sma20;
        private final double sma50;
        private final double ema12;
        private final double ema26;
        private final double macd;
        private final double macdSignal;
        private final double macdHistogram;
        private final double rsi;
        private final double bollMiddle;
        private final double bollUpper;
        private final double bollLower;
        private final double priceToSMA20;
        private final double priceToSMA50;

        public TechnicalFeatures(double sma5, double sma10, double sma20, double sma50,
                double ema12, double ema26, double macd, double macdSignal, double macdHistogram,
                double rsi, double bollMiddle, double bollUpper, double bollLower,
                double priceToSMA20, double priceToSMA50) {
            this.sma5 = sma5;
            this.sma10 = sma10;
            this.sma20 = sma20;
            this.sma50 = sma50;
            this.ema12 = ema12;
            this.ema26 = ema26;
            this.macd = macd;
            this.macdSignal = macdSignal;
            this.macdHistogram = macdHistogram;
            this.rsi = rsi;
            this.bollMiddle = bollMiddle;
            this.bollUpper = bollUpper;
            this.bollLower = bollLower;
            this.priceToSMA20 = priceToSMA20;
            this.priceToSMA50 = priceToSMA50;
        }

        // Getters
        public double getSma5() { return sma5; }
        public double getSma10() { return sma10; }
        public double getSma20() { return sma20; }
        public double getSma50() { return sma50; }
        public double getEma12() { return ema12; }
        public double getEma26() { return ema26; }
        public double getMacd() { return macd; }
        public double getMacdSignal() { return macdSignal; }
        public double getMacdHistogram() { return macdHistogram; }
        public double getRsi() { return rsi; }
        public double getBollMiddle() { return bollMiddle; }
        public double getBollUpper() { return bollUpper; }
        public double getBollLower() { return bollLower; }
        public double getPriceToSMA20() { return priceToSMA20; }
        public double getPriceToSMA50() { return priceToSMA50; }
    }

    public static class VolumeFeatures {
        private final long currentVolume;
        private final double avgVolume;
        private final long maxVolume;
        private final long minVolume;
        private final double volumeToAvg;
        private final double volumeToMax;
        private final double volumeSMA5;
        private final double volumeSMA10;

        public VolumeFeatures() {
            this(0L, 0.0, 0L, 0L, 0.0, 0.0, 0.0, 0.0);
        }

        public VolumeFeatures(long currentVolume, double avgVolume, long maxVolume, long minVolume,
                double volumeToAvg, double volumeToMax, double volumeSMA5, double volumeSMA10) {
            this.currentVolume = currentVolume;
            this.avgVolume = avgVolume;
            this.maxVolume = maxVolume;
            this.minVolume = minVolume;
            this.volumeToAvg = volumeToAvg;
            this.volumeToMax = volumeToMax;
            this.volumeSMA5 = volumeSMA5;
            this.volumeSMA10 = volumeSMA10;
        }

        // Getters
        public long getCurrentVolume() { return currentVolume; }
        public double getAvgVolume() { return avgVolume; }
        public long getMaxVolume() { return maxVolume; }
        public long getMinVolume() { return minVolume; }
        public double getVolumeToAvg() { return volumeToAvg; }
        public double getVolumeToMax() { return volumeToMax; }
        public double getVolumeSMA5() { return volumeSMA5; }
        public double getVolumeSMA10() { return volumeSMA10; }
    }

    public static class VolatilityFeatures {
        private final double volatility;
        private final double volatility5d;
        private final double volatility10d;
        private final double volatility20d;
        private final double avgHighLowRatio;

        public VolatilityFeatures() {
            this(0.0, 0.0, 0.0, 0.0, 0.0);
        }

        public VolatilityFeatures(double volatility, double volatility5d, double volatility10d,
                double volatility20d, double avgHighLowRatio) {
            this.volatility = volatility;
            this.volatility5d = volatility5d;
            this.volatility10d = volatility10d;
            this.volatility20d = volatility20d;
            this.avgHighLowRatio = avgHighLowRatio;
        }

        // Getters
        public double getVolatility() { return volatility; }
        public double getVolatility5d() { return volatility5d; }
        public double getVolatility10d() { return volatility10d; }
        public double getVolatility20d() { return volatility20d; }
        public double getAvgHighLowRatio() { return avgHighLowRatio; }
    }

    public static class MomentumFeatures {
        private final double momentum1d;
        private final double momentum5d;
        private final double momentum10d;
        private final double momentum20d;
        private final double roc5;
        private final double roc10;
        private final double momentumOscillator;

        public MomentumFeatures() {
            this(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }

        public MomentumFeatures(double momentum1d, double momentum5d, double momentum10d,
                double momentum20d, double roc5, double roc10, double momentumOscillator) {
            this.momentum1d = momentum1d;
            this.momentum5d = momentum5d;
            this.momentum10d = momentum10d;
            this.momentum20d = momentum20d;
            this.roc5 = roc5;
            this.roc10 = roc10;
            this.momentumOscillator = momentumOscillator;
        }

        // Getters
        public double getMomentum1d() { return momentum1d; }
        public double getMomentum5d() { return momentum5d; }
        public double getMomentum10d() { return momentum10d; }
        public double getMomentum20d() { return momentum20d; }
        public double getRoc5() { return roc5; }
        public double getRoc10() { return roc10; }
        public double getMomentumOscillator() { return momentumOscillator; }
    }
}
```

## Part 2: Model Training and Deployment Pipeline

### ML Pipeline Orchestrator

```java
package com.securetrading.ml.pipeline;

import ai.djl.training.TrainingResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class MLPipelineOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(MLPipelineOrchestrator.class);

    @Autowired
    private DataPipelineService dataPipelineService;

    @Autowired
    private FeatureEngineeringService featureEngineeringService;

    @Autowired
    private ModelTrainingService modelTrainingService;

    @Autowired
    private ModelValidationService modelValidationService;

    @Autowired
    private ModelDeploymentService modelDeploymentService;

    @Autowired
    private ModelMonitoringService modelMonitoringService;

    private final Map<String, PipelineExecution> activePipelines = new ConcurrentHashMap<>();

    @Async
    public CompletableFuture<PipelineResult> executePipeline(PipelineConfig config) {
        String executionId = UUID.randomUUID().toString();
        PipelineExecution execution = new PipelineExecution(executionId, config);
        activePipelines.put(executionId, execution);

        try {
            logger.info("Starting ML pipeline execution: {}", executionId);
            execution.updateStatus(PipelineStatus.RUNNING);

            // Step 1: Data Pipeline
            logger.info("Executing data pipeline for execution: {}", executionId);
            execution.updateStep(PipelineStep.DATA_PIPELINE);
            DataPipelineResult dataResult = dataPipelineService.executeDataPipeline(config.getDataConfig());
            
            if (!dataResult.isSuccessful()) {
                throw new PipelineException("Data pipeline failed: " + dataResult.getErrorMessage());
            }

            // Step 2: Feature Engineering
            logger.info("Executing feature engineering for execution: {}", executionId);
            execution.updateStep(PipelineStep.FEATURE_ENGINEERING);
            FeatureEngineeringResult featureResult = featureEngineeringService.processFeatures(
                    dataResult.getDataset(), config.getFeatureConfig());
            
            if (!featureResult.isSuccessful()) {
                throw new PipelineException("Feature engineering failed: " + featureResult.getErrorMessage());
            }

            // Step 3: Model Training
            logger.info("Training model for execution: {}", executionId);
            execution.updateStep(PipelineStep.MODEL_TRAINING);
            ModelTrainingResult trainingResult = modelTrainingService.trainModel(
                    featureResult.getFeatureDataset(), config.getTrainingConfig());
            
            if (!trainingResult.isSuccessful()) {
                throw new PipelineException("Model training failed: " + trainingResult.getErrorMessage());
            }

            // Step 4: Model Validation
            logger.info("Validating model for execution: {}", executionId);
            execution.updateStep(PipelineStep.MODEL_VALIDATION);
            ModelValidationResult validationResult = modelValidationService.validateModel(
                    trainingResult.getTrainedModel(), featureResult.getValidationDataset());
            
            if (!validationResult.meetsQualityThresholds()) {
                throw new PipelineException("Model validation failed: " + validationResult.getValidationReport());
            }

            // Step 5: Model Deployment (if validation passes)
            if (config.isAutoDeployEnabled() && validationResult.meetsDeploymentCriteria()) {
                logger.info("Deploying model for execution: {}", executionId);
                execution.updateStep(PipelineStep.MODEL_DEPLOYMENT);
                ModelDeploymentResult deploymentResult = modelDeploymentService.deployModel(
                        trainingResult.getTrainedModel(), config.getDeploymentConfig());
                
                if (!deploymentResult.isSuccessful()) {
                    throw new PipelineException("Model deployment failed: " + deploymentResult.getErrorMessage());
                }

                // Step 6: Setup Monitoring
                logger.info("Setting up monitoring for execution: {}", executionId);
                execution.updateStep(PipelineStep.MONITORING_SETUP);
                modelMonitoringService.setupMonitoring(
                        deploymentResult.getDeployedModelId(), config.getMonitoringConfig());
            }

            execution.updateStatus(PipelineStatus.COMPLETED);
            logger.info("ML pipeline execution completed successfully: {}", executionId);

            return CompletableFuture.completedFuture(new PipelineResult(
                    executionId,
                    PipelineStatus.COMPLETED,
                    trainingResult.getTrainedModel(),
                    validationResult,
                    "Pipeline executed successfully"
            ));

        } catch (Exception e) {
            execution.updateStatus(PipelineStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
            logger.error("ML pipeline execution failed: {}", executionId, e);

            return CompletableFuture.completedFuture(new PipelineResult(
                    executionId,
                    PipelineStatus.FAILED,
                    null,
                    null,
                    "Pipeline failed: " + e.getMessage()
            ));
        } finally {
            activePipelines.remove(executionId);
        }
    }

    public PipelineExecution getPipelineStatus(String executionId) {
        return activePipelines.get(executionId);
    }

    public Map<String, PipelineExecution> getActivePipelines() {
        return new ConcurrentHashMap<>(activePipelines);
    }

    // Supporting classes
    public static class PipelineConfig {
        private final String modelName;
        private final String modelType;
        private final DataPipelineConfig dataConfig;
        private final FeatureEngineeringConfig featureConfig;
        private final ModelTrainingConfig trainingConfig;
        private final ModelDeploymentConfig deploymentConfig;
        private final ModelMonitoringConfig monitoringConfig;
        private final boolean autoDeployEnabled;

        public PipelineConfig(String modelName, String modelType,
                DataPipelineConfig dataConfig, FeatureEngineeringConfig featureConfig,
                ModelTrainingConfig trainingConfig, ModelDeploymentConfig deploymentConfig,
                ModelMonitoringConfig monitoringConfig, boolean autoDeployEnabled) {
            this.modelName = modelName;
            this.modelType = modelType;
            this.dataConfig = dataConfig;
            this.featureConfig = featureConfig;
            this.trainingConfig = trainingConfig;
            this.deploymentConfig = deploymentConfig;
            this.monitoringConfig = monitoringConfig;
            this.autoDeployEnabled = autoDeployEnabled;
        }

        // Getters
        public String getModelName() { return modelName; }
        public String getModelType() { return modelType; }
        public DataPipelineConfig getDataConfig() { return dataConfig; }
        public FeatureEngineeringConfig getFeatureConfig() { return featureConfig; }
        public ModelTrainingConfig getTrainingConfig() { return trainingConfig; }
        public ModelDeploymentConfig getDeploymentConfig() { return deploymentConfig; }
        public ModelMonitoringConfig getMonitoringConfig() { return monitoringConfig; }
        public boolean isAutoDeployEnabled() { return autoDeployEnabled; }
    }

    public static class PipelineExecution {
        private final String executionId;
        private final PipelineConfig config;
        private final LocalDateTime startTime;
        private PipelineStatus status;
        private PipelineStep currentStep;
        private String errorMessage;
        private LocalDateTime lastUpdated;

        public PipelineExecution(String executionId, PipelineConfig config) {
            this.executionId = executionId;
            this.config = config;
            this.startTime = LocalDateTime.now();
            this.status = PipelineStatus.PENDING;
            this.currentStep = PipelineStep.INITIALIZATION;
            this.lastUpdated = LocalDateTime.now();
        }

        public void updateStatus(PipelineStatus status) {
            this.status = status;
            this.lastUpdated = LocalDateTime.now();
        }

        public void updateStep(PipelineStep step) {
            this.currentStep = step;
            this.lastUpdated = LocalDateTime.now();
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        // Getters
        public String getExecutionId() { return executionId; }
        public PipelineConfig getConfig() { return config; }
        public LocalDateTime getStartTime() { return startTime; }
        public PipelineStatus getStatus() { return status; }
        public PipelineStep getCurrentStep() { return currentStep; }
        public String getErrorMessage() { return errorMessage; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
    }

    public static class PipelineResult {
        private final String executionId;
        private final PipelineStatus status;
        private final Object trainedModel;
        private final ModelValidationResult validationResult;
        private final String message;

        public PipelineResult(String executionId, PipelineStatus status, Object trainedModel,
                ModelValidationResult validationResult, String message) {
            this.executionId = executionId;
            this.status = status;
            this.trainedModel = trainedModel;
            this.validationResult = validationResult;
            this.message = message;
        }

        // Getters
        public String getExecutionId() { return executionId; }
        public PipelineStatus getStatus() { return status; }
        public Object getTrainedModel() { return trainedModel; }
        public ModelValidationResult getValidationResult() { return validationResult; }
        public String getMessage() { return message; }
    }

    public enum PipelineStatus {
        PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    }

    public enum PipelineStep {
        INITIALIZATION,
        DATA_PIPELINE,
        FEATURE_ENGINEERING,
        MODEL_TRAINING,
        MODEL_VALIDATION,
        MODEL_DEPLOYMENT,
        MONITORING_SETUP
    }

    public static class PipelineException extends RuntimeException {
        public PipelineException(String message) {
            super(message);
        }

        public PipelineException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
```

### Real-time Prediction Service

```java
package com.securetrading.ml.service;

import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@Service
public class MLPredictionService {

    private static final Logger logger = LoggerFactory.getLogger(MLPredictionService.class);

    @Autowired
    private MLModelManager modelManager;

    @Autowired
    private FinancialFeatureService featureService;

    @Autowired
    private ModelPerformanceTracker performanceTracker;

    private final Map<String, PredictionCache> predictionCache = new ConcurrentHashMap<>();

    public CompletableFuture<PredictionResult> predictPriceMovement(String symbol, String modelVersion) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();
                
                // Extract features
                FeatureVector features = featureService.extractFeatures(
                        symbol, LocalDateTime.now(), 30);
                
                // Load model
                ZooModel<NDArray, NDArray> model = modelManager.loadModel(
                        "price-movement-" + modelVersion, NDArray.class, NDArray.class);
                
                // Make prediction
                PredictionResult result = makePrediction(model, features, symbol);
                
                long endTime = System.currentTimeMillis();
                long latency = endTime - startTime;
                
                // Track performance
                performanceTracker.recordPrediction(modelVersion, latency, result.getConfidence());
                
                logger.info("Prediction completed for symbol: {} in {}ms with confidence: {}", 
                        symbol, latency, result.getConfidence());
                
                return result;
                
            } catch (Exception e) {
                logger.error("Prediction failed for symbol: {}", symbol, e);
                return new PredictionResult(
                        symbol,
                        PredictionType.ERROR,
                        0.0,
                        0.0,
                        "Prediction failed: " + e.getMessage(),
                        LocalDateTime.now()
                );
            }
        });
    }

    @Cacheable(value = "batch-predictions", key = "#symbols.hashCode() + '_' + #modelVersion")
    public CompletableFuture<List<PredictionResult>> predictBatch(List<String> symbols, String modelVersion) {
        return CompletableFuture.supplyAsync(() -> {
            List<PredictionResult> results = new ArrayList<>();
            
            try {
                // Load model once for batch processing
                ZooModel<NDArray, NDArray> model = modelManager.loadModel(
                        "price-movement-" + modelVersion, NDArray.class, NDArray.class);
                
                // Process symbols in parallel
                List<CompletableFuture<PredictionResult>> futures = symbols.stream()
                        .map(symbol -> CompletableFuture.supplyAsync(() -> {
                            try {
                                FeatureVector features = featureService.extractFeatures(
                                        symbol, LocalDateTime.now(), 30);
                                return makePrediction(model, features, symbol);
                            } catch (Exception e) {
                                logger.error("Batch prediction failed for symbol: {}", symbol, e);
                                return new PredictionResult(
                                        symbol,
                                        PredictionType.ERROR,
                                        0.0,
                                        0.0,
                                        "Batch prediction failed: " + e.getMessage(),
                                        LocalDateTime.now()
                                );
                            }
                        }))
                        .toList();
                
                // Wait for all predictions to complete
                for (CompletableFuture<PredictionResult> future : futures) {
                    results.add(future.get());
                }
                
                logger.info("Batch prediction completed for {} symbols", symbols.size());
                
            } catch (Exception e) {
                logger.error("Batch prediction failed", e);
                // Return error results for all symbols
                for (String symbol : symbols) {
                    results.add(new PredictionResult(
                            symbol,
                            PredictionType.ERROR,
                            0.0,
                            0.0,
                            "Batch prediction failed: " + e.getMessage(),
                            LocalDateTime.now()
                    ));
                }
            }
            
            return results;
        });
    }

    private PredictionResult makePrediction(ZooModel<NDArray, NDArray> model, 
            FeatureVector features, String symbol) throws TranslateException {
        
        try (Predictor<NDArray, NDArray> predictor = model.newPredictor();
             NDManager manager = NDManager.newBaseManager()) {
            
            // Convert features to NDArray
            NDArray input = featureService.featureVectorToNDArray(features, manager);
            
            // Make prediction
            NDArray output = predictor.predict(input);
            
            // Process output (assuming binary classification: up/down)
            float[] predictions = output.toFloatArray();
            
            // Interpret predictions
            double upProbability = predictions[1]; // Assuming index 1 is "up" class
            double downProbability = predictions[0]; // Assuming index 0 is "down" class
            
            PredictionType predictedDirection;
            double confidence;
            double expectedChange;
            
            if (upProbability > downProbability) {
                predictedDirection = PredictionType.PRICE_UP;
                confidence = upProbability;
                expectedChange = Math.min(upProbability * 0.05, 0.10); // Cap at 10%
            } else {
                predictedDirection = PredictionType.PRICE_DOWN;
                confidence = downProbability;
                expectedChange = -Math.min(downProbability * 0.05, 0.10); // Cap at -10%
            }
            
            return new PredictionResult(
                    symbol,
                    predictedDirection,
                    confidence,
                    expectedChange,
                    "Prediction successful",
                    LocalDateTime.now()
            );
        }
    }

    public PredictionResult getCachedPrediction(String symbol, String modelVersion) {
        String cacheKey = symbol + "_" + modelVersion;
        PredictionCache cached = predictionCache.get(cacheKey);
        
        if (cached != null && cached.isValid()) {
            return cached.getResult();
        }
        
        return null;
    }

    public void invalidateCache(String symbol) {
        predictionCache.entrySet().removeIf(entry -> entry.getKey().startsWith(symbol + "_"));
    }

    // Supporting classes
    public static class PredictionResult {
        private final String symbol;
        private final PredictionType prediction;
        private final double confidence;
        private final double expectedChange;
        private final String message;
        private final LocalDateTime timestamp;

        public PredictionResult(String symbol, PredictionType prediction, double confidence,
                double expectedChange, String message, LocalDateTime timestamp) {
            this.symbol = symbol;
            this.prediction = prediction;
            this.confidence = confidence;
            this.expectedChange = expectedChange;
            this.message = message;
            this.timestamp = timestamp;
        }

        // Getters
        public String getSymbol() { return symbol; }
        public PredictionType getPrediction() { return prediction; }
        public double getConfidence() { return confidence; }
        public double getExpectedChange() { return expectedChange; }
        public String getMessage() { return message; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    public enum PredictionType {
        PRICE_UP, PRICE_DOWN, SIDEWAYS, ERROR
    }

    private static class PredictionCache {
        private final PredictionResult result;
        private final LocalDateTime cachedAt;
        private final long ttlMinutes;

        public PredictionCache(PredictionResult result, long ttlMinutes) {
            this.result = result;
            this.cachedAt = LocalDateTime.now();
            this.ttlMinutes = ttlMinutes;
        }

        public boolean isValid() {
            return cachedAt.plusMinutes(ttlMinutes).isAfter(LocalDateTime.now());
        }

        public PredictionResult getResult() {
            return result;
        }
    }
}
```

## Part 3: Model Monitoring and MLOps

### Model Performance Monitoring

```java
package com.securetrading.ml.monitoring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ModelPerformanceMonitor {

    private static final Logger logger = LoggerFactory.getLogger(ModelPerformanceMonitor.class);

    @Autowired
    private MetricsCollectionService metricsService;

    @Autowired
    private AlertingService alertingService;

    @Autowired
    private ModelRetrainingService retrainingService;

    private final Map<String, ModelMetrics> modelMetricsCache = new ConcurrentHashMap<>();
    private final Map<String, List<PredictionOutcome>> predictionHistory = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void monitorModelPerformance() {
        logger.info("Starting model performance monitoring cycle");

        for (String modelId : getActiveModels()) {
            try {
                ModelMetrics metrics = calculateModelMetrics(modelId);
                modelMetricsCache.put(modelId, metrics);
                
                // Check for performance degradation
                checkPerformanceDegradation(modelId, metrics);
                
                // Check for data drift
                checkDataDrift(modelId, metrics);
                
                // Check for concept drift
                checkConceptDrift(modelId, metrics);
                
            } catch (Exception e) {
                logger.error("Failed to monitor model: {}", modelId, e);
            }
        }
    }

    private ModelMetrics calculateModelMetrics(String modelId) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minus(1, ChronoUnit.HOURS);
        
        List<PredictionOutcome> recentPredictions = getPredictionHistory(modelId, startTime, endTime);
        
        if (recentPredictions.isEmpty()) {
            return new ModelMetrics(modelId, 0, 0.0, 0.0, 0.0, 0.0, endTime);
        }

        // Calculate accuracy
        long correctPredictions = recentPredictions.stream()
                .filter(PredictionOutcome::isCorrect)
                .count();
        double accuracy = (double) correctPredictions / recentPredictions.size();

        // Calculate precision and recall for binary classification
        long truePositives = recentPredictions.stream()
                .filter(p -> p.isCorrect() && p.getPredictedClass() == 1)
                .count();
        long falsePositives = recentPredictions.stream()
                .filter(p -> !p.isCorrect() && p.getPredictedClass() == 1)
                .count();
        long falseNegatives = recentPredictions.stream()
                .filter(p -> !p.isCorrect() && p.getPredictedClass() == 0)
                .count();

        double precision = (truePositives + falsePositives) > 0 ? 
                (double) truePositives / (truePositives + falsePositives) : 0.0;
        double recall = (truePositives + falseNegatives) > 0 ? 
                (double) truePositives / (truePositives + falseNegatives) : 0.0;

        // Calculate average latency
        double avgLatency = recentPredictions.stream()
                .mapToDouble(PredictionOutcome::getLatencyMs)
                .average()
                .orElse(0.0);

        return new ModelMetrics(
                modelId,
                recentPredictions.size(),
                accuracy,
                precision,
                recall,
                avgLatency,
                endTime
        );
    }

    private void checkPerformanceDegradation(String modelId, ModelMetrics currentMetrics) {
        ModelPerformanceBaseline baseline = getPerformanceBaseline(modelId);
        
        if (baseline == null) {
            logger.warn("No performance baseline found for model: {}", modelId);
            return;
        }

        // Check accuracy degradation
        double accuracyDrop = baseline.getAccuracy() - currentMetrics.getAccuracy();
        if (accuracyDrop > 0.05) { // 5% accuracy drop threshold
            AlertInfo alert = new AlertInfo(
                    AlertType.PERFORMANCE_DEGRADATION,
                    AlertSeverity.HIGH,
                    modelId,
                    String.format("Model accuracy dropped by %.2f%% (from %.2f%% to %.2f%%)",
                            accuracyDrop * 100, baseline.getAccuracy() * 100, 
                            currentMetrics.getAccuracy() * 100)
            );
            alertingService.sendAlert(alert);
            
            // Trigger retraining if accuracy drops significantly
            if (accuracyDrop > 0.10) { // 10% accuracy drop
                retrainingService.scheduleRetraining(modelId, "Significant accuracy degradation detected");
            }
        }

        // Check latency degradation
        double latencyIncrease = (currentMetrics.getAvgLatencyMs() - baseline.getAvgLatencyMs()) / 
                baseline.getAvgLatencyMs();
        if (latencyIncrease > 0.50) { // 50% latency increase threshold
            AlertInfo alert = new AlertInfo(
                    AlertType.LATENCY_DEGRADATION,
                    AlertSeverity.MEDIUM,
                    modelId,
                    String.format("Model latency increased by %.1f%% (from %.2fms to %.2fms)",
                            latencyIncrease * 100, baseline.getAvgLatencyMs(), 
                            currentMetrics.getAvgLatencyMs())
            );
            alertingService.sendAlert(alert);
        }
    }

    private void checkDataDrift(String modelId, ModelMetrics currentMetrics) {
        // Get recent feature distributions
        FeatureDistribution currentDistribution = getCurrentFeatureDistribution(modelId);
        FeatureDistribution baselineDistribution = getBaselineFeatureDistribution(modelId);
        
        if (currentDistribution == null || baselineDistribution == null) {
            return;
        }

        // Calculate KL divergence or other drift metrics
        double driftScore = calculateDataDriftScore(currentDistribution, baselineDistribution);
        
        if (driftScore > 0.1) { // Drift threshold
            AlertInfo alert = new AlertInfo(
                    AlertType.DATA_DRIFT,
                    AlertSeverity.MEDIUM,
                    modelId,
                    String.format("Data drift detected with score: %.3f", driftScore)
            );
            alertingService.sendAlert(alert);
            
            if (driftScore > 0.2) { // Significant drift
                retrainingService.scheduleRetraining(modelId, "Significant data drift detected");
            }
        }
    }

    private void checkConceptDrift(String modelId, ModelMetrics currentMetrics) {
        // Check if the relationship between features and target has changed
        List<PredictionOutcome> recentPredictions = getPredictionHistory(
                modelId, LocalDateTime.now().minus(7, ChronoUnit.DAYS), LocalDateTime.now());
        
        if (recentPredictions.size() < 100) {
            return; // Not enough data
        }

        // Calculate performance over time windows
        List<Double> weeklyAccuracies = calculateWeeklyAccuracies(recentPredictions);
        
        // Check for consistent declining trend
        boolean conceptDrift = isDecreasingTrend(weeklyAccuracies, 0.02); // 2% threshold
        
        if (conceptDrift) {
            AlertInfo alert = new AlertInfo(
                    AlertType.CONCEPT_DRIFT,
                    AlertSeverity.HIGH,
                    modelId,
                    "Concept drift detected - model performance declining over time"
            );
            alertingService.sendAlert(alert);
            
            retrainingService.scheduleRetraining(modelId, "Concept drift detected");
        }
    }

    public ModelHealthReport generateModelHealthReport(String modelId) {
        ModelMetrics currentMetrics = modelMetricsCache.get(modelId);
        ModelPerformanceBaseline baseline = getPerformanceBaseline(modelId);
        
        if (currentMetrics == null) {
            return new ModelHealthReport(modelId, ModelHealth.UNKNOWN, 
                    "No recent metrics available", LocalDateTime.now());
        }

        // Determine overall health
        ModelHealth health = ModelHealth.HEALTHY;
        List<String> issues = new ArrayList<>();

        if (baseline != null) {
            double accuracyDrop = baseline.getAccuracy() - currentMetrics.getAccuracy();
            if (accuracyDrop > 0.10) {
                health = ModelHealth.CRITICAL;
                issues.add(String.format("Accuracy dropped by %.1f%%", accuracyDrop * 100));
            } else if (accuracyDrop > 0.05) {
                health = ModelHealth.WARNING;
                issues.add(String.format("Accuracy dropped by %.1f%%", accuracyDrop * 100));
            }

            double latencyIncrease = (currentMetrics.getAvgLatencyMs() - baseline.getAvgLatencyMs()) / 
                    baseline.getAvgLatencyMs();
            if (latencyIncrease > 1.0) {
                health = ModelHealth.CRITICAL;
                issues.add(String.format("Latency increased by %.1f%%", latencyIncrease * 100));
            } else if (latencyIncrease > 0.5) {
                health = ModelHealth.WARNING;
                issues.add(String.format("Latency increased by %.1f%%", latencyIncrease * 100));
            }
        }

        // Check prediction volume
        if (currentMetrics.getPredictionCount() == 0) {
            health = ModelHealth.CRITICAL;
            issues.add("No predictions in the last hour");
        }

        String summary = issues.isEmpty() ? "Model is performing normally" : 
                String.join("; ", issues);

        return new ModelHealthReport(modelId, health, summary, LocalDateTime.now());
    }

    // Helper methods
    private List<String> getActiveModels() {
        // Implementation would query model registry
        return Arrays.asList("price-movement-v1.0", "volatility-prediction-v2.0", "risk-assessment-v1.1");
    }

    private List<PredictionOutcome> getPredictionHistory(String modelId, 
            LocalDateTime startTime, LocalDateTime endTime) {
        return predictionHistory.getOrDefault(modelId, new ArrayList<>()).stream()
                .filter(p -> p.getTimestamp().isAfter(startTime) && p.getTimestamp().isBefore(endTime))
                .collect(Collectors.toList());
    }

    private ModelPerformanceBaseline getPerformanceBaseline(String modelId) {
        // Implementation would retrieve from database
        return new ModelPerformanceBaseline(modelId, 0.85, 0.80, 0.82, 150.0);
    }

    private FeatureDistribution getCurrentFeatureDistribution(String modelId) {
        // Implementation would calculate current feature statistics
        return null;
    }

    private FeatureDistribution getBaselineFeatureDistribution(String modelId) {
        // Implementation would retrieve baseline feature statistics
        return null;
    }

    private double calculateDataDriftScore(FeatureDistribution current, FeatureDistribution baseline) {
        // Implementation would calculate KL divergence or PSI
        return 0.0;
    }

    private List<Double> calculateWeeklyAccuracies(List<PredictionOutcome> predictions) {
        // Implementation would group predictions by week and calculate accuracy
        return new ArrayList<>();
    }

    private boolean isDecreasingTrend(List<Double> values, double threshold) {
        // Implementation would check for statistically significant decreasing trend
        return false;
    }

    // Supporting classes
    public static class ModelMetrics {
        private final String modelId;
        private final int predictionCount;
        private final double accuracy;
        private final double precision;
        private final double recall;
        private final double avgLatencyMs;
        private final LocalDateTime timestamp;

        public ModelMetrics(String modelId, int predictionCount, double accuracy, 
                double precision, double recall, double avgLatencyMs, LocalDateTime timestamp) {
            this.modelId = modelId;
            this.predictionCount = predictionCount;
            this.accuracy = accuracy;
            this.precision = precision;
            this.recall = recall;
            this.avgLatencyMs = avgLatencyMs;
            this.timestamp = timestamp;
        }

        // Getters
        public String getModelId() { return modelId; }
        public int getPredictionCount() { return predictionCount; }
        public double getAccuracy() { return accuracy; }
        public double getPrecision() { return precision; }
        public double getRecall() { return recall; }
        public double getAvgLatencyMs() { return avgLatencyMs; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    public static class PredictionOutcome {
        private final String modelId;
        private final String instanceId;
        private final int predictedClass;
        private final int actualClass;
        private final double confidence;
        private final long latencyMs;
        private final LocalDateTime timestamp;

        public PredictionOutcome(String modelId, String instanceId, int predictedClass, 
                int actualClass, double confidence, long latencyMs, LocalDateTime timestamp) {
            this.modelId = modelId;
            this.instanceId = instanceId;
            this.predictedClass = predictedClass;
            this.actualClass = actualClass;
            this.confidence = confidence;
            this.latencyMs = latencyMs;
            this.timestamp = timestamp;
        }

        public boolean isCorrect() {
            return predictedClass == actualClass;
        }

        // Getters
        public String getModelId() { return modelId; }
        public String getInstanceId() { return instanceId; }
        public int getPredictedClass() { return predictedClass; }
        public int getActualClass() { return actualClass; }
        public double getConfidence() { return confidence; }
        public long getLatencyMs() { return latencyMs; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    public static class ModelPerformanceBaseline {
        private final String modelId;
        private final double accuracy;
        private final double precision;
        private final double recall;
        private final double avgLatencyMs;

        public ModelPerformanceBaseline(String modelId, double accuracy, double precision, 
                double recall, double avgLatencyMs) {
            this.modelId = modelId;
            this.accuracy = accuracy;
            this.precision = precision;
            this.recall = recall;
            this.avgLatencyMs = avgLatencyMs;
        }

        // Getters
        public String getModelId() { return modelId; }
        public double getAccuracy() { return accuracy; }
        public double getPrecision() { return precision; }
        public double getRecall() { return recall; }
        public double getAvgLatencyMs() { return avgLatencyMs; }
    }

    public static class ModelHealthReport {
        private final String modelId;
        private final ModelHealth health;
        private final String summary;
        private final LocalDateTime timestamp;

        public ModelHealthReport(String modelId, ModelHealth health, String summary, 
                LocalDateTime timestamp) {
            this.modelId = modelId;
            this.health = health;
            this.summary = summary;
            this.timestamp = timestamp;
        }

        // Getters
        public String getModelId() { return modelId; }
        public ModelHealth getHealth() { return health; }
        public String getSummary() { return summary; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    public enum ModelHealth {
        HEALTHY, WARNING, CRITICAL, UNKNOWN
    }

    public enum AlertType {
        PERFORMANCE_DEGRADATION, LATENCY_DEGRADATION, DATA_DRIFT, CONCEPT_DRIFT
    }

    public enum AlertSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public static class AlertInfo {
        private final AlertType type;
        private final AlertSeverity severity;
        private final String modelId;
        private final String message;
        private final LocalDateTime timestamp;

        public AlertInfo(AlertType type, AlertSeverity severity, String modelId, String message) {
            this.type = type;
            this.severity = severity;
            this.modelId = modelId;
            this.message = message;
            this.timestamp = LocalDateTime.now();
        }

        // Getters
        public AlertType getType() { return type; }
        public AlertSeverity getSeverity() { return severity; }
        public String getModelId() { return modelId; }
        public String getMessage() { return message; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    // Placeholder classes
    public static class FeatureDistribution {
        // Implementation would contain feature statistics
    }
}
```

## Practical Exercises

### Exercise 1: Feature Engineering Pipeline
Build a comprehensive feature engineering pipeline that extracts technical indicators, sentiment data, and market microstructure features for stock price prediction.

### Exercise 2: Model Training Automation
Create an automated model training pipeline that handles data validation, hyperparameter tuning, and model selection with cross-validation.

### Exercise 3: Real-time Prediction Service
Implement a high-performance prediction service that can handle 1000+ predictions per second with sub-100ms latency.

### Exercise 4: Model Drift Detection
Build a system that detects and alerts on data drift, concept drift, and performance degradation in production models.

### Exercise 5: A/B Testing Framework
Create an A/B testing framework for comparing different model versions in production with statistical significance testing.

## Performance Considerations

### ML Performance Optimization
- **Model Caching**: Cache loaded models in memory with LRU eviction
- **Batch Processing**: Process multiple predictions together for better throughput
- **Feature Preprocessing**: Cache and reuse preprocessed features when possible
- **Async Processing**: Use async methods for non-blocking prediction requests

### Monitoring and Alerting
- **Real-time Metrics**: Track prediction latency, throughput, and accuracy in real-time
- **Custom Dashboards**: Create monitoring dashboards with model performance visualizations
- **Automated Alerting**: Set up alerts for performance degradation and drift detection
- **Health Checks**: Implement comprehensive health checks for model endpoints

## ML Best Practices
- Use proper data validation and feature validation pipelines
- Implement comprehensive logging for model inputs, outputs, and performance metrics
- Use model versioning and experiment tracking for reproducibility
- Implement proper error handling and fallback mechanisms
- Regular model retraining and performance evaluation
- A/B testing for model deployment and performance comparison
- Comprehensive monitoring for data quality and model performance
- Implement data lineage tracking for debugging and compliance

## Summary

Day 109 covered comprehensive machine learning integration with Java applications:

1. **ML Framework Integration**: Deep Java Library (DJL) integration with comprehensive feature engineering
2. **ML Pipeline Orchestration**: Automated pipelines for training, validation, and deployment
3. **Real-time Prediction Service**: High-performance prediction service with caching and monitoring
4. **Model Performance Monitoring**: Comprehensive monitoring for drift detection and performance tracking
5. **MLOps Implementation**: Production-ready ML operations with automated retraining and alerting

These implementations provide the foundation for building scalable, maintainable ML systems that can operate reliably in production environments while maintaining high performance and accuracy.