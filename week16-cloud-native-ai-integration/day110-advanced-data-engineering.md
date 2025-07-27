# Day 110: Advanced Data Engineering - Stream Processing, Data Lakes & Real-time Analytics

## Learning Objectives
- Design and implement high-performance stream processing pipelines with Apache Kafka and Kafka Streams
- Build scalable data lake architectures with Apache Iceberg and Delta Lake integration
- Create real-time analytics systems with complex event processing and windowing
- Implement data lineage tracking and quality monitoring systems
- Design multi-tier storage strategies with hot, warm, and cold data patterns
- Build event-driven architectures with CQRS and event sourcing patterns

## Part 1: Apache Kafka Streams Processing

### Advanced Kafka Streams Configuration

```java
package com.securetrading.data.streaming.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.apache.kafka.streams.processor.WallclockTimestampExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafkaStreams
public class KafkaStreamsConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.streams.application-id}")
    private String applicationId;

    @Value("${spring.kafka.streams.state-dir:/tmp/kafka-streams}")
    private String stateDir;

    @Bean
    public KafkaStreamsConfiguration kafkaStreamsConfig() {
        Map<String, Object> props = new HashMap<>();
        
        // Basic configuration
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        
        // Performance optimization
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 4);
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 10 * 1024 * 1024L); // 10MB
        
        // State store configuration
        props.put(StreamsConfig.STATE_DIR_CONFIG, stateDir);
        props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, 3);
        
        // Error handling
        props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, 
                LogAndContinueExceptionHandler.class);
        
        // Timestamp extraction
        props.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, 
                WallclockTimestampExtractor.class);
        
        // Consumer configuration
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1000);
        
        // Producer configuration
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        return new KafkaStreamsConfiguration(props);
    }

    @Bean
    public JsonSerde<MarketDataEvent> marketDataEventSerde() {
        JsonSerde<MarketDataEvent> serde = new JsonSerde<>(MarketDataEvent.class);
        serde.configure(Map.of(), false);
        return serde;
    }

    @Bean
    public JsonSerde<TradingSignal> tradingSignalSerde() {
        JsonSerde<TradingSignal> serde = new JsonSerde<>(TradingSignal.class);
        serde.configure(Map.of(), false);
        return serde;
    }

    @Bean
    public JsonSerde<AggregatedMetrics> aggregatedMetricsSerde() {
        JsonSerde<AggregatedMetrics> serde = new JsonSerde<>(AggregatedMetrics.class);
        serde.configure(Map.of(), false);
        return serde;
    }
}
```

### Real-time Market Data Processing Pipeline

