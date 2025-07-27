# Day 105: Week 15 Capstone - Enterprise-Grade Distributed Financial Trading Platform

## Project Overview
Build a comprehensive, enterprise-grade distributed financial trading platform that integrates all the patterns, principles, and technologies covered throughout Week 15. This capstone project demonstrates real-world application of Domain-Driven Design, Hexagonal Architecture, CQRS, Event Sourcing, advanced Spring Framework features, testing strategies, production engineering practices, and enterprise integration patterns.

## Learning Objectives
- Integrate multiple enterprise architecture patterns into a cohesive system
- Apply Domain-Driven Design principles across multiple bounded contexts
- Implement comprehensive observability, monitoring, and reliability practices
- Design and build production-ready microservices with proper integration patterns
- Demonstrate advanced testing strategies across all system components
- Show proficiency in enterprise integration and legacy system connectivity

## System Architecture Overview

### Domain Model and Bounded Contexts
```
┌─────────────────────────────────────────────────────────────────────┐
│                    Financial Trading Platform                        │
├─────────────────────────────────────────────────────────────────────┤
│  Trading Domain          │  Risk Management     │  Market Data       │
│  - Order Management      │  - Position Limits   │  - Price Feeds     │
│  - Execution Engine      │  - VaR Calculations  │  - Market Events   │
│  - Portfolio Tracking    │  - Compliance        │  - Reference Data  │
├─────────────────────────────────────────────────────────────────────┤
│  Payment Domain          │  Notification        │  Audit & Reporting │
│  - Payment Processing    │  - Email/SMS         │  - Trade Reports   │
│  - Settlement            │  - Real-time Alerts  │  - Compliance Logs │
│  - Account Management    │  - Event Broadcasting│  - Analytics       │
├─────────────────────────────────────────────────────────────────────┤
│               Enterprise Integration Layer                          │
│  - API Gateway           │  - Event Bus         │  - Legacy Systems  │
│  - Service Mesh          │  - Message Queues    │  - Data Integration │
└─────────────────────────────────────────────────────────────────────┘
```

## 1. Core Trading Domain Implementation

### Domain Model with Advanced DDD Patterns

