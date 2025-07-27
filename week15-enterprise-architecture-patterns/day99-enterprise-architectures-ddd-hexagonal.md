# Day 99: Java Enterprise Architectures - Domain-Driven Design & Hexagonal Architecture

## Learning Objectives
- Master Domain-Driven Design (DDD) principles and implementation patterns
- Understand and implement Hexagonal Architecture (Ports and Adapters)
- Build clean, maintainable enterprise applications with proper boundaries
- Apply strategic and tactical DDD patterns in real-world scenarios
- Design systems that reflect business domain complexity

## 1. Domain-Driven Design Fundamentals

### Strategic Design Patterns

```java
package com.javajourney.trading.domain;

/**
 * Bounded Context: Trading Domain
 * 
 * This represents the core trading functionality with its own
 * ubiquitous language and domain model.
 */
public class TradingBoundedContext {
    
    // Aggregate Root - Order
    public static class Order {
        private final OrderId orderId;
        private final TraderId traderId;
        private final Symbol symbol;
        private final OrderType orderType;
        private final Quantity quantity;
        private final Price price;
        private OrderStatus status;
        private final Instant createdAt;
        private final List<DomainEvent> domainEvents = new ArrayList<>();
        
        private Order(OrderId orderId, TraderId traderId, Symbol symbol, 
                     OrderType orderType, Quantity quantity, Price price) {
            this.orderId = orderId;
            this.traderId = traderId;
            this.symbol = symbol;
            this.orderType = orderType;
            this.quantity = quantity;
            this.price = price;
            this.status = OrderStatus.PENDING;
            this.createdAt = Instant.now();
            
            // Domain event
            addDomainEvent(new OrderCreatedEvent(orderId, traderId, symbol, quantity, price));
        }
        
        // Factory method - enforces business rules
        public static Order createMarketOrder(TraderId traderId, Symbol symbol, 
                                            Quantity quantity, OrderSide side) {
            validateTrader(traderId);
            validateSymbol(symbol);
            validateQuantity(quantity);
            
            OrderId orderId = OrderId.generate();
            OrderType orderType = OrderType.market(side);
            
            return new Order(orderId, traderId, symbol, orderType, quantity, Price.market());
        }
        
        public static Order createLimitOrder(TraderId traderId, Symbol symbol, 
                                           Quantity quantity, Price price, OrderSide side) {
            validateTrader(traderId);
            validateSymbol(symbol);
            validateQuantity(quantity);
            validatePrice(price);
            
            OrderId orderId = OrderId.generate();
            OrderType orderType = OrderType.limit(side);
            
            return new Order(orderId, traderId, symbol, orderType, quantity, price);
        }
        
        // Business methods
        public void execute(Price executionPrice, Quantity executedQuantity) {
            if (status != OrderStatus.PENDING) {
                throw new OrderDomainException("Order cannot be executed in status: " + status);
            }
            
            if (executedQuantity.isGreaterThan(quantity)) {
                throw new OrderDomainException("Executed quantity cannot exceed order quantity");
            }
            
            this.status = executedQuantity.equals(quantity) ? 
                OrderStatus.FULLY_EXECUTED : OrderStatus.PARTIALLY_EXECUTED;
            
            addDomainEvent(new OrderExecutedEvent(orderId, executionPrice, executedQuantity));
        }
        
        public void cancel() {
            if (status == OrderStatus.FULLY_EXECUTED) {
                throw new OrderDomainException("Cannot cancel fully executed order");
            }
            
            this.status = OrderStatus.CANCELLED;
            addDomainEvent(new OrderCancelledEvent(orderId));
        }
        
        public boolean canBeModified() {
            return status == OrderStatus.PENDING || status == OrderStatus.PARTIALLY_EXECUTED;
        }
        
        private void addDomainEvent(DomainEvent event) {
            domainEvents.add(event);
        }
        
        public List<DomainEvent> getDomainEvents() {
            return List.copyOf(domainEvents);
        }
        
        public void clearDomainEvents() {
            domainEvents.clear();
        }
        
        // Validation methods
        private static void validateTrader(TraderId traderId) {
            if (traderId == null) {
                throw new OrderDomainException("Trader ID cannot be null");
            }
        }
        
        private static void validateSymbol(Symbol symbol) {
            if (symbol == null || !symbol.isValid()) {
                throw new OrderDomainException("Invalid symbol");
            }
        }
        
        private static void validateQuantity(Quantity quantity) {
            if (quantity == null || quantity.isZeroOrNegative()) {
                throw new OrderDomainException("Quantity must be positive");
            }
        }
        
        private static void validatePrice(Price price) {
            if (price == null || price.isNegative()) {
                throw new OrderDomainException("Price cannot be negative");
            }
        }
        
        // Getters
        public OrderId getOrderId() { return orderId; }
        public TraderId getTraderId() { return traderId; }
        public Symbol getSymbol() { return symbol; }
        public OrderType getOrderType() { return orderType; }
        public Quantity getQuantity() { return quantity; }
        public Price getPrice() { return price; }
        public OrderStatus getStatus() { return status; }
        public Instant getCreatedAt() { return createdAt; }
    }
    
    // Value Objects
    public static class OrderId {
        private final String value;
        
        private OrderId(String value) {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException("Order ID cannot be null or empty");
            }
            this.value = value;
        }
        
        public static OrderId of(String value) {
            return new OrderId(value);
        }
        
        public static OrderId generate() {
            return new OrderId(UUID.randomUUID().toString());
        }
        
        public String getValue() { return value; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OrderId orderId = (OrderId) o;
            return Objects.equals(value, orderId.value);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
        
        @Override
        public String toString() {
            return value;
        }
    }
    
    public static class TraderId {
        private final String value;
        
        private TraderId(String value) {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException("Trader ID cannot be null or empty");
            }
            this.value = value;
        }
        
        public static TraderId of(String value) {
            return new TraderId(value);
        }
        
        public String getValue() { return value; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TraderId traderId = (TraderId) o;
            return Objects.equals(value, traderId.value);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
        
        @Override
        public String toString() {
            return value;
        }
    }
    
    public static class Symbol {
        private final String value;
        
        private Symbol(String value) {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException("Symbol cannot be null or empty");
            }
            if (!value.matches("^[A-Z]{2,10}$")) {
                throw new IllegalArgumentException("Symbol must be 2-10 uppercase letters");
            }
            this.value = value;
        }
        
        public static Symbol of(String value) {
            return new Symbol(value.toUpperCase());
        }
        
        public boolean isValid() {
            return value.matches("^[A-Z]{2,10}$");
        }
        
        public String getValue() { return value; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Symbol symbol = (Symbol) o;
            return Objects.equals(value, symbol.value);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
        
        @Override
        public String toString() {
            return value;
        }
    }
    
    public static class Quantity {
        private final BigDecimal value;
        
        private Quantity(BigDecimal value) {
            if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            this.value = value.setScale(8, RoundingMode.HALF_UP);
        }
        
        public static Quantity of(BigDecimal value) {
            return new Quantity(value);
        }
        
        public static Quantity of(double value) {
            return new Quantity(BigDecimal.valueOf(value));
        }
        
        public boolean isZeroOrNegative() {
            return value.compareTo(BigDecimal.ZERO) <= 0;
        }
        
        public boolean isGreaterThan(Quantity other) {
            return value.compareTo(other.value) > 0;
        }
        
        public boolean equals(Quantity other) {
            return value.compareTo(other.value) == 0;
        }
        
        public Quantity subtract(Quantity other) {
            return new Quantity(value.subtract(other.value));
        }
        
        public Quantity add(Quantity other) {
            return new Quantity(value.add(other.value));
        }
        
        public BigDecimal getValue() { return value; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Quantity quantity = (Quantity) o;
            return value.compareTo(quantity.value) == 0;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
        
        @Override
        public String toString() {
            return value.toString();
        }
    }
    
    public static class Price {
        private final BigDecimal value;
        private final boolean isMarketPrice;
        
        private Price(BigDecimal value, boolean isMarketPrice) {
            if (!isMarketPrice && (value == null || value.compareTo(BigDecimal.ZERO) < 0)) {
                throw new IllegalArgumentException("Price cannot be negative");
            }
            this.value = isMarketPrice ? null : value.setScale(2, RoundingMode.HALF_UP);
            this.isMarketPrice = isMarketPrice;
        }
        
        public static Price of(BigDecimal value) {
            return new Price(value, false);
        }
        
        public static Price of(double value) {
            return new Price(BigDecimal.valueOf(value), false);
        }
        
        public static Price market() {
            return new Price(null, true);
        }
        
        public boolean isMarketPrice() { return isMarketPrice; }
        
        public boolean isNegative() {
            return !isMarketPrice && value.compareTo(BigDecimal.ZERO) < 0;
        }
        
        public BigDecimal getValue() { return value; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Price price = (Price) o;
            return isMarketPrice == price.isMarketPrice && Objects.equals(value, price.value);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(value, isMarketPrice);
        }
        
        @Override
        public String toString() {
            return isMarketPrice ? "MARKET" : value.toString();
        }
    }
    
    // Enums
    public enum OrderStatus {
        PENDING, PARTIALLY_EXECUTED, FULLY_EXECUTED, CANCELLED
    }
    
    public enum OrderSide {
        BUY, SELL
    }
    
    // Order Type as Value Object
    public static class OrderType {
        private final String type;
        private final OrderSide side;
        
        private OrderType(String type, OrderSide side) {
            this.type = type;
            this.side = side;
        }
        
        public static OrderType market(OrderSide side) {
            return new OrderType("MARKET", side);
        }
        
        public static OrderType limit(OrderSide side) {
            return new OrderType("LIMIT", side);
        }
        
        public boolean isMarket() { return "MARKET".equals(type); }
        public boolean isLimit() { return "LIMIT".equals(type); }
        
        public String getType() { return type; }
        public OrderSide getSide() { return side; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OrderType orderType = (OrderType) o;
            return Objects.equals(type, orderType.type) && side == orderType.side;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(type, side);
        }
        
        @Override
        public String toString() {
            return type + "_" + side;
        }
    }
    
    // Domain Events
    public abstract static class DomainEvent {
        protected final Instant occurredOn;
        
        protected DomainEvent() {
            this.occurredOn = Instant.now();
        }
        
        public Instant getOccurredOn() { return occurredOn; }
    }
    
    public static class OrderCreatedEvent extends DomainEvent {
        private final OrderId orderId;
        private final TraderId traderId;
        private final Symbol symbol;
        private final Quantity quantity;
        private final Price price;
        
        public OrderCreatedEvent(OrderId orderId, TraderId traderId, Symbol symbol, 
                               Quantity quantity, Price price) {
            super();
            this.orderId = orderId;
            this.traderId = traderId;
            this.symbol = symbol;
            this.quantity = quantity;
            this.price = price;
        }
        
        // Getters
        public OrderId getOrderId() { return orderId; }
        public TraderId getTraderId() { return traderId; }
        public Symbol getSymbol() { return symbol; }
        public Quantity getQuantity() { return quantity; }
        public Price getPrice() { return price; }
    }
    
    public static class OrderExecutedEvent extends DomainEvent {
        private final OrderId orderId;
        private final Price executionPrice;
        private final Quantity executedQuantity;
        
        public OrderExecutedEvent(OrderId orderId, Price executionPrice, Quantity executedQuantity) {
            super();
            this.orderId = orderId;
            this.executionPrice = executionPrice;
            this.executedQuantity = executedQuantity;
        }
        
        // Getters
        public OrderId getOrderId() { return orderId; }
        public Price getExecutionPrice() { return executionPrice; }
        public Quantity getExecutedQuantity() { return executedQuantity; }
    }
    
    public static class OrderCancelledEvent extends DomainEvent {
        private final OrderId orderId;
        
        public OrderCancelledEvent(OrderId orderId) {
            super();
            this.orderId = orderId;
        }
        
        public OrderId getOrderId() { return orderId; }
    }
    
    // Domain Exception
    public static class OrderDomainException extends RuntimeException {
        public OrderDomainException(String message) {
            super(message);
        }
        
        public OrderDomainException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
```