```java
package com.securetrading.data.streaming.processor;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Component
public class MarketDataStreamProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataStreamProcessor.class);

    @Autowired
    private JsonSerde<MarketDataEvent> marketDataEventSerde;

    @Autowired
    private JsonSerde<TradingSignal> tradingSignalSerde;

    @Autowired
    private JsonSerde<AggregatedMetrics> aggregatedMetricsSerde;

    @Autowired
    public void processMarketDataStreams(StreamsBuilder streamsBuilder) {
        
        // Create state stores
        StoreBuilder<KeyValueStore<String, Double>> priceStoreBuilder = Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore("price-store"),
                Serdes.String(),
                Serdes.Double()
        );
        streamsBuilder.addStateStore(priceStoreBuilder);

        StoreBuilder<KeyValueStore<String, VolumeMetrics>> volumeStoreBuilder = Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore("volume-store"),
                Serdes.String(),
                new JsonSerde<>(VolumeMetrics.class)
        );
        streamsBuilder.addStateStore(volumeStoreBuilder);

        // Main market data stream
        KStream<String, MarketDataEvent> marketDataStream = streamsBuilder
                .stream("market-data-events", 
                        Consumed.with(Serdes.String(), marketDataEventSerde))
                .filter((key, value) -> value != null && value.getSymbol() != null)
                .selectKey((key, value) -> value.getSymbol());

        // Process price movements and generate signals
        processSignalGeneration(marketDataStream);
        
        // Process real-time aggregations
        processRealTimeAggregations(marketDataStream);
        
        // Process anomaly detection
        processAnomalyDetection(marketDataStream);
        
        // Process risk calculations
        processRiskCalculations(marketDataStream);
    }

    private void processSignalGeneration(KStream<String, MarketDataEvent> marketDataStream) {
        marketDataStream
                .transform(() -> new SignalGenerationTransformer(), "price-store")
                .filter((key, signal) -> signal != null && signal.getConfidence() > 0.7)
                .peek((key, signal) -> logger.info("Generated trading signal: {} for {}", 
                        signal.getSignalType(), key))
                .to("trading-signals", Produced.with(Serdes.String(), tradingSignalSerde));
    }

    private void processRealTimeAggregations(KStream<String, MarketDataEvent> marketDataStream) {
        // 1-minute window aggregations
        TimeWindows oneMinuteWindow = TimeWindows.of(Duration.ofMinutes(1))
                .advanceBy(Duration.ofSeconds(10)); // Sliding window

        KTable<Windowed<String>, AggregatedMetrics> oneMinuteAggregations = marketDataStream
                .groupByKey(Grouped.with(Serdes.String(), marketDataEventSerde))
                .windowedBy(oneMinuteWindow)
                .aggregate(
                        AggregatedMetrics::new,
                        (key, value, aggregate) -> aggregate.update(value),
                        Materialized.<String, AggregatedMetrics, WindowStore<Bytes, byte[]>>as("one-minute-aggregates-store")
                                .withKeySerde(Serdes.String())
                                .withValueSerde(aggregatedMetricsSerde)
                );

        // Output 1-minute aggregations
        oneMinuteAggregations
                .toStream()
                .map((windowedKey, metrics) -> KeyValue.pair(
                        windowedKey.key() + "-" + windowedKey.window().start(),
                        metrics.withWindowInfo(windowedKey.window().start(), windowedKey.window().end())
                ))
                .to("market-aggregations-1min", Produced.with(Serdes.String(), aggregatedMetricsSerde));

        // 5-minute window aggregations
        TimeWindows fiveMinuteWindow = TimeWindows.of(Duration.ofMinutes(5))
                .advanceBy(Duration.ofMinutes(1));

        marketDataStream
                .groupByKey(Grouped.with(Serdes.String(), marketDataEventSerde))
                .windowedBy(fiveMinuteWindow)
                .aggregate(
                        AggregatedMetrics::new,
                        (key, value, aggregate) -> aggregate.update(value),
                        Materialized.<String, AggregatedMetrics, WindowStore<Bytes, byte[]>>as("five-minute-aggregates-store")
                                .withKeySerde(Serdes.String())
                                .withValueSerde(aggregatedMetricsSerde)
                )
                .toStream()
                .map((windowedKey, metrics) -> KeyValue.pair(
                        windowedKey.key() + "-" + windowedKey.window().start(),
                        metrics.withWindowInfo(windowedKey.window().start(), windowedKey.window().end())
                ))
                .to("market-aggregations-5min", Produced.with(Serdes.String(), aggregatedMetricsSerde));
    }

    private void processAnomalyDetection(KStream<String, MarketDataEvent> marketDataStream) {
        marketDataStream
                .transform(() -> new AnomalyDetectionTransformer(), "price-store", "volume-store")
                .filter((key, anomaly) -> anomaly != null && anomaly.getSeverity().ordinal() >= 2) // Medium+ severity
                .peek((key, anomaly) -> logger.warn("Market anomaly detected: {} for symbol {}", 
                        anomaly.getAnomalyType(), key))
                .to("market-anomalies", Produced.with(Serdes.String(), new JsonSerde<>(MarketAnomaly.class)));
    }

    private void processRiskCalculations(KStream<String, MarketDataEvent> marketDataStream) {
        // Calculate real-time VaR and risk metrics
        marketDataStream
                .transform(() -> new RiskCalculationTransformer(), "price-store")
                .filter((key, riskMetrics) -> riskMetrics != null)
                .to("risk-metrics", Produced.with(Serdes.String(), new JsonSerde<>(RiskMetrics.class)));
    }

    // Transformer implementations
    private static class SignalGenerationTransformer implements Transformer<String, MarketDataEvent, KeyValue<String, TradingSignal>> {
        private ProcessorContext context;
        private KeyValueStore<String, Double> priceStore;

        @Override
        public void init(ProcessorContext context) {
            this.context = context;
            this.priceStore = context.getStateStore("price-store");
        }

        @Override
        public KeyValue<String, TradingSignal> transform(String key, MarketDataEvent event) {
            if (event == null || event.getPrice() <= 0) {
                return null;
            }

            Double previousPrice = priceStore.get(key);
            priceStore.put(key, event.getPrice());

            if (previousPrice == null) {
                return null; // Not enough data for signal generation
            }

            // Simple momentum signal
            double priceChange = (event.getPrice() - previousPrice) / previousPrice;
            
            TradingSignal signal = null;
            if (Math.abs(priceChange) > 0.02) { // 2% threshold
                SignalType signalType = priceChange > 0 ? SignalType.BUY : SignalType.SELL;
                double confidence = Math.min(Math.abs(priceChange) * 10, 1.0); // Scale to 0-1
                
                signal = new TradingSignal(
                        key,
                        signalType,
                        event.getPrice(),
                        confidence,
                        priceChange,
                        LocalDateTime.ofInstant(Instant.ofEpochMilli(context.timestamp()), ZoneOffset.UTC)
                );
            }

            return signal != null ? KeyValue.pair(key, signal) : null;
        }

        @Override
        public void close() {
            // Cleanup if needed
        }
    }

    private static class AnomalyDetectionTransformer implements Transformer<String, MarketDataEvent, KeyValue<String, MarketAnomaly>> {
        private ProcessorContext context;
        private KeyValueStore<String, Double> priceStore;
        private KeyValueStore<String, VolumeMetrics> volumeStore;

        @Override
        public void init(ProcessorContext context) {
            this.context = context;
            this.priceStore = context.getStateStore("price-store");
            this.volumeStore = context.getStateStore("volume-store");
        }

        @Override
        public KeyValue<String, MarketAnomaly> transform(String key, MarketDataEvent event) {
            if (event == null) {
                return null;
            }

            // Price anomaly detection
            Double previousPrice = priceStore.get(key);
            if (previousPrice != null) {
                double priceChange = Math.abs((event.getPrice() - previousPrice) / previousPrice);
                if (priceChange > 0.10) { // 10% price spike
                    MarketAnomaly anomaly = new MarketAnomaly(
                            key,
                            AnomalyType.PRICE_SPIKE,
                            priceChange > 0.20 ? AnomalySeverity.HIGH : AnomalySeverity.MEDIUM,
                            String.format("Price changed by %.2f%%", priceChange * 100),
                            LocalDateTime.ofInstant(Instant.ofEpochMilli(context.timestamp()), ZoneOffset.UTC)
                    );
                    return KeyValue.pair(key, anomaly);
                }
            }
            priceStore.put(key, event.getPrice());

            // Volume anomaly detection
            VolumeMetrics previousVolume = volumeStore.get(key);
            VolumeMetrics currentVolume = new VolumeMetrics(event.getVolume(), 1);
            
            if (previousVolume != null) {
                double avgVolume = previousVolume.getTotalVolume() / previousVolume.getCount();
                double volumeRatio = event.getVolume() / avgVolume;
                
                if (volumeRatio > 5.0) { // 5x average volume
                    MarketAnomaly anomaly = new MarketAnomaly(
                            key,
                            AnomalyType.VOLUME_SPIKE,
                            volumeRatio > 10.0 ? AnomalySeverity.HIGH : AnomalySeverity.MEDIUM,
                            String.format("Volume %.1fx higher than average", volumeRatio),
                            LocalDateTime.ofInstant(Instant.ofEpochMilli(context.timestamp()), ZoneOffset.UTC)
                    );
                    return KeyValue.pair(key, anomaly);
                }
                
                currentVolume = previousVolume.update(event.getVolume());
            }
            
            volumeStore.put(key, currentVolume);
            return null;
        }

        @Override
        public void close() {
            // Cleanup if needed
        }
    }

    private static class RiskCalculationTransformer implements Transformer<String, MarketDataEvent, KeyValue<String, RiskMetrics>> {
        private ProcessorContext context;
        private KeyValueStore<String, Double> priceStore;

        @Override
        public void init(ProcessorContext context) {
            this.context = context;
            this.priceStore = context.getStateStore("price-store");
        }

        @Override
        public KeyValue<String, RiskMetrics> transform(String key, MarketDataEvent event) {
            if (event == null) {
                return null;
            }

            // Simple risk calculation - in practice this would be more sophisticated
            Double previousPrice = priceStore.get(key);
            if (previousPrice == null) {
                priceStore.put(key, event.getPrice());
                return null;
            }

            double priceReturn = (event.getPrice() - previousPrice) / previousPrice;
            double volatility = Math.abs(priceReturn); // Simplified volatility measure
            
            // Simple VaR calculation (95% confidence)
            double var95 = volatility * 1.645; // Normal distribution 95th percentile
            
            RiskLevel riskLevel;
            if (volatility > 0.05) {
                riskLevel = RiskLevel.HIGH;
            } else if (volatility > 0.02) {
                riskLevel = RiskLevel.MEDIUM;
            } else {
                riskLevel = RiskLevel.LOW;
            }

            RiskMetrics riskMetrics = new RiskMetrics(
                    key,
                    volatility,
                    var95,
                    riskLevel,
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(context.timestamp()), ZoneOffset.UTC)
            );

            priceStore.put(key, event.getPrice());
            return KeyValue.pair(key, riskMetrics);
        }

        @Override
        public void close() {
            // Cleanup if needed
        }
    }

    // Supporting data classes
    public static class MarketDataEvent {
        private String symbol;
        private double price;
        private long volume;
        private LocalDateTime timestamp;

        // Constructors, getters, setters
        public MarketDataEvent() {}

        public MarketDataEvent(String symbol, double price, long volume, LocalDateTime timestamp) {
            this.symbol = symbol;
            this.price = price;
            this.volume = volume;
            this.timestamp = timestamp;
        }

        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
        public long getVolume() { return volume; }
        public void setVolume(long volume) { this.volume = volume; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    public static class TradingSignal {
        private String symbol;
        private SignalType signalType;
        private double price;
        private double confidence;
        private double priceChange;
        private LocalDateTime timestamp;

        public TradingSignal() {}

        public TradingSignal(String symbol, SignalType signalType, double price, 
                double confidence, double priceChange, LocalDateTime timestamp) {
            this.symbol = symbol;
            this.signalType = signalType;
            this.price = price;
            this.confidence = confidence;
            this.priceChange = priceChange;
            this.timestamp = timestamp;
        }

        // Getters and setters
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public SignalType getSignalType() { return signalType; }
        public void setSignalType(SignalType signalType) { this.signalType = signalType; }
        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public double getPriceChange() { return priceChange; }
        public void setPriceChange(double priceChange) { this.priceChange = priceChange; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    public static class AggregatedMetrics {
        private String symbol;
        private double openPrice;
        private double closePrice;
        private double highPrice;
        private double lowPrice;
        private long totalVolume;
        private int count;
        private long windowStart;
        private long windowEnd;

        public AggregatedMetrics() {
            this.highPrice = Double.MIN_VALUE;
            this.lowPrice = Double.MAX_VALUE;
        }

        public AggregatedMetrics update(MarketDataEvent event) {
            if (this.symbol == null) {
                this.symbol = event.getSymbol();
                this.openPrice = event.getPrice();
            }
            
            this.closePrice = event.getPrice();
            this.highPrice = Math.max(this.highPrice, event.getPrice());
            this.lowPrice = Math.min(this.lowPrice, event.getPrice());
            this.totalVolume += event.getVolume();
            this.count++;
            
            return this;
        }

        public AggregatedMetrics withWindowInfo(long windowStart, long windowEnd) {
            this.windowStart = windowStart;
            this.windowEnd = windowEnd;
            return this;
        }

        // Getters and setters
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public double getOpenPrice() { return openPrice; }
        public void setOpenPrice(double openPrice) { this.openPrice = openPrice; }
        public double getClosePrice() { return closePrice; }
        public void setClosePrice(double closePrice) { this.closePrice = closePrice; }
        public double getHighPrice() { return highPrice; }
        public void setHighPrice(double highPrice) { this.highPrice = highPrice; }
        public double getLowPrice() { return lowPrice; }
        public void setLowPrice(double lowPrice) { this.lowPrice = lowPrice; }
        public long getTotalVolume() { return totalVolume; }
        public void setTotalVolume(long totalVolume) { this.totalVolume = totalVolume; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public long getWindowStart() { return windowStart; }
        public void setWindowStart(long windowStart) { this.windowStart = windowStart; }
        public long getWindowEnd() { return windowEnd; }
        public void setWindowEnd(long windowEnd) { this.windowEnd = windowEnd; }
    }

    public static class VolumeMetrics {
        private long totalVolume;
        private int count;

        public VolumeMetrics() {}

        public VolumeMetrics(long volume, int count) {
            this.totalVolume = volume;
            this.count = count;
        }

        public VolumeMetrics update(long volume) {
            return new VolumeMetrics(this.totalVolume + volume, this.count + 1);
        }

        public long getTotalVolume() { return totalVolume; }
        public void setTotalVolume(long totalVolume) { this.totalVolume = totalVolume; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
    }

    public enum SignalType { BUY, SELL, HOLD }
    public enum AnomalyType { PRICE_SPIKE, VOLUME_SPIKE, CORRELATION_BREAK }
    public enum AnomalySeverity { LOW, MEDIUM, HIGH, CRITICAL }
    public enum RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }

    public static class MarketAnomaly {
        private String symbol;
        private AnomalyType anomalyType;
        private AnomalySeverity severity;
        private String description;
        private LocalDateTime timestamp;

        public MarketAnomaly() {}

        public MarketAnomaly(String symbol, AnomalyType anomalyType, AnomalySeverity severity, 
                String description, LocalDateTime timestamp) {
            this.symbol = symbol;
            this.anomalyType = anomalyType;
            this.severity = severity;
            this.description = description;
            this.timestamp = timestamp;
        }

        // Getters and setters
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public AnomalyType getAnomalyType() { return anomalyType; }
        public void setAnomalyType(AnomalyType anomalyType) { this.anomalyType = anomalyType; }
        public AnomalySeverity getSeverity() { return severity; }
        public void setSeverity(AnomalySeverity severity) { this.severity = severity; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    public static class RiskMetrics {
        private String symbol;
        private double volatility;
        private double var95;
        private RiskLevel riskLevel;
        private LocalDateTime timestamp;

        public RiskMetrics() {}

        public RiskMetrics(String symbol, double volatility, double var95, 
                RiskLevel riskLevel, LocalDateTime timestamp) {
            this.symbol = symbol;
            this.volatility = volatility;
            this.var95 = var95;
            this.riskLevel = riskLevel;
            this.timestamp = timestamp;
        }

        // Getters and setters
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public double getVolatility() { return volatility; }
        public void setVolatility(double volatility) { this.volatility = volatility; }
        public double getVar95() { return var95; }
        public void setVar95(double var95) { this.var95 = var95; }
        public RiskLevel getRiskLevel() { return riskLevel; }
        public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}
```

