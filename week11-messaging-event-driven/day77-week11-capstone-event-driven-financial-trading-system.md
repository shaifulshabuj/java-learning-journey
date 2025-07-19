# Day 77: Week 11 Capstone - Event-Driven Financial Trading System

## Overview

Today we'll build a comprehensive Event-Driven Financial Trading System that integrates all the messaging and event-driven patterns we've learned throughout Week 11. This system will handle real-time trading operations, market data streaming, order processing, risk management, and compliance reporting.

## Learning Objectives

- Design and implement a complete event-driven trading system
- Integrate multiple messaging technologies (Kafka, RabbitMQ, WebSockets)
- Implement event sourcing for audit trails
- Build real-time market data streaming
- Create reactive order processing pipelines
- Implement risk management and compliance features

## 1. System Architecture Overview

### Architecture Components

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                        Event-Driven Financial Trading System                         │
├─────────────────────────────────────────────────────────────────────────────────────┤
│  Frontend Applications                                                              │
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐ │
│  │ Trading Web UI  │ │ Mobile Trading  │ │ Admin Dashboard │ │ Analytics Portal │ │
│  │  (WebSockets)   │ │     App (SSE)   │ │   (WebSockets)  │ │    (REST/SSE)    │ │
│  └─────────────────┘ └─────────────────┘ └─────────────────┘ └─────────────────┘ │
├─────────────────────────────────────────────────────────────────────────────────────┤
│  API Gateway Layer                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐   │
│  │               Spring Cloud Gateway with Rate Limiting & Security             │   │
│  └─────────────────────────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────────────────────────┤
│  Core Trading Services                                                              │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐             │
│  │Order Service │ │Market Data   │ │Risk Service  │ │Portfolio     │             │
│  │(CQRS/ES)    │ │Service       │ │              │ │Service       │             │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘             │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐             │
│  │Settlement    │ │Pricing       │ │Compliance    │ │Notification  │             │
│  │Service       │ │Engine        │ │Service       │ │Service       │             │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘             │
├─────────────────────────────────────────────────────────────────────────────────────┤
│  Message Infrastructure                                                             │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐             │
│  │Apache Kafka  │ │RabbitMQ      │ │Event Store   │ │Redis Cache   │             │
│  │(Streaming)   │ │(Commands)    │ │(PostgreSQL)  │ │(Real-time)   │             │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘             │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

### Technology Stack

- **Event Streaming**: Apache Kafka for market data and trade events
- **Message Queuing**: RabbitMQ for command processing
- **Event Sourcing**: Custom event store with PostgreSQL
- **Real-time Communication**: WebSockets and Server-Sent Events
- **Reactive Processing**: Project Reactor and Spring WebFlux
- **Integration**: Spring Integration for workflow orchestration
- **Caching**: Redis for real-time data

## 2. Domain Models

### Trading Domain Models

```java
// Trade Order
@Entity
@Table(name = "trade_orders")
public class TradeOrder {
    @Id
    private String orderId;
    
    @Enumerated(EnumType.STRING)
    private OrderType orderType; // MARKET, LIMIT, STOP, STOP_LIMIT
    
    @Enumerated(EnumType.STRING)
    private OrderSide side; // BUY, SELL
    
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal stopPrice;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    private String traderId;
    private String portfolioId;
    
    @Enumerated(EnumType.STRING)
    private TimeInForce timeInForce; // DAY, GTC, IOC, FOK
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime executedAt;
    
    private BigDecimal executedQuantity;
    private BigDecimal executedPrice;
    
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderExecution> executions;
    
    // Constructors, getters, and setters
}

// Market Data
public class MarketData {
    private String symbol;
    private BigDecimal bidPrice;
    private BigDecimal askPrice;
    private BigDecimal lastPrice;
    private BigDecimal volume;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal open;
    private BigDecimal close;
    private LocalDateTime timestamp;
    
    // Constructors, getters, and setters
}

// Trade Execution
@Entity
@Table(name = "order_executions")
public class OrderExecution {
    @Id
    private String executionId;
    
    private String orderId;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal commission;
    private String executionVenue;
    private LocalDateTime executedAt;
    
    // Constructors, getters, and setters
}

// Portfolio
@Entity
@Table(name = "portfolios")
public class Portfolio {
    @Id
    private String portfolioId;
    
    private String traderId;
    private String name;
    private BigDecimal cash;
    private BigDecimal totalValue;
    
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Position> positions;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors, getters, and setters
}

// Position
@Entity
@Table(name = "positions")
public class Position {
    @Id
    private String positionId;
    
    private String portfolioId;
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal averagePrice;
    private BigDecimal currentPrice;
    private BigDecimal unrealizedPnL;
    private BigDecimal realizedPnL;
    
    private LocalDateTime openedAt;
    private LocalDateTime updatedAt;
    
    // Constructors, getters, and setters
}

// Enums
public enum OrderType {
    MARKET, LIMIT, STOP, STOP_LIMIT
}

public enum OrderSide {
    BUY, SELL
}

public enum OrderStatus {
    NEW, PENDING, PARTIALLY_FILLED, FILLED, CANCELLED, REJECTED, EXPIRED
}

public enum TimeInForce {
    DAY, GTC, IOC, FOK
}
```