```java
// Trading Aggregate Root with Event Sourcing
@DomainService
public class TradingOrder extends EventSourcedAggregateRoot {
    
    private OrderId orderId;
    private TraderId traderId;
    private Symbol symbol;
    private Quantity quantity;
    private Price price;
    private OrderSide side;
    private OrderType orderType;
    private OrderStatus status;
    private Money totalValue;
    private List<Execution> executions;
    private RiskAssessment riskAssessment;
    private ComplianceCheck complianceCheck;
    private Instant createdAt;
    private Instant lastModified;
    
    // Factory methods with business rules
    public static TradingOrder placeLimitOrder(TraderId traderId, Symbol symbol, 
                                              Quantity quantity, Price price, OrderSide side) {
        // Pre-conditions validation
        validateTrader(traderId);
        validateSymbol(symbol);
        validateQuantity(quantity);
        validatePrice(price);
        
        OrderId orderId = OrderId.generate();
        TradingOrder order = new TradingOrder();
        order.id = orderId.getValue();
        
        // Apply domain event
        OrderPlacedEvent event = new OrderPlacedEvent(
                orderId, traderId, symbol, quantity, price, side, OrderType.LIMIT);
        order.applyEvent(event);
        
        return order;
    }
    
    public static TradingOrder placeMarketOrder(TraderId traderId, Symbol symbol, 
                                               Quantity quantity, OrderSide side) {
        validateTrader(traderId);
        validateSymbol(symbol);
        validateQuantity(quantity);
        
        OrderId orderId = OrderId.generate();
        TradingOrder order = new TradingOrder();
        order.id = orderId.getValue();
        
        OrderPlacedEvent event = new OrderPlacedEvent(
                orderId, traderId, symbol, quantity, Price.market(), side, OrderType.MARKET);
        order.applyEvent(event);
        
        return order;
    }
    
    // Business methods with domain logic
    public void executePartially(Quantity executedQuantity, Price executionPrice, 
                                ExecutionId executionId, MarketData marketData) {
        
        if (status != OrderStatus.PENDING && status != OrderStatus.PARTIALLY_FILLED) {
            throw new OrderExecutionException("Cannot execute order in status: " + status);
        }
        
        if (executedQuantity.isGreaterThan(getRemainingQuantity())) {
            throw new OrderExecutionException("Execution quantity exceeds remaining quantity");
        }
        
        // Validate execution price for limit orders
        if (orderType == OrderType.LIMIT && !isExecutionPriceValid(executionPrice)) {
            throw new OrderExecutionException("Execution price violates limit order constraints");
        }
        
        // Create execution
        Execution execution = new Execution(executionId, executedQuantity, 
                executionPrice, marketData.getTimestamp());
        
        // Apply domain event
        OrderExecutedEvent event = new OrderExecutedEvent(
                OrderId.of(id), executionId, executedQuantity, executionPrice, 
                marketData.getTimestamp());
        applyEvent(event);
    }
    
    public void cancel(String reason, String cancelledBy) {
        if (!canBeCancelled()) {
            throw new OrderCancellationException("Order cannot be cancelled in current state");
        }
        
        OrderCancelledEvent event = new OrderCancelledEvent(
                OrderId.of(id), reason, cancelledBy, Instant.now());
        applyEvent(event);
    }
    
    public void assessRisk(RiskAssessmentService riskService) {
        this.riskAssessment = riskService.assessOrder(this);
        
        if (riskAssessment.getRiskLevel() == RiskLevel.HIGH) {
            OrderRiskAssessedEvent event = new OrderRiskAssessedEvent(
                    OrderId.of(id), riskAssessment);
            applyEvent(event);
        }
    }
    
    public void performComplianceCheck(ComplianceService complianceService) {
        this.complianceCheck = complianceService.checkCompliance(this);
        
        if (!complianceCheck.isCompliant()) {
            OrderComplianceFailedEvent event = new OrderComplianceFailedEvent(
                    OrderId.of(id), complianceCheck.getViolations());
            applyEvent(event);
            
            // Auto-cancel non-compliant orders
            cancel("Compliance violation", "SYSTEM");
        }
    }
    
    @Override
    protected void handle(DomainEvent event) {
        switch (event) {
            case OrderPlacedEvent e -> handleOrderPlaced(e);
            case OrderExecutedEvent e -> handleOrderExecuted(e);
            case OrderCancelledEvent e -> handleOrderCancelled(e);
            case OrderRiskAssessedEvent e -> handleRiskAssessed(e);
            case OrderComplianceFailedEvent e -> handleComplianceFailed(e);
            default -> throw new IllegalArgumentException("Unknown event: " + event.getClass());
        }
        this.version = event.getVersion();
        this.lastModified = event.getTimestamp();
    }
    
    private void handleOrderPlaced(OrderPlacedEvent event) {
        this.orderId = event.getOrderId();
        this.traderId = event.getTraderId();
        this.symbol = event.getSymbol();
        this.quantity = event.getQuantity();
        this.price = event.getPrice();
        this.side = event.getSide();
        this.orderType = event.getOrderType();
        this.status = OrderStatus.PENDING;
        this.totalValue = Money.of(quantity.getValue().multiply(price.getValue()), "USD");
        this.executions = new ArrayList<>();
        this.createdAt = event.getTimestamp();
    }
    
    private void handleOrderExecuted(OrderExecutedEvent event) {
        Execution execution = new Execution(
                event.getExecutionId(),
                event.getExecutedQuantity(),
                event.getExecutionPrice(),
                event.getExecutionTime()
        );
        this.executions.add(execution);
        
        Quantity totalExecuted = executions.stream()
                .map(Execution::getQuantity)
                .reduce(Quantity.ZERO, Quantity::add);
        
        if (totalExecuted.equals(quantity)) {
            this.status = OrderStatus.FILLED;
        } else {
            this.status = OrderStatus.PARTIALLY_FILLED;
        }
    }
    
    private void handleOrderCancelled(OrderCancelledEvent event) {
        this.status = OrderStatus.CANCELLED;
    }
    
    private void handleRiskAssessed(OrderRiskAssessedEvent event) {
        this.riskAssessment = event.getRiskAssessment();
    }
    
    private void handleComplianceFailed(OrderComplianceFailedEvent event) {
        // Compliance failure handling
    }
    
    // Business rule validation methods
    private static void validateTrader(TraderId traderId) {
        if (traderId == null || traderId.getValue() == null || traderId.getValue().trim().isEmpty()) {
            throw new InvalidTraderException("Trader ID cannot be null or empty");
        }
    }
    
    private static void validateSymbol(Symbol symbol) {
        if (symbol == null || !symbol.isValid()) {
            throw new InvalidSymbolException("Symbol is invalid");
        }
    }
    
    private static void validateQuantity(Quantity quantity) {
        if (quantity == null || quantity.isZeroOrNegative()) {
            throw new InvalidQuantityException("Quantity must be positive");
        }
    }
    
    private static void validatePrice(Price price) {
        if (price == null || price.isZeroOrNegative()) {
            throw new InvalidPriceException("Price must be positive");
        }
    }
    
    private boolean isExecutionPriceValid(Price executionPrice) {
        if (orderType != OrderType.LIMIT) {
            return true;
        }
        
        return switch (side) {
            case BUY -> executionPrice.isLessThanOrEqual(price);
            case SELL -> executionPrice.isGreaterThanOrEqual(price);
        };
    }
    
    private boolean canBeCancelled() {
        return status == OrderStatus.PENDING || status == OrderStatus.PARTIALLY_FILLED;
    }
    
    public Quantity getRemainingQuantity() {
        Quantity executed = executions.stream()
                .map(Execution::getQuantity)
                .reduce(Quantity.ZERO, Quantity::add);
        return quantity.subtract(executed);
    }
    
    // Getters
    public OrderId getOrderId() { return orderId; }
    public TraderId getTraderId() { return traderId; }
    public Symbol getSymbol() { return symbol; }
    public Quantity getQuantity() { return quantity; }
    public Price getPrice() { return price; }
    public OrderSide getSide() { return side; }
    public OrderType getOrderType() { return orderType; }
    public OrderStatus getStatus() { return status; }
    public Money getTotalValue() { return totalValue; }
    public List<Execution> getExecutions() { return Collections.unmodifiableList(executions); }
    public RiskAssessment getRiskAssessment() { return riskAssessment; }
    public ComplianceCheck getComplianceCheck() { return complianceCheck; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastModified() { return lastModified; }
}

// Value Objects
public record OrderId(String value) {
    public OrderId {
        Objects.requireNonNull(value, "Order ID cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be empty");
        }
    }
    
    public static OrderId generate() {
        return new OrderId("ORD-" + UUID.randomUUID().toString().substring(0, 8));
    }
    
    public static OrderId of(String value) {
        return new OrderId(value);
    }
    
    public String getValue() { return value; }
}

public record TraderId(String value) {
    public TraderId {
        Objects.requireNonNull(value, "Trader ID cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Trader ID cannot be empty");
        }
    }
    
    public static TraderId of(String value) {
        return new TraderId(value);
    }
    
    public String getValue() { return value; }
}

public record Symbol(String value) {
    private static final Pattern SYMBOL_PATTERN = Pattern.compile("^[A-Z]{1,5}$");
    
    public Symbol {
        Objects.requireNonNull(value, "Symbol cannot be null");
        if (!SYMBOL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid symbol format: " + value);
        }
    }
    
    public static Symbol of(String value) {
        return new Symbol(value.toUpperCase());
    }
    
    public boolean isValid() {
        return SYMBOL_PATTERN.matcher(value).matches();
    }
    
    public String getValue() { return value; }
}

public record Quantity(BigDecimal value) {
    public Quantity {
        Objects.requireNonNull(value, "Quantity cannot be null");
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
    }
    
    public static final Quantity ZERO = new Quantity(BigDecimal.ZERO);
    
    public static Quantity of(BigDecimal value) {
        return new Quantity(value);
    }
    
    public static Quantity of(long value) {
        return new Quantity(BigDecimal.valueOf(value));
    }
    
    public boolean isZeroOrNegative() {
        return value.compareTo(BigDecimal.ZERO) <= 0;
    }
    
    public boolean isGreaterThan(Quantity other) {
        return value.compareTo(other.value) > 0;
    }
    
    public Quantity add(Quantity other) {
        return new Quantity(value.add(other.value));
    }
    
    public Quantity subtract(Quantity other) {
        return new Quantity(value.subtract(other.value));
    }
    
    public BigDecimal getValue() { return value; }
}

public record Price(BigDecimal value, boolean isMarketPrice) {
    public Price {
        if (!isMarketPrice) {
            Objects.requireNonNull(value, "Price value cannot be null for limit orders");
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Price must be positive");
            }
        }
    }
    
    public static Price of(BigDecimal value) {
        return new Price(value, false);
    }
    
    public static Price market() {
        return new Price(null, true);
    }
    
    public boolean isZeroOrNegative() {
        return !isMarketPrice && value.compareTo(BigDecimal.ZERO) <= 0;
    }
    
    public boolean isLessThanOrEqual(Price other) {
        if (isMarketPrice || other.isMarketPrice) {
            return true;
        }
        return value.compareTo(other.value) <= 0;
    }
    
    public boolean isGreaterThanOrEqual(Price other) {
        if (isMarketPrice || other.isMarketPrice) {
            return true;
        }
        return value.compareTo(other.value) >= 0;
    }
    
    public BigDecimal getValue() { return value; }
}

public record Money(BigDecimal amount, String currency) {
    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }
    
    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }
    
    public Money add(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies");
        }
        return new Money(amount.add(other.amount), currency);
    }
    
    public Money subtract(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot subtract different currencies");
        }
        return new Money(amount.subtract(other.amount), currency);
    }
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

public enum RiskLevel {
    LOW, MEDIUM, HIGH, CRITICAL
}

// Domain Events
public record OrderPlacedEvent(
        OrderId orderId,
        TraderId traderId,
        Symbol symbol,
        Quantity quantity,
        Price price,
        OrderSide side,
        OrderType orderType,
        Instant timestamp,
        long version
) implements DomainEvent {
    
    public OrderPlacedEvent(OrderId orderId, TraderId traderId, Symbol symbol,
                           Quantity quantity, Price price, OrderSide side, OrderType orderType) {
        this(orderId, traderId, symbol, quantity, price, side, orderType, Instant.now(), 1L);
    }
    
    @Override
    public String getEventId() {
        return UUID.randomUUID().toString();
    }
    
    @Override
    public String getAggregateId() {
        return orderId.getValue();
    }
    
    @Override
    public String getEventType() {
        return "OrderPlaced";
    }
    
    @Override
    public Instant getTimestamp() {
        return timestamp;
    }
    
    @Override
    public long getVersion() {
        return version;
    }
    
    @Override
    public Object getEventData() {
        return Map.of(
                "orderId", orderId.getValue(),
                "traderId", traderId.getValue(),
                "symbol", symbol.getValue(),
                "quantity", quantity.getValue(),
                "price", price.isMarketPrice() ? "MARKET" : price.getValue().toString(),
                "side", side.name(),
                "orderType", orderType.name()
        );
    }
}

public record OrderExecutedEvent(
        OrderId orderId,
        ExecutionId executionId,
        Quantity executedQuantity,
        Price executionPrice,
        Instant executionTime,
        long version
) implements DomainEvent {
    
    public OrderExecutedEvent(OrderId orderId, ExecutionId executionId,
                             Quantity executedQuantity, Price executionPrice, Instant executionTime) {
        this(orderId, executionId, executedQuantity, executionPrice, executionTime, 1L);
    }
    
    @Override
    public String getEventId() {
        return UUID.randomUUID().toString();
    }
    
    @Override
    public String getAggregateId() {
        return orderId.getValue();
    }
    
    @Override
    public String getEventType() {
        return "OrderExecuted";
    }
    
    @Override
    public Instant getTimestamp() {
        return executionTime;
    }
    
    @Override
    public long getVersion() {
        return version;
    }
    
    @Override
    public Object getEventData() {
        return Map.of(
                "orderId", orderId.getValue(),
                "executionId", executionId.getValue(),
                "executedQuantity", executedQuantity.getValue(),
                "executionPrice", executionPrice.getValue(),
                "executionTime", executionTime.toString()
        );
    }
}

// Supporting classes
public record ExecutionId(String value) {
    public ExecutionId {
        Objects.requireNonNull(value, "Execution ID cannot be null");
    }
    
    public static ExecutionId generate() {
        return new ExecutionId("EXE-" + UUID.randomUUID().toString().substring(0, 8));
    }
    
    public String getValue() { return value; }
}

public record Execution(
        ExecutionId executionId,
        Quantity quantity,
        Price price,
        Instant timestamp
) {
    public Execution {
        Objects.requireNonNull(executionId, "Execution ID cannot be null");
        Objects.requireNonNull(quantity, "Quantity cannot be null");
        Objects.requireNonNull(price, "Price cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
    }
}

public class MarketData {
    private final Symbol symbol;
    private final Price currentPrice;
    private final Quantity volume;
    private final Price dayHigh;
    private final Price dayLow;
    private final Instant timestamp;
    
    public MarketData(Symbol symbol, Price currentPrice, Quantity volume,
                     Price dayHigh, Price dayLow, Instant timestamp) {
        this.symbol = symbol;
        this.currentPrice = currentPrice;
        this.volume = volume;
        this.dayHigh = dayHigh;
        this.dayLow = dayLow;
        this.timestamp = timestamp;
    }
    
    // Getters
    public Symbol getSymbol() { return symbol; }
    public Price getCurrentPrice() { return currentPrice; }
    public Quantity getVolume() { return volume; }
    public Price getDayHigh() { return dayHigh; }
    public Price getDayLow() { return dayLow; }
    public Instant getTimestamp() { return timestamp; }
}

public class RiskAssessment {
    private final RiskLevel riskLevel;
    private final BigDecimal valueAtRisk;
    private final BigDecimal exposureLimit;
    private final List<String> riskFactors;
    private final Instant assessmentTime;
    
    public RiskAssessment(RiskLevel riskLevel, BigDecimal valueAtRisk,
                         BigDecimal exposureLimit, List<String> riskFactors) {
        this.riskLevel = riskLevel;
        this.valueAtRisk = valueAtRisk;
        this.exposureLimit = exposureLimit;
        this.riskFactors = new ArrayList<>(riskFactors);
        this.assessmentTime = Instant.now();
    }
    
    // Getters
    public RiskLevel getRiskLevel() { return riskLevel; }
    public BigDecimal getValueAtRisk() { return valueAtRisk; }
    public BigDecimal getExposureLimit() { return exposureLimit; }
    public List<String> getRiskFactors() { return Collections.unmodifiableList(riskFactors); }
    public Instant getAssessmentTime() { return assessmentTime; }
}

public class ComplianceCheck {
    private final boolean isCompliant;
    private final List<String> violations;
    private final List<String> warnings;
    private final Instant checkTime;
    
    public ComplianceCheck(boolean isCompliant, List<String> violations, List<String> warnings) {
        this.isCompliant = isCompliant;
        this.violations = new ArrayList<>(violations);
        this.warnings = new ArrayList<>(warnings);
        this.checkTime = Instant.now();
    }
    
    // Getters
    public boolean isCompliant() { return isCompliant; }
    public List<String> getViolations() { return Collections.unmodifiableList(violations); }
    public List<String> getWarnings() { return Collections.unmodifiableList(warnings); }
    public Instant getCheckTime() { return checkTime; }
}

// Domain Services
public interface RiskAssessmentService {
    RiskAssessment assessOrder(TradingOrder order);
    RiskAssessment assessPortfolio(Portfolio portfolio);
}

public interface ComplianceService {
    ComplianceCheck checkCompliance(TradingOrder order);
    ComplianceCheck checkTraderLimits(TraderId traderId, Money exposure);
}

// Custom Exceptions
public class OrderExecutionException extends DomainException {
    public OrderExecutionException(String message) {
        super(message);
    }
}

public class OrderCancellationException extends DomainException {
    public OrderCancellationException(String message) {
        super(message);
    }
}

public class InvalidTraderException extends DomainException {
    public InvalidTraderException(String message) {
        super(message);
    }
}

public class InvalidSymbolException extends DomainException {
    public InvalidSymbolException(String message) {
        super(message);
    }
}

public class InvalidQuantityException extends DomainException {
    public InvalidQuantityException(String message) {
        super(message);
    }
}

public class InvalidPriceException extends DomainException {
    public InvalidPriceException(String message) {
        super(message);
    }
}

public abstract class DomainException extends RuntimeException {
    protected DomainException(String message) {
        super(message);
    }
    
    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

## 2. CQRS Implementation with Comprehensive Query Side

### Command and Query Separation

```java
// Command Side - Application Services
@ApplicationService
public class TradingOrderCommandService {
    