## Part 2: Data Lake Architecture with Apache Iceberg

### Data Lake Configuration and Management

```java
package com.securetrading.data.lake.config;

import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.CatalogProperties;
import org.apache.iceberg.aws.glue.GlueCatalog;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.hadoop.HadoopCatalog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class DataLakeConfiguration {

    @Value("${data.lake.warehouse.path:/data/warehouse}")
    private String warehousePath;

    @Value("${data.lake.catalog.type:hadoop}")
    private String catalogType;

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Bean
    @Profile("local")
    public Catalog hadoopCatalog() {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", "file://");
        
        return new HadoopCatalog(conf, warehousePath);
    }

    @Bean
    @Profile("!local")
    public Catalog glueCatalog() {
        Map<String, String> properties = new HashMap<>();
        properties.put(CatalogProperties.WAREHOUSE_LOCATION, warehousePath);
        properties.put(CatalogProperties.CATALOG_IMPL, GlueCatalog.class.getName());
        properties.put("region", awsRegion);
        
        GlueCatalog catalog = new GlueCatalog();
        catalog.initialize("glue_catalog", properties);
        return catalog;
    }

    @Bean
    public DataLakeManager dataLakeManager(Catalog catalog) {
        return new DataLakeManager(catalog, warehousePath);
    }
}
```