## 3. Event Models

### Trading Events

```java
// Base Event
public abstract class TradingEvent {
    private String eventId;
    private String aggregateId;
    private LocalDateTime timestamp;
    private String userId;
    private Map<String, String> metadata;
    
    protected TradingEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.metadata = new HashMap<>();
    }
    
    // Getters and setters
}

// Order Events
public class OrderCreatedEvent extends TradingEvent {
    private TradeOrder order;
    
    public OrderCreatedEvent(TradeOrder order) {
        super();
        this.order = order;
        this.setAggregateId(order.getOrderId());
    }
    
    // Getters and setters
}

public class OrderFilledEvent extends TradingEvent {
    private String orderId;
    private BigDecimal executedQuantity;
    private BigDecimal executedPrice;
    private String executionId;
    
    // Constructors, getters, and setters
}

public class OrderCancelledEvent extends TradingEvent {
    private String orderId;
    private String reason;
    
    // Constructors, getters, and setters
}

public class OrderRejectedEvent extends TradingEvent {
    private String orderId;
    private String reason;
    private String riskViolation;
    
    // Constructors, getters, and setters
}

// Market Data Events
public class MarketDataUpdateEvent extends TradingEvent {
    private MarketData marketData;
    
    // Constructors, getters, and setters
}

public class TradeExecutedEvent extends TradingEvent {
    private String symbol;
    private BigDecimal price;
    private BigDecimal quantity;
    private OrderSide side;
    private String buyOrderId;
    private String sellOrderId;
    
    // Constructors, getters, and setters
}

// Risk Events
public class RiskLimitBreachedEvent extends TradingEvent {
    private String portfolioId;
    private String riskType;
    private BigDecimal currentValue;
    private BigDecimal limitValue;
    private String action;
    
    // Constructors, getters, and setters
}

// Compliance Events
public class ComplianceCheckFailedEvent extends TradingEvent {
    private String orderId;
    private String complianceRule;
    private String violation;
    private String action;
    
    // Constructors, getters, and setters
}
```

## 4. Order Service with Event Sourcing and CQRS

### Order Command Service

```java
@Service
@Transactional
public class OrderCommandService {
    
    private final EventStoreService eventStoreService;
    private final OrderEventPublisher orderEventPublisher;
    private final RiskService riskService;
    private final ComplianceService complianceService;
    
    public OrderCommandService(EventStoreService eventStoreService,
                              OrderEventPublisher orderEventPublisher,
                              RiskService riskService,
                              ComplianceService complianceService) {
        this.eventStoreService = eventStoreService;
        this.orderEventPublisher = orderEventPublisher;
        this.riskService = riskService;
        this.complianceService = complianceService;
    }
    
    public Mono<TradeOrder> createOrder(CreateOrderCommand command) {
        return Mono.fromCallable(() -> {
            // Create order
            TradeOrder order = new TradeOrder();
            order.setOrderId(UUID.randomUUID().toString());
            order.setOrderType(command.getOrderType());
            order.setSide(command.getSide());
            order.setSymbol(command.getSymbol());
            order.setQuantity(command.getQuantity());
            order.setPrice(command.getPrice());
            order.setStopPrice(command.getStopPrice());
            order.setStatus(OrderStatus.NEW);
            order.setTraderId(command.getTraderId());
            order.setPortfolioId(command.getPortfolioId());
            order.setTimeInForce(command.getTimeInForce());
            order.setCreatedAt(LocalDateTime.now());
            
            return order;
        })
        .flatMap(order -> performPreTradeChecks(order))
        .flatMap(order -> {
            // Save event
            OrderCreatedEvent event = new OrderCreatedEvent(order);
            eventStoreService.saveEvent(order.getOrderId(), "TradeOrder", event);
            
            // Publish event
            return orderEventPublisher.publishOrderCreated(event)
                .thenReturn(order);
        })
        .doOnSuccess(order -> 
            System.out.println("Order created: " + order.getOrderId()))
        .doOnError(error -> 
            System.err.println("Failed to create order: " + error.getMessage()));
    }
    
    public Mono<TradeOrder> cancelOrder(CancelOrderCommand command) {
        return loadOrderAggregate(command.getOrderId())
            .flatMap(order -> {
                if (order.getStatus() == OrderStatus.FILLED) {
                    return Mono.error(new IllegalStateException("Cannot cancel filled order"));
                }
                
                order.setStatus(OrderStatus.CANCELLED);
                order.setUpdatedAt(LocalDateTime.now());
                
                // Save event
                OrderCancelledEvent event = new OrderCancelledEvent();
                event.setOrderId(command.getOrderId());
                event.setReason(command.getReason());
                
                eventStoreService.saveEvent(order.getOrderId(), "TradeOrder", event);
                
                // Publish event
                return orderEventPublisher.publishOrderCancelled(event)
                    .thenReturn(order);
            });
    }
    
    private Mono<TradeOrder> performPreTradeChecks(TradeOrder order) {
        return riskService.checkRiskLimits(order)
            .zipWith(complianceService.checkCompliance(order))
            .flatMap(tuple -> {
                RiskCheckResult riskResult = tuple.getT1();
                ComplianceCheckResult complianceResult = tuple.getT2();
                
                if (!riskResult.isPassed()) {
                    OrderRejectedEvent event = new OrderRejectedEvent();
                    event.setOrderId(order.getOrderId());
                    event.setReason("Risk check failed");
                    event.setRiskViolation(riskResult.getViolation());
                    
                    return orderEventPublisher.publishOrderRejected(event)
                        .then(Mono.error(new RiskCheckException(riskResult.getViolation())));
                }
                
                if (!complianceResult.isPassed()) {
                    ComplianceCheckFailedEvent event = new ComplianceCheckFailedEvent();
                    event.setOrderId(order.getOrderId());
                    event.setComplianceRule(complianceResult.getRule());
                    event.setViolation(complianceResult.getViolation());
                    
                    return orderEventPublisher.publishComplianceCheckFailed(event)
                        .then(Mono.error(new ComplianceException(complianceResult.getViolation())));
                }
                
                return Mono.just(order);
            });
    }
    
    private Mono<TradeOrder> loadOrderAggregate(String orderId) {
        return Mono.fromCallable(() -> 
            eventStoreService.reconstructAggregate(orderId, "TradeOrder", TradeOrder.class))
            .subscribeOn(Schedulers.boundedElastic());
    }
}
```