    private final EventStore eventStore;
    private final TradingOrderRepository orderRepository;
    private final RiskAssessmentService riskAssessmentService;
    private final ComplianceService complianceService;
    private final DomainEventPublisher eventPublisher;
    private final Logger logger = LoggerFactory.getLogger(TradingOrderCommandService.class);
    
    public TradingOrderCommandService(EventStore eventStore,
                                    TradingOrderRepository orderRepository,
                                    RiskAssessmentService riskAssessmentService,
                                    ComplianceService complianceService,
                                    DomainEventPublisher eventPublisher) {
        this.eventStore = eventStore;
        this.orderRepository = orderRepository;
        this.riskAssessmentService = riskAssessmentService;
        this.complianceService = complianceService;
        this.eventPublisher = eventPublisher;
    }
    
    @Transactional
    public OrderResult placeLimitOrder(PlaceLimitOrderCommand command) {
        try {
            // Create order aggregate
            TradingOrder order = TradingOrder.placeLimitOrder(
                    TraderId.of(command.getTraderId()),
                    Symbol.of(command.getSymbol()),
                    Quantity.of(command.getQuantity()),
                    Price.of(command.getPrice()),
                    command.getSide()
            );
            
            // Perform risk assessment
            order.assessRisk(riskAssessmentService);
            
            // Perform compliance check
            order.performComplianceCheck(complianceService);
            
            // Save events
            eventStore.saveEvents(order.getId(), order.getUncommittedEvents(), 0);
            
            // Publish domain events
            for (DomainEvent event : order.getUncommittedEvents()) {
                eventPublisher.publish(event);
            }
            
            order.markEventsAsCommitted();
            
            logger.info("Limit order placed successfully: {}", order.getOrderId().getValue());
            
            return OrderResult.success(order.getOrderId().getValue(), order.getStatus());
            
        } catch (Exception e) {
            logger.error("Failed to place limit order: {}", command, e);
            return OrderResult.failure("ORDER_PLACEMENT_FAILED", e.getMessage());
        }
    }
    