### Data Lake Management Service

```java
package com.securetrading.data.lake.service;

import org.apache.iceberg.*;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.IcebergGenerics;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.types.Types;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class DataLakeService {

    private static final Logger logger = LoggerFactory.getLogger(DataLakeService.class);

    @Autowired
    private Catalog catalog;

    private final String namespace = "trading_data";

    public void initializeDataLake() {
        try {
            // Create namespace if it doesn't exist
            if (!catalog.namespaceExists(Namespace.of(namespace))) {
                catalog.createNamespace(Namespace.of(namespace));
                logger.info("Created namespace: {}", namespace);
            }

            // Initialize tables
            initializeMarketDataTable();
            initializeTradingSignalsTable();
            initializeRiskMetricsTable();
            initializeAggregatedMetricsTable();

            logger.info("Data lake initialization completed successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize data lake", e);
            throw new RuntimeException("Data lake initialization failed", e);
        }
    }

    private void initializeMarketDataTable() {
        TableIdentifier tableId = TableIdentifier.of(namespace, "market_data");
        
        if (!catalog.tableExists(tableId)) {
            Schema schema = new Schema(
                    Types.NestedField.required(1, "symbol", Types.StringType.get()),
                    Types.NestedField.required(2, "price", Types.DoubleType.get()),
                    Types.NestedField.required(3, "volume", Types.LongType.get()),
                    Types.NestedField.required(4, "timestamp", Types.TimestampType.withoutZone()),
                    Types.NestedField.optional(5, "bid", Types.DoubleType.get()),
                    Types.NestedField.optional(6, "ask", Types.DoubleType.get()),
                    Types.NestedField.optional(7, "spread", Types.DoubleType.get()),
                    Types.NestedField.required(8, "event_time", Types.TimestampType.withoutZone()),
                    Types.NestedField.required(9, "ingestion_time", Types.TimestampType.withoutZone())
            );

            PartitionSpec partitionSpec = PartitionSpec.builderFor(schema)
                    .identity("symbol")
                    .day("timestamp")
                    .build();

            SortOrder sortOrder = SortOrder.builderFor(schema)
                    .asc("symbol")
                    .asc("timestamp")
                    .build();

            Map<String, String> properties = new HashMap<>();
            properties.put(TableProperties.FORMAT_VERSION, "2");
            properties.put(TableProperties.WRITE_AUDIT_PUBLISH_ENABLED, "true");
            properties.put(TableProperties.WRITE_DATA_LOCATION, 
                    String.format("%s/%s/market_data/data", getWarehousePath(), namespace));

            catalog.createTable(tableId, schema, partitionSpec, sortOrder, properties);
            logger.info("Created market_data table");
        }
    }

    private void initializeTradingSignalsTable() {
        TableIdentifier tableId = TableIdentifier.of(namespace, "trading_signals");
        
        if (!catalog.tableExists(tableId)) {
            Schema schema = new Schema(
                    Types.NestedField.required(1, "signal_id", Types.StringType.get()),
                    Types.NestedField.required(2, "symbol", Types.StringType.get()),
                    Types.NestedField.required(3, "signal_type", Types.StringType.get()),
                    Types.NestedField.required(4, "price", Types.DoubleType.get()),
                    Types.NestedField.required(5, "confidence", Types.DoubleType.get()),
                    Types.NestedField.required(6, "price_change", Types.DoubleType.get()),
                    Types.NestedField.required(7, "timestamp", Types.TimestampType.withoutZone()),
                    Types.NestedField.optional(8, "model_version", Types.StringType.get()),
                    Types.NestedField.optional(9, "features", Types.MapType.ofRequired(10, 11, 
                            Types.StringType.get(), Types.DoubleType.get()))
            );

            PartitionSpec partitionSpec = PartitionSpec.builderFor(schema)
                    .identity("symbol")
                    .hour("timestamp")
                    .build();

            catalog.createTable(tableId, schema, partitionSpec);
            logger.info("Created trading_signals table");
        }
    }

    private void initializeRiskMetricsTable() {
        TableIdentifier tableId = TableIdentifier.of(namespace, "risk_metrics");
        
        if (!catalog.tableExists(tableId)) {
            Schema schema = new Schema(
                    Types.NestedField.required(1, "symbol", Types.StringType.get()),
                    Types.NestedField.required(2, "volatility", Types.DoubleType.get()),
                    Types.NestedField.required(3, "var_95", Types.DoubleType.get()),
                    Types.NestedField.required(4, "risk_level", Types.StringType.get()),
                    Types.NestedField.required(5, "timestamp", Types.TimestampType.withoutZone()),
                    Types.NestedField.optional(6, "beta", Types.DoubleType.get()),
                    Types.NestedField.optional(7, "correlation_sp500", Types.DoubleType.get()),
                    Types.NestedField.optional(8, "sharpe_ratio", Types.DoubleType.get())
            );

            PartitionSpec partitionSpec = PartitionSpec.builderFor(schema)
                    .identity("symbol")
                    .day("timestamp")
                    .build();

            catalog.createTable(tableId, schema, partitionSpec);
            logger.info("Created risk_metrics table");
        }
    }

    private void initializeAggregatedMetricsTable() {
        TableIdentifier tableId = TableIdentifier.of(namespace, "aggregated_metrics");
        
        if (!catalog.tableExists(tableId)) {
            Schema schema = new Schema(
                    Types.NestedField.required(1, "symbol", Types.StringType.get()),
                    Types.NestedField.required(2, "window_start", Types.TimestampType.withoutZone()),
                    Types.NestedField.required(3, "window_end", Types.TimestampType.withoutZone()),
                    Types.NestedField.required(4, "window_type", Types.StringType.get()), // 1min, 5min, 1hour, 1day
                    Types.NestedField.required(5, "open_price", Types.DoubleType.get()),
                    Types.NestedField.required(6, "close_price", Types.DoubleType.get()),
                    Types.NestedField.required(7, "high_price", Types.DoubleType.get()),
                    Types.NestedField.required(8, "low_price", Types.DoubleType.get()),
                    Types.NestedField.required(9, "total_volume", Types.LongType.get()),
                    Types.NestedField.required(10, "trade_count", Types.IntegerType.get()),
                    Types.NestedField.optional(11, "vwap", Types.DoubleType.get()),
                    Types.NestedField.optional(12, "volatility", Types.DoubleType.get())
            );

            PartitionSpec partitionSpec = PartitionSpec.builderFor(schema)
                    .identity("symbol")
                    .identity("window_type")
                    .day("window_start")
                    .build();

            catalog.createTable(tableId, schema, partitionSpec);
            logger.info("Created aggregated_metrics table");
        }
    }

    public CompletableFuture<Void> writeMarketData(List<MarketDataRecord> records) {
        return CompletableFuture.runAsync(() -> {
            try {
                TableIdentifier tableId = TableIdentifier.of(namespace, "market_data");
                Table table = catalog.loadTable(tableId);
                
                // Create Iceberg records
                List<Record> icebergRecords = records.stream()
                        .map(this::convertToIcebergRecord)
                        .collect(Collectors.toList());

                // Write to table
                DataFile dataFile = writeRecordsToTable(table, icebergRecords);
                
                // Commit transaction
                AppendFiles append = table.newAppend();
                append.appendFile(dataFile);
                append.commit();

                logger.info("Successfully wrote {} market data records", records.size());
            } catch (Exception e) {
                logger.error("Failed to write market data", e);
                throw new RuntimeException("Failed to write market data", e);
            }
        });
    }

    public List<MarketDataRecord> queryMarketData(String symbol, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            TableIdentifier tableId = TableIdentifier.of(namespace, "market_data");
            Table table = catalog.loadTable(tableId);

            // Build query with filters
            CloseableIterable<Record> records = IcebergGenerics.read(table)
                    .where(Expressions.and(
                            Expressions.equal("symbol", symbol),
                            Expressions.greaterThanOrEqual("timestamp", 
                                    startTime.toInstant(ZoneOffset.UTC).toEpochMilli()),
                            Expressions.lessThanOrEqual("timestamp", 
                                    endTime.toInstant(ZoneOffset.UTC).toEpochMilli())
                    ))
                    .build();

            List<MarketDataRecord> result = new ArrayList<>();
            for (Record record : records) {
                result.add(convertFromIcebergRecord(record));
            }

            logger.info("Queried {} market data records for symbol {} between {} and {}", 
                    result.size(), symbol, startTime, endTime);
            
            return result;
        } catch (Exception e) {
            logger.error("Failed to query market data", e);
            throw new RuntimeException("Failed to query market data", e);
        }
    }

    public void optimizeTable(String tableName) {
        try {
            TableIdentifier tableId = TableIdentifier.of(namespace, tableName);
            Table table = catalog.loadTable(tableId);

            // Rewrite data files for better performance
            Actions.forTable(table)
                    .rewriteDataFiles()
                    .targetSizeInBytes(134217728L) // 128MB target file size
                    .execute();

            // Compact manifests
            Actions.forTable(table)
                    .rewriteManifests()
                    .execute();

            logger.info("Optimized table: {}", tableName);
        } catch (Exception e) {
            logger.error("Failed to optimize table: {}", tableName, e);
            throw new RuntimeException("Failed to optimize table", e);
        }
    }

    public void expireSnapshots(String tableName, long olderThanTimestamp) {
        try {
            TableIdentifier tableId = TableIdentifier.of(namespace, tableName);
            Table table = catalog.loadTable(tableId);

            table.expireSnapshots()
                    .expireOlderThan(olderThanTimestamp)
                    .commit();

            logger.info("Expired snapshots older than {} for table: {}", 
                    new Date(olderThanTimestamp), tableName);
        } catch (Exception e) {
            logger.error("Failed to expire snapshots for table: {}", tableName, e);
            throw new RuntimeException("Failed to expire snapshots", e);
        }
    }

    public TableStatistics getTableStatistics(String tableName) {
        try {
            TableIdentifier tableId = TableIdentifier.of(namespace, tableName);
            Table table = catalog.loadTable(tableId);

            long recordCount = 0;
            long dataFileCount = 0;
            long totalSizeBytes = 0;

            for (FileScanTask task : table.newScan().planFiles()) {
                dataFileCount++;
                totalSizeBytes += task.file().fileSizeInBytes();
                recordCount += task.file().recordCount();
            }

            return new TableStatistics(
                    tableName,
                    recordCount,
                    dataFileCount,
                    totalSizeBytes,
                    table.snapshots().spliterator().getExactSizeIfKnown()
            );
        } catch (Exception e) {
            logger.error("Failed to get table statistics for: {}", tableName, e);
            throw new RuntimeException("Failed to get table statistics", e);
        }
    }

    // Helper methods
    private Record convertToIcebergRecord(MarketDataRecord marketData) {
        GenericRecord record = GenericRecord.create(getMarketDataSchema());
        record.setField("symbol", marketData.getSymbol());
        record.setField("price", marketData.getPrice());
        record.setField("volume", marketData.getVolume());
        record.setField("timestamp", marketData.getTimestamp().toInstant(ZoneOffset.UTC).toEpochMilli());
        record.setField("bid", marketData.getBid());
        record.setField("ask", marketData.getAsk());
        record.setField("spread", marketData.getSpread());
        record.setField("event_time", marketData.getEventTime().toInstant(ZoneOffset.UTC).toEpochMilli());
        record.setField("ingestion_time", LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli());
        return record;
    }

    private MarketDataRecord convertFromIcebergRecord(Record record) {
        return new MarketDataRecord(
                record.getField("symbol").toString(),
                (Double) record.getField("price"),
                (Long) record.getField("volume"),
                LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli((Long) record.getField("timestamp")), 
                        ZoneOffset.UTC),
                (Double) record.getField("bid"),
                (Double) record.getField("ask"),
                (Double) record.getField("spread"),
                LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli((Long) record.getField("event_time")), 
                        ZoneOffset.UTC)
        );
    }

    private Schema getMarketDataSchema() {
        // Return the schema for market data table
        return catalog.loadTable(TableIdentifier.of(namespace, "market_data")).schema();
    }

    private DataFile writeRecordsToTable(Table table, List<Record> records) {
        // Implementation would use Iceberg's data writer API
        // This is a simplified placeholder
        throw new UnsupportedOperationException("Data file writing not implemented in this example");
    }

    private String getWarehousePath() {
        // Implementation would return the configured warehouse path
        return "/data/warehouse";
    }

    // Supporting data classes
    public static class MarketDataRecord {
        private final String symbol;
        private final double price;
        private final long volume;
        private final LocalDateTime timestamp;
        private final Double bid;
        private final Double ask;
        private final Double spread;
        private final LocalDateTime eventTime;

        public MarketDataRecord(String symbol, double price, long volume, LocalDateTime timestamp,
                Double bid, Double ask, Double spread, LocalDateTime eventTime) {
            this.symbol = symbol;
            this.price = price;
            this.volume = volume;
            this.timestamp = timestamp;
            this.bid = bid;
            this.ask = ask;
            this.spread = spread;
            this.eventTime = eventTime;
        }

        // Getters
        public String getSymbol() { return symbol; }
        public double getPrice() { return price; }
        public long getVolume() { return volume; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public Double getBid() { return bid; }
        public Double getAsk() { return ask; }
        public Double getSpread() { return spread; }
        public LocalDateTime getEventTime() { return eventTime; }
    }

    public static class TableStatistics {
        private final String tableName;
        private final long recordCount;
        private final long dataFileCount;
        private final long totalSizeBytes;
        private final long snapshotCount;

        public TableStatistics(String tableName, long recordCount, long dataFileCount, 
                long totalSizeBytes, long snapshotCount) {
            this.tableName = tableName;
            this.recordCount = recordCount;
            this.dataFileCount = dataFileCount;
            this.totalSizeBytes = totalSizeBytes;
            this.snapshotCount = snapshotCount;
        }

        // Getters
        public String getTableName() { return tableName; }
        public long getRecordCount() { return recordCount; }
        public long getDataFileCount() { return dataFileCount; }
        public long getTotalSizeBytes() { return totalSizeBytes; }
        public long getSnapshotCount() { return snapshotCount; }
    }
}
```