### Domain Services and Factories

```java
package com.javajourney.trading.domain.service;

import com.javajourney.trading.domain.TradingBoundedContext.*;
import org.springframework.stereotype.Component;

/**
 * Domain Service for complex business logic that doesn't naturally
 * fit into a single aggregate
 */
@Component
public class OrderValidationService {
    
    private final MarketDataService marketDataService;
    private final TraderRiskService traderRiskService;
    
    public OrderValidationService(MarketDataService marketDataService, 
                                TraderRiskService traderRiskService) {
        this.marketDataService = marketDataService;
        this.traderRiskService = traderRiskService;
    }
    
    /**
     * Validates order against business rules that span multiple domains
     */
    public OrderValidationResult validateOrder(Order order) {
        List<String> violations = new ArrayList<>();
        
        // Market hours validation
        if (!marketDataService.isMarketOpen(order.getSymbol())) {
            violations.add("Market is closed for symbol: " + order.getSymbol());
        }
        
        // Price validation for limit orders
        if (order.getOrderType().isLimit()) {
            MarketPrice currentPrice = marketDataService.getCurrentPrice(order.getSymbol());
            if (isPriceOutOfRange(order.getPrice(), currentPrice)) {
                violations.add("Order price is outside acceptable range");
            }
        }
        
        // Risk validation
        RiskAssessment riskAssessment = traderRiskService.assessRisk(
            order.getTraderId(), order.getSymbol(), order.getQuantity(), order.getPrice());
        
        if (riskAssessment.exceedsLimits()) {
            violations.addAll(riskAssessment.getViolations());
        }
        
        // Regulatory compliance
        if (!isCompliantWithRegulations(order)) {
            violations.add("Order violates regulatory requirements");
        }
        
        return violations.isEmpty() ? 
            OrderValidationResult.valid() : 
            OrderValidationResult.invalid(violations);
    }
    
    private boolean isPriceOutOfRange(Price orderPrice, MarketPrice currentPrice) {
        if (orderPrice.isMarketPrice()) return false;
        
        BigDecimal currentValue = currentPrice.getValue();
        BigDecimal orderValue = orderPrice.getValue();
        
        // Allow 10% deviation from current price
        BigDecimal maxDeviation = currentValue.multiply(BigDecimal.valueOf(0.1));
        BigDecimal difference = orderValue.subtract(currentValue).abs();
        
        return difference.compareTo(maxDeviation) > 0;
    }
    
    private boolean isCompliantWithRegulations(Order order) {
        // Simplified compliance check
        // In real implementation, this would check against various regulations
        return order.getQuantity().getValue().compareTo(BigDecimal.valueOf(1000000)) <= 0;
    }
    
    // Result classes
    public static class OrderValidationResult {
        private final boolean isValid;
        private final List<String> violations;
        
        private OrderValidationResult(boolean isValid, List<String> violations) {
            this.isValid = isValid;
            this.violations = violations != null ? List.copyOf(violations) : List.of();
        }
        
        public static OrderValidationResult valid() {
            return new OrderValidationResult(true, null);
        }
        
        public static OrderValidationResult invalid(List<String> violations) {
            return new OrderValidationResult(false, violations);
        }
        
        public boolean isValid() { return isValid; }
        public List<String> getViolations() { return violations; }
    }
}

/**
 * Domain Service for market data operations
 */
@Component
public class MarketDataService {
    
    private final MarketDataProvider marketDataProvider;
    
    public MarketDataService(MarketDataProvider marketDataProvider) {
        this.marketDataProvider = marketDataProvider;
    }
    
    public boolean isMarketOpen(Symbol symbol) {
        return marketDataProvider.isMarketOpen(symbol.getValue());
    }
    
    public MarketPrice getCurrentPrice(Symbol symbol) {
        return marketDataProvider.getCurrentPrice(symbol.getValue());
    }
    
    public static class MarketPrice {
        private final BigDecimal value;
        private final Instant timestamp;
        
        public MarketPrice(BigDecimal value, Instant timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
        
        public BigDecimal getValue() { return value; }
        public Instant getTimestamp() { return timestamp; }
    }
}

/**
 * Domain Service for trader risk assessment
 */
@Component
public class TraderRiskService {
    
    private final RiskCalculator riskCalculator;
    private final TraderLimitsRepository traderLimitsRepository;
    
    public TraderRiskService(RiskCalculator riskCalculator, 
                           TraderLimitsRepository traderLimitsRepository) {
        this.riskCalculator = riskCalculator;
        this.traderLimitsRepository = traderLimitsRepository;
    }
    
    public RiskAssessment assessRisk(TraderId traderId, Symbol symbol, 
                                   Quantity quantity, Price price) {
        TraderLimits limits = traderLimitsRepository.findByTraderId(traderId);
        if (limits == null) {
            return RiskAssessment.failed("No risk limits found for trader");
        }
        
        List<String> violations = new ArrayList<>();
        
        // Check position limits
        BigDecimal currentPosition = riskCalculator.getCurrentPosition(traderId, symbol);
        BigDecimal proposedPosition = currentPosition.add(quantity.getValue());
        
        if (proposedPosition.abs().compareTo(limits.getMaxPositionSize()) > 0) {
            violations.add("Exceeds maximum position size limit");
        }
        
        // Check daily trading limit
        BigDecimal dailyVolume = riskCalculator.getDailyTradingVolume(traderId);
        BigDecimal orderValue = quantity.getValue().multiply(
            price.isMarketPrice() ? getCurrentMarketPrice(symbol) : price.getValue());
        
        if (dailyVolume.add(orderValue).compareTo(limits.getDailyTradingLimit()) > 0) {
            violations.add("Exceeds daily trading limit");
        }
        
        // Check leverage limits
        BigDecimal currentLeverage = riskCalculator.getCurrentLeverage(traderId);
        if (currentLeverage.compareTo(limits.getMaxLeverage()) > 0) {
            violations.add("Exceeds maximum leverage limit");
        }
        
        return violations.isEmpty() ? 
            RiskAssessment.passed() : 
            RiskAssessment.failed(violations);
    }
    
    private BigDecimal getCurrentMarketPrice(Symbol symbol) {
        // Simplified - would get from market data service
        return BigDecimal.valueOf(100);
    }
    
    public static class RiskAssessment {
        private final boolean passed;
        private final List<String> violations;
        
        private RiskAssessment(boolean passed, List<String> violations) {
            this.passed = passed;
            this.violations = violations != null ? List.copyOf(violations) : List.of();
        }
        
        public static RiskAssessment passed() {
            return new RiskAssessment(true, null);
        }
        
        public static RiskAssessment failed(String violation) {
            return new RiskAssessment(false, List.of(violation));
        }
        
        public static RiskAssessment failed(List<String> violations) {
            return new RiskAssessment(false, violations);
        }
        
        public boolean exceedsLimits() { return !passed; }
        public List<String> getViolations() { return violations; }
    }
}

/**
 * Factory for creating complex domain objects
 */
@Component
public class OrderFactory {
    
    private final OrderValidationService validationService;
    private final TraderService traderService;
    
    public OrderFactory(OrderValidationService validationService, 
                       TraderService traderService) {
        this.validationService = validationService;
        this.traderService = traderService;
    }
    
    /**
     * Creates a new order with full validation and business rule enforcement
     */
    public Order createOrder(CreateOrderRequest request) {
        // Validate trader exists and is active
        Trader trader = traderService.findById(request.getTraderId());
        if (trader == null || !trader.isActive()) {
            throw new OrderDomainException("Trader is not active");
        }
        
        // Create order based on type
        Order order = switch (request.getOrderType().toUpperCase()) {
            case "MARKET" -> Order.createMarketOrder(
                request.getTraderId(),
                request.getSymbol(),
                request.getQuantity(),
                request.getSide()
            );
            case "LIMIT" -> Order.createLimitOrder(
                request.getTraderId(),
                request.getSymbol(),
                request.getQuantity(),
                request.getPrice(),
                request.getSide()
            );
            default -> throw new OrderDomainException("Unsupported order type: " + request.getOrderType());
        };
        
        // Validate the created order
        OrderValidationService.OrderValidationResult validation = validationService.validateOrder(order);
        if (!validation.isValid()) {
            throw new OrderDomainException("Order validation failed: " + 
                String.join(", ", validation.getViolations()));
        }
        
        return order;
    }
    
    public static class CreateOrderRequest {
        private final TraderId traderId;
        private final Symbol symbol;
        private final String orderType;
        private final Quantity quantity;
        private final Price price;
        private final OrderSide side;
        
        public CreateOrderRequest(TraderId traderId, Symbol symbol, String orderType,
                                Quantity quantity, Price price, OrderSide side) {
            this.traderId = traderId;
            this.symbol = symbol;
            this.orderType = orderType;
            this.quantity = quantity;
            this.price = price;
            this.side = side;
        }
        
        // Getters
        public TraderId getTraderId() { return traderId; }
        public Symbol getSymbol() { return symbol; }
        public String getOrderType() { return orderType; }
        public Quantity getQuantity() { return quantity; }
        public Price getPrice() { return price; }
        public OrderSide getSide() { return side; }
    }
}

// Supporting interfaces and classes
interface MarketDataProvider {
    boolean isMarketOpen(String symbol);
    MarketDataService.MarketPrice getCurrentPrice(String symbol);
}

interface RiskCalculator {
    BigDecimal getCurrentPosition(TraderId traderId, Symbol symbol);
    BigDecimal getDailyTradingVolume(TraderId traderId);
    BigDecimal getCurrentLeverage(TraderId traderId);
}

interface TraderLimitsRepository {
    TraderLimits findByTraderId(TraderId traderId);
}

interface TraderService {
    Trader findById(TraderId traderId);
}

class TraderLimits {
    private final BigDecimal maxPositionSize;
    private final BigDecimal dailyTradingLimit;
    private final BigDecimal maxLeverage;
    
    public TraderLimits(BigDecimal maxPositionSize, BigDecimal dailyTradingLimit, BigDecimal maxLeverage) {
        this.maxPositionSize = maxPositionSize;
        this.dailyTradingLimit = dailyTradingLimit;
        this.maxLeverage = maxLeverage;
    }
    
    public BigDecimal getMaxPositionSize() { return maxPositionSize; }
    public BigDecimal getDailyTradingLimit() { return dailyTradingLimit; }
    public BigDecimal getMaxLeverage() { return maxLeverage; }
}

class Trader {
    private final TraderId id;
    private final boolean active;
    
    public Trader(TraderId id, boolean active) {
        this.id = id;
        this.active = active;
    }
    
    public TraderId getId() { return id; }
    public boolean isActive() { return active; }
}
```

