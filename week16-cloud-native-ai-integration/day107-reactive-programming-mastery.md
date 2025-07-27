# Day 107: Reactive Programming Mastery - WebFlux, R2DBC & Reactive Streams

## Learning Objectives
- Master reactive programming principles with Project Reactor
- Implement non-blocking applications with Spring WebFlux
- Design reactive data access with R2DBC
- Build end-to-end reactive microservices
- Optimize performance with reactive backpressure handling

## Topics Covered
1. Reactive Programming Fundamentals with Project Reactor
2. Spring WebFlux Framework Deep Dive
3. Reactive Data Access with R2DBC
4. Advanced Reactive Patterns and Operators
5. Reactive Security and Error Handling
6. Performance Optimization and Backpressure Management
7. Real-World Reactive Trading System Implementation

## 1. Reactive Programming Fundamentals

### Project Reactor Core Concepts

```java
// Advanced Reactor operators and patterns
@Service
public class ReactiveMarketDataService {
    
    private final WebClient marketDataClient;
    private final RedisReactiveTemplate<String, String> redisTemplate;
    private final Logger logger = LoggerFactory.getLogger(ReactiveMarketDataService.class);
    
    public ReactiveMarketDataService(WebClient.Builder webClientBuilder,
                                   RedisReactiveTemplate<String, String> redisTemplate) {
        this.marketDataClient = webClientBuilder
                .baseUrl("https://api.marketdata.com")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.redisTemplate = redisTemplate;
    }
    
    // Reactive stream with backpressure handling
    public Flux<MarketDataEvent> getMarketDataStream(Set<String> symbols) {
        return Flux.fromIterable(symbols)
                .flatMap(this::createSymbolStream, 10) // Concurrency limit
                .onBackpressureBuffer(1000, BufferOverflowStrategy.DROP_OLDEST)
                .share() // Hot stream for multiple subscribers
                .doOnNext(event -> logger.debug("Market data event: {}", event))
                .doOnError(error -> logger.error("Market data stream error", error))
                .onErrorResume(this::handleMarketDataError);
    }
    
    private Flux<MarketDataEvent> createSymbolStream(String symbol) {
        return Flux.interval(Duration.ofSeconds(1))
                .flatMap(tick -> fetchMarketData(symbol))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(throwable -> throwable instanceof WebClientException))
                .cache(Duration.ofSeconds(5)) // Cache for 5 seconds
                .takeUntilOther(getStopSignal(symbol));
    }
    
    private Mono<MarketDataEvent> fetchMarketData(String symbol) {
        return redisTemplate.opsForValue()
                .get("market:" + symbol)
                .cast(String.class)
                .flatMap(cached -> Mono.just(parseMarketData(cached, symbol)))
                .switchIfEmpty(fetchFromExternalApi(symbol)
                        .flatMap(data -> cacheMarketData(symbol, data)))
                .timeout(Duration.ofSeconds(5))
                .onErrorMap(TimeoutException.class, 
                        ex -> new MarketDataException("Timeout fetching data for " + symbol, ex));
    }
    
    private Mono<MarketDataEvent> fetchFromExternalApi(String symbol) {
        return marketDataClient.get()
                .uri("/quote/{symbol}", symbol)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        response -> Mono.error(new MarketDataException("Invalid symbol: " + symbol)))
                .onStatus(HttpStatusCode::is5xxServerError,
                        response -> Mono.error(new MarketDataException("Server error for: " + symbol)))
                .bodyToMono(ExternalMarketData.class)
                .map(external -> convertToMarketDataEvent(external, symbol))
                .doOnNext(event -> logger.debug("Fetched from API: {}", event));
    }
    
    private Mono<MarketDataEvent> cacheMarketData(String symbol, MarketDataEvent data) {
        return redisTemplate.opsForValue()
                .set("market:" + symbol, serializeMarketData(data), Duration.ofSeconds(60))
                .thenReturn(data);
    }
    
    // Complex reactive transformation with windowing
    public Flux<TechnicalIndicator> calculateMovingAverage(String symbol, Duration window, int periods) {
        return getMarketDataStream(Set.of(symbol))
                .filter(event -> event.getSymbol().equals(symbol))
                .map(MarketDataEvent::getPrice)
                .window(Duration.ofSeconds(window.getSeconds()))
                .flatMap(priceWindow -> priceWindow
                        .takeLast(periods)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .map(sum -> sum.divide(BigDecimal.valueOf(periods), 4, RoundingMode.HALF_UP))
                        .map(average -> new TechnicalIndicator(symbol, "SMA_" + periods, average, Instant.now())))
                .distinctUntilChanged(TechnicalIndicator::getValue);
    }
    
    // Reactive composition with multiple data sources
    public Mono<EnrichedMarketData> getEnrichedMarketData(String symbol) {
        Mono<MarketDataEvent> marketData = fetchMarketData(symbol);
        Mono<CompanyInfo> companyInfo = fetchCompanyInfo(symbol);
        Mono<List<NewsItem>> news = fetchNewsForSymbol(symbol);
        Mono<TechnicalIndicators> technicalData = fetchTechnicalIndicators(symbol);
        
        return Mono.zip(marketData, companyInfo, news, technicalData)
                .map(tuple -> new EnrichedMarketData(
                        tuple.getT1(), // MarketDataEvent
                        tuple.getT2(), // CompanyInfo
                        tuple.getT3(), // List<NewsItem>
                        tuple.getT4()  // TechnicalIndicators
                ))
                .timeout(Duration.ofSeconds(10))
                .doOnSuccess(enriched -> logger.info("Enriched data created for {}", symbol));
    }
    
    // Advanced error handling with retry strategies
    private Mono<CompanyInfo> fetchCompanyInfo(String symbol) {
        return marketDataClient.get()
                .uri("/company/{symbol}", symbol)
                .retrieve()
                .bodyToMono(CompanyInfo.class)
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2))
                        .filter(throwable -> !(throwable instanceof WebClientResponseException.NotFound)))
                .onErrorReturn(CompanyInfo.empty(symbol));
    }
    
    private Mono<List<NewsItem>> fetchNewsForSymbol(String symbol) {
        return marketDataClient.get()
                .uri("/news/{symbol}?limit=10", symbol)
                .retrieve()
                .bodyToFlux(NewsItem.class)
                .collectList()
                .timeout(Duration.ofSeconds(5))
                .onErrorReturn(Collections.emptyList());
    }
    
    private Mono<TechnicalIndicators> fetchTechnicalIndicators(String symbol) {
        return marketDataClient.get()
                .uri("/technical/{symbol}", symbol)
                .retrieve()
                .bodyToMono(TechnicalIndicators.class)
                .onErrorReturn(TechnicalIndicators.empty(symbol));
    }
    
    // Custom operators and transformations
    public Flux<PriceAlert> monitorPriceAlerts(String symbol, BigDecimal threshold, AlertType type) {
        return getMarketDataStream(Set.of(symbol))
                .filter(event -> event.getSymbol().equals(symbol))
                .compose(this::detectPriceBreakouts)
                .filter(event -> shouldTriggerAlert(event, threshold, type))
                .map(event -> new PriceAlert(symbol, event.getPrice(), threshold, type, Instant.now()))
                .distinctUntilChanged(alert -> alert.getPrice().setScale(2, RoundingMode.HALF_UP));
    }
    
    private Flux<MarketDataEvent> detectPriceBreakouts(Flux<MarketDataEvent> source) {
        return source
                .scan(new PriceMovement(), (previous, current) -> {
                    BigDecimal change = current.getPrice().subtract(previous.getCurrentPrice());
                    BigDecimal percentChange = previous.getCurrentPrice().compareTo(BigDecimal.ZERO) != 0 ?
                            change.divide(previous.getCurrentPrice(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) :
                            BigDecimal.ZERO;
                    
                    return new PriceMovement(current.getPrice(), change, percentChange, current.getTimestamp());
                })
                .skip(1) // Skip initial empty state
                .filter(movement -> movement.getPercentChange().abs().compareTo(BigDecimal.valueOf(2.0)) > 0) // 2% threshold
                .map(movement -> new MarketDataEvent(
                        source.blockFirst().getSymbol(), // Note: This is simplified for demo
                        movement.getCurrentPrice(),
                        movement.getCurrentPrice().longValue(),
                        movement.getTimestamp()
                ));
    }
    
    private boolean shouldTriggerAlert(MarketDataEvent event, BigDecimal threshold, AlertType type) {
        return switch (type) {
            case ABOVE -> event.getPrice().compareTo(threshold) > 0;
            case BELOW -> event.getPrice().compareTo(threshold) < 0;
            case CHANGE_PERCENT -> {
                // This would require comparison with previous price
                yield false; // Simplified for demo
            }
        };
    }
    
    private Flux<MarketDataEvent> handleMarketDataError(Throwable error) {
        logger.error("Market data error, providing fallback", error);
        return Flux.empty(); // Or provide cached/fallback data
    }
    
    private Mono<Void> getStopSignal(String symbol) {
        // This would be connected to a shutdown or control signal
        return Mono.never(); // Never stop for demo
    }
    
    // Helper methods
    private MarketDataEvent parseMarketData(String cached, String symbol) {
        // Parse cached data
        try {
            String[] parts = cached.split(",");
            return new MarketDataEvent(
                    symbol,
                    new BigDecimal(parts[0]),
                    Long.parseLong(parts[1]),
                    Instant.parse(parts[2])
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse cached market data", e);
        }
    }
    
    private MarketDataEvent convertToMarketDataEvent(ExternalMarketData external, String symbol) {
        return new MarketDataEvent(
                symbol,
                external.getPrice(),
                external.getVolume(),
                Instant.now()
        );
    }
    
    private String serializeMarketData(MarketDataEvent data) {
        return String.format("%s,%d,%s", 
                data.getPrice().toString(), 
                data.getVolume(), 
                data.getTimestamp().toString());
    }
}

// Domain models
public class MarketDataEvent {
    private final String symbol;
    private final BigDecimal price;
    private final Long volume;
    private final Instant timestamp;
    
    public MarketDataEvent(String symbol, BigDecimal price, Long volume, Instant timestamp) {
        this.symbol = symbol;
        this.price = price;
        this.volume = volume;
        this.timestamp = timestamp;
    }
    
    // Getters
    public String getSymbol() { return symbol; }
    public BigDecimal getPrice() { return price; }
    public Long getVolume() { return volume; }
    public Instant getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return String.format("MarketDataEvent{symbol='%s', price=%s, volume=%d, timestamp=%s}",
                symbol, price, volume, timestamp);
    }
}

public class TechnicalIndicator {
    private final String symbol;
    private final String indicator;
    private final BigDecimal value;
    private final Instant timestamp;
    
    public TechnicalIndicator(String symbol, String indicator, BigDecimal value, Instant timestamp) {
        this.symbol = symbol;
        this.indicator = indicator;
        this.value = value;
        this.timestamp = timestamp;
    }
    
    // Getters
    public String getSymbol() { return symbol; }
    public String getIndicator() { return indicator; }
    public BigDecimal getValue() { return value; }
    public Instant getTimestamp() { return timestamp; }
}

public class PriceMovement {
    private final BigDecimal currentPrice;
    private final BigDecimal change;
    private final BigDecimal percentChange;
    private final Instant timestamp;
    
    public PriceMovement() {
        this(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, Instant.now());
    }
    
    public PriceMovement(BigDecimal currentPrice, BigDecimal change, BigDecimal percentChange, Instant timestamp) {
        this.currentPrice = currentPrice;
        this.change = change;
        this.percentChange = percentChange;
        this.timestamp = timestamp;
    }
    
    // Getters
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public BigDecimal getChange() { return change; }
    public BigDecimal getPercentChange() { return percentChange; }
    public Instant getTimestamp() { return timestamp; }
}

public class PriceAlert {
    private final String symbol;
    private final BigDecimal currentPrice;
    private final BigDecimal threshold;
    private final AlertType type;
    private final Instant timestamp;
    
    public PriceAlert(String symbol, BigDecimal currentPrice, BigDecimal threshold, 
                     AlertType type, Instant timestamp) {
        this.symbol = symbol;
        this.currentPrice = currentPrice;
        this.threshold = threshold;
        this.type = type;
        this.timestamp = timestamp;
    }
    
    // Getters
    public String getSymbol() { return symbol; }
    public BigDecimal getPrice() { return currentPrice; }
    public BigDecimal getThreshold() { return threshold; }
    public AlertType getType() { return type; }
    public Instant getTimestamp() { return timestamp; }
}

public enum AlertType {
    ABOVE, BELOW, CHANGE_PERCENT
}

// External API models
public class ExternalMarketData {
    private BigDecimal price;
    private Long volume;
    private String currency;
    
    // Getters and setters
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public Long getVolume() { return volume; }
    public void setVolume(Long volume) { this.volume = volume; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}

public class CompanyInfo {
    private String symbol;
    private String name;
    private String sector;
    private BigDecimal marketCap;
    
    public static CompanyInfo empty(String symbol) {
        CompanyInfo info = new CompanyInfo();
        info.symbol = symbol;
        info.name = "Unknown";
        info.sector = "Unknown";
        info.marketCap = BigDecimal.ZERO;
        return info;
    }
    
    // Getters and setters
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }
    
    public BigDecimal getMarketCap() { return marketCap; }
    public void setMarketCap(BigDecimal marketCap) { this.marketCap = marketCap; }
}

public class NewsItem {
    private String title;
    private String summary;
    private Instant publishedAt;
    private String source;
    
    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}

public class TechnicalIndicators {
    private String symbol;
    private BigDecimal sma20;
    private BigDecimal sma50;
    private BigDecimal rsi;
    private BigDecimal macd;
    
    public static TechnicalIndicators empty(String symbol) {
        TechnicalIndicators indicators = new TechnicalIndicators();
        indicators.symbol = symbol;
        indicators.sma20 = BigDecimal.ZERO;
        indicators.sma50 = BigDecimal.ZERO;
        indicators.rsi = BigDecimal.valueOf(50);
        indicators.macd = BigDecimal.ZERO;
        return indicators;
    }
    
    // Getters and setters
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    
    public BigDecimal getSma20() { return sma20; }
    public void setSma20(BigDecimal sma20) { this.sma20 = sma20; }
    
    public BigDecimal getSma50() { return sma50; }
    public void setSma50(BigDecimal sma50) { this.sma50 = sma50; }
    
    public BigDecimal getRsi() { return rsi; }
    public void setRsi(BigDecimal rsi) { this.rsi = rsi; }
    
    public BigDecimal getMacd() { return macd; }
    public void setMacd(BigDecimal macd) { this.macd = macd; }
}

public class EnrichedMarketData {
    private final MarketDataEvent marketData;
    private final CompanyInfo companyInfo;
    private final List<NewsItem> news;
    private final TechnicalIndicators technicalIndicators;
    
    public EnrichedMarketData(MarketDataEvent marketData, CompanyInfo companyInfo,
                             List<NewsItem> news, TechnicalIndicators technicalIndicators) {
        this.marketData = marketData;
        this.companyInfo = companyInfo;
        this.news = news;
        this.technicalIndicators = technicalIndicators;
    }
    
    // Getters
    public MarketDataEvent getMarketData() { return marketData; }
    public CompanyInfo getCompanyInfo() { return companyInfo; }
    public List<NewsItem> getNews() { return news; }
    public TechnicalIndicators getTechnicalIndicators() { return technicalIndicators; }
}

// Custom exception
public class MarketDataException extends RuntimeException {
    public MarketDataException(String message) {
        super(message);
    }
    
    public MarketDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

## 2. Spring WebFlux Framework

### Reactive Web Controllers

```java
// Advanced WebFlux controller with reactive patterns
@RestController
@RequestMapping("/api/v1/reactive/trading")
@Validated
public class ReactiveeTradingController {
    