## Part 3: Real-time Analytics Engine

### Complex Event Processing Service

```java
package com.securetrading.data.analytics.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@Service
public class ComplexEventProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(ComplexEventProcessingService.class);

    @Autowired
    private RealTimeAnalyticsEngine analyticsEngine;

    @Autowired
    private PatternDetectionService patternDetectionService;

    @Autowired
    private AlertingService alertingService;

    // Event windows for pattern detection
    private final Map<String, Queue<MarketEvent>> eventWindows = new ConcurrentHashMap<>();
    
    // Pattern matching rules
    private final List<PatternRule> patternRules = new ArrayList<>();

    public void initializePatternRules() {
        // Price momentum pattern
        patternRules.add(new PatternRule(
                "price_momentum",
                "Detect sustained price momentum",
                this::detectPriceMomentumPattern,
                Duration.ofMinutes(5),
                0.8
        ));

        // Volume spike pattern
        patternRules.add(new PatternRule(
                "volume_spike",
                "Detect unusual volume spikes",
                this::detectVolumeSpikePattern,
                Duration.ofMinutes(2),
                0.9
        ));

        // Mean reversion pattern
        patternRules.add(new PatternRule(
                "mean_reversion",
                "Detect mean reversion opportunities",
                this::detectMeanReversionPattern,
                Duration.ofMinutes(10),
                0.7
        ));

        // Market correlation break pattern
        patternRules.add(new PatternRule(
                "correlation_break",
                "Detect breaks in market correlations",
                this::detectCorrelationBreakPattern,
                Duration.ofMinutes(15),
                0.75
        ));

        logger.info("Initialized {} pattern rules", patternRules.size());
    }

    public void processMarketEvent(MarketEvent event) {
        try {
            // Add event to window
            String symbol = event.getSymbol();
            eventWindows.computeIfAbsent(symbol, k -> new ConcurrentLinkedQueue<>()).offer(event);
            
            // Clean old events
            cleanEventWindow(symbol);
            
            // Process patterns for this symbol
            processPatterns(symbol);
            
            // Update real-time analytics
            analyticsEngine.updateMetrics(event);
            
        } catch (Exception e) {
            logger.error("Failed to process market event for symbol: {}", event.getSymbol(), e);
        }
    }

    private void cleanEventWindow(String symbol) {
        Queue<MarketEvent> window = eventWindows.get(symbol);
        if (window == null) return;

        LocalDateTime cutoff = LocalDateTime.now().minus(Duration.ofMinutes(30));
        window.removeIf(event -> event.getTimestamp().isBefore(cutoff));
    }

    private void processPatterns(String symbol) {
        Queue<MarketEvent> window = eventWindows.get(symbol);
        if (window == null || window.size() < 5) return;

        List<MarketEvent> events = new ArrayList<>(window);
        
        for (PatternRule rule : patternRules) {
            try {
                PatternMatchResult result = rule.getDetector().apply(events);
                if (result.isMatched() && result.getConfidence() >= rule.getConfidenceThreshold()) {
                    handlePatternMatch(symbol, rule, result);
                }
            } catch (Exception e) {
                logger.error("Failed to process pattern rule: {} for symbol: {}", 
                        rule.getName(), symbol, e);
            }
        }
    }

    private void handlePatternMatch(String symbol, PatternRule rule, PatternMatchResult result) {
        logger.info("Pattern matched: {} for symbol: {} with confidence: {}", 
                rule.getName(), symbol, result.getConfidence());

        // Create pattern event
        PatternEvent patternEvent = new PatternEvent(
                rule.getName(),
                symbol,
                result.getConfidence(),
                result.getDescription(),
                result.getMetrics(),
                LocalDateTime.now()
        );

        // Generate alerts if needed
        if (result.getConfidence() > 0.9) {
            AlertInfo alert = new AlertInfo(
                    AlertType.PATTERN_DETECTED,
                    AlertSeverity.HIGH,
                    symbol,
                    String.format("High confidence pattern detected: %s (%.2f)", 
                            rule.getName(), result.getConfidence())
            );
            alertingService.sendAlert(alert);
        }

        // Update analytics
        analyticsEngine.recordPatternEvent(patternEvent);
    }

    // Pattern detection methods
    private PatternMatchResult detectPriceMomentumPattern(List<MarketEvent> events) {
        if (events.size() < 5) {
            return PatternMatchResult.noMatch();
        }

        // Sort events by timestamp
        List<MarketEvent> sortedEvents = events.stream()
                .sorted(Comparator.comparing(MarketEvent::getTimestamp))
                .collect(Collectors.toList());

        // Calculate momentum indicators
        List<Double> prices = sortedEvents.stream()
                .map(MarketEvent::getPrice)
                .collect(Collectors.toList());

        // Check for consistent price direction
        int upMoves = 0;
        int downMoves = 0;
        
        for (int i = 1; i < prices.size(); i++) {
            double change = (prices.get(i) - prices.get(i-1)) / prices.get(i-1);
            if (change > 0.001) { // 0.1% threshold
                upMoves++;
            } else if (change < -0.001) {
                downMoves++;
            }
        }

        // Calculate momentum strength
        double totalMoves = upMoves + downMoves;
        if (totalMoves == 0) {
            return PatternMatchResult.noMatch();
        }

        double momentum = Math.max(upMoves, downMoves) / totalMoves;
        
        if (momentum >= 0.8) { // 80% of moves in same direction
            double totalChange = (prices.get(prices.size()-1) - prices.get(0)) / prices.get(0);
            
            Map<String, Double> metrics = new HashMap<>();
            metrics.put("momentum_ratio", momentum);
            metrics.put("total_price_change", totalChange);
            metrics.put("up_moves", (double) upMoves);
            metrics.put("down_moves", (double) downMoves);

            return new PatternMatchResult(
                    true,
                    momentum,
                    String.format("Strong momentum detected: %.1f%% moves in same direction", momentum * 100),
                    metrics
            );
        }

        return PatternMatchResult.noMatch();
    }

    private PatternMatchResult detectVolumeSpikePattern(List<MarketEvent> events) {
        if (events.size() < 10) {
            return PatternMatchResult.noMatch();
        }

        List<Long> volumes = events.stream()
                .map(MarketEvent::getVolume)
                .collect(Collectors.toList());

        // Calculate average volume (excluding recent spike)
        double avgVolume = volumes.subList(0, volumes.size() - 3).stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        // Check recent volume spike
        double recentVolume = volumes.subList(volumes.size() - 3, volumes.size()).stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        if (avgVolume > 0) {
            double volumeRatio = recentVolume / avgVolume;
            
            if (volumeRatio >= 3.0) { // 3x average volume
                Map<String, Double> metrics = new HashMap<>();
                metrics.put("volume_ratio", volumeRatio);
                metrics.put("average_volume", avgVolume);
                metrics.put("recent_volume", recentVolume);

                double confidence = Math.min(volumeRatio / 5.0, 1.0); // Scale to 0-1
                
                return new PatternMatchResult(
                        true,
                        confidence,
                        String.format("Volume spike detected: %.1fx average volume", volumeRatio),
                        metrics
                );
            }
        }

        return PatternMatchResult.noMatch();
    }

    private PatternMatchResult detectMeanReversionPattern(List<MarketEvent> events) {
        if (events.size() < 20) {
            return PatternMatchResult.noMatch();
        }

        List<Double> prices = events.stream()
                .sorted(Comparator.comparing(MarketEvent::getTimestamp))
                .map(MarketEvent::getPrice)
                .collect(Collectors.toList());

        // Calculate moving average
        int windowSize = Math.min(20, prices.size());
        double movingAverage = prices.subList(prices.size() - windowSize, prices.size()).stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double currentPrice = prices.get(prices.size() - 1);
        double deviation = Math.abs(currentPrice - movingAverage) / movingAverage;

        // Check for significant deviation followed by reversion signals
        if (deviation > 0.05) { // 5% deviation from moving average
            // Look for reversion signals in recent prices
            List<Double> recentPrices = prices.subList(Math.max(0, prices.size() - 5), prices.size());
            boolean reversionSignal = false;
            
            if (currentPrice > movingAverage) {
                // Price above average, look for downward movement
                reversionSignal = recentPrices.get(0) > recentPrices.get(recentPrices.size() - 1);
            } else {
                // Price below average, look for upward movement
                reversionSignal = recentPrices.get(0) < recentPrices.get(recentPrices.size() - 1);
            }

            if (reversionSignal) {
                Map<String, Double> metrics = new HashMap<>();
                metrics.put("price_deviation", deviation);
                metrics.put("moving_average", movingAverage);
                metrics.put("current_price", currentPrice);

                double confidence = Math.min(deviation * 10, 1.0); // Scale deviation to confidence
                
                return new PatternMatchResult(
                        true,
                        confidence,
                        String.format("Mean reversion pattern: %.1f%% deviation from MA", deviation * 100),
                        metrics
                );
            }
        }

        return PatternMatchResult.noMatch();
    }

    private PatternMatchResult detectCorrelationBreakPattern(List<MarketEvent> events) {
        // This would require market-wide data for correlation analysis
        // For simplicity, returning no match in this example
        return PatternMatchResult.noMatch();
    }

    // Supporting classes
    @FunctionalInterface
    public interface PatternDetector {
        PatternMatchResult apply(List<MarketEvent> events);
    }

    public static class PatternRule {
        private final String name;
        private final String description;
        private final PatternDetector detector;
        private final Duration windowSize;
        private final double confidenceThreshold;

        public PatternRule(String name, String description, PatternDetector detector, 
                Duration windowSize, double confidenceThreshold) {
            this.name = name;
            this.description = description;
            this.detector = detector;
            this.windowSize = windowSize;
            this.confidenceThreshold = confidenceThreshold;
        }

        // Getters
        public String getName() { return name; }
        public String getDescription() { return description; }
        public PatternDetector getDetector() { return detector; }
        public Duration getWindowSize() { return windowSize; }
        public double getConfidenceThreshold() { return confidenceThreshold; }
    }

    public static class PatternMatchResult {
        private final boolean matched;
        private final double confidence;
        private final String description;
        private final Map<String, Double> metrics;

        public PatternMatchResult(boolean matched, double confidence, String description, 
                Map<String, Double> metrics) {
            this.matched = matched;
            this.confidence = confidence;
            this.description = description;
            this.metrics = metrics != null ? new HashMap<>(metrics) : new HashMap<>();
        }

        public static PatternMatchResult noMatch() {
            return new PatternMatchResult(false, 0.0, "No pattern detected", new HashMap<>());
        }

        // Getters
        public boolean isMatched() { return matched; }
        public double getConfidence() { return confidence; }
        public String getDescription() { return description; }
        public Map<String, Double> getMetrics() { return new HashMap<>(metrics); }
    }

    public static class PatternEvent {
        private final String patternName;
        private final String symbol;
        private final double confidence;
        private final String description;
        private final Map<String, Double> metrics;
        private final LocalDateTime timestamp;

        public PatternEvent(String patternName, String symbol, double confidence, 
                String description, Map<String, Double> metrics, LocalDateTime timestamp) {
            this.patternName = patternName;
            this.symbol = symbol;
            this.confidence = confidence;
            this.description = description;
            this.metrics = new HashMap<>(metrics);
            this.timestamp = timestamp;
        }

        // Getters
        public String getPatternName() { return patternName; }
        public String getSymbol() { return symbol; }
        public double getConfidence() { return confidence; }
        public String getDescription() { return description; }
        public Map<String, Double> getMetrics() { return new HashMap<>(metrics); }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    public static class MarketEvent {
        private final String symbol;
        private final double price;
        private final long volume;
        private final LocalDateTime timestamp;

        public MarketEvent(String symbol, double price, long volume, LocalDateTime timestamp) {
            this.symbol = symbol;
            this.price = price;
            this.volume = volume;
            this.timestamp = timestamp;
        }

        // Getters
        public String getSymbol() { return symbol; }
        public double getPrice() { return price; }
        public long getVolume() { return volume; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    public enum AlertType { PATTERN_DETECTED, ANOMALY_DETECTED, THRESHOLD_EXCEEDED }
    public enum AlertSeverity { LOW, MEDIUM, HIGH, CRITICAL }

    public static class AlertInfo {
        private final AlertType type;
        private final AlertSeverity severity;
        private final String symbol;
        private final String message;
        private final LocalDateTime timestamp;

        public AlertInfo(AlertType type, AlertSeverity severity, String symbol, String message) {
            this.type = type;
            this.severity = severity;
            this.symbol = symbol;
            this.message = message;
            this.timestamp = LocalDateTime.now();
        }

        // Getters
        public AlertType getType() { return type; }
        public AlertSeverity getSeverity() { return severity; }
        public String getSymbol() { return symbol; }
        public String getMessage() { return message; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}
```