## 2. Hexagonal Architecture Implementation

### Port Definitions

```java
package com.javajourney.trading.application.port;

import com.javajourney.trading.domain.TradingBoundedContext.*;
import java.util.Optional;
import java.util.List;

/**
 * Primary Ports (Driving Ports) - Define how the outside world interacts with our application
 */

// Order Management Port
public interface OrderManagementPort {
    
    /**
     * Places a new order in the system
     */
    OrderResult placeOrder(PlaceOrderCommand command);
    
    /**
     * Cancels an existing order
     */
    OrderResult cancelOrder(CancelOrderCommand command);
    
    /**
     * Modifies an existing order
     */
    OrderResult modifyOrder(ModifyOrderCommand command);
    
    /**
     * Retrieves order by ID
     */
    Optional<OrderView> getOrder(OrderId orderId);
    
    /**
     * Retrieves orders for a trader
     */
    List<OrderView> getOrdersForTrader(TraderId traderId);
    
    // Command classes
    record PlaceOrderCommand(
        TraderId traderId,
        Symbol symbol,
        String orderType,
        Quantity quantity,
        Price price,
        OrderSide side
    ) {}
    
    record CancelOrderCommand(
        OrderId orderId,
        TraderId traderId
    ) {}
    
    record ModifyOrderCommand(
        OrderId orderId,
        TraderId traderId,
        Quantity newQuantity,
        Price newPrice
    ) {}
    
    // Result classes
    record OrderResult(
        boolean success,
        String message,
        OrderId orderId,
        List<String> errors
    ) {
        public static OrderResult success(OrderId orderId, String message) {
            return new OrderResult(true, message, orderId, List.of());
        }
        
        public static OrderResult failure(List<String> errors) {
            return new OrderResult(false, "Order operation failed", null, errors);
        }
    }
    
    // View classes (read models)
    record OrderView(
        OrderId orderId,
        TraderId traderId,
        Symbol symbol,
        OrderType orderType,
        Quantity quantity,
        Price price,
        OrderStatus status,
        Instant createdAt
    ) {
        public static OrderView from(Order order) {
            return new OrderView(
                order.getOrderId(),
                order.getTraderId(),
                order.getSymbol(),
                order.getOrderType(),
                order.getQuantity(),
                order.getPrice(),
                order.getStatus(),
                order.getCreatedAt()
            );
        }
    }
}

// Market Data Query Port
public interface MarketDataQueryPort {
    
    /**
     * Gets current market price for a symbol
     */
    Optional<MarketPriceView> getCurrentPrice(Symbol symbol);
    
    /**
     * Gets market status for a symbol
     */
    MarketStatusView getMarketStatus(Symbol symbol);
    
    /**
     * Gets price history for a symbol
     */
    List<PriceHistoryView> getPriceHistory(Symbol symbol, Instant from, Instant to);
    
    record MarketPriceView(
        Symbol symbol,
        BigDecimal price,
        Instant timestamp
    ) {}
    
    record MarketStatusView(
        Symbol symbol,
        boolean isOpen,
        String marketSession
    ) {}
    
    record PriceHistoryView(
        Symbol symbol,
        BigDecimal price,
        Instant timestamp,
        BigDecimal volume
    ) {}
}

/**
 * Secondary Ports (Driven Ports) - Define dependencies that our application needs
 */

// Persistence Port
public interface OrderRepositoryPort {
    
    /**
     * Saves an order aggregate
     */
    void save(Order order);
    
    /**
     * Finds order by ID
     */
    Optional<Order> findById(OrderId orderId);
    
    /**
     * Finds orders by trader
     */
    List<Order> findByTraderId(TraderId traderId);
    
    /**
     * Finds orders by symbol
     */
    List<Order> findBySymbol(Symbol symbol);
    
    /**
     * Finds orders by status
     */
    List<Order> findByStatus(OrderStatus status);
}

// Market Data Port
public interface MarketDataPort {
    
    /**
     * Gets current market price
     */
    Optional<MarketPrice> getCurrentPrice(Symbol symbol);
    
    /**
     * Checks if market is open
     */
    boolean isMarketOpen(Symbol symbol);
    
    /**
     * Subscribes to price updates
     */
    void subscribeToPriceUpdates(Symbol symbol, PriceUpdateCallback callback);
    
    record MarketPrice(
        Symbol symbol,
        BigDecimal price,
        Instant timestamp
    ) {}
    
    @FunctionalInterface
    interface PriceUpdateCallback {
        void onPriceUpdate(MarketPrice price);
    }
}

// Risk Management Port
public interface RiskManagementPort {
    
    /**
     * Validates order against risk limits
     */
    RiskValidationResult validateOrder(Order order);
    
    /**
     * Gets trader risk limits
     */
    Optional<TraderRiskLimits> getTraderLimits(TraderId traderId);
    
    /**
     * Updates position after trade execution
     */
    void updatePosition(TraderId traderId, Symbol symbol, Quantity quantity, BigDecimal price);
    
    record RiskValidationResult(
        boolean passed,
        List<String> violations
    ) {
        public static RiskValidationResult passed() {
            return new RiskValidationResult(true, List.of());
        }
        
        public static RiskValidationResult failed(List<String> violations) {
            return new RiskValidationResult(false, violations);
        }
    }
    
    record TraderRiskLimits(
        TraderId traderId,
        BigDecimal maxPositionSize,
        BigDecimal dailyTradingLimit,
        BigDecimal maxLeverage
    ) {}
}

// Event Publishing Port
public interface EventPublishingPort {
    
    /**
     * Publishes domain events
     */
    void publishEvents(List<DomainEvent> events);
    
    /**
     * Publishes a single domain event
     */
    void publishEvent(DomainEvent event);
}

// Trade Execution Port
public interface TradeExecutionPort {
    
    /**
     * Executes a trade
     */
    ExecutionResult executeTrade(Order order, BigDecimal executionPrice, Quantity executedQuantity);
    
    /**
     * Gets execution venue for symbol
     */
    String getExecutionVenue(Symbol symbol);
    
    record ExecutionResult(
        boolean success,
        String executionId,
        BigDecimal executionPrice,
        Quantity executedQuantity,
        Instant executionTime,
        String venue
    ) {}
}

// Notification Port
public interface NotificationPort {
    
    /**
     * Sends order confirmation
     */
    void sendOrderConfirmation(TraderId traderId, OrderId orderId);
    
    /**
     * Sends execution notification
     */
    void sendExecutionNotification(TraderId traderId, OrderId orderId, ExecutionDetails details);
    
    /**
     * Sends risk alert
     */
    void sendRiskAlert(TraderId traderId, String message);
    
    record ExecutionDetails(
        OrderId orderId,
        BigDecimal executionPrice,
        Quantity executedQuantity,
        Instant executionTime
    ) {}
}
```