    private final ReactiveTradingService tradingService;
    private final ReactiveMarketDataService marketDataService;
    private final ReactiveValidationService validationService;
    
    public ReactiveTradingController(ReactiveTradingService tradingService,
                                   ReactiveMarketDataService marketDataService,
                                   ReactiveValidationService validationService) {
        this.tradingService = tradingService;
        this.marketDataService = marketDataService;
        this.validationService = validationService;
    }
    
    // Server-Sent Events for real-time market data
    @GetMapping(value = "/market-data/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<MarketDataDto>> streamMarketData(
            @RequestParam Set<String> symbols,
            @RequestParam(defaultValue = "false") boolean includeTechnicals) {
        
        return marketDataService.getMarketDataStream(symbols)
                .map(this::convertToDto)
                .flatMap(dto -> includeTechnicals ? 
                        enrichWithTechnicals(dto) : Mono.just(dto))
                .map(dto -> ServerSentEvent.<MarketDataDto>builder()
                        .id(dto.getSymbol() + "-" + dto.getTimestamp().toEpochMilli())
                        .event("market-data")
                        .data(dto)
                        .build())
                .doOnNext(sse -> log.debug("Streaming market data: {}", sse.data()))
                .doOnError(error -> log.error("Error in market data stream", error))
                .onErrorResume(error -> Flux.empty());
    }
    