## Practical Exercises

### Exercise 1: Kafka Streams Advanced Processing
Build a multi-topology Kafka Streams application that processes market data with custom processors for complex calculations and state management.

### Exercise 2: Data Lake ETL Pipeline
Create a comprehensive ETL pipeline that ingests streaming data into the data lake with proper partitioning, compression, and optimization strategies.

### Exercise 3: Real-time OLAP Cube
Implement a real-time OLAP cube that provides sub-second aggregation queries across multiple dimensions (time, symbol, market sector).

### Exercise 4: Event Sourcing Implementation
Build an event sourcing system that captures all market events and provides point-in-time reconstruction capabilities.

### Exercise 5: Data Quality Monitoring
Create a comprehensive data quality monitoring system that detects and alerts on data issues, missing data, and anomalies in real-time.

## Performance Considerations

### Stream Processing Optimization
- **Parallelism**: Configure optimal number of stream threads based on partition count
- **State Store Tuning**: Optimize RocksDB settings for better performance
- **Memory Management**: Configure heap and off-heap memory for optimal throughput
- **Serialization**: Use efficient serialization formats like Avro or Protobuf

### Data Lake Performance
- **File Sizing**: Optimize file sizes for query performance (128-512MB per file)
- **Partitioning Strategy**: Use appropriate partitioning for query patterns
- **Compaction**: Regular table maintenance and optimization
- **Caching**: Implement query result caching for frequently accessed data

## Data Engineering Best Practices
- Implement comprehensive data lineage tracking
- Use schema evolution and versioning strategies
- Implement proper error handling and dead letter queues
- Monitor data quality and freshness metrics
- Use proper security and access control patterns
- Implement data retention and archival policies
- Create comprehensive monitoring and alerting systems
- Use proper backup and disaster recovery strategies

## Summary

Day 110 covered advanced data engineering concepts essential for modern data-driven applications:

1. **Apache Kafka Streams Processing**: High-performance stream processing with complex event processing, windowing, and state management
2. **Data Lake Architecture**: Apache Iceberg integration with schema evolution, time travel, and optimized storage strategies
3. **Real-time Analytics**: Complex event processing with pattern detection, anomaly detection, and real-time aggregations
4. **Performance Optimization**: Stream processing tuning, data lake optimization, and query performance strategies
5. **Data Quality & Monitoring**: Comprehensive monitoring, alerting, and data quality assurance systems

These implementations provide the foundation for building scalable, high-performance data engineering systems that can handle massive volumes of real-time data while maintaining data quality and providing fast analytical capabilities.