### Order Query Service

```java
@Service
public class OrderQueryService {
    
    private final OrderReadModelRepository orderReadModelRepository;
    private final ReactiveRedisTemplate<String, TradeOrder> redisTemplate;
    
    public OrderQueryService(OrderReadModelRepository orderReadModelRepository,
                            ReactiveRedisTemplate<String, TradeOrder> redisTemplate) {
        this.orderReadModelRepository = orderReadModelRepository;
        this.redisTemplate = redisTemplate;
    }
    
    public Mono<TradeOrder> getOrder(String orderId) {
        return redisTemplate.opsForValue()
            .get("order:" + orderId)
            .switchIfEmpty(
                orderReadModelRepository.findById(orderId)
                    .flatMap(order -> 
                        redisTemplate.opsForValue()
                            .set("order:" + orderId, order, Duration.ofMinutes(5))
                            .thenReturn(order)
                    )
            );
    }
    
    public Flux<TradeOrder> getOrdersByTrader(String traderId) {
        return orderReadModelRepository.findByTraderId(traderId)
            .sort(Comparator.comparing(TradeOrder::getCreatedAt).reversed());
    }
    
    public Flux<TradeOrder> getActiveOrders() {
        return orderReadModelRepository.findByStatusIn(
            Arrays.asList(OrderStatus.NEW, OrderStatus.PENDING, OrderStatus.PARTIALLY_FILLED)
        );
    }
    
    public Flux<OrderExecutionSummary> getExecutionSummary(String traderId, LocalDate date) {
        return orderReadModelRepository.findExecutionsByTraderAndDate(traderId, date)
            .groupBy(TradeOrder::getSymbol)
            .flatMap(group -> group
                .collectList()
                .map(orders -> calculateExecutionSummary(group.key(), orders))
            );
    }
    
    private OrderExecutionSummary calculateExecutionSummary(String symbol, List<TradeOrder> orders) {
        OrderExecutionSummary summary = new OrderExecutionSummary();
        summary.setSymbol(symbol);
        summary.setTotalOrders(orders.size());
        summary.setTotalVolume(orders.stream()
            .map(TradeOrder::getExecutedQuantity)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.setAveragePrice(calculateVWAP(orders));
        return summary;
    }
    
    private BigDecimal calculateVWAP(List<TradeOrder> orders) {
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalVolume = BigDecimal.ZERO;
        
        for (TradeOrder order : orders) {
            if (order.getExecutedQuantity() != null && order.getExecutedPrice() != null) {
                BigDecimal value = order.getExecutedQuantity().multiply(order.getExecutedPrice());
                totalValue = totalValue.add(value);
                totalVolume = totalVolume.add(order.getExecutedQuantity());
            }
        }
        
        return totalVolume.compareTo(BigDecimal.ZERO) > 0 ?
            totalValue.divide(totalVolume, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }
}
```

## 5. Market Data Service with Kafka Streams

### Market Data Streaming