    // Reactive order placement with validation
    @PostMapping("/orders")
    public Mono<ResponseEntity<OrderResponseDto>> placeOrder(
            @Valid @RequestBody Mono<PlaceOrderRequestDto> orderRequest,
            ServerHttpRequest request) {
        
        return orderRequest
                .flatMap(validationService::validateOrderRequest)
                .flatMap(tradingService::placeOrder)
                .map(this::convertToResponseDto)
                .map(dto -> ResponseEntity.status(HttpStatus.CREATED)
                        .header("Location", "/api/v1/reactive/trading/orders/" + dto.getOrderId())
                        .body(dto))
                .onErrorResume(ValidationException.class, ex -> 
                        Mono.just(ResponseEntity.badRequest().build()))
                .onErrorResume(InsufficientFundsException.class, ex ->
                        Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build()))
                .timeout(Duration.ofSeconds(10))
                .doOnSuccess(response -> log.info("Order placed successfully"))
                .doOnError(error -> log.error("Error placing order", error));
    }
    
    // Reactive batch order processing
    @PostMapping("/orders/batch")
    public Flux<OrderResponseDto> placeBatchOrders(
            @RequestBody Flux<PlaceOrderRequestDto> orderRequests,
            @RequestParam(defaultValue = "10") int concurrency) {
        
        return orderRequests
                .flatMap(validationService::validateOrderRequest, concurrency)
                .flatMap(tradingService::placeOrder, concurrency)
                .map(this::convertToResponseDto)
                .onErrorContinue((error, item) -> 
                        log.error("Error processing batch order: {}", item, error))
                .doOnNext(response -> log.debug("Batch order processed: {}", response.getOrderId()));
    }
    
    // WebSocket endpoint for real-time order updates
    @MessageMapping("/orders/subscribe")
    public Flux<OrderUpdateDto> subscribeToOrderUpdates(String traderId) {
        return tradingService.getOrderUpdatesStream(traderId)
                .map(this::convertToOrderUpdateDto)
                .doOnSubscribe(subscription -> log.info("Trader {} subscribed to order updates", traderId))
                .doOnCancel(() -> log.info("Trader {} unsubscribed from order updates", traderId));
    }
    
    // Reactive portfolio aggregation
    @GetMapping("/portfolio/{traderId}")
    public Mono<ResponseEntity<PortfolioDto>> getPortfolio(@PathVariable String traderId) {
        return tradingService.getPortfolio(traderId)
                .flatMap(this::enrichPortfolioWithMarketData)
                .map(this::convertToPortfolioDto)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .timeout(Duration.ofSeconds(5));
    }
    
    // Complex aggregation with multiple reactive streams
    @GetMapping("/analytics/dashboard/{traderId}")
    public Mono<ResponseEntity<TradingDashboardDto>> getTradingDashboard(@PathVariable String traderId) {
        
        Mono<PortfolioDto> portfolio = getPortfolio(traderId).map(ResponseEntity::getBody);
        Mono<List<OrderDto>> recentOrders = tradingService.getRecentOrders(traderId, 10)
                .map(this::convertToOrderDto)
                .collectList();
        Mono<TradingStatisticsDto> statistics = tradingService.getTradingStatistics(traderId)
                .map(this::convertToStatisticsDto);
        Mono<List<PriceAlertDto>> activeAlerts = tradingService.getActiveAlerts(traderId)
                .map(this::convertToAlertDto)
                .collectList();
        
        return Mono.zip(portfolio, recentOrders, statistics, activeAlerts)
                .map(tuple -> new TradingDashboardDto(
                        tuple.getT1(), // portfolio
                        tuple.getT2(), // recentOrders
                        tuple.getT3(), // statistics
                        tuple.getT4()  // activeAlerts
                ))
                .map(ResponseEntity::ok)
                .timeout(Duration.ofSeconds(15));
    }
    
    // Reactive search with filtering and pagination
    @GetMapping("/orders/search")
    public Flux<OrderDto> searchOrders(
            @RequestParam String traderId,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        OrderSearchCriteria criteria = new OrderSearchCriteria(traderId, symbol, status);
        
        return tradingService.searchOrders(criteria)
                .skip((long) page * size)
                .take(size)
                .map(this::convertToOrderDto);
    }
    
    // Helper methods for data transformation
    private MarketDataDto convertToDto(MarketDataEvent event) {
        return new MarketDataDto(
                event.getSymbol(),
                event.getPrice(),
                event.getVolume(),
                event.getTimestamp()
        );
    }
    
    private Mono<MarketDataDto> enrichWithTechnicals(MarketDataDto dto) {
        return marketDataService.fetchTechnicalIndicators(dto.getSymbol())
                .map(indicators -> {
                    dto.setTechnicalIndicators(indicators);
                    return dto;
                })
                .defaultIfEmpty(dto);
    }
    
    private OrderResponseDto convertToResponseDto(Order order) {
        return new OrderResponseDto(
                order.getId(),
                order.getStatus().name(),
                "Order processed successfully",
                order.getCreatedAt()
        );
    }
    
    private OrderUpdateDto convertToOrderUpdateDto(OrderUpdate update) {
        return new OrderUpdateDto(
                update.getOrderId(),
                update.getStatus(),
                update.getFilledQuantity(),
                update.getTimestamp()
        );
    }
    
    private PortfolioDto convertToPortfolioDto(Portfolio portfolio) {
        return new PortfolioDto(
                portfolio.getTraderId(),
                portfolio.getPositions().stream()
                        .collect(Collectors.toMap(
                                Position::getSymbol,
                                this::convertToPositionDto
                        )),
                portfolio.getTotalValue(),
                portfolio.getLastUpdated()
        );
    }
    
    private PositionDto convertToPositionDto(Position position) {
        return new PositionDto(
                position.getSymbol(),
                position.getQuantity(),
                position.getAveragePrice(),
                position.getCurrentPrice(),
                position.getUnrealizedPnL()
        );
    }
    
    private Mono<Portfolio> enrichPortfolioWithMarketData(Portfolio portfolio) {
        return Flux.fromIterable(portfolio.getPositions())
                .flatMap(position -> marketDataService.fetchMarketData(position.getSymbol())
                        .map(marketData -> position.withCurrentPrice(marketData.getPrice()))
                        .onErrorReturn(position))
                .collectList()
                .map(enrichedPositions -> portfolio.withPositions(enrichedPositions));
    }
    
    private OrderDto convertToOrderDto(Order order) {
        return new OrderDto(
                order.getId(),
                order.getTraderId(),
                order.getSymbol(),
                order.getQuantity(),
                order.getPrice(),
                order.getSide(),
                order.getType(),
                order.getStatus(),
                order.getFilledQuantity(),
                order.getCreatedAt(),
                order.getLastModified()
        );
    }
    
    private TradingStatisticsDto convertToStatisticsDto(TradingStatistics stats) {
        return new TradingStatisticsDto(
                stats.getTotalTrades(),
                stats.getProfitableTrades(),
                stats.getTotalPnL(),
                stats.getWinRate(),
                stats.getAverageTradeSize()
        );
    }
    
    private PriceAlertDto convertToAlertDto(PriceAlert alert) {
        return new PriceAlertDto(
                alert.getId(),
                alert.getSymbol(),
                alert.getThreshold(),
                alert.getType(),
                alert.isActive(),
                alert.getCreatedAt()
        );
    }
}