### Application Services (Use Cases)

```java
package com.javajourney.trading.application.service;

import com.javajourney.trading.application.port.*;
import com.javajourney.trading.domain.TradingBoundedContext.*;
import com.javajourney.trading.domain.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application Service implementing the primary ports
 * This layer orchestrates domain objects and coordinates with secondary ports
 */
@Service
@Transactional
public class OrderManagementService implements OrderManagementPort {
    
    private final OrderRepositoryPort orderRepository;
    private final RiskManagementPort riskManagement;
    private final EventPublishingPort eventPublishing;
    private final NotificationPort notification;
    private final OrderFactory orderFactory;
    private final OrderValidationService orderValidationService;
    
    public OrderManagementService(OrderRepositoryPort orderRepository,
                                RiskManagementPort riskManagement,
                                EventPublishingPort eventPublishing,
                                NotificationPort notification,
                                OrderFactory orderFactory,
                                OrderValidationService orderValidationService) {
        this.orderRepository = orderRepository;
        this.riskManagement = riskManagement;
        this.eventPublishing = eventPublishing;
        this.notification = notification;
        this.orderFactory = orderFactory;
        this.orderValidationService = orderValidationService;
    }
    
    @Override
    public OrderResult placeOrder(PlaceOrderCommand command) {
        try {
            // Create order using domain factory
            OrderFactory.CreateOrderRequest request = new OrderFactory.CreateOrderRequest(
                command.traderId(),
                command.symbol(),
                command.orderType(),
                command.quantity(),
                command.price(),
                command.side()
            );
            
            Order order = orderFactory.createOrder(request);
            
            // Additional risk validation
            RiskManagementPort.RiskValidationResult riskResult = riskManagement.validateOrder(order);
            if (!riskResult.passed()) {
                return OrderResult.failure(riskResult.violations());
            }
            
            // Save order
            orderRepository.save(order);
            
            // Publish domain events
            eventPublishing.publishEvents(order.getDomainEvents());
            order.clearDomainEvents();
            
            // Send confirmation
            notification.sendOrderConfirmation(command.traderId(), order.getOrderId());
            
            return OrderResult.success(order.getOrderId(), "Order placed successfully");
            
        } catch (OrderDomainException e) {
            return OrderResult.failure(List.of(e.getMessage()));
        } catch (Exception e) {
            return OrderResult.failure(List.of("Unexpected error: " + e.getMessage()));
        }
    }
    
    @Override
    public OrderResult cancelOrder(CancelOrderCommand command) {
        try {
            Optional<Order> orderOpt = orderRepository.findById(command.orderId());
            if (orderOpt.isEmpty()) {
                return OrderResult.failure(List.of("Order not found"));
            }
            
            Order order = orderOpt.get();
            
            // Verify trader ownership
            if (!order.getTraderId().equals(command.traderId())) {
                return OrderResult.failure(List.of("Not authorized to cancel this order"));
            }
            
            // Cancel order (domain logic)
            order.cancel();
            
            // Save updated order
            orderRepository.save(order);
            
            // Publish events
            eventPublishing.publishEvents(order.getDomainEvents());
            order.clearDomainEvents();
            
            return OrderResult.success(order.getOrderId(), "Order cancelled successfully");
            
        } catch (OrderDomainException e) {
            return OrderResult.failure(List.of(e.getMessage()));
        } catch (Exception e) {
            return OrderResult.failure(List.of("Unexpected error: " + e.getMessage()));
        }
    }
    
    @Override
    public OrderResult modifyOrder(ModifyOrderCommand command) {
        try {
            Optional<Order> orderOpt = orderRepository.findById(command.orderId());
            if (orderOpt.isEmpty()) {
                return OrderResult.failure(List.of("Order not found"));
            }
            
            Order order = orderOpt.get();
            
            // Verify trader ownership
            if (!order.getTraderId().equals(command.traderId())) {
                return OrderResult.failure(List.of("Not authorized to modify this order"));
            }
            
            // Check if order can be modified
            if (!order.canBeModified()) {
                return OrderResult.failure(List.of("Order cannot be modified in current status"));
            }
            
            // Create new order with modified parameters (simplified approach)
            // In real implementation, you might have a modify method on Order aggregate
            OrderFactory.CreateOrderRequest modifyRequest = new OrderFactory.CreateOrderRequest(
                order.getTraderId(),
                order.getSymbol(),
                order.getOrderType().getType(),
                command.newQuantity(),
                command.newPrice(),
                order.getOrderType().getSide()
            );
            
            // Validate modified order
            Order modifiedOrder = orderFactory.createOrder(modifyRequest);
            
            // For simplicity, cancel old order and create new one
            order.cancel();
            orderRepository.save(order);
            
            // Save new order
            orderRepository.save(modifiedOrder);
            
            // Publish events for both orders
            eventPublishing.publishEvents(order.getDomainEvents());
            eventPublishing.publishEvents(modifiedOrder.getDomainEvents());
            
            return OrderResult.success(modifiedOrder.getOrderId(), "Order modified successfully");
            
        } catch (OrderDomainException e) {
            return OrderResult.failure(List.of(e.getMessage()));
        } catch (Exception e) {
            return OrderResult.failure(List.of("Unexpected error: " + e.getMessage()));
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<OrderView> getOrder(OrderId orderId) {
        return orderRepository.findById(orderId)
            .map(OrderView::from);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<OrderView> getOrdersForTrader(TraderId traderId) {
        return orderRepository.findByTraderId(traderId).stream()
            .map(OrderView::from)
            .toList();
    }
}

/**
 * Market Data Query Service
 */
@Service
@Transactional(readOnly = true)
public class MarketDataQueryService implements MarketDataQueryPort {
    
    private final MarketDataPort marketDataPort;
    
    public MarketDataQueryService(MarketDataPort marketDataPort) {
        this.marketDataPort = marketDataPort;
    }
    
    @Override
    public Optional<MarketPriceView> getCurrentPrice(Symbol symbol) {
        return marketDataPort.getCurrentPrice(symbol)
            .map(price -> new MarketPriceView(
                price.symbol(),
                price.price(),
                price.timestamp()
            ));
    }
    
    @Override
    public MarketStatusView getMarketStatus(Symbol symbol) {
        boolean isOpen = marketDataPort.isMarketOpen(symbol);
        String session = isOpen ? "REGULAR" : "CLOSED";
        
        return new MarketStatusView(symbol, isOpen, session);
    }
    
    @Override
    public List<PriceHistoryView> getPriceHistory(Symbol symbol, Instant from, Instant to) {
        // This would typically delegate to a specialized time-series database
        // For now, return empty list
        return List.of();
    }
}

/**
 * Trade Execution Service
 */
@Service
@Transactional
public class TradeExecutionService {
    
    private final OrderRepositoryPort orderRepository;
    private final TradeExecutionPort tradeExecution;
    private final RiskManagementPort riskManagement;
    private final EventPublishingPort eventPublishing;
    private final NotificationPort notification;
    
    public TradeExecutionService(OrderRepositoryPort orderRepository,
                               TradeExecutionPort tradeExecution,
                               RiskManagementPort riskManagement,
                               EventPublishingPort eventPublishing,
                               NotificationPort notification) {
        this.orderRepository = orderRepository;
        this.tradeExecution = tradeExecution;
        this.riskManagement = riskManagement;
        this.eventPublishing = eventPublishing;
        this.notification = notification;
    }
    
    /**
     * Executes a trade for an order
     */
    public void executeTrade(OrderId orderId, BigDecimal executionPrice, Quantity executedQuantity) {
        try {
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                throw new IllegalArgumentException("Order not found: " + orderId);
            }
            
            Order order = orderOpt.get();
            
            // Execute trade through external system
            TradeExecutionPort.ExecutionResult executionResult = tradeExecution.executeTrade(
                order, executionPrice, executedQuantity);
            
            if (!executionResult.success()) {
                throw new RuntimeException("Trade execution failed");
            }
            
            // Update order with execution details
            order.execute(Price.of(executionPrice), executedQuantity);
            
            // Update risk positions
            riskManagement.updatePosition(
                order.getTraderId(),
                order.getSymbol(),
                executedQuantity,
                executionPrice
            );
            
            // Save updated order
            orderRepository.save(order);
            
            // Publish domain events
            eventPublishing.publishEvents(order.getDomainEvents());
            order.clearDomainEvents();
            
            // Send execution notification
            notification.sendExecutionNotification(
                order.getTraderId(),
                orderId,
                new NotificationPort.ExecutionDetails(
                    orderId,
                    executionPrice,
                    executedQuantity,
                    executionResult.executionTime()
                )
            );
            
        } catch (Exception e) {
            // Log error and potentially send alert
            System.err.println("Trade execution failed for order " + orderId + ": " + e.getMessage());
            throw e;
        }
    }
}
```