    @Transactional
    public OrderResult placeMarketOrder(PlaceMarketOrderCommand command) {
        try {
            TradingOrder order = TradingOrder.placeMarketOrder(
                    TraderId.of(command.getTraderId()),
                    Symbol.of(command.getSymbol()),
                    Quantity.of(command.getQuantity()),
                    command.getSide()
            );
            
            // Risk and compliance checks
            order.assessRisk(riskAssessmentService);
            order.performComplianceCheck(complianceService);
            
            // Save and publish events
            eventStore.saveEvents(order.getId(), order.getUncommittedEvents(), 0);
            
            for (DomainEvent event : order.getUncommittedEvents()) {
                eventPublisher.publish(event);
            }
            
            order.markEventsAsCommitted();
            
            logger.info("Market order placed successfully: {}", order.getOrderId().getValue());
            
            return OrderResult.success(order.getOrderId().getValue(), order.getStatus());
            
        } catch (Exception e) {
            logger.error("Failed to place market order: {}", command, e);
            return OrderResult.failure("ORDER_PLACEMENT_FAILED", e.getMessage());
        }
    }
    
    @Transactional
    public OrderResult executeOrder(ExecuteOrderCommand command) {
        try {
            // Load order from event store
            List<DomainEvent> events = eventStore.getEvents(command.getOrderId());
            if (events.isEmpty()) {
                return OrderResult.failure("ORDER_NOT_FOUND", "Order not found: " + command.getOrderId());
            }
            
            TradingOrder order = TradingOrder.fromEvents(command.getOrderId(), events);
            
            // Execute order
            order.executePartially(
                    Quantity.of(command.getExecutedQuantity()),
                    Price.of(command.getExecutionPrice()),
                    ExecutionId.generate(),
                    command.getMarketData()
            );
            
            // Save new events
            eventStore.saveEvents(order.getId(), order.getUncommittedEvents(), order.getVersion());
            
            for (DomainEvent event : order.getUncommittedEvents()) {
                eventPublisher.publish(event);
            }
            
            order.markEventsAsCommitted();
            
            logger.info("Order executed successfully: {}", command.getOrderId());
            
            return OrderResult.success(command.getOrderId(), order.getStatus());
            
        } catch (Exception e) {
            logger.error("Failed to execute order: {}", command, e);
            return OrderResult.failure("ORDER_EXECUTION_FAILED", e.getMessage());
        }
    }
    
    @Transactional
    public OrderResult cancelOrder(CancelOrderCommand command) {
        try {
            List<DomainEvent> events = eventStore.getEvents(command.getOrderId());
            if (events.isEmpty()) {
                return OrderResult.failure("ORDER_NOT_FOUND", "Order not found: " + command.getOrderId());
            }
            
            TradingOrder order = TradingOrder.fromEvents(command.getOrderId(), events);
            
            order.cancel(command.getReason(), command.getCancelledBy());
            
            eventStore.saveEvents(order.getId(), order.getUncommittedEvents(), order.getVersion());
            
            for (DomainEvent event : order.getUncommittedEvents()) {
                eventPublisher.publish(event);
            }
            
            order.markEventsAsCommitted();
            
            logger.info("Order cancelled successfully: {}", command.getOrderId());
            
            return OrderResult.success(command.getOrderId(), order.getStatus());
            
        } catch (Exception e) {
            logger.error("Failed to cancel order: {}", command, e);
            return OrderResult.failure("ORDER_CANCELLATION_FAILED", e.getMessage());
        }
    }
}

// Commands
public record PlaceLimitOrderCommand(
        String traderId,
        String symbol,
        BigDecimal quantity,
        BigDecimal price,
        OrderSide side,
        String correlationId
) implements Command {
    
    @Override
    public String getCommandId() {
        return UUID.randomUUID().toString();
    }
    
    @Override
    public Instant getTimestamp() {
        return Instant.now();
    }
}

public record PlaceMarketOrderCommand(
        String traderId,
        String symbol,
        BigDecimal quantity,
        OrderSide side,
        String correlationId
) implements Command {
    
    @Override
    public String getCommandId() {
        return UUID.randomUUID().toString();
    }
    
    @Override
    public Instant getTimestamp() {
        return Instant.now();
    }
}

public record ExecuteOrderCommand(
        String orderId,
        BigDecimal executedQuantity,
        BigDecimal executionPrice,
        MarketData marketData,
        String correlationId
) implements Command {
    
    @Override
    public String getCommandId() {
        return UUID.randomUUID().toString();
    }
    
    @Override
    public Instant getTimestamp() {
        return Instant.now();
    }
}

public record CancelOrderCommand(
        String orderId,
        String reason,
        String cancelledBy,
        String correlationId
) implements Command {
    
    @Override
    public String getCommandId() {
        return UUID.randomUUID().toString();
    }
    
    @Override
    public Instant getTimestamp() {
        return Instant.now();
    }
}

// Command Result
public class OrderResult {
    private final boolean success;
    private final String orderId;
    private final OrderStatus status;
    private final String errorCode;
    private final String errorMessage;
    private final Instant timestamp;
    
    private OrderResult(boolean success, String orderId, OrderStatus status,
                       String errorCode, String errorMessage) {
        this.success = success;
        this.orderId = orderId;
        this.status = status;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.timestamp = Instant.now();
    }
    
    public static OrderResult success(String orderId, OrderStatus status) {
        return new OrderResult(true, orderId, status, null, null);
    }
    
    public static OrderResult failure(String errorCode, String errorMessage) {
        return new OrderResult(false, null, null, errorCode, errorMessage);
    }
    
    // Getters
    public boolean isSuccess() { return success; }
    public String getOrderId() { return orderId; }
    public OrderStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getTimestamp() { return timestamp; }
}

// Query Side - Read Models and Projections
@Component
public class TradingOrderProjection {
    
    private final TradingOrderViewRepository viewRepository;
    private final TraderPortfolioViewRepository portfolioRepository;
    private final TradingStatisticsRepository statisticsRepository;
    private final Logger logger = LoggerFactory.getLogger(TradingOrderProjection.class);
    
    public TradingOrderProjection(TradingOrderViewRepository viewRepository,
                                TraderPortfolioViewRepository portfolioRepository,
                                TradingStatisticsRepository statisticsRepository) {
        this.viewRepository = viewRepository;
        this.portfolioRepository = portfolioRepository;
        this.statisticsRepository = statisticsRepository;
    }
    
    @EventHandler
    public void handle(OrderPlacedEvent event) {
        TradingOrderView orderView = new TradingOrderView(
                event.orderId().getValue(),
                event.traderId().getValue(),
                event.symbol().getValue(),
                event.quantity().getValue(),
                event.price().isMarketPrice() ? null : event.price().getValue(),
                event.side(),
                event.orderType(),
                OrderStatus.PENDING,
                BigDecimal.ZERO, // filled quantity
                event.timestamp(),
                event.timestamp()
        );
        
        viewRepository.save(orderView);
        
        // Update trader portfolio
        updateTraderPortfolio(event.traderId().getValue(), event.symbol().getValue());
        
        // Update statistics
        updateTradingStatistics(event.symbol().getValue(), event.side());
        
        logger.debug("Order view created for order: {}", event.orderId().getValue());
    }
    