// Reactive validation service
@Service
public class ReactiveValidationService {
    
    private final ReactiveRiskService riskService;
    private final ReactiveComplianceService complianceService;
    
    public ReactiveValidationService(ReactiveRiskService riskService,
                                   ReactiveComplianceService complianceService) {
        this.riskService = riskService;
        this.complianceService = complianceService;
    }
    
    public Mono<PlaceOrderRequestDto> validateOrderRequest(PlaceOrderRequestDto request) {
        return Mono.just(request)
                .flatMap(this::validateBasicFields)
                .flatMap(this::validateRiskLimits)
                .flatMap(this::validateCompliance)
                .onErrorMap(this::mapValidationError);
    }
    
    private Mono<PlaceOrderRequestDto> validateBasicFields(PlaceOrderRequestDto request) {
        return Mono.fromCallable(() -> {
            if (request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Quantity must be positive");
            }
            if (request.getPrice() != null && request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Price must be positive");
            }
            return request;
        });
    }
    
    private Mono<PlaceOrderRequestDto> validateRiskLimits(PlaceOrderRequestDto request) {
        return riskService.checkRiskLimits(request.getTraderId(), request.getSymbol(), 
                request.getQuantity(), request.getPrice())
                .flatMap(riskCheck -> {
                    if (!riskCheck.isWithinLimits()) {
                        return Mono.error(new RiskLimitExceededException(riskCheck.getReason()));
                    }
                    return Mono.just(request);
                });
    }
    