### Adapter Implementations

```java
package com.javajourney.trading.adapter.persistence;

import com.javajourney.trading.application.port.OrderRepositoryPort;
import com.javajourney.trading.domain.TradingBoundedContext.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.persistence.*;
import java.util.List;
import java.util.Optional;

/**
 * JPA Adapter for Order Repository
 */
@Repository
public class JpaOrderRepositoryAdapter implements OrderRepositoryPort {
    
    private final OrderJpaRepository jpaRepository;
    private final OrderMapper mapper;
    
    public JpaOrderRepositoryAdapter(OrderJpaRepository jpaRepository, OrderMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public void save(Order order) {
        OrderEntity entity = mapper.toEntity(order);
        jpaRepository.save(entity);
    }
    
    @Override
    public Optional<Order> findById(OrderId orderId) {
        return jpaRepository.findById(orderId.getValue())
            .map(mapper::toDomain);
    }
    
    @Override
    public List<Order> findByTraderId(TraderId traderId) {
        return jpaRepository.findByTraderId(traderId.getValue()).stream()
            .map(mapper::toDomain)
            .toList();
    }
    
    @Override
    public List<Order> findBySymbol(Symbol symbol) {
        return jpaRepository.findBySymbol(symbol.getValue()).stream()
            .map(mapper::toDomain)
            .toList();
    }
    
    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return jpaRepository.findByStatus(status.name()).stream()
            .map(mapper::toDomain)
            .toList();
    }
}

// JPA Repository Interface
interface OrderJpaRepository extends JpaRepository<OrderEntity, String> {
    
    List<OrderEntity> findByTraderId(String traderId);
    List<OrderEntity> findBySymbol(String symbol);
    List<OrderEntity> findByStatus(String status);
    
    @Query("SELECT o FROM OrderEntity o WHERE o.traderId = :traderId AND o.status IN :statuses")
    List<OrderEntity> findByTraderIdAndStatusIn(String traderId, List<String> statuses);
}

// JPA Entity
@Entity
@Table(name = "orders")
public class OrderEntity {
    
    @Id
    private String orderId;
    
    @Column(nullable = false)
    private String traderId;
    
    @Column(nullable = false)
    private String symbol;
    
    @Column(nullable = false)
    private String orderType;
    
    @Column(nullable = false)
    private String orderSide;
    
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;
    
    @Column(precision = 19, scale = 2)
    private BigDecimal price;
    
    @Column(nullable = false)
    private boolean isMarketPrice;
    
    @Column(nullable = false)
    private String status;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column
    private Instant updatedAt;
    
    // Constructors, getters, setters
    public OrderEntity() {}
    
    public OrderEntity(String orderId, String traderId, String symbol, String orderType,
                      String orderSide, BigDecimal quantity, BigDecimal price, 
                      boolean isMarketPrice, String status, Instant createdAt) {
        this.orderId = orderId;
        this.traderId = traderId;
        this.symbol = symbol;
        this.orderType = orderType;
        this.orderSide = orderSide;
        this.quantity = quantity;
        this.price = price;
        this.isMarketPrice = isMarketPrice;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }
    
    // Getters and setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getTraderId() { return traderId; }
    public void setTraderId(String traderId) { this.traderId = traderId; }
    
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    
    public String getOrderSide() { return orderSide; }
    public void setOrderSide(String orderSide) { this.orderSide = orderSide; }
    
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public boolean isMarketPrice() { return isMarketPrice; }
    public void setMarketPrice(boolean marketPrice) { isMarketPrice = marketPrice; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}

// Mapper between Domain and Entity
@Component
public class OrderMapper {
    
    public OrderEntity toEntity(Order order) {
        return new OrderEntity(
            order.getOrderId().getValue(),
            order.getTraderId().getValue(),
            order.getSymbol().getValue(),
            order.getOrderType().getType(),
            order.getOrderType().getSide().name(),
            order.getQuantity().getValue(),
            order.getPrice().isMarketPrice() ? null : order.getPrice().getValue(),
            order.getPrice().isMarketPrice(),
            order.getStatus().name(),
            order.getCreatedAt()
        );
    }
    
    public Order toDomain(OrderEntity entity) {
        // Reconstruct domain object
        // Note: This is simplified - in real implementation you might need
        // to use reflection or a more sophisticated mapping approach
        
        OrderId orderId = OrderId.of(entity.getOrderId());
        TraderId traderId = TraderId.of(entity.getTraderId());
        Symbol symbol = Symbol.of(entity.getSymbol());
        
        OrderSide side = OrderSide.valueOf(entity.getOrderSide());
        OrderType orderType = "MARKET".equals(entity.getOrderType()) ?
            OrderType.market(side) : OrderType.limit(side);
        
        Quantity quantity = Quantity.of(entity.getQuantity());
        Price price = entity.isMarketPrice() ? 
            Price.market() : Price.of(entity.getPrice());
        
        // Use reflection or a factory to create the order with status
        // This is a simplified approach
        try {
            Constructor<Order> constructor = Order.class.getDeclaredConstructor(
                OrderId.class, TraderId.class, Symbol.class, OrderType.class,
                Quantity.class, Price.class);
            constructor.setAccessible(true);
            
            Order order = constructor.newInstance(orderId, traderId, symbol, orderType, quantity, price);
            
            // Set status using reflection (simplified)
            Field statusField = Order.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(order, OrderStatus.valueOf(entity.getStatus()));
            
            return order;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to reconstruct domain object", e);
        }
    }
}

/**
 * External Market Data Adapter
 */
@Component
public class ExternalMarketDataAdapter implements MarketDataPort {
    
    private final MarketDataClient marketDataClient;
    private final Map<Symbol, List<PriceUpdateCallback>> subscribers = new ConcurrentHashMap<>();
    
    public ExternalMarketDataAdapter(MarketDataClient marketDataClient) {
        this.marketDataClient = marketDataClient;
    }
    
    @Override
    public Optional<MarketPrice> getCurrentPrice(Symbol symbol) {
        try {
            ExternalPriceData priceData = marketDataClient.getCurrentPrice(symbol.getValue());
            if (priceData != null) {
                return Optional.of(new MarketPrice(
                    symbol,
                    priceData.getPrice(),
                    priceData.getTimestamp()
                ));
            }
            return Optional.empty();
        } catch (Exception e) {
            System.err.println("Failed to get current price for " + symbol + ": " + e.getMessage());
            return Optional.empty();
        }
    }
    
    @Override
    public boolean isMarketOpen(Symbol symbol) {
        try {
            return marketDataClient.isMarketOpen(symbol.getValue());
        } catch (Exception e) {
            System.err.println("Failed to check market status for " + symbol + ": " + e.getMessage());
            return false; // Assume closed on error
        }
    }
    
    @Override
    public void subscribeToPriceUpdates(Symbol symbol, PriceUpdateCallback callback) {
        subscribers.computeIfAbsent(symbol, k -> new ArrayList<>()).add(callback);
        
        // Subscribe to external price feed
        marketDataClient.subscribeToPriceUpdates(symbol.getValue(), priceData -> {
            MarketPrice marketPrice = new MarketPrice(
                symbol,
                priceData.getPrice(),
                priceData.getTimestamp()
            );
            
            // Notify all callbacks for this symbol
            subscribers.getOrDefault(symbol, List.of())
                .forEach(cb -> {
                    try {
                        cb.onPriceUpdate(marketPrice);
                    } catch (Exception e) {
                        System.err.println("Error in price update callback: " + e.getMessage());
                    }
                });
        });
    }
}

// External market data client interface
interface MarketDataClient {
    ExternalPriceData getCurrentPrice(String symbol);
    boolean isMarketOpen(String symbol);
    void subscribeToPriceUpdates(String symbol, ExternalPriceCallback callback);
}

class ExternalPriceData {
    private final BigDecimal price;
    private final Instant timestamp;
    
    public ExternalPriceData(BigDecimal price, Instant timestamp) {
        this.price = price;
        this.timestamp = timestamp;
    }
    
    public BigDecimal getPrice() { return price; }
    public Instant getTimestamp() { return timestamp; }
}

@FunctionalInterface
interface ExternalPriceCallback {
    void onPriceUpdate(ExternalPriceData priceData);
}
```

## Practice Exercises

### Exercise 1: Complete DDD Implementation

Implement additional aggregates like Portfolio, Position, and Trader with proper domain modeling.

### Exercise 2: Event Sourcing Integration

Add event sourcing to the Order aggregate to maintain a complete audit trail.

### Exercise 3: Advanced Hexagonal Architecture

Implement additional adapters for different external systems (REST APIs, message queues, file systems).

### Exercise 4: Domain Event Handling

Create comprehensive domain event handlers for business process orchestration.

## Key Takeaways

1. **Domain-Driven Design**: Model business complexity through rich domain objects with proper boundaries
2. **Hexagonal Architecture**: Separate business logic from technical concerns using ports and adapters
3. **Aggregate Design**: Ensure consistency boundaries and proper encapsulation of business rules
4. **Clean Architecture**: Dependencies point inward, making the system testable and maintainable
5. **Business Logic Isolation**: Keep core business rules independent of frameworks and external systems

Enterprise architectures require careful design to manage complexity while maintaining flexibility and testability.