```java
@Service
public class MarketDataStreamingService {
    
    private final ReactiveKafkaProducerTemplate<String, MarketData> kafkaProducer;
    private final StreamsBuilder streamsBuilder;
    private final Serde<MarketData> marketDataSerde;
    
    public MarketDataStreamingService(ReactiveKafkaProducerTemplate<String, MarketData> kafkaProducer,
                                     StreamsBuilder streamsBuilder,
                                     Serde<MarketData> marketDataSerde) {
        this.kafkaProducer = kafkaProducer;
        this.streamsBuilder = streamsBuilder;
        this.marketDataSerde = marketDataSerde;
    }
    
    @PostConstruct
    public void initializeStreams() {
        // Market data aggregation stream
        KStream<String, MarketData> marketDataStream = streamsBuilder
            .stream("market-data-raw", Consumed.with(Serdes.String(), marketDataSerde));
        
        // Calculate 1-minute bars
        marketDataStream
            .groupByKey()
            .windowedBy(TimeWindows.of(Duration.ofMinutes(1)))
            .aggregate(
                MarketDataBar::new,
                (key, value, aggregate) -> aggregate.update(value),
                Materialized.with(Serdes.String(), marketDataBarSerde())
            )
            .toStream()
            .map((windowedKey, bar) -> KeyValue.pair(windowedKey.key(), bar))
            .to("market-data-1min");
        
        // Calculate moving averages
        marketDataStream
            .groupByKey()
            .windowedBy(TimeWindows.of(Duration.ofMinutes(5)))
            .aggregate(
                MovingAverage::new,
                (key, value, aggregate) -> aggregate.add(value.getLastPrice()),
                Materialized.with(Serdes.String(), movingAverageSerde())
            )
            .toStream()
            .map((windowedKey, ma) -> KeyValue.pair(
                windowedKey.key(), 
                new MarketIndicator(windowedKey.key(), "SMA5", ma.getAverage())
            ))
            .to("market-indicators");
        
        // Detect price anomalies
        marketDataStream
            .filter((key, value) -> isPriceAnomaly(value))
            .map((key, value) -> KeyValue.pair(
                key,
                new PriceAlert(key, value.getLastPrice(), "ANOMALY_DETECTED")
            ))
            .to("price-alerts");
    }
    
    public Mono<Void> publishMarketData(Flux<MarketData> marketDataFlux) {
        return marketDataFlux
            .flatMap(data -> kafkaProducer.send("market-data-raw", data.getSymbol(), data))
            .doOnNext(result -> 
                System.out.println("Market data published: " + result.recordMetadata().offset()))
            .then();
    }
    
    public Flux<MarketData> subscribeToMarketData(String symbol) {
        return KafkaReceiver.create(receiverOptions("market-data-raw"))
            .receive()
            .filter(record -> symbol.equals(record.key()))
            .map(record -> record.value())
            .onBackpressureBuffer(1000);
    }
    
    public Flux<MarketDataBar> subscribe1MinuteBars(String symbol) {
        return KafkaReceiver.create(receiverOptions("market-data-1min"))
            .receive()
            .filter(record -> symbol.equals(record.key()))
            .map(record -> record.value())
            .onBackpressureLatest();
    }
    
    private boolean isPriceAnomaly(MarketData data) {
        // Simple anomaly detection - price movement > 5%
        if (data.getLastPrice() != null && data.getClose() != null) {
            BigDecimal change = data.getLastPrice().subtract(data.getClose())
                .abs()
                .divide(data.getClose(), 4, RoundingMode.HALF_UP);
            return change.compareTo(new BigDecimal("0.05")) > 0;
        }
        return false;
    }
    
    private ReceiverOptions<String, Object> receiverOptions(String topic) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "market-data-consumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        
        return ReceiverOptions.<String, Object>create(props)
            .subscription(Collections.singleton(topic));
    }
}
```

## 6. Order Matching Engine

### Matching Engine Service