    private Mono<PlaceOrderRequestDto> validateCompliance(PlaceOrderRequestDto request) {
        return complianceService.checkCompliance(request.getTraderId(), request.getSymbol())
                .flatMap(complianceCheck -> {
                    if (!complianceCheck.isCompliant()) {
                        return Mono.error(new ComplianceViolationException(
                                String.join(", ", complianceCheck.getViolations())));
                    }
                    return Mono.just(request);
                });
    }
    
    private Throwable mapValidationError(Throwable error) {
        if (error instanceof RiskLimitExceededException || 
            error instanceof ComplianceViolationException) {
            return error;
        }
        return new ValidationException("Validation failed: " + error.getMessage(), error);
    }
}

// DTO classes
public class MarketDataDto {
    private String symbol;
    private BigDecimal price;
    private Long volume;
    private Instant timestamp;
    private TechnicalIndicators technicalIndicators;
    
    public MarketDataDto(String symbol, BigDecimal price, Long volume, Instant timestamp) {
        this.symbol = symbol;
        this.price = price;
        this.volume = volume;
        this.timestamp = timestamp;
    }
    
    // Getters and setters
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public Long getVolume() { return volume; }
    public void setVolume(Long volume) { this.volume = volume; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    public TechnicalIndicators getTechnicalIndicators() { return technicalIndicators; }
    public void setTechnicalIndicators(TechnicalIndicators technicalIndicators) { 
        this.technicalIndicators = technicalIndicators; 
    }
}

public class PlaceOrderRequestDto {
    @NotBlank
    private String traderId;
    
    @NotBlank
    private String symbol;
    
    @NotNull
    @Positive
    private BigDecimal quantity;
    
    @Positive
    private BigDecimal price; // null for market orders
    
    @NotNull
    private OrderSide side;
    
    @NotNull
    private OrderType type;
    
    // Getters and setters
    public String getTraderId() { return traderId; }
    public void setTraderId(String traderId) { this.traderId = traderId; }
    
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public OrderSide getSide() { return side; }
    public void setSide(OrderSide side) { this.side = side; }
    
    public OrderType getType() { return type; }
    public void setType(OrderType type) { this.type = type; }
}

public class OrderResponseDto {
    private String orderId;
    private String status;
    private String message;
    private Instant timestamp;
    
    public OrderResponseDto(String orderId, String status, String message, Instant timestamp) {
        this.orderId = orderId;
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
    }
    
    // Getters and setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}

// Custom exceptions
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

public class RiskLimitExceededException extends RuntimeException {
    public RiskLimitExceededException(String message) {
        super(message);
    }
}

public class ComplianceViolationException extends RuntimeException {
    public ComplianceViolationException(String message) {
        super(message);
    }
}

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
```

## 3. Reactive Data Access with R2DBC

### R2DBC Repository Implementation

```java
// Advanced R2DBC repository with reactive patterns
@Repository
public class ReactiveOrderRepository {
    
    private final R2dbcEntityTemplate template;
    private final DatabaseClient databaseClient;
    
    public ReactiveOrderRepository(R2dbcEntityTemplate template, DatabaseClient databaseClient) {
        this.template = template;
        this.databaseClient = databaseClient;
    }
    
    // Basic CRUD operations
    public Mono<Order> save(Order order) {
        return template.insert(order)
                .doOnSuccess(saved -> log.debug("Saved order: {}", saved.getId()))
                .doOnError(error -> log.error("Error saving order", error));
    }
    
    public Mono<Order> findById(String orderId) {
        return template.selectOne(
                Query.query(Criteria.where("id").is(orderId)), 
                Order.class
        );
    }
    
    public Flux<Order> findByTraderId(String traderId) {
        return template.select(
                Query.query(Criteria.where("trader_id").is(traderId))
                        .sort(Sort.by(Sort.Direction.DESC, "created_at")),
                Order.class
        );
    }
    