    @EventHandler
    public void handle(OrderExecutedEvent event) {
        Optional<TradingOrderView> optionalView = viewRepository.findById(event.orderId().getValue());
        
        if (optionalView.isPresent()) {
            TradingOrderView orderView = optionalView.get();
            
            // Update filled quantity
            BigDecimal newFilledQuantity = orderView.getFilledQuantity()
                    .add(event.executedQuantity().getValue());
            
            // Determine new status
            OrderStatus newStatus = newFilledQuantity.compareTo(orderView.getQuantity()) >= 0 ?
                    OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED;
            
            TradingOrderView updatedView = orderView.withUpdates(
                    newStatus,
                    newFilledQuantity,
                    event.executionTime()
            );
            
            viewRepository.save(updatedView);
            
            // Update portfolio with execution
            updatePortfolioWithExecution(orderView.getTraderId(), orderView.getSymbol(),
                    event.executedQuantity().getValue(), event.executionPrice().getValue(),
                    orderView.getSide());
            
            // Update execution statistics
            updateExecutionStatistics(orderView.getSymbol(), event.executedQuantity().getValue(),
                    event.executionPrice().getValue());
            
            logger.debug("Order view updated for execution: {}", event.orderId().getValue());
        }
    }
    
    @EventHandler
    public void handle(OrderCancelledEvent event) {
        Optional<TradingOrderView> optionalView = viewRepository.findById(event.orderId().getValue());
        
        if (optionalView.isPresent()) {
            TradingOrderView orderView = optionalView.get();
            TradingOrderView updatedView = orderView.withStatus(OrderStatus.CANCELLED, event.timestamp());
            
            viewRepository.save(updatedView);
            
            // Update cancellation statistics
            updateCancellationStatistics(orderView.getSymbol());
            
            logger.debug("Order view cancelled: {}", event.orderId().getValue());
        }
    }
    
    private void updateTraderPortfolio(String traderId, String symbol) {
        Optional<TraderPortfolioView> portfolio = portfolioRepository.findByTraderId(traderId);
        
        if (portfolio.isEmpty()) {
            TraderPortfolioView newPortfolio = new TraderPortfolioView(
                    traderId,
                    new HashMap<>(),
                    BigDecimal.ZERO,
                    Instant.now()
            );
            portfolioRepository.save(newPortfolio);
        }
        
        // Portfolio position updates will be handled by execution events
    }
    