```java
@Service
public class OrderMatchingEngine {
    
    private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();
    private final OrderExecutionService executionService;
    private final MarketDataPublisher marketDataPublisher;
    
    public OrderMatchingEngine(OrderExecutionService executionService,
                              MarketDataPublisher marketDataPublisher) {
        this.executionService = executionService;
        this.marketDataPublisher = marketDataPublisher;
    }
    
    public Mono<MatchingResult> submitOrder(TradeOrder order) {
        return Mono.fromCallable(() -> {
            OrderBook orderBook = orderBooks.computeIfAbsent(order.getSymbol(), OrderBook::new);
            
            synchronized (orderBook) {
                if (order.getOrderType() == OrderType.MARKET) {
                    return matchMarketOrder(order, orderBook);
                } else if (order.getOrderType() == OrderType.LIMIT) {
                    return matchLimitOrder(order, orderBook);
                }
                
                return new MatchingResult(order, Collections.emptyList());
            }
        })
        .flatMap(result -> processExecutions(result))
        .doOnSuccess(result -> updateMarketData(order.getSymbol()));
    }
    
    private MatchingResult matchMarketOrder(TradeOrder order, OrderBook orderBook) {
        List<Execution> executions = new ArrayList<>();
        BigDecimal remainingQuantity = order.getQuantity();
        
        PriorityQueue<TradeOrder> oppositeSide = order.getSide() == OrderSide.BUY ? 
            orderBook.getAsks() : orderBook.getBids();
        
        while (remainingQuantity.compareTo(BigDecimal.ZERO) > 0 && !oppositeSide.isEmpty()) {
            TradeOrder matchingOrder = oppositeSide.peek();
            BigDecimal executionQuantity = remainingQuantity.min(matchingOrder.getQuantity());
            
            Execution execution = new Execution(
                order.getOrderId(),
                matchingOrder.getOrderId(),
                executionQuantity,
                matchingOrder.getPrice()
            );
            executions.add(execution);
            
            remainingQuantity = remainingQuantity.subtract(executionQuantity);
            matchingOrder.setQuantity(matchingOrder.getQuantity().subtract(executionQuantity));
            
            if (matchingOrder.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                oppositeSide.poll();
            }
        }
        
        if (remainingQuantity.compareTo(BigDecimal.ZERO) > 0) {
            order.setQuantity(remainingQuantity);
            order.setStatus(OrderStatus.PARTIALLY_FILLED);
        } else {
            order.setStatus(OrderStatus.FILLED);
        }
        
        return new MatchingResult(order, executions);
    }
    
    private MatchingResult matchLimitOrder(TradeOrder order, OrderBook orderBook) {
        List<Execution> executions = new ArrayList<>();
        BigDecimal remainingQuantity = order.getQuantity();
        
        PriorityQueue<TradeOrder> oppositeSide = order.getSide() == OrderSide.BUY ? 
            orderBook.getAsks() : orderBook.getBids();
        
        while (remainingQuantity.compareTo(BigDecimal.ZERO) > 0 && !oppositeSide.isEmpty()) {
            TradeOrder matchingOrder = oppositeSide.peek();
            
            boolean priceMatch = order.getSide() == OrderSide.BUY ?
                order.getPrice().compareTo(matchingOrder.getPrice()) >= 0 :
                order.getPrice().compareTo(matchingOrder.getPrice()) <= 0;
            
            if (!priceMatch) {
                break;
            }
            
            BigDecimal executionQuantity = remainingQuantity.min(matchingOrder.getQuantity());
            BigDecimal executionPrice = matchingOrder.getPrice();
            
            Execution execution = new Execution(
                order.getOrderId(),
                matchingOrder.getOrderId(),
                executionQuantity,
                executionPrice
            );
            executions.add(execution);
            
            remainingQuantity = remainingQuantity.subtract(executionQuantity);
            matchingOrder.setQuantity(matchingOrder.getQuantity().subtract(executionQuantity));
            
            if (matchingOrder.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                oppositeSide.poll();
            }
        }
        
        if (remainingQuantity.compareTo(BigDecimal.ZERO) > 0) {
            order.setQuantity(remainingQuantity);
            if (executions.isEmpty()) {
                order.setStatus(OrderStatus.PENDING);
                if (order.getSide() == OrderSide.BUY) {
                    orderBook.getBids().offer(order);
                } else {
                    orderBook.getAsks().offer(order);
                }
            } else {
                order.setStatus(OrderStatus.PARTIALLY_FILLED);
            }
        } else {
            order.setStatus(OrderStatus.FILLED);
        }
        
        return new MatchingResult(order, executions);
    }
    
    private Mono<MatchingResult> processExecutions(MatchingResult result) {
        if (result.getExecutions().isEmpty()) {
            return Mono.just(result);
        }
        
        return Flux.fromIterable(result.getExecutions())
            .flatMap(execution -> executionService.processExecution(execution))
            .then(Mono.just(result));
    }
    
    private void updateMarketData(String symbol) {
        OrderBook orderBook = orderBooks.get(symbol);
        if (orderBook != null) {
            MarketData marketData = new MarketData();
            marketData.setSymbol(symbol);
            
            if (!orderBook.getBids().isEmpty()) {
                marketData.setBidPrice(orderBook.getBids().peek().getPrice());
            }
            if (!orderBook.getAsks().isEmpty()) {
                marketData.setAskPrice(orderBook.getAsks().peek().getPrice());
            }
            marketData.setTimestamp(LocalDateTime.now());
            
            marketDataPublisher.publishMarketData(marketData);
        }
    }
}
```

### Order Book

```java
public class OrderBook {
    private final String symbol;
    private final PriorityQueue<TradeOrder> bids;
    private final PriorityQueue<TradeOrder> asks;
    
    public OrderBook(String symbol) {
        this.symbol = symbol;
        this.bids = new PriorityQueue<>((a, b) -> b.getPrice().compareTo(a.getPrice())); // High to low
        this.asks = new PriorityQueue<>((a, b) -> a.getPrice().compareTo(b.getPrice())); // Low to high
    }
    
    public String getSymbol() { return symbol; }
    public PriorityQueue<TradeOrder> getBids() { return bids; }
    public PriorityQueue<TradeOrder> getAsks() { return asks; }
}
```

## 7. Risk Management Service

### Risk Service Implementation