    // Complex queries with custom SQL
    public Flux<Order> findActiveOrdersByTrader(String traderId) {
        String sql = """
                SELECT * FROM orders 
                WHERE trader_id = :traderId 
                AND status IN ('PENDING', 'PARTIALLY_FILLED')
                ORDER BY created_at DESC
                """;
        
        return databaseClient.sql(sql)
                .bind("traderId", traderId)
                .map((row, metadata) -> mapRowToOrder(row))
                .all();
    }
    
    // Reactive batch operations
    public Flux<Order> saveAll(Flux<Order> orders) {
        return orders
                .buffer(100) // Process in batches of 100
                .flatMap(this::saveBatch)
                .doOnNext(order -> log.debug("Batch saved order: {}", order.getId()));
    }
    
    private Flux<Order> saveBatch(List<Order> orderBatch) {
        return template.insert(orderBatch)
                .collectList()
                .flatMapMany(Flux::fromIterable);
    }
    
    // Complex aggregation queries
    public Mono<TradingStatistics> getTradingStatistics(String traderId, LocalDate fromDate, LocalDate toDate) {
        String sql = """
                SELECT 
                    COUNT(*) as total_trades,
                    COUNT(CASE WHEN status = 'FILLED' THEN 1 END) as executed_trades,
                    SUM(CASE WHEN status = 'FILLED' THEN quantity * price ELSE 0 END) as total_volume,
                    AVG(CASE WHEN status = 'FILLED' THEN quantity * price END) as avg_trade_size,
                    SUM(CASE WHEN status = 'FILLED' AND side = 'SELL' THEN 
                        (price - avg_cost) * quantity ELSE 0 END) as realized_pnl
                FROM orders o
                LEFT JOIN positions p ON o.symbol = p.symbol AND o.trader_id = p.trader_id
                WHERE o.trader_id = :traderId 
                AND o.created_at BETWEEN :fromDate AND :toDate
                """;
        
        return databaseClient.sql(sql)
                .bind("traderId", traderId)
                .bind("fromDate", fromDate.atStartOfDay())
                .bind("toDate", toDate.plusDays(1).atStartOfDay())
                .map((row, metadata) -> new TradingStatistics(
                        row.get("total_trades", Long.class),
                        row.get("executed_trades", Long.class),
                        row.get("total_volume", BigDecimal.class),
                        row.get("avg_trade_size", BigDecimal.class),
                        row.get("realized_pnl", BigDecimal.class)
                ))
                .one();
    }
    
    // Reactive streaming for large datasets
    public Flux<Order> streamOrdersByDateRange(LocalDate fromDate, LocalDate toDate) {
        String sql = """
                SELECT * FROM orders 
                WHERE created_at BETWEEN :fromDate AND :toDate
                ORDER BY created_at
                """;
        
        return databaseClient.sql(sql)
                .bind("fromDate", fromDate.atStartOfDay())
                .bind("toDate", toDate.plusDays(1).atStartOfDay())
                .map((row, metadata) -> mapRowToOrder(row))
                .all()
                .buffer(1000) // Stream in chunks
                .flatMap(Flux::fromIterable);
    }
    
    // Reactive search with dynamic criteria
    public Flux<Order> searchOrders(OrderSearchCriteria criteria) {
        Query query = Query.empty();
        
        if (criteria.getTraderId() != null) {
            query = query.and(Criteria.where("trader_id").is(criteria.getTraderId()));
        }
        
        if (criteria.getSymbol() != null) {
            query = query.and(Criteria.where("symbol").is(criteria.getSymbol()));
        }
        
        if (criteria.getStatus() != null) {
            query = query.and(Criteria.where("status").is(criteria.getStatus()));
        }
        
        if (criteria.getFromDate() != null) {
            query = query.and(Criteria.where("created_at").greaterThanOrEquals(criteria.getFromDate()));
        }
        
        if (criteria.getToDate() != null) {
            query = query.and(Criteria.where("created_at").lessThan(criteria.getToDate().plusDays(1)));
        }
        
        return template.select(
                query.sort(Sort.by(Sort.Direction.DESC, "created_at")),
                Order.class
        );
    }
    
    // Reactive transactions
    @Transactional
    public Mono<Void> updateOrderAndPosition(String orderId, BigDecimal executedQuantity, 
                                           BigDecimal executionPrice) {
        return findById(orderId)
                .flatMap(order -> {
                    // Update order
                    Order updatedOrder = order.withExecution(executedQuantity, executionPrice);
                    Mono<Order> saveOrder = template.update(updatedOrder);
                    
                    // Update position
                    Mono<Position> updatePosition = updatePositionForExecution(
                            order.getTraderId(), order.getSymbol(), 
                            executedQuantity, executionPrice, order.getSide()
                    );
                    
                    return Mono.when(saveOrder, updatePosition);
                })
                .then();
    }
    
    private Mono<Position> updatePositionForExecution(String traderId, String symbol,
                                                     BigDecimal quantity, BigDecimal price, OrderSide side) {
        String sql = """
                INSERT INTO positions (trader_id, symbol, quantity, avg_cost, last_updated)
                VALUES (:traderId, :symbol, :quantity, :price, :lastUpdated)
                ON CONFLICT (trader_id, symbol) 
                DO UPDATE SET 
                    quantity = positions.quantity + :deltaQuantity,
                    avg_cost = CASE 
                        WHEN :side = 'BUY' THEN 
                            ((positions.quantity * positions.avg_cost) + (:quantity * :price)) / 
                            (positions.quantity + :quantity)
                        ELSE positions.avg_cost
                    END,
                    last_updated = :lastUpdated
                """;
        
        BigDecimal deltaQuantity = side == OrderSide.BUY ? quantity : quantity.negate();
        
        return databaseClient.sql(sql)
                .bind("traderId", traderId)
                .bind("symbol", symbol)
                .bind("quantity", side == OrderSide.BUY ? quantity : BigDecimal.ZERO)
                .bind("price", price)
                .bind("deltaQuantity", deltaQuantity)
                .bind("side", side.name())
                .bind("lastUpdated", Instant.now())
                .fetch()
                .rowsUpdated()
                .then(findPositionByTraderAndSymbol(traderId, symbol));
    }
    