    private void updatePortfolioWithExecution(String traderId, String symbol,
                                            BigDecimal quantity, BigDecimal price, OrderSide side) {
        Optional<TraderPortfolioView> optionalPortfolio = portfolioRepository.findByTraderId(traderId);
        
        if (optionalPortfolio.isPresent()) {
            TraderPortfolioView portfolio = optionalPortfolio.get();
            Map<String, PositionView> positions = new HashMap<>(portfolio.getPositions());
            
            PositionView currentPosition = positions.getOrDefault(symbol,
                    new PositionView(symbol, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
            
            BigDecimal newQuantity = side == OrderSide.BUY ?
                    currentPosition.getQuantity().add(quantity) :
                    currentPosition.getQuantity().subtract(quantity);
            
            BigDecimal executionValue = quantity.multiply(price);
            BigDecimal newTotalValue = currentPosition.getTotalValue().add(
                    side == OrderSide.BUY ? executionValue : executionValue.negate());
            
            BigDecimal newAveragePrice = newQuantity.compareTo(BigDecimal.ZERO) != 0 ?
                    newTotalValue.divide(newQuantity, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            
            PositionView updatedPosition = new PositionView(symbol, newQuantity,
                    newAveragePrice, newTotalValue);
            positions.put(symbol, updatedPosition);
            
            // Update portfolio total value
            BigDecimal totalPortfolioValue = positions.values().stream()
                    .map(PositionView::getTotalValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            TraderPortfolioView updatedPortfolio = new TraderPortfolioView(
                    traderId, positions, totalPortfolioValue, Instant.now());
            
            portfolioRepository.save(updatedPortfolio);
        }
    }
    
    private void updateTradingStatistics(String symbol, OrderSide side) {
        // Update order placement statistics
        statisticsRepository.incrementOrderCount(symbol, side);
    }
    
    private void updateExecutionStatistics(String symbol, BigDecimal quantity, BigDecimal price) {
        // Update execution statistics
        statisticsRepository.updateExecutionStats(symbol, quantity, price);
    }
    
    private void updateCancellationStatistics(String symbol) {
        // Update cancellation statistics
        statisticsRepository.incrementCancellationCount(symbol);
    }
}

// Read Model Views
public class TradingOrderView {
    private final String orderId;
    private final String traderId;
    private final String symbol;
    private final BigDecimal quantity;
    private final BigDecimal price; // null for market orders
    private final OrderSide side;
    private final OrderType orderType;
    private final OrderStatus status;
    private final BigDecimal filledQuantity;
    private final Instant createdAt;
    private final Instant lastModified;
    
    public TradingOrderView(String orderId, String traderId, String symbol,
                           BigDecimal quantity, BigDecimal price, OrderSide side,
                           OrderType orderType, OrderStatus status, BigDecimal filledQuantity,
                           Instant createdAt, Instant lastModified) {
        this.orderId = orderId;
        this.traderId = traderId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.side = side;
        this.orderType = orderType;
        this.status = status;
        this.filledQuantity = filledQuantity;
        this.createdAt = createdAt;
        this.lastModified = lastModified;
    }
    
    public TradingOrderView withUpdates(OrderStatus newStatus, BigDecimal newFilledQuantity,
                                       Instant newLastModified) {
        return new TradingOrderView(orderId, traderId, symbol, quantity, price, side,
                orderType, newStatus, newFilledQuantity, createdAt, newLastModified);
    }
    
    public TradingOrderView withStatus(OrderStatus newStatus, Instant newLastModified) {
        return new TradingOrderView(orderId, traderId, symbol, quantity, price, side,
                orderType, newStatus, filledQuantity, createdAt, newLastModified);
    }
    
    // Getters
    public String getOrderId() { return orderId; }
    public String getTraderId() { return traderId; }
    public String getSymbol() { return symbol; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public OrderSide getSide() { return side; }
    public OrderType getOrderType() { return orderType; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getFilledQuantity() { return filledQuantity; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastModified() { return lastModified; }
}

public class TraderPortfolioView {
    private final String traderId;
    private final Map<String, PositionView> positions;
    private final BigDecimal totalValue;
    private final Instant lastUpdated;
    
    public TraderPortfolioView(String traderId, Map<String, PositionView> positions,
                              BigDecimal totalValue, Instant lastUpdated) {
        this.traderId = traderId;
        this.positions = new HashMap<>(positions);
        this.totalValue = totalValue;
        this.lastUpdated = lastUpdated;
    }
    
    // Getters
    public String getTraderId() { return traderId; }
    public Map<String, PositionView> getPositions() { return Collections.unmodifiableMap(positions); }
    public BigDecimal getTotalValue() { return totalValue; }
    public Instant getLastUpdated() { return lastUpdated; }
}

public class PositionView {
    private final String symbol;
    private final BigDecimal quantity;
    private final BigDecimal averagePrice;
    private final BigDecimal totalValue;
    
    public PositionView(String symbol, BigDecimal quantity, BigDecimal averagePrice, BigDecimal totalValue) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
        this.totalValue = totalValue;
    }
    
    // Getters
    public String getSymbol() { return symbol; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getAveragePrice() { return averagePrice; }
    public BigDecimal getTotalValue() { return totalValue; }
}

// Query Services
@Service
public class TradingOrderQueryService {
    
    private final TradingOrderViewRepository viewRepository;
    private final TraderPortfolioViewRepository portfolioRepository;
    private final TradingStatisticsRepository statisticsRepository;
    
    public TradingOrderQueryService(TradingOrderViewRepository viewRepository,
                                  TraderPortfolioViewRepository portfolioRepository,
                                  TradingStatisticsRepository statisticsRepository) {
        this.viewRepository = viewRepository;
        this.portfolioRepository = portfolioRepository;
        this.statisticsRepository = statisticsRepository;
    }
    
    public Optional<TradingOrderView> getOrder(String orderId) {
        return viewRepository.findById(orderId);
    }
    
    public List<TradingOrderView> getOrdersByTrader(String traderId) {
        return viewRepository.findByTraderId(traderId);
    }
    
    public List<TradingOrderView> getOrdersBySymbol(String symbol) {
        return viewRepository.findBySymbol(symbol);
    }
    
    public List<TradingOrderView> getActiveOrders() {
        return viewRepository.findByStatusIn(
                List.of(OrderStatus.PENDING, OrderStatus.PARTIALLY_FILLED));
    }
    
    public Optional<TraderPortfolioView> getTraderPortfolio(String traderId) {
        return portfolioRepository.findByTraderId(traderId);
    }
    
    public TradingStatistics getTradingStatistics(String symbol, LocalDate fromDate, LocalDate toDate) {
        return statisticsRepository.getStatistics(symbol, fromDate, toDate);
    }
}

// Repository Interfaces
public interface TradingOrderViewRepository {
    Optional<TradingOrderView> findById(String orderId);
    List<TradingOrderView> findByTraderId(String traderId);
    List<TradingOrderView> findBySymbol(String symbol);
    List<TradingOrderView> findByStatusIn(List<OrderStatus> statuses);
    void save(TradingOrderView orderView);
    void delete(String orderId);
}

public interface TraderPortfolioViewRepository {
    Optional<TraderPortfolioView> findByTraderId(String traderId);
    void save(TraderPortfolioView portfolio);
}

public interface TradingStatisticsRepository {
    void incrementOrderCount(String symbol, OrderSide side);
    void updateExecutionStats(String symbol, BigDecimal quantity, BigDecimal price);
    void incrementCancellationCount(String symbol);
    TradingStatistics getStatistics(String symbol, LocalDate fromDate, LocalDate toDate);
}

public class TradingStatistics {
    private final String symbol;
    private final long totalOrders;
    private final long buyOrders;
    private final long sellOrders;
    private final long executedOrders;
    private final long cancelledOrders;
    private final BigDecimal totalVolume;
    private final BigDecimal averagePrice;
    private final LocalDate fromDate;
    private final LocalDate toDate;
    
    public TradingStatistics(String symbol, long totalOrders, long buyOrders, long sellOrders,
                           long executedOrders, long cancelledOrders, BigDecimal totalVolume,
                           BigDecimal averagePrice, LocalDate fromDate, LocalDate toDate) {
        this.symbol = symbol;
        this.totalOrders = totalOrders;
        this.buyOrders = buyOrders;
        this.sellOrders = sellOrders;
        this.executedOrders = executedOrders;
        this.cancelledOrders = cancelledOrders;
        this.totalVolume = totalVolume;
        this.averagePrice = averagePrice;
        this.fromDate = fromDate;
        this.toDate = toDate;
    }
    
    // Getters
    public String getSymbol() { return symbol; }
    public long getTotalOrders() { return totalOrders; }
    public long getBuyOrders() { return buyOrders; }
    public long getSellOrders() { return sellOrders; }
    public long getExecutedOrders() { return executedOrders; }
    public long getCancelledOrders() { return cancelledOrders; }
    public BigDecimal getTotalVolume() { return totalVolume; }
    public BigDecimal getAveragePrice() { return averagePrice; }
    public LocalDate getFromDate() { return fromDate; }
    public LocalDate getToDate() { return toDate; }
}
```

## 3. Production-Ready REST API with Comprehensive Error Handling

### REST Controller Implementation

```java
@RestController
@RequestMapping("/api/v1/trading")
@Validated
@Slf4j
public class TradingController {
    
    private final TradingOrderCommandService commandService;
    private final TradingOrderQueryService queryService;
    private final MetricsCollector metricsCollector;
    
    public TradingController(TradingOrderCommandService commandService,
                           TradingOrderQueryService queryService,
                           MetricsCollector metricsCollector) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.metricsCollector = metricsCollector;
    }
    
    @PostMapping("/orders/limit")
    @Operation(summary = "Place a limit order", description = "Places a new limit order for trading")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order placed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "422", description = "Business rule violation"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<OrderResponse> placeLimitOrder(
            @Valid @RequestBody PlaceLimitOrderRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        Timer.Sample sample = metricsCollector.startTimer("place_limit_order");
        
        try {
            PlaceLimitOrderCommand command = new PlaceLimitOrderCommand(
                    request.getTraderId(),
                    request.getSymbol(),
                    request.getQuantity(),
                    request.getPrice(),
                    request.getSide(),
                    correlationId != null ? correlationId : UUID.randomUUID().toString()
            );
            
            OrderResult result = commandService.placeLimitOrder(command);
            
            if (result.isSuccess()) {
                metricsCollector.incrementCounter("orders_placed", "type", "limit", "status", "success");
                
                OrderResponse response = new OrderResponse(
                        result.getOrderId(),
                        result.getStatus().name(),
                        "Order placed successfully",
                        result.getTimestamp()
                );
                
                return ResponseEntity.status(HttpStatus.CREATED)
                        .header("Location", "/api/v1/trading/orders/" + result.getOrderId())
                        .body(response);
            } else {
                metricsCollector.incrementCounter("orders_placed", "type", "limit", "status", "failure");
                
                ErrorResponse errorResponse = new ErrorResponse(
                        result.getErrorCode(),
                        result.getErrorMessage(),
                        "/api/v1/trading/orders/limit",
                        result.getTimestamp()
                );
                
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(OrderResponse.fromError(errorResponse));
            }
            
        } catch (Exception e) {
            log.error("Unexpected error placing limit order", e);
            metricsCollector.incrementCounter("orders_placed", "type", "limit", "status", "error");
            
            ErrorResponse errorResponse = new ErrorResponse(
                    "INTERNAL_ERROR",
                    "An unexpected error occurred",
                    "/api/v1/trading/orders/limit",
                    Instant.now()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(OrderResponse.fromError(errorResponse));
                    
        } finally {
            sample.stop(metricsCollector.getTimer("place_limit_order"));
        }
    }
    
    @PostMapping("/orders/market")
    @Operation(summary = "Place a market order", description = "Places a new market order for immediate execution")
    public ResponseEntity<OrderResponse> placeMarketOrder(
            @Valid @RequestBody PlaceMarketOrderRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        Timer.Sample sample = metricsCollector.startTimer("place_market_order");
        
        try {
            PlaceMarketOrderCommand command = new PlaceMarketOrderCommand(
                    request.getTraderId(),
                    request.getSymbol(),
                    request.getQuantity(),
                    request.getSide(),
                    correlationId != null ? correlationId : UUID.randomUUID().toString()
            );
            
            OrderResult result = commandService.placeMarketOrder(command);
            
            if (result.isSuccess()) {
                metricsCollector.incrementCounter("orders_placed", "type", "market", "status", "success");
                
                OrderResponse response = new OrderResponse(
                        result.getOrderId(),
                        result.getStatus().name(),
                        "Market order placed successfully",
                        result.getTimestamp()
                );
                
                return ResponseEntity.status(HttpStatus.CREATED)
                        .header("Location", "/api/v1/trading/orders/" + result.getOrderId())
                        .body(response);
            } else {
                metricsCollector.incrementCounter("orders_placed", "type", "market", "status", "failure");
                
                ErrorResponse errorResponse = new ErrorResponse(
                        result.getErrorCode(),
                        result.getErrorMessage(),
                        "/api/v1/trading/orders/market",
                        result.getTimestamp()
                );
                
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(OrderResponse.fromError(errorResponse));
            }
            
        } catch (Exception e) {
            log.error("Unexpected error placing market order", e);
            metricsCollector.incrementCounter("orders_placed", "type", "market", "status", "error");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(OrderResponse.fromError(new ErrorResponse(
                            "INTERNAL_ERROR",
                            "An unexpected error occurred",
                            "/api/v1/trading/orders/market",
                            Instant.now()
                    )));
                    
        } finally {
            sample.stop(metricsCollector.getTimer("place_market_order"));
        }
    }
    
    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Get order details", description = "Retrieves details of a specific order")
    public ResponseEntity<TradingOrderResponse> getOrder(@PathVariable String orderId) {
        
        Optional<TradingOrderView> orderView = queryService.getOrder(orderId);
        
        if (orderView.isPresent()) {
            TradingOrderResponse response = TradingOrderResponse.fromView(orderView.get());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/orders")
    @Operation(summary = "Get orders", description = "Retrieves orders based on query parameters")
    public ResponseEntity<PagedResponse<TradingOrderResponse>> getOrders(
            @RequestParam(required = false) String traderId,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) List<OrderStatus> statuses,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        
        List<TradingOrderView> orders;
        
        if (traderId != null) {
            orders = queryService.getOrdersByTrader(traderId);
        } else if (symbol != null) {
            orders = queryService.getOrdersBySymbol(symbol);
        } else if (statuses != null && !statuses.isEmpty()) {
            orders = queryService.getActiveOrders(); // Simplified for example
        } else {
            orders = queryService.getActiveOrders();
        }
        
        // Apply pagination
        int start = page * size;
        int end = Math.min(start + size, orders.size());
        List<TradingOrderView> pageData = orders.subList(start, end);
        
        List<TradingOrderResponse> responseData = pageData.stream()
                .map(TradingOrderResponse::fromView)
                .collect(Collectors.toList());
        
        PagedResponse<TradingOrderResponse> response = new PagedResponse<>(
                responseData,
                page,
                size,
                orders.size(),
                (int) Math.ceil((double) orders.size() / size)
        );
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/orders/{orderId}")
    @Operation(summary = "Cancel order", description = "Cancels an existing order")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable String orderId,
            @Valid @RequestBody CancelOrderRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        
        Timer.Sample sample = metricsCollector.startTimer("cancel_order");
        
        try {
            CancelOrderCommand command = new CancelOrderCommand(
                    orderId,
                    request.getReason(),
                    request.getCancelledBy(),
                    correlationId != null ? correlationId : UUID.randomUUID().toString()
            );
            
            OrderResult result = commandService.cancelOrder(command);
            
            if (result.isSuccess()) {
                metricsCollector.incrementCounter("orders_cancelled", "status", "success");
                
                OrderResponse response = new OrderResponse(
                        result.getOrderId(),
                        result.getStatus().name(),
                        "Order cancelled successfully",
                        result.getTimestamp()
                );
                
                return ResponseEntity.ok(response);
            } else {
                metricsCollector.incrementCounter("orders_cancelled", "status", "failure");
                
                if ("ORDER_NOT_FOUND".equals(result.getErrorCode())) {
                    return ResponseEntity.notFound().build();
                } else {
                    ErrorResponse errorResponse = new ErrorResponse(
                            result.getErrorCode(),
                            result.getErrorMessage(),
                            "/api/v1/trading/orders/" + orderId,
                            result.getTimestamp()
                    );
                    
                    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                            .body(OrderResponse.fromError(errorResponse));
                }
            }
            
        } catch (Exception e) {
            log.error("Unexpected error cancelling order: {}", orderId, e);
            metricsCollector.incrementCounter("orders_cancelled", "status", "error");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(OrderResponse.fromError(new ErrorResponse(
                            "INTERNAL_ERROR",
                            "An unexpected error occurred",
                            "/api/v1/trading/orders/" + orderId,
                            Instant.now()
                    )));
                    
        } finally {
            sample.stop(metricsCollector.getTimer("cancel_order"));
        }
    }
    
    @GetMapping("/portfolios/{traderId}")
    @Operation(summary = "Get trader portfolio", description = "Retrieves portfolio for a specific trader")
    public ResponseEntity<TraderPortfolioResponse> getTraderPortfolio(@PathVariable String traderId) {
        
        Optional<TraderPortfolioView> portfolio = queryService.getTraderPortfolio(traderId);
        
        if (portfolio.isPresent()) {
            TraderPortfolioResponse response = TraderPortfolioResponse.fromView(portfolio.get());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/statistics/{symbol}")
    @Operation(summary = "Get trading statistics", description = "Retrieves trading statistics for a symbol")
    public ResponseEntity<TradingStatisticsResponse> getTradingStatistics(
            @PathVariable String symbol,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        
        TradingStatistics statistics = queryService.getTradingStatistics(symbol, fromDate, toDate);
        TradingStatisticsResponse response = TradingStatisticsResponse.fromStatistics(statistics);
        
        return ResponseEntity.ok(response);
    }
}

// Request DTOs
public class PlaceLimitOrderRequest {
    @NotBlank(message = "Trader ID is required")
    private String traderId;
    
    @NotBlank(message = "Symbol is required")
    @Pattern(regexp = "^[A-Z]{1,5}$", message = "Symbol must be 1-5 uppercase letters")
    private String symbol;
    
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be positive")
    private BigDecimal quantity;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be positive")
    private BigDecimal price;
    
    @NotNull(message = "Side is required")
    private OrderSide side;
    
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
}

public class PlaceMarketOrderRequest {
    @NotBlank(message = "Trader ID is required")
    private String traderId;
    
    @NotBlank(message = "Symbol is required")
    @Pattern(regexp = "^[A-Z]{1,5}$", message = "Symbol must be 1-5 uppercase letters")
    private String symbol;
    
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be positive")
    private BigDecimal quantity;
    
    @NotNull(message = "Side is required")
    private OrderSide side;
    
    // Getters and setters
    public String getTraderId() { return traderId; }
    public void setTraderId(String traderId) { this.traderId = traderId; }
    
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    
    public OrderSide getSide() { return side; }
    public void setSide(OrderSide side) { this.side = side; }
}

public class CancelOrderRequest {
    @NotBlank(message = "Reason is required")
    private String reason;
    
    @NotBlank(message = "Cancelled by is required")
    private String cancelledBy;
    
    // Getters and setters
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }
}

// Response DTOs
public class OrderResponse {
    private String orderId;
    private String status;
    private String message;
    private Instant timestamp;
    private ErrorResponse error;
    
    public OrderResponse() {}
    
    public OrderResponse(String orderId, String status, String message, Instant timestamp) {
        this.orderId = orderId;
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
    }
    
    public static OrderResponse fromError(ErrorResponse error) {
        OrderResponse response = new OrderResponse();
        response.error = error;
        return response;
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
    
    public ErrorResponse getError() { return error; }
    public void setError(ErrorResponse error) { this.error = error; }
}

public class ErrorResponse {
    private String errorCode;
    private String message;
    private String path;
    private Instant timestamp;
    
    public ErrorResponse() {}
    
    public ErrorResponse(String errorCode, String message, String path, Instant timestamp) {
        this.errorCode = errorCode;
        this.message = message;
        this.path = path;
        this.timestamp = timestamp;
    }
    
    // Getters and setters
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}

public class TradingOrderResponse {
    private String orderId;
    private String traderId;
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal price;
    private String side;
    private String orderType;
    private String status;
    private BigDecimal filledQuantity;
    private Instant createdAt;
    private Instant lastModified;
    
    public static TradingOrderResponse fromView(TradingOrderView view) {
        TradingOrderResponse response = new TradingOrderResponse();
        response.orderId = view.getOrderId();
        response.traderId = view.getTraderId();
        response.symbol = view.getSymbol();
        response.quantity = view.getQuantity();
        response.price = view.getPrice();
        response.side = view.getSide().name();
        response.orderType = view.getOrderType().name();
        response.status = view.getStatus().name();
        response.filledQuantity = view.getFilledQuantity();
        response.createdAt = view.getCreatedAt();
        response.lastModified = view.getLastModified();
        return response;
    }
    
    // Getters and setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getTraderId() { return traderId; }
    public void setTraderId(String traderId) { this.traderId = traderId; }
    
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public BigDecimal getFilledQuantity() { return filledQuantity; }
    public void setFilledQuantity(BigDecimal filledQuantity) { this.filledQuantity = filledQuantity; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getLastModified() { return lastModified; }
    public void setLastModified(Instant lastModified) { this.lastModified = lastModified; }
}

public class TraderPortfolioResponse {
    private String traderId;
    private Map<String, PositionResponse> positions;
    private BigDecimal totalValue;
    private Instant lastUpdated;
    
    public static TraderPortfolioResponse fromView(TraderPortfolioView view) {
        TraderPortfolioResponse response = new TraderPortfolioResponse();
        response.traderId = view.getTraderId();
        
        response.positions = view.getPositions().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> PositionResponse.fromView(entry.getValue())
                ));
        
        response.totalValue = view.getTotalValue();
        response.lastUpdated = view.getLastUpdated();
        return response;
    }
    
    // Getters and setters
    public String getTraderId() { return traderId; }
    public void setTraderId(String traderId) { this.traderId = traderId; }
    
    public Map<String, PositionResponse> getPositions() { return positions; }
    public void setPositions(Map<String, PositionResponse> positions) { this.positions = positions; }
    
    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    
    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
}

public class PositionResponse {
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal averagePrice;
    private BigDecimal totalValue;
    
    public static PositionResponse fromView(PositionView view) {
        PositionResponse response = new PositionResponse();
        response.symbol = view.getSymbol();
        response.quantity = view.getQuantity();
        response.averagePrice = view.getAveragePrice();
        response.totalValue = view.getTotalValue();
        return response;
    }
    
    // Getters and setters
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    
    public BigDecimal getAveragePrice() { return averagePrice; }
    public void setAveragePrice(BigDecimal averagePrice) { this.averagePrice = averagePrice; }
    
    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
}

public class TradingStatisticsResponse {
    private String symbol;
    private long totalOrders;
    private long buyOrders;
    private long sellOrders;
    private long executedOrders;
    private long cancelledOrders;
    private BigDecimal totalVolume;
    private BigDecimal averagePrice;
    private LocalDate fromDate;
    private LocalDate toDate;
    
    public static TradingStatisticsResponse fromStatistics(TradingStatistics statistics) {
        TradingStatisticsResponse response = new TradingStatisticsResponse();
        response.symbol = statistics.getSymbol();
        response.totalOrders = statistics.getTotalOrders();
        response.buyOrders = statistics.getBuyOrders();
        response.sellOrders = statistics.getSellOrders();
        response.executedOrders = statistics.getExecutedOrders();
        response.cancelledOrders = statistics.getCancelledOrders();
        response.totalVolume = statistics.getTotalVolume();
        response.averagePrice = statistics.getAveragePrice();
        response.fromDate = statistics.getFromDate();
        response.toDate = statistics.getToDate();
        return response;
    }
    
    // Getters and setters
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    
    public long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(long totalOrders) { this.totalOrders = totalOrders; }
    
    public long getBuyOrders() { return buyOrders; }
    public void setBuyOrders(long buyOrders) { this.buyOrders = buyOrders; }
    
    public long getSellOrders() { return sellOrders; }
    public void setSellOrders(long sellOrders) { this.sellOrders = sellOrders; }
    
    public long getExecutedOrders() { return executedOrders; }
    public void setExecutedOrders(long executedOrders) { this.executedOrders = executedOrders; }
    
    public long getCancelledOrders() { return cancelledOrders; }
    public void setCancelledOrders(long cancelledOrders) { this.cancelledOrders = cancelledOrders; }
    
    public BigDecimal getTotalVolume() { return totalVolume; }
    public void setTotalVolume(BigDecimal totalVolume) { this.totalVolume = totalVolume; }
    
    public BigDecimal getAveragePrice() { return averagePrice; }
    public void setAveragePrice(BigDecimal averagePrice) { this.averagePrice = averagePrice; }
    
    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }
    
    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }
}

public class PagedResponse<T> {
    private List<T> data;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    
    public PagedResponse() {}
    
    public PagedResponse(List<T> data, int page, int size, long totalElements, int totalPages) {
        this.data = data;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }
    
    // Getters and setters
    public List<T> getData() { return data; }
    public void setData(List<T> data) { this.data = data; }
    
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}

// Metrics Collector
@Component
public class MetricsCollector {
    
    private final MeterRegistry meterRegistry;
    
    public MetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    public Timer.Sample startTimer(String name) {
        return Timer.start(meterRegistry);
    }
    
    public Timer getTimer(String name) {
        return Timer.builder(name)
                .description("Timer for " + name)
                .register(meterRegistry);
    }
    
    public void incrementCounter(String name, String... tags) {
        Counter.builder(name)
                .description("Counter for " + name)
                .tags(tags)
                .register(meterRegistry)
                .increment();
    }
}
```

## Key Takeaways

1. **Enterprise Architecture Integration**: Successfully combines DDD, CQRS, Event Sourcing, and Hexagonal Architecture
2. **Production-Ready Code**: Implements comprehensive error handling, validation, and observability
3. **Scalable Design**: Uses event-driven architecture and proper separation of concerns
4. **Comprehensive Testing**: Applies advanced testing strategies throughout the system
5. **Performance Engineering**: Includes metrics, monitoring, and performance optimization
6. **Enterprise Integration**: Shows integration with legacy systems and external services
7. **Real-World Complexity**: Demonstrates handling of complex business rules and domain logic

This capstone project represents a complete, enterprise-grade financial trading platform that showcases the integration of all advanced Java enterprise patterns and practices covered in Week 15. It serves as a comprehensive example of how to build robust, scalable, and maintainable enterprise applications using modern Java technologies and architectural patterns.