```java
@Service
public class RiskService {
    
    private final PortfolioService portfolioService;
    private final MarketDataService marketDataService;
    private final RiskConfigRepository riskConfigRepository;
    private final RiskEventPublisher riskEventPublisher;
    
    public RiskService(PortfolioService portfolioService,
                      MarketDataService marketDataService,
                      RiskConfigRepository riskConfigRepository,
                      RiskEventPublisher riskEventPublisher) {
        this.portfolioService = portfolioService;
        this.marketDataService = marketDataService;
        this.riskConfigRepository = riskConfigRepository;
        this.riskEventPublisher = riskEventPublisher;
    }
    
    public Mono<RiskCheckResult> checkRiskLimits(TradeOrder order) {
        return portfolioService.getPortfolio(order.getPortfolioId())
            .zipWith(riskConfigRepository.findByPortfolioId(order.getPortfolioId()))
            .zipWith(marketDataService.getLatestMarketData(order.getSymbol()))
            .map(tuple -> {
                Portfolio portfolio = tuple.getT1().getT1();
                RiskConfig riskConfig = tuple.getT1().getT2();
                MarketData marketData = tuple.getT2();
                
                RiskCheckResult result = new RiskCheckResult();
                result.setPassed(true);
                
                // Check position limit
                if (!checkPositionLimit(order, portfolio, riskConfig)) {
                    result.setPassed(false);
                    result.setViolation("Position limit exceeded");
                    return result;
                }
                
                // Check exposure limit
                if (!checkExposureLimit(order, portfolio, riskConfig, marketData)) {
                    result.setPassed(false);
                    result.setViolation("Exposure limit exceeded");
                    return result;
                }
                
                // Check loss limit
                if (!checkLossLimit(portfolio, riskConfig)) {
                    result.setPassed(false);
                    result.setViolation("Daily loss limit exceeded");
                    return result;
                }
                
                // Check leverage
                if (!checkLeverage(order, portfolio, riskConfig, marketData)) {
                    result.setPassed(false);
                    result.setViolation("Leverage limit exceeded");
                    return result;
                }
                
                return result;
            })
            .doOnNext(result -> {
                if (!result.isPassed()) {
                    publishRiskLimitBreach(order, result.getViolation());
                }
            });
    }
    
    private boolean checkPositionLimit(TradeOrder order, Portfolio portfolio, RiskConfig config) {
        Optional<Position> existingPosition = portfolio.getPositions().stream()
            .filter(p -> p.getSymbol().equals(order.getSymbol()))
            .findFirst();
        
        BigDecimal currentQuantity = existingPosition
            .map(Position::getQuantity)
            .orElse(BigDecimal.ZERO);
        
        BigDecimal newQuantity = order.getSide() == OrderSide.BUY ?
            currentQuantity.add(order.getQuantity()) :
            currentQuantity.subtract(order.getQuantity());
        
        return newQuantity.abs().compareTo(config.getMaxPositionSize()) <= 0;
    }
    
    private boolean checkExposureLimit(TradeOrder order, Portfolio portfolio, 
                                     RiskConfig config, MarketData marketData) {
        BigDecimal orderValue = order.getQuantity().multiply(
            order.getPrice() != null ? order.getPrice() : marketData.getLastPrice()
        );
        
        BigDecimal totalExposure = portfolio.getPositions().stream()
            .map(p -> p.getQuantity().abs().multiply(p.getCurrentPrice()))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .add(orderValue);
        
        return totalExposure.compareTo(config.getMaxExposure()) <= 0;
    }
    
    private boolean checkLossLimit(Portfolio portfolio, RiskConfig config) {
        BigDecimal dailyPnL = calculateDailyPnL(portfolio);
        return dailyPnL.compareTo(config.getMaxDailyLoss().negate()) >= 0;
    }
    
    private boolean checkLeverage(TradeOrder order, Portfolio portfolio, 
                                RiskConfig config, MarketData marketData) {
        BigDecimal orderValue = order.getQuantity().multiply(
            order.getPrice() != null ? order.getPrice() : marketData.getLastPrice()
        );
        
        BigDecimal totalValue = portfolio.getTotalValue().add(orderValue);
        BigDecimal leverage = totalValue.divide(portfolio.getCash(), 2, RoundingMode.HALF_UP);
        
        return leverage.compareTo(config.getMaxLeverage()) <= 0;
    }
    
    private BigDecimal calculateDailyPnL(Portfolio portfolio) {
        return portfolio.getPositions().stream()
            .map(p -> p.getUnrealizedPnL().add(p.getRealizedPnL()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private void publishRiskLimitBreach(TradeOrder order, String violation) {
        RiskLimitBreachedEvent event = new RiskLimitBreachedEvent();
        event.setPortfolioId(order.getPortfolioId());
        event.setRiskType(violation);
        event.setAction("ORDER_REJECTED");
        
        riskEventPublisher.publishRiskLimitBreach(event);
    }
}
```

## 8. Real-time WebSocket Communication

### Trading WebSocket Handler