    private Mono<Position> findPositionByTraderAndSymbol(String traderId, String symbol) {
        return template.selectOne(
                Query.query(Criteria.where("trader_id").is(traderId)
                        .and("symbol").is(symbol)),
                Position.class
        );
    }
    
    // Helper methods
    private Order mapRowToOrder(Row row) {
        return new Order(
                row.get("id", String.class),
                row.get("trader_id", String.class),
                row.get("symbol", String.class),
                row.get("quantity", BigDecimal.class),
                row.get("price", BigDecimal.class),
                OrderSide.valueOf(row.get("side", String.class)),
                OrderType.valueOf(row.get("type", String.class)),
                OrderStatus.valueOf(row.get("status", String.class)),
                row.get("filled_quantity", BigDecimal.class),
                row.get("created_at", Instant.class),
                row.get("last_modified", Instant.class)
        );
    }
}

// Custom reactive repository for complex operations
@Repository
public class ReactivePortfolioRepository {
    
    private final DatabaseClient databaseClient;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    
    public ReactivePortfolioRepository(DatabaseClient databaseClient,
                                     ReactiveRedisTemplate<String, String> redisTemplate) {
        this.databaseClient = databaseClient;
        this.redisTemplate = redisTemplate;
    }
    
    public Mono<Portfolio> getPortfolio(String traderId) {
        return getPositions(traderId)
                .collectList()
                .map(positions -> new Portfolio(traderId, positions))
                .cache(Duration.ofMinutes(5)); // Cache for 5 minutes
    }
    
    private Flux<Position> getPositions(String traderId) {
        String sql = """
                SELECT p.*, 
                       COALESCE(md.current_price, p.avg_cost) as current_price,
                       (COALESCE(md.current_price, p.avg_cost) - p.avg_cost) * p.quantity as unrealized_pnl
                FROM positions p
                LEFT JOIN market_data md ON p.symbol = md.symbol
                WHERE p.trader_id = :traderId 
                AND p.quantity != 0
                """;
        
        return databaseClient.sql(sql)
                .bind("traderId", traderId)
                .map((row, metadata) -> new Position(
                        row.get("trader_id", String.class),
                        row.get("symbol", String.class),
                        row.get("quantity", BigDecimal.class),
                        row.get("avg_cost", BigDecimal.class),
                        row.get("current_price", BigDecimal.class),
                        row.get("unrealized_pnl", BigDecimal.class),
                        row.get("last_updated", Instant.class)
                ))
                .all();
    }
    
    // Reactive portfolio rebalancing
    public Mono<Void> rebalancePortfolio(String traderId, Map<String, BigDecimal> targetAllocations) {
        return getPortfolio(traderId)
                .flatMap(portfolio -> calculateRebalancingTrades(portfolio, targetAllocations))
                .flatMap(this::executeRebalancingTrades)
                .then();
    }
    
    private Mono<List<RebalancingTrade>> calculateRebalancingTrades(Portfolio portfolio, 
                                                                  Map<String, BigDecimal> targetAllocations) {
        return Mono.fromCallable(() -> {
            BigDecimal totalValue = portfolio.getTotalValue();
            List<RebalancingTrade> trades = new ArrayList<>();
            
            for (Map.Entry<String, BigDecimal> target : targetAllocations.entrySet()) {
                String symbol = target.getKey();
                BigDecimal targetAllocation = target.getValue();
                BigDecimal targetValue = totalValue.multiply(targetAllocation);
                
                Position currentPosition = portfolio.getPosition(symbol)
                        .orElse(new Position(portfolio.getTraderId(), symbol, BigDecimal.ZERO, 
                                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, Instant.now()));
                
                BigDecimal currentValue = currentPosition.getCurrentValue();
                BigDecimal deltaValue = targetValue.subtract(currentValue);
                
                if (deltaValue.abs().compareTo(BigDecimal.valueOf(100)) > 0) { // Minimum trade threshold
                    trades.add(new RebalancingTrade(symbol, deltaValue, 
                            deltaValue.compareTo(BigDecimal.ZERO) > 0 ? OrderSide.BUY : OrderSide.SELL));
                }
            }
            
            return trades;
        });
    }
    
    private Mono<Void> executeRebalancingTrades(List<RebalancingTrade> trades) {
        return Flux.fromIterable(trades)
                .flatMap(this::executeRebalancingTrade)
                .then();
    }
    
    private Mono<Order> executeRebalancingTrade(RebalancingTrade trade) {
        // This would integrate with the order placement service
        return Mono.empty(); // Placeholder
    }
}

// Domain models
@Table("orders")
public class Order {
    @Id
    private String id;
    
    @Column("trader_id")
    private String traderId;
    
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal price;
    private OrderSide side;
    private OrderType type;
    private OrderStatus status;
    
    @Column("filled_quantity")
    private BigDecimal filledQuantity;
    
    @Column("created_at")
    private Instant createdAt;
    
    @Column("last_modified")
    private Instant lastModified;
    
    // Constructors
    public Order() {}
    
    public Order(String id, String traderId, String symbol, BigDecimal quantity, BigDecimal price,
                OrderSide side, OrderType type, OrderStatus status, BigDecimal filledQuantity,
                Instant createdAt, Instant lastModified) {
        this.id = id;
        this.traderId = traderId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.side = side;
        this.type = type;
        this.status = status;
        this.filledQuantity = filledQuantity;
        this.createdAt = createdAt;
        this.lastModified = lastModified;
    }
    
    public Order withExecution(BigDecimal executedQuantity, BigDecimal executionPrice) {
        BigDecimal newFilledQuantity = this.filledQuantity.add(executedQuantity);
        OrderStatus newStatus = newFilledQuantity.compareTo(quantity) >= 0 ? 
                OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED;
        
        return new Order(id, traderId, symbol, quantity, price, side, type, newStatus,
                newFilledQuantity, createdAt, Instant.now());
    }
    
    // Getters
    public String getId() { return id; }
    public String getTraderId() { return traderId; }
    public String getSymbol() { return symbol; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public OrderSide getSide() { return side; }
    public OrderType getType() { return type; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getFilledQuantity() { return filledQuantity; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastModified() { return lastModified; }
}

@Table("positions")
public class Position {
    @Column("trader_id")
    private String traderId;
    
    private String symbol;
    private BigDecimal quantity;
    
    @Column("avg_cost")
    private BigDecimal averageCost;
    
    @Column("current_price")
    private BigDecimal currentPrice;
    
    @Column("unrealized_pnl")
    private BigDecimal unrealizedPnL;
    
    @Column("last_updated")
    private Instant lastUpdated;
    
    public Position() {}
    
    public Position(String traderId, String symbol, BigDecimal quantity, BigDecimal averageCost,
                   BigDecimal currentPrice, BigDecimal unrealizedPnL, Instant lastUpdated) {
        this.traderId = traderId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.averageCost = averageCost;
        this.currentPrice = currentPrice;
        this.unrealizedPnL = unrealizedPnL;
        this.lastUpdated = lastUpdated;
    }
    
    public Position withCurrentPrice(BigDecimal newCurrentPrice) {
        BigDecimal newUnrealizedPnL = newCurrentPrice.subtract(averageCost).multiply(quantity);
        return new Position(traderId, symbol, quantity, averageCost, newCurrentPrice, 
                newUnrealizedPnL, Instant.now());
    }
    
    public BigDecimal getCurrentValue() {
        return currentPrice.multiply(quantity);
    }
    
    // Getters
    public String getTraderId() { return traderId; }
    public String getSymbol() { return symbol; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getAverageCost() { return averageCost; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public BigDecimal getUnrealizedPnL() { return unrealizedPnL; }
    public Instant getLastUpdated() { return lastUpdated; }
}

public class Portfolio {
    private final String traderId;
    private final List<Position> positions;
    private final Instant lastUpdated;
    
    public Portfolio(String traderId, List<Position> positions) {
        this.traderId = traderId;
        this.positions = positions;
        this.lastUpdated = Instant.now();
    }
    
    public Portfolio withPositions(List<Position> newPositions) {
        return new Portfolio(traderId, newPositions);
    }
    
    public BigDecimal getTotalValue() {
        return positions.stream()
                .map(Position::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public Optional<Position> getPosition(String symbol) {
        return positions.stream()
                .filter(p -> p.getSymbol().equals(symbol))
                .findFirst();
    }
    
    // Getters
    public String getTraderId() { return traderId; }
    public List<Position> getPositions() { return Collections.unmodifiableList(positions); }
    public Instant getLastUpdated() { return lastUpdated; }
}

// Enums
public enum OrderSide {
    BUY, SELL
}

public enum OrderType {
    MARKET, LIMIT, STOP, STOP_LIMIT
}

public enum OrderStatus {
    PENDING, PARTIALLY_FILLED, FILLED, CANCELLED, REJECTED
}

// Supporting classes
public class OrderSearchCriteria {
    private String traderId;
    private String symbol;
    private OrderStatus status;
    private LocalDate fromDate;
    private LocalDate toDate;
    
    public OrderSearchCriteria(String traderId, String symbol, OrderStatus status) {
        this.traderId = traderId;
        this.symbol = symbol;
        this.status = status;
    }
    
    // Getters and setters
    public String getTraderId() { return traderId; }
    public void setTraderId(String traderId) { this.traderId = traderId; }
    
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    
    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }
    
    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }
}

public class TradingStatistics {
    private final Long totalTrades;
    private final Long executedTrades;
    private final BigDecimal totalVolume;
    private final BigDecimal averageTradeSize;
    private final BigDecimal realizedPnL;
    
    public TradingStatistics(Long totalTrades, Long executedTrades, BigDecimal totalVolume,
                           BigDecimal averageTradeSize, BigDecimal realizedPnL) {
        this.totalTrades = totalTrades;
        this.executedTrades = executedTrades;
        this.totalVolume = totalVolume;
        this.averageTradeSize = averageTradeSize;
        this.realizedPnL = realizedPnL;
    }
    
    public BigDecimal getWinRate() {
        return totalTrades > 0 ? 
                BigDecimal.valueOf(executedTrades).divide(BigDecimal.valueOf(totalTrades), 4, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;
    }
    
    // Getters
    public Long getTotalTrades() { return totalTrades; }
    public Long getProfitableTrades() { return executedTrades; }
    public BigDecimal getTotalVolume() { return totalVolume; }
    public BigDecimal getAverageTradeSize() { return averageTradeSize; }
    public BigDecimal getTotalPnL() { return realizedPnL; }
}

public class RebalancingTrade {
    private final String symbol;
    private final BigDecimal value;
    private final OrderSide side;
    
    public RebalancingTrade(String symbol, BigDecimal value, OrderSide side) {
        this.symbol = symbol;
        this.value = value;
        this.side = side;
    }
    
    // Getters
    public String getSymbol() { return symbol; }
    public BigDecimal getValue() { return value; }
    public OrderSide getSide() { return side; }
}
```

## Key Takeaways

1. **Reactive Principles**: Non-blocking I/O with proper backpressure handling
2. **WebFlux Framework**: Server-Sent Events, WebSocket, and reactive REST APIs
3. **R2DBC Integration**: Reactive database access with transactions and streaming
4. **Performance Optimization**: Efficient resource utilization and memory management
5. **Error Handling**: Comprehensive reactive error handling strategies
6. **Real-time Processing**: Stream processing with complex transformations
7. **Production Ready**: Monitoring, caching, and resilience patterns

This comprehensive reactive implementation provides the foundation for building high-performance, scalable financial applications that can handle thousands of concurrent users with minimal resource consumption.