```java
@Component
public class TradingWebSocketHandler extends TextWebSocketHandler {
    
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> subscriptions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final OrderQueryService orderQueryService;
    private final MarketDataService marketDataService;
    
    public TradingWebSocketHandler(ObjectMapper objectMapper,
                                  OrderQueryService orderQueryService,
                                  MarketDataService marketDataService) {
        this.objectMapper = objectMapper;
        this.orderQueryService = orderQueryService;
        this.marketDataService = marketDataService;
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String traderId = extractTraderId(session);
        sessions.put(traderId, session);
        subscriptions.put(traderId, new HashSet<>());
        
        sendMessage(session, new WelcomeMessage("Connected to Trading System", traderId));
        
        // Send initial portfolio state
        sendPortfolioSnapshot(session, traderId);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String traderId = extractTraderId(session);
        TradingMessage tradingMessage = objectMapper.readValue(message.getPayload(), TradingMessage.class);
        
        switch (tradingMessage.getType()) {
            case "SUBSCRIBE_MARKET_DATA":
                handleMarketDataSubscription(traderId, tradingMessage);
                break;
            case "SUBSCRIBE_ORDER_UPDATES":
                handleOrderUpdateSubscription(traderId, tradingMessage);
                break;
            case "GET_ORDER_BOOK":
                handleOrderBookRequest(session, tradingMessage);
                break;
            case "GET_POSITIONS":
                handlePositionsRequest(session, traderId);
                break;
            default:
                sendError(session, "Unknown message type: " + tradingMessage.getType());
        }
    }
    
    @EventListener
    public void handleMarketDataUpdate(MarketDataUpdateEvent event) {
        MarketData marketData = event.getMarketData();
        
        sessions.entrySet().parallelStream()
            .filter(entry -> {
                Set<String> symbols = subscriptions.get(entry.getKey());
                return symbols != null && symbols.contains(marketData.getSymbol());
            })
            .forEach(entry -> {
                try {
                    sendMessage(entry.getValue(), marketData);
                } catch (Exception e) {
                    System.err.println("Failed to send market data: " + e.getMessage());
                }
            });
    }
    
    @EventListener
    public void handleOrderUpdate(OrderUpdateEvent event) {
        String traderId = event.getTraderId();
        WebSocketSession session = sessions.get(traderId);
        
        if (session != null && session.isOpen()) {
            try {
                sendMessage(session, event);
            } catch (Exception e) {
                System.err.println("Failed to send order update: " + e.getMessage());
            }
        }
    }
    
    @EventListener
    public void handleTradeExecution(TradeExecutedEvent event) {
        // Broadcast trade executions to all connected clients
        broadcastToAll(new TradeNotification(
            event.getSymbol(),
            event.getPrice(),
            event.getQuantity(),
            event.getTimestamp()
        ));
    }
    
    private void handleMarketDataSubscription(String traderId, TradingMessage message) {
        String symbol = message.getData().get("symbol").toString();
        subscriptions.get(traderId).add(symbol);
        
        // Send latest market data
        marketDataService.getLatestMarketData(symbol)
            .subscribe(marketData -> {
                WebSocketSession session = sessions.get(traderId);
                if (session != null) {
                    try {
                        sendMessage(session, marketData);
                    } catch (Exception e) {
                        System.err.println("Failed to send initial market data: " + e.getMessage());
                    }
                }
            });
    }
    
    private void handleOrderUpdateSubscription(String traderId, TradingMessage message) {
        // Send all active orders
        orderQueryService.getActiveOrders()
            .filter(order -> order.getTraderId().equals(traderId))
            .collectList()
            .subscribe(orders -> {
                WebSocketSession session = sessions.get(traderId);
                if (session != null) {
                    try {
                        sendMessage(session, new OrderSnapshot(orders));
                    } catch (Exception e) {
                        System.err.println("Failed to send order snapshot: " + e.getMessage());
                    }
                }
            });
    }
    
    private void sendMessage(WebSocketSession session, Object message) throws Exception {
        if (session.isOpen()) {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        }
    }
    
    private void sendError(WebSocketSession session, String error) throws Exception {
        ErrorMessage errorMessage = new ErrorMessage(error, LocalDateTime.now());
        sendMessage(session, errorMessage);
    }
    
    private void broadcastToAll(Object message) {
        sessions.values().parallelStream()
            .filter(WebSocketSession::isOpen)
            .forEach(session -> {
                try {
                    sendMessage(session, message);
                } catch (Exception e) {
                    System.err.println("Failed to broadcast: " + e.getMessage());
                }
            });
    }
    
    private String extractTraderId(WebSocketSession session) {
        return session.getAttributes().get("traderId").toString();
    }
    
    private void sendPortfolioSnapshot(WebSocketSession session, String traderId) {
        // Implementation to send initial portfolio state
    }
    
    private void handleOrderBookRequest(WebSocketSession session, TradingMessage message) {
        // Implementation to send order book snapshot
    }
    
    private void handlePositionsRequest(WebSocketSession session, String traderId) {
        // Implementation to send current positions
    }
}
```

## 9. Integration Testing

### Trading System Integration Test

```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TradingSystemIntegrationTest {
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));
    
    @Container
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3-management");
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("trading")
            .withUsername("test")
            .withPassword("test");
    
    @Autowired
    private OrderCommandService orderCommandService;
    
    @Autowired
    private MarketDataStreamingService marketDataService;
    
    @Autowired
    private OrderMatchingEngine matchingEngine;
    
    @Test
    void testCompleteOrderFlow() {
        // Given
        CreateOrderCommand buyOrder = new CreateOrderCommand();
        buyOrder.setOrderType(OrderType.LIMIT);
        buyOrder.setSide(OrderSide.BUY);
        buyOrder.setSymbol("AAPL");
        buyOrder.setQuantity(new BigDecimal("100"));
        buyOrder.setPrice(new BigDecimal("150.00"));
        buyOrder.setTraderId("trader1");
        buyOrder.setPortfolioId("portfolio1");
        
        CreateOrderCommand sellOrder = new CreateOrderCommand();
        sellOrder.setOrderType(OrderType.LIMIT);
        sellOrder.setSide(OrderSide.SELL);
        sellOrder.setSymbol("AAPL");
        sellOrder.setQuantity(new BigDecimal("100"));
        sellOrder.setPrice(new BigDecimal("150.00"));
        sellOrder.setTraderId("trader2");
        sellOrder.setPortfolioId("portfolio2");
        
        // When
        StepVerifier.create(
            orderCommandService.createOrder(buyOrder)
                .flatMap(order -> matchingEngine.submitOrder(order))
                .then(orderCommandService.createOrder(sellOrder))
                .flatMap(order -> matchingEngine.submitOrder(order))
        )
        .expectNextCount(1)
        .verifyComplete();
        
        // Then - Verify execution occurred
        // Additional assertions would verify the orders were matched and executed
    }
    
    @Test
    void testMarketDataStreaming() {
        // Given
        Flux<MarketData> marketDataFlux = Flux.interval(Duration.ofMillis(100))
            .take(10)
            .map(i -> {
                MarketData data = new MarketData();
                data.setSymbol("AAPL");
                data.setLastPrice(new BigDecimal("150.00").add(new BigDecimal(i)));
                data.setTimestamp(LocalDateTime.now());
                return data;
            });
        
        // When & Then
        StepVerifier.create(
            marketDataService.publishMarketData(marketDataFlux)
                .thenMany(marketDataService.subscribe1MinuteBars("AAPL").take(1))
        )
        .expectNextCount(1)
        .verifyComplete();
    }
    
    @Test
    void testRiskManagement() {
        // Test risk checks and limit breaches
        // Implementation would verify risk limits are enforced
    }
}
```

## 10. System Monitoring and Dashboard

### System Metrics Collector

```java
@Component
public class TradingSystemMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public TradingSystemMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Register custom metrics
        Gauge.builder("trading.active_orders", this, TradingSystemMetrics::getActiveOrderCount)
            .description("Number of active orders")
            .register(meterRegistry);
        
        Gauge.builder("trading.market_connections", this, TradingSystemMetrics::getMarketConnections)
            .description("Number of active market data connections")
            .register(meterRegistry);
    }
    
    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        meterRegistry.counter("trading.orders.created",
            "symbol", event.getOrder().getSymbol(),
            "side", event.getOrder().getSide().toString()
        ).increment();
    }
    
    @EventListener
    public void onOrderFilled(OrderFilledEvent event) {
        meterRegistry.counter("trading.orders.filled").increment();
        
        meterRegistry.summary("trading.order.fill_price",
            "symbol", event.getSymbol()
        ).record(event.getExecutedPrice().doubleValue());
    }
    
    @EventListener
    public void onTradeExecuted(TradeExecutedEvent event) {
        meterRegistry.counter("trading.trades.executed",
            "symbol", event.getSymbol()
        ).increment();
        
        meterRegistry.summary("trading.trade.volume",
            "symbol", event.getSymbol()
        ).record(event.getQuantity().doubleValue());
    }
    
    @EventListener
    public void onRiskLimitBreached(RiskLimitBreachedEvent event) {
        meterRegistry.counter("trading.risk.breaches",
            "risk_type", event.getRiskType(),
            "portfolio", event.getPortfolioId()
        ).increment();
    }
    
    private double getActiveOrderCount() {
        // Implementation to get active order count
        return 0.0;
    }
    
    private double getMarketConnections() {
        // Implementation to get market connection count
        return 0.0;
    }
}
```

## Summary

This comprehensive Event-Driven Financial Trading System demonstrates:

1. **Event Sourcing & CQRS**: Complete audit trail with separate read/write models
2. **Real-time Market Data**: Kafka Streams for market data processing and analytics
3. **Order Management**: Full order lifecycle with matching engine
4. **Risk Management**: Pre-trade risk checks and position monitoring
5. **Reactive Processing**: Non-blocking, scalable message processing
6. **WebSocket Communication**: Real-time updates to trading clients
7. **Integration Patterns**: Spring Integration for complex workflows
8. **Multiple Message Brokers**: Kafka for streaming, RabbitMQ for commands
9. **Monitoring & Metrics**: Comprehensive system observability
10. **Testing**: Integration testing with containerized dependencies

## Key Architecture Patterns

- **Event-Driven Architecture**: Loose coupling through events
- **Microservices**: Domain-driven service boundaries
- **Event Sourcing**: Complete audit trail and temporal queries
- **CQRS**: Optimized read and write models
- **Saga Pattern**: Distributed transaction management
- **Reactive Streams**: Backpressure and flow control
- **Circuit Breakers**: Resilience and fault tolerance
- **Message Routing**: Intelligent message distribution

## Production Considerations

1. **Security**: Implement OAuth2/JWT for authentication
2. **Compliance**: Add regulatory reporting and audit trails
3. **Scalability**: Kubernetes deployment with auto-scaling
4. **High Availability**: Multi-region deployment
5. **Data Persistence**: Distributed databases and replication
6. **Monitoring**: Prometheus, Grafana, and ELK stack
7. **Performance**: Load testing and optimization

This completes Week 11's comprehensive exploration of messaging and event-driven architectures with a real-world financial trading system implementation.